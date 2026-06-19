package com.wawane.valet.construction;

import com.wawane.valet.ValetMod;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ValetConstructionMarkers {
    private static final int MAX_HEIGHT = 64;
    private static final int MAX_VOLUME = 24000;
    private static final int MAX_BLOCKS = 12000;
    private static final Map<UUID, Marker> FIRST_MARKERS = new ConcurrentHashMap<>();

    private ValetConstructionMarkers() {
    }

    public static void placeMarker(ServerWorld world, BlockPos pos, PlayerEntity player) {
        UUID playerId = player.getUuid();
        Marker previous = FIRST_MARKERS.get(playerId);
        if (previous == null || !previous.dimension().equals(world.getRegistryKey()) || previous.pos().equals(pos) || !isMarkerStillPlaced(world, previous.pos())) {
            FIRST_MARKERS.put(playerId, new Marker(world.getRegistryKey(), pos.toImmutable()));
            player.sendMessage(Text.translatable("message.valet.construction_first_marker"), true);
            return;
        }

        FIRST_MARKERS.remove(playerId);
        copyStructure(world, previous.pos(), pos.toImmutable(), player);
    }

    public static void clear(UUID playerId) {
        FIRST_MARKERS.remove(playerId);
    }

    public static void clearAll() {
        FIRST_MARKERS.clear();
    }

    private static boolean isMarkerStillPlaced(ServerWorld world, BlockPos pos) {
        return world.getBlockState(pos).isOf(ValetMod.CONSTRUCTION_BEACON);
    }

    private static void copyStructure(ServerWorld world, BlockPos first, BlockPos second, PlayerEntity player) {
        int minX = Math.min(first.getX(), second.getX());
        int maxX = Math.max(first.getX(), second.getX());
        int minZ = Math.min(first.getZ(), second.getZ());
        int maxZ = Math.max(first.getZ(), second.getZ());
        int baseY = Math.min(first.getY(), second.getY());
        int width = maxX - minX + 1;
        int depth = maxZ - minZ + 1;
        int maxWorldY = Math.min(world.getTopY(), baseY + MAX_HEIGHT);

        int topY = baseY - 1;
        for (int y = baseY; y < maxWorldY; y++) {
            if (hasCopyableBlockInLayer(world, minX, maxX, y, minZ, maxZ)) {
                topY = y;
                if (width * depth * (topY - baseY + 1) > MAX_VOLUME) {
                    player.sendMessage(Text.translatable("message.valet.construction_too_large"), true);
                    return;
                }
            }
        }

        if (topY < baseY) {
            player.sendMessage(Text.translatable("message.valet.construction_empty"), true);
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
                        player.sendMessage(Text.translatable("message.valet.construction_too_large"), true);
                        return;
                    }
                }
            }
        }

        if (entries.isEmpty()) {
            player.sendMessage(Text.translatable("message.valet.construction_empty"), true);
            return;
        }

        ValetConstructionStorage storage = ValetConstructionStorage.get(world);
        ValetConstructionBlueprint blueprint = storage.addBlueprint(storage.nextDefaultName(), width, topY - baseY + 1, depth, entries);
        if (blueprint == null) {
            player.sendMessage(Text.translatable("message.valet.construction_storage_full", ValetConstructionStorage.MAX_BLUEPRINTS), true);
            return;
        }

        player.sendMessage(Text.translatable(
                "message.valet.construction_saved",
                blueprint.name(),
                blueprint.width(),
                blueprint.height(),
                blueprint.depth(),
                blueprint.blockCount()
        ), false);
    }

    private static boolean hasCopyableBlockInLayer(ServerWorld world, int minX, int maxX, int y, int minZ, int maxZ) {
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
                && !state.isOf(ValetMod.CONSTRUCTION_BEACON)
                && state.getBlock().asItem() != Items.AIR;
    }

    private record Marker(RegistryKey<World> dimension, BlockPos pos) {
    }
}
