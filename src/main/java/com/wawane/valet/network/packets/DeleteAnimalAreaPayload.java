package com.wawane.valet.network.packets;

import com.wawane.valet.ValetMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record DeleteAnimalAreaPayload(int valetEntityId, int animalAreaId) implements CustomPacketPayload {
    public static final Type<DeleteAnimalAreaPayload> TYPE = new Type<>(ValetMod.id("delete_animal_area"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DeleteAnimalAreaPayload> CODEC = StreamCodec.ofMember(DeleteAnimalAreaPayload::write, DeleteAnimalAreaPayload::read);

    public static DeleteAnimalAreaPayload read(FriendlyByteBuf buf) {
        return new DeleteAnimalAreaPayload(buf.readInt(), buf.readInt());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeInt(valetEntityId);
        buf.writeInt(animalAreaId);
    }

    @Override
    public Type<DeleteAnimalAreaPayload> type() {
        return TYPE;
    }
}
