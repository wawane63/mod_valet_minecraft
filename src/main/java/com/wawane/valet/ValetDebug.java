package com.wawane.valet;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.wawane.valet.ai.ValetWorkDriver;
import com.wawane.valet.state.ValetData;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.phys.AABB;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static net.minecraft.commands.Commands.literal;

public final class ValetDebug {
    private static final int RANGE = 96;
    private static final int ACTIONBAR_INTERVAL_TICKS = 20;
    private static final int LOG_REPEAT_INTERVAL_TICKS = 40;
    private static final Set<UUID> VIEWERS = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, String> LAST_EVENTS = new ConcurrentHashMap<>();
    private static final Map<UUID, String> LAST_LOG_LINES = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_LOG_TICKS = new ConcurrentHashMap<>();
    private static volatile boolean verboseLog = true;

    private ValetDebug() {
    }

    public static void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> register(dispatcher));
    }

    public static void tick(ServerLevel world) {
        if (VIEWERS.isEmpty() || world.getGameTime() % ACTIONBAR_INTERVAL_TICKS != 0) {
            return;
        }

        for (ServerPlayer player : world.players()) {
            if (!VIEWERS.contains(player.getUUID())) {
                continue;
            }

            Optional<Villager> nearest = nearestValet(player);
            if (nearest.isEmpty()) {
                player.sendOverlayMessage(Component.literal("[Valet] aucun valet proche").withStyle(ChatFormatting.YELLOW));
                continue;
            }

            Villager villager = nearest.get();
            String event = LAST_EVENTS.getOrDefault(villager.getUUID(), "event=aucun");
            player.sendOverlayMessage(Component.literal("[Valet] profession=" + villager.getVillagerData().profession()
                    + " " + ValetWorkDriver.describe(villager) + " | " + event));
        }
    }

    public static void record(Villager villager, String message) {
        String line = shortUuid(villager.getUUID()) + " " + shortPos(villager.blockPosition()) + " " + message;
        LAST_EVENTS.put(villager.getUUID(), line);
        if (shouldWriteLog(villager, line)) {
            ValetMod.LOGGER.info("[valet-debug] {}", line);
        }
    }

    public static void clear(UUID uuid) {
        VIEWERS.remove(uuid);
        LAST_EVENTS.remove(uuid);
        LAST_LOG_LINES.remove(uuid);
        LAST_LOG_TICKS.remove(uuid);
    }

    public static void clearAll() {
        VIEWERS.clear();
        LAST_EVENTS.clear();
        LAST_LOG_LINES.clear();
        LAST_LOG_TICKS.clear();
    }

    public static String shortPos(BlockPos pos) {
        return pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(literal("valetdebug")
                .then(literal("on").executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    VIEWERS.add(player.getUUID());
                    verboseLog = true;
                    context.getSource().sendSuccess(() -> Component.literal("Valet debug ON").withStyle(ChatFormatting.GREEN), false);
                    return Command.SINGLE_SUCCESS;
                }))
                .then(literal("off").executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    VIEWERS.remove(player.getUUID());
                    context.getSource().sendSuccess(() -> Component.literal("Valet debug OFF").withStyle(ChatFormatting.GRAY), false);
                    return Command.SINGLE_SUCCESS;
                }))
                .then(literal("log").executes(context -> {
                    verboseLog = true;
                    context.getSource().sendSuccess(() -> Component.literal("Valet debug log always ON").withStyle(ChatFormatting.GREEN), false);
                    return Command.SINGLE_SUCCESS;
                }))
                .then(literal("dump").executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    dumpNearby(player);
                    return Command.SINGLE_SUCCESS;
                }))
        );
    }

    private static void dumpNearby(ServerPlayer player) {
        AABB searchBox = AABB.unitCubeFromLowerCorner(player.position()).inflate(RANGE);
        var valets = player.level().getEntitiesOfClass(Villager.class, searchBox, ValetDebug::isDebugTarget);
        if (valets.isEmpty()) {
            player.sendSystemMessage(Component.literal("[Valet] aucun valet proche").withStyle(ChatFormatting.YELLOW));
            return;
        }

        for (Villager villager : valets) {
            String event = LAST_EVENTS.getOrDefault(villager.getUUID(), "event=aucun");
            player.sendSystemMessage(Component.literal("[Valet] profession=" + villager.getVillagerData().profession()
                    + " " + ValetWorkDriver.describe(villager) + " | " + event));
        }
    }

    private static Optional<Villager> nearestValet(ServerPlayer player) {
        AABB searchBox = AABB.unitCubeFromLowerCorner(player.position()).inflate(RANGE);
        return player.level().getEntitiesOfClass(Villager.class, searchBox, ValetDebug::isDebugTarget)
                .stream()
                .min(Comparator.comparingDouble(villager -> player.distanceToSqr(villager)));
    }

    private static boolean shouldWriteLog(Villager villager, String line) {
        if (!verboseLog) {
            return false;
        }

        long tick = villager.level() instanceof ServerLevel world ? world.getGameTime() : 0L;
        UUID uuid = villager.getUUID();
        String previous = LAST_LOG_LINES.get(uuid);
        long previousTick = LAST_LOG_TICKS.getOrDefault(uuid, Long.MIN_VALUE);
        if (line.equals(previous) && tick - previousTick < LOG_REPEAT_INTERVAL_TICKS) {
            return false;
        }

        LAST_LOG_LINES.put(uuid, line);
        LAST_LOG_TICKS.put(uuid, tick);
        return true;
    }

    private static boolean isDebugTarget(Villager villager) {
        return ValetMod.isValet(villager) || ValetData.hasRuntimeData(villager);
    }

    private static String shortUuid(UUID uuid) {
        return uuid.toString().substring(0, 8);
    }
}
