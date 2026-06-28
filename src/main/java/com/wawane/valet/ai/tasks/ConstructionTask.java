package com.wawane.valet.ai.tasks;

import com.wawane.valet.construction.ValetConstructionBlueprint;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

public final class ConstructionTask {
    private ConstructionTask() {
    }

    public static BlockPos getBuildPos(BlockPos blueprintPos, Direction facing, ValetConstructionBlueprint.Entry entry) {
        Direction side = facing.getCounterClockWise();
        return blueprintPos
                .relative(facing)
                .relative(side, entry.x())
                .relative(facing, entry.z())
                .above(entry.y());
    }

    public static BlockState rotateBuildState(BlockState state, Direction facing) {
        return state.rotate(rotationFromSouth(facing));
    }

    public static boolean isSecondaryBuildPart(BlockState state) {
        if (state.getBlock() instanceof BedBlock && state.hasProperty(BedBlock.PART)) {
            return state.getValue(BedBlock.PART) == BedPart.HEAD;
        }
        if (state.getBlock() instanceof DoorBlock && state.hasProperty(DoorBlock.HALF)) {
            return state.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER;
        }
        return false;
    }

    private static Rotation rotationFromSouth(Direction facing) {
        return switch (facing) {
            case NORTH -> Rotation.CLOCKWISE_180;
            case EAST -> Rotation.COUNTERCLOCKWISE_90;
            case WEST -> Rotation.CLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }
}
