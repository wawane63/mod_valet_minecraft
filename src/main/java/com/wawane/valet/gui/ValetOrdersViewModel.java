package com.wawane.valet.gui;

import com.wawane.valet.construction.ValetConstructionBlueprint;
import com.wawane.valet.farm.ValetFarmArea;
import com.wawane.valet.order.ValetMineTarget;
import com.wawane.valet.order.ValetOrder;
import com.wawane.valet.order.ValetWoodTarget;
import com.wawane.valet.progress.ValetCombatPerk;
import com.wawane.valet.progress.ValetPerk;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.world.item.ItemStack;

public record ValetOrdersViewModel(
        int valetEntityId,
        ValetOrder currentOrder,
        int currentMineTargetIndex,
        int currentWoodTargetIndex,
        int currentFarmAreaId,
        int currentFarmCropMask,
        boolean farmReplant,
        boolean farmTillSoil,
        int currentConstructionTargetId,
        int currentCraftTargetIndex,
        int[] oreCounts,
        int[] woodCounts,
        List<ValetFarmArea> farmAreas,
        List<ValetConstructionBlueprint> constructions,
        List<ItemStack> valetInventory,
        int level,
        int xp,
        int nextLevelXp,
        int pendingPerks,
        boolean[] perks,
        boolean[] combatPerks,
        int swordLevel,
        int swordXp,
        int swordNextLevelXp,
        int swordPendingPerks,
        int bowLevel,
        int bowXp,
        int bowNextLevelXp,
        int bowPendingPerks,
        boolean allyAwareness,
        String valetName
) {
    public ValetOrdersViewModel {
        oreCounts = Arrays.copyOf(oreCounts, ValetMineTarget.values().length);
        woodCounts = Arrays.copyOf(woodCounts, ValetWoodTarget.values().length);
        farmAreas = List.copyOf(farmAreas);
        constructions = List.copyOf(constructions);
        valetInventory = copyInventory(valetInventory);
        perks = Arrays.copyOf(perks, ValetPerk.values().length);
        combatPerks = Arrays.copyOf(combatPerks, ValetCombatPerk.values().length);
        valetName = valetName == null ? "" : valetName;
    }

    public static ValetOrdersViewModel fromHandler(ValetOrdersScreenHandler handler) {
        boolean[] perks = new boolean[ValetPerk.values().length];
        for (ValetPerk perk : ValetPerk.values()) {
            perks[perk.ordinal()] = handler.hasPerk(perk);
        }
        boolean[] combatPerks = new boolean[ValetCombatPerk.values().length];
        for (ValetCombatPerk perk : ValetCombatPerk.values()) {
            combatPerks[perk.ordinal()] = handler.hasCombatPerk(perk);
        }
        int[] oreCounts = new int[ValetMineTarget.values().length];
        for (ValetMineTarget target : ValetMineTarget.values()) {
            oreCounts[target.ordinal()] = handler.getOreCount(target);
        }
        int[] woodCounts = new int[ValetWoodTarget.values().length];
        for (ValetWoodTarget target : ValetWoodTarget.values()) {
            woodCounts[target.ordinal()] = handler.getWoodCount(target);
        }

        return new ValetOrdersViewModel(
                handler.getValetEntityId(),
                handler.getCurrentOrder(),
                handler.getCurrentMineTargetIndex(),
                handler.getCurrentWoodTargetIndex(),
                handler.getCurrentFarmAreaId(),
                handler.getCurrentFarmCropMask(),
                handler.shouldReplantFarm(),
                handler.shouldTillFarm(),
                handler.getCurrentConstructionTargetId(),
                handler.getCurrentCraftTargetIndex(),
                oreCounts,
                woodCounts,
                handler.getFarmAreas(),
                handler.getConstructions(),
                handler.getValetInventory(),
                handler.getLevel(),
                handler.getXp(),
                handler.getNextLevelXp(),
                handler.getPendingPerks(),
                perks,
                combatPerks,
                handler.getSwordLevel(),
                handler.getSwordXp(),
                handler.getSwordNextLevelXp(),
                handler.getSwordPendingPerks(),
                handler.getBowLevel(),
                handler.getBowXp(),
                handler.getBowNextLevelXp(),
                handler.getBowPendingPerks(),
                handler.hasAllyAwareness(),
                handler.getValetName()
        );
    }

    public int getOreCount(ValetMineTarget target) {
        int index = target.ordinal();
        return index >= 0 && index < oreCounts.length ? oreCounts[index] : 0;
    }

    public int getWoodCount(ValetWoodTarget target) {
        int index = target.ordinal();
        return index >= 0 && index < woodCounts.length ? woodCounts[index] : 0;
    }

    public boolean hasPerk(ValetPerk perk) {
        int index = perk.ordinal();
        return index >= 0 && index < perks.length && perks[index];
    }

    public boolean hasCombatPerk(ValetCombatPerk perk) {
        int index = perk.ordinal();
        return index >= 0 && index < combatPerks.length && combatPerks[index];
    }

    @Override
    public int[] oreCounts() {
        return Arrays.copyOf(oreCounts, oreCounts.length);
    }

    @Override
    public int[] woodCounts() {
        return Arrays.copyOf(woodCounts, woodCounts.length);
    }

    @Override
    public List<ItemStack> valetInventory() {
        return copyInventory(valetInventory);
    }

    @Override
    public boolean[] perks() {
        return Arrays.copyOf(perks, perks.length);
    }

    @Override
    public boolean[] combatPerks() {
        return Arrays.copyOf(combatPerks, combatPerks.length);
    }

    private static List<ItemStack> copyInventory(List<ItemStack> stacks) {
        List<ItemStack> result = new ArrayList<>(stacks.size());
        for (ItemStack stack : stacks) {
            result.add(stack.copy());
        }
        return List.copyOf(result);
    }
}
