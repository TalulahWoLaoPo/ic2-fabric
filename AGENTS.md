# Agents.md - IC2-120 快速入门指南

## 一、Mod 注册流程（最简）

### 1.1 使用类级别注解注册

```kotlin
// 方块注册
@ModBlock(name = "copper_block", registerItem = true, tab = CreativeTab.IC2_MATERIALS)
class CopperBlock : Block(AbstractBlock.Settings.create())

// 物品注册
@ModItem(name = "copper_ingot", tab = CreativeTab.IC2_MATERIALS)
class CopperIngot : Item(FabricItemSettings())

// 方块实体注册
@ModBlockEntity(name = "electric_furnace")
class ElectricFurnaceBlockEntity(...) : BlockEntity(...)

// 物品栏注册
@ModCreativeTab(name = "ic2_materials", iconItem = "copper_ingot")
class Ic2MaterialsTab

// ScreenHandler 注册（服务端）
@ModScreenHandler(name = "electric_furnace")
class ElectricFurnaceScreenHandler(...) : ScreenHandler(...)

// Screen 注册（客户端）
@ModScreen(handler = "electric_furnace")
class ElectricFurnaceScreen(...) : HandledScreen<...>(...)
```

### 1.2 主入口点启用扫描

```kotlin
object Ic2_120 : ModInitializer {
    const val MOD_ID = "ic2_120"

    override fun onInitialize() {
        ClassScanner.scanAndRegister(
            MOD_ID,
            listOf(
                "ic2_120.content.tab",
                "ic2_120.content.block",
                "ic2_120.content.screen",
                "ic2_120.content.item"
            )
        )
    }
}
```

**关键特性**：
- ✅ 类级别注解：直接在类上添加 `@ModBlock`、`@ModItem` 等
- ✅ 类型安全枚举：使用 `CreativeTab.IC2_MATERIALS` 替代字符串
- ✅ 自动扫描：`ClassScanner.scanAndRegister()` 自动注册所有带注解的类

**注册顺序**：方块 → 方块实体类型 → ScreenHandler → 物品 → 物品栏

---

## 二、子系统快速概览

### 2.1 电力系统 (EU)

**核心组件**：
- **能量存储**：`UpgradeableTickLimitedSidedEnergyContainer` - 带能量容量、输入输出限制的容器
- **同步数据**：`XxxSync` 继承 `UpgradeableTickLimitedSidedEnergyContainer`，同步能量、进度等数据
- **能量网络**：`EnergyNetwork` - 导线连通形成的能量池，支持传输、损耗、过压检测
- **能量流速同步**：`EnergyFlowSync` - 统计输入/输出/发电/耗能速率，滑动平均显示

**关键概念**：
- EU 单位：1 EU/t = 每tick 1 个能量单位
- 电压等级：1=LV(32), 2=MV(128), 3=HV(512), 4=EV(2048)
- 电缆损耗：每根导线有损耗权重，Dijkstra 路径选择最优传输路径
- 过压爆炸：电网电压等级 > 机器耐压等级时机器会爆炸

**详细文档**：`docs/energy-network.md`、`docs/energy-flow-sync.md`

### 2.2 流体系统

**核心组件**：
- **管道网络**：`PipeNetwork` - 流体管道连通形成的流体网络
- **流体存储**：`SingleVariantStorage<FluidVariant>` - 机器内部流体储罐
- **流体升级**：`fluid_ejector_upgrade`、`fluid_pulling_upgrade` - 控制机器参与流体管道

**关键概念**：
- 流量单位：桶/s (1 bucket = 1000 mB)
- 单流体约束：同一网络每 tick 只允许一种流体
- Provider/Receiver 规则：安装对应升级的机器才参与管道分配

**详细文档**：`docs/fluid-system.md`

### 2.3 热能系统 (HU)

**核心组件**：
- **加热机**：产热并向外传输 HU（如 FluidHeatGenerator、ElectricHeatGenerator）
- **耗热机**：接收并消耗 HU
- **传热规则**：仅背面传热，必须两台机器背面互相贴合

**关键概念**：
- HU 单位：热能单位
- 无热量缓存：产生的热量必须立即传输，否则丢失
- 激活条件：有燃料/能量 且 背面有有效耗热机

**详细文档**：`docs/heat-system.md`

### 2.4 核电系统

**核心组件**：
- **核反应堆**：多方块结构（中心反应堆 + 6 个反应仓）
- **反应堆组件**：燃料棒、散热片、热交换器、中子反射器
- **双阶段计算**：Pass 0 发电与中子脉冲，Pass 1 热量分配

**关键概念**：
- 槽位容量：27 + 反应仓数 × 9（最大 81）
- 堆温：0 ~ 10,000，超过 10,000 爆炸
- MOX 燃料：发电量随堆温增加（0% → 100% 时 1.0 → 5.0 output/脉冲）

**详细文档**：`docs/nuclear-power.md`

### 2.5 升级系统

**核心组件**：
- **升级物品**：`OverclockerUpgrade`、`TransformerUpgrade`、`EnergyStorageUpgrade` 等
- **机器接口**：`IOverclockerUpgradeSupport`、`ITransformerUpgradeSupport` 等
- **升级组件**：`OverclockerUpgradeComponent`、`EnergyStorageUpgradeComponent` 等

**升级效果**：
- **加速升级**：每个超频缩短到 70% 耗能 ×1.6^n
- **高压升级**：每个提高电压等级 1，增加输入速度
- **储能升级**：每个增加 10,000 EU 电量缓冲

**详细文档**：`docs/upgrade-system.md`

### 2.6 同步系统 (SyncSystem)

**核心组件**：
- `SyncedData` - 服务端：实现 `PropertyDelegate`，通过 `addProperties()` 同步
- `SyncedDataView` - 客户端：包装 `PropertyDelegate`，与服务端共用属性定义类

**关键特性**：
- 属性只定义一次，两端复用同一个属性定义类
- 自动 index 对齐，无需手写 index
- 支持 NBT 持久化（`readNbt`/`writeNbt`）

**详细文档**：`docs/sync-system.md`

---

## 三、实现一台机器（完整模板）

### 3.1 前置决策

| 项目 | 值 |
|------|-----|
| 机器 ID | `macerator` |
| 能量等级 | `1-4` (1=LV, 2=MV, 3=HV, 4=EV) |
| 能量容量 | `xxx EU` (建议 4-10 秒耗能) |
| 每tick耗能 | `x EU/t` |
| 加工时间 | `xxx ticks` |

---

### 3.2 创建 Block 方块

**文件**：`src/main/kotlin/ic2_120/content/block/XxxBlock.kt`

```kotlin
package ic2_120.content.block

import ic2_120.content.block.machines.XxxBlockEntity
import ic2_120.registry.annotation.ModBlock
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.block.AbstractBlock.Settings
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.util.Identifier

@ModBlock(name = "xxx", registerItem = true, tab = CreativeTab.IC2_MACHINES)
class XxxBlock : MachineBlock(Settings.create()) {

    companion object {
        val ACTIVE = Properties.FACING_HOPPER // 或 Properties.POWERED
    }

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? =
        XxxBlockEntity(pos, state)

    override fun getTicker(world: World?, state: BlockState?, type: BlockEntityType<XxxBlockEntity>?): BlockEntityTicker<XxxBlockEntity>? =
        BlockEntityTicker { world, pos, state, blockEntity -> blockEntity.tick(world, pos, state) }
}
```

---

### 3.3 创建 BlockEntity

**文件**：`src/main/kotlin/ic2_120/content/block/machines/XxxBlockEntity.kt`

```kotlin
package ic2_120.content.block.machines

import ic2_120.content.sync.XxxSync
import ic2_120.content.ModBlockEntities
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.content.UpgradeableTickLimitedSidedEnergyContainer
import ic2_120.content.UpgradeableTickLimitedSidedEnergyContainer
import ic2_120.content.energy.charge.BatteryDischargerComponent
import ic2_120.content.upgrade.OverclockerUpgradeSupport
import ic2_120.content.upgrade.EnergyStorageUpgradeSupport
import ic2_120.content.upgrade.TransformerUpgradeSupport
import ic2_120.content.upgrade.OverclockerUpgradeComponent
import ic2_120.content.upgrade.EnergyStorageUpgradeComponent
import ic2_120.content.upgrade.TransformerUpgradeComponent
import ic2_120.content.block.MachineBlock
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.BlockEntityType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.PacketByteBuf
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

@ModBlockEntity(block = XxxBlock::class)
class XxxBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : BlockEntity(type, pos, state),
    Inventory,
    ITieredMachine,
    IOverclockerUpgradeSupport,
    IEnergyStorageUpgradeSupport,
    ITransformerUpgradeSupport,
    ExtendedScreenHandlerFactory {

    // 升级属性
    override var speedMultiplier: Float = 1f
    override var energyMultiplier: Float = 1f
    override var capacityBonus: Long = 0L
    override var voltageTierBonus: Int = 0

    companion object {
        const val XXX_TIER = 1           // 能量等级
        const val SLOT_INPUT = 0          // 输入槽
        const val SLOT_OUTPUT = 1         // 输出槽
        const val SLOT_DISCHARGING = 2    // 电池放电槽
        const val SLOT_UPGRADE_0 = 3      // 升级槽 0
        const val SLOT_UPGRADE_1 = 4      // 升级槽 1
        const val SLOT_UPGRADE_2 = 5      // 升级槽 2
        const val SLOT_UPGRADE_3 = 6      // 升级槽 3
        val SLOT_UPGRADE_INDICES = intArrayOf(SLOT_UPGRADE_0, SLOT_UPGRADE_1, SLOT_UPGRADE_2, SLOT_UPGRADE_3)
        const val INVENTORY_SIZE = 7       // 总槽位数
    }

    override val tier: Int = XXX_TIER
    private val inventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY)

    // 同步数据
    val syncedData = SyncedData(this)

    // 能量存储（支持升级）
    @RegisterEnergy
    val sync = XxxSync(
        syncedData,
        { world?.time },
        { capacityBonus },
        { TransformerUpgradeComponent.maxInsertForTier(XXX_TIER + voltageTierBonus) }
    )

    // 电池放电组件
    private val batteryDischarger = BatteryDischargerComponent(
        inventory = this,
        batterySlot = SLOT_DISCHARGING,
        machineTierProvider = { XXX_TIER },
        canDischargeNow = { sync.amount < sync.getEffectiveCapacity() }
    )

    // ========== Inventory 接口实现 ==========
    override fun size(): Int = INVENTORY_SIZE
    override fun getStack(slot: Int): ItemStack = inventory.getOrElse(slot) { ItemStack.EMPTY }
    override fun setStack(slot: Int, stack: ItemStack) {
        if (slot == SLOT_DISCHARGING && stack.count > 1) {
            stack.count = 1
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

    // ========== ScreenHandler 工厂方法 ==========
    override fun writeScreenOpeningData(player: ServerPlayerEntity, buf: PacketByteBuf) {
        buf.writeBlockPos(pos)
        buf.writeVarInt(syncedData.size())
    }

    override fun getDisplayName(): Text = Text.translatable("block.ic2_120.xxx")

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?): ScreenHandler =
        XxxScreenHandler(syncId, playerInventory, this, ScreenHandlerContext.create(world!!, pos), syncedData)

    // ========== NBT 持久化 ==========
    override fun readNbt(nbt: NbtCompound) {
        super.readNbt(nbt)
        Inventories.readNbt(nbt, inventory)
        syncedData.readNbt(nbt)
        sync.amount = nbt.getLong(XxxSync.NBT_ENERGY_STORED)
        sync.syncCommittedAmount()
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
    }

    override fun writeNbt(nbt: NbtCompound) {
        super.writeNbt(nbt)
        Inventories.writeNbt(nbt, inventory)
        syncedData.writeNbt(nbt)
        nbt.putLong(XxxSync.NBT_ENERGY_STORED, sync.amount)
    }

    // ========== 核心逻辑：tick() ==========
    fun tick(world: World, pos: BlockPos, state: BlockState) {
        if (world.isClient) return
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)

        // 1. 应用升级效果
        OverclockerUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        EnergyStorageUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        TransformerUpgradeComponent.apply(this, SLOT_UPGRADE_INDICES, this)
        sync.energyCapacity = sync.getEffectiveCapacity().toInt().coerceIn(0, Int.MAX_VALUE)

        // 2. 从相邻导线或电池槽提取能量
        pullEnergyFromNeighbors(world, pos, sync)
        extractFromDischargingSlot()

        // 3. 检查输入物品
        val input = getStack(SLOT_INPUT)
        if (input.isEmpty) {
            if (sync.progress != 0) sync.progress = 0
            setActiveState(world, pos, state, false)
            sync.syncCurrentTickFlow()
            return
        }

        // 4. 检查配方
        val result = XxxRecipes.getOutput(input) ?: run {
            if (sync.progress != 0) sync.progress = 0
            setActiveState(world, pos, state, false)
            sync.syncCurrentTickFlow()
            return
        }

        // 5. 检查输出槽是否有空间
        val outputSlot = getStack(SLOT_OUTPUT)
        val maxStack = result.maxCount
        val canAccept = outputSlot.isEmpty() ||
            (ItemStack.areItemsEqual(outputSlot, result) && outputSlot.count + result.count <= maxStack)

        if (!canAccept) {
            if (sync.progress != 0) sync.progress = 0
            setActiveState(world, pos, state, false)
            sync.syncCurrentTickFlow()
            return
        }

        // 6. 加工完成
        if (sync.progress >= XxxSync.PROGRESS_MAX) {
            input.decrement(1)
            if (outputSlot.isEmpty()) setStack(SLOT_OUTPUT, result)
            else outputSlot.increment(result.count)
            sync.progress = 0
            markDirty()
            setActiveState(world, pos, state, false)
            sync.syncCurrentTickFlow()
            return
        }

        // 7. 消耗能量并增加进度
        val progressIncrement = speedMultiplier.toInt().coerceAtLeast(1)
        val need = (XxxSync.ENERGY_PER_TICK * energyMultiplier).toLong().coerceAtLeast(1L)
        if (sync.consumeEnergy(need) > 0L) {
            sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
            sync.progress += progressIncrement
            markDirty()
            setActiveState(world, pos, state, true)
        } else {
            setActiveState(world, pos, state, false)
        }

        sync.syncCurrentTickFlow()
    }

    private fun setActiveState(world: World, pos: BlockPos, state: BlockState, active: Boolean) {
        if (state.get(XxxBlock.ACTIVE) != active) {
            world.setBlockState(pos, state.with(XxxBlock.ACTIVE, active))
        }
    }

    private fun extractFromDischargingSlot() {
        val space = (sync.getEffectiveCapacity() - sync.amount).coerceAtLeast(0L)
        if (space <= 0L) return

        val request = minOf(space, sync.getEffectiveMaxInsertPerTick())
        val extracted = batteryDischarger.tick(request)
        if (extracted <= 0L) return

        sync.insertEnergy(extracted)
        sync.energy = sync.amount.toInt().coerceIn(0, Int.MAX_VALUE)
        markDirty()
    }
}
```

---

### 3.4 创建 Sync（能量与进度同步）

**文件**：`src/main/kotlin/ic2_120/content/sync/XxxSync.kt`

```kotlin
package ic2_120.content.sync

import ic2_120.content.UpgradeableTickLimitedSidedEnergyContainer
import ic2_120.content.syncs.SyncSchema

class XxxSync(
    schema: SyncSchema,
    currentTickProvider: () -> Long? = { null },
    capacityBonusProvider: () -> Long = { 0L },
    maxInsertPerTickProvider: (() -> Long)? = null
) : UpgradeableTickLimitedSidedEnergyContainer(
    ENERGY_CAPACITY,
    capacityBonusProvider,
    MAX_INSERT,
    MAX_EXTRACT,
    currentTickProvider,
    maxInsertPerTickProvider
) {

    companion object {
        const val ENERGY_CAPACITY = 416L      // 基础容量
        const val MAX_INSERT = 32L             // 最大输入
        const val MAX_EXTRACT = 0L             // 最大输出（0=不输出）
        const val NBT_ENERGY_STORED = "EnergyStored"
        const val PROGRESS_MAX = 130           // 加工所需 ticks
        const val ENERGY_PER_TICK = 2L         // 每 tick 耗能
    }

    var energy by schema.int("Energy")
    var progress by schema.int("Progress")
    var energyCapacity by schema.int("EnergyCapacity", default = ENERGY_CAPACITY.toInt())

    private val flow = EnergyFlowSync(schema, this)

    override fun onEnergyCommitted() {
        energy = amount.toInt().coerceIn(0, Int.MAX_VALUE)
    }

    fun syncCurrentTickFlow() {
        flow.syncCurrentTickFlow()
    }

    fun getSyncedInsertedAmount(): Long = flow.getSyncedInsertedAmount()
    fun getSyncedExtractedAmount(): Long = flow.getSyncedExtractedAmount()
    fun getSyncedConsumedAmount(): Long = flow.getSyncedConsumedAmount()
}
```

---

### 3.5 创建 ScreenHandler

**文件**：`src/main/kotlin/ic2_120/content/screen/XxxScreenHandler.kt`

```kotlin
package ic2_120.content.screen

import ic2_120.content.sync.XxxSync
import ic2_120.content.block.XxxBlock
import ic2_120.content.block.machines.XxxBlockEntity
import ic2_120.content.item.IUpgradeItem
import ic2_120.content.item.energy.IBatteryItem
import ic2_120.content.screen.slot.PredicateSlot
import ic2_120.content.screen.slot.UpgradeSlotLayout
import ic2_120.content.screen.slot.SlotMoveHelper
import ic2_120.content.screen.slot.SlotSpec
import ic2_120.content.screen.slot.SlotTarget
import ic2_120.content.syncs.SyncedDataView
import ic2_120.registry.annotation.ModScreenHandler
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
import net.minecraft.inventory.SimpleInventory
import net.minecraft.item.ItemStack
import net.minecraft.network.PacketByteBuf
import net.minecraft.screen.ArrayPropertyDelegate
import net.minecraft.screen.PropertyDelegate
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.screen.slot.Slot

@ModScreenHandler(block = XxxBlock::class)
class XxxScreenHandler(
    syncId: Int,
    playerInventory: PlayerInventory,
    blockInventory: Inventory,
    private val context: ScreenHandlerContext,
    private val propertyDelegate: PropertyDelegate
) : ScreenHandler(ModScreenHandlers.getType(XxxScreenHandler::class), syncId) {

    val sync = XxxSync(SyncedDataView(propertyDelegate))

    private val upgradeSlotSpec: SlotSpec by lazy {
        UpgradeSlotLayout.slotSpec { context.get({ world, pos -> world.getBlockEntity(pos) }, null) }
    }

    init {
        checkSize(blockInventory, XxxBlockEntity.INVENTORY_SIZE)
        addProperties(propertyDelegate)

        // 输入槽
        addSlot(PredicateSlot(blockInventory, XxxBlockEntity.SLOT_INPUT, INPUT_SLOT_X, INPUT_SLOT_Y, INPUT_SLOT_SPEC))

        // 放电槽（电池）
        addSlot(PredicateSlot(
            blockInventory,
            XxxBlockEntity.SLOT_DISCHARGING,
            DISCHARGING_SLOT_X,
            DISCHARGING_SLOT_Y,
            DISCHARGING_SLOT_SPEC
        ))

        // 输出槽
        addSlot(PredicateSlot(blockInventory, XxxBlockEntity.SLOT_OUTPUT, OUTPUT_SLOT_X, OUTPUT_SLOT_Y, OUTPUT_SLOT_SPEC))

        // 4 个升级槽
        for (i in 0 until UpgradeSlotLayout.SLOT_COUNT) {
            addSlot(
                PredicateSlot(
                    blockInventory,
                    XxxBlockEntity.SLOT_UPGRADE_INDICES[i],
                    UpgradeSlotLayout.SLOT_X,
                    UpgradeSlotLayout.slotY(i),
                    upgradeSlotSpec
                )
            )
        }

        // 玩家物品栏
        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, PLAYER_INV_X + col * 18, PLAYER_INV_Y + row * 18))
            }
        }

        // 快捷栏
        for (col in 0 until 9) {
            addSlot(Slot(playerInventory, col, PLAYER_INV_X + col * 18, HOTBAR_Y))
        }
    }

    override fun quickMove(player: PlayerEntity, index: Int): ItemStack {
        var stack = ItemStack.EMPTY
        val slot = slots[index]
        if (slot.hasStack()) {
            val stackInSlot = slot.stack
            stack = stackInSlot.copy()
            when (index) {
                SLOT_OUTPUT_INDEX -> {
                    if (!insertItem(stackInSlot, PLAYER_INV_START, HOTBAR_END, true)) return ItemStack.EMPTY
                    slot.onQuickTransfer(stackInSlot, stack)
                }
                SLOT_DISCHARGING_INDEX -> {
                    if (!insertItem(stackInSlot, PLAYER_INV_START, HOTBAR_END, true)) return ItemStack.EMPTY
                    slot.onQuickTransfer(stackInSlot, stack)
                }
                in SLOT_UPGRADE_INDEX_START..SLOT_UPGRADE_INDEX_END -> {
                    if (!insertItem(stackInSlot, PLAYER_INV_START, HOTBAR_END, true)) return ItemStack.EMPTY
                    slot.onQuickTransfer(stackInSlot, stack)
                }
                else -> {
                    if (index in PLAYER_INV_START..HOTBAR_END) {
                        val upgradeTargets = (SLOT_UPGRADE_INDEX_START..SLOT_UPGRADE_INDEX_END).map {
                            SlotTarget(slots[it], upgradeSlotSpec)
                        }
                        val moved = SlotMoveHelper.insertIntoTargets(
                            stackInSlot,
                            listOf(
                                SlotTarget(slots[SLOT_DISCHARGING_INDEX], DISCHARGING_SLOT_SPEC),
                                SlotTarget(slots[SLOT_INPUT_INDEX], INPUT_SLOT_SPEC)
                            ) + upgradeTargets
                        )
                        if (!moved) return ItemStack.EMPTY
                    } else if (!insertItem(stackInSlot, PLAYER_INV_START, HOTBAR_END, false)) {
                        return ItemStack.EMPTY
                    }
                }
            }
            if (stackInSlot.isEmpty) slot.stack = ItemStack.EMPTY
            else slot.markDirty()
            if (stackInSlot.count == stack.count) return ItemStack.EMPTY
            slot.onTakeItem(player, stackInSlot)
        }
        return stack
    }

    override fun canUse(player: PlayerEntity): Boolean =
        context.get({ world, pos ->
            world.getBlockState(pos).block is XxxBlock && player.squaredDistanceTo(
                pos.x + 0.5, pos.y + 0.5, pos.z + 0.5
            ) <= 64.0
        }, true)

    companion object {
        // 槽位位置
        const val INPUT_SLOT_X = 56
        const val INPUT_SLOT_Y = 35
        const val DISCHARGING_SLOT_X = 56
        const val DISCHARGING_SLOT_Y = 59
        const val OUTPUT_SLOT_X = 116
        const val OUTPUT_SLOT_Y = 47
        const val SLOT_SIZE = 18

        // 玩家物品栏
        const val PLAYER_INV_X = 8
        const val PLAYER_INV_Y = 108
        const val HOTBAR_Y = 166

        // 槽位规则
        private val INPUT_SLOT_SPEC = SlotSpec(
            canInsert = { stack -> stack.item !is IBatteryItem }  // 避免电池进入输入槽
        )
        private val DISCHARGING_SLOT_SPEC = SlotSpec(
            maxItemCount = 1,
            canInsert = { stack -> stack.item is IBatteryItem }  // 仅电池可放入
        )
        private val OUTPUT_SLOT_SPEC = SlotSpec(
            canInsert = { false },
            canTake = { true }
        )

        // 槽位索引
        const val SLOT_INPUT_INDEX = 0
        const val SLOT_DISCHARGING_INDEX = 1
        const val SLOT_OUTPUT_INDEX = 2
        const val SLOT_UPGRADE_INDEX_START = 3
        const val SLOT_UPGRADE_INDEX_END = 6
        const val PLAYER_INV_START = 7
        const val HOTBAR_END = 43

        fun fromBuffer(syncId: Int, playerInventory: PlayerInventory, buf: PacketByteBuf): XxxScreenHandler {
            val pos = buf.readBlockPos()
            val propertyCount = buf.readVarInt()
            val context = ScreenHandlerContext.create(playerInventory.player.world, pos)
            val blockInv = SimpleInventory(XxxBlockEntity.INVENTORY_SIZE)
            return XxxScreenHandler(syncId, playerInventory, blockInv, context, ArrayPropertyDelegate(propertyCount))
        }
    }
}
```

---

### 3.6 创建 Screen（客户端 UI）

**文件**：`src/client/kotlin/ic2_120/client/XxxScreen.kt`

```kotlin
package ic2_120.client

import ic2_120.client.compose.*
import ic2_120.client.ui.EnergyBar
import ic2_120.client.ui.GuiBackground
import ic2_120.client.ui.ProgressBar
import ic2_120.content.sync.XxxSync
import ic2_120.content.block.XxxBlock
import ic2_120.content.screen.XxxScreenHandler
import ic2_120.content.screen.slot.UpgradeSlotLayout
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text

@ModScreen(block = XxxBlock::class)
class XxxScreen(
    handler: XxxScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<XxxScreenHandler>(handler, playerInventory, title) {

    private val ui = ComposeUI()

    init {
        backgroundWidth = PANEL_WIDTH
        backgroundHeight = PANEL_HEIGHT
        titleY = 4
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        GuiBackground.draw(context, x, y, backgroundWidth, backgroundHeight)
        GuiBackground.drawPlayerInventorySlotBorders(
            context, x, y,
            XxxScreenHandler.PLAYER_INV_Y,
            XxxScreenHandler.HOTBAR_Y,
            XxxScreenHandler.SLOT_SIZE
        )

        val borderColor = GuiBackground.BORDER_COLOR
        val slotSize = XxxScreenHandler.SLOT_SIZE
        val borderOffset = 1

        // 绘制机器槽位边框
        val inputSlot = handler.slots[XxxScreenHandler.SLOT_INPUT_INDEX]
        val dischargingSlot = handler.slots[XxxScreenHandler.SLOT_DISCHARGING_INDEX]
        val outputSlot = handler.slots[XxxScreenHandler.SLOT_OUTPUT_INDEX]

        context.drawBorder(x + inputSlot.x - borderOffset, y + inputSlot.y - borderOffset, slotSize, slotSize, borderColor)
        context.drawBorder(x + dischargingSlot.x - borderOffset, y + dischargingSlot.y - borderOffset, slotSize, slotSize, borderColor)
        context.drawBorder(x + outputSlot.x - borderOffset, y + outputSlot.y - borderOffset, slotSize, slotSize, borderColor)

        // 绘制升级槽边框
        for (i in XxxScreenHandler.SLOT_UPGRADE_INDEX_START..XxxScreenHandler.SLOT_UPGRADE_INDEX_END) {
            val slot = handler.slots[i]
            context.drawBorder(x + slot.x - borderOffset, y + slot.y - borderOffset, slotSize, slotSize, borderColor)
        }

        // 绘制进度条
        val progress = handler.sync.progress.coerceIn(0, XxxSync.PROGRESS_MAX)
        val progressFrac = if (XxxSync.PROGRESS_MAX > 0) (progress.toFloat() / XxxSync.PROGRESS_MAX).coerceIn(0f, 1f) else 0f
        val barX = x + inputSlot.x + slotSize + 2
        val barW = outputSlot.x - (inputSlot.x + slotSize) - 4
        val barH = 8
        val barY = y + inputSlot.y + (slotSize - barH) / 2
        ProgressBar.draw(context, barX, barY, barW, barH, progressFrac)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(context, mouseX, mouseY, delta)
        val left = x
        val top = y
        val energy = handler.sync.energy.toLong().coerceAtLeast(0)
        val cap = handler.sync.energyCapacity.toLong().coerceAtLeast(1)
        val energyFraction = if (cap > 0) (energy.toFloat() / cap).coerceIn(0f, 1f) else 0f
        val contentW = (backgroundWidth - 16).coerceAtLeast(0)
        val barW = (contentW - 36).coerceAtLeast(0)

        // 在UI左侧绘制速度文本
        val inputRate = handler.sync.getSyncedInsertedAmount()
        val consumeRate = handler.sync.getSyncedConsumedAmount()
        val inputText = "输入 ${formatEu(inputRate)} EU/t"
        val consumeText = "耗能 ${formatEu(consumeRate)} EU/t"
        val inputTextWidth = inputText.length * 6
        val consumeTextWidth = consumeText.length * 6
        val textX = left - maxOf(inputTextWidth, consumeTextWidth) - 4
        context.drawText(textRenderer, inputText, textX, top + 8, 0xAAAAAA, false)
        context.drawText(textRenderer, consumeText, textX, top + 20, 0xAAAAAA, false)

        ui.render(context, textRenderer, mouseX, mouseY) {
            Column(x = left + 8, y = top + 8, spacing = 6) {
                Text(title.string, color = 0xFFFFFF)
                Flex(
                    direction = FlexDirection.ROW,
                    alignItems = AlignItems.CENTER,
                    gap = 8,
                    modifier = Modifier.EMPTY.width(contentW)
                ) {
                    Text("能量", color = 0xAAAAAA)
                    EnergyBar(
                        energyFraction,
                        barWidth = 0,
                        barHeight = 9,
                        modifier = Modifier.EMPTY.width(barW)
                    )
                }
                Text("$energy / $cap EU", color = 0xCCCCCC, shadow = false)
            }
        }
        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean =
        ui.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button)

    companion object {
        private val PANEL_WIDTH = UpgradeSlotLayout.VANILLA_UI_WIDTH + UpgradeSlotLayout.SLOT_SPACING
        private const val PANEL_HEIGHT = 184
    }

    private fun formatEu(value: Long): String {
        return when {
            value >= 1_000_000 -> String.format("%.1fM", value / 1_000_000.0)
            value >= 1_000 -> String.format("%.1fK", value / 1_000.0)
            else -> value.toString()
        }
    }
}
```

---

### 3.7 添加配方系统（可选）

**文件**：`src/main/kotlin/ic2_120/content/recipes/XxxRecipes.kt`

```kotlin
package ic2_120.content.recipes

import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier

object XxxRecipes {
    private val recipes = mapOf<Identifier, ItemStack>(
        // 输入物品 ID -> 输出物品
        Identifier("minecraft", "iron_ore") to ItemStack(Registries.ITEM.get(Identifier("ic2_120", "iron_dust"))),
        Identifier("minecraft", "gold_ore") to ItemStack(Registries.ITEM.get(Identifier("ic2_120", "gold_dust"))),
    )

    fun getOutput(input: ItemStack): ItemStack? {
        if (input.isEmpty) return null
        val itemId = Registries.ITEM.getId(input.item)
        return recipes[itemId]?.copy()
    }
}
```

---

### 3.8 添加资源文件

#### 方块状态（blockstates/xxx.json）

```json
{
  "variants": {
    "facing=north,active=false": { "model": "ic2:block/machine/processing/basic/xxx" },
    "facing=north,active=true":  { "model": "ic2:block/machine/processing/basic/xxx_active" },
    "facing=east,active=false":  { "model": "ic2:block/machine/processing/basic/xxx", "y": 90 },
    "facing=east,active=true":   { "model": "ic2:block/machine/processing/basic/xxx_active", "y": 90 },
    "facing=south,active=false": { "model": "ic2:block/machine/processing/basic/xxx", "y": 180 },
    "facing=south,active=true":  { "model": "ic2:block/machine/processing/basic/xxx_active", "y": 180 },
    "facing=west,active=false":  { "model": "ic2:block/machine/processing/basic/xxx", "y": 270 },
    "facing=west,active=true":   { "model": "ic2:block/machine/processing/basic/xxx_active", "y": 270 }
  }
}
```

#### 物品模型（models/item/xxx.json）

```json
{
  "parent": "ic2:block/machine/processing/basic/xxx"
}
```

#### 翻译（lang/zh_cn.json）

```json
{
  "block.ic2_120.xxx": "粉碎机"
}
```

#### 翻译（lang/en_us.json）

```json
{
  "block.ic2_120.xxx": "Macerator"
}
```

---

### 3.9 编译验证

```bash
# 完整编译验证（必须同时编译客户端和服务端）
./gradlew clean compileKotlin compileClientKotlin
```

**重要**：机器实现完成后，不能只跑 `compileKotlin`，必须同时跑 `compileClientKotlin`，因为机器通常包含 `Screen/Renderer/客户端同步字段`，这些只在客户端源码中编译。

---

## 四、相关文档链接

- **注册系统详解**：`docs/CLASS_BASED_REGISTRY.md`
- **机器实现完整指南**：`docs/machine-implementation-guide.md`
- **同步系统详解**：`docs/sync-system.md`
- **能量系统详解**：`docs/energy-network.md`、`docs/energy-flow-sync.md`
- **升级系统详解**：`docs/upgrade-system.md`
- **槽位规则系统**：`docs/slot-spec-system.md`
- **ComposeUI GUI系统**：`docs/compose-ui.md`
- **DrawContext 绘制方法**：`docs/drawcontext-methods.md`
- **机器能力组合复用**：`docs/machine-composition-reuse.md`
- **流体系统详解**：`docs/fluid-system.md`
- **热能系统详解**：`docs/heat-system.md`
- **核电系统详解**：`docs/nuclear-power.md`
- **Assets 清单**：`docs/assets-inventory.md`

---

## 五、常见问题

### Q: 机器不工作？
- 检查能量是否充足（使用电池或导线供电）
- 检查配方是否正确注册
- 检查输入槽是否有物品
- 检查输出槽是否有空间

### Q: 电池不供电？
- 确保使用 `BatteryDischargerComponent` 而不是 `BatteryChargerComponent`
- 电池槽限制单堆叠（`count > 1` 时设为 `1`）
- 检查 `SLOT_DISCHARGING` 是否正确

### Q: 升级不生效？
- 确保机器实现了升级接口（`IOverclockerUpgradeSupport` 等）
- 确保在 `tick()` 开始时调用了升级组件的 `apply()`
- 检查升级槽索引是否正确

### Q: UI 显示错误？
- 检查 `PANEL_WIDTH` 是否包含升级槽宽度
- 确保槽位索引常量与实际添加顺序一致
- 确保客户端使用 `SyncedDataView` 而不是 `SyncedData`

### Q: 编译错误？
- 确保同时运行 `compileKotlin` 和 `compileClientKotlin`
- 检查所有 `import` 语句是否正确
- 检查 `@ModBlockEntity` 的 `block` 参数是否指向正确的 Block 类
