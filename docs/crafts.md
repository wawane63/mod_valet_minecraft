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

### Poste d'eleveur

`valet:poste_eleveur`

Role : reproduction, tonte, ramassage d'oeufs et traite dans les enclos.

| | Table de craft | |
| --- | --- | --- |
| Ble | Cisailles | Ble |
| Seau | Bloc de paille | Seau |
| Planches | Planches | Planches |

Sortie : `1 Poste d'eleveur`

### Poste de cuisinier

`valet:cook_workstation`

Role : recolte du ble et des pommes de terre, collecte d'ingredients crus dans son coffre de cuisinier, cuisson et depot des repas dans ce meme coffre dedie.

| | Table de craft | |
| --- | --- | --- |
| Ble | Houe en bois | Ble |
| Bol | Fumoir | Bol |
| Planches | Planches | Planches |

Sortie : `1 Poste de cuisinier`

Repas prepares automatiquement : pain, pomme de terre cuite, steak, cotelette de porc cuite, poulet roti, mouton cuit, lapin cuit, morue cuite et saumon cuit.

### Poste d'intendant

`valet:steward_workstation`

Role : transferts automatiques entre coffres, coffres pieges et barils proches.

Utilisation : place les items filtres dans les 9 premiers slots du coffre/baril cible. L'intendant garde ces filtres, prend les items dans les coffres/barils sans filtre et remplit les destinations compatibles. Le filtre le plus a gauche est prioritaire, puis le coffre/baril le plus proche du poste.

| | Table de craft | |
| --- | --- | --- |
| Coffre | Entonnoir | Coffre |
| Redstone | Baril | Redstone |
| Planches | Planches | Planches |

Sortie : `1 Poste d'intendant`

### Coffre de cuisinier

`valet:cook_chest`

Utilisation : seul coffre dans lequel le cuisinier prend ses ingredients et depose les repas prepares. Il offre 27 emplacements et reste ouvrable par le joueur.

| | Table de craft | |
| --- | --- | --- |
| Bol | Fourneau | Bol |
| Ble | Coffre | Ble |
| Planches | Planches | Planches |

Sortie : `1 Coffre de cuisinier`

## Groupes de valets

La creation des groupes et l'affectation des valets se font dans `Echap > Carte des valets > Groupes de valets`.

## Insigne de valet

`valet:valet_tag`

Utilisation : clic droit sur un villageois adulte pour lui attribuer une identite Valet persistante.

Recette sans forme :

- `1 Etiquette` (`minecraft:name_tag`)
- `1 Emeraude`

La recette apparait dans le livre de recettes lorsque le joueur a obtenu ces deux ingredients.

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

### Balise d'enclos

`valet:animal_beacon`

Utilisation : definit un enclos pour l'eleveur. Deux balises autour d'animaux creent une zone poules, vaches, moutons ou cochons selon le type dominant.

| | Table de craft | |
| --- | --- | --- |
|  | Ble |  |
| Graines de ble | Papier | Graines de ble |
|  | Barriere |  |

Sortie : `2 Balises d'enclos`

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
- `Barriere` accepte le tag Minecraft `minecraft:fences`, donc n'importe quel type de barriere.
- Les recettes sont des recettes de table de craft 3x3.
- Les noms affiches ici suivent la traduction francaise du mod.
