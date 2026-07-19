# Guide des objets et crafts Valet

Ce guide liste les objets ajoutes par le mod Valet et leur obtention.

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

## Insigne de lit

`valet:bed_badge`

- Aucun craft : ouvre l'UI du valet et clique `Insigne lit`.
- L'objet est lie a l'UUID de ce valet uniquement; utilise-le sur son lit.
- Il est consomme seulement si l'assignation du lit reussit.

## Balises de zone

Les balises servent a selectionner une zone dans le monde. Pose deux balises du meme type pour definir le volume; apres confirmation, leurs coordonnees sont sauvegardees et les blocs peuvent etre casses.

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
