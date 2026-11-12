# Curios Fabric

Curios 的 Fabric 1.21.1 实现，包名、命令、命名空间和运行时标识统一为
`curiosfabric`，可以避免与经 Connector/Kilt 加载的 NeoForge Curios 混淆。

- 模组 ID：`curiosfabric`
- Java 根包：`dev.arakiel.curiosfabric`
- 游戏内命令：`/curiosfabric`
- 数据目录：`data/<namespace>/curiosfabric/slots|entities`
- 物品标签：`#curiosfabric:<slot>`
- 必需前置：Fabric API、Gemini `>=1.0.5`
- 可选前置：JEI、Mod Menu

## 开发接口

声明槽位：

```json
{
  "order": 160,
  "icon": "curiosfabric:slot/empty_ring_slot",
  "validators": ["curiosfabric:tag"]
}
```

分配给玩家：

```json
{
  "entities": ["player"],
  "slots": ["ring"]
}
```

在 `fabric.mod.json` 中使用 `curiosfabric` entrypoint 注册槽位扩展：

```json
"entrypoints": {
  "curiosfabric": ["com.example.MyCuriosEntrypoint"]
}
```

Java API 入口为 `dev.arakiel.curiosfabric.api.CuriosApi`。

## 命令

```mcfunction
/curiosfabric add ring @s 1
/curiosfabric set necklace @s 3
/curiosfabric remove ring @s 1
/curiosfabric list
```

命令需要权限等级 2。`add`/`set`/`remove` 修改的是已注册槽位类型在玩家身上的容量；
新的槽位类型仍然通过数据包定义。

## Trinkets 兼容

每个 Trinkets 的 `group/slot` 都会独立映射为
`trinkets_<group>_<slot>`，不会把不同源槽位合并到同一个 Curios ID。槽位名称直接读取
Trinkets 的 `trinkets.slot.<group>.<slot>` 翻译，再回退到
`curiosfabric.identifier.<id>`。

槽位来源会作为 `SlotSource` 同步到客户端：

- Curios Fabric：`curiosfabric.identifier.<id>`
- NeoForge Curios：`curios.identifier.<id>`
- Trinkets：`trinkets.slot.<group>.<slot>`

## Gemini 集成

- 配置：TOML，注解字段和自动配置 GUI
- 网络：Gemini `NetworkChannel`、握手和 bundle
- 事件：Gemini 事件总线
- 实体数据：Gemini Entity Components
- 可选模组检测：Gemini Integrations

旧 JSON 配置和旧 Curios Fabric 数据不提供兼容。

## 构建

```text
gradlew.bat test build
```

许可证沿用上游 LGPL-3.0-or-later。
