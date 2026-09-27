# Architecture

| Module | Rôle | Cibles |
| --- | --- | --- |
| `webgpu-api` | Interfaces, énumérations, alias et buffers WebGPU portables | JVM, Android, JS, Wasm JS, Apple, Linux, Windows et autres cibles Kotlin/Native configurées |
| `webgpu-descriptors` | Implémentations des descripteurs fondées sur `webgpu-api` | JVM, Android, JS, Wasm JS et cibles Kotlin/Native configurées |
| `webgpu-web` | Interop JS et Wasm JS avec les valeurs WebGPU du navigateur | JS et Wasm JS |
| `webgpu-specifications` | HTML, IDL et données documentaires versionnées pour la génération | Outils de build JVM |

Les trois premiers modules sont publiés sur Maven. `webgpu-specifications` fournit les sources
au build ; il ne constitue pas un artefact destiné aux consommateurs. L’API partagée ne possède
ni périphérique GPU ni moteur de rendu. L’interop navigateur appartient à `webgpu-web` ; les
implémentations natives utilisent les contrats portables.

Le générateur lit l’IDL WebGPU et la documentation YAML versionnés, puis écrit les sources des
modules publics. Modifiez la spécification ou les données documentaires, puis suivez la procédure
manuelle d’[entretien de la spécification](specification-maintenance.md). Le
[mapping des types](generated/type-mapping.md) reste la source canonique des choix de types.
