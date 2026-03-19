package ic2_120.content.recipes

import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier

/**
 * 固体装罐机配方。
 * 食物装罐：空锡罐 + 食物 -> 罐装食物（锡罐满）
 * 不同食物产出不同数量的满锡罐，部分配方需消耗多份食物。
 *
 * 配料表（食物 -> 满锡罐数）：
 * 牛排8 | 毒马铃薯×2→1 | 曲奇2 | 熟鱼5 | 生鱼2 | 蛋糕12 | 西瓜片2 | 生鸡肉2 | 胡萝卜4 |
 * 生猪排3 | 腐肉×2→1 | 生牛肉3 | 南瓜派6 | 马铃薯1 | 蘑菇煲6 | 苹果4 | 熟猪排8 |
 * 熟鸡肉6 | 面包5 | 烤马铃薯6
 */
object SolidCannerRecipes {

    /** 配方：食物消耗数、锡罐消耗数、满锡罐产出数 */
    data class Recipe(val foodInputCount: Int, val tinCanInputCount: Int, val outputCount: Int)

    private val filledTinCanItem by lazy {
        Registries.ITEM.get(Identifier("ic2_120", "filled_tin_can"))
    }

    private val tinCanItem: Item by lazy {
        Registries.ITEM.get(Identifier("ic2_120", "tin_can"))
    }

    private val recipes: Map<Item, Recipe> by lazy {
        buildMap {
            // 牛排 8
            put(Items.COOKED_BEEF, Recipe(1, 8, 8))
            // 毒马铃薯×2 -> 1
            put(Items.POISONOUS_POTATO, Recipe(2, 1, 1))
            // 曲奇 2
            put(Items.COOKIE, Recipe(1, 2, 2))
            // 熟鱼 5 (鳕鱼、鲑鱼)
            put(Items.COOKED_COD, Recipe(1, 5, 5))
            put(Items.COOKED_SALMON, Recipe(1, 5, 5))
            // 生鱼 2
            put(Items.COD, Recipe(1, 2, 2))
            put(Items.SALMON, Recipe(1, 2, 2))
            put(Items.TROPICAL_FISH, Recipe(1, 2, 2))
            put(Items.PUFFERFISH, Recipe(1, 2, 2))
            // 蛋糕 12
            put(Items.CAKE, Recipe(1, 12, 12))
            // 西瓜片 2
            put(Items.MELON_SLICE, Recipe(1, 2, 2))
            // 生鸡肉 2
            put(Items.CHICKEN, Recipe(1, 2, 2))
            // 胡萝卜 4
            put(Items.CARROT, Recipe(1, 4, 4))
            // 生猪排 3
            put(Items.PORKCHOP, Recipe(1, 3, 3))
            // 腐肉×2 -> 1
            put(Items.ROTTEN_FLESH, Recipe(2, 1, 1))
            // 生牛肉 3
            put(Items.BEEF, Recipe(1, 3, 3))
            // 南瓜派 6
            put(Items.PUMPKIN_PIE, Recipe(1, 6, 6))
            // 马铃薯 1
            put(Items.POTATO, Recipe(1, 1, 1))
            // 蘑菇煲 6
            put(Items.MUSHROOM_STEW, Recipe(1, 6, 6))
            // 苹果 4
            put(Items.APPLE, Recipe(1, 4, 4))
            put(Items.GOLDEN_APPLE, Recipe(1, 4, 4))
            put(Items.ENCHANTED_GOLDEN_APPLE, Recipe(1, 4, 4))
            // 熟猪排 8
            put(Items.COOKED_PORKCHOP, Recipe(1, 8, 8))
            // 熟鸡肉 6
            put(Items.COOKED_CHICKEN, Recipe(1, 6, 6))
            // 面包 5
            put(Items.BREAD, Recipe(1, 5, 5))
            // 烤马铃薯 6
            put(Items.BAKED_POTATO, Recipe(1, 6, 6))
            // 甜菜根（常见食物，按 1 处理）
            put(Items.BEETROOT, Recipe(1, 1, 1))
            put(Items.BEETROOT_SOUP, Recipe(1, 6, 6))
            // 兔肉
            put(Items.RABBIT, Recipe(1, 2, 2))
            put(Items.COOKED_RABBIT, Recipe(1, 5, 5))
            // 兔肉煲
            put(Items.RABBIT_STEW, Recipe(1, 6, 6))
            // 甜浆果
            put(Items.SWEET_BERRIES, Recipe(1, 1, 1))
            // 发光浆果
            put(Items.GLOW_BERRIES, Recipe(1, 1, 1))
            // 蜘蛛眼、河豚等毒物按 1 处理
            put(Items.SPIDER_EYE, Recipe(1, 1, 1))
        }
    }

    /**
     * 获取配方结果。
     * @param tinCanSlot 锡罐槽
     * @param foodSlot 食物槽
     * @return 若匹配则返回 (输出堆叠)，否则 null。调用方需根据 Recipe 消耗对应数量的锡罐和食物。
     */
    fun getRecipe(tinCanSlot: ItemStack, foodSlot: ItemStack): Recipe? {
        if (tinCanSlot.isEmpty || foodSlot.isEmpty) return null
        if (tinCanSlot.item != tinCanItem) return null
        return recipes[foodSlot.item]
    }

    /** 检查物品是否为可装罐食物 */
    fun isCanningFood(item: Item): Boolean = recipes.containsKey(item)

    /**
     * 检查是否有足够材料并返回输出。
     * @return Pair(Recipe, ItemStack)? 若材料不足返回 null
     */
    fun getOutput(tinCanSlot: ItemStack, foodSlot: ItemStack): Pair<Recipe, ItemStack>? {
        val recipe = getRecipe(tinCanSlot, foodSlot) ?: return null
        if (tinCanSlot.count < recipe.tinCanInputCount) return null
        if (foodSlot.count < recipe.foodInputCount) return null
        return recipe to ItemStack(filledTinCanItem, recipe.outputCount)
    }
}
