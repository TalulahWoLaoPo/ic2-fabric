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
 * 下部：扫描结果列表（矿物名称 + 数量）
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
        // 能量、次数等由 handler.sync（PropertyDelegate）同步，与其他机器一致
        val energy = handler.sync.energy.toLong().coerceAtLeast(0)
        val cap = handler.sync.energyCapacity.toLong().coerceAtLeast(1)
        val energyFraction = if (cap > 0) (energy.toFloat() / cap).coerceIn(0f, 1f) else 0f
        val stack = handler.playerInventory.getStack(handler.playerInventory.selectedSlot)
        val type = OdScannerItem.getScannerType(stack)
        val usesRemaining = handler.sync.usesRemaining
        val maxUses = handler.sync.maxUses
        val scannerTitle = if (type.tier == 3) "OV 扫描仪" else "OD 扫描仪"
        val scanRangeText = "${type.scanRadius * 2 + 1} × ${type.scanRadius * 2 + 1}"
        val canScan = energy >= type.energyPerScan && usesRemaining > 0
        val results = ScannerScreen.lastResults

        val left = x
        val top = y - topOffset

        // 预布局通道（当前 scanner 暂无 slot 锚点，但渲染时序与动态 slot 方案保持一致）
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

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (ui.mouseClicked(mouseX, mouseY, button)) return true
        return super.mouseClicked(mouseX, mouseY, button)
    }

    companion object {
        /** 最新扫描结果（由 S2C 包更新，能量/次数由 handler.sync 同步） */
        @JvmField
        var lastResults: List<OreScanEntry> = emptyList()

        /**
         * 接收扫描结果（S2C 包回调在渲染线程执行）。
         * 仅更新结果列表；能量、次数由 PropertyDelegate 同步。
         */
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
        Column(x = left + 8, y = top + 6, spacing = 4) {
            Text(scannerTitle, color = 0xFFFFFF)
            Row(spacing = 4, modifier = Modifier.EMPTY) {
                Text("${energy.toInt()} / $cap EU", color = 0xCCCCCC, shadow = false)
            }
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

        if (results.isNotEmpty()) {
            val resultsAreaW = backgroundWidth - 16
            val resultsAreaH = (backgroundHeight - 88).coerceAtLeast(40)
            Column(
                x = left + 8,
                y = top + 80,
                spacing = 4,
                modifier = Modifier.EMPTY.width(resultsAreaW)
            ) {
                Text("扫描结果:", color = 0xFFFFFF)
                Flex(
                    direction = FlexDirection.COLUMN,
                    justifyContent = JustifyContent.SPACE_EVENLY,
                    alignItems = AlignItems.START,
                    gap = 4,
                    modifier = Modifier.EMPTY.width(resultsAreaW).height(resultsAreaH)
                ) {
                    for (row in results.chunked(6)) {
                        Flex(
                            direction = FlexDirection.ROW,
                            justifyContent = JustifyContent.SPACE_BETWEEN,
                            alignItems = AlignItems.CENTER,
                            gap = 8,
                            modifier = Modifier.EMPTY.width(resultsAreaW)
                        ) {
                            for (entry in row) {
                                val oreStack = entryToItemStack(entry)
                                if (!oreStack.isEmpty) {
                                    Row(spacing = 4) {
                                        ItemStack(oreStack, size = 16)
                                        Text(
                                            if (entry.count >= 1000) "${entry.count / 1000}k" else entry.count.toString(),
                                            color = 0xFFAA33,
                                            shadow = false
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Text(
                "点击「扫描」开始扫描周围矿物",
                x = left + 8,
                y = top + 80,
                color = 0x666666,
                shadow = false
            )
        }
    }
}
