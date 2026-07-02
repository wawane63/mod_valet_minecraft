package com.wawane.valet.network.packets;

import com.wawane.valet.ValetMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record SetBreedingOrderPayload(
        int valetEntityId,
        int animalAreaId,
        boolean feed,
        boolean breed,
        boolean shear,
        boolean collectEggs,
        boolean milk,
        int maxAnimals,
        boolean closeScreen
) implements CustomPacketPayload {
    public static final Type<SetBreedingOrderPayload> TYPE = new Type<>(ValetMod.id("set_breeding_order"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SetBreedingOrderPayload> CODEC = StreamCodec.ofMember(SetBreedingOrderPayload::write, SetBreedingOrderPayload::read);

    public static SetBreedingOrderPayload read(FriendlyByteBuf buf) {
        return new SetBreedingOrderPayload(
                buf.readInt(),
                buf.readInt(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readInt(),
                buf.readBoolean()
        );
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeInt(valetEntityId);
        buf.writeInt(animalAreaId);
        buf.writeBoolean(feed);
        buf.writeBoolean(breed);
        buf.writeBoolean(shear);
        buf.writeBoolean(collectEggs);
        buf.writeBoolean(milk);
        buf.writeInt(maxAnimals);
        buf.writeBoolean(closeScreen);
    }

    @Override
    public Type<SetBreedingOrderPayload> type() {
        return TYPE;
    }
}
