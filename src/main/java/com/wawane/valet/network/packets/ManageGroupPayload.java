package com.wawane.valet.network.packets;

import com.wawane.valet.ValetMod;
import com.wawane.valet.group.ValetGroupMode;
import com.wawane.valet.gui.ValetGroupScreenHandler;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ManageGroupPayload(BlockPos stationPos, Action action, int groupId, UUID valetUuid, ValetGroupMode mode) implements CustomPacketPayload {
    public static final Type<ManageGroupPayload> TYPE = new Type<>(ValetMod.id("manage_group"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ManageGroupPayload> CODEC = StreamCodec.ofMember(ManageGroupPayload::write, ManageGroupPayload::read);
    public static final UUID NO_VALET = new UUID(0L, 0L);

    public static ManageGroupPayload read(FriendlyByteBuf buf) {
        return new ManageGroupPayload(
                ValetGroupScreenHandler.readBlockPos(buf),
                Action.fromIndex(buf.readInt()),
                buf.readInt(),
                buf.readUUID(),
                ValetGroupMode.fromIndex(buf.readInt())
        );
    }

    public void write(FriendlyByteBuf buf) {
        ValetGroupScreenHandler.writeBlockPos(buf, stationPos);
        buf.writeInt(action.ordinal());
        buf.writeInt(groupId);
        buf.writeUUID(valetUuid == null ? NO_VALET : valetUuid);
        buf.writeInt(mode.ordinal());
    }

    @Override
    public Type<ManageGroupPayload> type() {
        return TYPE;
    }

    public enum Action {
        CREATE,
        DELETE,
        TOGGLE_MEMBER,
        GIVE_CARD,
        BIND_HORN,
        COMMAND;

        public static Action fromIndex(int index) {
            Action[] values = values();
            if (index < 0 || index >= values.length) {
                return CREATE;
            }
            return values[index];
        }
    }
}
