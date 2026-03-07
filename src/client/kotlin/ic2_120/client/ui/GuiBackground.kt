package ic2_120.client.ui

import net.minecraft.client.gui.DrawContext

/**
 * 可复用的 GUI 空背景：纯 Compose 绘制，不使用 PNG 纹理。
 * 用于机器界面（电炉、MFSU 等）的统一背景样式。
 */
object GuiBackground {

    /** 内层填充色（深灰） */
    const val FILL_COLOR = 0xFF3C3C3C.toInt()

    /** 边框色（浅灰） */
    const val BORDER_COLOR = 0xFF8B8B8B.toInt()

    /** 外缘高光（更浅，可选） */
    const val HIGHLIGHT_COLOR = 0xFFAAAAAA.toInt()

    /**
     * 在指定区域绘制空背景：填充 + 单线边框。
     * 可在 [DrawContext] 的 drawBackground 中直接调用。
     */
    @JvmStatic
    fun draw(
        context: DrawContext,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        fillColor: Int = FILL_COLOR,
        borderColor: Int = BORDER_COLOR
    ) {
        context.fill(x, y, x + width, y + height, fillColor)
        context.drawBorder(x, y, width, height, borderColor)
    }
}
