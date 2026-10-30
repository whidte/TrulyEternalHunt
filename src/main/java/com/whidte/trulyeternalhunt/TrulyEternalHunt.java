package com.whidte.trulyeternalhunt;

import com.mojang.logging.LogUtils;
import com.whidte.trulyeternalhunt.config.Config;
import com.whidte.trulyeternalhunt.event.DreamRelicsCrownEvents;
import com.whidte.trulyeternalhunt.event.DreamRelicsCurioEvents;
import com.whidte.trulyeternalhunt.event.HandbookEvents;
import com.whidte.trulyeternalhunt.event.HuntingBeltEvents;
import com.whidte.trulyeternalhunt.event.PetBoneEvents;
import com.whidte.trulyeternalhunt.event.SpeedEvents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

/**
 * 真正永恒的狩猎 / Truly Eternal Hunt (1.21.1 / NeoForge).
 *
 * <p>A small compatibility addon that rewires pet-based items so that their pet buffs follow
 * Truly Best Friends (TBF) pet records instead of vanilla {@code OwnableEntity} ownership:</p>
 *
 * <ul>
 *   <li>Enigmatic Legacy+ Sanguinary Hunting Handbook ({@code enigmaticlegacyplus:sanguinary_handbook}), when EL+ is installed;</li>
 *   <li>Relics Hunting Belt ({@code relics:hunting_belt}) and the pet bone, when Relics is installed;</li>
 *   <li>Dream Relics player-only trinkets, plus the Royal Crown ({@code dream_relics:royal_crown}) whose
 *       tamed-animal rule becomes a TBF rule, when Dream Relics is installed.</li>
 * </ul>
 *
 * <p>All integrations are optional and completely independent: no target mod is required, and
 * installing only one of them still enables just that part. Every handler class that touches a target
 * mod's types is registered here instead of through {@code @EventBusSubscriber}, so its class never
 * loads when the target mod is absent.</p>
 */
@Mod(TrulyEternalHunt.MODID)
public final class TrulyEternalHunt {
    public static final String MODID = "trulyeternalhunt";
    public static final Logger LOGGER = LogUtils.getLogger();

    /** Mod id of the 1.21.1 Enigmatic Legacy+ build (handbook + speed integration). */
    public static final String ENIGMATIC_LEGACY_PLUS = "enigmaticlegacyplus";
    /** Mod id of Relics (hunting belt + pet bone integration). */
    public static final String RELICS = "relics";
    /** Mod id of Dream Relics (royal crown reroute + player-only trinkets for pets). */
    public static final String DREAM_RELICS = "dream_relics";
    /** Mod id of Curios, which Dream Relics uses to find the worn trinket. */
    public static final String CURIOS = "curios";

    public TrulyEternalHunt(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        boolean handbook = ModList.get().isLoaded(ENIGMATIC_LEGACY_PLUS);
        boolean relics = ModList.get().isLoaded(RELICS);
        boolean dreamRelics = ModList.get().isLoaded(DREAM_RELICS) && ModList.get().isLoaded(CURIOS);

        if (handbook) {
            NeoForge.EVENT_BUS.register(HandbookEvents.class);
            NeoForge.EVENT_BUS.register(SpeedEvents.class);
            LOGGER.info("[Truly Eternal Hunt] Sanguinary Handbook now follows TBF pet records.");
        } else {
            LOGGER.info("[Truly Eternal Hunt] {} is not installed; handbook integration stays off.",
                    ENIGMATIC_LEGACY_PLUS);
        }

        if (relics) {
            NeoForge.EVENT_BUS.register(HuntingBeltEvents.class);
            NeoForge.EVENT_BUS.register(PetBoneEvents.class);
            LOGGER.info("[Truly Eternal Hunt] Hunting Belt now follows TBF pet records.");
        } else {
            LOGGER.info("[Truly Eternal Hunt] {} is not installed; hunting belt integration stays off.",
                    RELICS);
        }

        if (dreamRelics) {
            NeoForge.EVENT_BUS.register(DreamRelicsCrownEvents.class);
            NeoForge.EVENT_BUS.register(DreamRelicsCurioEvents.class);
            LOGGER.info("[Truly Eternal Hunt] Royal Crown and player-only Dream Relics trinkets now work on TBF pets.");
        } else {
            LOGGER.info("[Truly Eternal Hunt] {} (with {}) is not installed; Dream Relics integration stays off.",
                    DREAM_RELICS, CURIOS);
        }

        if (!handbook && !relics && !dreamRelics) {
            LOGGER.warn("[Truly Eternal Hunt] None of {}, {} or {} is installed, so this mod has nothing to do.",
                    ENIGMATIC_LEGACY_PLUS, RELICS, DREAM_RELICS);
        }
    }
}
