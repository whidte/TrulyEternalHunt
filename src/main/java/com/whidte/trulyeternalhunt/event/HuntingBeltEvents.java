package com.whidte.trulyeternalhunt.event;

import com.whidte.trulyeternalhunt.TrulyEternalHunt;
import com.whidte.trulyeternalhunt.compat.HunterBeltCompat;
import com.whidte.trulyeternalhunt.compat.TbfPets;
import com.whidte.trulyeternalhunt.config.Config;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Reroutes Relics' Hunter Belt pet checks onto TBF records (1.20.1).
 *
 * <p>The original only recognises tamed animals whose owner carries the belt. The original
 * multiplier is snapshotted at {@code HIGHEST} of {@link LivingHurtEvent}, divided back out at
 * {@code LOWEST} and replaced by the TBF rule, exactly like the handbook override.</p>
 *
 * <p>This class is registered manually and only when Relics is loaded, so the hard references to
 * Relics classes never resolve on a mod list without it.</p>
 */
public final class HuntingBeltEvents {
    private static final Map<LivingHurtEvent, Double> ORIGINAL_FACTORS =
            Collections.synchronizedMap(new IdentityHashMap<>());

    private HuntingBeltEvents() {
    }

    private static boolean disabled() {
        return !Config.ENABLED.get() || !Config.ENABLE_HUNTING_BELT.get();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void captureOriginalFactor(LivingHurtEvent event) {
        if (disabled() || event.getEntity().level().isClientSide()) {
            return;
        }
        if (ORIGINAL_FACTORS.size() > 1024) {
            ORIGINAL_FACTORS.clear();
        }
        if (!(event.getSource().getEntity() instanceof LivingEntity pet)) {
            return;
        }
        double factor = HunterBeltCompat.originalFactor(pet);
        if (factor > 0.0D) {
            ORIGINAL_FACTORS.put(event, factor);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void applyTbfRule(LivingHurtEvent event) {
        if (disabled() || event.getEntity().level().isClientSide()) {
            return;
        }
        boolean replace = Config.REPLACE_VANILLA_RULE.get();
        Double factor = ORIGINAL_FACTORS.remove(event);
        if (factor != null && replace) {
            event.setAmount((float) (event.getAmount() / factor));
        }
        if (!(event.getSource().getEntity() instanceof LivingEntity pet)) {
            return;
        }
        if (!(pet.level() instanceof ServerLevel level)) {
            return;
        }
        ServerPlayer owner = TbfPets.findOwner(level, pet);
        if (owner == null || !TbfPets.isTbfPetOf(owner, pet)) {
            return;
        }
        if (!replace && HunterBeltCompat.originalCovers(owner, pet)) {
            // Additive mode: the vanilla rule already covered this pet.
            return;
        }
        double own = HunterBeltCompat.factorFor(owner);
        if (own <= 0.0D) {
            return;
        }
        float before = event.getAmount();
        event.setAmount((float) (before * own));
        HunterBeltCompat.awardExperience(owner);
        if (Config.DEBUG.get()) {
            TrulyEternalHunt.LOGGER.info("[Truly Eternal Hunt] hunter belt: pet {} of {} dealt damage: {} -> {} (x{})",
                    pet.getUUID(), owner.getGameProfile().getName(), before, event.getAmount(), own);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        ORIGINAL_FACTORS.clear();
    }
}
