package com.wawane.valet.order;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.npc.villager.Villager;

public final class ValetOrders {
    public static final int DATA_VERSION = 3;
    private static final String DATA_VERSION_KEY = "ValetOrdersDataVersion";
    private static final String ORDER_KEY = "ValetOrder";
    private static final String MINE_TARGET_KEY = "ValetMineTarget";
    private static final String WOOD_TARGET_KEY = "ValetWoodTarget";
    private static final String FARM_AREA_KEY = "ValetFarmArea";
    private static final String FARM_CROP_MASK_KEY = "ValetFarmCropMask";
    private static final String FARM_REPLANT_KEY = "ValetFarmReplant";
    private static final String FARM_TILL_SOIL_KEY = "ValetFarmTillSoil";
    private static final String CONSTRUCTION_TARGET_KEY = "ValetConstructionTarget";
    private static final String CRAFT_TARGET_KEY = "ValetCraftTarget";
    private static final Map<UUID, ValetOrder> ORDERS = new ConcurrentHashMap<>();
    private static final Map<UUID, ValetMineTarget> MINE_TARGETS = new ConcurrentHashMap<>();
    private static final Map<UUID, ValetWoodTarget> WOOD_TARGETS = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> FARM_AREAS = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> FARM_CROP_MASKS = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> FARM_REPLANT = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> FARM_TILL_SOIL = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> CONSTRUCTION_TARGETS = new ConcurrentHashMap<>();
    private static final Map<UUID, ValetCraftTarget> CRAFT_TARGETS = new ConcurrentHashMap<>();

    private ValetOrders() {
    }

    public static ValetOrder get(Villager villager) {
        return ORDERS.getOrDefault(villager.getUUID(), ValetOrder.NONE);
    }

    public static void set(Villager villager, ValetOrder order) {
        UUID uuid = villager.getUUID();
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
            if (order != ValetOrder.HARVEST_CROPS) {
                FARM_AREAS.remove(uuid);
                FARM_CROP_MASKS.remove(uuid);
                FARM_REPLANT.remove(uuid);
                FARM_TILL_SOIL.remove(uuid);
            }
            if (order != ValetOrder.BUILD_STRUCTURE) {
                CONSTRUCTION_TARGETS.remove(uuid);
            }
            if (order != ValetOrder.CRAFT) {
                CRAFT_TARGETS.remove(uuid);
            }
        }
    }

    public static boolean hasData(Villager villager) {
        UUID uuid = villager.getUUID();
        return ORDERS.containsKey(uuid)
                || MINE_TARGETS.containsKey(uuid)
                || WOOD_TARGETS.containsKey(uuid)
                || FARM_AREAS.containsKey(uuid)
                || FARM_CROP_MASKS.containsKey(uuid)
                || FARM_REPLANT.containsKey(uuid)
                || FARM_TILL_SOIL.containsKey(uuid)
                || CONSTRUCTION_TARGETS.containsKey(uuid)
                || CRAFT_TARGETS.containsKey(uuid);
    }

    public static boolean hasNbt(CompoundTag nbt) {
        return nbt.contains(ORDER_KEY)
                || nbt.contains(DATA_VERSION_KEY)
                || nbt.contains(MINE_TARGET_KEY)
                || nbt.contains(WOOD_TARGET_KEY)
                || nbt.contains(FARM_AREA_KEY)
                || nbt.contains(FARM_CROP_MASK_KEY)
                || nbt.contains(FARM_REPLANT_KEY)
                || nbt.contains(FARM_TILL_SOIL_KEY)
                || nbt.contains(CONSTRUCTION_TARGET_KEY)
                || nbt.contains(CRAFT_TARGET_KEY);
    }

    public static void clear(UUID uuid) {
        ORDERS.remove(uuid);
        MINE_TARGETS.remove(uuid);
        WOOD_TARGETS.remove(uuid);
        FARM_AREAS.remove(uuid);
        FARM_CROP_MASKS.remove(uuid);
        FARM_REPLANT.remove(uuid);
        FARM_TILL_SOIL.remove(uuid);
        CONSTRUCTION_TARGETS.remove(uuid);
        CRAFT_TARGETS.remove(uuid);
    }

    public static void clearAll() {
        ORDERS.clear();
        MINE_TARGETS.clear();
        WOOD_TARGETS.clear();
        FARM_AREAS.clear();
        FARM_CROP_MASKS.clear();
        FARM_REPLANT.clear();
        FARM_TILL_SOIL.clear();
        CONSTRUCTION_TARGETS.clear();
        CRAFT_TARGETS.clear();
    }

    public static ValetMineTarget getMineTarget(Villager villager) {
        return MINE_TARGETS.get(villager.getUUID());
    }

    public static ValetWoodTarget getWoodTarget(Villager villager) {
        return WOOD_TARGETS.get(villager.getUUID());
    }

    public static int getFarmAreaId(Villager villager) {
        return FARM_AREAS.getOrDefault(villager.getUUID(), -1);
    }

    public static int getFarmCropMask(Villager villager) {
        return sanitizeFarmCropMask(FARM_CROP_MASKS.getOrDefault(villager.getUUID(), ValetFarmCrop.defaultMask()));
    }

    public static boolean shouldReplantFarm(Villager villager) {
        return FARM_REPLANT.getOrDefault(villager.getUUID(), false);
    }

    public static boolean shouldTillFarm(Villager villager) {
        return FARM_TILL_SOIL.getOrDefault(villager.getUUID(), false);
    }

    public static int getConstructionTargetId(Villager villager) {
        return CONSTRUCTION_TARGETS.getOrDefault(villager.getUUID(), -1);
    }

    public static ValetCraftTarget getCraftTarget(Villager villager) {
        return CRAFT_TARGETS.get(villager.getUUID());
    }

    public static void setMineTarget(Villager villager, ValetMineTarget target) {
        if (target == null) {
            set(villager, ValetOrder.NONE);
            return;
        }

        ORDERS.put(villager.getUUID(), ValetOrder.MINE_ORES);
        MINE_TARGETS.put(villager.getUUID(), target);
        WOOD_TARGETS.remove(villager.getUUID());
        FARM_AREAS.remove(villager.getUUID());
        FARM_CROP_MASKS.remove(villager.getUUID());
        FARM_REPLANT.remove(villager.getUUID());
        FARM_TILL_SOIL.remove(villager.getUUID());
        CONSTRUCTION_TARGETS.remove(villager.getUUID());
        CRAFT_TARGETS.remove(villager.getUUID());
    }

    public static void setWoodTarget(Villager villager, ValetWoodTarget target) {
        if (target == null) {
            set(villager, ValetOrder.NONE);
            return;
        }

        ORDERS.put(villager.getUUID(), ValetOrder.CHOP_WOOD);
        WOOD_TARGETS.put(villager.getUUID(), target);
        MINE_TARGETS.remove(villager.getUUID());
        FARM_AREAS.remove(villager.getUUID());
        FARM_CROP_MASKS.remove(villager.getUUID());
        FARM_REPLANT.remove(villager.getUUID());
        FARM_TILL_SOIL.remove(villager.getUUID());
        CONSTRUCTION_TARGETS.remove(villager.getUUID());
        CRAFT_TARGETS.remove(villager.getUUID());
    }

    public static void setHarvestCrops(Villager villager, int farmAreaId, int cropMask, boolean replant, boolean tillSoil) {
        ORDERS.put(villager.getUUID(), ValetOrder.HARVEST_CROPS);
        FARM_AREAS.put(villager.getUUID(), Math.max(-1, farmAreaId));
        FARM_CROP_MASKS.put(villager.getUUID(), sanitizeFarmCropMask(cropMask));
        FARM_REPLANT.put(villager.getUUID(), replant);
        FARM_TILL_SOIL.put(villager.getUUID(), tillSoil);
        MINE_TARGETS.remove(villager.getUUID());
        WOOD_TARGETS.remove(villager.getUUID());
        CONSTRUCTION_TARGETS.remove(villager.getUUID());
        CRAFT_TARGETS.remove(villager.getUUID());
    }

    public static void setConstructionTarget(Villager villager, int constructionId) {
        if (constructionId < 0) {
            set(villager, ValetOrder.NONE);
            return;
        }

        ORDERS.put(villager.getUUID(), ValetOrder.BUILD_STRUCTURE);
        CONSTRUCTION_TARGETS.put(villager.getUUID(), constructionId);
        MINE_TARGETS.remove(villager.getUUID());
        WOOD_TARGETS.remove(villager.getUUID());
        FARM_AREAS.remove(villager.getUUID());
        FARM_CROP_MASKS.remove(villager.getUUID());
        FARM_REPLANT.remove(villager.getUUID());
        FARM_TILL_SOIL.remove(villager.getUUID());
        CRAFT_TARGETS.remove(villager.getUUID());
    }

    public static void setCraftTarget(Villager villager, ValetCraftTarget target) {
        if (target == null) {
            set(villager, ValetOrder.NONE);
            return;
        }

        ORDERS.put(villager.getUUID(), ValetOrder.CRAFT);
        CRAFT_TARGETS.put(villager.getUUID(), target);
        MINE_TARGETS.remove(villager.getUUID());
        WOOD_TARGETS.remove(villager.getUUID());
        FARM_AREAS.remove(villager.getUUID());
        FARM_CROP_MASKS.remove(villager.getUUID());
        FARM_REPLANT.remove(villager.getUUID());
        FARM_TILL_SOIL.remove(villager.getUUID());
        CONSTRUCTION_TARGETS.remove(villager.getUUID());
    }

    public static void writeToNbt(Villager villager, CompoundTag nbt) {
        ValetOrder order = get(villager);
        if (order == ValetOrder.NONE) {
            nbt.remove(DATA_VERSION_KEY);
            nbt.remove(ORDER_KEY);
            nbt.remove(MINE_TARGET_KEY);
            nbt.remove(WOOD_TARGET_KEY);
            nbt.remove(FARM_AREA_KEY);
            nbt.remove(FARM_CROP_MASK_KEY);
            nbt.remove(FARM_REPLANT_KEY);
            nbt.remove(FARM_TILL_SOIL_KEY);
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
            nbt.remove(FARM_AREA_KEY);
            nbt.remove(FARM_CROP_MASK_KEY);
            nbt.remove(FARM_REPLANT_KEY);
            nbt.remove(FARM_TILL_SOIL_KEY);
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
            nbt.remove(FARM_AREA_KEY);
            nbt.remove(FARM_CROP_MASK_KEY);
            nbt.remove(FARM_REPLANT_KEY);
            nbt.remove(FARM_TILL_SOIL_KEY);
            nbt.remove(CONSTRUCTION_TARGET_KEY);
            nbt.remove(CRAFT_TARGET_KEY);
        } else if (order == ValetOrder.HARVEST_CROPS) {
            nbt.putInt(FARM_AREA_KEY, getFarmAreaId(villager));
            nbt.putInt(FARM_CROP_MASK_KEY, getFarmCropMask(villager));
            nbt.putBoolean(FARM_REPLANT_KEY, shouldReplantFarm(villager));
            nbt.putBoolean(FARM_TILL_SOIL_KEY, shouldTillFarm(villager));
            nbt.remove(MINE_TARGET_KEY);
            nbt.remove(WOOD_TARGET_KEY);
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
            nbt.remove(FARM_AREA_KEY);
            nbt.remove(FARM_CROP_MASK_KEY);
            nbt.remove(FARM_REPLANT_KEY);
            nbt.remove(FARM_TILL_SOIL_KEY);
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
            nbt.remove(FARM_AREA_KEY);
            nbt.remove(FARM_CROP_MASK_KEY);
            nbt.remove(FARM_REPLANT_KEY);
            nbt.remove(FARM_TILL_SOIL_KEY);
            nbt.remove(CONSTRUCTION_TARGET_KEY);
        }
    }

    public static void readFromNbt(Villager villager, CompoundTag nbt) {
        if (!nbt.contains(ORDER_KEY)) {
            set(villager, ValetOrder.NONE);
            return;
        }

        try {
            ValetOrder order = ValetOrder.fromId(nbt.getString(ORDER_KEY).orElse(""));
            if (order == ValetOrder.MINE_ORES && nbt.contains(MINE_TARGET_KEY)) {
                setMineTarget(villager, ValetMineTarget.valueOf(nbt.getString(MINE_TARGET_KEY).orElse("")));
            } else if (order == ValetOrder.CHOP_WOOD && nbt.contains(WOOD_TARGET_KEY)) {
                setWoodTarget(villager, ValetWoodTarget.valueOf(nbt.getString(WOOD_TARGET_KEY).orElse("")));
            } else if (order == ValetOrder.HARVEST_CROPS) {
                setHarvestCrops(
                        villager,
                        nbt.getIntOr(FARM_AREA_KEY, -1),
                        nbt.getIntOr(FARM_CROP_MASK_KEY, ValetFarmCrop.defaultMask()),
                        nbt.getBooleanOr(FARM_REPLANT_KEY, false),
                        nbt.getBooleanOr(FARM_TILL_SOIL_KEY, false)
                );
            } else if (order == ValetOrder.BUILD_STRUCTURE && nbt.contains(CONSTRUCTION_TARGET_KEY)) {
                setConstructionTarget(villager, nbt.getIntOr(CONSTRUCTION_TARGET_KEY, -1));
            } else if (order == ValetOrder.CRAFT && nbt.contains(CRAFT_TARGET_KEY)) {
                setCraftTarget(villager, ValetCraftTarget.valueOf(nbt.getString(CRAFT_TARGET_KEY).orElse("")));
            } else {
                set(villager, order);
            }
        } catch (IllegalArgumentException ignored) {
            set(villager, ValetOrder.NONE);
        }
    }

    private static int sanitizeFarmCropMask(int cropMask) {
        int allowed = ValetFarmCrop.defaultMask();
        return cropMask & allowed;
    }
}
