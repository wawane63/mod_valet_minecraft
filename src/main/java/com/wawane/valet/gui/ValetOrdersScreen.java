package com.wawane.valet.gui;

import com.wawane.valet.construction.ValetConstructionBlueprint;
import com.wawane.valet.order.ValetCraftTarget;
import com.wawane.valet.order.ValetMineTarget;
import com.wawane.valet.order.ValetOrder;
import com.wawane.valet.order.ValetWoodTarget;
import com.wawane.valet.progress.ValetCombatPerk;
import com.wawane.valet.progress.ValetPerk;
import com.wawane.valet.network.packets.ChooseCombatPerkPayload;
import com.wawane.valet.network.packets.ChoosePerkPayload;
import com.wawane.valet.network.packets.DeleteConstructionPayload;
import com.wawane.valet.network.packets.RenameValetPayload;
import com.wawane.valet.network.packets.SetOrderPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ValetOrdersScreen extends AbstractContainerScreen<ValetOrdersScreenHandler> {
    private static ValetOrdersScreen currentScreen;

    private static final int SCREEN_WIDTH = 430;
    private static final int SCREEN_HEIGHT = 420;
    private static final int PANEL_MARGIN = 9;
    private static final int PANEL_TOP = 19;
    private static final int LEFT_WIDTH = 136;
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
    private static final int TREE_TITLE_OFFSET = 158;
    private static final int COMBAT_HEADER_OFFSET = 180;
    private static final int TREE_DETAIL_OFFSET = 350;
    private static final int TREE_DETAIL_HEIGHT = 42;
    private static final int TREE_BOTTOM_ROW = 300;
    private static final int TREE_MIDDLE_ROW = 242;
    private static final int TREE_TOP_ROW = 188;
    private static final int TREE_CENTER_X = 124;
    private static final int TREE_LEFT_X = 72;
    private static final int TREE_RIGHT_X = 176;
    private static final int INVENTORY_TOP_OFFSET = 184;
    private static final int INVENTORY_COLUMNS = 6;
    private static final int INVENTORY_SLOT_SIZE = 18;
    private static final int INVENTORY_SLOT_GAP = 5;
    private static final ValetPerk[] RESOURCE_TREE_PERKS = {ValetPerk.SPEED, ValetPerk.VISION, ValetPerk.MOVEMENT};
    private static final ValetCombatPerk[] SWORD_TREE_PERKS = {ValetCombatPerk.SWORD_STRENGTH, ValetCombatPerk.SWORD_RECOVERY, ValetCombatPerk.SWORD_DEFENSE};
    private static final ValetCombatPerk[] BOW_TREE_PERKS = {ValetCombatPerk.ALLY_AWARENESS, ValetCombatPerk.BOW_QUICK_SHOT, ValetCombatPerk.BOW_STRENGTH, ValetCombatPerk.BOW_RECYCLE_ARROW};

    private final List<OrderEntry> orderEntries = new ArrayList<>();
    private final ValetOrdersViewModel viewModel;
    private final List<ValetConstructionBlueprint> localConstructions;
    private EditBox nameField;
    private Button renameButton;
    private Button buildConstructionButton;
    private Button deleteConstructionButton;
    private Button generalPageButton;
    private Button swordPageButton;
    private Button bowPageButton;
    private Button inventoryPageButton;
    private RightPage selectedRightPage = RightPage.GENERAL;
    private TargetCategory selectedCategory = TargetCategory.NONE;
    private int selectedMineTargetIndex = -1;
    private int selectedWoodTargetIndex = -1;
    private int selectedConstructionTargetId = -1;
    private int selectedCraftTargetIndex = -1;
    private int localLevel;
    private int localXp;
    private int localNextLevelXp;
    private int localPendingPerks;
    private int localSwordLevel;
    private int localSwordXp;
    private int localSwordNextLevelXp;
    private int localSwordPendingPerks;
    private int localBowLevel;
    private int localBowXp;
    private int localBowNextLevelXp;
    private int localBowPendingPerks;
    private boolean localAllyAwareness;
    private int[] localOreCounts;
    private int[] localWoodCounts;
    private List<ItemStack> localInventoryStacks;
    private int orderScroll;
    private final boolean[] localPerks = new boolean[ValetPerk.values().length];
    private final boolean[] localCombatPerks = new boolean[ValetCombatPerk.values().length];
    private String localValetName;
    private ValetPerk selectedPerk = ValetPerk.SPEED;
    private ValetCombatPerk selectedCombatPerk = ValetCombatPerk.SWORD_STRENGTH;

    public ValetOrdersScreen(ValetOrdersScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title, SCREEN_WIDTH, SCREEN_HEIGHT);
        viewModel = ValetOrdersViewModel.fromHandler(handler);
        localConstructions = new ArrayList<>(viewModel.constructions());
        ValetOrder currentOrder = viewModel.currentOrder();
        selectedCategory = currentOrder == ValetOrder.CHOP_WOOD ? TargetCategory.WOOD : currentOrder == ValetOrder.MINE_ORES ? TargetCategory.ORE : currentOrder == ValetOrder.BUILD_STRUCTURE ? TargetCategory.CONSTRUCTION : currentOrder == ValetOrder.CRAFT ? TargetCategory.CRAFT : TargetCategory.NONE;
        selectedMineTargetIndex = viewModel.currentMineTargetIndex();
        selectedWoodTargetIndex = viewModel.currentWoodTargetIndex();
        selectedConstructionTargetId = viewModel.currentConstructionTargetId();
        selectedCraftTargetIndex = viewModel.currentCraftTargetIndex();
        localLevel = viewModel.level();
        localXp = viewModel.xp();
        localNextLevelXp = viewModel.nextLevelXp();
        localPendingPerks = viewModel.pendingPerks();
        localSwordLevel = viewModel.swordLevel();
        localSwordXp = viewModel.swordXp();
        localSwordNextLevelXp = viewModel.swordNextLevelXp();
        localSwordPendingPerks = viewModel.swordPendingPerks();
        localBowLevel = viewModel.bowLevel();
        localBowXp = viewModel.bowXp();
        localBowNextLevelXp = viewModel.bowNextLevelXp();
        localBowPendingPerks = viewModel.bowPendingPerks();
        localAllyAwareness = viewModel.allyAwareness();
        localOreCounts = viewModel.oreCounts();
        localWoodCounts = viewModel.woodCounts();
        localInventoryStacks = copyInventory(viewModel.valetInventory());
        for (ValetCombatPerk perk : ValetCombatPerk.values()) {
            localCombatPerks[perk.ordinal()] = viewModel.hasCombatPerk(perk);
        }
        for (ValetPerk perk : ValetPerk.values()) {
            localPerks[perk.ordinal()] = viewModel.hasPerk(perk);
        }
        localValetName = viewModel.valetName();
        titleLabelX = -10000;
        inventoryLabelY = 10000;
    }

    public static ValetOrdersScreen current() {
        return currentScreen;
    }

    @Override
    protected void init() {
        super.init();
        currentScreen = this;
        rebuildOrderEntries();
        int rightLeft = getRightPanelLeft();
        int top = getPanelTop();
        nameField = addRenderableWidget(new EditBox(font, rightLeft + 10, top + 21, RIGHT_WIDTH - 78, 16, Component.translatable("screen.valet.rename")));
        nameField.setMaxLength(32);
        nameField.setValue(localValetName);
        renameButton = addRenderableWidget(Button.builder(Component.translatable("screen.valet.rename_apply"), button -> sendRename())
                .bounds(rightLeft + RIGHT_WIDTH - 62, top + 20, 52, 18)
                .build());
        buildConstructionButton = addRenderableWidget(Button.builder(Component.translatable("screen.valet.get_blueprint"), button -> sendSelectedConstructionOrder())
                .bounds(rightLeft + RIGHT_WIDTH - 116, top + 42, 52, 18)
                .build());
        deleteConstructionButton = addRenderableWidget(Button.builder(Component.translatable("screen.valet.delete"), button -> sendDeleteConstruction())
                .bounds(rightLeft + RIGHT_WIDTH - 60, top + 42, 50, 18)
                .build());
        generalPageButton = addRenderableWidget(Button.builder(Component.translatable("screen.valet.page_general"), button -> selectRightPage(RightPage.GENERAL))
                .bounds(rightLeft + 10, top + 132, 58, 18)
                .build());
        swordPageButton = addRenderableWidget(Button.builder(Component.translatable("screen.valet.page_sword"), button -> selectRightPage(RightPage.SWORD))
                .bounds(rightLeft + 72, top + 132, 48, 18)
                .build());
        bowPageButton = addRenderableWidget(Button.builder(Component.translatable("screen.valet.page_bow"), button -> selectRightPage(RightPage.BOW))
                .bounds(rightLeft + 124, top + 132, 42, 18)
                .build());
        inventoryPageButton = addRenderableWidget(Button.builder(Component.translatable("screen.valet.page_inventory"), button -> selectRightPage(RightPage.INVENTORY))
                .bounds(rightLeft + 170, top + 132, 86, 18)
                .build());
        updatePageButtons();
        updateConstructionButtons();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        extractTransparentBackground(context);
        drawFrame(context, leftPos, topPos, imageWidth, imageHeight);
        drawPanel(context, getLeftPanelLeft(), getPanelTop(), LEFT_WIDTH, PANEL_HEIGHT);
        drawPanel(context, getRightPanelLeft(), getPanelTop(), RIGHT_WIDTH, PANEL_HEIGHT);
        drawOrderPanel(context, mouseX, mouseY);
        drawInfoPanel(context);
        if (selectedRightPage == RightPage.GENERAL) {
            drawPerkTree(context, mouseX, mouseY);
            drawPerkDetails(context, mouseX, mouseY);
        } else if (selectedRightPage == RightPage.INVENTORY) {
            drawInventoryPanel(context);
        } else {
            drawCombatTalentTree(context, mouseX, mouseY, selectedRightPage);
            drawCombatTalentDetails(context, mouseX, mouseY, selectedRightPage);
        }
        super.extractRenderState(context, mouseX, mouseY, delta);
        extractMouseoverTooltip(context, mouseX, mouseY);
    }

    private void drawOrderPanel(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        int left = getLeftPanelLeft();
        int top = getPanelTop();
        context.text(font, title, left + 8, top + 7, 0xFF303030, false);

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
            context.text(font, getOrderLabel(entry, selected), left + 12, rowTop + 6, selected ? 0xFF2B1A05 : 0xFF303030, false);
        }
        context.disableScissor();
        drawOrderScrollbar(context);
    }

    private void drawInfoPanel(GuiGraphicsExtractor context) {
        int left = getRightPanelLeft();
        int top = getPanelTop();

        context.text(font, Component.translatable("screen.valet.rename"), left + 10, top + 9, 0xFF303030, false);
        context.text(font, getAvailableTitle(), left + 10, top + 45, 0xFF303030, false);
        context.text(font, getSelectedText(), left + 10, top + 60, 0xFF1F1F1F, false);
        context.text(font, getHintText(), left + 10, top + 75, 0xFF5A5142, false);
        drawTargetPreview(context, left + RIGHT_WIDTH - CONSTRUCTION_PREVIEW_SIZE - 10, top + 66);
        context.text(font, Component.translatable("screen.valet.level", localLevel), left + 10, top + 93, 0xFF202020, false);
        drawXpBar(context, left + 10, top + 108);

        Component pageTitle = getRightPageTitle();
        context.text(font, pageTitle, left + 10, top + TREE_TITLE_OFFSET, 0xFF303030, false);
        if (selectedRightPage != RightPage.INVENTORY) {
            int points = selectedRightPage == RightPage.GENERAL ? localPendingPerks : getCombatPendingPoints(selectedRightPage);
            context.text(font, Component.translatable("screen.valet.pending_points", points), left + RIGHT_WIDTH - 72, top + TREE_TITLE_OFFSET, points > 0 ? 0xFF8A5A00 : 0xFF606060, false);
        }
    }

    private Component getRightPageTitle() {
        return switch (selectedRightPage) {
            case GENERAL -> Component.translatable("screen.valet.skill_tree");
            case SWORD -> Component.translatable("screen.valet.sword_tree");
            case BOW -> Component.translatable("screen.valet.bow_tree");
            case INVENTORY -> Component.translatable("screen.valet.inventory_title");
        };
    }

    private void drawInventoryPanel(GuiGraphicsExtractor context) {
        int left = getInventoryGridLeft();
        int top = getInventoryGridTop();
        int slotCount = Math.max(localInventoryStacks.size(), 8);
        boolean empty = true;

        for (int slot = 0; slot < slotCount; slot++) {
            int slotLeft = getInventorySlotLeft(slot);
            int slotTop = getInventorySlotTop(slot);
            ItemStack stack = getInventoryStack(slot);
            drawInset(context, slotLeft, slotTop, INVENTORY_SLOT_SIZE, INVENTORY_SLOT_SIZE, 0xFF9C9688);
            if (!stack.isEmpty()) {
                empty = false;
                context.item(stack, slotLeft + 1, slotTop + 1);
                context.itemDecorations(font, stack, slotLeft + 1, slotTop + 1);
            }
        }

        if (empty) {
            context.text(font, Component.translatable("screen.valet.inventory_empty"), left, top + 54, 0xFF5A5142, false);
        }
    }

    private int getInventoryGridLeft() {
        return getRightPanelLeft() + 10;
    }

    private int getInventoryGridTop() {
        return getPanelTop() + INVENTORY_TOP_OFFSET;
    }

    private int getInventorySlotLeft(int slot) {
        return getInventoryGridLeft() + (slot % INVENTORY_COLUMNS) * (INVENTORY_SLOT_SIZE + INVENTORY_SLOT_GAP);
    }

    private int getInventorySlotTop(int slot) {
        return getInventoryGridTop() + (slot / INVENTORY_COLUMNS) * (INVENTORY_SLOT_SIZE + INVENTORY_SLOT_GAP);
    }

    private ItemStack getInventoryStack(int slot) {
        return slot >= 0 && slot < localInventoryStacks.size() ? localInventoryStacks.get(slot) : ItemStack.EMPTY;
    }

    private ItemStack getHoveredInventoryStack(int mouseX, int mouseY) {
        int slotCount = Math.max(localInventoryStacks.size(), 8);
        for (int slot = 0; slot < slotCount; slot++) {
            if (isInside(mouseX, mouseY, getInventorySlotLeft(slot), getInventorySlotTop(slot), INVENTORY_SLOT_SIZE, INVENTORY_SLOT_SIZE)) {
                return getInventoryStack(slot);
            }
        }
        return ItemStack.EMPTY;
    }

    private void drawTargetPreview(GuiGraphicsExtractor context, int left, int top) {
        if (selectedCategory == TargetCategory.CONSTRUCTION) {
            drawConstructionPreview(context, getSelectedConstruction(), left, top);
            return;
        }
        if (selectedCategory == TargetCategory.CRAFT) {
            drawCraftPreview(context, getSelectedCraftTarget(), left, top);
        }
    }

    private void drawCraftPreview(GuiGraphicsExtractor context, ValetCraftTarget target, int left, int top) {
        if (target == null) {
            return;
        }

        drawInset(context, left, top, CONSTRUCTION_PREVIEW_SIZE, CONSTRUCTION_PREVIEW_SIZE, 0xFF9FA394);
        ItemStack output = new ItemStack(target.getOutputItem());
        int itemLeft = left + (CONSTRUCTION_PREVIEW_SIZE - 16) / 2;
        int itemTop = top + 9;
        context.item(output, itemLeft, itemTop);
        context.itemDecorations(font, output, itemLeft, itemTop);
    }

    private void drawCombatTalentTree(GuiGraphicsExtractor context, int mouseX, int mouseY, RightPage page) {
        int left = getRightPanelLeft();
        int top = getPanelTop();
        drawCombatSkillHeader(context, page, left + 10, top + COMBAT_HEADER_OFFSET);

        if (page == RightPage.SWORD) {
            CombatPerkNode strength = getCombatNode(ValetCombatPerk.SWORD_STRENGTH);
            drawCombatConnection(context, strength, getCombatNode(ValetCombatPerk.SWORD_RECOVERY));
            drawCombatConnection(context, strength, getCombatNode(ValetCombatPerk.SWORD_DEFENSE));
        } else {
            CombatPerkNode awareness = getCombatNode(ValetCombatPerk.ALLY_AWARENESS);
            CombatPerkNode strength = getCombatNode(ValetCombatPerk.BOW_STRENGTH);
            drawCombatConnection(context, awareness, getCombatNode(ValetCombatPerk.BOW_QUICK_SHOT));
            drawCombatConnection(context, awareness, strength);
            drawCombatConnection(context, strength, getCombatNode(ValetCombatPerk.BOW_RECYCLE_ARROW));
        }

        for (ValetCombatPerk perk : getCombatTreePerks(page)) {
            drawCombatPerkNode(context, getCombatNode(perk), mouseX, mouseY);
        }
    }

    private void drawCombatSkillHeader(GuiGraphicsExtractor context, RightPage page, int left, int top) {
        boolean sword = page == RightPage.SWORD;
        int level = sword ? localSwordLevel : localBowLevel;
        int xp = sword ? localSwordXp : localBowXp;
        int nextLevelXp = sword ? localSwordNextLevelXp : localBowNextLevelXp;
        int color = sword ? 0xFFB84A3D : 0xFF3E7FB5;
        Component label = Component.translatable(sword ? "screen.valet.skill_sword" : "screen.valet.skill_bow")
                .copy()
                .append(" ")
                .append(Component.translatable("screen.valet.level", level));

        context.text(font, label, left, top, 0xFF303030, false);
        int barTop = top + 13;
        int maxXp = Math.max(1, nextLevelXp);
        int fillWidth = Math.min(XP_BAR_WIDTH, Math.max(0, xp) * XP_BAR_WIDTH / maxXp);
        context.fill(left, barTop, left + XP_BAR_WIDTH, barTop + XP_BAR_HEIGHT, 0xFF4C4C4C);
        context.fill(left + 1, barTop + 1, left + XP_BAR_WIDTH - 1, barTop + XP_BAR_HEIGHT - 1, 0xFF222222);
        context.fill(left + 1, barTop + 1, left + 1 + fillWidth, barTop + XP_BAR_HEIGHT - 1, color);
        context.text(font, Component.translatable("screen.valet.xp", xp, nextLevelXp), left, barTop + 13, 0xFF4A4030, false);
    }

    private void drawCombatPerkNode(GuiGraphicsExtractor context, CombatPerkNode node, int mouseX, int mouseY) {
        boolean owned = hasLocalCombatPerk(node.perk);
        boolean hovered = isInside(mouseX, mouseY, node.left, node.top, NODE_SIZE, NODE_SIZE);
        boolean selected = selectedCombatPerk == node.perk;
        int pending = getCombatPendingPoints(getPageForCombatPerk(node.perk));
        boolean learnable = !owned && pending > 0 && canLearnCombatPerk(node.perk);
        int border = owned ? 0xFFFFD36B : learnable ? 0xFF7F9CCB : 0xFF555555;
        int fill = owned ? 0xFFB9872D : learnable ? 0xFF505C72 : 0xFF767676;

        if (hovered || selected) {
            border = 0xFFFFFFFF;
        }

        drawInset(context, node.left, node.top, NODE_SIZE, NODE_SIZE, border);
        context.fill(node.left + 3, node.top + 3, node.left + NODE_SIZE - 3, node.top + NODE_SIZE - 3, fill);
        context.centeredText(font, getCombatPerkIcon(node.perk, owned), node.left + NODE_SIZE / 2, node.top + 10, 0xFFFFFFFF);
    }

    private void drawCombatConnection(GuiGraphicsExtractor context, CombatPerkNode from, CombatPerkNode to) {
        int color = hasLocalCombatPerk(to.perk) ? 0xFFD39A35 : hasLocalCombatPerk(from.perk) ? 0xFF7F9CCB : 0xFF555555;
        int x1 = from.centerX();
        int y1 = from.centerY();
        int x2 = to.centerX();
        int y2 = to.centerY();
        int midX = (x1 + x2) / 2;

        context.fill(Math.min(x1, midX), y1 - 1, Math.max(x1, midX) + 1, y1 + 1, color);
        context.fill(midX - 1, Math.min(y1, y2), midX + 1, Math.max(y1, y2) + 1, color);
        context.fill(Math.min(midX, x2), y2 - 1, Math.max(midX, x2) + 1, y2 + 1, color);
    }

    private void drawCombatTalentDetails(GuiGraphicsExtractor context, int mouseX, int mouseY, RightPage page) {
        ValetCombatPerk perk = getHoveredCombatPerk(mouseX, mouseY, page);
        if (perk == null) {
            perk = combatPerkBelongsToPage(selectedCombatPerk, page) ? selectedCombatPerk : getDefaultCombatPerk(page);
        }

        int left = getRightPanelLeft() + 10;
        int top = getPanelTop() + TREE_DETAIL_OFFSET;
        int width = RIGHT_WIDTH - 20;
        drawInset(context, left, top, width, TREE_DETAIL_HEIGHT, 0xFFB8B19E);
        context.text(font, Component.translatable(perk.getTranslationKey()), left + 7, top + 6, 0xFF202020, false);
        context.text(font, Component.translatable(perk.getDescriptionKey()), left + 7, top + 18, 0xFF4A4030, false);

        Component status = getCombatPerkStatus(perk);
        context.text(font, status, left + width - font.width(status) - 7, top + 6, hasLocalCombatPerk(perk) ? 0xFF8A5A00 : 0xFF555555, false);
    }

    private void drawPerkTree(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        PerkNode speed = getNode(ValetPerk.SPEED);
        PerkNode vision = getNode(ValetPerk.VISION);
        PerkNode movement = getNode(ValetPerk.MOVEMENT);

        drawConnection(context, speed, vision);
        drawConnection(context, speed, movement);

        for (ValetPerk perk : RESOURCE_TREE_PERKS) {
            drawPerkNode(context, getNode(perk), mouseX, mouseY);
        }
    }

    private void drawPerkNode(GuiGraphicsExtractor context, PerkNode node, int mouseX, int mouseY) {
        boolean owned = hasLocalPerk(node.perk);
        boolean hovered = isInside(mouseX, mouseY, node.left, node.top, NODE_SIZE, NODE_SIZE);
        boolean selected = selectedPerk == node.perk;
        boolean learnable = !owned && localPendingPerks > 0 && canLearnPerk(node.perk);
        int border = owned ? 0xFFFFD36B : learnable ? 0xFF7F9CCB : 0xFF555555;
        int fill = owned ? 0xFFB9872D : learnable ? 0xFF505C72 : 0xFF767676;

        if (hovered || selected) {
            border = 0xFFFFFFFF;
        }

        drawInset(context, node.left, node.top, NODE_SIZE, NODE_SIZE, border);
        context.fill(node.left + 3, node.top + 3, node.left + NODE_SIZE - 3, node.top + NODE_SIZE - 3, fill);
        context.centeredText(font, getPerkIcon(node.perk, owned), node.left + NODE_SIZE / 2, node.top + 10, 0xFFFFFFFF);
    }

    private void drawConnection(GuiGraphicsExtractor context, PerkNode from, PerkNode to) {
        int color = hasLocalPerk(to.perk) ? 0xFFD39A35 : hasLocalPerk(from.perk) ? 0xFF7F9CCB : 0xFF555555;
        int x1 = from.centerX();
        int y1 = from.centerY();
        int x2 = to.centerX();
        int y2 = to.centerY();
        int midX = (x1 + x2) / 2;

        context.fill(Math.min(x1, midX), y1 - 1, Math.max(x1, midX) + 1, y1 + 1, color);
        context.fill(midX - 1, Math.min(y1, y2), midX + 1, Math.max(y1, y2) + 1, color);
        context.fill(Math.min(midX, x2), y2 - 1, Math.max(midX, x2) + 1, y2 + 1, color);
    }

    private void drawPerkDetails(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        ValetPerk perk = getHoveredPerk(mouseX, mouseY);
        if (perk == null) {
            perk = selectedPerk;
        }

        int left = getRightPanelLeft() + 10;
        int top = getPanelTop() + TREE_DETAIL_OFFSET;
        int width = RIGHT_WIDTH - 20;
        drawInset(context, left, top, width, TREE_DETAIL_HEIGHT, 0xFFB8B19E);
        context.text(font, Component.translatable(perk.getTranslationKey()), left + 7, top + 6, 0xFF202020, false);
        context.text(font, Component.translatable(getPerkDescriptionKey(perk)), left + 7, top + 18, 0xFF4A4030, false);

        Component status = getPerkStatus(perk);
        context.text(font, status, left + width - font.width(status) - 7, top + 6, hasLocalPerk(perk) ? 0xFF8A5A00 : 0xFF555555, false);
    }

    private void drawXpBar(GuiGraphicsExtractor context, int left, int top) {
        int maxXp = Math.max(1, localNextLevelXp);
        int fillWidth = Math.min(XP_BAR_WIDTH, localXp * XP_BAR_WIDTH / maxXp);

        context.fill(left, top, left + XP_BAR_WIDTH, top + XP_BAR_HEIGHT, 0xFF4C4C4C);
        context.fill(left + 1, top + 1, left + XP_BAR_WIDTH - 1, top + XP_BAR_HEIGHT - 1, 0xFF222222);
        context.fill(left + 1, top + 1, left + 1 + fillWidth, top + XP_BAR_HEIGHT - 1, 0xFF70B536);
        context.text(font, Component.translatable("screen.valet.xp", localXp, localNextLevelXp), left, top + 13, 0xFF4A4030, false);
    }

    private void drawFrame(GuiGraphicsExtractor context, int left, int top, int width, int height) {
        context.fill(left, top, left + width, top + height, 0xFF1F1A14);
        context.fill(left + 1, top + 1, left + width - 1, top + height - 1, 0xFFE7D6A3);
        context.fill(left + 3, top + 3, left + width - 3, top + height - 3, 0xFF5A4631);
        context.fill(left + 5, top + 5, left + width - 5, top + height - 5, 0xFFC8BEA6);
    }

    private void drawPanel(GuiGraphicsExtractor context, int left, int top, int width, int height) {
        context.fill(left, top, left + width, top + height, 0xFF3C3226);
        context.fill(left + 1, top + 1, left + width - 1, top + height - 1, 0xFFE4D8BD);
        context.fill(left + 3, top + 3, left + width - 3, top + height - 3, 0xFFB8B19E);
        context.fill(left + 4, top + 4, left + width - 4, top + height - 4, 0xFFD1CAB8);
    }

    private void drawInset(GuiGraphicsExtractor context, int left, int top, int width, int height, int fill) {
        context.fill(left, top, left + width, top + height, 0xFF5A5142);
        context.fill(left + 1, top + 1, left + width - 1, top + height - 1, 0xFFE9DFCA);
        context.fill(left + 2, top + 2, left + width - 2, top + height - 2, fill);
    }

    private void rebuildOrderEntries() {
        orderEntries.clear();
        orderEntries.add(OrderEntry.target(ValetOrder.NONE, -1, Component.translatable("order.valet.none")));
        orderEntries.add(OrderEntry.category(TargetCategory.ORE, Component.translatable("screen.valet.category_ores")));
        if (selectedCategory == TargetCategory.ORE) {
            for (ValetMineTarget target : ValetMineTarget.values()) {
                int count = getLocalOreCount(target);
                if (count > 0) {
                    orderEntries.add(OrderEntry.target(ValetOrder.MINE_ORES, target.ordinal(), Component.literal("  ").append(Component.translatable("screen.valet.ore_count", Component.translatable(target.getTranslationKey()), count))));
                }
            }
        }

        orderEntries.add(OrderEntry.category(TargetCategory.WOOD, Component.translatable("screen.valet.category_wood")));
        if (selectedCategory == TargetCategory.WOOD) {
            for (ValetWoodTarget target : ValetWoodTarget.values()) {
                int count = getLocalWoodCount(target);
                if (count > 0) {
                    orderEntries.add(OrderEntry.target(ValetOrder.CHOP_WOOD, target.ordinal(), Component.literal("  ").append(Component.translatable("screen.valet.wood_count", Component.translatable(target.getTranslationKey()), count))));
                }
            }
        }

        orderEntries.add(OrderEntry.category(TargetCategory.CONSTRUCTION, Component.translatable("screen.valet.category_constructions")));
        if (selectedCategory == TargetCategory.CONSTRUCTION) {
            for (ValetConstructionBlueprint construction : localConstructions) {
                orderEntries.add(OrderEntry.target(ValetOrder.BUILD_STRUCTURE, construction.id(), Component.literal("  ").append(Component.translatable("screen.valet.construction_count", construction.name(), construction.blockCount()))));
            }
        }

        orderEntries.add(OrderEntry.category(TargetCategory.CRAFT, Component.translatable("screen.valet.category_craft")));
        if (selectedCategory == TargetCategory.CRAFT) {
            for (ValetCraftTarget target : ValetCraftTarget.values()) {
                orderEntries.add(OrderEntry.target(ValetOrder.CRAFT, target.ordinal(), Component.literal("  ").append(Component.translatable(target.getTranslationKey()))));
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
        if (entry.order == ValetOrder.CRAFT) {
            return selectedCategory == TargetCategory.CRAFT && selectedCraftTargetIndex == entry.targetIndex;
        }
        return false;
    }

    private Component getAvailableTitle() {
        return switch (selectedCategory) {
            case ORE -> Component.translatable("screen.valet.available_ores");
            case WOOD -> Component.translatable("screen.valet.available_wood");
            case CONSTRUCTION -> Component.translatable("screen.valet.available_constructions");
            case CRAFT -> Component.translatable("screen.valet.available_craft");
            case NONE -> Component.translatable("screen.valet.available_targets");
        };
    }

    private Component getCategoryText(TargetCategory category) {
        return switch (category) {
            case ORE -> Component.translatable("screen.valet.category_ores");
            case WOOD -> Component.translatable("screen.valet.category_wood");
            case CONSTRUCTION -> Component.translatable("screen.valet.category_constructions");
            case CRAFT -> Component.translatable("screen.valet.category_craft");
            case NONE -> Component.translatable("order.valet.none");
        };
    }

    private Component getOrderLabel(OrderEntry entry, boolean selected) {
        return selected ? Component.literal("> ").append(entry.label) : entry.label;
    }

    private Component getSelectedText() {
        if (selectedCategory == TargetCategory.ORE) {
            ValetMineTarget target = ValetMineTarget.fromIndex(selectedMineTargetIndex);
            return target == null ? getCategoryText(TargetCategory.ORE) : Component.translatable(target.getTranslationKey());
        }
        if (selectedCategory == TargetCategory.WOOD) {
            ValetWoodTarget target = ValetWoodTarget.fromIndex(selectedWoodTargetIndex);
            return target == null ? getCategoryText(TargetCategory.WOOD) : Component.translatable(target.getTranslationKey());
        }
        if (selectedCategory == TargetCategory.CONSTRUCTION) {
            ValetConstructionBlueprint construction = getSelectedConstruction();
            return construction == null ? getCategoryText(TargetCategory.CONSTRUCTION) : Component.literal(construction.name());
        }
        if (selectedCategory == TargetCategory.CRAFT) {
            ValetCraftTarget target = ValetCraftTarget.fromIndex(selectedCraftTargetIndex);
            return target == null ? getCategoryText(TargetCategory.CRAFT) : Component.translatable(target.getTranslationKey());
        }
        return Component.translatable("order.valet.none");
    }

    private Component getHintText() {
        if (selectedCategory == TargetCategory.CONSTRUCTION) {
            return localConstructions.isEmpty() ? Component.translatable("screen.valet.no_constructions") : Component.translatable("screen.valet.build_hint");
        }
        if (selectedCategory == TargetCategory.CRAFT) {
            return Component.translatable("screen.valet.craft_hint");
        }
        return Component.translatable("screen.valet.mine_hint");
    }

    private ValetConstructionBlueprint getSelectedConstruction() {
        for (ValetConstructionBlueprint construction : localConstructions) {
            if (construction.id() == selectedConstructionTargetId) {
                return construction;
            }
        }
        return null;
    }

    private ValetCraftTarget getSelectedCraftTarget() {
        return ValetCraftTarget.fromIndex(selectedCraftTargetIndex);
    }

    private PerkNode getNode(ValetPerk perk) {
        int left = getRightPanelLeft();
        int top = getPanelTop();
        return switch (perk) {
            case SPEED -> new PerkNode(perk, left + TREE_CENTER_X, top + TREE_BOTTOM_ROW);
            case VISION -> new PerkNode(perk, left + TREE_LEFT_X, top + TREE_MIDDLE_ROW);
            case MOVEMENT -> new PerkNode(perk, left + TREE_RIGHT_X, top + TREE_MIDDLE_ROW);
            default -> new PerkNode(perk, left + TREE_CENTER_X, top + TREE_BOTTOM_ROW);
        };
    }

    private CombatPerkNode getCombatNode(ValetCombatPerk perk) {
        int left = getRightPanelLeft();
        int top = getPanelTop();
        return switch (perk) {
            case SWORD_STRENGTH -> new CombatPerkNode(perk, left + TREE_CENTER_X, top + TREE_BOTTOM_ROW);
            case SWORD_RECOVERY -> new CombatPerkNode(perk, left + TREE_LEFT_X, top + TREE_MIDDLE_ROW);
            case SWORD_DEFENSE -> new CombatPerkNode(perk, left + TREE_RIGHT_X, top + TREE_MIDDLE_ROW);
            case ALLY_AWARENESS -> new CombatPerkNode(perk, left + TREE_CENTER_X, top + TREE_BOTTOM_ROW);
            case BOW_QUICK_SHOT -> new CombatPerkNode(perk, left + TREE_LEFT_X, top + TREE_MIDDLE_ROW);
            case BOW_STRENGTH -> new CombatPerkNode(perk, left + TREE_RIGHT_X, top + TREE_MIDDLE_ROW);
            case BOW_RECYCLE_ARROW -> new CombatPerkNode(perk, left + TREE_RIGHT_X, top + TREE_TOP_ROW);
        };
    }

    private ValetPerk getHoveredPerk(int mouseX, int mouseY) {
        for (ValetPerk perk : RESOURCE_TREE_PERKS) {
            PerkNode node = getNode(perk);
            if (isInside(mouseX, mouseY, node.left, node.top, NODE_SIZE, NODE_SIZE)) {
                return perk;
            }
        }
        return null;
    }

    private ValetCombatPerk getHoveredCombatPerk(int mouseX, int mouseY, RightPage page) {
        for (ValetCombatPerk perk : getCombatTreePerks(page)) {
            CombatPerkNode node = getCombatNode(perk);
            if (isInside(mouseX, mouseY, node.left, node.top, NODE_SIZE, NODE_SIZE)) {
                return perk;
            }
        }
        return null;
    }

    private ValetCombatPerk getDefaultCombatPerk(RightPage page) {
        return page == RightPage.SWORD ? ValetCombatPerk.SWORD_STRENGTH : ValetCombatPerk.ALLY_AWARENESS;
    }

    private boolean combatPerkBelongsToPage(ValetCombatPerk perk, RightPage page) {
        return switch (page) {
            case SWORD -> perk.getTree() == com.wawane.valet.progress.ValetCombatSkillTree.SWORD;
            case BOW -> perk.getTree() == com.wawane.valet.progress.ValetCombatSkillTree.BOW;
            case GENERAL, INVENTORY -> false;
        };
    }

    private RightPage getPageForCombatPerk(ValetCombatPerk perk) {
        return perk.getTree() == com.wawane.valet.progress.ValetCombatSkillTree.SWORD ? RightPage.SWORD : RightPage.BOW;
    }

    private Component getPerkIcon(ValetPerk perk, boolean owned) {
        if (owned) {
            return Component.literal("*");
        }

        return Component.literal(perk.getIcon());
    }

    private Component getCombatPerkIcon(ValetCombatPerk perk, boolean owned) {
        if (owned) {
            return Component.literal("*");
        }

        return Component.literal(perk.getIcon());
    }

    private Component getPerkStatus(ValetPerk perk) {
        if (hasLocalPerk(perk)) {
            return Component.translatable("screen.valet.perk_owned");
        }
        if (!canLearnPerk(perk)) {
            return Component.translatable("screen.valet.perk_locked");
        }
        if (localPendingPerks > 0) {
            return Component.translatable("screen.valet.perk_learn");
        }
        return Component.translatable("screen.valet.perk_no_points");
    }

    private Component getCombatPerkStatus(ValetCombatPerk perk) {
        if (hasLocalCombatPerk(perk)) {
            return Component.translatable("screen.valet.perk_owned");
        }
        if (!canLearnCombatPerk(perk)) {
            return Component.translatable("screen.valet.perk_locked");
        }
        if (getCombatPendingPoints(getPageForCombatPerk(perk)) > 0) {
            return Component.translatable("screen.valet.perk_learn");
        }
        return Component.translatable("screen.valet.perk_no_points");
    }

    private String getPerkDescriptionKey(ValetPerk perk) {
        return perk.getDescriptionKey();
    }

    private int getLocalOreCount(ValetMineTarget target) {
        int index = target.ordinal();
        return localOreCounts != null && index >= 0 && index < localOreCounts.length ? localOreCounts[index] : 0;
    }

    private int getLocalWoodCount(ValetWoodTarget target) {
        int index = target.ordinal();
        return localWoodCounts != null && index >= 0 && index < localWoodCounts.length ? localWoodCounts[index] : 0;
    }

    private boolean hasLocalPerk(ValetPerk perk) {
        return localPerks[perk.ordinal()];
    }

    private void setLocalPerk(ValetPerk perk) {
        localPerks[perk.ordinal()] = true;
    }

    private boolean hasLocalCombatPerk(ValetCombatPerk perk) {
        int index = perk.ordinal();
        return index >= 0 && index < localCombatPerks.length && localCombatPerks[index];
    }

    private void setLocalCombatPerk(ValetCombatPerk perk) {
        localCombatPerks[perk.ordinal()] = true;
    }

    private boolean canLearnPerk(ValetPerk perk) {
        return switch (perk) {
            case SPEED -> true;
            case VISION, MOVEMENT -> hasLocalPerk(ValetPerk.SPEED);
            default -> true;
        };
    }

    private boolean canLearnCombatPerk(ValetCombatPerk perk) {
        return switch (perk) {
            case SWORD_STRENGTH, ALLY_AWARENESS -> true;
            case SWORD_RECOVERY, SWORD_DEFENSE -> hasLocalCombatPerk(ValetCombatPerk.SWORD_STRENGTH);
            case BOW_QUICK_SHOT, BOW_STRENGTH -> hasLocalCombatPerk(ValetCombatPerk.ALLY_AWARENESS);
            case BOW_RECYCLE_ARROW -> hasLocalCombatPerk(ValetCombatPerk.BOW_STRENGTH);
        };
    }

    private ValetCombatPerk[] getCombatTreePerks(RightPage page) {
        return page == RightPage.SWORD ? SWORD_TREE_PERKS : BOW_TREE_PERKS;
    }

    private int getCombatPendingPoints(RightPage page) {
        return switch (page) {
            case SWORD -> localSwordPendingPerks;
            case BOW -> localBowPendingPerks;
            case GENERAL -> localPendingPerks;
            case INVENTORY -> 0;
        };
    }

    private int getLeftPanelLeft() {
        return leftPos + PANEL_MARGIN;
    }

    private int getRightPanelLeft() {
        return leftPos + PANEL_MARGIN + LEFT_WIDTH + PANEL_GAP;
    }

    private int getPanelTop() {
        return topPos + PANEL_TOP;
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

    private void drawOrderScrollbar(GuiGraphicsExtractor context) {
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

    private void extractMouseoverTooltip(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        if (selectedRightPage == RightPage.INVENTORY) {
            ItemStack hoveredStack = getHoveredInventoryStack(mouseX, mouseY);
            if (!hoveredStack.isEmpty()) {
                context.setTooltipForNextFrame(font, hoveredStack, mouseX, mouseY);
            }
        }
    }
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
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

        if (selectedRightPage == RightPage.GENERAL) {
            ValetPerk hoveredPerk = getHoveredPerk((int) mouseX, (int) mouseY);
            if (hoveredPerk != null) {
                selectedPerk = hoveredPerk;
                if (localPendingPerks > 0 && !hasLocalPerk(hoveredPerk) && canLearnPerk(hoveredPerk)) {
                    sendPerkSelection(hoveredPerk);
                }
                return true;
            }
        } else if (selectedRightPage != RightPage.INVENTORY) {
            ValetCombatPerk hoveredPerk = getHoveredCombatPerk((int) mouseX, (int) mouseY, selectedRightPage);
            if (hoveredPerk != null) {
                selectedCombatPerk = hoveredPerk;
                if (getCombatPendingPoints(selectedRightPage) > 0 && !hasLocalCombatPerk(hoveredPerk) && canLearnCombatPerk(hoveredPerk)) {
                    sendCombatPerkSelection(hoveredPerk);
                }
                return true;
            }
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (isInside(mouseX, mouseY, getLeftPanelLeft() + 7, getOrderListTop(), LEFT_WIDTH - 14, getOrderListHeight()) && getMaxOrderScroll() > 0) {
            scrollOrders((int) (-verticalAmount * ORDER_ROW_STRIDE));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    public int getValetEntityId() {
        return viewModel.valetEntityId();
    }

    public void applyServerState(int orderIndex, int mineTargetIndex, int woodTargetIndex, int constructionTargetId, int craftTargetIndex, int[] oreCounts, int[] woodCounts, List<ItemStack> inventoryStacks, int level, int xp, int nextLevelXp, int pendingPerks, boolean[] perks, boolean[] combatPerks, int swordLevel, int swordXp, int swordNextLevelXp, int swordPendingPerks, int bowLevel, int bowXp, int bowNextLevelXp, int bowPendingPerks, boolean allyAwareness, String valetName) {
        ValetOrder order = ValetOrder.fromIndex(orderIndex);
        selectedCategory = order == ValetOrder.CHOP_WOOD ? TargetCategory.WOOD : order == ValetOrder.MINE_ORES ? TargetCategory.ORE : order == ValetOrder.BUILD_STRUCTURE ? TargetCategory.CONSTRUCTION : order == ValetOrder.CRAFT ? TargetCategory.CRAFT : TargetCategory.NONE;
        selectedMineTargetIndex = mineTargetIndex;
        selectedWoodTargetIndex = woodTargetIndex;
        selectedConstructionTargetId = constructionTargetId;
        selectedCraftTargetIndex = craftTargetIndex;
        localLevel = level;
        localXp = xp;
        localNextLevelXp = nextLevelXp;
        localPendingPerks = pendingPerks;
        localSwordLevel = swordLevel;
        localSwordXp = swordXp;
        localSwordNextLevelXp = swordNextLevelXp;
        localSwordPendingPerks = swordPendingPerks;
        localBowLevel = bowLevel;
        localBowXp = bowXp;
        localBowNextLevelXp = bowNextLevelXp;
        localBowPendingPerks = bowPendingPerks;
        localAllyAwareness = allyAwareness;
        localOreCounts = Arrays.copyOf(oreCounts, ValetMineTarget.values().length);
        localWoodCounts = Arrays.copyOf(woodCounts, ValetWoodTarget.values().length);
        localInventoryStacks = copyInventory(inventoryStacks);
        for (ValetPerk perk : ValetPerk.values()) {
            localPerks[perk.ordinal()] = perk.ordinal() < perks.length && perks[perk.ordinal()];
        }
        for (ValetCombatPerk perk : ValetCombatPerk.values()) {
            localCombatPerks[perk.ordinal()] = perk.ordinal() < combatPerks.length && combatPerks[perk.ordinal()];
        }
        localValetName = valetName;
        if (nameField != null) {
            nameField.setValue(localValetName);
        }
        rebuildOrderEntries();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int keyCode = event.key();
        if (nameField != null && nameField.isFocused()) {
            if (keyCode == 257 || keyCode == 335) {
                sendRename();
                return true;
            }
            if (keyCode == 256) {
                nameField.setFocused(false);
                return true;
            }
            nameField.keyPressed(event);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (nameField != null && nameField.isFocused()) {
            return nameField.charTyped(event);
        }
        return super.charTyped(event);
    }

    @Override
    public void onClose() {
        if (currentScreen == this) {
            currentScreen = null;
        }
        super.onClose();
    }

    @Override
    public void removed() {
        if (currentScreen == this) {
            currentScreen = null;
        }
        super.removed();
    }

    private void sendSelection(ValetOrder order, int targetIndex) {
        if (order == ValetOrder.NONE) {
            selectedCategory = TargetCategory.NONE;
            selectedMineTargetIndex = -1;
            selectedWoodTargetIndex = -1;
            selectedConstructionTargetId = -1;
            selectedCraftTargetIndex = -1;
        } else if (order == ValetOrder.MINE_ORES) {
            selectedCategory = TargetCategory.ORE;
            selectedMineTargetIndex = targetIndex;
        } else if (order == ValetOrder.CHOP_WOOD) {
            selectedCategory = TargetCategory.WOOD;
            selectedWoodTargetIndex = targetIndex;
        } else if (order == ValetOrder.BUILD_STRUCTURE) {
            selectedCategory = TargetCategory.CONSTRUCTION;
            selectedConstructionTargetId = targetIndex;
        } else if (order == ValetOrder.CRAFT) {
            selectedCategory = TargetCategory.CRAFT;
            selectedCraftTargetIndex = targetIndex;
        }
        rebuildOrderEntries();

        ClientPlayNetworking.send(new SetOrderPayload(viewModel.valetEntityId(), order, targetIndex));
    }

    private void sendPerkSelection(ValetPerk perk) {
        if (localPendingPerks <= 0 || hasLocalPerk(perk) || !canLearnPerk(perk)) {
            return;
        }

        setLocalPerk(perk);
        localPendingPerks = Math.max(0, localPendingPerks - 1);

        ClientPlayNetworking.send(new ChoosePerkPayload(viewModel.valetEntityId(), perk));
    }

    private void sendCombatPerkSelection(ValetCombatPerk perk) {
        RightPage page = getPageForCombatPerk(perk);
        if (getCombatPendingPoints(page) <= 0 || hasLocalCombatPerk(perk) || !canLearnCombatPerk(perk)) {
            return;
        }

        setLocalCombatPerk(perk);
        if (page == RightPage.SWORD) {
            localSwordPendingPerks = Math.max(0, localSwordPendingPerks - 1);
        } else if (page == RightPage.BOW) {
            localBowPendingPerks = Math.max(0, localBowPendingPerks - 1);
        }

        ClientPlayNetworking.send(new ChooseCombatPerkPayload(viewModel.valetEntityId(), perk));
    }

    private void sendRename() {
        if (nameField == null) {
            return;
        }

        localValetName = nameField.getValue().trim();
        ClientPlayNetworking.send(new RenameValetPayload(viewModel.valetEntityId(), localValetName));
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

        ClientPlayNetworking.send(new DeleteConstructionPayload(viewModel.valetEntityId(), constructionId));
    }

    private void selectRightPage(RightPage page) {
        selectedRightPage = page;
        if ((page == RightPage.SWORD || page == RightPage.BOW) && !combatPerkBelongsToPage(selectedCombatPerk, page)) {
            selectedCombatPerk = getDefaultCombatPerk(page);
        }
        updatePageButtons();
        updateConstructionButtons();
    }

    private void updatePageButtons() {
        updatePageButton(generalPageButton, RightPage.GENERAL, "screen.valet.page_general");
        updatePageButton(swordPageButton, RightPage.SWORD, "screen.valet.page_sword");
        updatePageButton(bowPageButton, RightPage.BOW, "screen.valet.page_bow");
        updatePageButton(inventoryPageButton, RightPage.INVENTORY, "screen.valet.page_inventory");
    }

    private void updatePageButton(Button button, RightPage page, String translationKey) {
        if (button == null) {
            return;
        }

        button.active = selectedRightPage != page;
        button.setMessage(Component.literal(selectedRightPage == page ? "> " : "").append(Component.translatable(translationKey)));
    }

    private void updateConstructionButtons() {
        boolean hasSelectedConstruction = selectedRightPage == RightPage.GENERAL && selectedCategory == TargetCategory.CONSTRUCTION && getSelectedConstruction() != null;
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

    private void drawConstructionPreview(GuiGraphicsExtractor context, ValetConstructionBlueprint blueprint, int left, int top) {
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

    private static List<ItemStack> copyInventory(List<ItemStack> stacks) {
        List<ItemStack> result = new ArrayList<>(stacks.size());
        for (ItemStack stack : stacks) {
            result.add(stack.copy());
        }
        return result;
    }

    private enum RightPage {
        GENERAL,
        SWORD,
        BOW,
        INVENTORY
    }

    private enum TargetCategory {
        NONE,
        ORE,
        WOOD,
        CONSTRUCTION,
        CRAFT
    }

    private static final class OrderEntry {
        private final ValetOrder order;
        private final int targetIndex;
        private final Component label;
        private final TargetCategory category;
        private final boolean categoryOnly;

        private OrderEntry(ValetOrder order, int targetIndex, Component label, TargetCategory category, boolean categoryOnly) {
            this.order = order;
            this.targetIndex = targetIndex;
            this.label = label;
            this.category = category;
            this.categoryOnly = categoryOnly;
        }

        private static OrderEntry target(ValetOrder order, int targetIndex, Component label) {
            return new OrderEntry(order, targetIndex, label, TargetCategory.NONE, false);
        }

        private static OrderEntry category(TargetCategory category, Component label) {
            return new OrderEntry(ValetOrder.NONE, -1, label, category, true);
        }
    }

    private static final class CombatPerkNode {
        private final ValetCombatPerk perk;
        private final int left;
        private final int top;

        private CombatPerkNode(ValetCombatPerk perk, int left, int top) {
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
