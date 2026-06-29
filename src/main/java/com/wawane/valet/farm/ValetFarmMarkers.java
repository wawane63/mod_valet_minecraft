package com.wawane.valet.farm;

import com.wawane.valet.ValetMod;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public final class ValetFarmMarkers {
    private static final int MAX_WIDTH = 64;
    private static final int MAX_DEPTH = 64;
    private static final Map<UUID, Marker> FIRST_MARKERS = new ConcurrentHashMap<>();

    private ValetFarmMarkers() {
    }

    public static void placeMarker(ServerLevel world, BlockPos pos, Player player) {
        UUID playerId = player.getUUID();
        Marker previous = FIRST_MARKERS.get(playerId);
        if (previous == null || !previous.dimension().equals(world.dimension()) || previous.pos().equals(pos) || !isMarkerStillPlaced(world, previous.pos())) {
            FIRST_MARKERS.put(playerId, new Marker(world.dimension(), pos.immutable()));
            player.sendOverlayMessage(Component.translatable("message.valet.farm_first_marker"));
            return;
        }

        FIRST_MARKERS.remove(playerId);
        createFarmArea(world, previous.pos(), pos.immutable(), player);
    }

    public static void clear(UUID playerId) {
        FIRST_MARKERS.remove(playerId);
    }

    public static void clearAll() {
        FIRST_MARKERS.clear();
    }

    private static boolean isMarkerStillPlaced(ServerLevel world, BlockPos pos) {
        return world.getBlockState(pos).is(ValetMod.FARM_BEACON);
    }

    private static void createFarmArea(ServerLevel world, BlockPos first, BlockPos second, Player player) {
        int minX = Math.min(first.getX(), second.getX());
        int maxX = Math.max(first.getX(), second.getX());
        int minZ = Math.min(first.getZ(), second.getZ());
        int maxZ = Math.max(first.getZ(), second.getZ());
        int layerY = Math.min(world.getMaxY() - 1, Math.max(first.getY(), second.getY()));
        int width = maxX - minX + 1;
        int depth = maxZ - minZ + 1;
        if (width > MAX_WIDTH || depth > MAX_DEPTH) {
            player.sendOverlayMessage(Component.translatable("message.valet.farm_too_large"));
            return;
        }

        ValetFarmStorage storage = ValetFarmStorage.get(world);
        ValetFarmArea area = storage.addArea(storage.nextDefaultName(), minX, layerY, minZ, maxX, layerY, maxZ);
        if (area == null) {
            player.sendOverlayMessage(Component.translatable("message.valet.farm_storage_full", ValetFarmStorage.MAX_AREAS));
            return;
        }

        player.sendSystemMessage(Component.translatable("message.valet.farm_saved", area.name(), width, depth));
    }

    private record Marker(ResourceKey<Level> dimension, BlockPos pos) {
    }
}
