package com.wawane.valet.ai;

import com.wawane.valet.ValetMod;
import com.wawane.valet.order.ValetOrder;
import com.wawane.valet.order.ValetOrders;
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
    private static final Map<UUID, Runtime> RUNTIMES = new ConcurrentHashMap<>();

    private ValetWorkDriver() {
    }

    public static void tick(ServerWorld world) {
        Set<UUID> seen = new HashSet<>();
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

    private static boolean isValet(VillagerEntity villager) {
        return villager.getVillagerData().getProfession() == ValetMod.VALET_PROFESSION;
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
                ValetMod.LOGGER.info("Valet {} switches order {} -> {}", villager.getUuid(), runtime.orderKey, orderKey);
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
            ValetMod.LOGGER.info("Valet {} starts order {}", villager.getUuid(), orderKey);
            runtime.goal.tick();
        }
    }

    private static String orderKey(VillagerEntity villager) {
        ValetOrder order = ValetOrders.get(villager);
        return switch (order) {
            case MINE_ORES -> order.getId() + ":" + ValetOrders.getMineTarget(villager);
            case CHOP_WOOD -> order.getId() + ":" + ValetOrders.getWoodTarget(villager);
            case BUILD_STRUCTURE -> order.getId() + ":" + ValetOrders.getConstructionTargetId(villager);
            case NONE -> order.getId();
        };
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
