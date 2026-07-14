package com.wawane.valet.ai.inventory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;

public final class ValetInventoryTransfer {
    private ValetInventoryTransfer() {
    }

    public static Container getContainerInventory(ServerLevel world, BlockPos pos) {
        BlockPos containerPos = canonicalContainerPos(world, pos);
        BlockState blockState = world.getBlockState(containerPos);
        if (blockState.getBlock() instanceof ChestBlock chestBlock) {
            return ChestBlock.getContainer(chestBlock, blockState, world, containerPos, true);
        }
        BlockEntity blockEntity = world.getBlockEntity(containerPos);
        return blockEntity instanceof Container inventory ? inventory : null;
    }

    public static BlockPos canonicalContainerPos(ServerLevel world, BlockPos pos) {
        BlockPos immutable = pos.immutable();
        BlockState state = world.getBlockState(immutable);
        if (!(state.getBlock() instanceof ChestBlock)
                || !state.hasProperty(ChestBlock.TYPE)
                || state.getValue(ChestBlock.TYPE) == ChestType.SINGLE) {
            return immutable;
        }

        BlockPos connected = ChestBlock.getConnectedBlockPos(immutable, state).immutable();
        return comparePositions(immutable, connected) <= 0 ? immutable : connected;
    }

    public static int depositInventory(ServerLevel world, BlockPos pos, Container sourceInventory) {
        return depositInventory(world, pos, sourceInventory, stack -> false, 0);
    }

    public static int depositInventory(
            ServerLevel world,
            BlockPos pos,
            Container sourceInventory,
            Predicate<ItemStack> retainPredicate,
            int retainPerItem
    ) {
        Container targetInventory = getContainerInventory(world, pos);
        if (targetInventory == null) {
            return 0;
        }

        Map<Item, Integer> remainingRetained = new HashMap<>();
        if (retainPerItem > 0) {
            for (int slot = 0; slot < sourceInventory.getContainerSize(); slot++) {
                ItemStack stack = sourceInventory.getItem(slot);
                if (!stack.isEmpty() && retainPredicate.test(stack)) {
                    remainingRetained.merge(stack.getItem(), stack.getCount(), Integer::sum);
                }
            }
            remainingRetained.replaceAll((item, count) -> Math.min(retainPerItem, count));
        }

        int movedTotal = 0;
        for (int slot = 0; slot < sourceInventory.getContainerSize(); slot++) {
            ItemStack sourceStack = sourceInventory.getItem(slot);
            if (sourceStack.isEmpty()) {
                continue;
            }
            if (sourceStack.is(Items.ARROW)) {
                continue;
            }

            int retained = Math.min(sourceStack.getCount(), remainingRetained.getOrDefault(sourceStack.getItem(), 0));
            if (retained > 0) {
                remainingRetained.put(sourceStack.getItem(), remainingRetained.get(sourceStack.getItem()) - retained);
            }
            ItemStack remaining = sourceStack.copy();
            remaining.setCount(sourceStack.getCount() - retained);
            if (remaining.isEmpty()) {
                continue;
            }
            int transferable = remaining.getCount();
            insertStack(targetInventory, remaining);
            int moved = transferable - remaining.getCount();
            if (moved > 0) {
                movedTotal += moved;
                sourceStack.shrink(moved);
                if (sourceStack.isEmpty()) {
                    sourceInventory.setItem(slot, ItemStack.EMPTY);
                }
            }
        }

        if (movedTotal > 0) {
            sourceInventory.setChanged();
            targetInventory.setChanged();
        }
        return movedTotal;
    }

    public static int withdrawMatching(
            ServerLevel world,
            BlockPos pos,
            Container targetInventory,
            int targetSlots,
            Predicate<ItemStack> predicate,
            int maxItems
    ) {
        Container sourceInventory = getContainerInventory(world, pos);
        if (sourceInventory == null || maxItems <= 0) {
            return 0;
        }

        int movedTotal = 0;
        for (int slot = 0; slot < sourceInventory.getContainerSize() && movedTotal < maxItems; slot++) {
            ItemStack sourceStack = sourceInventory.getItem(slot);
            if (sourceStack.isEmpty() || !predicate.test(sourceStack)) {
                continue;
            }

            ItemStack moving = sourceStack.copy();
            moving.setCount(Math.min(sourceStack.getCount(), maxItems - movedTotal));
            int requested = moving.getCount();
            insertStack(targetInventory, moving, targetSlots);
            int moved = requested - moving.getCount();
            if (moved <= 0) {
                continue;
            }

            sourceStack.shrink(moved);
            if (sourceStack.isEmpty()) {
                sourceInventory.setItem(slot, ItemStack.EMPTY);
            }
            movedTotal += moved;
        }

        if (movedTotal > 0) {
            sourceInventory.setChanged();
            targetInventory.setChanged();
        }
        return movedTotal;
    }

    public static boolean canStoreAllDrops(Container inventory, int usableSlots, List<ItemStack> drops) {
        int slots = Math.min(inventory.getContainerSize(), Math.max(0, usableSlots));
        List<ItemStack> simulated = new ArrayList<>(slots);
        for (int slot = 0; slot < slots; slot++) {
            simulated.add(inventory.getItem(slot).copy());
        }

        for (ItemStack drop : drops) {
            if (drop.isEmpty()) {
                continue;
            }
            ItemStack remaining = drop.copy();
            simulateInsert(inventory, simulated, remaining);
            if (!remaining.isEmpty()) {
                return false;
            }
        }

        return true;
    }

    public static boolean inventoryHasItem(Container inventory, Item item, int maxSlots) {
        int slots = Math.min(inventory.getContainerSize(), Math.max(0, maxSlots));
        for (int slot = 0; slot < slots; slot++) {
            if (inventory.getItem(slot).is(item)) {
                return true;
            }
        }
        return false;
    }

    public static boolean takeOneItem(Container inventory, Item item, int maxSlots) {
        int slots = Math.min(inventory.getContainerSize(), Math.max(0, maxSlots));
        for (int slot = 0; slot < slots; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.is(item)) {
                continue;
            }

            stack.shrink(1);
            if (stack.isEmpty()) {
                inventory.setItem(slot, ItemStack.EMPTY);
            }
            inventory.setChanged();
            return true;
        }
        return false;
    }

    public static boolean insertStack(Container inventory, ItemStack stack) {
        return insertStack(inventory, stack, inventory.getContainerSize());
    }

    public static boolean insertStack(Container inventory, ItemStack stack, int maxSlots) {
        int slots = Math.min(inventory.getContainerSize(), Math.max(0, maxSlots));
        int initialCount = stack.getCount();
        for (int slot = 0; slot < slots && !stack.isEmpty(); slot++) {
            ItemStack current = inventory.getItem(slot);
            if (inventory.canPlaceItem(slot, stack) && canMerge(current, stack)) {
                int limit = Math.min(current.getMaxStackSize(), inventory.getMaxStackSize(current));
                int amount = Math.min(stack.getCount(), limit - current.getCount());
                if (amount > 0) {
                    current.grow(amount);
                    stack.shrink(amount);
                }
            }
        }

        for (int slot = 0; slot < slots && !stack.isEmpty(); slot++) {
            if (inventory.getItem(slot).isEmpty() && inventory.canPlaceItem(slot, stack)) {
                int amount = Math.min(stack.getCount(), Math.min(stack.getMaxStackSize(), inventory.getMaxStackSize(stack)));
                inventory.setItem(slot, stack.split(amount));
            }
        }

        if (stack.getCount() != initialCount) {
            inventory.setChanged();
        }
        return stack.isEmpty();
    }

    public static boolean canAcceptAnyDepositableStack(Container target, Container source) {
        for (int sourceSlot = 0; sourceSlot < source.getContainerSize(); sourceSlot++) {
            ItemStack incoming = source.getItem(sourceSlot);
            if (incoming.isEmpty() || incoming.is(Items.ARROW)) {
                continue;
            }
            for (int targetSlot = 0; targetSlot < target.getContainerSize(); targetSlot++) {
                if (!target.canPlaceItem(targetSlot, incoming)) {
                    continue;
                }
                ItemStack current = target.getItem(targetSlot);
                if (current.isEmpty()) {
                    return true;
                }
                int limit = Math.min(current.getMaxStackSize(), target.getMaxStackSize(current));
                if (canMerge(current, incoming) && current.getCount() < limit) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void simulateInsert(Container inventory, List<ItemStack> stacks, ItemStack stack) {
        for (int slot = 0; slot < stacks.size() && !stack.isEmpty(); slot++) {
            ItemStack current = stacks.get(slot);
            if (inventory.canPlaceItem(slot, stack) && canMerge(current, stack)) {
                int limit = Math.min(current.getMaxStackSize(), inventory.getMaxStackSize(current));
                int amount = Math.min(stack.getCount(), limit - current.getCount());
                if (amount > 0) {
                    current.grow(amount);
                    stack.shrink(amount);
                }
            }
        }

        for (int slot = 0; slot < stacks.size() && !stack.isEmpty(); slot++) {
            if (stacks.get(slot).isEmpty() && inventory.canPlaceItem(slot, stack)) {
                int amount = Math.min(stack.getCount(), Math.min(stack.getMaxStackSize(), inventory.getMaxStackSize(stack)));
                stacks.set(slot, stack.split(amount));
            }
        }
    }

    private static boolean canMerge(ItemStack current, ItemStack incoming) {
        return !current.isEmpty()
                && ItemStack.isSameItemSameComponents(current, incoming)
                && current.getCount() < current.getMaxStackSize();
    }

    private static int comparePositions(BlockPos first, BlockPos second) {
        if (first.getX() != second.getX()) {
            return Integer.compare(first.getX(), second.getX());
        }
        if (first.getY() != second.getY()) {
            return Integer.compare(first.getY(), second.getY());
        }
        return Integer.compare(first.getZ(), second.getZ());
    }
}
