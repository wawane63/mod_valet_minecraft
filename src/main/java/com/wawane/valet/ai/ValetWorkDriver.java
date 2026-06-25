package com.wawane.valet.ai;

import com.wawane.valet.ValetMod;
import com.wawane.valet.ai.core.ValetOrderKey;
import com.wawane.valet.order.ValetOrder;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ValetWorkDriver {
    private static final int PLAYER_WORK_SCAN_RADIUS = 96;
    private static final int VALET_DISCOVERY_INTERVAL_TICKS = 20;
    private static final Map<UUID, Runtime> RUNTIMES = new ConcurrentHashMap<>();

    private ValetWorkDriver() {
    }

    public static void tick(ServerWorld world) {
        Set<UUID> seen = new HashSet<>();
        for (Runtime runtime : RUNTIMES.values()) {
            if (runtime.villager.getWorld() == world && isValet(runtime.villager) && isNearAnyPlayer(world, runtime.villager)) {
                seen.add(runtime.villager.getUuid());
                tickVillager(runtime.villager);
            }
        }

        if (world.getTime() % VALET_DISCOVERY_INTERVAL_TICKS != 0) {
            return;
        }

        for (ServerPlayerEntity player : world.getPlayers()) {
            Box searchBox = Box.from(player.getPos()).expand(PLAYER_WORK_SCAN_RADIUS);
            for (VillagerEntity villager : world.getEntitiesByClass(VillagerEntity.class, searchBox, ValetWorkDriver::isValet)) {
                if (seen.add(villager.getUuid())) {
                    tickVillager(villager);
                }
            }
        }
    }

    public static void clear(UUID uuid) {
        Runtime runtime = RUNTIMES.remove(uuid);
        ValetWorkGoal.clearRestartRequest(uuid);
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
    }

    public static String describe(VillagerEntity villager) {
        Runtime runtime = RUNTIMES.get(villager.getUuid());
        if (runtime == null) {
            return shortUuid(villager.getUuid()) + " runtime=absent order=" + orderKey(villager);
        }
        return shortUuid(villager.getUuid()) + " running=" + runtime.running + " " + runtime.goal.debugSummary();
    }

    private static boolean isValet(VillagerEntity villager) {
        return !villager.isRemoved()
                && villager.getVillagerData().getProfession() == ValetMod.VALET_PROFESSION;
    }

    private static boolean isNearAnyPlayer(ServerWorld world, VillagerEntity villager) {
        double maxDistanceSquared = PLAYER_WORK_SCAN_RADIUS * PLAYER_WORK_SCAN_RADIUS;
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (player.squaredDistanceTo(villager) <= maxDistanceSquared) {
                return true;
            }
        }
        return false;
    }

    private static void tickVillager(VillagerEntity villager) {
        Runtime runtime = RUNTIMES.compute(villager.getUuid(), (uuid, current) -> {
            if (current == null || current.villager != villager || current.villager.isRemoved()) {
                return new Runtime(villager);
            }
            return current;
        });

        String orderKey = orderKey(villager);
        if (runtime.running) {
            if (!orderKey.equals(runtime.orderKey)) {
                runtime.goal.stop();
                runtime.running = false;
            } else if (runtime.goal.shouldContinue()) {
                runtime.goal.tick();
                return;
            } else {
                runtime.goal.stop();
                runtime.running = false;
            }
        }

        if (runtime.goal.canStart()) {
            runtime.goal.start();
            runtime.running = true;
            runtime.orderKey = orderKey;
            runtime.goal.tick();
        }
    }

    private static String orderKey(VillagerEntity villager) {
        return ValetOrderKey.of(villager);
    }

    private static String shortUuid(UUID uuid) {
        return uuid.toString().substring(0, 8);
    }

    private static final class Runtime {
        private final VillagerEntity villager;
        private final ValetWorkGoal goal;
        private boolean running;
        private String orderKey = ValetOrder.NONE.getId();

        private Runtime(VillagerEntity villager) {
            this.villager = villager;
            this.goal = new ValetWorkGoal(villager);
        }
    }
}
