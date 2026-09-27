# Graphiks WebGPU Browser — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking. L’utilisateur confiera l’exécution à un autre agent, dans un autre worktree. Ne pas exécuter ce plan dans le worktree de brainstorming.

**Goal:** Publier une implémentation navigateur autonome et fidèle à l’API Graphiks WebGPU pour Kotlin/JS et Kotlin/Wasm JS.

**Architecture:** Séparer le contrat portable, les descripteurs, l’interop WebGPU et l’implémentation navigateur en quatre modules. Adapter le binding existant au contrat actuel, corriger ses conversions et vérifier son fonctionnement depuis les projets puis depuis les artefacts Maven.

**Tech Stack:** Kotlin Multiplatform 2.4.20, JDK 25, Gradle wrapper du dépôt, Kotlin wrappers 2026.9.2, coroutines 1.11.0, kotlin.test, Karma/Chromium, plugin Maven Publish 0.36.0.

**Spec:** `docs/superpowers/specs/2026-09-27-webgpu-browser-design.md` — conception approuvée, avec précisions techniques de l’inventaire.

## Global Constraints

- `org.graphiks:webgpu-api` — `org.graphiks.webgpu` — Contrats portables.
- `org.graphiks:webgpu-descriptors` — `org.graphiks.webgpu.descriptors` — Implémentations des descripteurs.
- `org.graphiks:webgpu-web-bindings` — `org.graphiks.webgpu.bindings` — Types JavaScript et interop JS/Wasm.
- `org.graphiks:webgpu-browser` — `org.graphiks.webgpu.browser` — Implémentation navigateur.
- `webgpu-web` devient `webgpu-web-bindings`.
- L’implémentation navigateur conserve l’architecture des descripteurs génériques.
- Les défauts de fidélité au contrat sont corrigés pendant l’intégration.
- Le dépôt externe reste hors du périmètre des modifications.
- La documentation décrit les modules et leur utilisation, sans historique de migration.
- Les notices de licence applicables sont conservées.
- La version provient de `releaseVersion`, avec `0.1.0-SNAPSHOT` comme valeur par défaut.
- Préfixer les commandes shell par `rtk` dans l’environnement de l’utilisateur ; `rtk proxy` permet de lancer les commandes non prises en charge directement. Les workflows GitHub utilisent les commandes natives, sans dépendance à RTK.
- Le présent worktree reste réservé aux documents. L’exécution et ses commits de code appartiennent au worktree de l’autre agent.

## Review Focus

1. Un navigateur absent, un adapter nul, une Promise rejetée ou une coroutine annulée doivent avoir des issues distinctes et conformes au contrat — tests tâche 4.
2. Les valeurs nullable et les paramètres omis ne sont pas interchangeables : unbind, tailles omises et index de timestamp égal à zéro — tests tâches 2, 5 et 6.
3. Les records doivent être des propriétés JavaScript ; une mutation des constantes entre deux appels doit être visible sans cache périmé — tests tâches 2 et 5.
4. Les nouveautés de l’API d’accueil doivent traverser le binding : unions d’attachments, swizzle, usages des vues, immediates et limites par stage — tests tâches 3, 5 et 6.
5. Les ressources canvas appartiennent au navigateur et les artefacts Maven doivent réellement suffire à un consommateur JS/Wasm — tests tâches 7 et 8.

---

## Mode d’emploi et état de départ

Lire la spec et ce plan intégralement avant de modifier le code. Exécuter les tâches dans l’ordre ; chacune se termine par ses vérifications et un commit ciblé. Ne pas lancer une publication distante pour vérifier le plan : la validation utilise un dépôt Maven local isolé, et les workflows configurent la publication effective.

Le dépôt d’accueil a été examiné au commit `48db776d11b6f1fed844096593b92a7c425f264b`. Le code à importer est disponible à `https://github.com/wgpu4k/wgpu4k.git`. L’inventaire correspond au commit `9a35a17effbfbe33ddc436052664ef8651d3fc52`, disponible lors de la conception sous `/private/tmp/graphiks-wgpu4k-review`. Ces informations sont des entrées techniques pour l’exécutant, pas un historique à ajouter aux guides publics. Si le checkout n’existe plus, le recréer dans le répertoire temporaire approuvé par le harness ; conserver le dépôt source en lecture seule.

Les exemples de code ci-dessous sont des instructions d’implémentation, non des résultats de compilation. Les vérifications doivent être exécutées dans le worktree d’implémentation. Si une commande échoue avant d’atteindre le comportement testé, diagnostiquer ce problème avant de compter un test rouge comme une régression démontrée.

### Inventaire et responsabilité des fichiers

| Zone | Changements |
| --- | --- |
| `settings.gradle.kts` | Renommer le module de bindings, inclure le module navigateur |
| `webgpu-descriptors/src/commonMain/kotlin/descriptor.kt` | Package des descripteurs, imports du contrat |
| `webgpu-web/` → `webgpu-web-bindings/` | Déplacement du module et packages de toutes ses sources/tests |
| `build-logic/.../tasks/ModelWriter.kt` | Chemins, packages/imports distincts, racine injectable pour tests |
| `build-logic/.../mapper/Idl.kt`, `WebInterface.kt` | Records et nullabilité des bindings |
| `webgpu-web-bindings/src/commonMain/kotlin/WebGpuRecord.kt` | Représentation des records WebIDL |
| `webgpu-browser/src/commonMain/kotlin/` | Wrappers, acquisition adapter, surfaces et auxiliaires |
| `webgpu-browser/src/commonMain/kotlin/mapper/` | Conversions internes |
| `webgpu-browser/src/jsMain/kotlin/`, `src/wasmJsMain/kotlin/` | Callbacks et discrimination des erreurs spécifiques à la cible |
| `webgpu-browser/src/commonTest/kotlin/` | Tests à objets JS contrôlés, sans GPU |
| `integration-tests/browser/` | Build autonome de tests GPU, également consommateur des artefacts Maven |
| `.github/workflows/test.yml`, `publish.yml` | Tests navigateur réels et quatre publications |
| `buildSrc/src/main/kotlin/publish.gradle.kts` | Descriptions POM adaptées |
| `docs/build.gradle.kts`, `docs/mkdocs.yml`, guides EN/FR et `README.md` | Référence des quatre modules et parcours navigateur |

Les chemins `build-logic/...` du tableau désignent `build-logic/src/main/kotlin/org/graphiks/webgpu/generator/`. Dans les tâches, ils sont écrits intégralement.

### Préparation dans le worktree d’exécution

- [ ] Lire les instructions locales et vérifier `rtk git status --short`, `rtk git rev-parse HEAD` et `rtk proxy java -version`.
- [ ] Vérifier les deux documents de conception dans le worktree d’exécution ; les transférer depuis le worktree de brainstorming s’ils ne sont pas encore présents sur la branche de départ.
- [ ] Vérifier le checkout source avec `rtk git -C /private/tmp/graphiks-wgpu4k-review rev-parse HEAD`. En cas d’absence, utiliser `rtk git clone https://github.com/wgpu4k/wgpu4k.git "$SOURCE"`, puis `rtk git -C "$SOURCE" checkout --detach 9a35a17effbfbe33ddc436052664ef8651d3fc52`, où `SOURCE` est un chemin temporaire du harness.
- [ ] Exécuter la baseline pertinente : `rtk proxy ./gradlew -p build-logic test` puis `rtk proxy ./gradlew :webgpu-api:jsNodeTest :webgpu-api:wasmJsNodeTest :webgpu-web:jsNodeTest :webgpu-web:wasmJsNodeTest`. Consigner toute panne préexistante avant les modifications.

## Task 1: Séparer les namespaces et renommer les bindings durablement

**Files:**
- Modify: `settings.gradle.kts`
- Modify: `build-logic/src/main/kotlin/org/graphiks/webgpu/generator/tasks/ModelWriter.kt`
- Modify: `build-logic/src/main/kotlin/org/graphiks/webgpu/generator/GenerateBindingTask.kt`
- Move: `webgpu-web/` → `webgpu-web-bindings/`
- Modify: `webgpu-descriptors/src/commonMain/kotlin/descriptor.kt`
- Modify: `webgpu-descriptors/build.gradle.kts`, `webgpu-web-bindings/build.gradle.kts`
- Modify: `docs/build.gradle.kts`, `.github/workflows/test.yml`, `.github/workflows/publish.yml` (références de module nécessaires dès cette tâche)
- Create: `build-logic/src/test/kotlin/org/graphiks/webgpu/generator/tasks/ModelWriterTest.kt`
- Create: `build-logic/src/test/kotlin/org/graphiks/webgpu/generator/TestContexts.kt`

**Interfaces:**
- Consumes: `MapperContext`, `ModelWriter.write(context)`.
- Produces: `ModelWriter.write(context: MapperContext, root: Path = Paths.get("."))`; mêmes noms de classes publiques, packages séparés.

- [ ] **Step 1: Ajouter un test de génération dans un répertoire temporaire.** Fournir ce helper partagé dans `TestContexts.kt` :

```kotlin
package org.graphiks.webgpu.generator

import de.fabmax.webidl.parser.WebIdlParser
import org.graphiks.webgpu.generator.domain.MapperContext
import org.graphiks.webgpu.generator.domain.YamlModel

internal fun testContext(idl: String) = MapperContext(
    WebIdlParser.parseFromInputStream(idl.byteInputStream()),
    YamlModel(
        copyright = "", name = "", enum_prefix = "",
        constants = emptyList(), typedefs = emptyList(), bitflags = emptyList(),
        structs = emptyList(), functions = emptyList(), objects = emptyList(), enums = emptyList(),
    ),
)
```

Créer ensuite ce test dans `ModelWriterTest` :

```kotlin
val idl = """
    interface GPUBuffer {};
    dictionary GPUBufferDescriptor { required unsigned long size; };
""".trimIndent()
val context = testContext(idl).apply {
    loadInterfaces()
    loadDictionaries()
    loadDescriptors()
    loadWebInterfaces()
}
val root = Files.createTempDirectory("webgpu-writer-test")
try {
    ModelWriter.write(context, root)
    val descriptors = root.resolve("webgpu-descriptors/src/commonMain/kotlin/descriptor.kt").readText()
    val bindings = root.resolve("webgpu-web-bindings/src/commonMain/kotlin/types.kt").readText()
    val api = root.resolve("webgpu-api/src/commonMain/kotlin/interfaces.kt").readText()
    assertTrue(descriptors.contains("package org.graphiks.webgpu.descriptors"))
    assertTrue(descriptors.contains("import org.graphiks.webgpu.*"))
    assertTrue(bindings.contains("package org.graphiks.webgpu.bindings"))
    assertTrue(api.contains("package org.graphiks.webgpu\n"))
    assertFalse(root.resolve("webgpu-web").toFile().exists())
    ModelWriter.write(context, root)
    assertEquals(descriptors, root.resolve("webgpu-descriptors/src/commonMain/kotlin/descriptor.kt").readText())
} finally {
    root.toFile().deleteRecursively()
}
```

Imports du test : `java.nio.file.Files`, `kotlin.io.path.readText`, `kotlin.test.*`, `org.graphiks.webgpu.generator.testContext` et les quatre fonctions du package `.generator.mapper`. Encapsuler le corps dans une méthode annotée `@Test`.

- [ ] **Step 2: Exécuter le test rouge.** `rtk proxy ./gradlew -p build-logic test --tests '*ModelWriterTest'`. La première erreur attendue est l’absence du paramètre `root`; après ajout de la signature, les assertions de chemins/packages doivent montrer l’ancien comportement.
- [ ] **Step 3: Adapter le writer.** Construire ses cinq chemins à partir de `root` dans `write`. Rendre le package et les imports explicites dans les helpers :

```kotlin
private fun Path.createSourceFile(
    fileName: String,
    packageName: String = "org.graphiks.webgpu",
    imports: List<String> = emptyList(),
    block: File.() -> Unit,
) {
    createDirectories()
    resolve(fileName).toFile().apply {
        writeText("@file:Suppress(\"unused\")\n// This file has been generated DO NO EDIT\n")
        appendText("package $packageName\n\n")
        imports.forEach { appendText("import $it\n") }
        appendText("\n")
        block()
    }
}
```

Appeler ce helper avec `org.graphiks.webgpu.descriptors` et `listOf("org.graphiks.webgpu.*")` pour les descripteurs. Le helper web écrit `org.graphiks.webgpu.bindings` et conserve les annotations/imports JS nécessaires, avec un saut de ligne après l’annotation `OptIn`. `GenerateBindingTask` appelle `ModelWriter.write(context, project.projectDir.toPath())`.

- [ ] **Step 4: Déplacer le module et ses packages.** Utiliser l’outil de patch du harness ou `rtk git mv webgpu-web webgpu-web-bindings`. Modifier les packages dans ses sources common/JS/Wasm et ses tests. Mettre les références Gradle/Dokka/workflows sur le nouveau nom. Dans les deux modules dont des signatures exposent les types communs, utiliser `api(project(":webgpu-api"))` plutôt que `implementation`.
- [ ] **Step 5: Régénérer avec les ressources versionnées.** `rtk proxy ./gradlew generate-binding`. Examiner le diff pour vérifier que le contrat commun n’a pas été modifié involontairement et que les imports des valeurs par défaut des descripteurs compilent dans leur nouveau package. Aucune actualisation distante de la spécification n’est nécessaire.
- [ ] **Step 6: Vérifier puis committer.**

```sh
rtk proxy ./gradlew -p build-logic test
rtk proxy ./gradlew :webgpu-descriptors:compileKotlinJs :webgpu-descriptors:compileKotlinWasmJs :webgpu-web-bindings:jsNodeTest :webgpu-web-bindings:wasmJsNodeTest
rtk git diff --check
rtk git add settings.gradle.kts build-logic webgpu-web webgpu-web-bindings webgpu-descriptors docs/build.gradle.kts .github/workflows/test.yml .github/workflows/publish.yml
rtk git commit -m "refactor: separate WebGPU descriptor and binding namespaces"
```

Si le répertoire supprimé ne peut plus être nommé dans `git add`, utiliser `git add -u` pour sa suppression, en vérifiant le diff indexé pour exclure le travail étranger à la tâche.

## Task 2: Corriger les records et la nullabilité à la frontière JavaScript

**Files:**
- Modify: `build-logic/src/main/kotlin/org/graphiks/webgpu/generator/mapper/Idl.kt`
- Modify: `build-logic/src/main/kotlin/org/graphiks/webgpu/generator/mapper/WebInterface.kt`
- Modify: `build-logic/src/main/kotlin/org/graphiks/webgpu/generator/tasks/ModelWriter.kt`
- Create: `build-logic/src/test/kotlin/org/graphiks/webgpu/generator/mapper/WebInteropMappingTest.kt`
- Create: `webgpu-web-bindings/src/commonMain/kotlin/WebGpuRecord.kt`
- Create: `webgpu-web-bindings/src/commonTest/kotlin/WebGpuRecordTest.kt`
- Regenerate: `webgpu-web-bindings/src/commonMain/kotlin/types.kt`

**Interfaces:**
- Produces: `external interface WebGpuRecord : JsAny`, `createWebGpuRecord(): WebGpuRecord`, `setRecordValue(record: WebGpuRecord, key: String, value: JsAny): Unit` dans `org.graphiks.webgpu.bindings`.
- Produces: arguments nullable des bindings `setBindGroup`/`setVertexBuffer`, résultats nullable des Promises correspondantes à `requestAdapter`/`popErrorScope`.

- [ ] **Step 1: Tester la génération d’une IDL minimale.** Utiliser le helper `testContext` de la tâche 1, avec `val context = testContext(idl).apply { loadWebInterfaces() }`, sur les déclarations suivantes :

```webidl
interface GPUBindGroup {};
interface GPUError {};
interface GPUFixture {
    undefined setBindGroup(unsigned long index, GPUBindGroup? group);
    Promise<GPUError?> popErrorScope();
};
dictionary GPUFixtureDescriptor {
    record<USVString, double> constants;
};
```

Assertions Kotlin sur `context.webInterfaces` :

```kotlin
val fixture = context.webInterfaces.single { it.name == "WGPUFixture" }
val group = fixture.methods.single { it.name == "setBindGroup" }.parameters.last()
assertTrue(group.type.substringBefore("/*").trim().endsWith("?"))
assertTrue(fixture.methods.single { it.name == "popErrorScope" }.returnType.contains("JsAny?"))
val descriptor = context.webInterfaces.single { it.name == "WGPUFixtureDescriptor" }
assertTrue(descriptor.attributes.single { it.name == "constants" }.type.startsWith("WebGpuRecord"))
```

- [ ] **Step 2: Vérifier l’échec**, avec `rtk proxy ./gradlew -p build-logic test --tests '*WebInteropMappingTest'`.
- [ ] **Step 3: Fournir un record JavaScript à propriétés propres.**

```kotlin
@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
package org.graphiks.webgpu.bindings

import kotlin.js.JsAny
import kotlin.js.js

external interface WebGpuRecord : JsAny

fun createWebGpuRecord(): WebGpuRecord = js("Object.create(null)")

fun setRecordValue(record: WebGpuRecord, key: String, value: JsAny): Unit =
    js("{ record[key] = value; }")
```

Tester dans les deux cibles :

```kotlin
private fun readNumber(record: WebGpuRecord, key: String): Double = js("record[key]")
private fun ownCount(record: WebGpuRecord): Int = js("Object.keys(record).length")
private fun isMap(record: WebGpuRecord): Boolean = js("record instanceof Map")

@Test
fun recordContainsEnumerableOwnProperties() {
    val record = createWebGpuRecord()
    setRecordValue(record, "scale", 7.0.asJsNumber())
    setRecordValue(record, "__proto__", 9.0.asJsNumber())
    assertEquals(7.0, readNumber(record, "scale"))
    assertEquals(9.0, readNumber(record, "__proto__"))
    assertEquals(2, ownCount(record))
    assertFalse(isMap(record))
}
```

- [ ] **Step 4: Modifier le mapping généré.** Dans `Idl.kt`, le cas `record` retourne `WebGpuRecord` en conservant le commentaire IDL. Détecter le suffixe `?` de `IdlSimpleType.typeName` avant sa conversion, convertir le nom sans suffixe puis réappliquer la nullabilité avant le commentaire. Pour `Promise`, inspecter le premier paramètre et produire `Promise<JsAny?>` si son type est nullable, sinon conserver `Promise<JsAny>`. `convertType` doit résoudre `GPUBuffer?` en `WGPUBuffer?`, pas en `JsAny` non nullable. Retirer l’import `JsMap` du writer s’il n’est plus utilisé.
- [ ] **Step 5: Régénérer et vérifier.**

```sh
rtk proxy ./gradlew -p build-logic test
rtk proxy ./gradlew generate-binding
rtk proxy ./gradlew :webgpu-web-bindings:jsNodeTest :webgpu-web-bindings:wasmJsNodeTest
rtk git diff --check
rtk git add build-logic webgpu-web-bindings
rtk git commit -m "fix: preserve WebIDL records and nullable browser values"
```

## Task 3: Importer un module navigateur compilable contre le contrat actuel

**Files:**
- Modify: `settings.gradle.kts`
- Create: `webgpu-browser/build.gradle.kts`
- Create: `webgpu-browser/src/commonMain/kotlin/{Adapter,AdapterInfo,BindGroup,BindGroupLayout,Buffer,CommandBuffer,CommandEncoder,ComputePassEncoder,ComputePipeline,Device,GPUError,Limits,PipelineLayout,QuerySet,Queue,RenderBundle,RenderBundleEncoder,RenderPassEncoder,RenderPipeline,Sampler,ShaderModule,Texture,TextureView}.kt`
- Create: `webgpu-browser/src/commonMain/kotlin/mapper/` avec les conversions listées ci-dessous
- Create: `webgpu-browser/src/jsMain/kotlin/Device.js.kt`, `mapper/GPUError.js.kt`
- Create: `webgpu-browser/src/wasmJsMain/kotlin/Device.wasmJs.kt`, `mapper/GPUError.wasmJs.kt`
- Create: `webgpu-browser/src/commonTest/kotlin/WrapperSmokeTest.kt`

**Interfaces:**
- Consumes: toutes les interfaces publiques de `webgpu-api`, tous les `WGPU*` et helpers de `webgpu-web-bindings`.
- Produces: classes wrappers `X(val handler: WGPUX)` implémentant `GPUX` ; `Device(handler: WGPUDevice, onUncapturedError: GPUUncapturedErrorCallback? = null)` ; `Texture(handler: WGPUTexture, canBeDestroy: Boolean = true)`.
- Produces: `suspend fun requestAdapter(options: GPURequestAdapterOptions? = null): Result<Adapter>`.

- [ ] **Step 1: Créer le module et un test de wrapper.**

```kotlin
plugins {
    kmp
    publish
}

kotlin {
    js {
        browser { testTask { useKarma { useChromeHeadless() } } }
        nodejs()
    }
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        browser { testTask { useKarma { useChromeHeadless() } } }
        nodejs()
    }
    compilerOptions {
        allWarningsAsErrors = true
        freeCompilerArgs.add("-Xexpect-actual-classes")
        optIn.add("kotlin.js.ExperimentalWasmJsInterop")
        optIn.add("kotlin.ExperimentalUnsignedTypes")
    }
    sourceSets {
        commonMain.dependencies {
            api(project(":webgpu-api"))
            api(project(":webgpu-web-bindings"))
            api(kotlinWrappers.browser)
            api(kotlinWrappers.web)
            implementation(libs.coroutines)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(project(":webgpu-descriptors"))
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
        }
    }
}

java { toolchain { languageVersion.set(JavaLanguageVersion.of(25)) } }
```

Ajouter `include("webgpu-browser")`. Le test, dans `org.graphiks.webgpu.browser`, utilise des bindings fictifs qui n’accèdent pas au GPU :

```kotlin
@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
package org.graphiks.webgpu.browser

import org.graphiks.webgpu.bindings.WGPUBuffer
import kotlin.js.js
import kotlin.test.Test
import kotlin.test.assertEquals

private fun fakeBuffer(): WGPUBuffer = js("({ label: 'before', size: 16, usage: 8, mapState: 'unmapped' })")

class WrapperSmokeTest {
    @Test fun bufferDelegatesLabelAndSize() {
        val raw = fakeBuffer()
        val wrapped = Buffer(raw)
        assertEquals(16uL, wrapped.size)
        wrapped.label = "after"
        assertEquals("after", raw.label)
    }
}
```

- [ ] **Step 2: Exécuter le test rouge.** `rtk proxy ./gradlew :webgpu-browser:jsNodeTest :webgpu-browser:wasmJsNodeTest`. Attendre l’absence de `Buffer`, puis importer le code.
- [ ] **Step 3: Copier le graphe minimal des wrappers.** Depuis `wgpu4k/src/webMain/kotlin`, copier les fichiers wrappers listés dans **Files**, en omettant pour l’instant les surfaces. Depuis `commonMain/kotlin`, copier `AdapterInfo.kt`, `Limits.kt`, `GPUError.kt`. Depuis les dossiers JS/Wasm, copier les quatre fichiers spécifiques listés ci-dessus.

Conserver les conversions suivantes : `AdapterInfo`, `BindGroupDescriptor`, `BindGroupLayoutDescriptor`, `BufferDescriptor`, `Color`, `CommandBufferDescriptor`, `CommandEncoderDescriptor`, `ComputePassDescriptor`, `ComputePipelineDescriptor`, `DeviceDescriptor`, `Extent3D`, `GPUCompilationInfo`, `GPUError`, `List`, `Origin3D`, `PipelineLayoutDescriptor`, `QuerySetDescriptor`, `RenderBundleDescriptor`, `RenderBundleEncoderDescriptor`, `RenderPassDescriptor`, `RenderPipelineDescriptor`, `SamplerDescriptor`, `ShaderModuleDescriptor`, `SupportedLimits`, `TexelCopyBufferInfo`, `TexelCopyBufferLayout`, `TexelCopyTextureInfo`, `TextureDescriptor`, `TextureViewDescriptor` (tous `.kt`). `SurfaceConfiguration.kt` sera traité avec le canvas.

Les helpers de compatibilité hors contrat (`DeviceExt.web.kt`, `QueueExt.web.kt` et les anciennes énumérations natives de surface) ne font pas partie du graphe nécessaire. En particulier, ne pas importer les anciens types d’image dépréciés pour faire compiler le module.

- [ ] **Step 4: Adapter les packages et les déclarations.** Les wrappers sont dans `.browser`, les conversions dans `.browser.mapper`. Importer explicitement les contrats depuis `org.graphiks.webgpu` et l’interop depuis `.bindings`. Enlever `actual` des wrappers et de leurs overrides, mais conserver les `expect`/`actual` de `configureUncapturedError` et des trois tests de type d’erreur. Retirer les champs/imports de logging inutilisés dans `Adapter.kt` et `Buffer.kt` plutôt qu’ajouter une dépendance inutile.

Exemple d’adaptation complète d’un wrapper simple :

```kotlin
package org.graphiks.webgpu.browser

import org.graphiks.webgpu.GPUBindGroup
import org.graphiks.webgpu.bindings.WGPUBindGroup

class BindGroup(val handler: WGPUBindGroup) : GPUBindGroup {
    override var label: String
        get() = handler.label
        set(value) { handler.label = value }
    override fun close() = Unit
}
```

- [ ] **Step 5: Combler les membres obligatoires déjà présents dans le contrat.** Ajouter les cinq propriétés `UInt` à `Limits`, à la lecture `WGPUSupportedLimits → Limits` et au record des limites demandées : `maxImmediateSize`, `maxStorageBuffersInVertexStage`, `maxStorageBuffersInFragmentStage`, `maxStorageTexturesInVertexStage`, `maxStorageTexturesInFragmentStage`. Lire et convertir explicitement chacune comme les limites existantes. Le mapper inverse retourne désormais `WebGpuRecord`, construit par `createWebGpuRecord` et rempli par `setRecordValue`, en reprenant toutes les entrées existantes et ces cinq entrées.

Ajouter `setImmediates` aux trois encodeurs qui implémentent `GPUBindingCommandsMixin` :

```kotlin
override fun setImmediates(
    rangeOffset: GPUSize32,
    data: ArrayBuffer,
    dataOffset: GPUSize64,
    dataSize: GPUSize64?,
) {
    val raw = (data as WebArrayBuffer).buffer
    when (dataSize) {
        null -> handler.setImmediates(rangeOffset.asJsNumber(), raw, dataOffset.asJsNumber())
        else -> handler.setImmediates(rangeOffset.asJsNumber(), raw, dataOffset.asJsNumber(), dataSize.asJsNumber())
    }
}
```

Ce code s’insère dans `RenderPassEncoder`, `RenderBundleEncoder` et `ComputePassEncoder`. Les tests de transmission arrivent en tâche 6, avant toute extension de cette logique.

Le contrat `GPUBindingResource` accepte aussi directement `GPUBuffer` et `GPUTexture`. Dans `mapper/BindGroupDescriptor.kt`, compléter le `when` existant avec `is GPUBuffer -> (localResource as Buffer).handler` et `is GPUTexture -> (localResource as Texture).handler`. Conserver les branches sampler, buffer binding et texture view. Cette adaptation rend le `when` exhaustif contre l’API actuelle ; la tâche 5 en vérifie les valeurs transmises.

- [ ] **Step 6: Vérifier la compilation et le test de wrapper**, puis committer. Résoudre les différences de signatures avec l’API du dépôt, jamais en ramenant l’API à une version antérieure.

```sh
rtk proxy ./gradlew :webgpu-browser:jsNodeTest :webgpu-browser:wasmJsNodeTest
rtk git diff --check
rtk git add settings.gradle.kts webgpu-browser
rtk git commit -m "feat: add Graphiks WebGPU browser implementation"
```

## Task 4: Garantir les résultats asynchrones et les erreurs

**Files:**
- Modify: `webgpu-browser/src/commonMain/kotlin/{Adapter,Buffer,Device,Queue,ShaderModule}.kt`
- Modify: `webgpu-web-bindings/src/commonMain/kotlin/interop.kt`
- Create: `webgpu-browser/src/commonMain/kotlin/BrowserResult.kt`
- Create: `webgpu-browser/src/commonTest/kotlin/AsyncContractTest.kt`
- Modify: fichiers `mapper/GPUError.js.kt` et `mapper/GPUError.wasmJs.kt` si les tests identifient un problème de classification

**Interfaces:**
- Produces: `internal suspend fun <T> browserResult(block: suspend () -> T): Result<T>`.
- Produces: surcharge interne testable `requestAdapter(gpu: GPU?, options: GPURequestAdapterOptions?): Result<Adapter>` ; `GPU` désigne le type de `.bindings.interop.kt`, pas une nouvelle API publique.

- [ ] **Step 1: Écrire les tests à Promises contrôlées.** Dans le package `.browser`, définir :

```kotlin
private fun emptyScopeDevice(): WGPUDevice = js("({ popErrorScope: () => Promise.resolve(null) })")
private fun rejectedScopeDevice(): WGPUDevice = js("({ popErrorScope: () => Promise.reject(new Error('scope rejected')) })")
private fun rejectingBuffer(): WGPUBuffer = js("({ mapAsync: () => Promise.reject(new Error('map rejected')) })")
private fun fulfilledBuffer(): WGPUBuffer = js("({ mapAsync: () => Promise.resolve() })")
private fun pendingBuffer(): WGPUBuffer = js("({ mapAsync: () => new Promise(() => {}) })")
private fun noAdapterGpu(): GPU = js("({ requestAdapter: () => Promise.resolve(null) })")
private fun rejectingGpu(): GPU = js("({ requestAdapter: () => Promise.reject(new Error('adapter rejected')) })")

@Test fun emptyErrorScopeIsSuccessfulNull() = runTest {
    val result = Device(emptyScopeDevice()).popErrorScope()
    assertTrue(result.isSuccess)
    assertNull(result.getOrThrow())
}

@Test fun rejectedPromisesBecomeFailures() = runTest {
    assertTrue(Device(rejectedScopeDevice()).popErrorScope().isFailure)
    assertTrue(Buffer(rejectingBuffer()).mapAsync(GPUMapMode.Read).isFailure)
    assertTrue(Buffer(fulfilledBuffer()).mapAsync(GPUMapMode.Read).isSuccess)
    assertTrue(requestAdapter(null, null).isFailure)
    assertTrue(requestAdapter(noAdapterGpu(), null).isFailure)
    assertTrue(requestAdapter(rejectingGpu(), null).isFailure)
}

@Test fun cancellationDoesNotReturnAResult() = runTest {
    var returned = false
    val job = launch {
        Buffer(pendingBuffer()).mapAsync(GPUMapMode.Read)
        returned = true
    }
    runCurrent()
    job.cancelAndJoin()
    assertFalse(returned)
}
```

Importer `kotlinx.coroutines.test.runTest`, `runCurrent`, `kotlinx.coroutines.launch`, `cancelAndJoin`, `kotlin.test.*`, les contrats/bindings et `kotlin.js.js`. Ajouter l’opt-in `ExperimentalCoroutinesApi` au test de cancellation si requis par la version utilisée.

- [ ] **Step 2: Démontrer les échecs de comportement** avec les deux `jsNodeTest`/`wasmJsNodeTest`, puis corriger :

```kotlin
internal suspend fun <T> browserResult(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (cancelled: kotlinx.coroutines.CancellationException) {
    throw cancelled
} catch (failure: Throwable) {
    Result.failure(failure)
}
```

`mapAsync` attend la Promise dans ce helper et termine par `Unit`. `popErrorScope` teste le résultat nullable avant `unsafeCast<WGPUError>()` et `errorOf`. Les autres méthodes publiques suspendues retournant `Result` utilisent la même politique, notamment pipelines async, `requestDevice`, `onSubmittedWorkDone` et compilation info.

```kotlin
override suspend fun popErrorScope(): Result<GPUError?> = browserResult {
    handler.popErrorScope().await()?.let { errorOf(it.unsafeCast<WGPUError>()) }
}

private fun browserGpu(): GPU? = js("globalThis.navigator?.gpu ?? null")

suspend fun requestAdapter(options: GPURequestAdapterOptions? = null): Result<Adapter> =
    requestAdapter(browserGpu(), options)

internal suspend fun requestAdapter(gpu: GPU?, options: GPURequestAdapterOptions?): Result<Adapter> = browserResult {
    checkNotNull(gpu) { "WebGPU is not available in this environment." }
    val raw = when (options) {
        null -> gpu.requestAdapter()
        else -> gpu.requestAdapter(map(options))
    }.await()
    Adapter(checkNotNull(raw) { "No WebGPU adapter is available." }.unsafeCast<WGPUAdapter>())
}
```

La fonction locale `map(GPURequestAdapterOptions)` existe dans `Adapter.kt`. Le type manuel `GPU` de `webgpu-web-bindings/src/commonMain/kotlin/interop.kt` doit aussi déclarer `Promise<JsAny?>` pour les deux surcharges de `requestAdapter`, en cohérence avec les bindings générés.

- [ ] **Step 3: Vérifier la classification des vraies erreurs dans la tâche 7.** Les tests Node ci-dessus vérifient rejets et null, mais ne simulent pas des constructeurs GPU globaux inexistants. Le test navigateur provoquera une erreur de validation capturée et vérifiera `GPUValidationError` et son message non vide ; le callback uncaptured doit être testé sous JS et Wasm avec une erreur volontaire hors scope.
- [ ] **Step 4: Vérifier et committer.**

```sh
rtk proxy ./gradlew :webgpu-browser:jsNodeTest :webgpu-browser:wasmJsNodeTest
rtk git diff --check
rtk git add webgpu-browser webgpu-web-bindings/src/commonMain/kotlin/interop.kt
rtk git commit -m "fix: honor browser asynchronous result and cancellation contracts"
```

## Task 5: Rendre les conversions de descripteurs fidèles

**Files:**
- Modify: `webgpu-browser/src/commonMain/kotlin/mapper/{ComputePipelineDescriptor,RenderPipelineDescriptor,ShaderModuleDescriptor,RenderPassDescriptor,ComputePassDescriptor,PipelineLayoutDescriptor,TextureViewDescriptor,SupportedLimits,BindGroupDescriptor}.kt`
- Create: `webgpu-browser/src/commonMain/kotlin/mapper/{PipelineConstants,TextureAttachment}.kt`
- Create: `webgpu-browser/src/commonTest/kotlin/{PipelineDescriptorTest,PassDescriptorTest,TextureDescriptorTest,LimitsDescriptorTest,BindGroupDescriptorTest}.kt`

**Interfaces:**
- Produces: `internal fun mapConstants(input: Map<String, GPUPipelineConstantValue>): WebGpuRecord`.
- Produces: `internal fun mapAttachment(input: GPUTextureOrGPUTextureView): JsAny`.
- Consumes: les wrappers et les helpers de record déjà compilés.

- [ ] **Step 1: Ajouter les régressions de pipeline avant les conversions.** Les fixtures JS vides sont suffisantes car ces tests comparent les objets transmis, sans exécuter de GPU.

```kotlin
private fun emptyShader(): WGPUShaderModule = js("({})")
private fun propertyNumber(value: JsAny, key: String): Double = js("value[key]")
private fun propertyString(value: JsAny, key: String): String = js("value[key]")
private fun sameJs(left: JsAny?, right: JsAny?): Boolean = js("left === right")

@Test fun computeAutoLayoutAndMutableConstantsAreForwarded() {
    val constants = mutableMapOf("scale" to 2.0)
    val descriptor = ComputePipelineDescriptor(
        compute = ProgrammableStage(ShaderModule(emptyShader()), constants = constants),
    )
    val first = map(descriptor)
    assertEquals("auto", propertyString(first, "layout"))
    assertEquals(2.0, propertyNumber(first.compute.constants, "scale"))
    constants["scale"] = 7.0
    val second = map(descriptor)
    assertEquals(7.0, propertyNumber(second.compute.constants, "scale"))
    assertEquals(2.0, propertyNumber(first.compute.constants, "scale"))
}
```

Ajouter un test render construisant `RenderPipelineDescriptor(vertex = VertexState(shader, constants = mapOf("vertexScale" to 3.0)), fragment = FragmentState(targets = emptyList(), module = shader, constants = mapOf("fragmentScale" to 4.0)))`. Vérifier les deux records, un layout explicite par identité et l’omission d’`entryPoint` quand il vaut `null` via `Object.hasOwn(value, 'entryPoint')`.

- [ ] **Step 2: Exécuter les tests rouges**, puis implémenter les constantes et layouts dans les trois stages et les hints :

```kotlin
internal fun mapConstants(input: Map<String, GPUPipelineConstantValue>): WebGpuRecord =
    createWebGpuRecord().also { record ->
        input.forEach { (name, value) -> setRecordValue(record, name, value.asJsNumber()) }
    }
```

Dans les mappers compute/render : `layout = (input.layout as PipelineLayout?)?.handler ?: "auto".toJsString()`. Dans chaque stage : `constants = mapConstants(input.constants)`. Dans un compilation hint : `entryPoint = input.entryPoint` et le même mapping de layout. Tester `ShaderModuleDescriptor(code = "", compilationHints = listOf(ShaderModuleCompilationHint("main")))` et un hint avec layout explicite ; vérifier l’array JS et la valeur/identité du layout. Le `TODO()` doit disparaître.

- [ ] **Step 3: Tester les passes avec queries et timestamps.** Construire `QuerySet(createJsObject<WGPUQuerySet>())`, puis les deux types de timestamp avec `beginningOfPassWriteIndex = 0u`, `endOfPassWriteIndex = 3u`. Vérifier par lecture JS : le même handler, `0`, `3`. Tester aussi beginning seul et end seul et vérifier l’absence de propriété pour l’index nul.

```kotlin
private fun hasOwn(value: JsAny, key: String): Boolean = js("Object.hasOwn(value, key)")

@Test fun computeTimestampPreservesZeroAndOmitsMissingEnd() {
    val query = QuerySet(createJsObject<WGPUQuerySet>())
    val descriptor = ComputePassDescriptor(
        timestampWrites = ComputePassTimestampWrites(query, beginningOfPassWriteIndex = 0u),
    )
    val out = map(descriptor)
    assertTrue(sameJs(query.handler, out.timestampWrites.querySet))
    assertEquals(0.0, propertyNumber(out.timestampWrites, "beginningOfPassWriteIndex"))
    assertFalse(hasOwn(out.timestampWrites, "endOfPassWriteIndex"))
}
```

Le test render utilise `RenderPassDescriptor(colorAttachments = emptyList(), occlusionQuerySet = query, timestampWrites = RenderPassTimestampWrites(query, endOfPassWriteIndex = 3u))`. Vérifier `occlusionQuerySet` et les timestamps séparément ; le test de conversion n’impose pas le type réel de la query fictive.

- [ ] **Step 4: Implémenter les champs de passes**, en utilisant un `createJsObject<WGPUComputePassTimestampWrites>()` ou `WGPURenderPassTimestampWrites` :

```kotlin
querySet = (input.querySet as QuerySet).handler
input.beginningOfPassWriteIndex?.let { beginningOfPassWriteIndex = it.asJsNumber() }
input.endOfPassWriteIndex?.let { endOfPassWriteIndex = it.asJsNumber() }
```

Dans les descripteurs de passe, assigner les objets uniquement si fournis. Ne pas réinterpréter `0u` comme une absence.

- [ ] **Step 5: Tester puis prendre en charge l’union d’attachments.** Tester les deux alternatives pour `view` couleur et profondeur et pour `resolveTarget`, ainsi que `resolveTarget = null`. Les handlers transmis doivent être identiques, sans fabriquer une vue par défaut ni accepter un objet arbitraire :

```kotlin
internal fun mapAttachment(input: GPUTextureOrGPUTextureView): JsAny = when (input) {
    is Texture -> input.handler
    is TextureView -> input.handler
    else -> error("The attachment must belong to the browser implementation.")
}
```

Remplacer les trois casts vers `TextureView` dans `RenderPassDescriptor.kt` par ce helper. Le type généré de l’union est déjà `JsAny`.

- [ ] **Step 6: Tester les champs supplémentaires.**

```kotlin
@Test fun textureViewForwardsUsageAndSwizzle() {
    val out = map(TextureViewDescriptor(
        usage = GPUTextureUsage.TextureBinding,
        swizzle = GPUTextureSwizzle(
            GPUTextureSwizzleSource.Blue,
            GPUTextureSwizzleSource.Zero,
            GPUTextureSwizzleSource.One,
            GPUTextureSwizzleSource.Red,
        ),
    ))
    assertEquals("b01r", out.swizzle)
    assertEquals(GPUTextureUsage.TextureBinding.value.toDouble(), propertyNumber(out, "usage"))
}

@Test fun pipelineLayoutForwardsImmediateSize() {
    val out = map(PipelineLayoutDescriptor(bindGroupLayouts = emptyList(), immediateSize = 16u))
    assertEquals(16.0, propertyNumber(out, "immediateSize"))
}
```

Ajouter `usage = input.usage.value.asJsNumber()`, `swizzle = input.swizzle.toWebGpuString()` au mapper de vue, et `immediateSize = input.immediateSize.asJsNumber()` au mapper de layout. Pour `LimitsDescriptorTest`, fournir un `WGPUSupportedLimits` via un objet JS `Proxy` retournant `64` pour chaque champ numérique, sauf `maxBufferSize` qui retourne `4294967296`; convertir vers `GPUSupportedLimits` puis vers record et vérifier les cinq nouveaux noms et la valeur 64 bits. Cela vérifie l’aller-retour et les propriétés énumérables, pas une simple liste codée deux fois en Kotlin.

Dans `BindGroupDescriptorTest`, vérifier l’identité du handler pour un buffer direct, une texture directe, une vue et un sampler, puis vérifier le dictionnaire d’un `BufferBinding(buffer, offset = 8uL, size = 16uL)`. Utiliser ce test pour les deux branches ajoutées à l’extraction :

```kotlin
private fun resourceAt(descriptor: WGPUBindGroupDescriptor, index: Int): JsAny = js("descriptor.entries[index].resource")

@Test fun directBindingResourcesKeepTheirNativeIdentity() {
    val layout = BindGroupLayout(createJsObject<WGPUBindGroupLayout>())
    val buffer = Buffer(createJsObject<WGPUBuffer>())
    val texture = Texture(createJsObject<WGPUTexture>())
    val out = map(BindGroupDescriptor(layout, listOf(
        BindGroupEntry(0u, buffer),
        BindGroupEntry(1u, texture),
    )))
    assertTrue(sameJs(buffer.handler, resourceAt(out, 0)))
    assertTrue(sameJs(texture.handler, resourceAt(out, 1)))
}
```

Les fonctions JS de lecture et d’identité utilisées dans plusieurs tests peuvent
être regroupées dans `webgpu-browser/src/commonTest/kotlin/JsAssertions.kt` avec
une visibilité `internal`, dans le package `.browser`, et importées par les
tests de mappers. Elles ne font pas partie de l’API de production.

- [ ] **Step 7: Exécuter la suite et committer.**

```sh
rtk proxy ./gradlew :webgpu-browser:jsNodeTest :webgpu-browser:wasmJsNodeTest
rtk git diff --check
rtk git add webgpu-browser
rtk git commit -m "fix: faithfully map browser pipeline and pass descriptors"
```

## Task 6: Vérifier les commandes, les offsets et la propriété des ressources

**Files:**
- Modify: `webgpu-browser/src/commonMain/kotlin/{RenderPassEncoder,RenderBundleEncoder,ComputePassEncoder,Texture}.kt`
- Modify if regression fails: `webgpu-browser/src/commonMain/kotlin/{Queue,Buffer}.kt`
- Create: `webgpu-browser/src/commonTest/kotlin/{EncoderContractTest,ResourceContractTest}.kt`

**Interfaces:**
- Consumes: signatures existantes `setBindGroup`, `setVertexBuffer`, `setImmediates`, `Queue.writeBuffer`, `Texture.close` et `Texture.usage`.
- Produces: transmission fidèle des handles, de `null`, des arguments optionnels et des flags.

- [ ] **Step 1: Enregistrer les appels JS des encodeurs.** Dans le test, créer un objet unique utilisable via `unsafeCast` avec les trois types de handlers :

```kotlin
private fun recorder(): JsAny = js("""({
    calls: [],
    setBindGroup(...args) { this.calls.push(args); },
    setVertexBuffer(...args) { this.calls.push(args); },
    setImmediates(...args) { this.calls.push(args); },
    writeBuffer(...args) { this.calls.push(args); }
})""")
private fun argCount(raw: JsAny, call: Int): Int = js("raw.calls[call].length")
private fun argIsNull(raw: JsAny, call: Int, index: Int): Boolean = js("raw.calls[call][index] === null")
private fun argNumber(raw: JsAny, call: Int, index: Int): Double = js("raw.calls[call][index]")
private fun argSame(raw: JsAny, call: Int, index: Int, expected: JsAny): Boolean = js("raw.calls[call][index] === expected")
```

Pour les trois encodeurs : appeler `setBindGroup(0u, null)` et vérifier l’argument `null`; appeler avec un bind group et `[256u, 512u]` et vérifier son identité et le contenu du tableau. Pour render pass et render bundle : appeler `setVertexBuffer(1u, null)` puis avec un buffer, offset `8uL`, taille `16uL`; vérifier les deux variantes.

- [ ] **Step 2: Exécuter les tests rouges et corriger les casts.** Remplacer `(bindGroup as BindGroup).handler` par `(bindGroup as BindGroup?)?.handler` et idem pour `Buffer?`. Utiliser les paramètres nullable corrigés en tâche 2 ; ne pas caster `null` en `JsAny` non nullable. Le cas sans offsets peut utiliser la surcharge à deux paramètres du binding ; vérifier le cas avec offsets séparément.
- [ ] **Step 3: Tester les unités et l’omission des tailles.** Créer `val data = ArrayBuffer.of(intArrayOf(1, 2, 3, 4))`, puis récupérer `(data as WebArrayBuffer).buffer` pour les assertions d’identité. Pour chaque encodeur, appeler `setImmediates(4u, data, 8uL, null)` et vérifier trois arguments, offsets `4` et `8`, même buffer. Appeler à nouveau avec `dataSize = 4uL` et vérifier quatre arguments et taille `4`. Faire les mêmes assertions pour `Queue.writeBuffer(target, 8uL, data, 4uL, null)` et une taille explicite `8uL` (quatre puis cinq arguments). Les offsets et tailles portent sur l’ArrayBuffer brut et sont donc en octets.
- [ ] **Step 4: Tester puis corriger les flags.**

```kotlin
private fun fakeTexture(usage: Double): WGPUTexture = js("({ usage, destroyed: 0, destroy() { this.destroyed++; } })")
private fun destroyed(texture: WGPUTexture): Int = js("texture.destroyed")

@Test fun textureUsageContainsOnlySetBits() {
    assertEquals(emptySet(), Texture(fakeTexture(0.0)).usage)
    assertEquals(setOf(GPUTextureUsage.CopySrc), Texture(fakeTexture(GPUTextureUsage.CopySrc.value.toDouble())).usage)
    val flags = GPUTextureUsage.CopySrc or GPUTextureUsage.RenderAttachment
    assertEquals(setOf(GPUTextureUsage.CopySrc, GPUTextureUsage.RenderAttachment), Texture(fakeTexture(flags.value.toDouble())).usage)
}

@Test fun borrowedCanvasTextureIsNotDestroyed() {
    val borrowed = fakeTexture(0.0)
    Texture(borrowed, canBeDestroy = false).close()
    assertEquals(0, destroyed(borrowed))
    val owned = fakeTexture(0.0)
    Texture(owned).close()
    assertEquals(1, destroyed(owned))
}
```

La correction est `GPUTextureUsage.entries.filter { (it.value and handler.usage.toULong()) != 0uL }.toSet()`. `None` ne doit pas apparaître dans l’ensemble vide.

- [ ] **Step 5: Exécuter puis committer.**

```sh
rtk proxy ./gradlew :webgpu-browser:jsNodeTest :webgpu-browser:wasmJsNodeTest
rtk git diff --check
rtk git add webgpu-browser
rtk git commit -m "fix: preserve nullable browser commands and resource semantics"
```

## Task 7: Livrer le canvas et un parcours GPU réel sous Chromium

**Files:**
- Create: `webgpu-browser/src/commonMain/kotlin/{CanvasSurface,Surface,SurfaceConfiguration}.kt`
- Create: `webgpu-browser/src/commonMain/kotlin/mapper/SurfaceConfiguration.kt`
- Create: `webgpu-browser/src/commonTest/kotlin/CanvasSurfaceTest.kt`
- Create: `integration-tests/browser/{settings.gradle.kts,build.gradle.kts,karma.config.d/webgpu.js}`
- Create: `integration-tests/browser/src/commonTest/kotlin/{ComputeReadbackTest,CanvasReadbackTest,BrowserErrorsTest}.kt`
- Create: `.github/workflows/browser-gpu.yml`
- Modify: `.github/workflows/test.yml`

**Interfaces:**
- Produces: `fun HTMLCanvasElement.getCanvasSurface(): CanvasSurface`.
- Produces: `class CanvasSurface(val handler: WGPUCanvasContext)` avec `width`, `height`, `preferredCanvasFormat`, `configure(SurfaceConfiguration)`, `getCurrentTexture(): SurfaceTexture`, `present()`, `close()`.
- Produces: `data class SurfaceTexture(val texture: GPUTexture)` ; `SurfaceConfiguration` décrit les paramètres web ci-dessous. Ne pas exposer des statuts natifs que le navigateur ne fournit pas.

- [ ] **Step 1: Définir le contrat canvas minimal et son test à handler contrôlé.**

```kotlin
data class SurfaceConfiguration(
    val device: GPUDevice,
    val format: GPUTextureFormat,
    val usage: GPUTextureUsage = GPUTextureUsage.RenderAttachment,
    val viewFormats: Set<GPUTextureFormat> = emptySet(),
    val colorSpace: PredefinedColorSpace = PredefinedColorSpace.srgb,
    val alphaMode: GPUCanvasAlphaMode = GPUCanvasAlphaMode.Opaque,
)

enum class PredefinedColorSpace(val value: String) { srgb("srgb"), displayp3("display-p3") }
enum class GPUCanvasAlphaMode(val value: String) { Opaque("opaque"), Premultiplied("premultiplied") }
data class SurfaceTexture(val texture: GPUTexture)
```

Ces types restent dans `.browser`. Le navigateur ne propose pas de `presentMode`; ne pas importer `PresentMode` ou `CompositeAlphaMode` natifs. `present()` reste un no-op documenté (présentation gérée par le navigateur). `close()` appelle `unconfigure()`, pas `destroy()` sur une texture acquise.

Le test crée un handler JS avec `canvas: {width: 2, height: 3}`, `configure(value)` enregistrant le dernier descripteur, `getCurrentTexture()` retournant une texture enregistrée, et `unconfigure()` incrémentant un compteur. Vérifier dimensions, identité du device, format, flags, mode alpha, texture empruntée et une invocation d’unconfigure. Vérifier que `getCanvasSurface` retourne le wrapper et qu’un contexte absent produit un message explicite.

- [ ] **Step 2: Adapter les fichiers canvas importés à ce contrat.** Le mapper de configuration reprend les champs web existants. `getCanvasSurface` obtient le contexte `GPUCanvasContext.ID`, vérifie qu’il n’est pas nul et construit `CanvasSurface(context.unsafeCast<WGPUCanvasContext>())`. `getCurrentTexture()` retourne `SurfaceTexture(Texture(handler.getCurrentTexture(), canBeDestroy = false))`. Les défauts de configuration remontent comme exceptions des méthodes synchrones ; les opérations asynchrones conservent leurs `Result`.
- [ ] **Step 3: Vérifier les tests canvas sans GPU**, avec les deux tâches Node du module.
- [ ] **Step 4: Créer un build de tests autonome, réutilisable comme consommateur publié.** `settings.gradle.kts` configure les plugins et la résolution :

```kotlin
pluginManagement {
    repositories { gradlePluginPortal(); google(); mavenCentral() }
}
rootProject.name = "webgpu-browser-integration"

val publicationRepository = providers.gradleProperty("publicationRepository").orNull
dependencyResolutionManagement {
    repositories {
        if (publicationRepository != null) {
            exclusiveContent {
                forRepository { maven { url = uri(publicationRepository) } }
                filter { includeGroup("org.graphiks") }
            }
        }
        mavenCentral()
        google()
    }
}
if (publicationRepository == null) {
    includeBuild("../..") {
        dependencySubstitution {
            substitute(module("org.graphiks:webgpu-browser")).using(project(":webgpu-browser"))
            substitute(module("org.graphiks:webgpu-descriptors")).using(project(":webgpu-descriptors"))
            substitute(module("org.graphiks:webgpu-api")).using(project(":webgpu-api"))
            substitute(module("org.graphiks:webgpu-web-bindings")).using(project(":webgpu-web-bindings"))
        }
    }
}
```

`build.gradle.kts` :

```kotlin
plugins { kotlin("multiplatform") version "2.4.20" }

val testedVersion = providers.gradleProperty("testedVersion").getOrElse("0.1.0-SNAPSHOT")
kotlin {
    js { browser { testTask { useKarma { useChromeHeadless() } } } }
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs { browser { testTask { useKarma { useChromeHeadless() } } } }
    compilerOptions {
        optIn.add("kotlin.js.ExperimentalWasmJsInterop")
        optIn.add("kotlin.ExperimentalUnsignedTypes")
    }
    sourceSets.commonTest.dependencies {
        implementation(kotlin("test"))
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
        implementation("org.graphiks:webgpu-browser:$testedVersion")
        implementation("org.graphiks:webgpu-descriptors:$testedVersion")
    }
}
```

Ce build n’est pas inclus dans le `settings.gradle.kts` principal et n’applique pas `publish`. Il ne possède pas de cible Node. Les tests utilisent uniquement les API publiques.

- [ ] **Step 5: Configurer Chromium avec un backend logiciel WebGPU.** Le fragment `karma.config.d/webgpu.js` :

```javascript
config.customLaunchers = {
  ...config.customLaunchers,
  ChromeHeadlessWebGPU: {
    base: 'ChromeHeadless',
    flags: [
      '--enable-unsafe-webgpu',
      '--enable-unsafe-swiftshader',
      '--use-angle=swiftshader',
      '--disable-dev-shm-usage'
    ]
  }
};
config.browsers = ['ChromeHeadlessWebGPU'];
config.browserNoActivityTimeout = 120000;
```

Karma sert les tests sur localhost, un contexte sûr pour WebGPU. La disponibilité réelle du backend est vérifiée par `requestAdapter().getOrThrow()` et `requestDevice().getOrThrow()` ; aucun `return` silencieux si WebGPU est absent. Si ces flags ne fournissent pas d’adapter sur le runner, résoudre la configuration Chromium/Vulkan et conserver la gate rouge jusqu’à un vrai passage.

- [ ] **Step 6: Écrire le test compute avec résultat mémoire déterministe.** Utiliser les descripteurs publics et ce shader :

```wgsl
override scale: u32 = 1u;
@group(0) @binding(0) var<storage, read_write> output: array<u32>;
@compute @workgroup_size(1)
fn main(@builtin(global_invocation_id) id: vec3<u32>) {
    output[id.x] = (id.x + 1u) * scale;
}
```

Le cœur du test est :

```kotlin
val adapter = requestAdapter().getOrThrow()
val device = adapter.requestDevice().getOrThrow()
val storage = device.createBuffer(BufferDescriptor(16uL, GPUBufferUsage.Storage or GPUBufferUsage.CopySrc))
val staging = device.createBuffer(BufferDescriptor(16uL, GPUBufferUsage.MapRead or GPUBufferUsage.CopyDst))
try {
    device.pushErrorScope(GPUErrorFilter.Validation)
    val shader = device.createShaderModule(ShaderModuleDescriptor(code = shaderCode))
    val pipeline = device.createComputePipeline(ComputePipelineDescriptor(
        compute = ProgrammableStage(shader, entryPoint = "main", constants = mapOf("scale" to 7.0)),
    ))
    val group = device.createBindGroup(BindGroupDescriptor(
        layout = pipeline.getBindGroupLayout(0u),
        entries = listOf(BindGroupEntry(0u, BufferBinding(storage))),
    ))
    val encoder = device.createCommandEncoder()
    val pass = encoder.beginComputePass()
    pass.setPipeline(pipeline)
    pass.setBindGroup(0u, group)
    pass.dispatchWorkgroups(4u)
    pass.end()
    encoder.copyBufferToBuffer(storage, 0uL, staging, 0uL, 16uL)
    device.queue.submit(listOf(encoder.finish()))
    staging.mapAsync(GPUMapMode.Read).getOrThrow()
    val actual = staging.getMappedRange().toUIntArray()
    assertContentEquals(uintArrayOf(7u, 14u, 21u, 28u), actual)
    staging.unmap()
    assertNull(device.popErrorScope().getOrThrow())
} finally {
    staging.close()
    storage.close()
    device.close()
    adapter.close()
}
```

Déclarer `shaderCode` avec le WGSL ci-dessus dans le fichier du test. Exécuter ce bloc dans `runTest(timeout = 60.seconds)`, avec les imports du contrat, de `.descriptors`, `.browser`, `kotlin.test.*` et `kotlin.time.Duration.Companion.seconds`.

- [ ] **Step 7: Écrire le test canvas avec lecture des pixels.** Créer un élément canvas de 4×4 via `document.createElement("canvas")`, obtenir la surface, configurer le format préféré avec `RenderAttachment or CopySrc`. Acquérir la texture, créer une passe qui clear en rouge `(1.0, 0.0, 0.0, 1.0)` et stocke le résultat. Copier la texture vers un buffer de 1024 octets avec `bytesPerRow = 256u`, `rowsPerImage = 4u`, puis `mapAsync` et lire les quatre premiers octets avant unmap :

```kotlin
val pass = encoder.beginRenderPass(RenderPassDescriptor(
    colorAttachments = listOf(RenderPassColorAttachment(
        view = current.texture.createView(),
        loadOp = GPULoadOp.Clear,
        storeOp = GPUStoreOp.Store,
        clearValue = Color(1.0, 0.0, 0.0, 1.0),
    )),
))
pass.end()
encoder.copyTextureToBuffer(
    TexelCopyTextureInfo(texture = current.texture),
    TexelCopyBufferInfo(buffer = staging, bytesPerRow = 256u, rowsPerImage = 4u),
    Extent3D(4u, 4u, 1u),
)
```

Si le format est `Bgra8Unorm`, attendre `[0, 0, 255, 255]`; pour `Rgba8Unorm`, `[255, 0, 0, 255]`. Échouer explicitement sur un autre format pour ce test de 8 bits. Vérifier un scope de validation vide. Appeler `current.texture.close()` puis `surface.close()` sans détruire le device avant la fin de la lecture. Le test du clear est un test du chemin canvas, pas une comparaison d’image tolérante.

- [ ] **Step 8: Tester les erreurs réelles et le callback.** Sous scope `Validation`, créer un buffer de taille `4uL` avec `GPUBufferUsage.None`; `popErrorScope().getOrThrow()` doit être un `GPUValidationError` au message non vide. Pour le callback, demander un second device avec `DeviceDescriptor(onUncapturedError = GPUUncapturedErrorCallback { deferred.complete(it) })`, provoquer le même buffer invalide hors scope et attendre `CompletableDeferred<GPUError>` avec `withContext(Dispatchers.Default) { withTimeout(10.seconds) { deferred.await() } }`. Ce timeout utilise le dispatcher réel et ne doit pas être avancé artificiellement par l’horloge virtuelle de `runTest`. Fermer les devices dans `finally`. Les deux cibles doivent passer.

Les timestamps et le swizzle restent couverts par les tests de conversion indépendants du matériel. Ne les déclarer comme testés sur GPU que si un test correspondant demande explicitement les features disponibles ; toute non-exécution optionnelle doit être signalée.

- [ ] **Step 9: Exécuter les tests GPU puis les intégrer à la CI.**

```sh
rtk proxy ./gradlew -p integration-tests/browser jsBrowserTest wasmJsBrowserTest
```

Créer un workflow réutilisable `.github/workflows/browser-gpu.yml` :

```yaml
name: Browser GPU tests
on:
  workflow_call:
permissions:
  contents: read
jobs:
  gpu:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 25
      - uses: gradle/actions/setup-gradle@v6
      - uses: browser-actions/setup-chrome@v1
        id: chrome
      - name: Compute, canvas and errors on JS and Wasm
        env:
          CHROME_BIN: ${{ steps.chrome.outputs.chrome-path }}
        run: ./gradlew --no-daemon -p integration-tests/browser jsBrowserTest wasmJsBrowserTest
      - uses: actions/upload-artifact@v4
        if: always()
        with:
          name: browser-gpu-reports
          path: integration-tests/browser/build/reports/tests/
```

Dans `test.yml`, ajouter le job `browser-gpu: { uses: ./.github/workflows/browser-gpu.yml }`. La matrice existante conserve les tests de l’API et de l’interop ; ajouter les deux tâches Node du nouveau module à la commande ciblée Linux. La disponibilité du navigateur et ses bibliothèques système doit être confirmée sur le runner par les tests, pas déduite de l’installation du binaire.

- [ ] **Step 10: Commiter après passage local ou passage CI observé.**

```sh
rtk git diff --check
rtk git add webgpu-browser integration-tests/browser .github/workflows/test.yml .github/workflows/browser-gpu.yml
rtk git commit -m "test: validate browser compute and canvas paths on JS and Wasm"
```

## Task 8: Publier les quatre modules et vérifier un consommateur Maven

**Files:**
- Modify: `buildSrc/src/main/kotlin/publish.gradle.kts`
- Modify: `.github/workflows/publish.yml`
- Modify: `docs/build.gradle.kts`, `docs/mkdocs.yml`, `README.md`, `TYPE_MAPPING.md`
- Modify: `docs/docs/{index,index.fr,getting-started,getting-started.fr,architecture,architecture.fr,testing,testing.fr,specification-maintenance,specification-maintenance.fr}.md`
- Modify: `integration-tests/browser/` uniquement si la résolution publiée révèle une erreur du harness

**Interfaces:**
- Consumes: les quatre coordonnées Maven et les tests GPU publics des tâches précédentes.
- Produces: publication locale complète et workflows snapshot/release listant les quatre modules.

- [ ] **Step 1: Adapter les descriptions POM par module.**

```kotlin
val libraryDescription = when (project.name) {
    "webgpu-api" -> "Shared WebGPU API for Kotlin Multiplatform"
    "webgpu-descriptors" -> "Descriptor implementations for the Graphiks WebGPU API"
    "webgpu-web-bindings" -> "WebGPU JavaScript bindings and Kotlin JS/Wasm interop"
    "webgpu-browser" -> "Browser implementation of the Graphiks WebGPU API for Kotlin JS and Wasm"
    else -> "Graphiks WebGPU for Kotlin Multiplatform"
}
```

Conserver la signature, le group ID et la politique de version existants. Dans les deux jobs de publication, utiliser :

```sh
./gradlew --no-configuration-cache --no-daemon \
  :webgpu-api:publishToMavenCentral \
  :webgpu-descriptors:publishToMavenCentral \
  :webgpu-web-bindings:publishToMavenCentral \
  :webgpu-browser:publishToMavenCentral
```

Conserver l’argument `releaseVersion` et les contrôles de credentials propres à chaque job. Dans `publish.yml`, ajouter un job `browser-gpu: { uses: ./.github/workflows/browser-gpu.yml }` et `needs: browser-gpu` aux jobs `snapshot` et `release`. Ainsi une publication dépend du succès GPU dans le même workflow ; un workflow `test.yml` séparé ne bloque pas à lui seul `publish.yml`.

- [ ] **Step 2: Publier dans un dépôt local isolé.** Choisir un répertoire temporaire approuvé et le stocker dans `MAVEN_REPO`; utiliser exactement la même valeur pour Gradle et le consommateur. Version de vérification : `0.1.0-browser-verification-SNAPSHOT`.

```sh
rtk proxy ./gradlew -Dmaven.repo.local="$MAVEN_REPO" -PreleaseVersion=0.1.0-browser-verification-SNAPSHOT :webgpu-api:publishToMavenLocal :webgpu-descriptors:publishToMavenLocal :webgpu-web-bindings:publishToMavenLocal :webgpu-browser:publishToMavenLocal
rtk proxy ./gradlew -p integration-tests/browser -PpublicationRepository="$MAVEN_REPO" -PtestedVersion=0.1.0-browser-verification-SNAPSHOT jsBrowserTest wasmJsBrowserTest
```

Attendu : aucune substitution vers les projets du dépôt dans le second build, résolution exclusive des artefacts `org.graphiks` dans ce répertoire et réussite du calcul, du canvas et des erreurs sur les deux cibles. Inspecter aussi `dependencies --configuration jsTestCompileClasspath` et `wasmJsTestCompileClasspath` dans le build consommateur. En cas de nom de configuration différent dans Kotlin 2.4.20, obtenir le nom depuis `dependencies` et consigner la commande réellement exécutée.

- [ ] **Step 3: Rendre les quatre modules visibles dans Dokka/MkDocs.** Ajouter `:webgpu-browser` à `apiModules`, remplacer la navigation `webgpu-web` par `webgpu-web-bindings`, ajouter `webgpu-browser`. Décrire les packages/imports explicites dans les guides EN/FR. Exemple utilisateur :

```kotlin
import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.webgpu.browser.requestAdapter
import org.graphiks.webgpu.descriptors.BufferDescriptor

suspend fun createExampleBuffer() {
    val adapter = requestAdapter().getOrThrow()
    val device = adapter.requestDevice().getOrThrow()
    try {
        val buffer = device.createBuffer(BufferDescriptor(
            size = 16uL,
            usage = GPUBufferUsage.CopyDst or GPUBufferUsage.Storage,
        ))
        buffer.close()
    } finally {
        device.close()
        adapter.close()
    }
}
```

Le guide Gradle indique `implementation("org.graphiks:webgpu-browser:0.1.0-SNAPSHOT")` et `implementation("org.graphiks:webgpu-descriptors:0.1.0-SNAPSHOT")` dans le source set partagé JS/Wasm, avec le repository snapshots déjà utilisé par le projet ; les releases utilisent Maven Central. Préciser que `webgpu-web-bindings` sert à l’interop directe, que l’accès WebGPU exige un navigateur compatible et un contexte sûr, et que les helpers canvas appartiennent au package `.browser`.

- [ ] **Step 4: Actualiser les commandes de maintenance et de tests.** Documenter les commandes Node des bindings/browser, le build GPU autonome et son mode `publicationRepository`. Dans `TYPE_MAPPING.md`, corriger la représentation web de `record`, les packages et la nullabilité modifiée. Ne pas ajouter de récit de provenance dans les guides.
- [ ] **Step 5: Vérifier l’ensemble final.**

```sh
rtk proxy ./gradlew -p build-logic test
rtk proxy ./gradlew check
rtk proxy ./gradlew :docs:embedDokkaIntoMkDocs
rtk git diff --check
```

Sur un hôte compatible avec la baseline ABI, exécuter `rtk proxy ./gradlew :webgpu-api:checkKotlinAbi`. Le package de l’API commune n’a pas changé : ne pas réécrire les snapshots pour masquer un changement de contrat accidentel. Le build de documentation peut écrire des sorties ignorées et compacter la navigation ; examiner le diff avant commit. Rechercher les références résiduelles à l’ancien module dans les fichiers actifs, en excluant les deux documents de conception qui décrivent volontairement le point de départ.

- [ ] **Step 6: Commiter et remettre les résultats à l’utilisateur.**

```sh
rtk git add buildSrc/src/main/kotlin/publish.gradle.kts .github/workflows/publish.yml docs README.md TYPE_MAPPING.md
rtk git commit -m "feat: publish and document Graphiks WebGPU browser modules"
```

Dans le compte rendu final de l’exécutant, fournir : commits, commandes réellement passées, résultats JS et Wasm séparés, version du Chromium utilisé, preuve de résolution des artefacts locaux, limitations effectivement observées. La publication distante est assurée par le workflow habituel lorsque l’utilisateur intègre les changements.

## Couverture de la spec et revue du plan

| Exigence | Tâches |
| --- | --- |
| Noms, packages, générateur et imports reproductibles | 1 |
| Records et valeurs nullable utilisables en JS/Wasm | 2 |
| Wrappers autonomes, interfaces actuelles et publication du module | 3, 8 |
| Adapter absent/null, rejets et cancellation | 4 |
| Layouts, constantes, compilation hints, queries, timestamps | 5 |
| Unions d’attachments, swizzle, usages des vues et nouvelles limites | 3, 5 |
| Unbind, offsets, immediates, flags et propriété des textures | 6 |
| Canvas, calcul avec lecture mémoire, erreurs réelles et CI GPU | 7 |
| Consommation des artefacts et documentation EN/FR | 8 |

- [x] Chaque exigence de la conception possède une tâche.
- [x] Les cinq risques de Review Focus ont des tests identifiés dans les tâches.
- [x] Les helpers introduits et les interfaces entre tâches sont nommés.
- [x] Les exemples de code ont été confrontés aux signatures sources consultées.
- [ ] Revue du plan par l’utilisateur avant exécution dans un autre worktree.

## Transmission à l’agent d’exécution

Lire la spec puis ce plan, vérifier le worktree d’exécution et suivre les tâches
1 à 8. Le worktree d’origine sert au brainstorming. L’utilisateur a validé le
périmètre et la conception ; la revue de ce plan est le dernier préalable à son
exécution. Aucune migration, compilation GPU ou publication n’a été effectuée
pendant sa rédaction.
