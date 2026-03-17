package ic2_120.client.screen

import ic2_120.client.compose.ComposeUI
import ic2_120.client.compose.*
import ic2_120.client.ui.FluidBar
import ic2_120.client.ui.GuiBackground
import ic2_120.content.block.FluidHeatExchangerBlock
import ic2_120.content.block.machines.FluidHeatExchangerBlockEntity
import ic2_120.content.screen.FluidHeatExchangerScreenHandler
import ic2_120.content.sync.FluidHeatExchangerSync
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text as McText

@ModScreen(block = FluidHeatExchangerBlock::class)
class FluidHeatExchangerScreen(
    handler: FluidHeatExchangerScreenHandler,
    playerInventory: PlayerInventory,
    title: McText
) : HandledScreen<FluidHeatExchangerScreenHandler>(handler, playerInventory, title) {

    private val ui = ComposeUI()

    init {
        backgroundWidth = 176
        backgroundHeight = 184
        titleY = 4
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        GuiBackground.draw(context, x, y, backgroundWidth, backgroundHeight)
        GuiBackground.drawPlayerInventorySlotBorders(
            context,
            x,
            y,
            FluidHeatExchangerScreenHandler.PLAYER_INV_Y,
            FluidHeatExchangerScreenHandler.HOTBAR_Y,
            FluidHeatExchangerScreenHandler.SLOT_SIZE
        )

        val borderColor = GuiBackground.BORDER_COLOR
        val borderOffset = 1
        val slotSize = FluidHeatExchangerScreenHandler.SLOT_SIZE

        for (i in FluidHeatExchangerScreenHandler.EXCHANGER_SLOT_INDEX_START..FluidHeatExchangerScreenHandler.SLOT_OUTPUT_FILLED_CONTAINER_INDEX) {
            val slot = handler.slots[i]
            context.drawBorder(x + slot.x - borderOffset, y + slot.y - borderOffset, slotSize, slotSize, borderColor)
        }
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(context, mouseX, mouseY, delta)

        val left = x
        val top = y
        val centerX = left + backgroundWidth / 2
        val leftBarX = left + BAR_SIDE_PADDING
        val rightBarX = left + backgroundWidth - BAR_SIDE_PADDING - BAR_WIDTH
        val barTop = top + BAR_TOP

        val inputFraction = (handler.sync.inputFluidMb.toFloat() / FluidHeatExchangerSync.TANK_CAPACITY_MB).coerceIn(0f, 1f)
        val outputFraction = (handler.sync.outputFluidMb.toFloat() / FluidHeatExchangerSync.TANK_CAPACITY_MB).coerceIn(0f, 1f)
        val exchangerCount = FluidHeatExchangerBlockEntity.SLOT_EXCHANGER_INDICES.count { handler.slots[it].hasStack() }
        val generatedRate = handler.sync.getSyncedGeneratedHeat()
        val outputRate = handler.sync.getSyncedOutputHeat()

        ui.render(context, textRenderer, mouseX, mouseY) {
            FluidBar(
                inputFraction,
                barWidth = BAR_WIDTH,
                barHeight = BAR_HEIGHT,
                vertical = true,
                x = leftBarX,
                y = barTop,
                absolute = true,
                modifier = Modifier.EMPTY.width(BAR_WIDTH).height(BAR_HEIGHT)
            )
            FluidBar(
                outputFraction,
                barWidth = BAR_WIDTH,
                barHeight = BAR_HEIGHT,
                vertical = true,
                x = rightBarX,
                y = barTop,
                absolute = true,
                modifier = Modifier.EMPTY.width(BAR_WIDTH).height(BAR_HEIGHT)
            )
        }

        drawCenteredText(context, title.string, centerX, top + 6, 0xFFFFFF, shadow = true)
        drawCenteredText(context, "换热器: $exchangerCount/10", centerX, top + 14, 0xAAAAAA)
        drawCenteredText(
            context,
            if (handler.sync.isWorking != 0) "状态: 工作中" else "状态: 停止",
            centerX,
            top + 88,
            0xAAAAAA
        )
        drawCenteredText(context, "产热: $generatedRate HU/t  输出热: $outputRate HU/t", centerX, top + 97, 0xAAAAAA)

        val inputCenter = leftBarX + BAR_WIDTH / 2
        val outputCenter = rightBarX + BAR_WIDTH / 2
        drawCenteredText(context, "输入液", inputCenter, top + 14, 0xAAAAAA)
        drawCenteredText(context, "输出液", outputCenter, top + 14, 0xAAAAAA)
        drawCenteredText(context, "${handler.sync.inputFluidMb} mB", inputCenter, barTop + BAR_HEIGHT + 2, 0xCCCCCC)
        drawCenteredText(context, "${handler.sync.outputFluidMb} mB", outputCenter, barTop + BAR_HEIGHT + 2, 0xCCCCCC)

        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean =
        ui.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button)

    private fun drawCenteredText(
        context: DrawContext,
        text: String,
        centerX: Int,
        y: Int,
        color: Int,
        shadow: Boolean = false
    ) {
        val textX = centerX - textRenderer.getWidth(text) / 2
        if (shadow) context.drawTextWithShadow(textRenderer, text, textX, y, color)
        else context.drawText(textRenderer, text, textX, y, color, false)
    }

    companion object {
        private const val BAR_WIDTH = 8
        private const val BAR_HEIGHT = 58
        private const val BAR_TOP = 22
        private const val BAR_SIDE_PADDING = 12
    }
}
