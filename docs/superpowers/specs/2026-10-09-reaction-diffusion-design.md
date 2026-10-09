# Démo réaction-diffusion — spécification

Date : 2026-10-09
Statut : conception conversationnelle approuvée ; spécification écrite à relire.

## Intention et périmètre

La demande porte sur de nouvelles démos Graphiks WebGPU à la fois visuellement
impressionnantes et pédagogiques. La sélection approuvée comprend réaction-diffusion,
ville instanciée et laboratoire de filtres. Cette spécification ne couvre que la
première : une simulation Gray–Scott interactive, avec un mode vitrine et un panneau
pédagogique facultatif.

Le succès se mesure par des motifs distincts pour les trois préréglages, une interaction
souris/tactile utilisable, des explications cohérentes avec les passes réellement exécutées
et des vérifications GPU sur les deux runners JS et Wasm JS. La démo ne constitue pas
un benchmark ni une preuve de conformité de toute l'API.

Hors périmètre : les deux autres démos, import d'image, éditeur de shaders, grille
redimensionnable, framework générique de scènes, mesure ou classement de performances.

## Choix d'architecture

Choix retenu : compute shaders et deux textures ping-pong. Un calcul sur buffers serait
possible mais moins naturel pour un champ 2D ; une simulation par passes de rendu serait
possible mais montrerait moins directement les textures de stockage.

Suivre les frontières de `ParticleScene` :

- `suite-demos/src/commonMain/kotlin/org/graphiks/webgpu/suite/demos/reactiondiffusion/` :
  paramètres, préréglages, état initial, WGSL et `ReactionDiffusionScene` portable.
- `suite-browser/src/commonMain/kotlin/org/graphiks/webgpu/suite/browser/demos/` :
  page, interactions, animation et vérifications GPU de la nouvelle démo.
- `suite-browser/.../Main.kt` : nouvelles routes explicites.
- `site/demos/` : nouvelle entrée de galerie, présentation et liens JS/Wasm.
- `tools/run-browser.mjs` : collecte des vérifications des deux démos.
- Ressources EN/FR et `docs/running.md` : contrôles, explications et procédure de lancement.

Ne pas modifier le contrat public de `ParticleScene`. Réutiliser les helpers existants
lorsqu'ils sont adaptés, sans déplacer globalement le code des particules.

## Modèle numérique

Grille fixe de 256 × 256 cellules, deux concentrations A et B par cellule.
Utiliser deux textures `rgba32float` avec les usages de stockage, échantillonnage,
copie source et copie destination. R stocke A, G stocke B ; B et alpha restent
respectivement 0 et 1. Lire avec `textureLoad` sans sampler ni filtrage flottant,
et écrire avec `textureStore`. Aucune fonctionnalité GPU optionnelle n'est requise.

Pour chaque cellule, effectuer une étape d'Euler explicite, avec dt = 1 :

```text
reaction = A * B * B
A' = A + (DA * laplacien(A) - reaction + feed * (1 - A)) * dt
B' = B + (DB * laplacien(B) + reaction - (kill + feed) * B) * dt
```

DA = 1 et DB = 0,5. Le laplacien utilise le centre de poids -1, les quatre
voisins orthogonaux de poids 0,2 et les quatre diagonales de poids 0,05.
Les coordonnées des voisins sont périodiques. Borner les concentrations résultantes
à [0, 1]. Les paramètres CPU doivent être finis avant tout envoi au GPU.

Préréglages initiaux :

| Nom | Feed | Kill |
| --- | --- | --- |
| Corail | 0,0545 | 0,062 |
| Labyrinthe | 0,029 | 0,057 |
| Taches | 0,0367 | 0,0649 |

Ces noms décrivent l'intention visuelle, non une garantie mathématique de forme.
La vérification visuelle doit confirmer leur distinction avant livraison.
Les curseurs exposent feed et kill entre 0 et 0,1.

L'état initial est produit sur CPU de manière déterministe : fond A = 1, B = 0,
et neuf carrés de 8 × 8 cellules, centrés sur les combinaisons des coordonnées
64, 128 et 192, avec A = 0,5 et B = 0,25. Réinitialiser recharge les deux textures
et restaure la première comme état courant.

## Passes et contrat de la scène

La scène reçoit le device et le format de rendu. Elle possède ses textures, vues,
buffers, bind groups et pipelines. Elle emprunte l'encoder et la vue cible du runner,
encode les commandes, mais ne les soumet pas et ne ferme jamais le device.
La fermeture est idempotente ; les créations partielles sont libérées en cas d'échec.

Une frame comporte, dans l'ordre :

1. Si une interaction est en attente, une passe compute copie l'état courant vers
   l'autre texture en appliquant un disque de pinceau ; échange des textures.
2. De zéro à seize étapes de simulation ; après chaque étape, échange des textures.
3. Une passe de rendu plein écran lit le dernier état et applique le mode d'affichage.

Les bind groups A → B et B → A sont créés à l'avance. Les uniformes sont invariants
pour les étapes d'une même soumission. Le pinceau dispose de paramètres séparés,
afin d'éviter de réécrire le même buffer entre des passes encodées avant soumission.
Un appel d'encodage correspond à une soumission ; documenter cette contrainte.

La vitesse choisit 1 à 16 étapes par frame, avec 8 par défaut. Ce n'est pas une vitesse
en secondes physiques : l'évolution par seconde dépend de la cadence du navigateur.
Pause encode zéro étape ; « Un pas » encode exactement une étape tout en restant
en pause. La peinture reste disponible en pause sans faire avancer la réaction.

Le runner garde une seule boucle d'animation. Changer les paramètres ou la palette
ne recrée pas la scène. Changer de préréglage applique ses paramètres et réinitialise
l'état ; reset garde les paramètres et la palette actuels.

Pour le readback de vérification, exposer une opération qui encode une copie de l'état
courant vers un buffer fourni par le consommateur. Ne pas transférer la propriété
des textures ni laisser le consommateur dépendre de leur alternance interne.

## Interface et interactions

Canvas carré responsive, avec taille d'affichage indépendante de la grille.
Contrôles accessibles et libellés EN/FR : préréglage, pause/reprise, un pas,
réinitialisation, vitesse, feed, kill, palette et ouverture du panneau pédagogique.
« Un pas » n'est activé qu'en pause.

Le pinceau a un rayon fixe de six cellules et affecte A = 0,5, B = 1 à l'intérieur
du disque, sans modifier les autres cellules. Un seul pointeur actif est capturé.
Convertir ses coordonnées à partir du rectangle réel du canvas, avec origine en haut
à gauche. Les cellules du disque suivent les mêmes frontières périodiques que la
simulation. Au plus une position, la plus récente, est consommée par frame ; aucune
interpolation de trait n'est prévue dans cette version. Le défilement tactile est
neutralisé sur le canvas, pas sur la page entière.

Trois palettes : océan (bleu sombre → cyan → blanc), braise (noir → rouge → jaune)
et niveaux de gris. Chaque palette est une interpolation déterministe selon B ;
elle ne modifie pas les concentrations.

## Mode pédagogique

Le panneau est masqué par défaut et explique :

- les substances A et B, diffusion et réaction ;
- le rôle de feed et kill et la nature des préréglages ;
- le diagramme lecture A → compute → écriture B → échange ;
- la séparation entre état simulé et coloration ;
- le nombre d'étapes par frame, sans promettre une vitesse temporelle fixe.

Deux modes supplémentaires affichent A ou B en niveaux de gris, indépendamment
de la palette. Montrer les sources WGSL commentées utilisées par les pipelines,
sans copie divergente et sans édition. Quand le pinceau est actif, l'explication
indique sa passe supplémentaire.

## Routes, rapports et publication

Routes :

- `?demo=reaction-diffusion&lang=en|fr` : interface interactive.
- `?demo=reaction-diffusion&verify=1` : vérifications GPU et publication de
  `globalThis.graphiksDemoReport` selon le schéma `DemoReport` existant.

Conserver les routes des particules et le rejet explicite des routes inconnues.
Le collecteur `--demo-check` lance les deux routes de vérification, contrôle les
identifiants attendus de chacune et agrège leurs cas dans le rapport de démos
existant `build/reports/demos-<target>.json`. Ne pas écraser une démo avec l'autre,
modifier les rapports acid tests/benchmarks, ni accepter une liste de cas incomplète.
Toute erreur fatale ou vérification échouée produit une sortie non nulle.

La galerie conserve la carte des particules et ajoute la nouvelle carte avec
liens JS/Wasm et sources. Adapter les compteurs ou listes de cas éventuellement
codés en dur dans le site et ses outils. Les résultats publiés restent distingués
des lancements interactifs locaux.

## Erreurs et cycle de vie

Vérifier les limites nécessaires avant les allocations. En cas d'absence de WebGPU,
d'adapter, de limites insuffisantes ou d'échec de création, afficher un message
explicite et ne pas démarrer l'animation. Aucune simulation CPU de remplacement.

Une perte de device arrête la boucle, désactive les contrôles GPU et propose de
recharger la page. À la fermeture de la page, arrêter la boucle et supprimer les
listeners ; libérer les ressources possédées. Toute donnée invalide est rejetée
avant écriture GPU. Les vérifications utilisent les error scopes comme les tests
des particules et ne transforment jamais une exécution indisponible en succès.

## Vérification et critères d'acceptation

Tests CPU :

- forme et valeurs de l'état initial, reproductibilité et validation des paramètres ;
- étape CPU de référence du même modèle, utilisée pour comparer les résultats GPU ;
- sélection des préréglages et validation du nombre d'étapes.

Trois scénarios GPU avec identifiants stables :

1. `reaction-diffusion.compute-render-readback` : comparer une étape GPU à la
   référence CPU sur un état reproductible (tolérance absolue 0,0001 par concentration),
   vérifier valeurs finies dans [0, 1] et pixels rendus représentatifs en niveaux de gris.
2. `reaction-diffusion.pause-step-reset` : pause conserve l'état, un pas le change,
   plusieurs étapes correspondent à la référence, reset restitue les données initiales.
3. `reaction-diffusion.brush-boundaries` : peindre en pause ne simule pas de réaction,
   le disque modifie les cellules attendues, y compris à une frontière périodique,
   et conserve les cellules extérieures.

Les copies texture → buffer respectent l'alignement des lignes WebGPU. Comparer les
concentrations avec une tolérance numérique, pas avec des captures strictement identiques.
Les tests de rendu utilisent une cible hors écran et des assertions de pixels adaptées
à la quantification du format de sortie.

Vérifications d'intégration : distributions JS et Wasm JS, collecte stricte des cinq
cas de démos (deux existants, trois nouveaux), tests des outils concernés et absence
de régression des routes existantes. Compiler les modules portables sur les cibles
disponibles sans prétendre avoir exécuté le GPU sur les cibles natives.

Contrôle navigateur : préréglages visuellement distincts après évolution, pinceau,
pause/un pas/reset, palettes, affichage A/B, panneau pédagogique, EN/FR et disposition
mobile/desktop. Ne pas annoncer une compatibilité tactile complète sur la seule base
d'un redimensionnement desktop ; vérifier les événements tactiles ou pointer correspondants.

## Suivi de conception

- [x] Intention et sélection des démos établies.
- [x] Approches et conception présentées dans la conversation.
- [x] Conception conversationnelle approuvée.
- [x] Spécification écrite.
- [x] Auto-relecture : périmètre, cohérence, décisions numériques et rapports précisés.
- [ ] Spécification écrite relue et approuvée par l'utilisateur.
- [ ] Plan d'implémentation rédigé après cette approbation.
- [ ] Plan relu et méthode d'exécution choisie.
