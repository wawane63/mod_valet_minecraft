# Rapport de reconstruction intégrale du mod Valet

## Finalité du document

Ce document doit permettre à une personne, une IA ou un agent qui ne connaît pas le projet de reconstruire le mod Valet depuis zéro, sans nouvelle explication du créateur.

Il ne contient volontairement aucun extrait de code, aucun pseudo-code et aucune solution prête à recopier. Il décrit le produit attendu, les contraintes, les décisions déjà prises, les erreurs historiques, les mécanismes de persistance, les preuves disponibles et les choix encore ouverts. L’objectif est de préserver l’intention sans enfermer une future reconstruction dans l’implémentation actuelle.

Le dépôt courant reste la source de vérité factuelle. Les conversations et les logs servent à expliquer pourquoi certaines règles existent et à distinguer une correction réellement validée d’une hypothèse seulement compilée.

## 1. Résumé exécutif

Valet est un mod Fabric pour Minecraft 26.2. Il transforme un villageois adulte en assistant spécialisé au moyen d’une Insigne de valet. L’identité du valet est persistante et ne dépend plus d’un poste de métier. Le joueur choisit ensuite un métier dans une interface unique.

Le produit vise sept métiers :

- Artisan : minage ciblé, coupe de bois, construction par blueprint et craft.
- Combattant : défense locale, épée, arc, progression et atouts dédiés.
- Fermier : récolte, labour, plantation, replantation et logistique de graines.
- Éleveur : reproduction, tonte, collecte d’œufs, traite et contrôle du surplus dans un enclos choisi.
- Magicien : attaque de glace, crocs magiques, soin, régénération, résistance et affaiblissement.
- Cuisinier : collecte d’ingrédients, préparation de repas et usage exclusif d’un coffre de cuisinier.
- Intendant : transfert automatique entre coffres et barils selon des filtres visibles et des priorités.

Le mod comprend aussi :

- une carte tactique ;
- des groupes persistants ;
- des ordres longue distance ;
- un maire unique par dimension ;
- des quêtes de livraison ;
- des zones de ferme et d’élevage enregistrées par deux balises temporaires ;
- des blueprints de construction ;
- une progression et des arbres d’atouts ;
- un tri manuel de coffre ;
- un inventaire de valet directement manipulable.

La règle directrice du projet est la suivante : lorsqu’un comportement Minecraft vanilla correspond réellement au besoin, il faut le réutiliser aussi fidèlement que possible. Lorsqu’il n’existe pas d’équivalent, le comportement custom doit rester borné, explicite, visible et testable.

La reconstruction ne doit surtout pas réintroduire :

- un poste comme source de l’identité ou du métier ;
- une recherche globale de lit ;
- plusieurs ordonnanceurs concurrents ;
- un moteur de déplacement parallèle au pathfinding vanilla ;
- une téléportation bloc par bloc ;
- le creusement d’un tunnel uniquement pour atteindre une destination ;
- une action cachée impossible à observer en jeu ou dans les logs ;
- une confiance aveugle dans les données envoyées par le client.

## 2. État exact au 17 juillet 2026

### 2.1 État publié

- Branche publiée : main.
- Dernier commit publié : 7c80f7b.
- Dernier tag publié : v0.4.3.
- Hash du jar publié sur GitHub : 4ED4C5A9BC3C23FB2C66808E5ABDD4D6780114A91BDE22428D9FCF8A06C06E27.
- Cette version publiée contient la navigation de surface, le maire et les quêtes, mais pas toute la refonte locale décrite ci-dessous.

### 2.2 État local en cours

La branche `main` contient une refonte majeure au-dessus du tag v0.4.3 :

- 85 chemins suivis par Git sont modifiés ou supprimés ;
- plusieurs nouveaux fichiers Java, ressources, textures, documents et scripts ne sont pas encore suivis ;
- les sept anciens postes et leurs ressources sont supprimés ;
- l’ancien driver de travail, l’ancien planificateur A* et l’excavation de groupe sont supprimés ;
- le Brain borné, l’ancre sans bloc, la résidence explicite, l’Insigne de lit, l’inventaire interactif et la suppression d’enclos sont présents ;
- le kit visuel Artisan local est présent.

Le jar local installé et le jar de build sont identiques :

- fichier : valet-0.4.3.jar ;
- hash : 21705B769C2554A5A84DE0D791C313FD539ED3B2D6D90DFF8A41EAA3B2C0B45C ;
- Minecraft 26.2, Fabric Loader 0.19.3, Fabric API 0.154.2+26.2 et Java 25 le chargent correctement ;
- un seul jar Valet est installé ;
- les quatre assets visuels reconstruits correspondent bien à ceux inclus dans le jar.

Le dernier log disponible confirme seulement le démarrage client du 17 juillet à 22 h 33. Il ne contient pas de session de jeu avec les métiers. Il ne faut donc pas présenter ce démarrage comme une validation gameplay complète.

### 2.3 Conséquence pour une reprise

Il existe deux références différentes :

- v0.4.3 publié : point de retour propre et reproductible ;
- état de `main` après le tag : direction fonctionnelle la plus récente et jar réellement testé localement.

Une reconstruction fidèle doit utiliser l’état local pour l’intention finale, mais conserver le tag publié comme témoin historique et base de comparaison. Il ne faut pas écraser ou perdre l’état local avant d’en avoir fait une sauvegarde Git explicite.

## 3. Vision produit et expérience joueur

### 3.1 Fantaisie centrale

Le joueur ne commande pas un automate invisible. Il recrute des villageois et leur donne des métiers lisibles. Chaque système doit avoir une représentation en jeu : un item, une zone, un coffre, une interface, un repère, un groupe, une quête ou un message.

Les valets doivent paraître compétents mais rester des habitants du monde Minecraft :

- ils marchent réellement ;
- ils ouvrent les portes qu’un villageois peut ouvrir ;
- ils renoncent lorsqu’un chemin est impossible ;
- ils ne traversent pas les murs ;
- ils ne se téléportent pas ;
- ils ne détruisent pas une maison pour atteindre un lit ;
- ils ne creusent pas sous un chemin praticable ;
- ils ne choisissent pas spontanément le lit du joueur ;
- ils ne quittent pas leur territoire sans ordre explicite.

### 3.2 Identité et recrutement

- L’Insigne de valet est fabriquée avec une étiquette et une émeraude.
- Elle s’utilise sur un villageois adulte.
- Elle crée une identité Valet persistante liée à l’UUID du villageois.
- Le métier initial est Artisan.
- Le métier peut ensuite être changé dans l’interface.
- L’identité survit au déchargement de chunk, au retrait de blocs proches, aux missions et aux rechargements de sauvegarde.
- La profession vanilla historique peut encore servir à reconnaître et migrer un ancien valet, mais elle ne doit plus diriger le gameplay.

### 3.3 Principe des métiers

- Un métier correspond à un ensemble clair d’actions, d’options et d’atouts.
- Un métier ne doit pas devenir une simple étiquette posée sur un valet générique capable de tout faire.
- Les ordres incompatibles doivent être refusés côté serveur.
- Lors d’un changement de métier, un ordre incompatible doit être nettoyé proprement.
- Les données durables importantes, comme le niveau ou l’identité, doivent être conservées.
- Les actions automatiques sans ordre explicite sont réservées aux métiers conçus ainsi, notamment Cuisinier, Intendant, Magicien et Combattant.

### 3.4 Interface principale du valet

L’interface doit permettre au minimum :

- d’identifier le valet par son nom et son métier ;
- de changer son métier ;
- de renommer le valet ;
- de choisir un ordre compatible ;
- de choisir les paramètres propres au métier ;
- de voir le niveau, l’expérience et les atouts ;
- d’ouvrir le véritable inventaire du valet ;
- de demander l’Insigne de lit liée à ce valet ;
- de supprimer une zone persistante sélectionnée ;
- de régler le comportement libre et le retour nocturne.

L’interface est une vue du serveur. Toute modification doit être revalidée par le serveur. Une liste affichée peut être devenue obsolète au moment du clic ; le serveur doit alors refuser proprement et rafraîchir l’état.

## 4. Ancre, territoire, lit et routine

### 4.1 Ancre

Chaque valet possède une ancre persistante dans une dimension.

- Pour un valet sans zone spécialisée, l’ancre est créée à sa position lors du recrutement ou de la migration.
- Pour un fermier, le champ sélectionné devient l’autorité spatiale et déplace l’ancre.
- Pour un éleveur, l’enclos sélectionné devient l’autorité spatiale et déplace l’ancre.
- Le centre géométrique d’une zone n’est pas nécessairement praticable. L’ancre de travail doit être normalisée vers la case sûre la plus proche appartenant à la zone.
- Le changement de champ ou d’enclos doit déplacer l’ancre sans perdre l’identité, le métier, l’inventaire ou la progression.

### 4.2 Territoire de travail

Le territoire sert à borner :

- les cibles de travail ;
- les coffres et barils utilisables ;
- la fuite autonome ;
- le combat autonome ;
- la promenade libre ;
- les sorties d’eau locales ;
- les retours à l’ancre.

Une cible ou un chemin autonome qui sort du territoire doit être refusé. La seule exception générale est un ordre de groupe explicite du joueur.

Le territoire ne doit pas être une union floue entre ancienne ancre, poste, zone et maison. Cette union a déjà permis à des valets d’explorer les habitations du joueur et de viser des coffres ou des lits hors contexte.

### 4.3 Lit et résidence

Le lit est optionnel et explicitement assigné.

- Le joueur demande l’Insigne de lit depuis l’interface du valet.
- Le valet remet gratuitement un item lié à son UUID et à son nom.
- Le joueur utilise cet item sur le lit voulu.
- L’objet n’est consommé qu’en cas de succès.
- Le lit doit être un vrai point d’intérêt HOME, libre, présent dans la bonne dimension et atteignable.
- Le lit de réapparition d’un joueur doit être refusé.
- Un autre valet ne doit pas déjà posséder ce lit.
- Le rayon résidentiel est de 32 blocs autour de l’ancre.
- Ce rayon ne doit jamais élargir le territoire de travail ou la recherche de coffres.
- Sans lit valide, le valet ne cherche pas un autre lit et n’explore pas les maisons.
- À la mort ou à la suppression du valet, seul le ticket HOME dont la propriété est prouvée doit être libéré.

### 4.4 Routine et priorité

Le Brain est l’unique ordonnanceur général.

Priorité conceptuelle :

1. danger immédiat et survie ;
2. commande de groupe qui contrôle réellement le valet ;
3. activité de travail ;
4. retour ou repos ;
5. activité libre bornée ;
6. attente.

La nuit ne doit pas rendre le valet incapable de fuir ou de se défendre. Une mission ou une menace peut interrompre le repos. Quand l’interruption cesse, les anciennes mémoires de mouvement ne doivent pas reprendre un chemin devenu invalide.

Le rappel de groupe doit rendre la main au métier lorsque le relais local près de l’ancre est atteint. Une commande RECALL persistante ne doit jamais garder indéfiniment le contrôle d’un fermier arrivé chez lui.

## 5. Déplacement et intelligence artificielle

### 5.1 Règle vanilla

La règle validée par le créateur est : si Minecraft vanilla fournit le même comportement et qu’il peut être repris à l’identique, l’utiliser. Cette règle ne signifie pas qu’un comportement local de villageois suffit pour une mission de mille blocs.

Répartition attendue :

- sélection de la cible, territoire, ordre, réservations et règles métier : Valet ;
- calcul et exécution d’un trajet terrestre local : vanilla ;
- portes en bois : comportement villageois vanilla ;
- tâches sans équivalent, comme carte, groupes, blueprints et magie : custom ;
- missions longue distance : orchestration Valet par tronçons locaux, déplacement physique vanilla sur chaque tronçon.

### 5.2 Autorité unique

Une reconstruction ne doit avoir qu’une seule autorité de mouvement à un instant donné.

Les anciens problèmes venaient de la coexistence de :

- l’ordonnanceur Brain ;
- un driver de travail parallèle ;
- un goal général ;
- un planificateur A* custom ;
- une excavation de groupe ;
- une commande de groupe persistante ;
- des mémoires vanilla de lit et de poste.

Ces couches pouvaient effacer leurs cibles mutuelles, arrêter un chemin vanilla trop tôt, reprendre un ancien chemin, ou décider qu’un valet était arrivé alors qu’il se trouvait à une mauvaise hauteur.

### 5.3 Garde-fous obligatoires

Tout trajet autonome doit vérifier :

- que la destination est dans le territoire applicable ;
- que la dimension est correcte ;
- que le volume du valet est libre ;
- que le support est praticable ;
- que les pas verticaux restent réalistes ;
- qu’une grande chute n’est pas possible ;
- que le trajet ne traverse pas un fluide hors scénario de nage ;
- qu’il évite feu, cactus, magma, feu de camp, buisson épineux, rose du Wither, neige poudreuse et stalagmite ;
- qu’il ne brise aucun bloc pour ouvrir le chemin ;
- qu’il progresse réellement ;
- qu’un timeout entraîne un abandon et une replanification, pas une boucle immédiate.

Les chemins en terre, terres labourées et escaliers doivent être reconnus comme supports praticables dans toutes les couches. Le premier nœud d’un chemin peut être décalé par rapport à la position logique du villageois lorsqu’il se trouve sur un lit, de la terre labourée ou un autre support partiel ; ce cas ne doit pas être interprété comme un saut interdit.

### 5.4 Portes et portillons

- Une porte en bois fermée peut être ouverte.
- Une porte en fer ou en cuivre fermée est un obstacle.
- Le valet ne doit pas actionner un bouton pour contourner cette règle.
- Une porte ouverte n’est pas forcément une bonne destination finale ; le valet doit la traverser, pas tenter de s’immobiliser dans son bloc.
- Un portillon ne doit être ouvert que s’il est cohérent avec la direction de la cible et proche de la fin d’un chemin partiel.
- Après ouverture, un nouveau chemin doit confirmer que le passage est utile.
- Le portillon doit rester ouvert le temps du franchissement.
- Il doit être refermé lorsque le valet a changé de côté et que son volume est libre, avec un timeout de sécurité borné.

### 5.5 Eau

Pour une mission de groupe :

- le trajet terrestre rejoint d’abord la berge ;
- la nage vise une rive opposée ou un point aquatique intermédiaire ;
- la cible de nage reste stable ;
- le valet saute pour rester à la surface ;
- les suiveurs gardent une distance évitant d’emprisonner le meneur ;
- après 40 ticks sans progrès horizontal, un détour aquatique borné est choisi ;
- un échec de nage ne déclenche jamais une excavation sous-marine.

### 5.6 Longue distance

- Ne pas calculer un chemin global de mille blocs.
- Découper la mission en tronçons locaux.
- Utiliser des tickets de chunks temporaires et bornés.
- Conserver un meneur stable pendant la mission.
- Les suiveurs doivent rattraper le meneur sans bloquer le groupe parce qu’ils dépassent une distance arbitraire.
- Après plusieurs échecs de surface, attendre et replanifier.
- Ne jamais créer automatiquement un tunnel pour atteindre le repère.

### 5.7 Machine learning éventuel

Le machine learning n’est pas implémenté. La direction acceptée est hybride :

- sécurité, territoire, légalité et exécution restent déterministes ;
- un modèle futur peut classer des cibles ou détours locaux déjà autorisés ;
- le modèle ne peut jamais autoriser une chute, un fluide interdit, un bloc dangereux ou un creusement ;
- l’apprentissage doit se faire à partir d’une télémétrie structurée observation, action, résultat ;
- les logs textuels actuels sont une base, pas un dataset suffisant ;
- l’apprentissage direct en jeu n’est pas une première étape raisonnable.

## 6. Spécification détaillée des métiers

### 6.1 Artisan

L’Artisan regroupe quatre activités explicitement ordonnées.

#### Minage

- Le joueur choisit un type de minerai disponible.
- Le valet recherche une ressource correspondant au choix dans son territoire.
- Deux valets ne doivent pas réserver le même bloc.
- Le valet marche jusqu’à une position sûre à portée.
- Il mine uniquement le bloc cible et éventuellement le filon explicitement reconnu.
- Il ne casse jamais un bloc seulement pour ouvrir le passage.
- Les drops sont calculés avec les règles vanilla et un outil cohérent.
- Si l’inventaire ne peut pas recevoir tous les drops, le bloc ne doit pas être détruit sans solution de repli.
- Après le filon ou lorsque l’inventaire est plein, le valet revient déposer.

#### Coupe de bois

- Le joueur choisit une famille de bois.
- Le valet détecte les bûches connectées du même arbre, y compris en diagonale.
- La taille d’un arbre est bornée.
- La réservation empêche plusieurs valets de travailler sur le même arbre.
- Les feuilles et constructions voisines ne doivent pas être détruites comme chemin.

#### Craft

- La cible actuellement disponible est la pioche en pierre.
- Le valet peut utiliser plusieurs essences de bûches et de planches.
- Il peut prendre les matériaux dans son inventaire ou dans des coffres proches autorisés.
- Il peut transformer bûches en planches puis planches en bâtons.
- Il fabrique à l’ancre après avoir vérifié l’espace de sortie.
- À terme, les recettes doivent venir du gestionnaire de recettes vanilla plutôt que d’une liste Java figée.

#### Construction

- Deux balises copient une structure bornée.
- Les chunks de la zone doivent être chargés.
- La copie ignore les blocs non copiables définis par le produit.
- Le blueprint persistant contient dimensions, blocs et états.
- L’objet de blueprint conserve son identité et ses données.
- La pose tourne la structure selon le regard du joueur.
- La pose accroupie active le miroir.
- Un aperçu monde doit correspondre exactement au résultat réel.
- Avant de partir, le valet calcule les matériaux manquants dans son inventaire et les stockages autorisés.
- Le chantier est matérialisé par le bloc blueprint.
- Le valet place progressivement les blocs et gère les paires comme portes ou lits.
- Un emplacement occupé ou un matériau manquant produit un message clair.
- À la fin, le blueprint de chantier est retiré.
- À terme, la pose doit se rapprocher du pipeline vanilla pour les mises à jour de voisins et les blocs à entité.

### 6.2 Combattant

- Le combattant ne reçoit pas les ordres de récolte de l’Artisan.
- Il se défend localement et participe aux commandes de combat de groupe.
- Il choisit épée ou arc selon la situation et les ressources.
- Il ne doit pas recalculer un chemin chaque tick.
- À portée, il s’arrête proprement puis attaque selon un cooldown.
- L’arc consomme des flèches, sauf atout de récupération.
- Il peut se réapprovisionner dans un coffre contenant des flèches ou dans le coffre de flèches infini.
- Il évite de tirer lorsqu’un allié bloque la ligne si l’atout correspondant est actif.
- Deux arbres de progression séparés existent : épée et arc.
- La poursuite peut reprendre le mouvement vanilla ; la sélection d’alliés, les perks et les ordres restent custom.

### 6.3 Fermier

Le Fermier exige un champ enregistré et sélectionné.

- Deux balises définissent les coins du champ.
- Une fois la zone enregistrée, les balises peuvent être cassées.
- Le champ persiste par dimension avec un identifiant stable.
- L’interface permet de supprimer définitivement le champ.
- Supprimer un champ annule les ordres qui le référencent.
- La zone sélectionnée devient l’ancre et le territoire du fermier.

Cultures prises en charge :

- blé ;
- carottes ;
- pommes de terre ;
- betteraves ;
- verrues du Nether sur sable des âmes.

Options :

- cultures actives ;
- replanter, activé par défaut ;
- passer la houe.

Comportement attendu :

- choisir entre récolte, plantation et labour selon la proximité ;
- ne pas laisser un type d’action affamer les autres ;
- retirer une fine couche de neige ou un bloc de neige gênant si le sol doit être travaillé ;
- labourer un terrain vierge compatible ;
- planter directement si le sol vide est déjà à portée ;
- conserver une pile de chaque item de plantation actif avant dépôt ;
- chercher des plantations dans les coffres proches du valet, de l’ancre et des quatre coins sauvegardés ;
- différer de dix secondes une demande infructueuse sans bloquer les récoltes mûres et le labour ;
- marcher continûment vers une case sûre adjacente, puis agir localement comme un fermier vanilla.

### 6.4 Éleveur

L’Éleveur exige un enclos précis enregistré et sélectionné.

- Le choix global de tous les enclos est abandonné.
- Deux balises définissent un volume en trois dimensions.
- Le type dominant peut nommer l’enclos, mais une zone mixte reste possible.
- Les balises peuvent être cassées après enregistrement.
- Tous les enclos sauvegardés doivent rester visibles dans l’interface.
- La sélection devient l’ancre et le territoire.
- Une marge logistique de deux blocs permet les stockages juste à l’extérieur de la clôture.
- La suppression depuis l’interface est persistante et annule les ordres liés.

Actions optionnelles :

- reproduction ;
- tonte ;
- collecte d’œufs ;
- traite ;
- abattage du surplus.

Règles de reproduction :

- cibler un adulte vivant, prêt et non déjà amoureux ;
- réserver la cible pour éviter deux éleveurs ;
- rejoindre physiquement une position à portée ;
- prendre la nourriture compatible dans l’inventaire ou un stockage autorisé ;
- retirer jusqu’à 16 aliments par trajet au coffre ;
- nourrir individuellement l’adulte ;
- laisser ensuite le comportement de reproduction vanilla des animaux former le couple ;
- inclure les naissances déjà en attente dans le plafond ;
- revalider le plafond juste avant le nourrissage ;
- ne jamais abattre un animal amoureux ;
- mémoriser temporairement les animaux et coffres inaccessibles.

Le plafond par défaut reste 8. Le créateur a explicitement demandé de ne pas le modifier lors de la dernière correction. Avec 11 animaux, plafond 8 et abattage désactivé, l’absence de reproduction est normale.

La traite exige un seau et applique un long cooldown par vache. La tonte exige des cisailles. Les œufs au sol sont collectés. L’abattage ne s’active que si l’option est cochée et si le surplus est confirmé au dernier moment.

### 6.5 Magicien

- Le sort de base est un projectile de glace utilisant une primitive vanilla.
- Il inflige des dégâts et du ralentissement sans incendie.
- Après trois tirs, l’atout concerné peut déclencher des crocs magiques.
- Les branches sont Destruction, Soin et Altération.
- Le soin vise uniquement un allié blessé.
- La régénération vise un allié qui en a réellement besoin.
- La résistance vise un allié blessé ou engagé.
- L’affaiblissement et le ralentissement visent les ennemis.
- Les sorts de soutien ne doivent pas boucler en permanence hors besoin.
- Le magicien respecte les cibles de groupe et les limites du territoire hors ordre explicite.

### 6.6 Cuisinier

- Le Cuisinier fonctionne automatiquement.
- Il récolte blé et pommes de terre mûrs proches dans son territoire.
- Il replante si possible.
- Il utilise exclusivement le Coffre de cuisinier pour prendre les ingrédients et déposer les repas.
- Ce coffre possède 27 emplacements et une interface vanilla.
- Les préparations actuelles sont pain, pomme de terre cuite, viandes cuites et poissons cuits.
- Un retrait peut prendre un lot d’ingrédients plutôt qu’un seul item.
- La préparation s’effectue à l’ancre.
- La liste fixe actuelle doit être remplacée à terme par les recettes vanilla chargées, sans perdre le coffre dédié ni la cadence métier.

### 6.7 Intendant

- L’Intendant fonctionne automatiquement dans son territoire.
- Il considère coffres, coffres piégés et barils.
- Les neuf premiers slots d’une destination servent de filtres visibles.
- Les slots de filtre ne sont ni vidés ni remplis par l’intendant.
- Un conteneur sans filtre sert de source.
- Le filtre le plus à gauche a la priorité la plus forte.
- À priorité égale, le conteneur le plus proche de l’ancre est choisi.
- Un mauvais item déjà placé dans une destination filtrée peut être évacué vers une autre destination compatible.
- Le valet transporte des lots bornés via son inventaire visible.
- Les flèches ne sont pas traitées comme une marchandise normale.
- Le chemin vers les coffres ne mine aucun bloc.
- À terme, le cycle de recherche, déplacement et exclusion des conteneurs doit se rapprocher du comportement vanilla du golem de cuivre, tout en conservant les filtres Valet.

## 7. Systèmes transversaux

### 7.1 Inventaire

- Le valet conserve l’inventaire vanilla du villageois.
- Huit slots réels sont exposés dans une interface 9 par 1.
- Le neuvième emplacement est un adaptateur non déposable.
- Le joueur peut prendre et déposer directement des objets.
- Il ne doit pas exister de copie intermédiaire susceptible de diverger.
- Les métiers doivent respecter le nombre de slots utilisables et les atouts de capacité.

### 7.2 Logistique générale

- Après travail ou inventaire plein, le valet cherche un stockage autorisé.
- Les coffres de maison hors territoire sont interdits.
- Les flèches sont protégées des dépôts génériques.
- Le fermier conserve ses réserves de plantation.
- Un coffre vide ou inaccessible est temporairement exclu, pas relancé chaque tick.
- Un échec de dépôt ne doit pas empêcher le valet de reprendre un travail encore possible.

### 7.3 Tri de conteneur

- Un bouton Tri apparaît dans l’interface d’un coffre ou baril.
- Le client envoie uniquement une demande.
- Le serveur trie uniquement le conteneur actuellement ouvert et encore valide.
- Les piles identiques sont fusionnées puis ordonnées par identifiant.
- Les coffres doubles doivent être traités comme une unité cohérente.

### 7.4 Progression

- Chaque valet possède niveau, expérience, points en attente et atouts persistants.
- Le seuil général commence à 40 XP et augmente de 25 par niveau.
- Les arbres généraux sont liés au métier actif.
- Les atouts déjà acquis persistent lors d’un changement de métier, mais seuls les atouts compatibles doivent s’appliquer.
- Le Magicien possède Gel comme capacité intrinsèque.
- Le Combattant possède deux progressions séparées, épée et arc, avec un seuil initial de 30 XP et une augmentation de 20 par niveau.
- L’attribution d’un atout vérifie métier, points, prérequis et absence de doublon côté serveur.

### 7.5 Carte tactique

- La touche K ouvre la carte.
- La carte ne doit afficher que les chunks déjà connus du client.
- Elle ne doit ni découvrir ni charger le monde côté serveur.
- Elle affiche joueur, valets visibles, repère et groupe sélectionné.
- La molette zoome, le glisser déplace la vue et le clic droit pose un repère.
- Le terrain est rendu dans une texture dynamique unique.
- Le cache est borné à 131 072 cellules.
- Pendant un glisser, le recalcul est limité à 20 Hz.
- La texture doit être libérée à la fermeture.
- L’ancienne approche, qui lisait le monde et envoyait des milliers de rectangles à chaque événement souris, est interdite car elle faisait ramer l’interface.

### 7.6 Groupes

- Jusqu’à 32 groupes persistants par dimension.
- Un valet ne peut appartenir qu’à un seul groupe.
- Les groupes conservent nom, membres et commande.
- La gestion est centralisée dans les onglets Carte et Groupes.
- Les anciens pupitre, carte de groupe et liaison de corne sont supprimés et ne doivent pas revenir.

Modes :

- attente ;
- suivre ;
- garde proche ;
- garde large ;
- attaque d’une cible ;
- attaque d’une zone ;
- rappel ;
- aller au repère.

Tous les métiers peuvent suivre, être rappelés et aller à un repère. Les non-combattants ne doivent pas devenir combattants parce qu’un groupe reçoit un ordre d’attaque ou de garde.

### 7.7 Maire et quêtes

- Un maire peut apparaître près d’une cloche si un chat et un golem de fer vivent dans le village.
- Il ne doit y avoir qu’un maire par dimension.
- Son UUID et la cloche associée sont persistants.
- Les doublons chargés sont supprimés en conservant un maire canonique.
- Le maire reste près de la cloche, porte un trident non récupérable et a un nom visible.
- La touche J et le clic droit sur le maire ouvrent la même interface.
- Les textes doivent utiliser une couleur opaque.

Quêtes actuelles :

- 16 pains contre 3 émeraudes ;
- 32 torches contre 4 émeraudes ;
- 12 lingots de fer contre 6 émeraudes.

L’état accepté ou terminé est persistant par joueur. Le serveur compte et retire directement les items présents dans l’inventaire, puis attribue la récompense. L’interface affiche objet, quantité possédée, récompense et bilan terminé.

## 8. Persistance et mémoire

### 8.1 Principe général

La persistance est le risque technique le plus important du projet. L’état actuel combine :

- données attachées au NBT du villageois ;
- données SavedData par dimension ;
- tags persistants du joueur ;
- composants d’item ;
- mémoires Brain vanilla ;
- caches runtime en mémoire vive.

Une reconstruction doit définir une autorité unique pour chaque donnée et une procédure claire de chargement, sauvegarde, migration, déchargement et suppression.

### 8.2 Données persistantes par valet

À conserver sur l’entité villageoise :

- identité Valet ;
- métier ;
- ancre et dimension ;
- lit explicite et preuve de possession du ticket HOME ;
- options de comportement ;
- ordre actif ;
- cible de minerai ou de bois ;
- identifiant de champ et masque de cultures ;
- replantation et labour ;
- identifiant d’enclos et actions d’élevage ;
- plafond d’animaux ;
- identifiant de construction ;
- cible de craft ;
- niveau, XP, points et atouts ;
- niveaux, XP et atouts épée/arc.

L’inventaire n’a pas besoin d’un stockage Valet séparé : il appartient déjà au villageois vanilla.

### 8.3 Données persistantes par dimension

- Champs : identifiant, nom et limites.
- Enclos : identifiant, nom, type dominant éventuel et limites tridimensionnelles.
- Constructions copiées : identifiant, nom, dimensions et états de blocs.
- Groupes : identifiant, nom, membres et commande.
- Maire : UUID et position de cloche.

Chaque stockage doit :

- avoir une limite explicite ;
- ignorer les entrées invalides ;
- refuser les identifiants nuls, négatifs, dupliqués ou à risque de débordement ;
- recalculer le prochain identifiant à partir des données valides ;
- marquer la donnée comme modifiée après ajout ou suppression ;
- préserver l’ordre stable des identifiants.

Limites actuelles :

- 64 champs ;
- 64 enclos ;
- 64 blueprints ;
- 32 groupes.

### 8.4 Données persistantes par joueur

Les quêtes utilisent actuellement des tags de l’entité joueur :

- état actif par quête ;
- état terminé par quête.

Ce choix est simple et persistant, mais une reconstruction plus extensible pourra préférer une donnée structurée et versionnée. La règle fonctionnelle reste : l’état appartient au joueur, pas au maire ni à la dimension.

### 8.5 Données persistantes par item

- L’Insigne de lit contient l’UUID et le nom du valet ciblé.
- Le blueprint d’inventaire contient l’identité et les données compactes de la construction.
- Le bloc blueprint posé conserve construction, orientation et miroir.

Les composants custom doivent être bornés et validés avant usage. Un item falsifié ou ancien ne doit jamais permettre de modifier une autre entité ou de charger une structure démesurée.

### 8.6 Mémoire runtime éphémère

Les éléments suivants ne doivent pas être considérés comme autorité durable :

- cibles temporairement inaccessibles ;
- réservations de blocs et d’entités ;
- cooldowns de traite ;
- progression de nage ;
- meneur de mission ;
- cadence de recalcul de chemin ;
- conversation ou écran ouvert ;
- premier coin d’une paire de balises ;
- rappel temporaire ;
- portillon ouvert par un valet.

Ils doivent être nettoyés au déchargement de l’entité, à la mort, à l’arrêt du serveur ou à l’annulation de la tâche. Un cache runtime survivant à une entité supprimée peut réappliquer un ancien ordre à un nouvel objet partageant une référence logique.

### 8.7 Risques des caches statiques

L’implémentation actuelle recharge de nombreuses données d’entité dans des maps indexées par UUID. Cela fonctionne si les hooks de lecture, sauvegarde et déchargement restent parfaitement symétriques. Les risques sont :

- entrée conservée après suppression ;
- donnée runtime appliquée avant lecture complète du NBT ;
- divergence entre map et entité si une mutation n’est pas écrite ;
- ticket POI libéré deux fois ;
- ancienne donnée d’une dimension conservée après transfert ;
- état partiellement présent après migration.

Une reconstruction peut conserver ce modèle ou adopter des attachments, mais doit garantir les mêmes invariants et disposer de tests de cycle de vie.

### 8.8 HOME et tickets POI

La possession d’un lit ne peut pas être déduite uniquement d’une mémoire HOME présente dans le Brain. Cette mémoire peut avoir été créée par vanilla ou par une ancienne version.

Règles :

- seul un lit explicitement choisi est un HOME Valet ;
- la position, la dimension et la preuve de ticket possédé sont persistées ;
- au chargement, le POI est validé et le ticket réconcilié ;
- un ticket absent peut être repris uniquement pour le même lit encore valide ;
- un ticket qui n’appartient pas avec certitude au valet ne doit pas être libéré ;
- une mémoire HOME globale ou incohérente doit être effacée ;
- les mémoires JOB_SITE, POTENTIAL_JOB_SITE et SECONDARY_JOB_SITE ne sont pas des autorités Valet.

### 8.9 Suppression en cascade

Supprimer un champ ou un enclos doit :

- retirer la zone du SavedData ;
- annuler tous les ordres chargés qui la référencent ;
- déplacer ou réinitialiser l’ancre si elle dépend de cette zone ;
- invalider les cibles et chemins runtime ;
- rafraîchir l’interface ;
- ne pas dépendre de la présence des balises.

Supprimer un groupe doit libérer ses membres de la commande persistante et retirer ses tickets de mission.

Supprimer ou tuer un valet doit nettoyer ses réservations, ses caches, son éventuel ticket HOME prouvé et ses états de conversation, sans supprimer les zones mondiales partagées.

### 8.10 Migration depuis les anciennes versions

Les versions historiques utilisaient sept postes et JOB_SITE. La migration doit :

- reconnaître les anciens valets par leur identité ou leur ancienne profession ;
- conserver UUID, nom, inventaire, métier, niveau, XP et atouts ;
- convertir l’ancien home logique en ancre si aucune ancre moderne n’existe ;
- effacer les mémoires de poste ;
- ne pas recréer les anciens blocs, POI ou recettes ;
- conserver les identifiants de champs, enclos, constructions et groupes valides ;
- traiter les ordres pointant vers une zone supprimée comme attente ;
- ne pas exiger que le joueur supprime ses anciens valets.

Le créateur ne voulait pas refaire sa sauvegarde et a explicitement demandé une migration automatique. Une reconstruction qui impose de tuer les valets existants ne respecte pas le contrat.

## 9. Réseau et sécurité serveur

Le client n’est jamais l’autorité.

Pour toute action visant un valet :

- vérifier que l’identifiant désigne bien un villageois chargé ;
- vérifier l’UUID attendu lorsque disponible ;
- vérifier la dimension ;
- vérifier que l’entité est bien un Valet ;
- vérifier que le joueur est à moins de huit blocs ;
- vérifier que l’écran actuellement ouvert correspond au valet ;
- vérifier le métier et la compatibilité de l’ordre ;
- revalider l’existence de la zone, de la construction ou de l’atout ;
- borner noms, indices, listes, tailles et quantités ;
- exécuter la mutation sur le thread serveur.

Pour les groupes et la carte :

- vérifier que le groupe existe ;
- vérifier les coordonnées de destination ;
- ne pas accepter une liste arbitraire de membres fournie par le client ;
- reconstruire la vue serveur après chaque mutation.

Pour le tri :

- refuser si le joueur n’a pas un conteneur compatible réellement ouvert ;
- ne jamais trier un conteneur désigné uniquement par une position cliente.

## 10. Archéologie des régressions

### 10.1 Réponse courte à « quel bout a tout fait partir en vrille ? »

Il n’existe pas une ligne unique. La cause systémique est la coexistence de plusieurs autorités de mouvement et de plusieurs définitions de la maison, du poste et de la zone.

Les responsables concrets les plus importants sont :

- l’ancien module d’excavation de groupe, qui creusait après une immobilité ou un dénivelé et ignorait certains supports vanilla ;
- l’ancien planificateur A* et le déplacement bloc par bloc ;
- l’ancien driver de travail parallèle au Brain ;
- les délais Valet qui interrompaient un chemin vanilla encore valide ;
- la dépendance JOB_SITE et poste, qui rendait des zones invisibles et empêchait le travail ;
- le rappel de groupe persistant, qui continuait à contrôler un fermier arrivé ;
- l’usage de l’ancre géométrique non praticable comme destination ;
- la confusion entre territoire de travail et rayon résidentiel.

### 10.2 Audit 0.3.8

L’audit a corrigé de nombreux problèmes de persistance, réseau, inventaire, ressources et performances. Il a aussi touché un très grand nombre de fichiers. Le problème n’est pas qu’un audit soit mauvais, mais que des changements structurels invisibles aient été validés surtout par compilation et bootstrap.

Leçon : après un nettoyage transversal, rejouer un scénario visible par métier. Un build vert ne prouve ni la marche, ni la porte, ni la zone, ni le dépôt.

### 10.3 Excavation de groupe 0.4.0

Symptôme : envoyé par la carte, le valet descendait, creusait sous un chemin puis remontait.

Causes :

- l’excavation se déclenchait après 40 ticks d’immobilité ou un dénivelé supérieur à trois blocs ;
- elle n’acceptait pas le chemin en terre comme support valide ;
- elle n’essayait pas assez de petits trajets de surface ;
- elle pouvait creuser sous une surface praticable proche.

Correction temporaire : reconnaître les supports, essayer plusieurs pas de surface et interdire le tunnel sous une surface proche.

Décision finale : supprimer toute excavation utilisée uniquement pour se déplacer.

### 10.4 Remplacement de la téléportation en 0.4.2

Le remplacement de la téléportation bloc par bloc par la navigation vanilla était une bonne décision. Les premières intégrations ont cependant créé plusieurs régressions :

- la terre labourée faisait apparaître un premier nœud à une hauteur différente ;
- un lit ou support partiel était refusé comme départ ;
- une porte ouverte était utilisée comme destination finale ;
- le goal Valet annulait le chemin avant le délai du comportement vanilla ;
- le fermier visait directement une culture lointaine au lieu d’une case adjacente ;
- une cible déjà sous ses pieds exigeait malgré tout un chemin.

Ces problèmes ne prouvent pas que vanilla marchait mal. Ils prouvent que la couche Valet annulait ou survalidait ce que vanilla savait faire.

### 10.5 Brain, poste et JOB_SITE en 0.4.3 local

La première refonte Brain conservait encore une dépendance au poste et au JOB_SITE pour relier zones et métiers.

Conséquences observées :

- l’enclos existait mais l’UI le filtrait ;
- l’ordre Éleveur était annulé ;
- le fermier possédait un champ mais restait en retour maison ;
- le clic sur le poste disait qu’aucun valet n’était attribué ;
- remettre les balises ou le poste ne réparait pas l’autorité persistante.

Décision finale : supprimer complètement les sept postes, choisir le métier dans l’UI et faire de la zone sélectionnée l’autorité du Fermier et de l’Éleveur.

### 10.6 Double ordonnancement

L’Éleveur pouvait avoir une cible valide mais ne jamais atteindre l’action finale parce que l’ancien driver, le goal et le Brain se disputaient le mouvement et la priorité.

Décision finale : Brain unique pour WORK, REST et IDLE ; tâche métier custom seulement pour choisir et exécuter l’action dans ce cadre.

### 10.7 Rappel persistant

Symptôme : le Fermier restait immobile alors que son champ et son rôle étaient valides.

Cause : une ancienne commande RECALL continuait de contrôler le valet après son arrivée locale.

Correction : la mission longue distance effectue un relais près de l’ancre, puis rend la main au travail local.

### 10.8 Ancre impraticable et lit refusé

Symptômes : Éleveur bloqué avec retour impossible ; Insigne de lit refusée alors que le lit était proche.

Causes :

- centre géométrique de l’enclos non praticable ;
- lit limité au territoire strict de travail.

Correction : normaliser l’ancre vers une case sûre et séparer le rayon résidentiel de 32 blocs du territoire de travail.

### 10.9 Portes et portillons

Porte : une porte métallique a été ouverte directement par le mod, même si le joueur avait l’impression que le valet utilisait le bouton. La condition acceptait toute porte. La règle a été limitée au bois.

Portillon : la séquence ouverture, refus de navigation, fermeture se répétait plusieurs fois par seconde. Le pathfinding ne savait pas utiliser le bloc du portillon comme destination et le délai de fermeture était trop court. Le passage doit être maintenu ouvert jusqu’au changement de côté.

### 10.10 Fermier et plantations

Problèmes successifs :

- les pommes de terre étaient déposées au lieu d’être conservées ;
- il ne plantait pas depuis un coffre ;
- un coffre vide bloquait les récoltes mûres ;
- il ignorait des ressources prêtes ;
- il bouclait sur la porte ;
- une cible déjà à portée produisait un échec de chemin.

Choix validés : réserve de plantation, recherche aux coins du champ, priorité de plantation manquante, délai après coffre vide, approche adjacente et action locale.

### 10.11 Éleveur et diagnostic nourriture

Le message « aucune source de nourriture » a été interprété à tort comme un coffre vide alors que le joueur confirmait la présence de blé.

Le message était ambigu : coffre non détecté, temporairement exclu, inaccessible, mauvais aliment ou aliment déjà consommé produisaient le même résultat.

Correction : compter les conteneurs détectés, exclus et types d’aliments ; retirer un lot de 16 ; journaliser la quantité réelle.

Leçon : un log doit permettre de distinguer absence de donnée et échec d’accès.

### 10.12 Nage

Symptôme : les valets alternaient entre deux cibles et ne repartaient qu’après avoir été poussés.

Cause : aucune détection de progrès horizontal et cible instable.

Correction : cible stable, récupération après deux secondes, détour borné et écartement des suiveurs.

### 10.13 Carte lente

Cause : lecture synchrone du terrain et soumission d’un rendu par cellule à chaque glisser.

Correction : cache borné, fréquence limitée et texture dynamique unique. Cette décision s’est avérée bonne en jeu.

### 10.14 Quêtes invisibles

Symptôme : le joueur acceptait et livrait, mais aucun texte n’apparaissait.

Cause : couleurs sans canal alpha en Minecraft 26.2.

Correction : couleurs opaques, icônes et bilan persistant affiché.

### 10.15 Comptage de quête

Une API vanilla a été appelée avec un conteneur nul, ce qui provoquait une erreur. La correction consiste à parcourir directement les slots de l’inventaire joueur côté serveur.

### 10.16 Maire dupliqué

Cause : la présence du maire n’était pas suffisamment persistée et dédupliquée entre scans et chargements.

Correction : UUID par dimension, choix canonique et suppression des doublons chargés.

### 10.17 Crash après mort du joueur

Un crash du gestionnaire de tickets de chunks Minecraft 26.2 a été étudié. La pile ne contenait aucune classe Valet et les tickets de mission avaient été retirés quatorze minutes plus tôt.

Le créateur a explicitement refusé un patch du code vanilla tant que le mod n’était pas responsable. Aucun garde anti-réentrance ne doit être ajouté sur la base de cet incident isolé.

### 10.18 Assets visuels

Première erreur de processus : des concepts ont été générés avec l’IA OpenAI alors que le joueur voulait les outils en ligne recommandés, sans que ce changement soit annoncé clairement. Le résultat a été jugé laid.

Choix final : utiliser les services en ligne sous contrôle du joueur pour la direction artistique, puis reconstruire des fichiers Minecraft valides, sans filigrane ni faux damier. Le modèle SuperMaker validé est une référence, pas un atlas UV directement intégrable.

## 11. Choix éprouvés à conserver

- Identité par Insigne, indépendante de la profession et des blocs.
- Métier choisi dans l’interface.
- Suppression complète des postes comme autorité.
- Brain unique.
- Pathfinding vanilla pour le mouvement physique.
- Garde-fous déterministes autour des cibles légales.
- Zones persistantes indépendantes des balises.
- Zone sélectionnée comme ancre mobile du Fermier et de l’Éleveur.
- HOME explicitement assigné et séparé du territoire de travail.
- Approche longue vers une case adjacente, action locale à portée.
- Pas d’excavation de déplacement.
- Missions longue distance par tronçons et tickets temporaires.
- Meneur stable et recalculs bornés.
- Cache de carte borné et texture unique.
- Interface de quêtes commune à J et au maire.
- Serveur autoritaire pour toutes les mutations.
- Logs métier avec cause et résultat.
- Validation par jar réellement installé, pas seulement par fichier de build.

## 12. Décisions abandonnées à ne pas réintroduire

- Sept postes de métier.
- JOB_SITE comme identité ou autorité du métier.
- Tous les enclos comme cible globale.
- Téléportation pour simuler la marche.
- Planificateur A* Valet parallèle.
- Excavation automatique de mission.
- Driver de travail parallèle au Brain.
- Recherche vanilla globale de HOME.
- Union de plusieurs territoires historiques.
- Arrêt de mission fondé seulement sur la distance horizontale.
- Recalcul de chemin à chaque tick.
- Porte métallique ouverte directement.
- Portillon refermé après quelques ticks sans vérifier le passage.
- Chemin global de mille blocs.
- Carte qui relit le monde à chaque cellule et chaque mouvement de souris.
- Correctif de code vanilla sans preuve que Valet est impliqué.
- Assets externes intégrés bruts avec filigrane, damier ou mauvais UV.

## 13. Points encore incomplets ou non prouvés

### 13.1 Gameplay à retester sur le jar local actuel

- Assignation et sommeil avec l’Insigne de lit.
- Éleveur passant un portillon, le refermant puis nourrissant plusieurs adultes.
- Effet exact du plafond 8 dans un grand enclos.
- Fermier après changement de champ et après rappel de groupe.
- Tous les métiers après changement de rôle sans redémarrage.
- Combat intermittent signalé en 0.3.6.
- Construction qui pouvait repartir seulement après une poussée.
- Missions longues avec escaliers, eau, portes et retour local tridimensionnel.

### 13.2 Dette fonctionnelle

- Craft limité essentiellement à la pioche en pierre.
- Cuisine fondée sur une liste fixe.
- Intendant encore largement custom alors qu’un comportement vanilla proche existe.
- Ramassage d’items au sol encore custom.
- Pose de blocs pas encore entièrement alignée sur l’usage vanilla d’un item.
- Combat mêlée encore custom sur plusieurs aspects.
- Pas de télémétrie structurée pour le machine learning.

### 13.3 Dette visuelle

- Le skin Artisan sert encore d’apparence commune à tous les métiers.
- Il manque six skins de métier.
- Il manque les icônes cohérentes des six autres métiers.
- Beaucoup d’ordres et d’atouts utilisent encore du texte ou des symboles.
- Le maire n’a pas encore une identité visuelle complète dédiée.
- Les assets promotionnels publics restent à produire.

### 13.4 Incohérences documentaires

- La description publique du manifeste parle encore de postes alors qu’ils sont supprimés.
- Certaines sections historiques de la cartographie vanilla mentionnent encore le poste comme point de travail.
- La version porte toujours le numéro 0.4.3 alors que le jar local diffère fortement de la release 0.4.3.
- La documentation de `main` décrit la refonte; le tag et l’asset de release v0.4.3 restent antérieurs à cette refonte.

## 14. Stratégie de reconstruction depuis zéro

Cette stratégie impose des résultats observables. Elle ne prescrit pas l’architecture interne exacte.

### Phase 0 — figer le contrat

- Conserver ce rapport comme spécification.
- Exporter une copie du monde de test si le créateur l’accepte ; il a déjà refusé d’en faire une pendant une correction, donc ne pas en faire une condition bloquante.
- Conserver le jar publié, le jar local et leurs hashes.
- Inventorier les identifiants persistants à migrer.
- Définir les invariants de sauvegarde avant toute IA.

Critère de sortie : aucune ambiguïté sur identité, métier, zone, ancre, lit et ordre.

### Phase 1 — persistance minimale

- Identité et métier.
- Ancre et dimension.
- Champ, enclos et groupes par dimension.
- Ordre et options.
- Cycle sauvegarde, déchargement et rechargement.
- Migration d’un ancien valet.

Critère de sortie : les données survivent à fermeture complète, changement de chunk et redémarrage.

### Phase 2 — interface et sécurité réseau

- Recrutement par Insigne.
- Interface serveur autoritaire.
- Changement de métier, nom, ordre et options.
- Inventaire réel.
- Validation de proximité, UUID et dimension.

Critère de sortie : aucun paquet falsifié simple ne peut agir sur un valet distant ou incompatible.

### Phase 3 — déplacement local commun

- Brain unique.
- Ancre et territoire.
- Marche vanilla.
- Supports partiels.
- portes bois et métal ;
- portillons ;
- eau de secours ;
- timeout et replanification ;
- aucun bris de bloc ;
- aucun teleport.

Critère de sortie : un valet peut quitter une maison, traverser un champ, franchir un portillon et revenir sans boucle.

### Phase 4 — Fermier comme métier de référence

Le Fermier est le meilleur premier test car il combine zone, marche, action locale, inventaire et coffre.

- Enregistrer puis retirer les balises.
- Récolter les cinq cultures.
- Labourer sous neige.
- Planter depuis inventaire et coffre.
- Conserver la réserve.
- Supprimer le champ.

Critère de sortie : test complet validé en jeu et dans le log sur une sauvegarde réelle.

### Phase 5 — Éleveur

- Enclos tridimensionnel.
- Coffre en marge.
- Portillon.
- Nourrissage et BreedGoal vanilla.
- Outils, œufs, lait et surplus.
- Suppression d’enclos.

Critère de sortie : plusieurs adultes sont nourris avec un seul retrait de lot, le portillon est refermé et le plafond est respecté.

### Phase 6 — Artisan et logistique

- Minage et bois sans destruction de passage.
- Craft par recettes chargées.
- Blueprints et matériaux.
- Dépôt et reprise.

### Phase 7 — Combat, magie, cuisine et intendance

Traiter un métier à la fois. Ne pas réactiver un super-valet générique.

### Phase 8 — groupes et carte

- Groupes persistants.
- Modes locaux.
- Repère et tronçons longue distance.
- Tickets temporaires.
- Nage et récupération.
- Carte performante.

### Phase 9 — maire, quêtes et finition

- Maire unique.
- Quêtes persistantes.
- UI opaque et commune.
- Assets professionnels par métier.

## 15. Matrice de validation indispensable

### Identité et migration

- Recruter un villageois adulte.
- Refuser un enfant.
- Recharger le chunk.
- Fermer et rouvrir le monde.
- Casser tous les anciens postes.
- Charger un valet d’une ancienne version.
- Vérifier UUID, nom, métier, inventaire, niveau et ordre.

### Zones

- Poser deux balises puis les casser.
- Redémarrer et retrouver la zone.
- Sélectionner une autre zone et vérifier le déplacement de l’ancre.
- Supprimer la zone et vérifier l’annulation de l’ordre.
- Tester une zone dont le centre est clôture, eau ou bloc non praticable.

### Lit

- Demander l’Insigne de lit.
- Refuser un item lié à un autre valet.
- Refuser un lit joueur.
- Refuser un lit occupé.
- Refuser un lit hors rayon.
- Accepter un lit à 26 ou 32 blocs selon géométrie valide.
- Recharger le monde et vérifier le même HOME.
- Tuer le valet et vérifier la libération du ticket exact.

### Mouvement

- Terre pleine, chemin en terre, terre labourée, escalier et lit comme départ.
- Porte bois ouverte puis fermée.
- Porte fer avec bouton.
- Portillon fermé dans un enclos.
- Dénivelé d’un bloc.
- grande chute ;
- eau, lave et surfaces dangereuses ;
- cible impossible ;
- interruption par combat ;
- reprise après interruption ;
- aucun bloc cassé pour se déplacer.

### Fermier

- Chaque culture séparément.
- Toutes cultures ensemble.
- Sol vierge avec neige.
- Graines dans inventaire.
- Graines seulement dans coffre près d’un coin.
- Coffre vide.
- Inventaire plein.
- Porte entre maison et champ.
- Suppression du champ en cours de tâche.

### Éleveur

- Vaches, moutons, cochons et poules.
- Enclos mixte.
- Coffre dedans et juste dehors.
- Blé visible mais coffre temporairement inaccessible.
- Plusieurs adultes prêts.
- Plafond atteint avec abattage désactivé.
- Plafond atteint avec abattage activé.
- Animaux amoureux.
- Portillon.
- Suppression d’enclos.
- Prise et dépôt via l’inventaire interactif.

### Groupes

- Un valet dans un seul groupe.
- Groupe supprimé pendant mission.
- Suivre, garde, attaque, rappel et repère.
- Non-combattant avec ordre de combat.
- Mission de mille blocs.
- Chunk déchargé.
- Eau large.
- Meneur bloqué et récupération.
- Rappel vers une ancre à un autre niveau vertical.
- Retour du métier après rappel.

### Sauvegarde et arrêt

- Sauvegarder pendant chaque tâche.
- Décharger le valet pendant un chemin.
- Arrêter le serveur avec un portillon ouvert.
- Redémarrer avec une cible supprimée.
- Transférer une entité entre dimensions si le jeu le permet.
- Vérifier qu’aucun cache runtime orphelin n’agit après redémarrage.

## 16. Contrat d’observabilité

Un log utile doit répondre à quatre questions :

- quel valet ?
- quelle intention ?
- quelle cible ?
- quel résultat ou motif de refus ?

Familles d’événements à conserver :

- activité Brain et changement de priorité ;
- démarrage, progression, arrivée, blocage et refus de navigation ;
- assignation ou refus HOME avec motif ;
- ouverture et fermeture de portillon ;
- ordre de groupe et relais de rappel ;
- cible métier réservée, atteinte, perdue ou temporairement exclue ;
- contenu de coffre réellement déplacé ;
- action finale, par exemple planté, nourri, construit ou crafté ;
- plafond ou cooldown empêchant une action.

Les logs doivent être temporisés. Une même cible impossible ne doit pas remplir le fichier plusieurs fois par seconde.

## 17. Build, livraison et publication

Environnement attendu :

- Minecraft 26.2 ;
- Fabric Loader 0.19.3 ;
- Fabric API 0.154.2+26.2 ;
- Fabric Loom 1.17.14 ;
- Java 25.

Après toute modification Java ou ressource qui change le jar :

- compiler ;
- exécuter les vérifications d’architecture ;
- produire le jar ;
- supprimer les anciens jars Valet du dossier mods ;
- installer le nouveau jar ;
- vérifier que build et installation ont le même hash ;
- lancer un bootstrap serveur ;
- lancer le scénario de jeu pertinent ;
- relire le log ;
- mettre à jour README, changelog, registre de jar et note de version.

La vérification d’architecture doit refuser :

- toute réapparition de téléportation dans le runtime ;
- l’ancien driver ;
- l’ancien planificateur ;
- l’excavation de déplacement ;
- une navigation custom héritée de la navigation vanilla ;
- un évaluateur de nœuds custom pour les valets ;
- un bris de bloc dans les couches de déplacement ;
- toute écriture de JOB_SITE comme autorité Valet.

Ne pas pousser GitHub sans demande explicite. Lorsqu’une publication est demandée, elle comprend commit, tag, push, release, jar, notes complètes et vérification du hash distant.

## 18. Priorités recommandées après reprise

1. Conserver l’état actuel dans un commit Git dédié, sans le mélanger à une nouvelle feature.
2. Donner un nouveau numéro à la refonte locale pour ne plus avoir deux jars 0.4.3 différents.
3. Corriger la description du manifeste et les notes encore liées aux postes.
4. Exécuter la matrice de gameplay sur le jar local 21705B.
5. Stabiliser Éleveur, HOME et rappel avant toute nouvelle profession.
6. Remplacer craft et cuisine fixes par les recettes chargées.
7. Rapprocher Intendant et ramassage du vanilla.
8. Ajouter une télémétrie structurée avant toute expérimentation ML.
9. Produire les six autres identités visuelles de métier sous contrôle du joueur.

## 19. Sources internes utilisées pour ce rapport

Le rapport a été construit à partir de :

- l’état actuel complet du dépôt ;
- README, changelog, registre des jars, notes importantes, plan et suivi ;
- toutes les notes de version disponibles ;
- la cartographie vanilla ;
- l’historique Git et les tags ;
- le diff entre v0.4.3 publié et l’état local ;
- les sources de persistance, Brain, tâches, groupes, réseau et interfaces ;
- le dernier jar local et le dernier log ;
- les conversations Codex sur l’audit, la carte, les groupes, le maire, les déplacements, le Fermier, l’Éleveur, les portes, les quêtes et les assets ;
- les résumés de sessions antérieures conservant les décisions et échecs importants.

## 20. Contrat final de fidélité

Une reconstruction peut changer entièrement l’organisation interne, les classes, les algorithmes ou la bibliothèque d’interface. Elle est fidèle uniquement si elle conserve les invariants suivants :

- le joueur recrute par Insigne ;
- le métier ne dépend d’aucun poste ;
- la zone choisie gouverne le travail spatial ;
- le lit est explicite, local et séparé du travail ;
- le Brain a une seule autorité ;
- le mouvement est physique et vanilla autant que possible ;
- aucun déplacement ne creuse, ne casse ou ne téléporte ;
- les missions lointaines restent bornées et replanifiées ;
- la persistance survit aux cycles réels de Minecraft ;
- les anciens valets sont migrés sans être sacrifiés ;
- chaque action est visible, contrôlable, sécurisée côté serveur et diagnostiquable ;
- aucune nouvelle fonctionnalité ne doit réintroduire les postes, l’excavation de déplacement ou les autorités concurrentes.

Ce contrat prime sur la reproduction littérale de l’implémentation actuelle.
