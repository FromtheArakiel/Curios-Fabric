# Curios Fabric

Curios 的 Fabric 1.21.1 实现。包名、命令、命名空间和运行时标识统一为
`curiosfabric`，并通过兼容桥读取 NeoForge Curios 与 Trinkets 的槽位、实体绑定和物品标签。

- 模组 ID：`curiosfabric`
- Java 根包：`dev.arakiel.curiosfabric`
- 游戏内命令：`/curiosfabric`
- 数据目录：`data/<namespace>/curiosfabric/slots|entities`
- 物品标签：`#curiosfabric:<slot>`
- 必需前置：Fabric API、Gemini `>=1.0.5`
- 可选前置：JEI、Mod Menu、Kilt、NeoForge Curios、Trinkets

## 开发接口

声明槽位：

```json
{
  "order": 4,
  "icon": "curiosfabric:slot/empty_ring_slot",
  "validators": ["curiosfabric:tag"]
}
```

原生数据包中的 `order` 是零基锚点位置，上例会将槽位固定到位置 4。

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

## 槽位来源与顺序

槽位按来源处理，不做跨来源的简单合并：

- Curios Fabric 原生槽位使用 `curiosfabric` 数据目录。
- NeoForge Curios 的原生槽位通过语义映射表去重，第三方槽位保留独立实例。
- Trinkets 的原生槽位通过语义映射表去重，第三方槽位保留独立实例并忽略 group 表示的身体部位。
- 映射后的槽位共享物品标签语义，但实例标识仍是唯一的，并可同时被同步和渲染。

Curios Fabric 原生 JSON 的 `order` 直接作为锚点位置。例如 `order: 4` 表示占用位置 4，
不会经过任何缩放或取整。

NeoForge Curios 的 `order` 会转换为内部锚点：

```text
position = max(0, round((order - 20) / 20))
```

排序过程如下：

1. 先确定 Curios Fabric 原生锚点和 NeoForge 锚点，包括 IMC 注册的第三方槽位。
2. 同一位置的锚点按标识符 `a-z` 排序。
3. 后面的锚点被占用时继续顺延，例如 `a`、`b` 申请位置 3，`c` 申请位置 4，最终为 `a=3、b=4、c=5`。
4. NeoForge 原生映射槽位和 Trinkets 槽位没有锚点时进入队列，Trinkets 第三方槽位按发现顺序追加。
5. 队列从位置 0 开始填补未被锚点占用的位置；映射到原生槽位的 Trinkets 槽位排在同映射原生槽位之后。

Trinkets 自身同时具有 group order 和 slot order，但当前兼容策略暂时不继承这两个值。
相关改进记录为低优先级 TODO，后续实现时应保持同一 group 的槽位连续，并先按 group、
再按 group 内 slot 的顺序处理。

来源同时决定槽位语言的继承位置：

- `curiosfabric`：`curiosfabric.identifier.<id>`
- `neoforgecurios`：`curios.identifier.<id>`
- `trinkets`：`trinkets.slot.<group>.<slot>`

三种来源之间不会互相回退。缺失翻译时直接显示原始键名，并写入一次缺失日志。

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

Trinkets 原生槽位通过语义表映射到 Curios Fabric 原生槽位，例如
`hand/ring -> ring`、`head/hat -> head`。第三方 `group/slot` 追加为
`trinkets_<slot>`，忽略 group 表示的身体部位概念。槽位元数据、实体绑定、物品标签和
渲染入口均由兼容桥读取；服务端与客户端使用各自的加载器快照。

NeoForge Curios 的 `data/<namespace>/curios/slots` 和
`data/<namespace>/curios/entities` 会直接读取。第三方槽位名即使与原生槽位同名，
只要来自第三方命名空间，就保持独立实例；原生语义映射仍通过共享的 mapping identifier
识别物品和标签。

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

## 验证数据包

锚点排序的服务端验证数据包位于：

```text
run/smoketest/datapacks/order_anchor_validation
```

它覆盖 Curios Fabric 原生 `order=4` 直接锚定到位置 4、重复锚点顺延、队列补位、
NeoForge 第三方 `ring` 独立实例，以及 Trinkets 第三方槽位忽略自身 `order` 后进入
后置队列的行为。

许可证沿用上游 LGPL-3.0-or-later，版权持有者为 FromtheArakiel，2026。
