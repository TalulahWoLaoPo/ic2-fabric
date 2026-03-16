package ic2_120.content.block.machines

import ic2_120.content.heat.IHeatConsumer
import ic2_120.content.heat.IHeatNode
import ic2_120.content.sync.HeatFlowSync
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.state.property.Properties
import net.minecraft.world.World
import org.slf4j.LoggerFactory

/**
 * 热机基类：默认仅背面传热，不建立热网，直接邻接传输。
 * 跟踪热产生和输出速率，用于 UI 显示。
 */
abstract class HeatGeneratorBlockEntityBase(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : MachineBlockEntity(type, pos, state), IHeatNode, HeatFlowSync.HeatProducer {

    companion object {
        private val logger = LoggerFactory.getLogger("ic2_120/HeatGeneratorBase")
    }

    private var lastGeneratedHeat: Long = 0L
    private var lastOutputHeat: Long = 0L

    abstract val heatFlow: HeatFlowSync

    override fun getHeatTransferFace(): Direction {
        return world?.getBlockState(pos)?.get(Properties.HORIZONTAL_FACING) ?: Direction.NORTH
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

        logger.info("[{}] transferHeatToBack: 可用热量 {} HU, 我的传热面 {}, 邻居位置 {}",
            this.javaClass.simpleName, availableHu, myFace, neighborPos)

        // 只有双方传热面相对贴合才允许传热
        val neighborFace = neighbor.getHeatTransferFace()
        if (neighborFace != myFace.opposite) {
            logger.info("[{}] 传热失败：邻居传热面 {} != 预期 {}",
                this.javaClass.simpleName, neighborFace, myFace.opposite)
            return 0L
        }

        // receiveHeat 的 fromSide 以“接收方视角”表示，因此要传邻居看到的入热面
        val transferred = neighbor.receiveHeat(availableHu, myFace.opposite).coerceIn(0L, availableHu)
        lastOutputHeat = transferred
        logger.info("[{}] 传热成功：传输 {} HU / 可用 {} HU, 从我的传热面 {} 到邻居",
            this.javaClass.simpleName, transferred, availableHu, myFace)
        return transferred
    }

    protected fun hasValidHeatConsumer(): Boolean {
        val world = world ?: return false
        val facing = world.getBlockState(pos)?.get(Properties.HORIZONTAL_FACING) ?: Direction.NORTH
        val myFace = getHeatTransferFace()
        val neighborPos = pos.offset(myFace)
        val neighbor = world.getBlockEntity(neighborPos) as? IHeatConsumer ?: run {
            logger.info("[{}] hasValidHeatConsumer: 邻居位置 {} 没有IHeatConsumer, 我的位置={}, facing={}, 传热面={}",
                this.javaClass.simpleName, neighborPos, pos, facing, myFace)
            return false
        }

        val neighborFace = neighbor.getHeatTransferFace()
        val isValid = neighborFace == myFace.opposite
        logger.info("[{}] hasValidHeatConsumer: 我的位置={}, facing={}, 传热面={}, 邻居位置={}, 邻居传热面={}, 有效={}",
            this.javaClass.simpleName, pos, facing, myFace, neighborPos, neighborFace, isValid)
        return isValid
    }

    protected fun tickHeatMachine(world: World, pos: BlockPos, state: BlockState) {
        if (world.isClient) return

        resetHeatTracking()

        val generatedThisTick = generateHeat(world, pos, state)
        logger.info("[{}] tick: 我的={}, 产生热量={} HU",
            this.javaClass.simpleName, pos, generatedThisTick)

        logger.info("[{}] tick: facing={}, 传热面={}",
            this.javaClass.simpleName,
            world.getBlockState(pos)?.get(Properties.HORIZONTAL_FACING),
            getHeatTransferFace())

        transferHeatToBack(generatedThisTick)
        recordGeneratedHeat(generatedThisTick)
        heatFlow.syncCurrentTickFlow()
        syncAdditionalData()

        val hasValidConsumer = hasValidHeatConsumer()
        val shouldRun = shouldActivate(generatedThisTick, hasValidConsumer)
        logger.info("[{}] 激活状态: 产生={}, 有耗热机={}, 应激活={}",
            this.javaClass.simpleName, generatedThisTick > 0, hasValidConsumer, shouldRun)
        if (getActiveState(state) != shouldRun) {
            setActiveState(world, pos, state, shouldRun)
        }
    }

    protected abstract fun generateHeat(world: World, pos: BlockPos, state: BlockState): Long

    protected open fun syncAdditionalData() {}

    protected abstract fun shouldActivate(generatedHeat: Long, hasValidConsumer: Boolean): Boolean

    protected abstract fun getActiveState(state: BlockState): Boolean

    protected abstract fun setActiveState(world: World, pos: BlockPos, state: BlockState, active: Boolean)
}
