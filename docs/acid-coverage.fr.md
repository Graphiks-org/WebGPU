# Couverture acid et lacunes restantes du contrat

Ce document est le bilan du catalogue d’acid tests portable. Il enregistre ce qui est validé, ce qui
est optionnel, ce qui n’a pas pu être exercé dans l’environnement de référence et ce qui reste non
couvert. C’est une déclaration de couverture pour un catalogue défini, pas un certificat de
conformité WebGPU.

Le catalogue vit dans `suite-acid-tests` ; chaque cas est un fichier annoté avec `@AcidTest`, et le
runner est `suite-browser`. L’inventaire généré (`suite-acid-tests/build/suite-inventory/`) et le
site publié sont des sorties de build, jamais édités à la main.

## Catalogue

Il y a **147 cas** : **138 obligatoires** et **9 optionnels**. Un cas n’est optionnel que lorsque la
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
| `features.compressed-bc` | `TextureCompressionBC` | passé |
| `features.subgroups` | `Subgroups` | passé |

Les neuf cas optionnels couvrent six features distinctes : `TimestampQuery` est utilisée par quatre
cas, `TextureComponentSwizzle` par une, `ShaderF16` par une, `IndirectFirstInstance` par une,
`TextureCompressionBC` par une et `Subgroups` par une.

Le runner protège chaque device contre une feature demandée mais non reçue : l’ensemble demandé
doit être un sous-ensemble de `adapter.features`, et après `requestDevice` il doit aussi être un
sous-ensemble de `device.features`.

## Résultat d’exécution

Sur les deux cibles navigateur, avec Chromium 153.0.8010.12 / SwiftShader sur `darwin` :

| Cible | Passés | Unsupported | Échoués | Total |
| --- | ---: | ---: | ---: | ---: |
| JS | 146 | 1 | 0 | 147 |
| Wasm JS | 146 | 1 | 0 | 147 |

Les **138 cas obligatoires passent** sur les deux cibles. Le seul cas non passant est optionnel :
`compute.shader-f16` est `unsupported` parce que l’environnement ne dispose pas de `ShaderF16`. Il
n’est pas présenté comme une preuve de support.

La liste complète des commandes, les métadonnées d’environnement et les rapports bruts sont
consignés dans [verification.fr.md](verification.fr.md).

## Inventaire des résidus restants

Ces entrées restent dans `inventory/uncovered-behaviours.json` et sont affichées comme *à tester* sur
la page de Validation. Le compte est un résultat du scoping, pas une cible à réduire à zéro.

- `device.request-required-features` — le refus déterministe d’une feature optionnelle absente n’a
  pas de cas portable : la feature absente varie avec l’environnement.
- `features.compressed-and-tiered` — le décodage d’un bloc BC1 est exercé avec
  `TextureCompressionBC` ; les features compressées ETC2 et ASTC et les features de formats
  tier1/tier2 sont déclarées dans le contrat mais pas exercées.
- `texture.creation` — les contraintes de création par dimension de vue et nombre de couches sont
  exercées ; les contraintes de création restantes, comme les bornes de niveaux de mip et les
  règles de nombre d’échantillons, ne le sont pas.
- `texture.usage-and-formats` — `TransientAttachment` est exercé avec sa contrainte de compagnon et
  les formats compressés via le cas compressed-bc ; les formats tiered et les paires format-usage
  restantes ne le sont pas.
- `textures.storage-constraints` — les refus storage côté format sont exercés ; les contraintes
  d’accès par format au-delà du cas write-only exercé ne sont pas couvertes.
- `formats.depth-and-packed` — les cibles `Depth16Unorm` et `RGB10A2Uint` paqueté sont exercées ;
  les formats paquetés et de profondeur gated par feature, comme `rg11b10ufloat` et
  `depth32float-stencil8`, ne le sont pas.
- `sampling.comparison-pcf` — l’enveloppe de comparaison indépendante du filtrage et les deux
  directions de comparateur sont exercées sur une profondeur partagée ; la fraction filtrée exacte
  est dépendante de l’implémentation et n’est volontairement pas assertée.
- `data.identifiers-and-indices` — les indices de tracé 32 bits pleine plage et la largeur de
  texture refusée sont exercés ; l’étendue 64 bits complète des alias de taille et d’offset ne
  l’est pas.

## Limites connues de cette preuve

- **Observabilité des timestamps.** Retirer `timestampWrites` du render pass laisse le cas
  `query.render-timestamp-writes` vert, parce que `resolveQuerySet` remplace les octets sentinelle
  même pour une requête que le pass n’a jamais écrite. Le cas prouve une utilisation valide des
  requêtes, la résolution et la préservation de l’étendue, pas qu’un temps mesurable a été
  enregistré ; aucun seuil `end > begin` ou `> 0` n’est imposé.
- **Filtrage de comparaison.** Le résultat filtré exact d’un sampler de comparaison est dépendant
  de l’implémentation ; `sampler.comparison-pcf` asserte l’enveloppe indépendante du filtrage —
  des références au-delà des deux bornes, répondues différemment par `Less` et `Greater` — et les
  résultats binaires par tap, jamais une valeur fractionnaire.
- **Chemin de décodage compressé.** Sur le backend de référence, le texel décodé d’une texture
  compressée s’observe via le sampling fragment ; un load côté compute renvoie zéro, donc
  `features.compressed-bc` rend un quad qui échantillonne au lieu de charger des texels.
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
