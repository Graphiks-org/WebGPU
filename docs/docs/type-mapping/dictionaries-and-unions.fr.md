# Dictionnaires et unions

## Gestion des types union

### Traitement des types « Dict »

WebGPU utilise des types union dans certains cas, notamment avec les séquences et les dictionnaires.
Pour simplifier l'implémentation et garantir la sûreté de typage, ces unions sont transformées en un
seul type dictionnaire.

#### Exemple : GPUColor

Dans la spécification WebIDL, `GPUColor` est défini comme une union :

```webidl
typedef (sequence<double> or GPUColorDict) GPUColor;
```

Dans notre implémentation, il devient une interface unique :

```kotlin
// Depuis interfaces.kt
interface GPUColor {
    val r: Double
    val g: Double
    val b: Double
    val a: Double
}
```

Cette transformation apporte plusieurs bénéfices :

1. **Sûreté de typage** — les propriétés sont définies explicitement avec leur type.
2. **Clarté** — l'interface communique clairement la structure attendue.
3. **Cohérence** — toutes les implémentations doivent fournir les mêmes propriétés.
4. **Indépendance de plateforme** — l'interface peut être implémentée sur n'importe quelle cible.

#### Implémentation selon les modules

Dans le module cœur, l'interface est définie avec des types Kotlin :

```kotlin
// Depuis interfaces.kt dans webgpu-api
interface GPUColor {
    val r: Double
    val g: Double
    val b: Double
    val a: Double
}
```

Dans le module de bindings, l'interface est définie avec des types JavaScript :

```kotlin
// Depuis types.kt dans webgpu-web-bindings
external interface WGPUColor : JsAny {
    var r: JsNumber /* double */
    var g: JsNumber /* double */
    var b: JsNumber /* double */
    var a: JsNumber /* double */
}
```

Cette approche permet des implémentations spécifiques à chaque plateforme tout en conservant une API
cohérente.

### Records WebIDL et valeurs nullable

Un `record<K, V>` WebIDL n'est pas une `Map` Kotlin. Le binding généré le représente par
`WebGpuRecord`, un `JsAny` externe créé par `createWebGpuRecord()` (un objet sans prototype) et rempli
par `setRecordValue()`. Le dictionnaire reste ainsi un objet JavaScript ordinaire à propriétés
énumérables, comme l'exigent les API navigateur, au lieu d'une instance de `Map`. Il sert aux
constantes de pipeline et aux dictionnaires de limites demandées.

Les membres WebIDL nullable sont conservés à travers la frontière JavaScript. Un membre nullable est
généré en paramètre ou résultat nullable (`WGPUBindGroup?`, `WGPUBuffer?`), et une `Promise` dont la
valeur est nullable devient `Promise<JsAny?>`. Les wrappers navigateur transmettent donc `null` au
lieu de le contraindre, et les points d'entrée asynchrones renvoient des `Result` qui distinguent une
valeur absente, une promesse rejetée et une coroutine annulée.

## Types dictionnaires et interfaces

### Implémentation par interfaces

Les dictionnaires et interfaces WebGPU sont transformés en `interface` Kotlin. Ce choix apporte
plusieurs avantages :

1. **Indépendance de plateforme** — les interfaces peuvent être implémentées différemment selon la
   cible tout en conservant une API cohérente.
2. **Extensibilité** — de nouvelles fonctionnalités peuvent être ajoutées via des fonctions
   d'extension ou l'héritage d'interfaces.
3. **Sûreté de typage** — le compilateur vérifie que toutes les propriétés et méthodes requises sont
   implémentées.
4. **Flexibilité** — les implémentations peuvent être adaptées aux besoins de chaque plateforme.

### Implémentation cœur

Le module cœur (`webgpu-api`) définit des interfaces portables avec des types Kotlin :

```kotlin
// Depuis interfaces.kt
interface GPUBufferDescriptor {
    val size: GPUSize64
    val usage: GPUBufferUsageFlags
    val mappedAtCreation: Boolean
}

interface GPUDevice : GPUObjectBase, AutoCloseable {
    val features: GPUSupportedFeatures
    val limits: GPUSupportedLimits
    val adapterInfo: GPUAdapterInfo
    val queue: GPUQueue
    fun createBuffer(descriptor: GPUBufferDescriptor): GPUBuffer
    // Autres méthodes...
}
```

### Implémentation web

Le module de bindings (`webgpu-web-bindings`) définit des interfaces JavaScript préfixées par `WGPU` :

```kotlin
// Depuis types.kt
external interface WGPUBufferDescriptor : JsAny, WGPUObjectDescriptorBase {
    var size: JsNumber  /* GPUSize64 */
    var usage: JsNumber  /* GPUBufferUsageFlags */
    var mappedAtCreation: Boolean
}

external interface WGPUDevice : JsAny, WGPUObjectBase {
    var features: JsAny /* GPUSupportedFeatures */
    var limits: WGPUSupportedLimits  /* GPUSupportedLimits */
    var adapterInfo: WGPUAdapterInfo  /* GPUAdapterInfo */
    var queue: WGPUQueue  /* GPUQueue */
    fun createBuffer(descriptor: WGPUBufferDescriptor /* GPUBufferDescriptor */): WGPUBuffer
    // Autres méthodes...
}
```

### Extensibilité

Cette approche par interfaces autorise différentes implémentations et extensions :

1. **Implémentations de base** — une implémentation fournit des versions directes de ces interfaces.
2. **Implémentations avancées** — d'autres projets peuvent étendre ces interfaces pour proposer des
   DSL ou des méthodes utilitaires riches.
3. **Extensions personnalisées** — les développeurs peuvent ajouter des fonctions d'extension sans
   modifier les interfaces cœur.

### Valeurs par défaut

Chaque implémentation doit respecter les valeurs par défaut définies par le WebIDL pour rester
conforme à la spécification WebGPU. Cette approche évite les divergences entre plateformes.

Par exemple, si une propriété WebIDL a une valeur par défaut :

```webidl
dictionary GPUSamplerDescriptor {
    GPUAddressMode addressModeU = "clamp-to-edge";
    // Autres propriétés...
};
```

L'implémentation Kotlin doit respecter cette valeur :

```kotlin
interface GPUSamplerDescriptor {
    val addressModeU: GPUAddressMode get() = GPUAddressMode.ClampToEdge
    // Autres propriétés...
}
```
