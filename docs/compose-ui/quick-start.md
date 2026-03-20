# ComposeUI 快速开始

## 最小示例

```kotlin
class MyScreen(...) : HandledScreen<...>(...) {
    private val ui = ComposeUI()

    override fun render(ctx: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(ctx, mouseX, mouseY, delta)

        ui.render(ctx, textRenderer, mouseX, mouseY) {
            Column(x = 10, y = 10, spacing = 4) {
                Text("Hello IC2")
                Row(spacing = 8) {
                    Button("确认") { }
                    Button("取消") { }
                }
            }
        }
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean =
        ui.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button)
}
```

## ScrollView 事件转发（可选）

```kotlin
override fun mouseScrolled(mouseX: Double, mouseY: Double, amount: Double): Boolean =
    ui.mouseScrolled(mouseX, mouseY, 0.0, amount)
        || super.mouseScrolled(mouseX, mouseY, amount)

override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int,
                          deltaX: Double, deltaY: Double): Boolean =
    ui.mouseDragged(mouseX, mouseY, button)
        || super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)

override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
    ui.stopDrag()
    return super.mouseReleased(mouseX, mouseY, button)
}
```

## Tooltip

```kotlin
val tooltip = ui.getTooltipAt(mouseX, mouseY)
if (tooltip != null) {
    context.drawTooltip(textRenderer, tooltip, mouseX, mouseY)
} else {
    drawMouseoverTooltip(context, mouseX, mouseY)
}
```
