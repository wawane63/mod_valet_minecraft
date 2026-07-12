package com.wawane.valet.group;

import com.wawane.valet.ValetMod;
import com.wawane.valet.ValetRole;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.levelgen.Heightmap;

public final class ValetGroupRuntime {
    private static final double FOLLOW_DISTANCE = 4.0D;
    private static final double GUARD_CLOSE_DISTANCE = 6.0D;
    private static final double GUARD_WIDE_DISTANCE = 10.0D;
    private static final double MOVE_STEP_DISTANCE = 24.0D;
    private static final int MOVE_REFRESH_TICKS = 20;
    private static final Map<UUID, Long> NEXT_MOVE_REFRESH = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> MOVE_FAILURES = new ConcurrentHashMap<>();

    private ValetGroupRuntime() {
    }

    public static ValetGroupCommand getCommand(ServerLevel world, Villager villager) {
        return ValetGroupStorage.get(world).getCommandForValet(villager.getUUID());
    }

    public static boolean hasControllingCommand(ServerLevel world, Villager villager) {
        ValetGroupCommand command = getCommand(world, villager);
        return switch (command.mode()) {
            case IDLE -> false;
            case FOLLOW -> getPlayer(world, command.playerUuid()) != null;
            case GUARD_CLOSE, GUARD_WIDE -> canUseCombatGroupCommand(world, villager)
                    && getPlayer(world, command.playerUuid()) != null;
            case ATTACK_TARGET -> canUseCombatGroupCommand(world, villager)
                    && (command.targetUuid() != null || command.pos() != null);
            case ATTACK_AREA -> canUseCombatGroupCommand(world, villager) && command.pos() != null;
            case MOVE_TO -> command.pos() != null;
            case RECALL -> true;
        };
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
            case IDLE, FOLLOW, MOVE_TO, RECALL -> null;
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
        if (command.mode() == ValetGroupMode.MOVE_TO && command.pos() != null) {
            return tickLongDistanceMovement(world, villager, command.pos(), speed);
        }
        return false;
    }

    private static boolean tickLongDistanceMovement(ServerLevel world, Villager villager, BlockPos destination, double speed) {
        double offsetX = formationOffset(villager.getUUID(), true);
        double offsetZ = formationOffset(villager.getUUID(), false);
        double targetX = destination.getX() + 0.5D + offsetX;
        double targetZ = destination.getZ() + 0.5D + offsetZ;
        double dx = targetX - villager.getX();
        double dz = targetZ - villager.getZ();
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
        if (horizontalDistance <= 3.0D) {
            villager.getNavigation().stop();
            NEXT_MOVE_REFRESH.remove(villager.getUUID());
            MOVE_FAILURES.remove(villager.getUUID());
            return true;
        }

        long gameTime = world.getGameTime();
        long nextRefresh = NEXT_MOVE_REFRESH.getOrDefault(villager.getUUID(), 0L);
        if (!villager.getNavigation().isDone() && gameTime < nextRefresh) {
            return true;
        }

        int failures = MOVE_FAILURES.getOrDefault(villager.getUUID(), 0);
        double step = Math.min(MOVE_STEP_DISTANCE, horizontalDistance);
        double angle = Math.atan2(dz, dx) + detourAngle(failures);
        int stepX = (int) Math.floor(villager.getX() + Math.cos(angle) * step);
        int stepZ = (int) Math.floor(villager.getZ() + Math.sin(angle) * step);
        if (!world.hasChunk(stepX >> 4, stepZ >> 4)) {
            NEXT_MOVE_REFRESH.put(villager.getUUID(), gameTime + 5L);
            return true;
        }
        int stepY = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, stepX, stepZ);
        boolean moving = villager.getNavigation().moveTo(stepX + 0.5D, stepY, stepZ + 0.5D, speed);
        NEXT_MOVE_REFRESH.put(villager.getUUID(), gameTime + MOVE_REFRESH_TICKS);
        if (moving) {
            MOVE_FAILURES.put(villager.getUUID(), Math.max(0, failures - 1));
        } else {
            MOVE_FAILURES.put(villager.getUUID(), Math.min(6, failures + 1));
        }
        return true;
    }

    private static double formationOffset(UUID uuid, boolean xAxis) {
        int hash = xAxis ? uuid.hashCode() : Integer.rotateLeft(uuid.hashCode(), 13);
        return Math.floorMod(hash, 7) - 3;
    }

    private static double detourAngle(int failures) {
        return switch (failures) {
            case 1 -> Math.toRadians(30.0D);
            case 2 -> Math.toRadians(-30.0D);
            case 3 -> Math.toRadians(60.0D);
            case 4 -> Math.toRadians(-60.0D);
            case 5 -> Math.toRadians(90.0D);
            case 6 -> Math.toRadians(-90.0D);
            default -> 0.0D;
        };
    }

    public static void clear(UUID valetUuid) {
        NEXT_MOVE_REFRESH.remove(valetUuid);
        MOVE_FAILURES.remove(valetUuid);
    }

    public static void clearAll() {
        NEXT_MOVE_REFRESH.clear();
        MOVE_FAILURES.clear();
    }

    public static ServerPlayer getPlayer(ServerLevel world, java.util.UUID uuid) {
        if (uuid == null) {
            return null;
        }
        ServerPlayer player = world.getServer().getPlayerList().getPlayer(uuid);
        return player != null && player.level() == world && player.isAlive() && !player.isSpectator() ? player : null;
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
                center.getX() - (double) radius,
                center.getY() - 6.0D,
                center.getZ() - (double) radius,
                center.getX() + (double) radius + 1.0D,
                center.getY() + 7.0D,
                center.getZ() + (double) radius + 1.0D
        );
        List<Monster> hostiles = world.getEntitiesOfClass(Monster.class, area, hostile -> isValidTarget(villager, hostile, Double.MAX_VALUE));
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

    private static boolean isNearPlayer(ServerLevel world, LivingEntity target, java.util.UUID playerUuid, int radius) {
        ServerPlayer player = getPlayer(world, playerUuid);
        return player != null && target.distanceToSqr(player) <= radius * radius;
    }

    private static boolean isInsideArea(BlockPos pos, BlockPos center, int radius) {
        double dx = (double) pos.getX() - center.getX();
        long dy = (long) pos.getY() - center.getY();
        double dz = (double) pos.getZ() - center.getZ();
        double radiusSquared = (double) radius * radius;
        return Math.abs(dy) <= 8L && dx * dx + dz * dz <= radiusSquared;
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
