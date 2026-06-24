package com.wawane.valet.mixin;

import com.wawane.valet.ValetMod;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.mob.ZombieVillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.village.VillagerData;
import net.minecraft.village.VillagerProfession;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ZombieVillagerEntity.class)
public abstract class ZombieVillagerEntityMixin extends ZombieEntity {
    protected ZombieVillagerEntityMixin(EntityType<? extends ZombieEntity> entityType, World world) {
        super(entityType, world);
    }

    @ModifyVariable(method = "setVillagerData", at = @At("HEAD"), argsOnly = true)
    private VillagerData valet$stripValetProfession(VillagerData data) {
        return data.getProfession() == ValetMod.VALET_PROFESSION
                ? data.withProfession(VillagerProfession.NONE)
                : data;
    }

    @Inject(method = "finishConversion", at = @At("HEAD"))
    private void valet$clearValetProfessionBeforeConversion(ServerWorld world, CallbackInfo ci) {
        ZombieVillagerEntity zombie = (ZombieVillagerEntity) (Object) this;
        if (zombie.getVillagerData().getProfession() == ValetMod.VALET_PROFESSION) {
            zombie.setVillagerData(zombie.getVillagerData().withProfession(VillagerProfession.NONE));
        }
    }
}
