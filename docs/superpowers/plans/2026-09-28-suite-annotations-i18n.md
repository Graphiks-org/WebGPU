# Graphiks WebGPU Suite — Plan 1b : annotations typées, textes localisés et inventaire généré

Date : 2026-09-28

Statut au 2026-09-28 : amendement intégré avec les fondations dans `b4d750a` (PR #120). Ne pas rejouer ce plan. Les tâches ci-dessous décrivent le découpage du travail ; les checkboxes ne certifient pas les vérifications exécutées. Consulter `docs/verification.md` pour les résultats consignés.

## Goal

Faire du code des cas la source de vérité de l'inventaire : annotations typées sur chaque cas, textes
en ressources localisées, et inventaire **généré** au build plutôt que rédigé et dupliqué. Supprimer
les fichiers d'inventaire maintenus à la main.

## Global Constraints

- En Kotlin, un paramètre d'annotation n'accepte ni value class ni structure : seulement primitifs,
  `String`, `KClass`, `enum`, annotation, et tableaux. Les champs typés utilisent donc des `enum` ou
  des `const val` générées.
- Les textes ne sont pas dans les annotations. La clé est l'identifiant sérialisé de l'enum (par exemple `buffers.mapped-at-creation`), pas son nom Kotlin. Les textes vivent
  dans `inventory/i18n/behaviours.<locale>.json` (locales `en` et `fr`).
- Le code reste lisible : annotation courte et typée sur chaque cas, pas de prose multilingue.
- La génération est de l'outillage de build ; aucun test dédié à l'architecture.
- Aucun doublon : `behaviours.<locale>.json`, `foundation-case-ids.json`, `contract.md`, `symbols.tsv` et
  `baseline.json` deviennent des sorties de build, plus des fichiers versionnés.
- Les comportements sans cas (à tester) sont rédigés dans `inventory/uncovered-behaviours.json`, car une
  annotation ne peut pas s'attacher à une fonction inexistante. Leurs textes sont dans les ressources localisées.

## Interfaces

```kotlin
// suite-core
enum class AcidFamily { BuffersMapping, TransfersBufferTexture, Compute, ErrorsAsync, /* … */ }
enum class AcidCaseId { BuffersMappedAtCreation, TransfersCopyOffsets, /* … */ }

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class AcidTest(
    val id: AcidCaseId,
    val family: AcidFamily,
    val contract: Array<String>,                       // ApiSymbols.* (généré)
    val requiredFeatures: Array<GPUFeatureName> = [],
)

// AcidCase perd son `title` : la clé i18n est l'id.
data class AcidCase(
    val id: AcidCaseId,
    val family: AcidFamily,
    val contract: List<String>,
    val requiredFeatures: Set<GPUFeatureName> = emptySet(),
    val run: suspend (GPUDevice) -> Unit,
)
```

- `object ApiSymbols` est **généré** depuis `webgpu-api` : une `const val` par symbole du contrat,
  nommée d'après le symbole (`GPUBuffer_mapAsync` pour `GPUBuffer.mapAsync`). Les annotations
  référencent ces constantes ; un symbole inconnu ne compile pas.
- Ressources rédigées : `inventory/i18n/behaviours.en.json`, `inventory/i18n/behaviours.fr.json`.
  Sections : `cases`, `families`, `behaviours`, indexées par les identifiants sérialisés.
  Exemple : `cases["buffers.mapped-at-creation"].title` et `.expectation`.
- Sorties Gradle : `ApiSymbols.kt` et `FoundationCases.kt` sous
  `suite-acid-tests/build/generated/suite/commonMain/kotlin/`, puis `cases.json`,
  `foundation-case-ids.json` et `baseline.json` sous `suite-acid-tests/build/suite-inventory/`.
- Sorties du site : `tools/build-inventory.mjs` produit l’inventaire et les textes localisés sous
  `build/site/inventory/`. `tools/build-site.mjs` appelle ce script après assemblage des rapports et distributions.
- Organisation retenue : un fichier par cas, dans le package défini par `AcidFamily.packageName`.

## Tasks

- [ ] **1. Introduire les types.** Dans `suite-core` : `AcidFamily`, `AcidCaseId`, `AcidTest`.
  Retirer `title` d'`AcidCase`.
- [ ] **2. Rédiger les textes.** Créer `inventory/i18n/behaviours.en.json` et `.fr.json` pour les onze
  cas, les familles et les comportements non couverts.
- [ ] **3. Ajouter le générateur.** Implémentation intégrée dans
  `build-logic/src/main/kotlin/org/graphiks/webgpu/inventory/SuiteInventoryPlugin.kt` : le plugin
  `org.graphiks.webgpu-suite-inventory`, appliqué à `suite-acid-tests`, enregistre `generateSuiteInventory`.
  Il parse les sources et annotations, génère `ApiSymbols.kt`, `FoundationCases.kt`, les manifestes des cas
  et `baseline.json`. Le script Node construit ensuite les vues documentaires de l’inventaire.
- [ ] **4. Annoter les cas.** Ajouter `@AcidTest` sur les onze fonctions ; supprimer le
  `FoundationCases.kt` rédigé et la structure `AcidCase` à titre.
- [ ] **5. Brancher le runner et le site.** `tools/run-browser.mjs` lit les ids depuis
  `suite-acid-tests/build/suite-inventory/foundation-case-ids.json` ; `tools/build-site.mjs` appelle le générateur d’inventaire du site ;
  `site/app.js` charge les textes de la locale et la page `suite/` devient bilingue en/fr.
- [ ] **6. Mettre à jour docs et CI.** `docs/running.md`, `docs/adding-a-case.md`,
  `docs/verification.md`, `CHANGELOG.md` ; la CI exécute le générateur avant le runner.
- [ ] **7. Vérifier.** `./gradlew check`, run navigateur JS/Wasm, `build-site`, site sous préfixe et en
  largeur mobile.

## Non-objectifs

- Ne pas générer `symbols.tsv`/`contract.md` comme tests ; ce sont des sorties de présentation.
- Ne pas introduire KSP : la génération est une tâche Gradle, comme le reste de l'outillage du dépôt.
- Ne pas versionner les sorties générées.
