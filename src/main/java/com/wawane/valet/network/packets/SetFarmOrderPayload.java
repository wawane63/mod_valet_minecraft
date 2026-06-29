package com.wawane.valet.network.packets;

import com.wawane.valet.ValetMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record SetFarmOrderPayload(int valetEntityId, int farmAreaId, int cropMask, boolean replant, boolean tillSoil, boolean closeScreen) implements CustomPacketPayload {
    public static final Type<SetFarmOrderPayload> TYPE = new Type<>(ValetMod.id("set_farm_order"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SetFarmOrderPayload> CODEC = StreamCodec.ofMember(SetFarmOrderPayload::write, SetFarmOrderPayload::read);

    public static SetFarmOrderPayload read(FriendlyByteBuf buf) {
        return new SetFarmOrderPayload(buf.readInt(), buf.readInt(), buf.readInt(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeInt(valetEntityId);
        buf.writeInt(farmAreaId);
        buf.writeInt(cropMask);
        buf.writeBoolean(replant);
        buf.writeBoolean(tillSoil);
        buf.writeBoolean(closeScreen);
    }

    @Override
    public Type<SetFarmOrderPayload> type() {
        return TYPE;
    }
}
