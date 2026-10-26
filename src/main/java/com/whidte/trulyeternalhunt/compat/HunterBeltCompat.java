package com.whidte.trulyeternalhunt.compat;

import com.whidte.trulyeternalhunt.config.Config;
import it.hurts.sskirillss.relics.init.ItemRegistry;
import it.hurts.sskirillss.relics.items.relics.base.IRelicItem;
import it.hurts.sskirillss.relics.utils.EntityUtils;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Rules that mirror Relics' Hunter Belt ({@code relics:hunter_belt}) on 1.20.1.
 *
 * <p>The belt's "Training" ability only recognises tamed animals owned by a player. Both the
 * damage multiplier and the experience gain are reproduced here so the original contribution can
 * be divided back out and replaced by the TBF rule.</p>
 */
public final class HunterBeltCompat {
    private static final String TRAINING = "training";

    private HunterBeltCompat() {
    }

    /** Mirror of the original lookup: the first equipped hunter belt, or an empty stack. */
    public static ItemStack beltOf(Player player) {
        ItemStack stack = EntityUtils.findEquippedCurio(player, ItemRegistry.HUNTER_BELT.get());
        return stack.getItem() instanceof IRelicItem ? stack : ItemStack.EMPTY;
    }

    /** The multiplier the original applies, or {@code 0} when it did not apply. */
    public static double originalFactor(LivingEntity pet) {
        if (!(pet instanceof TamableAnimal tamable)) {
            return 0.0D;
        }
        if (!(tamable.getOwner() instanceof Player owner)) {
            return 0.0D;
        }
        return factorFor(owner);
    }

    /** Same magnitude as the original handler, but for an explicitly resolved owner. */
    public static double factorFor(Player owner) {
        ItemStack belt = beltOf(owner);
        if (belt.isEmpty()) {
            return 0.0D;
        }
        double value = ((IRelicItem) belt.getItem()).getAbilityValue(belt, TRAINING, "damage");
        return value > 0.0D ? value : 0.0D;
    }

    /** The original spreads experience on every hit from a tamed pet of a belt wearer. */
    public static void awardExperience(Player owner) {
        if (!Config.ENABLE_HUNTING_BELT_EXPERIENCE.get()) {
            return;
        }
        ItemStack belt = beltOf(owner);
        if (belt.isEmpty()) {
            return;
        }
        ((IRelicItem) belt.getItem()).spreadExperience(owner, belt, 1);
    }

    /** True when the original already handled this pet, so no second contribution may be added. */
    public static boolean originalCovers(Player owner, LivingEntity pet) {
        return pet instanceof TamableAnimal tamable
                && tamable.getOwner() instanceof Player wearer
                && wearer.getUUID().equals(owner.getUUID());
    }

    /** Same "owned by that player" test the original uses, for additive mode. */
    public static boolean ownedBy(LivingEntity pet, Player owner) {
        return pet instanceof OwnableEntity ownable
                && ownable.getOwner() != null
                && ownable.getOwner().getUUID().equals(owner.getUUID());
    }
}
