package com.wawane.valet.ai.path;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;

/** Garde-fous communs autour du pathfinding vanilla utilise par les valets. */
public final class ValetSafeNavigation {
    private static final int DEFAULT_MAX_LOCAL_NODES = 8;

    private ValetSafeNavigation() {
    }

    public static Path createSafeLocalPath(
            ServerLevel world,
            Villager villager,
            BlockPos target,
            int passageHeight,
            boolean allowWaterNodes
    ) {
        return createSafeLocalPath(world, villager, target, passageHeight, allowWaterNodes, DEFAULT_MAX_LOCAL_NODES);
    }

    public static Path createSafeLocalPath(
            ServerLevel world,
            Villager villager,
            BlockPos target,
            int passageHeight,
            boolean allowWaterNodes,
            int maxNodes
    ) {
        if (target == null || passageHeight < 2 || maxNodes < 1
                || (!isSafeStand(world, target, passageHeight)
                && !(allowWaterNodes && isSafeWaterColumn(world, target, passageHeight)))) {
            return null;
        }

        Path path = villager.getNavigation().createPath(target, 0);
        if (path == null || !path.canReach() || path.getNodeCount() == 0 || path.getNodeCount() > maxNodes) {
            return null;
        }

        BlockPos navigationStart = villager.blockPosition();
        BlockPos previous = null;
        for (int index = 0; index < path.getNodeCount(); index++) {
            BlockPos node = path.getNodePos(index);
            boolean startNode = index == 0;
            if (startNode && !isValidNavigationStart(navigationStart, node)) {
                return null;
            }
            if (!isSafeStand(world, node, passageHeight)
                    && !(startNode && isSafeNavigationStartStand(world, node, passageHeight))
                    && !(allowWaterNodes && (isSafeWaterColumn(world, node, passageHeight)
                    || startNode && isFluidStartColumn(world, node, passageHeight)))) {
                return null;
            }
            if (previous != null && !isHumanSizedStep(previous, node)) {
                return null;
            }
            previous = node;
        }

        return path;
    }

    public static boolean isSafeStand(ServerLevel world, BlockPos stand, int passageHeight) {
        if (stand == null || !isSafeSupport(world, stand.below())) {
            return false;
        }
        for (int y = 0; y < passageHeight; y++) {
            BlockPos pos = stand.above(y);
            BlockState state = world.getBlockState(pos);
            if (isHazardous(state) || !state.getFluidState().isEmpty() || !isOpenPassage(world, pos, state)) {
                return false;
            }
        }
        return true;
    }

    public static boolean isHumanSizedStep(BlockPos from, BlockPos to) {
        if (from == null || to == null) {
            return false;
        }
        int dx = Math.abs(to.getX() - from.getX());
        int dy = Math.abs(to.getY() - from.getY());
        int dz = Math.abs(to.getZ() - from.getZ());
        return dx <= 1 && dz <= 1 && dy <= 1 && dx + dz > 0;
    }

    private static boolean isValidNavigationStart(BlockPos entityBlock, BlockPos firstNode) {
        return Math.abs(firstNode.getX() - entityBlock.getX()) <= 1
                && Math.abs(firstNode.getY() - entityBlock.getY()) <= 1
                && Math.abs(firstNode.getZ() - entityBlock.getZ()) <= 1;
    }

    private static boolean isSafeNavigationStartStand(ServerLevel world, BlockPos stand, int passageHeight) {
        BlockPos supportPos = stand.below();
        BlockState support = world.getBlockState(supportPos);
        if (isHazardous(support)
                || !support.getFluidState().isEmpty()
                || support.getCollisionShape(world, supportPos).isEmpty()) {
            return false;
        }
        for (int y = 0; y < passageHeight; y++) {
            BlockPos pos = stand.above(y);
            BlockState state = world.getBlockState(pos);
            if (isHazardous(state) || !state.getFluidState().isEmpty() || !isOpenPassage(world, pos, state)) {
                return false;
            }
        }
        return true;
    }

    public static double distanceSquaredToCenter(Villager villager, BlockPos pos) {
        double dx = pos.getX() + 0.5D - villager.getX();
        double dy = pos.getY() - villager.getY();
        double dz = pos.getZ() + 0.5D - villager.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    public static boolean hasReached(Villager villager, BlockPos pos) {
        if (villager.blockPosition().equals(pos)) {
            return true;
        }
        double dx = pos.getX() + 0.5D - villager.getX();
        double dz = pos.getZ() + 0.5D - villager.getZ();
        return dx * dx + dz * dz <= 0.45D * 0.45D && Math.abs(villager.getY() - pos.getY()) <= 0.75D;
    }

    public static boolean isSafeWaterColumn(ServerLevel world, BlockPos stand, int passageHeight) {
        boolean foundWater = false;
        for (int y = 0; y < passageHeight; y++) {
            BlockPos pos = stand.above(y);
            BlockState state = world.getBlockState(pos);
            if (!state.getCollisionShape(world, pos).isEmpty()) {
                return false;
            }
            if (!state.getFluidState().isEmpty()) {
                if (!state.getFluidState().is(FluidTags.WATER)) {
                    return false;
                }
                foundWater = true;
            }
        }
        return foundWater;
    }

    public static boolean isSafeSupport(ServerLevel world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (state.is(Blocks.DIRT_PATH) || state.is(Blocks.FARMLAND) || state.getBlock() instanceof StairBlock) {
            return !isHazardous(state) && state.getFluidState().isEmpty();
        }
        return !isHazardous(state)
                && state.getFluidState().isEmpty()
                && !state.getCollisionShape(world, pos).isEmpty()
                && state.isFaceSturdy(world, pos, Direction.UP);
    }

    /** Comme un villageois vanilla : ouvre le bois, mais jamais une porte metallique fermee. */
    public static boolean isDoorPassage(BlockState state) {
        return state.getBlock() instanceof DoorBlock
                && state.hasProperty(DoorBlock.OPEN)
                && (state.getValue(DoorBlock.OPEN) || canOpenDoor(state));
    }

    public static boolean canOpenDoor(BlockState state) {
        return state.getBlock() instanceof DoorBlock && state.is(BlockTags.WOODEN_DOORS);
    }

    private static boolean isOpenPassage(ServerLevel world, BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof DoorBlock && state.hasProperty(DoorBlock.OPEN)) {
            return isDoorPassage(state);
        }
        if (state.getBlock() instanceof FenceGateBlock && state.hasProperty(FenceGateBlock.OPEN)) {
            return state.getValue(FenceGateBlock.OPEN);
        }
        return state.getCollisionShape(world, pos).isEmpty();
    }

    private static boolean isFluidStartColumn(ServerLevel world, BlockPos stand, int passageHeight) {
        boolean foundFluid = false;
        for (int y = 0; y < passageHeight; y++) {
            BlockPos pos = stand.above(y);
            BlockState state = world.getBlockState(pos);
            if (isHazardous(state) || !state.getCollisionShape(world, pos).isEmpty()) {
                return false;
            }
            foundFluid |= !state.getFluidState().isEmpty();
        }
        return foundFluid;
    }

    private static boolean isHazardous(BlockState state) {
        return state.is(Blocks.FIRE)
                || state.is(Blocks.SOUL_FIRE)
                || state.is(Blocks.CACTUS)
                || state.is(Blocks.MAGMA_BLOCK)
                || state.is(Blocks.CAMPFIRE)
                || state.is(Blocks.SOUL_CAMPFIRE)
                || state.is(Blocks.SWEET_BERRY_BUSH)
                || state.is(Blocks.WITHER_ROSE)
                || state.is(Blocks.POWDER_SNOW)
                || state.is(Blocks.POINTED_DRIPSTONE);
    }
}
