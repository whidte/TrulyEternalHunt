package com.whidte.trulyeternalhunt.event;

import com.whidte.trulyeternalhunt.TrulyEternalHunt;
import com.whidte.trulyeternalhunt.compat.HuntingBeltCompat;
import com.whidte.trulyeternalhunt.compat.TbfPets;
import com.whidte.trulyeternalhunt.config.Config;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Reroutes Relics' Hunting Belt pet checks onto TBF records.
 *
 * <p>Relics resolves pets through {@link OwnableEntity} in four places, all of which are handled
 * here: the Pack damage bonus, the Leader damage resistance, the Pack experience gain and the
 * Revival pet bone. The original contributions are snapshotted at {@code HIGHEST} of
 * {@link LivingDamageEvent.Pre}, divided back out at {@code LOWEST} and replaced with the TBF rule,
 * mirroring the approach used for the Sanguinary Handbook.</p>
 *
 * <p>This class is registered manually and only when Relics is loaded, so the hard references to
 * Relics classes below never resolve on a mod list without it.</p>
 */
public final class HuntingBeltEvents {
    private static final Map<LivingDamageEvent.Pre, Double> ORIGINAL_DAMAGE_FACTORS =
            Collections.synchronizedMap(new IdentityHashMap<>());
    private static final Map<LivingDamageEvent.Pre, Double> ORIGINAL_RESISTANCE_FACTORS =
            Collections.synchronizedMap(new IdentityHashMap<>());

    private HuntingBeltEvents() {
    }

    private static boolean disabled() {
        return !Config.ENABLED.get() || !Config.ENABLE_HUNTING_BELT.get();
    }

    private static boolean replaceVanilla() {
        return Config.REPLACE_VANILLA_RULE.get();
    }

    /** The pet set the belt should see: TBF-only when replacing, TBF + vanilla pets when additive. */
    private static Predicate<LivingEntity> petsOf(ServerPlayer owner) {
        Predicate<LivingEntity> tbf = candidate -> TbfPets.isTbfPetOf(owner, candidate);
        if (replaceVanilla()) {
            return tbf;
        }
        return HuntingBeltCompat.vanillaPet(owner).or(tbf);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void captureDamageFactor(LivingDamageEvent.Pre event) {
        if (disabled() || event.getEntity().level().isClientSide()) {
            return;
        }
        if (ORIGINAL_DAMAGE_FACTORS.size() > 1024) {
            ORIGINAL_DAMAGE_FACTORS.clear();
        }
        if (!(event.getSource().getEntity() instanceof LivingEntity pet)) {
            return;
        }
        double factor = HuntingBeltCompat.originalDamageFactor(pet, event.getEntity());
        if (factor > 0.0D) {
            ORIGINAL_DAMAGE_FACTORS.put(event, factor);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void applyDamageFactor(LivingDamageEvent.Pre event) {
        if (disabled() || event.getEntity().level().isClientSide()) {
            return;
        }
        Double originalFactor = ORIGINAL_DAMAGE_FACTORS.remove(event);
        if (originalFactor != null && replaceVanilla()) {
            event.setNewDamage((float) (event.getNewDamage() / originalFactor));
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
        if (!replaceVanilla() && HuntingBeltCompat.originalCovers(owner, pet)) {
            // Additive mode: the vanilla rule already covered this pet.
            return;
        }
        double factor = HuntingBeltCompat.damageFactor(owner, event.getEntity());
        if (factor <= 0.0D) {
            return;
        }
        if (HuntingBeltCompat.ignoresInvulnerability(owner, event.getEntity())) {
            event.getEntity().invulnerableTime = 0;
        }
        float before = event.getNewDamage();
        event.setNewDamage((float) (before * factor));
        if (Config.DEBUG.get()) {
            TrulyEternalHunt.LOGGER.info(
                    "[Truly Eternal Hunt] hunting belt: pet {} of {} hit {} for {} -> {} (+{}%)",
                    pet.getUUID(), owner.getGameProfile().getName(), event.getEntity().getUUID(),
                    before, event.getNewDamage(), Math.round((factor - 1.0D) * 100.0D));
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void captureResistanceFactor(LivingDamageEvent.Pre event) {
        if (disabled() || !Config.ENABLE_HUNTING_BELT_RESISTANCE.get()) {
            return;
        }
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (ORIGINAL_RESISTANCE_FACTORS.size() > 1024) {
            ORIGINAL_RESISTANCE_FACTORS.clear();
        }
        double factor = HuntingBeltCompat.resistanceFactor(player, HuntingBeltCompat.vanillaPet(player));
        if (factor < 1.0D) {
            ORIGINAL_RESISTANCE_FACTORS.put(event, factor);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void applyResistanceFactor(LivingDamageEvent.Pre event) {
        if (disabled() || !Config.ENABLE_HUNTING_BELT_RESISTANCE.get()) {
            return;
        }
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        // The resistance block is purely multiplicative, so undo the original factor and apply ours.
        Double originalFactor = ORIGINAL_RESISTANCE_FACTORS.remove(event);
        if (originalFactor != null) {
            event.setNewDamage((float) (event.getNewDamage() / originalFactor));
        }
        double factor = HuntingBeltCompat.resistanceFactor(player, petsOf(player));
        if (factor >= 1.0D) {
            return;
        }
        float before = event.getNewDamage();
        event.setNewDamage((float) (before * factor));
        if (Config.DEBUG.get()) {
            TrulyEternalHunt.LOGGER.info(
                    "[Truly Eternal Hunt] hunting belt: leader resistance for {} reduced {} -> {}",
                    player.getGameProfile().getName(), before, event.getNewDamage());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void awardDamageExperience(LivingDamageEvent.Post event) {
        if (disabled() || !Config.ENABLE_HUNTING_BELT_EXPERIENCE.get()) {
            return;
        }
        if (event.getEntity().level().isClientSide() || event.getNewDamage() <= 0.0F) {
            return;
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
        if (HuntingBeltCompat.originalCovers(owner, pet)) {
            // The original already awarded this hit to the owner's belts.
            return;
        }
        HuntingBeltCompat.awardPetDamage(owner, event.getEntity(), event.getNewDamage());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void dropRevivalBone(LivingDeathEvent event) {
        if (disabled() || !Config.ENABLE_HUNTING_BELT_REVIVAL.get()) {
            return;
        }
        LivingEntity pet = event.getEntity();
        if (pet.level().isClientSide() || !(pet.level() instanceof ServerLevel level)) {
            return;
        }
        if (HuntingBeltCompat.isRevivalBlacklisted(pet)) {
            return;
        }
        ServerPlayer owner = TbfPets.findOwner(level, pet);
        if (owner == null || !TbfPets.isTbfPetOf(owner, pet)) {
            return;
        }
        if (HuntingBeltCompat.originalCovers(owner, pet)) {
            // The original already dropped the bone.
            return;
        }
        List<ItemStack> belts = HuntingBeltCompat.revivalBelts(owner);
        if (belts.isEmpty()) {
            return;
        }
        HuntingBeltCompat.dropPetBone(pet, owner, belts);
        if (Config.DEBUG.get()) {
            TrulyEternalHunt.LOGGER.info("[Truly Eternal Hunt] hunting belt: dropped pet bone for {} of {}",
                    pet.getUUID(), owner.getGameProfile().getName());
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        ORIGINAL_DAMAGE_FACTORS.clear();
        ORIGINAL_RESISTANCE_FACTORS.clear();
    }
}
