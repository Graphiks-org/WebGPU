# Reaction-Diffusion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ajouter une démo Gray–Scott interactive et pédagogique, vérifiée sur JS et Wasm JS, sans régression des particules.

**Architecture:** Une scène Kotlin portable possède deux textures ping-pong et encode injection, simulation et rendu. Le runner navigateur possède canvas, contrôles et animation ; la galerie statique présente les deux démos. Le collecteur agrège leurs vérifications GPU dans les rapports existants.

**Tech Stack:** Kotlin Multiplatform, API/descripteurs Graphiks WebGPU, WGSL, kotlinx.coroutines/serialization, Gradle (JDK 25), Node.js 22 et Playwright 1.63.0 déjà utilisé dans `tools/package.json`.

**Spec:** `docs/superpowers/specs/2026-10-09-reaction-diffusion-design.md`

## Suivi d'exécution (2026-10-10)

Les cases détaillées ci-dessous conservent le plan original comme procédure de reproduction.
Le statut effectif des livrables est suivi ici ; les preuves sont dans `docs/verification.md`
et `docs/verification.fr.md`.

- [x] Task 1 : données portables, 3 tests CPU.
- [x] Task 2 : scène GPU, WGSL et trois scénarios de vérification JS/Wasm.
- [x] Task 3 : collecte stricte des deux démos, cinq ids conservés.
- [x] Task 4 : interface EN/FR, pointeurs et cycle de vie.
- [x] Task 5 : pédagogie, galerie et inspection visuelle.
- [x] Task 6 : campagne complète, documentation et résultats attribués au build testé.
- [ ] Revue indépendante de la branche et traitement de ses constats.

## Global Constraints

- « Grille fixe de 256 × 256 cellules, deux concentrations A et B par cellule. »
- « Utiliser deux textures `rgba32float` » ; R = A, G = B, bleu = 0, alpha = 1.
- « Aucune fonctionnalité GPU optionnelle n'est requise. » Lecture par `textureLoad`, layouts explicites `UnfilterableFloat`, aucun sampler.
- dt = 1 ; DA = 1 ; DB = 0,5 ; poids du laplacien : -1, 0,2, 0,05 ; frontières périodiques ; concentrations bornées à [0, 1].
- « La vitesse choisit 1 à 16 étapes par frame, avec 8 par défaut. » Pause = zéro étape, avance = une étape ; ce n'est pas une vitesse physique.
- Feed et kill finis dans [0, 0,1] ; pinceau de rayon six cellules, A = 0,5 et B = 1 ; une seule position consommée par frame.
- « Un appel d'encodage correspond à une soumission ; documenter cette contrainte. » La scène ne soumet pas et ne ferme pas le device.
- « La fermeture est idempotente ; les créations partielles sont libérées en cas d'échec. »
- « Aucun nouveau framework de scènes » ; préserver le contrat de `ParticleScene` et les rapports acid tests/benchmarks.
- EN/FR, JS/Wasm JS ; pas de `dynamic` ni de nouvelle dépendance DOM ; pas de simulation CPU de remplacement.
- « La démo ne constitue pas un benchmark ni une preuve de conformité de toute l'API. »
- Ne pas implémenter ville instanciée, filtres, import d'image ou éditeur de shaders.

## Review Focus

1. Canvas masqué ou de taille nulle : ne pas acquérir de texture de surface ni produire de coordonnées invalides ; test navigateur tâche 4.
2. Pointeur capturé hors canvas, annulation et deuxième pointeur : coordonnées bornées, capture nettoyée, aucun mélange de traits ; tests tâche 4.
3. Device perdu ou page fermée pendant l'initialisation : arrêt, contrôles désactivés, ressources tardives libérées ; tests tâche 4.
4. Lecture GPU après un nombre impair/pair d'étapes ou un reset : lire le véritable état courant, pas une texture fixe ; tests tâche 2.
5. Une route de vérification absente, invalide ou d'une autre révision : rapport d'échec conservé, sortie non nulle ; tests tâche 3.

---

## Fichiers et responsabilités

Les chemins ci-dessous sont relatifs au dépôt. Les dossiers Kotlin créés portent le package correspondant.

| Fichier | Responsabilité |
| --- | --- |
| `suite-demos/src/commonMain/kotlin/org/graphiks/webgpu/suite/demos/reactiondiffusion/ReactionDiffusionData.kt` | constantes, paramètres, presets, état initial, validation |
| `suite-demos/src/commonMain/kotlin/org/graphiks/webgpu/suite/demos/reactiondiffusion/ReactionDiffusionShaders.kt` | trois sources WGSL commentées effectivement compilées |
| `suite-demos/src/commonMain/kotlin/org/graphiks/webgpu/suite/demos/reactiondiffusion/ReactionDiffusionScene.kt` | création, ping-pong, encodage, copies, fermeture |
| `suite-demos/src/commonTest/kotlin/org/graphiks/webgpu/suite/demos/reactiondiffusion/ReactionDiffusionDataTest.kt` | tests CPU des données |
| `suite-browser/src/commonMain/kotlin/org/graphiks/webgpu/suite/browser/demos/ReactionDiffusionReference.kt` | oracle CPU indépendant pour les vérifications GPU, jamais utilisé dans l'animation |
| `suite-browser/src/commonMain/kotlin/org/graphiks/webgpu/suite/browser/demos/ReactionDiffusionGpuChecks.kt` | trois scénarios GPU et readback |
| `suite-browser/src/commonMain/kotlin/org/graphiks/webgpu/suite/browser/demos/ReactionDiffusionControls.kt` | état des contrôles et conversion géométrique testables sans DOM |
| `suite-browser/src/commonMain/kotlin/org/graphiks/webgpu/suite/browser/demos/ReactionDiffusionPointerDom.kt` | interfaces DOM typées pour pointeurs et rectangle |
| `suite-browser/src/commonMain/kotlin/org/graphiks/webgpu/suite/browser/demos/ReactionDiffusionPage.kt` | cycle de vie, surface, animation et listeners |
| `suite-browser/src/commonMain/kotlin/org/graphiks/webgpu/suite/browser/demos/ReactionDiffusionView.kt` | construction des contrôles, textes, panneau pédagogique |
| `suite-browser/src/commonTest/kotlin/org/graphiks/webgpu/suite/browser/demos/ReactionDiffusionControlsTest.kt` | transitions et conversion des coordonnées |
| `suite-browser/src/commonTest/kotlin/org/graphiks/webgpu/suite/browser/demos/ReactionDiffusionReferenceTest.kt` | oracle CPU : centre uniforme, équilibre et frontière périodique |
| `suite-browser/src/commonMain/resources/demos/reaction-diffusion.en.json` et `.fr.json` | libellés et explications |
| `tools/demo-reports.mjs` et `tools/demo-reports.test.mjs` | catalogue, validation et agrégation pure des rapports |
| `tools/reaction-diffusion.browser.test.mjs` | tests navigateur réels et serveur temporaire de distribution |
| `tools/demos-gallery.browser.test.mjs` | galerie avec ressources simulées, sans rapports inventés publiés |

Modifier aussi `suite-demos/build.gradle.kts` et `suite-browser/build.gradle.kts` (dépendance de test), `Main.kt` (routes), `suite-browser/src/commonMain/resources/index.html` (CSS ciblé), `tools/run-browser.mjs` (collecte), `site/demos/app.js` (galerie), `README.md`, `docs/running.md`, `docs/verification.md` et `CHANGELOG.md`.

## Préparation d'exécution

- [ ] Lire spec, plan et consignes locales ; utiliser `using-git-worktrees` à l'exécution pour vérifier l'isolation de ce checkout avant de choisir un autre worktree. Aucun worktree supplémentaire n'est requis par ce plan.
- [ ] Vérifier `git status --short`, `java -version`, `node --version` et `./gradlew --version` séparément. Préserver tout changement tiers. JDK 25 et Node 22 sont les versions de référence.
- [ ] Installer seulement si nécessaire les dépendances déjà déclarées : `npm install --prefix tools`, puis `npm exec --prefix tools -- playwright install chromium`.
- [ ] Avant chaque code de production, charger TDD et constater le test rouge. Lors d'un échec inattendu, appliquer systematic-debugging ; ne pas diminuer les assertions pour obtenir du vert.

## Task 1: Données portables et contrats numériques

**Files:** créer `ReactionDiffusionData.kt` et `ReactionDiffusionDataTest.kt` aux chemins du tableau ; modifier `suite-demos/build.gradle.kts`.

**Interfaces — produces:**

```kotlin
const val ReactionGridSize = 256
const val ReactionStateFloats = ReactionGridSize * ReactionGridSize * 4
data class ReactionParameters(val feed: Float, val kill: Float)
enum class ReactionPreset(val parameters: ReactionParameters) {
    Coral(ReactionParameters(0.0545f, 0.062f)),
    Labyrinth(ReactionParameters(0.029f, 0.057f)),
    Spots(ReactionParameters(0.0367f, 0.0649f)),
}
enum class ReactionPalette { Ocean, Ember, Grayscale }
enum class ReactionDisplay { Color, A, B }
data class ReactionBrush(val x: Int, val y: Int)
fun initialReactionState(): FloatArray
fun validateReactionState(state: FloatArray): Unit
fun validateReactionParameters(parameters: ReactionParameters): Unit
fun validateReactionSteps(steps: Int): Unit
fun validateReactionBrush(brush: ReactionBrush): Unit
```

- [ ] **1. Écrire les tests rouges et ajouter la dépendance de test.** Ajouter `sourceSets.commonTest.dependencies { implementation(kotlin("test")) }`. Tests minimaux :

```kotlin
@Test fun initialStateIsReproducible() {
    val first = initialReactionState()
    assertEquals(ReactionStateFloats, first.size)
    assertContentEquals(first, initialReactionState())
    assertEquals(1f, first[0]); assertEquals(0f, first[1])
    val center = (64 * 256 + 64) * 4
    assertEquals(0.5f, first[center]); assertEquals(0.25f, first[center + 1])
    assertEquals(9 * 8 * 8, first.indices.count { it % 4 == 1 && first[it] == 0.25f })
    first[0] = 0f
    assertEquals(1f, initialReactionState()[0])
}
@Test fun rejectsInvalidParametersAndSteps() {
    for (bad in listOf(Float.NaN, Float.POSITIVE_INFINITY, -0.01f, 0.101f)) {
        assertFailsWith<IllegalArgumentException> { validateReactionParameters(ReactionParameters(bad, 0.06f)) }
        assertFailsWith<IllegalArgumentException> { validateReactionParameters(ReactionParameters(0.03f, bad)) }
    }
    for (n in listOf(-1, 17)) assertFailsWith<IllegalArgumentException> { validateReactionSteps(n) }
    for (n in listOf(0, 1, 8, 16)) validateReactionSteps(n)
}
@Test fun rejectsInvalidStateAndBrush() {
    assertFailsWith<IllegalArgumentException> { validateReactionState(FloatArray(4)) }
    val state = initialReactionState()
    for ((offset, value) in listOf(0 to Float.NaN, 1 to -1f, 2 to 1f, 3 to 0f)) {
        val copy = state.copyOf(); copy[offset] = value
        assertFailsWith<IllegalArgumentException> { validateReactionState(copy) }
    }
    assertFailsWith<IllegalArgumentException> { validateReactionBrush(ReactionBrush(-1, 0)) }
    assertFailsWith<IllegalArgumentException> { validateReactionBrush(ReactionBrush(0, 256)) }
}
```

- [ ] **2. Constater le rouge.** Run: `./gradlew :suite-demos:jvmTest --tests '*ReactionDiffusionDataTest'`. Attendu : références nouvelles non résolues, pas erreur d'environnement.
- [ ] **3. Implémenter l'état et la validation.** Utiliser les interfaces ci-dessus et ce corps d'initialisation ; chaque fonction de validation utilise `require`, avec message incluant l'entrée invalide.

```kotlin
fun initialReactionState(): FloatArray {
    val state = FloatArray(ReactionStateFloats)
    for (i in state.indices step 4) { state[i] = 1f; state[i + 3] = 1f }
    for (cy in listOf(64, 128, 192)) for (cx in listOf(64, 128, 192)) {
        for (y in cy - 4 until cy + 4) for (x in cx - 4 until cx + 4) {
            val i = (y * ReactionGridSize + x) * 4
            state[i] = 0.5f; state[i + 1] = 0.25f
        }
    }
    return state
}
fun validateReactionParameters(parameters: ReactionParameters) {
    require(parameters.feed.isFinite() && parameters.feed in 0f..0.1f) { "Invalid feed: ${parameters.feed}" }
    require(parameters.kill.isFinite() && parameters.kill in 0f..0.1f) { "Invalid kill: ${parameters.kill}" }
}
fun validateReactionSteps(steps: Int) { require(steps in 0..16) { "Invalid steps: $steps" } }
fun validateReactionBrush(brush: ReactionBrush) {
    require(brush.x in 0 until 256 && brush.y in 0 until 256) { "Invalid brush: $brush" }
}
fun validateReactionState(state: FloatArray) {
    require(state.size == ReactionStateFloats) { "Invalid state size: ${state.size}" }
    for (i in state.indices step 4) {
        require(state[i].isFinite() && state[i] in 0f..1f) { "Invalid A at $i" }
        require(state[i + 1].isFinite() && state[i + 1] in 0f..1f) { "Invalid B at $i" }
        require(state[i + 2] == 0f && state[i + 3] == 1f) { "Invalid padding at $i" }
    }
}
```

- [ ] **4. Vérifier le vert**, mêmes commandes ; ajouter assertions des trois couples feed/kill exacts et des bornes 0 et 0,1.
- [ ] **5. Commit** des deux fichiers et du Gradle : `feat: add deterministic reaction-diffusion data`.

## Task 2: Scène GPU, rendu et route de vérification

**Files:** créer `ReactionDiffusionScene.kt`, `ReactionDiffusionShaders.kt`, `ReactionDiffusionReference.kt`, `ReactionDiffusionReferenceTest.kt`, `ReactionDiffusionGpuChecks.kt` et `tools/reaction-diffusion.browser.test.mjs` ; modifier `Main.kt` sans modifier `ParticleScene`. Ajouter dès cette tâche la dépendance `kotlin("test")` de commonTest à `suite-browser/build.gradle.kts` pour tester l'oracle.

**Interfaces — consumes:** données et types tâche 1, `DemoReport`/`CaseResult` existants.

**Interfaces — produces:**

```kotlin
class ReactionDiffusionScene : AutoCloseable {
    companion object {
        fun create(device: GPUDevice, format: GPUTextureFormat,
                   initial: FloatArray = initialReactionState()): ReactionDiffusionScene
    }
    fun reset(initial: FloatArray = initialReactionState())
    fun encodeFrame(encoder: GPUCommandEncoder, target: GPUTextureView,
                    width: Int, height: Int, steps: Int, parameters: ReactionParameters,
                    palette: ReactionPalette = ReactionPalette.Ocean,
                    display: ReactionDisplay = ReactionDisplay.Color,
                    brush: ReactionBrush? = null)
    fun encodeStateCopy(encoder: GPUCommandEncoder, destination: GPUBuffer)
    override fun close()
}
val ReactionSimulationShader: String
val ReactionBrushShader: String
val ReactionRenderShader: String
internal fun referenceReactionStep(state: FloatArray, parameters: ReactionParameters): FloatArray
internal fun referenceReactionBrush(state: FloatArray, brush: ReactionBrush): FloatArray
internal suspend fun reactionDiffusionGpuResults(device: GPUDevice): List<CaseResult>
```

Le constructeur réel de la scène est privé avec ses ressources ; le bloc ci-dessus décrit son API, pas un corps compilable vide. Les copies utilisent 4096 octets par ligne et un buffer MapRead|CopyDst de 1 048 576 octets. `encodeStateCopy` exige un buffer assez grand et un état non fermé.

- [ ] **1. Écrire le test navigateur de route rouge.** Le fichier utilise `node:test`, `node:assert/strict`, `node:http`, `node:fs/promises` et `chromium` de Playwright. Créer un serveur localhost port automatique servant seulement `GRAPHIKS_DISTRIBUTION`, défaut `suite-browser/build/dist/js/productionExecutable`, avec types MIME html/js/wasm/json/map. Vérifier que `resolve(root, '.' + pathname)` reste dans `root` avec `relative`, refuser les traversées ; fermer serveur/page/browser avec `after`. Le test est :

```javascript
test('reaction-diffusion publishes three successful GPU checks', async () => {
  const page = await browser.newPage();
  try {
    await page.goto(`${origin}/?demo=reaction-diffusion&verify=1`);
    await page.waitForFunction(() => typeof globalThis.graphiksDemoReport === 'string', { }, { timeout: 120000 });
    const report = await page.evaluate(() => JSON.parse(globalThis.graphiksDemoReport));
    assert.equal(report.fatalError, null);
    assert.deepEqual(report.cases.map(c => c.id), [
      'reaction-diffusion.compute-render-readback',
      'reaction-diffusion.pause-step-reset',
      'reaction-diffusion.brush-boundaries',
    ]);
    assert.ok(report.cases.every(c => c.status === 'passed'), JSON.stringify(report));
  } finally { await page.close(); }
});
```

Lancer Chromium avec `--enable-unsafe-webgpu`, `--enable-unsafe-swiftshader`, `--use-angle=swiftshader`. `origin` et `browser` sont créés dans le hook `before`, pas exportés par la production. Les attentes à 120 s et un timeout de test à 150 s évitent les attentes illimitées.

- [ ] **2. Constater le rouge.** Construire avec `./gradlew :suite-browser:jsBrowserDistribution`, puis `node --test tools/reaction-diffusion.browser.test.mjs`. La route inconnue doit faire échouer l'attente du rapport.
- [ ] **3. Écrire l'oracle et les assertions GPU avant la scène.** L'oracle utilise une copie de sortie, indices périodiques et coefficients de la spec, pas une traduction appelée par le shader. Noyau CPU :

```kotlin
internal fun referenceReactionStep(state: FloatArray, parameters: ReactionParameters): FloatArray {
    validateReactionState(state); validateReactionParameters(parameters)
    fun sample(x: Int, y: Int, c: Int): Float = state[(((y + 256) % 256) * 256 + (x + 256) % 256) * 4 + c]
    fun lap(x: Int, y: Int, c: Int): Float = -sample(x, y, c) +
        0.2f * (sample(x - 1, y, c) + sample(x + 1, y, c) + sample(x, y - 1, c) + sample(x, y + 1, c)) +
        0.05f * (sample(x - 1, y - 1, c) + sample(x + 1, y - 1, c) + sample(x - 1, y + 1, c) + sample(x + 1, y + 1, c))
    val result = state.copyOf()
    for (y in 0 until 256) for (x in 0 until 256) {
        val i = (y * 256 + x) * 4
        val a = state[i]; val b = state[i + 1]; val r = a * b * b
        result[i] = (a + lap(x, y, 0) - r + parameters.feed * (1f - a)).coerceIn(0f, 1f)
        result[i + 1] = (b + 0.5f * lap(x, y, 1) + r - (parameters.kill + parameters.feed) * b).coerceIn(0f, 1f)
    }
    return result
}
```

La référence pinceau copie l'entrée ; pour chaque cellule, `dx = min(abs(x - brush.x), 256 - abs(x - brush.x))`, idem dy ; modifier A/B seulement si `dx*dx + dy*dy <= 36`.

Écrire aussi les tests Kotlin de l'oracle avant son corps :

```kotlin
@Test fun uniformSeedCenterMatchesHandCalculation() {
    val result = referenceReactionStep(initialReactionState(), ReactionPreset.Coral.parameters)
    val i = (64 * 256 + 64) * 4
    assertEquals(0.496f, result[i], 0.00001f)
    assertEquals(0.252125f, result[i + 1], 0.00001f)
}
@Test fun periodicBrushReachesOppositeEdge() {
    val result = referenceReactionBrush(initialReactionState(), ReactionBrush(0, 0))
    assertEquals(1f, result[(255 * 256 + 255) * 4 + 1])
    assertEquals(0f, result[(10 * 256 + 10) * 4 + 1])
}
```

Ajouter l'équilibre uniforme A=1/B=0, inchangé par une étape, et une cellule B non nulle à x=0 dont la diffusion atteint x=255. Run rouge puis vert : `./gradlew :suite-browser:jsBrowserTest --tests '*ReactionDiffusionReferenceTest'`.

Dans les scénarios GPU, utiliser une cible RGBA8Unorm 256² RenderAttachment|CopySrc, un readback d'état et un readback de pixels. Encodage test :

```kotlin
device.createCommandEncoder().use { encoder ->
    scene.encodeFrame(encoder, target, 256, 256, 1, ReactionPreset.Coral.parameters,
        display = ReactionDisplay.B)
    scene.encodeStateCopy(encoder, staging)
    encoder.finish().use { device.queue.submit(listOf(it)) }
}
device.queue.onSubmittedWorkDone().getOrThrow()
staging.mapAsync(GPUMapMode.Read).getOrThrow()
try {
    val actual = staging.getMappedRange().toFloatArray()
    val expected = referenceReactionStep(initialReactionState(), ReactionPreset.Coral.parameters)
    for (i in actual.indices) {
        assertTrue(actual[i].isFinite(), "Non-finite concentration at $i")
        assertTrue(kotlin.math.abs(actual[i] - expected[i]) <= 0.0001f, "Mismatch at $i")
    }
} finally { staging.unmap() }
```

Répéter cette lecture pour zéro, un, deux et seize pas et après reset, en soumettant chaque frame séparément. Tester pinceau `(128,128)` et `(0,0)` en pause ; comparer toute la texture à la référence. Vérifier pixel extérieur noir, pixel central égal à B × 255 à ±2, RGB égaux et alpha 255. Entourer chaque scénario d'un error scope Validation ; réémettre `CancellationException`, convertir les autres échecs en `CaseResult(..., "failed", diagnostic)`.

- [ ] **4. Implémenter les shaders compute.** Workgroups 8×8 et dispatch 32×32 ; guards explicites. Layout compute : binding 0 sampled UnfilterableFloat, 1 storage WriteOnly RGBA32Float, 2 uniform de 16 octets. Paramètres simulation = `[feed, kill, 0, 0]`, pinceau = `[x, y, 6, 0]`, buffers séparés. Noyau WGSL :

```wgsl
@group(0) @binding(0) var inputState: texture_2d<f32>;
@group(0) @binding(1) var outputState: texture_storage_2d<rgba32float, write>;
@group(0) @binding(2) var<uniform> parameters: vec4<f32>;
fn readState(p: vec2<i32>) -> vec2<f32> {
    return textureLoad(inputState, (p + vec2<i32>(256)) % vec2<i32>(256), 0).rg;
}
@compute @workgroup_size(8, 8)
fn simulate(@builtin(global_invocation_id) id: vec3<u32>) {
    if (id.x >= 256u || id.y >= 256u) { return; }
    let p = vec2<i32>(id.xy);
    let s = readState(p);
    let orthogonal = readState(p + vec2<i32>(-1, 0)) + readState(p + vec2<i32>(1, 0))
        + readState(p + vec2<i32>(0, -1)) + readState(p + vec2<i32>(0, 1));
    let diagonal = readState(p + vec2<i32>(-1, -1)) + readState(p + vec2<i32>(1, -1))
        + readState(p + vec2<i32>(-1, 1)) + readState(p + vec2<i32>(1, 1));
    let lap = -s + 0.2 * orthogonal + 0.05 * diagonal;
    let r = s.x * s.y * s.y;
    let next = s + vec2<f32>(lap.x - r + parameters.x * (1.0 - s.x),
        0.5 * lap.y + r - (parameters.y + parameters.x) * s.y);
    textureStore(outputState, p, vec4<f32>(clamp(next, vec2<f32>(0.0), vec2<f32>(1.0)), 0.0, 1.0));
}
```

Le shader pinceau garde les mêmes bindings mais son entrée `paint` calcule le disque périodique à partir du troisième buffer :

```wgsl
@compute @workgroup_size(8, 8)
fn paint(@builtin(global_invocation_id) id: vec3<u32>) {
    if (id.x >= 256u || id.y >= 256u) { return; }
    let p = vec2<i32>(id.xy);
    let raw = abs(vec2<f32>(id.xy) - parameters.xy);
    let d = min(raw, vec2<f32>(256.0) - raw);
    var s = textureLoad(inputState, p, 0).rg;
    if (dot(d, d) <= parameters.z * parameters.z) { s = vec2<f32>(0.5, 1.0); }
    textureStore(outputState, p, vec4<f32>(s, 0.0, 1.0));
}
```

- [ ] **5. Implémenter le rendu et les ressources.** Triangle plein écran `(-1,-1), (3,-1), (-1,3)` ; passer UV avec origine supérieure gauche `(position.xy * vec2(0.5,-0.5) + 0.5)`. Fragment : `textureLoad` aux coordonnées UV×256 bornées [0,255]. Uniform rendu vec4 de 16 octets : palette ordinal, display ordinal, 0, 0. En modes A/B : `vec4(vec3(concentration),1)`. En mode couleur, interpoler trois couleurs avec B : `mix(c0,c1,2*B)` pour B≤0,5, sinon `mix(c1,c2,2*B-1)` ; océan `(0.01,0.03,0.12)→(0,0.8,0.9)→(1,1,1)`, braise `(0,0,0)→(0.9,0.05,0.01)→(1,0.9,0.1)`, gris `(0,0,0)→(0.5,0.5,0.5)→(1,1,1)`.

Créer layouts/pipeline layouts explicites ; ne pas utiliser auto-layout pour le float non filtrable. Vérifier limites `maxTextureDimension2D >= 256`, invocations >=64, tailles workgroup X/Y >=8, workgroups/dimension >=32, sampled textures >=1, storage textures >=1, uniform buffers >=1, maxBufferSize >=16 et maxUniformBufferBindingSize >=16. Aucun workgroup shared storage. Stockage et rendu utilisent des vues séparées des mêmes textures si nécessaire, pas de copie CPU par frame.

Schéma d'encodage à appliquer après validation complète des entrées :

```kotlin
if (brush != null) {
    device.queue.writeBuffer(brushBuffer, 0uL, ArrayBuffer.of(floatArrayOf(brush.x.toFloat(), brush.y.toFloat(), 6f, 0f)))
    val pass = encoder.beginComputePass()
    pass.setPipeline(brushPipeline); pass.setBindGroup(0u, brushGroups[current])
    pass.dispatchWorkgroups(32u, 32u); pass.end()
    current = 1 - current
}
device.queue.writeBuffer(simulationBuffer, 0uL, ArrayBuffer.of(floatArrayOf(parameters.feed, parameters.kill, 0f, 0f)))
repeat(steps) {
    val pass = encoder.beginComputePass()
    pass.setPipeline(simulationPipeline); pass.setBindGroup(0u, simulationGroups[current])
    pass.dispatchWorkgroups(32u, 32u); pass.end()
    current = 1 - current
}
```

Les noms de ressources sont les propriétés privées créées dans `ReactionDiffusionScene`. Précréer deux groupes par pipeline ; le groupe indexé par `current` lit cette texture et écrit l'autre. Le rendu utilise le groupe de la texture `current`, clear opaque et draw(3u). `reset` valide avant écritures, écrit les deux textures via `queue.writeTexture`, `TexelCopyTextureInfo`, `TexelCopyBufferLayout(bytesPerRow=4096u, rowsPerImage=256u)`, puis `current=0`. `encodeStateCopy` utilise `copyTextureToBuffer` avec la même stride. Fermer tous les groupes, pipelines, vues, textures, buffers et layouts possédés ; détruire en ordre inverse les allocations partielles. Fermer les shader modules après création des pipelines.

- [ ] **6. Ajouter la route verify.** Dans `Main.kt`, généraliser le wrapper de `verifyParticles` en `verifyDemo(ids: List<String>, results: suspend (GPUDevice) -> List<CaseResult>)`. Conserver timeout 60 s, identité `SuiteBuildIdentity`, acquisition/fermeture adapter/device et rendu de rapport. `failedDemoReport(ids, diagnostic)` doit créer exactement les ids demandés. La nouvelle route verify appelle les trois ids ; la route interactive affiche encore un diagnostic explicite jusqu'à tâche 4. Maintenir le rejet demo+benchmark et les routes inconnues.
- [ ] **7. Vérifier le vert sur les deux targets.** Build : `./gradlew :suite-demos:compileKotlinJvm :suite-browser:jsBrowserDistribution :suite-browser:wasmJsBrowserDistribution`. Run : `node --test tools/reaction-diffusion.browser.test.mjs`, puis `GRAPHIKS_DISTRIBUTION=suite-browser/build/dist/wasmJs/productionExecutable node --test tools/reaction-diffusion.browser.test.mjs`. Les trois scénarios passent, aucune erreur de page. Ajouter aussi au test une route inconnue et demo+benchmark pour préserver le rejet.
- [ ] **8. Commit :** `feat: add verified portable reaction-diffusion scene`.

## Task 3: Collecte stricte de toutes les démos

**Files:** créer `tools/demo-reports.mjs`, `tools/demo-reports.test.mjs` ; modifier `tools/run-browser.mjs`.

**Interfaces — consumes:** rapports `DemoReport` des deux routes, même identité et schéma 1.
**Interfaces — produces:** `DEMO_ROUTES`, `validateDemoReport(route, report, baseline): string[]`, `aggregateDemoReports(reports): {schemaVersion, buildCommit, buildVersion, cases, fatalError}`. Route = objet `{name, ids}` ; reports = tableau des rapports dans l'ordre des routes.

- [ ] **1. Tests rouges d'agrégation.** Utiliser node:test et node:assert/strict :

```javascript
const baseline = { commit: 'abc', suiteVersion: '0.1.0-SNAPSHOT' };
const good = route => ({ schemaVersion: 1, buildCommit: 'abc', buildVersion: baseline.suiteVersion,
  cases: route.ids.map(id => ({ id, status: 'passed' })), fatalError: null });
test('both routes retain all five cases', () => {
  const reports = DEMO_ROUTES.map(good);
  assert.deepEqual(aggregateDemoReports(reports).cases.map(c => c.id), DEMO_ROUTES.flatMap(r => r.ids));
});
test('rejects incomplete, duplicate, unexpected and failed cases', () => {
  const route = DEMO_ROUTES[1];
  for (const cases of [[], [good(route).cases[0], good(route).cases[0]],
    [...good(route).cases, { id: 'extra', status: 'passed' }],
    good(route).cases.map(c => ({ ...c, status: 'unsupported' }))]) {
    assert.ok(validateDemoReport(route, { ...good(route), cases }, baseline).length > 0);
  }
});
```

Ajouter tests rapport null, JSON invalide dans le lecteur, fatalError, schéma autre que 1, mauvaise identité, une route seulement, rapports d'identités différentes et erreurs page. Attendu : chaque problème interdit une réussite ; l'enveloppe d'échec reste écrite.
- [ ] **2. Run rouge :** `node --test tools/demo-reports.test.mjs` ; import absent attendu.
- [ ] **3. Implémenter catalogue et validation.** Catalogue exact :

```javascript
export const DEMO_ROUTES = [
  { name: 'particles', ids: ['particles.compute-render-readback', 'particles.bounds-pause-reset'] },
  { name: 'reaction-diffusion', ids: ['reaction-diffusion.compute-render-readback',
    'reaction-diffusion.pause-step-reset', 'reaction-diffusion.brush-boundaries'] },
];
export function aggregateDemoReports(reports) {
  if (reports.length !== DEMO_ROUTES.length) throw new Error('Incomplete demo campaign');
  const first = reports[0];
  if (reports.some(r => r.buildCommit !== first.buildCommit || r.buildVersion !== first.buildVersion)) {
    throw new Error('Mixed distribution identities');
  }
  return { schemaVersion: 1, buildCommit: first.buildCommit, buildVersion: first.buildVersion,
    cases: reports.flatMap(r => r.cases), fatalError: reports.map(r => r.fatalError).filter(Boolean).join('\n') || null };
}
```

`validateDemoReport` retourne des diagnostics pour structure invalide, fatalError, schéma, identité/baseline, doublons, manquants, inattendus et tout statut autre que passed ; vérifier `Array.isArray(report.cases)` avant `.map`.

- [ ] **4. Intégrer la campagne.** Garder une seule instance Chromium, mais une page neuve pour chaque route. Pour chacune : naviguer à `?demo=<name>&verify=1`, attendre le global avec timeout existant, parser et valider avant agrégation, fermer dans finally. Les pageErrors sont cumulées avec nom de route. Si une route échoue, tenter quand même la suivante ; enregistrer le diagnostic fatal et les cas obtenus, jamais un rapport complet artificiel. Réutiliser l'enveloppe et le nom `demos-<target>.json`. Le chemin suite/benchmark et ses règles restent inchangés. expectedIds devient `DEMO_ROUTES.flatMap(r => r.ids)` uniquement en mode démos.
- [ ] **5. Vérifier vert :** test Node, puis `node tools/run-browser.mjs js suite-browser/build/dist/js/productionExecutable --demo-check` et commande équivalente wasm. Les rapports contiennent exactement cinq cas passés. Injecter un rapport vide dans le test unitaire et constater le refus, pas dans les rapports publiables.
- [ ] **6. Commit :** `feat: collect and validate both demo reports`.

## Task 4: Interface interactive, état, pointeurs et cycle de vie

**Files:** créer les quatre fichiers Controls/PointerDom/Page/View, leur test Kotlin et les ressources EN/FR du tableau ; modifier les deux Gradle au besoin (commonTest), `Main.kt`, `index.html` et le test navigateur tâche 2.

**Interfaces — consumes:** scène tâche 2, types tâche 1, helpers `ParticlesDom.kt` existants (`listen`, `AbortController`, `fetchText`, surface et animation).

**Interfaces — produces:**

```kotlin
internal class ReactionControls {
    var parameters: ReactionParameters = ReactionPreset.Coral.parameters
    var palette: ReactionPalette = ReactionPalette.Ocean
    var display: ReactionDisplay = ReactionDisplay.Color
    var speed: Int = 8
    var paused: Boolean = false
    fun togglePause()
    fun requestStep()
    fun takeSteps(): Int
    fun selectPreset(preset: ReactionPreset)
}
internal fun reactionBrushAt(clientX: Double, clientY: Double, left: Double, top: Double,
                             width: Double, height: Double): ReactionBrush?
suspend fun showReactionDiffusionPage(locale: String)
```

`ReactionControls` valide paramètres et speed dans leurs setters ; il conserve un booléen de pas demandé, consommé une fois. `selectPreset` change les paramètres ; la page appelle reset et efface pinceau/pas en attente. Reset ne change pas palette, vitesse ou paramètres. `ReactionDiffusionView` retourne les références DOM et un `ReactionTexts` sérialisable ; tous les champs des JSON ont les mêmes clés EN/FR.

- [ ] **1. Tests rouges des contrôles.** Ajouter `implementation(kotlin("test"))` dans commonTest de suite-browser. Tests :

```kotlin
@Test fun singleStepOnlyAdvancesOnceWhilePaused() {
    val c = ReactionControls()
    assertEquals(8, c.takeSteps())
    c.togglePause(); assertEquals(0, c.takeSteps())
    c.requestStep(); assertEquals(1, c.takeSteps()); assertEquals(0, c.takeSteps())
    assertTrue(c.paused)
    c.togglePause(); c.requestStep(); assertEquals(8, c.takeSteps())
}
@Test fun pointerUsesCssCoordinatesAndRejectsHiddenCanvas() {
    assertEquals(ReactionBrush(128, 128), reactionBrushAt(210.0, 220.0, 10.0, 20.0, 400.0, 400.0))
    assertEquals(ReactionBrush(255, 0), reactionBrushAt(999.0, -10.0, 10.0, 20.0, 400.0, 400.0))
    assertNull(reactionBrushAt(0.0, 0.0, 0.0, 0.0, 0.0, 10.0))
    assertNull(reactionBrushAt(Double.NaN, 0.0, 0.0, 0.0, 10.0, 10.0))
}
```

Add speed=0/17 et paramètres NaN rejetés, préréglage rétablit son couple sans changer palette, plusieurs requestStep avant frame donnent un seul pas. Run rouge : `./gradlew :suite-browser:jsBrowserTest --tests '*ReactionDiffusionControlsTest'` (Karma Chromium configuré par Kotlin browser). Si Chrome est absent, installer/configurer le navigateur de test, ne pas déclarer le rouge métier à partir de ce seul blocage.

- [ ] **2. Implémenter les transitions.** Noyau :

```kotlin
private var stepPending = false
fun requestStep() { if (paused) stepPending = true }
fun takeSteps(): Int {
    val result = if (!paused) speed else if (stepPending) 1 else 0
    stepPending = false
    return result
}
```

`togglePause` efface stepPending ; la validation des setters appelle les fonctions tâche 1. Conversion de coordonnées : vérifier toutes valeurs finies et dimensions >0, puis `floor((clientX-left)/width*256).toInt().coerceIn(0,255)` et idem y. Run mêmes tests pour obtenir vert.

- [ ] **3. Ajouter des tests navigateur rouges d'interface.** IDs stables : `reaction-canvas`, `reaction-preset`, `reaction-pause`, `reaction-step`, `reaction-reset`, `reaction-speed`, `reaction-feed`, `reaction-kill`, `reaction-palette`, `reaction-display`, `reaction-lesson`, `reaction-status`. Attendre `data-ready="true"` sur le conteneur après initialisation. Tests exacts de base :

```javascript
await page.goto(`${origin}/?demo=reaction-diffusion&lang=fr`);
await page.locator('[data-ready="true"]').waitFor();
await page.locator('#reaction-pause').click();
assert.equal(await page.locator('#reaction-step').isEnabled(), true);
assert.equal(await page.locator('#reaction-pause').textContent(), 'Reprendre');
await page.locator('#reaction-step').click();
assert.equal(await page.locator('#reaction-pause').textContent(), 'Reprendre');
await page.locator('#reaction-preset').selectOption('Labyrinth');
assert.equal(Number(await page.locator('#reaction-feed').inputValue()), 0.029);
assert.equal(Number(await page.locator('#reaction-kill').inputValue()), 0.057);
```

Ajouter test masque canvas puis resize : aucune pageerror ; révéler canvas permet le rendu. Test pointer : dispatch pointerdown id1, pointerdown id2, pointermove id2, pointercancel id1, pointermove id1 ; vérifier qu'aucun trait id2 n'est appliqué. Après une peinture id1 effectivement rendue en pause, ces événements ignorés doivent laisser une capture identique. Les événements synthétiques vérifient le filtrage ; compléter avec une souris Playwright réelle pour tester `setPointerCapture`, que les événements synthétiques seuls ne garantissent pas.

```javascript
const canvas = page.locator('#reaction-canvas');
const box = await canvas.boundingBox();
await page.mouse.move(box.x + box.width / 2, box.y + box.height / 2);
await page.mouse.down();
await page.evaluate(() => new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve))));
const painted = await canvas.screenshot();
await canvas.dispatchEvent('pointermove', { pointerId: 999, clientX: box.x + 20, clientY: box.y + 20 });
await page.evaluate(() => new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve))));
assert.deepEqual(await canvas.screenshot(), painted);
await page.mouse.up();
```

La page de ce test est mise en pause avant interaction, donc aucune évolution spontanée ne change la capture. Utiliser contexte Playwright `hasTouch:true` et `page.touchscreen.tap` pour une vraie source tactile, pas uniquement un viewport mobile.

- [ ] **4. Implémenter frontière pointeur typée.** Sans changer la signature de `DomEvent`, créer ces interfaces dans PointerDom :

```kotlin
internal external interface ReactionPointerEvent : JsAny {
    val pointerId: Int
    val clientX: Double
    val clientY: Double
    val button: Int
}
internal external interface ReactionRect : JsAny {
    val left: Double; val top: Double; val width: Double; val height: Double
}
internal external interface ReactionPointerCanvas : JsAny {
    fun getBoundingClientRect(): ReactionRect
    fun setPointerCapture(pointerId: Int)
    fun hasPointerCapture(pointerId: Int): Boolean
    fun releasePointerCapture(pointerId: Int)
}
```

Événements reçus par `listen` sont `unsafeCast<ReactionPointerEvent>()`. Capturer seulement le premier pointerdown bouton principal ; stocker id actif et position en attente. Ignore id étranger ; pointerup/cancel/lostpointercapture effacent l'id et libèrent la capture si encore possédée. Une position déjà reçue reste consommable une fois ; cancel efface la position restante. CSS `touch-action:none` seulement sur le canvas. Les interactions en pause planifient une frame, pas une reprise de simulation.

- [ ] **5. Implémenter page/vue/localisation.** Construire inputs avec labels `for`, range feed/kill step 0.0001, vitesse entière 1..16, selects presets/palettes/display, pause/un pas/reset, source et status role=status. Aucun innerHTML pour texte ou shaders. Valeurs de ressource essentielles : FR pause="Pause", resume="Reprendre", step="Un pas", reset="Réinitialiser" ; EN pause="Pause", resume="Resume", step="One step", reset="Reset". Textes doivent expliquer « étapes par frame, pas secondes physiques ». Status loading/unavailable/failed/lost/reload et tous les autres labels sont traduits.

Créer surface et scène sous scope de validation comme `ParticlesPage`, mais `awaitLost()` dans une coroutine possédée par la page. `fail` annule RAF, désactive contrôles GPU et montre erreur + bouton rechargement. Utiliser contrôleur abort pour listeners, `MainScope` pour initialisation/loss et cleanup idempotent. Vérifier `closed` après chaque acquisition suspendue, fermer les ressources nouvellement obtenues si page fermée ; ne pas lancer une boucle tardive.

La boucle n'a qu'un handle RAF ; au callback, le remettre à null, sortir si fermé, effectuer takeSteps et consommer un pinceau, rendre puis reprogrammer seulement si simulation active. Les changements palette/display/resize/reset et un pas demandent une frame en pause. Un canvas sans dimensions saute le rendu sans consommer le pinceau ni le pas. Caper la résolution physique du canvas au `maxTextureDimension2D`, comme les particules. Fermer la vue cible et le command buffer après soumission ; ne pas détruire la texture de surface empruntée.

CSS ciblé pour ne pas modifier les particules :

```css
.reaction-demo canvas { aspect-ratio: 1; touch-action: none; }
.reaction-demo .demo-controls input { font: inherit; max-width: 100%; }
.reaction-demo pre { overflow-x: auto; max-width: 100%; }
.reaction-demo details { min-width: 0; }
```

Main route interactive appelle désormais `showReactionDiffusionPage(selectedLocale())` ; le verify reste inchangé.

- [ ] **6. Tester perte et fermeture pendant init.** Dans tests navigateur, `page.addInitScript` enveloppe navigator.gpu.requestAdapter puis adapter.requestDevice, expose uniquement dans le test `globalThis.__reactionTestDevice` et une gate de résolution requestDevice. Appeler device.destroy via cette référence après ready : status data-error=true, tous contrôles GPU disabled, pas de RAF encore actif. Pour gate, émettre pagehide avant résolution, relâcher gate : aucun ready, device tardif détruit. Instrumenter RAF dans addInitScript pour compter handles ; après pagehide/erreur, taille du Set =0. Garder ces hooks hors production. Vérifier aussi requestAdapter retournant null affiche indisponibilité sans simulation de secours.
- [ ] **7. Vert JS/Wasm.** Run tests Kotlin, rebuild des deux distributions, puis les deux variantes du test navigateur tâche 2. Inclure une locale de ressource manquante simulée par interception HTTP : diagnostic visible, pas page vide. Aucun résultat de ces tests n'est présenté comme une exécution GPU native.
- [ ] **8. Commit :** `feat: add interactive bilingual reaction-diffusion controls`.

## Task 5: Pédagogie et galerie des deux démos

**Files:** modifier View, ressources JSON et `site/demos/app.js` ; créer `tools/demos-gallery.browser.test.mjs` ; étendre le test navigateur réaction-diffusion.

**Interfaces — consumes:** `ReactionSimulationShader`, `ReactionBrushShader`, `ReactionRenderShader` tâche 2 ; titres/descriptions JSON ; routes tâche 4.
**Interfaces — produces:** panneau pédagogique replié par défaut ; galerie qui rend chaque démo indépendamment.

- [ ] **1. Tests rouges du panneau et de la galerie.** Test panneau : ouvrir `#reaction-lesson`, vérifier les trois sources complètes dans `<pre><code>` et explications de l'injection + ping-pong ; changer display A/B et constater que la palette/paramètres restent inchangés. Vérifier `details` fermé initialement.

Le test galerie sert les fichiers `site` comme le serveur test tâche 2, et intercepte les ressources attendues avec `page.route` ; ne pas écrire ces fixtures dans build/reports. Réponse demo dict = `{title,description}` selon locale, report fixture = identité commit connue. Assertions :

```javascript
assert.equal(await page.locator('#gallery article').count(), 2);
assert.equal(await page.locator('a[href="../run/js/?demo=reaction-diffusion&lang=fr"]').count(), 1);
assert.equal(await page.locator('a[href="../run/wasm/?demo=particles&lang=fr"]').count(), 1);
assert.ok(await page.locator('#gallery').textContent());
```

Ajouter rapport absent : source master et avertissement ; ressource réaction-diffusion 404 : carte particules conservée + diagnostic propre à la carte manquante. Ajouter titre hostile `<img src=x onerror=...>` : présent comme texte, aucun élément img créé.
- [ ] **2. Rouge :** `node --test tools/demos-gallery.browser.test.mjs`, puis test réaction-diffusion sur build tâche 4. Attendu : une seule carte et panneau absent.
- [ ] **3. Implémenter panneau.** `details/summary` avec explication feed/kill, diagramme texte « Lecture A → compute → écriture B → échange », explication de passe pinceau, étapes par frame et séparation palette/état. Trois blocs code alimentés directement par les constantes WGSL, `setText`, pas copie de shader dans les JSON. Affichage concentrations est le select existant Color/A/B ; ne pas ajouter une seconde simulation pédagogique.
- [ ] **4. Implémenter catalogue de galerie.** Remplacer DEMO/SCENE_DIR/SCENE_SOURCES uniques par :

```javascript
const DEMOS = [
  { name: 'particles', directory: 'particles',
    sources: ['ParticleScene.kt', 'ParticleShaders.kt', 'ParticleData.kt'] },
  { name: 'reaction-diffusion', directory: 'reactiondiffusion',
    sources: ['ReactionDiffusionScene.kt', 'ReactionDiffusionShaders.kt', 'ReactionDiffusionData.kt'] },
];
```

Passer demo à `renderCard(demo, dict, commit)` ; dict URL `../run/js/demos/${demo.name}.${locale}.json`, launch URL `../run/${target}/?demo=${demo.name}&lang=${locale}`, source dir `suite-demos/src/commonMain/kotlin/org/graphiks/webgpu/suite/demos/${demo.directory}`. Charger publishedCommit une fois ; chaque ressource a son try/catch afin qu'une démo absente ne supprime pas l'autre. Conserver source lié au commit publié quand disponible, avertissement autrement, et textContent pour tout contenu.
- [ ] **5. Vert et contrôle visuel.** Rebuild, run tests galerie et tests navigateur JS/Wasm. Avec le navigateur OpenChamber, examiner viewport desktop puis mobile, panneau fermé/ouvert, trois presets après évolution et palettes ; vérifier pas d'overflow global. Pour différencier presets, reset avant chaque observation et laisser chacun au moins 2000 étapes ; consigner captures et observations, ne pas annoncer une forme d'après les paramètres seuls.
- [ ] **6. Commit :** `feat: publish reaction-diffusion gallery and learning view`.

## Task 6: Campagne finale, documentation et preuves

**Files:** modifier `README.md`, `docs/running.md`, `docs/verification.md`, `CHANGELOG.md` ; mettre à jour checkboxes du plan/spec. Lire `.github/workflows` pour vérifier que `--demo-check` couvre automatiquement les deux routes ; ne changer CI que si ce n'est pas le cas.

**Interfaces — consumes:** deux distributions, collecteur cinq ids, site, tests des tâches précédentes.
**Interfaces — produces:** instructions reproductibles et preuves correspondant à de vrais runs.

- [ ] **1. Écrire les instructions avant la campagne.** README décrit désormais les deux scènes et non une seule ; docs/running décrit routes, contrôles, modèle de vitesse et cinq ids exacts ; changelog documente ajout, pas promesse de publication. Documenter reprise par un binding natif via création/device/encoder/cible/close sans prétendre l'avoir exécutée. docs/verification ne reçoit des résultats qu'après les runs.
- [ ] **2. Vérifier tous les tests et builds.** Commandes à exécuter séparément, JS/Wasm GPU séquentiels :

```sh
./gradlew :suite-demos:jvmTest :suite-browser:jsBrowserTest :suite-browser:wasmJsBrowserTest
node --test tools/demo-reports.test.mjs tools/demos-gallery.browser.test.mjs
./gradlew :suite-core:compileKotlinJvm :suite-demos:compileKotlinJvm :suite-demos:compileKotlinMacosArm64 :suite-browser:jsBrowserDistribution :suite-browser:wasmJsBrowserDistribution
node --test tools/reaction-diffusion.browser.test.mjs
GRAPHIKS_DISTRIBUTION=suite-browser/build/dist/wasmJs/productionExecutable node --test tools/reaction-diffusion.browser.test.mjs
node tools/run-browser.mjs js suite-browser/build/dist/js/productionExecutable --demo-check
node tools/run-browser.mjs wasm suite-browser/build/dist/wasmJs/productionExecutable --demo-check
node tools/run-browser.mjs js suite-browser/build/dist/js/productionExecutable
node tools/run-browser.mjs wasm suite-browser/build/dist/wasmJs/productionExecutable
node tools/build-site.mjs
git diff --check
```

Les tests existants Node `tools/arraybuffer-report.test.mjs` et `tools/compare-arraybuffer-benchmarks.test.mjs` restent verts ; les exécuter avec node --test. Aucun seuil de durée n'est un critère de réussite. Linux native compilé uniquement sur hôte compatible ; rapporter une cible non exécutée comme telle.
- [ ] **3. Inspecter les rapports réels.** Vérifier schemaVersion, buildCommit/version correspondant au baseline généré, cinq ids uniques/passés, aucune fatalError ni pageErrors pour démos. Vérifier rapports acid tests séparés toujours présents ; build-site copie deux rapports agrégés aux noms inchangés. Le site ne requiert pas de nouveau benchmark.
- [ ] **4. Documenter les preuves.** Dans docs/verification, ajouter commandes réellement exécutées, date, identité embarquée des distributions, environnement/backend, résultats, captures et limites (GPU browser JS/Wasm, compilation native seulement). Si un outil manque ou un test échoue, conserver le diagnostic et ne pas annoncer la campagne complète. Après le commit docs, ne pas relabeller les anciens rapports avec le nouveau HEAD ; l'identité du build testé reste la référence.
- [ ] **5. Revue et commit.** Charger verification-before-completion, examiner diff/spec ligne à ligne ; charger requesting-code-review selon la méthode choisie par l'utilisateur. Vérifier aucune dépendance ni modification API générée non prévue, aucune fixture devenue résultat publié. Commit : `docs: document reaction-diffusion usage and verification`.

## Auto-relecture du plan

- [x] Intention, périmètre et données numériques : tâche 1.
- [x] Ping-pong, ressources, rendu, injection, pause/reset et tests GPU : tâche 2.
- [x] Routes et collecte stricte préservant les particules : tâches 2 et 3.
- [x] Interface EN/FR, entrées, cycle de vie et cinq Review Focus : tâches 2 à 4.
- [x] Pédagogie, galerie et contrôle visuel : tâche 5.
- [x] Documentation, builds et preuves sans confusion native/browser : tâche 6.
- [x] Noms, packages et signatures cohérents entre tâches ; pas de stub de production à garder.
- [x] Plan relu et approuvé par l'utilisateur.
- [x] Méthode d'exécution choisie par l'utilisateur : native, 2026-10-10.

## Handoff

Recommandation : exécution **native dans cette session**, car les six tâches sont
séquentielles et partagent les contrats de scène, de page et de rapport. Cela évite
de recharger le même contexte pour chaque tâche. Une revue indépendante de branche
reste prévue à la fin si cette méthode est choisie.

Alternative : exécution **avec sous-agents**, un implémenteur puis un reviewer frais
par tâche, et une revue globale finale ; davantage de contrôles indépendants, au prix
de contextes supplémentaires. Aucun agent ne sera lancé avant ce choix.
