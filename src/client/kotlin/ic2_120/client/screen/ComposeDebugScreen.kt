package ic2_120.client.screen

import ic2_120.client.compose.*
import ic2_120.client.ui.GuiBackground
import ic2_120.content.block.ComposeDebugBlock
import ic2_120.content.screen.ComposeDebugScreenHandler
import ic2_120.registry.annotation.ModScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import net.minecraft.text.Text
import net.minecraft.util.Identifier

/**
 * Compose UI 调试屏幕。
 * 展示 ScrollView 的完整功能：滚轮滚动、track 跳转、thumb 拖拽。
 *
 * 内容：
 * - 标题区（按钮：增减测试数据）
 * - ScrollView：模拟矿物扫描结果，每行显示矿物图标+名称+数量
 * - 底部信息栏
 */
@ModScreen(block = ComposeDebugBlock::class)
class ComposeDebugScreen(
    handler: ComposeDebugScreenHandler,
    playerInventory: PlayerInventory,
    title: Text
) : HandledScreen<ComposeDebugScreenHandler>(handler, playerInventory, title) {

    private val ui = ComposeUI()

    init {
        backgroundWidth = ComposeDebugScreenHandler.GUI_WIDTH
        backgroundHeight = ComposeDebugScreenHandler.GUI_HEIGHT
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        GuiBackground.draw(context, x, y, backgroundWidth, backgroundHeight)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(context, mouseX, mouseY, delta)

        val left = x
        val top = y
        val innerW = backgroundWidth - 16

        // === 演示用测试数据 ===
        val results = buildList {
            for (i in 1..40) {
                val oreName = when (i % 8) {
                    0 -> "煤矿" to 128
                    1 -> "铁矿" to 64
                    2 -> "铜矿" to 95
                    3 -> "锡矿" to 87
                    4 -> "银矿" to 31
                    5 -> "铅矿" to 29
                    6 -> "金矿" to 18
                    else -> "钻石矿" to 7
                }
                add(OreEntry(oreName.first, oreName.second, i))
            }
        }

        var itemCount = 0

        ui.render(context, textRenderer, mouseX, mouseY) {
            // ===== 标题栏 =====
            Column(x = left + 8, y = top + 8, spacing = 6) {
                Text("Compose UI 调试面板", color = 0xFFFFFF)
                Text("ScrollView 演示 — 滚轮/track/thumb", color = 0xAAAAAA, shadow = false)
            }

            // ===== 顶部按钮（不受滚动影响）=====
            Row(
                x = left + 8,
                y = top + 40,
                spacing = 8,
                modifier = Modifier.EMPTY.width(innerW)
            ) {
                Button(
                    text = "按钮 A",
                    modifier = Modifier.EMPTY.width(60),
                    onClick = { /* demo */ }
                )
                Button(
                    text = "按钮 B",
                    modifier = Modifier.EMPTY.width(60),
                    onClick = { /* demo */ }
                )
                Text(
                    "Hover 我看 Tooltip！",
                    x = 0,
                    y = 0,
                    modifier = Modifier.EMPTY.width(innerW - 140).padding(4, 0, 0, 0)
                )
            }

            // ===== 标签说明（不受滚动影响）=====
            Text(
                "扫描结果（${results.size} 种矿物）:",
                x = left + 8,
                y = top + 64,
                color = 0xCCCCCC,
                shadow = false
            )

            // ===== ScrollView（核心演示）=====
            // 视口：innerW 宽，120 高
            ScrollView(
                width = innerW,
                height = 120,
                scrollbarWidth = 8,
                x = left + 8,
                y = top + 76,
                modifier = Modifier.EMPTY
            ) {
                Column(spacing = 2) {
                    for (entry in results) {
                        Flex(
                            direction = FlexDirection.ROW,
                            alignItems = AlignItems.CENTER,
                            gap = 4,
                            modifier = Modifier.EMPTY.width(innerW - 8).height(14)
                        ) {
                            // 矿物图标（虚拟）
                            val oreItem = getOreItemStack(entry.name)
                            ItemStack(oreItem, size = 12, showCount = false)
                            // 矿物名称
                            Text(entry.name, color = 0xFFAA33, shadow = false)
                            // 数量
                            Text(
                                "× ${entry.count}",
                                color = 0x888888,
                                shadow = false,
                                modifier = Modifier.EMPTY.padding(4, 0, 0, 0)
                            )
                            // 填充空白
                            Text(
                                "",
                                modifier = Modifier.EMPTY.width(60)
                            )
                            // 按钮（在 ScrollView 内）
                            Button(
                                text = "×",
                                modifier = Modifier.EMPTY.width(16).height(12),
                                onClick = { /* demo */ }
                            )
                        }
                        itemCount++
                    }
                }
            }

            // ===== 底部状态栏 =====
            Row(
                x = left + 8,
                y = top + backgroundHeight - 28,
                spacing = 4,
                modifier = Modifier.EMPTY.width(innerW)
            ) {
                Text("行数: $itemCount", color = 0x666666, shadow = false)
                Text(" | ", color = 0x444444, shadow = false)
                Text("右键关闭", color = 0x666666, shadow = false)
            }
        }

        val tooltip = ui.getTooltipAt(mouseX, mouseY)
        if (tooltip != null) {
            context.drawTooltip(textRenderer, tooltip, mouseX, mouseY)
        } else {
            drawMouseoverTooltip(context, mouseX, mouseY)
        }
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean =
        ui.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button)

    override fun mouseScrolled(
        mouseX: Double,
        mouseY: Double,
        amount: Double
    ): Boolean =
        ui.mouseScrolled(mouseX, mouseY, 0.0, amount)
            || super.mouseScrolled(mouseX, mouseY, amount)

    override fun mouseDragged(
        mouseX: Double,
        mouseY: Double,
        button: Int,
        deltaX: Double,
        deltaY: Double
    ): Boolean =
        ui.mouseDragged(mouseX, mouseY, button) || super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)

    override fun mouseReleased(
        mouseX: Double,
        mouseY: Double,
        button: Int
    ): Boolean {
        ui.stopDrag()
        return super.mouseReleased(mouseX, mouseY, button)
    }

    private data class OreEntry(val name: String, val count: Int, val index: Int)

    private fun getOreItemStack(name: String): ItemStack {
        val id = when (name) {
            "煤矿" -> "minecraft:coal"
            "铁矿" -> "minecraft:iron_ore"
            "铜矿" -> "minecraft:copper_ore"
            "锡矿" -> "ic2_120:tin_ore"
            "银矿" -> "ic2_120:silver_ore"
            "铅矿" -> "ic2_120:lead_ore"
            "金矿" -> "minecraft:gold_ore"
            else -> "minecraft:diamond_ore"
        }
        val identifier = Identifier.tryParse(id) ?: return ItemStack.EMPTY
        val item = Registries.ITEM.getOrEmpty(identifier).orElse(null) ?: return ItemStack.EMPTY
        if (item == net.minecraft.item.Items.AIR) return ItemStack.EMPTY
        return ItemStack(item)
    }
}
