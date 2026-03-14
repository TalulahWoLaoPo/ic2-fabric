# 热能系统（HU）

本文档定义当前实现的机器热能系统，单位为 `HU`。

## 1. 基本规则

- 热能分为两类机器：
  1. `加热机`：产热并向外传输 HU。
  2. `耗热机`：接收并消耗 HU。
- 不实现独立热网（Heat Network）。
- 传热仅允许机器与机器直接相邻。
- **所有热机都没有热量缓存**，未传输的热量会丢失。

## 2. 传热面规则（单面、背面）

- 每台热机/耗热机都只有一个传热面：`背面`。
- 其他 5 个面不参与传热。
- 两台机器要传热，必须满足：
  1. 两台机器方块位置紧贴。
  2. 双方传热面互相贴合（A 的背面贴 B 的背面朝向）。

实现位于：
- `HeatGeneratorBlockEntityBase`：加热机基类，提供背面传热方法。
- `HeatConsumerBlockEntityBase`：耗热机基类，只允许从背面接热。
- `IHeatNode` / `IHeatConsumer`：热能节点与耗热接口。

## 3. Liquid Fuel Firebox（流体加热机）

机器 ID：`fluid_heat_generator`
英文名：`Liquid Fuel Firebox`

### 行为

- 燃料：`沼气（Biofuel）`
- 产热：`32 HU/t`（瞬时产生，不可缓存）
- 燃料消耗：`10 mB/s`
- 结算周期：`每秒一次`
- **无热量缓存**：每秒直接产生 640 HU 并尝试传输，未传输的热量会丢失

由于按秒结算：
- 每秒产热：`32 * 20 = 640 HU/s`（瞬时）
- 每秒消耗：`10 mB`

### 实现文件

- 方块：`src/main/kotlin/ic2_120/content/block/FluidHeatGeneratorBlock.kt`
- 方块实体：`src/main/kotlin/ic2_120/content/block/machines/FluidHeatGeneratorBlockEntity.kt`
- 资源：
  1. `src/main/resources/assets/ic2_120/blockstates/fluid_heat_generator.json`
  2. `src/main/resources/assets/ic2_120/models/item/fluid_heat_generator.json`

## 4. Electric Heat Generator（电力加热机）

机器 ID：`electric_heat_generator`

### 行为

- 内部线圈槽：`10` 个，每槽最多 `1` 个线圈（`coil`）
- 热功率：每个线圈 `10 HU/t`，满线圈 `100 HU/t`（瞬时产生，不可缓存）
- 转换比：`1 EU = 1 HU`
- 电力等级：`Tier 4`
- 电量缓存：`10,000 EU`
- **无热量缓存**：消耗的 EU 直接转换为 HU 并尝试传输，未传输的热量会丢失
- 红石控制：使用 `RedstoneControlComponent`（有信号运行，反转后无信号运行）

### 实现文件

- 方块：`src/main/kotlin/ic2_120/content/block/ElectricHeatGeneratorBlock.kt`
- 方块实体：`src/main/kotlin/ic2_120/content/block/machines/ElectricHeatGeneratorBlockEntity.kt`
- 能量同步：`src/main/kotlin/ic2_120/content/sync/ElectricHeatGeneratorSync.kt`

## 5. Solid Heat Generator（固体加热机）

机器 ID：`solid_heat_generator`

### 行为

- 热功率：`20 HU/t`（瞬时产生，不可缓存）
- 燃料：`煤炭 / 木炭`
- 单个燃料总热量：`8000 HU`（即 `400 tick`）
- **无热量缓存**：燃烧时每 tick 直接产生 20 HU 并尝试传输，未传输的热量会丢失
- 传热规则与其他热机一致（仅背面传热）

### 实现文件

- 方块：`src/main/kotlin/ic2_120/content/block/SolidHeatGeneratorBlock.kt`
- 方块实体：`src/main/kotlin/ic2_120/content/block/machines/SolidHeatGeneratorBlockEntity.kt`

## 6. 无热量缓存说明

所有热机都**没有热量缓存**，这意味着：

- 产生的热量必须立即被背面的耗热机接收，否则会丢失
- 如果热机背面没有耗热机，或者耗热机已满/无法接收热量，产生的热量会完全浪费
- 玩家需要确保热机背面始终连接着可接收热量的耗热机，以避免能源浪费

这种设计：
- ✅ 简化了实现逻辑
- ✅ 减少了状态管理的复杂性
- ❌ 要求玩家更谨慎地规划机器布局
- ❌ 可能导致能源浪费（如果布局不当）
