package com.wawane.valet.quest;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public enum ValetQuest {
    FOOD("food", Items.BREAD, 16, 3),
    TORCHES("torches", Items.TORCH, 32, 4),
    IRON("iron", Items.IRON_INGOT, 12, 6);

    private final String id;
    private final Item requestedItem;
    private final int requestedCount;
    private final int emeraldReward;

    ValetQuest(String id, Item requestedItem, int requestedCount, int emeraldReward) {
        this.id = id;
        this.requestedItem = requestedItem;
        this.requestedCount = requestedCount;
        this.emeraldReward = emeraldReward;
    }

    public String id() { return id; }
    public Item requestedItem() { return requestedItem; }
    public int requestedCount() { return requestedCount; }
    public int emeraldReward() { return emeraldReward; }

    public static ValetQuest fromIndex(int index) {
        ValetQuest[] values = values();
        return index >= 0 && index < values.length ? values[index] : null;
    }
}
