package com.wawane.valet.ai;

import com.wawane.valet.ValetMod;
import com.wawane.valet.ValetConversations;
import com.wawane.valet.ValetHome;
import com.wawane.valet.ai.ValetStateMachine.PathPurpose;
import com.wawane.valet.ai.ValetStateMachine.State;
import com.wawane.valet.ai.inventory.ValetInventoryTransfer;
import com.wawane.valet.ai.path.ValetPathPlanner;
import com.wawane.valet.ai.tasks.ConstructionRuntimeTask;
import com.wawane.valet.ai.tasks.LogisticsRuntimeTask;
import com.wawane.valet.ai.tasks.MiningRuntimeTask;
import com.wawane.valet.order.ValetMineTarget;
import com.wawane.valet.order.ValetOrder;
import com.wawane.valet.order.ValetOrders;
import com.wawane.valet.order.ValetWoodTarget;
import com.wawane.valet.progress.ValetPerk;
import com.wawane.valet.progress.ValetProgress;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
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

public class ValetWorkGoal extends Goal {
    private static final int CHEST_RADIUS = 10;
    private static final int MINE_RADIUS = 18;
    private static final int MINE_VERTICAL_RADIUS = 12;
    private static final int MAX_PATH_NODES = 18000;
    private static final int MAX_PATH_LENGTH = 96;
    private static final int NO_TARGET_DELAY_TICKS = 80;
    private static final int MAX_VEIN_BLOCKS = 96;
    private static final int BASE_INVENTORY_SLOTS = 4;
    private static final int STORAGE_PERK_BONUS_SLOTS = 4;
    private static final int PASSAGE_HEIGHT = 2;
    private static final int STAIR_CLEARANCE_HEIGHT = 4;
    private static final int CHEST_RADIUS_BONUS = 8;
    private static final int MAX_PATH_NODES_BONUS = 9000;
    private static final int MAX_PATH_LENGTH_BONUS = 48;
    private static final int MAX_VEIN_BLOCKS_BONUS = 64;
    private static final int NAVIGATION_STEP_TIMEOUT_TICKS = 30;
    private static final double NAVIGATION_STEP_SPEED = 1.0D;
    private static final double NAVIGATION_REACHED_DISTANCE_SQUARED = 0.75D;
    private static final int MONSTER_SPAWN_BLOCK_LIGHT = 0;
    private static final int COMFORT_TORCH_BLOCK_LIGHT = 7;
    private static final int BUILD_MATERIAL_RADIUS_BONUS = 16;
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

    private final VillagerEntity villager;
    private final ValetPathPlanner pathPlanner = new ValetPathPlanner();
    private final MiningRuntimeTask miningTask;
    private final ConstructionRuntimeTask constructionTask;
    private final LogisticsRuntimeTask logisticsTask;
    private State state = State.FIND_TARGET;
    private PathPurpose pathPurpose = PathPurpose.ORE;
    private List<BlockPos> path = List.of();
    private int pathIndex;
    private BlockPos navigationStepTarget;
    private int navigationStepTicks;
    private int delayTicks;

    public ValetWorkGoal(VillagerEntity villager) {
        this.villager = villager;
        this.miningTask = new MiningRuntimeTask(new MiningControl());
        this.constructionTask = new ConstructionRuntimeTask(new ConstructionControl());
        this.logisticsTask = new LogisticsRuntimeTask(new LogisticsControl());
        setControls(EnumSet.of(Control.MOVE, Control.LOOK));
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
        state = chooseStartState();
        delayTicks = 0;
        clearPathState();
        ValetMod.LOGGER.info("Valet {} goal starts order {}", villager.getUuid(), ValetOrders.get(villager).getId());
    }

    @Override
    public void stop() {
        state = State.FIND_TARGET;
        delayTicks = 0;
        clearPathState();
        clearMiningState();
        clearVeinState();
        villager.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (!(villager.getWorld() instanceof ServerWorld world)) {
            return;
        }

        constructionTask.tickCooldown();

        if (ValetConversations.isTalking(villager)) {
            suppressVanillaMovementTargets();
            holdConversationPosition();
            return;
        }

        if (shouldClaimMovement()) {
            suppressVanillaMovementTargets();
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
            case COLLECTING -> miningTask.tickCollecting(world);
            case RETURNING -> logisticsTask.returnToChest(world);
            case RETURNING_HOME -> logisticsTask.returnToWorkstation(world);
            case DEPOSITING -> logisticsTask.tickDepositing(world);
        }
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

    private boolean hasActiveOrder() {
        return hasMiningOrder() || hasConstructionOrder();
    }

    private boolean hasWorkOrigin() {
        return villager.getWorld() instanceof ServerWorld world && getKnownWorkOrigin(world) != null;
    }

    private State chooseStartState() {
        return ValetStateMachine.chooseStartState(ValetConversations.isTalking(villager), hasActiveOrder(), shouldReturnToChestBeforeWork());
    }

    private boolean shouldReturnToChestBeforeWork() {
        return !hasConstructionOrder() && hasInventoryItems() && (!hasMiningOrder() || !hasInventorySpace());
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
        return hasInventorySpace() && (state == State.RETURNING || state == State.DEPOSITING || isExecuting(PathPurpose.CHEST));
    }

    private boolean shouldPreemptForConstructionOrder() {
        return state == State.IDLE
                || state == State.RETURNING_HOME
                || state == State.RETURNING
                || state == State.DEPOSITING
                || isExecuting(PathPurpose.HOME)
                || isExecuting(PathPurpose.CHEST);
    }

    private void findTarget(ServerWorld world) {
        if (hasConstructionOrder()) {
            constructionTask.findTarget(world);
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
            } else {
                state = State.IDLE;
            }
            return;
        }

        BlockPos next = path.get(pathIndex);
        if (hasReachedPathStep(next)) {
            clearNavigationStep();
            pathIndex++;
            delayTicks = actionDelayTicks();
            return;
        }

        BlockPos obstruction = findMovementObstruction(world, villager.getBlockPos(), next);
        if (obstruction != null) {
            miningTask.beginMining(world, obstruction, false);
            return;
        }

        if (!canTraverseStep(world, villager.getBlockPos(), next)) {
            state = interruptedPathState();
            clearPathState();
            delayTicks = 10;
            return;
        }

        moveTowardPathStep(next);
    }

    private List<BlockPos> planPathToAdjacent(ServerWorld world, BlockPos targetBlock, Set<BlockPos> goals) {
        BlockPos start = villager.getBlockPos();
        if (goals.isEmpty()) {
            return List.of();
        }

        BlockPos origin = getWorkOrigin(world);
        if (origin == null) {
            return List.of();
        }

        return pathPlanner.planPathToAdjacent(world, origin, start, targetBlock, goals, maxPathNodes(), maxPathLength(), this::canPrepareStep, this::movementCost);
    }

    private void startPath(PathPurpose purpose, List<BlockPos> nextPath) {
        pathPurpose = purpose;
        path = nextPath;
        pathIndex = 0;
        clearNavigationStep();
        state = State.EXECUTING_PATH;
    }

    private boolean hasReachedPathStep(BlockPos step) {
        return villager.getBlockPos().equals(step)
                || villager.squaredDistanceTo(step.getX() + 0.5D, step.getY(), step.getZ() + 0.5D) <= NAVIGATION_REACHED_DISTANCE_SQUARED;
    }

    private void moveTowardPathStep(BlockPos step) {
        boolean newStep = !step.equals(navigationStepTarget);
        if (newStep) {
            navigationStepTarget = step.toImmutable();
            navigationStepTicks = NAVIGATION_STEP_TIMEOUT_TICKS;
            villager.getNavigation().stop();
        }

        suppressVanillaMovementTargets();
        villager.getLookControl().lookAt(step.getX() + 0.5D, step.getY() + 1.0D, step.getZ() + 0.5D);
        if (newStep || villager.getNavigation().isIdle() || navigationStepTicks % 5 == 0) {
            villager.getNavigation().startMovingTo(step.getX() + 0.5D, step.getY(), step.getZ() + 0.5D, NAVIGATION_STEP_SPEED);
        }

        navigationStepTicks--;
        if (navigationStepTicks <= 0) {
            state = interruptedPathState();
            clearPathState();
            delayTicks = 10;
        }
    }

    private void clearNavigationStep() {
        navigationStepTarget = null;
        navigationStepTicks = 0;
    }

    private Set<BlockPos> findStandGoals(ServerWorld world, BlockPos targetBlock) {
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
            if (!stand.equals(targetBlock) && canReachTargetFromStand(targetBlock, stand) && canPrepareStand(world, stand)) {
                goals.add(stand);
            }
        }

        return goals;
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
            if (!stand.equals(targetBlock) && canReachBuildTargetFromStand(targetBlock, stand) && canPrepareStand(world, stand)) {
                goals.add(stand);
            }
        }

        return goals;
    }

    private boolean isNearWorkstation(ServerWorld world, BlockPos workOrigin) {
        Set<BlockPos> goals = findStandGoals(world, workOrigin);
        return goals.contains(villager.getBlockPos()) || squaredDistance(villager.getBlockPos(), workOrigin) <= 4;
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

    private int movementCost(ServerWorld world, BlockPos from, BlockPos to) {
        int cost = 10 + Math.abs(to.getY() - from.getY()) * 6;
        for (BlockPos pos : movementClearancePositions(from, to)) {
            cost += clearCost(world, pos);
        }
        return cost;
    }

    private int clearCost(ServerWorld world, BlockPos pos) {
        BlockState blockState = world.getBlockState(pos);
        if (blockState.isAir()) {
            return 0;
        }
        return 8 + Math.max(0, Math.round(blockState.getHardness(world, pos) * 4.0F));
    }

    private boolean canPrepareStand(ServerWorld world, BlockPos standPos) {
        if (!canStandOn(world, standPos.down())) {
            return false;
        }

        for (BlockPos pos : standClearancePositions(standPos)) {
            if (!canClearForTunnel(world, pos)) {
                return false;
            }
        }
        return true;
    }

    private boolean canPrepareStep(ServerWorld world, BlockPos from, BlockPos to) {
        int dx = Math.abs(to.getX() - from.getX());
        int dy = to.getY() - from.getY();
        int dz = Math.abs(to.getZ() - from.getZ());
        if (dx + dz != 1 || dy < -1 || dy > 1 || !canStandOn(world, to.down())) {
            return false;
        }

        for (BlockPos pos : movementClearancePositions(from, to)) {
            if (!canClearForTunnel(world, pos)) {
                return false;
            }
        }
        return true;
    }

    private boolean canClearForTunnel(ServerWorld world, BlockPos pos) {
        BlockState blockState = world.getBlockState(pos);
        return isPassableTunnelSpace(blockState) || canMinePathBlock(world, pos, blockState);
    }

    private BlockPos findMovementObstruction(ServerWorld world, BlockPos from, BlockPos to) {
        for (BlockPos candidate : movementClearancePositions(from, to)) {
            BlockState blockState = world.getBlockState(candidate);
            if (!isPassableTunnelSpace(blockState)) {
                return canMinePathBlock(world, candidate, blockState) ? candidate : null;
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
            if (!isPassableTunnelSpace(world.getBlockState(pos))) {
                return false;
            }
        }
        return true;
    }

    private boolean isSafeStand(ServerWorld world, BlockPos standPos) {
        if (!canStandOn(world, standPos.down())) {
            return false;
        }

        for (BlockPos pos : standClearancePositions(standPos)) {
            if (!isPassableTunnelSpace(world.getBlockState(pos))) {
                return false;
            }
        }
        return true;
    }

    private List<BlockPos> standClearancePositions(BlockPos standPos) {
        List<BlockPos> positions = new ArrayList<>(PASSAGE_HEIGHT);
        for (int y = 0; y < PASSAGE_HEIGHT; y++) {
            positions.add(standPos.up(y));
        }
        return positions;
    }

    private List<BlockPos> movementClearancePositions(BlockPos from, BlockPos to) {
        List<BlockPos> positions = standClearancePositions(to);
        if (from.getY() != to.getY()) {
            BlockPos lower = from.getY() < to.getY() ? from : to;
            for (int y = 0; y < STAIR_CLEARANCE_HEIGHT; y++) {
                addUnique(positions, lower.up(y));
            }
        }
        return positions;
    }

    private void addUnique(List<BlockPos> positions, BlockPos pos) {
        if (!positions.contains(pos)) {
            positions.add(pos);
        }
    }

    private boolean isPassableTunnelSpace(BlockState blockState) {
        return blockState.isAir() || blockState.isOf(Blocks.TORCH) || blockState.isOf(Blocks.WALL_TORCH);
    }

    private boolean canStandOn(ServerWorld world, BlockPos pos) {
        BlockState blockState = world.getBlockState(pos);
        return !isPassableTunnelSpace(blockState)
                && blockState.getFluidState().isEmpty()
                && blockState.getHardness(world, pos) >= 0.0F
                && blockState.isSideSolidFullSquare(world, pos, Direction.UP);
    }

    private boolean canMinePathBlock(ServerWorld world, BlockPos pos, BlockState blockState) {
        if (blockState.isAir()
                || !blockState.getFluidState().isEmpty()
                || blockState.isOf(ValetMod.VALET_WORKSTATION)
                || blockState.isOf(ValetMod.CONSTRUCTION_BEACON)
                || blockState.isOf(ValetMod.CONSTRUCTION_BLUEPRINT)
                || blockState.isOf(Blocks.CHEST)
                || blockState.isOf(Blocks.TRAPPED_CHEST)
                || blockState.isOf(Blocks.BARREL)
                || blockState.getHardness(world, pos) < 0.0F) {
            return false;
        }

        return ORE_TAGS.stream().anyMatch(blockState::isIn)
                || blockState.isIn(BlockTags.PICKAXE_MINEABLE)
                || blockState.isIn(BlockTags.SHOVEL_MINEABLE)
                || blockState.isIn(BlockTags.AXE_MINEABLE);
    }

    private int mineRadius() {
        return ValetProgress.hasPerk(villager, ValetPerk.VISION) ? MINE_RADIUS + 8 : MINE_RADIUS;
    }

    private int mineVerticalRadius() {
        return ValetProgress.hasPerk(villager, ValetPerk.VISION) ? MINE_VERTICAL_RADIUS + 4 : MINE_VERTICAL_RADIUS;
    }

    private int actionDelayTicks() {
        return ValetProgress.hasPerk(villager, ValetPerk.SPEED) ? 0 : 1;
    }

    private int chestRadius() {
        return ValetProgress.hasPerk(villager, ValetPerk.HAUL) ? CHEST_RADIUS + CHEST_RADIUS_BONUS : CHEST_RADIUS;
    }

    private int materialRadius() {
        return chestRadius() + BUILD_MATERIAL_RADIUS_BONUS;
    }

    private int maxPathNodes() {
        return ValetProgress.hasPerk(villager, ValetPerk.PATHING) ? MAX_PATH_NODES + MAX_PATH_NODES_BONUS : MAX_PATH_NODES;
    }

    private int maxPathLength() {
        return ValetProgress.hasPerk(villager, ValetPerk.PATHING) ? MAX_PATH_LENGTH + MAX_PATH_LENGTH_BONUS : MAX_PATH_LENGTH;
    }

    private int maxVeinBlocks() {
        return ValetProgress.hasPerk(villager, ValetPerk.VEIN) ? MAX_VEIN_BLOCKS + MAX_VEIN_BLOCKS_BONUS : MAX_VEIN_BLOCKS;
    }

    private int torchLightThreshold() {
        return ValetProgress.hasPerk(villager, ValetPerk.LIGHTING) ? COMFORT_TORCH_BLOCK_LIGHT : MONSTER_SPAWN_BLOCK_LIGHT;
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
            world.playSound(null, pos, SoundEvents.BLOCK_BARREL_OPEN, SoundCategory.BLOCKS, 0.6F, 1.0F);
        } else {
            world.playSound(null, pos, SoundEvents.BLOCK_CHEST_OPEN, SoundCategory.BLOCKS, 0.6F, 1.0F);
        }
    }

    private boolean matchesSelectedTarget(BlockState blockState) {
        ValetOrder order = ValetOrders.get(villager);
        if (order == ValetOrder.MINE_ORES) {
            ValetMineTarget target = ValetOrders.getMineTarget(villager);
            return target != null && target.matches(blockState);
        }
        if (order == ValetOrder.CHOP_WOOD) {
            ValetWoodTarget target = ValetOrders.getWoodTarget(villager);
            return target != null && target.matches(blockState);
        }
        return false;
    }

    private boolean isBonusResource(BlockState blockState) {
        return ORE_TAGS.stream().anyMatch(blockState::isIn) || blockState.isIn(BlockTags.LOGS);
    }

    private ItemStack getToolForBlock(BlockState blockState) {
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
            if (!inventory.getStack(slot).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private boolean isExecuting(PathPurpose purpose) {
        return (state == State.EXECUTING_PATH || state == State.MINING) && pathPurpose == purpose;
    }

    private State interruptedPathState() {
        return ValetStateMachine.interruptedPathState(pathPurpose, hasConstructionOrder(), hasMiningOrder(), hasInventorySpace(), hasInventoryItems());
    }

    private State interruptedWorkState() {
        return ValetStateMachine.interruptedWorkState(hasConstructionOrder(), hasMiningOrder(), hasInventorySpace(), hasInventoryItems());
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
        int slots = BASE_INVENTORY_SLOTS;
        if (ValetProgress.hasPerk(villager, ValetPerk.STORAGE)) {
            slots += STORAGE_PERK_BONUS_SLOTS;
        }
        return Math.min(inventory.size(), slots);
    }

    private void clearPathState() {
        path = List.of();
        pathIndex = 0;
        clearNavigationStep();
        villager.getNavigation().stop();
        miningTask.clearTarget();
        logisticsTask.clearChestTarget();
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
        public Set<BlockPos> findStandGoals(ServerWorld world, BlockPos targetBlock) {
            return ValetWorkGoal.this.findStandGoals(world, targetBlock);
        }

        @Override
        public List<BlockPos> planPathToAdjacent(ServerWorld world, BlockPos targetBlock, Set<BlockPos> goals) {
            return ValetWorkGoal.this.planPathToAdjacent(world, targetBlock, goals);
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
        public List<BlockPos> planPathToAdjacent(ServerWorld world, BlockPos targetBlock, Set<BlockPos> goals) {
            return ValetWorkGoal.this.planPathToAdjacent(world, targetBlock, goals);
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
        public boolean matchesSelectedTarget(BlockState blockState) {
            return ValetWorkGoal.this.matchesSelectedTarget(blockState);
        }

        @Override
        public boolean isBonusResource(BlockState blockState) {
            return ValetWorkGoal.this.isBonusResource(blockState);
        }

        @Override
        public Set<BlockPos> findStandGoals(ServerWorld world, BlockPos targetBlock) {
            return ValetWorkGoal.this.findStandGoals(world, targetBlock);
        }

        @Override
        public List<BlockPos> planPathToAdjacent(ServerWorld world, BlockPos targetBlock, Set<BlockPos> goals) {
            return ValetWorkGoal.this.planPathToAdjacent(world, targetBlock, goals);
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
        public boolean canMinePathBlock(ServerWorld world, BlockPos pos, BlockState blockState) {
            return ValetWorkGoal.this.canMinePathBlock(world, pos, blockState);
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
            return NO_TARGET_DELAY_TICKS;
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
