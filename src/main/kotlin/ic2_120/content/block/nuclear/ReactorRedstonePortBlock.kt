package ic2_120.content.block.nuclear

import ic2_120.content.block.MachineBlock
import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import net.minecraft.block.AbstractBlock
import net.minecraft.block.Blocks

/**
 * 反应堆红石接口。占位方块，暂不实现实际作用。
 */
@ModBlock(name = "reactor_redstone_port", registerItem = true, tab = CreativeTab.IC2_MACHINES, group = "reactor")
class ReactorRedstonePortBlock(settings: AbstractBlock.Settings = AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).strength(5.0f, 6.0f)) : MachineBlock(settings)
