package com.wawane.valet.construction;

import com.wawane.valet.ValetMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
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
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    public void setFromStackNbt(CompoundTag nbt) {
        if (nbt == null) {
            return;
        }

        ValetConstructionBlueprint stackBlueprint = nbt.getCompound(BLUEPRINT_KEY).map(ValetConstructionBlueprint::readNbt).orElse(null);
        int id = nbt.getInt(CONSTRUCTION_ID_KEY).orElse(stackBlueprint == null ? -1 : stackBlueprint.id());
        String name = nbt.getString(CONSTRUCTION_NAME_KEY).orElse(stackBlueprint == null ? "" : stackBlueprint.name());
        setConstruction(id, name, stackBlueprint);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        constructionId = input.getIntOr(CONSTRUCTION_ID_KEY, -1);
        constructionName = input.getStringOr(CONSTRUCTION_NAME_KEY, "");
        blueprint = input.read(BLUEPRINT_KEY, CompoundTag.CODEC).map(ValetConstructionBlueprint::readNbt).orElse(null);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt(CONSTRUCTION_ID_KEY, constructionId);
        output.putString(CONSTRUCTION_NAME_KEY, constructionName);
        if (blueprint != null) {
            output.store(BLUEPRINT_KEY, CompoundTag.CODEC, blueprint.writeNbt());
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
