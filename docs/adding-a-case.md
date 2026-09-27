# Adding a validation case

An acid test is a small, readable function that exercises one observable behaviour of the public
WebGPU contract. It takes a `GPUDevice`, creates the resources it needs, asserts on data, errors or
observable state, and closes what it created. The suite has no mock GPU and no framework of its own:
the case is the deliverable, and a browser run is what validates it.

## 1. Write the case

Add a `suspend fun` in `suite-acid-tests/src/commonMain/kotlin/org/graphiks/webgpu/suite/acid/`. A
valid case uses `withValidationScope` so an unexpected validation error fails the case:

```kotlin
package org.graphiks.webgpu.suite.acid

import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.descriptors.BufferDescriptor
import kotlin.test.assertEquals

suspend fun bufferSize(device: GPUDevice) = withValidationScope(device) {
    val buffer = device.createBuffer(BufferDescriptor(16uL, GPUBufferUsage.CopySrc))
    try {
        assertEquals(16uL, buffer.size)
    } finally {
        buffer.close()
    }
}
```

Rules of thumb:

- Assert on data, error categories or contract states, never on an implementation's exact error
  text or on values produced by the same code under test.
- Close every resource the case creates; do not destroy resources supplied by the runner.
- Keep the GPU commands visible. A small private helper is fine when it improves reading; a generic
  runner is not.
- A failure of the binding stays visible. Do not weaken an assertion to match an observed defect.
- A capability required by the core contract is never hidden behind `requiredFeatures`; that set is
  only for optional features.

## 2. Register the case

Add an `AcidCase` to `foundationCases()` in `FoundationCases.kt` with a stable `id`, a readable
title, and the contract members it exercises:

```kotlin
AcidCase(
    id = "buffers.size",
    title = "A buffer reports its requested size",
    contract = listOf("GPUDevice.createBuffer", "GPUBuffer.size"),
    run = ::bufferSize,
),
```

Then list the id in `inventory/foundation-case-ids.json`; the browser runner cross-checks this file
against `FoundationCases.kt` and fails the run when the two disagree. Duplicate ids are rejected.

## 3. Update the inventory

Describe the behaviour in `inventory/behaviors.json` and link the case:

```json
{
  "id": "buffers.size-reported",
  "family": "buffers/mapping",
  "contract": ["GPUDevice.createBuffer", "GPUBuffer.size"],
  "expectation": "Un buffer expose la taille demandée à la création.",
  "requiredFeatures": [],
  "caseIds": ["buffers.size"]
}
```

Link a case only to the behaviours its assertions actually justify. A case that merely creates a
device does not cover device acquisition just by running through it. Regenerate
`inventory/contract.md` after editing behaviours so the coverage and the "to be tested" lists stay
faithful.

## 4. Verify

Run the case on both browser targets and read the real output:

```sh
./gradlew :suite-browser:jsBrowserDistribution :suite-browser:wasmJsBrowserDistribution
node tools/run-browser.mjs js suite-browser/build/dist/js/productionExecutable
node tools/run-browser.mjs wasm suite-browser/build/dist/wasmJs/productionExecutable
```

The runner fails when a case does not pass, when the id set differs from
`inventory/foundation-case-ids.json`, or when a page error is reported. Record the observed result
in `docs/verification.md`.
