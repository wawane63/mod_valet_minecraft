package com.wawane.valet.network.packets;

import com.wawane.valet.order.ValetMineTarget;
import com.wawane.valet.order.ValetOrders;
import com.wawane.valet.order.ValetWoodTarget;
import com.wawane.valet.progress.ValetPerk;
import com.wawane.valet.progress.ValetProgress;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.network.PacketByteBuf;

import java.util.Arrays;

public record ValetStatePayload(int valetEntityId, int orderIndex, int mineTargetIndex, int woodTargetIndex, int constructionTargetId, int level, int xp, int nextLevelXp, int pendingPerks, boolean[] perks, String valetName) {
    public ValetStatePayload {
        perks = Arrays.copyOf(perks, ValetPerk.values().length);
    }

    public static ValetStatePayload from(VillagerEntity villager) {
        return new ValetStatePayload(
                villager.getId(),
                ValetOrders.get(villager).ordinal(),
                getCurrentMineTargetIndex(villager),
                getCurrentWoodTargetIndex(villager),
                ValetOrders.getConstructionTargetId(villager),
                ValetProgress.getLevel(villager),
                ValetProgress.getXp(villager),
                ValetProgress.getNextLevelXp(villager),
                ValetProgress.getPendingPerks(villager),
                ValetProgress.getPerks(villager),
                getValetName(villager)
        );
    }

    public static ValetStatePayload read(PacketByteBuf buf) {
        int valetEntityId = buf.readInt();
        int orderIndex = buf.readInt();
        int mineTargetIndex = buf.readInt();
        int woodTargetIndex = buf.readInt();
        int constructionTargetId = buf.readInt();
        int level = buf.readInt();
        int xp = buf.readInt();
        int nextLevelXp = buf.readInt();
        int pendingPerks = buf.readInt();
        boolean[] perks = new boolean[ValetPerk.values().length];
        for (int i = 0; i < perks.length; i++) {
            perks[i] = buf.readBoolean();
        }
        return new ValetStatePayload(valetEntityId, orderIndex, mineTargetIndex, woodTargetIndex, constructionTargetId, level, xp, nextLevelXp, pendingPerks, perks, buf.readString(32));
    }

    public void write(PacketByteBuf buf) {
        buf.writeInt(valetEntityId);
        buf.writeInt(orderIndex);
        buf.writeInt(mineTargetIndex);
        buf.writeInt(woodTargetIndex);
        buf.writeInt(constructionTargetId);
        buf.writeInt(level);
        buf.writeInt(xp);
        buf.writeInt(nextLevelXp);
        buf.writeInt(pendingPerks);
        for (boolean perk : perks) {
            buf.writeBoolean(perk);
        }
        buf.writeString(valetName, 32);
    }

    @Override
    public boolean[] perks() {
        return Arrays.copyOf(perks, perks.length);
    }

    private static int getCurrentMineTargetIndex(VillagerEntity villager) {
        ValetMineTarget target = ValetOrders.getMineTarget(villager);
        return target == null ? -1 : target.ordinal();
    }

    private static int getCurrentWoodTargetIndex(VillagerEntity villager) {
        ValetWoodTarget target = ValetOrders.getWoodTarget(villager);
        return target == null ? -1 : target.ordinal();
    }

    private static String getValetName(VillagerEntity villager) {
        return villager.hasCustomName() && villager.getCustomName() != null ? villager.getCustomName().getString() : "";
    }
}
