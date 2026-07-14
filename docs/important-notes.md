# Notes importantes pour reprendre le projet

Ce fichier sert a donner le contexte utile a Codex quand le repo est clone sur un autre ordinateur.

## Etat courant

- Repo GitHub : `https://github.com/wawane63/mod_valet_minecraft.git`
- Branche durable : `main`
- Version stable actuelle : voir `README.md` et `JAR_REGISTRY.md`
- Version locale en cours : `0.4.1` (maire, quetes et raccourcis clavier)
- Derniere release publiee : `v0.4.1`
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

## Fichiers de trace

- `JAR_REGISTRY.md` : registre des versions, jars et hash SHA-256.
- `CHANGELOG.md` : changements humains par version.
- `docs/releases/vX.Y.Z.md` : notes de release versionnees.
- `README.md` : etat public du projet et liens principaux.
- `docs/crafts.md` : guide craft public.

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
  - `logistics no_home_path`
  - `breeding no_target`
  - `cap_reached`
- Si le valet ne bouge pas, ne pas s'arreter a l'UI : verifier le chemin runtime et le comportement visible.

## Direction gameplay

- Preferer des metiers separes avec poste dedie : artisan, combattant, fermier, eleveur, magicien, etc.
- Favoriser les systemes visibles : blocs, items, UI, commandes, messages.
- Eviter les comportements caches ou trop automatiques sans controle joueur.
- Pour les nouvelles features, penser test en jeu des le depart.
