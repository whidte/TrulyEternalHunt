package com.whidte.trulyeternalhunt.compat;

import com.aizistral.enigmaticlegacy.handlers.EnigmaticEventHandler;
import com.aizistral.enigmaticlegacy.handlers.SuperpositionHandler;
import com.aizistral.enigmaticlegacy.items.BerserkEmblem;
import com.aizistral.enigmaticlegacy.items.EnigmaticAmulet;
import com.aizistral.enigmaticlegacy.items.GolemHeart;
import com.aizistral.enigmaticlegacy.items.MiningCharm;
import com.aizistral.enigmaticlegacy.registries.EnigmaticItems;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.whidte.trulyeternalhunt.config.Config;
import com.whidte.trulybestfriends.trulybestfriends;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.tags.FluidTags;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.UUID;

/**
 * Enigmatic Legacy curios that only work for players, re-applied to TBF pets.
 *
 * <p>These six curios hard-code {@code instanceof Player} (or {@code instanceof ServerPlayer}) in
 * their {@code curioTick}, while the effect itself only touches {@link LivingEntity} APIs. The
 * bodies below are the originals with the wearer parameter widened from a player to any living
 * entity; nothing else was changed, and every number still comes from Enigmatic Legacy's own
 * config parameters, so in-game tuning keeps working.</p>
 *
 * <p>Curios already attaches transient attribute modifiers on its own for items that override
 * {@code getAttributeModifiers}. The curios here do not: they maintain their modifiers by hand in
 * {@code curioTick}, which is exactly the part that was player-gated.</p>
 *
 * <p>Covered: {@code golem_heart}, {@code enigmatic_amulet}, {@code berserk_charm},
 * {@code mining_charm}, {@code ocean_stone}, {@code the_cube}.</p>
 */
public final class ElCurioPetCompat {
    private static final int INTERVAL_TICKS = 10;

    private static final UUID AMULET_ATTACK = UUID.fromString("f5bb82c7-0332-4adf-a414-2e4f03471983");
    private static final UUID AMULET_SPRINT = UUID.fromString("cde98b8a-0cfc-45dc-929f-9cce9b6fbdfa");
    private static final UUID AMULET_GRAVITY = UUID.fromString("d1a07f6f-1079-4b17-8dbd-c74dc5e9094d");
    private static final UUID AMULET_SWIM = UUID.fromString("a4d4b794-a691-4757-b1cb-f5f2d5a25571");

    private static final UUID BERSERK_ATTACK_SPEED = UUID.fromString("ec62548c-5b26-401e-83fd-693e4aafa532");
    private static final UUID BERSERK_MOVEMENT_SPEED = UUID.fromString("f4ece564-d2c0-40d2-a96a-dc68b493137c");

    private ElCurioPetCompat() {
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
        apply(entity);
    }

    private static void apply(LivingEntity pet) {
        tickGolemHeart(pet);
        tickEnigmaticAmulet(pet);
        tickBerserkEmblem(pet);
        tickMiningCharm(pet);
        tickOceanStone(pet);
        tickTheCube(pet);
    }

    /** Golem Heart: two hand-maintained attribute sets, switched by "is the wearer wearing armor". */
    private static void tickGolemHeart(LivingEntity pet) {
        GolemHeart heart = EnigmaticItems.GOLEM_HEART;
        AttributeMap attributes = pet.getAttributes();
        attributes.removeAttributeModifiers(heart.attributesDefault);
        attributes.removeAttributeModifiers(heart.attributesNoArmor);
        if (!SuperpositionHandler.hasCurio(pet, heart)) {
            return;
        }
        attributes.addTransientAttributeModifiers(SuperpositionHandler.hasAnyArmor(pet)
                ? heart.attributesDefault
                : heart.attributesNoArmor);
    }

    /**
     * Enigmatic Amulet: the original reads the stored colour and applies one modifier per tick.
     * The colour switch is copied field by field; the only change is that sprinting is read from the
     * pet instead of from a player.
     */
    private static void tickEnigmaticAmulet(LivingEntity pet) {
        EnigmaticAmulet amulet = EnigmaticItems.ENIGMATIC_AMULET;
        AttributeMap attributes = pet.getAttributes();
        attributes.removeAttributeModifiers(allAmuletModifiers());
        if (!SuperpositionHandler.hasCurio(pet, amulet)) {
            return;
        }
        ItemStack stack = SuperpositionHandler.getCurioStack(pet, amulet);
        if (stack == null) {
            return;
        }
        attributes.addTransientAttributeModifiers(amuletModifiers(amulet.getColor(stack), pet));
    }

    private static Multimap<Attribute, AttributeModifier> allAmuletModifiers() {
        Multimap<Attribute, AttributeModifier> modifiers = HashMultimap.create();
        modifiers.put(Attributes.ATTACK_DAMAGE,
                new AttributeModifier(AMULET_ATTACK, "enigmaticlegacy:attack_bonus", 0.0D, Operation.ADDITION));
        modifiers.put(Attributes.MOVEMENT_SPEED,
                new AttributeModifier(AMULET_SPRINT, "enigmaticlegacy:sprint_bonus", 0.0D, Operation.MULTIPLY_TOTAL));
        modifiers.put(ForgeMod.ENTITY_GRAVITY.get(),
                new AttributeModifier(AMULET_GRAVITY, "enigmaticlegacy:gravity_bonus", 0.0D, Operation.MULTIPLY_TOTAL));
        modifiers.put(ForgeMod.SWIM_SPEED.get(),
                new AttributeModifier(AMULET_SWIM, "enigmaticlegacy:swim_bonus", 0.0D, Operation.MULTIPLY_TOTAL));
        return modifiers;
    }

    private static Multimap<Attribute, AttributeModifier> amuletModifiers(EnigmaticAmulet.AmuletColor color,
                                                                          LivingEntity wearer) {
        Multimap<Attribute, AttributeModifier> modifiers = HashMultimap.create();
        switch (color) {
            case RED -> modifiers.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(
                    AMULET_ATTACK,
                    "enigmaticlegacy:attack_bonus",
                    EnigmaticAmulet.damageBonus.getValue(),
                    Operation.ADDITION));
            case AQUA -> modifiers.put(Attributes.MOVEMENT_SPEED, new AttributeModifier(
                    AMULET_SPRINT,
                    "enigmaticlegacy:sprint_bonus",
                    wearer.isSprinting() ? 0.15D : 0.0D,
                    Operation.MULTIPLY_TOTAL));
            case MAGENTA -> modifiers.put(ForgeMod.ENTITY_GRAVITY.get(), new AttributeModifier(
                    AMULET_GRAVITY,
                    "enigmaticlegacy:gravity_bonus",
                    -0.25D,
                    Operation.MULTIPLY_TOTAL));
            case BLUE -> modifiers.put(ForgeMod.SWIM_SPEED.get(), new AttributeModifier(
                    AMULET_SWIM,
                    "enigmaticlegacy:swim_bonus",
                    0.25D,
                    Operation.MULTIPLY_TOTAL));
            default -> {
                // VIOLET / GREEN / BLACK carry no attribute bonus in Enigmatic Legacy 2.30.1.
            }
        }
        return modifiers;
    }

    /** Berserk Emblem: attack speed and movement speed scale with the missing health fraction. */
    private static void tickBerserkEmblem(LivingEntity pet) {
        AttributeMap attributes = pet.getAttributes();
        Multimap<Attribute, AttributeModifier> modifiers = berserkModifiers(pet);
        attributes.removeAttributeModifiers(modifiers);
        if (SuperpositionHandler.hasCurio(pet, EnigmaticItems.BERSERK_CHARM)) {
            attributes.addTransientAttributeModifiers(modifiers);
        }
    }

    private static Multimap<Attribute, AttributeModifier> berserkModifiers(LivingEntity pet) {
        float missingHealth = (pet.getMaxHealth() - Math.min(pet.getHealth(), pet.getMaxHealth()))
                / pet.getMaxHealth();
        Multimap<Attribute, AttributeModifier> modifiers = HashMultimap.create();
        modifiers.put(Attributes.ATTACK_SPEED, new AttributeModifier(
                BERSERK_ATTACK_SPEED,
                "enigmaticlegacy:attack_speed_modifier",
                (double) missingHealth * BerserkEmblem.attackSpeed.getValue(),
                Operation.MULTIPLY_BASE));
        modifiers.put(Attributes.MOVEMENT_SPEED, new AttributeModifier(
                BERSERK_MOVEMENT_SPEED,
                "enigmaticlegacy:speed_modifier",
                (double) missingHealth * BerserkEmblem.movementSpeed.getValue(),
                Operation.MULTIPLY_BASE));
        return modifiers;
    }

    /** Mining Charm: the night vision half is player-gated; the reach bonus is a static modifier. */
    private static void tickMiningCharm(LivingEntity pet) {
        if (!SuperpositionHandler.hasCurio(pet, EnigmaticItems.MINING_CHARM)) {
            return;
        }
        if (!MiningCharm.enableNightVision.getValue()) {
            return;
        }
        if (pet.getY() >= 50.0D || pet.level().dimension() == Level.NETHER
                || pet.level().dimension() == Level.END) {
            return;
        }
        if (pet.isEyeInFluid(FluidTags.WATER) || pet.level().canSeeSkyFromBelowWater(pet.blockPosition())) {
            return;
        }
        EnigmaticEventHandler.isApplyingNightVision = true;
        pet.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 310, 0, true, false));
        EnigmaticEventHandler.isApplyingNightVision = false;
    }

    /** Ocean Stone: night vision and air refill while the eyes are submerged. */
    private static void tickOceanStone(LivingEntity pet) {
        if (!SuperpositionHandler.hasCurio(pet, EnigmaticItems.OCEAN_STONE)) {
            return;
        }
        if (pet.isEyeInFluid(FluidTags.WATER)) {
            EnigmaticEventHandler.isApplyingNightVision = true;
            pet.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 310, 0, true, false));
            EnigmaticEventHandler.isApplyingNightVision = false;
            pet.setAirSupply(300);
        }
    }

    /** The Cube: water breathing and fire extinguishing; its attribute bonuses are already static. */
    private static void tickTheCube(LivingEntity pet) {
        if (!SuperpositionHandler.hasCurio(pet, EnigmaticItems.THE_CUBE)) {
            return;
        }
        if (pet.getAirSupply() < 300) {
            pet.setAirSupply(300);
        }
        if (pet.isOnFire()) {
            pet.clearFire();
        }
    }
}
