# Ville instanciée cyberpunk — spécification

Date : 2026-10-10
Statut : spécification écrite approuvée par l'utilisateur ; plan rédigé, à relire.

## Intention et périmètre

Ajouter la deuxième nouvelle démo retenue : une ville 3D nocturne, visuellement
marquante et pédagogique. L'utilisateur a choisi l'ambiance cyberpunk et approuvé
les façades sombres, fenêtres cyan/magenta, rues lumineuses, caméra orbitale
souris/tactile, rotation automatique désactivable, densité et hauteur réglables,
génération reproductible et panneau EN/FR sur l'instancing. La démo rejoint la
galerie et le sélecteur des démos existantes sur JS et Wasm JS.

Hors périmètre : trafic, personnages, bloom ou autres post-traitements, ombres
projetées, import de modèles/textures, sélection individuelle de bâtiments,
éditeur de shaders, moteur 3D générique et laboratoire de filtres. Aucun benchmark
ni gain de performance chiffré ne sera revendiqué.

Le succès est une ville lisible en profondeur, aux fenêtres et rues clairement
lumineuses sans bloom, manipulable à la souris et au toucher, avec des explications
conformes aux vrais appels GPU et des vérifications exécutées sur les deux runners.

## Architecture et alternatives

Choix retenu : maillage de cube indexé partagé, buffer de données par bâtiment
lu avec `GPUVertexStepMode.Instance`, et passe de rendu avec test de profondeur.
Un seul appel `drawIndexed` dessine tous les bâtiments. Un second appel dessine
le sol et ses rues ; le panneau ne doit donc pas annoncer « toute la scène en
un seul draw call ».

Alternatives écartées : un draw call par bâtiment rendrait moins directement
visible l'intérêt de l'instancing ; génération/culling GPU par compute et dessin
indirect ajouteraient des mécanismes étrangers à cette première démonstration.
La génération CPU déterministe ne tourne qu'au chargement ou au changement de
densité, jamais par bâtiment à chaque frame. Pas de compute dans cette démo.

Frontières :

- `suite-demos/.../demos/instancedcity/` : génération, validation, géométrie,
  mathématiques caméra, WGSL et `InstancedCityScene`, sans DOM ni animation navigateur.
- `suite-browser/.../browser/demos/` : page, vue, contrôles, événements pointer,
  boucle d'animation et vérifications GPU de la ville.
- `Main.kt` : routes interactives et vérification explicites.
- `DemoNavigation.kt` et `site/demos/app.js` : troisième choix et troisième carte.
- `tools/demo-reports.mjs` : troisième route avec ses identifiants attendus.
- Ressources EN/FR, `docs/running.md`, preuves bilingues et changelog : instructions
  et résultats réels, sans étendre artificiellement la couverture acid.

Ne pas modifier les contrats publics de `ParticleScene` ou `ReactionDiffusionScene`.
Réutiliser les helpers DOM, rapports, acquisition et assertions existants lorsqu'ils
sont adaptés, sans refonte générale des deux démos précédentes.

## Données et génération

Trois densités : 256, 1024 et 4096 bâtiments, soit des grilles de 16 × 16,
32 × 32 et 64 × 64. Valeur par défaut : 4096, ou la plus grande option admise par
les limites du device. Si aucune option n'est admise, afficher une erreur.

Les trois grilles occupent le même secteur de 128 × 128 unités monde, centré en
(0, 0) sur le plan XZ. Pas de grille = 128 / côté ; chaque bâtiment est centré
dans sa cellule. Largeur et profondeur sont chacune entre 45 % et 75 % du pas.
Les espaces entre cellules restent des rues. Hauteurs de base entre 2 et 14 unités.
La distribution privilégie des tours vers le centre, sans dépasser ces bornes.

Chaque instance contient huit Float32 (stride de 32 octets) :

```text
[centreX, centreZ, largeur, profondeur, hauteur, couleur, motif, 0]
```

`couleur` vaut 0 (cyan) ou 1 (magenta). `motif` est un entier compris entre 0 et
65535, représentable exactement en Float32, qui stabilise les fenêtres allumées.
Le padding vaut 0. La génération utilise un PRNG explicite portable à arithmétique
entière non signée, avec une graine fixe `1u`, pas le générateur aléatoire de plateforme.
Même densité et même graine produisent les mêmes données sur toutes les cibles.

Toutes les données sont validées avant allocation ou écriture GPU : tableau non
vide, taille multiple de huit, nombre d'instances autorisé par les limites,
valeurs finies, positions dans le secteur, dimensions positives et bâtiments
intégralement dans le secteur, hauteur dans les bornes, couleur/motif entiers dans
leurs domaines et padding nul. Les fixtures de vérification peuvent disposer des
bâtiments librement dans ces bornes sans suivre la grille du générateur.

Le curseur de hauteur applique un facteur uniforme entre 0,5 et 2,0, défaut 1,0.
Il ne régénère ni ne réécrit le buffer des instances. Le réglage de densité génère
et prépare une nouvelle scène ; ne libérer l'ancienne qu'après préparation réussie.
Une erreur de remplacement garde l'ancienne scène utilisable et affiche un diagnostic.

## Géométrie, rendu et style

Cube partagé : 24 sommets (quatre par face pour conserver normales et UV),
36 indices UInt16, dimensions locales X/Z dans [-0,5 ; 0,5] et Y dans [0 ; 1].
Chaque sommet contient position XYZ, normale XYZ et UV, soit huit Float32.
Les indices et le sens des faces sont cohérents avec le back-face culling.

Le vertex shader applique translation et dimensions de l'instance, puis la
matrice vue-projection. La hauteur tient compte du facteur uniforme. Les normales
des faces restent normalisées et cohérentes avec ce cube sans rotation.

Le fragment shader distingue toit et façades : surfaces bleu-violet sombres,
éclairage ambiant plus une direction de lumière fixe ; fenêtres procédurales
cyan/magenta déterminées par les UV, les dimensions et le motif d'instance.
Le toit ne reçoit pas de fausses fenêtres. Les fenêtres émettent une couleur
vive, sans halos, éclairage différé ni texture externe. Leur disposition est
statique : aucune animation des fenêtres n'est requise.

Le sol est une géométrie plane couvrant le secteur et une marge sombre. Son shader
dessine les rues et lignes lumineuses à partir du pas de grille, en accord avec
la disposition réelle des bâtiments. Le rendu distingue chaussées et façades.
Utiliser l'anti-aliasing analytique des motifs si nécessaire ; pas de MSAA dans
cette version. Les valeurs émises sont adaptées à la sortie unorm, pas présentées
comme du HDR. Fond nocturne uni ; pas de skybox ni de brouillard requis.

La passe efface couleur et profondeur, dessine le sol puis tous les bâtiments.
Utiliser `Depth24Plus`, écriture de profondeur activée, comparaison `Less`, une
texture de profondeur à la taille physique de la cible et un sample count de 1.
Aucune fonctionnalité GPU optionnelle ne doit être requise.

## Caméra et interactions

Repère droit, axe Y vers le haut, centre d'orbite fixe (0, 8, 0). Projection
perspective, champ vertical 50°, near 0,1, far 1024. La matrice respecte la
convention WebGPU (profondeur projetée dans [0 ; 1]) et le ratio width / height.
Les mathématiques portables sont testées indépendamment du rendu.

Caméra initiale : azimut 45°, élévation 35°, distance 180 unités. Distance bornée
à [100 ; 300], élévation à [15° ; 75°], azimut périodique. Ces bornes maintiennent
la caméra au-dessus des plus hautes tours, y compris avec le facteur 2.

- Glisser souris/tactile : changer azimut et élévation, sens et sensibilité stables
  en coordonnées CSS indépendamment du device pixel ratio.
- Molette sur canvas : zoom borné, avec normalisation de `deltaMode` ; ne pas
  neutraliser le scroll en dehors du canvas.
- Curseur de distance accessible : zoom clavier et tactile, sans imposer le pinch.
- Un seul pointeur capturé ; second pointeur ignoré. Pointerup, pointercancel,
  lostpointercapture, perte de device et fermeture nettoient la capture.
- `touch-action: none` sur le canvas uniquement. Pas de pan ni de pinch dans
  cette première version ; l'interface indique le glisser et le curseur de distance.
- Rotation automatique active par défaut, 0,1 radian/seconde, désactivable. Pendant
  un glisser, elle est suspendue ; après relâchement, elle reprend si activée.
- Respecter `prefers-reduced-motion` au premier chargement en désactivant par défaut
  la rotation ; une activation explicite reste possible.

La rotation utilise le temps RAF, avec delta borné à 0,05 seconde et remise à
zéro du timestamp à la reprise ; pas de grand saut après un onglet masqué.
Quand la rotation est désactivée, ne rendre qu'au changement d'un contrôle,
interaction ou redimensionnement. Une seule boucle RAF est active.

Un bouton « Réinitialiser la vue » restaure azimut, élévation et distance,
sans modifier densité, facteur de hauteur ou préférence de rotation. La génération
reste reproductible sans bouton « Aléatoire ».

## Contrat et propriété de la scène

La scène reçoit un device emprunté, le format couleur et les données d'instances.
Elle possède vertex/index/instance/uniform buffers, layouts, groupes, shaders ou
pipelines et profondeur/vues qu'elle crée. Elle emprunte encoder et vue couleur
à chaque frame, encode sans soumettre, et ne ferme jamais le device.

Une opération d'encodage reçoit taille, caméra, facteur de hauteur et pas de grille.
Les paramètres sont finis et bornés ; la taille est positive. Un appel correspond
à une soumission avant la frame suivante, puisque les uniformes sont des écritures
de queue et non des snapshots capturés dans les commandes.

Avant la première frame ou un redimensionnement, créer une profondeur adaptée.
Remplacer et fermer proprement l'ancienne profondeur après création réussie.
Ne pas accumuler des cibles ou créer de nouveaux buffers/pipelines à chaque frame.
Les limites texture, buffer, vertex stride/attributes/buffers et uniform binding
sont contrôlées avant allocation. Fermeture idempotente, rejet des opérations
sur scène fermée et libération en ordre inverse des créations partielles.

## Interface, pédagogie et navigation

Canvas responsive au ratio 16:10, contrôles accessibles avec noms EN/FR : densité,
facteur de hauteur, rotation automatique, distance et réinitialisation de la vue.
Le sélecteur commun propose Particules, Réaction-diffusion et Ville cyberpunk,
en conservant langue et chemin JS/Wasm lors d'une navigation complète.

Panneau pédagogique fermé par défaut :

- un cube partagé + buffer par instance → un dessin indexé pour les bâtiments ;
- différence entre données par sommet et données par instance ;
- transformations, caméra et projection ;
- profondeur et occlusion, distinctes de l'ordre des bâtiments dans le buffer ;
- fenêtres et routes procédurales sans textures externes ni bloom ;
- deux draws au total par frame : sol et bâtiments, et 12 triangles par bâtiment
  plus 2 pour le sol. Ces nombres viennent de la géométrie réellement soumise,
  pas d'une estimation de travail exécuté ou d'un compteur de FPS.

Afficher les sources WGSL commentées utilisées par les pipelines, sans copie
divergente ni édition. La démo explique l'économie de soumissions par l'instancing,
sans comparer arbitrairement les performances d'autres techniques.

## Routes, galerie et rapports

- `?demo=instanced-city&lang=en|fr` : interface.
- `?demo=instanced-city&verify=1` : vérifications GPU et `graphiksDemoReport`.

La galerie ajoute une troisième carte avec lancements JS/Wasm et liens vers les
sources portables. Conserver cartes et routes précédentes. Le collecteur existant
visite trois routes dans des pages neuves, valide les identités/schémas et tous
les ids, et agrège huit cas dans `demos-<target>.json` : cinq existants + trois
nouveaux. Toute indisponibilité, liste incomplète, erreur de page ou erreur de
validation est un échec explicite ; conserver les résultats partiels réels.
Ne pas modifier les rapports acid/benchmarks ou augmenter leurs compteurs.

## Erreurs et cycle de vie

Établir le garde de fermeture avant le premier chargement asynchrone, y compris
les textes. Toute acquisition tardive après fermeture doit être abandonnée et
libérée, sans recréer du DOM ni démarrer RAF. Une absence de WebGPU ou de limites
adéquates donne une erreur visible ; aucun remplacement CPU du rendu.

À la perte du device : arrêter RAF, nettoyer capture et ressources, désactiver
les contrôles GPU et proposer rechargement. Le sélecteur de démos reste utilisable.
À pagehide : annuler callbacks/listeners/tâches et fermer les ressources possédées.

Ne pas acquérir de texture de surface ou avancer la rotation pour un canvas
masqué (display/visibility), déconnecté ou de dimensions rendues nulles. Une
interaction en attente ne doit pas être perdue ; la prochaine frame visible
utilise le dernier état valide. Les vérifications encadrent le travail GPU par
des error scopes et attendent les résultats avant de déclarer le succès.

## Vérification et critères d'acceptation

Tests CPU rouges puis verts : géométrie/indices/normales, stride et validation
des instances, limites, génération déterministe pour les trois densités, matrices
vue/projection avec points attendus calculés indépendamment, ratio de canvas,
bornes de caméra, événements glisser/molette, reduced motion et deltas RAF.

Trois scénarios GPU, hors écran RGBA8Unorm, sans dépendre d'une capture exacte :

1. `instanced-city.instances-render-readback` : petite fixture contrôlée de deux
   bâtiments distincts ; assert pixels intérieurs aux silhouettes et couleurs
   distinctes dans une vue de vérification déterministe. Vérifier que le second
   bâtiment est dessiné et utilise ses dimensions/données, pas celles du premier.
   La vue de vérification est une option interne de shader utilisée uniquement
   par les checks, avec couleurs constantes par instance plutôt que fenêtres.
2. `instanced-city.depth-occlusion` : fixture de deux bâtiments qui se recouvrent
   en projection ; dessiner la même géométrie dans deux ordres d'instances. Le
   pixel intérieur de recouvrement conserve la couleur du bâtiment le plus proche.
   Tester avec un point dont la profondeur et l'appartenance aux silhouettes sont
   établies indépendamment ; ne pas tester seulement l'égalité de deux fonds vides.
3. `instanced-city.camera-resize-lifecycle` : changer caméra et ratio de cible,
   vérifier des pixels représentatifs attendus et absence d'erreurs de validation,
   restaurer la vue initiale et retrouver les pixels initiaux à tolérance unorm.
   Vérifier le rejet des entrées invalides avant encodage, close idempotent,
   scène fermée rejetée et device emprunté encore utilisable.

Les modes de vérification ne sont pas des contrôles de l'interface utilisateur.
Échantillons de pixels éloignés des arêtes, assertions avec tolérance adaptée
à la sortie unorm ; copies texture→buffer avec alignement WebGPU des lignes.
Les fixtures utilisent le même maillage, layouts et pipelines que la vraie scène.

Tests navigateur JS et Wasm : sélecteur à trois choix avec aller-retour et langue,
contrôles, reset vue sans changement de densité, génération reproductible, vraie
interaction souris et tactile, capture/annulation/second pointeur, reduced motion,
canvas masqué, redimensionnement, perte du device et fermeture pendant chargement
des textes/acquisition. Vérifier WGSL affiché, noms accessibles et absence de
débordement sur desktop/mobile. Contrôle visuel des trois densités et hauteur
min/max : rues et fenêtres lisibles, pas de façades transparentes ou de tours
traversant le sol. Éviter une revendication de compatibilité tactile basée
seulement sur un viewport mobile.

Campagne finale : `./gradlew check`, distributions JS/Wasm, huit checks GPU de
démos, régressions acid, outils/galerie, API Dokka et MkDocs strict conformément
à CONTRIBUTING. Compiler et tester le portable sur les cibles disponibles en
distinguant clairement compilation/tests CPU natifs d'exécution GPU native.
Lancer la nouvelle démo localement, inspecter son rendu, consigner uniquement
des résultats effectivement exécutés et garder les sorties générées hors Git.

## Livraison et dépendances

Branche dédiée `feat/instanced-cyberpunk-city`, démarrée au commit `203f040` de
la branche réaction-diffusion. La PR #141 est encore ouverte : cette ville ne
doit pas être poussée dans sa branche ni présentée comme une modification de
cette PR. Une future PR ville devra isoler ses commits une fois la dépendance
fusionnée ou suivant une stratégie explicitement convenue avec l'utilisateur.
Toute issue ou PR effectivement travaillée sera liée à la session dès sa création.

La spécification décrit le choix technique, pas une implémentation déjà faite.
Les valeurs esthétiques pourront être ajustées pendant la vérification visuelle
sans changer le contrat, les bornes ou les tests ; tout élargissement de périmètre
doit être approuvé. Une issue de coordination sera ouverte avant l'implémentation
si requise par la contribution ; ne pas créer rétrospectivement une promesse de
discussion en amont.

## Suivi de conception

- [x] Intention visuelle et pédagogique reprise de la sélection initiale.
- [x] Ambiance cyberpunk nocturne choisie par l'utilisateur.
- [x] Périmètre conversationnel approuvé.
- [x] Frontières, données, rendu et interactions précisés.
- [x] Spécification écrite.
- [x] Auto-relecture : pas de placeholder, invariants numériques et rapports cohérents.
- [x] Spécification écrite relue et approuvée par l'utilisateur.
- [x] Plan d'implémentation rédigé après approbation de la spécification.
- [ ] Plan approuvé et méthode d'exécution choisie par l'utilisateur.
