package com.whidte.trulyeternalhunt.event;

import com.whidte.trulyeternalhunt.TrulyEternalHunt;
import com.whidte.trulyeternalhunt.compat.DreamRelicsCompat;
import com.whidte.trulyeternalhunt.compat.TbfPets;
import com.whidte.trulyeternalhunt.config.Config;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Reroutes the Dream Relics Royal Crown onto TBF records (1.20.1).
 *
 * <p>The original recognises {@code TamableAnimal}s whose owner wears the crown: they take less damage
 * and deal more. The new rule recognises the creatures TBF has recorded under the player who is
 * currently wearing the crown, so zombies, summons and modded mobs that never implement
 * {@code OwnableEntity} are covered too.</p>
 *
 * <p>As with the handbook, the original contribution is snapshotted at {@code HIGHEST}, divided back
 * out at {@code LOWEST} and replaced by the TBF rule, so nothing is applied twice and unrelated
 * damage modifiers are left alone.</p>
 *
 * <p>Registered manually, and only when Dream Relics is present, because the helpers it calls live in
 * that mod.</p>
 */
public final class DreamRelicsCrownEvents {
    private static final Map<LivingHurtEvent, Double> ORIGINAL_FACTORS =
            Collections.synchronizedMap(new IdentityHashMap<>());

    private DreamRelicsCrownEvents() {
    }

    private static boolean disabled() {
        return !Config.ENABLED.get() || !Config.ENABLE_DREAM_RELICS.get()
                || !Config.ENABLE_DREAM_RELICS_CROWN.get();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void captureOriginalFactor(LivingHurtEvent event) {
        if (disabled() || event.getEntity().level().isClientSide()) {
            return;
        }
        if (DreamRelicsCompat.crownReduction() <= 0.0D || DreamRelicsCompat.crownBoost() <= 0.0D) {
            // Config values not loaded yet; a zero multiplier would erase the hit.
            return;
        }
        if (ORIGINAL_FACTORS.size() > 1024) {
            ORIGINAL_FACTORS.clear();
        }
        double factor = DreamRelicsCompat.originalCrownFactor(
                event.getSource().getEntity(), event.getEntity());
        if (factor != 1.0D) {
            ORIGINAL_FACTORS.put(event, factor);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void applyTbfRule(LivingHurtEvent event) {
        if (disabled() || event.getEntity().level().isClientSide()) {
            return;
        }
        if (DreamRelicsCompat.crownReduction() <= 0.0D || DreamRelicsCompat.crownBoost() <= 0.0D) {
            return;
        }
        boolean replace = Config.REPLACE_VANILLA_RULE.get();
        Double factor = ORIGINAL_FACTORS.remove(event);
        if (factor != null && replace) {
            event.setAmount((float) (event.getAmount() / factor));
        }
        if (!(event.getEntity().level() instanceof ServerLevel level)) {
            return;
        }

        LivingEntity victim = event.getEntity();
        if (!(victim instanceof Player)) {
            ServerPlayer owner = TbfPets.findOwner(level, victim);
            if (owner != null && DreamRelicsCompat.crownApplies(owner, victim)
                    && (replace || !DreamRelicsCompat.originalCovers(victim))) {
                applyCrown(event, victim, DreamRelicsCompat.crownReduction(), owner, "took");
            }
        }

        if (event.getSource().getEntity() instanceof LivingEntity attacker
                && !(attacker instanceof Player)) {
            ServerPlayer owner = TbfPets.findOwner(level, attacker);
            if (owner != null && DreamRelicsCompat.crownApplies(owner, attacker)
                    && (replace || !DreamRelicsCompat.originalCovers(attacker))) {
                applyCrown(event, attacker, DreamRelicsCompat.crownBoost(), owner, "dealt");
            }
        }
    }

    private static void applyCrown(LivingHurtEvent event, LivingEntity pet, double multiplier,
                                   ServerPlayer owner, String side) {
        float before = event.getAmount();
        event.setAmount((float) (before * multiplier));
        if (Config.DEBUG.get()) {
            TrulyEternalHunt.LOGGER.info(
                    "[Truly Eternal Hunt] royal crown: pet {} of {} {} damage: {} -> {} (x{})",
                    pet.getUUID(), owner.getGameProfile().getName(), side,
                    before, event.getAmount(), multiplier);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        ORIGINAL_FACTORS.clear();
        TbfPets.clearCaches();
    }
}
