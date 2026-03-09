# 导线与电网系统（路径拉取 + 缓存版）

基于 TechReborn Energy API（`EnergyStorage.SIDED`）实现 IC2 风格导线网络。  
相连导线形成一个电网，由电网在每 tick 统一执行能量路由，避免方块实体 tick 顺序造成吞吐偏差。

源码：`src/main/kotlin/ic2_120/content/block/` 下的
- `BaseCableBlock.kt`
- `CableBlockEntity.kt`
- `EnergyNetwork.kt`
- `EnergyNetworkManager.kt`

---

## 核心模型

- 所有连通导线共享一个 `EnergyNetwork`。
- 每根导线提供：
  - `transferRate`（EU/t，每 tick 可通过容量）
  - `energyLoss`（milliEU/格，路径损耗权重）
- 电网每 tick 仅执行一次（`lastTickTime` 去重）。
- 真实写入通过 Fabric 事务，失败自动回滚。

---

## 电网输出等级与触电伤害

### 输出等级

电网输出等级（1–5）由**电网内输出等级最高的机器**（`ITieredMachine.tier`）决定，与导线材质无关。该等级决定漏电导线的触电伤害范围与伤害量。

### 漏电与触电伤害

- **绝缘等级**：`IInsulatedCable.getInsulationLevel()` 返回 2–5。1 倍绝缘=2（≤128），2 倍=3（≤512），3 倍=4（≤2048），玻纤=5（绝不漏电）。
- **漏电判定**：当 `getInsulationLevel() < 电网 outputLevel` 时导线漏电；未绝缘导线等级为 0。
- **伤害规则**：漏电导线附近 n 格（n=电网等级，切比雪夫距离 ≤ n）为伤害区域，每次造成 n 心伤害。
- **触发间隔**：每 10 秒（200 tick）触发一次。
- **错峰机制**：电网构建时分配 0–200 tick 的随机偏移，使不同电网在不同 tick 触发伤害，避免大电网同时遍历。

---

## 电网生命周期

### 惰性构建

`CableBlockEntity.tick()` 发现 `network == null` 时调用 `EnergyNetworkManager.getOrCreateNetwork(world, pos)`：

1. BFS 收集连通导线；
2. 汇总导线容量到 `capacity`；
3. 记录每根导线损耗；
4. 汇总成员 `localEnergy` 到共享池；
5. 吸收接触到的旧网能量并完成合并。

### 拆分

导线被替换时 `BaseCableBlock.onStateReplaced()` 调 `EnergyNetworkManager.invalidateAt(world, pos)`：

- 共享池能量均分回成员导线 `localEnergy`；
- 清理成员 `be.network`；
- 从索引移除该网成员；
- 剩余导线下次 tick 自动重建。

### 世界卸载

`EnergyNetworkManager` 按维度缓存：`RegistryKey<World> -> MutableMap<Long, EnergyNetwork>`。  
`ServerWorldEvents.UNLOAD` 仅清理当前卸载维度。

---

## 传输算法（当前实现）

每 tick 在 `EnergyNetwork.pushToConsumers()` 中执行：

1. 扫描边界端点：
   - 消费者：`supportsInsertion == true`
   - 供电者：`supportsExtraction == true`
2. 初始化导线临时容量：`remainingCableCapacity[cable] = transferRate`；
3. 先从 `network.energy` 给消费者供电（走完整路径规则）；
4. 再从真实供电者向消费者供电。

### 路径选择

对每个消费者：

- 用“与该消费者相邻的导线集合”作为 Dijkstra 多源起点；
- 边权为目标导线 `energyLoss`（milliEU）；
- 得到到导线/供电入口的最小损耗路径；
- 按损耗从小到大依次尝试拉取。

### 单路径传输上限

候选路径上限：

- `pathCapacity = min(路径导线 remainingCableCapacity)`
- `pathLossEu = pathLossMilliEu / 1000`
- `maxDeliverable = max(pathCapacity - pathLossEu, 0)`

实际传输还受：

- 消费者实时可接收量（事务模拟插入）；
- 供电者实时可提取量（真实 `extract`）；
- 缓冲池可用量（`network.energy`）。

### 防止导线超载

路径传输成功后，路径上每根导线都扣减相同通过量（含损耗）：

- `remainingCableCapacity[cable] -= moved`

确保同一 tick 内任意导线不会被多路径叠加超过 `transferRate`。

---

## 缓冲池一致性

push 型发电机注入导线后，能量进入 `network.energy`。  
消费者从缓冲池取电时同样走“最小损耗路径 + 路径最小容量 + 全路径扣减”，与真实供电者一致。

---

## 缓存策略

`EnergyNetwork` 使用两层缓存：

1. 拓扑缓存：导线邻接、边界连接、导线速率；
2. 最短路缓存：按“消费者入口导线集合”缓存 Dijkstra 结果与候选路径。

有缓存上限保护（过多时清空），防止极端场景内存增长。

---

## NBT 持久化

- 键：`CableEnergy`
- 读取：`readNbt -> localEnergy`
- 写入：
  - 有网络：`network.getEnergySharePerCable()`
  - 无网络：`localEnergy`

用于区块卸载/重载时保留能量并在重建网络时回收。
# 导线与电网系统（路径拉取 + 缓存版）

基于 TechReborn Energy API（`EnergyStorage.SIDED`）实现 IC2 风格导线网络。  
相连导线形成一个电网，由电网统一在每 tick 执行能量路由，避免方块实体 tick 顺序造成吞吐偏差。

源码：`src/main/kotlin/ic2_120/content/block/` 下的
- `BaseCableBlock.kt`
- `CableBlockEntity.kt`
- `EnergyNetwork.kt`
- `EnergyNetworkManager.kt`

---

## 核心模型

- 所有连通导线共享一个 `EnergyNetwork`。
- 每根导线提供：
  - `transferRate`（EU/t，作为每 tick 可通过容量）
  - `energyLoss`（milliEU/格，作为路径损耗权重）
- 电网每 tick 只执行一次（`lastTickTime` 去重）。
- 真实能量写入通过 Fabric 事务执行，失败自动回滚。

---

## 电网输出等级与触电伤害

### 输出等级

电网输出等级由电网内最高等级机器决定。绝缘等级不足的导线漏电：n 级电网附近 n 格为伤害区域，每次 n 心伤害，每 10 秒触发。绝缘等级见 `IInsulatedCable.getInsulationLevel()`。电网构建时分配 0–200 tick 随机偏移以错峰。

---

## 电网生命周期

### 惰性构建

`CableBlockEntity.tick()` 发现 `network == null` 时调用 `EnergyNetworkManager.getOrCreateNetwork(world, pos)`：

1. BFS 收集连通导线；
2. 累加导线 `transferRate` 得到 `capacity`；
3. 记录每根导线 `energyLoss`；
4. 汇总成员 `localEnergy` 到共享池；
5. 若遇旧网成员则吸收旧网能量并合并。

### 拆分

导线被替换时 `BaseCableBlock.onStateReplaced()` 调 `EnergyNetworkManager.invalidateAt(world, pos)`：

- 将共享池能量均分回成员导线 `localEnergy`；
- 清理成员 `be.network`；
- 从索引移除该网成员；
- 剩余导线下次 tick 自动重建新网。

### 世界卸载

`EnergyNetworkManager` 按维度维护缓存：`RegistryKey<World> -> MutableMap<Long, EnergyNetwork>`。  
`ServerWorldEvents.UNLOAD` 仅清理当前卸载维度，不影响其他维度。

---

## 传输算法（当前实现）

每 tick 在 `EnergyNetwork.pushToConsumers()` 中执行：

1. 基于导线拓扑识别边界端点：
   - 消费者：`supportsInsertion == true`
   - 供电者：`supportsExtraction == true`
2. 初始化导线本 tick 临时容量：
   - `remainingCableCapacity[cable] = transferRate`
3. 先从缓冲池 `network.energy` 给消费者供电（路径规则与供电者一致）；
4. 再从真实供电者向消费者供电。

### 路径选择

对每个消费者：

- 用“与该消费者相邻的导线集合”作为 Dijkstra 多源起点；
- 边权使用目标导线 `energyLoss`（milliEU）；
- 得到到导线/供电入口的最小损耗路径；
- 按路径损耗从小到大依次尝试拉取。

### 单路径最大传输量

对候选路径：

- `pathCapacity = min(路径上每根导线的 remainingCableCapacity)`
- `pathLossEu = pathLossMilliEu / 1000`
- `maxDeliverable = max(pathCapacity - pathLossEu, 0)`

实际传输还受以下约束：

- 消费者实时可接收量（事务模拟插入）
- 供电者实时可提取量（真实 `extract`）
- 缓冲池实时可用量（`network.energy`）

### 防止导线超载

路径传输成功后，路径上每根导线都扣减同样的通过总量（含损耗）：

- `remainingCableCapacity[cable] -= moved`

这样可避免单根导线在同一 tick 被多条路径叠加超过自身 `transferRate`。

---

## 缓冲池与路径一致性

push 型发电机把能量注入导线时，能量进入 `network.energy`。  
消费者从缓冲池取电时也走完整路径计算（最小损耗 + 路径最小容量 + 全路径扣减），与真实供电者一致。

---

## 缓存策略

`EnergyNetwork` 内包含两层缓存：

1. **拓扑缓存（跨 tick）**
   - 缓存导线邻接、边界连接、导线速率；
   - 减少每 tick 重扫导线状态的成本。

2. **最短路缓存（跨 tick）**
   - 按“消费者入口导线集合”缓存 Dijkstra 结果；
   - 缓存该入口集合对应的候选路径列表（按损耗升序）。

并有上限保护（缓存条目过多时清空），防止极端场景无限增长。

---

## NBT 持久化

- 字段：`CableEnergy`
- 读：`readNbt -> localEnergy`
- 写：
  - 有网络：写 `network.getEnergySharePerCable()`
  - 无网络：写 `localEnergy`

用于区块卸载/重载时保留能量并在重建网络时回收。

---

## 注册机制

导线使用统一 `BlockEntityType<CableBlockEntity>`：

1. 扫描所有 `BaseCableBlock`；
2. 注册 `ic2_120:cable`；
3. `EnergyStorage.SIDED.registerForBlockEntity` 暴露导线能量接口。

---

## 已知权衡

- 采用“每轮每供电者最优路径”迭代，不枚举全量简单路径（避免组合爆炸）。
- `milliEU / 1000` 会做整数 EU 结算，极小损耗可能在单次传输中截断。
- 保留共享缓冲池以兼容 push 设备，但其放电已纳入路径限流。
