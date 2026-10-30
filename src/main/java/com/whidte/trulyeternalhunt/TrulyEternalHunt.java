package com.whidte.trulyeternalhunt;

import com.mojang.logging.LogUtils;
import com.whidte.trulyeternalhunt.compat.AddonsCurioPetCompat;
import com.whidte.trulyeternalhunt.compat.ElCurioPetCompat;
import com.whidte.trulyeternalhunt.compat.ElSpellstonePetCompat;
import com.whidte.trulyeternalhunt.config.Config;
import com.whidte.trulyeternalhunt.event.DreamRelicsCrownEvents;
import com.whidte.trulyeternalhunt.event.DreamRelicsCurioEvents;
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
 *   <li>Relics Hunter Belt ({@code relics:hunter_belt}), when Relics is installed;</li>
 *   <li>Dream Relics player-only trinkets, plus the Royal Crown ({@code dream_relics:royal_crown}) whose
 *       tamed-animal rule becomes a TBF rule, when Dream Relics is installed.</li>
 * </ul>
 *
 * <p>All integrations are optional and completely independent: no target mod is required, and
 * installing only one of them still enables just that part. Every handler class that touches a target
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
    /** Mod id of Enigmatic Legacy itself (curio pet compat). */
    public static final String ENIGMATIC_LEGACY = "enigmaticlegacy";
    /** Mod id of Relics (hunter belt integration). */
    public static final String RELICS = "relics";
    /** Mod id of Dream Relics (royal crown reroute + player-only trinkets for pets). */
    public static final String DREAM_RELICS = "dream_relics";
    /** Mod id of Curios, which Dream Relics uses to find the worn trinket. */
    public static final String CURIOS = "curios";

    @SuppressWarnings("removal")
    public TrulyEternalHunt() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        boolean handbook = ModList.get().isLoaded(ENIGMATIC_ADDONS);
        boolean legacy = ModList.get().isLoaded(ENIGMATIC_LEGACY);
        boolean relics = ModList.get().isLoaded(RELICS);
        boolean dreamRelics = ModList.get().isLoaded(DREAM_RELICS) && ModList.get().isLoaded(CURIOS);

        if (handbook) {
            MinecraftForge.EVENT_BUS.register(HandbookEvents.class);
            MinecraftForge.EVENT_BUS.register(SpeedEvents.class);
            LOGGER.info("[Truly Eternal Hunt] Sanguinary Handbook now follows TBF pet records.");
        } else {
            LOGGER.info("[Truly Eternal Hunt] {} is not installed; handbook integration stays off.",
                    ENIGMATIC_ADDONS);
        }

        if (legacy) {
            MinecraftForge.EVENT_BUS.register(ElCurioPetCompat.class);
            MinecraftForge.EVENT_BUS.register(ElSpellstonePetCompat.class);
            LOGGER.info("[Truly Eternal Hunt] Player-only Enigmatic Legacy curios now work on TBF pets.");
        }

        if (handbook) {
            MinecraftForge.EVENT_BUS.register(AddonsCurioPetCompat.class);
            LOGGER.info("[Truly Eternal Hunt] Player-only Enigmatic Addons curios now work on TBF pets.");
        }

        if (relics) {
            MinecraftForge.EVENT_BUS.register(HuntingBeltEvents.class);
            LOGGER.info("[Truly Eternal Hunt] Hunter Belt now follows TBF pet records.");
        } else {
            LOGGER.info("[Truly Eternal Hunt] {} is not installed; hunter belt integration stays off.",
                    RELICS);
        }

        if (dreamRelics) {
            MinecraftForge.EVENT_BUS.register(DreamRelicsCrownEvents.class);
            MinecraftForge.EVENT_BUS.register(DreamRelicsCurioEvents.class);
            LOGGER.info("[Truly Eternal Hunt] Royal Crown and player-only Dream Relics trinkets now work on TBF pets.");
        } else if (!ModList.get().isLoaded(DREAM_RELICS)) {
            LOGGER.info("[Truly Eternal Hunt] {} is not installed; Dream Relics integration stays off.",
                    DREAM_RELICS);
        } else {
            LOGGER.warn("[Truly Eternal Hunt] {} is installed but {} is missing; Dream Relics integration stays off.",
                    DREAM_RELICS, CURIOS);
        }

        if (!handbook && !legacy && !relics && !dreamRelics) {
            LOGGER.warn("[Truly Eternal Hunt] None of {}, {}, {} or {} is installed, so this mod has nothing to do.",
                    ENIGMATIC_ADDONS, ENIGMATIC_LEGACY, RELICS, DREAM_RELICS);
        }
    }
}
