# Performance CPU d’ArrayBuffer

Ce guide consigne le protocole `arraybuffer-cpu`, les commandes qui produisent ses rapports, et
deux lectures des contrôles de bornes Graphiks : la comparaison avant/après, puis des médianes à
cinq lancements du chemin vérifié contre la primitive de plateforme. Les durées sont informatives :
la CI n’échoue jamais sur une durée, seulement sur un résultat faux, un plantage ou un rapport
incomplet.

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

## Médianes à cinq lancements

Même poste de travail que la première lecture (macOS arm64 physique), sommet fusionné de la
branche `94dfcbd` (2026-10-06), JDK 25.0.1, Chromium 153.0.8010.12, cinq lancements `standard` par
cible (rapports `post3-<cible>-run0..4.json`). Là où la première lecture compare deux builds,
celle-ci compare deux variantes **au sein de chaque rapport** : `Checked` et `Reference` partagent
le compte de répétitions de leur groupe de layout, donc chaque rapport est calculé par lancement,
et la médiane des cinq lancements est consignée avec son étendue. Le rapport mesure donc le chemin
public vérifié complet — validation plus couche API — contre la primitive de plateforme brute, sur
le même build. Les sources de la bibliothèque n’ont pas changé depuis la première lecture
(`libraryHash` identique) : les deux lectures décrivent le même code, et le coût des seuls
contrôles reste celui des pourcentages de la première lecture. La même réserve s’applique : c’est
un poste de travail, pas un banc d’essai.

Médiane des rapports `Checked`/`Reference` par lancement (étendue des cinq lancements entre
parenthèses) :

| Cible | scalar.write.i32 | scalar.read.i32 | scalar.write.f32 | scatter.write.i32 | bulk ≥ 64 Kio | bulk 256 o |
| --- | --- | --- | --- | --- | --- | --- |
| jvm | 1.63–1.91× (1.62–2.38×) | ≈0.99× (0.98–1.19×) | 1.61–1.94× (1.58–1.95×) | 0.15–0.17× (0.14–10.20×) | 0.83–1.12× (0.69–1.55×) | 1.02–1.03× (0.98–1.05×) |
| native | ≈4.3× (4.00–4.41×) | ≈7.3× (6.85–7.48×) | 6.97–9.58× (6.92–9.71×) | 4.32–6.12× (4.25–11.67×) | 1.00–1.03× (0.90–1.13×) | 1.33–1.46× (1.31–1.47×) |
| js | ≈27.7× (26.7–36.2×) | ≈27.8× (26.3–36.8×) | ≈27.5× (26.8–36.3×) | ≈29.2× (26.5–34.7×) | 1.00–1.01× (0.97–1.03×) | 2.24–4.36× (2.18–4.91×) |
| wasm | ≈1.18× (1.15–1.33×) | ≈1.22× (1.18–1.42×) | ≈1.20× (1.03–1.24×) | ≈1.25× (1.23–1.28×) | 0.94–0.95× (0.93–0.97×) | 0.94–0.96× (0.93–0.97×) |

Stockages vérifiés par élément contre la copie validée une seule fois (`Checked`/`BulkPrepared`,
médiane, étendue) :

| Cible | image 256² | image 1024² | image 4096² | 1 K sommets | 64 K sommets | 1 M sommets |
| --- | --- | --- | --- | --- | --- | --- |
| jvm | 29.5× (28.4–164×) | 40.3× (30.2–176×) | 26.9× (26.1–111×) | 10.4× (7.6–43.8×) | 13.3× (11.1–66.0×) | 5.9× (5.9–28.7×) |
| native | 260× (252–262×) | 262× (255–268×) | 236× (225–243×) | 90.4× (39.3–98.2×) | 116× (114–117×) | 109× (85.5–110×) |
| js | 138× (135–194×) | 161× (156–196×) | 161× (158–201×) | n/a | 871× (651–996×) | 764× (753–1003×) |
| wasm | 3.67× (3.66–3.72×) | 3.68× (2.39–3.70×) | 3.68× (3.64–3.73×) | 3.45× (3.38–3.53×) | 3.41× (3.31–3.44×) | 3.40× (3.21–3.44×) |

### Lecture

- **Les contrôles sont gratuits sur les chemins pour lesquels l’API est faite.** À 64 Kio et
  au-delà, chaque copie vérifiée se situe entre 0.83× et 1.12× sur les quatre cibles : la
  validation n’intervient qu’une fois par appel et la copie domine. Sur Wasm, le chemin vérifié est
  même constamment environ 5 % plus rapide que la primitive brute (0.94–0.96×, étendue 0.93–0.97×) ;
  la primitive `Reference` n’est pas toujours le plancher.
- **L’accès scalaire paie par élément.** Les écritures JVM paient +61–94 % et les lectures restent
  à plat (≈0.99×) ; Native paie ≈4.3× par stockage d’int et ≈7.3× par lecture ; JavaScript paie
  ≈27× parce que l’arithmétique non signée des contrôles est émulée à chaque élément ; Wasm paie
  +18–25 %, le coût scalaire le plus faible des quatre.
- La cellule scatter de la JVM est bimodale entre lancements (rapports par lancement de 0.14× à
  10.2×) : la primitive `ByteBuffer` éparpillée subit elle-même une pathologie JIT et aucune
  variante ne domine sur cette cible. Considérer la cellule comme instable, pas comme une victoire
  du chemin vérifié.
- Construire une image ou un flux de sommets par stockages vérifiés élément par élément plutôt
  qu’avec la copie validée une seule fois coûte de 3.4× (Wasm) à 262× (Native). La cellule
  JavaScript 1 K sommets n’est pas rapportée : la médiane en bloc arrondit à une durée nulle, comme
  dans le tableau des writers ci-dessous.
- Les étendues image et sommets sont larges sur la JVM parce qu’un lancement isolé peut réchauffer
  le chemin de copie en bloc de façon inégale ; les médianes des rapports par lancement restent la
  lecture robuste.

La campagne Android instrumentée conserve le lancement unique de la première lecture ; cinq
lancements sur un appareil restent à produire.

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
couvre les suites `webgpu-api` et `arraybuffer-benchmarks`, les cibles natives de chaque hôte et
`checkKotlinAbi`. La matrice instrumentée Android ne fait pas partie de la CI ; elle est lancée à
la main sur un appareil ou un émulateur avec la commande documentée. Aucune campagne de mesure ne
s’exécute sur une pull request — un runner partagé n’est pas une référence de performance. Les campagnes, leur
outillage et les tests unitaires de cet outillage appartiennent aux machines où les campagnes sont
lancées ; les commandes sont listées ci-dessus, et les durées ne sont jamais un critère de CI.

## Campagnes non exécutées

- Une campagne compagnon de cycle de vie (allocation + remplissage, `of` + consommation) n’est pas
  encore implémentée.
- Le profiling d’allocation (JFR sur la JVM, Perfetto sur Android, échantillonnage DevTools dans
  Chromium, un profileur d’allocation sur l’hôte pour Native) n’a pas été exécuté ; chaque rapport
  garde `allocationMeasurement: "unavailable"`.
- Cinq lancements `standard` par cible ont été capturés sur le poste de travail de la première
  lecture pour JVM, Native, JavaScript et Wasm (2026-10-06) ; les médianes sont consignées plus
  haut. La campagne Android instrumentée reste à un lancement unique ; cinq lancements sur un
  appareil restent à produire.
