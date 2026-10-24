package com.whidte.trulyeternalhunt.event;

import com.whidte.trulyeternalhunt.TrulyEternalHunt;
import com.whidte.trulyeternalhunt.compat.HandbookCompat;
import com.whidte.trulyeternalhunt.compat.TbfPets;
import com.whidte.trulyeternalhunt.config.Config;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Reroutes the Sanguinary Handbook's pet damage buff.
 *
 * <p>The original handler applies its multiplier at {@code NORMAL} priority. We snapshot the
 * multiplier it is about to apply at {@code HIGHEST}, then at {@code LOWEST} divide that exact
 * factor back out and apply our own rule on top. This removes the vanilla qualification without
 * touching the other mod's event handler and without discarding unrelated damage modifiers.</p>
 */
@EventBusSubscriber(modid = TrulyEternalHunt.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class HandbookEvents {
    private static final Map<LivingIncomingDamageEvent, Double> ORIGINAL_FACTORS =
            Collections.synchronizedMap(new IdentityHashMap<>());

    private HandbookEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void captureOriginalFactor(LivingIncomingDamageEvent event) {
        if (!Config.ENABLED.get() || event.getAmount() >= Float.MAX_VALUE) {
            return;
        }
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        if (ORIGINAL_FACTORS.size() > 1024) {
            ORIGINAL_FACTORS.clear();
        }
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) {
            return;
        }
        double original = HandbookCompat.originalMultiplier(attacker);
        if (original > 0.0D) {
            ORIGINAL_FACTORS.put(event, 1.0D + original);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void applyTbfRule(LivingIncomingDamageEvent event) {
        if (!Config.ENABLED.get() || event.getAmount() >= Float.MAX_VALUE) {
            return;
        }
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        boolean replace = Config.REPLACE_VANILLA_RULE.get();
        Double factor = ORIGINAL_FACTORS.remove(event);
        if (factor != null && replace) {
            event.setAmount((float) (event.getAmount() / factor));
        }
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) {
            return;
        }
        if (!(attacker.level() instanceof ServerLevel level)) {
            return;
        }
        ServerPlayer owner = TbfPets.findOwner(level, attacker);
        if (owner == null || !HandbookCompat.qualifies(owner, attacker)) {
            return;
        }
        if (!replace && attacker instanceof OwnableEntity) {
            // Additive mode: the vanilla rule already covered this pet.
            return;
        }
        float before = event.getAmount();
        double multiplier = HandbookCompat.multiplier(owner);
        event.setAmount((float) (before * (1.0D + multiplier)));
        if (Config.DEBUG.get()) {
            TrulyEternalHunt.LOGGER.info("[Truly Eternal Hunt] pet {} of {} dealt damage: {} -> {} (+{}%)",
                    attacker.getUUID(), owner.getGameProfile().getName(),
                    before, event.getAmount(), Math.round(multiplier * 100.0D));
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        ORIGINAL_FACTORS.clear();
        TbfPets.clearCaches();
    }
}
