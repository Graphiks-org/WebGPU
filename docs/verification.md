# Verification

This document records the reference contract, the implemented coverage, and the evidence observed
while building the Graphiks WebGPU Suite foundation increment. It is a report of what was actually
run, not a conformance certificate.

## Reference contract

- API and suite version: `0.1.0-SNAPSHOT`.
- Reference commit and source hashes: [`inventory/baseline.json`](../inventory/baseline.json).
- The inventory is extracted from the seven `commonMain` files of `webgpu-api` (not the JVM ABI
  snapshot alone), so it represents the shared signature surface.

## Inventory

`inventory/symbols.tsv` lists **834 declarations** across 14 families. `inventory/behaviors.json`
describes **54 observable behaviours**. The number of symbols is a measure of surface area only:
it is not a conformance percentage, and a symbol being listed never means it is tested.

| Family | Declarations |
| --- | ---: |
| textures/views/samplers | 220 |
| pipelines/render state | 183 |
| types de données/descripteurs/flags/swizzle | 115 |
| adapter/device/features/limits | 86 |
| rendu/passes/attachments | 39 |
| shaders/compilation | 35 |
| buffers/mapping | 33 |
| bind groups/layouts | 32 |
| erreurs/asynchronisme | 22 |
| transferts buffers/textures | 18 |
| compute | 17 |
| queries/timestamps | 15 |
| queue/commandes | 11 |
| render bundles | 8 |
| **Total** | **834** |

Families with no executable case in this increment: adapter/device/features/limits,
textures/views/samplers, rendu/passes/attachments, pipelines/render state, render bundles,
queries/timestamps. Their behavioural analysis remains to be deepened.

## Executable cases

The browser runner executes eleven foundation cases. The mapping from case to behaviour is in
[`inventory/contract.md`](../inventory/contract.md).

| Case | Exercised on |
| --- | --- |
| `buffers.mapped-at-creation` | JS, Wasm |
| `transfers.copy-offsets` | JS, Wasm |
| `transfers.write-offsets` | JS, Wasm |
| `transfers.write-remaining` | JS, Wasm |
| `buffers.partial-map-remap` | JS, Wasm |
| `compute.auto-layout-constants` | JS, Wasm |
| `compute.explicit-layout-entrypoint` | JS, Wasm |
| `errors.empty-scope` | JS, Wasm |
| `errors.invalid-buffer-usage` | JS, Wasm |
| `errors.map-alignment` | JS, Wasm |
| `buffers.map-destroyed` | JS, Wasm |

Execution results, browser version, resolved dependency versions and Maven consumption evidence are
recorded in the sections added by the browser runner and publication tasks.
