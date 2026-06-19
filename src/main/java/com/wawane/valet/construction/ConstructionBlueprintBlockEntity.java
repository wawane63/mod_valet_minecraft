package com.wawane.valet.construction;

import com.wawane.valet.ValetMod;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

public class ConstructionBlueprintBlockEntity extends BlockEntity {
    public static final String CONSTRUCTION_ID_KEY = "ConstructionId";
    public static final String CONSTRUCTION_NAME_KEY = "ConstructionName";
    public static final String BLUEPRINT_KEY = "Blueprint";
    public static final String VALET_UUID_KEY = "ValetUuid";

    private int constructionId = -1;
    private String constructionName = "";
    private ValetConstructionBlueprint blueprint;

    public ConstructionBlueprintBlockEntity(BlockPos pos, BlockState state) {
        super(ValetMod.CONSTRUCTION_BLUEPRINT_BLOCK_ENTITY, pos, state);
    }

    public int getConstructionId() {
        return constructionId;
    }

    public String getConstructionName() {
        return constructionName;
    }

    public ValetConstructionBlueprint getBlueprint() {
        return blueprint;
    }

    public void setConstruction(int constructionId, String constructionName, ValetConstructionBlueprint blueprint) {
        this.constructionId = constructionId;
        this.constructionName = constructionName == null ? "" : constructionName;
        this.blueprint = blueprint;
        markDirty();
        if (world != null && !world.isClient) {
            world.updateListeners(pos, getCachedState(), getCachedState(), Block.NOTIFY_ALL);
        }
    }

    public void setFromStackNbt(NbtCompound nbt) {
        if (nbt == null) {
            return;
        }

        ValetConstructionBlueprint stackBlueprint = nbt.contains(BLUEPRINT_KEY) ? ValetConstructionBlueprint.readNbt(nbt.getCompound(BLUEPRINT_KEY)) : null;
        int id = nbt.contains(CONSTRUCTION_ID_KEY) ? nbt.getInt(CONSTRUCTION_ID_KEY) : stackBlueprint == null ? -1 : stackBlueprint.id();
        String name = nbt.contains(CONSTRUCTION_NAME_KEY) ? nbt.getString(CONSTRUCTION_NAME_KEY) : stackBlueprint == null ? "" : stackBlueprint.name();
        setConstruction(id, name, stackBlueprint);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        constructionId = nbt.contains(CONSTRUCTION_ID_KEY) ? nbt.getInt(CONSTRUCTION_ID_KEY) : -1;
        constructionName = nbt.getString(CONSTRUCTION_NAME_KEY);
        blueprint = nbt.contains(BLUEPRINT_KEY) ? ValetConstructionBlueprint.readNbt(nbt.getCompound(BLUEPRINT_KEY)) : null;
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putInt(CONSTRUCTION_ID_KEY, constructionId);
        nbt.putString(CONSTRUCTION_NAME_KEY, constructionName);
        if (blueprint != null) {
            nbt.put(BLUEPRINT_KEY, blueprint.writeNbt());
        }
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        return createNbt();
    }

    @Nullable
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }
}
