# Graphiks WebGPU Suite — Plan 2 : démo de particules compute

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking. Exécution confiée à un autre agent dans un worktree du dépôt actuel `webgpu`. Ce document est un plan à relire, pas une implémentation exécutée.

**Goal:** Livrer une première démo visuelle portable, pédagogique et interactive, dans `suite-demos`, avec lancement JS/Wasm et une entrée Démos sur le site existant.

**Architecture:** Une classe `ParticleScene` possède les ressources GPU ; le runner fournit device, format et cible de rendu. Une passe compute met à jour un buffer de positions/vitesses, ensuite utilisé comme vertex buffer instancié. Le navigateur gère le canvas, les contrôles et l’animation ; aucune dépendance navigateur n’entre dans la scène.

**Tech Stack:** Conventions `kmp` et `publish`, catalogue et wrapper existants : Kotlin 2.4.20, JDK 25, coroutines et serialization du dépôt, WGSL, HTML/CSS/JavaScript natifs, Playwright déjà installé par le workflow Suite.

**Spec:** `docs/superpowers/specs/2026-09-27-webgpu-suite-design.md`, actualisée après `b4d750a` (PR #120).

## Global Constraints

- Tout réside dans ce dépôt. Ajouter un module Gradle, pas un dépôt, un sous-module Git ou un build autonome.
- Lisibilité humaine et usage WebGPU priment : commandes et shaders explicites, pas de moteur de scènes, registre générique ou framework de rendu.
- Réutiliser les conventions, le catalogue, `releaseVersion`, le site et la publication existants.
- Compilation et lancement des vrais modules valident l’intégration. Aucun test d’architecture, de publication Maven ou de consommateur artificiel.
- Les vérifications automatisées nouvelles portent sur des résultats GPU : valeurs du calcul et pixels réellement rendus.
- `suite-demos` et `suite-acid-tests` ne dépendent pas l’un de l’autre. Une démo visible ne devient pas implicitement une preuve de couverture du contrat.
- Préserver les onze acid tests, leurs annotations, l’inventaire généré, les rapports existants et la séparation JS/Wasm.
- Pas de benchmark, compteur FPS présenté comme une mesure, temps GPU ni classement dans ce lot.
- Commandes locales préfixées par `rtk` ; commandes natives dans GitHub Actions.
- Les extraits de ce plan sont à compiler et exécuter par l’agent : aucune réussite GPU n’est attestée ici.

## Review Focus

1. Effectif non multiple de 64 : le compute doit protéger les invocations hors buffer — lecture des 65 particules, tâche 4.
2. Passage compute → vertex buffer dans une seule soumission : vérifier les positions calculées et les pixels sans lecture CPU entre les deux passes — tâche 4.
3. Pause, reset et changement d’effectif : aucune boucle concurrente, aucune accumulation du temps pendant une pause, nettoyage de l’ancienne scène — tâche 3, lancement interactif.
4. Canvas redimensionné ou masqué : taille physique bornée, pas d’acquisition d’une texture de taille nulle, proportions préservées — tâches 2 et 3, inspection visuelle.
5. Échec de création, erreur GPU et départ de page : diagnostic visible et fermeture des ressources possédées, jamais destruction d’une texture empruntée au canvas — tâches 1 et 3.

---

## Point de départ et limites du lot

Baseline examinée : `b4d750a`. Lire les guides `docs/running.md`, `docs/adding-a-case.md` et les builds des trois modules existants. Le catalogue des acid tests est généré ; ne pas modifier un ancien `FoundationCases.kt` manuscrit provenant du plan 1.

La première scène est volontairement simple : particules colorées qui se déplacent et rebondissent dans un carré, sans interaction entre particules. Elle démontre compute, stockage GPU et rendu instancié sans imposer une simulation physique complexe. L’interface explique le chemin des données et donne accès aux sources.

Effectif par défaut : 4 096, plafonné par les limites disponibles. Choix proposés : 256, 1 024, 4 096, 16 384 et 65 536, filtrés selon le device. La graine initiale est fixe (`1u`) ; reset reproduit la même configuration pour un effectif donné. Aucun besoin de feature optionnelle.

### Préparation

- [ ] Vérifier `rtk git status --short` et préserver les fichiers déjà modifiés, notamment les documents de conception.
- [ ] Lire la spec et ce plan, puis les signatures actuelles de `GPUCommandEncoder`, `GPURenderPassEncoder`, `CanvasSurface`, `RenderPipelineDescriptor`, `VertexState` et `FragmentState`.
- [ ] Confirmer la baseline par une compilation de `suite-browser` puis les commandes JS/Wasm de `docs/running.md`. Consigner une panne préexistante sans la transformer en régression de ce lot.

## Fichiers et interfaces

| Fichier | Rôle |
| --- | --- |
| `suite-demos/build.gradle.kts` | Module portable publié |
| `suite-demos/src/commonMain/kotlin/org/graphiks/webgpu/suite/demos/particles/ParticleData.kt` | Initialisation reproductible et limite d’effectif |
| `suite-demos/src/commonMain/kotlin/org/graphiks/webgpu/suite/demos/particles/ParticleShaders.kt` | Deux sources WGSL lisibles |
| `suite-demos/src/commonMain/kotlin/org/graphiks/webgpu/suite/demos/particles/ParticleScene.kt` | Ressources, compute, rendu et fermeture |
| `suite-browser/src/commonMain/kotlin/org/graphiks/webgpu/suite/browser/Main.kt` | Choix explicite entre validation et démo |
| `suite-browser/src/commonMain/kotlin/org/graphiks/webgpu/suite/browser/demos/ParticlesPage.kt` | Cycle de vie et boucle navigateur |
| `suite-browser/src/commonMain/kotlin/org/graphiks/webgpu/suite/browser/demos/ParticlesDom.kt` | Petite frontière DOM partagée JS/Wasm |
| `suite-browser/src/commonMain/kotlin/org/graphiks/webgpu/suite/browser/demos/ParticleGpuChecks.kt` | Scénarios exécutables de readback, activés explicitement |
| `suite-browser/src/commonMain/resources/demos/particles.{en,fr}.json` | Textes de la démo et de sa carte de galerie |
| `site/demos/index.html`, `site/demos/app.js` | Première entrée de galerie, choix JS/Wasm et liens de code |
| `tools/run-browser.mjs` | Mode additionnel de vérification GPU de la démo |
| `.github/workflows/suite.yml`, `.github/workflows/publish.yml` | Vérifications GPU et artifact supplémentaire |

Les chemins Kotlin des tâches ci-dessous désignent les fichiers complets de cette table. Les fichiers existants sont modifiés de façon ciblée.

## Task 1 : créer la scène portable et ses ressources

**Files:** créer les trois fichiers Kotlin et le build de `suite-demos` ; modifier `settings.gradle.kts` et `suite-browser/build.gradle.kts`.

**Interfaces publiques :**

```kotlin
fun initialParticles(count: Int, seed: UInt = 1u): FloatArray
fun maxParticleCount(limits: GPUSupportedLimits): Int

class ParticleScene private constructor(/* ressources privées */) : AutoCloseable {
    val count: Int
    val particleBuffer: GPUBuffer // emprunté par le lecteur ; fermé par la scène
    fun reset(initial: FloatArray)
    fun encodeFrame(
        encoder: GPUCommandEncoder,
        target: GPUTextureView,
        width: Int,
        height: Int,
        deltaSeconds: Float,
    )
    override fun close()

    companion object {
        fun create(device: GPUDevice, format: GPUTextureFormat, initial: FloatArray): ParticleScene
    }
}
```

Le constructeur privé contient les ressources décrites ci-dessous, pas un conteneur générique. `particleBuffer` permet la composition et les readbacks ; documenter qu’il ne doit pas être fermé par le consommateur. `encodeFrame` emprunte encoder et vue, écrit les paramètres via la queue mais ne soumet pas. Un appel correspond à une soumission : ne pas encoder plusieurs frames avec des valeurs de paramètres différentes avant de soumettre.

- [ ] **1. Ajouter le module aux conventions existantes.**

```kotlin
plugins { kmp; publish }
kotlin {
    jvmToolchain(25)
    jvm()
    js { browser() }
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs { browser() }
    linuxX64()
    macosArm64()
    compilerOptions.optIn.add("kotlin.ExperimentalUnsignedTypes")
    sourceSets.commonMain.dependencies {
        api(project(":suite-core"))
        implementation(project(":webgpu-descriptors"))
    }
}
```

Ajouter `include("suite-demos")` et `implementation(project(":suite-demos"))` dans le runner. Ne pas introduire de nouveau contrat abstrait dans `suite-core` pour une unique scène.

- [ ] **2. Écrire les données initiales.** Format d’une particule : quatre floats `[x, y, vx, vy]`, stride 16 octets. Utiliser un LCG UInt local reproductible :

```kotlin
var state = seed
fun nextUnit(): Float {
    state = state * 1664525u + 1013904223u
    return (state shr 8).toFloat() / 16777216f
}
```

Pour chaque particule, positions dans `[-0.85, 0.85]`, vitesses dans `[-0.3, 0.3]`, via `nextUnit`. La fonction retourne un `FloatArray(count * 4)`. Rejeter un effectif hors `1..65536` avant toute allocation. La limite utilisable est le minimum, calculé en ULong, de `65536`, `maxBufferSize / 16`, `maxStorageBufferBindingSize / 16`, `maxComputeWorkgroupsPerDimension * 64`. Vérifier aussi que le device permet un workgroup de 64 sur X et 64 invocations ; sinon présenter une incompatibilité explicite, pas un succès silencieux.

- [ ] **3. Créer les ressources avec des descripteurs explicites.**

| Ressource | Taille/usages |
| --- | --- |
| Particules | `initial.size * 4` octets ; `Storage or Vertex or CopyDst or CopySrc` |
| Paramètres | 16 octets ; `Uniform or CopyDst` |
| Paramètres CPU | `ArrayBuffer.allocate(16uL)` réutilisé |
| Pipeline compute | layout automatique, module compute ci-dessous |
| Bind group compute | binding 0 = particules, binding 1 = paramètres |
| Pipeline render | layout automatique, format reçu, vertex buffer instancié |
| Bind group render | binding 0 = paramètres |

Valider `initial.size % 4 == 0`, l’effectif, des valeurs finies et positions initiales dans `[-0.95,0.95]`, avant allocation GPU. Charger les données par `device.queue.writeBuffer(particleBuffer, 0uL, ArrayBuffer.of(initial))`. `reset` exige le même effectif et réécrit les données ; changer d’effectif recrée une scène.

Le factory doit fermer les ressources déjà créées si une création Kotlin échoue. Utiliser des variables locales et un `try/catch` avec fermeture inverse, sans nouveau gestionnaire de ressources. Les modules shaders et layouts temporaires sont fermés après construction. `close` ferme bind groups, pipelines et buffers possédés, est idempotent et ne ferme jamais le device. Les erreurs GPU asynchrones seront capturées par le runner de la tâche 3.

- [ ] **4. Compiler le module** avec `rtk proxy ./gradlew :suite-demos:compileKotlinJvm :suite-demos:compileKotlinJs :suite-demos:compileKotlinWasmJs`. Les méthodes sont implémentées avec les shaders et commandes de la tâche 2 avant le commit ; ne pas laisser de méthodes vides publiables.

## Task 2 : encoder compute puis rendu instancié

**Files:** `ParticleShaders.kt`, `ParticleScene.kt`.

**Interfaces:** implémente `encodeFrame` et complète le factory de la tâche 1. `deltaSeconds=0` rend l’état actuel sans compute.

- [ ] **1. Écrire le shader compute dans une constante multiline.**

```wgsl
struct Particle { position: vec2f, velocity: vec2f }
struct Parameters { delta: f32, aspect: f32, radius: f32, padding: f32 }
@group(0) @binding(0) var<storage, read_write> particles: array<Particle>;
@group(0) @binding(1) var<uniform> parameters: Parameters;

@compute @workgroup_size(64)
fn update(@builtin(global_invocation_id) id: vec3u) {
    if (id.x >= arrayLength(&particles)) { return; }
    var p = particles[id.x];
    p.position += p.velocity * parameters.delta;
    if (p.position.x > 0.95) { p.position.x = 0.95; p.velocity.x = -abs(p.velocity.x); }
    if (p.position.x < -0.95) { p.position.x = -0.95; p.velocity.x = abs(p.velocity.x); }
    if (p.position.y > 0.95) { p.position.y = 0.95; p.velocity.y = -abs(p.velocity.y); }
    if (p.position.y < -0.95) { p.position.y = -0.95; p.velocity.y = abs(p.velocity.y); }
    particles[id.x] = p;
}
```

- [ ] **2. Écrire le shader de rendu.** Les particules sont des disques opaques colorés, sans texture ou blend requis :

```wgsl
struct Parameters { delta: f32, aspect: f32, radius: f32, padding: f32 }
@group(0) @binding(0) var<uniform> parameters: Parameters;
struct VertexOutput {
    @builtin(position) position: vec4f,
    @location(0) local: vec2f,
    @location(1) color: vec3f,
}
@vertex
fn vertexMain(@location(0) center: vec2f, @builtin(vertex_index) index: u32,
              @builtin(instance_index) instance: u32) -> VertexOutput {
    let corners = array<vec2f, 6>(vec2f(-1,-1), vec2f(1,-1), vec2f(-1,1),
                                 vec2f(-1,1), vec2f(1,-1), vec2f(1,1));
    let local = corners[index];
    // Le carré simulé est centré et conserve ses proportions dans le canvas.
    let scale = vec2f(min(1.0 / parameters.aspect, 1.0), min(parameters.aspect, 1.0));
    var out: VertexOutput;
    out.position = vec4f((center + local * parameters.radius) * scale, 0, 1);
    out.local = local;
    let shade = f32(instance % 7u) / 6.0;
    out.color = vec3f(0.25 + shade * 0.65, 0.8, 1.0 - shade * 0.4);
    return out;
}
@fragment
fn fragmentMain(input: VertexOutput) -> @location(0) vec4f {
    if (dot(input.local, input.local) > 1.0) { discard; }
    return vec4f(input.color, 1);
}
```

- [ ] **3. Définir le layout vertex.**

```kotlin
VertexState(
    module = renderShader,
    entryPoint = "vertexMain",
    buffers = listOf(VertexBufferLayout(
        arrayStride = 16uL,
        attributes = listOf(VertexAttribute(GPUVertexFormat.Float32x2, 0uL, 0u)),
        stepMode = GPUVertexStepMode.Instance,
    )),
)
```

Utiliser `FragmentState(targets = listOf(ColorTargetState(format)), module = renderShader, entryPoint = "fragmentMain")`, triangle list par défaut, sans profondeur ni MSAA. Obtenir séparément le layout de bind group de chaque pipeline automatique ; ne pas partager un bind group entre layouts automatiques différents.

- [ ] **4. Implémenter `encodeFrame`.** Exiger dimensions positives, delta fini dans `0f..0.05f` et scène ouverte. Écrire `delta` à l’octet 0, `width.toFloat()/height` à 4, rayon `0.018f` à 8, zéro à 12, puis `queue.writeBuffer(parametersBuffer, 0uL, parametersData)`.

```kotlin
if (deltaSeconds > 0f) {
    val compute = encoder.beginComputePass()
    compute.setPipeline(computePipeline)
    compute.setBindGroup(0u, computeGroup)
    compute.dispatchWorkgroups(((count + 63) / 64).toUInt())
    compute.end()
}
val render = encoder.beginRenderPass(RenderPassDescriptor(
    colorAttachments = listOf(RenderPassColorAttachment(
        view = target,
        loadOp = GPULoadOp.Clear,
        storeOp = GPUStoreOp.Store,
        clearValue = Color(0.0, 0.0, 0.0, 1.0),
    )),
))
render.setPipeline(renderPipeline)
render.setBindGroup(0u, renderGroup)
render.setVertexBuffer(0u, particleBuffer)
render.draw(6u, count.toUInt())
render.end()
```

Ne pas faire de `use` sur les passes : elles n’implémentent pas `AutoCloseable`. Aucune lecture CPU des particules par frame.

- [ ] **5. Compiler puis commiter tâches 1 et 2 ensemble** : `feat: add portable compute particle scene`. Ce premier commit porte une scène complète compilable ; la preuve GPU suit dans les tâches 3 et 4.

## Task 3 : lancer la scène dans le navigateur avec ses contrôles

**Files:** `Main.kt`, `ParticlesPage.kt`, `ParticlesDom.kt`, les deux ressources JSON ; modifier `suite-browser/src/commonMain/resources/index.html`.

**Interfaces :** `suspend fun showParticlesPage(locale: String)` ; route `?demo=particles&lang=en|fr`. Sans `demo`, conserver exactement l’exécution de validation actuelle et `graphiksSuiteReport`.

- [ ] **1. Ajouter une bifurcation explicite avant `runFoundations`.** Lire les query parameters via un petit helper `js(...)`, compatible JS/Wasm, comme `publishReport` existant. `demo=particles` lance la scène ; une valeur inconnue affiche une erreur. Aucun routeur générique. Prévoir la route `demo=particles&verify=1` pour la tâche 4.

- [ ] **2. Charger les textes externes.** Ressources JSON avec clés `title`, `description`, `pause`, `resume`, `reset`, `count`, `source`, `unavailable`, `failed`, `loading`. Locale `en` par défaut ; reprendre le paramètre `lang` et le choix `graphiks-suite-locale` du site. Afficher le diagnostic technique avec `textContent`. Le texte pédagogique explique : CPU initialise une fois → compute met à jour le buffer → le rendu lit ce buffer directement.

- [ ] **3. Construire le DOM minimal.** Canvas, contrôles accessibles par clavier et libellés associés, zone de statut, lien de retour Validation, lien vers le code. Mettre les accès DOM dans `ParticlesDom.kt` : helpers `js` de création/lecture/écriture, aucun `dynamic` dans le code commun Wasm. Pour les callbacks, réutiliser le style d’interop JS/Wasm existant ; adapter la signature réelle de `requestAnimationFrame` avec un callback `(Double) -> Unit`, et conserver l’ID pour `cancelAnimationFrame`.

Le canvas est obtenu comme `org.graphiks.webgpu.browser.HTMLCanvasElement`, puis `getCanvasSurface()`. Acquérir adapter et device ; `preferredCanvasFormat ?: error(...)` doit produire une erreur visible. Configurer `SurfaceConfiguration(device, format)` ; aucune feature optionnelle demandée.

- [ ] **4. Encadrer l’initialisation par un error scope.** Capturer `Validation` pendant la construction/configuration, attendre `popErrorScope`, refuser de lancer l’animation si une erreur est présente. Installer le callback `onUncapturedError` à la demande du device pour arrêter la boucle, afficher le diagnostic et nettoyer. Si la construction échoue après création partielle, fermer scène, surface, device, adapter dans l’ordre inverse. Un rejet de `requestAdapter` ou `requestDevice` suit le même chemin visible.

- [ ] **5. Implémenter la boucle unique.** `lastTime` est remis à zéro à chaque pause, reprise, reset et retour d’onglet. Delta = différence RAF en secondes, bornée à `0.05f` ; première frame = zéro. En pause, ne pas mettre à jour la simulation ; un resize ou reset peut demander un rendu avec delta nul. Pas de rattrapage du temps passé dans un onglet masqué.

À chaque rendu, calculer largeur/hauteur physiques depuis dimensions CSS et `devicePixelRatio`, plafonnées à `device.limits.maxTextureDimension2D` ; ignorer une taille CSS nulle. Actualiser les dimensions du canvas si besoin, acquérir la texture courante, créer une vue, encoder et soumettre une seule frame. Fermer la vue, l’encoder et le command buffer après soumission. Ne pas détruire la texture canvas empruntée et ne pas stocker sa vue pour la frame suivante.

- [ ] **6. Brancher les contrôles.** Pause/reprise change l’état de la boucle, reset réécrit `initialParticles(scene.count, 1u)`. Pour changer l’effectif : arrêter la planification RAF, désactiver les contrôles, attendre `queue.onSubmittedWorkDone`, fermer la scène, créer la nouvelle sous scope de validation, rendre une frame initiale, restaurer pause/reprise. Un drapeau de transition empêche deux changements concurrents. Filtrer les options selon `maxParticleCount`, avec message explicite si aucune option ne convient. Le nettoyage de page annule RAF, retire les listeners et ferme la scène, surface, device et adapter une seule fois.

- [ ] **7. Lancer JS puis Wasm.** Utiliser les tâches `:suite-browser:jsBrowserDevelopmentRun` et `:suite-browser:wasmJsBrowserDevelopmentRun`, ajouter `?demo=particles&lang=fr` à l’URL. Vérifier mouvement, proportions, pause, reset, changement d’effectif, resize, onglet masqué, navigation et indisponibilité WebGPU. Noter le comportement observé, sans tests de structure DOM.
- [ ] **8. Commiter** : `feat: run interactive particles on JS and Wasm`.

## Task 4 : vérifier les données et les pixels sur GPU réel

**Files:** `ParticleGpuChecks.kt`, branche de vérification de `Main.kt`, `tools/run-browser.mjs`, `.github/workflows/suite.yml`. Ajouter `implementation(kotlin("test"))` à `suite-browser` pour ces assertions exécutables de démo.

**Interfaces :** `suspend fun checkParticleGpu(device: GPUDevice): Unit`, rapport `graphiksDemoReport` distinct du rapport d’acid tests. N’ajouter aucune dépendance de `suite-acid-tests` vers la démo et aucune entrée factice à l’inventaire de conformité.

- [ ] **1. Écrire le cas compute → rendu → readback.** Créer une texture offscreen 256×256, format `Rgba8Unorm`, usages `RenderAttachment or CopySrc`. Créer une scène avec une particule `[0,0,1,0]`. Sous scope Validation, encoder une frame de `0.02f`, puis, dans le même encoder, copier le buffer de particules vers un staging `MapRead | CopyDst` de 16 octets et la texture vers un staging de 262 144 octets (`bytesPerRow=1024`, `rowsPerImage=256`). Soumettre, mapper et attendre :

```kotlin
val actual = particleStaging.getMappedRange().toFloatArray()
assertTrue(kotlin.math.abs(actual[0] - 0.02f) < 0.00001f)
assertTrue(kotlin.math.abs(actual[1]) < 0.00001f)
assertEquals(1f, actual[2])
assertEquals(0f, actual[3])
val pixels = pixelStaging.getMappedRange().toUByteArray()
val center = 128 * 1024 + 130 * 4
assertTrue(pixels[center].toInt() > 40)
assertTrue(pixels[center + 1].toInt() > 150)
assertTrue(pixels[center + 2].toInt() > 200)
assertEquals(255u.toUByte(), pixels[center + 3])
assertEquals(0u.toUByte(), pixels[0])
assertEquals(0u.toUByte(), pixels[1])
assertEquals(0u.toUByte(), pixels[2])
```

Le pixel choisi est à l’intérieur du disque déplacé, pas sur son bord. Lire un fond noir seul ne suffit pas. Unmap dans `finally`, fermer toutes les ressources créées, vérifier le scope vide. L’attente vient du calcul et de la couleur connus, pas d’une image de référence produite par la même exécution.

- [ ] **2. Vérifier bornes, rebonds et pause.** Créer 65 particules : première `[0.94,0,1,0]`, dernière `[-0.94,0,-1,0]`, autres `[0,0,0.5,0]`. Après une frame `0.02f`, lire les 65 entrées : première `x≈0.95, vx=-1`, dernière `x≈-0.95, vx=1`, les 63 autres `x≈0.01, vx=0.5`. Tolérance positions `1e-5`. Le workgroup partiel doit rester sans erreur de validation. Encoder une seconde frame à delta zéro : les données lues doivent être inchangées. Appeler `reset` avec le tableau d’origine, rendre à delta zéro, relire les données et les comparer au tableau initial.

- [ ] **3. Ajouter le lancement automatisable.** Le mode `?demo=particles&verify=1` crée son propre adapter/device, exécute ces deux scénarios avec timeout réel de 60 secondes et publie un JSON sérialisé dans `globalThis.graphiksDemoReport` : deux IDs `particles.compute-render-readback` et `particles.bounds-pause-reset`, statut et diagnostic. Un échec d’initialisation est fatal, pas un skip. Fermer adapter/device dans `finally` et propager les erreurs inattendues vers le rapport.

- [ ] **4. Étendre le collecteur existant de façon explicite.** Ajouter un argument optionnel `--demo-check` à `tools/run-browser.mjs`. Ce mode ouvre la route de vérification, attend `graphiksDemoReport`, contrôle les deux résultats GPU et écrit `build/reports/demos-<target>.json`. Le mode existant, ses onze IDs générés et ses rapports restent distincts. Réutiliser serveur, lancement Chromium, environnement et gestion d’erreur du script ; ne pas créer un projet de test d’intégration de l’architecture.

```sh
rtk proxy ./gradlew :suite-browser:jsBrowserDistribution :suite-browser:wasmJsBrowserDistribution
rtk proxy node tools/run-browser.mjs js suite-browser/build/dist/js/productionExecutable --demo-check
rtk proxy node tools/run-browser.mjs wasm suite-browser/build/dist/wasmJs/productionExecutable --demo-check
```

Ajouter les deux commandes au workflow Suite, avant l’assemblage du site, avec collecte des rapports même en cas d’échec. Rejouer aussi les onze acid tests via les commandes existantes. La démo ne doit pas rendre leur contrôle de complétude moins strict.
- [ ] **5. Commiter après résultats observés** : `test: verify particle compute and rendering on real WebGPU`.

## Task 5 : publier la galerie et l’artifact

**Files:** `site/demos/index.html`, `site/demos/app.js`, `site/index.html`, `site/app.js`, `site/style.css`, `tools/build-site.mjs`, `buildSrc/src/main/kotlin/publish.gradle.kts`, `.github/workflows/publish.yml`, `README.md`, `docs/running.md`, `docs/verification.md`, `docs/docs/suite.md`, `docs/docs/suite.fr.md`.

- [ ] **1. Ajouter la galerie.** Une carte « Particules compute » avec description courte et boutons JS/Wasm. Les liens relatifs sont `../run/js/?demo=particles&lang=…` et `../run/wasm/?demo=particles&lang=…`. Charger les textes de carte depuis `../run/js/demos/particles.<locale>.json`, déjà copiés avec la distribution, afin de ne pas doubler les traductions. Ajouter des liens Validation ↔ Démos. Réutiliser la sélection EN/FR et les styles existants ; ne pas générer le catalogue de démos avec le générateur d’acid tests.

- [ ] **2. Donner accès aux sources du build présenté.** Utiliser le `suiteCommit` d’un rapport publié pour construire les liens GitHub vers `ParticleScene.kt`, `ParticleShaders.kt` et `ParticleData.kt`. Les liens restent dans `Graphiks-org/WebGPU`. Si le commit est absent, afficher un lien vers le dossier de sources sans prétendre qu’il correspond au build. La galerie ne fait pas passer un résultat de démo pour une mesure de performance ou une couverture exhaustive.

- [ ] **3. Assembler dans le site existant.** `build-site.mjs` copie déjà `site/` et les deux distributions. Ajouter la copie des rapports `demos-js.json` et `demos-wasm.json` ; leur absence après une construction de ce lot fait échouer l’assemblage, comme les rapports de validation. Conserver l’unique déploiement `_site/suite/` du workflow Documentation. Inspecter la galerie, les deux lancements et les liens de sources sous le préfixe réel et sur écran mobile.

- [ ] **4. Publier via la convention existante.** Ajouter la description `"suite-demos" -> "Portable demonstrations of the Graphiks WebGPU public API"` au `when` de `publish.gradle.kts`, et `:suite-demos:publishToMavenCentral` aux commandes snapshot/release existantes. Coordonnée : `org.graphiks:suite-demos`, version commune. Aucun repository Maven isolé ni consommateur artificiel. Compiler JVM, JS, Wasm et les deux cibles natives sur les hôtes compatibles avec les tâches normales du module.

- [ ] **5. Documenter le lancement natif par son vrai contrat.** Montrer `ParticleScene.create(device, format, initialParticles(4096))`, puis `encodeFrame` avec une vue fournie par l’application et une soumission par frame. Expliquer la propriété des ressources et l’appel final `close()`. Ne pas ajouter une application native dans ce dépôt. Documenter les routes navigateur, les deux vérifications GPU, les limites d’effectif et l’absence de benchmark.

- [ ] **6. Consigner les preuves.** Dans `docs/verification.md`, nouvelle section datée : commit, commandes, deux résultats de démo par cible, état des onze acid tests, navigateur/backend demandé et observations manuelles. Distinguer compilation native et exécution GPU native. Vérifier `rtk git diff --check`, relire les docs puis commiter `feat: publish particle demo gallery and shared artifact`.

## Revue du plan

| Exigence | Tâches |
| --- | --- |
| Module portable, conventions et publication existantes | 1 et 5 |
| Scène lisible compute → rendu | 1 et 2 |
| Pause, reset, effectif et cycle de vie | 3 |
| JS et Wasm, erreurs visibles | 3 et 4 |
| Attentes métier sur calcul et pixels, pas tests d’architecture | 4 |
| Galerie pédagogique EN/FR et code source | 3 et 5 |
| Préservation de l’inventaire et des onze acid tests | 4 et 5 |
| Benchmarks | Hors de ce lot, plan suivant |

- [x] Signatures des descripteurs, commandes et surfaces confrontées aux sources de la baseline.
- [x] Ownership explicite ; aucune passe traitée comme `AutoCloseable` ni texture canvas détruite par la scène.
- [x] Les cinq risques de Review Focus ont une vérification GPU ou de lancement attribuée.
- [x] Aucun nouveau test de publication/consommation ou de structure de l’architecture.
- [ ] Revue du plan par l’utilisateur avant transmission à l’agent d’exécution.

**Transmission :** lire la spec actualisée et ce plan ; implémenter les cinq tâches dans ce dépôt. Conserver les choix intégrés du premier incrément, notamment annotations, inventaire généré et i18n. Privilégier une scène explicite et de petites fonctions plutôt qu’un framework. Fournir les résultats réellement exécutés. Ne pas interpréter la présence d’extraits dans ce plan comme une preuve de compilation ou d’exécution.
