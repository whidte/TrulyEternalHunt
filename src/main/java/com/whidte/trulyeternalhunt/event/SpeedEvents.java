package com.whidte.trulyeternalhunt.event;

import auviotre.enigmatic.legacy.contents.item.books.SanguinaryHandbook;
import com.google.common.collect.Multimap;
import com.whidte.trulyeternalhunt.compat.HandbookCompat;
import com.whidte.trulyeternalhunt.compat.TbfPets;
import com.whidte.trulyeternalhunt.config.Config;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Reroutes the handbook's pet attack speed / movement speed modifiers (Berserk Emblem synergy).
 *
 * <p>Runs after the original handler: TBF pets get the modifiers, every other owned pet gets
 * them removed, so the new qualification also covers the speed half of the item.</p>
 *
 * <p>Registered from the mod constructor only when Enigmatic Legacy+ is present.</p>
 */
public final class SpeedEvents {
    private SpeedEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onEntityTick(EntityTickEvent.Pre event) {
        if (!Config.ENABLED.get() || !Config.ENABLE_SPEED_EFFECT.get()) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity living)) {
            return;
        }
        if (living.level().isClientSide() || !(living.level() instanceof ServerLevel level)) {
            return;
        }
        ServerPlayer owner = TbfPets.findOwner(level, living);
        if (owner == null) {
            return;
        }
        boolean active = HandbookCompat.qualifies(owner, living) && HandbookCompat.hasBerserkEmblem(owner);
        Multimap<Holder<Attribute>, AttributeModifier> modifiers = SanguinaryHandbook.createAttributeMap(owner);
        AttributeMap attributes = living.getAttributes();
        if (active) {
            attributes.addTransientAttributeModifiers(modifiers);
        } else {
            attributes.removeAttributeModifiers(modifiers);
        }
    }
}
