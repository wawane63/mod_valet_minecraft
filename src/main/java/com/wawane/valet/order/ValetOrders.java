package com.wawane.valet.order;

import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.nbt.NbtCompound;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ValetOrders {
    public static final int DATA_VERSION = 1;
    private static final String DATA_VERSION_KEY = "ValetOrdersDataVersion";
    private static final String ORDER_KEY = "ValetOrder";
    private static final String MINE_TARGET_KEY = "ValetMineTarget";
    private static final String WOOD_TARGET_KEY = "ValetWoodTarget";
    private static final String CONSTRUCTION_TARGET_KEY = "ValetConstructionTarget";
    private static final String CRAFT_TARGET_KEY = "ValetCraftTarget";
    private static final Map<UUID, ValetOrder> ORDERS = new ConcurrentHashMap<>();
    private static final Map<UUID, ValetMineTarget> MINE_TARGETS = new ConcurrentHashMap<>();
    private static final Map<UUID, ValetWoodTarget> WOOD_TARGETS = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> CONSTRUCTION_TARGETS = new ConcurrentHashMap<>();
    private static final Map<UUID, ValetCraftTarget> CRAFT_TARGETS = new ConcurrentHashMap<>();

    private ValetOrders() {
    }

    public static ValetOrder get(VillagerEntity villager) {
        return ORDERS.getOrDefault(villager.getUuid(), ValetOrder.NONE);
    }

    public static void set(VillagerEntity villager, ValetOrder order) {
        UUID uuid = villager.getUuid();
        if (order == ValetOrder.NONE) {
            clear(uuid);
        } else {
            ORDERS.put(uuid, order);
            if (order != ValetOrder.MINE_ORES) {
                MINE_TARGETS.remove(uuid);
            }
            if (order != ValetOrder.CHOP_WOOD) {
                WOOD_TARGETS.remove(uuid);
            }
            if (order != ValetOrder.BUILD_STRUCTURE) {
                CONSTRUCTION_TARGETS.remove(uuid);
            }
            if (order != ValetOrder.CRAFT) {
                CRAFT_TARGETS.remove(uuid);
            }
        }
    }

    public static boolean hasData(VillagerEntity villager) {
        UUID uuid = villager.getUuid();
        return ORDERS.containsKey(uuid)
                || MINE_TARGETS.containsKey(uuid)
                || WOOD_TARGETS.containsKey(uuid)
                || CONSTRUCTION_TARGETS.containsKey(uuid)
                || CRAFT_TARGETS.containsKey(uuid);
    }

    public static boolean hasNbt(NbtCompound nbt) {
        return nbt.contains(ORDER_KEY)
                || nbt.contains(DATA_VERSION_KEY)
                || nbt.contains(MINE_TARGET_KEY)
                || nbt.contains(WOOD_TARGET_KEY)
                || nbt.contains(CONSTRUCTION_TARGET_KEY)
                || nbt.contains(CRAFT_TARGET_KEY);
    }

    public static void clear(UUID uuid) {
        ORDERS.remove(uuid);
        MINE_TARGETS.remove(uuid);
        WOOD_TARGETS.remove(uuid);
        CONSTRUCTION_TARGETS.remove(uuid);
        CRAFT_TARGETS.remove(uuid);
    }

    public static void clearAll() {
        ORDERS.clear();
        MINE_TARGETS.clear();
        WOOD_TARGETS.clear();
        CONSTRUCTION_TARGETS.clear();
        CRAFT_TARGETS.clear();
    }

    public static ValetMineTarget getMineTarget(VillagerEntity villager) {
        return MINE_TARGETS.get(villager.getUuid());
    }

    public static ValetWoodTarget getWoodTarget(VillagerEntity villager) {
        return WOOD_TARGETS.get(villager.getUuid());
    }

    public static int getConstructionTargetId(VillagerEntity villager) {
        return CONSTRUCTION_TARGETS.getOrDefault(villager.getUuid(), -1);
    }

    public static ValetCraftTarget getCraftTarget(VillagerEntity villager) {
        return CRAFT_TARGETS.get(villager.getUuid());
    }

    public static void setMineTarget(VillagerEntity villager, ValetMineTarget target) {
        if (target == null) {
            set(villager, ValetOrder.NONE);
            return;
        }

        ORDERS.put(villager.getUuid(), ValetOrder.MINE_ORES);
        MINE_TARGETS.put(villager.getUuid(), target);
        WOOD_TARGETS.remove(villager.getUuid());
        CONSTRUCTION_TARGETS.remove(villager.getUuid());
        CRAFT_TARGETS.remove(villager.getUuid());
    }

    public static void setWoodTarget(VillagerEntity villager, ValetWoodTarget target) {
        if (target == null) {
            set(villager, ValetOrder.NONE);
            return;
        }

        ORDERS.put(villager.getUuid(), ValetOrder.CHOP_WOOD);
        WOOD_TARGETS.put(villager.getUuid(), target);
        MINE_TARGETS.remove(villager.getUuid());
        CONSTRUCTION_TARGETS.remove(villager.getUuid());
        CRAFT_TARGETS.remove(villager.getUuid());
    }

    public static void setConstructionTarget(VillagerEntity villager, int constructionId) {
        if (constructionId < 0) {
            set(villager, ValetOrder.NONE);
            return;
        }

        ORDERS.put(villager.getUuid(), ValetOrder.BUILD_STRUCTURE);
        CONSTRUCTION_TARGETS.put(villager.getUuid(), constructionId);
        MINE_TARGETS.remove(villager.getUuid());
        WOOD_TARGETS.remove(villager.getUuid());
        CRAFT_TARGETS.remove(villager.getUuid());
    }

    public static void setCraftTarget(VillagerEntity villager, ValetCraftTarget target) {
        if (target == null) {
            set(villager, ValetOrder.NONE);
            return;
        }

        ORDERS.put(villager.getUuid(), ValetOrder.CRAFT);
        CRAFT_TARGETS.put(villager.getUuid(), target);
        MINE_TARGETS.remove(villager.getUuid());
        WOOD_TARGETS.remove(villager.getUuid());
        CONSTRUCTION_TARGETS.remove(villager.getUuid());
    }

    public static void writeToNbt(VillagerEntity villager, NbtCompound nbt) {
        ValetOrder order = get(villager);
        if (order == ValetOrder.NONE) {
            nbt.remove(DATA_VERSION_KEY);
            nbt.remove(ORDER_KEY);
            nbt.remove(MINE_TARGET_KEY);
            nbt.remove(WOOD_TARGET_KEY);
            nbt.remove(CONSTRUCTION_TARGET_KEY);
            nbt.remove(CRAFT_TARGET_KEY);
            return;
        }

        nbt.putInt(DATA_VERSION_KEY, DATA_VERSION);
        nbt.putString(ORDER_KEY, order.getId());
        if (order == ValetOrder.MINE_ORES) {
            ValetMineTarget target = getMineTarget(villager);
            if (target == null) {
                nbt.remove(MINE_TARGET_KEY);
            } else {
                nbt.putString(MINE_TARGET_KEY, target.name());
            }
            nbt.remove(WOOD_TARGET_KEY);
            nbt.remove(CONSTRUCTION_TARGET_KEY);
            nbt.remove(CRAFT_TARGET_KEY);
        } else if (order == ValetOrder.CHOP_WOOD) {
            ValetWoodTarget target = getWoodTarget(villager);
            if (target == null) {
                nbt.remove(WOOD_TARGET_KEY);
            } else {
                nbt.putString(WOOD_TARGET_KEY, target.name());
            }
            nbt.remove(MINE_TARGET_KEY);
            nbt.remove(CONSTRUCTION_TARGET_KEY);
            nbt.remove(CRAFT_TARGET_KEY);
        } else if (order == ValetOrder.BUILD_STRUCTURE) {
            int constructionId = getConstructionTargetId(villager);
            if (constructionId < 0) {
                nbt.remove(CONSTRUCTION_TARGET_KEY);
            } else {
                nbt.putInt(CONSTRUCTION_TARGET_KEY, constructionId);
            }
            nbt.remove(MINE_TARGET_KEY);
            nbt.remove(WOOD_TARGET_KEY);
            nbt.remove(CRAFT_TARGET_KEY);
        } else if (order == ValetOrder.CRAFT) {
            ValetCraftTarget target = getCraftTarget(villager);
            if (target == null) {
                nbt.remove(CRAFT_TARGET_KEY);
            } else {
                nbt.putString(CRAFT_TARGET_KEY, target.name());
            }
            nbt.remove(MINE_TARGET_KEY);
            nbt.remove(WOOD_TARGET_KEY);
            nbt.remove(CONSTRUCTION_TARGET_KEY);
        }
    }

    public static void readFromNbt(VillagerEntity villager, NbtCompound nbt) {
        if (!nbt.contains(ORDER_KEY)) {
            set(villager, ValetOrder.NONE);
            return;
        }

        try {
            ValetOrder order = ValetOrder.fromId(nbt.getString(ORDER_KEY));
            if (order == ValetOrder.MINE_ORES && nbt.contains(MINE_TARGET_KEY)) {
                setMineTarget(villager, ValetMineTarget.valueOf(nbt.getString(MINE_TARGET_KEY)));
            } else if (order == ValetOrder.CHOP_WOOD && nbt.contains(WOOD_TARGET_KEY)) {
                setWoodTarget(villager, ValetWoodTarget.valueOf(nbt.getString(WOOD_TARGET_KEY)));
            } else if (order == ValetOrder.BUILD_STRUCTURE && nbt.contains(CONSTRUCTION_TARGET_KEY)) {
                setConstructionTarget(villager, nbt.getInt(CONSTRUCTION_TARGET_KEY));
            } else if (order == ValetOrder.CRAFT && nbt.contains(CRAFT_TARGET_KEY)) {
                setCraftTarget(villager, ValetCraftTarget.valueOf(nbt.getString(CRAFT_TARGET_KEY)));
            } else {
                set(villager, order);
            }
        } catch (IllegalArgumentException ignored) {
            set(villager, ValetOrder.NONE);
        }
    }
}
