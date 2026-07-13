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
import com.wawane.valet.ai.core.ValetEntityReservations;
import com.wawane.valet.breeding.AnimalBeaconBlock;
import com.wawane.valet.breeding.ValetAnimalMarkers;
import com.wawane.valet.combat.InfiniteArrowChestBlock;
import com.wawane.valet.cooking.CookChestBlock;
import com.wawane.valet.cooking.CookChestBlockEntity;
import com.wawane.valet.farm.FarmBeaconBlock;
import com.wawane.valet.farm.ValetFarmMarkers;
import com.wawane.valet.group.ValetGroupRuntime;
import com.wawane.valet.group.ValetGroupTravelTickets;
import com.wawane.valet.gui.ValetOrdersScreenHandler;
import com.wawane.valet.order.ValetOrders;
import com.wawane.valet.state.ValetBehavior;
import com.wawane.valet.state.ValetData;
import com.wawane.valet.state.ValetIdentity;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
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
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ValetMod implements ModInitializer {
    public static final String MOD_ID = "valet";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final int JOB_ASSIGN_INTERVAL = 40;
    private static final int PLAYER_SCAN_RADIUS = 48;
    private static final int WORKSTATION_RECALL_RADIUS = 128;
    private static final int WORKSTATION_SEARCH_RADIUS = 8;
    private static final Set<UUID> GLOWING_VALETS = ConcurrentHashMap.newKeySet();
    public static final Identifier VALET_WORKSTATION_ID = id("valet_workstation");
    public static final Identifier COMBAT_WORKSTATION_ID = id("combat_workstation");
    public static final Identifier FARMER_WORKSTATION_ID = id("farmer_workstation");
    public static final Identifier ANIMAL_WORKSTATION_ID = id("poste_eleveur");
    public static final Identifier MAGIC_WORKSTATION_ID = id("magic_workstation");
    public static final Identifier COOK_WORKSTATION_ID = id("cook_workstation");
    public static final Identifier STEWARD_WORKSTATION_ID = id("steward_workstation");
    public static final Identifier COOK_CHEST_ID = id("cook_chest");
    public static final Identifier CONSTRUCTION_BEACON_ID = id("construction_beacon");
    public static final Identifier FARM_BEACON_ID = id("farm_beacon");
    public static final Identifier ANIMAL_BEACON_ID = id("animal_beacon");
    public static final Identifier CONSTRUCTION_BLUEPRINT_ID = id("construction_blueprint");
    public static final Identifier INFINITE_ARROW_CHEST_ID = id("infinite_arrow_chest");
    public static final Identifier VALET_TAG_ID = id("valet_tag");
    public static final Identifier VALET_GROUP_MISSION_TICKET_ID = id("group_mission");
    public static final TicketType VALET_GROUP_MISSION_TICKET = Registry.register(
            BuiltInRegistries.TICKET_TYPE,
            VALET_GROUP_MISSION_TICKET_ID,
            new TicketType(TicketType.NO_TIMEOUT, TicketType.FLAG_LOADING | TicketType.FLAG_SIMULATION | TicketType.FLAG_KEEP_DIMENSION_ACTIVE)
    );
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

    public static final Block ANIMAL_WORKSTATION = Registry.register(
            BuiltInRegistries.BLOCK,
            ANIMAL_WORKSTATION_ID,
            new Block(blockProperties(ANIMAL_WORKSTATION_ID, BlockBehaviour.Properties.ofFullCopy(Blocks.CRAFTING_TABLE).strength(2.5F)))
    );

    public static final Item ANIMAL_WORKSTATION_ITEM = Registry.register(
            BuiltInRegistries.ITEM,
            ANIMAL_WORKSTATION_ID,
            new BlockItem(ANIMAL_WORKSTATION, itemProperties(ANIMAL_WORKSTATION_ID))
    );

    public static final Block MAGIC_WORKSTATION = Registry.register(
            BuiltInRegistries.BLOCK,
            MAGIC_WORKSTATION_ID,
            new Block(blockProperties(MAGIC_WORKSTATION_ID, BlockBehaviour.Properties.ofFullCopy(Blocks.ENCHANTING_TABLE).strength(2.5F)))
    );

    public static final Item MAGIC_WORKSTATION_ITEM = Registry.register(
            BuiltInRegistries.ITEM,
            MAGIC_WORKSTATION_ID,
            new BlockItem(MAGIC_WORKSTATION, itemProperties(MAGIC_WORKSTATION_ID))
    );

    public static final Block COOK_WORKSTATION = Registry.register(
            BuiltInRegistries.BLOCK,
            COOK_WORKSTATION_ID,
            new Block(blockProperties(COOK_WORKSTATION_ID, BlockBehaviour.Properties.ofFullCopy(Blocks.CRAFTING_TABLE).strength(2.5F)))
    );

    public static final Item COOK_WORKSTATION_ITEM = Registry.register(
            BuiltInRegistries.ITEM,
            COOK_WORKSTATION_ID,
            new BlockItem(COOK_WORKSTATION, itemProperties(COOK_WORKSTATION_ID))
    );

    public static final Block STEWARD_WORKSTATION = Registry.register(
            BuiltInRegistries.BLOCK,
            STEWARD_WORKSTATION_ID,
            new Block(blockProperties(STEWARD_WORKSTATION_ID, BlockBehaviour.Properties.ofFullCopy(Blocks.BARREL).strength(2.5F)))
    );

    public static final Item STEWARD_WORKSTATION_ITEM = Registry.register(
            BuiltInRegistries.ITEM,
            STEWARD_WORKSTATION_ID,
            new BlockItem(STEWARD_WORKSTATION, itemProperties(STEWARD_WORKSTATION_ID))
    );

    public static final Block COOK_CHEST = Registry.register(
            BuiltInRegistries.BLOCK,
            COOK_CHEST_ID,
            new CookChestBlock(blockProperties(COOK_CHEST_ID, BlockBehaviour.Properties.ofFullCopy(Blocks.BARREL).strength(2.5F)))
    );

    public static final Item COOK_CHEST_ITEM = Registry.register(
            BuiltInRegistries.ITEM,
            COOK_CHEST_ID,
            new BlockItem(COOK_CHEST, itemProperties(COOK_CHEST_ID))
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

    public static final Block ANIMAL_BEACON = Registry.register(
            BuiltInRegistries.BLOCK,
            ANIMAL_BEACON_ID,
            new AnimalBeaconBlock(blockProperties(ANIMAL_BEACON_ID, BlockBehaviour.Properties.ofFullCopy(Blocks.SCAFFOLDING).strength(0.8F)))
    );

    public static final Item ANIMAL_BEACON_ITEM = Registry.register(
            BuiltInRegistries.ITEM,
            ANIMAL_BEACON_ID,
            new BlockItem(ANIMAL_BEACON, itemProperties(ANIMAL_BEACON_ID))
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

    public static final Item VALET_TAG_ITEM = Registry.register(
            BuiltInRegistries.ITEM,
            VALET_TAG_ID,
            new Item(itemProperties(VALET_TAG_ID).stacksTo(16))
    );

    public static final BlockEntityType<ConstructionBlueprintBlockEntity> CONSTRUCTION_BLUEPRINT_BLOCK_ENTITY = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            CONSTRUCTION_BLUEPRINT_ID,
            new BlockEntityType<>(ConstructionBlueprintBlockEntity::new, Set.of(CONSTRUCTION_BLUEPRINT))
    );

    public static final BlockEntityType<CookChestBlockEntity> COOK_CHEST_BLOCK_ENTITY = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            COOK_CHEST_ID,
            new BlockEntityType<>(CookChestBlockEntity::new, Set.of(COOK_CHEST))
    );

    public static final PoiType VALET_POI = PoiHelper.register(
            VALET_WORKSTATION_ID,
            1,
            1,
            VALET_WORKSTATION,
            COMBAT_WORKSTATION,
            FARMER_WORKSTATION,
            ANIMAL_WORKSTATION,
            MAGIC_WORKSTATION,
            COOK_WORKSTATION,
            STEWARD_WORKSTATION
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
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(entries -> {
            entries.accept(VALET_WORKSTATION_ITEM);
            entries.accept(COMBAT_WORKSTATION_ITEM);
            entries.accept(FARMER_WORKSTATION_ITEM);
            entries.accept(ANIMAL_WORKSTATION_ITEM);
            entries.accept(MAGIC_WORKSTATION_ITEM);
            entries.accept(COOK_WORKSTATION_ITEM);
            entries.accept(STEWARD_WORKSTATION_ITEM);
            entries.accept(COOK_CHEST_ITEM);
            entries.accept(CONSTRUCTION_BEACON_ITEM);
            entries.accept(FARM_BEACON_ITEM);
            entries.accept(ANIMAL_BEACON_ITEM);
            entries.accept(INFINITE_ARROW_CHEST_ITEM);
        });
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries -> entries.accept(VALET_TAG_ITEM));
        UseEntityCallback.EVENT.register(ValetMod::tagVillagerAsValet);
        UseEntityCallback.EVENT.register(ValetNetworking::openValetOrders);
        UseBlockCallback.EVENT.register(ValetMod::recallValetAtWorkstation);
        ServerEntityEvents.ENTITY_UNLOAD.register(ValetMod::clearEntityRuntimeState);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> clearAllRuntimeState());
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ValetConstructionMarkers.clear(handler.player.getUUID());
            ValetFarmMarkers.clear(handler.player.getUUID());
            ValetAnimalMarkers.clear(handler.player.getUUID());
            ValetDebug.clear(handler.player.getUUID());
        });
        ServerTickEvents.END_LEVEL_TICK.register(world -> {
            assignValetJobs(world);
            ValetGroupTravelTickets.tick(world);
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
            ValetEntityReservations.releaseAll(villager.getUUID());
            ValetGroupRuntime.clear(villager.getUUID());
            ValetBehavior.clearRecall(villager.getUUID());
            GLOWING_VALETS.remove(villager.getUUID());
            if (entity.getRemovalReason() != null && entity.getRemovalReason().shouldDestroy()) {
                ValetData.clearVillagerRuntime(villager.getUUID());
            }
        }
    }

    private static void clearAllRuntimeState() {
        ValetWorkDriver.clearAll();
        ValetData.clearAllVillagerRuntime();
        ValetConstructionMarkers.clearAll();
        ValetFarmMarkers.clearAll();
        ValetAnimalMarkers.clearAll();
        ValetDebug.clearAll();
        ValetBlockReservations.clearAll();
        ValetEntityReservations.clearAll();
        ValetGroupRuntime.clearAll();
        ValetGroupTravelTickets.clearAll();
        GLOWING_VALETS.clear();
    }

    private static void assignValetJobs(ServerLevel world) {
        if (world.getGameTime() % JOB_ASSIGN_INTERVAL != 0) {
            return;
        }

        Set<UUID> checkedVillagers = new HashSet<>();
        for (ServerPlayer player : world.players()) {
            AABB searchBox = AABB.unitCubeFromLowerCorner(player.position()).inflate(PLAYER_SCAN_RADIUS);
            normalizeValetZombies(world, searchBox);
            for (Villager villager : world.getEntitiesOfClass(Villager.class, searchBox, ValetMod::shouldCheckValetJob)) {
                if (!checkedVillagers.add(villager.getUUID())) {
                    continue;
                }
                restoreValetIdentity(world, villager);
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
            suppressVanillaVillageMemoriesIfNeeded(world, villager);
            return home;
        }

        BlockPos workstation = claimNearbyWorkstation(world, villager, recoveryOrigin);
        if (workstation == null) {
            return null;
        }
        ValetHome.set(villager, workstation);
        suppressVanillaVillageMemoriesIfNeeded(world, villager);
        return workstation;
    }

    private static boolean restoreValetIdentity(ServerLevel world, Villager villager) {
        if (!ValetIdentity.isTagged(villager) && isValet(villager.getVillagerData())) {
            ValetIdentity.tag(villager);
            ValetRole.set(villager, ValetRole.get(world, villager));
            ValetDebug.record(villager, "identity_migrated_from_profession");
        }
        if (ValetIdentity.isTagged(villager)) {
            suppressVanillaVillageMemoriesIfNeeded(world, villager);
            return true;
        }
        if (ValetData.hasRuntimeData(villager)) {
            ValetIdentity.tag(villager);
            suppressVanillaVillageMemoriesIfNeeded(world, villager);
            ValetDebug.record(villager, "identity_migrated_from_runtime");
            return true;
        }
        return false;
    }

    public static void markValetGlow(Villager villager) {
        GLOWING_VALETS.add(villager.getUUID());
    }

    private static void clearValetGlow(Villager villager) {
        villager.removeEffect(MobEffects.GLOWING);
        villager.setGlowingTag(false);
        GLOWING_VALETS.remove(villager.getUUID());
    }

    private static void clearValetHeldItem(Villager villager) {
        if (!villager.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty()) {
            villager.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        }
    }

    private static boolean shouldCheckValetJob(Villager villager) {
        return !villager.isBaby()
                && !villager.isSleeping()
                && (isValet(villager)
                || ValetData.hasRuntimeData(villager)
                || GLOWING_VALETS.contains(villager.getUUID()));
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
        AABB box = new AABB(workstation).inflate(PLAYER_SCAN_RADIUS);
        return !world.getEntitiesOfClass(Villager.class, box, villager ->
                villager != ignored
                        && villager.isAlive()
                        && !villager.isRemoved()
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
        boolean alreadyAssigned = hasJobSite(villager, world, workstation);
        clearNonValetWorkstationClaims(world, workstation, villager);
        if (isWorkstationClaimed(world, workstation, villager)) {
            return false;
        }
        if (alreadyAssigned && ValetHome.isHome(world, villager, workstation)) {
            suppressVanillaVillageMemoriesIfNeeded(world, villager);
            return true;
        }
        if (alreadyAssigned) {
            villager.getBrain().setMemory(MemoryModuleType.JOB_SITE, GlobalPos.of(world.dimension(), workstation));
            suppressVanillaVillageMemoriesIfNeeded(world, villager);
            return true;
        } else {
            Optional<BlockPos> claimed = world.getPoiManager().take(
                    entry -> entry.is(VALET_POI_KEY),
                    (entry, pos) -> pos.equals(workstation),
                    workstation,
                    1
            );
            if (claimed.isEmpty()) {
                world.getPoiManager().release(workstation);
                claimed = world.getPoiManager().take(
                        entry -> entry.is(VALET_POI_KEY),
                        (entry, pos) -> pos.equals(workstation),
                        workstation,
                        1
                );
                if (claimed.isEmpty()) {
                    return false;
                }
                ValetDebug.record(villager, "stale_job_site_reclaimed home=" + ValetDebug.shortPos(workstation));
            }
        }
        villager.getBrain().setMemory(MemoryModuleType.JOB_SITE, GlobalPos.of(world.dimension(), workstation));
        suppressVanillaVillageMemoriesIfNeeded(world, villager);
        return true;
    }

    private static void suppressVanillaVillageMemoriesIfNeeded(ServerLevel world, Villager villager) {
        if (!ValetBehavior.shouldUseVanillaBehavior(world, villager)) {
            suppressVanillaVillageMemories(villager);
        }
    }

    public static void suppressVanillaVillageMemories(Villager villager) {
        villager.getBrain().eraseMemory(MemoryModuleType.MEETING_POINT);
        villager.getBrain().eraseMemory(MemoryModuleType.POTENTIAL_JOB_SITE);
        villager.getBrain().eraseMemory(MemoryModuleType.SECONDARY_JOB_SITE);
        villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        villager.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
        villager.getBrain().eraseMemory(MemoryModuleType.PATH);
    }

    public static boolean isValet(Villager villager) {
        return ValetIdentity.isTagged(villager) || isValet(villager.getVillagerData());
    }

    private static InteractionResult tagVillagerAsValet(Player player, net.minecraft.world.level.Level world, InteractionHand hand, Entity entity, net.minecraft.world.phys.EntityHitResult hitResult) {
        if (hand != InteractionHand.MAIN_HAND || !(entity instanceof Villager villager) || !player.getItemInHand(hand).is(VALET_TAG_ITEM)) {
            return InteractionResult.PASS;
        }
        if (villager.isBaby()) {
            return InteractionResult.FAIL;
        }
        if (!world.isClientSide() && world instanceof ServerLevel serverWorld) {
            ValetIdentity.tag(villager);
            ValetRole.set(villager, ValetRole.ARTISAN);
            suppressVanillaVillageMemoriesIfNeeded(serverWorld, villager);
            ValetWorkGoal.requestRestart(villager);
            if (!player.getAbilities().instabuild) {
                player.getItemInHand(hand).shrink(1);
            }
            player.sendOverlayMessage(Component.translatable("message.valet.tagged"));
            ValetDebug.record(villager, "identity_tagged role=artisan");
        }
        return InteractionResult.SUCCESS;
    }

    public static boolean isValetWorkstation(net.minecraft.world.level.block.state.BlockState state) {
        return state.is(VALET_WORKSTATION)
                || state.is(COMBAT_WORKSTATION)
                || state.is(FARMER_WORKSTATION)
                || state.is(ANIMAL_WORKSTATION)
                || state.is(MAGIC_WORKSTATION)
                || state.is(COOK_WORKSTATION)
                || state.is(STEWARD_WORKSTATION);
    }

    public static boolean isValet(net.minecraft.world.entity.npc.villager.VillagerData data) {
        return data.profession().is(VALET_PROFESSION_KEY);
    }

    private static InteractionResult recallValetAtWorkstation(Player player, net.minecraft.world.level.Level world, InteractionHand hand, BlockHitResult hitResult) {
        if (hand != InteractionHand.MAIN_HAND || !isValetWorkstation(world.getBlockState(hitResult.getBlockPos()))) {
            return InteractionResult.PASS;
        }
        if (world.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(world instanceof ServerLevel serverWorld)) {
            return InteractionResult.SUCCESS;
        }

        BlockPos workstation = hitResult.getBlockPos();
        Villager villager = findValetForWorkstation(serverWorld, workstation);
        if (villager == null) {
            player.sendOverlayMessage(Component.translatable("message.valet.no_valet_at_post"));
            return InteractionResult.SUCCESS;
        }

        ValetBehavior.recallToWorkstation(serverWorld, villager);
        ValetWorkGoal.requestRestart(villager);
        ValetDebug.record(villager, "post_recall home=" + ValetDebug.shortPos(workstation));
        player.sendOverlayMessage(Component.translatable("message.valet.post_recall"));
        return InteractionResult.SUCCESS;
    }

    private static Villager findValetForWorkstation(ServerLevel world, BlockPos workstation) {
        AABB box = new AABB(workstation).inflate(WORKSTATION_RECALL_RADIUS);
        Villager best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Villager villager : world.getEntitiesOfClass(Villager.class, box, candidate ->
                candidate.isAlive()
                        && !candidate.isRemoved()
                        && isValet(candidate))) {
            BlockPos home = ValetHome.get(world, villager);
            if (!workstation.equals(home) && !hasJobSite(villager, world, workstation)) {
                continue;
            }
            double distance = villager.distanceToSqr(workstation.getX() + 0.5D, workstation.getY() + 0.5D, workstation.getZ() + 0.5D);
            if (distance < bestDistance) {
                best = villager;
                bestDistance = distance;
            }
        }
        return best;
    }
}
