package com.wawane.valet.network.packets;

import net.minecraft.network.PacketByteBuf;

public record DeleteConstructionPayload(int valetEntityId, int constructionId) {
    public static DeleteConstructionPayload read(PacketByteBuf buf) {
        return new DeleteConstructionPayload(buf.readInt(), buf.readInt());
    }

    public void write(PacketByteBuf buf) {
        buf.writeInt(valetEntityId);
        buf.writeInt(constructionId);
    }
}
