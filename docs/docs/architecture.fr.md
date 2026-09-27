# Architecture

| Module | Rôle | Cibles |
| --- | --- | --- |
| `webgpu-api` | Interfaces, énumérations, alias et buffers WebGPU portables | JVM, Android, JS, Wasm JS, Apple, Linux, Windows et autres cibles Kotlin/Native configurées |
| `webgpu-descriptors` | Implémentations des descripteurs fondées sur `webgpu-api` | JVM, Android, JS, Wasm JS et cibles Kotlin/Native configurées |
| `webgpu-web-bindings` | Bindings JavaScript générés et interop Kotlin JS/Wasm | JS et Wasm JS |
| `webgpu-browser` | Implémentation navigateur : wrappers de ressources, conversions de descripteurs et surfaces canvas | JS et Wasm JS |
| `webgpu-specifications` | HTML, IDL et données documentaires versionnées pour la génération | Outils de build JVM |

Les quatre premiers modules sont publiés sur Maven. `webgpu-specifications` fournit les sources
au build ; il ne constitue pas un artefact destiné aux consommateurs. L’API partagée ne possède
ni périphérique GPU ni moteur de rendu. L’interop navigateur appartient à `webgpu-web-bindings` ;
`webgpu-browser` est l’implémentation navigateur qui consomme les contrats portables et les
bindings ; les implémentations natives utilisent les contrats portables.

Le générateur lit l’IDL WebGPU et la documentation YAML versionnés, puis écrit les sources des
modules publics. Modifiez la spécification ou les données documentaires, puis suivez la procédure
manuelle d’[entretien de la spécification](specification-maintenance.md). Le
[mapping des types](type-mapping/index.md) reste la source canonique des choix de types.
