# Public API contract

The repository is **incubating**: the API and published coordinates may evolve, and signature
breaks ship without a migration path. This page records the current contract so a backend
implementer (and the next contributor) knows exactly what to implement. For day-to-day usage,
see [Getting started](getting-started.md).

## Nullable sequence slots

The five `sequence<T?>` members of the versioned IDL keep their element nullability end to end:

- `GPUPipelineLayoutDescriptor.bindGroupLayouts: List<GPUBindGroupLayout?>`
- `GPUFragmentState.targets: List<GPUColorTargetState?>`
- `GPUVertexState.buffers: List<GPUVertexBufferLayout?>`
- `GPURenderPassDescriptor.colorAttachments: List<GPURenderPassColorAttachment?>`
- `GPURenderPassLayout.colorFormats: List<GPUTextureFormat?>`

A list keeps its length and its indices all the way into the JavaScript array; never use
`filterNotNull()` to convert these properties.

## Requested limits

`GPUDeviceDescriptor.requiredLimits` is `GPURequiredLimits?`: `null` means "no constraint", an
explicit zero stays an explicit value. `RequiredLimits` mirrors every property of
`GPUSupportedLimits` as a nullable property with a `null` default; the browser only emits the
keys that are not null. `adapter.limits` and `device.limits` keep the complete, non-nullable
`GPUSupportedLimits` model.

## Device loss

`GPUDevice.awaitLost(): Result<GPUDeviceLostInfo>` is the portable, cancellable observation of
the device loss. The loss is a successful result carrying a reason and a message, not a `Result`
failure; only an interop failure is a failure. Several observers see the same loss, an observer
cancelled before the loss neither blocks the other observers nor destroys the device, and an
observer that starts after the loss resolves immediately. Closing the device explicitly triggers
the loss notification with the destruction reason when the backend provides one.

## Usage masks

`GPUBuffer.usage` returns `GPUBufferUsage` and `GPUTexture.usage` returns `GPUTextureUsage`
(value-class masks wrapping a `ULong`). `a in mask` means "every bit of `a` is present in
`mask`", so `None` is contained in every mask. `fromBits` preserves unknown bits and performs no
GPU validation.

## Resource lifetimes and scoped mapping

Resources created by a device are owned by the caller: `close()` destroys them (a native backend
must also release its owned reference), and closing an already closed resource does not release
the same reference twice. Handles without a WebGPU `destroy` operation only release the owned
reference on `close()`. Mapped ranges are borrowed and invalidated by `unmap()` or destruction.
A canvas texture is borrowed: closing its wrapper does not destroy the canvas-owned texture.

`GPUBuffer.withMappedRange(mode, offset, size) { view -> ... }` maps a range, runs a
non-suspending block with the borrowed view and unmaps in a `finally`.

## Browser interop

Texture ownership is explicit: `Texture.wrapOwned(handler)` takes over the destruction of the
handle, `Texture.wrapBorrowed(handler)` never destroys it. `CanvasSurface` is `AutoCloseable`:
`close()` unconfigures the canvas context and does not own the device passed to `configure`. The
`handler` property of the browser wrappers is an interop escape hatch: calling operations on it
directly can invalidate the wrapper's contract.

## Handoff to the Dawn backend

Conceptual compatibility with Dawn and native-backend validation are two distinct results. The
native validation is **not executed in this repository**; run the portable suite cases on the
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
