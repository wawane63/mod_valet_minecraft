package com.wawane.valet.order;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class ValetOrders {
    public static final int DATA_VERSION = 7;
    public static final int DEFAULT_MAX_ANIMALS = 8;
    public static final boolean DEFAULT_FARM_REPLANT = true;

    private static final String DATA_VERSION_KEY = "ValetOrdersDataVersion";
    private static final String ORDER_KEY = "ValetOrder";
    private static final String MINE_TARGET_KEY = "ValetMineTarget";
    private static final String WOOD_TARGET_KEY = "ValetWoodTarget";
    private static final String FARM_AREA_KEY = "ValetFarmArea";
    private static final String FARM_CROP_MASK_KEY = "ValetFarmCropMask";
    private static final String FARM_REPLANT_KEY = "ValetFarmReplant";
    private static final String FARM_TILL_SOIL_KEY = "ValetFarmTillSoil";
    private static final String ANIMAL_AREA_KEY = "ValetAnimalArea";
    private static final String LEGACY_ANIMAL_FEED_KEY = "ValetAnimalFeed";
    private static final String ANIMAL_BREED_KEY = "ValetAnimalBreed";
    private static final String ANIMAL_SHEAR_KEY = "ValetAnimalShear";
    private static final String ANIMAL_COLLECT_EGGS_KEY = "ValetAnimalCollectEggs";
    private static final String ANIMAL_MILK_KEY = "ValetAnimalMilk";
    private static final String ANIMAL_CULL_KEY = "ValetAnimalCull";
    private static final String ANIMAL_MAX_KEY = "ValetAnimalMax";
    private static final String CONSTRUCTION_TARGET_KEY = "ValetConstructionTarget";
    private static final String CRAFT_TARGET_KEY = "ValetCraftTarget";

    private static final Map<UUID, ValetOrder> ORDERS = new ConcurrentHashMap<>();
    private static final Map<UUID, ValetMineTarget> MINE_TARGETS = new ConcurrentHashMap<>();
    private static final Map<UUID, ValetWoodTarget> WOOD_TARGETS = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> FARM_AREAS = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> FARM_CROP_MASKS = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> FARM_REPLANT = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> FARM_TILL_SOIL = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> ANIMAL_AREAS = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> ANIMAL_BREED = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> ANIMAL_SHEAR = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> ANIMAL_COLLECT_EGGS = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> ANIMAL_MILK = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> ANIMAL_CULL = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> ANIMAL_MAX = new ConcurrentHashMap<>();
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
            return;
        }

        ORDERS.put(uuid, order);
        clearNonMatchingTargets(uuid, order);
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
                || ANIMAL_AREAS.containsKey(uuid)
                || ANIMAL_BREED.containsKey(uuid)
                || ANIMAL_SHEAR.containsKey(uuid)
                || ANIMAL_COLLECT_EGGS.containsKey(uuid)
                || ANIMAL_MILK.containsKey(uuid)
                || ANIMAL_CULL.containsKey(uuid)
                || ANIMAL_MAX.containsKey(uuid)
                || CONSTRUCTION_TARGETS.containsKey(uuid)
                || CRAFT_TARGETS.containsKey(uuid);
    }

    public static boolean hasNbt(ValueInput input) {
        return input.getString(ORDER_KEY).isPresent()
                || input.getInt(DATA_VERSION_KEY).isPresent()
                || input.getString(MINE_TARGET_KEY).isPresent()
                || input.getString(WOOD_TARGET_KEY).isPresent()
                || input.getInt(FARM_AREA_KEY).isPresent()
                || input.getInt(ANIMAL_AREA_KEY).isPresent()
                || input.getInt(CONSTRUCTION_TARGET_KEY).isPresent()
                || input.getString(CRAFT_TARGET_KEY).isPresent();
    }

    public static void clear(UUID uuid) {
        ORDERS.remove(uuid);
        MINE_TARGETS.remove(uuid);
        WOOD_TARGETS.remove(uuid);
        FARM_AREAS.remove(uuid);
        FARM_CROP_MASKS.remove(uuid);
        FARM_REPLANT.remove(uuid);
        FARM_TILL_SOIL.remove(uuid);
        ANIMAL_AREAS.remove(uuid);
        ANIMAL_BREED.remove(uuid);
        ANIMAL_SHEAR.remove(uuid);
        ANIMAL_COLLECT_EGGS.remove(uuid);
        ANIMAL_MILK.remove(uuid);
        ANIMAL_CULL.remove(uuid);
        ANIMAL_MAX.remove(uuid);
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
        ANIMAL_AREAS.clear();
        ANIMAL_BREED.clear();
        ANIMAL_SHEAR.clear();
        ANIMAL_COLLECT_EGGS.clear();
        ANIMAL_MILK.clear();
        ANIMAL_CULL.clear();
        ANIMAL_MAX.clear();
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
        return FARM_REPLANT.getOrDefault(villager.getUUID(), DEFAULT_FARM_REPLANT);
    }

    public static boolean shouldTillFarm(Villager villager) {
        return FARM_TILL_SOIL.getOrDefault(villager.getUUID(), false);
    }

    public static int getAnimalAreaId(Villager villager) {
        return ANIMAL_AREAS.getOrDefault(villager.getUUID(), -1);
    }

    public static boolean shouldBreedAnimals(Villager villager) {
        return ANIMAL_BREED.getOrDefault(villager.getUUID(), true);
    }

    public static boolean shouldShearAnimals(Villager villager) {
        return ANIMAL_SHEAR.getOrDefault(villager.getUUID(), true);
    }

    public static boolean shouldCollectAnimalEggs(Villager villager) {
        return ANIMAL_COLLECT_EGGS.getOrDefault(villager.getUUID(), true);
    }

    public static boolean shouldMilkAnimals(Villager villager) {
        return ANIMAL_MILK.getOrDefault(villager.getUUID(), true);
    }

    public static boolean shouldCullAnimals(Villager villager) {
        return ANIMAL_CULL.getOrDefault(villager.getUUID(), false);
    }

    public static int getMaxAnimals(Villager villager) {
        return sanitizeMaxAnimals(ANIMAL_MAX.getOrDefault(villager.getUUID(), DEFAULT_MAX_ANIMALS));
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

        UUID uuid = villager.getUUID();
        ORDERS.put(uuid, ValetOrder.MINE_ORES);
        MINE_TARGETS.put(uuid, target);
        clearNonMatchingTargets(uuid, ValetOrder.MINE_ORES);
    }

    public static void setWoodTarget(Villager villager, ValetWoodTarget target) {
        if (target == null) {
            set(villager, ValetOrder.NONE);
            return;
        }

        UUID uuid = villager.getUUID();
        ORDERS.put(uuid, ValetOrder.CHOP_WOOD);
        WOOD_TARGETS.put(uuid, target);
        clearNonMatchingTargets(uuid, ValetOrder.CHOP_WOOD);
    }

    public static void setHarvestCrops(Villager villager, int farmAreaId, int cropMask, boolean replant, boolean tillSoil) {
        UUID uuid = villager.getUUID();
        ORDERS.put(uuid, ValetOrder.HARVEST_CROPS);
        FARM_AREAS.put(uuid, Math.max(-1, farmAreaId));
        FARM_CROP_MASKS.put(uuid, sanitizeFarmCropMask(cropMask));
        FARM_REPLANT.put(uuid, replant);
        FARM_TILL_SOIL.put(uuid, tillSoil);
        clearNonMatchingTargets(uuid, ValetOrder.HARVEST_CROPS);
    }

    public static void setBreedingAnimals(Villager villager, int animalAreaId, boolean breed, boolean shear, boolean collectEggs, boolean milk, boolean cull, int maxAnimals) {
        UUID uuid = villager.getUUID();
        ORDERS.put(uuid, ValetOrder.BREED_ANIMALS);
        ANIMAL_AREAS.put(uuid, Math.max(-1, animalAreaId));
        ANIMAL_BREED.put(uuid, breed);
        ANIMAL_SHEAR.put(uuid, shear);
        ANIMAL_COLLECT_EGGS.put(uuid, collectEggs);
        ANIMAL_MILK.put(uuid, milk);
        ANIMAL_CULL.put(uuid, cull);
        ANIMAL_MAX.put(uuid, sanitizeMaxAnimals(maxAnimals));
        clearNonMatchingTargets(uuid, ValetOrder.BREED_ANIMALS);
    }

    public static void setConstructionTarget(Villager villager, int constructionId) {
        if (constructionId < 0) {
            set(villager, ValetOrder.NONE);
            return;
        }

        UUID uuid = villager.getUUID();
        ORDERS.put(uuid, ValetOrder.BUILD_STRUCTURE);
        CONSTRUCTION_TARGETS.put(uuid, constructionId);
        clearNonMatchingTargets(uuid, ValetOrder.BUILD_STRUCTURE);
    }

    public static void setCraftTarget(Villager villager, ValetCraftTarget target) {
        if (target == null) {
            set(villager, ValetOrder.NONE);
            return;
        }

        UUID uuid = villager.getUUID();
        ORDERS.put(uuid, ValetOrder.CRAFT);
        CRAFT_TARGETS.put(uuid, target);
        clearNonMatchingTargets(uuid, ValetOrder.CRAFT);
    }

    public static void writeToNbt(Villager villager, ValueOutput output) {
        ValetOrder order = get(villager);
        if (order == ValetOrder.NONE) {
            removeOrderNbt(output);
            return;
        }

        output.putInt(DATA_VERSION_KEY, DATA_VERSION);
        output.putString(ORDER_KEY, order.getId());
        switch (order) {
            case MINE_ORES -> {
                ValetMineTarget target = getMineTarget(villager);
                if (target == null) {
                    output.discard(MINE_TARGET_KEY);
                } else {
                    output.putString(MINE_TARGET_KEY, target.name());
                }
            }
            case CHOP_WOOD -> {
                ValetWoodTarget target = getWoodTarget(villager);
                if (target == null) {
                    output.discard(WOOD_TARGET_KEY);
                } else {
                    output.putString(WOOD_TARGET_KEY, target.name());
                }
            }
            case HARVEST_CROPS -> {
                output.putInt(FARM_AREA_KEY, getFarmAreaId(villager));
                output.putInt(FARM_CROP_MASK_KEY, getFarmCropMask(villager));
                output.putBoolean(FARM_REPLANT_KEY, shouldReplantFarm(villager));
                output.putBoolean(FARM_TILL_SOIL_KEY, shouldTillFarm(villager));
            }
            case BREED_ANIMALS -> {
                output.putInt(ANIMAL_AREA_KEY, getAnimalAreaId(villager));
                output.putBoolean(ANIMAL_BREED_KEY, shouldBreedAnimals(villager));
                output.putBoolean(ANIMAL_SHEAR_KEY, shouldShearAnimals(villager));
                output.putBoolean(ANIMAL_COLLECT_EGGS_KEY, shouldCollectAnimalEggs(villager));
                output.putBoolean(ANIMAL_MILK_KEY, shouldMilkAnimals(villager));
                output.putBoolean(ANIMAL_CULL_KEY, shouldCullAnimals(villager));
                output.putInt(ANIMAL_MAX_KEY, getMaxAnimals(villager));
            }
            case BUILD_STRUCTURE -> {
                int constructionId = getConstructionTargetId(villager);
                if (constructionId < 0) {
                    output.discard(CONSTRUCTION_TARGET_KEY);
                } else {
                    output.putInt(CONSTRUCTION_TARGET_KEY, constructionId);
                }
            }
            case CRAFT -> {
                ValetCraftTarget target = getCraftTarget(villager);
                if (target == null) {
                    output.discard(CRAFT_TARGET_KEY);
                } else {
                    output.putString(CRAFT_TARGET_KEY, target.name());
                }
            }
            case NONE -> {
            }
        }
        removeNonMatchingNbt(output, order);
    }

    public static void readFromNbt(Villager villager, ValueInput input) {
        if (input.getString(ORDER_KEY).isEmpty()) {
            set(villager, ValetOrder.NONE);
            return;
        }

        int dataVersion = input.getIntOr(DATA_VERSION_KEY, 0);

        try {
            ValetOrder order = ValetOrder.fromId(input.getString(ORDER_KEY).orElse(""));
            if (order == ValetOrder.MINE_ORES && input.getString(MINE_TARGET_KEY).isPresent()) {
                setMineTarget(villager, ValetMineTarget.valueOf(input.getString(MINE_TARGET_KEY).orElse("")));
            } else if (order == ValetOrder.CHOP_WOOD && input.getString(WOOD_TARGET_KEY).isPresent()) {
                setWoodTarget(villager, ValetWoodTarget.valueOf(input.getString(WOOD_TARGET_KEY).orElse("")));
            } else if (order == ValetOrder.HARVEST_CROPS) {
                setHarvestCrops(
                        villager,
                        input.getIntOr(FARM_AREA_KEY, -1),
                        input.getIntOr(FARM_CROP_MASK_KEY, ValetFarmCrop.defaultMask()),
                        input.getBooleanOr(FARM_REPLANT_KEY, DEFAULT_FARM_REPLANT),
                        input.getBooleanOr(FARM_TILL_SOIL_KEY, false)
                );
            } else if (order == ValetOrder.BREED_ANIMALS) {
                int maxAnimals = input.getIntOr(ANIMAL_MAX_KEY, DEFAULT_MAX_ANIMALS);
                if (dataVersion < 6 && input.getInt(ANIMAL_MAX_KEY).isPresent()) {
                    maxAnimals = rebalanceMaxAnimals(maxAnimals);
                }
                setBreedingAnimals(
                        villager,
                        input.getIntOr(ANIMAL_AREA_KEY, -1),
                        input.getBooleanOr(ANIMAL_BREED_KEY, true),
                        input.getBooleanOr(ANIMAL_SHEAR_KEY, true),
                        input.getBooleanOr(ANIMAL_COLLECT_EGGS_KEY, true),
                        input.getBooleanOr(ANIMAL_MILK_KEY, true),
                        input.getBooleanOr(ANIMAL_CULL_KEY, false),
                        maxAnimals
                );
            } else if (order == ValetOrder.BUILD_STRUCTURE && input.getInt(CONSTRUCTION_TARGET_KEY).isPresent()) {
                setConstructionTarget(villager, input.getIntOr(CONSTRUCTION_TARGET_KEY, -1));
            } else if (order == ValetOrder.CRAFT && input.getString(CRAFT_TARGET_KEY).isPresent()) {
                setCraftTarget(villager, ValetCraftTarget.valueOf(input.getString(CRAFT_TARGET_KEY).orElse("")));
            } else if (order == ValetOrder.NONE) {
                set(villager, ValetOrder.NONE);
            } else {
                set(villager, ValetOrder.NONE);
            }
        } catch (IllegalArgumentException ignored) {
            set(villager, ValetOrder.NONE);
        }
    }

    private static void clearNonMatchingTargets(UUID uuid, ValetOrder order) {
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
        if (order != ValetOrder.BREED_ANIMALS) {
            ANIMAL_AREAS.remove(uuid);
            ANIMAL_BREED.remove(uuid);
            ANIMAL_SHEAR.remove(uuid);
            ANIMAL_COLLECT_EGGS.remove(uuid);
            ANIMAL_MILK.remove(uuid);
            ANIMAL_CULL.remove(uuid);
            ANIMAL_MAX.remove(uuid);
        }
        if (order != ValetOrder.BUILD_STRUCTURE) {
            CONSTRUCTION_TARGETS.remove(uuid);
        }
        if (order != ValetOrder.CRAFT) {
            CRAFT_TARGETS.remove(uuid);
        }
    }

    private static void removeOrderNbt(ValueOutput output) {
        output.discard(DATA_VERSION_KEY);
        output.discard(ORDER_KEY);
        output.discard(MINE_TARGET_KEY);
        output.discard(WOOD_TARGET_KEY);
        output.discard(FARM_AREA_KEY);
        output.discard(FARM_CROP_MASK_KEY);
        output.discard(FARM_REPLANT_KEY);
        output.discard(FARM_TILL_SOIL_KEY);
        output.discard(ANIMAL_AREA_KEY);
        output.discard(LEGACY_ANIMAL_FEED_KEY);
        output.discard(ANIMAL_BREED_KEY);
        output.discard(ANIMAL_SHEAR_KEY);
        output.discard(ANIMAL_COLLECT_EGGS_KEY);
        output.discard(ANIMAL_MILK_KEY);
        output.discard(ANIMAL_CULL_KEY);
        output.discard(ANIMAL_MAX_KEY);
        output.discard(CONSTRUCTION_TARGET_KEY);
        output.discard(CRAFT_TARGET_KEY);
    }

    private static void removeNonMatchingNbt(ValueOutput output, ValetOrder order) {
        output.discard(LEGACY_ANIMAL_FEED_KEY);
        if (order != ValetOrder.MINE_ORES) {
            output.discard(MINE_TARGET_KEY);
        }
        if (order != ValetOrder.CHOP_WOOD) {
            output.discard(WOOD_TARGET_KEY);
        }
        if (order != ValetOrder.HARVEST_CROPS) {
            output.discard(FARM_AREA_KEY);
            output.discard(FARM_CROP_MASK_KEY);
            output.discard(FARM_REPLANT_KEY);
            output.discard(FARM_TILL_SOIL_KEY);
        }
        if (order != ValetOrder.BREED_ANIMALS) {
            output.discard(ANIMAL_AREA_KEY);
            output.discard(ANIMAL_BREED_KEY);
            output.discard(ANIMAL_SHEAR_KEY);
            output.discard(ANIMAL_COLLECT_EGGS_KEY);
            output.discard(ANIMAL_MILK_KEY);
            output.discard(ANIMAL_CULL_KEY);
            output.discard(ANIMAL_MAX_KEY);
        }
        if (order != ValetOrder.BUILD_STRUCTURE) {
            output.discard(CONSTRUCTION_TARGET_KEY);
        }
        if (order != ValetOrder.CRAFT) {
            output.discard(CRAFT_TARGET_KEY);
        }
    }

    private static int sanitizeFarmCropMask(int cropMask) {
        int allowed = ValetFarmCrop.defaultMask();
        return cropMask & allowed;
    }

    private static int sanitizeMaxAnimals(int maxAnimals) {
        return Math.max(2, Math.min(64, maxAnimals));
    }

    private static int rebalanceMaxAnimals(int maxAnimals) {
        return sanitizeMaxAnimals((int) Math.round(maxAnimals * 2.0D / 3.0D));
    }
}
