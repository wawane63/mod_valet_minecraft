package com.wawane.valet.group;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;

/** Donnees bornees partagees par les onglets Carte et Groupes de valets. */
public final class ValetGroupViewData {
    public static final int MAX_VALETS = 512;

    private ValetGroupViewData() {
    }

    public static void writeGroups(FriendlyByteBuf buf, List<GroupEntry> groups) {
        int count = Math.min(groups.size(), ValetGroupStorage.MAX_GROUPS);
        buf.writeInt(count);
        for (int i = 0; i < count; i++) {
            GroupEntry group = groups.get(i);
            buf.writeInt(group.id());
            buf.writeUtf(group.name(), ValetGroup.MAX_NAME_LENGTH);
            buf.writeInt(group.memberCount());
            buf.writeInt(group.mode().ordinal());
        }
    }

    public static List<GroupEntry> readGroups(FriendlyByteBuf buf) {
        int count = readBoundedCount(buf, ValetGroupStorage.MAX_GROUPS, "groups");
        List<GroupEntry> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            result.add(new GroupEntry(buf.readInt(), buf.readUtf(ValetGroup.MAX_NAME_LENGTH), buf.readInt(), ValetGroupMode.fromIndex(buf.readInt())));
        }
        return result;
    }

    public static void writeValets(FriendlyByteBuf buf, List<ValetEntry> valets) {
        int count = Math.min(valets.size(), MAX_VALETS);
        buf.writeInt(count);
        for (int i = 0; i < count; i++) {
            ValetEntry valet = valets.get(i);
            buf.writeInt(valet.entityId());
            buf.writeUUID(valet.uuid());
            buf.writeUtf(valet.name(), 32);
            buf.writeInt(valet.roleIndex());
            buf.writeInt(valet.groupId());
        }
    }

    public static List<ValetEntry> readValets(FriendlyByteBuf buf) {
        int count = readBoundedCount(buf, MAX_VALETS, "valets");
        List<ValetEntry> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            result.add(new ValetEntry(buf.readInt(), buf.readUUID(), buf.readUtf(32), buf.readInt(), buf.readInt()));
        }
        return result;
    }

    private static int readBoundedCount(FriendlyByteBuf buf, int maximum, String label) {
        int count = buf.readInt();
        if (count < 0 || count > maximum) {
            throw new IllegalArgumentException("Invalid " + label + " count: " + count);
        }
        return count;
    }

    public record GroupEntry(int id, String name, int memberCount, ValetGroupMode mode) {
    }

    public record ValetEntry(int entityId, UUID uuid, String name, int roleIndex, int groupId) {
    }
}
