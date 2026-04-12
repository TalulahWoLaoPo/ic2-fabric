# 掉落物系统

## 概述

本项目使用 **Fabric DataGen** 自动生成方块掉落表，支持三种掉落模式：

1. **自动生成掉落表**（默认）- 通过 `ModBlockLootTableProvider` 自动处理
2. **自定义掉落表** - 使用 `@ModBlock(generateBlockLootTable = false)` 禁用自动生成
3. **完全不掉落** - 在 Block Settings 中使用 `.dropsNothing()`

## 三种掉落模式

### 模式一：自动生成掉落表（默认）

默认情况下，所有使用 `@ModBlock` 注解的方块都会自动生成掉落表。

```kotlin
@ModBlock(
    name = "reinforced_stone",
    registerItem = true,
    tab = CreativeTab.IC2_MATERIALS,
    group = "building"
)
class ReinforcedStoneBlock : Block(
    AbstractBlock.Settings.copy(Blocks.STONE).strength(50.0f, 1200.0f)
)
// 自动生成：被破坏时掉落自身
```

**自动生成的规则**（在 `ModBlockLootTableProvider.kt` 中定义）：

| 方块类型 | 掉落行为 |
|---------|---------|
| `MachineBlock` | 用扳手拆 → 完整机器<br>不用扳手 → 只掉外壳 |
| 矿石（铅/锡/铀） | 掉落粗矿石，带时运加成 |
| 防爆门 | 掉落门物品 |
| 其他方块 | 掉落方块物品本身 |

#### 机器掉落表详解

机器有特殊的掉落逻辑（带防爆衰减）：

```kotlin
// ModBlockLootTableProvider.kt:72-92
private fun createMachineLootTable(block: MachineBlock): LootTable.Builder {
    return LootTable.builder()
        .pool(
            // 用扳手：掉完整机器
            LootPool.builder()
                .conditionally(MatchToolLootCondition.builder(wrenchPredicate))
                .with(ItemEntry.builder(blockItem).apply(ExplosionDecayLootFunction.builder()))
        )
        .pool(
            // 不用扳手：只掉外壳
            LootPool.builder()
                .conditionally(InvertedLootCondition.builder(wrenchCondition))
                .with(ItemEntry.builder(casingItem).apply(ExplosionDecayLootFunction.builder()))
        )
}
```

#### 矿石时运加成

```kotlin
// ModBlockLootTableProvider.kt:99-105
// 时运1→1.2倍、时运2→1.4倍、时运3→1.6倍
private fun addCustomOreDrop(block: Block, dropItem: Item) {
    addDrop(block, dropsWithSilkTouch(block,
        applyExplosionDecay(dropItem, ItemEntry.builder(dropItem)
            .apply(ApplyBonusLootFunction.binomialWithBonusCount(
                Enchantments.FORTUNE, 0.2f, 0
            ))
        )
    ))
}
```

### 模式二：完全不掉落（`.dropsNothing()`）

适用于像玻璃这样的方块，被破坏时完全不掉落物品。

```kotlin
@ModBlock(
    name = "reinforced_glass",
    registerItem = true,
    tab = CreativeTab.IC2_MATERIALS,
    group = "building",
    renderLayer = "cutout_mipped",
    generateBlockLootTable = false  // 禁用自动生成掉落表
)
class ReinforcedGlassBlock : Block(
    AbstractBlock.Settings.copy(Blocks.GLASS)
        .strength(10.0f, 1200.0f)
        .nonOpaque()
        .dropsNothing()  // 声明完全不掉落
)
```

**两层保障**：
- **注解层**：`generateBlockLootTable = false` - 禁用自动生成掉落表
- **代码层**：`.dropsNothing()` - 方块设置中声明不掉落

### 模式三：禁用自动生成，自定义掉落

使用 `@ModBlock(generateBlockLootTable = false)` 禁用自动生成，然后通过其他方式处理掉落。

#### 示例 1：橡胶叶（使用 resources 自定义战利品表）

```kotlin
@ModBlock(
    name = "rubber_leaves",
    registerItem = true,
    tab = CreativeTab.IC2_MATERIALS,
    group = "wood",
    generateBlockLootTable = false  // 禁用自动生成
)
class RubberLeavesBlock : LeavesBlock(settings)
```

然后在 `src/main/resources/data/ic2_120/loot_tables/blocks/rubber_leaves.json` 中自定义掉落表。

#### 示例 2：储物箱/储罐（保留内容）

```kotlin
@ModBlock(
    name = "wooden_storage_box",
    registerItem = true,
    tab = CreativeTab.IC2_MATERIALS,
    group = "storage",
    generateBlockLootTable = false  // 禁用自动生成
)
class WoodenStorageBoxBlock : MachineBlock(...)
```

在 BlockEntity 的 `onStateReplaced` 中处理带 NBT 标签的掉落，保留箱子内容。

#### 示例 3：作物（`onBreak` 自定义掉落）

```kotlin
@ModBlock(
    name = "crop",
    registerItem = false,
    generateBlockLootTable = false  // 禁用自动生成
)
class CropBlock : CropBlock(...) {
    override fun onBreak(state: BlockState, world: World, pos: BlockPos, ...) {
        // 自定义掉落逻辑
        val drops = calculateDrops(state, world, pos)
        drops.forEach { ItemScatterer.spawn(...) }
    }
}
```

## Block Settings 掉落相关方法

| 方法 | 用途 |
|------|------|
| `.dropsNothing()` | 完全不掉落 |
| `.dropsLike(block)` | 掉落像指定方块一样（需要配合自定义战利品表） |
| `.drops(table)` | 使用自定义 LootTable |

## 实现位置

- **掉落表生成器**：`ic2_120.content.recipes.ModBlockLootTableProvider`
- **注解参数**：`ic2_120.registry.annotation.ModBlock.generateBlockLootTable`
- **扫描器**：`ic2_120.registry.ClassScanner.shouldSkipGeneratedBlockLootTable()`

## 最佳实践

1. **普通方块**：使用默认的自动生成掉落表
2. **玻璃类方块**：使用 `dropsNothing()` + `generateBlockLootTable = false`
3. **机器方块**：自动处理扳手逻辑，无需额外配置
4. **需要保留 NBT 的方块**：使用 `generateBlockLootTable = false` + 自定义掉落逻辑
5. **需要复杂掉落逻辑**：在 `resources` 中编写 JSON 战利品表
