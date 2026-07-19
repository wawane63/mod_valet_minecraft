package com.wawane.valet.state;

import com.wawane.valet.ValetConversations;
import com.wawane.valet.ValetAnchor;
import com.wawane.valet.ai.ValetResidence;
import com.wawane.valet.ValetMod;
import com.wawane.valet.ValetRole;
import com.wawane.valet.order.ValetOrders;
import com.wawane.valet.progress.ValetCombatProgress;
import com.wawane.valet.progress.ValetProgress;
import java.util.UUID;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class ValetData {
    private ValetData() {
    }

    public static boolean shouldPersist(Villager villager) {
        return isValet(villager) || hasRuntimeData(villager);
    }

    public static boolean shouldRead(Villager villager, ValueInput input) {
        return isValet(villager) || hasPersistentData(input);
    }

    public static void writeToNbt(Villager villager, ValueOutput output) {
        ValetIdentity.writeToNbt(villager, output);
        ValetBehavior.writeToNbt(villager, output);
        ValetAnchor.writeToNbt(villager, output);
        ValetResidence.writeToNbt(villager, output);
        ValetRole.writeToNbt(villager, output);
        ValetOrders.writeToNbt(villager, output);
        ValetProgress.writeToNbt(villager, output);
        ValetCombatProgress.writeToNbt(villager, output);
    }

    public static void readFromNbt(Villager villager, ValueInput input) {
        ValetIdentity.readFromNbt(villager, input);
        ValetBehavior.readFromNbt(villager, input);
        ValetAnchor.readFromNbt(villager, input);
        ValetResidence.readFromNbt(villager, input);
        ValetRole.readFromNbt(villager, input);
        ValetOrders.readFromNbt(villager, input);
        ValetProgress.readFromNbt(villager, input);
        ValetCombatProgress.readFromNbt(villager, input);
    }

    public static void clearVillagerRuntime(UUID uuid) {
        ValetIdentity.clear(uuid);
        ValetBehavior.clear(uuid);
        ValetAnchor.clear(uuid);
        ValetResidence.clear(uuid);
        ValetRole.clear(uuid);
        ValetConversations.clear(uuid);
        ValetOrders.clear(uuid);
        ValetProgress.clear(uuid);
        ValetCombatProgress.clear(uuid);
    }

    public static void clearAllVillagerRuntime() {
        ValetIdentity.clearAll();
        ValetBehavior.clearAll();
        ValetAnchor.clearAll();
        ValetResidence.clearAll();
        ValetRole.clearAll();
        ValetConversations.clearAll();
        ValetOrders.clearAll();
        ValetProgress.clearAll();
        ValetCombatProgress.clearAll();
    }

    public static boolean hasRuntimeData(Villager villager) {
        return ValetBehavior.hasData(villager) || ValetAnchor.hasData(villager) || ValetResidence.hasData(villager) || ValetOrders.hasData(villager) || ValetProgress.hasData(villager) || ValetCombatProgress.hasData(villager);
    }

    private static boolean hasPersistentData(ValueInput input) {
        return ValetIdentity.hasNbt(input)
                || ValetBehavior.hasNbt(input)
                || ValetAnchor.hasNbt(input)
                || ValetResidence.hasNbt(input)
                || ValetRole.hasNbt(input)
                || ValetOrders.hasNbt(input)
                || ValetProgress.hasNbt(input)
                || ValetCombatProgress.hasNbt(input);
    }

    public static boolean isValet(Villager villager) {
        return ValetMod.isValet(villager);
    }
}
