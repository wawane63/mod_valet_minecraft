package com.wawane.valet;

import com.wawane.valet.gui.ValetOrdersScreenHandler;
import com.wawane.valet.ai.ValetWorkGoal;
import com.wawane.valet.construction.ConstructionBlueprintBlockEntity;
import com.wawane.valet.construction.ConstructionBlueprintNbt;
import com.wawane.valet.construction.ValetConstructionBlueprint;
import com.wawane.valet.construction.ValetConstructionStorage;
import com.wawane.valet.farm.ValetFarmArea;
import com.wawane.valet.farm.ValetFarmStorage;
import com.wawane.valet.order.ValetMineTarget;
import com.wawane.valet.order.ValetOrder;
import com.wawane.valet.order.ValetOrders;
import com.wawane.valet.order.ValetCraftTarget;
import com.wawane.valet.order.ValetMiningScanner;
import com.wawane.valet.order.ValetWoodTarget;
import com.wawane.valet.progress.ValetPerk;
import com.wawane.valet.progress.ValetCombatPerk;
import com.wawane.valet.progress.ValetCombatProgress;
import com.wawane.valet.progress.ValetCombatSkillTree;
import com.wawane.valet.progress.ValetProgress;
import com.wawane.valet.network.packets.ChooseCombatPerkPayload;
import com.wawane.valet.network.packets.ChoosePerkPayload;
import com.wawane.valet.network.packets.DeleteConstructionPayload;
import com.wawane.valet.network.packets.RenameValetPayload;
import com.wawane.valet.network.packets.SetFarmOrderPayload;
import com.wawane.valet.network.packets.SetOrderPayload;
import com.wawane.valet.network.packets.ValetStatePayload;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import java.util.OptionalInt;
import java.util.List;

public final class ValetNetworking {
    public static final Identifier SET_ORDER_PACKET_ID = ValetMod.id("set_order");
    public static final Identifier CHOOSE_PERK_PACKET_ID = ValetMod.id("choose_perk");
    public static final Identifier CHOOSE_COMBAT_PERK_PACKET_ID = ValetMod.id("choose_combat_perk");
    public static final Identifier RENAME_PACKET_ID = ValetMod.id("rename_valet");
    public static final Identifier DELETE_CONSTRUCTION_PACKET_ID = ValetMod.id("delete_construction");
    public static final Identifier VALET_STATE_PACKET_ID = ValetMod.id("valet_state");
    private static final int GLOW_TICKS = 20 * 60 * 30;
    private static boolean payloadTypesRegistered;

    private ValetNetworking() {
    }

    public static void registerServerReceivers() {
        registerPayloadTypes();
        ServerPlayNetworking.registerGlobalReceiver(SetOrderPayload.TYPE, ValetNetworking::setValetOrder);
        ServerPlayNetworking.registerGlobalReceiver(SetFarmOrderPayload.TYPE, ValetNetworking::setFarmOrder);
        ServerPlayNetworking.registerGlobalReceiver(ChoosePerkPayload.TYPE, ValetNetworking::chooseValetPerk);
        ServerPlayNetworking.registerGlobalReceiver(ChooseCombatPerkPayload.TYPE, ValetNetworking::chooseValetCombatPerk);
        ServerPlayNetworking.registerGlobalReceiver(RenameValetPayload.TYPE, ValetNetworking::renameValet);
        ServerPlayNetworking.registerGlobalReceiver(DeleteConstructionPayload.TYPE, ValetNetworking::deleteConstruction);
    }

    public static void registerPayloadTypes() {
        if (payloadTypesRegistered) {
            return;
        }
        payloadTypesRegistered = true;
        PayloadTypeRegistry.serverboundPlay().register(SetOrderPayload.TYPE, SetOrderPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(SetFarmOrderPayload.TYPE, SetFarmOrderPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ChoosePerkPayload.TYPE, ChoosePerkPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ChooseCombatPerkPayload.TYPE, ChooseCombatPerkPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(RenameValetPayload.TYPE, RenameValetPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(DeleteConstructionPayload.TYPE, DeleteConstructionPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ValetStatePayload.TYPE, ValetStatePayload.CODEC);
    }

    public static InteractionResult openValetOrders(Player player, Level world, InteractionHand hand, Entity entity, EntityHitResult hitResult) {
        if (hand != InteractionHand.MAIN_HAND || !(entity instanceof Villager villager)) {
            return InteractionResult.PASS;
        }

        if (!ValetMod.isValet(villager)) {
            if (world instanceof ServerLevel serverWorld && ValetMod.tryAssignValetJob(serverWorld, villager, player.blockPosition())) {
                return openValetOrders(player, world, hand, entity, hitResult);
            }
            return InteractionResult.PASS;
        }

        if (!world.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            villager.addEffect(new MobEffectInstance(MobEffects.GLOWING, GLOW_TICKS, 0, false, false));
            if (villager.hasCustomName()) {
                villager.setCustomNameVisible(true);
            }
            ValetHome.getOrRecover(serverPlayer.level(), villager, serverPlayer.blockPosition());
            ValetConversations.begin(villager);
            int[] oreCounts = ValetMiningScanner.countNearbyOres(serverPlayer.level(), villager);
            int[] woodCounts = ValetMiningScanner.countNearbyWood(serverPlayer.level(), villager);
            List<ValetFarmArea> farmAreas = ValetFarmStorage.get(serverPlayer.level()).getAreas();
            List<ValetConstructionBlueprint> constructions = ValetConstructionStorage.get(serverPlayer.level()).getBlueprints();
            OptionalInt openedScreen = serverPlayer.openMenu(new ExtendedMenuProvider<ValetOrdersScreenHandler.OpeningData>() {
                @Override
                public ValetOrdersScreenHandler.OpeningData getScreenOpeningData(ServerPlayer player) {
                    return ValetOrdersScreenHandler.OpeningData.create(serverPlayer.registryAccess(), buf -> {
                        buf.writeInt(villager.getId());
                        buf.writeUUID(villager.getUUID());
                        buf.writeIdentifier(serverPlayer.level().dimension().identifier());
                        buf.writeInt(ValetOrders.get(villager).ordinal());
                        buf.writeInt(getCurrentMineTargetIndex(villager));
                        buf.writeInt(getCurrentWoodTargetIndex(villager));
                        buf.writeInt(ValetOrders.getFarmAreaId(villager));
                        buf.writeInt(ValetOrders.getFarmCropMask(villager));
                        buf.writeBoolean(ValetOrders.shouldReplantFarm(villager));
                        buf.writeBoolean(ValetOrders.shouldTillFarm(villager));
                        buf.writeInt(ValetOrders.getConstructionTargetId(villager));
                        buf.writeInt(getCurrentCraftTargetIndex(villager));
                        for (int count : oreCounts) {
                            buf.writeInt(count);
                        }
                        for (int count : woodCounts) {
                            buf.writeInt(count);
                        }
                        writeFarmAreas(farmAreas, buf);
                        writeConstructions(constructions, buf);
                        writeValetInventory(villager.getInventory(), buf);
                        writeValetProgress(villager, buf);
                    });
                }

                @Override
                public Component getDisplayName() {
                    return Component.translatable("screen.valet.orders");
                }

                @Override
                public AbstractContainerMenu createMenu(int syncId, Inventory inventory, Player player) {
                    return new ValetOrdersScreenHandler(
                            syncId,
                            inventory,
                            villager.getId(),
                            villager.getUUID(),
                            serverPlayer.level().dimension().identifier(),
                            ValetOrders.get(villager).ordinal(),
                            getCurrentMineTargetIndex(villager),
                            getCurrentWoodTargetIndex(villager),
                            ValetOrders.getFarmAreaId(villager),
                            ValetOrders.getFarmCropMask(villager),
                            ValetOrders.shouldReplantFarm(villager),
                            ValetOrders.shouldTillFarm(villager),
                            ValetOrders.getConstructionTargetId(villager),
                            getCurrentCraftTargetIndex(villager),
                            oreCounts,
                            woodCounts,
                            farmAreas,
                            constructions,
                            copyInventory(villager.getInventory()),
                            ValetProgress.getLevel(villager),
                            ValetProgress.getXp(villager),
                            ValetProgress.getNextLevelXp(villager),
                            ValetProgress.getPendingPerks(villager),
                            ValetProgress.getPerks(villager),
                            ValetCombatProgress.getPerks(villager),
                            ValetCombatProgress.getLevel(villager, ValetCombatSkillTree.SWORD),
                            ValetCombatProgress.getXp(villager, ValetCombatSkillTree.SWORD),
                            ValetCombatProgress.getNextLevelXp(villager, ValetCombatSkillTree.SWORD),
                            ValetCombatProgress.getPendingPerks(villager, ValetCombatSkillTree.SWORD),
                            ValetCombatProgress.getLevel(villager, ValetCombatSkillTree.BOW),
                            ValetCombatProgress.getXp(villager, ValetCombatSkillTree.BOW),
                            ValetCombatProgress.getNextLevelXp(villager, ValetCombatSkillTree.BOW),
                            ValetCombatProgress.getPendingPerks(villager, ValetCombatSkillTree.BOW),
                            ValetCombatProgress.hasPerk(villager, ValetCombatPerk.ALLY_AWARENESS),
                            getValetName(villager)
                    );
                }
            });
            if (!openedScreen.isPresent()) {
                ValetConversations.end(villager);
            }
        }

        return InteractionResult.SUCCESS;
    }

    private static int getCurrentMineTargetIndex(Villager villager) {
        ValetMineTarget target = ValetOrders.getMineTarget(villager);
        return target == null ? -1 : target.ordinal();
    }

    private static int getCurrentWoodTargetIndex(Villager villager) {
        ValetWoodTarget target = ValetOrders.getWoodTarget(villager);
        return target == null ? -1 : target.ordinal();
    }

    private static int getCurrentCraftTargetIndex(Villager villager) {
        ValetCraftTarget target = ValetOrders.getCraftTarget(villager);
        return target == null ? -1 : target.ordinal();
    }

    private static void writeValetProgress(Villager villager, RegistryFriendlyByteBuf buf) {
        buf.writeInt(ValetProgress.getLevel(villager));
        buf.writeInt(ValetProgress.getXp(villager));
        buf.writeInt(ValetProgress.getNextLevelXp(villager));
        buf.writeInt(ValetProgress.getPendingPerks(villager));
        for (boolean perk : ValetProgress.getPerks(villager)) {
            buf.writeBoolean(perk);
        }
        for (boolean perk : ValetCombatProgress.getPerks(villager)) {
            buf.writeBoolean(perk);
        }
        writeCombatSkill(villager, ValetCombatSkillTree.SWORD, buf);
        writeCombatSkill(villager, ValetCombatSkillTree.BOW, buf);
        buf.writeBoolean(ValetCombatProgress.hasPerk(villager, ValetCombatPerk.ALLY_AWARENESS));
        buf.writeUtf(getValetName(villager), 32);
    }

    private static void writeCombatSkill(Villager villager, ValetCombatSkillTree tree, RegistryFriendlyByteBuf buf) {
        buf.writeInt(ValetCombatProgress.getLevel(villager, tree));
        buf.writeInt(ValetCombatProgress.getXp(villager, tree));
        buf.writeInt(ValetCombatProgress.getNextLevelXp(villager, tree));
        buf.writeInt(ValetCombatProgress.getPendingPerks(villager, tree));
    }

    private static void sendValetState(ServerPlayer player, Villager villager) {
        ServerPlayNetworking.send(player, ValetStatePayload.from(player.level(), villager));
    }

    private static void writeConstructions(List<ValetConstructionBlueprint> blueprints, RegistryFriendlyByteBuf buf) {
        buf.writeInt(blueprints.size());
        for (ValetConstructionBlueprint blueprint : blueprints) {
            buf.writeNbt(blueprint.writeNbt());
        }
    }

    private static void writeFarmAreas(List<ValetFarmArea> farmAreas, RegistryFriendlyByteBuf buf) {
        buf.writeInt(farmAreas.size());
        for (ValetFarmArea area : farmAreas) {
            buf.writeNbt(area.writeNbt());
        }
    }

    private static void writeValetInventory(Container inventory, RegistryFriendlyByteBuf buf) {
        buf.writeInt(inventory.getContainerSize());
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, inventory.getItem(slot));
        }
    }

    private static List<ItemStack> copyInventory(Container inventory) {
        List<ItemStack> result = new java.util.ArrayList<>(inventory.getContainerSize());
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            result.add(inventory.getItem(slot).copy());
        }
        return result;
    }

    private static void setValetOrder(SetOrderPayload payload, ServerPlayNetworking.Context context) {
        MinecraftServer server = context.server();
        ServerPlayer player = context.player();
        server.execute(() -> {
            Entity entity = player.level().getEntity(payload.valetEntityId());
            if (!(entity instanceof Villager villager)) {
                return;
            }

            if (!isValidValetInteraction(player, villager)) {
                return;
            }

            ValetOrder order = payload.order();
            int targetIndex = payload.targetIndex();
            ValetHome.getOrRecover(player.level(), villager, player.blockPosition());
            if (order == ValetOrder.NONE) {
                ValetOrders.set(villager, ValetOrder.NONE);
                ValetWorkGoal.requestRestart(villager);
                ValetMod.LOGGER.info("Valet {} order set to none", villager.getUUID());
                player.sendOverlayMessage(Component.translatable("message.valet.order_set", Component.translatable("order.valet.none")));
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

                int[] counts = ValetMiningScanner.countNearbyOres(player.level(), villager);
                if (counts[target.ordinal()] <= 0) {
                    player.sendOverlayMessage(Component.translatable("message.valet.no_target"));
                    sendValetState(player, villager);
                    return;
                }

                ValetOrders.setMineTarget(villager, target);
                ValetWorkGoal.requestRestart(villager);
                ValetMod.LOGGER.info("Valet {} order set to mine {}", villager.getUUID(), target.name());
                player.sendOverlayMessage(Component.translatable("message.valet.mine_target_set", Component.translatable(target.getTranslationKey())));
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

                int[] counts = ValetMiningScanner.countNearbyWood(player.level(), villager);
                if (counts[target.ordinal()] <= 0) {
                    player.sendOverlayMessage(Component.translatable("message.valet.no_target"));
                    sendValetState(player, villager);
                    return;
                }

                ValetOrders.setWoodTarget(villager, target);
                ValetWorkGoal.requestRestart(villager);
                ValetMod.LOGGER.info("Valet {} order set to chop {}", villager.getUUID(), target.name());
                player.sendOverlayMessage(Component.translatable("message.valet.wood_target_set", Component.translatable(target.getTranslationKey())));
                finishOrderInteraction(player, villager);
                sendValetState(player, villager);
                return;
            }

            if (order == ValetOrder.BUILD_STRUCTURE) {
                ValetConstructionBlueprint blueprint = ValetConstructionStorage.get(player.level()).getBlueprint(targetIndex);
                if (blueprint == null) {
                    player.sendOverlayMessage(Component.translatable("message.valet.no_construction"));
                    sendValetState(player, villager);
                    return;
                }

                ValetOrders.setConstructionTarget(villager, targetIndex);
                ValetWorkGoal.requestRestart(villager);
                ValetMod.LOGGER.info("Valet {} order set to build {}", villager.getUUID(), targetIndex);
                giveBlueprintItem(player, villager, blueprint);
                player.sendOverlayMessage(Component.translatable("message.valet.construction_target_set", blueprint.name()));
                finishOrderInteraction(player, villager);
                sendValetState(player, villager);
                return;
            }

            if (order == ValetOrder.CRAFT) {
                ValetCraftTarget target = ValetCraftTarget.fromIndex(targetIndex);
                if (target == null) {
                    sendValetState(player, villager);
                    return;
                }

                ValetOrders.setCraftTarget(villager, target);
                ValetWorkGoal.requestRestart(villager);
                ValetMod.LOGGER.info("Valet {} order set to craft {}", villager.getUUID(), target.name());
                player.sendOverlayMessage(Component.translatable("message.valet.craft_target_set", Component.translatable(target.getTranslationKey())));
                finishOrderInteraction(player, villager);
                sendValetState(player, villager);
                return;
            }
            sendValetState(player, villager);
        });
    }

    private static void setFarmOrder(SetFarmOrderPayload payload, ServerPlayNetworking.Context context) {
        MinecraftServer server = context.server();
        ServerPlayer player = context.player();
        server.execute(() -> {
            Entity entity = player.level().getEntity(payload.valetEntityId());
            if (!(entity instanceof Villager villager)) {
                return;
            }

            if (!isValidValetInteraction(player, villager)) {
                return;
            }

            int farmAreaId = payload.farmAreaId();
            ValetFarmArea area = farmAreaId < 0 ? null : ValetFarmStorage.get(player.level()).getArea(farmAreaId);
            if (farmAreaId >= 0 && area == null) {
                player.sendOverlayMessage(Component.translatable("message.valet.no_farm_area"));
                sendValetState(player, villager);
                return;
            }

            ValetHome.getOrRecover(player.level(), villager, player.blockPosition());
            ValetOrders.setHarvestCrops(villager, farmAreaId, payload.cropMask(), payload.replant(), payload.tillSoil());
            ValetWorkGoal.requestRestart(villager);
            ValetMod.LOGGER.info("Valet {} order set to farm area={} crops={} replant={} till={}", villager.getUUID(), farmAreaId, payload.cropMask(), payload.replant(), payload.tillSoil());
            Component target = area == null ? Component.translatable("screen.valet.farm_all") : Component.literal(area.name());
            player.sendOverlayMessage(Component.translatable("message.valet.farm_target_set", target));
            if (payload.closeScreen()) {
                finishOrderInteraction(player, villager);
            }
            sendValetState(player, villager);
        });
    }

    private static void finishOrderInteraction(ServerPlayer player, Villager villager) {
        ValetConversations.end(villager);
        player.closeContainer();
    }

    private static void giveBlueprintItem(ServerPlayer player, Villager villager, ValetConstructionBlueprint blueprint) {
        ItemStack stack = new ItemStack(ValetMod.CONSTRUCTION_BLUEPRINT_ITEM);
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        tag.putInt(ConstructionBlueprintBlockEntity.CONSTRUCTION_ID_KEY, blueprint.id());
        tag.putString(ConstructionBlueprintBlockEntity.CONSTRUCTION_NAME_KEY, blueprint.name());
        tag.putString(ConstructionBlueprintBlockEntity.VALET_UUID_KEY, villager.getUUID().toString());
        tag.put(ConstructionBlueprintBlockEntity.BLUEPRINT_KEY, blueprint.writeNbt());
        ConstructionBlueprintNbt.set(stack, tag);
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    private static void deleteConstruction(DeleteConstructionPayload payload, ServerPlayNetworking.Context context) {
        MinecraftServer server = context.server();
        ServerPlayer player = context.player();
        server.execute(() -> {
            Entity entity = player.level().getEntity(payload.valetEntityId());
            if (!(entity instanceof Villager villager)) {
                return;
            }

            if (!isValidValetInteraction(player, villager)) {
                return;
            }

            int constructionId = payload.constructionId();
            boolean removed = ValetConstructionStorage.get(player.level()).removeBlueprint(constructionId);
            if (!removed) {
                player.sendOverlayMessage(Component.translatable("message.valet.no_construction"));
                sendValetState(player, villager);
                return;
            }

            if (ValetOrders.getConstructionTargetId(villager) == constructionId) {
                ValetOrders.set(villager, ValetOrder.NONE);
            }
            removeBlueprintItems(player, constructionId);
            player.sendOverlayMessage(Component.translatable("message.valet.construction_deleted"));
            sendValetState(player, villager);
        });
    }

    private static void removeBlueprintItems(ServerPlayer player, int constructionId) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            net.minecraft.nbt.CompoundTag tag = ConstructionBlueprintNbt.get(stack);
            if (stack.is(ValetMod.CONSTRUCTION_BLUEPRINT_ITEM)
                    && tag != null
                    && tag.getInt(ConstructionBlueprintBlockEntity.CONSTRUCTION_ID_KEY).orElse(-1) == constructionId) {
                player.getInventory().setItem(i, ItemStack.EMPTY);
            }
        }
        player.getInventory().setChanged();
    }

    private static void chooseValetPerk(ChoosePerkPayload payload, ServerPlayNetworking.Context context) {
        MinecraftServer server = context.server();
        ServerPlayer player = context.player();
        server.execute(() -> {
            Entity entity = player.level().getEntity(payload.valetEntityId());
            if (!(entity instanceof Villager villager)) {
                return;
            }

            if (!isValidValetInteraction(player, villager)) {
                return;
            }

            ValetPerk perk = payload.perk();
            if (ValetProgress.choosePerk(villager, perk)) {
                ValetWorkGoal.requestRestart(villager);
                player.sendOverlayMessage(Component.translatable("message.valet.perk_set", Component.translatable(perk.getTranslationKey())));
            }
            sendValetState(player, villager);
        });
    }

    private static void chooseValetCombatPerk(ChooseCombatPerkPayload payload, ServerPlayNetworking.Context context) {
        MinecraftServer server = context.server();
        ServerPlayer player = context.player();
        server.execute(() -> {
            Entity entity = player.level().getEntity(payload.valetEntityId());
            if (!(entity instanceof Villager villager)) {
                return;
            }

            if (!isValidValetInteraction(player, villager)) {
                return;
            }

            ValetCombatPerk perk = payload.perk();
            if (ValetCombatProgress.choosePerk(villager, perk)) {
                ValetWorkGoal.requestRestart(villager);
                player.sendOverlayMessage(Component.translatable("message.valet.perk_set", Component.translatable(perk.getTranslationKey())));
            }
            sendValetState(player, villager);
        });
    }

    private static void renameValet(RenameValetPayload payload, ServerPlayNetworking.Context context) {
        MinecraftServer server = context.server();
        ServerPlayer player = context.player();
        String name = cleanName(payload.name());
        server.execute(() -> {
            Entity entity = player.level().getEntity(payload.valetEntityId());
            if (!(entity instanceof Villager villager)) {
                return;
            }

            if (!isValidValetInteraction(player, villager)) {
                return;
            }

            if (name.isEmpty()) {
                villager.setCustomName(null);
                villager.setCustomNameVisible(false);
            } else {
                villager.setCustomName(Component.literal(name));
                villager.setCustomNameVisible(true);
            }
            sendValetState(player, villager);
        });
    }

    private static String getValetName(Villager villager) {
        return villager.hasCustomName() && villager.getCustomName() != null ? villager.getCustomName().getString() : "";
    }

    private static boolean isValidValetInteraction(ServerPlayer player, Villager villager) {
        return ValetMod.isValet(villager) && player.distanceToSqr(villager) <= 64.0D;
    }

    private static String cleanName(String name) {
        return name.trim().replaceAll("\\s+", " ");
    }
}
