package com.wawane.valet.network.packets;

import com.wawane.valet.progress.ValetCombatPerk;
import net.minecraft.network.PacketByteBuf;

public record ChooseCombatPerkPayload(int valetEntityId, ValetCombatPerk perk) {
    public static ChooseCombatPerkPayload read(PacketByteBuf buf) {
        return new ChooseCombatPerkPayload(buf.readInt(), ValetCombatPerk.fromIndex(buf.readInt()));
    }

    public void write(PacketByteBuf buf) {
        buf.writeInt(valetEntityId);
        buf.writeInt(perk == null ? -1 : perk.ordinal());
    }
}
