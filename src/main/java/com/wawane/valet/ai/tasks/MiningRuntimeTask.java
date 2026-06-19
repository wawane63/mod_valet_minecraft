package com.wawane.valet.ai.tasks;

import com.wawane.valet.ai.ValetStateMachine.PathPurpose;
import com.wawane.valet.ai.ValetStateMachine.State;
import com.wawane.valet.order.ValetOrder;
import com.wawane.valet.order.ValetOrders;
import com.wawane.valet.progress.ValetProgress;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class MiningRuntimeTask {
    private final Control control;
    private BlockPos targetOrePos;
    private BlockPos miningPos;
    private BlockState miningState;
    private boolean miningOre;
    private final List<BlockPos> veinTargets = new ArrayList<>();

    public MiningRuntimeTask(Control control) {
        this.control = control;
    }

    public void findTarget(ServerWorld world) {
        BlockPos workOrigin = control.getWorkOrigin(world);
        if (workOrigin == null) {
            control.setDelayTicks(40);
            return;
        }

        if (!control.hasInventorySpace()) {
            control.setState(State.RETURNING);
            return;
        }

        targetOrePos = selectNextVeinOre(world);
        if (targetOrePos == null) {
            BlockPos seedOrePos = findNearestResource(world, workOrigin);
            if (seedOrePos == null) {
                clearVeinState();
                control.setState(control.hasInventoryItems() ? State.RETURNING : State.RETURNING_HOME);
                control.setDelayTicks(control.noTargetDelayTicks());
                return;
            }

            veinTargets.clear();
            veinTargets.addAll(findResourceCluster(world, seedOrePos, workOrigin));
            targetOrePos = selectNextVeinOre(world);
        }

        if (targetOrePos == null) {
            control.setState(control.hasInventoryItems() ? State.RETURNING : State.RETURNING_HOME);
            control.setDelayTicks(control.noTargetDelayTicks());
            return;
        }

        Set<BlockPos> goals = control.findStandGoals(world, targetOrePos);
        if (goals.contains(control.villager().getBlockPos())) {
            beginMining(world, targetOrePos, true);
            return;
        }

        List<BlockPos> path = control.planPathToAdjacent(world, targetOrePos, goals);
        if (path.isEmpty()) {
            removeFromCurrentVein(targetOrePos);
            targetOrePos = null;
            control.setState(control.interruptedWorkState());
            control.setDelayTicks(10);
            return;
        }

        control.startPath(PathPurpose.ORE, path);
    }

    public void completePath(ServerWorld world) {
        if (isSelectedTarget(world, targetOrePos)) {
            beginMining(world, targetOrePos, true);
        } else {
            removeFromCurrentVein(targetOrePos);
            control.setState(control.interruptedWorkState());
        }
    }

    public void beginMining(ServerWorld world, BlockPos pos, boolean ore) {
        BlockState blockState = world.getBlockState(pos);
        boolean targetResource = ore || control.currentPathPurpose() == PathPurpose.ORE && control.matchesSelectedTarget(blockState);
        if (ore && !control.canReachTargetFromStand(pos, control.villager().getBlockPos())) {
            removeFromCurrentVein(pos);
            control.setState(control.interruptedWorkState());
            return;
        }

        if (!control.canMinePathBlock(world, pos, blockState)) {
            control.setState(control.interruptedPathState());
            return;
        }

        miningPos = pos.toImmutable();
        miningState = blockState;
        miningOre = targetResource;
        control.villager().getNavigation().stop();
        control.setState(State.MINING);
    }

    public void tickMining(ServerWorld world) {
        if (miningPos == null || miningState == null) {
            control.setState(control.interruptedWorkState());
            return;
        }

        if (miningOre && !control.canReachTargetFromStand(miningPos, control.villager().getBlockPos())) {
            control.setState(control.interruptedWorkState());
            clearMiningState();
            return;
        }

        if (!world.getBlockState(miningPos).isOf(miningState.getBlock())) {
            if (miningOre) {
                removeFromCurrentVein(miningPos);
            }
            control.setState(miningOre ? State.COLLECTING : State.EXECUTING_PATH);
            clearMiningState();
            return;
        }

        if (miningOre && ValetOrders.get(control.villager()) == ValetOrder.CHOP_WOOD) {
            tickWoodMining(world);
            return;
        }

        control.villager().getLookControl().lookAt(miningPos.getX() + 0.5D, miningPos.getY() + 0.5D, miningPos.getZ() + 0.5D);
        List<ItemStack> drops = Block.getDroppedStacks(miningState, world, miningPos, world.getBlockEntity(miningPos), control.villager(), control.getToolForBlock(miningState));
        if (!control.canStoreAllDrops(drops)) {
            control.setState(State.RETURNING);
            clearMiningState();
            control.setDelayTicks(1);
            return;
        }

        control.animateMining(world, miningPos, miningState);
        world.breakBlock(miningPos, false, control.villager());
        control.placeTorchIfNeeded(world, miningPos);
        control.collectDrops(drops);
        ValetProgress.addXp(control.villager(), miningOre ? 8 : 1);
        if (miningOre) {
            removeFromCurrentVein(miningPos);
        }

        control.setState(!control.hasInventorySpace() ? State.RETURNING : miningOre ? State.COLLECTING : State.EXECUTING_PATH);
        clearMiningState();
        control.setDelayTicks(control.actionDelayTicks());
    }

    public void tickCollecting(ServerWorld world) {
        control.collectNearbyItemEntities(world);
        control.setState(control.hasInventorySpace() && control.hasMiningOrder() ? State.FIND_TARGET : State.RETURNING);
        control.clearPathState();
    }

    public void clearTarget() {
        targetOrePos = null;
    }

    public void clearMiningState() {
        miningPos = null;
        miningState = null;
        miningOre = false;
    }

    public void clearVeinState() {
        veinTargets.clear();
    }

    public void clearAll() {
        clearTarget();
        clearMiningState();
        clearVeinState();
    }

    private void tickWoodMining(ServerWorld world) {
        List<BlockPos> logs = findWoodCluster(world, miningPos);
        if (logs.isEmpty()) {
            control.setState(control.interruptedWorkState());
            clearMiningState();
            return;
        }

        List<ItemStack> drops = new ArrayList<>();
        for (BlockPos pos : logs) {
            BlockState blockState = world.getBlockState(pos);
            if (control.matchesSelectedTarget(blockState)) {
                drops.addAll(Block.getDroppedStacks(blockState, world, pos, world.getBlockEntity(pos), control.villager(), control.getToolForBlock(blockState)));
            }
        }

        if (!control.canStoreAllDrops(drops)) {
            control.setState(State.RETURNING);
            clearMiningState();
            control.setDelayTicks(1);
            return;
        }

        control.villager().swingHand(net.minecraft.util.Hand.MAIN_HAND);
        for (BlockPos pos : logs) {
            BlockState blockState = world.getBlockState(pos);
            if (!control.matchesSelectedTarget(blockState)) {
                continue;
            }

            world.syncWorldEvent(2001, pos, Block.getRawIdFromState(blockState));
            world.breakBlock(pos, false, control.villager());
            removeFromCurrentVein(pos);
        }

        control.collectDrops(drops);
        ValetProgress.addXp(control.villager(), Math.max(4, logs.size() * 3));
        control.setState(control.hasInventorySpace() && control.hasMiningOrder() ? State.COLLECTING : State.RETURNING);
        clearMiningState();
        control.setDelayTicks(control.actionDelayTicks());
    }

    private BlockPos findNearestResource(ServerWorld world, BlockPos origin) {
        if (!control.hasMiningOrder()) {
            return null;
        }

        BlockPos nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        int radius = control.mineRadius();
        int verticalRadius = control.mineVerticalRadius();

        for (BlockPos pos : BlockPos.iterateOutwards(origin, radius, verticalRadius, radius)) {
            BlockPos immutable = pos.toImmutable();
            if (!control.matchesSelectedTarget(world.getBlockState(immutable))) {
                continue;
            }

            double distance = squaredDistance(origin, immutable);
            if (distance < nearestDistance) {
                nearest = immutable;
                nearestDistance = distance;
            }
        }

        return nearest;
    }

    private List<BlockPos> findResourceCluster(ServerWorld world, BlockPos seed, BlockPos origin) {
        return MiningTask.findCluster(
                world,
                seed,
                control.maxVeinBlocks(),
                control::matchesSelectedTarget,
                pos -> isInsideMiningArea(origin, pos),
                ValetOrders.get(control.villager()) == ValetOrder.CHOP_WOOD ? WoodcuttingTask::woodNeighbors : MiningTask::oreNeighbors
        );
    }

    private BlockPos selectNextVeinOre(ServerWorld world) {
        pruneCurrentVein(world);
        BlockPos nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        BlockPos villagerPos = control.villager().getBlockPos();

        for (BlockPos pos : veinTargets) {
            double distance = squaredDistance(villagerPos, pos);
            if (distance < nearestDistance) {
                nearest = pos;
                nearestDistance = distance;
            }
        }

        return nearest;
    }

    private void pruneCurrentVein(ServerWorld world) {
        veinTargets.removeIf(pos -> !isSelectedTarget(world, pos));
    }

    private void removeFromCurrentVein(BlockPos pos) {
        if (pos != null) {
            veinTargets.remove(pos);
        }
    }

    private List<BlockPos> findWoodCluster(ServerWorld world, BlockPos seed) {
        return WoodcuttingTask.findWoodCluster(world, seed, control.maxVeinBlocks(), control::matchesSelectedTarget);
    }

    private boolean isSelectedTarget(ServerWorld world, BlockPos pos) {
        return pos != null && control.matchesSelectedTarget(world.getBlockState(pos));
    }

    private boolean isInsideMiningArea(BlockPos origin, BlockPos pos) {
        return Math.abs(pos.getX() - origin.getX()) <= control.mineRadius()
                && Math.abs(pos.getY() - origin.getY()) <= control.mineVerticalRadius()
                && Math.abs(pos.getZ() - origin.getZ()) <= control.mineRadius();
    }

    private static double squaredDistance(BlockPos first, BlockPos second) {
        int dx = first.getX() - second.getX();
        int dy = first.getY() - second.getY();
        int dz = first.getZ() - second.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    public interface Control {
        VillagerEntity villager();

        PathPurpose currentPathPurpose();

        BlockPos getWorkOrigin(ServerWorld world);

        boolean hasMiningOrder();

        boolean hasInventorySpace();

        boolean hasInventoryItems();

        boolean matchesSelectedTarget(BlockState blockState);

        Set<BlockPos> findStandGoals(ServerWorld world, BlockPos targetBlock);

        List<BlockPos> planPathToAdjacent(ServerWorld world, BlockPos targetBlock, Set<BlockPos> goals);

        void startPath(PathPurpose purpose, List<BlockPos> path);

        void clearPathState();

        boolean canReachTargetFromStand(BlockPos targetBlock, BlockPos stand);

        boolean canMinePathBlock(ServerWorld world, BlockPos pos, BlockState blockState);

        ItemStack getToolForBlock(BlockState blockState);

        boolean canStoreAllDrops(List<ItemStack> drops);

        void collectDrops(List<ItemStack> drops);

        void collectNearbyItemEntities(ServerWorld world);

        void placeTorchIfNeeded(ServerWorld world, BlockPos minedPos);

        void animateMining(ServerWorld world, BlockPos miningPos, BlockState miningState);

        State interruptedPathState();

        State interruptedWorkState();

        void setState(State state);

        void setDelayTicks(int ticks);

        int noTargetDelayTicks();

        int mineRadius();

        int mineVerticalRadius();

        int maxVeinBlocks();

        int actionDelayTicks();
    }
}
