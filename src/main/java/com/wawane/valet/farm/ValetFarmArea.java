package com.wawane.valet.farm;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

public record ValetFarmArea(int id, String name, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
    public ValetFarmArea {
        int layerY = Math.max(minY, maxY);
        minY = layerY;
        maxY = layerY;
    }

    public boolean contains(BlockPos pos) {
        return pos.getX() >= minX
                && pos.getX() <= maxX
                && pos.getY() >= minY
                && pos.getY() <= maxY
                && pos.getZ() >= minZ
                && pos.getZ() <= maxZ;
    }

    public int blockCount() {
        return (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
    }

    public CompoundTag writeNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putInt("Id", id);
        nbt.putString("Name", name);
        nbt.putInt("MinX", minX);
        nbt.putInt("MinY", minY);
        nbt.putInt("MinZ", minZ);
        nbt.putInt("MaxX", maxX);
        nbt.putInt("MaxY", maxY);
        nbt.putInt("MaxZ", maxZ);
        return nbt;
    }

    public static ValetFarmArea readNbt(CompoundTag nbt) {
        int layerY = Math.max(nbt.getIntOr("MinY", 0), nbt.getIntOr("MaxY", 0));
        return new ValetFarmArea(
                nbt.getIntOr("Id", 0),
                nbt.getString("Name").orElse("Champ"),
                nbt.getIntOr("MinX", 0),
                layerY,
                nbt.getIntOr("MinZ", 0),
                nbt.getIntOr("MaxX", 0),
                layerY,
                nbt.getIntOr("MaxZ", 0)
        );
    }
}
