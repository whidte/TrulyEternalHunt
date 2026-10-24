package com.whidte.trulyeternalhunt.compat;

import auviotre.enigmatic.legacy.contents.item.books.HunterGuidebook;
import auviotre.enigmatic.legacy.contents.item.books.LivingOde;
import auviotre.enigmatic.legacy.contents.item.books.SanguinaryHandbook;
import auviotre.enigmatic.legacy.contents.item.charms.BerserkEmblem;
import auviotre.enigmatic.legacy.contents.item.scrolls.CursedScroll;
import auviotre.enigmatic.legacy.handlers.EnigmaticHandler;
import auviotre.enigmatic.legacy.registries.EnigmaticEffects;
import auviotre.enigmatic.legacy.registries.EnigmaticItems;
import com.whidte.trulyeternalhunt.config.Config;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;

/**
 * Rules that mirror Enigmatic Legacy+'s {@code SanguinaryHandbook.Events}.
 *
 * <p>The original check is reproduced exactly so its contribution can be divided back out,
 * while {@link #qualifies(ServerPlayer, LivingEntity)} implements the new TBF-based rule.</p>
 */
public final class HandbookCompat {
    private HandbookCompat() {
    }

    /**
     * The 1.21.1 original only looked for the handbook in a Curios slot, which never matched
     * because the item is a plain book. We accept both the inventory (matching the tooltip) and
     * a curio slot (in case a data pack makes it equippable).
     */
    public static boolean hasHandbook(LivingEntity owner) {
        return EnigmaticHandler.hasItem(owner, EnigmaticItems.SANGUINARY_HANDBOOK.get())
                || EnigmaticHandler.hasCurio(owner, EnigmaticItems.SANGUINARY_HANDBOOK.get());
    }

    /** Same range calculation as the original: Ode to Living in a curio slot widens it to 24. */
    public static double range(LivingEntity owner) {
        return EnigmaticHandler.hasCurio(owner, EnigmaticItems.ODE_TO_LIVING.get())
                ? LivingOde.effectiveRange.get()
                : HunterGuidebook.effectiveRange.get();
    }

    public static boolean hasBerserkEmblem(LivingEntity owner) {
        return EnigmaticHandler.hasCurio(owner, EnigmaticItems.BERSERK_EMBLEM.get());
    }

    public static boolean ownerSideConditions(Player owner, LivingEntity pet) {
        if (owner.level() != pet.level()) {
            return false;
        }
        if (owner.distanceTo(pet) > range(owner)) {
            return false;
        }
        return !Config.REQUIRE_CURSED_ONE.get() || EnigmaticHandler.isTheCursedOne(owner);
    }

    /** Same magnitude as the original handler: base + Berserk Emblem + Cursed Scroll + Blazing Might. */
    public static double multiplier(LivingEntity owner) {
        double multiplier = SanguinaryHandbook.damageMultiplier.get();
        if (hasBerserkEmblem(owner)) {
            multiplier += 0.5D * (BerserkEmblem.getMissingHealthPool(owner)
                    * BerserkEmblem.attackDamage.get());
        }
        if (EnigmaticHandler.hasCurio(owner, EnigmaticItems.CURSED_SCROLL.get())) {
            multiplier += 0.75D * (CursedScroll.getCurseAmount(owner)
                    * CursedScroll.damageBoost.get() * 0.01D);
        }
        MobEffectInstance blazingMight = owner.getEffect(EnigmaticEffects.BLAZING_MIGHT);
        if (blazingMight != null && blazingMight.getAmplifier() > 2) {
            multiplier += 2.0D * blazingMight.getAmplifier();
        }
        return multiplier;
    }

    /**
     * Exact replication of the original 1.21.1 pet block. Returns the multiplier the original
     * handler applied to this attacker, or {@code 0} when it did not apply.
     */
    public static double originalMultiplier(LivingEntity attacker) {
        if (!(attacker instanceof OwnableEntity ownable)) {
            return 0.0D;
        }
        if (attacker instanceof TamableAnimal tamable && !tamable.isTame()) {
            return 0.0D;
        }
        if (!(ownable.getOwner() instanceof LivingEntity owner)) {
            return 0.0D;
        }
        if (!EnigmaticHandler.hasCurio(owner, EnigmaticItems.SANGUINARY_HANDBOOK.get())) {
            return 0.0D;
        }
        if (owner.level() != attacker.level()) {
            return 0.0D;
        }
        if (owner.distanceTo(attacker) > range(owner)) {
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
