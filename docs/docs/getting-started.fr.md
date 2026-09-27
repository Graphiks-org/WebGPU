# Démarrer

## Prérequis

Utilisez le JDK 25 et le Gradle wrapper du dépôt. Un consommateur utilise Maven Central ; les
versions snapshot demandent également le dépôt des snapshots Maven Central.

## Ajouter un module

Les coordonnées publiées utilisent le groupe `org.graphiks`. Choisissez le module nécessaire :

```kotlin
dependencies {
    implementation("org.graphiks:webgpu-api:<version>")
    // Facultatif : implémentations des descripteurs ou interop navigateur.
    // implementation("org.graphiks:webgpu-descriptors:<version>")
    // implementation("org.graphiks:webgpu-web:<version>")
}
```

Remplacez `<version>` par une version release ou snapshot disponible. La valeur de développement
du dépôt est `0.1.0-SNAPSHOT` ; elle ne signifie pas qu’une publication existe déjà.

## Utiliser un type portable

```kotlin
import org.graphiks.webgpu.GPUTextureSwizzle

val identity = GPUTextureSwizzle().toWebGpuString() // "rgba"
```

Ce comportement est vérifié par `GPUTextureSwizzleTest` dans `webgpu-api`.

Consultez l’[architecture](architecture.md) pour les limites des modules et le
[mapping des types](generated/type-mapping.md) pour les correspondances WebGPU/Kotlin.
