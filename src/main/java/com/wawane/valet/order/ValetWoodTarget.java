package com.wawane.valet.order;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;

public enum ValetWoodTarget {
    OAK("wood.valet.oak", Blocks.OAK_LOG, Blocks.OAK_WOOD),
    SPRUCE("wood.valet.spruce", Blocks.SPRUCE_LOG, Blocks.SPRUCE_WOOD),
    BIRCH("wood.valet.birch", Blocks.BIRCH_LOG, Blocks.BIRCH_WOOD),
    JUNGLE("wood.valet.jungle", Blocks.JUNGLE_LOG, Blocks.JUNGLE_WOOD),
    ACACIA("wood.valet.acacia", Blocks.ACACIA_LOG, Blocks.ACACIA_WOOD),
    DARK_OAK("wood.valet.dark_oak", Blocks.DARK_OAK_LOG, Blocks.DARK_OAK_WOOD),
    MANGROVE("wood.valet.mangrove", Blocks.MANGROVE_LOG, Blocks.MANGROVE_WOOD),
    CHERRY("wood.valet.cherry", Blocks.CHERRY_LOG, Blocks.CHERRY_WOOD),
    CRIMSON("wood.valet.crimson", Blocks.CRIMSON_STEM, Blocks.CRIMSON_HYPHAE),
    WARPED("wood.valet.warped", Blocks.WARPED_STEM, Blocks.WARPED_HYPHAE);

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

    public static ValetWoodTarget fromIndex(int index) {
        ValetWoodTarget[] values = values();
        if (index < 0 || index >= values.length) {
            return null;
        }
        return values[index];
    }
}
