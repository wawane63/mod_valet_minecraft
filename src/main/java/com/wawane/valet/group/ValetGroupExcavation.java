package com.wawane.valet.group;

import com.wawane.valet.ValetDebug;
import com.wawane.valet.ai.path.ValetPathPlanner;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/** Reutilise le chemin 3D des artisans pour ouvrir les passages de mission. */
final class ValetGroupExcavation {
    private static final int PASSAGE_HEIGHT = 3;
    private static final int LOCAL_DISTANCE = 8;
    private static final int MAX_VERTICAL_STEP = 4;
    private static final int STUCK_TICKS_BEFORE_EXCAVATION = 40;
    private static final int MAX_PATH_NODES = 2_500;
    private static final int MAX_PATH_LENGTH = 32;
    private static final int ACTION_INTERVAL_TICKS = 4;
    private static final double[] SEARCH_ANGLES = {0.0D, 30.0D, -30.0D, 60.0D, -60.0D, 90.0D, -90.0D};
    private static final Set<Block> NATURAL_PATH_BLOCKS = Set.of(
            Blocks.STONE, Blocks.GRANITE, Blocks.DIORITE, Blocks.ANDESITE,
            Blocks.DEEPSLATE, Blocks.TUFF, Blocks.CALCITE, Blocks.DRIPSTONE_BLOCK,
            Blocks.DIRT, Blocks.GRASS_BLOCK, Blocks.COARSE_DIRT, Blocks.ROOTED_DIRT,
            Blocks.PODZOL, Blocks.MYCELIUM, Blocks.MUD, Blocks.CLAY,
            Blocks.GRAVEL, Blocks.SAND, Blocks.RED_SAND, Blocks.SANDSTONE,
            Blocks.RED_SANDSTONE, Blocks.NETHERRACK, Blocks.BASALT,
            Blocks.BLACKSTONE, Blocks.END_STONE, Blocks.SNOW, Blocks.SNOW_BLOCK,
            Blocks.ICE, Blocks.PACKED_ICE, Blocks.BLUE_ICE
    );
    private static final Set<Block> FALLING_PATH_BLOCKS = Set.of(Blocks.GRAVEL, Blocks.SAND, Blocks.RED_SAND);
    private static final Map<UUID, TravelPath> PATHS = new ConcurrentHashMap<>();
    private static final Map<UUID, Progress> PROGRESS = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> NEXT_ACTION = new ConcurrentHashMap<>();
    private static final ValetPathPlanner PATH_PLANNER = new ValetPathPlanner();

    private ValetGroupExcavation() {
    }

    static boolean observeAndIsStuck(Villager villager) {
        UUID uuid = villager.getUUID();
        BlockPos current = villager.blockPosition();
        Progress previous = PROGRESS.get(uuid);
        if (previous == null || !previous.pos().equals(current)) {
            PROGRESS.put(uuid, new Progress(current.immutable(), 0));
            return false;
        }
        int ticks = Math.min(STUCK_TICKS_BEFORE_EXCAVATION, previous.stillTicks() + 1);
        PROGRESS.put(uuid, new Progress(current.immutable(), ticks));
        return ticks >= STUCK_TICKS_BEFORE_EXCAVATION;
    }

    static boolean hasActivePath(Villager villager) {
        return PATHS.containsKey(villager.getUUID());
    }

    static boolean tick(ServerLevel world, Villager villager, BlockPos destination) {
        long gameTime = world.getGameTime();
        if (gameTime < NEXT_ACTION.getOrDefault(villager.getUUID(), 0L)) {
            return true;
        }
        TravelPath travelPath = PATHS.get(villager.getUUID());
        if (travelPath == null || travelPath.index() >= travelPath.steps().size()) {
            travelPath = planLocalPath(world, villager, destination);
            if (travelPath == null) {
                PATHS.remove(villager.getUUID());
                return false;
            }
            PATHS.put(villager.getUUID(), travelPath);
            ValetDebug.record(villager, "group excavation_start len=" + travelPath.steps().size()
                    + " target=" + ValetDebug.shortPos(travelPath.target()));
        }

        BlockPos current = currentStandPos(world, villager);
        int index = travelPath.index();
        while (index < travelPath.steps().size() && current.equals(travelPath.steps().get(index))) {
            index++;
        }
        if (index >= travelPath.steps().size()) {
            PATHS.remove(villager.getUUID());
            PROGRESS.remove(villager.getUUID());
            return true;
        }

        BlockPos next = travelPath.steps().get(index);
        BlockPos obstruction = findObstruction(world, next);
        if (obstruction != null) {
            BlockState state = world.getBlockState(obstruction);
            if (!canMineNaturalBlock(world, obstruction, state)) {
                ValetDebug.record(villager, "group excavation_blocked pos=" + ValetDebug.shortPos(obstruction)
                        + " block=" + state.getBlock().getDescriptionId());
                PATHS.remove(villager.getUUID());
                return false;
            }
            villager.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_PICKAXE));
            faceTarget(villager, obstruction.getX() + 0.5D, obstruction.getZ() + 0.5D);
            villager.getLookControl().setLookAt(obstruction.getX() + 0.5D, obstruction.getY() + 0.5D, obstruction.getZ() + 0.5D);
            villager.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            if (world.destroyBlock(obstruction, false, villager)) {
                NEXT_ACTION.put(villager.getUUID(), gameTime + ACTION_INTERVAL_TICKS);
                ValetDebug.record(villager, "group excavation_broke pos=" + ValetDebug.shortPos(obstruction));
                return true;
            }
            PATHS.remove(villager.getUUID());
            return false;
        }

        if (!canTraverseStep(world, current, next)) {
            PATHS.remove(villager.getUUID());
            return false;
        }

        villager.getNavigation().stop();
        double targetX = next.getX() + 0.5D;
        double targetZ = next.getZ() + 0.5D;
        faceTarget(villager, targetX, targetZ);
        villager.getLookControl().setLookAt(targetX, next.getY() + 1.0D, targetZ);
        villager.teleportTo(targetX, next.getY(), targetZ);
        villager.setDeltaMovement(0.0D, 0.0D, 0.0D);
        PATHS.put(villager.getUUID(), new TravelPath(travelPath.steps(), index + 1, travelPath.target()));
        PROGRESS.remove(villager.getUUID());
        NEXT_ACTION.put(villager.getUUID(), gameTime + ACTION_INTERVAL_TICKS);
        ValetDebug.record(villager, "group excavation_step pos=" + ValetDebug.shortPos(next));
        return true;
    }

    private static TravelPath planLocalPath(ServerLevel world, Villager villager, BlockPos destination) {
        BlockPos start = currentStandPos(world, villager);
        double dx = destination.getX() + 0.5D - villager.getX();
        double dz = destination.getZ() + 0.5D - villager.getZ();
        double length = Math.sqrt(dx * dx + dz * dz);
        if (length < 1.0D) {
            return null;
        }

        double baseAngle = Math.atan2(dz, dx);
        for (double offsetDegrees : SEARCH_ANGLES) {
            double angle = baseAngle + Math.toRadians(offsetDegrees);
            int targetX = (int) Math.floor(villager.getX() + Math.cos(angle) * Math.min(LOCAL_DISTANCE, length));
            int targetZ = (int) Math.floor(villager.getZ() + Math.sin(angle) * Math.min(LOCAL_DISTANCE, length));
            if (!world.hasChunk(targetX >> 4, targetZ >> 4)) {
                continue;
            }
            int surfaceY = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, targetX, targetZ);
            int targetY = Math.max(start.getY() - MAX_VERTICAL_STEP, Math.min(start.getY() + MAX_VERTICAL_STEP, surfaceY));
            BlockPos target = new BlockPos(targetX, targetY, targetZ);
            if (!canPrepareStand(world, target)) {
                continue;
            }
            List<BlockPos> steps = PATH_PLANNER.planPathToAdjacent(
                    world, start, start, target, Set.of(target), MAX_PATH_NODES, MAX_PATH_LENGTH,
                    ValetGroupExcavation::canPrepareStep, ValetGroupExcavation::movementCost
            );
            if (!steps.isEmpty()) {
                int directLength = Math.abs(start.getX() - target.getX())
                        + Math.abs(start.getY() - target.getY())
                        + Math.abs(start.getZ() - target.getZ());
                if (steps.size() <= directLength + 6 && doesNotBacktrack(steps, start, destination)) {
                    return new TravelPath(steps, 0, target);
                }
            }
        }
        return null;
    }

    private static boolean doesNotBacktrack(List<BlockPos> steps, BlockPos start, BlockPos destination) {
        double maximumDistance = horizontalDistanceSquared(start, destination) + 4.0D;
        for (BlockPos step : steps) {
            if (horizontalDistanceSquared(step, destination) > maximumDistance) {
                return false;
            }
        }
        return true;
    }

    private static double horizontalDistanceSquared(BlockPos first, BlockPos second) {
        int dx = first.getX() - second.getX();
        int dz = first.getZ() - second.getZ();
        return dx * dx + dz * dz;
    }

    private static int movementCost(ServerLevel world, BlockPos from, BlockPos to) {
        int cost = 10 + Math.abs(to.getY() - from.getY()) * 6;
        for (int y = 0; y < PASSAGE_HEIGHT; y++) {
            BlockPos pos = to.above(y);
            if (!isPassable(world, pos)) {
                BlockState state = world.getBlockState(pos);
                cost += 80 + Math.max(0, Math.round(state.getDestroySpeed(world, pos) * 20.0F));
            }
        }
        return cost;
    }

    private static boolean canPrepareStep(ServerLevel world, BlockPos from, BlockPos to) {
        int dx = Math.abs(to.getX() - from.getX());
        int dy = to.getY() - from.getY();
        int dz = Math.abs(to.getZ() - from.getZ());
        return dx + dz == 1 && dy >= -1 && dy <= 1 && canPrepareStand(world, to);
    }

    private static boolean canPrepareStand(ServerLevel world, BlockPos stand) {
        if (!canStandOn(world, stand.below())) {
            return false;
        }
        for (int y = 0; y < PASSAGE_HEIGHT; y++) {
            BlockPos pos = stand.above(y);
            if (!isPassable(world, pos) && !canMineNaturalBlock(world, pos, world.getBlockState(pos))) {
                return false;
            }
        }
        return true;
    }

    private static boolean canTraverseStep(ServerLevel world, BlockPos from, BlockPos to) {
        int dx = Math.abs(to.getX() - from.getX());
        int dy = to.getY() - from.getY();
        int dz = Math.abs(to.getZ() - from.getZ());
        if (dx + dz != 1 || dy < -1 || dy > 1 || !canStandOn(world, to.below())) {
            return false;
        }
        for (int y = 0; y < PASSAGE_HEIGHT; y++) {
            if (!isPassable(world, to.above(y))) {
                return false;
            }
        }
        return true;
    }

    private static BlockPos findObstruction(ServerLevel world, BlockPos stand) {
        for (int y = PASSAGE_HEIGHT - 1; y >= 0; y--) {
            BlockPos pos = stand.above(y);
            if (!isPassable(world, pos)) {
                return pos;
            }
        }
        return null;
    }

    private static void faceTarget(Villager villager, double targetX, double targetZ) {
        double dx = targetX - villager.getX();
        double dz = targetZ - villager.getZ();
        if (dx * dx + dz * dz < 1.0E-6D) {
            return;
        }
        float yaw = (float) (Math.atan2(dz, dx) * 180.0D / Math.PI) - 90.0F;
        villager.setYRot(yaw);
        villager.setYHeadRot(yaw);
        villager.setYBodyRot(yaw);
    }

    private static BlockPos currentStandPos(ServerLevel world, Villager villager) {
        BlockPos current = villager.blockPosition();
        if (canStandOn(world, current) && isPassable(world, current.above())) {
            return current.above();
        }
        return current;
    }

    private static boolean isPassable(ServerLevel world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        return state.isAir() || state.getFluidState().isEmpty() && state.getCollisionShape(world, pos).isEmpty();
    }

    private static boolean canStandOn(ServerLevel world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        return !isPassable(world, pos)
                && state.getFluidState().isEmpty()
                && state.getDestroySpeed(world, pos) >= 0.0F
                && state.isFaceSturdy(world, pos, Direction.UP);
    }

    private static boolean canMineNaturalBlock(ServerLevel world, BlockPos pos, BlockState state) {
        return NATURAL_PATH_BLOCKS.contains(state.getBlock())
                && state.getDestroySpeed(world, pos) >= 0.0F
                && state.getFluidState().isEmpty()
                && !FALLING_PATH_BLOCKS.contains(world.getBlockState(pos.above()).getBlock());
    }

    static void clear(UUID uuid) {
        PATHS.remove(uuid);
        PROGRESS.remove(uuid);
        NEXT_ACTION.remove(uuid);
    }

    static void clearAll() {
        PATHS.clear();
        PROGRESS.clear();
        NEXT_ACTION.clear();
    }

    private record TravelPath(List<BlockPos> steps, int index, BlockPos target) {
    }

    private record Progress(BlockPos pos, int stillTicks) {
    }
}
