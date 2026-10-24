package com.whidte.trulyeternalhunt.compat;

import com.whidte.trulyeternalhunt.config.Config;
import com.whidte.trulybestfriends.network.PetIOUtil;
import com.whidte.trulybestfriends.trulybestfriends;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.OwnableEntity;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves the TBF record of a live entity.
 *
 * <p>TBF stores every collected pet as {@code <world>/trulybestfriends/<ownerUuid>/<petUuid>.nbt}.
 * That file is the authoritative "collected by TBF, owned by this player" marker, and it also
 * works for pets whose entity class does not implement {@link OwnableEntity} (zombies, summons,
 * modded mobs without an owner field...).</p>
 */
public final class TbfPets {
    private record CachedOwner(UUID ownerId, long expiresAt) {
    }

    private record CachedRecord(UUID ownerId, boolean recorded, long expiresAt) {
    }

    private static final Map<UUID, CachedOwner> OWNER_CACHE = new ConcurrentHashMap<>();
    private static final Map<UUID, CachedRecord> RECORD_CACHE = new ConcurrentHashMap<>();

    private TbfPets() {
    }

    /** Cached "is this pet recorded by TBF under that player" check. */
    public static boolean isTbfPetOf(ServerPlayer player, Entity pet) {
        long now = System.currentTimeMillis();
        CachedRecord cached = RECORD_CACHE.get(pet.getUUID());
        if (cached != null && cached.expiresAt > now && cached.ownerId.equals(player.getUUID())) {
            return cached.recorded;
        }
        boolean recorded = checkRecordFile(player, pet);
        RECORD_CACHE.put(pet.getUUID(), new CachedRecord(player.getUUID(), recorded, now + ttlMillis()));
        return recorded;
    }

    private static boolean checkRecordFile(ServerPlayer player, Entity pet) {
        try {
            Path record = PetIOUtil.getOwnerDir(player).resolve(pet.getUUID() + ".nbt");
            return Files.isRegularFile(record);
        } catch (RuntimeException exception) {
            return false;
        }
    }

    /**
     * Finds the online player that owns this pet.
     *
     * <p>Vanilla / modded {@link OwnableEntity} pets are resolved directly. For TBF pets that do
     * not expose their owner on the entity, the owner is recovered from the pet record directory
     * and cached for a few seconds.</p>
     */
    @Nullable
    public static ServerPlayer findOwner(ServerLevel level, Entity pet) {
        if (pet instanceof OwnableEntity ownable) {
            UUID ownerId = ownable.getOwnerUUID();
            if (ownerId != null) {
                ServerPlayer online = level.getServer().getPlayerList().getPlayer(ownerId);
                if (online != null) {
                    return online;
                }
            }
        }
        if (!trulybestfriends.isTrackedPet(pet.getUUID())) {
            return null;
        }
        long now = System.currentTimeMillis();
        CachedOwner cached = OWNER_CACHE.get(pet.getUUID());
        if (cached != null && cached.expiresAt > now) {
            return level.getServer().getPlayerList().getPlayer(cached.ownerId);
        }
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            if (checkRecordFile(player, pet)) {
                long expiresAt = now + ttlMillis();
                OWNER_CACHE.put(pet.getUUID(), new CachedOwner(player.getUUID(), expiresAt));
                RECORD_CACHE.put(pet.getUUID(), new CachedRecord(player.getUUID(), true, expiresAt));
                return player;
            }
        }
        OWNER_CACHE.remove(pet.getUUID());
        return null;
    }

    /** Drops all cached records (used when the server stops). */
    public static void clearCaches() {
        OWNER_CACHE.clear();
        RECORD_CACHE.clear();
    }

    private static long ttlMillis() {
        return Math.max(1, Config.OWNER_CACHE_SECONDS.get()) * 1000L;
    }
}
