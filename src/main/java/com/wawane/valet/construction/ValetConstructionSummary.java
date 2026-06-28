package com.wawane.valet.construction;

import net.minecraft.network.FriendlyByteBuf;

public record ValetConstructionSummary(int id, String name, int width, int height, int depth, int blockCount) {
    public static ValetConstructionSummary fromBlueprint(ValetConstructionBlueprint blueprint) {
        return new ValetConstructionSummary(
                blueprint.id(),
                blueprint.name(),
                blueprint.width(),
                blueprint.height(),
                blueprint.depth(),
                blueprint.blockCount()
        );
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeInt(id);
        buf.writeUtf(name, 48);
        buf.writeInt(width);
        buf.writeInt(height);
        buf.writeInt(depth);
        buf.writeInt(blockCount);
    }

    public static ValetConstructionSummary read(FriendlyByteBuf buf) {
        return new ValetConstructionSummary(
                buf.readInt(),
                buf.readUtf(48),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt()
        );
    }
}
