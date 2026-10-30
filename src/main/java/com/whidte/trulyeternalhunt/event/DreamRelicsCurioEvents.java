package com.whidte.trulyeternalhunt.event;

import com.bmt.dream_relics.init.DRItems;
import com.whidte.trulyeternalhunt.TrulyEternalHunt;
import com.whidte.trulyeternalhunt.compat.DreamRelicsCompat;
import com.whidte.trulyeternalhunt.config.Config;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingBreatheEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

/**
 * Dream Relics trinkets that only work for players, re-applied to TBF pets (1.21.1 / NeoForge).
 *
 * <p>The original bodies are kept verbatim and only the wearer is widened to {@link LivingEntity}.
 * Trinkets whose whole effect needs a player-only API (experience, inventory, block breaking, key
 * binds, the client glow overlay) or a player-only event are deliberately left out.</p>
 *
 * <p>Covered: {@code astral_necklace}, {@code void_necklace}, {@code memory_necklace},
 * {@code heart_voice_pendant}, {@code echo_earring}, {@code tassel_ring}, {@code mist_veil_ring},
 * {@code dark_whisper_ring}, {@code endless_dream}, {@code ocean_current_blessing}.</p>
 *
 * <p>Left out, because the effect cannot be reached from the server side without a mixin:
 * {@code royal_lens} (the crit multiplier only exists on {@code CriticalHitEvent}, a player-only
 * event), {@code past_ring} (player XP and inventory), {@code years_amber} and
 * {@code rare_gold_bracelet} (block-break / harvest events), {@code pure_holy_grail},
 * {@code dream_balance}, {@code time_hourglass}, {@code soul_mirror}, {@code liminal_key},
 * {@code memory_stardust}, {@code flawless_gem}, {@code nightmare_book}, {@code observe_self_eye}
 * (client-side glow), {@code icarus_wings}, {@code elven_boots} (movement mixins) and
 * {@code dream_totem} (TBF cancels pet death before {@code LivingDeathEvent} ever fires).</p>
 *
 * <p>Registered manually, and only when Dream Relics is present.</p>
 */
public final class DreamRelicsCurioEvents {
    private DreamRelicsCurioEvents() {
    }

    private static boolean disabled() {
        return !Config.ENABLED.get() || !Config.ENABLE_DREAM_RELICS.get();
    }

    /** Common gate: server side, non-player, recorded by TBF. */
    private static LivingEntity petOrNull(LivingEntity entity) {
        if (disabled() || entity.level().isClientSide() || entity instanceof Player) {
            return null;
        }
        return DreamRelicsCompat.isTrackedPet(entity) ? entity : null;
    }

    // ------------------------------------------------------------------ damage

    /**
     * Armour penetration of the Astral Necklace has to run before vanilla applies armour, i.e. on the
     * pre-armour amount of {@link LivingIncomingDamageEvent}, so it gets its own {@code HIGHEST} handler.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPreArmorDamage(LivingIncomingDamageEvent event) {
        if (event.isCanceled()) {
            return;
        }
        if (event.getSource().is(DamageTypeTags.BYPASSES_ARMOR)) {
            // Nothing to pierce. Dream Relics' mixin ignores this tag; here the effect stays sane.
            return;
        }
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) {
            return;
        }
        LivingEntity pet = petOrNull(attacker);
        if (pet == null || !DreamRelicsCompat.hasCurio(pet, DRItems.ASTRAL_NECKLACE.get())) {
            return;
        }
        float ratio = DreamRelicsCompat.astralArmorRatio(event.getEntity(), event.getSource(), event.getAmount());
        if (ratio <= 0.0F || Math.abs(ratio - 1.0F) < 1.0E-4F) {
            return;
        }
        float before = event.getAmount();
        event.setAmount(before * ratio);
        if (Config.DEBUG.get()) {
            TrulyEternalHunt.LOGGER.info(
                    "[Truly Eternal Hunt] astral necklace: pet {} pierced armour: {} -> {}",
                    pet.getUUID(), before, event.getAmount());
        }
    }

    /**
     * Everything Dream Relics does around a hit for a player wearer. Runs after the original mod's own
     * {@code NORMAL} handler but before the Royal Crown reroute at {@code LOWEST}.
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (disabled() || event.isCanceled() || event.getEntity().level().isClientSide()) {
            return;
        }
        applyToDefender(event);
        applyToAttacker(event);
    }

    private static void applyToDefender(LivingIncomingDamageEvent event) {
        LivingEntity pet = petOrNull(event.getEntity());
        if (pet == null) {
            return;
        }
        // Heart Voice Pendant: "no single hit may take more than a share of your max health".
        if (DreamRelicsCompat.hasCurio(pet, DRItems.HEART_VOICE_PENDANT.get())) {
            float cap = DreamRelicsCompat.pendantDamageCap(pet);
            if (event.getAmount() > cap) {
                event.setAmount(cap);
            }
        }
        // Memory Necklace: bank the damage, release it on the next hit.
        if (DreamRelicsCompat.hasCurio(pet, DRItems.MEMORY_NECKLACE.get())) {
            DreamRelicsCompat.storeDamage(pet, event.getAmount());
        }
    }

    private static void applyToAttacker(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) {
            return;
        }
        LivingEntity pet = petOrNull(attacker);
        if (pet == null) {
            return;
        }
        float before = event.getAmount();

        // Echo Earring: a chance to send a sonic wave along the wearer's line of sight. The original
        // fires it first, before its own multipliers, so the wave's base damage is the incoming hit.
        if (DreamRelicsCompat.hasCurio(pet, DRItems.ECHO_EARRING.get())) {
            DreamRelicsCompat.tryEchoWave(pet, event.getAmount());
        }
        // Tassel Ring: more damage the healthier the target is.
        if (DreamRelicsCompat.hasCurio(pet, DRItems.TASSEL_RING.get())) {
            event.setAmount(event.getAmount() * DreamRelicsCompat.tasselMultiplier(event.getEntity()));
        }
        // Endless Dream: a chance to put the struck mob to sleep.
        if (DreamRelicsCompat.hasCurio(pet, DRItems.ENDLESS_DREAM.get())) {
            DreamRelicsCompat.tryPutToSleep(pet, event.getEntity());
        }
        // Memory Necklace: the stored damage is released on top, after the multipliers, as in the original.
        if (DreamRelicsCompat.hasCurio(pet, DRItems.MEMORY_NECKLACE.get())) {
            event.setAmount(DreamRelicsCompat.releaseStoredDamage(pet, event.getAmount()));
        }
        if (Config.DEBUG.get() && event.getAmount() != before) {
            TrulyEternalHunt.LOGGER.info("[Truly Eternal Hunt] dream relic: pet {} damage: {} -> {}",
                    pet.getUUID(), before, event.getAmount());
        }
    }

    // -------------------------------------------------------------- attack gate

    /** Void Necklace: immunity to damage that has no attacker behind it. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onIncomingDamageGate(LivingIncomingDamageEvent event) {
        if (event.isCanceled() || event.getSource().getEntity() != null) {
            return;
        }
        LivingEntity pet = petOrNull(event.getEntity());
        if (pet != null && DreamRelicsCompat.hasCurio(pet, DRItems.VOID_NECKLACE.get())) {
            event.setCanceled(true);
        }
    }

    // ------------------------------------------------------------- effects / AI

    /** Mist Veil Ring: no harmful effect ever sticks — the other half of "addEffect" refusing it. */
    @SubscribeEvent
    public static void onPotionApplicable(MobEffectEvent.Applicable event) {
        if (disabled()) {
            return;
        }
        LivingEntity pet = event.getEntity();
        if (pet.level().isClientSide() || pet instanceof Player
                || !DreamRelicsCompat.isTrackedPet(pet)) {
            return;
        }
        MobEffectInstance instance = event.getEffectInstance();
        if (instance == null || instance.getEffect().value().getCategory() != MobEffectCategory.HARMFUL) {
            return;
        }
        if (DreamRelicsCompat.hasCurio(pet, DRItems.MIST_VEIL_RING.get())) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    /** Dark Whisper Ring: the undead simply do not pick the wearer as a target. */
    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        if (disabled() || !(event.getNewAboutToBeSetTarget() instanceof LivingEntity target)) {
            return;
        }
        if (!event.getEntity().getType().is(EntityTypeTags.UNDEAD)) {
            return;
        }
        LivingEntity pet = petOrNull(target);
        if (pet == null || !DreamRelicsCompat.hasCurio(pet, DRItems.DARK_WHISPER_RING.get())) {
            return;
        }
        // Retaliation is still allowed, exactly like the original.
        if (event.getEntity().getLastHurtByMob() == pet) {
            return;
        }
        event.setCanceled(true);
    }

    /**
     * Ocean Current Blessing: the wearer never runs out of air underwater.
     *
     * <p>{@code isEyeInFluid(TagKey)} is deprecated on 1.21.1 but is still what Dream Relics itself
     * calls, so it is kept here to stay behaviourally identical.</p>
     */
    @SuppressWarnings("deprecation")
    @SubscribeEvent
    public static void onLivingBreathe(LivingBreatheEvent event) {
        LivingEntity pet = petOrNull(event.getEntity());
        if (pet == null || !pet.isEyeInFluid(FluidTags.WATER)) {
            return;
        }
        if (DreamRelicsCompat.hasCurio(pet, DRItems.OCEAN_CURRENT_BLESSING.get())) {
            event.setCanBreathe(true);
        }
    }
}
