package com.wawane.valet.ai.tasks;

import com.wawane.valet.ValetDebug;
import com.wawane.valet.ai.ValetStateMachine.PathPurpose;
import com.wawane.valet.ai.ValetStateMachine.State;
import com.wawane.valet.order.ValetOrder;
import com.wawane.valet.order.ValetOrders;
import com.wawane.valet.progress.ValetProgress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class MiningRuntimeTask {
    private static final int FAILED_TARGET_MEMORY_TICKS = 400;
    private static final int MAX_PATH_FAILURES_BEFORE_BACKOFF = 6;
    private static final int BLOCK_RESERVATION_TICKS = 1200;
    private static final int WOODCUTTING_TICKS = 60;
    private static final int MAX_TREE_BLOCKS = 512;

    private final Control control;
    private BlockPos targetOrePos;
    private BlockPos miningPos;
    private BlockPos reservedTargetPos;
    private BlockPos reservedPathBlockPos;
    private BlockState miningState;
    private boolean miningOre;
    private boolean miningBonusResource;
    private boolean veinExhausted;
    private int pathFailures;
    private int woodcuttingTicks;
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

    public void findTarget(ServerLevel world) {
        BlockPos workOrigin = control.getWorkOrigin(world);
        if (workOrigin == null) {
            ValetDebug.record(control.villager(), "mine no_work_origin");
            control.setDelayTicks(40);
            return;
        }

        if (!control.hasInventorySpace()) {
            releaseReservedTarget();
            ValetDebug.record(control.villager(), "mine inventory_full -> RETURNING");
            control.setState(State.RETURNING);
            return;
        }

        if (veinExhausted && control.hasInventoryItems()) {
            releaseReservedTarget();
            ValetDebug.record(control.villager(), "mine vein_done -> RETURNING");
            veinExhausted = false;
            control.setState(State.RETURNING);
            control.setDelayTicks(4);
            return;
        }

        targetOrePos = selectNextVeinOre(world);
        if (targetOrePos == null && veinExhausted && control.hasInventoryItems()) {
            releaseReservedTarget();
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
                control.setState(control.hasInventoryItems() ? State.RETURNING : State.RETURNING_HOME);
                control.setDelayTicks(control.noTargetDelayTicks());
                return;
            }

            veinTargets.clear();
            veinExhausted = false;
            veinTargets.addAll(findResourceCluster(world, seedOrePos, workOrigin));
            targetOrePos = selectNextVeinOre(world);
        }

        if (targetOrePos == null) {
            releaseReservedTarget();
            control.setState(control.hasInventoryItems() ? State.RETURNING : State.RETURNING_HOME);
            control.setDelayTicks(control.noTargetDelayTicks());
            return;
        }

        if (!claimMiningBlock(world, targetOrePos, true)) {
            return;
        }

        Set<BlockPos> goals = control.findStandGoals(world, targetOrePos, PathPurpose.ORE);
        if (goals.contains(control.villager().blockPosition())) {
            beginMining(world, targetOrePos, true);
            return;
        }

        List<BlockPos> path = control.planPathToAdjacent(world, PathPurpose.ORE, targetOrePos, goals);
        if (path.isEmpty()) {
            pathFailures++;
            rememberFailedTarget(targetOrePos);
            ValetDebug.record(control.villager(), "mine no_path target=" + ValetDebug.shortPos(targetOrePos));
            removeFromCurrentVein(targetOrePos);
            releaseReservedTarget();
            targetOrePos = null;
            if (pathFailures >= MAX_PATH_FAILURES_BEFORE_BACKOFF) {
                clearVeinState();
                pathFailures = 0;
                control.setState(control.hasInventoryItems() ? State.RETURNING : State.RETURNING_HOME);
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

    public void completePath(ServerLevel world) {
        if (isSelectedTarget(world, targetOrePos)) {
            beginMining(world, targetOrePos, true);
        } else {
            removeFromCurrentVein(targetOrePos);
            releaseReservedTarget();
            control.setState(control.interruptedWorkState());
        }
    }

    public void beginMining(ServerLevel world, BlockPos pos, boolean ore) {
        BlockState blockState = world.getBlockState(pos);
        boolean bonusResource = !ore && control.currentPathPurpose() == PathPurpose.ORE && control.isBonusResource(blockState);
        if (ore && !control.canReachTargetFromStand(pos, control.villager().blockPosition())) {
            ValetDebug.record(control.villager(), "mine unreachable target=" + ValetDebug.shortPos(pos));
            rememberFailedTarget(pos);
            removeFromCurrentVein(pos);
            releaseReservedTarget();
            targetOrePos = null;
            control.setState(control.interruptedWorkState());
            return;
        }

        if (!control.canMineWorkBlock(world, pos, blockState)) {
            ValetDebug.record(control.villager(), "mine unmineable target=" + ValetDebug.shortPos(pos) + " block=" + blockState.getBlock().getDescriptionId());
            if (ore) {
                rememberFailedTarget(pos);
                removeFromCurrentVein(pos);
                releaseReservedTarget();
                targetOrePos = null;
            }
            control.setState(control.interruptedWorkState());
            return;
        }

        if (!claimMiningBlock(world, pos, ore)) {
            return;
        }

        miningPos = pos.immutable();
        failedTargets.remove(miningPos);
        miningState = blockState;
        miningOre = ore;
        miningBonusResource = bonusResource;
        woodcuttingTicks = ore && ValetOrders.get(control.villager()) == ValetOrder.CHOP_WOOD ? WOODCUTTING_TICKS : 0;
        control.villager().getNavigation().stop();
        control.setState(State.MINING);
        ValetDebug.record(control.villager(), "mine begin target=" + ValetDebug.shortPos(miningPos) + " ore=" + miningOre + " block=" + miningState.getBlock().getDescriptionId());
    }

    public void tickMining(ServerLevel world) {
        if (miningPos == null || miningState == null) {
            ValetDebug.record(control.villager(), "mine lost_state");
            control.setState(control.interruptedWorkState());
            return;
        }

        if (miningOre && !control.canReachTargetFromStand(miningPos, control.villager().blockPosition())) {
            ValetDebug.record(control.villager(), "mine lost_reach target=" + ValetDebug.shortPos(miningPos));
            control.setState(control.interruptedWorkState());
            clearMiningState();
            return;
        }

        if (!world.getBlockState(miningPos).is(miningState.getBlock())) {
            if (miningOre) {
                removeFromCurrentVein(miningPos);
            }
            ValetDebug.record(control.villager(), "mine target_changed target=" + ValetDebug.shortPos(miningPos));
            control.setState(getAfterMiningState());
            clearMiningState();
            return;
        }

        if (!claimMiningBlock(world, miningPos, miningOre)) {
            return;
        }

        if (miningOre && ValetOrders.get(control.villager()) == ValetOrder.CHOP_WOOD) {
            tickWoodMining(world);
            return;
        }

        control.villager().getLookControl().setLookAt(miningPos.getX() + 0.5D, miningPos.getY() + 0.5D, miningPos.getZ() + 0.5D);
        boolean collectDrops = miningOre || miningBonusResource;
        List<ItemStack> drops = collectDrops ? Block.getDrops(miningState, world, miningPos, world.getBlockEntity(miningPos), control.villager(), control.getToolForBlock(miningState)) : List.of();
        if (collectDrops && !control.canStoreAllDrops(drops)) {
            ValetDebug.record(control.villager(), "mine drops_no_space target=" + ValetDebug.shortPos(miningPos) + " drops=" + drops.size());
            control.setState(State.RETURNING);
            clearMiningState();
            control.setDelayTicks(1);
            return;
        }

        if (!world.destroyBlock(miningPos, false, control.villager())) {
            ValetDebug.record(control.villager(), "mine break_failed target=" + ValetDebug.shortPos(miningPos));
            if (miningOre) {
                rememberFailedTarget(miningPos);
                removeFromCurrentVein(miningPos);
                control.setState(control.interruptedWorkState());
            } else {
                control.clearPathState();
                control.setState(control.interruptedPathState());
            }
            clearMiningState();
            control.setDelayTicks(20);
            return;
        }
        control.animateMining(world, miningPos, miningState);
        ValetDebug.record(control.villager(), "mine broke target=" + ValetDebug.shortPos(miningPos) + " block=" + miningState.getBlock().getDescriptionId());
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

    public void tickCollecting(ServerLevel world) {
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
        releaseReservedTarget();
    }

    public void clearMiningState() {
        if (miningOre) {
            releaseReservedTarget();
        } else {
            releaseReservedPathBlock();
        }
        miningPos = null;
        miningState = null;
        miningOre = false;
        miningBonusResource = false;
        woodcuttingTicks = 0;
    }

    public void clearVeinState() {
        veinTargets.clear();
        veinExhausted = false;
        pathFailures = 0;
        releaseReservedTarget();
    }

    public void clearAll() {
        clearTarget();
        clearMiningState();
        clearVeinState();
        failedTargets.clear();
    }

    private void tickWoodMining(ServerLevel world) {
        control.villager().getLookControl().setLookAt(miningPos.getX() + 0.5D, miningPos.getY() + 0.5D, miningPos.getZ() + 0.5D);
        if (woodcuttingTicks > 0) {
            if (woodcuttingTicks % 10 == 0) {
                control.villager().swing(InteractionHand.MAIN_HAND);
            }
            woodcuttingTicks--;
            return;
        }

        List<BlockPos> logs = findWoodCluster(world, miningPos);
        if (logs.isEmpty()) {
            control.setState(control.interruptedWorkState());
            clearMiningState();
            return;
        }

        List<WoodBlock> blocks = new ArrayList<>();
        List<ItemStack> drops = new ArrayList<>();
        for (BlockPos pos : logs) {
            BlockState blockState = world.getBlockState(pos);
            if (control.matchesSelectedWoodBlock(blockState)) {
                List<ItemStack> blockDrops = Block.getDrops(blockState, world, pos, world.getBlockEntity(pos), control.villager(), control.getToolForBlock(blockState));
                blocks.add(new WoodBlock(pos, blockState, blockDrops));
                drops.addAll(blockDrops);
            }
        }

        if (!control.canStoreAllDrops(drops)) {
            control.setState(State.RETURNING);
            clearMiningState();
            control.setDelayTicks(1);
            return;
        }

        control.villager().swing(InteractionHand.MAIN_HAND);
        List<ItemStack> collectedDrops = new ArrayList<>(drops.size());
        int brokenBlocks = 0;
        for (WoodBlock block : blocks) {
            if (!world.getBlockState(block.pos()).is(block.state().getBlock())
                    || !world.destroyBlock(block.pos(), false, control.villager())) {
                continue;
            }

            world.levelEvent(2001, block.pos(), Block.getId(block.state()));
            collectedDrops.addAll(block.drops());
            removeFromCurrentVein(block.pos());
            brokenBlocks++;
        }

        if (brokenBlocks == 0) {
            rememberFailedTarget(miningPos);
            control.setState(control.interruptedWorkState());
            clearMiningState();
            control.setDelayTicks(20);
            return;
        }

        control.collectDrops(collectedDrops);
        ValetProgress.addXp(control.villager(), Math.max(4, brokenBlocks * 3));
        control.setState(control.hasInventorySpace() && control.hasMiningOrder() ? State.COLLECTING : State.RETURNING);
        clearMiningState();
        control.setDelayTicks(control.actionDelayTicks());
    }

    private BlockPos findNearestResource(ServerLevel world, BlockPos origin) {
        if (!control.hasMiningOrder()) {
            return null;
        }

        BlockPos nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        int radius = control.mineRadius();
        int verticalRadius = control.mineVerticalRadius();

        for (BlockPos pos : BlockPos.withinManhattan(origin, radius, verticalRadius, radius)) {
            BlockPos immutable = pos.immutable();
            BlockState blockState = world.getBlockState(immutable);
            if (isFailedTarget(immutable)
                    || control.isBlockReservedByOther(world, immutable)
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

    private List<BlockPos> findResourceCluster(ServerLevel world, BlockPos seed, BlockPos origin) {
        return MiningTask.findCluster(
                world,
                seed,
                control.maxVeinBlocks(),
                (targetWorld, pos, blockState) -> control.matchesSelectedTarget(targetWorld, pos, blockState)
                        && !control.isBlockReservedByOther(targetWorld, pos)
                        && control.canMineWorkBlock(targetWorld, pos, blockState),
                pos -> isInsideMiningArea(origin, pos)
        );
    }

    private BlockPos selectNextVeinOre(ServerLevel world) {
        pruneCurrentVein(world);
        BlockPos nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        BlockPos villagerPos = control.villager().blockPosition();

        for (BlockPos pos : veinTargets) {
            double distance = squaredDistance(villagerPos, pos);
            if (distance < nearestDistance) {
                nearest = pos;
                nearestDistance = distance;
            }
        }

        return nearest;
    }

    private void pruneCurrentVein(ServerLevel world) {
        int before = veinTargets.size();
        veinTargets.removeIf(pos -> control.isBlockReservedByOther(world, pos) || !isSelectedTarget(world, pos));
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
            failedTargets.put(pos.immutable(), FAILED_TARGET_MEMORY_TICKS);
        }
    }

    private boolean claimMiningBlock(ServerLevel world, BlockPos pos, boolean ore) {
        BlockPos immutable = pos.immutable();
        if (control.claimBlock(world, immutable, BLOCK_RESERVATION_TICKS)) {
            if (ore) {
                if (reservedTargetPos != null && !reservedTargetPos.equals(immutable)) {
                    releaseReservedTarget();
                }
                reservedTargetPos = immutable;
            } else {
                if (reservedPathBlockPos != null && !reservedPathBlockPos.equals(immutable)) {
                    releaseReservedPathBlock();
                }
                reservedPathBlockPos = immutable;
            }
            return true;
        }

        ValetDebug.record(control.villager(), "mine reserved target=" + ValetDebug.shortPos(immutable));
        if (ore) {
            releaseReservedTarget();
            removeFromCurrentVein(immutable);
            targetOrePos = null;
            control.setState(control.interruptedWorkState());
        } else {
            releaseReservedPathBlock();
            control.clearPathState();
            control.setState(control.interruptedPathState());
        }
        control.setDelayTicks(8);
        return false;
    }

    private void releaseReservedTarget() {
        if (reservedTargetPos != null) {
            control.releaseBlock(reservedTargetPos);
            reservedTargetPos = null;
        }
    }

    private void releaseReservedPathBlock() {
        if (reservedPathBlockPos != null) {
            control.releaseBlock(reservedPathBlockPos);
            reservedPathBlockPos = null;
        }
    }

    private boolean isFailedTarget(BlockPos pos) {
        return failedTargets.containsKey(pos);
    }

    private List<BlockPos> findWoodCluster(ServerLevel world, BlockPos seed) {
        return WoodcuttingTask.findWoodCluster(world, seed, MAX_TREE_BLOCKS, (targetWorld, pos, state) -> control.matchesSelectedWoodBlock(state));
    }

    private State getAfterMiningState() {
        if (!control.hasInventorySpace()) {
            return State.RETURNING;
        }
        return miningOre ? State.COLLECTING : State.EXECUTING_PATH;
    }

    private boolean isSelectedTarget(ServerLevel world, BlockPos pos) {
        if (pos == null) {
            return false;
        }
        BlockState blockState = world.getBlockState(pos);
        return control.matchesSelectedTarget(world, pos, blockState) && control.canMineWorkBlock(world, pos, blockState);
    }

    public void rememberCurrentTarget() {
        rememberFailedTarget(targetOrePos);
        removeFromCurrentVein(targetOrePos);
        releaseReservedTarget();
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

    private record WoodBlock(BlockPos pos, BlockState state, List<ItemStack> drops) {
    }

    public interface Control {
        Villager villager();

        PathPurpose currentPathPurpose();

        BlockPos getWorkOrigin(ServerLevel world);

        boolean hasMiningOrder();

        boolean hasInventorySpace();

        boolean hasInventoryItems();

        boolean matchesSelectedTarget(ServerLevel world, BlockPos pos, BlockState blockState);

        boolean isBonusResource(BlockState blockState);

        boolean matchesSelectedWoodBlock(BlockState blockState);

        Set<BlockPos> findStandGoals(ServerLevel world, BlockPos targetBlock, PathPurpose purpose);

        List<BlockPos> planPathToAdjacent(ServerLevel world, PathPurpose purpose, BlockPos targetBlock, Set<BlockPos> goals);

        void startPath(PathPurpose purpose, List<BlockPos> path);

        void clearPathState();

        boolean canReachTargetFromStand(BlockPos targetBlock, BlockPos stand);

        boolean canMineWorkBlock(ServerLevel world, BlockPos pos, BlockState blockState);

        boolean claimBlock(ServerLevel world, BlockPos pos, int ttlTicks);

        boolean isBlockReservedByOther(ServerLevel world, BlockPos pos);

        void releaseBlock(BlockPos pos);

        ItemStack getToolForBlock(BlockState blockState);

        boolean canStoreAllDrops(List<ItemStack> drops);

        void collectDrops(List<ItemStack> drops);

        void collectNearbyItemEntities(ServerLevel world);

        void placeTorchIfNeeded(ServerLevel world, BlockPos minedPos);

        void animateMining(ServerLevel world, BlockPos miningPos, BlockState miningState);

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
