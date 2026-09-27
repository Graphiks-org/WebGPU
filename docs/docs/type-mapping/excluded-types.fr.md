# Types exclus

Certains types WebGPU sont exclus de l'implémentation cœur, car ils sont soit spécifiques au
navigateur, soit redondants, soit liés à des contextes web particuliers. Cette sélection maintient
une API propre et portable.

## Types spécifiques au navigateur

| Type exclu | Raison |
|------------|--------|
| `NavigatorGPU`, `Navigator`, `WorkerNavigator` | Ces types appartiennent aux API navigateur et ne font pas partie du cœur WebGPU. Les inclure couplerait inutilement le code à un environnement navigateur, au détriment de la portabilité. |

## Types liés au canvas web

| Type exclu | Raison |
|------------|--------|
| `GPUCanvasContext`, `GPUCanvasConfiguration`, `GPUCanvasAlphaMode`, `GPUCanvasToneMappingMode`, `GPUCanvasToneMapping` | Ces types servent surtout aux opérations canvas. Ils sont omis du cœur mais fournis par `webgpu-browser` pour le navigateur. |

L'implémentation navigateur expose ses propres types canvas (`SurfaceConfiguration`,
`PredefinedColorSpace`, `GPUCanvasAlphaMode`, `CanvasSurface`) dans `org.graphiks.webgpu.browser`
plutôt que dans l'API portable.

## Dictionnaires redondants

| Type exclu | Raison |
|------------|--------|
| `GPUColorDict`, `GPUOrigin2DDict`, `GPUOrigin3DDict`, `GPUExtent3DDict` | Ces dictionnaires sont répétitifs en structure et en rôle. Ils sont regroupés dans des interfaces plus simples. |

## Types d'événements web

| Type exclu | Raison |
|------------|--------|
| `GPUUncapturedErrorEvent`, `GPUUncapturedErrorEventInit` | Ces types d'événements sont liés à la gestion d'erreurs WebGPU dans le navigateur. Ils sont disponibles dans l'implémentation navigateur, qui expose le callback d'erreur non capturée. |

## Types liés aux Web Workers

| Type exclu | Raison |
|------------|--------|
| `GPUExternalTexture`, `GPUExternalTextureDescriptor`, `GPUExternalTextureBindingLayout`, `GPUCopyExternalImageSource`, `GPUCopyExternalImageDestInfo`, `GPUCopyExternalImageSourceInfo` | Ces types concernent les Web Workers ou la gestion avancée des textures externes. Ils sont exclus pour réduire la complexité du cœur. |

En excluant ces types, l'implémentation reste ciblée et évite les dépendances inutiles aux détails
web. Les types propres au navigateur dont une application a besoin sont fournis par
`webgpu-web-bindings` et `webgpu-browser`.
