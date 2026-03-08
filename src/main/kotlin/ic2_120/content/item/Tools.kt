package ic2_120.content.item

import ic2_120.Ic2_120
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModItem
import net.fabricmc.fabric.api.item.v1.FabricItemSettings
import net.minecraft.item.AxeItem
import net.minecraft.item.HoeItem
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.PickaxeItem
import net.minecraft.item.ShovelItem
import net.minecraft.item.SwordItem
import net.minecraft.item.ToolMaterial
import net.minecraft.recipe.Ingredient
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier

// ========== 工具材料 ==========

/** 青铜工具材料：耐久与挖掘等级同铁，挖掘速度同石头，可用青铜锭修复 */
object BronzeToolMaterial : ToolMaterial {
    override fun getDurability() = 250
    override fun getMiningSpeedMultiplier() = 4.0f
    override fun getAttackDamage() = 2.0f
    override fun getMiningLevel() = 2
    override fun getEnchantability() = 10
    override fun getRepairIngredient(): Ingredient =
        Ingredient.ofItems(Registries.ITEM.get(Identifier(Ic2_120.MOD_ID, "bronze_ingot")))
}

// ========== 工具类 ==========

/** 锻造锤 - 将锭锻造成板，将板锻造成外壳 */
@ModItem(name = "forge_hammer", tab = CreativeTab.IC2_TOOLS)
class ForgeHammer : Item(FabricItemSettings().maxDamage(80)) {
    override fun getRecipeRemainder(stack: ItemStack): ItemStack {
        val result = stack.copy()
        if (result.damage < result.maxDamage - 1) {
            result.damage += 1
            return result
        }
        return ItemStack.EMPTY
    }
}

/** 板材切割剪刀 - 将板材切割成导线 */
@ModItem(name = "cutter", tab = CreativeTab.IC2_TOOLS)
class Cutter : Item(FabricItemSettings().maxDamage(60)) {
    override fun getRecipeRemainder(stack: ItemStack): ItemStack {
        val result = stack.copy()
        if (result.damage < result.maxDamage - 1) {
            result.damage += 1
            return result
        }
        return ItemStack.EMPTY
    }
}

// ========== 青铜工具 ==========

@ModItem(name = "bronze_axe", tab = CreativeTab.IC2_TOOLS, group = "bronze_tools")
class BronzeAxe : AxeItem(BronzeToolMaterial, 5f, -3f, FabricItemSettings())

@ModItem(name = "bronze_hoe", tab = CreativeTab.IC2_TOOLS, group = "bronze_tools")
class BronzeHoe : HoeItem(BronzeToolMaterial, -1, 0f, FabricItemSettings())

@ModItem(name = "bronze_sword", tab = CreativeTab.IC2_TOOLS, group = "bronze_tools")
class BronzeSword : SwordItem(BronzeToolMaterial, 3, -2.4f, FabricItemSettings())

@ModItem(name = "bronze_shovel", tab = CreativeTab.IC2_TOOLS, group = "bronze_tools")
class BronzeShovel : ShovelItem(BronzeToolMaterial, 1.5f, -3f, FabricItemSettings())

@ModItem(name = "bronze_pickaxe", tab = CreativeTab.IC2_TOOLS, group = "bronze_tools")
class BronzePickaxe : PickaxeItem(BronzeToolMaterial, 1, -2.8f, FabricItemSettings())
