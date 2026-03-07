package ic2_120.content.block

import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import net.minecraft.block.AbstractBlock

/**
 * 金质导线。中压传输，512 EU/t，损耗 0.8 EU/格。
 */
@ModBlock(name = "gold_cable", registerItem = true, tab = CreativeTab.IC2_MATERIALS)
class GoldCableBlock(settings: AbstractBlock.Settings = defaultSettings()) : BaseCableBlock(settings) {

    override fun getTransferRate(): Long = 512L
    override fun getEnergyLoss(): Long = 500L  // 0.5 EU/格，对照 IC2 实验版 Wiki 金线未绝缘

    
    override fun getCableMin(): Double = 3.0 / 16.0
    override fun getCableMax(): Double = 12.0 / 16.0
}
