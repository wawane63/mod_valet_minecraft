package com.wawane.valet.order;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.TagKey;

public enum ValetMineTarget {
    COAL("ore.valet.coal", BlockTags.COAL_ORES),
    COPPER("ore.valet.copper", BlockTags.COPPER_ORES),
    IRON("ore.valet.iron", BlockTags.IRON_ORES),
    GOLD("ore.valet.gold", BlockTags.GOLD_ORES),
    REDSTONE("ore.valet.redstone", BlockTags.REDSTONE_ORES),
    LAPIS("ore.valet.lapis", BlockTags.LAPIS_ORES),
    EMERALD("ore.valet.emerald", BlockTags.EMERALD_ORES),
    DIAMOND("ore.valet.diamond", BlockTags.DIAMOND_ORES);

    private final String translationKey;
    private final TagKey<Block> tag;

    ValetMineTarget(String translationKey, TagKey<Block> tag) {
        this.translationKey = translationKey;
        this.tag = tag;
    }

    public String getTranslationKey() {
        return translationKey;
    }

    public boolean matches(BlockState state) {
        return state.isIn(tag);
    }

    public static ValetMineTarget fromIndex(int index) {
        ValetMineTarget[] values = values();
        if (index < 0 || index >= values.length) {
            return null;
        }
        return values[index];
    }
}
