package com.wawane.valet;

import net.minecraft.entity.passive.VillagerEntity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ValetConversations {
    private static final Map<UUID, Integer> OPEN_SCREENS = new ConcurrentHashMap<>();

    private ValetConversations() {
    }

    public static void begin(VillagerEntity villager) {
        OPEN_SCREENS.merge(villager.getUuid(), 1, Integer::sum);
    }

    public static void end(VillagerEntity villager) {
        end(villager.getUuid());
    }

    public static void end(UUID uuid) {
        OPEN_SCREENS.computeIfPresent(uuid, (key, count) -> count <= 1 ? null : count - 1);
    }

    public static void clear(UUID uuid) {
        OPEN_SCREENS.remove(uuid);
    }

    public static void clearAll() {
        OPEN_SCREENS.clear();
    }

    public static boolean isTalking(VillagerEntity villager) {
        return OPEN_SCREENS.getOrDefault(villager.getUuid(), 0) > 0;
    }
}
