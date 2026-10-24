package com.whidte.trulyeternalhunt;

import com.mojang.logging.LogUtils;
import com.whidte.trulyeternalhunt.config.Config;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

/**
 * 真正永恒的狩猎 / Truly Eternal Hunt (1.21.1 / NeoForge).
 *
 * <p>A small compatibility addon that rewires the Sanguinary Hunting Handbook
 * ({@code enigmaticlegacyplus:sanguinary_handbook}) so that its pet buffs follow
 * Truly Best Friends (TBF) pet records instead of vanilla {@code OwnableEntity}
 * ownership.</p>
 */
@Mod(TrulyEternalHunt.MODID)
public final class TrulyEternalHunt {
    public static final String MODID = "trulyeternalhunt";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TrulyEternalHunt(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        LOGGER.info("[Truly Eternal Hunt] Sanguinary Handbook now follows TBF pet records.");
    }
}
