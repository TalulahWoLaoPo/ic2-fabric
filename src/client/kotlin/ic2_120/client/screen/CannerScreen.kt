package ic2_120.client.screen

import ic2_120.client.ui.GuiBackground
import ic2_120.client.ui.ProgressBar
import ic2_120.content.block.CannerBlock
import ic2_120.content.screen.CannerScreenHandler
import ic2_120.content.screen.slot.UpgradeSlotLayout
import ic2_120.content.sync.CannerSync
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text

@ModScreen(block = CannerBlock::class)
class CannerScreen(
    handler: CannerScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<CannerScreenHandler>(handler, playerInventory, title) {

    init {
        backgroundWidth = PANEL_WIDTH
        backgroundHeight = PANEL_HEIGHT
        titleY = -1000
        playerInventoryTitleY = -1000
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        GuiBackground.draw(context, x, y, backgroundWidth, backgroundHeight)
        GuiBackground.drawPlayerInventorySlotBorders(
            context, x, y,
            CannerScreenHandler.PLAYER_INV_Y,
            CannerScreenHandler.HOTBAR_Y,
            CannerScreenHandler.SLOT_SIZE
        )

        drawTopMachinePanel(context)
        drawSlotBorders(context)
        drawProgress(context)
        drawTanks(context)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(context, mouseX, mouseY, delta)

        val energy = handler.sync.energy.toLong().coerceAtLeast(0)
        val cap = handler.sync.energyCapacity.toLong().coerceAtLeast(1)
        val energyFraction = (energy.toFloat() / cap).coerceIn(0f, 1f)

        val panelLeft = x + 8
        val panelTop = y + 8
        val panelRight = x + 170

        val titleText = "流体/固体装罐机"
        val energyText = "$energy/$cap EU"

        context.drawText(textRenderer, titleText, panelLeft + 2, panelTop + 2, 0xE6E6E6, false)
        context.drawText(textRenderer, energyText, panelRight - textRenderer.getWidth(energyText) - 2, panelTop + 2, 0xD0D0D0, false)

        val energyBarW = 44
        val energyBarH = 8
        val energyBarX = panelLeft + (170 - energyBarW) / 2
        val energyBarY = panelTop + 2
        context.fill(energyBarX, energyBarY, energyBarX + energyBarW, energyBarY + energyBarH, 0xFF2A2A2A.toInt())
        val fillW = (energyBarW * energyFraction).toInt().coerceIn(0, energyBarW)
        if (fillW > 0) {
            context.fill(energyBarX + 1, energyBarY + 1, energyBarX + fillW, energyBarY + energyBarH - 1, 0xFF7FD34E.toInt())
        }
        context.drawBorder(energyBarX, energyBarY, energyBarW, energyBarH, 0xFF8A8A8A.toInt())

        val inputRate = handler.sync.getSyncedInsertedAmount()
        val consumeRate = handler.sync.getSyncedConsumedAmount()
        context.drawText(textRenderer, "输入 ${formatEu(inputRate)} EU/t", x - 86, y + 8, 0xA8A8A8, false)
        context.drawText(textRenderer, "耗能 ${formatEu(consumeRate)} EU/t", x - 86, y + 20, 0xA8A8A8, false)

        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    private fun drawTopMachinePanel(context: DrawContext) {
        val panelX = x + 8
        val panelY = y + 8
        val panelW = 162
        val sepY = y + CannerScreenHandler.PLAYER_INV_Y - 8
        val panelH = sepY - panelY

        context.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xFF3E3E40.toInt())
        context.drawBorder(panelX, panelY, panelW, panelH, 0xFF8A8A8A.toInt())

        val innerX = panelX + 7
        val innerY = panelY + 16
        val innerW = panelW - 14
        val innerH = panelH - 24
        context.fill(innerX, innerY, innerX + innerW, innerY + innerH, 0xFF474749.toInt())
        context.drawBorder(innerX, innerY, innerW, innerH, 0xFF7E7E7E.toInt())

        context.drawHorizontalLine(panelX, sepY, panelX + panelW, 0xFF676767.toInt())
    }

    private fun drawSlotBorders(context: DrawContext) {
        val borderColor = GuiBackground.BORDER_COLOR
        val slotSize = CannerScreenHandler.SLOT_SIZE
        val borderOffset = 1

        val containerSlot = handler.slots[CannerScreenHandler.SLOT_CONTAINER_INDEX]
        val materialSlot = handler.slots[CannerScreenHandler.SLOT_MATERIAL_INDEX]
        val outputSlot = handler.slots[CannerScreenHandler.SLOT_OUTPUT_INDEX]
        val dischargingSlot = handler.slots[CannerScreenHandler.SLOT_DISCHARGING_INDEX]

        context.drawBorder(x + containerSlot.x - borderOffset, y + containerSlot.y - borderOffset, slotSize, slotSize, borderColor)
        context.drawBorder(x + materialSlot.x - borderOffset, y + materialSlot.y - borderOffset, slotSize, slotSize, borderColor)
        context.drawBorder(x + outputSlot.x - borderOffset, y + outputSlot.y - borderOffset, slotSize, slotSize, borderColor)
        context.drawBorder(x + dischargingSlot.x - borderOffset, y + dischargingSlot.y - borderOffset, slotSize, slotSize, borderColor)

        for (i in CannerScreenHandler.SLOT_UPGRADE_INDEX_START..CannerScreenHandler.SLOT_UPGRADE_INDEX_END) {
            val slot = handler.slots[i]
            context.drawBorder(x + slot.x - borderOffset, y + slot.y - borderOffset, slotSize, slotSize, borderColor)
        }
    }

    private fun drawTanks(context: DrawContext) {
        val leftFluidFraction = run {
            val amt = handler.sync.rightFluidAmountMb.toLong()
            val cap = handler.sync.rightFluidCapacityMb.toLong().coerceAtLeast(1)
            (amt.toFloat() / cap).coerceIn(0f, 1f)
        }
        val rightFluidFraction = run {
            val amt = handler.sync.leftFluidAmountMb.toLong()
            val cap = handler.sync.leftFluidCapacityMb.toLong().coerceAtLeast(1)
            (amt.toFloat() / cap).coerceIn(0f, 1f)
        }

        drawVerticalTank(
            context,
            x + CannerScreenHandler.LEFT_TANK_X,
            y + CannerScreenHandler.LEFT_TANK_Y,
            CannerScreenHandler.FLUID_BAR_W,
            CannerScreenHandler.FLUID_BAR_H,
            leftFluidFraction
        )
        drawVerticalTank(
            context,
            x + CannerScreenHandler.RIGHT_TANK_X,
            y + CannerScreenHandler.RIGHT_TANK_Y,
            CannerScreenHandler.FLUID_BAR_W,
            CannerScreenHandler.FLUID_BAR_H,
            rightFluidFraction
        )
    }

    private fun drawProgress(context: DrawContext) {
        val progress = handler.sync.progress.coerceIn(0, CannerSync.PROGRESS_MAX)
        val progressFrac = if (CannerSync.PROGRESS_MAX > 0) {
            (progress.toFloat() / CannerSync.PROGRESS_MAX).coerceIn(0f, 1f)
        } else {
            0f
        }

        ProgressBar.draw(
            context,
            x + CannerScreenHandler.PROGRESS_BAR_X,
            y + CannerScreenHandler.PROGRESS_BAR_Y,
            CannerScreenHandler.PROGRESS_BAR_W,
            CannerScreenHandler.PROGRESS_BAR_H,
            progressFrac
        )
    }

    private fun drawVerticalTank(context: DrawContext, px: Int, py: Int, w: Int, h: Int, fraction: Float) {
        val f = fraction.coerceIn(0f, 1f)
        context.fill(px, py, px + w, py + h, 0xFF1E1E1F.toInt())
        val filledH = (h * f).toInt()
        if (filledH > 0) {
            context.fill(px + 1, py + h - filledH, px + w - 1, py + h - 1, 0xFF31A14B.toInt())
        }
        context.drawBorder(px, py, w, h, 0xFF8A8A8A.toInt())
    }

    companion object {
        private val PANEL_WIDTH = UpgradeSlotLayout.VANILLA_UI_WIDTH + UpgradeSlotLayout.SLOT_SPACING
        private const val PANEL_HEIGHT = 214
    }

    private fun formatEu(value: Long): String {
        return when {
            value >= 1_000_000 -> String.format("%.1fM", value / 1_000_000.0)
            value >= 1_000 -> String.format("%.1fK", value / 1_000.0)
            else -> value.toString()
        }
    }
}
