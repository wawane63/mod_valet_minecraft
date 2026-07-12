package com.wawane.valet;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class ValetHome {
    private static final String HOME_X_KEY = "ValetHomeX";
    private static final String HOME_Y_KEY = "ValetHomeY";
    private static final String HOME_Z_KEY = "ValetHomeZ";
    private static final String HOME_DIMENSION_KEY = "ValetHomeDimension";
    private static final Map<UUID, GlobalPos> HOMES = new ConcurrentHashMap<>();

    private ValetHome() {
    }

    public static BlockPos get(ServerLevel world, Villager villager) {
        GlobalPos storedHome = HOMES.get(villager.getUUID());
        if (storedHome != null && storedHome.dimension().equals(world.dimension())) {
            if (!world.isInWorldBounds(storedHome.pos())) {
                HOMES.remove(villager.getUUID());
            } else if (!isChunkLoaded(world, storedHome.pos()) || isValidWorkstation(world, storedHome.pos())) {
                villager.getBrain().setMemory(MemoryModuleType.JOB_SITE, storedHome);
                return storedHome.pos();
            } else {
                HOMES.remove(villager.getUUID());
            }
        }

        Optional<GlobalPos> jobSite = villager.getBrain().getMemoryInternal(MemoryModuleType.JOB_SITE);
        if (jobSite.isPresent() && jobSite.get().dimension().equals(world.dimension())) {
            BlockPos pos = jobSite.get().pos();
            if (world.isInWorldBounds(pos) && (!isChunkLoaded(world, pos) || isValidWorkstation(world, pos))) {
                set(villager, jobSite.get());
                return pos;
            }
            villager.getBrain().eraseMemory(MemoryModuleType.JOB_SITE);
            HOMES.remove(villager.getUUID());
        }
        return null;
    }

    public static BlockPos getOrRecover(ServerLevel world, Villager villager, BlockPos recoveryOrigin) {
        BlockPos home = get(world, villager);
        if (home != null) {
            return home;
        }
        return ValetMod.claimOrRecoverValetHome(world, villager, recoveryOrigin);
    }

    public static void set(Villager villager, BlockPos pos) {
        set(villager, GlobalPos.of(villager.level().dimension(), pos.immutable()));
    }

    private static void set(Villager villager, GlobalPos pos) {
        HOMES.put(villager.getUUID(), pos);
    }

    private static boolean isValidWorkstation(ServerLevel world, BlockPos pos) {
        return isChunkLoaded(world, pos) && ValetMod.isValetWorkstation(world.getBlockState(pos));
    }

    static boolean isChunkLoaded(ServerLevel world, BlockPos pos) {
        return world.hasChunk(pos.getX() >> 4, pos.getZ() >> 4);
    }

    public static boolean isHome(ServerLevel world, Villager villager, BlockPos pos) {
        GlobalPos home = HOMES.get(villager.getUUID());
        return home != null && home.dimension().equals(world.dimension()) && home.pos().equals(pos);
    }

    public static boolean hasData(Villager villager) {
        return HOMES.containsKey(villager.getUUID());
    }

    public static boolean hasNbt(ValueInput input) {
        return input.getInt(HOME_X_KEY).isPresent()
                || input.getInt(HOME_Y_KEY).isPresent()
                || input.getInt(HOME_Z_KEY).isPresent()
                || input.getString(HOME_DIMENSION_KEY).isPresent();
    }

    public static void clear(UUID uuid) {
        HOMES.remove(uuid);
    }

    public static void clearAll() {
        HOMES.clear();
    }

    public static void writeToNbt(Villager villager, ValueOutput output) {
        GlobalPos home = HOMES.get(villager.getUUID());
        if (home == null) {
            output.discard(HOME_X_KEY);
            output.discard(HOME_Y_KEY);
            output.discard(HOME_Z_KEY);
            output.discard(HOME_DIMENSION_KEY);
            return;
        }

        BlockPos pos = home.pos();
        output.putString(HOME_DIMENSION_KEY, home.dimension().identifier().toString());
        output.putInt(HOME_X_KEY, pos.getX());
        output.putInt(HOME_Y_KEY, pos.getY());
        output.putInt(HOME_Z_KEY, pos.getZ());
    }

    public static void readFromNbt(Villager villager, ValueInput input) {
        if (input.getInt(HOME_X_KEY).isEmpty() || input.getInt(HOME_Y_KEY).isEmpty() || input.getInt(HOME_Z_KEY).isEmpty()) {
            HOMES.remove(villager.getUUID());
            return;
        }

        Identifier dimensionId = parseDimension(input.getString(HOME_DIMENSION_KEY).orElse(null), villager.level().dimension().identifier());
        ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, dimensionId);
        BlockPos pos = new BlockPos(
                input.getIntOr(HOME_X_KEY, 0),
                input.getIntOr(HOME_Y_KEY, 0),
                input.getIntOr(HOME_Z_KEY, 0)
        );
        if (!villager.level().isInWorldBounds(pos)) {
            HOMES.remove(villager.getUUID());
            return;
        }
        set(villager, GlobalPos.of(dimension, pos));
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
}
