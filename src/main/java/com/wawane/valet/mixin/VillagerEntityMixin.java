package com.wawane.valet.mixin;

import com.wawane.valet.ai.ValetWorkGoal;
import com.wawane.valet.state.ValetData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VillagerEntity.class)
public abstract class VillagerEntityMixin extends MerchantEntity {
    protected VillagerEntityMixin(EntityType<? extends MerchantEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void valet$addWorkGoal(EntityType<? extends VillagerEntity> entityType, World world, CallbackInfo ci) {
        this.goalSelector.add(3, new ValetWorkGoal((VillagerEntity) (Object) this));
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void valet$writeOrder(NbtCompound nbt, CallbackInfo ci) {
        VillagerEntity villager = (VillagerEntity) (Object) this;
        if (!ValetData.shouldPersist(villager, nbt)) {
            ValetData.clearVillagerRuntime(villager.getUuid());
            return;
        }

        ValetData.writeToNbt(villager, nbt);
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void valet$readOrder(NbtCompound nbt, CallbackInfo ci) {
        VillagerEntity villager = (VillagerEntity) (Object) this;
        if (!ValetData.shouldRead(villager, nbt)) {
            ValetData.clearVillagerRuntime(villager.getUuid());
            return;
        }

        ValetData.readFromNbt(villager, nbt);
    }
}
