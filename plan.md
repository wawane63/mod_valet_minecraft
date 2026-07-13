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
- `order/` : ordres joueur et cibles de recolte, minage, bois, craft, ferme.
- `gui/` et `network/packets/` : ecrans client, payloads bornes et validation serveur; les blueprints y circulent sous forme de resumes compacts.
- `progress/` : perks, arbres de competences et progression.
- `construction/`, `farm/`, `breeding/`, `cooking/`, `combat/`, `group/` : modules fonctionnels dedies.
- `client/` et `mixin/` : rendu, previews, animations et integrations Minecraft/Fabric.
- `client/ValetWorldMapScreen`, `client/ValetGroupsScreen`, `group/ValetGroupRuntime`, `group/ValetGroupExcavation` et `group/ValetGroupTravelTickets` : onglets carte/groupes, ordres longue distance, excavation 3D locale et tickets temporaires de mission.
- La gestion des groupes est centralisee dans le menu Echap; aucun bloc ou item de groupe n'est enregistre en 0.3.9.
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

## Build et release

- Ligne actuelle : Minecraft 26.2, Fabric Loader 0.19.3, Fabric API 0.154.2+26.2, Loom 1.17.14, JDK 25.
- Apres changement Java/resources qui modifie le jar : compiler et installer localement le jar.
- Verifier qu'un seul `valet-*.jar` est present dans le dossier mods.
- Pour une version : synchroniser `README.md`, `CHANGELOG.md`, `JAR_REGISTRY.md` et `docs/releases/vX.Y.Z.md`.
- Calculer le SHA-256 final apres le build final seulement.
