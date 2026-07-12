package com.wawane.valet.client;

import com.wawane.valet.ValetMod;
import com.wawane.valet.group.ValetGroupViewData;
import com.wawane.valet.network.packets.ManageMapGroupPayload;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;

/** Carte tactique client. Elle n'affiche que les chunks deja recus par le client. */
public final class ValetWorldMapScreen extends Screen {
    private static final int LEGEND_WIDTH = 176;
    private static final int MAP_MARGIN = 12;
    private static final int MAP_TOP = 58;
    private static final int MAP_BOTTOM_MARGIN = 28;
    private static final int CELL_SIZE = 6;
    private static final int[] ZOOM_LEVELS = {1, 2, 4, 8, 16};
    private static final int UNKNOWN_COLOR = 0xFF11181B;
    private static final int PLAYER_COLOR = 0xFFFFD34E;
    private static final int VALET_COLOR = 0xFF55D6D0;
    private static final int SELECTED_GROUP_COLOR = 0xFF76E36D;
    private static final int OTHER_GROUP_COLOR = 0xFFFFA94D;
    private static final int WAYPOINT_COLOR = 0xFFFF5B5B;

    private static BlockPos waypoint;
    private static ResourceKey<Level> waypointDimension;

    private final Screen parent;
    private double centerX;
    private double centerZ;
    private int zoomIndex = 2;
    private int mapLeft;
    private int mapTop;
    private int mapWidth;
    private int mapHeight;
    private int columns;
    private int rows;
    private int[] terrainColors = new int[0];
    private boolean dragging;
    private final List<ValetGroupViewData.GroupEntry> groups = new ArrayList<>();
    private final List<ValetGroupViewData.ValetEntry> valets = new ArrayList<>();
    private int selectedGroupId = -1;
    private Button sendGroupButton;
    private Button recallGroupButton;

    public ValetWorldMapScreen(Screen parent) {
        super(Component.translatable("screen.valet_map.title"));
        this.parent = parent;
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            centerX = client.player.getX();
            centerZ = client.player.getZ();
        }
    }

    @Override
    protected void init() {
        mapLeft = MAP_MARGIN;
        mapTop = MAP_TOP;
        mapWidth = Math.max(120, width - LEGEND_WIDTH - MAP_MARGIN * 3);
        mapHeight = Math.max(90, height - MAP_TOP - MAP_BOTTOM_MARGIN);

        int panelLeft = mapLeft + mapWidth + MAP_MARGIN;
        int buttonWidth = Math.max(80, width - panelLeft - MAP_MARGIN);
        int halfButtonWidth = (buttonWidth - 4) / 2;
        int tabWidth = Math.min(150, Math.max(100, (width - MAP_MARGIN * 2 - 4) / 2));
        Button mapTab = addRenderableWidget(Button.builder(Component.translatable("screen.valet_map.tab"), ignored -> {})
                .bounds(MAP_MARGIN, 8, tabWidth, 22).build());
        mapTab.active = false;
        addRenderableWidget(Button.builder(Component.translatable("screen.valet_groups.tab"), ignored -> minecraft.setScreenAndShow(new ValetGroupsScreen(this)))
                .bounds(MAP_MARGIN + tabWidth + 4, 8, tabWidth, 22).build());
        addRenderableWidget(Button.builder(Component.literal("<"), ignored -> cycleGroup(-1))
                .bounds(panelLeft, height - 122, halfButtonWidth, 18)
                .build());
        addRenderableWidget(Button.builder(Component.literal(">"), ignored -> cycleGroup(1))
                .bounds(panelLeft + halfButtonWidth + 4, height - 122, buttonWidth - halfButtonWidth - 4, 18)
                .build());
        sendGroupButton = addRenderableWidget(Button.builder(Component.translatable("screen.valet_map.send_group"), ignored -> sendSelectedGroup())
                .bounds(panelLeft, height - 100, buttonWidth, 18)
                .build());
        recallGroupButton = addRenderableWidget(Button.builder(Component.translatable("screen.valet_map.recall_group"), ignored -> sendMapAction(ManageMapGroupPayload.Action.RECALL, null, BlockPos.ZERO))
                .bounds(panelLeft, height - 78, buttonWidth, 18)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("screen.valet_map.center"), ignored -> centerOnPlayer())
                .bounds(panelLeft, height - 56, buttonWidth, 18)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("screen.valet_map.clear_waypoint"), ignored -> {
                    clearWaypoint();
                    updateGroupButtons();
                })
                .bounds(panelLeft, height - 34, buttonWidth, 18)
                .build());
        rebuildTerrain();
        updateGroupButtons();
        sendMapAction(ManageMapGroupPayload.Action.REQUEST, null, BlockPos.ZERO);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, width, height, 0xF20A0E12);
        graphics.text(font, title, MAP_MARGIN, 40, 0xFFFFFFFF, false);
        drawHeaderInfo(graphics, mouseX, mouseY);
        drawTerrain(graphics);
        drawMarkers(graphics);
        drawLegend(graphics, mouseX, mouseY);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    private void drawTerrain(GuiGraphicsExtractor graphics) {
        graphics.fill(mapLeft - 2, mapTop - 2, mapLeft + mapWidth + 2, mapTop + mapHeight + 2, 0xFF5E6B70);
        graphics.enableScissor(mapLeft, mapTop, mapLeft + mapWidth, mapTop + mapHeight);
        for (int row = 0; row < rows; row++) {
            int y = mapTop + row * CELL_SIZE;
            for (int column = 0; column < columns; column++) {
                int x = mapLeft + column * CELL_SIZE;
                graphics.fill(x, y, x + CELL_SIZE, y + CELL_SIZE, terrainColors[row * columns + column]);
            }
        }
        drawGrid(graphics);
        graphics.disableScissor();
    }

    private void drawGrid(GuiGraphicsExtractor graphics) {
        int blocksPerCell = blocksPerCell();
        int gridBlocks = blocksPerCell <= 2 ? 32 : blocksPerCell <= 8 ? 64 : 128;
        int minWorldX = sampleWorldX(0);
        int minWorldZ = sampleWorldZ(0);
        int firstX = Math.floorDiv(minWorldX, gridBlocks) * gridBlocks;
        int firstZ = Math.floorDiv(minWorldZ, gridBlocks) * gridBlocks;
        for (int worldX = firstX; worldX <= sampleWorldX(columns); worldX += gridBlocks) {
            int x = worldToScreenX(worldX);
            graphics.verticalLine(x, mapTop, mapTop + mapHeight, 0x303B464B);
        }
        for (int worldZ = firstZ; worldZ <= sampleWorldZ(rows); worldZ += gridBlocks) {
            int y = worldToScreenY(worldZ);
            graphics.horizontalLine(mapLeft, mapLeft + mapWidth, y, 0x303B464B);
        }
    }

    private void drawMarkers(GuiGraphicsExtractor graphics) {
        Minecraft client = Minecraft.getInstance();
        ClientLevel world = client.level;
        if (world == null || client.player == null) {
            return;
        }
        graphics.enableScissor(mapLeft, mapTop, mapLeft + mapWidth, mapTop + mapHeight);
        for (Entity entity : world.entitiesForRendering()) {
            if (entity instanceof Villager villager && ValetMod.isValet(villager)) {
                int groupId = groupIdFor(villager.getUUID());
                int color = groupId == selectedGroupId ? SELECTED_GROUP_COLOR : groupId > 0 ? OTHER_GROUP_COLOR : VALET_COLOR;
                drawSquareMarker(graphics, worldToScreenX(villager.getX()), worldToScreenY(villager.getZ()), color, 3);
            }
        }
        if (waypoint != null && world.dimension().equals(waypointDimension)) {
            drawDiamondMarker(graphics, worldToScreenX(waypoint.getX() + 0.5D), worldToScreenY(waypoint.getZ() + 0.5D), WAYPOINT_COLOR, 5);
        }
        drawPlayerMarker(graphics, worldToScreenX(client.player.getX()), worldToScreenY(client.player.getZ()));
        graphics.disableScissor();
    }

    private void drawLegend(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int left = mapLeft + mapWidth + MAP_MARGIN;
        int right = width - MAP_MARGIN;
        graphics.fill(left, mapTop, right, height - MAP_BOTTOM_MARGIN, 0xD91A2228);
        graphics.outline(left, mapTop, right - left, height - MAP_BOTTOM_MARGIN - mapTop, 0xFF5E6B70);
        graphics.text(font, Component.translatable("screen.valet_map.legend"), left + 10, mapTop + 10, 0xFFFFFFFF, false);
        drawLegendEntry(graphics, left + 12, mapTop + 34, PLAYER_COLOR, Component.translatable("screen.valet_map.player"));
        drawLegendEntry(graphics, left + 12, mapTop + 54, VALET_COLOR, Component.translatable("screen.valet_map.valet"));
        drawLegendEntry(graphics, left + 12, mapTop + 74, SELECTED_GROUP_COLOR, Component.translatable("screen.valet_map.group_member"));
        drawLegendEntry(graphics, left + 12, mapTop + 94, WAYPOINT_COLOR, Component.translatable("screen.valet_map.waypoint"));

        graphics.text(font, Component.translatable("screen.valet_map.controls"), left + 10, mapTop + 116, 0xFFFFFFFF, false);
        graphics.textWithWordWrap(font, Component.translatable("screen.valet_map.help"), left + 10, mapTop + 132, right - left - 20, 0xFFCAD2D6, false);

        int groupLabelY = height - 190;
        graphics.text(font, Component.translatable("screen.valet_map.groups"), left + 10, groupLabelY, 0xFFFFFFFF, false);
        ValetGroupViewData.GroupEntry selected = selectedGroup();
        Component groupLabel = selected == null
                ? Component.translatable("screen.valet_map.no_group")
                : Component.translatable("screen.valet_map.selected_group", selected.name(), selected.memberCount());
        graphics.textWithWordWrap(font, groupLabel, left + 10, groupLabelY + 14, right - left - 20, 0xFFCAD2D6, false);
    }

    private void drawHeaderInfo(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        Minecraft client = Minecraft.getInstance();
        int x = MAP_MARGIN + 112;
        Component info;
        if (isInsideMap(mouseX, mouseY)) {
            BlockPos hovered = screenToWorld(mouseX, mouseY);
            info = Component.translatable("screen.valet_map.header_cursor", hovered.getX(), hovered.getZ(), blocksPerCell());
        } else if (client.player != null) {
            info = Component.translatable("screen.valet_map.header_player", client.player.blockPosition().getX(), client.player.blockPosition().getZ(), blocksPerCell());
        } else {
            return;
        }
        graphics.text(font, info, x, 40, 0xFFCAD2D6, false);
    }

    private void drawLegendEntry(GuiGraphicsExtractor graphics, int x, int y, int color, Component label) {
        graphics.fill(x, y, x + 9, y + 9, 0xFF050708);
        graphics.fill(x + 2, y + 2, x + 7, y + 7, color);
        graphics.text(font, label, x + 16, y + 1, 0xFFCAD2D6, false);
    }

    private void drawSquareMarker(GuiGraphicsExtractor graphics, int x, int y, int color, int radius) {
        graphics.fill(x - radius - 1, y - radius - 1, x + radius + 2, y + radius + 2, 0xFF050708);
        graphics.fill(x - radius, y - radius, x + radius + 1, y + radius + 1, color);
    }

    private void drawDiamondMarker(GuiGraphicsExtractor graphics, int x, int y, int color, int radius) {
        for (int offset = -radius; offset <= radius; offset++) {
            int halfWidth = radius - Math.abs(offset);
            graphics.fill(x - halfWidth - 1, y + offset, x + halfWidth + 2, y + offset + 1, 0xFF050708);
            if (halfWidth > 0) {
                graphics.fill(x - halfWidth, y + offset, x + halfWidth + 1, y + offset + 1, color);
            }
        }
    }

    private void drawPlayerMarker(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.fill(x - 1, y - 7, x + 2, y + 6, 0xFF050708);
        graphics.fill(x - 6, y - 1, x + 7, y + 2, 0xFF050708);
        graphics.fill(x, y - 6, x + 1, y + 5, PLAYER_COLOR);
        graphics.fill(x - 5, y, x + 6, y + 1, PLAYER_COLOR);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (isInsideMap(event.x(), event.y())) {
            if (event.button() == 0) {
                dragging = true;
                return true;
            }
            if (event.button() == 1) {
                setWaypoint(screenToWorld(event.x(), event.y()));
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        if (dragging && event.button() == 0) {
            centerX -= deltaX * blocksPerCell() / CELL_SIZE;
            centerZ -= deltaY * blocksPerCell() / CELL_SIZE;
            rebuildTerrain();
            return true;
        }
        return super.mouseDragged(event, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0 && dragging) {
            dragging = false;
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (!isInsideMap(mouseX, mouseY) || verticalAmount == 0.0D) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
        int oldBlocksPerCell = blocksPerCell();
        int nextZoom = Math.max(0, Math.min(ZOOM_LEVELS.length - 1, zoomIndex + (verticalAmount > 0.0D ? -1 : 1)));
        if (nextZoom == zoomIndex) {
            return true;
        }
        double worldXUnderCursor = centerX + (mouseX - (mapLeft + mapWidth / 2.0D)) * oldBlocksPerCell / CELL_SIZE;
        double worldZUnderCursor = centerZ + (mouseY - (mapTop + mapHeight / 2.0D)) * oldBlocksPerCell / CELL_SIZE;
        zoomIndex = nextZoom;
        centerX = worldXUnderCursor - (mouseX - (mapLeft + mapWidth / 2.0D)) * blocksPerCell() / CELL_SIZE;
        centerZ = worldZUnderCursor - (mouseY - (mapTop + mapHeight / 2.0D)) * blocksPerCell() / CELL_SIZE;
        rebuildTerrain();
        return true;
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreenAndShow(parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }

    private void centerOnPlayer() {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            centerX = client.player.getX();
            centerZ = client.player.getZ();
            rebuildTerrain();
        }
    }

    private void setWaypoint(BlockPos pos) {
        ClientLevel world = Minecraft.getInstance().level;
        if (world != null) {
            int y = world.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)
                    ? world.getHeight(Heightmap.Types.WORLD_SURFACE, pos.getX(), pos.getZ())
                    : 64;
            waypoint = new BlockPos(pos.getX(), y, pos.getZ());
            waypointDimension = world.dimension();
        }
    }

    private static void clearWaypoint() {
        waypoint = null;
        waypointDimension = null;
    }

    public static void clearSession() {
        clearWaypoint();
    }

    public void applyServerState(int nextSelectedGroupId, List<ValetGroupViewData.GroupEntry> nextGroups, List<ValetGroupViewData.ValetEntry> nextValets) {
        groups.clear();
        groups.addAll(nextGroups);
        valets.clear();
        valets.addAll(nextValets);
        selectedGroupId = groups.stream().anyMatch(group -> group.id() == nextSelectedGroupId)
                ? nextSelectedGroupId
                : groups.isEmpty() ? -1 : groups.get(0).id();
        updateGroupButtons();
    }

    private void cycleGroup(int direction) {
        if (groups.isEmpty()) {
            selectedGroupId = -1;
            updateGroupButtons();
            return;
        }
        int index = 0;
        for (int i = 0; i < groups.size(); i++) {
            if (groups.get(i).id() == selectedGroupId) {
                index = i;
                break;
            }
        }
        selectedGroupId = groups.get(Math.floorMod(index + direction, groups.size())).id();
        updateGroupButtons();
    }

    private void sendSelectedGroup() {
        Minecraft client = Minecraft.getInstance();
        if (waypoint != null && client.level != null && client.level.dimension().equals(waypointDimension)) {
            sendMapAction(ManageMapGroupPayload.Action.MOVE_TO, null, waypoint);
        }
    }

    private void sendMapAction(ManageMapGroupPayload.Action action, UUID valetUuid, BlockPos destination) {
        if (ClientPlayNetworking.canSend(ManageMapGroupPayload.TYPE)) {
            ClientPlayNetworking.send(new ManageMapGroupPayload(action, selectedGroupId, valetUuid, destination));
        }
    }

    private void updateGroupButtons() {
        boolean hasGroup = selectedGroupId > 0;
        if (sendGroupButton != null) {
            sendGroupButton.active = hasGroup && waypoint != null;
        }
        if (recallGroupButton != null) {
            recallGroupButton.active = hasGroup;
        }
    }

    private ValetGroupViewData.GroupEntry selectedGroup() {
        for (ValetGroupViewData.GroupEntry group : groups) {
            if (group.id() == selectedGroupId) {
                return group;
            }
        }
        return null;
    }

    private int groupIdFor(UUID uuid) {
        for (ValetGroupViewData.ValetEntry valet : valets) {
            if (valet.uuid().equals(uuid)) {
                return valet.groupId();
            }
        }
        return -1;
    }

    public int getSelectedGroupId() {
        return selectedGroupId;
    }

    public List<ValetGroupViewData.GroupEntry> getGroups() {
        return List.copyOf(groups);
    }

    public List<ValetGroupViewData.ValetEntry> getValets() {
        return List.copyOf(valets);
    }

    private void rebuildTerrain() {
        columns = Math.max(1, (mapWidth + CELL_SIZE - 1) / CELL_SIZE);
        rows = Math.max(1, (mapHeight + CELL_SIZE - 1) / CELL_SIZE);
        terrainColors = new int[columns * rows];
        ClientLevel world = Minecraft.getInstance().level;
        if (world == null) {
            java.util.Arrays.fill(terrainColors, UNKNOWN_COLOR);
            return;
        }
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int row = 0; row < rows; row++) {
            int worldZ = sampleWorldZ(row);
            for (int column = 0; column < columns; column++) {
                int worldX = sampleWorldX(column);
                terrainColors[row * columns + column] = sampleColor(world, cursor, worldX, worldZ);
            }
        }
    }

    private int sampleColor(ClientLevel world, BlockPos.MutableBlockPos cursor, int worldX, int worldZ) {
        if (!world.hasChunk(worldX >> 4, worldZ >> 4)) {
            return UNKNOWN_COLOR;
        }
        int height = world.getHeight(Heightmap.Types.WORLD_SURFACE, worldX, worldZ);
        cursor.set(worldX, height - 1, worldZ);
        BlockState state = world.getBlockState(cursor);
        MapColor color = state.getMapColor(world, cursor);
        if (color == MapColor.NONE) {
            return 0xFF283035;
        }
        MapColor.Brightness brightness = height > world.getSeaLevel() + 32
                ? MapColor.Brightness.HIGH
                : height < world.getSeaLevel() - 8 ? MapColor.Brightness.LOW : MapColor.Brightness.NORMAL;
        return color.calculateARGBColor(brightness) | 0xFF000000;
    }

    private int blocksPerCell() {
        return ZOOM_LEVELS[zoomIndex];
    }

    private int sampleWorldX(int column) {
        return (int) Math.floor(centerX + (column - columns / 2.0D) * blocksPerCell());
    }

    private int sampleWorldZ(int row) {
        return (int) Math.floor(centerZ + (row - rows / 2.0D) * blocksPerCell());
    }

    private int worldToScreenX(double worldX) {
        return (int) Math.round(mapLeft + mapWidth / 2.0D + (worldX - centerX) * CELL_SIZE / blocksPerCell());
    }

    private int worldToScreenY(double worldZ) {
        return (int) Math.round(mapTop + mapHeight / 2.0D + (worldZ - centerZ) * CELL_SIZE / blocksPerCell());
    }

    private BlockPos screenToWorld(double screenX, double screenY) {
        int worldX = (int) Math.floor(centerX + (screenX - (mapLeft + mapWidth / 2.0D)) * blocksPerCell() / CELL_SIZE);
        int worldZ = (int) Math.floor(centerZ + (screenY - (mapTop + mapHeight / 2.0D)) * blocksPerCell() / CELL_SIZE);
        return new BlockPos(worldX, 0, worldZ);
    }

    private boolean isInsideMap(double mouseX, double mouseY) {
        return mouseX >= mapLeft && mouseX < mapLeft + mapWidth && mouseY >= mapTop && mouseY < mapTop + mapHeight;
    }
}
