# 已有方块的创造模式变体

在不新建方块类型的前提下，为已有方块增加“变体”：同一方块、同一 BlockEntity 类型，仅通过**物品 NBT** 区分，放置时根据 NBT 初始化不同状态，且变体**仅在创造模式物品栏**中提供。

参考实现：**MFSU (Full)**（满电 MFSU），见 `MfsuBlock`、`MfsuBlockEntity`、`Ic2_120.onInitialize()`。

---

## 思路

- **不新建 Block / BlockEntity**：复用现有方块与方块实体。
- **自定义 BlockItem**：替换该方块的默认 `BlockItem`，在 `place()` 中根据物品 NBT 在放置后设置 BlockEntity 状态；在 `getName()` 中为带 NBT 的物品返回变体名称。
- **主类中覆盖注册**：在 `onInitialize()` 里用自定义 BlockItem 再次 `Registry.register(Registries.ITEM, id, customBlockItem)`，覆盖扫描阶段注册的默认 BlockItem。
- **创造模式物品栏**：用 `ItemGroupEvents.modifyEntriesEvent(...)` 向对应物品组追加一个带 NBT 的 `ItemStack`，即“变体”入口。

---

## 步骤

### 1. 在方块类中定义 NBT 键与自定义 BlockItem

在对应 Block 类中（例如 `MfsuBlock.kt`）：

- 在 **companion object** 里定义变体用的 NBT 键常量（如 `NBT_FULL = "Full"`）。
- 增加一个继承 `BlockItem` 的**内部类**（如 `MfsuBlockItem`）：
  - **`place(context: ItemPlacementContext): ActionResult`**  
    先 `super.place(context)`，若 `result.isAccepted` 且非客户端，再根据 `context.stack.nbt` 判断是否为变体；若是，则取 `context.world.getBlockEntity(context.blockPos)` 转为对应 BlockEntity，设置所需状态并 `markDirty()`。
  - **`getName(stack: ItemStack): Text`**  
    若 `stack.nbt` 包含变体键则返回变体翻译键（如 `block.modid.xxx_variant`），否则 `super.getName(stack)`。

示例（节选）：

```kotlin
companion object {
    const val NBT_FULL = "Full"
}

class MfsuBlockItem(block: Block, settings: Item.Settings) : BlockItem(block, settings) {
    override fun place(context: ItemPlacementContext): ActionResult {
        val result = super.place(context)
        if (result.isAccepted && !context.world.isClient) {
            val nbt = context.stack.nbt ?: return result
            if (nbt.getBoolean(NBT_FULL)) {
                val be = context.world.getBlockEntity(context.blockPos) as? MfsuBlockEntity ?: return result
                be.sync.amount = MfsuSync.ENERGY_CAPACITY
                be.markDirty()
            }
        }
        return result
    }

    override fun getName(stack: ItemStack): Text =
        if (stack.nbt?.getBoolean(NBT_FULL) == true)
            Text.translatable("block.ic2_120.mfsu_full")
        else
            super.getName(stack)
}
```

### 2. 在主类中覆盖物品注册并添加创造模式入口

在 `ModInitializer.onInitialize()` 中（例如在“添加特殊物品”等注释下）：

1. **用自定义 BlockItem 覆盖该方块的物品**  
   - 取方块 ID：`Identifier(MOD_ID, "mfsu")`。  
   - 取方块：`Registries.BLOCK.get(mfsuId)`。  
   - 用 **`.set()`** 覆盖同一 ID 下已注册的物品（不能用 `Registry.register()`，否则会报 “different raw IDs”）。需保留原条目的 raw ID 与 key，并传入 `Lifecycle.stable()`：  
     - 取 `mfsuKey = RegistryKey.of(RegistryKeys.ITEM, mfsuId)`，`rawId = Registries.ITEM.getRawId(Registries.ITEM.get(mfsuId))`；  
     - 调用 `(Registries.ITEM as SimpleRegistry<Item>).set(rawId, mfsuKey, customBlockItem, Lifecycle.stable())`（`Lifecycle` 为 `com.mojang.serialization.Lifecycle`）。  
   这样会覆盖 ClassScanner 之前注册的默认 BlockItem。

2. **向创造模式物品栏追加变体物品堆**  
   - 取物品组 Key：`RegistryKey.of(RegistryKeys.ITEM_GROUP, Identifier(MOD_ID, CreativeTab.IC2_MACHINES.id))`（按你用的物品组改）。  
   - `ItemGroupEvents.modifyEntriesEvent(ic2MachinesKey).register { entries -> ... }`。  
   - 在回调里：`ItemStack(Registries.ITEM.get(mfsuId))`，`stack.orCreateNbt.putBoolean(YourBlock.NBT_FULL, true)`，`entries.add(fullStack)`。

示例（节选）：

```kotlin
val mfsuId = Identifier(MOD_ID, "mfsu")
val mfsuBlock = Registries.BLOCK.get(mfsuId)
val mfsuKey = RegistryKey.of(RegistryKeys.ITEM, mfsuId)
val rawId = Registries.ITEM.getRawId(Registries.ITEM.get(mfsuId))
val customMfsuItem = MfsuBlock.MfsuBlockItem(mfsuBlock, FabricItemSettings())
(Registries.ITEM as SimpleRegistry<Item>).set(rawId, mfsuKey, customMfsuItem, Lifecycle.stable())
val ic2MachinesKey = RegistryKey.of(RegistryKeys.ITEM_GROUP, Identifier(MOD_ID, CreativeTab.IC2_MACHINES.id))
ItemGroupEvents.modifyEntriesEvent(ic2MachinesKey).register { entries ->
    val fullStack = ItemStack(Registries.ITEM.get(mfsuId))
    fullStack.orCreateNbt.putBoolean(MfsuBlock.NBT_FULL, true)
    entries.add(fullStack)
}
```

### 3. 语言文件

在 `assets/<modid>/lang/` 中为变体名称增加条目，例如：

- `block.ic2_120.mfsu_full`: `"MFSU (Full)"` / `"MFSU储电箱（满电）"`

---

## 要点小结

| 项目 | 说明 |
|------|------|
| 不新建方块 | 仅复用已有 Block + BlockEntity，用物品 NBT 区分变体。 |
| 自定义 BlockItem | 必须覆盖物品注册（主类里再 `Registry.register(ITEM, id, customItem)`），否则创造栏里拿到的仍是默认 BlockItem。 |
| 放置时写 BE | 在 `place()` 里服务端、`result.isAccepted` 后根据 `context.stack.nbt` 写 BlockEntity 并 `markDirty()`。 |
| 创造栏入口 | 只通过 `ItemGroupEvents.modifyEntriesEvent(...).add(带 NBT 的 ItemStack)` 添加，生存无法合成该 NBT，即“仅创造可拿”。 |

按上述步骤即可为任意已有方块增加仅创造模式可用的变体，而不新增方块类型。
