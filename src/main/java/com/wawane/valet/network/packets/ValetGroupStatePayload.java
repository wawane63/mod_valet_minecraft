package com.wawane.valet.network.packets;

import com.wawane.valet.ValetMod;
import com.wawane.valet.group.ValetGroupViewData;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ValetGroupStatePayload(
        int selectedGroupId,
        List<ValetGroupViewData.GroupEntry> groups,
        List<ValetGroupViewData.ValetEntry> valets
) implements CustomPacketPayload {
    public static final Type<ValetGroupStatePayload> TYPE = new Type<>(ValetMod.id("group_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ValetGroupStatePayload> CODEC = StreamCodec.ofMember(ValetGroupStatePayload::write, ValetGroupStatePayload::read);

    public ValetGroupStatePayload {
        groups = List.copyOf(groups.subList(0, Math.min(groups.size(), com.wawane.valet.group.ValetGroupStorage.MAX_GROUPS)));
        valets = List.copyOf(valets.subList(0, Math.min(valets.size(), ValetGroupViewData.MAX_VALETS)));
    }

    public static ValetGroupStatePayload read(RegistryFriendlyByteBuf buf) {
        int selectedGroupId = buf.readInt();
        return new ValetGroupStatePayload(
                selectedGroupId,
                ValetGroupViewData.readGroups(buf),
                ValetGroupViewData.readValets(buf)
        );
    }

    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeInt(selectedGroupId);
        ValetGroupViewData.writeGroups(buf, groups);
        ValetGroupViewData.writeValets(buf, valets);
    }

    @Override
    public Type<ValetGroupStatePayload> type() {
        return TYPE;
    }
}
