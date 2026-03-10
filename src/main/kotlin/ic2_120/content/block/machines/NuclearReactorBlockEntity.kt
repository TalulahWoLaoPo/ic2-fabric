package ic2_120.content.block.machines

import ic2_120.content.ModBlockEntities
import ic2_120.content.block.IGenerator
import ic2_120.content.block.ITieredMachine
import ic2_120.content.block.NuclearReactorBlock
import ic2_120.Ic2_120
import ic2_120.content.block.ReactorChamberBlock
import ic2_120.content.screen.NuclearReactorScreenHandler
import ic2_120.content.sync.NuclearReactorSync
import net.minecraft.registry.Registries
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
import net.minecraft.state.property.Properties
import net.minecraft.text.Text
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World

/**
 * 核反应堆方块实体。
 *
 * 多方块结构：中心为核反应堆，六面可各接触 0 或 1 个核反应仓。
 * 容量 = 27 + 相邻反应仓数 * 9，最大 81 格。
 * 所有 NBT 存在反应堆 BE 上。
 * 电力等级 5，视为发电机（IGenerator），不实现电池充电。
 */
@ModBlockEntity(block = NuclearReactorBlock::class)
class NuclearReactorBlockEntity(
    type: net.minecraft.block.entity.BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : BlockEntity(type, pos, state), Inventory, IGenerator, ITieredMachine,
    ExtendedScreenHandlerFactory {

    override val tier: Int = NuclearReactorSync.REACTOR_TIER

    private val inventory = DefaultedList.ofSize(MAX_SLOTS, ItemStack.EMPTY)

    val syncedData = SyncedData(this)
    @RegisterEnergy
    val sync = NuclearReactorSync(
        syncedData,
        getFacing = { world?.getBlockState(pos)?.get(Properties.HORIZONTAL_FACING) ?: Direction.NORTH },
        currentTickProvider = { world?.time }
    )

    constructor(pos: BlockPos, state: BlockState) : this(
        ModBlockEntities.getType(NuclearReactorBlockEntity::class),
        pos,
        state
    )

    override fun size(): Int = MAX_SLOTS
    override fun getStack(slot: Int): ItemStack = inventory.getOrElse(slot) { ItemStack.EMPTY }
    override fun setStack(slot: Int, stack: ItemStack) {
        if (!stack.isEmpty) {
            if (!isReactorComponent(stack)) return
            if (slot >= currentCapacity()) return  // 防止物流 mod 向超出容量的槽位强制插入
        }
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
        buf.writeVarInt(currentCapacity())
    }

    override fun getDisplayName(): Text = Text.translatable("block.ic2_120.nuclear_reactor")

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?): ScreenHandler =
        NuclearReactorScreenHandler(syncId, playerInventory, this, net.minecraft.screen.ScreenHandlerContext.create(world!!, pos), syncedData, currentCapacity())

    override fun readNbt(nbt: NbtCompound) {
        super.readNbt(nbt)
        Inventories.readNbt(nbt, inventory)
        syncedData.readNbt(nbt)
        sync.amount = nbt.getLong(NuclearReactorSync.NBT_ENERGY_STORED).coerceIn(0L, NuclearReactorSync.ENERGY_CAPACITY)
        sync.syncCommittedAmount()
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
    }

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)
        Inventories.writeNbt(nbt, inventory)
        syncedData.writeNbt(nbt)
        nbt.putLong(NuclearReactorSync.NBT_ENERGY_STORED, sync.amount)
    }

    /** 当前有效容量（27 + 相邻反应仓数 * 9），供 setStack 等服务端逻辑校验 */
    private fun currentCapacity(): Int {
        val world = world ?: return NuclearReactorSync.BASE_SLOTS
        var chamberCount = 0
        for (dir in Direction.values()) {
            if (world.getBlockState(pos.offset(dir)).block is ReactorChamberBlock) chamberCount++
        }
        return NuclearReactorSync.BASE_SLOTS + chamberCount * NuclearReactorSync.SLOTS_PER_CHAMBER
    }

    fun tick(world: World, pos: BlockPos, state: BlockState) {
        if (world.isClient) return

        val newCapacity = currentCapacity()
        sync.capacity = newCapacity

        // 容量减少时，溢出槽位的物品掉落
        if (newCapacity < MAX_SLOTS) {
            for (i in newCapacity until MAX_SLOTS) {
                val stack = getStack(i)
                if (!stack.isEmpty) {
                    net.minecraft.util.ItemScatterer.spawn(world, pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(), stack)
                    setStack(i, ItemStack.EMPTY)
                }
            }
        }

        //  placeholder 发电：有燃料棒时发电
        val hasFuel = (0 until newCapacity).any { !getStack(it).isEmpty }
        if (hasFuel) {
            val gen = 100L.coerceAtMost(NuclearReactorSync.ENERGY_CAPACITY - sync.amount)
            if (gen > 0) {
                sync.amount = (sync.amount + gen).coerceAtMost(NuclearReactorSync.ENERGY_CAPACITY)
            }
        }

        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)

        val active = hasFuel
        if (state.get(NuclearReactorBlock.ACTIVE) != active) {
            world.setBlockState(pos, state.with(NuclearReactorBlock.ACTIVE, active))
        }
    }

    companion object {
        const val MAX_SLOTS = 81

        /** 允许放入核反应堆的反应堆核心部件 ID（见 assets-inventory 反应堆核心部件） */
        private val REACTOR_COMPONENT_IDS = setOf(
            "reactor_coolant_cell", "triple_reactor_coolant_cell", "sextuple_reactor_coolant_cell",
            "reactor_plating", "reactor_heat_plating", "containment_reactor_plating",
            "heat_exchanger", "reactor_heat_exchanger", "component_heat_exchanger", "advanced_heat_exchanger",
            "heat_vent", "reactor_heat_vent", "overclocked_heat_vent", "component_heat_vent", "advanced_heat_vent",
            "neutron_reflector", "thick_neutron_reflector", "iridium_neutron_reflector",
            "rsh_condensator", "lzh_condensator",
            "fuel_rod", "uranium_fuel_rod", "dual_uranium_fuel_rod", "quad_uranium_fuel_rod",
            "mox_fuel_rod", "dual_mox_fuel_rod", "quad_mox_fuel_rod",
            "lithium_fuel_rod", "depleted_isotope_fuel_rod", "heatpack"
        )

        fun isReactorComponent(stack: ItemStack): Boolean {
            if (stack.isEmpty) return false
            val id = Registries.ITEM.getId(stack.item)
            return id.namespace == Ic2_120.MOD_ID && id.path in REACTOR_COMPONENT_IDS
        }
    }
}
