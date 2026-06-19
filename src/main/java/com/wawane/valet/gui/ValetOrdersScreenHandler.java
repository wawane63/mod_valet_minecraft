package com.wawane.valet.gui;

import com.wawane.valet.ValetConversations;
import com.wawane.valet.ValetMod;
import com.wawane.valet.construction.ValetConstructionBlueprint;
import com.wawane.valet.order.ValetMineTarget;
import com.wawane.valet.order.ValetOrder;
import com.wawane.valet.order.ValetWoodTarget;
import com.wawane.valet.progress.ValetPerk;
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
    private final int[] oreCounts;
    private final int[] woodCounts;
    private final List<ValetConstructionBlueprint> constructions;
    private final int level;
    private final int xp;
    private final int nextLevelXp;
    private final int pendingPerks;
    private final boolean[] perks;
    private final String valetName;

    public ValetOrdersScreenHandler(int syncId, PlayerInventory inventory, PacketByteBuf buf) {
        this(syncId, inventory, buf.readInt(), buf.readUuid(), buf.readIdentifier(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(), readOreCounts(buf), readWoodCounts(buf), readConstructions(buf), buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(), readPerks(buf), buf.readString(32));
    }

    public ValetOrdersScreenHandler(int syncId, PlayerInventory inventory, int valetEntityId) {
        this(syncId, inventory, valetEntityId, new UUID(0L, 0L), World.OVERWORLD.getValue(), ValetOrder.NONE.ordinal(), -1, -1, -1, new int[ValetMineTarget.values().length], new int[ValetWoodTarget.values().length], List.of(), 1, 0, 40, 0, new boolean[ValetPerk.values().length], "");
    }

    public ValetOrdersScreenHandler(int syncId, PlayerInventory inventory, int valetEntityId, UUID valetUuid, Identifier valetDimension, int currentOrderIndex, int currentMineTargetIndex, int currentWoodTargetIndex, int currentConstructionTargetId, int[] oreCounts, int[] woodCounts, List<ValetConstructionBlueprint> constructions, int level, int xp, int nextLevelXp, int pendingPerks, boolean[] perks, String valetName) {
        super(ValetMod.VALET_ORDERS_SCREEN_HANDLER, syncId);
        this.valetEntityId = valetEntityId;
        this.valetUuid = valetUuid;
        this.valetDimension = valetDimension;
        this.currentOrderIndex = currentOrderIndex;
        this.currentMineTargetIndex = currentMineTargetIndex;
        this.currentWoodTargetIndex = currentWoodTargetIndex;
        this.currentConstructionTargetId = currentConstructionTargetId;
        this.oreCounts = oreCounts;
        this.woodCounts = woodCounts;
        this.constructions = constructions;
        this.level = level;
        this.xp = xp;
        this.nextLevelXp = nextLevelXp;
        this.pendingPerks = pendingPerks;
        this.perks = perks;
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
}
