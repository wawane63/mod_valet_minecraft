package com.wawane.valet.construction;

import com.mojang.serialization.Codec;
import com.wawane.valet.ValetMod;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public final class ValetConstructionStorage extends SavedData {
    private static final String KEY = "valet_constructions";
    private static final Codec<ValetConstructionStorage> CODEC = CompoundTag.CODEC.xmap(ValetConstructionStorage::fromNbt, ValetConstructionStorage::saveToNbt);
    private static final SavedDataType<ValetConstructionStorage> TYPE = new SavedDataType<>(
            ValetMod.id(KEY),
            ValetConstructionStorage::new,
            CODEC,
            DataFixTypes.LEVEL
    );
    public static final int MAX_BLUEPRINTS = 64;

    private final List<ValetConstructionBlueprint> blueprints = new ArrayList<>();
    private int nextId = 1;

    public static ValetConstructionStorage get(ServerLevel world) {
        return world.getDataStorage().computeIfAbsent(TYPE);
    }

    public ValetConstructionBlueprint addBlueprint(String name, int width, int height, int depth, List<ValetConstructionBlueprint.Entry> entries) {
        if (blueprints.size() >= MAX_BLUEPRINTS
                || nextId <= 0
                || nextId == Integer.MAX_VALUE
                || !ValetConstructionBlueprint.hasValidDimensions(width, height, depth)
                || entries == null
                || entries.isEmpty()
                || entries.size() > ValetConstructionBlueprint.MAX_BLOCKS) {
            return null;
        }

        ValetConstructionBlueprint blueprint = new ValetConstructionBlueprint(nextId, name, width, height, depth, entries);
        if (!blueprint.isValid()) {
            return null;
        }
        nextId++;
        blueprints.add(blueprint);
        setDirty();
        return blueprint;
    }

    public boolean removeBlueprint(int id) {
        boolean removed = blueprints.removeIf(blueprint -> blueprint.id() == id);
        if (removed) {
            setDirty();
        }
        return removed;
    }

    public ValetConstructionBlueprint getBlueprint(int id) {
        for (ValetConstructionBlueprint blueprint : blueprints) {
            if (blueprint.id() == id) {
                return blueprint;
            }
        }
        return null;
    }

    public List<ValetConstructionBlueprint> getBlueprints() {
        return List.copyOf(blueprints);
    }

    public String nextDefaultName() {
        return "Construction " + nextId;
    }

    private CompoundTag saveToNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putInt("NextId", nextId);
        ListTag list = new ListTag();
        for (ValetConstructionBlueprint blueprint : blueprints) {
            list.add(blueprint.writeNbt());
        }
        nbt.put("Blueprints", list);
        return nbt;
    }

    private static ValetConstructionStorage fromNbt(CompoundTag nbt) {
        ValetConstructionStorage storage = new ValetConstructionStorage();
        int savedNextId = nbt.getIntOr("NextId", 1);
        storage.nextId = savedNextId > 0 && savedNextId < Integer.MAX_VALUE ? savedNextId : 1;
        ListTag list = nbt.getListOrEmpty("Blueprints");
        Set<Integer> loadedIds = new HashSet<>();
        for (int i = 0; i < list.size() && storage.blueprints.size() < MAX_BLUEPRINTS; i++) {
            CompoundTag blueprintNbt = list.getCompound(i).orElse(null);
            if (blueprintNbt == null) {
                continue;
            }
            ValetConstructionBlueprint blueprint = ValetConstructionBlueprint.readNbt(blueprintNbt);
            if (!blueprint.isValid() || blueprint.id() == Integer.MAX_VALUE || !loadedIds.add(blueprint.id())) {
                continue;
            }
            storage.blueprints.add(blueprint);
            storage.nextId = Math.max(storage.nextId, blueprint.id() + 1);
        }
        storage.blueprints.sort(java.util.Comparator.comparingInt(ValetConstructionBlueprint::id));
        return storage;
    }
}
