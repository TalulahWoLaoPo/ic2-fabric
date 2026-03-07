package ic2_120.content.block

import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import net.minecraft.block.AbstractBlock

/**
 * 玻璃纤维导线。超高压传输，8192 EU/t，损耗仅 0.025 EU/格。
 */
@ModBlock(name = "glass_fibre_cable", registerItem = true, tab = CreativeTab.IC2_MATERIALS)
class GlassFibreCableBlock(settings: AbstractBlock.Settings = defaultSettings()) : BaseCableBlock(settings) {

    override fun getTransferRate(): Long = 8192L
    override fun getEnergyLoss(): Long = 25L
}
