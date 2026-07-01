package com.wawane.valet.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wawane.valet.ValetMod;
import com.wawane.valet.ai.tasks.ConstructionTask;
import com.wawane.valet.construction.ConstructionBlueprintBlockEntity;
import com.wawane.valet.construction.ConstructionBlueprintNbt;
import com.wawane.valet.construction.ValetConstructionBlueprint;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ARGB;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;

public final class ConstructionBlueprintPlacementPreview {
    private static final int MAX_RENDERED_BLOCKS = 6000;
    private static final int BLUEPRINT_COLOR = ARGB.color(210, 242, 192, 64);
    private static final int BUILD_COLOR = ARGB.color(170, 95, 225, 255);

    private ConstructionBlueprintPlacementPreview() {
    }

    public static void register() {
        LevelRenderEvents.COLLECT_SUBMITS.register(ConstructionBlueprintPlacementPreview::render);
    }

    private static void render(LevelRenderContext context) {
        Minecraft client = Minecraft.getInstance();
        PreviewTarget target = findPreviewTarget();
        if (client.level == null || target == null || target.blueprint().entries().isEmpty() || context.submitNodeCollector() == null) {
            return;
        }

        Vec3 camera = client.gameRenderer.mainCamera().position();
        float pulse = (float) ((Math.sin(client.level.getGameTime() * 0.08F) + 1.0F) * 0.5F);
        float lineWidth = 1.0F + pulse;

        drawBox(context, camera, target.blueprintPos(), BLUEPRINT_COLOR, lineWidth);

        int rendered = 0;
        for (ValetConstructionBlueprint.Entry entry : target.blueprint().entries()) {
            if (rendered >= MAX_RENDERED_BLOCKS) {
                break;
            }

            BlockPos buildPos = ConstructionTask.getBuildPos(target.blueprintPos(), target.facing(), entry);
            BlockState targetState = ConstructionTask.rotateBuildState(entry.state(), target.facing());
            if (client.level.getBlockState(buildPos).equals(targetState)) {
                continue;
            }

            drawBox(context, camera, buildPos, BUILD_COLOR, lineWidth);
            rendered++;
        }
    }

    private static void drawBox(LevelRenderContext context, Vec3 camera, BlockPos pos, int color, float lineWidth) {
        PoseStack matrices = context.poseStack();
        matrices.pushPose();
        matrices.translate(pos.getX() - camera.x + 0.04D, pos.getY() - camera.y + 0.04D, pos.getZ() - camera.z + 0.04D);
        matrices.scale(0.92F, 0.92F, 0.92F);
        context.submitNodeCollector().submitShapeOutline(matrices, Shapes.block(), RenderTypes.linesTranslucent(), color, lineWidth, false);
        matrices.popPose();
    }

    private static PreviewTarget findPreviewTarget() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null
                || client.level == null
                || !(client.hitResult instanceof BlockHitResult hitResult)
                || hitResult.getType() != HitResult.Type.BLOCK) {
            return null;
        }

        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = client.player.getItemInHand(hand);
            ValetConstructionBlueprint blueprint = readBlueprint(stack);
            if (blueprint == null) {
                continue;
            }

            BlockPlaceContext placementContext = new BlockPlaceContext(client.player, hand, stack, hitResult);
            Direction facing = placementContext.getHorizontalDirection();
            return new PreviewTarget(blueprint, placementContext.getClickedPos(), facing);
        }
        return null;
    }

    private static ValetConstructionBlueprint readBlueprint(ItemStack stack) {
        if (!stack.is(ValetMod.CONSTRUCTION_BLUEPRINT_ITEM)) {
            return null;
        }

        CompoundTag nbt = ConstructionBlueprintNbt.get(stack);
        if (nbt == null || !nbt.contains(ConstructionBlueprintBlockEntity.BLUEPRINT_KEY)) {
            return null;
        }
        return nbt.getCompound(ConstructionBlueprintBlockEntity.BLUEPRINT_KEY)
                .map(ValetConstructionBlueprint::readNbt)
                .orElse(null);
    }

    private record PreviewTarget(ValetConstructionBlueprint blueprint, BlockPos blueprintPos, Direction facing) {
    }
}
