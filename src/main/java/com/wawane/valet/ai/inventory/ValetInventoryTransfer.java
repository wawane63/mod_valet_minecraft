package com.wawane.valet.ai.inventory;

import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public final class ValetInventoryTransfer {
    private ValetInventoryTransfer() {
    }

    public static Inventory getContainerInventory(ServerWorld world, BlockPos pos) {
        BlockState blockState = world.getBlockState(pos);
        if (blockState.getBlock() instanceof ChestBlock chestBlock) {
            return ChestBlock.getInventory(chestBlock, blockState, world, pos, true);
        }
        BlockEntity blockEntity = world.getBlockEntity(pos);
        return blockEntity instanceof Inventory inventory ? inventory : null;
    }

    public static int depositInventory(ServerWorld world, BlockPos pos, Inventory sourceInventory) {
        Inventory targetInventory = getContainerInventory(world, pos);
        if (targetInventory == null) {
            return 0;
        }

        int movedTotal = 0;
        for (int slot = 0; slot < sourceInventory.size(); slot++) {
            ItemStack sourceStack = sourceInventory.getStack(slot);
            if (sourceStack.isEmpty()) {
                continue;
            }

            ItemStack remaining = sourceStack.copy();
            insertStack(targetInventory, remaining);
            int moved = sourceStack.getCount() - remaining.getCount();
            if (moved > 0) {
                movedTotal += moved;
                sourceStack.decrement(moved);
                if (sourceStack.isEmpty()) {
                    sourceInventory.setStack(slot, ItemStack.EMPTY);
                }
            }
        }

        sourceInventory.markDirty();
        targetInventory.markDirty();
        return movedTotal;
    }

    public static boolean canStoreAllDrops(Inventory inventory, int usableSlots, List<ItemStack> drops) {
        List<ItemStack> simulated = new ArrayList<>(usableSlots);
        for (int slot = 0; slot < usableSlots; slot++) {
            simulated.add(inventory.getStack(slot).copy());
        }

        for (ItemStack drop : drops) {
            ItemStack remaining = drop.copy();
            simulateInsert(simulated, inventory.getMaxCountPerStack(), remaining);
            if (!remaining.isEmpty()) {
                return false;
            }
        }

        return true;
    }

    public static boolean inventoryHasItem(Inventory inventory, Item item, int maxSlots) {
        int slots = Math.min(inventory.size(), Math.max(0, maxSlots));
        for (int slot = 0; slot < slots; slot++) {
            if (inventory.getStack(slot).isOf(item)) {
                return true;
            }
        }
        return false;
    }

    public static boolean takeOneItem(Inventory inventory, Item item, int maxSlots) {
        int slots = Math.min(inventory.size(), Math.max(0, maxSlots));
        for (int slot = 0; slot < slots; slot++) {
            ItemStack stack = inventory.getStack(slot);
            if (!stack.isOf(item)) {
                continue;
            }

            stack.decrement(1);
            if (stack.isEmpty()) {
                inventory.setStack(slot, ItemStack.EMPTY);
            }
            inventory.markDirty();
            return true;
        }
        return false;
    }

    public static boolean insertStack(Inventory inventory, ItemStack stack) {
        return insertStack(inventory, stack, inventory.size());
    }

    public static boolean insertStack(Inventory inventory, ItemStack stack, int maxSlots) {
        int slots = Math.min(inventory.size(), Math.max(0, maxSlots));
        for (int slot = 0; slot < slots && !stack.isEmpty(); slot++) {
            ItemStack current = inventory.getStack(slot);
            if (canMerge(current, stack)) {
                int limit = Math.min(current.getMaxCount(), inventory.getMaxCountPerStack());
                int amount = Math.min(stack.getCount(), limit - current.getCount());
                if (amount > 0) {
                    current.increment(amount);
                    stack.decrement(amount);
                }
            }
        }

        for (int slot = 0; slot < slots && !stack.isEmpty(); slot++) {
            if (inventory.getStack(slot).isEmpty()) {
                int amount = Math.min(stack.getCount(), Math.min(stack.getMaxCount(), inventory.getMaxCountPerStack()));
                inventory.setStack(slot, stack.split(amount));
            }
        }

        inventory.markDirty();
        return stack.isEmpty();
    }

    private static void simulateInsert(List<ItemStack> stacks, int inventoryMaxCount, ItemStack stack) {
        for (int slot = 0; slot < stacks.size() && !stack.isEmpty(); slot++) {
            ItemStack current = stacks.get(slot);
            if (canMerge(current, stack)) {
                int limit = Math.min(current.getMaxCount(), inventoryMaxCount);
                int amount = Math.min(stack.getCount(), limit - current.getCount());
                if (amount > 0) {
                    current.increment(amount);
                    stack.decrement(amount);
                }
            }
        }

        for (int slot = 0; slot < stacks.size() && !stack.isEmpty(); slot++) {
            if (stacks.get(slot).isEmpty()) {
                int amount = Math.min(stack.getCount(), Math.min(stack.getMaxCount(), inventoryMaxCount));
                stacks.set(slot, stack.split(amount));
            }
        }
    }

    private static boolean canMerge(ItemStack current, ItemStack incoming) {
        return !current.isEmpty()
                && ItemStack.canCombine(current, incoming)
                && current.getCount() < current.getMaxCount();
    }
}
