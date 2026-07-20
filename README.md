# Valet

Mod Fabric 26.2 qui ajoute les metiers de villageois `Valet`.

La tracabilite des jars, bugs corriges et changements par version est tenue dans [JAR_REGISTRY.md](JAR_REGISTRY.md).

Version actuelle : 0.4.4 - Brain borne, valets sans poste et residence explicite.

Derniere version publiee : 0.4.4 (`v0.4.4`).

## Guides

- [Objets et crafts Valet](docs/crafts.md)
- [Notes 0.3.8](docs/releases/v0.3.8.md)
- [Notes 0.3.9](docs/releases/v0.3.9.md)
- [Notes 0.4.0](docs/releases/v0.4.0.md)
- [Notes 0.4.1](docs/releases/v0.4.1.md)
- [Notes 0.4.2](docs/releases/v0.4.2.md)
- [Notes 0.4.3](docs/releases/v0.4.3.md)
- [Notes 0.4.4](docs/releases/v0.4.4.md)
- [Pipeline visuel IA](docs/visual-asset-pipeline.md)

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
| 0.4.3 | Navigation de surface et maire unique interactif | [`v0.4.3`](https://github.com/wawane63/mod_valet_minecraft/tree/v0.4.3) |
| 0.4.4 | Brain borne, valets sans poste et residence explicite | [`v0.4.4`](https://github.com/wawane63/mod_valet_minecraft/tree/v0.4.4) |

## Depart

- Fabrique une `Insigne de valet` avec une etiquette et une emeraude.
- Utilise l'insigne sur un villageois adulte pour lui attribuer l'identite Valet persistante.
- Choisis son metier depuis son interface; le premier metier est Artisan.
- Aucun poste de metier n'existe encore : l'insigne porte l'identite et le role se choisit dans l'UI.
- Pour le lit, ouvre l'UI du valet et clique `Insigne lit`; le valet te donne gratuitement un insigne lie a son identite. Utilise cet objet sur un lit libre dans un rayon de 32 blocs autour de son ancre pour reserver son `HOME`.
- Pour le fermier et l'eleveur, le champ ou l'enclos selectionne devient l'ancre et le territoire; changer de selection deplace cette ancre.
- Si le centre geometrique du champ ou de l'enclos n'est pas praticable, l'ancre est placee sur la case sure la plus proche dans cette zone.
- Deux balises enregistrent les coordonnees d'un champ ou d'un enclos; elles peuvent ensuite etre cassees sans supprimer la zone.
- Les champs et les enclos enregistres peuvent etre supprimes depuis l'UI du valet correspondant.
- Le bouton `Inventaire` ouvre un conteneur vanilla interactif pour prendre ou deposer directement les objets du valet.
- Sans lit ou chemin valide, il abandonne la tentative sans explorer les maisons, casser un bloc ni creuser.
- La version 0.4.4 inclut le kit visuel refait depuis des concepts IA en ligne : Insigne original, icone Artisan, skin Valet 64x64 fidele au modele SuperMaker valide et icone du mod, tous reconstruits sans filigrane.
- Artisan : minerais, bois, construction, craft.
- Combattant : defense locale, arbre epee et arbre arc.
- Fermier : recolte et plantation du ble, carottes, pommes de terre, betteraves et verrues du Nether; reserve les items a replanter, peut en prendre dans un coffre proche, zones supprimables depuis l'UI et deplacement continu vanilla.
- Magicien : sort de glace, crocs magiques, soins, buffs allies et malus ennemis.
- Eleveur : exige un enclos sauvegarde choisi dans l'UI, rejoint chaque adulte pret et le nourrit individuellement; le `BreedGoal` vanilla forme ensuite le couple. Il marche aussi jusqu'aux coffres proches de l'enclos et prend une reserve de 16 nourritures par passage.
- Cuisinier : recolte ble et pommes de terre, prend les ingredients crus dans son coffre dedie, prepare puis depose les repas.
- Coffre de cuisinier : stockage dedie et obligatoire pour les ingredients et les repas du cuisinier.
- Intendant : transfere automatiquement des piles entre coffres/barils proches.
- Filtres intendant : les 9 premiers slots d'un coffre/baril cible declarent les items acceptes; le filtre le plus a gauche et le coffre le plus proche dans son territoire sont prioritaires.
- Construction : les blueprints se tournent selon le regard, se posent en miroir accroupi, et signalent les materiaux manquants avant depart.
- Coffres/barils : bouton `Tri` directement dans l'interface du conteneur.
- Carte des valets : touche `K`, avec les onglets `Carte` et `Groupes de valets`.
- Quetes du maire : clic droit sur le maire ou touche `J`; la meme UI montre l'objet demande, la progression et les livraisons terminees. Un seul maire par dimension apparait pres d'une cloche si le village possede un chat et un golem.
- Groupes de valets : cree ou supprime les groupes et affecte les valets disponibles depuis un menu unique.
- Carte tactique : affiche le terrain charge et permet d'envoyer ou rappeler le groupe selectionne.
- Portes : les valets ouvrent les portes en bois, mais n'actionnent jamais une porte en fer ou en cuivre fermee.
- Portillons : le Brain ouvre seulement un portillon coherent avec le chemin bloque, exige que le nouveau trajet vanilla passe par ce portillon puis le referme apres le passage.
- Missions de groupe : un ordre joueur peut sortir du territoire; sans chemin de surface, le groupe attend et replanifie au lieu de creuser.

## Installation sur un PC vierge Windows

1. Lance une fois **Minecraft 26.2**, puis ferme Minecraft et son launcher.
2. Telecharge [Fabric Installer pour Windows](https://fabricmc.net/use/installer/).
3. Dans l'installateur :
   - Minecraft : `26.2`
   - Loader : `0.19.3`
   - Coche `Create Profile`
   - Clique sur `Install`
4. Telecharge :
   - [Valet 0.4.4](https://github.com/wawane63/mod_valet_minecraft/releases/download/v0.4.4/valet-0.4.4.jar)
   - [Fabric API 0.154.2+26.2](https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/0.154.2+26.2/fabric-api-0.154.2+26.2.jar)
5. Appuie sur `Windows + R`, puis saisis :

   ```text
   %APPDATA%\.minecraft\mods
   ```

6. Cree le dossier `mods` s'il n'existe pas.
7. Place les deux jars dedans, sans les decompresser :

   ```text
   valet-0.4.4.jar
   fabric-api-0.154.2+26.2.jar
   ```

8. Dans le launcher Minecraft, selectionne `fabric-loader-0.19.3-26.2`.
9. Lance le jeu.

Tu n'as pas besoin d'installer Java, Git, Gradle ou le code source : l'installateur Fabric Windows utilise le Java fourni avec le launcher officiel. Voir la [documentation Fabric](https://docs.fabricmc.net/players/installing-fabric/windows).

Le projet est distribue sous licence MIT, voir [LICENSE](LICENSE).
