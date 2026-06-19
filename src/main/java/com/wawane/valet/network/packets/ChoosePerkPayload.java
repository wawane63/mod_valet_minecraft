package com.wawane.valet.network.packets;

import com.wawane.valet.progress.ValetPerk;
import net.minecraft.network.PacketByteBuf;

public record ChoosePerkPayload(int valetEntityId, ValetPerk perk) {
    public static ChoosePerkPayload read(PacketByteBuf buf) {
        return new ChoosePerkPayload(buf.readInt(), ValetPerk.fromIndex(buf.readInt()));
    }

    public void write(PacketByteBuf buf) {
        buf.writeInt(valetEntityId);
        buf.writeInt(perk == null ? -1 : perk.ordinal());
    }
}
