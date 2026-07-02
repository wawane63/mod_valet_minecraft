package com.wawane.valet.breeding;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.AABB;

public record ValetAnimalArea(int id, String name, int animalTypeIndex, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
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
    }

    public ValetAnimalType animalType() {
        return ValetAnimalType.fromIndex(animalTypeIndex);
    }

    public boolean contains(BlockPos pos) {
        return pos.getX() >= minX
                && pos.getX() <= maxX
                && pos.getY() >= minY
                && pos.getY() <= maxY
                && pos.getZ() >= minZ
                && pos.getZ() <= maxZ;
    }

    public AABB bounds() {
        return new AABB(minX, minY, minZ, maxX + 1.0D, maxY + 1.0D, maxZ + 1.0D);
    }

    public int blockCount() {
        return (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
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
}
