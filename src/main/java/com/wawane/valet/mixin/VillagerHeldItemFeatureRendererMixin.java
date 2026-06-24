package com.wawane.valet.mixin;

import com.wawane.valet.ValetMod;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.VillagerHeldItemFeatureRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VillagerHeldItemFeatureRenderer.class)
public abstract class VillagerHeldItemFeatureRendererMixin<T extends LivingEntity> {
    @Inject(
            method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/entity/LivingEntity;FFFFFF)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"
            )
    )
    private void valet$animateHeldSword(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, T entity, float limbAngle, float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch, CallbackInfo ci) {
        if (!(entity instanceof VillagerEntity villager)
                || villager.getVillagerData().getProfession() != ValetMod.VALET_PROFESSION
                || !villager.getEquippedStack(EquipmentSlot.MAINHAND).isOf(Items.WOODEN_SWORD)
                || villager.handSwingProgress <= 0.0F) {
            return;
        }

        float swing = MathHelper.sin(villager.handSwingProgress * MathHelper.PI);
        float followThrough = MathHelper.sin(MathHelper.sqrt(villager.handSwingProgress) * MathHelper.PI);
        matrices.translate(0.0F, -0.08F - swing * 0.18F, -swing * 0.35F);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-70.0F * swing));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(30.0F * followThrough));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-35.0F * swing));
    }
}
