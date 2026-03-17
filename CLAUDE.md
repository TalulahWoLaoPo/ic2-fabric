# Claude Code - IC2-120 项目指南

当处理此项目时，请务必先阅读 **AGENTS.md** 文件。

## AGENTS.md 包含以下关键信息：

1. **Mod 注册流程** - 使用类级别注解（@ModBlock、@ModItem 等）进行快速注册
2. **子系统概览** - 电力(EU)、流体、热能(HU)、核电、升级、同步系统
3. **机器实现模板** - 完整的 Block → BlockEntity → ScreenHandler → Screen 实现流程
4. **配方系统** - 如何添加处理配方
5. **资源文件** - blockstates、models、lang 等配置
6. **常见问题** - 调试技巧

## 编译注意事项

实现机器时必须同时编译客户端和服务端：
```bash
./gradlew clean compileKotlin compileClientKotlin
```

## 相关文档

AGENTS.md 中列出了详细的子系统文档，位于 `docs/` 目录下。

---
**开始任何工作前，请先阅读 AGENTS.md！**
