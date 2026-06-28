package com.wawane.valet.network.packets;

import com.wawane.valet.ValetMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record RenameValetPayload(int valetEntityId, String name) implements CustomPacketPayload {
    public static final Type<RenameValetPayload> TYPE = new Type<>(ValetMod.id("rename_valet"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RenameValetPayload> CODEC = StreamCodec.ofMember(RenameValetPayload::write, RenameValetPayload::read);

    public static RenameValetPayload read(FriendlyByteBuf buf) {
        return new RenameValetPayload(buf.readInt(), buf.readUtf(32));
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeInt(valetEntityId);
        buf.writeUtf(name, 32);
    }

    @Override
    public Type<RenameValetPayload> type() {
        return TYPE;
    }
}
