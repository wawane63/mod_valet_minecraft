package com.wawane.valet.group;

import com.wawane.valet.ValetMod;
import com.wawane.valet.ValetRole;
import com.wawane.valet.ValetDebug;
import com.wawane.valet.ai.path.ValetSafeNavigation;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.pathfinder.Path;

public final class ValetGroupRuntime {
    private static final double FOLLOW_DISTANCE = 4.0D;
    private static final double GUARD_CLOSE_DISTANCE = 6.0D;
    private static final double GUARD_WIDE_DISTANCE = 10.0D;
    private static final double MOVE_STEP_DISTANCE = 24.0D;
    private static final int MOVE_REFRESH_TICKS = 20;
    private static final int FOLLOW_REFRESH_TICKS = 10;
    private static final int SWIM_PROBE_DISTANCE = 32;
    private static final int SWIM_MAX_NODES = 96;
    private static final int SWIM_NO_PROGRESS_TICKS = 40;
    private static final double SWIM_PROGRESS_EPSILON = 0.16D;
    private static final double SWIM_TARGET_REACHED_DISTANCE_SQUARED = 2.25D;
    private static final double SWIM_FOLLOW_STOP_DISTANCE_SQUARED = 6.25D;
    private static final Map<UUID, Long> NEXT_MOVE_REFRESH = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> NEXT_FOLLOW_REFRESH = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> MOVE_FAILURES = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> NEXT_DEFENSE_HIT = new ConcurrentHashMap<>();
    private static final Map<UUID, BlockPos> ACTIVE_SWIM_TARGETS = new ConcurrentHashMap<>();
    private static final Map<UUID, SwimProgress> SWIM_PROGRESS = new ConcurrentHashMap<>();
    private static final Map<Integer, UUID> TRAVEL_LEADERS = new ConcurrentHashMap<>();

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

    public static boolean isTravelCommand(ServerLevel world, Villager villager) {
        return getCommand(world, villager).mode() == ValetGroupMode.MOVE_TO;
    }

    public static boolean tickSelfDefense(ServerLevel world, Villager villager, double speed) {
        LivingEntity target = findNearestHostileNearPos(world, villager, villager.blockPosition(), 5);
        if (target == null) {
            return false;
        }
        villager.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new ItemStack(Items.WOODEN_SWORD));
        villager.getLookControl().setLookAt(target, 30.0F, 30.0F);
        if (villager.distanceToSqr(target) > 6.25D) {
            villager.getNavigation().moveTo(target, speed);
            return true;
        }
        long now = world.getGameTime();
        if (now >= NEXT_DEFENSE_HIT.getOrDefault(villager.getUUID(), 0L)) {
            villager.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            target.hurtServer(world, world.damageSources().mobAttack(villager), 3.0F);
            NEXT_DEFENSE_HIT.put(villager.getUUID(), now + 20L);
        }
        return true;
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
        Villager leader = stableTravelLeader(world, villager);
        if (leader != villager) {
            double distanceSquared = villager.distanceToSqr(leader);
            boolean swimming = villager.isInWater() || leader.isInWater();
            if (distanceSquared <= 9.0D && !swimming) {
                villager.getNavigation().stop();
                return true;
            }
            if (swimming) {
                villager.getNavigation().stop();
                if (distanceSquared <= SWIM_FOLLOW_STOP_DISTANCE_SQUARED) {
                    villager.getMoveControl().setWantedPosition(
                            villager.getX(), villager.getY() + 0.2D, villager.getZ(), speed * 0.35D
                    );
                } else {
                    villager.getMoveControl().setWantedPosition(
                            leader.getX(), leader.getY() + 0.35D, leader.getZ(), speed * 1.15D
                    );
                }
                if (villager.isInWater()) {
                    villager.getJumpControl().jump();
                }
                return true;
            }
            long gameTime = world.getGameTime();
            long nextRefresh = NEXT_FOLLOW_REFRESH.getOrDefault(villager.getUUID(), 0L);
            if (villager.getNavigation().isDone() || gameTime >= nextRefresh) {
                villager.getNavigation().moveTo(leader, speed * 1.15D);
                NEXT_FOLLOW_REFRESH.put(villager.getUUID(), gameTime + FOLLOW_REFRESH_TICKS);
            }
            return true;
        }
        double targetX = destination.getX() + 0.5D;
        double targetZ = destination.getZ() + 0.5D;
        double dx = targetX - villager.getX();
        double dz = targetZ - villager.getZ();
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
        if (horizontalDistance <= 3.0D && !villager.isInWater()) {
            villager.getNavigation().stop();
            NEXT_MOVE_REFRESH.remove(villager.getUUID());
            MOVE_FAILURES.remove(villager.getUUID());
            ACTIVE_SWIM_TARGETS.remove(villager.getUUID());
            SWIM_PROGRESS.remove(villager.getUUID());
            return true;
        }

        if (tickWaterTraversal(world, villager, dx, dz, horizontalDistance, speed)) {
            return true;
        }

        boolean stuck = ValetGroupExcavation.observeAndIsStuck(villager);
        boolean excavationActive = ValetGroupExcavation.hasActivePath(villager);
        if (excavationActive || stuck) {
            if (!excavationActive) {
                villager.getNavigation().stop();
            }
            if (ValetGroupExcavation.tick(world, villager, destination)) {
                return true;
            }
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
        int currentY = villager.blockPosition().getY();
        if (!world.getFluidState(villager.blockPosition()).isEmpty()) {
            BlockPos landing = findLanding(world, villager, dx, dz);
            if (landing != null) {
                Path landingPath = ValetSafeNavigation.createSafeLocalPath(world, villager, landing, 2, true, 32);
                if (landingPath != null && villager.getNavigation().moveTo(landingPath, speed * 1.2D)) {
                    if (villager.distanceToSqr(landing.getX() + 0.5D, landing.getY(), landing.getZ() + 0.5D) < 6.0D
                            && landing.getY() > villager.getY()) {
                        villager.getJumpControl().jump();
                    }
                    return true;
                }
            }
        }
        if (Math.abs(stepY - currentY) > 3) {
            villager.getNavigation().stop();
            if (ValetGroupExcavation.tick(world, villager, destination)) {
                return true;
            }
        }
        BlockPos stepTarget = new BlockPos(stepX, stepY, stepZ);
        Path safePath = ValetSafeNavigation.createSafeLocalPath(world, villager, stepTarget, 2, false, 64);
        boolean moving = safePath != null && villager.getNavigation().moveTo(safePath, speed);
        NEXT_MOVE_REFRESH.put(villager.getUUID(), gameTime + MOVE_REFRESH_TICKS);
        if (moving) {
            MOVE_FAILURES.put(villager.getUUID(), Math.max(0, failures - 1));
        } else {
            MOVE_FAILURES.put(villager.getUUID(), Math.min(6, failures + 1));
        }
        return true;
    }

    private static boolean tickWaterTraversal(
            ServerLevel world,
            Villager villager,
            double dx,
            double dz,
            double horizontalDistance,
            double speed
    ) {
        UUID uuid = villager.getUUID();
        SwimProgress progress = villager.isInWater() ? SWIM_PROGRESS.get(uuid) : null;
        boolean keepTarget = progress != null
                && isValidSwimTarget(world, progress.target())
                && !hasReachedSwimTarget(villager, progress.target());
        SwimRoute route = keepTarget ? null : findSwimRoute(world, villager, dx, dz, horizontalDistance, 0.0D);
        if (!keepTarget && route == null) {
            ACTIVE_SWIM_TARGETS.remove(villager.getUUID());
            SWIM_PROGRESS.remove(uuid);
            return false;
        }

        long gameTime = world.getGameTime();
        long nextRefresh = NEXT_MOVE_REFRESH.getOrDefault(villager.getUUID(), 0L);
        if (!villager.isInWater() && route != null && route.approach() != null
                && !isCloseTo(villager, route.approach(), 2.25D)) {
            SWIM_PROGRESS.remove(uuid);
            if (!villager.getNavigation().isDone() && gameTime < nextRefresh) {
                return true;
            }
            Path approachPath = ValetSafeNavigation.createSafeLocalPath(
                    world,
                    villager,
                    route.approach(),
                    2,
                    false,
                    SWIM_MAX_NODES
            );
            boolean approaching = approachPath != null && villager.getNavigation().moveTo(approachPath, speed);
            NEXT_MOVE_REFRESH.put(villager.getUUID(), gameTime + MOVE_REFRESH_TICKS);
            if (approaching) {
                ValetGroupExcavation.clear(villager.getUUID());
                recordSwimTarget(villager, route.approach(), "group swim_approach target=");
                return true;
            }
            ACTIVE_SWIM_TARGETS.remove(villager.getUUID());
            return false;
        }

        if (!villager.isInWater() && route != null && route.firstWater() != null
                && villager.distanceToSqr(route.firstWater().getX() + 0.5D, villager.getY(), route.firstWater().getZ() + 0.5D) > 6.25D) {
            ACTIVE_SWIM_TARGETS.remove(villager.getUUID());
            SWIM_PROGRESS.remove(uuid);
            return false;
        }

        ValetGroupExcavation.clear(villager.getUUID());
        villager.getNavigation().stop();
        BlockPos swimTarget;
        if (keepTarget) {
            progress = updateSwimProgress(villager, progress);
            if (progress.noProgressTicks() >= SWIM_NO_PROGRESS_TICKS) {
                int recoveryAttempt = progress.recoveryAttempt() + 1;
                SwimRoute recoveryRoute = findSwimRoute(
                        world,
                        villager,
                        dx,
                        dz,
                        horizontalDistance,
                        swimRecoveryAngle(recoveryAttempt)
                );
                if (recoveryRoute != null && !recoveryRoute.target().equals(progress.target())) {
                    swimTarget = recoveryRoute.target();
                    progress = new SwimProgress(
                            swimTarget.immutable(),
                            swimDistanceSquared(villager, swimTarget),
                            0,
                            recoveryAttempt
                    );
                    ValetDebug.record(villager, "group swim_recovery retry=" + recoveryAttempt
                            + " target=" + ValetDebug.shortPos(swimTarget));
                    ACTIVE_SWIM_TARGETS.put(uuid, swimTarget.immutable());
                } else {
                    swimTarget = progress.target();
                    progress = new SwimProgress(
                            swimTarget,
                            swimDistanceSquared(villager, swimTarget),
                            0,
                            recoveryAttempt
                    );
                    ValetDebug.record(villager, "group swim_recovery retry=" + recoveryAttempt
                            + " target_unchanged=" + ValetDebug.shortPos(swimTarget));
                }
            } else {
                swimTarget = progress.target();
            }
        } else {
            swimTarget = route.target();
            progress = new SwimProgress(
                    swimTarget.immutable(),
                    swimDistanceSquared(villager, swimTarget),
                    0,
                    0
            );
        }
        SWIM_PROGRESS.put(uuid, progress);
        boolean waterTarget = world.getFluidState(swimTarget).is(FluidTags.WATER);
        double targetY = swimTarget.getY() + (waterTarget ? 0.8D : 0.2D);
        villager.getMoveControl().setWantedPosition(
                swimTarget.getX() + 0.5D,
                targetY,
                swimTarget.getZ() + 0.5D,
                speed * 1.2D
        );
        if (villager.isInWater()) {
            villager.getJumpControl().jump();
        }
        MOVE_FAILURES.put(villager.getUUID(), 0);
        recordSwimTarget(villager, swimTarget, "group swim_start target=");
        return true;
    }

    private static SwimRoute findSwimRoute(
            ServerLevel world,
            Villager villager,
            double dx,
            double dz,
            double horizontalDistance,
            double angleOffset
    ) {
        double length = Math.max(0.001D, horizontalDistance);
        double directionX = dx / length;
        double directionZ = dz / length;
        double cos = Math.cos(angleOffset);
        double sin = Math.sin(angleOffset);
        double probeX = directionX * cos - directionZ * sin;
        double probeZ = directionX * sin + directionZ * cos;
        int probeDistance = Math.min(SWIM_PROBE_DISTANCE, Math.max(1, (int) Math.ceil(horizontalDistance)));
        boolean foundWater = villager.isInWater();
        BlockPos approach = foundWater ? null : ValetSafeNavigation.isSafeStand(world, villager.blockPosition(), 2)
                ? villager.blockPosition().immutable()
                : null;
        BlockPos firstWater = null;
        BlockPos farthestWater = null;
        for (int distance = 1; distance <= probeDistance; distance++) {
            int x = (int) Math.floor(villager.getX() + probeX * distance);
            int z = (int) Math.floor(villager.getZ() + probeZ * distance);
            if (!world.hasChunk(x >> 4, z >> 4)) {
                break;
            }

            int surface = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos surfaceBlock = new BlockPos(x, surface - 1, z);
            if (world.getFluidState(surfaceBlock).is(FluidTags.WATER)
                    && ValetSafeNavigation.isSafeWaterColumn(world, surfaceBlock, 2)) {
                foundWater = true;
                if (firstWater == null) {
                    firstWater = surfaceBlock;
                }
                farthestWater = surfaceBlock;
                continue;
            }

            BlockPos shore = new BlockPos(x, surface, z);
            if (foundWater && ValetSafeNavigation.isSafeStand(world, shore, 2)) {
                return new SwimRoute(approach, firstWater, shore);
            }
            if (foundWater) {
                break;
            }
            if (ValetSafeNavigation.isSafeStand(world, shore, 2)) {
                approach = shore;
            }
        }
        return farthestWater == null ? null : new SwimRoute(approach, firstWater, farthestWater);
    }

    private static SwimProgress updateSwimProgress(Villager villager, SwimProgress progress) {
        double distanceSquared = swimDistanceSquared(villager, progress.target());
        if (distanceSquared + SWIM_PROGRESS_EPSILON < progress.bestDistanceSquared()) {
            return new SwimProgress(progress.target(), distanceSquared, 0, progress.recoveryAttempt());
        }
        return new SwimProgress(
                progress.target(),
                progress.bestDistanceSquared(),
                progress.noProgressTicks() + 1,
                progress.recoveryAttempt()
        );
    }

    private static boolean isValidSwimTarget(ServerLevel world, BlockPos target) {
        return world.getFluidState(target).is(FluidTags.WATER)
                ? ValetSafeNavigation.isSafeWaterColumn(world, target, 2)
                : ValetSafeNavigation.isSafeStand(world, target, 2);
    }

    private static boolean hasReachedSwimTarget(Villager villager, BlockPos target) {
        double dx = target.getX() + 0.5D - villager.getX();
        double dz = target.getZ() + 0.5D - villager.getZ();
        return dx * dx + dz * dz <= SWIM_TARGET_REACHED_DISTANCE_SQUARED
                && Math.abs(target.getY() + 0.5D - villager.getY()) <= 2.5D;
    }

    private static double swimDistanceSquared(Villager villager, BlockPos target) {
        double dx = target.getX() + 0.5D - villager.getX();
        double dz = target.getZ() + 0.5D - villager.getZ();
        return dx * dx + dz * dz;
    }

    private static double swimRecoveryAngle(int recoveryAttempt) {
        int attempt = Math.floorMod(recoveryAttempt - 1, 6) + 1;
        return detourAngle(attempt);
    }

    private static void recordSwimTarget(Villager villager, BlockPos target, String prefix) {
        BlockPos previous = ACTIVE_SWIM_TARGETS.put(villager.getUUID(), target.immutable());
        if (!target.equals(previous)) {
            ValetDebug.record(villager, prefix + ValetDebug.shortPos(target));
        }
    }

    private static boolean isCloseTo(Villager villager, BlockPos target, double horizontalDistanceSquared) {
        double dx = target.getX() + 0.5D - villager.getX();
        double dz = target.getZ() + 0.5D - villager.getZ();
        return dx * dx + dz * dz <= horizontalDistanceSquared && Math.abs(target.getY() - villager.getY()) <= 1.5D;
    }

    private static Villager stableTravelLeader(ServerLevel world, Villager villager) {
        ValetGroup group = groupFor(world, villager);
        if (group == null) {
            return villager;
        }
        UUID selectedUuid = TRAVEL_LEADERS.get(group.id());
        Entity selected = selectedUuid == null ? null : world.getEntity(selectedUuid);
        if (selected instanceof Villager current && current.isAlive() && group.hasMember(current.getUUID())) {
            return current;
        }
        Villager leader = null;
        for (UUID member : group.members()) {
            Entity entity = world.getEntity(member);
            if (entity instanceof Villager candidate
                    && candidate.isAlive()
                    && (leader == null || candidate.getUUID().toString().compareTo(leader.getUUID().toString()) < 0)) {
                leader = candidate;
            }
        }
        if (leader == null) {
            return villager;
        }
        TRAVEL_LEADERS.put(group.id(), leader.getUUID());
        return leader;
    }

    private static ValetGroup groupFor(ServerLevel world, Villager villager) {
        ValetGroupStorage storage = ValetGroupStorage.get(world);
        return storage.getGroup(storage.getGroupIdForMember(villager.getUUID()));
    }

    private static BlockPos findLanding(ServerLevel world, Villager villager, double dx, double dz) {
        double length = Math.max(0.001D, Math.sqrt(dx * dx + dz * dz));
        for (int distance = 1; distance <= 8; distance++) {
            int x = (int) Math.floor(villager.getX() + dx / length * distance);
            int z = (int) Math.floor(villager.getZ() + dz / length * distance);
            int surface = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos stand = new BlockPos(x, surface, z);
            if (ValetSafeNavigation.isSafeStand(world, stand, 2)) {
                return stand;
            }
        }
        return null;
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
        NEXT_FOLLOW_REFRESH.remove(valetUuid);
        MOVE_FAILURES.remove(valetUuid);
        NEXT_DEFENSE_HIT.remove(valetUuid);
        ACTIVE_SWIM_TARGETS.remove(valetUuid);
        SWIM_PROGRESS.remove(valetUuid);
        TRAVEL_LEADERS.entrySet().removeIf(entry -> entry.getValue().equals(valetUuid));
        ValetGroupExcavation.clear(valetUuid);
    }

    public static void clearAll() {
        NEXT_MOVE_REFRESH.clear();
        NEXT_FOLLOW_REFRESH.clear();
        MOVE_FAILURES.clear();
        NEXT_DEFENSE_HIT.clear();
        ACTIVE_SWIM_TARGETS.clear();
        SWIM_PROGRESS.clear();
        TRAVEL_LEADERS.clear();
        ValetGroupExcavation.clearAll();
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

    private record SwimRoute(BlockPos approach, BlockPos firstWater, BlockPos target) {
    }

    private record SwimProgress(
            BlockPos target,
            double bestDistanceSquared,
            int noProgressTicks,
            int recoveryAttempt
    ) {
    }
}
