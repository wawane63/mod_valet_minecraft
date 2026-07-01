package com.wawane.valet.ai;

import com.wawane.valet.ValetMod;
import com.wawane.valet.ValetRole;
import com.wawane.valet.ValetConversations;
import com.wawane.valet.ValetDebug;
import com.wawane.valet.ValetHome;
import com.wawane.valet.ai.ValetStateMachine.PathPurpose;
import com.wawane.valet.ai.ValetStateMachine.State;
import com.wawane.valet.ai.core.ValetBlockReservations;
import com.wawane.valet.ai.core.ValetOrderKey;
import com.wawane.valet.ai.core.ValetWorkSettings;
import com.wawane.valet.ai.inventory.ValetInventoryTransfer;
import com.wawane.valet.ai.path.ValetPathPlanner;
import com.wawane.valet.ai.tasks.ConstructionRuntimeTask;
import com.wawane.valet.ai.tasks.FarmingRuntimeTask;
import com.wawane.valet.ai.tasks.LogisticsRuntimeTask;
import com.wawane.valet.ai.tasks.MiningRuntimeTask;
import com.wawane.valet.ai.tasks.combat.CombatRuntimeTask;
import com.wawane.valet.ai.tasks.crafting.CraftingRuntimeTask;
import com.wawane.valet.farm.ValetFarmArea;
import com.wawane.valet.farm.ValetFarmStorage;
import com.wawane.valet.order.ValetCraftTarget;
import com.wawane.valet.order.ValetMineTarget;
import com.wawane.valet.order.ValetOrder;
import com.wawane.valet.order.ValetOrders;
import com.wawane.valet.order.ValetWoodTarget;
import com.wawane.valet.state.ValetBehavior;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;

public class ValetWorkGoal extends Goal {
    private static final Set<UUID> RESTART_REQUESTS = ConcurrentHashMap.newKeySet();
    private static final int PASSAGE_HEIGHT = 2;
    private static final int BUILD_HORIZONTAL_REACH = 4;
    private static final int BUILD_VERTICAL_REACH = 6;

    private static final Set<TagKey<Block>> ORE_TAGS = Set.of(
            ValetMineTarget.COAL.tag(),
            ValetMineTarget.COPPER.tag(),
            ValetMineTarget.IRON.tag(),
            ValetMineTarget.GOLD.tag(),
            ValetMineTarget.REDSTONE.tag(),
            ValetMineTarget.LAPIS.tag(),
            ValetMineTarget.EMERALD.tag(),
            ValetMineTarget.DIAMOND.tag()
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

    private final Villager villager;
    private final ValetWorkSettings settings;
    private final ValetPathPlanner pathPlanner = new ValetPathPlanner();
    private final MiningRuntimeTask miningTask;
    private final FarmingRuntimeTask farmingTask;
    private final ConstructionRuntimeTask constructionTask;
    private final LogisticsRuntimeTask logisticsTask;
    private final CombatRuntimeTask combatTask;
    private final CraftingRuntimeTask craftingTask;
    private State state = State.FIND_TARGET;
    private PathPurpose pathPurpose = PathPurpose.ORE;
    private List<BlockPos> path = List.of();
    private int pathIndex;
    private int delayTicks;
    private String activeOrderKey = "";
    private BlockPos animatedContainerPos;
    private Block animatedContainerBlock;
    private int animatedContainerCloseTicks;

    public ValetWorkGoal(Villager villager) {
        this.villager = villager;
        this.settings = new ValetWorkSettings(villager);
        this.miningTask = new MiningRuntimeTask(new MiningControl());
        this.farmingTask = new FarmingRuntimeTask(new FarmingControl());
        this.constructionTask = new ConstructionRuntimeTask(new ConstructionControl());
        this.logisticsTask = new LogisticsRuntimeTask(new LogisticsControl());
        this.combatTask = new CombatRuntimeTask(new CombatControl());
        this.craftingTask = new CraftingRuntimeTask(new CraftingControl());
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    public static void requestRestart(Villager villager) {
        RESTART_REQUESTS.add(villager.getUUID());
    }

    public static void clearRestartRequest(UUID uuid) {
        RESTART_REQUESTS.remove(uuid);
    }

    @Override
    public boolean canUse() {
        return isAvailableValet() && shouldRunCustomControl();
    }

    @Override
    public boolean canContinueToUse() {
        return isAvailableValet() && shouldRunCustomControl();
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
        farmingTask.clearAll();
        ValetBlockReservations.releaseAll(villager.getUUID());
        closeAnimatedContainerNow();
        villager.getNavigation().stop();
        if (villager.level() instanceof ServerLevel world && ValetBehavior.shouldUseVanillaBehavior(world, villager)) {
            villager.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        }
    }

    @Override
    public void tick() {
        if (!(villager.level() instanceof ServerLevel world)) {
            return;
        }

        tickAnimatedContainer(world);
        constructionTask.tickCooldown();
        miningTask.tickCooldown();
        farmingTask.tickCooldown();
        sanitizeOrderForRole(world);
        String currentOrderKey = currentOrderKey();
        if (RESTART_REQUESTS.remove(villager.getUUID()) || !currentOrderKey.equals(activeOrderKey)) {
            resetForCurrentOrder("restarts");
        }

        if (ValetConversations.isTalking(villager)) {
            updateDisplayedMainHand();
            suppressVanillaMovementTargets();
            holdConversationPosition();
            return;
        }

        if (shouldClaimMovement(world)) {
            suppressVanillaMovementTargets();
        }

        if (escapeFluidIfNeeded(world)) {
            return;
        }

        if (combatTask.tick(world)) {
            return;
        }

        updateDisplayedMainHand();

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
            case HARVESTING -> farmingTask.tickHarvesting(world);
            case PLACING -> constructionTask.tickPlacing(world);
            case CRAFTING -> craftingTask.tickCrafting(world);
            case COLLECTING -> miningTask.tickCollecting(world);
            case RETURNING -> logisticsTask.returnToChest(world);
            case RETURNING_HOME -> logisticsTask.returnToWorkstation(world);
            case DEPOSITING -> logisticsTask.tickDepositing(world);
        }
    }

    public String debugSummary() {
        Container inventory = villager.getInventory();
        return "state=" + state
                + " role=" + currentRoleName()
                + " order=" + currentOrderKey()
                + " pos=" + ValetDebug.shortPos(villager.blockPosition())
                + " inv=" + inventoryItemCount(inventory) + "/" + getUsableInventorySlots(inventory)
                + " delay=" + delayTicks
                + " path=" + pathPurpose + ":" + pathIndex + "/" + path.size()
                + " " + miningTask.debugSummary()
                + " " + farmingTask.debugSummary()
                + " " + constructionTask.debugSummary()
                + " " + craftingTask.debugSummary()
                + " " + logisticsTask.debugSummary()
                + " " + combatTask.debugSummary();
    }

    private boolean isAvailableValet() {
        return !villager.level().isClientSide()
                && !villager.isBaby()
                && !villager.isSleeping()
                && ValetMod.isValet(villager);
    }

    private boolean hasMiningOrder() {
        ValetOrder order = ValetOrders.get(villager);
        return isOrderAllowed(order)
                && (order == ValetOrder.MINE_ORES && ValetOrders.getMineTarget(villager) != null
                || order == ValetOrder.CHOP_WOOD && ValetOrders.getWoodTarget(villager) != null);
    }

    private boolean hasConstructionOrder() {
        return isOrderAllowed(ValetOrders.get(villager))
                && ValetOrders.get(villager) == ValetOrder.BUILD_STRUCTURE
                && ValetOrders.getConstructionTargetId(villager) >= 0;
    }

    private boolean hasFarmOrder() {
        return isOrderAllowed(ValetOrders.get(villager)) && ValetOrders.get(villager) == ValetOrder.HARVEST_CROPS;
    }

    private boolean hasCraftOrder() {
        return isOrderAllowed(ValetOrders.get(villager))
                && ValetOrders.get(villager) == ValetOrder.CRAFT
                && ValetOrders.getCraftTarget(villager) != null;
    }

    private boolean hasActiveOrder() {
        return hasMiningOrder() || hasFarmOrder() || hasConstructionOrder() || hasCraftOrder();
    }

    private boolean hasWorkOrigin() {
        return villager.level() instanceof ServerLevel world && getKnownWorkOrigin(world) != null;
    }

    private boolean shouldRunCustomControl() {
        if (!(villager.level() instanceof ServerLevel world)) {
            return false;
        }
        if (ValetConversations.isTalking(villager)) {
            return true;
        }
        if (ValetBehavior.shouldUseVanillaBehavior(world, villager)) {
            return false;
        }
        return hasWorkOrigin() || hasActiveOrder() || hasInventoryItems();
    }

    private State chooseStartState() {
        if (villager.level() instanceof ServerLevel world && ValetBehavior.isRecallActive(world, villager)) {
            return State.RETURNING_HOME;
        }
        return ValetStateMachine.chooseStartState(ValetConversations.isTalking(villager), hasActiveOrder(), shouldReturnToChestBeforeWork());
    }

    private void resetForCurrentOrder(String action) {
        state = chooseStartState();
        delayTicks = 0;
        activeOrderKey = currentOrderKey();
        closeAnimatedContainerNow();
        clearPathState();
        miningTask.clearAll();
        farmingTask.clearAll();
        ValetMod.LOGGER.info("Valet {} goal {} order {}", villager.getUUID(), action, activeOrderKey);
        ValetDebug.record(villager, "reset=" + action + " order=" + activeOrderKey + " state=" + state);
    }

    private String currentOrderKey() {
        return ValetOrderKey.of(villager);
    }

    private boolean isOrderAllowed(ValetOrder order) {
        return villager.level() instanceof ServerLevel world && ValetRole.get(world, villager).allows(order);
    }

    private void sanitizeOrderForRole(ServerLevel world) {
        ValetOrder order = ValetOrders.get(villager);
        ValetRole role = ValetRole.get(world, villager);
        if (role.allows(order)) {
            return;
        }

        ValetDebug.record(villager, "order_cleared role=" + role.name().toLowerCase() + " order=" + order.name().toLowerCase());
        ValetOrders.set(villager, ValetOrder.NONE);
        clearPathState();
        miningTask.clearAll();
        farmingTask.clearAll();
        constructionTask.clearBuildState();
        craftingTask.clearTarget();
        state = chooseStartState();
        activeOrderKey = currentOrderKey();
    }

    private String currentRoleName() {
        if (!(villager.level() instanceof ServerLevel world)) {
            return "-";
        }
        return ValetRole.get(world, villager).name().toLowerCase();
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
        if (hasFarmOrder() && villager.level() instanceof ServerLevel world) {
            if (!hasInventorySpace()) {
                return true;
            }
            BlockPos workOrigin = getKnownWorkOrigin(world);
            return workOrigin != null && squaredDistance(villager.blockPosition(), workOrigin) > 64;
        }
        if (hasMiningOrder() && villager.level() instanceof ServerLevel world) {
            if (!hasInventorySpace()) {
                return true;
            }
            BlockPos workOrigin = getKnownWorkOrigin(world);
            return workOrigin != null && squaredDistance(villager.blockPosition(), workOrigin) > 64;
        }
        return hasInventoryItems();
    }

    private void updatePassiveState() {
        if (villager.level() instanceof ServerLevel world && ValetBehavior.isRecallActive(world, villager)) {
            if (state != State.RETURNING_HOME && !isExecuting(PathPurpose.HOME)) {
                clearPathState();
                clearMiningState();
                clearVeinState();
                state = State.RETURNING_HOME;
            }
            return;
        }

        if (hasMiningOrder()) {
            if (shouldPreemptForMiningOrder()) {
                clearPathState();
                clearMiningState();
                clearVeinState();
                state = hasInventorySpace() ? State.FIND_TARGET : State.RETURNING;
            }
            return;
        }

        if (hasFarmOrder()) {
            if (shouldPreemptForFarmOrder()) {
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
        if (!hasInventoryItems() && hasInventorySpace() && (state == State.RETURNING || state == State.DEPOSITING || isExecuting(PathPurpose.CHEST))) {
            return true;
        }
        return false;
    }

    private boolean shouldPreemptForFarmOrder() {
        if (state == State.IDLE || state == State.RETURNING_HOME || isExecuting(PathPurpose.HOME)) {
            return true;
        }
        if (!hasInventoryItems() && hasInventorySpace() && (state == State.RETURNING || state == State.DEPOSITING || isExecuting(PathPurpose.CHEST))) {
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
        if (state == State.RETURNING_HOME || isExecuting(PathPurpose.HOME)) {
            return !hasInventoryItems();
        }
        return state == State.IDLE;
    }

    private void findTarget(ServerLevel world) {
        if (hasConstructionOrder()) {
            constructionTask.findTarget(world);
            return;
        }

        if (hasCraftOrder()) {
            craftingTask.findTarget(world);
            return;
        }

        if (hasFarmOrder()) {
            farmingTask.findTarget(world);
            return;
        }

        miningTask.findTarget(world);
    }

    private void executePath(ServerLevel world) {
        if (pathIndex >= path.size()) {
            if (pathPurpose == PathPurpose.ORE) {
                miningTask.completePath(world);
            } else if (pathPurpose == PathPurpose.CROP) {
                farmingTask.completePath(world);
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

    private List<BlockPos> planPathToAdjacent(ServerLevel world, PathPurpose purpose, BlockPos targetBlock, Set<BlockPos> goals) {
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
        suppressVanillaMovementTargets();
        double targetX = step.getX() + 0.5D;
        double targetZ = step.getZ() + 0.5D;
        faceTarget(targetX, targetZ);
        villager.getLookControl().setLookAt(targetX, step.getY() + 1.0D, targetZ);
        villager.teleportTo(targetX, step.getY(), targetZ);
        villager.setDeltaMovement(0.0D, villager.getDeltaMovement().y, 0.0D);
        ValetDebug.record(villager, "path step purpose=" + pathPurpose + " step=" + ValetDebug.shortPos(step));
    }

    private void faceTarget(double targetX, double targetZ) {
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

    private void clearNavigationStep() {
        villager.getNavigation().stop();
    }

    private Set<BlockPos> findStandGoals(ServerLevel world, BlockPos targetBlock, PathPurpose purpose) {
        Set<BlockPos> goals = new HashSet<>();

        for (BlockPos pos : BlockPos.betweenClosed(
                targetBlock.getX() - 2,
                targetBlock.getY() - 2,
                targetBlock.getZ() - 2,
                targetBlock.getX() + 2,
                targetBlock.getY() + 2,
                targetBlock.getZ() + 2
        )) {
            BlockPos stand = pos.immutable();
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
        return (purpose == PathPurpose.ORE || purpose == PathPurpose.CRAFT) && stand.below().equals(targetBlock);
    }

    private Set<BlockPos> findBuildStandGoals(ServerLevel world, BlockPos targetBlock) {
        Set<BlockPos> goals = new HashSet<>();

        for (BlockPos pos : BlockPos.betweenClosed(
                targetBlock.getX() - BUILD_HORIZONTAL_REACH,
                targetBlock.getY() - BUILD_VERTICAL_REACH,
                targetBlock.getZ() - BUILD_HORIZONTAL_REACH,
                targetBlock.getX() + BUILD_HORIZONTAL_REACH,
                targetBlock.getY(),
                targetBlock.getZ() + BUILD_HORIZONTAL_REACH
        )) {
            BlockPos stand = pos.immutable();
            if (!stand.equals(targetBlock) && canReachBuildTargetFromStand(targetBlock, stand) && canPrepareStand(world, stand, PathPurpose.BUILD)) {
                goals.add(stand);
            }
        }

        return goals;
    }

    private boolean isNearWorkstation(ServerLevel world, BlockPos workOrigin) {
        Set<BlockPos> goals = findStandGoals(world, workOrigin, PathPurpose.HOME);
        BlockPos current = currentStandPos(world);
        return goals.contains(current) || squaredDistance(current, workOrigin) <= 4;
    }

    private boolean canReachTargetFromStand(BlockPos targetBlock, BlockPos stand) {
        return isFaceAdjacent(targetBlock, stand) || isFaceAdjacent(targetBlock, stand.above());
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

    private int movementCost(ServerLevel world, BlockPos from, BlockPos to, PathPurpose purpose) {
        int cost = 10 + Math.abs(to.getY() - from.getY()) * 6;
        for (BlockPos pos : movementClearancePositions(from, to)) {
            cost += clearCost(world, pos, purpose);
        }
        return cost;
    }

    private int clearCost(ServerLevel world, BlockPos pos, PathPurpose purpose) {
        BlockState blockState = world.getBlockState(pos);
        if (isPassableTunnelSpace(world, pos)) {
            return 0;
        }
        if (!canMinePathBlock(world, pos, blockState, purpose)) {
            return 10_000;
        }
        return 80 + Math.max(0, Math.round(blockState.getDestroySpeed(world, pos) * 20.0F));
    }

    private boolean canPrepareStand(ServerLevel world, BlockPos standPos, PathPurpose purpose) {
        if (!canStandOn(world, standPos.below())) {
            return false;
        }

        for (BlockPos pos : standClearancePositions(standPos)) {
            if (!canClearForTunnel(world, pos, purpose)) {
                return false;
            }
        }
        return true;
    }

    private boolean canPrepareStep(ServerLevel world, BlockPos from, BlockPos to, PathPurpose purpose) {
        int dx = Math.abs(to.getX() - from.getX());
        int dy = to.getY() - from.getY();
        int dz = Math.abs(to.getZ() - from.getZ());
        if (dx + dz != 1 || dy < -1 || dy > 1 || !canStandOn(world, to.below())) {
            return false;
        }

        for (BlockPos pos : movementClearancePositions(from, to)) {
            if (!canClearForTunnel(world, pos, purpose)) {
                return false;
            }
        }
        return true;
    }

    private boolean canClearForTunnel(ServerLevel world, BlockPos pos, PathPurpose purpose) {
        BlockState blockState = world.getBlockState(pos);
        return isPassableTunnelSpace(world, pos) || canMinePathBlock(world, pos, blockState, purpose);
    }

    private BlockPos findMovementObstruction(ServerLevel world, BlockPos from, BlockPos to) {
        for (BlockPos candidate : movementClearancePositions(from, to)) {
            BlockState blockState = world.getBlockState(candidate);
            if (!isPassableTunnelSpace(world, candidate)) {
                return canMinePathBlock(world, candidate, blockState, pathPurpose) ? candidate : null;
            }
        }

        return null;
    }

    private boolean canTraverseStep(ServerLevel world, BlockPos from, BlockPos to) {
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

    private String describeBlockedStep(ServerLevel world, BlockPos from, BlockPos to) {
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
                return ValetDebug.shortPos(pos) + ":" + state.getBlock().getDescriptionId();
            }
        }
        return "unknown";
    }

    private boolean isSafeStand(ServerLevel world, BlockPos standPos) {
        if (!canStandOn(world, standPos.below())) {
            return false;
        }

        for (BlockPos pos : standClearancePositions(standPos)) {
            if (!isPassableTunnelSpace(world, pos)) {
                return false;
            }
        }
        return true;
    }

    private boolean escapeFluidIfNeeded(ServerLevel world) {
        BlockPos current = villager.blockPosition();
        if (world.getBlockState(current).getFluidState().isEmpty()
                && world.getBlockState(current.above()).getFluidState().isEmpty()) {
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
        villager.teleportTo(safeStand.getX() + 0.5D, safeStand.getY(), safeStand.getZ() + 0.5D);
        villager.setDeltaMovement(0.0D, 0.0D, 0.0D);
        state = hasConstructionOrder() && !hasInventoryItems() ? State.RETURNING_HOME : State.RETURNING;
        delayTicks = 2;
        ValetDebug.record(villager, "water_escape to=" + ValetDebug.shortPos(safeStand) + " state=" + state);
        return true;
    }

    private BlockPos findNearestSafeStand(ServerLevel world, BlockPos origin, int horizontalRadius, int verticalRadius) {
        BlockPos nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.withinManhattan(origin, horizontalRadius, verticalRadius, horizontalRadius)) {
            BlockPos stand = pos.immutable();
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
            positions.add(standPos.above(y));
        }
        return positions;
    }

    private List<BlockPos> movementClearancePositions(BlockPos from, BlockPos to) {
        return standClearancePositions(to);
    }

    private boolean isPassableTunnelSpace(ServerLevel world, BlockPos pos) {
        BlockState blockState = world.getBlockState(pos);
        return blockState.isAir()
                || blockState.is(Blocks.SNOW)
                || isDoorPassage(blockState)
                || blockState.is(Blocks.TORCH)
                || blockState.is(Blocks.WALL_TORCH)
                || blockState.getFluidState().isEmpty() && blockState.getCollisionShape(world, pos).isEmpty();
    }

    private boolean isDoorPassage(BlockState blockState) {
        return blockState.getBlock() instanceof DoorBlock && blockState.getFluidState().isEmpty();
    }

    private void openDoorsForStep(ServerLevel world, BlockPos from, BlockPos to) {
        for (BlockPos pos : movementClearancePositions(from, to)) {
            openDoorAt(world, pos);
        }
    }

    private void openDoorAt(ServerLevel world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof DoorBlock) || !state.hasProperty(DoorBlock.OPEN)) {
            return;
        }

        BlockPos lowerPos = state.hasProperty(DoorBlock.HALF) && state.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
        BlockState lowerState = world.getBlockState(lowerPos);
        BlockState upperState = world.getBlockState(lowerPos.above());
        boolean opened = false;
        if (lowerState.getBlock() instanceof DoorBlock && lowerState.hasProperty(DoorBlock.OPEN) && !lowerState.getValue(DoorBlock.OPEN)) {
            world.setBlock(lowerPos, lowerState.setValue(DoorBlock.OPEN, true), Block.UPDATE_ALL);
            opened = true;
        }
        if (upperState.getBlock() instanceof DoorBlock && upperState.hasProperty(DoorBlock.OPEN) && !upperState.getValue(DoorBlock.OPEN)) {
            world.setBlock(lowerPos.above(), upperState.setValue(DoorBlock.OPEN, true), Block.UPDATE_ALL);
            opened = true;
        }
        if (opened) {
            world.playSound(null, lowerPos, SoundEvents.WOODEN_DOOR_OPEN, SoundSource.BLOCKS, 0.7F, 1.0F);
            ValetDebug.record(villager, "path door_open pos=" + ValetDebug.shortPos(lowerPos));
        }
    }

    private boolean canStandOn(ServerLevel world, BlockPos pos) {
        BlockState blockState = world.getBlockState(pos);
        if (blockState.is(Blocks.DIRT_PATH) || blockState.is(Blocks.FARMLAND)) {
            return blockState.getFluidState().isEmpty();
        }
        if (blockState.getBlock() instanceof StairBlock) {
            return blockState.getFluidState().isEmpty();
        }
        return !isPassableTunnelSpace(world, pos)
                && blockState.getFluidState().isEmpty()
                && blockState.getDestroySpeed(world, pos) >= 0.0F
                && blockState.isFaceSturdy(world, pos, Direction.UP);
    }

    private BlockPos currentStandPos(ServerLevel world) {
        BlockPos current = villager.blockPosition();
        if (canStandOn(world, current) && isPassableTunnelSpace(world, current.above())) {
            return current.above();
        }
        return current;
    }

    private boolean canMinePathBlock(ServerLevel world, BlockPos pos, BlockState blockState, PathPurpose purpose) {
        if (purpose == PathPurpose.CROP) {
            return false;
        }
        if (purpose == PathPurpose.CHEST || purpose == PathPurpose.HOME || purpose == PathPurpose.CRAFT) {
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

    private boolean canMineWorkBlock(ServerLevel world, BlockPos pos, BlockState blockState) {
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
                || ORE_TAGS.stream().anyMatch(blockState::is)
                || NATURAL_PATH_BLOCKS.contains(block);
    }

    private boolean canMineNaturalPathBlock(ServerLevel world, BlockPos pos, BlockState blockState) {
        if (!canMineBaseBlock(world, pos, blockState) || hasFallingBlockDirectlyAbove(world, pos)) {
            return false;
        }
        return NATURAL_PATH_BLOCKS.contains(blockState.getBlock());
    }

    private boolean canMineCraftResource(ServerLevel world, BlockPos pos, BlockState blockState) {
        return canMineBaseBlock(world, pos, blockState) && !hasFallingBlockDirectlyAbove(world, pos);
    }

    private boolean canMineBaseBlock(ServerLevel world, BlockPos pos, BlockState blockState) {
        return !blockState.isAir()
                && blockState.getFluidState().isEmpty()
                && !ValetBlockReservations.isClaimedByOther(world, villager.getUUID(), pos)
                && !wouldExposeFluid(world, pos)
                && !ValetMod.isValetWorkstation(blockState)
                && !blockState.is(ValetMod.CONSTRUCTION_BEACON)
                && !blockState.is(ValetMod.CONSTRUCTION_BLUEPRINT)
                && !blockState.is(ValetMod.FARM_BEACON)
                && !blockState.is(Blocks.CHEST)
                && !blockState.is(Blocks.TRAPPED_CHEST)
                && !blockState.is(Blocks.BARREL)
                && blockState.getDestroySpeed(world, pos) >= 0.0F;
    }

    private boolean wouldExposeFluid(ServerLevel world, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            if (!world.getFluidState(pos.relative(direction)).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private boolean hasFallingBlockDirectlyAbove(ServerLevel world, BlockPos pos) {
        return FALLING_PATH_BLOCKS.contains(world.getBlockState(pos.above()).getBlock());
    }

    private boolean isSelectedResource(ServerLevel world, BlockPos pos, BlockState blockState) {
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

    private void placeTorchIfNeeded(ServerLevel world, BlockPos minedPos) {
        BlockPos origin = villager.blockPosition();
        if (world.getBrightness(LightLayer.BLOCK, origin) > torchLightThreshold()) {
            return;
        }

        List<BlockPos> candidates = new ArrayList<>();
        if (minedPos != null) {
            candidates.add(minedPos);
            candidates.add(minedPos.below());
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

    private boolean tryPlaceTorch(ServerLevel world, BlockPos pos) {
        if (!world.getBlockState(pos).isAir()) {
            return false;
        }

        BlockState torchState = Blocks.TORCH.defaultBlockState();
        if (!torchState.canSurvive(world, pos)) {
            return false;
        }

        world.setBlock(pos, torchState, Block.UPDATE_ALL);
        world.playSound(null, pos, torchState.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 0.7F, 1.0F);
        return true;
    }

    private void animateMining(ServerLevel world, BlockPos miningPos, BlockState miningState) {
        villager.swing(InteractionHand.MAIN_HAND);
        villager.getLookControl().setLookAt(miningPos.getX() + 0.5D, miningPos.getY() + 0.5D, miningPos.getZ() + 0.5D);
        world.levelEvent(2001, miningPos, Block.getId(miningState));
        world.playSound(null, miningPos, miningState.getSoundType().getBreakSound(), SoundSource.BLOCKS, 0.75F, 1.0F);
    }

    private void animateChestUse(ServerLevel world, BlockPos pos) {
        if (pos == null) {
            return;
        }

        villager.swing(InteractionHand.MAIN_HAND);
        villager.getLookControl().setLookAt(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
        BlockState blockState = world.getBlockState(pos);
        if (blockState.is(Blocks.BARREL)) {
            openAnimatedContainer(world, pos, blockState);
            world.playSound(null, pos, SoundEvents.BARREL_OPEN, SoundSource.BLOCKS, 0.6F, 1.0F);
        } else if (blockState.is(Blocks.CHEST) || blockState.is(Blocks.TRAPPED_CHEST)) {
            openAnimatedContainer(world, pos, blockState);
            world.playSound(null, pos, SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 0.6F, 1.0F);
        }
    }

    private void openAnimatedContainer(ServerLevel world, BlockPos pos, BlockState blockState) {
        world.blockEvent(pos, blockState.getBlock(), 1, 1);
        animatedContainerPos = pos.immutable();
        animatedContainerBlock = blockState.getBlock();
        animatedContainerCloseTicks = 20;
    }

    private void tickAnimatedContainer(ServerLevel world) {
        if (animatedContainerCloseTicks <= 0 || animatedContainerPos == null || animatedContainerBlock == null) {
            return;
        }

        animatedContainerCloseTicks--;
        if (animatedContainerCloseTicks > 0) {
            return;
        }

        BlockState blockState = world.getBlockState(animatedContainerPos);
        if (blockState.is(animatedContainerBlock)) {
            world.blockEvent(animatedContainerPos, animatedContainerBlock, 1, 0);
            if (blockState.is(Blocks.BARREL)) {
                world.playSound(null, animatedContainerPos, SoundEvents.BARREL_CLOSE, SoundSource.BLOCKS, 0.5F, 1.0F);
            } else {
                world.playSound(null, animatedContainerPos, SoundEvents.CHEST_CLOSE, SoundSource.BLOCKS, 0.5F, 1.0F);
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
                && villager.level() instanceof ServerLevel world
                && world.getBlockState(animatedContainerPos).is(animatedContainerBlock)) {
            world.blockEvent(animatedContainerPos, animatedContainerBlock, 1, 0);
        }
        closeAnimatedContainer();
    }

    private boolean matchesSelectedTarget(ServerLevel world, BlockPos pos, BlockState blockState) {
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

    private boolean matchesSelectedWoodBlock(BlockState blockState) {
        ValetWoodTarget target = ValetOrders.getWoodTarget(villager);
        return target != null && target.matches(blockState);
    }

    private boolean isBonusResource(BlockState blockState) {
        return ORE_TAGS.stream().anyMatch(blockState::is) || blockState.is(BlockTags.LOGS);
    }

    private boolean isTreeCrownBlock(BlockState blockState) {
        return blockState.is(BlockTags.LEAVES)
                || blockState.is(Blocks.NETHER_WART_BLOCK)
                || blockState.is(Blocks.WARPED_WART_BLOCK);
    }

    private ItemStack getToolForBlock(BlockState blockState) {
        if (isTreeCrownBlock(blockState)) {
            return new ItemStack(Items.WOODEN_HOE);
        }
        if (blockState.is(BlockTags.MINEABLE_WITH_AXE)) {
            return new ItemStack(Items.WOODEN_AXE);
        }
        if (blockState.is(BlockTags.MINEABLE_WITH_SHOVEL)) {
            return new ItemStack(Items.WOODEN_SHOVEL);
        }
        if (blockState.is(BlockTags.NEEDS_DIAMOND_TOOL)) {
            return new ItemStack(Items.DIAMOND_PICKAXE);
        }
        if (blockState.is(BlockTags.NEEDS_IRON_TOOL)) {
            return new ItemStack(Items.IRON_PICKAXE);
        }
        if (blockState.is(BlockTags.NEEDS_STONE_TOOL)) {
            return new ItemStack(Items.STONE_PICKAXE);
        }
        return new ItemStack(Items.WOODEN_PICKAXE);
    }

    private void updateDisplayedMainHand() {
        if (hasFarmOrder()) {
            equipMainHand(Items.WOODEN_HOE);
            return;
        }

        if (villager.level() instanceof ServerLevel world) {
            ValetRole role = ValetRole.get(world, villager);
            if (role == ValetRole.COMBATANT) {
                equipMainHand(Items.WOODEN_SWORD);
                return;
            }
            if (role == ValetRole.MAGICIAN) {
                equipMainHand(null);
                return;
            }
        }

        if (state == State.IDLE) {
            equipMainHand(Items.COOKIE);
            return;
        }

        if (!hasActiveOrder() && !hasInventoryItems() && (state == State.RETURNING_HOME || isExecuting(PathPurpose.HOME))) {
            equipMainHand(Items.TORCH);
            return;
        }

        equipMainHand(null);
    }

    private void equipMainHand(Item item) {
        ItemStack current = villager.getItemBySlot(EquipmentSlot.MAINHAND);
        if (item == null) {
            if (!current.isEmpty()) {
                villager.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            }
            return;
        }

        if (current.is(item)) {
            return;
        }

        villager.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(item));
        villager.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
    }

    private BlockPos getWorkOrigin(ServerLevel world) {
        BlockPos workOrigin = getKnownWorkOrigin(world);
        if (workOrigin != null) {
            return workOrigin;
        }
        return hasActiveOrder() ? villager.blockPosition() : null;
    }

    private BlockPos getKnownWorkOrigin(ServerLevel world) {
        return ValetHome.get(world, villager);
    }

    private boolean canStoreAllDrops(List<ItemStack> drops) {
        Container inventory = villager.getInventory();
        return ValetInventoryTransfer.canStoreAllDrops(inventory, getUsableInventorySlots(inventory), drops);
    }

    private void collectDrops(List<ItemStack> drops) {
        Container inventory = villager.getInventory();
        for (ItemStack drop : drops) {
            if (drop.isEmpty()) {
                continue;
            }

            ValetInventoryTransfer.insertStack(inventory, drop, getUsableInventorySlots(inventory));
            if (!drop.isEmpty() && villager.level() instanceof ServerLevel world) {
                villager.spawnAtLocation(world, drop);
            }
        }
        inventory.setChanged();
    }

    private void collectNearbyItemEntities(ServerLevel world) {
        AABB box = AABB.unitCubeFromLowerCorner(villager.position()).inflate(2.0D);
        for (ItemEntity itemEntity : world.getEntitiesOfClass(ItemEntity.class, box, item -> !item.isRemoved())) {
            ItemStack stack = itemEntity.getItem();
            Container inventory = villager.getInventory();
            ValetInventoryTransfer.insertStack(inventory, stack, getUsableInventorySlots(inventory));
            if (stack.isEmpty()) {
                itemEntity.discard();
            }
        }
    }

    private boolean hasInventoryItems() {
        Container inventory = villager.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty() && !stack.is(Items.ARROW)) {
                return true;
            }
        }
        return false;
    }

    private boolean isExecuting(PathPurpose purpose) {
        return (state == State.EXECUTING_PATH || state == State.MINING) && pathPurpose == purpose;
    }

    private State interruptedPathState() {
        return ValetStateMachine.interruptedPathState(pathPurpose, hasConstructionOrder(), hasMiningOrder(), hasFarmOrder(), hasCraftOrder(), hasInventorySpace(), hasInventoryItems());
    }

    private State interruptedWorkState() {
        return ValetStateMachine.interruptedWorkState(hasConstructionOrder(), hasMiningOrder(), hasFarmOrder(), hasCraftOrder(), hasInventorySpace(), hasInventoryItems());
    }

    private void holdConversationPosition() {
        villager.getNavigation().stop();
        villager.setDeltaMovement(0.0D, villager.getDeltaMovement().y, 0.0D);
    }

    private boolean shouldClaimMovement(ServerLevel world) {
        if (ValetBehavior.shouldUseVanillaBehavior(world, villager)) {
            return false;
        }
        return hasWorkOrigin() || hasActiveOrder() || hasInventoryItems() || state != State.IDLE;
    }

    private void suppressVanillaMovementTargets() {
        villager.getBrain().eraseMemory(MemoryModuleType.MEETING_POINT);
        villager.getBrain().eraseMemory(MemoryModuleType.POTENTIAL_JOB_SITE);
        villager.getBrain().eraseMemory(MemoryModuleType.SECONDARY_JOB_SITE);
        villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        villager.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
    }

    private boolean hasInventorySpace() {
        Container inventory = villager.getInventory();
        int usableSlots = getUsableInventorySlots(inventory);
        for (int slot = 0; slot < usableSlots; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty() || stack.getCount() < Math.min(stack.getMaxStackSize(), inventory.getMaxStackSize())) {
                return true;
            }
        }
        return false;
    }

    private int getUsableInventorySlots(Container inventory) {
        return settings.usableInventorySlots(inventory);
    }

    private int inventoryItemCount(Container inventory) {
        int count = 0;
        for (int slot = 0; slot < getUsableInventorySlots(inventory); slot++) {
            count += inventory.getItem(slot).getCount();
        }
        return count;
    }

    private void clearPathState() {
        path = List.of();
        pathIndex = 0;
        clearNavigationStep();
        villager.getNavigation().stop();
        miningTask.clearTarget();
        farmingTask.clearTarget();
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
        public Villager villager() {
            return villager;
        }

        @Override
        public BlockPos getWorkOrigin(ServerLevel world) {
            return ValetWorkGoal.this.getWorkOrigin(world);
        }

        @Override
        public boolean isDefenseEnabled() {
            return villager.level() instanceof ServerLevel world && ValetRole.get(world, villager) == ValetRole.COMBATANT;
        }

        @Override
        public boolean isMagicEnabled() {
            return villager.level() instanceof ServerLevel world && ValetRole.get(world, villager) == ValetRole.MAGICIAN;
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
        public double magicAttackRangeSquared() {
            return settings.magicAttackRangeSquared();
        }

        @Override
        public int magicAttackCooldownTicks() {
            return settings.magicAttackCooldownTicks();
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
        public int combatDefenseAmplifier() {
            return settings.combatDefenseAmplifier();
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
        public int getUsableInventorySlots(Container inventory) {
            return ValetWorkGoal.this.getUsableInventorySlots(inventory);
        }

        @Override
        public void animateChestUse(ServerLevel world, BlockPos pos) {
            ValetWorkGoal.this.animateChestUse(world, pos);
        }

        @Override
        public void onCombatStarted(LivingEntity target) {
            clearPathState();
            clearMiningState();
            state = interruptedWorkState();
            delayTicks = 0;
            ValetDebug.record(villager, "combat target=" + ValetDebug.shortPos(target.blockPosition()));
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
        public Villager villager() {
            return villager;
        }

        @Override
        public BlockPos getWorkOrigin(ServerLevel world) {
            return ValetWorkGoal.this.getWorkOrigin(world);
        }

        @Override
        public boolean hasMiningOrder() {
            return ValetWorkGoal.this.hasMiningOrder();
        }

        @Override
        public boolean hasFarmOrder() {
            return ValetWorkGoal.this.hasFarmOrder();
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
        public boolean isNearWorkstation(ServerLevel world, BlockPos workOrigin) {
            return ValetWorkGoal.this.isNearWorkstation(world, workOrigin);
        }

        @Override
        public Set<BlockPos> findStandGoals(ServerLevel world, BlockPos targetBlock, PathPurpose purpose) {
            return ValetWorkGoal.this.findStandGoals(world, targetBlock, purpose);
        }

        @Override
        public List<BlockPos> planPathToAdjacent(ServerLevel world, PathPurpose purpose, BlockPos targetBlock, Set<BlockPos> goals) {
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
        public void animateChestUse(ServerLevel world, BlockPos pos) {
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
        public Villager villager() {
            return villager;
        }

        @Override
        public BlockPos getWorkOrigin(ServerLevel world) {
            return ValetWorkGoal.this.getWorkOrigin(world);
        }

        @Override
        public Set<BlockPos> findBuildStandGoals(ServerLevel world, BlockPos targetBlock) {
            return ValetWorkGoal.this.findBuildStandGoals(world, targetBlock);
        }

        @Override
        public List<BlockPos> planPathToAdjacent(ServerLevel world, PathPurpose purpose, BlockPos targetBlock, Set<BlockPos> goals) {
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
        public int getUsableInventorySlots(Container inventory) {
            return ValetWorkGoal.this.getUsableInventorySlots(inventory);
        }

        @Override
        public int actionDelayTicks() {
            return ValetWorkGoal.this.actionDelayTicks();
        }

        @Override
        public void animateChestUse(ServerLevel world, BlockPos pos) {
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
        public Villager villager() {
            return villager;
        }

        @Override
        public ValetCraftTarget getCraftTarget() {
            return ValetOrders.getCraftTarget(villager);
        }

        @Override
        public BlockPos getWorkOrigin(ServerLevel world) {
            return ValetWorkGoal.this.getWorkOrigin(world);
        }

        @Override
        public BlockPos currentStandPos(ServerLevel world) {
            return ValetWorkGoal.this.currentStandPos(world);
        }

        @Override
        public Set<BlockPos> findStandGoals(ServerLevel world, BlockPos targetBlock, PathPurpose purpose) {
            return ValetWorkGoal.this.findStandGoals(world, targetBlock, purpose);
        }

        @Override
        public List<BlockPos> planPathToAdjacent(ServerLevel world, PathPurpose purpose, BlockPos targetBlock, Set<BlockPos> goals) {
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
        public boolean canMineCraftResource(ServerLevel world, BlockPos pos, BlockState blockState) {
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
        public void animateMining(ServerLevel world, BlockPos miningPos, BlockState miningState) {
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
        public int getUsableInventorySlots(Container inventory) {
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
        public void animateChestUse(ServerLevel world, BlockPos pos) {
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

    private final class FarmingControl implements FarmingRuntimeTask.Control {
        @Override
        public Villager villager() {
            return villager;
        }

        @Override
        public BlockPos getWorkOrigin(ServerLevel world) {
            return ValetWorkGoal.this.getWorkOrigin(world);
        }

        @Override
        public ValetFarmArea getFarmArea(ServerLevel world, int areaId) {
            return areaId < 0 ? null : ValetFarmStorage.get(world).getArea(areaId);
        }

        @Override
        public boolean hasFarmOrder() {
            return ValetWorkGoal.this.hasFarmOrder();
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
        public Set<BlockPos> findStandGoals(ServerLevel world, BlockPos targetBlock, PathPurpose purpose) {
            return ValetWorkGoal.this.findStandGoals(world, targetBlock, purpose);
        }

        @Override
        public List<BlockPos> planPathToAdjacent(ServerLevel world, PathPurpose purpose, BlockPos targetBlock, Set<BlockPos> goals) {
            return ValetWorkGoal.this.planPathToAdjacent(world, purpose, targetBlock, goals);
        }

        @Override
        public void startPath(PathPurpose purpose, List<BlockPos> path) {
            ValetWorkGoal.this.startPath(purpose, path);
        }

        @Override
        public boolean canReachTargetFromStand(BlockPos targetBlock, BlockPos stand) {
            return ValetWorkGoal.this.canReachTargetFromStand(targetBlock, stand);
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
        public boolean claimBlock(ServerLevel world, BlockPos pos, int ttlTicks) {
            return ValetBlockReservations.claim(world, villager.getUUID(), pos, ttlTicks);
        }

        @Override
        public boolean isBlockReservedByOther(ServerLevel world, BlockPos pos) {
            return ValetBlockReservations.isClaimedByOther(world, villager.getUUID(), pos);
        }

        @Override
        public void releaseBlock(BlockPos pos) {
            ValetBlockReservations.release(villager.getUUID(), pos);
        }

        @Override
        public boolean takeOneItem(Item item) {
            Container inventory = villager.getInventory();
            return ValetInventoryTransfer.takeOneItem(inventory, item, getUsableInventorySlots(inventory));
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
        public int farmRadius() {
            return settings.farmRadius();
        }

        @Override
        public int farmVerticalRadius() {
            return settings.farmVerticalRadius();
        }

        @Override
        public int actionDelayTicks() {
            return ValetWorkGoal.this.actionDelayTicks();
        }
    }

    private final class MiningControl implements MiningRuntimeTask.Control {
        @Override
        public Villager villager() {
            return villager;
        }

        @Override
        public PathPurpose currentPathPurpose() {
            return pathPurpose;
        }

        @Override
        public BlockPos getWorkOrigin(ServerLevel world) {
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
        public boolean matchesSelectedTarget(ServerLevel world, BlockPos pos, BlockState blockState) {
            return ValetWorkGoal.this.matchesSelectedTarget(world, pos, blockState);
        }

        @Override
        public boolean isBonusResource(BlockState blockState) {
            return ValetWorkGoal.this.isBonusResource(blockState);
        }

        @Override
        public boolean matchesSelectedWoodBlock(BlockState blockState) {
            return ValetWorkGoal.this.matchesSelectedWoodBlock(blockState);
        }

        @Override
        public Set<BlockPos> findStandGoals(ServerLevel world, BlockPos targetBlock, PathPurpose purpose) {
            return ValetWorkGoal.this.findStandGoals(world, targetBlock, purpose);
        }

        @Override
        public List<BlockPos> planPathToAdjacent(ServerLevel world, PathPurpose purpose, BlockPos targetBlock, Set<BlockPos> goals) {
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
        public boolean canMineWorkBlock(ServerLevel world, BlockPos pos, BlockState blockState) {
            return ValetWorkGoal.this.canMineWorkBlock(world, pos, blockState);
        }

        @Override
        public boolean claimBlock(ServerLevel world, BlockPos pos, int ttlTicks) {
            return ValetBlockReservations.claim(world, villager.getUUID(), pos, ttlTicks);
        }

        @Override
        public boolean isBlockReservedByOther(ServerLevel world, BlockPos pos) {
            return ValetBlockReservations.isClaimedByOther(world, villager.getUUID(), pos);
        }

        @Override
        public void releaseBlock(BlockPos pos) {
            ValetBlockReservations.release(villager.getUUID(), pos);
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
        public void collectNearbyItemEntities(ServerLevel world) {
            ValetWorkGoal.this.collectNearbyItemEntities(world);
        }

        @Override
        public void placeTorchIfNeeded(ServerLevel world, BlockPos minedPos) {
            ValetWorkGoal.this.placeTorchIfNeeded(world, minedPos);
        }

        @Override
        public void animateMining(ServerLevel world, BlockPos miningPos, BlockState miningState) {
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
