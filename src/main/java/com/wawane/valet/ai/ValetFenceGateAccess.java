package com.wawane.valet.ai;

import com.wawane.valet.ValetDebug;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;

/** Interaction de portillon bornee, sans mouvement force ni modification du pathfinder. */
public final class ValetFenceGateAccess {
    private static final int BLOCKING_GATE_RADIUS = 2;
    private static final int MAX_GATE_PROBE_GOALS = 8;
    private static final int MIN_OPEN_TICKS = 10;
    private static final int FAILED_PATH_CLOSE_TICKS = 80;
    private static final int HARD_MAX_OPEN_TICKS = 200;
    private static final double APPROACH_DISTANCE_SQUARED = 6.25D;
    private static final Map<ResourceKey<Level>, Map<BlockPos, OpenedGate>> OPENED_GATES = new ConcurrentHashMap<>();

    private ValetFenceGateAccess() {
    }

    public static BlockPos tryOpenForPath(
            ServerLevel world,
            Villager villager,
            Collection<BlockPos> goals,
            ValetWorkZone.Zone zone
    ) {
        BlockPos gate = findBlockingGate(world, villager, goals, zone);
        if (gate == null) {
            return null;
        }

        BlockState state = world.getBlockState(gate);
        if (!world.setBlock(gate, state.setValue(FenceGateBlock.OPEN, true), Block.UPDATE_ALL)) {
            return null;
        }
        OPENED_GATES.computeIfAbsent(world.dimension(), ignored -> new ConcurrentHashMap<>())
                .put(gate, new OpenedGate(
                        villager.getUUID(),
                        world.getGameTime(),
                        Integer.signum(sideOfGate(villager.blockPosition(), gate, state))
                ));
        ValetDebug.record(villager, "brain gate_open pos=" + ValetDebug.shortPos(gate));
        return gate;
    }

    /** Ferme les portillons meme si leur valet proprietaire vient d'etre decharge. */
    public static void tick(ServerLevel world) {
        Map<BlockPos, OpenedGate> gates = OPENED_GATES.get(world.dimension());
        if (gates == null) {
            return;
        }
        gates.entrySet().removeIf(entry -> closeIfReady(world, entry.getKey(), entry.getValue()));
        if (gates.isEmpty()) {
            OPENED_GATES.remove(world.dimension(), gates);
        }
    }

    /** Fermeture deterministe avant la sauvegarde finale du serveur. */
    public static void closeAll(MinecraftServer server) {
        for (Map.Entry<ResourceKey<Level>, Map<BlockPos, OpenedGate>> dimension : OPENED_GATES.entrySet()) {
            ServerLevel world = server.getLevel(dimension.getKey());
            if (world == null) {
                continue;
            }
            for (BlockPos pos : dimension.getValue().keySet()) {
                BlockState state = world.getBlockState(pos);
                if (isOpenGate(state)) {
                    world.setBlock(pos, state.setValue(FenceGateBlock.OPEN, false), Block.UPDATE_ALL);
                }
            }
        }
        OPENED_GATES.clear();
    }

    public static void clearAll() {
        OPENED_GATES.clear();
    }

    /** Annule immediatement une ouverture qui n'a produit aucun chemin valide. */
    public static void rollbackLastOpen(ServerLevel world, Villager villager) {
        Map<BlockPos, OpenedGate> gates = OPENED_GATES.get(world.dimension());
        if (gates == null) {
            return;
        }
        long now = world.getGameTime();
        gates.entrySet().removeIf(entry -> {
            OpenedGate gate = entry.getValue();
            if (!gate.owner().equals(villager.getUUID()) || gate.openedAt() != now) {
                return false;
            }
            BlockState state = world.getBlockState(entry.getKey());
            return !isOpenGate(state)
                    || isPassageClear(world, entry.getKey())
                    && world.setBlock(entry.getKey(), state.setValue(FenceGateBlock.OPEN, false), Block.UPDATE_ALL);
        });
    }

    private static BlockPos findBlockingGate(
            ServerLevel world,
            Villager villager,
            Collection<BlockPos> goals,
            ValetWorkZone.Zone zone
    ) {
        BlockPos origin = villager.blockPosition();
        BlockPos best = null;
        double bestScore = Double.MAX_VALUE;
        int probedGoals = 0;
        for (BlockPos goal : goals) {
            if (probedGoals++ >= MAX_GATE_PROBE_GOALS) {
                break;
            }
            if (goal == null) {
                continue;
            }
            Path partial = villager.getNavigation().createPath(goal, 0);
            if (partial == null || partial.canReach() || partial.getNodeCount() == 0) {
                continue;
            }
            BlockPos endpoint = partial.getNodePos(partial.getNodeCount() - 1);
            for (BlockPos candidate : BlockPos.withinManhattan(
                    endpoint,
                    BLOCKING_GATE_RADIUS,
                    BLOCKING_GATE_RADIUS,
                    BLOCKING_GATE_RADIUS
            )) {
                BlockPos gate = candidate.immutable();
                BlockState state = world.getBlockState(gate);
                if (!zone.contains(gate)
                        || !isClosedGate(state)
                        || !separatesOriginFromGoal(origin, gate, state, java.util.List.of(goal))) {
                    continue;
                }
                double score = endpoint.distSqr(gate) * 1024.0D + origin.distSqr(gate);
                if (score < bestScore) {
                    best = gate;
                    bestScore = score;
                }
            }
        }
        if (best != null) {
            return best;
        }

        double nearestDistance = Double.MAX_VALUE;
        for (BlockPos candidate : BlockPos.withinManhattan(origin, 2, 2, 2)) {
            BlockPos gate = candidate.immutable();
            BlockState state = world.getBlockState(gate);
            if (!zone.contains(gate)
                    || !isClosedGate(state)
                    || !separatesOriginFromGoal(origin, gate, state, goals)) {
                continue;
            }
            double distance = origin.distSqr(gate);
            if (distance < nearestDistance) {
                best = gate;
                nearestDistance = distance;
            }
        }
        return best;
    }

    private static boolean separatesOriginFromGoal(
            BlockPos origin,
            BlockPos gate,
            BlockState state,
            Collection<BlockPos> goals
    ) {
        net.minecraft.core.Direction facing = state.getValue(FenceGateBlock.FACING);
        int originSide = (origin.getX() - gate.getX()) * facing.getStepX()
                + (origin.getZ() - gate.getZ()) * facing.getStepZ();
        if (originSide == 0) {
            return false;
        }
        for (BlockPos goal : goals) {
            if (goal == null) {
                continue;
            }
            int goalSide = (goal.getX() - gate.getX()) * facing.getStepX()
                    + (goal.getZ() - gate.getZ()) * facing.getStepZ();
            if (goalSide != 0 && Integer.signum(originSide) != Integer.signum(goalSide)) {
                return true;
            }
        }
        return false;
    }

    private static boolean closeIfReady(ServerLevel world, BlockPos pos, OpenedGate gate) {
        if (!isOpenGate(world.getBlockState(pos))) {
            return true;
        }
        long openTicks = world.getGameTime() - gate.openedAt();
        Entity owner = world.getEntityInAnyDimension(gate.owner());
        boolean ownerLoaded = owner != null && owner.level() == world && !owner.isRemoved();
        double ownerDistance = ownerLoaded
                ? owner.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D)
                : Double.MAX_VALUE;
        if (ownerLoaded && ownerDistance <= APPROACH_DISTANCE_SQUARED) {
            gate.markApproached();
        }
        int currentSide = ownerLoaded
                ? Integer.signum(sideOfGate(owner.blockPosition(), pos, world.getBlockState(pos)))
                : 0;
        boolean crossed = ownerLoaded
                && gate.openedSide() != 0
                && currentSide != 0
                && currentSide != gate.openedSide();
        boolean passed = crossed || gate.approached() && (!ownerLoaded || ownerDistance > 9.0D);
        boolean navigationFinished = owner instanceof Villager villager && villager.getNavigation().isDone();
        boolean timedOut = openTicks >= FAILED_PATH_CLOSE_TICKS && (!ownerLoaded || navigationFinished);
        boolean hardTimedOut = openTicks >= HARD_MAX_OPEN_TICKS;
        if (!isPassageClear(world, pos) || openTicks < MIN_OPEN_TICKS || !passed && !timedOut && !hardTimedOut) {
            return false;
        }

        BlockState state = world.getBlockState(pos);
        if (!world.setBlock(pos, state.setValue(FenceGateBlock.OPEN, false), Block.UPDATE_ALL)) {
            return false;
        }
        if (owner instanceof Villager villager) {
            ValetDebug.record(villager, "brain gate_close pos=" + ValetDebug.shortPos(pos));
        }
        return true;
    }

    private static boolean isPassageClear(ServerLevel world, BlockPos pos) {
        return world.getEntitiesOfClass(
                LivingEntity.class,
                new AABB(pos).inflate(0.2D),
                LivingEntity::isAlive
        ).isEmpty();
    }

    private static int sideOfGate(BlockPos entityPos, BlockPos gatePos, BlockState state) {
        net.minecraft.core.Direction facing = state.getValue(FenceGateBlock.FACING);
        return (entityPos.getX() - gatePos.getX()) * facing.getStepX()
                + (entityPos.getZ() - gatePos.getZ()) * facing.getStepZ();
    }

    private static boolean isClosedGate(BlockState state) {
        return state.getBlock() instanceof FenceGateBlock
                && state.hasProperty(FenceGateBlock.OPEN)
                && !state.getValue(FenceGateBlock.OPEN);
    }

    private static boolean isOpenGate(BlockState state) {
        return state.getBlock() instanceof FenceGateBlock
                && state.hasProperty(FenceGateBlock.OPEN)
                && state.getValue(FenceGateBlock.OPEN);
    }

    private static final class OpenedGate {
        private final UUID owner;
        private final long openedAt;
        private final int openedSide;
        private boolean approached;

        private OpenedGate(UUID owner, long openedAt, int openedSide) {
            this.owner = owner;
            this.openedAt = openedAt;
            this.openedSide = openedSide;
        }

        private UUID owner() {
            return owner;
        }

        private long openedAt() {
            return openedAt;
        }

        private int openedSide() {
            return openedSide;
        }

        private boolean approached() {
            return approached;
        }

        private void markApproached() {
            approached = true;
        }
    }
}
