package ic2_120.content

import ic2_120.content.syncs.SyncSchema

/**
 * 电炉的同步属性与能量存储——继承 [TickLimitedEnergyStorage]，一个对象同时作为 Energy API 存储与 GUI 同步数据源。
 * 同步属性（energy、progress）用于 GUI；本对象即 [EnergyStorage][team.reborn.energy.api.EnergyStorage]，供 SIDED 与 NBT 使用。
 * 服务端 [SyncedData][ic2_120.content.syncs.SyncedData] 依赖 BlockEntity 上下文，写入属性时自动 [BlockEntity.markDirty]，无需再传回调。
 */
class ElectricFurnaceSync(
    schema: SyncSchema,
    currentTickProvider: () -> Long? = { null }
) : TickLimitedEnergyStorage(ENERGY_CAPACITY, MAX_INSERT, MAX_EXTRACT, currentTickProvider) {

    companion object {
        const val ENERGY_CAPACITY = 10_000L
        const val MAX_INSERT = 32L
        const val MAX_EXTRACT = 0L
        const val NBT_ENERGY_STORED = "EnergyStored"
        /** 烧炼完成所需进度（与 MC 熔炉 200 tick 一致，用于 GUI 进度条） */
        const val PROGRESS_MAX = 200
    }

    var energy by schema.int("Energy")
    var progress by schema.int("Progress")

    override fun onFinalCommit() {
        energy = amount.toInt().coerceIn(0, Int.MAX_VALUE)
    }
}
