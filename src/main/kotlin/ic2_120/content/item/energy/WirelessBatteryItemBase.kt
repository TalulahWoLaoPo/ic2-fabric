package ic2_120.content.item.energy

import net.fabricmc.fabric.api.item.v1.FabricItemSettings

/**
 * 无线充电电池基类
 *
 * 继承自 BatteryItemBase，增加无线充电功能。
 * 容量为同等级普通电池的4倍，速度不变。
 *
 * @param name 物品 ID
 * @param tier 能量等级
 * @param baseMaxCapacity 同等级普通电池的最大容量
 * @param transferSpeed 充放电速度
 */
abstract class WirelessBatteryItemBase(
    name: String,
    tier: Int,
    baseMaxCapacity: Long,
    transferSpeed: Int
) : BatteryItemBase(
    name = name,
    tier = tier,
    maxCapacity = baseMaxCapacity * 4, // 容量是普通版本的4倍
    transferSpeed = transferSpeed,
    canChargeWireless = true // 支持无线充电
) {
    /**
     * 给玩家物品栏中的装备充电
     *
     * TODO: 待实现可充电装备系统后再完善此功能
     *
     * @param player 玩家
     * @param amount 尝试充电的电量
     * @return 实际充入的电量
     */
    fun chargeEquipment(
        player: net.minecraft.entity.player.PlayerEntity,
        amount: Long
    ): Long {
        var remaining = amount
        var charged = 0L

        // 遍历玩家物品栏（除了当前电池所在的槽位）
        val inventory = player.inventory
        for (i in 0 until inventory.size()) {
            if (remaining <= 0) break

            val stack = inventory.getStack(i)
            if (stack.isEmpty) continue
            if (stack.item === this) continue // 跳过自己

            // 检查是否为可充电装备
            if (stack.item is IBatteryItem) {
                val battery = stack.item as IBatteryItem
                val transferred = battery.charge(stack, remaining)
                charged += transferred
                remaining -= transferred
            }

            // TODO: 添加更多可充电装备类型（如电动工具、量子盔甲等）
        }

        return charged
    }

    /**
     * 每秒自动给装备充电
     *
     * TODO: 在物品 tick 系统中调用此方法
     */
    fun autoChargeEquipment(stack: net.minecraft.item.ItemStack, player: net.minecraft.entity.player.PlayerEntity) {
        if (!canChargeWireless) return

        val energy = getCurrentCharge(stack)
        if (energy <= 0) return

        // 每次尝试充电 transferSpeed 的电量
        val charged = chargeEquipment(player, transferSpeed.toLong())
        if (charged > 0) {
            discharge(stack, charged)
        }
    }
}
