# Notes importantes pour reprendre le projet

Ce fichier sert a donner le contexte utile a Codex quand le repo est clone sur un autre ordinateur.

## Etat courant

- Repo GitHub : `https://github.com/wawane63/mod_valet_minecraft.git`
- Branche durable : `main`
- Version stable actuelle : voir `README.md` et `JAR_REGISTRY.md`
- Version actuelle : `0.4.4` (Brain borne, valets sans poste et residence explicite)
- Derniere release publiee : `v0.4.4`
- Le jar publie est sur la page GitHub Releases.

## Reprise sur un autre ordinateur

```powershell
git clone https://github.com/wawane63/mod_valet_minecraft.git
cd mod_valet_minecraft
git pull --tags
```

Routine avant de travailler :

```powershell
git pull --tags
git status
```

Routine apres un changement valide, uniquement si un commit/push est demande :

```powershell
git status
git add <fichiers-valides>
git commit -m "Message court"
git push
```

Sur l'autre ordinateur, reprendre par :

```powershell
git pull --tags
```

## Build local

- Minecraft cible actuel : voir `gradle.properties`.
- Pour Minecraft 26.2, utiliser JDK 25.
- Commande Windows habituelle :

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-25.0.3.9-hotspot'
.\gradlew.bat --no-daemon clean build
```

- Commande macOS/Linux :

```bash
./gradlew --no-daemon clean build
```

- La tache Gradle `installClientJar` installe le jar dans `%APPDATA%/.minecraft/mods` (Windows), `~/Library/Application Support/minecraft/mods` (macOS) ou `~/.minecraft/mods` (Linux).
- Verifier qu'un seul `valet-*.jar` est present dans le dossier mods.
- Le build 0.4.4 publie porte le SHA-256 `8C32B2940A1F23A06EB57499435AA5C9110D527BFA24AF575697B38B47027B68`.

## Fichiers de trace

- `JAR_REGISTRY.md` : registre des versions, jars et hash SHA-256.
- `CHANGELOG.md` : changements humains par version.
- `docs/releases/vX.Y.Z.md` : notes de release versionnees.
- `README.md` : etat public du projet et liens principaux.
- `docs/crafts.md` : guide craft public.
- `docs/visual-asset-pipeline.md` : outils IA par type d'asset, prompts anglais et controles Minecraft.
- `scripts/generate_visual_assets.py` : source reproductible des quatre PNG visibles; les vues avant/arriere du skin y reprennent le modele SuperMaker valide sur les UV et secondes couches Java. Les concepts externes avec filigrane ou faux damier ne doivent jamais etre copies directement dans le jar.

## Regles de release

- Finir le build final avant de mettre a jour les hash.
- Mettre a jour `JAR_REGISTRY.md` et `docs/releases/vX.Y.Z.md` avec le hash final.
- Si publication GitHub demandee : commit, tag `vX.Y.Z`, push `main` + tag, puis release GitHub avec le jar.
- Ne pas pousser ni publier sans demande explicite.

## Debug en jeu

- Lire le jar installe et `latest.log` avant de modifier le code.
- Les logs sont dans le dossier Minecraft de l'OS, sous `logs/latest.log`.
- Les signatures connues utiles :
  - `goal restarts order`
  - `craft no_path`
  - `craft drops_no_space`
  - `group command=attack_area`
  - `group swim_start`
  - `group swim_approach`
  - `group swim_recovery`
  - `group swim_rejected`
  - `brain home_assigned`
  - `brain home_reclaimed`
  - `brain gate_open`
  - `brain gate_close`
  - `logistics no_home_path`
  - `breeding no_target`
  - `cap_reached`
- Si le valet ne bouge pas, ne pas s'arreter a l'UI : verifier le chemin runtime et le comportement visible.
- L'UI de quetes ouverte par `J` et par clic droit sur le maire est la meme; elle doit afficher les objets, les quantites et les livraisons terminees avec des couleurs ARGB opaques.
- La navigation Valet suit la regle villageois pour les portes : bois ouvrable, fer et cuivre fermes non actionnes.
- Pour un portillon, le log attendu est `brain gate_open`, puis `path navigation_start ... mode=brain_vanilla`, la progression, puis `brain gate_close`; `navigation_rejected` ne doit plus suivre immediatement l'ouverture.
- La boucle fermier `navigation_rejected purpose=CROP` depuis une terre labouree a ete corrigee en 0.4.2 en normalisant le premier noeud vanilla sur les supports partiels.
- Le fermier 0.4.2 vise une case sure adjacente pendant l'approche longue, maintient sa `WALK_TARGET`, puis agit localement a portee; `MoveToTargetSink` calcule le trajet et `InteractWithDoor` ouvre les portes.
- Un nouvel ordre de ferme active `Replanter` par defaut. Dans les logs, `replanted=false` reste normal si l'ordre affiche `replant=false`.
- Une cible `PLANT` deja sous les pieds du fermier doit produire directement `farm planted ... item=...`, sans passer par `farm no_path`; le flux couvre les quatre cultures de terre labouree et les verrues du Nether sur sable des ames.
- Avec `Replanter` actif, le depot doit conserver une pile par item de plantation. Si l'inventaire est vide devant un sol compatible, les logs attendus sont `farm needs_planting_items`, puis `logistics withdrew_planting`, puis `farm planted`.
- Retirer les balises ne supprime pas un champ deja enregistre : selectionner sa ligne dans l'UI du fermier puis utiliser `Suppr. champ`; le serveur efface alors la zone persistante et les ordres qui la referencent.
- Pour planter depuis un stockage, placer un coffre/baril pres du valet, de son ancre ou d'un coin du champ; les quatre coins sauvegardes sont recherches meme si les balises ont ete retirees.
- Quand ces stockages sont epuises, le fermier continue la recolte et le labour puis retente la demande de plantation apres 10 secondes.
- Pour une traversee, `swim_approach` doit preceder `swim_start`; toute nouvelle occurrence de l'ancienne signature `swim_rejected` indiquerait qu'un ancien jar est encore charge.
- En 0.4.4, aucune mission de groupe ne creuse pour se deplacer; un echec de surface provoque attente et replanification.
- Le HOME d'un valet se choisit avec l'`Insigne lit` demande dans son UI, puis utilise sur un lit dans un rayon de 32 blocs autour de l'ancre. L'objet est lie a l'UUID exact, produit `brain home_assigned mode=explicit` et n'est consomme qu'en cas de succes.
- Pour un fermier ou un eleveur, la zone selectionnee devient son ancre mobile. Un centre non praticable est remplace par la case sure la plus proche; le rayon residentiel ne doit jamais elargir les cibles ou coffres de travail.
- Les sept anciens postes sont completement absents du jar; ne pas recreer leurs registres, POI, assets ou recettes.
- L'eleveur exige un enclos sauvegarde selectionne dans l'UI, sans poste; `breeding restocked` prouve qu'il a atteint le coffre et `breeding fed` qu'il a nourri un adulte pret a portee.
- Apres la seconde balise de ferme ou d'enclos, les coordonnees sont persistantes : les deux blocs peuvent etre casses. Les zones se suppriment ensuite dans l'UI; supprimer un enclos annule les ordres qui le referencent.
- `breeding no_feed_source` signifie qu'aucun aliment compatible n'a ete reconnu dans l'inventaire ou les stockages scannes; `containers`, `excluded` et `feedTypes` permettent de distinguer coffre absent, coffre temporairement inaccessible et mauvais aliment. Le retrait se fait maintenant par lot de 16 et `breeding restocked count=N` donne la quantite reelle.
- Le bouton `Inventaire` ouvre les huit slots reels du villageois dans un conteneur vanilla 9x1; le neuvieme emplacement d'adaptation refuse tout depot.
- Deux adultes nourris se rapprochent et s'accouplent ensuite via leur `BreedGoal` vanilla; `breeding no_path` doit etre temporise et ne pas boucler sur la meme entite ou le meme coffre.
- Les quetes parcourent directement les slots de l'inventaire joueur : ne pas reutiliser `clearOrCountMatchingItems` avec un conteneur nul en 26.2.
- Le maire est unique par dimension via `ValetMayorState`; tout doublon charge est supprime, le maire porte un trident et son clic droit ouvre les quetes.
- `swim_recovery` doit apparaitre environ deux secondes apres une immobilite aquatique et indiquer le nouveau detour choisi.
- La carte tactique utilise une texture dynamique liberee a la fermeture de l'ecran et un cache de 131 072 cellules; verifier ces deux points avant d'ajouter un nouveau rendu par cellule.

## Direction gameplay

- Preferer des metiers separes choisis dans l'UI; ne pas recreer de dependance a un poste ou a `JOB_SITE`.
- Favoriser les systemes visibles : blocs, items, UI, commandes, messages.
- Eviter les comportements caches ou trop automatiques sans controle joueur.
- Pour les nouvelles features, penser test en jeu des le depart.
