package com.wawane.valet.construction;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.block.state.BlockState;

public final class ValetConstructionBlueprint {
    public static final int DATA_VERSION = 1;
    public static final int MAX_HEIGHT = 64;
    public static final int MAX_VOLUME = 24000;
    public static final int MAX_BLOCKS = 12000;
    public static final int MAX_NAME_LENGTH = 48;
    private static final String DATA_VERSION_KEY = "DataVersion";

    private final int id;
    private final String name;
    private final int width;
    private final int height;
    private final int depth;
    private final List<Entry> entries;

    public ValetConstructionBlueprint(int id, String name, int width, int height, int depth, List<Entry> entries) {
        this.id = id;
        this.name = cleanName(name, id);
        boolean validDimensions = hasValidDimensions(width, height, depth);
        this.width = validDimensions ? width : 0;
        this.height = validDimensions ? height : 0;
        this.depth = validDimensions ? depth : 0;
        if (!validDimensions || entries == null) {
            this.entries = List.of();
        } else {
            this.entries = entries.stream()
                    .filter(entry -> isValidEntry(entry, width, height, depth))
                    .limit(MAX_BLOCKS)
                    .sorted(Comparator.comparingInt(Entry::y).thenComparingInt(Entry::x).thenComparingInt(Entry::z))
                    .toList();
        }
    }

    public int id() {
        return id;
    }

    public String name() {
        return name;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public int depth() {
        return depth;
    }

    public List<Entry> entries() {
        return entries;
    }

    public int blockCount() {
        return entries.size();
    }

    public boolean isValid() {
        return id > 0 && hasValidDimensions(width, height, depth) && !entries.isEmpty();
    }

    public static boolean hasValidDimensions(int width, int height, int depth) {
        return width > 0
                && height > 0
                && height <= MAX_HEIGHT
                && depth > 0
                && (long) width * height * depth <= MAX_VOLUME;
    }

    public CompoundTag writeNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putInt(DATA_VERSION_KEY, DATA_VERSION);
        nbt.putInt("Id", id);
        nbt.putString("Name", name);
        nbt.putInt("Width", width);
        nbt.putInt("Height", height);
        nbt.putInt("Depth", depth);

        ListTag blockList = new ListTag();
        for (Entry entry : entries) {
            CompoundTag blockNbt = new CompoundTag();
            blockNbt.putInt("X", entry.x());
            blockNbt.putInt("Y", entry.y());
            blockNbt.putInt("Z", entry.z());
            BlockStateCodec.write(blockNbt, entry.state());
            blockList.add(blockNbt);
        }
        nbt.put("Blocks", blockList);
        return nbt;
    }

    public static ValetConstructionBlueprint readNbt(CompoundTag nbt) {
        int id = nbt.getIntOr("Id", -1);
        String name = nbt.getStringOr("Name", "");
        int width = nbt.getIntOr("Width", 0);
        int height = nbt.getIntOr("Height", 0);
        int depth = nbt.getIntOr("Depth", 0);
        if (!hasValidDimensions(width, height, depth)) {
            return new ValetConstructionBlueprint(id, name, 0, 0, 0, List.of());
        }

        List<Entry> entries = new ArrayList<>();
        Set<Long> occupiedPositions = new HashSet<>();
        ListTag blockList = nbt.getListOrEmpty("Blocks");
        for (int i = 0; i < blockList.size() && entries.size() < MAX_BLOCKS; i++) {
            CompoundTag blockNbt = blockList.getCompound(i).orElse(null);
            if (blockNbt == null) {
                continue;
            }
            int x = blockNbt.getIntOr("X", -1);
            int y = blockNbt.getIntOr("Y", -1);
            int z = blockNbt.getIntOr("Z", -1);
            if (x < 0 || x >= width || y < 0 || y >= height || z < 0 || z >= depth) {
                continue;
            }
            if (!occupiedPositions.add(BlockPos.asLong(x, y, z))) {
                continue;
            }
            BlockState state = BlockStateCodec.read(blockNbt);
            if (!state.isAir()) {
                entries.add(new Entry(x, y, z, state));
            }
        }

        return new ValetConstructionBlueprint(id, name, width, height, depth, entries);
    }

    private static boolean isValidEntry(Entry entry, int width, int height, int depth) {
        return entry != null
                && entry.state() != null
                && !entry.state().isAir()
                && entry.x() >= 0
                && entry.x() < width
                && entry.y() >= 0
                && entry.y() < height
                && entry.z() >= 0
                && entry.z() < depth;
    }

    static String cleanName(String name, int id) {
        String clean = name == null ? "" : name.trim().replaceAll("\\s+", " ");
        if (clean.isEmpty()) {
            clean = id > 0 ? "Construction " + id : "Construction";
        }
        return clean.length() <= MAX_NAME_LENGTH ? clean : clean.substring(0, MAX_NAME_LENGTH);
    }

    public record Entry(int x, int y, int z, BlockState state) {
    }
}
