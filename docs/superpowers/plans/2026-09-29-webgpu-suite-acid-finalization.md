# Graphiks WebGPU Suite — Plan 5 : finalisation des scénarios acid

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking. L’utilisateur transmet ce plan à un autre agent ; sa rédaction ne lance pas l’implémentation.

**Goal:** Finaliser un socle de validation portable avec 40 scénarios supplémentaires, des oracles résistants aux opérations ignorées et une disposition explicite de chaque lacune recensée : 123 cas prévus au total.

**Architecture:** Conserver les cinq modules, les cas annotés par fichier, le catalogue généré et les rapports JS/Wasm. Introduire un contexte d’exécution minimal pour les seuls scénarios qui doivent créer leur propre device, tout en conservant les fonctions existantes prenant `GPUDevice`. Les tests portent sur données, pixels, états, erreurs et résultats asynchrones du contrat commun.

**Tech Stack:** Kotlin KMP, API/descripteurs publics du dépôt, WGSL, `kotlin.test`, coroutines, génération Gradle existante, collecteur Playwright existant ; JDK 25 et Node.js 22 selon `docs/running.md`.

**Spec:** `docs/superpowers/specs/2026-09-27-webgpu-suite-design.md`, notamment §§ 5, 6, 11 et 12. Le présent plan prolonge le plan d’extension du 2026-09-28 à partir de son implémentation mergée.

## Global Constraints

- Même dépôt `Graphiks-org/webgpu`, conventions Gradle et politique de version commune (`releaseVersion`, défaut `0.1.0-SNAPSHOT`).
- API publique commune uniquement dans les cas ; aucun import `.browser`, DOM ou binding natif dans `suite-core`/`suite-acid-tests`.
- Un cas par fichier, une propriété observable nommée, opérations GPU explicites, attentes justifiées. Pas de framework de scénarios ni DSL de rendu.
- Aucun test d’architecture, de générateur, de structure HTML, de publication/résolution Maven ou projet consommateur artificiel. Compiler et lancer les vrais modules.
- Ne jamais convertir un défaut d’une fonctionnalité obligatoire en `unsupported`. Ce statut est réservé aux features déclarées et réellement absentes.
- Les résultats natifs appartiennent aux dépôts des bindings. Compiler une cible native ne signifie pas y avoir exécuté les cas GPU.
- Préserver IDs existants, rapports complets/ciblés séparés, parcours démos/benchmarks et ressources utilisateur locales.
- En cas de défaut du binding, reproduire avec le scénario public, diagnostiquer et isoler la correction ; ne pas modifier les attentes pour normaliser le défaut.
- Les sources API/descripteurs portent un avertissement de génération : une évolution du contrat doit passer par sa source de génération, dans un chantier identifié.
- La finalisation porte sur ce socle défini. Elle n’est ni une certification CTS, ni une couverture de toutes les combinaisons WebGPU.

## Review Focus

1. **Opération ignorée donnant le résultat initial** : sentinelles et contrôles contrastés dans les readbacks, queues et queries — tâches 1, 3, 6, 7.
2. **Mauvais canal d’erreur** : exception synchrone, `Result.failure`, error scope et callback sont vérifiés séparément — tâches 2, 3, 6.
3. **Création de devices et ownership** : adapter frais pour chaque demande, fermeture même après assertion, callback attendu isolé — tâche 2.
4. **État de rendu qui ne change aucun pixel inspecté** : témoins positifs/négatifs et pixels intérieurs distinguant les configurations — tâches 4, 5, 7.
5. **Couverture surestimée** : symboles appelés sans assertion, entrées trop larges, features jamais exécutées et API non exposée restent identifiables — tâche 8.

---

## 1. Baseline examinée et définition de « finaliser »

Inspection statique sur `a429925` — PR #126, après les PR #123, #124 et #125. Le catalogue contient **83 IDs** ; `inventory/uncovered-behaviours.json` contient **23 entrées**. Ce sont des entrées de granularité inégale, pas 23 tests qu’il suffirait de transcrire un pour un.

Le durcissement des oracles est déjà mergé : staging buffer prérempli dans `readBufferBytes`, contrôles de taille et assertions par composante. Les relire avant d’en ajouter. `readRgba8` ne possède pas encore le même préremplissage ni les mêmes contrôles explicites de longueur.

Trois fichiers ont des modifications locales au moment de la rédaction :

- `queries/Occlusion.kt` : retrait d’un contrôle de remplacement de sentinelle ;
- `textures/LinearMipSampling.kt` et `textures/SamplingSupport.kt` : remplacement d’une tolérance alpha par une tolérance commune aux canaux invariants.

Ce plan ne présume ni l’intention ni la validation de ces modifications. Aucun résultat GPU nouveau n’a été produit pour sa rédaction. Les sections historiques de `docs/verification.md` ne sont pas une campagne exécutée sur ce checkout.

### Résultat attendu

| Lot | Objet | Nouveaux cas |
| --- | --- | ---: |
| A | Capabilities, limites, labels et canaux d’erreur | 6 |
| B | Shaders, pipelines asynchrones et enregistrement | 8 |
| C | Aspects, vues, comparison sampler et texture 3D | 8 |
| D | Géométrie, faces, profondeur/stencil et biais | 8 |
| E | Cycle de vie, mapping et queue | 6 |
| F | Queries de rendu, MSAA et limites de passe | 4 |
| **Total** | **83 existants + 40 nouveaux** | **123** |

Deux nouveaux cas sont optionnels : `texture.view-swizzle` (`TextureComponentSwizzle`) et `query.render-timestamp-writes` (`TimestampQuery`). Avec les trois cas optionnels existants : **118 obligatoires, 5 optionnels**. Les 5 cas ne représentent pas cinq features différentes.

La livraison est finie quand les 118 cas obligatoires passent sur JS et Wasm, que les features présentes ont leurs cas exécutés, que les absences sont publiées comme telles et que les résidus de couverture sont documentés. Un cas optionnel absent partout est implémenté mais **pas validé en exécution** : l’écrire explicitement.

## 2. Carte des fichiers et interfaces

Préfixes exacts utilisés dans la suite du plan :

```text
CORE = suite-core/src/commonMain/kotlin/org/graphiks/webgpu/suite/
ACID = suite-acid-tests/src/commonMain/kotlin/org/graphiks/webgpu/suite/acid/
BROWSER = suite-browser/src/commonMain/kotlin/org/graphiks/webgpu/suite/browser/
```

- Créer `CORE/AcidContext.kt`, `CORE/AcidInput.kt` : contexte et choix typé d’argument du cas.
- Modifier `CORE/AcidCase.kt`, `CORE/AcidTest.kt`, `CORE/AcidCaseId.kt` : contrat d’exécution, métadonnées et IDs.
- Modifier `build-logic/src/main/kotlin/org/graphiks/webgpu/inventory/SuiteInventoryPlugin.kt` : adaptation des références aux fonctions selon leur argument déclaré.
- Modifier `BROWSER/RunFoundations.kt` : fournir le contexte, vérifier les features demandées, conserver isolation et garde des erreurs inattendues.
- Modifier `ACID/Readback.kt` : observations fiables pour les cas existants et nouveaux.
- Créer les **40 chemins exacts** donnés dans les tables des lots, sous `ACID/`. Fonction Kotlin = nom du fichier en lowerCamelCase.
- Modifier `inventory/i18n/behaviours.en.json`, `inventory/i18n/behaviours.fr.json`, `inventory/uncovered-behaviours.json` avec chaque lot, jamais les sorties générées.
- Modifier les guides `docs/adding-a-case.md`, `docs/running.md`, `docs/verification.md` et la spec au moment de la livraison ; créer `docs/acid-coverage.md` pour le bilan et les résidus.

Les cas prennent `device: GPUDevice` sauf quatre cas du lot A explicitement marqués **contexte**. Les familles utilisent `AcidFamily.packageName` existant : notamment `adapter`, `queue`, `shaders`, `pipelines` et `errors`. Les IDs sérialisés de la table sont normatifs ; pour le nom d’enum, préfixer le nom de fichier par sa famille (exemple `ShadersCompilationValid`).

### Cycle de validation par livraison

Un lot est une unité de livraison ; chaque ligne de sa table est une unité de travail : coder le cas, annoter/localiser, compiler, exécuter cet ID sur les deux cibles, puis passer au suivant. Une assertion rouge GPU n’est pas interchangeable avec une erreur de compilation.

```sh
rtk proxy ./gradlew :suite-browser:jsBrowserDistribution :suite-browser:wasmJsBrowserDistribution
rtk proxy node tools/run-browser.mjs js suite-browser/build/dist/js/productionExecutable --cases=shader.compilation-valid
rtk proxy node tools/run-browser.mjs wasm suite-browser/build/dist/wasmJs/productionExecutable --cases=shader.compilation-valid
```

Les options ciblées existent déjà. Remplacer l’ID par celui du cas travaillé ; une liste séparée par des virgules exécute le lot. Les rapports `selected-*` ne remplacent jamais les rapports complets.

## Task 1 — Stabiliser les observations du socle existant

**Files:** modifier `ACID/Readback.kt` ; examiner les trois fichiers locaux listés plus haut ; consigner dans `docs/verification.md`.

**Consumes:** `readBufferBytes`, `readRgba8`, `assertPixel`, `assertChannel` existants. **Produces:** mêmes signatures et mêmes attentes publiques, readback texturé non ambigu.

- [ ] **1. Établir la baseline.** Consigner commit et diff, puis compiler/lancer les 83 cas complets JS/Wasm. Conserver les défauts constatés avec leurs IDs. Ne pas attribuer au merge un résultat provenant de modifications locales non identifiées.
- [ ] **2. Réconcilier les modifications locales.** Examiner leur objectif avec les preuves disponibles. Pour l’occlusion, un u64 `0xffffffffffffffff` ne doit pas satisfaire à lui seul « visible > 0 ». Pour le sampling, une tolérance alpha justifiée ne justifie pas automatiquement d’élargir la tolérance verte. Préserver le travail local ; si son intention reste ambiguë, demander clarification avant de le modifier.
- [ ] **3. Durcir `readRgba8`.** Staging `mappedAtCreation=true`, initialisé à `0xa5`, puis unmap avant la copie. Vérifier taille mappée et tableau exactement égaux à `stride * height` avant retrait du padding. La copie CPU reste effectuée avant unmap.

```kotlin
val byteCount = (stride * height).toULong()
staging.getMappedRange().setBytes(0uL, ByteArray(byteCount.toInt()) { 0xA5.toByte() })
staging.unmap()
// Après copie, submit et mapAsync :
val mapped = staging.getMappedRange()
val padded = mapped.toByteArray()
assertEquals(byteCount, mapped.size, "Mapped texture readback length")
assertEquals(byteCount.toInt(), padded.size, "Texture readback byte count")
```

- [ ] **4. Vérifier l’effet sur de vrais cas.** Exécuter `render.clear-only`, `transfers.readback-offset-padding`, `queries.occlusion` et les cas de sampling concernés. Retirer temporairement la copie du helper : un cas de clear doit échouer sur `0xa5`, puis passer après restauration. Aucune mutation incorrecte dans le commit.
- [ ] **5. Livrer le socle.** Catalogue complet JS/Wasm après modifications partagées ; preuves distinguant merge et diff local. Commit proposé : `test: finalize acid readback and invariant checks`.

## Task 2 — Lot A : création et capabilities, six scénarios

**Files:** créer `CORE/AcidContext.kt`, `CORE/AcidInput.kt` ; modifier `AcidCase.kt`, `AcidTest.kt`, le plugin d’inventaire et `BROWSER/RunFoundations.kt` ; créer les six fichiers de la table ; mettre à jour IDs/i18n et `docs/running.md`.

### Interface minimale requise

Le contrat actuel `run(GPUDevice)` ne permet pas d’observer `requestDevice`, ses limites ou un callback choisi par le cas. Le nouveau contexte fournit une factory d’adapter commune, pas un service de fixtures.

```kotlin
// CORE/AcidContext.kt
package org.graphiks.webgpu.suite

import org.graphiks.webgpu.GPUAdapter
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPURequestAdapterOptions

class AcidContext(
    val device: GPUDevice,
    val requestAdapter: suspend (GPURequestAdapterOptions?) -> Result<GPUAdapter>,
)

// CORE/AcidInput.kt
package org.graphiks.webgpu.suite

enum class AcidInput { Device, Context }
```

- [ ] **1. Adapter le catalogue sans réécrire les 83 cas.** `AcidCase.run` devient `suspend (AcidContext) -> Unit`. Ajouter `val input: AcidInput = AcidInput.Device` à l’annotation, un champ correspondant dans `ParsedCase` et sa lecture dans `AcidTestParser`. Générer pour Device `run = { context -> functionName(context.device) }`, pour Context `run = ::functionName`. Rejeter une valeur d’annotation inconnue plutôt que retomber silencieusement sur Device. Le compilateur contrôle la signature effective.
- [ ] **2. Brancher le runner réel.** Garder son adapter/device par cas et les features déclarées ; juste après création, vérifier que toutes les features demandées appartiennent à `device.features`. Fournir :

```kotlin
val context = AcidContext(
    device = device,
    requestAdapter = { options -> requestAdapter(options).map { it } },
)
case.run(context)
```

La factory renvoie un **adapter frais à chaque appel**. Le contexte emprunte le device principal, jamais fermé par le cas. Le cas ferme tout adapter/device supplémentaire via `use`/`finally`. Le timeout global du cas et la propagation de cancellation restent actifs. Les quatre cas contexte créent un device supplémentaire au plus par adapter ; ce coût borné est préférable à une seconde infrastructure de runners.

- [ ] **3. Documenter la migration publique.** L’appel des consommateurs devient `case.run(AcidContext(device, requestAdapter))`. Mettre à jour l’exemple natif avec une factory fournie par le binding et des noms/types explicites ; ne pas fournir une factory factice qui échoue ou ignorer les cas Context. Ce changement de signature des artifacts de suite doit être signalé ; les fonctions individuelles existantes `caseFunction(device)` restent utilisables.

### Cas du lot A

| Fichier / ID | Scénario, assertion et contrôle contrasté |
| --- | --- |
| `adapter/Capabilities.kt` / `adapter.request-and-capabilities` | **Contexte.** Demander un adapter avec `RequestAdapterOptions(powerPreference=LowPower)`, capturer features/limites/info, demander un device sans feature optionnelle. Assert device.features inclus dans adapter.features ; limites choisies `maxTextureDimension2D`, `maxBindGroups`, `maxBufferSize` positives et ≤ celles de l’adapter, alignements uniform/storage du device ≥ ceux de l’adapter. Comparer les champs stables vendor/architecture/device/description entre adapter.info et device.adapterInfo ; chaînes vides autorisées. Exécuter un clearBuffer et lire zéro via staging sentinelle. Ne pas exiger un modèle GPU ni une sélection matérielle particulière à partir d’un hint. |
| `adapter/RequiredLimits.kt` / `device.required-limits` | **Contexte.** Sur adapter frais, demander `maxComputeWorkgroupSizeX=adapter.limits.maxComputeWorkgroupSizeX`, autres champs délégués aux limites de ce même adapter. Vérifier résultat et borne du device, puis exécuter un kernel workgroup_size(1) écrivant 37. Le cas de rejet suivant prouve que le champ n’est pas ignoré. Ne pas copier les limites d’un autre adapter, qui pourrait désigner un GPU différent. |
| `adapter/RejectedLimits.kt` / `device.reject-excess-limit` | **Contexte.** Même descripteur de limites de base, mais `maxComputeWorkgroupSizeX=adapter.limits.maxComputeWorkgroupSizeX+1`. Vérifier absence d’overflow, `requestDevice(...).isFailure`, sans création de ressource géante. Ne pas réutiliser cet adapter pour une deuxième demande. |
| `adapter/DeviceLabels.kt` / `device.queue-and-object-labels` | Sur le device prêté : buffer avec label `acid-étiquette-λ`, modifier en `renamed-λ`, relire ; refaire sur encoder puis command buffer. Sur une queue, sauvegarder le label, le changer et le restaurer dans finally. Clear/copie/readback pour prouver que les objets restent utilisables. Le label initial de device/defaultQueue est vérifié par les descripteurs des cas contexte, pas par modification du device prêté. |
| `errors/UncapturedCallback.kt` / `errors.uncaptured-error` | **Contexte.** Device dédié avec callback et aucun scope autour d’un unique `createBuffer` à usage None. Attendre un `GPUValidationError` via `CompletableDeferred<GPUError>` sous timeout de 5 s. Le message est diagnostique, pas un oracle. Fermer le buffer invalide et le device. Ne jamais désactiver la garde d’erreurs du device principal pour accepter cet événement. |
| `errors/ScopeFilterRouting.kt` / `errors.scope-filter-routing` | Scope externe Validation, interne OutOfMemory, créer un buffer usage None. Pop interne → succès null ; pop externe → `GPUValidationError`. Vérifier que l’ordre/filtres dirigent l’erreur ; aucune tentative réelle d’épuisement mémoire. |

Pour les limites, l’API prend un `GPUSupportedLimits`, pas une map de chaînes :

```kotlin
val requested = object : GPUSupportedLimits by adapter.limits {
    override val maxComputeWorkgroupSizeX = adapter.limits.maxComputeWorkgroupSizeX
}
val descriptor = DeviceDescriptor(
    requiredLimits = requested,
    label = "acid-device-λ",
    defaultQueue = QueueDescriptor(label = "acid-queue-λ"),
    onUncapturedError = GPUUncapturedErrorCallback { unexpected.complete(it) },
)
```

`unexpected` est un `CompletableDeferred<GPUError>` local au cas ; après `queue.onSubmittedWorkDone()` et le drainage de 50 ms utilisé par le runner actuel, il doit rester incomplet. Cela conserve la politique d’observation existante, sans prétendre que ce délai constitue une garantie universelle de livraison des callbacks. Dans `UncapturedCallback`, utiliser au contraire `received` et attendre sa complétion effective sous timeout, sans sleep servant d’oracle. Vérifier les labels device/queue du descripteur à la création. Le mapping actuel transmet tous les champs de limites, dont certains récents : si le navigateur de référence rejette un champ non exposé, diagnostiquer le binding et sa fidélité à la version de contrat ; ne pas contourner `requiredLimits` en le remplaçant par null.

- [ ] **4. Implémenter et exécuter les six cas** un par un, puis le catalogue complet après changement de contrat. Tester par mutation temporaire `requiredLimits=null` : le scénario `device.reject-excess-limit` doit devenir rouge.
- [ ] **5. Vérifier l’isolation** en exécutant `errors.uncaptured-error` suivi de `errors.empty-scope` dans la même campagne ; chacun a son résultat propre, aucune erreur attendue ne fuit dans l’autre.
- [ ] **6. Livrer.** Commit proposé : `test: validate adapter device requests and error delivery`.

## Task 3 — Lot B : compilation et commandes, huit scénarios

**Files:** créer les huit fichiers ci-dessous ; modifier enum/i18n/inventaire. **Consumes:** device fourni, `readBufferBytes`, `readRgba8`, scopes et shaders simples existants. **Produces:** assertions sur compilation, résultats des deux pipelines async et commandes réellement exécutées.

| Fichier / ID | Scénario et oracle |
| --- | --- |
| `shaders/CompilationValid.kt` / `shader.compilation-valid` | WGSL valide écrit 37 dans storage. `getCompilationInfo().getOrThrow()` ne contient aucun message Error ; warnings/info autorisés. Créer le pipeline et lire 37 pour que compilation silencieuse et non-fonctionnement ne soient pas confondus. |
| `shaders/CompilationInvalid.kt` / `shader.compilation-invalid` | WGSL contient `let value: u32 = ;` dans main. Scope Validation avant création ; compilation info contient au moins un Error ; pop retourne GPUValidationError. Ne pas figer texte, nombre, ordre, positions ou longueurs des diagnostics, qui sont dépendants de l’implémentation. Conserver ces champs pour le diagnostic. |
| `compute/AsyncPipeline.kt` / `compute.pipeline-async` | Shader u32 `out[i]=i*3+7`, quatre invocations. `createComputePipelineAsync(...).getOrThrow()`, bind/dispatch/readback → `[7,10,13,16]`. |
| `pipelines/AsyncRenderPipeline.kt` / `render.pipeline-async` | `createRenderPipelineAsync` du triangle plein écran, rouge sur clear bleu. Lire pixels intérieurs `[255,0,0,255]`. Le simple succès du Result ne suffit pas. |
| `errors/AsyncComputeRejection.kt` / `errors.async-compute-rejection` | Module valide, entryPoint du descripteur inexistant. `createComputePipelineAsync` → `Result.failure`, scope Validation → null ; pas de callback inattendu. Le contrat commun ne publie pas de type `GPUPipelineError` : ne pas inventer de cast vers ce type ni comparer un message browser. |
| `errors/AsyncRenderRejection.kt` / `errors.async-render-rejection` | Modules valides, cible couleur RGBA8 sampleCount1 mais pipeline multisample.count=3 invalide. Résultat async en échec, scope Validation vide, pas de callback inattendu. |
| `queue/OrderedCommandBuffers.kt` / `command.ordered-command-buffers` | Source u32=7, intermédiaire=99, sortie=123 ; CB1 copie source→intermédiaire, CB2 copie intermédiaire→sortie. Une queue.submit([CB1,CB2]), lire 7. Inverser temporairement la liste doit lire 99 et faire échouer le cas. |
| `queue/DebugMarkers.kt` / `command.debug-markers` | Groupes imbriqués équilibrés et insertDebugMarker avec labels Unicode sur encoder, passe compute et render bundle ; compute écrit 37, bundle peint rouge. Lire les deux résultats et scopes vides. Prouve l’usage valide et la conservation du travail, pas l’apparition d’un label dans un debugger externe. |

Exemple de distinction des erreurs de pipeline, avec module déjà créé et valide :

```kotlin
val descriptorWithMissingEntryPoint = ComputePipelineDescriptor(
    compute = ProgrammableStage(validModule, entryPoint = "missing"),
)
device.pushErrorScope(GPUErrorFilter.Validation)
try {
    val result = device.createComputePipelineAsync(descriptorWithMissingEntryPoint)
    assertTrue(result.isFailure, "Missing entry point must reject async creation")
} finally {
    assertNull(device.popErrorScope().getOrThrow(), "Async pipeline failure is not a scope error")
}
```

`validModule` provient de `ShaderModuleDescriptor(code="@compute @workgroup_size(1) fn main() {}")` et reste ouvert pendant cet appel ; le layout est auto (`null`), comme dans `ACID/compute/AutoLayoutConstants.kt`. Une exception avant le retour du Result est un défaut de l’adaptation asynchrone, pas un résultat accepté.

- [ ] **1. Implémenter chaque ligne**, avec annotation, titre et attente EN/FR ; fermer modules/pipelines/buffers obtenus même après assertion.
- [ ] **2. Exécuter les huit IDs JS/Wasm**, puis prouver la dépendance à l’ordre en inversant temporairement les command buffers et en restaurant.
- [ ] **3. Livrer** : `test: finalize shader async pipeline and command coverage`.

## Task 4 — Lot C : textures et aspects, huit scénarios

**Files:** créer les huit fichiers de la table ; modifier enum/i18n/inventaire. **Consumes:** readback texturé durci et helpers compute existants. **Produces:** distinction entre métadonnées, permissions des vues et données réellement exposées.

| Fichier / ID | Scénario et oracle |
| --- | --- |
| `textures/CreationMetadata.kt` / `texture.creation-metadata` | Texture 8×4×3 couches, mipCount3, RGBA8, usage TextureBinding|CopyDst|CopySrc. Vérifier width/height/depthOrArrayLayers/mipLevelCount/sampleCount=1/dimension/format et **ensemble** usage exact. Écrire vert dans mip1 couche2, relire ses 4×2 texels. Ajouter texture multisamplée count4, usage RenderAttachment, vérifier son getter count4 sans tenter de copie directe. |
| `textures/ViewUsageRestriction.kt` / `texture.view-usage-restriction` | Texture RGBA8 avec TextureBinding|RenderAttachment|CopySrc. Vue autorisée seulement TextureBinding : textureLoad valide d’un clear rouge préalable via une autre vue ; tentative de passe render avec la vue restreinte → GPUValidationError. Si usage est ignoré, cette deuxième attente devient rouge. |
| `textures/DepthAspectLoad.kt` / `texture.depth-aspect-load` | Depth24PlusStencil8 avec RenderAttachment|TextureBinding, clear depth0.25/stencil7. Vue DepthOnly, binding texture_depth_2d, textureLoad compute → float0.25, tolérance absolue 1e-6. Ne pas lire des octets du format opaque. |
| `transfers/DepthAspectCopy.kt` / `transfers.texture-copy-aspect` | Depth32Float 2×2 clear0.25, usage RenderAttachment|CopySrc. copyTextureToBuffer aspect DepthOnly, offset256, stride256, staging 1024 octets prérempli 0xa5. Lire les quatre floats 0.25 exactement ; vérifier toutes les plages hors texels copiés intactes. Depth32Float possède ici une représentation copiée définie ; ne pas généraliser à Depth24Plus. |
| `textures/ComparisonSampler.kt` / `sampler.comparison` | Depth32Float 2×2 clear0.5, sampler compare Less/nearest, `textureSampleCompareLevel` au centre avec référence0.25 puis0.75 → 1 puis0. Sortie storage préremplie 0xa5. Pas de PCF sur une frontière entre profondeurs. |
| `renderpasses/VolumeDepthSlice.kt` / `render.volume-depth-slice` | Texture ThreeD RGBA8 2×2×2 initialisée bleue, usage CopyDst|RenderAttachment|CopySrc. Vue ThreeD ; attachment depthSlice=1, clear rouge. Readback z0 bleu et z1 rouge. Ne pas fabriquer une vue TwoD d’une texture ThreeD. |
| `textures/ViewSwizzle.kt` / `texture.view-swizzle` | **Feature TextureComponentSwizzle.** RGBA8 source `[17,61,149,233]`, vue swizzle `b01r`, `textureLoad` puis round(*255) vers u32 → `[149,0,255,17]`. Deuxième vue `argb` → `[233,17,61,149]`. Ensemble les deux lectures exercent les six sources du swizzle. La vue identité sert de témoin `[17,61,149,233]`. |
| `textures/OneDimensionalLoad.kt` / `texture.one-dimensional-load` | Texture OneD RGBA8 width4, quatre couleurs distinctes. Upload une ligne, vue OneD, textureLoad(texture_1d, index,0) des quatre texels vers storage : matrice exacte. Vérifier textureDimensions=4. |

Shader du comparison sampler, output de deux floats :

```wgsl
@group(0) @binding(0) var image: texture_depth_2d;
@group(0) @binding(1) var compareSampler: sampler_comparison;
@group(0) @binding(2) var<storage, read_write> result: array<f32>;
@compute @workgroup_size(1) fn main() {
    result[0] = textureSampleCompareLevel(image, compareSampler, vec2f(0.5), 0.25);
    result[1] = textureSampleCompareLevel(image, compareSampler, vec2f(0.5), 0.75);
}
```

Les cas d’aspects encodent leurs propres copies ; ne pas élargir `readRgba8` en helper « tout format » ni lui faire interpréter la profondeur.

- [ ] **1. Implémenter et exécuter chaque scénario**, y compris le contrôle invalide de ViewUsageRestriction ; les scopes attendus ne doivent pas polluer le runner.
- [ ] **2. Vérifier le cas optionnel.** Sur feature absente : `unsupported` avec nom exact ; sur feature présente : les trois vues sont obligatoirement lues. Consigner le manque d’environnement si aucun ne permet de l’exécuter.
- [ ] **3. Exécuter le catalogue complet après lots A/B/C** : 105 IDs attendus, deux nouveaux passages par les limites de contrat décrites dans ce plan (création propre et feature swizzle).
- [ ] **4. Livrer** : `test: finalize texture aspect view and comparison sampling coverage`.

## Task 5 — Lot D : états de rendu, huit scénarios

**Files:** créer les huit fichiers ci-dessous ; modifier enum/i18n/inventaire. **Consumes:** cible offscreen 16×16, couleurs exactes, readback existant. **Produces:** paires de résultats dont la différence prouve l’effet de chaque état.

| Fichier / ID | Scénario et oracle |
| --- | --- |
| `pipelines/FrontFaceCulling.kt` / `render.front-face-culling` | Triangle CCW défini en clip space, cullMode Back : frontFace CCW → rouge, CW → fond bleu. Troisième cible cullMode Front/frontFace CCW → bleu. Inspecter le centre strictement intérieur ; utiliser la convention de face WebGPU, sans inférer le winding depuis un screenshot à axe Y inversé. |
| `pipelines/TriangleStripRestart.kt` / `render.triangle-strip-restart` | Deux rectangles disjoints x∈[-0.875,-0.25] et [0.25,0.875], y∈[-0.75,0.75]. Chaque strip ordonne bas-gauche, haut-gauche, bas-droite, haut-droite. Indices u16 `[0,1,2,3,65535,4,5,6,7]` (18 octets utiles ; allocation mappedAtCreation arrondie à20), TriangleStrip, stripIndexFormat Uint16, cull None, drawIndexed9. Pixels des deux rectangles rouges ; gap (7,10) noir, qui serait couvert par un triangle de liaison si restart était retiré. |
| `pipelines/FirstIndex.kt` / `render.first-index` | Indices u16 `[0,0,0,0,1,2]` sur triangle plein écran. Index buffer lié offset0 ; drawIndexed(indexCount3,firstIndex3) rouge. Le témoin firstIndex0 sur seconde cible reste bleu. Vérifie firstIndex et non setIndexBuffer.offset déjà couvert. |
| `pipelines/FirstInstance.kt` / `render.first-instance` | Draw direct vertexCount3,instanceCount1,firstInstance2. Shader vert uniquement pour instance_index=2, rouge sinon ; cible verte. Le firstInstance non nul direct ne requiert pas IndirectFirstInstance. |
| `pipelines/NegativeBaseVertex.kt` / `render.negative-base-vertex` | Vrais sommets aux indices0..2, sentinelles dégénérées aux indices3..5 ; indices `[3,4,5]`, baseVertex=-3 → triangle rouge. Buffer valide pour la variante baseVertex0, qui ne dessine rien sur une seconde cible. Détecte perte de signe de GPUSignedOffset32 sans faire d’accès hors limites. |
| `renderpasses/DepthReadOnly.kt` / `depth.read-only-attachment` | Passe1 écrit depth0.25, couleur rouge. Passe2 depthReadOnly=true, omet depthLoadOp/depthStoreOp, pipeline depthWriteEnabled=false/Less : vert z0.75 rejeté, bleu z0.1 accepté. Passe3 load profondeur, draw vert z0.2/Less : vert doit passer, prouvant que la passe2 n’a pas écrit 0.1. |
| `renderpasses/StencilFrontBack.kt` / `stencil.front-back-operations` | Deux petits triangles séparés, winding opposé, cull None ; stencil compare Always, front.passOp Replace/ref3, back.passOp IncrementClamp, clear0, couleur masquée. Deux lectures visuelles par comparaison Equal : ref3 peint seulement le triangle front en rouge, ref1 seulement le back en vert ; fond bleu. |
| `pipelines/DepthBiasSlopeClamp.kt` / `depth.bias-slope-clamp` | Depth32Float, plan de référence z=0.5+0.25*x_ndc, viewport16×16, Less. Recouvrir avec même plan moins0.0625 : slopeScale0 → rouge visible ; slopeScale4 (biais théorique0.125) → rejet ; slopeScale4,clamp0.015625 → rouge visible. Trois cibles, couleur témoin verte, pixels proches du centre. Les marges entre profondeurs sont bien plus grandes que l’arrondi. |

Pour le dernier cas, le vertex shader utilise le triangle plein écran habituel et calcule explicitement la profondeur :

```wgsl
let p = points[index];
return vec4f(p, 0.5 + 0.25 * p.x - offset, 1.0);
```

`offset` vaut 0 pour le draw de référence, 0.0625 pour les overlays (constante de shader ou override explicite). Pour width16, dz/dx écran=0.5/16 ; ne pas mesurer cette pente à partir du backend. `depthBias=0` dans ce cas pour isoler slopeScale/clamp. La tâche 7 couvre le biais constant.

- [ ] **1. Implémenter chaque état avec ses témoins**, valeurs numériques et pixels attendus visibles dans le fichier ; pas de snapshots calculés par le backend testé.
- [ ] **2. Exécuter les huit cas sur JS et Wasm.** Mutation temporaire du clamp à0 : la variante qui doit passer doit échouer. Restaurer avant livraison.
- [ ] **3. Livrer** : `test: finalize primitive face and depth stencil state coverage`.

## Task 6 — Lot E : cycle de vie et synchronisation, six scénarios

**Files:** créer les six fichiers ci-dessous ; modifier enum/i18n/inventaire. **Consumes:** API mapping/queue, helpers et catégories d’erreurs. **Produces:** assertions aux frontières définies, sans hypothèse sur l’ordonnancement des Promises.

| Fichier / ID | Scénario et oracle |
| --- | --- |
| `queue/SubmittedWorkDone.kt` / `queue.submitted-work-done` | Soumettre compute écrivant `[11,22,33,44]` puis copie vers staging déjà encodée, attendre `onSubmittedWorkDone().getOrThrow()`, mapper et lire les quatre valeurs. Prouve l’usage fonctionnel de cette attente ; le readback ne prouve pas à lui seul qu’une implémentation attendait au bon instant. Ne pas annoncer une validation temporelle exhaustive ni comparer l’ordre de résolution de deux Promises. |
| `errors/EncoderFinishedTwice.kt` / `errors.encoder-finished-twice` | Encoder valide, premier finish retourne CB utilisable qui copie7. Deuxième finish dans scope Validation → GPUValidationError ; fermer l’objet retourné s’il existe. Vérifier en dehors du scope que le premier CB produit toujours7. |
| `errors/CommandBufferResubmission.kt` / `errors.command-buffer-resubmission` | CB copiant7 soumis une fois, attendre achèvement et lire7. Deuxième submit du même CB dans scope → GPUValidationError. Ne pas reconstruire le CB entre les deux submissions. |
| `buffers/DisjointMappedWrites.kt` / `buffers.disjoint-mapped-writes` | Buffer32 octets mappedAtCreation, obtenir deux vues non chevauchantes offset0/size16 et offset16/size16, écrire respectivement `[1,2,3,4]` et `[5,6,7,8]`. Vérifier les tailles des vues, unmap, readback des huit u32 exacts. Ne jamais utiliser les vues après unmap. |
| `errors/OverlappingMappedRanges.kt` / `errors.overlapping-mapped-ranges` | Buffer32 mappedAtCreation ; getMappedRange(0,16), puis getMappedRange(8,16) : rejet **synchrone**. Une vue adjacente (16,16) reste valide, ses écritures sont lisibles après unmap. Le contrat ne fournit pas de type d’exception portable précis ; capturer l’échec autour de ce seul appel, scope Validation doit rester vide. |
| `errors/DestroyedTextureSubmission.kt` / `errors.destroyed-texture-submission` | Créer texture CopySrc|CopyDst, écrire rouge, encoder une copie valide vers staging et finish ; fermer la texture avant submit. La soumission dans scope Validation retourne une erreur capturée ; ne pas exiger une exception synchrone de close ou un readback défini après ce travail invalide. |

Différence entre exception synchrone et erreur GPU pour le chevauchement :

```kotlin
device.pushErrorScope(GPUErrorFilter.Validation)
try {
    buffer.getMappedRange(0uL, 16uL)
    assertFails("Overlapping CPU-visible ranges must be rejected") {
        buffer.getMappedRange(8uL, 16uL)
    }
    assertEquals(16uL, buffer.getMappedRange(16uL, 16uL).size)
} finally {
    assertNull(device.popErrorScope().getOrThrow())
}
```

Conserver la deuxième vue obtenue pour écrire ses sentinelles ; ne pas la demander une deuxième fois, ce qui créerait un nouveau chevauchement.

- [ ] **1. Implémenter et exécuter chaque cas.** Les valeurs Pending de mapState ne sont pas testées par polling ni sleep arbitraire : les états Mapped/Unmapped sont déjà vérifiés dans `buffers.partial-map-remap`.
- [ ] **2. Vérifier les frontières d’exception.** Une assertion portant sur tout le corps ne doit jamais accepter un échec de création valide comme preuve d’un chevauchement refusé. Une cancellation ne doit jamais devenir une réussite.
- [ ] **3. Livrer** : `test: finalize mapping resource lifetime and submission semantics`.

## Task 7 — Lot F : derniers états et queries, quatre scénarios

**Files:** créer les quatre fichiers de la table ; modifier enum/i18n/inventaire. **Consumes:** conventions de `ACID/queries/Timestamps.kt`, rendu offscreen, readbacks. **Produces:** oracles de résolution, biais constant, alpha-to-coverage et borne de draw count.

| Fichier / ID | Scénario et oracle |
| --- | --- |
| `queries/RenderTimestamps.kt` / `query.render-timestamp-writes` | **Feature TimestampQuery.** Passe dessinant rouge avec timestamps début/fin dans indices1 et2 d’un QuerySet count4. Resolve firstQuery1,count2,destinationOffset256 vers buffer512 prérempli0xff ; chaque u64 remplace sa sentinelle, end≥begin par comparaison high/low non signée, autres plages intactes. Vérifier aussi le rendu rouge. L’égalité est permise, aucun seuil de durée ni exigence d’un timestamp positif. |
| `pipelines/DepthBiasConstant.kt` / `depth.bias-constant` | Depth32Float, référence verte à z0.5, overlay rouge exactement coplanaire, Less : bias0 → vert ; bias=-1048576 → rouge ; bias=+1048576 → vert. slopeScale=0,clamp=0. Comparer couleurs, pas représentation mémoire ni amplitude exacte du biais flottant. |
| `renderpasses/MaxDrawCount.kt` / `render.max-draw-count` | Passe maxDrawCount=1 avec deux draws valides → GPUValidationError à la frontière d’encodage/finish prévue. Témoin maxDrawCount=2, mêmes commandes : rendu final vert. Ne pas limiter le test à un descripteur sans draw ni uniquement à un rejet. |
| `pipelines/AlphaToCoverageExtremes.kt` / `msaa.alpha-to-coverage-extremes` | MSAA4, alphaToCoverageEnabled=true, clear bleu opaque, fragment rouge : alpha0 → resolve `[0,0,255,255]`, alpha1 → resolve `[255,0,0,255]`. Témoin alphaToCoverageEnabled=false/alpha0 → `[255,0,0,0]`. Aucun seuil intermédiaire ni emplacement des samples imposé. Blend désactivé. |

Comparaison u64 réutilisable localement, sans passage par Double :

```kotlin
assertTrue(endHigh > beginHigh || (endHigh == beginHigh && endLow >= beginLow))
assertTrue(beginHigh != UInt.MAX_VALUE || beginLow != UInt.MAX_VALUE)
assertTrue(endHigh != UInt.MAX_VALUE || endLow != UInt.MAX_VALUE)
```

- [ ] **1. Implémenter et exécuter les quatre cas** ; ne partager avec Timestamps qu’un petit helper si la duplication gêne réellement la lecture. Les commandes et ranges résolus restent visibles dans chaque fichier.
- [ ] **2. Contrôle d’observabilité.** Retirer temporairement timestampWrites : le remplacement de sentinelles seul pourrait encore réussir si la résolution écrit zéro pour une query non écrite. Consigner cette limite si la mutation reste verte ; ne pas imposer arbitrairement end>begin ou >0 pour la masquer. Les oracles garantissent usage valide, résolution et conservation des ranges, pas l’existence d’un temps mesurable.
- [ ] **3. Livrer** : `test: finalize render queries draw limits and multisample states`.

## Task 8 — Fermer le bilan d’inventaire et publier les preuves

**Files:** modifier `inventory/uncovered-behaviours.json`, ressources EN/FR, `docs/verification.md`, `docs/running.md`, `docs/adding-a-case.md`, `docs/superpowers/specs/2026-09-27-webgpu-suite-design.md` ; créer `docs/acid-coverage.md` ; guides Validation/Tests EN/FR existants concernés par les anciens comptes.

### Disposition des 23 entrées actuelles

« Couvert » ci-dessous signifie uniquement que les assertions du scénario portent sur le comportement détaillé. Les résidus listés restent dans l’inventaire, sous des IDs plus précis si une entrée doit être scindée. Les motifs sont publiés dans les attentes EN/FR et le bilan ; aucun nouveau système de statuts d’exécution n’est nécessaire.

| Entrée actuelle | Traitement et résidu à conserver |
| --- | --- |
| `adapter.request-and-capabilities` | Cas A ; hint low-power et valeurs observables. Ne pas prétendre certifier une sélection high-performance, fallback forcé ou WebXR. |
| `device.features-and-limits` | Cas A + garde des features demandées sur chaque device, dont les cas optionnels existants. Associer seulement les limites effectivement vérifiées ; ne pas prétendre avoir testé toutes les bornes. |
| `device.request-required-features` | Relier aux cas optionnels qui demandent et utilisent leurs features et à la garde de leur présence ; limites/labels/defaultQueue/callback couverts par A. Garder explicitement refus d’une feature absente comme résidu si aucun cas déterministe ne l’exerce. |
| `command.encoder-recording` | B et E : ordre, finish, réutilisation invalide et labels. |
| `command.debug-markers` | B : scopes de debug valides autour de travail vérifié. Affichage debugger non observable par l’API commune. |
| `transfers.texture-copy-aspect` | C : copie depth32float DepthOnly. Garder copie StencilOnly et autres combinaisons non exercées. |
| `shader.compilation-info` | B : shader valide/invalide, catégorie et accès aux messages. Positions/textes exacts non portables. |
| `compute.pipeline-async` | B : résultat exécuté et rejet sans erreur capturée GPU. |
| `texture.creation` | C : getters et ressources utilisables ; garder `textureBindingViewDimension` et contraintes non exercées dans une entrée précise. |
| `texture.view-usage-aspect` | C : restriction d’usage et DepthOnly. Garder StencilOnly comme résidu. |
| `texture.view-swizzle` | C : cas optionnel GPU, pas seulement test de concaténation `toWebGpuString`. |
| `sampler.comparison` | C : comparaison Less à profondeur uniforme avec témoins 0/1. Garder PCF et autres compare functions non testées. |
| `texture.usage-and-formats` | Scinder : usages/formats réellement exercés liés à leurs cas ; formats compressés, formats tiers et TransientAttachment restent à traiter. Pas de test d’enum.entries ou de l’opérateur or pour gonfler la couverture. |
| `render.color-resolve` | Relier resolveTarget au cas MSAA existant, depthSlice au cas C. |
| `render.pass-state` | F : borne maxDrawCount validée avec témoin accepté. |
| `render.primitive-and-multisample` | D/F et cas existants ; garder points/lines, topologies ou masques partiels non exercés. |
| `render.depth-bias` | D/F : biais constant, pente et clamp contrastés, sans oracle de représentation opaque. |
| `render.pipeline-async` | B : rendu et rejet. |
| `query.render-timestamp-writes` | F : résolution observable, précision temporelle explicitement non prouvée. |
| `errors.uncaptured-error` | A : callback sur device isolé et catégorie Validation. |
| `errors.device-lost` | **Bloqué par le contrat** : pas d’accès `GPUDevice.lost` ni callback commun de perte ; les deux types seuls ne permettent pas de scénario. Laisser visible, ne pas fabriquer une promesse dans le runner. |
| `async.promise-results` | **Non déterministe pour les types qu’il référence actuellement** : GPUOutOfMemoryError/GPUInternalError ne se provoquent pas portablement. Renommer/scinder l’attente pour ne pas la confondre avec les Result de pipelines effectivement testés. Aucun stress OOM ni mock. |
| `data.identifiers-and-indices` | Ventiler les références utiles vers les cas offsets/regions/indices, dont baseVertex négatif. Ne pas ajouter de tests de typealias ni revendiquer toutes les valeurs 64 bits à partir de petits buffers. |

### Lacunes supplémentaires à nommer

L’ancien inventaire n’est pas un recensement exhaustif de comportements. Ajouter au bilan et aux entrées restantes celles découvertes dans la revue : `setImmediates`/`maxImmediateSize`, features compressées/tiers, subgroups, fonctionnalités récentes non exposées par le navigateur de référence, contraintes supplémentaires de storage textures, formats/profondeurs non exercés, discard suivi d’une réinitialisation, limites de sampling et validation négative des bundles. Les maintenir visibles avec justification ; ne pas cocher une famille entière parce qu’un membre possède un cas.

`setImmediates` a une signature publique mais pas de feature optionnelle dédiée dans le catalogue actuel. Une limite nulle ou un navigateur trop ancien ne peut pas être transformé artificiellement en `missingFeatures`. Son extension demande un chantier capabilities/contrat identifié ; elle n’est pas ajoutée silencieusement aux 123 cas de ce plan.

- [ ] **1. Réconcilier l’inventaire après chaque lot**, en déplaçant les textes d’un ID repris depuis `behaviours` vers `cases` selon les conventions du générateur et en conservant les résidus. Ne jamais laisser le même ID à la fois couvert et non couvert.
- [ ] **2. Créer `docs/acid-coverage.md`** avec la disposition ci-dessus actualisée, les 123 cas attendus, features requises, limites connues et liens vers les sources/preuves. Le nombre final d’entrées restantes est calculé après scission, pas un objectif à ramener à zéro.
- [ ] **3. Actualiser les textes historiques sans effacer les preuves.** La spec affirme encore que démos/benchmarks restent à réaliser et cite 11 cas ; ajouter une actualisation datée avec commits réellement intégrés, modules et nouveau contrat `AcidContext`. `docs/verification.md` conserve les campagnes historiques et reçoit une nouvelle campagne identifiée. Le plan 4 devient historique avec lien vers ses PR et le présent bilan.
- [ ] **4. Exécuter les modules réels pour la livraison**, sur hôtes compatibles :

```sh
rtk proxy ./gradlew :suite-core:compileKotlinJvm :suite-acid-tests:compileKotlinJvm :suite-browser:jsBrowserDistribution :suite-browser:wasmJsBrowserDistribution
rtk proxy node tools/run-browser.mjs js suite-browser/build/dist/js/productionExecutable
rtk proxy node tools/run-browser.mjs wasm suite-browser/build/dist/wasmJs/productionExecutable
rtk proxy ./gradlew :suite-core:compileKotlinMacosArm64 :suite-acid-tests:compileKotlinMacosArm64
rtk proxy ./gradlew :suite-core:compileKotlinLinuxX64 :suite-acid-tests:compileKotlinLinuxX64
rtk proxy node tools/build-site.mjs
```

Les deux commandes natives sont à distribuer aux hôtes qui peuvent réellement les compiler ; ne pas annoncer leur passage à partir de leur seule déclaration Gradle. Pour le site, réutiliser des rapports démos/benchmarks complets identifiés ; si les chemins partagés ont été modifiés, relancer `--demo-check` et `--benchmark --profile=ci` sur les deux distributions avant assemblage.

- [ ] **5. Inspecter le site EN/FR** : 123 IDs attendus, comptes passed/failed/unsupported/not-run distincts, aucune campagne ciblée publiée comme complète, liens des cas exacts, résidus visibles. Les cas optionnels jamais exécutés ne sont pas présentés comme une preuve de support.
- [ ] **6. Consigner les preuves** : commit et état du diff, commandes, navigateur/version/backend, features disponibles, nombre passé/non pris en charge par cible, mutations utiles et limites révélées. SwiftShader reste présenté comme backend logiciel ; `--backend=default` ne prouve pas un GPU matériel.
- [ ] **7. Relire et livrer** : `docs: publish finalized acid coverage and remaining contract gaps`. Ne déclarer le socle terminé que si ses cas obligatoires sont verts ; isoler clairement tout blocage de binding ou de contrat.

## Transmission et revue de ce plan

- [x] Baseline et fichiers locaux examinés ; 83 IDs et 23 entrées restantes constatés statiquement.
- [x] Chaque cas proposé a un chemin, un ID, des entrées et un résultat observable.
- [x] Dépendance à un contexte de création explicitée avec signatures, ownership et migration des consommateurs.
- [x] Erreurs synchrones/asynchrones, features, oracles et résidus distingués.
- [ ] Implémentation et preuves GPU : responsabilité de l’agent exécutant.
- [ ] Relecture du périmètre par l’utilisateur avant transmission.

**Ordre conseillé :** tâche 1, puis A/B/C, puis D/E/F, puis bilan final. Intégrer inventaire et textes avec chaque lot. Les durcissements d’oracles ou corrections du binding ont leur propre justification et ne se cachent pas dans un commit de nouveaux cas. Ne pas développer une infrastructure supplémentaire pour faire artificiellement disparaître les dernières entrées non couvertes.
