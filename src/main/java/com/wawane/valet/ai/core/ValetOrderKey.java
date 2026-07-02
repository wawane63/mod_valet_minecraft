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
            case HARVEST_CROPS -> order.getId()
                    + ":" + ValetOrders.getFarmAreaId(villager)
                    + ":" + ValetOrders.getFarmCropMask(villager)
                    + ":" + ValetOrders.shouldReplantFarm(villager)
                    + ":" + ValetOrders.shouldTillFarm(villager);
            case BREED_ANIMALS -> order.getId()
                    + ":" + ValetOrders.getAnimalAreaId(villager)
                    + ":" + ValetOrders.shouldFeedAnimals(villager)
                    + ":" + ValetOrders.shouldBreedAnimals(villager)
                    + ":" + ValetOrders.shouldShearAnimals(villager)
                    + ":" + ValetOrders.shouldCollectAnimalEggs(villager)
                    + ":" + ValetOrders.shouldMilkAnimals(villager)
                    + ":" + ValetOrders.getMaxAnimals(villager);
            case BUILD_STRUCTURE -> order.getId() + ":" + ValetOrders.getConstructionTargetId(villager);
            case CRAFT -> order.getId() + ":" + ValetOrders.getCraftTarget(villager);
            case NONE -> order.getId();
        };
    }
}
