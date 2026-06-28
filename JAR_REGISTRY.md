# Registre des versions et des jars

Ce fichier est la source de tracabilite du mod Valet.

Regles obligatoires :

- Chaque modification testable du jar doit etre documentee ici avant push.
- Chaque build local de test doit installer le jar dans `%APPDATA%\.minecraft\mods`.
- Chaque version doit lister les bugs corriges et les changements structurels.
- Chaque push GitHub de release doit reprendre les notes de la version concernee.
- Aucun push GitHub ne doit etre fait sans validation explicite.

## Etat des jars connus

| Version | Theme | Tag GitHub | Jar local | SHA-256 |
| --- | --- | --- | --- | --- |
| 0.1.0 | Recolte | `v0.1.0` | `build/libs/valet-0.1.0.jar` | `9FEAB6940A8F8416632590EE316C8EEBCC323E9CF002D6E00F336D37690435A7` |
| 0.1.1 | Combat | `v0.1.1` | `build/libs/valet-0.1.1.jar` | `213603857FAAD7317B6ABC1018AD4C7D512D74705020C3608658CDDE14CA2758` |
| 0.1.2 | Craft | `v0.1.2` | `build/libs/valet-0.1.2.jar` | `2C803C8974BCDA6433285BC2C57532A73991E52D3E3666BB227B9E7B65CEC2CE` |
| 0.1.3 | Better UI + correctifs craft | `v0.1.3` | `build/libs/valet-0.1.3.jar` | `B49BBF6280A23EE60C51B4714467410C340FC73EFC919B6DEEE6B0C2A33A495D` |
| 0.2.0 | Portage Minecraft 26.2 | `v0.2.0` | `build/libs/valet-0.2.0.jar` | `919212508C48A3C5448AA907C22C50C398E4106EE83CB808DB6C3535B46DE6D8` |

Le jar `0.2.0` ouvre la branche post-`0.1.3` et inclut le portage Minecraft `26.2`, Fabric Loader `0.19.3`, Fabric API `0.153.0+26.2`, Java `25` et l'UI complete portee en 26.2.

## 0.2.0 - Portage Minecraft 26.2

Objectif : rendre le mod lancable et jouable via Minecraft Launcher en `26.2`.

Bugs corriges / portage :

- Les mixins sont temporairement desactives pour eviter les crashs de chargement pendant le portage.
- L'ecran d'ordres complet est restaure en 26.2 : pages general/epee/arc/inventaire, XP, perks et selection d'ordres.
- L'UI affiche maintenant un apercu d'item pour les crafts.
- Les textes custom de l'UI sont rendus avec une couleur opaque pour etre visibles en 26.2.
- Les definitions `assets/valet/items/*.json` corrigent les icones violet/noir des blocs/items en 26.2.
- Les noms `item.valet.*` sont traduits pour eviter les noms techniques en inventaire.
- Les ressources mine/bois a `0` ne sont plus listees comme cibles disponibles.
- La touche `E` ne ferme plus l'UI quand le champ de nom du valet est actif.
- Le poste de valet se comporte comme un poste metier vanilla : un seul ticket POI, un seul valet par poste, et nettoyage des anciens doublons `JOB_SITE`.
- Le bucheron coupe maintenant tout le cluster de l'arbre apres 3 secondes, avec voisins diagonaux et limite relevee a 512 logs.
- Les valets reservent les blocs de mine/bois et les blocs casses pour le passage, pour eviter qu'un autre valet reste bloque sur une cible deja prise.
- Les retours apres minage sont corriges : plus de preemption vers `FIND_TARGET` pendant depot/retour coffre, et retour au poste quand il n'y a plus de cible ni d'items.
- La perte du poste de valet demote maintenant le villageois, nettoie l'ordre actif et retire le glow serveur.
- Le glow serveur reste apres fermeture de l'UI et n'est retire que quand le valet perd son metier.
- L'ecran d'ordres ne pose plus de flag glow client local, pour eviter un glow bloque apres perte du metier.
- La coupe d'arbre inclut les logs connectes du meme arbre, y compris les connexions diagonales/hautes.
- Le rendu preview construction monde reste a verifier en jeu.
- Le jar est installe localement dans `%APPDATA%\.minecraft\mods` avec le profil launcher `Fabric 26.2 - Valet`.

## 0.1.0 - Recolte

Objectif : premiere version jouable centree sur la recolte et le minage.

Bugs corriges :

- Correction du demarrage du travail des valets apres attribution d'un ordre.
- Correction du redemarrage du goal quand un ordre change.
- Correction du cheminement initial : le valet recommence a executer ses chemins au lieu de rester bloque.
- Correction du path traversal de base pour rendre la recolte/minage utilisable en jeu.

Modifications structurelles :

- Ajout du metier `Valet` et du poste de valet.
- Ajout des ordres de recolte/minage/bois initiaux.
- Ajout du depot vers coffres/barils proches.
- Mise en place du goal principal `ValetWorkGoal`.
- Mise en place de la sauvegarde des donnees de valet et des ordres.

## 0.1.1 - Combat

Objectif : ajouter le role combat et les progres/perks associes.

Bugs corriges :

- Correction de l'animation de combat invisible : envoi explicite du paquet vanilla `EntityAnimationS2CPacket.SWING_MAIN_HAND`.
- Correction du rendu rose/violet des valets zombifies : les zombie-valets retombent sur une apparence vanilla.
- Correction de plusieurs cas de suivi de cible et de priorite combat.
- Correction de la synchronisation UI apres choix de perks combat.

Modifications structurelles :

- Ajout de `CombatRuntimeTask` et de la logique de ciblage combat.
- Ajout des perks combat et de la progression combat.
- Ajout du coffre a fleches infinies.
- Ajout de mixins pour les projectiles et le rendu d'items tenus.
- Ajout des payloads reseau dedies au choix de perk combat.
- Debut de separation des reglages metier dans `ValetWorkSettings`.

## 0.1.2 - Craft

Objectif : ajouter le premier ordre de craft, avec collecte automatique des besoins.

Bugs corriges :

- Correction du craft qui ne repartait pas proprement apres relance du meme ordre.
- Correction des besoins de craft : le valet identifie cobblestone, bois, planches et sticks manquants.
- Correction du restock depuis les coffres proches pour les materiaux de craft.
- Correction des boucles d'inventaire plein pendant les etapes de craft.
- Correction de la distinction entre ordre actif, retour au poste et depot pendant le craft.

Modifications structurelles :

- Ajout de `CraftingRuntimeTask`.
- Ajout de `ValetCraftTarget`.
- Ajout de l'ordre `CRAFT`.
- Extension de `ValetOrderKey` pour redemarrer correctement les ordres craft.
- Ajout de l'etat `CRAFTING` et du `PathPurpose.CRAFT`.
- Ajout de l'UI et des payloads reseau pour choisir le craft.

## 0.1.3 - Better UI + maintenance craft

Objectif : ameliorer l'interface, l'arbre de competences et stabiliser le craft avant portage.

Bugs corriges :

- Correction du craft de pioche en pierre qui demandait indirectement des outils en fer pour miner la cobble.
- Correction du valet qui minait trois cobbles puis restait bloque loin du poste.
- Correction de la boucle `craft no_workstation_path` apres recolte des ressources.
- Correction du gros freeze serveur pendant le craft : limitation de la recherche de path a 32 ressources proches.
- Correction du debug non exploitable : debug log actif par defaut et anti-spam.
- Correction de la recuperation du poste trop stricte verticalement apres descente dans un trou.
- Correction des budgets de pathfinding trop bas depuis la refonte UI.
- Correction du retour au poste quand un ordre craft actif transporte encore des items.
- Correction partielle du bois/feuilles : le path peut degager des blocs naturels pour `CRAFT` et `HOME`.

Modifications structurelles :

- Refonte de l'arbre de competences en fenetre plus grande, avec depart en bas facon Skyrim.
- Reorganisation des perks ressources, epee et arc.
- Ajout du bouton d'inventaire valet.
- Ajout du debug permanent throttled dans `ValetDebug`.
- Ajout d'une selection de cible craft bornee pour eviter les A* massifs.
- Restauration de budgets pathfinding plus coherents avec l'UI actuelle.
- Extension de la logique de retour au poste et de recherche de workstation.

## 0.1.3 - Portage Minecraft 26.2 local

Objectif : rendre le mod lancable via Minecraft Launcher en `26.2`.

Bugs corriges / portage :

- Migration Gradle vers `net.fabricmc.fabric-loom` et Java 25.
- Migration des mappings Yarn vers mappings Mojang/officiels.
- Migration Fabric networking/menu vers payloads types `CustomPacketPayload`.
- Migration des NBT item vers `DataComponents.CUSTOM_DATA`.
- Migration partielle des APIs `SavedData`, `BlockEntity`, `VillagerProfession`, `ServerTickEvents`, messages joueur et tags minerais.
- Installation du profil launcher `Fabric 26.2 - Valet`.

Limitations temporaires :

- Mixins neutralises.
- Rendu preview construction neutralise.
- Ecran d'ordres complet restaure pour compatibilite 26.2.

## Notes de push GitHub

Pour le push `0.2.0`, le message/release note doit mentionner au minimum :

- Portage Minecraft `26.2`, Fabric Loader `0.19.3`, Fabric API `0.153.0+26.2`, Java `25`.
- Migration Gradle/Fabric Loom, mappings Mojang, networking/menu Fabric 26.2 et NBT item components.
- UI Valet complete restauree en 26.2.
- Corrections d'icones, traductions, glow, perte de job, poste unique et blocages entre valets.
- Installation locale verifiee par hash SHA-256.
