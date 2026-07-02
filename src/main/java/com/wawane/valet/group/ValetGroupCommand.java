package com.wawane.valet.group;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

public record ValetGroupCommand(ValetGroupMode mode, UUID playerUuid, UUID targetUuid, BlockPos pos, int radius) {
    private static final int CLOSE_GUARD_RADIUS = 8;
    private static final int WIDE_GUARD_RADIUS = 18;
    private static final int ATTACK_AREA_RADIUS = 14;

    public static ValetGroupCommand idle() {
        return new ValetGroupCommand(ValetGroupMode.IDLE, null, null, null, 0);
    }

    public static ValetGroupCommand follow(UUID playerUuid) {
        return new ValetGroupCommand(ValetGroupMode.FOLLOW, playerUuid, null, null, 4);
    }

    public static ValetGroupCommand guardClose(UUID playerUuid) {
        return new ValetGroupCommand(ValetGroupMode.GUARD_CLOSE, playerUuid, null, null, CLOSE_GUARD_RADIUS);
    }

    public static ValetGroupCommand guardWide(UUID playerUuid) {
        return new ValetGroupCommand(ValetGroupMode.GUARD_WIDE, playerUuid, null, null, WIDE_GUARD_RADIUS);
    }

    public static ValetGroupCommand attackTarget(UUID targetUuid, BlockPos fallbackPos) {
        return new ValetGroupCommand(ValetGroupMode.ATTACK_TARGET, null, targetUuid, fallbackPos, ATTACK_AREA_RADIUS);
    }

    public static ValetGroupCommand attackArea(BlockPos pos) {
        return new ValetGroupCommand(ValetGroupMode.ATTACK_AREA, null, null, pos, ATTACK_AREA_RADIUS);
    }

    public static ValetGroupCommand recall() {
        return new ValetGroupCommand(ValetGroupMode.RECALL, null, null, null, 0);
    }

    public ValetGroupCommand withMode(ValetGroupMode mode, UUID playerUuid, BlockPos pos) {
        return switch (mode) {
            case FOLLOW -> follow(playerUuid);
            case GUARD_CLOSE -> guardClose(playerUuid);
            case GUARD_WIDE -> guardWide(playerUuid);
            case ATTACK_AREA -> attackArea(pos);
            case RECALL -> recall();
            case IDLE, ATTACK_TARGET -> idle();
        };
    }

    public CompoundTag writeNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putInt("Mode", mode.ordinal());
        if (playerUuid != null) {
            nbt.putString("Player", playerUuid.toString());
        }
        if (targetUuid != null) {
            nbt.putString("Target", targetUuid.toString());
        }
        if (pos != null) {
            nbt.putInt("X", pos.getX());
            nbt.putInt("Y", pos.getY());
            nbt.putInt("Z", pos.getZ());
        }
        nbt.putInt("Radius", radius);
        return nbt;
    }

    public static ValetGroupCommand readNbt(CompoundTag nbt) {
        ValetGroupMode mode = ValetGroupMode.fromIndex(nbt.getIntOr("Mode", 0));
        UUID playerUuid = readUuid(nbt, "Player");
        UUID targetUuid = readUuid(nbt, "Target");
        BlockPos pos = nbt.contains("X")
                ? new BlockPos(nbt.getIntOr("X", 0), nbt.getIntOr("Y", 0), nbt.getIntOr("Z", 0))
                : null;
        int radius = Math.max(0, nbt.getIntOr("Radius", 0));
        return new ValetGroupCommand(mode, playerUuid, targetUuid, pos, radius);
    }

    private static UUID readUuid(CompoundTag nbt, String key) {
        return nbt.getString(key)
                .flatMap(value -> {
                    try {
                        return java.util.Optional.of(UUID.fromString(value));
                    } catch (IllegalArgumentException ignored) {
                        return java.util.Optional.empty();
                    }
                })
                .orElse(null);
    }
}
