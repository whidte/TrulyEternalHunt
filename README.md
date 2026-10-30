# 真正永恒的狩猎 / Truly Eternal Hunt

让宠物类物品的加成改用 **真正永恒的伙伴（Truly Best Friends, TBF）** 的收录记录来判定「谁算我的宠物」。

- 1.20.1 / Forge：`enigmaticaddons:sanguinary_handbook`（Enigmatic Addons 1.2.6+，可选）、`relics:hunter_belt`（Relics 0.8.0+，可选）、`dream_relics:royal_crown` 等（Dream Relics 1.0.3+，可选）
- 1.21.1 / NeoForge 版本在 `1.21.1` 分支。

三个联动目标都是**可选**且互相独立的，只装其中之一时只有对应的那部分生效；都装上就三部分同时生效。唯一的硬前置是「真正永恒的伙伴」。

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

## Dream Relics

安装 Dream Relics（`dream_relics`，需同时安装 Curios）后自动启用，分成两部分。

### 王室之冠（Royal Crown）

原版判定：佩戴者**驯服的**生物受伤 ×0.6、造成伤害 ×1.2 —— 也就是 `TamableAnimal` 且主人是佩戴者。

现在判定：「**当前佩戴王冠玩家的 TBF 收录生物**」。链条是：

1. 这只生物（受伤方或攻击方）被 TBF 收录；
2. 在它的 TBF 记录里找到记名的玩家；
3. 该玩家此刻戴着王室之冠。

于是僵尸、模组召唤物等不实现 `OwnableEntity` 的生物也能吃到这顶王冠，而未收录的别人的狗不再吃你的王冠。倍率仍读 Dream Relics 自己的配置（`royalCrownPetDamageReduction` / `royalCrownPetDamageBoost`），并沿用 `replaceVanillaRule`：设为 `false` 时保留原版驯服判定，只额外追加 TBF 生物。

### 只对玩家生效的饰品

下列饰品原本把 `instanceof Player` 写死在 `EventHandler` 或 `LivingEntity` 的 mixin 里，现在对 TBF 收录的生物同样生效；数值全部继续读 Dream Relics 的配置：

| 饰品 | 效果 |
| --- | --- |
| `astral_necklace` 星界项链 | 攻击时无视目标 30% 护甲与 10 点护甲 |
| `void_necklace` 虚空项链 | 免疫无来源伤害（摔落、火焰、虚空……） |
| `memory_necklace` 回忆项链 | 受伤时积蓄，下次攻击追加（上限读配置） |
| `heart_voice_pendant` 心声吊坠 | 单次受到的伤害不超过最大生命的一定比例 |
| `echo_earring` 回响耳坠 | 命中时有几率沿视线方向打出音波 |
| `tassel_ring` 流苏戒指 | 目标生命越高，伤害越高 |
| `mist_veil_ring` 薄雾面纱戒指 | 免疫所有负面效果 |
| `dark_whisper_ring` 暗黑低语戒指 | 亡灵不会把佩戴者选为目标 |
| `endless_dream` 无尽之梦 | 命中时有几率让目标陷入沉睡 |
| `ocean_current_blessing` 洋流祝福 | 水下不消耗氧气 |

未适配（原因）：`royal_lens`（`CriticalHitEvent` 只对玩家触发）、`past_ring`（需要玩家经验与背包）、`years_amber` / `rare_gold_bracelet`（破坏/采集事件只对玩家触发）、`pure_holy_grail` / `dream_balance`（经验）、`time_hourglass` / `moment_stone` / `soul_mirror` / `liminal_key` / `memory_stardust` / `flawless_gem` / `nightmare_book`（玩家交互与方块逻辑）、`observe_self_eye`（纯客户端辉光）、`icarus_wings` / `elven_boots`（移动与跳跃写在 mixin 里，服务器侧无法复刻）、`dream_totem`（TBF 在死亡处理阶段就拦下了宠物死亡，`LivingDeathEvent` 不会触发）。

音效与粒子照旧省略，它们只影响观感。

### 前提：宠物得先有饰品槽

Dream Relics 的饰品走 Curios。Curios 5 只为 `data/<模组>/curios/entities/*.json` 里列出的实体类型创建饰品槽，原版只列了 `minecraft:player`（Dream Relics 自己另外列了 `minecraft:armor_stand`）。也就是说**一只狼默认没有任何饰品槽**，`CuriosApi.getCuriosInventory()` 对它返回空 —— 上表、以及本模组更早的 Enigmatic Legacy / Addons 饰品层，都只有在目标生物确实戴着对应饰品时才生效。

要让生物能戴，需要给它所属的实体类型加槽位，例如数据包：

```json
// data/<命名空间>/curios/entities/pets.json
{
  "entities": ["minecraft:wolf", "minecraft:cat", "minecraft:horse"],
  "slots": ["charm", "necklace", "ring", "head", "hands", "belt", "feet", "bracelet", "body"]
}
```

TBF 能收录任意生物，而静态数据包无法枚举所有类型，所以「任意生物都能戴饰品」这一层本模组没有做 —— 那需要 Mixin 或反射改写 Curios 的内部表，与「不碰其它模组的类」这条前提冲突。

## 实现方式

不修改神秘遗物/扩展、Relics、Dream Relics 的任何类，也不使用 Mixin：

- 在 `LivingHurtEvent` 的 `HIGHEST` 阶段精确复刻原版判定与倍率；
- 在 `LOWEST` 阶段把原版贡献除回去，再按 TBF 规则重新施加；
- 在实体 tick 的 `LOWEST` 阶段对速度/攻速修饰符做同样的替换；
- Dream Relics 的「只对玩家生效」饰品直接把原方法体照抄，只把佩戴者从玩家放宽成任意生物（与神秘遗物饰品层同一套做法）。

这样既不会叠加两次，也不会误删其它模组的伤害修改。

## 可选联动

`enigmaticaddons`（连同它自己依赖的 `enigmaticlegacy`）、`relics`、`dream_relics` 都是可选依赖，且彼此独立：

| 装了什么 | 生效的部分 |
| --- | --- |
| 只有 Enigmatic Addons | 血腥狩猎手册（伤害 + 攻速/移速） |
| 只有 Relics | 猎手腰带（伤害 + 训练经验） |
| 只有 Dream Relics | 王室之冠改判定 + 只对玩家生效的饰品 |
| 想装几个装几个 | 各自对应部分同时生效 |
| 都没装 | 模组正常加载但什么都不做，日志里给一条 warning |

实现上，凡是会碰到目标模组类型的处理器类都**不再**用 `@Mod.EventBusSubscriber` 自动注册，而是由主类在 `ModList.get().isLoaded(...)` 为真时手动 `register(...)`。这样目标模组不存在时，这些类根本不会被加载，也就不会出现 `NoClassDefFoundError`。

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
- `enableCurioPetCompat`：神秘遗物 / 扩展的饰品对 TBF 生物生效
- `enableDreamRelics`：Dream Relics 联动总开关（王室之冠 + 只对玩家生效的饰品）
- `enableDreamRelicsCrown`：王室之冠的判定改写（`replaceVanillaRule` 对它同样有效）
- `ownerCacheSeconds`：TBF 记录缓存时间
- `debug`：打印每次伤害调整
