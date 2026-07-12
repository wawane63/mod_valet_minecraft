package com.wawane.valet.progress;

import com.mojang.serialization.Codec;
import com.wawane.valet.ValetRole;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

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
        return perk != null && (isIntrinsicPerk(villager, perk) || data(villager).perks[perk.ordinal()]);
    }

    public static boolean[] getPerks(Villager villager) {
        boolean[] perks = Arrays.copyOf(data(villager).perks, ValetPerk.values().length);
        for (ValetPerk perk : ValetPerk.values()) {
            if (isIntrinsicPerk(villager, perk)) {
                perks[perk.ordinal()] = true;
            }
        }
        return perks;
    }

    public static boolean hasData(Villager villager) {
        return DATA.containsKey(villager.getUUID());
    }

    public static boolean hasNbt(ValueInput input) {
        if (input.getInt(DATA_VERSION_KEY).isPresent()
                || input.getInt(LEVEL_KEY).isPresent()
                || input.getInt(XP_KEY).isPresent()
                || input.getInt(PENDING_PERKS_KEY).isPresent()) {
            return true;
        }
        for (ValetPerk perk : ValetPerk.values()) {
            if (input.read(perk.getNbtKey(), Codec.BOOL).isPresent()) {
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
        data.xp = saturatingAdd(data.xp, amount);
        while (data.xp >= xpForNextLevel(data.level)) {
            data.xp -= xpForNextLevel(data.level);
            if (data.level == Integer.MAX_VALUE) {
                data.xp = 0;
                break;
            }
            data.level++;
            data.pendingPerks = saturatingAdd(data.pendingPerks, 1);
        }
    }

    public static boolean choosePerk(Villager villager, ValetPerk perk) {
        if (perk == null || !isRolePerk(villager, perk)) {
            return false;
        }
        Data data = data(villager);
        normalize(data);
        if (data.pendingPerks <= 0 || hasPerk(villager, perk) || !canChoosePerk(data, perk)) {
            return false;
        }

        data.perks[perk.ordinal()] = true;
        data.pendingPerks--;
        return true;
    }

    public static void writeToNbt(Villager villager, ValueOutput output) {
        Data data = data(villager);
        normalize(data);
        output.putInt(DATA_VERSION_KEY, DATA_VERSION);
        output.putInt(LEVEL_KEY, data.level);
        output.putInt(XP_KEY, data.xp);
        output.putInt(PENDING_PERKS_KEY, data.pendingPerks);
        for (ValetPerk perk : ValetPerk.values()) {
            output.putBoolean(perk.getNbtKey(), data.perks[perk.ordinal()]);
        }
    }

    public static void readFromNbt(Villager villager, ValueInput input) {
        Data data = new Data();
        DATA.put(villager.getUUID(), data);
        if (!hasNbt(input)) {
            return;
        }

        if (input.getInt(LEVEL_KEY).isPresent()) {
            data.level = Math.max(1, input.getIntOr(LEVEL_KEY, 1));
        }
        if (input.getInt(XP_KEY).isPresent()) {
            data.xp = Math.max(0, input.getIntOr(XP_KEY, 0));
        }
        if (input.getInt(PENDING_PERKS_KEY).isPresent()) {
            data.pendingPerks = Math.max(0, input.getIntOr(PENDING_PERKS_KEY, 0));
        }
        for (ValetPerk perk : ValetPerk.values()) {
            if (input.read(perk.getNbtKey(), Codec.BOOL).isPresent()) {
                data.perks[perk.ordinal()] = input.getBooleanOr(perk.getNbtKey(), false);
            }
        }
        normalize(data);
    }

    private static int xpForNextLevel(int level) {
        long threshold = 40L + Math.max(0L, (long) level - 1L) * 25L;
        return (int) Math.min(Integer.MAX_VALUE, threshold);
    }

    private static int saturatingAdd(int value, int amount) {
        return (int) Math.min(Integer.MAX_VALUE, (long) value + amount);
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
            case MAGIC_ICE -> true;
            case MAGIC_FANGS, MAGIC_HEAL, MAGIC_WARD -> true;
            case MAGIC_SHATTER -> data.perks[ValetPerk.MAGIC_FANGS.ordinal()];
            case MAGIC_REGEN_AURA -> data.perks[ValetPerk.MAGIC_HEAL.ordinal()];
            case MAGIC_WEAKEN -> data.perks[ValetPerk.MAGIC_WARD.ordinal()];
        };
    }

    private static boolean isRolePerk(Villager villager, ValetPerk perk) {
        return villager.level() instanceof ServerLevel world && ValetRole.get(world, villager) == perk.getRole();
    }

    private static boolean isIntrinsicPerk(Villager villager, ValetPerk perk) {
        return perk == ValetPerk.MAGIC_ICE
                && villager.level() instanceof ServerLevel world
                && ValetRole.get(world, villager) == ValetRole.MAGICIAN;
    }

    private static final class Data {
        private int level = 1;
        private int xp;
        private int pendingPerks;
        private final boolean[] perks = new boolean[ValetPerk.values().length];
    }
}
