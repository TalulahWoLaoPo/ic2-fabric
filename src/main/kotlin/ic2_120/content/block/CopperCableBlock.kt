package ic2_120.content.block

import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import net.minecraft.block.AbstractBlock

/**
 * 铜质导线。中低压传输，128 EU/t，损耗 0.2 EU/格。
 */
@ModBlock(name = "copper_cable", registerItem = true, tab = CreativeTab.IC2_MATERIALS)
class CopperCableBlock(settings: AbstractBlock.Settings = defaultSettings()) : BaseCableBlock(settings) {

    override fun getTransferRate(): Long = 128L
    override fun getEnergyLoss(): Long = 300L  // 0.3 EU/格，对照 IC2 实验版 Wiki 铜线未绝缘
}
