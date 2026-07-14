package com.wawane.valet.network.packets;

import com.wawane.valet.ValetMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ManageQuestPayload(int questIndex) implements CustomPacketPayload {
    public static final Type<ManageQuestPayload> TYPE = new Type<>(ValetMod.id("manage_quest"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ManageQuestPayload> CODEC = StreamCodec.ofMember(ManageQuestPayload::write, ManageQuestPayload::read);
    public static ManageQuestPayload read(RegistryFriendlyByteBuf buf) { return new ManageQuestPayload(buf.readInt()); }
    public void write(RegistryFriendlyByteBuf buf) { buf.writeInt(questIndex); }
    @Override public Type<ManageQuestPayload> type() { return TYPE; }
}
