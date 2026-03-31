package ic2_120.content.item

import ic2_120.content.entity.ModEntities
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.CreativeTab
import ic2_120.registry.id
import ic2_120.registry.instance
import ic2_120.registry.recipeId
import ic2_120.registry.type
import ic2_120.registry.annotation.ModItem
import ic2_120.registry.type
import net.fabricmc.fabric.api.item.v1.FabricItemSettings
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
import net.minecraft.data.server.recipe.RecipeJsonProvider
import net.minecraft.data.server.recipe.ShapelessRecipeJsonBuilder
import net.minecraft.recipe.book.RecipeCategory
import java.util.function.Consumer

/** 破损的橡胶船 - 使用后在水面生成可乘坐的船实体 */
@ModItem(name = "broken_rubber_boat", tab = CreativeTab.IC2_TOOLS, group = "boats")
class BrokenRubberBoatItem : Ic2BoatItem(ModEntities.BROKEN_RUBBER_BOAT, FabricItemSettings()) {
    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            ShapelessRecipeJsonBuilder.create(RecipeCategory.TRANSPORTATION, RubberBoatItem::class.instance(), 1)
                .input(BrokenRubberBoatItem::class.instance())
                .input(RubberItem::class.instance())
                .criterion(hasItem(BrokenRubberBoatItem::class.instance()), conditionsFromItem(BrokenRubberBoatItem::class.instance()))
                .offerTo(exporter, BrokenRubberBoatItem::class.recipeId("repair_to_rubber_boat"))
        }
    }
}

/** 碳纤维船 - 使用后在水面生成可乘坐的船实体 */
@ModItem(name = "carbon_boat", tab = CreativeTab.IC2_TOOLS, group = "boats")
class CarbonBoatItem : Ic2BoatItem(ModEntities.CARBON_BOAT, FabricItemSettings())

/** 橡皮艇 - 使用后在水面生成可乘坐的船实体 */
@ModItem(name = "rubber_boat", tab = CreativeTab.IC2_TOOLS, group = "boats")
class RubberBoatItem : Ic2BoatItem(ModEntities.RUBBER_BOAT, FabricItemSettings())

/** 电动艇 - 使用后在水面生成可乘坐的船实体 */
@ModItem(name = "electric_boat", tab = CreativeTab.IC2_TOOLS, group = "boats")
class ElectricBoatItem : Ic2BoatItem(ModEntities.ELECTRIC_BOAT, FabricItemSettings())
