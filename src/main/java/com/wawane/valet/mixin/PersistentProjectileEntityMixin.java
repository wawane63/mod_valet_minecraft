package com.wawane.valet.mixin;

import com.wawane.valet.ai.tasks.combat.CombatRuntimeTask;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.util.hit.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PersistentProjectileEntity.class)
public abstract class PersistentProjectileEntityMixin {
    @Inject(method = "onEntityHit", at = @At("HEAD"), cancellable = true)
    private void valet$preventValetFriendlyFire(EntityHitResult entityHitResult, CallbackInfo ci) {
        Entity projectile = (Entity) (Object) this;
        Entity owner = ((ProjectileEntity) (Object) this).getOwner();
        if (CombatRuntimeTask.shouldProtectAllyFromProjectile(owner, entityHitResult.getEntity())) {
            projectile.discard();
            ci.cancel();
        }
    }
}
