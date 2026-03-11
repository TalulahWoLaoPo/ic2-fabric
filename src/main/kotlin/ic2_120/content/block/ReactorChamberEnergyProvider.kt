package ic2_120.content.block

import ic2_120.content.block.machines.ReactorChamberBlockEntity
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant
import net.minecraft.block.entity.BlockEntity
import net.minecraft.util.math.Direction
import team.reborn.energy.api.EnergyStorage
import team.reborn.energy.api.base.SimpleSidedEnergyContainer

/**
 * 核反应仓能量存储提供者。
 * 与核反应堆相邻时，共享反应堆的能量实例。
 */
object ReactorChamberEnergyProvider {
    
    /**
     * 获取核反应仓的能量存储
     */
    fun getEnergyStorage(be: ReactorChamberBlockEntity, side: Direction?): EnergyStorage? {
        return be.getEnergyStorage(side)
    }
}
