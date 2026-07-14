# Valet

Mod Fabric 26.2 qui ajoute les metiers de villageois `Valet`.

La tracabilite des jars, bugs corriges et changements par version est tenue dans [JAR_REGISTRY.md](JAR_REGISTRY.md).

Version locale actuelle : 0.4.2 - navigation vanilla, fermier et ameliorations generales.

Derniere version publiee : 0.4.2 (`v0.4.2`).

## Guides

- [Objets et crafts Valet](docs/crafts.md)
- [Notes 0.3.8](docs/releases/v0.3.8.md)
- [Notes 0.3.9](docs/releases/v0.3.9.md)
- [Notes 0.4.0](docs/releases/v0.4.0.md)
- [Notes 0.4.1](docs/releases/v0.4.1.md)
- [Notes 0.4.2](docs/releases/v0.4.2.md)

## Versions disponibles

| Version | Theme | Code GitHub |
| --- | --- | --- |
| 0.1.0 | Recolte | [`v0.1.0`](https://github.com/wawane63/mod_valet_minecraft/tree/v0.1.0) |
| 0.1.1 | Combat | [`v0.1.1`](https://github.com/wawane63/mod_valet_minecraft/tree/v0.1.1) |
| 0.1.2 | Craft | [`v0.1.2`](https://github.com/wawane63/mod_valet_minecraft/tree/v0.1.2) |
| 0.1.3 | Better UI + correctifs craft | [`v0.1.3`](https://github.com/wawane63/mod_valet_minecraft/tree/v0.1.3) |
| 0.2.0 | Portage Minecraft 26.2 | [`v0.2.0`](https://github.com/wawane63/mod_valet_minecraft/tree/v0.2.0) |
| 0.2.1 | Fermier local | [`v0.2.1`](https://github.com/wawane63/mod_valet_minecraft/tree/v0.2.1) |
| 0.3.0 | Decoupage metiers | [`v0.3.0`](https://github.com/wawane63/mod_valet_minecraft/tree/v0.3.0) |
| 0.3.1 | Crafts bois | [`v0.3.1`](https://github.com/wawane63/mod_valet_minecraft/tree/v0.3.1) |
| 0.3.2 | Tri coffre + magicien | [`v0.3.2`](https://github.com/wawane63/mod_valet_minecraft/tree/v0.3.2) |
| 0.3.3 | Creation de groupe | integre dans `v0.3.4` |
| 0.3.4 | Eleveur | [`v0.3.4`](https://github.com/wawane63/mod_valet_minecraft/tree/v0.3.4) |
| 0.3.5 | Blueprints ameliores | [`v0.3.5`](https://github.com/wawane63/mod_valet_minecraft/tree/v0.3.5) |
| 0.3.6 | Cuisinier + correctifs metiers | [`v0.3.6`](https://github.com/wawane63/mod_valet_minecraft/tree/v0.3.6) |
| 0.3.7 | Intendant + transferts coffres | [`v0.3.7`](https://github.com/wawane63/mod_valet_minecraft/tree/v0.3.7) |
| 0.3.8 | Audit, carte tactique et missions de groupe | [`v0.3.8`](https://github.com/wawane63/mod_valet_minecraft/tree/v0.3.8) |
| 0.3.9 | Gestion centralisee des groupes | [`v0.3.9`](https://github.com/wawane63/mod_valet_minecraft/tree/v0.3.9) |
| 0.4.0 | Tag Valet independant du poste | [`v0.4.0`](https://github.com/wawane63/mod_valet_minecraft/tree/v0.4.0) |
| 0.4.1 | Maire, quetes et raccourcis | [`v0.4.1`](https://github.com/wawane63/mod_valet_minecraft/tree/v0.4.1) |
| 0.4.2 | Navigation vanilla, fermier et ameliorations generales | [`v0.4.2`](https://github.com/wawane63/mod_valet_minecraft/tree/v0.4.2) |

## Depart

- Fabrique une `Insigne de valet` avec une etiquette et une emeraude.
- Utilise l'insigne sur un villageois adulte pour lui attribuer l'identite Valet persistante.
- Choisis son metier depuis son interface; le premier metier est Artisan.
- Place ensuite le poste visible correspondant quand le travail choisi en a besoin.
- Un poste seul ne cree plus de valet et son absence ne retire jamais l'identite Valet.
- Artisan : minerais, bois, construction, craft.
- Combattant : defense locale, arbre epee et arbre arc.
- Fermier : recolte et plantation du ble, carottes, pommes de terre, betteraves et verrues du Nether; reserve les items a replanter, peut en prendre dans un coffre proche, zones supprimables depuis l'UI et deplacement continu vanilla.
- Magicien : sort de glace, crocs magiques, soins, buffs allies et malus ennemis.
- Eleveur : reproduit, tond, ramasse les oeufs, trait et abat le surplus dans les enclos.
- Cuisinier : recolte ble et pommes de terre, prend les ingredients crus dans son coffre dedie, prepare puis depose les repas.
- Coffre de cuisinier : stockage dedie et obligatoire pour les ingredients et les repas du cuisinier.
- Intendant : transfere automatiquement des piles entre coffres/barils proches.
- Filtres intendant : les 9 premiers slots d'un coffre/baril cible declarent les items acceptes; le filtre le plus a gauche et le coffre le plus proche du poste sont prioritaires.
- Construction : les blueprints se tournent selon le regard, se posent en miroir accroupi, et signalent les materiaux manquants avant depart.
- Coffres/barils : bouton `Tri` directement dans l'interface du conteneur.
- Carte des valets : touche `K`, avec les onglets `Carte` et `Groupes de valets`.
- Quetes du maire : touche `J`; le maire apparait pres d'une cloche si le village possede un chat et un golem.
- Groupes de valets : cree ou supprime les groupes et affecte les valets disponibles depuis un menu unique.
- Carte tactique : affiche le terrain charge et permet d'envoyer ou rappeler le groupe selectionne.

## Build

Prerequis :

- JDK 25
- Minecraft 26.2 + Fabric Loader 0.19.3
- Fabric API 0.154.2+26.2 pour lancer le jeu (la dependance de compilation est geree par Gradle)
- Gradle wrapper du repo

Windows PowerShell :

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-25.0.3.9-hotspot'
.\gradlew.bat --no-daemon clean build
```

macOS/Linux :

```bash
./gradlew --no-daemon clean build
```

Le `.jar` est genere dans `build/libs/`. A la fin du build, `installClientJar` retire les anciens `valet-*.jar` puis installe la version courante dans le dossier Minecraft de l'OS :

- Windows : `%APPDATA%\.minecraft\mods`
- macOS : `~/Library/Application Support/minecraft/mods`
- Linux : `~/.minecraft/mods`

Le projet est distribue sous licence MIT, voir [LICENSE](LICENSE).
