package com.wawane.valet.ai.tasks;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public final class WoodcuttingTask {
    private WoodcuttingTask() {
    }

    public static List<BlockPos> findWoodCluster(ServerLevel world, BlockPos seed, int maxBlocks, MiningTask.ResourceMatcher matcher) {
        return MiningTask.findCluster(world, seed, maxBlocks, matcher, pos -> true);
    }
}
