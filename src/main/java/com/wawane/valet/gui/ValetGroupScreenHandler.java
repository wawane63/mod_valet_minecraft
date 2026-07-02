package com.wawane.valet.gui;

import com.wawane.valet.ValetMod;
import com.wawane.valet.group.ValetGroupMode;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public class ValetGroupScreenHandler extends AbstractContainerMenu {
    private final BlockPos stationPos;
    private final int selectedGroupId;
    private final List<GroupEntry> groups;
    private final List<ValetEntry> valets;

    public ValetGroupScreenHandler(int syncId, Inventory inventory, FriendlyByteBuf buf) {
        this(syncId, inventory, new RegistryFriendlyByteBuf(buf, inventory.player.registryAccess()));
    }

    public ValetGroupScreenHandler(int syncId, Inventory inventory, OpeningData data) {
        this(syncId, inventory, data.asBuffer(inventory.player.registryAccess()));
    }

    private ValetGroupScreenHandler(int syncId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(syncId, inventory, readBlockPos(buf), buf.readInt(), readGroups(buf), readValets(buf));
    }

    public ValetGroupScreenHandler(int syncId, Inventory inventory, BlockPos stationPos, int selectedGroupId, List<GroupEntry> groups, List<ValetEntry> valets) {
        super(ValetMod.VALET_GROUP_SCREEN_HANDLER, syncId);
        this.stationPos = stationPos.immutable();
        this.selectedGroupId = selectedGroupId;
        this.groups = List.copyOf(groups);
        this.valets = List.copyOf(valets);
    }

    public BlockPos getStationPos() {
        return stationPos;
    }

    public int getSelectedGroupId() {
        return selectedGroupId;
    }

    public List<GroupEntry> getGroups() {
        return groups;
    }

    public List<ValetEntry> getValets() {
        return valets;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.level().getBlockState(stationPos).is(ValetMod.VALET_GROUP_STATION)
                && player.distanceToSqr(stationPos.getX() + 0.5D, stationPos.getY() + 0.5D, stationPos.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        return ItemStack.EMPTY;
    }

    public static void writeBlockPos(FriendlyByteBuf buf, BlockPos pos) {
        buf.writeInt(pos.getX());
        buf.writeInt(pos.getY());
        buf.writeInt(pos.getZ());
    }

    public static BlockPos readBlockPos(FriendlyByteBuf buf) {
        return new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());
    }

    public static void writeGroups(FriendlyByteBuf buf, List<GroupEntry> groups) {
        buf.writeInt(groups.size());
        for (GroupEntry group : groups) {
            buf.writeInt(group.id());
            buf.writeUtf(group.name(), 32);
            buf.writeInt(group.memberCount());
            buf.writeInt(group.mode().ordinal());
        }
    }

    public static List<GroupEntry> readGroups(FriendlyByteBuf buf) {
        int count = Math.max(0, buf.readInt());
        List<GroupEntry> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            result.add(new GroupEntry(buf.readInt(), buf.readUtf(32), buf.readInt(), ValetGroupMode.fromIndex(buf.readInt())));
        }
        return result;
    }

    public static void writeValets(FriendlyByteBuf buf, List<ValetEntry> valets) {
        buf.writeInt(valets.size());
        for (ValetEntry valet : valets) {
            buf.writeInt(valet.entityId());
            buf.writeUUID(valet.uuid());
            buf.writeUtf(valet.name(), 32);
            buf.writeInt(valet.roleIndex());
            buf.writeInt(valet.groupId());
        }
    }

    public static List<ValetEntry> readValets(FriendlyByteBuf buf) {
        int count = Math.max(0, buf.readInt());
        List<ValetEntry> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            result.add(new ValetEntry(buf.readInt(), buf.readUUID(), buf.readUtf(32), buf.readInt(), buf.readInt()));
        }
        return result;
    }

    public record GroupEntry(int id, String name, int memberCount, ValetGroupMode mode) {
    }

    public record ValetEntry(int entityId, UUID uuid, String name, int roleIndex, int groupId) {
    }

    public record OpeningData(byte[] bytes) {
        private static final int MAX_OPENING_DATA_BYTES = 131072;
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
