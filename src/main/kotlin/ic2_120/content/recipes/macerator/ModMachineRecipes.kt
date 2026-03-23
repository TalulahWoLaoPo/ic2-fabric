package ic2_120.content.recipes.macerator

import ic2_120.Ic2_120
import net.minecraft.recipe.RecipeSerializer
import net.minecraft.recipe.RecipeType
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry

object ModMachineRecipes {
    val MACERATOR_TYPE: RecipeType<MaceratorRecipe> = object : RecipeType<MaceratorRecipe> {
        override fun toString(): String = "${Ic2_120.MOD_ID}:macerating"
    }

    val MACERATOR_SERIALIZER: RecipeSerializer<MaceratorRecipe> = MaceratorRecipeSerializer

    fun register() {
        Registry.register(Registries.RECIPE_TYPE, Ic2_120.id("macerating"), MACERATOR_TYPE)
        Registry.register(Registries.RECIPE_SERIALIZER, Ic2_120.id("macerating"), MACERATOR_SERIALIZER)
    }
}

