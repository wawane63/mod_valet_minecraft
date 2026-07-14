package com.wawane.valet.client;

import com.wawane.valet.ValetNetworking;
import com.wawane.valet.ValetMod;
import com.wawane.valet.client.render.ValetConditionalVillagerRenderer;
import com.wawane.valet.gui.ValetOrdersScreen;
import com.wawane.valet.network.packets.ValetMagicCastPayload;
import com.wawane.valet.network.packets.ValetGroupStatePayload;
import com.wawane.valet.network.packets.ValetStatePayload;
import com.wawane.valet.network.packets.ValetQuestStatePayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.network.chat.Component;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.world.entity.EntityTypes;
import org.lwjgl.glfw.GLFW;

public class ValetClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ValetNetworking.registerPayloadTypes();
        EntityRenderers.register(EntityTypes.VILLAGER, ValetConditionalVillagerRenderer::new);
        ConstructionBlueprintPlacementPreview.register();
        MenuScreens.register(ValetMod.VALET_ORDERS_SCREEN_HANDLER, ValetOrdersScreen::new);
        KeyMapping.Category category = KeyMapping.Category.register(ValetMod.id("controls"));
        KeyMapping questKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.valet.quests", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_J, category));
        KeyMapping mapKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.valet.map", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, category));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (questKey.consumeClick()) {
                if (client.level != null && client.gui.screen() == null) client.setScreenAndShow(new ValetQuestScreen());
            }
            while (mapKey.consumeClick()) {
                if (client.level != null && client.gui.screen() == null) client.setScreenAndShow(new ValetWorldMapScreen(null));
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
                } else if (context.client().gui.screen() instanceof ValetGroupsScreen screen) {
                    screen.applyServerState(payload.selectedGroupId(), payload.groups(), payload.valets());
                }
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(ValetQuestStatePayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                if (context.client().gui.screen() instanceof ValetQuestScreen screen) {
                    screen.applyState(payload.mayorNearby(), payload.states(), payload.counts());
                }
            });
        });
    }
}
