package com.whidte.trulyeternalhunt.event;

import com.whidte.trulybestfriends.api.PetStoredDeathEvent;
import com.whidte.trulybestfriends.api.TrulyBestFriendsApi;
import com.whidte.trulyeternalhunt.TrulyEternalHunt;
import com.whidte.trulyeternalhunt.compat.HuntingBeltCompat;
import com.whidte.trulyeternalhunt.compat.TbfPets;
import com.whidte.trulyeternalhunt.config.Config;
import it.hurts.sskirillss.relics.init.RelicsDataComponents;
import it.hurts.sskirillss.relics.init.RelicsItems;
import it.hurts.sskirillss.relics.items.PetBoneItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.List;
import java.util.UUID;

/**
 * Turns Relics' pet bone into the revive path for TBF pets.
 *
 * <p>TBF intercepts fatal damage and stores the pet instead of letting it die, so
 * {@code LivingDeathEvent} never fires for it and Relics' own bone drop can never happen. This
 * class listens to TBF's stored-death event, drops the bone, locks TBF's own revive while the bone
 * exists, and makes the bone revive the pet through TBF (keeping its UUID and tracking) instead of
 * spawning a plain entity copy. Charging is extended to kills made by the owner's TBF pets.</p>
 *
 * <p>Registered manually and only when Relics is loaded.</p>
 */
public final class PetBoneEvents {
    private PetBoneEvents() {
    }

    private static boolean compatEnabled() {
        return Config.ENABLED.get() && Config.ENABLE_PET_BONE_COMPAT.get();
    }

    private static boolean boneDropEnabled() {
        return compatEnabled() && Config.ENABLE_HUNTING_BELT_REVIVAL.get();
    }

    /** TBF-stored deaths never reach {@code LivingDeathEvent}, so the bone is dropped here. */
    @SubscribeEvent
    public static void onStoredDeath(PetStoredDeathEvent event) {
        if (!boneDropEnabled()) {
            return;
        }
        ServerPlayer owner = event.getOwner();
        LivingEntity pet = event.getPet();
        if (owner == null || pet == null) {
            return;
        }
        if (HuntingBeltCompat.isRevivalBlacklisted(pet)) {
            return;
        }
        List<ItemStack> belts = HuntingBeltCompat.revivalBelts(owner);
        if (belts.isEmpty()) {
            return;
        }
        // Lock first: "a bone exists" must always imply "TBF's own revive is refused".
        if (!TrulyBestFriendsApi.setReviveLocked(event.getLevel(), event.getOwnerUuid(), event.getPetUuid(), true)) {
            TrulyEternalHunt.LOGGER.warn(
                    "[Truly Eternal Hunt] could not lock TBF revive for pet {}; no pet bone dropped",
                    event.getPetUuid());
            return;
        }
        pet.spawnAtLocation(HuntingBeltCompat.createPetBone(pet, owner, belts, event.getPetUuid()));
        HuntingBeltCompat.awardBoneDrop(owner, belts);
        if (Config.DEBUG.get()) {
            TrulyEternalHunt.LOGGER.info("[Truly Eternal Hunt] stored-death bone dropped for pet {} of {}",
                    event.getPetUuid(), owner.getGameProfile().getName());
        }
    }

    /**
     * Kills made by a player's TBF pets charge that player's bones, just like the player's own kills
     * do in Relics. Player kills are left to Relics so a bone is never charged twice.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void chargeBonesFromPetKills(LivingDeathEvent event) {
        if (!compatEnabled()) {
            return;
        }
        LivingEntity victim = event.getEntity();
        if (victim instanceof Player) {
            return;
        }
        if (!(victim.level() instanceof ServerLevel level)) {
            return;
        }
        Entity killer = event.getSource().getEntity();
        if (killer instanceof Player) {
            return;
        }
        if (!(killer instanceof LivingEntity pet)) {
            return;
        }
        ServerPlayer owner = TbfPets.findOwner(level, pet);
        if (owner == null || !TbfPets.isTbfPetOf(owner, pet)) {
            return;
        }
        HuntingBeltCompat.chargeBones(owner, victim.getMaxHealth());
        if (Config.DEBUG.get()) {
            TrulyEternalHunt.LOGGER.info("[Truly Eternal Hunt] pet {} charged the bones of {} (+{} hp)",
                    pet.getUUID(), owner.getGameProfile().getName(), victim.getMaxHealth());
        }
    }

    /**
     * A finished bone for a TBF pet revives through TBF instead of spawning an entity copy.
     *
     * <p>Bones Relics created itself carry no pet UUID (the pet died normally and TBF cannot revive
     * it anyway), so those keep Relics' original behaviour.</p>
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void useBone(PlayerInteractEvent.RightClickItem event) {
        if (!compatEnabled()) {
            return;
        }
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty() || stack.getItem() != RelicsItems.PET_BONE.get()) {
            return;
        }
        PetBoneItem.PetBoneData data = stack.getOrDefault(
                RelicsDataComponents.PET_BONE_DATA.get(), PetBoneItem.PetBoneData.EMPTY);
        if (!data.isValid() || data.collectedHealth() < data.requiredHealth()) {
            return;
        }
        String rawPetUuid = data.entityData().getString(HuntingBeltCompat.PET_UUID_KEY);
        if (rawPetUuid.isEmpty()) {
            return;
        }
        UUID petUuid;
        try {
            petUuid = UUID.fromString(rawPetUuid);
        } catch (IllegalArgumentException exception) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!TrulyBestFriendsApi.revivePet(player, petUuid, true)) {
            player.sendSystemMessage(Component.translatable("trulyeternalhunt.pet_bone.revive_failed")
                    .withStyle(ChatFormatting.RED));
            return;
        }
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        player.swing(event.getHand(), true);
    }
}
