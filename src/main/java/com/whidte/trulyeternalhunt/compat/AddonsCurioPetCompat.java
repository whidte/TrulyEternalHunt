package com.whidte.trulyeternalhunt.compat;

import auviotre.enigmatic.addon.contents.items.HellBladeCharm;
import auviotre.enigmatic.addon.contents.items.ScorchedCharm;
import auviotre.enigmatic.addon.registries.EnigmaticAddonItems;
import com.aizistral.enigmaticlegacy.handlers.SuperpositionHandler;
import com.aizistral.enigmaticlegacy.registries.EnigmaticItems;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.whidte.trulyeternalhunt.config.Config;
import com.whidte.trulybestfriends.trulybestfriends;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.UUID;

/**
 * Enigmatic Addons curios that only work for players, re-applied to TBF pets.
 *
 * <p>Same treatment as {@link ElCurioPetCompat}: the original {@code curioTick} bodies are kept
 * verbatim and only the wearer is widened to {@link LivingEntity}. Curios whose whole logic depends
 * on a player-only API (flight, experience, item pickup, key binds, slot expansion) are deliberately
 * left out.</p>
 *
 * <p>Covered: {@code forgotten_ice}, {@code scorched_charm}, {@code hell_blade_charm},
 * {@code avarice_ring}.</p>
 */
public final class AddonsCurioPetCompat {
    private static final int INTERVAL_TICKS = 10;

    private static final UUID HELL_BLADE_ARMOR = UUID.fromString("E7EC5AC7-8D8C-9D83-A87C-1830B55951FA");
    private static final UUID HELL_BLADE_TOUGHNESS = UUID.fromString("03153759-3B92-E47E-EFED-DD4F2ECA6B47");

    private static final double FORGOTTEN_ICE_RANGE = 5.0D;
    private static final double AVARICE_RING_RANGE = 8.0D;

    private AddonsCurioPetCompat() {
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!Config.ENABLE_CURIO_PET_COMPAT.get()) {
            return;
        }
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide() || entity instanceof Player) {
            return;
        }
        if (entity.tickCount % INTERVAL_TICKS != 0) {
            return;
        }
        if (!trulybestfriends.isTrackedPet(entity.getUUID())) {
            return;
        }
        tickForgottenIce(entity);
        tickScorchedCharm(entity);
        tickHellBladeCharm(entity);
        tickAvariceRing(entity);
    }

    /** Forgotten Ice Crystal: counts how long nearby fully frozen entities stay frozen. */
    private static void tickForgottenIce(LivingEntity pet) {
        if (!SuperpositionHandler.hasCurio(pet, EnigmaticAddonItems.FORGOTTEN_ICE)) {
            return;
        }
        for (LivingEntity target : pet.level().getEntitiesOfClass(
                LivingEntity.class, pet.getBoundingBox().inflate(FORGOTTEN_ICE_RANGE))) {
            if (!target.isFullyFrozen()) {
                continue;
            }
            int tick = target.getPersistentData().getInt("ForgottenFrozenTick");
            if (target.getTicksFrozen() > 340) {
                tick++;
                target.getPersistentData().putBoolean("ForgottenFrozenHard", true);
            }
            target.getPersistentData().putInt("ForgottenFrozenTick", tick + 1);
        }
    }

    /** Scorched Charm: fire immunity, lava healing and the lava-swimming push. */
    private static void tickScorchedCharm(LivingEntity pet) {
        if (!SuperpositionHandler.hasCurio(pet, EnigmaticAddonItems.SCORCHED_CHARM)) {
            return;
        }
        if (pet.isOnFire()) {
            pet.clearFire();
        }
        if (!pet.isInLava()) {
            return;
        }
        if (pet.tickCount % 20 == 0) {
            pet.heal((float) ScorchedCharm.lavaHealAmount.getValue());
        }
        CollisionContext context = CollisionContext.of(pet);
        if (context.isAbove(LiquidBlock.STABLE_SHAPE, pet.blockPosition(), true)
                && !pet.level().getFluidState(pet.blockPosition().above()).is(FluidTags.LAVA)) {
            pet.setOnGround(true);
        } else {
            pet.setDeltaMovement(pet.getDeltaMovement().add(0.0D, pet.isCrouching() ? -0.01D : 0.07D, 0.0D));
        }
    }

    /** Hell Blade Charm: the armor / toughness penalty, softened while a Berserk Emblem is worn. */
    private static void tickHellBladeCharm(LivingEntity pet) {
        AttributeMap attributes = pet.getAttributes();
        Multimap<Attribute, AttributeModifier> modifiers = hellBladeModifiers(pet);
        attributes.removeAttributeModifiers(modifiers);
        if (SuperpositionHandler.hasCurio(pet, EnigmaticAddonItems.HELL_BLADE_CHARM)) {
            attributes.addTransientAttributeModifiers(modifiers);
        }
    }

    private static Multimap<Attribute, AttributeModifier> hellBladeModifiers(LivingEntity pet) {
        double armorModifier = HellBladeCharm.armorDebuff.getValue().asModifier()
                * (SuperpositionHandler.hasCurio(pet, EnigmaticItems.BERSERK_CHARM) ? 0.6D : 1.0D);
        Multimap<Attribute, AttributeModifier> modifiers = HashMultimap.create();
        modifiers.put(Attributes.ARMOR, new AttributeModifier(
                HELL_BLADE_ARMOR, "Hell Bonus", -armorModifier, Operation.MULTIPLY_TOTAL));
        modifiers.put(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(
                HELL_BLADE_TOUGHNESS, "Hell Bonus", -armorModifier, Operation.MULTIPLY_TOTAL));
        return modifiers;
    }

    /** Avarice Ring: pulls nearby raiders onto the wearer. Creative players are exempt, as before. */
    private static void tickAvariceRing(LivingEntity pet) {
        if (!SuperpositionHandler.hasCurio(pet, EnigmaticAddonItems.AVARICE_RING)) {
            return;
        }
        if (pet instanceof Player player && player.isCreative()) {
            return;
        }
        for (Raider raider : pet.level().getEntitiesOfClass(Raider.class,
                SuperpositionHandler.getBoundingBoxAroundEntity(pet, AVARICE_RING_RANGE))) {
            double visibility = pet.getVisibilityPercent(raider);
            double angerDistance = Math.max(AVARICE_RING_RANGE * visibility, 5.0D);
            if (!(pet.hasLineOfSight(raider) || pet.distanceTo(raider) <= 5.0F)) {
                continue;
            }
            if (raider.distanceToSqr(pet.getX(), pet.getY(), pet.getZ()) > angerDistance * angerDistance) {
                continue;
            }
            LivingEntity currentTarget = raider.getTarget();
            if (currentTarget != null && currentTarget.isAlive()) {
                if (!SuperpositionHandler.hasCurio(currentTarget, EnigmaticAddonItems.AVARICE_RING)) {
                    raider.setLastHurtByMob(pet);
                    raider.setTarget(pet);
                }
            } else {
                raider.setLastHurtByMob(pet);
                raider.setTarget(pet);
            }
        }
    }
}
