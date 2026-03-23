package ic2_120.integration.jei

import mezz.jei.api.recipe.RecipeType
import net.minecraft.util.Identifier

object Ic2JeiRecipeTypes {
    val MACERATOR: RecipeType<MaceratorJeiRecipe> = RecipeType.create(
        "ic2_120",
        "macerating",
        MaceratorJeiRecipe::class.java
    )

    val MACERATOR_UID: Identifier = Identifier("ic2_120", "macerating")
}

