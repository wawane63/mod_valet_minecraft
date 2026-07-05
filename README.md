# Valet

Mod Fabric 26.2 qui ajoute les metiers de villageois `Valet`.

La tracabilite des jars, bugs corriges et changements par version est tenue dans [JAR_REGISTRY.md](JAR_REGISTRY.md).

Derniere version : 0.3.5 - blueprints ameliores : rotation, miroir et liste des materiaux manquants avant lancement.

## Guides

- [Objets et crafts Valet](docs/crafts.md)
- [Notes 0.3.5](docs/releases/v0.3.5.md)

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
| 0.3.5 | Blueprints ameliores | `local` |

## Depart

- Place un `Poste d'artisan`, un `Poste de combattant`, un `Poste de fermier`, un `Poste de magicien` ou un `Poste d'eleveur`.
- Un villageois sans metier peut prendre un de ces postes comme travail.
- Artisan : minerais, bois, construction, craft.
- Combattant : defense locale, arbre epee et arbre arc.
- Fermier : recolte, replantation, houe, zones de ferme.
- Magicien : sort de glace, crocs magiques, soins, buffs allies et malus ennemis.
- Eleveur : reproduit, tond, ramasse les oeufs, trait et abat le surplus dans les enclos.
- Construction : les blueprints se tournent selon le regard, se posent en miroir accroupi, et signalent les materiaux manquants avant depart.
- Coffres/barils : bouton `Tri` directement dans l'interface du conteneur.
- Pupitre de groupe : cree des groupes et lie une carte ou une corne de chevre pour commander suivi, garde, attaque et rappel.

## Build

Prerequis :

- JDK 25
- Minecraft 26.2 + Fabric Loader 0.19.3
- Fabric API 0.153.0+26.2 dans `%APPDATA%\.minecraft\mods`
- Gradle wrapper du repo

Commande :

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-25.0.3.9-hotspot'
.\gradlew.bat build
```

Le `.jar` est genere dans `build/libs/` et la tache `installClientJar` installe automatiquement la derniere version dans `%APPDATA%\.minecraft\mods`.
