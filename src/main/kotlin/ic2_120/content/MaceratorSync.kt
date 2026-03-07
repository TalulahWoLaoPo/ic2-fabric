package ic2_120.content

import ic2_120.content.syncs.SyncSchema

/**
 * 粉碎机的同步属性与能量存储。
 */
class MaceratorSync(
    schema: SyncSchema,
    currentTickProvider: () -> Long? = { null }
) : TickLimitedEnergyStorage(ENERGY_CAPACITY, MAX_INSERT, MAX_EXTRACT, currentTickProvider) {

    companion object {
        const val ENERGY_CAPACITY = 416L
        const val MAX_INSERT = 32L
        const val MAX_EXTRACT = 0L
        const val NBT_ENERGY_STORED = "EnergyStored"
        const val PROGRESS_MAX = 130
        const val ENERGY_PER_TICK = 2L
    }

    var energy by schema.int("Energy")
    var progress by schema.int("Progress")  // 加工进度 0..PROGRESS_MAX

    override fun onFinalCommit() {
        energy = amount.toInt().coerceIn(0, Int.MAX_VALUE)
    }
}
