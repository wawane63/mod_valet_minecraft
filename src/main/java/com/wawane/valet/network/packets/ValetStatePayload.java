package com.wawane.valet.network.packets;

import com.wawane.valet.order.ValetMineTarget;
import com.wawane.valet.order.ValetMiningScanner;
import com.wawane.valet.order.ValetOrders;
import com.wawane.valet.order.ValetWoodTarget;
import com.wawane.valet.order.ValetCraftTarget;
import com.wawane.valet.progress.ValetCombatPerk;
import com.wawane.valet.progress.ValetCombatProgress;
import com.wawane.valet.progress.ValetCombatSkillTree;
import com.wawane.valet.progress.ValetPerk;
import com.wawane.valet.progress.ValetProgress;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.world.ServerWorld;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public record ValetStatePayload(
        int valetEntityId,
        int orderIndex,
        int mineTargetIndex,
        int woodTargetIndex,
        int constructionTargetId,
        int craftTargetIndex,
        int[] oreCounts,
        int[] woodCounts,
        List<ItemStack> valetInventory,
        int level,
        int xp,
        int nextLevelXp,
        int pendingPerks,
        boolean[] perks,
        boolean[] combatPerks,
        int swordLevel,
        int swordXp,
        int swordNextLevelXp,
        int swordPendingPerks,
        int bowLevel,
        int bowXp,
        int bowNextLevelXp,
        int bowPendingPerks,
        boolean allyAwareness,
        String valetName
) {
    public ValetStatePayload {
        oreCounts = Arrays.copyOf(oreCounts, ValetMineTarget.values().length);
        woodCounts = Arrays.copyOf(woodCounts, ValetWoodTarget.values().length);
        valetInventory = copyInventory(valetInventory);
        perks = Arrays.copyOf(perks, ValetPerk.values().length);
        combatPerks = Arrays.copyOf(combatPerks, ValetCombatPerk.values().length);
    }

    public static ValetStatePayload from(ServerWorld world, VillagerEntity villager) {
        return new ValetStatePayload(
                villager.getId(),
                ValetOrders.get(villager).ordinal(),
                getCurrentMineTargetIndex(villager),
                getCurrentWoodTargetIndex(villager),
                ValetOrders.getConstructionTargetId(villager),
                getCurrentCraftTargetIndex(villager),
                ValetMiningScanner.countNearbyOres(world, villager),
                ValetMiningScanner.countNearbyWood(world, villager),
                copyInventory(villager.getInventory()),
                ValetProgress.getLevel(villager),
                ValetProgress.getXp(villager),
                ValetProgress.getNextLevelXp(villager),
                ValetProgress.getPendingPerks(villager),
                ValetProgress.getPerks(villager),
                ValetCombatProgress.getPerks(villager),
                ValetCombatProgress.getLevel(villager, ValetCombatSkillTree.SWORD),
                ValetCombatProgress.getXp(villager, ValetCombatSkillTree.SWORD),
                ValetCombatProgress.getNextLevelXp(villager, ValetCombatSkillTree.SWORD),
                ValetCombatProgress.getPendingPerks(villager, ValetCombatSkillTree.SWORD),
                ValetCombatProgress.getLevel(villager, ValetCombatSkillTree.BOW),
                ValetCombatProgress.getXp(villager, ValetCombatSkillTree.BOW),
                ValetCombatProgress.getNextLevelXp(villager, ValetCombatSkillTree.BOW),
                ValetCombatProgress.getPendingPerks(villager, ValetCombatSkillTree.BOW),
                ValetCombatProgress.hasPerk(villager, ValetCombatPerk.ALLY_AWARENESS),
                getValetName(villager)
        );
    }

    public static ValetStatePayload read(PacketByteBuf buf) {
        int valetEntityId = buf.readInt();
        int orderIndex = buf.readInt();
        int mineTargetIndex = buf.readInt();
        int woodTargetIndex = buf.readInt();
        int constructionTargetId = buf.readInt();
        int craftTargetIndex = buf.readInt();
        int[] oreCounts = readOreCounts(buf);
        int[] woodCounts = readWoodCounts(buf);
        List<ItemStack> valetInventory = readInventory(buf);
        int level = buf.readInt();
        int xp = buf.readInt();
        int nextLevelXp = buf.readInt();
        int pendingPerks = buf.readInt();
        boolean[] perks = new boolean[ValetPerk.values().length];
        for (int i = 0; i < perks.length; i++) {
            perks[i] = buf.readBoolean();
        }
        boolean[] combatPerks = new boolean[ValetCombatPerk.values().length];
        for (int i = 0; i < combatPerks.length; i++) {
            combatPerks[i] = buf.readBoolean();
        }
        int swordLevel = buf.readInt();
        int swordXp = buf.readInt();
        int swordNextLevelXp = buf.readInt();
        int swordPendingPerks = buf.readInt();
        int bowLevel = buf.readInt();
        int bowXp = buf.readInt();
        int bowNextLevelXp = buf.readInt();
        int bowPendingPerks = buf.readInt();
        boolean allyAwareness = buf.readBoolean();
        return new ValetStatePayload(valetEntityId, orderIndex, mineTargetIndex, woodTargetIndex, constructionTargetId, craftTargetIndex, oreCounts, woodCounts, valetInventory, level, xp, nextLevelXp, pendingPerks, perks, combatPerks, swordLevel, swordXp, swordNextLevelXp, swordPendingPerks, bowLevel, bowXp, bowNextLevelXp, bowPendingPerks, allyAwareness, buf.readString(32));
    }

    public void write(PacketByteBuf buf) {
        buf.writeInt(valetEntityId);
        buf.writeInt(orderIndex);
        buf.writeInt(mineTargetIndex);
        buf.writeInt(woodTargetIndex);
        buf.writeInt(constructionTargetId);
        buf.writeInt(craftTargetIndex);
        for (int count : oreCounts) {
            buf.writeInt(count);
        }
        for (int count : woodCounts) {
            buf.writeInt(count);
        }
        buf.writeInt(valetInventory.size());
        for (ItemStack stack : valetInventory) {
            buf.writeItemStack(stack);
        }
        buf.writeInt(level);
        buf.writeInt(xp);
        buf.writeInt(nextLevelXp);
        buf.writeInt(pendingPerks);
        for (boolean perk : perks) {
            buf.writeBoolean(perk);
        }
        for (boolean perk : combatPerks) {
            buf.writeBoolean(perk);
        }
        buf.writeInt(swordLevel);
        buf.writeInt(swordXp);
        buf.writeInt(swordNextLevelXp);
        buf.writeInt(swordPendingPerks);
        buf.writeInt(bowLevel);
        buf.writeInt(bowXp);
        buf.writeInt(bowNextLevelXp);
        buf.writeInt(bowPendingPerks);
        buf.writeBoolean(allyAwareness);
        buf.writeString(valetName, 32);
    }

    @Override
    public int[] oreCounts() {
        return Arrays.copyOf(oreCounts, oreCounts.length);
    }

    @Override
    public int[] woodCounts() {
        return Arrays.copyOf(woodCounts, woodCounts.length);
    }

    @Override
    public List<ItemStack> valetInventory() {
        return copyInventory(valetInventory);
    }

    @Override
    public boolean[] perks() {
        return Arrays.copyOf(perks, perks.length);
    }

    @Override
    public boolean[] combatPerks() {
        return Arrays.copyOf(combatPerks, combatPerks.length);
    }

    private static int getCurrentMineTargetIndex(VillagerEntity villager) {
        ValetMineTarget target = ValetOrders.getMineTarget(villager);
        return target == null ? -1 : target.ordinal();
    }

    private static int getCurrentWoodTargetIndex(VillagerEntity villager) {
        ValetWoodTarget target = ValetOrders.getWoodTarget(villager);
        return target == null ? -1 : target.ordinal();
    }

    private static int getCurrentCraftTargetIndex(VillagerEntity villager) {
        ValetCraftTarget target = ValetOrders.getCraftTarget(villager);
        return target == null ? -1 : target.ordinal();
    }

    private static int[] readOreCounts(PacketByteBuf buf) {
        int[] counts = new int[ValetMineTarget.values().length];
        for (int i = 0; i < counts.length; i++) {
            counts[i] = buf.readInt();
        }
        return counts;
    }

    private static int[] readWoodCounts(PacketByteBuf buf) {
        int[] counts = new int[ValetWoodTarget.values().length];
        for (int i = 0; i < counts.length; i++) {
            counts[i] = buf.readInt();
        }
        return counts;
    }

    private static List<ItemStack> readInventory(PacketByteBuf buf) {
        int count = Math.max(0, buf.readInt());
        List<ItemStack> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            result.add(buf.readItemStack());
        }
        return result;
    }

    private static List<ItemStack> copyInventory(Inventory inventory) {
        List<ItemStack> result = new ArrayList<>(inventory.size());
        for (int slot = 0; slot < inventory.size(); slot++) {
            result.add(inventory.getStack(slot).copy());
        }
        return List.copyOf(result);
    }

    private static List<ItemStack> copyInventory(List<ItemStack> stacks) {
        List<ItemStack> result = new ArrayList<>(stacks.size());
        for (ItemStack stack : stacks) {
            result.add(stack.copy());
        }
        return List.copyOf(result);
    }

    private static String getValetName(VillagerEntity villager) {
        return villager.hasCustomName() && villager.getCustomName() != null ? villager.getCustomName().getString() : "";
    }
}
