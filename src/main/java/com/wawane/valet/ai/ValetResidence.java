package com.wawane.valet.ai;

import com.wawane.valet.ValetDebug;
import com.wawane.valet.ai.path.ValetSafeNavigation;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

/** Lit HOME explicitement assigne au valet dans son territoire borne. */
public final class ValetResidence {
    private static final String BED_X_KEY = "ValetBedX";
    private static final String BED_Y_KEY = "ValetBedY";
    private static final String BED_Z_KEY = "ValetBedZ";
    private static final String BED_DIMENSION_KEY = "ValetBedDimension";
    private static final String BED_TICKET_OWNED_KEY = "ValetBedTicketOwned";
    private static final int PASSAGE_HEIGHT = 2;
    private static final int MAX_BED_PATH_NODES = 192;
    private static final Map<UUID, GlobalPos> BEDS = new ConcurrentHashMap<>();
    private static final Map<UUID, GlobalPos> OWNED_TICKETS = new ConcurrentHashMap<>();

    private ValetResidence() {
    }

    /** Conserve uniquement un HOME explicitement assigne et dont le ticket est prouve. */
    public static BlockPos ensureAssigned(ServerLevel world, Villager villager) {
        ValetWorkZone.Zone zone = ValetWorkZone.residenceZone(world, villager);
        UUID uuid = villager.getUUID();
        GlobalPos stored = BEDS.get(uuid);
        GlobalPos owned = OWNED_TICKETS.get(uuid);
        Optional<GlobalPos> remembered = villager.getBrain().getMemoryInternal(MemoryModuleType.HOME);

        if (zone == null) {
            clear(world, villager);
            return null;
        }
        if (stored != null && stored.dimension().equals(world.dimension())) {
            world.getPoiManager().ensureLoadedAndValid(world, stored.pos(), 1);
        }

        if (stored != null && (!stored.equals(owned)
                || !isEligibleBed(world, zone, stored)
                || isClaimedByOtherLoadedVillager(world, uuid, stored))) {
            if (stored.equals(owned)) {
                releaseOwned(world, villager, stored);
            }
            BEDS.remove(uuid);
            stored = null;
        }

        if (stored != null
                && stored.equals(OWNED_TICKETS.get(uuid))
                && ensureOwnedTicket(world, villager, zone, stored)) {
            if (remembered.isPresent() && !remembered.get().equals(stored)) {
                villager.getBrain().eraseMemory(MemoryModuleType.HOME);
            }
            villager.getBrain().setMemory(MemoryModuleType.HOME, stored);
            return stored.pos();
        }

        GlobalPos orphaned = OWNED_TICKETS.get(uuid);
        if (orphaned != null) {
            releaseOwned(world, villager, orphaned);
        }
        if (remembered.isPresent()) {
            villager.getBrain().eraseMemory(MemoryModuleType.HOME);
        }
        villager.getBrain().eraseMemory(MemoryModuleType.HOME);
        BEDS.remove(uuid);
        return null;
    }

    /** Assigne le lit choisi par le joueur apres validation POI, zone et trajet vanilla. */
    public static boolean assign(ServerLevel world, Villager villager, BlockPos clickedPos) {
        BlockPos bedPos = resolveBedPoi(world, clickedPos);
        ValetWorkZone.Zone zone = ValetWorkZone.residenceZone(world, villager);
        if (bedPos == null || zone == null) {
            ValetDebug.record(villager, "brain home_rejected reason=" + (bedPos == null ? "invalid_bed" : "no_zone"));
            return false;
        }
        world.getPoiManager().ensureLoadedAndValid(world, bedPos, 1);
        GlobalPos bed = GlobalPos.of(world.dimension(), bedPos);
        String rejection = bedRejectionReason(world, zone, bed);
        if (rejection != null) {
            ValetDebug.record(villager, "brain home_rejected reason=" + rejection
                    + " pos=" + ValetDebug.shortPos(bedPos));
            return false;
        }
        if (isClaimedByOtherLoadedVillager(world, villager.getUUID(), bed)) {
            ValetDebug.record(villager, "brain home_rejected reason=claimed pos=" + ValetDebug.shortPos(bedPos));
            return false;
        }
        if (!canReachBed(world, villager, zone, bedPos)) {
            ValetDebug.record(villager, "brain home_rejected reason=no_path pos=" + ValetDebug.shortPos(bedPos));
            return false;
        }

        UUID uuid = villager.getUUID();
        GlobalPos tracked = OWNED_TICKETS.get(uuid);
        if (bed.equals(tracked)
                && bed.equals(BEDS.get(uuid))
                && ensureOwnedTicket(world, villager, zone, bed)) {
            villager.getBrain().setMemory(MemoryModuleType.HOME, bed);
            return true;
        }

        Optional<GlobalPos> remembered = villager.getBrain().getMemoryInternal(MemoryModuleType.HOME);
        if (remembered.isPresent() && !remembered.get().equals(tracked)) {
            villager.getBrain().eraseMemory(MemoryModuleType.HOME);
        }
        if (takeExact(world, zone, bedPos).isEmpty()) {
            ValetDebug.record(villager, "brain home_rejected reason=poi_unavailable pos=" + ValetDebug.shortPos(bedPos));
            return false;
        }

        if (tracked != null && !tracked.equals(bed)) {
            releaseOwned(world, villager, tracked);
        }
        BEDS.put(uuid, bed);
        OWNED_TICKETS.put(uuid, bed);
        villager.getBrain().setMemory(MemoryModuleType.HOME, bed);
        ValetDebug.record(villager, "brain home_assigned mode=explicit pos=" + ValetDebug.shortPos(bedPos));
        return true;
    }

    public static BlockPos resolveBedPoi(ServerLevel world, BlockPos clickedPos) {
        if (isBedPoi(world, clickedPos)) {
            return clickedPos.immutable();
        }
        BlockState state = world.getBlockState(clickedPos);
        if (!state.is(BlockTags.BEDS)
                || !state.hasProperty(BedBlock.FACING)
                || !state.hasProperty(BedBlock.PART)) {
            return null;
        }
        Direction direction = state.getValue(BedBlock.FACING);
        BlockPos otherHalf = clickedPos.relative(
                state.getValue(BedBlock.PART) == BedPart.FOOT ? direction : direction.getOpposite()
        );
        return isBedPoi(world, otherHalf) ? otherHalf.immutable() : null;
    }

    public static BlockPos get(ServerLevel world, Villager villager) {
        GlobalPos bed = BEDS.get(villager.getUUID());
        if (bed == null || !bed.dimension().equals(world.dimension()) || !world.isInWorldBounds(bed.pos())) {
            return null;
        }
        return bed.pos();
    }

    public static boolean hasData(Villager villager) {
        return BEDS.containsKey(villager.getUUID());
    }

    public static boolean hasNbt(ValueInput input) {
        return input.getInt(BED_X_KEY).isPresent()
                || input.getInt(BED_Y_KEY).isPresent()
                || input.getInt(BED_Z_KEY).isPresent()
                || input.getString(BED_DIMENSION_KEY).isPresent()
                || input.getInt(BED_TICKET_OWNED_KEY).isPresent();
    }

    public static void clear(ServerLevel world, Villager villager) {
        UUID uuid = villager.getUUID();
        GlobalPos owned = OWNED_TICKETS.get(uuid);
        if (owned != null) {
            releaseOwned(world, villager, owned);
        }
        BEDS.remove(uuid);
        OWNED_TICKETS.remove(uuid);
        villager.getBrain().eraseMemory(MemoryModuleType.HOME);
    }

    public static void clear(UUID uuid) {
        BEDS.remove(uuid);
        OWNED_TICKETS.remove(uuid);
    }

    public static void clearAll() {
        BEDS.clear();
        OWNED_TICKETS.clear();
    }

    public static void writeToNbt(Villager villager, ValueOutput output) {
        GlobalPos bed = BEDS.get(villager.getUUID());
        if (bed == null) {
            output.discard(BED_X_KEY);
            output.discard(BED_Y_KEY);
            output.discard(BED_Z_KEY);
            output.discard(BED_DIMENSION_KEY);
            output.discard(BED_TICKET_OWNED_KEY);
            return;
        }
        output.putString(BED_DIMENSION_KEY, bed.dimension().identifier().toString());
        output.putInt(BED_X_KEY, bed.pos().getX());
        output.putInt(BED_Y_KEY, bed.pos().getY());
        output.putInt(BED_Z_KEY, bed.pos().getZ());
        output.putInt(BED_TICKET_OWNED_KEY, bed.equals(OWNED_TICKETS.get(villager.getUUID())) ? 1 : 0);
    }

    public static void readFromNbt(Villager villager, ValueInput input) {
        UUID uuid = villager.getUUID();
        if (input.getInt(BED_X_KEY).isEmpty()
                || input.getInt(BED_Y_KEY).isEmpty()
                || input.getInt(BED_Z_KEY).isEmpty()) {
            BEDS.remove(uuid);
            OWNED_TICKETS.remove(uuid);
            return;
        }
        Identifier fallback = villager.level().dimension().identifier();
        Identifier dimensionId = parseDimension(input.getString(BED_DIMENSION_KEY).orElse(null), fallback);
        ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, dimensionId);
        BlockPos pos = new BlockPos(
                input.getIntOr(BED_X_KEY, 0),
                input.getIntOr(BED_Y_KEY, 0),
                input.getIntOr(BED_Z_KEY, 0)
        );
        if (!dimension.equals(villager.level().dimension())) {
            if (input.getIntOr(BED_TICKET_OWNED_KEY, 0) == 1) {
                releaseTransferredTicket(villager, dimension, pos);
            }
            BEDS.remove(uuid);
            OWNED_TICKETS.remove(uuid);
            return;
        }
        if (!villager.level().isInWorldBounds(pos)) {
            BEDS.remove(uuid);
            OWNED_TICKETS.remove(uuid);
            return;
        }
        GlobalPos bed = GlobalPos.of(dimension, pos);
        BEDS.put(uuid, bed);
        if (input.getIntOr(BED_TICKET_OWNED_KEY, 0) == 1) {
            OWNED_TICKETS.put(uuid, bed);
        } else {
            OWNED_TICKETS.remove(uuid);
        }
    }

    private static void releaseTransferredTicket(Villager villager, ResourceKey<Level> dimension, BlockPos pos) {
        if (!(villager.level() instanceof ServerLevel destination)) {
            return;
        }
        ServerLevel previous = destination.getServer().getLevel(dimension);
        if (previous != null
                && previous.isInWorldBounds(pos)
                && previous.getPoiManager().existsAtPosition(PoiTypes.HOME, pos)) {
            previous.getPoiManager().release(pos);
        }
    }

    private static boolean canReachBed(
            ServerLevel world,
            Villager villager,
            ValetWorkZone.Zone zone,
            BlockPos bedPos
    ) {
        for (BlockPos candidate : BlockPos.withinManhattan(bedPos, 2, 1, 2)) {
            BlockPos stand = candidate.immutable();
            if (!zone.contains(stand) || !ValetSafeNavigation.isSafeStand(world, stand, PASSAGE_HEIGHT)) {
                continue;
            }
            Path path = ValetSafeNavigation.createSafeLocalPath(
                    world,
                    villager,
                    stand,
                    PASSAGE_HEIGHT,
                    false,
                    MAX_BED_PATH_NODES
            );
            if (path != null && pathReentersAndStaysInZone(villager, path, zone)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isEligibleBed(ServerLevel world, ValetWorkZone.Zone zone, GlobalPos bed) {
        return bedRejectionReason(world, zone, bed) == null;
    }

    private static String bedRejectionReason(ServerLevel world, ValetWorkZone.Zone zone, GlobalPos bed) {
        if (!bed.dimension().equals(world.dimension())) {
            return "wrong_dimension";
        }
        if (!world.isInWorldBounds(bed.pos())) {
            return "out_of_world";
        }
        if (!zone.contains(bed.pos())) {
            return "outside_residence_radius";
        }
        if (isPlayerRespawnBed(world, bed.pos())) {
            return "player_respawn";
        }
        return isBedPoi(world, bed.pos()) ? null : "missing_poi";
    }

    private static boolean isBedPoi(ServerLevel world, BlockPos pos) {
        return world.getPoiManager().existsAtPosition(PoiTypes.HOME, pos)
                && world.getBlockState(pos).is(BlockTags.BEDS);
    }

    private static boolean isAvailableBed(ServerLevel world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        return state.is(BlockTags.BEDS)
                && state.hasProperty(BedBlock.OCCUPIED)
                && !state.getValue(BedBlock.OCCUPIED);
    }

    private static boolean isPlayerRespawnBed(ServerLevel world, BlockPos pos) {
        for (net.minecraft.server.level.ServerPlayer player : world.getServer().getPlayerList().getPlayers()) {
            net.minecraft.server.level.ServerPlayer.RespawnConfig respawn = player.getRespawnConfig();
            if (respawn != null
                    && respawn.respawnData().dimension().equals(world.dimension())
                    && respawn.respawnData().pos().distManhattan(pos) <= 1
                    && world.getBlockState(respawn.respawnData().pos()).is(BlockTags.BEDS)) {
                return true;
            }
        }
        return false;
    }

    private static Optional<BlockPos> takeExact(ServerLevel world, ValetWorkZone.Zone zone, BlockPos target) {
        if (!isEligibleBed(world, zone, GlobalPos.of(world.dimension(), target))) {
            return Optional.empty();
        }
        return world.getPoiManager().take(
                holder -> holder.is(PoiTypes.HOME),
                (holder, pos) -> pos.equals(target) && isAvailableBed(world, pos),
                target,
                1
        );
    }

    private static boolean ensureOwnedTicket(
            ServerLevel world,
            Villager villager,
            ValetWorkZone.Zone zone,
            GlobalPos bed
    ) {
        UUID uuid = villager.getUUID();
        if (!bed.equals(OWNED_TICKETS.get(uuid))) {
            return false;
        }
        if (isExactPoiOccupied(world, bed.pos())) {
            return true;
        }
        OWNED_TICKETS.remove(uuid);
        if (takeExact(world, zone, bed.pos()).isEmpty()) {
            return false;
        }
        OWNED_TICKETS.put(uuid, bed);
        return true;
    }

    private static boolean isExactPoiOccupied(ServerLevel world, BlockPos pos) {
        return world.getPoiManager().getInRange(
                holder -> holder.is(PoiTypes.HOME),
                pos,
                1,
                PoiManager.Occupancy.IS_OCCUPIED
        ).anyMatch(record -> record.getPos().equals(pos));
    }

    private static boolean pathReentersAndStaysInZone(Villager villager, Path path, ValetWorkZone.Zone zone) {
        boolean entered = zone.contains(villager.blockPosition());
        for (int index = 0; index < path.getNodeCount(); index++) {
            boolean inside = zone.contains(path.getNodePos(index));
            if (entered && !inside) {
                return false;
            }
            entered |= inside;
        }
        return entered;
    }

    private static void releaseOwned(ServerLevel context, Villager villager, GlobalPos bed) {
        UUID uuid = villager.getUUID();
        if (!bed.equals(OWNED_TICKETS.get(uuid))) {
            return;
        }
        ServerLevel level = context.getServer().getLevel(bed.dimension());
        if (level != null
                && level.isInWorldBounds(bed.pos())
                && level.getPoiManager().existsAtPosition(PoiTypes.HOME, bed.pos())
                && !isClaimedByOtherLoadedVillager(level, uuid, bed)) {
            level.getPoiManager().release(bed.pos());
        }
        OWNED_TICKETS.remove(uuid);
    }

    private static boolean isClaimedByOtherLoadedVillager(ServerLevel world, UUID owner, GlobalPos bed) {
        for (Map.Entry<UUID, GlobalPos> entry : BEDS.entrySet()) {
            if (!entry.getKey().equals(owner) && entry.getValue().equals(bed)) {
                return true;
            }
        }
        AABB search = new AABB(bed.pos()).inflate(64.0D);
        return !world.getEntitiesOfClass(Villager.class, search, candidate ->
                !candidate.getUUID().equals(owner)
                        && candidate.getBrain().getMemoryInternal(MemoryModuleType.HOME)
                        .filter(home -> home.equals(bed))
                        .isPresent()
        ).isEmpty();
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
