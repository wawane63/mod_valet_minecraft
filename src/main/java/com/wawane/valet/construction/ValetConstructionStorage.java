package com.wawane.valet.construction;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ValetConstructionStorage extends PersistentState {
    private static final String KEY = "valet_constructions";
    public static final int MAX_BLUEPRINTS = 64;

    private final List<ValetConstructionBlueprint> blueprints = new ArrayList<>();
    private int nextId = 1;

    public static ValetConstructionStorage get(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(
                ValetConstructionStorage::fromNbt,
                ValetConstructionStorage::new,
                KEY
        );
    }

    public ValetConstructionBlueprint addBlueprint(String name, int width, int height, int depth, List<ValetConstructionBlueprint.Entry> entries) {
        if (blueprints.size() >= MAX_BLUEPRINTS) {
            return null;
        }

        ValetConstructionBlueprint blueprint = new ValetConstructionBlueprint(nextId, name, width, height, depth, entries);
        nextId++;
        blueprints.add(blueprint);
        markDirty();
        return blueprint;
    }

    public boolean removeBlueprint(int id) {
        boolean removed = blueprints.removeIf(blueprint -> blueprint.id() == id);
        if (removed) {
            markDirty();
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
                markDirty();
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

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        nbt.putInt("NextId", nextId);
        NbtList list = new NbtList();
        for (ValetConstructionBlueprint blueprint : blueprints) {
            list.add(blueprint.writeNbt());
        }
        nbt.put("Blueprints", list);
        return nbt;
    }

    private static ValetConstructionStorage fromNbt(NbtCompound nbt) {
        ValetConstructionStorage storage = new ValetConstructionStorage();
        storage.nextId = Math.max(1, nbt.getInt("NextId"));
        NbtList list = nbt.getList("Blueprints", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < list.size(); i++) {
            ValetConstructionBlueprint blueprint = ValetConstructionBlueprint.readNbt(list.getCompound(i));
            storage.blueprints.add(blueprint);
            storage.nextId = Math.max(storage.nextId, blueprint.id() + 1);
        }
        return storage;
    }
}
