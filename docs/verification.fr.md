# Vérification

Ce document enregistre le contrat de référence, l’inventaire généré et les preuves de vérification
de la Graphiks WebGPU Suite : ce qui est vérifié, comment, et les résultats observés. Ce n’est pas
un certificat de conformité.

## Contrat de référence

- Version d’API et de suite : `0.1.0-SNAPSHOT`.
- Commit de référence et hachages des sources : générés à la construction (publiés dans
  `suite/inventory/baseline.json`), à partir du commit et du SHA-256 des sept fichiers sources de
  l’API.
- L’inventaire est extrait des sept fichiers `commonMain` de `webgpu-api` (pas du seul snapshot
  d’ABI JVM), il représente donc la surface de signatures partagée.

## Inventaire

L’inventaire est **généré à la construction** à partir des annotations de cas et des sources de
l’API ; il n’est pas versionné. Le `symbols.tsv` généré liste **884 déclarations** sur 14 familles,
et les fichiers localisés de comportements décrivent **147 comportements par locale** :
**132 cas exécutables** et **15 résidus**. Le nombre de symboles est une simple mesure de surface :
ce n’est pas un pourcentage de conformité, et un symbole listé ne signifie jamais qu’il est testé.

| Famille | Déclarations |
| --- | ---: |
| textures/views/samplers | 223 |
| pipelines/render state | 185 |
| types de données/descripteurs/flags/swizzle | 115 |
| adapter/device/features/limits | 88 |
| rendu/passes/attachments | 55 |
| shaders/compilation | 36 |
| bind groups/layouts | 35 |
| buffers/mapping | 34 |
| compute | 24 |
| erreurs/asynchronisme | 22 |
| render bundles | 21 |
| transferts buffers/textures | 18 |
| queue/commandes | 16 |
| queries/timestamps | 12 |
| **Total** | **884** |

Le bilan de couverture, les résidus et les limites connues de la preuve sont dans
[acid-coverage.fr.md](acid-coverage.fr.md).

## Sémantique du runner

- Sans adaptateur, le runner rapporte chaque cas comme `failed` avec `No WebGPU adapter is
  available.` — jamais `passed` et jamais `unsupported`.
- Le collecteur sort en non-zéro lorsqu’un id de cas manque, est dupliqué ou n’est pas passé.
- L’isolation des cas tient : `errors.uncaptured-error` sur son device dédié suivi de
  `errors.empty-scope` dans la même campagne ont chacun rapporté leur propre résultat, sans fuite
  d’erreur vers le cas suivant.

## Campagne acid

- Commandes :

  ```sh
  ./gradlew :suite-acid-tests:generateSuiteInventory \
    :suite-core:compileKotlinJvm :suite-acid-tests:compileKotlinJvm :suite-benchmarks:compileKotlinJvm \
    :suite-browser:jsBrowserDistribution :suite-browser:wasmJsBrowserDistribution
  node tools/run-browser.mjs js suite-browser/build/dist/js/productionExecutable
  node tools/run-browser.mjs wasm suite-browser/build/dist/wasmJs/productionExecutable
  node tools/run-browser.mjs js suite-browser/build/dist/js/productionExecutable --demo-check
  node tools/run-browser.mjs wasm suite-browser/build/dist/wasmJs/productionExecutable --demo-check
  node tools/run-browser.mjs js suite-browser/build/dist/js/productionExecutable --benchmark --profile=ci --backend=swiftshader
  node tools/run-browser.mjs wasm suite-browser/build/dist/wasmJs/productionExecutable --benchmark --profile=ci --backend=swiftshader
  node tools/build-site.mjs
  ./gradlew check
  ```

- Environnement : `Chromium 153.0.8010.12` (Playwright 1.63.0) sur `darwin`, headless avec
  `--enable-unsafe-webgpu --enable-unsafe-swiftshader --use-angle=swiftshader`. Ce sont des
  résultats fonctionnels sur backend logiciel, pas des résultats de GPU physique.
- Résultat : **132 cas (125 obligatoires, 7 optionnels)**.

  | Cible | Passés | Unsupported | Échoués | Total |
  | --- | ---: | ---: | ---: | ---: |
  | JS | 131 | 1 | 0 | 132 |
  | Wasm JS | 131 | 1 | 0 | 132 |

  Les **125 cas obligatoires passent** sur les deux cibles. Le seul cas non passant est l’optionnel
  `compute.shader-f16`, `unsupported` parce que l’environnement ne dispose pas de `ShaderF16`.
- Démos : **2/2 sur les deux cibles**. Scénarios de benchmark : **10/10 sur les deux cibles** (profil
  `ci`, 5 échantillons retenus par scénario).
- Attribution des rapports : le `buildCommit` de chaque enveloppe égale `git rev-parse HEAD` à la
  construction et correspond au `baseline.commit` de l’inventaire, et le `buildVersion` correspond au
  `baseline.suiteVersion` ; le collecteur échoue fermé sur une identité manquante ou discordante.
- Site : assemblé avec **884 symboles et 147 comportements par locale** ; chacune des 132 entrées de
  cas pointe vers son fichier source.
- Preuves unitaires : `./gradlew check` passe.
- Observé le 2026-10-02.

## Preuves de mutation des oracles

Chaque ligne ci-dessous retire ou casse le comportement qu’un oracle doit observer ; le cas
rapporte alors `failed`. Aucune mutation ne fait partie du catalogue.

| Mutation | Cas qui échouent |
| --- | --- |
| `readRgba8` : l’appel `copyTextureToBuffer` retiré | `render.clear-only` (observe le préremplissage `0xa5` au lieu de 255) |
| `requiredLimits = null` dans la requête de device | `device.reject-excess-limit` |
| Deux command buffers soumis en ordre inverse | `command.ordered-command-buffers` |
| Biais de slope-clamp mis à 0 | `depth.bias-slope-clamp` |
| Le mapper supprime la `size` transmise du binding (`mapper/BindGroupDescriptor.kt`) | `bindings.storage-range-length` |
| `minBindingSize` abaissée pour que la sonde de 12 octets se lie | `errors.min-binding-size` |
| `setViewport` remplacé par `setScissorRect(4,4,8,8)` | `render.viewport` |
| Le bundle dessine un triangle (`draw(3)`) | `bundles.draw`, `bundles.reuse`, `command.debug-markers` |
| Descripteur de timestamp hors bornes omis dans la sonde | `errors.compute-timestamp-indices` |
| Descripteur de timestamp hors bornes omis dans la sonde | `errors.render-timestamp-indices` |
| État du pass réassocié avant le dessin post-bundle (substitut de rétention d’état) | `errors.bundle-post-execute-state` |
| Mapping de readback de profondeur raccourci à 520 octets | `transfers.texture-copy-aspect` |

Survivants acceptés — mutations que le catalogue ne détecte volontairement pas, avec la
revendication resserrée qui les couvre : omettre `size` seulement dans `bindings.buffer-range`,
retirer `timestampWrites` seulement des cas de timestamps positifs (couverts par les cas d’index
déterministes), et un `onSubmittedWorkDone` immédiat. La limite d’observabilité des timestamps est
documentée dans [acid-coverage.fr.md](acid-coverage.fr.md).

## Preuves des démos (2026-09-28)

- Commandes :

  ```sh
  ./gradlew :suite-browser:jsBrowserDistribution :suite-browser:wasmJsBrowserDistribution
  node tools/run-browser.mjs js suite-browser/build/dist/js/productionExecutable
  node tools/run-browser.mjs wasm suite-browser/build/dist/wasmJs/productionExecutable
  node tools/run-browser.mjs js suite-browser/build/dist/js/productionExecutable --demo-check
  node tools/run-browser.mjs wasm suite-browser/build/dist/wasmJs/productionExecutable --demo-check
  node tools/build-site.mjs
  ```

- Résultats des démos : **2/2 passés sur JS et 2/2 passés sur Wasm**.
  `particles.compute-render-readback` relit la position calculée `x=0.02` avec la vélocité
  inchangée, et lit les pixels du disque rendu (`r>40`, `g>150`, `b>200`, `a=255`) sur fond noir,
  depuis la même soumission que le pass de compute.
  `particles.bounds-pause-reset` exerce 65 particules (un dernier workgroup partiel), les bornes de
  rebond `±0.95`, une frame à delta zéro inchangée et une réinitialisation vers les données
  initiales. Les contrôles de démo tournent sur leur propre adaptateur et device et publient
  `globalThis.graphiksDemoReport` ; les rapports sont `build/reports/demos-js.json` et
  `demos-wasm.json`, séparés des rapports des acid tests.
- Les contrôles détectent une vraie régression : forcer le delta de frame à zéro a fait échouer les
  deux ids avec `Computed x was 0.0` et `Bounced x ... expected 0.95 but was 0.94` ; la restauration
  a redonné 2/2 sur les deux cibles.
- `device.limits` lit comme zéro une propriété de limite que l’implémentation omet ; la démo exécute
  le chemin de readback qui en dépend.
- Environnement : `Chromium 140.0.7339.186` sur `darwin`, lancé headless avec
  `--enable-unsafe-webgpu --enable-unsafe-swiftshader --use-angle=swiftshader`. Le backend demandé
  est `swiftshader`, qui décrit les drapeaux de lancement ; ce sont des résultats fonctionnels sur
  backend logiciel, ni des résultats de GPU physique ni des mesures de performance.
- Contrôles interactifs manuels sur les deux cibles (Chromium headless, captures) :
  `?demo=particles&lang=fr` rend des particules colorées (environ 29k pixels saturés dans la région
  du canvas) ; pause bascule `Pause`/`Reprendre` ; réinitialisation et changement de nombre
  (4096→1024) gardent la page saine ; un redimensionnement 1280×720 → 480×900 change le canvas de
  768×480 à 448×280 (même ratio) ; sans les drapeaux WebGPU la page montre le diagnostic localisé
  `unavailable` ; `?demo=unknown` montre une erreur explicite ; le lien Validation revient et masque
  la démo. La réinitialisation de l’horloge de frame lors de l’onglet caché a été inspectée dans le
  code ; elle n’est pas automatisable dans cette configuration headless. Ce sont des contrôles
  manuels, pas des tests de structure HTML.
- Galerie : servie sous un préfixe `/suite/`, la page Démos charge la carte localisée depuis la
  ressource de démo partagée avec le runner, pointe vers
  `../run/js|wasm/?demo=particles&lang=…`, et construit les liens source depuis le `buildCommit` du
  rapport de démo publié. Sans les rapports, les liens source retombent vers le répertoire source sur
  `master` et le disent. La page n’a pas de débordement horizontal à 375 px, et le lien de lancement
  ouvre la démo en cours.
- Compilation versus exécution native : `suite-demos` compile pour JVM 25, JS, Wasm JS, Linux x64 et
  macOS ARM64 via ses tâches normales. Aucune exécution GPU native n’est effectuée dans ce dépôt ;
  cela appartient aux dépôts de bindings consommateurs, comme pour les acid tests.

## Preuves du rapport lisible

- La route de validation (`run/js|wasm/`) et la route de vérification de la démo rendent une ligne
  de résumé localisée (EN/FR, `?lang=` en priorité, puis la locale mémorisée), un tableau des cas avec
  statuts colorés et diagnostics, et le JSON brut pretty-printé dans un bloc `details` replié. Les
  chaînes publiées `graphiksSuiteReport` et `graphiksDemoReport` sont un JSON compact pour le
  collecteur ; la présentation DOM est un rendu séparé du même rapport.
- Commandes :

  ```sh
  ./gradlew :suite-browser:jsBrowserDistribution :suite-browser:wasmJsBrowserDistribution
  node tools/run-browser.mjs js suite-browser/build/dist/js/productionExecutable
  node tools/run-browser.mjs wasm suite-browser/build/dist/wasmJs/productionExecutable
  node tools/run-browser.mjs js suite-browser/build/dist/js/productionExecutable --demo-check
  node tools/run-browser.mjs wasm suite-browser/build/dist/wasmJs/productionExecutable --demo-check
  ```

- Collecteur : **131 réussis + 1 non pris en charge (`compute.shader-f16` optionnel) sur 132 en JS
  et en Wasm**, et **2/2 cas de démo sur les deux cibles**.
- Contrôles DOM manuels dans Chromium headless (Playwright, swiftshader) : EN et FR rendent le résumé
  (`2 passed · 0 failed · 0 unsupported — 2 cases` / `2 réussis · 0 échoués · 0 non pris en charge —
  2 cas`), les lignes des cas affichent des statuts localisés, le JSON replié est pretty-printé, le
  mode sombre utilise sa palette sombre, la distribution Wasm rend la même page que la JS, et la
  route de démo affiche son propre titre avec la section de validation masquée. Aucune erreur de page
  n’est journalisée. Avec les ressources de textes bloquées, la page affiche le JSON brut dans
  `#result` avec un diagnostic explicite dans la ligne de statut.

## Preuves de benchmark (2026-09-28)

- Commandes :

  ```sh
  ./gradlew :suite-benchmarks:compileKotlinJvm :suite-benchmarks:compileKotlinJs :suite-benchmarks:compileKotlinWasmJs
  ./gradlew :suite-browser:jsBrowserDistribution :suite-browser:wasmJsBrowserDistribution
  node tools/run-browser.mjs js suite-browser/build/dist/js/productionExecutable --benchmark --profile=ci --backend=swiftshader
  node tools/run-browser.mjs wasm suite-browser/build/dist/wasmJs/productionExecutable --benchmark --profile=ci --backend=swiftshader
  node tools/run-browser.mjs js suite-browser/build/dist/js/productionExecutable --benchmark --profile=standard --backend=swiftshader
  node tools/build-site.mjs
  ```

- Résultat : **10/10 scénarios `completed` et vérifiés GPU sur JS et 10/10 sur Wasm**, pour le profil
  `ci` (5 échantillons retenus par scénario) et, sur JS, pour le profil `standard` (30 échantillons).
  Aucune erreur de page ni d’erreur fatale. Les champs de protocole étaient `schemaVersion=1`,
  `protocol=foundations-v1`, `clock=kotlin.time.TimeSource.Monotonic`. Les dix ids, le profil et le
  nombre d’échantillons sont exigés par le collecteur ; un scénario manquant, dupliqué, non
  `completed`, non vérifié ou au mauvais compte le fait sortir en non-zéro. Aucun seuil n’est
  appliqué à une durée.
- Readback de contrôle : chaque scénario relit tout son buffer de résultats avant les warm-ups et
  après le dernier échantillon, contre une valeur calculée depuis l’index. `transfer.write-buffer`
  vérifie le mot `i xor (0x9e3779b9 + j)` pour la dernière écriture d’un lot ;
  `compute.encode-submit` vérifie `i * 3 + 7`.
- Le contrôle détecte une vraie régression GPU : remplacer `+ 7u` par `+ 8u` dans le shader de
  compute a mis les quatre scénarios de compute en `failed` avec un mismatch de readback (les six
  scénarios de transfert sont restés passés), le collecteur est sorti en non-zéro, et la
  restauration du shader a redonné **10/10 sur les deux cibles**.
- Limites d’horloge et d’environnement observées : sous le backend `swiftshader` demandé, la
  plupart des `cpuIssueMs` valent exactement `0.000` — sous la résolution pratique de l’horloge — et
  sont conservés et comptés comme `zeroCpuSamples` plutôt que filtrés ou transformés en taux.
  `completionMs` résout l’attente de compute (environ 2,6 ms pour un dispatch sur 65 536 éléments,
  environ 17–19 ms pour seize, et environ 0,9–1,4 ms pour seize écritures de 1 MiB). La description
  d’adaptateur rapportée était vide et est enregistrée comme `null`/inconnue ; `isFallbackAdapter`
  était `true`. Ce sont des observations fonctionnelles sur backend logiciel, pas des performances
  de GPU physique.
- Classification des timeouts : avec le budget de campagne temporairement réduit à une seconde, le
  scénario en vol a été rapporté `failed` avec `The campaign timed out after 16 minutes.` plutôt que
  le timeout de scénario de 90 secondes ou un diagnostic d’annulation d’onglet caché ; la
  restauration du budget a redonné 10/10.
- Options du collecteur : `--profile=ci|standard` (`ci` par défaut dans l’outil) et
  `--backend=swiftshader|default` (`swiftshader` par défaut). `--backend=default` retire les
  drapeaux SwiftShader et enregistre `default`, jamais `hardware`. JS et Wasm ont été exécutés
  séquentiellement, jamais en parallèle.
- Inspection du site : la page Benchmarks assemblée a été servie sous un préfixe local et vérifiée à
  un viewport mobile de 390 px en EN et FR, avec les profils `ci` et `standard` chargés côte à côte.
  Elle affiche la taille, le lot, les warm-ups, les échantillons retenus/planifiés, le contrôle GPU,
  et la médiane et le min/max des deux durées ; le p95 n’apparaît qu’à partir de 20 échantillons.
  Une médiane et un p95 affichés ont été comparés à la main avec les échantillons bruts d’une vraie
  campagne et correspondaient (`médiane 0,9 [0,8..11,4]`, `p95 1,1` pour les seize écritures de
  1 MiB). Sans rapport, la page montre « No published measurements: not run. », jamais une série de
  zéros. Ce sont des contrôles de livraison manuels, pas des tests de structure HTML ou
  statistiques.
- Annulation et refus, observés sur les deux cibles via la page locale : annuler une campagne a
  marqué le scénario en vol `interrupted` et le reste `not-run`, a gardé le résultat affiché
  précédent et sa note, et a publié le rapport incomplet pour l’outillage. Sans WebGPU
  (`navigator.gpu` retiré) la page a refusé de démarrer avec un message visible et n’a publié aucun
  rapport.
- Compilation : `suite-benchmarks` compile pour JVM 25, JS et Wasm JS via ses tâches normales ; ses
  cibles Linux x64 et macOS ARM64 sont déclarées et publient avec la même convention que le reste
  de la suite. Aucune exécution GPU native ni test de consommation Maven ne sont effectués ici.

## Preuves de compilation et de publication

- Les notes d’implémentation consignent la compilation des cinq cibles annoncées via leurs tâches
  Gradle normales (`compileKotlinJvm`, `compileKotlinJs`, `compileKotlinWasmJs`,
  `compileKotlinLinuxX64`, `compileKotlinMacosArm64`), y compris la cross-compilation Linux x64
  locale sur macOS ARM64. Les scripts de modules fusionnés déclarent ces cibles mais ne câblent pas
  explicitement `check` aux cinq tâches de compilation. La CI de la suite compile explicitement la
  JVM et les distributions navigateur ; la matrice Tests invoque la tâche générale `check`. Cette
  configuration seule n’établit pas une compilation fraîche et réussie de chaque cible native.
- Les artefacts suivent la convention de publication existante du dépôt : `buildSrc` fournit le
  POM, la licence, les sources et la signature, et `.github/workflows/publish.yml` publie
  `org.graphiks:suite-core`, `org.graphiks:suite-acid-tests`, `org.graphiks:suite-demos` et
  `org.graphiks:suite-benchmarks` aux côtés des quatre modules de base sous le `releaseVersion`
  partagé.
- Il n’y a pas de projet consommateur dédié, de dépôt Maven isolé ni de test de
  publication/consommation : le plan et la spec amendés excluent un tel test d’architecture. La
  résolution des artefacts par les bindings externes est exercée dans leurs propres dépôts, pas
  ici.

## Limites connues

- L’installeur Playwright Chromium pinné peut bloquer pendant l’extraction sur certaines machines ;
  dans ce cas le build `chromium-headless-shell` correspondant est installé directement et marqué
  complet. `tools/package.json` pinne Playwright 1.63.0 et la CI exécute normalement
  `playwright install --with-deps chromium`. Un cache assemblé à la main dans cette situation
  exerce toujours la révision pinnée.
- Le `mkdocs build --strict` complet (embed Dokka plus `mkdocs-static-i18n`) n’a pas été exécuté
  localement car ce plugin Python n’est pas installé ici. L’entrée de navigation et le lien relatif
  de la suite ont été validés statiquement ; le site de documentation assemblé est exercé par le
  workflow Documentation dans la CI.
- L’exécution GPU native des cas partagés sur JVM, Linux x64 ou macOS ARM64 n’est pas effectuée
  dans ce dépôt. Ces résultats appartiennent aux dépôts de bindings consommateurs, qui fournissent
  leur propre device. Ce dépôt compile les modules partagés et exécute les cas navigateur.
- Les résultats ci-dessus sont des résultats fonctionnels sur backend logiciel. Ce ne sont pas des
  mesures de performance et ne représentent pas un GPU physique.
