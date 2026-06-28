package com.wawane.valet.construction;

import com.mojang.serialization.Codec;
import com.wawane.valet.ValetMod;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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
        if (blueprints.size() >= MAX_BLUEPRINTS) {
            return null;
        }

        ValetConstructionBlueprint blueprint = new ValetConstructionBlueprint(nextId, name, width, height, depth, entries);
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

    public boolean renameBlueprint(int id, String name) {
        String cleanName = name == null ? "" : name.trim().replaceAll("\\s+", " ");
        if (cleanName.isEmpty()) {
            return false;
        }

        for (int i = 0; i < blueprints.size(); i++) {
            ValetConstructionBlueprint blueprint = blueprints.get(i);
            if (blueprint.id() == id) {
                blueprints.set(i, new ValetConstructionBlueprint(blueprint.id(), cleanName, blueprint.width(), blueprint.height(), blueprint.depth(), blueprint.entries()));
                setDirty();
                return true;
            }
        }
        return false;
    }

    public ValetConstructionBlueprint getBlueprint(int id) {
        for (ValetConstructionBlueprint blueprint : blueprints) {
            if (blueprint.id() == id) {
                return blueprint;
            }
        }
        return null;
    }

    public List<ValetConstructionSummary> getSummaries() {
        return getBlueprints().stream()
                .map(ValetConstructionSummary::fromBlueprint)
                .toList();
    }

    public List<ValetConstructionBlueprint> getBlueprints() {
        return blueprints.stream()
                .sorted(Comparator.comparingInt(ValetConstructionBlueprint::id))
                .toList();
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
        storage.nextId = Math.max(1, nbt.getIntOr("NextId", 1));
        ListTag list = nbt.getListOrEmpty("Blueprints");
        for (int i = 0; i < list.size(); i++) {
            CompoundTag blueprintNbt = list.getCompound(i).orElse(null);
            if (blueprintNbt == null) {
                continue;
            }
            ValetConstructionBlueprint blueprint = ValetConstructionBlueprint.readNbt(blueprintNbt);
            storage.blueprints.add(blueprint);
            storage.nextId = Math.max(storage.nextId, blueprint.id() + 1);
        }
        return storage;
    }
}
