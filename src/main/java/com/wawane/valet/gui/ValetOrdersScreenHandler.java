package com.wawane.valet.gui;

import com.wawane.valet.ValetConversations;
import com.wawane.valet.ValetMod;
import com.wawane.valet.ValetRole;
import com.wawane.valet.construction.ValetConstructionBlueprint;
import com.wawane.valet.farm.ValetFarmArea;
import com.wawane.valet.order.ValetCraftTarget;
import com.wawane.valet.order.ValetMineTarget;
import com.wawane.valet.order.ValetOrder;
import com.wawane.valet.order.ValetWoodTarget;
import com.wawane.valet.progress.ValetPerk;
import com.wawane.valet.progress.ValetCombatPerk;
import io.netty.buffer.Unpooled;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ValetOrdersScreenHandler extends AbstractContainerMenu {
    private final int valetEntityId;
    private final UUID valetUuid;
    private final Identifier valetDimension;
    private final int roleIndex;
    private final int currentOrderIndex;
    private final int currentMineTargetIndex;
    private final int currentWoodTargetIndex;
    private final int currentFarmAreaId;
    private final int currentFarmCropMask;
    private final boolean farmReplant;
    private final boolean farmTillSoil;
    private final int currentConstructionTargetId;
    private final int currentCraftTargetIndex;
    private final int[] oreCounts;
    private final int[] woodCounts;
    private final List<ValetFarmArea> farmAreas;
    private final List<ValetConstructionBlueprint> constructions;
    private final List<ItemStack> valetInventory;
    private final int level;
    private final int xp;
    private final int nextLevelXp;
    private final int pendingPerks;
    private final boolean[] perks;
    private final boolean[] combatPerks;
    private final int swordLevel;
    private final int swordXp;
    private final int swordNextLevelXp;
    private final int swordPendingPerks;
    private final int bowLevel;
    private final int bowXp;
    private final int bowNextLevelXp;
    private final int bowPendingPerks;
    private final boolean allyAwareness;
    private final String valetName;

    public ValetOrdersScreenHandler(int syncId, Inventory inventory, FriendlyByteBuf buf) {
        this(syncId, inventory, new RegistryFriendlyByteBuf(buf, inventory.player.registryAccess()));
    }

    public ValetOrdersScreenHandler(int syncId, Inventory inventory, OpeningData data) {
        this(syncId, inventory, data.asBuffer(inventory.player.registryAccess()));
    }

    private ValetOrdersScreenHandler(int syncId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(syncId, inventory, buf.readInt(), buf.readUUID(), buf.readIdentifier(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readBoolean(), buf.readBoolean(), buf.readInt(), buf.readInt(), readOreCounts(buf), readWoodCounts(buf), readFarmAreas(buf), readConstructions(buf), readInventory(buf), buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(), readPerks(buf), readCombatPerks(buf), buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readBoolean(), buf.readUtf(32));
    }

    public ValetOrdersScreenHandler(int syncId, Inventory inventory, int valetEntityId) {
        this(syncId, inventory, valetEntityId, new UUID(0L, 0L), Level.OVERWORLD.identifier(), ValetRole.ARTISAN.ordinal(), ValetOrder.NONE.ordinal(), -1, -1, -1, com.wawane.valet.order.ValetFarmCrop.defaultMask(), false, false, -1, -1, new int[ValetMineTarget.values().length], new int[ValetWoodTarget.values().length], List.of(), List.of(), List.of(), 1, 0, 40, 0, new boolean[ValetPerk.values().length], new boolean[ValetCombatPerk.values().length], 1, 0, 30, 0, 1, 0, 30, 0, true, "");
    }

    public ValetOrdersScreenHandler(int syncId, Inventory inventory, int valetEntityId, UUID valetUuid, Identifier valetDimension, int roleIndex, int currentOrderIndex, int currentMineTargetIndex, int currentWoodTargetIndex, int currentFarmAreaId, int currentFarmCropMask, boolean farmReplant, boolean farmTillSoil, int currentConstructionTargetId, int currentCraftTargetIndex, int[] oreCounts, int[] woodCounts, List<ValetFarmArea> farmAreas, List<ValetConstructionBlueprint> constructions, List<ItemStack> valetInventory, int level, int xp, int nextLevelXp, int pendingPerks, boolean[] perks, boolean[] combatPerks, int swordLevel, int swordXp, int swordNextLevelXp, int swordPendingPerks, int bowLevel, int bowXp, int bowNextLevelXp, int bowPendingPerks, boolean allyAwareness, String valetName) {
        super(ValetMod.VALET_ORDERS_SCREEN_HANDLER, syncId);
        this.valetEntityId = valetEntityId;
        this.valetUuid = valetUuid;
        this.valetDimension = valetDimension;
        this.roleIndex = roleIndex;
        this.currentOrderIndex = currentOrderIndex;
        this.currentMineTargetIndex = currentMineTargetIndex;
        this.currentWoodTargetIndex = currentWoodTargetIndex;
        this.currentFarmAreaId = currentFarmAreaId;
        this.currentFarmCropMask = currentFarmCropMask;
        this.farmReplant = farmReplant;
        this.farmTillSoil = farmTillSoil;
        this.currentConstructionTargetId = currentConstructionTargetId;
        this.currentCraftTargetIndex = currentCraftTargetIndex;
        this.oreCounts = oreCounts;
        this.woodCounts = woodCounts;
        this.farmAreas = List.copyOf(farmAreas);
        this.constructions = constructions;
        this.valetInventory = copyInventory(valetInventory);
        this.level = level;
        this.xp = xp;
        this.nextLevelXp = nextLevelXp;
        this.pendingPerks = pendingPerks;
        this.perks = perks;
        this.combatPerks = combatPerks;
        this.swordLevel = swordLevel;
        this.swordXp = swordXp;
        this.swordNextLevelXp = swordNextLevelXp;
        this.swordPendingPerks = swordPendingPerks;
        this.bowLevel = bowLevel;
        this.bowXp = bowXp;
        this.bowNextLevelXp = bowNextLevelXp;
        this.bowPendingPerks = bowPendingPerks;
        this.allyAwareness = allyAwareness;
        this.valetName = valetName;
    }

    public int getValetEntityId() {
        return valetEntityId;
    }

    public UUID getValetUuid() {
        return valetUuid;
    }

    public Identifier getValetDimension() {
        return valetDimension;
    }

    public ValetRole getRole() {
        return ValetRole.fromIndex(roleIndex);
    }

    public ValetOrder getCurrentOrder() {
        return ValetOrder.fromIndex(currentOrderIndex);
    }

    public int getCurrentMineTargetIndex() {
        return currentMineTargetIndex;
    }

    public int getCurrentWoodTargetIndex() {
        return currentWoodTargetIndex;
    }

    public int getCurrentFarmAreaId() {
        return currentFarmAreaId;
    }

    public int getCurrentFarmCropMask() {
        return currentFarmCropMask;
    }

    public boolean shouldReplantFarm() {
        return farmReplant;
    }

    public boolean shouldTillFarm() {
        return farmTillSoil;
    }

    public int getCurrentConstructionTargetId() {
        return currentConstructionTargetId;
    }

    public int getCurrentCraftTargetIndex() {
        return currentCraftTargetIndex;
    }

    public int getOreCount(ValetMineTarget target) {
        int index = target.ordinal();
        if (index < 0 || index >= oreCounts.length) {
            return 0;
        }
        return oreCounts[index];
    }

    public int getWoodCount(ValetWoodTarget target) {
        int index = target.ordinal();
        if (index < 0 || index >= woodCounts.length) {
            return 0;
        }
        return woodCounts[index];
    }

    public List<ValetConstructionBlueprint> getConstructions() {
        return constructions;
    }

    public List<ValetFarmArea> getFarmAreas() {
        return farmAreas;
    }

    public List<ItemStack> getValetInventory() {
        return copyInventory(valetInventory);
    }

    public int getLevel() {
        return level;
    }

    public int getXp() {
        return xp;
    }

    public int getNextLevelXp() {
        return nextLevelXp;
    }

    public int getPendingPerks() {
        return pendingPerks;
    }

    public boolean hasPerk(ValetPerk perk) {
        int index = perk.ordinal();
        return index >= 0 && index < perks.length && perks[index];
    }

    public boolean hasCombatPerk(ValetCombatPerk perk) {
        int index = perk.ordinal();
        return index >= 0 && index < combatPerks.length && combatPerks[index];
    }

    public int getSwordLevel() {
        return swordLevel;
    }

    public int getSwordXp() {
        return swordXp;
    }

    public int getSwordNextLevelXp() {
        return swordNextLevelXp;
    }

    public int getSwordPendingPerks() {
        return swordPendingPerks;
    }

    public int getBowLevel() {
        return bowLevel;
    }

    public int getBowXp() {
        return bowXp;
    }

    public int getBowNextLevelXp() {
        return bowNextLevelXp;
    }

    public int getBowPendingPerks() {
        return bowPendingPerks;
    }

    public boolean hasAllyAwareness() {
        return allyAwareness;
    }

    public String getValetName() {
        return valetName;
    }

    @Override
    public boolean stillValid(Player player) {
        net.minecraft.world.entity.Entity entity = player.level().getEntity(valetEntityId);
        return entity instanceof Villager villager
                && ValetMod.isValet(villager)
                && player.distanceToSqr(villager) <= 64.0D;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (player.level().isClientSide()) {
            return;
        }

        ValetConversations.end(valetUuid);
    }

    private static int[] readOreCounts(FriendlyByteBuf buf) {
        int[] counts = new int[ValetMineTarget.values().length];
        for (int i = 0; i < counts.length; i++) {
            counts[i] = buf.readInt();
        }
        return counts;
    }

    private static int[] readWoodCounts(FriendlyByteBuf buf) {
        int[] counts = new int[ValetWoodTarget.values().length];
        for (int i = 0; i < counts.length; i++) {
            counts[i] = buf.readInt();
        }
        return counts;
    }

    private static boolean[] readPerks(FriendlyByteBuf buf) {
        boolean[] perks = new boolean[ValetPerk.values().length];
        for (int i = 0; i < perks.length; i++) {
            perks[i] = buf.readBoolean();
        }
        return perks;
    }

    private static boolean[] readCombatPerks(FriendlyByteBuf buf) {
        boolean[] perks = new boolean[ValetCombatPerk.values().length];
        for (int i = 0; i < perks.length; i++) {
            perks[i] = buf.readBoolean();
        }
        return perks;
    }

    private static List<ValetConstructionBlueprint> readConstructions(FriendlyByteBuf buf) {
        int count = buf.readInt();
        List<ValetConstructionBlueprint> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            CompoundTag nbt = buf.readNbt();
            if (nbt != null) {
                result.add(ValetConstructionBlueprint.readNbt(nbt));
            }
        }
        return result;
    }

    private static List<ValetFarmArea> readFarmAreas(FriendlyByteBuf buf) {
        int count = buf.readInt();
        List<ValetFarmArea> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            CompoundTag nbt = buf.readNbt();
            if (nbt != null) {
                result.add(ValetFarmArea.readNbt(nbt));
            }
        }
        return result;
    }

    private static List<ItemStack> readInventory(RegistryFriendlyByteBuf buf) {
        int count = Math.max(0, buf.readInt());
        List<ItemStack> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            result.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(buf));
        }
        return result;
    }

    private static List<ItemStack> copyInventory(List<ItemStack> stacks) {
        List<ItemStack> result = new ArrayList<>(stacks.size());
        for (ItemStack stack : stacks) {
            result.add(stack.copy());
        }
        return result;
    }

    public record OpeningData(byte[] bytes) {
        private static final int MAX_OPENING_DATA_BYTES = 262144;
        public static final StreamCodec<RegistryFriendlyByteBuf, OpeningData> CODEC = StreamCodec.ofMember(OpeningData::write, OpeningData::read);

        public OpeningData {
            bytes = Arrays.copyOf(bytes, bytes.length);
        }

        public static OpeningData create(RegistryAccess registryAccess, Consumer<RegistryFriendlyByteBuf> writer) {
            RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), registryAccess);
            writer.accept(buf);
            byte[] bytes = new byte[buf.readableBytes()];
            buf.getBytes(buf.readerIndex(), bytes);
            return new OpeningData(bytes);
        }

        public static OpeningData read(RegistryFriendlyByteBuf buf) {
            return new OpeningData(buf.readByteArray(MAX_OPENING_DATA_BYTES));
        }

        public void write(RegistryFriendlyByteBuf buf) {
            buf.writeByteArray(bytes);
        }

        public RegistryFriendlyByteBuf asBuffer(RegistryAccess registryAccess) {
            return new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(bytes), registryAccess);
        }
    }
}
