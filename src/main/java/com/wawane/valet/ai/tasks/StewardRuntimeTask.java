package com.wawane.valet.ai.tasks;

import com.wawane.valet.ValetDebug;
import com.wawane.valet.ai.ValetStateMachine.PathPurpose;
import com.wawane.valet.ai.ValetStateMachine.State;
import com.wawane.valet.ai.inventory.ValetInventoryTransfer;
import com.wawane.valet.progress.ValetProgress;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Deplace des piles entre coffres/barils proches. Les 9 premiers slots d'un
 * conteneur filtrent les items acceptes et restent reserves au joueur.
 */
public final class StewardRuntimeTask {
    private static final int FILTER_SLOT_COUNT = 9;
    private static final int TRANSFER_BATCH = 16;
    private static final int FILTER_PRIORITY_WEIGHT = 10_000;

    private final Control control;
    private BlockPos sourcePos;
    private BlockPos targetPos;
    private ItemStack plannedStack = ItemStack.EMPTY;
    private int targetStartSlot;
    private Action action = Action.NONE;

    public StewardRuntimeTask(Control control) {
        this.control = control;
    }

    public void findTarget(ServerLevel world) {
        BlockPos workOrigin = control.getWorkOrigin(world);
        if (workOrigin == null) {
            control.setDelayTicks(40);
            return;
        }

        ItemStack carried = firstCarriedStack();
        if (!carried.isEmpty()) {
            Destination destination = findDestinationForCarried(world, workOrigin, carried);
            if (destination == null) {
                ValetDebug.record(control.villager(), "steward no_destination carried=" + itemId(carried));
                control.setDelayTicks(40);
                return;
            }
            plannedStack = carried.copyWithCount(1);
            targetPos = destination.ref().pos();
            targetStartSlot = destination.startSlot();
            startTarget(world, targetPos, Action.DEPOSIT_TO_TARGET);
            return;
        }

        TransferPlan plan = findTransferPlan(world, workOrigin);
        if (plan == null) {
            clearTarget();
            control.setState(State.RETURNING_HOME);
            control.setDelayTicks(control.noTargetDelayTicks());
            return;
        }

        sourcePos = plan.source().pos();
        targetPos = plan.destination().ref().pos();
        targetStartSlot = plan.destination().startSlot();
        plannedStack = plan.stack().copyWithCount(1);
        startTarget(world, sourcePos, Action.TAKE_FROM_SOURCE);
    }

    public void completePath() {
        control.setState(State.STEWARDING);
    }

    public void tickStewarding(ServerLevel world) {
        BlockPos activeTarget = action == Action.TAKE_FROM_SOURCE ? sourcePos : targetPos;
        if (activeTarget == null || plannedStack.isEmpty() || action == Action.NONE) {
            clearTarget();
            control.setState(State.FIND_TARGET);
            return;
        }

        if (!control.canReachTargetFromStand(activeTarget, control.currentStandPos(world))) {
            ValetDebug.record(control.villager(), "steward lost_reach action=" + action + " pos=" + ValetDebug.shortPos(activeTarget));
            clearTarget();
            control.setState(State.FIND_TARGET);
            return;
        }

        switch (action) {
            case TAKE_FROM_SOURCE -> takeFromSource(world);
            case DEPOSIT_TO_TARGET -> depositToTarget(world);
            case NONE -> control.setState(State.FIND_TARGET);
        }
    }

    public Item getDisplayItem() {
        return action == Action.TAKE_FROM_SOURCE ? Items.HOPPER : Items.CHEST;
    }

    public void clearTarget() {
        sourcePos = null;
        targetPos = null;
        plannedStack = ItemStack.EMPTY;
        targetStartSlot = 0;
        action = Action.NONE;
    }

    public void clearAll() {
        clearTarget();
    }

    public String debugSummary() {
        return "steward=" + action
                + " source=" + shortPos(sourcePos)
                + " target=" + shortPos(targetPos);
    }

    private void startTarget(ServerLevel world, BlockPos pos, Action nextAction) {
        action = nextAction;
        Set<BlockPos> goals = control.findStandGoals(world, pos, PathPurpose.STEWARD);
        if (goals.contains(control.currentStandPos(world))) {
            control.setState(State.STEWARDING);
            return;
        }

        List<BlockPos> path = control.planPathToAdjacent(world, PathPurpose.STEWARD, pos, goals);
        if (path.isEmpty()) {
            ValetDebug.record(control.villager(), "steward no_path action=" + nextAction + " pos=" + ValetDebug.shortPos(pos));
            clearTarget();
            control.setDelayTicks(20);
            return;
        }
        control.startPath(PathPurpose.STEWARD, path);
    }

    private void takeFromSource(ServerLevel world) {
        Container source = ValetInventoryTransfer.getContainerInventory(world, sourcePos);
        if (source == null || targetPos == null || !hasDestinationCapacity(world, targetPos, plannedStack, targetStartSlot)) {
            clearTarget();
            control.setState(State.FIND_TARGET);
            return;
        }

        ContainerRef sourceRef = new ContainerRef(sourcePos, source, hasFilters(source));
        int moved = moveFromSource(sourceRef, control.villager().getInventory(), plannedStack, control.getUsableInventorySlots(control.villager().getInventory()));
        if (moved <= 0) {
            clearTarget();
            control.setState(State.FIND_TARGET);
            control.setDelayTicks(20);
            return;
        }

        control.animateChestUse(world, sourcePos);
        ValetDebug.record(control.villager(), "steward took items=" + moved
                + " item=" + itemId(plannedStack)
                + " source=" + ValetDebug.shortPos(sourcePos));
        startTarget(world, targetPos, Action.DEPOSIT_TO_TARGET);
    }

    private void depositToTarget(ServerLevel world) {
        Container target = ValetInventoryTransfer.getContainerInventory(world, targetPos);
        if (target == null) {
            clearTarget();
            control.setState(State.FIND_TARGET);
            return;
        }

        int moved = moveFromInventoryToTarget(control.villager().getInventory(), target, plannedStack, targetStartSlot);
        if (moved > 0) {
            control.animateChestUse(world, targetPos);
            ValetProgress.addXp(control.villager(), Math.max(1, moved / 8));
            ValetDebug.record(control.villager(), "steward deposited items=" + moved
                    + " item=" + itemId(plannedStack)
                    + " target=" + ValetDebug.shortPos(targetPos));
        } else {
            ValetDebug.record(control.villager(), "steward deposit_empty target=" + ValetDebug.shortPos(targetPos));
        }

        clearTarget();
        control.setState(State.FIND_TARGET);
        control.setDelayTicks(moved > 0 ? control.actionDelayTicks() : 20);
    }

    private TransferPlan findTransferPlan(ServerLevel world, BlockPos workOrigin) {
        List<ContainerRef> containers = findContainers(world, workOrigin);
        List<TransferPlan> plans = new ArrayList<>();
        for (ContainerRef source : containers) {
            for (int slot = 0; slot < source.inventory().getContainerSize(); slot++) {
                ItemStack stack = source.inventory().getItem(slot);
                if (stack.isEmpty() || stack.is(Items.ARROW) || shouldSkipSourceSlot(source, slot, stack)) {
                    continue;
                }
                Destination destination = findBestDestination(containers, source, stack, workOrigin);
                if (destination == null) {
                    continue;
                }
                int score = destination.priority() * FILTER_PRIORITY_WEIGHT
                        + squaredDistance(workOrigin, destination.ref().pos())
                        + squaredDistance(control.villager().blockPosition(), source.pos());
                plans.add(new TransferPlan(source, destination, stack.copyWithCount(1), score));
            }
        }

        plans.sort(Comparator.comparingInt(TransferPlan::score));
        for (TransferPlan plan : plans) {
            if (hasReachableStand(world, plan.source().pos()) && hasReachableStand(world, plan.destination().ref().pos())) {
                return plan;
            }
        }
        return null;
    }

    private Destination findDestinationForCarried(ServerLevel world, BlockPos workOrigin, ItemStack carried) {
        List<ContainerRef> containers = findContainers(world, workOrigin);
        Destination destination = findBestDestination(containers, null, carried, workOrigin);
        if (destination != null && hasReachableStand(world, destination.ref().pos())) {
            return destination;
        }
        if (sourcePos == null) {
            return null;
        }
        Container source = ValetInventoryTransfer.getContainerInventory(world, sourcePos);
        if (source == null || !canInsertInto(source, carried, 0) || !hasReachableStand(world, sourcePos)) {
            return null;
        }
        return new Destination(new ContainerRef(sourcePos, source, hasFilters(source)), 0, 100);
    }

    private Destination findBestDestination(List<ContainerRef> containers, ContainerRef source, ItemStack stack, BlockPos workOrigin) {
        Destination bestFiltered = null;
        Destination bestFallback = null;
        for (ContainerRef target : containers) {
            if (source != null && target.pos().equals(source.pos())) {
                continue;
            }

            int filterPriority = filterPriority(target.inventory(), stack);
            if (filterPriority >= 0 && canInsertInto(target.inventory(), stack, filterStartSlot(target.inventory()))) {
                Destination destination = new Destination(target, filterStartSlot(target.inventory()), filterPriority);
                if (isBetterDestination(destination, bestFiltered, workOrigin)) {
                    bestFiltered = destination;
                }
                continue;
            }

            if (!target.hasFilters()
                    && containsMergeableItem(target.inventory(), stack)
                    && canInsertInto(target.inventory(), stack, 0)
                    && (source == null || isFallbackPreferred(source.pos(), target.pos(), workOrigin))) {
                Destination destination = new Destination(target, 0, 50);
                if (isBetterDestination(destination, bestFallback, workOrigin)) {
                    bestFallback = destination;
                }
            }
        }
        return bestFiltered != null ? bestFiltered : bestFallback;
    }

    private List<ContainerRef> findContainers(ServerLevel world, BlockPos workOrigin) {
        List<ContainerRef> containers = new ArrayList<>();
        for (BlockPos pos : BlockPos.withinManhattan(workOrigin, control.transferRadius(), 4, control.transferRadius())) {
            BlockPos candidate = pos.immutable();
            if (!isManagedContainer(world.getBlockState(candidate))) {
                continue;
            }
            Container inventory = ValetInventoryTransfer.getContainerInventory(world, candidate);
            if (inventory != null) {
                containers.add(new ContainerRef(candidate, inventory, hasFilters(inventory)));
            }
        }
        return containers;
    }

    private boolean hasDestinationCapacity(ServerLevel world, BlockPos pos, ItemStack stack, int startSlot) {
        Container inventory = ValetInventoryTransfer.getContainerInventory(world, pos);
        return inventory != null && canInsertInto(inventory, stack, startSlot);
    }

    private boolean hasReachableStand(ServerLevel world, BlockPos pos) {
        Set<BlockPos> goals = control.findStandGoals(world, pos, PathPurpose.STEWARD);
        return goals.contains(control.currentStandPos(world))
                || !control.planPathToAdjacent(world, PathPurpose.STEWARD, pos, goals).isEmpty();
    }

    private boolean isBetterDestination(Destination candidate, Destination current, BlockPos workOrigin) {
        if (current == null) {
            return true;
        }
        if (candidate.priority() != current.priority()) {
            return candidate.priority() < current.priority();
        }
        int candidateDistance = squaredDistance(workOrigin, candidate.ref().pos());
        int currentDistance = squaredDistance(workOrigin, current.ref().pos());
        if (candidateDistance != currentDistance) {
            return candidateDistance < currentDistance;
        }
        return comparePos(candidate.ref().pos(), current.ref().pos()) < 0;
    }

    private boolean isFallbackPreferred(BlockPos source, BlockPos target, BlockPos workOrigin) {
        int sourceDistance = squaredDistance(workOrigin, source);
        int targetDistance = squaredDistance(workOrigin, target);
        if (targetDistance != sourceDistance) {
            return targetDistance < sourceDistance;
        }
        return comparePos(target, source) < 0;
    }

    private boolean shouldSkipSourceSlot(ContainerRef source, int slot, ItemStack stack) {
        if (!source.hasFilters()) {
            return false;
        }
        if (slot < filterStartSlot(source.inventory())) {
            return true;
        }
        return filterPriority(source.inventory(), stack) >= 0;
    }

    private int moveFromSource(ContainerRef source, Container target, ItemStack planned, int targetSlots) {
        int moved = 0;
        for (int slot = 0; slot < source.inventory().getContainerSize() && moved < TRANSFER_BATCH; slot++) {
            ItemStack sourceStack = source.inventory().getItem(slot);
            if (sourceStack.isEmpty() || !sameStackKind(sourceStack, planned) || shouldSkipSourceSlot(source, slot, sourceStack)) {
                continue;
            }

            int amount = Math.min(TRANSFER_BATCH - moved, sourceStack.getCount());
            ItemStack moving = sourceStack.copyWithCount(amount);
            int before = moving.getCount();
            ValetInventoryTransfer.insertStack(target, moving, targetSlots);
            int accepted = before - moving.getCount();
            if (accepted <= 0) {
                break;
            }
            sourceStack.shrink(accepted);
            if (sourceStack.isEmpty()) {
                source.inventory().setItem(slot, ItemStack.EMPTY);
            }
            moved += accepted;
        }
        source.inventory().setChanged();
        target.setChanged();
        return moved;
    }

    private int moveFromInventoryToTarget(Container source, Container target, ItemStack planned, int startSlot) {
        int moved = 0;
        for (int slot = 0; slot < source.getContainerSize(); slot++) {
            ItemStack sourceStack = source.getItem(slot);
            if (sourceStack.isEmpty() || !sameStackKind(sourceStack, planned)) {
                continue;
            }

            ItemStack moving = sourceStack.copy();
            int before = moving.getCount();
            insertStack(target, moving, startSlot);
            int accepted = before - moving.getCount();
            if (accepted <= 0) {
                break;
            }
            sourceStack.shrink(accepted);
            if (sourceStack.isEmpty()) {
                source.setItem(slot, ItemStack.EMPTY);
            }
            moved += accepted;
        }
        source.setChanged();
        target.setChanged();
        return moved;
    }

    private ItemStack firstCarriedStack() {
        Container inventory = control.villager().getInventory();
        int slots = control.getUsableInventorySlots(inventory);
        for (int slot = 0; slot < slots; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty() && !stack.is(Items.ARROW)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static boolean isManagedContainer(BlockState state) {
        return state.is(Blocks.CHEST) || state.is(Blocks.TRAPPED_CHEST) || state.is(Blocks.BARREL);
    }

    private static boolean hasFilters(Container inventory) {
        int slots = filterStartSlot(inventory);
        for (int slot = 0; slot < slots; slot++) {
            if (!inventory.getItem(slot).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static int filterPriority(Container inventory, ItemStack stack) {
        int slots = filterStartSlot(inventory);
        for (int slot = 0; slot < slots; slot++) {
            ItemStack filter = inventory.getItem(slot);
            if (!filter.isEmpty() && stack.is(filter.getItem())) {
                return slot;
            }
        }
        return -1;
    }

    private static int filterStartSlot(Container inventory) {
        return Math.min(FILTER_SLOT_COUNT, inventory.getContainerSize());
    }

    private static boolean containsMergeableItem(Container inventory, ItemStack stack) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack current = inventory.getItem(slot);
            if (!current.isEmpty() && sameStackKind(current, stack)) {
                return true;
            }
        }
        return false;
    }

    private static boolean canInsertInto(Container inventory, ItemStack stack, int startSlot) {
        ItemStack simulated = stack.copy();
        simulateInsert(inventory, simulated, startSlot);
        return simulated.getCount() < stack.getCount();
    }

    private static int insertStack(Container inventory, ItemStack stack, int startSlot) {
        int before = stack.getCount();
        int firstSlot = Math.min(Math.max(0, startSlot), inventory.getContainerSize());
        for (int slot = firstSlot; slot < inventory.getContainerSize() && !stack.isEmpty(); slot++) {
            ItemStack current = inventory.getItem(slot);
            if (canMerge(current, stack, inventory)) {
                int limit = Math.min(current.getMaxStackSize(), inventory.getMaxStackSize());
                int amount = Math.min(stack.getCount(), limit - current.getCount());
                if (amount > 0) {
                    current.grow(amount);
                    stack.shrink(amount);
                }
            }
        }
        for (int slot = firstSlot; slot < inventory.getContainerSize() && !stack.isEmpty(); slot++) {
            if (inventory.getItem(slot).isEmpty()) {
                int amount = Math.min(stack.getCount(), Math.min(stack.getMaxStackSize(), inventory.getMaxStackSize()));
                inventory.setItem(slot, stack.split(amount));
            }
        }
        if (stack.getCount() != before) {
            inventory.setChanged();
        }
        return before - stack.getCount();
    }

    private static void simulateInsert(Container inventory, ItemStack stack, int startSlot) {
        int firstSlot = Math.min(Math.max(0, startSlot), inventory.getContainerSize());
        List<ItemStack> slots = new ArrayList<>();
        for (int slot = firstSlot; slot < inventory.getContainerSize(); slot++) {
            slots.add(inventory.getItem(slot).copy());
        }

        for (ItemStack current : slots) {
            if (canMerge(current, stack, inventory)) {
                int limit = Math.min(current.getMaxStackSize(), inventory.getMaxStackSize());
                int amount = Math.min(stack.getCount(), limit - current.getCount());
                if (amount > 0) {
                    current.grow(amount);
                    stack.shrink(amount);
                }
            }
        }
        for (int slot = 0; slot < slots.size() && !stack.isEmpty(); slot++) {
            if (slots.get(slot).isEmpty()) {
                int amount = Math.min(stack.getCount(), Math.min(stack.getMaxStackSize(), inventory.getMaxStackSize()));
                slots.set(slot, stack.split(amount));
            }
        }
    }

    private static boolean canMerge(ItemStack current, ItemStack incoming, Container inventory) {
        return !current.isEmpty()
                && sameStackKind(current, incoming)
                && current.getCount() < Math.min(current.getMaxStackSize(), inventory.getMaxStackSize());
    }

    private static boolean sameStackKind(ItemStack first, ItemStack second) {
        return ItemStack.isSameItemSameComponents(first, second);
    }

    private static String itemId(ItemStack stack) {
        return stack.getItem().getDescriptionId();
    }

    private static int squaredDistance(BlockPos first, BlockPos second) {
        int dx = first.getX() - second.getX();
        int dy = first.getY() - second.getY();
        int dz = first.getZ() - second.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    private static int comparePos(BlockPos first, BlockPos second) {
        if (first.getX() != second.getX()) {
            return Integer.compare(first.getX(), second.getX());
        }
        if (first.getY() != second.getY()) {
            return Integer.compare(first.getY(), second.getY());
        }
        return Integer.compare(first.getZ(), second.getZ());
    }

    private static String shortPos(BlockPos pos) {
        return pos == null ? "-" : ValetDebug.shortPos(pos);
    }

    private enum Action {
        NONE,
        TAKE_FROM_SOURCE,
        DEPOSIT_TO_TARGET
    }

    private record ContainerRef(BlockPos pos, Container inventory, boolean hasFilters) {
    }

    private record Destination(ContainerRef ref, int startSlot, int priority) {
    }

    private record TransferPlan(ContainerRef source, Destination destination, ItemStack stack, int score) {
    }

    public interface Control {
        Villager villager();

        BlockPos getWorkOrigin(ServerLevel world);

        BlockPos currentStandPos(ServerLevel world);

        Set<BlockPos> findStandGoals(ServerLevel world, BlockPos targetBlock, PathPurpose purpose);

        List<BlockPos> planPathToAdjacent(ServerLevel world, PathPurpose purpose, BlockPos targetBlock, Set<BlockPos> goals);

        void startPath(PathPurpose purpose, List<BlockPos> path);

        boolean canReachTargetFromStand(BlockPos targetBlock, BlockPos stand);

        int transferRadius();

        int getUsableInventorySlots(Container inventory);

        int actionDelayTicks();

        int noTargetDelayTicks();

        void animateChestUse(ServerLevel world, BlockPos pos);

        void setState(State state);

        void setDelayTicks(int ticks);
    }
}
