package com.wawane.valet.network.packets;

import com.wawane.valet.ValetMod;
import com.wawane.valet.gui.ValetGroupScreenHandler;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ValetGroupStatePayload(
        BlockPos stationPos,
        int selectedGroupId,
        List<ValetGroupScreenHandler.GroupEntry> groups,
        List<ValetGroupScreenHandler.ValetEntry> valets
) implements CustomPacketPayload {
    public static final Type<ValetGroupStatePayload> TYPE = new Type<>(ValetMod.id("group_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ValetGroupStatePayload> CODEC = StreamCodec.ofMember(ValetGroupStatePayload::write, ValetGroupStatePayload::read);

    public ValetGroupStatePayload {
        groups = List.copyOf(groups);
        valets = List.copyOf(valets);
    }

    public static ValetGroupStatePayload read(RegistryFriendlyByteBuf buf) {
        BlockPos stationPos = ValetGroupScreenHandler.readBlockPos(buf);
        int selectedGroupId = buf.readInt();
        return new ValetGroupStatePayload(
                stationPos,
                selectedGroupId,
                ValetGroupScreenHandler.readGroups(buf),
                ValetGroupScreenHandler.readValets(buf)
        );
    }

    public void write(RegistryFriendlyByteBuf buf) {
        ValetGroupScreenHandler.writeBlockPos(buf, stationPos);
        buf.writeInt(selectedGroupId);
        ValetGroupScreenHandler.writeGroups(buf, groups);
        ValetGroupScreenHandler.writeValets(buf, valets);
    }

    @Override
    public Type<ValetGroupStatePayload> type() {
        return TYPE;
    }
}
