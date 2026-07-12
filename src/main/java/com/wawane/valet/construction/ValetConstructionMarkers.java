package com.wawane.valet.construction;

import com.wawane.valet.ValetMod;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class ValetConstructionMarkers {
    private static final Map<UUID, Marker> FIRST_MARKERS = new ConcurrentHashMap<>();

    private ValetConstructionMarkers() {
    }

    public static void placeMarker(ServerLevel world, BlockPos pos, Player player) {
        UUID playerId = player.getUUID();
        Marker previous = FIRST_MARKERS.get(playerId);
        if (previous == null || !previous.dimension().equals(world.dimension()) || previous.pos().equals(pos) || !isMarkerStillPlaced(world, previous.pos())) {
            FIRST_MARKERS.put(playerId, new Marker(world.dimension(), pos.immutable()));
            player.sendOverlayMessage(Component.translatable("message.valet.construction_first_marker"));
            return;
        }

        FIRST_MARKERS.remove(playerId);
        copyStructure(world, previous.pos(), pos.immutable(), player);
    }

    public static void clear(UUID playerId) {
        FIRST_MARKERS.remove(playerId);
    }

    public static void clearAll() {
        FIRST_MARKERS.clear();
    }

    private static boolean isMarkerStillPlaced(ServerLevel world, BlockPos pos) {
        return world.getBlockState(pos).is(ValetMod.CONSTRUCTION_BEACON);
    }

    private static void copyStructure(ServerLevel world, BlockPos first, BlockPos second, Player player) {
        int minX = Math.min(first.getX(), second.getX());
        int maxX = Math.max(first.getX(), second.getX());
        int minZ = Math.min(first.getZ(), second.getZ());
        int maxZ = Math.max(first.getZ(), second.getZ());
        int baseY = Math.min(first.getY(), second.getY());
        long widthLong = (long) maxX - minX + 1L;
        long depthLong = (long) maxZ - minZ + 1L;
        long footprint = widthLong * depthLong;
        if (widthLong > ValetConstructionBlueprint.MAX_VOLUME
                || depthLong > ValetConstructionBlueprint.MAX_VOLUME
                || footprint > ValetConstructionBlueprint.MAX_VOLUME) {
            player.sendOverlayMessage(Component.translatable("message.valet.construction_too_large"));
            return;
        }
        int width = (int) widthLong;
        int depth = (int) depthLong;
        int maxWorldY = Math.min(world.getMaxY(), baseY + ValetConstructionBlueprint.MAX_HEIGHT);
        if (maxWorldY <= baseY || !areChunksLoaded(world, minX, minZ, maxX, maxZ)) {
            player.sendOverlayMessage(Component.translatable("message.valet.construction_too_large"));
            return;
        }

        int topY = baseY - 1;
        ArrayList<ValetConstructionBlueprint.Entry> entries = new ArrayList<>();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int y = baseY; y < maxWorldY; y++) {
            boolean hasCopyableBlock = false;
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockState state = world.getBlockState(cursor.set(x, y, z));
                    if (!isCopyable(state)) {
                        continue;
                    }

                    hasCopyableBlock = true;
                    entries.add(new ValetConstructionBlueprint.Entry(x - minX, y - baseY, z - minZ, state));
                    if (entries.size() > ValetConstructionBlueprint.MAX_BLOCKS) {
                        player.sendOverlayMessage(Component.translatable("message.valet.construction_too_large"));
                        return;
                    }
                }
            }
            if (hasCopyableBlock) {
                topY = y;
                if (footprint * (topY - baseY + 1L) > ValetConstructionBlueprint.MAX_VOLUME) {
                    player.sendOverlayMessage(Component.translatable("message.valet.construction_too_large"));
                    return;
                }
            }
        }

        if (topY < baseY || entries.isEmpty()) {
            player.sendOverlayMessage(Component.translatable("message.valet.construction_empty"));
            return;
        }

        ValetConstructionStorage storage = ValetConstructionStorage.get(world);
        ValetConstructionBlueprint blueprint = storage.addBlueprint(storage.nextDefaultName(), width, topY - baseY + 1, depth, entries);
        if (blueprint == null) {
            player.sendOverlayMessage(Component.translatable("message.valet.construction_storage_full", ValetConstructionStorage.MAX_BLUEPRINTS));
            return;
        }

        player.sendSystemMessage(Component.translatable(
                "message.valet.construction_saved",
                blueprint.name(),
                blueprint.width(),
                blueprint.height(),
                blueprint.depth(),
                blueprint.blockCount()
        ));
    }

    private static boolean areChunksLoaded(ServerLevel world, int minX, int minZ, int maxX, int maxZ) {
        int minChunkX = minX >> 4;
        int minChunkZ = minZ >> 4;
        int maxChunkX = maxX >> 4;
        int maxChunkZ = maxZ >> 4;
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                if (!world.hasChunk(chunkX, chunkZ)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean isCopyable(BlockState state) {
        return !state.isAir()
                && state.getFluidState().isEmpty()
                && !state.is(ValetMod.CONSTRUCTION_BEACON)
                && state.getBlock().asItem() != Items.AIR;
    }

    private record Marker(ResourceKey<Level> dimension, BlockPos pos) {
    }
}
