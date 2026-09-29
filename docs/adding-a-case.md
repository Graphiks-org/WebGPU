# Adding a validation case

An acid test is a small, readable function that exercises one observable behaviour of the public
WebGPU contract. It takes a `GPUDevice`, creates the resources it needs, asserts on data, errors or
observable state, and closes what it created. The suite has no mock GPU and no framework of its own:
the case is the deliverable, and a browser run is what validates it.

## 1. Write the case and annotate it

Use **one file per case**, named after it, in the package of its family under
`suite-acid-tests/src/commonMain/kotlin/org/graphiks/webgpu/suite/acid/<packageName>/`. The package
must match the family's `AcidFamily.packageName` (for example `AcidFamily.BuffersMapping` →
`...suite.acid.buffers`); the generator rejects a mismatch.

Declare the inventory metadata with `@AcidTest`. The id and the family are enums, and the contract
references the generated `ApiSymbols` constants, so a typo does not compile.

```kotlin
package org.graphiks.webgpu.suite.acid.buffers

import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.descriptors.BufferDescriptor
import org.graphiks.webgpu.suite.AcidCaseId
import org.graphiks.webgpu.suite.AcidFamily
import org.graphiks.webgpu.suite.AcidTest
import org.graphiks.webgpu.suite.acid.ApiSymbols
import org.graphiks.webgpu.suite.acid.withValidationScope
import kotlin.test.assertEquals

@AcidTest(
    id = AcidCaseId.BuffersSize,
    family = AcidFamily.BuffersMapping,
    contract = [ApiSymbols.GPUDevice_createBuffer, ApiSymbols.GPUBuffer_size],
)
suspend fun bufferSize(device: GPUDevice) = withValidationScope(device) {
    val buffer = device.createBuffer(BufferDescriptor(16uL, GPUBufferUsage.CopySrc))
    try {
        assertEquals(16uL, buffer.size)
    } finally {
        buffer.close()
    }
}
```

Most cases take the runner's borrowed `GPUDevice`. A case that must observe adapter capabilities, a
device request or its own uncaptured-error callback declares the full context instead:

```kotlin
@AcidTest(
    id = AcidCaseId.SomeAdapterCase,
    family = AcidFamily.AdapterDeviceFeaturesLimits,
    contract = [ApiSymbols.GPUAdapter_requestDevice],
    input = AcidInput.Context,
)
suspend fun someAdapterCase(context: AcidContext) {
    val adapter = context.requestAdapter(RequestAdapterOptions()).getOrThrow()
    try {
        adapter.requestDevice(DeviceDescriptor(label = "acid-device")).getOrThrow().use { device ->
            // The case owns and closes this extra device; context.device stays borrowed.
        }
    } finally {
        adapter.close()
    }
}
```

`context.requestAdapter` returns a **fresh** adapter on every call. The individual case functions
still take a `GPUDevice`, so a binding that only wants the device cases can call them directly.

Rules of thumb:

- Assert on data, error categories or contract states, never on an implementation's exact error text
  or on values produced by the same code under test.
- Close every resource the case creates; do not destroy resources supplied by the runner.
- Use `withValidationScope` for a valid case so an unexpected validation error fails it.
- Shared helpers live in the parent `suite.acid` package (`withValidationScope`) or in a
  `<family>/…Support.kt` file as `internal`; keep the GPU commands of a case visible.
- A failure of the binding stays visible. Do not weaken an assertion to match an observed defect.
- A capability required by the core contract is never hidden behind `requiredFeatures`; that set is
  only for optional features.

## 2. Declare the identity and the texts

If the case is new, add its entry to the `AcidCaseId` enum (and the family to `AcidFamily`, with a
`packageName`, if needed) in `suite-core`. These enums carry the stable dotted ids of the report.

Then add the localized texts in **every** locale file, keyed by the case id:

- `inventory/i18n/behaviours.en.json`
- `inventory/i18n/behaviours.fr.json`

```json
"cases": {
  "buffers.size": {
    "title": "Report the requested buffer size",
    "expectation": "A buffer exposes the size requested at creation."
  }
}
```

A behaviour without a case is listed in `inventory/uncovered-behaviours.json` (its metadata) and its
expectation in each locale file under `behaviours`. Move it into a case when it becomes covered.

## 3. Generate and verify

Do not edit the generated catalogue, `ApiSymbols`, the case-id manifest or the baseline: they are
build outputs. Build and run the two browser targets and read the real output:

```sh
./gradlew :suite-browser:jsBrowserDistribution :suite-browser:wasmJsBrowserDistribution
node tools/run-browser.mjs js suite-browser/build/dist/js/productionExecutable
node tools/run-browser.mjs wasm suite-browser/build/dist/wasmJs/productionExecutable
```

The runner fails when a case does not pass, when a generated case id is missing or duplicated, or
when a page error is reported. Regenerate the site inventory with `node tools/build-site.mjs`, then
record the observed result in `docs/verification.md`.
