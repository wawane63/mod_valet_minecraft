package com.wawane.valet.gui;

import com.wawane.valet.ValetRole;
import com.wawane.valet.group.ValetGroupMode;
import com.wawane.valet.network.packets.ManageGroupPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

public class ValetGroupScreen extends AbstractContainerScreen<ValetGroupScreenHandler> {
    private static final int SCREEN_WIDTH = 430;
    private static final int SCREEN_HEIGHT = 276;
    private static final int PANEL_TOP = 22;
    private static final int MARGIN = 10;
    private static final int GROUP_WIDTH = 128;
    private static final int VALET_WIDTH = 160;
    private static final int COMMAND_WIDTH = 104;
    private static final int PANEL_HEIGHT = SCREEN_HEIGHT - PANEL_TOP - MARGIN;
    private static final int ROW_HEIGHT = 19;
    private static final int ROW_GAP = 3;
    private static final int VISIBLE_ROWS = 9;

    private final List<ValetGroupScreenHandler.GroupEntry> groups = new ArrayList<>();
    private final List<ValetGroupScreenHandler.ValetEntry> valets = new ArrayList<>();
    private int selectedGroupId;
    private Button deleteButton;
    private Button cardButton;
    private Button hornButton;
    private final List<Button> commandButtons = new ArrayList<>();
    private int groupScroll;
    private int valetScroll;

    public ValetGroupScreen(ValetGroupScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title, SCREEN_WIDTH, SCREEN_HEIGHT);
        groups.addAll(handler.getGroups());
        valets.addAll(handler.getValets());
        selectedGroupId = handler.getSelectedGroupId();
        titleLabelX = -10000;
        inventoryLabelY = 10000;
    }

    @Override
    protected void init() {
        super.init();
        int commandLeft = getCommandLeft();
        int top = topPos + PANEL_TOP + 20;
        addRenderableWidget(Button.builder(Component.translatable("screen.valet_group.create"), button -> send(ManageGroupPayload.Action.CREATE, selectedGroupId, ValetGroupMode.IDLE))
                .bounds(commandLeft, top, COMMAND_WIDTH, 18)
                .build());
        deleteButton = addRenderableWidget(Button.builder(Component.translatable("screen.valet_group.delete"), button -> send(ManageGroupPayload.Action.DELETE, selectedGroupId, ValetGroupMode.IDLE))
                .bounds(commandLeft, top + 22, COMMAND_WIDTH, 18)
                .build());
        cardButton = addRenderableWidget(Button.builder(Component.translatable("screen.valet_group.card"), button -> send(ManageGroupPayload.Action.GIVE_CARD, selectedGroupId, ValetGroupMode.IDLE))
                .bounds(commandLeft, top + 48, COMMAND_WIDTH, 18)
                .build());
        hornButton = addRenderableWidget(Button.builder(Component.translatable("screen.valet_group.horn"), button -> send(ManageGroupPayload.Action.BIND_HORN, selectedGroupId, ValetGroupMode.IDLE))
                .bounds(commandLeft, top + 70, COMMAND_WIDTH, 18)
                .build());
        addCommandButton(commandLeft, top + 104, "screen.valet_group.follow", ValetGroupMode.FOLLOW);
        addCommandButton(commandLeft, top + 126, "screen.valet_group.guard_close", ValetGroupMode.GUARD_CLOSE);
        addCommandButton(commandLeft, top + 148, "screen.valet_group.guard_wide", ValetGroupMode.GUARD_WIDE);
        addCommandButton(commandLeft, top + 170, "screen.valet_group.attack_area", ValetGroupMode.ATTACK_AREA);
        addCommandButton(commandLeft, top + 192, "screen.valet_group.recall", ValetGroupMode.RECALL);
        updateButtons();
    }

    private void addCommandButton(int left, int top, String key, ValetGroupMode mode) {
        Button button = addRenderableWidget(Button.builder(Component.translatable(key), ignored -> send(ManageGroupPayload.Action.COMMAND, selectedGroupId, mode))
                .bounds(left, top, COMMAND_WIDTH, 18)
                .build());
        commandButtons.add(button);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        extractTransparentBackground(context);
        drawFrame(context, leftPos, topPos, imageWidth, imageHeight);
        drawPanel(context, getGroupLeft(), getPanelTop(), GROUP_WIDTH, PANEL_HEIGHT);
        drawPanel(context, getValetLeft(), getPanelTop(), VALET_WIDTH, PANEL_HEIGHT);
        drawPanel(context, getCommandLeft() - 6, getPanelTop(), COMMAND_WIDTH + 12, PANEL_HEIGHT);
        context.text(font, title, getGroupLeft() + 8, topPos + 8, 0xFF303030, false);
        context.text(font, Component.translatable("screen.valet_group.groups"), getGroupLeft() + 8, getPanelTop() + 8, 0xFF303030, false);
        context.text(font, Component.translatable("screen.valet_group.valets"), getValetLeft() + 8, getPanelTop() + 8, 0xFF303030, false);
        context.text(font, Component.translatable("screen.valet_group.commands"), getCommandLeft(), getPanelTop() + 8, 0xFF303030, false);
        drawGroups(context, mouseX, mouseY);
        drawValets(context, mouseX, mouseY);
        super.extractRenderState(context, mouseX, mouseY, delta);
    }

    private void drawGroups(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        int left = getGroupLeft() + 7;
        int top = getPanelTop() + 26;
        if (groups.isEmpty()) {
            context.text(font, Component.translatable("screen.valet_group.no_group"), left + 4, top + 6, 0xFF5A5142, false);
            return;
        }
        for (int row = 0; row < VISIBLE_ROWS && groupScroll + row < groups.size(); row++) {
            ValetGroupScreenHandler.GroupEntry group = groups.get(groupScroll + row);
            int rowTop = top + row * (ROW_HEIGHT + ROW_GAP);
            boolean selected = group.id() == selectedGroupId;
            boolean hovered = isInside(mouseX, mouseY, left, rowTop, GROUP_WIDTH - 14, ROW_HEIGHT);
            drawInset(context, left, rowTop, GROUP_WIDTH - 14, ROW_HEIGHT, selected ? 0xFFB99A45 : hovered ? 0xFFD8D1BD : 0xFFC5C0B2);
            Component label = Component.literal(group.name() + " (" + group.memberCount() + ")");
            context.text(font, label, left + 5, rowTop + 6, selected ? 0xFF2B1A05 : 0xFF303030, false);
        }
        drawScrollbar(context, getGroupLeft() + GROUP_WIDTH - 8, top, groups.size(), groupScroll);
    }

    private void drawValets(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        int left = getValetLeft() + 7;
        int top = getPanelTop() + 26;
        if (valets.isEmpty()) {
            context.text(font, Component.translatable("screen.valet_group.no_valet"), left + 4, top + 6, 0xFF5A5142, false);
            return;
        }
        for (int row = 0; row < VISIBLE_ROWS && valetScroll + row < valets.size(); row++) {
            ValetGroupScreenHandler.ValetEntry valet = valets.get(valetScroll + row);
            int rowTop = top + row * (ROW_HEIGHT + ROW_GAP);
            boolean selected = selectedGroupId > 0 && valet.groupId() == selectedGroupId;
            boolean inOtherGroup = valet.groupId() > 0 && valet.groupId() != selectedGroupId;
            boolean hovered = isInside(mouseX, mouseY, left, rowTop, VALET_WIDTH - 14, ROW_HEIGHT);
            drawInset(context, left, rowTop, VALET_WIDTH - 14, ROW_HEIGHT, selected ? 0xFFA6B66A : hovered ? 0xFFD8D1BD : 0xFFC5C0B2);
            Component role = Component.translatable(ValetRole.fromIndex(valet.roleIndex()).getTranslationKey());
            String marker = selected ? " + " : inOtherGroup ? " * " : "   ";
            Component label = Component.literal(marker + valet.name() + " - ").append(role);
            context.text(font, label, left + 4, rowTop + 6, inOtherGroup ? 0xFF6A5A25 : 0xFF303030, false);
        }
        drawScrollbar(context, getValetLeft() + VALET_WIDTH - 8, top, valets.size(), valetScroll);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int groupLeft = getGroupLeft() + 7;
        int groupTop = getPanelTop() + 26;
        for (int row = 0; row < VISIBLE_ROWS && groupScroll + row < groups.size(); row++) {
            if (isInside(mouseX, mouseY, groupLeft, groupTop + row * (ROW_HEIGHT + ROW_GAP), GROUP_WIDTH - 14, ROW_HEIGHT)) {
                selectedGroupId = groups.get(groupScroll + row).id();
                updateButtons();
                return true;
            }
        }

        int valetLeft = getValetLeft() + 7;
        int valetTop = getPanelTop() + 26;
        for (int row = 0; row < VISIBLE_ROWS && valetScroll + row < valets.size(); row++) {
            if (isInside(mouseX, mouseY, valetLeft, valetTop + row * (ROW_HEIGHT + ROW_GAP), VALET_WIDTH - 14, ROW_HEIGHT)) {
                if (selectedGroupId > 0) {
                    ClientPlayNetworking.send(new ManageGroupPayload(menu.getStationPos(), ManageGroupPayload.Action.TOGGLE_MEMBER, selectedGroupId, valets.get(valetScroll + row).uuid(), ValetGroupMode.IDLE));
                }
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int delta = verticalAmount > 0.0D ? -1 : verticalAmount < 0.0D ? 1 : 0;
        if (delta == 0) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
        int listTop = getPanelTop() + 26;
        int listHeight = VISIBLE_ROWS * (ROW_HEIGHT + ROW_GAP) - ROW_GAP;
        if (isInside(mouseX, mouseY, getGroupLeft() + 7, listTop, GROUP_WIDTH - 14, listHeight)) {
            groupScroll = clampScroll(groupScroll + delta, groups.size());
            return true;
        }
        if (isInside(mouseX, mouseY, getValetLeft() + 7, listTop, VALET_WIDTH - 14, listHeight)) {
            valetScroll = clampScroll(valetScroll + delta, valets.size());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    public void applyServerState(int nextSelectedGroupId, List<ValetGroupScreenHandler.GroupEntry> nextGroups, List<ValetGroupScreenHandler.ValetEntry> nextValets) {
        groups.clear();
        groups.addAll(nextGroups);
        valets.clear();
        valets.addAll(nextValets);
        selectedGroupId = nextSelectedGroupId;
        if (selectedGroupId <= 0 && !groups.isEmpty()) {
            selectedGroupId = groups.get(0).id();
        }
        groupScroll = clampScroll(groupScroll, groups.size());
        valetScroll = clampScroll(valetScroll, valets.size());
        updateButtons();
    }

    private void send(ManageGroupPayload.Action action, int groupId, ValetGroupMode mode) {
        ClientPlayNetworking.send(new ManageGroupPayload(menu.getStationPos(), action, groupId, ManageGroupPayload.NO_VALET, mode));
    }

    private void updateButtons() {
        boolean hasSelected = selectedGroupId > 0;
        if (deleteButton != null) {
            deleteButton.active = hasSelected;
        }
        if (cardButton != null) {
            cardButton.active = hasSelected;
        }
        if (hornButton != null) {
            hornButton.active = hasSelected;
        }
        for (Button button : commandButtons) {
            button.active = hasSelected;
        }
    }

    private int getGroupLeft() {
        return leftPos + MARGIN;
    }

    private int getValetLeft() {
        return getGroupLeft() + GROUP_WIDTH + 10;
    }

    private int getCommandLeft() {
        return getValetLeft() + VALET_WIDTH + 14;
    }

    private int getPanelTop() {
        return topPos + PANEL_TOP;
    }

    private void drawFrame(GuiGraphicsExtractor context, int left, int top, int width, int height) {
        context.fill(left, top, left + width, top + height, 0xFF1F1A14);
        context.fill(left + 1, top + 1, left + width - 1, top + height - 1, 0xFFE7D6A3);
        context.fill(left + 3, top + 3, left + width - 3, top + height - 3, 0xFF5A4631);
        context.fill(left + 5, top + 5, left + width - 5, top + height - 5, 0xFFC8BEA6);
    }

    private void drawPanel(GuiGraphicsExtractor context, int left, int top, int width, int height) {
        context.fill(left, top, left + width, top + height, 0xFFE6E0D1);
        context.fill(left, top, left + width, top + 1, 0xFF8F836C);
        context.fill(left, top + height - 1, left + width, top + height, 0xFF8F836C);
        context.fill(left, top, left + 1, top + height, 0xFF8F836C);
        context.fill(left + width - 1, top, left + width, top + height, 0xFF8F836C);
    }

    private void drawInset(GuiGraphicsExtractor context, int left, int top, int width, int height, int color) {
        context.fill(left, top, left + width, top + height, color);
        context.fill(left, top, left + width, top + 1, 0xFF7C735F);
        context.fill(left, top, left + 1, top + height, 0xFF7C735F);
        context.fill(left, top + height - 1, left + width, top + height, 0xFFF3EBD7);
        context.fill(left + width - 1, top, left + width, top + height, 0xFFF3EBD7);
    }

    private void drawScrollbar(GuiGraphicsExtractor context, int left, int top, int itemCount, int scroll) {
        int maxScroll = Math.max(0, itemCount - VISIBLE_ROWS);
        if (maxScroll == 0) {
            return;
        }
        int height = VISIBLE_ROWS * (ROW_HEIGHT + ROW_GAP) - ROW_GAP;
        int thumbHeight = Math.max(18, height * VISIBLE_ROWS / itemCount);
        int thumbTop = top + scroll * (height - thumbHeight) / maxScroll;
        context.fill(left, top, left + 3, top + height, 0x667C735F);
        context.fill(left, thumbTop, left + 3, thumbTop + thumbHeight, 0xFF7C735F);
    }

    private static int clampScroll(int scroll, int itemCount) {
        return Math.max(0, Math.min(scroll, Math.max(0, itemCount - VISIBLE_ROWS)));
    }

    private boolean isInside(double mouseX, double mouseY, int left, int top, int width, int height) {
        return mouseX >= left && mouseX < left + width && mouseY >= top && mouseY < top + height;
    }
}
