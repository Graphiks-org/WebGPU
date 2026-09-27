# Démarrer

## Prérequis

Utilisez le JDK 25 et le Gradle wrapper du dépôt. Un consommateur utilise Maven Central ; les
versions snapshot demandent également le dépôt des snapshots Maven Central.

## Ajouter un module

Les coordonnées publiées utilisent le groupe `org.graphiks`. Choisissez le module nécessaire :

```kotlin
dependencies {
    implementation("org.graphiks:webgpu-api:<version>")
    // Implémentations des descripteurs (nécessaires à l’implémentation navigateur).
    // implementation("org.graphiks:webgpu-descriptors:<version>")
    // Implémentation navigateur.
    // implementation("org.graphiks:webgpu-browser:<version>")
    // Interop JavaScript directe.
    // implementation("org.graphiks:webgpu-web-bindings:<version>")
}
```

Remplacez `<version>` par une version release ou snapshot disponible. La valeur de développement
du dépôt est `0.1.0-SNAPSHOT` ; elle ne signifie pas qu’une publication existe déjà.

## Utiliser l’implémentation navigateur

Ajoutez `webgpu-browser` et `webgpu-descriptors` au source set JS/Wasm partagé. L’implémentation
navigateur expose le package `org.graphiks.webgpu.browser` ; les descripteurs utilisés ci-dessous
appartiennent à `org.graphiks.webgpu.descriptors`. L’accès WebGPU exige un navigateur compatible et
un contexte sûr.

```kotlin
import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.webgpu.browser.requestAdapter
import org.graphiks.webgpu.descriptors.BufferDescriptor

suspend fun createExampleBuffer() {
    val adapter = requestAdapter().getOrThrow()
    val device = adapter.requestDevice().getOrThrow()
    try {
        val buffer = device.createBuffer(
            BufferDescriptor(
                size = 16uL,
                usage = GPUBufferUsage.CopyDst or GPUBufferUsage.Storage,
            ),
        )
        buffer.close()
    } finally {
        device.close()
        adapter.close()
    }
}
```

Les helpers canvas (`getCanvasSurface`, `SurfaceConfiguration`) se trouvent également dans
`org.graphiks.webgpu.browser`. `getCanvasSurface()` est une extension du type léger
`org.graphiks.webgpu.browser.HTMLCanvasElement` ; convertissez votre canvas DOM vers ce type (par
exemple `(canvas as HTMLCanvasElement).getCanvasSurface()`). `webgpu-browser` ne fournit pas de
bibliothèque DOM de façon transitive : ajoutez-en une (par exemple `kotlin-browser`) si votre code
utilise des types DOM.

## Utiliser un type portable

```kotlin
import org.graphiks.webgpu.GPUTextureSwizzle

val identity = GPUTextureSwizzle().toWebGpuString() // "rgba"
```

Ce comportement est vérifié par `GPUTextureSwizzleTest` dans `webgpu-api`.

Consultez l’[architecture](architecture.md) pour les limites des modules et le
[mapping des types](generated/type-mapping.md) pour les correspondances WebGPU/Kotlin.
