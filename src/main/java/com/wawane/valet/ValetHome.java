package com.wawane.valet;

import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.world.World;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ValetHome {
    private static final int WORKSTATION_RECOVERY_RADIUS = 24;
    private static final int WORKSTATION_RECOVERY_VERTICAL_RADIUS = 4;
    private static final String HOME_X_KEY = "ValetHomeX";
    private static final String HOME_Y_KEY = "ValetHomeY";
    private static final String HOME_Z_KEY = "ValetHomeZ";
    private static final String HOME_DIMENSION_KEY = "ValetHomeDimension";
    private static final Map<UUID, GlobalPos> HOMES = new ConcurrentHashMap<>();

    private ValetHome() {
    }

    public static BlockPos get(ServerWorld world, VillagerEntity villager) {
        Optional<GlobalPos> jobSite = villager.getBrain().getOptionalMemory(MemoryModuleType.JOB_SITE);
        if (jobSite.isPresent() && jobSite.get().getDimension().equals(world.getRegistryKey())) {
            BlockPos pos = jobSite.get().getPos();
            if (isValidWorkstation(world, pos)) {
                set(villager, jobSite.get());
                return pos;
            }
            villager.getBrain().forget(MemoryModuleType.JOB_SITE);
            HOMES.remove(villager.getUuid());
        }
        GlobalPos home = HOMES.get(villager.getUuid());
        if (home == null || !home.getDimension().equals(world.getRegistryKey())) {
            BlockPos workstation = findNearbyWorkstation(world, villager.getBlockPos());
            if (workstation == null) {
                return null;
            }

            GlobalPos recoveredHome = GlobalPos.create(world.getRegistryKey(), workstation);
            villager.getBrain().remember(MemoryModuleType.JOB_SITE, recoveredHome);
            set(villager, recoveredHome);
            return workstation;
        }
        if (!isValidWorkstation(world, home.getPos())) {
            HOMES.remove(villager.getUuid());
            villager.getBrain().forget(MemoryModuleType.JOB_SITE);
            return getOrRecover(world, villager, villager.getBlockPos());
        }
        villager.getBrain().remember(MemoryModuleType.JOB_SITE, home);
        return home.getPos();
    }

    public static BlockPos getOrRecover(ServerWorld world, VillagerEntity villager, BlockPos recoveryOrigin) {
        BlockPos home = get(world, villager);
        if (home != null) {
            return home;
        }

        BlockPos workstation = findNearbyWorkstation(world, recoveryOrigin);
        if (workstation == null) {
            return null;
        }

        GlobalPos recoveredHome = GlobalPos.create(world.getRegistryKey(), workstation);
        villager.getBrain().remember(MemoryModuleType.JOB_SITE, recoveredHome);
        set(villager, recoveredHome);
        return workstation;
    }

    public static void set(VillagerEntity villager, BlockPos pos) {
        set(villager, GlobalPos.create(villager.getWorld().getRegistryKey(), pos.toImmutable()));
    }

    private static void set(VillagerEntity villager, GlobalPos pos) {
        HOMES.put(villager.getUuid(), pos);
    }

    private static BlockPos findNearbyWorkstation(ServerWorld world, BlockPos origin) {
        for (BlockPos pos : BlockPos.iterateOutwards(origin, WORKSTATION_RECOVERY_RADIUS, WORKSTATION_RECOVERY_VERTICAL_RADIUS, WORKSTATION_RECOVERY_RADIUS)) {
            if (isValidWorkstation(world, pos)) {
                return pos.toImmutable();
            }
        }
        return null;
    }

    private static boolean isValidWorkstation(ServerWorld world, BlockPos pos) {
        return world.getBlockState(pos).isOf(ValetMod.VALET_WORKSTATION);
    }

    public static boolean hasData(VillagerEntity villager) {
        return HOMES.containsKey(villager.getUuid());
    }

    public static boolean hasNbt(NbtCompound nbt) {
        return nbt.contains(HOME_X_KEY) || nbt.contains(HOME_Y_KEY) || nbt.contains(HOME_Z_KEY) || nbt.contains(HOME_DIMENSION_KEY);
    }

    public static void clear(UUID uuid) {
        HOMES.remove(uuid);
    }

    public static void clearAll() {
        HOMES.clear();
    }

    public static void writeToNbt(VillagerEntity villager, NbtCompound nbt) {
        GlobalPos home = HOMES.get(villager.getUuid());
        if (home == null) {
            nbt.remove(HOME_X_KEY);
            nbt.remove(HOME_Y_KEY);
            nbt.remove(HOME_Z_KEY);
            nbt.remove(HOME_DIMENSION_KEY);
            return;
        }

        BlockPos pos = home.getPos();
        nbt.putString(HOME_DIMENSION_KEY, home.getDimension().getValue().toString());
        nbt.putInt(HOME_X_KEY, pos.getX());
        nbt.putInt(HOME_Y_KEY, pos.getY());
        nbt.putInt(HOME_Z_KEY, pos.getZ());
    }

    public static void readFromNbt(VillagerEntity villager, NbtCompound nbt) {
        if (!nbt.contains(HOME_X_KEY) || !nbt.contains(HOME_Y_KEY) || !nbt.contains(HOME_Z_KEY)) {
            HOMES.remove(villager.getUuid());
            return;
        }

        Identifier dimensionId = nbt.contains(HOME_DIMENSION_KEY) ? new Identifier(nbt.getString(HOME_DIMENSION_KEY)) : villager.getWorld().getRegistryKey().getValue();
        RegistryKey<World> dimension = RegistryKey.of(RegistryKeys.WORLD, dimensionId);
        set(villager, GlobalPos.create(dimension, new BlockPos(nbt.getInt(HOME_X_KEY), nbt.getInt(HOME_Y_KEY), nbt.getInt(HOME_Z_KEY))));
    }
}
