package com.wawane.valet.client;

import com.wawane.valet.ValetNetworking;
import com.wawane.valet.ValetMod;
import com.wawane.valet.client.render.ValetConditionalVillagerRenderer;
import com.wawane.valet.gui.ValetOrdersScreen;
import com.wawane.valet.network.packets.ValetMagicCastPayload;
import com.wawane.valet.network.packets.ValetStatePayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.entity.EntityTypes;

public class ValetClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ValetNetworking.registerPayloadTypes();
        EntityRendererRegistry.register(EntityTypes.VILLAGER, ValetConditionalVillagerRenderer::new);
        ConstructionBlueprintPlacementPreview.register();
        MenuScreens.register(ValetMod.VALET_ORDERS_SCREEN_HANDLER, ValetOrdersScreen::new);
        ClientPlayNetworking.registerGlobalReceiver(ValetStatePayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                ValetOrdersScreen screen = ValetOrdersScreen.current();
                if (screen != null && screen.getValetEntityId() == payload.valetEntityId()) {
                    screen.applyServerState(payload.roleIndex(), payload.orderIndex(), payload.mineTargetIndex(), payload.woodTargetIndex(), payload.farmAreaId(), payload.farmCropMask(), payload.farmReplant(), payload.farmTillSoil(), payload.avoidNightReturn(), payload.freeBehavior(), payload.constructionTargetId(), payload.craftTargetIndex(), payload.oreCounts(), payload.woodCounts(), payload.valetInventory(), payload.level(), payload.xp(), payload.nextLevelXp(), payload.pendingPerks(), payload.perks(), payload.combatPerks(), payload.swordLevel(), payload.swordXp(), payload.swordNextLevelXp(), payload.swordPendingPerks(), payload.bowLevel(), payload.bowXp(), payload.bowNextLevelXp(), payload.bowPendingPerks(), payload.allyAwareness(), payload.valetName());
                }
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(ValetMagicCastPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> ValetConditionalVillagerRenderer.markMagicCast(payload.valetEntityId()));
        });
    }
}
