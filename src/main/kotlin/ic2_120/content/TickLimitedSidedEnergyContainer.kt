package ic2_120.content

import net.minecraft.util.math.Direction
import team.reborn.energy.api.base.SimpleSidedEnergyContainer

/**
 * 将分面容器的 maxInsert 语义从“单次调用上限”提升为“每 tick 总输入上限”的可复用基类。
 *
 * - 子类只需实现各面的基础输入/输出规则；
 * - 基类统一处理按 tick 的输入预算；
 * - 提供 [syncCommittedAmount] 以兼容外部直接改 amount（如读 NBT）。
 */
open class TickLimitedSidedEnergyContainer(
    private val capacity: Long,
    private val maxInsertPerTick: Long,
    private val maxExtractPerTick: Long,
    private val currentTickProvider: () -> Long? = { null }
) : SimpleSidedEnergyContainer() {

    private var insertTrackedTick: Long = Long.MIN_VALUE
    private var insertedThisTick: Long = 0L
    private var lastCommittedAmount: Long = 0L

    override fun getCapacity(): Long = capacity

    final override fun getMaxInsert(side: Direction?): Long {
        normalizeTickBudget()
        val sideLimit = getSideMaxInsert(side).coerceAtLeast(0L).coerceAtMost(maxInsertPerTick)
        val remainingThisTick = (maxInsertPerTick - insertedThisTick).coerceAtLeast(0L)
        return minOf(sideLimit, remainingThisTick)
    }

    final override fun getMaxExtract(side: Direction?): Long =
        getSideMaxExtract(side).coerceAtLeast(0L).coerceAtMost(maxExtractPerTick)

    final override fun onFinalCommit() {
        normalizeTickBudget()
        val insertedDelta = (amount - lastCommittedAmount).coerceAtLeast(0L)
        if (insertedDelta > 0L) {
            insertedThisTick = (insertedThisTick + insertedDelta).coerceAtMost(maxInsertPerTick)
        }
        lastCommittedAmount = amount
        onEnergyCommitted()
    }

    /** 外部直接改 amount（例如读 NBT）后调用，重置提交基线，避免首个事务误判为大额插入。 */
    fun syncCommittedAmount() {
        lastCommittedAmount = amount
    }

    /** 子类定义各面的基础输入能力（不含每 tick 预算限制）。 */
    protected open fun getSideMaxInsert(side: Direction?): Long = maxInsertPerTick

    /** 子类定义各面的基础输出能力。 */
    protected open fun getSideMaxExtract(side: Direction?): Long = maxExtractPerTick

    /** 子类可在提交后同步 GUI/NBT 镜像字段。 */
    protected open fun onEnergyCommitted() = Unit

    private fun normalizeTickBudget() {
        val now = currentTickProvider() ?: return
        if (insertTrackedTick != now) {
            insertTrackedTick = now
            insertedThisTick = 0L
        }
    }
}
