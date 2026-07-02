package com.wawane.valet.group;

import com.wawane.valet.ValetMod;
import com.wawane.valet.ValetRole;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.phys.AABB;

public final class ValetGroupRuntime {
    private static final double FOLLOW_DISTANCE = 4.0D;
    private static final double GUARD_CLOSE_DISTANCE = 6.0D;
    private static final double GUARD_WIDE_DISTANCE = 10.0D;

    private ValetGroupRuntime() {
    }

    public static ValetGroupCommand getCommand(ServerLevel world, Villager villager) {
        return ValetGroupStorage.get(world).getCommandForValet(villager.getUUID());
    }

    public static boolean hasActiveCommand(ServerLevel world, Villager villager) {
        return getCommand(world, villager).mode() != ValetGroupMode.IDLE;
    }

    public static boolean hasControllingCommand(ServerLevel world, Villager villager) {
        ValetGroupMode mode = getCommand(world, villager).mode();
        if (mode == ValetGroupMode.IDLE) {
            return false;
        }
        if (mode == ValetGroupMode.FOLLOW || mode == ValetGroupMode.RECALL) {
            return true;
        }
        return canUseCombatGroupCommand(world, villager);
    }

    public static boolean isCombatCommand(ServerLevel world, Villager villager) {
        return getCommand(world, villager).mode().isCombatMode();
    }

    public static double combatSearchRadius(ServerLevel world, Villager villager, double fallback) {
        ValetGroupCommand command = getCommand(world, villager);
        if (command.mode().isCombatMode() && command.radius() > 0) {
            return Math.max(fallback, command.radius());
        }
        return fallback;
    }

    public static LivingEntity chooseCommandedTarget(ServerLevel world, Villager villager, LivingEntity currentTarget, double chaseRadius) {
        ValetGroupCommand command = getCommand(world, villager);
        if (command.mode().isCombatMode() && !canUseCombatGroupCommand(world, villager)) {
            return null;
        }
        double chaseDistanceSquared = chaseRadius * chaseRadius;
        if (isValidTarget(villager, currentTarget, chaseDistanceSquared)) {
            if (command.mode() == ValetGroupMode.ATTACK_AREA && command.pos() != null && isInsideArea(currentTarget.blockPosition(), command.pos(), command.radius())) {
                return currentTarget;
            }
            if ((command.mode() == ValetGroupMode.GUARD_CLOSE || command.mode() == ValetGroupMode.GUARD_WIDE)
                    && command.playerUuid() != null
                    && isNearPlayer(world, currentTarget, command.playerUuid(), command.radius())) {
                return currentTarget;
            }
            if (command.mode() == ValetGroupMode.ATTACK_TARGET && command.targetUuid() != null && command.targetUuid().equals(currentTarget.getUUID())) {
                return currentTarget;
            }
        }

        return switch (command.mode()) {
            case GUARD_CLOSE, GUARD_WIDE -> findNearestHostileNearPlayer(world, villager, command.playerUuid(), command.radius());
            case ATTACK_TARGET -> findTargetEntity(world, villager, command);
            case ATTACK_AREA -> findNearestHostileNearPos(world, villager, command.pos(), command.radius());
            case IDLE, FOLLOW, RECALL -> null;
        };
    }

    public static boolean tickMovement(ServerLevel world, Villager villager, double speed) {
        ValetGroupCommand command = getCommand(world, villager);
        if (command.mode().isCombatMode() && !canUseCombatGroupCommand(world, villager)) {
            return false;
        }
        if (command.mode() == ValetGroupMode.FOLLOW || command.mode() == ValetGroupMode.GUARD_CLOSE || command.mode() == ValetGroupMode.GUARD_WIDE) {
            ServerPlayer player = getPlayer(world, command.playerUuid());
            if (player == null) {
                return false;
            }
            double distance = command.mode() == ValetGroupMode.GUARD_WIDE ? GUARD_WIDE_DISTANCE : command.mode() == ValetGroupMode.GUARD_CLOSE ? GUARD_CLOSE_DISTANCE : FOLLOW_DISTANCE;
            if (villager.distanceToSqr(player) <= distance * distance) {
                return false;
            }
            villager.getLookControl().setLookAt(player, 20.0F, 20.0F);
            return villager.getNavigation().moveTo(player, speed);
        }

        if (command.mode() == ValetGroupMode.ATTACK_AREA && command.pos() != null) {
            double radius = Math.max(4.0D, command.radius() * 0.75D);
            if (villager.distanceToSqr(command.pos().getX() + 0.5D, command.pos().getY() + 0.5D, command.pos().getZ() + 0.5D) <= radius * radius) {
                return false;
            }
            return villager.getNavigation().moveTo(command.pos().getX() + 0.5D, command.pos().getY(), command.pos().getZ() + 0.5D, speed);
        }

        if (command.mode() == ValetGroupMode.ATTACK_TARGET) {
            BlockPos targetPos = command.pos();
            if (command.targetUuid() != null) {
                Entity entity = world.getEntity(command.targetUuid());
                if (entity instanceof LivingEntity living && living.isAlive() && !living.isRemoved()) {
                    targetPos = living.blockPosition();
                }
            }
            if (targetPos == null || villager.distanceToSqr(targetPos.getX() + 0.5D, targetPos.getY() + 0.5D, targetPos.getZ() + 0.5D) <= 36.0D) {
                return false;
            }
            return villager.getNavigation().moveTo(targetPos.getX() + 0.5D, targetPos.getY(), targetPos.getZ() + 0.5D, speed);
        }
        return false;
    }

    public static ServerPlayer getPlayer(ServerLevel world, java.util.UUID uuid) {
        if (uuid == null) {
            return null;
        }
        return world.getServer().getPlayerList().getPlayer(uuid);
    }

    private static LivingEntity findTargetEntity(ServerLevel world, Villager villager, ValetGroupCommand command) {
        if (command.targetUuid() == null) {
            return null;
        }
        Entity entity = world.getEntity(command.targetUuid());
        if (entity instanceof LivingEntity living && isValidTarget(villager, living, command.radius() * command.radius())) {
            return living;
        }
        if (command.pos() != null) {
            return findNearestHostileNearPos(world, villager, command.pos(), command.radius());
        }
        return null;
    }

    private static LivingEntity findNearestHostileNearPlayer(ServerLevel world, Villager villager, java.util.UUID playerUuid, int radius) {
        ServerPlayer player = getPlayer(world, playerUuid);
        return player == null ? null : findNearestHostileNearPos(world, villager, player.blockPosition(), radius);
    }

    private static LivingEntity findNearestHostileNearPos(ServerLevel world, Villager villager, BlockPos center, int radius) {
        if (center == null || radius <= 0) {
            return null;
        }
        AABB area = new AABB(
                center.getX() - radius,
                center.getY() - 6,
                center.getZ() - radius,
                center.getX() + radius + 1,
                center.getY() + 7,
                center.getZ() + radius + 1
        );
        List<Monster> hostiles = world.getEntitiesOfClass(Monster.class, area, hostile -> isValidTarget(villager, hostile, Double.MAX_VALUE));
        return hostiles.stream()
                .min(Comparator.comparingDouble(villager::distanceToSqr))
                .orElse(null);
    }

    private static boolean isNearPlayer(ServerLevel world, LivingEntity target, java.util.UUID playerUuid, int radius) {
        ServerPlayer player = getPlayer(world, playerUuid);
        return player != null && target.distanceToSqr(player) <= radius * radius;
    }

    private static boolean isInsideArea(BlockPos pos, BlockPos center, int radius) {
        int dx = pos.getX() - center.getX();
        int dz = pos.getZ() - center.getZ();
        return Math.abs(pos.getY() - center.getY()) <= 8 && dx * dx + dz * dz <= radius * radius;
    }

    private static boolean isValidTarget(Villager villager, LivingEntity target, double maxDistanceSquared) {
        return target instanceof Monster
                && target.isAlive()
                && !target.isRemoved()
                && !target.isSpectator()
                && ValetMod.isValet(villager)
                && villager.distanceToSqr(target) <= maxDistanceSquared
                && villager.hasLineOfSight(target);
    }

    private static boolean canUseCombatGroupCommand(ServerLevel world, Villager villager) {
        ValetRole role = ValetRole.get(world, villager);
        return role == ValetRole.COMBATANT || role == ValetRole.MAGICIAN;
    }
}
