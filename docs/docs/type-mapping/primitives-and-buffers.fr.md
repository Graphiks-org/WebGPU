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
- Sur la JVM, il enveloppe un `MemorySegment` via `ArrayBuffer.wrap(segment)`.
- Sur les cibles Kotlin/Native, il enveloppe un pointeur brut via
  `ArrayBuffer.wrap(pointer, size)`.
- Sur Android, il enveloppe un `ByteBuffer` direct via `ArrayBuffer.wrap(buffer)` ou —
  pour une mémoire qu'une bibliothèque native prête à l'appelant, telle qu'une plage
  mappée d'un buffer GPU entre map et unmap — la plage empruntée elle-même via
  `ArrayBuffer.wrap(address, size)`. La vue empruntée lit et écrit cette mémoire sur
  place, dans l'ordre des octets natif de la plateforme, et ne la libère jamais :
  l'appelant reste propriétaire de la mémoire et doit la maintenir valide pendant
  toute la durée de vie de la vue.

### AllowSharedBufferSource

`AllowSharedBufferSource` correspond au type `ArrayBuffer` sur toutes les plateformes, ce qui
simplifie l'API tout en restant compatible avec la spécification WebGPU.
