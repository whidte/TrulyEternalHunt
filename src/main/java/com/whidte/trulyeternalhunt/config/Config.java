package com.whidte.trulyeternalhunt.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Common config for the handbook override. */
public final class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ENABLED = BUILDER
            .comment("Enable the Sanguinary Handbook override.")
            .define("enabled", true);

    public static final ModConfigSpec.BooleanValue REPLACE_VANILLA_RULE = BUILDER
            .comment("true: only TBF-recorded pets owned by the player receive the handbook / hunting belt pet effects.",
                    "false: keep the vanilla OwnableEntity rule and merely add TBF pets that it misses.")
            .define("replaceVanillaRule", true);

    public static final ModConfigSpec.BooleanValue REQUIRE_CURSED_ONE = BUILDER
            .comment("Keep the handbook's cursed-one requirement (Cursed Ring / Seven Curses).")
            .define("requireCursedOne", true);

    public static final ModConfigSpec.BooleanValue ENABLE_SPEED_EFFECT = BUILDER
            .comment("Also reroute the handbook's pet attack speed / movement speed effect (needs the Berserk Emblem).")
            .define("enableSpeedEffect", true);

    public static final ModConfigSpec.IntValue OWNER_CACHE_SECONDS = BUILDER
            .comment("How long a resolved TBF pet owner / record is cached, in seconds.")
            .defineInRange("ownerCacheSeconds", 5, 1, 60);

    public static final ModConfigSpec.BooleanValue DEBUG = BUILDER
            .comment("Log every damage adjustment.")
            .define("debug", false);

    public static final ModConfigSpec.BooleanValue ENABLE_HUNTING_BELT = BUILDER
            .comment("Enable the Relics Hunting Belt override (only active when Relics is installed).",
                    "Every belt rule below is part of this switch.")
            .define("enableHuntingBelt", true);

    public static final ModConfigSpec.BooleanValue ENABLE_HUNTING_BELT_RESISTANCE = BUILDER
            .comment("Reroute the Leader rank damage resistance (nearby pet count).")
            .define("enableHuntingBeltResistance", true);

    public static final ModConfigSpec.BooleanValue ENABLE_HUNTING_BELT_EXPERIENCE = BUILDER
            .comment("Also award Pack experience / statistics when a TBF pet deals damage.")
            .define("enableHuntingBeltExperience", true);

    public static final ModConfigSpec.BooleanValue ENABLE_HUNTING_BELT_REVIVAL = BUILDER
            .comment("Also drop a pet bone when a TBF pet dies while the owner has the Revival rank.")
            .define("enableHuntingBeltRevival", true);

    public static final ModConfigSpec.BooleanValue ENABLE_PET_BONE_COMPAT = BUILDER
            .comment("Rework the pet bone into the TBF revive path (only active when Relics is installed).",
                    "true: a TBF-stored death also drops a pet bone, that bone is what revives the pet through TBF,",
                    "      TBF's own revive is refused until the bone is used, and TBF pets charge their owner's bones.",
                    "false: bones only come from Relics' own death handling and revive a plain entity copy, and only",
                    "      the killer's own inventory charges bones.")
            .define("enablePetBoneCompat", true);

    public static final ModConfigSpec.BooleanValue ENABLE_DREAM_RELICS = BUILDER
            .comment("Enable the Dream Relics integration (only active when Dream Relics is installed).",
                    "Trinkets handled: astral_necklace, void_necklace, memory_necklace, heart_voice_pendant,",
                    "echo_earring, tassel_ring, mist_veil_ring, dark_whisper_ring, endless_dream,",
                    "ocean_current_blessing, plus the royal_crown reroute below.")
            .define("enableDreamRelics", true);

    public static final ModConfigSpec.BooleanValue ENABLE_DREAM_RELICS_CROWN = BUILDER
            .comment("Reroute the Royal Crown: its tamed-animal rule becomes",
                    "\"creatures recorded by TBF under the player wearing the crown\".",
                    "Covers the pet damage reduction, the pet damage boost and the pet size scaling.",
                    "Respects replaceVanillaRule: when that is false the tamed rule stays and TBF pets",
                    "are merely added on top.")
            .define("enableDreamRelicsCrown", true);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private Config() {
    }
}
