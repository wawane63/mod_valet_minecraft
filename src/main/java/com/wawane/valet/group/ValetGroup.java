package com.wawane.valet.group;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

public final class ValetGroup {
    private final int id;
    private String name;
    private final Set<UUID> members = new LinkedHashSet<>();
    private ValetGroupCommand command = ValetGroupCommand.idle();

    public ValetGroup(int id, String name) {
        this.id = id;
        this.name = cleanName(name, "Groupe " + id);
    }

    public int id() {
        return id;
    }

    public String name() {
        return name;
    }

    public void rename(String name) {
        this.name = cleanName(name, this.name);
    }

    public Set<UUID> members() {
        return Set.copyOf(members);
    }

    public int memberCount() {
        return members.size();
    }

    public boolean hasMember(UUID uuid) {
        return members.contains(uuid);
    }

    public void addMember(UUID uuid) {
        members.add(uuid);
    }

    public void removeMember(UUID uuid) {
        members.remove(uuid);
    }

    public ValetGroupCommand command() {
        return command;
    }

    public void setCommand(ValetGroupCommand command) {
        this.command = command == null ? ValetGroupCommand.idle() : command;
    }

    public CompoundTag writeNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putInt("Id", id);
        nbt.putString("Name", name);
        nbt.put("Command", command.writeNbt());
        ListTag memberList = new ListTag();
        for (UUID member : members) {
            CompoundTag memberNbt = new CompoundTag();
            memberNbt.putString("Uuid", member.toString());
            memberList.add(memberNbt);
        }
        nbt.put("Members", memberList);
        return nbt;
    }

    public static ValetGroup readNbt(CompoundTag nbt) {
        int id = nbt.getIntOr("Id", 0);
        ValetGroup group = new ValetGroup(id, nbt.getStringOr("Name", "Groupe " + id));
        group.setCommand(nbt.getCompound("Command").map(ValetGroupCommand::readNbt).orElse(ValetGroupCommand.idle()));
        ListTag memberList = nbt.getListOrEmpty("Members");
        for (int i = 0; i < memberList.size(); i++) {
            CompoundTag memberNbt = memberList.getCompound(i).orElse(null);
            if (memberNbt == null) {
                continue;
            }
            memberNbt.getString("Uuid").ifPresent(value -> {
                try {
                    group.addMember(UUID.fromString(value));
                } catch (IllegalArgumentException ignored) {
                }
            });
        }
        return group;
    }

    private static String cleanName(String name, String fallback) {
        String clean = name == null ? "" : name.trim().replaceAll("\\s+", " ");
        return clean.isEmpty() ? fallback : clean;
    }
}
