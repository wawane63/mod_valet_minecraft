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

Le jar `0.1.3` actuel remplace la premiere publication `0.1.3` afin d'inclure les correctifs de craft, de retour au poste, de debug et de pathfinding testes localement.

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

## Notes de push GitHub

Pour le prochain push `0.1.3`, le message/release note doit mentionner au minimum :

- Better UI et arbre de competences.
- Bouton d'inventaire valet.
- Stabilisation du craft de pioche en pierre.
- Suppression de la dependance implicite aux outils en fer.
- Correction du blocage apres minage de cobble.
- Correction du freeze serveur par limitation de la recherche de ressources.
- Debug actif et lisible dans `latest.log`.
- Installation locale verifiee par hash SHA-256.
