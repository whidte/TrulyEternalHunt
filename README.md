# 真正永恒的狩猎 / Truly Eternal Hunt

让宠物类物品的加成改用 **真正永恒的伙伴（Truly Best Friends, TBF）** 的收录记录来判定「谁算我的宠物」。

- 1.21.1 / NeoForge：`enigmaticlegacyplus:sanguinary_handbook`（Enigmatic Legacy+ 1.0.0+，可选）、`relics:hunting_belt`（Relics 0.12.10+，可选）、`dream_relics:royal_crown` 等（Dream Relics 1.0.3+，可选）
- 1.20.1 / Forge 版本在 `master` 分支。

三个联动目标都是**可选**且互相独立的，只装其中之一时只有对应的那部分生效；都装上就三部分同时生效。唯一的硬前置是「真正永恒的伙伴」。

Curios 也不是硬前置（`type="optional"`，版本范围写成 `[0,)`）：只有 Dream Relics 那一层会直接调用 Curios，其余饰品栏判定都发生在 EL+ / Relics 内部，由它们自己去要求 Curios。这样即使你装的 Curios 版本与某个模组的期望不符，也不会因为本模组而卡在启动界面。

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

## Dream Relics

安装 Dream Relics（`dream_relics`，需同时安装 Curios）后自动启用，分成两部分。

### 王室之冠（Royal Crown）

原版判定：佩戴者**驯服的**生物受伤 ×0.6、造成伤害 ×1.2、体型放大到 1.5 倍 —— 也就是 `TamableAnimal` 且主人是佩戴者。

现在判定：「**当前佩戴王冠玩家的 TBF 收录生物**」。链条是：

1. 这只生物（受伤方或攻击方）被 TBF 收录；
2. 在它的 TBF 记录里找到记名的玩家；
3. 该玩家此刻戴着王室之冠。

于是僵尸、模组召唤物等不实现 `OwnableEntity` 的生物也能吃到这顶王冠，而未收录的别人的狗不再吃你的王冠。三项效果全部跟着走：减伤、增伤、体型（体型用的是 Dream Relics 自己的修饰符 id `dream_relics:royal_crown_size`，两层不会叠加）。倍率仍读 Dream Relics 自己的配置（`royalCrownPetDamageReduction` / `royalCrownPetDamageBoost` / `royalCrownPetSizeScale`），并沿用 `replaceVanillaRule`：设为 `false` 时保留原版驯服判定，只额外追加 TBF 生物。

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
| `dark_whisper_ring` 暗黑低语戒指 | 亡灵不会把佩戴者选为目标（被反击时仍会追击） |
| `endless_dream` 无尽之梦 | 命中时有几率让目标陷入沉睡 |
| `ocean_current_blessing` 洋流祝福 | 水下不消耗氧气 |

未适配（原因）：`royal_lens`（`CriticalHitEvent` 只对玩家触发）、`past_ring`（需要玩家经验与背包）、`years_amber` / `rare_gold_bracelet`（破坏/采集事件只对玩家触发）、`pure_holy_grail` / `dream_balance`（经验）、`time_hourglass` / `moment_stone` / `soul_mirror` / `liminal_key` / `memory_stardust` / `flawless_gem` / `nightmare_book`（玩家交互与方块逻辑）、`observe_self_eye`（纯客户端辉光）、`icarus_wings` / `elven_boots`（移动与跳跃写在 mixin 里，服务器侧无法复刻）、`dream_totem`（TBF 在死亡处理阶段就拦下了宠物死亡，`LivingDeathEvent` 不会触发）。

粒子与音效照旧省略，它们只影响观感。

### 判定范围：戴着就生效，不代劳加槽

本模组只做一件事：把「是不是玩家」这一条拿掉。判定从 `instanceof Player`（或 `Player` 专用的 mixin）放宽成「**饰品栏里找得到这件饰品**」，对玩家、对 TBF 收录的生物用同一套查询：

```java
CuriosApi.getCuriosInventory(wearer).map(h -> h.isEquipped(item)).orElse(false)   // wearer 可以是任何 LivingEntity
```

所以一个生物身上是否真的有饰品槽，**由它所属的生物模组（或其数据包）决定，本模组不加槽、也不提供装备入口**。没有槽位的生物，查询自然返回 false，上表、以及本模组更早的 Enigmatic Legacy+ 饰品层对它都不生效，也不会报错或干扰原逻辑；一旦那个生物模组给它的生物配了槽位，这些饰品就直接可用，无需再做适配。

需要自己给生物加槽的作者，槽位还是走 Curios 的数据文件（放在模组 jar 或数据包里）：

```json
// data/<命名空间>/curios/entities/pets.json
{
  "entities": ["minecraft:wolf", "minecraft:cat", "minecraft:horse"],
  "slots": ["charm", "necklace", "ring", "head", "hands", "belt", "feet", "bracelet", "body"]
}
```

两点实测结论（Curios 9.5.1+1.21.1）：

- **实体只能写具体 id，不能写 `#标签`。** Curios 虽然对 `"#"` 开头的条目有分支，但它把带 `#` 的整个字符串丢进 `ResourceLocation.parse`，而 `#` 不是合法命名空间字符 —— 于是抛 `ResourceLocationException`，且这个异常没有被捕获，结果是**服务器直接启动失败**（`Failed to load datapacks, can't proceed with server load`）。1.20.1 的 Curios 5.14.1 同样不剥 `#`，只是那边构造器不校验字符集，表现为**静默建不出槽位**（不报错、也不生效）。
- 槽位名必须是已注册的槽位类型，否则 Curios 只在日志里抱怨一句就跳过。

另：原版 `/curios` 命令只接受玩家，也没有「拿着饰品右键生物」的原生交互，所以把饰品放进生物饰品栏的手段同样取决于那个生物模组。

## 实现方式

不修改 Enigmatic Legacy+ / Relics / Dream Relics 的任何类，也不使用 Mixin：

- 在事件 `HIGHEST` 阶段精确复刻原版判定与倍率；
- 在 `LOWEST` 阶段把原版贡献除回去，再按 TBF 规则重新施加；
- 手册：`LivingIncomingDamageEvent` + `EntityTickEvent.Pre`（攻速/移速）；
- 腰带：`LivingDamageEvent.Pre`（伤害、领袖减伤）、`LivingDamageEvent.Post`（经验）、`LivingDeathEvent`（宠物骨）；
- 宠物骨：TBF 的 `PetStoredDeathEvent`（掉骨 + 复活锁）、`PlayerInteractEvent.RightClickItem`（改用 TBF 复活）、`LivingDeathEvent`（宠物击杀充能）；
- Dream Relics：`LivingIncomingDamageEvent`（王室之冠的替换、护甲穿透、其余饰品）、`EntityTickEvent.Pre`（王室之冠的体型修饰符）、`LivingBreatheEvent`、`LivingChangeTargetEvent`、`MobEffectEvent.Applicable`。

「只对玩家生效」的饰品是直接把原方法体照抄、只把佩戴者从玩家放宽成任意生物，不做「先除回再重算」那一步 —— 这些效果对宠物本来就没结算过。

这样既不会叠加两次，也不会误删其它模组的伤害修改。

## 可选联动

`enigmaticlegacyplus`、`relics`、`dream_relics` 都是可选依赖，且彼此独立：

| 装了什么 | 生效的部分 |
| --- | --- |
| 只有 EL+ | 血腥狩猎手册（伤害 + 攻速/移速） |
| 只有 Relics | 猎手腰带（伤害、领袖减伤、兽群经验）+ 宠物骨整套机制 |
| 只有 Dream Relics | 王室之冠改判定 + 只对玩家生效的饰品 |
| 想装几个装几个 | 各自对应部分同时生效 |
| 都没装 | 模组正常加载但什么都不做，日志里给一条 warning |

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
- `enableDreamRelics`：Dream Relics 联动总开关（王室之冠 + 只对玩家生效的饰品）
- `enableDreamRelicsCrown`：王室之冠的判定改写（减伤、增伤、体型；`replaceVanillaRule` 对它同样有效）
- `ownerCacheSeconds`：TBF 记录缓存时间
- `debug`：打印每次伤害调整
