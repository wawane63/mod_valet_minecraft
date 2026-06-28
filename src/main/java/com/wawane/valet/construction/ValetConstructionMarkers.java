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
    private static final int MAX_HEIGHT = 64;
    private static final int MAX_VOLUME = 24000;
    private static final int MAX_BLOCKS = 12000;
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
        int width = maxX - minX + 1;
        int depth = maxZ - minZ + 1;
        int maxWorldY = Math.min(world.getMaxY(), baseY + MAX_HEIGHT);

        int topY = baseY - 1;
        for (int y = baseY; y < maxWorldY; y++) {
            if (hasCopyableBlockInLayer(world, minX, maxX, y, minZ, maxZ)) {
                topY = y;
                if (width * depth * (topY - baseY + 1) > MAX_VOLUME) {
                    player.sendOverlayMessage(Component.translatable("message.valet.construction_too_large"));
                    return;
                }
            }
        }

        if (topY < baseY) {
            player.sendOverlayMessage(Component.translatable("message.valet.construction_empty"));
            return;
        }

        ArrayList<ValetConstructionBlueprint.Entry> entries = new ArrayList<>();
        for (int y = baseY; y <= topY; y++) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = world.getBlockState(pos);
                    if (!isCopyable(state)) {
                        continue;
                    }

                    entries.add(new ValetConstructionBlueprint.Entry(x - minX, y - baseY, z - minZ, state));
                    if (entries.size() > MAX_BLOCKS) {
                        player.sendOverlayMessage(Component.translatable("message.valet.construction_too_large"));
                        return;
                    }
                }
            }
        }

        if (entries.isEmpty()) {
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

    private static boolean hasCopyableBlockInLayer(ServerLevel world, int minX, int maxX, int y, int minZ, int maxZ) {
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                if (isCopyable(world.getBlockState(new BlockPos(x, y, z)))) {
                    return true;
                }
            }
        }
        return false;
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
