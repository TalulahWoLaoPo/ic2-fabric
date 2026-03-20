# ComposeUI — DSL 总览与导航

ComposeUI 是一个基于 `DrawContext` 的声明式 UI DSL，适用于 `HandledScreen` 的布局与渲染。

源码目录：`src/client/kotlin/ic2_120/client/compose/`

---

## 你应该先看哪篇

- 快速上手：`docs/compose-ui/quick-start.md`
- SlotAnchor 全链路（ScreenHandler + Screen）：`docs/compose-ui/slot-anchor-pipeline.md`
- 元素（Text / Image / Button / ItemStack / SlotAnchor）：`docs/compose-ui/elements.md`
- 容器（Column / Row / Flex / Table）：`docs/compose-ui/containers.md`
- ScrollView：`docs/compose-ui/scrollview.md`
- 架构与文件结构：`docs/compose-ui/architecture.md`

---

## DSL 总览

```kotlin
ui.render(context, textRenderer, mouseX, mouseY) {
    Column(x = left + 8, y = top + 8, spacing = 6) {
        Text("标题")

        Row(spacing = 8) {
            Button("确认") { }
            Button("取消") { }
        }

        Flex(
            direction = FlexDirection.ROW,
            justifyContent = JustifyContent.SPACE_BETWEEN,
            alignItems = AlignItems.CENTER,
            modifier = Modifier.EMPTY.width(176)
        ) {
            Text("左")
            Text("右")
        }

        Table(columnSpacing = 8, rowSpacing = 4) {
            row {
                Text("能量")
                Text("128 / 512 EU")
            }
        }

        ScrollView(width = 160, height = 80, scrollbarWidth = 8) {
            Column(spacing = 2) {
                Text("可滚动内容")
            }
        }

        ItemStack(stack)
        Image(texture, width = 16, height = 16)
        SlotAnchor("machine.input")
    }
}
```

---

## 两种渲染模式

1. 纯 Compose 绘制：直接 `ui.render(...)`。
2. Slot 融合（推荐）：
   1) `ui.layout(...)` 产出锚点
   2) 将锚点写回 `handler.slots`
   3) `super.render(...)`
   4) `ui.render(...)` 画 overlay

详细见：`docs/compose-ui/slot-anchor-pipeline.md`
