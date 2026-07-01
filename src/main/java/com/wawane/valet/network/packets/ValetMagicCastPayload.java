package com.wawane.valet.network.packets;

import com.wawane.valet.ValetMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ValetMagicCastPayload(int valetEntityId) implements CustomPacketPayload {
    public static final Type<ValetMagicCastPayload> TYPE = new Type<>(ValetMod.id("magic_cast"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ValetMagicCastPayload> CODEC = StreamCodec.ofMember(ValetMagicCastPayload::write, ValetMagicCastPayload::read);

    public static ValetMagicCastPayload read(FriendlyByteBuf buf) {
        return new ValetMagicCastPayload(buf.readInt());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeInt(valetEntityId);
    }

    @Override
    public Type<ValetMagicCastPayload> type() {
        return TYPE;
    }
}
