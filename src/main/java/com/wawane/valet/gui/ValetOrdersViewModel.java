package com.wawane.valet.gui;

import com.wawane.valet.construction.ValetConstructionBlueprint;
import com.wawane.valet.order.ValetMineTarget;
import com.wawane.valet.order.ValetOrder;
import com.wawane.valet.order.ValetWoodTarget;
import com.wawane.valet.progress.ValetPerk;

import java.util.Arrays;
import java.util.List;

public record ValetOrdersViewModel(
        int valetEntityId,
        ValetOrder currentOrder,
        int currentMineTargetIndex,
        int currentWoodTargetIndex,
        int currentConstructionTargetId,
        int[] oreCounts,
        int[] woodCounts,
        List<ValetConstructionBlueprint> constructions,
        int level,
        int xp,
        int nextLevelXp,
        int pendingPerks,
        boolean[] perks,
        String valetName
) {
    public ValetOrdersViewModel {
        oreCounts = Arrays.copyOf(oreCounts, ValetMineTarget.values().length);
        woodCounts = Arrays.copyOf(woodCounts, ValetWoodTarget.values().length);
        constructions = List.copyOf(constructions);
        perks = Arrays.copyOf(perks, ValetPerk.values().length);
        valetName = valetName == null ? "" : valetName;
    }

    public static ValetOrdersViewModel fromHandler(ValetOrdersScreenHandler handler) {
        boolean[] perks = new boolean[ValetPerk.values().length];
        for (ValetPerk perk : ValetPerk.values()) {
            perks[perk.ordinal()] = handler.hasPerk(perk);
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
                handler.getCurrentConstructionTargetId(),
                oreCounts,
                woodCounts,
                handler.getConstructions(),
                handler.getLevel(),
                handler.getXp(),
                handler.getNextLevelXp(),
                handler.getPendingPerks(),
                perks,
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

    @Override
    public int[] oreCounts() {
        return Arrays.copyOf(oreCounts, oreCounts.length);
    }

    @Override
    public int[] woodCounts() {
        return Arrays.copyOf(woodCounts, woodCounts.length);
    }

    @Override
    public boolean[] perks() {
        return Arrays.copyOf(perks, perks.length);
    }
}
