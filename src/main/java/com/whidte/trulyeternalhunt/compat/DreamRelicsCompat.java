package com.whidte.trulyeternalhunt.compat;

import com.bmt.dream_relics.config.CommonConfig;
import com.bmt.dream_relics.init.DRItems;
import com.bmt.dream_relics.util.SleepStateManager;
import com.whidte.trulybestfriends.trulybestfriends;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;

import java.util.List;
import java.util.Optional;

/**
 * Rules that mirror Dream Relics (1.20.1, {@code dream_relics} 1.0.3).
 *
 * <p>Dream Relics keeps almost every trinket behind {@code instanceof Player} — either directly in
 * {@code EventHandler.ForgeEventHandler} or inside a {@code LivingEntity} mixin whose helper starts
 * with {@code if (entity instanceof Player player)}. The bodies below are the originals with the
 * wearer widened from a player to any TBF-recorded pet; every number still comes from Dream Relics'
 * own config, so in-game tuning keeps working.</p>
 *
 * <p>The Royal Crown is the one item whose <em>target</em> is narrowed instead of widened: the
 * original buffs the wearer's tamed animals ({@code TamableAnimal} whose owner wears the crown), and
 * here it buffs the creatures TBF has recorded under the wearer.</p>
 */
public final class DreamRelicsCompat {
    /** Same NBT key Dream Relics' {@code MemoryNecklaceItem} writes. */
    private static final String MEMORY_STORED_DAMAGE = "StoredDamage";

    private DreamRelicsCompat() {
    }

    // ------------------------------------------------------------------ curios

    private static Optional<ICuriosItemHandler> inventory(LivingEntity wearer) {
        return CuriosApi.getCuriosInventory(wearer).resolve();
    }

    public static boolean hasCurio(LivingEntity wearer, Item item) {
        return inventory(wearer).map(handler -> handler.isEquipped(item)).orElse(false);
    }

    public static ItemStack curioStack(LivingEntity wearer, Item item) {
        return inventory(wearer)
                .flatMap(handler -> handler.findFirstCurio(item))
                .map(SlotResult::stack)
                .orElse(ItemStack.EMPTY);
    }

    // ------------------------------------------------------------- royal crown

    public static boolean wearsCrown(Player player) {
        return hasCurio(player, DRItems.ROYAL_CROWN.get());
    }

    /** Multiplier the crown applies to damage a pet <em>takes</em> (config default {@code 0.6}). */
    public static double crownReduction() {
        return CommonConfig.royalCrownPetDamageReduction;
    }

    /** Multiplier the crown applies to damage a pet <em>deals</em> (config default {@code 1.2}). */
    public static double crownBoost() {
        return CommonConfig.royalCrownPetDamageBoost;
    }

    /**
     * Exact replication of the crown's original condition for one side of a hit.
     *
     * @return the multiplier the original handler applies for that side, or {@code 1} when it does not apply
     */
    public static double originalCrownFactor(@Nullable Entity attacker, LivingEntity victim) {
        double factor = 1.0D;
        if (originalCovers(victim)) {
            factor *= crownReduction();
        }
        if (attacker != null && originalCovers(attacker)) {
            factor *= crownBoost();
        }
        return factor;
    }

    /** True when the original tamed-animal rule already covers this entity. */
    public static boolean originalCovers(Entity entity) {
        return entity instanceof TamableAnimal tamable
                && tamable.getOwner() instanceof Player owner
                && wearsCrown(owner);
    }

    /** New rule: the creature is recorded by TBF under the player who currently wears the crown. */
    public static boolean crownApplies(ServerPlayer owner, LivingEntity pet) {
        return TbfPets.isTbfPetOf(owner, pet) && wearsCrown(owner);
    }

    // ------------------------------------------------- astral necklace (armour)

    /**
     * Armour-penetration ratio of the Astral Necklace, as a multiplier on pre-armour damage.
     *
     * <p>Dream Relics rewrites {@code LivingEntity#getDamageAfterArmorAbsorb} for players wearing the
     * necklace: 30% of the target's armour plus a flat 10 points are ignored, then 4% damage reduction
     * per remaining armour point. Vanilla rolls armour and toughness into a different curve, so the
     * effect is expressed as the ratio between the two results — the later protection-enchantment pass
     * is linear in damage, so the ratio survives it unchanged. In practice the ratio is above 1, since
     * the necklace's curve is the shallower of the two.</p>
     */
    public static float astralArmorRatio(LivingEntity defender, float damage) {
        float armor = defender.getArmorValue();
        if (armor <= 0.0F || damage <= 0.0F) {
            return 1.0F;
        }
        float toughness = (float) defender.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
        float vanilla = CombatRules.getDamageAfterAbsorb(damage, armor, toughness);
        float remaining = Math.max(0.0F, armor - (armor * 0.3F + 10.0F));
        float pierced = Math.max(0.0F, damage - remaining * 0.04F * damage);
        return vanilla > 0.0F ? pierced / vanilla : 1.0F;
    }

    // ------------------------------------------------------ tassel ring (damage)

    /** Tassel Ring: the lower the target's health, the smaller the bonus; 1.0 at 0% health. */
    public static float tasselMultiplier(LivingEntity target) {
        float maxHealth = target.getMaxHealth();
        if (maxHealth <= 0.0F) {
            return 1.0F;
        }
        float healthPercentage = Math.min(target.getHealth(), maxHealth) / maxHealth * 100.0F;
        return 1.0F + healthPercentage / 100.0F * (float) CommonConfig.tasselRingDamageMultiplierMax;
    }

    // ---------------------------------------------- heart voice pendant (cap)

    /** Heart Voice Pendant: no single hit may take more than a configurable share of max health. */
    public static float pendantDamageCap(LivingEntity wearer) {
        return wearer.getMaxHealth() * (float) CommonConfig.heartVoicePendantMaxDamagePercent;
    }

    // ------------------------------------------------ memory necklace (storage)

    /** Records damage the wearer just took, capped by the item's own config value. */
    public static void storeDamage(LivingEntity wearer, float damage) {
        ItemStack necklace = curioStack(wearer, DRItems.MEMORY_NECKLACE.get());
        if (necklace.isEmpty() || damage <= 0.0F) {
            return;
        }
        CompoundTag tag = necklace.getOrCreateTag();
        float stored = tag.getFloat(MEMORY_STORED_DAMAGE);
        tag.putFloat(MEMORY_STORED_DAMAGE,
                Math.min(stored + damage, (float) CommonConfig.memoryNecklaceMaxStoredDamage));
    }

    /** Releases the stored damage into the wearer's next hit and clears it. */
    public static float releaseStoredDamage(LivingEntity wearer, float damage) {
        ItemStack necklace = curioStack(wearer, DRItems.MEMORY_NECKLACE.get());
        if (necklace.isEmpty()) {
            return damage;
        }
        CompoundTag tag = necklace.getOrCreateTag();
        float stored = tag.getFloat(MEMORY_STORED_DAMAGE);
        if (stored <= 0.0F) {
            return damage;
        }
        tag.putFloat(MEMORY_STORED_DAMAGE, 0.0F);
        return damage + stored;
    }

    // ---------------------------------------------------- echo earring (sonic wave)

    /**
     * Echo Earring: a chance to hit everything along the wearer's line of sight for extra damage.
     *
     * <p>Dream Relics' own intersection test always reports a hit (its fallback result is a block hit,
     * not a miss), so the effective behaviour is "every living entity in the ray's bounding box". That
     * is what is reproduced here. The original's sound and particle pass is cosmetic and omitted.</p>
     */
    public static void tryEchoWave(LivingEntity wearer, float baseDamage) {
        if (wearer.getRandom().nextDouble() >= CommonConfig.echoEarringTriggerChance) {
            return;
        }
        if (!(wearer.level() instanceof ServerLevel level)) {
            return;
        }
        double range = CommonConfig.echoEarringRange;
        Vec3 start = wearer.getEyePosition();
        Vec3 end = start.add(wearer.getViewVector(1.0F).scale(range));
        AABB area = wearer.getBoundingBox().expandTowards(end.subtract(start));
        List<LivingEntity> victims = level.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != wearer && entity.isAlive());
        float extraDamage = baseDamage * (float) CommonConfig.echoEarringExtraDamageMultiplier;
        for (LivingEntity victim : victims) {
            victim.hurt(wearer.damageSources().mobAttack(wearer), extraDamage);
        }
    }

    // -------------------------------------------------- endless dream (sleep)

    /** Endless Dream: a chance to put the struck mob to sleep, players excluded as before. */
    public static void tryPutToSleep(LivingEntity wearer, LivingEntity target) {
        if (target instanceof Player) {
            return;
        }
        if (wearer.getRandom().nextDouble() < CommonConfig.endlessDreamTriggerChance) {
            SleepStateManager.setSleeping(target, CommonConfig.endlessDreamSleepDuration);
        }
    }

    // ------------------------------------------------------------- shared gates

    /** True for a non-player entity that TBF has collected. */
    public static boolean isTrackedPet(LivingEntity entity) {
        return !(entity instanceof Player) && trulybestfriends.isTrackedPet(entity.getUUID());
    }
}
