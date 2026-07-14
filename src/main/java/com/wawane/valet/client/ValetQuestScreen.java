package com.wawane.valet.client;

import com.wawane.valet.network.packets.ManageQuestPayload;
import com.wawane.valet.quest.ValetQuest;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class ValetQuestScreen extends Screen {
    private boolean mayorNearby;
    private int[] states = new int[ValetQuest.values().length];
    private int[] counts = new int[ValetQuest.values().length];

    public ValetQuestScreen() { super(Component.translatable("screen.valet_quest.title")); }

    @Override protected void init() {
        rebuildButtons();
        ClientPlayNetworking.send(new ManageQuestPayload(-1));
    }

    public void applyState(boolean mayorNearby, int[] states, int[] counts) {
        this.mayorNearby = mayorNearby;
        this.states = states.clone(); this.counts = counts.clone();
        rebuildButtons();
    }

    private void rebuildButtons() {
        clearWidgets();
        int left = width / 2 - 150;
        for (int i = 0; i < ValetQuest.values().length; i++) {
            int index = i;
            int state = i < states.length ? states[i] : 0;
            Component label = Component.translatable(state == 0 ? "screen.valet_quest.accept" : state == 1 ? "screen.valet_quest.deliver" : "screen.valet_quest.done");
            Button button = Button.builder(label, ignored -> ClientPlayNetworking.send(new ManageQuestPayload(index)))
                    .bounds(left + 210, 66 + i * 62, 90, 20).build();
            button.active = mayorNearby && state != 2;
            addRenderableWidget(button);
        }
    }

    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, width, height, 0xE0101418);
        graphics.centeredText(font, title, width / 2, 24, 0xFFFFFF);
        if (!mayorNearby) graphics.centeredText(font, Component.translatable("screen.valet_quest.no_mayor"), width / 2, 43, 0xFF7777);
        int left = width / 2 - 150;
        for (int i = 0; i < ValetQuest.values().length; i++) {
            ValetQuest quest = ValetQuest.values()[i];
            int owned = i < counts.length ? counts[i] : 0;
            graphics.text(font, Component.translatable("quest.valet." + quest.id()), left, 66 + i * 62, 0xFFFFFF, false);
            graphics.text(font, Component.translatable("screen.valet_quest.requirement", owned, quest.requestedCount()), left, 82 + i * 62, 0xBBBBBB, false);
            graphics.text(font, Component.translatable("screen.valet_quest.reward", quest.emeraldReward()), left, 98 + i * 62, 0x55FF55, false);
        }
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override public boolean isPauseScreen() { return false; }
}
