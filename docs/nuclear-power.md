# 核电（核反应堆）容量与热量逻辑

## 多方块结构

核反应堆采用多方块结构：

- **中心**：核反应堆方块（`nuclear_reactor`）
- **扩展**：六面（上下前后左右）可各接触 0 或 1 个核反应仓（`reactor_chamber`）

所有 NBT 数据均存储在反应堆的 BlockEntity 上，反应仓本身无独立存储。

## 槽位布局

- 布局：9 行 × (3–9) 列，`index = col * 9 + row`
- 相邻：上下左右四方向 `(x±1, y)`、`(x, y±1)`

## 槽位容量公式

```
当前有效容量 = 27 + 相邻反应仓数 × 9
```

### 常量说明

| 常量 | 值 | 说明 |
|------|-----|------|
| `BASE_SLOTS` | 27 | 基础槽位，无反应仓时的容量 |
| `SLOTS_PER_CHAMBER` | 9 | 每个相邻反应仓增加的槽位 |
| `MAX_SLOTS` | 81 | 最大槽位数（6 个反应仓时） |

### 容量对照表

| 相邻反应仓数 | 有效容量 |
|-------------|---------|
| 0 | 27 |
| 1 | 36 |
| 2 | 45 |
| 3 | 54 |
| 4 | 63 |
| 5 | 72 |
| 6 | 81 |

## 计算周期与工作流程

- **Tick 率**：20 tick（每秒一次计算）
- **Tick 偏移**：BE 构造时随机 `0..19`，当 `(world.time + tickOffset) % 20 == 0` 时执行
- **目的**：分散全服核电计算，避免同一 tick 卡顿

### processChambers 双阶段

每周期执行两次遍历：

1. **Pass 0 (heatRun=false)**：发电与中子脉冲
   - 燃料棒：`acceptUraniumPulse` 向邻接传递脉冲，`addOutput(1.0F)` 发电
   - 中子反射器：`acceptUraniumPulse` 返回 true，增加邻接燃料棒脉冲数
   - 燃料棒耐久消耗，耗尽后替换为枯竭燃料棒

2. **Pass 1 (heatRun=true)**：热量分配
   - 燃料棒：`heat = triangularNumber(pulses) * 4`，优先传给邻接可储热组件，剩余 `addHeat`
   - 散热片：吸堆温 → 自身蒸发 / 向邻接蒸发
   - 热交换器：与堆/邻接组件交换热量

### 能量输出

- `output` 为 float 累加值（每脉冲 1.0）
- 每周期结束：`output * 5.0` 转为 EU（`getOfferedEnergy()` 中计算）
- 输出上限：8192 EU/t（tier 5）

#### 铀燃料棒发电量

| 类型 | 基础脉冲数 | 循环次数 | 总脉冲数 | EU/t |
|------|----------|---------|---------|------|
| 单联铀燃料棒 | 1 | 1 | 1 | 5 |
| 双联铀燃料棒 | 2 | 2 | 4 | 20 |
| 四联铀燃料棒 | 3 | 4 | 12 | 60 |

计算公式：
- `basePulses = 1 + numberOfCells / 2`
- 总脉冲数 = `basePulses * numberOfCells`
- EU/t = `总脉冲数 * 5`

## 热量与堆温

反应堆可贮存热量，最多 **10,000 点**。贮存的热量即为**堆温**。

- 堆温范围：0 ~ 10,000
- 燃料棒发热优先传给邻接可储热组件，剩余进入堆温
- 散热片蒸发热量通过 `addEmitHeat(-evaporated)` 降低堆温
- 堆温 ≥ 10,000 时爆炸

## 堆温对环境的影响

| 堆温阈值 | 影响范围 | 效果 |
|---------|---------|------|
| > 4,000 | 5×5×5 | 方块有几率着火 |
| > 5,000 | 5×5×5 | 水有几率蒸发 |
| > 7,000 | 7×7×7 | 生物有几率受到伤害（防化服可抵挡） |
| > 8,500 | 5×5×5 | 方块有几率变成岩浆 |
| ≥ 10,000 | — | **核反应堆爆炸** |

### 防化服

穿戴完整防化服（头盔、胸甲、护腿）的生物可免疫堆温 > 7,000 时的辐射伤害。防化服物品 ID 包含 `hazmat`。注意：防护服尚未实现。

## 反应堆组件 API

- **IReactor**：反应堆接口，供组件访问槽位、热量、输出等
- **IBaseReactorComponent**：可放入反应堆的物品需实现，`canBePlacedIn`
- **IReactorComponent**：`processChamber`、`acceptUraniumPulse`、`canStoreHeat`、`alterHeat` 等
- **AbstractReactorComponent**：默认空实现
- **AbstractDamageableReactorComponent**：NBT `"use"` 存储耐久/热量

## 散热片器件

| 组件 | 热容量 | 自身蒸发 | 吸堆温 | 邻接蒸发 |
|------|--------|----------|--------|----------|
| 散热片 (heat_vent) | 1000 | 6 | 0 | 0 |
| 反应堆散热片 (reactor_heat_vent) | 1000 | 5 | 5 | 0 |
| 元件散热片 (component_heat_vent) | 0 | 0 | 0 | 4/方向 |
| 高级散热片 (advanced_heat_vent) | 1000 | 12 | 12 | 0 |
| 超频散热片 (overclocked_heat_vent) | 1000 | 20 | 36 | 0 |

带热容量的散热片超热容后损坏（消失）。

## 热交换器器件

| 组件 | 热容量 | 邻接交换 | 堆交换 |
|------|--------|----------|--------|
| 热交换器 (heat_exchanger) | 2500 | 12 | 4 |
| 反应堆热交换器 (reactor_heat_exchanger) | 5000 | 0 | 72 |
| 元件热交换器 (component_heat_exchanger) | 5000 | 36 | 0 |
| 高级热交换器 (advanced_heat_exchanger) | 10000 | 24 | 8 |

热交换器按温度差与邻接/堆双向交换热量，超热容后损坏。

## 燃料棒

- **铀燃料棒**（单/双/四联）：发电、发热，耗尽后变为枯竭燃料棒
- **枯竭燃料棒**：占位，不可发电
- 发热量：`triangularNumber(pulses) * 4`，优先传给邻接可储热组件

## MOX 燃料棒

MOX 燃料棒（Mixed Oxide Fuel）是一种特殊的燃料棒，其发电和发热特性与堆温密切相关。

### 类型

- **单联 MOX 燃料棒**（`mox_fuel_rod`）
- **双联 MOX 燃料棒**（`dual_mox_fuel_rod`）
- **四联 MOX 燃料棒**（`quad_mox_fuel_rod`）

耗尽后变为对应的枯竭 MOX 燃料棒。

### 发电公式

MOX 燃料棒的发电量与堆温成正比：

```
堆温效率 = reactor.heat / reactor.maxHeat
每脉冲发电 = 4.0 × 堆温效率 + 1.0
```

- **最低发电**（堆温 = 0）：每脉冲 1.0 output
- **最高发电**（堆温 = 10,000）：每脉冲 5.0 output
- **中等发电**（堆温 = 5,000）：每脉冲 3.0 output

#### MOX 燃料棒发电量（单联）

| 堆温 | 堆温效率 | 每脉冲发电 | EU/t |
|------|---------|-----------|------|
| 0 | 0% | 1.0 | 5 |
| 2,500 | 25% | 2.0 | 10 |
| 5,000 | 50% | 3.0 | 15 |
| 7,500 | 75% | 4.0 | 20 |
| 10,000 | 100% | 5.0 | 25 |

计算公式：
- EU/t = `每脉冲发电 * 5`
- 总脉冲数计算与铀燃料棒相同（受邻接燃料棒影响）

### 发热公式

在液冷反应堆（`isFluidCooled()` 返回 true）中：

- 基础发热：`triangularNumber(pulses) * 4`（与铀燃料棒相同）
- **热量翻倍条件**：当 `堆温效率 > 0.5`（即堆温 > 5,000）时，发热量 × 2

### 特性对比

| 特性 | 铀燃料棒 | MOX 燃料棒 |
|------|---------|-----------|
| 每脉冲发电 | 固定 1.0 | 1.0 ~ 5.0（取决于堆温） |
| 单联 EU/t | 5 | 5 ~ 25（堆温 0 ~ 10,000） |
| 发热量 | `triangularNumber(pulses) * 4` | 同铀燃料棒，液冷且堆温>5000时翻倍 |
| 最佳工作温度 | 任意 | 高温（堆温越高发电越多） |
| 适用场景 | 稳定发电 | 高温高发电效率 |

### 使用建议

- MOX 燃料棒适合在高温反应堆中使用，以获得最大发电效率
- 需要配合足够的散热系统，避免堆温达到爆炸阈值
- 在液冷反应堆中，堆温超过 5,000 时发热量会翻倍，需要更强的散热能力

## 中子反射器

- **neutron_reflector**、**thick_neutron_reflector**、**iridium_neutron_reflector**
- `acceptUraniumPulse` 返回 true，增加邻接燃料棒脉冲数（当前为简化实现，无耐久消耗）

## 元件能力概览

| 能力 | 说明 |
|------|------|
| 发电 | 燃料棒每脉冲 +1 output，周期结束转为 EU |
| 发热 | 燃料棒产生热量，优先传给邻接可储热组件，剩余进堆温 |
| 散热 | 组件蒸发自身热量，堆温降低 |
| 吸热 | 组件从堆吸收热量 |
| 热交换 | 组件与邻接/堆按温度差交换热量 |

## 容量变化时的行为

### 容量减少（如反应仓被拆）

1. **立即掉落**：`NuclearReactorBlock.neighborUpdate` 调用 `dropOverflowItems`
2. **每 tick 兜底**：`NuclearReactorBlockEntity.tick()` 也会调用 `dropOverflowItems`

### 容量增加（如放置反应仓）

仅增加可用槽位，原有物品保留。

## 相关代码位置

- 反应堆 API：`ic2_120.content.reactor` 包
- 主逻辑：`NuclearReactorBlockEntity`（实现 IReactor、processChambers）
- 燃料棒：`UraniumFuelRods.kt`
- 散热片：`ReactorHeatVents.kt`
- 热交换器：`ReactorHeatExchangers.kt`
- 同步与常量：`NuclearReactorSync`