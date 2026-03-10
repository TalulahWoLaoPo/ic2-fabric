package ic2_120.content.sync

import ic2_120.content.TickLimitedSidedEnergyContainer
import ic2_120.content.block.ITieredMachine
import ic2_120.content.syncs.SyncSchema
import net.minecraft.util.math.Direction

/**
 * 核反应堆同步属性与能量容器。
 *
 * 电力等级 5，最大输出 8192 EU/t。
 * 能量缓冲容量较大，供反应堆持续发电输出。
 */
class NuclearReactorSync(
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
        /** 电力等级 5 */
        const val REACTOR_TIER = 5
        /** 电力缓存容量 100000 EU */
        const val ENERGY_CAPACITY = 100_000L
        /** 整机每 tick 最大输出（8192 EU/t，tier 5） */
        val MAX_EXTRACT = ITieredMachine.euPerTickFromTier(REACTOR_TIER)
        const val NBT_ENERGY_STORED = "EnergyStored"
        /** 基础槽位（无反应仓时） */
        const val BASE_SLOTS = 27
        /** 每个相邻反应仓增加的槽位 */
        const val SLOTS_PER_CHAMBER = 9
    }

    var energy by schema.int("Energy")
    /** 当前有效槽位数量（27 + 相邻反应仓数 * 9），用于 GUI 与槽位校验 */
    var capacity by schema.int("Capacity", default = BASE_SLOTS)
    /** 反应堆温度（0–100 或自定义范围），目前未使用，后续热力学逻辑会用到 */
    var temperature by schema.int("Temperature", default = 0)

    override fun getSideMaxInsert(side: Direction?): Long = 0L
    /** 正面不输出；其余面可输出 */
    override fun getSideMaxExtract(side: Direction?): Long =
        if (side != getFacing()) MAX_EXTRACT else 0L

    override fun onEnergyCommitted() {
        energy = amount.toInt().coerceIn(0, Int.MAX_VALUE)
    }
}
