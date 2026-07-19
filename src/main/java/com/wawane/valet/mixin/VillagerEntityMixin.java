package com.wawane.valet.mixin;

import com.wawane.valet.state.ValetData;
import com.wawane.valet.ValetMod;
import com.wawane.valet.ai.ValetBrain;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Villager.class)
public abstract class VillagerEntityMixin {
    @Inject(method = "customServerAiStep", at = @At("HEAD"))
    private void valet$configureBrain(ServerLevel world, CallbackInfo ci) {
        Villager villager = (Villager) (Object) this;
        if (ValetMod.isValet(villager)) {
            ValetBrain.ensureConfigured(world, villager);
        }
    }

    @Inject(method = "customServerAiStep", at = @At("TAIL"))
    private void valet$reassertBrainActivity(ServerLevel world, CallbackInfo ci) {
        Villager villager = (Villager) (Object) this;
        if (ValetMod.isValet(villager)) {
            ValetBrain.reassertActivity(world, villager);
        }
    }

    @Inject(method = "refreshBrain", at = @At("TAIL"))
    private void valet$restoreBrainAfterRefresh(ServerLevel world, CallbackInfo ci) {
        Villager villager = (Villager) (Object) this;
        if (ValetMod.isValet(villager)) {
            ValetBrain.ensureConfigured(world, villager);
        }
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void valet$writeData(ValueOutput output, CallbackInfo ci) {
        Villager villager = (Villager) (Object) this;
        if (!ValetData.shouldPersist(villager)) {
            ValetData.clearVillagerRuntime(villager.getUUID());
            return;
        }
        ValetData.writeToNbt(villager, output);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void valet$readData(ValueInput input, CallbackInfo ci) {
        Villager villager = (Villager) (Object) this;
        if (!ValetData.shouldRead(villager, input)) {
            ValetData.clearVillagerRuntime(villager.getUUID());
            return;
        }
        ValetData.readFromNbt(villager, input);
        if (villager.level() instanceof ServerLevel world && ValetMod.isValet(villager)) {
            ValetBrain.ensureConfigured(world, villager);
        }
    }
}
