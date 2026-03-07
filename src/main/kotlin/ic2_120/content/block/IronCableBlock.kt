package ic2_120.content.block

import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import net.minecraft.block.AbstractBlock

/**
 * 高压导线（铁质）。2048 EU/t，损耗 0.8 EU/格。碰撞箱较默认导线更粗。
 */
@ModBlock(name = "iron_cable", registerItem = true, tab = CreativeTab.IC2_MATERIALS)
class IronCableBlock(settings: AbstractBlock.Settings = defaultSettings()) : BaseCableBlock(settings) {

    override fun getTransferRate(): Long = 2048L
    override fun getEnergyLoss(): Long = 1000L  // 1.0 EU/格，对照 IC2 实验版 Wiki 高压线未绝缘

    override fun getCableMin(): Double = 5.0 / 16.0
    override fun getCableMax(): Double = 11.0 / 16.0
}
