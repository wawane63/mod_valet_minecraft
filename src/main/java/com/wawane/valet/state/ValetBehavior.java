package com.wawane.valet.state;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;

public final class ValetBehavior {
    private static final boolean DEFAULT_FREE_BEHAVIOR = false;
    private static final boolean DEFAULT_AVOID_NIGHT_RETURN = false;
    private static final int RECALL_HOLD_TICKS = 20 * 5;
    private static final int RECALL_MAX_TRAVEL_TICKS = 20 * 60;
    private static final String FREE_BEHAVIOR_KEY = "ValetFreeBehavior";
    private static final String AVOID_NIGHT_RETURN_KEY = "ValetAvoidNightReturn";
    private static final Map<UUID, Data> DATA = new ConcurrentHashMap<>();
    private static final Map<UUID, Recall> RECALLS = new ConcurrentHashMap<>();

    private ValetBehavior() {
    }

    public static boolean isFreeBehavior(Villager villager) {
        return dataOrDefault(villager).freeBehavior;
    }

    public static boolean shouldAvoidNightReturn(Villager villager) {
        return dataOrDefault(villager).avoidNightReturn;
    }

    public static void setSettings(Villager villager, boolean freeBehavior, boolean avoidNightReturn) {
        UUID uuid = villager.getUUID();
        if (freeBehavior == DEFAULT_FREE_BEHAVIOR && avoidNightReturn == DEFAULT_AVOID_NIGHT_RETURN) {
            DATA.remove(uuid);
            return;
        }
        DATA.put(uuid, new Data(freeBehavior, avoidNightReturn));
    }

    public static boolean shouldUseVanillaBehavior(ServerLevel world, Villager villager) {
        if (isRecallActive(world, villager)) {
            return false;
        }
        if (isFreeBehavior(villager)) {
            return true;
        }
        return world.isDarkOutside() && !shouldAvoidNightReturn(villager);
    }

    public static void recallToWorkstation(ServerLevel world, Villager villager) {
        RECALLS.put(villager.getUUID(), new Recall(world.getGameTime() + RECALL_MAX_TRAVEL_TICKS, 0L));
    }

    public static void markRecallArrived(ServerLevel world, Villager villager) {
        UUID uuid = villager.getUUID();
        Recall recall = RECALLS.get(uuid);
        if (recall == null || recall.holdUntil > 0L) {
            return;
        }
        RECALLS.put(uuid, new Recall(world.getGameTime() + RECALL_HOLD_TICKS, world.getGameTime() + RECALL_HOLD_TICKS));
    }

    public static boolean isRecallActive(ServerLevel world, Villager villager) {
        UUID uuid = villager.getUUID();
        Recall recall = RECALLS.get(uuid);
        if (recall == null) {
            return false;
        }
        long now = world.getGameTime();
        long expiresAt = recall.holdUntil > 0L ? recall.holdUntil : recall.travelDeadline;
        if (now > expiresAt) {
            RECALLS.remove(uuid);
            return false;
        }
        return true;
    }

    public static void clear(UUID uuid) {
        DATA.remove(uuid);
        RECALLS.remove(uuid);
    }

    public static void clearRecall(UUID uuid) {
        RECALLS.remove(uuid);
    }

    public static void clearAll() {
        DATA.clear();
        RECALLS.clear();
    }

    public static boolean hasData(Villager villager) {
        return DATA.containsKey(villager.getUUID());
    }

    public static boolean hasNbt(CompoundTag nbt) {
        return nbt.contains(FREE_BEHAVIOR_KEY) || nbt.contains(AVOID_NIGHT_RETURN_KEY);
    }

    public static void writeToNbt(Villager villager, CompoundTag nbt) {
        Data data = DATA.get(villager.getUUID());
        if (data == null) {
            nbt.remove(FREE_BEHAVIOR_KEY);
            nbt.remove(AVOID_NIGHT_RETURN_KEY);
            return;
        }
        nbt.putBoolean(FREE_BEHAVIOR_KEY, data.freeBehavior);
        nbt.putBoolean(AVOID_NIGHT_RETURN_KEY, data.avoidNightReturn);
    }

    public static void readFromNbt(Villager villager, CompoundTag nbt) {
        if (!hasNbt(nbt)) {
            DATA.remove(villager.getUUID());
            return;
        }
        setSettings(
                villager,
                nbt.getBooleanOr(FREE_BEHAVIOR_KEY, DEFAULT_FREE_BEHAVIOR),
                nbt.getBooleanOr(AVOID_NIGHT_RETURN_KEY, DEFAULT_AVOID_NIGHT_RETURN)
        );
    }

    private static Data dataOrDefault(Villager villager) {
        return DATA.getOrDefault(villager.getUUID(), new Data(DEFAULT_FREE_BEHAVIOR, DEFAULT_AVOID_NIGHT_RETURN));
    }

    private record Data(boolean freeBehavior, boolean avoidNightReturn) {
    }

    private record Recall(long travelDeadline, long holdUntil) {
    }
}
