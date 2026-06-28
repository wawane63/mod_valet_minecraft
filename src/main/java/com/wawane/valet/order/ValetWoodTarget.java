package com.wawane.valet.order;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public enum ValetWoodTarget {
    OAK("wood.valet.oak", Blocks.OAK_LOG),
    SPRUCE("wood.valet.spruce", Blocks.SPRUCE_LOG),
    BIRCH("wood.valet.birch", Blocks.BIRCH_LOG),
    JUNGLE("wood.valet.jungle", Blocks.JUNGLE_LOG),
    ACACIA("wood.valet.acacia", Blocks.ACACIA_LOG),
    DARK_OAK("wood.valet.dark_oak", Blocks.DARK_OAK_LOG),
    MANGROVE("wood.valet.mangrove", Blocks.MANGROVE_LOG),
    CHERRY("wood.valet.cherry", Blocks.CHERRY_LOG),
    CRIMSON("wood.valet.crimson", Blocks.CRIMSON_STEM),
    WARPED("wood.valet.warped", Blocks.WARPED_STEM);

    private static final int TREE_LOG_CLUSTER_LIMIT = 512;
    private static final int TREE_HORIZONTAL_SPREAD = 4;
    private static final int TREE_VERTICAL_SPREAD = 32;
    private static final int MIN_TREE_LOGS = 2;

    private final String translationKey;
    private final Block[] blocks;

    ValetWoodTarget(String translationKey, Block... blocks) {
        this.translationKey = translationKey;
        this.blocks = blocks;
    }

    public String getTranslationKey() {
        return translationKey;
    }

    public boolean matches(BlockState state) {
        for (Block block : blocks) {
            if (state.is(block)) {
                return true;
            }
        }
        return false;
    }

    public boolean matchesNaturalTree(Level world, BlockPos pos) {
        return matches(world.getBlockState(pos)) && hasConnectedTreeCrown(world, pos);
    }

    private boolean hasConnectedTreeCrown(Level world, BlockPos origin) {
        ArrayDeque<BlockPos> open = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        open.add(origin.immutable());
        int logs = 0;
        boolean hasCrown = false;

        while (!open.isEmpty() && visited.size() < TREE_LOG_CLUSTER_LIMIT) {
            BlockPos current = open.removeFirst();
            if (!visited.add(current) || !isInsideTreeCluster(origin, current) || !matches(world.getBlockState(current))) {
                continue;
            }

            logs++;
            hasCrown = hasCrown || hasNearbyCrownBlock(world, current);
            for (BlockPos next : connectedLogNeighbors(current)) {
                open.add(next);
            }
        }

        return logs >= MIN_TREE_LOGS && hasCrown;
    }

    private boolean hasNearbyCrownBlock(Level world, BlockPos pos) {
        for (BlockPos candidate : BlockPos.betweenClosed(
                pos.getX() - 2,
                pos.getY(),
                pos.getZ() - 2,
                pos.getX() + 2,
                pos.getY() + 3,
                pos.getZ() + 2
        )) {
            BlockState state = world.getBlockState(candidate);
            if (state.is(BlockTags.LEAVES)
                    || state.is(Blocks.NETHER_WART_BLOCK)
                    || state.is(Blocks.WARPED_WART_BLOCK)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isInsideTreeCluster(BlockPos origin, BlockPos pos) {
        return Math.abs(pos.getX() - origin.getX()) <= TREE_HORIZONTAL_SPREAD
                && Math.abs(pos.getY() - origin.getY()) <= TREE_VERTICAL_SPREAD
                && Math.abs(pos.getZ() - origin.getZ()) <= TREE_HORIZONTAL_SPREAD;
    }

    private static Iterable<BlockPos> connectedLogNeighbors(BlockPos pos) {
        Set<BlockPos> result = new HashSet<>(26);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx != 0 || dy != 0 || dz != 0) {
                        result.add(pos.offset(dx, dy, dz));
                    }
                }
            }
        }
        return result;
    }

    public static ValetWoodTarget fromState(BlockState state) {
        for (ValetWoodTarget target : values()) {
            if (target.matches(state)) {
                return target;
            }
        }
        return null;
    }

    public static ValetWoodTarget fromIndex(int index) {
        ValetWoodTarget[] values = values();
        if (index < 0 || index >= values.length) {
            return null;
        }
        return values[index];
    }
}
