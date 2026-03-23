package ic2_120.integration.jei

import ic2_120.content.recipes.macerator.ModMachineRecipes
import ic2_120.content.recipes.macerator.MaceratorRecipeDatagen
import ic2_120.content.item.armor.JetpackItem
import ic2_120.content.item.energy.IBatteryItem
import ic2_120.content.item.energy.IElectricTool
import mezz.jei.api.IModPlugin
import mezz.jei.api.JeiPlugin
import mezz.jei.api.ingredients.subtypes.IIngredientSubtypeInterpreter
import mezz.jei.api.registration.IExtraIngredientRegistration
import mezz.jei.api.registration.IRecipeCatalystRegistration
import mezz.jei.api.registration.IRecipeCategoryRegistration
import mezz.jei.api.registration.IRecipeRegistration
import mezz.jei.api.registration.ISubtypeRegistration
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.recipe.Ingredient
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier

/**
 * JEI 插件 - 注册电力物品的空电和满电变体
 *
 * 此插件会在 JEI 中显示电力物品的空电和满电版本，
 * 与创造模式物品栏中的显示保持一致。
 */
@JeiPlugin
class Ic2JeiPlugin : IModPlugin {
    override fun getPluginUid(): Identifier {
        return Identifier("ic2_120", "main")
    }

    override fun registerItemSubtypes(registration: ISubtypeRegistration) {
        // 遍历所有已注册的物品
        Registries.ITEM.forEach { item ->
            // 检查物品是否实现了电力接口
            if (item is IBatteryItem || item is IElectricTool) {
                // 注册 NBT 子类型解释器
                registration.registerSubtypeInterpreter(
                    item,
                    ElectricItemSubtypeInterpreter()
                )
            }
            // 检查物品是否为喷气背包
            if (item is JetpackItem) {
                registration.registerSubtypeInterpreter(
                    item,
                    JetpackItemSubtypeInterpreter()
                )
            }
        }
    }

    override fun registerExtraIngredients(registration: IExtraIngredientRegistration) {
        val extraStacks = mutableListOf<ItemStack>()

        Registries.ITEM.forEach { item ->
            when (item) {
                is IBatteryItem -> {
                    // 电池：补充空电 + 满电两个明确变体
                    extraStacks += ItemStack(item as Item).also { stack ->
                        item.setCurrentCharge(stack, 0L)
                    }
                    extraStacks += ItemStack(item as Item).also { stack ->
                        item.setCurrentCharge(stack, item.maxCapacity)
                    }
                }

                is IElectricTool -> {
                    // 电动工具：补充空电 + 满电两个明确变体
                    extraStacks += ItemStack(item as Item).also { stack ->
                        item.setEnergy(stack, 0L)
                    }
                    extraStacks += ItemStack(item as Item).also { stack ->
                        item.setEnergy(stack, item.maxCapacity)
                    }
                }

                is JetpackItem -> {
                    // 喷气背包：补充空燃料 + 满燃料两个明确变体
                    extraStacks += ItemStack(item as Item).also { stack ->
                        JetpackItem.setFuel(stack, 0L)
                    }
                    extraStacks += ItemStack(item as Item).also { stack ->
                        JetpackItem.setFuel(stack, JetpackItem.MAX_FUEL)
                    }
                }
            }
        }

        if (extraStacks.isNotEmpty()) {
            registration.addExtraItemStacks(extraStacks)
        }
    }

    override fun registerCategories(registration: IRecipeCategoryRegistration) {
        registration.addRecipeCategories(MaceratorRecipeCategory(registration.jeiHelpers.guiHelper))
    }

    override fun registerRecipes(registration: IRecipeRegistration) {
        val recipes = MaceratorRecipeDatagen.allEntries()
            .map { entry ->
                MaceratorJeiRecipe(
                    Ingredient.ofItems(entry.input),
                    ItemStack(entry.output, entry.count)
                )
            }
        registration.addRecipes(Ic2JeiRecipeTypes.MACERATOR, recipes)
    }

    override fun registerRecipeCatalysts(registration: IRecipeCatalystRegistration) {
        registration.addRecipeCatalyst(
            ItemStack(Registries.ITEM.get(Identifier("ic2_120", "macerator"))),
            Ic2JeiRecipeTypes.MACERATOR
        )
    }

    /**
     * 电力物品 NBT 子类型解释器
     *
     * 使用 "Energy" NBT 标签来区分不同电量状态的物品。
     * JEI 会根据此解释器识别不同的 ItemStack 为独立的物品。
     */
    class ElectricItemSubtypeInterpreter : IIngredientSubtypeInterpreter<ItemStack> {
        companion object {
            /** 子类型标识符：空电版本 */
            private const val EMPTY_TAG = "empty"

            /** 子类型标识符：满电版本 */
            private const val FULL_TAG = "full"

            /** 子类型标识符：部分电量 */
            private const val PARTIAL_TAG = "partial"
        }

        override fun apply(itemStack: ItemStack, uidContext: mezz.jei.api.ingredients.subtypes.UidContext): String {
            // 通过接口读取电量，避免依赖固定 NBT 键名
            val maxCapacity = when (val item = itemStack.item) {
                is IBatteryItem -> item.maxCapacity
                is IElectricTool -> item.maxCapacity
                else -> 1L
            }
            val energy = when (val item = itemStack.item) {
                is IBatteryItem -> item.getCurrentCharge(itemStack)
                is IElectricTool -> item.getEnergy(itemStack)
                else -> return ""
            }

            // 根据电量比例返回子类型标识符
            return when {
                energy <= 0 -> EMPTY_TAG
                energy >= maxCapacity -> FULL_TAG
                else -> "$PARTIAL_TAG:$energy"
            }
        }
    }

    /**
     * 喷气背包 NBT 子类型解释器
     *
     * 使用 "Fuel" NBT 标签来区分不同燃料状态的物品。
     * JEI 会根据此解释器识别不同的 ItemStack 为独立的物品。
     */
    class JetpackItemSubtypeInterpreter : IIngredientSubtypeInterpreter<ItemStack> {
        companion object {
            /** 子类型标识符：空燃料版本 */
            private const val EMPTY_TAG = "empty"

            /** 子类型标识符：满燃料版本 */
            private const val FULL_TAG = "full"

            /** 子类型标识符：部分燃料 */
            private const val PARTIAL_TAG = "partial"
        }

        override fun apply(itemStack: ItemStack, uidContext: mezz.jei.api.ingredients.subtypes.UidContext): String {
            val fuel = JetpackItem.getFuel(itemStack)

            // 根据燃料比例返回子类型标识符
            return when {
                fuel <= 0 -> EMPTY_TAG
                fuel >= JetpackItem.MAX_FUEL -> FULL_TAG
                else -> "$PARTIAL_TAG:$fuel"
            }
        }
    }
}
