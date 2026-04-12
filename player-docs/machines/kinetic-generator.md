# 动能发电机 (Kinetic Generator)

## 基础信息

- **电压等级**：Tier 3 (HV)
- **能量缓存**：10,000 EU
- **最大输入**：0 EU/t（不输入）
- **最大输出**：512 EU/t
- **输入**：KU（动能）

## 发电机制

### KU 转 EU 转换

- **转换比例**：4 KU = 1 EU
- **最大 KU 输入**：2,048 KU/t
- **最大 EU 输出**：512 EU/t

### 发电公式

```
EU/t = KU输入 / 4
```

---

## 与传动系统配合

动能发电机接收来自传动轴网络的 KU 并转换为 EU：

1. 传动轴提供 KU 动力
2. 动能发电机连接到传动网络
3. 将 KU 转换为 EU 输出到电网

详见 [动能系统](../systems/kinetic-transmission.md)

---

## 相关

- [风力发电机](wind-generator.md) - 风能发电
- [动能系统](../systems/kinetic-transmission.md) - KU 传动详情
