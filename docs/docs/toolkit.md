# The WebGPU toolkit

The toolkit is the opt-in layer over the WebGPU bindings: it owns the session lifecycle,
presentation, and the extension contract that lets a binding repository front the same helpers.
This guide shows the dependency coordinates, the session and presentation entry points, the
extension contract, and how an extension is validated with the contract kit.

## Add a module

The published coordinates use the `org.graphiks` group. Choose the modules your application needs:

```kotlin
dependencies {
    implementation("org.graphiks:webgpu-toolkit-core:<version>")
    // Presentation and window contracts.
    // implementation("org.graphiks:webgpu-toolkit-surface:<version>")
    // Browser extension presenting to HTML canvases.
    // implementation("org.graphiks:webgpu-toolkit-web:<version>")
    // Protocol contract kit.
    // implementation("org.graphiks:webgpu-toolkit-tck:<version>")
}
```

Replace `<version>` with an available release or snapshot version. The repository's development
default is `0.1.0-SNAPSHOT`; that value does not imply a release has already been published.

`webgpu-toolkit-core`, `webgpu-toolkit-surface` and `webgpu-toolkit-tck` follow the shared target
matrix (JVM, Android, JS, Wasm JS, and the configured Kotlin/Native targets); `webgpu-toolkit-web`
configures JS and Wasm JS. The entry points live in `org.graphiks.webgpu.toolkit`, the presentation
helpers in `org.graphiks.webgpu.toolkit.surface`, the web extension in
`org.graphiks.webgpu.toolkit.web`, and the contract kit in `org.graphiks.webgpu.toolkit.tck`.

## Open a headless session

`withSession` opens a session through an extension, runs the block with the live device, and shuts
everything down on every exit path, including cancellation of the caller:

```kotlin
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUQueue
import org.graphiks.webgpu.toolkit.WebGpuToolkitExtension
import org.graphiks.webgpu.toolkit.withSession

suspend fun <R> headless(
    extension: WebGpuToolkitExtension,
    block: suspend (GPUDevice, GPUQueue) -> R,
): R =
    withSession(extension) { session ->
        session.state.value // `Ready`: the device is live and admits work
        block(
            session.device, // borrowed: the session closes it, callers never do
            session.queue, // the primary queue of session.device
        )
    }
```

Startup is bounded by a timeout and maps every failure to the `Failed` state before rethrowing.
`session.state` publishes the lifecycle as it progresses: `Ready`, then `ShuttingDown` while
handles are released, ending in `Closed`; a device loss publishes `Lost` and cancels admitted work
with `DeviceLostException`. A session whose shutdown exceeds its timeout publishes
`ShutdownTimedOut` while cleanup keeps running; calling `shutdown()` again retries the release.

## Present to a target

`withSurface` runs the target-lease protocol — retaining a lease, requesting a target-bound
adapter, negotiating the surface configuration from the capabilities — and hands the block the
session and the resulting surface:

```kotlin
import org.graphiks.webgpu.toolkit.surface.withSurface
import org.graphiks.webgpu.toolkit.web.CanvasSurfaceTarget
import org.graphiks.webgpu.toolkit.web.webToolkit

suspend fun presentToCanvas(canvas: HTMLCanvasElement) {
    withSurface(webToolkit(), CanvasSurfaceTarget(canvas)) { session, surface ->
        surface.configuration // StateFlow<PresentationConfiguration?>: negotiated format and alpha mode
        surface.withFrame { frame ->
            // render into frame.view at frame.extent with session.device
            frame.present() // optional: withFrame presents on a normal return
        }
    }
}
```

`withFrame` suspends while the target cannot be drawn to — unavailable, or a zero extent — and
releases the frame on any exit path; a block that returns normally has its frame presented exactly
once unless it presented explicitly, and a second `present()` fails. On web, `webToolkit()` fronts
the browser implementation and `CanvasSurfaceTarget(canvas)` wraps a canvas of the
`org.graphiks.webgpu.browser.HTMLCanvasElement` type: the target observes the canvas with a
`ResizeObserver` and republishes its extent — moving to a fresh generation — when the canvas
resizes, so the `state` flow genuinely emits and a suspended `withFrame` resumes; when the browser
offers no `ResizeObserver`, every read of the target's `state` refreshes the extent instead (the
pull fallback). An extent change still bumps the generation, which kills the surfaces leased
against the old one in v1: their next use fails fast, and a fresh `withSurface` pass negotiates a
new surface. The suite browser's `?demo=toolkit-particles` route is the living integration example
of this pattern; `?demo=particles` runs the animated variant of the same scene.

## Open a window

On desktop targets, a `WindowHost` provider opens windowing-neutral `Window`s. `withWindow`
composes the window lifetime, the session and the negotiated surface:

```kotlin
import org.graphiks.webgpu.toolkit.surface.WindowConfig
import org.graphiks.webgpu.toolkit.surface.withWindow

suspend fun runInWindow(extension: WebGpuToolkitExtension, host: WindowHost) {
    withWindow(extension, host, WindowConfig(title = "toolkit")) { session, window, surface ->
        window.events // close requests and resizes published by the window
        surface.withFrame { frame ->
            // render into frame.view at frame.extent
            frame.present()
        }
    }
}
```

This repository defines the window contract only: how a window is concretely opened is at each
native extension's discretion — hosts are provider artifacts published wherever their extension
lives, never here. When the host owns its windows
(`WindowRuntime.Owned`), `withWindow` destroys the window on exit; a borrowed window
(`WindowRuntime.Borrowed`) stays open, its lifetime belonging to the embedder that lent it.

## The extension contract

An extension is everything a backend supplies; `WebGpuToolkitExtension` is five members deep:

1. `id` names the binding the extension fronts, such as `"web"`.
2. `open(config)` starts a `ToolkitInstance`, the session's adapter and surface factory.
3. `requestAdapter(hints)` acquires an adapter; `requestAdapter(forTarget, hints)` acquires one
   that can present to the exact retained target.
4. `createSurface(adapter, lease)` creates the surface handle for a retained target: it publishes
   its capabilities and configures, acquires, presents and releases frames.
5. `progression` and `pumpEvents` say how host events advance: `Pumped` backends are pumped by the
   session, `EventLoop` backends ride the host loop (`Manual` progression is rejected in v1).

## Where adapters live

The toolkit ships the contract and the web extension. The dawn4k and wgpu4k repositories publish
their toolkit extensions alongside their bindings; this repository ships only the web extension
(`webToolkit()` in `webgpu-toolkit-web`). Window hosts follow the same split: nothing here names
or prescribes a windowing system — each native extension opens windows however it sees fit and
publishes its host artifact alongside itself.

## Validate an extension with the TCK

`webgpu-toolkit-tck` runs the protocol cases any extension must honor.
`ProtocolTck.runAll(extension, target)` returns a `TckReport` whose `failures` list carries
one line per violated case instead of throwing on the first, so a binding repository can run it
against its own extension in its tests and see every violation at once. The kit's
`SimulatedToolkitExtension` is the scripted counterpart that proves the strict paths in this
repository (lifecycle walk, idempotent shutdown, present-once, zero-extent suspension, stale-lease
rollback, loss to `Lost`); cases that need scripting skip for fixtures that cannot script them, so
an empty report certifies the cases that ran. GPU-real cases — presenting through a live graphics
stack — are v2.

See [Architecture](architecture.md) for the module boundaries and [Getting started](getting-started.md)
for the underlying browser API.
