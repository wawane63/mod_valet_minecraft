package com.wawane.valet.network.packets;

import com.wawane.valet.order.ValetOrder;
import net.minecraft.network.PacketByteBuf;

public record SetOrderPayload(int valetEntityId, ValetOrder order, int targetIndex) {
    public static SetOrderPayload read(PacketByteBuf buf) {
        return new SetOrderPayload(buf.readInt(), ValetOrder.fromIndex(buf.readInt()), buf.readInt());
    }

    public void write(PacketByteBuf buf) {
        buf.writeInt(valetEntityId);
        buf.writeInt(order.ordinal());
        buf.writeInt(targetIndex);
    }
}
