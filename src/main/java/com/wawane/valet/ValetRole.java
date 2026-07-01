package com.wawane.valet;

import com.wawane.valet.order.ValetOrder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.block.state.BlockState;

public enum ValetRole {
    ARTISAN("role.valet.artisan"),
    COMBATANT("role.valet.combatant"),
    FARMER("role.valet.farmer"),
    MAGICIAN("role.valet.magician");

    private final String translationKey;

    ValetRole(String translationKey) {
        this.translationKey = translationKey;
    }

    public String getTranslationKey() {
        return translationKey;
    }

    public boolean allows(ValetOrder order) {
        return switch (this) {
            case ARTISAN -> order == ValetOrder.NONE
                    || order == ValetOrder.MINE_ORES
                    || order == ValetOrder.CHOP_WOOD
                    || order == ValetOrder.BUILD_STRUCTURE
                    || order == ValetOrder.CRAFT;
            case COMBATANT -> order == ValetOrder.NONE;
            case FARMER -> order == ValetOrder.NONE || order == ValetOrder.HARVEST_CROPS;
            case MAGICIAN -> order == ValetOrder.NONE;
        };
    }

    public static ValetRole fromIndex(int index) {
        ValetRole[] values = values();
        if (index < 0 || index >= values.length) {
            return ARTISAN;
        }
        return values[index];
    }

    public static ValetRole fromWorkstation(BlockState state) {
        if (state.is(ValetMod.COMBAT_WORKSTATION)) {
            return COMBATANT;
        }
        if (state.is(ValetMod.FARMER_WORKSTATION)) {
            return FARMER;
        }
        if (state.is(ValetMod.MAGIC_WORKSTATION)) {
            return MAGICIAN;
        }
        return ARTISAN;
    }

    public static ValetRole get(ServerLevel world, Villager villager) {
        BlockPos home = ValetHome.get(world, villager);
        if (home == null) {
            return ARTISAN;
        }
        return fromWorkstation(world.getBlockState(home));
    }
}
