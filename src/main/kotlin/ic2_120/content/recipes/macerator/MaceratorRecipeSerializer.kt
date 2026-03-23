package ic2_120.content.recipes.macerator

import com.google.gson.JsonObject
import net.minecraft.item.ItemStack
import net.minecraft.network.PacketByteBuf
import net.minecraft.recipe.Ingredient
import net.minecraft.recipe.RecipeSerializer
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier
import net.minecraft.util.JsonHelper

object MaceratorRecipeSerializer : RecipeSerializer<MaceratorRecipe> {
    override fun read(id: Identifier, json: JsonObject): MaceratorRecipe {
        val ingredient = Ingredient.fromJson(JsonHelper.getObject(json, "ingredient"))
        val result = JsonHelper.getObject(json, "result")
        val itemId = Identifier(JsonHelper.getString(result, "item"))
        val item = Registries.ITEM.get(itemId)
        val count = JsonHelper.getInt(result, "count", 1)
        return MaceratorRecipe(id, ingredient, ItemStack(item, count))
    }

    override fun read(id: Identifier, buf: PacketByteBuf): MaceratorRecipe {
        val ingredient = Ingredient.fromPacket(buf)
        val output = buf.readItemStack()
        return MaceratorRecipe(id, ingredient, output)
    }

    override fun write(buf: PacketByteBuf, recipe: MaceratorRecipe) {
        recipe.ingredient.write(buf)
        buf.writeItemStack(recipe.output.copy())
    }
}
