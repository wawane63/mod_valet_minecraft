package com.wawane.valet.gui;

import com.wawane.valet.construction.ValetConstructionBlueprint;
import com.wawane.valet.ValetRole;
import com.wawane.valet.breeding.ValetAnimalArea;
import com.wawane.valet.breeding.ValetAnimalType;
import com.wawane.valet.farm.ValetFarmArea;
import com.wawane.valet.order.ValetCraftTarget;
import com.wawane.valet.order.ValetFarmCrop;
import com.wawane.valet.order.ValetMineTarget;
import com.wawane.valet.order.ValetOrder;
import com.wawane.valet.order.ValetWoodTarget;
import com.wawane.valet.progress.ValetCombatPerk;
import com.wawane.valet.progress.ValetPerk;
import com.wawane.valet.network.packets.ChooseCombatPerkPayload;
import com.wawane.valet.network.packets.ChoosePerkPayload;
import com.wawane.valet.network.packets.DeleteConstructionPayload;
import com.wawane.valet.network.packets.RenameValetPayload;
import com.wawane.valet.network.packets.SetBehaviorPayload;
import com.wawane.valet.network.packets.SetBreedingOrderPayload;
import com.wawane.valet.network.packets.SetFarmOrderPayload;
import com.wawane.valet.network.packets.SetOrderPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
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
    private static final int ORDER_LIST_BOTTOM_PADDING = 54;
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
    private static final int TREE_FAR_LEFT_X = 30;
    private static final int TREE_FAR_RIGHT_X = 218;
    private static final int INVENTORY_TOP_OFFSET = 184;
    private static final int INVENTORY_COLUMNS = 6;
    private static final int INVENTORY_SLOT_SIZE = 18;
    private static final int INVENTORY_SLOT_GAP = 5;
    private static final ValetPerk[] ARTISAN_TREE_PERKS = {ValetPerk.SPEED, ValetPerk.VISION, ValetPerk.MOVEMENT, ValetPerk.STORAGE, ValetPerk.PATHING, ValetPerk.VEIN, ValetPerk.HAUL, ValetPerk.LIGHTING};
    private static final ValetPerk[] FARMER_TREE_PERKS = {ValetPerk.FARM_HANDS, ValetPerk.FARM_RANGE, ValetPerk.FARM_REPLANTING, ValetPerk.FARM_TILLING, ValetPerk.FARM_STORAGE, ValetPerk.FARM_STEWARD};
    private static final ValetPerk[] MAGIC_TREE_PERKS = {
            ValetPerk.MAGIC_ICE,
            ValetPerk.MAGIC_FANGS,
            ValetPerk.MAGIC_SHATTER,
            ValetPerk.MAGIC_HEAL,
            ValetPerk.MAGIC_REGEN_AURA,
            ValetPerk.MAGIC_WARD,
            ValetPerk.MAGIC_WEAKEN
    };
    private static final ValetCombatPerk[] SWORD_TREE_PERKS = {ValetCombatPerk.SWORD_STRENGTH, ValetCombatPerk.SWORD_RECOVERY, ValetCombatPerk.SWORD_DEFENSE, ValetCombatPerk.SWORD_REACH, ValetCombatPerk.SWORD_GUARDIAN};
    private static final ValetCombatPerk[] BOW_TREE_PERKS = {ValetCombatPerk.ALLY_AWARENESS, ValetCombatPerk.BOW_QUICK_SHOT, ValetCombatPerk.BOW_STRENGTH, ValetCombatPerk.BOW_RANGE, ValetCombatPerk.BOW_VOLLEY, ValetCombatPerk.BOW_RECYCLE_ARROW};

    private final List<OrderEntry> orderEntries = new ArrayList<>();
    private final ValetOrdersViewModel viewModel;
    private final List<ValetFarmArea> localFarmAreas;
    private final List<ValetAnimalArea> localAnimalAreas;
    private final List<ValetConstructionBlueprint> localConstructions;
    private EditBox nameField;
    private EditBox animalMaxField;
    private Button renameButton;
    private Button buildConstructionButton;
    private Button deleteConstructionButton;
    private Checkbox replantFarmCheckbox;
    private Checkbox tillFarmCheckbox;
    private Checkbox animalFeedCheckbox;
    private Checkbox animalBreedCheckbox;
    private Checkbox animalShearCheckbox;
    private Checkbox animalEggsCheckbox;
    private Checkbox animalMilkCheckbox;
    private Checkbox animalCullCheckbox;
    private Checkbox avoidNightReturnCheckbox;
    private Checkbox freeBehaviorCheckbox;
    private final List<Checkbox> farmCropCheckboxes = new ArrayList<>();
    private Button generalPageButton;
    private Button swordPageButton;
    private Button bowPageButton;
    private Button inventoryPageButton;
    private RightPage selectedRightPage = RightPage.GENERAL;
    private ValetRole localRole;
    private TargetCategory selectedCategory = TargetCategory.NONE;
    private int selectedMineTargetIndex = -1;
    private int selectedWoodTargetIndex = -1;
    private int selectedFarmAreaId = -1;
    private int selectedAnimalAreaId = -1;
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
    private int localFarmCropMask;
    private boolean localFarmReplant;
    private boolean localFarmTillSoil;
    private boolean localAnimalFeed;
    private boolean localAnimalBreed;
    private boolean localAnimalShear;
    private boolean localAnimalCollectEggs;
    private boolean localAnimalMilk;
    private boolean localAnimalCull;
    private int localMaxAnimals;
    private boolean localAvoidNightReturn;
    private boolean localFreeBehavior;
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
        localFarmAreas = new ArrayList<>(viewModel.farmAreas());
        localAnimalAreas = new ArrayList<>(viewModel.animalAreas());
        localConstructions = new ArrayList<>(viewModel.constructions());
        localRole = viewModel.role();
        selectedRightPage = defaultPageForRole();
        selectedPerk = defaultPerkForRole();
        ValetOrder currentOrder = viewModel.currentOrder();
        selectedCategory = currentOrder == ValetOrder.CHOP_WOOD ? TargetCategory.WOOD : currentOrder == ValetOrder.MINE_ORES ? TargetCategory.ORE : currentOrder == ValetOrder.HARVEST_CROPS ? TargetCategory.FARM : currentOrder == ValetOrder.BREED_ANIMALS ? TargetCategory.ANIMAL : currentOrder == ValetOrder.BUILD_STRUCTURE ? TargetCategory.CONSTRUCTION : currentOrder == ValetOrder.CRAFT ? TargetCategory.CRAFT : TargetCategory.NONE;
        selectedMineTargetIndex = viewModel.currentMineTargetIndex();
        selectedWoodTargetIndex = viewModel.currentWoodTargetIndex();
        selectedFarmAreaId = viewModel.currentFarmAreaId();
        localFarmCropMask = viewModel.currentFarmCropMask();
        localFarmReplant = viewModel.farmReplant();
        localFarmTillSoil = viewModel.farmTillSoil();
        selectedAnimalAreaId = viewModel.currentAnimalAreaId();
        localAnimalFeed = false;
        localAnimalBreed = viewModel.animalBreed();
        localAnimalShear = viewModel.animalShear();
        localAnimalCollectEggs = viewModel.animalCollectEggs();
        localAnimalMilk = viewModel.animalMilk();
        localAnimalCull = viewModel.animalCull();
        localMaxAnimals = viewModel.maxAnimals();
        localAvoidNightReturn = viewModel.avoidNightReturn();
        localFreeBehavior = viewModel.freeBehavior();
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
        int behaviorLeft = getLeftPanelLeft() + 8;
        int behaviorTop = getPanelTop() + PANEL_HEIGHT - 44;
        avoidNightReturnCheckbox = addRenderableWidget(Checkbox.builder(Component.translatable("screen.valet.avoid_night_return"), font)
                .pos(behaviorLeft, behaviorTop)
                .selected(localAvoidNightReturn)
                .maxWidth(120)
                .onValueChange((checkbox, selected) -> {
                    localAvoidNightReturn = selected;
                    sendBehaviorSettings();
                })
                .build());
        freeBehaviorCheckbox = addRenderableWidget(Checkbox.builder(Component.translatable("screen.valet.free_behavior"), font)
                .pos(behaviorLeft, behaviorTop + 20)
                .selected(localFreeBehavior)
                .maxWidth(120)
                .onValueChange((checkbox, selected) -> {
                    localFreeBehavior = selected;
                    sendBehaviorSettings();
                })
                .build());
        int farmOptionsLeft = rightLeft + 16;
        int farmOptionsTop = top + 170;
        replantFarmCheckbox = addRenderableWidget(Checkbox.builder(Component.translatable("screen.valet.replant_crop"), font)
                .pos(farmOptionsLeft, farmOptionsTop)
                .selected(localFarmReplant)
                .maxWidth(116)
                .onValueChange((checkbox, selected) -> {
                    localFarmReplant = selected;
                    if (selectedCategory == TargetCategory.FARM) {
                        sendFarmSelection(false);
                    }
                })
                .build());
        tillFarmCheckbox = addRenderableWidget(Checkbox.builder(Component.translatable("screen.valet.till_soil"), font)
                .pos(farmOptionsLeft + 128, farmOptionsTop)
                .selected(localFarmTillSoil)
                .maxWidth(118)
                .onValueChange((checkbox, selected) -> {
                    localFarmTillSoil = selected;
                    if (selectedCategory == TargetCategory.FARM) {
                        sendFarmSelection(false);
                    }
                })
                .build());
        farmCropCheckboxes.clear();
        int cropIndex = 0;
        for (ValetFarmCrop crop : ValetFarmCrop.values()) {
            int column = cropIndex % 2;
            int row = cropIndex / 2;
            Checkbox checkbox = addRenderableWidget(Checkbox.builder(Component.translatable(crop.getTranslationKey()), font)
                    .pos(farmOptionsLeft + column * 128, farmOptionsTop + 44 + row * 18)
                    .selected(crop.isEnabled(localFarmCropMask))
                    .maxWidth(118)
                    .onValueChange((box, selected) -> {
                        if (selected) {
                            localFarmCropMask |= crop.mask();
                        } else {
                            localFarmCropMask &= ~crop.mask();
                        }
                        if (selectedCategory == TargetCategory.FARM) {
                            sendFarmSelection(false);
                        }
                    })
                    .build());
            farmCropCheckboxes.add(checkbox);
            cropIndex++;
        }
        animalFeedCheckbox = addRenderableWidget(Checkbox.builder(Component.translatable("screen.valet.animal_feed"), font)
                .pos(farmOptionsLeft, farmOptionsTop)
                .selected(localAnimalFeed)
                .maxWidth(118)
                .onValueChange((checkbox, selected) -> {
                    localAnimalFeed = selected;
                    if (selectedCategory == TargetCategory.ANIMAL) {
                        sendBreedingSelection(false);
                    }
                })
                .build());
        animalBreedCheckbox = addRenderableWidget(Checkbox.builder(Component.translatable("screen.valet.animal_breed"), font)
                .pos(farmOptionsLeft, farmOptionsTop)
                .selected(localAnimalBreed)
                .maxWidth(118)
                .onValueChange((checkbox, selected) -> {
                    localAnimalBreed = selected;
                    if (selectedCategory == TargetCategory.ANIMAL) {
                        sendBreedingSelection(false);
                    }
                })
                .build());
        animalShearCheckbox = addRenderableWidget(Checkbox.builder(Component.translatable("screen.valet.animal_shear"), font)
                .pos(farmOptionsLeft + 128, farmOptionsTop)
                .selected(localAnimalShear)
                .maxWidth(118)
                .onValueChange((checkbox, selected) -> {
                    localAnimalShear = selected;
                    if (selectedCategory == TargetCategory.ANIMAL) {
                        sendBreedingSelection(false);
                    }
                })
                .build());
        animalEggsCheckbox = addRenderableWidget(Checkbox.builder(Component.translatable("screen.valet.animal_eggs"), font)
                .pos(farmOptionsLeft, farmOptionsTop + 22)
                .selected(localAnimalCollectEggs)
                .maxWidth(118)
                .onValueChange((checkbox, selected) -> {
                    localAnimalCollectEggs = selected;
                    if (selectedCategory == TargetCategory.ANIMAL) {
                        sendBreedingSelection(false);
                    }
                })
                .build());
        animalMilkCheckbox = addRenderableWidget(Checkbox.builder(Component.translatable("screen.valet.animal_milk"), font)
                .pos(farmOptionsLeft + 128, farmOptionsTop + 22)
                .selected(localAnimalMilk)
                .maxWidth(118)
                .onValueChange((checkbox, selected) -> {
                    localAnimalMilk = selected;
                    if (selectedCategory == TargetCategory.ANIMAL) {
                        sendBreedingSelection(false);
                    }
                })
                .build());
        animalCullCheckbox = addRenderableWidget(Checkbox.builder(Component.translatable("screen.valet.animal_cull"), font)
                .pos(farmOptionsLeft, farmOptionsTop + 44)
                .selected(localAnimalCull)
                .maxWidth(118)
                .onValueChange((checkbox, selected) -> {
                    localAnimalCull = selected;
                    if (selectedCategory == TargetCategory.ANIMAL) {
                        sendBreedingSelection(false);
                    }
                })
                .build());
        animalMaxField = addRenderableWidget(new EditBox(font, farmOptionsLeft + 128, farmOptionsTop + 47, 52, 16, Component.translatable("screen.valet.animal_max")));
        animalMaxField.setMaxLength(2);
        animalMaxField.setValue(Integer.toString(localMaxAnimals));
        generalPageButton = addRenderableWidget(Button.builder(Component.translatable("screen.valet.page_general"), button -> selectRightPage(RightPage.GENERAL))
                .bounds(rightLeft + 10, top + 132, 58, 18)
                .build());
        swordPageButton = addRenderableWidget(Button.builder(Component.translatable("screen.valet.page_sword"), button -> selectRightPage(localRole == ValetRole.FARMER || localRole == ValetRole.BREEDER ? RightPage.FARM_OPTIONS : RightPage.SWORD))
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
        updateFarmControls();
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
        } else if (selectedRightPage == RightPage.FARM_OPTIONS) {
            drawFarmOptions(context);
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
        Component roleLabel = Component.translatable(localRole.getTranslationKey());
        context.text(font, roleLabel, left + RIGHT_WIDTH - font.width(roleLabel) - 10, top + 9, 0xFF5A5142, false);
        context.text(font, getAvailableTitle(), left + 10, top + 45, 0xFF303030, false);
        context.text(font, getSelectedText(), left + 10, top + 60, 0xFF1F1F1F, false);
        context.text(font, getHintText(), left + 10, top + 75, 0xFF5A5142, false);
        drawTargetPreview(context, left + RIGHT_WIDTH - CONSTRUCTION_PREVIEW_SIZE - 10, top + 66);
        context.text(font, Component.translatable("screen.valet.level", localLevel), left + 10, top + 93, 0xFF202020, false);
        drawXpBar(context, left + 10, top + 108);

        Component pageTitle = getRightPageTitle();
        context.text(font, pageTitle, left + 10, top + TREE_TITLE_OFFSET, 0xFF303030, false);
        if (selectedRightPage == RightPage.GENERAL || selectedRightPage == RightPage.SWORD || selectedRightPage == RightPage.BOW) {
            int points = selectedRightPage == RightPage.GENERAL ? localPendingPerks : getCombatPendingPoints(selectedRightPage);
            context.text(font, Component.translatable("screen.valet.pending_points", points), left + RIGHT_WIDTH - 72, top + TREE_TITLE_OFFSET, points > 0 ? 0xFF8A5A00 : 0xFF606060, false);
        }
    }

    private Component getRightPageTitle() {
        return switch (selectedRightPage) {
            case GENERAL -> Component.translatable(localRole == ValetRole.FARMER ? "screen.valet.farmer_tree" : localRole == ValetRole.MAGICIAN ? "screen.valet.magic_tree" : "screen.valet.skill_tree");
            case FARM_OPTIONS -> Component.translatable("screen.valet.farm_options");
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
            CombatPerkNode recovery = getCombatNode(ValetCombatPerk.SWORD_RECOVERY);
            CombatPerkNode defense = getCombatNode(ValetCombatPerk.SWORD_DEFENSE);
            drawCombatConnection(context, strength, recovery);
            drawCombatConnection(context, strength, defense);
            drawCombatConnection(context, recovery, getCombatNode(ValetCombatPerk.SWORD_REACH));
            drawCombatConnection(context, defense, getCombatNode(ValetCombatPerk.SWORD_GUARDIAN));
        } else {
            CombatPerkNode awareness = getCombatNode(ValetCombatPerk.ALLY_AWARENESS);
            CombatPerkNode strength = getCombatNode(ValetCombatPerk.BOW_STRENGTH);
            CombatPerkNode quickShot = getCombatNode(ValetCombatPerk.BOW_QUICK_SHOT);
            drawCombatConnection(context, awareness, getCombatNode(ValetCombatPerk.BOW_QUICK_SHOT));
            drawCombatConnection(context, awareness, strength);
            drawCombatConnection(context, strength, getCombatNode(ValetCombatPerk.BOW_RANGE));
            drawCombatConnection(context, strength, getCombatNode(ValetCombatPerk.BOW_RECYCLE_ARROW));
            drawCombatConnection(context, quickShot, getCombatNode(ValetCombatPerk.BOW_VOLLEY));
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
        if (localRole == ValetRole.FARMER) {
            PerkNode hands = getNode(ValetPerk.FARM_HANDS);
            PerkNode range = getNode(ValetPerk.FARM_RANGE);
            PerkNode replanting = getNode(ValetPerk.FARM_REPLANTING);
            drawConnection(context, hands, range);
            drawConnection(context, hands, replanting);
            drawConnection(context, hands, getNode(ValetPerk.FARM_TILLING));
            drawConnection(context, replanting, getNode(ValetPerk.FARM_STORAGE));
            drawConnection(context, range, getNode(ValetPerk.FARM_STEWARD));
        } else if (localRole == ValetRole.MAGICIAN) {
            PerkNode ice = getNode(ValetPerk.MAGIC_ICE);
            PerkNode fangs = getNode(ValetPerk.MAGIC_FANGS);
            PerkNode heal = getNode(ValetPerk.MAGIC_HEAL);
            PerkNode ward = getNode(ValetPerk.MAGIC_WARD);
            drawConnection(context, ice, fangs);
            drawConnection(context, fangs, getNode(ValetPerk.MAGIC_SHATTER));
            drawConnection(context, ice, heal);
            drawConnection(context, heal, getNode(ValetPerk.MAGIC_REGEN_AURA));
            drawConnection(context, ice, ward);
            drawConnection(context, ward, getNode(ValetPerk.MAGIC_WEAKEN));
            context.centeredText(font, Component.translatable("screen.valet.magic_branch_destruction"), fangs.centerX(), fangs.top - 12, 0xFF5A5142);
            context.centeredText(font, Component.translatable("screen.valet.magic_branch_healing"), heal.centerX(), heal.top - 12, 0xFF5A5142);
            context.centeredText(font, Component.translatable("screen.valet.magic_branch_alteration"), ward.centerX(), ward.top - 12, 0xFF5A5142);
        } else {
            PerkNode speed = getNode(ValetPerk.SPEED);
            PerkNode vision = getNode(ValetPerk.VISION);
            PerkNode movement = getNode(ValetPerk.MOVEMENT);
            PerkNode storage = getNode(ValetPerk.STORAGE);
            drawConnection(context, speed, vision);
            drawConnection(context, speed, movement);
            drawConnection(context, speed, storage);
            drawConnection(context, movement, getNode(ValetPerk.PATHING));
            drawConnection(context, vision, getNode(ValetPerk.VEIN));
            drawConnection(context, storage, getNode(ValetPerk.HAUL));
            drawConnection(context, getNode(ValetPerk.PATHING), getNode(ValetPerk.LIGHTING));
        }

        for (ValetPerk perk : getPerkTreePerks()) {
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
        if (perk.getRole() != localRole) {
            perk = defaultPerkForRole();
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

    private void drawFarmOptions(GuiGraphicsExtractor context) {
        int left = getRightPanelLeft();
        int top = getPanelTop();
        if (localRole == ValetRole.BREEDER) {
            context.text(font, Component.translatable("screen.valet.animal_options"), left + 16, top + 204, 0xFF303030, false);
            context.text(font, Component.translatable("screen.valet.animal_max"), left + 184, top + 219, 0xFF303030, false);
        } else {
            context.text(font, Component.translatable("screen.valet.farm_crops"), left + 16, top + 204, 0xFF303030, false);
        }
    }

    private void rebuildOrderEntries() {
        if (!isCategoryAllowed(selectedCategory)) {
            selectedCategory = defaultCategoryForRole();
        }

        orderEntries.clear();
        orderEntries.add(OrderEntry.target(ValetOrder.NONE, -1, Component.translatable("order.valet.none")));

        if (localRole == ValetRole.ARTISAN) {
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
        }

        if (localRole == ValetRole.FARMER) {
            orderEntries.add(OrderEntry.category(TargetCategory.FARM, Component.translatable("screen.valet.category_farm")));
            orderEntries.add(OrderEntry.target(ValetOrder.HARVEST_CROPS, -1, Component.literal("  ").append(Component.translatable("screen.valet.farm_all"))));
            for (ValetFarmArea area : localFarmAreas) {
                orderEntries.add(OrderEntry.target(ValetOrder.HARVEST_CROPS, area.id(), Component.literal("  ").append(Component.translatable("screen.valet.farm_area_count", area.name(), area.blockCount()))));
            }
        }
        if (localRole == ValetRole.BREEDER) {
            orderEntries.add(OrderEntry.category(TargetCategory.ANIMAL, Component.translatable("screen.valet.category_animals")));
            orderEntries.add(OrderEntry.target(ValetOrder.BREED_ANIMALS, -1, Component.literal("  ").append(Component.translatable("screen.valet.animal_all"))));
            for (ValetAnimalArea area : localAnimalAreas) {
                Component typeLabel = getAnimalTypeLabel(area);
                orderEntries.add(OrderEntry.target(ValetOrder.BREED_ANIMALS, area.id(), Component.literal("  ").append(Component.translatable("screen.valet.animal_area_count", area.name(), typeLabel))));
            }
        }
        clampOrderScroll();
        updateConstructionButtons();
        updateFarmControls();
    }

    private TargetCategory defaultCategoryForRole() {
        return localRole == ValetRole.FARMER ? TargetCategory.FARM : localRole == ValetRole.BREEDER ? TargetCategory.ANIMAL : TargetCategory.NONE;
    }

    private boolean isCategoryAllowed(TargetCategory category) {
        return switch (localRole) {
            case ARTISAN -> category == TargetCategory.NONE
                    || category == TargetCategory.ORE
                    || category == TargetCategory.WOOD
                    || category == TargetCategory.CONSTRUCTION
                    || category == TargetCategory.CRAFT;
            case FARMER -> category == TargetCategory.NONE || category == TargetCategory.FARM;
            case BREEDER -> category == TargetCategory.NONE || category == TargetCategory.ANIMAL;
            case COMBATANT, MAGICIAN, COOK -> category == TargetCategory.NONE;
        };
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
        if (entry.order == ValetOrder.HARVEST_CROPS) {
            return selectedCategory == TargetCategory.FARM && selectedFarmAreaId == entry.targetIndex;
        }
        if (entry.order == ValetOrder.BREED_ANIMALS) {
            return selectedCategory == TargetCategory.ANIMAL && selectedAnimalAreaId == entry.targetIndex;
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
            case FARM -> Component.translatable("screen.valet.available_farm");
            case ANIMAL -> Component.translatable("screen.valet.available_animals");
            case CONSTRUCTION -> Component.translatable("screen.valet.available_constructions");
            case CRAFT -> Component.translatable("screen.valet.available_craft");
            case NONE -> Component.translatable("screen.valet.available_targets");
        };
    }

    private Component getCategoryText(TargetCategory category) {
        return switch (category) {
            case ORE -> Component.translatable("screen.valet.category_ores");
            case WOOD -> Component.translatable("screen.valet.category_wood");
            case FARM -> Component.translatable("screen.valet.category_farm");
            case ANIMAL -> Component.translatable("screen.valet.category_animals");
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
        if (selectedCategory == TargetCategory.FARM) {
            ValetFarmArea area = getSelectedFarmArea();
            return area == null ? Component.translatable("screen.valet.farm_all") : Component.literal(area.name());
        }
        if (selectedCategory == TargetCategory.ANIMAL) {
            ValetAnimalArea area = getSelectedAnimalArea();
            return area == null ? Component.translatable("screen.valet.animal_all") : Component.literal(area.name());
        }
        if (selectedCategory == TargetCategory.CONSTRUCTION) {
            ValetConstructionBlueprint construction = getSelectedConstruction();
            return construction == null ? getCategoryText(TargetCategory.CONSTRUCTION) : Component.literal(construction.name());
        }
        if (selectedCategory == TargetCategory.CRAFT) {
            ValetCraftTarget target = ValetCraftTarget.fromIndex(selectedCraftTargetIndex);
            return target == null ? getCategoryText(TargetCategory.CRAFT) : Component.translatable(target.getTranslationKey());
        }
        return Component.translatable(localRole == ValetRole.COOK ? "order.valet.cook" : "order.valet.none");
    }

    private Component getHintText() {
        if (selectedCategory == TargetCategory.NONE && localRole == ValetRole.MAGICIAN) {
            return Component.translatable("screen.valet.magic_hint");
        }
        if (selectedCategory == TargetCategory.NONE && localRole == ValetRole.COOK) {
            return Component.translatable("screen.valet.cook_hint");
        }
        if (selectedCategory == TargetCategory.CONSTRUCTION) {
            return localConstructions.isEmpty() ? Component.translatable("screen.valet.no_constructions") : Component.translatable("screen.valet.build_hint");
        }
        if (selectedCategory == TargetCategory.CRAFT) {
            return Component.translatable("screen.valet.craft_hint");
        }
        if (selectedCategory == TargetCategory.FARM) {
            return localFarmAreas.isEmpty() ? Component.translatable("screen.valet.farm_hint") : Component.translatable("screen.valet.farm_beacon_hint");
        }
        if (selectedCategory == TargetCategory.ANIMAL) {
            return localAnimalAreas.isEmpty() ? Component.translatable("screen.valet.animal_hint") : Component.translatable("screen.valet.animal_beacon_hint");
        }
        return Component.translatable("screen.valet.mine_hint");
    }

    private ValetFarmArea getSelectedFarmArea() {
        for (ValetFarmArea area : localFarmAreas) {
            if (area.id() == selectedFarmAreaId) {
                return area;
            }
        }
        return null;
    }

    private ValetAnimalArea getSelectedAnimalArea() {
        for (ValetAnimalArea area : localAnimalAreas) {
            if (area.id() == selectedAnimalAreaId) {
                return area;
            }
        }
        return null;
    }

    private Component getAnimalTypeLabel(ValetAnimalArea area) {
        ValetAnimalType type = area.animalType();
        return type == null ? Component.translatable("screen.valet.animal_mixed") : Component.translatable(type.getTranslationKey());
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
            case STORAGE -> new PerkNode(perk, left + TREE_CENTER_X, top + TREE_MIDDLE_ROW);
            case PATHING -> new PerkNode(perk, left + TREE_RIGHT_X, top + TREE_TOP_ROW);
            case VEIN -> new PerkNode(perk, left + TREE_LEFT_X, top + TREE_TOP_ROW);
            case HAUL -> new PerkNode(perk, left + TREE_FAR_LEFT_X, top + TREE_TOP_ROW);
            case LIGHTING -> new PerkNode(perk, left + TREE_FAR_RIGHT_X, top + TREE_TOP_ROW);
            case FARM_HANDS -> new PerkNode(perk, left + TREE_CENTER_X, top + TREE_BOTTOM_ROW);
            case FARM_RANGE -> new PerkNode(perk, left + TREE_LEFT_X, top + TREE_MIDDLE_ROW);
            case FARM_REPLANTING -> new PerkNode(perk, left + TREE_CENTER_X, top + TREE_MIDDLE_ROW);
            case FARM_TILLING -> new PerkNode(perk, left + TREE_RIGHT_X, top + TREE_MIDDLE_ROW);
            case FARM_STORAGE -> new PerkNode(perk, left + TREE_LEFT_X, top + TREE_TOP_ROW);
            case FARM_STEWARD -> new PerkNode(perk, left + TREE_RIGHT_X, top + TREE_TOP_ROW);
            case MAGIC_ICE -> new PerkNode(perk, left + TREE_CENTER_X, top + TREE_BOTTOM_ROW);
            case MAGIC_FANGS -> new PerkNode(perk, left + TREE_LEFT_X, top + TREE_MIDDLE_ROW);
            case MAGIC_SHATTER -> new PerkNode(perk, left + TREE_FAR_LEFT_X, top + TREE_TOP_ROW);
            case MAGIC_HEAL -> new PerkNode(perk, left + TREE_CENTER_X, top + TREE_MIDDLE_ROW);
            case MAGIC_REGEN_AURA -> new PerkNode(perk, left + TREE_CENTER_X, top + TREE_TOP_ROW);
            case MAGIC_WARD -> new PerkNode(perk, left + TREE_RIGHT_X, top + TREE_MIDDLE_ROW);
            case MAGIC_WEAKEN -> new PerkNode(perk, left + TREE_FAR_RIGHT_X, top + TREE_TOP_ROW);
        };
    }

    private CombatPerkNode getCombatNode(ValetCombatPerk perk) {
        int left = getRightPanelLeft();
        int top = getPanelTop();
        return switch (perk) {
            case SWORD_STRENGTH -> new CombatPerkNode(perk, left + TREE_CENTER_X, top + TREE_BOTTOM_ROW);
            case SWORD_RECOVERY -> new CombatPerkNode(perk, left + TREE_LEFT_X, top + TREE_MIDDLE_ROW);
            case SWORD_DEFENSE -> new CombatPerkNode(perk, left + TREE_RIGHT_X, top + TREE_MIDDLE_ROW);
            case SWORD_REACH -> new CombatPerkNode(perk, left + TREE_LEFT_X, top + TREE_TOP_ROW);
            case SWORD_GUARDIAN -> new CombatPerkNode(perk, left + TREE_RIGHT_X, top + TREE_TOP_ROW);
            case ALLY_AWARENESS -> new CombatPerkNode(perk, left + TREE_CENTER_X, top + TREE_BOTTOM_ROW);
            case BOW_QUICK_SHOT -> new CombatPerkNode(perk, left + TREE_LEFT_X, top + TREE_MIDDLE_ROW);
            case BOW_STRENGTH -> new CombatPerkNode(perk, left + TREE_RIGHT_X, top + TREE_MIDDLE_ROW);
            case BOW_RANGE -> new CombatPerkNode(perk, left + TREE_RIGHT_X, top + TREE_TOP_ROW);
            case BOW_VOLLEY -> new CombatPerkNode(perk, left + TREE_LEFT_X, top + TREE_TOP_ROW);
            case BOW_RECYCLE_ARROW -> new CombatPerkNode(perk, left + TREE_FAR_RIGHT_X, top + TREE_TOP_ROW);
        };
    }

    private ValetPerk[] getPerkTreePerks() {
        return switch (localRole) {
            case FARMER -> FARMER_TREE_PERKS;
            case MAGICIAN -> MAGIC_TREE_PERKS;
            case ARTISAN, BREEDER, COMBATANT, COOK -> ARTISAN_TREE_PERKS;
        };
    }

    private ValetPerk getHoveredPerk(int mouseX, int mouseY) {
        for (ValetPerk perk : getPerkTreePerks()) {
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
            case GENERAL, FARM_OPTIONS, INVENTORY -> false;
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
        if (perk.getRole() != localRole) {
            return false;
        }
        return switch (perk) {
            case SPEED -> true;
            case VISION, MOVEMENT -> hasLocalPerk(ValetPerk.SPEED);
            case STORAGE -> hasLocalPerk(ValetPerk.SPEED);
            case PATHING -> hasLocalPerk(ValetPerk.MOVEMENT);
            case VEIN -> hasLocalPerk(ValetPerk.VISION);
            case HAUL -> hasLocalPerk(ValetPerk.STORAGE);
            case LIGHTING -> hasLocalPerk(ValetPerk.PATHING);
            case FARM_HANDS -> true;
            case FARM_RANGE, FARM_REPLANTING, FARM_TILLING -> hasLocalPerk(ValetPerk.FARM_HANDS);
            case FARM_STORAGE -> hasLocalPerk(ValetPerk.FARM_REPLANTING);
            case FARM_STEWARD -> hasLocalPerk(ValetPerk.FARM_RANGE) && hasLocalPerk(ValetPerk.FARM_STORAGE);
            case MAGIC_ICE -> true;
            case MAGIC_FANGS, MAGIC_HEAL, MAGIC_WARD -> true;
            case MAGIC_SHATTER -> hasLocalPerk(ValetPerk.MAGIC_FANGS);
            case MAGIC_REGEN_AURA -> hasLocalPerk(ValetPerk.MAGIC_HEAL);
            case MAGIC_WEAKEN -> hasLocalPerk(ValetPerk.MAGIC_WARD);
        };
    }

    private boolean canLearnCombatPerk(ValetCombatPerk perk) {
        return switch (perk) {
            case SWORD_STRENGTH, ALLY_AWARENESS -> true;
            case SWORD_RECOVERY, SWORD_DEFENSE -> hasLocalCombatPerk(ValetCombatPerk.SWORD_STRENGTH);
            case SWORD_REACH -> hasLocalCombatPerk(ValetCombatPerk.SWORD_RECOVERY);
            case SWORD_GUARDIAN -> hasLocalCombatPerk(ValetCombatPerk.SWORD_DEFENSE);
            case BOW_QUICK_SHOT, BOW_STRENGTH -> hasLocalCombatPerk(ValetCombatPerk.ALLY_AWARENESS);
            case BOW_RANGE -> hasLocalCombatPerk(ValetCombatPerk.BOW_STRENGTH);
            case BOW_VOLLEY -> hasLocalCombatPerk(ValetCombatPerk.BOW_QUICK_SHOT);
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
            case FARM_OPTIONS, INVENTORY -> 0;
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
        } else if (selectedRightPage == RightPage.SWORD || selectedRightPage == RightPage.BOW) {
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

    public void applyServerState(int roleIndex, int orderIndex, int mineTargetIndex, int woodTargetIndex, int farmAreaId, int farmCropMask, boolean farmReplant, boolean farmTillSoil, int animalAreaId, boolean animalFeed, boolean animalBreed, boolean animalShear, boolean animalCollectEggs, boolean animalMilk, boolean animalCull, int maxAnimals, boolean avoidNightReturn, boolean freeBehavior, int constructionTargetId, int craftTargetIndex, int[] oreCounts, int[] woodCounts, List<ItemStack> inventoryStacks, int level, int xp, int nextLevelXp, int pendingPerks, boolean[] perks, boolean[] combatPerks, int swordLevel, int swordXp, int swordNextLevelXp, int swordPendingPerks, int bowLevel, int bowXp, int bowNextLevelXp, int bowPendingPerks, boolean allyAwareness, String valetName) {
        localRole = ValetRole.fromIndex(roleIndex);
        ensurePageForRole();
        if (selectedPerk.getRole() != localRole) {
            selectedPerk = defaultPerkForRole();
        }
        ValetOrder order = ValetOrder.fromIndex(orderIndex);
        selectedCategory = order == ValetOrder.CHOP_WOOD ? TargetCategory.WOOD : order == ValetOrder.MINE_ORES ? TargetCategory.ORE : order == ValetOrder.HARVEST_CROPS ? TargetCategory.FARM : order == ValetOrder.BREED_ANIMALS ? TargetCategory.ANIMAL : order == ValetOrder.BUILD_STRUCTURE ? TargetCategory.CONSTRUCTION : order == ValetOrder.CRAFT ? TargetCategory.CRAFT : TargetCategory.NONE;
        selectedMineTargetIndex = mineTargetIndex;
        selectedWoodTargetIndex = woodTargetIndex;
        selectedFarmAreaId = farmAreaId;
        localFarmCropMask = farmCropMask;
        localFarmReplant = farmReplant;
        localFarmTillSoil = farmTillSoil;
        selectedAnimalAreaId = animalAreaId;
        localAnimalFeed = false;
        localAnimalBreed = animalBreed;
        localAnimalShear = animalShear;
        localAnimalCollectEggs = animalCollectEggs;
        localAnimalMilk = animalMilk;
        localAnimalCull = animalCull;
        localMaxAnimals = maxAnimals;
        localAvoidNightReturn = avoidNightReturn;
        localFreeBehavior = freeBehavior;
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
        if (animalMaxField != null) {
            animalMaxField.setValue(Integer.toString(localMaxAnimals));
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
        if (animalMaxField != null && animalMaxField.isFocused()) {
            if (keyCode == 257 || keyCode == 335) {
                readAnimalMaxField();
                if (selectedCategory == TargetCategory.ANIMAL) {
                    sendBreedingSelection(false);
                }
                animalMaxField.setFocused(false);
                return true;
            }
            if (keyCode == 256) {
                animalMaxField.setFocused(false);
                return true;
            }
            animalMaxField.keyPressed(event);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (nameField != null && nameField.isFocused()) {
            return nameField.charTyped(event);
        }
        if (animalMaxField != null && animalMaxField.isFocused()) {
            int codepoint = event.codepoint();
            if (codepoint >= '0' && codepoint <= '9') {
                return animalMaxField.charTyped(event);
            }
            return true;
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
            selectedFarmAreaId = -1;
            selectedAnimalAreaId = -1;
            selectedConstructionTargetId = -1;
            selectedCraftTargetIndex = -1;
        } else if (order == ValetOrder.MINE_ORES) {
            selectedCategory = TargetCategory.ORE;
            selectedMineTargetIndex = targetIndex;
        } else if (order == ValetOrder.CHOP_WOOD) {
            selectedCategory = TargetCategory.WOOD;
            selectedWoodTargetIndex = targetIndex;
        } else if (order == ValetOrder.HARVEST_CROPS) {
            selectedCategory = TargetCategory.FARM;
            selectedFarmAreaId = targetIndex;
        } else if (order == ValetOrder.BREED_ANIMALS) {
            selectedCategory = TargetCategory.ANIMAL;
            selectedAnimalAreaId = targetIndex;
        } else if (order == ValetOrder.BUILD_STRUCTURE) {
            selectedCategory = TargetCategory.CONSTRUCTION;
            selectedConstructionTargetId = targetIndex;
        } else if (order == ValetOrder.CRAFT) {
            selectedCategory = TargetCategory.CRAFT;
            selectedCraftTargetIndex = targetIndex;
        }
        rebuildOrderEntries();

        if (order == ValetOrder.HARVEST_CROPS) {
            sendFarmSelection(true);
            return;
        }

        if (order == ValetOrder.BREED_ANIMALS) {
            sendBreedingSelection(true);
            return;
        }

        ClientPlayNetworking.send(new SetOrderPayload(viewModel.valetEntityId(), order, targetIndex));
    }

    private void sendFarmSelection(boolean closeScreen) {
        ClientPlayNetworking.send(new SetFarmOrderPayload(viewModel.valetEntityId(), selectedFarmAreaId, localFarmCropMask, localFarmReplant, localFarmTillSoil, closeScreen));
    }

    private void sendBreedingSelection(boolean closeScreen) {
        readAnimalMaxField();
        ClientPlayNetworking.send(new SetBreedingOrderPayload(viewModel.valetEntityId(), selectedAnimalAreaId, false, localAnimalBreed, localAnimalShear, localAnimalCollectEggs, localAnimalMilk, localAnimalCull, localMaxAnimals, closeScreen));
    }

    private void readAnimalMaxField() {
        if (animalMaxField == null) {
            return;
        }
        try {
            localMaxAnimals = Math.max(2, Math.min(64, Integer.parseInt(animalMaxField.getValue())));
        } catch (NumberFormatException ignored) {
            localMaxAnimals = com.wawane.valet.order.ValetOrders.DEFAULT_MAX_ANIMALS;
        }
        animalMaxField.setValue(Integer.toString(localMaxAnimals));
    }

    private void sendBehaviorSettings() {
        ClientPlayNetworking.send(new SetBehaviorPayload(viewModel.valetEntityId(), localFreeBehavior, localAvoidNightReturn));
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
        if (!isPageAllowed(page)) {
            page = defaultPageForRole();
        }
        selectedRightPage = page;
        if ((page == RightPage.SWORD || page == RightPage.BOW) && !combatPerkBelongsToPage(selectedCombatPerk, page)) {
            selectedCombatPerk = getDefaultCombatPerk(page);
        }
        updatePageButtons();
        updateConstructionButtons();
        updateFarmControls();
    }

    private void ensurePageForRole() {
        if (!isPageAllowed(selectedRightPage)) {
            selectedRightPage = defaultPageForRole();
        }
    }

    private RightPage defaultPageForRole() {
        return switch (localRole) {
            case COMBATANT -> RightPage.SWORD;
            case BREEDER -> RightPage.FARM_OPTIONS;
            case MAGICIAN -> RightPage.GENERAL;
            case COOK -> RightPage.INVENTORY;
            case ARTISAN, FARMER -> RightPage.GENERAL;
        };
    }

    private ValetPerk defaultPerkForRole() {
        return switch (localRole) {
            case FARMER -> ValetPerk.FARM_HANDS;
            case MAGICIAN -> ValetPerk.MAGIC_ICE;
            case ARTISAN, BREEDER, COMBATANT, COOK -> ValetPerk.SPEED;
        };
    }

    private boolean isPageAllowed(RightPage page) {
        return switch (localRole) {
            case ARTISAN -> page == RightPage.GENERAL || page == RightPage.INVENTORY;
            case FARMER -> page == RightPage.GENERAL || page == RightPage.FARM_OPTIONS || page == RightPage.INVENTORY;
            case BREEDER -> page == RightPage.FARM_OPTIONS || page == RightPage.INVENTORY;
            case COMBATANT -> page == RightPage.SWORD || page == RightPage.BOW || page == RightPage.INVENTORY;
            case MAGICIAN -> page == RightPage.GENERAL || page == RightPage.INVENTORY;
            case COOK -> page == RightPage.INVENTORY;
        };
    }

    private void updatePageButtons() {
        String generalPageKey = switch (localRole) {
            case FARMER -> "screen.valet.page_farmer";
            case BREEDER -> "screen.valet.page_breeder";
            case MAGICIAN -> "screen.valet.page_magic";
            case COOK -> "screen.valet.page_cook";
            case ARTISAN, COMBATANT -> "screen.valet.page_artisan";
        };
        updatePageButton(generalPageButton, RightPage.GENERAL, generalPageKey, localRole == ValetRole.ARTISAN || localRole == ValetRole.FARMER || localRole == ValetRole.MAGICIAN);
        updatePageButton(swordPageButton, localRole == ValetRole.FARMER || localRole == ValetRole.BREEDER ? RightPage.FARM_OPTIONS : RightPage.SWORD, localRole == ValetRole.BREEDER ? "screen.valet.page_animal_options" : localRole == ValetRole.FARMER ? "screen.valet.page_farm_options" : "screen.valet.page_sword", localRole == ValetRole.FARMER || localRole == ValetRole.BREEDER || localRole == ValetRole.COMBATANT);
        updatePageButton(bowPageButton, RightPage.BOW, "screen.valet.page_bow", localRole == ValetRole.COMBATANT);
        updatePageButton(inventoryPageButton, RightPage.INVENTORY, "screen.valet.page_inventory", true);
    }

    private void updatePageButton(Button button, RightPage page, String translationKey, boolean visible) {
        if (button == null) {
            return;
        }

        button.visible = visible;
        button.active = visible && selectedRightPage != page;
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

    private void updateFarmControls() {
        if (replantFarmCheckbox == null) {
            return;
        }

        boolean farmVisible = selectedRightPage == RightPage.FARM_OPTIONS && selectedCategory == TargetCategory.FARM;
        replantFarmCheckbox.visible = farmVisible;
        replantFarmCheckbox.active = farmVisible;
        if (tillFarmCheckbox != null) {
            tillFarmCheckbox.visible = farmVisible;
            tillFarmCheckbox.active = farmVisible;
        }
        for (Checkbox checkbox : farmCropCheckboxes) {
            checkbox.visible = farmVisible;
            checkbox.active = farmVisible;
        }

        boolean animalVisible = selectedRightPage == RightPage.FARM_OPTIONS && selectedCategory == TargetCategory.ANIMAL;
        if (animalFeedCheckbox != null) {
            animalFeedCheckbox.visible = false;
            animalFeedCheckbox.active = false;
        }
        if (animalBreedCheckbox != null) {
            animalBreedCheckbox.visible = animalVisible;
            animalBreedCheckbox.active = animalVisible;
        }
        if (animalShearCheckbox != null) {
            animalShearCheckbox.visible = animalVisible;
            animalShearCheckbox.active = animalVisible;
        }
        if (animalEggsCheckbox != null) {
            animalEggsCheckbox.visible = animalVisible;
            animalEggsCheckbox.active = animalVisible;
        }
        if (animalMilkCheckbox != null) {
            animalMilkCheckbox.visible = animalVisible;
            animalMilkCheckbox.active = animalVisible;
        }
        if (animalCullCheckbox != null) {
            animalCullCheckbox.visible = animalVisible;
            animalCullCheckbox.active = animalVisible;
        }
        if (animalMaxField != null) {
            animalMaxField.visible = animalVisible;
            animalMaxField.setEditable(animalVisible);
        }
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
        FARM_OPTIONS,
        SWORD,
        BOW,
        INVENTORY
    }

    private enum TargetCategory {
        NONE,
        ORE,
        WOOD,
        FARM,
        ANIMAL,
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
