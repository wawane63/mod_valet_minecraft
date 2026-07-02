package com.wawane.valet.group;

import com.wawane.valet.ValetDebug;
import com.wawane.valet.ValetMod;
import com.wawane.valet.ValetRole;
import com.wawane.valet.ai.ValetWorkGoal;
import com.wawane.valet.gui.ValetGroupScreenHandler;
import com.wawane.valet.network.packets.ManageGroupPayload;
import com.wawane.valet.network.packets.ValetGroupStatePayload;
import com.wawane.valet.state.ValetBehavior;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

public final class ValetGroupInteractions {
    private static final int VALET_SCAN_RADIUS = 36;

    private ValetGroupInteractions() {
    }

    public static InteractionResult useBlock(Player player, Level world, InteractionHand hand, BlockHitResult hitResult) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        BlockPos pos = hitResult.getBlockPos();
        if (world.getBlockState(pos).is(ValetMod.VALET_GROUP_STATION)) {
            if (!world.isClientSide() && player instanceof ServerPlayer serverPlayer) {
                openGroupStation(serverPlayer, pos, -1);
            }
            return InteractionResult.SUCCESS;
        }

        ItemStack stack = player.getItemInHand(hand);
        int groupId = ValetGroupBindings.getGroupId(stack);
        if (groupId <= 0) {
            return InteractionResult.PASS;
        }
        if (!world.isClientSide() && world instanceof ServerLevel serverWorld) {
            ValetGroupCommand command = player.isShiftKeyDown() ? ValetGroupCommand.recall() : ValetGroupCommand.attackArea(pos);
            applyGroupCommand(serverWorld, player, groupId, command);
        }
        return InteractionResult.SUCCESS;
    }

    public static InteractionResult useItem(Player player, Level world, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(ValetMod.VALET_GROUP_CARD_ITEM)) {
            return InteractionResult.PASS;
        }
        return useBoundControlItem(player, world, hand);
    }

    public static InteractionResult useBoundControlItem(Player player, Level world, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        ItemStack stack = player.getItemInHand(hand);
        int groupId = ValetGroupBindings.getGroupId(stack);
        if (groupId <= 0) {
            return InteractionResult.PASS;
        }

        if (!world.isClientSide() && world instanceof ServerLevel serverWorld) {
            ValetGroupStorage storage = ValetGroupStorage.get(serverWorld);
            ValetGroup group = storage.getGroup(groupId);
            if (group == null) {
                player.sendOverlayMessage(Component.translatable("message.valet_group.unknown"));
                return stack.is(Items.GOAT_HORN) ? InteractionResult.PASS : InteractionResult.SUCCESS;
            }
            ValetGroupCommand command = player.isShiftKeyDown()
                    ? nextHandCommand(group.command(), player.getUUID())
                    : ValetGroupCommand.follow(player.getUUID());
            applyGroupCommand(serverWorld, player, groupId, command);
        }
        return stack.is(Items.GOAT_HORN) ? InteractionResult.PASS : InteractionResult.SUCCESS;
    }

    public static InteractionResult useEntity(Player player, Level world, InteractionHand hand, Entity entity, EntityHitResult hitResult) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        ItemStack stack = player.getItemInHand(hand);
        int groupId = ValetGroupBindings.getGroupId(stack);
        if (groupId <= 0 || !(entity instanceof Monster)) {
            return InteractionResult.PASS;
        }
        if (!world.isClientSide() && world instanceof ServerLevel serverWorld) {
            applyGroupCommand(serverWorld, player, groupId, ValetGroupCommand.attackTarget(entity.getUUID(), entity.blockPosition()));
        }
        return InteractionResult.SUCCESS;
    }

    public static void handleManagement(ManageGroupPayload payload, ServerPlayNetworking.Context context) {
        ServerPlayer player = context.player();
        context.server().execute(() -> {
            BlockPos stationPos = payload.stationPos();
            if (!isValidStation(player, stationPos)) {
                return;
            }
            ServerLevel world = player.level();
            ValetGroupStorage storage = ValetGroupStorage.get(world);
            int selectedGroupId = payload.groupId();

            switch (payload.action()) {
                case CREATE -> {
                    ValetGroup group = storage.addGroup(storage.nextDefaultName());
                    if (group != null) {
                        selectedGroupId = group.id();
                        player.sendOverlayMessage(Component.translatable("message.valet_group.created", group.name()));
                    }
                }
                case DELETE -> {
                    storage.removeGroup(payload.groupId());
                    selectedGroupId = firstGroupId(storage);
                    player.sendOverlayMessage(Component.translatable("message.valet_group.deleted"));
                }
                case TOGGLE_MEMBER -> {
                    if (!ManageGroupPayload.NO_VALET.equals(payload.valetUuid()) && isValetNearStation(world, payload.valetUuid(), stationPos)) {
                        storage.toggleMember(payload.groupId(), payload.valetUuid());
                        ValetGroup group = storage.getGroup(payload.groupId());
                        if (group != null) {
                            restartGroup(world, group);
                        }
                    }
                }
                case GIVE_CARD -> giveGroupCard(player, storage.getGroup(payload.groupId()));
                case BIND_HORN -> bindHeldHorn(player, storage.getGroup(payload.groupId()));
                case COMMAND -> applyGroupCommand(world, player, payload.groupId(), commandFromMode(payload.mode(), player, stationPos));
            }
            sendGroupState(player, stationPos, selectedGroupId);
        });
    }

    public static void openGroupStation(ServerPlayer player, BlockPos stationPos, int selectedGroupId) {
        if (!isValidStation(player, stationPos)) {
            return;
        }
        int selected = selectedGroupId > 0 ? selectedGroupId : firstGroupId(ValetGroupStorage.get(player.level()));
        List<ValetGroupScreenHandler.GroupEntry> groups = buildGroupEntries(player.level());
        List<ValetGroupScreenHandler.ValetEntry> valets = buildValetEntries(player.level(), stationPos);
        player.openMenu(new ExtendedMenuProvider<ValetGroupScreenHandler.OpeningData>() {
            @Override
            public ValetGroupScreenHandler.OpeningData getScreenOpeningData(ServerPlayer openingPlayer) {
                return ValetGroupScreenHandler.OpeningData.create(openingPlayer.registryAccess(), buf -> writeOpeningData(buf, stationPos, selected, groups, valets));
            }

            @Override
            public Component getDisplayName() {
                return Component.translatable("screen.valet_group.title");
            }

            @Override
            public AbstractContainerMenu createMenu(int syncId, Inventory inventory, Player openingPlayer) {
                return new ValetGroupScreenHandler(syncId, inventory, stationPos, selected, groups, valets);
            }
        });
    }

    private static void sendGroupState(ServerPlayer player, BlockPos stationPos, int selectedGroupId) {
        if (!ServerPlayNetworking.canSend(player, ValetGroupStatePayload.TYPE)) {
            return;
        }
        ServerPlayNetworking.send(player, new ValetGroupStatePayload(
                stationPos,
                selectedGroupId > 0 ? selectedGroupId : firstGroupId(ValetGroupStorage.get(player.level())),
                buildGroupEntries(player.level()),
                buildValetEntries(player.level(), stationPos)
        ));
    }

    private static void writeOpeningData(RegistryFriendlyByteBuf buf, BlockPos stationPos, int selectedGroupId, List<ValetGroupScreenHandler.GroupEntry> groups, List<ValetGroupScreenHandler.ValetEntry> valets) {
        ValetGroupScreenHandler.writeBlockPos(buf, stationPos);
        buf.writeInt(selectedGroupId);
        ValetGroupScreenHandler.writeGroups(buf, groups);
        ValetGroupScreenHandler.writeValets(buf, valets);
    }

    private static List<ValetGroupScreenHandler.GroupEntry> buildGroupEntries(ServerLevel world) {
        return ValetGroupStorage.get(world).getGroups().stream()
                .map(group -> new ValetGroupScreenHandler.GroupEntry(group.id(), group.name(), group.memberCount(), group.command().mode()))
                .toList();
    }

    private static List<ValetGroupScreenHandler.ValetEntry> buildValetEntries(ServerLevel world, BlockPos stationPos) {
        AABB box = new AABB(stationPos).inflate(VALET_SCAN_RADIUS, 8.0D, VALET_SCAN_RADIUS);
        ValetGroupStorage storage = ValetGroupStorage.get(world);
        return world.getEntitiesOfClass(Villager.class, box, villager -> villager.isAlive() && !villager.isRemoved() && ValetMod.isValet(villager)).stream()
                .sorted(Comparator.comparingDouble(villager -> villager.distanceToSqr(stationPos.getX() + 0.5D, stationPos.getY() + 0.5D, stationPos.getZ() + 0.5D)))
                .map(villager -> new ValetGroupScreenHandler.ValetEntry(
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

    private static boolean isValidStation(ServerPlayer player, BlockPos stationPos) {
        return player.level().getBlockState(stationPos).is(ValetMod.VALET_GROUP_STATION)
                && player.distanceToSqr(stationPos.getX() + 0.5D, stationPos.getY() + 0.5D, stationPos.getZ() + 0.5D) <= 64.0D;
    }

    private static boolean isValetNearStation(ServerLevel world, UUID valetUuid, BlockPos stationPos) {
        AABB box = new AABB(stationPos).inflate(VALET_SCAN_RADIUS, 8.0D, VALET_SCAN_RADIUS);
        return !world.getEntitiesOfClass(Villager.class, box, villager -> villager.getUUID().equals(valetUuid) && ValetMod.isValet(villager)).isEmpty();
    }

    private static void giveGroupCard(ServerPlayer player, ValetGroup group) {
        if (group == null) {
            return;
        }
        ItemStack stack = new ItemStack(ValetMod.VALET_GROUP_CARD_ITEM);
        ValetGroupBindings.setGroup(stack, group);
        player.getInventory().placeItemBackInInventory(stack);
        player.sendOverlayMessage(Component.translatable("message.valet_group.card_given", group.name()));
    }

    private static void bindHeldHorn(ServerPlayer player, ValetGroup group) {
        if (group == null) {
            return;
        }
        ItemStack stack = player.getMainHandItem();
        if (!stack.is(Items.GOAT_HORN)) {
            stack = player.getOffhandItem();
        }
        if (!stack.is(Items.GOAT_HORN)) {
            player.sendOverlayMessage(Component.translatable("message.valet_group.no_horn"));
            return;
        }
        ValetGroupBindings.setGroup(stack, group);
        player.sendOverlayMessage(Component.translatable("message.valet_group.horn_bound", group.name()));
    }

    private static ValetGroupCommand commandFromMode(ValetGroupMode mode, ServerPlayer player, BlockPos stationPos) {
        return switch (mode) {
            case FOLLOW -> ValetGroupCommand.follow(player.getUUID());
            case GUARD_CLOSE -> ValetGroupCommand.guardClose(player.getUUID());
            case GUARD_WIDE -> ValetGroupCommand.guardWide(player.getUUID());
            case ATTACK_AREA -> ValetGroupCommand.attackArea(stationPos);
            case RECALL -> ValetGroupCommand.recall();
            case IDLE, ATTACK_TARGET -> ValetGroupCommand.idle();
        };
    }

    private static ValetGroupCommand nextHandCommand(ValetGroupCommand current, UUID playerUuid) {
        return switch (current.mode()) {
            case FOLLOW -> ValetGroupCommand.guardClose(playerUuid);
            case GUARD_CLOSE -> ValetGroupCommand.guardWide(playerUuid);
            case GUARD_WIDE -> ValetGroupCommand.recall();
            case IDLE, ATTACK_TARGET, ATTACK_AREA, RECALL -> ValetGroupCommand.follow(playerUuid);
        };
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
            if (!(entity instanceof Villager villager) || !ValetMod.isValet(villager)) {
                continue;
            }
            if (group.command().mode() == ValetGroupMode.RECALL) {
                ValetBehavior.recallToWorkstation(world, villager);
            }
            ValetWorkGoal.requestRestart(villager);
            ValetDebug.record(villager, "group command=" + group.command().mode().name().toLowerCase() + " group=" + group.id());
        }
    }

    private static int firstGroupId(ValetGroupStorage storage) {
        return storage.getGroups().isEmpty() ? -1 : storage.getGroups().get(0).id();
    }
}
