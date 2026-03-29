package ic2_120.client.screen

import ic2_120.client.compose.*
import ic2_120.client.ui.GuiBackground
import ic2_120.content.block.PatternStorageBlock
import ic2_120.content.block.machines.PatternStorageBlockEntity
import ic2_120.content.screen.PatternStorageScreenHandler
import ic2_120.content.uu.UuTemplateEntry
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.item.ItemStack
import net.minecraft.network.packet.c2s.play.ButtonClickC2SPacket
import net.minecraft.registry.Registries
import net.minecraft.text.Text
import net.minecraft.util.Identifier

@ModScreen(block = PatternStorageBlock::class)
class PatternStorageScreen(
    handler: PatternStorageScreenHandler,
    playerInventory: net.minecraft.entity.player.PlayerInventory,
    title: Text
) : HandledScreen<PatternStorageScreenHandler>(handler, playerInventory, title) {

    private val ui = ComposeUI()

    init {
        backgroundWidth = GUI_SIZE.width
        backgroundHeight = GUI_SIZE.height
        titleY = 4
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        GuiBackground.drawVanillaLikePanel(context, x, y, backgroundWidth, backgroundHeight)
        GuiBackground.drawPlayerInventorySlotBorders(
            context,
            x,
            y,
            PatternStorageScreenHandler.PLAYER_INV_Y,
            PatternStorageScreenHandler.HOTBAR_Y,
            18
        )
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val world = client?.world ?: client?.player?.world
        val storage = world?.let(handler::getPatternStorage)
        val templates = storage?.getTemplatesSnapshot().orEmpty()
        val selectedIndex = storage?.selectedTemplateIndex ?: -1

        val content: UiScope.() -> Unit = {
            Flex(
                x = x + 8,
                y = y + 6,
                direction = FlexDirection.ROW,
                gap = 8,
                modifier = Modifier.EMPTY.width(GUI_SIZE.contentWidth).height(GUI_SIZE.height - 16)
            ) {
                Column(
                    spacing = 6,
                    modifier = Modifier.EMPTY.width(GuiSize.STANDARD.contentWidth)
                ) {
                    Text(title.string, color = 0xFFFFFF)
                    Text("模板数: ${templates.size}", color = 0xAAAAAA, shadow = false)

                    Row(spacing = 4) {
                        SlotAnchor(id = slotAnchorId(PatternStorageScreenHandler.SLOT_CRYSTAL_INDEX), width = 18, height = 18)
                        Button("写入水晶", modifier = Modifier.EMPTY.width(70)) {
                            client?.player?.networkHandler?.sendPacket(
                                ButtonClickC2SPacket(handler.syncId, PatternStorageScreenHandler.BUTTON_EXPORT_TO_CRYSTAL)
                            )
                        }
                        Button("导入水晶", modifier = Modifier.EMPTY.width(70)) {
                            client?.player?.networkHandler?.sendPacket(
                                ButtonClickC2SPacket(handler.syncId, PatternStorageScreenHandler.BUTTON_IMPORT_FROM_CRYSTAL)
                            )
                        }
                    }

                    ScrollView(
                        width = GuiSize.STANDARD.contentWidth,
                        height = 1,
                        scrollbarWidth = 8,
                        modifier = Modifier.EMPTY.fractionHeight(1.0f)
                    ) {
                        Column(spacing = 3) {
                            if (templates.isEmpty()) {
                                Text("暂无模板", color = 0x666666, shadow = false)
                            } else {
                                templates.forEachIndexed { index, template ->
                                    Button(
                                        text = templateLine(index, selectedIndex, template),
                                        modifier = Modifier.EMPTY.width(GuiSize.STANDARD.contentWidth - 10),
                                        onClick = {
                                            client?.player?.networkHandler?.sendPacket(
                                                ButtonClickC2SPacket(
                                                    handler.syncId,
                                                    PatternStorageScreenHandler.BUTTON_SELECT_BASE + index
                                                )
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Column(
                    spacing = 4,
                    modifier = Modifier.EMPTY.width(60)
                ) {
                    val selected = templates.getOrNull(selectedIndex)
                    Text("当前模板", color = 0xAAAAAA, shadow = false)
                    if (selected != null) {
                        val stack = templateToStack(selected)
                        if (!stack.isEmpty) {
                            ItemStack(stack, size = 18)
                        }
                        Text(selected.displayName().string, color = 0xFFFFFF, shadow = false)
                        Text("${selected.uuCostUb} uB", color = 0xFFAA33, shadow = false)
                    } else {
                        Text("<空>", color = 0x666666, shadow = false)
                    }
                }
            }
        }

        val layout = ui.layout(context, textRenderer, mouseX, mouseY, content = content)
        handler.slots.forEachIndexed { index, slot ->
            val anchor = layout.anchors[slotAnchorId(index)] ?: return@forEachIndexed
            slot.x = anchor.x - x
            slot.y = anchor.y - y
        }

        super.render(context, mouseX, mouseY, delta)
        ui.render(context, textRenderer, mouseX, mouseY, content = content)
        drawMouseoverTooltip(context, mouseX, mouseY)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean =
        ui.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button)

    override fun mouseScrolled(mouseX: Double, mouseY: Double, amount: Double): Boolean =
        ui.mouseScrolled(mouseX, mouseY, 0.0, amount) || super.mouseScrolled(mouseX, mouseY, amount)

    override fun mouseDragged(
        mouseX: Double,
        mouseY: Double,
        button: Int,
        deltaX: Double,
        deltaY: Double
    ): Boolean = ui.mouseDragged(mouseX, mouseY, button) || super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        ui.stopDrag()
        return super.mouseReleased(mouseX, mouseY, button)
    }

    private fun slotAnchorId(slotIndex: Int): String = "slot.$slotIndex"

    private fun templateLine(index: Int, selectedIndex: Int, template: UuTemplateEntry): String {
        val prefix = if (index == selectedIndex) "> " else "  "
        return "$prefix${template.displayName().string} (${template.uuCostUb} uB)"
    }

    private fun templateToStack(template: UuTemplateEntry): ItemStack {
        val id = Identifier.tryParse(template.itemId) ?: return ItemStack.EMPTY
        val item = Registries.ITEM.getOrEmpty(id).orElse(null) ?: return ItemStack.EMPTY
        return if (item == net.minecraft.item.Items.AIR) ItemStack.EMPTY else ItemStack(item)
    }

    companion object {
        private val GUI_SIZE = GuiSize.LARGE
    }
}
