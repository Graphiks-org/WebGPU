# Migration de l’API publique

Ce guide couvre les ruptures du contrat public WebGPU et les obligations des implémenteurs de
backends. Les ruptures de signatures sont intentionnelles : cette version ne promet **aucune**
compatibilité binaire ou source avec l’ancien contrat. Coordonnez la montée de version avant
publication.

## Emplacements vides des séquences

Les cinq membres `sequence<T?>` de l’IDL versionné conservent la nullabilité de leur élément de
bout en bout. Une liste conserve sa longueur et ses indices jusque dans le tableau JavaScript ;
n’utilisez jamais `filterNotNull()` pour convertir ces propriétés.

| Propriété | Avant | Après |
| --- | --- | --- |
| `GPUPipelineLayoutDescriptor.bindGroupLayouts` | `List<GPUBindGroupLayout>` | `List<GPUBindGroupLayout?>` |
| `GPUFragmentState.targets` | `List<GPUColorTargetState>` | `List<GPUColorTargetState?>` |
| `GPUVertexState.buffers` | `List<GPUVertexBufferLayout>` | `List<GPUVertexBufferLayout?>` |
| `GPURenderPassDescriptor.colorAttachments` | `List<GPURenderPassColorAttachment>` | `List<GPURenderPassColorAttachment?>` |
| `GPURenderPassLayout.colorFormats` | `List<GPUTextureFormat>` | `List<GPUTextureFormat?>` |

```kotlin
// Un emplacement null conserve l'index de l'entrée suivante.
RenderPassDescriptor(
    colorAttachments = listOf(null, RenderPassColorAttachment(view = view, loadOp = GPULoadOp.Clear, storeOp = GPUStoreOp.Store)),
)
```

## Limites requises

`GPUDeviceDescriptor.requiredLimits` devient `GPURequiredLimits?`. `null` signifie l’absence de
contrainte ; une valeur zéro explicite reste une valeur explicite. Le nouveau descripteur
`RequiredLimits` reflète chaque propriété de `GPUSupportedLimits` en une propriété nullable à
défaut `null`, et le navigateur n’émet que les clés non nulles.

```kotlin
import org.graphiks.webgpu.descriptors.DeviceDescriptor
import org.graphiks.webgpu.descriptors.RequiredLimits

// Avant : requiredLimits = object : GPUSupportedLimits by adapter.limits { ... }
val descriptor = DeviceDescriptor(
    requiredLimits = RequiredLimits(
        maxComputeWorkgroupSizeX = adapter.limits.maxComputeWorkgroupSizeX,
        maxBufferSize = 65536uL,
    ),
)
```

`adapter.limits` et `device.limits` conservent leur modèle complet et non nullable
`GPUSupportedLimits`.

## Perte du device

`GPUDevice` expose `suspend fun awaitLost(): Result<GPUDeviceLostInfo>`. La perte est un résultat
réussi portant une raison et un message, pas un échec de `Result` ; seule une erreur d’interop
est un échec. Plusieurs observateurs voient la même perte, un observateur annulé avant la perte
ne bloque ni les autres observateurs ni ne détruit le device, et un observateur qui démarre
après la perte se résout immédiatement. La fermeture explicite du device déclenche la
notification de perte avec la raison de destruction lorsque le backend la fournit.

```kotlin
val info = device.awaitLost().getOrThrow()
println("device perdu : ${info.reason} ${info.message}")
```

Les implémenteurs de backends doivent implémenter `awaitLost`.

## Masques d’usage

`GPUBuffer.usage` retourne `GPUBufferUsage` et `GPUTexture.usage` retourne `GPUTextureUsage`
(des value class masquées) au lieu de `Set<...>`. Les classes de masques conservent leurs
constantes et `or`, et gagnent `fromBits(ULong)` et `operator contains`. `contains` signifie
« tous les bits demandés sont présents » : `None` est donc contenu dans tout masque. `fromBits`
préserve les bits inconnus et n’effectue aucune validation GPU.

```kotlin
import org.graphiks.webgpu.GPUBufferUsage

val usage = GPUBufferUsage.CopyDst or GPUBufferUsage.Storage
check(GPUBufferUsage.CopyDst in usage)
check(GPUBufferUsage.None in usage)
val raw = (1uL shl 63) or usage.value
check(GPUBufferUsage.fromBits(raw).value == raw)

// Un buffer peut être recréé avec son propre masque d'usage.
val copy = device.createBuffer(BufferDescriptor(size = buffer.size, usage = buffer.usage))
```

## Durée de vie des ressources et mapping borné

Les ressources créées par un device appartiennent à l’appelant : `close()` les détruit (un backend
natif doit aussi libérer sa référence possédée), et fermer une ressource déjà fermée ne libère pas
la même référence une seconde fois. Les handles sans opération WebGPU `destroy` ne font que
libérer la référence possédée à la fermeture. Les plages mappées sont empruntées et invalidées par
`unmap()` ou par la destruction. Une texture de canvas est empruntée : fermer son wrapper ne
détruit pas la texture détenue par le canvas.

`GPUBuffer.withMappedRange` mappe une plage, exécute un bloc non suspendu avec la vue empruntée,
puis dépappe dans un `finally` :

```kotlin
import org.graphiks.webgpu.withMappedRange

buffer.withMappedRange(GPUMapMode.Write) { view ->
    view.setUInts(0uL, uintArrayOf(1u, 2u, 3u, 4u))
}
```

## Interop navigateur

L’ownership des textures est explicite : `Texture.wrapOwned(handler)` prend en charge la
destruction du handle, `Texture.wrapBorrowed(handler)` ne la détruit jamais. Le constructeur
`Texture(handler, canBeDestroy)` et la propriété `canBeDestroy` sont dépréciés mais conservés
comme pont de migration. `CanvasSurface` est `AutoCloseable` : `close()` déconfigure le contexte
canvas et ne possède pas le device passé à `configure`. La propriété `handler` des wrappers
navigateur est une échappatoire d’interop : les opérations directes sur le handle peuvent
invalider le contrat du wrapper.

## Transmission au backend Dawn

La compatibilité conceptuelle avec Dawn et la validation du backend natif sont deux résultats
distincts. La validation native **n’est pas exécutée dans ce dépôt** ; exécutez les nouveaux cas
portables sur le backend natif dans une session explicitement autorisée pour ce dépôt.

Obligations pour le backend Dawn :

- **Emplacements vides des séquences.** Conserver les indices ; déterminer dans l’en-tête C épinglé
  la représentation d’un emplacement vide propre à chaque tableau (handle nul, format indéfini,
  structure d’attachement/layout vide) ; ne pas assimiler toutes les structures à des pointeurs
  nuls.
- **Limites requises.** Initialiser les limites absentes avec les sentinelles adéquates de
  l’en-tête utilisé, en distinguant les champs 32 et 64 bits ; ne pas supposer qu’une struct C
  mise à zéro est une requête vide.
- **Perte du device.** Enregistrer le callback de perte dès la création du device, le connecter
  à un résultat partagé durable, conserver les données du callback jusqu’à la fin des callbacks
  possibles, et rendre l’annulation d’un observateur indépendante du device.
- **Durée de vie.** Définir `Destroy` et la libération de la référence possédée, sans double
  libération.
- **Annulation.** Gérer les callbacks tardifs après une annulation et la durée de vie de leur
  userdata.
- **Masques d’usage.** Retourner les masques sans perte de bits.
