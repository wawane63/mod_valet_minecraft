package com.wawane.valet;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.Level;

public final class ValetHome {
    private static final int WORKSTATION_RECOVERY_RADIUS = 24;
    private static final int WORKSTATION_RECOVERY_VERTICAL_RADIUS = 16;
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
            if (isValidWorkstation(world, storedHome.pos())) {
                villager.getBrain().setMemory(MemoryModuleType.JOB_SITE, storedHome);
                return storedHome.pos();
            }
            HOMES.remove(villager.getUUID());
        }

        Optional<GlobalPos> jobSite = villager.getBrain().getMemoryInternal(MemoryModuleType.JOB_SITE);
        if (jobSite.isPresent() && jobSite.get().dimension().equals(world.dimension())) {
            BlockPos pos = jobSite.get().pos();
            if (isValidWorkstation(world, pos)) {
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

    private static BlockPos findNearbyWorkstation(ServerLevel world, BlockPos origin) {
        for (BlockPos pos : BlockPos.withinManhattan(origin, WORKSTATION_RECOVERY_RADIUS, WORKSTATION_RECOVERY_VERTICAL_RADIUS, WORKSTATION_RECOVERY_RADIUS)) {
            if (isValidWorkstation(world, pos)) {
                return pos.immutable();
            }
        }
        return null;
    }

    private static boolean isValidWorkstation(ServerLevel world, BlockPos pos) {
        return ValetMod.isValetWorkstation(world.getBlockState(pos));
    }

    public static boolean isHome(ServerLevel world, Villager villager, BlockPos pos) {
        GlobalPos home = HOMES.get(villager.getUUID());
        return home != null && home.dimension().equals(world.dimension()) && home.pos().equals(pos);
    }

    public static boolean isClaimedHome(ServerLevel world, BlockPos pos, UUID ignored) {
        for (Map.Entry<UUID, GlobalPos> entry : HOMES.entrySet()) {
            if (!entry.getKey().equals(ignored)
                    && entry.getValue().dimension().equals(world.dimension())
                    && entry.getValue().pos().equals(pos)) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasData(Villager villager) {
        return HOMES.containsKey(villager.getUUID());
    }

    public static boolean hasNbt(CompoundTag nbt) {
        return nbt.contains(HOME_X_KEY) || nbt.contains(HOME_Y_KEY) || nbt.contains(HOME_Z_KEY) || nbt.contains(HOME_DIMENSION_KEY);
    }

    public static void clear(UUID uuid) {
        HOMES.remove(uuid);
    }

    public static void clearAll() {
        HOMES.clear();
    }

    public static void writeToNbt(Villager villager, CompoundTag nbt) {
        GlobalPos home = HOMES.get(villager.getUUID());
        if (home == null) {
            nbt.remove(HOME_X_KEY);
            nbt.remove(HOME_Y_KEY);
            nbt.remove(HOME_Z_KEY);
            nbt.remove(HOME_DIMENSION_KEY);
            return;
        }

        BlockPos pos = home.pos();
        nbt.putString(HOME_DIMENSION_KEY, home.dimension().identifier().toString());
        nbt.putInt(HOME_X_KEY, pos.getX());
        nbt.putInt(HOME_Y_KEY, pos.getY());
        nbt.putInt(HOME_Z_KEY, pos.getZ());
    }

    public static void readFromNbt(Villager villager, CompoundTag nbt) {
        if (!nbt.contains(HOME_X_KEY) || !nbt.contains(HOME_Y_KEY) || !nbt.contains(HOME_Z_KEY)) {
            HOMES.remove(villager.getUUID());
            return;
        }

        Identifier dimensionId = nbt.getString(HOME_DIMENSION_KEY)
                .map(Identifier::parse)
                .orElse(villager.level().dimension().identifier());
        ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, dimensionId);
        set(villager, GlobalPos.of(dimension, new BlockPos(
                nbt.getIntOr(HOME_X_KEY, 0),
                nbt.getIntOr(HOME_Y_KEY, 0),
                nbt.getIntOr(HOME_Z_KEY, 0)
        )));
    }
}
