package com.whidte.trulyeternalhunt.compat;

import com.aizistral.enigmaticlegacy.handlers.EnigmaticEventHandler;
import com.aizistral.enigmaticlegacy.handlers.SuperpositionHandler;
import com.aizistral.enigmaticlegacy.items.CursedRing;
import com.aizistral.enigmaticlegacy.items.EyeOfNebula;
import com.aizistral.enigmaticlegacy.items.VoidPearl;
import com.aizistral.enigmaticlegacy.items.generic.ItemSpellstoneCurio;
import com.aizistral.enigmaticlegacy.registries.EnigmaticDamageTypes;
import com.aizistral.enigmaticlegacy.registries.EnigmaticItems;
import com.whidte.trulyeternalhunt.config.Config;
import com.whidte.trulybestfriends.trulybestfriends;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Enigmatic Legacy spellstones and the Ring of the Seven Curses, re-applied to TBF pets.
 *
 * <p>These items keep every meaningful effect behind {@code instanceof Player} —
 * {@code VoidPearl.curioTick}, {@code EnigmaticEventHandler.onApplyPotion},
 * {@code onLivingDeath}, {@code onEntityAttacked} (dodge), {@code onEntityHurt} (the spellstone
 * resistance pass, the damage multipliers and the Seven Curses) — while the effect bodies
 * themselves only touch {@link LivingEntity} APIs. The implementations below are the originals
 * with the wearer widened from a player to any tracked pet; durations, amplifiers and multipliers
 * still come from Enigmatic Legacy's own config parameters.</p>
 *
 * <p>Covered: {@code void_pearl} (Pearl of the Void), {@code eye_of_nebula} (Eye of the Nebula),
 * {@code cursed_ring} (Ring of the Seven Curses).</p>
 *
 * <p>The ring is intentionally limited to the damage-side curses — pain, the monster damage
 * penalty, the armor penalty and knockback. That is exactly the set Enigmatic Legacy Plus grants a
 * pet in 1.21.1, where the neutral-creature rage and the eternal burning are still guarded by
 * {@code instanceof Player}; its armor penalty is a static {@code getAttributeModifiers} entry, so
 * Curios already applies it to any wearer and nothing has to be done here for it.</p>
 *
 * <p>Deliberate deviations, all of them safety rails that only exist because the wearer is a pet
 * rather than the player themself:</p>
 * <ul>
 *   <li>the Void Pearl darkness aura never touches the pet's owner, the pet itself, or any other
 *       TBF-collected pet, and it skips allied entities;</li>
 *   <li>sound effects are omitted, they are cosmetic only.</li>
 * </ul>
 */
public final class ElSpellstonePetCompat {
    private static final int INTERVAL_TICKS = 10;
    @SuppressWarnings("removal")
    private static final ResourceLocation CHRONO_EXHAUSTION =
            new ResourceLocation("mana-and-artifice", "chrono-exhaustion");

    private ElSpellstonePetCompat() {
    }

    /** Common gate: server side, non-player, tick interval, and recorded by TBF. */
    private static LivingEntity petOrNull(LivingEntity entity) {
        if (!Config.ENABLE_CURIO_PET_COMPAT.get()) {
            return null;
        }
        if (entity.level().isClientSide() || entity instanceof Player) {
            return null;
        }
        if (entity.tickCount % INTERVAL_TICKS != 0) {
            return null;
        }
        return trulybestfriends.isTrackedPet(entity.getUUID()) ? entity : null;
    }

    // ------------------------------------------------------------------ ticks

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity pet = petOrNull(event.getEntity());
        if (pet == null) {
            return;
        }
        tickVoidPearl(pet);
    }

    // ------------------------------------------------------- Pearl of the Void

    private static void tickVoidPearl(LivingEntity pet) {
        if (!SuperpositionHandler.hasCurio(pet, EnigmaticItems.VOID_PEARL)) {
            return;
        }
        // "You no longer need air to breathe" + the flame snuffing.
        if (pet.getAirSupply() < 300) {
            pet.setAirSupply(300);
        }
        if (pet.isOnFire()) {
            pet.clearFire();
        }
        purgeEffects(pet);
        voidPearlAura(pet);
    }

    /**
     * "Immunity to any status effects": the original removes everything except a night vision
     * instance of exactly the duration its own curios hand out, and except Mana and Artifice's
     * chrono-exhaustion.
     */
    private static void purgeEffects(LivingEntity pet) {
        for (MobEffectInstance effect : pet.getActiveEffects().toArray(new MobEffectInstance[0])) {
            boolean keep;
            if (effect.getEffect() == MobEffects.NIGHT_VISION) {
                keep = effect.getDuration() >= 300 && effect.getDuration() <= 310;
            } else {
                keep = CHRONO_EXHAUSTION.equals(ForgeRegistries.MOB_EFFECTS.getKey(effect.getEffect()));
            }
            if (!keep) {
                pet.removeEffect(effect.getEffect());
            }
        }
    }

    /**
     * "Any creatures nearby you, if they are exposed to darkness, receive massive damage and heavy
     * debuffs. Phantoms are affected regardless of light level."
     *
     * <p>The original damages everything except the wearer, plus allied players. A pet has no
     * notion of "allied players" beyond its owner, and pets of the same owner must not eat each
     * other, so those are excluded explicitly.</p>
     */
    private static void voidPearlAura(LivingEntity pet) {
        if (!(pet.level() instanceof ServerLevel level)) {
            return;
        }
        double range = VoidPearl.shadowRange.getValue();
        LivingEntity owner = TbfPets.findOwner(level, pet);
        DamageSource darkness = new DamageSource(
                level.registryAccess()
                        .registryOrThrow(Registries.DAMAGE_TYPE)
                        .getHolderOrThrow(EnigmaticDamageTypes.DARKNESS),
                pet);
        for (LivingEntity victim : level.getEntitiesOfClass(
                LivingEntity.class,
                SuperpositionHandler.getBoundingBoxAroundEntity(pet, range))) {
            if (victim == pet || victim == owner) {
                continue;
            }
            if (victim instanceof Player || trulybestfriends.isTrackedPet(victim.getUUID())) {
                continue;
            }
            if (pet.isAlliedTo(victim)) {
                continue;
            }
            boolean inDarkness = level.getMaxLocalRawBrightness(victim.blockPosition(), 0) < 3
                    || victim instanceof Phantom && !victim.isOnFire();
            if (!inDarkness) {
                continue;
            }
            if (victim.hurt(darkness, (float) VoidPearl.baseDarknessDamage.getValue())) {
                victim.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 1, false, true));
                victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2, false, true));
                victim.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0, false, true));
                victim.addEffect(new MobEffectInstance(MobEffects.HUNGER, 160, 2, false, true));
                victim.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 100, 3, false, true));
            }
        }
    }

    /** "Immunity to any status effects" from the other side: refuse every incoming effect. */
    @SubscribeEvent
    public static void onPotionApplicable(MobEffectEvent.Applicable event) {
        if (!Config.ENABLE_CURIO_PET_COMPAT.get()) {
            return;
        }
        LivingEntity pet = event.getEntity();
        if (pet.level().isClientSide() || pet instanceof Player) {
            return;
        }
        if (EnigmaticEventHandler.isApplyingNightVision) {
            return;
        }
        if (CHRONO_EXHAUSTION.equals(ForgeRegistries.MOB_EFFECTS.getKey(event.getEffectInstance().getEffect()))) {
            return;
        }
        if (!trulybestfriends.isTrackedPet(pet.getUUID())) {
            return;
        }
        if (SuperpositionHandler.hasCurio(pet, EnigmaticItems.VOID_PEARL)) {
            event.setResult(Event.Result.DENY);
        }
    }

    /** "35% chance that otherwise lethal damage won't kill you." */
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!Config.ENABLE_CURIO_PET_COMPAT.get()) {
            return;
        }
        LivingEntity pet = event.getEntity();
        if (pet.level().isClientSide() || pet instanceof Player) {
            return;
        }
        if (!trulybestfriends.isTrackedPet(pet.getUUID())) {
            return;
        }
        if (!SuperpositionHandler.hasCurio(pet, EnigmaticItems.VOID_PEARL)) {
            return;
        }
        if (Math.random() <= VoidPearl.undeadProbability.getValue().asMultiplier(false)) {
            event.setCanceled(true);
            pet.setHealth(1.0F);
        }
    }

    // ----------------------------------------------------- shared hurt pass

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!Config.ENABLE_CURIO_PET_COMPAT.get() || event.getEntity().level().isClientSide()) {
            return;
        }
        applyToDefender(event);
        applyToAttacker(event);
    }

    /** Everything Enigmatic Legacy does when the wearer is the one taking damage. */
    private static void applyToDefender(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim instanceof Player || !trulybestfriends.isTrackedPet(victim.getUUID())) {
            return;
        }
        boolean nebula = SuperpositionHandler.hasCurio(victim, EnigmaticItems.EYE_OF_NEBULA);
        boolean cursed = SuperpositionHandler.hasCurio(victim, EnigmaticItems.CURSED_RING);
        if (!nebula && !cursed) {
            return;
        }

        if (nebula) {
            // "Made of water vapour" — the spellstone is twice as bad at stopping a wet wearer.
            if (victim.isInWater()) {
                event.setAmount(event.getAmount() * 2.0F);
            }
            applySpellstoneResistances(victim, event.getSource(), event);
        }

        if (cursed) {
            // First Curse of Pain.
            event.setAmount(event.getAmount() * CursedRing.painMultiplier.getValue().asModifier());
        }
    }

    /**
     * The generic {@code resistanceList} pass. The Eye of the Nebula fills it with 65% resistance
     * against magic, dragon breath and wither — those three, and only those three, which is why the
     * list is read from the item instead of being hardcoded here.
     */
    private static void applySpellstoneResistances(LivingEntity pet, DamageSource source, LivingHurtEvent event) {
        ItemStack stack = SuperpositionHandler.getCurioStack(pet, EnigmaticItems.EYE_OF_NEBULA);
        if (stack == null || !(stack.getItem() instanceof ItemSpellstoneCurio spellstone)) {
            return;
        }
        ResourceKey<DamageType> key = source.typeHolder().unwrapKey().orElse(null);
        if (key == null) {
            return;
        }
        if (spellstone.resistanceList.containsKey(key)) {
            event.setAmount(event.getAmount() * spellstone.resistanceList.get(key).get());
        }
    }

    /** Everything Enigmatic Legacy does when the wearer is the one dealing damage. */
    private static void applyToAttacker(LivingHurtEvent event) {
        Entity attacker = event.getSource().getEntity();
        if (!(attacker instanceof LivingEntity pet) || pet instanceof Player) {
            return;
        }
        if (!trulybestfriends.isTrackedPet(pet.getUUID())) {
            return;
        }

        // Pearl of the Void: "your attacks inflict Withering".
        if (SuperpositionHandler.hasCurio(pet, EnigmaticItems.VOID_PEARL)
                && event.getSource().getDirectEntity() == pet
                && event.getSource().is(DamageTypes.MOB_ATTACK)) {
            event.getEntity().addEffect(new MobEffectInstance(
                    MobEffects.WITHER,
                    VoidPearl.witheringTime.getValue(),
                    VoidPearl.witheringLevel.getValue(),
                    false,
                    true));
        }

        // Eye of the Nebula: +40% magic damage.
        if (SuperpositionHandler.hasCurio(pet, EnigmaticItems.EYE_OF_NEBULA)
                && SuperpositionHandler.isMagic(event.getSource())) {
            event.setAmount(event.getAmount() * EyeOfNebula.magicBoost.getValue().asModifier(true));
        }

        // Ring of the Seven Curses, Fourth Curse: monsters take half damage from the bearer.
        if (SuperpositionHandler.hasCurio(pet, EnigmaticItems.CURSED_RING)
                && (event.getEntity() instanceof Monster || event.getEntity() instanceof EnderDragon)) {
            event.setAmount(event.getAmount() * CursedRing.monsterDamageDebuff.getValue().asModifierInverted());
        }
    }

    /** Eye of the Nebula: "teleport away from any attack without receiving any damage". */
    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        if (!Config.ENABLE_CURIO_PET_COMPAT.get() || event.isCanceled()) {
            return;
        }
        LivingEntity pet = event.getEntity();
        if (pet.level().isClientSide() || pet instanceof Player) {
            return;
        }
        if (!trulybestfriends.isTrackedPet(pet.getUUID())) {
            return;
        }
        if (!SuperpositionHandler.hasCurio(pet, EnigmaticItems.EYE_OF_NEBULA)) {
            return;
        }
        if (pet.invulnerableTime > 10 || !(event.getSource().getEntity() instanceof LivingEntity)) {
            return;
        }
        if (!EyeOfNebula.dodgeProbability.getValue().roll()) {
            return;
        }
        int counter = 0;
        while (counter <= 32 && !SuperpositionHandler.validTeleportRandomly(
                pet, pet.level(), (int) EyeOfNebula.dodgeRange.getValue())) {
            counter++;
        }
        pet.invulnerableTime = 20;
        event.setCanceled(true);
    }

    // -------------------------------------------------- Ring of the Seven Curses

    /**
     * Fifth Curse: "you take double knockback."
     *
     * <p>The ring's compat surface stops at the damage-side curses — pain, the monster damage
     * penalty, the armor penalty and knockback — because those are the only ones Enigmatic Legacy
     * Plus actually grants a non-player wearer in 1.21.1. Its neutral-creature rage and its eternal
     * burning are still behind {@code instanceof Player} there, so a pet never gets them and this
     * layer does not add them either.</p>
     */
    @SubscribeEvent
    public static void onKnockback(LivingKnockBackEvent event) {
        if (!Config.ENABLE_CURIO_PET_COMPAT.get()) {
            return;
        }
        LivingEntity pet = event.getEntity();
        if (pet.level().isClientSide() || pet instanceof Player) {
            return;
        }
        if (!trulybestfriends.isTrackedPet(pet.getUUID())) {
            return;
        }
        if (SuperpositionHandler.hasCurio(pet, EnigmaticItems.CURSED_RING)) {
            event.setStrength(event.getStrength() * CursedRing.knockbackDebuff.getValue().asModifier());
        }
    }
}
