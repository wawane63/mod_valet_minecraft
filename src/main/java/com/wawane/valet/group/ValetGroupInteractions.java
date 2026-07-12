package com.wawane.valet.group;

import com.wawane.valet.ValetDebug;
import com.wawane.valet.ValetMod;
import com.wawane.valet.ValetRole;
import com.wawane.valet.ai.ValetWorkGoal;
import com.wawane.valet.network.packets.ManageMapGroupPayload;
import com.wawane.valet.network.packets.ValetGroupStatePayload;
import com.wawane.valet.state.ValetBehavior;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;

public final class ValetGroupInteractions {
    private static final double MAP_VALET_ASSIGN_RADIUS = 512.0D;

    private ValetGroupInteractions() {
    }

    public static void handleMapManagement(ManageMapGroupPayload payload, ServerPlayNetworking.Context context) {
        ServerPlayer player = context.player();
        context.server().execute(() -> {
            ServerLevel world = player.level();
            ValetGroupStorage storage = ValetGroupStorage.get(world);
            int selectedGroupId = payload.groupId();
            switch (payload.action()) {
                case REQUEST -> {
                }
                case CREATE -> {
                    ValetGroup group = storage.addGroup(storage.nextDefaultName());
                    if (group != null) {
                        selectedGroupId = group.id();
                        player.sendOverlayMessage(Component.translatable("message.valet_group.created", group.name()));
                    }
                }
                case DELETE -> {
                    ValetGroup removed = storage.getGroup(payload.groupId());
                    if (removed != null && storage.removeGroup(payload.groupId())) {
                        restartMembers(world, removed.members());
                        player.sendOverlayMessage(Component.translatable("message.valet_group.deleted"));
                    }
                    selectedGroupId = firstGroupId(storage);
                }
                case TOGGLE_MEMBER -> {
                    if (isAssignableMapValet(world, player, payload.valetUuid())) {
                        ValetGroup target = storage.getGroup(payload.groupId());
                        boolean removing = target != null && target.hasMember(payload.valetUuid());
                        storage.toggleMember(payload.groupId(), payload.valetUuid());
                        if (removing) {
                            restartMember(world, payload.valetUuid());
                        } else if (target != null && target.hasMember(payload.valetUuid())) {
                            restartGroup(world, target);
                        }
                    }
                }
                case MOVE_TO -> {
                    ValetGroup group = storage.getGroup(payload.groupId());
                    BlockPos destination = payload.destination();
                    if (group != null && group.memberCount() > 0 && destination != null
                            && world.getWorldBorder().isWithinBounds(destination.getX(), destination.getZ())) {
                        applyGroupCommand(world, player, group.id(), ValetGroupCommand.moveTo(new BlockPos(destination.getX(), 0, destination.getZ())));
                    }
                }
                case RECALL -> {
                    if (storage.getGroup(payload.groupId()) != null) {
                        applyGroupCommand(world, player, payload.groupId(), ValetGroupCommand.recall());
                    }
                }
            }
            sendMapGroupState(player, selectedGroupId);
        });
    }

    private static void sendMapGroupState(ServerPlayer player, int selectedGroupId) {
        if (!ServerPlayNetworking.canSend(player, ValetGroupStatePayload.TYPE)) {
            return;
        }
        ServerPlayNetworking.send(player, new ValetGroupStatePayload(
                selectedGroupId > 0 ? selectedGroupId : firstGroupId(ValetGroupStorage.get(player.level())),
                buildGroupEntries(player.level()),
                buildMapValetEntries(player.level(), player)
        ));
    }

    private static List<ValetGroupViewData.GroupEntry> buildGroupEntries(ServerLevel world) {
        return ValetGroupStorage.get(world).getGroups().stream()
                .map(group -> new ValetGroupViewData.GroupEntry(group.id(), group.name(), group.memberCount(), group.command().mode()))
                .toList();
    }

    private static List<ValetGroupViewData.ValetEntry> buildMapValetEntries(ServerLevel world, ServerPlayer player) {
        List<Villager> candidates = new ArrayList<>();
        for (Entity entity : world.getAllEntities()) {
            if (entity instanceof Villager villager && villager.isAlive() && !villager.isRemoved() && ValetMod.isValet(villager)
                    && villager.distanceToSqr(player) <= MAP_VALET_ASSIGN_RADIUS * MAP_VALET_ASSIGN_RADIUS) {
                candidates.add(villager);
            }
        }
        candidates.sort(Comparator.comparingDouble(player::distanceToSqr));
        ValetGroupStorage storage = ValetGroupStorage.get(world);
        return candidates.stream()
                .limit(ValetGroupViewData.MAX_VALETS)
                .map(villager -> new ValetGroupViewData.ValetEntry(
                        villager.getId(),
                        villager.getUUID(),
                        displayName(villager),
                        ValetRole.get(world, villager).ordinal(),
                        storage.getGroupIdForMember(villager.getUUID())
                ))
                .toList();
    }

    private static String displayName(Villager villager) {
        String name = villager.hasCustomName() && villager.getCustomName() != null ? villager.getCustomName().getString() : "";
        if (name.isBlank() || "profession.valet.valet".equals(name)) {
            String uuid = villager.getUUID().toString();
            name = "Valet " + uuid.substring(0, 4);
        }
        return name.length() > 32 ? name.substring(0, 32) : name;
    }

    private static boolean isAssignableMapValet(ServerLevel world, ServerPlayer player, UUID valetUuid) {
        if (valetUuid == null || ManageMapGroupPayload.NO_VALET.equals(valetUuid)) {
            return false;
        }
        Entity entity = world.getEntity(valetUuid);
        return entity instanceof Villager villager
                && villager.isAlive()
                && !villager.isRemoved()
                && ValetMod.isValet(villager)
                && villager.distanceToSqr(player) <= MAP_VALET_ASSIGN_RADIUS * MAP_VALET_ASSIGN_RADIUS;
    }

    private static void applyGroupCommand(ServerLevel world, Player player, int groupId, ValetGroupCommand command) {
        ValetGroupStorage storage = ValetGroupStorage.get(world);
        ValetGroup group = storage.getGroup(groupId);
        if (group == null) {
            player.sendOverlayMessage(Component.translatable("message.valet_group.unknown"));
            return;
        }
        if (group.command().equals(command)) {
            return;
        }
        storage.setCommand(groupId, command);
        restartGroup(world, group);
        Component mode = Component.translatable(command.mode().getTranslationKey());
        player.sendOverlayMessage(Component.translatable("message.valet_group.command_set", group.name(), mode));
    }

    private static void restartGroup(ServerLevel world, ValetGroup group) {
        for (UUID member : group.members()) {
            Entity entity = world.getEntity(member);
            if (entity instanceof Villager villager && ValetMod.isValet(villager)) {
                ValetBehavior.clearRecall(villager.getUUID());
                if (group.command().mode() == ValetGroupMode.RECALL) {
                    ValetBehavior.recallToWorkstation(world, villager);
                }
                ValetWorkGoal.requestRestart(villager);
                ValetDebug.record(villager, "group command=" + group.command().mode().name().toLowerCase() + " group=" + group.id());
            }
        }
    }

    private static void restartMembers(ServerLevel world, java.util.Set<UUID> members) {
        for (UUID member : members) {
            restartMember(world, member);
        }
    }

    private static void restartMember(ServerLevel world, UUID member) {
        Entity entity = world.getEntity(member);
        if (entity instanceof Villager villager && ValetMod.isValet(villager)) {
            ValetBehavior.clearRecall(villager.getUUID());
            ValetWorkGoal.requestRestart(villager);
        }
    }

    private static int firstGroupId(ValetGroupStorage storage) {
        List<ValetGroup> groups = storage.getGroups();
        return groups.isEmpty() ? -1 : groups.get(0).id();
    }
}
