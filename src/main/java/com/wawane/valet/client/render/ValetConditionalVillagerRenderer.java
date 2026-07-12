package com.wawane.valet.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wawane.valet.ValetMod;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.VillagerRenderer;
import net.minecraft.client.renderer.entity.layers.PlayerItemInHandLayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import java.util.HashMap;
import java.util.Map;

public final class ValetConditionalVillagerRenderer extends EntityRenderer<Villager, ValetConditionalVillagerRenderer.State> {
    private static final Map<Integer, Long> MAGIC_CASTS = new HashMap<>();
    private static final long MAGIC_CAST_ANIMATION_NANOS = 650_000_000L;
    private final VillagerRenderer vanillaRenderer;
    private final ValetAvatarRenderer valetRenderer;

    public ValetConditionalVillagerRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.vanillaRenderer = new VillagerRenderer(context);
        this.valetRenderer = new ValetAvatarRenderer(context);
    }

    @Override
    public State createRenderState() {
        return new State(vanillaRenderer.createRenderState(), valetRenderer.createRenderState());
    }

    @Override
    public void extractRenderState(Villager villager, State state, float tickProgress) {
        super.extractRenderState(villager, state, tickProgress);
        state.useValetRenderer = shouldUseValetRenderer(villager);
        if (state.useValetRenderer) {
            valetRenderer.extractRenderState(villager, state.valetState, tickProgress);
        } else {
            vanillaRenderer.extractRenderState(villager, state.villagerState, tickProgress);
        }
    }

    @Override
    public boolean shouldRender(Villager villager, net.minecraft.client.renderer.culling.Frustum frustum, double x, double y, double z) {
        return shouldUseValetRenderer(villager)
                ? valetRenderer.shouldRender(villager, frustum, x, y, z)
                : vanillaRenderer.shouldRender(villager, frustum, x, y, z);
    }

    @Override
    public Vec3 getRenderOffset(State state) {
        return state.useValetRenderer
                ? valetRenderer.getRenderOffset(state.valetState)
                : vanillaRenderer.getRenderOffset(state.villagerState);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        if (state.useValetRenderer) {
            valetRenderer.submit(state.valetState, poseStack, collector, cameraState);
        } else {
            vanillaRenderer.submit(state.villagerState, poseStack, collector, cameraState);
        }
    }

    private static boolean shouldUseValetRenderer(Villager villager) {
        return ValetMod.isValet(villager);
    }

    public static void markMagicCast(int entityId) {
        long now = System.nanoTime();
        MAGIC_CASTS.entrySet().removeIf(entry -> entry.getValue() < now);
        MAGIC_CASTS.put(entityId, now + MAGIC_CAST_ANIMATION_NANOS);
    }

    public static void clearMagicCasts() {
        MAGIC_CASTS.clear();
    }

    private static boolean isMagicCasting(int entityId) {
        Long until = MAGIC_CASTS.get(entityId);
        if (until == null) {
            return false;
        }
        if (System.nanoTime() > until) {
            MAGIC_CASTS.remove(entityId);
            return false;
        }
        return true;
    }

    public static final class State extends EntityRenderState {
        private final VillagerRenderState villagerState;
        private final AvatarRenderState valetState;
        private boolean useValetRenderer;

        private State(VillagerRenderState villagerState, AvatarRenderState valetState) {
            this.villagerState = villagerState;
            this.valetState = valetState;
        }
    }

    private static final class ValetAvatarRenderer extends LivingEntityRenderer<Villager, AvatarRenderState, ValetPlayerModel> {
        private static final Identifier STEVE_TEXTURE = Identifier.withDefaultNamespace("textures/entity/player/wide/steve.png");

        private ValetAvatarRenderer(EntityRendererProvider.Context context) {
            super(context, new ValetPlayerModel(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
            addLayer(new PlayerItemInHandLayer<>(this));
        }

        @Override
        public AvatarRenderState createRenderState() {
            return new AvatarRenderState();
        }

        @Override
        public Identifier getTextureLocation(AvatarRenderState state) {
            return STEVE_TEXTURE;
        }

        @Override
        public void extractRenderState(Villager villager, AvatarRenderState state, float tickProgress) {
            super.extractRenderState(villager, state, tickProgress);
            ArmedEntityRenderState.extractArmedEntityRenderState(villager, state, itemModelResolver, tickProgress);
            state.id = villager.getId();
            state.isSpectator = false;
            state.showHat = true;
            state.showJacket = true;
            state.showLeftPants = true;
            state.showRightPants = true;
            state.showLeftSleeve = true;
            state.showRightSleeve = true;
            state.showCape = false;
            applyMainHandPose(villager, state);
        }

        private static void applyMainHandPose(Villager villager, AvatarRenderState state) {
            ItemStack mainHand = villager.getItemBySlot(EquipmentSlot.MAINHAND);
            HumanoidModel.ArmPose pose = mainHand.is(Items.BOW)
                    ? HumanoidModel.ArmPose.BOW_AND_ARROW
                    : mainHand.isEmpty() ? HumanoidModel.ArmPose.EMPTY : HumanoidModel.ArmPose.ITEM;

            if (mainHand.is(Items.BOW)) {
                state.isUsingItem = true;
                state.useItemHand = InteractionHand.MAIN_HAND;
                state.ticksUsingItem = 20.0F;
            }

            if (state.mainArm == HumanoidArm.LEFT) {
                state.leftArmPose = pose;
            } else {
                state.rightArmPose = pose;
            }
        }
    }

    private static final class ValetPlayerModel extends PlayerModel {
        private ValetPlayerModel(ModelPart root, boolean slim) {
            super(root, slim);
        }

        @Override
        public void setupAnim(AvatarRenderState state) {
            super.setupAnim(state);
            if (!isMagicCasting(state.id)) {
                return;
            }
            float wave = 0.05F * (float) Math.sin(state.ageInTicks * 0.6F);
            rightArm.xRot = -2.2F;
            rightArm.yRot = 0.0F;
            rightArm.zRot = 0.35F + wave;
            leftArm.xRot = -2.2F;
            leftArm.yRot = 0.0F;
            leftArm.zRot = -0.35F - wave;
        }
    }
}
