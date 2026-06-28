package com.wawane.valet.ai.inventory;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class ValetInventoryTransfer {
    private ValetInventoryTransfer() {
    }

    public static Container getContainerInventory(ServerLevel world, BlockPos pos) {
        BlockState blockState = world.getBlockState(pos);
        if (blockState.getBlock() instanceof ChestBlock chestBlock) {
            return ChestBlock.getContainer(chestBlock, blockState, world, pos, true);
        }
        BlockEntity blockEntity = world.getBlockEntity(pos);
        return blockEntity instanceof Container inventory ? inventory : null;
    }

    public static int depositInventory(ServerLevel world, BlockPos pos, Container sourceInventory) {
        Container targetInventory = getContainerInventory(world, pos);
        if (targetInventory == null) {
            return 0;
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

            ItemStack remaining = sourceStack.copy();
            insertStack(targetInventory, remaining);
            int moved = sourceStack.getCount() - remaining.getCount();
            if (moved > 0) {
                movedTotal += moved;
                sourceStack.shrink(moved);
                if (sourceStack.isEmpty()) {
                    sourceInventory.setItem(slot, ItemStack.EMPTY);
                }
            }
        }

        sourceInventory.setChanged();
        targetInventory.setChanged();
        return movedTotal;
    }

    public static boolean canStoreAllDrops(Container inventory, int usableSlots, List<ItemStack> drops) {
        List<ItemStack> simulated = new ArrayList<>(usableSlots);
        for (int slot = 0; slot < usableSlots; slot++) {
            simulated.add(inventory.getItem(slot).copy());
        }

        for (ItemStack drop : drops) {
            ItemStack remaining = drop.copy();
            simulateInsert(simulated, inventory.getMaxStackSize(), remaining);
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
        for (int slot = 0; slot < slots && !stack.isEmpty(); slot++) {
            ItemStack current = inventory.getItem(slot);
            if (canMerge(current, stack)) {
                int limit = Math.min(current.getMaxStackSize(), inventory.getMaxStackSize());
                int amount = Math.min(stack.getCount(), limit - current.getCount());
                if (amount > 0) {
                    current.grow(amount);
                    stack.shrink(amount);
                }
            }
        }

        for (int slot = 0; slot < slots && !stack.isEmpty(); slot++) {
            if (inventory.getItem(slot).isEmpty()) {
                int amount = Math.min(stack.getCount(), Math.min(stack.getMaxStackSize(), inventory.getMaxStackSize()));
                inventory.setItem(slot, stack.split(amount));
            }
        }

        inventory.setChanged();
        return stack.isEmpty();
    }

    private static void simulateInsert(List<ItemStack> stacks, int inventoryMaxCount, ItemStack stack) {
        for (int slot = 0; slot < stacks.size() && !stack.isEmpty(); slot++) {
            ItemStack current = stacks.get(slot);
            if (canMerge(current, stack)) {
                int limit = Math.min(current.getMaxStackSize(), inventoryMaxCount);
                int amount = Math.min(stack.getCount(), limit - current.getCount());
                if (amount > 0) {
                    current.grow(amount);
                    stack.shrink(amount);
                }
            }
        }

        for (int slot = 0; slot < stacks.size() && !stack.isEmpty(); slot++) {
            if (stacks.get(slot).isEmpty()) {
                int amount = Math.min(stack.getCount(), Math.min(stack.getMaxStackSize(), inventoryMaxCount));
                stacks.set(slot, stack.split(amount));
            }
        }
    }

    private static boolean canMerge(ItemStack current, ItemStack incoming) {
        return !current.isEmpty()
                && ItemStack.isSameItemSameComponents(current, incoming)
                && current.getCount() < current.getMaxStackSize();
    }
}
