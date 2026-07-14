package com.wawane.valet.ai.tasks;

import com.wawane.valet.ValetDebug;
import com.wawane.valet.ai.ValetStateMachine.PathPurpose;
import com.wawane.valet.ai.ValetStateMachine.State;
import com.wawane.valet.farm.ValetFarmArea;
import com.wawane.valet.order.ValetFarmCrop;
import com.wawane.valet.order.ValetOrders;
import com.wawane.valet.progress.ValetPerk;
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
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public final class FarmingRuntimeTask {
    private static final int FAILED_TARGET_MEMORY_TICKS = 300;
    private static final int FARM_RESERVATION_TICKS = 600;
    private static final int MAX_PATH_FAILURES_BEFORE_BACKOFF = 6;
    private static final int PLANTING_REQUEST_RETRY_TICKS = 200;

    private final Control control;
    private final Map<BlockPos, Integer> failedTargets = new HashMap<>();
    private BlockPos targetPos;
    private BlockPos reservedTargetPos;
    private BlockState targetState;
    private TargetType targetType;
    private int pathFailures;
    private int requestedPlantingCropMask;
    private int plantingRequestCooldownTicks;

    public FarmingRuntimeTask(Control control) {
        this.control = control;
    }

    public void tickCooldown() {
        if (plantingRequestCooldownTicks > 0) {
            plantingRequestCooldownTicks--;
        }
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
        requestedPlantingCropMask = plantingRequestCooldownTicks == 0
                ? findRequestedPlantingCropMask(world, workOrigin, cropMask)
                : 0;
        if (requestedPlantingCropMask != 0) {
            releaseReservedTarget();
            ValetDebug.record(control.villager(), "farm needs_planting_items crops=" + requestedPlantingCropMask + " -> RETURNING");
            control.setState(State.RETURNING);
            control.setDelayTicks(4);
            return;
        }

        boolean canHarvest = cropMask != 0 && control.hasInventorySpace();
        boolean canPlant = cropMask != 0
                && ValetOrders.shouldReplantFarm(control.villager())
                && hasAnyPlantingItem(cropMask);
        boolean canTill = ValetOrders.shouldTillFarm(control.villager());
        if (!canHarvest && !canPlant && !canTill) {
            releaseReservedTarget();
            ValetDebug.record(control.villager(), "farm inventory_full -> RETURNING");
            control.setState(control.hasInventoryItems() ? State.RETURNING : State.RETURNING_HOME);
            return;
        }

        WorkTarget target = findNearestTarget(world, workOrigin, canHarvest, canPlant, canTill, cropMask);
        if (target == null) {
            releaseReservedTarget();
            ValetDebug.record(control.villager(), "farm no_target");
            boolean hasDepositableItems = control.hasInventoryItems() && !hasOnlyPlantingItems(cropMask);
            control.setState(hasDepositableItems ? State.RETURNING : State.RETURNING_HOME);
            control.setDelayTicks(control.noTargetDelayTicks());
            return;
        }

        requestedPlantingCropMask = 0;
        targetPos = target.pos();
        targetType = target.type();
        if (!claimTarget(world, targetPos)) {
            return;
        }

        if (control.canReachTargetFromStand(targetPos, control.villager().blockPosition())) {
            pathFailures = 0;
            beginHarvesting(world, targetPos);
            return;
        }

        Set<BlockPos> goals = control.findStandGoals(world, targetPos, PathPurpose.CROP);
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

    public boolean canWorkFromCurrentStand() {
        return targetPos != null
                && control.canReachTargetFromStand(targetPos, control.villager().blockPosition());
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
        if (targetType == TargetType.PLANT) {
            tickPlanting(world);
            return;
        }
        if (targetType == TargetType.LOOT) {
            tickLootCollection(world);
            return;
        }
        tickCropHarvesting(world);
    }

    public void clearTarget() {
        targetPos = null;
        targetType = null;
        releaseReservedTarget();
    }

    public void rememberCurrentTargetFailure() {
        rememberFailedTarget(targetPos);
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
        requestedPlantingCropMask = 0;
        plantingRequestCooldownTicks = 0;
    }

    public int requestedPlantingCropMask() {
        return requestedPlantingCropMask;
    }

    public void deferPlantingRequest() {
        requestedPlantingCropMask = 0;
        plantingRequestCooldownTicks = PLANTING_REQUEST_RETRY_TICKS;
    }

    public String debugSummary() {
        return "farmTarget=" + shortPos(targetPos)
                + " farmType=" + (targetType == null ? "-" : targetType)
                + " failed=" + failedTargets.size()
                + " seedRequest=" + requestedPlantingCropMask
                + " seedRetry=" + plantingRequestCooldownTicks;
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
        if (!world.destroyBlock(targetPos, false, control.villager())) {
            ValetDebug.record(control.villager(), "farm break_failed target=" + ValetDebug.shortPos(targetPos));
            rememberFailedTarget(targetPos);
            control.setState(control.interruptedWorkState());
            clearHarvestState();
            control.setDelayTicks(20);
            return;
        }
        world.levelEvent(2001, targetPos, Block.getId(currentState));
        control.collectDrops(drops);
        control.collectNearbyItemEntities(world);

        boolean replanted = tryReplant(world, targetPos, currentState);
        int xp = replanted ? 5 : 4;
        if (replanted && ValetProgress.hasPerk(control.villager(), ValetPerk.FARM_REPLANTING)) {
            xp++;
        }
        ValetProgress.addXp(control.villager(), xp);
        ValetDebug.record(control.villager(), "farm harvested target=" + ValetDebug.shortPos(targetPos) + " replanted=" + replanted);

        control.setState(afterFarmActionState());
        clearHarvestState();
        control.setDelayTicks(control.actionDelayTicks());
    }

    private void tickLootCollection(ServerLevel world) {
        if (targetPos == null || !hasFarmLootNear(world, targetPos, ValetOrders.getFarmCropMask(control.villager()))) {
            ValetDebug.record(control.villager(), "farm loot_changed target=" + ValetDebug.shortPos(targetPos));
            control.setState(control.interruptedWorkState());
            clearHarvestState();
            return;
        }

        if (!claimTarget(world, targetPos)) {
            return;
        }

        control.villager().getLookControl().setLookAt(targetPos.getX() + 0.5D, targetPos.getY() + 0.5D, targetPos.getZ() + 0.5D);
        int collected = control.collectNearbyItemEntities(world);
        if (collected <= 0) {
            rememberFailedTarget(targetPos);
            ValetDebug.record(control.villager(), "farm loot_not_collected target=" + ValetDebug.shortPos(targetPos));
            control.setState(control.interruptedWorkState());
            clearHarvestState();
            control.setDelayTicks(10);
            return;
        }
        ValetProgress.addXp(control.villager(), 1);
        ValetDebug.record(control.villager(), "farm collected_loot items=" + collected + " target=" + ValetDebug.shortPos(targetPos));
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
            ValetProgress.addXp(control.villager(), ValetProgress.hasPerk(control.villager(), ValetPerk.FARM_TILLING) ? 3 : 2);
        }
        ValetDebug.record(control.villager(), "farm tilled target=" + ValetDebug.shortPos(targetPos) + " success=" + tilled);
        control.setState(afterFarmActionState());
        clearHarvestState();
        control.setDelayTicks(control.actionDelayTicks());
    }

    private void tickPlanting(ServerLevel world) {
        int cropMask = ValetOrders.getFarmCropMask(control.villager());
        if (targetPos == null || !isPlantableSoil(world, targetPos, cropMask)) {
            ValetDebug.record(control.villager(), "farm plant_changed target=" + ValetDebug.shortPos(targetPos));
            control.setState(control.interruptedWorkState());
            clearHarvestState();
            return;
        }

        Planting planting = choosePlanting(world, targetPos, cropMask);
        if (planting == null) {
            ValetDebug.record(control.villager(), "farm no_seed target=" + ValetDebug.shortPos(targetPos));
            control.setState(afterFarmActionState());
            clearHarvestState();
            control.setDelayTicks(control.noTargetDelayTicks());
            return;
        }

        BlockPos cropPos = targetPos.above();
        clearSnowAbove(world, targetPos);
        if (!planting.state().canSurvive(world, cropPos)) {
            ValetDebug.record(control.villager(), "farm cannot_plant target=" + ValetDebug.shortPos(cropPos));
            control.setState(control.interruptedWorkState());
            clearHarvestState();
            return;
        }
        if (!control.takeOneItem(planting.item())) {
            ValetDebug.record(control.villager(), "farm no_seed target=" + ValetDebug.shortPos(targetPos));
            control.setState(afterFarmActionState());
            clearHarvestState();
            control.setDelayTicks(control.noTargetDelayTicks());
            return;
        }

        control.villager().getLookControl().setLookAt(cropPos.getX() + 0.5D, cropPos.getY() + 0.5D, cropPos.getZ() + 0.5D);
        control.villager().swing(InteractionHand.MAIN_HAND);
        if (!world.setBlock(cropPos, planting.state(), Block.UPDATE_ALL)) {
            control.returnItem(planting.item());
            rememberFailedTarget(targetPos);
            ValetDebug.record(control.villager(), "farm plant_failed target=" + ValetDebug.shortPos(cropPos));
            control.setState(control.interruptedWorkState());
            clearHarvestState();
            control.setDelayTicks(20);
            return;
        }
        world.playSound(null, cropPos, SoundEvents.CROP_PLANTED, SoundSource.BLOCKS, 0.8F, 1.0F);
        ValetProgress.addXp(control.villager(), 2);
        ValetDebug.record(control.villager(), "farm planted target=" + ValetDebug.shortPos(cropPos)
                + " item=" + planting.item().getDescriptionId());
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

    private WorkTarget findNearestTarget(
            ServerLevel world,
            BlockPos workOrigin,
            boolean includeCrops,
            boolean includePlanting,
            boolean includeSoil,
            int cropMask
    ) {
        ValetFarmArea area = control.getFarmArea(world, ValetOrders.getFarmAreaId(control.villager()));
        WorkTarget nearest = null;
        if (includeCrops) {
            nearest = nearerTarget(nearest, findNearestLoot(world, workOrigin, area, cropMask), TargetType.LOOT);
            nearest = nearerTarget(nearest, findNearestCrop(world, workOrigin, area, cropMask), TargetType.CROP);
        }
        if (includePlanting) {
            nearest = nearerTarget(nearest, findNearestPlantableSoil(world, workOrigin, area, cropMask), TargetType.PLANT);
        }
        if (includeSoil) {
            nearest = nearerTarget(nearest, findNearestSoil(world, workOrigin, area), TargetType.SOIL);
        }
        return nearest;
    }

    private WorkTarget nearerTarget(WorkTarget current, BlockPos candidate, TargetType type) {
        if (candidate == null) {
            return current;
        }
        if (current == null) {
            return new WorkTarget(candidate, type);
        }
        BlockPos villagerPos = control.villager().blockPosition();
        return squaredDistance(villagerPos, candidate) < squaredDistance(villagerPos, current.pos())
                ? new WorkTarget(candidate, type)
                : current;
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

    private BlockPos findNearestLoot(ServerLevel world, BlockPos workOrigin, ValetFarmArea area, int cropMask) {
        BlockPos nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (ItemEntity itemEntity : world.getEntitiesOfClass(ItemEntity.class, lootSearchBox(workOrigin, area), item -> !item.isRemoved())) {
            ItemStack stack = itemEntity.getItem();
            BlockPos itemPos = itemEntity.blockPosition().immutable();
            if (stack.isEmpty()
                    || isFailedTarget(itemPos)
                    || control.isBlockReservedByOther(world, itemPos)
                    || area != null && !areaContainsLooseItem(area, itemPos)
                    || !isFarmLoot(stack, cropMask)
                    || !control.canStoreAllDrops(List.of(stack.copy()))) {
                continue;
            }

            double distance = squaredDistance(control.villager().blockPosition(), itemPos);
            if (distance < nearestDistance) {
                nearest = itemPos;
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

    private BlockPos findNearestPlantableSoil(ServerLevel world, BlockPos workOrigin, ValetFarmArea area, int cropMask) {
        if (!hasAnyPlantingItem(cropMask)) {
            return null;
        }

        BlockPos nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (BlockPos pos : candidates(workOrigin, area)) {
            BlockPos candidate = pos.immutable();
            BlockPos soilPos = area == null ? candidate : candidate.below();
            if (control.isBlockReservedByOther(world, soilPos)
                    || area != null && !area.contains(candidate)
                    || choosePlanting(world, soilPos, cropMask) == null) {
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

    private int findRequestedPlantingCropMask(ServerLevel world, BlockPos workOrigin, int cropMask) {
        if (!ValetOrders.shouldReplantFarm(control.villager())) {
            return 0;
        }

        ValetFarmArea area = control.getFarmArea(world, ValetOrders.getFarmAreaId(control.villager()));
        for (BlockPos pos : candidates(workOrigin, area)) {
            BlockPos candidate = pos.immutable();
            BlockPos soilPos = area == null ? candidate : candidate.below();
            if (isFailedTarget(soilPos)
                    || control.isBlockReservedByOther(world, soilPos)
                    || area != null && !area.contains(candidate)) {
                continue;
            }

            int compatibleCropMask = plantingCropMaskForSoil(world, soilPos, cropMask);
            if (compatibleCropMask != 0 && !hasAnyPlantingItem(compatibleCropMask)) {
                return compatibleCropMask;
            }
        }
        return 0;
    }

    private static int plantingCropMaskForSoil(ServerLevel world, BlockPos soilPos, int cropMask) {
        if (soilPos == null || !isClearForFarmUse(world.getBlockState(soilPos.above()))) {
            return 0;
        }
        if (world.getBlockState(soilPos).is(Blocks.SOUL_SAND)) {
            return cropMask & ValetFarmCrop.NETHER_WART.mask();
        }
        if (!world.getBlockState(soilPos).is(Blocks.FARMLAND)) {
            return 0;
        }
        int regularCropMask = ValetFarmCrop.WHEAT.mask()
                | ValetFarmCrop.CARROT.mask()
                | ValetFarmCrop.POTATO.mask()
                | ValetFarmCrop.BEETROOT.mask();
        return cropMask & regularCropMask;
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

        if (world.setBlock(pos, replantState, Block.UPDATE_ALL)) {
            return true;
        }
        control.returnItem(replantItem);
        return false;
    }

    private boolean isValidTarget(ServerLevel world, BlockPos pos, TargetType type) {
        if (type == TargetType.CROP) {
            return isHarvestableCrop(world, pos, ValetOrders.getFarmCropMask(control.villager()));
        }
        if (type == TargetType.SOIL) {
            return isTillableSoil(world, pos);
        }
        if (type == TargetType.PLANT) {
            int cropMask = ValetOrders.getFarmCropMask(control.villager());
            return ValetOrders.shouldReplantFarm(control.villager())
                    && isPlantableSoil(world, pos, cropMask)
                    && choosePlanting(world, pos, cropMask) != null;
        }
        if (type == TargetType.LOOT) {
            return hasFarmLootNear(world, pos, ValetOrders.getFarmCropMask(control.villager()));
        }
        return false;
    }

    private boolean hasFarmLootNear(ServerLevel world, BlockPos pos, int cropMask) {
        if (pos == null || cropMask == 0 || !control.hasInventorySpace()) {
            return false;
        }

        return !world.getEntitiesOfClass(ItemEntity.class, lootTargetBox(pos), item -> !item.isRemoved()
                && isFarmLoot(item.getItem(), cropMask)
                && control.canStoreAllDrops(List.of(item.getItem().copy()))).isEmpty();
    }

    private AABB lootSearchBox(BlockPos workOrigin, ValetFarmArea area) {
        if (area != null) {
            return new AABB(
                    area.minX() - 1.0D,
                    area.minY() - 1.0D,
                    area.minZ() - 1.0D,
                    area.maxX() + 2.0D,
                    area.maxY() + 2.0D,
                    area.maxZ() + 2.0D
            );
        }

        int radius = control.farmRadius();
        int verticalRadius = control.farmVerticalRadius();
        return new AABB(
                workOrigin.getX() - radius,
                workOrigin.getY() - verticalRadius,
                workOrigin.getZ() - radius,
                workOrigin.getX() + radius + 1.0D,
                workOrigin.getY() + verticalRadius + 1.0D,
                workOrigin.getZ() + radius + 1.0D
        );
    }

    private static AABB lootTargetBox(BlockPos pos) {
        return new AABB(
                pos.getX() - 1.25D,
                pos.getY() - 1.0D,
                pos.getZ() - 1.25D,
                pos.getX() + 2.25D,
                pos.getY() + 2.0D,
                pos.getZ() + 2.25D
        );
    }

    private static boolean areaContainsLooseItem(ValetFarmArea area, BlockPos itemPos) {
        return area.contains(itemPos) || area.contains(itemPos.below());
    }

    private static boolean isFarmLoot(ItemStack stack, int cropMask) {
        return ValetFarmCrop.WHEAT.isEnabled(cropMask) && (stack.is(Items.WHEAT) || stack.is(Items.WHEAT_SEEDS))
                || ValetFarmCrop.CARROT.isEnabled(cropMask) && stack.is(Items.CARROT)
                || ValetFarmCrop.POTATO.isEnabled(cropMask) && (stack.is(Items.POTATO) || stack.is(Items.POISONOUS_POTATO))
                || ValetFarmCrop.BEETROOT.isEnabled(cropMask) && (stack.is(Items.BEETROOT) || stack.is(Items.BEETROOT_SEEDS))
                || ValetFarmCrop.NETHER_WART.isEnabled(cropMask) && stack.is(Items.NETHER_WART);
    }

    private static boolean isHarvestableCrop(ServerLevel world, BlockPos pos, int cropMask) {
        if (pos == null || cropMask == 0) {
            return false;
        }
        BlockState state = world.getBlockState(pos);
        return ValetFarmCrop.matchesAnyEnabled(state, cropMask) && isMatureCrop(state);
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
        return state.isAir() || state.is(Blocks.SNOW) || state.is(Blocks.SNOW_BLOCK);
    }

    private static void clearSnowAbove(ServerLevel world, BlockPos pos) {
        BlockPos above = pos.above();
        BlockState state = world.getBlockState(above);
        if (state.is(Blocks.SNOW) || state.is(Blocks.SNOW_BLOCK)) {
            world.setBlock(above, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    private static boolean isPlantableSoil(ServerLevel world, BlockPos pos, int cropMask) {
        if (pos == null || !isClearForFarmUse(world.getBlockState(pos.above()))) {
            return false;
        }
        BlockState soil = world.getBlockState(pos);
        boolean regularCropEnabled = ValetFarmCrop.WHEAT.isEnabled(cropMask)
                || ValetFarmCrop.CARROT.isEnabled(cropMask)
                || ValetFarmCrop.POTATO.isEnabled(cropMask)
                || ValetFarmCrop.BEETROOT.isEnabled(cropMask);
        return soil.is(Blocks.FARMLAND) && regularCropEnabled
                || soil.is(Blocks.SOUL_SAND) && ValetFarmCrop.NETHER_WART.isEnabled(cropMask);
    }

    private boolean hasAnyPlantingItem(int cropMask) {
        return ValetFarmCrop.WHEAT.isEnabled(cropMask) && control.hasItem(Items.WHEAT_SEEDS)
                || ValetFarmCrop.CARROT.isEnabled(cropMask) && control.hasItem(Items.CARROT)
                || ValetFarmCrop.POTATO.isEnabled(cropMask) && control.hasItem(Items.POTATO)
                || ValetFarmCrop.BEETROOT.isEnabled(cropMask) && control.hasItem(Items.BEETROOT_SEEDS)
                || ValetFarmCrop.NETHER_WART.isEnabled(cropMask) && control.hasItem(Items.NETHER_WART);
    }

    private boolean hasOnlyPlantingItems(int cropMask) {
        Container inventory = control.villager().getInventory();
        boolean foundPlantingItem = false;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty() || stack.is(Items.ARROW)) {
                continue;
            }
            if (!isPlantingItem(stack, cropMask)) {
                return false;
            }
            foundPlantingItem = true;
        }
        return foundPlantingItem;
    }

    private static boolean isPlantingItem(ItemStack stack, int cropMask) {
        return ValetFarmCrop.WHEAT.isEnabled(cropMask) && stack.is(Items.WHEAT_SEEDS)
                || ValetFarmCrop.CARROT.isEnabled(cropMask) && stack.is(Items.CARROT)
                || ValetFarmCrop.POTATO.isEnabled(cropMask) && stack.is(Items.POTATO)
                || ValetFarmCrop.BEETROOT.isEnabled(cropMask) && stack.is(Items.BEETROOT_SEEDS)
                || ValetFarmCrop.NETHER_WART.isEnabled(cropMask) && stack.is(Items.NETHER_WART);
    }

    private Planting choosePlanting(ServerLevel world, BlockPos soilPos, int cropMask) {
        if (!isPlantableSoil(world, soilPos, cropMask)) {
            return null;
        }

        if (world.getBlockState(soilPos).is(Blocks.SOUL_SAND)) {
            return ValetFarmCrop.NETHER_WART.isEnabled(cropMask) && control.hasItem(Items.NETHER_WART)
                    ? new Planting(Items.NETHER_WART, Blocks.NETHER_WART.defaultBlockState())
                    : null;
        }

        if (ValetFarmCrop.WHEAT.isEnabled(cropMask) && control.hasItem(Items.WHEAT_SEEDS)) {
            return new Planting(Items.WHEAT_SEEDS, Blocks.WHEAT.defaultBlockState());
        }
        if (ValetFarmCrop.CARROT.isEnabled(cropMask) && control.hasItem(Items.CARROT)) {
            return new Planting(Items.CARROT, Blocks.CARROTS.defaultBlockState());
        }
        if (ValetFarmCrop.POTATO.isEnabled(cropMask) && control.hasItem(Items.POTATO)) {
            return new Planting(Items.POTATO, Blocks.POTATOES.defaultBlockState());
        }
        if (ValetFarmCrop.BEETROOT.isEnabled(cropMask) && control.hasItem(Items.BEETROOT_SEEDS)) {
            return new Planting(Items.BEETROOT_SEEDS, Blocks.BEETROOTS.defaultBlockState());
        }
        return null;
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
        SOIL,
        PLANT,
        LOOT
    }

    private record WorkTarget(BlockPos pos, TargetType type) {
    }

    private record Planting(Item item, BlockState state) {
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

        int collectNearbyItemEntities(ServerLevel world);

        boolean claimBlock(ServerLevel world, BlockPos pos, int ttlTicks);

        boolean isBlockReservedByOther(ServerLevel world, BlockPos pos);

        void releaseBlock(BlockPos pos);

        boolean takeOneItem(Item item);

        void returnItem(Item item);

        boolean hasItem(Item item);

        State interruptedWorkState();

        void setState(State state);

        void setDelayTicks(int ticks);

        int noTargetDelayTicks();

        int farmRadius();

        int farmVerticalRadius();

        int actionDelayTicks();
    }
}
