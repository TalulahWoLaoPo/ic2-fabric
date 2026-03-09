package ic2_120.client

import ic2_120.content.item.getFluidCellVariant
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry
import net.minecraft.fluid.Fluids
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier
import ic2_120.Ic2_120

/**
 * 通用流体单元着色器：使用 Fabric FluidRenderHandler 获取流体颜色，
 * 将颜色渲染到物品中心（tintindex 1），便于手持时识别所含流体。
 * 水和岩浆使用原版硬编码颜色。
 */
object FluidCellColorProvider {

    private const val WATER_COLOR = 0x3F76E4
    private const val LAVA_COLOR = 0xFF4400

    fun register() {
        val fluidCell = Registries.ITEM.get(Identifier(Ic2_120.MOD_ID, "fluid_cell"))
        ColorProviderRegistry.ITEM.register({ stack, tintIndex ->
            if (tintIndex != 1) return@register -1
            val fluid = stack.getFluidCellVariant()?.fluid ?: return@register -1
            when (fluid) {
                Fluids.WATER -> WATER_COLOR
                Fluids.LAVA -> LAVA_COLOR
                else -> {
                    val handler = FluidRenderHandlerRegistry.INSTANCE.get(fluid)
                        ?: return@register -1
                    val color = handler.getFluidColor(client.world, client.player?.blockPos ?: BlockPos.ORIGIN, fluid.defaultState)
                    println("$fluid: $color")
                    color
                }
            }
        }, fluidCell)
    }
}
