package com.wawane.valet.progress;

import com.wawane.valet.ValetRole;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;

public final class ValetProgress {
    public static final int DATA_VERSION = 1;
    private static final String DATA_VERSION_KEY = "ValetProgressDataVersion";
    private static final String LEVEL_KEY = "ValetLevel";
    private static final String XP_KEY = "ValetXp";
    private static final String PENDING_PERKS_KEY = "ValetPendingPerks";
    private static final Map<UUID, Data> DATA = new ConcurrentHashMap<>();

    private ValetProgress() {
    }

    public static int getLevel(Villager villager) {
        Data data = data(villager);
        normalize(data);
        return data.level;
    }

    public static int getXp(Villager villager) {
        Data data = data(villager);
        normalize(data);
        return data.xp;
    }

    public static int getNextLevelXp(Villager villager) {
        Data data = data(villager);
        normalize(data);
        return xpForNextLevel(data.level);
    }

    public static int getPendingPerks(Villager villager) {
        Data data = data(villager);
        normalize(data);
        return data.pendingPerks;
    }

    public static boolean hasPerk(Villager villager, ValetPerk perk) {
        return perk != null && data(villager).perks[perk.ordinal()];
    }

    public static boolean[] getPerks(Villager villager) {
        return Arrays.copyOf(data(villager).perks, ValetPerk.values().length);
    }

    public static boolean hasData(Villager villager) {
        return DATA.containsKey(villager.getUUID());
    }

    public static boolean hasNbt(CompoundTag nbt) {
        if (nbt.contains(DATA_VERSION_KEY) || nbt.contains(LEVEL_KEY) || nbt.contains(XP_KEY) || nbt.contains(PENDING_PERKS_KEY)) {
            return true;
        }
        for (ValetPerk perk : ValetPerk.values()) {
            if (nbt.contains(perk.getNbtKey())) {
                return true;
            }
        }
        return false;
    }

    public static void clear(UUID uuid) {
        DATA.remove(uuid);
    }

    public static void clearAll() {
        DATA.clear();
    }

    public static void addXp(Villager villager, int amount) {
        if (amount <= 0) {
            return;
        }

        Data data = data(villager);
        normalize(data);
        data.xp += amount;
        while (data.xp >= xpForNextLevel(data.level)) {
            data.xp -= xpForNextLevel(data.level);
            data.level++;
            data.pendingPerks++;
        }
    }

    public static boolean choosePerk(Villager villager, ValetPerk perk) {
        Data data = data(villager);
        normalize(data);
        if (data.pendingPerks <= 0 || perk == null || hasPerk(villager, perk) || !isRolePerk(villager, perk) || !canChoosePerk(data, perk)) {
            return false;
        }

        data.perks[perk.ordinal()] = true;
        data.pendingPerks--;
        return true;
    }

    public static void writeToNbt(Villager villager, CompoundTag nbt) {
        Data data = data(villager);
        normalize(data);
        nbt.putInt(DATA_VERSION_KEY, DATA_VERSION);
        nbt.putInt(LEVEL_KEY, data.level);
        nbt.putInt(XP_KEY, data.xp);
        nbt.putInt(PENDING_PERKS_KEY, data.pendingPerks);
        for (ValetPerk perk : ValetPerk.values()) {
            nbt.putBoolean(perk.getNbtKey(), data.perks[perk.ordinal()]);
        }
    }

    public static void readFromNbt(Villager villager, CompoundTag nbt) {
        Data data = data(villager);
        if (!hasNbt(nbt)) {
            normalize(data);
            return;
        }

        if (nbt.contains(LEVEL_KEY)) {
            data.level = Math.max(1, nbt.getIntOr(LEVEL_KEY, 1));
        }
        if (nbt.contains(XP_KEY)) {
            data.xp = Math.max(0, nbt.getIntOr(XP_KEY, 0));
        }
        if (nbt.contains(PENDING_PERKS_KEY)) {
            data.pendingPerks = Math.max(0, nbt.getIntOr(PENDING_PERKS_KEY, 0));
        }
        for (ValetPerk perk : ValetPerk.values()) {
            if (nbt.contains(perk.getNbtKey())) {
                data.perks[perk.ordinal()] = nbt.getBooleanOr(perk.getNbtKey(), false);
            }
        }
        normalize(data);
    }

    private static int xpForNextLevel(int level) {
        return 40 + Math.max(0, level - 1) * 25;
    }

    private static Data data(Villager villager) {
        return DATA.computeIfAbsent(villager.getUUID(), uuid -> new Data());
    }

    private static void normalize(Data data) {
        data.level = Math.max(1, data.level);
        data.xp = Math.max(0, data.xp);
        data.pendingPerks = Math.max(0, data.pendingPerks);
    }

    private static boolean canChoosePerk(Data data, ValetPerk perk) {
        return switch (perk) {
            case SPEED -> true;
            case VISION, MOVEMENT -> data.perks[ValetPerk.SPEED.ordinal()];
            case STORAGE -> data.perks[ValetPerk.SPEED.ordinal()];
            case PATHING -> data.perks[ValetPerk.MOVEMENT.ordinal()];
            case VEIN -> data.perks[ValetPerk.VISION.ordinal()];
            case HAUL -> data.perks[ValetPerk.STORAGE.ordinal()];
            case LIGHTING -> data.perks[ValetPerk.PATHING.ordinal()];
            case FARM_HANDS -> true;
            case FARM_RANGE, FARM_REPLANTING, FARM_TILLING -> data.perks[ValetPerk.FARM_HANDS.ordinal()];
            case FARM_STORAGE -> data.perks[ValetPerk.FARM_REPLANTING.ordinal()];
            case FARM_STEWARD -> data.perks[ValetPerk.FARM_RANGE.ordinal()] && data.perks[ValetPerk.FARM_STORAGE.ordinal()];
        };
    }

    private static boolean isRolePerk(Villager villager, ValetPerk perk) {
        return villager.level() instanceof ServerLevel world && ValetRole.get(world, villager) == perk.getRole();
    }

    private static final class Data {
        private int level = 1;
        private int xp;
        private int pendingPerks;
        private final boolean[] perks = new boolean[ValetPerk.values().length];
    }
}
