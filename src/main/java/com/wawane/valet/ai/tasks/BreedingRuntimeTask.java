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
import net.minecraft.world.entity.animal.chicken.Chicken;
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
    private static final int BREED_PARENT_COOLDOWN_TICKS = 6000;
    private static final int MAX_PATH_FAILURES_BEFORE_BACKOFF = 5;
    private static final int AREA_VERTICAL_MARGIN = 2;
    private static final int CULL_ANIMALS_PER_SURFACE_BLOCK = 4;

    private final Control control;
    private final Map<UUID, Integer> failedTargets = new HashMap<>();
    private final Map<UUID, Integer> milkCooldowns = new HashMap<>();
    private final Map<UUID, Integer> breedCooldowns = new HashMap<>();
    private UUID primaryEntityUuid;
    private UUID secondaryEntityUuid;
    private BlockPos targetPos;
    private Action action = Action.NONE;
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
        Iterator<Map.Entry<UUID, Integer>> breedIterator = breedCooldowns.entrySet().iterator();
        while (breedIterator.hasNext()) {
            Map.Entry<UUID, Integer> entry = breedIterator.next();
            int ticks = entry.getValue() - 1;
            if (ticks <= 0) {
                breedIterator.remove();
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

        ValetAnimalArea area = control.getAnimalArea(world, ValetOrders.getAnimalAreaId(control.villager()));
        if (ValetOrders.getAnimalAreaId(control.villager()) >= 0 && area == null) {
            ValetDebug.record(control.villager(), "breeding unknown_area");
            ValetOrders.set(control.villager(), com.wawane.valet.order.ValetOrder.NONE);
            control.setState(State.RETURNING_HOME);
            return;
        }

        WorkTarget target = findWorkTarget(world, workOrigin, area);
        if (target == null) {
            releaseReservedEntities(world);
            ValetDebug.record(control.villager(), "breeding no_target animals=" + countAnimalsInScope(world, workOrigin, area));
            control.setState(control.hasInventoryItems() ? State.RETURNING : State.RETURNING_HOME);
            control.setDelayTicks(control.noTargetDelayTicks());
            return;
        }

        action = target.action();
        primaryEntityUuid = target.primaryUuid();
        secondaryEntityUuid = target.secondaryUuid();
        targetPos = target.pos();
        if (!claimTargetEntities(world)) {
            return;
        }

        Set<BlockPos> goals = control.findStandGoals(world, targetPos, PathPurpose.ANIMAL);
        if (goals.contains(control.currentStandPos(world)) || isNearTarget()) {
            control.setState(State.BREEDING);
            return;
        }

        List<BlockPos> path = control.planPathToAdjacent(world, PathPurpose.ANIMAL, targetPos, goals);
        if (path.isEmpty()) {
            pathFailures++;
            rememberFailedTarget(primaryEntityUuid);
            ValetDebug.record(control.villager(), "breeding no_path action=" + action + " target=" + ValetDebug.shortPos(targetPos));
            clearTarget(world);
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
        if (resolvePrimaryEntity(world) == null) {
            clearTarget(world);
            control.setState(control.interruptedWorkState());
            return;
        }
        control.setState(State.BREEDING);
    }

    public void tickBreeding(ServerLevel world) {
        Entity primary = resolvePrimaryEntity(world);
        if (primary == null || primary.isRemoved()) {
            clearTarget(world);
            control.setState(control.interruptedWorkState());
            return;
        }

        targetPos = primary.blockPosition();
        if (!isNearTarget()) {
            clearTarget(world);
            control.setState(State.FIND_TARGET);
            return;
        }

        boolean done = switch (action) {
            case BREED -> breedPair(world, asAnimal(primary), resolveSecondaryAnimal(world));
            case SHEAR -> shearSheep(world, primary);
            case COLLECT_EGG -> collectEgg(world, primary);
            case MILK -> milkCow(world, primary);
            case CULL -> cullAnimal(world, primary);
            case NONE -> false;
        };

        if (!done) {
            rememberFailedTarget(primaryEntityUuid);
        }
        clearTarget(world);
        control.setState(done && control.hasBreedingOrder() ? State.FIND_TARGET : control.interruptedWorkState());
        control.setDelayTicks(done ? control.actionDelayTicks() : 20);
    }

    public void clearTarget(ServerLevel world) {
        releaseReservedEntities(world);
        primaryEntityUuid = null;
        secondaryEntityUuid = null;
        targetPos = null;
        action = Action.NONE;
    }

    public void clearAll(ServerLevel world) {
        clearTarget(world);
        failedTargets.clear();
        milkCooldowns.clear();
        breedCooldowns.clear();
        pathFailures = 0;
    }

    public Item getDisplayItem() {
        return switch (action) {
            case SHEAR -> Items.SHEARS;
            case MILK -> Items.BUCKET;
            case COLLECT_EGG -> Items.EGG;
            case CULL -> Items.WOODEN_SWORD;
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
                + " failedAnimals=" + failedTargets.size();
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
        if (selectedArea != null) {
            return findCullTargetInArea(world, workOrigin, selectedArea);
        }

        List<ValetAnimalArea> areas = new ArrayList<>(control.getAnimalAreas(world));
        areas.sort(Comparator.comparingDouble(area -> squaredDistance(control.villager().blockPosition(), areaCenter(area))));
        for (ValetAnimalArea area : areas) {
            WorkTarget target = findCullTargetInArea(world, workOrigin, area);
            if (target != null) {
                return target;
            }
        }
        return null;
    }

    private WorkTarget findCullTargetInArea(ServerLevel world, BlockPos workOrigin, ValetAnimalArea area) {
        List<Animal> animals = animalsInScope(world, workOrigin, area, animal -> true);
        int areaCapacity = areaSurface(area) * CULL_ANIMALS_PER_SURFACE_BLOCK;
        int capacity = Math.max(1, Math.min(ValetOrders.getMaxAnimals(control.villager()), areaCapacity));
        if (animals.size() < capacity) {
            return null;
        }

        animals.sort(Comparator
                .comparing((Animal animal) -> !isBreedingCoolingDown(animal.getUUID()))
                .thenComparingDouble(animal -> squaredDistance(control.villager().blockPosition(), animal.blockPosition())));
        for (Animal animal : animals) {
            if (!animal.isBaby()
                    && !animal.isInLove()
                    && !isFailedTarget(animal.getUUID())
                    && !control.isEntityReservedByOther(world, animal)) {
                return new WorkTarget(Action.CULL, animal.getUUID(), null, animal.blockPosition());
            }
        }
        return null;
    }

    private WorkTarget findBreedTarget(ServerLevel world, BlockPos workOrigin, ValetAnimalArea area) {
        List<Animal> animals = animalsInScope(world, workOrigin, area, this::canParticipateInBreeding);
        animals.sort(Comparator.comparingDouble(animal -> squaredDistance(control.villager().blockPosition(), animal.blockPosition())));
        boolean capLogged = false;
        for (int firstIndex = 0; firstIndex < animals.size(); firstIndex++) {
            Animal first = animals.get(firstIndex);
            ValetAnimalType type = ValetAnimalType.fromAnimal(first);
            int animalCount = type == null ? 0 : countAnimals(world, workOrigin, area, type);
            int maxAnimals = ValetOrders.getMaxAnimals(control.villager());
            if (type == null
                    || isFailedTarget(first.getUUID())
                    || control.isEntityReservedByOther(world, first)
                    || animalCount >= maxAnimals) {
                if (!capLogged && type != null && animalCount >= maxAnimals) {
                    capLogged = true;
                    ValetDebug.record(control.villager(), "breeding cap_reached type=" + type.name().toLowerCase() + " animals=" + animalCount + " max=" + maxAnimals);
                }
                continue;
            }

            for (int secondIndex = firstIndex + 1; secondIndex < animals.size(); secondIndex++) {
                Animal second = animals.get(secondIndex);
                if (second.getUUID().equals(first.getUUID())
                        || !type.matches(second)
                        || control.isEntityReservedByOther(world, second)) {
                    continue;
                }
                if (!ensureFeedAvailable(world, workOrigin, type, feedNeeded(first, second))) {
                    continue;
                }
                return new WorkTarget(Action.BREED, first.getUUID(), second.getUUID(), first.blockPosition());
            }
        }
        return null;
    }

    private WorkTarget findShearTarget(ServerLevel world, BlockPos workOrigin, ValetAnimalArea area) {
        List<Animal> animals = animalsInScope(world, workOrigin, area, animal -> animal instanceof Sheep sheep && sheep.readyForShearing());
        animals.sort(Comparator.comparingDouble(animal -> squaredDistance(control.villager().blockPosition(), animal.blockPosition())));
        for (Animal animal : animals) {
            if (!isFailedTarget(animal.getUUID())
                    && !control.isEntityReservedByOther(world, animal)
                    && ensureItemAvailable(world, workOrigin, Items.SHEARS, 1)) {
                return new WorkTarget(Action.SHEAR, animal.getUUID(), null, animal.blockPosition());
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
                    && !control.isEntityReservedByOther(world, animal)
                    && ensureItemAvailable(world, workOrigin, Items.BUCKET, 1)) {
                return new WorkTarget(Action.MILK, animal.getUUID(), null, animal.blockPosition());
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
        return new WorkTarget(Action.COLLECT_EGG, egg.getUUID(), null, egg.blockPosition());
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

    private int countAnimals(ServerLevel world, BlockPos workOrigin, ValetAnimalArea area, ValetAnimalType type) {
        return animalsInScope(world, workOrigin, area, animal -> type.matches(animal)).size();
    }

    private int countAnimalsInScope(ServerLevel world, BlockPos workOrigin, ValetAnimalArea area) {
        return animalsInScope(world, workOrigin, area, animal -> true).size();
    }

    private AABB scopeBounds(BlockPos workOrigin, ValetAnimalArea area) {
        if (area != null) {
            return new AABB(
                    area.minX(),
                    area.minY() - AREA_VERTICAL_MARGIN,
                    area.minZ(),
                    area.maxX() + 1.0D,
                    area.maxY() + AREA_VERTICAL_MARGIN + 1.0D,
                    area.maxZ() + 1.0D
            );
        }
        int radius = control.animalRadius();
        int verticalRadius = control.animalVerticalRadius();
        return new AABB(
                workOrigin.getX() - radius,
                workOrigin.getY() - verticalRadius,
                workOrigin.getZ() - radius,
                workOrigin.getX() + radius + 1.0D,
                workOrigin.getY() + verticalRadius + 1.0D,
                workOrigin.getZ() + radius + 1.0D
        );
    }

    private boolean isInsideArea(BlockPos pos, ValetAnimalArea area) {
        return area == null
                || (pos.getX() >= area.minX()
                && pos.getX() <= area.maxX()
                && pos.getY() >= area.minY() - AREA_VERTICAL_MARGIN
                && pos.getY() <= area.maxY() + AREA_VERTICAL_MARGIN
                && pos.getZ() >= area.minZ()
                && pos.getZ() <= area.maxZ());
    }

    private boolean breedPair(ServerLevel world, Animal first, Animal second) {
        ValetAnimalType type = first == null ? null : ValetAnimalType.fromAnimal(first);
        if (first == null || second == null || type == null || !type.matches(second) || !canParticipateInBreeding(first) || !canParticipateInBreeding(second)) {
            ValetDebug.record(control.villager(), "breeding pair_invalid");
            return false;
        }

        int feedNeeded = feedNeeded(first, second);
        if (feedNeeded > 0 && !takeFeed(type, feedNeeded)) {
            ValetDebug.record(control.villager(), "breeding no_feed action=breed");
            return false;
        }

        lookAt(first);
        control.villager().swing(InteractionHand.MAIN_HAND);
        if (!first.isInLove()) {
            first.setInLove(null);
        }
        if (!second.isInLove()) {
            second.setInLove(null);
        }
        first.spawnChildFromBreeding(world, second);
        breedCooldowns.put(first.getUUID(), BREED_PARENT_COOLDOWN_TICKS);
        breedCooldowns.put(second.getUUID(), BREED_PARENT_COOLDOWN_TICKS);
        ValetProgress.addXp(control.villager(), 8);
        ValetDebug.record(control.villager(), "breeding bred type=" + type.name().toLowerCase() + " pos=" + ValetDebug.shortPos(first.blockPosition()));
        return true;
    }

    private boolean canParticipateInBreeding(Animal animal) {
        return animal != null && !animal.isBaby() && !isBreedingCoolingDown(animal.getUUID()) && (animal.canFallInLove() || animal.isInLove());
    }

    private int feedNeeded(Animal first, Animal second) {
        int needed = 0;
        if (!first.isInLove()) {
            needed++;
        }
        if (!second.isInLove()) {
            needed++;
        }
        return needed;
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

    private boolean cullAnimal(ServerLevel world, Entity entity) {
        if (!(entity instanceof Animal animal) || animal.isBaby()) {
            ValetDebug.record(control.villager(), "breeding cull_invalid");
            return false;
        }

        lookAt(animal);
        control.villager().swing(InteractionHand.MAIN_HAND);
        world.playSound(null, animal.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.NEUTRAL, 0.8F, 1.0F);
        animal.hurt(world.damageSources().mobAttack(control.villager()), animal.getMaxHealth() + 10.0F);
        control.collectNearbyItemEntities(world);
        ValetProgress.addXp(control.villager(), 2);
        ValetDebug.record(control.villager(), "breeding culled pos=" + ValetDebug.shortPos(animal.blockPosition()));
        return true;
    }

    private boolean ensureFeedAvailable(ServerLevel world, BlockPos workOrigin, ValetAnimalType type, int amount) {
        if (countFeed(type) >= amount) {
            return true;
        }
        restockFromNearbyContainers(world, workOrigin, stack -> isFeed(type, stack), amount - countFeed(type));
        return countFeed(type) >= amount;
    }

    private boolean ensureItemAvailable(ServerLevel world, BlockPos workOrigin, Item item, int amount) {
        if (countItem(item) >= amount) {
            return true;
        }
        restockFromNearbyContainers(world, workOrigin, stack -> stack.is(item), amount - countItem(item));
        return countItem(item) >= amount;
    }

    private void restockFromNearbyContainers(ServerLevel world, BlockPos workOrigin, Predicate<ItemStack> predicate, int amount) {
        if (amount <= 0) {
            return;
        }

        List<BlockPos> containers = collectContainers(world, workOrigin);
        Container target = control.villager().getInventory();
        int moved = 0;
        for (BlockPos containerPos : containers) {
            Container source = ValetInventoryTransfer.getContainerInventory(world, containerPos);
            if (source == null) {
                continue;
            }

            int movedHere = takeMatchingFromContainer(source, target, predicate, amount - moved);
            if (movedHere > 0) {
                moved += movedHere;
                control.animateChestUse(world, containerPos);
                ValetDebug.record(control.villager(), "breeding took_items count=" + movedHere + " chest=" + ValetDebug.shortPos(containerPos));
            }
            if (moved >= amount) {
                return;
            }
        }
    }

    private List<BlockPos> collectContainers(ServerLevel world, BlockPos workOrigin) {
        List<BlockPos> containers = new ArrayList<>();
        collectContainersAround(world, control.villager().blockPosition(), containers);
        collectContainersAround(world, workOrigin, containers);
        return containers;
    }

    private void collectContainersAround(ServerLevel world, BlockPos origin, List<BlockPos> containers) {
        if (origin == null) {
            return;
        }

        int radius = control.materialRadius();
        for (BlockPos pos : BlockPos.withinManhattan(origin, radius, 8, radius)) {
            BlockPos immutable = pos.immutable();
            if (containers.contains(immutable)) {
                continue;
            }
            BlockState state = world.getBlockState(immutable);
            if ((state.is(Blocks.CHEST) || state.is(Blocks.TRAPPED_CHEST) || state.is(Blocks.BARREL))
                    && ValetInventoryTransfer.getContainerInventory(world, immutable) != null) {
                containers.add(immutable);
            }
        }
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
                break;
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
            clearTarget(world);
            control.setState(control.interruptedWorkState());
            control.setDelayTicks(8);
            return false;
        }

        Entity secondary = resolveSecondaryEntity(world);
        if (secondary != null && !control.claimEntity(world, secondary, ENTITY_RESERVATION_TICKS)) {
            rememberFailedTarget(secondaryEntityUuid);
            clearTarget(world);
            control.setState(control.interruptedWorkState());
            control.setDelayTicks(8);
            return false;
        }
        return true;
    }

    private void releaseReservedEntities(ServerLevel world) {
        Entity primary = resolvePrimaryEntity(world);
        if (primary != null) {
            control.releaseEntity(primary);
        }
        Entity secondary = resolveSecondaryEntity(world);
        if (secondary != null) {
            control.releaseEntity(secondary);
        }
    }

    private Entity resolvePrimaryEntity(ServerLevel world) {
        return primaryEntityUuid == null ? null : world.getEntityInAnyDimension(primaryEntityUuid);
    }

    private Entity resolveSecondaryEntity(ServerLevel world) {
        return secondaryEntityUuid == null ? null : world.getEntityInAnyDimension(secondaryEntityUuid);
    }

    private Animal resolvePrimaryAnimal(ServerLevel world) {
        return asAnimal(resolvePrimaryEntity(world));
    }

    private Animal resolveSecondaryAnimal(ServerLevel world) {
        return asAnimal(resolveSecondaryEntity(world));
    }

    private Animal asAnimal(Entity entity) {
        return entity instanceof Animal animal ? animal : null;
    }

    private boolean isNearTarget() {
        return targetPos != null && squaredDistance(control.villager().blockPosition(), targetPos) <= 9.0D;
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

    private boolean isBreedingCoolingDown(UUID uuid) {
        return uuid != null && breedCooldowns.containsKey(uuid);
    }

    private static int areaSurface(ValetAnimalArea area) {
        return Math.max(1, area.maxX() - area.minX() + 1) * Math.max(1, area.maxZ() - area.minZ() + 1);
    }

    private static BlockPos areaCenter(ValetAnimalArea area) {
        return new BlockPos((area.minX() + area.maxX()) / 2, (area.minY() + area.maxY()) / 2, (area.minZ() + area.maxZ()) / 2);
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
        BREED,
        SHEAR,
        COLLECT_EGG,
        MILK,
        CULL
    }

    private record WorkTarget(Action action, UUID primaryUuid, UUID secondaryUuid, BlockPos pos) {
    }

    public interface Control {
        Villager villager();

        BlockPos getWorkOrigin(ServerLevel world);

        ValetAnimalArea getAnimalArea(ServerLevel world, int areaId);

        List<ValetAnimalArea> getAnimalAreas(ServerLevel world);

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

        void releaseEntity(Entity entity);

        State interruptedWorkState();

        void animateChestUse(ServerLevel world, BlockPos pos);

        void setState(State state);

        void setDelayTicks(int ticks);

        int noTargetDelayTicks();

        int animalRadius();

        int animalVerticalRadius();

        int materialRadius();

        int getUsableInventorySlots(Container inventory);

        int actionDelayTicks();
    }
}
