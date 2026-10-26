package com.whidte.trulyeternalhunt.event;

import auviotre.enigmatic.addon.contents.items.SanguinaryHandbook;
import com.google.common.collect.Multimap;
import com.whidte.trulyeternalhunt.compat.HandbookCompat;
import com.whidte.trulyeternalhunt.compat.TbfPets;
import com.whidte.trulyeternalhunt.config.Config;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Reroutes the handbook's pet speed modifier (Berserk Emblem synergy).
 *
 * <p>Runs after the original addon's tick handler: TBF pets get the modifier, every other
 * owned pet gets it removed, so the new qualification also covers the speed half of the item.</p>
 *
 * <p>Registered from the mod constructor only when Enigmatic Addons is present.</p>
 */
public final class SpeedEvents {
    private SpeedEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!Config.ENABLED.get() || !Config.ENABLE_SPEED_EFFECT.get()) {
            return;
        }
        LivingEntity living = event.getEntity();
        if (living.level().isClientSide() || !(living.level() instanceof ServerLevel level)) {
            return;
        }
        ServerPlayer owner = TbfPets.findOwner(level, living);
        if (owner == null) {
            return;
        }
        boolean active = HandbookCompat.qualifies(owner, living) && HandbookCompat.hasBerserkEmblem(owner);
        Multimap<Attribute, AttributeModifier> modifiers = SanguinaryHandbook.createAttributeMap(owner);
        AttributeMap attributes = living.getAttributes();
        if (active) {
            attributes.addTransientAttributeModifiers(modifiers);
        } else {
            attributes.removeAttributeModifiers(modifiers);
        }
    }
}
