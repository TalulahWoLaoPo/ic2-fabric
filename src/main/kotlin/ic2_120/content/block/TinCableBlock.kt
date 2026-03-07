package ic2_120.content.block

import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import net.minecraft.block.AbstractBlock

/**
 * 锡质导线。低压传输，32 EU/t，损耗 0.025 EU/格。
 */
@ModBlock(name = "tin_cable", registerItem = true, tab = CreativeTab.IC2_MATERIALS)
class TinCableBlock(settings: AbstractBlock.Settings = defaultSettings()) : BaseCableBlock(settings) {

    override fun getTransferRate(): Long = 32L
    override fun getEnergyLoss(): Long = 25L
}
