# 容器组件（Layout Nodes）

## GuiSize — 通用 GUI 尺寸

项目预定义了常用 GUI 尺寸枚举，位于 `ic2_120.client.compose.GuiSize`。

| 枚举值 | 尺寸 | 适用场景 |
|---|---|---|
| `STANDARD` | 176×166 | 发电机、基础机器（无升级槽） |
| `STANDARD_UPGRADE` | 194×184 | 标准机器 + 4 个升级槽 |
| `TALL` | 194×214 | 加高机器，如灌装机 |
| `COMPACT` | 176×120 | 紧凑机器，如变压器 |
| `LARGE` | 256×256 | 大型 GUI，如扫描仪 |
| `DEBUG` | 240×200 | 调试面板 |

**属性：**
- `GuiSize.WIDTH` / `GuiSize.HEIGHT` — GUI 总尺寸
- `GuiSize.contentWidth` — 内容宽度（= WIDTH - 16）
- `GuiSize.PLAYER_INVENTORY_Y` — 背包 Y 起始坐标
- `GuiSize.HOTBAR_Y` — 快捷栏 Y 起始坐标

```kotlin
val gui = GuiSize.STANDARD_UPGRADE
// Flex 根节点使用 GuiSize 固定宽高
Flex(
    direction = FlexDirection.COLUMN,
    gap = 8,
    modifier = Modifier.EMPTY.width(gui.width).height(gui.height)
) {
    // 固定头部
    Title()
    // flex-1: 填充剩余空间
    ScrollView(width = gui.contentWidth, modifier = Modifier.EMPTY.fractionHeight(1.0f)) { ... }
    // 固定底部
    StatusBar()
}
```

自定义尺寸：`GuiSize(210, 180)`

## Column

```kotlin
Column(spacing = 4) {
    Text("A")
    Text("B")
}
```

- 垂直布局
- `width = max(child.width)`
- `height = sum(child.height) + spacing*(n-1)`

## Row

```kotlin
Row(spacing = 8) {
    Button("A") { }
    Button("B") { }
}
```

- 水平布局
- `width = sum(child.width) + spacing*(n-1)`
- `height = max(child.height)`

## Flex

```kotlin
Flex(
    direction = FlexDirection.ROW,
    justifyContent = JustifyContent.SPACE_BETWEEN,
    alignItems = AlignItems.CENTER,
    gap = 8,
    modifier = Modifier.EMPTY.width(176).height(20)
) {
    Text("左")
    Text("右")
}
```

- 支持 `ROW/COLUMN`
- 支持 `justifyContent` 与 `alignItems`

### 百分比尺寸（fractionWidth / fractionHeight）

父容器为 `Flex` 时，子组件可通过 `Modifier.fractionWidth()` / `Modifier.fractionHeight()` 设置相对于父容器内尺寸的百分比。

**规则：**
- `fractionWidth` — 占父容器**宽度**的百分比
- `fractionHeight` — 占父容器**高度**的百分比
- 仅在父 Flex 有明确尺寸（或父级有界约束）时生效
- 百分比子节点不参与 Flex 自身尺寸的计算，避免循环依赖
- 当百分比作用在 **Flex 主轴** 时（ROW 的 `fractionWidth` / COLUMN 的 `fractionHeight`）：
  - 会按“固定子项 + gap 之后的剩余空间”分配
  - 多个主轴百分比子项按各自 fraction 比例瓜分剩余空间（`flex-grow` 语义）
- 当百分比作用在 **Flex 交叉轴** 时：直接按父容器该轴尺寸计算

```kotlin
// ROW Flex：主轴按剩余宽度比例分配
Flex(
    direction = FlexDirection.ROW,
    modifier = Modifier.EMPTY.width(240).height(20),
    gap = 4
) {
    Text("固定", modifier = Modifier.EMPTY.width(40))
    Text("A", modifier = Modifier.EMPTY.fractionWidth(0.5f))
    Text("B", modifier = Modifier.EMPTY.fractionWidth(0.5f))
}

// COLUMN Flex：ScrollView 吃掉剩余高度（推荐）
Flex(
    direction = FlexDirection.COLUMN,
    gap = 8,
    modifier = Modifier.EMPTY.width(224).height(184)
) {
    Text("Header", modifier = Modifier.EMPTY.height(24))
    Text("Toolbar", modifier = Modifier.EMPTY.height(18))
    ScrollView(
        width = 224,
        height = 1,
        modifier = Modifier.EMPTY.fractionHeight(1.0f)
    ) { /* ... */ }
    Text("Footer", modifier = Modifier.EMPTY.height(9))
}

// 混合：width 用固定值，height 用百分比
Flex(
    direction = FlexDirection.ROW,
    modifier = Modifier.EMPTY.width(200).height(40)
) {
    Panel(0, 0, modifier = Modifier.EMPTY.width(100).fractionHeight(1.0f))
    Text("剩余空间", modifier = Modifier.EMPTY.fractionWidth(0.5f).fractionHeight(1.0f))
}
```

## Table

```kotlin
Table(columnSpacing = 8, rowSpacing = 4) {
    row { Text("能量") ; Text("128 / 512 EU") }
    row { Text("进度") ; Text("67%") }
}
```

- 列对齐，行高取该行最大高度
- 行内单元格垂直居中
