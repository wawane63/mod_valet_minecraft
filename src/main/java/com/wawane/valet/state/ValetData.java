package com.wawane.valet.state;

import com.wawane.valet.ValetConversations;
import com.wawane.valet.ValetHome;
import com.wawane.valet.ValetMod;
import com.wawane.valet.order.ValetOrders;
import com.wawane.valet.progress.ValetCombatProgress;
import com.wawane.valet.progress.ValetProgress;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.npc.villager.Villager;

public final class ValetData {
    private ValetData() {
    }

    public static boolean shouldPersist(Villager villager, CompoundTag nbt) {
        return isValet(villager) || hasPersistentData(nbt) || hasRuntimeData(villager);
    }

    public static boolean shouldRead(Villager villager, CompoundTag nbt) {
        return isValet(villager) || hasPersistentData(nbt);
    }

    public static void writeToNbt(Villager villager, CompoundTag nbt) {
        ValetHome.writeToNbt(villager, nbt);
        ValetOrders.writeToNbt(villager, nbt);
        ValetProgress.writeToNbt(villager, nbt);
        ValetCombatProgress.writeToNbt(villager, nbt);
    }

    public static void readFromNbt(Villager villager, CompoundTag nbt) {
        ValetHome.readFromNbt(villager, nbt);
        ValetOrders.readFromNbt(villager, nbt);
        ValetProgress.readFromNbt(villager, nbt);
        ValetCombatProgress.readFromNbt(villager, nbt);
    }

    public static void clearVillagerRuntime(UUID uuid) {
        ValetHome.clear(uuid);
        ValetConversations.clear(uuid);
        ValetOrders.clear(uuid);
        ValetProgress.clear(uuid);
        ValetCombatProgress.clear(uuid);
    }

    public static void clearAllVillagerRuntime() {
        ValetHome.clearAll();
        ValetConversations.clearAll();
        ValetOrders.clearAll();
        ValetProgress.clearAll();
        ValetCombatProgress.clearAll();
    }

    public static boolean hasRuntimeData(Villager villager) {
        return ValetHome.hasData(villager) || ValetOrders.hasData(villager) || ValetProgress.hasData(villager) || ValetCombatProgress.hasData(villager);
    }

    private static boolean hasPersistentData(CompoundTag nbt) {
        return ValetHome.hasNbt(nbt) || ValetOrders.hasNbt(nbt) || ValetProgress.hasNbt(nbt) || ValetCombatProgress.hasNbt(nbt);
    }

    public static boolean isValet(Villager villager) {
        return ValetMod.isValet(villager);
    }
}
