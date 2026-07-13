package com.wawane.valet.network.packets;

import com.wawane.valet.ValetMod;
import com.wawane.valet.ValetRole;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record SetValetRolePayload(int valetEntityId, ValetRole role) implements CustomPacketPayload {
    public static final Type<SetValetRolePayload> TYPE = new Type<>(ValetMod.id("set_valet_role"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SetValetRolePayload> CODEC = StreamCodec.ofMember(SetValetRolePayload::write, SetValetRolePayload::read);

    private static SetValetRolePayload read(FriendlyByteBuf buf) {
        return new SetValetRolePayload(buf.readInt(), ValetRole.fromIndex(buf.readInt()));
    }

    private void write(FriendlyByteBuf buf) {
        buf.writeInt(valetEntityId);
        buf.writeInt(role.ordinal());
    }

    @Override
    public Type<SetValetRolePayload> type() {
        return TYPE;
    }
}
