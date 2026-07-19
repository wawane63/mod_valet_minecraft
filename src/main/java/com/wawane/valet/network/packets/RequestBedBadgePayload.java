package com.wawane.valet.network.packets;

import com.wawane.valet.ValetMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record RequestBedBadgePayload(int valetEntityId) implements CustomPacketPayload {
    public static final Type<RequestBedBadgePayload> TYPE = new Type<>(ValetMod.id("request_bed_badge"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RequestBedBadgePayload> CODEC = StreamCodec.ofMember(
            RequestBedBadgePayload::write,
            RequestBedBadgePayload::read
    );

    private static RequestBedBadgePayload read(FriendlyByteBuf buf) {
        return new RequestBedBadgePayload(buf.readInt());
    }

    private void write(FriendlyByteBuf buf) {
        buf.writeInt(valetEntityId);
    }

    @Override
    public Type<RequestBedBadgePayload> type() {
        return TYPE;
    }
}
