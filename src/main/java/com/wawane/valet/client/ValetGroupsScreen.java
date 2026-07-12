package com.wawane.valet.client;

import com.wawane.valet.ValetRole;
import com.wawane.valet.group.ValetGroupViewData;
import com.wawane.valet.network.packets.ManageMapGroupPayload;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/** Onglet unique de creation et d'affectation des groupes. */
public final class ValetGroupsScreen extends Screen {
    private static final int MARGIN = 16;
    private static final int TOP = 42;
    private static final int ROW_HEIGHT = 22;
    private static final int VISIBLE_ROWS = 9;

    private final ValetWorldMapScreen mapScreen;
    private final List<ValetGroupViewData.GroupEntry> groups = new ArrayList<>();
    private final List<ValetGroupViewData.ValetEntry> valets = new ArrayList<>();
    private int selectedGroupId = -1;
    private int groupScroll;
    private int valetScroll;
    private Button deleteButton;

    public ValetGroupsScreen(ValetWorldMapScreen mapScreen) {
        super(Component.translatable("screen.valet_groups.title"));
        this.mapScreen = mapScreen;
        applyServerState(mapScreen.getSelectedGroupId(), mapScreen.getGroups(), mapScreen.getValets());
    }

    @Override
    protected void init() {
        int tabWidth = Math.min(150, Math.max(100, (width - MARGIN * 2 - 4) / 2));
        addRenderableWidget(Button.builder(Component.translatable("screen.valet_map.tab"), ignored -> openMap())
                .bounds(MARGIN, 8, tabWidth, 22).build());
        Button groupsTab = addRenderableWidget(Button.builder(Component.translatable("screen.valet_groups.tab"), ignored -> {})
                .bounds(MARGIN + tabWidth + 4, 8, tabWidth, 22).build());
        groupsTab.active = false;

        int actionsLeft = width - MARGIN - 110;
        addRenderableWidget(Button.builder(Component.translatable("screen.valet_group.create"), ignored -> send(ManageMapGroupPayload.Action.CREATE, null))
                .bounds(actionsLeft, TOP, 110, 20).build());
        deleteButton = addRenderableWidget(Button.builder(Component.translatable("screen.valet_group.delete"), ignored -> send(ManageMapGroupPayload.Action.DELETE, null))
                .bounds(actionsLeft, TOP + 24, 110, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), ignored -> onClose())
                .bounds(actionsLeft, height - 36, 110, 20).build());
        updateButtons();
        send(ManageMapGroupPayload.Action.REQUEST, null);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, width, height, 0xF20A0E12);
        int actionsLeft = width - MARGIN - 110;
        int contentRight = actionsLeft - 12;
        int groupWidth = Math.max(80, (contentRight - MARGIN - 12) / 3);
        int valetLeft = MARGIN + groupWidth + 12;
        int valetWidth = Math.max(40, contentRight - valetLeft);
        drawPanel(graphics, MARGIN, TOP, groupWidth, height - TOP - MARGIN);
        drawPanel(graphics, valetLeft, TOP, valetWidth, height - TOP - MARGIN);
        graphics.text(font, Component.translatable("screen.valet_group.groups"), MARGIN + 8, TOP + 8, 0xFFFFFFFF, false);
        graphics.text(font, Component.translatable("screen.valet_group.valets"), valetLeft + 8, TOP + 8, 0xFFFFFFFF, false);
        drawGroups(graphics, mouseX, mouseY, MARGIN + 6, TOP + 26, groupWidth - 12);
        drawValets(graphics, mouseX, mouseY, valetLeft + 6, TOP + 26, valetWidth - 12);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    private void drawGroups(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int left, int top, int width) {
        if (groups.isEmpty()) {
            graphics.text(font, Component.translatable("screen.valet_group.no_group"), left + 4, top + 6, 0xFFCAD2D6, false);
            return;
        }
        for (int row = 0; row < visibleRows() && groupScroll + row < groups.size(); row++) {
            ValetGroupViewData.GroupEntry group = groups.get(groupScroll + row);
            int y = top + row * ROW_HEIGHT;
            boolean selected = group.id() == selectedGroupId;
            boolean hovered = inside(mouseX, mouseY, left, y, width, ROW_HEIGHT - 3);
            graphics.fill(left, y, left + width, y + ROW_HEIGHT - 3, selected ? 0xFF406A43 : hovered ? 0xFF374047 : 0xFF252D32);
            graphics.text(font, Component.literal(group.name() + " (" + group.memberCount() + ")"), left + 6, y + 6, 0xFFFFFFFF, false);
        }
    }

    private void drawValets(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int left, int top, int width) {
        if (valets.isEmpty()) {
            graphics.text(font, Component.translatable("screen.valet_group.no_valet"), left + 4, top + 6, 0xFFCAD2D6, false);
            return;
        }
        for (int row = 0; row < visibleRows() && valetScroll + row < valets.size(); row++) {
            ValetGroupViewData.ValetEntry valet = valets.get(valetScroll + row);
            int y = top + row * ROW_HEIGHT;
            boolean member = selectedGroupId > 0 && valet.groupId() == selectedGroupId;
            boolean other = valet.groupId() > 0 && valet.groupId() != selectedGroupId;
            boolean hovered = inside(mouseX, mouseY, left, y, width, ROW_HEIGHT - 3);
            graphics.fill(left, y, left + width, y + ROW_HEIGHT - 3, member ? 0xFF406A43 : hovered ? 0xFF374047 : 0xFF252D32);
            Component role = Component.translatable(ValetRole.fromIndex(valet.roleIndex()).getTranslationKey());
            String marker = member ? "+ " : other ? "• " : "  ";
            graphics.text(font, Component.literal(marker + valet.name() + " — ").append(role), left + 6, y + 6, other ? 0xFFFFC56D : 0xFFFFFFFF, false);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int actionsLeft = width - MARGIN - 110;
        int contentRight = actionsLeft - 12;
        int groupWidth = Math.max(80, (contentRight - MARGIN - 12) / 3);
        int valetLeft = MARGIN + groupWidth + 12;
        int valetWidth = Math.max(40, contentRight - valetLeft);
        int top = TOP + 26;
        for (int row = 0; row < visibleRows() && groupScroll + row < groups.size(); row++) {
            if (inside(event.x(), event.y(), MARGIN + 6, top + row * ROW_HEIGHT, groupWidth - 12, ROW_HEIGHT - 3)) {
                selectedGroupId = groups.get(groupScroll + row).id();
                updateButtons();
                return true;
            }
        }
        for (int row = 0; row < visibleRows() && valetScroll + row < valets.size(); row++) {
            if (inside(event.x(), event.y(), valetLeft + 6, top + row * ROW_HEIGHT, valetWidth - 12, ROW_HEIGHT - 3) && selectedGroupId > 0) {
                send(ManageMapGroupPayload.Action.TOGGLE_MEMBER, valets.get(valetScroll + row).uuid());
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int delta = verticalAmount > 0 ? -1 : verticalAmount < 0 ? 1 : 0;
        if (delta == 0) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
        int actionsLeft = width - MARGIN - 110;
        int contentRight = actionsLeft - 12;
        int groupWidth = Math.max(80, (contentRight - MARGIN - 12) / 3);
        if (mouseX < MARGIN + groupWidth + 6) {
            groupScroll = clamp(groupScroll + delta, groups.size());
        } else {
            valetScroll = clamp(valetScroll + delta, valets.size());
        }
        return true;
    }

    public void applyServerState(int nextSelectedGroupId, List<ValetGroupViewData.GroupEntry> nextGroups, List<ValetGroupViewData.ValetEntry> nextValets) {
        groups.clear();
        groups.addAll(nextGroups);
        valets.clear();
        valets.addAll(nextValets);
        selectedGroupId = groups.stream().anyMatch(group -> group.id() == nextSelectedGroupId)
                ? nextSelectedGroupId : groups.isEmpty() ? -1 : groups.get(0).id();
        groupScroll = clamp(groupScroll, groups.size());
        valetScroll = clamp(valetScroll, valets.size());
        updateButtons();
    }

    private void send(ManageMapGroupPayload.Action action, java.util.UUID valetUuid) {
        if (ClientPlayNetworking.canSend(ManageMapGroupPayload.TYPE)) {
            ClientPlayNetworking.send(new ManageMapGroupPayload(action, selectedGroupId, valetUuid, BlockPos.ZERO));
        }
    }

    private void openMap() {
        mapScreen.applyServerState(selectedGroupId, groups, valets);
        Minecraft.getInstance().setScreenAndShow(mapScreen);
    }

    @Override
    public void onClose() {
        mapScreen.applyServerState(selectedGroupId, groups, valets);
        mapScreen.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }

    private int visibleRows() {
        return Math.max(1, Math.min(VISIBLE_ROWS, (height - TOP - MARGIN - 30) / ROW_HEIGHT));
    }

    private int clamp(int scroll, int count) {
        return Math.max(0, Math.min(scroll, Math.max(0, count - visibleRows())));
    }

    private void updateButtons() {
        if (deleteButton != null) {
            deleteButton.active = selectedGroupId > 0;
        }
    }

    private static void drawPanel(GuiGraphicsExtractor graphics, int left, int top, int width, int height) {
        graphics.fill(left, top, left + width, top + height, 0xD91A2228);
        graphics.outline(left, top, width, height, 0xFF5E6B70);
    }

    private static boolean inside(double x, double y, int left, int top, int width, int height) {
        return x >= left && x < left + width && y >= top && y < top + height;
    }
}
