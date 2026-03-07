package ic2_120.content

import ic2_120.content.syncs.SyncSchema
import net.minecraft.util.math.Direction

/**
 * 火力发电机的同步属性与能量存储。
 * 燃烧燃料产生 EU，可被相邻方块（电缆等）提取。
 */
class GeneratorSync(
    schema: SyncSchema,
    private val getFacing: () -> Direction,
    private val currentTickProvider: () -> Long? = { null }
) : TickLimitedSidedEnergyContainer(
    capacity = ENERGY_CAPACITY,
    maxInsertPerTick = 0L,
    maxExtractPerTick = MAX_EXTRACT,
    currentTickProvider = currentTickProvider
) {

    companion object {
        /** 电力缓存容量 1 万 EU */
        const val ENERGY_CAPACITY = 10_000L
        const val MAX_EXTRACT = 32L
        const val NBT_ENERGY_STORED = "EnergyStored"
        /** 燃料燃烧进度条最大值 */
        const val BURN_TIME_MAX = 100
        /** 每 tick 燃烧进度消耗（用于 GUI 显示） */
        const val BURN_PROGRESS_PER_TICK = 1
        /** 每 tick 产生 EU（与 IC2 经典一致：1 煤 1600 tick → 4000 EU，即 2.5 EU/t） */
        const val EU_PER_BURN_TICK = 2.5
    }

    var energy by schema.int("Energy")
    /** 当前燃料剩余燃烧时间（tick） */
    var burnTime by schema.int("BurnTime")
    /** 当前燃料总燃烧时间（tick），用于 GUI 进度条 */
    var totalBurnTime by schema.int("TotalBurnTime")

    override fun getSideMaxInsert(side: Direction?): Long = 0L
    override fun getSideMaxExtract(side: Direction?): Long =
        if (side != getFacing()) MAX_EXTRACT else 0L

    override fun onEnergyCommitted() {
        energy = amount.toInt().coerceIn(0, Int.MAX_VALUE)
    }
}
