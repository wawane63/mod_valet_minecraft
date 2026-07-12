package com.wawane.valet.order;

import com.wawane.valet.ValetHome;
import com.wawane.valet.progress.ValetPerk;
import com.wawane.valet.progress.ValetProgress;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.Level;

public final class ValetMiningScanner {
    public static final int ORE_SCAN_RADIUS = 14;
    public static final int ORE_SCAN_VERTICAL_RADIUS = 8;
    private static final int VISION_SCAN_RADIUS_BONUS = 8;
    private static final int VISION_SCAN_VERTICAL_RADIUS_BONUS = 4;

    private ValetMiningScanner() {
    }

    public static int[] countNearbyOres(Level world, BlockPos origin) {
        return countNearbyOres(world, origin, ORE_SCAN_RADIUS, ORE_SCAN_VERTICAL_RADIUS);
    }

    private static int[] countNearbyOres(Level world, BlockPos origin, int radius, int verticalRadius) {
        int[] counts = new int[ValetMineTarget.values().length];

        for (BlockPos pos : BlockPos.withinManhattan(origin, radius, verticalRadius, radius)) {
            for (ValetMineTarget target : ValetMineTarget.values()) {
                if (target.matches(world.getBlockState(pos))) {
                    counts[target.ordinal()]++;
                    break;
                }
            }
        }

        return counts;
    }

    public static int[] countNearbyWood(Level world, BlockPos origin) {
        return countNearbyWood(world, origin, ORE_SCAN_RADIUS, ORE_SCAN_VERTICAL_RADIUS);
    }

    private static int[] countNearbyWood(Level world, BlockPos origin, int radius, int verticalRadius) {
        int[] counts = new int[ValetWoodTarget.values().length];

        for (BlockPos pos : BlockPos.withinManhattan(origin, radius, verticalRadius, radius)) {
            ValetWoodTarget target = ValetWoodTarget.fromState(world.getBlockState(pos));
            if (target != null && target.matchesNaturalTree(world, pos)) {
                counts[target.ordinal()]++;
            }
        }

        return counts;
    }

    public static int[] countNearbyOres(ServerLevel world, Villager villager) {
        return countNearbyOres(world, getWorkOrigin(world, villager), scanRadius(villager), scanVerticalRadius(villager));
    }

    public static int[] countNearbyWood(ServerLevel world, Villager villager) {
        return countNearbyWood(world, getWorkOrigin(world, villager), scanRadius(villager), scanVerticalRadius(villager));
    }

    private static BlockPos getWorkOrigin(ServerLevel world, Villager villager) {
        BlockPos home = ValetHome.get(world, villager);
        return home == null ? villager.blockPosition() : home;
    }

    private static int scanRadius(Villager villager) {
        return ValetProgress.hasPerk(villager, ValetPerk.VISION) ? ORE_SCAN_RADIUS + VISION_SCAN_RADIUS_BONUS : ORE_SCAN_RADIUS;
    }

    private static int scanVerticalRadius(Villager villager) {
        return ValetProgress.hasPerk(villager, ValetPerk.VISION) ? ORE_SCAN_VERTICAL_RADIUS + VISION_SCAN_VERTICAL_RADIUS_BONUS : ORE_SCAN_VERTICAL_RADIUS;
    }
}
