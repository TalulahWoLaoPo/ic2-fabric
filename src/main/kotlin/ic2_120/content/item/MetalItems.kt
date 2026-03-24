package ic2_120.content.item

import ic2_120.registry.CreativeTab
import ic2_120.registry.instance
import ic2_120.registry.recipeId
import ic2_120.registry.annotation.ModItem
import ic2_120.content.block.BronzeBlock
import ic2_120.content.block.TinBlock
import net.minecraft.data.server.recipe.ShapelessRecipeJsonBuilder
import net.minecraft.data.server.recipe.RecipeJsonProvider
import net.minecraft.item.Item
import net.fabricmc.fabric.api.item.v1.FabricItemSettings
import net.minecraft.recipe.book.RecipeCategory
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
import java.util.function.Consumer

// 铜锭：使用原版 minecraft:copper_ingot，此处不再注册

/**
 * 锡锭。
 */
@ModItem(name = "tin_ingot", tab = CreativeTab.IC2_MATERIALS, group = "ingots")
class TinIngot : Item(FabricItemSettings()) {
    companion object {
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, TinIngot::class.instance(), 1)
                .apply { repeat(9) { input(TinBlock::class.instance()) } }
                .criterion(hasItem(TinBlock::class.instance()), conditionsFromItem(TinBlock::class.instance()))
                .offerTo(exporter, TinIngot::class.recipeId("from_block"))

            ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, TinBlock::class.instance(), 1)
                .input(TinIngot::class.instance())
                .criterion(hasItem(TinIngot::class.instance()), conditionsFromItem(TinIngot::class.instance()))
                .offerTo(exporter, TinBlock::class.recipeId("from_ingot"))
        }
    }
}

/**
 * 青铜锭。
 */
@ModItem(name = "bronze_ingot", tab = CreativeTab.IC2_MATERIALS, group = "ingots")
class BronzeIngot : Item(FabricItemSettings()) {
    companion object {
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, BronzeIngot::class.instance(), 1)
                .apply { repeat(9) { input(BronzeBlock::class.instance()) } }
                .criterion(hasItem(BronzeBlock::class.instance()), conditionsFromItem(BronzeBlock::class.instance()))
                .offerTo(exporter, BronzeIngot::class.recipeId("from_block"))

            ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, BronzeBlock::class.instance(), 1)
                .input(BronzeIngot::class.instance())
                .criterion(hasItem(BronzeIngot::class.instance()), conditionsFromItem(BronzeIngot::class.instance()))
                .offerTo(exporter, BronzeBlock::class.recipeId("from_ingot"))
        }
    }
}

/**
 * 橡胶。
 */
@ModItem(name = "rubber", tab = CreativeTab.IC2_MATERIALS, group = "materials")
class RubberItem : Item(FabricItemSettings())

