package com.wawane.valet.network.packets;

import com.wawane.valet.ValetMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ValetQuestStatePayload(boolean mayorNearby, int[] states, int[] counts) implements CustomPacketPayload {
    public static final Type<ValetQuestStatePayload> TYPE = new Type<>(ValetMod.id("quest_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ValetQuestStatePayload> CODEC = StreamCodec.ofMember(ValetQuestStatePayload::write, ValetQuestStatePayload::read);
    public static ValetQuestStatePayload read(RegistryFriendlyByteBuf buf) {
        int size = Math.min(16, Math.max(0, buf.readInt()));
        int[] states = new int[size]; int[] counts = new int[size];
        for (int i = 0; i < size; i++) { states[i] = buf.readInt(); counts[i] = buf.readInt(); }
        return new ValetQuestStatePayload(buf.readBoolean(), states, counts);
    }
    public void write(RegistryFriendlyByteBuf buf) {
        int size = Math.min(16, Math.min(states.length, counts.length));
        buf.writeInt(size);
        for (int i = 0; i < size; i++) { buf.writeInt(states[i]); buf.writeInt(counts[i]); }
        buf.writeBoolean(mayorNearby);
    }
    @Override public Type<ValetQuestStatePayload> type() { return TYPE; }
}
