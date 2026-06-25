package com.wawane.valet.order;

import com.wawane.valet.ValetHome;
import com.wawane.valet.progress.ValetPerk;
import com.wawane.valet.progress.ValetProgress;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;

public final class ValetMiningScanner {
    public static final int ORE_SCAN_RADIUS = 14;
    public static final int ORE_SCAN_VERTICAL_RADIUS = 8;
    private static final int VISION_SCAN_RADIUS_BONUS = 8;
    private static final int VISION_SCAN_VERTICAL_RADIUS_BONUS = 4;

    private ValetMiningScanner() {
    }

    public static int[] countNearbyOres(World world, BlockPos origin) {
        return countNearbyOres(world, origin, ORE_SCAN_RADIUS, ORE_SCAN_VERTICAL_RADIUS);
    }

    private static int[] countNearbyOres(World world, BlockPos origin, int radius, int verticalRadius) {
        int[] counts = new int[ValetMineTarget.values().length];

        for (BlockPos pos : BlockPos.iterateOutwards(origin, radius, verticalRadius, radius)) {
            for (ValetMineTarget target : ValetMineTarget.values()) {
                if (target.matches(world.getBlockState(pos))) {
                    counts[target.ordinal()]++;
                    break;
                }
            }
        }

        return counts;
    }

    public static int[] countNearbyWood(World world, BlockPos origin) {
        return countNearbyWood(world, origin, ORE_SCAN_RADIUS, ORE_SCAN_VERTICAL_RADIUS);
    }

    private static int[] countNearbyWood(World world, BlockPos origin, int radius, int verticalRadius) {
        int[] counts = new int[ValetWoodTarget.values().length];
        Map<BlockPos, Boolean> naturalTreeCache = new HashMap<>();

        for (BlockPos pos : BlockPos.iterateOutwards(origin, radius, verticalRadius, radius)) {
            BlockPos immutable = pos.toImmutable();
            ValetWoodTarget target = ValetWoodTarget.fromState(world.getBlockState(immutable));
            if (target != null && naturalTreeCache.computeIfAbsent(immutable, key -> target.matchesNaturalTree(world, key))) {
                counts[target.ordinal()]++;
            }
        }

        return counts;
    }

    public static int[] countNearbyOres(ServerWorld world, VillagerEntity villager) {
        return countNearbyOres(world, getWorkOrigin(world, villager), scanRadius(villager), scanVerticalRadius(villager));
    }

    public static int[] countNearbyWood(ServerWorld world, VillagerEntity villager) {
        return countNearbyWood(world, getWorkOrigin(world, villager), scanRadius(villager), scanVerticalRadius(villager));
    }

    private static BlockPos getWorkOrigin(ServerWorld world, VillagerEntity villager) {
        BlockPos home = ValetHome.get(world, villager);
        return home == null ? villager.getBlockPos() : home;
    }

    private static int scanRadius(VillagerEntity villager) {
        return ValetProgress.hasPerk(villager, ValetPerk.VISION) ? ORE_SCAN_RADIUS + VISION_SCAN_RADIUS_BONUS : ORE_SCAN_RADIUS;
    }

    private static int scanVerticalRadius(VillagerEntity villager) {
        return ValetProgress.hasPerk(villager, ValetPerk.VISION) ? ORE_SCAN_VERTICAL_RADIUS + VISION_SCAN_VERTICAL_RADIUS_BONUS : ORE_SCAN_VERTICAL_RADIUS;
    }
}
