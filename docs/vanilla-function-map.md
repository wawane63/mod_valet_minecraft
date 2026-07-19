# Cartographie Vanilla / Valet - Minecraft 26.2

Date : 15 juillet 2026

## Mise en oeuvre 0.4.4

- Le Brain du villageois est l'unique ordonnanceur : `CORE`, `WORK`, `REST` et `IDLE`; aucun `Goal` custom ni driver de tick parallele ne pilote encore le mouvement.
- Tous les trajets autonomes utilisent `WALK_TARGET`, `MoveToTargetSink` et `PathNavigation`; l'A* Valet et l'excavation de groupe sont supprimes.
- Les trajets longue distance restent decoupes par Valet, mais chaque troncon praticable est execute et valide par la navigation vanilla.
- Les missions de groupe peuvent traverser l'eau de surface avec la navigation et les controles de nage vanilla, sans aucune excavation de mouvement.
- `ValetSafeNavigation` ajoute l'enveloppe deterministe : support solide, volume libre, denivele humain, refus des fluides et blocs dangereux, progression et timeout.
- Les trajets de ferme utilisent maintenant le meme flux que `HarvestFarmland` : `WALK_TARGET`, `MoveToTargetSink`, memoire `PATH` et `InteractWithDoor` pour un mouvement continu.
- Un controle Gradle fait echouer `check` si l'ancien driver, l'A* custom, un `NodeEvaluator`, `teleportTo` ou du bris de bloc reapparait dans une couche de deplacement.

## Inventaire runtime du deplacement

- `GoalSelector` : aucun `Goal` Valet enregistre; `ValetWorkGoal` est un moteur metier appele uniquement par `WorkBehavior` dans le Brain.
- `CORE` : `Swim`, `InteractWithDoor`, `WakeUp`, `ValetBoundedMoveToTargetSink`, `LookAtTargetSink`, `ActivityController`.
- `WORK` : `WorkBehavior`, qui choisit la cible metier et pose une `WALK_TARGET` bornee.
- `REST` : `SetWalkTargetFromBlockMemory(HOME)` puis `SleepInBed`.
- `IDLE` : promenade locale custom, mais destination et trajet executes par le Brain vanilla borne.
- Navigation physique : `GroundPathNavigation` / `PathNavigation` vanilla du `Villager`; aucun `NodeEvaluator` custom, aucun `canDig`, aucun `canBreakDoors` Valet.
- Portes : `InteractWithDoor`; portillons : ouverture bornee seulement si le chemin recalcule passe par le portillon.
- Groupes : `PathNavigation` cadencee, tickets temporaires pour `MOVE_TO` et `RECALL`, nage via `MoveControl` / `JumpControl`.

## Regle de decision

- `VANILLA` : Minecraft fournit le meme comportement; Valet doit le reutiliser.
- `HYBRIDE` : Minecraft fournit la mecanique de base, mais l'ordre ou la regle Valet reste custom.
- `CUSTOM` : aucun comportement vanilla equivalent ne couvre le besoin du mod.
- Cette cartographie porte sur les fonctions gameplay/runtime. Les getters, codecs, DTO reseau et widgets elementaires ne sont pas listes individuellement.

## Conclusion

- Le principal ecart etait la concurrence entre Brain vanilla, driver Valet et A* bloc par bloc; il est supprime en 0.4.4.
- Les missions de groupe utilisent `PathNavigation` sur terrain praticable; sans chemin de surface elles attendent et replanifient, sans tunnelage.
- L'intendant double une grande partie du comportement vanilla du golem de cuivre 26.2 (`TransportItemsBetweenContainers`). Les filtres Valet restent custom.
- Le craft et la cuisine codent des recettes en Java alors que `RecipeManager` est la source vanilla.
- La ferme garde les zones et options Valet, mais reprend desormais le mouvement continu de `HarvestFarmland`; les actions de recolte et de pose restent hybrides.

## Priorite 0 - remplacement direct

| Fonction Valet | Etat actuel | Vanilla 26.2 | Decision |
| --- | --- | --- | --- |
| Deplacement local vers minerai, culture, animal, coffre, chantier, craft, cuisine et intendant | `WALK_TARGET`, `ValetBoundedMoveToTargetSink` et chemin vanilla complet | `PathNavigation`, `MoveToTargetSink`, memoires `WALK_TARGET` / `PATH` | `VANILLA BORNE`, termine en 0.4.4; Valet choisit seulement la cible legale. |
| Ouverture et fermeture des portes | `InteractWithDoor`; portes metalliques fermees refusees | `InteractWithDoor` gere le chemin, l'ouverture, la fermeture et les autres mobs dans le passage | `VANILLA`, termine pour les portes normales. |
| Fuite locale et sortie de fluide sans excavation | Cible Valet bornee, chemin/navigation vanilla, aucune teleportation | Navigation, comportements de panique du cerveau villageois et pathfinding eau/terre | `HYBRIDE` : priorite Valet, mouvement vanilla et territoire strict. |
| Ramassage d'items au sol | Scan, trajet puis insertion/discard directs | `GoToWantedItem`, `WANTED_ITEM` et ramassage natif du villageois | `VANILLA` : reutiliser le trajet et le ramassage vanilla; conserver seulement le filtre d'items et la zone Valet. |

## Priorite 1 - bases vanilla a reutiliser

| Fonction Valet | Etat actuel | Vanilla 26.2 | Decision |
| --- | --- | --- | --- |
| Intendant : chercher deux conteneurs et transporter une pile | Machine d'etat et recherche custom dans `StewardRuntimeTask` | `TransportItemsBetweenContainers`, utilise par `CopperGolemAi` | `HYBRIDE` : reprendre le cycle vanilla de recherche, chemin, positions inaccessibles, file d'attente et interaction. Conserver les 9 slots de filtre et les priorites Valet. |
| Insertion et transfert de piles | Plusieurs implementations de fusion/simulation dans `ValetInventoryTransfer`, `StewardRuntimeTask`, cuisine et craft | `HopperBlockEntity.addItem`, regles `Container` / `WorldlyContainer` | `VANILLA` pour les transferts standards; adaptateur custom uniquement pour limiter les slots Valet et proteger les filtres. |
| Resolution des coffres doubles | `ChestBlock.getContainer` et position canonique | API vanilla des coffres | Deja `VANILLA`; conserver. |
| Recettes de craft | Planches, batons et pioche en pierre codes manuellement | `RecipeManager` et recettes datapack | `VANILLA` : la recette chargee doit determiner ingredients, resultat et composants. L'ordre de craft reste Valet. |
| Recettes de cuisine | Liste Java fixe pain/viandes/poissons/pomme de terre | `RecipeManager`, recettes crafting et smelting | `VANILLA` : interroger les recettes chargees. Le poste, le coffre dedie et le rythme restent Valet. |
| Pose des blocs de construction | `world.setBlock` avec etat final du blueprint | Logique de placement `BlockItem`, survie, mises a jour de voisins et callbacks de placement | `HYBRIDE` : conserver la transformation du blueprint, mais appliquer autant que possible le pipeline vanilla de placement. Cas portes/lits/blocs a entite a tester separement. |
| Pose de torches et plantations | Etat de bloc pose directement | Interaction/placement des items vanilla | `VANILLA` lorsque l'action est identique a l'utilisation de l'item; garder les choix de cible Valet. |

## Metiers et actions

| Fonction | Base vanilla deja utilisee | Ce qui doit changer ou rester |
| --- | --- | --- |
| Minage et bucheron | `Block.getDrops`, outil passe au loot, `destroyBlock`, tags de blocs | `CUSTOM` uniquement pour casser la ressource explicitement ciblee; aucun bloc n'est casse pour ouvrir un trajet. |
| Ferme : recolte et replantation | `CropBlock`, loot vanilla, etats de cultures | `HYBRIDE` avec `HarvestFarmland` comme reference exacte pour ble/carottes/pommes de terre/betteraves. Zones, neige, labour, Nether Wart et options restent Valet. |
| Ferme : labour | Son et bloc vanilla, mais mutation directe | Reprendre l'action de houe vanilla quand elle produit exactement le meme resultat. Choix de parcelle et ordre restent `CUSTOM`. |
| Elevage | `Animal.setInLove` puis `BreedGoal` animal vanilla | `HYBRIDE` : Valet choisit un adulte pret, rejoint un stand adjacent, le nourrit et laisse vanilla former le couple; enclos, stock, plafond et reservations restent Valet. |
| Tonte | Appel direct a `Sheep.shear` | Deja presque `VANILLA`; cibler l'interface vanilla `Shearable` plutot que seulement `Sheep` si le gameplay l'autorise. |
| Traite | Creation manuelle du seau de lait et son vanilla | Aucun comportement IA vanilla generique identique; `CUSTOM`, avec les memes validations que l'interaction vanilla de la vache. |
| Ramassage d'oeufs | Insertion/discard directs | Utiliser le ramassage vanilla d'item; le filtre `EGG` et l'enclos restent Valet. |
| Abattage du surplus | Degats vanilla, selection custom | `HYBRIDE` : selection et seuils Valet; attaque et navigation vanilla. |
| Combat melee | Cible, cooldown et degats geres par Valet; poursuite via `PathNavigation` | Poursuite deja `VANILLA`. Reprendre l'attaque `Mob.doHurtTarget` / comportement melee lorsque compatible; conserver allies, perks et priorites. |
| Combat a l'arc | Entite `Arrow`, trajectoire et navigation vanilla | `RangedBowAttackGoal` exige un type de mob incompatible avec `Villager`; conserver le controle `CUSTOM` et les projectiles vanilla. |
| Magie | `EvokerFangs`, `Snowball`, effets et degats vanilla | Sorts, perks, soutien et ciblage sont `CUSTOM`; les primitives vanilla sont deja bien reutilisees. |
| Construction | Etats, rotations, drops et conteneurs vanilla | Blueprint, miroir, materiaux, ordre et apercu sont `CUSTOM`; seul le pipeline de pose doit etre rapproche du vanilla. |
| Logistique de retour/depot | Coffres vanilla, transfert custom | Navigation et insertion vanilla; decision de retour et exclusion des fleches restent Valet. |

## Groupes et navigation longue distance

| Fonction | Etat | Decision |
| --- | --- | --- |
| Suivre joueur, cible ou meneur | `PathNavigation.moveTo` avec destination et cadence Valet | `HYBRIDE` correct : mouvement vanilla, choix du membre/meneur et cohesion custom. |
| Aller a un repere lointain | Troncons locaux, tickets de chunks et contournements Valet; navigation vanilla sur chaque troncon praticable | `HYBRIDE` necessaire. Aucun comportement villageois vanilla ne garantit une mission de 1000 blocs. |
| Echec de mission sans chemin | Attente et replanification de surface avec backoff | Aucun villageois vanilla ne cree un tunnel pour atteindre une destination | `HYBRIDE` borne; excavation de mouvement supprimee. |
| Defense de groupe | Selection d'ennemi et interruption de mission custom | `HYBRIDE` : mouvement/attaque vanilla, regles de groupe Valet. |
| Accostage | Recherche de berge custom et navigation vanilla, saut force si necessaire | `HYBRIDE` justifie; supprimer uniquement les corrections de position non necessaires. |
| Tickets de chunks | API de ticket Minecraft avec politique Valet bornee | `CUSTOM` necessaire pour la mission longue distance. |

## Identite, cerveau et donnees

| Fonction | Etat | Decision |
| --- | --- | --- |
| Identite Valet et role choisi dans l'UI | Donnees persistantes independantes de la profession | `CUSTOM` necessaire; une profession vanilla ne reproduit pas cette regle. |
| Poste de travail | `JOB_SITE` exact, ticket possede marque et zone liee | `HYBRIDE` : POI vanilla avec assignation Valet locale et explicite. |
| Lit | `HOME` assigne par le joueur, ticket exact et trajet local valide | `HYBRIDE` : sommeil vanilla, aucune recherche globale `AcquirePoi(HOME)`. |
| Orchestration des metiers | `WorkBehavior` dans l'activite Brain `WORK`; aucun driver externe | `HYBRIDE` : Brain vanilla ordonnance, machine metier custom choisit les actions. |
| Nettoyage des memoires de mouvement | A l'interruption seulement, avant fuite/combat/groupe | Necessaire pour qu'une ancienne `WALK_TARGET` ne concurrence pas la nouvelle commande. |
| Reservations blocs/entites | Maps temporaires Valet | `CUSTOM` necessaire pour eviter deux ouvriers sur la meme cible; aucun equivalent vanilla generique. |
| NBT, migration, UUID, progression et ordres | Donnees Valet via les API de sauvegarde Minecraft | `CUSTOM` necessaire; API vanilla deja utilisee. |

## Fonctions sans equivalent vanilla identique

- Carte tactique, groupes, repere et UI de gestion.
- Blueprints, rotation, miroir, apercu et calcul des materiaux.
- Zones de ferme et d'enclos, filtres de l'intendant et coffres dedies.
- Progression, perks, arbre de combat et magie.
- Maire conditionnel, quetes et livraisons.
- Tri manuel depuis l'interface d'un conteneur.
- Protection des allies contre les projectiles.
- Conversations, renommage, affichage conditionnel et payloads du mod.

## Ordre de correction recommande

1. Termine en 0.4.4 : Brain unique, territoire, POI locaux, pathfinding vanilla et suppression de toute excavation de mouvement.
2. Termine pour l'eleveur : stand adjacent, coffre atteint physiquement, nourrissage individuel et accouplement animal vanilla.
3. Restant : rapprocher le ramassage d'items et l'intendant de leurs comportements vanilla dedies.
4. Restant : remplacer les recettes Java par `RecipeManager`.
5. Restant : aligner combat melee et pose de blocs sur leurs actions vanilla, un metier a la fois avec test en jeu et lecture de `latest.log`.

## References vanilla inspectees dans le jar local 26.2

- `Villager`, `VillagerGoalPackages`, `MoveToTargetSink`, `InteractWithDoor`.
- `HarvestFarmland`, `GoToWantedItem`, `VillagerMakeLove`, `WorkAtPoi`.
- `TransportItemsBetweenContainers`, `CopperGolemAi`, `HopperBlockEntity`.
- `MeleeAttack`, `RangedBowAttackGoal`, `OpenDoorGoal`, `FollowMobGoal`.
- `RecipeManager`, `BlockItem`, `Shearable`.
