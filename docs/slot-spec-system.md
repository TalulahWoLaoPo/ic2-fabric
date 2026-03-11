# SlotSpec 槽位规则系统

SlotSpec 系统是一个用于定义槽位规则的声明式系统，用于控制物品放入、取出和堆叠限制。

## 核心组件

### 1. SlotSpec

`SlotSpec` 是一个数据类，定义了槽位的基本规则：

```kotlin
data class SlotSpec(
    val maxItemCount: Int = 64,
    val canInsert: (ItemStack) -> Boolean = { true },
    val canTake: (PlayerEntity) -> Boolean = { true }
)
```

**参数说明**：
- `maxItemCount` - 槽位最大堆叠数量（默认 64）
- `canInsert` - 是否允许放入物品的判断函数
- `canTake` - 是否允许取出物品的判断函数

### 2. PredicateSlot

`PredicateSlot` 继承自 Minecraft 的 `Slot` 类，使用 `SlotSpec` 来控制槽位行为：

```kotlin
class PredicateSlot(
    inventory: Inventory,
    index: Int,
    x: Int,
    y: Int,
    private val spec: SlotSpec
) : Slot(inventory, index, x, y) {

    override fun canInsert(stack: ItemStack): Boolean = spec.canInsert(stack)

    override fun getMaxItemCount(stack: ItemStack): Int = spec.maxItemCount

    override fun canTakeItems(playerEntity: PlayerEntity): Boolean = spec.canTake(playerEntity)
}
```

### 3. SlotMoveHelper

`SlotMoveHelper` 提供了基于槽位规则的快速移动功能，用于 `quickMove` 操作：

```kotlin
object SlotMoveHelper {
    fun insertIntoTargets(stack: ItemStack, targets: List<SlotTarget>): Boolean
    
    private fun insertIntoSingleTarget(stack: ItemStack, target: SlotTarget): Boolean
}

data class SlotTarget(
    val slot: Slot,
    val spec: SlotSpec
)
```

## 使用示例

### 基础槽位规则

```kotlin
// 允许放入任意物品，最大堆叠 64
val BASIC_SLOT_SPEC = SlotSpec()

// 只允许放入特定物品类型
val FUEL_SLOT_SPEC = SlotSpec(
    canInsert = { stack -> stack.item is IBatteryItem }
)

// 限制堆叠数量为 1
val SINGLE_ITEM_SLOT_SPEC = SlotSpec(
    maxItemCount = 1,
    canInsert = { stack -> stack.item is IUpgradeItem }
)

// 禁止放入，但允许取出（输出槽）
val OUTPUT_SLOT_SPEC = SlotSpec(
    canInsert = { false },
    canTake = { true }
)
```

### 在 ScreenHandler 中使用

```kotlin
class MyScreenHandler(...) : ScreenHandler(...) {
    
    companion object {
        private val INPUT_SLOT_SPEC = SlotSpec(
            canInsert = { stack -> stack.item !is IBatteryItem }
        )
        
        private val DISCHARGING_SLOT_SPEC = SlotSpec(
            maxItemCount = 1,
            canInsert = { stack -> stack.item is IBatteryItem }
        )
        
        private val OUTPUT_SLOT_SPEC = SlotSpec(
            canInsert = { false },
            canTake = { true }
        )
    }
    
    init {
        addSlot(PredicateSlot(blockInventory, SLOT_INPUT, INPUT_X, INPUT_Y, INPUT_SLOT_SPEC))
        addSlot(PredicateSlot(blockInventory, SLOT_DISCHARGING, DISCHARGING_X, DISCHARGING_Y, DISCHARGING_SLOT_SPEC))
        addSlot(PredicateSlot(blockInventory, SLOT_OUTPUT, OUTPUT_X, OUTPUT_Y, OUTPUT_SLOT_SPEC))
    }
}
```

### 处理快速移动（Shift+点击）

**重要**：当槽位有堆叠限制时，必须使用 `SlotMoveHelper.insertIntoTargets` 而不是原版的 `insertItem` 方法。

#### 错误示例（使用原版 insertItem）

```kotlin
override fun quickMove(player: PlayerEntity, index: Int): ItemStack {
    var stack = ItemStack.EMPTY
    val slot = slots[index]
    if (slot.hasStack()) {
        val stackInSlot = slot.stack
        stack = stackInSlot.copy()
        when {
            index < machineSlotCount -> {
                if (!insertItem(stackInSlot, machineSlotCount, slots.size, true)) return ItemStack.EMPTY
            }
            else -> {
                // ❌ 错误：原版 insertItem 不会遵守 SlotSpec 的 maxItemCount 限制
                if (!insertItem(stackInSlot, 0, machineSlotCount, false)) return ItemStack.EMPTY
            }
        }
        // ...
    }
    return stack
}
```

#### 正确示例（使用 SlotMoveHelper）

```kotlin
override fun quickMove(player: PlayerEntity, index: Int): ItemStack {
    var stack = ItemStack.EMPTY
    val slot = slots[index]
    if (slot.hasStack()) {
        val stackInSlot = slot.stack
        stack = stackInSlot.copy()
        when {
            index < machineSlotCount -> {
                if (!insertItem(stackInSlot, machineSlotCount, slots.size, true)) return ItemStack.EMPTY
                slot.onQuickTransfer(stackInSlot, stack)
            }
            else -> {
                val machineTargets = (0 until machineSlotCount).map {
                    SlotTarget(slots[it], MACHINE_SLOT_SPEC)
                }
                val moved = SlotMoveHelper.insertIntoTargets(stackInSlot, machineTargets)
                if (!moved) {
                    return ItemStack.EMPTY
                }
            }
        }
        // ...
    }
    return stack
}
```

## 工作原理

### SlotMoveHelper.insertIntoTargets

该方法按顺序尝试将物品放入目标槽位列表：

1. 遍历每个目标槽位
2. 检查槽位是否可以放入物品（`canInsert`）
3. 计算实际可放入的数量（考虑 `maxItemCount`、物品本身的 `maxCount` 和槽位当前数量）
4. 如果槽位为空，放入物品
5. 如果槽位已有相同物品，尝试堆叠
6. 返回是否成功移动了至少一个物品

### 关键代码逻辑

```kotlin
private fun insertIntoSingleTarget(stack: ItemStack, target: SlotTarget): Boolean {
    val slot = target.slot
    if (!slot.canInsert(stack)) return false

    val slotStack = slot.stack
    val slotLimit = minOf(target.spec.maxItemCount, slot.maxItemCount, stack.maxCount)
    if (slotLimit <= 0) return false

    if (slotStack.isEmpty) {
        val moveCount = minOf(slotLimit, stack.count)
        if (moveCount <= 0) return false
        val moved = stack.copy()
        moved.count = moveCount
        slot.stack = moved
        stack.decrement(moveCount)
        slot.markDirty()
        return true
    }

    if (!ItemStack.canCombine(slotStack, stack)) return false

    val space = (slotLimit - slotStack.count).coerceAtLeast(0)
    if (space <= 0) return false

    val moveCount = minOf(space, stack.count)
    if (moveCount <= 0) return false
    slotStack.increment(moveCount)
    stack.decrement(moveCount)
    slot.markDirty()
    return true
}
```

## 实际案例

### 核反应堆槽位

核反应堆的每个槽位只能放 1 个反应堆组件：

```kotlin
private val REACTOR_SLOT_SPEC = SlotSpec(
    maxItemCount = 1,
    canInsert = { stack -> !stack.isEmpty && stack.item is IBaseReactorComponent }
)

override fun quickMove(player: PlayerEntity, index: Int): ItemStack {
    var stack = ItemStack.EMPTY
    val slot = slots[index]
    if (slot.hasStack()) {
        val stackInSlot = slot.stack
        stack = stackInSlot.copy()
        when {
            index < reactorSlotCount -> {
                if (!insertItem(stackInSlot, reactorSlotCount, slots.size, true)) return ItemStack.EMPTY
                slot.onQuickTransfer(stackInSlot, stack)
            }
            else -> {
                val reactorTargets = (0 until reactorSlotCount).map {
                    SlotTarget(slots[it], REACTOR_SLOT_SPEC)
                }
                val moved = SlotMoveHelper.insertIntoTargets(stackInSlot, reactorTargets)
                if (!moved) {
                    return ItemStack.EMPTY
                }
            }
        }
        // ...
    }
    return stack
}
```

### 金属成型机槽位

```kotlin
private val INPUT_SLOT_SPEC = SlotSpec(
    canInsert = { stack -> stack.item !is IBatteryItem }
)

private val DISCHARGING_SLOT_SPEC = SlotSpec(
    maxItemCount = 1,
    canInsert = { stack -> stack.item is IBatteryItem }
)

private val OUTPUT_SLOT_SPEC = SlotSpec(
    canInsert = { false },
    canTake = { true }
)

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
            else -> {
                if (index in PLAYER_INV_START..HOTBAR_END) {
                    val moved = SlotMoveHelper.insertIntoTargets(
                        stackInSlot,
                        listOf(
                            SlotTarget(slots[SLOT_DISCHARGING_INDEX], DISCHARGING_SLOT_SPEC),
                            SlotTarget(slots[SLOT_INPUT_INDEX], INPUT_SLOT_SPEC)
                        )
                    )
                    if (!moved) {
                        return ItemStack.EMPTY
                    }
                } else if (!insertItem(stackInSlot, PLAYER_INV_START, HOTBAR_END, false)) {
                    return ItemStack.EMPTY
                }
            }
        }
        // ...
    }
    return stack
}
```

## 注意事项

1. **必须使用 SlotMoveHelper**：当槽位有 `maxItemCount` 限制时，必须使用 `SlotMoveHelper.insertIntoTargets` 来处理快速移动，否则限制不会生效。

2. **槽位顺序**：`SlotMoveHelper.insertIntoTargets` 按照目标列表的顺序尝试放入，可以控制物品的优先放置位置。

3. **onQuickTransfer**：从机器槽位移出到玩家物品栏时，需要调用 `slot.onQuickTransfer(stackInSlot, stack)` 来触发槽位的快速转移逻辑。

4. **onTakeItem**：最后需要调用 `slot.onTakeItem(player, stackInSlot)` 来触发槽位的物品取出逻辑。

5. **返回值**：`quickMove` 方法应该返回实际移动的物品堆，如果没有移动任何物品则返回 `ItemStack.EMPTY`。

## 相关文件

- `src/main/kotlin/ic2_120/content/screen/slot/SlotSpec.kt` - SlotSpec 定义
- `src/main/kotlin/ic2_120/content/screen/slot/PredicateSlot.kt` - PredicateSlot 实现
- `src/main/kotlin/ic2_120/content/screen/slot/SlotMoveHelper.kt` - 快速移动工具
- `src/main/kotlin/ic2_120/content/screen/slot/UpgradeSlotLayout.kt` - 升级槽位布局示例
