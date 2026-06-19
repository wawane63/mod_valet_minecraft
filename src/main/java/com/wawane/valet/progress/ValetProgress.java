package com.wawane.valet.progress;

import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.nbt.NbtCompound;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ValetProgress {
    public static final int DATA_VERSION = 1;
    private static final String DATA_VERSION_KEY = "ValetProgressDataVersion";
    private static final String LEVEL_KEY = "ValetLevel";
    private static final String XP_KEY = "ValetXp";
    private static final String PENDING_PERKS_KEY = "ValetPendingPerks";
    private static final Map<UUID, Data> DATA = new ConcurrentHashMap<>();

    private ValetProgress() {
    }

    public static int getLevel(VillagerEntity villager) {
        return data(villager).level;
    }

    public static int getXp(VillagerEntity villager) {
        return data(villager).xp;
    }

    public static int getNextLevelXp(VillagerEntity villager) {
        return xpForNextLevel(data(villager).level);
    }

    public static int getPendingPerks(VillagerEntity villager) {
        return data(villager).pendingPerks;
    }

    public static boolean hasPerk(VillagerEntity villager, ValetPerk perk) {
        return perk != null && data(villager).perks[perk.ordinal()];
    }

    public static boolean[] getPerks(VillagerEntity villager) {
        return Arrays.copyOf(data(villager).perks, ValetPerk.values().length);
    }

    public static boolean hasData(VillagerEntity villager) {
        return DATA.containsKey(villager.getUuid());
    }

    public static boolean hasNbt(NbtCompound nbt) {
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

    public static void addXp(VillagerEntity villager, int amount) {
        if (amount <= 0) {
            return;
        }

        Data data = data(villager);
        data.xp += amount;
        while (data.xp >= xpForNextLevel(data.level)) {
            data.xp -= xpForNextLevel(data.level);
            data.level++;
            data.pendingPerks++;
        }
    }

    public static boolean choosePerk(VillagerEntity villager, ValetPerk perk) {
        Data data = data(villager);
        if (data.pendingPerks <= 0 || perk == null || hasPerk(villager, perk)) {
            return false;
        }

        data.perks[perk.ordinal()] = true;
        data.pendingPerks--;
        return true;
    }

    public static void writeToNbt(VillagerEntity villager, NbtCompound nbt) {
        Data data = data(villager);
        nbt.putInt(DATA_VERSION_KEY, DATA_VERSION);
        nbt.putInt(LEVEL_KEY, data.level);
        nbt.putInt(XP_KEY, data.xp);
        nbt.putInt(PENDING_PERKS_KEY, data.pendingPerks);
        for (ValetPerk perk : ValetPerk.values()) {
            nbt.putBoolean(perk.getNbtKey(), data.perks[perk.ordinal()]);
        }
    }

    public static void readFromNbt(VillagerEntity villager, NbtCompound nbt) {
        Data data = data(villager);
        data.level = Math.max(1, nbt.getInt(LEVEL_KEY));
        data.xp = Math.max(0, nbt.getInt(XP_KEY));
        data.pendingPerks = Math.max(0, nbt.getInt(PENDING_PERKS_KEY));
        for (ValetPerk perk : ValetPerk.values()) {
            data.perks[perk.ordinal()] = nbt.getBoolean(perk.getNbtKey());
        }
    }

    private static int xpForNextLevel(int level) {
        return 40 + Math.max(0, level - 1) * 25;
    }

    private static Data data(VillagerEntity villager) {
        return DATA.computeIfAbsent(villager.getUuid(), uuid -> new Data());
    }

    private static final class Data {
        private int level = 1;
        private int xp;
        private int pendingPerks;
        private final boolean[] perks = new boolean[ValetPerk.values().length];
    }
}
