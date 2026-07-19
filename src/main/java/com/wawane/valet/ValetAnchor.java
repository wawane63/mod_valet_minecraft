package com.wawane.valet;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Ancre persistante de l'identite Valet, sans bloc, profession ou ticket POI. */
public final class ValetAnchor {
    private static final String X_KEY = "ValetAnchorX";
    private static final String Y_KEY = "ValetAnchorY";
    private static final String Z_KEY = "ValetAnchorZ";
    private static final String DIMENSION_KEY = "ValetAnchorDimension";
    private static final String LEGACY_HOME_X_KEY = "ValetHomeX";
    private static final String LEGACY_HOME_Y_KEY = "ValetHomeY";
    private static final String LEGACY_HOME_Z_KEY = "ValetHomeZ";
    private static final String LEGACY_HOME_DIMENSION_KEY = "ValetHomeDimension";
    private static final Map<UUID, GlobalPos> ANCHORS = new ConcurrentHashMap<>();

    private ValetAnchor() {
    }

    public static BlockPos get(ServerLevel world, Villager villager) {
        GlobalPos anchor = ANCHORS.get(villager.getUUID());
        if (anchor == null || !anchor.dimension().equals(world.dimension()) || !world.isInWorldBounds(anchor.pos())) {
            ANCHORS.remove(villager.getUUID());
            return null;
        }
        return anchor.pos();
    }

    public static BlockPos ensure(ServerLevel world, Villager villager) {
        BlockPos anchor = get(world, villager);
        if (anchor != null) {
            clearJobSite(villager);
            return anchor;
        }
        set(villager, villager.blockPosition());
        clearJobSite(villager);
        return villager.blockPosition();
    }

    public static void set(Villager villager, BlockPos pos) {
        ANCHORS.put(villager.getUUID(), GlobalPos.of(villager.level().dimension(), pos.immutable()));
    }

    public static boolean hasData(Villager villager) {
        return ANCHORS.containsKey(villager.getUUID());
    }

    public static boolean hasNbt(ValueInput input) {
        return hasCompletePosition(input, X_KEY, Y_KEY, Z_KEY)
                || hasCompletePosition(input, LEGACY_HOME_X_KEY, LEGACY_HOME_Y_KEY, LEGACY_HOME_Z_KEY);
    }

    public static void writeToNbt(Villager villager, ValueOutput output) {
        discardLegacyKeys(output);
        GlobalPos anchor = ANCHORS.get(villager.getUUID());
        if (anchor == null) {
            output.discard(X_KEY);
            output.discard(Y_KEY);
            output.discard(Z_KEY);
            output.discard(DIMENSION_KEY);
            return;
        }
        output.putString(DIMENSION_KEY, anchor.dimension().identifier().toString());
        output.putInt(X_KEY, anchor.pos().getX());
        output.putInt(Y_KEY, anchor.pos().getY());
        output.putInt(Z_KEY, anchor.pos().getZ());
    }

    public static void readFromNbt(Villager villager, ValueInput input) {
        PositionKeys keys = positionKeys(input);
        GlobalPos anchor = keys == null ? null : readPosition(villager, input, keys);
        if (anchor == null) {
            anchor = GlobalPos.of(villager.level().dimension(), villager.blockPosition());
        }
        ANCHORS.put(villager.getUUID(), anchor);
        clearJobSite(villager);
    }

    public static void clear(UUID uuid) {
        ANCHORS.remove(uuid);
    }

    public static void clearAll() {
        ANCHORS.clear();
    }

    private static GlobalPos readPosition(Villager villager, ValueInput input, PositionKeys keys) {
        Identifier dimensionId = parseDimension(
                input.getString(keys.dimension()).orElse(null),
                villager.level().dimension().identifier()
        );
        ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, dimensionId);
        BlockPos pos = new BlockPos(
                input.getIntOr(keys.x(), 0),
                input.getIntOr(keys.y(), 0),
                input.getIntOr(keys.z(), 0)
        );
        if (!dimension.equals(villager.level().dimension()) || !villager.level().isInWorldBounds(pos)) {
            return null;
        }
        return GlobalPos.of(dimension, pos);
    }

    private static PositionKeys positionKeys(ValueInput input) {
        if (hasCompletePosition(input, X_KEY, Y_KEY, Z_KEY)) {
            return new PositionKeys(X_KEY, Y_KEY, Z_KEY, DIMENSION_KEY);
        }
        if (hasCompletePosition(input, LEGACY_HOME_X_KEY, LEGACY_HOME_Y_KEY, LEGACY_HOME_Z_KEY)) {
            return new PositionKeys(
                    LEGACY_HOME_X_KEY,
                    LEGACY_HOME_Y_KEY,
                    LEGACY_HOME_Z_KEY,
                    LEGACY_HOME_DIMENSION_KEY
            );
        }
        return null;
    }

    private static void clearJobSite(Villager villager) {
        villager.getBrain().eraseMemory(MemoryModuleType.JOB_SITE);
        villager.getBrain().eraseMemory(MemoryModuleType.POTENTIAL_JOB_SITE);
        villager.getBrain().eraseMemory(MemoryModuleType.SECONDARY_JOB_SITE);
    }

    private static boolean hasCompletePosition(ValueInput input, String x, String y, String z) {
        return input.getInt(x).isPresent() && input.getInt(y).isPresent() && input.getInt(z).isPresent();
    }

    private static void discardLegacyKeys(ValueOutput output) {
        output.discard(LEGACY_HOME_X_KEY);
        output.discard(LEGACY_HOME_Y_KEY);
        output.discard(LEGACY_HOME_Z_KEY);
        output.discard(LEGACY_HOME_DIMENSION_KEY);
    }

    private static Identifier parseDimension(String value, Identifier fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Identifier.parse(value);
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private record PositionKeys(String x, String y, String z, String dimension) {
    }
}
