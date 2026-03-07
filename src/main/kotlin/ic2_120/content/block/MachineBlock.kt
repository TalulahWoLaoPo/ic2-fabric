package ic2_120.content.block

import net.minecraft.block.AbstractBlock
import net.minecraft.block.BlockRenderType
import net.minecraft.block.BlockState
import net.minecraft.block.BlockWithEntity
import net.minecraft.block.entity.BlockEntity
import net.minecraft.item.ItemPlacementContext
import net.minecraft.state.StateManager
import net.minecraft.state.property.Properties
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction

/**
 * 简单的机器方块基类。
 * 可被子类扩展以支持 BlockEntity。
 * 放置时根据玩家朝向设置 HORIZONTAL_FACING（玩家面向的方向的相反方向为方块正面）。
 */
open class MachineBlock(settings: AbstractBlock.Settings) : BlockWithEntity(settings) {

    init {
        defaultState = stateManager.defaultState.with(Properties.HORIZONTAL_FACING, Direction.NORTH)
    }

    override fun appendProperties(builder: StateManager.Builder<net.minecraft.block.Block, BlockState>) {
        super.appendProperties(builder)
        builder.add(Properties.HORIZONTAL_FACING)
    }

    override fun getPlacementState(ctx: ItemPlacementContext): BlockState? =
        defaultState.with(Properties.HORIZONTAL_FACING, ctx.horizontalPlayerFacing.opposite)

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? = null

    /**
     * BlockWithEntity 默认可能不会使用 JSON 模型渲染，显式指定为 MODEL
     * 以确保像电炉这类机器方块按 blockstate/model 显示材质。
     */
    override fun getRenderType(state: BlockState): BlockRenderType = BlockRenderType.MODEL
}
