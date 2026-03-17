# 配方迁移总结 - Phase 2

## 总体进度

**当前状态**：60/75 配方已完成（80%），剩余 15 个配方待迁移（20%）

---

## 已完成配方（57 个）

### 高优先级配方（13 个）✅

#### 变压器（4 个）
- ✅ lv_transformer - 低压变压器
  - 配方：6 木板 + 2 绝缘锡质导线 + 1 线圈
- ✅ mv_transformer - 中压变压器
  - 配方：2 绝缘铜质导线 + 1 基础机械外壳（中间一列）
- ✅ hv_transformer - 高压变压器
  - 配方：2 绝缘金质导线 + 1 电路板 + 1 中压变压器 + 1 高级充电电池
- ✅ ev_transformer - 超高压变压器
  - 配方：2 绝缘高压导线 + 1 高级电路 + 1 高压变压器 + 1 拉普顿晶体

#### 机器配方（3 个）
- ✅ extractor - 提取机
  - 配方：4 木龙头 + 1 基础机械外壳 + 1 电路板
- ✅ pump - 泵
  - 配方：空单元 + 基础电路 + 空单元 + 采矿管道 + 木龙头 + 基础机械外壳
- ✅ solar_generator - 太阳能发电机
  - 配方：煤粉 x3 + 玻璃 x3 + 电路板 x2 + 火力发电机 x1

#### 核能配方（2 个）
- ✅ reactor_chamber - 核反应仓
  - 配方：4 铅板（四角）+ 1 基础机械外壳（中心）
- ✅ nuclear_reactor - 核反应堆
  - 配方：4 致密铅板（上下两行）+ 1 高级电路（上中）+ 3 核反应仓（中行）+ 1 火力发电机（下中）

#### 其他配方（4 个）
- ✅ iron_furnace - 铁炉
  - 配方：铁板 x5 + 熔炉 x1
- ✅ luminator_flat - 日光灯
  - 配方：绝缘铜质导线（上中）+ 锡质导线（中中）+ 玻璃 x5 → 日光灯 x8
- ✅ tesla_coil_iron - 特斯拉线圈（铁版）
  - 配方：5 红石粉 + 1 中压变压器 + 2 铁质外壳 + 1 电路板
- ✅ tesla_coil_steel - 特斯拉线圈（钢版）
  - 配方：5 红石粉 + 1 中压变压器 + 2 钢锭 + 1 电路板

#### 发电机（1 个）
- ✅ water_generator - 水力发电机
  - 配方：4 木棍（四角）+ 4 橡木木板（四边）+ 1 火力发电机 → 水力发电机 x2

---

## 待完成配方（18 个）

### 中优先级配方（4 个）⏳

#### 发电机（3 个）
- ✅ wind_generator - 风力发电机
  - 配方：4 铁锭（四角）+ 1 火力发电机（中心）→ 风力发电机 x1
  - 方块状态：WindGeneratorBlock ✅ 已存在
- ✅ stirling_generator - 斯特林发电机
  - 配方：7 个铁质外壳 U 形 + 1 个火发发电机 + 1 个热传导器
  - 方块状态：StirlingGeneratorBlock ✅ 已存在
- ✅ rt_generator - 放射性同位素温差发电机
  - 配方：7 个铁质外壳 + 1 个核反应仓 + 1 个火发发电机
  - 方块状态：RtGeneratorBlock ✅ 已存在

### 机器（0 个）
- [x] (无待确定的机器配方)

---

### 低优先级配方（15 个）⏳

#### 发电机（7 个）
- ⏳ semifluid_generator - 半流体发电机
- ⏳ electric_heat_generator - 电力热发生器
- ⏳ fluid_heat_generator - 流体热发生器
- ⏳ solid_heat_generator - 固体热发生器
- ⏳ solar_distiller - 太阳能蒸馏器
- ⏳ wind_kinetic_generator - 风力动能发电机
- ⏳ geo_generator - 地热发电机（配方未在备份中，需确认）

#### 机器（8 个）
- ⏳ macerator - 碎机
- ⏳ compressor - 压缩机
- ⏳ ore_washing_plant - 矿石洗涤厂
- ⏳ electric_furnace - 电炉
- ⏳ metal_former - 金属成型机
- ⏳ (其他机器待确定)

---

## 迁移计划

### Phase 1：高优先级配方（已完成 57/75）✅
- [x] 变压器（4 个）
- [x] 机器配方（3 个）
- [x] 核能配方（2 个）
- [x] 其他配方（4 个）
- [x] 发电机（1 个）

### Phase 2：中优先级配方（已完成 3/3）✅
- [x] 发电机（3/3 已完成）
  - [x] wind_generator
  - [x] stirling_generator
  - [x] rt_generator
  - [x] (无其他机器)

### Phase 3：低优先级配方（待开始，15/15 待完成）⏸
- [ ] 发电机（7 个）
- [ ] 机器（7 个）
- [ ] 确认缺失的方块类

### Phase 4：验证和清理（待开始）
- [ ] 运行 `./gradlew runDatagen` 验证所有配方生成
- [ ] 确认生成的 JSON 文件数量（应该为 75 个）
- [ ] 删除 `plans/recipe-system-refactor.md.bak` 文件
- [ ] 清理 `ModRecipeProvider.kt` 中的旧代码（如需要）
- [ ] 更新项目文档

---

## 快速开始指南

### 迁移配方到指定方块类

1. **打开目标方块类文件**
   ```bash
   # 示例：WindGeneratorBlock.kt
   ```

2. **添加 `companion object` 和 `generateRecipes` 方法**
   ```kotlin
   companion object {
       val ACTIVE: BooleanProperty = BooleanProperty.of("active")

       fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
           val generator = GeneratorBlock::class.item()
           if (generator != Items.AIR) {
               val builder = ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, WindGeneratorBlock::class.item(), 1)
               builder.pattern("I I").pattern(" G ").pattern("I I")
               builder.input('I', Items.IRON_INGOT).input('G', generator)
               builder.criterion(hasItem(generator), conditionsFromItem(generator))
               builder.offerTo(exporter, Identifier(Ic2_120.MOD_ID, "wind_generator"))
           }
       }
   }
   ```

3. **添加必要的导入**
   ```kotlin
   import ic2_120.Ic2_120
   import ic2_120.content.block.GeneratorBlock
   import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder
   import net.minecraft.data.server.recipe.RecipeJsonProvider
   import net.minecraft.entity.player.PlayerEntity
   import net.minecraft.item.ItemPlacementContext
   import net.minecraft.item.Items
   import net.minecraft.recipe.book.RecipeCategory
   import net.minecraft.state.StateManager
   import net.minecraft.state.property.BooleanProperty
   import net.minecraft.util.ActionResult
   import net.minecraft.util.Hand
   import net.minecraft.util.hit.BlockHitResult
   import net.minecraft.util.Identifier
   import net.minecraft.util.math.BlockPos
   import net.minecraft.world.World
   import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
   import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
   import java.util.function.Consumer
   ```

4. **验证配方生成**
   ```bash
   ./gradlew runDatagen
   ```

5. **检查生成的配方文件**
   ```bash
   ls src/main/generated/data/ic2_120/recipes/ | grep wind_generator
   ```

---

## 重要注意事项

### 编译错误修复技巧

如果遇到 "Unresolved reference" 编译错误，请检查：
1. **导入完整路径**：确保所有引用的类都在 import 语句中
2. **使用限定类名**：如果类名冲突，使用完整包名
3. **类型推断**：Kotlin 可能需要显式类型声明
4. **缓存清理**：运行 `./gradlew clean` 清理构建缓存

### 配方格式参考

所有配方格式应该与 `plans/recipe-system-refactor.md.bak` 中的原始配方保持一致。

---

## 更新日志

- **2026-03-16**: 创建 Phase 2 迁移计划
- **2026-03-16**: 完成高优先级配方迁移（57 个）
- **2026-03-16**: 更新进度文件，准备开始中优先级配方迁移
- **2026-03-16**: 完成中优先级配方迁移（60 个，80%完成）
