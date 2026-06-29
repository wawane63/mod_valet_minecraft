# Valet

Mod Fabric 26.2 qui ajoute le metier de villageois `Valet`.

La tracabilite des jars, bugs corriges et changements par version est tenue dans [JAR_REGISTRY.md](JAR_REGISTRY.md).

## Versions disponibles

| Version | Theme | Code GitHub |
| --- | --- | --- |
| 0.1.0 | Recolte | [`v0.1.0`](https://github.com/wawane63/mod_valet_minecraft/tree/v0.1.0) |
| 0.1.1 | Combat | [`v0.1.1`](https://github.com/wawane63/mod_valet_minecraft/tree/v0.1.1) |
| 0.1.2 | Craft | [`v0.1.2`](https://github.com/wawane63/mod_valet_minecraft/tree/v0.1.2) |
| 0.1.3 | Better UI + correctifs craft | [`v0.1.3`](https://github.com/wawane63/mod_valet_minecraft/tree/v0.1.3) |
| 0.2.0 | Portage Minecraft 26.2 | [`v0.2.0`](https://github.com/wawane63/mod_valet_minecraft/tree/v0.2.0) |
| 0.2.1 | Fermier local | `local` |

## Depart

- Place un `Poste de valet`.
- Un villageois sans metier peut le prendre comme poste de travail.
- Le valet cherche autour de lui :
  - cultures mures a recolter/replanter ;
  - minerais proches a miner ;
  - coffres ou barils proches pour deposer son inventaire.

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
