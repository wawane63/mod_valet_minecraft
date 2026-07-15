package com.wawane.valet.quest;

import com.wawane.valet.ValetMod;
import com.wawane.valet.network.packets.ValetQuestStatePayload;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;

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
            counts[i] = countRequestedItems(player, quest);
        }
        ServerPlayNetworking.send(player, new ValetQuestStatePayload(mayorNearby, states, counts));
    }

    public static InteractionResult openQuestScreen(
            Player player,
            Level world,
            InteractionHand hand,
            Entity entity,
            EntityHitResult hitResult
    ) {
        if (hand != InteractionHand.MAIN_HAND || !(entity instanceof Villager villager) || !isMayor(villager)) {
            return InteractionResult.PASS;
        }
        if (!world.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            sendState(serverPlayer);
        }
        return InteractionResult.SUCCESS;
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
            int available = countRequestedItems(player, quest);
            if (available < quest.requestedCount()) {
                player.sendOverlayMessage(Component.translatable("message.valet_quest.missing", quest.requestedCount() - available));
            } else {
                removeRequestedItems(player, quest, quest.requestedCount());
                player.removeTag(activeTag(quest));
                player.addTag(doneTag(quest));
                player.getInventory().placeItemBackInInventory(new ItemStack(Items.EMERALD, quest.emeraldReward()));
                player.sendSystemMessage(Component.translatable("message.valet_quest.completed", quest.emeraldReward()));
            }
        }
        sendState(player);
    }

    private static int countRequestedItems(ServerPlayer player, ValetQuest quest) {
        Inventory inventory = player.getInventory();
        int count = 0;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(quest.requestedItem())) count += stack.getCount();
        }
        return count;
    }

    private static void removeRequestedItems(ServerPlayer player, ValetQuest quest, int amount) {
        Inventory inventory = player.getInventory();
        int remaining = amount;
        for (int slot = 0; slot < inventory.getContainerSize() && remaining > 0; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.is(quest.requestedItem())) continue;
            int removed = Math.min(remaining, stack.getCount());
            stack.shrink(removed);
            remaining -= removed;
        }
        inventory.setChanged();
    }

    private static BlockPos findBell(ServerLevel world, BlockPos origin) {
        for (BlockPos pos : BlockPos.withinManhattan(origin, VILLAGE_RADIUS, 12, VILLAGE_RADIUS)) {
            if (world.getBlockState(pos).is(Blocks.BELL)) return pos.immutable();
        }
        return null;
    }

    private static void ensureMayor(ServerLevel world, BlockPos bell) {
        ValetMayorState state = ValetMayorState.get(world);
        List<Villager> loadedMayors = findLoadedMayors(world);
        Villager mayor = chooseCanonicalMayor(loadedMayors, state.mayorUuid(), bell);
        if (mayor != null) {
            state.setMayor(mayor.getUUID(), state.bell() == null ? bell : state.bell());
            removeDuplicates(loadedMayors, mayor);
            configureMayor(mayor);
            return;
        }

        if (state.mayorUuid() != null) {
            BlockPos registeredBell = state.bell();
            if (registeredBell == null || registeredBell.distSqr(bell) > VILLAGE_RADIUS * VILLAGE_RADIUS) {
                return;
            }
            state.clearMayor();
        }

        AABB area = new AABB(bell).inflate(VILLAGE_RADIUS, 16, VILLAGE_RADIUS);
        if (world.getEntitiesOfClass(Cat.class, area).isEmpty() || world.getEntitiesOfClass(IronGolem.class, area).isEmpty()) return;
        mayor = EntityTypes.VILLAGER.spawn(world, bell.above(), EntitySpawnReason.EVENT);
        if (mayor == null) return;
        mayor.addTag(MAYOR_TAG);
        configureMayor(mayor);
        state.setMayor(mayor.getUUID(), bell);
        ValetMod.LOGGER.info("Mayor spawned at {} uuid={}", bell, mayor.getUUID());
    }

    private static List<Villager> findLoadedMayors(ServerLevel world) {
        List<Villager> mayors = new ArrayList<>();
        for (Entity entity : world.getAllEntities()) {
            if (entity instanceof Villager villager && isMayor(villager) && !villager.isRemoved()) {
                mayors.add(villager);
            }
        }
        return mayors;
    }

    private static Villager chooseCanonicalMayor(List<Villager> mayors, UUID registeredUuid, BlockPos bell) {
        if (registeredUuid != null) {
            for (Villager mayor : mayors) {
                if (registeredUuid.equals(mayor.getUUID())) {
                    return mayor;
                }
            }
        }
        return mayors.stream()
                .min(Comparator.comparingDouble((Villager mayor) -> mayor.distanceToSqr(
                                bell.getX() + 0.5D,
                                bell.getY() + 0.5D,
                                bell.getZ() + 0.5D
                        ))
                        .thenComparing(mayor -> mayor.getUUID().toString()))
                .orElse(null);
    }

    private static void removeDuplicates(List<Villager> mayors, Villager canonical) {
        for (Villager duplicate : mayors) {
            if (duplicate == canonical) continue;
            ValetMod.LOGGER.warn("Removing duplicate mayor uuid={} kept={}", duplicate.getUUID(), canonical.getUUID());
            duplicate.discard();
        }
    }

    private static void configureMayor(Villager mayor) {
        mayor.addTag(MAYOR_TAG);
        mayor.setCustomName(Component.translatable("entity.valet.mayor"));
        mayor.setCustomNameVisible(true);
        mayor.setPersistenceRequired();
        mayor.setNoAi(true);
        if (!mayor.getItemBySlot(EquipmentSlot.MAINHAND).is(Items.TRIDENT)) {
            mayor.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.TRIDENT));
        }
        mayor.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
    }

    public static boolean isMayor(Villager villager) { return villager.entityTags().contains(MAYOR_TAG); }

    private static boolean hasMayorNearby(ServerLevel world, BlockPos pos) {
        return !world.getEntitiesOfClass(Villager.class, new AABB(pos).inflate(MAYOR_ACCESS_RADIUS), ValetMayorManager::isMayor).isEmpty();
    }

    private static String activeTag(ValetQuest quest) { return "valet_quest_active_" + quest.id(); }
    private static String doneTag(ValetQuest quest) { return "valet_quest_done_" + quest.id(); }
}
