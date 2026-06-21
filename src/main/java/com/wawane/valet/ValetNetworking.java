package com.wawane.valet;

import com.wawane.valet.gui.ValetOrdersScreenHandler;
import com.wawane.valet.construction.ConstructionBlueprintBlockEntity;
import com.wawane.valet.construction.ValetConstructionBlueprint;
import com.wawane.valet.construction.ValetConstructionStorage;
import com.wawane.valet.order.ValetMineTarget;
import com.wawane.valet.order.ValetOrder;
import com.wawane.valet.order.ValetOrders;
import com.wawane.valet.order.ValetMiningScanner;
import com.wawane.valet.order.ValetWoodTarget;
import com.wawane.valet.progress.ValetPerk;
import com.wawane.valet.progress.ValetProgress;
import com.wawane.valet.network.packets.ChoosePerkPayload;
import com.wawane.valet.network.packets.DeleteConstructionPayload;
import com.wawane.valet.network.packets.RenameValetPayload;
import com.wawane.valet.network.packets.SetOrderPayload;
import com.wawane.valet.network.packets.ValetStatePayload;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.World;

import java.util.OptionalInt;
import java.util.List;

public final class ValetNetworking {
    public static final Identifier SET_ORDER_PACKET_ID = ValetMod.id("set_order");
    public static final Identifier CHOOSE_PERK_PACKET_ID = ValetMod.id("choose_perk");
    public static final Identifier RENAME_PACKET_ID = ValetMod.id("rename_valet");
    public static final Identifier DELETE_CONSTRUCTION_PACKET_ID = ValetMod.id("delete_construction");
    public static final Identifier VALET_STATE_PACKET_ID = ValetMod.id("valet_state");
    private static final int GLOW_TICKS = 20 * 60 * 30;

    private ValetNetworking() {
    }

    public static void registerServerReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(SET_ORDER_PACKET_ID, ValetNetworking::setValetOrder);
        ServerPlayNetworking.registerGlobalReceiver(CHOOSE_PERK_PACKET_ID, ValetNetworking::chooseValetPerk);
        ServerPlayNetworking.registerGlobalReceiver(RENAME_PACKET_ID, ValetNetworking::renameValet);
        ServerPlayNetworking.registerGlobalReceiver(DELETE_CONSTRUCTION_PACKET_ID, ValetNetworking::deleteConstruction);
    }

    public static ActionResult openValetOrders(PlayerEntity player, World world, Hand hand, Entity entity, EntityHitResult hitResult) {
        if (hand != Hand.MAIN_HAND || !(entity instanceof VillagerEntity villager)) {
            return ActionResult.PASS;
        }

        if (villager.getVillagerData().getProfession() != ValetMod.VALET_PROFESSION) {
            if (world instanceof ServerWorld serverWorld && ValetMod.tryAssignValetJob(serverWorld, villager, player.getBlockPos())) {
                return openValetOrders(player, world, hand, entity, hitResult);
            }
            return ActionResult.PASS;
        }

        if (!world.isClient && player instanceof ServerPlayerEntity serverPlayer) {
            villager.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, GLOW_TICKS, 0, false, false));
            if (villager.hasCustomName()) {
                villager.setCustomNameVisible(true);
            }
            ValetHome.getOrRecover(serverPlayer.getServerWorld(), villager, serverPlayer.getBlockPos());
            ValetConversations.begin(villager);
            int[] oreCounts = ValetMiningScanner.countNearbyOres(serverPlayer.getServerWorld(), villager);
            int[] woodCounts = ValetMiningScanner.countNearbyWood(serverPlayer.getServerWorld(), villager);
            List<ValetConstructionBlueprint> constructions = ValetConstructionStorage.get(serverPlayer.getServerWorld()).getBlueprints();
            OptionalInt openedScreen = serverPlayer.openHandledScreen(new ExtendedScreenHandlerFactory() {
                @Override
                public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
                    buf.writeInt(villager.getId());
                    buf.writeUuid(villager.getUuid());
                    buf.writeIdentifier(serverPlayer.getServerWorld().getRegistryKey().getValue());
                    buf.writeInt(ValetOrders.get(villager).ordinal());
                    buf.writeInt(getCurrentMineTargetIndex(villager));
                    buf.writeInt(getCurrentWoodTargetIndex(villager));
                    buf.writeInt(ValetOrders.getConstructionTargetId(villager));
                    for (int count : oreCounts) {
                        buf.writeInt(count);
                    }
                    for (int count : woodCounts) {
                        buf.writeInt(count);
                    }
                    writeConstructions(constructions, buf);
                    writeValetProgress(villager, buf);
                }

                @Override
                public Text getDisplayName() {
                    return Text.translatable("screen.valet.orders");
                }

                @Override
                public ScreenHandler createMenu(int syncId, PlayerInventory inventory, PlayerEntity player) {
                    return new ValetOrdersScreenHandler(
                            syncId,
                            inventory,
                            villager.getId(),
                            villager.getUuid(),
                            serverPlayer.getServerWorld().getRegistryKey().getValue(),
                            ValetOrders.get(villager).ordinal(),
                            getCurrentMineTargetIndex(villager),
                            getCurrentWoodTargetIndex(villager),
                            ValetOrders.getConstructionTargetId(villager),
                            oreCounts,
                            woodCounts,
                            constructions,
                            ValetProgress.getLevel(villager),
                            ValetProgress.getXp(villager),
                            ValetProgress.getNextLevelXp(villager),
                            ValetProgress.getPendingPerks(villager),
                            ValetProgress.getPerks(villager),
                            getValetName(villager)
                    );
                }
            });
            if (!openedScreen.isPresent()) {
                ValetConversations.end(villager);
            }
        }

        return ActionResult.SUCCESS;
    }

    private static int getCurrentMineTargetIndex(VillagerEntity villager) {
        ValetMineTarget target = ValetOrders.getMineTarget(villager);
        return target == null ? -1 : target.ordinal();
    }

    private static int getCurrentWoodTargetIndex(VillagerEntity villager) {
        ValetWoodTarget target = ValetOrders.getWoodTarget(villager);
        return target == null ? -1 : target.ordinal();
    }

    private static void writeValetProgress(VillagerEntity villager, PacketByteBuf buf) {
        buf.writeInt(ValetProgress.getLevel(villager));
        buf.writeInt(ValetProgress.getXp(villager));
        buf.writeInt(ValetProgress.getNextLevelXp(villager));
        buf.writeInt(ValetProgress.getPendingPerks(villager));
        for (boolean perk : ValetProgress.getPerks(villager)) {
            buf.writeBoolean(perk);
        }
        buf.writeString(getValetName(villager), 32);
    }

    private static void sendValetState(ServerPlayerEntity player, VillagerEntity villager) {
        PacketByteBuf buf = PacketByteBufs.create();
        ValetStatePayload.from(villager).write(buf);
        ServerPlayNetworking.send(player, VALET_STATE_PACKET_ID, buf);
    }

    private static void writeConstructions(List<ValetConstructionBlueprint> blueprints, PacketByteBuf buf) {
        buf.writeInt(blueprints.size());
        for (ValetConstructionBlueprint blueprint : blueprints) {
            buf.writeNbt(blueprint.writeNbt());
        }
    }

    private static void setValetOrder(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PacketByteBuf buf, PacketSender sender) {
        SetOrderPayload payload = SetOrderPayload.read(buf);
        server.execute(() -> {
            Entity entity = player.getWorld().getEntityById(payload.valetEntityId());
            if (!(entity instanceof VillagerEntity villager)) {
                return;
            }

            if (villager.getVillagerData().getProfession() != ValetMod.VALET_PROFESSION || player.squaredDistanceTo(villager) > 64.0D) {
                return;
            }

            ValetOrder order = payload.order();
            int targetIndex = payload.targetIndex();
            ValetHome.getOrRecover(player.getServerWorld(), villager, player.getBlockPos());
            if (order == ValetOrder.NONE) {
                ValetOrders.set(villager, ValetOrder.NONE);
                ValetMod.LOGGER.info("Valet {} order set to none", villager.getUuid());
                player.sendMessage(Text.translatable("message.valet.order_set", Text.translatable("order.valet.none")), true);
                finishOrderInteraction(player, villager);
                sendValetState(player, villager);
                return;
            }

            if (order == ValetOrder.MINE_ORES) {
                ValetMineTarget target = ValetMineTarget.fromIndex(targetIndex);
                if (target == null) {
                    sendValetState(player, villager);
                    return;
                }

                int[] counts = ValetMiningScanner.countNearbyOres(player.getServerWorld(), villager);
                if (counts[target.ordinal()] <= 0) {
                    player.sendMessage(Text.translatable("message.valet.no_target"), true);
                    sendValetState(player, villager);
                    return;
                }

                ValetOrders.setMineTarget(villager, target);
                ValetMod.LOGGER.info("Valet {} order set to mine {}", villager.getUuid(), target.name());
                player.sendMessage(Text.translatable("message.valet.mine_target_set", Text.translatable(target.getTranslationKey())), true);
                finishOrderInteraction(player, villager);
                sendValetState(player, villager);
                return;
            }

            if (order == ValetOrder.CHOP_WOOD) {
                ValetWoodTarget target = ValetWoodTarget.fromIndex(targetIndex);
                if (target == null) {
                    sendValetState(player, villager);
                    return;
                }

                int[] counts = ValetMiningScanner.countNearbyWood(player.getServerWorld(), villager);
                if (counts[target.ordinal()] <= 0) {
                    player.sendMessage(Text.translatable("message.valet.no_target"), true);
                    sendValetState(player, villager);
                    return;
                }

                ValetOrders.setWoodTarget(villager, target);
                ValetMod.LOGGER.info("Valet {} order set to chop {}", villager.getUuid(), target.name());
                player.sendMessage(Text.translatable("message.valet.wood_target_set", Text.translatable(target.getTranslationKey())), true);
                finishOrderInteraction(player, villager);
                sendValetState(player, villager);
                return;
            }

            if (order == ValetOrder.BUILD_STRUCTURE) {
                ValetConstructionBlueprint blueprint = ValetConstructionStorage.get(player.getServerWorld()).getBlueprint(targetIndex);
                if (blueprint == null) {
                    player.sendMessage(Text.translatable("message.valet.no_construction"), true);
                    sendValetState(player, villager);
                    return;
                }

                ValetOrders.setConstructionTarget(villager, targetIndex);
                ValetMod.LOGGER.info("Valet {} order set to build {}", villager.getUuid(), targetIndex);
                giveBlueprintItem(player, villager, blueprint);
                player.sendMessage(Text.translatable("message.valet.construction_target_set", blueprint.name()), true);
                finishOrderInteraction(player, villager);
                sendValetState(player, villager);
                return;
            }
            sendValetState(player, villager);
        });
    }

    private static void finishOrderInteraction(ServerPlayerEntity player, VillagerEntity villager) {
        ValetConversations.end(villager);
        player.closeHandledScreen();
    }

    private static void giveBlueprintItem(ServerPlayerEntity player, VillagerEntity villager, ValetConstructionBlueprint blueprint) {
        ItemStack stack = new ItemStack(ValetMod.CONSTRUCTION_BLUEPRINT_ITEM);
        stack.getOrCreateNbt().putInt(ConstructionBlueprintBlockEntity.CONSTRUCTION_ID_KEY, blueprint.id());
        stack.getOrCreateNbt().putString(ConstructionBlueprintBlockEntity.CONSTRUCTION_NAME_KEY, blueprint.name());
        stack.getOrCreateNbt().putUuid(ConstructionBlueprintBlockEntity.VALET_UUID_KEY, villager.getUuid());
        stack.getOrCreateNbt().put(ConstructionBlueprintBlockEntity.BLUEPRINT_KEY, blueprint.writeNbt());
        if (!player.getInventory().insertStack(stack)) {
            player.dropItem(stack, false);
        }
    }

    private static void deleteConstruction(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PacketByteBuf buf, PacketSender sender) {
        DeleteConstructionPayload payload = DeleteConstructionPayload.read(buf);
        server.execute(() -> {
            Entity entity = player.getWorld().getEntityById(payload.valetEntityId());
            if (!(entity instanceof VillagerEntity villager)) {
                return;
            }

            if (villager.getVillagerData().getProfession() != ValetMod.VALET_PROFESSION || player.squaredDistanceTo(villager) > 64.0D) {
                return;
            }

            int constructionId = payload.constructionId();
            boolean removed = ValetConstructionStorage.get(player.getServerWorld()).removeBlueprint(constructionId);
            if (!removed) {
                player.sendMessage(Text.translatable("message.valet.no_construction"), true);
                sendValetState(player, villager);
                return;
            }

            if (ValetOrders.getConstructionTargetId(villager) == constructionId) {
                ValetOrders.set(villager, ValetOrder.NONE);
            }
            removeBlueprintItems(player, constructionId);
            player.sendMessage(Text.translatable("message.valet.construction_deleted"), true);
            sendValetState(player, villager);
        });
    }

    private static void removeBlueprintItems(ServerPlayerEntity player, int constructionId) {
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isOf(ValetMod.CONSTRUCTION_BLUEPRINT_ITEM)
                    && stack.getNbt() != null
                    && stack.getNbt().contains(ConstructionBlueprintBlockEntity.CONSTRUCTION_ID_KEY)
                    && stack.getNbt().getInt(ConstructionBlueprintBlockEntity.CONSTRUCTION_ID_KEY) == constructionId) {
                player.getInventory().setStack(i, ItemStack.EMPTY);
            }
        }
        player.getInventory().markDirty();
    }

    private static void chooseValetPerk(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PacketByteBuf buf, PacketSender sender) {
        ChoosePerkPayload payload = ChoosePerkPayload.read(buf);
        server.execute(() -> {
            Entity entity = player.getWorld().getEntityById(payload.valetEntityId());
            if (!(entity instanceof VillagerEntity villager)) {
                return;
            }

            if (villager.getVillagerData().getProfession() != ValetMod.VALET_PROFESSION || player.squaredDistanceTo(villager) > 64.0D) {
                return;
            }

            ValetPerk perk = payload.perk();
            if (ValetProgress.choosePerk(villager, perk)) {
                player.sendMessage(Text.translatable("message.valet.perk_set", Text.translatable(perk.getTranslationKey())), true);
            }
            sendValetState(player, villager);
        });
    }

    private static void renameValet(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PacketByteBuf buf, PacketSender sender) {
        RenameValetPayload payload = RenameValetPayload.read(buf);
        String name = cleanName(payload.name());
        server.execute(() -> {
            Entity entity = player.getWorld().getEntityById(payload.valetEntityId());
            if (!(entity instanceof VillagerEntity villager)) {
                return;
            }

            if (villager.getVillagerData().getProfession() != ValetMod.VALET_PROFESSION || player.squaredDistanceTo(villager) > 64.0D) {
                return;
            }

            if (name.isEmpty()) {
                villager.setCustomName(null);
                villager.setCustomNameVisible(false);
            } else {
                villager.setCustomName(Text.literal(name));
                villager.setCustomNameVisible(true);
            }
            sendValetState(player, villager);
        });
    }

    private static String getValetName(VillagerEntity villager) {
        return villager.hasCustomName() && villager.getCustomName() != null ? villager.getCustomName().getString() : "";
    }

    private static String cleanName(String name) {
        return name.trim().replaceAll("\\s+", " ");
    }
}
