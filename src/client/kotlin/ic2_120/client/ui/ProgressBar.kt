package ic2_120.client.ui

import net.minecraft.client.gui.DrawContext

/**
 * 原版风格的进度条：深色底、浅色填充、细边框。
 * 支持横向和竖向进度条，可用于燃烧进度、能量条等。
 */
object ProgressBar {

    /** 背景色（原版 GUI 深灰） */
    const val BG_COLOR = 0xFF555555.toInt()

    /** 填充色（原版熔炉箭头/火焰的暖色） */
    const val FILL_COLOR = 0xFFB0B0B0.toInt()

    /** 边框色（与 GuiBackground 一致） */
    const val BORDER_COLOR = 0xFF8B8B8B.toInt()

    /**
     * 在指定区域绘制横向进度条。
     * @param context DrawContext（如 drawBackground 的 context）
     * @param x 左上角 X（屏幕坐标）
     * @param y 左上角 Y
     * @param width 总宽度
     * @param height 高度（建议 8～10）
     * @param fraction 进度 0f～1f
     */
    @JvmStatic
    fun draw(
        context: DrawContext,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        fraction: Float
    ) {
        val f = fraction.coerceIn(0f, 1f)
        context.fill(x, y, x + width, y + height, BG_COLOR)
        val filledW = (f * width).toInt()
        if (filledW > 0) {
            context.fill(x, y, x + filledW, y + height, FILL_COLOR)
        }
        context.drawBorder(x, y, width, height, BORDER_COLOR)
    }

    /**
     * 绘制竖向燃烧进度条，从红到蓝渐变（满=红，空=蓝）。
     * 进度条从底部向上填充，表示燃料消耗。
     * @param context DrawContext
     * @param x 左上角 X
     * @param y 左上角 Y
     * @param width 宽度（建议 6～8）
     * @param height 总高度
     * @param fraction 剩余燃料比例 0f～1f（1=满，0=空）
     */
    @JvmStatic
    fun drawVerticalFuelBar(
        context: DrawContext,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        fraction: Float
    ) {
        val f = fraction.coerceIn(0f, 1f)

        // 绘制背景
        context.fill(x, y, x + width, y + height, BG_COLOR)

        // 计算填充高度
        val filledH = (f * height).toInt()
        if (filledH > 0) {
            // 从底部向上绘制渐变（红 -> 橙 -> 黄 -> 绿 -> 蓝）
            // 底部是燃料充足（红），顶部是燃料即将耗尽（蓝）
            for (row in 0 until filledH) {
                // row=0 是底部，row=filledH-1 是顶部
                // progress: 0（底）到 1（顶）
                val progress = row.toFloat() / filledH.coerceAtLeast(1)
                val color = interpolateColor(0xFFFF0000.toInt(), 0xFF0000FF.toInt(), progress)
                // 从底部往上绘制
                val drawY = y + height - filledH + row
                context.fill(x, drawY, x + width, drawY + 1, color)
            }
        }

        // 绘制边框
        context.drawBorder(x, y, width, height, BORDER_COLOR)
    }

    /**
     * 在两个 ARGB 颜色之间插值
     * @param color1 起始颜色（ARGB 格式，如 0xFFFF0000）
     * @param color2 结束颜色（ARGB 格式，如 0xFF0000FF）
     * @param t 插值因子 0f～1f
     * @return 插值后的 ARGB 颜色
     */
    private fun interpolateColor(color1: Int, color2: Int, t: Float): Int {
        val a1 = (color1 shr 24) and 0xFF
        val r1 = (color1 shr 16) and 0xFF
        val g1 = (color1 shr 8) and 0xFF
        val b1 = color1 and 0xFF

        val a2 = (color2 shr 24) and 0xFF
        val r2 = (color2 shr 16) and 0xFF
        val g2 = (color2 shr 8) and 0xFF
        val b2 = color2 and 0xFF

        val a = (a1 + (a2 - a1) * t).toInt().coerceIn(0, 255)
        val r = (r1 + (r2 - r1) * t).toInt().coerceIn(0, 255)
        val g = (g1 + (g2 - g1) * t).toInt().coerceIn(0, 255)
        val b = (b1 + (b2 - b1) * t).toInt().coerceIn(0, 255)

        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }
}
