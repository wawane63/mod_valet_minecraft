package com.wawane.valet.network.packets;

import com.wawane.valet.ValetMod;
import com.wawane.valet.progress.ValetCombatPerk;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ChooseCombatPerkPayload(int valetEntityId, ValetCombatPerk perk) implements CustomPacketPayload {
    public static final Type<ChooseCombatPerkPayload> TYPE = new Type<>(ValetMod.id("choose_combat_perk"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ChooseCombatPerkPayload> CODEC = StreamCodec.ofMember(ChooseCombatPerkPayload::write, ChooseCombatPerkPayload::read);

    public static ChooseCombatPerkPayload read(FriendlyByteBuf buf) {
        return new ChooseCombatPerkPayload(buf.readInt(), ValetCombatPerk.fromIndex(buf.readInt()));
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeInt(valetEntityId);
        buf.writeInt(perk == null ? -1 : perk.ordinal());
    }

    @Override
    public Type<ChooseCombatPerkPayload> type() {
        return TYPE;
    }
}
