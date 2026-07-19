package com.wawane.valet.ai;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import com.wawane.valet.ai.path.ValetSafeNavigation;
import com.wawane.valet.ValetConversations;
import com.wawane.valet.ValetMod;
import com.wawane.valet.ValetAnchor;
import com.wawane.valet.group.ValetGroupRuntime;
import com.wawane.valet.order.ValetOrder;
import com.wawane.valet.order.ValetOrders;
import com.wawane.valet.state.ValetBehavior;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.InteractWithDoor;
import net.minecraft.world.entity.ai.behavior.LookAtTargetSink;
import net.minecraft.world.entity.ai.behavior.SetWalkTargetFromBlockMemory;
import net.minecraft.world.entity.ai.behavior.SleepInBed;
import net.minecraft.world.entity.ai.behavior.Swim;
import net.minecraft.world.entity.ai.behavior.WakeUp;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.schedule.Activity;

/**
 * Configuration Brain propre aux valets.
 *
 * <p>Le Brain devient l'unique proprietaire de l'ordonnancement du travail,
 * du repos et du pathfinding. La profession vanilla reste independante.</p>
 */
public final class ValetBrain {
    private static final float WALK_SPEED = 0.5F;
    private static final int BED_ASSIGNMENT_REFRESH_TICKS = 100;
    private static final Set<MemoryModuleType<?>> MOVEMENT_MEMORIES = Set.of(
            MemoryModuleType.WALK_TARGET,
            MemoryModuleType.LOOK_TARGET,
            MemoryModuleType.PATH,
            MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE
    );
    private static final Map<UUID, Runtime> RUNTIMES = new ConcurrentHashMap<>();

    private ValetBrain() {
    }

    public static void ensureConfigured(ServerLevel world, Villager villager) {
        if (!ValetMod.isValet(villager) || villager.isBaby() || villager.isRemoved()) {
            clear(villager.getUUID());
            return;
        }

        Brain<Villager> brain = villager.getBrain();
        Runtime current = RUNTIMES.get(villager.getUUID());
        if (current != null && current.brain == brain) {
            return;
        }
        if (current != null) {
            current.work.forceStop();
        }

        brain.stopAll(world, villager);
        brain.removeAllBehaviors();

        WorkBehavior work = new WorkBehavior(villager);
        ActivityController controller = new ActivityController();
        IdleBehavior idle = new IdleBehavior();

        brain.addActivity(
                Activity.CORE,
                ImmutableList.of(
                        pair(0, new Swim<Villager>(0.8F)),
                        pair(0, InteractWithDoor.create()),
                        pair(0, WakeUp.create()),
                        pair(1, new ValetBoundedMoveToTargetSink()),
                        pair(2, new LookAtTargetSink(45, 90)),
                        pair(10, controller)
                ),
                Set.of(),
                Set.of()
        );
        brain.addActivity(
                Activity.WORK,
                ImmutableList.of(pair(5, work)),
                Set.of(),
                MOVEMENT_MEMORIES
        );
        brain.addActivity(
                Activity.REST,
                ImmutableList.of(
                        pair(5, SetWalkTargetFromBlockMemory.create(MemoryModuleType.HOME, WALK_SPEED, 1, 150, 1200)),
                        pair(10, new SleepInBed())
                ),
                Set.of(),
                MOVEMENT_MEMORIES
        );
        brain.addActivity(
                Activity.IDLE,
                ImmutableList.of(pair(5, idle)),
                Set.of(),
                MOVEMENT_MEMORIES
        );
        brain.setCoreActivities(Set.of(Activity.CORE));
        brain.setDefaultActivity(Activity.WORK);
        brain.setActiveActivityIfPossible(Activity.WORK);

        RUNTIMES.put(villager.getUUID(), new Runtime(brain, work, controller));
        ValetAnchor.ensure(world, villager);
        ValetResidence.ensureAssigned(world, villager);
        ValetMod.suppressVanillaVillageMemories(villager);
    }

    /** Reprend la priorite apres VillagerAi.updateActivity, appele en fin de tick vanilla. */
    public static void reassertActivity(ServerLevel world, Villager villager) {
        Runtime runtime = RUNTIMES.get(villager.getUUID());
        if (runtime != null && runtime.brain == villager.getBrain()) {
            runtime.controller.reassert(world, villager);
        }
    }

    public static void clear(UUID uuid) {
        Runtime runtime = RUNTIMES.remove(uuid);
        if (runtime != null) {
            runtime.work.forceStop();
        }
        ValetWorkGoal.clearRestartRequest(uuid);
    }

    public static void clearAll() {
        for (Runtime runtime : RUNTIMES.values()) {
            runtime.work.forceStop();
        }
        RUNTIMES.clear();
    }

    public static String describe(Villager villager) {
        Runtime runtime = RUNTIMES.get(villager.getUUID());
        if (runtime == null || runtime.brain != villager.getBrain()) {
            return shortUuid(villager.getUUID()) + " brain=vanilla_or_pending";
        }
        return shortUuid(villager.getUUID())
                + " brain=valet activity=" + villager.getBrain().getActiveNonCoreActivity().map(Activity::getName).orElse("-")
                + " running=" + (runtime.work.status == Behavior.Status.RUNNING)
                + " " + runtime.work.goal.debugSummary();
    }

    static boolean isThreatened(Villager villager) {
        return villager.getBrain().getMemory(MemoryModuleType.NEAREST_HOSTILE)
                .filter(hostile -> hostile.isAlive() && villager.distanceToSqr(hostile) <= 256.0D)
                .isPresent()
                || villager.getLastHurtByMob() != null
                && villager.getLastHurtByMob().isAlive()
                && villager.distanceToSqr(villager.getLastHurtByMob()) <= 256.0D;
    }

    private static Pair<Integer, ? extends BehaviorControl<? super Villager>> pair(
            int priority,
            BehaviorControl<? super Villager> behavior
    ) {
        return Pair.of(priority, behavior);
    }

    private static String shortUuid(UUID uuid) {
        return uuid.toString().substring(0, 8);
    }

    private record Runtime(Brain<Villager> brain, WorkBehavior work, ActivityController controller) {
    }

    private static final class ActivityController implements BehaviorControl<Villager> {
        private Behavior.Status status = Behavior.Status.STOPPED;
        private int bedAssignmentCooldown;

        @Override
        public Behavior.Status getStatus() {
            return status;
        }

        @Override
        public Set<MemoryModuleType<?>> getRequiredMemories() {
            return Set.of();
        }

        @Override
        public boolean tryStart(ServerLevel world, Villager villager, long gameTime) {
            status = Behavior.Status.RUNNING;
            tick(world, villager);
            return true;
        }

        @Override
        public void tickOrStop(ServerLevel world, Villager villager, long gameTime) {
            if (status == Behavior.Status.RUNNING) {
                tick(world, villager);
            }
        }

        private void tick(ServerLevel world, Villager villager) {
            Activity desired = desiredActivity(world, villager);
            if (bedAssignmentCooldown-- <= 0 || desired == Activity.REST && !villager.getBrain().hasMemoryValue(MemoryModuleType.HOME)) {
                ValetResidence.ensureAssigned(world, villager);
                bedAssignmentCooldown = BED_ASSIGNMENT_REFRESH_TICKS;
            }
            villager.getBrain().setActiveActivityIfPossible(desired);
        }

        private void reassert(ServerLevel world, Villager villager) {
            villager.getBrain().setActiveActivityIfPossible(desiredActivity(world, villager));
        }

        private Activity desiredActivity(ServerLevel world, Villager villager) {
            boolean forcedWork = ValetConversations.isTalking(villager)
                    || ValetBehavior.isRecallActive(world, villager)
                    || ValetGroupRuntime.hasControllingCommand(world, villager)
                    || isThreatened(villager);
            Activity desired;
            if (forcedWork) {
                desired = Activity.WORK;
            } else if (world.isDarkOutside() && !ValetBehavior.shouldAvoidNightReturn(villager)) {
                desired = Activity.REST;
            } else if (ValetOrders.get(villager) != ValetOrder.NONE) {
                desired = Activity.WORK;
            } else if (ValetBehavior.isFreeBehavior(villager)) {
                desired = Activity.IDLE;
            } else {
                desired = Activity.WORK;
            }

            return desired;
        }

        @Override
        public void doStop(ServerLevel world, Villager villager, long gameTime) {
            status = Behavior.Status.STOPPED;
        }

        @Override
        public String debugString() {
            return "valet_activity_controller";
        }
    }

    private static final class WorkBehavior implements BehaviorControl<Villager> {
        private final ValetWorkGoal goal;
        private Behavior.Status status = Behavior.Status.STOPPED;

        private WorkBehavior(Villager villager) {
            this.goal = new ValetWorkGoal(villager);
        }

        @Override
        public Behavior.Status getStatus() {
            return status;
        }

        @Override
        public Set<MemoryModuleType<?>> getRequiredMemories() {
            return Set.of();
        }

        @Override
        public boolean tryStart(ServerLevel world, Villager villager, long gameTime) {
            if (!goal.canUse()) {
                return false;
            }
            goal.start();
            status = Behavior.Status.RUNNING;
            goal.tick();
            return true;
        }

        @Override
        public void tickOrStop(ServerLevel world, Villager villager, long gameTime) {
            if (status != Behavior.Status.RUNNING) {
                return;
            }
            if (!villager.getBrain().isActive(Activity.WORK) || !goal.canContinueToUse()) {
                doStop(world, villager, gameTime);
                return;
            }
            goal.tick();
        }

        @Override
        public void doStop(ServerLevel world, Villager villager, long gameTime) {
            forceStop();
        }

        private void forceStop() {
            if (status == Behavior.Status.RUNNING) {
                goal.stop();
            }
            status = Behavior.Status.STOPPED;
        }

        @Override
        public String debugString() {
            return "valet_work";
        }
    }

    private static final class IdleBehavior implements BehaviorControl<Villager> {
        private Behavior.Status status = Behavior.Status.STOPPED;

        @Override
        public Behavior.Status getStatus() {
            return status;
        }

        @Override
        public Set<MemoryModuleType<?>> getRequiredMemories() {
            return Set.of();
        }

        @Override
        public boolean tryStart(ServerLevel world, Villager villager, long gameTime) {
            status = Behavior.Status.RUNNING;
            return true;
        }

        @Override
        public void tickOrStop(ServerLevel world, Villager villager, long gameTime) {
            if (status != Behavior.Status.RUNNING) {
                return;
            }
            if (!villager.getBrain().isActive(Activity.IDLE)) {
                doStop(world, villager, gameTime);
                return;
            }
            if (gameTime % 20L != 0L) {
                return;
            }
            ValetWorkZone.zone(world, villager).ifPresentOrElse(zone -> {
                if (!zone.contains(villager.blockPosition())) {
                    villager.getBrain().setMemory(
                            MemoryModuleType.WALK_TARGET,
                            new net.minecraft.world.entity.ai.memory.WalkTarget(zone.anchor(), WALK_SPEED, 1)
                    );
                    return;
                }
                if (!villager.getNavigation().isDone()
                        || villager.getBrain().hasMemoryValue(MemoryModuleType.WALK_TARGET)) {
                    return;
                }
                for (int attempt = 0; attempt < 12; attempt++) {
                    BlockPos candidate = zone.clamp(villager.blockPosition().offset(
                            villager.getRandom().nextInt(13) - 6,
                            villager.getRandom().nextInt(5) - 2,
                            villager.getRandom().nextInt(13) - 6
                    ));
                    if (ValetSafeNavigation.isSafeStand(world, candidate, 2)) {
                        villager.getBrain().setMemory(
                                MemoryModuleType.WALK_TARGET,
                                new net.minecraft.world.entity.ai.memory.WalkTarget(candidate, WALK_SPEED, 1)
                        );
                        break;
                    }
                }
            }, () -> {
                villager.getNavigation().stop();
                for (MemoryModuleType<?> memory : MOVEMENT_MEMORIES) {
                    eraseUnchecked(villager.getBrain(), memory);
                }
            });
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        private static void eraseUnchecked(Brain<Villager> brain, MemoryModuleType<?> memory) {
            brain.eraseMemory((MemoryModuleType) memory);
        }

        @Override
        public void doStop(ServerLevel world, Villager villager, long gameTime) {
            status = Behavior.Status.STOPPED;
        }

        @Override
        public String debugString() {
            return "valet_bounded_idle";
        }
    }
}
