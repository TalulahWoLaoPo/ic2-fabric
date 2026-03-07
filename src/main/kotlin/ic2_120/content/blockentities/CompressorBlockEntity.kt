package ic2_120.content.blockentities

import ic2_120.content.recipes.CompressorRecipes
import ic2_120.content.CompressorSync
import ic2_120.content.ModBlockEntities
import ic2_120.content.pullEnergyFromNeighbors
import ic2_120.content.block.CompressorBlock
import ic2_120.content.screen.CompressorScreenHandler
import ic2_120.content.syncs.SyncedData
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.annotation.RegisterEnergy
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventories
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.PacketByteBuf
import net.minecraft.screen.ScreenHandler
import net.minecraft.text.Text
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

@ModBlockEntity(block = CompressorBlock::class)
class CompressorBlockEntity(
    type: net.minecraft.block.entity.BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : BlockEntity(type, pos, state), Inventory, net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory {

    private val inventory = DefaultedList.ofSize(2, ItemStack.EMPTY)  // 0: 输入, 1: 输出

    val syncedData = SyncedData(this)
    @RegisterEnergy
    val sync = CompressorSync(syncedData) { world?.time }

    constructor(pos: BlockPos, state: BlockState) : this(
        ModBlockEntities.getType(CompressorBlockEntity::class),
        pos,
        state
    )

    override fun size(): Int = 2
    override fun getStack(slot: Int): ItemStack = inventory.getOrElse(slot) { ItemStack.EMPTY }
    override fun setStack(slot: Int, stack: ItemStack) {
        inventory[slot] = stack
        if (stack.count > maxCountPerStack) stack.count = maxCountPerStack
        markDirty()
    }
    override fun removeStack(slot: Int, amount: Int): ItemStack = Inventories.splitStack(inventory, slot, amount)
    override fun removeStack(slot: Int): ItemStack = Inventories.removeStack(inventory, slot)
    override fun clear() = inventory.clear()
    override fun isEmpty(): Boolean = inventory.all { it.isEmpty }
    override fun markDirty() { super.markDirty() }
    override fun canPlayerUse(player: PlayerEntity): Boolean = Inventory.canPlayerUse(this, player)

    override fun writeScreenOpeningData(player: net.minecraft.server.network.ServerPlayerEntity, buf: PacketByteBuf) {
        buf.writeBlockPos(pos)
        buf.writeVarInt(syncedData.size())
    }

    override fun getDisplayName(): Text = Text.translatable("block.ic2_120.compressor")

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?): ScreenHandler =
        CompressorScreenHandler(syncId, playerInventory, this, net.minecraft.screen.ScreenHandlerContext.create(world!!, pos), syncedData)

    override fun readNbt(nbt: NbtCompound) {
        super.readNbt(nbt)
        Inventories.readNbt(nbt, inventory)
        syncedData.readNbt(nbt)
        sync.amount = nbt.getLong(CompressorSync.NBT_ENERGY_STORED)
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
    }

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)
        Inventories.writeNbt(nbt, inventory)
        syncedData.writeNbt(nbt)
        nbt.putLong(CompressorSync.NBT_ENERGY_STORED, sync.amount)
    }

    fun tick(world: World, pos: BlockPos, state: BlockState) {
        if (world.isClient) return
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
        pullEnergyFromNeighbors(world, pos, sync, CompressorSync.MAX_INSERT)

        val input = getStack(0)
        val recipe = CompressorRecipes.getRecipe(input) ?: run {
            if (sync.progress != 0) sync.progress = 0
            setActiveState(world, pos, state, false)
            return
        }
        val (inputCount, result) = recipe
        val outputSlot = getStack(1)
        val maxStack = result.maxCount
        val canAccept = outputSlot.isEmpty() ||
            (ItemStack.areItemsEqual(outputSlot, result) && outputSlot.count + result.count <= maxStack)

        if (!canAccept) {
            if (sync.progress != 0) sync.progress = 0
            setActiveState(world, pos, state, false)
            return
        }

        if (sync.progress >= CompressorSync.PROGRESS_MAX) {
            input.decrement(inputCount)
            if (outputSlot.isEmpty()) setStack(1, result)
            else outputSlot.increment(result.count)
            sync.progress = 0
            markDirty()
            setActiveState(world, pos, state, false)
            return
        }

        val need = CompressorSync.ENERGY_PER_TICK
        if (sync.amount >= need) {
            sync.amount = (sync.amount - need).coerceAtLeast(0L)
            sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
            sync.progress += 1
            markDirty()
            setActiveState(world, pos, state, true)
        } else {
            setActiveState(world, pos, state, false)
        }
    }

    private fun setActiveState(world: World, pos: BlockPos, state: BlockState, active: Boolean) {
        if (state.get(CompressorBlock.ACTIVE) != active) {
            world.setBlockState(pos, state.with(CompressorBlock.ACTIVE, active))
        }
    }
}
