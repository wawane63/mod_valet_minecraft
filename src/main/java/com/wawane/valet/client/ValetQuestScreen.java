package com.wawane.valet.client;

import com.wawane.valet.network.packets.ManageQuestPayload;
import com.wawane.valet.quest.ValetQuest;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public final class ValetQuestScreen extends Screen {
    private static final int ROW_TOP = 58;
    private static final int ROW_HEIGHT = 62;
    private static final int ROW_WIDTH = 300;

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
                    .bounds(left + 210, ROW_TOP + i * ROW_HEIGHT + 18, 90, 20).build();
            button.active = mayorNearby && state != 2;
            addRenderableWidget(button);
        }
    }

    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, width, height, 0xE0101418);
        graphics.centeredText(font, title, width / 2, 24, 0xFFFFFFFF);
        if (!mayorNearby) graphics.centeredText(font, Component.translatable("screen.valet_quest.no_mayor"), width / 2, 43, 0xFFFF7777);
        int left = width / 2 - 150;
        for (int i = 0; i < ValetQuest.values().length; i++) {
            ValetQuest quest = ValetQuest.values()[i];
            int state = i < states.length ? states[i] : 0;
            int owned = i < counts.length ? counts[i] : 0;
            int rowY = ROW_TOP + i * ROW_HEIGHT;
            int rowColor = state == 2 ? 0xC0223C28 : state == 1 ? 0xC02B3442 : 0xC01B2329;
            ItemStack requested = new ItemStack(quest.requestedItem(), quest.requestedCount());
            Component progress = state == 2
                    ? Component.translatable("screen.valet_quest.delivered", quest.requestedCount(), quest.requestedCount(), requested.getHoverName())
                    : Component.translatable("screen.valet_quest.requirement", owned, quest.requestedCount(), requested.getHoverName());

            graphics.fill(left, rowY, left + ROW_WIDTH, rowY + 56, rowColor);
            graphics.text(font, Component.translatable("quest.valet." + quest.id()), left + 8, rowY + 7, 0xFFFFFFFF, false);
            graphics.item(requested, left + 7, rowY + 28);
            graphics.itemDecorations(font, requested, left + 7, rowY + 28);
            graphics.text(font, progress, left + 29, rowY + 27, state == 2 ? 0xFF8FE69A : 0xFFD6DDE1, false);
            graphics.text(font, Component.translatable("screen.valet_quest.reward", quest.emeraldReward()), left + 29, rowY + 42, 0xFF55FF55, false);

            if (mouseX >= left + 7 && mouseX < left + 23 && mouseY >= rowY + 28 && mouseY < rowY + 44) {
                graphics.setTooltipForNextFrame(font, requested, mouseX, mouseY);
            }
        }
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override public boolean isPauseScreen() { return false; }
}
