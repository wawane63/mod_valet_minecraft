package com.wawane.valet.ai.core;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public final class ValetEntityReservations {
    private static final ConcurrentMap<Key, Reservation> RESERVATIONS = new ConcurrentHashMap<>();

    private ValetEntityReservations() {
    }

    public static boolean claim(ServerLevel world, UUID owner, Entity entity, int ttlTicks) {
        long now = world.getGameTime();
        long expiresAt = now + Math.max(1, ttlTicks);
        Key key = new Key(world.dimension(), entity.getUUID());
        Reservation reservation = RESERVATIONS.compute(key, (ignored, current) -> {
            if (current == null || current.expiresAt <= now || current.owner.equals(owner)) {
                return new Reservation(owner, expiresAt);
            }
            return current;
        });
        return reservation != null && reservation.owner.equals(owner);
    }

    public static boolean isClaimedByOther(ServerLevel world, UUID owner, Entity entity) {
        long now = world.getGameTime();
        Key key = new Key(world.dimension(), entity.getUUID());
        Reservation reservation = RESERVATIONS.get(key);
        if (reservation != null && reservation.expiresAt <= now) {
            RESERVATIONS.remove(key, reservation);
            return false;
        }
        return reservation != null && reservation.expiresAt > now && !reservation.owner.equals(owner);
    }

    public static void release(UUID owner, UUID entityUuid) {
        RESERVATIONS.entrySet().removeIf(entry ->
                entry.getValue().owner.equals(owner) && entry.getKey().entityUuid.equals(entityUuid));
    }

    public static void release(UUID owner, Entity entity) {
        release(owner, entity.getUUID());
    }

    public static void releaseAll(UUID owner) {
        RESERVATIONS.entrySet().removeIf(entry -> entry.getValue().owner.equals(owner));
    }

    public static void clearAll() {
        RESERVATIONS.clear();
    }

    private record Key(ResourceKey<Level> dimension, UUID entityUuid) {
    }

    private record Reservation(UUID owner, long expiresAt) {
    }
}
