package com.wawane.valet.order;

import com.wawane.valet.ValetHome;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class ValetMiningScanner {
    public static final int ORE_SCAN_RADIUS = 14;
    public static final int ORE_SCAN_VERTICAL_RADIUS = 8;

    private ValetMiningScanner() {
    }

    public static int[] countNearbyOres(World world, BlockPos origin) {
        int[] counts = new int[ValetMineTarget.values().length];

        for (BlockPos pos : BlockPos.iterateOutwards(origin, ORE_SCAN_RADIUS, ORE_SCAN_VERTICAL_RADIUS, ORE_SCAN_RADIUS)) {
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
        int[] counts = new int[ValetWoodTarget.values().length];

        for (BlockPos pos : BlockPos.iterateOutwards(origin, ORE_SCAN_RADIUS, ORE_SCAN_VERTICAL_RADIUS, ORE_SCAN_RADIUS)) {
            for (ValetWoodTarget target : ValetWoodTarget.values()) {
                if (target.matches(world.getBlockState(pos))) {
                    counts[target.ordinal()]++;
                    break;
                }
            }
        }

        return counts;
    }

    public static int[] countNearbyOres(ServerWorld world, VillagerEntity villager) {
        return countNearbyOres(world, getWorkOrigin(world, villager));
    }

    public static int[] countNearbyWood(ServerWorld world, VillagerEntity villager) {
        return countNearbyWood(world, getWorkOrigin(world, villager));
    }

    private static BlockPos getWorkOrigin(ServerWorld world, VillagerEntity villager) {
        BlockPos home = ValetHome.get(world, villager);
        return home == null ? villager.getBlockPos() : home;
    }
}
