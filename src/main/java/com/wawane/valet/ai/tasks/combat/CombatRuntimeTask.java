package com.wawane.valet.ai.tasks.combat;

import com.wawane.valet.ValetDebug;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.EntityAnimationS2CPacket;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;

public final class CombatRuntimeTask {
    private final Control control;
    private LivingEntity target;
    private int attackCooldownTicks;
    private int pathRefreshTicks;

    public CombatRuntimeTask(Control control) {
        this.control = control;
    }

    public boolean tick(ServerWorld world) {
        if (attackCooldownTicks > 0) {
            attackCooldownTicks--;
        }
        if (pathRefreshTicks > 0) {
            pathRefreshTicks--;
        }

        if (!control.isDefenseEnabled()) {
            clearTarget();
            return false;
        }

        ensureWoodenSword();
        LivingEntity nextTarget = ValetCombatTargeting.chooseTarget(
                world,
                control.villager(),
                target,
                control.combatSearchRadius(),
                control.combatChaseRadius()
        );
        if (nextTarget == null) {
            if (target != null) {
                clearTarget();
                control.onCombatFinished();
            }
            return false;
        }

        if (target == null || target.getId() != nextTarget.getId()) {
            target = nextTarget;
            pathRefreshTicks = 0;
            control.onCombatStarted(target);
        }

        fight(world, target);
        return true;
    }

    public String debugSummary() {
        return control.isDefenseEnabled()
                ? "combat=" + (target == null ? "ready" : ValetDebug.shortPos(target.getBlockPos()))
                : "combat=inactive";
    }

    private void fight(ServerWorld world, LivingEntity target) {
        VillagerEntity villager = control.villager();
        villager.getLookControl().lookAt(target, 30.0F, 30.0F);

        if (villager.squaredDistanceTo(target) <= control.combatAttackRangeSquared()) {
            villager.getNavigation().stop();
            if (attackCooldownTicks <= 0 && villager.canSee(target)) {
                villager.swingHand(Hand.MAIN_HAND, true);
                world.getChunkManager().sendToNearbyPlayers(
                        villager,
                        new EntityAnimationS2CPacket(villager, EntityAnimationS2CPacket.SWING_MAIN_HAND)
                );
                world.playSound(null, villager.getBlockPos(), SoundEvents.ENTITY_PLAYER_ATTACK_STRONG, SoundCategory.NEUTRAL, 1.0F, 1.0F);
                target.damage(world.getDamageSources().mobAttack(villager), control.combatAttackDamage());
                attackCooldownTicks = control.combatAttackCooldownTicks();
            }
            return;
        }

        if (pathRefreshTicks <= 0 || villager.getNavigation().isIdle()) {
            villager.getNavigation().startMovingTo(target, control.combatMoveSpeed());
            pathRefreshTicks = 8;
        }
    }

    private void clearTarget() {
        target = null;
        pathRefreshTicks = 0;
    }

    private void ensureWoodenSword() {
        VillagerEntity villager = control.villager();
        if (villager.getEquippedStack(EquipmentSlot.MAINHAND).isOf(Items.WOODEN_SWORD)) {
            return;
        }

        villager.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.WOODEN_SWORD));
        villager.setEquipmentDropChance(EquipmentSlot.MAINHAND, 0.0F);
    }

    public interface Control {
        VillagerEntity villager();

        boolean isDefenseEnabled();

        double combatSearchRadius();

        double combatChaseRadius();

        double combatAttackRangeSquared();

        double combatMoveSpeed();

        float combatAttackDamage();

        int combatAttackCooldownTicks();

        void onCombatStarted(LivingEntity target);

        void onCombatFinished();
    }
}
