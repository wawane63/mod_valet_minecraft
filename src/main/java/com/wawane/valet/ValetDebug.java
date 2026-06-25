package com.wawane.valet;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.wawane.valet.ai.ValetWorkDriver;
import com.wawane.valet.state.ValetData;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static net.minecraft.server.command.CommandManager.literal;

public final class ValetDebug {
    private static final int RANGE = 96;
    private static final int ACTIONBAR_INTERVAL_TICKS = 20;
    private static final Set<UUID> VIEWERS = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, String> LAST_EVENTS = new ConcurrentHashMap<>();
    private static volatile boolean verboseLog;

    private ValetDebug() {
    }

    public static void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> register(dispatcher));
    }

    public static void tick(ServerWorld world) {
        if (VIEWERS.isEmpty() || world.getTime() % ACTIONBAR_INTERVAL_TICKS != 0) {
            return;
        }

        for (ServerPlayerEntity player : world.getPlayers()) {
            if (!VIEWERS.contains(player.getUuid())) {
                continue;
            }

            Optional<VillagerEntity> nearest = nearestValet(player);
            if (nearest.isEmpty()) {
                player.sendMessage(Text.literal("[Valet] aucun valet proche").formatted(Formatting.YELLOW), true);
                continue;
            }

            VillagerEntity villager = nearest.get();
            String event = LAST_EVENTS.getOrDefault(villager.getUuid(), "event=aucun");
            player.sendMessage(Text.literal("[Valet] profession=" + villager.getVillagerData().getProfession()
                    + " " + ValetWorkDriver.describe(villager) + " | " + event), true);
        }
    }

    public static void record(VillagerEntity villager, String message) {
        String line = shortUuid(villager.getUuid()) + " " + shortPos(villager.getBlockPos()) + " " + message;
        LAST_EVENTS.put(villager.getUuid(), line);
        if (verboseLog) {
            ValetMod.LOGGER.info("[valet-debug] {}", line);
        }
    }

    public static void clear(UUID uuid) {
        VIEWERS.remove(uuid);
        LAST_EVENTS.remove(uuid);
    }

    public static void clearAll() {
        VIEWERS.clear();
        LAST_EVENTS.clear();
    }

    public static String shortPos(BlockPos pos) {
        return pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }

    private static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("valetdebug")
                .then(literal("on").executes(context -> {
                    ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                    VIEWERS.add(player.getUuid());
                    context.getSource().sendFeedback(() -> Text.literal("Valet debug ON").formatted(Formatting.GREEN), false);
                    return Command.SINGLE_SUCCESS;
                }))
                .then(literal("off").executes(context -> {
                    ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                    VIEWERS.remove(player.getUuid());
                    context.getSource().sendFeedback(() -> Text.literal("Valet debug OFF").formatted(Formatting.GRAY), false);
                    return Command.SINGLE_SUCCESS;
                }))
                .then(literal("log").executes(context -> {
                    verboseLog = !verboseLog;
                    boolean enabled = verboseLog;
                    context.getSource().sendFeedback(() -> Text.literal("Valet debug log " + (enabled ? "ON" : "OFF")).formatted(enabled ? Formatting.GREEN : Formatting.GRAY), false);
                    return Command.SINGLE_SUCCESS;
                }))
                .then(literal("dump").executes(context -> {
                    ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                    dumpNearby(player);
                    return Command.SINGLE_SUCCESS;
                }))
        );
    }

    private static void dumpNearby(ServerPlayerEntity player) {
        Box searchBox = Box.from(player.getPos()).expand(RANGE);
        var valets = player.getServerWorld().getEntitiesByClass(VillagerEntity.class, searchBox, ValetDebug::isDebugTarget);
        if (valets.isEmpty()) {
            player.sendMessage(Text.literal("[Valet] aucun valet proche").formatted(Formatting.YELLOW), false);
            return;
        }

        for (VillagerEntity villager : valets) {
            String event = LAST_EVENTS.getOrDefault(villager.getUuid(), "event=aucun");
            player.sendMessage(Text.literal("[Valet] profession=" + villager.getVillagerData().getProfession()
                    + " " + ValetWorkDriver.describe(villager) + " | " + event), false);
        }
    }

    private static Optional<VillagerEntity> nearestValet(ServerPlayerEntity player) {
        Box searchBox = Box.from(player.getPos()).expand(RANGE);
        return player.getServerWorld().getEntitiesByClass(VillagerEntity.class, searchBox, ValetDebug::isDebugTarget)
                .stream()
                .min(Comparator.comparingDouble(player::squaredDistanceTo));
    }

    private static boolean isDebugTarget(VillagerEntity villager) {
        return villager.getVillagerData().getProfession() == ValetMod.VALET_PROFESSION || ValetData.hasRuntimeData(villager);
    }

    private static String shortUuid(UUID uuid) {
        return uuid.toString().substring(0, 8);
    }
}
