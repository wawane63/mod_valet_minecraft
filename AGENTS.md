# AGENTS.md

Instructions pour Codex et autres agents sur ce repo.

## Reponses

- Repondre en francais.
- Faire court et concret.
- Utiliser des bullets quand c'est plus lisible.
- Eviter les longs plans si une action directe est possible.

## Source de verite

- Le repo courant est la source de verite, pas les anciens chats.
- Lire d'abord `README.md`, `CHANGELOG.md`, `JAR_REGISTRY.md` et `docs/important-notes.md`.
- Les notes de release versionnees sont dans `docs/releases/`.
- Les tags GitHub marquent les versions publiees.

## Workflow Valet

- Apres un changement Java/resources qui modifie le jar : compiler et installer localement le jar.
- Pour la ligne Minecraft 26.2, utiliser JDK 25 si disponible.
- Verifier que `%APPDATA%/.minecraft/mods` ne garde pas plusieurs `valet-*.jar`.
- Pour les changements de jar, garder `JAR_REGISTRY.md`, `CHANGELOG.md` et `docs/releases/vX.Y.Z.md` synchronises.
- Ne pas push GitHub sans demande explicite de l'utilisateur.

## Debug runtime

- Pour les bugs en jeu, lire `latest.log` avant de patcher.
- Verifier le jar vraiment installe avant de conclure.
- Prioriser le comportement visible en jeu : mouvement, ordre actif, blocs poses, UI, messages joueur.
- Si une correction ne change pas le symptome, relire les logs et suivre la nouvelle signature.

## Direction projet

- Preferer des metiers separes et des postes visibles en jeu.
- Eviter de transformer le valet generique en super-valet cache.
- Les nouveaux systemes doivent etre testables via blocs, UI, items ou commandes visibles.
