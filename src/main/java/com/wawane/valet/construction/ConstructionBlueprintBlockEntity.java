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
    public static final String MIRRORED_KEY = "Mirrored";

    private int constructionId = -1;
    private String constructionName = "";
    private ValetConstructionBlueprint blueprint;
    private boolean mirrored;

    public ConstructionBlueprintBlockEntity(BlockPos pos, BlockState state) {
        super(ValetMod.CONSTRUCTION_BLUEPRINT_BLOCK_ENTITY, pos, state);
    }

    public int getConstructionId() {
        return constructionId;
    }

    public ValetConstructionBlueprint getBlueprint() {
        return blueprint;
    }

    public boolean isMirrored() {
        return mirrored;
    }

    public void setConstruction(int constructionId, String constructionName, ValetConstructionBlueprint blueprint, boolean mirrored) {
        this.constructionId = constructionId;
        this.constructionName = ValetConstructionBlueprint.cleanName(constructionName, constructionId);
        this.blueprint = blueprint != null && blueprint.isValid() ? blueprint : null;
        this.mirrored = mirrored;
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    public void setFromStackNbt(CompoundTag nbt, boolean placementMirrored) {
        if (nbt == null) {
            return;
        }

        ValetConstructionBlueprint stackBlueprint = nbt.getCompound(BLUEPRINT_KEY)
                .map(ValetConstructionBlueprint::readNbt)
                .filter(ValetConstructionBlueprint::isValid)
                .orElse(null);
        int id = stackBlueprint == null ? nbt.getIntOr(CONSTRUCTION_ID_KEY, -1) : stackBlueprint.id();
        String name = stackBlueprint == null ? nbt.getStringOr(CONSTRUCTION_NAME_KEY, "") : stackBlueprint.name();
        boolean stackMirrored = nbt.getBooleanOr(MIRRORED_KEY, false);
        setConstruction(id, name, stackBlueprint, stackMirrored || placementMirrored);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        constructionId = input.getIntOr(CONSTRUCTION_ID_KEY, -1);
        constructionName = ValetConstructionBlueprint.cleanName(input.getStringOr(CONSTRUCTION_NAME_KEY, ""), constructionId);
        blueprint = input.read(BLUEPRINT_KEY, CompoundTag.CODEC)
                .map(ValetConstructionBlueprint::readNbt)
                .filter(ValetConstructionBlueprint::isValid)
                .orElse(null);
        if (blueprint != null) {
            constructionId = blueprint.id();
            constructionName = blueprint.name();
        }
        mirrored = input.getBooleanOr(MIRRORED_KEY, false);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt(CONSTRUCTION_ID_KEY, constructionId);
        output.putString(CONSTRUCTION_NAME_KEY, constructionName);
        output.putBoolean(MIRRORED_KEY, mirrored);
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
