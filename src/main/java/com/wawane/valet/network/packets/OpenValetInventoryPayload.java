package com.wawane.valet.network.packets;

import com.wawane.valet.ValetMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record OpenValetInventoryPayload(int valetEntityId) implements CustomPacketPayload {
    public static final Type<OpenValetInventoryPayload> TYPE = new Type<>(ValetMod.id("open_valet_inventory"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenValetInventoryPayload> CODEC = StreamCodec.ofMember(OpenValetInventoryPayload::write, OpenValetInventoryPayload::read);

    public static OpenValetInventoryPayload read(FriendlyByteBuf buf) {
        return new OpenValetInventoryPayload(buf.readInt());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeInt(valetEntityId);
    }

    @Override
    public Type<OpenValetInventoryPayload> type() {
        return TYPE;
    }
}
