package com.wawane.valet.construction;

import java.util.Arrays;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Vue compacte d'un blueprint pour l'interface. Le plan complet reste cote
 * serveur; seule une grille de hauteur 16x16 maximum traverse le reseau.
 */
public record ValetConstructionSummary(
        int id,
        String name,
        int width,
        int height,
        int depth,
        int blockCount,
        int previewWidth,
        int previewDepth,
        byte[] previewHeights
) {
    private static final int MAX_PREVIEW_SIDE = 16;
    private static final int MAX_PREVIEW_CELLS = MAX_PREVIEW_SIDE * MAX_PREVIEW_SIDE;

    public ValetConstructionSummary {
        name = ValetConstructionBlueprint.cleanName(name, id);
        width = Math.max(0, width);
        height = Math.max(0, height);
        depth = Math.max(0, depth);
        blockCount = Math.max(0, blockCount);
        previewWidth = Math.clamp(previewWidth, 0, MAX_PREVIEW_SIDE);
        previewDepth = Math.clamp(previewDepth, 0, MAX_PREVIEW_SIDE);
        int expectedLength = previewWidth * previewDepth;
        if (previewHeights == null || expectedLength == 0 || previewHeights.length != expectedLength) {
            previewWidth = 0;
            previewDepth = 0;
            previewHeights = new byte[0];
        } else {
            previewHeights = Arrays.copyOf(previewHeights, expectedLength);
        }
    }

    public static ValetConstructionSummary fromBlueprint(ValetConstructionBlueprint blueprint) {
        int width = Math.max(0, blueprint.width());
        int depth = Math.max(0, blueprint.depth());
        int previewWidth = Math.min(MAX_PREVIEW_SIDE, width);
        int previewDepth = Math.min(MAX_PREVIEW_SIDE, depth);
        byte[] heights = new byte[previewWidth * previewDepth];
        if (previewWidth > 0 && previewDepth > 0) {
            for (ValetConstructionBlueprint.Entry entry : blueprint.entries()) {
                int previewX = (int) Math.min(previewWidth - 1L, (long) entry.x() * previewWidth / width);
                int previewZ = (int) Math.min(previewDepth - 1L, (long) entry.z() * previewDepth / depth);
                int index = previewZ * previewWidth + previewX;
                int encodedHeight = Math.min(255, entry.y() + 1);
                if (encodedHeight > Byte.toUnsignedInt(heights[index])) {
                    heights[index] = (byte) encodedHeight;
                }
            }
        }
        return new ValetConstructionSummary(
                blueprint.id(),
                blueprint.name(),
                width,
                blueprint.height(),
                depth,
                blueprint.blockCount(),
                previewWidth,
                previewDepth,
                heights
        );
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeInt(id);
        buf.writeUtf(name, ValetConstructionBlueprint.MAX_NAME_LENGTH);
        buf.writeInt(width);
        buf.writeInt(height);
        buf.writeInt(depth);
        buf.writeInt(blockCount);
        buf.writeInt(previewWidth);
        buf.writeInt(previewDepth);
        buf.writeByteArray(previewHeights);
    }

    public static ValetConstructionSummary read(FriendlyByteBuf buf) {
        return new ValetConstructionSummary(
                buf.readInt(),
                buf.readUtf(ValetConstructionBlueprint.MAX_NAME_LENGTH),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readByteArray(MAX_PREVIEW_CELLS)
        );
    }

    public int previewHeight(int x, int z) {
        if (x < 0 || x >= previewWidth || z < 0 || z >= previewDepth) {
            return -1;
        }
        return Byte.toUnsignedInt(previewHeights[z * previewWidth + x]) - 1;
    }

    @Override
    public byte[] previewHeights() {
        return Arrays.copyOf(previewHeights, previewHeights.length);
    }
}
