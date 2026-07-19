package com.wawane.valet.state;

import com.mojang.serialization.Codec;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class ValetBehavior {
    private static final boolean DEFAULT_FREE_BEHAVIOR = false;
    private static final boolean DEFAULT_AVOID_NIGHT_RETURN = false;
    private static final int RECALL_HOLD_TICKS = 20 * 5;
    private static final int RECALL_MAX_TRAVEL_TICKS = 20 * 60;
    private static final String FREE_BEHAVIOR_KEY = "ValetFreeBehavior";
    private static final String AVOID_NIGHT_RETURN_KEY = "ValetAvoidNightReturn";
    private static final Data DEFAULT_DATA = new Data(DEFAULT_FREE_BEHAVIOR, DEFAULT_AVOID_NIGHT_RETURN);
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

    public static void recallToAnchor(ServerLevel world, Villager villager) {
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

    public static boolean hasNbt(ValueInput input) {
        return input.read(FREE_BEHAVIOR_KEY, Codec.BOOL).isPresent()
                || input.read(AVOID_NIGHT_RETURN_KEY, Codec.BOOL).isPresent();
    }

    public static void writeToNbt(Villager villager, ValueOutput output) {
        Data data = DATA.get(villager.getUUID());
        if (data == null) {
            output.discard(FREE_BEHAVIOR_KEY);
            output.discard(AVOID_NIGHT_RETURN_KEY);
            return;
        }
        output.putBoolean(FREE_BEHAVIOR_KEY, data.freeBehavior);
        output.putBoolean(AVOID_NIGHT_RETURN_KEY, data.avoidNightReturn);
    }

    public static void readFromNbt(Villager villager, ValueInput input) {
        if (!hasNbt(input)) {
            DATA.remove(villager.getUUID());
            return;
        }
        setSettings(
                villager,
                input.getBooleanOr(FREE_BEHAVIOR_KEY, DEFAULT_FREE_BEHAVIOR),
                input.getBooleanOr(AVOID_NIGHT_RETURN_KEY, DEFAULT_AVOID_NIGHT_RETURN)
        );
    }

    private static Data dataOrDefault(Villager villager) {
        return DATA.getOrDefault(villager.getUUID(), DEFAULT_DATA);
    }

    private record Data(boolean freeBehavior, boolean avoidNightReturn) {
    }

    private record Recall(long travelDeadline, long holdUntil) {
    }
}
