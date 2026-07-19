package com.wawane.valet.ai.tasks;

import com.wawane.valet.ValetDebug;
import com.wawane.valet.ai.ValetStateMachine.PathPurpose;
import com.wawane.valet.ai.ValetStateMachine.State;
import com.wawane.valet.ai.inventory.ValetInventoryTransfer;
import com.wawane.valet.breeding.ValetAnimalArea;
import com.wawane.valet.breeding.ValetAnimalType;
import com.wawane.valet.order.ValetOrders;
import com.wawane.valet.progress.ValetProgress;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public final class BreedingRuntimeTask {
    private static final int ENTITY_RESERVATION_TICKS = 600;
    private static final int FAILED_TARGET_MEMORY_TICKS = 240;
    private static final int MILK_COOLDOWN_TICKS = 6000;
    private static final int MAX_PATH_FAILURES_BEFORE_BACKOFF = 5;
    private static final int AREA_VERTICAL_MARGIN = 2;
    private static final int CONTAINER_MARGIN = 2;
    private static final int FEED_RESTOCK_BATCH = 16;
    private static final int CULL_ANIMALS_PER_SURFACE_BLOCK = 4;

    private final Control control;
    private final Map<UUID, Integer> failedTargets = new HashMap<>();
    private final Map<BlockPos, Integer> failedContainers = new HashMap<>();
    private final Map<UUID, Integer> milkCooldowns = new HashMap<>();
    private UUID primaryEntityUuid;
    private BlockPos targetPos;
    private Action action = Action.NONE;
    private ValetAnimalType restockFeedType;
    private Item restockItem;
    private int pathFailures;

    public BreedingRuntimeTask(Control control) {
        this.control = control;
    }

    public void tickCooldown() {
        Iterator<Map.Entry<UUID, Integer>> iterator = failedTargets.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Integer> entry = iterator.next();
            int ticks = entry.getValue() - 1;
            if (ticks <= 0) {
                iterator.remove();
            } else {
                entry.setValue(ticks);
            }
        }
        Iterator<Map.Entry<UUID, Integer>> milkIterator = milkCooldowns.entrySet().iterator();
        while (milkIterator.hasNext()) {
            Map.Entry<UUID, Integer> entry = milkIterator.next();
            int ticks = entry.getValue() - 1;
            if (ticks <= 0) {
                milkIterator.remove();
            } else {
                entry.setValue(ticks);
            }
        }
        Iterator<Map.Entry<BlockPos, Integer>> containerIterator = failedContainers.entrySet().iterator();
        while (containerIterator.hasNext()) {
            Map.Entry<BlockPos, Integer> entry = containerIterator.next();
            int ticks = entry.getValue() - 1;
            if (ticks <= 0) {
                containerIterator.remove();
            } else {
                entry.setValue(ticks);
            }
        }
    }

    public void findTarget(ServerLevel world) {
        BlockPos workOrigin = control.getWorkOrigin(world);
        if (workOrigin == null) {
            ValetDebug.record(control.villager(), "breeding no_work_origin");
            control.setDelayTicks(40);
            return;
        }

        int areaId = ValetOrders.getAnimalAreaId(control.villager());
        if (areaId < 0) {
            clearTarget();
            ValetDebug.record(control.villager(), "breeding no_assigned_area");
            control.setState(State.RETURNING_HOME);
            control.setDelayTicks(control.noTargetDelayTicks());
            return;
        }
        ValetAnimalArea area = control.getAnimalArea(world, areaId);
        if (area == null) {
            ValetDebug.record(control.villager(), "breeding unknown_area");
            ValetOrders.set(control.villager(), com.wawane.valet.order.ValetOrder.NONE);
            control.setState(State.RETURNING_HOME);
            return;
        }
        WorkTarget target = findWorkTarget(world, workOrigin, area);
        if (target == null) {
            releaseReservedEntities();
            ValetDebug.record(control.villager(), "breeding no_target animals=" + countAnimalsInScope(world, workOrigin, area));
            control.setState(control.hasInventoryItems() ? State.RETURNING : State.RETURNING_HOME);
            control.setDelayTicks(control.noTargetDelayTicks());
            return;
        }

        action = target.action();
        primaryEntityUuid = target.primaryUuid();
        targetPos = target.pos();
        restockFeedType = target.feedType();
        restockItem = target.item();
        if (!isRestockAction() && !claimTargetEntities(world)) {
            return;
        }

        Set<BlockPos> goals = control.findStandGoals(world, targetPos, PathPurpose.ANIMAL);
        if (goals.contains(control.currentStandPos(world))) {
            control.setState(State.BREEDING);
            return;
        }

        List<BlockPos> path = control.planPathToAdjacent(world, PathPurpose.ANIMAL, targetPos, goals);
        if (path.isEmpty()) {
            pathFailures++;
            rememberCurrentLocationFailure();
            ValetDebug.record(control.villager(), "breeding no_path action=" + action + " target=" + ValetDebug.shortPos(targetPos));
            clearTarget();
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
        control.startPath(PathPurpose.ANIMAL, path);
    }

    public void completePath(ServerLevel world) {
        if (!isRestockAction() && resolvePrimaryEntity(world) == null) {
            clearTarget();
            control.setState(control.interruptedWorkState());
            return;
        }
        control.setState(State.BREEDING);
    }

    public void rememberCurrentTargetFailure() {
        pathFailures++;
        rememberCurrentLocationFailure();
        clearTarget();
    }

    public void tickBreeding(ServerLevel world) {
        ValetAnimalArea area = currentAssignedArea(world);
        if (area == null) {
            rememberCurrentLocationFailure();
            clearTarget();
            control.setState(control.interruptedWorkState());
            return;
        }
        if (isRestockAction()) {
            tickRestocking(world, area);
            return;
        }

        Entity primary = resolvePrimaryEntity(world);
        if (primary == null || primary.isRemoved()) {
            clearTarget();
            control.setState(control.interruptedWorkState());
            return;
        }

        if (!isInsideArea(primary.blockPosition(), area)) {
            rememberFailedTarget(primaryEntityUuid);
            clearTarget();
            control.setState(control.interruptedWorkState());
            return;
        }

        targetPos = primary.blockPosition();
        if (!canInteractFromCurrentStand(world, targetPos)) {
            clearTarget();
            control.setState(State.FIND_TARGET);
            return;
        }

        boolean done = switch (action) {
            case BREED -> feedAnimalForBreeding(world, area, asAnimal(primary));
            case SHEAR -> shearSheep(world, primary);
            case COLLECT_EGG -> collectEgg(world, primary);
            case MILK -> milkCow(world, primary);
            case CULL -> cullAnimal(world, area, primary);
            case RESTOCK_FEED, RESTOCK_ITEM, NONE -> false;
        };

        if (!done) {
            rememberFailedTarget(primaryEntityUuid);
        }
        clearTarget();
        control.setState(done && control.hasBreedingOrder() ? State.FIND_TARGET : control.interruptedWorkState());
        control.setDelayTicks(done ? control.actionDelayTicks() : 20);
    }

    public void clearTarget() {
        releaseReservedEntities();
        primaryEntityUuid = null;
        targetPos = null;
        action = Action.NONE;
        restockFeedType = null;
        restockItem = null;
    }

    public void clearAll() {
        clearTarget();
        failedTargets.clear();
        failedContainers.clear();
        milkCooldowns.clear();
        pathFailures = 0;
    }

    public Item getDisplayItem() {
        return switch (action) {
            case SHEAR -> Items.SHEARS;
            case MILK -> Items.BUCKET;
            case COLLECT_EGG -> Items.EGG;
            case CULL -> Items.WOODEN_SWORD;
            case RESTOCK_FEED -> restockFeedType == null ? Items.WHEAT : restockFeedType.primaryFeedItem();
            case RESTOCK_ITEM -> restockItem == null ? Items.WHEAT : restockItem;
            case BREED -> {
                if (control.villager().level() instanceof ServerLevel world) {
                    Animal animal = resolvePrimaryAnimal(world);
                    ValetAnimalType type = animal == null ? null : ValetAnimalType.fromAnimal(animal);
                    yield type == null ? Items.WHEAT : type.primaryFeedItem();
                }
                yield Items.WHEAT;
            }
            case NONE -> Items.WHEAT;
        };
    }

    public String debugSummary() {
        return "animalAction=" + action
                + " animalTarget=" + shortPos(targetPos)
                + " failedAnimals=" + failedTargets.size()
                + " failedContainers=" + failedContainers.size();
    }

    private WorkTarget findWorkTarget(ServerLevel world, BlockPos workOrigin, ValetAnimalArea area) {
        if (ValetOrders.shouldCullAnimals(control.villager())) {
            WorkTarget target = findCullTarget(world, workOrigin, area);
            if (target != null) {
                return target;
            }
        }
        if (ValetOrders.shouldBreedAnimals(control.villager())) {
            WorkTarget target = findBreedTarget(world, workOrigin, area);
            if (target != null) {
                return target;
            }
        }
        if (ValetOrders.shouldShearAnimals(control.villager())) {
            WorkTarget target = findShearTarget(world, workOrigin, area);
            if (target != null) {
                return target;
            }
        }
        if (ValetOrders.shouldMilkAnimals(control.villager())) {
            WorkTarget target = findMilkTarget(world, workOrigin, area);
            if (target != null) {
                return target;
            }
        }
        if (ValetOrders.shouldCollectAnimalEggs(control.villager())) {
            WorkTarget target = findEggTarget(world, workOrigin, area);
            if (target != null) {
                return target;
            }
        }
        return null;
    }

    private WorkTarget findCullTarget(ServerLevel world, BlockPos workOrigin, ValetAnimalArea selectedArea) {
        if (!control.hasInventorySpace()) {
            return null;
        }
        return findCullTargetInArea(world, workOrigin, selectedArea);
    }

    private WorkTarget findCullTargetInArea(ServerLevel world, BlockPos workOrigin, ValetAnimalArea area) {
        List<Animal> animals = animalsInScope(world, workOrigin, area, animal -> true);
        int capacity = animalCapacity(area);
        if (animals.size() <= capacity) {
            return null;
        }

        animals.sort(Comparator.comparingDouble(animal -> squaredDistance(control.villager().blockPosition(), animal.blockPosition())));
        for (Animal animal : animals) {
            if (animal.getAge() == 0
                    && !animal.isInLove()
                    && !isFailedTarget(animal.getUUID())
                    && !control.isEntityReservedByOther(world, animal)) {
                return WorkTarget.entity(Action.CULL, animal);
            }
        }
        return null;
    }

    private WorkTarget findBreedTarget(ServerLevel world, BlockPos workOrigin, ValetAnimalArea area) {
        List<Animal> animals = animalsInScope(world, workOrigin, area, animal -> true);
        int capacity = animalCapacity(area);
        int projectedPopulation = projectedPopulation(animals);
        if (projectedPopulation >= capacity) {
            ValetDebug.record(control.villager(), "breeding cap_reached animals=" + animals.size()
                    + " projected=" + projectedPopulation + " max=" + capacity);
            return null;
        }
        animals.removeIf(animal -> !canFeedForBreeding(animal));
        int eligibleAnimals = animals.size();
        animals.sort(Comparator.comparingDouble(animal -> squaredDistance(control.villager().blockPosition(), animal.blockPosition())));
        List<BlockPos> containers = null;
        Set<ValetAnimalType> searchedFeedTypes = new HashSet<>();
        for (Animal animal : animals) {
            ValetAnimalType type = ValetAnimalType.fromAnimal(animal);
            if (type == null
                    || isFailedTarget(animal.getUUID())
                    || control.isEntityReservedByOther(world, animal)) {
                continue;
            }
            if (countFeed(type) >= 1) {
                return WorkTarget.entity(Action.BREED, animal);
            }
            if (!searchedFeedTypes.add(type)) {
                continue;
            }
            if (containers == null) {
                containers = collectContainers(world, area);
            }
            BlockPos container = findContainerWithItem(world, containers, stack -> isFeed(type, stack));
            if (container != null) {
                return WorkTarget.restockFeed(container, type);
            }
        }
        if (eligibleAnimals > 0) {
            ValetDebug.record(control.villager(), "breeding no_feed_source eligible=" + eligibleAnimals
                    + " containers=" + (containers == null ? 0 : containers.size())
                    + " excluded=" + failedContainers.size()
                    + " feedTypes=" + searchedFeedTypes.size());
        }
        return null;
    }

    private WorkTarget findShearTarget(ServerLevel world, BlockPos workOrigin, ValetAnimalArea area) {
        List<Animal> animals = animalsInScope(world, workOrigin, area, animal -> animal instanceof Sheep sheep && sheep.readyForShearing());
        animals.sort(Comparator.comparingDouble(animal -> squaredDistance(control.villager().blockPosition(), animal.blockPosition())));
        for (Animal animal : animals) {
            if (!isFailedTarget(animal.getUUID())
                    && !control.isEntityReservedByOther(world, animal)) {
                if (countItem(Items.SHEARS) >= 1) {
                    return WorkTarget.entity(Action.SHEAR, animal);
                }
                BlockPos container = findContainerWithItem(world, collectContainers(world, area), stack -> stack.is(Items.SHEARS));
                return container == null ? null : WorkTarget.restockItem(container, Items.SHEARS);
            }
        }
        return null;
    }

    private WorkTarget findMilkTarget(ServerLevel world, BlockPos workOrigin, ValetAnimalArea area) {
        List<Animal> animals = animalsInScope(world, workOrigin, area, animal -> animal instanceof Cow && !animal.isBaby());
        animals.sort(Comparator.comparingDouble(animal -> squaredDistance(control.villager().blockPosition(), animal.blockPosition())));
        for (Animal animal : animals) {
            if (!isFailedTarget(animal.getUUID())
                    && !isMilkCoolingDown(animal.getUUID())
                    && !control.isEntityReservedByOther(world, animal)) {
                if (countItem(Items.BUCKET) >= 1) {
                    return WorkTarget.entity(Action.MILK, animal);
                }
                BlockPos container = findContainerWithItem(world, collectContainers(world, area), stack -> stack.is(Items.BUCKET));
                return container == null ? null : WorkTarget.restockItem(container, Items.BUCKET);
            }
        }
        return null;
    }

    private WorkTarget findEggTarget(ServerLevel world, BlockPos workOrigin, ValetAnimalArea area) {
        AABB bounds = scopeBounds(workOrigin, area);
        List<ItemEntity> eggs = world.getEntitiesOfClass(ItemEntity.class, bounds, item ->
                item.isAlive()
                        && !item.isRemoved()
                        && item.getItem().is(Items.EGG)
                        && !isFailedTarget(item.getUUID())
                        && !control.isEntityReservedByOther(world, item)
                        && isInsideArea(item.blockPosition(), area)
                        && control.canStoreAllDrops(List.of(item.getItem())));
        eggs.sort(Comparator.comparingDouble(item -> squaredDistance(control.villager().blockPosition(), item.blockPosition())));
        if (eggs.isEmpty()) {
            return null;
        }

        ItemEntity egg = eggs.getFirst();
        return WorkTarget.entity(Action.COLLECT_EGG, egg);
    }

    private List<Animal> animalsInScope(ServerLevel world, BlockPos workOrigin, ValetAnimalArea area, Predicate<Animal> predicate) {
        ValetAnimalType areaType = area == null ? null : area.animalType();
        List<Animal> animals = new ArrayList<>();
        for (Animal animal : world.getEntitiesOfClass(Animal.class, scopeBounds(workOrigin, area), animal ->
                animal.isAlive()
                        && !animal.isRemoved()
                        && ValetAnimalType.fromAnimal(animal) != null
                        && (areaType == null || areaType.matches(animal))
                        && isInsideArea(animal.blockPosition(), area)
                        && predicate.test(animal))) {
            animals.add(animal);
        }
        return animals;
    }

    private int countAnimalsInScope(ServerLevel world, BlockPos workOrigin, ValetAnimalArea area) {
        return animalsInScope(world, workOrigin, area, animal -> true).size();
    }

    private AABB scopeBounds(BlockPos workOrigin, ValetAnimalArea area) {
        return new AABB(
                area.minX(),
                area.minY() - AREA_VERTICAL_MARGIN,
                area.minZ(),
                area.maxX() + 1.0D,
                area.maxY() + AREA_VERTICAL_MARGIN + 1.0D,
                area.maxZ() + 1.0D
        );
    }

    private boolean isInsideArea(BlockPos pos, ValetAnimalArea area) {
        return pos.getX() >= area.minX()
                && pos.getX() <= area.maxX()
                && pos.getY() >= area.minY() - AREA_VERTICAL_MARGIN
                && pos.getY() <= area.maxY() + AREA_VERTICAL_MARGIN
                && pos.getZ() >= area.minZ()
                && pos.getZ() <= area.maxZ();
    }

    private boolean feedAnimalForBreeding(ServerLevel world, ValetAnimalArea area, Animal animal) {
        ValetAnimalType type = animal == null ? null : ValetAnimalType.fromAnimal(animal);
        if (type == null || !canFeedForBreeding(animal)) {
            ValetDebug.record(control.villager(), "breeding animal_not_ready");
            return false;
        }
        List<Animal> animals = animalsInScope(world, control.getWorkOrigin(world), area, candidate -> true);
        int projectedPopulation = projectedPopulation(animals);
        int capacity = animalCapacity(area);
        if (projectedPopulation >= capacity) {
            ValetDebug.record(control.villager(), "breeding cap_changed projected=" + projectedPopulation + " max=" + capacity);
            return false;
        }
        if (!takeFeed(type, 1)) {
            ValetDebug.record(control.villager(), "breeding no_feed action=feed");
            return false;
        }
        lookAt(animal);
        control.villager().swing(InteractionHand.MAIN_HAND);
        animal.setInLove(null);
        ValetProgress.addXp(control.villager(), 2);
        ValetDebug.record(control.villager(), "breeding fed type=" + type.name().toLowerCase()
                + " pos=" + ValetDebug.shortPos(animal.blockPosition()));
        return true;
    }

    private boolean canFeedForBreeding(Animal animal) {
        return animal != null && animal.getAge() == 0 && !animal.isInLove() && animal.canFallInLove();
    }

    private boolean shearSheep(ServerLevel world, Entity entity) {
        if (!(entity instanceof Sheep sheep) || !sheep.readyForShearing()) {
            ValetDebug.record(control.villager(), "breeding shear_invalid");
            return false;
        }

        ItemStack shears = findInventoryStack(Items.SHEARS);
        if (shears.isEmpty()) {
            ValetDebug.record(control.villager(), "breeding no_shears");
            return false;
        }

        lookAt(sheep);
        control.villager().swing(InteractionHand.MAIN_HAND);
        sheep.shear(world, SoundSource.NEUTRAL, shears);
        control.collectNearbyItemEntities(world);
        ValetProgress.addXp(control.villager(), 4);
        ValetDebug.record(control.villager(), "breeding sheared pos=" + ValetDebug.shortPos(sheep.blockPosition()));
        return true;
    }

    private boolean collectEgg(ServerLevel world, Entity entity) {
        if (!(entity instanceof ItemEntity itemEntity) || !itemEntity.getItem().is(Items.EGG)) {
            ValetDebug.record(control.villager(), "breeding egg_invalid");
            return false;
        }

        if (!control.canStoreAllDrops(List.of(itemEntity.getItem()))) {
            control.setState(State.RETURNING);
            return false;
        }

        lookAt(itemEntity);
        control.villager().swing(InteractionHand.MAIN_HAND);
        ItemStack stack = itemEntity.getItem();
        control.insertStack(stack);
        if (stack.isEmpty()) {
            itemEntity.discard();
        }
        world.playSound(null, itemEntity.blockPosition(), SoundEvents.CHICKEN_EGG, SoundSource.NEUTRAL, 0.6F, 1.2F);
        ValetProgress.addXp(control.villager(), 2);
        ValetDebug.record(control.villager(), "breeding collected_egg pos=" + ValetDebug.shortPos(itemEntity.blockPosition()));
        return true;
    }

    private boolean milkCow(ServerLevel world, Entity entity) {
        if (!(entity instanceof Cow cow) || cow.isBaby()) {
            ValetDebug.record(control.villager(), "breeding milk_invalid");
            return false;
        }

        if (!takeItem(Items.BUCKET, 1)) {
            ValetDebug.record(control.villager(), "breeding no_bucket");
            return false;
        }

        ItemStack milk = new ItemStack(Items.MILK_BUCKET);
        if (!control.insertStack(milk)) {
            control.villager().spawnAtLocation(world, milk);
        }
        lookAt(cow);
        control.villager().swing(InteractionHand.MAIN_HAND);
        world.playSound(null, cow.blockPosition(), SoundEvents.COW_MILK, SoundSource.NEUTRAL, 0.8F, 1.0F);
        milkCooldowns.put(cow.getUUID(), MILK_COOLDOWN_TICKS);
        ValetProgress.addXp(control.villager(), 3);
        ValetDebug.record(control.villager(), "breeding milked pos=" + ValetDebug.shortPos(cow.blockPosition()));
        return true;
    }

    private boolean cullAnimal(ServerLevel world, ValetAnimalArea area, Entity entity) {
        if (!(entity instanceof Animal animal)
                || animal.getAge() != 0
                || animal.isInLove()
                || animalsInScope(world, control.getWorkOrigin(world), area, candidate -> true).size() <= animalCapacity(area)) {
            ValetDebug.record(control.villager(), "breeding cull_invalid");
            return false;
        }

        lookAt(animal);
        control.villager().swing(InteractionHand.MAIN_HAND);
        world.playSound(null, animal.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.NEUTRAL, 0.8F, 1.0F);
        if (!animal.hurtServer(world, world.damageSources().mobAttack(control.villager()), animal.getMaxHealth() + 10.0F)) {
            ValetDebug.record(control.villager(), "breeding cull_failed pos=" + ValetDebug.shortPos(animal.blockPosition()));
            return false;
        }
        control.collectNearbyItemEntities(world);
        ValetProgress.addXp(control.villager(), 2);
        ValetDebug.record(control.villager(), "breeding culled pos=" + ValetDebug.shortPos(animal.blockPosition()));
        return true;
    }

    private void tickRestocking(ServerLevel world, ValetAnimalArea area) {
        BlockPos containerPos = targetPos;
        if (containerPos == null
                || !isInsideLogisticsArea(containerPos, area)
                || !canInteractFromCurrentStand(world, containerPos)) {
            rememberCurrentLocationFailure();
            clearTarget();
            control.setState(State.FIND_TARGET);
            control.setDelayTicks(20);
            return;
        }

        Predicate<ItemStack> predicate = action == Action.RESTOCK_FEED && restockFeedType != null
                ? stack -> isFeed(restockFeedType, stack)
                : stack -> restockItem != null && stack.is(restockItem);
        Container source = ValetInventoryTransfer.getContainerInventory(world, containerPos);
        int moved = source == null ? 0 : takeMatchingFromContainer(
                source,
                control.villager().getInventory(),
                predicate,
                action == Action.RESTOCK_FEED ? FEED_RESTOCK_BATCH : 1
        );
        boolean done = moved > 0;
        if (done) {
            control.animateChestUse(world, containerPos);
            ValetDebug.record(control.villager(), "breeding restocked count=" + moved + " chest=" + ValetDebug.shortPos(containerPos));
        } else {
            rememberCurrentLocationFailure();
            ValetDebug.record(control.villager(), "breeding restock_failed chest=" + ValetDebug.shortPos(containerPos));
        }
        clearTarget();
        control.setState(done && control.hasBreedingOrder() ? State.FIND_TARGET : control.interruptedWorkState());
        control.setDelayTicks(done ? control.actionDelayTicks() : 20);
    }

    private BlockPos findContainerWithItem(
            ServerLevel world,
            List<BlockPos> containers,
            Predicate<ItemStack> predicate
    ) {
        for (BlockPos containerPos : containers) {
            if (failedContainers.containsKey(containerPos)) {
                continue;
            }
            Container source = ValetInventoryTransfer.getContainerInventory(world, containerPos);
            if (source == null) {
                continue;
            }
            for (int slot = 0; slot < source.getContainerSize(); slot++) {
                ItemStack stack = source.getItem(slot);
                if (!stack.isEmpty() && predicate.test(stack)) {
                    return containerPos;
                }
            }
        }
        return null;
    }

    private List<BlockPos> collectContainers(ServerLevel world, ValetAnimalArea area) {
        List<BlockPos> containers = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        for (int chunkX = area.minX() - CONTAINER_MARGIN >> 4; chunkX <= area.maxX() + CONTAINER_MARGIN >> 4; chunkX++) {
            for (int chunkZ = area.minZ() - CONTAINER_MARGIN >> 4; chunkZ <= area.maxZ() + CONTAINER_MARGIN >> 4; chunkZ++) {
                if (!world.hasChunk(chunkX, chunkZ)) {
                    continue;
                }
                for (BlockPos pos : world.getChunk(chunkX, chunkZ).getBlockEntities().keySet()) {
                    if (!isInsideLogisticsArea(pos, area)) {
                        continue;
                    }
                    BlockState state = world.getBlockState(pos);
                    if (!state.is(Blocks.CHEST) && !state.is(Blocks.TRAPPED_CHEST) && !state.is(Blocks.BARREL)) {
                        continue;
                    }
                    BlockPos canonical = ValetInventoryTransfer.canonicalContainerPos(world, pos);
                    if (isInsideLogisticsArea(canonical, area)
                            && seen.add(canonical)
                            && ValetInventoryTransfer.getContainerInventory(world, canonical) != null) {
                        containers.add(canonical);
                    }
                }
            }
        }
        containers.sort(Comparator.comparingDouble(pos -> squaredDistance(control.villager().blockPosition(), pos)));
        return containers;
    }

    private boolean isInsideLogisticsArea(BlockPos pos, ValetAnimalArea area) {
        return pos.getX() >= area.minX() - CONTAINER_MARGIN
                && pos.getX() <= area.maxX() + CONTAINER_MARGIN
                && pos.getY() >= area.minY() - AREA_VERTICAL_MARGIN - CONTAINER_MARGIN
                && pos.getY() <= area.maxY() + AREA_VERTICAL_MARGIN + CONTAINER_MARGIN
                && pos.getZ() >= area.minZ() - CONTAINER_MARGIN
                && pos.getZ() <= area.maxZ() + CONTAINER_MARGIN;
    }

    private int takeMatchingFromContainer(Container source, Container target, Predicate<ItemStack> predicate, int amount) {
        int movedTotal = 0;
        for (int slot = 0; slot < source.getContainerSize() && movedTotal < amount; slot++) {
            ItemStack sourceStack = source.getItem(slot);
            if (sourceStack.isEmpty() || !predicate.test(sourceStack)) {
                continue;
            }

            int requested = Math.min(sourceStack.getCount(), amount - movedTotal);
            ItemStack moving = sourceStack.copy();
            moving.setCount(requested);
            ValetInventoryTransfer.insertStack(target, moving, control.getUsableInventorySlots(target));
            int moved = requested - moving.getCount();
            if (moved <= 0) {
                continue;
            }

            sourceStack.shrink(moved);
            if (sourceStack.isEmpty()) {
                source.setItem(slot, ItemStack.EMPTY);
            }
            movedTotal += moved;
        }
        if (movedTotal > 0) {
            source.setChanged();
            target.setChanged();
        }
        return movedTotal;
    }

    private boolean takeFeed(ValetAnimalType type, int amount) {
        if (countFeed(type) < amount) {
            return false;
        }
        for (int i = 0; i < amount; i++) {
            boolean taken = false;
            for (Item item : type.feedItems()) {
                if (takeItem(item, 1)) {
                    taken = true;
                    break;
                }
            }
            if (!taken) {
                return false;
            }
        }
        return true;
    }

    private boolean takeItem(Item item, int amount) {
        Container inventory = control.villager().getInventory();
        int remaining = amount;
        for (int slot = 0; slot < control.getUsableInventorySlots(inventory) && remaining > 0; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.is(item)) {
                continue;
            }

            int moved = Math.min(stack.getCount(), remaining);
            stack.shrink(moved);
            if (stack.isEmpty()) {
                inventory.setItem(slot, ItemStack.EMPTY);
            }
            remaining -= moved;
        }
        if (remaining < amount) {
            inventory.setChanged();
        }
        return remaining == 0;
    }

    private int countFeed(ValetAnimalType type) {
        int count = 0;
        for (Item item : type.feedItems()) {
            count += countItem(item);
        }
        return count;
    }

    private int countItem(Item item) {
        Container inventory = control.villager().getInventory();
        int count = 0;
        for (int slot = 0; slot < control.getUsableInventorySlots(inventory); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private ItemStack findInventoryStack(Item item) {
        Container inventory = control.villager().getInventory();
        for (int slot = 0; slot < control.getUsableInventorySlots(inventory); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(item)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private boolean isFeed(ValetAnimalType type, ItemStack stack) {
        for (Item item : type.feedItems()) {
            if (stack.is(item)) {
                return true;
            }
        }
        return false;
    }

    private boolean claimTargetEntities(ServerLevel world) {
        Entity primary = resolvePrimaryEntity(world);
        if (primary == null || !control.claimEntity(world, primary, ENTITY_RESERVATION_TICKS)) {
            rememberFailedTarget(primaryEntityUuid);
            clearTarget();
            control.setState(control.interruptedWorkState());
            control.setDelayTicks(8);
            return false;
        }

        return true;
    }

    private void releaseReservedEntities() {
        if (primaryEntityUuid != null) {
            control.releaseEntity(primaryEntityUuid);
        }
    }

    private Entity resolvePrimaryEntity(ServerLevel world) {
        return resolveEntity(world, primaryEntityUuid);
    }

    private static Entity resolveEntity(ServerLevel world, UUID uuid) {
        if (uuid == null) {
            return null;
        }
        Entity entity = world.getEntityInAnyDimension(uuid);
        return entity != null && entity.level() == world ? entity : null;
    }

    private Animal resolvePrimaryAnimal(ServerLevel world) {
        return asAnimal(resolvePrimaryEntity(world));
    }

    private Animal asAnimal(Entity entity) {
        return entity instanceof Animal animal ? animal : null;
    }

    private boolean canInteractFromCurrentStand(ServerLevel world, BlockPos target) {
        return target != null
                && control.findStandGoals(world, target, PathPurpose.ANIMAL)
                .contains(control.currentStandPos(world));
    }

    private boolean isRestockAction() {
        return action == Action.RESTOCK_FEED || action == Action.RESTOCK_ITEM;
    }

    private void rememberCurrentLocationFailure() {
        if (isRestockAction() && targetPos != null) {
            failedContainers.put(targetPos.immutable(), FAILED_TARGET_MEMORY_TICKS);
        } else {
            rememberFailedTarget(primaryEntityUuid);
        }
    }

    private void lookAt(Entity entity) {
        control.villager().getLookControl().setLookAt(entity.getX(), entity.getEyeY(), entity.getZ());
    }

    private void rememberFailedTarget(UUID uuid) {
        if (uuid != null) {
            failedTargets.put(uuid, FAILED_TARGET_MEMORY_TICKS);
        }
    }

    private boolean isFailedTarget(UUID uuid) {
        return uuid != null && failedTargets.containsKey(uuid);
    }

    private boolean isMilkCoolingDown(UUID uuid) {
        return uuid != null && milkCooldowns.containsKey(uuid);
    }

    private ValetAnimalArea currentAssignedArea(ServerLevel world) {
        int areaId = ValetOrders.getAnimalAreaId(control.villager());
        return areaId < 0 ? null : control.getAnimalArea(world, areaId);
    }

    private static int areaSurface(ValetAnimalArea area) {
        return Math.max(1, area.maxX() - area.minX() + 1) * Math.max(1, area.maxZ() - area.minZ() + 1);
    }

    private int animalCapacity(ValetAnimalArea area) {
        int surfaceCapacity = areaSurface(area) * CULL_ANIMALS_PER_SURFACE_BLOCK;
        return Math.max(1, Math.min(ValetOrders.getMaxAnimals(control.villager()), surfaceCapacity));
    }

    private int projectedPopulation(List<Animal> animals) {
        Map<ValetAnimalType, Integer> animalsInLove = new HashMap<>();
        for (Animal animal : animals) {
            ValetAnimalType type = ValetAnimalType.fromAnimal(animal);
            if (type != null && animal.isInLove()) {
                animalsInLove.merge(type, 1, Integer::sum);
            }
        }
        int pendingBirths = animalsInLove.values().stream().mapToInt(count -> count / 2).sum();
        return animals.size() + pendingBirths;
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

    private enum Action {
        NONE,
        RESTOCK_FEED,
        RESTOCK_ITEM,
        BREED,
        SHEAR,
        COLLECT_EGG,
        MILK,
        CULL
    }

    private record WorkTarget(Action action, UUID primaryUuid, BlockPos pos, ValetAnimalType feedType, Item item) {
        private static WorkTarget entity(Action action, Entity entity) {
            return new WorkTarget(action, entity.getUUID(), entity.blockPosition(), null, null);
        }

        private static WorkTarget restockFeed(BlockPos pos, ValetAnimalType type) {
            return new WorkTarget(Action.RESTOCK_FEED, null, pos, type, null);
        }

        private static WorkTarget restockItem(BlockPos pos, Item item) {
            return new WorkTarget(Action.RESTOCK_ITEM, null, pos, null, item);
        }
    }

    public interface Control {
        Villager villager();

        BlockPos getWorkOrigin(ServerLevel world);

        ValetAnimalArea getAnimalArea(ServerLevel world, int areaId);

        boolean hasBreedingOrder();

        boolean hasInventorySpace();

        boolean hasInventoryItems();

        boolean canStoreAllDrops(List<ItemStack> drops);

        boolean insertStack(ItemStack stack);

        void collectNearbyItemEntities(ServerLevel world);

        BlockPos currentStandPos(ServerLevel world);

        Set<BlockPos> findStandGoals(ServerLevel world, BlockPos targetBlock, PathPurpose purpose);

        List<BlockPos> planPathToAdjacent(ServerLevel world, PathPurpose purpose, BlockPos targetBlock, Set<BlockPos> goals);

        void startPath(PathPurpose purpose, List<BlockPos> path);

        boolean claimEntity(ServerLevel world, Entity entity, int ttlTicks);

        boolean isEntityReservedByOther(ServerLevel world, Entity entity);

        void releaseEntity(UUID entityUuid);

        State interruptedWorkState();

        void animateChestUse(ServerLevel world, BlockPos pos);

        void setState(State state);

        void setDelayTicks(int ticks);

        int noTargetDelayTicks();

        int getUsableInventorySlots(Container inventory);

        int actionDelayTicks();
    }
}
