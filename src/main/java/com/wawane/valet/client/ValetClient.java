package com.wawane.valet.client;

import com.wawane.valet.ValetNetworking;
import com.wawane.valet.ValetMod;
import com.wawane.valet.client.render.ValetConditionalVillagerRenderer;
import com.wawane.valet.gui.ValetGroupScreen;
import com.wawane.valet.gui.ValetOrdersScreen;
import com.wawane.valet.network.packets.ValetMagicCastPayload;
import com.wawane.valet.network.packets.ValetGroupStatePayload;
import com.wawane.valet.network.packets.ValetStatePayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.world.entity.EntityTypes;

public class ValetClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ValetNetworking.registerPayloadTypes();
        EntityRenderers.register(EntityTypes.VILLAGER, ValetConditionalVillagerRenderer::new);
        ConstructionBlueprintPlacementPreview.register();
        MenuScreens.register(ValetMod.VALET_ORDERS_SCREEN_HANDLER, ValetOrdersScreen::new);
        MenuScreens.register(ValetMod.VALET_GROUP_SCREEN_HANDLER, ValetGroupScreen::new);
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof PauseScreen pauseScreen && pauseScreen.showsPauseMenu() && client.level != null) {
                Screens.getWidgets(screen).add(Button.builder(
                                Component.translatable("screen.valet_map.open"),
                                ignored -> client.setScreenAndShow(new ValetWorldMapScreen(screen)))
                        .bounds(scaledWidth - 164, 8, 156, 20)
                        .build());
            }
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ValetConditionalVillagerRenderer.clearMagicCasts();
            ValetWorldMapScreen.clearSession();
        });
        ClientPlayNetworking.registerGlobalReceiver(ValetStatePayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                if (context.client().gui.screen() instanceof ValetOrdersScreen screen
                        && screen.getValetEntityId() == payload.valetEntityId()) {
                    screen.applyServerState(payload.roleIndex(), payload.orderIndex(), payload.mineTargetIndex(), payload.woodTargetIndex(), payload.farmAreaId(), payload.farmCropMask(), payload.farmReplant(), payload.farmTillSoil(), payload.animalAreaId(), payload.animalBreed(), payload.animalShear(), payload.animalCollectEggs(), payload.animalMilk(), payload.animalCull(), payload.maxAnimals(), payload.avoidNightReturn(), payload.freeBehavior(), payload.constructionTargetId(), payload.craftTargetIndex(), payload.oreCounts(), payload.woodCounts(), payload.valetInventory(), payload.level(), payload.xp(), payload.nextLevelXp(), payload.pendingPerks(), payload.perks(), payload.combatPerks(), payload.swordLevel(), payload.swordXp(), payload.swordNextLevelXp(), payload.swordPendingPerks(), payload.bowLevel(), payload.bowXp(), payload.bowNextLevelXp(), payload.bowPendingPerks(), payload.allyAwareness(), payload.valetName());
                }
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(ValetMagicCastPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> ValetConditionalVillagerRenderer.markMagicCast(payload.valetEntityId()));
        });
        ClientPlayNetworking.registerGlobalReceiver(ValetGroupStatePayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                if (context.client().gui.screen() instanceof ValetWorldMapScreen mapScreen) {
                    mapScreen.applyServerState(payload.selectedGroupId(), payload.groups(), payload.valets());
                } else if (context.client().gui.screen() instanceof ValetGroupScreen screen
                        && screen.getMenu().getStationPos().equals(payload.stationPos())) {
                    screen.applyServerState(payload.selectedGroupId(), payload.groups(), payload.valets());
                }
            });
        });
    }
}
