package com.wawane.valet.network.packets;

import com.wawane.valet.ValetMod;
import com.wawane.valet.order.ValetOrder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record SetOrderPayload(int valetEntityId, ValetOrder order, int targetIndex) implements CustomPacketPayload {
    public static final Type<SetOrderPayload> TYPE = new Type<>(ValetMod.id("set_order"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SetOrderPayload> CODEC = StreamCodec.ofMember(SetOrderPayload::write, SetOrderPayload::read);

    public static SetOrderPayload read(FriendlyByteBuf buf) {
        return new SetOrderPayload(buf.readInt(), ValetOrder.fromIndex(buf.readInt()), buf.readInt());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeInt(valetEntityId);
        buf.writeInt(order.ordinal());
        buf.writeInt(targetIndex);
    }

    @Override
    public Type<SetOrderPayload> type() {
        return TYPE;
    }
}
