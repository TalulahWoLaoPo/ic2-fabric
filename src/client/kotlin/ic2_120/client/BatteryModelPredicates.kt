package ic2_120.client

import ic2_120.content.item.energy.IBatteryItem
import net.minecraft.client.item.ModelPredicateProviderRegistry
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier

/**
 * 电池模型 Predicate 注册
 *
 * 为所有电池物品注册 "ic2:charge" predicate，使电池贴图随电量变化。
 */
object BatteryModelPredicates {

    private val CHARGE_ID = Identifier("ic2", "charge")

    /**
     * 注册所有电池物品的模型 predicate
     */
    fun register() {
        // 遍历所有已注册的物品，找到电池物品并注册 predicate
        Registries.ITEM.ids.forEach { id ->
            if (id.namespace == "ic2_120") {
                val item = Registries.ITEM.get(id)
                if (item is IBatteryItem) {
                    registerChargePredicate(item, id)
                }
            }
        }
    }

    /**
     * 为单个电池物品注册电量 predicate
     *
     * @param item 已注册的电池物品
     * @param itemId 物品ID
     */
    private fun registerChargePredicate(item: Item, itemId: Identifier) {
        ModelPredicateProviderRegistry.register(
            item,
            CHARGE_ID
        ) { stack: ItemStack, _, _, _ ->
            // 计算电量比例 (0.0 - 1.0)
            val batteryItem = stack.item
            if (batteryItem is IBatteryItem) {
                val ratio = batteryItem.getChargeRatio(stack)
                ratio.toFloat()
            } else {
                0.0f
            }
        }
    }
}
