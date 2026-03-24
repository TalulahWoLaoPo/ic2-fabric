package ic2_120.content.item

import ic2_120.registry.CreativeTab
import ic2_120.registry.instance
import ic2_120.registry.recipeId
import ic2_120.registry.annotation.ModItem
import ic2_120.content.block.LeadBlock
import ic2_120.content.block.SilverBlock
import ic2_120.content.block.SteelBlock
import ic2_120.content.block.UraniumBlock
import net.minecraft.data.server.recipe.ShapelessRecipeJsonBuilder
import net.minecraft.data.server.recipe.RecipeJsonProvider
import net.minecraft.item.Item
import net.fabricmc.fabric.api.item.v1.FabricItemSettings
import net.minecraft.recipe.book.RecipeCategory
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
import java.util.function.Consumer

// ========== 锭（已存在：copper_ingot, tin_ingot, bronze_ingot 在 MetalItems.kt） ==========

/** 合金锭 */
@ModItem(name = "mixed_metal_ingot", tab = CreativeTab.IC2_MATERIALS, group = "ingots")
class MixedMetalIngot : Item(FabricItemSettings())

/** 铅锭 */
@ModItem(name = "lead_ingot", tab = CreativeTab.IC2_MATERIALS, group = "ingots")
class LeadIngot : Item(FabricItemSettings()) {
    companion object {
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, LeadIngot::class.instance(), 1)
                .apply { repeat(9) { input(LeadBlock::class.instance()) } }
                .criterion(hasItem(LeadBlock::class.instance()), conditionsFromItem(LeadBlock::class.instance()))
                .offerTo(exporter, LeadIngot::class.recipeId("from_block"))

            ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, LeadBlock::class.instance(), 1)
                .input(LeadIngot::class.instance())
                .criterion(hasItem(LeadIngot::class.instance()), conditionsFromItem(LeadIngot::class.instance()))
                .offerTo(exporter, LeadBlock::class.recipeId("from_ingot"))
        }
    }
}

/** 银锭 */
@ModItem(name = "silver_ingot", tab = CreativeTab.IC2_MATERIALS, group = "ingots")
class SilverIngot : Item(FabricItemSettings()) {
    companion object {
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, SilverIngot::class.instance(), 1)
                .apply { repeat(9) { input(SilverBlock::class.instance()) } }
                .criterion(hasItem(SilverBlock::class.instance()), conditionsFromItem(SilverBlock::class.instance()))
                .offerTo(exporter, SilverIngot::class.recipeId("from_block"))

            ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, SilverBlock::class.instance(), 1)
                .input(SilverIngot::class.instance())
                .criterion(hasItem(SilverIngot::class.instance()), conditionsFromItem(SilverIngot::class.instance()))
                .offerTo(exporter, SilverBlock::class.recipeId("from_ingot"))
        }
    }
}

/** 钢锭 */
@ModItem(name = "steel_ingot", tab = CreativeTab.IC2_MATERIALS, group = "ingots")
class SteelIngot : Item(FabricItemSettings()) {
    companion object {
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, SteelIngot::class.instance(), 1)
                .apply { repeat(9) { input(SteelBlock::class.instance()) } }
                .criterion(hasItem(SteelBlock::class.instance()), conditionsFromItem(SteelBlock::class.instance()))
                .offerTo(exporter, SteelIngot::class.recipeId("from_block"))

            ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, SteelBlock::class.instance(), 1)
                .input(SteelIngot::class.instance())
                .criterion(hasItem(SteelIngot::class.instance()), conditionsFromItem(SteelIngot::class.instance()))
                .offerTo(exporter, SteelBlock::class.recipeId("from_ingot"))
        }
    }
}

/** 精炼铁锭 */
@ModItem(name = "refined_iron_ingot", tab = CreativeTab.IC2_MATERIALS, group = "ingots")
class RefinedIronIngot : Item(FabricItemSettings())

/** 铀锭 */
@ModItem(name = "uranium_ingot", tab = CreativeTab.IC2_MATERIALS, group = "ingots")
class UraniumIngot : Item(FabricItemSettings()) {
    companion object {
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, UraniumIngot::class.instance(), 1)
                .apply { repeat(9) { input(UraniumBlock::class.instance()) } }
                .criterion(hasItem(UraniumBlock::class.instance()), conditionsFromItem(UraniumBlock::class.instance()))
                .offerTo(exporter, UraniumIngot::class.recipeId("from_block"))

            ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, UraniumBlock::class.instance(), 1)
                .input(UraniumIngot::class.instance())
                .criterion(hasItem(UraniumIngot::class.instance()), conditionsFromItem(UraniumIngot::class.instance()))
                .offerTo(exporter, UraniumBlock::class.recipeId("from_ingot"))
        }
    }
}

// ========== 粗金属（raw，冶炼粗矿得） ==========

/** 粗铅 */
@ModItem(name = "raw_lead", tab = CreativeTab.IC2_MATERIALS, group = "raw_metals")
class RawLead : Item(FabricItemSettings())

/** 粗锡 */
@ModItem(name = "raw_tin", tab = CreativeTab.IC2_MATERIALS, group = "raw_metals")
class RawTin : Item(FabricItemSettings())

/** 粗铀 */
@ModItem(name = "raw_uranium", tab = CreativeTab.IC2_MATERIALS, group = "raw_metals")
class RawUranium : Item(FabricItemSettings())

// ========== 粉碎矿石（打粉机产物） ==========

@ModItem(name = "crushed_copper", tab = CreativeTab.IC2_MATERIALS, group = "crushed_ore")
class CrushedCopper : Item(FabricItemSettings())

@ModItem(name = "crushed_gold", tab = CreativeTab.IC2_MATERIALS, group = "crushed_ore")
class CrushedGold : Item(FabricItemSettings())

@ModItem(name = "crushed_iron", tab = CreativeTab.IC2_MATERIALS, group = "crushed_ore")
class CrushedIron : Item(FabricItemSettings())

@ModItem(name = "crushed_lead", tab = CreativeTab.IC2_MATERIALS, group = "crushed_ore")
class CrushedLead : Item(FabricItemSettings())

@ModItem(name = "crushed_silver", tab = CreativeTab.IC2_MATERIALS, group = "crushed_ore")
class CrushedSilver : Item(FabricItemSettings())

@ModItem(name = "crushed_tin", tab = CreativeTab.IC2_MATERIALS, group = "crushed_ore")
class CrushedTin : Item(FabricItemSettings())

@ModItem(name = "crushed_uranium", tab = CreativeTab.IC2_MATERIALS, group = "crushed_ore")
class CrushedUranium : Item(FabricItemSettings())

// ========== 纯净的粉碎矿石（洗矿机产物） ==========

@ModItem(name = "purified_copper", tab = CreativeTab.IC2_MATERIALS, group = "purified_ore")
class PurifiedCopper : Item(FabricItemSettings())

@ModItem(name = "purified_gold", tab = CreativeTab.IC2_MATERIALS, group = "purified_ore")
class PurifiedGold : Item(FabricItemSettings())

@ModItem(name = "purified_iron", tab = CreativeTab.IC2_MATERIALS, group = "purified_ore")
class PurifiedIron : Item(FabricItemSettings())

@ModItem(name = "purified_lead", tab = CreativeTab.IC2_MATERIALS, group = "purified_ore")
class PurifiedLead : Item(FabricItemSettings())

@ModItem(name = "purified_silver", tab = CreativeTab.IC2_MATERIALS, group = "purified_ore")
class PurifiedSilver : Item(FabricItemSettings())

@ModItem(name = "purified_tin", tab = CreativeTab.IC2_MATERIALS, group = "purified_ore")
class PurifiedTin : Item(FabricItemSettings())

@ModItem(name = "purified_uranium", tab = CreativeTab.IC2_MATERIALS, group = "purified_ore")
class PurifiedUranium : Item(FabricItemSettings())
