package com.wawane.valet.network.packets;

import com.wawane.valet.ValetMod;
import com.wawane.valet.progress.ValetPerk;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ChoosePerkPayload(int valetEntityId, ValetPerk perk) implements CustomPacketPayload {
    public static final Type<ChoosePerkPayload> TYPE = new Type<>(ValetMod.id("choose_perk"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ChoosePerkPayload> CODEC = StreamCodec.ofMember(ChoosePerkPayload::write, ChoosePerkPayload::read);

    public static ChoosePerkPayload read(FriendlyByteBuf buf) {
        return new ChoosePerkPayload(buf.readInt(), ValetPerk.fromIndex(buf.readInt()));
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeInt(valetEntityId);
        buf.writeInt(perk == null ? -1 : perk.ordinal());
    }

    @Override
    public Type<ChoosePerkPayload> type() {
        return TYPE;
    }
}
