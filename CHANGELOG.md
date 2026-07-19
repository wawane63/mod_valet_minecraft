# Changelog

## 0.4.4 - Brain borne, valets sans poste et residence explicite

### Added

- Un bouton `Insigne lit` permet de demander gratuitement au valet un objet non craftable lie a son UUID.
- Le bouton `Inventaire` ouvre les huit vrais emplacements du valet dans une interface vanilla; le joueur peut y prendre ou deposer des objets.
- L'eleveur peut supprimer definitivement l'enclos selectionne avec `Suppr. enclos`, comme le fermier avec ses champs.
- Le maire porte un trident visible pour etre identifiable immediatement.
- Un clic droit sur le maire ouvre directement l'interface des quetes.
- L'interface affiche l'icone de l'objet demande, la quantite presente dans l'inventaire et le bilan conserve apres livraison.
- Un kit visuel pilote donne a l'Artisan une icone de role 16x16 et un skin Java wide 64x64.
- Le mod possede maintenant une icone 128x128 dans les listes Fabric.

### Changed

- Le Brain du villageois est l'unique ordonnanceur des valets avec les activites `WORK`, `REST` et `IDLE`; l'ancien driver de tick parallele est supprime.
- Tous les trajets metier utilisent une `WALK_TARGET`, `MoveToTargetSink`, `InteractWithDoor` et un chemin vanilla complet borne par `ValetWorkZone`.
- L'insigne cree l'identite sans bloc; pour le fermier et l'eleveur, la zone choisie dans l'UI devient l'ancre mobile et remplace toute assignation par poste ou `JOB_SITE`.
- Le lit `HOME` est choisi avec l'Insigne de lit fourni par le valet, dans un rayon residentiel de 32 blocs autour de l'ancre, reserve uniquement si son POI et son trajet local sont valides, et reste le seul ticket POI possede par le valet.
- L'Insigne de lit cible un valet exact, n'est consomme qu'apres une assignation reussie et evite de fabriquer un second Insigne de valet.
- Les sept anciens postes sont entierement retires des registres, ressources, modeles, traductions, recettes, loot tables et POI.
- Le mode libre conserve une promenade locale; fuite, combat, eau et coffres restent dans le territoire, sauf mission de groupe explicitement ordonnee.
- Les missions de groupe replanifient ou attendent lorsqu'aucun trajet de surface n'existe; elles ne creusent plus pour se deplacer.
- Le rappel de groupe derive maintenant de la commande persistante, rejoint l'ancre propre a chaque valet, puis cede la main au trajet local 3D.
- Les balises de champ et d'enclos ne servent qu'a saisir les coordonnees; la zone persiste dans le monde apres leur retrait.
- L'eleveur retire jusqu'a 16 aliments par trajet de coffre au lieu de refaire une detection et un aller-retour apres chaque animal.
- L'Insigne de valet utilise une icone originale en pixel art au lieu du modele d'etiquette vanilla.
- Le rendu Valet utilise le skin Artisan comme apparence par defaut au lieu de Steve.
- Le skin Artisan reprend maintenant fidelement le modele SuperMaker valide : frange rousse asymetrique, veste vert sapin, manches creme, tablier long a deux poches, badge, harnais croise, pantalon brun et bottes charbon. Les secondes couches Java donnent du relief aux cheveux, au tablier, aux poignets et aux sangles; le PNG final reste regenerable localement sans filigrane.

### Fixed

- Le fermier ne recherche plus un HOME global et ne peut plus viser le lit du joueur ni creuser sous une maison pour l'atteindre.
- Aucun obstacle de trajet n'est casse : ancien A* bloc par bloc, minage de chemin et excavation de groupe sont retires.
- L'eleveur memorise les animaux et coffres inaccessibles, ouvre uniquement un portillon coherent avec le chemin bloque, laisse vanilla recalculer puis referme apres approche et passage.
- Le nourrissage automatique n'est plus affame par l'idle : un enclos precis est obligatoire, le valet atteint chaque adulte pret, le nourrit puis laisse le `BreedGoal` vanilla former le couple.
- L'UI de l'eleveur affiche tous les enclos sauvegardes sans filtrage par poste; animaux et stockages sont limites a l'enclos choisi et sa marge logistique.
- Les coffres poses juste a l'exterieur des balises d'enclos sont detectes dans une marge de deux blocs; le log distingue maintenant `no_feed_source` de `no_target`.
- `no_feed_source` indique maintenant le nombre de coffres detectes, exclus temporairement et de types d'aliments recherches.
- La suppression d'un enclos annule tous les ordres d'elevage encore lies a cette zone persistante.
- Le plafond compte les naissances deja en attente et est revalide juste avant nourrissage ou abattage; aucun animal amoureux n'est abattu.
- Les valets actuels gardent leur UUID et leurs donnees; les memoires `JOB_SITE` et leur ticket Valet sont nettoyes sans recreer de poste.
- Une commande de groupe `RECALL` rend la main au metier des que son relais local est atteint; elle ne peut plus immobiliser indefiniment le fermier a l'ancre.
- Les portillons se referment des que le valet a change de cote et libere leur volume, avec un timeout de securite borne.
- L'Insigne de lit accepte tout lit libre et atteignable dans le rayon residentiel de l'ancre, sans elargir la recherche des cibles ou coffres de travail.
- Le centre non praticable d'un champ ou d'un enclos est remplace par la case sure la plus proche; le retour HOME peut viser directement cette case et ne boucle plus sur `logistics no_home_path`.
- La mort ou la suppression d'un valet libere uniquement son ticket `HOME` confirme avant d'effacer ses marqueurs.
- Suivi, garde, attaque et defense ne recalculent plus un chemin chaque tick; leur navigation s'arrete proprement a portee et le backoff de mission est respecte apres un echec de surface.
- Un valet au-dessus ou au-dessous de son ancre ne considere plus le rappel termine sur la seule distance horizontale et conserve son trajet local pendant un detour d'escalier.
- Construction, craft, logistique et combat ne peuvent plus lire ou selectionner un coffre de maison hors territoire.
- Le menu des quetes ne transmet plus de conteneur nul a Minecraft 26.2 lors du comptage ou du retrait des livraisons.
- Les titres, descriptions, quantites et recompenses des quetes ne sont plus transparents en Minecraft 26.2.
- Les valets n'ouvrent plus les portes en fer ou en cuivre; seules les portes en bois fermees peuvent etre actionnees, comme pour les villageois.
- L'eleveur traverse physiquement un portillon ouvert sans demander un chemin vanilla vers son bloc; le portillon reste ouvert jusqu'a son passage puis se referme.
- Un UUID de maire est conserve par dimension; les doublons deja charges sont automatiquement supprimes et le maire reste pres de sa cloche.

## 0.4.3 - Navigation de surface et maire unique interactif

### Added

- Le maire porte un trident visible et ouvre directement les quetes au clic droit.
- L'interface des quetes affiche les objets demandes, la progression et le bilan des livraisons.

### Changed

- Les trajets essaient plusieurs chemins de surface avant toute autre strategie.
- Les portes en bois suivent le comportement villageois; les portes metalliques fermees ne sont pas actionnees.

### Fixed

- Le menu des quetes ne transmet plus de conteneur nul a Minecraft 26.2.
- Un seul maire est conserve par dimension et les doublons charges sont retires.
- Les textes de quete utilisent des couleurs opaques et restent lisibles.
- Les trajets refusent les tunnels peu profonds sous une surface praticable.

## 0.4.2 - Navigation vanilla, fermier et ameliorations generales

### Added

- L'UI du fermier permet de supprimer definitivement le champ selectionne avec le bouton `Suppr. champ`.

### Changed

- Les trajets de travail locaux utilisent maintenant `PathNavigation` au lieu de teleporter le valet bloc par bloc.
- Le creuseur des missions longue distance marche reellement dans les galeries apres les avoir ouvertes.
- La sortie d'eau recherche une berge atteignable, nage et saute vers elle sans teleportation.
- Les missions de groupe detectent l'eau dans l'axe du repere, nagent vers la rive opposee ou par points intermediaires et maintiennent les suiveurs en mouvement dans l'eau.
- L'approche d'une rive reste confiee a `PathNavigation`, puis `MoveControl` et `JumpControl` vanilla prennent le relais au bord et dans l'eau pour eviter le refus des cibles aquatiques.
- La cible de nage reste stable jusqu'a son atteinte; apres 40 ticks sans progres, le meneur tente automatiquement un detour aquatique alterne.
- Les suiveurs gardent une distance minimale dans l'eau au lieu de se concentrer exactement sur le meneur et de le bloquer par collision.
- Les pas adjacents d'une galerie securisee sont parcourus physiquement sans relancer un calcul A* complet a chaque bloc.

### Safety

- Validation de chaque chemin local : support solide, espace libre, fluides, denivele maximal d'un bloc et nombre de noeuds borne.
- Refus des surfaces dangereuses : feu, cactus, magma, feux de camp, buissons epineux, rose du Wither, neige poudreuse et stalagmites.
- Abandon et replanification apres immobilite, timeout ou interruption par fuite, combat et ordre de groupe.
- Les troncons longue distance et accostages sont verifies avant execution; le tunnelage continue de refuser fluides, blocs artificiels et colonnes instables.
- Une traversee d'eau detectee ne peut plus basculer en excavation sous-marine si le chemin de nage est refuse.
- Le meneur ne valide plus l'arrivee tant qu'il est encore dans l'eau; les suiveurs nagent directement vers lui au lieu d'attendre un chemin terrestre impossible.

### Fixed

- Le fermier conserve jusqu'a une pile de chaque item de plantation actif au lieu de deposer toutes les pommes de terre, graines, carottes, betteraves ou verrues du Nether dans le coffre.
- Quand une terre compatible est vide et son inventaire ne contient rien a planter, le fermier peut maintenant prendre une pile compatible dans un coffre proche puis revenir planter.
- La suppression d'un champ annule les ordres des valets qui le referencent; une ancienne reference chargee plus tard est aussi nettoyee automatiquement.
- Le fermier peut de nouveau quitter une terre labouree ou un chemin partiel pour aller labourer; le premier noeud vanilla n'est plus refuse quand sa hauteur logique differe du bloc occupe par l'entite.
- Une cible de ferme dont la navigation est reellement refusee est temporairement ignoree au lieu de provoquer une boucle de mouvements de tete.
- Le fermier vise maintenant sa destination avec `WALK_TARGET`, comme `HarvestFarmland`; `MoveToTargetSink` produit un trajet continu et `InteractWithDoor` gere les portes en bois naturellement.
- L'approche agricole longue vise maintenant une case sure adjacente et maintient sa `WALK_TARGET` si le cerveau la perd, au lieu de demander a `HarvestFarmland` de rejoindre directement une culture lointaine.
- Recolte, plantation et labour sont choisis par proximite; les cultures mures ne peuvent plus affamer indefiniment le labour ou la plantation voisine.
- Un sol vide sans item compatible declenche immediatement une demande logistique, meme si d'autres cultures sont mures.
- La recherche de plantations en coffre inclut les quatre coins du champ afin de couvrir les coffres places pres des balises.
- Un coffre sans plantation impose maintenant un delai de 10 secondes avant une nouvelle demande; les recoltes mures et le labour continuent pendant ce temps.
- La replantation est active par defaut sur un nouvel ordre de ferme; une option explicitement decochee reste respectee.
- Un sol de culture vide sous les pieds du fermier est maintenant plante directement; le correctif couvre ble, carottes, pommes de terre, betteraves et verrues du Nether.
- Les verrues du Nether disponibles dans l'inventaire peuvent aussi etre plantees automatiquement sur du sable des ames vide.
- Le fermier peut sortir d'une maison depuis un lit ou un autre support partiel sans viser la porte comme destination intermediaire.

### Performance

- La carte tactique met en cache les couleurs de terrain deja lues, avec une limite memoire fixe.
- Le glisser limite les reconstructions du terrain a 20 Hz au lieu de recalculer a chaque evenement souris.
- Le terrain est envoye au rendu sous forme d'une texture dynamique unique au lieu d'une commande par cellule.

## 0.4.1 - Maire et quetes

### Added

- Apparition d'un maire pres de la cloche lorsqu'un village possede au moins un chat et un golem.
- Trois quetes de livraison persistantes : pain, torches et fer, avec recompenses en emeraudes.
- Menu des quetes ouvert par la touche `J`.

### Changed

- La carte tactique s'ouvre directement avec `K`; le bouton du menu Echap est retire.

## 0.4.0 - Tag Valet

### Added

- Ajout de l'`Insigne de valet`, utilisable sur un villageois adulte.
- Recette sans forme de l'insigne : une etiquette et une emeraude, avec deverrouillage dans le livre de recettes.
- Ajout d'une identite Valet persistante en NBT, independante de la profession vanilla et du poste.
- Migration automatique des valets 0.3.9 bases sur la profession vers cette nouvelle identite.

### Changed

- Un valet marque reste reconnu comme valet sans poste et pendant les missions longue distance.
- Le métier se choisit directement dans l'interface du valet et ne dépend plus du type de poste.
- Les postes ne créent plus automatiquement de valets; seule l'Insigne attribue cette identité.

### Cleaned

- Suppression des chemins de perte/restauration de profession qui effaçaient les données lorsque le poste devenait inaccessible.

### Fixed

- Les missions vers un repère passent en petits tronçons lorsque le dénivelé est fort ou que le meneur reste immobile.
- Les valets réutilisent le planificateur 3D des artisans : galerie garantie à trois blocs de haut et escaliers limités à un bloc de montée ou descente par pas.
- Les chutes d'eau sont refusées comme passage d'excavation et recherchées par angles de contournement locaux.
- Le tunnelage de mission refuse les constructions, conteneurs, fluides et blocs incassables.
- Un seul valet par petit groupe proche ouvre la galerie; les autres suivent ce passage au lieu de creuser en parallèle.
- L'ordre `Aller au repère` prend désormais la priorité sur les retours métier, la fuite et le combat, notamment dans l'eau.
- Le gravier et le sable isolés peuvent être retirés, mais une colonne susceptible de s'effondrer bloque le tunnelage et force un autre passage.
- Le groupe conserve un meneur stable pendant toute la mission; les suiveurs visent uniquement ce meneur et continuent de le rattraper au-delà de 5 blocs sans bloquer sa progression.
- En traversée d'eau, les valets recherchent une berge solide et sautent pour accoster.
- Tous les membres d'un groupe peuvent interrompre brièvement la mission pour se défendre à l'épée en combat rapproché.
- Le creuseur synchronise désormais le regard, la tête et le corps avec chaque bloc cassé et chaque pas, au lieu de glisser de côté ou à reculons.
- Les pas d'excavation sont espacés, les chemins faisant un grand retour arrière sont refusés et la navigation des suiveurs n'est recalculée que deux fois par seconde.

## 0.3.9 - Gestion centralisee des groupes

### Changed

- Ajout des onglets `Carte` et `Groupes de valets` dans la carte ouverte depuis le menu Echap.
- Creation, suppression et affectation des valets centralisees dans l'onglet `Groupes de valets`.
- La carte conserve la selection du groupe, le repere, l'envoi en mission et le rappel.

### Removed

- Retrait provisoire du pupitre de groupe, de sa recette et de ses ressources.
- Retrait provisoire de la carte de groupe et de ses interactions.
- Retrait de la liaison des cornes de chevre aux groupes.
- Suppression de l'ancien menu de pupitre, de son payload et des classes de liaison d'items devenues inutiles.

## 0.3.8 - Audit et carte tactique

### Added

- Ajout d'un bouton `Carte des valets` dans le menu Echap.
- Ajout d'une carte tactique plein ecran inspiree des cartes de jeux en monde ouvert.
- Affichage topographique des chunks deja charges par le client, sans chargement force de chunks.
- Zoom centre sur le curseur et deplacement de la carte par glisser gauche.
- Affichage du joueur et des valets actuellement visibles.
- Ajout d'un repere personnel par clic droit, avec coordonnees et bouton d'effacement.
- Ajout d'une legende, des coordonnees du joueur et du curseur, et d'un bouton de recentrage.
- Creation, suppression et selection des groupes directement depuis la carte tactique.
- Un clic sur un marqueur de valet l'ajoute ou le retire du groupe selectionne; les cartes de groupe et cornes existantes restent compatibles.
- Ajout de l'ordre universel `Aller au repere`, utilisable par tous les metiers.
- Les valets parcourent reellement la distance avec le pathfinding Minecraft, recalcule par troncons locaux de 24 blocs au lieu de calculer un trajet global.
- En cas d'echec local, la direction est decalee progressivement pour chercher un contournement sans exploration globale couteuse.
- Des tickets temporaires non persistants suivent les groupes en mission afin que les entites continuent a se deplacer hors de la zone du joueur.
- Le chargement de mission est borne a 32 centres de chunks et est retire des que l'ordre prend fin.

### Limitations

- Le repere est conserve pendant la session de jeu et peut servir de destination au groupe selectionne.
- Les zones jamais recues par le client restent masquees.

### Fixed

- Restauration de la persistence complete des valets via un mixin 26.2 actif : poste, role, ordres, progression et reglages survivent de nouveau aux sauvegardes.
- Migration du mixin de protection des allies vers `AbstractArrow`, avec verification du tireur avant d'annuler les degats.
- Validation serveur renforcee pour tous les payloads : menu ouvert, valet, UUID, dimension, distance, bornes et tailles de listes.
- Les buffers reseau temporaires sont liberes et les etats envoyes au client utilisent des copies defensives bornees.
- Les taches craft, cuisine, ferme et construction restaurent les ingredients ou blocs si la sortie echoue.
- Les transferts respectent les limites et regles de chaque slot, y compris les doubles coffres dedupliques.
- Les UUID, dimensions, groupes, zones et blueprints NBT invalides ou surdimensionnes sont ignores sans exception ni chargement arbitraire de chunk.
- Les homes hors limites sont rejetes; les roles 0.3.7 sans cle dediee sont inferes sans effacer leur ordre, et deux valets ne peuvent plus conserver le meme poste restaure.
- L'ouverture artisan transmet des resumes de blueprint compacts au lieu de plans complets; l'interface de groupe borne les valets et exige le bon menu/pupitre.
- Le tri preserve les 9 slots de filtre des coffres geres par un poste d'intendant proche.
- Les portes et lits ne sont plus comptes deux fois dans les materiaux requis d'un blueprint.
- Les caches persistants sont purges quand un valet est reellement detruit, sans effacer les donnees lors d'un simple dechargement de chunk.
- Le coffre de cuisinier ne double plus son contenu a la destruction et met correctement a jour les comparateurs.
- Ajout de la loot table manquante du coffre de fleches infini.
- Migration des recettes, loot tables et tags de blocs vers les dossiers de registre et schemas JSON requis par Minecraft 26.2; les crafts et la conservation des donnees du blueprint sont de nouveau charges correctement.
- Les sorts de glace ne donnent plus d'XP quand les degats sont refuses.

### Optimized

- Reduction des allocations par tick dans l'orchestration, les recherches de menaces, les directions, les recettes et les inventaires.
- Mise en cache ou temporisation des scans de combat, fuite, support magique, fleches et cibles d'elevage.
- Expiration proactive des reservations de blocs et d'entites pour eviter l'accumulation en memoire.
- Les scans de construction reutilisent une position mutable, bornent volume/blocs et refusent proprement les chunks non charges.
- Les snapshots GUI ne scannent plus les fermes, enclos ou constructions sans rapport avec le role affiche.
- Les apercus de construction sont compresses en grilles de hauteur 16x16, ce qui borne fortement la taille des paquets d'ouverture.
- Consolidation des enregistrements d'onglet creatif et nettoyage des caches de rendu au dechargement.

### Cleaned

- Suppression de sept classes Java mortes, dont trois mixins vides ou obsoletes, et remplacement du mixin projectile renomme.
- Suppression de l'archive asset externe inutilisee, des fichiers `.DS_Store` et de huit entrees de traduction orphelines.
- Suppression de l'option `Nourrir` inactive, de ses donnees reseau/runtime et des surcharges de methodes sans appel.
- Suppression des imports, champs, methodes, TODO et chemins de code sans appel identifies par l'audit.
- Simplification des dependances Gradle, centralisation des versions du manifeste et ajout du checksum du wrapper.
- Mise a jour de maintenance vers Fabric API `0.154.2+26.2` et Loom `1.17.14`; Fabric Loader `0.19.3` reste la version courante.
- Ajout des regles d'edition/Git, de la licence MIT et d'une installation Gradle multiplateforme.
- Migration des API Minecraft/Fabric depreciees quand une alternative 26.2 stable existe.

## 0.3.7 - Intendant et transferts coffres

Bugs corriges / fonctionnalite :

- Ajout du `Poste d'intendant` (`valet:steward_workstation`) et du role `Intendant`.
- L'intendant transfere automatiquement des piles entre coffres, coffres pieges et barils proches du poste.
- Les 9 premiers slots d'un coffre/baril servent de filtres visibles et ne sont pas consommes par l'intendant.
- Les destinations filtrees sont prioritaires sur les simples regroupements de piles identiques.
- Priorite des filtres : slot le plus a gauche d'abord, puis coffre/baril le plus proche du poste.
- Les coffres/barils sans filtre servent d'entree : l'intendant y prend les items et les range dans les destinations filtrees compatibles.
- Les mauvais items places dans un coffre filtre peuvent etre evacues vers une destination compatible.
- Le transfert passe par l'inventaire visible du valet et conserve les filtres en place.
- Le chemin intendant ne mine pas de blocs autour des coffres, pour eviter de casser le stockage.

## 0.3.6 - Cuisinier et correctifs metiers

Bugs corriges / fonctionnalite :

- Ajout du `Poste de cuisinier` (`valet:cook_workstation`) et du role `Cuisinier`.
- Ajout du `Coffre de cuisinier` (`valet:cook_chest`), ouvrable et dote de 27 emplacements.
- Le cuisinier prend ses ingredients et depose ses repas uniquement dans ce coffre dedie.
- Le cuisinier recolte automatiquement le ble et les pommes de terre murs proches, puis replante si possible.
- Le cuisinier prend les ingredients crus dans son coffre dedie proche.
- Il prepare automatiquement pain, pommes de terre cuites, viandes et poissons cuits au poste.
- Les repas termines sont deposes dans le coffre de cuisinier proche.
- Le magicien ne relance plus soins, regeneration et resistance sur des allies qui n'en ont pas besoin.
- Les buffs de resistance du magicien sont reserves aux allies blesses ou au combat.
- Le craft de pioche reconnait les buches et planches de tous les bois via les tags items et blocs.
- Le fermier peut retirer une couche ou un bloc de neige avant de labourer.
- Le fermier plante automatiquement une culture choisie sur une terre labouree vide, y compris dans un champ neuf.
- Les logs de test ont ete analyses : aucun echec de construction explicite n'a ete enregistre; le deplacement reste a surveiller.
- Le combattant retombe bien sur l'epee sans fleches; le cas intermittent reste a preciser par une nouvelle signature de log.

## 0.3.5 - Blueprints ameliores

Bugs corriges / fonctionnalite :

- Rotation du chantier selon la direction du joueur au moment de poser le blueprint.
- Pose accroupie du blueprint pour construire une version miroir.
- Apercu monde mis a jour avec la meme rotation et le meme miroir que le chantier reel.
- Le valet verifie les materiaux requis avant de partir construire.
- Le message de blocage liste les premiers materiaux manquants avec les quantites.
- Le mode miroir reste sauvegarde dans le bloc blueprint et dans son drop.

## 0.3.4 - Eleveur

Bugs corriges / fonctionnalite :

- Ajout du `Poste d'eleveur` (`valet:poste_eleveur`).
- Ajout de la `Balise d'enclos` pour declarer des zones poules, vaches, moutons et cochons.
- L'interface de l'eleveur permet de choisir un enclos ou tous les enclos.
- Options UI : reproduire, tondre, ramasser oeufs, traire, abattre surplus et limite max animaux.
- Ajout de `BreedingRuntimeTask` pour gerer l'elevage.
- Ajout de reservations d'animaux pour eviter que deux valets ciblent le meme animal.
- L'eleveur prend graines, ble, carottes, seaux et cisailles dans les coffres/barils proches.
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

## 0.3.3 - Creation de groupe

Bugs corriges / fonctionnalite :

- Ajout du `Pupitre de groupe`, base sur le bloc de lutrin.
- Ajout d'une interface de creation de groupes : groupes, valets proches, ajout/retrait par clic.
- Ajout d'une `Carte de groupe` liee au groupe depuis l'interface.
- Une corne de chevre tenue en main peut etre liee a un groupe depuis le pupitre.
- Commandes de groupe : suivre, garde proche, garde large, attaque de zone et rappel.
- Clic droit avec carte/corne liee : le groupe suit le joueur.
- Accroupi + clic droit avec carte/corne liee : cycle suivre -> garde proche -> garde large -> rappel.
- Clic sur un monstre avec carte/corne liee : attaque cible.
- Clic sur un bloc avec carte/corne liee : attaque de zone autour du bloc.
- Les combattants et magiciens utilisent les ordres de groupe comme priorite de ciblage, sans casser leurs comportements de combat existants.
- Les valets non combattants peuvent suivre et etre rappeles, mais ne recoivent pas de logique de combat forcee.
- Correction apres test log : les commandes identiques ne relancent plus le groupe en boucle et les ordres attaque/garde ne deplacent plus les non-combattants vers le combat.
- Correction apres test log : ajout de la texture zombie-valet manquante pour eviter le warning de ressource.
- Correction : les artisans et fermiers fuient les monstres proches au lieu de continuer leur travail.
- Correction : un valet en groupe ne bascule plus rapidement entre epee et pioche quand il perd/retrouve une cible.

## 0.3.2 - Tri coffre + magicien

Bugs corriges / fonctionnalite :

- Ajout d'un bouton `Tri` dans les coffres/barils.
- Le tri est valide cote serveur sur le conteneur ouvert.
- Les piles identiques sont fusionnees puis rangees par identifiant d'item.
- Ajout du role `Magicien` et du `Poste de magicien`.
- Le magicien defend localement avec un sort de glace : projectile boule de neige, degats et ralentissement, sans incendie.
- Ajout d'un arbre magie en 3 branches : `Destruction`, `Soin`, `Alteration`.
- Le perk `Gel` est acquis automatiquement par les magiciens et ne consomme pas de point.
- Branche `Destruction` : `Gel`, `Crocs magiques`, `Fracas`.
- Branche `Soin` : `Soin`, `Aura de soin`.
- Branche `Alteration` : `Rempart`, `Affaiblir`.
- Le perk `Crocs magiques` lance des crocs apres 3 sorts de glace.
- Correction : un poste deja utilise ne laisse plus une reservation fantome faire alterner le valet entre oisif et magicien.
- Correction : un magicien qui perd son metier retire sa fiole et son glow.
- Correction : le magicien ne tient plus de fiole en main et garde les bras normaux hors combat.
- Correction : le compteur des crocs ne repart plus a zero quand la cible change, et la magie porte maintenant a 12 blocs.
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

## 0.3.1 - Crafts bois

Bugs corriges / fonctionnalite :

- Le `Poste d'artisan` utilise maintenant une pioche en bois et deux haches en bois.
- Le `Poste de combattant` utilise maintenant deux epees en bois au lieu de deux epees en fer.
- Le `Poste de combattant` utilise maintenant deux buches au lieu de deux lingots de fer.
- Le `Poste de fermier` utilise maintenant une houe en bois au lieu d'une houe en fer.
- La page GitHub des crafts est mise a jour avec les nouvelles recettes.

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
- `src/main/resources/data/valet/loot_table/blocks/*`, `ConstructionBlueprintBlock.java`: ajoute les loot tables des blocs custom et preserve le NBT des blueprints via loot table; risque residuel faible, depend de `copy_nbt` vanilla.
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
- `ValetWorkGoal.java`: annule la navigation vanilla step-by-step qui provoquait des `path step_timeout` et restaure le deplacement metier fiable; risque residuel faible pour craft/farm/build, animation de marche a reprendre proprement plus tard.
- `ValetBehavior.java`, `ValetMod.java`, `ValetWorkGoal.java`, `ValetOrdersScreen.java`: ajoute les options de comportement libre et de nuit, conserve `HOME` pour le sommeil, efface seulement les memoires sociales/marche quand le mod controle le valet, et ajoute le rappel 5 secondes par clic droit sur le poste; risque residuel moyen, a verifier en jeu sur sommeil, reproduction et rappel poste.
- `ai/tasks/LogisticsRuntimeTask.java`, `ValetWorkGoal.java`: extrait retour coffre, depot, retour poste et idle dans une task dediee; risque residuel moyen tant que les tests en jeu depot/retour poste ne sont pas faits.
