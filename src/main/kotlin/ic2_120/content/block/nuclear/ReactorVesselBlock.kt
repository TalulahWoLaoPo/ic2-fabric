package ic2_120.content.block.nuclear

import ic2_120.content.block.MachineBlock
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import net.minecraft.block.AbstractBlock
import net.minecraft.block.Blocks

/**
 * 核反应堆压力容器。占位方块，暂不实现实际作用。
 */
@ModBlock(name = "reactor_vessel", registerItem = true, tab = CreativeTab.IC2_MATERIALS, group = "reactor")
class ReactorVesselBlock(settings: AbstractBlock.Settings = AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).strength(5.0f, 6.0f)) : MachineBlock(settings)
