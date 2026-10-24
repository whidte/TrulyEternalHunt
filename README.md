# 真正永恒的狩猎 / Truly Eternal Hunt

让 **血腥狩猎手册**（Sanguinary Hunting Handbook）的宠物加成改用 **真正永恒的伙伴（Truly Best Friends, TBF）** 的收录记录来判定。

- 1.21.1 / NeoForge：`enigmaticlegacyplus:sanguinary_handbook`（Enigmatic Legacy+ 1.1.2+）
- 1.20.1 / Forge 版本在 `master` 分支。

## 规则

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

## 实现方式

不修改 Enigmatic Legacy+ 的任何类，也不使用 Mixin：

- 在 `LivingIncomingDamageEvent` 的 `HIGHEST` 阶段精确复刻原版判定与倍率；
- 在 `LOWEST` 阶段把原版贡献除回去，再按 TBF 规则重新施加；
- 在 `EntityTickEvent.Pre` 的 `LOWEST` 阶段对攻速/移速修饰符做同样的替换。

这样既不会叠加两次，也不会误删其它模组的伤害修改。

顺带修复了上游的一个问题：1.21.1 的 ELP 用 `hasCurio` 查找这本普通书本，导致手册原本无法生效；本模组同时接受物品栏与饰品栏。

## 构建

1. 把依赖 jar 放进 `libs/`（见 [libs/README.md](libs/README.md)）。
2. `gradlew compileJava`（需要 JDK 21 工具链）。
3. `gradlew build` 产出 `build/libs/trulyeternalhunt-*.jar`。

## 配置

`config/trulyeternalhunt-common.toml`：

- `enabled`：总开关
- `replaceVanillaRule`：严格替换（默认 `true`）；`false` 时保留原版规则，只额外覆盖 TBF 宠物
- `requireCursedOne`：保留手册的诅咒者条件
- `enableSpeedEffect`：攻速/移速部分也按新规则
- `ownerCacheSeconds`：TBF 记录缓存时间
- `debug`：打印每次伤害调整
