# Performance CPU d’ArrayBuffer

Ce guide consigne le protocole `arraybuffer-cpu`, les commandes qui produisent ses rapports, et la
première lecture avant/après des contrôles de bornes Graphiks. Les durées sont informatives : la CI
n’échoue jamais sur une durée, seulement sur un résultat faux, un plantage ou un rapport incomplet.

## Protocole

- Protocole `arraybuffer-cpu`, implémenté dans `arraybuffer-benchmarks`. Ce sont des mesures
  CPU uniquement, jamais comparées aux résultats du suite GPU.
- Format d’identifiant de scénario : `<workload>.bytes-<bytes>.<variant>`.
- Charges : `scalar.write.i32`, `scalar.read.i32`, `scalar.write.f32`, `scatter.write.i32`,
  `bulk.bytes`, `bulk.floats`, `image.rgba8`, `vertices.p3n3uv2`.
- Variantes : `Checked` utilise les méthodes publiques d’`ArrayBuffer` ; `Reference` utilise la
  primitive de plateforme qu’utilise la bibliothèque aujourd’hui, sans les nouveaux contrôles
  Graphiks ; `BulkPrepared` copie un tableau source produit hors de la fenêtre chronométrée ;
  `PrepareAndBulk` construit et copie ce tableau dans la fenêtre.
- Profils : `ci` (3 warm-ups, 5 échantillons, 1 lancement) et `standard` (5 warm-ups et au moins
  2 s d’échauffement, 30 échantillons, 5 lancements).
- Le runner rapporte les échantillons, les graines, la calibration, l’environnement et la somme de
  contrôle. Une durée nulle est conservée ; elle ne devient jamais un débit infini.

## Commandes

```sh
# Construire le harnas et exécuter ses tests unitaires
./gradlew :arraybuffer-benchmarks:jvmTest :arraybuffer-benchmarks:jsNodeTest \
  :arraybuffer-benchmarks:wasmJsNodeTest :arraybuffer-benchmarks:macosArm64Test
./gradlew :arraybuffer-benchmarks:jsBrowserDistribution \
  :arraybuffer-benchmarks:wasmJsBrowserDistribution :arraybuffer-benchmarks:linkReleaseExecutableMacosArm64

# JVM
./gradlew :arraybuffer-benchmarks:runJvmBenchmarks \
  --args="--profile=standard --output=build/reports/arraybuffer/baseline-jvm-run0.json --run-index=0"

# Native (macOS arm64 ; linuxX64 sur Linux)
arraybuffer-benchmarks/build/bin/macosArm64/releaseExecutable/arraybuffer-benchmarks.kexe \
  --profile=standard --run-index=0 --output=build/reports/arraybuffer/baseline-native-run0.json

# Navigateur (aucun device GPU n’est créé)
node tools/run-arraybuffer-benchmarks.mjs js \
  arraybuffer-benchmarks/build/dist/js/productionExecutable \
  --profile=standard --run-index=0 --output=build/reports/arraybuffer/baseline-js-run0.json
node tools/run-arraybuffer-benchmarks.mjs wasm \
  arraybuffer-benchmarks/build/dist/wasmJs/productionExecutable \
  --profile=standard --run-index=0 --output=build/reports/arraybuffer/baseline-wasm-run0.json

# Android (émulateur ou appareil déjà connecté)
./gradlew :arraybuffer-android-instrumentation:installRelease \
  :arraybuffer-android-instrumentation:installReleaseAndroidTest
node tools/run-arraybuffer-android.mjs \
  --profile=standard --run-index=0 --output=build/reports/arraybuffer/baseline-android-run0.json

# Comparer une paire avant/après. Le candidat doit réutiliser les comptes de répétitions de la
# référence : --calibration=<rapport de référence> fonctionne pour chaque runner.
node tools/compare-arraybuffer-benchmarks.mjs \
  build/reports/arraybuffer/baseline-jvm-run0.json \
  build/reports/arraybuffer/post-jvm-run0.json \
  --output=build/reports/arraybuffer/compare-jvm.json
```

Valider ou comparer des rapports :

```sh
node tools/arraybuffer-report.mjs <report.json> --profile=standard
node --test tools/arraybuffer-report.test.mjs tools/compare-arraybuffer-benchmarks.test.mjs
```

## Première lecture

Environnement : macOS (arm64), JDK 25, Chromium 153, émulateur Android API 35 (`bench35`). Un
lancement `standard` par cible a été capturé avant les contrôles (`baseline-*`) et un après
(`post2-*`), avec réutilisation du compte de répétitions de la référence pour que les deux
exécutions partagent `operationsPerSample`. La comparaison est stricte : un environnement
différent ou un compte de répétitions différent est refusé plutôt que silencieusement normalisé.
Un lancement unique sur un poste de travail n’est **pas** une référence matérielle de mesure ;
traitez ces nombres comme indicatifs.

Variation médiane de la durée par échantillon, par charge (positif = la version avec contrôles est
plus lente) :

| Cible | scalar.write.i32 | scalar.read.i32 | scalar.write.f32 | scatter.write.i32 | bulk.bytes | bulk.floats | image.rgba8 | vertices.p3n3uv2 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| jvm | +61% | +0.3% | -0.6% | +0.2% | +3.7% | +2.7% | -2.3% | +2.6% |
| native | +266% | +277% | +74% | +279% | +7.1% | -3.6% | -3.0% | -3.7% |
| js | +1768% | +1607% | +2099% | +2262% | 0.0% | -1.3% | 0.0% | +1.8% |
| wasm | +14% | +15% | +17% | +17% | -2.0% | -0.9% | -0.5% | +2.2% |
| android (émulateur) | +72% | +95% | +58% | +78% | +3.4% | +2.6% | -0.3% | -1.0% |

### Interprétation

- **Copies en bloc, images et sommets sont essentiellement plats.** Leurs contrôles s’exécutent une
  fois par opération et la primitive elle-même domine, donc la validation supplémentaire est
  amortie. C’est la forme voulue : valider une plage une fois, puis copier ou produire.
- **L’accès scalaire paie tout le coût de contrôle par élément.** Sur Native le contrôle triple
  environ le temps d’un stockage brut ; sur la JVM il est autour de +60% pour la boucle d’écriture
  d’int ; l’émulateur Android est dans la même gamme. La variante `Reference` de la même exécution
  reste près de sa référence (par exemple, `scalar.read.i32` en JS passe de ~9.1 ms à ~9.9 ms pour
  le même compte de répétitions), donc le delta vient des contrôles ajoutés, pas d’un changement
  d’environnement.
- **Kotlin/JS est de loin le plus affecté.** L’arithmétique non signée des contrôles est émulée en
  JavaScript, bien plus coûteuse qu’un accès à un élément de typed array ; un accès scalaire par
  élément devient un ordre de grandeur plus lent. C’est un signal de conception, pas un artefact de
  mesure : la variante `Reference` de la même exécution est inchangée. Les writers prévalidés
  prototypés dans le protocole compagnon `arraybuffer-cpu-writers` existent précisément pour sortir
  les contrôles de la boucle par élément sur cette cible.
- Les médianes JVM `scalar.read.i32` et `scalar.write.f32` ont bougé de moins de 1% ; la boucle
  d’écriture est la régression la plus nette, cohérente avec deux contrôles non inlinés par
  stockage.

### Ce que cela ne dit pas

- Les deltas scalaires ne sont **pas** des attributions d’un coût d’instruction précis ; les
  contrôles ne sont pas inlinés et aucun profiler n’a été attaché à cette lecture.
  `allocationMeasurement` est `unavailable`.
- Le débit logique d’une copie en bloc n’est pas le trafic DRAM de la copie.
- `of` partage le stockage sous-jacent en JS et copie sur les autres cibles ; aucune conclusion
  inter-cibles sur « un runtime plus rapide » ne s’en suit.

## Writers prévalidés (`arraybuffer-cpu-writers`)

Les prototypes internes `RgbaWriterPrototype`/`VertexWriterPrototype` valident un layout entier une
fois (lignes, stride, plage, alignement de base), puis effectuent les stockages de pixels ou de
sommets avec la primitive de plateforme. Ils n’exposent ni handle ni callback. Leur campagne
compagnon re-mesure ses propres contrôles :

```sh
./gradlew :arraybuffer-benchmarks:runJvmBenchmarks \
  --args="--writers --profile=ci --output=build/reports/arraybuffer/writers-jvm.json --run-index=0"
node tools/run-arraybuffer-benchmarks.mjs js <dist> --writers --profile=ci
node tools/run-arraybuffer-android.mjs --writers --profile=ci
```

Chaque variante d’un groupe de layout partage un seul compte de répétitions
(`commonRepetitions`), donc les médianes brutes sont directement comparables dans une campagne.
`writer/bulk` ci-dessous est la médiane du writer divisée par la médiane `BulkPrepared` ; en
dessous de 1, le writer gagne. La cellule JavaScript 1 K sommets n’est pas rapportée : la médiane
en bloc arrondit à une durée nulle dans cette exécution.

| Cible | image 256² | image 1024² | image 4096² | 1 K sommets | 64 K sommets | 1 M sommets |
| --- | --- | --- | --- | --- | --- | --- |
| jvm | 9.3 | 14.7 | 12.5 | 4.1 | 4.7 | 3.0 |
| native | 21.5 | 12.9 | 19.9 | 5.2 | 7.8 | 5.8 |
| js | 0.50 | 0.67 | 0.67 | n/a | 1.33 | 1.15 |
| wasm | 1.00 | 1.01 | 1.00 | 1.11 | 1.07 | 1.10 |
| android (émulateur) | 129 | 128 | 118 | 21 | 35 | 26 |

Contre le chemin contrôlé **scalaire**, les writers sont plus rapides partout (`writer/checked` va
de 0.49 sur la JVM jusqu’à ~0.06 sur Native/Android et ~0.00 sur JS, dont le chemin contrôlé est
dominé par l’arithmétique non signée). Contre **`BulkPrepared`** en revanche, les writers sont
3–21× plus lents sur JVM, Native et Android, seulement compétitifs sur les images JavaScript
(≈0.5–0.67) et à peu près à égalité sur Wasm.

### Décision

**Garder les writers comme prototypes internes ; ne pas exposer d’API writer publique sur cette
preuve.**

- Partout où le chemin scalaire contrôlé est lent, un tableau CPU préparé plus
  `setBytes`/`setFloats` (`BulkPrepared`/`PrepareAndBulk`) égale ou bat déjà le writer (3–21× plus
  rapide sur JVM, Native et Android ; égal sur Wasm), sans nouvelle surface publique.
- Le seul gain clair du writer est les images JavaScript (≈0.5–0.67 de la copie en bloc), où le
  chemin scalaire contrôlé est un ordre de grandeur plus lent ; une API publique justifiée par un
  seul lancement `ci` sur une seule cible ne se justifie pas.
- La dispersion par cible est du même ordre que plusieurs des différences, donc une décision
  exige cinq lancements `standard` sur un hôte stable, pas une seule exécution `ci`.

## CI

Il n’y a pas de workflow ArrayBuffer dédié. Les pull requests exécutent le workflow standard
**Tests** (`.github/workflows/test.yml`) : sa matrice `./gradlew check` (Ubuntu, macOS, Windows)
couvre déjà les suites `webgpu-api` et `arraybuffer-benchmarks`, les cibles natives de chaque hôte
et `checkKotlinAbi`. Son job `android` exécute la matrice instrumentée
`ArrayBufferAndroidBusinessCases` sur un émulateur, en miroir de l’implémentation Android
`ByteBuffer`, comme pair des autres cibles de test. Aucune campagne de mesure ne s’exécute sur une
pull request — un runner partagé n’est pas une référence de performance. Les campagnes, leur
outillage et les tests unitaires de cet outillage appartiennent aux machines où les campagnes sont
lancées ; les commandes sont listées ci-dessus, et les durées ne sont jamais un critère de CI.

## Campagnes non exécutées

- Une campagne compagnon de cycle de vie (allocation + remplissage, `of` + consommation) n’est pas
  encore implémentée.
- Le profiling d’allocation (JFR sur la JVM, Perfetto sur Android, échantillonnage DevTools dans
  Chromium, un profileur d’allocation sur l’hôte pour Native) n’a pas été exécuté ; chaque rapport
  garde `allocationMeasurement: "unavailable"`.
- Un seul lancement `standard` par cible a été capturé ; les médianes à cinq lancements restent à
  produire sur un hôte stable.
