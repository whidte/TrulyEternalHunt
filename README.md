# 真正永恒的狩猎 / Truly Eternal Hunt

让宠物类物品的加成改用 **真正永恒的伙伴（Truly Best Friends, TBF）** 的收录记录来判定「谁算我的宠物」。

- 1.21.1 / NeoForge：`enigmaticlegacyplus:sanguinary_handbook`（Enigmatic Legacy+ 1.0.0+）、`relics:hunting_belt`（Relics 0.12.10+，可选）
- 1.20.1 / Forge 版本在 `master` 分支。

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

## 实现方式

不修改 Enigmatic Legacy+ / Relics 的任何类，也不使用 Mixin：

- 在事件 `HIGHEST` 阶段精确复刻原版判定与倍率；
- 在 `LOWEST` 阶段把原版贡献除回去，再按 TBF 规则重新施加；
- 手册：`LivingIncomingDamageEvent` + `EntityTickEvent.Pre`（攻速/移速）；
- 腰带：`LivingDamageEvent.Pre`（伤害、领袖减伤）、`LivingDamageEvent.Post`（经验）、`LivingDeathEvent`（宠物骨）。

这样既不会叠加两次，也不会误删其它模组的伤害修改。

Relics 支持是可选依赖：只有检测到 `relics` 已加载时才注册对应事件处理器，未安装 Relics 时这些类不会被加载。

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
- `ownerCacheSeconds`：TBF 记录缓存时间
- `debug`：打印每次伤害调整
