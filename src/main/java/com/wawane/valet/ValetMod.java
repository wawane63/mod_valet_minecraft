package com.wawane.valet;

import com.google.common.collect.ImmutableSet;
import com.wawane.valet.construction.ConstructionBlueprintBlock;
import com.wawane.valet.construction.ConstructionBlueprintBlockEntity;
import com.wawane.valet.construction.ConstructionBlueprintItem;
import com.wawane.valet.construction.ConstructionBeaconBlock;
import com.wawane.valet.construction.ValetConstructionMarkers;
import com.wawane.valet.ai.ValetWorkDriver;
import com.wawane.valet.ai.ValetWorkGoal;
import com.wawane.valet.combat.InfiniteArrowChestBlock;
import com.wawane.valet.gui.ValetOrdersScreenHandler;
import com.wawane.valet.state.ValetData;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.world.poi.PointOfInterestHelper;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.mob.ZombieVillagerEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.village.VillagerProfession;
import net.minecraft.world.poi.PointOfInterestType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ValetMod implements ModInitializer {
    public static final String MOD_ID = "valet";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final int JOB_ASSIGN_INTERVAL = 40;
    private static final int PLAYER_SCAN_RADIUS = 48;
    private static final int WORKSTATION_SEARCH_RADIUS = 8;
    public static final Identifier VALET_WORKSTATION_ID = id("valet_workstation");
    public static final Identifier CONSTRUCTION_BEACON_ID = id("construction_beacon");
    public static final Identifier CONSTRUCTION_BLUEPRINT_ID = id("construction_blueprint");
    public static final Identifier INFINITE_ARROW_CHEST_ID = id("infinite_arrow_chest");
    public static final RegistryKey<PointOfInterestType> VALET_POI_KEY = RegistryKey.of(
            RegistryKeys.POINT_OF_INTEREST_TYPE,
            VALET_WORKSTATION_ID
    );

    public static final Block VALET_WORKSTATION = Registry.register(
            Registries.BLOCK,
            VALET_WORKSTATION_ID,
            new Block(FabricBlockSettings.copyOf(Blocks.CRAFTING_TABLE).strength(2.5F))
    );

    public static final Item VALET_WORKSTATION_ITEM = Registry.register(
            Registries.ITEM,
            VALET_WORKSTATION_ID,
            new BlockItem(VALET_WORKSTATION, new Item.Settings())
    );

    public static final Block CONSTRUCTION_BEACON = Registry.register(
            Registries.BLOCK,
            CONSTRUCTION_BEACON_ID,
            new ConstructionBeaconBlock(FabricBlockSettings.copyOf(Blocks.SCAFFOLDING).strength(0.8F))
    );

    public static final Item CONSTRUCTION_BEACON_ITEM = Registry.register(
            Registries.ITEM,
            CONSTRUCTION_BEACON_ID,
            new BlockItem(CONSTRUCTION_BEACON, new Item.Settings())
    );

    public static final Block CONSTRUCTION_BLUEPRINT = Registry.register(
            Registries.BLOCK,
            CONSTRUCTION_BLUEPRINT_ID,
            new ConstructionBlueprintBlock(FabricBlockSettings.copyOf(Blocks.SCAFFOLDING).strength(0.6F).nonOpaque())
    );

    public static final Item CONSTRUCTION_BLUEPRINT_ITEM = Registry.register(
            Registries.ITEM,
            CONSTRUCTION_BLUEPRINT_ID,
            new ConstructionBlueprintItem(CONSTRUCTION_BLUEPRINT, new Item.Settings().maxCount(1))
    );

    public static final Block INFINITE_ARROW_CHEST = Registry.register(
            Registries.BLOCK,
            INFINITE_ARROW_CHEST_ID,
            new InfiniteArrowChestBlock(FabricBlockSettings.copyOf(Blocks.CHEST).strength(2.5F))
    );

    public static final Item INFINITE_ARROW_CHEST_ITEM = Registry.register(
            Registries.ITEM,
            INFINITE_ARROW_CHEST_ID,
            new BlockItem(INFINITE_ARROW_CHEST, new Item.Settings())
    );

    public static final BlockEntityType<ConstructionBlueprintBlockEntity> CONSTRUCTION_BLUEPRINT_BLOCK_ENTITY = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            CONSTRUCTION_BLUEPRINT_ID,
            BlockEntityType.Builder.create(ConstructionBlueprintBlockEntity::new, CONSTRUCTION_BLUEPRINT).build(null)
    );

    public static final PointOfInterestType VALET_POI = PointOfInterestHelper.register(
            VALET_WORKSTATION_ID,
            1,
            1,
            VALET_WORKSTATION
    );

    public static final VillagerProfession VALET_PROFESSION = Registry.register(
            Registries.VILLAGER_PROFESSION,
            id("valet"),
            new VillagerProfession(
                    id("valet").toString(),
                    entry -> entry.matchesKey(VALET_POI_KEY),
                    entry -> entry.matchesKey(VALET_POI_KEY),
                    ImmutableSet.of(),
                    ImmutableSet.of(),
                    SoundEvents.ENTITY_VILLAGER_WORK_TOOLSMITH
            )
    );

    public static final ScreenHandlerType<ValetOrdersScreenHandler> VALET_ORDERS_SCREEN_HANDLER = Registry.register(
            Registries.SCREEN_HANDLER,
            id("orders"),
            new ExtendedScreenHandlerType<>(ValetOrdersScreenHandler::new)
    );

    public static Identifier id(String path) {
        return new Identifier(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(entries -> entries.add(VALET_WORKSTATION_ITEM));
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(entries -> entries.add(CONSTRUCTION_BEACON_ITEM));
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(entries -> entries.add(INFINITE_ARROW_CHEST_ITEM));
        UseEntityCallback.EVENT.register(ValetNetworking::openValetOrders);
        ServerEntityEvents.ENTITY_UNLOAD.register(ValetMod::clearEntityRuntimeState);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> clearAllRuntimeState());
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ValetConstructionMarkers.clear(handler.player.getUuid());
            ValetDebug.clear(handler.player.getUuid());
        });
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            assignValetJobs(world);
            ValetWorkDriver.tick(world);
            ValetDebug.tick(world);
        });
        ValetDebug.registerCommands();
        ValetNetworking.registerServerReceivers();
        LOGGER.info("Valet mod initialized");
    }

    private static void clearEntityRuntimeState(Entity entity, ServerWorld world) {
        if (entity instanceof VillagerEntity villager) {
            ValetWorkDriver.clear(villager.getUuid());
            ValetWorkGoal.clearRestartRequest(villager.getUuid());
            ValetConversations.clear(villager.getUuid());
        }
    }

    private static void clearAllRuntimeState() {
        ValetWorkDriver.clearAll();
        ValetData.clearAllVillagerRuntime();
        ValetConstructionMarkers.clearAll();
        ValetDebug.clearAll();
    }

    private static void assignValetJobs(ServerWorld world) {
        if (world.getTime() % JOB_ASSIGN_INTERVAL != 0) {
            return;
        }

        for (ServerPlayerEntity player : world.getPlayers()) {
            Box searchBox = Box.from(player.getPos()).expand(PLAYER_SCAN_RADIUS);
            normalizeValetZombies(world, searchBox);
            for (VillagerEntity villager : world.getEntitiesByClass(VillagerEntity.class, searchBox, ValetMod::shouldCheckValetJob)) {
                if (!restoreValetIdentity(world, villager)) {
                    tryAssignValetJob(world, villager, villager.getBlockPos());
                }
            }
        }
    }

    private static void normalizeValetZombies(ServerWorld world, Box searchBox) {
        for (ZombieVillagerEntity zombie : world.getEntitiesByClass(ZombieVillagerEntity.class, searchBox, zombie ->
                zombie.getVillagerData().getProfession() == VALET_PROFESSION)) {
            zombie.setVillagerData(zombie.getVillagerData().withProfession(VillagerProfession.NONE));
            LOGGER.info("Cleared valet profession from zombie villager {}", zombie.getUuid());
        }
    }

    public static boolean tryAssignValetJob(ServerWorld world, VillagerEntity villager, BlockPos searchOrigin) {
        if (villager.getVillagerData().getProfession() == VALET_PROFESSION) {
            return true;
        }
        if (!canBecomeValet(villager)) {
            return false;
        }

        BlockPos workstation = findNearbyWorkstation(world, villager.getBlockPos());
        if (workstation == null) {
            workstation = findNearbyWorkstation(world, searchOrigin);
        }
        if (workstation == null || isWorkstationClaimed(world, workstation, villager)) {
            return false;
        }

        villager.setVillagerData(villager.getVillagerData().withProfession(VALET_PROFESSION));
        villager.reinitializeBrain(world);
        villager.getBrain().remember(MemoryModuleType.JOB_SITE, GlobalPos.create(world.getRegistryKey(), workstation));
        ValetHome.set(villager, workstation);
        ValetDebug.record(villager, "profession_assigned home=" + ValetDebug.shortPos(workstation));
        return true;
    }

    private static boolean restoreValetIdentity(ServerWorld world, VillagerEntity villager) {
        if (villager.getVillagerData().getProfession() == VALET_PROFESSION) {
            ValetHome.get(world, villager);
            return true;
        }
        if (!ValetData.hasRuntimeData(villager)) {
            return false;
        }

        VillagerProfession previousProfession = villager.getVillagerData().getProfession();
        BlockPos home = ValetHome.get(world, villager);
        if (home == null) {
            ValetDebug.record(villager, "profession_lost previous=" + previousProfession + " home=missing");
            return false;
        }

        villager.setVillagerData(villager.getVillagerData().withProfession(VALET_PROFESSION));
        villager.reinitializeBrain(world);
        villager.getBrain().remember(MemoryModuleType.JOB_SITE, GlobalPos.create(world.getRegistryKey(), home));
        ValetWorkGoal.requestRestart(villager);
        ValetDebug.record(villager, "profession_restored previous=" + previousProfession + " home=" + ValetDebug.shortPos(home));
        LOGGER.warn("Restored valet profession for {} from {} at {}", villager.getUuid(), previousProfession, home);
        return true;
    }

    private static boolean shouldCheckValetJob(VillagerEntity villager) {
        return !villager.isBaby()
                && !villager.isSleeping()
                && (villager.getVillagerData().getProfession() == VALET_PROFESSION
                || villager.getVillagerData().getProfession() == VillagerProfession.NONE
                || ValetData.hasRuntimeData(villager));
    }

    private static boolean canBecomeValet(VillagerEntity villager) {
        return !villager.isBaby()
                && !villager.isSleeping()
                && villager.getVillagerData().getProfession() == VillagerProfession.NONE;
    }

    private static BlockPos findNearbyWorkstation(ServerWorld world, BlockPos origin) {
        for (BlockPos pos : BlockPos.iterateOutwards(origin, WORKSTATION_SEARCH_RADIUS, 2, WORKSTATION_SEARCH_RADIUS)) {
            if (world.getBlockState(pos).isOf(VALET_WORKSTATION)) {
                return pos.toImmutable();
            }
        }
        return null;
    }

    private static boolean isWorkstationClaimed(ServerWorld world, BlockPos workstation, VillagerEntity ignored) {
        Box box = new Box(workstation).expand(WORKSTATION_SEARCH_RADIUS);
        return !world.getEntitiesByClass(VillagerEntity.class, box, villager ->
                villager != ignored
                        && villager.getVillagerData().getProfession() == VALET_PROFESSION
                        && villager.getBrain().getOptionalMemory(MemoryModuleType.JOB_SITE)
                        .filter(pos -> pos.getDimension().equals(world.getRegistryKey()) && pos.getPos().equals(workstation))
                        .isPresent()
        ).isEmpty();
    }
}
