package com.wawane.valet.network.packets;

import net.minecraft.network.PacketByteBuf;

public record RenameValetPayload(int valetEntityId, String name) {
    public static RenameValetPayload read(PacketByteBuf buf) {
        return new RenameValetPayload(buf.readInt(), buf.readString(32));
    }

    public void write(PacketByteBuf buf) {
        buf.writeInt(valetEntityId);
        buf.writeString(name, 32);
    }
}
