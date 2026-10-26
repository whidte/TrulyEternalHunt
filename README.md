# 真正永恒的狩猎 / Truly Eternal Hunt

让宠物类物品的加成改用 **真正永恒的伙伴（Truly Best Friends, TBF）** 的收录记录来判定「谁算我的宠物」。

- 1.20.1 / Forge：`enigmaticaddons:sanguinary_handbook`（Enigmatic Addons 1.2.6+）、`relics:hunter_belt`（Relics 0.8.0+，可选）
- 1.21.1 / NeoForge 版本在 `1.21.1` 分支。

## 血腥狩猎手册

只有同时满足以下条件的宠物才能获得手册加成：

1. 该宠物在 TBF 的存档里有记录：`<world>/trulybestfriends/<玩家UUID>/<宠物UUID>.nbt`；
2. 记录所在目录的主人就是当前玩家；
3. 玩家与该宠物同维度、距离在手册的有效范围内；
4. 玩家满足手册本身的诅咒者条件（可配置）；
5. 玩家物品栏里有血腥狩猎手册；
6. 宠物造成伤害时结算（近战或它射出的箭）。

不符合条件的原版 `OwnableEntity` 宠物（别人的狗、未驯服的猫、野生马……）不再获得手册加成；TBF 收录的任意生物（包括不实现 `OwnableEntity` 的僵尸、模组召唤物等）现在可以获得。

手册原本的数值不变：

- 基础 `+25%` 宠物攻击伤害；
- 血战沙场之证（Berserk Emblem）：按玩家缺失生命追加伤害，并给宠物移速/攻速；
- 千咒卷轴（Cursed Scroll）：按诅咒等级追加伤害；
- 1.21.1 额外的烈焰巨力（Blazing Might）联动。

## Relics 猎手腰带（Hunter Belt）

安装 Relics 后自动启用，把腰带的「谁算我的宠物」判定改到 TBF 记录上：

| 腰带效果 | 原版判定 | 现在 |
| --- | --- | --- |
| 训练（Training）伤害倍率 | 攻击者是 `TamableAnimal` 且主人戴腰带 | 攻击者是该玩家的 TBF 宠物 |
| 攻击经验 | 同上 | 同上 |

腰带自身的倍率、等级与饰品槽加成全部沿用原版；只替换「谁是宠物」这一步。因此 TBF 收录的僵尸、模组召唤物等也能吃到加成，而未收录的别人的狗不会再吃你的腰带。

需要说明：经验只能**追加**——原版已经结算过的原版宠物无法在不改 Relics 代码的前提下撤回，所以 `replaceVanillaRule` 对经验这项只影响「额外给谁」，不影响原版原有的那份。

## 实现方式

不修改神秘遗物/扩展、Relics 的任何类，也不使用 Mixin：

- 在 `LivingHurtEvent` 的 `HIGHEST` 阶段精确复刻原版判定与倍率；
- 在 `LOWEST` 阶段把原版贡献除回去，再按 TBF 规则重新施加；
- 在实体 tick 的 `LOWEST` 阶段对速度/攻速修饰符做同样的替换。

这样既不会叠加两次，也不会误删其它模组的伤害修改。

Relics 支持是可选依赖：只有检测到 `relics` 已加载时才注册对应事件处理器，未安装 Relics 时这些类不会被加载。

## 构建

1. 把依赖 jar 放进 `libs/`（见 [libs/README.md](libs/README.md)）。
2. `gradlew compileJava`（1.20.1 需要 JDK 17）。
3. `gradlew build` 产出 `build/libs/trulyeternalhunt-*.jar`。

## 配置

`config/trulyeternalhunt-common.toml`：

- `enabled`：总开关
- `replaceVanillaRule`：严格替换（默认 `true`）；`false` 时保留原版规则，只额外覆盖 TBF 宠物（手册与腰带共用）
- `requireCursedOne`：保留手册的诅咒者条件
- `enableSpeedEffect`：速度/攻速部分也按新规则
- `enableHuntingBelt`：猎手腰带兼容总开关
- `enableHuntingBeltExperience`：腰带的宠物攻击经验
- `ownerCacheSeconds`：TBF 记录缓存时间
- `debug`：打印每次伤害调整
