package ic2_120.content.blockentities

import ic2_120.content.ElectricFurnaceSync
import ic2_120.content.ModBlockEntities
import ic2_120.content.block.ElectricFurnaceBlock
import ic2_120.content.screen.ElectricFurnaceScreenHandler
import ic2_120.content.syncs.SyncedData
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.annotation.RegisterEnergy
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
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

/**
 * 电力熔炉方块实体。提供输入/输出槽位并实现简单 GUI。
 * [sync] 继承 TickLimitedEnergyStorage，即同步数据又即 Energy API 存储。
 */
@ModBlockEntity(block = ElectricFurnaceBlock::class)
class ElectricFurnaceBlockEntity(
    type: net.minecraft.block.entity.BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : BlockEntity(type, pos, state), Inventory, ExtendedScreenHandlerFactory {

    private val inventory = DefaultedList.ofSize(2, ItemStack.EMPTY)  // 0: 输入, 1: 输出

    val syncedData = SyncedData(this)
    @RegisterEnergy
    val sync = ElectricFurnaceSync(syncedData) { world?.time }

    constructor(pos: BlockPos, state: BlockState) : this(
        ModBlockEntities.getType(ElectricFurnaceBlockEntity::class),
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
    override fun markDirty() {
        super.markDirty()
    }
    override fun canPlayerUse(player: PlayerEntity): Boolean =
        Inventory.canPlayerUse(this, player)

    override fun writeScreenOpeningData(player: net.minecraft.server.network.ServerPlayerEntity, buf: PacketByteBuf) {
        buf.writeBlockPos(pos)
        buf.writeVarInt(syncedData.size())
    }

    override fun getDisplayName(): Text =
        Text.translatable("block.ic2_120.electric_furnace")

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?): ScreenHandler =
        ElectricFurnaceScreenHandler(syncId, playerInventory, this, net.minecraft.screen.ScreenHandlerContext.create(world!!, pos), syncedData)

    override fun readNbt(nbt: NbtCompound) {
        super.readNbt(nbt)
        Inventories.readNbt(nbt, inventory)
        syncedData.readNbt(nbt)
        sync.amount = nbt.getLong(ElectricFurnaceSync.NBT_ENERGY_STORED)
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
    }

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)
        Inventories.writeNbt(nbt, inventory)
        syncedData.writeNbt(nbt)
        nbt.putLong(ElectricFurnaceSync.NBT_ENERGY_STORED, sync.amount)
    }

    fun tick(world: World, pos: BlockPos, state: BlockState) {
        if (world.isClient) return
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
        // if (world.time % 20L == 0L) markDirty()
    }
}
