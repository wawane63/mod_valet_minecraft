package com.wawane.valet.construction;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class BlockStateCodec {
    private static final String BLOCK_STATE_KEY = "BlockState";
    private static final String LEGACY_RAW_STATE_KEY = "State";

    private BlockStateCodec() {
    }

    public static void write(CompoundTag nbt, BlockState state) {
        nbt.put(BLOCK_STATE_KEY, NbtUtils.writeBlockState(state));
    }

    public static BlockState read(CompoundTag nbt) {
        if (nbt.contains(BLOCK_STATE_KEY)) {
            return NbtUtils.readBlockState(BuiltInRegistries.BLOCK, nbt.getCompoundOrEmpty(BLOCK_STATE_KEY));
        }
        if (nbt.contains(LEGACY_RAW_STATE_KEY)) {
            return Block.stateById(nbt.getIntOr(LEGACY_RAW_STATE_KEY, 0));
        }
        return Blocks.AIR.defaultBlockState();
    }
}
