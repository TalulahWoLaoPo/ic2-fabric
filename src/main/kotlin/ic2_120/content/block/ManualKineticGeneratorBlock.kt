package ic2_120.content.block

import ic2_120.content.block.machines.ManualKineticGeneratorBlockEntity
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.id
import ic2_120.registry.instance
import ic2_120.registry.item
import ic2_120.registry.type
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityTicker
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.state.StateManager
import net.minecraft.state.property.BooleanProperty
import net.minecraft.state.property.Properties
import net.minecraft.world.World
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
import net.minecraft.data.server.recipe.RecipeJsonProvider
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder
import net.minecraft.recipe.book.RecipeCategory
import java.util.function.Consumer

@ModBlock(name = "manual_kinetic_generator", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "generator")
class ManualKineticGeneratorBlock : DirectionalMachineBlock() {

    companion object {
        val ACTIVE: BooleanProperty = BooleanProperty.of("active")

        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            val casing = MachineCasingBlock::class.instance()
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, ManualKineticGeneratorBlock::class.item(), 1)
                .pattern(" I ")
                .pattern("ICI")
                .pattern(" I ")
                .input('I', net.minecraft.item.Items.IRON_INGOT)
                .input('C', casing)
                .criterion(
                    hasItem(casing),
                    conditionsFromItem(casing)
                )
                .offerTo(exporter, ManualKineticGeneratorBlock::class.id())
        }
    }

    override fun appendProperties(builder: StateManager.Builder<net.minecraft.block.Block, BlockState>) {
        super.appendProperties(builder)
        builder.add(ACTIVE)
    }

    override fun getPlacementState(ctx: net.minecraft.item.ItemPlacementContext): BlockState? =
        super.getPlacementState(ctx)?.with(ACTIVE, false)

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? =
        ManualKineticGeneratorBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: World,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else checkType(type, ManualKineticGeneratorBlockEntity::class.type()) { w, p, s, be ->
            (be as ManualKineticGeneratorBlockEntity).tick(w, p, s)
        }

    override fun onUse(
        state: BlockState,
        world: World,
        pos: BlockPos,
        player: PlayerEntity,
        hand: Hand,
        hit: BlockHitResult
    ): ActionResult {
        val be = world.getBlockEntity(pos) as? ManualKineticGeneratorBlockEntity ?: return ActionResult.PASS
        if (world.isClient) return ActionResult.SUCCESS
        return be.onRightClick(player)
    }
}