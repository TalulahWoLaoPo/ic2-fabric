# 元素组件（Leaf Nodes）

## Text

```kotlin
Text(
    text = "Hello",
    color = 0xFFFFFF,
    shadow = true,
    tooltip = null,  // 可选：List<Text>，悬停时显示
    x = 0, y = 0,
    absolute = false,
    modifier = Modifier.EMPTY
)
```

- `tooltip`：可选，传入 `List<net.minecraft.text.Text>` 时，鼠标悬停显示提示

## Image

```kotlin
Image(texture, width = 16, height = 16)

Image(
    texture = atlas,
    width = 16, height = 16,
    u = 32f, v = 0f,
    regionWidth = 16, regionHeight = 16,
    textureWidth = 256, textureHeight = 256
)
```

## Button

```kotlin
Button(
    text = "确认",
    modifier = Modifier.EMPTY,
    onClick = { }
)
```

默认样式：灰色背景 + 边框，hover 高亮。

## ItemStack

```kotlin
ItemStack(stack, size = 16)
```

- 渲染物品图标
- 自动 tooltip（物品名）

## SlotAnchor

```kotlin
SlotAnchor(
    id = "machine.input",
    width = 18,
    height = 18,
    showBorder = true,
    borderColor = GuiBackground.BORDER_COLOR
)
```

- 参与布局并导出锚点
- 默认绘制边框
- 可 `showBorder = false`
