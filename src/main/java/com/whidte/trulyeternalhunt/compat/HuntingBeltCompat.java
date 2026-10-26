package com.whidte.trulyeternalhunt.compat;

import it.hurts.sskirillss.relics.Relics;
import it.hurts.sskirillss.relics.api.relics.IRelicItem;
import it.hurts.sskirillss.relics.api.relics.data.AbilityData;
import it.hurts.sskirillss.relics.api.relics.data.RelicData;
import it.hurts.sskirillss.relics.init.RelicsConfigs;
import it.hurts.sskirillss.relics.init.RelicsDataComponents;
import it.hurts.sskirillss.relics.init.RelicsItems;
import it.hurts.sskirillss.relics.items.PetBoneItem;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import it.hurts.sskirillss.relics.utils.TargetingUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Rules that mirror Relics' Hunting Belt ({@code relics:hunting_belt}).
 *
 * <p>The belt's "Pack" ability resolves "my pet" through vanilla {@link OwnableEntity} ownership
 * in four places: the pet damage bonus, the Leader damage resistance, the pet damage experience
 * and the Revival pet bone drop. Every rule below reproduces the original computation exactly so
 * the original contribution can be divided back out, while the {@code pet} predicates supplied by
 * the caller decide which entities count as pets.</p>
 */
public final class HuntingBeltCompat {
    /** Persistent-data flag the original sets so its {@code LivingDropsEvent} handler can eat the drops. */
    public static final String BONE_DROP_TAG = Relics.MODID + ":hunting_belt_bone_drop";

    /** Key injected into the bone's entity data so a bone revive can be routed back to TBF. */
    public static final String PET_UUID_KEY = "TrulyEternalHunt:PetUuid";

    private static final String PACK = "pack";
    private static final String LEADER = "leader";
    private static final String RELENTLESS = "relentless";
    private static final String REVIVAL = "revival";

    private HuntingBeltCompat() {
    }

    private static RelicData data(LivingEntity owner, ItemStack stack) {
        return ((IRelicItem) stack.getItem()).getRelicData(owner, stack);
    }

    private static AbilityData pack(LivingEntity owner, ItemStack stack) {
        return data(owner, stack).getAbilitiesData().getAbilityData(PACK);
    }

    /** Same filter the original uses: an equipped, usable Pack ability belt. */
    public static List<ItemStack> usableBelts(LivingEntity owner) {
        return EntityUtils.findEquippedCurios(owner, RelicsItems.HUNTING_BELT.get(),
                stack -> pack(owner, stack).canPlayerUse(owner));
    }

    /** The vanilla pet predicate: an owned entity whose owner is that player. */
    public static Predicate<LivingEntity> vanillaPet(Player owner) {
        return candidate -> candidate instanceof OwnableEntity ownable
                && ownable.getOwner() != null
                && ownable.getOwner().getUUID().equals(owner.getUUID());
    }

    /**
     * Exact replication of the original Pack damage block for an attacker that is an
     * {@link OwnableEntity}: {@code 1 + sum(damage_modifier)} over every usable belt that may harm
     * the target, or {@code 0} when the original applied nothing.
     */
    public static double originalDamageFactor(LivingEntity pet, LivingEntity target) {
        if (!(pet instanceof OwnableEntity ownable)) {
            return 0.0D;
        }
        if (!(ownable.getOwner() instanceof LivingEntity owner)) {
            return 0.0D;
        }
        return damageFactor(owner, target);
    }

    /** Same magnitude as the original, but with an explicit owner instead of the entity's owner field. */
    public static double damageFactor(LivingEntity owner, LivingEntity target) {
        double total = 0.0D;
        for (ItemStack stack : usableBelts(owner)) {
            if (!TargetingUtils.canHarm(owner, target, stack, PACK)) {
                continue;
            }
            total += pack(owner, stack).getStatData("damage_modifier").getValue();
        }
        return total > 0.0D ? 1.0D + total : 0.0D;
    }

    /** The Relentless rank modifier zeroes the target's invulnerability window. */
    public static boolean ignoresInvulnerability(LivingEntity owner, LivingEntity target) {
        for (ItemStack stack : usableBelts(owner)) {
            if (!TargetingUtils.canHarm(owner, target, stack, PACK)) {
                continue;
            }
            if (pack(owner, stack).getRankModifierData(RELENTLESS).isEnabled()) {
                return true;
            }
        }
        return false;
    }

    /**
     * The damage-reduction <em>factor</em> the Leader rank produces for this player.
     *
     * <p>The original formula only depends on the pet count inside each belt's radius, so the
     * incoming damage amount cancels out and the whole block can be expressed as a factor:
     * {@code 1 - appliedResisted / original}.</p>
     */
    public static double resistanceFactor(Player player, Predicate<LivingEntity> isPet) {
        List<ItemStack> leaderBelts = new ArrayList<>();
        for (ItemStack stack : usableBelts(player)) {
            if (pack(player, stack).getRankModifierData(LEADER).isEnabled()) {
                leaderBelts.add(stack);
            }
        }
        if (leaderBelts.isEmpty()) {
            return 1.0D;
        }
        double maxRadius = 0.0D;
        for (ItemStack stack : leaderBelts) {
            maxRadius = Math.max(maxRadius, pack(player, stack).getStatData("pet_radius").getValue());
        }
        List<LivingEntity> nearbyPets = player.level().getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(maxRadius), isPet::test);
        if (nearbyPets.isEmpty()) {
            return 1.0D;
        }
        double totalReduction = 0.0D;
        for (ItemStack stack : leaderBelts) {
            AbilityData ability = pack(player, stack);
            double radius = ability.getStatData("pet_radius").getValue();
            double perPet = ability.getStatData("resistance_per_pet").getValue();
            long inRange = nearbyPets.stream()
                    .filter(pet -> pet.distanceToSqr(player) <= radius * radius)
                    .count();
            double reduction = Math.min(inRange, 5L) * perPet;
            totalReduction += Math.max(0.0D, Math.min(reduction, 0.9D));
        }
        if (totalReduction <= 0.0D) {
            return 1.0D;
        }
        // Every belt caps out at 90% together, exactly like the original.
        double scale = totalReduction > 0.9D ? 0.9D / totalReduction : 1.0D;
        return 1.0D - totalReduction * scale;
    }

    /** The pet-damage experience / statistics the original awards for one hit. */
    public static void awardPetDamage(LivingEntity owner, LivingEntity target, float damage) {
        for (ItemStack stack : usableBelts(owner)) {
            if (!TargetingUtils.canHarm(owner, target, stack, PACK)) {
                continue;
            }
            RelicData data = data(owner, stack);
            AbilityData ability = data.getAbilitiesData().getAbilityData(PACK);
            data.getLevelingData().addExperience(PACK, "pet_damage", 1);
            ability.getStatisticData().getMetricData("pet_attacks").addValue(1);
            ability.getStatisticData().getMetricData("pet_damage").addValue(damage);
        }
    }

    /** Belts of that owner that have the Revival rank modifier unlocked. */
    public static List<ItemStack> revivalBelts(LivingEntity owner) {
        List<ItemStack> belts = new ArrayList<>();
        for (ItemStack stack : usableBelts(owner)) {
            if (pack(owner, stack).getRankModifierData(REVIVAL).isEnabled()) {
                belts.add(stack);
            }
        }
        return belts;
    }

    /** Same blacklist the original consults before dropping a pet bone. */
    public static boolean isRevivalBlacklisted(LivingEntity pet) {
        return RelicsConfigs.RELICS_CONFIG.getHuntingBeltRevivalEntitiesBlacklist()
                .contains(BuiltInRegistries.ENTITY_TYPE.getKey(pet.getType()).toString());
    }

    /**
     * Drops the pet bone the original would have dropped, including the persistent-data flag that
     * lets the original's own {@code LivingDropsEvent} handler replace the normal loot.
     */
    public static void dropPetBone(LivingEntity pet, LivingEntity owner, List<ItemStack> belts) {
        pet.spawnAtLocation(createPetBone(pet, owner, belts, null));
        pet.getPersistentData().putBoolean(BONE_DROP_TAG, true);
        awardBoneDrop(owner, belts);
    }

    /** Builds the bone with the same numbers Relics uses, optionally remembering the pet's UUID. */
    public static ItemStack createPetBone(LivingEntity pet, LivingEntity owner, List<ItemStack> belts,
                                          @Nullable UUID petUuid) {
        double requiredHealth = Double.MAX_VALUE;
        for (ItemStack stack : belts) {
            requiredHealth = Math.min(requiredHealth, reviveHealthRequirement(owner, stack, pet));
        }
        PetBoneItem.PetBoneData data = PetBoneItem.PetBoneData.fromEntity(pet, owner.getUUID(), requiredHealth);
        if (petUuid != null) {
            data.entityData().putString(PET_UUID_KEY, petUuid.toString());
        }
        ItemStack bone = new ItemStack(RelicsItems.PET_BONE.get());
        bone.set(RelicsDataComponents.PET_BONE_DATA.get(), data);
        return bone;
    }

    /** The experience and statistics the original awards when it drops a bone. */
    public static void awardBoneDrop(LivingEntity owner, List<ItemStack> belts) {
        for (ItemStack stack : belts) {
            RelicData data = data(owner, stack);
            data.getLevelingData().addExperience(PACK, "bone_drop", 1);
            data.getAbilitiesData().getAbilityData(PACK).getStatisticData()
                    .getMetricData("pet_bones").addValue(1);
        }
    }

    /**
     * Charges every pet bone in the player's inventory, exactly like the original does when the
     * player kills a mob. Used to extend charging to kills made by the player's TBF pets.
     */
    public static void chargeBones(Player owner, double amount) {
        if (amount <= 0.0D) {
            return;
        }
        for (ItemStack stack : EntityUtils.findItemsInInventory(owner, RelicsItems.PET_BONE.get())) {
            PetBoneItem.PetBoneData data = stack.getOrDefault(
                    RelicsDataComponents.PET_BONE_DATA.get(), PetBoneItem.PetBoneData.EMPTY);
            if (!data.isValid() || !owner.getUUID().equals(data.owner())
                    || data.collectedHealth() >= data.requiredHealth()) {
                continue;
            }
            double updated = Math.min(data.requiredHealth(), data.collectedHealth() + amount);
            stack.set(RelicsDataComponents.PET_BONE_DATA.get(), data.withCollectedHealth(updated));
        }
    }

    private static double reviveHealthRequirement(LivingEntity owner, ItemStack stack, LivingEntity pet) {
        AttributeInstance attribute = pet.getAttribute(Attributes.MAX_HEALTH);
        double baseHealth = attribute != null ? attribute.getBaseValue() : pet.getMaxHealth();
        double modifier = pack(owner, stack).getStatData("revival_cost").getValue();
        return Math.max(1.0D, baseHealth * modifier);
    }

    /** True when the original already handled this pet, so we must not add a second contribution. */
    public static boolean originalCovers(@Nullable LivingEntity owner, LivingEntity pet) {
        return owner != null
                && pet instanceof OwnableEntity ownable
                && ownable.getOwner() != null
                && ownable.getOwner().getUUID().equals(owner.getUUID());
    }
}
