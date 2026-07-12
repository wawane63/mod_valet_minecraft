package com.wawane.valet.order;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public enum ValetFarmCrop {
    WHEAT("crop.valet.wheat"),
    CARROT("crop.valet.carrot"),
    POTATO("crop.valet.potato"),
    BEETROOT("crop.valet.beetroot"),
    NETHER_WART("crop.valet.nether_wart");

    private static final int ALL_CROPS_MASK = (1 << values().length) - 1;
    private final String translationKey;

    ValetFarmCrop(String translationKey) {
        this.translationKey = translationKey;
    }

    public String getTranslationKey() {
        return translationKey;
    }

    public int mask() {
        return 1 << ordinal();
    }

    public boolean isEnabled(int cropMask) {
        return (cropMask & mask()) != 0;
    }

    public boolean matches(BlockState state) {
        return switch (this) {
            case WHEAT -> state.is(Blocks.WHEAT);
            case CARROT -> state.is(Blocks.CARROTS);
            case POTATO -> state.is(Blocks.POTATOES);
            case BEETROOT -> state.is(Blocks.BEETROOTS);
            case NETHER_WART -> state.is(Blocks.NETHER_WART);
        };
    }

    public static int defaultMask() {
        return ALL_CROPS_MASK;
    }

    public static boolean matchesAnyEnabled(BlockState state, int cropMask) {
        for (ValetFarmCrop crop : values()) {
            if (crop.isEnabled(cropMask) && crop.matches(state)) {
                return true;
            }
        }
        return false;
    }
}
