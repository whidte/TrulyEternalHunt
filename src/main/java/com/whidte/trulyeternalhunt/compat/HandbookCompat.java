package com.whidte.trulyeternalhunt.compat;

import auviotre.enigmatic.addon.contents.items.BlessRing;
import auviotre.enigmatic.addon.contents.items.SanguinaryHandbook;
import auviotre.enigmatic.addon.registries.EnigmaticAddonItems;
import com.aizistral.enigmaticlegacy.handlers.SuperpositionHandler;
import com.aizistral.enigmaticlegacy.items.BerserkEmblem;
import com.aizistral.enigmaticlegacy.items.CursedScroll;
import com.aizistral.enigmaticlegacy.items.HunterGuidebook;
import com.aizistral.enigmaticlegacy.registries.EnigmaticItems;
import com.whidte.trulyeternalhunt.config.Config;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;

/**
 * Rules that mirror Enigmatic Addons' {@code AddonEventHandler} handbook block.
 *
 * <p>The original check is reproduced exactly so its contribution can be divided back out,
 * while {@link #qualifies(ServerPlayer, LivingEntity)} implements the new TBF-based rule.</p>
 */
public final class HandbookCompat {
    private HandbookCompat() {
    }

    /** The original 1.20.1 addon reads the handbook from the player's inventory. */
    public static boolean hasHandbook(Player owner) {
        return SuperpositionHandler.hasItem(owner, EnigmaticAddonItems.SANGUINARY_HANDBOOK);
    }

    public static boolean ownerSideConditions(Player owner, LivingEntity pet) {
        if (owner.level() != pet.level()) {
            return false;
        }
        if (owner.distanceTo(pet) > HunterGuidebook.effectiveDistance.getValue()) {
            return false;
        }
        return !Config.REQUIRE_CURSED_ONE.get()
                || SuperpositionHandler.isTheCursedOne(owner)
                || BlessRing.Helper.betrayalAvailable(owner);
    }

    public static boolean hasBerserkEmblem(Player owner) {
        return SuperpositionHandler.hasCurio(owner, EnigmaticItems.BERSERK_CHARM);
    }

    /** Same magnitude as the original handler: base + Berserk Emblem + Scroll of a Thousand Curses. */
    public static double multiplier(Player owner) {
        double multiplier = SanguinaryHandbook.DamageMultiplier.getValue();
        if (hasBerserkEmblem(owner)) {
            multiplier += 0.5D * (SuperpositionHandler.getMissingHealthPool(owner)
                    * BerserkEmblem.attackDamage.getValue());
        }
        if (SuperpositionHandler.hasCurio(owner, EnigmaticItems.CURSED_SCROLL)) {
            multiplier += 0.75D * (SuperpositionHandler.getCurseAmount(owner)
                    * CursedScroll.damageBoost.getValue().asModifier());
        }
        return multiplier;
    }

    /**
     * Exact replication of the original addon's pet block (1.20.1). Returns the multiplier the
     * original handler applied to this attacker, or {@code 0} when it did not apply.
     */
    public static double originalMultiplier(LivingEntity attacker) {
        if (!(attacker instanceof OwnableEntity ownable)) {
            return 0.0D;
        }
        if (attacker instanceof TamableAnimal tamable && !tamable.isTame()) {
            return 0.0D;
        }
        if (!(ownable.getOwner() instanceof Player owner)) {
            return 0.0D;
        }
        if (!ownerSideConditions(owner, attacker) || !hasHandbook(owner)) {
            return 0.0D;
        }
        return multiplier(owner);
    }

    /** New rule: the pet must be recorded by TBF under this player, and the player side conditions still apply. */
    public static boolean qualifies(ServerPlayer owner, LivingEntity pet) {
        return TbfPets.isTbfPetOf(owner, pet)
                && ownerSideConditions(owner, pet)
                && hasHandbook(owner);
    }
}
