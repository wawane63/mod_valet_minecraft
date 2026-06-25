package com.wawane.valet.gui;

import com.wawane.valet.ValetConversations;
import com.wawane.valet.ValetMod;
import com.wawane.valet.construction.ValetConstructionBlueprint;
import com.wawane.valet.order.ValetCraftTarget;
import com.wawane.valet.order.ValetMineTarget;
import com.wawane.valet.order.ValetOrder;
import com.wawane.valet.order.ValetWoodTarget;
import com.wawane.valet.progress.ValetPerk;
import com.wawane.valet.progress.ValetCombatPerk;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ValetOrdersScreenHandler extends ScreenHandler {
    private final int valetEntityId;
    private final UUID valetUuid;
    private final Identifier valetDimension;
    private final int currentOrderIndex;
    private final int currentMineTargetIndex;
    private final int currentWoodTargetIndex;
    private final int currentConstructionTargetId;
    private final int currentCraftTargetIndex;
    private final int[] oreCounts;
    private final int[] woodCounts;
    private final List<ValetConstructionBlueprint> constructions;
    private final List<ItemStack> valetInventory;
    private final int level;
    private final int xp;
    private final int nextLevelXp;
    private final int pendingPerks;
    private final boolean[] perks;
    private final boolean[] combatPerks;
    private final int swordLevel;
    private final int swordXp;
    private final int swordNextLevelXp;
    private final int swordPendingPerks;
    private final int bowLevel;
    private final int bowXp;
    private final int bowNextLevelXp;
    private final int bowPendingPerks;
    private final boolean allyAwareness;
    private final String valetName;

    public ValetOrdersScreenHandler(int syncId, PlayerInventory inventory, PacketByteBuf buf) {
        this(syncId, inventory, buf.readInt(), buf.readUuid(), buf.readIdentifier(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(), readOreCounts(buf), readWoodCounts(buf), readConstructions(buf), readInventory(buf), buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(), readPerks(buf), readCombatPerks(buf), buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readBoolean(), buf.readString(32));
    }

    public ValetOrdersScreenHandler(int syncId, PlayerInventory inventory, int valetEntityId) {
        this(syncId, inventory, valetEntityId, new UUID(0L, 0L), World.OVERWORLD.getValue(), ValetOrder.NONE.ordinal(), -1, -1, -1, -1, new int[ValetMineTarget.values().length], new int[ValetWoodTarget.values().length], List.of(), List.of(), 1, 0, 40, 0, new boolean[ValetPerk.values().length], new boolean[ValetCombatPerk.values().length], 1, 0, 30, 0, 1, 0, 30, 0, true, "");
    }

    public ValetOrdersScreenHandler(int syncId, PlayerInventory inventory, int valetEntityId, UUID valetUuid, Identifier valetDimension, int currentOrderIndex, int currentMineTargetIndex, int currentWoodTargetIndex, int currentConstructionTargetId, int currentCraftTargetIndex, int[] oreCounts, int[] woodCounts, List<ValetConstructionBlueprint> constructions, List<ItemStack> valetInventory, int level, int xp, int nextLevelXp, int pendingPerks, boolean[] perks, boolean[] combatPerks, int swordLevel, int swordXp, int swordNextLevelXp, int swordPendingPerks, int bowLevel, int bowXp, int bowNextLevelXp, int bowPendingPerks, boolean allyAwareness, String valetName) {
        super(ValetMod.VALET_ORDERS_SCREEN_HANDLER, syncId);
        this.valetEntityId = valetEntityId;
        this.valetUuid = valetUuid;
        this.valetDimension = valetDimension;
        this.currentOrderIndex = currentOrderIndex;
        this.currentMineTargetIndex = currentMineTargetIndex;
        this.currentWoodTargetIndex = currentWoodTargetIndex;
        this.currentConstructionTargetId = currentConstructionTargetId;
        this.currentCraftTargetIndex = currentCraftTargetIndex;
        this.oreCounts = oreCounts;
        this.woodCounts = woodCounts;
        this.constructions = constructions;
        this.valetInventory = copyInventory(valetInventory);
        this.level = level;
        this.xp = xp;
        this.nextLevelXp = nextLevelXp;
        this.pendingPerks = pendingPerks;
        this.perks = perks;
        this.combatPerks = combatPerks;
        this.swordLevel = swordLevel;
        this.swordXp = swordXp;
        this.swordNextLevelXp = swordNextLevelXp;
        this.swordPendingPerks = swordPendingPerks;
        this.bowLevel = bowLevel;
        this.bowXp = bowXp;
        this.bowNextLevelXp = bowNextLevelXp;
        this.bowPendingPerks = bowPendingPerks;
        this.allyAwareness = allyAwareness;
        this.valetName = valetName;
    }

    public int getValetEntityId() {
        return valetEntityId;
    }

    public UUID getValetUuid() {
        return valetUuid;
    }

    public Identifier getValetDimension() {
        return valetDimension;
    }

    public ValetOrder getCurrentOrder() {
        return ValetOrder.fromIndex(currentOrderIndex);
    }

    public int getCurrentMineTargetIndex() {
        return currentMineTargetIndex;
    }

    public int getCurrentWoodTargetIndex() {
        return currentWoodTargetIndex;
    }

    public int getCurrentConstructionTargetId() {
        return currentConstructionTargetId;
    }

    public int getCurrentCraftTargetIndex() {
        return currentCraftTargetIndex;
    }

    public int getOreCount(ValetMineTarget target) {
        int index = target.ordinal();
        if (index < 0 || index >= oreCounts.length) {
            return 0;
        }
        return oreCounts[index];
    }

    public int getWoodCount(ValetWoodTarget target) {
        int index = target.ordinal();
        if (index < 0 || index >= woodCounts.length) {
            return 0;
        }
        return woodCounts[index];
    }

    public List<ValetConstructionBlueprint> getConstructions() {
        return constructions;
    }

    public List<ItemStack> getValetInventory() {
        return copyInventory(valetInventory);
    }

    public int getLevel() {
        return level;
    }

    public int getXp() {
        return xp;
    }

    public int getNextLevelXp() {
        return nextLevelXp;
    }

    public int getPendingPerks() {
        return pendingPerks;
    }

    public boolean hasPerk(ValetPerk perk) {
        int index = perk.ordinal();
        return index >= 0 && index < perks.length && perks[index];
    }

    public boolean hasCombatPerk(ValetCombatPerk perk) {
        int index = perk.ordinal();
        return index >= 0 && index < combatPerks.length && combatPerks[index];
    }

    public int getSwordLevel() {
        return swordLevel;
    }

    public int getSwordXp() {
        return swordXp;
    }

    public int getSwordNextLevelXp() {
        return swordNextLevelXp;
    }

    public int getSwordPendingPerks() {
        return swordPendingPerks;
    }

    public int getBowLevel() {
        return bowLevel;
    }

    public int getBowXp() {
        return bowXp;
    }

    public int getBowNextLevelXp() {
        return bowNextLevelXp;
    }

    public int getBowPendingPerks() {
        return bowPendingPerks;
    }

    public boolean hasAllyAwareness() {
        return allyAwareness;
    }

    public String getValetName() {
        return valetName;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        Entity entity = player.getWorld().getEntityById(valetEntityId);
        return entity instanceof VillagerEntity villager
                && villager.getVillagerData().getProfession() == ValetMod.VALET_PROFESSION
                && player.squaredDistanceTo(villager) <= 64.0D;
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        if (player.getWorld().isClient) {
            return;
        }

        ValetConversations.end(valetUuid);
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

    private static boolean[] readPerks(PacketByteBuf buf) {
        boolean[] perks = new boolean[ValetPerk.values().length];
        for (int i = 0; i < perks.length; i++) {
            perks[i] = buf.readBoolean();
        }
        return perks;
    }

    private static boolean[] readCombatPerks(PacketByteBuf buf) {
        boolean[] perks = new boolean[ValetCombatPerk.values().length];
        for (int i = 0; i < perks.length; i++) {
            perks[i] = buf.readBoolean();
        }
        return perks;
    }

    private static List<ValetConstructionBlueprint> readConstructions(PacketByteBuf buf) {
        int count = buf.readInt();
        List<ValetConstructionBlueprint> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            NbtCompound nbt = buf.readNbt();
            if (nbt != null) {
                result.add(ValetConstructionBlueprint.readNbt(nbt));
            }
        }
        return result;
    }

    private static List<ItemStack> readInventory(PacketByteBuf buf) {
        int count = Math.max(0, buf.readInt());
        List<ItemStack> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            result.add(buf.readItemStack());
        }
        return result;
    }

    private static List<ItemStack> copyInventory(List<ItemStack> stacks) {
        List<ItemStack> result = new ArrayList<>(stacks.size());
        for (ItemStack stack : stacks) {
            result.add(stack.copy());
        }
        return result;
    }
}
