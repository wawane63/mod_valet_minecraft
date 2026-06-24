package com.wawane.valet.ai.core;

import com.wawane.valet.progress.ValetPerk;
import com.wawane.valet.progress.ValetCombatPerk;
import com.wawane.valet.progress.ValetCombatProgress;
import com.wawane.valet.progress.ValetProgress;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.inventory.Inventory;

public final class ValetWorkSettings {
    private static final int CHEST_RADIUS = 10;
    private static final int MINE_RADIUS = 18;
    private static final int MINE_VERTICAL_RADIUS = 12;
    private static final int MAX_PATH_NODES = 18000;
    private static final int MAX_PATH_LENGTH = 96;
    private static final int NO_TARGET_DELAY_TICKS = 80;
    private static final int MAX_VEIN_BLOCKS = 96;
    private static final int BASE_INVENTORY_SLOTS = 4;
    private static final int STORAGE_PERK_BONUS_SLOTS = 4;
    private static final int CHEST_RADIUS_BONUS = 8;
    private static final int MAX_PATH_NODES_BONUS = 9000;
    private static final int MAX_PATH_LENGTH_BONUS = 48;
    private static final int MAX_VEIN_BLOCKS_BONUS = 64;
    private static final int MONSTER_SPAWN_BLOCK_LIGHT = 0;
    private static final int COMFORT_TORCH_BLOCK_LIGHT = 7;
    private static final int BUILD_MATERIAL_RADIUS_BONUS = 16;
    private static final double COMBAT_SEARCH_RADIUS = 8.0D;
    private static final double COMBAT_SEARCH_RADIUS_BONUS = 4.0D;
    private static final double COMBAT_CHASE_RADIUS_BONUS = 6.0D;
    private static final double COMBAT_ATTACK_RANGE_SQUARED = 2.25D;
    private static final double COMBAT_MOVE_SPEED = 1.0D;
    private static final double COMBAT_MOVE_SPEED_BONUS = 0.15D;
    private static final float COMBAT_ATTACK_DAMAGE = 4.0F;
    private static final int COMBAT_ATTACK_COOLDOWN_TICKS = 22;
    private static final int COMBAT_ATTACK_COOLDOWN_BONUS = 4;
    private static final float COMBAT_ARROW_DAMAGE = 3.0F;
    private static final int COMBAT_ARROW_COOLDOWN_TICKS = 30;
    private static final int COMBAT_ARROW_COOLDOWN_BONUS = 5;
    private static final int COMBAT_ARROW_RESTOCK_COUNT = 16;

    private final VillagerEntity villager;

    public ValetWorkSettings(VillagerEntity villager) {
        this.villager = villager;
    }

    public int mineRadius() {
        return ValetProgress.hasPerk(villager, ValetPerk.VISION) ? MINE_RADIUS + 8 : MINE_RADIUS;
    }

    public int mineVerticalRadius() {
        return ValetProgress.hasPerk(villager, ValetPerk.VISION) ? MINE_VERTICAL_RADIUS + 4 : MINE_VERTICAL_RADIUS;
    }

    public int actionDelayTicks() {
        return ValetProgress.hasPerk(villager, ValetPerk.SPEED) ? 0 : 1;
    }

    public int chestRadius() {
        return ValetProgress.hasPerk(villager, ValetPerk.HAUL) ? CHEST_RADIUS + CHEST_RADIUS_BONUS : CHEST_RADIUS;
    }

    public int materialRadius() {
        return chestRadius() + BUILD_MATERIAL_RADIUS_BONUS;
    }

    public int maxPathNodes() {
        return ValetProgress.hasPerk(villager, ValetPerk.PATHING) ? MAX_PATH_NODES + MAX_PATH_NODES_BONUS : MAX_PATH_NODES;
    }

    public int maxPathLength() {
        return ValetProgress.hasPerk(villager, ValetPerk.PATHING) ? MAX_PATH_LENGTH + MAX_PATH_LENGTH_BONUS : MAX_PATH_LENGTH;
    }

    public int maxVeinBlocks() {
        return ValetProgress.hasPerk(villager, ValetPerk.VEIN) ? MAX_VEIN_BLOCKS + MAX_VEIN_BLOCKS_BONUS : MAX_VEIN_BLOCKS;
    }

    public int torchLightThreshold() {
        return ValetProgress.hasPerk(villager, ValetPerk.LIGHTING) ? COMFORT_TORCH_BLOCK_LIGHT : MONSTER_SPAWN_BLOCK_LIGHT;
    }

    public int usableInventorySlots(Inventory inventory) {
        int slots = BASE_INVENTORY_SLOTS;
        if (ValetProgress.hasPerk(villager, ValetPerk.STORAGE)) {
            slots += STORAGE_PERK_BONUS_SLOTS;
        }
        return Math.min(inventory.size(), slots);
    }

    public int noTargetDelayTicks() {
        return NO_TARGET_DELAY_TICKS;
    }

    public double combatSearchRadius() {
        return ValetProgress.hasPerk(villager, ValetPerk.VISION) ? COMBAT_SEARCH_RADIUS + COMBAT_SEARCH_RADIUS_BONUS : COMBAT_SEARCH_RADIUS;
    }

    public double combatChaseRadius() {
        return combatSearchRadius() + COMBAT_CHASE_RADIUS_BONUS;
    }

    public double combatAttackRangeSquared() {
        return COMBAT_ATTACK_RANGE_SQUARED;
    }

    public double combatRangedAttackRangeSquared() {
        double range = combatSearchRadius();
        return range * range;
    }

    public double combatMoveSpeed() {
        return ValetProgress.hasPerk(villager, ValetPerk.SPEED) ? COMBAT_MOVE_SPEED + COMBAT_MOVE_SPEED_BONUS : COMBAT_MOVE_SPEED;
    }

    public float combatAttackDamage() {
        return ValetCombatProgress.hasPerk(villager, ValetCombatPerk.SWORD_STRENGTH)
                ? COMBAT_ATTACK_DAMAGE + 1.0F
                : COMBAT_ATTACK_DAMAGE;
    }

    public int combatAttackCooldownTicks() {
        int cooldown = ValetProgress.hasPerk(villager, ValetPerk.SPEED)
                ? Math.max(1, COMBAT_ATTACK_COOLDOWN_TICKS - COMBAT_ATTACK_COOLDOWN_BONUS)
                : COMBAT_ATTACK_COOLDOWN_TICKS;
        return ValetCombatProgress.hasPerk(villager, ValetCombatPerk.SWORD_RECOVERY)
                ? Math.max(1, cooldown - 4)
                : cooldown;
    }

    public float combatArrowDamage() {
        return ValetCombatProgress.hasPerk(villager, ValetCombatPerk.BOW_STRENGTH)
                ? COMBAT_ARROW_DAMAGE + 1.0F
                : COMBAT_ARROW_DAMAGE;
    }

    public int combatArrowCooldownTicks() {
        return ValetProgress.hasPerk(villager, ValetPerk.SPEED)
                ? Math.max(1, COMBAT_ARROW_COOLDOWN_TICKS - COMBAT_ARROW_COOLDOWN_BONUS)
                : COMBAT_ARROW_COOLDOWN_TICKS;
    }

    public int combatArrowRestockCount() {
        return COMBAT_ARROW_RESTOCK_COUNT;
    }
}
