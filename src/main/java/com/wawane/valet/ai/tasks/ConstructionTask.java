package com.wawane.valet.ai.tasks;

import com.wawane.valet.construction.ValetConstructionBlueprint;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.enums.BedPart;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.state.property.Properties;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public final class ConstructionTask {
    private ConstructionTask() {
    }

    public static BlockPos getBuildPos(BlockPos blueprintPos, Direction facing, ValetConstructionBlueprint.Entry entry) {
        Direction side = facing.rotateYCounterclockwise();
        return blueprintPos
                .offset(facing)
                .offset(side, entry.x())
                .offset(facing, entry.z())
                .up(entry.y());
    }

    public static BlockState rotateBuildState(BlockState state, Direction facing) {
        return state.rotate(rotationFromSouth(facing));
    }

    public static boolean isSecondaryBuildPart(BlockState state) {
        if (state.getBlock() instanceof BedBlock && state.contains(BedBlock.PART)) {
            return state.get(BedBlock.PART) == BedPart.HEAD;
        }
        if (state.getBlock() instanceof DoorBlock && state.contains(DoorBlock.HALF)) {
            return state.get(DoorBlock.HALF) == DoubleBlockHalf.UPPER;
        }
        return false;
    }

    private static BlockRotation rotationFromSouth(Direction facing) {
        return switch (facing) {
            case NORTH -> BlockRotation.CLOCKWISE_180;
            case EAST -> BlockRotation.COUNTERCLOCKWISE_90;
            case WEST -> BlockRotation.CLOCKWISE_90;
            default -> BlockRotation.NONE;
        };
    }
}
