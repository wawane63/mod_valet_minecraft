package com.wawane.valet.network.packets;

import com.wawane.valet.ValetMod;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ManageMapGroupPayload(Action action, int groupId, UUID valetUuid, BlockPos destination) implements CustomPacketPayload {
    public static final Type<ManageMapGroupPayload> TYPE = new Type<>(ValetMod.id("manage_map_group"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ManageMapGroupPayload> CODEC = StreamCodec.ofMember(ManageMapGroupPayload::write, ManageMapGroupPayload::read);
    public static final UUID NO_VALET = new UUID(0L, 0L);

    public static ManageMapGroupPayload read(FriendlyByteBuf buf) {
        return new ManageMapGroupPayload(
                Action.fromIndex(buf.readInt()),
                buf.readInt(),
                buf.readUUID(),
                new BlockPos(buf.readInt(), buf.readInt(), buf.readInt())
        );
    }

    public void write(FriendlyByteBuf buf) {
        BlockPos pos = destination == null ? BlockPos.ZERO : destination;
        buf.writeInt(action.ordinal());
        buf.writeInt(groupId);
        buf.writeUUID(valetUuid == null ? NO_VALET : valetUuid);
        buf.writeInt(pos.getX());
        buf.writeInt(pos.getY());
        buf.writeInt(pos.getZ());
    }

    @Override
    public Type<ManageMapGroupPayload> type() {
        return TYPE;
    }

    public enum Action {
        REQUEST,
        CREATE,
        DELETE,
        TOGGLE_MEMBER,
        MOVE_TO,
        RECALL;

        public static Action fromIndex(int index) {
            Action[] values = values();
            return index >= 0 && index < values.length ? values[index] : REQUEST;
        }
    }
}
