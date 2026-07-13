package com.wawane.valet.state;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Identite persistante du valet, independante de sa profession vanilla et de son poste. */
public final class ValetIdentity {
    private static final String TAG_KEY = "ValetTagged";
    private static final Set<UUID> TAGGED = ConcurrentHashMap.newKeySet();

    private ValetIdentity() {
    }

    public static boolean isTagged(Villager villager) {
        return TAGGED.contains(villager.getUUID());
    }

    public static void tag(Villager villager) {
        TAGGED.add(villager.getUUID());
    }

    public static void clear(UUID uuid) {
        TAGGED.remove(uuid);
    }

    public static void clearAll() {
        TAGGED.clear();
    }

    public static boolean hasNbt(ValueInput input) {
        return input.getBooleanOr(TAG_KEY, false);
    }

    public static void writeToNbt(Villager villager, ValueOutput output) {
        if (isTagged(villager)) {
            output.putBoolean(TAG_KEY, true);
        }
    }

    public static void readFromNbt(Villager villager, ValueInput input) {
        if (hasNbt(input)) {
            tag(villager);
        } else {
            clear(villager.getUUID());
        }
    }
}
