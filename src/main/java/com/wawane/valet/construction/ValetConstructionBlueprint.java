package com.wawane.valet.construction;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.block.state.BlockState;

public final class ValetConstructionBlueprint {
    public static final int DATA_VERSION = 1;
    private static final String DATA_VERSION_KEY = "DataVersion";

    private final int id;
    private final String name;
    private final int width;
    private final int height;
    private final int depth;
    private final List<Entry> entries;

    public ValetConstructionBlueprint(int id, String name, int width, int height, int depth, List<Entry> entries) {
        this.id = id;
        this.name = name;
        this.width = width;
        this.height = height;
        this.depth = depth;
        this.entries = entries.stream()
                .sorted(Comparator.comparingInt(Entry::y).thenComparingInt(Entry::x).thenComparingInt(Entry::z))
                .toList();
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
        List<Entry> entries = new ArrayList<>();
        ListTag blockList = nbt.getListOrEmpty("Blocks");
        for (int i = 0; i < blockList.size(); i++) {
            CompoundTag blockNbt = blockList.getCompound(i).orElse(null);
            if (blockNbt == null) {
                continue;
            }
            BlockState state = BlockStateCodec.read(blockNbt);
            if (!state.isAir()) {
                entries.add(new Entry(
                        blockNbt.getIntOr("X", 0),
                        blockNbt.getIntOr("Y", 0),
                        blockNbt.getIntOr("Z", 0),
                        state
                ));
            }
        }

        return new ValetConstructionBlueprint(
                nbt.getIntOr("Id", -1),
                nbt.getStringOr("Name", ""),
                nbt.getIntOr("Width", 0),
                nbt.getIntOr("Height", 0),
                nbt.getIntOr("Depth", 0),
                entries
        );
    }

    public record Entry(int x, int y, int z, BlockState state) {
    }
}
