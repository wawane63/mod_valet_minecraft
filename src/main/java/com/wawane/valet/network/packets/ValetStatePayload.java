package com.wawane.valet.network.packets;

import com.wawane.valet.ValetMod;
import com.wawane.valet.ValetRole;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;

public record ValetStatePayload(
        int valetEntityId,
        int roleIndex,
        int orderIndex,
        int mineTargetIndex,
        int woodTargetIndex,
        int farmAreaId,
        int farmCropMask,
        boolean farmReplant,
        boolean farmTillSoil,
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
) implements CustomPacketPayload {
    public static final Type<ValetStatePayload> TYPE = new Type<>(ValetMod.id("valet_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ValetStatePayload> CODEC = StreamCodec.ofMember(ValetStatePayload::write, ValetStatePayload::read);

    public ValetStatePayload {
        oreCounts = Arrays.copyOf(oreCounts, ValetMineTarget.values().length);
        woodCounts = Arrays.copyOf(woodCounts, ValetWoodTarget.values().length);
        valetInventory = copyInventory(valetInventory);
        perks = Arrays.copyOf(perks, ValetPerk.values().length);
        combatPerks = Arrays.copyOf(combatPerks, ValetCombatPerk.values().length);
    }

    public static ValetStatePayload from(ServerLevel world, Villager villager) {
        return new ValetStatePayload(
                villager.getId(),
                ValetRole.get(world, villager).ordinal(),
                ValetOrders.get(villager).ordinal(),
                getCurrentMineTargetIndex(villager),
                getCurrentWoodTargetIndex(villager),
                ValetOrders.getFarmAreaId(villager),
                ValetOrders.getFarmCropMask(villager),
                ValetOrders.shouldReplantFarm(villager),
                ValetOrders.shouldTillFarm(villager),
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

    public static ValetStatePayload read(RegistryFriendlyByteBuf buf) {
        int valetEntityId = buf.readInt();
        int roleIndex = buf.readInt();
        int orderIndex = buf.readInt();
        int mineTargetIndex = buf.readInt();
        int woodTargetIndex = buf.readInt();
        int farmAreaId = buf.readInt();
        int farmCropMask = buf.readInt();
        boolean farmReplant = buf.readBoolean();
        boolean farmTillSoil = buf.readBoolean();
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
        return new ValetStatePayload(valetEntityId, roleIndex, orderIndex, mineTargetIndex, woodTargetIndex, farmAreaId, farmCropMask, farmReplant, farmTillSoil, constructionTargetId, craftTargetIndex, oreCounts, woodCounts, valetInventory, level, xp, nextLevelXp, pendingPerks, perks, combatPerks, swordLevel, swordXp, swordNextLevelXp, swordPendingPerks, bowLevel, bowXp, bowNextLevelXp, bowPendingPerks, allyAwareness, buf.readUtf(32));
    }

    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeInt(valetEntityId);
        buf.writeInt(roleIndex);
        buf.writeInt(orderIndex);
        buf.writeInt(mineTargetIndex);
        buf.writeInt(woodTargetIndex);
        buf.writeInt(farmAreaId);
        buf.writeInt(farmCropMask);
        buf.writeBoolean(farmReplant);
        buf.writeBoolean(farmTillSoil);
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
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, stack);
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
        buf.writeUtf(valetName, 32);
    }

    @Override
    public Type<ValetStatePayload> type() {
        return TYPE;
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

    private static int getCurrentMineTargetIndex(Villager villager) {
        ValetMineTarget target = ValetOrders.getMineTarget(villager);
        return target == null ? -1 : target.ordinal();
    }

    private static int getCurrentWoodTargetIndex(Villager villager) {
        ValetWoodTarget target = ValetOrders.getWoodTarget(villager);
        return target == null ? -1 : target.ordinal();
    }

    private static int getCurrentCraftTargetIndex(Villager villager) {
        ValetCraftTarget target = ValetOrders.getCraftTarget(villager);
        return target == null ? -1 : target.ordinal();
    }

    private static int[] readOreCounts(RegistryFriendlyByteBuf buf) {
        int[] counts = new int[ValetMineTarget.values().length];
        for (int i = 0; i < counts.length; i++) {
            counts[i] = buf.readInt();
        }
        return counts;
    }

    private static int[] readWoodCounts(RegistryFriendlyByteBuf buf) {
        int[] counts = new int[ValetWoodTarget.values().length];
        for (int i = 0; i < counts.length; i++) {
            counts[i] = buf.readInt();
        }
        return counts;
    }

    private static List<ItemStack> readInventory(RegistryFriendlyByteBuf buf) {
        int count = Math.max(0, buf.readInt());
        List<ItemStack> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            result.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(buf));
        }
        return result;
    }

    private static List<ItemStack> copyInventory(Container inventory) {
        List<ItemStack> result = new ArrayList<>(inventory.getContainerSize());
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            result.add(inventory.getItem(slot).copy());
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

    private static String getValetName(Villager villager) {
        return villager.hasCustomName() && villager.getCustomName() != null ? villager.getCustomName().getString() : "";
    }
}
