# Instanced Cyberpunk City Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ajouter une ville cyberpunk nocturne interactive et pédagogique, troisième démo portable vérifiée sur JS et Wasm JS.

**Architecture:** Cube indexé partagé + buffer par instance, profondeur et deux dessins par frame (sol puis bâtiments). La scène portable possède les ressources GPU ; le runner possède DOM, caméra interactive et animation. Les outils existants agrègent les trois routes sans modifier les rapports acid/benchmarks.

**Tech Stack:** Kotlin Multiplatform, API/descripteurs Graphiks WebGPU, WGSL, coroutines/serialization existants, JDK 25, Gradle wrapper, Node/Playwright existants, Dokka/MkDocs.

**Spec:** `docs/superpowers/specs/2026-10-10-instanced-cyberpunk-city-design.md`

## Global Constraints

- « Trois densités : 256, 1024 et 4096 bâtiments » ; « Valeur par défaut : 4096, ou la plus grande option admise par les limites du device. »
- « Les trois grilles occupent le même secteur de 128 × 128 unités monde » ; « Hauteurs de base entre 2 et 14 unités. »
- « Chaque instance contient huit Float32 (stride de 32 octets) » : `[centreX, centreZ, largeur, profondeur, hauteur, couleur, motif, 0]`.
- « Le curseur de hauteur applique un facteur uniforme entre 0,5 et 2,0, défaut 1,0. »
- « Cube partagé : 24 sommets » ; « 36 indices UInt16 » ; « un second appel dessine le sol et ses rues ».
- « Utiliser Depth24Plus, écriture de profondeur activée, comparaison Less » ; sample count 1, aucune feature optionnelle.
- « Caméra initiale : azimut 45°, élévation 35°, distance 180 unités » ; distance [100 ; 300], élévation [15° ; 75°], cible (0, 8, 0).
- « Perspective, champ vertical 50°, near 0,1, far 1024 » ; profondeur WebGPU [0 ; 1].
- « Rotation automatique active par défaut, 0,1 radian/seconde » ; reduced motion désactive le défaut ; delta max 0,05 s.
- « Pas de pan ni de pinch » ; glisser + molette normalisée + curseur de distance, un seul pointeur.
- « Pas de compute dans cette démo » ; pas de bloom, trafic, ombres projetées, import ni dépendance supplémentaire.
- Une frame encodée = une soumission avant la suivante ; la scène ne ferme pas son device emprunté.
- Préserver les cinq ids précédents ; huit cas au total dans les rapports démos existants. Pas de couverture acid supplémentaire ni promesse de performance.
- Branche `feat/instanced-cyberpunk-city` basée sur `203f040`, dépendante de la PR #141 encore ouverte à la rédaction ; ne pas pousser la ville dans cette PR.

## Review Focus

1. Fermeture avant textes/device ou remplacement de densité terminé : aucune création tardive, aucune fuite, ancienne scène conservée si remplacement échoue — tâches 3 et 5.
2. Canvas masqué/déconnecté/rectangle nul, onglet repris : ni acquisition de surface ni saut d'orbite, dernier contrôle conservé — tâche 5.
3. Ordre d'instances inversé et profondeur redimensionnée : bâtiment proche visible, pas de profondeur périmée — tâche 3.
4. Pointeur capturé hors canvas, second pointeur, cancel ; molette deltaMode 0/1/2 : caméra finie/bornée et capture nettoyée — tâches 4 et 5.
5. Troisième route manquante/malformée ou mauvais commit : échec conservé, huit vrais ids exigés, autres routes encore exécutées — tâche 6.

## Prévol et découpage

Ne pas commencer l'implémentation avant approbation de ce plan. Après approbation, confirmer
l'isolation existante et suivre CONTRIBUTING : issue de coordination pour ce changement substantiel,
liée à la session dès création, sans prétendre qu'elle précédait la conception déjà faite.
Ne pas modifier ni fusionner la PR #141 sans demande. Avant une future PR ville, vérifier son état
et isoler les commits ville selon la stratégie convenue (base fusionnée ou PR dépendante explicitement choisie).

Chemins exacts ; les noms ci-dessous sont aussi les contrats entre tâches :

| Fichier | Action / responsabilité |
| --- | --- |
| `suite-demos/src/commonMain/kotlin/org/graphiks/webgpu/suite/demos/instancedcity/CityData.kt` | créer : densités, génération et validation |
| `suite-demos/src/commonMain/kotlin/org/graphiks/webgpu/suite/demos/instancedcity/CityGeometry.kt` | créer : cube indexé partagé |
| `suite-demos/src/commonMain/kotlin/org/graphiks/webgpu/suite/demos/instancedcity/CityCamera.kt` | créer : caméra, matrices portables |
| `suite-demos/src/commonMain/kotlin/org/graphiks/webgpu/suite/demos/instancedcity/CityShaders.kt` | créer : WGSL réel |
| `suite-demos/src/commonMain/kotlin/org/graphiks/webgpu/suite/demos/instancedcity/InstancedCityScene.kt` | créer : ressources et encodage |
| `suite-demos/src/commonTest/kotlin/org/graphiks/webgpu/suite/demos/instancedcity/CityDataTest.kt` | créer : génération/validation |
| `suite-demos/src/commonTest/kotlin/org/graphiks/webgpu/suite/demos/instancedcity/CityGeometryTest.kt` | créer : géométrie/culling |
| `suite-demos/src/commonTest/kotlin/org/graphiks/webgpu/suite/demos/instancedcity/CityCameraTest.kt` | créer : projection/bornes |
| `suite-browser/src/commonMain/kotlin/org/graphiks/webgpu/suite/browser/demos/CityGpuChecks.kt` | créer : trois fixtures GPU |
| `suite-browser/src/commonMain/kotlin/org/graphiks/webgpu/suite/browser/demos/CityControls.kt` | créer : état pur, gestes, cadence |
| `suite-browser/src/commonMain/kotlin/org/graphiks/webgpu/suite/browser/demos/CityPointerDom.kt` | créer : frontière JS/Wasm typée |
| `suite-browser/src/commonMain/kotlin/org/graphiks/webgpu/suite/browser/demos/CityView.kt` | créer : DOM et textes, pas de GPU |
| `suite-browser/src/commonMain/kotlin/org/graphiks/webgpu/suite/browser/demos/CityPage.kt` | créer : bootstrap, ressources, RAF |
| `suite-browser/src/commonTest/kotlin/org/graphiks/webgpu/suite/browser/demos/CityControlsTest.kt` | créer : pointeurs/zoom/rotation |
| `suite-browser/src/commonMain/resources/demos/instanced-city.en.json` et `instanced-city.fr.json` | créer : textes complets |
| `suite-browser/src/commonMain/kotlin/org/graphiks/webgpu/suite/browser/Main.kt` | modifier : route/checks |
| `suite-browser/src/commonMain/kotlin/org/graphiks/webgpu/suite/browser/demos/DemoNavigation.kt` | modifier : troisième option |
| `suite-browser/src/commonMain/resources/index.html` | modifier : CSS ville responsive |
| `tools/instanced-city.browser.test.mjs` | créer : navigateur JS/Wasm |
| `tools/demo-reports.mjs`, `tools/demo-reports.test.mjs` | modifier : huit cas, route ville |
| `tools/reaction-diffusion.browser.test.mjs` | modifier : sélecteur à trois choix |
| `site/demos/app.js`, `tools/demos-gallery.browser.test.mjs` | modifier : troisième carte |
| `README.md`, `docs/running.md`, `docs/verification.md`, `docs/verification.fr.md`, `CHANGELOG.md` | modifier : usage/preuves |

Tests/configuration existants suffisent : kotlin(test) est déjà dans commonTest des deux modules,
Karma ChromeHeadless configuré. Réutiliser `tools/browser-test-server.mjs`, sans nouveau serveur
de production ni dépendance npm. Ne pas toucher aux sources générées des bindings.

### Task 1: Données déterministes et cube indexé

**Files:** `CityData.kt`, `CityGeometry.kt`, `CityDataTest.kt`, `CityGeometryTest.kt` aux chemins du tableau.

**Interfaces — produces:**

```kotlin
const val CityInstanceFloats = 8
const val CityInstanceBytes = 32
enum class CityDensity(val count: Int, val side: Int) {
    Sparse(256, 16), Medium(1024, 32), Dense(4096, 64)
}
fun cityGridStep(density: CityDensity): Float = 128f / density.side
fun initialCityInstances(density: CityDensity, seed: UInt = 1u): FloatArray
fun validateCityInstances(instances: FloatArray)
fun cityCubeVertices(): FloatArray // 24 * 8, independent fresh allocation
fun cityCubeIndices(): UShortArray // 36, independent fresh allocation
```

- [ ] **1. Écrire les tests rouges.** Package `org.graphiks.webgpu.suite.demos.instancedcity`, imports kotlin.test. Assertions sur vrais buffers, pas grep des sources :

```kotlin
@Test fun denseGridFitsTheSectorAndIsReproducible() {
    val a = initialCityInstances(CityDensity.Dense)
    assertEquals(4096 * 8, a.size)
    assertContentEquals(a, initialCityInstances(CityDensity.Dense))
    assertEquals(-63f, a[0]); assertEquals(-63f, a[1])
    assertEquals(63f, a[a.size - 8]); assertEquals(63f, a[a.size - 7])
    validateCityInstances(a)
    a[0] = 0f
    assertEquals(-63f, initialCityInstances(CityDensity.Dense)[0])
}
@Test fun invalidFootprintsAndMetadataAreRejected() {
    val good = floatArrayOf(0f, 0f, 4f, 4f, 8f, 0f, 12f, 0f)
    validateCityInstances(good)
    for ((offset, bad) in listOf(0 to 64f, 2 to 0f, 3 to Float.NaN,
        4 to 15f, 5 to 0.5f, 6 to 65536f, 7 to 1f)) {
        val data = good.copyOf(); data[offset] = bad
        assertFailsWith<IllegalArgumentException> { validateCityInstances(data) }
    }
    assertFailsWith<IllegalArgumentException> { validateCityInstances(FloatArray(0)) }
    assertFailsWith<IllegalArgumentException> { validateCityInstances(FloatArray(9)) }
}
```

Ajouter tableaux Sparse/Medium : centres extrêmes ±60/±62 et pas 8/4. Pour chaque cellule,
vérifier largeur/profondeur dans [0,45*pas ; 0,75*pas], hauteur [2 ; 14], couleur entière,
motif entier [0 ; 65535]. Une autre graine doit modifier des dimensions, pas déplacer la grille.
Géométrie : 192 floats/36 indices tous <24 ; chaque triangle a une aire non nulle et son
produit vectoriel pointe dans la normale stockée, six normales axiales, positions dans les bornes.
Exemple central de ce test (les opérations de vectoriel sont écrites dans le test) :

```kotlin
val abx = bx - ax; val aby = by - ay; val abz = bz - az
val acx = cx - ax; val acy = cy - ay; val acz = cz - az
val dot = (aby * acz - abz * acy) * nx +
    (abz * acx - abx * acz) * ny + (abx * acy - aby * acx) * nz
assertTrue(dot > 0f, "Triangle must face outward for back-face culling")
```

- [ ] **2. Exécuter rouge.** `./gradlew :suite-demos:jvmTest --tests '*City*Test'` ; échec attendu API absente, pas erreur de configuration.
- [ ] **3. Implémenter le générateur.** LCG portable, cinq tirages par cellule dans ordre largeur, profondeur, hauteur, couleur, motif ; tous les entiers sont exacts, pas de Random de plateforme :

```kotlin
var state = seed
fun next(): UInt { state = state * 1664525u + 1013904223u; return state }
fun unit(): Float = (next() shr 8).toFloat() / 16777216f
val step = cityGridStep(density)
val result = FloatArray(density.count * CityInstanceFloats)
for (z in 0 until density.side) for (x in 0 until density.side) {
    val cx = -64f + (x + 0.5f) * step
    val cz = -64f + (z + 0.5f) * step
    val width = step * (0.45f + 0.30f * unit())
    val depth = step * (0.45f + 0.30f * unit())
    val centrality = (1f - kotlin.math.sqrt(cx * cx + cz * cz) / 90.51f).coerceIn(0f, 1f)
    val height = 2f + 12f * (0.25f + 0.75f * centrality) * unit()
    val color = (next() and 1u).toFloat()
    val pattern = (next() and 65535u).toFloat()
    val offset = (z * density.side + x) * CityInstanceFloats
    floatArrayOf(cx, cz, width, depth, height, color, pattern, 0f).copyInto(result, offset)
}
return result
```

Validation require valeurs finies, dimensions >0, `abs(cx)+width/2<=64`, même Z,
hauteur [2 ; 14], color exactement0/1, motif `value==value.toInt().toFloat()` et domaine,
padding0. Pas de limite 256 sur les fixtures : toute quantité positive permise par le device
est valide, seuls les choix UI sont 256/1024/4096.

- [ ] **4. Implémenter le cube.** Construire chaque face à partir d'un centre `c`, deux axes `u,v`
de longueur1 et normale `cross(u,v)` ; quatre coins `c-u/2-v/2`, `c+u/2-v/2`,
`c+u/2+v/2`, `c-u/2+v/2`, UV (0,0),(1,0),(1,1),(0,1). Faces :

```text
normal +Z: c=(0,.5,.5), u=(1,0,0), v=(0,1,0)
normal -Z: c=(0,.5,-.5), u=(-1,0,0), v=(0,1,0)
normal +X: c=(.5,.5,0), u=(0,0,-1), v=(0,1,0)
normal -X: c=(-.5,.5,0), u=(0,0,1), v=(0,1,0)
normal +Y: c=(0,1,0), u=(1,0,0), v=(0,0,-1)
normal -Y: c=(0,0,0), u=(1,0,0), v=(0,0,1)
indices per face base: [0,1,2, 0,2,3] + base
```

- [ ] **5. Vert puis commit.** `./gradlew :suite-demos:jvmTest` ; puis `git add` les quatre fichiers et `git commit -m "feat: add deterministic city instances and shared geometry"`.

### Task 2: Caméra et projection portable

**Files:** `CityCamera.kt`, `CityCameraTest.kt`.

**Interfaces — consumes:** aucune API GPU ; dimensions physiques positives.
**Interfaces — produces:**

```kotlin
data class CityCamera(val azimuth: Float = (kotlin.math.PI / 4).toFloat(),
    val elevation: Float = (35 * kotlin.math.PI / 180).toFloat(), val distance: Float = 180f)
fun validateCityCamera(camera: CityCamera)
fun cityViewProjection(camera: CityCamera, width: Int, height: Int): FloatArray // 16, column-major
internal fun cityPerspective(aspect: Float): FloatArray
internal fun multiplyCityMatrices(a: FloatArray, b: FloatArray): FloatArray
```

- [ ] **1. Tests rouges mathématiques indépendants.** Helpers uniquement dans le test pour multiplier un vecteur par matrice et comparer abs<1e-5 ; valeurs de profondeur calculées à la main :

```kotlin
@Test fun perspectiveMapsNearAndFarToWebGpuDepth() {
    val p = cityPerspective(1f)
    fun depth(z: Float): Float = (p[10] * z + p[14]) / (p[11] * z + p[15])
    assertTrue(kotlin.math.abs(depth(-0.1f)) < 0.00001f)
    assertTrue(kotlin.math.abs(depth(-1024f) - 1f) < 0.00001f)
    assertTrue(kotlin.math.abs(cityPerspective(2f)[0] * 2f - p[0]) < 0.00001f)
}
@Test fun orbitTargetProjectsToCenter() {
    val m = cityViewProjection(CityCamera(0f, (kotlin.math.PI/4).toFloat(), 180f), 256, 256)
    val clipX = m[4] * 8f + m[12]
    val clipY = m[5] * 8f + m[13]
    assertTrue(kotlin.math.abs(clipX) < 0.0001f)
    assertTrue(kotlin.math.abs(clipY) < 0.0001f)
    val w = m[7] * 8f + m[15]
    assertTrue(kotlin.math.abs(w - 180f) < 0.0001f)
}
```

Ajouter caméra azimut0 : un point X positif se projette à droite ; au-dessus cible se projette
en Y NDC positif. Vérifier matrices finies pour les quatre extrêmes elevation/distance.
Rejeter NaN/Inf, distance99/301, élévation14°/76°, width0/height0 avant division.

- [ ] **2. Rouge.** `./gradlew :suite-demos:jvmTest --tests '*CityCameraTest'`.
- [ ] **3. Implémenter en column-major.** Eye = `(d*cos(e)*sin(a), 8+d*sin(e), d*cos(e)*cos(a))`.
`z=normalize(eye-target)`, `x=normalize(cross(up,z))`, `y=cross(z,x)`. Vue :

```text
[ x.x, y.x, z.x, 0,
  x.y, y.y, z.y, 0,
  x.z, y.z, z.z, 0,
  -dot(x,eye), -dot(y,eye), -dot(z,eye), 1 ]
Projection: f=1/tan(25°), n=.1, far=1024
[ f/aspect,0,0,0,  0,f,0,0,  0,0,far/(n-far),-1,
  0,0,far*n/(n-far),0 ]
result = projection * view
multiply: out[col*4+row] = sum(a[k*4+row] * b[col*4+k], k=0..3)
```

Validation elevation utilise les mêmes constantes radians15/75, distance100/300 ; azimut fini
sans restriction modulo (le contrôleur le normalise). Matrices publiques retournées fraîches.
- [ ] **4. Vert/commit.** `./gradlew :suite-demos:jvmTest`, `git commit -m "feat: add portable city orbit projection"` avec les deux fichiers explicitement stagés.

### Task 3: Scène GPU, shaders et vérification hors écran

**Files:** `InstancedCityScene.kt`, `CityShaders.kt`, `CityGpuChecks.kt`, `Main.kt`, créer `tools/instanced-city.browser.test.mjs`.
**Consumes:** données/géométrie tâche1, caméra/matrice tâche2, `DemoReport`, `CaseResult`, `verifyDemo` existants.
**Produces:**

```kotlin
enum class CityDisplay { Neon, InstanceColors } // second mode for verification, not UI
class InstancedCityScene : AutoCloseable {
    val count: Int
    companion object {
        fun create(device: GPUDevice, format: GPUTextureFormat,
            instances: FloatArray = initialCityInstances(CityDensity.Dense)): InstancedCityScene
        fun supportedDensities(limits: GPUSupportedLimits): List<CityDensity>
    }
    fun encodeFrame(encoder: GPUCommandEncoder, target: GPUTextureView, width: Int, height: Int,
        camera: CityCamera, heightScale: Float = 1f, gridStep: Float = 2f,
        display: CityDisplay = CityDisplay.Neon)
    override fun close()
}
val CityBuildingShader: String
val CityGroundShader: String
internal val CityCheckIds: List<String>
internal suspend fun cityGpuResults(device: GPUDevice): List<CaseResult>
```

- [ ] **1. Tests navigateur rouges.** Réutiliser server helper, env `GRAPHIKS_DISTRIBUTION`, before/after, page fermée dans finally :

```javascript
test('city verification reports three genuine GPU checks', {timeout:150000}, async () => {
  const page = await server.browser.newPage();
  try {
    await page.goto(`${server.origin}/?demo=instanced-city&verify=1`);
    await page.waitForFunction(() => typeof globalThis.graphiksDemoReport === 'string'
      || document.body.textContent.includes("Unknown demo 'instanced-city'"));
    const report = await page.evaluate(() => typeof graphiksDemoReport === 'string'
      ? JSON.parse(graphiksDemoReport) : null);
    assert.ok(report, 'The city verification route must exist');
    assert.equal(report.fatalError ?? null, null);
    assert.deepEqual(report.cases.map(c => c.id), [
      'instanced-city.instances-render-readback', 'instanced-city.depth-occlusion',
      'instanced-city.camera-resize-lifecycle']);
    assert.ok(report.cases.every(c => c.status === 'passed'), JSON.stringify(report));
  } finally { await page.close(); }
});
```

`node --test tools/instanced-city.browser.test.mjs` doit échouer route absente contre distribution
précédente. Écrire ensuite les checks Kotlin avant la scène ; `:suite-browser:compileKotlinJs`
doit échouer scène absente. Scopes validation push/pop await, CancellationException relancée,
les autres erreurs deviennent failed CaseResult ; pas de succès synthétique ni unsupported.

- [ ] **2. Fixtures et assertions GPU explicites.** Offscreen RGBA8Unorm, projection réelle,
camera azimut0, elevation15° et distance100. Mode InstanceColors : cyan `(0,1,1,1)`, magenta
`(1,0,1,1)` ; même vertex shader et même profondeur que Neon. Fixture instances :

```kotlin
val separate = floatArrayOf(
    -12f, 0f, 8f, 12f, 10f, 0f, 0f, 0f,
     12f, 0f, 16f, 12f, 14f, 1f, 0f, 0f)
val overlap = floatArrayOf(
     0f, 12f, 12f, 12f, 14f, 0f, 0f, 0f,
     0f,-12f, 12f, 12f, 14f, 1f, 0f, 0f)
```

Pour 256² et separate, assertions intérieures : (100,140) cyan, (156,140) magenta,
(180,140) magenta pour discriminer largeur16 vs8 ; (156,112) magenta et (100,112)
non-cyan pour discriminer hauteur14 vs10. Ces positions ont été vérifiées à la rédaction avec
un calcul scalaire indépendant ray/cube, pas `cityViewProjection` utilisée pour l'attendu.
Le rayon est dérivé de tan25°, eye `(0,8+100*sin15°,100*cos15°)` et basis
right=(1,0,0), up=(0,cos15°,-sin15°), forward=(0,-sin15°,-cos15°).
Chaque pixel centre `(px+.5,py+.5)` donne NDC ; intersecter les AABB. Pour les trois
pixels à y140, le paramètre du rayon à la face d'entrée est environ94,947 ; le pixel
(156,112) entre par le toit vers97,329, et le rayon(100,112) manque le cube de hauteur10.
Conserver ces fixtures/coordonnées ; ne pas remplacer les assertions par un scan qui se
contente de trouver une couleur attendue dans la sortie GPU.

Overlap : centre(128,128) cyan, même en inversant les deux tranches de huit floats. Ce rayon
intersecte bien les deux : à z12, y≈11,2 ; à z-12, y≈4,8. Tester également un pixel magenta
non recouvert si disponible via calcul, ou un rendu far-only magenta au centre pour prouver
que la fixture lointaine est réellement présente.

Camera/resize : overlap, azimut0 centre cyan → azimutPI centre magenta → cible512×256
avec azimut0 centre(256,128) cyan → retour256² cyan. Tester heightScaleNaN/0, width0,
camera invalide : rejet avant queue writes. Close deux fois, encode sur scène fermée rejeté,
device.queue.onSubmittedWorkDone encore réussit. Chaque cible/buffer/vue/scene dans use/finally.
Readback : rowBytes=`((width*4+255)/256)*256`, staging rowBytes*height, copie texture,
mapAsync(Read), supprimer padding par rangée, unmap enfin close. Tolérance±2 sur RGB, alpha255.

- [ ] **3. Implémenter layouts, uniformes et gestion des ressources.** Shader uniform commun :

```wgsl
struct Frame { viewProjection: mat4x4<f32>, options: vec4<f32> }
@group(0) @binding(0) var<uniform> frame: Frame;
// options = [heightScale, gridStep, display.ordinal, 0], buffer 80 bytes
struct BuildingIn {
    @location(0) position: vec3<f32>, @location(1) normal: vec3<f32>,
    @location(2) uv: vec2<f32>, @location(3) footprint: vec4<f32>,
    @location(4) metadata: vec4<f32>,
}
```

Vertex buffer stride32 : locations0 Float32x3 offset0,1 Float32x3 offset12,2 Float32x2 offset24.
Instance stride32 StepMode.Instance : location3 Float32x4 offset0,4 Float32x4 offset16.
Uniform binding visibility Vertex|Fragment, minBindingSize80. Valider maxBufferSize pour
instances et géométrie, maxUniformBufferBindingSize>=80, maxVertexBuffers>=2,
maxVertexAttributes>=5, maxVertexBufferArrayStride>=32, maxBindingsPerBindGroup>=1,
maxBindGroups>=1, maxUniformBuffersPerShaderStage>=1. Retirer les densités qui dépassent
la taille admissible ; ne pas imposer une feature compute/storage inexistante dans cette scène.
Instance/index/vertex buffers CopyDst|Vertex/Index ; uploader avec ArrayBuffer.of, UInt16 exact.

Owned resources reverse-close sur création partielle, close idempotent ; profondeur hors liste
immutable des allocations fixes, avec son propre remplacement atomique :

```kotlin
val replacement = device.createTexture(TextureDescriptor(
    size = Extent3D(width.toUInt(), height.toUInt()), format = GPUTextureFormat.Depth24Plus,
    usage = GPUTextureUsage.RenderAttachment))
val replacementView = try { replacement.createView() } catch (failure: Throwable) {
    replacement.close(); throw failure
}
// Publish replacement texture/view and dimensions only after both succeeded;
// close old view then old texture. Never append resized textures to a growing owned list.
```

Pour erreurs de validation asynchrones, le runner encadre la création dans error scope avant
d'accepter une scène candidate. Frame checks avant uniform write ; limites de taille texture
vérifiées avant profondeur. Queue writes 20 floats=16 matrix+4options.

- [ ] **4. Implémenter le WGSL compilé.** World position = `(local.x*width+cx, local.y*height*scale,
local.z*depth+cz)`, position=`frame.viewProjection*vec4(world,1)`. Varyings normale, UV,
dimensions et metadata avec flat interpolation sur couleur/motif. Fragment InstanceColors
retourne les couleurs exactes avant motifs/éclairage. Neon : ambient(.06,.04,.10) plus
diffuse max(dot(normal,normalize(vec3(.4,1,.2))),0)*.12 ; façades foncées.
Fenêtres : cellules `uv*vec2(max(2,width*2),max(3,height*scale*2))`, éclairées selon hash
entier du couple floor(cell) + motif (u32), seuil fixe ~60%, centre de cellule avec bordure.
Masque antialias smoothstep avec fwidth ; pas de fenêtres si abs(normal.y)>.5.
Neon cyan=(.05,.9,1), magenta=(1,.05,.65). Tous les outputs alpha1.
Sol : six sommets générés par vertex_index dans [-80,80] XZ à y0, UV monde ; routes centrées
aux frontières des cellules (`fract((world.xz+64)/gridStep)`), lignes fines cyan/magenta,
hors secteur couleur sombre sans routes. Culling None pour le sol, Back/CCW bâtiments.

Encodage central :

```kotlin
val pass = encoder.beginRenderPass(RenderPassDescriptor(
    colorAttachments = listOf(RenderPassColorAttachment(target, loadOp = GPULoadOp.Clear,
        storeOp = GPUStoreOp.Store, clearValue = Color(0.005, 0.005, 0.025, 1.0))),
    depthStencilAttachment = RenderPassDepthStencilAttachment(view = depthView,
        depthClearValue = 1f, depthLoadOp = GPULoadOp.Clear, depthStoreOp = GPUStoreOp.Store)))
pass.setPipeline(groundPipeline); pass.setBindGroup(0u, frameGroup); pass.draw(6u)
pass.setPipeline(buildingPipeline); pass.setBindGroup(0u, frameGroup)
pass.setVertexBuffer(0u, vertices); pass.setVertexBuffer(1u, instances)
pass.setIndexBuffer(indices, GPUIndexFormat.Uint16)
pass.drawIndexed(36u, count.toUInt()); pass.end()
```

Main : branche `demo=="instanced-city" && verify=="1"` appelle `verifyDemo(CityCheckIds,::cityGpuResults)`.
Sans verify, message explicite « interactive page is not available yet » jusqu'à tâche5,
pas de suite validation silencieuse. Mettre à jour le diagnostic unknown route.

- [ ] **5. Vert JVM et deux runners, commit.** Commandes :

```sh
./gradlew :suite-demos:jvmTest :suite-demos:compileKotlinMacosArm64 :suite-browser:jsBrowserDistribution :suite-browser:wasmJsBrowserDistribution
node --test tools/instanced-city.browser.test.mjs
GRAPHIKS_DISTRIBUTION=suite-browser/build/dist/wasmJs/productionExecutable node --test tools/instanced-city.browser.test.mjs
```

Les trois ids doivent réellement passer avant `git commit -m "feat: add verified instanced city render scene"`.

### Task 4: Contrôles purs de caméra et temps

**Files:** `CityControls.kt`, `CityControlsTest.kt`.
**Consumes:** CityCamera, CityDensity, validateCityCamera.
**Produces:**

```kotlin
internal class CityControls(reducedMotion: Boolean = false) {
    var density: CityDensity
    var heightScale: Float // validating setter .5..2
    var rotating: Boolean // default !reducedMotion
    val camera: CityCamera
    val dragging: Boolean
    fun setDistance(distance: Float) // finite; clamps 100..300
    fun resetView() // preserves density/heightScale/rotating
    fun pointerDown(id: Int, x: Double, y: Double): Boolean
    fun pointerMove(id: Int, x: Double, y: Double)
    fun pointerEnd(id: Int)
    fun clearPointer()
    fun wheel(deltaY: Double, deltaMode: Int, viewportHeight: Double)
    fun advance(timestampMs: Double, visible: Boolean): Boolean // true if camera changed
    fun resetClock()
}
```

- [ ] **1. Tests rouges.** CPU sans DOM, captures testées séparément tâche5 :

```kotlin
@Test fun secondPointerCannotChangeAnOrbitOrEndItsDrag() {
    val c = CityControls(true)
    assertTrue(c.pointerDown(1, 10.0, 10.0))
    assertFalse(c.pointerDown(2, 100.0, 100.0))
    val before = c.camera
    c.pointerMove(2, 0.0, 0.0); c.pointerEnd(2)
    assertEquals(before, c.camera); assertTrue(c.dragging)
    c.pointerMove(1, 110.0, 10.0)
    assertTrue(kotlin.math.abs(c.camera.azimuth - before.azimuth - 0.5f) < 0.00001f)
    c.pointerEnd(1); assertFalse(c.dragging)
}
@Test fun invisibleFramesAndLongGapsDoNotJumpTheCamera() {
    val c = CityControls()
    c.advance(1000.0, true); val first = c.camera
    c.advance(2000.0, false); assertEquals(first, c.camera)
    c.advance(10000.0, true); assertEquals(first, c.camera)
    c.advance(10020.0, true)
    assertTrue(kotlin.math.abs(c.camera.azimuth - first.azimuth - 0.002f) < 0.00001f)
    val before = c.camera; c.advance(20000.0, true)
    assertTrue(kotlin.math.abs(c.camera.azimuth - before.azimuth - 0.005f) < 0.00001f)
}
```

Wheel table : delta40 pixels,2.5 lines,0.1 pages at viewport400 all normalize40 ; distance
increases by4 (0.1unit/pixel). Delta±1e6 clamp100/300 ; NaN ignored, deltaMode3 ignored,
negative/nonfinite viewport ignored for mode2. Drag dy±1e6 clamps15/75°, NaN coords ignored,
clearPointer terminates without applying another gesture. Reset garde trois paramètres non-caméra.
Reduced-motion default rotating false ; explicit rotating=true marche, toggling appelle resetClock
pour la première frame sans mouvement ; advance ignore backward/nonfinite timestamp and resets clock.

- [ ] **2. Rouge.** `./gradlew :suite-browser:jsBrowserTest --tests '*CityControlsTest'`.
- [ ] **3. Implémenter état et gestes.** Azimut dx*.005 radians, elevation=-dy*.005 clamped,
last coordonnées mises à jour seulement pour id actif valide ; azimut normalisé [0,2PI).
wheel px = deltaY*(1,16,viewportHeight) selon mode0/1/2 ; distance +px*.1 clamp.
Rotation : mémoriser previous nullable, dt=((timestamp-prev)/1000).coerceIn(0,.05),
delta=dt*.1 ; invisible/dragging/!rotating remet previous=null. Validation setters avant mutation.
- [ ] **4. Vert deux cibles/commit.** `./gradlew :suite-browser:jsBrowserTest :suite-browser:wasmJsBrowserTest`, puis `git commit -m "feat: add bounded city orbit controls"`.

### Task 5: Interface EN/FR, interactions DOM et cycle de vie

**Files:** `CityPage.kt`, `CityView.kt`, `CityPointerDom.kt`, deux JSON, `Main.kt`, `index.html`, test navigateur ville.
**Consumes:** scène tâche3, CityControls tâche4, helpers DomElement/AbortController/fetchText existants.
**Produces:** `suspend fun showCityPage(locale: String)` ; page prête `[data-ready="true"]`, IDs ci-dessous.

- [ ] **1. Tests navigateur rouges avant le DOM.** IDs : `city-canvas`, `city-density`,
`city-height`, `city-rotation` (checkbox), `city-distance`, `city-reset-view`,
`city-status`, `city-reload`, `city-lesson`. Page test instrumente seulement GPUDevice.destroy,
RAF handles et getCurrentTexture ; ne pas exposer ces compteurs dans la production.
Exemple comportement de base :

```javascript
test('city controls redraw without replacing device and reset only the camera', async () => {
  const page = await server.browser.newPage({reducedMotion:'reduce'});
  try {
    await page.goto(`${server.origin}/?demo=instanced-city&lang=fr`);
    assert.equal(await page.locator('#city-canvas').count(), 1);
    await page.locator('[data-ready="true"]').waitFor();
    assert.equal(await page.locator('#city-rotation').isChecked(), false);
    const initial = await page.locator('#city-canvas').screenshot();
    await page.locator('#city-height').fill('2');
    await page.locator('#city-distance').fill('100');
    await page.locator('#city-reset-view').click();
    assert.equal(await page.locator('#city-height').inputValue(), '2');
    assert.equal(await page.locator('#city-distance').inputValue(), '180');
    assert.notDeepEqual(await page.locator('#city-canvas').screenshot(), initial);
  } finally { await page.close(); }
});
```

Pour range Playwright, utiliser `.evaluate(e=>{e.value='2';e.dispatchEvent(new Event('input'))})`
si fill n'est pas supporté par cette version ; attendre deux RAF test-only avant capture.
Le défaut rotation neutralisé pour captures, pas via changement production.

Écrire les scénarios suivants individuellement avec assertions visibles :
- Densité4096→256→4096 avec hauteur1 et rotation off : captures4096 reproduites, device identique.
- Camera drag souris puis reset : capture modifiée puis initiale ; vrai hasPointerCapture,
  second id synthétique ignoré, cancel libère, move après cancel ne change rien.
- Vrai touchscreen sur contexte hasTouch, events PointerEvent de type touch pour drag,
  distance slider accessible, aucune erreur ; viewport mobile seul insuffisant.
- Molette vraie et deltaMode synthétique0/1/2 : distance change/bornée, scroll page non neutralisé hors canvas.
- Canvas display:none, visibility:hidden, transform:scale(0), remove/reinsert : compteur
  getCurrentTexture stable, caméra ne saute pas, dernière hauteur/distance appliquée à reprise.
- Device détruit : zéro RAF, contrôles GPU disabled, reload visible, navigation disponible.
- Textes retardés + pagehide : relâcher réponse, aucun canvas tardif/device/RAF.
- requestDevice retardé + pagehide : late device détruit exactement une fois, page jamais prête.
- Création de scène candidate en erreur sur changement densité : injecter un échec au bon
  boundary createBuffer après préparation initiale ; ancienne capture/contrôles conservés,
  candidate resources détruites, message d'erreur visible sans arrêter l'ancienne scène.

Faire échouer d'abord `node --test --test-name-pattern='city controls' tools/instanced-city.browser.test.mjs`
sur absence de canvas, puis activer chaque nouveau scénario avant son implémentation.

Pour un vrai glisser tactile Chromium, envoyer les événements via CDP (ils produisent les
PointerEvents touch du navigateur, contrairement à dispatchEvent d'un objet inventé) :

```javascript
const context = await server.browser.newContext({hasTouch:true,
  reducedMotion:'reduce', viewport:{width:390,height:844}});
const page = await context.newPage();
await page.goto(`${server.origin}/?demo=instanced-city&lang=fr`);
await page.locator('[data-ready="true"]').waitFor();
const canvas = page.locator('#city-canvas');
const box = await canvas.boundingBox();
const before = await canvas.screenshot();
const cdp = await context.newCDPSession(page);
const x = box.x + box.width/2, y = box.y + box.height/2;
await cdp.send('Input.dispatchTouchEvent', {type:'touchStart',touchPoints:[{x,y,id:1}]});
await cdp.send('Input.dispatchTouchEvent', {type:'touchMove',touchPoints:[{x:x+50,y:y+20,id:1}]});
await cdp.send('Input.dispatchTouchEvent', {type:'touchEnd',touchPoints:[]});
await page.evaluate(() => new Promise(ok => requestAnimationFrame(() => requestAnimationFrame(ok))));
assert.notDeepEqual(await canvas.screenshot(), before);
await context.close(); // put this cleanup in finally in the test
```

- [ ] **2. Frontière DOM typée.** `CityPointerDom.kt` utilise kotlin.js.JsAny/external interfaces,
pas dynamic. Pointer id/client/button + pointer capture rect, wheel deltaY/deltaMode/preventDefault,
checkbox.checked, document visibility et matchMedia réduite. Wheel listener non passive :

```kotlin
private fun cityWheelOptions(controller: AbortController): JsAny =
    js("({signal: controller.signal, passive: false})")
internal fun cityReducedMotion(): Boolean =
    js("globalThis.matchMedia('(prefers-reduced-motion: reduce)').matches")
```

Canvas visible : checkVisibility({visibilityProperty:true}) ET isConnected ET rect width/height>0
ET document.visibilityState!='hidden'. `visibilitychange` resetClock et requestFrame à reprise.
Ne pas modifier le helper réaction-diffusion pour cette nouvelle exigence locale.

- [ ] **3. Bootstrap et gestion GPU.** Établir garde pagehide avant coroutine lazy fetch,
comme ReactionDiffusionPage lignes33–54 ; closed/cancelled après chaque suspension, late
adapter/device fermé. Page start installe events avant requestAdapter. Device errors scope
awaitLost MainScope dédié, cancellation non convertie en message d'erreur. Init : densités
permises, default last, surface configure, scope validation avant accepter scène initiale.

Remplacement densité dans un job suspend unique ; désactiver uniquement densité pendant job,
éviter deux candidats concurrents. Capturer selected density, créer données/scène candidate
dans scope validation, attendre pop ; en cas d'échec close candidate, sélectionner ancienne
densité, afficher status, conserver ready/RAF. Si closed/failed au retour, close candidate
sans toucher DOM. Publier candidate avant close ancienne, changer controls.density et gridStep.

- [ ] **4. Loop et release.** Une seule frameHandle nullable ; preview sans rotation pendant
initialisation ; la caméra advance seulement après checks visibilité/dimensions. Clamp taille
physique au maxTextureDimension2D ; cube et profondeur utilisent cette taille et ce ratio.
Frame : currentSurface view → encoder → scene.encodeFrame → finish → submit → closes.
Schedule continue seulement si rotating et !closed/failed ; gestes/resize demandent une frame
même si rotation off. resetClock sur hidden/reprise/toggle/capture. Capture abort dans fail/close,
scene/surface/device/adapter libérés chacun malgré une erreur de close, diagnostic non perdu.

- [ ] **5. Vue et ressources complètes.** `@Serializable internal data class CityTexts` champs
title,description,gesture,density,height,rotation,distance,resetView,loading,unavailable,failed,
lost,reload,source,lesson,instancingHelp,depthHelp,proceduralHelp,drawHelp,buildingShader,groundShader.
Traductions sans texte anglais hardcodé hors diagnostic fallback de fetch. Défauts exemple :

```json
{"title":"Ville cyberpunk","description":"Des milliers de bâtiments partagent un cube indexé.",
 "gesture":"Glissez pour tourner, utilisez la molette ou le curseur de distance pour zoomer.",
 "density":"Bâtiments","height":"Hauteur","rotation":"Rotation automatique",
 "distance":"Distance","resetView":"Réinitialiser la vue"}
```

Ce bloc fixe les libellés ; les autres champs sont renseignés avec les explications décrites
dans la spec, dont « deux dessins : sol et bâtiments » et 12*count+2 triangles soumis.
Chaque contrôle label.for=id ; status role=status/data-error, reload bouton. View ne possède
aucune ressource GPU. Panneau `<details>` fermé, `<pre><code>` textContent des constantes shader,
aucun innerHTML. Liens sources dossier instancedcity. CSS `.city-demo canvas {aspect-ratio:16/10;
touch-action:none}` ; pre overflow-x:auto et details min-width0. Main route non-verify appelle
showDemoNavigation puis showCityPage.

- [ ] **6. Vert deux distributions, commit.** Rebuild distribution JS/Wasm puis suites navigateur
ville entières et réaction-diffusion. Test galerie reste vert (ville ajoutée tâche6).
`git commit -m "feat: add bilingual interactive cyberpunk city page"`.

### Task 6: Troisième démo dans navigation, galerie et rapports stricts

**Files:** DemoNavigation.kt, site/demos/app.js, tools/demo-reports.mjs/test, galerie test,
test navigateur réaction-diffusion et ville.
**Consumes:** route/code/ressources tâche5, trois CityCheckIds.
**Produces:** huit ids, trois choix et cartes, même schéma/fichiers de rapports.

- [ ] **1. Tests rouges des outils et navigation.** Ajouter trois noms à l'attendu des rapports,
pas réduire les exigences précédentes ; fixtures report intégrales par route. Assert littéral :

```javascript
assert.deepEqual(aggregateDemoReports(DEMO_ROUTES.map(good)).cases.map(c=>c.id), [
 'particles.compute-render-readback','particles.bounds-pause-reset',
 'reaction-diffusion.compute-render-readback','reaction-diffusion.pause-step-reset',
 'reaction-diffusion.brush-boundaries','instanced-city.instances-render-readback',
 'instanced-city.depth-occlusion','instanced-city.camera-resize-lifecycle']);
```

Modifier fixtures HTTP existantes pour répondre aux trois routes. Tests négatifs city troisième
route : rapport absent, JSON invalide, mauvais commit, duplicate id, failed, null schema/falsy
fatal ; collecteur doit garder cas des deux autres et fatal imbriqué/enveloppe. Test CLI
temp fixture répond valid aux deux premières, invalid schema à ville ; exit1 et diagnostic ville.
Ne jamais mettre ces fixtures dans build/reports du checkout réel.

Navigation test browser : réaction→ville→particules→réaction pour EN/FR ; combobox accessible,
valeur courante à chaque arrêt, path/lang inchangés, un seul canvas ; lancements directs des
trois routes. Démo vérification/benchmark/unknown ne montre pas ce sélecteur interactif.
Galerie : trois articles, six launch liens JS/Wasm, neuf sources principales, erreur isolée
si dict ville404, autres cartes intactes, textes hostiles rendus textContent. Sources ville
liens `InstancedCityScene.kt`, `CityShaders.kt`, `CityData.kt`, ne pas compter caméra/géométrie
comme nouveaux liens tant que le test affirme neuf liens.

- [ ] **2. Rouge puis implémentation minimale.** `node --test tools/demo-reports.test.mjs tools/demos-gallery.browser.test.mjs`
doit échouer avant production. Ajouter :

```javascript
// DEMO_ROUTES entry
{name:'instanced-city',ids:['instanced-city.instances-render-readback',
 'instanced-city.depth-occlusion','instanced-city.camera-resize-lifecycle']}
// gallery DEMOS entry
{name:'instanced-city',directory:'instancedcity',
 sources:['InstancedCityScene.kt','CityShaders.kt','CityData.kt']}
```

Selector list ajoute `"instanced-city" to if(locale=="fr") "Ville cyberpunk" else "Cyberpunk city"`.
Ne pas créer un registre/framework commun pour seulement trois constantes ; zéro changement
schéma DemoReport ni artefact collection. run-browser expected ids déjà derive de DEMO_ROUTES,
ne modifier ce script que si une incompatibilité réelle est testée.

- [ ] **3. Vert tous outils, navigation et collectors.** Rebuild JS/Wasm ; tests Node galerie/rapports,
browser ville/réaction dans chaque distribution ; collectors GPU séquentiels :

```sh
node tools/run-browser.mjs js suite-browser/build/dist/js/productionExecutable --demo-check
node tools/run-browser.mjs wasm suite-browser/build/dist/wasmJs/productionExecutable --demo-check
```

Rapports huit ids uniques tous passed, no fatal/page errors ; attribution identique au baseline.
`git commit -m "feat: integrate city into demo navigation and reports"`.

### Task 7: Campagne complète, visuel et documentation

**Files:** README.md, docs/running.md, docs/verification.md, docs/verification.fr.md, CHANGELOG.md,
suivi spec/plan. Pas de sources générées ni screenshots obligatoires en Git.
**Consumes:** les trois scènes et leur collecte huit cas.
**Produces:** instructions reproductibles, preuves réelles et branche prête à review.

- [ ] **1. Documentation usage avant campagne.** README suite-demos cite trois scènes.
running : routes ville/lang/verify, geste et curseur tactile (pas de pinch), rotation/reduced motion,
reset vue indépendant des autres réglages, densités limit-filtered, hauteur. Énoncer deux dessins
et absence de bloom/compute, règles d'encodage/propriété natives sans prétendre un run GPU natif.
Changelog Unreleased nouveau feat ville. Ne jamais écraser les preuves historiques par des
résultats pas encore exécutés ; append une section après la campagne.

- [ ] **2. Exécuter suite et platforms.** Commandes distinctes avec stdout conservé dans ledger :

```sh
./gradlew check :suite-demos:compileKotlinJvm :suite-demos:compileKotlinMacosArm64 :suite-browser:jsBrowserDistribution :suite-browser:wasmJsBrowserDistribution
node --test tools/arraybuffer-report.test.mjs tools/compare-arraybuffer-benchmarks.test.mjs tools/demo-reports.test.mjs tools/demos-gallery.browser.test.mjs
node --test tools/instanced-city.browser.test.mjs tools/reaction-diffusion.browser.test.mjs
GRAPHIKS_DISTRIBUTION=suite-browser/build/dist/wasmJs/productionExecutable node --test tools/instanced-city.browser.test.mjs tools/reaction-diffusion.browser.test.mjs
node tools/run-browser.mjs js suite-browser/build/dist/js/productionExecutable --demo-check
node tools/run-browser.mjs wasm suite-browser/build/dist/wasmJs/productionExecutable --demo-check
node tools/run-browser.mjs js suite-browser/build/dist/js/productionExecutable
node tools/run-browser.mjs wasm suite-browser/build/dist/wasmJs/productionExecutable
node tools/build-site.mjs
```

GPU JS/Wasm collectors séquentiels. Native compile = pas native GPU execution. Ne pas relancer
benchmarks pour cette démo sans demande. Garder runtime Node réel dans les preuves ; CI22
ne devient pas local22 par affirmation. Report identity correspond au commit réellement compilé.

- [ ] **3. Documentation contributive.** Environnement local venv déjà possible, vérifier :

```sh
python3 -m venv build/docs-venv
build/docs-venv/bin/python -m pip install mkdocs-material mkdocs-static-i18n
./gradlew :docs:embedDokkaIntoMkDocs
build/docs-venv/bin/mkdocs build --strict -f docs/mkdocs.yml
```

- [ ] **4. Inspection locale réelle.** Servir build/site sur localhost via serveur existant ou
python http.server, ouvrir `/run/js/?demo=instanced-city&lang=fr` dans OpenChamber, snapshot puis
inspect/capture desktop et mobile. Interagir souris/toucher avec tests Playwright réels, pas
inventer un résultat du navigateur. Captures avec rotation désactivée pour les trois densités,
height .5/2, vue de dessus/oblique. Panneau ouvert sans horizontal overflow, source WGSL complète.
Si défaut visuel/cycle de vie : test rouge avant fix, puis rerun les cibles concernées.

- [ ] **5. Lire les artefacts avant les preuves/commit.** JSON envelope buildCommit==baseline.commit,
buildVersion==baseline.suiteVersion ; cases exactly8 unique passed ; fatal/pageErrors absents.
Read test XML counts/failures et résumés commandes, relever optional unsupported acid réellement
observés, pas présumer le nombre actuel152 invariant. Append vérification EN/FR date/env/commandes
et limites (GPU logiciel vs physique, hôtes non testés) ; `git diff --check` et status avant stage.
`git commit -m "docs: document cyberpunk city usage and verification"`.

- [ ] **6. Review finale et handoff.** Une revue indépendante whole-branch si exécution native,
ou workflow choisi par l'utilisateur si subagents. Fournir spec/plan, base `203f040`, HEAD,
diff et rulings pour exclure les travaux précédents ; vérifier les cinq Review Focus.
Tester/fixer tous constats importants et re-vérifier avant completion. Ne pas push/PR/merge
sans décision de livraison ; si demandée, suivre CONTRIBUTING et lier l'issue/PR à session.
Conserver le serveur local de démo quand demandé et donner son URL réelle.

## Auto-relecture et approbation

- [x] Exigences de la spec reliées aux tâches1–7, y compris reduced motion, profondeur et trois routes.
- [x] Interfaces tâches1/2→3→5 et4→5 cohérentes ; route `instanced-city`, package `instancedcity` intentionnel.
- [x] Cinq Review Focus ont leurs cas CPU/GPU/browser dans les tâches propriétaires.
- [x] Pas de nouvelle dépendance, prototype ni code de production écrit avec ce plan.
- [x] Branche distincte et dépendance PR #141 documentées ; publication non présumée.
- [ ] Plan relu et approuvé par l'utilisateur.
- [ ] Méthode d'exécution confirmée par l'utilisateur.
