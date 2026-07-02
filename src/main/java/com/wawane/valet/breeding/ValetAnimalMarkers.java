package com.wawane.valet.breeding;

import com.wawane.valet.ValetMod;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

public final class ValetAnimalMarkers {
    private static final int MAX_WIDTH = 64;
    private static final int MAX_DEPTH = 64;
    private static final int MAX_HEIGHT = 24;
    private static final int DETECTION_VERTICAL_MARGIN = 2;
    private static final Map<UUID, Marker> FIRST_MARKERS = new ConcurrentHashMap<>();

    private ValetAnimalMarkers() {
    }

    public static void placeMarker(ServerLevel world, BlockPos pos, Player player) {
        UUID playerId = player.getUUID();
        Marker previous = FIRST_MARKERS.get(playerId);
        if (previous == null || !previous.dimension().equals(world.dimension()) || previous.pos().equals(pos) || !isMarkerStillPlaced(world, previous.pos())) {
            FIRST_MARKERS.put(playerId, new Marker(world.dimension(), pos.immutable()));
            player.sendOverlayMessage(Component.translatable("message.valet.animal_first_marker"));
            return;
        }

        FIRST_MARKERS.remove(playerId);
        createAnimalArea(world, previous.pos(), pos.immutable(), player);
    }

    public static void clear(UUID playerId) {
        FIRST_MARKERS.remove(playerId);
    }

    public static void clearAll() {
        FIRST_MARKERS.clear();
    }

    private static boolean isMarkerStillPlaced(ServerLevel world, BlockPos pos) {
        return world.getBlockState(pos).is(ValetMod.ANIMAL_BEACON);
    }

    private static void createAnimalArea(ServerLevel world, BlockPos first, BlockPos second, Player player) {
        int minX = Math.min(first.getX(), second.getX());
        int maxX = Math.max(first.getX(), second.getX());
        int minY = Math.min(first.getY(), second.getY());
        int maxY = Math.max(first.getY(), second.getY());
        int minZ = Math.min(first.getZ(), second.getZ());
        int maxZ = Math.max(first.getZ(), second.getZ());
        int width = maxX - minX + 1;
        int height = maxY - minY + 1;
        int depth = maxZ - minZ + 1;
        if (width > MAX_WIDTH || depth > MAX_DEPTH || height > MAX_HEIGHT) {
            player.sendOverlayMessage(Component.translatable("message.valet.animal_too_large"));
            return;
        }

        ValetAnimalType type = detectDominantType(world, new AABB(
                minX,
                minY - DETECTION_VERTICAL_MARGIN,
                minZ,
                maxX + 1.0D,
                maxY + DETECTION_VERTICAL_MARGIN + 1.0D,
                maxZ + 1.0D
        ));
        ValetAnimalStorage storage = ValetAnimalStorage.get(world);
        ValetAnimalArea area = storage.addArea(
                storage.nextDefaultName(type),
                type == null ? -1 : type.ordinal(),
                minX,
                minY,
                minZ,
                maxX,
                maxY,
                maxZ
        );
        if (area == null) {
            player.sendOverlayMessage(Component.translatable("message.valet.animal_storage_full", ValetAnimalStorage.MAX_AREAS));
            return;
        }

        player.sendSystemMessage(Component.translatable("message.valet.animal_saved", area.name(), width, height, depth));
    }

    private static ValetAnimalType detectDominantType(ServerLevel world, AABB bounds) {
        EnumMap<ValetAnimalType, Integer> counts = new EnumMap<>(ValetAnimalType.class);
        for (Animal animal : world.getEntitiesOfClass(Animal.class, bounds, animal -> animal.isAlive() && !animal.isRemoved())) {
            ValetAnimalType type = ValetAnimalType.fromAnimal(animal);
            if (type != null) {
                counts.merge(type, 1, Integer::sum);
            }
        }

        ValetAnimalType best = null;
        int bestCount = 0;
        for (Map.Entry<ValetAnimalType, Integer> entry : counts.entrySet()) {
            if (entry.getValue() > bestCount) {
                best = entry.getKey();
                bestCount = entry.getValue();
            }
        }
        return best;
    }

    private record Marker(ResourceKey<Level> dimension, BlockPos pos) {
    }
}
