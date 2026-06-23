package com.wawane.valet.ai.tasks;

import com.wawane.valet.ValetDebug;
import com.wawane.valet.ValetMod;
import com.wawane.valet.ai.ValetStateMachine.PathPurpose;
import com.wawane.valet.ai.ValetStateMachine.State;
import com.wawane.valet.ai.inventory.ValetInventoryTransfer;
import com.wawane.valet.construction.ConstructionBlueprintBlock;
import com.wawane.valet.construction.ConstructionBlueprintBlockEntity;
import com.wawane.valet.construction.ValetConstructionBlueprint;
import com.wawane.valet.construction.ValetConstructionStorage;
import com.wawane.valet.order.ValetOrder;
import com.wawane.valet.order.ValetOrders;
import com.wawane.valet.progress.ValetProgress;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.enums.BedPart;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public final class ConstructionRuntimeTask {
    private static final int BUILD_SITE_RADIUS = 64;
    private static final int BUILD_SITE_VERTICAL_RADIUS = 24;
    private static final int CONSTRUCTION_REPORT_INTERVAL_TICKS = 80;

    private final Control control;
    private BlockPos targetBuildPos;
    private BlockState targetBuildState;
    private BlockPos targetBuildSecondaryPos;
    private BlockState targetBuildSecondaryState;
    private int constructionReportDelayTicks;
    private String lastConstructionReport = "";

    public ConstructionRuntimeTask(Control control) {
        this.control = control;
    }

    public void tickCooldown() {
        if (constructionReportDelayTicks > 0) {
            constructionReportDelayTicks--;
        }
    }

    public void findTarget(ServerWorld world) {
        BlockPos workOrigin = control.getWorkOrigin(world);
        if (workOrigin == null) {
            ValetDebug.record(control.villager(), "build no_workstation");
            reportConstructionIssue(world, "message.valet.construction_no_workstation");
            control.setDelayTicks(40);
            return;
        }

        ValetConstructionBlueprint blueprint = ValetConstructionStorage.get(world).getBlueprint(ValetOrders.getConstructionTargetId(control.villager()));
        if (blueprint == null) {
            ValetDebug.record(control.villager(), "build unknown_blueprint");
            reportConstructionIssue(world, "message.valet.construction_unknown");
            ValetOrders.set(control.villager(), ValetOrder.NONE);
            control.setState(State.RETURNING_HOME);
            control.setDelayTicks(20);
            return;
        }

        ConstructionSite site = findNearestConstructionSite(world, workOrigin, blueprint.id());
        if (site == null) {
            ValetDebug.record(control.villager(), "build no_blueprint_item id=" + blueprint.id());
            reportConstructionIssue(world, "message.valet.construction_no_blueprint", blueprint.name());
            control.setState(State.RETURNING_HOME);
            control.setDelayTicks(40);
            return;
        }

        for (ValetConstructionBlueprint.Entry entry : blueprint.entries()) {
            BuildTarget buildTarget = createBuildTarget(world, site, blueprint, entry);
            if (buildTarget == null || !needsBuildTarget(world, buildTarget)) {
                continue;
            }

            BlockPos buildPos = buildTarget.pos();
            BlockState buildState = buildTarget.state();
            BlockPos blockedPos = getBlockedBuildPos(world, buildTarget);
            if (blockedPos != null) {
                ValetDebug.record(control.villager(), "build blocked pos=" + ValetDebug.shortPos(blockedPos));
                reportConstructionIssue(world, "message.valet.construction_blocked", blockedPos.getX(), blockedPos.getY(), blockedPos.getZ());
                control.setState(State.RETURNING_HOME);
                control.setDelayTicks(40);
                return;
            }

            if (!hasBuildMaterial(world, workOrigin, buildTarget)) {
                ValetDebug.record(control.villager(), "build missing_material block=" + buildState.getBlock().getTranslationKey());
                reportConstructionIssue(world, "message.valet.construction_missing_material", Text.translatable(getBuildItem(buildState).getTranslationKey()));
                control.setState(State.RETURNING_HOME);
                control.setDelayTicks(40);
                return;
            }

            targetBuildPos = buildPos;
            targetBuildState = buildState;
            targetBuildSecondaryPos = buildTarget.secondaryPos();
            targetBuildSecondaryState = buildTarget.secondaryState();
            Set<BlockPos> goals = control.findBuildStandGoals(world, buildPos);
            if (goals.contains(control.villager().getBlockPos())) {
                control.setState(State.PLACING);
                return;
            }

            List<BlockPos> path = control.planPathToAdjacent(world, PathPurpose.BUILD, buildPos, goals);
            if (path.isEmpty()) {
                ValetDebug.record(control.villager(), "build no_path pos=" + ValetDebug.shortPos(buildPos));
                reportConstructionIssue(world, "message.valet.construction_no_path", buildPos.getX(), buildPos.getY(), buildPos.getZ());
                clearBuildState();
                control.setState(State.RETURNING_HOME);
                control.setDelayTicks(20);
                return;
            }

            control.startPath(PathPurpose.BUILD, path);
            return;
        }

        ValetOrders.set(control.villager(), ValetOrder.NONE);
        ValetDebug.record(control.villager(), "build done id=" + blueprint.id() + " name=" + blueprint.name());
        reportConstructionIssue(world, "message.valet.construction_done", blueprint.name());
        removeFinishedBlueprint(world, site.blueprintPos());
        ValetProgress.addXp(control.villager(), Math.max(10, blueprint.blockCount() / 4));
        clearBuildState();
        control.setState(control.hasInventoryItems() ? State.RETURNING : State.RETURNING_HOME);
        control.setDelayTicks(20);
    }

    public void tickPlacing(ServerWorld world) {
        if (targetBuildPos == null || targetBuildState == null) {
            ValetDebug.record(control.villager(), "build lost_target");
            control.setState(State.FIND_TARGET);
            return;
        }

        if (!control.canReachBuildTargetFromStand(targetBuildPos, control.villager().getBlockPos())) {
            ValetDebug.record(control.villager(), "build lost_reach pos=" + ValetDebug.shortPos(targetBuildPos));
            control.setState(State.FIND_TARGET);
            return;
        }

        BuildTarget buildTarget = new BuildTarget(targetBuildPos, targetBuildState, targetBuildSecondaryPos, targetBuildSecondaryState);
        if (!needsBuildTarget(world, buildTarget)) {
            ValetDebug.record(control.villager(), "build already_done pos=" + ValetDebug.shortPos(targetBuildPos));
            clearBuildState();
            control.setState(State.FIND_TARGET);
            return;
        }

        BlockPos blockedPos = getBlockedBuildPos(world, buildTarget);
        if (blockedPos != null) {
            ValetDebug.record(control.villager(), "build blocked pos=" + ValetDebug.shortPos(blockedPos));
            reportConstructionIssue(world, "message.valet.construction_blocked", blockedPos.getX(), blockedPos.getY(), blockedPos.getZ());
            clearBuildState();
            control.setState(State.RETURNING_HOME);
            control.setDelayTicks(40);
            return;
        }

        BlockPos workOrigin = control.getWorkOrigin(world);
        if (workOrigin == null) {
            ValetDebug.record(control.villager(), "build no_workstation");
            reportConstructionIssue(world, "message.valet.construction_no_workstation");
            control.setState(State.RETURNING_HOME);
            control.setDelayTicks(40);
            return;
        }

        if (!consumeBuildMaterial(world, workOrigin, buildTarget)) {
            ValetDebug.record(control.villager(), "build missing_material block=" + targetBuildState.getBlock().getTranslationKey());
            reportConstructionIssue(world, "message.valet.construction_missing_material", Text.translatable(getBuildItem(targetBuildState).getTranslationKey()));
            control.setState(State.RETURNING_HOME);
            control.setDelayTicks(40);
            return;
        }

        control.villager().swingHand(Hand.MAIN_HAND);
        control.villager().getLookControl().lookAt(targetBuildPos.getX() + 0.5D, targetBuildPos.getY() + 0.5D, targetBuildPos.getZ() + 0.5D);
        if (needsBuildBlock(world, targetBuildPos, targetBuildState)) {
            placeBuildBlock(world, targetBuildPos, targetBuildState);
        }
        if (targetBuildSecondaryPos != null && targetBuildSecondaryState != null && needsBuildBlock(world, targetBuildSecondaryPos, targetBuildSecondaryState)) {
            placeBuildBlock(world, targetBuildSecondaryPos, targetBuildSecondaryState);
        }
        world.playSound(null, targetBuildPos, targetBuildState.getSoundGroup().getPlaceSound(), SoundCategory.BLOCKS, 0.7F, 1.0F);
        ValetDebug.record(control.villager(), "build placed pos=" + ValetDebug.shortPos(targetBuildPos) + " block=" + targetBuildState.getBlock().getTranslationKey());
        ValetProgress.addXp(control.villager(), 1);
        clearBuildState();
        control.setState(State.FIND_TARGET);
        control.setDelayTicks(control.actionDelayTicks());
    }

    public void clearBuildState() {
        targetBuildPos = null;
        targetBuildState = null;
        targetBuildSecondaryPos = null;
        targetBuildSecondaryState = null;
    }

    public String debugSummary() {
        return "buildTarget=" + shortPos(targetBuildPos)
                + " buildBlock=" + (targetBuildState == null ? "-" : targetBuildState.getBlock().getTranslationKey());
    }

    private ConstructionSite findNearestConstructionSite(ServerWorld world, BlockPos origin, int constructionId) {
        ConstructionSite nearest = findNearestConstructionSiteAround(world, control.villager().getBlockPos(), constructionId, null);
        return findNearestConstructionSiteAround(world, origin, constructionId, nearest);
    }

    private ConstructionSite findNearestConstructionSiteAround(ServerWorld world, BlockPos center, int constructionId, ConstructionSite currentNearest) {
        ConstructionSite nearest = currentNearest;
        double nearestDistance = nearest == null ? Double.MAX_VALUE : squaredDistance(control.villager().getBlockPos(), nearest.blueprintPos());

        for (BlockPos pos : BlockPos.iterateOutwards(center, BUILD_SITE_RADIUS, BUILD_SITE_VERTICAL_RADIUS, BUILD_SITE_RADIUS)) {
            BlockPos immutable = pos.toImmutable();
            BlockState state = world.getBlockState(immutable);
            if (!state.isOf(ValetMod.CONSTRUCTION_BLUEPRINT)
                    || !(world.getBlockEntity(immutable) instanceof ConstructionBlueprintBlockEntity blueprint)
                    || !matchesConstructionId(blueprint, constructionId)) {
                continue;
            }

            double distance = squaredDistance(control.villager().getBlockPos(), immutable);
            if (distance < nearestDistance) {
                nearest = new ConstructionSite(immutable, state.get(ConstructionBlueprintBlock.FACING));
                nearestDistance = distance;
            }
        }

        return nearest;
    }

    private boolean matchesConstructionId(ConstructionBlueprintBlockEntity blueprint, int constructionId) {
        return blueprint.getConstructionId() == constructionId
                || blueprint.getBlueprint() != null && blueprint.getBlueprint().id() == constructionId;
    }

    private BlockPos getBuildPos(ConstructionSite site, ValetConstructionBlueprint.Entry entry) {
        return ConstructionTask.getBuildPos(site.blueprintPos(), site.facing(), entry);
    }

    private BlockState rotateBuildState(BlockState state, Direction facing) {
        return ConstructionTask.rotateBuildState(state, facing);
    }

    private BuildTarget createBuildTarget(ServerWorld world, ConstructionSite site, ValetConstructionBlueprint blueprint, ValetConstructionBlueprint.Entry entry) {
        BlockPos buildPos = getBuildPos(site, entry);
        BlockState buildState = rotateBuildState(entry.state(), site.facing());

        if (ConstructionTask.isSecondaryBuildPart(buildState)) {
            BuildTarget primaryTarget = findPairedPrimaryBuildTarget(site, blueprint, buildPos, buildState);
            if (primaryTarget != null && needsBuildBlock(world, primaryTarget.pos(), primaryTarget.state())) {
                return null;
            }
        }

        BuildTarget secondaryTarget = findSecondaryBuildTarget(site, blueprint, buildPos, buildState);
        if (secondaryTarget == null) {
            return new BuildTarget(buildPos, buildState, null, null);
        }
        return new BuildTarget(buildPos, buildState, secondaryTarget.pos(), secondaryTarget.state());
    }

    private BuildTarget findSecondaryBuildTarget(ConstructionSite site, ValetConstructionBlueprint blueprint, BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof DoorBlock
                && state.contains(DoorBlock.HALF)
                && state.get(DoorBlock.HALF) == DoubleBlockHalf.LOWER) {
            BlockPos upperPos = pos.up();
            return findMatchingBuildTarget(site, blueprint, candidate -> candidate.pos().equals(upperPos)
                    && candidate.state().isOf(state.getBlock())
                    && candidate.state().contains(DoorBlock.HALF)
                    && candidate.state().get(DoorBlock.HALF) == DoubleBlockHalf.UPPER);
        }

        if (state.getBlock() instanceof BedBlock
                && state.contains(BedBlock.PART)
                && state.get(BedBlock.PART) == BedPart.FOOT
                && state.contains(Properties.HORIZONTAL_FACING)) {
            BlockPos headPos = pos.offset(state.get(Properties.HORIZONTAL_FACING));
            return findMatchingBuildTarget(site, blueprint, candidate -> candidate.pos().equals(headPos)
                    && candidate.state().isOf(state.getBlock())
                    && candidate.state().contains(BedBlock.PART)
                    && candidate.state().get(BedBlock.PART) == BedPart.HEAD);
        }

        return null;
    }

    private BuildTarget findPairedPrimaryBuildTarget(ConstructionSite site, ValetConstructionBlueprint blueprint, BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof DoorBlock
                && state.contains(DoorBlock.HALF)
                && state.get(DoorBlock.HALF) == DoubleBlockHalf.UPPER) {
            BlockPos lowerPos = pos.down();
            return findMatchingBuildTarget(site, blueprint, candidate -> candidate.pos().equals(lowerPos)
                    && candidate.state().isOf(state.getBlock())
                    && candidate.state().contains(DoorBlock.HALF)
                    && candidate.state().get(DoorBlock.HALF) == DoubleBlockHalf.LOWER);
        }

        if (state.getBlock() instanceof BedBlock
                && state.contains(BedBlock.PART)
                && state.get(BedBlock.PART) == BedPart.HEAD
                && state.contains(Properties.HORIZONTAL_FACING)) {
            BlockPos footPos = pos.offset(state.get(Properties.HORIZONTAL_FACING).getOpposite());
            return findMatchingBuildTarget(site, blueprint, candidate -> candidate.pos().equals(footPos)
                    && candidate.state().isOf(state.getBlock())
                    && candidate.state().contains(BedBlock.PART)
                    && candidate.state().get(BedBlock.PART) == BedPart.FOOT);
        }

        return null;
    }

    private BuildTarget findMatchingBuildTarget(ConstructionSite site, ValetConstructionBlueprint blueprint, Predicate<BuildTarget> predicate) {
        for (ValetConstructionBlueprint.Entry candidateEntry : blueprint.entries()) {
            BlockPos candidatePos = getBuildPos(site, candidateEntry);
            BlockState candidateState = rotateBuildState(candidateEntry.state(), site.facing());
            BuildTarget candidate = new BuildTarget(candidatePos, candidateState, null, null);
            if (predicate.test(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private void removeFinishedBlueprint(ServerWorld world, BlockPos pos) {
        if (world.getBlockState(pos).isOf(ValetMod.CONSTRUCTION_BLUEPRINT)) {
            world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
        }
    }

    private boolean needsBuildBlock(ServerWorld world, BlockPos pos, BlockState targetState) {
        return !isOptionalNaturalBlock(targetState) && !world.getBlockState(pos).equals(targetState);
    }

    private boolean needsBuildTarget(ServerWorld world, BuildTarget target) {
        return needsBuildBlock(world, target.pos(), target.state())
                || target.secondaryPos() != null
                && target.secondaryState() != null
                && needsBuildBlock(world, target.secondaryPos(), target.secondaryState());
    }

    private BlockPos getBlockedBuildPos(ServerWorld world, BuildTarget target) {
        if (needsBuildBlock(world, target.pos(), target.state()) && !canBuildAt(world, target.pos(), target.state())) {
            return target.pos();
        }
        if (target.secondaryPos() != null
                && target.secondaryState() != null
                && needsBuildBlock(world, target.secondaryPos(), target.secondaryState())
                && !canBuildAt(world, target.secondaryPos(), target.secondaryState())) {
            return target.secondaryPos();
        }
        return null;
    }

    private boolean canBuildAt(ServerWorld world, BlockPos pos, BlockState targetState) {
        BlockState current = world.getBlockState(pos);
        return current.equals(targetState)
                || current.isAir()
                || current.isOf(Blocks.TORCH)
                || current.isOf(Blocks.WALL_TORCH)
                || current.isReplaceable()
                || current.isOf(ValetMod.CONSTRUCTION_BEACON)
                || current.isOf(targetState.getBlock());
    }

    private void placeBuildBlock(ServerWorld world, BlockPos pos, BlockState state) {
        world.setBlockState(pos, state, Block.NOTIFY_ALL);
    }

    private boolean hasBuildMaterial(ServerWorld world, BlockPos origin, BuildTarget buildTarget) {
        return !needsBuildBlock(world, buildTarget.pos(), buildTarget.state())
                || hasBuildMaterial(world, origin, buildTarget.pos(), buildTarget.state());
    }

    private boolean hasBuildMaterial(ServerWorld world, BlockPos origin, BlockPos buildPos, BlockState buildState) {
        if (!requiresBuildMaterial(buildState)) {
            return true;
        }

        Item item = getBuildItem(buildState);
        return item != Items.AIR
                && (ValetInventoryTransfer.inventoryHasItem(control.villager().getInventory(), item, control.getUsableInventorySlots(control.villager().getInventory()))
                || findNearestContainerWithItem(world, buildPos, item) != null
                || findNearestContainerWithItem(world, origin, item) != null);
    }

    private boolean consumeBuildMaterial(ServerWorld world, BlockPos origin, BuildTarget buildTarget) {
        return !needsBuildBlock(world, buildTarget.pos(), buildTarget.state())
                || consumeBuildMaterial(world, origin, buildTarget.pos(), buildTarget.state());
    }

    private boolean consumeBuildMaterial(ServerWorld world, BlockPos origin, BlockPos buildPos, BlockState buildState) {
        if (!requiresBuildMaterial(buildState)) {
            return true;
        }

        Item item = getBuildItem(buildState);
        if (item == Items.AIR) {
            return false;
        }

        if (ValetInventoryTransfer.takeOneItem(control.villager().getInventory(), item, control.getUsableInventorySlots(control.villager().getInventory()))) {
            return true;
        }

        BlockPos containerPos = findNearestContainerWithItem(world, buildPos, item);
        if (containerPos == null) {
            containerPos = findNearestContainerWithItem(world, origin, item);
        }
        if (containerPos == null) {
            return false;
        }
        Inventory inventory = ValetInventoryTransfer.getContainerInventory(world, containerPos);
        if (inventory == null) {
            return false;
        }

        boolean taken = ValetInventoryTransfer.takeOneItem(inventory, item, inventory.size());
        if (taken) {
            control.animateChestUse(world, containerPos);
        }
        return taken;
    }

    private BlockPos findNearestContainerWithItem(ServerWorld world, BlockPos origin, Item item) {
        return findNearest(world, origin, control.materialRadius(), 8, pos -> {
            BlockState blockState = world.getBlockState(pos);
            Inventory inventory = ValetInventoryTransfer.getContainerInventory(world, pos);
            return (blockState.isOf(Blocks.CHEST) || blockState.isOf(Blocks.TRAPPED_CHEST) || blockState.isOf(Blocks.BARREL))
                    && inventory != null
                    && ValetInventoryTransfer.inventoryHasItem(inventory, item, inventory.size());
        });
    }

    private BlockPos findNearest(ServerWorld world, BlockPos origin, int horizontalRadius, int verticalRadius, BlockPredicate predicate) {
        BlockPos nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (BlockPos pos : BlockPos.iterateOutwards(origin, horizontalRadius, verticalRadius, horizontalRadius)) {
            BlockPos immutable = pos.toImmutable();
            if (predicate.test(immutable)) {
                double distance = squaredDistance(origin, immutable);
                if (distance < nearestDistance) {
                    nearest = immutable;
                    nearestDistance = distance;
                }
            }
        }

        return nearest;
    }

    private Item getBuildItem(BlockState buildState) {
        return buildState.getBlock().asItem();
    }

    private boolean requiresBuildMaterial(BlockState buildState) {
        if (isOptionalNaturalBlock(buildState)) {
            return false;
        }
        if (buildState.getBlock() instanceof BedBlock && buildState.contains(BedBlock.PART)) {
            return buildState.get(BedBlock.PART) == BedPart.FOOT;
        }
        if (buildState.getBlock() instanceof DoorBlock && buildState.contains(DoorBlock.HALF)) {
            return buildState.get(DoorBlock.HALF) == DoubleBlockHalf.LOWER;
        }
        return true;
    }

    private boolean isOptionalNaturalBlock(BlockState buildState) {
        return buildState.isReplaceable()
                || buildState.isIn(BlockTags.LEAVES);
    }

    private void reportConstructionIssue(ServerWorld world, String messageKey, Object... args) {
        String signature = messageKey + Arrays.toString(args);
        if (constructionReportDelayTicks > 0 && signature.equals(lastConstructionReport)) {
            return;
        }

        constructionReportDelayTicks = CONSTRUCTION_REPORT_INTERVAL_TICKS;
        lastConstructionReport = signature;
        ValetMod.LOGGER.info("Valet construction: {} {}", messageKey, Arrays.toString(args));
        Text message = Text.translatable(messageKey, args);
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (player.squaredDistanceTo(control.villager()) <= 64.0D * 64.0D) {
                player.sendMessage(message, true);
            }
        }
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

    private record ConstructionSite(BlockPos blueprintPos, Direction facing) {
    }

    private record BuildTarget(BlockPos pos, BlockState state, BlockPos secondaryPos, BlockState secondaryState) {
    }

    @FunctionalInterface
    private interface BlockPredicate {
        boolean test(BlockPos pos);
    }

    public interface Control {
        VillagerEntity villager();

        BlockPos getWorkOrigin(ServerWorld world);

        Set<BlockPos> findBuildStandGoals(ServerWorld world, BlockPos targetBlock);

        List<BlockPos> planPathToAdjacent(ServerWorld world, PathPurpose purpose, BlockPos targetBlock, Set<BlockPos> goals);

        void startPath(PathPurpose purpose, List<BlockPos> path);

        boolean canReachBuildTargetFromStand(BlockPos targetBlock, BlockPos stand);

        boolean hasInventoryItems();

        int materialRadius();

        int getUsableInventorySlots(Inventory inventory);

        int actionDelayTicks();

        void animateChestUse(ServerWorld world, BlockPos pos);

        void setState(State state);

        void setDelayTicks(int ticks);
    }
}
