package com.wawane.valet.client;

import com.wawane.valet.ValetNetworking;
import com.wawane.valet.ValetMod;
import com.wawane.valet.gui.ValetOrdersScreen;
import com.wawane.valet.network.packets.ValetStatePayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screens.MenuScreens;

public class ValetClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ValetNetworking.registerPayloadTypes();
        MenuScreens.register(ValetMod.VALET_ORDERS_SCREEN_HANDLER, ValetOrdersScreen::new);
        ClientPlayNetworking.registerGlobalReceiver(ValetStatePayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                ValetOrdersScreen screen = ValetOrdersScreen.current();
                if (screen != null && screen.getValetEntityId() == payload.valetEntityId()) {
                    screen.applyServerState(payload.orderIndex(), payload.mineTargetIndex(), payload.woodTargetIndex(), payload.constructionTargetId(), payload.craftTargetIndex(), payload.oreCounts(), payload.woodCounts(), payload.valetInventory(), payload.level(), payload.xp(), payload.nextLevelXp(), payload.pendingPerks(), payload.perks(), payload.combatPerks(), payload.swordLevel(), payload.swordXp(), payload.swordNextLevelXp(), payload.swordPendingPerks(), payload.bowLevel(), payload.bowXp(), payload.bowNextLevelXp(), payload.bowPendingPerks(), payload.allyAwareness(), payload.valetName());
                }
            });
        });
    }
}
