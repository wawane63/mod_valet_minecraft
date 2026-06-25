package com.wawane.valet.ai.tasks;

import com.wawane.valet.ValetDebug;
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class MiningRuntimeTask {
    private static final int FAILED_TARGET_MEMORY_TICKS = 400;
    private static final int MAX_PATH_FAILURES_BEFORE_BACKOFF = 6;

    private final Control control;
    private BlockPos targetOrePos;
    private BlockPos miningPos;
    private BlockState miningState;
    private boolean miningOre;
    private boolean miningBonusResource;
    private boolean veinExhausted;
    private int pathFailures;
    private final List<BlockPos> veinTargets = new ArrayList<>();
    private final Map<BlockPos, Integer> failedTargets = new HashMap<>();

    public MiningRuntimeTask(Control control) {
        this.control = control;
    }

    public void tickCooldown() {
        Iterator<Map.Entry<BlockPos, Integer>> iterator = failedTargets.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, Integer> entry = iterator.next();
            int ticks = entry.getValue() - 1;
            if (ticks <= 0) {
                iterator.remove();
            } else {
                entry.setValue(ticks);
            }
        }
    }

    public void findTarget(ServerWorld world) {
        BlockPos workOrigin = control.getWorkOrigin(world);
        if (workOrigin == null) {
            ValetDebug.record(control.villager(), "mine no_work_origin");
            control.setDelayTicks(40);
            return;
        }

        if (!control.hasInventorySpace()) {
            ValetDebug.record(control.villager(), "mine inventory_full -> RETURNING");
            control.setState(State.RETURNING);
            return;
        }

        if (veinExhausted && control.hasInventoryItems()) {
            ValetDebug.record(control.villager(), "mine vein_done -> RETURNING");
            veinExhausted = false;
            control.setState(State.RETURNING);
            control.setDelayTicks(4);
            return;
        }

        targetOrePos = selectNextVeinOre(world);
        if (targetOrePos == null && veinExhausted && control.hasInventoryItems()) {
            ValetDebug.record(control.villager(), "mine vein_done -> RETURNING");
            veinExhausted = false;
            control.setState(State.RETURNING);
            control.setDelayTicks(4);
            return;
        }

        if (targetOrePos == null) {
            BlockPos seedOrePos = findNearestResource(world, workOrigin);
            if (seedOrePos == null) {
                clearVeinState();
                ValetDebug.record(control.villager(), "mine no_target");
                control.setState(State.RETURNING);
                control.setDelayTicks(control.noTargetDelayTicks());
                return;
            }

            veinTargets.clear();
            veinExhausted = false;
            veinTargets.addAll(findResourceCluster(world, seedOrePos, workOrigin));
            targetOrePos = selectNextVeinOre(world);
        }

        if (targetOrePos == null) {
            control.setState(State.RETURNING);
            control.setDelayTicks(control.noTargetDelayTicks());
            return;
        }

        Set<BlockPos> goals = control.findStandGoals(world, targetOrePos, PathPurpose.ORE);
        if (goals.contains(control.villager().getBlockPos())) {
            beginMining(world, targetOrePos, true);
            return;
        }

        List<BlockPos> path = control.planPathToAdjacent(world, PathPurpose.ORE, targetOrePos, goals);
        if (path.isEmpty()) {
            pathFailures++;
            rememberFailedTarget(targetOrePos);
            ValetDebug.record(control.villager(), "mine no_path target=" + ValetDebug.shortPos(targetOrePos));
            removeFromCurrentVein(targetOrePos);
            targetOrePos = null;
            if (pathFailures >= MAX_PATH_FAILURES_BEFORE_BACKOFF) {
                clearVeinState();
                pathFailures = 0;
                control.setState(State.RETURNING);
                control.setDelayTicks(control.noTargetDelayTicks());
                return;
            }
            control.setState(control.interruptedWorkState());
            control.setDelayTicks(30);
            return;
        }

        pathFailures = 0;
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
        boolean bonusResource = !ore && control.currentPathPurpose() == PathPurpose.ORE && control.isBonusResource(blockState);
        if (ore && !control.canReachTargetFromStand(pos, control.villager().getBlockPos())) {
            ValetDebug.record(control.villager(), "mine unreachable target=" + ValetDebug.shortPos(pos));
            rememberFailedTarget(pos);
            removeFromCurrentVein(pos);
            control.setState(control.interruptedWorkState());
            return;
        }

        if (!control.canMineWorkBlock(world, pos, blockState)) {
            ValetDebug.record(control.villager(), "mine unmineable target=" + ValetDebug.shortPos(pos) + " block=" + blockState.getBlock().getTranslationKey());
            if (ore) {
                rememberFailedTarget(pos);
                removeFromCurrentVein(pos);
            }
            control.setState(control.interruptedWorkState());
            return;
        }

        miningPos = pos.toImmutable();
        failedTargets.remove(miningPos);
        miningState = blockState;
        miningOre = ore;
        miningBonusResource = bonusResource;
        control.villager().getNavigation().stop();
        control.setState(State.MINING);
        ValetDebug.record(control.villager(), "mine begin target=" + ValetDebug.shortPos(miningPos) + " ore=" + miningOre + " block=" + miningState.getBlock().getTranslationKey());
    }

    public void tickMining(ServerWorld world) {
        if (miningPos == null || miningState == null) {
            ValetDebug.record(control.villager(), "mine lost_state");
            control.setState(control.interruptedWorkState());
            return;
        }

        if (miningOre && !control.canReachTargetFromStand(miningPos, control.villager().getBlockPos())) {
            ValetDebug.record(control.villager(), "mine lost_reach target=" + ValetDebug.shortPos(miningPos));
            control.setState(control.interruptedWorkState());
            clearMiningState();
            return;
        }

        if (!world.getBlockState(miningPos).isOf(miningState.getBlock())) {
            if (miningOre) {
                removeFromCurrentVein(miningPos);
            }
            ValetDebug.record(control.villager(), "mine target_changed target=" + ValetDebug.shortPos(miningPos));
            control.setState(getAfterMiningState());
            clearMiningState();
            return;
        }

        if (miningOre && ValetOrders.get(control.villager()) == ValetOrder.CHOP_WOOD) {
            tickWoodMining(world);
            return;
        }

        control.villager().getLookControl().lookAt(miningPos.getX() + 0.5D, miningPos.getY() + 0.5D, miningPos.getZ() + 0.5D);
        boolean collectDrops = miningOre || miningBonusResource;
        List<ItemStack> drops = collectDrops ? Block.getDroppedStacks(miningState, world, miningPos, world.getBlockEntity(miningPos), control.villager(), control.getToolForBlock(miningState)) : List.of();
        if (collectDrops && !control.canStoreAllDrops(drops)) {
            ValetDebug.record(control.villager(), "mine drops_no_space target=" + ValetDebug.shortPos(miningPos) + " drops=" + drops.size());
            control.setState(State.RETURNING);
            clearMiningState();
            control.setDelayTicks(1);
            return;
        }

        control.animateMining(world, miningPos, miningState);
        world.breakBlock(miningPos, false, control.villager());
        ValetDebug.record(control.villager(), "mine broke target=" + ValetDebug.shortPos(miningPos) + " block=" + miningState.getBlock().getTranslationKey());
        if (collectDrops) {
            control.placeTorchIfNeeded(world, miningPos);
            control.collectDrops(drops);
            ValetProgress.addXp(control.villager(), miningOre ? 8 : 2);
        }
        if (miningOre) {
            removeFromCurrentVein(miningPos);
        }

        control.setState(getAfterMiningState());
        clearMiningState();
        control.setDelayTicks(control.actionDelayTicks());
    }

    public void tickCollecting(ServerWorld world) {
        control.collectNearbyItemEntities(world);
        if (veinExhausted && control.hasInventoryItems()) {
            ValetDebug.record(control.villager(), "mine collect vein_done -> RETURNING");
            veinExhausted = false;
            control.setState(State.RETURNING);
            control.clearPathState();
            return;
        }
        ValetDebug.record(control.villager(), "mine collect -> " + (control.hasInventorySpace() && control.hasMiningOrder() ? "FIND_TARGET" : "RETURNING"));
        control.setState(control.hasInventorySpace() && control.hasMiningOrder() ? State.FIND_TARGET : State.RETURNING);
        control.clearPathState();
    }

    public String debugSummary() {
        return "mineTarget=" + shortPos(targetOrePos)
                + " mining=" + shortPos(miningPos)
                + " vein=" + veinTargets.size()
                + " failed=" + failedTargets.size();
    }

    public void clearTarget() {
        targetOrePos = null;
    }

    public void clearMiningState() {
        miningPos = null;
        miningState = null;
        miningOre = false;
        miningBonusResource = false;
    }

    public void clearVeinState() {
        veinTargets.clear();
        veinExhausted = false;
        pathFailures = 0;
    }

    public void clearAll() {
        clearTarget();
        clearMiningState();
        clearVeinState();
        failedTargets.clear();
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
            if (control.matchesSelectedTarget(world, pos, blockState)) {
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
            if (!control.matchesSelectedTarget(world, pos, blockState)) {
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
            BlockState blockState = world.getBlockState(immutable);
            if (isFailedTarget(immutable)
                    || !control.matchesSelectedTarget(world, immutable, blockState)
                    || !control.canMineWorkBlock(world, immutable, blockState)) {
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
                (targetWorld, pos, blockState) -> control.matchesSelectedTarget(targetWorld, pos, blockState)
                        && control.canMineWorkBlock(targetWorld, pos, blockState),
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
        int before = veinTargets.size();
        veinTargets.removeIf(pos -> !isSelectedTarget(world, pos));
        if (before > 0 && veinTargets.isEmpty()) {
            veinExhausted = true;
        }
    }

    private void removeFromCurrentVein(BlockPos pos) {
        if (pos != null) {
            boolean removed = veinTargets.remove(pos);
            if (removed && veinTargets.isEmpty()) {
                veinExhausted = true;
            }
        }
    }

    private void rememberFailedTarget(BlockPos pos) {
        if (pos != null) {
            failedTargets.put(pos.toImmutable(), FAILED_TARGET_MEMORY_TICKS);
        }
    }

    private boolean isFailedTarget(BlockPos pos) {
        return failedTargets.containsKey(pos);
    }

    private List<BlockPos> findWoodCluster(ServerWorld world, BlockPos seed) {
        return WoodcuttingTask.findWoodCluster(world, seed, control.maxVeinBlocks(), control::matchesSelectedTarget);
    }

    private State getAfterMiningState() {
        if (!control.hasInventorySpace()) {
            return State.RETURNING;
        }
        return miningOre ? State.COLLECTING : State.EXECUTING_PATH;
    }

    private boolean isSelectedTarget(ServerWorld world, BlockPos pos) {
        if (pos == null) {
            return false;
        }
        BlockState blockState = world.getBlockState(pos);
        return control.matchesSelectedTarget(world, pos, blockState) && control.canMineWorkBlock(world, pos, blockState);
    }

    public void rememberCurrentTarget() {
        rememberFailedTarget(targetOrePos);
        removeFromCurrentVein(targetOrePos);
        targetOrePos = null;
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

    private static String shortPos(BlockPos pos) {
        return pos == null ? "-" : ValetDebug.shortPos(pos);
    }

    public interface Control {
        VillagerEntity villager();

        PathPurpose currentPathPurpose();

        BlockPos getWorkOrigin(ServerWorld world);

        boolean hasMiningOrder();

        boolean hasInventorySpace();

        boolean hasInventoryItems();

        boolean matchesSelectedTarget(ServerWorld world, BlockPos pos, BlockState blockState);

        boolean isBonusResource(BlockState blockState);

        Set<BlockPos> findStandGoals(ServerWorld world, BlockPos targetBlock, PathPurpose purpose);

        List<BlockPos> planPathToAdjacent(ServerWorld world, PathPurpose purpose, BlockPos targetBlock, Set<BlockPos> goals);

        void startPath(PathPurpose purpose, List<BlockPos> path);

        void clearPathState();

        boolean canReachTargetFromStand(BlockPos targetBlock, BlockPos stand);

        boolean canMineWorkBlock(ServerWorld world, BlockPos pos, BlockState blockState);

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
