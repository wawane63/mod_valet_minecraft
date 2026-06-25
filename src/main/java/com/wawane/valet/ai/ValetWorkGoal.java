package com.wawane.valet.ai;

import com.wawane.valet.ValetMod;
import com.wawane.valet.ValetConversations;
import com.wawane.valet.ValetDebug;
import com.wawane.valet.ValetHome;
import com.wawane.valet.ai.ValetStateMachine.PathPurpose;
import com.wawane.valet.ai.ValetStateMachine.State;
import com.wawane.valet.ai.core.ValetOrderKey;
import com.wawane.valet.ai.core.ValetWorkSettings;
import com.wawane.valet.ai.inventory.ValetInventoryTransfer;
import com.wawane.valet.ai.path.ValetPathPlanner;
import com.wawane.valet.ai.tasks.ConstructionRuntimeTask;
import com.wawane.valet.ai.tasks.LogisticsRuntimeTask;
import com.wawane.valet.ai.tasks.MiningRuntimeTask;
import com.wawane.valet.ai.tasks.combat.CombatRuntimeTask;
import com.wawane.valet.ai.tasks.crafting.CraftingRuntimeTask;
import com.wawane.valet.order.ValetCraftTarget;
import com.wawane.valet.order.ValetMineTarget;
import com.wawane.valet.order.ValetOrder;
import com.wawane.valet.order.ValetOrders;
import com.wawane.valet.order.ValetWoodTarget;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.world.LightType;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ValetWorkGoal extends Goal {
    private static final Set<UUID> RESTART_REQUESTS = ConcurrentHashMap.newKeySet();
    private static final int PASSAGE_HEIGHT = 2;
    private static final int NAVIGATION_STEP_TIMEOUT_TICKS = 80;
    private static final int BUILD_HORIZONTAL_REACH = 4;
    private static final int BUILD_VERTICAL_REACH = 6;

    private static final Set<TagKey<Block>> ORE_TAGS = Set.of(
            BlockTags.COAL_ORES,
            BlockTags.COPPER_ORES,
            BlockTags.IRON_ORES,
            BlockTags.GOLD_ORES,
            BlockTags.REDSTONE_ORES,
            BlockTags.LAPIS_ORES,
            BlockTags.EMERALD_ORES,
            BlockTags.DIAMOND_ORES
    );
    private static final Set<Block> NATURAL_PATH_BLOCKS = Set.of(
            Blocks.STONE,
            Blocks.GRANITE,
            Blocks.DIORITE,
            Blocks.ANDESITE,
            Blocks.DEEPSLATE,
            Blocks.TUFF,
            Blocks.CALCITE,
            Blocks.DRIPSTONE_BLOCK,
            Blocks.DIRT,
            Blocks.GRASS_BLOCK,
            Blocks.COARSE_DIRT,
            Blocks.ROOTED_DIRT,
            Blocks.PODZOL,
            Blocks.MYCELIUM,
            Blocks.MUD,
            Blocks.CLAY,
            Blocks.GRAVEL,
            Blocks.SAND,
            Blocks.RED_SAND,
            Blocks.SANDSTONE,
            Blocks.RED_SANDSTONE,
            Blocks.NETHERRACK,
            Blocks.BASALT,
            Blocks.BLACKSTONE,
            Blocks.END_STONE,
            Blocks.SNOW,
            Blocks.SNOW_BLOCK,
            Blocks.ICE,
            Blocks.PACKED_ICE,
            Blocks.BLUE_ICE
    );
    private static final Set<Block> FALLING_PATH_BLOCKS = Set.of(
            Blocks.GRAVEL,
            Blocks.SAND,
            Blocks.RED_SAND
    );

    private final VillagerEntity villager;
    private final ValetWorkSettings settings;
    private final ValetPathPlanner pathPlanner = new ValetPathPlanner();
    private final MiningRuntimeTask miningTask;
    private final ConstructionRuntimeTask constructionTask;
    private final LogisticsRuntimeTask logisticsTask;
    private final CombatRuntimeTask combatTask;
    private final CraftingRuntimeTask craftingTask;
    private State state = State.FIND_TARGET;
    private PathPurpose pathPurpose = PathPurpose.ORE;
    private List<BlockPos> path = List.of();
    private int pathIndex;
    private BlockPos navigationStepTarget;
    private int navigationStepTicks;
    private int delayTicks;
    private String activeOrderKey = "";
    private BlockPos animatedContainerPos;
    private Block animatedContainerBlock;
    private int animatedContainerCloseTicks;

    public ValetWorkGoal(VillagerEntity villager) {
        this.villager = villager;
        this.settings = new ValetWorkSettings(villager);
        this.miningTask = new MiningRuntimeTask(new MiningControl());
        this.constructionTask = new ConstructionRuntimeTask(new ConstructionControl());
        this.logisticsTask = new LogisticsRuntimeTask(new LogisticsControl());
        this.combatTask = new CombatRuntimeTask(new CombatControl());
        this.craftingTask = new CraftingRuntimeTask(new CraftingControl());
        setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    public static void requestRestart(VillagerEntity villager) {
        RESTART_REQUESTS.add(villager.getUuid());
    }

    public static void clearRestartRequest(UUID uuid) {
        RESTART_REQUESTS.remove(uuid);
    }

    @Override
    public boolean canStart() {
        return isAvailableValet() && (ValetConversations.isTalking(villager) || hasWorkOrigin() || hasActiveOrder() || hasInventoryItems());
    }

    @Override
    public boolean shouldContinue() {
        return isAvailableValet() && (ValetConversations.isTalking(villager) || hasWorkOrigin() || hasActiveOrder() || hasInventoryItems());
    }

    @Override
    public void start() {
        resetForCurrentOrder("starts");
    }

    @Override
    public void stop() {
        state = State.FIND_TARGET;
        delayTicks = 0;
        clearPathState();
        clearMiningState();
        clearVeinState();
        closeAnimatedContainerNow();
        villager.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (!(villager.getWorld() instanceof ServerWorld world)) {
            return;
        }

        tickAnimatedContainer(world);
        constructionTask.tickCooldown();
        miningTask.tickCooldown();
        String currentOrderKey = currentOrderKey();
        if (RESTART_REQUESTS.remove(villager.getUuid()) || !currentOrderKey.equals(activeOrderKey)) {
            resetForCurrentOrder("restarts");
        }

        if (ValetConversations.isTalking(villager)) {
            suppressVanillaMovementTargets();
            holdConversationPosition();
            return;
        }

        if (shouldClaimMovement()) {
            suppressVanillaMovementTargets();
        }

        if (escapeFluidIfNeeded(world)) {
            return;
        }

        if (combatTask.tick(world)) {
            return;
        }

        if (delayTicks > 0) {
            delayTicks--;
            return;
        }

        updatePassiveState();

        switch (state) {
            case IDLE -> logisticsTask.idleAtWorkstation(world);
            case FIND_TARGET -> findTarget(world);
            case EXECUTING_PATH -> executePath(world);
            case MINING -> miningTask.tickMining(world);
            case PLACING -> constructionTask.tickPlacing(world);
            case CRAFTING -> craftingTask.tickCrafting(world);
            case COLLECTING -> miningTask.tickCollecting(world);
            case RETURNING -> logisticsTask.returnToChest(world);
            case RETURNING_HOME -> logisticsTask.returnToWorkstation(world);
            case DEPOSITING -> logisticsTask.tickDepositing(world);
        }
    }

    public String debugSummary() {
        Inventory inventory = villager.getInventory();
        return "state=" + state
                + " order=" + currentOrderKey()
                + " pos=" + ValetDebug.shortPos(villager.getBlockPos())
                + " inv=" + inventoryItemCount(inventory) + "/" + getUsableInventorySlots(inventory)
                + " delay=" + delayTicks
                + " path=" + pathPurpose + ":" + pathIndex + "/" + path.size()
                + " " + miningTask.debugSummary()
                + " " + constructionTask.debugSummary()
                + " " + craftingTask.debugSummary()
                + " " + logisticsTask.debugSummary()
                + " " + combatTask.debugSummary();
    }

    private boolean isAvailableValet() {
        return !villager.getWorld().isClient
                && !villager.isBaby()
                && !villager.isSleeping()
                && villager.getVillagerData().getProfession() == ValetMod.VALET_PROFESSION;
    }

    private boolean hasMiningOrder() {
        ValetOrder order = ValetOrders.get(villager);
        return order == ValetOrder.MINE_ORES && ValetOrders.getMineTarget(villager) != null
                || order == ValetOrder.CHOP_WOOD && ValetOrders.getWoodTarget(villager) != null;
    }

    private boolean hasConstructionOrder() {
        return ValetOrders.get(villager) == ValetOrder.BUILD_STRUCTURE && ValetOrders.getConstructionTargetId(villager) >= 0;
    }

    private boolean hasCraftOrder() {
        return ValetOrders.get(villager) == ValetOrder.CRAFT && ValetOrders.getCraftTarget(villager) != null;
    }

    private boolean hasActiveOrder() {
        return hasMiningOrder() || hasConstructionOrder() || hasCraftOrder();
    }

    private boolean hasWorkOrigin() {
        return villager.getWorld() instanceof ServerWorld world && getKnownWorkOrigin(world) != null;
    }

    private State chooseStartState() {
        return ValetStateMachine.chooseStartState(ValetConversations.isTalking(villager), hasActiveOrder(), shouldReturnToChestBeforeWork());
    }

    private void resetForCurrentOrder(String action) {
        state = chooseStartState();
        delayTicks = 0;
        activeOrderKey = currentOrderKey();
        closeAnimatedContainerNow();
        clearPathState();
        miningTask.clearAll();
        ValetMod.LOGGER.info("Valet {} goal {} order {}", villager.getUuid(), action, activeOrderKey);
        ValetDebug.record(villager, "reset=" + action + " order=" + activeOrderKey + " state=" + state);
    }

    private String currentOrderKey() {
        return ValetOrderKey.of(villager);
    }

    private boolean shouldReturnToChestBeforeWork() {
        if (hasConstructionOrder()) {
            return false;
        }
        if (hasCraftOrder()) {
            return false;
        }
        if (state == State.RETURNING || isExecuting(PathPurpose.CHEST)) {
            return true;
        }
        if (hasMiningOrder() && villager.getWorld() instanceof ServerWorld world) {
            if (!hasInventorySpace()) {
                return true;
            }
            BlockPos workOrigin = getKnownWorkOrigin(world);
            return workOrigin != null && squaredDistance(villager.getBlockPos(), workOrigin) > 64;
        }
        return hasInventoryItems();
    }

    private void updatePassiveState() {
        if (hasMiningOrder()) {
            if (shouldPreemptForMiningOrder()) {
                clearPathState();
                clearMiningState();
                clearVeinState();
                state = hasInventorySpace() ? State.FIND_TARGET : State.RETURNING;
            }
            return;
        }

        clearVeinState();
        if (hasConstructionOrder()) {
            if (shouldPreemptForConstructionOrder()) {
                clearPathState();
                clearMiningState();
                state = State.FIND_TARGET;
            }
            return;
        }

        if (hasCraftOrder()) {
            if (shouldPreemptForCraftOrder()) {
                clearPathState();
                clearMiningState();
                clearVeinState();
                state = State.FIND_TARGET;
            }
            return;
        }

        if (state == State.IDLE || state == State.RETURNING_HOME || state == State.RETURNING || state == State.DEPOSITING || isExecuting(PathPurpose.CHEST) || isExecuting(PathPurpose.HOME)) {
            return;
        }

        clearPathState();
        clearMiningState();
        state = State.RETURNING_HOME;
    }

    private boolean shouldPreemptForMiningOrder() {
        if (state == State.IDLE || state == State.RETURNING_HOME || isExecuting(PathPurpose.HOME)) {
            return true;
        }
        if (hasInventorySpace() && (state == State.RETURNING || state == State.DEPOSITING || isExecuting(PathPurpose.CHEST))) {
            return true;
        }
        return false;
    }

    private boolean shouldPreemptForConstructionOrder() {
        return state == State.IDLE
                || state == State.RETURNING_HOME
                || state == State.RETURNING
                || state == State.DEPOSITING
                || isExecuting(PathPurpose.HOME)
                || isExecuting(PathPurpose.CHEST);
    }

    private boolean shouldPreemptForCraftOrder() {
        if (state == State.RETURNING || state == State.DEPOSITING || isExecuting(PathPurpose.CHEST)) {
            return !hasInventoryItems();
        }
        return state == State.IDLE
                || state == State.RETURNING_HOME
                || isExecuting(PathPurpose.HOME);
    }

    private void findTarget(ServerWorld world) {
        if (hasConstructionOrder()) {
            constructionTask.findTarget(world);
            return;
        }

        if (hasCraftOrder()) {
            craftingTask.findTarget(world);
            return;
        }

        miningTask.findTarget(world);
    }

    private void executePath(ServerWorld world) {
        if (pathIndex >= path.size()) {
            if (pathPurpose == PathPurpose.ORE) {
                miningTask.completePath(world);
            } else if (pathPurpose == PathPurpose.CHEST) {
                state = State.DEPOSITING;
            } else if (pathPurpose == PathPurpose.BUILD) {
                state = State.PLACING;
            } else if (pathPurpose == PathPurpose.CRAFT) {
                state = State.CRAFTING;
            } else {
                state = State.IDLE;
            }
            return;
        }

        BlockPos current = currentStandPos(world);
        BlockPos next = path.get(pathIndex);
        if (hasReachedPathStep(current, next)) {
            clearNavigationStep();
            pathIndex++;
            delayTicks = pathStepDelayTicks();
            return;
        }

        BlockPos obstruction = findMovementObstruction(world, current, next);
        if (obstruction != null) {
            miningTask.beginMining(world, obstruction, false);
            return;
        }

        if (!canTraverseStep(world, current, next)) {
            ValetDebug.record(villager, "path blocked purpose=" + pathPurpose
                    + " from=" + ValetDebug.shortPos(current)
                    + " next=" + ValetDebug.shortPos(next)
                    + " blocked=" + describeBlockedStep(world, current, next));
            if (pathPurpose == PathPurpose.ORE) {
                miningTask.rememberCurrentTarget();
            }
            state = interruptedPathState();
            clearPathState();
            delayTicks = 10;
            return;
        }

        openDoorsForStep(world, current, next);
        moveToPathStep(next);
    }

    private List<BlockPos> planPathToAdjacent(ServerWorld world, PathPurpose purpose, BlockPos targetBlock, Set<BlockPos> goals) {
        BlockPos start = currentStandPos(world);
        if (goals.isEmpty()) {
            return List.of();
        }

        BlockPos origin = getWorkOrigin(world);
        if (origin == null) {
            return List.of();
        }

        return pathPlanner.planPathToAdjacent(
                world,
                origin,
                start,
                targetBlock,
                goals,
                maxPathNodes(),
                maxPathLength(),
                (stepWorld, from, to) -> canPrepareStep(stepWorld, from, to, purpose),
                (stepWorld, from, to) -> movementCost(stepWorld, from, to, purpose)
        );
    }

    private void startPath(PathPurpose purpose, List<BlockPos> nextPath) {
        pathPurpose = purpose;
        path = nextPath;
        pathIndex = 0;
        clearNavigationStep();
        state = State.EXECUTING_PATH;
        ValetDebug.record(villager, "path start purpose=" + purpose + " len=" + nextPath.size());
    }

    private boolean hasReachedPathStep(BlockPos current, BlockPos step) {
        return current.equals(step);
    }

    private void moveToPathStep(BlockPos step) {
        boolean newStep = !step.equals(navigationStepTarget);
        if (newStep) {
            navigationStepTarget = step.toImmutable();
            navigationStepTicks = NAVIGATION_STEP_TIMEOUT_TICKS;
            villager.getNavigation().stop();
        }

        suppressVanillaMovementTargets();
        villager.getLookControl().lookAt(step.getX() + 0.5D, step.getY() + 1.0D, step.getZ() + 0.5D);
        villager.refreshPositionAndAngles(step.getX() + 0.5D, step.getY(), step.getZ() + 0.5D, villager.getYaw(), villager.getPitch());
        villager.setVelocity(0.0D, villager.getVelocity().y, 0.0D);
        ValetDebug.record(villager, "path step purpose=" + pathPurpose + " step=" + ValetDebug.shortPos(step));
    }

    private void clearNavigationStep() {
        navigationStepTarget = null;
        navigationStepTicks = 0;
    }

    private Set<BlockPos> findStandGoals(ServerWorld world, BlockPos targetBlock, PathPurpose purpose) {
        Set<BlockPos> goals = new HashSet<>();

        for (BlockPos pos : BlockPos.iterate(
                targetBlock.getX() - 2,
                targetBlock.getY() - 2,
                targetBlock.getZ() - 2,
                targetBlock.getX() + 2,
                targetBlock.getY() + 2,
                targetBlock.getZ() + 2
        )) {
            BlockPos stand = pos.toImmutable();
            if (!stand.equals(targetBlock)
                    && !wouldMineOwnSupport(targetBlock, stand, purpose)
                    && canReachTargetFromStand(targetBlock, stand)
                    && canPrepareStand(world, stand, purpose)) {
                goals.add(stand);
            }
        }

        return goals;
    }

    private boolean wouldMineOwnSupport(BlockPos targetBlock, BlockPos stand, PathPurpose purpose) {
        return (purpose == PathPurpose.ORE || purpose == PathPurpose.CRAFT) && stand.down().equals(targetBlock);
    }

    private Set<BlockPos> findBuildStandGoals(ServerWorld world, BlockPos targetBlock) {
        Set<BlockPos> goals = new HashSet<>();

        for (BlockPos pos : BlockPos.iterate(
                targetBlock.getX() - BUILD_HORIZONTAL_REACH,
                targetBlock.getY() - BUILD_VERTICAL_REACH,
                targetBlock.getZ() - BUILD_HORIZONTAL_REACH,
                targetBlock.getX() + BUILD_HORIZONTAL_REACH,
                targetBlock.getY(),
                targetBlock.getZ() + BUILD_HORIZONTAL_REACH
        )) {
            BlockPos stand = pos.toImmutable();
            if (!stand.equals(targetBlock) && canReachBuildTargetFromStand(targetBlock, stand) && canPrepareStand(world, stand, PathPurpose.BUILD)) {
                goals.add(stand);
            }
        }

        return goals;
    }

    private boolean isNearWorkstation(ServerWorld world, BlockPos workOrigin) {
        Set<BlockPos> goals = findStandGoals(world, workOrigin, PathPurpose.HOME);
        BlockPos current = currentStandPos(world);
        return goals.contains(current) || squaredDistance(current, workOrigin) <= 4;
    }

    private boolean canReachTargetFromStand(BlockPos targetBlock, BlockPos stand) {
        return isFaceAdjacent(targetBlock, stand) || isFaceAdjacent(targetBlock, stand.up());
    }

    private boolean canReachBuildTargetFromStand(BlockPos targetBlock, BlockPos stand) {
        int dx = Math.abs(targetBlock.getX() - stand.getX());
        int dy = targetBlock.getY() - stand.getY();
        int dz = Math.abs(targetBlock.getZ() - stand.getZ());
        return dx <= BUILD_HORIZONTAL_REACH
                && dz <= BUILD_HORIZONTAL_REACH
                && dy >= 0
                && dy <= BUILD_VERTICAL_REACH
                && dx + dy + dz > 0;
    }

    private boolean isFaceAdjacent(BlockPos first, BlockPos second) {
        int distance = Math.abs(first.getX() - second.getX())
                + Math.abs(first.getY() - second.getY())
                + Math.abs(first.getZ() - second.getZ());
        return distance == 1;
    }

    private int movementCost(ServerWorld world, BlockPos from, BlockPos to, PathPurpose purpose) {
        int cost = 10 + Math.abs(to.getY() - from.getY()) * 6;
        for (BlockPos pos : movementClearancePositions(from, to)) {
            cost += clearCost(world, pos, purpose);
        }
        return cost;
    }

    private int clearCost(ServerWorld world, BlockPos pos, PathPurpose purpose) {
        BlockState blockState = world.getBlockState(pos);
        if (isPassableTunnelSpace(world, pos)) {
            return 0;
        }
        if (!canMinePathBlock(world, pos, blockState, purpose)) {
            return 10_000;
        }
        return 80 + Math.max(0, Math.round(blockState.getHardness(world, pos) * 20.0F));
    }

    private boolean canPrepareStand(ServerWorld world, BlockPos standPos, PathPurpose purpose) {
        if (!canStandOn(world, standPos.down())) {
            return false;
        }

        for (BlockPos pos : standClearancePositions(standPos)) {
            if (!canClearForTunnel(world, pos, purpose)) {
                return false;
            }
        }
        return true;
    }

    private boolean canPrepareStep(ServerWorld world, BlockPos from, BlockPos to, PathPurpose purpose) {
        int dx = Math.abs(to.getX() - from.getX());
        int dy = to.getY() - from.getY();
        int dz = Math.abs(to.getZ() - from.getZ());
        if (dx + dz != 1 || dy < -1 || dy > 1 || !canStandOn(world, to.down())) {
            return false;
        }

        for (BlockPos pos : movementClearancePositions(from, to)) {
            if (!canClearForTunnel(world, pos, purpose)) {
                return false;
            }
        }
        return true;
    }

    private boolean canClearForTunnel(ServerWorld world, BlockPos pos, PathPurpose purpose) {
        BlockState blockState = world.getBlockState(pos);
        return isPassableTunnelSpace(world, pos) || canMinePathBlock(world, pos, blockState, purpose);
    }

    private BlockPos findMovementObstruction(ServerWorld world, BlockPos from, BlockPos to) {
        for (BlockPos candidate : movementClearancePositions(from, to)) {
            BlockState blockState = world.getBlockState(candidate);
            if (!isPassableTunnelSpace(world, candidate)) {
                return canMinePathBlock(world, candidate, blockState, pathPurpose) ? candidate : null;
            }
        }

        return null;
    }

    private boolean canTraverseStep(ServerWorld world, BlockPos from, BlockPos to) {
        int dx = Math.abs(to.getX() - from.getX());
        int dy = to.getY() - from.getY();
        int dz = Math.abs(to.getZ() - from.getZ());
        if (dx + dz != 1 || dy < -1 || dy > 1) {
            return false;
        }

        if (!isSafeStand(world, to)) {
            return false;
        }

        for (BlockPos pos : movementClearancePositions(from, to)) {
            if (!isPassableTunnelSpace(world, pos)) {
                return false;
            }
        }
        return true;
    }

    private String describeBlockedStep(ServerWorld world, BlockPos from, BlockPos to) {
        int dx = Math.abs(to.getX() - from.getX());
        int dy = to.getY() - from.getY();
        int dz = Math.abs(to.getZ() - from.getZ());
        if (dx + dz != 1 || dy < -1 || dy > 1) {
            return "delta:" + dx + "," + dy + "," + dz;
        }

        if (!isSafeStand(world, to)) {
            return "stand:" + ValetDebug.shortPos(to);
        }

        for (BlockPos pos : movementClearancePositions(from, to)) {
            if (!isPassableTunnelSpace(world, pos)) {
                BlockState state = world.getBlockState(pos);
                return ValetDebug.shortPos(pos) + ":" + state.getBlock().getTranslationKey();
            }
        }
        return "unknown";
    }

    private boolean isSafeStand(ServerWorld world, BlockPos standPos) {
        if (!canStandOn(world, standPos.down())) {
            return false;
        }

        for (BlockPos pos : standClearancePositions(standPos)) {
            if (!isPassableTunnelSpace(world, pos)) {
                return false;
            }
        }
        return true;
    }

    private boolean escapeFluidIfNeeded(ServerWorld world) {
        BlockPos current = villager.getBlockPos();
        if (world.getBlockState(current).getFluidState().isEmpty()
                && world.getBlockState(current.up()).getFluidState().isEmpty()) {
            return false;
        }

        BlockPos safeStand = findNearestSafeStand(world, current, 5, 4);
        if (safeStand == null) {
            ValetDebug.record(villager, "water_stuck pos=" + ValetDebug.shortPos(current));
            state = State.RETURNING;
            delayTicks = 10;
            return false;
        }

        clearPathState();
        clearMiningState();
        villager.getNavigation().stop();
        villager.refreshPositionAndAngles(safeStand.getX() + 0.5D, safeStand.getY(), safeStand.getZ() + 0.5D, villager.getYaw(), villager.getPitch());
        villager.setVelocity(0.0D, 0.0D, 0.0D);
        state = hasConstructionOrder() && !hasInventoryItems() ? State.RETURNING_HOME : State.RETURNING;
        delayTicks = 2;
        ValetDebug.record(villager, "water_escape to=" + ValetDebug.shortPos(safeStand) + " state=" + state);
        return true;
    }

    private BlockPos findNearestSafeStand(ServerWorld world, BlockPos origin, int horizontalRadius, int verticalRadius) {
        BlockPos nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.iterateOutwards(origin, horizontalRadius, verticalRadius, horizontalRadius)) {
            BlockPos stand = pos.toImmutable();
            if (!isSafeStand(world, stand)) {
                continue;
            }

            double distance = squaredDistance(origin, stand);
            if (distance < nearestDistance) {
                nearest = stand;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    private List<BlockPos> standClearancePositions(BlockPos standPos) {
        List<BlockPos> positions = new ArrayList<>(PASSAGE_HEIGHT);
        for (int y = 0; y < PASSAGE_HEIGHT; y++) {
            positions.add(standPos.up(y));
        }
        return positions;
    }

    private List<BlockPos> movementClearancePositions(BlockPos from, BlockPos to) {
        return standClearancePositions(to);
    }

    private boolean isPassableTunnelSpace(ServerWorld world, BlockPos pos) {
        BlockState blockState = world.getBlockState(pos);
        return blockState.isAir()
                || isDoorPassage(blockState)
                || blockState.isOf(Blocks.TORCH)
                || blockState.isOf(Blocks.WALL_TORCH)
                || blockState.getFluidState().isEmpty() && blockState.getCollisionShape(world, pos).isEmpty();
    }

    private boolean isDoorPassage(BlockState blockState) {
        return blockState.getBlock() instanceof DoorBlock && blockState.getFluidState().isEmpty();
    }

    private void openDoorsForStep(ServerWorld world, BlockPos from, BlockPos to) {
        for (BlockPos pos : movementClearancePositions(from, to)) {
            openDoorAt(world, pos);
        }
    }

    private void openDoorAt(ServerWorld world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof DoorBlock) || !state.contains(DoorBlock.OPEN)) {
            return;
        }

        BlockPos lowerPos = state.contains(DoorBlock.HALF) && state.get(DoorBlock.HALF) == DoubleBlockHalf.UPPER ? pos.down() : pos;
        BlockState lowerState = world.getBlockState(lowerPos);
        BlockState upperState = world.getBlockState(lowerPos.up());
        boolean opened = false;
        if (lowerState.getBlock() instanceof DoorBlock && lowerState.contains(DoorBlock.OPEN) && !lowerState.get(DoorBlock.OPEN)) {
            world.setBlockState(lowerPos, lowerState.with(DoorBlock.OPEN, true), Block.NOTIFY_ALL);
            opened = true;
        }
        if (upperState.getBlock() instanceof DoorBlock && upperState.contains(DoorBlock.OPEN) && !upperState.get(DoorBlock.OPEN)) {
            world.setBlockState(lowerPos.up(), upperState.with(DoorBlock.OPEN, true), Block.NOTIFY_ALL);
            opened = true;
        }
        if (opened) {
            world.playSound(null, lowerPos, SoundEvents.BLOCK_WOODEN_DOOR_OPEN, SoundCategory.BLOCKS, 0.7F, 1.0F);
            ValetDebug.record(villager, "path door_open pos=" + ValetDebug.shortPos(lowerPos));
        }
    }

    private boolean canStandOn(ServerWorld world, BlockPos pos) {
        BlockState blockState = world.getBlockState(pos);
        if (blockState.isOf(Blocks.DIRT_PATH)) {
            return blockState.getFluidState().isEmpty();
        }
        if (blockState.getBlock() instanceof StairsBlock) {
            return blockState.getFluidState().isEmpty();
        }
        return !isPassableTunnelSpace(world, pos)
                && blockState.getFluidState().isEmpty()
                && blockState.getHardness(world, pos) >= 0.0F
                && blockState.isSideSolidFullSquare(world, pos, Direction.UP);
    }

    private BlockPos currentStandPos(ServerWorld world) {
        BlockPos current = villager.getBlockPos();
        if (canStandOn(world, current) && isPassableTunnelSpace(world, current.up())) {
            return current.up();
        }
        return current;
    }

    private boolean canMinePathBlock(ServerWorld world, BlockPos pos, BlockState blockState, PathPurpose purpose) {
        if (purpose == PathPurpose.CHEST) {
            return canMineNaturalPathBlock(world, pos, blockState);
        }
        if (purpose == PathPurpose.ORE && ValetOrders.get(villager) == ValetOrder.MINE_ORES) {
            return canMineWorkBlock(world, pos, blockState);
        }
        if (purpose == PathPurpose.ORE && ValetOrders.get(villager) == ValetOrder.CHOP_WOOD) {
            return canMineWorkBlock(world, pos, blockState);
        }
        return false;
    }

    private boolean canMineWorkBlock(ServerWorld world, BlockPos pos, BlockState blockState) {
        if (!canMineBaseBlock(world, pos, blockState)) {
            return false;
        }

        if (hasFallingBlockDirectlyAbove(world, pos)) {
            return false;
        }

        Block block = blockState.getBlock();
        if (FALLING_PATH_BLOCKS.contains(block)) {
            return true;
        }

        if (ValetOrders.get(villager) == ValetOrder.CHOP_WOOD && isTreeCrownBlock(blockState)) {
            return true;
        }

        return isSelectedResource(world, pos, blockState)
                || ORE_TAGS.stream().anyMatch(blockState::isIn)
                || NATURAL_PATH_BLOCKS.contains(block);
    }

    private boolean canMineNaturalPathBlock(ServerWorld world, BlockPos pos, BlockState blockState) {
        if (!canMineBaseBlock(world, pos, blockState) || hasFallingBlockDirectlyAbove(world, pos)) {
            return false;
        }
        return NATURAL_PATH_BLOCKS.contains(blockState.getBlock());
    }

    private boolean canMineCraftResource(ServerWorld world, BlockPos pos, BlockState blockState) {
        return canMineBaseBlock(world, pos, blockState) && !hasFallingBlockDirectlyAbove(world, pos);
    }

    private boolean canMineBaseBlock(ServerWorld world, BlockPos pos, BlockState blockState) {
        return !blockState.isAir()
                && blockState.getFluidState().isEmpty()
                && !wouldExposeFluid(world, pos)
                && !blockState.isOf(ValetMod.VALET_WORKSTATION)
                && !blockState.isOf(ValetMod.CONSTRUCTION_BEACON)
                && !blockState.isOf(ValetMod.CONSTRUCTION_BLUEPRINT)
                && !blockState.isOf(Blocks.CHEST)
                && !blockState.isOf(Blocks.TRAPPED_CHEST)
                && !blockState.isOf(Blocks.BARREL)
                && blockState.getHardness(world, pos) >= 0.0F;
    }

    private boolean wouldExposeFluid(ServerWorld world, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            if (!world.getFluidState(pos.offset(direction)).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private boolean hasFallingBlockDirectlyAbove(ServerWorld world, BlockPos pos) {
        return FALLING_PATH_BLOCKS.contains(world.getBlockState(pos.up()).getBlock());
    }

    private boolean isSelectedResource(ServerWorld world, BlockPos pos, BlockState blockState) {
        return matchesSelectedTarget(world, pos, blockState);
    }

    private int mineRadius() {
        return settings.mineRadius();
    }

    private int mineVerticalRadius() {
        return settings.mineVerticalRadius();
    }

    private int actionDelayTicks() {
        return settings.actionDelayTicks();
    }

    private int pathStepDelayTicks() {
        return settings.pathStepDelayTicks();
    }

    private int chestRadius() {
        return settings.chestRadius();
    }

    private int materialRadius() {
        return settings.materialRadius();
    }

    private int maxPathNodes() {
        return settings.maxPathNodes();
    }

    private int maxPathLength() {
        return settings.maxPathLength();
    }

    private int maxVeinBlocks() {
        return settings.maxVeinBlocks();
    }

    private int torchLightThreshold() {
        return settings.torchLightThreshold();
    }

    private void placeTorchIfNeeded(ServerWorld world, BlockPos minedPos) {
        BlockPos origin = villager.getBlockPos();
        if (world.getLightLevel(LightType.BLOCK, origin) > torchLightThreshold()) {
            return;
        }

        List<BlockPos> candidates = new ArrayList<>();
        if (minedPos != null) {
            candidates.add(minedPos);
            candidates.add(minedPos.down());
        }
        candidates.add(origin.north());
        candidates.add(origin.south());
        candidates.add(origin.east());
        candidates.add(origin.west());
        candidates.add(origin);

        for (BlockPos candidate : candidates) {
            if (tryPlaceTorch(world, candidate)) {
                return;
            }
        }
    }

    private boolean tryPlaceTorch(ServerWorld world, BlockPos pos) {
        if (!world.getBlockState(pos).isAir()) {
            return false;
        }

        BlockState torchState = Blocks.TORCH.getDefaultState();
        if (!torchState.canPlaceAt(world, pos)) {
            return false;
        }

        world.setBlockState(pos, torchState, Block.NOTIFY_ALL);
        world.playSound(null, pos, torchState.getSoundGroup().getPlaceSound(), SoundCategory.BLOCKS, 0.7F, 1.0F);
        return true;
    }

    private void animateMining(ServerWorld world, BlockPos miningPos, BlockState miningState) {
        villager.swingHand(Hand.MAIN_HAND);
        villager.getLookControl().lookAt(miningPos.getX() + 0.5D, miningPos.getY() + 0.5D, miningPos.getZ() + 0.5D);
        world.syncWorldEvent(2001, miningPos, Block.getRawIdFromState(miningState));
        world.playSound(null, miningPos, miningState.getSoundGroup().getBreakSound(), SoundCategory.BLOCKS, 0.75F, 1.0F);
    }

    private void animateChestUse(ServerWorld world, BlockPos pos) {
        if (pos == null) {
            return;
        }

        villager.swingHand(Hand.MAIN_HAND);
        villager.getLookControl().lookAt(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
        BlockState blockState = world.getBlockState(pos);
        if (blockState.isOf(Blocks.BARREL)) {
            openAnimatedContainer(world, pos, blockState);
            world.playSound(null, pos, SoundEvents.BLOCK_BARREL_OPEN, SoundCategory.BLOCKS, 0.6F, 1.0F);
        } else if (blockState.isOf(Blocks.CHEST) || blockState.isOf(Blocks.TRAPPED_CHEST)) {
            openAnimatedContainer(world, pos, blockState);
            world.playSound(null, pos, SoundEvents.BLOCK_CHEST_OPEN, SoundCategory.BLOCKS, 0.6F, 1.0F);
        }
    }

    private void openAnimatedContainer(ServerWorld world, BlockPos pos, BlockState blockState) {
        world.addSyncedBlockEvent(pos, blockState.getBlock(), 1, 1);
        animatedContainerPos = pos.toImmutable();
        animatedContainerBlock = blockState.getBlock();
        animatedContainerCloseTicks = 20;
    }

    private void tickAnimatedContainer(ServerWorld world) {
        if (animatedContainerCloseTicks <= 0 || animatedContainerPos == null || animatedContainerBlock == null) {
            return;
        }

        animatedContainerCloseTicks--;
        if (animatedContainerCloseTicks > 0) {
            return;
        }

        BlockState blockState = world.getBlockState(animatedContainerPos);
        if (blockState.isOf(animatedContainerBlock)) {
            world.addSyncedBlockEvent(animatedContainerPos, animatedContainerBlock, 1, 0);
            if (blockState.isOf(Blocks.BARREL)) {
                world.playSound(null, animatedContainerPos, SoundEvents.BLOCK_BARREL_CLOSE, SoundCategory.BLOCKS, 0.5F, 1.0F);
            } else {
                world.playSound(null, animatedContainerPos, SoundEvents.BLOCK_CHEST_CLOSE, SoundCategory.BLOCKS, 0.5F, 1.0F);
            }
        }
        closeAnimatedContainer();
    }

    private void closeAnimatedContainer() {
        animatedContainerPos = null;
        animatedContainerBlock = null;
        animatedContainerCloseTicks = 0;
    }

    private void closeAnimatedContainerNow() {
        if (animatedContainerPos != null
                && animatedContainerBlock != null
                && villager.getWorld() instanceof ServerWorld world
                && world.getBlockState(animatedContainerPos).isOf(animatedContainerBlock)) {
            world.addSyncedBlockEvent(animatedContainerPos, animatedContainerBlock, 1, 0);
        }
        closeAnimatedContainer();
    }

    private boolean matchesSelectedTarget(ServerWorld world, BlockPos pos, BlockState blockState) {
        ValetOrder order = ValetOrders.get(villager);
        if (order == ValetOrder.MINE_ORES) {
            ValetMineTarget target = ValetOrders.getMineTarget(villager);
            return target != null && target.matches(blockState);
        }
        if (order == ValetOrder.CHOP_WOOD) {
            ValetWoodTarget target = ValetOrders.getWoodTarget(villager);
            return target != null && target.matchesNaturalTree(world, pos);
        }
        return false;
    }

    private boolean isBonusResource(BlockState blockState) {
        return ORE_TAGS.stream().anyMatch(blockState::isIn) || blockState.isIn(BlockTags.LOGS);
    }

    private boolean isTreeCrownBlock(BlockState blockState) {
        return blockState.isIn(BlockTags.LEAVES)
                || blockState.isOf(Blocks.NETHER_WART_BLOCK)
                || blockState.isOf(Blocks.WARPED_WART_BLOCK);
    }

    private ItemStack getToolForBlock(BlockState blockState) {
        if (isTreeCrownBlock(blockState)) {
            return new ItemStack(Items.IRON_HOE);
        }
        if (blockState.isIn(BlockTags.AXE_MINEABLE)) {
            return new ItemStack(Items.IRON_AXE);
        }
        if (blockState.isIn(BlockTags.SHOVEL_MINEABLE)) {
            return new ItemStack(Items.IRON_SHOVEL);
        }
        return new ItemStack(Items.IRON_PICKAXE);
    }

    private BlockPos getWorkOrigin(ServerWorld world) {
        BlockPos workOrigin = getKnownWorkOrigin(world);
        if (workOrigin != null) {
            return workOrigin;
        }
        return hasActiveOrder() ? villager.getBlockPos() : null;
    }

    private BlockPos getKnownWorkOrigin(ServerWorld world) {
        return ValetHome.get(world, villager);
    }

    private boolean canStoreAllDrops(List<ItemStack> drops) {
        Inventory inventory = villager.getInventory();
        return ValetInventoryTransfer.canStoreAllDrops(inventory, getUsableInventorySlots(inventory), drops);
    }

    private void collectDrops(List<ItemStack> drops) {
        Inventory inventory = villager.getInventory();
        for (ItemStack drop : drops) {
            if (drop.isEmpty()) {
                continue;
            }

            ValetInventoryTransfer.insertStack(inventory, drop, getUsableInventorySlots(inventory));
            if (!drop.isEmpty()) {
                villager.dropStack(drop);
            }
        }
        inventory.markDirty();
    }

    private void collectNearbyItemEntities(ServerWorld world) {
        Box box = Box.from(villager.getPos()).expand(2.0D);
        for (ItemEntity itemEntity : world.getEntitiesByClass(ItemEntity.class, box, item -> !item.isRemoved())) {
            ItemStack stack = itemEntity.getStack();
            Inventory inventory = villager.getInventory();
            ValetInventoryTransfer.insertStack(inventory, stack, getUsableInventorySlots(inventory));
            if (stack.isEmpty()) {
                itemEntity.discard();
            }
        }
    }

    private boolean hasInventoryItems() {
        Inventory inventory = villager.getInventory();
        for (int slot = 0; slot < inventory.size(); slot++) {
            ItemStack stack = inventory.getStack(slot);
            if (!stack.isEmpty() && !stack.isOf(Items.ARROW)) {
                return true;
            }
        }
        return false;
    }

    private boolean isExecuting(PathPurpose purpose) {
        return (state == State.EXECUTING_PATH || state == State.MINING) && pathPurpose == purpose;
    }

    private State interruptedPathState() {
        return ValetStateMachine.interruptedPathState(pathPurpose, hasConstructionOrder(), hasMiningOrder(), hasCraftOrder(), hasInventorySpace(), hasInventoryItems());
    }

    private State interruptedWorkState() {
        return ValetStateMachine.interruptedWorkState(hasConstructionOrder(), hasMiningOrder(), hasCraftOrder(), hasInventorySpace(), hasInventoryItems());
    }

    private void holdConversationPosition() {
        villager.getNavigation().stop();
        villager.setVelocity(0.0D, villager.getVelocity().y, 0.0D);
    }

    private boolean shouldClaimMovement() {
        return hasActiveOrder() || hasInventoryItems() || state != State.IDLE;
    }

    private void suppressVanillaMovementTargets() {
        villager.getBrain().forget(MemoryModuleType.WALK_TARGET);
        villager.getBrain().forget(MemoryModuleType.LOOK_TARGET);
    }

    private boolean hasInventorySpace() {
        Inventory inventory = villager.getInventory();
        int usableSlots = getUsableInventorySlots(inventory);
        for (int slot = 0; slot < usableSlots; slot++) {
            ItemStack stack = inventory.getStack(slot);
            if (stack.isEmpty() || stack.getCount() < Math.min(stack.getMaxCount(), inventory.getMaxCountPerStack())) {
                return true;
            }
        }
        return false;
    }

    private int getUsableInventorySlots(Inventory inventory) {
        return settings.usableInventorySlots(inventory);
    }

    private int inventoryItemCount(Inventory inventory) {
        int count = 0;
        for (int slot = 0; slot < getUsableInventorySlots(inventory); slot++) {
            count += inventory.getStack(slot).getCount();
        }
        return count;
    }

    private void clearPathState() {
        path = List.of();
        pathIndex = 0;
        clearNavigationStep();
        villager.getNavigation().stop();
        miningTask.clearTarget();
        logisticsTask.clearChestTarget();
        craftingTask.clearTarget();
        clearBuildState();
    }

    private void clearMiningState() {
        miningTask.clearMiningState();
    }

    private void clearVeinState() {
        miningTask.clearVeinState();
    }

    private void clearBuildState() {
        constructionTask.clearBuildState();
    }

    private final class CombatControl implements CombatRuntimeTask.Control {
        @Override
        public VillagerEntity villager() {
            return villager;
        }

        @Override
        public BlockPos getWorkOrigin(ServerWorld world) {
            return ValetWorkGoal.this.getWorkOrigin(world);
        }

        @Override
        public boolean isDefenseEnabled() {
            return true;
        }

        @Override
        public double combatSearchRadius() {
            return settings.combatSearchRadius();
        }

        @Override
        public double combatChaseRadius() {
            return settings.combatChaseRadius();
        }

        @Override
        public double combatAttackRangeSquared() {
            return settings.combatAttackRangeSquared();
        }

        @Override
        public double combatRangedAttackRangeSquared() {
            return settings.combatRangedAttackRangeSquared();
        }

        @Override
        public double combatMoveSpeed() {
            return settings.combatMoveSpeed();
        }

        @Override
        public float combatAttackDamage() {
            return settings.combatAttackDamage();
        }

        @Override
        public int combatAttackCooldownTicks() {
            return settings.combatAttackCooldownTicks();
        }

        @Override
        public float combatArrowDamage() {
            return settings.combatArrowDamage();
        }

        @Override
        public int combatArrowCooldownTicks() {
            return settings.combatArrowCooldownTicks();
        }

        @Override
        public int combatArrowRestockCount() {
            return settings.combatArrowRestockCount();
        }

        @Override
        public boolean combatHasDefense() {
            return settings.combatHasDefense();
        }

        @Override
        public boolean combatCanRecycleArrow() {
            return settings.combatCanRecycleArrow();
        }

        @Override
        public int chestRadius() {
            return ValetWorkGoal.this.chestRadius();
        }

        @Override
        public int getUsableInventorySlots(Inventory inventory) {
            return ValetWorkGoal.this.getUsableInventorySlots(inventory);
        }

        @Override
        public void animateChestUse(ServerWorld world, BlockPos pos) {
            ValetWorkGoal.this.animateChestUse(world, pos);
        }

        @Override
        public void onCombatStarted(LivingEntity target) {
            clearPathState();
            clearMiningState();
            state = interruptedWorkState();
            delayTicks = 0;
            ValetDebug.record(villager, "combat target=" + ValetDebug.shortPos(target.getBlockPos()));
        }

        @Override
        public void onCombatFinished() {
            villager.getNavigation().stop();
            state = interruptedWorkState();
            delayTicks = 4;
        }
    }

    private final class LogisticsControl implements LogisticsRuntimeTask.Control {
        @Override
        public VillagerEntity villager() {
            return villager;
        }

        @Override
        public BlockPos getWorkOrigin(ServerWorld world) {
            return ValetWorkGoal.this.getWorkOrigin(world);
        }

        @Override
        public boolean hasMiningOrder() {
            return ValetWorkGoal.this.hasMiningOrder();
        }

        @Override
        public boolean hasConstructionOrder() {
            return ValetWorkGoal.this.hasConstructionOrder();
        }

        @Override
        public boolean hasCraftOrder() {
            return ValetWorkGoal.this.hasCraftOrder();
        }

        @Override
        public boolean hasInventorySpace() {
            return ValetWorkGoal.this.hasInventorySpace();
        }

        @Override
        public boolean hasInventoryItems() {
            return ValetWorkGoal.this.hasInventoryItems();
        }

        @Override
        public boolean isNearWorkstation(ServerWorld world, BlockPos workOrigin) {
            return ValetWorkGoal.this.isNearWorkstation(world, workOrigin);
        }

        @Override
        public Set<BlockPos> findStandGoals(ServerWorld world, BlockPos targetBlock, PathPurpose purpose) {
            return ValetWorkGoal.this.findStandGoals(world, targetBlock, purpose);
        }

        @Override
        public List<BlockPos> planPathToAdjacent(ServerWorld world, PathPurpose purpose, BlockPos targetBlock, Set<BlockPos> goals) {
            return ValetWorkGoal.this.planPathToAdjacent(world, purpose, targetBlock, goals);
        }

        @Override
        public void startPath(PathPurpose purpose, List<BlockPos> path) {
            ValetWorkGoal.this.startPath(purpose, path);
        }

        @Override
        public void clearPathState() {
            ValetWorkGoal.this.clearPathState();
        }

        @Override
        public void clearMiningState() {
            ValetWorkGoal.this.clearMiningState();
        }

        @Override
        public int chestRadius() {
            return ValetWorkGoal.this.chestRadius();
        }

        @Override
        public void animateChestUse(ServerWorld world, BlockPos pos) {
            ValetWorkGoal.this.animateChestUse(world, pos);
        }

        @Override
        public void setState(State nextState) {
            state = nextState;
        }

        @Override
        public void setDelayTicks(int ticks) {
            delayTicks = ticks;
        }
    }

    private final class ConstructionControl implements ConstructionRuntimeTask.Control {
        @Override
        public VillagerEntity villager() {
            return villager;
        }

        @Override
        public BlockPos getWorkOrigin(ServerWorld world) {
            return ValetWorkGoal.this.getWorkOrigin(world);
        }

        @Override
        public Set<BlockPos> findBuildStandGoals(ServerWorld world, BlockPos targetBlock) {
            return ValetWorkGoal.this.findBuildStandGoals(world, targetBlock);
        }

        @Override
        public List<BlockPos> planPathToAdjacent(ServerWorld world, PathPurpose purpose, BlockPos targetBlock, Set<BlockPos> goals) {
            return ValetWorkGoal.this.planPathToAdjacent(world, purpose, targetBlock, goals);
        }

        @Override
        public void startPath(PathPurpose purpose, List<BlockPos> path) {
            ValetWorkGoal.this.startPath(purpose, path);
        }

        @Override
        public boolean canReachBuildTargetFromStand(BlockPos targetBlock, BlockPos stand) {
            return ValetWorkGoal.this.canReachBuildTargetFromStand(targetBlock, stand);
        }

        @Override
        public boolean hasInventoryItems() {
            return ValetWorkGoal.this.hasInventoryItems();
        }

        @Override
        public int materialRadius() {
            return ValetWorkGoal.this.materialRadius();
        }

        @Override
        public int getUsableInventorySlots(Inventory inventory) {
            return ValetWorkGoal.this.getUsableInventorySlots(inventory);
        }

        @Override
        public int actionDelayTicks() {
            return ValetWorkGoal.this.actionDelayTicks();
        }

        @Override
        public void animateChestUse(ServerWorld world, BlockPos pos) {
            ValetWorkGoal.this.animateChestUse(world, pos);
        }

        @Override
        public void setState(State nextState) {
            state = nextState;
        }

        @Override
        public void setDelayTicks(int ticks) {
            delayTicks = ticks;
        }
    }

    private final class CraftingControl implements CraftingRuntimeTask.Control {
        @Override
        public VillagerEntity villager() {
            return villager;
        }

        @Override
        public ValetCraftTarget getCraftTarget() {
            return ValetOrders.getCraftTarget(villager);
        }

        @Override
        public BlockPos getWorkOrigin(ServerWorld world) {
            return ValetWorkGoal.this.getWorkOrigin(world);
        }

        @Override
        public BlockPos currentStandPos(ServerWorld world) {
            return ValetWorkGoal.this.currentStandPos(world);
        }

        @Override
        public Set<BlockPos> findStandGoals(ServerWorld world, BlockPos targetBlock, PathPurpose purpose) {
            return ValetWorkGoal.this.findStandGoals(world, targetBlock, purpose);
        }

        @Override
        public List<BlockPos> planPathToAdjacent(ServerWorld world, PathPurpose purpose, BlockPos targetBlock, Set<BlockPos> goals) {
            return ValetWorkGoal.this.planPathToAdjacent(world, purpose, targetBlock, goals);
        }

        @Override
        public void startPath(PathPurpose purpose, List<BlockPos> path) {
            ValetWorkGoal.this.startPath(purpose, path);
        }

        @Override
        public void clearPathState() {
            ValetWorkGoal.this.clearPathState();
        }

        @Override
        public boolean canReachTargetFromStand(BlockPos targetBlock, BlockPos stand) {
            return ValetWorkGoal.this.canReachTargetFromStand(targetBlock, stand);
        }

        @Override
        public boolean canMineCraftResource(ServerWorld world, BlockPos pos, BlockState blockState) {
            return ValetWorkGoal.this.canMineCraftResource(world, pos, blockState);
        }

        @Override
        public ItemStack getToolForBlock(BlockState blockState) {
            return ValetWorkGoal.this.getToolForBlock(blockState);
        }

        @Override
        public boolean canStoreAllDrops(List<ItemStack> drops) {
            return ValetWorkGoal.this.canStoreAllDrops(drops);
        }

        @Override
        public void collectDrops(List<ItemStack> drops) {
            ValetWorkGoal.this.collectDrops(drops);
        }

        @Override
        public void animateMining(ServerWorld world, BlockPos miningPos, BlockState miningState) {
            ValetWorkGoal.this.animateMining(world, miningPos, miningState);
        }

        @Override
        public boolean hasInventoryItems() {
            return ValetWorkGoal.this.hasInventoryItems();
        }

        @Override
        public boolean hasInventorySpace() {
            return ValetWorkGoal.this.hasInventorySpace();
        }

        @Override
        public int getUsableInventorySlots(Inventory inventory) {
            return ValetWorkGoal.this.getUsableInventorySlots(inventory);
        }

        @Override
        public int actionDelayTicks() {
            return ValetWorkGoal.this.actionDelayTicks();
        }

        @Override
        public int noTargetDelayTicks() {
            return settings.noTargetDelayTicks();
        }

        @Override
        public int mineRadius() {
            return ValetWorkGoal.this.mineRadius();
        }

        @Override
        public int mineVerticalRadius() {
            return ValetWorkGoal.this.mineVerticalRadius();
        }

        @Override
        public int materialRadius() {
            return ValetWorkGoal.this.materialRadius();
        }

        @Override
        public void animateChestUse(ServerWorld world, BlockPos pos) {
            ValetWorkGoal.this.animateChestUse(world, pos);
        }

        @Override
        public void setState(State nextState) {
            state = nextState;
        }

        @Override
        public void setDelayTicks(int ticks) {
            delayTicks = ticks;
        }
    }

    private final class MiningControl implements MiningRuntimeTask.Control {
        @Override
        public VillagerEntity villager() {
            return villager;
        }

        @Override
        public PathPurpose currentPathPurpose() {
            return pathPurpose;
        }

        @Override
        public BlockPos getWorkOrigin(ServerWorld world) {
            return ValetWorkGoal.this.getWorkOrigin(world);
        }

        @Override
        public boolean hasMiningOrder() {
            return ValetWorkGoal.this.hasMiningOrder();
        }

        @Override
        public boolean hasInventorySpace() {
            return ValetWorkGoal.this.hasInventorySpace();
        }

        @Override
        public boolean hasInventoryItems() {
            return ValetWorkGoal.this.hasInventoryItems();
        }

        @Override
        public boolean matchesSelectedTarget(ServerWorld world, BlockPos pos, BlockState blockState) {
            return ValetWorkGoal.this.matchesSelectedTarget(world, pos, blockState);
        }

        @Override
        public boolean isBonusResource(BlockState blockState) {
            return ValetWorkGoal.this.isBonusResource(blockState);
        }

        @Override
        public Set<BlockPos> findStandGoals(ServerWorld world, BlockPos targetBlock, PathPurpose purpose) {
            return ValetWorkGoal.this.findStandGoals(world, targetBlock, purpose);
        }

        @Override
        public List<BlockPos> planPathToAdjacent(ServerWorld world, PathPurpose purpose, BlockPos targetBlock, Set<BlockPos> goals) {
            return ValetWorkGoal.this.planPathToAdjacent(world, purpose, targetBlock, goals);
        }

        @Override
        public void startPath(PathPurpose purpose, List<BlockPos> path) {
            ValetWorkGoal.this.startPath(purpose, path);
        }

        @Override
        public void clearPathState() {
            ValetWorkGoal.this.clearPathState();
        }

        @Override
        public boolean canReachTargetFromStand(BlockPos targetBlock, BlockPos stand) {
            return ValetWorkGoal.this.canReachTargetFromStand(targetBlock, stand);
        }

        @Override
        public boolean canMineWorkBlock(ServerWorld world, BlockPos pos, BlockState blockState) {
            return ValetWorkGoal.this.canMineWorkBlock(world, pos, blockState);
        }

        @Override
        public ItemStack getToolForBlock(BlockState blockState) {
            return ValetWorkGoal.this.getToolForBlock(blockState);
        }

        @Override
        public boolean canStoreAllDrops(List<ItemStack> drops) {
            return ValetWorkGoal.this.canStoreAllDrops(drops);
        }

        @Override
        public void collectDrops(List<ItemStack> drops) {
            ValetWorkGoal.this.collectDrops(drops);
        }

        @Override
        public void collectNearbyItemEntities(ServerWorld world) {
            ValetWorkGoal.this.collectNearbyItemEntities(world);
        }

        @Override
        public void placeTorchIfNeeded(ServerWorld world, BlockPos minedPos) {
            ValetWorkGoal.this.placeTorchIfNeeded(world, minedPos);
        }

        @Override
        public void animateMining(ServerWorld world, BlockPos miningPos, BlockState miningState) {
            ValetWorkGoal.this.animateMining(world, miningPos, miningState);
        }

        @Override
        public State interruptedPathState() {
            return ValetWorkGoal.this.interruptedPathState();
        }

        @Override
        public State interruptedWorkState() {
            return ValetWorkGoal.this.interruptedWorkState();
        }

        @Override
        public void setState(State nextState) {
            state = nextState;
        }

        @Override
        public void setDelayTicks(int ticks) {
            delayTicks = ticks;
        }

        @Override
        public int noTargetDelayTicks() {
            return settings.noTargetDelayTicks();
        }

        @Override
        public int mineRadius() {
            return ValetWorkGoal.this.mineRadius();
        }

        @Override
        public int mineVerticalRadius() {
            return ValetWorkGoal.this.mineVerticalRadius();
        }

        @Override
        public int maxVeinBlocks() {
            return ValetWorkGoal.this.maxVeinBlocks();
        }

        @Override
        public int actionDelayTicks() {
            return ValetWorkGoal.this.actionDelayTicks();
        }
    }

    private static double squaredDistance(BlockPos first, BlockPos second) {
        int dx = first.getX() - second.getX();
        int dy = first.getY() - second.getY();
        int dz = first.getZ() - second.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

}
