# Public API migration

This guide covers the breaking changes of the public WebGPU contract rework and the obligations
for backend implementers. The signature breaks are intentional: this release does **not** promise
binary or source compatibility with the previous backend contract. Coordinate the upgrade before
publishing.

## Nullable sequence slots

The five `sequence<T?>` members of the versioned IDL keep their element nullability end to end.
A list keeps its length and its indices all the way into the JavaScript array; never use
`filterNotNull()` to convert these properties.

| Property | Before | After |
| --- | --- | --- |
| `GPUPipelineLayoutDescriptor.bindGroupLayouts` | `List<GPUBindGroupLayout>` | `List<GPUBindGroupLayout?>` |
| `GPUFragmentState.targets` | `List<GPUColorTargetState>` | `List<GPUColorTargetState?>` |
| `GPUVertexState.buffers` | `List<GPUVertexBufferLayout>` | `List<GPUVertexBufferLayout?>` |
| `GPURenderPassDescriptor.colorAttachments` | `List<GPURenderPassColorAttachment>` | `List<GPURenderPassColorAttachment?>` |
| `GPURenderPassLayout.colorFormats` | `List<GPUTextureFormat>` | `List<GPUTextureFormat?>` |

```kotlin
// A null slot keeps the index of the following entry.
RenderPassDescriptor(
    colorAttachments = listOf(null, RenderPassColorAttachment(view = view, loadOp = GPULoadOp.Clear, storeOp = GPUStoreOp.Store)),
)
```

## Requested limits

`GPUDeviceDescriptor.requiredLimits` is now `GPURequiredLimits?`. `null` means "no constraint";
an explicit zero stays an explicit value. The new `RequiredLimits` descriptor mirrors every
property of `GPUSupportedLimits` as a nullable property with a `null` default, and the browser
only emits the keys that are not null.

```kotlin
import org.graphiks.webgpu.descriptors.DeviceDescriptor
import org.graphiks.webgpu.descriptors.RequiredLimits

// Before: requiredLimits = object : GPUSupportedLimits by adapter.limits { ... }
val descriptor = DeviceDescriptor(
    requiredLimits = RequiredLimits(
        maxComputeWorkgroupSizeX = adapter.limits.maxComputeWorkgroupSizeX,
        maxBufferSize = 65536uL,
    ),
)
```

`adapter.limits` and `device.limits` keep their complete, non-nullable `GPUSupportedLimits` model.

## Device loss

`GPUDevice` exposes `suspend fun awaitLost(): Result<GPUDeviceLostInfo>`. The loss is a
successful result carrying a reason and a message, not a `Result` failure; only an interop
failure is a failure. Several observers see the same loss, an observer cancelled before the loss
neither blocks the other observers nor destroys the device, and an observer that starts after
the loss resolves immediately. Closing the device explicitly triggers the loss notification with
the destruction reason when the backend provides one.

```kotlin
val info = device.awaitLost().getOrThrow()
println("device lost: ${info.reason} ${info.message}")
```

Backend implementers must implement `awaitLost`.

## Usage masks

`GPUBuffer.usage` returns `GPUBufferUsage` and `GPUTexture.usage` returns `GPUTextureUsage`
(value-class masks) instead of `Set<...>`. The mask classes keep their constants and `or`, and
gain `fromBits(ULong)` and `operator contains`. `contains` means "every requested bit is
present", so `None` is contained in every mask. `fromBits` preserves unknown bits and performs no
GPU validation.

```kotlin
import org.graphiks.webgpu.GPUBufferUsage

val usage = GPUBufferUsage.CopyDst or GPUBufferUsage.Storage
check(GPUBufferUsage.CopyDst in usage)
check(GPUBufferUsage.None in usage)
val raw = (1uL shl 63) or usage.value
check(GPUBufferUsage.fromBits(raw).value == raw)

// A buffer can be recreated with its own usage mask.
val copy = device.createBuffer(BufferDescriptor(size = buffer.size, usage = buffer.usage))
```

## Resource lifetimes and scoped mapping

Resources created by a device are owned by the caller: `close()` destroys them (a native backend
must also release its owned reference), and closing an already closed resource does not release
the same reference twice. Handles without a WebGPU `destroy` operation only release the owned
reference on `close()`. Mapped ranges are borrowed and invalidated by `unmap()` or destruction.
A canvas texture is borrowed: closing its wrapper does not destroy the canvas-owned texture.

`GPUBuffer.withMappedRange` maps a range, runs a non-suspending block with the borrowed view and
unmaps in a `finally`:

```kotlin
import org.graphiks.webgpu.withMappedRange

buffer.withMappedRange(GPUMapMode.Write) { view ->
    view.setUInts(0uL, uintArrayOf(1u, 2u, 3u, 4u))
}
```

## Browser interop

Texture ownership is explicit: `Texture.wrapOwned(handler)` takes over the destruction of the
handle, `Texture.wrapBorrowed(handler)` never destroys it. The previous
`Texture(handler, canBeDestroy)` constructor and the `canBeDestroy` property are deprecated but
kept as a migration bridge. `CanvasSurface` is `AutoCloseable`: `close()` unconfigures the canvas
context and does not own the device passed to `configure`. The `handler` property of the browser
wrappers is an interop escape hatch: calling operations on it directly can invalidate the
wrapper's contract.

## Handoff to the Dawn backend

Conceptual compatibility with Dawn and native-backend validation are two distinct results. The
native validation is **not executed in this repository**; run the new portable cases on the
native backend in a session explicitly authorized for that repository.

Obligations for the Dawn backend:

- **Nullable sequence slots.** Keep the indices: determine, in the pinned C header, the
  empty-slot representation of each array (null handle, undefined format, empty
  attachment/layout structure); do not collapse every structure to a null pointer.
- **Requested limits.** Initialize absent limits with the proper sentinels of the header in use,
  distinguishing 32-bit and 64-bit fields; do not assume a zeroed C struct is an empty request.
- **Device loss.** Register the loss callback at device creation, connect it to a durable
  shared result, keep the callback data alive until no callback can run anymore, and make the
  cancellation of one observer independent of the device.
- **Lifetime.** Define `Destroy` and the release of the owned reference, without double release.
- **Cancellation.** Handle late callbacks after a cancellation and the lifetime of their
  userdata.
- **Usage masks.** Return masks without losing bits.
