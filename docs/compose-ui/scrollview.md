# ScrollView

垂直滚动容器，支持：

- 滚轮滚动
- 点击轨道跳转
- 拖拽 thumb

## 用法

```kotlin
ScrollView(
    width = 160,
    height = 80,
    scrollbarWidth = 8,
    x = left + 8,
    y = top + 80
) {
    Column(spacing = 2) {
        for (item in items) {
            Text(item.name)
        }
    }
}
```

## 在 Flex 中使用（推荐）

`ScrollView` 放在 `Flex(direction = COLUMN)` 中时，建议使用：

- `height = 1`（占位值）
- `modifier = Modifier.EMPTY.fractionHeight(1.0f)`（主轴剩余空间）

这样 `ScrollView` 会自动吃掉“头部/底部固定区块 + gap”之后的剩余高度，不会把容器撑爆。

```kotlin
Flex(
    direction = FlexDirection.COLUMN,
    gap = 8,
    modifier = Modifier.EMPTY.width(gui.contentWidth).height(gui.height - 16)
) {
    Text("Header", modifier = Modifier.EMPTY.height(24))
    Row(modifier = Modifier.EMPTY.height(18)) { /* ... */ }
    ScrollView(
        width = gui.contentWidth,
        height = 1,
        modifier = Modifier.EMPTY.fractionHeight(1.0f)
    ) {
        Column(spacing = 2) { /* ... */ }
    }
    Text("Footer", modifier = Modifier.EMPTY.height(9))
}
```

## Screen 事件转发

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

## 限制

不支持嵌套 ScrollView（scissor 区域不可嵌套）。
