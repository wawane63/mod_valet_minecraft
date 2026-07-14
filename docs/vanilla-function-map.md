# Cartographie Vanilla / Valet - Minecraft 26.2

Date : 14 juillet 2026

## Mise en oeuvre 0.4.2

- La priorite 0 sur le mouvement est appliquee : trajets metier, sortie d'eau et excavation de groupe utilisent maintenant `PathNavigation` sans `teleportTo`.
- Les trajets longue distance restent decoupes par Valet, mais chaque troncon praticable est execute et valide par la navigation vanilla.
- Les missions de groupe peuvent traverser l'eau de surface avec le pathfinding et le comportement de nage vanilla, sans rendre les trajets metier ni l'excavation amphibies.
- `ValetSafeNavigation` ajoute l'enveloppe deterministe : support solide, volume libre, denivele humain, refus des fluides et blocs dangereux, progression et timeout.
- Les trajets de ferme utilisent maintenant le meme flux que `HarvestFarmland` : `WALK_TARGET`, `MoveToTargetSink`, memoire `PATH` et `InteractWithDoor` pour un mouvement continu.
- Un controle Gradle fait echouer `check` si `.teleportTo(` reapparait dans le runtime Java.
- Les colonnes `Etat actuel` ci-dessous conservent l'instantane initial qui a motive la correction; cette section indique le nouvel etat.

## Regle de decision

- `VANILLA` : Minecraft fournit le meme comportement; Valet doit le reutiliser.
- `HYBRIDE` : Minecraft fournit la mecanique de base, mais l'ordre ou la regle Valet reste custom.
- `CUSTOM` : aucun comportement vanilla equivalent ne couvre le besoin du mod.
- Cette cartographie porte sur les fonctions gameplay/runtime. Les getters, codecs, DTO reseau et widgets elementaires ne sont pas listes individuellement.
- Aucun changement Java ou ressource n'a ete realise pendant cette cartographie.

## Conclusion

- Le principal ecart identifie etait le deplacement metier local applique avec `teleportTo`; il est corrige en 0.4.2 avec la navigation vanilla.
- Les missions de groupe utilisent deja `PathNavigation` sur terrain praticable; leur planification longue distance et leur tunnelage restent legitimement custom.
- L'intendant double une grande partie du comportement vanilla du golem de cuivre 26.2 (`TransportItemsBetweenContainers`). Les filtres Valet restent custom.
- Le craft et la cuisine codent des recettes en Java alors que `RecipeManager` est la source vanilla.
- La ferme garde les zones et options Valet, mais reprend desormais le mouvement continu de `HarvestFarmland`; les actions de recolte et de pose restent hybrides.

## Priorite 0 - remplacement direct

| Fonction Valet | Etat actuel | Vanilla 26.2 | Decision |
| --- | --- | --- | --- |
| Deplacement local vers minerai, culture, animal, coffre, chantier, craft, cuisine et intendant | A* Valet, puis `teleportTo` bloc par bloc dans `ValetWorkGoal.moveToPathStep` | `PathNavigation`, `MoveToTargetSink`, memoires `WALK_TARGET` / `PATH` | `VANILLA` : utiliser la navigation vanilla sur tout trajet praticable. Garder le planificateur Valet seulement pour decider ou creuser ou preparer un passage. |
| Ouverture et fermeture des portes | Modification directe de `DoorBlock.OPEN` et suivi custom des portes | `InteractWithDoor` gere le chemin, l'ouverture, la fermeture et les autres mobs dans le passage | `VANILLA` : supprimer la copie pour les portes normales des que le trajet utilise un vrai `Path`. |
| Fuite locale et sortie de fluide sans excavation | Cible et mouvement custom; la sortie de fluide peut teleporter le valet | Navigation, comportements de panique du cerveau villageois et pathfinding eau/terre | `HYBRIDE` : conserver la priorite Valet, confier le mouvement au vanilla; aucune teleportation hors sauvetage exceptionnel prouve. |
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
| Minage et bucheron | `Block.getDrops`, outil passe au loot, `destroyBlock`, tags de blocs | `CUSTOM` pour selection, veines, arbres, securite et excavation. Remplacer seulement le deplacement libre par vanilla. |
| Ferme : recolte et replantation | `CropBlock`, loot vanilla, etats de cultures | `HYBRIDE` avec `HarvestFarmland` comme reference exacte pour ble/carottes/pommes de terre/betteraves. Zones, neige, labour, Nether Wart et options restent Valet. |
| Ferme : labour | Son et bloc vanilla, mais mutation directe | Reprendre l'action de houe vanilla quand elle produit exactement le meme resultat. Choix de parcelle et ordre restent `CUSTOM`. |
| Elevage | `Animal.setInLove`, `spawnChildFromBreeding` | `HYBRIDE` : interactions animales vanilla, selection de paire, stock, zones, limites et reservations Valet. |
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
| Excavation de mission | Planificateur 3D, securite fluides/gravier, casse puis `teleportTo` par pas | Planification et casse `CUSTOM`; apres ouverture du passage, faire marcher le valet avec `PathNavigation` au lieu de le teleporter. |
| Defense de groupe | Selection d'ennemi et interruption de mission custom | `HYBRIDE` : mouvement/attaque vanilla, regles de groupe Valet. |
| Accostage | Recherche de berge custom et navigation vanilla, saut force si necessaire | `HYBRIDE` justifie; supprimer uniquement les corrections de position non necessaires. |
| Tickets de chunks | API de ticket Minecraft avec politique Valet bornee | `CUSTOM` necessaire pour la mission longue distance. |

## Identite, cerveau et donnees

| Fonction | Etat | Decision |
| --- | --- | --- |
| Identite Valet et role choisi dans l'UI | Donnees persistantes independantes de la profession | `CUSTOM` necessaire; une profession vanilla ne reproduit pas cette regle. |
| Poste de travail | Memoire vanilla `JOB_SITE` deja utilisee, plus cache/persistence Valet | `HYBRIDE` correct. Etudier `AcquirePoi` / `ValidateNearbyPoi` pour l'acquisition, sans rendre l'identite dependante du poste. |
| Orchestration des metiers | `ValetWorkDriver` ticke une machine d'etat externe au cerveau villageois | `CUSTOM` acceptable pour les ordres. Ne pas reimplementer mouvement, portes et panique a l'interieur. |
| Suppression des memoires `WALK_TARGET`, `LOOK_TARGET`, `PATH` | Effectuee pour reprendre le controle du villageois | A reduire : elle neutralise justement les comportements vanilla que la nouvelle regle demande de reutiliser. |
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

1. Fait en 0.4.2 : remplacer le `teleportTo` des trajets metier par `PathNavigation`, sans toucher aux actions de metier.
2. Fait pour les trajets du fermier : brancher les portes sur `InteractWithDoor`; le ramassage d'items reste a rapprocher du vanilla.
3. Centraliser les transferts sur les primitives vanilla, puis adapter l'intendant au comportement du golem de cuivre.
4. Remplacer les recettes Java par `RecipeManager`.
5. Aligner ferme, combat melee et pose de blocs sur leurs actions vanilla, un metier a la fois avec test en jeu et lecture de `latest.log`.

## References vanilla inspectees dans le jar local 26.2

- `Villager`, `VillagerGoalPackages`, `MoveToTargetSink`, `InteractWithDoor`.
- `HarvestFarmland`, `GoToWantedItem`, `VillagerMakeLove`, `WorkAtPoi`.
- `TransportItemsBetweenContainers`, `CopperGolemAi`, `HopperBlockEntity`.
- `MeleeAttack`, `RangedBowAttackGoal`, `OpenDoorGoal`, `FollowMobGoal`.
- `RecipeManager`, `BlockItem`, `Shearable`.
