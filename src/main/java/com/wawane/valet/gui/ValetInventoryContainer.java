package com.wawane.valet.gui;

import com.wawane.valet.ValetConversations;
import com.wawane.valet.ValetMod;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Adapts the eight-slot villager inventory to Minecraft's vanilla 9x1 chest UI. */
public final class ValetInventoryContainer implements Container {
    private static final int MENU_SIZE = 9;
    private static final int VALET_SLOTS = 8;

    private final Villager villager;
    private final Container inventory;

    public ValetInventoryContainer(Villager villager) {
        this.villager = villager;
        this.inventory = villager.getInventory();
    }

    private boolean isValetSlot(int slot) {
        return slot >= 0 && slot < Math.min(VALET_SLOTS, inventory.getContainerSize());
    }

    @Override
    public int getContainerSize() {
        return MENU_SIZE;
    }

    @Override
    public boolean isEmpty() {
        for (int slot = 0; slot < Math.min(VALET_SLOTS, inventory.getContainerSize()); slot++) {
            if (!inventory.getItem(slot).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return isValetSlot(slot) ? inventory.getItem(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return isValetSlot(slot) ? inventory.removeItem(slot, amount) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return isValetSlot(slot) ? inventory.removeItemNoUpdate(slot) : ItemStack.EMPTY;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (isValetSlot(slot)) {
            inventory.setItem(slot, stack);
        }
    }

    @Override
    public void setChanged() {
        inventory.setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return villager.isAlive()
                && ValetMod.isValet(villager)
                && player.level() == villager.level()
                && player.distanceToSqr(villager) <= 64.0D;
    }

    @Override
    public void startOpen(ContainerUser user) {
        ValetConversations.begin(villager);
    }

    @Override
    public void stopOpen(ContainerUser user) {
        ValetConversations.end(villager.getUUID());
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return isValetSlot(slot) && inventory.canPlaceItem(slot, stack);
    }

    @Override
    public void clearContent() {
        for (int slot = 0; slot < Math.min(VALET_SLOTS, inventory.getContainerSize()); slot++) {
            inventory.setItem(slot, ItemStack.EMPTY);
        }
        inventory.setChanged();
    }
}
