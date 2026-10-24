package com.whidte.trulyeternalhunt.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Common config for the handbook override. */
public final class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ENABLED = BUILDER
            .comment("Enable the Sanguinary Handbook override.")
            .define("enabled", true);

    public static final ModConfigSpec.BooleanValue REPLACE_VANILLA_RULE = BUILDER
            .comment("true: only TBF-recorded pets owned by the player receive the handbook buff.",
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

    public static final ModConfigSpec SPEC = BUILDER.build();

    private Config() {
    }
}
