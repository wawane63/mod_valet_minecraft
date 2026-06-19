package com.wawane.valet.client;

import com.wawane.valet.ValetMod;
import com.wawane.valet.ai.tasks.ConstructionTask;
import com.wawane.valet.construction.ConstructionBlueprintBlockEntity;
import com.wawane.valet.construction.ValetConstructionBlueprint;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public final class ConstructionBlueprintPlacementPreview {
    private static final int MAX_RENDERED_BLOCKS = 6000;

    private ConstructionBlueprintPlacementPreview() {
    }

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(ConstructionBlueprintPlacementPreview::render);
    }

    private static void render(WorldRenderContext context) {
        VertexConsumerProvider consumers = context.consumers();
        PreviewTarget target = findPreviewTarget();
        if (consumers == null || target == null || target.blueprint().entries().isEmpty()) {
            return;
        }

        VertexConsumer lines = consumers.getBuffer(RenderLayer.getLines());
        Vec3d camera = context.camera().getPos();
        float pulse = (float) ((Math.sin((context.world().getTime() + context.tickDelta()) * 0.08F) + 1.0F) * 0.5F);
        float alpha = 0.35F + pulse * 0.25F;

        drawBox(context, lines, camera, target.blueprintPos(), 0.95F, 0.75F, 0.25F, 0.75F);

        int rendered = 0;
        for (ValetConstructionBlueprint.Entry entry : target.blueprint().entries()) {
            if (rendered >= MAX_RENDERED_BLOCKS) {
                break;
            }

            BlockPos buildPos = ConstructionTask.getBuildPos(target.blueprintPos(), target.facing(), entry);
            BlockState targetState = ConstructionTask.rotateBuildState(entry.state(), target.facing());
            if (context.world().getBlockState(buildPos).equals(targetState)) {
                continue;
            }

            drawBox(context, lines, camera, buildPos, 0.45F, 0.9F, 1.0F, alpha);
            rendered++;
        }
    }

    private static void drawBox(WorldRenderContext context, VertexConsumer lines, Vec3d camera, BlockPos pos, float red, float green, float blue, float alpha) {
        double x1 = pos.getX() - camera.x + 0.04D;
        double y1 = pos.getY() - camera.y + 0.04D;
        double z1 = pos.getZ() - camera.z + 0.04D;
        double x2 = pos.getX() - camera.x + 0.96D;
        double y2 = pos.getY() - camera.y + 0.96D;
        double z2 = pos.getZ() - camera.z + 0.96D;
        WorldRenderer.drawBox(context.matrixStack(), lines, x1, y1, z1, x2, y2, z2, red, green, blue, alpha);
    }

    private static PreviewTarget findPreviewTarget() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null || !(client.crosshairTarget instanceof BlockHitResult hitResult) || hitResult.getType() != HitResult.Type.BLOCK) {
            return null;
        }

        for (Hand hand : Hand.values()) {
            ItemStack stack = client.player.getStackInHand(hand);
            ValetConstructionBlueprint blueprint = readBlueprint(stack);
            if (blueprint == null) {
                continue;
            }

            ItemPlacementContext placementContext = new ItemPlacementContext(client.player, hand, stack, hitResult);
            Direction facing = placementContext.getHorizontalPlayerFacing();
            return new PreviewTarget(blueprint, placementContext.getBlockPos(), facing);
        }
        return null;
    }

    private static ValetConstructionBlueprint readBlueprint(ItemStack stack) {
        if (!stack.isOf(ValetMod.CONSTRUCTION_BLUEPRINT_ITEM)) {
            return null;
        }

        NbtCompound nbt = stack.getNbt();
        if (nbt == null || !nbt.contains(ConstructionBlueprintBlockEntity.BLUEPRINT_KEY)) {
            return null;
        }
        return ValetConstructionBlueprint.readNbt(nbt.getCompound(ConstructionBlueprintBlockEntity.BLUEPRINT_KEY));
    }

    private record PreviewTarget(ValetConstructionBlueprint blueprint, BlockPos blueprintPos, Direction facing) {
    }
}
