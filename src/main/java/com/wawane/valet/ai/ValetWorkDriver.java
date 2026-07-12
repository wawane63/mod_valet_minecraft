package com.wawane.valet.ai;

import com.wawane.valet.ValetMod;
import com.wawane.valet.ai.core.ValetBlockReservations;
import com.wawane.valet.ai.core.ValetEntityReservations;
import com.wawane.valet.ai.core.ValetOrderKey;
import com.wawane.valet.order.ValetOrder;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.phys.AABB;

public final class ValetWorkDriver {
    private static final int PLAYER_WORK_SCAN_RADIUS = 96;
    private static final int VALET_DISCOVERY_INTERVAL_TICKS = 20;
    private static final Map<UUID, Runtime> RUNTIMES = new ConcurrentHashMap<>();

    private ValetWorkDriver() {
    }

    public static void tick(ServerLevel world) {
        boolean discoverValets = world.getGameTime() % VALET_DISCOVERY_INTERVAL_TICKS == 0;
        Set<UUID> seen = discoverValets ? new HashSet<>() : null;
        for (Runtime runtime : RUNTIMES.values()) {
            if (runtime.villager.level() == world && isValet(runtime.villager) && isNearAnyPlayer(world, runtime.villager)) {
                if (seen != null) {
                    seen.add(runtime.villager.getUUID());
                }
                tickVillager(runtime.villager);
            }
        }

        if (!discoverValets) {
            return;
        }

        for (ServerPlayer player : world.players()) {
            AABB searchBox = AABB.unitCubeFromLowerCorner(player.position()).inflate(PLAYER_WORK_SCAN_RADIUS);
            for (Villager villager : world.getEntitiesOfClass(Villager.class, searchBox, ValetWorkDriver::isValet)) {
                if (seen.add(villager.getUUID())) {
                    tickVillager(villager);
                }
            }
        }
    }

    public static void clear(UUID uuid) {
        Runtime runtime = RUNTIMES.remove(uuid);
        ValetWorkGoal.clearRestartRequest(uuid);
        ValetBlockReservations.releaseAll(uuid);
        ValetEntityReservations.releaseAll(uuid);
        if (runtime != null && runtime.running) {
            runtime.goal.stop();
        }
    }

    public static void clearAll() {
        for (Runtime runtime : RUNTIMES.values()) {
            if (runtime.running) {
                runtime.goal.stop();
            }
        }
        RUNTIMES.clear();
        ValetBlockReservations.clearAll();
        ValetEntityReservations.clearAll();
    }

    public static String describe(Villager villager) {
        Runtime runtime = RUNTIMES.get(villager.getUUID());
        if (runtime == null) {
            return shortUuid(villager.getUUID()) + " runtime=absent order=" + orderKey(villager);
        }
        return shortUuid(villager.getUUID()) + " running=" + runtime.running + " " + runtime.goal.debugSummary();
    }

    private static boolean isValet(Villager villager) {
        return !villager.isRemoved()
                && ValetMod.isValet(villager);
    }

    private static boolean isNearAnyPlayer(ServerLevel world, Villager villager) {
        double maxDistanceSquared = PLAYER_WORK_SCAN_RADIUS * PLAYER_WORK_SCAN_RADIUS;
        for (ServerPlayer player : world.players()) {
            if (player.distanceToSqr(villager) <= maxDistanceSquared) {
                return true;
            }
        }
        return false;
    }

    private static void tickVillager(Villager villager) {
        Runtime runtime = RUNTIMES.compute(villager.getUUID(), (uuid, current) -> {
            if (current == null || current.villager != villager || current.villager.isRemoved()) {
                if (current != null && current.running) {
                    current.goal.stop();
                }
                return new Runtime(villager);
            }
            return current;
        });

        String orderKey = orderKey(villager);
        if (runtime.running) {
            if (!orderKey.equals(runtime.orderKey)) {
                runtime.goal.stop();
                runtime.running = false;
            } else if (runtime.goal.canContinueToUse()) {
                runtime.goal.tick();
                return;
            } else {
                runtime.goal.stop();
                runtime.running = false;
            }
        }

        if (runtime.goal.canUse()) {
            runtime.goal.start();
            runtime.running = true;
            runtime.orderKey = orderKey;
            runtime.goal.tick();
        }
    }

    private static String orderKey(Villager villager) {
        return ValetOrderKey.of(villager);
    }

    private static String shortUuid(UUID uuid) {
        return uuid.toString().substring(0, 8);
    }

    private static final class Runtime {
        private final Villager villager;
        private final ValetWorkGoal goal;
        private boolean running;
        private String orderKey = ValetOrder.NONE.getId();

        private Runtime(Villager villager) {
            this.villager = villager;
            this.goal = new ValetWorkGoal(villager);
        }
    }
}
