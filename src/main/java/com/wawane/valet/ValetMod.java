package com.wawane.valet;

import com.google.common.collect.ImmutableSet;
import com.wawane.valet.construction.ConstructionBlueprintBlock;
import com.wawane.valet.construction.ConstructionBlueprintBlockEntity;
import com.wawane.valet.construction.ConstructionBlueprintItem;
import com.wawane.valet.construction.ConstructionBeaconBlock;
import com.wawane.valet.construction.ValetConstructionMarkers;
import com.wawane.valet.ai.ValetWorkDriver;
import com.wawane.valet.ai.ValetWorkGoal;
import com.wawane.valet.ai.core.ValetBlockReservations;
import com.wawane.valet.combat.InfiniteArrowChestBlock;
import com.wawane.valet.farm.FarmBeaconBlock;
import com.wawane.valet.farm.ValetFarmMarkers;
import com.wawane.valet.gui.ValetOrdersScreenHandler;
import com.wawane.valet.order.ValetOrders;
import com.wawane.valet.state.ValetData;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.object.builder.v1.world.poi.PoiHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.AABB;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ValetMod implements ModInitializer {
    public static final String MOD_ID = "valet";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final int JOB_ASSIGN_INTERVAL = 40;
    private static final int PLAYER_SCAN_RADIUS = 48;
    private static final int WORKSTATION_SEARCH_RADIUS = 8;
    private static final java.util.Set<java.util.UUID> GLOWING_VALETS = java.util.concurrent.ConcurrentHashMap.newKeySet();
    public static final Identifier VALET_WORKSTATION_ID = id("valet_workstation");
    public static final Identifier COMBAT_WORKSTATION_ID = id("combat_workstation");
    public static final Identifier FARMER_WORKSTATION_ID = id("farmer_workstation");
    public static final Identifier CONSTRUCTION_BEACON_ID = id("construction_beacon");
    public static final Identifier FARM_BEACON_ID = id("farm_beacon");
    public static final Identifier CONSTRUCTION_BLUEPRINT_ID = id("construction_blueprint");
    public static final Identifier INFINITE_ARROW_CHEST_ID = id("infinite_arrow_chest");
    public static final ResourceKey<PoiType> VALET_POI_KEY = ResourceKey.create(
            Registries.POINT_OF_INTEREST_TYPE,
            VALET_WORKSTATION_ID
    );
    public static final ResourceKey<VillagerProfession> VALET_PROFESSION_KEY = ResourceKey.create(
            Registries.VILLAGER_PROFESSION,
            id("valet")
    );

    public static final Block VALET_WORKSTATION = Registry.register(
            BuiltInRegistries.BLOCK,
            VALET_WORKSTATION_ID,
            new Block(blockProperties(VALET_WORKSTATION_ID, BlockBehaviour.Properties.ofFullCopy(Blocks.CRAFTING_TABLE).strength(2.5F)))
    );

    public static final Item VALET_WORKSTATION_ITEM = Registry.register(
            BuiltInRegistries.ITEM,
            VALET_WORKSTATION_ID,
            new BlockItem(VALET_WORKSTATION, itemProperties(VALET_WORKSTATION_ID))
    );

    public static final Block COMBAT_WORKSTATION = Registry.register(
            BuiltInRegistries.BLOCK,
            COMBAT_WORKSTATION_ID,
            new Block(blockProperties(COMBAT_WORKSTATION_ID, BlockBehaviour.Properties.ofFullCopy(Blocks.SMITHING_TABLE).strength(2.5F)))
    );

    public static final Item COMBAT_WORKSTATION_ITEM = Registry.register(
            BuiltInRegistries.ITEM,
            COMBAT_WORKSTATION_ID,
            new BlockItem(COMBAT_WORKSTATION, itemProperties(COMBAT_WORKSTATION_ID))
    );

    public static final Block FARMER_WORKSTATION = Registry.register(
            BuiltInRegistries.BLOCK,
            FARMER_WORKSTATION_ID,
            new Block(blockProperties(FARMER_WORKSTATION_ID, BlockBehaviour.Properties.ofFullCopy(Blocks.COMPOSTER).strength(0.8F)))
    );

    public static final Item FARMER_WORKSTATION_ITEM = Registry.register(
            BuiltInRegistries.ITEM,
            FARMER_WORKSTATION_ID,
            new BlockItem(FARMER_WORKSTATION, itemProperties(FARMER_WORKSTATION_ID))
    );

    public static final Block CONSTRUCTION_BEACON = Registry.register(
            BuiltInRegistries.BLOCK,
            CONSTRUCTION_BEACON_ID,
            new ConstructionBeaconBlock(blockProperties(CONSTRUCTION_BEACON_ID, BlockBehaviour.Properties.ofFullCopy(Blocks.SCAFFOLDING).strength(0.8F)))
    );

    public static final Item CONSTRUCTION_BEACON_ITEM = Registry.register(
            BuiltInRegistries.ITEM,
            CONSTRUCTION_BEACON_ID,
            new BlockItem(CONSTRUCTION_BEACON, itemProperties(CONSTRUCTION_BEACON_ID))
    );

    public static final Block FARM_BEACON = Registry.register(
            BuiltInRegistries.BLOCK,
            FARM_BEACON_ID,
            new FarmBeaconBlock(blockProperties(FARM_BEACON_ID, BlockBehaviour.Properties.ofFullCopy(Blocks.SCAFFOLDING).strength(0.8F)))
    );

    public static final Item FARM_BEACON_ITEM = Registry.register(
            BuiltInRegistries.ITEM,
            FARM_BEACON_ID,
            new BlockItem(FARM_BEACON, itemProperties(FARM_BEACON_ID))
    );

    public static final Block CONSTRUCTION_BLUEPRINT = Registry.register(
            BuiltInRegistries.BLOCK,
            CONSTRUCTION_BLUEPRINT_ID,
            new ConstructionBlueprintBlock(blockProperties(CONSTRUCTION_BLUEPRINT_ID, BlockBehaviour.Properties.ofFullCopy(Blocks.SCAFFOLDING).strength(0.6F).noOcclusion()))
    );

    public static final Item CONSTRUCTION_BLUEPRINT_ITEM = Registry.register(
            BuiltInRegistries.ITEM,
            CONSTRUCTION_BLUEPRINT_ID,
            new ConstructionBlueprintItem(CONSTRUCTION_BLUEPRINT, itemProperties(CONSTRUCTION_BLUEPRINT_ID).stacksTo(1))
    );

    public static final Block INFINITE_ARROW_CHEST = Registry.register(
            BuiltInRegistries.BLOCK,
            INFINITE_ARROW_CHEST_ID,
            new InfiniteArrowChestBlock(blockProperties(INFINITE_ARROW_CHEST_ID, BlockBehaviour.Properties.ofFullCopy(Blocks.CHEST).strength(2.5F)))
    );

    public static final Item INFINITE_ARROW_CHEST_ITEM = Registry.register(
            BuiltInRegistries.ITEM,
            INFINITE_ARROW_CHEST_ID,
            new BlockItem(INFINITE_ARROW_CHEST, itemProperties(INFINITE_ARROW_CHEST_ID))
    );

    public static final BlockEntityType<ConstructionBlueprintBlockEntity> CONSTRUCTION_BLUEPRINT_BLOCK_ENTITY = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            CONSTRUCTION_BLUEPRINT_ID,
            new BlockEntityType<>(ConstructionBlueprintBlockEntity::new, java.util.Set.of(CONSTRUCTION_BLUEPRINT))
    );

    public static final PoiType VALET_POI = PoiHelper.register(
            VALET_WORKSTATION_ID,
            1,
            1,
            VALET_WORKSTATION,
            COMBAT_WORKSTATION,
            FARMER_WORKSTATION
    );

    public static final VillagerProfession VALET_PROFESSION = Registry.register(
            BuiltInRegistries.VILLAGER_PROFESSION,
            id("valet"),
            new VillagerProfession(
                    Component.translatable("profession.valet.valet"),
                    entry -> entry.is(VALET_POI_KEY),
                    entry -> entry.is(VALET_POI_KEY),
                    ImmutableSet.of(),
                    ImmutableSet.of(),
                    SoundEvents.VILLAGER_WORK_TOOLSMITH,
                    new Int2ObjectOpenHashMap<>()
            )
    );

    public static final MenuType<ValetOrdersScreenHandler> VALET_ORDERS_SCREEN_HANDLER = Registry.register(
            BuiltInRegistries.MENU,
            id("orders"),
            new ExtendedMenuType<>(ValetOrdersScreenHandler::new, ValetOrdersScreenHandler.OpeningData.CODEC)
    );

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    private static BlockBehaviour.Properties blockProperties(Identifier id, BlockBehaviour.Properties properties) {
        return properties.setId(ResourceKey.create(Registries.BLOCK, id));
    }

    private static Item.Properties itemProperties(Identifier id) {
        return new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id));
    }

    @Override
    public void onInitialize() {
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(entries -> entries.accept(VALET_WORKSTATION_ITEM));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(entries -> entries.accept(COMBAT_WORKSTATION_ITEM));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(entries -> entries.accept(FARMER_WORKSTATION_ITEM));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(entries -> entries.accept(CONSTRUCTION_BEACON_ITEM));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(entries -> entries.accept(FARM_BEACON_ITEM));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(entries -> entries.accept(INFINITE_ARROW_CHEST_ITEM));
        UseEntityCallback.EVENT.register(ValetNetworking::openValetOrders);
        ServerEntityEvents.ENTITY_UNLOAD.register(ValetMod::clearEntityRuntimeState);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> clearAllRuntimeState());
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ValetConstructionMarkers.clear(handler.player.getUUID());
            ValetFarmMarkers.clear(handler.player.getUUID());
            ValetDebug.clear(handler.player.getUUID());
        });
        ServerTickEvents.END_LEVEL_TICK.register(world -> {
            assignValetJobs(world);
            ValetWorkDriver.tick(world);
            ValetDebug.tick(world);
        });
        ValetDebug.registerCommands();
        ValetNetworking.registerServerReceivers();
        LOGGER.info("Valet mod initialized");
    }

    private static void clearEntityRuntimeState(Entity entity, ServerLevel world) {
        if (entity instanceof Villager villager) {
            ValetWorkDriver.clear(villager.getUUID());
            ValetWorkGoal.clearRestartRequest(villager.getUUID());
            ValetConversations.clear(villager.getUUID());
            ValetBlockReservations.releaseAll(villager.getUUID());
        }
    }

    private static void clearAllRuntimeState() {
        ValetWorkDriver.clearAll();
        ValetData.clearAllVillagerRuntime();
        ValetConstructionMarkers.clearAll();
        ValetFarmMarkers.clearAll();
        ValetDebug.clearAll();
        ValetBlockReservations.clearAll();
        GLOWING_VALETS.clear();
    }

    private static void assignValetJobs(ServerLevel world) {
        if (world.getGameTime() % JOB_ASSIGN_INTERVAL != 0) {
            return;
        }

        java.util.Set<java.util.UUID> checkedVillagers = new java.util.HashSet<>();
        for (ServerPlayer player : world.players()) {
            AABB searchBox = AABB.unitCubeFromLowerCorner(player.position()).inflate(PLAYER_SCAN_RADIUS);
            normalizeValetZombies(world, searchBox);
            for (Villager villager : world.getEntitiesOfClass(Villager.class, searchBox, ValetMod::shouldCheckValetJob)) {
                if (!checkedVillagers.add(villager.getUUID())) {
                    continue;
                }
                if (!restoreValetIdentity(world, villager)) {
                    tryAssignValetJob(world, villager, villager.blockPosition());
                }
            }
        }
    }

    private static void normalizeValetZombies(ServerLevel world, AABB searchBox) {
        for (ZombieVillager zombie : world.getEntitiesOfClass(ZombieVillager.class, searchBox, zombie ->
                isValet(zombie.getVillagerData()))) {
            zombie.setVillagerData(zombie.getVillagerData().withProfession(world.registryAccess(), VillagerProfession.NONE));
            LOGGER.info("Cleared valet profession from zombie villager {}", zombie.getUUID());
        }
    }

    public static boolean tryAssignValetJob(ServerLevel world, Villager villager, BlockPos searchOrigin) {
        if (isValet(villager)) {
            return true;
        }
        if (!canBecomeValet(villager)) {
            return false;
        }

        BlockPos workstation = claimNearbyWorkstation(world, villager, villager.blockPosition());
        if (workstation == null && !searchOrigin.equals(villager.blockPosition())) {
            workstation = claimNearbyWorkstation(world, villager, searchOrigin);
        }
        if (workstation == null) {
            return false;
        }

        villager.setVillagerData(villager.getVillagerData().withProfession(world.registryAccess(), VALET_PROFESSION_KEY));
        villager.refreshBrain(world);
        ValetHome.set(villager, workstation);
        ValetDebug.record(villager, "profession_assigned role=" + ValetRole.fromWorkstation(world.getBlockState(workstation)).name().toLowerCase() + " home=" + ValetDebug.shortPos(workstation));
        return true;
    }

    public static BlockPos claimOrRecoverValetHome(ServerLevel world, Villager villager, BlockPos recoveryOrigin) {
        if (!isValet(villager)) {
            return null;
        }

        BlockPos home = ValetHome.get(world, villager);
        if (home != null) {
            if (!claimValetJobSite(world, villager, home)) {
                return null;
            }
            ValetHome.set(villager, home);
            return home;
        }

        BlockPos workstation = claimNearbyWorkstation(world, villager, recoveryOrigin);
        if (workstation == null) {
            return null;
        }
        ValetHome.set(villager, workstation);
        return workstation;
    }

    private static boolean restoreValetIdentity(ServerLevel world, Villager villager) {
        if (isValet(villager)) {
            BlockPos home = ValetHome.get(world, villager);
            if (home == null) {
                loseValetJob(world, villager, "home_missing");
                return false;
            }
            if (!claimValetJobSite(world, villager, home)) {
                loseValetJob(world, villager, "job_site_claimed");
                return false;
            }
            return true;
        }
        if (!ValetData.hasRuntimeData(villager)) {
            if (GLOWING_VALETS.contains(villager.getUUID())) {
                clearValetGlow(villager);
                ValetDebug.record(villager, "glow_removed reason=profession_lost_no_runtime");
            }
            return false;
        }

        String previousProfession = villager.getVillagerData().profession().toString();
        BlockPos home = ValetHome.get(world, villager);
        if (home == null) {
            clearFormerValetRuntime(world, villager, "home_missing_after_profession_loss");
            ValetDebug.record(villager, "profession_lost previous=" + previousProfession + " home=missing");
            return false;
        }

        if (!claimValetJobSite(world, villager, home)) {
            clearFormerValetRuntime(world, villager, "restore_blocked_after_profession_loss");
            ValetDebug.record(villager, "profession_restore_blocked home=" + ValetDebug.shortPos(home));
            return false;
        }
        villager.setVillagerData(villager.getVillagerData().withProfession(world.registryAccess(), VALET_PROFESSION_KEY));
        villager.refreshBrain(world);
        ValetWorkGoal.requestRestart(villager);
        ValetDebug.record(villager, "profession_restored previous=" + previousProfession + " home=" + ValetDebug.shortPos(home));
        LOGGER.warn("Restored valet profession for {} from {} at {}", villager.getUUID(), previousProfession, home);
        return true;
    }

    private static void loseValetJob(ServerLevel world, Villager villager, String reason) {
        clearValetGlow(villager);
        villager.getBrain().getMemoryInternal(MemoryModuleType.JOB_SITE)
                .filter(pos -> pos.dimension().equals(world.dimension()))
                .ifPresent(pos -> world.getPoiManager().release(pos.pos()));
        villager.getBrain().eraseMemory(MemoryModuleType.JOB_SITE);
        villager.setVillagerData(villager.getVillagerData().withProfession(world.registryAccess(), VillagerProfession.NONE));
        villager.refreshBrain(world);
        ValetWorkDriver.clear(villager.getUUID());
        ValetWorkGoal.clearRestartRequest(villager.getUUID());
        ValetConversations.clear(villager.getUUID());
        ValetHome.clear(villager.getUUID());
        ValetOrders.clear(villager.getUUID());
        ValetBlockReservations.releaseAll(villager.getUUID());
        ValetDebug.record(villager, "profession_removed reason=" + reason);
        LOGGER.info("Removed valet profession from {} reason={}", villager.getUUID(), reason);
    }

    private static void clearFormerValetRuntime(ServerLevel world, Villager villager, String reason) {
        clearValetGlow(villager);
        villager.getBrain().getMemoryInternal(MemoryModuleType.JOB_SITE)
                .filter(pos -> pos.dimension().equals(world.dimension()))
                .ifPresent(pos -> world.getPoiManager().release(pos.pos()));
        villager.getBrain().eraseMemory(MemoryModuleType.JOB_SITE);
        ValetWorkDriver.clear(villager.getUUID());
        ValetWorkGoal.clearRestartRequest(villager.getUUID());
        ValetConversations.clear(villager.getUUID());
        ValetData.clearVillagerRuntime(villager.getUUID());
        ValetBlockReservations.releaseAll(villager.getUUID());
        ValetDebug.record(villager, "former_valet_runtime_cleared reason=" + reason);
    }

    public static void markValetGlow(Villager villager) {
        GLOWING_VALETS.add(villager.getUUID());
    }

    private static void clearValetGlow(Villager villager) {
        villager.removeEffect(MobEffects.GLOWING);
        villager.setGlowingTag(false);
        GLOWING_VALETS.remove(villager.getUUID());
    }

    private static boolean shouldCheckValetJob(Villager villager) {
        return !villager.isBaby()
                && !villager.isSleeping()
                && (isValet(villager)
                || isUnemployed(villager)
                || ValetData.hasRuntimeData(villager)
                || GLOWING_VALETS.contains(villager.getUUID()));
    }

    private static boolean canBecomeValet(Villager villager) {
        return !villager.isBaby()
                && !villager.isSleeping()
                && isUnemployed(villager);
    }

    private static BlockPos claimNearbyWorkstation(ServerLevel world, Villager villager, BlockPos origin) {
        for (BlockPos pos : BlockPos.withinManhattan(origin, WORKSTATION_SEARCH_RADIUS, 2, WORKSTATION_SEARCH_RADIUS)) {
            BlockPos workstation = pos.immutable();
            if (isValetWorkstation(world.getBlockState(workstation)) && claimValetJobSite(world, villager, workstation)) {
                return workstation;
            }
        }
        return null;
    }

    private static boolean isWorkstationClaimed(ServerLevel world, BlockPos workstation, Villager ignored) {
        if (ValetHome.isClaimedHome(world, workstation, ignored.getUUID())) {
            return true;
        }
        AABB box = new AABB(workstation).inflate(PLAYER_SCAN_RADIUS);
        return !world.getEntitiesOfClass(Villager.class, box, villager ->
                villager != ignored
                        && (ValetMod.isValet(villager) || ValetData.hasRuntimeData(villager))
                        && (hasJobSite(villager, world, workstation) || ValetHome.isHome(world, villager, workstation))
        ).isEmpty();
    }

    private static boolean hasJobSite(Villager villager, ServerLevel world, BlockPos workstation) {
        return villager.getBrain().getMemoryInternal(MemoryModuleType.JOB_SITE)
                .filter(pos -> pos.dimension().equals(world.dimension()) && pos.pos().equals(workstation))
                .isPresent();
    }

    private static void clearNonValetWorkstationClaims(ServerLevel world, BlockPos workstation, Villager ignored) {
        AABB box = new AABB(workstation).inflate(PLAYER_SCAN_RADIUS);
        for (Villager villager : world.getEntitiesOfClass(Villager.class, box, candidate ->
                candidate != ignored
                        && !ValetMod.isValet(candidate)
                        && !ValetData.hasRuntimeData(candidate)
                        && hasJobSite(candidate, world, workstation))) {
            villager.getBrain().eraseMemory(MemoryModuleType.JOB_SITE);
            world.getPoiManager().release(workstation);
            ValetDebug.record(villager, "idle_job_site_cleared home=" + ValetDebug.shortPos(workstation));
        }
    }

    private static boolean claimValetJobSite(ServerLevel world, Villager villager, BlockPos workstation) {
        if (!isValetWorkstation(world.getBlockState(workstation))) {
            return false;
        }
        if (!world.getPoiManager().existsAtPosition(VALET_POI_KEY, workstation)) {
            world.getPoiManager().add(workstation, BuiltInRegistries.POINT_OF_INTEREST_TYPE.wrapAsHolder(VALET_POI));
        }
        clearNonValetWorkstationClaims(world, workstation, villager);
        if (isWorkstationClaimed(world, workstation, villager)) {
            return false;
        }
        boolean alreadyAssigned = hasJobSite(villager, world, workstation);
        if (alreadyAssigned) {
            world.getPoiManager().take(
                    entry -> entry.is(VALET_POI_KEY),
                    (entry, pos) -> pos.equals(workstation),
                    workstation,
                    1
            );
        } else {
            java.util.Optional<BlockPos> claimed = world.getPoiManager().take(
                    entry -> entry.is(VALET_POI_KEY),
                    (entry, pos) -> pos.equals(workstation),
                    workstation,
                    1
            );
            if (claimed.isEmpty()) {
                return false;
            }
        }
        villager.getBrain().setMemory(MemoryModuleType.JOB_SITE, GlobalPos.of(world.dimension(), workstation));
        return true;
    }

    public static boolean isValet(Villager villager) {
        return isValet(villager.getVillagerData());
    }

    public static boolean isValetWorkstation(net.minecraft.world.level.block.state.BlockState state) {
        return state.is(VALET_WORKSTATION) || state.is(COMBAT_WORKSTATION) || state.is(FARMER_WORKSTATION);
    }

    public static boolean isValet(net.minecraft.world.entity.npc.villager.VillagerData data) {
        return data.profession().is(VALET_PROFESSION_KEY);
    }

    public static boolean isUnemployed(Villager villager) {
        return villager.getVillagerData().profession().is(VillagerProfession.NONE);
    }
}
