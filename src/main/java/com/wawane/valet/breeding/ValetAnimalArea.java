package com.wawane.valet.breeding;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.AABB;

public record ValetAnimalArea(int id, String name, int animalTypeIndex, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
    public static final int MAX_WIDTH = 64;
    public static final int MAX_HEIGHT = 24;
    public static final int MAX_DEPTH = 64;
    private static final int MAX_NAME_LENGTH = 48;

    public ValetAnimalArea {
        int actualMinX = Math.min(minX, maxX);
        int actualMinY = Math.min(minY, maxY);
        int actualMinZ = Math.min(minZ, maxZ);
        int actualMaxX = Math.max(minX, maxX);
        int actualMaxY = Math.max(minY, maxY);
        int actualMaxZ = Math.max(minZ, maxZ);
        minX = actualMinX;
        minY = actualMinY;
        minZ = actualMinZ;
        maxX = actualMaxX;
        maxY = actualMaxY;
        maxZ = actualMaxZ;
        name = cleanName(name);
        if (ValetAnimalType.fromIndex(animalTypeIndex) == null) {
            animalTypeIndex = -1;
        }
    }

    public ValetAnimalType animalType() {
        return ValetAnimalType.fromIndex(animalTypeIndex);
    }

    public AABB bounds() {
        return new AABB(minX, minY, minZ, maxX + 1.0D, maxY + 1.0D, maxZ + 1.0D);
    }

    public boolean hasValidSize() {
        return span(minX, maxX) <= MAX_WIDTH
                && span(minY, maxY) <= MAX_HEIGHT
                && span(minZ, maxZ) <= MAX_DEPTH;
    }

    public CompoundTag writeNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putInt("Id", id);
        nbt.putString("Name", name);
        nbt.putInt("AnimalType", animalTypeIndex);
        nbt.putInt("MinX", minX);
        nbt.putInt("MinY", minY);
        nbt.putInt("MinZ", minZ);
        nbt.putInt("MaxX", maxX);
        nbt.putInt("MaxY", maxY);
        nbt.putInt("MaxZ", maxZ);
        return nbt;
    }

    public static ValetAnimalArea readNbt(CompoundTag nbt) {
        return new ValetAnimalArea(
                nbt.getIntOr("Id", 0),
                nbt.getString("Name").orElse("Enclos"),
                nbt.getIntOr("AnimalType", -1),
                nbt.getIntOr("MinX", 0),
                nbt.getIntOr("MinY", 0),
                nbt.getIntOr("MinZ", 0),
                nbt.getIntOr("MaxX", 0),
                nbt.getIntOr("MaxY", 0),
                nbt.getIntOr("MaxZ", 0)
        );
    }

    private static long span(int min, int max) {
        return (long) max - min + 1L;
    }

    private static String cleanName(String name) {
        String clean = name == null ? "" : name.trim().replaceAll("\\s+", " ");
        if (clean.isEmpty()) {
            return "Enclos";
        }
        return clean.length() <= MAX_NAME_LENGTH ? clean : clean.substring(0, MAX_NAME_LENGTH);
    }
}
