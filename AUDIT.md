# Audit exhaustif du projet Valet - 0.3.8

> Inventaire historique. Les fichiers du pupitre et de la carte de groupe listes ci-dessous ont ete retires en 0.3.9 au profit du menu `Carte > Groupes de valets`.

Date : 12 juillet 2026

## Perimetre et resultat

- Cible reelle du depot : Minecraft 26.2, Fabric Loader 0.19.3, Fabric API 0.154.2+26.2, Loom 1.17.14 et Java 25.
- Tous les fichiers source et suivis, les fichiers locaux non ignores, les assets, la configuration Gradle, la documentation et le depot Git ont ete relus.
- Inventaire final : 224 fichiers presents, dont 99 Java, 90 JSON, 1 metadata, 21 Markdown et 2 PNG; 26 ressources ont ete deplacees vers les chemins 26.2 et 8 fichiers orphelins ont ete supprimes.
- Compilation et build Gradle complets sans avertissement.
- Audit ressources : 91 JSON lus, 14 blocs, 15 items, 12 recettes, 14 loot tables, schemas 26.2, parite EN/FR et references de modeles valides.
- Jar final : `build/libs/valet-0.3.8.jar`, SHA-256 `B51AE5685B80EFCC8022D3141127D08D3BD99671DCB75BC146E5BD4E623071FB`.
- Installation locale : un seul `valet-*.jar`, hash identique au build; Fabric API local `0.154.2+26.2` verifie par son hash Maven.
- Bootstrap serveur : Minecraft, Fabric, les mixins et Valet 0.3.8 se chargent; le monde de test n'est pas lance car son EULA n'est pas acceptee.
- Depot Git : 31 commits, 13 tags, `main` aligne sur `origin/main` avant l'audit; `git fsck` ne signale aucune corruption.
- Publication preparee sous le tag `v0.3.8`, avec l'audit et la carte tactique regroupes dans la meme version.

## Corrections majeures

### Fixed

- Persistence NBT 26.2 restauree pour les valets; roles mis en cache sans charger les chunks.
- Mixins actifs remis en coherence avec les classes Minecraft actuelles.
- Payloads serveur bornes et lies au bon menu, joueur, valet, UUID et dimension.
- Rollback des ressources sur les echecs de craft, cuisine, ferme et construction.
- Slots, limites de piles et doubles coffres respectes par les transferts.
- NBT corrompu ou surdimensionne neutralise; chargements arbitraires de chunks bloques.
- Recettes, loot tables et tags migres vers les chemins et schemas JSON 26.2; donnees du blueprint preservees avec `copy_custom_data`.
- Double drop du coffre cuisinier et loot table manquante du coffre de fleches corriges.

### Optimized

- Scans et calculs par tick temporises, mis en cache ou dedupliques.
- Reservations expirees purgees; caches de glow et de rendu nettoyes.
- Scans de construction bornes et realises avec des positions mutables.
- Snapshots reseau adaptes au role et apercus blueprint compresses en grilles 16x16.

### Cleaned

- Sept classes Java mortes, une archive externe et les fichiers macOS orphelins retires.
- Huit entrees de traduction inutilisees, imports, champs, methodes et surcharges morts supprimes.
- Option `Nourrir` inactive retiree du protocole, du runtime, du NBT et de l'interface.
- Gradle et manifeste centralises, checksum wrapper ajoute et installation rendue multiplateforme.
- Licence MIT et regles d'edition et de normalisation ajoutees.

## Limites de verification

- Aucun framework de tests automatise n'existe dans le projet; Gradle confirme `NO-SOURCE` pour les tests.
- Le bootstrap serveur valide le chargement du mod, mais pas un scenario de jeu interactif complet; l'EULA de test n'a pas ete acceptee.
- Les repertoires generes et ignores (`.gradle/`, `build/`, `run/`) ont ete regeneres et inspectes, mais ne sont pas des sources a conserver.
- Les objets internes de `.git/` ont ete controles par `status`, `log`, `tag`, `remote`, `ls-remote` et `fsck`, plutot que modifies.

## Inventaire fichier par fichier

Legende : chaque ligne confirme la relecture; le statut indique l'action finale.

### Racine, build, documentation et historique

- `.editorconfig` - Ajoute - conventions d'edition
- `.gitattributes` - Ajoute - normalisation Git
- `.gitignore` - Relu / valide sans changement
- `AGENTS.md` - Relu / valide sans changement
- `AUDIT.md` - Mis a jour - rapport exhaustif 0.3.8
- `CHANGELOG.md` - Mis a jour
- `JAR_REGISTRY.md` - Mis a jour
- `LICENSE` - Ajoute - licence MIT
- `README.md` - Mis a jour
- `build.gradle` - Corrige / fiabilise
- `docs/crafts.md` - Mis a jour
- `docs/important-notes.md` - Mis a jour
- `docs/releases/v0.1.3.md` - Relu / valide sans changement
- `docs/releases/v0.2.0.md` - Relu / valide sans changement
- `docs/releases/v0.2.1.md` - Relu / valide sans changement
- `docs/releases/v0.3.0.md` - Relu / valide sans changement
- `docs/releases/v0.3.1.md` - Relu / valide sans changement
- `docs/releases/v0.3.2.md` - Relu / valide sans changement
- `docs/releases/v0.3.3.md` - Relu / valide sans changement
- `docs/releases/v0.3.4.md` - Relu / valide sans changement
- `docs/releases/v0.3.5.md` - Relu / valide sans changement
- `docs/releases/v0.3.6.md` - Relu / valide sans changement
- `docs/releases/v0.3.7.md` - Relu / valide sans changement
- `docs/releases/v0.3.8.md` - Ajoute - note locale 0.3.8
- `gradle.properties` - Corrige / fiabilise
- `gradle/wrapper/gradle-wrapper.jar` - Relu / valide sans changement
- `gradle/wrapper/gradle-wrapper.properties` - Corrige / fiabilise
- `gradlew` - Relu / valide sans changement
- `gradlew.bat` - Relu / valide sans changement
- `plan.md` - Ajoute - suivi local du projet
- `settings.gradle` - Relu / valide sans changement
- `task.md` - Ajoute - suivi local du projet
- `asset_source/seller_npc_1.2.zip` - Supprime - archive asset externe orpheline

### Code Java

- `src/main/java/com/wawane/valet/ValetConversations.java` - Relu / valide sans changement
- `src/main/java/com/wawane/valet/ValetDebug.java` - Relu / valide sans changement
- `src/main/java/com/wawane/valet/ValetHome.java` - Corrige / optimise / nettoye
- `src/main/java/com/wawane/valet/ValetMod.java` - Corrige / optimise / nettoye
- `src/main/java/com/wawane/valet/ValetNetworking.java` - Corrige / optimise / nettoye
- `src/main/java/com/wawane/valet/ValetRole.java` - Corrige / optimise / nettoye
- `src/main/java/com/wawane/valet/ai/ValetStateMachine.java` - Relu / valide sans changement
- `src/main/java/com/wawane/valet/ai/ValetWorkDriver.java` - Corrige / optimise / nettoye
- `src/main/java/com/wawane/valet/ai/ValetWorkGoal.java` - Corrige / optimise / nettoye
- `src/main/java/com/wawane/valet/ai/core/ValetBlockReservations.java` - Corrige / optimise / nettoye
- `src/main/java/com/wawane/valet/ai/core/ValetEntityReservations.java` - Corrige / optimise / nettoye
- `src/main/java/com/wawane/valet/ai/core/ValetOrderKey.java` - Corrige / optimise / nettoye
- `src/main/java/com/wawane/valet/ai/core/ValetWorkSettings.java` - Corrige / optimise / nettoye
- `src/main/java/com/wawane/valet/ai/inventory/ValetInventoryTransfer.java` - Corrige / optimise / nettoye
- `src/main/java/com/wawane/valet/ai/path/ValetPathPlanner.java` - Corrige / optimise / nettoye
- `src/main/java/com/wawane/valet/ai/tasks/BreedingRuntimeTask.java` - Corrige / optimise / nettoye
- `src/main/java/com/wawane/valet/ai/tasks/ConstructionRuntimeTask.java` - Corrige / optimise / nettoye
- `src/main/java/com/wawane/valet/ai/tasks/ConstructionTask.java` - Relu / valide sans changement
- `src/main/java/com/wawane/valet/ai/tasks/CookingRuntimeTask.java` - Corrige / optimise / nettoye
- `src/main/java/com/wawane/valet/ai/tasks/FarmingRuntimeTask.java` - Corrige / optimise / nettoye
- `src/main/java/com/wawane/valet/ai/tasks/LogisticsRuntimeTask.java` - Corrige / optimise / nettoye
- `src/main/java/com/wawane/valet/ai/tasks/MiningRuntimeTask.java` - Corrige / optimise / nettoye
- `src/main/java/com/wawane/valet/ai/tasks/MiningTask.java` - Corrige / optimise / nettoye
- `src/main/java/com/wawane/valet/ai/tasks/StewardRuntimeTask.java` - Corrige / optimise / nettoye
- `src/main/java/com/wawane/valet/ai/tasks/WoodcuttingTask.java` - Corrige / optimise / nettoye
- `src/main/java/com/wawane/valet/ai/tasks/combat/CombatRuntimeTask.java` - Corrige / optimise / nettoye
- `src/main/java/com/wawane/valet/ai/tasks/combat/ValetCombatTargeting.java` - Corrige / optimise / nettoye
- `src/main/java/com/wawane/valet/ai/tasks/crafting/CraftingRuntimeTask.java` - Corrige / optimise / nettoye
- `src/main/java/com/wawane/valet/breeding/AnimalBeaconBlock.java` - Relu / valide sans changement
- `src/main/java/com/wawane/valet/breeding/ValetAnimalArea.java` - Corrige / optimise / nettoye
- `src/main/java/com/wawane/valet/breeding/ValetAnimalMarkers.java` - Corrige / optimise / nettoye
- `src/main/java/com/wawane/valet/breeding/ValetAnimalStorage.java` - Corrige / optimise / nettoye
- `src/main/java/com/wawane/valet/breeding/ValetAnimalType.java` - Corrige / optimise / nettoye
- `src/main/java/com/wawane/valet/client/ConstructionBlueprintPlacementPreview.java` - Corrige / optimise / nettoye
- `src/main/java/com/wawane/valet/client/ValetClient.java` - Corrige / optimise / nettoye
- `src/main/java/com/wawane/valet/client/render/ValetConditionalVillagerRenderer.java` - Corrige / optimise / nettoye
- `src/main/java/com/wawane/valet/combat/InfiniteArrowChestBlock.java` - Corrige / optimise / nettoye
- `src/main/java/com/wawane/valet/construction/BlockStateCodec.java` - Relu / valide sans changement
- `src/main/java/com/wawane/valet/construction/ConstructionBeaconBlock.java` - Relu / valide sans changement
- `src/main/java/com/wawane/valet/construction/ConstructionBlueprintBlock.java` - Corrige / optimise / nettoye
- `src/main/java/com/wawane/valet/construction/ConstructionBlueprintBlockEntity.java` - Corrige / optimise / nettoye
- `src/main/java/com/wawane/valet/construction/ConstructionBlueprintItem.java` - Corrige / optimise / nettoye
- `src/main/java/com/wawane/valet/construction/ConstructionBlueprintNbt.java` - Relu / valide sans changement
- `src/main/java/com/wawane/valet/construction/ValetConstructionBlueprint.java` - Corrige / optimise / nettoye
- `src/main/java/com/wawane/valet/construction/ValetConstructionMarkers.java` - Corrige / optimise / nettoye
- `src/main/java/com/wawane/valet/construction/ValetConstructionStorage.java` - Corrige / optimise / nettoye
- `src/main/java/com/wawane/valet/construction/ValetConstructionSummary.java` - Corrige / optimise / nettoye
- `src/main/java/com/wawane/valet/cooking/CookChestBlock.java` - Corrige / optimise / nettoye
- `src/main/java/com/wawane/valet/cooking/CookChestBlockEntity.java` - Relu / valide sans changement
- `src/main/java/com/wawane/valet/farm/FarmBeaconBlock.java` - Relu / valide sans changement
- `src/main/java/com/wawane/valet/farm/ValetFarmArea.java` - Corrige / optimise / nettoye
- `src/main/java/com/wawane/valet/farm/ValetFarmMarkers.java` - Corrige / optimise / nettoye
- `src/main/java/com/wawane/valet/farm/ValetFarmStorage.java` - Corrige / optimise / nettoye
- `src/main/java/com/wawane/valet/group/ValetGroup.java` - Corrige / optimise / nettoye
- `src/main/java/com/wawane/valet/group/ValetGroupBindings.java` - Corrige / optimise / nettoye
- `src/main/java/com/wawane/valet/group/ValetGroupCardItem.java` - Corrige / optimise / nettoye
- `src/main/java/com/wawane/valet/group/ValetGroupCommand.java` - Corrige / optimise / nettoye
- `src/main/java/com/wawane/valet/group/ValetGroupInteractions.java` - Corrige / optimise / nettoye
- `src/main/java/com/wawane/valet/group/ValetGroupMode.java` - Corrige / optimise / nettoye
- `src/main/java/com/wawane/valet/group/ValetGroupRuntime.java` - Corrige / optimise / nettoye
- `src/main/java/com/wawane/valet/group/ValetGroupStorage.java` - Corrige / optimise / nettoye
- `src/main/java/com/wawane/valet/gui/ValetGroupScreen.java` - Corrige / optimise / nettoye
- `src/main/java/com/wawane/valet/gui/ValetGroupScreenHandler.java` - Corrige / optimise / nettoye
- `src/main/java/com/wawane/valet/gui/ValetOrdersScreen.java` - Corrige / optimise / nettoye
- `src/main/java/com/wawane/valet/gui/ValetOrdersScreenHandler.java` - Corrige / optimise / nettoye
- `src/main/java/com/wawane/valet/gui/ValetOrdersViewModel.java` - Corrige / optimise / nettoye
- `src/main/java/com/wawane/valet/mixin/AbstractArrowMixin.java` - Ajoute - remplacement du mixin projectile 26.2
- `src/main/java/com/wawane/valet/mixin/AbstractContainerScreenMixin.java` - Relu / valide sans changement
- `src/main/java/com/wawane/valet/mixin/VillagerEntityMixin.java` - Corrige / optimise / nettoye
- `src/main/java/com/wawane/valet/network/packets/ChooseCombatPerkPayload.java` - Relu / valide sans changement
- `src/main/java/com/wawane/valet/network/packets/ChoosePerkPayload.java` - Relu / valide sans changement
- `src/main/java/com/wawane/valet/network/packets/DeleteConstructionPayload.java` - Relu / valide sans changement
- `src/main/java/com/wawane/valet/network/packets/ManageGroupPayload.java` - Relu / valide sans changement
- `src/main/java/com/wawane/valet/network/packets/RenameValetPayload.java` - Relu / valide sans changement
- `src/main/java/com/wawane/valet/network/packets/SetBehaviorPayload.java` - Relu / valide sans changement
- `src/main/java/com/wawane/valet/network/packets/SetBreedingOrderPayload.java` - Corrige / optimise / nettoye
- `src/main/java/com/wawane/valet/network/packets/SetFarmOrderPayload.java` - Relu / valide sans changement
- `src/main/java/com/wawane/valet/network/packets/SetOrderPayload.java` - Relu / valide sans changement
- `src/main/java/com/wawane/valet/network/packets/SortContainerPayload.java` - Relu / valide sans changement
- `src/main/java/com/wawane/valet/network/packets/ValetGroupStatePayload.java` - Corrige / optimise / nettoye
- `src/main/java/com/wawane/valet/network/packets/ValetMagicCastPayload.java` - Relu / valide sans changement
- `src/main/java/com/wawane/valet/network/packets/ValetStatePayload.java` - Corrige / optimise / nettoye
- `src/main/java/com/wawane/valet/order/ValetCraftTarget.java` - Relu / valide sans changement
- `src/main/java/com/wawane/valet/order/ValetFarmCrop.java` - Corrige / optimise / nettoye
- `src/main/java/com/wawane/valet/order/ValetMineTarget.java` - Relu / valide sans changement
- `src/main/java/com/wawane/valet/order/ValetMiningScanner.java` - Corrige / optimise / nettoye
- `src/main/java/com/wawane/valet/order/ValetOrder.java` - Relu / valide sans changement
- `src/main/java/com/wawane/valet/order/ValetOrders.java` - Corrige / optimise / nettoye
- `src/main/java/com/wawane/valet/order/ValetWoodTarget.java` - Corrige / optimise / nettoye
- `src/main/java/com/wawane/valet/progress/ValetCombatPerk.java` - Relu / valide sans changement
- `src/main/java/com/wawane/valet/progress/ValetCombatProgress.java` - Corrige / optimise / nettoye
- `src/main/java/com/wawane/valet/progress/ValetCombatSkillTree.java` - Relu / valide sans changement
- `src/main/java/com/wawane/valet/progress/ValetPerk.java` - Relu / valide sans changement
- `src/main/java/com/wawane/valet/progress/ValetProgress.java` - Corrige / optimise / nettoye
- `src/main/java/com/wawane/valet/state/ValetBehavior.java` - Corrige / optimise / nettoye
- `src/main/java/com/wawane/valet/state/ValetData.java` - Corrige / optimise / nettoye
- `src/main/java/com/wawane/valet/ai/tasks/materials/MaterialAcquisitionRuntimeTask.java` - Supprime - classe morte ou mixin obsolete
- `src/main/java/com/wawane/valet/client/ConstructionBlueprintRenderer.java` - Supprime - classe morte ou mixin obsolete
- `src/main/java/com/wawane/valet/group/ValetGroupStationBlock.java` - Supprime - classe morte ou mixin obsolete
- `src/main/java/com/wawane/valet/mixin/PersistentProjectileEntityMixin.java` - Supprime - classe morte ou mixin obsolete
- `src/main/java/com/wawane/valet/mixin/VillagerHeldItemFeatureRendererMixin.java` - Supprime - classe morte ou mixin obsolete
- `src/main/java/com/wawane/valet/mixin/VillagerResemblingModelMixin.java` - Supprime - classe morte ou mixin obsolete
- `src/main/java/com/wawane/valet/mixin/ZombieVillagerEntityMixin.java` - Supprime - classe morte ou mixin obsolete

### Assets, data et configuration Fabric

- `src/main/resources/assets/valet/blockstates/animal_beacon.json` - Relu / valide sans changement
- `src/main/resources/assets/valet/blockstates/combat_workstation.json` - Relu / valide sans changement
- `src/main/resources/assets/valet/blockstates/construction_beacon.json` - Relu / valide sans changement
- `src/main/resources/assets/valet/blockstates/construction_blueprint.json` - Relu / valide sans changement
- `src/main/resources/assets/valet/blockstates/cook_chest.json` - Relu / valide sans changement
- `src/main/resources/assets/valet/blockstates/cook_workstation.json` - Relu / valide sans changement
- `src/main/resources/assets/valet/blockstates/farm_beacon.json` - Relu / valide sans changement
- `src/main/resources/assets/valet/blockstates/farmer_workstation.json` - Relu / valide sans changement
- `src/main/resources/assets/valet/blockstates/infinite_arrow_chest.json` - Relu / valide sans changement
- `src/main/resources/assets/valet/blockstates/magic_workstation.json` - Relu / valide sans changement
- `src/main/resources/assets/valet/blockstates/poste_eleveur.json` - Relu / valide sans changement
- `src/main/resources/assets/valet/blockstates/steward_workstation.json` - Relu / valide sans changement
- `src/main/resources/assets/valet/blockstates/valet_group_station.json` - Relu / valide sans changement
- `src/main/resources/assets/valet/blockstates/valet_workstation.json` - Relu / valide sans changement
- `src/main/resources/assets/valet/items/animal_beacon.json` - Relu / valide sans changement
- `src/main/resources/assets/valet/items/combat_workstation.json` - Relu / valide sans changement
- `src/main/resources/assets/valet/items/construction_beacon.json` - Relu / valide sans changement
- `src/main/resources/assets/valet/items/construction_blueprint.json` - Relu / valide sans changement
- `src/main/resources/assets/valet/items/cook_chest.json` - Relu / valide sans changement
- `src/main/resources/assets/valet/items/cook_workstation.json` - Relu / valide sans changement
- `src/main/resources/assets/valet/items/farm_beacon.json` - Relu / valide sans changement
- `src/main/resources/assets/valet/items/farmer_workstation.json` - Relu / valide sans changement
- `src/main/resources/assets/valet/items/infinite_arrow_chest.json` - Relu / valide sans changement
- `src/main/resources/assets/valet/items/magic_workstation.json` - Relu / valide sans changement
- `src/main/resources/assets/valet/items/poste_eleveur.json` - Relu / valide sans changement
- `src/main/resources/assets/valet/items/steward_workstation.json` - Relu / valide sans changement
- `src/main/resources/assets/valet/items/valet_group_card.json` - Relu / valide sans changement
- `src/main/resources/assets/valet/items/valet_group_station.json` - Relu / valide sans changement
- `src/main/resources/assets/valet/items/valet_workstation.json` - Relu / valide sans changement
- `src/main/resources/assets/valet/lang/en_us.json` - Corrige / nettoye / valide
- `src/main/resources/assets/valet/lang/fr_fr.json` - Corrige / nettoye / valide
- `src/main/resources/assets/valet/models/block/animal_beacon.json` - Relu / valide sans changement
- `src/main/resources/assets/valet/models/block/combat_workstation.json` - Relu / valide sans changement
- `src/main/resources/assets/valet/models/block/construction_beacon.json` - Relu / valide sans changement
- `src/main/resources/assets/valet/models/block/construction_blueprint.json` - Relu / valide sans changement
- `src/main/resources/assets/valet/models/block/cook_chest.json` - Relu / valide sans changement
- `src/main/resources/assets/valet/models/block/cook_workstation.json` - Relu / valide sans changement
- `src/main/resources/assets/valet/models/block/farm_beacon.json` - Relu / valide sans changement
- `src/main/resources/assets/valet/models/block/farmer_workstation.json` - Relu / valide sans changement
- `src/main/resources/assets/valet/models/block/infinite_arrow_chest.json` - Relu / valide sans changement
- `src/main/resources/assets/valet/models/block/magic_workstation.json` - Relu / valide sans changement
- `src/main/resources/assets/valet/models/block/poste_eleveur.json` - Relu / valide sans changement
- `src/main/resources/assets/valet/models/block/steward_workstation.json` - Relu / valide sans changement
- `src/main/resources/assets/valet/models/block/valet_group_station.json` - Relu / valide sans changement
- `src/main/resources/assets/valet/models/block/valet_workstation.json` - Relu / valide sans changement
- `src/main/resources/assets/valet/models/item/animal_beacon.json` - Relu / valide sans changement
- `src/main/resources/assets/valet/models/item/combat_workstation.json` - Relu / valide sans changement
- `src/main/resources/assets/valet/models/item/construction_beacon.json` - Relu / valide sans changement
- `src/main/resources/assets/valet/models/item/construction_blueprint.json` - Relu / valide sans changement
- `src/main/resources/assets/valet/models/item/cook_chest.json` - Relu / valide sans changement
- `src/main/resources/assets/valet/models/item/cook_workstation.json` - Relu / valide sans changement
- `src/main/resources/assets/valet/models/item/farm_beacon.json` - Relu / valide sans changement
- `src/main/resources/assets/valet/models/item/farmer_workstation.json` - Relu / valide sans changement
- `src/main/resources/assets/valet/models/item/infinite_arrow_chest.json` - Relu / valide sans changement
- `src/main/resources/assets/valet/models/item/magic_workstation.json` - Relu / valide sans changement
- `src/main/resources/assets/valet/models/item/poste_eleveur.json` - Relu / valide sans changement
- `src/main/resources/assets/valet/models/item/steward_workstation.json` - Relu / valide sans changement
- `src/main/resources/assets/valet/models/item/valet_group_card.json` - Relu / valide sans changement
- `src/main/resources/assets/valet/models/item/valet_group_station.json` - Relu / valide sans changement
- `src/main/resources/assets/valet/models/item/valet_workstation.json` - Relu / valide sans changement
- `src/main/resources/assets/valet/textures/entity/villager/profession/valet.png` - Relu / valide sans changement
- `src/main/resources/assets/valet/textures/entity/villager/profession/valet.png.mcmeta` - Relu / valide sans changement
- `src/main/resources/assets/valet/textures/entity/zombie_villager/profession/valet.png` - Relu / valide sans changement
- `src/main/resources/data/minecraft/tags/block/mineable/axe.json` - Deplace vers le chemin Minecraft 26.2 / valide
- `src/main/resources/data/minecraft/tags/point_of_interest_type/acquirable_job_site.json` - Relu / valide sans changement
- `src/main/resources/data/valet/loot_table/blocks/animal_beacon.json` - Deplace vers le chemin Minecraft 26.2 / valide
- `src/main/resources/data/valet/loot_table/blocks/combat_workstation.json` - Deplace vers le chemin Minecraft 26.2 / valide
- `src/main/resources/data/valet/loot_table/blocks/construction_beacon.json` - Deplace vers le chemin Minecraft 26.2 / valide
- `src/main/resources/data/valet/loot_table/blocks/construction_blueprint.json` - Deplace / corrige avec copy_custom_data / valide
- `src/main/resources/data/valet/loot_table/blocks/cook_chest.json` - Deplace vers le chemin Minecraft 26.2 / valide
- `src/main/resources/data/valet/loot_table/blocks/cook_workstation.json` - Deplace vers le chemin Minecraft 26.2 / valide
- `src/main/resources/data/valet/loot_table/blocks/farm_beacon.json` - Deplace vers le chemin Minecraft 26.2 / valide
- `src/main/resources/data/valet/loot_table/blocks/farmer_workstation.json` - Deplace vers le chemin Minecraft 26.2 / valide
- `src/main/resources/data/valet/loot_table/blocks/infinite_arrow_chest.json` - Ajoute - loot table manquante / valide
- `src/main/resources/data/valet/loot_table/blocks/magic_workstation.json` - Deplace vers le chemin Minecraft 26.2 / valide
- `src/main/resources/data/valet/loot_table/blocks/poste_eleveur.json` - Deplace vers le chemin Minecraft 26.2 / valide
- `src/main/resources/data/valet/loot_table/blocks/steward_workstation.json` - Deplace vers le chemin Minecraft 26.2 / valide
- `src/main/resources/data/valet/loot_table/blocks/valet_group_station.json` - Deplace vers le chemin Minecraft 26.2 / valide
- `src/main/resources/data/valet/loot_table/blocks/valet_workstation.json` - Deplace vers le chemin Minecraft 26.2 / valide
- `src/main/resources/data/valet/recipe/animal_beacon.json` - Deplace / migre au schema Minecraft 26.2 / valide
- `src/main/resources/data/valet/recipe/combat_workstation.json` - Deplace / migre au schema Minecraft 26.2 / valide
- `src/main/resources/data/valet/recipe/construction_beacon.json` - Deplace / migre au schema Minecraft 26.2 / valide
- `src/main/resources/data/valet/recipe/cook_chest.json` - Deplace / migre au schema Minecraft 26.2 / valide
- `src/main/resources/data/valet/recipe/cook_workstation.json` - Deplace / migre au schema Minecraft 26.2 / valide
- `src/main/resources/data/valet/recipe/farm_beacon.json` - Deplace / migre au schema Minecraft 26.2 / valide
- `src/main/resources/data/valet/recipe/farmer_workstation.json` - Deplace / migre au schema Minecraft 26.2 / valide
- `src/main/resources/data/valet/recipe/magic_workstation.json` - Deplace / migre au schema Minecraft 26.2 / valide
- `src/main/resources/data/valet/recipe/poste_eleveur.json` - Deplace / migre au schema Minecraft 26.2 / valide
- `src/main/resources/data/valet/recipe/steward_workstation.json` - Deplace / migre au schema Minecraft 26.2 / valide
- `src/main/resources/data/valet/recipe/valet_group_station.json` - Deplace / migre au schema Minecraft 26.2 / valide
- `src/main/resources/data/valet/recipe/valet_workstation.json` - Deplace / migre au schema Minecraft 26.2 / valide
- `src/main/resources/fabric.mod.json` - Corrige / nettoye / valide
- `src/main/resources/valet.mixins.json` - Corrige / nettoye / valide

### Anciennes ressources deplacees

- `src/main/resources/data/minecraft/tags/blocks/mineable/axe.json` - Deplace vers `src/main/resources/data/minecraft/tags/block/mineable/axe.json` - compatibilite Minecraft 26.2
- `src/main/resources/data/valet/loot_tables/blocks/animal_beacon.json` - Deplace vers `src/main/resources/data/valet/loot_table/blocks/animal_beacon.json` - compatibilite Minecraft 26.2
- `src/main/resources/data/valet/loot_tables/blocks/combat_workstation.json` - Deplace vers `src/main/resources/data/valet/loot_table/blocks/combat_workstation.json` - compatibilite Minecraft 26.2
- `src/main/resources/data/valet/loot_tables/blocks/construction_beacon.json` - Deplace vers `src/main/resources/data/valet/loot_table/blocks/construction_beacon.json` - compatibilite Minecraft 26.2
- `src/main/resources/data/valet/loot_tables/blocks/construction_blueprint.json` - Deplace vers `src/main/resources/data/valet/loot_table/blocks/construction_blueprint.json` - compatibilite Minecraft 26.2
- `src/main/resources/data/valet/loot_tables/blocks/cook_chest.json` - Deplace vers `src/main/resources/data/valet/loot_table/blocks/cook_chest.json` - compatibilite Minecraft 26.2
- `src/main/resources/data/valet/loot_tables/blocks/cook_workstation.json` - Deplace vers `src/main/resources/data/valet/loot_table/blocks/cook_workstation.json` - compatibilite Minecraft 26.2
- `src/main/resources/data/valet/loot_tables/blocks/farm_beacon.json` - Deplace vers `src/main/resources/data/valet/loot_table/blocks/farm_beacon.json` - compatibilite Minecraft 26.2
- `src/main/resources/data/valet/loot_tables/blocks/farmer_workstation.json` - Deplace vers `src/main/resources/data/valet/loot_table/blocks/farmer_workstation.json` - compatibilite Minecraft 26.2
- `src/main/resources/data/valet/loot_tables/blocks/magic_workstation.json` - Deplace vers `src/main/resources/data/valet/loot_table/blocks/magic_workstation.json` - compatibilite Minecraft 26.2
- `src/main/resources/data/valet/loot_tables/blocks/poste_eleveur.json` - Deplace vers `src/main/resources/data/valet/loot_table/blocks/poste_eleveur.json` - compatibilite Minecraft 26.2
- `src/main/resources/data/valet/loot_tables/blocks/steward_workstation.json` - Deplace vers `src/main/resources/data/valet/loot_table/blocks/steward_workstation.json` - compatibilite Minecraft 26.2
- `src/main/resources/data/valet/loot_tables/blocks/valet_group_station.json` - Deplace vers `src/main/resources/data/valet/loot_table/blocks/valet_group_station.json` - compatibilite Minecraft 26.2
- `src/main/resources/data/valet/loot_tables/blocks/valet_workstation.json` - Deplace vers `src/main/resources/data/valet/loot_table/blocks/valet_workstation.json` - compatibilite Minecraft 26.2
- `src/main/resources/data/valet/recipes/animal_beacon.json` - Deplace vers `src/main/resources/data/valet/recipe/animal_beacon.json` - compatibilite Minecraft 26.2
- `src/main/resources/data/valet/recipes/combat_workstation.json` - Deplace vers `src/main/resources/data/valet/recipe/combat_workstation.json` - compatibilite Minecraft 26.2
- `src/main/resources/data/valet/recipes/construction_beacon.json` - Deplace vers `src/main/resources/data/valet/recipe/construction_beacon.json` - compatibilite Minecraft 26.2
- `src/main/resources/data/valet/recipes/cook_chest.json` - Deplace vers `src/main/resources/data/valet/recipe/cook_chest.json` - compatibilite Minecraft 26.2
- `src/main/resources/data/valet/recipes/cook_workstation.json` - Deplace vers `src/main/resources/data/valet/recipe/cook_workstation.json` - compatibilite Minecraft 26.2
- `src/main/resources/data/valet/recipes/farm_beacon.json` - Deplace vers `src/main/resources/data/valet/recipe/farm_beacon.json` - compatibilite Minecraft 26.2
- `src/main/resources/data/valet/recipes/farmer_workstation.json` - Deplace vers `src/main/resources/data/valet/recipe/farmer_workstation.json` - compatibilite Minecraft 26.2
- `src/main/resources/data/valet/recipes/magic_workstation.json` - Deplace vers `src/main/resources/data/valet/recipe/magic_workstation.json` - compatibilite Minecraft 26.2
- `src/main/resources/data/valet/recipes/poste_eleveur.json` - Deplace vers `src/main/resources/data/valet/recipe/poste_eleveur.json` - compatibilite Minecraft 26.2
- `src/main/resources/data/valet/recipes/steward_workstation.json` - Deplace vers `src/main/resources/data/valet/recipe/steward_workstation.json` - compatibilite Minecraft 26.2
- `src/main/resources/data/valet/recipes/valet_group_station.json` - Deplace vers `src/main/resources/data/valet/recipe/valet_group_station.json` - compatibilite Minecraft 26.2
- `src/main/resources/data/valet/recipes/valet_workstation.json` - Deplace vers `src/main/resources/data/valet/recipe/valet_workstation.json` - compatibilite Minecraft 26.2
