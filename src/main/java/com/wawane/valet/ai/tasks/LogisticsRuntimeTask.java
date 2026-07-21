package com.wawane.valet.ai.tasks;

import com.wawane.valet.ValetDebug;
import com.wawane.valet.ValetMod;
import com.wawane.valet.ai.ValetStateMachine.PathPurpose;
import com.wawane.valet.ai.ValetStateMachine.State;
import com.wawane.valet.ai.inventory.ValetInventoryTransfer;
import com.wawane.valet.breeding.ValetAnimalType;
import com.wawane.valet.order.ValetFarmCrop;
import com.wawane.valet.order.ValetOrders;
import com.wawane.valet.progress.ValetProgress;
import com.wawane.valet.state.ValetBehavior;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class LogisticsRuntimeTask {
    private static final int FARM_PLANTING_RESERVE_PER_ITEM = 16;
    private static final int BREEDING_SUPPLY_RESERVE_PER_ITEM = 16;

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

        int requestedPlantingCropMask = control.requestedFarmPlantingCropMask();
        if (!control.hasInventoryItems() && requestedPlantingCropMask == 0) {
            ValetDebug.record(control.villager(), "logistics no_items");
            control.setState(canResumeWorkWithoutDepositing() ? State.FIND_TARGET : State.RETURNING_HOME);
            control.setDelayTicks(4);
            return;
        }

        chestPos = findBestContainer(world, workOrigin, requestedPlantingCropMask);
        if (chestPos == null) {
            if (requestedPlantingCropMask != 0) {
                control.deferFarmPlantingRequest();
            }
            ValetDebug.record(control.villager(), requestedPlantingCropMask == 0
                    ? "logistics no_chest"
                    : "logistics no_planting_items crops=" + requestedPlantingCropMask + " deferred=true");
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
            if (requestedPlantingCropMask != 0) {
                control.deferFarmPlantingRequest();
            }
            ValetDebug.record(control.villager(), "logistics no_chest_path pos=" + ValetDebug.shortPos(chestPos));
            control.setState(canResumeWorkWithoutDepositing() ? State.FIND_TARGET : State.RETURNING_HOME);
            control.setDelayTicks(20);
            return;
        }

        control.startPath(PathPurpose.CHEST, path);
    }

    public void returnToAnchor(ServerLevel world) {
        BlockPos workOrigin = control.getWorkOrigin(world);
        if (workOrigin == null) {
            ValetDebug.record(control.villager(), "logistics no_work_origin_home");
            control.setDelayTicks(40);
            return;
        }

        if (control.isNearAnchor(world, workOrigin)) {
            ValetDebug.record(control.villager(), "logistics at_home");
            ValetBehavior.markRecallArrived(world, control.villager());
            control.clearPathState();
            control.clearMiningState();
            if (control.isRecallActive(world)) {
                control.villager().getNavigation().stop();
                control.villager().getLookControl().setLookAt(
                        workOrigin.getX() + 0.5D,
                        workOrigin.getY() + 0.5D,
                        workOrigin.getZ() + 0.5D
                );
                control.setState(State.RETURNING_HOME);
                control.setDelayTicks(20);
                return;
            }
            control.setState(State.IDLE);
            idleAtAnchor(world);
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

    public void idleAtAnchor(ServerLevel world) {
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

        if (!control.isNearAnchor(world, workOrigin)) {
            control.setState(State.RETURNING_HOME);
            return;
        }

        ValetBehavior.markRecallArrived(world, control.villager());
        control.villager().getNavigation().stop();
        control.villager().setDeltaMovement(0.0D, control.villager().getDeltaMovement().y, 0.0D);
        control.villager().getLookControl().setLookAt(workOrigin.getX() + 0.5D, workOrigin.getY() + 0.5D, workOrigin.getZ() + 0.5D);
    }

    public void tickDepositing(ServerLevel world) {
        Container inventory = control.villager().getInventory();
        int farmCropMask = control.hasFarmOrder() && ValetOrders.shouldReplantFarm(control.villager())
                ? ValetOrders.getFarmCropMask(control.villager())
                : 0;
        int movedItems = 0;
        if (chestPos != null) {
            movedItems += farmCropMask != 0
                    ? ValetInventoryTransfer.depositInventory(
                            world,
                            chestPos,
                            inventory,
                            stack -> isFarmPlantingItem(stack, farmCropMask),
                            FARM_PLANTING_RESERVE_PER_ITEM
                    )
                    : control.hasBreedingOrder()
                    ? ValetInventoryTransfer.depositInventory(
                            world,
                            chestPos,
                            inventory,
                            LogisticsRuntimeTask::isBreedingSupply,
                            BREEDING_SUPPLY_RESERVE_PER_ITEM
                    )
                    : ValetInventoryTransfer.depositInventory(world, chestPos, inventory);
        }

        int requestedPlantingCropMask = control.requestedFarmPlantingCropMask();
        int withdrawnItems = chestPos == null || requestedPlantingCropMask == 0
                ? 0
                : ValetInventoryTransfer.withdrawMatching(
                        world,
                        chestPos,
                        inventory,
                        control.getUsableInventorySlots(inventory),
                        stack -> isFarmPlantingItem(stack, requestedPlantingCropMask),
                        FARM_PLANTING_RESERVE_PER_ITEM
                );

        if (movedItems > 0) {
            control.animateChestUse(world, chestPos);
            ValetProgress.addXp(control.villager(), Math.max(2, movedItems / 8));
            ValetDebug.record(control.villager(), "logistics deposited items=" + movedItems + " chest=" + shortPos(chestPos));
        } else {
            ValetDebug.record(control.villager(), "logistics deposit_empty chest=" + shortPos(chestPos));
        }
        if (withdrawnItems > 0) {
            control.animateChestUse(world, chestPos);
            ValetDebug.record(control.villager(), "logistics withdrew_planting items=" + withdrawnItems
                    + " crops=" + requestedPlantingCropMask + " chest=" + shortPos(chestPos));
        } else if (requestedPlantingCropMask != 0) {
            control.deferFarmPlantingRequest();
            ValetDebug.record(control.villager(), "logistics no_planting_items crops="
                    + requestedPlantingCropMask + " chest=" + shortPos(chestPos) + " deferred=true");
        }

        clearChestTarget();
        control.clearPathState();
        if (withdrawnItems > 0) {
            control.setState(State.FIND_TARGET);
            control.setDelayTicks(4);
            return;
        }
        if (control.hasInventoryItems()) {
            if (farmCropMask != 0 && hasOnlyFarmPlantingItems(inventory, farmCropMask) && canResumeWorkWithoutDepositing()) {
                ValetDebug.record(control.villager(), "logistics retained_planting crops=" + farmCropMask);
                control.setState(State.FIND_TARGET);
                control.setDelayTicks(4);
                return;
            }
            if (control.hasBreedingOrder() && hasOnlyBreedingSupplies(inventory) && canResumeWorkWithoutDepositing()) {
                ValetDebug.record(control.villager(), "logistics retained_breeding_supplies");
                control.setState(State.FIND_TARGET);
                control.setDelayTicks(4);
                return;
            }
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

    public static boolean isBreedingSupply(ItemStack stack) {
        if (stack.is(Items.SHEARS) || stack.is(Items.BUCKET)) {
            return true;
        }
        for (ValetAnimalType type : ValetAnimalType.values()) {
            if (type.feedItems().contains(stack.getItem())) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasOnlyBreedingSupplies(Container inventory) {
        boolean found = false;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (!isBreedingSupply(stack)) {
                return false;
            }
            found = true;
        }
        return found;
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

    private BlockPos findBestContainer(ServerLevel world, BlockPos workOrigin, int requestedPlantingCropMask) {
        if (requestedPlantingCropMask != 0) {
            BlockPos plantingContainer = findBestPlantingContainer(world, workOrigin, requestedPlantingCropMask);
            if (plantingContainer != null) {
                return plantingContainer;
            }
        }

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

    private BlockPos findBestPlantingContainer(ServerLevel world, BlockPos workOrigin, int cropMask) {
        BlockPos nearest = nearerToVillager(null, findNearestPlantingContainer(world, control.villager().blockPosition(), cropMask));
        nearest = nearerToVillager(nearest, findNearestPlantingContainer(world, workOrigin, cropMask));
        for (BlockPos farmOrigin : control.farmContainerOrigins(world)) {
            nearest = nearerToVillager(nearest, findNearestPlantingContainer(world, farmOrigin, cropMask));
        }
        return nearest;
    }

    private BlockPos nearerToVillager(BlockPos current, BlockPos candidate) {
        if (candidate == null) {
            return current;
        }
        if (current == null) {
            return candidate;
        }
        BlockPos villagerPos = control.villager().blockPosition();
        return squaredDistance(villagerPos, candidate) < squaredDistance(villagerPos, current)
                ? candidate
                : current;
    }

    private BlockPos findNearestPlantingContainer(ServerLevel world, BlockPos origin, int cropMask) {
        return findNearest(world, origin, control.chestRadius(), 4, pos -> {
            BlockState state = world.getBlockState(pos);
            if (!state.is(Blocks.CHEST) && !state.is(Blocks.TRAPPED_CHEST) && !state.is(Blocks.BARREL)) {
                return false;
            }
            Container inventory = ValetInventoryTransfer.getContainerInventory(world, pos);
            return inventory != null && containsFarmPlantingItem(inventory, cropMask);
        });
    }

    private static boolean containsFarmPlantingItem(Container inventory, int cropMask) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (isFarmPlantingItem(inventory.getItem(slot), cropMask)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasOnlyFarmPlantingItems(Container inventory, int cropMask) {
        boolean foundPlantingItem = false;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty() || stack.is(Items.ARROW)) {
                continue;
            }
            if (!isFarmPlantingItem(stack, cropMask)) {
                return false;
            }
            foundPlantingItem = true;
        }
        return foundPlantingItem;
    }

    private static boolean isFarmPlantingItem(ItemStack stack, int cropMask) {
        return !stack.isEmpty() && (ValetFarmCrop.WHEAT.isEnabled(cropMask) && stack.is(Items.WHEAT_SEEDS)
                || ValetFarmCrop.CARROT.isEnabled(cropMask) && stack.is(Items.CARROT)
                || ValetFarmCrop.POTATO.isEnabled(cropMask) && stack.is(Items.POTATO)
                || ValetFarmCrop.BEETROOT.isEnabled(cropMask) && stack.is(Items.BEETROOT_SEEDS)
                || ValetFarmCrop.NETHER_WART.isEnabled(cropMask) && stack.is(Items.NETHER_WART));
    }

    private BlockPos findNearest(ServerLevel world, BlockPos origin, int horizontalRadius, int verticalRadius, BlockPredicate predicate) {
        BlockPos nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (BlockPos pos : BlockPos.withinManhattan(origin, horizontalRadius, verticalRadius, horizontalRadius)) {
            BlockPos immutable = pos.immutable();
            if (control.isWithinWorkZone(world, immutable) && predicate.test(immutable)) {
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

        boolean isRecallActive(ServerLevel world);

        int requestedFarmPlantingCropMask();

        void deferFarmPlantingRequest();

        List<BlockPos> farmContainerOrigins(ServerLevel world);

        int getUsableInventorySlots(Container inventory);

        boolean isNearAnchor(ServerLevel world, BlockPos workOrigin);

        Set<BlockPos> findStandGoals(ServerLevel world, BlockPos targetBlock, PathPurpose purpose);

        List<BlockPos> planPathToAdjacent(ServerLevel world, PathPurpose purpose, BlockPos targetBlock, Set<BlockPos> goals);

        void startPath(PathPurpose purpose, List<BlockPos> path);

        void clearPathState();

        void clearMiningState();

        int chestRadius();

        boolean isWithinWorkZone(ServerLevel world, BlockPos pos);

        void animateChestUse(ServerLevel world, BlockPos pos);

        void setState(State state);

        void setDelayTicks(int ticks);
    }
}
