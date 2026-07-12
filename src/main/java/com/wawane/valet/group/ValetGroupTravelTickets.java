package com.wawane.valet.group;

import com.wawane.valet.ValetMod;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.ChunkPos;

/** Tickets temporaires non persistants qui suivent les groupes en deplacement. */
public final class ValetGroupTravelTickets {
    private static final int TICKET_RADIUS = 2;
    private static final int MAX_TICKET_CENTERS = 32;
    private static final double LOOK_AHEAD_BLOCKS = 32.0D;
    private static final Map<ServerLevel, Set<Long>> ACTIVE_TICKETS = new IdentityHashMap<>();

    private ValetGroupTravelTickets() {
    }

    public static void tick(ServerLevel world) {
        Set<Long> desired = collectDesiredTickets(world);
        Set<Long> previous = ACTIVE_TICKETS.computeIfAbsent(world, ignored -> new HashSet<>());

        for (long packed : desired) {
            if (!previous.contains(packed)) {
                world.getChunkSource().addTicketWithRadius(
                        ValetMod.VALET_GROUP_MISSION_TICKET,
                        ChunkPos.unpack(packed),
                        TICKET_RADIUS
                );
            }
        }
        for (long packed : Set.copyOf(previous)) {
            if (!desired.contains(packed)) {
                world.getChunkSource().removeTicketWithRadius(
                        ValetMod.VALET_GROUP_MISSION_TICKET,
                        ChunkPos.unpack(packed),
                        TICKET_RADIUS
                );
            }
        }

        previous.clear();
        previous.addAll(desired);
        if (previous.isEmpty()) {
            ACTIVE_TICKETS.remove(world);
        }
    }

    private static Set<Long> collectDesiredTickets(ServerLevel world) {
        Set<Long> desired = new HashSet<>();
        for (ValetGroup group : ValetGroupStorage.get(world).getGroups()) {
            ValetGroupCommand command = group.command();
            if (command.mode() != ValetGroupMode.MOVE_TO || command.pos() == null) {
                continue;
            }
            for (UUID member : group.members()) {
                Entity entity = world.getEntity(member);
                if (!(entity instanceof Villager villager) || !villager.isAlive() || villager.isRemoved()) {
                    continue;
                }
                desired.add(villager.chunkPosition().pack());
                double dx = command.pos().getX() + 0.5D - villager.getX();
                double dz = command.pos().getZ() + 0.5D - villager.getZ();
                double distance = Math.sqrt(dx * dx + dz * dz);
                if (distance > 1.0D) {
                    double lookAhead = Math.min(LOOK_AHEAD_BLOCKS, distance);
                    int aheadX = (int) Math.floor(villager.getX() + dx / distance * lookAhead);
                    int aheadZ = (int) Math.floor(villager.getZ() + dz / distance * lookAhead);
                    desired.add(ChunkPos.pack(aheadX >> 4, aheadZ >> 4));
                }
                if (desired.size() >= MAX_TICKET_CENTERS) {
                    return desired;
                }
            }
        }
        return desired;
    }

    public static void clearAll() {
        ACTIVE_TICKETS.clear();
    }
}
