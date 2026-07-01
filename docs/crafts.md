# Guide des objets et crafts Valet

Ce guide liste les objets ajoutes par le mod Valet et leur obtention.

## Postes de metier

Les postes transforment un villageois sans metier en valet specialise. Un poste doit rester reserve a un seul valet.

### Poste d'artisan

`valet:valet_workstation`

Role : bois, minerais, construction et craft.

| | Table de craft | |
| --- | --- | --- |
|  | Pioche en bois |  |
| Hache en bois | Coffre | Hache en bois |
| Planches | Planches | Planches |

Sortie : `1 Poste d'artisan`

### Poste de combattant

`valet:combat_workstation`

Role : defense locale, arbre epee et arbre arc.

| | Table de craft | |
| --- | --- | --- |
| Epee en bois | Arc | Epee en bois |
| Buche | Coffre | Buche |
| Planches | Planches | Planches |

Sortie : `1 Poste de combattant`

### Poste de fermier

`valet:farmer_workstation`

Role : recolte, replantation, passage de houe et zones de ferme.

| | Table de craft | |
| --- | --- | --- |
| Graines de ble | Houe en bois | Graines de ble |
| Composteur | Planches | Composteur |
| Planches | Planches | Planches |

Sortie : `1 Poste de fermier`

### Poste de magicien

`valet:magic_workstation`

Role : defense locale avec sort de glace, puis arbre magie `Destruction` / `Soin` / `Alteration`.

| | Table de craft | |
| --- | --- | --- |
| Fiole | Arc | Fiole |
| Buche | Coffre | Buche |
| Planches | Planches | Planches |

Sortie : `1 Poste de magicien`

## Balises de zone

Les balises servent a selectionner une zone dans le monde. Pose deux balises du meme type pour definir le volume.

### Balise de construction

`valet:construction_beacon`

Utilisation : definit une zone a copier en blueprint de construction.

| | Table de craft | |
| --- | --- | --- |
|  | Torche |  |
| Baton | Papier | Baton |
|  | Planches |  |

Sortie : `2 Balises de construction`

### Balise de ferme

`valet:farm_beacon`

Utilisation : definit une zone de champ pour le fermier.

| | Table de craft | |
| --- | --- | --- |
|  | Graines de ble |  |
| Graines de ble | Papier | Graines de ble |
|  | Planches |  |

Sortie : `2 Balises de ferme`

## Objets sans craft direct

### Blueprint de construction

`valet:construction_blueprint`

Obtention : donne par l'interface du valet artisan quand une construction copiee est selectionnee avec le bouton `Plan`.

### Coffre de fleches infini

`valet:infinite_arrow_chest`

Obtention : pas de recette de survie actuellement. Objet reserve au mode creatif ou a une commande.

## Legende

- `Planches` accepte le tag Minecraft `minecraft:planks`, donc n'importe quel type de planches.
- `Buche` accepte le tag Minecraft `minecraft:logs`, donc n'importe quel type de buche.
- Les recettes sont des recettes de table de craft 3x3.
- Les noms affiches ici suivent la traduction francaise du mod.
