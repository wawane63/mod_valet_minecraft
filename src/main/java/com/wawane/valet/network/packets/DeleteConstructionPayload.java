package com.wawane.valet.network.packets;

import com.wawane.valet.ValetMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record DeleteConstructionPayload(int valetEntityId, int constructionId) implements CustomPacketPayload {
    public static final Type<DeleteConstructionPayload> TYPE = new Type<>(ValetMod.id("delete_construction"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DeleteConstructionPayload> CODEC = StreamCodec.ofMember(DeleteConstructionPayload::write, DeleteConstructionPayload::read);

    public static DeleteConstructionPayload read(FriendlyByteBuf buf) {
        return new DeleteConstructionPayload(buf.readInt(), buf.readInt());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeInt(valetEntityId);
        buf.writeInt(constructionId);
    }

    @Override
    public Type<DeleteConstructionPayload> type() {
        return TYPE;
    }
}
