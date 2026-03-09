package ic2_120.content.block

import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import net.minecraft.block.AbstractBlock
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.item.ItemPlacementContext
import net.minecraft.state.StateManager
import net.minecraft.state.property.BooleanProperty
import net.minecraft.state.property.Properties
import net.minecraft.util.math.Direction

/**
 * 变压器方块。仅作为合成材料，无 BlockEntity，不实现电力转换功能。
 */
@ModBlock(name = "lv_transformer", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "transformer")
class LvTransformerBlock : Block(
    AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).strength(5.0f, 6.0f)
) {
    init {
        defaultState = stateManager.defaultState
            .with(Properties.FACING, Direction.NORTH)
            .with(ACTIVE, false)
    }

    override fun appendProperties(builder: StateManager.Builder<Block, BlockState>) {
        super.appendProperties(builder)
        builder.add(Properties.FACING, ACTIVE)
    }

    override fun getPlacementState(ctx: ItemPlacementContext): BlockState? =
        defaultState.with(Properties.FACING, ctx.side.opposite)

    companion object {
        val ACTIVE: BooleanProperty = BooleanProperty.of("active")
    }
}

@ModBlock(name = "mv_transformer", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "transformer")
class MvTransformerBlock : Block(
    AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).strength(5.0f, 6.0f)
) {
    init {
        defaultState = stateManager.defaultState
            .with(Properties.FACING, Direction.NORTH)
            .with(ACTIVE, false)
    }

    override fun appendProperties(builder: StateManager.Builder<Block, BlockState>) {
        super.appendProperties(builder)
        builder.add(Properties.FACING, ACTIVE)
    }

    override fun getPlacementState(ctx: ItemPlacementContext): BlockState? =
        defaultState.with(Properties.FACING, ctx.side.opposite)

    companion object {
        val ACTIVE: BooleanProperty = BooleanProperty.of("active")
    }
}

@ModBlock(name = "hv_transformer", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "transformer")
class HvTransformerBlock : Block(
    AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).strength(5.0f, 6.0f)
) {
    init {
        defaultState = stateManager.defaultState
            .with(Properties.FACING, Direction.NORTH)
            .with(ACTIVE, false)
    }

    override fun appendProperties(builder: StateManager.Builder<Block, BlockState>) {
        super.appendProperties(builder)
        builder.add(Properties.FACING, ACTIVE)
    }

    override fun getPlacementState(ctx: ItemPlacementContext): BlockState? =
        defaultState.with(Properties.FACING, ctx.side.opposite)

    companion object {
        val ACTIVE: BooleanProperty = BooleanProperty.of("active")
    }
}

@ModBlock(name = "ev_transformer", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "transformer")
class EvTransformerBlock : Block(
    AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).strength(5.0f, 6.0f)
) {
    init {
        defaultState = stateManager.defaultState
            .with(Properties.FACING, Direction.NORTH)
            .with(ACTIVE, false)
    }

    override fun appendProperties(builder: StateManager.Builder<Block, BlockState>) {
        super.appendProperties(builder)
        builder.add(Properties.FACING, ACTIVE)
    }

    override fun getPlacementState(ctx: ItemPlacementContext): BlockState? =
        defaultState.with(Properties.FACING, ctx.side.opposite)

    companion object {
        val ACTIVE: BooleanProperty = BooleanProperty.of("active")
    }
}
