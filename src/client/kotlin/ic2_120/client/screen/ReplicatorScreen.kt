package ic2_120.client.screen

import ic2_120.client.EnergyFormatUtils
import ic2_120.client.compose.*
import ic2_120.client.ui.EnergyBar
import ic2_120.client.ui.FluidBar
import ic2_120.client.ui.GuiBackground
import ic2_120.content.block.ReplicatorBlock
import ic2_120.content.screen.ReplicatorScreenHandler
import ic2_120.content.sync.ReplicatorSync
import ic2_120.content.uu.UuTemplateEntry
import ic2_120.content.uu.findUniqueAdjacentPatternStorage
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.item.ItemStack
import net.minecraft.network.packet.c2s.play.ButtonClickC2SPacket
import net.minecraft.registry.Registries
import net.minecraft.text.Text
import net.minecraft.util.Identifier

@ModScreen(block = ReplicatorBlock::class)
class ReplicatorScreen(
    handler: ReplicatorScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<ReplicatorScreenHandler>(handler, playerInventory, title) {

    private val ui = ComposeUI()

    init {
        backgroundWidth = GUI_SIZE.width
        backgroundHeight = GUI_SIZE.height
        titleY = 4
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        GuiBackground.drawVanillaLikePanel(context, x, y, backgroundWidth, backgroundHeight)
        GuiBackground.drawPlayerInventorySlotBorders(
            context, x, y, ReplicatorScreenHandler.PLAYER_INV_Y, ReplicatorScreenHandler.HOTBAR_Y, 18
        )
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val world = client?.world ?: return super.render(context, mouseX, mouseY, delta)
        val storage = findUniqueAdjacentPatternStorage(world, handler.blockPos)
        val templates = storage?.getTemplatesSnapshot().orEmpty()
        val selectedIndex = storage?.selectedTemplateIndex ?: -1
        val energy = handler.sync.energy.toLong().coerceAtLeast(0L)
        val cap = handler.sync.energyCapacity.toLong().coerceAtLeast(1L)
        val energyFraction = (energy.toFloat() / cap.toFloat()).coerceIn(0f, 1f)
        val fluidFraction = if (handler.sync.fluidCapacityMb > 0) {
            handler.sync.fluidAmountMb.toFloat() / handler.sync.fluidCapacityMb.toFloat()
        } else {
            0f
        }.coerceIn(0f, 1f)
        val progressFraction = if (handler.sync.progressMaxUb > 0) {
            handler.sync.progressUb.toFloat() / handler.sync.progressMaxUb.toFloat()
        } else {
            0f
        }.coerceIn(0f, 1f)

        val content: UiScope.() -> Unit = {
            Row(
                x = x + 8,
                y = y + 6,
                spacing = 8,
                modifier = Modifier.EMPTY.width(GUI_SIZE.contentWidth)
            ) {
                Column(spacing = 4, modifier = Modifier.EMPTY.width(112)) {
                    Text(title.string, color = 0xFFFFFF)
                    Text(statusText(handler.sync.status), color = statusColor(handler.sync.status), shadow = false)
                    Text("模式: ${modeText(handler.sync.mode)}", color = 0xAAAAAA, shadow = false)
                    Button("切换模式", modifier = Modifier.EMPTY.width(80)) {
                        client?.player?.networkHandler?.sendPacket(
                            ButtonClickC2SPacket(handler.syncId, ReplicatorScreenHandler.BUTTON_MODE_TOGGLE)
                        )
                    }
                    Flex(direction = FlexDirection.ROW, justifyContent = JustifyContent.SPACE_BETWEEN, gap = 8) {
                        Column(spacing = 2) {
                            Text("UU储罐", color = 0xAAAAAA, shadow = false)
                            FluidBar(
                                fluidFraction,
                                barWidth = 8,
                                barHeight = 52,
                                vertical = true,
                                modifier = Modifier.EMPTY.width(8).height(52)
                            )
                            Text("${handler.sync.fluidAmountMb} mB", color = 0xFFFFFF, shadow = false)
                        }

                        Column(spacing = 4) {
                            Row(spacing = 4) {
                                Text("产物", color = 0xAAAAAA, shadow = false)
                                SlotAnchor(id = slotAnchorId(ReplicatorScreenHandler.SLOT_OUTPUT_INDEX), width = 18, height = 18)
                            }
                            Row(spacing = 4) {
                                Text("进液", color = 0xAAAAAA, shadow = false)
                                SlotAnchor(id = slotAnchorId(ReplicatorScreenHandler.SLOT_CONTAINER_INPUT_INDEX), width = 18, height = 18)
                            }
                            Row(spacing = 4) {
                                Text("空桶", color = 0xAAAAAA, shadow = false)
                                SlotAnchor(id = slotAnchorId(ReplicatorScreenHandler.SLOT_CONTAINER_OUTPUT_INDEX), width = 18, height = 18)
                            }
                            Row(spacing = 4) {
                                Text("电池", color = 0xAAAAAA, shadow = false)
                                SlotAnchor(id = slotAnchorId(ReplicatorScreenHandler.SLOT_BATTERY_INDEX), width = 18, height = 18)
                            }
                        }
                    }
                    Text("$energy / $cap EU", color = 0xCCCCCC, shadow = false)
                    EnergyBar(energyFraction, modifier = Modifier.EMPTY.width(112))
                    Text("进度: ${handler.sync.progressUb} / ${handler.sync.progressMaxUb} uB", color = 0xAAAAAA, shadow = false)
                    EnergyBar(progressFraction, modifier = Modifier.EMPTY.width(112), barHeight = 6)
                }

                Column(spacing = 4, modifier = Modifier.EMPTY.width(GuiSize.STANDARD.contentWidth - 112 - 8)) {
                    Text("模板列表", color = 0xFFFFFF)
                    ScrollView(
                        width = GuiSize.STANDARD.contentWidth - 112 - 8,
                        height = 88,
                        scrollbarWidth = 8
                    ) {
                        Column(spacing = 3) {
                            if (templates.isEmpty()) {
                                Text("未连接模板", color = 0x666666, shadow = false)
                            } else {
                                templates.forEachIndexed { index, template ->
                                    Button(
                                        text = templateLine(index, selectedIndex, template),
                                        modifier = Modifier.EMPTY.width(GuiSize.STANDARD.contentWidth - 112 - 18),
                                        onClick = {
                                            client?.player?.networkHandler?.sendPacket(
                                                ButtonClickC2SPacket(
                                                    handler.syncId,
                                                    ReplicatorScreenHandler.BUTTON_SELECT_BASE + index
                                                )
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Column(spacing = 4, modifier = Modifier.EMPTY.width(GuiSize.UPGRADE_COLUMN_WIDTH).padding(0, 8, 0, 0)) {
                    for (i in ReplicatorScreenHandler.SLOT_UPGRADE_INDEX_START..ReplicatorScreenHandler.SLOT_UPGRADE_INDEX_END) {
                        SlotAnchor(id = slotAnchorId(i), width = 18, height = 18)
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
        context.drawText(
            textRenderer,
            "输入 ${EnergyFormatUtils.formatEu(handler.sync.getSyncedInsertedAmount())} EU/t",
            x - 92,
            y + 8,
            0xAAAAAA,
            false
        )
        context.drawText(
            textRenderer,
            "耗能 ${EnergyFormatUtils.formatEu(handler.sync.getSyncedConsumedAmount())} EU/t",
            x - 92,
            y + 20,
            0xAAAAAA,
            false
        )
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

    private fun modeText(mode: Int): String =
        if (mode == ReplicatorSync.MODE_CONTINUOUS) "连续" else "单次"

    private fun statusText(status: Int): String = when (status) {
        ReplicatorSync.STATUS_NO_REDSTONE -> "等待红石"
        ReplicatorSync.STATUS_NO_STORAGE -> "未连接唯一存储机"
        ReplicatorSync.STATUS_NO_TEMPLATE -> "没有模板"
        ReplicatorSync.STATUS_NO_FLUID -> "UU 物质不足"
        ReplicatorSync.STATUS_NO_OUTPUT -> "输出槽已满"
        ReplicatorSync.STATUS_NO_ENERGY -> "能量不足"
        ReplicatorSync.STATUS_RUNNING -> "复制中"
        ReplicatorSync.STATUS_COMPLETE -> "完成"
        else -> "待机"
    }

    private fun statusColor(status: Int): Int = when (status) {
        ReplicatorSync.STATUS_COMPLETE -> 0x55FF55
        ReplicatorSync.STATUS_RUNNING -> 0x55AAFF
        ReplicatorSync.STATUS_NO_REDSTONE,
        ReplicatorSync.STATUS_NO_STORAGE,
        ReplicatorSync.STATUS_NO_TEMPLATE,
        ReplicatorSync.STATUS_NO_FLUID,
        ReplicatorSync.STATUS_NO_OUTPUT,
        ReplicatorSync.STATUS_NO_ENERGY -> 0xFF5555
        else -> 0xAAAAAA
    }

    companion object {
        private val GUI_SIZE = GuiSize.STANDARD_UPGRADE
    }
}
