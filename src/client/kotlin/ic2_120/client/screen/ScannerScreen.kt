package ic2_120.client.screen

import ic2_120.client.compose.*
import ic2_120.client.ui.EnergyBar
import ic2_120.client.ui.GuiBackground
import ic2_120.content.item.OdScannerItem
import ic2_120.content.network.OreScanEntry
import ic2_120.content.network.ScannerResultPacket
import ic2_120.content.screen.ScannerScreenHandler
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.item.ItemStack
import net.minecraft.network.packet.c2s.play.ButtonClickC2SPacket
import net.minecraft.registry.Registries
import net.minecraft.text.Text
import net.minecraft.util.Identifier

/**
 * 扫描仪 GUI（客户端）。
 *
 * 上部：能量条 + 使用次数 + 扫描按钮
 * 下部：扫描结果列表（矿物名称 + 数量，可滚动）
 */
@ModScreen(handler = "scanner")
class ScannerScreen(
    handler: ScannerScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<ScannerScreenHandler>(handler, playerInventory, title) {

    private val ui = ComposeUI()

    init {
        backgroundWidth = ScannerScreenHandler.PANEL_WIDTH
        backgroundHeight = ScannerScreenHandler.PANEL_HEIGHT
        titleY = 4
        playerInventoryTitleY = -1000  // 隐藏背包标题
    }

    /** UI 整体上移的像素偏移 */
    private val topOffset = 40

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        GuiBackground.draw(context, x, y - topOffset, backgroundWidth, backgroundHeight)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val energy = handler.sync.energy.toLong().coerceAtLeast(0)
        val cap = handler.sync.energyCapacity.toLong().coerceAtLeast(1)
        val energyFraction = if (cap > 0) (energy.toFloat() / cap).coerceIn(0f, 1f) else 0f
        val type = OdScannerItem.getScannerType(handler.playerInventory.getStack(handler.playerInventory.selectedSlot))
        val usesRemaining = handler.sync.usesRemaining
        val maxUses = handler.sync.maxUses
        val scannerTitle = if (type.tier == 3) "OV 扫描仪" else "OD 扫描仪"
        val scanRangeText = "${type.scanRadius * 2 + 1} × ${type.scanRadius * 2 + 1}"
        val canScan = energy >= type.energyPerScan && usesRemaining > 0
        val results = lastResults

        val left = x
        val top = y - topOffset

        // 预布局通道（当前 scanner 暂无 slot 锚点，渲染时序与动态 slot 方案保持一致）
        ui.layout(context, textRenderer, mouseX, mouseY) {
            buildUi(left, top, energy, cap, energyFraction, scannerTitle, scanRangeText, canScan, usesRemaining, maxUses, results)
        }

        super.render(context, mouseX, mouseY, delta)

        ui.render(context, textRenderer, mouseX, mouseY) {
            buildUi(left, top, energy, cap, energyFraction, scannerTitle, scanRangeText, canScan, usesRemaining, maxUses, results)
        }

        val tooltip = ui.getTooltipAt(mouseX, mouseY)
        if (tooltip != null) {
            context.drawTooltip(textRenderer, tooltip, mouseX, mouseY)
        } else {
            drawMouseoverTooltip(context, mouseX, mouseY)
        }
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean =
        if (ui.mouseClicked(mouseX, mouseY, button)) true
        else super.mouseClicked(mouseX, mouseY, button)

    override fun mouseScrolled(mouseX: Double, mouseY: Double, amount: Double): Boolean =
        ui.mouseScrolled(mouseX, mouseY, 0.0, amount)
            || super.mouseScrolled(mouseX, mouseY, amount)

    override fun mouseDragged(
        mouseX: Double, mouseY: Double, button: Int,
        deltaX: Double, deltaY: Double
    ): Boolean = ui.mouseDragged(mouseX, mouseY, button)
        || super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        ui.stopDrag()
        return super.mouseReleased(mouseX, mouseY, button)
    }

    // ─── 扫描结果（S2C 包更新）──────────────────────────────────────

    companion object {
        @JvmField
        var lastResults: List<OreScanEntry> = emptyList()

        fun receiveResults(packet: ScannerResultPacket) {
            lastResults = packet.results
        }

        private fun entryToItemStack(entry: OreScanEntry): ItemStack {
            val id = Identifier.tryParse(entry.blockId) ?: return ItemStack.EMPTY
            val block = Registries.BLOCK.getOrEmpty(id).orElse(null) ?: return ItemStack.EMPTY
            val item = block.asItem()
            if (item == net.minecraft.item.Items.AIR) return ItemStack.EMPTY
            return ItemStack(item, entry.count.coerceAtLeast(1))
        }
    }

    // ─── UI 构建 ───────────────────────────────────────────────────

    private fun UiScope.buildUi(
        left: Int,
        top: Int,
        energy: Long,
        cap: Long,
        energyFraction: Float,
        scannerTitle: String,
        scanRangeText: String,
        canScan: Boolean,
        usesRemaining: Int,
        maxUses: Int,
        results: List<OreScanEntry>
    ) {
        // 信息面板（左侧）
        Column(x = left + 8, y = top + 6, spacing = 4) {
            Text(scannerTitle, color = 0xFFFFFF)
            Text(
                "${energy.toInt()} / $cap EU",
                color = 0xCCCCCC,
                shadow = false
            )
            EnergyBar(energyFraction, barWidth = 140, barHeight = 6)
            Text(
                "剩余次数: $usesRemaining / $maxUses",
                color = if (usesRemaining > 0) 0xAAAAAA else 0xFF4A4A,
                shadow = false
            )
            Text(
                "扫描范围: $scanRangeText",
                color = 0x888888,
                shadow = false
            )
            Button(
                text = if (canScan) "扫描" else "能量不足",
                modifier = Modifier.EMPTY.width(100),
                onClick = {
                    client?.player?.networkHandler?.sendPacket(
                        ButtonClickC2SPacket(handler.syncId, ScannerScreenHandler.BUTTON_ID_SCAN)
                    )
                }
            )
        }

        // 扫描结果区（可滚动）
        val resultsX = left + 8
        val resultsY = top + 82
        val resultsW = backgroundWidth - 16
        val resultsH = (backgroundHeight - 94).coerceAtLeast(40)

        if (results.isNotEmpty()) {
            // 带滚动支持的列表
            ScrollView(
                width = resultsW,
                height = resultsH,
                scrollbarWidth = 8,
                x = resultsX,
                y = resultsY
            ) {
                Column(spacing = 6) {
                    Text("扫描结果:", color = 0xFFFFFF)
                    for (entry in results) {
                        val oreStack = entryToItemStack(entry)
                        if (!oreStack.isEmpty) {
                            Row(
                                spacing = 6,
                                modifier = Modifier.EMPTY.width(resultsW - 8)
                            ) {
                                ItemStack(oreStack, size = 16)
                                Text(
                                    entry.blockId.substringAfterLast("/")
                                        .replace("_", " ")
                                        .replaceFirstChar { it.uppercase() },
                                    color = 0xDDDDDD,
                                    shadow = false
                                )
                                // 数量右对齐
                                Text(
                                    if (entry.count >= 1000) "${entry.count / 1000}k"
                                    else entry.count.toString(),
                                    color = 0xFFAA33,
                                    shadow = false,
                                    modifier = Modifier.EMPTY
                                )
                            }
                        }
                    }
                }
            }
        } else {
            Text(
                "点击「扫描」开始扫描周围矿物",
                x = resultsX,
                y = resultsY,
                color = 0x666666,
                shadow = false
            )
        }
    }
}
