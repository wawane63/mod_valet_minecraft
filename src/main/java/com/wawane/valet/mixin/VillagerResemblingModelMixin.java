package com.wawane.valet.mixin;

import com.wawane.valet.ValetMod;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.EntityModelPartNames;
import net.minecraft.client.render.entity.model.VillagerResemblingModel;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VillagerResemblingModel.class)
public abstract class VillagerResemblingModelMixin<T extends Entity> {
    @Shadow
    @Final
    private ModelPart root;

    @Inject(method = "setAngles(Lnet/minecraft/entity/Entity;FFFFF)V", at = @At("TAIL"))
    private void valet$animateSwordSwing(T entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch, CallbackInfo ci) {
        if (!(entity instanceof VillagerEntity villager)
                || villager.getVillagerData().getProfession() != ValetMod.VALET_PROFESSION
                || !villager.getEquippedStack(EquipmentSlot.MAINHAND).isOf(Items.WOODEN_SWORD)
                || villager.handSwingProgress <= 0.0F) {
            return;
        }

        ModelPart arms = root.getChild(EntityModelPartNames.ARMS);
        float swing = MathHelper.sin(villager.handSwingProgress * MathHelper.PI);
        arms.pitch = -0.8F - swing * 1.7F;
        arms.yaw = MathHelper.sin(villager.handSwingProgress * MathHelper.PI * 2.0F) * 0.35F;
        arms.roll = -swing * 0.25F;
    }
}
