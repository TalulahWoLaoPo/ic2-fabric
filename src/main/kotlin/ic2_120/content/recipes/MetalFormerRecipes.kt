package ic2_120.content.recipes

import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier

/**
 * 金属成型机配方：输入物品 -> 输出物品及数量。
 * 单输入单输出，每次消耗 1 个输入。
 */
object MetalFormerRecipes {

    private val cache = mutableMapOf<Item, ItemStack>()

    init {
        fun stack(id: String, count: Int): ItemStack {
            val ident = Identifier.tryParse(id) ?: return ItemStack.EMPTY
            return ItemStack(Registries.ITEM.get(ident), count)
        }
        // 锭 -> 板（切割成型）
        add("ic2_120:copper_ingot", stack("ic2_120:copper_plate", 1))
        add("ic2_120:tin_ingot", stack("ic2_120:tin_plate", 1))
        add("ic2_120:bronze_ingot", stack("ic2_120:bronze_plate", 1))
        add("minecraft:iron_ingot", stack("ic2_120:iron_plate", 1))
        add("minecraft:gold_ingot", stack("ic2_120:gold_plate", 1))
        add("ic2_120:lead_ingot", stack("ic2_120:lead_plate", 1))
        add("ic2_120:steel_ingot", stack("ic2_120:steel_plate", 1))
        add("minecraft:lapis_lazuli", stack("ic2_120:lapis_plate", 1))
    }

    private fun add(inputId: String, output: ItemStack) {
        val item = Registries.ITEM.get(Identifier.tryParse(inputId) ?: return)
        if (output.isEmpty) return
        cache[item] = output
    }

    fun getOutput(input: ItemStack): ItemStack? {
        if (input.isEmpty) return null
        return cache[input.item]?.copy()
    }
}
