# Graphiks WebGPU Suite — Plan 4 : extension ambitieuse des acid tests

> **Document historique.** Ce plan a été exécuté et mergé par les PR #123, #124, #125 et #126, portant
> le catalogue de 11 à 83 cas. Il prolongé et clos par le plan de finalisation
> `2026-09-29-webgpu-suite-acid-finalization.md` (123 cas) ; voir `docs/acid-coverage.md` pour le
> bilan et `docs/verification.md` pour les preuves.

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking. Exécution par un autre agent dans ce même dépôt, à partir du code intégré. Ce plan est soumis à revue ; il ne certifie aucune nouvelle exécution GPU.

**Goal:** Ajouter 72 cas métier ciblés aux 11 cas existants, pour constituer un catalogue de 83 acid tests couvrant données, transferts, textures, sampling, rendu, blending, profondeur/stencil, MSAA, commandes avancées et erreurs.

**Architecture:** Conserver les modules, annotations typées, inventaire généré et ressources EN/FR existants. Un fichier par cas ; quelques helpers de readback et shaders simples rendent les attentes lisibles sans masquer les commandes GPU. Les cas restent portables et le runner navigateur exécute JS/Wasm sur GPU réel.

**Tech Stack:** Kotlin KMP et conventions du dépôt, `kotlin.test`, descripteurs publics, WGSL, coroutines et runner Playwright existants. Aucun nouveau framework, module ou générateur.

**Spec:** `docs/superpowers/specs/2026-09-27-webgpu-suite-design.md`, principes de solidité technique, couverture progressive et absence de tests d’architecture. Ce plan précise l’extension de couverture prévue par la spec, sans restructurer son architecture.

## Global Constraints

- Même dépôt et mêmes conventions ; tous les cas utilisent uniquement l’API publique commune.
- Lisibilité humaine : un cas nommé pour son comportement, entrées connues, commandes explicites, assertion justifiée.
- Aucun test de structure Gradle, de publication Maven, de générateur, de mocks GPU ou de schéma pour lui-même. Compilation et lancement des modules réels vérifient l’intégration.
- Un cas correspond à un comportement observable, pas simplement à un appel sans exception.
- Privilégier données exactes et petits rendus offscreen. Pas de comparaison globale d’images par score de similarité, pas de référence générée par le backend testé.
- Ne pas affaiblir une attente pour faire passer un binding défectueux. Documenter le cas et corriger la fidélité du binding dans un changement identifié, après diagnostic.
- Les fonctionnalités obligatoires ne sont jamais ignorées. Les trois cas optionnels déclarent précisément leurs features.
- Pas de dépendance vers `suite-demos` ou `suite-benchmarks`. Les helpers nécessaires aux tests appartiennent à `suite-acid-tests`.
- Conserver les IDs des onze cas existants, leurs résultats et les routes démos/benchmarks. Le nom historique `foundationCases()` peut rester ; aucun renommage cosmétique nécessaire.
- Les résultats natifs restent dans les dépôts des bindings. Ne pas présenter les résultats navigateur comme une validation native.
- Shell local avec `rtk`, workflows avec commandes natives.

## Review Focus

1. Confusion octets/texels, padding, origin et couches : données distinctes et sentinelles — lots A, B et C.
2. Référence visuelle circulaire ou rasterisation de bord instable : attentes analytiques, pixels intérieurs, tolérance localisée — lots D à H.
3. États masqués par un clear ou une passe réinitialisée : séquences de passes et couleurs distinctes prouvant l’effet — lots E, F, G et H.
4. Erreur attendue qui s’échappe, scope vide ou erreur tardive : vérifier catégorie et frontière de l’opération, pas le message — lot I.
5. Feature absente comptée comme réussite, feature présente mais défectueuse ignorée : manifeste et résultats explicites — tâche de runner et cas optionnels H8, I7, I8.

---

## Point de départ et ambition

Baseline examinée : `696bfb3` (benchmarks, PR #122), après `a0287f1` et `b4d750a`. La suite compte encore onze IDs dans `AcidCaseId`. `RunFoundations.kt` crée un adapter/device par cas, avec timeout de 30 secondes et vérification des erreurs non capturées. Le collecteur accepte seulement `passed` pour les acid tests, bien que le runner puisse émettre `unsupported`.

L’objectif est **72 nouveaux cas, dont 69 obligatoires et 3 conditionnés par une feature**, en neuf lots de huit. Avec l’existant : 80 obligatoires et 3 optionnels. Il s’agit d’un périmètre concret, pas d’une promesse de conformité WebGPU exhaustive.

Les familles suivantes restent explicitement hors de ce lot : formats compressés BC/ETC2/ASTC, external textures et sources vidéo, fuzzer WGSL, subgroups, ray tracing, nouvelles extensions propres aux bindings, certification W3C CTS, tests de perf et matrice multi-navigateurs. Les gaps restent visibles dans l’inventaire.

### Ordre des livraisons

1. **Vague 1 — données** : runner, helpers nécessaires, lots A/B/C, total 35 cas.
2. **Vague 2 — rendu couleur** : lots D/E/F, total 59 cas.
3. **Vague 3 — états et commandes avancées** : lots G/H/I, total 83 cas.

Chaque lot peut constituer une PR indépendante après son passage JS/Wasm. Ne pas repousser tous les diagnostics à une unique exécution finale.

## Préparation

- [ ] Lire la spec, ce plan, `docs/adding-a-case.md`, `docs/running.md` et les annotations présentes. Préserver les fichiers utilisateur non suivis/modifiés.
- [ ] Vérifier le commit, les versions et la baseline existante avec les commandes navigateur habituelles. Les comptes rendus précédents ne prouvent pas le passage de ce checkout.
- [ ] Lire les signatures de `writeTexture`, `copyTextureToBuffer`, `setBindGroup`, les descripteurs de profondeur/stencil, les bundles et les queries. Les champs non disponibles ne doivent pas être simulés dans la suite.
- [ ] Préparer une note de progression dans `docs/verification.md` indiquant les lots effectivement livrés et leurs résultats, sans cocher à l’avance les étapes GPU.

## Règles communes d’implémentation

Les chemins de cas sont relatifs à :

`suite-acid-tests/src/commonMain/kotlin/org/graphiks/webgpu/suite/acid/`

Le chemin indiqué dans chaque ligne des tables est exact. La fonction Kotlin est le nom du fichier en lowerCamelCase, par exemple `textures/BaseMipView.kt` → `baseMipView(device: GPUDevice)`. Ajouter une entrée à `AcidCaseId` nommée à partir de famille + nom du fichier, avec l’ID de rapport donné dans la table.

Chaque cas porte `@AcidTest(id = AcidCaseId.…, family = AcidFamily.…, contract = [ApiSymbols.…])`. Utiliser les constantes réellement générées ; ne pas entrer des chaînes libres pour contourner une référence absente. La famille doit correspondre au package : `buffers`, `transfers`, `bindgroups`, `compute`, `textures`, `pipelines`, `renderpasses`, `renderbundles`, `queries` ou `errors`.

Ajouter titre et attente dans `inventory/i18n/behaviours.en.json` et `.fr.json`. Lorsqu’une ancienne entrée de `inventory/uncovered-behaviours.json` est trop large, la scinder : déplacer uniquement le comportement effectivement testé et garder les variantes non couvertes. Ne pas retirer toute la famille « textures » parce qu’un format fonctionne.

### Attentes et ressources

- RGBA8/BGRA8 copiés : comparer les octets, sans conversion sRGB implicite.
- Calcul u32 : égalité exacte. Comparaison float issue d’une opération spécifiée : tolérance absolue explicitée par le cas.
- Clear primaire, write mask, rendu rouge/vert/bleu opaque : égalité exacte sur pixels intérieurs.
- Blending UNORM, sRGB et filtrage linéaire : tolérance **±1 octet par composante concernée**, donnée dans le cas ; jamais tolérance générale cachée.
- Rendu : cible 8×8 ou 16×16, sampleCount 1 par défaut. Choisir des triangles couvrant largement les pixels inspectés, éviter diagonales et bords de primitives.
- Les contrôles mémoire copient les tableaux avant `unmap`. Ne jamais relire une vue mappée après unmap.
- Les tests de profondeur/stencil observent un résultat couleur, pas la représentation mémoire d’un format opaque.
- Les scopes d’erreurs sont équilibrés ; ressources créées fermées via `use`/`finally`, même en cas d’assertion. Les passes utilisent `end()`, pas `use`.
- Ne pas vérifier des pixels provenant d’un attachment `Discard` comme si son contenu était préservé.

## Task 1 : permettre le développement ciblé et les cas optionnels

**Files:** `suite-browser/.../browser/RunFoundations.kt`, `Main.kt`, `Report.kt`, `tools/run-browser.mjs`, `.github/workflows/suite.yml`, `docs/running.md`.

- [ ] **1. Permettre un sous-ensemble explicite.** Signature compatible : `runFoundations(caseIds: Set<String>? = null)`. Paramètre URL `cases=id1,id2` et option CLI `--cases=id1,id2`, réservée au mode acid. Un ID inconnu ou une sélection vide explicitement fournie échoue. Sans filtre, tous les cas sont exécutés. Le collecteur déduit les attendus du manifeste généré puis applique la même sélection.
- [ ] **2. Séparer les sorties ciblées.** Une exécution ciblée écrit `build/reports/selected-<target>.json`, avec champ `selectedCaseIds`. Elle ne doit pas écraser `<target>.json` ni alimenter la page de couverture complète. Le build du site continue à consommer seulement les rapports complets.
- [ ] **3. Préciser les features absentes dans le rapport.** Ajouter à `CaseResult` un champ `missingFeatures: List<String> = emptyList()`. Lors du `missing = requiredFeatures - adapter.features`, sérialiser les **noms d’enum Kotlin** (`it.name`) pour correspondre au manifeste `cases.json`. Conserver le diagnostic lisible. Une feature disponible est demandée au device ; rejet ou échec du cas = `failed`.
- [ ] **4. Adapter uniquement la garde acid.** Lire `cases.json` avec le manifeste des IDs. Accepter `unsupported` seulement si le cas déclare des features, `missingFeatures` n’est pas vide et est inclus dans cette déclaration. Tous les cas sans feature doivent être `passed`. Les statuts `failed` et `not-run`, doublons, absences, erreurs de page ou fatales restent bloquants. Afficher séparément le nombre passé et non pris en charge, jamais « 83 passed » si des cas sont ignorés. Ne pas relâcher les modes démos/benchmarks.
- [ ] **5. Dimensionner les timeouts.** Conserver 30 s par cas. Le collecteur acid attend `expectedIds.length * 30_000 + 60_000` ms, borné à une heure. Configurer le job Suite avec un budget total de 120 minutes pour deux catalogues et les autres parcours ; aucune hausse du timeout ne transforme un cas bloqué en succès.
- [ ] **6. Vérifier par lancement.** Exécuter les onze cas, puis un cas ciblé. Confirmer que l’inventaire complet et ses rapports n’ont pas été remplacés par le sous-ensemble. Vérifier les chemins `unsupported` et présent→exécuté lors du lot I sur les environments disponibles ; consigner si aucune machine disponible n’expose une feature. Aucun test artificiel du collecteur.
- [ ] **7. Commiter** : `feat: run selected acid cases and report optional capabilities`.

## Task 2 : petits helpers de lecture et shaders de référence

**Files:** créer au besoin `Readback.kt`, `RenderSupport.kt` dans le package parent `suite.acid`, compléter `ValidationScope.kt` si nécessaire. Visibilité `internal` ; ni nouveau module ni DSL.

Interfaces :

```kotlin
internal suspend fun readBufferBytes(device: GPUDevice, buffer: GPUBuffer, size: ULong): ByteArray
internal suspend fun readRgba8(
    device: GPUDevice, texture: GPUTexture, width: Int, height: Int,
    mipLevel: UInt = 0u, origin: GPUOrigin3D = Origin3D(),
): ByteArray
internal fun assertPixel(
    pixels: ByteArray, width: Int, x: Int, y: Int,
    r: Int, g: Int, b: Int, a: Int, tolerance: Int = 0,
)
```

- [ ] **1. Lire un buffer.** Buffer staging `CopyDst | MapRead`, copie explicite de la taille, submit, `mapAsync().getOrThrow()`, `toByteArray()` puis unmap et fermeture. Le helper emprunte le buffer source et ne le ferme pas.
- [ ] **2. Lire RGBA8.** `rowBytes = width * 4`, `stride = ((rowBytes + 255) / 256) * 256`. Allouer `stride * height` octets, copier avec `TexelCopyBufferInfo(staging, bytesPerRow = stride.toUInt(), rowsPerImage = height.toUInt())`, retirer le padding CPU lors de la copie vers le tableau résultat. Rejeter les dimensions non positives. Ce helper est pour une couche/mip et un format 4 octets ; les cas qui testent le layout de copie encodent leur copie directement pour ne pas tester le helper à sa place.
- [ ] **3. Définir l’assertion par pixel.** Offset `(y * width + x) * 4`, conversion `byte.toInt() and 255`, différence absolue par canal. Le message donne position, canal, attendu et observé. Le paramètre tolerance par défaut est zéro.
- [ ] **4. Fournir un triangle plein écran simple.**

```wgsl
@vertex fn vertexMain(@builtin(vertex_index) i: u32) -> @builtin(position) vec4f {
    let points = array<vec2f,3>(vec2f(-1,-1), vec2f(3,-1), vec2f(-1,3));
    return vec4f(points[i], 0.5, 1.0);
}
@fragment fn fragmentMain() -> @location(0) vec4f { return vec4f(1,0,0,1); }
```

Les variantes de shader propres au comportement restent dans le fichier du cas. Un helper `createColorTarget(device, width, height)` peut fabriquer une texture RGBA8 `RenderAttachment | CopySrc`, mais pas exécuter tout le scénario de rendu à la place du cas.
- [ ] **5. Introduire les helpers avec leur premier cas consommateur.** Les valider en lisant de vraies données GPU, pas par un test de leur implémentation interne. Commiter avec le premier lot qui les utilise.

## Routine d’exécution de chaque lot

Pour chacun des lots A à I :

1. Ajouter les huit fonctions, IDs, annotations et textes EN/FR de la table.
2. Utiliser `withValidationScope` pour un scénario valide. Les erreurs attendues du lot I gèrent leurs propres scopes.
3. Compiler les deux distributions, exécuter le lot ciblé sur JS et Wasm, diagnostiquer les différences.
4. Compléter l’inventaire généré par les sources rédigées, sans éditer les sorties de build.
5. Exécuter le catalogue complet à la fin de chaque vague, les démos/benchmarks si leurs chemins partagés ont changé, puis mettre à jour les preuves et commiter le lot.

```sh
rtk proxy ./gradlew :suite-browser:jsBrowserDistribution :suite-browser:wasmJsBrowserDistribution
rtk proxy node tools/run-browser.mjs js suite-browser/build/dist/js/productionExecutable --cases=transfers.write-texture-tight-rows
rtk proxy node tools/run-browser.mjs wasm suite-browser/build/dist/wasmJs/productionExecutable --cases=transfers.write-texture-tight-rows
```

Remplacer la sélection par les huit IDs du lot, séparés par des virgules. Sans `--cases`, les commandes lancent l’intégralité du catalogue. Ne pas traiter une compilation rouge comme preuve d’un défaut GPU.

## Lot A — buffers, bindings et commandes compute : 8 cas

| Fichier / ID | Scénario et attente |
| --- | --- |
| `buffers/ZeroInitialized.kt` / `buffers.zero-initialized` | Créer 64 octets `CopySrc`, lire via staging : 64 zéros exacts. Aucun write CPU avant lecture. |
| `transfers/ClearBufferRange.kt` / `transfers.clear-buffer-range` | Buffer 32 octets rempli de huit u32 `0x11223344`. `clearBuffer(buffer, 8uL, 12uL)`, lecture : seuls les mots 2,3,4 sont zéro. |
| `transfers/CopyRemaining.kt` / `transfers.copy-remaining` | Source `[10,20,30,40]`, destination quatre mots zéro. `copyBufferToBuffer(source,8,destination,0)` sans size : `[30,40,0,0]`. |
| `buffers/MapWriteRoundTrip.kt` / `buffers.map-write-roundtrip` | Buffer `MapWrite | CopySrc`, map mode Write, écrire `[5,7,11,13]`, unmap, copier dans staging et lire exactement ces valeurs. |
| `bindgroups/BufferBindingRange.kt` / `bindings.buffer-range` | Uniform buffer : valeur 7 au début, 29 à l’offset aligné `minUniformBufferOffsetAlignment`. Bind explicite offset aligné/size 16, shader copie le u32 vers storage : 29, pas 7. |
| `bindgroups/DynamicUniformOffsets.kt` / `bindings.dynamic-uniform-offsets` | Layout uniform `hasDynamicOffset=true`, deux blocs 16 octets aux offsets 0 et `minUniformBufferOffsetAlignment`, valeurs 3 et 9. Deux passes avec offsets dynamiques respectifs, chacune copiée vers un staging distinct : 3 puis 9. |
| `compute/IndirectDispatch.kt` / `compute.indirect-dispatch` | Buffer indirect `[2,1,1]` (12 octets, `Indirect | CopyDst`), shader workgroup_size(1) écrit `id.x+10`. Output quatre u32 zéro : `[10,11,0,0]`. |
| `compute/OrderedPasses.kt` / `compute.ordered-passes` | Une soumission, deux passes : première écrit 7, seconde lit/modifie en `value*3+1`. Lire 22. Pas de synchronisation CPU entre passes. |

- [ ] Construire les bindings avec tailles/alignements lus sur le device, non codés à 256 par hypothèse.
- [ ] Écrire le shader uniform avec `struct Input { value: u32, pad0:u32, pad1:u32, pad2:u32 }`, storage output u32 et workgroup_size(1).
- [ ] Exécuter les huit cas JS/Wasm, puis commiter `test: expand buffer binding and compute command coverage`.

## Lot B — transferts de textures : 8 cas

Formats de base RGBA8Unorm, couleurs `[255,0,0,255]`, `[0,255,0,255]`, `[0,0,255,255]`, blanc. Les zones non écrites sont initialisées explicitement à noir opaque pour distinguer conservation et absence d’écriture.

| Fichier / ID | Scénario et attente |
| --- | --- |
| `transfers/WriteTextureTightRows.kt` / `transfers.write-texture-tight-rows` | Texture 3×2. `writeTexture` avec bytesPerRow=12, rowsPerImage=2 : ligne rouge puis verte. Lire les six texels exacts. La contrainte 256 ne s’applique pas à `writeTexture`. |
| `transfers/UploadPaddedRows.kt` / `transfers.upload-padded-rows` | Buffer 512 octets, première ligne rouge aux octets 0..11, seconde verte 256..267, padding `0x55`. `copyBufferToTexture` vers 3×2 : aucun padding dans les texels. |
| `transfers/ReadbackOffsetPadding.kt` / `transfers.readback-offset-padding` | Texture 3×2 connue. Destination staging de 1024 octets initialisée à `0x5a` via mappedAtCreation ; copy offset=256, stride=256. Vérifier texels aux offsets 256 et 512 et toutes les sentinelles hors plages copiées, sans utiliser le helper qui retire le padding. |
| `transfers/ArrayLayerStride.kt` / `transfers.array-layer-stride` | Texture 2×2×2 couches. Upload depuis buffer avec bytesPerRow=256, rowsPerImage=3 : couche rouge offset0, couche verte offset768. Lire chaque couche et vérifier ses quatre texels. |
| `transfers/WriteSubrectangle.kt` / `transfers.write-subrectangle` | Texture noire 4×4, `writeTexture` rouge 2×2 à origin(1,1,0). Comparer les 16 pixels à la matrice attendue, bord intact. |
| `transfers/TextureRegionCopy.kt` / `transfers.texture-region-copy` | Source 4×4 avec chaque coordonnée encodée R=32*x,G=32*y,B=0,A=255. Copier région origin(1,1) taille2×2 vers destination noire origin(0,2). Vérifier pixels copiés et reste noir. |
| `transfers/MipLevelCopy.kt` / `transfers.mip-level-copy` | Source 8×8, mipCount=3, mip2 (2×2) bleu, mip0 rouge. Copier uniquement mip2 vers texture2×2 : bleu partout. |
| `transfers/VolumeSlices.kt` / `transfers.volume-slices` | Texture ThreeD 2×2×2, upload slices rouge/verte avec layout explicite et copySize.depth=2. Lire z=0 et z=1 séparément : rouge et vert. |

- [ ] Encodage canonique du cas de lignes serrées :

```kotlin
device.queue.writeTexture(
    TexelCopyTextureInfo(texture = texture),
    ArrayBuffer.of(bytes),
    TexelCopyBufferLayout(offset = 0uL, bytesPerRow = 12u, rowsPerImage = 2u),
    Extent3D(3u, 2u, 1u),
)
```

- [ ] Les cas de padding inspectent les plages logiques et les sentinelles, pas seulement le premier pixel.
- [ ] Exécuter puis commiter `test: cover texture copy layouts regions mips and slices`.

## Lot C — vues, formats et storage textures : 8 cas

Utiliser un compute `textureLoad` et un output buffer pour les lectures de vues : pas de dépendance à un sampler. Pour rgba8unorm, convertir le résultat en u32 avec `round(channel*255.0)`.

| Fichier / ID | Scénario et attente |
| --- | --- |
| `textures/BaseMipView.kt` / `textures.view-base-mip` | Mip0 rouge, mip1 vert, mip2 bleu. Vue baseMipLevel=1,mipLevelCount=1 ; textureLoad(view,vec2i(0),0) donne vert. |
| `textures/ArrayLayerView.kt` / `textures.view-base-layer` | Texture 2D array trois couches rouge/vert/bleu. Vue TwoDArray baseArrayLayer=1,arrayLayerCount=2 ; lire index local0→vert,1→bleu. |
| `textures/ViewDimensions.kt` / `textures.view-dimensions` | Texture 8×4, mipCount=3, vue baseMip=1,count=2. Shader `textureDimensions(view,0)` → (4,2), niveau1 → (2,1), `textureNumLevels` →2. |
| `textures/UintLoad.kt` / `textures.uint-load` | Texture R32Uint 2×1, données `0x12345678` et `0xffffffff`; `texture_2d<u32>` + textureLoad vers storage : mots exacts, sans conversion float. |
| `textures/StorageWrite.kt` / `textures.storage-write` | Texture RGBA8Unorm 2×2 `StorageBinding | CopySrc`. Compute `texture_storage_2d<rgba8unorm,write>` écrit rouge, vert, bleu, blanc par coordonnée. Readback exact des quatre texels. |
| `textures/SrgbDecode.kt` / `textures.srgb-decode` | Texture RGBA8UnormSrgb, RGB=128,A=128. `textureLoad` vers buffer float : RGB≈0.2158605 (tolérance absolue 0.001 pour la conversion matérielle), alpha≈128/255 (tolérance 0.000001). |
| `textures/ReinterpretSrgbView.kt` / `textures.srgb-view-format` | Texture RGBA8Unorm déclarant viewFormats=[Rgba8UnormSrgb], RGB=128,A=128. Deux vues : lecture linéaire ≈0.5019608 (tolérance 0.000001), lecture sRGB≈0.2158605 (tolérance 0.001). Alpha identique à 0.000001 près. |
| `textures/CubeFaces.kt` / `textures.cube-faces` | Texture 2×2×6 couches, couleurs rouge/vert/bleu/jaune/magenta/cyan dans cet ordre, vue Cube. Compute `textureSampleLevel` aux axes +X,-X,+Y,-Y,+Z,-Z, sampler nearest : chaque axe donne la couleur de la couche correspondante. |

- [ ] Les bind groups de sampling n’utilisent pas le layout d’une storage texture ; chaque cas montre les ressources qu’il emploie.
- [ ] Shader de storage write : `@group(0) @binding(0) var image: texture_storage_2d<rgba8unorm, write>;` et `textureStore(image, vec2i(id.xy), color)` avec workgroup_size(1), dispatch2×2.
- [ ] Exécuter le catalogue complet de la vague 1 : 35 cas. Commiter `test: cover texture views formats and storage writes`.

## Lot D — sampling : 8 cas

Texture de base 2×1 : texel0 rouge, texel1 bleu, alpha1. Lire avec compute `textureSampleLevel`, output float4 puis comparaison ; u et v explicitement passés dans le shader. Les shaders restent dans chaque fichier, pas de fabrique de chaînes remplaçant des tokens.

| Fichier / ID | Scénario et attente |
| --- | --- |
| `textures/NearestSampling.kt` / `sampling.nearest` | nearest, positions (0.25,0.5) et (0.75,0.5) : rouge puis bleu exact. |
| `textures/LinearSampling.kt` / `sampling.linear` | min/mag linear, position (0.5,0.5), LOD0 : (0.5,0,0.5,1), tolérance 1/255 sur RGB. |
| `textures/ClampSampling.kt` / `sampling.clamp` | addressModeU ClampToEdge ; u=-0.25 et 1.25 : rouge puis bleu. |
| `textures/RepeatSampling.kt` / `sampling.repeat` | Repeat ; u=1.25 et -0.25 : rouge puis bleu. |
| `textures/MirrorSampling.kt` / `sampling.mirror-repeat` | MirrorRepeat ; u=1.25 et -0.25 : bleu puis rouge. |
| `textures/ExplicitMipSampling.kt` / `sampling.explicit-mip` | Mips uniformes rouge/vert/bleu, sampler nearest+mipmapNearest, sampleLevel 0/1/2 → rouge/vert/bleu. |
| `textures/LinearMipSampling.kt` / `sampling.linear-mip` | Mip0 rouge, mip1 bleu, mipmapFilter Linear, LOD0.5 → violet (0.5,0,0.5,1), ±1/255. |
| `textures/LodClampSampling.kt` / `sampling.lod-clamp` | Trois mips rouge/vert/bleu ; lodMinClamp=lodMaxClamp=1 ; demander LOD0 et2 : vert dans les deux cas. |

- [ ] Écrire un shader explicite `texture_2d<f32>` + sampler + output storage, et renseigner toutes les options de filtre pertinentes au lieu de dépendre de valeurs par défaut cachées.
- [ ] Protéger les assertions contre NaN : vérifier `actual.isFinite()` avant une comparaison à tolérance.
- [ ] Exécuter puis commiter `test: validate sampler filters addressing and mip selection`.

## Lot E — géométrie et commandes de rendu : 8 cas

Utiliser cible 16×16 noire, couleur rouge opaque. Les régions observées sont loin des bords ; vérifier aussi au moins un pixel extérieur pour les dessins partiels.

| Fichier / ID | Scénario et attente |
| --- | --- |
| `pipelines/VertexBufferOffset.kt` / `render.vertex-buffer-offset` | Buffer avec trois vec2 de sentinelle dégénérée puis triangle plein écran ; `setVertexBuffer` offset=24,size=24, draw3 : centre rouge. Offset zéro aurait laissé noir. |
| `pipelines/IndexedUint16.kt` / `render.indexed-u16` | Quatre sommets d’un carré centré ±0.5, index u16 `[0,1,2,2,1,3]`, drawIndexed6 : quatre pixels intérieurs rouges, coins noirs. |
| `pipelines/IndexedUint32.kt` / `render.indexed-u32-offset` | Indices u32 avec préfixe trois indices dégénérés puis six indices du carré ; `setIndexBuffer` offset12,size24 : même carré. |
| `pipelines/FirstVertex.kt` / `render.first-vertex` | Vertex buffer : trois sentinelles puis triangle plein écran ; draw(vertexCount=3,firstVertex=3) : rouge. |
| `pipelines/BaseVertex.kt` / `render.base-vertex` | Vertex buffer contient trois sentinelles puis vrai triangle ; indices `[0,1,2]`, drawIndexed3 avec baseVertex=3 : rouge. |
| `pipelines/InstancedAttributes.kt` / `render.instanced-attributes` | Deux instances, buffer d’offset/couleur en stepMode Instance : petit carré gauche rouge, droite vert. Inspecter (4,8), (12,8), et fond (8,1). |
| `renderpasses/Viewport.kt` / `render.viewport` | Triangle plein écran avec viewport x=4,y=4,width=8,height=8,minDepth0,maxDepth1 : intérieur rouge, pixels (1,1),(14,14) noirs. |
| `renderpasses/Scissor.kt` / `render.scissor` | Triangle plein écran, scissor x=4,y=6,width=8,height=4 : pixels (5,7),(10,8) rouges, (5,4),(5,12) noirs. |

- [ ] Définir positions/indices directement en Kotlin ; les offsets sont en octets tandis que `firstVertex`/`firstIndex` sont des indices.
- [ ] Exemple de layout instancié : stride24 pour `vec2 offset` puis `vec4 color`, attributs Float32x2 offset0 et Float32x4 offset8, stepMode Instance. Le vertex shader applique l’offset au carré.
- [ ] Exécuter puis commiter `test: cover vertex indexed instanced and raster region commands`.

## Lot F — attachments, couleurs et blending : 8 cas

| Fichier / ID | Scénario et attente |
| --- | --- |
| `renderpasses/ClearOnly.kt` / `render.clear-only` | Passe sans draw avec clear rouge, Store : tous les pixels `[255,0,0,255]`. |
| `renderpasses/LoadPreserves.kt` / `render.load-preserves` | Première passe clear bleu Store ; seconde Load Store avec draw rouge limité par scissor. Région rouge, reste bleu. |
| `pipelines/ColorWriteMask.kt` / `render.color-write-mask` | Clear `(0,1,0,1)`, shader rouge alpha0, writeMask Red seulement : `[255,255,0,255]`. |
| `pipelines/AlphaBlend.kt` / `blend.source-alpha` | Clear bleu alpha1, fragment rouge alpha0.5. Color SrcAlpha/OneMinusSrcAlpha ; alpha One/OneMinusSrcAlpha : `[128,0,128,255]` ±1 sur R/B. |
| `pipelines/AdditiveBlend.kt` / `blend.additive` | Clear `(0.25,0,0,1)`, source `(0.25,0.5,0,1)`, color One/One Add ; alpha One/Zero : `[128,128,0,255]` ±1 sur R/G. |
| `pipelines/BlendConstant.kt` / `blend.constant` | Clear bleu, source rouge. color Constant/OneMinusConstant et blendConstant `(0.25,0.25,0.25,1)`, alpha One/Zero : `[64,0,191,255]` ±1 sur R/B. |
| `pipelines/MultipleColorTargets.kt` / `render.multiple-targets` | Deux attachments RGBA8, fragment écrit location0 rouge et location1 vert. Lire séparément chaque texture : couleurs exactes. |
| `textures/SrgbEncode.kt` / `render.srgb-encode` | Attachment RGBA8UnormSrgb, fragment RGB=0.5,A=0.5 : readback RGB≈188 et alpha≈128, ±1. |

- [ ] Implémenter le blend avec `BlendState(color = BlendComponent(...), alpha = BlendComponent(...))`, jamais par défaut implicite sur alpha.
- [ ] Exemple d’attente du cas source-alpha :

```kotlin
assertPixel(pixels, 8, 4, 4, r = 128, g = 0, b = 128, a = 255, tolerance = 1)
```

La table impose ±1 seulement là où la quantification l’exige : si le helper applique cette tolérance à tous les canaux, ajouter les assertions exactes G=0 et A=255 séparément.
- [ ] Exécuter tout le catalogue de la vague 2 : 59 cas. Commiter `test: validate attachment persistence blending and color conversion`.

## Lot G — profondeur et stencil : 8 cas

Utiliser `Depth24PlusStencil8` pour les cas stencil et `Depth32Float` pour les cas pure profondeur. Les clears de profondeur/stencil sont explicites. Chaque draw utilise un triangle plein écran, sa couleur et sa profondeur constantes, sauf région scissor mentionnée.

| Fichier / ID | Scénario et attente |
| --- | --- |
| `pipelines/DepthLess.kt` / `depth.less` | Clear depth1. Draw rouge z0.25 puis vert z0.75, compare Less/write=true : rouge. |
| `pipelines/DepthGreater.kt` / `depth.greater` | Clear depth0. Draw rouge z0.75 puis vert z0.25, Greater/write=true : rouge. |
| `pipelines/DepthWriteDisabled.kt` / `depth.write-disabled` | Clear1. Rouge z0.25 Less/write=false puis vert z0.75 Less/write=true : vert, prouvant que le premier draw n’a pas écrit la profondeur. |
| `renderpasses/DepthLoad.kt` / `depth.load-preserves` | Passe1 rouge z0.25, profondeur Store ; passe2 depthLoad=Load avec vert z0.75 : couleur rouge conservée. |
| `renderpasses/StencilReplace.kt` / `stencil.replace-equal` | Clear stencil0 ; scissor gauche, colorWriteMask None, stencil Always/Replace ref3. Ensuite fullscreen Equal ref3 vert : gauche verte, droite noire. |
| `renderpasses/StencilReadMask.kt` / `stencil.read-mask` | Clear stencil0xA5. Pipeline Equal, readMask0x0F, reference0x15 : vert passe. Deuxième draw rouge avec ref0x16 : échoue, reste vert. |
| `renderpasses/StencilWriteMask.kt` / `stencil.write-mask` | Clear0xA0 ; Replace ref0x05, writeMask0x0F, couleur masquée. Puis Equal ref0xA5/full mask : vert. Comparaison ref0x05 doit échouer lors d’un draw rouge supplémentaire. |
| `renderpasses/StencilDepthFail.kt` / `stencil.depth-fail` | Depth clear0, compare Less avec fragment z0.5 échoue ; stencil Always et depthFailOp Replace ref7, couleur masquée. Ensuite depth Always, stencil Equal ref7 : vert. |

- [ ] Construire les deux faces stencil explicitement identiques afin de ne pas dépendre du winding du triangle.
- [ ] Pour un attachment depth-stencil, définir les load/store de chaque aspect utilisé ; ne pas confondre readOnly et writeMask. Si aspect en lecture seule, omettre les opérations de chargement/stockage exigées absentes par le contrat ; aucun cas de ce lot ne nécessite ce chemin.
- [ ] Exécuter puis commiter `test: validate depth persistence and stencil operations`.

## Lot H — MSAA, bundles, indirect et queries : 8 cas

| Fichier / ID | Scénario et attente |
| --- | --- |
| `renderpasses/MultisampleResolve.kt` / `msaa.resolve` | Texture RGBA8 sampleCount4 avec resolveTarget sampleCount1. Pipeline count4, triangle plein écran rouge : pixels intérieurs rouges après resolve. Copier uniquement la texture résolue. |
| `pipelines/MultisampleMask.kt` / `msaa.sample-mask-zero` | Même configuration, clear bleu, pipeline multisample mask=0 : aucun sample du draw rouge n’écrit, resolve bleu. |
| `renderbundles/BundleDraw.kt` / `bundles.draw` | Bundle compatible RGBA8 qui fixe pipeline/buffers et draw un carré rouge ; executeBundles : carré rouge et fond noir. |
| `renderbundles/BundleReuse.kt` / `bundles.reuse` | Exécuter le même bundle dans deux passes sur deux textures compatibles initialisées différemment. Carré rouge dans les deux, fonds respectifs bleu/vert intacts. |
| `pipelines/DrawIndirect.kt` / `render.draw-indirect` | Buffer args `[3,1,0,0]`, usage Indirect ; triangle plein écran rouge. Ajouter dans le même cas une exécution avec vertexCount0 sur une seconde cible : celle-ci reste noire. |
| `pipelines/DrawIndexedIndirect.kt` / `render.draw-indexed-indirect` | Buffer args `[6,1,0,0,0]`, indices du carré, drawIndexedIndirect : carré rouge, fond noir. |
| `queries/Occlusion.kt` / `queries.occlusion` | Deux queries dans une passe : triangle visible puis triangle rejeté par depth. Resolve dans buffer QueryResolve|CopySrc, lire deux u64 via paires de u32 : première non nulle, seconde zéro. Pas de nombre précis d’échantillons attendu. |
| `pipelines/IndirectFirstInstance.kt` / `render.indirect-first-instance` | **Feature `IndirectFirstInstance`**. args draw `[3,1,0,1]`, shader choisit vert si instance_index=1, rouge sinon. Attendre vert. |

- [ ] Encoder les bundles via `RenderBundleEncoderDescriptor(colorFormats=listOf(Rgba8Unorm))`; ils fixent leur propre état. Ne pas supposer l’héritage des bindings de la passe.
- [ ] Occlusion : `QuerySetDescriptor(GPUQueryType.Occlusion, 2u)`, attachment de profondeur clear1, compare Less/write=true. Query0 entoure le draw à z0.25, query1 celui à z0.75 ; les deux triangles couvrent la cible. Définir `RenderPassDescriptor.occlusionQuerySet`, équilibrer begin/end pour chaque index. `resolveQuerySet` destinationOffset=0 (aligné à 256) vers 16 octets, puis copie dans staging `MapRead`. Pour le nombre visible, vérifier `(low != 0u || high != 0u)` ; éviter Double et perte de précision 64 bits.
- [ ] Exécuter puis commiter `test: cover multisampling bundles indirect draws and occlusion`.

## Lot I — erreurs ciblées et deux features : 8 cas

| Fichier / ID | Scénario et attente |
| --- | --- |
| `errors/TextureCopyRowAlignment.kt` / `errors.texture-copy-row-alignment` | `copyBufferToTexture` 3×2 avec bytesPerRow12 (au lieu de multiple256), buffer suffisamment grand. Encoder, finir/soumettre si nécessaire, scope retourne `GPUValidationError`. Le lot B prouve que le même stride est valide pour writeTexture. |
| `errors/TextureCopyBounds.kt` / `errors.texture-copy-bounds` | Texture4×4, origin(3,3), copySize2×2, tous les autres paramètres valides : `GPUValidationError`. |
| `errors/MissingCopyUsage.kt` / `errors.missing-copy-usage` | Source texture seulement TextureBinding, tenter copie vers buffer avec layout valide : `GPUValidationError`. |
| `errors/DynamicOffsetAlignment.kt` / `errors.dynamic-offset-alignment` | Layout uniform dynamique correct, buffer assez grand, offset1 non aligné, dispatch réellement encodé : `GPUValidationError`. |
| `errors/BindGroupLayoutMismatch.kt` / `errors.bindgroup-layout-mismatch` | Bind group avec binding0 uniform utilisé sur pipeline attendant binding0 storage ; tailles/usages par ailleurs valides. Dispatch puis scope : `GPUValidationError`. |
| `errors/NestedScopes.kt` / `errors.nested-scopes` | Deux scopes Validation imbriqués, buffer usage None invalide. Pop interne → GPUValidationError, pop externe → succès null. |
| `queries/Timestamps.kt` / `queries.timestamp-resolve` | **Feature `TimestampQuery`**. QuerySet timestamp2, `ComputePassTimestampWrites(beginningOfPassWriteIndex=0,endOfPassWriteIndex=1)`, petit calcul réel. Resolve deux u64 ; fin ≥ début en comparaison high/low. L’égalité est permise ; jamais convertir en performance ni exiger >0. |
| `compute/HalfPrecision.kt` / `compute.shader-f16` | **Feature `ShaderF16`**. WGSL `enable f16;` calcule `f32(f16(1.5) + f16(2.0))`, écrit float dans storage : 3.5 exact. |

- [ ] Pour une erreur encodée, poser le scope avant création/encodage, terminer/soumettre le travail requis, puis vérifier le résultat. Ne pas accepter n’importe quelle exception comme erreur WebGPU attendue.

```kotlin
device.createBuffer(BufferDescriptor(512uL, GPUBufferUsage.CopySrc)).use { source ->
    device.createTexture(TextureDescriptor(
        size = Extent3D(3u, 2u, 1u),
        format = GPUTextureFormat.Rgba8Unorm,
        usage = GPUTextureUsage.CopyDst,
    )).use { target ->
        device.pushErrorScope(GPUErrorFilter.Validation)
        try {
            device.createCommandEncoder().use { encoder ->
                encoder.copyBufferToTexture(
                    TexelCopyBufferInfo(buffer = source, bytesPerRow = 12u, rowsPerImage = 2u),
                    TexelCopyTextureInfo(texture = target),
                    Extent3D(3u, 2u, 1u),
                )
                encoder.finish().use { device.queue.submit(listOf(it)) }
            }
        } finally {
            val result = device.popErrorScope()
            assertTrue(result.isSuccess)
            assertIs<GPUValidationError>(result.getOrThrow())
        }
    }
}
```

Cet exemple implémente le cas d’alignement des lignes. Conserver de même l’opération invalide visible dans chaque autre fichier.
- [ ] Timestamps : `requiredFeatures = [GPUFeatureName.TimestampQuery]` dans l’annotation ; demander la feature uniquement pour ce cas. Validation des valeurs lues en paires u32 ; un rejet de résolution est un échec, pas `unsupported`. Ajouter un contrôle de plage : destination de 512 octets préremplie à `0xff`, résolution des deux queries à l’offset256 ; les 16 octets écrits doivent remplacer les sentinelles et toutes les autres plages rester intactes. Ce cas valide l’usage et la résolution des queries, pas la précision de l’horloge ni la garantie d’une durée positive ; des timestamps égaux ne prouvent pas que les deux écritures ont des instants distincts.
- [ ] Ajouter ShaderF16 et IndirectFirstInstance de la même façon. Si une feature manque, vérifier qu’elle apparaît comme non prise en charge dans le rapport et le site, sans augmenter le compte de réussites.
- [ ] Exécuter tout le catalogue de la vague 3 : 83 cas recensés. Commiter `test: validate targeted failures and optional WebGPU features`.

## Finalisation de chaque vague et livraison globale

**Files:** `inventory/uncovered-behaviours.json`, les deux ressources i18n, `docs/verification.md`, `docs/adding-a-case.md`, `docs/running.md`, guides Suite/Tests EN/FR ; adaptation ciblée du site si un compteur suppose que tout cas présent est passé.

- [ ] **1. Lire le rapport complet JS puis Wasm.** Vérifier nombres recensés/exécutés/passés/non pris en charge/échoués séparément. Les trois features peuvent être disponibles ou non selon l’environnement : ne pas écrire à l’avance « 83 passent ».
- [ ] **2. Observer une détection réelle de régression par vague.** Modifier temporairement un élément GPU, sans changer l’attente : stride du transfert pour vague1, couleur/option blend pour vague2, compare stencil pour vague3. Exécuter le cas, constater le défaut, restaurer et constater le passage. Ces manipulations vérifient le comportement WebGPU, pas le générateur. Aucun changement volontairement incorrect ne doit être commité.
- [ ] **3. Régénérer et inspecter l’inventaire.** Les lignes couvertes pointent vers les cas exacts. Les fonctionnalités non exercées restent à tester. Les textes FR/EN expliquent la propriété observée et ses limites. Le nombre de symboles n’est pas un pourcentage de conformité.
- [ ] **4. Construire le site avec les rapports complets.** Vérifier affichage des erreurs et des features absentes, source des cas et résultats séparés par cible. Ne pas publier les rapports `selected-*` comme campagne complète.
- [ ] **5. Compiler les artifacts partagés** pour JVM/JS/Wasm et les cibles natives sur hôtes compatibles, via leurs tâches existantes. Aucune application native de vérification ajoutée.
- [ ] **6. Consigner les preuves** : commit, cas réellement exécutés, Chromium/backend, features disponibles/absentes, résultats et commandes, défauts du binding rencontrés et résolution. Une correction de binding doit être décrite séparément de l’ajout du test qui l’a révélée.
- [ ] **7. Vérifier le diff documentaire et les commandes réellement passées** puis livrer le compte rendu. Ne pas déclarer une vague finie si ses cas obligatoires sont encore rouges ; conserver l’avancement déjà livré et expliciter le blocage.

## Revue du plan et transmission

| Objectif | Traitement |
| --- | --- |
| Extension ambitieuse et graduelle | 72 nouveaux cas, 9 lots, 3 vagues |
| Données et calcul sans rendu | Lot A |
| Transferts, vues, formats, storage | Lots B/C |
| Samplers et mipmaps | Lot D |
| Géométrie, attachments, blending | Lots E/F |
| Profondeur et stencil | Lot G |
| MSAA, bundles, indirect, queries | Lot H |
| Erreurs précises et features optionnelles | Lot I et adaptation du runner |
| Lisibilité et aucun test d’architecture | Règles communes et cas concrets |
| Traçabilité et publication des résultats | Inventaire généré, i18n, rapports complets |

- [x] Comptage : neuf lots de huit = 72 ; avec les onze existants = 83.
- [x] Trois cas optionnels identifiés, sans convertir leurs absences en réussites.
- [x] Chaque nouveau cas possède un chemin, un ID, des entrées et une attente observable.
- [x] Le plan préserve modules, conventions, générateur et styles intégrés.
- [ ] Revue du périmètre par l’utilisateur avant transmission pour exécution.

**Transmission :** ce plan remplace une approche limitée à quelques textures par une extension substantielle, mais conserve les PR/lots lisibles. Exécuter dans l’ordre des vagues ; une nouvelle famille ne justifie pas un framework. Le code doit permettre à un humain de comprendre pourquoi chaque valeur attendue est correcte. Rapporter les écarts de fidélité de l’implémentation, pas les dissimuler sous des tolérances ou des skips.
