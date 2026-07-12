package com.wawane.valet.farm;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

public record ValetFarmArea(int id, String name, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
    public static final int MAX_WIDTH = 64;
    public static final int MAX_DEPTH = 64;
    private static final int MAX_NAME_LENGTH = 48;

    public ValetFarmArea {
        int actualMinX = Math.min(minX, maxX);
        int actualMinZ = Math.min(minZ, maxZ);
        int actualMaxX = Math.max(minX, maxX);
        int actualMaxZ = Math.max(minZ, maxZ);
        int layerY = Math.max(minY, maxY);
        minX = actualMinX;
        minY = layerY;
        minZ = actualMinZ;
        maxX = actualMaxX;
        maxY = layerY;
        maxZ = actualMaxZ;
        name = cleanName(name);
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
        long count = span(minX, maxX) * span(minZ, maxZ);
        return (int) Math.min(Integer.MAX_VALUE, count);
    }

    public boolean hasValidSize() {
        return span(minX, maxX) <= MAX_WIDTH && span(minZ, maxZ) <= MAX_DEPTH;
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

    private static long span(int min, int max) {
        return (long) max - min + 1L;
    }

    private static String cleanName(String name) {
        String clean = name == null ? "" : name.trim().replaceAll("\\s+", " ");
        if (clean.isEmpty()) {
            return "Champ";
        }
        return clean.length() <= MAX_NAME_LENGTH ? clean : clean.substring(0, MAX_NAME_LENGTH);
    }
}
