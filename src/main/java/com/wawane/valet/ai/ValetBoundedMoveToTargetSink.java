package com.wawane.valet.ai;

import com.wawane.valet.ValetDebug;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.behavior.MoveToTargetSink;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.entity.schedule.Activity;

/** MoveToTargetSink vanilla avec une limite de territoire pour les valets. */
public final class ValetBoundedMoveToTargetSink extends MoveToTargetSink {
    @Override
    protected boolean checkExtraStartConditions(ServerLevel world, Mob mob) {
        if (mob instanceof Villager villager && !hasBoundedTarget(world, villager)) {
            reject(villager, "target_outside_zone");
            return false;
        }
        return super.checkExtraStartConditions(world, mob);
    }

    @Override
    protected void start(ServerLevel world, Mob mob, long gameTime) {
        super.start(world, mob, gameTime);
        if (mob instanceof Villager villager && !hasBoundedPath(world, villager)) {
            reject(villager, "path_outside_zone");
        }
    }

    @Override
    protected void tick(ServerLevel world, Mob mob, long gameTime) {
        super.tick(world, mob, gameTime);
        if (mob instanceof Villager villager && !hasBoundedPath(world, villager)) {
            reject(villager, "repath_outside_zone");
        }
    }

    private boolean hasBoundedTarget(ServerLevel world, Villager villager) {
        ValetWorkZone.Zone zone = movementZone(world, villager);
        if (zone == null) {
            return false;
        }
        return villager.getBrain().getMemory(MemoryModuleType.WALK_TARGET)
                .map(target -> zone.contains(target.getTarget().currentBlockPosition()))
                .orElse(false);
    }

    private boolean hasBoundedPath(ServerLevel world, Villager villager) {
        ValetWorkZone.Zone zone = movementZone(world, villager);
        if (zone == null) {
            return false;
        }
        Path path = villager.getNavigation().getPath();
        if (path == null) {
            return !villager.getBrain().hasMemoryValue(MemoryModuleType.WALK_TARGET);
        }

        boolean enteredZone = zone.contains(villager.blockPosition());
        for (int index = 0; index < path.getNodeCount(); index++) {
            BlockPos node = path.getNodePos(index);
            boolean inside = zone.contains(node);
            if (enteredZone && !inside) {
                return false;
            }
            enteredZone |= inside;
        }
        return enteredZone;
    }

    private ValetWorkZone.Zone movementZone(ServerLevel world, Villager villager) {
        return villager.getBrain().isActive(Activity.REST)
                ? ValetWorkZone.residenceZone(world, villager)
                : ValetWorkZone.get(world, villager);
    }

    private void reject(Villager villager, String reason) {
        villager.getNavigation().stop();
        villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        villager.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
        villager.getBrain().eraseMemory(MemoryModuleType.PATH);
        ValetDebug.record(villager, "brain movement_rejected reason=" + reason);
    }
}
