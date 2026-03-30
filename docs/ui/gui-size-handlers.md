# GuiSize 与 ScreenHandler 槽位坐标

## 单一语义入口

- 客户端绘制玩家背包边框、Compose 锚点布局：优先使用对应档位的 `GuiSize` 实例属性（`playerInvY`、`hotbarY`、`contentWidth` 等），见 [`GuiSize.kt`](../../src/main/kotlin/ic2_120/content/screen/GuiSize.kt)。
- 标准 176×166 几何基线：[`StandardGuiLayout`](../../src/main/kotlin/ic2_120/content/screen/StandardGuiLayout.kt)。

## ScreenHandler 三类

### 1. 固定坐标（与某档 GuiSize 对齐）

多数机器在 `companion object` 中声明 `PLAYER_INV_Y`、`HOTBAR_Y`、`SLOT_SIZE`，与 `GuiSize` 分档一致：

| 分档 | 典型 `playerInvY` / `hotbarY` | 示例 Handler |
|------|-------------------------------|--------------|
| `STANDARD` | 84 / 142 | `GeneratorScreenHandler` |
| `STANDARD_UPGRADE` / `STANDARD_TALL` | 108 / 166 | `CompressorScreenHandler` |
| `UPGRADE_TALL` | 138 / 196 | `CannerScreenHandler` |
| `ATTACHMENT` | 58 / 116 | `PumpAttachmentScreenHandler` |

服务端仍用这些常量放置玩家槽，以保证未打开 GUI 时的逻辑一致；客户端 Screen 绘制边框时应使用同一档位的 `GuiSize`。

### 2. 客户端锚点（初始 `0,0`）

部分界面由客户端 `Screen` 每帧根据 Compose 锚点写回 `slot.x` / `slot.y`，Handler 里槽位可初始为 `0,0`：

- `ReplicatorScreenHandler`
- `MatterGeneratorScreenHandler`
- `UuScannerScreenHandler`
- `ComposeDebugScreenHandler`

### 3. 特殊布局（不强行套用枚举公式）

- `NuclearReactorScreenHandler`：格网与可选热流体槽，坐标由布局推导。
- `StorageBoxScreenHandler`：动态高度 + 滚动储物区，`GuiSize.computeHeight()` 与本地 `PLAYER_INV_Y` 配合。
- `ScannerScreenHandler`、`TransformerScreenHandler`：无常规玩家栏或极简 UI。

## 相关文档

- Compose UI：`docs/ui/compose-ui.md`
