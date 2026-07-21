package com.wawane.valet.network.packets;

import com.wawane.valet.ValetMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record RenameWorkAreaPayload(int valetEntityId, boolean animalArea, int areaId, String name) implements CustomPacketPayload {
    public static final Type<RenameWorkAreaPayload> TYPE = new Type<>(ValetMod.id("rename_work_area"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RenameWorkAreaPayload> CODEC = StreamCodec.ofMember(RenameWorkAreaPayload::write, RenameWorkAreaPayload::read);

    public static RenameWorkAreaPayload read(FriendlyByteBuf buf) {
        return new RenameWorkAreaPayload(buf.readInt(), buf.readBoolean(), buf.readInt(), buf.readUtf(48));
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeInt(valetEntityId);
        buf.writeBoolean(animalArea);
        buf.writeInt(areaId);
        buf.writeUtf(name, 48);
    }

    @Override
    public Type<RenameWorkAreaPayload> type() {
        return TYPE;
    }
}
