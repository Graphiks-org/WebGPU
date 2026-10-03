# Couverture acid et lacunes restantes du contrat

Ce document est le bilan du catalogue d’acid tests portable. Il enregistre ce qui est validé, ce qui
est optionnel, ce qui n’a pas pu être exercé dans l’environnement de référence et ce qui reste non
couvert. C’est une déclaration de couverture pour un catalogue défini, pas un certificat de
conformité WebGPU.

Le catalogue vit dans `suite-acid-tests` ; chaque cas est un fichier annoté avec `@AcidTest`, et le
runner est `suite-browser`. L’inventaire généré (`suite-acid-tests/build/suite-inventory/`) et le
site publié sont des sorties de build, jamais édités à la main.

## Catalogue

Il y a **132 cas** : **125 obligatoires** et **7 optionnels**. Un cas n’est optionnel que lorsque la
feature du contrat dont il a besoin est elle-même optionnelle ; la feature est déclarée dans
`requiredFeatures`, jamais cachée.

| Cas optionnel | Feature | Statut dans l’environnement de référence |
| --- | --- | --- |
| `compute.shader-f16` | `ShaderF16` | `unsupported` (feature absente) |
| `render.indirect-first-instance` | `IndirectFirstInstance` | passé |
| `queries.timestamp-resolve` | `TimestampQuery` | passé |
| `query.render-timestamp-writes` | `TimestampQuery` | passé |
| `errors.compute-timestamp-indices` | `TimestampQuery` | passé |
| `errors.render-timestamp-indices` | `TimestampQuery` | passé |
| `texture.view-swizzle` | `TextureComponentSwizzle` | passé |

Les sept cas optionnels couvrent quatre features distinctes : `TimestampQuery` est utilisée par
quatre cas, `TextureComponentSwizzle` par un, `ShaderF16` par un et `IndirectFirstInstance` par un.

Le runner protège chaque device contre une feature demandée mais non reçue : l’ensemble demandé
doit être un sous-ensemble de `adapter.features`, et après `requestDevice` il doit aussi être un
sous-ensemble de `device.features`.

## Résultat d’exécution

Sur les deux cibles navigateur, avec Chromium 153.0.8010.12 / SwiftShader sur `darwin` :

| Cible | Passés | Unsupported | Échoués | Total |
| --- | ---: | ---: | ---: | ---: |
| JS | 131 | 1 | 0 | 132 |
| Wasm JS | 131 | 1 | 0 | 132 |

Les **125 cas obligatoires passent** sur les deux cibles. Le seul cas non passant est optionnel :
`compute.shader-f16` est `unsupported` parce que l’environnement ne dispose pas de `ShaderF16`. Il
n’est pas présenté comme une preuve de support.

La liste complète des commandes, les métadonnées d’environnement et les rapports bruts sont
consignés dans [verification.fr.md](verification.fr.md).

## Inventaire des résidus restants

Ces entrées restent dans `inventory/uncovered-behaviours.json` et sont affichées comme *à tester* sur
la page de Validation. Le compte est un résultat du scoping, pas une cible à réduire à zéro.

- `device.features-and-limits`, `device.request-required-features` — l’espace complet des limites et
  le refus déterministe d’une feature absente.
- `features.compressed-and-tiered`, `features.subgroups` — features optionnelles déclarées dans le
  contrat mais non exercées.
- `texture.creation`, `texture.view-usage-aspect`, `texture.usage-and-formats`,
  `textures.storage-constraints`, `formats.depth-and-packed`, `sampling.limits` — contraintes de
  création de texture, vues StencilOnly, formats compressés/tiered/à paquets/profondeur, contraintes
  de texture de stockage et limites de sampler.
- `sampling.comparison-pcf` — le filtrage pourcentage-plus-proche (un sampler de comparaison filtre
  des résultats de comparaison de profondeur plutôt que des valeurs) et les fonctions de comparaison
  au-delà du cas à profondeur uniforme.
- `transfers.stencil-copy-aspect` — un aspect de copie de texture StencilOnly.
- `render.primitive-and-multisample`, `bundles.negative-validation` — points/lignes et masques
  partiels, validation négative de render bundles.
- `data.identifiers-and-indices` — l’étendue 64 bits complète des alias de taille/index/coordonnées.

## Limites connues de cette preuve

- **Observabilité des timestamps.** Retirer `timestampWrites` du render pass laisse le cas
  `query.render-timestamp-writes` vert, parce que `resolveQuerySet` remplace les octets sentinelle
  même pour une requête que le pass n’a jamais écrite. Le cas prouve une utilisation valide des
  requêtes, la résolution et la préservation de l’étendue, pas qu’un temps mesurable a été
  enregistré ; aucun seuil `end > begin` ou `> 0` n’est imposé.
- **Attachements discardés.** Un attachement de rendu stocké avec `GPUStoreOp.Discard` puis attaché
  avec `Load` est vérifié comme lisant le noir transparent `(0,0,0,0)` : la spécification pinnée
  garantit que la sous-région discardée est mise à zéro, y compris pour les attachements `Load`
  ultérieurs.
- **Backend logiciel.** Tous les résultats ci-dessus sont des résultats fonctionnels SwiftShader.
  Ce ne sont ni des résultats de GPU physique ni des mesures de performance.
- **`--backend=default`** signifie seulement « pas de drapeaux SwiftShader » ; ce n’est jamais
  rapporté comme du matériel.
- **Cibles natives.** L’exécution GPU native appartient aux dépôts de bindings. Ce dépôt compile
  les modules partagés pour JVM, JS, Wasm JS, Linux x64 et macOS ARM64 et n’exécute que les cas
  navigateur.
- **Comportements jamais testables.** La perte de device, `setImmediates`, les erreurs
  out-of-memory/interne et les features que le navigateur de référence n’expose pas n’ont pas de cas
  portable dans ce contrat — aucun chemin d’accès de perte, aucune feature optionnelle dédiée, non
  provoquables de façon portable. Elles ne sont pas suivies comme résidus ; ce sont des limites
  permanentes du catalogue.
