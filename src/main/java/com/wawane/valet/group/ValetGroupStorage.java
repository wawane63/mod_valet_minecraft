package com.wawane.valet.group;

import com.mojang.serialization.Codec;
import com.wawane.valet.ValetMod;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public final class ValetGroupStorage extends SavedData {
    private static final String KEY = "valet_groups";
    private static final Codec<ValetGroupStorage> CODEC = CompoundTag.CODEC.xmap(ValetGroupStorage::fromNbt, ValetGroupStorage::saveToNbt);
    private static final SavedDataType<ValetGroupStorage> TYPE = new SavedDataType<>(
            ValetMod.id(KEY),
            ValetGroupStorage::new,
            CODEC,
            DataFixTypes.LEVEL
    );
    public static final int MAX_GROUPS = 32;

    private final List<ValetGroup> groups = new ArrayList<>();
    private int nextId = 1;

    public static ValetGroupStorage get(ServerLevel world) {
        return world.getDataStorage().computeIfAbsent(TYPE);
    }

    public ValetGroup addGroup(String name) {
        if (groups.size() >= MAX_GROUPS || nextId <= 0 || nextId == Integer.MAX_VALUE) {
            return null;
        }
        ValetGroup group = new ValetGroup(nextId, name);
        nextId++;
        groups.add(group);
        setDirty();
        return group;
    }

    public boolean removeGroup(int id) {
        boolean removed = groups.removeIf(group -> group.id() == id);
        if (removed) {
            setDirty();
        }
        return removed;
    }

    public ValetGroup getGroup(int id) {
        for (ValetGroup group : groups) {
            if (group.id() == id) {
                return group;
            }
        }
        return null;
    }

    public List<ValetGroup> getGroups() {
        return List.copyOf(groups);
    }

    public int getGroupIdForMember(UUID valetUuid) {
        for (ValetGroup group : groups) {
            if (group.hasMember(valetUuid)) {
                return group.id();
            }
        }
        return -1;
    }

    public ValetGroupCommand getCommandForValet(UUID valetUuid) {
        for (ValetGroup group : groups) {
            if (group.hasMember(valetUuid)) {
                return group.command();
            }
        }
        return ValetGroupCommand.idle();
    }

    public void toggleMember(int groupId, UUID valetUuid) {
        ValetGroup target = getGroup(groupId);
        if (target == null) {
            return;
        }
        if (target.hasMember(valetUuid)) {
            target.removeMember(valetUuid);
            setDirty();
            return;
        }
        for (ValetGroup group : groups) {
            group.removeMember(valetUuid);
        }
        target.addMember(valetUuid);
        setDirty();
    }

    public void setCommand(int groupId, ValetGroupCommand command) {
        ValetGroup group = getGroup(groupId);
        if (group == null) {
            return;
        }
        group.setCommand(command);
        setDirty();
    }

    public String nextDefaultName() {
        return "Groupe " + nextId;
    }

    private CompoundTag saveToNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putInt("NextId", nextId);
        ListTag list = new ListTag();
        for (ValetGroup group : groups) {
            list.add(group.writeNbt());
        }
        nbt.put("Groups", list);
        return nbt;
    }

    private static ValetGroupStorage fromNbt(CompoundTag nbt) {
        ValetGroupStorage storage = new ValetGroupStorage();
        int savedNextId = nbt.getIntOr("NextId", 1);
        storage.nextId = savedNextId > 0 && savedNextId < Integer.MAX_VALUE ? savedNextId : 1;
        ListTag list = nbt.getListOrEmpty("Groups");
        Set<Integer> loadedIds = new HashSet<>();
        for (int i = 0; i < list.size() && storage.groups.size() < MAX_GROUPS; i++) {
            CompoundTag groupNbt = list.getCompound(i).orElse(null);
            if (groupNbt == null) {
                continue;
            }
            ValetGroup group = ValetGroup.readNbt(groupNbt);
            if (group.id() <= 0 || group.id() == Integer.MAX_VALUE || !loadedIds.add(group.id())) {
                continue;
            }
            storage.groups.add(group);
            storage.nextId = Math.max(storage.nextId, group.id() + 1);
        }
        storage.groups.sort(java.util.Comparator.comparingInt(ValetGroup::id));
        Set<UUID> assignedMembers = new HashSet<>();
        for (ValetGroup group : storage.groups) {
            for (UUID member : group.members()) {
                if (!assignedMembers.add(member)) {
                    group.removeMember(member);
                }
            }
        }
        return storage;
    }
}
