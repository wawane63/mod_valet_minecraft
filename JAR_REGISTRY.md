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
| 0.2.1 | Fermier local | `v0.2.1` | `build/libs/valet-0.2.1.jar` | `B6AB19489870FA8161BC3D61E4BC26CEB9D558A038AD9FC8DA16B3D31B9F1D3A` |
| 0.3.0 | Decoupage metiers | `v0.3.0` | `build/libs/valet-0.3.0.jar` | `CDA87ACD625BF55BB0DF7F616D5295C5EDE34AFA5F57C993C8FB64AB18D489B3` |
| 0.3.1 | Crafts bois | `v0.3.1` | `build/libs/valet-0.3.1.jar` | `3003512D386DCC04BFC55DB42072BD10C9BFD8A9E498446E1A6684BC2DA772EB` |
| 0.3.2 | Tri coffre + magicien | `v0.3.2` | `build/libs/valet-0.3.2.jar` | `9E01576BE53A9BB89048E559461E96BFB8DBA8D2A006BD1B4E3C7E2B0E65D5E3` |
| 0.3.3 | Creation de groupe | integre dans `v0.3.4` | pas de jar separe | - |
| 0.3.4 | Eleveur | `v0.3.4` | `build/libs/valet-0.3.4.jar` | `2DC045E5225419A7EAB3652A096F83B40D9FF539A9B93E5E8E60AF2A2827B2B5` |
| 0.3.5 | Blueprints ameliores | `v0.3.5` | `build/libs/valet-0.3.5.jar` | `3D5E4513A4AC0FCB6FF9DCF8248E0BDBD2AE372A0A59FC87149706652C2DCE1C` |
| 0.3.6 | Cuisinier + correctifs metiers | `v0.3.6` | `build/libs/valet-0.3.6.jar` | `46FB736093CD499193BA415EB2889EE477CC066BDF2C849087909F66C9269807` |
| 0.3.7 | Intendant + transferts coffres | `v0.3.7` | `build/libs/valet-0.3.7.jar` | `F917E72A650AFC405FC79E76BA924DD373049C2FA505ED60BF57521546D1D3D2` |
| 0.3.8 | Audit, carte tactique et missions de groupe | `v0.3.8` | `build/libs/valet-0.3.8.jar` | `B51AE5685B80EFCC8022D3141127D08D3BD99671DCB75BC146E5BD4E623071FB` |
| 0.3.9 | Gestion centralisee des groupes | `v0.3.9` | `build/libs/valet-0.3.9.jar` | `3C41471973EEB2F60C9873337A6C75349436263809B9E93109AD50C70EEFAF06` |
| 0.4.0 | Tag Valet independant du poste | `v0.4.0` | `build/libs/valet-0.4.0.jar` | `01A717BF93DB703F698D3BEC9D7BEE2675A6D480CFCAB2EB311A98195CB61688` |
| 0.4.1 | Maire, quetes et raccourcis | `v0.4.1` | `build/libs/valet-0.4.1.jar` | `757312C85636AC36C772722961A23EAC5194BB1ECBD6888CBFAE1D151DD60156` |
| 0.4.2 | Navigation vanilla, fermier et ameliorations generales | `v0.4.2` | `build/libs/valet-0.4.2.jar` | `1853752E58FE1C16A8C78B0E54FD2FC292AA9DC20555917A400FE401F5F46CA5` |
| 0.4.3 | Navigation de surface et maire unique interactif | `v0.4.3` | `build/libs/valet-0.4.3.jar` | `4ED4C5A9BC3C23FB2C66808E5ABDD4D6780114A91BDE22428D9FCF8A06C06E27` |

Le jar `0.2.1` correspond a la release `v0.2.1`, juste avant le decoupage metiers.
Le jar `0.3.0` correspond a la release `v0.3.0`.
Le jar `0.3.1` correspond a la release `v0.3.1`.
Le jar `0.3.2` correspond a la release `v0.3.2`.
La version `0.3.3` correspond a la creation de groupe, integree dans la publication `0.3.4`.
Le jar `0.3.4` correspond a l'eleveur.
Le jar `0.3.5` correspond aux blueprints ameliores.
Le jar `0.3.6` ajoute le cuisinier et les correctifs metiers issus du test en jeu.
Le jar `0.3.7` ajoute l'intendant et les transferts filtres entre coffres.
Le jar `0.3.8` regroupe l'audit exhaustif et la carte tactique avec missions de groupe.
Le jar `0.3.9` centralise la gestion des groupes sous la carte et retire les anciens objets/blocs de commande.
La version `0.4.0` commence la migration vers une identite Valet marquee directement sur le villageois.
La version `0.4.1` ajoute le maire, les quetes de livraison et les raccourcis `J` / `K`.
La version `0.4.2` publie l'ensemble de la branche `0.4.x` avec le maire et ses quetes, puis remplace les teleportations de deplacement par la navigation vanilla sous garde-fous Valet et fiabilise le fermier.
La version `0.4.3` donne la priorite aux petits trajets de surface, empeche les galeries peu profondes et rend le maire unique directement interactif.

## 0.4.3 - Navigation de surface et maire unique interactif

Objectif : conserver l'excavation comme dernier recours lorsqu'un trajet de surface existe.

Fonctionnalite :

- Validation de support partagee avec `ValetSafeNavigation`, y compris chemins en terre, terres labourees et escaliers.
- Essais de surface bornes a 12, 8, 4 puis 24 blocs avec detours alternes.
- Quatre echecs de surface requis avant le lancement d'une galerie.
- Refus d'un chemin d'excavation si une surface sure se trouve de un a quatre blocs au-dessus.
- Comptage et retrait des livraisons de quetes sans conteneur nul en Minecraft 26.2.
- UUID de maire persistant par dimension et suppression automatique des doublons charges.
- Maire immobilise pres de sa cloche, equipe d'un trident visible et ouvrant les quetes au clic droit.
- UI de quetes lisible avec textes opaques, icones d'objets, progression d'inventaire et bilan apres livraison.
- Portes metalliques fermees traitees comme obstacles; ouverture reservee aux portes en bois.
- Signatures de debug `surface_path`, `surface_failed` et `surface_exhausted`.

Verification :

- Compilation Java : OK.
- Build complet et installation locale : OK.
- Jar actuel : `valet-0.4.3.jar`.
- SHA-256 : `4ED4C5A9BC3C23FB2C66808E5ABDD4D6780114A91BDE22428D9FCF8A06C06E27`.
- Dossier mods : un seul jar Valet, hash identique au build.
- Bootstrap serveur : Minecraft 26.2, Fabric Loader 0.19.3 et Valet 0.4.3 charges; arret EULA attendu.
- Publication GitHub : `v0.4.3`.

## 0.4.2 - Navigation vanilla, fermier et ameliorations generales

Objectif : faire marcher physiquement les valets tout en conservant une enveloppe de securite deterministe.

Fonctionnalite :

- Suppression de tous les `teleportTo` du runtime Valet.
- `PathNavigation` pour les trajets metier, l'excavation et la sortie d'eau.
- Validation anti-chute, anti-fluide, anti-danger et anti-detour anormal avant lancement.
- Surveillance de progression, timeout et replanification apres interruption.
- Planificateur d'excavation conserve pour choisir et ouvrir un passage sur les missions longues.
- Nage vanilla de groupe vers la rive opposee ou par points d'eau locaux, sans excavation sous-marine.
- Carte tactique fluidifiee par cache de terrain borne, reconstruction limitee a 20 Hz et texture dynamique unique.
- Nage pilotee par les controles vanilla apres approche terrestre de la rive; pas de pathfinding terrestre vers un bloc d'eau.
- Cible de nage stabilisee, recuperation automatique apres 40 ticks sans progres et espacement anti-collision des suiveurs.
- Pas de galerie securisee parcourus par mouvement adjacent vanilla pour supprimer les refus A* et les a-coups par bloc.
- Normalisation du premier noeud vanilla sur les supports partiels, notamment la terre labouree, pour restaurer les trajets du fermier.
- Memorisation temporaire des cibles de ferme dont la navigation echoue afin d'eviter les relances en boucle.
- Le fermier utilise une cible continue `WALK_TARGET` comme `HarvestFarmland`; `MoveToTargetSink` execute le trajet complet et `InteractWithDoor` gere les portes en bois.
- L'approche agricole cible une case sure adjacente, maintient sa `WALK_TARGET` si elle est effacee et reste bornee sans mouvement bloc par bloc.
- Les actions agricoles sont choisies par proximite; la demande de plantation precede les autres cibles et cherche les coffres aux quatre coins du champ.
- Une demande de plantation sans stock est differee de 200 ticks au lieu de bloquer les recoltes et le labour restants.
- Le premier noeud peut quitter un lit ou un mobilier non dangereux sans viser la porte comme destination intermediaire.
- La replantation est active par defaut sur les nouveaux ordres de ferme; les pommes de terre sont replantees depuis leur drop collecte lorsque l'option est active.
- Si le fermier est deja debout sur un sol de culture vide, il plante directement l'item compatible au lieu d'echouer sur `farm no_path`.
- La plantation automatique couvre ble, carottes, pommes de terre et betteraves sur terre labouree, ainsi que les verrues du Nether sur sable des ames.
- Le bouton `Suppr. champ` retire la zone persistante selectionnee et annule les ordres encore lies a cette zone.
- La logistique conserve une pile par item de plantation actif et le fermier peut retirer d'un coffre proche une pile compatible lorsqu'un sol vide doit etre replante.

Verification :

- Compilation Java : OK.
- Build complet et installation locale : OK.
- Jar actuel : `valet-0.4.2.jar`.
- SHA-256 : `1853752E58FE1C16A8C78B0E54FD2FC292AA9DC20555917A400FE401F5F46CA5`.
- Dossier mods : un seul jar Valet, hash identique au build.
- Bootstrap serveur : Minecraft, Fabric et Valet 0.4.2 charges; arret EULA attendu.
- Publication GitHub : preparee pour le tag `v0.4.2` avec ce jar.

## 0.4.1 - Maire et quetes

Fonctionnalite :

- Maire persistant genere pres d'une cloche si un chat et un golem sont presents.
- Trois quetes de livraison persistantes, controlees cote serveur et recompensees en emeraudes.
- Menu des quetes sur `J` et carte tactique sur `K`, touches reconfigurables.
- Retrait du bouton de carte dans le menu Echap.

Verification :

- Compilation Java : OK.
- Build complet et installation locale : OK.
- Jar actuel : `valet-0.4.1.jar`.
- SHA-256 : `757312C85636AC36C772722961A23EAC5194BB1ECBD6888CBFAE1D151DD60156`.
- Dossier mods : un seul jar Valet, hash identique au build.
- Publication GitHub : `v0.4.1`.

## 0.4.0 - Tag Valet

Objectif : dissocier l'identite du valet de la profession vanilla et de la presence d'un poste.

Fonctionnalite :

- `Insigne de valet` utilisable sur un villageois adulte.
- Identite `ValetTagged` persistante dans les donnees de l'entite.
- Migration automatique des anciens valets bases sur la profession.
- Identite conservee sans poste et pendant les missions longue distance.
- Role Artisan au premier tag, puis choix du métier dans l'interface.
- Les postes ne créent plus l'identité Valet et les anciennes restaurations destructrices sont supprimées.
- Les missions de groupe reprennent le chemin 3D des artisans pour creuser des escaliers, contourner les fluides et repartir après 40 ticks d'immobilité.
- La galerie de mission réserve trois blocs de hauteur pour le pathfinding des suiveurs et le creuseur oriente son corps vers chaque action.
- Le meneur de mission est stable; il n'attend plus obligatoirement à 5 blocs, tandis que les suiveurs le rattrapent avec des recalculs bornés et sans chemins d'excavation en boucle.

Verification :

- Compilation Java : OK.
- Jar actuel : `valet-0.4.0.jar`.
- SHA-256 : `01A717BF93DB703F698D3BEC9D7BEE2675A6D480CFCAB2EB311A98195CB61688`.
- Build Gradle complet et installation locale : OK.
- Dossier mods : un seul jar Valet, hash identique au build.
- Bootstrap serveur : mod 0.4.0 et ressources charges; arret attendu sur l'EULA de test.
- Publication GitHub : `v0.4.0`.

## 0.3.9 - Gestion centralisee des groupes

Objectif : simplifier les groupes autour d'un menu unique accessible depuis la carte des valets.

Fonctionnalite :

- Onglets `Carte` et `Groupes de valets` dans le menu ouvert depuis Echap.
- Creation, suppression et affectation des valets dans l'onglet dedie.
- Selection partagee avec la carte pour l'envoi au repere et le rappel.
- Persistance des groupes et runtime de mission conserves.

Nettoyage :

- Pupitre de groupe, carte de groupe et liaison des cornes retires.
- Ancien menu, payload, interactions, recettes, loot table, modeles et traductions supprimes.

Verification :

- Compilation Java : OK.
- Jar actuel : `valet-0.3.9.jar`.
- SHA-256 : `3C41471973EEB2F60C9873337A6C75349436263809B9E93109AD50C70EEFAF06`.
- Build Gradle complet et installation locale : OK.
- Dossier mods : un seul `valet-*.jar`, hash identique au build.
- Publication GitHub : `v0.3.9`.

## 0.3.8 - Audit et carte tactique

Objectif : fournir une carte plein ecran depuis le menu Echap avant d'ajouter les missions de groupe.

Fonctionnalite :

- Bouton `Carte des valets` ajoute au menu Echap.
- Terrain des chunks deja charges affiche avec couleurs topographiques.
- Zoom, deplacement, recentrage et coordonnees du curseur.
- Marqueurs joueur, valets visibles et repere personnel.
- Legende et commandes integrees a l'ecran.
- Aucun chargement de chunk provoque par l'interface.
- Gestion des groupes depuis la carte avec affectation des valets visibles.
- Ordre `Aller au repere` pour tous les metiers, execute par pathfinding local continu.
- Etapes de navigation de 24 blocs, rafraichies au maximum toutes les 20 ticks avec angles de contournement sur echec.
- Tickets de mission temporaires et non persistants, limites a 32 centres de rayon deux chunks.

Verification :

- Compilation Java : OK.
- Jar actuel : `valet-0.3.8.jar`.
- SHA-256 : `B51AE5685B80EFCC8022D3141127D08D3BD99671DCB75BC146E5BD4E623071FB`.
- Build Gradle complet et installation locale : OK.
- Dossier mods : un seul `valet-*.jar`, hash identique au build.
- Bootstrap serveur : ticket `group_mission`, paquets reseau et mod charges; arret attendu sur EULA de test.
- Publication GitHub : `v0.3.8`.

Audit et maintenance :

Objectif : fiabiliser et optimiser l'ensemble du mod sans modifier volontairement ses fonctionnalites visibles.

Fixed :

- Persistence NBT restauree par le mixin villageois 26.2 et cache de role persistant.
- Mixin projectile migre vers `AbstractArrow`; les anciens mixins vides ont ete retires.
- Validation reseau renforcee : menu, valet, UUID, dimension, distance, indices et tailles.
- Rollback des ingredients/materiaux sur echec et respect des regles de slots/doubles coffres.
- Donnees NBT invalides ou surdimensionnees bornees; chargements arbitraires de chunks evites.
- Homes hors limites, migration des roles 0.3.7 et conflits de poste dupliques corriges.
- Resumes de blueprint compacts, listes de groupe bornees et validation menu/pupitre ajoutees.
- Tri de coffre rendu compatible avec les 9 filtres de l'intendant.
- Comptage des materiaux porte/lit corrige.
- Double drop du coffre cuisinier corrige et loot table du coffre de fleches ajoutee.
- Recettes, loot tables et tags de blocs migres vers les chemins et schemas JSON requis par Minecraft 26.2; le drop blueprint utilise `copy_custom_data`.

Optimized :

- Allocations et scans par tick reduits ou temporises.
- Reservations expirees purgees automatiquement.
- Scans de construction bornes et snapshots GUI adaptes au role.
- Apercus de blueprint 16x16 compacts pour borner les paquets d'ouverture.

Cleaned :

- Sept classes Java mortes, une archive asset orpheline et huit entrees de traduction supprimees.
- Option `Nourrir` inactive et surcharges sans appel supprimees.
- Dependances/manifeste Gradle centralises; checksum wrapper et installation multiplateforme ajoutes.
- Fabric API mis a jour vers `0.154.2+26.2` et Loom vers `1.17.14`.
- Licence MIT, regles d'edition et normalisation Git ajoutees.

## 0.3.7 - Intendant et transferts coffres

Objectif : ajouter un metier dedie au rangement automatique entre coffres, avec filtres visibles et priorites simples.

Bugs corriges / fonctionnalite :

- Ajout du poste `valet:steward_workstation` et du role `Intendant`.
- L'intendant scanne les coffres, coffres pieges et barils proches du poste.
- Les 9 premiers slots d'un conteneur filtrent les items acceptes et sont reserves au joueur.
- Les coffres/barils sans filtre servent d'entree pour alimenter les destinations filtrees.
- Les filtres sont prioritaires de gauche a droite, puis par proximite avec le poste.
- Si aucun filtre ne correspond, l'intendant peut regrouper une pile dans un coffre/baril non filtre qui contient deja le meme item.
- Les mauvais items presents dans un coffre filtre peuvent etre deplaces vers une destination compatible.
- Le transfert passe par l'inventaire du valet et donne de l'XP selon le nombre d'items deposes.
- Le chemin d'intendant ne mine pas de blocs, pour proteger les zones de stockage.

Verification :

- Jar actuel : `valet-0.3.7.jar`
- SHA-256 : `F917E72A650AFC405FC79E76BA924DD373049C2FA505ED60BF57521546D1D3D2`
- Build Gradle local OK, jar installe dans le dossier `mods`.
- Verification du dossier `mods` : un seul `valet-*.jar`, `valet-0.3.7.jar`.

## 0.3.6 - Cuisinier et correctifs metiers

Objectif : ajouter un metier de cuisine autonome et corriger les boucles et blocages observes dans `latest.log`.

Bugs corriges / fonctionnalite :

- Ajout du poste `valet:cook_workstation` et du role `Cuisinier`.
- Ajout du coffre dedie `valet:cook_chest` avec 27 emplacements et inventaire sauvegarde.
- Le cuisinier ignore les coffres ordinaires pour les ingredients et les repas.
- Recolte automatique du ble et des pommes de terre murs proches, avec replantation si l'inventaire contient la semence.
- Collecte des ingredients crus depuis le coffre de cuisinier proche.
- Preparation automatique de pain, pommes de terre cuites, viandes et poissons cuits.
- Depot des repas termines dans le coffre de cuisinier proche.
- Le support magique ne se lance plus en boucle sur des allies en pleine sante.
- Regeneration et resistance ne sont rafraichies qu'a expiration et dans un contexte utile.
- Le craft de pioche reconnait les variantes de buches et de planches par tags items et blocs.
- Le fermier retire neige en couche et bloc de neige avant labour.
- Le fermier plante les terres labourees vides avec une culture active disponible dans son inventaire.
- La construction et le combattant n'ont pas produit de signature d'erreur concluante dans le log fourni.

Verification :

- Jar actuel : `valet-0.3.6.jar`
- SHA-256 : `46FB736093CD499193BA415EB2889EE477CC066BDF2C849087909F66C9269807`
- Build Gradle local OK, jar installe dans le dossier `mods`.

## 0.3.5 - Blueprints ameliores

Objectif : ameliorer la pose des chantiers et prevenir les departs sans materiaux.

Bugs corriges / fonctionnalite :

- Rotation du chantier selon la direction du joueur au moment de poser le blueprint.
- Pose accroupie du blueprint pour construire une version miroir.
- Apercu monde mis a jour avec la meme rotation et le meme miroir que le chantier reel.
- Le bloc blueprint sauvegarde le mode miroir.
- Le drop du bloc blueprint conserve le mode miroir.
- Le valet calcule les materiaux requis avant de partir construire.
- Le message de blocage liste les premiers materiaux manquants avec les quantites.
- Le calcul des materiaux prend en compte l'inventaire du valet et les coffres/barils proches du poste ou du blueprint.

Verification :

- Jar actuel : `valet-0.3.5.jar`
- SHA-256 : `3D5E4513A4AC0FCB6FF9DCF8248E0BDBD2AE372A0A59FC87149706652C2DCE1C`
- Build Gradle local OK, jar installe dans `%APPDATA%\.minecraft\mods`.

## 0.3.4 - Eleveur

Objectif : ajouter un poste dedie a l'elevage local.

Bugs corriges / fonctionnalite :

- Ajout du `Poste d'eleveur` (`valet:poste_eleveur`).
- Ajout de la `Balise d'enclos`.
- Deux balises d'enclos creent une zone poules, vaches, moutons ou cochons selon les animaux presents.
- Interface dediee : selection d'enclos, reproduire, tondre, ramasser oeufs, traire, abattre surplus et limite max animaux.
- Ajout de `BreedingRuntimeTask`.
- Ajout de reservations d'animaux pour eviter que deux valets ciblent le meme animal.
- L'eleveur prend graines, ble, carottes, seaux et cisailles dans les coffres/barils proches.
- Les oeufs sont ramasses dans l'enclos, les moutons peuvent etre tondus et les vaches traites.
- Les portillons fermes peuvent etre ouverts pendant le trajet vers les enclos.
- Correction : le `Poste d'eleveur` ne copie plus les proprietes lumineuses du smoker, ce qui evitait un crash au demarrage.
- Correction : les portillons ouverts par le valet sont refermes automatiquement apres son passage.
- Correction : l'elevage ne nourrit plus les animaux hors reproduction, pour ne plus vider le ble inutilement.
- Correction : une vache traitee passe en cooldown avant de pouvoir etre retraitee.
- Correction : `Reproduire` nourrit maintenant les deux adultes avant de verifier la paire, au lieu d'attendre qu'ils soient deja en amour.
- Correction apres log : l'eleveur ne prend plus seau/cisailles sans cible valide et les enclos tolerent un decalage vertical de balises.
- Ajout : option `Abattre surplus`, uniquement dans les enclos balises, au-dessus de 4 animaux par bloc de surface.
- Correction : les parents reproduits passent en cooldown interne pour eviter les bebes en rafale.
- Correction apres log : `Max` devient aussi le plafond d'abattage; la densite `surface * 4` reste seulement une limite de securite.
- Ajustement : le `Max` animaux par defaut passe de 12 a 8.
- Migration : les limites animaux sauvegardees passent a environ deux tiers au prochain chargement.
- Correction : le fermier cible et ramasse les drops de cultures deja au sol dans sa zone.
- Correction apres log : un depot vide ne bloque plus les valets en boucle coffre, ils reprennent le travail si possible.
- Correction apres log : avec `Abattre surplus`, l'eleveur abat des que l'enclos atteint `Max`, ce qui relance le cycle au lieu de rester bloque a la limite.
- Correction : l'abattage priorise les parents en cooldown, donc ceux qui viennent de reproduire.

Verification :

- Jar actuel : `valet-0.3.4.jar`
- SHA-256 : `2DC045E5225419A7EAB3652A096F83B40D9FF539A9B93E5E8E60AF2A2827B2B5`
- Build Gradle local OK, jar installe dans `%APPDATA%\.minecraft\mods`.

## 0.3.3 - Creation de groupe

Objectif : creer et commander des groupes de valets.

Bugs corriges / fonctionnalite :

- Ajout du `Pupitre de groupe`, base sur le lutrin.
- Ajout d'une interface de gestion : creation de groupes, valets proches, ajout/retrait par clic.
- Ajout d'une `Carte de groupe` liee au groupe depuis l'interface.
- Une corne de chevre tenue en main peut etre liee a un groupe.
- Ordres de groupe : suivre, garde proche, garde large, attaque cible, attaque de zone et rappel.
- La carte ou la corne liee controle le groupe : clic droit pour suivre, accroupi + clic droit pour cycler les ordres, clic sur monstre pour attaquer une cible, clic sur bloc pour attaquer une zone.
- Les combattants et magiciens priorisent les cibles de groupe.
- Les valets non combattants suivent et se rappellent sans recevoir de logique de combat forcee.
- Le debug valet affiche le mode de groupe actif.
- Correction apres lecture de `latest.log` : les commandes identiques ne relancent plus le groupe en boucle.
- Correction apres lecture de `latest.log` : attaque/garde ne deplacent plus les artisans/fermiers vers les zones de combat.
- Correction apres lecture de `latest.log` : ajout de la texture zombie-valet manquante.
- Correction : les artisans et fermiers fuient les monstres proches.
- Correction : stabilisation de l'item tenu en groupe pour eviter l'alternance epee/pioche.

Verification :

- Pas de jar separe publie pour `0.3.3`.
- Contenu integre dans la publication `0.3.4`.

## 0.3.2 - Tri coffre + magicien

Objectif : ajouter un tri rapide dans les coffres/barils et un role magicien.

Bugs corriges / fonctionnalite :

- Ajout d'un bouton `Tri` dans l'ecran des coffres/barils.
- Le tri passe par un payload serveur `sort_container`.
- Le serveur trie uniquement le `ChestMenu` actuellement ouvert.
- Les piles identiques sont fusionnees, puis rangees par identifiant d'item.
- Ajout du role `Magicien`.
- Ajout du `Poste de magicien`.
- Le magicien defend localement avec un sort de glace : projectile boule de neige, degats et ralentissement, sans incendie.
- Ajout d'un arbre magie en 3 branches : `Destruction`, `Soin`, `Alteration`.
- Le perk `Gel` est acquis automatiquement par les magiciens et ne consomme pas de point.
- Branche `Destruction` : `Gel`, `Crocs magiques`, `Fracas`.
- Branche `Soin` : `Soin`, `Aura de soin`.
- Branche `Alteration` : `Rempart`, `Affaiblir`.
- Le perk `Crocs magiques` lance des crocs apres 3 sorts de glace.
- Correction : les reservations fantomes de poste deja utilise ne bloquent plus le nouveau magicien.
- Correction : la perte du metier nettoie aussi la fiole tenue.
- Correction : le magicien ne tient plus de fiole en main et garde les bras normaux hors combat.
- Correction : le compteur des crocs reste actif entre changements de cible et la portee magique passe a 12 blocs.
- Le craft du poste de magicien reprend celui du poste de combattant avec des fioles a la place des epees.
- Premiere couche client d'animations player-like pour les valets : modele joueur, item en main, pose arc, swing outils/epee.
- Correction : le magicien utilise aussi le renderer player-like et leve les bras seulement pendant le lancement d'un sort.
- Correction : le nom generique `profession.valet.valet` est masque, mais les vrais noms saisis restent affiches.
- Correction : le deplacement metier fiable est restaure pour craft, ressources, construction, retour poste et champs.
- Correction : les valets font face a la prochaine case pendant le deplacement metier.
- Restauration de l'aperçu monde quand un blueprint de construction est tenu.
- Correction : poser un villageois par oeuf ne crash plus le rendu client du renderer valet.
- Correction : les valets ne suivent plus les memoires vanilla de village/cloche quand ils ont un poste.
- Ajout des options `Ne pas aller se coucher` et `Comportement libre` dans l'UI.
- L'option `Ne pas aller se coucher` est decochee par defaut : les valets vont dormir la nuit. Si elle est cochee, ils ne vont plus au lit.
- Clic droit sur un poste : le valet lie revient au poste meme avec un ordre actif, et y reste 5 secondes avant de reprendre sa routine libre si l'option est active.

Verification :

- Jar actuel : `valet-0.3.2.jar`
- SHA-256 : `9E01576BE53A9BB89048E559461E96BFB8DBA8D2A006BD1B4E3C7E2B0E65D5E3`
- Build Gradle local OK, jar installe dans `%APPDATA%\.minecraft\mods`.

## 0.3.1 - Crafts bois

Objectif : rendre les postes de metier accessibles plus tot en remplacant les outils en fer par des outils en bois.

Bugs corriges / fonctionnalite :

- Le `Poste d'artisan` utilise une pioche en bois et deux haches en bois.
- Le `Poste de combattant` utilise deux epees en bois.
- Le `Poste de combattant` utilise deux buches a la place des lingots de fer.
- Le `Poste de fermier` utilise une houe en bois.
- Le guide GitHub des objets et crafts est mis a jour.

Verification :

- Jar actuel : `valet-0.3.1.jar`
- SHA-256 : `3003512D386DCC04BFC55DB42072BD10C9BFD8A9E498446E1A6684BC2DA772EB`
- Build Gradle local OK, jar installe dans `%APPDATA%\.minecraft\mods`.

## 0.3.0 - Decoupage metiers

Objectif : separer les competences en metiers distincts avec un poste dedie par role.

Bugs corriges / fonctionnalite :

- Ajout de trois postes : artisan, combattant et fermier.
- L'artisan garde les ordres minerais, bois, construction et craft.
- Le combattant est limite au combat defensif et aux arbres epee/arc.
- Le fermier est limite a la recolte et aux options de champs.
- Ajout d'un arbre fermier dedie : vitesse, portee, replantation, labour, stockage, intendant.
- Extension des arbres combat : portee epee, garde renforcee, tir lointain et volee.
- L'UI masque les ordres, pages et perks non compatibles avec le role actif.
- Les payloads reseau refusent les ordres/perks hors role.
- Le goal nettoie automatiquement un ordre incompatible si le poste change.
- Les bonus de perks sont limites au metier actif.
- Les nouveaux postes ont assets, loot tables, recettes, tags de minage et traductions.
- Le poste stocke du valet redevient prioritaire sur la memoire vanilla `JOB_SITE`.
- Les claims `JOB_SITE` de villageois non-valets sur un poste Valet reserve sont nettoyes.
- Le scan d'attribution ignore les doublons quand plusieurs joueurs couvrent le meme villageois.
- Le glow pose par le mod est suivi et retire sur tous les chemins de perte/restauration impossible du metier.

Verification :

- Jar actuel : `valet-0.3.0.jar`
- SHA-256 : `CDA87ACD625BF55BB0DF7F616D5295C5EDE34AFA5F57C993C8FB64AB18D489B3`
- Build Gradle local OK, jar installe dans `%APPDATA%\.minecraft\mods`.

## 0.2.1 - Fermier local

Objectif : recolter automatiquement les cultures mures, avec zone optionnelle, choix des cultures, replantation et passage de houe.

Bugs corriges / fonctionnalite :

- Ajout de l'ordre `Recolter` dans l'UI du valet.
- Ajout de la `Balise de ferme` verte pour creer une zone de travail avec deux marqueurs.
- Selection possible entre toutes les cultures proches et une zone de ferme balisee.
- Choix des cultures travaillees : ble, carottes, patates, betteraves, verrues du Nether.
- Ajout des checkboxes `Replanter` et `Passer la houe`.
- Replantation depuis l'inventaire du valet pour ble, carottes, pommes de terre, betteraves et verrues du Nether.
- Passage de houe sur les sols de la zone pour transformer les sols nus en terre labouree.
- Le valet fermier tient une houe ; le valet sans travail tient un cookie ou une torche.
- Integration avec les chemins, reservations de blocs et depots en coffre existants.
- Les zones de ferme sont limitees a la couche haute des deux balises.
- Le fermier ne casse plus les blocs du terrain pour atteindre une culture.
- Le panneau fermier est separe de l'arbre de competences dans l'UI.
- La neige fine est ignoree ou retiree quand elle couvre le passage, une culture ou un sol a labourer.
- La terre labouree est maintenant un support de marche valide pour atteindre les cultures interieures.
- Les sols tentes a la houe sont mis en pause temporaire, meme si le bloc refuse ou reperd son etat laboure.
- Les cibles fermier changees/interrompues sont marquees puis liberees pour mieux repartir avec plusieurs valets.

Limites connues :

- Test visible en jeu encore a faire sur plusieurs champs et plusieurs valets.

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
