package ic2_120.client

import ic2_120.Ic2_120
import ic2_120.content.item.getFluidCellVariant
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier

/**
 * 通用流体单元着色器：使用 Fabric FluidRenderHandler 获取流体颜色，
 * 将颜色渲染到物品中心（tintindex 0），便于手持时识别所含流体。
 */
object FluidCellColorProvider {

    fun register() {
        val fluidCell = Registries.ITEM.get(Identifier(Ic2_120.MOD_ID, "fluid_cell"))
        ColorProviderRegistry.ITEM.register({ stack, tintIndex ->
            // layer1 (fluid_cell_window) 对应 tintIndex 1，渲染流体颜色到中心
            if (tintIndex != 1) return@register -1
            val variant = stack.getFluidCellVariant() ?: return@register -1
            val fluid = variant.fluid
            val handler = FluidRenderHandlerRegistry.INSTANCE.get(fluid) ?: return@register -1
            handler.getFluidColor(null, null, fluid.defaultState)
        }, fluidCell)
    }
}
