package com.wawane.valet.gui;

import com.wawane.valet.ValetNetworking;
import com.wawane.valet.construction.ValetConstructionBlueprint;
import com.wawane.valet.order.ValetMineTarget;
import com.wawane.valet.order.ValetOrder;
import com.wawane.valet.order.ValetWoodTarget;
import com.wawane.valet.progress.ValetPerk;
import com.wawane.valet.network.packets.ChoosePerkPayload;
import com.wawane.valet.network.packets.DeleteConstructionPayload;
import com.wawane.valet.network.packets.RenameValetPayload;
import com.wawane.valet.network.packets.SetOrderPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class ValetOrdersScreen extends HandledScreen<ValetOrdersScreenHandler> {
    private static final int SCREEN_WIDTH = 360;
    private static final int SCREEN_HEIGHT = 286;
    private static final int PANEL_MARGIN = 9;
    private static final int PANEL_TOP = 19;
    private static final int LEFT_WIDTH = 128;
    private static final int PANEL_GAP = 8;
    private static final int RIGHT_WIDTH = SCREEN_WIDTH - PANEL_MARGIN * 2 - LEFT_WIDTH - PANEL_GAP;
    private static final int PANEL_HEIGHT = SCREEN_HEIGHT - PANEL_TOP - PANEL_MARGIN;
    private static final int ORDER_ROW_HEIGHT = 19;
    private static final int NODE_SIZE = 24;
    private static final int XP_BAR_WIDTH = 162;
    private static final int XP_BAR_HEIGHT = 9;
    private static final int ORDER_LIST_TOP_OFFSET = 24;
    private static final int ORDER_LIST_BOTTOM_PADDING = 8;
    private static final int ORDER_ROW_STRIDE = ORDER_ROW_HEIGHT + 3;
    private static final int CONSTRUCTION_PREVIEW_SIZE = 58;

    private final List<OrderEntry> orderEntries = new ArrayList<>();
    private final ValetOrdersViewModel viewModel;
    private final List<ValetConstructionBlueprint> localConstructions;
    private TextFieldWidget nameField;
    private ButtonWidget renameButton;
    private ButtonWidget buildConstructionButton;
    private ButtonWidget deleteConstructionButton;
    private TargetCategory selectedCategory = TargetCategory.NONE;
    private int selectedMineTargetIndex = -1;
    private int selectedWoodTargetIndex = -1;
    private int selectedConstructionTargetId = -1;
    private int localLevel;
    private int localXp;
    private int localNextLevelXp;
    private int localPendingPerks;
    private int orderScroll;
    private final boolean[] localPerks = new boolean[ValetPerk.values().length];
    private String localValetName;
    private ValetPerk selectedPerk = ValetPerk.SPEED;

    public ValetOrdersScreen(ValetOrdersScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        backgroundWidth = SCREEN_WIDTH;
        backgroundHeight = SCREEN_HEIGHT;
        viewModel = ValetOrdersViewModel.fromHandler(handler);
        localConstructions = new ArrayList<>(viewModel.constructions());
        ValetOrder currentOrder = viewModel.currentOrder();
        selectedCategory = currentOrder == ValetOrder.CHOP_WOOD ? TargetCategory.WOOD : currentOrder == ValetOrder.MINE_ORES ? TargetCategory.ORE : currentOrder == ValetOrder.BUILD_STRUCTURE ? TargetCategory.CONSTRUCTION : TargetCategory.NONE;
        selectedMineTargetIndex = viewModel.currentMineTargetIndex();
        selectedWoodTargetIndex = viewModel.currentWoodTargetIndex();
        selectedConstructionTargetId = viewModel.currentConstructionTargetId();
        localLevel = viewModel.level();
        localXp = viewModel.xp();
        localNextLevelXp = viewModel.nextLevelXp();
        localPendingPerks = viewModel.pendingPerks();
        for (ValetPerk perk : ValetPerk.values()) {
            localPerks[perk.ordinal()] = viewModel.hasPerk(perk);
        }
        localValetName = viewModel.valetName();
        titleX = -10000;
        playerInventoryTitleY = 10000;
    }

    @Override
    protected void init() {
        super.init();
        highlightValet(true);
        rebuildOrderEntries();
        int rightLeft = getRightPanelLeft();
        int top = getPanelTop();
        nameField = addDrawableChild(new TextFieldWidget(textRenderer, rightLeft + 10, top + 21, 128, 16, Text.translatable("screen.valet.rename")));
        nameField.setMaxLength(32);
        nameField.setText(localValetName);
        renameButton = addDrawableChild(ButtonWidget.builder(Text.translatable("screen.valet.rename_apply"), button -> sendRename())
                .dimensions(rightLeft + 143, top + 20, 52, 18)
                .build());
        buildConstructionButton = addDrawableChild(ButtonWidget.builder(Text.translatable("screen.valet.get_blueprint"), button -> sendSelectedConstructionOrder())
                .dimensions(rightLeft + 88, top + 42, 50, 18)
                .build());
        deleteConstructionButton = addDrawableChild(ButtonWidget.builder(Text.translatable("screen.valet.delete"), button -> sendDeleteConstruction())
                .dimensions(rightLeft + 143, top + 42, 52, 18)
                .build());
        updateConstructionButtons();
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        drawFrame(context, x, y, backgroundWidth, backgroundHeight);
        drawPanel(context, getLeftPanelLeft(), getPanelTop(), LEFT_WIDTH, PANEL_HEIGHT);
        drawPanel(context, getRightPanelLeft(), getPanelTop(), RIGHT_WIDTH, PANEL_HEIGHT);
        drawOrderPanel(context, mouseX, mouseY);
        drawInfoPanel(context);
        drawPerkTree(context, mouseX, mouseY);
        drawPerkDetails(context, mouseX, mouseY);
    }

    private void drawOrderPanel(DrawContext context, int mouseX, int mouseY) {
        int left = getLeftPanelLeft();
        int top = getPanelTop();
        context.drawText(textRenderer, title, left + 8, top + 7, 0x303030, false);

        context.enableScissor(left + 7, getOrderListTop(), left + LEFT_WIDTH - 7, getOrderListTop() + getOrderListHeight());
        for (int i = 0; i < orderEntries.size(); i++) {
            OrderEntry entry = orderEntries.get(i);
            int rowTop = getOrderTop(i);
            if (rowTop + ORDER_ROW_HEIGHT < getOrderListTop() || rowTop > getOrderListTop() + getOrderListHeight()) {
                continue;
            }
            boolean selected = isSelectedEntry(entry);
            boolean hovered = isInside(mouseX, mouseY, left + 7, rowTop, LEFT_WIDTH - 14, ORDER_ROW_HEIGHT);
            drawInset(context, left + 7, rowTop, LEFT_WIDTH - 14, ORDER_ROW_HEIGHT, selected ? 0xFFB99A45 : hovered ? 0xFFD8D1BD : 0xFFC5C0B2);
            context.drawText(textRenderer, getOrderLabel(entry, selected), left + 12, rowTop + 6, selected ? 0x2B1A05 : 0x303030, false);
        }
        context.disableScissor();
        drawOrderScrollbar(context);
    }

    private void drawInfoPanel(DrawContext context) {
        int left = getRightPanelLeft();
        int top = getPanelTop();

        context.drawText(textRenderer, Text.translatable("screen.valet.rename"), left + 10, top + 9, 0x303030, false);
        context.drawText(textRenderer, getAvailableTitle(), left + 10, top + 45, 0x303030, false);
        context.drawText(textRenderer, getSelectedText(), left + 10, top + 60, 0x1F1F1F, false);
        context.drawText(textRenderer, getHintText(), left + 10, top + 75, 0x5A5142, false);
        drawConstructionPreview(context, getSelectedConstruction(), left + RIGHT_WIDTH - CONSTRUCTION_PREVIEW_SIZE - 10, top + 66);
        context.drawText(textRenderer, Text.translatable("screen.valet.level", localLevel), left + 10, top + 93, 0x202020, false);
        drawXpBar(context, left + 10, top + 108);

        context.drawText(textRenderer, Text.translatable("screen.valet.skill_tree"), left + 10, top + 134, 0x303030, false);
        context.drawText(textRenderer, Text.translatable("screen.valet.pending_points", localPendingPerks), left + RIGHT_WIDTH - 72, top + 134, localPendingPerks > 0 ? 0x8A5A00 : 0x606060, false);
    }

    private void drawPerkTree(DrawContext context, int mouseX, int mouseY) {
        PerkNode speed = getNode(ValetPerk.SPEED);
        PerkNode vision = getNode(ValetPerk.VISION);
        PerkNode storage = getNode(ValetPerk.STORAGE);
        PerkNode pathing = getNode(ValetPerk.PATHING);
        PerkNode vein = getNode(ValetPerk.VEIN);
        PerkNode haul = getNode(ValetPerk.HAUL);
        PerkNode lighting = getNode(ValetPerk.LIGHTING);

        drawConnection(context, speed, vision);
        drawConnection(context, speed, storage);
        drawConnection(context, vision, pathing);
        drawConnection(context, vision, vein);
        drawConnection(context, vision, lighting);
        drawConnection(context, storage, haul);

        for (ValetPerk perk : ValetPerk.values()) {
            drawPerkNode(context, getNode(perk), mouseX, mouseY);
        }
    }

    private void drawPerkNode(DrawContext context, PerkNode node, int mouseX, int mouseY) {
        boolean owned = hasLocalPerk(node.perk);
        boolean hovered = isInside(mouseX, mouseY, node.left, node.top, NODE_SIZE, NODE_SIZE);
        boolean selected = selectedPerk == node.perk;
        int border = owned ? 0xFFFFD36B : localPendingPerks > 0 ? 0xFF7F9CCB : 0xFF555555;
        int fill = owned ? 0xFFB9872D : localPendingPerks > 0 ? 0xFF505C72 : 0xFF767676;

        if (hovered || selected) {
            border = 0xFFFFFFFF;
        }

        drawInset(context, node.left, node.top, NODE_SIZE, NODE_SIZE, border);
        context.fill(node.left + 3, node.top + 3, node.left + NODE_SIZE - 3, node.top + NODE_SIZE - 3, fill);
        context.drawCenteredTextWithShadow(textRenderer, getPerkIcon(node.perk, owned), node.left + NODE_SIZE / 2, node.top + 10, 0xFFFFFF);
    }

    private void drawConnection(DrawContext context, PerkNode from, PerkNode to) {
        int color = hasLocalPerk(from.perk) && hasLocalPerk(to.perk) ? 0xFFD39A35 : 0xFF555555;
        int x1 = from.centerX();
        int y1 = from.centerY();
        int x2 = to.centerX();
        int y2 = to.centerY();
        int midX = (x1 + x2) / 2;

        context.fill(Math.min(x1, midX), y1 - 1, Math.max(x1, midX) + 1, y1 + 1, color);
        context.fill(midX - 1, Math.min(y1, y2), midX + 1, Math.max(y1, y2) + 1, color);
        context.fill(Math.min(midX, x2), y2 - 1, Math.max(midX, x2) + 1, y2 + 1, color);
    }

    private void drawPerkDetails(DrawContext context, int mouseX, int mouseY) {
        ValetPerk perk = getHoveredPerk(mouseX, mouseY);
        if (perk == null) {
            perk = selectedPerk;
        }

        int left = getRightPanelLeft() + 10;
        int top = getPanelTop() + 222;
        int width = RIGHT_WIDTH - 20;
        drawInset(context, left, top, width, 36, 0xFFB8B19E);
        context.drawText(textRenderer, Text.translatable(perk.getTranslationKey()), left + 7, top + 6, 0x202020, false);
        context.drawText(textRenderer, Text.translatable(getPerkDescriptionKey(perk)), left + 7, top + 18, 0x4A4030, false);

        Text status = getPerkStatus(perk);
        context.drawText(textRenderer, status, left + width - textRenderer.getWidth(status) - 7, top + 6, hasLocalPerk(perk) ? 0x8A5A00 : 0x555555, false);
    }

    private void drawXpBar(DrawContext context, int left, int top) {
        int maxXp = Math.max(1, localNextLevelXp);
        int fillWidth = Math.min(XP_BAR_WIDTH, localXp * XP_BAR_WIDTH / maxXp);

        context.fill(left, top, left + XP_BAR_WIDTH, top + XP_BAR_HEIGHT, 0xFF4C4C4C);
        context.fill(left + 1, top + 1, left + XP_BAR_WIDTH - 1, top + XP_BAR_HEIGHT - 1, 0xFF222222);
        context.fill(left + 1, top + 1, left + 1 + fillWidth, top + XP_BAR_HEIGHT - 1, 0xFF70B536);
        context.drawText(textRenderer, Text.translatable("screen.valet.xp", localXp, localNextLevelXp), left, top + 13, 0x4A4030, false);
    }

    private void drawFrame(DrawContext context, int left, int top, int width, int height) {
        context.fill(left, top, left + width, top + height, 0xFF1F1A14);
        context.fill(left + 1, top + 1, left + width - 1, top + height - 1, 0xFFE7D6A3);
        context.fill(left + 3, top + 3, left + width - 3, top + height - 3, 0xFF5A4631);
        context.fill(left + 5, top + 5, left + width - 5, top + height - 5, 0xFFC8BEA6);
    }

    private void drawPanel(DrawContext context, int left, int top, int width, int height) {
        context.fill(left, top, left + width, top + height, 0xFF3C3226);
        context.fill(left + 1, top + 1, left + width - 1, top + height - 1, 0xFFE4D8BD);
        context.fill(left + 3, top + 3, left + width - 3, top + height - 3, 0xFFB8B19E);
        context.fill(left + 4, top + 4, left + width - 4, top + height - 4, 0xFFD1CAB8);
    }

    private void drawInset(DrawContext context, int left, int top, int width, int height, int fill) {
        context.fill(left, top, left + width, top + height, 0xFF5A5142);
        context.fill(left + 1, top + 1, left + width - 1, top + height - 1, 0xFFE9DFCA);
        context.fill(left + 2, top + 2, left + width - 2, top + height - 2, fill);
    }

    private void rebuildOrderEntries() {
        orderEntries.clear();
        orderEntries.add(OrderEntry.target(ValetOrder.NONE, -1, Text.translatable("order.valet.none")));
        orderEntries.add(OrderEntry.category(TargetCategory.ORE, Text.translatable("screen.valet.category_ores")));
        if (selectedCategory == TargetCategory.ORE) {
            for (ValetMineTarget target : ValetMineTarget.values()) {
                int count = viewModel.getOreCount(target);
                if (count > 0) {
                    orderEntries.add(OrderEntry.target(ValetOrder.MINE_ORES, target.ordinal(), Text.literal("  ").append(Text.translatable("screen.valet.ore_count", Text.translatable(target.getTranslationKey()), count))));
                }
            }
        }

        orderEntries.add(OrderEntry.category(TargetCategory.WOOD, Text.translatable("screen.valet.category_wood")));
        if (selectedCategory == TargetCategory.WOOD) {
            for (ValetWoodTarget target : ValetWoodTarget.values()) {
                int count = viewModel.getWoodCount(target);
                if (count > 0) {
                    orderEntries.add(OrderEntry.target(ValetOrder.CHOP_WOOD, target.ordinal(), Text.literal("  ").append(Text.translatable("screen.valet.wood_count", Text.translatable(target.getTranslationKey()), count))));
                }
            }
        }

        orderEntries.add(OrderEntry.category(TargetCategory.CONSTRUCTION, Text.translatable("screen.valet.category_constructions")));
        if (selectedCategory == TargetCategory.CONSTRUCTION) {
            for (ValetConstructionBlueprint construction : localConstructions) {
                orderEntries.add(OrderEntry.target(ValetOrder.BUILD_STRUCTURE, construction.id(), Text.literal("  ").append(Text.translatable("screen.valet.construction_count", construction.name(), construction.blockCount()))));
            }
        }
        clampOrderScroll();
        updateConstructionButtons();
    }

    private boolean isSelectedEntry(OrderEntry entry) {
        if (entry.categoryOnly) {
            return selectedCategory == entry.category;
        }
        if (entry.order == ValetOrder.NONE) {
            return selectedCategory == TargetCategory.NONE;
        }
        if (entry.order == ValetOrder.MINE_ORES) {
            return selectedCategory == TargetCategory.ORE && selectedMineTargetIndex == entry.targetIndex;
        }
        if (entry.order == ValetOrder.CHOP_WOOD) {
            return selectedCategory == TargetCategory.WOOD && selectedWoodTargetIndex == entry.targetIndex;
        }
        if (entry.order == ValetOrder.BUILD_STRUCTURE) {
            return selectedCategory == TargetCategory.CONSTRUCTION && selectedConstructionTargetId == entry.targetIndex;
        }
        return false;
    }

    private Text getAvailableTitle() {
        return switch (selectedCategory) {
            case ORE -> Text.translatable("screen.valet.available_ores");
            case WOOD -> Text.translatable("screen.valet.available_wood");
            case CONSTRUCTION -> Text.translatable("screen.valet.available_constructions");
            case NONE -> Text.translatable("screen.valet.available_targets");
        };
    }

    private Text getCategoryText(TargetCategory category) {
        return switch (category) {
            case ORE -> Text.translatable("screen.valet.category_ores");
            case WOOD -> Text.translatable("screen.valet.category_wood");
            case CONSTRUCTION -> Text.translatable("screen.valet.category_constructions");
            case NONE -> Text.translatable("order.valet.none");
        };
    }

    private Text getOrderLabel(OrderEntry entry, boolean selected) {
        return selected ? Text.literal("> ").append(entry.label) : entry.label;
    }

    private Text getSelectedText() {
        if (selectedCategory == TargetCategory.ORE) {
            ValetMineTarget target = ValetMineTarget.fromIndex(selectedMineTargetIndex);
            return target == null ? getCategoryText(TargetCategory.ORE) : Text.translatable(target.getTranslationKey());
        }
        if (selectedCategory == TargetCategory.WOOD) {
            ValetWoodTarget target = ValetWoodTarget.fromIndex(selectedWoodTargetIndex);
            return target == null ? getCategoryText(TargetCategory.WOOD) : Text.translatable(target.getTranslationKey());
        }
        if (selectedCategory == TargetCategory.CONSTRUCTION) {
            ValetConstructionBlueprint construction = getSelectedConstruction();
            return construction == null ? getCategoryText(TargetCategory.CONSTRUCTION) : Text.literal(construction.name());
        }
        return Text.translatable("order.valet.none");
    }

    private Text getHintText() {
        if (selectedCategory == TargetCategory.CONSTRUCTION) {
            return localConstructions.isEmpty() ? Text.translatable("screen.valet.no_constructions") : Text.translatable("screen.valet.build_hint");
        }
        return Text.translatable("screen.valet.mine_hint");
    }

    private ValetConstructionBlueprint getSelectedConstruction() {
        for (ValetConstructionBlueprint construction : localConstructions) {
            if (construction.id() == selectedConstructionTargetId) {
                return construction;
            }
        }
        return null;
    }

    private PerkNode getNode(ValetPerk perk) {
        int left = getRightPanelLeft();
        int top = getPanelTop();
        return switch (perk) {
            case SPEED -> new PerkNode(perk, left + 20, top + 166);
            case VISION -> new PerkNode(perk, left + 65, top + 150);
            case STORAGE -> new PerkNode(perk, left + 65, top + 183);
            case PATHING -> new PerkNode(perk, left + 110, top + 137);
            case VEIN -> new PerkNode(perk, left + 110, top + 166);
            case HAUL -> new PerkNode(perk, left + 110, top + 196);
            case LIGHTING -> new PerkNode(perk, left + 155, top + 166);
        };
    }

    private ValetPerk getHoveredPerk(int mouseX, int mouseY) {
        for (ValetPerk perk : ValetPerk.values()) {
            PerkNode node = getNode(perk);
            if (isInside(mouseX, mouseY, node.left, node.top, NODE_SIZE, NODE_SIZE)) {
                return perk;
            }
        }
        return null;
    }

    private Text getPerkIcon(ValetPerk perk, boolean owned) {
        if (owned) {
            return Text.literal("*");
        }

        return Text.literal(perk.getIcon());
    }

    private Text getPerkStatus(ValetPerk perk) {
        if (hasLocalPerk(perk)) {
            return Text.translatable("screen.valet.perk_owned");
        }
        if (localPendingPerks > 0) {
            return Text.translatable("screen.valet.perk_learn");
        }
        return Text.translatable("screen.valet.perk_no_points");
    }

    private String getPerkDescriptionKey(ValetPerk perk) {
        return perk.getDescriptionKey();
    }

    private boolean hasLocalPerk(ValetPerk perk) {
        return localPerks[perk.ordinal()];
    }

    private void setLocalPerk(ValetPerk perk) {
        localPerks[perk.ordinal()] = true;
    }

    private int getLeftPanelLeft() {
        return x + PANEL_MARGIN;
    }

    private int getRightPanelLeft() {
        return x + PANEL_MARGIN + LEFT_WIDTH + PANEL_GAP;
    }

    private int getPanelTop() {
        return y + PANEL_TOP;
    }

    private int getOrderTop(int row) {
        return getOrderListTop() + row * ORDER_ROW_STRIDE - orderScroll;
    }

    private int getOrderListTop() {
        return getPanelTop() + ORDER_LIST_TOP_OFFSET;
    }

    private int getOrderListHeight() {
        return PANEL_HEIGHT - ORDER_LIST_TOP_OFFSET - ORDER_LIST_BOTTOM_PADDING;
    }

    private int getMaxOrderScroll() {
        return Math.max(0, orderEntries.size() * ORDER_ROW_STRIDE - getOrderListHeight());
    }

    private void clampOrderScroll() {
        orderScroll = Math.max(0, Math.min(orderScroll, getMaxOrderScroll()));
    }

    private void scrollOrders(int amount) {
        orderScroll = Math.max(0, Math.min(orderScroll + amount, getMaxOrderScroll()));
    }

    private void drawOrderScrollbar(DrawContext context) {
        int maxScroll = getMaxOrderScroll();
        if (maxScroll <= 0) {
            return;
        }

        int left = getLeftPanelLeft() + LEFT_WIDTH - 10;
        int top = getOrderListTop();
        int height = getOrderListHeight();
        int thumbHeight = Math.max(18, height * height / (height + maxScroll));
        int thumbTop = top + orderScroll * (height - thumbHeight) / maxScroll;
        context.fill(left, top, left + 3, top + height, 0x665A5142);
        context.fill(left, thumbTop, left + 3, thumbTop + thumbHeight, 0xFF5A5142);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        drawMouseoverTooltip(context, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int left = getLeftPanelLeft();
        for (int i = 0; i < orderEntries.size(); i++) {
            OrderEntry entry = orderEntries.get(i);
            if (isInside(mouseX, mouseY, left + 7, getOrderTop(i), LEFT_WIDTH - 14, ORDER_ROW_HEIGHT)) {
                if (entry.categoryOnly) {
                    selectedCategory = entry.category;
                    rebuildOrderEntries();
                } else if (entry.order == ValetOrder.BUILD_STRUCTURE) {
                    selectedCategory = TargetCategory.CONSTRUCTION;
                    selectedConstructionTargetId = entry.targetIndex;
                    rebuildOrderEntries();
                } else {
                    sendSelection(entry.order, entry.targetIndex);
                }
                return true;
            }
        }

        ValetPerk hoveredPerk = getHoveredPerk((int) mouseX, (int) mouseY);
        if (hoveredPerk != null) {
            selectedPerk = hoveredPerk;
            if (localPendingPerks > 0 && !hasLocalPerk(hoveredPerk)) {
                sendPerkSelection(hoveredPerk);
            }
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (isInside(mouseX, mouseY, getLeftPanelLeft() + 7, getOrderListTop(), LEFT_WIDTH - 14, getOrderListHeight()) && getMaxOrderScroll() > 0) {
            scrollOrders((int) (-amount * ORDER_ROW_STRIDE));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    public int getValetEntityId() {
        return viewModel.valetEntityId();
    }

    public void applyServerState(int orderIndex, int mineTargetIndex, int woodTargetIndex, int constructionTargetId, int level, int xp, int nextLevelXp, int pendingPerks, boolean[] perks, String valetName) {
        ValetOrder order = ValetOrder.fromIndex(orderIndex);
        selectedCategory = order == ValetOrder.CHOP_WOOD ? TargetCategory.WOOD : order == ValetOrder.MINE_ORES ? TargetCategory.ORE : order == ValetOrder.BUILD_STRUCTURE ? TargetCategory.CONSTRUCTION : TargetCategory.NONE;
        selectedMineTargetIndex = mineTargetIndex;
        selectedWoodTargetIndex = woodTargetIndex;
        selectedConstructionTargetId = constructionTargetId;
        localLevel = level;
        localXp = xp;
        localNextLevelXp = nextLevelXp;
        localPendingPerks = pendingPerks;
        for (ValetPerk perk : ValetPerk.values()) {
            localPerks[perk.ordinal()] = perk.ordinal() < perks.length && perks[perk.ordinal()];
        }
        localValetName = valetName;
        if (nameField != null) {
            nameField.setText(localValetName);
        }
        rebuildOrderEntries();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if ((keyCode == 257 || keyCode == 335) && nameField != null && nameField.isFocused()) {
            sendRename();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void close() {
        highlightValet(false);
        super.close();
    }

    @Override
    public void removed() {
        highlightValet(false);
        super.removed();
    }

    private void highlightValet(boolean highlighted) {
        MinecraftClient minecraft = MinecraftClient.getInstance();
        if (minecraft.world == null) {
            return;
        }

        Entity entity = minecraft.world.getEntityById(viewModel.valetEntityId());
        if (entity == null) {
            return;
        }

        entity.setGlowing(highlighted);
    }

    private void sendSelection(ValetOrder order, int targetIndex) {
        if (order == ValetOrder.NONE) {
            selectedCategory = TargetCategory.NONE;
            selectedMineTargetIndex = -1;
            selectedWoodTargetIndex = -1;
            selectedConstructionTargetId = -1;
        } else if (order == ValetOrder.MINE_ORES) {
            selectedCategory = TargetCategory.ORE;
            selectedMineTargetIndex = targetIndex;
        } else if (order == ValetOrder.CHOP_WOOD) {
            selectedCategory = TargetCategory.WOOD;
            selectedWoodTargetIndex = targetIndex;
        } else if (order == ValetOrder.BUILD_STRUCTURE) {
            selectedCategory = TargetCategory.CONSTRUCTION;
            selectedConstructionTargetId = targetIndex;
        }
        rebuildOrderEntries();

        PacketByteBuf buf = PacketByteBufs.create();
        new SetOrderPayload(viewModel.valetEntityId(), order, targetIndex).write(buf);
        ClientPlayNetworking.send(ValetNetworking.SET_ORDER_PACKET_ID, buf);
    }

    private void sendPerkSelection(ValetPerk perk) {
        if (localPendingPerks <= 0 || hasLocalPerk(perk)) {
            return;
        }

        setLocalPerk(perk);
        localPendingPerks = Math.max(0, localPendingPerks - 1);

        PacketByteBuf buf = PacketByteBufs.create();
        new ChoosePerkPayload(viewModel.valetEntityId(), perk).write(buf);
        ClientPlayNetworking.send(ValetNetworking.CHOOSE_PERK_PACKET_ID, buf);
    }

    private void sendRename() {
        if (nameField == null) {
            return;
        }

        localValetName = nameField.getText().trim();
        PacketByteBuf buf = PacketByteBufs.create();
        new RenameValetPayload(viewModel.valetEntityId(), localValetName).write(buf);
        ClientPlayNetworking.send(ValetNetworking.RENAME_PACKET_ID, buf);
        nameField.setFocused(false);
    }

    private void sendSelectedConstructionOrder() {
        ValetConstructionBlueprint construction = getSelectedConstruction();
        if (construction == null) {
            return;
        }

        sendSelection(ValetOrder.BUILD_STRUCTURE, construction.id());
    }

    private void sendDeleteConstruction() {
        ValetConstructionBlueprint construction = getSelectedConstruction();
        if (construction == null) {
            return;
        }

        int constructionId = construction.id();
        localConstructions.removeIf(candidate -> candidate.id() == constructionId);
        selectedConstructionTargetId = -1;
        rebuildOrderEntries();

        PacketByteBuf buf = PacketByteBufs.create();
        new DeleteConstructionPayload(viewModel.valetEntityId(), constructionId).write(buf);
        ClientPlayNetworking.send(ValetNetworking.DELETE_CONSTRUCTION_PACKET_ID, buf);
    }

    private void updateConstructionButtons() {
        boolean hasSelectedConstruction = selectedCategory == TargetCategory.CONSTRUCTION && getSelectedConstruction() != null;
        if (buildConstructionButton != null) {
            buildConstructionButton.visible = hasSelectedConstruction;
            buildConstructionButton.active = hasSelectedConstruction;
        }
        if (deleteConstructionButton == null) {
            return;
        }

        deleteConstructionButton.visible = hasSelectedConstruction;
        deleteConstructionButton.active = hasSelectedConstruction;
    }

    private void drawConstructionPreview(DrawContext context, ValetConstructionBlueprint blueprint, int left, int top) {
        if (blueprint == null || blueprint.entries().isEmpty()) {
            return;
        }

        drawInset(context, left, top, CONSTRUCTION_PREVIEW_SIZE, CONSTRUCTION_PREVIEW_SIZE, 0xFF9FA394);
        int maxSide = Math.max(1, Math.max(blueprint.width(), blueprint.depth()));
        int cell = Math.max(2, (CONSTRUCTION_PREVIEW_SIZE - 8) / maxSide);
        int gridWidth = cell * Math.max(1, blueprint.width());
        int gridDepth = cell * Math.max(1, blueprint.depth());
        int startX = left + (CONSTRUCTION_PREVIEW_SIZE - gridWidth) / 2;
        int startY = top + (CONSTRUCTION_PREVIEW_SIZE - gridDepth) / 2;

        boolean[][] occupied = new boolean[Math.max(1, blueprint.width())][Math.max(1, blueprint.depth())];
        int[][] highestY = new int[Math.max(1, blueprint.width())][Math.max(1, blueprint.depth())];
        for (ValetConstructionBlueprint.Entry entry : blueprint.entries()) {
            if (entry.x() < 0 || entry.z() < 0 || entry.x() >= occupied.length || entry.z() >= occupied[0].length) {
                continue;
            }
            occupied[entry.x()][entry.z()] = true;
            highestY[entry.x()][entry.z()] = Math.max(highestY[entry.x()][entry.z()], entry.y());
        }

        for (int xCell = 0; xCell < occupied.length; xCell++) {
            for (int zCell = 0; zCell < occupied[xCell].length; zCell++) {
                if (!occupied[xCell][zCell]) {
                    continue;
                }
                int color = highestY[xCell][zCell] > blueprint.height() / 2 ? 0xD8B98F3A : 0xD84A7C8C;
                int x1 = startX + xCell * cell;
                int y1 = startY + zCell * cell;
                context.fill(x1, y1, x1 + cell - 1, y1 + cell - 1, color);
            }
        }
    }

    private boolean isInside(double mouseX, double mouseY, int left, int top, int width, int height) {
        return mouseX >= left && mouseX < left + width && mouseY >= top && mouseY < top + height;
    }

    private enum TargetCategory {
        NONE,
        ORE,
        WOOD,
        CONSTRUCTION
    }

    private static final class OrderEntry {
        private final ValetOrder order;
        private final int targetIndex;
        private final Text label;
        private final TargetCategory category;
        private final boolean categoryOnly;

        private OrderEntry(ValetOrder order, int targetIndex, Text label, TargetCategory category, boolean categoryOnly) {
            this.order = order;
            this.targetIndex = targetIndex;
            this.label = label;
            this.category = category;
            this.categoryOnly = categoryOnly;
        }

        private static OrderEntry target(ValetOrder order, int targetIndex, Text label) {
            return new OrderEntry(order, targetIndex, label, TargetCategory.NONE, false);
        }

        private static OrderEntry category(TargetCategory category, Text label) {
            return new OrderEntry(ValetOrder.NONE, -1, label, category, true);
        }
    }

    private static final class PerkNode {
        private final ValetPerk perk;
        private final int left;
        private final int top;

        private PerkNode(ValetPerk perk, int left, int top) {
            this.perk = perk;
            this.left = left;
            this.top = top;
        }

        private int centerX() {
            return left + NODE_SIZE / 2;
        }

        private int centerY() {
            return top + NODE_SIZE / 2;
        }
    }
}
