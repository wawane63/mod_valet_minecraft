package com.wawane.valet.ai.core;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public final class ValetBlockReservations {
    private static final ConcurrentMap<Key, Reservation> RESERVATIONS = new ConcurrentHashMap<>();

    private ValetBlockReservations() {
    }

    public static boolean claim(ServerLevel world, UUID owner, BlockPos pos, int ttlTicks) {
        long now = world.getGameTime();
        long expiresAt = now + Math.max(1, ttlTicks);
        Key key = new Key(world.dimension(), pos.immutable());
        Reservation reservation = RESERVATIONS.compute(key, (ignored, current) -> {
            if (current == null || current.expiresAt <= now || current.owner.equals(owner)) {
                return new Reservation(owner, expiresAt);
            }
            return current;
        });
        return reservation != null && reservation.owner.equals(owner);
    }

    public static boolean isClaimedByOther(ServerLevel world, UUID owner, BlockPos pos) {
        long now = world.getGameTime();
        Reservation reservation = RESERVATIONS.get(new Key(world.dimension(), pos.immutable()));
        return reservation != null && reservation.expiresAt > now && !reservation.owner.equals(owner);
    }

    public static void releaseAll(UUID owner) {
        RESERVATIONS.entrySet().removeIf(entry -> entry.getValue().owner.equals(owner));
    }

    public static void release(UUID owner, BlockPos pos) {
        BlockPos immutable = pos.immutable();
        RESERVATIONS.entrySet().removeIf(entry ->
                entry.getValue().owner.equals(owner) && entry.getKey().pos.equals(immutable));
    }

    public static void clearAll() {
        RESERVATIONS.clear();
    }

    private record Key(ResourceKey<Level> dimension, BlockPos pos) {
    }

    private record Reservation(UUID owner, long expiresAt) {
    }
}
