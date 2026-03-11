package ic2_120.client

import ic2_120.client.compose.*
import ic2_120.client.ui.GuiBackground
import ic2_120.content.block.NuclearReactorBlock
import ic2_120.content.screen.NuclearReactorScreenHandler
import ic2_120.content.sync.NuclearReactorSync
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text

@ModScreen(block = NuclearReactorBlock::class)
class NuclearReactorScreen(
    handler: NuclearReactorScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<NuclearReactorScreenHandler>(handler, playerInventory, title) {

    private val ui = ComposeUI()

    /** 整体 UI 下移偏移，避免顶部文字溢出 */
    private val guiOffsetY = 8

    /** 能量条/温度条宽度 */
    private val barWidth = 14

    init {
        backgroundWidth = NuclearReactorScreenHandler.FRAME_WIDTH
        backgroundHeight = handler.hotbarY + 18 + 8
        titleY = -1000
        playerInventoryTitleY = -1000  // 隐藏 "Inv" 文本
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        GuiBackground.draw(context, x, y, backgroundWidth, backgroundHeight)
        GuiBackground.drawPlayerInventorySlotBorders(
            context, x, y,
            handler.playerInvY -4,
            handler.hotbarY -4 ,
            NuclearReactorScreenHandler.SLOT_SIZE,
            playerInvX = NuclearReactorScreenHandler.PLAYER_INV_X
        )
        val borderColor = GuiBackground.BORDER_COLOR
        val slotSize = NuclearReactorScreenHandler.SLOT_SIZE
        val borderOffset = 1

        // 反应堆槽位整体 9x9 区域外边框
        val gridX = x + NuclearReactorScreenHandler.SLOT_GRID_X - borderOffset
        val gridY = y + NuclearReactorScreenHandler.SLOT_GRID_Y - borderOffset
        val gridW = 9 * slotSize + borderOffset * 2
        val gridH = 9 * slotSize + borderOffset * 2
        context.drawBorder(gridX, gridY, gridW, gridH, borderColor)

        // 为每个反应堆槽位绘制外边框（参考 GeneratorScreen），让用户清楚实际有多少 slot
        for (i in 0 until handler.reactorSlotCount) {
            val slot = handler.slots[i]
            context.drawBorder(x + slot.x - borderOffset, y + slot.y - borderOffset, slotSize, slotSize, borderColor)
        }

        // 左侧竖能量条（加宽，与 SLOT_GRID_X 配合使满容量时槽位区域居中）
        val energyBarX = x + 9
        drawVerticalEnergyBar(context, energyBarX, y + NuclearReactorScreenHandler.SLOT_GRID_Y, barWidth, 9 * slotSize)

        // 右侧竖温度条（蓝→红，加宽）
        val tempBarX = x + NuclearReactorScreenHandler.SLOT_GRID_X + 9 * slotSize + 4
        drawVerticalTemperatureBar(context, tempBarX, y + NuclearReactorScreenHandler.SLOT_GRID_Y, barWidth, 9 * slotSize)
    }

    private fun drawVerticalEnergyBar(context: DrawContext, barX: Int, barY: Int, w: Int, h: Int) {
        val energy = handler.sync.energy.toLong().coerceAtLeast(0)
        val cap = NuclearReactorSync.ENERGY_CAPACITY
        val fraction = if (cap > 0) (energy.toFloat() / cap).coerceIn(0f, 1f) else 0f

        context.fill(barX, barY, barX + w, barY + h, 0xFF333333.toInt())
        val filledH = (fraction * h).toInt()
        if (filledH > 0) {
            val fillY = barY + h - filledH
            context.enableScissor(barX, fillY, barX + w, barY + h)
            val strips = maxOf(2, h)
            for (i in 0 until strips) {
                val t = i.toFloat() / (strips - 1).coerceAtLeast(1)
                val color = lerpArgb(0xFFCC0000.toInt(), 0xFF00CC00.toInt(), t)
                val y1 = fillY + (i * h / strips)
                val y2 = fillY + ((i + 1) * h / strips).coerceAtMost(fillY + h)
                context.fill(barX, y1, barX + w, y2, color)
            }
            context.disableScissor()
        }
        context.drawBorder(barX, barY, w, h, 0xFF888888.toInt())
    }

    private fun drawVerticalTemperatureBar(context: DrawContext, barX: Int, barY: Int, w: Int, h: Int) {
        val temp = handler.sync.temperature.coerceIn(0, NuclearReactorSync.HEAT_CAPACITY)
        val fraction = temp.toFloat() / NuclearReactorSync.HEAT_CAPACITY

        context.fill(barX, barY, barX + w, barY + h, 0xFF333333.toInt())
        val filledH = (fraction * h).toInt()
        if (filledH > 0) {
            val fillY = barY + h - filledH
            context.enableScissor(barX, fillY, barX + w, barY + h)
            val strips = maxOf(2, h)
            for (i in 0 until strips) {
                val t = i.toFloat() / (strips - 1).coerceAtLeast(1)
                val color = lerpArgb(0xFF0066CC.toInt(), 0xFFCC0000.toInt(), t)
                val y1 = fillY + (i * h / strips)
                val y2 = fillY + ((i + 1) * h / strips).coerceAtMost(fillY + h)
                context.fill(barX, y1, barX + w, y2, color)
            }
            context.disableScissor()
        }
        context.drawBorder(barX, barY, w, h, 0xFF888888.toInt())
    }

    private fun lerpArgb(a: Int, b: Int, t: Float): Int {
        val u = t.coerceIn(0f, 1f)
        val aa = (a shr 24) and 0xFF
        val ar = (a shr 16) and 0xFF
        val ag = (a shr 8) and 0xFF
        val ab = a and 0xFF
        val ba = (b shr 24) and 0xFF
        val br = (b shr 16) and 0xFF
        val bg = (b shr 8) and 0xFF
        val bb = b and 0xFF
        return ((aa + (ba - aa) * u).toInt() and 0xFF shl 24) or
            ((ar + (br - ar) * u).toInt() and 0xFF shl 16) or
            ((ag + (bg - ag) * u).toInt() and 0xFF shl 8) or
            ((ab + (bb - ab) * u).toInt() and 0xFF)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(context, mouseX, mouseY, delta)
        val left = x
        val top = y + guiOffsetY  // 内容下移避免顶部溢出
        val energy = handler.sync.energy.toLong().coerceAtLeast(0)
        val cap = NuclearReactorSync.ENERGY_CAPACITY
        val temp = handler.sync.temperature.coerceIn(0, NuclearReactorSync.HEAT_CAPACITY)
        val outputRate = handler.sync.outputRate.toLong()
        val slotSize = NuclearReactorScreenHandler.SLOT_SIZE
        val barH = 9 * slotSize

        // 标题（下移避免溢出） 暂不渲染
        // ui.render(context, textRenderer, mouseX, mouseY) {
        //     Column(x = left + 8, y = top + 8, spacing = 2, absolute = true) {
        //         Text(title.string, color = 0xFFFFFF)
        //         Text(
        //             "输出 ${NuclearReactorSync.MAX_EXTRACT} EU/t",
        //             color = 0xCCCCCC,
        //             shadow = false
        //         )
        //     }
        // }

        // 能量数值与输出速率竖排叠加在能量条上（条与 drawBackground 同位置）
        val energyBarX = left + 9
        val energyBarY = y + NuclearReactorScreenHandler.SLOT_GRID_Y
        drawVerticalTextOnBar(context, energyBarX, energyBarY, barWidth, barH, listOf(formatEu(energy), formatEu(cap), "EU", "${formatEu(outputRate)} EU/t"))

        // 温度数值竖排叠加在温度条上
        val tempBarX = left + NuclearReactorScreenHandler.SLOT_GRID_X + 9 * slotSize + 4
        val tempBarY = y + NuclearReactorScreenHandler.SLOT_GRID_Y
        drawVerticalTextOnBar(context, tempBarX, tempBarY, barWidth, barH, listOf("$temp", "堆温"))

        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    /** 在竖条上竖排叠加显示文本（多行居中） */
    private fun drawVerticalTextOnBar(context: DrawContext, barX: Int, barY: Int, barW: Int, barH: Int, lines: List<String>) {
        val lineH = textRenderer.fontHeight
        val totalH = lines.size * lineH + (lines.size - 1) * 2
        var y = barY + (barH - totalH) / 2
        for (line in lines) {
            val lineW = textRenderer.getWidth(line)
            val x = barX + (barW - lineW) / 2
            context.drawText(textRenderer, line, x, y, 0xFFFFFF, false)
            y += lineH + 2
        }
    }

    private fun formatEu(value: Long): String = when {
        value >= 1_000_000 -> String.format("%.1fM", value / 1_000_000.0)
        value >= 1_000 -> String.format("%.1fK", value / 1_000.0)
        else -> value.toString()
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean =
        ui.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button)
}
