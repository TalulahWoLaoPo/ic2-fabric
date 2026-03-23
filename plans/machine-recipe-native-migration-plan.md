# 机器配方原生化改造计划（可复用模板）

目标：把“机器内置代码配方表”迁移为 Minecraft 原生配方系统，并继续保持 Kotlin 侧类型安全（由 kt 导出 json）。

## 1. 改造目标

- 机器逻辑从 `XXXRecipes.getRecipe(...)` 切换为 `world.recipeManager.getFirstMatch(...)`。
- 配方存储从“代码硬编码表”切换为“原生 `RecipeType + RecipeSerializer + JSON`”。
- JSON 来源由 Kotlin datagen 导出（不手写），并使用项目现有 `RegistryExtensions.kt` 的类型安全引用。
- JEI 后续可直接读取 `RecipeManager` 的该自定义类型，不再耦合内部代码表。

## 2. 推荐目录结构

- `src/main/kotlin/ic2_120/content/recipes/<machine>/`
  - `<Machine>Recipe.kt`
  - `<Machine>RecipeSerializer.kt`
  - `ModMachineRecipes.kt`（或统一集中注册文件）
  - `<Machine>RecipeDatagen.kt`
- `src/main/generated/data/ic2_120/recipes/<recipe_type_path>/`（datagen 产物）

说明：generated 文件由 `./gradlew rundatagen` 生成，不手动编辑。

## 3. 标准改造步骤（每台机器）

1. 新增自定义 Recipe 实体
- 实现 `Recipe<SimpleInventory>`（或按机器需求使用其它 Inventory）。
- 实现 `matches/craft/getOutput/getSerializer/getType`。

2. 新增 Serializer
- 实现 `read(id, json)`、`read(id, buf)`、`write(buf, recipe)`。
- JSON 建议结构统一为：
  - `type`
  - `ingredient`（可先 item，后续可扩展 tag/list）
  - `result`（item/count）

3. 注册 RecipeType / RecipeSerializer
- 在初始化入口（当前在 `Ic2_120.onInitialize()`）执行 `register()`。
- ID 规则建议：`ic2_120:<machine_action>`（如 `macerating`、`compressing`）。

4. 机器 BlockEntity 切换到 RecipeManager
- 删除对旧代码配方表的调用。
- 改为：
  - 构造用于匹配的 `SimpleInventory`。
  - `getFirstMatch(自定义RecipeType, inv, world)`。
  - 从匹配结果读取输出并继续原有进度/能耗/产出逻辑。

5. 增加 Kotlin datagen（类型安全）
- 在 `<Machine>RecipeDatagen.kt` 中维护 `Entry` 列表。
- `Entry` 使用强类型：`Item` / `KClass.item()` / `KClass.instance()` / `Items.*`，避免字符串 id。
- 用自定义 `RecipeJsonProvider` 输出 json。

6. 接入 ClassScanner 配方导出
- 在对应方块类 companion 中实现：
  - `fun generateRecipes(exporter: Consumer<RecipeJsonProvider>)`
- 内部调用 `<Machine>RecipeDatagen.generateRecipes(exporter)`。

7. 清理旧代码配方表
- 删除旧 `XXXRecipes.kt`。
- 全局搜索确认无残留引用。

8. 验证
- 必跑：`./gradlew compileKotlin compileClientKotlin`
- 必跑：`./gradlew rundatagen`
- 检查输出：`src/main/generated/data/ic2_120/recipes/<type>/...json`

## 4. 类型安全约束（建议执行）

- datagen 禁止直接写字符串 ID 表（除极少外部资源兜底场景）。
- 优先使用：
  - 模组方块：`SomeBlock::class.item()`
  - 模组物品：`SomeItem::class.instance()`
  - 原版物品：`Items.*`
- 当引用不存在时应编译失败（而不是运行时 silently skip）。

## 5. 统一 JSON 规范（建议）

- type: `ic2_120:<machine_action>`
- ingredient:
  - 最小：`{ "item": "..." }`
  - 可扩展：支持 tag / 多输入
- result:
  - `{ "item": "...", "count": n }`

## 6. 批量迁移顺序建议

1. 单输入单输出机器（低风险）
- Macerator / Compressor / Extractor / Recycler

2. 多输出机器（中风险）
- Centrifuge（需要扩展 result 数组与 UI/处理逻辑）

3. 涉及流体/NBT/模式切换的机器（高风险）
- Canner / FluidBottler / OreWashingPlant 等

## 7. 机器差异化检查清单

- 输入槽数量与匹配 Inventory 构造是否一致。
- 输出叠加规则（同物品合并、上限检查）是否保持原行为。
- 每 tick 能耗与进度推进逻辑是否未被配方重构破坏。
- 升级组件（超频/变压/储能）行为是否保持。
- 与 ScreenHandler 同步字段顺序是否未改动。

## 8. JEI 接入（迁移后）

- 迁移后 JEI 不再读内部代码表，改读 `RecipeManager` 里的自定义类型。
- 仍需 JEI Category/Catalyst（显示层），但配方源统一从原生配方获取。

### 8.1 简易 JEI 画面最低标准（本项目约定）

- 目标：只让玩家看懂“输入是什么、输出是什么、对应机器是什么”。
- 不要求复刻机器 Screen，不要求复用 Compose 布局。
- 最小实现：
  - `registerCategories`：创建一个简洁 category（空背景 + 输入槽 + 输出槽）。
  - `registerRecipes`：从 Kotlin datagen 的同源数据导入（与 JSON 导出保持一致）。
  - `registerRecipeCatalysts`：绑定机器方块（如 `macerator`）。
- 坐标建议固定模板：左输入、右输出（避免和动态 UI 强绑定）。
- 扩展策略：后续如果需要精细 UI，再在 category `draw` 增加文本/箭头，不影响配方数据层。

## 9. 本次已完成（样板）

- 打粉机（Macerator）已完成以上链路，可作为其它机器模板。
- 后续迁移时建议直接复制 `macerator` 目录骨架改名。

## 10. 批量迁移执行清单（可直接开工）

### Phase A：单输入单输出

| 机器 | 配方系统 | JEI | 状态 |
|------|---------|-----|------|
| Macerator | ✅ | ✅ | 已完成 |
| Compressor | ✅ | ✅ | 已完成 |
| Extractor | ✅ | ✅ | 已完成 |
| Recycler | ✅ | ❌ | 仅更新为JSON |

### Phase B：多输出/附加条件

| 机器 | 配方系统 | JEI | 状态 |
|------|---------|-----|------|
| Centrifuge | ✅ | ✅ | 已完成 |
| BlockCutter | ✅ | ✅ | 已完成 |

### Phase C：复杂机器

| 机器 | 配方系统 | JEI | 状态 |
|------|---------|-----|------|
| BlastFurnace | ✅ | ✅ | 已完成 |
| OreWashingPlant | ✅ | ✅ | 已完成 |
| MetalFormer | ✅ | ✅ | 已完成（统一RecipeType，JEI分3类） |
| Canner | ❌ | ❌ | 待迁移 |
| FluidBottler | ❌ | ❌ | 待迁移 |

### 关键技术决策记录

- **MetalFormer三模式**：共用单一 RecipeType + Serializer，JSON 中用 `mode` 字段区分。
  JEI 层分为 Rolling/Cutting/Extruding 三个 Category。
- **JVM签名冲突**：sealed class 中 `val id` 与 `Recipe.getId()` 冲突，
  使用 `@JvmField val recipeId` 解决。
- **OreWashingRecipe**：`val outputs` 与 `Recipe.getOutputs()` 冲突，
  字段改名为 `outputItems`。

## 11. 每台机器 PR 模板（建议）

1. 新增 `<Machine>Recipe.kt`
2. 新增 `<Machine>RecipeSerializer.kt`
3. 在 `ModMachineRecipes` 增加 `<MACHINE>_TYPE + <MACHINE>_SERIALIZER + register`
4. 新增 `<Machine>RecipeDatagen.kt`（仅类型安全引用）
5. `<Machine>Block.kt` 增加 `generateRecipes(exporter)`
6. `<Machine>BlockEntity.kt` 切 `RecipeManager`
7. 删除旧 `XXXRecipes.kt`
8. 跑编译 + datagen
9. 附回归点（输入输出、能耗、进度、升级、同步）

## 12. 分工建议（多人并行）

- A 组：Phase A（Compressor/Extractor）- 低耦合，优先并行
- B 组：Phase B（Centrifuge/BlockCutter）- 需额外字段设计
- C 组：Phase C（流体/模式机）- 最后做，避免反复返工

并行原则：
- 每台机器独立 recipe 子目录，避免冲突
- 公共注册文件（`ModMachineRecipes`）由 1 人集中合并

## 13. 验收标准（Definition of Done）

- 旧代码配方表已删除，无引用残留
- 机器运行行为与迁移前一致（除你主动修正的已知 bug）
- datagen 输出稳定，可重复生成
- `compileKotlin + compileClientKotlin + rundatagen` 全通过
- JEI（若接入）数据来源改为原生 RecipeManager
