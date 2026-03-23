# AGENTS.md - ic2_120 协作规范（重排版）

本文件是代理/协作者的最小执行规范。详细实现细节统一放在 `docs/`，避免重复维护。

## 1. 先看这里

- 文档总入口：`docs/README.md`
- 新机器实现：`docs/guides/machine-implementation-guide.md`
- 新物品实现：`docs/guides/item-implemented.md`
- 注解注册系统：`docs/registry/CLASS_BASED_REGISTRY.md`

## 2. 硬性约束

- 资源文件只能新增/修改在 `src/main/resources/assets/ic2_120/**`。
- `assets/ic2/**` 仅作为被引用上游资源，不可直接修改。
- 机器类改动后必须同时验证服务端与客户端编译，不接受只跑一侧。
- 涉及 Screen/ScreenHandler/SyncedData 的改动，必须检查属性顺序与同步链路一致。

## 3. 注册约定（摘要）

使用类级注解进行注册，不手写分散注册表逻辑：

- `@ModBlock`
- `@ModItem`
- `@ModBlockEntity`
- `@ModScreenHandler`
- `@ModScreen`
- `@ModCreativeTab`

主入口通过 `ClassScanner.scanAndRegister(...)` 扫描包。
注册与参数细节以 `docs/registry/CLASS_BASED_REGISTRY.md` 为准。

## 4. 机器实现最小清单

1. Block + BlockEntity + Sync + ScreenHandler + Screen 成套落地。
2. 能量容器使用现有可升级容器与组件，不重复造轮子。
3. 升级支持至少覆盖：超频、变压、储能（按机器需求可裁剪）。
4. Shift 快速移动遵循 `SlotSpec + SlotMoveHelper` 规则。
5. 语言、模型、blockstates 在 `ic2_120` 命名空间补齐。

完整模板见 `docs/guides/machine-implementation-guide.md`。

## 5. 子系统文档位置

- 电网与能量流速：`docs/systems/energy-network.md`、`docs/systems/energy-flow-sync.md`
- 流体：`docs/systems/fluid-system.md`
- 热能：`docs/systems/heat-system.md`
- 核电：`docs/systems/nuclear-power.md`
- 升级：`docs/systems/upgrade-system.md`
- 同步：`docs/systems/sync-system.md`
- 声音：`docs/systems/sound-system.md`

## 6. UI 文档位置

- Compose UI 总览：`docs/ui/compose-ui.md`
- 槽位规则：`docs/ui/slot-spec-system.md`
- DrawContext 参考：`docs/ui/drawcontext-methods.md`
- 坐标换算：`docs/ui/canner-ui-coordinates.md`
- Compose 子文档：`docs/compose-ui/*.md`

## 7. 提交前验证

推荐最小命令：

```bash
./gradlew clean compileKotlin compileClientKotlin
```

若只改文档，可跳过编译；若改 Kotlin/资源/注册链路，不可跳过。

## 8. 变更策略

- 优先复用已有组件与模式，避免引入第二套实现。
- 发现规则冲突时：以 `docs/README.md` 导航到对应主文档修正，而不是在 AGENTS.md 堆细节。
- 新增规范时，优先写入对应 `docs/{guides|systems|ui|registry}`，AGENTS.md 只保留摘要与入口。
