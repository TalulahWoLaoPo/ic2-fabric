package ic2_120.client

import ic2_120.Ic2_120
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry
import net.minecraft.client.color.world.BiomeColors
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier

/**
 * 橡胶树叶生物群系着色器
 *
 * 根据生物群系对橡胶树叶应用 foliage 颜色（类似原版橡树树叶），
 * 在世界中随群系变化，物品栏使用默认绿色。
 */
object RubberLeavesColorProvider {

    /** 物品栏/手持时的默认树叶绿色 */
    private const val DEFAULT_FOLIAGE_COLOR = 0x59ae30

    fun register() {
        val block = Registries.BLOCK.get(Identifier(Ic2_120.MOD_ID, "rubber_leaves"))
        val item = Registries.ITEM.get(Identifier(Ic2_120.MOD_ID, "rubber_leaves"))

        ColorProviderRegistry.BLOCK.register({ state, world, pos, tintIndex ->
            if (world != null && pos != null) {
                BiomeColors.getFoliageColor(world, pos)
            } else {
                DEFAULT_FOLIAGE_COLOR
            }
        }, block)

        ColorProviderRegistry.ITEM.register({ stack, tintIndex ->
            DEFAULT_FOLIAGE_COLOR
        }, item)
    }
}
