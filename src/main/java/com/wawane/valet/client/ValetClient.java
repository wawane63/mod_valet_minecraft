package com.wawane.valet.client;

import com.wawane.valet.ValetNetworking;
import com.wawane.valet.ValetMod;
import com.wawane.valet.gui.ValetOrdersScreen;
import com.wawane.valet.network.packets.ValetStatePayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.minecraft.client.gui.screen.ingame.HandledScreens;

public class ValetClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HandledScreens.register(ValetMod.VALET_ORDERS_SCREEN_HANDLER, ValetOrdersScreen::new);
        BlockEntityRendererRegistry.register(ValetMod.CONSTRUCTION_BLUEPRINT_BLOCK_ENTITY, ConstructionBlueprintRenderer::new);
        ConstructionBlueprintPlacementPreview.register();
        ClientPlayNetworking.registerGlobalReceiver(ValetNetworking.VALET_STATE_PACKET_ID, (client, handler, buf, responseSender) -> {
            ValetStatePayload payload = ValetStatePayload.read(buf);
            client.execute(() -> {
                if (client.currentScreen instanceof ValetOrdersScreen screen && screen.getValetEntityId() == payload.valetEntityId()) {
                    screen.applyServerState(payload.orderIndex(), payload.mineTargetIndex(), payload.woodTargetIndex(), payload.constructionTargetId(), payload.level(), payload.xp(), payload.nextLevelXp(), payload.pendingPerks(), payload.perks(), payload.combatPerks(), payload.swordLevel(), payload.swordXp(), payload.swordNextLevelXp(), payload.swordPendingPerks(), payload.bowLevel(), payload.bowXp(), payload.bowNextLevelXp(), payload.bowPendingPerks(), payload.allyAwareness(), payload.valetName());
                }
            });
        });
    }
}
