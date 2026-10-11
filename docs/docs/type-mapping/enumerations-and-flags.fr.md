# Énumérations et drapeaux

## Implémentation des énumérations

Les énumérations WebGPU sont implémentées par des classes `enum` Kotlin. Les valeurs proviennent des
en-têtes C WebGPU décrits dans
[wgpu.yml](https://github.com/webgpu-native/webgpu-headers/blob/main/webgpu.yml).

Les conventions de nommage suivent la spécification C plutôt que les valeurs IDL, car elles sont
plus naturelles et plus lisibles pour les développeurs.

## Types de drapeaux

Les drapeaux (bit flags) sont implémentés par des `value class` Kotlin enveloppant un `ULong` : un
masque est donc un seul entier dans les deux sens — les descripteurs l’acceptent et
`GPUBuffer.usage` / `GPUTexture.usage` le retournent.

```kotlin
// Depuis bitflags.kt
@JvmInline
value class GPUBufferUsage private constructor(val value: ULong) {
    public infix fun or(other: GPUBufferUsage): GPUBufferUsage
    public infix fun of(values: Array<GPUBufferUsage>): GPUBufferUsage

    public companion object {
        public val None: GPUBufferUsage
        public val MapRead: GPUBufferUsage
        public val MapWrite: GPUBufferUsage
        public val CopySrc: GPUBufferUsage
        // ...

        /** `true` quand tous les bits de [other] sont présents dans ce masque. */
        public operator fun GPUBufferUsage.contains(other: GPUBufferUsage): Boolean

        /** Reconstruit un masque depuis des bits bruts, en préservant les bits inconnus. */
        public fun fromBits(value: ULong): GPUBufferUsage
    }
}
```

`a in mask` signifie « tous les bits de `a` sont présents dans `mask` » ; comme `None` n’a aucun
bit, `None in mask` est vrai pour tout masque, y compris `None` lui-même.

`fromBits` préserve les bits inconnus : il n’effectue aucune validation GPU, donc un masque portant
des bits que le contrat actuel ne nomme pas reste une valeur valide à manipuler, et c’est le
backend qui décide si une commande l’accepte.

## Constantes

Les constantes WebGPU deviennent des valeurs de masque portant des valeurs `ULong` explicites. Cette
approche garantit la sûreté de typage tout en restant compatible avec les en-têtes WebGPU natifs.

L’usage de `ULong` comme type sous-jacent s’aligne sur le type utilisé pour les constantes dans
l’implémentation WebGPU native, ce qui assure un comportement identique sur toutes les plateformes.
