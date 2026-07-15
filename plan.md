# Plan projet Valet

## Regle de travail

- Lire `plan.md` et `task.md` avant chaque action.
- Apres chaque modification de code ou de ressources impactant le jar, mettre a jour `task.md` et, si l'architecture change, `plan.md`.
- Pour un bug en jeu, lire le jar installe et `latest.log` avant de patcher.
- Ne pas push GitHub sans demande explicite.

## Source de verite

- `README.md` : etat public, version stable, guide de depart.
- `CHANGELOG.md` : changements humains par version.
- `JAR_REGISTRY.md` : tracabilite des jars, builds, hashes et validations.
- `docs/important-notes.md` : reprise projet, build, release, debug.
- `docs/releases/vX.Y.Z.md` : notes de release versionnees.

## Architecture globale

- `src/main/java/com/wawane/valet/ValetMod.java` : enregistrement principal du mod, blocs, items, menus et integrations.
- `ValetRole`, `ValetHome`, `state/` : role actif, poste, comportement et donnees persistantes du valet.
- `ai/ValetWorkGoal.java`, `ai/ValetStateMachine.java`, `ai/ValetWorkDriver.java` : boucle de travail, transitions et orchestration runtime.
- `ai/tasks/` : taches metier visibles en jeu.
- `ai/path/` et `ai/core/` : pathfinding, reservations de blocs/entities et reglages de travail.
- `ai/path/ValetSafeNavigation` : enveloppe de securite deterministe autour de `PathNavigation`; refuse fluides hors sortie d'eau, supports dangereux, chutes et pas trop hauts avant tout mouvement Valet.
- `order/` : ordres joueur et cibles de recolte, minage, bois, craft, ferme.
- `gui/` et `network/packets/` : ecrans client, payloads bornes et validation serveur; les blueprints y circulent sous forme de resumes compacts.
- La suppression d'un champ part de l'UI du fermier, passe par un payload valide cote serveur, retire la zone du `SavedData` de la dimension et annule les ordres qui la referencent.
- `progress/` : perks, arbres de competences et progression.
- `construction/`, `farm/`, `breeding/`, `cooking/`, `combat/`, `group/` : modules fonctionnels dedies.
- La logistique du fermier conserve une pile des items de plantation actifs; si un sol vide n'a aucun item compatible dans l'inventaire, le runtime cherche un coffre proche, en retire une pile puis reprend la plantation.
- `client/` et `mixin/` : rendu, previews, animations et integrations Minecraft/Fabric.
- `client/ValetWorldMapScreen`, `client/ValetGroupsScreen`, `group/ValetGroupRuntime`, `group/ValetGroupExcavation` et `group/ValetGroupTravelTickets` : onglets carte/groupes, ordres longue distance, excavation 3D locale et tickets temporaires de mission.
- La carte tactique garde un cache client borne des couleurs de terrain et rend une texture dynamique unique; le glisser ne doit jamais relire le monde ni soumettre une commande par cellule a chaque evenement souris.
- `quest/`, `client/ValetQuestScreen` et les payloads de quete : apparition du maire, quetes joueur persistantes et livraisons validees par le serveur.
- L'UI de quetes partagee par `J` et le maire affiche des textes ARGB opaques, l'icone de chaque objet et un bilan conserve apres livraison.
- `quest/ValetMayorState` conserve l'UUID du maire unique de chaque dimension; le gestionnaire supprime les doublons charges, equipe le maire et ouvre les quetes par interaction directe.
- La gestion des groupes reste centralisee dans les onglets de la carte, ouverte directement avec `K`; aucun bloc ou item de groupe n'est enregistre.
- A partir de 0.4.0, `state/ValetIdentity` porte l'identite persistante du valet; les postes ne doivent plus pouvoir retirer cette identite.
- L'`Insigne de valet` est l'unique point d'entree joueur; le rôle est choisi dans l'interface du valet.
- Les postes restent des points de travail et ne créent, ne restaurent ni ne suppriment l'identité Valet.
- `src/main/resources/assets/valet/` : blockstates, models, textures et traductions.
- `src/main/resources/data/valet/recipe/` et `src/main/resources/data/valet/loot_table/` : recettes et loot tables aux chemins de registre Minecraft 26.2.

## Regle d'ajout d'un metier

- Ajouter le role dans `ValetRole`.
- Enregistrer poste/blocs/items dans `ValetMod`.
- Brancher attribution, etat et routine dans `ValetStateMachine` / `ValetWorkGoal`.
- Ajouter ou ajuster la tache runtime dans `ai/tasks/` ou un module dedie.
- Ajouter UI, payloads, traductions, assets, recettes, loot tables et tags si necessaire.
- Preferer un poste visible et un stockage dedie quand le gameplay le justifie.
- Tester par comportement visible : mouvement, ordre actif, bloc pose/utilise, UI, message joueur.

## Direction IA du deplacement

- Le mouvement physique est execute par le pathfinding vanilla; aucun deplacement normal ne doit utiliser `teleportTo`.
- Les regles de securite restent deterministes : support solide, volume libre, fluides, danger, denivele, timeout et repli.
- Le fermier reprend le flux vanilla en deux niveaux : Valet choisit une case sure adjacente pour l'approche longue, puis l'action locale s'effectue a portee comme `HarvestFarmland`; `MoveToTargetSink` et `InteractWithDoor` possedent le trajet continu.
- La cible `WALK_TARGET` agricole reste autoritaire pendant l'approche et est reaffirmee avec un delai borne si le cerveau l'efface; aucun mouvement bloc par bloc ni abandon sur 40 ticks devant une porte.
- Recolte, plantation et labour sont arbitres par proximite pour eviter qu'un type d'action affame les autres; une plantation manquante est demandee avant ce choix.
- La logistique de plantation cherche les coffres pres du valet, du poste et des quatre coins de la zone, ce qui couvre les stockages poses a cote des balises de champ.
- Un coffre vide ou inaccessible differe seulement la prochaine demande de plantation; il ne bloque jamais les recoltes mures ni le labour pendant ce delai.
- L'eau de surface peut etre traversee pour une mission de groupe : rive opposee ou point de nage local, noeuds d'eau uniquement et aucune excavation sous-marine.
- Pour l'eau et les pas adjacents d'une galerie deja validee, `MoveControl` et `JumpControl` vanilla executent le mouvement physique; `PathNavigation` reste reserve aux parcours terrestres ou il peut construire un vrai chemin.
- Une cible de nage valide reste stable jusqu'a ce qu'elle soit atteinte; apres 40 ticks sans progres, le meneur choisit un detour aquatique borne et les suiveurs conservent une distance anti-collision.
- Les missions de groupe sont `surface-first` : elles essaient plusieurs petits troncons vanilla et detours bornes avant toute excavation.
- La validation des supports est partagee entre navigation et excavation; chemins en terre, terres labourees et escaliers sont praticables partout.
- La regle de porte est partagee par `ValetSafeNavigation` : une porte en bois fermee peut etre ouverte, une porte metallique fermee reste un obstacle et n'est jamais actionnee par un valet.
- Une galerie ne peut pas passer de un a quatre blocs sous une surface sure deja praticable.
- Un futur modele peut classer des cibles ou detours locaux deja declares legaux, mais ne doit jamais contourner ces garde-fous.
- Les evenements `navigation_start`, `navigation_stuck`, `navigation_rejected` et leur resultat servent de base a une future telemetrie structuree `observation -> action -> resultat`.

## Build et release

- Ligne actuelle : Minecraft 26.2, Fabric Loader 0.19.3, Fabric API 0.154.2+26.2, Loom 1.17.14, JDK 25.
- Apres changement Java/resources qui modifie le jar : compiler et installer localement le jar.
- Verifier qu'un seul `valet-*.jar` est present dans le dossier mods.
- Pour une version : synchroniser `README.md`, `CHANGELOG.md`, `JAR_REGISTRY.md` et `docs/releases/vX.Y.Z.md`.
- Calculer le SHA-256 final apres le build final seulement.
