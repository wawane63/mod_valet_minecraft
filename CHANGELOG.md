# Changelog

## 0.3.0 - Decoupage metiers

Bugs corriges / fonctionnalite :

- Separation des valets en trois postes : `Poste d'artisan`, `Poste de combattant`, `Poste de fermier`.
- L'artisan garde les ordres bois, minerais, construction et craft.
- Le combattant est verrouille sur la defense/combat et ne recoit plus d'ordres de recolte.
- Le fermier garde uniquement l'ordre `Recolter` et les options de champs.
- Ajout d'un arbre fermier dedie : vitesse, portee, replantation, labour, stockage, intendant.
- Extension des arbres combat epee/arc : portee, garde renforcee, tir lointain, volee.
- L'UI affiche seulement les pages, ordres et perks compatibles avec le poste actif.
- Les ordres incompatibles sont refuses cote serveur et nettoyes automatiquement si le poste change.
- Les bonus de perks sont limites au metier actif pour eviter les effets croises.
- Ajout des assets, recettes, loot tables et traductions des nouveaux postes.
- Correction : un valet conserve son poste stocke au lieu de suivre une memoire vanilla changee.
- Correction : un villageois oisif ne peut plus garder/prendre le `JOB_SITE` d'un poste Valet deja reserve.
- Correction : le scan d'attribution ne retraite plus le meme villageois plusieurs fois dans le meme tick.
- Correction : le glow pose par le mod est retire meme si Minecraft a deja retire le metier avant le nettoyage Valet.

Limitations :

- Verification visible en jeu encore a faire sur les trois postes avec plusieurs valets.

## 0.2.1 - Fermier local

Bugs corriges / fonctionnalite :

- Ajout de l'ordre `Recolter` pour ramasser les cultures mures proches.
- Ajout de la `Balise de ferme` verte : deux balises definissent une zone de travail selectionnable dans l'UI.
- Ajout du choix des cultures travaillees : ble, carottes, patates, betteraves, verrues du Nether.
- Ajout des options `Replanter` et `Passer la houe`.
- Le valet fermier tient une houe ; le valet sans travail tient un cookie ou une torche.
- Depot des recoltes via la logistique coffre existante.
- Correction : une zone de ferme ne garde plus que la couche haute des balises.
- Correction : le fermier ne creuse plus pour rejoindre une culture.
- Correction : le panneau fermier ne se superpose plus avec l'arbre de competences.
- Correction : la fine couche de neige ne bloque plus le trajet, la recolte, la replantation ou le passage de houe.
- Correction : le fermier peut marcher sur la terre labouree pour atteindre les cultures au centre du champ.
- Correction : un sol tente a la houe est ignore temporairement s'il refuse ou reperd son etat laboure.
- Correction : les cibles changees/interrompues sont liberees et replanifiees plus proprement avec plusieurs valets.

Limitations :

- Verification visible en jeu encore a faire sur champs multi-cultures.

## 0.2.0 - Portage Minecraft 26.2

Bugs corriges / portage :

- Build compatible Minecraft `26.2`, Fabric Loader `0.19.3`, Fabric API `0.153.0+26.2`, Java `25`.
- Migration Gradle/Fabric Loom, mappings Mojang, networking/menu Fabric 26.2 et NBT item components.
- Installation locale du jar et du profil launcher `Fabric 26.2 - Valet`.
- Restauration de l'UI complete Valet : pages general/epee/arc/inventaire, XP et perks.
- Ajout d'un apercu d'item pour les crafts dans l'UI.
- Correction des textes invisibles dans l'UI 26.2.
- Correction des icones violet/noir des blocs/items via les definitions `assets/valet/items`.
- Ajout des traductions `item.valet.*` pour les blocs/items du mod.
- Filtrage des ressources mine/bois a `0` dans les cibles disponibles.
- La touche `E` ne ferme plus l'UI quand le champ de nom du valet est actif.
- Correction de la boucle profession `none` apres retour/minage : reservation du job site.
- Coupe d'arbre complete en 3 secondes, avec detection diagonale des logs et limite relevee.
- Correction des blocages entre valets : minerais/bois et blocs de passage sont reserves pendant l'action, les autres valets replanifient.
- Correction du retour apres tache : depot/retour coffre ne sont plus interrompus par un ordre de minage encore actif.
- Correction de la perte de job : suppression du metier, de l'ordre actif et du glow quand le poste n'est plus valide.
- Correction du poste valet : un seul valet par poste, comme un poste metier vanilla, sans alternance entre villageois oisifs.
- Le glow n'est plus retire a la fermeture de l'UI, seulement a la perte du metier.
- Suppression du glow client local de l'ecran d'ordres pour que la perte du metier eteigne bien le glow.
- Coupe d'arbre elargie aux logs connectes du meme arbre pour eviter les restes en hauteur.

Limitations :

- Mixins et previews construction neutralises temporairement.
- Rendu preview construction monde encore a verifier en jeu.

## 0.1.3 - Better UI + correctifs craft

Bugs corriges :

- Craft de pioche en pierre stabilise : plus de dependance implicite aux outils en fer.
- Blocage apres minage de cobble corrige par meilleure transition vers le retour au poste.
- Freeze serveur corrige en bornant la recherche de ressources craft a 32 candidats proches.
- Debug actif par defaut et limite contre le spam de logs.
- Recuperation verticale du poste et budgets de pathfinding corriges.

Modifications structurelles :

- Fenetre de competences agrandie et arbre repositionne avec depart en bas.
- Reorganisation des perks ressources, epee et arc.
- Ajout du bouton d'inventaire valet.
- Ajout d'une logique de pathing craft/home capable de degager des blocs naturels.

## 0.1.2 - Craft

Bugs corriges :

- Relance du meme ordre craft rendue fiable.
- Detection des besoins de craft : cobblestone, bois, planches et sticks.
- Restock depuis coffres proches pour les materiaux de craft.
- Gestion des cas inventaire plein pendant craft/depot.

Modifications structurelles :

- Ajout de `CraftingRuntimeTask`.
- Ajout de `ValetCraftTarget` et de l'ordre `CRAFT`.
- Extension de `ValetStateMachine` avec `CRAFTING` et `PathPurpose.CRAFT`.
- Ajout des payloads et de l'UI pour choisir un craft.

## 0.1.1 - Combat

Bugs corriges :

- Animation d'attaque visible via paquet vanilla `EntityAnimationS2CPacket.SWING_MAIN_HAND`.
- Rendu zombie-valet corrige pour eviter l'apparence rose/violette.
- Synchronisation UI/progression combat corrigee apres choix de perk.

Modifications structurelles :

- Ajout de `CombatRuntimeTask`.
- Ajout des perks et de la progression combat.
- Ajout du coffre a fleches infinies.
- Ajout des mixins de projectile/rendu necessaires au combat.

## 0.1.0 - Recolte

Bugs corriges :

- Activation des ordres valet corrigee.
- Redemarrage du goal a chaque changement d'ordre.
- Navigation/path traversal de base retablie.

Modifications structurelles :

- Ajout du metier `Valet`.
- Ajout des ordres initiaux recolte/minage/bois.
- Ajout du depot vers coffre/baril.
- Mise en place du goal principal `ValetWorkGoal`.

## Journal technique detaille historique

- `ValetOrdersScreenHandler.java`, `ValetOrdersScreen.java`, `ValetNetworking.java`: envoie les blueprints complets a l'UI, separe selection/preview de l'action `Plan`, et empeche la suppression de donner un blueprint; risque residuel faible, l'ouverture d'UI transporte plus de NBT pour les constructions.
- `ConstructionBlueprintBlock.java`, `ConstructionBlueprintBlockEntity.java`, `ValetNetworking.java`: stocke l'UUID du valet dans le blueprint donne, reactive l'ordre construction a la pose, resynchronise le NBT du bloc pose, et retire les blueprints d'inventaire quand la construction est supprimee; corrige le chantier qui ne se lance pas apres pose.
- `ConstructionBlueprintPlacementPreview.java`, `ValetClient.java`, `ValetOrdersScreen.java`: ajoute une preview hologramme avant pose du blueprint tenu et une mini-preview top-down dans l'UI valet; risque residuel faible, rendu limite a 6000 blocs.
- `ConstructionBlueprintBlockEntity.java`, `ConstructionRuntimeTask.java`: rend la pose de blueprint tolerante aux anciens stacks qui n'ont que le NBT `Blueprint` et fait matcher le chantier sur l'id imbrique; corrige le valet qui ne demarrait pas apres pose du blueprint.
- `ValetNetworking.java`, `ValetOrdersScreen.java`, `DeleteConstructionPayload.java`, lang: expose la suppression des constructions depuis l'UI valet et efface le blueprint persistant cote serveur; risque residuel faible, l'UI retire localement l'entree avant confirmation serveur.
- `src/main/resources/data/valet/loot_tables/blocks/*`, `ConstructionBlueprintBlock.java`: ajoute les loot tables des blocs custom et preserve le NBT des blueprints via loot table; risque residuel faible, depend de `copy_nbt` vanilla.
- `ValetNetworking.java`, `ValetOrdersScreen.java`: supprime le flag glowing serveur permanent et remet le highlight client a false a la fermeture; risque residuel faible, l'effet Glowing serveur reste temporaire pendant 30 minutes.
- `VillagerEntityMixin.java`, `ValetMod.java`, `ValetHome.java`, `ValetOrders.java`, `ValetProgress.java`, `ValetConversations.java`: limite la lecture/ecriture NBT Valet aux valets ou donnees deja presentes et purge les maps UUID a l'unload/world stop; risque residuel moyen, purge unload depend du timing de sauvegarde entite Fabric/vanilla.
- `ValetConstructionBlueprint.java`, `BlockStateCodec.java`: remplace la serialization raw ID des BlockState par NBT stable `NbtHelper` avec `DataVersion` blueprint et migration legacy `State`; risque residuel faible, les anciens blueprints sont convertis au prochain write.
- `ValetOrdersScreen.java`: ajoute scroll interne et barre de defilement pour la liste d'ordres afin d'eviter l'overflow visuel; risque residuel faible, navigation clavier non ajoutee.
- `ValetNetworking.java`, `ValetClient.java`, `ValetOrdersScreen.java`: ajoute un refresh S2C `valet_state` apres ordre/perk/rename et applique l'etat serveur dans l'ecran; risque residuel faible, l'UI reste brievement optimiste jusqu'au retour serveur.
- `ValetMod.java`: remplace la comparaison `RegistryKey` par `.equals` dans la detection de workstation deja reclamee; risque residuel nul attendu.
- `ValetNetworking.java`, `ValetOrdersScreenHandler.java`: ajoute UUID/dimension au payload d'ouverture et ferme les conversations par UUID meme si l'entite est unload; risque residuel faible, la dimension est stockee pour tracabilite mais l'API conversation reste indexee UUID.
- `ValetWorkGoal.java`: retire `Block.FORCE_STATE` de la pose de blocs de construction et utilise `Block.NOTIFY_ALL`; risque residuel moyen, certains blocs complexes peuvent reagir aux updates voisins differemment qu'avant.
- `ValetOrder.java`, `en_us.json`, `fr_fr.json`: supprime les ordres morts `FARM` et `DEPOSIT` faute de comportement implemente; risque residuel faible, d'anciens NBT avec ces noms retomberont sur `NONE`.
- `ValetOrders.java`: nettoie les cibles mine/bois/construction incompatibles pour tous les changements d'ordre; risque residuel nul attendu.
- `en_us.json`, `fr_fr.json`: supprime les cles mortes `screen.valet.perk_available` et `message.valet.no_ore`; risque residuel nul attendu.
- `ValetNetworking.java`: deduplique les scans minerais/bois et la lecture des constructions pendant l'ouverture GUI; risque residuel nul attendu.
- `ValetConstructionStorage.java`, `ValetConstructionMarkers.java`, lang: ajoute limite de 64 blueprints et APIs publiques remove/rename; risque residuel moyen, suppression/renommage ne sont pas encore exposes dans une UI.
- `ValetMod.java`, `ValetConstructionMarkers.java`: purge le marqueur de premiere balise construction a la deconnexion joueur; risque residuel nul attendu.
- `ValetHome.java`: persiste la dimension du home avec migration des anciennes donnees vers la dimension courante; risque residuel faible pour des anciens NBT charges dans une autre dimension que celle d'origine.
- `ValetWorkGoal.java`: utilise l'inventaire combine vanilla des double chests pour depot, recherche et consommation de materiaux; risque residuel faible, les deux moities du coffre peuvent encore etre scannees mais sans duplication d'items.
- `ValetWorkGoal.java`: corrige le depot a distance en deposant seulement dans le coffre atteint puis en repartant chercher un autre coffre si l'inventaire reste plein; changement visible, le depot multi-coffres peut prendre plus de trajets.
- `ValetWorkGoal.java`: evalue le deplacement case par case via `refreshPositionAndAngles` et le garde comme limitation connue faute de regression visible confirmee; risque residuel moyen, portes/collisions restent a surveiller en jeu.
- `ValetConstructionMarkers.java`: la copie de structure ignore maintenant les layers vides intermediaires au lieu de s'arreter au premier vide; risque residuel faible, les volumes tres hauts restent limites par `MAX_HEIGHT` et `MAX_VOLUME`.
- `ConstructionBlueprintBlock.java`, `construction_blueprint.json`: confirme le cas blueprint vide via loot table, sans drop manuel conditionne par `constructionId`; risque residuel faible.
- `ValetOrder.java`, `ValetOrders.java`, `ValetProgress.java`, `ValetConstructionBlueprint.java`: ajoute `DATA_VERSION` aux donnees persistantes Valet/progression/blueprints et remplace l'ordre NBT par une cle stable avec migration des anciens noms enum; risque residuel faible.
- `ValetStateMachine.java`, `ValetWorkGoal.java`: extrait les enums d'etat/path purpose et transitions simples de depart/interruption sans changement comportemental voulu; risque residuel faible.
- `ai/path/ValetPathPlanner.java`, `ValetWorkGoal.java`: extrait l'A* pur, les bounds de recherche et la reconstruction de chemin; risque residuel faible, les regles de passability restent dans `ValetWorkGoal`.
- `ai/tasks/MiningTask.java`, `WoodcuttingTask.java`, `ConstructionTask.java`, `ValetWorkGoal.java`: extrait les helpers purs de cluster ressources/bois et de calcul blueprint; risque residuel faible, les ticks metier restent dans `ValetWorkGoal`.
- `ai/inventory/ValetInventoryTransfer.java`, `ValetWorkGoal.java`: extrait depot, insertion, simulation de place, prise d'item et double chests; risque residuel faible.
- `network/packets/*`, `ValetNetworking.java`, `ValetClient.java`, `ValetOrdersScreen.java`: extrait les payloads reseau client/serveur sans changer les IDs ni les handlers; risque residuel faible.
- `gui/ValetOrdersViewModel.java`, `ValetOrdersScreen.java`: ajoute un snapshot immuable des donnees d'ouverture GUI pour isoler l'ecran des tableaux du handler; risque residuel faible.
- `state/ValetData.java`, `VillagerEntityMixin.java`, `ValetMod.java`: centralise les operations lifecycle villager Valet sans fusionner les formats NBT existants; risque residuel faible.
- `ai/tasks/MiningRuntimeTask.java`, `ValetWorkGoal.java`: extrait l'execution runtime mine/bois (cible, filon, minage, collecte) dans une task dediee; risque residuel moyen tant que les tests en jeu mine/bois ne sont pas faits.
- `ai/tasks/ConstructionRuntimeTask.java`, `ValetWorkGoal.java`: extrait l'execution runtime construction (selection site, materiaux, pose, reports) dans une task dediee; risque residuel moyen tant que les tests en jeu construction ne sont pas faits.
- `ValetWorkGoal.java`: remplace le deplacement par teleport `refreshPositionAndAngles` par navigation vanilla step-by-step avec timeout; risque residuel moyen, a verifier en jeu dans tunnels/escaliers.
- `ai/tasks/LogisticsRuntimeTask.java`, `ValetWorkGoal.java`: extrait retour coffre, depot, retour poste et idle dans une task dediee; risque residuel moyen tant que les tests en jeu depot/retour poste ne sont pas faits.
