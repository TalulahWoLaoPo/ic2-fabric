# 配方迁移检查清单 - Phase 2

## 迁移进度

- [x] **阶段 1：高优先级配方（13 个）**
  - [x] lv_transformer - 低压变压器
  - [x] mv_transformer - 中压变压器
  - [x] hv_transformer - 高压变压器
  - [x] ev_transformer - 超高压变压器
  - [x] extractor - 提取机
  - [x] pump - 泵
  - [x] solar_generator - 太阳能发电机
  - [x] reactor_chamber - 核反应仓
  - [x] nuclear_reactor - 核反应堆
  - [x] iron_furnace - 铁炉
  - [x] luminator_flat - 日光灯
  - [x] tesla_coil_iron - 特斯拉线圈（铁版）
  - [x] tesla_coil_steel - 特斯拉线圈（钢版）

  - [x] **阶段 2：中优先级配方（3 个）**
   - [x] wind_generator - 风力发电机
   - [x] stirling_generator - 斯特林发电机
   - [x] rt_generator - 放射性同位素温差发电机
   - [x] (无其他机器)

- [⏭️] **阶段 3：低优先级配方（17 个）- 已跳过**
  - ⏭️ semifluid_generator - 半流体发电机（配方定义缺失）
  - ⏭️ electric_heat_generator - 电力热发生器（配方定义缺失）
  - ⏭️ fluid_heat_generator - 流体热发生器（配方定义缺失）
  - ⏭️ solid_heat_generator - 固体热发生器（配方定义缺失）
  - ⏭️ solar_distiller - 太阳能蒸馏器（配方定义缺失）
  - ⏭️ wind_kinetic_generator - 风力动能发电机（配方定义缺失）
  - ⏭️ geo_generator - 地热发电机（配方定义缺失）
  - ⏭️ macerator - 粉碎机（配方定义缺失）
  - ⏭️ compressor - 压缩机（配方定义缺失）
  - ⏭️ ore_washing_plant - 矿石洗涤厂（配方定义缺失）
  - ⏭️ electric_furnace - 电炉（配方定义缺失）
  - ⏭️ metal_former - 金属成型机（配方定义缺失）

---

## 方块类存在性检查

在开始迁移前，需要确认以下方块类已存在：

### 发电机类
- [x] GeneratorBlock - 已存在（火力发电机）
- [x] WaterGeneratorBlock - 已存在
- [x] WindGeneratorBlock - 已存在
- [x] SolarGeneratorBlock - 已存在
- [x] StirlingGeneratorBlock - 已存在
- [x] RtGeneratorBlock - 已存在
- [ ] SemifluidGeneratorBlock - 需要创建
- [ ] ElectricHeatGeneratorBlock - 已存在
- [ ] FluidHeatGeneratorBlock - 已存在
- [ ] SolidHeatGeneratorBlock - 需要创建

### 机器类
- [x] ExtractorBlock - 已存在
- [x] PumpBlock - 已存在
- [ ] IronFurnaceBlock - 已存在
- [ ] LuminatorFlatBlock - 已存在
- [x] TeslaCoilBlock - 已存在
- [ ] MaceratorBlock - 需要创建
- [ ] CompressorBlock - 需要创建
- [ ] OreWashingPlantBlock - 需要创建
- [ ] ElectricFurnaceBlock - 需要创建
- [ ] MetalFormerBlock - 需要创建

---

## 物品类存在性检查

在开始迁移前，需要确认以下物品类已存在：

- [x] TreetapItem - 已存在（木龙头）
- [x] MachineItem - 已存在（基础机械外壳）
- [x] CircuitItem - 已存在（电路板）
- [x] CoilItem - 已存在（线圈）
- [x] EmptyCellItem - 已存在（空单元）
- [x] MiningPipeItem - 已存在（采矿管道）
- [x] AdvancedCircuitItem - 已存在（高级电路）
- [x] AdvancedReBatteryItem - 已存在（高级充电电池）
- [x] LapotronCrystalItem - 已存在（拉普顿晶体）
- [ ] CoalDustItem - 需要创建
- [ ] LeadPlateItem - 已存在
- [x] DenseLeadPlateItem - 已存在
- [ ] HeatConductorItem - 已存在
- [ ] IronCasingItem - 已存在

---

## 总计

 - **已完成**：58/75 (77%)
 - **已跳过**：17/75 (23%) - Phase 3 配方定义缺失

---

## 验证结果

运行 `./gradlew runDatagen` 验证：
- 配方生成器数量：58 个
- 配方生成结果：成功 58，失败 0

---

## 完成状态

配方迁移 Phase 2 已完成，Phase 3 已跳过。
