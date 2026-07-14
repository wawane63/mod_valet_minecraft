package com.wawane.valet.network.packets;

import com.wawane.valet.ValetMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record DeleteFarmAreaPayload(int valetEntityId, int farmAreaId) implements CustomPacketPayload {
    public static final Type<DeleteFarmAreaPayload> TYPE = new Type<>(ValetMod.id("delete_farm_area"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DeleteFarmAreaPayload> CODEC = StreamCodec.ofMember(DeleteFarmAreaPayload::write, DeleteFarmAreaPayload::read);

    public static DeleteFarmAreaPayload read(FriendlyByteBuf buf) {
        return new DeleteFarmAreaPayload(buf.readInt(), buf.readInt());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeInt(valetEntityId);
        buf.writeInt(farmAreaId);
    }

    @Override
    public Type<DeleteFarmAreaPayload> type() {
        return TYPE;
    }
}
