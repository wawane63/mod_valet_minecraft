# Pipeline visuel IA

## Outils par asset

- Modeles 3D Blockbench futurs : Spaleforce AI, export `.bbmodel` puis JSON Java.
- Textures de blocs : Media.io, PNG 16x16 ou 32x32, obligatoirement `seamless` / `tileable`.
- Items et icones : PicFixer ou SuperMaker pour le concept, puis reconstruction pixel-perfect en PNG 16x16.
- Skins humanoides : SuperMaker pour le concept, puis reconstruction sur le patron Java wide 64x64.
- Finition repo : `scripts/generate_visual_assets.py`, validation des dimensions, transparence, UV, references JSON et build Gradle.

Les sites externes peuvent demander une connexion, des credits ou produire un filigrane. Les exports gratuits testes peuvent aussi integrer visuellement leur damier de transparence. Ces fichiers restent des concepts : le filigrane n'est pas retire et aucun export brut n'entre dans le jar.

## Production locale reproductible

```bash
/Users/mac/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/bin/python3 scripts/generate_visual_assets.py
```

Le script reconstruit l'Insigne, l'icone Artisan et l'icone Fabric depuis la direction artistique IA. Pour le skin, `artisan-skin-source.svg` fournit les faces laterales et le script reporte pixel par pixel les vues avant/arriere du modele SuperMaker valide sur le patron Java wide 64x64. La frange, le tablier, les poignets et le harnais utilisent les vraies secondes couches Minecraft pour conserver leur relief en jeu.

## Kit pilote Artisan

### Insigne de Valet

```text
Minecraft item icon, professional Valet badge made from a polished emerald set into a compact brass heraldic crest, 16x16 pixel art, clear one-pixel dark outline, flat 2D sprite, centered, transparent background, modern post-1.14 vanilla Minecraft style, limited palette, readable at inventory scale, no text, no letters, no 3D render, no realism, no smooth gradients, no antialiasing
```

Sortie : `assets/valet/textures/item/valet_tag.png`.

### Icone de role Artisan

```text
Minecraft role-selection UI icon, one small iron hammer crossed with one compact wooden-handled pickaxe, tiny emerald badge at their crossing point, 16x16 pixel art, clear one-pixel dark outline, flat 2D sprite, transparent background, modern post-1.14 vanilla Minecraft style, limited spruce brown, iron gray and emerald green palette, readable at 16x16, no text, no 3D render, no realism, no smooth gradients, no antialiasing
```

Sortie : `assets/valet/textures/gui/role/artisan.png`.

### Skin Artisan

```text
Official Minecraft Java Edition player skin texture sheet, 64x64 PNG, wide Steve arm layout, professional village Artisan Valet, layered asymmetric auburn hair, dark spruce-green work coat, long tan leather apron with two pockets, emerald-and-brass badge on chest, rolled cream sleeves, crossed brown leather harness on the back, brown work trousers, sturdy charcoal boots, modern post-1.14 vanilla Minecraft pixel art, all front, back, left, right, top and bottom faces aligned to the official skin UV template, outer layer used for apron, harness, cuffs and hair, no cape, no slim Alex arms, no photorealism, no smooth gradients, no 3D render
```

Sortie : `assets/valet/textures/entity/valet/artisan.png`.

## Regles de validation

- PNG items et UI : 16x16, RGBA, fond transparent et silhouette lisible a taille reelle.
- Skins : 64x64, patron Java valide, bras wide 4 pixels et zones inutilisees transparentes.
- Blocs : coordonnees cubiques dans l'espace 0-16 et textures Vanilla ou locales resolues.
- Aucun photorealisme, antialiasing, degrade lisse, filigrane ou texte genere.
- Le damier de transparence doit etre une vraie couche alpha, jamais un motif gris et blanc dans les pixels.
- Compiler et installer le jar apres chaque integration de ressources.
