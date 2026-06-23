package com.wawane.valet.ai.tasks;

import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class MiningTask {
    private MiningTask() {
    }

    public static List<BlockPos> findCluster(ServerWorld world, BlockPos seed, int maxBlocks, ResourceMatcher matcher, AreaPredicate areaPredicate, NeighborProvider neighborProvider) {
        if (!matcher.matches(world, seed, world.getBlockState(seed))) {
            return List.of();
        }

        List<BlockPos> result = new ArrayList<>();
        List<BlockPos> open = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        open.add(seed.toImmutable());
        visited.add(seed.toImmutable());

        for (int index = 0; index < open.size() && result.size() < maxBlocks; index++) {
            BlockPos current = open.get(index);
            if (!areaPredicate.contains(current) || !matcher.matches(world, current, world.getBlockState(current))) {
                continue;
            }

            result.add(current);
            for (BlockPos next : neighborProvider.neighbors(current)) {
                if (visited.add(next)) {
                    open.add(next);
                }
            }
        }

        return result;
    }

    public static List<BlockPos> oreNeighbors(BlockPos pos) {
        List<BlockPos> result = new ArrayList<>(26);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx != 0 || dy != 0 || dz != 0) {
                        result.add(pos.add(dx, dy, dz));
                    }
                }
            }
        }
        return result;
    }

    @FunctionalInterface
    public interface ResourceMatcher {
        boolean matches(ServerWorld world, BlockPos pos, BlockState state);
    }

    @FunctionalInterface
    public interface AreaPredicate {
        boolean contains(BlockPos pos);
    }

    @FunctionalInterface
    public interface NeighborProvider {
        List<BlockPos> neighbors(BlockPos pos);
    }
}
