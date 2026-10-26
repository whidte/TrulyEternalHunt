package com.whidte.trulyeternalhunt;

import com.mojang.logging.LogUtils;
import com.whidte.trulyeternalhunt.config.Config;
import com.whidte.trulyeternalhunt.event.HandbookEvents;
import com.whidte.trulyeternalhunt.event.HuntingBeltEvents;
import com.whidte.trulyeternalhunt.event.SpeedEvents;
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
 *   <li>Enigmatic Addons' Sanguinary Hunting Handbook ({@code enigmaticaddons:sanguinary_handbook}), when Enigmatic Addons is installed;</li>
 *   <li>Relics Hunter Belt ({@code relics:hunter_belt}), when Relics is installed.</li>
 * </ul>
 *
 * <p>Both integrations are optional and completely independent: neither target mod is required, and
 * installing only one of them still enables just that half. Every handler class that touches a target
 * mod's types is registered here instead of through {@code @Mod.EventBusSubscriber}, so its class never
 * loads when the target mod is absent.</p>
 */
@Mod(TrulyEternalHunt.MODID)
public final class TrulyEternalHunt {
    public static final String MODID = "trulyeternalhunt";
    public static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Mod id of the 1.20.1 addon that actually ships the Sanguinary Handbook. Enigmatic Legacy itself is
     * only reached through this addon, so it is not checked separately: the addon requires it.
     */
    public static final String ENIGMATIC_ADDONS = "enigmaticaddons";
    /** Mod id of Relics (hunter belt integration). */
    public static final String RELICS = "relics";

    @SuppressWarnings("removal")
    public TrulyEternalHunt() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        boolean handbook = ModList.get().isLoaded(ENIGMATIC_ADDONS);
        boolean relics = ModList.get().isLoaded(RELICS);

        if (handbook) {
            MinecraftForge.EVENT_BUS.register(HandbookEvents.class);
            MinecraftForge.EVENT_BUS.register(SpeedEvents.class);
            LOGGER.info("[Truly Eternal Hunt] Sanguinary Handbook now follows TBF pet records.");
        } else {
            LOGGER.info("[Truly Eternal Hunt] {} is not installed; handbook integration stays off.",
                    ENIGMATIC_ADDONS);
        }

        if (relics) {
            MinecraftForge.EVENT_BUS.register(HuntingBeltEvents.class);
            LOGGER.info("[Truly Eternal Hunt] Hunter Belt now follows TBF pet records.");
        } else {
            LOGGER.info("[Truly Eternal Hunt] {} is not installed; hunter belt integration stays off.",
                    RELICS);
        }

        if (!handbook && !relics) {
            LOGGER.warn("[Truly Eternal Hunt] Neither {} nor {} is installed, so this mod has nothing to do.",
                    ENIGMATIC_ADDONS, RELICS);
        }
    }
}
