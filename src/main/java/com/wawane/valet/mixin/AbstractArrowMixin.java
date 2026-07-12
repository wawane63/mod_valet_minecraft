package com.wawane.valet.mixin;

import com.wawane.valet.ai.tasks.combat.CombatRuntimeTask;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractArrow.class)
public abstract class AbstractArrowMixin {
    @Inject(method = "onHitEntity", at = @At("HEAD"), cancellable = true)
    private void valet$preventValetFriendlyFire(EntityHitResult hitResult, CallbackInfo ci) {
        Entity projectile = (Entity) (Object) this;
        Entity owner = ((Projectile) (Object) this).getOwner();
        if (CombatRuntimeTask.shouldProtectAllyFromProjectile(owner, hitResult.getEntity())) {
            projectile.discard();
            ci.cancel();
        }
    }
}
