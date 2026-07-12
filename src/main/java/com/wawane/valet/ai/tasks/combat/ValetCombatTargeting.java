package com.wawane.valet.ai.tasks.combat;

import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.villager.Villager;

public final class ValetCombatTargeting {
    private ValetCombatTargeting() {
    }

    public static LivingEntity chooseTarget(ServerLevel world, Villager villager, LivingEntity currentTarget, double searchRadius, double chaseRadius) {
        if (isValidTarget(villager, currentTarget, chaseRadius * chaseRadius)) {
            return currentTarget;
        }

        LivingEntity attacker = villager.getLastHurtByMob();
        if (isValidTarget(villager, attacker, chaseRadius * chaseRadius)) {
            return attacker;
        }

        LivingEntity lastAttacker = villager.getLastAttacker();
        if (isValidTarget(villager, lastAttacker, chaseRadius * chaseRadius)) {
            return lastAttacker;
        }

        return findNearestHostile(world, villager, searchRadius);
    }

    private static LivingEntity findNearestHostile(ServerLevel world, Villager villager, double radius) {
        double maxDistanceSquared = radius * radius;
        List<Monster> hostiles = world.getEntitiesOfClass(
                Monster.class,
                villager.getBoundingBox().inflate(radius, 4.0D, radius),
                hostile -> isValidTarget(villager, hostile, maxDistanceSquared)
        );

        Monster nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (Monster hostile : hostiles) {
            double distance = villager.distanceToSqr(hostile);
            if (distance < nearestDistance) {
                nearest = hostile;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    private static boolean isValidTarget(Villager villager, LivingEntity target, double maxDistanceSquared) {
        return target instanceof Monster
                && target.isAlive()
                && !target.isRemoved()
                && !target.isSpectator()
                && villager.distanceToSqr(target) <= maxDistanceSquared
                && villager.hasLineOfSight(target);
    }
}
