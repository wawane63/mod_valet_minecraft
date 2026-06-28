package com.wawane.valet.order;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public enum ValetMineTarget {
    COAL("ore.valet.coal", oreTag("coal_ores")),
    COPPER("ore.valet.copper", oreTag("copper_ores")),
    IRON("ore.valet.iron", oreTag("iron_ores")),
    GOLD("ore.valet.gold", oreTag("gold_ores")),
    REDSTONE("ore.valet.redstone", oreTag("redstone_ores")),
    LAPIS("ore.valet.lapis", oreTag("lapis_ores")),
    EMERALD("ore.valet.emerald", oreTag("emerald_ores")),
    DIAMOND("ore.valet.diamond", oreTag("diamond_ores"));

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
        return state.is(tag);
    }

    public TagKey<Block> tag() {
        return tag;
    }

    public static ValetMineTarget fromIndex(int index) {
        ValetMineTarget[] values = values();
        if (index < 0 || index >= values.length) {
            return null;
        }
        return values[index];
    }

    private static TagKey<Block> oreTag(String path) {
        return TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("minecraft", path));
    }
}
