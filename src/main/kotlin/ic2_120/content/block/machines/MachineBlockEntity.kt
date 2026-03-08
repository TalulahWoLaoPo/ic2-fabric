package ic2_120.content.block.machines

import ic2_120.content.block.ITieredMachine
import ic2_120.content.item.energy.IBatteryItem
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.item.ItemStack
import net.minecraft.util.math.BlockPos

/**
 * 机器 BlockEntity 基类
 *
 * 提供通用的机器功能：
 * - 能量等级（tier）
 * - 电池槽支持（可以用电池为机器供电）
 * - 电池放电逻辑（遵循能量等级规则）
 */
abstract class MachineBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : BlockEntity(type, pos, state), ITieredMachine {

    companion object {
        /** 槽位索引常量 */
        const val FUEL_SLOT = 0      // 燃料槽（子类可使用）
        const val BATTERY_SLOT = 1   // 电池充电/供电槽
    }

    /**
     * 机器的能量等级（1-4）
     * 子类必须覆写此属性
     */
    abstract override val tier: Int

    /**
     * 从电池槽获取能量
     *
     * 此方法会在机器的 tick 方法中调用，自动从电池槽放电并为机器供电。
     * 遵循能量等级规则：只有电池等级 >= 机器等级时才能供电。
     *
     * @param maxAmount 需要的最大能量量
     * @return 实际获得的能量量
     */
    protected fun drawEnergyFromBattery(maxAmount: Long): Long {
        // 确保有 inventory 且有电池槽
        val inventory = getInventory() ?: return 0

        val batteryStack = inventory.getStack(BATTERY_SLOT)
        if (batteryStack.isEmpty || batteryStack.item !is IBatteryItem) {
            return 0
        }

        val battery = batteryStack.item as IBatteryItem

        // 能量等级检查：只有电池等级 >= 机器等级才能供电
        if (battery.tier < tier) {
            // 低级电池无法给高级机器供电
            return 0
        }

        // 检查电池是否有电
        if (battery.isEmpty(batteryStack)) {
            return 0
        }

        // 计算可放电量（考虑传输速度、需求量和电池剩余电量）
        val canDischarge = minOf(
            battery.transferSpeed.toLong(),        // 电池传输速度限制
            maxAmount,                              // 机器需求量
            battery.getCurrentCharge(batteryStack)  // 电池剩余电量
        )

        if (canDischarge <= 0) {
            return 0
        }

        // 执行放电
        val discharged = battery.discharge(batteryStack, canDischarge)
        inventory.setStack(BATTERY_SLOT, batteryStack)
        markDirty()

        return discharged
    }

    /**
     * 检查电池槽是否有可用电池
     *
     * @return 如果电池槽有符合能量等级的电池返回 true
     */
    protected fun hasBatteryAvailable(): Boolean {
        val inventory = getInventory() ?: return false
        val batteryStack = inventory.getStack(BATTERY_SLOT)

        if (batteryStack.isEmpty || batteryStack.item !is IBatteryItem) {
            return false
        }

        val battery = batteryStack.item as IBatteryItem
        // 检查能量等级和剩余电量
        return battery.tier >= tier && !battery.isEmpty(batteryStack)
    }

    /**
     * 获取 inventory
     *
     * 子类需要实现此方法以提供 inventory 访问。
     * 如果机器没有 inventory，返回 null。
     */
    protected abstract fun getInventory(): net.minecraft.inventory.Inventory?
}
