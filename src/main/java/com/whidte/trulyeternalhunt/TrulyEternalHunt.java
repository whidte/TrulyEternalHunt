package com.whidte.trulyeternalhunt;

import com.mojang.logging.LogUtils;
import com.whidte.trulyeternalhunt.config.Config;
import com.whidte.trulyeternalhunt.event.HuntingBeltEvents;
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
 *   <li>Enigmatic Legacy+ Sanguinary Hunting Handbook ({@code enigmaticlegacyplus:sanguinary_handbook});</li>
 *   <li>Relics Hunting Belt ({@code relics:hunting_belt}), when Relics is installed.</li>
 * </ul>
 */
@Mod(TrulyEternalHunt.MODID)
public final class TrulyEternalHunt {
    public static final String MODID = "trulyeternalhunt";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TrulyEternalHunt(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        LOGGER.info("[Truly Eternal Hunt] Sanguinary Handbook now follows TBF pet records.");

        // Relics support is optional; registering only when it is present keeps its classes unloaded
        // on a mod list without it.
        if (ModList.get().isLoaded("relics")) {
            NeoForge.EVENT_BUS.register(HuntingBeltEvents.class);
            LOGGER.info("[Truly Eternal Hunt] Hunting Belt now follows TBF pet records.");
        }
    }
}
