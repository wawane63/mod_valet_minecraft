package com.wawane.valet.quest;

import com.wawane.valet.ValetMod;
import com.wawane.valet.network.packets.ValetQuestStatePayload;
import java.util.List;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

public final class ValetMayorManager {
    public static final String MAYOR_TAG = "valet_mayor";
    private static final int SCAN_INTERVAL = 200;
    private static final int VILLAGE_RADIUS = 40;
    private static final int MAYOR_ACCESS_RADIUS = 32;

    private ValetMayorManager() {}

    public static void tick(ServerLevel world) {
        if (world.getGameTime() % SCAN_INTERVAL != 0) return;
        for (ServerPlayer player : world.players()) {
            BlockPos bell = findBell(world, player.blockPosition());
            if (bell != null) ensureMayor(world, bell);
        }
    }

    public static void sendState(ServerPlayer player) {
        boolean mayorNearby = hasMayorNearby(player.level(), player.blockPosition());
        int[] states = new int[ValetQuest.values().length];
        int[] counts = new int[states.length];
        for (int i = 0; i < states.length; i++) {
            ValetQuest quest = ValetQuest.values()[i];
            states[i] = player.entityTags().contains(doneTag(quest)) ? 2 : player.entityTags().contains(activeTag(quest)) ? 1 : 0;
            counts[i] = player.getInventory().clearOrCountMatchingItems(stack -> stack.is(quest.requestedItem()), 0, null);
        }
        ServerPlayNetworking.send(player, new ValetQuestStatePayload(mayorNearby, states, counts));
    }

    public static void act(ServerPlayer player, ValetQuest quest) {
        if (quest == null || !hasMayorNearby(player.level(), player.blockPosition())) {
            player.sendOverlayMessage(Component.translatable("message.valet_quest.no_mayor"));
            sendState(player);
            return;
        }
        if (player.entityTags().contains(doneTag(quest))) return;
        if (!player.entityTags().contains(activeTag(quest))) {
            player.addTag(activeTag(quest));
            player.sendOverlayMessage(Component.translatable("message.valet_quest.accepted"));
        } else {
            int available = player.getInventory().clearOrCountMatchingItems(stack -> stack.is(quest.requestedItem()), 0, null);
            if (available < quest.requestedCount()) {
                player.sendOverlayMessage(Component.translatable("message.valet_quest.missing", quest.requestedCount() - available));
            } else {
                player.getInventory().clearOrCountMatchingItems(stack -> stack.is(quest.requestedItem()), quest.requestedCount(), null);
                player.removeTag(activeTag(quest));
                player.addTag(doneTag(quest));
                player.getInventory().placeItemBackInInventory(new ItemStack(Items.EMERALD, quest.emeraldReward()));
                player.sendSystemMessage(Component.translatable("message.valet_quest.completed", quest.emeraldReward()));
            }
        }
        sendState(player);
    }

    private static BlockPos findBell(ServerLevel world, BlockPos origin) {
        for (BlockPos pos : BlockPos.withinManhattan(origin, VILLAGE_RADIUS, 12, VILLAGE_RADIUS)) {
            if (world.getBlockState(pos).is(Blocks.BELL)) return pos.immutable();
        }
        return null;
    }

    private static void ensureMayor(ServerLevel world, BlockPos bell) {
        AABB area = new AABB(bell).inflate(VILLAGE_RADIUS, 16, VILLAGE_RADIUS);
        if (world.getEntitiesOfClass(Cat.class, area).isEmpty() || world.getEntitiesOfClass(IronGolem.class, area).isEmpty()) return;
        List<Villager> mayors = world.getEntitiesOfClass(Villager.class, area, ValetMayorManager::isMayor);
        if (!mayors.isEmpty()) return;
        Villager mayor = EntityTypes.VILLAGER.spawn(world, bell.above(), EntitySpawnReason.EVENT);
        if (mayor == null) return;
        mayor.addTag(MAYOR_TAG);
        mayor.setCustomName(Component.translatable("entity.valet.mayor"));
        mayor.setCustomNameVisible(true);
        mayor.setPersistenceRequired();
        ValetMod.LOGGER.info("Mayor spawned at {}", bell);
    }

    public static boolean isMayor(Villager villager) { return villager.entityTags().contains(MAYOR_TAG); }

    private static boolean hasMayorNearby(ServerLevel world, BlockPos pos) {
        return !world.getEntitiesOfClass(Villager.class, new AABB(pos).inflate(MAYOR_ACCESS_RADIUS), ValetMayorManager::isMayor).isEmpty();
    }

    private static String activeTag(ValetQuest quest) { return "valet_quest_active_" + quest.id(); }
    private static String doneTag(ValetQuest quest) { return "valet_quest_done_" + quest.id(); }
}
