# Suivi des taches

## Regle active

- Lire `plan.md` et `task.md` avant chaque action.
- Apres chaque modification de code, mettre a jour ce fichier.
- Si une modification change l'architecture ou le workflow, mettre aussi `plan.md` a jour.

## Etat courant

- [x] Initialiser `plan.md` a la racine du projet.
- [x] Initialiser `task.md` a la racine du projet.
- [x] Integrer les regles du repo : francais, court, source de verite locale, pas de push sans demande.
- [x] Noter l'architecture globale actuelle du mod.
- [x] Relire les 220+ fichiers source, assets, configs et documents du depot.
- [x] Auditer l'historique, les tags, le remote et l'integrite Git.
- [x] Corriger le code Java, les mixins, le reseau, la persistence, les inventaires et les ressources.
- [x] Optimiser les boucles de tick, scans, allocations, reservations et payloads GUI.
- [x] Nettoyer le code mort, les assets orphelins et les traductions inutilisees.
- [x] Passer le projet en maintenance locale `0.3.8` et synchroniser les documents.
- [x] Mettre a jour Fabric API `0.154.2+26.2` et Loom `1.17.14`.
- [x] Ajouter la carte tactique au menu Echap avec zoom, repere et legende.
- [x] Gerer les groupes depuis la carte et envoyer tous les metiers vers un repere.
- [x] Ajouter le pathfinding longue distance par etapes et les tickets de chunks temporaires bornes.
- [x] Centraliser creation, suppression et affectation dans l'onglet `Groupes de valets`.
- [x] Retirer le pupitre, la carte de groupe, la liaison de corne et leurs fichiers devenus inutiles.
- [x] Publier la 0.3.9 avec tag, release GitHub, notes et jar.
- [x] Demarrer la 0.4.0 avec une identite Valet persistante independante du poste.
- [x] Ajouter l'Insigne de valet, sa recette et la migration des anciens valets.
- [x] Ajouter le choix du métier dans l'interface du valet.
- [x] Supprimer l'attribution automatique par poste et les restaurations destructrices de profession.
- [x] Ajouter la recette `Etiquette + Emeraude` de l'insigne au livre de recettes.
- [x] Reprendre le tunnelage des artisans pour les missions de groupe bloquées par un fort dénivelé.
- [x] Corriger les galeries parallèles et les boucles `water_escape` observées dans le log de mission.
- [x] Sécuriser le tunnelage sous les colonnes de gravier et de sable.
- [x] Ajouter cohésion à 5 blocs, accostage et défense minimale à l'épée aux groupes en mission.
- [x] Remplacer le tunnel droit des groupes par le planificateur 3D des artisans : escaliers, contournement des fluides et détection d'immobilité.
- [x] Garantir trois blocs de hauteur dans les galeries pour le suiveur et corriger l'orientation tête/corps du creuseur.
- [x] Stabiliser le meneur, supprimer l'attente bloquante à 5 blocs et calmer les recalculs/retours arrière observés dans le log de 20:06.
- [x] Finaliser et publier la 0.4.0 avec commit, tag, notes completes et jar GitHub.

## Derniere action

- 0.3.9 publiee sur GitHub avec son jar et ses notes.
- 0.4.0 locale initialisee avec `ValetIdentity` et l'Insigne de valet.
- Les anciens valets bases sur la profession sont automatiquement marques au chargement.
- Build local valide; `valet-0.4.0.jar` installe seul avec le SHA-256 `01A717BF93DB703F698D3BEC9D7BEE2675A6D480CFCAB2EB311A98195CB61688`.
- Bootstrap serveur 0.4.0 valide jusqu'a l'arret EULA attendu.
- Le log du test de 13:23 confirme la cohésion, mais montre que l'ancien tunnel de groupe cassait à hauteur constante; la mission utilise désormais un chemin d'excavation 3D local.
- Le log du test de 13:38 confirme le chemin 3D du creuseur; la hauteur passe à trois blocs car son déplacement forcé masquait les passages trop bas pour le suiveur.
- Le log du test de 20:01 montre des chemins de 28 à 32 pas qui reviennent sur leurs traces et des positions reprises en boucle; le meneur est désormais fixe et les chemins fortement rétrogrades sont rejetés.
- 0.4.0 finalisee pour publication GitHub avec le jar `01A717BF93DB703F698D3BEC9D7BEE2675A6D480CFCAB2EB311A98195CB61688`.

## Prochaine tache

- Reprendre le developpement sur une future version apres les retours de la 0.4.0.
