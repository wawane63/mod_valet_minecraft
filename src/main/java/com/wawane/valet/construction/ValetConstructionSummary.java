package com.wawane.valet.construction;

import net.minecraft.network.PacketByteBuf;

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

    public void write(PacketByteBuf buf) {
        buf.writeInt(id);
        buf.writeString(name, 48);
        buf.writeInt(width);
        buf.writeInt(height);
        buf.writeInt(depth);
        buf.writeInt(blockCount);
    }

    public static ValetConstructionSummary read(PacketByteBuf buf) {
        return new ValetConstructionSummary(
                buf.readInt(),
                buf.readString(48),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt()
        );
    }
}
