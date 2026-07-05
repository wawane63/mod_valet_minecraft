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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

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

    public void findTarget(ServerLevel world) {
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

        List<MissingMaterial> missingMaterials = findMissingMaterials(world, workOrigin, site, blueprint);
        if (!missingMaterials.isEmpty()) {
            ValetDebug.record(control.villager(), "build missing_materials " + missingMaterialsDebug(missingMaterials));
            reportConstructionIssue(world, "message.valet.construction_missing_materials", formatMissingMaterials(missingMaterials));
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
                ValetDebug.record(control.villager(), "build missing_material block=" + buildState.getBlock().getDescriptionId());
                reportConstructionIssue(world, "message.valet.construction_missing_material", Component.translatable(getBuildItem(buildState).getDescriptionId()));
                control.setState(State.RETURNING_HOME);
                control.setDelayTicks(40);
                return;
            }

            targetBuildPos = buildPos;
            targetBuildState = buildState;
            targetBuildSecondaryPos = buildTarget.secondaryPos();
            targetBuildSecondaryState = buildTarget.secondaryState();
            Set<BlockPos> goals = control.findBuildStandGoals(world, buildPos);
            if (goals.contains(control.villager().blockPosition())) {
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

    public void tickPlacing(ServerLevel world) {
        if (targetBuildPos == null || targetBuildState == null) {
            ValetDebug.record(control.villager(), "build lost_target");
            control.setState(State.FIND_TARGET);
            return;
        }

        if (!control.canReachBuildTargetFromStand(targetBuildPos, control.villager().blockPosition())) {
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
            ValetDebug.record(control.villager(), "build missing_material block=" + targetBuildState.getBlock().getDescriptionId());
            reportConstructionIssue(world, "message.valet.construction_missing_material", Component.translatable(getBuildItem(targetBuildState).getDescriptionId()));
            control.setState(State.RETURNING_HOME);
            control.setDelayTicks(40);
            return;
        }

        control.villager().swing(InteractionHand.MAIN_HAND);
        control.villager().getLookControl().setLookAt(targetBuildPos.getX() + 0.5D, targetBuildPos.getY() + 0.5D, targetBuildPos.getZ() + 0.5D);
        if (needsBuildBlock(world, targetBuildPos, targetBuildState)) {
            placeBuildBlock(world, targetBuildPos, targetBuildState);
        }
        if (targetBuildSecondaryPos != null && targetBuildSecondaryState != null && needsBuildBlock(world, targetBuildSecondaryPos, targetBuildSecondaryState)) {
            placeBuildBlock(world, targetBuildSecondaryPos, targetBuildSecondaryState);
        }
        world.playSound(null, targetBuildPos, targetBuildState.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 0.7F, 1.0F);
        ValetDebug.record(control.villager(), "build placed pos=" + ValetDebug.shortPos(targetBuildPos) + " block=" + targetBuildState.getBlock().getDescriptionId());
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
                + " buildBlock=" + (targetBuildState == null ? "-" : targetBuildState.getBlock().getDescriptionId());
    }

    private ConstructionSite findNearestConstructionSite(ServerLevel world, BlockPos origin, int constructionId) {
        ConstructionSite nearest = findNearestConstructionSiteAround(world, control.villager().blockPosition(), constructionId, null);
        return findNearestConstructionSiteAround(world, origin, constructionId, nearest);
    }

    private ConstructionSite findNearestConstructionSiteAround(ServerLevel world, BlockPos center, int constructionId, ConstructionSite currentNearest) {
        ConstructionSite nearest = currentNearest;
        double nearestDistance = nearest == null ? Double.MAX_VALUE : squaredDistance(control.villager().blockPosition(), nearest.blueprintPos());

        for (BlockPos pos : BlockPos.withinManhattan(center, BUILD_SITE_RADIUS, BUILD_SITE_VERTICAL_RADIUS, BUILD_SITE_RADIUS)) {
            BlockPos immutable = pos.immutable();
            BlockState state = world.getBlockState(immutable);
            if (!state.is(ValetMod.CONSTRUCTION_BLUEPRINT)
                    || !(world.getBlockEntity(immutable) instanceof ConstructionBlueprintBlockEntity blueprint)
                    || !matchesConstructionId(blueprint, constructionId)) {
                continue;
            }

            double distance = squaredDistance(control.villager().blockPosition(), immutable);
            if (distance < nearestDistance) {
                nearest = new ConstructionSite(immutable, state.getValue(ConstructionBlueprintBlock.FACING), blueprint.isMirrored());
                nearestDistance = distance;
            }
        }

        return nearest;
    }

    private boolean matchesConstructionId(ConstructionBlueprintBlockEntity blueprint, int constructionId) {
        return blueprint.getConstructionId() == constructionId
                || blueprint.getBlueprint() != null && blueprint.getBlueprint().id() == constructionId;
    }

    private BlockPos getBuildPos(ConstructionSite site, ValetConstructionBlueprint blueprint, ValetConstructionBlueprint.Entry entry) {
        return ConstructionTask.getBuildPos(site.blueprintPos(), site.facing(), site.mirrored(), blueprint, entry);
    }

    private BlockState transformBuildState(BlockState state, ConstructionSite site) {
        return ConstructionTask.transformBuildState(state, site.facing(), site.mirrored());
    }

    private BuildTarget createBuildTarget(ServerLevel world, ConstructionSite site, ValetConstructionBlueprint blueprint, ValetConstructionBlueprint.Entry entry) {
        BlockPos buildPos = getBuildPos(site, blueprint, entry);
        BlockState buildState = transformBuildState(entry.state(), site);

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
                && state.hasProperty(DoorBlock.HALF)
                && state.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER) {
            BlockPos upperPos = pos.above();
            return findMatchingBuildTarget(site, blueprint, candidate -> candidate.pos().equals(upperPos)
                    && candidate.state().is(state.getBlock())
                    && candidate.state().hasProperty(DoorBlock.HALF)
                    && candidate.state().getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER);
        }

        if (state.getBlock() instanceof BedBlock
                && state.hasProperty(BedBlock.PART)
                && state.getValue(BedBlock.PART) == BedPart.FOOT
                && state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            BlockPos headPos = pos.relative(state.getValue(BlockStateProperties.HORIZONTAL_FACING));
            return findMatchingBuildTarget(site, blueprint, candidate -> candidate.pos().equals(headPos)
                    && candidate.state().is(state.getBlock())
                    && candidate.state().hasProperty(BedBlock.PART)
                    && candidate.state().getValue(BedBlock.PART) == BedPart.HEAD);
        }

        return null;
    }

    private BuildTarget findPairedPrimaryBuildTarget(ConstructionSite site, ValetConstructionBlueprint blueprint, BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof DoorBlock
                && state.hasProperty(DoorBlock.HALF)
                && state.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER) {
            BlockPos lowerPos = pos.below();
            return findMatchingBuildTarget(site, blueprint, candidate -> candidate.pos().equals(lowerPos)
                    && candidate.state().is(state.getBlock())
                    && candidate.state().hasProperty(DoorBlock.HALF)
                    && candidate.state().getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER);
        }

        if (state.getBlock() instanceof BedBlock
                && state.hasProperty(BedBlock.PART)
                && state.getValue(BedBlock.PART) == BedPart.HEAD
                && state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            BlockPos footPos = pos.relative(state.getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite());
            return findMatchingBuildTarget(site, blueprint, candidate -> candidate.pos().equals(footPos)
                    && candidate.state().is(state.getBlock())
                    && candidate.state().hasProperty(BedBlock.PART)
                    && candidate.state().getValue(BedBlock.PART) == BedPart.FOOT);
        }

        return null;
    }

    private BuildTarget findMatchingBuildTarget(ConstructionSite site, ValetConstructionBlueprint blueprint, Predicate<BuildTarget> predicate) {
        for (ValetConstructionBlueprint.Entry candidateEntry : blueprint.entries()) {
            BlockPos candidatePos = getBuildPos(site, blueprint, candidateEntry);
            BlockState candidateState = transformBuildState(candidateEntry.state(), site);
            BuildTarget candidate = new BuildTarget(candidatePos, candidateState, null, null);
            if (predicate.test(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private void removeFinishedBlueprint(ServerLevel world, BlockPos pos) {
        if (world.getBlockState(pos).is(ValetMod.CONSTRUCTION_BLUEPRINT)) {
            world.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    private boolean needsBuildBlock(ServerLevel world, BlockPos pos, BlockState targetState) {
        return !isOptionalNaturalBlock(targetState) && !world.getBlockState(pos).equals(targetState);
    }

    private boolean needsBuildTarget(ServerLevel world, BuildTarget target) {
        return needsBuildBlock(world, target.pos(), target.state())
                || target.secondaryPos() != null
                && target.secondaryState() != null
                && needsBuildBlock(world, target.secondaryPos(), target.secondaryState());
    }

    private BlockPos getBlockedBuildPos(ServerLevel world, BuildTarget target) {
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

    private boolean canBuildAt(ServerLevel world, BlockPos pos, BlockState targetState) {
        BlockState current = world.getBlockState(pos);
        return current.equals(targetState)
                || current.isAir()
                || current.is(Blocks.TORCH)
                || current.is(Blocks.WALL_TORCH)
                || current.canBeReplaced()
                || current.is(ValetMod.CONSTRUCTION_BEACON)
                || current.is(targetState.getBlock());
    }

    private void placeBuildBlock(ServerLevel world, BlockPos pos, BlockState state) {
        world.setBlock(pos, state, Block.UPDATE_ALL);
    }

    private List<MissingMaterial> findMissingMaterials(ServerLevel world, BlockPos workOrigin, ConstructionSite site, ValetConstructionBlueprint blueprint) {
        Map<Item, Integer> required = new HashMap<>();
        for (ValetConstructionBlueprint.Entry entry : blueprint.entries()) {
            BuildTarget buildTarget = createBuildTarget(world, site, blueprint, entry);
            if (buildTarget == null || !needsBuildTarget(world, buildTarget)) {
                continue;
            }

            addRequiredMaterial(world, required, buildTarget.pos(), buildTarget.state());
            if (buildTarget.secondaryPos() != null && buildTarget.secondaryState() != null) {
                addRequiredMaterial(world, required, buildTarget.secondaryPos(), buildTarget.secondaryState());
            }
        }

        if (required.isEmpty()) {
            return List.of();
        }

        Map<Item, Integer> available = countAvailableMaterials(world, workOrigin, site.blueprintPos(), required.keySet());
        List<MissingMaterial> missing = new ArrayList<>();
        for (Map.Entry<Item, Integer> entry : required.entrySet()) {
            int availableCount = available.getOrDefault(entry.getKey(), 0);
            if (availableCount < entry.getValue()) {
                missing.add(new MissingMaterial(entry.getKey(), entry.getValue(), availableCount));
            }
        }
        missing.sort(Comparator.comparing(material -> material.item().getDescriptionId()));
        return missing;
    }

    private void addRequiredMaterial(ServerLevel world, Map<Item, Integer> required, BlockPos pos, BlockState state) {
        if (!needsBuildBlock(world, pos, state) || !requiresBuildMaterial(state)) {
            return;
        }

        Item item = getBuildItem(state);
        if (item != Items.AIR) {
            required.merge(item, 1, Integer::sum);
        }
    }

    private Map<Item, Integer> countAvailableMaterials(ServerLevel world, BlockPos workOrigin, BlockPos sitePos, Set<Item> items) {
        Map<Item, Integer> counts = new HashMap<>();
        countContainerMaterials(control.villager().getInventory(), control.getUsableInventorySlots(control.villager().getInventory()), items, counts);

        Set<BlockPos> scannedContainers = new HashSet<>();
        countNearbyContainerMaterials(world, workOrigin, items, counts, scannedContainers);
        countNearbyContainerMaterials(world, sitePos, items, counts, scannedContainers);
        return counts;
    }

    private void countNearbyContainerMaterials(ServerLevel world, BlockPos origin, Set<Item> items, Map<Item, Integer> counts, Set<BlockPos> scannedContainers) {
        for (BlockPos pos : BlockPos.withinManhattan(origin, control.materialRadius(), 8, control.materialRadius())) {
            BlockPos immutable = pos.immutable();
            if (!scannedContainers.add(immutable)) {
                continue;
            }

            BlockState blockState = world.getBlockState(immutable);
            if (!isMaterialContainer(blockState)) {
                continue;
            }

            Container inventory = ValetInventoryTransfer.getContainerInventory(world, immutable);
            if (inventory != null) {
                countContainerMaterials(inventory, inventory.getContainerSize(), items, counts);
            }
        }
    }

    private void countContainerMaterials(Container inventory, int maxSlots, Set<Item> items, Map<Item, Integer> counts) {
        int slots = Math.min(inventory.getContainerSize(), Math.max(0, maxSlots));
        for (int slot = 0; slot < slots; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty() && items.contains(stack.getItem())) {
                counts.merge(stack.getItem(), stack.getCount(), Integer::sum);
            }
        }
    }

    private Component formatMissingMaterials(List<MissingMaterial> missingMaterials) {
        MutableComponent text = Component.empty();
        int shown = Math.min(5, missingMaterials.size());
        for (int i = 0; i < shown; i++) {
            MissingMaterial material = missingMaterials.get(i);
            if (i > 0) {
                text.append(Component.literal(", "));
            }
            text.append(Component.translatable(material.item().getDescriptionId()))
                    .append(Component.literal(" x" + material.missingCount()));
        }
        if (missingMaterials.size() > shown) {
            text.append(Component.literal(", +" + (missingMaterials.size() - shown)));
        }
        return text;
    }

    private String missingMaterialsDebug(List<MissingMaterial> missingMaterials) {
        StringBuilder builder = new StringBuilder();
        int shown = Math.min(5, missingMaterials.size());
        for (int i = 0; i < shown; i++) {
            MissingMaterial material = missingMaterials.get(i);
            if (i > 0) {
                builder.append(",");
            }
            builder.append(material.item().getDescriptionId()).append("x").append(material.missingCount());
        }
        if (missingMaterials.size() > shown) {
            builder.append(",+").append(missingMaterials.size() - shown);
        }
        return builder.toString();
    }

    private boolean hasBuildMaterial(ServerLevel world, BlockPos origin, BuildTarget buildTarget) {
        return !needsBuildBlock(world, buildTarget.pos(), buildTarget.state())
                || hasBuildMaterial(world, origin, buildTarget.pos(), buildTarget.state());
    }

    private boolean hasBuildMaterial(ServerLevel world, BlockPos origin, BlockPos buildPos, BlockState buildState) {
        if (!requiresBuildMaterial(buildState)) {
            return true;
        }

        Item item = getBuildItem(buildState);
        return item != Items.AIR
                && (ValetInventoryTransfer.inventoryHasItem(control.villager().getInventory(), item, control.getUsableInventorySlots(control.villager().getInventory()))
                || findNearestContainerWithItem(world, buildPos, item) != null
                || findNearestContainerWithItem(world, origin, item) != null);
    }

    private boolean consumeBuildMaterial(ServerLevel world, BlockPos origin, BuildTarget buildTarget) {
        return !needsBuildBlock(world, buildTarget.pos(), buildTarget.state())
                || consumeBuildMaterial(world, origin, buildTarget.pos(), buildTarget.state());
    }

    private boolean consumeBuildMaterial(ServerLevel world, BlockPos origin, BlockPos buildPos, BlockState buildState) {
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
        Container inventory = ValetInventoryTransfer.getContainerInventory(world, containerPos);
        if (inventory == null) {
            return false;
        }

        boolean taken = ValetInventoryTransfer.takeOneItem(inventory, item, inventory.getContainerSize());
        if (taken) {
            control.animateChestUse(world, containerPos);
        }
        return taken;
    }

    private BlockPos findNearestContainerWithItem(ServerLevel world, BlockPos origin, Item item) {
        return findNearest(world, origin, control.materialRadius(), 8, pos -> {
            BlockState blockState = world.getBlockState(pos);
            Container inventory = ValetInventoryTransfer.getContainerInventory(world, pos);
            return isMaterialContainer(blockState)
                    && inventory != null
                    && ValetInventoryTransfer.inventoryHasItem(inventory, item, inventory.getContainerSize());
        });
    }

    private boolean isMaterialContainer(BlockState blockState) {
        return blockState.is(Blocks.CHEST) || blockState.is(Blocks.TRAPPED_CHEST) || blockState.is(Blocks.BARREL);
    }

    private BlockPos findNearest(ServerLevel world, BlockPos origin, int horizontalRadius, int verticalRadius, BlockPredicate predicate) {
        BlockPos nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (BlockPos pos : BlockPos.withinManhattan(origin, horizontalRadius, verticalRadius, horizontalRadius)) {
            BlockPos immutable = pos.immutable();
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
        if (buildState.getBlock() instanceof BedBlock && buildState.hasProperty(BedBlock.PART)) {
            return buildState.getValue(BedBlock.PART) == BedPart.FOOT;
        }
        if (buildState.getBlock() instanceof DoorBlock && buildState.hasProperty(DoorBlock.HALF)) {
            return buildState.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER;
        }
        return true;
    }

    private boolean isOptionalNaturalBlock(BlockState buildState) {
        return buildState.canBeReplaced()
                || buildState.is(BlockTags.LEAVES);
    }

    private void reportConstructionIssue(ServerLevel world, String messageKey, Object... args) {
        String signature = messageKey + Arrays.toString(args);
        if (constructionReportDelayTicks > 0 && signature.equals(lastConstructionReport)) {
            return;
        }

        constructionReportDelayTicks = CONSTRUCTION_REPORT_INTERVAL_TICKS;
        lastConstructionReport = signature;
        ValetMod.LOGGER.info("Valet construction: {} {}", messageKey, Arrays.toString(args));
        Component message = Component.translatable(messageKey, args);
        for (ServerPlayer player : world.players()) {
            if (player.distanceToSqr(control.villager()) <= 64.0D * 64.0D) {
                player.sendOverlayMessage(message);
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

    private record ConstructionSite(BlockPos blueprintPos, Direction facing, boolean mirrored) {
    }

    private record BuildTarget(BlockPos pos, BlockState state, BlockPos secondaryPos, BlockState secondaryState) {
    }

    private record MissingMaterial(Item item, int neededCount, int availableCount) {
        int missingCount() {
            return neededCount - availableCount;
        }
    }

    @FunctionalInterface
    private interface BlockPredicate {
        boolean test(BlockPos pos);
    }

    public interface Control {
        Villager villager();

        BlockPos getWorkOrigin(ServerLevel world);

        Set<BlockPos> findBuildStandGoals(ServerLevel world, BlockPos targetBlock);

        List<BlockPos> planPathToAdjacent(ServerLevel world, PathPurpose purpose, BlockPos targetBlock, Set<BlockPos> goals);

        void startPath(PathPurpose purpose, List<BlockPos> path);

        boolean canReachBuildTargetFromStand(BlockPos targetBlock, BlockPos stand);

        boolean hasInventoryItems();

        int materialRadius();

        int getUsableInventorySlots(Container inventory);

        int actionDelayTicks();

        void animateChestUse(ServerLevel world, BlockPos pos);

        void setState(State state);

        void setDelayTicks(int ticks);
    }
}
