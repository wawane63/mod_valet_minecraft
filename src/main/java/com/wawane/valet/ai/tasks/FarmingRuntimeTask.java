package com.wawane.valet.ai.tasks;

import com.wawane.valet.ValetDebug;
import com.wawane.valet.ai.ValetStateMachine.PathPurpose;
import com.wawane.valet.ai.ValetStateMachine.State;
import com.wawane.valet.farm.ValetFarmArea;
import com.wawane.valet.order.ValetFarmCrop;
import com.wawane.valet.order.ValetOrders;
import com.wawane.valet.progress.ValetProgress;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class FarmingRuntimeTask {
    private static final int FAILED_TARGET_MEMORY_TICKS = 300;
    private static final int FARM_RESERVATION_TICKS = 600;
    private static final int MAX_PATH_FAILURES_BEFORE_BACKOFF = 6;

    private final Control control;
    private final Map<BlockPos, Integer> failedTargets = new HashMap<>();
    private BlockPos targetPos;
    private BlockPos reservedTargetPos;
    private BlockState targetState;
    private TargetType targetType;
    private int pathFailures;

    public FarmingRuntimeTask(Control control) {
        this.control = control;
    }

    public void tickCooldown() {
        Iterator<Map.Entry<BlockPos, Integer>> iterator = failedTargets.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, Integer> entry = iterator.next();
            int ticks = entry.getValue() - 1;
            if (ticks <= 0) {
                iterator.remove();
            } else {
                entry.setValue(ticks);
            }
        }
    }

    public void findTarget(ServerLevel world) {
        BlockPos workOrigin = control.getWorkOrigin(world);
        if (workOrigin == null) {
            ValetDebug.record(control.villager(), "farm no_work_origin");
            control.setDelayTicks(40);
            return;
        }

        int cropMask = ValetOrders.getFarmCropMask(control.villager());
        boolean canHarvest = cropMask != 0 && control.hasInventorySpace();
        boolean canTill = ValetOrders.shouldTillFarm(control.villager());
        if (!canHarvest && !canTill) {
            releaseReservedTarget();
            ValetDebug.record(control.villager(), "farm inventory_full -> RETURNING");
            control.setState(control.hasInventoryItems() ? State.RETURNING : State.RETURNING_HOME);
            return;
        }

        WorkTarget target = findNearestTarget(world, workOrigin, canHarvest, canTill, cropMask);
        if (target == null) {
            releaseReservedTarget();
            ValetDebug.record(control.villager(), "farm no_target");
            control.setState(control.hasInventoryItems() ? State.RETURNING : State.RETURNING_HOME);
            control.setDelayTicks(control.noTargetDelayTicks());
            return;
        }

        targetPos = target.pos();
        targetType = target.type();
        if (!claimTarget(world, targetPos)) {
            return;
        }

        Set<BlockPos> goals = control.findStandGoals(world, targetPos, PathPurpose.CROP);
        if (goals.contains(control.villager().blockPosition())) {
            beginHarvesting(world, targetPos);
            return;
        }

        List<BlockPos> path = control.planPathToAdjacent(world, PathPurpose.CROP, targetPos, goals);
        if (path.isEmpty()) {
            pathFailures++;
            rememberFailedTarget(targetPos);
            ValetDebug.record(control.villager(), "farm no_path target=" + ValetDebug.shortPos(targetPos) + " type=" + targetType);
            releaseReservedTarget();
            targetPos = null;
            targetType = null;
            if (pathFailures >= MAX_PATH_FAILURES_BEFORE_BACKOFF) {
                pathFailures = 0;
                control.setState(control.hasInventoryItems() ? State.RETURNING : State.RETURNING_HOME);
                control.setDelayTicks(control.noTargetDelayTicks());
                return;
            }
            control.setState(control.interruptedWorkState());
            control.setDelayTicks(20);
            return;
        }

        pathFailures = 0;
        control.startPath(PathPurpose.CROP, path);
    }

    public void completePath(ServerLevel world) {
        if (isValidTarget(world, targetPos, targetType)) {
            beginHarvesting(world, targetPos);
        } else {
            rememberFailedTarget(targetPos);
            releaseReservedTarget();
            targetPos = null;
            targetType = null;
            control.setState(control.interruptedWorkState());
        }
    }

    public void beginHarvesting(ServerLevel world, BlockPos pos) {
        if (!isValidTarget(world, pos, targetType)) {
            ValetDebug.record(control.villager(), "farm target_changed target=" + ValetDebug.shortPos(pos));
            rememberFailedTarget(pos);
            releaseReservedTarget();
            targetPos = null;
            targetType = null;
            control.setState(control.interruptedWorkState());
            return;
        }

        if (!control.canReachTargetFromStand(pos, control.villager().blockPosition())) {
            ValetDebug.record(control.villager(), "farm unreachable target=" + ValetDebug.shortPos(pos));
            rememberFailedTarget(pos);
            releaseReservedTarget();
            targetPos = null;
            targetType = null;
            control.setState(control.interruptedWorkState());
            return;
        }

        if (!claimTarget(world, pos)) {
            return;
        }

        targetState = world.getBlockState(pos);
        control.villager().getNavigation().stop();
        control.setState(State.HARVESTING);
        ValetDebug.record(control.villager(), "farm begin target=" + ValetDebug.shortPos(pos) + " type=" + targetType + " block=" + targetState.getBlock().getDescriptionId());
    }

    public void tickHarvesting(ServerLevel world) {
        if (targetType == TargetType.SOIL) {
            tickTilling(world);
            return;
        }
        tickCropHarvesting(world);
    }

    public void clearTarget() {
        targetPos = null;
        targetType = null;
        releaseReservedTarget();
    }

    public void clearHarvestState() {
        targetPos = null;
        targetState = null;
        targetType = null;
        releaseReservedTarget();
    }

    public void clearAll() {
        clearTarget();
        clearHarvestState();
        failedTargets.clear();
        pathFailures = 0;
    }

    public String debugSummary() {
        return "farmTarget=" + shortPos(targetPos)
                + " farmType=" + (targetType == null ? "-" : targetType)
                + " failed=" + failedTargets.size();
    }

    private void tickCropHarvesting(ServerLevel world) {
        if (targetPos == null || targetState == null) {
            ValetDebug.record(control.villager(), "farm lost_state");
            control.setState(control.interruptedWorkState());
            clearHarvestState();
            return;
        }

        BlockState currentState = world.getBlockState(targetPos);
        if (!isSameCrop(currentState, targetState) || !isMatureCrop(currentState)) {
            ValetDebug.record(control.villager(), "farm target_changed target=" + ValetDebug.shortPos(targetPos));
            control.setState(control.interruptedWorkState());
            clearHarvestState();
            return;
        }

        if (!claimTarget(world, targetPos)) {
            return;
        }

        List<ItemStack> drops = Block.getDrops(currentState, world, targetPos, world.getBlockEntity(targetPos), control.villager(), ItemStack.EMPTY);
        if (!control.canStoreAllDrops(drops)) {
            ValetDebug.record(control.villager(), "farm drops_no_space target=" + ValetDebug.shortPos(targetPos) + " drops=" + drops.size());
            control.setState(State.RETURNING);
            clearHarvestState();
            control.setDelayTicks(1);
            return;
        }

        control.villager().getLookControl().setLookAt(targetPos.getX() + 0.5D, targetPos.getY() + 0.5D, targetPos.getZ() + 0.5D);
        control.villager().swing(InteractionHand.MAIN_HAND);
        clearSnowAbove(world, targetPos);
        world.levelEvent(2001, targetPos, Block.getId(currentState));
        control.collectDrops(drops);
        world.destroyBlock(targetPos, false, control.villager());

        boolean replanted = tryReplant(world, targetPos, currentState);
        ValetProgress.addXp(control.villager(), replanted ? 5 : 4);
        ValetDebug.record(control.villager(), "farm harvested target=" + ValetDebug.shortPos(targetPos) + " replanted=" + replanted);

        control.setState(afterFarmActionState());
        clearHarvestState();
        control.setDelayTicks(control.actionDelayTicks());
    }

    private void tickTilling(ServerLevel world) {
        if (targetPos == null || !isTillableSoil(world, targetPos)) {
            ValetDebug.record(control.villager(), "farm soil_changed target=" + ValetDebug.shortPos(targetPos));
            rememberFailedTarget(targetPos);
            control.setState(control.interruptedWorkState());
            clearHarvestState();
            return;
        }

        if (!claimTarget(world, targetPos)) {
            return;
        }

        control.villager().getLookControl().setLookAt(targetPos.getX() + 0.5D, targetPos.getY() + 0.5D, targetPos.getZ() + 0.5D);
        control.villager().swing(InteractionHand.MAIN_HAND);
        clearSnowAbove(world, targetPos);
        world.playSound(null, targetPos, SoundEvents.HOE_TILL, SoundSource.BLOCKS, 0.8F, 1.0F);
        world.setBlock(targetPos, Blocks.FARMLAND.defaultBlockState(), Block.UPDATE_ALL);
        boolean tilled = world.getBlockState(targetPos).is(Blocks.FARMLAND);
        rememberFailedTarget(targetPos);

        if (tilled) {
            ValetProgress.addXp(control.villager(), 2);
        }
        ValetDebug.record(control.villager(), "farm tilled target=" + ValetDebug.shortPos(targetPos) + " success=" + tilled);
        control.setState(afterFarmActionState());
        clearHarvestState();
        control.setDelayTicks(control.actionDelayTicks());
    }

    private State afterFarmActionState() {
        if (control.hasFarmOrder() && (control.hasInventorySpace() || ValetOrders.shouldTillFarm(control.villager()))) {
            return State.FIND_TARGET;
        }
        return control.hasInventoryItems() ? State.RETURNING : State.RETURNING_HOME;
    }

    private WorkTarget findNearestTarget(ServerLevel world, BlockPos workOrigin, boolean includeCrops, boolean includeSoil, int cropMask) {
        ValetFarmArea area = control.getFarmArea(world, ValetOrders.getFarmAreaId(control.villager()));
        if (includeCrops) {
            BlockPos crop = findNearestCrop(world, workOrigin, area, cropMask);
            if (crop != null) {
                return new WorkTarget(crop, TargetType.CROP);
            }
        }
        if (includeSoil) {
            BlockPos soil = findNearestSoil(world, workOrigin, area);
            if (soil != null) {
                return new WorkTarget(soil, TargetType.SOIL);
            }
        }
        return null;
    }

    private BlockPos findNearestCrop(ServerLevel world, BlockPos workOrigin, ValetFarmArea area, int cropMask) {
        BlockPos nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (BlockPos pos : candidates(workOrigin, area)) {
            BlockPos candidate = pos.immutable();
            BlockPos cropPos = resolveCropPos(world, candidate, cropMask);
            if (cropPos == null
                    || isFailedTarget(cropPos)
                    || control.isBlockReservedByOther(world, cropPos)
                    || area != null && !area.contains(candidate) && !area.contains(cropPos)) {
                continue;
            }

            double distance = squaredDistance(control.villager().blockPosition(), cropPos);
            if (distance < nearestDistance) {
                nearest = cropPos;
                nearestDistance = distance;
            }
        }

        return nearest;
    }

    private BlockPos findNearestSoil(ServerLevel world, BlockPos workOrigin, ValetFarmArea area) {
        BlockPos nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (BlockPos pos : candidates(workOrigin, area)) {
            BlockPos candidate = pos.immutable();
            BlockPos soilPos = area == null ? candidate : candidate.below();
            if (isFailedTarget(soilPos)
                    || control.isBlockReservedByOther(world, soilPos)
                    || area != null && !area.contains(candidate)
                    || !isTillableSoil(world, soilPos)) {
                continue;
            }

            double distance = squaredDistance(control.villager().blockPosition(), soilPos);
            if (distance < nearestDistance) {
                nearest = soilPos;
                nearestDistance = distance;
            }
        }

        return nearest;
    }

    private Iterable<BlockPos> candidates(BlockPos workOrigin, ValetFarmArea area) {
        if (area == null) {
            return BlockPos.withinManhattan(workOrigin, control.farmRadius(), control.farmVerticalRadius(), control.farmRadius());
        }
        return BlockPos.betweenClosed(area.minX(), area.minY(), area.minZ(), area.maxX(), area.maxY(), area.maxZ());
    }

    private boolean tryReplant(ServerLevel world, BlockPos pos, BlockState harvestedState) {
        if (!ValetOrders.shouldReplantFarm(control.villager())) {
            return false;
        }

        Item replantItem = getReplantItem(harvestedState);
        BlockState replantState = getReplantState(harvestedState);
        clearSnowAbove(world, pos);
        if (replantItem == null || replantState == null || !replantState.canSurvive(world, pos)) {
            return false;
        }

        if (!control.takeOneItem(replantItem)) {
            return false;
        }

        world.setBlock(pos, replantState, Block.UPDATE_ALL);
        return true;
    }

    private boolean isValidTarget(ServerLevel world, BlockPos pos, TargetType type) {
        if (type == TargetType.CROP) {
            return isHarvestableCrop(world, pos, ValetOrders.getFarmCropMask(control.villager()));
        }
        if (type == TargetType.SOIL) {
            return isTillableSoil(world, pos);
        }
        return false;
    }

    private static boolean isHarvestableCrop(ServerLevel world, BlockPos pos, int cropMask) {
        return pos != null
                && cropMask != 0
                && ValetFarmCrop.matchesAnyEnabled(world.getBlockState(pos), cropMask)
                && isMatureCrop(world.getBlockState(pos));
    }

    private static BlockPos resolveCropPos(ServerLevel world, BlockPos pos, int cropMask) {
        if (isHarvestableCrop(world, pos, cropMask)) {
            return pos;
        }
        if (pos != null && world.getBlockState(pos).is(Blocks.SNOW) && isHarvestableCrop(world, pos.below(), cropMask)) {
            return pos.below();
        }
        return null;
    }

    private static boolean isMatureCrop(BlockState state) {
        if (state.getBlock() instanceof CropBlock crop) {
            return crop.isMaxAge(state);
        }
        return state.is(Blocks.NETHER_WART) && state.getValue(NetherWartBlock.AGE) >= NetherWartBlock.MAX_AGE;
    }

    private static boolean isSameCrop(BlockState currentState, BlockState expectedState) {
        return currentState.is(expectedState.getBlock());
    }

    private static boolean isTillableSoil(ServerLevel world, BlockPos pos) {
        if (pos == null || !isClearForFarmUse(world.getBlockState(pos.above()))) {
            return false;
        }

        BlockState state = world.getBlockState(pos);
        return state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.DIRT)
                || state.is(Blocks.DIRT_PATH)
                || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.PODZOL)
                || state.is(Blocks.MYCELIUM);
    }

    private static boolean isClearForFarmUse(BlockState state) {
        return state.isAir() || state.is(Blocks.SNOW);
    }

    private static void clearSnowAbove(ServerLevel world, BlockPos pos) {
        BlockPos above = pos.above();
        if (world.getBlockState(above).is(Blocks.SNOW)) {
            world.setBlock(above, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    private static Item getReplantItem(BlockState state) {
        if (state.is(Blocks.WHEAT)) {
            return Items.WHEAT_SEEDS;
        }
        if (state.is(Blocks.CARROTS)) {
            return Items.CARROT;
        }
        if (state.is(Blocks.POTATOES)) {
            return Items.POTATO;
        }
        if (state.is(Blocks.BEETROOTS)) {
            return Items.BEETROOT_SEEDS;
        }
        if (state.is(Blocks.NETHER_WART)) {
            return Items.NETHER_WART;
        }
        return null;
    }

    private static BlockState getReplantState(BlockState harvestedState) {
        if (harvestedState.getBlock() instanceof CropBlock crop) {
            return crop.getStateForAge(0);
        }
        if (harvestedState.is(Blocks.NETHER_WART)) {
            return harvestedState.setValue(NetherWartBlock.AGE, 0);
        }
        return null;
    }

    private boolean claimTarget(ServerLevel world, BlockPos pos) {
        BlockPos immutable = pos.immutable();
        if (control.claimBlock(world, immutable, FARM_RESERVATION_TICKS)) {
            if (reservedTargetPos != null && !reservedTargetPos.equals(immutable)) {
                releaseReservedTarget();
            }
            reservedTargetPos = immutable;
            return true;
        }

        ValetDebug.record(control.villager(), "farm reserved target=" + ValetDebug.shortPos(immutable));
        releaseReservedTarget();
        rememberFailedTarget(immutable);
        targetPos = null;
        targetType = null;
        control.setState(control.interruptedWorkState());
        control.setDelayTicks(8);
        return false;
    }

    private void releaseReservedTarget() {
        if (reservedTargetPos != null) {
            control.releaseBlock(reservedTargetPos);
            reservedTargetPos = null;
        }
    }

    private void rememberFailedTarget(BlockPos pos) {
        if (pos != null) {
            failedTargets.put(pos.immutable(), FAILED_TARGET_MEMORY_TICKS);
        }
    }

    private boolean isFailedTarget(BlockPos pos) {
        return failedTargets.containsKey(pos);
    }

    private static double squaredDistance(BlockPos first, BlockPos second) {
        int dx = first.getX() - second.getX();
        int dy = first.getY() - second.getY();
        int dz = first.getZ() - second.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    private static String shortPos(BlockPos pos) {
        return pos == null ? "-" : ValetDebug.shortPos(pos);
    }

    private enum TargetType {
        CROP,
        SOIL
    }

    private record WorkTarget(BlockPos pos, TargetType type) {
    }

    public interface Control {
        Villager villager();

        BlockPos getWorkOrigin(ServerLevel world);

        ValetFarmArea getFarmArea(ServerLevel world, int areaId);

        boolean hasFarmOrder();

        boolean hasInventorySpace();

        boolean hasInventoryItems();

        Set<BlockPos> findStandGoals(ServerLevel world, BlockPos targetBlock, PathPurpose purpose);

        List<BlockPos> planPathToAdjacent(ServerLevel world, PathPurpose purpose, BlockPos targetBlock, Set<BlockPos> goals);

        void startPath(PathPurpose purpose, List<BlockPos> path);

        boolean canReachTargetFromStand(BlockPos targetBlock, BlockPos stand);

        boolean canStoreAllDrops(List<ItemStack> drops);

        void collectDrops(List<ItemStack> drops);

        boolean claimBlock(ServerLevel world, BlockPos pos, int ttlTicks);

        boolean isBlockReservedByOther(ServerLevel world, BlockPos pos);

        void releaseBlock(BlockPos pos);

        boolean takeOneItem(Item item);

        State interruptedWorkState();

        void setState(State state);

        void setDelayTicks(int ticks);

        int noTargetDelayTicks();

        int farmRadius();

        int farmVerticalRadius();

        int actionDelayTicks();
    }
}
