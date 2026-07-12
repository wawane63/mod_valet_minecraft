package com.wawane.valet.ai.tasks;

import com.wawane.valet.ValetDebug;
import com.wawane.valet.ValetMod;
import com.wawane.valet.ai.ValetStateMachine.PathPurpose;
import com.wawane.valet.ai.ValetStateMachine.State;
import com.wawane.valet.ai.inventory.ValetInventoryTransfer;
import com.wawane.valet.progress.ValetProgress;
import com.wawane.valet.state.ValetBehavior;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class LogisticsRuntimeTask {
    private final Control control;
    private BlockPos chestPos;

    public LogisticsRuntimeTask(Control control) {
        this.control = control;
    }

    public void returnToChest(ServerLevel world) {
        BlockPos workOrigin = control.getWorkOrigin(world);
        if (workOrigin == null) {
            ValetDebug.record(control.villager(), "logistics no_work_origin");
            control.setDelayTicks(40);
            return;
        }

        if (!control.hasInventoryItems()) {
            ValetDebug.record(control.villager(), "logistics no_items");
            control.setState(canResumeWorkWithoutDepositing() ? State.FIND_TARGET : State.RETURNING_HOME);
            control.setDelayTicks(4);
            return;
        }

        chestPos = findBestContainer(world, workOrigin);
        if (chestPos == null) {
            ValetDebug.record(control.villager(), "logistics no_chest");
            control.setState(canResumeWorkWithoutDepositing() ? State.FIND_TARGET : State.RETURNING_HOME);
            control.setDelayTicks(40);
            return;
        }

        Set<BlockPos> goals = control.findStandGoals(world, chestPos, PathPurpose.CHEST);
        if (goals.contains(control.villager().blockPosition())) {
            control.setState(State.DEPOSITING);
            return;
        }

        List<BlockPos> path = control.planPathToAdjacent(world, PathPurpose.CHEST, chestPos, goals);
        if (path.isEmpty()) {
            ValetDebug.record(control.villager(), "logistics no_chest_path pos=" + ValetDebug.shortPos(chestPos));
            control.setState(canResumeWorkWithoutDepositing() ? State.FIND_TARGET : State.RETURNING_HOME);
            control.setDelayTicks(20);
            return;
        }

        control.startPath(PathPurpose.CHEST, path);
    }

    public void returnToWorkstation(ServerLevel world) {
        BlockPos workOrigin = control.getWorkOrigin(world);
        if (workOrigin == null) {
            ValetDebug.record(control.villager(), "logistics no_work_origin_home");
            control.setDelayTicks(40);
            return;
        }

        if (control.isNearWorkstation(world, workOrigin)) {
            ValetDebug.record(control.villager(), "logistics at_home");
            ValetBehavior.markRecallArrived(world, control.villager());
            control.clearPathState();
            control.clearMiningState();
            control.setState(State.IDLE);
            idleAtWorkstation(world);
            return;
        }

        Set<BlockPos> goals = control.findStandGoals(world, workOrigin, PathPurpose.HOME);
        List<BlockPos> path = control.planPathToAdjacent(world, PathPurpose.HOME, workOrigin, goals);
        if (path.isEmpty()) {
            ValetDebug.record(control.villager(), "logistics no_home_path pos=" + ValetDebug.shortPos(workOrigin));
            control.setDelayTicks(20);
            return;
        }

        control.startPath(PathPurpose.HOME, path);
    }

    public void idleAtWorkstation(ServerLevel world) {
        if (control.hasConstructionOrder()) {
            control.setState(State.FIND_TARGET);
            return;
        }

        if (control.hasCraftOrder()) {
            control.setState(State.FIND_TARGET);
            return;
        }

        if (control.hasFarmOrder()) {
            control.setState(control.hasInventorySpace() ? State.FIND_TARGET : State.RETURNING);
            return;
        }

        if (control.hasBreedingOrder()) {
            control.setState(control.hasInventorySpace() ? State.FIND_TARGET : State.RETURNING);
            return;
        }

        if (control.hasCookingWork()) {
            control.setState(State.FIND_TARGET);
            return;
        }

        if (control.hasStewardWork()) {
            control.setState(State.FIND_TARGET);
            return;
        }

        if (control.hasMiningOrder()) {
            control.setState(control.hasInventorySpace() ? State.FIND_TARGET : State.RETURNING);
            return;
        }

        BlockPos workOrigin = control.getWorkOrigin(world);
        if (workOrigin == null) {
            control.setDelayTicks(40);
            return;
        }

        if (!control.isNearWorkstation(world, workOrigin)) {
            control.setState(State.RETURNING_HOME);
            return;
        }

        ValetBehavior.markRecallArrived(world, control.villager());
        control.villager().getNavigation().stop();
        control.villager().setDeltaMovement(0.0D, control.villager().getDeltaMovement().y, 0.0D);
        control.villager().getLookControl().setLookAt(workOrigin.getX() + 0.5D, workOrigin.getY() + 0.5D, workOrigin.getZ() + 0.5D);
    }

    public void tickDepositing(ServerLevel world) {
        int movedItems = 0;
        if (chestPos != null) {
            movedItems += ValetInventoryTransfer.depositInventory(world, chestPos, control.villager().getInventory());
        }

        if (movedItems > 0) {
            control.animateChestUse(world, chestPos);
            ValetProgress.addXp(control.villager(), Math.max(2, movedItems / 8));
            ValetDebug.record(control.villager(), "logistics deposited items=" + movedItems + " chest=" + shortPos(chestPos));
        } else {
            ValetDebug.record(control.villager(), "logistics deposit_empty chest=" + shortPos(chestPos));
        }

        clearChestTarget();
        control.clearPathState();
        if (control.hasInventoryItems()) {
            if (movedItems <= 0 && canResumeWorkWithoutDepositing()) {
                ValetDebug.record(control.villager(), "logistics resume_after_empty_deposit");
                control.setState(State.FIND_TARGET);
                control.setDelayTicks(4);
                return;
            }
            if (movedItems <= 0) {
                control.setState(State.RETURNING_HOME);
                control.setDelayTicks(20);
                return;
            }
            control.setState(State.RETURNING);
            control.setDelayTicks(4);
            return;
        }

        control.setState(control.hasConstructionOrder()
                || control.hasCraftOrder()
                || control.hasCookingWork()
                || control.hasStewardWork()
                || (control.hasMiningOrder() || control.hasFarmOrder() || control.hasBreedingOrder()) && control.hasInventorySpace()
                ? State.FIND_TARGET
                : State.RETURNING_HOME);
        control.setDelayTicks(4);
    }

    public void clearChestTarget() {
        chestPos = null;
    }

    public String debugSummary() {
        return "chest=" + shortPos(chestPos);
    }

    private boolean canResumeWorkWithoutDepositing() {
        return control.hasInventorySpace()
                && (control.hasMiningOrder()
                || control.hasFarmOrder()
                || control.hasBreedingOrder()
                || control.hasConstructionOrder()
                || control.hasCraftOrder()
                || control.hasCookingWork()
                || control.hasStewardWork());
    }

    private BlockPos findNearestContainer(ServerLevel world, BlockPos origin) {
        return findNearest(world, origin, control.chestRadius(), 4, pos -> {
            BlockState blockState = world.getBlockState(pos);
            if (control.hasCookingWork()) {
                if (!blockState.is(ValetMod.COOK_CHEST)) {
                    return false;
                }
            } else if (!blockState.is(Blocks.CHEST) && !blockState.is(Blocks.TRAPPED_CHEST) && !blockState.is(Blocks.BARREL)) {
                return false;
            }

            Container target = ValetInventoryTransfer.getContainerInventory(world, pos);
            return target != null
                    && ValetInventoryTransfer.canAcceptAnyDepositableStack(target, control.villager().getInventory());
        });
    }

    private BlockPos findBestContainer(ServerLevel world, BlockPos workOrigin) {
        BlockPos villagerPos = control.villager().blockPosition();
        BlockPos nearVillager = findNearestContainer(world, villagerPos);
        BlockPos nearWork = findNearestContainer(world, workOrigin);
        if (nearVillager == null) {
            return nearWork;
        }
        if (nearWork == null) {
            return nearVillager;
        }
        return squaredDistance(villagerPos, nearVillager) <= squaredDistance(villagerPos, nearWork) ? nearVillager : nearWork;
    }

    private BlockPos findNearest(ServerLevel world, BlockPos origin, int horizontalRadius, int verticalRadius, BlockPredicate predicate) {
        BlockPos nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (BlockPos pos : BlockPos.withinManhattan(origin, horizontalRadius, verticalRadius, horizontalRadius)) {
            BlockPos immutable = pos.immutable();
            if (predicate.test(immutable)) {
                double distance = squaredDistance(origin, immutable);
                if (distance < nearestDistance) {
                    nearest = immutable;
                    nearestDistance = distance;
                }
            }
        }

        return nearest;
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

    @FunctionalInterface
    private interface BlockPredicate {
        boolean test(BlockPos pos);
    }

    public interface Control {
        Villager villager();

        BlockPos getWorkOrigin(ServerLevel world);

        boolean hasMiningOrder();

        boolean hasFarmOrder();

        boolean hasBreedingOrder();

        boolean hasConstructionOrder();

        boolean hasCraftOrder();

        boolean hasCookingWork();

        boolean hasStewardWork();

        boolean hasInventorySpace();

        boolean hasInventoryItems();

        boolean isNearWorkstation(ServerLevel world, BlockPos workOrigin);

        Set<BlockPos> findStandGoals(ServerLevel world, BlockPos targetBlock, PathPurpose purpose);

        List<BlockPos> planPathToAdjacent(ServerLevel world, PathPurpose purpose, BlockPos targetBlock, Set<BlockPos> goals);

        void startPath(PathPurpose purpose, List<BlockPos> path);

        void clearPathState();

        void clearMiningState();

        int chestRadius();

        void animateChestUse(ServerLevel world, BlockPos pos);

        void setState(State state);

        void setDelayTicks(int ticks);
    }
}
