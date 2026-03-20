package ic2_120.client.compose

data class Padding(
    val left: Int = 0,
    val top: Int = 0,
    val right: Int = 0,
    val bottom: Int = 0
) {
    val horizontal get() = left + right
    val vertical get() = top + bottom

    companion object {
        val ZERO = Padding()
    }
}

data class Modifier(
    val width: Int? = null,
    val height: Int? = null,
    val fractionWidth: Float? = null, // 0.0~1.0，相对于父 Flex 容器的内尺寸
    val fractionHeight: Float? = null, // 0.0~1.0，相对于父 Flex 容器的内尺寸
    val padding: Padding = Padding.ZERO,
    val backgroundColor: Int? = null,
    val borderColor: Int? = null,
) {
    fun width(w: Int) = copy(width = w)
    fun height(h: Int) = copy(height = h)
    /**
     * 设置相对于父 Flex 容器内尺寸的百分比宽度。
     * 仅在父容器为 Flex 时生效。
     * @param fraction 0.0~1.0，如 0.5 表示占父容器宽度的 50%
     */
    fun fractionWidth(fraction: Float) = copy(fractionWidth = fraction.coerceIn(0f, 1f))
    /**
     * 设置相对于父 Flex 容器内尺寸的百分比高度。
     * 仅在父容器为 Flex 时生效。
     * @param fraction 0.0~1.0，如 0.5 表示占父容器高度的 50%
     */
    fun fractionHeight(fraction: Float) = copy(fractionHeight = fraction.coerceIn(0f, 1f))
    fun size(w: Int, h: Int) = copy(width = w, height = h)
    fun padding(all: Int) = copy(padding = Padding(all, all, all, all))
    fun padding(horizontal: Int, vertical: Int) = copy(padding = Padding(horizontal, vertical, horizontal, vertical))
    fun padding(left: Int, top: Int, right: Int, bottom: Int) = copy(padding = Padding(left, top, right, bottom))
    fun background(color: Int) = copy(backgroundColor = color)
    fun border(color: Int) = copy(borderColor = color)

    companion object {
        val EMPTY = Modifier()
    }
}
