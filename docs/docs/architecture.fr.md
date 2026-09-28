# Architecture

| Module | Rôle | Cibles |
| --- | --- | --- |
| `webgpu-api` | Interfaces, énumérations, alias et buffers WebGPU portables | JVM, Android, JS, Wasm JS, Apple, Linux, Windows et autres cibles Kotlin/Native configurées |
| `webgpu-descriptors` | Implémentations des descripteurs fondées sur `webgpu-api` | JVM, Android, JS, Wasm JS et cibles Kotlin/Native configurées |
| `webgpu-web-bindings` | Bindings JavaScript générés et interop Kotlin JS/Wasm | JS et Wasm JS |
| `webgpu-browser` | Implémentation navigateur : wrappers de ressources, conversions de descripteurs et surfaces canvas | JS et Wasm JS |
| `webgpu-specifications` | HTML, IDL et données documentaires versionnées pour la génération | Outils de build JVM |
| `suite-core` | Identités typées, annotations et contrat d’exécution des cas | JVM 25, JS, Wasm JS, Linux x64, macOS ARM64 |
| `suite-acid-tests` | Cas portables annotés et catalogue généré | JVM 25, JS, Wasm JS, Linux x64, macOS ARM64 |
| `suite-demos` | Scène de particules compute portable et données reproductibles | JVM 25, JS, Wasm JS, Linux x64, macOS ARM64 |
| `suite-benchmarks` | Charges portables de transfert et de compute et mesures | JVM 25, JS, Wasm JS, Linux x64, macOS ARM64 |
| `suite-browser` | Exécution réelle dans le navigateur et rapports JSON | JS et Wasm JS |

Les quatre premiers modules sont publiés sur Maven. `webgpu-specifications` fournit les sources
au build ; il ne constitue pas un artefact destiné aux consommateurs. L’API partagée ne possède
ni périphérique GPU ni moteur de rendu. L’interop navigateur appartient à `webgpu-web-bindings` ;
`webgpu-browser` est l’implémentation navigateur qui consomme les contrats portables et les
bindings ; les implémentations natives utilisent les contrats portables.

`suite-core`, `suite-acid-tests`, `suite-demos` et `suite-benchmarks` utilisent aussi la convention de
publication Maven ; `suite-browser` est une application. La suite utilise les dépendances entre projets
et les conventions Gradle du dépôt. Chaque cas réside dans son propre fichier, regroupé par famille, et
exerce l’API publique. Les bindings natifs consomment les artifacts partagés et les exécutent dans
leurs dépôts.

Le plugin de build-logic `org.graphiks.webgpu-suite-inventory` génère les constantes de symboles,
le catalogue et les manifestes depuis les sources de l’API et les annotations `@AcidTest`.
L’inventaire du site combine ces sorties avec les comportements non couverts rédigés et les ressources
EN/FR. Les sorties générées ne sont pas versionnées. Le [site de validation](suite.md) est déployé
avec la documentation sous `suite/`.

Le générateur lit l’IDL WebGPU et la documentation YAML versionnés, puis écrit les sources des
modules publics. Modifiez la spécification ou les données documentaires, puis suivez la procédure
manuelle d’[entretien de la spécification](specification-maintenance.md). Le
[mapping des types](type-mapping/index.md) reste la source canonique des choix de types.
