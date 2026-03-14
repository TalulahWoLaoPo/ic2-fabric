package ic2_120.client

import ic2_120.client.compose.*
import ic2_120.client.ui.GuiBackground
import ic2_120.content.block.FluidHeatGeneratorBlock
import ic2_120.content.block.machines.FluidHeatGeneratorBlockEntity
import ic2_120.content.screen.FluidHeatGeneratorScreenHandler
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text as McText

@ModScreen(block = FluidHeatGeneratorBlock::class)
class FluidHeatGeneratorScreen(
    handler: FluidHeatGeneratorScreenHandler,
    playerInventory: PlayerInventory,
    title: McText
) : HandledScreen<FluidHeatGeneratorScreenHandler>(handler, playerInventory, title) {
    private val ui = ComposeUI()

    init {
        backgroundWidth = 176
        backgroundHeight = 166
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        GuiBackground.draw(context, x, y, backgroundWidth, backgroundHeight)
        GuiBackground.drawPlayerInventorySlotBorders(context, x, y, 84, 142, 18)

        val borderColor = GuiBackground.BORDER_COLOR
        val slotSize = 18
        val borderOffset = 1

        // 燃料容器槽边框
        val fuelSlot = handler.slots[FluidHeatGeneratorBlockEntity.FUEL_SLOT]
        context.drawBorder(x + fuelSlot.x - borderOffset, y + fuelSlot.y - borderOffset, slotSize, slotSize, borderColor)

        // 空容器槽边框
        val emptyContainerSlot = handler.slots[FluidHeatGeneratorBlockEntity.EMPTY_CONTAINER_SLOT]
        context.drawBorder(x + emptyContainerSlot.x - borderOffset, y + emptyContainerSlot.y - borderOffset, slotSize, slotSize, borderColor)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(context, mouseX, mouseY, delta)
        val fuel = handler.sync.fuelAmountMb.coerceAtLeast(0)
        val generatedRate = handler.sync.getSyncedGeneratedHeat()
        val outputRate = handler.sync.getSyncedOutputHeat()

        ui.render(context, textRenderer, mouseX, mouseY) {
            Column(x = x + 8, y = y + 8, spacing = 6) {
                Text(title.string, color = 0xFFFFFF)
                Text("燃料: $fuel mB", color = 0xCCCCCC, shadow = false)
                if (generatedRate > 0L || outputRate > 0L) {
                    Text("产热 $generatedRate HU/t · 输出 $outputRate HU/t", color = 0xAAAAAA, shadow = false)
                } else {
                    Text("速率: 32 HU/t, 10 mB/s", color = 0xAAAAAA, shadow = false)
                }
            }
        }
        drawMouseoverTooltip(context, mouseX, mouseY)
    }
}

