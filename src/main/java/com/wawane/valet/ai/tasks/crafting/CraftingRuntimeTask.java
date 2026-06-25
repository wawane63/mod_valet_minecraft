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
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public final class CraftingRuntimeTask {
    private final Control control;
    private BlockPos targetPos;
    private BlockState targetState;
    private Action action = Action.NONE;

    public CraftingRuntimeTask(Control control) {
        this.control = control;
    }

    public void findTarget(ServerWorld world) {
        if (control.getCraftTarget() != ValetCraftTarget.STONE_PICKAXE) {
            ValetDebug.record(control.villager(), "craft no_target");
            control.setDelayTicks(40);
            return;
        }

        Inventory inventory = control.villager().getInventory();
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
                    + "/2 planks=" + countMatching(inventory, stack -> stack.isIn(ItemTags.PLANKS), slots)
                    + " logs=" + countMatching(inventory, stack -> stack.isIn(ItemTags.LOGS), slots));
            return;
        }

        startWorkstationStep(world);
    }

    public void tickCrafting(ServerWorld world) {
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

        List<ItemStack> drops = Block.getDroppedStacks(current, world, targetPos, world.getBlockEntity(targetPos), control.villager(), control.getToolForBlock(current));
        if (!control.canStoreAllDrops(drops)) {
            ValetDebug.record(control.villager(), "craft drops_no_space pos=" + ValetDebug.shortPos(targetPos));
            control.setState(State.RETURNING);
            clearTarget();
            return;
        }

        control.villager().swingHand(Hand.MAIN_HAND);
        control.animateMining(world, targetPos, current);
        world.breakBlock(targetPos, false, control.villager());
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

    private void startResourceStep(ServerWorld world, Action nextAction, String needDebug) {
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

    private void startWorkstationStep(ServerWorld world) {
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
            control.setDelayTicks(20);
            return;
        }
        control.startPath(PathPurpose.CRAFT, path);
    }

    private void craftAtWorkstation(ServerWorld world) {
        BlockPos workOrigin = control.getWorkOrigin(world);
        if (workOrigin == null || !control.canReachTargetFromStand(workOrigin, control.currentStandPos(world))) {
            ValetDebug.record(control.villager(), "craft lost_workstation");
            clearTarget();
            control.setState(State.FIND_TARGET);
            return;
        }

        Inventory inventory = control.villager().getInventory();
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
        inventory.markDirty();
        control.villager().swingHand(Hand.MAIN_HAND);
        control.villager().getLookControl().lookAt(workOrigin.getX() + 0.5D, workOrigin.getY() + 0.5D, workOrigin.getZ() + 0.5D);
        world.playSound(null, workOrigin, SoundEvents.BLOCK_WOOD_PLACE, SoundCategory.BLOCKS, 0.7F, 1.0F);
        ValetProgress.addXp(control.villager(), 12);
        ValetOrders.set(control.villager(), ValetOrder.NONE);
        ValetDebug.record(control.villager(), "craft done target=stone_pickaxe");
        clearTarget();
        control.clearPathState();
        control.setState(control.hasInventoryItems() ? State.RETURNING : State.RETURNING_HOME);
        control.setDelayTicks(control.actionDelayTicks());
    }

    private ResourceTarget findReachableResource(ServerWorld world, BlockPos origin, Action resourceAction) {
        ResourceTarget nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        BlockPos currentStand = control.currentStandPos(world);
        for (BlockPos pos : BlockPos.iterateOutwards(origin, control.mineRadius(), control.mineVerticalRadius(), control.mineRadius())) {
            BlockPos immutable = pos.toImmutable();
            BlockState blockState = world.getBlockState(immutable);
            if (!matchesResource(world, immutable, blockState, resourceAction) || !control.canMineCraftResource(world, immutable, blockState)) {
                continue;
            }

            Set<BlockPos> goals = control.findStandGoals(world, immutable, PathPurpose.CRAFT);
            List<BlockPos> path = List.of();
            if (!goals.contains(currentStand)) {
                path = control.planPathToAdjacent(world, PathPurpose.CRAFT, immutable, goals);
                if (path.isEmpty()) {
                    continue;
                }
            }

            double distance = squaredDistance(origin, immutable);
            if (distance < nearestDistance) {
                nearest = new ResourceTarget(immutable, blockState, path);
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    private void restockFromNearbyCraftContainers(ServerWorld world) {
        Inventory target = control.villager().getInventory();
        int targetSlots = control.getUsableInventorySlots(target);
        if (!needsCraftMaterials(target, targetSlots)) {
            return;
        }

        Set<BlockPos> containers = new LinkedHashSet<>();
        collectCraftContainers(world, control.villager().getBlockPos(), containers);
        collectCraftContainers(world, control.getWorkOrigin(world), containers);

        for (BlockPos containerPos : containers) {
            Inventory source = ValetInventoryTransfer.getContainerInventory(world, containerPos);
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

    private void collectCraftContainers(ServerWorld world, BlockPos origin, Set<BlockPos> containers) {
        if (origin == null) {
            return;
        }

        int radius = control.materialRadius();
        for (BlockPos pos : BlockPos.iterateOutwards(origin, radius, 8, radius)) {
            BlockPos immutable = pos.toImmutable();
            if (isCraftContainer(world, immutable)) {
                containers.add(immutable);
            }
        }
    }

    private boolean isCraftContainer(ServerWorld world, BlockPos pos) {
        BlockState blockState = world.getBlockState(pos);
        if (!blockState.isOf(Blocks.CHEST) && !blockState.isOf(Blocks.TRAPPED_CHEST) && !blockState.isOf(Blocks.BARREL)) {
            return false;
        }

        Inventory inventory = ValetInventoryTransfer.getContainerInventory(world, pos);
        return inventory != null && hasCraftMaterial(inventory, inventory.size());
    }

    private boolean hasCraftMaterial(Inventory inventory, int slots) {
        return countItem(inventory, Items.COBBLESTONE, slots) > 0
                || countItem(inventory, Items.STICK, slots) > 0
                || countMatching(inventory, stack -> stack.isIn(ItemTags.PLANKS), slots) > 0
                || countMatching(inventory, stack -> stack.isIn(ItemTags.LOGS), slots) > 0;
    }

    private int takeCraftMaterialsFromContainer(Inventory source, Inventory target, int targetSlots) {
        int moved = 0;
        moved += takeMatchingFromContainer(source, target, targetSlots, stack -> stack.isOf(Items.COBBLESTONE), Math.max(0, 3 - countItem(target, Items.COBBLESTONE, targetSlots)));
        moved += takeMatchingFromContainer(source, target, targetSlots, stack -> stack.isOf(Items.STICK), Math.max(0, 2 - countItem(target, Items.STICK, targetSlots)));

        if (countItem(target, Items.STICK, targetSlots) < 2 && !hasWoodForSticks(target, targetSlots)) {
            moved += takeMatchingFromContainer(source, target, targetSlots, stack -> stack.isIn(ItemTags.PLANKS), 2);
        }
        if (countItem(target, Items.STICK, targetSlots) < 2 && !hasWoodForSticks(target, targetSlots)) {
            moved += takeMatchingFromContainer(source, target, targetSlots, stack -> stack.isIn(ItemTags.LOGS), 1);
        }

        if (moved > 0) {
            source.markDirty();
            target.markDirty();
        }
        return moved;
    }

    private int takeMatchingFromContainer(Inventory source, Inventory target, int targetSlots, Predicate<ItemStack> predicate, int amount) {
        int movedTotal = 0;
        for (int slot = 0; slot < source.size() && movedTotal < amount; slot++) {
            ItemStack sourceStack = source.getStack(slot);
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

            sourceStack.decrement(moved);
            if (sourceStack.isEmpty()) {
                source.setStack(slot, ItemStack.EMPTY);
            }
            movedTotal += moved;
        }
        return movedTotal;
    }

    private boolean needsCraftMaterials(Inventory inventory, int slots) {
        return countItem(inventory, Items.COBBLESTONE, slots) < 3
                || countItem(inventory, Items.STICK, slots) < 2 && !hasWoodForSticks(inventory, slots);
    }

    private boolean matchesResource(ServerWorld world, BlockPos pos, BlockState blockState, Action resourceAction) {
        return switch (resourceAction) {
            case MINE_COBBLESTONE -> blockState.isOf(Blocks.STONE);
            case MINE_LOG -> {
                ValetWoodTarget target = ValetWoodTarget.fromState(blockState);
                yield target != null && target.matchesNaturalTree(world, pos);
            }
            case NONE, CRAFT_AT_WORKSTATION -> false;
        };
    }

    private boolean hasWoodForSticks(Inventory inventory, int slots) {
        return countItem(inventory, Items.STICK, slots) >= 2
                || countMatching(inventory, stack -> stack.isIn(ItemTags.PLANKS), slots) >= 2
                || countMatching(inventory, stack -> stack.isIn(ItemTags.LOGS), slots) > 0;
    }

    private void craftPlanksFromLogs(Inventory inventory, int slots) {
        while (countItem(inventory, Items.STICK, slots) < 2
                && countMatching(inventory, stack -> stack.isIn(ItemTags.PLANKS), slots) < 2
                && consumeMatching(inventory, stack -> stack.isIn(ItemTags.LOGS), 1, slots) == 1) {
            ValetInventoryTransfer.insertStack(inventory, new ItemStack(Blocks.OAK_PLANKS, 4), slots);
        }
    }

    private void craftSticksFromPlanks(Inventory inventory, int slots) {
        while (countItem(inventory, Items.STICK, slots) < 2
                && consumeMatching(inventory, stack -> stack.isIn(ItemTags.PLANKS), 2, slots) == 2) {
            ValetInventoryTransfer.insertStack(inventory, new ItemStack(Items.STICK, 4), slots);
        }
    }

    private int countItem(Inventory inventory, Item item, int slots) {
        return countMatching(inventory, stack -> stack.isOf(item), slots);
    }

    private int countMatching(Inventory inventory, Predicate<ItemStack> predicate, int slots) {
        int total = 0;
        for (int slot = 0; slot < Math.min(slots, inventory.size()); slot++) {
            ItemStack stack = inventory.getStack(slot);
            if (!stack.isEmpty() && predicate.test(stack)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private void consumeItem(Inventory inventory, Item item, int amount, int slots) {
        consumeMatching(inventory, stack -> stack.isOf(item), amount, slots);
    }

    private int consumeMatching(Inventory inventory, Predicate<ItemStack> predicate, int amount, int slots) {
        int consumed = 0;
        for (int slot = 0; slot < Math.min(slots, inventory.size()) && consumed < amount; slot++) {
            ItemStack stack = inventory.getStack(slot);
            if (stack.isEmpty() || !predicate.test(stack)) {
                continue;
            }

            int take = Math.min(amount - consumed, stack.getCount());
            stack.decrement(take);
            consumed += take;
            if (stack.isEmpty()) {
                inventory.setStack(slot, ItemStack.EMPTY);
            }
        }
        inventory.markDirty();
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

    public interface Control {
        VillagerEntity villager();

        ValetCraftTarget getCraftTarget();

        BlockPos getWorkOrigin(ServerWorld world);

        BlockPos currentStandPos(ServerWorld world);

        Set<BlockPos> findStandGoals(ServerWorld world, BlockPos targetBlock, PathPurpose purpose);

        List<BlockPos> planPathToAdjacent(ServerWorld world, PathPurpose purpose, BlockPos targetBlock, Set<BlockPos> goals);

        void startPath(PathPurpose purpose, List<BlockPos> path);

        void clearPathState();

        boolean canReachTargetFromStand(BlockPos targetBlock, BlockPos stand);

        boolean canMineCraftResource(ServerWorld world, BlockPos pos, BlockState blockState);

        ItemStack getToolForBlock(BlockState blockState);

        boolean canStoreAllDrops(List<ItemStack> drops);

        void collectDrops(List<ItemStack> drops);

        void animateMining(ServerWorld world, BlockPos miningPos, BlockState miningState);

        boolean hasInventoryItems();

        boolean hasInventorySpace();

        int getUsableInventorySlots(Inventory inventory);

        int actionDelayTicks();

        int noTargetDelayTicks();

        int mineRadius();

        int mineVerticalRadius();

        int materialRadius();

        void animateChestUse(ServerWorld world, BlockPos pos);

        void setState(State state);

        void setDelayTicks(int ticks);
    }
}
