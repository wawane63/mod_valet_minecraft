package com.wawane.valet.quest;

import com.mojang.serialization.Codec;
import com.wawane.valet.ValetMod;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** Identite persistante du maire unique de la dimension. */
final class ValetMayorState extends SavedData {
    private static final String KEY = "valet_mayor";
    private static final Codec<ValetMayorState> CODEC = CompoundTag.CODEC.xmap(
            ValetMayorState::fromNbt,
            ValetMayorState::saveToNbt
    );
    private static final SavedDataType<ValetMayorState> TYPE = new SavedDataType<>(
            ValetMod.id(KEY),
            ValetMayorState::new,
            CODEC,
            DataFixTypes.LEVEL
    );

    private UUID mayorUuid;
    private BlockPos bell;

    static ValetMayorState get(ServerLevel world) {
        return world.getDataStorage().computeIfAbsent(TYPE);
    }

    UUID mayorUuid() {
        return mayorUuid;
    }

    BlockPos bell() {
        return bell;
    }

    void setMayor(UUID mayorUuid, BlockPos bell) {
        this.mayorUuid = mayorUuid;
        this.bell = bell == null ? null : bell.immutable();
        setDirty();
    }

    void clearMayor() {
        mayorUuid = null;
        bell = null;
        setDirty();
    }

    private CompoundTag saveToNbt() {
        CompoundTag nbt = new CompoundTag();
        if (mayorUuid != null) {
            nbt.putString("MayorUuid", mayorUuid.toString());
        }
        if (bell != null) {
            nbt.putInt("BellX", bell.getX());
            nbt.putInt("BellY", bell.getY());
            nbt.putInt("BellZ", bell.getZ());
        }
        return nbt;
    }

    private static ValetMayorState fromNbt(CompoundTag nbt) {
        ValetMayorState state = new ValetMayorState();
        nbt.getString("MayorUuid").ifPresent(value -> {
            try {
                state.mayorUuid = UUID.fromString(value);
            } catch (IllegalArgumentException ignored) {
                state.mayorUuid = null;
            }
        });
        if (nbt.contains("BellX") && nbt.contains("BellY") && nbt.contains("BellZ")) {
            state.bell = new BlockPos(
                    nbt.getIntOr("BellX", 0),
                    nbt.getIntOr("BellY", 0),
                    nbt.getIntOr("BellZ", 0)
            );
        }
        return state;
    }
}
