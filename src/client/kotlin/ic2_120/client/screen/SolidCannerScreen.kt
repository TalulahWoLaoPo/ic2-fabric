package ic2_120.client.screen

import ic2_120.client.compose.*
import ic2_120.client.ui.EnergyBar
import ic2_120.client.ui.GuiBackground
import ic2_120.client.ui.ProgressBar
import ic2_120.content.block.SolidCannerBlock
import ic2_120.content.screen.SolidCannerScreenHandler
import ic2_120.content.sync.SolidCannerSync
import ic2_120.content.screen.slot.UpgradeSlotLayout
import ic2_120.registry.annotation.ModScreen
import ic2_120.registry.type
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text

@ModScreen(block = SolidCannerBlock::class)
class SolidCannerScreen(
    handler: SolidCannerScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<SolidCannerScreenHandler>(handler, playerInventory, title) {

    private val ui = ComposeUI()

    init {
        backgroundWidth = PANEL_WIDTH
        backgroundHeight = PANEL_HEIGHT
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        GuiBackground.draw(context, x, y, backgroundWidth, backgroundHeight)
        GuiBackground.drawPlayerInventorySlotBorders(
            context, x, y,
            SolidCannerScreenHandler.PLAYER_INV_Y,
            SolidCannerScreenHandler.HOTBAR_Y,
            SolidCannerScreenHandler.SLOT_SIZE
        )
        val borderColor = GuiBackground.BORDER_COLOR
        val slotSize = SolidCannerScreenHandler.SLOT_SIZE
        val borderOffset = 1
        val tinCanSlot = handler.slots[SolidCannerScreenHandler.SLOT_TIN_CAN_INDEX]
        val foodSlot = handler.slots[SolidCannerScreenHandler.SLOT_FOOD_INDEX]
        val outputSlot = handler.slots[SolidCannerScreenHandler.SLOT_OUTPUT_INDEX]
        val dischargingSlot = handler.slots[SolidCannerScreenHandler.SLOT_DISCHARGING_INDEX]
        context.drawBorder(x + tinCanSlot.x - borderOffset, y + tinCanSlot.y - borderOffset, slotSize, slotSize, borderColor)
        context.drawBorder(x + foodSlot.x - borderOffset, y + foodSlot.y - borderOffset, slotSize, slotSize, borderColor)
        context.drawBorder(x + outputSlot.x - borderOffset, y + outputSlot.y - borderOffset, slotSize, slotSize, borderColor)
        context.drawBorder(x + dischargingSlot.x - borderOffset, y + dischargingSlot.y - borderOffset, slotSize, slotSize, borderColor)
        for (i in SolidCannerScreenHandler.SLOT_UPGRADE_INDEX_START..SolidCannerScreenHandler.SLOT_UPGRADE_INDEX_END) {
            val slot = handler.slots[i]
            context.drawBorder(x + slot.x - borderOffset, y + slot.y - borderOffset, slotSize, slotSize, borderColor)
        }
        val progress = handler.sync.progress.coerceIn(0, SolidCannerSync.PROGRESS_MAX)
        val progressFrac = if (SolidCannerSync.PROGRESS_MAX > 0) (progress.toFloat() / SolidCannerSync.PROGRESS_MAX).coerceIn(0f, 1f) else 0f
        val barX = x + foodSlot.x + slotSize + 2
        val barW = outputSlot.x - (foodSlot.x + slotSize) - 4
        val barH = 8
        val barY = y + foodSlot.y + (slotSize - barH) / 2
        ProgressBar.draw(context, barX, barY, barW, barH, progressFrac)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(context, mouseX, mouseY, delta)
        val left = x
        val top = y
        val energy = handler.sync.energy.toLong().coerceAtLeast(0)
        val inputRate = handler.sync.getSyncedInsertedAmount()
        val consumeRate = handler.sync.getSyncedConsumedAmount()
        val cap = handler.sync.energyCapacity.toLong().coerceAtLeast(1L)
        val energyFraction = if (cap > 0) (energy.toFloat() / cap).coerceIn(0f, 1f) else 0f
        val contentW = (backgroundWidth - 16).coerceAtLeast(0)
        val barW = (contentW - 36).coerceAtLeast(0)

        val inputText = "输入 ${formatEu(inputRate)} EU/t"
        val consumeText = "耗能 ${formatEu(consumeRate)} EU/t"
        val inputTextWidth = inputText.length * 6
        val consumeTextWidth = consumeText.length * 6
        val textX = left - maxOf(inputTextWidth, consumeTextWidth) - 4
        context.drawText(textRenderer, inputText, textX, top + 8, 0xAAAAAA, false)
        context.drawText(textRenderer, consumeText, textX, top + 20, 0xAAAAAA, false)

        ui.render(context, textRenderer, mouseX, mouseY) {
            Column(x = left + 8, y = top + 8, spacing = 6) {
                Text(title.string, color = 0xFFFFFF)
                Flex(
                    direction = FlexDirection.ROW,
                    alignItems = AlignItems.CENTER,
                    gap = 8,
                    modifier = Modifier.EMPTY.width(contentW)
                ) {
                    Text("能量", color = 0xAAAAAA)
                    EnergyBar(
                        energyFraction,
                        barWidth = 0,
                        barHeight = 9,
                        modifier = Modifier.EMPTY.width(barW)
                    )
                }
                Text(
                    "$energy / $cap EU",
                    color = 0xCCCCCC,
                    shadow = false
                )
            }
        }
        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean =
        ui.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button)

    companion object {
        private val PANEL_WIDTH = UpgradeSlotLayout.VANILLA_UI_WIDTH + UpgradeSlotLayout.SLOT_SPACING
        private const val PANEL_HEIGHT = 166
    }

    private fun formatEu(value: Long): String {
        return when {
            value >= 1_000_000 -> String.format("%.1fM", value / 1_000_000.0)
            value >= 1_000 -> String.format("%.1fK", value / 1_000.0)
            else -> value.toString()
        }
    }
}
