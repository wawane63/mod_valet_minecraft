package com.wawane.valet;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.entity.npc.villager.Villager;

public final class ValetConversations {
    private static final Set<UUID> OPEN_SCREENS = ConcurrentHashMap.newKeySet();

    private ValetConversations() {
    }

    public static void begin(Villager villager) {
        OPEN_SCREENS.add(villager.getUUID());
    }

    public static void end(Villager villager) {
        end(villager.getUUID());
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

    public static boolean isTalking(Villager villager) {
        return OPEN_SCREENS.contains(villager.getUUID());
    }
}
