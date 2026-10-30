package com.whidte.trulyeternalhunt.event;

import com.whidte.trulyeternalhunt.TrulyEternalHunt;
import com.whidte.trulyeternalhunt.compat.DreamRelicsCompat;
import com.whidte.trulyeternalhunt.compat.TbfPets;
import com.whidte.trulyeternalhunt.config.Config;
import com.whidte.trulybestfriends.trulybestfriends;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Reroutes the Dream Relics Royal Crown onto TBF records (1.21.1 / NeoForge).
 *
 * <p>The original recognises {@code TamableAnimal}s whose owner wears the crown: they take less damage,
 * deal more, and are scaled up to {@code royalCrownPetSizeScale}. The new rule recognises the creatures
 * TBF has recorded under the player who is currently wearing the crown, so zombies, summons and modded
 * mobs that never implement {@code OwnableEntity} are covered too.</p>
 *
 * <p>As with the handbook, the original damage contribution is snapshotted at {@code HIGHEST}, divided
 * back out at {@code LOWEST} and replaced by the TBF rule, so nothing is applied twice and unrelated
 * damage modifiers are left alone. The size modifier uses Dream Relics' own modifier id, so the two
 * layers can never stack.</p>
 *
 * <p>Registered manually, and only when Dream Relics is present, because the helpers it calls live in
 * that mod.</p>
 */
public final class DreamRelicsCrownEvents {
    private static final Map<LivingIncomingDamageEvent, Double> ORIGINAL_FACTORS =
            Collections.synchronizedMap(new IdentityHashMap<>());

    private DreamRelicsCrownEvents() {
    }

    private static boolean disabled() {
        return !Config.ENABLED.get() || !Config.ENABLE_DREAM_RELICS.get()
                || !Config.ENABLE_DREAM_RELICS_CROWN.get();
    }

    // ------------------------------------------------------------------ damage

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void captureOriginalFactor(LivingIncomingDamageEvent event) {
        if (disabled() || event.getAmount() >= Float.MAX_VALUE) {
            return;
        }
        if (event.getEntity().level().isClientSide()) {
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
    public static void applyTbfRule(LivingIncomingDamageEvent event) {
        if (disabled() || event.getAmount() >= Float.MAX_VALUE) {
            return;
        }
        if (event.getEntity().level().isClientSide()) {
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

    private static void applyCrown(LivingIncomingDamageEvent event, LivingEntity pet, double multiplier,
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

    // -------------------------------------------------------------------- size

    /**
     * The crown also scales its pets. Dream Relics does that on every entity tick for {@code
     * TamableAnimal}s only; the same modifier is maintained here for TBF pets, and removed again once
     * the crown no longer applies to them.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onEntityTick(EntityTickEvent.Pre event) {
        if (!Config.ENABLED.get() || !Config.ENABLE_DREAM_RELICS.get()
                || !Config.ENABLE_DREAM_RELICS_CROWN.get()) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity pet) || pet instanceof Player) {
            return;
        }
        if (pet.level().isClientSide() || !(pet.level() instanceof ServerLevel level)) {
            return;
        }
        AttributeInstance scale = pet.getAttribute(Attributes.SCALE);
        if (scale == null) {
            return;
        }
        if (!trulybestfriends.isTrackedPet(pet.getUUID()) && !scale.hasModifier(DreamRelicsCompat.CROWN_SIZE_MODIFIER)) {
            // Neither a TBF pet nor currently scaled: nothing to do for ordinary mobs.
            return;
        }
        ServerPlayer owner = TbfPets.findOwner(level, pet);
        boolean applies = owner != null && DreamRelicsCompat.crownApplies(owner, pet);
        if (applies) {
            // Reusing Dream Relics' modifier id keeps this from stacking with its own handler.
            double amount = DreamRelicsCompat.crownSizeScale() - 1.0D;
            AttributeModifier existing = scale.getModifier(DreamRelicsCompat.CROWN_SIZE_MODIFIER);
            if (existing == null || Math.abs(existing.amount() - amount) > 0.01D) {
                scale.removeModifier(DreamRelicsCompat.CROWN_SIZE_MODIFIER);
                scale.addPermanentModifier(new AttributeModifier(
                        DreamRelicsCompat.CROWN_SIZE_MODIFIER,
                        amount,
                        AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            }
            return;
        }
        if (!Config.REPLACE_VANILLA_RULE.get() && DreamRelicsCompat.originalCovers(pet)) {
            // Additive mode: the original tamed-animal rule keeps its scale.
            return;
        }
        if (scale.hasModifier(DreamRelicsCompat.CROWN_SIZE_MODIFIER)) {
            scale.removeModifier(DreamRelicsCompat.CROWN_SIZE_MODIFIER);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        ORIGINAL_FACTORS.clear();
        TbfPets.clearCaches();
    }
}
