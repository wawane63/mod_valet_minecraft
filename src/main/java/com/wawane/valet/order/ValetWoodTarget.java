package com.wawane.valet.order;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public enum ValetWoodTarget {
    OAK("wood.valet.oak", Blocks.OAK_LOG),
    SPRUCE("wood.valet.spruce", Blocks.SPRUCE_LOG),
    BIRCH("wood.valet.birch", Blocks.BIRCH_LOG),
    JUNGLE("wood.valet.jungle", Blocks.JUNGLE_LOG),
    ACACIA("wood.valet.acacia", Blocks.ACACIA_LOG),
    DARK_OAK("wood.valet.dark_oak", Blocks.DARK_OAK_LOG),
    MANGROVE("wood.valet.mangrove", Blocks.MANGROVE_LOG),
    CHERRY("wood.valet.cherry", Blocks.CHERRY_LOG),
    CRIMSON("wood.valet.crimson", Blocks.CRIMSON_STEM),
    WARPED("wood.valet.warped", Blocks.WARPED_STEM);

    private final String translationKey;
    private final Block[] blocks;

    ValetWoodTarget(String translationKey, Block... blocks) {
        this.translationKey = translationKey;
        this.blocks = blocks;
    }

    public String getTranslationKey() {
        return translationKey;
    }

    public boolean matches(BlockState state) {
        for (Block block : blocks) {
            if (state.isOf(block)) {
                return true;
            }
        }
        return false;
    }

    public boolean matchesNaturalTree(World world, BlockPos pos) {
        return matches(world.getBlockState(pos)) && hasTreeCrown(world, pos);
    }

    private boolean hasTreeCrown(World world, BlockPos pos) {
        for (BlockPos candidate : BlockPos.iterate(
                pos.getX() - 3,
                pos.getY(),
                pos.getZ() - 3,
                pos.getX() + 3,
                pos.getY() + 7,
                pos.getZ() + 3
        )) {
            BlockState state = world.getBlockState(candidate);
            if (state.isIn(BlockTags.LEAVES)
                    || state.isOf(Blocks.NETHER_WART_BLOCK)
                    || state.isOf(Blocks.WARPED_WART_BLOCK)) {
                return true;
            }
        }
        return false;
    }

    public static ValetWoodTarget fromIndex(int index) {
        ValetWoodTarget[] values = values();
        if (index < 0 || index >= values.length) {
            return null;
        }
        return values[index];
    }
}
