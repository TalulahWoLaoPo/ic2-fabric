package ic2_120.content.block.machines

import ic2_120.content.heat.IHeatConsumer
import ic2_120.content.heat.IHeatNode
import ic2_120.content.sync.HeatFlowSync
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.state.property.Properties

/**
 * 热机基类：默认仅背面传热，不建立热网，直接邻接传输。
 * 跟踪热产生和输出速率，用于 UI 显示。
 */
abstract class HeatGeneratorBlockEntityBase(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : MachineBlockEntity(type, pos, state), IHeatNode, HeatFlowSync.HeatProducer {

    private var lastGeneratedHeat: Long = 0L
    private var lastOutputHeat: Long = 0L

    override fun getHeatTransferFace(): Direction {
        val front = world?.getBlockState(pos)?.get(Properties.HORIZONTAL_FACING) ?: Direction.NORTH
        return front.opposite
    }

    /** 获取上一 tick 产生的热量（HU） */
    override fun getLastGeneratedHeat(): Long = lastGeneratedHeat

    /** 获取上一 tick 输出的热量（HU） */
    override fun getLastOutputHeat(): Long = lastOutputHeat

    /** 记录产生的热量（在产生热时调用） */
    protected fun recordGeneratedHeat(amount: Long) {
        lastGeneratedHeat = amount.coerceAtLeast(0L)
    }

    /** 重置热跟踪（在 tick 开始时调用） */
    protected fun resetHeatTracking() {
        lastGeneratedHeat = 0L
        lastOutputHeat = 0L
    }

    protected fun transferHeatToBack(availableHu: Long): Long {
        if (availableHu <= 0L) return 0L
        val world = world ?: return 0L
        val myFace = getHeatTransferFace()
        val neighborPos = pos.offset(myFace)
        val neighbor = world.getBlockEntity(neighborPos) as? IHeatConsumer ?: return 0L

        // 只有双方传热面相对贴合才允许传热
        if (neighbor.getHeatTransferFace() != myFace.opposite) return 0L

        val transferred = neighbor.receiveHeat(availableHu, myFace.opposite).coerceIn(0L, availableHu)
        lastOutputHeat = transferred
        return transferred
    }
}

