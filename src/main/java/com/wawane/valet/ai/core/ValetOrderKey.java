package com.wawane.valet.ai.core;

import com.wawane.valet.order.ValetOrder;
import com.wawane.valet.order.ValetOrders;
import net.minecraft.world.entity.npc.villager.Villager;

public final class ValetOrderKey {
    private ValetOrderKey() {
    }

    public static String of(Villager villager) {
        ValetOrder order = ValetOrders.get(villager);
        return switch (order) {
            case MINE_ORES -> order.getId() + ":" + ValetOrders.getMineTarget(villager);
            case CHOP_WOOD -> order.getId() + ":" + ValetOrders.getWoodTarget(villager);
            case BUILD_STRUCTURE -> order.getId() + ":" + ValetOrders.getConstructionTargetId(villager);
            case CRAFT -> order.getId() + ":" + ValetOrders.getCraftTarget(villager);
            case NONE -> order.getId();
        };
    }
}
