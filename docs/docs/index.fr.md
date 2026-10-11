# WebGPU

WebGPU fournit des types et des interfaces Kotlin Multiplatform pour l’API WebGPU. Les modules
publiés sont `webgpu-api`, `webgpu-descriptors`, `webgpu-web-bindings` et `webgpu-browser`, dans le
groupe `org.graphiks`. Le module `webgpu-specifications` contient les sources versionnées utilisées
pour générer les bindings.

Une application navigateur part de `webgpu-browser`, qui fournit `requestAdapter`, les wrappers de
ressources, les conversions de descripteurs et les surfaces canvas. `webgpu-web-bindings` expose les
bindings JavaScript générés pour l’interop directe.

- [Démarrer](getting-started.md) avec une dépendance et un exemple d’API.
- [Lire le contrat de l’API publique](public-api-contract.md) pour les signatures actuelles et les obligations des backends.
- [Comprendre les modules](architecture.md) et leurs limites par plateforme.
- [Lancer les tests métier](testing.md) avant de proposer un changement.
- [Entretenir la spécification](specification-maintenance.md) lors d’une évolution de WebGPU.
- Consulter le [mapping des types](type-mapping/index.md) pour les correspondances WebGPU/Kotlin.
