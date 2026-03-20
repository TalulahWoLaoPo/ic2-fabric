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
    private val slotMapping = mapOf(
        "compose.slot.left" to ComposeDebugScreenHandler.SLOT_LEFT_INDEX,
        "compose.slot.right" to ComposeDebugScreenHandler.SLOT_RIGHT_INDEX
    )

    init {
        backgroundWidth = ComposeDebugScreenHandler.GUI_WIDTH
        backgroundHeight = ComposeDebugScreenHandler.GUI_HEIGHT
    }

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        GuiBackground.draw(context, x, y, backgroundWidth, backgroundHeight)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val left = x
        val top = y
        val innerW = backgroundWidth - 16

        val results = buildDemoResults()
        val itemCount = results.size

        // 1) 预布局（不绘制）→ 2) 套用 slot 坐标
        val layout = ui.layout(context, textRenderer, mouseX, mouseY) {
            buildUi(left, top, innerW, results, itemCount)
        }
        SlotLayoutBridge.apply(layout.anchors, handler, left, top, slotMapping)

        // 3) 原生渲染（slot+交互）
        super.render(context, mouseX, mouseY, delta)

        // 4) Compose overlay 绘制
        ui.render(context, textRenderer, mouseX, mouseY) {
            buildUi(left, top, innerW, results, itemCount)
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

    private fun UiScope.buildUi(
        left: Int,
        top: Int,
        innerW: Int,
        results: List<OreEntry>,
        itemCount: Int
    ) {
        Column(
            x = left + 8,
            y = top + 8,
            spacing = 8,
            modifier = Modifier.EMPTY.width(innerW)
        ) {
            // 标题栏
            Column(spacing = 6) {
                Text("Compose UI 调试面板", color = 0xFFFFFF)
                Text("ScrollView 演示 — 滚轮/track/thumb", color = 0xAAAAAA, shadow = false)
            }

            // 顶部按钮（不受滚动影响）
            Row(spacing = 8) {
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
                    modifier = Modifier.EMPTY.width(innerW - 140).padding(4, 0, 0, 0)
                )
            }

            // Slot 锚点（驱动 handler.slots 坐标）
            Flex(justifyContent = JustifyContent.SPACE_BETWEEN) {
                SlotAnchor("compose.slot.left")
                SlotAnchor("compose.slot.right")
            }

            // 标签说明（不受滚动影响）
            Text(
                "扫描结果（${results.size} 种矿物）:",
                color = 0xCCCCCC,
                shadow = false
            )

            // ScrollView（核心演示）
            ScrollView(
                width = innerW,
                height = 100,
                scrollbarWidth = 8,
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
                            val oreItem = getOreItemStack(entry.name)
                            ItemStack(oreItem, size = 12)
                            Text(entry.name, color = 0xFFAA33, shadow = false)
                            Text(
                                "× ${entry.count}",
                                color = 0x888888,
                                shadow = false,
                                modifier = Modifier.EMPTY.padding(4, 0, 0, 0)
                            )
                            Text("", modifier = Modifier.EMPTY.width(60))
                            Button(
                                text = "×",
                                modifier = Modifier.EMPTY.width(16).height(12),
                                onClick = { /* demo */ }
                            )
                        }
                    }
                }
            }

            // 底部状态栏
            Row(spacing = 4) {
                Text("行数: $itemCount", color = 0x666666, shadow = false)
                Text(" | ", color = 0x444444, shadow = false)
                Text("右键关闭", color = 0x666666, shadow = false)
            }
        }
    }

    private fun buildDemoResults(): List<OreEntry> = buildList {
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
