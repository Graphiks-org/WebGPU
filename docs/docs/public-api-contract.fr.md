# Contrat de l’API publique

Le dépôt est **incubateur** : l’API et les coordonnées publiées peuvent évoluer, et des ruptures
de signatures peuvent être livrées sans chemin de migration. Cette page décrit le contrat actuel
pour qu’un implémenteur de backend (et le prochain contributeur) sache exactement quoi
implémenter. Pour l’usage au quotidien, voir [Démarrage](getting-started.md).

## Emplacements vides des séquences

Les cinq membres `sequence<T?>` de l’IDL versionné conservent la nullabilité de leur élément de
bout en bout :

- `GPUPipelineLayoutDescriptor.bindGroupLayouts: List<GPUBindGroupLayout?>`
- `GPUFragmentState.targets: List<GPUColorTargetState?>`
- `GPUVertexState.buffers: List<GPUVertexBufferLayout?>`
- `GPURenderPassDescriptor.colorAttachments: List<GPURenderPassColorAttachment?>`
- `GPURenderPassLayout.colorFormats: List<GPUTextureFormat?>`

Une liste conserve sa longueur et ses indices jusque dans le tableau JavaScript ; n’utilisez
jamais `filterNotNull()` pour convertir ces propriétés.

## Limites requises

`GPUDeviceDescriptor.requiredLimits` est `GPURequiredLimits?` : `null` signifie l’absence de
contrainte, une valeur zéro explicite reste une valeur explicite. `RequiredLimits` reflète chaque
propriété de `GPUSupportedLimits` en une propriété nullable à défaut `null` ; le navigateur
n’émet que les clés non nulles. `adapter.limits` et `device.limits` conservent le modèle complet
et non nullable `GPUSupportedLimits`.

## Perte du device

`GPUDevice.awaitLost(): Result<GPUDeviceLostInfo>` est l’observation portable et annulable de la
perte du device. La perte est un résultat réussi portant une raison et un message, pas un échec
de `Result` ; seule une erreur d’interop est un échec. Plusieurs observateurs voient la même
perte, un observateur annulé avant la perte ne bloque ni les autres observateurs ni ne détruit
le device, et un observateur qui démarre après la perte se résout immédiatement. La fermeture
explicite du device déclenche la notification de perte avec la raison de destruction lorsque le
backend la fournit.

## Masques d’usage

`GPUBuffer.usage` retourne `GPUBufferUsage` et `GPUTexture.usage` retourne `GPUTextureUsage`
(des value class masquées enveloppant un `ULong`). `a in mask` signifie « tous les bits de `a`
sont présents dans `mask` » : `None` est donc contenu dans tout masque. `fromBits` préserve les
bits inconnus et n’effectue aucune validation GPU.

## Durée de vie des ressources et mapping borné

Les ressources créées par un device appartiennent à l’appelant : `close()` les détruit (un
backend natif doit aussi libérer sa référence possédée), et fermer une ressource déjà fermée ne
libère pas la même référence une seconde fois. Les handles sans opération WebGPU `destroy` ne
font que libérer la référence possédée à la fermeture. Les plages mappées sont empruntées et
invalidées par `unmap()` ou par la destruction. Une texture de canvas est empruntée : fermer son
wrapper ne détruit pas la texture détenue par le canvas.

`GPUBuffer.withMappedRange(mode, offset, size) { view -> ... }` mappe une plage, exécute un bloc
non suspendu avec la vue empruntée, puis dépappe dans un `finally`.

## Interop navigateur

L’ownership des textures est explicite : `Texture.wrapOwned(handler)` prend en charge la
destruction du handle, `Texture.wrapBorrowed(handler)` ne la détruit jamais. `CanvasSurface` est
`AutoCloseable` : `close()` déconfigure le contexte canvas et ne possède pas le device passé à
`configure`. La propriété `handler` des wrappers navigateur est une échappatoire d’interop :
les opérations directes sur le handle peuvent invalider le contrat du wrapper.

## Transmission au backend Dawn

La compatibilité conceptuelle avec Dawn et la validation du backend natif sont deux résultats
distincts. La validation native **n’est pas exécutée dans ce dépôt** ; exécutez les cas portables
de la suite sur le backend natif dans une session explicitement autorisée pour ce dépôt.

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
