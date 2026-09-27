# Correspondance des types

Cette section décrit la correspondance entre les types WebGPU et les implémentations Kotlin
Multiplatform, module par module. C'est la référence qui maintient la cohérence entre l'API
portable, les descripteurs et les bindings JavaScript, tout en restant typée et portable.

## Modules

La correspondance est répartie sur quatre modules publiés :

1. **`webgpu-api`** — interfaces et définitions de types portables.
2. **`webgpu-descriptors`** — implémentations des descripteurs fondées sur l'API portable.
3. **`webgpu-web-bindings`** — bindings JavaScript générés et interop Kotlin JS/Wasm.
4. **`webgpu-browser`** — implémentation navigateur : wrappers de ressources, conversions de
   descripteurs et surfaces canvas.

## Entrées de la correspondance

Les définitions WebIDL de [w3.org/TR/webgpu](https://www.w3.org/TR/webgpu/) servent de base aux
correspondances portables. Les valeurs d'énumérations et les conventions de nommage suivent les
en-têtes C WebGPU décrits par
[wgpu.yml](https://github.com/webgpu-native/webgpu-headers/blob/main/webgpu.yml).

## Principes

1. **Indépendance de plateforme** — les types de base sont définis de façon portable afin de se
   comporter identiquement sur chaque cible.
2. **Sûreté de typage** — le système de types Kotlin assure la sûreté à la compilation des
   opérations WebGPU.
3. **Performance** — des correspondances directes vers les types natifs sont utilisées lorsque
   c'est possible.
4. **Extensibilité** — la conception par interfaces autorise différentes implémentations et
   extensions.
5. **Cohérence** — les noms et les valeurs sont alignés sur les spécifications WebGPU officielles.

## Dans cette section

- [Primitives et buffers](primitives-and-buffers.md)
- [Énumérations et drapeaux](enumerations-and-flags.md)
- [Dictionnaires et unions](dictionaries-and-unions.md)
- [Types exclus](excluded-types.md)
