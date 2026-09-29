# Graphiks WebGPU Suite — Plan 1 : fondations et validation navigateur

> **Statut au 2026-09-28 : incrément intégré dans `b4d750a` (PR #120). Document historique, ne pas rejouer les tâches ci-dessous.** Les checkboxes et extraits conservent la proposition initiale ; ils ne constituent pas un relevé des commandes effectivement exécutées. Pour les opérations courantes, lire `docs/running.md` et `docs/adding-a-case.md` ; pour les résultats consignés, lire `docs/verification.md`.

## Écarts intégrés par rapport à ce plan

- Les cas sont organisés à raison d’un fichier par cas, sous les packages `buffers`, `transfers`, `compute` et `errors`. Les helpers partagés restent limités (`ValidationScope.kt`, `ComputeSupport.kt`).
- `AcidCase` porte un `AcidCaseId` et une `AcidFamily`, sans champ `title`. Les fonctions portent `@AcidTest`, avec des références au contrat via `ApiSymbols` généré.
- `FoundationCases.kt` et le manifeste des IDs sont générés, et non rédigés ou synchronisés manuellement.
- L’inventaire n’est pas maintenu dans les fichiers versionnés proposés à la tâche 5 : le plugin `org.graphiks.webgpu-suite-inventory` génère les sources et manifestes dans `suite-acid-tests/build/`, puis `tools/build-inventory.mjs` assemble l’inventaire du site.
- Les textes EN/FR sont dans `inventory/i18n/behaviours.<locale>.json`. Les métadonnées des comportements sans cas sont dans `inventory/uncovered-behaviours.json`.
- Les trois modules, le workflow `suite.yml` et son intégration aux workflows existants sont livrés. Aucun projet de consommation Maven dédié n’a été ajouté.
- Onze cas sont présents. Les ressources décrivent 42 comportements (11 avec cas, 31 sans cas). Démos et benchmarks restent les prochains incréments.

L’amendement [annotations et i18n](2026-09-28-suite-annotations-i18n.md) décrit les choix de génération intégrés. La [spec actualisée](../specs/2026-09-27-webgpu-suite-design.md) fait référence pour la suite du travail.

---

## Plan initial conservé pour traçabilité

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking. L’utilisateur transmet ce document à un autre agent pour exécution dans un worktree du même dépôt `webgpu`. Le worktree de rédaction reste documentaire.

**Goal:** Livrer un premier incrément utilisable de Graphiks WebGPU Suite : inventaire public du contrat, acid tests portables sur les fondations, artifacts consommables et exécution navigateur JS/Wasm avec rapports consultables.

**Architecture:** Deux sous-modules KMP (`suite-core`, `suite-acid-tests`) exposent des cas lisibles prenant un `GPUDevice`. Ils réutilisent les conventions Gradle `kmp` et `publish` du dépôt. `suite-browser` consomme le projet `webgpu-browser` et exécute les cas sur un vrai navigateur. Une section statique du site existant présente l’inventaire et les rapports, sans framework applicatif ni framework de tests maison.

**Tech Stack:** Kotlin Multiplatform 2.4.20, JDK 25, Gradle 9.8.0, coroutines 1.11.0, kotlinx.serialization JSON 1.11.0, `kotlin.test`, plugin Maven Publish 0.36.0, Node.js 22, Playwright 1.55.1 (Chromium installé par Playwright), HTML/CSS/JavaScript natifs, GitHub Actions/Pages.

**Spec:** `docs/superpowers/specs/2026-09-27-webgpu-suite-design.md` — approuvée. Transmettre la spec avec ce plan.

## Global Constraints

- Dépôt cible : ce même dépôt `Graphiks-org/webgpu`. Sous-modules Gradle à la racine, inclus dans le build existant ; aucun dépôt séparé ni sous-module Git.
- Réutiliser `kmp`, `publish`, le wrapper, le catalogue `libs` et `releaseVersion`. Les dépendances internes sont des `project(...)`. Aucun projet dédié à la vérification de publication ou de consommation Maven.
- API publique commune uniquement ; extensions et résultats natifs dans les dépôts des bindings.
- Le code doit être lisible par les humains et centré sur l’usage et les comportements WebGPU.
- Compilation, résolution des dépendances et lancement valident l’assemblage ; aucun test dédié à la structure des modules, au graphe de dépendances ou au câblage interne.
- Les acid tests sont eux-mêmes le livrable : ne pas ajouter des tests qui testent les tests, ni des mocks d’un GPU pour valider le runner.
- Les assertions doivent porter sur des données, erreurs ou états observables. Un échec réel du binding doit rester visible, sans adapter l’attente au défaut observé.
- Les exemples de code de ce plan sont des instructions, pas une preuve de compilation ou de passage GPU.
- Les bibliothèques partagées ne dépendent ni du navigateur ni d’un binding concret.
- JS et Wasm ont des résultats distincts. Absence de GPU, rejet, timeout et succès ne sont jamais confondus.
- Préfixer les commandes shell locales avec `rtk` ; utiliser `rtk proxy` pour les commandes non prises en charge. Les workflows CI utilisent les commandes natives.
- Documenter les régressions révélées dans le compte rendu et les faire traiter dans le chantier navigateur du même dépôt, sans masquer les défauts dans les acid tests.

## Review Focus

1. Offsets exprimés en octets : un sous-ensemble écrit/copié doit donner les bons mots et préserver les sentinelles — tâche 3.
2. Mapping partiel, unmapping et remapping : lire une plage valide sans réutiliser une vue invalidée — tâche 3.
3. Layout automatique et explicite, constantes de spécialisation : calculer les mêmes valeurs attendues sur GPU — tâche 4.
4. Rejet de mapping et scopes vides/invalides : distinguer `Result.failure`, erreur de validation capturée et `Result.success(null)` — tâche 4.
5. Adapter absent, échec d’initialisation, erreur inattendue ou timeout : publier un diagnostic et échouer la commande CI — tâches 2 et 6, par lancement réel et cas GPU, sans tests de l’architecture du runner.

---

## Périmètre de ce premier plan

Ce lot est un premier incrément de la phase « fondations », pas la livraison de toute la spec. Il livre une page Validation utilisable et deux artifacts :

- `org.graphiks:suite-core` ;
- `org.graphiks:suite-acid-tests`.

Version initiale commune : `0.1.0-SNAPSHOT`, surchargeable par `releaseVersion`.

Matrice initiale de ces deux artifacts : JS, Wasm JS, JVM 25, Linux x64 et macOS ARM64. Les modules sont compilés sur les cibles annoncées via leur build normal ; les exécutions de backend natif appartiennent aux dépôts des bindings. Les autres cibles de l’API pourront être ajoutées progressivement ; ne pas annoncer leur support à ce stade.

Les prochains plans de la même phase livreront `suite-demos` avec la scène de particules et la galerie, puis `suite-benchmarks` avec protocoles et présentation des mesures. Ne pas créer ici des modules vides ou des pages qui simulent leur disponibilité. Cette découpe conserve le périmètre de la spec et permet de livrer un premier résultat vérifiable.

## Contexte vérifié et préparation

Le code navigateur a été examiné au commit `f1b0e1bc5783e3daf6af8315da512f268014e4e4` de `webgpu`. Les modules et helpers sont présents ; les tests navigateur et le build GPU autonome annoncés dans le plan de rapatriement ne sont pas présents dans cette baseline. La publication effective et les exécutions GPU n’ont pas été vérifiées pendant la rédaction.

- [ ] Lire la spec, ce plan, les instructions locales et `/Users/chaos/.codex/RTK.md` si disponible.
- [ ] Ouvrir un worktree d’exécution du dépôt actuel `webgpu`, contenant la spec et ce plan. Aucun clone d’un dépôt `webgpu-suite` n’est requis.
- [ ] Vérifier l’état du checkout avec `rtk git status --short` ; préserver le travail existant et adapter les créations de fichiers si des modules de la suite existent déjà.
- [ ] Lire le plan navigateur comme historique des prérequis, sans exécuter à nouveau ses tâches. Vérifier `buildSrc/src/main/kotlin/{kmp,publish}.gradle.kts`, `gradle/libs.versions.toml`, `settings.gradle.kts` et les workflows `docs.yml` et `publish.yml` existants.
- [ ] Identifier le commit source de référence et la version `releaseVersion` du build. Les modules locaux suffisent pour développer et exécuter la suite ; une publication Maven préalable du navigateur n’est pas requise.
- [ ] Inscrire version et commit de référence dans `inventory/baseline.json`. Si le code a évolué depuis la baseline citée, vérifier les signatures avant de recopier les exemples. Les rapports enregistrent aussi le commit de l’exécution, qui peut inclure des changements atomiques de l’API et de la suite.

## Carte des fichiers

Tous les chemins suivants sont relatifs à la racine du dépôt actuel `webgpu`.

| Fichiers | Rôle |
| --- | --- |
| `settings.gradle.kts`, `buildSrc/src/main/kotlin/publish.gradle.kts` | Inclusion des modules et descriptions de publication ; wrapper, catalogue et version racine existants |
| `suite-core/build.gradle.kts`, `src/commonMain/kotlin/org/graphiks/webgpu/suite/AcidCase.kt` | Contrat minimal d’un cas |
| `suite-acid-tests/build.gradle.kts`, `src/commonMain/kotlin/org/graphiks/webgpu/suite/acid/*.kt` | Scénarios GPU et catalogue explicite |
| `suite-browser/build.gradle.kts`, `src/commonMain/kotlin/org/graphiks/webgpu/suite/browser/*.kt` | Exécution, rapport JSON et point d’entrée |
| `suite-browser/src/commonMain/resources/index.html` | Lancement navigateur et résultat local |
| `inventory/baseline.json`, `inventory/contract.md`, `inventory/symbols.tsv`, `inventory/behaviors.json` | Version de référence, inventaire et couverture |
| `tools/package.json`, `tools/package-lock.json`, `tools/run-browser.mjs` | Exécution Chromium et collecte des rapports |
| `tools/build-site.mjs`, `site/index.html`, `site/app.js`, `site/style.css` | Publication statique de l’inventaire et des résultats |
| `.github/workflows/suite.yml`, `.github/workflows/{test,docs,publish}.yml` | Exécution de la suite intégrée aux workflows existants, déploiement Pages commun et publication |
| `README.md`, `docs/running.md`, `docs/adding-a-case.md`, `docs/verification.md` | Utilisation et preuves observées |

Les fichiers Kotlin des scénarios portent le package `org.graphiks.webgpu.suite.acid`. Écrire des imports explicites dans le code livré ; les blocs courts du plan utilisent parfois des noms non qualifiés pour rester lisibles.

Chemins Kotlin complets à utiliser dans les tâches ci-dessous :

```text
suite-core/src/commonMain/kotlin/org/graphiks/webgpu/suite/AcidCase.kt
suite-acid-tests/src/commonMain/kotlin/org/graphiks/webgpu/suite/acid/BufferCases.kt
suite-acid-tests/src/commonMain/kotlin/org/graphiks/webgpu/suite/acid/TransferCases.kt
suite-acid-tests/src/commonMain/kotlin/org/graphiks/webgpu/suite/acid/ComputeCases.kt
suite-acid-tests/src/commonMain/kotlin/org/graphiks/webgpu/suite/acid/ErrorCases.kt
suite-acid-tests/src/commonMain/kotlin/org/graphiks/webgpu/suite/acid/ValidationScope.kt
suite-acid-tests/src/commonMain/kotlin/org/graphiks/webgpu/suite/acid/FoundationCases.kt
suite-browser/src/commonMain/kotlin/org/graphiks/webgpu/suite/browser/Main.kt
suite-browser/src/commonMain/kotlin/org/graphiks/webgpu/suite/browser/Report.kt
suite-browser/src/commonMain/kotlin/org/graphiks/webgpu/suite/browser/RunFoundations.kt
```

## Task 1 : livrer un premier cas portable compilable et un consommateur navigateur

**Files:** modifier `settings.gradle.kts` ; créer les trois builds de sous-modules, `suite-core/.../AcidCase.kt`, `suite-acid-tests/.../BufferCases.kt`, `suite-acid-tests/.../FoundationCases.kt`, `suite-browser/.../Main.kt` et `suite-browser/src/commonMain/resources/index.html`.

**Interfaces:**
- Consomme les projets locaux `:webgpu-api`, `:webgpu-descriptors`, `:webgpu-browser`.
- Produit `AcidCase(id, title, contract, requiredFeatures, run)` et `foundationCases(): List<AcidCase>`.
- Les cas empruntent un `GPUDevice` et ferment leurs ressources ; le runner ferme le device et l’adapter.

- [ ] **1. Étendre le build existant.** Ajouter uniquement ces inclusions dans `settings.gradle.kts`, en conservant le nom racine, les repositories, le générateur et tous les modules existants :

```kotlin
include("suite-core", "suite-acid-tests", "suite-browser")
```

Le build racine fournit déjà `group = "org.graphiks"` et `releaseVersion` avec défaut `0.1.0-SNAPSHOT`. Conserver cette configuration et le wrapper. Le catalogue fournit déjà Kotlin, coroutines, serialization-json et Maven Publish ; utiliser ces entrées existantes. Les deux bibliothèques appliquent les conventions existantes :

```kotlin
plugins {
    kmp
    publish
}
```

- [ ] **2. Définir les deux builds de bibliothèques.** `kmp` applique actuellement le plugin multiplatform, sans définir les cibles. Comme dans les modules existants, déclarer les cibles explicitement ; `publish` prend en charge les métadonnées et la signature :

```kotlin
kotlin {
    jvmToolchain(25)
    jvm()
    js { browser() }
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs { browser() }
    linuxX64()
    macosArm64()
    compilerOptions.optIn.add("kotlin.ExperimentalUnsignedTypes")
}
```

Dans `suite-core`, `commonMain.dependencies` contient `api(project(":webgpu-api"))`. Dans `suite-acid-tests`, il contient `api(project(":suite-core"))`, `implementation(kotlin("test"))` et `implementation(project(":webgpu-descriptors"))`. Les assertions sont livrées avec les cas, donc dans `commonMain`, pour être exécutées par les consommateurs.

- [ ] **3. Définir le contrat minimal dans `AcidCase.kt`.**

```kotlin
package org.graphiks.webgpu.suite

import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUFeatureName

data class AcidCase(
    val id: String,
    val title: String,
    val contract: List<String>,
    val requiredFeatures: Set<GPUFeatureName> = emptySet(),
    val run: suspend (GPUDevice) -> Unit,
)
```

Tous les cas de ce plan fonctionnent avec les limites garanties par défaut ; ne pas introduire de DSL de limites, d’injection de dépendances, de registre par réflexion ou de moteur de scènes. `requiredFeatures` désigne uniquement des features optionnelles, pas un moyen d’ignorer un membre obligatoire du contrat.

- [ ] **4. Écrire le premier acid test : buffer mappé à la création.** Dans `BufferCases.kt` :

```kotlin
suspend fun mappedAtCreation(device: GPUDevice) {
    val buffer = device.createBuffer(BufferDescriptor(
        size = 16uL,
        usage = GPUBufferUsage.CopySrc,
        mappedAtCreation = true,
    ))
    try {
        assertEquals(16uL, buffer.size)
        assertEquals(setOf(GPUBufferUsage.CopySrc), buffer.usage)
        assertEquals(GPUBufferMapState.Mapped, buffer.mapState)
        val range = buffer.getMappedRange()
        range.setUInts(0uL, uintArrayOf(11u, 22u, 33u, 44u))
        assertContentEquals(uintArrayOf(11u, 22u, 33u, 44u), range.toUIntArray())
        buffer.unmap()
        assertEquals(GPUBufferMapState.Unmapped, buffer.mapState)
    } finally {
        buffer.close()
    }
}
```

`FoundationCases.kt` retourne une liste explicite contenant `AcidCase("buffers.mapped-at-creation", "Write initial buffer contents", listOf("GPUDevice.createBuffer", "GPUBuffer.getMappedRange", "GPUBuffer.unmap", "GPUBuffer.size", "GPUBuffer.usage", "GPUBuffer.mapState"), run = ::mappedAtCreation)`.

- [ ] **5. Rendre le cas lançable dans le navigateur.** `suite-browser` applique `kmp` et `alias(libs.plugins.kotlin.serialization)`, mais pas `publish` car c’est une application. Déclarer `js { browser { commonWebpackConfig { outputFileName = "suite.js" } }; binaries.executable() }` et la même configuration pour `wasmJs` avec l’opt-in Wasm. `commonMain` dépend de `project(":suite-acid-tests")`, `project(":webgpu-browser")`, `project(":webgpu-descriptors")`, `libs.coroutines` et `libs.kotlinx.serialization.json`. Le point d’entrée provisoire lance le cas :

```kotlin
fun main() {
    MainScope().launch {
        val adapter = requestAdapter().getOrThrow()
        try {
            val device = adapter.requestDevice().getOrThrow()
            try {
                foundationCases().single().run(device)
                showResult("PASS buffers.mapped-at-creation")
            } finally { device.close() }
        } finally { adapter.close() }
    }
}

private fun showResult(value: String): Unit =
    js("{ document.getElementById('result').textContent = value; }")
```

Ajouter l’opt-in `kotlin.js.ExperimentalWasmJsInterop`. `index.html` contient un titre, `<pre id="result">Running…</pre>` et `<script src="suite.js"></script>`. La tâche 2 remplace ce lancement provisoire par un rapport complet qui capture aussi les échecs.

- [ ] **6. Compiler puis lancer le cas sur JS et Wasm.**

```sh
rtk proxy ./gradlew :suite-core:compileKotlinJvm :suite-acid-tests:compileKotlinJvm :suite-browser:jsBrowserDistribution :suite-browser:wasmJsBrowserDistribution
rtk proxy ./gradlew :suite-browser:jsBrowserDevelopmentRun
```

Ouvrir l’URL servie, constater la valeur réelle, puis arrêter le serveur et lancer `:suite-browser:wasmJsBrowserDevelopmentRun`. Les projets internes sont construits au même commit. Un téléchargement ou une compilation ne compte pas comme exécution GPU.

- [ ] **7. Commiter le premier parcours utilisable.** `rtk git add` sur les fichiers de cette tâche puis `rtk git commit -m "feat: run the first portable WebGPU acid case"`.

## Task 2 : produire des résultats honnêtes, isolés et bornés dans le temps

**Files:** créer `suite-browser/.../Report.kt`, `suite-browser/.../RunFoundations.kt` ; remplacer `Main.kt`.

**Interfaces:** produit `CaseResult`, `BrowserReport` et `suspend fun runFoundations(): BrowserReport`. Le runner publie son JSON dans `globalThis.graphiksSuiteReport` et affiche le même contenu dans `#result`.

- [ ] **1. Écrire un format de rapport simple.** Annoter avec `@Serializable` :

```kotlin
data class CaseResult(
    val id: String,
    val status: String,
    val diagnostic: String? = null,
    val adapterDescription: String? = null,
)

data class BrowserReport(
    val schemaVersion: Int = 1,
    val cases: List<CaseResult>,
    val fatalError: String? = null,
)
```

Valeurs de `status` : `passed`, `failed`, `unsupported`, `not-run`. Le collecteur de la tâche 6 ajoutera les versions et l’environnement dans une enveloppe. Ce format sert aux rapports navigateur ; il n’impose pas un service de collecte aux dépôts natifs.

- [ ] **2. Exécuter chaque cas sur un nouvel adapter et un nouveau device.** Le contrat actuel décrit `requestDevice` comme une opération qui consomme l’adapter : ne pas demander plusieurs devices au même adapter. Implémenter `runFoundations` avec une boucle `for` explicite, sans paralléliser les cas GPU. Pour chaque cas, utiliser un `withTimeout(30.seconds)` et ce déroulement :

```kotlin
val adapter = requestAdapter().getOrThrow()
try {
    val missing = case.requiredFeatures - adapter.features
    if (missing.isNotEmpty()) {
        CaseResult(case.id, "unsupported", "Missing optional features: $missing")
    } else {
        val uncapturedErrors = mutableListOf<String>()
        val device = adapter.requestDevice(DeviceDescriptor(
            requiredFeatures = case.requiredFeatures.toList(),
            onUncapturedError = GPUUncapturedErrorCallback {
                uncapturedErrors.add(it.message)
            },
        )).getOrThrow()
        try {
            case.run(device)
            device.queue.onSubmittedWorkDone().getOrThrow()
            delay(50) // laisse au navigateur un tour d'événements pour les callbacks
            check(uncapturedErrors.isEmpty()) { uncapturedErrors.joinToString("\n") }
            CaseResult(case.id, "passed", adapterDescription = adapter.info.description)
        } finally { device.close() }
    }
} finally { adapter.close() }
```

La temporisation ne prétend pas prouver l’absence de toute erreur tardive ; les cas valides utilisent aussi un scope explicite, défini à la tâche 3. Encadrer ce bloc par les catches dans cet ordre : `TimeoutCancellationException` → résultat `failed` avec durée limite ; `CancellationException` → propager l’annulation ; `Throwable` → résultat `failed` avec `stackTraceToString()`. Le timeout inclut l’acquisition, la demande du device et les opérations GPU. La durée est réelle : ne pas utiliser l’horloge virtuelle de `runTest` pour piloter le runner.

- [ ] **3. Publier le rapport même en cas d’échec global.** `Main.kt` :

```kotlin
fun main() {
    MainScope().launch {
        val report = try {
            runFoundations()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Throwable) {
            BrowserReport(
                cases = foundationCases().map {
                    CaseResult(it.id, "not-run", "Runner could not complete")
                },
                fatalError = failure.stackTraceToString(),
            )
        }
        publishReport(Json.encodeToString(report))
    }
}

private fun publishReport(value: String): Unit = js("""{
    globalThis.graphiksSuiteReport = value;
    document.getElementById('result').textContent = value;
}""")
```

- [ ] **4. Vérifier par lancement.** Lancer les distributions JS et Wasm, observer un résultat par cas et un rapport JSON. Lancer aussi dans un navigateur où WebGPU est désactivé : le résultat doit signaler l’échec d’initialisation, pas `passed` ni `unsupported`. Aucun test de mock du runner n’est demandé.
- [ ] **5. Commiter.** `rtk git commit -m "feat: report browser acid results and initialization failures"` après ajout des seuls fichiers concernés.

## Task 3 : vérifier buffers, mapping et offsets par lecture mémoire

**Files:** compléter `BufferCases.kt`, créer `TransferCases.kt`, `ValidationScope.kt`, compléter `FoundationCases.kt`.

**Interfaces:** produit `withValidationScope(device, block)`, `bufferCopyOffsets(device)`, `queueWriteOffsets(device)`, `partialMapping(device)`.

- [ ] **1. Ajouter un helper de scope lisible.** Il ne remplace pas les commandes GPU des cas :

```kotlin
internal suspend fun withValidationScope(device: GPUDevice, block: suspend () -> Unit) {
    device.pushErrorScope(GPUErrorFilter.Validation)
    var bodyFailure: Throwable? = null
    try {
        block()
    } catch (failure: Throwable) {
        bodyFailure = failure
        throw failure
    } finally {
        try {
            assertNull(device.popErrorScope().getOrThrow(), "Unexpected validation error")
        } catch (scopeFailure: Throwable) {
            val original = bodyFailure
            if (original == null) throw scopeFailure
            original.addSuppressed(scopeFailure)
        }
    }
}
```

Encadrer `mappedAtCreation` et les cas valides suivants avec ce helper. Un timeout conduit à la fermeture du device par le runner ; ne pas rendre le nettoyage GPU non annulable indéfiniment.

- [ ] **2. Écrire `bufferCopyOffsets`.** Ce cas complet vérifie des offsets non nuls et des sentinelles :

```kotlin
suspend fun bufferCopyOffsets(device: GPUDevice) = withValidationScope(device) {
    device.createBuffer(BufferDescriptor(
        16uL, GPUBufferUsage.CopySrc, mappedAtCreation = true,
    )).use { source ->
        device.createBuffer(BufferDescriptor(
            16uL, GPUBufferUsage.CopyDst or GPUBufferUsage.MapRead,
        )).use { destination ->
            source.getMappedRange().setUInts(0uL, uintArrayOf(10u, 20u, 30u, 40u))
            source.unmap()
            device.createCommandEncoder().use { encoder ->
                encoder.copyBufferToBuffer(source, 4uL, destination, 8uL, 8uL)
                encoder.finish().use { commands -> device.queue.submit(listOf(commands)) }
            }
            destination.mapAsync(GPUMapMode.Read).getOrThrow()
            try {
                assertContentEquals(
                    uintArrayOf(0u, 0u, 20u, 30u),
                    destination.getMappedRange().toUIntArray(),
                )
            } finally { destination.unmap() }
        }
    }
}
```

Utiliser `use` pour les objets `AutoCloseable` du contrat ; si l’opt-in standard est exigé par le compilateur de la baseline, l’ajouter explicitement plutôt que créer un gestionnaire de ressources générique.

- [ ] **3. Écrire `queueWriteOffsets`.** Créer un buffer `CopyDst | CopySrc` de 16 octets et un staging `CopyDst | MapRead` de 16 octets avec des `use` imbriqués. Le cœur est :

```kotlin
val data = ArrayBuffer.of(uintArrayOf(10u, 20u, 30u, 40u))
device.queue.writeBuffer(buffer, 4uL, data, 8uL, 8uL)
device.createCommandEncoder().use { encoder ->
    encoder.copyBufferToBuffer(buffer, 0uL, staging, 0uL, 16uL)
    encoder.finish().use { device.queue.submit(listOf(it)) }
}
staging.mapAsync(GPUMapMode.Read).getOrThrow()
try {
    assertContentEquals(uintArrayOf(0u, 30u, 40u, 0u), staging.getMappedRange().toUIntArray())
} finally { staging.unmap() }
```

Ajouter un second cas `queueWriteRemaining` dont l’écriture est `device.queue.writeBuffer(buffer, 0uL, data, 8uL)` (taille omise) et le résultat attendu `[30, 40, 0, 0]`. Garder les deux fonctions lisibles plutôt que construire une matrice de lambdas opaques.

- [ ] **4. Écrire `partialMapping`.** Initialiser un source `[11,22,33,44]`, copier ses 16 octets dans un staging `MapRead | CopyDst`, puis effectuer :

```kotlin
staging.mapAsync(GPUMapMode.Read, 8uL, 8uL).getOrThrow()
try {
    assertEquals(GPUBufferMapState.Mapped, staging.mapState)
    assertContentEquals(uintArrayOf(33u, 44u), staging.getMappedRange(8uL, 8uL).toUIntArray())
} finally { staging.unmap() }
assertEquals(GPUBufferMapState.Unmapped, staging.mapState)
staging.mapAsync(GPUMapMode.Read).getOrThrow()
try {
    assertContentEquals(uintArrayOf(11u, 22u, 33u, 44u), staging.getMappedRange().toUIntArray())
} finally { staging.unmap() }
```

Ne pas conserver ni relire la première vue après `unmap`. Encadrer les allocations dans des `use` et tout le cas dans `withValidationScope`.

- [ ] **5. Enregistrer les cas.** IDs stables : `transfers.copy-offsets`, `transfers.write-offsets`, `transfers.write-remaining`, `buffers.partial-map-remap`. Lier chaque cas à ses méthodes et propriétés publiques dans `contract`.
- [ ] **6. Exécuter sur JS puis Wasm.** Examiner les valeurs attendues et les scopes. Si un cas échoue sur le binding, conserver sa véritable assertion et rapporter la régression ; ne pas remplacer le test par une comparaison de valeurs produites par la même implémentation.
- [ ] **7. Commiter.** `rtk git commit -m "test: validate buffer mapping and byte offset transfers"`.

## Task 4 : vérifier compute, layouts, constantes et erreurs

**Files:** créer `ComputeCases.kt`, `ErrorCases.kt`, compléter `FoundationCases.kt`.

**Interfaces:** produit `computeAutoLayout`, `computeExplicitLayout`, `emptyErrorScope`, `invalidBufferUsage`, `invalidMapAlignment`, `mappingDestroyedBuffer` — toutes `suspend fun (device: GPUDevice): Unit`.

- [ ] **1. Définir un shader déterministe dans `ComputeCases.kt`.**

```kotlin
private const val SCALE_SHADER = """
override scale: u32 = 1u;
@group(0) @binding(0) var<storage, read_write> output: array<u32>;
@compute @workgroup_size(1)
fn main(@builtin(global_invocation_id) id: vec3<u32>) {
    output[id.x] = (id.x + 1u) * scale;
}
"""
```

- [ ] **2. Écrire le parcours compute automatique.** Dans un scope de validation, allouer `storage` (`Storage | CopySrc`, 16 octets), `staging` (`MapRead | CopyDst`, 16 octets), puis un shader avec `ShaderModuleDescriptor(code = SCALE_SHADER)`. Utiliser des `use` imbriqués et ce cœur :

```kotlin
device.createComputePipeline(ComputePipelineDescriptor(
    compute = ProgrammableStage(shader, entryPoint = "main", constants = mapOf("scale" to 7.0)),
)).use { pipeline ->
    pipeline.getBindGroupLayout(0u).use { layout ->
        device.createBindGroup(BindGroupDescriptor(
            layout = layout,
            entries = listOf(BindGroupEntry(0u, BufferBinding(storage))),
        )).use { group ->
            device.createCommandEncoder().use { encoder ->
                val pass = encoder.beginComputePass()
                pass.setPipeline(pipeline)
                pass.setBindGroup(0u, group)
                pass.dispatchWorkgroups(4u)
                pass.end()
                encoder.copyBufferToBuffer(storage, 0uL, staging, 0uL, 16uL)
                encoder.finish().use { device.queue.submit(listOf(it)) }
            }
            staging.mapAsync(GPUMapMode.Read).getOrThrow()
            try {
                assertContentEquals(uintArrayOf(7u, 14u, 21u, 28u), staging.getMappedRange().toUIntArray())
            } finally { staging.unmap() }
        }
    }
}
```

- [ ] **3. Écrire le parcours avec layout explicite.** Créer le même storage, staging et shader. Créer les deux layouts avec `use` :

```kotlin
val bindGroupLayout = device.createBindGroupLayout(BindGroupLayoutDescriptor(
    entries = listOf(BindGroupLayoutEntry(
        binding = 0u,
        visibility = GPUShaderStage.Compute,
        buffer = BufferBindingLayout(type = GPUBufferBindingType.Storage, minBindingSize = 16uL),
    )),
))
val pipelineLayout = device.createPipelineLayout(PipelineLayoutDescriptor(listOf(bindGroupLayout)))
val pipeline = device.createComputePipeline(ComputePipelineDescriptor(
    compute = ProgrammableStage(shader, constants = mapOf("scale" to 3.0)),
    layout = pipelineLayout,
))
```

Le stage omet volontairement `entryPoint` : le shader n’a qu’un entry point compute. Créer un bind group avec `bindGroupLayout`, encoder `setPipeline`, `setBindGroup`, `dispatchWorkgroups(4u)`, `end`, puis copier les 16 octets dans le staging et soumettre. Mapper, attendre `[3,6,9,12]`, unmap dans `finally`, fermer les ressources dans l’ordre inverse. `GPUComputePassEncoder` n’est pas `AutoCloseable` dans le contrat examiné : appeler `end()` explicitement, sans `use`. Les commandes restent visibles dans la fonction ; extraire au maximum un petit helper privé de readback si cela améliore réellement la lecture.

- [ ] **4. Ajouter les cas d’erreur comme attentes métier.**

```kotlin
suspend fun emptyErrorScope(device: GPUDevice) {
    device.pushErrorScope(GPUErrorFilter.Validation)
    val result = device.popErrorScope()
    assertTrue(result.isSuccess)
    assertNull(result.getOrThrow())
}

suspend fun invalidBufferUsage(device: GPUDevice) {
    device.pushErrorScope(GPUErrorFilter.Validation)
    try {
        device.createBuffer(BufferDescriptor(4uL, GPUBufferUsage.None)).close()
    } finally {
        val error = device.popErrorScope().getOrThrow()
        assertIs<GPUValidationError>(error)
    }
}

suspend fun invalidMapAlignment(device: GPUDevice) {
    device.createBuffer(BufferDescriptor(16uL, GPUBufferUsage.MapRead or GPUBufferUsage.CopyDst)).use { buffer ->
        device.pushErrorScope(GPUErrorFilter.Validation)
        try {
            val result = buffer.mapAsync(GPUMapMode.Read, 4uL, 4uL)
            assertTrue(result.isFailure, "Mapping offset must be aligned to 8 bytes")
            assertEquals(GPUBufferMapState.Unmapped, buffer.mapState)
        } finally {
            assertIs<GPUValidationError>(device.popErrorScope().getOrThrow())
        }
    }
}

suspend fun mappingDestroyedBuffer(device: GPUDevice) {
    val buffer = device.createBuffer(BufferDescriptor(16uL, GPUBufferUsage.MapRead or GPUBufferUsage.CopyDst))
    buffer.close()
    device.pushErrorScope(GPUErrorFilter.Validation)
    try {
        assertTrue(buffer.mapAsync(GPUMapMode.Read).isFailure)
    } finally {
        assertIs<GPUValidationError>(device.popErrorScope().getOrThrow())
    }
}
```

Ces cas n’emploient pas `withValidationScope`, car ils attendent une erreur capturée. Une exception qui s’échappe de `mapAsync` au lieu d’un `Result.failure` fait échouer le cas. Ne pas comparer le texte exact des erreurs.

- [ ] **5. Enregistrer les six cas.** IDs : `compute.auto-layout-constants`, `compute.explicit-layout-entrypoint`, `errors.empty-scope`, `errors.invalid-buffer-usage`, `errors.map-alignment`, `buffers.map-destroyed`. Le catalogue contient désormais 11 cas.
- [ ] **6. Lancer les 11 cas sous JS et Wasm.** Vérifier les résultats mémoire et les catégories d’erreur, sans modifier le binding dans ce dépôt. Consigner les défauts bloquants et demander leur résolution avant de déclarer ce lot vert.
- [ ] **7. Commiter.** `rtk git commit -m "test: validate compute layouts constants and WebGPU errors"`.

## Task 5 : établir l’inventaire complet du contrat et sa couverture réelle

**Files:** créer `inventory/baseline.json`, `inventory/symbols.tsv`, `inventory/contract.md`, `inventory/behaviors.json`.

**Interfaces:** `behaviors.json` est un tableau d’entrées `{id, family, contract, expectation, requiredFeatures, caseIds}` ; `caseIds` vide signifie « à tester », jamais « réussi ».

- [ ] **1. Fixer la référence.** `baseline.json` contient les clés `repository`, `commit`, `apiVersion`, `suiteVersion`, `sourceFiles` (chemins relatifs et SHA-256). Utiliser le commit intégral des sources locales recensées et la version commune du build. Les sept fichiers publics communs actuels sont `ArrayBuffer.kt`, `FlagEnumeration.kt`, `GPUTextureSwizzle.kt`, `bitflags.kt`, `enumerations.kt`, `interfaces.kt`, `typealiases.kt` dans `webgpu-api/src/commonMain/kotlin`.
- [ ] **2. Inventorier les déclarations depuis cette référence.** Lire les sept fichiers et les snapshots ABI comme aide à la revue. Produire `symbols.tsv` avec les colonnes `symbol`, `kind`, `source`, `line`, `family`. Qualifier les membres par leur type (`GPUBuffer.mapAsync`) et conserver les surcharges distinctes ; inclure les enum entries, bitflags, alias, interfaces des descripteurs et helpers de données publics. Ne pas utiliser le snapshot JVM seul : il ne représente pas toutes les signatures communes.
- [ ] **3. Couvrir toutes les familles dans `contract.md` et `behaviors.json`.** Pour chaque symbole, le rattacher à au moins une entrée de comportement ou expliciter son rôle de type support. Utiliser les familles : adapter/device/features/limits ; queue/commandes ; buffers/mapping ; transferts buffers/textures ; bind groups/layouts ; shaders/compilation ; compute ; textures/views/samplers ; rendu/passes/attachments ; pipelines/render state ; render bundles ; queries/timestamps ; erreurs/asynchronisme ; types de données/descripteurs/flags/swizzle. Relever les symboles restants et créer une famille nommée si le contrat de référence en comporte d’autres. Cette revue de contenu est une tâche d’inventaire, pas un test automatisé de l’architecture.
- [ ] **4. Décrire les comportements plutôt que l’existence d’une méthode.** Exemple exact pour le format :

```json
[
  {
    "id": "buffers.mapping.partial-range",
    "family": "buffers/mapping",
    "contract": ["GPUBuffer.mapAsync", "GPUBuffer.getMappedRange", "GPUBuffer.unmap"],
    "expectation": "Lire une plage alignée puis remapper le buffer entier sans conserver la vue invalidée.",
    "requiredFeatures": [],
    "caseIds": ["buffers.partial-map-remap"]
  },
  {
    "id": "render.depth.load-store",
    "family": "rendu/passes/attachments",
    "contract": ["GPURenderPassDepthStencilAttachment"],
    "expectation": "Les opérations de chargement et de stockage préservent ou remplacent la profondeur selon le descripteur.",
    "requiredFeatures": [],
    "caseIds": []
  }
]
```

Rédiger les autres entrées à partir de la documentation du contrat, en distinguant chemins valides, paramètres optionnels, bornes et erreurs. L’inventaire complet signifie que tout le contrat est référencé et que les comportements connus sont décrits ; ne pas prétendre avoir énuméré toutes les combinaisons possibles. Marquer explicitement les familles dont l’analyse comportementale doit être approfondie.
- [ ] **5. Relier les 11 cas effectivement exécutables.** Ajouter seulement les liens justifiés par leurs assertions. `compute.auto-layout-constants` ne couvre ni les pipelines render ni toutes les possibilités de constants. Les entrées sans cas restent visibles.
- [ ] **6. Relire la traçabilité.** Parcourir les sept fichiers source en regard de l’inventaire, puis les 11 fonctions en regard de `caseIds`. Consigner les familles et le nombre de déclarations recensées dans `docs/verification.md`, sans annoncer un pourcentage de conformité basé sur le nombre de symboles.
- [ ] **7. Commiter.** `rtk git commit -m "docs: inventory the public WebGPU contract and foundation coverage"`.

## Task 6 : automatiser l’exécution Chromium et publier la page Validation

**Files:** créer `tools/package.json`, lockfile, `run-browser.mjs`, `build-site.mjs`, `site/index.html`, `site/app.js`, `site/style.css`, `.github/workflows/suite.yml` ; modifier `.github/workflows/test.yml`, `.github/workflows/docs.yml` et `docs/mkdocs.yml`.

**Interfaces:** la commande `node tools/run-browser.mjs <js|wasm> <distribution-directory>` produit `build/reports/<target>.json` et sort avec code non nul si l’exécution est incomplète ou échouée. `node tools/build-site.mjs` produit `build/site/`.

- [ ] **1. Installer le lanceur.** `tools/package.json` contient `{"private":true,"type":"module","devDependencies":{"playwright":"1.55.1"}}`. Exécuter `rtk proxy npm install --prefix tools` puis `rtk proxy npm exec --prefix tools -- playwright install chromium`. Versionner le lockfile. Le navigateur installé et sa version réelle apparaissent dans les rapports.
- [ ] **2. Servir la distribution et collecter les résultats.** `run-browser.mjs` utilise `node:http`, `node:fs/promises`, `node:path` et `chromium` de `playwright`. Servir uniquement le répertoire de distribution demandé sur `127.0.0.1` avec port automatique, `index.html` pour `/`, MIME JavaScript pour `.js`/`.mjs`, `application/wasm` pour `.wasm`, JSON pour `.json`. Rejeter les chemins qui sortent de la racine après `resolve`. Le cœur de collecte est :

```javascript
const browser = await chromium.launch({
  headless: true,
  args: [
    '--enable-unsafe-webgpu', '--enable-unsafe-swiftshader',
    '--use-angle=swiftshader', '--disable-dev-shm-usage',
  ],
});
const page = await browser.newPage();
const pageErrors = [];
page.on('pageerror', error => pageErrors.push(String(error)));
await page.goto(url);
await page.waitForFunction(() => typeof globalThis.graphiksSuiteReport === 'string', {
}, { timeout: 420000 });
const report = JSON.parse(await page.evaluate(() => globalThis.graphiksSuiteReport));
const environment = {
  target,
  browser: browser.version(),
  userAgent: await page.evaluate(() => navigator.userAgent),
  platform: process.platform,
  requestedBackend: 'swiftshader',
};
```

Définir `target` et la racine depuis les arguments, `url` depuis le serveur. Dans un `try/catch/finally`, toujours écrire un rapport (y compris erreur fatale si timeout ou lancement impossible), puis fermer page, browser et serveur. Ajouter l’inventaire de référence et `git rev-parse HEAD` à l’enveloppe `{schemaVersion:1, baseline, suiteCommit, generatedAt, environment, report, pageErrors}`. `requestedBackend` décrit les flags, pas une détection matérielle ; conserver la description de l’adapter renvoyée par les cas sans déduire son identité à partir d’un texte libre.

Le contrôle de complétude est une garde de lancement : attendre exactement les 11 IDs du catalogue courant, aucun doublon, tous `passed` pour ce lot sans features optionnelles, aucun `fatalError` ni `pageErrors`. En cas de différence, `process.exitCode = 1`. Tenir les IDs attendus dans `inventory/foundation-case-ids.json`, relu en même temps que `FoundationCases.kt` :

```json
[
  "buffers.mapped-at-creation",
  "transfers.copy-offsets",
  "transfers.write-offsets",
  "transfers.write-remaining",
  "buffers.partial-map-remap",
  "compute.auto-layout-constants",
  "compute.explicit-layout-entrypoint",
  "errors.empty-scope",
  "errors.invalid-buffer-usage",
  "errors.map-alignment",
  "buffers.map-destroyed"
]
```

Ne pas écrire une suite de tests pour cette garde.

- [ ] **3. Lancer les distributions.** Vérifier leurs chemins dans la sortie Gradle ; les chemins attendus sont :

```sh
rtk proxy ./gradlew :suite-browser:jsBrowserDistribution :suite-browser:wasmJsBrowserDistribution
rtk proxy node tools/run-browser.mjs js suite-browser/build/dist/js/productionExecutable
rtk proxy node tools/run-browser.mjs wasm suite-browser/build/dist/wasmJs/productionExecutable
```

Sur Linux, installer les dépendances système via `playwright install --with-deps chromium`. Si SwiftShader ne fournit pas d’adapter, diagnostiquer l’environnement Chromium/Vulkan ; conserver la commande en échec jusqu’à une vraie exécution. Ne pas contourner avec un skip. Les résultats fonctionnels logiciels ne sont pas des benchmarks.

- [ ] **4. Construire une page statique lisible.** `build-site.mjs` copie `site/` vers `build/site/`, `inventory/` vers `build/site/inventory/`, les deux rapports vers `build/site/reports/`, et les distributions vers `build/site/run/js/` et `build/site/run/wasm/`. Utiliser `fs.cp` avec `recursive:true` ; échouer si l’inventaire ou un rapport est absent. Le script n’interprète pas une absence comme une réussite.

`site/index.html` contient un titre « Graphiks WebGPU Suite — Validation », un résumé des versions, une table de couverture, deux sections JS et Wasm, et des liens vers les deux lancements locaux. `app.js` charge les JSON avec des chemins relatifs (compatibles avec le préfixe du site existant suivi de `/suite/`), crée les éléments DOM et remplit les textes avec `textContent`, jamais avec le HTML des diagnostics. Pour chaque comportement, afficher ses attentes, ses cas ou « À tester », puis les résultats par cible. Un résultat manquant est « Non exécuté ». Afficher le commit et la date des rapports publiés ; étiqueter les liens de lancement « Exécuter dans votre navigateur — résultats locaux ».

CSS : typographie système, largeur de lecture limitée, tables scrollables sur mobile, statuts avec libellés textuels en plus des couleurs. Ajouter des liens vers le code source des cas et la documentation de référence. Aucun chiffre « conformité 100 % » ni page Démos/Benchmarks vide.

- [ ] **5. Vérifier par lancement et inspection.** Servir `build/site` sur localhost, ouvrir la page, lire une entrée couverte et une non couverte, consulter les résultats séparés et exécuter les deux liens locaux. Vérifier aussi le site assemblé sous le préfixe réel de la documentation suivi de `/suite/`, ainsi qu’à largeur mobile. Ce sont des vérifications manuelles de livraison, pas des tests de structure HTML.
- [ ] **6. Intégrer la CI aux workflows existants.** Créer `.github/workflows/suite.yml` comme workflow réutilisable (`workflow_call`). Il installe JDK 25, Gradle et Node 22 ; lance `npm ci --prefix tools`, `npm exec --prefix tools -- playwright install --with-deps chromium`, compile les bibliothèques JVM et les distributions, puis exécute JS et Wasm dans deux étapes `if: !cancelled()` afin de conserver les deux diagnostics. Uploader `build/reports/` sous le nom `suite-reports` avec `if: always()`. Une étape échouée doit garder le job rouge. Après succès, assembler `build/site` et l’uploader comme artifact ordinaire `suite-site` via `actions/upload-artifact@v4`.

Ajouter un job appelant ce workflow dans `test.yml`. Dans `docs.yml`, ajouter également ce job et faire dépendre `build` de son succès. Après `mkdocs build --strict -f docs/mkdocs.yml`, télécharger `suite-site` via `actions/download-artifact@v4` dans `_site/suite/`, avant le contrôle de taille et l’upload Pages déjà présents. Ajouter une entrée Suite dans la navigation existante ; privilégier une page MkDocs d’introduction avec lien relatif vers `suite/` si un lien direct déclenche un avertissement en mode strict. Conserver un unique artifact Pages contenant documentation et suite, ainsi que le job de déploiement existant. Aucun deuxième déploiement Pages susceptible d’écraser le premier.

Réutiliser les versions d’actions déjà présentes ; ajouter `actions/setup-node@v4` pour Node 22. Conserver les conditions de publication sur `master` et l’absence de déploiement depuis les PR. Le workflow réutilisable ne déploie rien lui-même.

- [ ] **7. Commiter.** `rtk git commit -m "feat: publish browser validation reports and contract inventory"`.

## Task 7 : intégrer les deux artifacts à la publication existante

**Files:** modifier `buildSrc/src/main/kotlin/publish.gradle.kts`, `.github/workflows/publish.yml` ; compléter la documentation existante.

**Interfaces:** artifacts `org.graphiks:suite-core:<version>` et `org.graphiks:suite-acid-tests:<version>` ; un consommateur fournit le device à `AcidCase.run`.

- [ ] **1. Étendre la convention de publication.** Les deux bibliothèques appliquent déjà `publish`. Ajouter seulement ces branches au `when (project.name)` de `buildSrc/src/main/kotlin/publish.gradle.kts` :

```kotlin
"suite-core" -> "Execution contracts for Graphiks WebGPU validation cases"
"suite-acid-tests" -> "Portable acid tests for the Graphiks WebGPU public API"
```

Les coordonnées, métadonnées POM, sources, licence et signature sont fournies par la convention existante. Ne pas recopier sa configuration dans les modules. La convention applique également Dokka : vérifier que les modules se compilent avec ce plugin sans imposer leur ajout automatique à la documentation API du socle.

- [ ] **2. Étendre la publication distante existante.** Dans `.github/workflows/publish.yml`, ajouter un job appelant `suite.yml` et rendre les jobs `snapshot` et `release` dépendants de son succès. Ajouter `:suite-core:publishToMavenCentral` et `:suite-acid-tests:publishToMavenCentral` aux commandes qui publient déjà les quatre modules du socle, en conservant leurs arguments `releaseVersion`, credentials et déclencheurs. Utiliser les tâches normales de compilation des modules pour les cibles annoncées, en particulier macOS ARM64 et Linux x64, sur les hôtes compatibles du dépôt.

Conserver une publication coordonnée du socle et de la suite sous la version commune du dépôt. La publication utilise l’infrastructure existante, sans scénario supplémentaire destiné à la tester et sans repository Maven isolé de vérification.

- [ ] **3. Documenter l’usage concret.** Dans `README.md`, présenter l’objectif et la commande de lancement ; dans `docs/running.md`, documenter JDK, version de l’API, matrice publiée, commandes JS/Wasm et consommation depuis un binding ; dans `docs/adding-a-case.md`, montrer un cas lisible qui prend un device, ferme ses ressources et met à jour l’inventaire. Mentionner les statuts sans confondre couverture et exécution. Les résultats natifs restent dans les dépôts consommateurs.
- [ ] **4. Finaliser les preuves.** `docs/verification.md` indique commits, commandes de compilation et d’exécution effectivement passées, nombre de cas par cible, version Chromium, environnement logiciel et limitations observées. Ne pas transformer des étapes prévues en résultats affirmés.
- [ ] **5. Vérifier et commiter.**

```sh
rtk git diff --check
rtk git status --short
```

Examiner les fichiers ajoutés, exclure outputs et credentials, puis `rtk git commit -m "feat: publish portable WebGPU foundation validation artifacts"`.

## Revue du plan et transmission

### Couverture de la spec pour ce lot

| Exigence | Traitement |
| --- | --- |
| Valeur technique et usages lisibles | Cas explicites, tâches 1 à 4 |
| Sous-modules du dépôt et API commune | Préparation, conventions existantes, tâches 1 et 7 |
| Contrat minimal et artifacts consommables | Tâches 1 et 7 |
| Inventaire complet et couverture progressive | Tâche 5 |
| Fondations buffers/transferts/compute/erreurs | Tâches 3 et 4 : 11 cas initiaux, approfondissement progressif |
| Résultats navigateur JS/Wasm honnêtes | Tâches 2 et 6 |
| Page publique Validation | Tâche 6 |
| Démo de particules et `suite-demos` | Plan suivant de la même phase, non livré par ce lot |
| Benchmarks et `suite-benchmarks` | Plan suivant de la même phase, non livré par ce lot |
| Résultats natifs dans les dépôts des bindings | Artifacts disponibles via la publication existante ; exécutions dans les bindings |
| Absence de tests d’architecture | Contraintes et vérifications par compilation/lancement |

### Checklist de rédaction

- [x] Le plan intègre les sous-modules au dépôt existant et réutilise ses conventions, versions et workflows.
- [x] Les signatures GPU utilisées ont été confrontées à la baseline consultée.
- [x] Les risques de Review Focus ont un cas GPU ou une vérification de lancement attribuée.
- [x] Les besoins de la spec hors de ce lot sont explicitement conservés pour les plans suivants.
- [x] Aucune compilation ni exécution GPU n’est présentée comme réalisée pendant la rédaction.
- [ ] Revue et approbation de ce premier plan par l’utilisateur avant transmission pour exécution.

### Message à l’agent d’exécution

Lire la spec puis ce plan dans leur intégralité. Exécuter les tâches 1 à 7 dans un worktree du dépôt actuel `webgpu`, avec des commits ciblés. Réutiliser les conventions `kmp` et `publish`, le catalogue et les workflows existants. Privilégier le code Kotlin lisible et les assertions sur les usages WebGPU. Compiler et lancer les modules réels ; ne pas ajouter de tests d’architecture, y compris de projet dédié à la publication ou à la consommation Maven. Signaler les défauts réels de `webgpu-browser` sans les masquer ; leur correction relève du chantier navigateur. Remettre les résultats GPU JS/Wasm et de compilation des modules, ainsi que les limites observées. La démo et les benchmarks seront traités dans les plans suivants.
