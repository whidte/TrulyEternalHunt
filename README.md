# 真正永恒的狩猎 / Truly Eternal Hunt

让宠物类物品的加成改用 **真正永恒的伙伴（Truly Best Friends, TBF）** 的收录记录来判定「谁算我的宠物」。

- 1.21.1 / NeoForge：`enigmaticlegacyplus:sanguinary_handbook`（Enigmatic Legacy+ 1.0.0+，可选）、`relics:hunting_belt`（Relics 0.12.10+，可选）
- 1.20.1 / Forge 版本在 `master` 分支。

两个联动目标都是**可选**且互相独立的，只装其中之一时只有对应的那半生效；都装上就两半同时生效。唯一的硬前置是「真正永恒的伙伴」。

## 血腥狩猎手册

只有同时满足以下条件的宠物才能获得手册加成：

1. 该宠物在 TBF 的存档里有记录：`<world>/trulybestfriends/<玩家UUID>/<宠物UUID>.nbt`；
2. 记录所在目录的主人就是当前玩家；
3. 玩家与该宠物同维度、距离在手册的有效范围内；
4. 玩家满足手册本身的诅咒者条件（可配置）；
5. 玩家物品栏里有血腥狩猎手册（兼容饰品栏）；
6. 宠物造成伤害时结算（近战或它射出的箭）。

不符合条件的原版 `OwnableEntity` 宠物（别人的狗、未驯服的猫、野生马……）不再获得手册加成；TBF 收录的任意生物（包括不实现 `OwnableEntity` 的僵尸、模组召唤物等）现在可以获得。

手册原本的数值不变：

- 基础 `+25%` 宠物攻击伤害；
- 血战沙场之证（Berserk Emblem）：按玩家缺失生命追加伤害，并给宠物攻速/移速；
- 千咒卷轴（Cursed Scroll）：按诅咒等级追加伤害；
- 烈焰巨力（Blazing Might）：等级 > II 时追加伤害。

## Relics 猎手腰带（Hunting Belt）

安装 Relics 后自动启用，把腰带的四处「谁算我的宠物」判定全部改到 TBF 记录上：

| 腰带效果 | 原版判定 | 现在 |
| --- | --- | --- |
| 兽群（Pack）伤害加成 | 攻击者是 `OwnableEntity` 且主人戴腰带 | 攻击者是该玩家的 TBF 宠物 |
| 领袖（Leader）减伤：统计半径内宠物数量 | 半径内主人是自己的 `OwnableEntity` | 半径内该玩家的 TBF 宠物（`replaceVanillaRule=false` 时为两者并集） |
| 兽群经验 / 统计 | 同上 | 同上；原版已结算过的宠物不重复结算 |
| 复活（Revival）掉落宠物骨 | 死亡的 `OwnableEntity` 宠物主人戴腰带 | 死亡的 TBF 宠物 |

腰带自身的数值、等级、Rank 修饰与目标选择（`TargetingUtils.canHarm`）全部沿用原版；只替换「谁是宠物」这一步。因此 TBF 收录的僵尸、模组召唤物等也能吃到加成，而未收录的别人的狗不会再吃你的腰带。

两点需要说明：

- 经验与宠物骨只能**追加**：原版已经结算过的原版宠物无法在不改 Relics 代码的前提下撤回，所以 `replaceVanillaRule=false/true` 对这两项都只影响「额外给谁」，不影响原版原有的那份。
- 复活沿用 Relics 自己的 `relics:hunting_belt_bone_drop` 标记，所以宠物骨的掉落替换行为与原本一致。

## 宠物骨（Pet Bone）

1.21.1 的 Relics 在「兽群」第 5 阶解锁 Revival：宠物死亡留下宠物骨，骨头充满后右键复活。但 TBF 会在致命伤时拦截死亡（`die` 被 cancel，`LivingDeathEvent` 不会触发），所以原版逻辑对 TBF 宠物完全不生效。本模组把整条链路接到 TBF 上（需要 TBF 0.2.6+，开关 `enablePetBoneCompat`，默认开）：

| 环节 | 原版 | 现在 |
| --- | --- | --- |
| 掉骨 | 宠物真正死亡时掉落 | TBF 把死亡存入快照时也掉落（主人需在线且戴有复活词条的腰带） |
| 复活 | 用宠物骨里的实体数据原地生成一个**新 UUID** 的复制体 | 用骨头触发 **TBF 的复活**：按 TBF 快照恢复，保留原 UUID 与追踪状态，满血、附带图腾效果 |
| 代价 | 宠物骨 + TBF 的复活物品与冷却（同时生效时） | 只消耗宠物骨，跳过 TBF 的复活物品与冷却 |
| TBF 常规复活 | 可用 | 掉骨期间**被拒绝**（聊天栏提示「只能用宠物骨复活」），直到用骨头复活为止 |
| 充能 | 玩家击杀非玩家生物，充能其背包里的每根骨头 | 玩家**与其 TBF 记录宠物**的击杀都会充能该玩家的骨头 |

TBF 宠物掉骨时会先在它的快照里写入复活锁（`TBF_ReviveLocked`），所以「有骨头」必然等价于「TBF 常规复活已被拒绝」，且跨重启有效。复活锁只能通过使用这根骨头（或删除该宠物数据）解除。

## 实现方式

不修改 Enigmatic Legacy+ / Relics 的任何类，也不使用 Mixin：

- 在事件 `HIGHEST` 阶段精确复刻原版判定与倍率；
- 在 `LOWEST` 阶段把原版贡献除回去，再按 TBF 规则重新施加；
- 手册：`LivingIncomingDamageEvent` + `EntityTickEvent.Pre`（攻速/移速）；
- 腰带：`LivingDamageEvent.Pre`（伤害、领袖减伤）、`LivingDamageEvent.Post`（经验）、`LivingDeathEvent`（宠物骨）；
- 宠物骨：TBF 的 `PetStoredDeathEvent`（掉骨 + 复活锁）、`PlayerInteractEvent.RightClickItem`（改用 TBF 复活）、`LivingDeathEvent`（宠物击杀充能）。

这样既不会叠加两次，也不会误删其它模组的伤害修改。

## 可选联动

`enigmaticlegacyplus` 与 `relics` 都是可选依赖，且彼此独立：

| 装了什么 | 生效的部分 |
| --- | --- |
| 只有 EL+ | 血腥狩猎手册（伤害 + 攻速/移速） |
| 只有 Relics | 猎手腰带（伤害、领袖减伤、兽群经验）+ 宠物骨整套机制 |
| 两个都有 | 全部 |
| 都没有 | 模组正常加载但什么都不做，日志里给一条 warning |

实现上，凡是会碰到目标模组类型的处理器类都**不再**用 `@EventBusSubscriber` 自动注册，而是由主类在 `ModList.get().isLoaded(...)` 为真时手动 `register(...)`。这样目标模组不存在时，这些类根本不会被加载，也就不会出现 `NoClassDefFoundError`。

顺带修复了上游的一个问题：1.21.1 的 ELP 用 `hasCurio` 查找这本普通书本，导致手册原本无法生效；本模组同时接受物品栏与饰品栏。

## 构建

1. 把依赖 jar 放进 `libs/`（见 [libs/README.md](libs/README.md)）。
2. `gradlew compileJava`（需要 JDK 21 工具链）。
3. `gradlew build` 产出 `build/libs/trulyeternalhunt-*.jar`。

## 配置

`config/trulyeternalhunt-common.toml`：

- `enabled`：总开关
- `replaceVanillaRule`：严格替换（默认 `true`）；`false` 时保留原版规则，只额外覆盖 TBF 宠物（手册与腰带共用）
- `requireCursedOne`：保留手册的诅咒者条件
- `enableSpeedEffect`：手册的攻速/移速部分也按新规则
- `enableHuntingBelt`：猎手腰带兼容总开关
- `enableHuntingBeltResistance`：腰带「领袖」减伤的宠物统计
- `enableHuntingBeltExperience`：腰带兽群经验 / 统计
- `enableHuntingBeltRevival`：腰带复活掉落宠物骨
- `enablePetBoneCompat`：宠物骨整条链路改走 TBF（掉骨 / 用骨头触发 TBF 复活 / 掉骨期间禁止 TBF 复活 / TBF 宠物击杀充能），默认开
- `ownerCacheSeconds`：TBF 记录缓存时间
- `debug`：打印每次伤害调整
