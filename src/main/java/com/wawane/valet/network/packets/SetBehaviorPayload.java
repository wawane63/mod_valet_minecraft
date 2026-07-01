package com.wawane.valet.network.packets;

import com.wawane.valet.ValetMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record SetBehaviorPayload(int valetEntityId, boolean freeBehavior, boolean avoidNightReturn) implements CustomPacketPayload {
    public static final Type<SetBehaviorPayload> TYPE = new Type<>(ValetMod.id("set_behavior"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SetBehaviorPayload> CODEC = StreamCodec.ofMember(SetBehaviorPayload::write, SetBehaviorPayload::read);

    public static SetBehaviorPayload read(FriendlyByteBuf buf) {
        return new SetBehaviorPayload(buf.readInt(), buf.readBoolean(), buf.readBoolean());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeInt(valetEntityId);
        buf.writeBoolean(freeBehavior);
        buf.writeBoolean(avoidNightReturn);
    }

    @Override
    public Type<SetBehaviorPayload> type() {
        return TYPE;
    }
}
