# Excluded Types

Certain WebGPU types are excluded from the core implementation because they are either
browser-specific, redundant, or relate to specialized web contexts. This selective approach helps
maintain a clean, platform-agnostic API.

## Browser-specific types

| Excluded type | Reason |
|---------------|--------|
| `NavigatorGPU`, `Navigator`, `WorkerNavigator` | These types belong to browser APIs and are not part of the core WebGPU functionality. Including them would unnecessarily couple the code to a browser environment, limiting portability. |

## Web canvas-related types

| Excluded type | Reason |
|---------------|--------|
| `GPUCanvasContext`, `GPUCanvasConfiguration`, `GPUCanvasAlphaMode`, `GPUCanvasToneMappingMode`, `GPUCanvasToneMapping` | These types are primarily used for web-based canvas operations. They are omitted from the core implementation but are provided by `webgpu-browser` for browser use. |

The browser implementation exposes its own canvas types (`SurfaceConfiguration`,
`PredefinedColorSpace`, `GPUCanvasAlphaMode`, `CanvasSurface`) in `org.graphiks.webgpu.browser`
rather than in the portable API.

## Redundant dictionary types

| Excluded type | Reason |
|---------------|--------|
| `GPUColorDict`, `GPUOrigin2DDict`, `GPUOrigin3DDict`, `GPUExtent3DDict` | These dictionary types are repetitive in structure and purpose. They are consolidated into more streamlined interfaces in the implementation. |

## Web event types

| Excluded type | Reason |
|---------------|--------|
| `GPUUncapturedErrorEvent`, `GPUUncapturedErrorEventInit` | These specialized event types are tied to WebGPU error handling in browsers. They are available in the browser implementation, which exposes the uncaptured-error callback. |

## Web Worker-related types

| Excluded type | Reason |
|---------------|--------|
| `GPUExternalTexture`, `GPUExternalTextureDescriptor`, `GPUExternalTextureBindingLayout`, `GPUCopyExternalImageSource`, `GPUCopyExternalImageDestInfo`, `GPUCopyExternalImageSourceInfo` | These types focus on Web Workers or advanced external texture handling. They are excluded to reduce complexity in the core implementation. |

By excluding these types, the implementation remains focused and avoids unnecessary dependencies on
web-specific details. Browser-only types that an application still needs are provided by
`webgpu-web-bindings` and `webgpu-browser`.
