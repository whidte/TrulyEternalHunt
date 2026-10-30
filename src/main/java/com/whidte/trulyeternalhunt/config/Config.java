package com.whidte.trulyeternalhunt.config;

import net.minecraftforge.common.ForgeConfigSpec;

/** Common config for the handbook override. */
public final class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.BooleanValue ENABLED = BUILDER
            .comment("Enable the Sanguinary Handbook override.")
            .define("enabled", true);

    public static final ForgeConfigSpec.BooleanValue REPLACE_VANILLA_RULE = BUILDER
            .comment("true: only TBF-recorded pets owned by the player receive the handbook / hunter belt pet effects.",
                    "false: keep the vanilla OwnableEntity rule and merely add TBF pets that it misses.")
            .define("replaceVanillaRule", true);

    public static final ForgeConfigSpec.BooleanValue REQUIRE_CURSED_ONE = BUILDER
            .comment("Keep the handbook's cursed-one requirement (Cursed Ring or the Bless Ring betrayal state).")
            .define("requireCursedOne", true);

    public static final ForgeConfigSpec.BooleanValue ENABLE_SPEED_EFFECT = BUILDER
            .comment("Also reroute the handbook's pet movement speed effect (needs the Berserk Emblem).")
            .define("enableSpeedEffect", true);

    public static final ForgeConfigSpec.IntValue OWNER_CACHE_SECONDS = BUILDER
            .comment("How long a resolved TBF pet owner / record is cached, in seconds.")
            .defineInRange("ownerCacheSeconds", 5, 1, 60);

    public static final ForgeConfigSpec.BooleanValue DEBUG = BUILDER
            .comment("Log every damage adjustment.")
            .define("debug", false);

    public static final ForgeConfigSpec.BooleanValue ENABLE_HUNTING_BELT = BUILDER
            .comment("Enable the Relics Hunter Belt override (only active when Relics is installed).")
            .define("enableHuntingBelt", true);

    public static final ForgeConfigSpec.BooleanValue ENABLE_HUNTING_BELT_EXPERIENCE = BUILDER
            .comment("Also spread belt experience when a TBF pet deals damage.")
            .define("enableHuntingBeltExperience", true);

    public static final ForgeConfigSpec.BooleanValue ENABLE_CURIO_PET_COMPAT = BUILDER
            .comment("Re-apply Enigmatic Legacy / Enigmatic Addons curios to TBF pets.",
                    "Only curios whose effect is player-gated but otherwise living-entity safe are covered;",
                    "curios that need a player-only API (flight, experience, item pickup, key binds) are skipped.",
                    "Spellstones handled: golem_heart, enigmatic_amulet, berserk_charm, mining_charm,",
                    "ocean_stone, the_cube, void_pearl, eye_of_nebula, cursed_ring, forgotten_ice,",
                    "scorched_charm, hell_blade_charm, avarice_ring.")
            .define("enableCurioPetCompat", true);

    public static final ForgeConfigSpec.BooleanValue ENABLE_DREAM_RELICS = BUILDER
            .comment("Enable the Dream Relics integration (only active when Dream Relics is installed).",
                    "Trinkets handled: astral_necklace, void_necklace, memory_necklace, heart_voice_pendant,",
                    "echo_earring, tassel_ring, mist_veil_ring, dark_whisper_ring, endless_dream,",
                    "ocean_current_blessing, plus the royal_crown reroute below.")
            .define("enableDreamRelics", true);

    public static final ForgeConfigSpec.BooleanValue ENABLE_DREAM_RELICS_CROWN = BUILDER
            .comment("Reroute the Royal Crown: its tamed-animal rule becomes",
                    "\"creatures recorded by TBF under the player wearing the crown\".",
                    "Respects replaceVanillaRule: when that is false the tamed rule stays and TBF pets",
                    "are merely added on top.")
            .define("enableDreamRelicsCrown", true);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private Config() {
    }
}
