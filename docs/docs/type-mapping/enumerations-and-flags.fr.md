# Énumérations et drapeaux

## Implémentation des énumérations

Les énumérations WebGPU sont implémentées par des classes `enum` Kotlin qui implémentent l'interface
`FlagEnumeration`. Les valeurs proviennent des en-têtes C WebGPU décrits dans
[wgpu.yml](https://github.com/webgpu-native/webgpu-headers/blob/main/webgpu.yml).

Les conventions de nommage suivent la spécification C plutôt que les valeurs IDL, car elles sont plus
naturelles et plus lisibles pour les développeurs.

### Exemple : GPUBufferUsage

Dans la spécification WebGPU, les drapeaux d'usage des buffers sont définis par des constantes :

```webidl
namespace GPUBufferUsage {
    const GPUFlagsConstant MAP_READ = 0x0001;
    const GPUFlagsConstant MAP_WRITE = 0x0002;
    const GPUFlagsConstant COPY_SRC = 0x0004;
    // ...
};
```

Dans notre implémentation, elles deviennent une classe `enum` :

```kotlin
// Depuis bitflags.kt
enum class GPUBufferUsage(override val value: ULong): FlagEnumeration {
    None(0uL),
    MapRead(1uL),
    MapWrite(2uL),
    CopySrc(4uL),
    CopyDst(8uL),
    Index(16uL),
    Vertex(32uL),
    Uniform(64uL),
    Storage(128uL),
    Indirect(256uL),
    QueryResolve(512uL);
}
```

## Types de drapeaux

Les drapeaux (bit flags) sont implémentés par des `Set<EnumType>` Kotlin pour des opérations sûres :

```kotlin
// Depuis typealiases.kt
typealias GPUBufferUsageFlags = Set<GPUBufferUsage>
typealias GPUMapModeFlags = Set<GPUMapMode>
typealias GPUTextureUsageFlags = Set<GPUTextureUsage>
typealias GPUShaderStageFlags = Set<GPUShaderStage>
```

## Constantes

Les constantes WebGPU deviennent des valeurs d'énumération portant des valeurs `ULong` explicites.
Cette approche garantit la sûreté de typage tout en restant compatible avec les en-têtes WebGPU
natifs.

L'usage de `ULong` comme type sous-jacent s'aligne sur le type utilisé pour les constantes dans
l'implémentation WebGPU native, ce qui assure un comportement identique sur toutes les plateformes.
