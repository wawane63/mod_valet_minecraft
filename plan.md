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
- `ValetRole`, `ValetAnchor`, `state/` : role actif, ancre persistante sans bloc, comportement et donnees persistantes du valet.
- `ai/ValetBrain.java`, `ai/ValetWorkGoal.java` et `ai/ValetStateMachine.java` : le Brain du villageois ordonnance les activites WORK/REST/IDLE; le runtime metier ne possede plus une boucle de tick parallele.
- `ai/ValetWorkZone.java` et `ai/ValetResidence.java` : territoire borne derive de l'ancre et du champ/enclos choisi, avec lit HOME reserve explicitement dans ce territoire.
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
- `client/ValetWorldMapScreen`, `client/ValetGroupsScreen`, `group/ValetGroupRuntime` et `group/ValetGroupTravelTickets` : onglets carte/groupes, ordres longue distance par navigation de surface et tickets temporaires de mission.
- La carte tactique garde un cache client borne des couleurs de terrain et rend une texture dynamique unique; le glisser ne doit jamais relire le monde ni soumettre une commande par cellule a chaque evenement souris.
- `quest/`, `client/ValetQuestScreen` et les payloads de quete : apparition du maire, quetes joueur persistantes et livraisons validees par le serveur.
- L'UI de quetes partagee par `J` et le maire affiche des textes ARGB opaques, l'icone de chaque objet et un bilan conserve apres livraison.
- `quest/ValetMayorState` conserve l'UUID du maire unique de chaque dimension; le gestionnaire supprime les doublons charges, equipe le maire et ouvre les quetes par interaction directe.
- La gestion des groupes reste centralisee dans les onglets de la carte, ouverte directement avec `K`; aucun bloc ou item de groupe n'est enregistre.
- A partir de 0.4.0, `state/ValetIdentity` porte l'identite persistante du valet; les postes ne doivent plus pouvoir retirer cette identite.
- L'`Insigne de valet` est l'unique point d'entree joueur; le rôle est choisi dans l'interface du valet.
- Les postes, leurs blocs, items, POI, recettes et assets sont totalement retires. La profession legacy reste seulement comme identifiant de migration, sans predicate POI.
- L'insigne cree l'identite. Pour le fermier et l'eleveur, l'ancre devient automatiquement le centre de la zone selectionnee et se deplace avec chaque nouvelle selection.
- `src/main/resources/assets/valet/` : blockstates, models, textures et traductions.
- `scripts/generate_visual_assets.py` reconstruit les PNG pixel-perfect depuis les concepts IA; le skin reporte fidelement les vues du modele SuperMaker valide sur les UV Java et leurs secondes couches. Aucun filigrane, damier simule ou export brut d'un service externe n'entre dans le jar.
- `src/main/resources/data/valet/recipe/` et `src/main/resources/data/valet/loot_table/` : recettes et loot tables aux chemins de registre Minecraft 26.2.

## Regle d'ajout d'un metier

- Ajouter le role dans `ValetRole`.
- Brancher le role a l'ancre et a ses cibles visibles dans l'UI; ne pas ajouter de dependance `JOB_SITE`.
- Brancher attribution, etat et routine dans `ValetStateMachine` / `ValetWorkGoal`.
- Ajouter ou ajuster la tache runtime dans `ai/tasks/` ou un module dedie.
- Ajouter UI, payloads, traductions, assets, recettes, loot tables et tags si necessaire.
- Preferer un stockage ou une zone visible seulement quand le gameplay le justifie; l'identite et le role restent portes par l'insigne et l'UI.
- Tester par comportement visible : mouvement, ordre actif, bloc pose/utilise, UI, message joueur.

## Direction IA du deplacement

- Pour un valet, le Brain est l'unique autorite : activites separees WORK/REST/IDLE, `WALK_TARGET`, `MoveToTargetSink` borne et `InteractWithDoor` vanilla.
- L'activite Valet est reaffirmee apres `VillagerAi.updateActivity`; une menace force WORK meme la nuit pour laisser fuite ou combat interrompre REST.
- Le mode libre conserve une promenade aleatoire bornee au territoire; il ne reactive jamais la recherche globale de POI vanilla.
- Aucun `AcquirePoi(HOME)` global n'est conserve : le joueur assigne explicitement un lit local avec l'insigne, puis Valet reserve uniquement ce POI apres validation de la zone et du trajet vanilla.
- Seul le ticket `HOME` explicitement pris est marque dans le NBT et peut etre libere par le valet; aucun ticket `JOB_SITE` Valet n'est conserve.
- L'UI permet de demander au valet un `Insigne de lit` non craftable lie a son UUID. Cet objet assigne uniquement ce valet au lit clique, puis est consomme apres validation du POI et du trajet borne.
- Un trajet autonome doit rester dans `ValetWorkZone`; sans ancre ou cible valide, il est abandonne sans errance, minage ni bris de bloc.
- Pour un fermier ou un eleveur avec zone selectionnee, cette zone remplace entierement l'ancienne ancre et devient son territoire; changer de selection deplace l'ancre.
- L'ancre logique d'une zone est normalisee vers la case praticable la plus proche; le retour local peut viser directement cette case au lieu d'exiger une case adjacente.
- Le HOME explicite dispose d'un rayon residentiel borne de 32 blocs autour de l'ancre mobile, utilise uniquement pendant REST et jamais pour elargir les cibles ou coffres de travail.
- Fuite, combat autonome, sortie d'eau et recherche de coffre restent aussi dans `ValetWorkZone`; seul un ordre de groupe explicite du joueur peut autoriser une mission hors territoire.
- Le moteur A* bloc par bloc Valet et l'excavation de deplacement sont obsoletes; seul le pathfinding vanilla calcule les trajets complets.
- Le mouvement physique est execute par le pathfinding vanilla; aucun deplacement normal ne doit utiliser `teleportTo`.
- Les regles de securite restent deterministes : support solide, volume libre, fluides, danger, denivele, timeout et repli.
- Le fermier reprend le flux vanilla en deux niveaux : Valet choisit une case sure adjacente pour l'approche longue, puis l'action locale s'effectue a portee comme `HarvestFarmland`; `MoveToTargetSink` et `InteractWithDoor` possedent le trajet continu.
- La cible `WALK_TARGET` agricole reste autoritaire pendant l'approche et est reaffirmee avec un delai borne si le cerveau l'efface; aucun mouvement bloc par bloc ni abandon sur 40 ticks devant une porte.
- Recolte, plantation et labour sont arbitres par proximite pour eviter qu'un type d'action affame les autres; une plantation manquante est demandee avant ce choix.
- La logistique de plantation cherche les coffres pres du valet, de l'ancre et des quatre coins sauvegardes de la zone.
- Un coffre vide ou inaccessible differe seulement la prochaine demande de plantation; il ne bloque jamais les recoltes mures ni le labour pendant ce delai.
- L'eleveur exige un enclos explicitement assigne, rejoint et nourrit individuellement chaque adulte pret, puis laisse le `BreedGoal` vanilla des animaux les rapprocher et accomplir l'accouplement.
- Tous les enclos sauvegardes sont visibles dans l'UI de l'eleveur; l'enclos choisi borne le travail sans dependance a un poste.
- Champs et enclos possedent la meme suppression persistante depuis l'UI; les ordres lies a une zone supprimee sont annules.
- L'inventaire du valet reste le conteneur vanilla du villageois : l'UI de gestion ouvre directement ses huit slots via un adaptateur 9x1, sans copie intermediaire.
- L'eleveur constitue une reserve bornee de 16 aliments par retrait pour ne pas rescanner et retraverser l'enclos apres chaque animal.
- Les balises servent uniquement a enregistrer les deux coins dans le `SavedData`; elles peuvent etre cassees des que la confirmation de sauvegarde est affichee.
- L'eau de surface peut etre traversee pour une mission de groupe : rive opposee ou point de nage local, noeuds d'eau uniquement et aucune excavation sous-marine.
- Pour l'eau et les pas adjacents d'une galerie deja validee, `MoveControl` et `JumpControl` vanilla executent le mouvement physique; `PathNavigation` reste reserve aux parcours terrestres ou il peut construire un vrai chemin.
- Une cible de nage valide reste stable jusqu'a ce qu'elle soit atteinte; apres 40 ticks sans progres, le meneur choisit un detour aquatique borne et les suiveurs conservent une distance anti-collision.
- Les missions de groupe essaient plusieurs troncons vanilla et detours bornes, puis attendent/replanifient si aucun chemin n'existe; elles ne creusent jamais pour se deplacer.
- Un rappel de groupe utilise le trajet de surface jusqu'au voisinage horizontal de l'ancre, puis cede la main au trajet local en trois dimensions; les tickets restent actifs jusqu'a l'arrivee reelle.
- La validation des supports est partagee par la navigation; chemins en terre, terres labourees et escaliers sont praticables partout.
- La regle de porte est partagee par `ValetSafeNavigation` : une porte en bois fermee peut etre ouverte, une porte metallique fermee reste un obstacle et n'est jamais actionnee par un valet.
- Les portes en bois suivent `InteractWithDoor`; les portillons ne recoivent aucun traitement de creusage ou de mouvement force.
- Seul un portillon oriente entre le valet et la cible, proche du dernier noeud d'un chemin vanilla partiel, peut etre ouvert; l'ouverture est annulee sans chemin, puis refermee apres approche et passage ou timeout.
- Un futur modele peut classer des cibles ou detours locaux deja declares legaux, mais ne doit jamais contourner ces garde-fous.
- Les evenements `navigation_start`, `navigation_stuck`, `navigation_rejected` et leur resultat servent de base a une future telemetrie structuree `observation -> action -> resultat`.

## Build et release

- Ligne actuelle : Minecraft 26.2, Fabric Loader 0.19.3, Fabric API 0.154.2+26.2, Loom 1.17.14, JDK 25.
- Apres changement Java/resources qui modifie le jar : compiler et installer localement le jar.
- `check` refuse aussi le retour de l'ancien driver parallele, de l'A* Valet, d'un `NodeEvaluator` custom ou d'un `destroyBlock` dans les couches de deplacement.
- Verifier qu'un seul `valet-*.jar` est present dans le dossier mods.
- Pour une version : synchroniser `README.md`, `CHANGELOG.md`, `JAR_REGISTRY.md` et `docs/releases/vX.Y.Z.md`.
- Calculer le SHA-256 final apres le build final seulement.
