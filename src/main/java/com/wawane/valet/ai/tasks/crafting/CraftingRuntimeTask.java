package com.wawane.valet.ai.tasks.crafting;

import com.wawane.valet.ValetDebug;
import com.wawane.valet.ai.ValetStateMachine.PathPurpose;
import com.wawane.valet.ai.ValetStateMachine.State;
import com.wawane.valet.ai.inventory.ValetInventoryTransfer;
import com.wawane.valet.order.ValetCraftTarget;
import com.wawane.valet.order.ValetOrder;
import com.wawane.valet.order.ValetOrders;
import com.wawane.valet.order.ValetWoodTarget;
import com.wawane.valet.progress.ValetProgress;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class CraftingRuntimeTask {
    private static final int MAX_RESOURCE_PATH_CANDIDATES = 32;
    private final Control control;
    private BlockPos targetPos;
    private BlockState targetState;
    private Action action = Action.NONE;

    public CraftingRuntimeTask(Control control) {
        this.control = control;
    }

    public void findTarget(ServerLevel world) {
        if (control.getCraftTarget() != ValetCraftTarget.STONE_PICKAXE) {
            ValetDebug.record(control.villager(), "craft no_target");
            control.setDelayTicks(40);
            return;
        }

        Container inventory = control.villager().getInventory();
        int slots = control.getUsableInventorySlots(inventory);
        restockFromNearbyCraftContainers(world);
        int cobblestone = countItem(inventory, Items.COBBLESTONE, slots);
        if (cobblestone < 3) {
            startResourceStep(world, Action.MINE_COBBLESTONE, "craft need=cobblestone have=" + cobblestone + "/3");
            return;
        }

        if (countItem(inventory, Items.STICK, slots) < 2 && !hasWoodForSticks(inventory, slots)) {
            startResourceStep(world, Action.MINE_LOG, "craft need=wood sticks="
                    + countItem(inventory, Items.STICK, slots)
                    + "/2 planks=" + countMatching(inventory, CraftingRuntimeTask::isPlank, slots)
                    + " logs=" + countMatching(inventory, CraftingRuntimeTask::isLog, slots));
            return;
        }

        startWorkstationStep(world);
    }

    public void tickCrafting(ServerLevel world) {
        if (action == Action.CRAFT_AT_WORKSTATION) {
            craftAtWorkstation(world);
            return;
        }

        if (targetPos == null || targetState == null || action == Action.NONE) {
            clearTarget();
            control.setState(State.FIND_TARGET);
            return;
        }

        if (!control.canReachTargetFromStand(targetPos, control.currentStandPos(world))) {
            ValetDebug.record(control.villager(), "craft lost_reach pos=" + ValetDebug.shortPos(targetPos));
            clearTarget();
            control.setState(State.FIND_TARGET);
            return;
        }

        BlockState current = world.getBlockState(targetPos);
        if (!matchesResource(world, targetPos, current, action) || !control.canMineCraftResource(world, targetPos, current)) {
            ValetDebug.record(control.villager(), "craft target_changed pos=" + ValetDebug.shortPos(targetPos));
            clearTarget();
            control.setState(State.FIND_TARGET);
            return;
        }

        List<ItemStack> drops = Block.getDrops(current, world, targetPos, world.getBlockEntity(targetPos), control.villager(), control.getToolForBlock(current));
        if (!control.canStoreAllDrops(drops)) {
            ValetDebug.record(control.villager(), "craft drops_no_space pos=" + ValetDebug.shortPos(targetPos));
            control.setState(State.RETURNING);
            clearTarget();
            return;
        }

        control.villager().swing(InteractionHand.MAIN_HAND);
        control.animateMining(world, targetPos, current);
        world.destroyBlock(targetPos, false, control.villager());
        control.collectDrops(drops);
        ValetProgress.addXp(control.villager(), action == Action.MINE_LOG ? 3 : 2);
        ValetDebug.record(control.villager(), "craft gathered action=" + action + " pos=" + ValetDebug.shortPos(targetPos));
        clearTarget();
        control.setState(State.FIND_TARGET);
        control.setDelayTicks(control.actionDelayTicks());
    }

    public void clearTarget() {
        targetPos = null;
        targetState = null;
        action = Action.NONE;
    }

    public String debugSummary() {
        return "craft=" + action + " target=" + shortPos(targetPos);
    }

    private void startResourceStep(ServerLevel world, Action nextAction, String needDebug) {
        BlockPos workOrigin = control.getWorkOrigin(world);
        if (workOrigin == null) {
            ValetDebug.record(control.villager(), "craft no_workstation");
            control.setDelayTicks(40);
            return;
        }

        if (!control.hasInventorySpace()) {
            ValetDebug.record(control.villager(), needDebug + " no_space");
            clearTarget();
            control.setState(State.RETURNING);
            control.setDelayTicks(4);
            return;
        }

        ResourceTarget resource = findReachableResource(world, workOrigin, nextAction);
        if (resource == null) {
            ValetDebug.record(control.villager(), needDebug + " no_reachable_resource");
            control.setDelayTicks(control.noTargetDelayTicks());
            return;
        }

        targetPos = resource.pos();
        targetState = resource.state();
        action = nextAction;
        if (resource.path().isEmpty()) {
            control.setState(State.CRAFTING);
            return;
        }

        control.startPath(PathPurpose.CRAFT, resource.path());
    }

    private void startWorkstationStep(ServerLevel world) {
        BlockPos workOrigin = control.getWorkOrigin(world);
        if (workOrigin == null) {
            ValetDebug.record(control.villager(), "craft no_workstation");
            control.setDelayTicks(40);
            return;
        }

        targetPos = workOrigin;
        targetState = world.getBlockState(workOrigin);
        action = Action.CRAFT_AT_WORKSTATION;
        Set<BlockPos> goals = control.findStandGoals(world, workOrigin, PathPurpose.CRAFT);
        if (goals.contains(control.currentStandPos(world))) {
            control.setState(State.CRAFTING);
            return;
        }

        List<BlockPos> path = control.planPathToAdjacent(world, PathPurpose.CRAFT, workOrigin, goals);
        if (path.isEmpty()) {
            ValetDebug.record(control.villager(), "craft no_workstation_path pos=" + ValetDebug.shortPos(workOrigin));
            clearTarget();
            control.clearPathState();
            control.setState(State.RETURNING_HOME);
            control.setDelayTicks(4);
            return;
        }
        control.startPath(PathPurpose.CRAFT, path);
    }

    private void craftAtWorkstation(ServerLevel world) {
        BlockPos workOrigin = control.getWorkOrigin(world);
        if (workOrigin == null || !control.canReachTargetFromStand(workOrigin, control.currentStandPos(world))) {
            ValetDebug.record(control.villager(), "craft lost_workstation");
            clearTarget();
            control.setState(State.FIND_TARGET);
            return;
        }

        Container inventory = control.villager().getInventory();
        int slots = control.getUsableInventorySlots(inventory);
        craftPlanksFromLogs(inventory, slots);
        craftSticksFromPlanks(inventory, slots);

        if (countItem(inventory, Items.COBBLESTONE, slots) < 3 || countItem(inventory, Items.STICK, slots) < 2) {
            clearTarget();
            control.setState(State.FIND_TARGET);
            return;
        }

        consumeItem(inventory, Items.COBBLESTONE, 3, slots);
        consumeItem(inventory, Items.STICK, 2, slots);
        ValetInventoryTransfer.insertStack(inventory, new ItemStack(Items.STONE_PICKAXE), slots);
        inventory.setChanged();
        control.villager().swing(InteractionHand.MAIN_HAND);
        control.villager().getLookControl().setLookAt(workOrigin.getX() + 0.5D, workOrigin.getY() + 0.5D, workOrigin.getZ() + 0.5D);
        world.playSound(null, workOrigin, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 0.7F, 1.0F);
        ValetProgress.addXp(control.villager(), 12);
        ValetOrders.set(control.villager(), ValetOrder.NONE);
        ValetDebug.record(control.villager(), "craft done target=stone_pickaxe");
        clearTarget();
        control.clearPathState();
        control.setState(control.hasInventoryItems() ? State.RETURNING : State.RETURNING_HOME);
        control.setDelayTicks(control.actionDelayTicks());
    }

    private ResourceTarget findReachableResource(ServerLevel world, BlockPos origin, Action resourceAction) {
        BlockPos currentStand = control.currentStandPos(world);
        List<ResourceCandidate> candidates = new ArrayList<>();
        for (BlockPos pos : BlockPos.withinManhattan(origin, control.mineRadius(), control.mineVerticalRadius(), control.mineRadius())) {
            BlockPos immutable = pos.immutable();
            BlockState blockState = world.getBlockState(immutable);
            if (!matchesResource(world, immutable, blockState, resourceAction) || !control.canMineCraftResource(world, immutable, blockState)) {
                continue;
            }

            if (!currentStand.below().equals(immutable) && control.canReachTargetFromStand(immutable, currentStand)) {
                return new ResourceTarget(immutable, blockState, List.of());
            }

            candidates.add(new ResourceCandidate(
                    immutable,
                    blockState,
                    squaredDistance(currentStand, immutable),
                    squaredDistance(origin, immutable)
            ));
        }

        candidates.sort(Comparator
                .comparingDouble(ResourceCandidate::distanceFromCurrent)
                .thenComparingDouble(ResourceCandidate::distanceFromOrigin));

        int attempts = Math.min(candidates.size(), MAX_RESOURCE_PATH_CANDIDATES);
        for (int index = 0; index < attempts; index++) {
            ResourceCandidate candidate = candidates.get(index);
            Set<BlockPos> goals = control.findStandGoals(world, candidate.pos(), PathPurpose.CRAFT);
            if (goals.isEmpty()) {
                continue;
            }

            List<BlockPos> path = control.planPathToAdjacent(world, PathPurpose.CRAFT, candidate.pos(), goals);
            if (!path.isEmpty()) {
                return new ResourceTarget(candidate.pos(), candidate.state(), path);
            }
        }

        ValetDebug.record(control.villager(), "craft no_resource_path action=" + resourceAction + " candidates=" + candidates.size() + " attempts=" + attempts);
        return null;
    }

    private void restockFromNearbyCraftContainers(ServerLevel world) {
        Container target = control.villager().getInventory();
        int targetSlots = control.getUsableInventorySlots(target);
        if (!needsCraftMaterials(target, targetSlots)) {
            return;
        }

        Set<BlockPos> containers = new LinkedHashSet<>();
        collectCraftContainers(world, control.villager().blockPosition(), containers);
        collectCraftContainers(world, control.getWorkOrigin(world), containers);

        for (BlockPos containerPos : containers) {
            Container source = ValetInventoryTransfer.getContainerInventory(world, containerPos);
            if (source == null) {
                continue;
            }

            int moved = takeCraftMaterialsFromContainer(source, target, targetSlots);
            if (moved > 0) {
                control.animateChestUse(world, containerPos);
                ValetDebug.record(control.villager(), "craft took_items count=" + moved + " chest=" + ValetDebug.shortPos(containerPos));
            }
            if (!needsCraftMaterials(target, targetSlots)) {
                return;
            }
        }
    }

    private void collectCraftContainers(ServerLevel world, BlockPos origin, Set<BlockPos> containers) {
        if (origin == null) {
            return;
        }

        int radius = control.materialRadius();
        for (BlockPos pos : BlockPos.withinManhattan(origin, radius, 8, radius)) {
            BlockPos immutable = pos.immutable();
            if (isCraftContainer(world, immutable)) {
                containers.add(immutable);
            }
        }
    }

    private boolean isCraftContainer(ServerLevel world, BlockPos pos) {
        BlockState blockState = world.getBlockState(pos);
        if (!blockState.is(Blocks.CHEST) && !blockState.is(Blocks.TRAPPED_CHEST) && !blockState.is(Blocks.BARREL)) {
            return false;
        }

        Container inventory = ValetInventoryTransfer.getContainerInventory(world, pos);
        return inventory != null && hasCraftMaterial(inventory, inventory.getContainerSize());
    }

    private boolean hasCraftMaterial(Container inventory, int slots) {
        return countItem(inventory, Items.COBBLESTONE, slots) > 0
                || countItem(inventory, Items.STICK, slots) > 0
                || countMatching(inventory, CraftingRuntimeTask::isPlank, slots) > 0
                || countMatching(inventory, CraftingRuntimeTask::isLog, slots) > 0;
    }

    private int takeCraftMaterialsFromContainer(Container source, Container target, int targetSlots) {
        int moved = 0;
        moved += takeMatchingFromContainer(source, target, targetSlots, stack -> stack.is(Items.COBBLESTONE), Math.max(0, 3 - countItem(target, Items.COBBLESTONE, targetSlots)));
        moved += takeMatchingFromContainer(source, target, targetSlots, stack -> stack.is(Items.STICK), Math.max(0, 2 - countItem(target, Items.STICK, targetSlots)));

        if (countItem(target, Items.STICK, targetSlots) < 2 && !hasWoodForSticks(target, targetSlots)) {
            moved += takeMatchingFromContainer(source, target, targetSlots, CraftingRuntimeTask::isPlank, 2);
        }
        if (countItem(target, Items.STICK, targetSlots) < 2 && !hasWoodForSticks(target, targetSlots)) {
            moved += takeMatchingFromContainer(source, target, targetSlots, CraftingRuntimeTask::isLog, 1);
        }

        if (moved > 0) {
            source.setChanged();
            target.setChanged();
        }
        return moved;
    }

    private int takeMatchingFromContainer(Container source, Container target, int targetSlots, Predicate<ItemStack> predicate, int amount) {
        int movedTotal = 0;
        for (int slot = 0; slot < source.getContainerSize() && movedTotal < amount; slot++) {
            ItemStack sourceStack = source.getItem(slot);
            if (sourceStack.isEmpty() || !predicate.test(sourceStack)) {
                continue;
            }

            int requested = Math.min(sourceStack.getCount(), amount - movedTotal);
            ItemStack moving = sourceStack.copy();
            moving.setCount(requested);
            ValetInventoryTransfer.insertStack(target, moving, targetSlots);
            int moved = requested - moving.getCount();
            if (moved <= 0) {
                break;
            }

            sourceStack.shrink(moved);
            if (sourceStack.isEmpty()) {
                source.setItem(slot, ItemStack.EMPTY);
            }
            movedTotal += moved;
        }
        return movedTotal;
    }

    private boolean needsCraftMaterials(Container inventory, int slots) {
        return countItem(inventory, Items.COBBLESTONE, slots) < 3
                || countItem(inventory, Items.STICK, slots) < 2 && !hasWoodForSticks(inventory, slots);
    }

    private boolean matchesResource(ServerLevel world, BlockPos pos, BlockState blockState, Action resourceAction) {
        return switch (resourceAction) {
            case MINE_COBBLESTONE -> blockState.is(Blocks.STONE);
            case MINE_LOG -> {
                ValetWoodTarget target = ValetWoodTarget.fromState(blockState);
                yield target != null && target.matchesNaturalTree(world, pos);
            }
            case NONE, CRAFT_AT_WORKSTATION -> false;
        };
    }

    private boolean hasWoodForSticks(Container inventory, int slots) {
        return countItem(inventory, Items.STICK, slots) >= 2
                || countMatching(inventory, CraftingRuntimeTask::isPlank, slots) >= 2
                || countMatching(inventory, CraftingRuntimeTask::isLog, slots) > 0;
    }

    private void craftPlanksFromLogs(Container inventory, int slots) {
        while (countItem(inventory, Items.STICK, slots) < 2
                && countMatching(inventory, CraftingRuntimeTask::isPlank, slots) < 2
                && consumeMatching(inventory, CraftingRuntimeTask::isLog, 1, slots) == 1) {
            ValetInventoryTransfer.insertStack(inventory, new ItemStack(Blocks.OAK_PLANKS, 4), slots);
        }
    }

    private void craftSticksFromPlanks(Container inventory, int slots) {
        while (countItem(inventory, Items.STICK, slots) < 2
                && consumeMatching(inventory, CraftingRuntimeTask::isPlank, 2, slots) == 2) {
            ValetInventoryTransfer.insertStack(inventory, new ItemStack(Items.STICK, 4), slots);
        }
    }

    private int countItem(Container inventory, Item item, int slots) {
        return countMatching(inventory, stack -> stack.is(item), slots);
    }

    private static boolean isPlank(ItemStack stack) {
        return stack.is(ItemTags.PLANKS)
                || stack.getItem() instanceof BlockItem blockItem
                && blockItem.getBlock().defaultBlockState().is(BlockTags.PLANKS);
    }

    private static boolean isLog(ItemStack stack) {
        return stack.is(ItemTags.LOGS)
                || stack.getItem() instanceof BlockItem blockItem
                && blockItem.getBlock().defaultBlockState().is(BlockTags.LOGS);
    }

    private int countMatching(Container inventory, Predicate<ItemStack> predicate, int slots) {
        int total = 0;
        for (int slot = 0; slot < Math.min(slots, inventory.getContainerSize()); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty() && predicate.test(stack)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private void consumeItem(Container inventory, Item item, int amount, int slots) {
        consumeMatching(inventory, stack -> stack.is(item), amount, slots);
    }

    private int consumeMatching(Container inventory, Predicate<ItemStack> predicate, int amount, int slots) {
        int consumed = 0;
        for (int slot = 0; slot < Math.min(slots, inventory.getContainerSize()) && consumed < amount; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty() || !predicate.test(stack)) {
                continue;
            }

            int take = Math.min(amount - consumed, stack.getCount());
            stack.shrink(take);
            consumed += take;
            if (stack.isEmpty()) {
                inventory.setItem(slot, ItemStack.EMPTY);
            }
        }
        inventory.setChanged();
        return consumed;
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

    private enum Action {
        NONE,
        MINE_COBBLESTONE,
        MINE_LOG,
        CRAFT_AT_WORKSTATION
    }

    private record ResourceTarget(BlockPos pos, BlockState state, List<BlockPos> path) {
    }

    private record ResourceCandidate(BlockPos pos, BlockState state, double distanceFromCurrent, double distanceFromOrigin) {
    }

    public interface Control {
        Villager villager();

        ValetCraftTarget getCraftTarget();

        BlockPos getWorkOrigin(ServerLevel world);

        BlockPos currentStandPos(ServerLevel world);

        Set<BlockPos> findStandGoals(ServerLevel world, BlockPos targetBlock, PathPurpose purpose);

        List<BlockPos> planPathToAdjacent(ServerLevel world, PathPurpose purpose, BlockPos targetBlock, Set<BlockPos> goals);

        void startPath(PathPurpose purpose, List<BlockPos> path);

        void clearPathState();

        boolean canReachTargetFromStand(BlockPos targetBlock, BlockPos stand);

        boolean canMineCraftResource(ServerLevel world, BlockPos pos, BlockState blockState);

        ItemStack getToolForBlock(BlockState blockState);

        boolean canStoreAllDrops(List<ItemStack> drops);

        void collectDrops(List<ItemStack> drops);

        void animateMining(ServerLevel world, BlockPos miningPos, BlockState miningState);

        boolean hasInventoryItems();

        boolean hasInventorySpace();

        int getUsableInventorySlots(Container inventory);

        int actionDelayTicks();

        int noTargetDelayTicks();

        int mineRadius();

        int mineVerticalRadius();

        int materialRadius();

        void animateChestUse(ServerLevel world, BlockPos pos);

        void setState(State state);

        void setDelayTicks(int ticks);
    }
}
