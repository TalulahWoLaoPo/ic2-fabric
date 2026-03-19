package ic2_120.client.screen

import ic2_120.client.compose.*
import ic2_120.client.ui.EnergyBar
import ic2_120.client.ui.GuiBackground
import ic2_120.client.ui.ProgressBar
import ic2_120.content.sync.InductionFurnaceSync
import ic2_120.content.block.InductionFurnaceBlock
import ic2_120.content.screen.InductionFurnaceScreenHandler
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text

/**
 * 感应炉 GUI（客户端）。
 * 显示：能量条、热量指示，双槽进度条。
 */
@ModScreen(block = InductionFurnaceBlock::class)
class InductionFurnaceScreen(
    handler: InductionFurnaceScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<InductionFurnaceScreenHandler>(handler, playerInventory, title) {

    private val ui = ComposeUI()

    init {
        backgroundWidth = PANEL_WIDTH
        backgroundHeight = PANEL_HEIGHT
        titleY = 4
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        GuiBackground.draw(context, x, y, backgroundWidth, backgroundHeight)
        GuiBackground.drawPlayerInventorySlotBorders(
            context,
            x,
            y,
            InductionFurnaceScreenHandler.PLAYER_INV_Y,
            InductionFurnaceScreenHandler.HOTBAR_Y,
            InductionFurnaceScreenHandler.SLOT_SIZE
        )

        val borderColor = GuiBackground.BORDER_COLOR
        val slotSize = InductionFurnaceScreenHandler.SLOT_SIZE
        val borderOffset = 1

        // 绘制机器槽位边框
        val inputSlot0 = handler.slots[InductionFurnaceScreenHandler.SLOT_INPUT_0_INDEX]
        val inputSlot1 = handler.slots[InductionFurnaceScreenHandler.SLOT_INPUT_1_INDEX]
        val outputSlot0 = handler.slots[InductionFurnaceScreenHandler.SLOT_OUTPUT_0_INDEX]
        val outputSlot1 = handler.slots[InductionFurnaceScreenHandler.SLOT_OUTPUT_1_INDEX]
        val dischargingSlot = handler.slots[InductionFurnaceScreenHandler.SLOT_DISCHARGING_INDEX]

        context.drawBorder(x + inputSlot0.x - borderOffset, y + inputSlot0.y - borderOffset, slotSize, slotSize, borderColor)
        context.drawBorder(x + inputSlot1.x - borderOffset, y + inputSlot1.y - borderOffset, slotSize, slotSize, borderColor)
        context.drawBorder(x + outputSlot0.x - borderOffset, y + outputSlot0.y - borderOffset, slotSize, slotSize, borderColor)
        context.drawBorder(x + outputSlot1.x - borderOffset, y + outputSlot1.y - borderOffset, slotSize, slotSize, borderColor)
        context.drawBorder(x + dischargingSlot.x - borderOffset, y + dischargingSlot.y - borderOffset, slotSize, slotSize, borderColor)

        // 双槽进度条（与 BlockEntity 一致：progressNeeded = baseTicks * HEAT_MAX / heat）
        val barH = 6
        val barW = outputSlot0.x - (inputSlot0.x + slotSize) - 4
        val baseTicks = InductionFurnaceSync.BASE_TICKS_PER_OPERATION
        val heat = handler.sync.heat.coerceAtLeast(InductionFurnaceSync.MIN_HEAT_THRESHOLD)
        val progressNeeded = (baseTicks * InductionFurnaceSync.HEAT_MAX / heat).toInt().coerceAtLeast(baseTicks.toInt())

        // 槽 0 进度
        val progress0 = handler.sync.progressSlot0.coerceIn(0, progressNeeded)
        val progressFrac0 = if (progressNeeded > 0) progress0.toFloat() / progressNeeded else 0f
        ProgressBar.draw(context,
            x + inputSlot0.x + slotSize + 2,
            y + inputSlot0.y + (slotSize - barH) / 2,
            barW, barH, progressFrac0)

        // 槽 1 进度
        val progress1 = handler.sync.progressSlot1.coerceIn(0, progressNeeded)
        val progressFrac1 = if (progressNeeded > 0) progress1.toFloat() / progressNeeded else 0f
        ProgressBar.draw(context,
            x + inputSlot1.x + slotSize + 2,
            y + inputSlot1.y + (slotSize - barH) / 2,
            barW, barH, progressFrac1)

        // 热量条（在玩家物品栏上方）
        val heatFactor = handler.sync.heat / InductionFurnaceSync.HEAT_MAX.toFloat()
        val heatBarX = x + 8
        val heatBarY = y + InductionFurnaceScreenHandler.PLAYER_INV_Y - 14
        val heatBarW = backgroundWidth - 16
        val heatBarH = 8
        ProgressBar.drawHeatBar(context, heatBarX, heatBarY, heatBarW, heatBarH, heatFactor)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(context, mouseX, mouseY, delta)

        val left = x
        val top = y

        // EU 耗能显示
        val inputRate = handler.sync.getSyncedInsertedAmount()
        val consumeRate = handler.sync.getSyncedConsumedAmount()
        val inputText = "输入 ${formatEu(inputRate)} EU/t"
        val consumeText = "耗能 ${formatEu(consumeRate)} EU/t"
        val inputTextWidth = inputText.length * 6
        val consumeTextWidth = consumeText.length * 6
        val textX = left - maxOf(inputTextWidth, consumeTextWidth) - 4
        context.drawText(textRenderer, inputText, textX, top + 8, 0xAAAAAA, false)
        context.drawText(textRenderer, consumeText, textX, top + 20, 0xAAAAAA, false)

        // 热量文本
        val heatPercent = handler.sync.heat / 100
        val heatText = "热量 $heatPercent%"
        context.drawText(textRenderer, heatText, left + 8, top + InductionFurnaceScreenHandler.PLAYER_INV_Y - 24, 0xDDDDDD, false)

        val energy = handler.sync.energy.toLong().coerceAtLeast(0)
        val cap = handler.sync.energyCapacity.toLong().coerceAtLeast(1)
        val energyFraction = if (cap > 0) (energy.toFloat() / cap).coerceIn(0f, 1f) else 0f
        val contentW = (backgroundWidth - 16).coerceAtLeast(0)
        val barW = (contentW - 36).coerceAtLeast(0)

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
                Text("$energy / $cap EU", color = 0xCCCCCC, shadow = false)
            }
        }
        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean =
        ui.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button)

    companion object {
        private const val PANEL_WIDTH = 176
        private const val PANEL_HEIGHT = 184
    }

    private fun formatEu(value: Long): String {
        return when {
            value >= 1_000_000 -> String.format("%.1fM", value / 1_000_000.0)
            value >= 1_000 -> String.format("%.1fK", value / 1_000.0)
            else -> value.toString()
        }
    }
}
