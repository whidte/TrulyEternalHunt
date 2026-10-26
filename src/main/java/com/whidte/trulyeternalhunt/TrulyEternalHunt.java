package com.whidte.trulyeternalhunt;

import com.mojang.logging.LogUtils;
import com.whidte.trulyeternalhunt.config.Config;
import com.whidte.trulyeternalhunt.event.HuntingBeltEvents;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.slf4j.Logger;

/**
 * 真正永恒的狩猎 / Truly Eternal Hunt.
 *
 * <p>A small compatibility addon that rewires pet-based items so that their pet buffs follow
 * Truly Best Friends (TBF) pet records instead of vanilla {@code OwnableEntity} ownership:</p>
 *
 * <ul>
 *   <li>Enigmatic Addons' Sanguinary Hunting Handbook ({@code enigmaticaddons:sanguinary_handbook});</li>
 *   <li>Relics Hunter Belt ({@code relics:hunter_belt}), when Relics is installed.</li>
 * </ul>
 */
@Mod(TrulyEternalHunt.MODID)
public final class TrulyEternalHunt {
    public static final String MODID = "trulyeternalhunt";
    public static final Logger LOGGER = LogUtils.getLogger();

    @SuppressWarnings("removal")
    public TrulyEternalHunt() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        LOGGER.info("[Truly Eternal Hunt] Sanguinary Handbook now follows TBF pet records.");

        // Relics support is optional; registering only when it is present keeps its classes unloaded
        // on a mod list without it.
        if (ModList.get().isLoaded("relics")) {
            MinecraftForge.EVENT_BUS.register(HuntingBeltEvents.class);
            LOGGER.info("[Truly Eternal Hunt] Hunter Belt now follows TBF pet records.");
        }
    }
}
