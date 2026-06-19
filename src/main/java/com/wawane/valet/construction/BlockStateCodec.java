package com.wawane.valet.construction;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.registry.Registries;

public final class BlockStateCodec {
    private static final String BLOCK_STATE_KEY = "BlockState";
    private static final String LEGACY_RAW_STATE_KEY = "State";

    private BlockStateCodec() {
    }

    public static void write(NbtCompound nbt, BlockState state) {
        nbt.put(BLOCK_STATE_KEY, NbtHelper.fromBlockState(state));
    }

    public static BlockState read(NbtCompound nbt) {
        if (nbt.contains(BLOCK_STATE_KEY, NbtElement.COMPOUND_TYPE)) {
            return NbtHelper.toBlockState(Registries.BLOCK.getReadOnlyWrapper(), nbt.getCompound(BLOCK_STATE_KEY));
        }
        if (nbt.contains(LEGACY_RAW_STATE_KEY)) {
            return Block.getStateFromRawId(nbt.getInt(LEGACY_RAW_STATE_KEY));
        }
        return Blocks.AIR.getDefaultState();
    }
}
