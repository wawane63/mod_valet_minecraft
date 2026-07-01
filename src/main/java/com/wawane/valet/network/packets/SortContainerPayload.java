package com.wawane.valet.network.packets;

import com.wawane.valet.ValetMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record SortContainerPayload() implements CustomPacketPayload {
    public static final SortContainerPayload INSTANCE = new SortContainerPayload();
    public static final Type<SortContainerPayload> TYPE = new Type<>(ValetMod.id("sort_container"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SortContainerPayload> CODEC = StreamCodec.ofMember(SortContainerPayload::write, SortContainerPayload::read);

    public static SortContainerPayload read(FriendlyByteBuf buf) {
        return INSTANCE;
    }

    public void write(FriendlyByteBuf buf) {
    }

    @Override
    public Type<SortContainerPayload> type() {
        return TYPE;
    }
}
