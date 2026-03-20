# 架构与文件结构

## 渲染管线

每帧流程：

`DSL 构建 -> 节点树 -> Measure(自底向上) -> Render(自顶向下)`

- `ui.render(...)`：绘制 + 交互命中采集
- `ui.layout(...)`：仅布局，不绘制（用于 SlotAnchor 锚点导出）

## RenderContext

共享状态：

- 输入：`mouseX`, `mouseY`
- 命中：`buttonHits`, `tooltipHits`, `scrollHits`
- 滚动偏移：`scrollOffsets`
- 锚点：`anchors`

## 文件结构

```text
src/client/kotlin/ic2_120/client/compose/
├── ComposeUI.kt
├── Constraints.kt
├── Modifier.kt
├── Position.kt
├── SlotLayoutBridge.kt
├── UiNode.kt
└── UiScope.kt
```

业务组件：

```text
src/client/kotlin/ic2_120/client/ui/
└── EnergyBar.kt
```
