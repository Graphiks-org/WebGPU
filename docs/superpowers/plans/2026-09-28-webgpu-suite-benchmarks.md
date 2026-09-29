# Graphiks WebGPU Suite — Plan 3 : benchmarks de transferts et compute

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking. Exécution par un autre agent dans un worktree du même dépôt. Ce plan est soumis à revue ; les extraits ne constituent pas une preuve de compilation ou de mesure.

**Goal:** Livrer `suite-benchmarks`, deux workloads portables avec protocole explicite, leur exécution navigateur JS/Wasm et une page Benchmarks EN/FR.

**Architecture:** Deux fonctions spécialisées mesurent `queue.writeBuffer` et l’encodage/soumission de compute. Les ressources sont préparées hors mesure, les échantillons bruts sont conservés et les résultats GPU sont vérifiés séparément. Le runner navigateur fournit device, environnement et cycle de vie ; la publication et le site réutilisent les conventions existantes.

**Tech Stack:** Kotlin KMP 2.4.20, JDK 25, conventions `kmp` et `publish`, catalogue `libs`, `TimeSource.Monotonic`, coroutines, serialization déjà utilisée dans `suite-browser`, Playwright et site statique existants.

**Spec:** `docs/superpowers/specs/2026-09-27-webgpu-suite-design.md`, section Benchmarks et principes de lisibilité/validation. Baseline examinée : `a0287f1` (PR #121), après `b4d750a` (PR #120).

## Global Constraints

- Même dépôt, wrapper, catalogue, conventions et `releaseVersion`. Aucun build autonome ni projet de consommation Maven.
- Code lisible, commandes GPU visibles, deux workloads explicites ; pas de moteur de benchmarks générique, réflexion, annotation ou génération supplémentaire.
- `suite-benchmarks` dépend de `suite-core` et des descripteurs, pas des acid tests, des démos ou d’un binding concret.
- Les tests démontrent un résultat WebGPU. Compilation et lancement des vrais modules vérifient l’intégration ; aucun test d’architecture, de publication ou de structure de rapport.
- Une durée CPU n’est pas une durée GPU. Le temps jusqu’à achèvement inclut l’attente, la soumission, les transferts éventuels, l’exécution et le retour au runtime.
- Aucun timestamp GPU, feature optionnelle, benchmark de particules, seuil de régression de performance ou classement inter-backends dans ce lot.
- Les onze acid tests et les deux vérifications de particules par cible restent exécutables et distincts.
- Les résultats de CI SwiftShader sont des observations sur un backend logiciel, pas les performances d’un GPU matériel.
- Shell local préfixé par `rtk`, commandes natives dans les workflows.

## Review Focus

1. Mesurer une file déjà occupée biaise les résultats : vider la queue avant chaque échantillon, hors fenêtre — tâches 2 et 3.
2. Un travail faux peut sembler rapide : readback complet avant et après mesures, données attendues connues — tâches 2 et 3.
3. Mesures nulles ou peu résolues : conserver les zéros, identifier l’horloge et ne pas dériver de débit infini — tâches 1, 4 et 5.
4. Onglet masqué, annulation, perte du device ou erreur GPU : invalider le scénario, conserver le diagnostic et ne pas présenter un résumé exploitable — tâche 4.
5. Warm-up, allocations et lecture de contrôle dans la fenêtre : protocole visible, tailles et répétitions sérialisées avec chaque résultat — tâches 1 à 3 et présentation tâche 5.

---

## État intégré et périmètre

Le merge `a0287f1` contient `suite-demos`, les particules, leur galerie, les routes `?demo=particles` et `?demo=particles&verify=1`, ainsi que le collecteur `--demo-check`. `tools/build-site.mjs` assemble déjà les rapports des acid tests et des démos avec les distributions. Le workflow Suite est appelé par Tests, Documentation et Publication.

Ce lot complète l’offre par des mesures sans réutiliser la démo de particules : une charge compute courte permet de comprendre exactement ce qui est chronométré. Les benchmarks ne modifient pas les annotations des acid tests ni leur inventaire de couverture.

### Protocole initial versionné : `foundations-v1`

| Workload | Paramètres | Nombre de scénarios |
| --- | --- | --- |
| `transfer.write-buffer` | taille 4 KiB, 64 KiB ou 1 MiB ; 1 ou 16 écritures par échantillon | 6 |
| `compute.encode-submit` | 1 024 ou 65 536 éléments u32 ; 1 ou 16 dispatches par échantillon | 4 |

Deux profils explicites : `standard` = 5 warm-ups + 30 échantillons ; `ci` = 3 warm-ups + 5 échantillons. Les tailles et charges sont identiques ; seul le nombre de répétitions change. Le profil CI est un contrôle de fonctionnement avec quelques observations de durée, pas une campagne statistique représentative.

Ordre stable : transferts par taille croissante, puis compute par effectif croissant ; pour chacun batch 1 puis 16. JS et Wasm sont lancés séquentiellement, chacun dans sa page isolée. L’ordre est publié et ne justifie pas une comparaison causale entre cibles.

Chaque échantillon fournit :
- `cpuIssueMs` : début des appels GPU jusqu’à leur retour synchrone (soumission incluse pour compute) ;
- `completionMs` : même début jusqu’au retour de `queue.onSubmittedWorkDone()`.

Ces valeurs sont des durées par batch. Leur division par le nombre d’opérations peut être affichée comme moyenne amortie par opération, jamais comme latence individuelle. La différence entre les deux durées n’est pas présentée comme du temps GPU.

## Préparation

- [ ] Lire spec, plan et `docs/running.md` ; vérifier le worktree et préserver les modifications documentaires déjà présentes.
- [ ] Lire `suite-demos/build.gradle.kts`, `suite-browser/Main.kt`, `tools/run-browser.mjs`, `tools/build-site.mjs`, `.github/workflows/suite.yml` et le workflow de publication.
- [ ] Compiler les distributions et lancer une baseline des acid tests et des démos selon les commandes existantes. Consigner les pannes préexistantes. Les données de `docs/verification.md` sont historiques, pas une preuve du présent lancement.

## Carte des fichiers

Les sources portables sont sous `suite-benchmarks/src/commonMain/kotlin/org/graphiks/webgpu/suite/benchmarks/` :

| Fichier | Responsabilité |
| --- | --- |
| `BenchmarkResult.kt` | Profil, échantillon et résultat portable sans sérialisation navigateur |
| `WriteBufferBenchmark.kt` | Ressources, mesures et readbacks du transfert |
| `ComputeBenchmark.kt` | Shader, pipeline, mesures et readbacks compute |
| `Readback.kt` | Petit helper interne de lecture u32, sans framework de scénarios |

Dans `suite-browser/src/commonMain/kotlin/org/graphiks/webgpu/suite/browser/benchmarks/` :

| Fichier | Responsabilité |
| --- | --- |
| `BenchmarkReport.kt` | DTO sérialisables, protocole et environnement |
| `RunBenchmarks.kt` | Dix scénarios, isolation, timeouts et invalidation |
| `BenchmarksPage.kt` | Lancement explicite, progression et résultats locaux |

Autres fichiers : `suite-benchmarks/build.gradle.kts`, `settings.gradle.kts`, `suite-browser/build.gradle.kts`, `suite-browser/.../Main.kt`, `suite-browser/src/commonMain/resources/benchmarks/texts.{en,fr}.json`, `site/benchmarks/{index.html,app.js}`, navigation des deux pages existantes, outils et workflows existants, `docs/benchmarks.md` et guides EN/FR.

## Task 1 : contrat minimal et compilation du module

**Interfaces produites :**

```kotlin
enum class BenchmarkProfile(val warmups: Int, val samples: Int) {
    Standard(5, 30), Ci(3, 5)
}
data class TimingSample(val cpuIssueMs: Double, val completionMs: Double)
data class BenchmarkResult(
    val workload: String,
    val size: Int,
    val operationsPerSample: Int,
    val profile: BenchmarkProfile,
    val samples: List<TimingSample>,
)
suspend fun benchmarkWriteBuffer(
    device: GPUDevice, sizeBytes: Int, writes: Int, profile: BenchmarkProfile,
): BenchmarkResult
suspend fun benchmarkCompute(
    device: GPUDevice, elements: Int, dispatches: Int, profile: BenchmarkProfile,
): BenchmarkResult
```

Les deux fonctions sont implémentées aux tâches suivantes ; ne pas livrer de corps vide. Une erreur fait échouer la fonction et le runner enregistre le diagnostic ; un `BenchmarkResult` retourné signifie que les readbacks ont réussi.

- [ ] **1. Ajouter `suite-benchmarks` aux settings** et créer son build en reprenant celui de `suite-demos` : `plugins { kmp; publish }`, JVM 25, JS/browser, Wasm JS/browser, Linux x64, macOS ARM64, opt-in unsigned, `api(project(":suite-core"))` et `implementation(project(":webgpu-descriptors"))`. Pas de bibliothèque de benchmarks supplémentaire.
- [ ] **2. Écrire les trois types** de `BenchmarkResult.kt` ci-dessus ; leur package est celui de la carte des fichiers. Utiliser des `require` pour rejeter paramètres hors des tailles/batches du protocole avant allocation.
- [ ] **3. Ajouter `implementation(project(":suite-benchmarks"))` au runner.** La sérialisation est réalisée dans le runner, avec les plugins existants ; le module portable n’a pas besoin de dépendre de serialization.
- [ ] **4. Compiler** `:suite-benchmarks:compileKotlinJvm`, `:suite-benchmarks:compileKotlinJs` et `:suite-benchmarks:compileKotlinWasmJs`. Ce contrôle est une compilation normale, sans nouveau test d’architecture.

## Task 2 : mesurer les écritures et vérifier leur résultat GPU

**Files:** `WriteBufferBenchmark.kt`, `Readback.kt`. **Produit :** `benchmarkWriteBuffer`.

- [ ] **1. Préparer hors chronométrage.** Allouer un buffer cible `CopyDst or CopySrc` de `sizeBytes` et un staging `MapRead or CopyDst` de même taille, une fois par scénario. Préparer `writes` tableaux `ArrayBuffer` réutilisés : le mot d’index `i` de l’écriture `j` vaut `i.toUInt() xor (0x9e3779b9u + j.toUInt())`. Remplir sur CPU avant les mesures. La mémoire préparée maximale est de 16 MiB pour les données d’entrée.

```kotlin
val input = List(writes) { writeIndex ->
    ArrayBuffer.of(UIntArray(sizeBytes / 4) { index ->
        index.toUInt() xor (0x9e3779b9u + writeIndex.toUInt())
    })
}
```

Vérifier `sizeBytes.toULong() <= device.limits.maxBufferSize`. Toutes les tailles sont multiples de 4. Les buffers, tableaux, collecte de résultats et readback restent hors de la fenêtre CPU.

- [ ] **2. Écrire un échantillon explicitement.** Importer `kotlin.time.TimeSource` ; utiliser la même horloge pour les deux durées :

```kotlin
device.queue.onSubmittedWorkDone().getOrThrow() // hors mesure : vider la queue
val start = TimeSource.Monotonic.markNow()
for (data in input) {
    device.queue.writeBuffer(target, 0uL, data)
}
val cpuMs = start.elapsedNow().inWholeNanoseconds / 1_000_000.0
device.queue.onSubmittedWorkDone().getOrThrow()
val completeMs = start.elapsedNow().inWholeNanoseconds / 1_000_000.0
val sample = TimingSample(cpuMs, completeMs)
```

`writeBuffer` copie depuis des données CPU déjà préparées ; son coût inclut l’appel Kotlin, les conversions du binding et le travail synchrone du navigateur. La mesure ne prétend pas isoler un débit DMA. Les écritures successives ciblent volontairement le même buffer ; la dernière valeur est celle vérifiée.

- [ ] **3. Ajouter `internal suspend fun readUints(device, source, staging, byteSize): UIntArray` dans `Readback.kt`.** Types exacts : `GPUDevice`, deux `GPUBuffer`, `ULong`. Créer un encoder, copier `byteSize` octets de source vers staging, finir/soumettre, fermer command buffer et encoder. Mapper le staging avec `GPUMapMode.Read`, copier avec `getMappedRange().toUIntArray()`, puis unmap dans `finally`. Aucun chronométrage dans ce helper.

- [ ] **4. Vérifier avant et après les mesures.** Faire une première séquence d’écritures non mesurée, puis readback complet. Comparer chaque mot à `index.toUInt() xor (0x9e3779b9u + (writes - 1).toUInt())`, avec `check` et diagnostic index/attendu/observé. Répéter cette vérification après le dernier échantillon. Toute différence invalide le scénario entier ; aucune durée n’est publiée comme exploitable.
- [ ] **5. Exécuter les warm-ups puis les échantillons.** Même code de travail pour les deux, mais ne conserver que les mesures après warm-up. Ajouter le résultat à la liste après le second timestamp. Ne pas afficher la progression ni sérialiser dans la fenêtre. Fermer les buffers dans `finally`.
- [ ] **6. Compiler et commiter tâches 1–2** : `feat: add portable writeBuffer benchmark with GPU readback`.

## Task 3 : mesurer encodage et soumission compute

**Files:** `ComputeBenchmark.kt`, réutilise `Readback.kt`. **Produit :** `benchmarkCompute`.

- [ ] **1. Préparer hors mesure.** Un buffer `Storage or CopySrc` de `elements * 4` octets, staging de même taille, shader, pipeline et bind group. Pipeline automatique, WGSL :

```wgsl
@group(0) @binding(0) var<storage, read_write> output: array<u32>;
@compute @workgroup_size(64)
fn main(@builtin(global_invocation_id) id: vec3u) {
    if (id.x >= arrayLength(&output)) { return; }
    output[id.x] = id.x * 3u + 7u;
}
```

Créer via `ShaderModuleDescriptor`, `ComputePipelineDescriptor(compute = ProgrammableStage(shader, entryPoint = "main"))`, `pipeline.getBindGroupLayout(0u)`, puis `BindGroupDescriptor(layout, listOf(BindGroupEntry(0u, BufferBinding(output))))`. Utiliser `use`/`finally` pour tous les objets possédés `AutoCloseable`, jamais sur une passe compute.

Les tailles requises doivent respecter `maxBufferSize`, `maxStorageBufferBindingSize`, `maxComputeWorkgroupsPerDimension`, `maxComputeWorkgroupSizeX` et `maxComputeInvocationsPerWorkgroup`. Une absence de limite requise est un échec explicite de scénario ; ne pas réduire silencieusement la charge.

- [ ] **2. Chronométrer uniquement le chemin annoncé.**

```kotlin
device.queue.onSubmittedWorkDone().getOrThrow()
val start = TimeSource.Monotonic.markNow()
val encoder = device.createCommandEncoder()
var commands: GPUCommandBuffer? = null
try {
    val pass = encoder.beginComputePass()
    pass.setPipeline(pipeline)
    pass.setBindGroup(0u, group)
    repeat(dispatches) { pass.dispatchWorkgroups(((elements + 63) / 64).toUInt()) }
    pass.end()
    val finished = encoder.finish()
    commands = finished
    device.queue.submit(listOf(finished))
    val cpuMs = start.elapsedNow().inWholeNanoseconds / 1_000_000.0
    device.queue.onSubmittedWorkDone().getOrThrow()
    val completeMs = start.elapsedNow().inWholeNanoseconds / 1_000_000.0
    // Conserver TimingSample(cpuMs, completeMs) seulement après ces deux lectures.
} finally {
    commands?.close()
    encoder.close()
}
```

Le coût CPU inclut allocations de l’encoder/command buffer, encodage des dispatches et soumission. Il exclut création du pipeline/bind group, lecture mémoire, destruction après timestamp et préparation. Les dispatches écrivent les mêmes valeurs ; il s’agit d’une charge simple, pas d’un modèle général d’application GPU.

- [ ] **3. Vérifier les résultats avant et après mesure.** Exécuter une séquence non mesurée, readback complet, comparer `actual[i] == i.toUInt() * 3u + 7u`. Répéter après tous les échantillons. Garder les assertions indépendantes du shader, sans lire un résultat précédent comme référence attendue.
- [ ] **4. Appliquer les mêmes warm-ups et stockage d’échantillons**, sans inclure leur agrégation dans la fenêtre. Retourner `BenchmarkResult("compute.encode-submit", elements, dispatches, profile, samples)` uniquement après succès des vérifications.
- [ ] **5. Compiler et commiter** : `feat: measure compute encoding submission and completion`.

## Task 4 : runner navigateur et protocole de rapport

**Files:** les trois fichiers du package navigateur `benchmarks`, `Main.kt`, ressources `benchmarks/texts.{en,fr}.json` et `index.html` de la distribution.

**Interfaces :** `suspend fun runBenchmarks(profile: BenchmarkProfile): BenchmarkReport` et `suspend fun showBenchmarksPage(locale: String)`. Route `?benchmark=foundations&profile=standard|ci`. Un flag `autorun=1` est réservé au lancement outillé ; le parcours utilisateur affiche un bouton Démarrer.

- [ ] **1. Étendre le choix de route explicitement.** Si `benchmark` et `demo` sont présents simultanément, afficher une erreur de paramètres. `benchmark=foundations` ouvre la nouvelle page ; valeur ou profil inconnu = erreur visible. Sans ces paramètres conserver le lancement de validation existant. Réutiliser `queryParameter` et les helpers DOM existants sans refactor global ; l’emplacement actuel de ces helpers sous `demos` ne justifie pas une nouvelle architecture.
- [ ] **2. Définir les DTO sérialisables.** Un scénario possède un ID déterministe : `transfer.write-buffer.bytes-4096.batch-1`, etc., ou `compute.encode-submit.elements-1024.batch-1`. Inclure : workload, unité de taille (`bytes`/`elements`), taille, opérations par batch, profil, warm-ups, nombre prévu de mesures, liste brute de paires de durées, `status`, `diagnostic`, `outputVerified`, description adapter, `isFallbackAdapter`, features et limites utilisées.

Le rapport racine contient `schemaVersion=1`, `protocol="foundations-v1"`, profil, horloge `"kotlin.time.TimeSource.Monotonic"`, ordre des dix IDs et résultats, date/versions/commit via l’enveloppe habituelle. Durées en millisecondes. Les DTO du runner copient les résultats portables, sans imposer serialization à `suite-benchmarks`.

Statuts : `completed` avec `outputVerified=true`, `failed` en cas de défaut GPU/initialisation/timeout, `interrupted` si masquage ou annulation utilisateur, `not-run` pour les scénarios restants. Seuls les résultats `completed` sont agrégés. Pas de statut « rapide », « lent » ou « conforme ».

- [ ] **3. Isoler les dix scénarios.** Un adapter puis un device par scénario, acquisition et fermeture hors mesure. Aucun canvas ni animation pendant le benchmark. Installer `onUncapturedError` et un scope Validation couvrant préparation, warm-up, mesures et readbacks. Après le travail, vider la queue, terminer le scope et vérifier le callback avant de conserver `completed`. Une erreur invalide les mesures de ce scénario ; continuer les autres après fermeture, sauf interruption globale.

Timeout réel : 90 secondes par scénario et 16 minutes pour la campagne. Attraper `TimeoutCancellationException` avant `CancellationException`. Sur annulation/masquage, arrêter toute planification, fermer les ressources et marquer les résultats incomplets ; ne pas reprendre une campagne suspendue comme si elle était continue. Le chronométrage n’utilise pas `runTest` ni une horloge virtuelle.

- [ ] **4. Traiter le navigateur visible.** Au départ exiger `document.visibilityState === "visible"`. Un listener `visibilitychange` invalide la campagne dès que l’onglet devient caché. Boutons Démarrer et Annuler ; désactiver le lancement concurrent. Afficher la progression uniquement entre scénarios. La pause/reprise n’est pas proposée. Conserver l’ancien résultat avec sa date si une nouvelle campagne est annulée, sans le présenter comme le nouveau résultat.
- [ ] **5. Préserver les observations d’horloge.** Garder les zéros et les valeurs identiques ; rejeter seulement valeurs négatives ou non finies. Un échantillon sous la résolution pratique de l’horloge doit être affiché comme peu résolu ; ne pas augmenter automatiquement le batch, filtrer l’échantillon ou produire un débit en divisant par zéro. Publier le nombre d’échantillons à `cpuIssueMs == 0`.
- [ ] **6. Publier le JSON local** dans `globalThis.graphiksBenchmarkReport` via le même mécanisme que les autres rapports ; offrir son téléchargement. Les textes EN/FR expliquent les deux durées, l’exclusion de préparation/readback, le profil, la dépendance au runtime et à l’environnement. Les résultats locaux ne remplacent pas les rapports publiés.
- [ ] **7. Lancer JS et Wasm**, profil CI puis standard sur l’environnement disponible, et constater les dix scénarios, les readbacks vérifiés, l’arrêt par annulation et le refus d’un lancement sans WebGPU. Aucun test du DOM ou du format pour lui-même. Commiter `feat: run browser benchmark campaigns with explicit protocol`.

## Task 5 : collecte, statistiques descriptives et page Benchmarks

**Files:** `tools/run-browser.mjs`, `tools/build-site.mjs`, `site/benchmarks/{index.html,app.js}`, navigation existante et styles.

- [ ] **1. Ajouter le mode `--benchmark` au collecteur.** Incompatible avec `--demo-check`. Options `--profile=ci|standard` (défaut `ci` dans cet outil) et `--backend=swiftshader|default` (défaut `swiftshader`). Le backend `default` laisse Chromium choisir sans flags forçant SwiftShader ; le rapport dit `default`, pas « matériel ». En CI conserver le mode logiciel explicite.

Route outillée : `?benchmark=foundations&profile=<profil>&autorun=1`. Attendre `graphiksBenchmarkReport` avec timeout 17 minutes. Écrire `build/reports/benchmarks-<target>.json`, incluant profil, flags de lancement, version Chromium, OS, user agent, mode headless, commit et baseline. JS/Wasm séquentiels, jamais en parallèle sur la même machine pendant la mesure.

- [ ] **2. Appliquer la garde de lancement adaptée.** Exiger les dix IDs attendus, sans doublon, `completed`, `outputVerified`, le bon profil et le nombre d’échantillons correspondant. Une erreur de page, erreur fatale ou résultat incomplet fait sortir non zéro. Aucun seuil sur la valeur des durées. Garder intactes les règles des deux modes existants. Les gardes de lancement ne donnent pas lieu à des tests d’architecture supplémentaires.

```sh
rtk proxy node tools/run-browser.mjs js suite-browser/build/dist/js/productionExecutable --benchmark --profile=ci --backend=swiftshader
rtk proxy node tools/run-browser.mjs wasm suite-browser/build/dist/wasmJs/productionExecutable --benchmark --profile=ci --backend=swiftshader
```

- [ ] **3. Ajouter la page EN/FR et la navigation.** Reprendre les mécanismes de locale et `textContent` du site. Charger les deux rapports et afficher séparément par cible, profil et scénario : taille, batch, warm-ups, nombre de mesures, résultat du contrôle GPU, médiane et intervalle min/max de chaque durée. P95 seulement pour au moins 20 échantillons, avec convention explicite « rang le plus proche supérieur » : index `ceil(0.95*n)-1` après tri. Médiane = valeur centrale ou moyenne des deux centrales. Les échantillons bruts sont téléchargeables ; aucune suppression d’outlier.

```javascript
const sorted = values.toSorted((a, b) => a - b);
const middle = Math.floor(sorted.length / 2);
const median = sorted.length % 2
  ? sorted[middle]
  : (sorted[middle - 1] + sorted[middle]) / 2;
const p95 = sorted.length >= 20 ? sorted[Math.ceil(sorted.length * 0.95) - 1] : null;
```

Appeler ce calcul seulement pour une liste non vide de valeurs finies d’un scénario terminé. La page détaille ce qui est mesuré ; `completionMs - cpuIssueMs` n’est jamais étiqueté GPU. Pas de score global, ratio JS/Wasm, vitesse « x fois plus rapide » ou pourcentage de régression. Afficher les résultats du profil CI comme observations de fonctionnement, avec la mention backend logiciel demandé lorsque les flags l’imposent. Une description d’adapter vide reste inconnue.

- [ ] **4. Ajouter les liens de lancement local** `../run/js/?benchmark=foundations&lang=…` et équivalent Wasm. Profil standard par défaut pour l’interface utilisateur ; bouton Démarrer avant les mesures. Lier le protocole et les sources au commit de l’enveloppe. Une absence de rapport affiche « aucune mesure publiée », jamais une série de zéros.
- [ ] **5. Étendre l’assemblage du site** aux deux rapports `benchmarks-js.json` et `benchmarks-wasm.json`. Ils sont requis pour la livraison complète de ce lot ; les téléchargements de JSON et le site restent sous `_site/suite/`. Pas de nouveau déploiement Pages.
- [ ] **6. Inspecter le site assemblé** sous le préfixe réel et sur mobile, profils CI et standard, liens, unités et localisation. Comparer manuellement une médiane affichée aux valeurs brutes d’une vraie campagne ; aucun framework de tests statistiques/présentation. Commiter `feat: publish contextualized WebGPU benchmark results`.

## Task 6 : CI, publication et documentation d’usage

**Files:** `.github/workflows/suite.yml`, `.github/workflows/publish.yml`, `buildSrc/src/main/kotlin/publish.gradle.kts`, `README.md`, `docs/benchmarks.md`, `docs/running.md`, `docs/verification.md`, guides Architecture et Suite EN/FR.

- [ ] **1. Étendre le workflow Suite.** Ajouter la compilation JVM du nouveau module et les deux commandes `--benchmark --profile=ci --backend=swiftshader`, après les démos et avant l’upload de rapports/assemblage. Laisser une durée de job suffisante (40 minutes), les rapports collectés avec `always()`, les exécutions séparées avec `!cancelled()`. Un échec de correction ou de lancement fait échouer la livraison ; une variation de durée ne la bloque pas.
- [ ] **2. Publier par la convention existante.** Ajouter `"suite-benchmarks" -> "Portable workloads and measurements for the Graphiks WebGPU API"` aux descriptions POM et `:suite-benchmarks:publishToMavenCentral` aux commandes snapshot/release. Coordonnée `org.graphiks:suite-benchmarks`, version commune, mêmes cinq cibles que les autres contenus de la suite. Compilation normale de chaque cible sur hôte compatible, sans test de consommation Maven.
- [ ] **3. Documenter le protocole dans `docs/benchmarks.md`.** Reprendre `foundations-v1`, la table des dix scénarios, frontières temporelles exactes, warm-up, synchronisation préalable, lecture de contrôle, ordre d’exécution, profils et limites d’interprétation. Toute modification future du workload ou des frontières temporelles change l’identifiant de protocole ; ne pas comparer silencieusement des protocoles différents.
- [ ] **4. Montrer l’usage externe concret.** Le binding fournit un device isolé puis appelle `benchmarkWriteBuffer(device, 65536, 16, BenchmarkProfile.Standard)` ou `benchmarkCompute(device, 65536, 16, BenchmarkProfile.Standard)`. Il gère les scopes, timeouts, erreurs non capturées et métadonnées comme le runner navigateur. Les fonctions ne ferment pas le device, mais ferment les ressources qu’elles allouent. Aucun runner natif ajouté ici.
- [ ] **5. Vérifier les workloads par exécution réelle.** Observer les readbacks avant/après sur les deux cibles. Pour démontrer la pertinence d’une attente, modifier temporairement le shader en remplaçant `+7u` par `+8u`, constater l’échec du contrôle, puis restaurer et relancer les scénarios compute. Cette manipulation est un contrôle du résultat WebGPU, pas un test de l’architecture. Ne pas commiter la mutation.
- [ ] **6. Compléter `docs/verification.md`.** Commit, commandes réellement exécutées, navigateur, profil, backend demandé, résultats de correction, nombre d’échantillons, limitations d’horloge/environnement et compilations observées. Ne pas recopier les résultats historiques comme nouveaux. Les données chiffrées canoniques restent les rapports avec échantillons bruts, pas un tableau de performances promis dans le README.
- [ ] **7. Lancer les contrôles finaux nécessaires**, dont les onze acid tests et les deux vérifications de démo par cible si le runner a changé ; compiler/lancer le site complet et exécuter `rtk git diff --check`. Commiter `feat: integrate benchmark publication and document the protocol`.

## Revue et transmission

| Exigence | Tâches |
| --- | --- |
| Artifact portable, conventions et module du même dépôt | 1 et 6 |
| Transferts CPU → GPU | 2 |
| Encodage/soumission compute | 3 |
| Durées CPU et achèvement distinctes | 2 à 5 |
| Résultats mémoire vérifiés hors mesure | 2, 3 et 6 |
| Warm-up, répétitions, données brutes et environnement | 1 à 5 |
| JS/Wasm séparés, CI logicielle identifiée | 4 à 6 |
| Page EN/FR, lancement local et publication commune | 5 et 6 |
| Timestamps GPU et mesures de particules | Compléments futurs, hors de ce lot |

- [x] Départ aligné sur le merge des démos `a0287f1`, pas sur des fichiers proposés mais absents.
- [x] Les cinq risques de Review Focus possèdent une règle de mesure, une vérification GPU ou un lancement attribué.
- [x] Aucune création de projet consommateur ou de test de l’architecture.
- [x] Les timings sont explicitement des observations CPU/achèvement, pas du temps GPU pur.
- [ ] Revue du plan par l’utilisateur avant transmission à l’agent d’exécution.

**Transmission :** lire spec et plan, exécuter dans ce dépôt en préservant les changements existants. Implémenter les workloads lisiblement ; réutiliser le runner sans l’élargir en framework. Compiler, lancer les workloads réels et consigner les observations. Une durée n’est exploitable que si le travail GPU et le contexte d’exécution sont valides. Les variations de performance n’introduisent pas de gate CI dans ce lot.
