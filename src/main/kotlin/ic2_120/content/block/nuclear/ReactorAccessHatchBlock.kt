package ic2_120.content.block.nuclear

import ic2_120.content.block.MachineBlock
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import net.minecraft.block.AbstractBlock
import net.minecraft.block.Blocks

/**
 * 反应堆访问接口。占位方块，暂不实现实际作用。
 */
@ModBlock(name = "reactor_access_hatch", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "reactor")
class ReactorAccessHatchBlock(settings: AbstractBlock.Settings = AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).strength(5.0f, 6.0f)) : MachineBlock(settings)
