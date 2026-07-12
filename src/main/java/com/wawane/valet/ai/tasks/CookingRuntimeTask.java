package com.wawane.valet.ai.tasks;

import com.wawane.valet.ValetDebug;
import com.wawane.valet.ValetMod;
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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Runtime autonome du cuisinier : il recolte des cultures, prend des ingredients
 * crus dans les conteneurs proches, cuisine au poste puis laisse la logistique
 * deposer les repas termines.
 */
public final class CookingRuntimeTask {
    private static final int INGREDIENT_BATCH = 8;
    private static final List<Recipe> RECIPES = List.of(
            new Recipe(Items.WHEAT, 3, Items.BREAD),
            new Recipe(Items.POTATO, 1, Items.BAKED_POTATO),
            new Recipe(Items.BEEF, 1, Items.COOKED_BEEF),
            new Recipe(Items.PORKCHOP, 1, Items.COOKED_PORKCHOP),
            new Recipe(Items.CHICKEN, 1, Items.COOKED_CHICKEN),
            new Recipe(Items.MUTTON, 1, Items.COOKED_MUTTON),
            new Recipe(Items.RABBIT, 1, Items.COOKED_RABBIT),
            new Recipe(Items.COD, 1, Items.COOKED_COD),
            new Recipe(Items.SALMON, 1, Items.COOKED_SALMON)
    );

    private final Control control;
    private BlockPos targetPos;
    private Action action = Action.NONE;

    public CookingRuntimeTask(Control control) {
        this.control = control;
    }

    public void findTarget(ServerLevel world) {
        BlockPos workOrigin = control.getWorkOrigin(world);
        if (workOrigin == null) {
            control.setDelayTicks(40);
            return;
        }

        if (hasCookableIngredient()) {
            startTarget(world, workOrigin, Action.COOK_AT_WORKSTATION);
            return;
        }

        BlockPos container = findIngredientContainer(world, workOrigin);
        if (container != null) {
            startTarget(world, container, Action.TAKE_FROM_CONTAINER);
            return;
        }

        BlockPos crop = findMatureCrop(world, workOrigin);
        if (crop != null) {
            startTarget(world, crop, Action.HARVEST_CROP);
            return;
        }

        clearTarget();
        control.setState(control.hasInventoryItems() ? State.RETURNING : State.RETURNING_HOME);
        control.setDelayTicks(control.noTargetDelayTicks());
    }

    public void completePath() {
        control.setState(State.COOKING);
    }

    public void tickCooking(ServerLevel world) {
        if (targetPos == null || action == Action.NONE) {
            control.setState(State.FIND_TARGET);
            return;
        }

        if (!control.canReachTargetFromStand(targetPos, control.currentStandPos(world))) {
            ValetDebug.record(control.villager(), "cooking lost_reach action=" + action + " pos=" + ValetDebug.shortPos(targetPos));
            clearTarget();
            control.setState(State.FIND_TARGET);
            return;
        }

        switch (action) {
            case TAKE_FROM_CONTAINER -> takeFromContainer(world);
            case HARVEST_CROP -> harvestCrop(world);
            case COOK_AT_WORKSTATION -> cookAtWorkstation(world);
            case NONE -> control.setState(State.FIND_TARGET);
        }
    }

    public boolean hasCookableIngredient() {
        Container inventory = control.villager().getInventory();
        int slots = control.getUsableInventorySlots(inventory);
        return RECIPES.stream().anyMatch(recipe -> countItem(inventory, recipe.ingredient(), slots) >= recipe.ingredientCount());
    }

    public Item getDisplayItem() {
        if (action == Action.HARVEST_CROP) {
            return Items.WOODEN_HOE;
        }
        return Items.BOWL;
    }

    public void clearTarget() {
        if (targetPos != null && action == Action.HARVEST_CROP) {
            control.releaseBlock(targetPos);
        }
        targetPos = null;
        action = Action.NONE;
    }

    public void clearAll() {
        clearTarget();
    }

    public String debugSummary() {
        return "cooking=" + action + " cookingTarget=" + (targetPos == null ? "-" : ValetDebug.shortPos(targetPos));
    }

    private void startTarget(ServerLevel world, BlockPos pos, Action nextAction) {
        targetPos = pos;
        action = nextAction;
        if (nextAction == Action.HARVEST_CROP && !control.claimBlock(world, pos, 200)) {
            clearTarget();
            control.setDelayTicks(10);
            return;
        }

        Set<BlockPos> goals = control.findStandGoals(world, pos, PathPurpose.COOK);
        if (goals.contains(control.currentStandPos(world))) {
            control.setState(State.COOKING);
            return;
        }

        List<BlockPos> path = control.planPathToAdjacent(world, PathPurpose.COOK, pos, goals);
        if (path.isEmpty()) {
            ValetDebug.record(control.villager(), "cooking no_path action=" + nextAction + " pos=" + ValetDebug.shortPos(pos));
            clearTarget();
            control.setDelayTicks(20);
            return;
        }
        control.startPath(PathPurpose.COOK, path);
    }

    private void takeFromContainer(ServerLevel world) {
        Container source = ValetInventoryTransfer.getContainerInventory(world, targetPos);
        if (source == null) {
            clearTarget();
            control.setState(State.FIND_TARGET);
            return;
        }

        Container inventory = control.villager().getInventory();
        int targetSlots = control.getUsableInventorySlots(inventory);
        int moved = 0;
        for (Recipe recipe : RECIPES) {
            int available = countItem(source, recipe.ingredient(), source.getContainerSize());
            int carried = countItem(inventory, recipe.ingredient(), targetSlots);
            if (available <= 0 || available + carried < recipe.ingredientCount()) {
                continue;
            }
            moved = moveItem(source, inventory, targetSlots, recipe.ingredient(), Math.min(INGREDIENT_BATCH, available));
            if (moved > 0) {
                break;
            }
        }

        if (moved > 0) {
            control.animateChestUse(world, targetPos);
            ValetDebug.record(control.villager(), "cooking ingredients_taken count=" + moved + " chest=" + ValetDebug.shortPos(targetPos));
        }
        clearTarget();
        control.setState(State.FIND_TARGET);
        control.setDelayTicks(moved > 0 ? control.actionDelayTicks() : 20);
    }

    private void harvestCrop(ServerLevel world) {
        BlockState state = world.getBlockState(targetPos);
        if (!isMatureCookingCrop(state)) {
            clearTarget();
            control.setState(State.FIND_TARGET);
            return;
        }
        if (!control.claimBlock(world, targetPos, 200)) {
            clearTarget();
            control.setState(State.FIND_TARGET);
            control.setDelayTicks(10);
            return;
        }

        List<ItemStack> drops = Block.getDrops(state, world, targetPos, world.getBlockEntity(targetPos), control.villager(), ItemStack.EMPTY);
        if (!control.canStoreAllDrops(drops)) {
            clearTarget();
            control.setState(State.RETURNING);
            return;
        }

        control.villager().getLookControl().setLookAt(targetPos.getX() + 0.5D, targetPos.getY() + 0.5D, targetPos.getZ() + 0.5D);
        control.villager().swing(InteractionHand.MAIN_HAND);
        if (!world.destroyBlock(targetPos, false, control.villager())) {
            ValetDebug.record(control.villager(), "cooking break_failed pos=" + ValetDebug.shortPos(targetPos));
            clearTarget();
            control.setState(State.FIND_TARGET);
            control.setDelayTicks(20);
            return;
        }
        world.levelEvent(2001, targetPos, Block.getId(state));
        control.collectDrops(drops);
        replantCrop(world, targetPos, state);
        ValetProgress.addXp(control.villager(), 3);
        ValetDebug.record(control.villager(), "cooking crop_harvested pos=" + ValetDebug.shortPos(targetPos));
        clearTarget();
        control.setState(State.FIND_TARGET);
        control.setDelayTicks(control.actionDelayTicks());
    }

    private void cookAtWorkstation(ServerLevel world) {
        Recipe recipe = firstAvailableRecipe();
        if (recipe == null) {
            clearTarget();
            control.setState(control.hasInventoryItems() ? State.RETURNING : State.FIND_TARGET);
            return;
        }

        Container inventory = control.villager().getInventory();
        int slots = control.getUsableInventorySlots(inventory);
        if (consumeItem(inventory, recipe.ingredient(), recipe.ingredientCount(), slots) != recipe.ingredientCount()) {
            clearTarget();
            control.setState(State.FIND_TARGET);
            return;
        }
        ItemStack result = new ItemStack(recipe.result());
        if (!ValetInventoryTransfer.insertStack(inventory, result, slots)) {
            returnToInventory(world, new ItemStack(recipe.ingredient(), recipe.ingredientCount()), inventory, slots);
            ValetDebug.record(control.villager(), "cooking output_insert_failed item=" + recipe.result().getDescriptionId());
            clearTarget();
            control.setState(State.RETURNING);
            control.setDelayTicks(20);
            return;
        }
        inventory.setChanged();
        control.villager().swing(InteractionHand.MAIN_HAND);
        control.villager().getLookControl().setLookAt(targetPos.getX() + 0.5D, targetPos.getY() + 0.5D, targetPos.getZ() + 0.5D);
        world.playSound(null, targetPos, SoundEvents.SMOKER_SMOKE, SoundSource.BLOCKS, 0.8F, 1.0F);
        ValetProgress.addXp(control.villager(), 4);
        ValetDebug.record(control.villager(), "cooking meal_done item=" + recipe.result().getDescriptionId());
        clearTarget();
        control.setState(hasCookableIngredient() ? State.FIND_TARGET : State.RETURNING);
        control.setDelayTicks(control.actionDelayTicks());
    }

    private BlockPos findIngredientContainer(ServerLevel world, BlockPos workOrigin) {
        List<BlockPos> candidates = new ArrayList<>();
        for (BlockPos pos : BlockPos.withinManhattan(workOrigin, control.ingredientRadius(), 4, control.ingredientRadius())) {
            BlockPos candidate = pos.immutable();
            if (!world.getBlockState(candidate).is(ValetMod.COOK_CHEST)) {
                continue;
            }
            Container container = ValetInventoryTransfer.getContainerInventory(world, candidate);
            if (container != null && canCompleteRecipeFrom(container)) {
                candidates.add(candidate);
            }
        }
        candidates.sort(Comparator.comparingDouble(pos -> squaredDistance(control.villager().blockPosition(), pos)));
        for (BlockPos candidate : candidates) {
            if (hasReachableStand(world, candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private BlockPos findMatureCrop(ServerLevel world, BlockPos workOrigin) {
        List<BlockPos> candidates = new ArrayList<>();
        for (BlockPos pos : BlockPos.withinManhattan(workOrigin, control.cropRadius(), control.cropVerticalRadius(), control.cropRadius())) {
            BlockPos candidate = pos.immutable();
            if (isMatureCookingCrop(world.getBlockState(candidate))
                    && !control.isBlockReservedByOther(world, candidate)) {
                candidates.add(candidate);
            }
        }
        candidates.sort(Comparator.comparingDouble(pos -> squaredDistance(control.villager().blockPosition(), pos)));
        for (BlockPos candidate : candidates) {
            if (hasReachableStand(world, candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private boolean hasReachableStand(ServerLevel world, BlockPos pos) {
        Set<BlockPos> goals = control.findStandGoals(world, pos, PathPurpose.COOK);
        return goals.contains(control.currentStandPos(world))
                || !control.planPathToAdjacent(world, PathPurpose.COOK, pos, goals).isEmpty();
    }

    private boolean canCompleteRecipeFrom(Container source) {
        Container inventory = control.villager().getInventory();
        int inventorySlots = control.getUsableInventorySlots(inventory);
        return RECIPES.stream().anyMatch(recipe ->
                countItem(inventory, recipe.ingredient(), inventorySlots)
                        + countItem(source, recipe.ingredient(), source.getContainerSize())
                        >= recipe.ingredientCount());
    }

    private Recipe firstAvailableRecipe() {
        Container inventory = control.villager().getInventory();
        int slots = control.getUsableInventorySlots(inventory);
        return RECIPES.stream()
                .filter(recipe -> countItem(inventory, recipe.ingredient(), slots) >= recipe.ingredientCount())
                .findFirst()
                .orElse(null);
    }

    private static boolean isMatureCookingCrop(BlockState state) {
        return (state.is(Blocks.WHEAT) || state.is(Blocks.POTATOES))
                && state.getBlock() instanceof CropBlock crop
                && crop.isMaxAge(state);
    }

    private void replantCrop(ServerLevel world, BlockPos pos, BlockState harvestedState) {
        Item seed = harvestedState.is(Blocks.WHEAT) ? Items.WHEAT_SEEDS : Items.POTATO;
        Container inventory = control.villager().getInventory();
        int slots = control.getUsableInventorySlots(inventory);
        if (!ValetInventoryTransfer.takeOneItem(inventory, seed, slots)) {
            return;
        }
        if (harvestedState.getBlock() instanceof CropBlock crop) {
            if (!world.setBlock(pos, crop.getStateForAge(0), Block.UPDATE_ALL)) {
                returnToInventory(world, new ItemStack(seed), inventory, slots);
            }
        }
    }

    private static int moveItem(Container source, Container target, int targetSlots, Item item, int maxCount) {
        int moved = 0;
        for (int slot = 0; slot < source.getContainerSize() && moved < maxCount; slot++) {
            ItemStack stack = source.getItem(slot);
            if (!stack.is(item)) {
                continue;
            }
            int amount = Math.min(maxCount - moved, stack.getCount());
            ItemStack extracted = stack.copyWithCount(amount);
            int before = extracted.getCount();
            ValetInventoryTransfer.insertStack(target, extracted, targetSlots);
            int accepted = before - extracted.getCount();
            if (accepted <= 0) {
                continue;
            }
            stack.shrink(accepted);
            if (stack.isEmpty()) {
                source.setItem(slot, ItemStack.EMPTY);
            }
            moved += accepted;
        }
        source.setChanged();
        target.setChanged();
        return moved;
    }

    private static int countItem(Container inventory, Item item, int maxSlots) {
        int count = 0;
        int slots = Math.min(inventory.getContainerSize(), Math.max(0, maxSlots));
        for (int slot = 0; slot < slots; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static int consumeItem(Container inventory, Item item, int count, int maxSlots) {
        int remaining = count;
        int slots = Math.min(inventory.getContainerSize(), Math.max(0, maxSlots));
        for (int slot = 0; slot < slots && remaining > 0; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.is(item)) {
                continue;
            }
            int consumed = Math.min(remaining, stack.getCount());
            stack.shrink(consumed);
            remaining -= consumed;
            if (stack.isEmpty()) {
                inventory.setItem(slot, ItemStack.EMPTY);
            }
        }
        int consumed = count - remaining;
        if (consumed > 0) {
            inventory.setChanged();
        }
        return consumed;
    }

    private void returnToInventory(ServerLevel world, ItemStack stack, Container inventory, int slots) {
        ValetInventoryTransfer.insertStack(inventory, stack, slots);
        if (!stack.isEmpty()) {
            control.villager().spawnAtLocation(world, stack);
        }
    }

    private static double squaredDistance(BlockPos first, BlockPos second) {
        int dx = first.getX() - second.getX();
        int dy = first.getY() - second.getY();
        int dz = first.getZ() - second.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    private enum Action {
        NONE,
        TAKE_FROM_CONTAINER,
        HARVEST_CROP,
        COOK_AT_WORKSTATION
    }

    private record Recipe(Item ingredient, int ingredientCount, Item result) {
    }

    public interface Control {
        Villager villager();

        BlockPos getWorkOrigin(ServerLevel world);

        BlockPos currentStandPos(ServerLevel world);

        Set<BlockPos> findStandGoals(ServerLevel world, BlockPos targetBlock, PathPurpose purpose);

        List<BlockPos> planPathToAdjacent(ServerLevel world, PathPurpose purpose, BlockPos targetBlock, Set<BlockPos> goals);

        void startPath(PathPurpose purpose, List<BlockPos> path);

        boolean canReachTargetFromStand(BlockPos targetBlock, BlockPos stand);

        boolean canStoreAllDrops(List<ItemStack> drops);

        void collectDrops(List<ItemStack> drops);

        boolean hasInventoryItems();

        int getUsableInventorySlots(Container inventory);

        int ingredientRadius();

        int cropRadius();

        int cropVerticalRadius();

        int actionDelayTicks();

        int noTargetDelayTicks();

        boolean claimBlock(ServerLevel world, BlockPos pos, int ttlTicks);

        boolean isBlockReservedByOther(ServerLevel world, BlockPos pos);

        void releaseBlock(BlockPos pos);

        void animateChestUse(ServerLevel world, BlockPos pos);

        void setState(State state);

        void setDelayTicks(int ticks);
    }
}
