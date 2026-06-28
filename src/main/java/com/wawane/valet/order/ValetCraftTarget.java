package com.wawane.valet.order;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public enum ValetCraftTarget {
    STONE_PICKAXE("craft.valet.stone_pickaxe", Items.STONE_PICKAXE);

    private final String translationKey;
    private final Item outputItem;

    ValetCraftTarget(String translationKey, Item outputItem) {
        this.translationKey = translationKey;
        this.outputItem = outputItem;
    }

    public String getTranslationKey() {
        return translationKey;
    }

    public Item getOutputItem() {
        return outputItem;
    }

    public static ValetCraftTarget fromIndex(int index) {
        ValetCraftTarget[] values = values();
        if (index < 0 || index >= values.length) {
            return null;
        }
        return values[index];
    }
}
