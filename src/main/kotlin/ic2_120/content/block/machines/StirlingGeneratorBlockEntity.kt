package ic2_120.content.block.machines

import ic2_120.content.ModBlockEntities
import ic2_120.content.block.StirlingGeneratorBlock
import ic2_120.content.screen.StirlingGeneratorScreenHandler
import ic2_120.content.sync.StirlingGeneratorSync
import ic2_120.content.syncs.SyncedData
import ic2_120.content.heat.IHeatConsumer
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.annotation.RegisterEnergy
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventories
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.PacketByteBuf
import net.minecraft.screen.ScreenHandler
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.state.property.Properties
import net.minecraft.text.Text
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World
import org.slf4j.LoggerFactory

@ModBlockEntity(block = StirlingGeneratorBlock::class)
class StirlingGeneratorBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : HeatConsumerBlockEntityBase(type, pos, state), Inventory, ExtendedScreenHandlerFactory {

    companion object {
        const val STIRLING_TIER = 2
        const val BATTERY_SLOT = 0
        const val INVENTORY_SIZE = 1

        private const val NBT_HEAT_BUFFERED = "HeatBuffered"

        const val HU_PER_EU = 2L
        const val MAX_OUTPUT_EU_PER_TICK = 50L
        const val MAX_HEAT_PER_TICK = MAX_OUTPUT_EU_PER_TICK * HU_PER_EU

        private val logger = LoggerFactory.getLogger("ic2_120/StirlingGenerator")
    }

    override val tier: Int = STIRLING_TIER
    private val inventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY)
    val syncedData = SyncedData(this)

    @RegisterEnergy
    val sync = StirlingGeneratorSync(
        syncedData,
        { world?.getBlockState(pos)?.get(Properties.HORIZONTAL_FACING) ?: Direction.NORTH },
        { world?.time }
    )

    private var heatBuffered: Long = 0L

    constructor(pos: BlockPos, state: BlockState) : this(
        ModBlockEntities.getType(StirlingGeneratorBlockEntity::class),
        pos,
        state
    )

    override fun getInventory(): Inventory = this
    override fun size(): Int = INVENTORY_SIZE
    override fun isEmpty(): Boolean = inventory.all { it.isEmpty }
    override fun getStack(slot: Int): ItemStack = inventory.getOrElse(slot) { ItemStack.EMPTY }
    override fun removeStack(slot: Int, amount: Int): ItemStack = Inventories.splitStack(inventory, slot, amount)
    override fun removeStack(slot: Int): ItemStack = Inventories.removeStack(inventory, slot)
    override fun clear() = inventory.clear()
    override fun canPlayerUse(player: PlayerEntity): Boolean = Inventory.canPlayerUse(this, player)

    override fun setStack(slot: Int, stack: ItemStack) {
        if (slot == BATTERY_SLOT && stack.count > 1) stack.count = 1
        inventory[slot] = stack
        markDirty()
    }

    override fun writeScreenOpeningData(player: ServerPlayerEntity, buf: PacketByteBuf) {
        buf.writeBlockPos(pos)
        buf.writeVarInt(syncedData.size())
    }

    override fun getDisplayName(): Text = Text.translatable("block.ic2_120.stirling_generator")

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?): ScreenHandler =
        StirlingGeneratorScreenHandler(
            syncId,
            playerInventory,
            this,
            net.minecraft.screen.ScreenHandlerContext.create(world!!, pos),
            syncedData
        )

    override fun readNbt(nbt: NbtCompound) {
        super.readNbt(nbt)
        Inventories.readNbt(nbt, inventory)
        syncedData.readNbt(nbt)
        sync.amount = nbt.getLong(StirlingGeneratorSync.NBT_ENERGY_STORED).coerceIn(0L, StirlingGeneratorSync.ENERGY_CAPACITY)
        sync.syncCommittedAmount()
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
        heatBuffered = nbt.getLong(NBT_HEAT_BUFFERED)
    }

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)
        Inventories.writeNbt(nbt, inventory)
        syncedData.writeNbt(nbt)
        nbt.putLong(StirlingGeneratorSync.NBT_ENERGY_STORED, sync.amount)
        nbt.putLong(NBT_HEAT_BUFFERED, heatBuffered)
    }

    override fun receiveHeatInternal(hu: Long): Long {
        if (hu <= 0L) return 0L
        val toAdd = hu.coerceAtMost(MAX_HEAT_PER_TICK - heatBuffered)
        if (toAdd <= 0L) {
            logger.info("[StirlingGenerator] receiveHeatInternal: 接收 {} HU, 但缓冲已满 (当前 {}/最大 {})",
                hu, heatBuffered, MAX_HEAT_PER_TICK)
            return 0L
        }
        heatBuffered += toAdd
        markDirty()
        logger.info("[StirlingGenerator] receiveHeatInternal: 接收 {} HU, 缓冲: {} -> {}",
            toAdd, heatBuffered - toAdd, heatBuffered)
        return toAdd
    }

    fun tick(world: World, pos: BlockPos, state: BlockState) {
        if (world.isClient) return
        sync.energy = sync.amount.toInt().coerceAtLeast(0)

        val euToGenerate = (heatBuffered / HU_PER_EU).coerceAtMost(MAX_OUTPUT_EU_PER_TICK)
        logger.info("[StirlingGenerator] tick: 热缓冲={}, 可产生EU={}, HU/EU={}",
            heatBuffered, euToGenerate, HU_PER_EU)
        if (euToGenerate > 0L) {
            val space = (StirlingGeneratorSync.ENERGY_CAPACITY - sync.amount).coerceAtLeast(0L)
            val actualGenerate = minOf(euToGenerate, space)
            if (actualGenerate > 0L) {
                sync.generateEnergy(actualGenerate)
                heatBuffered -= actualGenerate * HU_PER_EU
                logger.info("[StirlingGenerator] tick: 产生 {} EU, 消耗 {} HU, 缓冲剩余 {}",
                    actualGenerate, actualGenerate * HU_PER_EU, heatBuffered)
                markDirty()
            }
        }

        val active = sync.amount < StirlingGeneratorSync.ENERGY_CAPACITY &&
            heatBuffered > 0L &&
            hasValidHeatSource()
        if (state.get(StirlingGeneratorBlock.ACTIVE) != active) {
            world.setBlockState(pos, state.with(StirlingGeneratorBlock.ACTIVE, active))
        }
        sync.syncCurrentTickFlow()
    }

    private fun hasValidHeatSource(): Boolean {
        val world = world ?: return false
        val myFace = getHeatTransferFace()
        val neighborPos = pos.offset(myFace)
        val neighbor = world.getBlockEntity(neighborPos) as? ic2_120.content.heat.IHeatNode ?: run {
            logger.info("[StirlingGenerator] hasValidHeatSource: 邻居位置 {} 没有IHeatNode",
                neighborPos)
            return false
        }
        val neighborFace = neighbor.getHeatTransferFace()
        val isValid = neighborFace == myFace.opposite
        logger.info("[StirlingGenerator] hasValidHeatSource: 我的传热面 {}, 邻居传热面 {}, 有效 = {}",
            myFace, neighborFace, isValid)
        return isValid
    }

    fun onNeighborUpdate() {
        markDirty()
    }
}
