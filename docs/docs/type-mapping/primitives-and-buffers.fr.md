# Primitives et buffers

## Correspondance des types primitifs

Les primitives WebGPU sont mappées directement vers les types primitifs Kotlin, ce qui garantit la
sûreté de typage et l'efficacité. La correspondance suit une stratégie portable pour rester cohérente
sur toutes les cibles.

### Correspondances de base

| Type WebGPU | Type Kotlin | Exemple |
|-------------|-------------|---------|
| `unsigned long` | `UInt` | `GPUSize32`, `GPUIndex32` |
| `long` | `Int` | `GPUSignedOffset32`, `GPUDepthBias` |
| `unsigned long long` | `ULong` | `GPUSize64` |
| `float` | `Float` | Paramètres de `setViewport` |
| `double` | `Double` | Propriétés de `GPUColor`, `GPUPipelineConstantValue` |
| `boolean` | `Boolean` | Diverses propriétés booléennes |
| `DOMString` | `String` | Labels, code de shader |

### Exemples extraits du code

Le module cœur définit des alias de types propres à WebGPU :

```kotlin
// Depuis typealiases.kt
typealias GPUBufferDynamicOffset = UInt
typealias GPUStencilValue = UInt
typealias GPUSampleMask = UInt
typealias GPUDepthBias = Int
typealias GPUSize64 = ULong
typealias GPUIntegerCoordinate = UInt
typealias GPUIndex32 = UInt
typealias GPUSize32 = UInt
typealias GPUSignedOffset32 = Int
```

Dans l'implémentation web, les mêmes valeurs sont exposées à JavaScript via les bindings générés :

```kotlin
// Depuis types.kt dans webgpu-web-bindings
external interface WGPUColor : JsAny {
    var r: JsNumber /* double */
    var g: JsNumber /* double */
    var b: JsNumber /* double */
    var a: JsNumber /* double */
}
```

## Types de buffers

### ArrayBuffer

`ArrayBuffer` est mappé différemment selon la plateforme :

- Dans les environnements navigateur (JavaScript et WebAssembly), il correspond au type
  `ArrayBuffer` de JavaScript.
- Sur les autres plateformes (natif), il correspond à un pointeur brut.

### AllowSharedBufferSource

`AllowSharedBufferSource` correspond au type `ArrayBuffer` sur toutes les plateformes, ce qui
simplifie l'API tout en restant compatible avec la spécification WebGPU.
