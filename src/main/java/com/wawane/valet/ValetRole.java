package com.wawane.valet;

import com.wawane.valet.order.ValetOrder;
import com.wawane.valet.order.ValetOrders;
import com.wawane.valet.state.ValetIdentity;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public enum ValetRole {
    ARTISAN("artisan", "role.valet.artisan"),
    COMBATANT("combatant", "role.valet.combatant"),
    FARMER("farmer", "role.valet.farmer"),
    BREEDER("breeder", "role.valet.breeder"),
    MAGICIAN("magician", "role.valet.magician"),
    COOK("cook", "role.valet.cook"),
    STEWARD("steward", "role.valet.steward");

    private static final String ROLE_KEY = "ValetRole";
    private static final Map<UUID, ValetRole> LAST_KNOWN_ROLES = new ConcurrentHashMap<>();

    private final String id;
    private final String translationKey;

    ValetRole(String id, String translationKey) {
        this.id = id;
        this.translationKey = translationKey;
    }

    public String getTranslationKey() {
        return translationKey;
    }

    public String getId() {
        return id;
    }

    public boolean allows(ValetOrder order) {
        return switch (this) {
            case ARTISAN -> order == ValetOrder.NONE
                    || order == ValetOrder.MINE_ORES
                    || order == ValetOrder.CHOP_WOOD
                    || order == ValetOrder.BUILD_STRUCTURE
                    || order == ValetOrder.CRAFT;
            case COMBATANT -> order == ValetOrder.NONE;
            case FARMER -> order == ValetOrder.NONE || order == ValetOrder.HARVEST_CROPS;
            case BREEDER -> order == ValetOrder.NONE || order == ValetOrder.BREED_ANIMALS;
            case MAGICIAN, COOK, STEWARD -> order == ValetOrder.NONE;
        };
    }

    public static ValetRole fromIndex(int index) {
        ValetRole[] values = values();
        if (index < 0 || index >= values.length) {
            return ARTISAN;
        }
        return values[index];
    }

    public static ValetRole fromId(String id) {
        for (ValetRole role : values()) {
            if (role.id.equals(id) || role.name().equalsIgnoreCase(id)) {
                return role;
            }
        }
        return ARTISAN;
    }

    public static ValetRole fromWorkstation(BlockState state) {
        if (state.is(ValetMod.COMBAT_WORKSTATION)) {
            return COMBATANT;
        }
        if (state.is(ValetMod.FARMER_WORKSTATION)) {
            return FARMER;
        }
        if (state.is(ValetMod.ANIMAL_WORKSTATION)) {
            return BREEDER;
        }
        if (state.is(ValetMod.MAGIC_WORKSTATION)) {
            return MAGICIAN;
        }
        if (state.is(ValetMod.COOK_WORKSTATION)) {
            return COOK;
        }
        if (state.is(ValetMod.STEWARD_WORKSTATION)) {
            return STEWARD;
        }
        return ARTISAN;
    }

    public static ValetRole get(ServerLevel world, Villager villager) {
        if (ValetIdentity.isTagged(villager)) {
            return LAST_KNOWN_ROLES.getOrDefault(villager.getUUID(), ARTISAN);
        }
        BlockPos home = ValetHome.get(world, villager);
        if (home == null) {
            LAST_KNOWN_ROLES.remove(villager.getUUID());
            return ARTISAN;
        }
        if (!ValetHome.isChunkLoaded(world, home)) {
            ValetRole cached = LAST_KNOWN_ROLES.get(villager.getUUID());
            return cached != null ? cached : inferFromOrder(villager);
        }
        ValetRole role = fromWorkstation(world.getBlockState(home));
        LAST_KNOWN_ROLES.put(villager.getUUID(), role);
        return role;
    }

    public static void clear(UUID uuid) {
        LAST_KNOWN_ROLES.remove(uuid);
    }

    public static void set(Villager villager, ValetRole role) {
        LAST_KNOWN_ROLES.put(villager.getUUID(), role == null ? ARTISAN : role);
    }

    public static void clearAll() {
        LAST_KNOWN_ROLES.clear();
    }

    public static boolean hasNbt(ValueInput input) {
        return input.getString(ROLE_KEY).isPresent();
    }

    public static void writeToNbt(Villager villager, ValueOutput output) {
        ValetRole role = villager.level() instanceof ServerLevel world
                ? get(world, villager)
                : LAST_KNOWN_ROLES.getOrDefault(villager.getUUID(), ARTISAN);
        output.putString(ROLE_KEY, role.id);
    }

    public static void readFromNbt(Villager villager, ValueInput input) {
        input.getString(ROLE_KEY).ifPresentOrElse(
                id -> LAST_KNOWN_ROLES.put(villager.getUUID(), fromId(id)),
                () -> LAST_KNOWN_ROLES.remove(villager.getUUID())
        );
    }

    private static ValetRole inferFromOrder(Villager villager) {
        return switch (ValetOrders.get(villager)) {
            case HARVEST_CROPS -> FARMER;
            case BREED_ANIMALS -> BREEDER;
            case NONE, MINE_ORES, CHOP_WOOD, BUILD_STRUCTURE, CRAFT -> ARTISAN;
        };
    }
}
