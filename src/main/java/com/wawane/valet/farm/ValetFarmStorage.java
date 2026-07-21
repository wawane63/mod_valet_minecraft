package com.wawane.valet.farm;

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

public final class ValetFarmStorage extends SavedData {
    private static final String KEY = "valet_farms";
    private static final Codec<ValetFarmStorage> CODEC = CompoundTag.CODEC.xmap(ValetFarmStorage::fromNbt, ValetFarmStorage::saveToNbt);
    private static final SavedDataType<ValetFarmStorage> TYPE = new SavedDataType<>(
            ValetMod.id(KEY),
            ValetFarmStorage::new,
            CODEC,
            DataFixTypes.LEVEL
    );
    public static final int MAX_AREAS = 64;

    private final List<ValetFarmArea> areas = new ArrayList<>();
    private int nextId = 1;

    public static ValetFarmStorage get(ServerLevel world) {
        return world.getDataStorage().computeIfAbsent(TYPE);
    }

    public ValetFarmArea addArea(String name, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        if (areas.size() >= MAX_AREAS || nextId <= 0 || nextId == Integer.MAX_VALUE) {
            return null;
        }

        ValetFarmArea area = new ValetFarmArea(nextId, name, minX, minY, minZ, maxX, maxY, maxZ);
        if (!area.hasValidSize()) {
            return null;
        }
        nextId++;
        areas.add(area);
        setDirty();
        return area;
    }

    public ValetFarmArea getArea(int id) {
        for (ValetFarmArea area : areas) {
            if (area.id() == id) {
                return area;
            }
        }
        return null;
    }

    public boolean removeArea(int id) {
        boolean removed = areas.removeIf(area -> area.id() == id);
        if (removed) {
            setDirty();
        }
        return removed;
    }

    public ValetFarmArea renameArea(int id, String name) {
        for (int i = 0; i < areas.size(); i++) {
            ValetFarmArea area = areas.get(i);
            if (area.id() != id) {
                continue;
            }
            ValetFarmArea renamed = new ValetFarmArea(area.id(), name, area.minX(), area.minY(), area.minZ(), area.maxX(), area.maxY(), area.maxZ());
            areas.set(i, renamed);
            setDirty();
            return renamed;
        }
        return null;
    }

    public List<ValetFarmArea> getAreas() {
        return List.copyOf(areas);
    }

    public String nextDefaultName() {
        return "Champ " + nextId;
    }

    private CompoundTag saveToNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putInt("NextId", nextId);
        ListTag list = new ListTag();
        for (ValetFarmArea area : areas) {
            list.add(area.writeNbt());
        }
        nbt.put("Areas", list);
        return nbt;
    }

    private static ValetFarmStorage fromNbt(CompoundTag nbt) {
        ValetFarmStorage storage = new ValetFarmStorage();
        int savedNextId = nbt.getIntOr("NextId", 1);
        storage.nextId = savedNextId > 0 && savedNextId < Integer.MAX_VALUE ? savedNextId : 1;
        ListTag list = nbt.getListOrEmpty("Areas");
        Set<Integer> loadedIds = new HashSet<>();
        for (int i = 0; i < list.size() && storage.areas.size() < MAX_AREAS; i++) {
            CompoundTag areaNbt = list.getCompound(i).orElse(null);
            if (areaNbt == null) {
                continue;
            }
            ValetFarmArea area = ValetFarmArea.readNbt(areaNbt);
            if (area.id() <= 0 || area.id() == Integer.MAX_VALUE || !area.hasValidSize() || !loadedIds.add(area.id())) {
                continue;
            }
            storage.areas.add(area);
            storage.nextId = Math.max(storage.nextId, area.id() + 1);
        }
        storage.areas.sort(java.util.Comparator.comparingInt(ValetFarmArea::id));
        return storage;
    }
}
