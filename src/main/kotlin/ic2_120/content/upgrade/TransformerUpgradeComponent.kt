package ic2_120.content.upgrade

import ic2_120.content.item.TransformerUpgrade
import net.minecraft.inventory.Inventory

/**
 * 高压（变压器）升级处理组件。
 *
 * 每个高压升级提高电压等级 1，从而增加 maxInsertPerTick。
 * 等级 1 = 32 EU/t，等级 2 = 128 EU/t，等级 3 = 512 EU/t。
 */
object TransformerUpgradeComponent {

    /** 每个电压等级对应的 maxInsertPerTick（32 * 4^(tier-1)） */
    fun maxInsertForTier(tier: Int): Long {
        if (tier <= 1) return 32L
        var m = 32L
        repeat((tier - 1).coerceAtMost(8)) { m *= 4 }
        return m
    }

    /**
     * 从升级槽统计高压升级数量，并应用到机器。
     */
    fun apply(inventory: Inventory, upgradeSlotIndices: IntArray, machine: Any) {
        if (machine !is ITransformerUpgradeSupport) return

        val count = countUpgrades(inventory, upgradeSlotIndices)
        machine.voltageTierBonus = count
    }

    fun countUpgrades(inventory: Inventory, upgradeSlotIndices: IntArray): Int {
        var count = 0
        for (idx in upgradeSlotIndices) {
            val stack = inventory.getStack(idx)
            if (!stack.isEmpty && stack.item is TransformerUpgrade) {
                count += stack.count
            }
        }
        return count
    }
}
