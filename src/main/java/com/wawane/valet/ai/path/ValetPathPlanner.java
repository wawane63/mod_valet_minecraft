package com.wawane.valet.ai.path;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

public final class ValetPathPlanner {
    private static final int SEARCH_MARGIN = 6;

    public List<BlockPos> planPathToAdjacent(ServerWorld world, BlockPos origin, BlockPos start, BlockPos targetBlock, Set<BlockPos> goals, int maxPathNodes, int maxPathLength, StepPredicate stepPredicate, StepCost stepCost) {
        if (goals.isEmpty()) {
            return List.of();
        }

        SearchBounds bounds = SearchBounds.around(origin, start, targetBlock);
        PriorityQueue<PathNode> open = new PriorityQueue<>(Comparator.comparingInt(node -> node.score));
        Map<BlockPos, Integer> costs = new HashMap<>();
        Map<BlockPos, BlockPos> parents = new HashMap<>();
        Set<BlockPos> closed = new HashSet<>();

        costs.put(start, 0);
        open.add(new PathNode(start, heuristic(start, targetBlock)));

        int visited = 0;
        while (!open.isEmpty() && visited++ < maxPathNodes) {
            PathNode node = open.poll();
            if (!closed.add(node.pos)) {
                continue;
            }

            if (goals.contains(node.pos)) {
                return rebuildPath(parents, start, node.pos, maxPathLength);
            }

            for (BlockPos next : neighbors(node.pos)) {
                if (!bounds.contains(next) || closed.contains(next) || !stepPredicate.canPrepareStep(world, node.pos, next)) {
                    continue;
                }

                int nextCost = costs.get(node.pos) + stepCost.movementCost(world, node.pos, next);
                if (nextCost >= costs.getOrDefault(next, Integer.MAX_VALUE)) {
                    continue;
                }

                costs.put(next, nextCost);
                parents.put(next, node.pos);
                open.add(new PathNode(next, nextCost + heuristic(next, targetBlock)));
            }
        }

        return List.of();
    }

    private List<BlockPos> rebuildPath(Map<BlockPos, BlockPos> parents, BlockPos start, BlockPos end, int maxPathLength) {
        List<BlockPos> reversed = new ArrayList<>();
        BlockPos current = end;

        while (!current.equals(start)) {
            reversed.add(current);
            current = parents.get(current);
            if (current == null || reversed.size() > maxPathLength) {
                return List.of();
            }
        }

        List<BlockPos> result = new ArrayList<>();
        for (int i = reversed.size() - 1; i >= 0; i--) {
            result.add(reversed.get(i));
        }
        return result;
    }

    private List<BlockPos> neighbors(BlockPos pos) {
        List<BlockPos> result = new ArrayList<>(12);
        int[][] horizontal = new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

        for (int[] direction : horizontal) {
            for (int dy = -1; dy <= 1; dy++) {
                result.add(pos.add(direction[0], dy, direction[1]));
            }
        }

        return result;
    }

    private int heuristic(BlockPos pos, BlockPos target) {
        return (Math.abs(pos.getX() - target.getX())
                + Math.abs(pos.getY() - target.getY())
                + Math.abs(pos.getZ() - target.getZ())) * 10;
    }

    private record PathNode(BlockPos pos, int score) {
    }

    private record SearchBounds(int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
        static SearchBounds around(BlockPos origin, BlockPos start, BlockPos target) {
            int minX = Math.min(Math.min(origin.getX(), start.getX()), target.getX()) - SEARCH_MARGIN;
            int maxX = Math.max(Math.max(origin.getX(), start.getX()), target.getX()) + SEARCH_MARGIN;
            int minY = Math.min(Math.min(origin.getY(), start.getY()), target.getY()) - SEARCH_MARGIN;
            int maxY = Math.max(Math.max(origin.getY(), start.getY()), target.getY()) + SEARCH_MARGIN;
            int minZ = Math.min(Math.min(origin.getZ(), start.getZ()), target.getZ()) - SEARCH_MARGIN;
            int maxZ = Math.max(Math.max(origin.getZ(), start.getZ()), target.getZ()) + SEARCH_MARGIN;
            return new SearchBounds(minX, maxX, minY, maxY, minZ, maxZ);
        }

        boolean contains(BlockPos pos) {
            return pos.getX() >= minX
                    && pos.getX() <= maxX
                    && pos.getY() >= minY
                    && pos.getY() <= maxY
                    && pos.getZ() >= minZ
                    && pos.getZ() <= maxZ;
        }
    }

    @FunctionalInterface
    public interface StepPredicate {
        boolean canPrepareStep(ServerWorld world, BlockPos from, BlockPos to);
    }

    @FunctionalInterface
    public interface StepCost {
        int movementCost(ServerWorld world, BlockPos from, BlockPos to);
    }
}
