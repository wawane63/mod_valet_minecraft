package com.wawane.valet;

import net.minecraft.entity.passive.VillagerEntity;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ValetConversations {
    private static final Set<UUID> OPEN_SCREENS = ConcurrentHashMap.newKeySet();

    private ValetConversations() {
    }

    public static void begin(VillagerEntity villager) {
        OPEN_SCREENS.add(villager.getUuid());
    }

    public static void end(VillagerEntity villager) {
        end(villager.getUuid());
    }

    public static void end(UUID uuid) {
        OPEN_SCREENS.remove(uuid);
    }

    public static void clear(UUID uuid) {
        OPEN_SCREENS.remove(uuid);
    }

    public static void clearAll() {
        OPEN_SCREENS.clear();
    }

    public static boolean isTalking(VillagerEntity villager) {
        return OPEN_SCREENS.contains(villager.getUuid());
    }
}
