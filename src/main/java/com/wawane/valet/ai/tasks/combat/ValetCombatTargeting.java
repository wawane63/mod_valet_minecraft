package com.wawane.valet.ai.tasks.combat;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.Comparator;
import java.util.List;

public final class ValetCombatTargeting {
    private ValetCombatTargeting() {
    }

    public static LivingEntity chooseTarget(ServerWorld world, VillagerEntity villager, LivingEntity currentTarget, double searchRadius, double chaseRadius) {
        if (isValidTarget(villager, currentTarget, chaseRadius * chaseRadius)) {
            return currentTarget;
        }

        LivingEntity attacker = villager.getAttacker();
        if (isValidTarget(villager, attacker, chaseRadius * chaseRadius)) {
            return attacker;
        }

        LivingEntity lastAttacker = villager.getLastAttacker();
        if (isValidTarget(villager, lastAttacker, chaseRadius * chaseRadius)) {
            return lastAttacker;
        }

        return findNearestHostile(world, villager, searchRadius);
    }

    private static LivingEntity findNearestHostile(ServerWorld world, VillagerEntity villager, double radius) {
        double maxDistanceSquared = radius * radius;
        List<HostileEntity> hostiles = world.getEntitiesByClass(
                HostileEntity.class,
                villager.getBoundingBox().expand(radius, 4.0D, radius),
                hostile -> isValidTarget(villager, hostile, maxDistanceSquared)
        );

        return hostiles.stream()
                .min(Comparator.comparingDouble(villager::squaredDistanceTo))
                .orElse(null);
    }

    private static boolean isValidTarget(VillagerEntity villager, LivingEntity target, double maxDistanceSquared) {
        return target instanceof HostileEntity
                && target.isAlive()
                && !target.isRemoved()
                && !target.isSpectator()
                && villager.squaredDistanceTo(target) <= maxDistanceSquared
                && villager.canSee(target);
    }
}
