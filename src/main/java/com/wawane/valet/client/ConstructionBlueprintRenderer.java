package com.wawane.valet.client;

import com.wawane.valet.construction.ConstructionBlueprintBlock;
import com.wawane.valet.construction.ConstructionBlueprintBlockEntity;
import com.wawane.valet.construction.ValetConstructionBlueprint;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class ConstructionBlueprintRenderer implements BlockEntityRenderer<ConstructionBlueprintBlockEntity> {
    private static final int MAX_RENDERED_BLOCKS = 6000;

    public ConstructionBlueprintRenderer(BlockEntityRendererFactory.Context context) {
    }

    @Override
    public void render(ConstructionBlueprintBlockEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        ValetConstructionBlueprint blueprint = entity.getBlueprint();
        World world = entity.getWorld();
        if (blueprint == null || world == null || blueprint.entries().isEmpty()) {
            return;
        }

        Direction facing = entity.getCachedState().contains(ConstructionBlueprintBlock.FACING)
                ? entity.getCachedState().get(ConstructionBlueprintBlock.FACING)
                : Direction.SOUTH;
        VertexConsumer lines = vertexConsumers.getBuffer(RenderLayer.getLines());
        float pulse = (float) ((Math.sin((world.getTime() + tickDelta) * 0.08F) + 1.0F) * 0.5F);
        float alpha = 0.45F + pulse * 0.25F;

        int rendered = 0;
        for (ValetConstructionBlueprint.Entry entry : blueprint.entries()) {
            if (rendered >= MAX_RENDERED_BLOCKS) {
                break;
            }

            BlockPos offset = getBuildOffset(facing, entry);
            BlockPos worldPos = entity.getPos().add(offset);
            BlockState targetState = rotateBuildState(entry.state(), facing);
            if (world.getBlockState(worldPos).equals(targetState)) {
                continue;
            }

            float x1 = offset.getX() + 0.04F;
            float y1 = offset.getY() + 0.04F;
            float z1 = offset.getZ() + 0.04F;
            float x2 = offset.getX() + 0.96F;
            float y2 = offset.getY() + 0.96F;
            float z2 = offset.getZ() + 0.96F;
            WorldRenderer.drawBox(matrices, lines, x1, y1, z1, x2, y2, z2, 0.45F, 0.9F, 1.0F, alpha);
            rendered++;
        }
    }

    @Override
    public boolean rendersOutsideBoundingBox(ConstructionBlueprintBlockEntity blockEntity) {
        return true;
    }

    private static BlockPos getBuildOffset(Direction facing, ValetConstructionBlueprint.Entry entry) {
        Direction side = facing.rotateYCounterclockwise();
        return BlockPos.ORIGIN
                .offset(facing)
                .offset(side, entry.x())
                .offset(facing, entry.z())
                .up(entry.y());
    }

    private static BlockState rotateBuildState(BlockState state, Direction facing) {
        return state.rotate(rotationFromSouth(facing));
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
