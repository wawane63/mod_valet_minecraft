package com.wawane.valet.construction;

import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtElement;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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

    public NbtCompound writeNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putInt(DATA_VERSION_KEY, DATA_VERSION);
        nbt.putInt("Id", id);
        nbt.putString("Name", name);
        nbt.putInt("Width", width);
        nbt.putInt("Height", height);
        nbt.putInt("Depth", depth);

        NbtList blockList = new NbtList();
        for (Entry entry : entries) {
            NbtCompound blockNbt = new NbtCompound();
            blockNbt.putInt("X", entry.x());
            blockNbt.putInt("Y", entry.y());
            blockNbt.putInt("Z", entry.z());
            BlockStateCodec.write(blockNbt, entry.state());
            blockList.add(blockNbt);
        }
        nbt.put("Blocks", blockList);
        return nbt;
    }

    public static ValetConstructionBlueprint readNbt(NbtCompound nbt) {
        List<Entry> entries = new ArrayList<>();
        NbtList blockList = nbt.getList("Blocks", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < blockList.size(); i++) {
            NbtCompound blockNbt = blockList.getCompound(i);
            BlockState state = BlockStateCodec.read(blockNbt);
            if (!state.isAir()) {
                entries.add(new Entry(
                        blockNbt.getInt("X"),
                        blockNbt.getInt("Y"),
                        blockNbt.getInt("Z"),
                        state
                ));
            }
        }

        return new ValetConstructionBlueprint(
                nbt.getInt("Id"),
                nbt.getString("Name"),
                nbt.getInt("Width"),
                nbt.getInt("Height"),
                nbt.getInt("Depth"),
                entries
        );
    }

    public record Entry(int x, int y, int z, BlockState state) {
    }
}
