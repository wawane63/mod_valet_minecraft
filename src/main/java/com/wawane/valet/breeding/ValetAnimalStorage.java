package com.wawane.valet.breeding;

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

public final class ValetAnimalStorage extends SavedData {
    private static final String KEY = "valet_animal_areas";
    private static final Codec<ValetAnimalStorage> CODEC = CompoundTag.CODEC.xmap(ValetAnimalStorage::fromNbt, ValetAnimalStorage::saveToNbt);
    private static final SavedDataType<ValetAnimalStorage> TYPE = new SavedDataType<>(
            ValetMod.id(KEY),
            ValetAnimalStorage::new,
            CODEC,
            DataFixTypes.LEVEL
    );
    public static final int MAX_AREAS = 64;

    private final List<ValetAnimalArea> areas = new ArrayList<>();
    private int nextId = 1;

    public static ValetAnimalStorage get(ServerLevel world) {
        return world.getDataStorage().computeIfAbsent(TYPE);
    }

    public ValetAnimalArea addArea(String name, int animalTypeIndex, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        if (areas.size() >= MAX_AREAS) {
            return null;
        }

        ValetAnimalArea area = new ValetAnimalArea(nextId, name, animalTypeIndex, minX, minY, minZ, maxX, maxY, maxZ);
        nextId++;
        areas.add(area);
        setDirty();
        return area;
    }

    public ValetAnimalArea getArea(int id) {
        for (ValetAnimalArea area : areas) {
            if (area.id() == id) {
                return area;
            }
        }
        return null;
    }

    public List<ValetAnimalArea> getAreas() {
        return areas.stream()
                .sorted(Comparator.comparingInt(ValetAnimalArea::id))
                .toList();
    }

    public String nextDefaultName(ValetAnimalType type) {
        return type == null ? "Enclos " + nextId : type.defaultAreaName(nextId);
    }

    private CompoundTag saveToNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putInt("NextId", nextId);
        ListTag list = new ListTag();
        for (ValetAnimalArea area : areas) {
            list.add(area.writeNbt());
        }
        nbt.put("Areas", list);
        return nbt;
    }

    private static ValetAnimalStorage fromNbt(CompoundTag nbt) {
        ValetAnimalStorage storage = new ValetAnimalStorage();
        storage.nextId = Math.max(1, nbt.getIntOr("NextId", 1));
        ListTag list = nbt.getListOrEmpty("Areas");
        for (int i = 0; i < list.size(); i++) {
            CompoundTag areaNbt = list.getCompound(i).orElse(null);
            if (areaNbt == null) {
                continue;
            }
            ValetAnimalArea area = ValetAnimalArea.readNbt(areaNbt);
            storage.areas.add(area);
            storage.nextId = Math.max(storage.nextId, area.id() + 1);
        }
        return storage;
    }
}
