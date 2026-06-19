package com.wawane.valet.ai.tasks;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.List;

public final class WoodcuttingTask {
    private WoodcuttingTask() {
    }

    public static List<BlockPos> findWoodCluster(ServerWorld world, BlockPos seed, int maxBlocks, MiningTask.ResourceMatcher matcher) {
        return MiningTask.findCluster(world, seed, maxBlocks, matcher, pos -> true, WoodcuttingTask::woodNeighbors);
    }

    public static List<BlockPos> woodNeighbors(BlockPos pos) {
        return List.of(pos.up(), pos.down(), pos.north(), pos.south(), pos.east(), pos.west());
    }
}
