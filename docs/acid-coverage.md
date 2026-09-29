# Acid coverage and remaining contract gaps

This document is the balance of the portable acid-test catalogue after the finalization increment. It
records what is validated, what is optional, what could not be exercised in the reference environment
and what remains uncovered. It is a coverage statement for a defined catalogue, not a WebGPU
conformance certificate.

The catalogue lives in `suite-acid-tests`; each case is one file annotated with `@AcidTest`, and the
runner is `suite-browser`. The generated inventory (`suite-acid-tests/build/suite-inventory/`) and
the published site are build outputs, never edited by hand.

## Catalogue

There are **123 cases**: **118 mandatory** and **5 optional**. A case is optional only when the
contract feature it needs is itself optional; the feature is declared in `requiredFeatures`, never
hidden.

| Optional case | Feature | Status in the reference environment |
| --- | --- | --- |
| `compute.shader-f16` | `ShaderF16` | `unsupported` (feature absent) |
| `render.indirect-first-instance` | `IndirectFirstInstance` | passed |
| `queries.timestamp-resolve` | `TimestampQuery` | passed |
| `query.render-timestamp-writes` | `TimestampQuery` | passed |
| `texture.view-swizzle` | `TextureComponentSwizzle` | passed |

The five optional cases cover four distinct features: `TimestampQuery` is used by two cases,
`TextureComponentSwizzle` by one, `ShaderF16` by one and `IndirectFirstInstance` by one.

The runner guards every device against a feature it was asked for but did not receive: the requested
set must be a subset of `adapter.features`, and after `requestDevice` it must also be a subset of
`device.features`.

## Execution result

On the two browser targets, with Chromium 153.0.8010.12 / SwiftShader on `darwin`:

| Target | Passed | Unsupported | Failed | Total |
| --- | ---: | ---: | ---: | ---: |
| JS | 122 | 1 | 0 | 123 |
| Wasm JS | 122 | 1 | 0 | 123 |

All **118 mandatory cases pass** on both targets. The only non-passing case is optional:
`compute.shader-f16` is `unsupported` because the environment lacks `ShaderF16`. It is not presented
as evidence of support.

The pinned Playwright/Chromium was updated to a release that implements the contract's four-character
`DOMString` swizzle, so the optional `texture.view-swizzle` case is now executed rather than reported
as an environment gap.

Full command list, environment metadata and the raw reports are recorded in
[verification.md](verification.md).

## Disposition of the original uncovered behaviours

The inventory started this increment with 23 uncovered entries. Each is now either covered by cases,
kept as a precise residual, or split. "Covered" means the case's assertions exercise the detailed
behaviour; it does not mean every possible input is tested.

| Original entry | Disposition |
| --- | --- |
| `adapter.request-and-capabilities` | Case `adapter.request-and-capabilities`; low-power hint and observable capabilities. |
| `device.features-and-limits` | Cases A plus the per-device feature guard; residual kept (limits pinned, not enumerated). |
| `device.request-required-features` | Related optional cases request and use their features; residual kept (refusal of an absent feature). |
| `command.encoder-recording` | Covered by the B/E cases: order, finish, invalid reuse and labels. |
| `command.debug-markers` | Case `command.debug-markers`; debugger display is not observable through the common API. |
| `transfers.texture-copy-aspect` | Case `transfers.texture-copy-aspect` (Depth32Float DepthOnly); StencilOnly copy remains in `texture.view-usage-aspect`. |
| `shader.compilation-info` | Cases `shader.compilation-valid` / `shader.compilation-invalid`; exact positions/texts are not portable. |
| `compute.pipeline-async` | Case `compute.pipeline-async`; ran and rejected without a captured scope error. |
| `texture.creation` | Case `texture.creation-metadata`; residual `textureBindingViewDimension` and creation constraints kept. |
| `texture.view-usage-aspect` | Case `texture.view-usage-restriction` and `texture.depth-aspect-load`; StencilOnly kept as residual. |
| `texture.view-swizzle` | Case `texture.view-swizzle` (optional); not validated in the reference environment. |
| `sampler.comparison` | Case `sampler.comparison` (Less at a uniform depth); PCF and other compare functions remain residual. |
| `texture.usage-and-formats` | Split: exercised usages/formats are tied to their cases; other formats and `TransientAttachment` remain residual. No enum-entry or flag-operator test was added. |
| `render.color-resolve` | Covered by the existing MSAA case and `render.volume-depth-slice`. |
| `render.pass-state` | Case `render.max-draw-count`, with an accepted witness. |
| `render.primitive-and-multisample` | Covered by the D/F and existing cases; points, lines and partial masks remain residual. |
| `render.depth-bias` | Cases `depth.bias-slope-clamp` and `depth.bias-constant`, with colour oracles. |
| `render.pipeline-async` | Case `render.pipeline-async`; rendered and rejected. |
| `query.render-timestamp-writes` | Case `query.render-timestamp-writes`; temporal precision is not proven. |
| `errors.uncaptured-error` | Case `errors.uncaptured-error` on an isolated device, Validation category. |
| `errors.device-lost` | Blocked by the contract: no `GPUDevice.lost` and no common loss callback. |
| `async.promise-results` | Renamed to `async.oom-and-internal-results`: OOM/Internal errors are not portable; the pipeline `Result` paths are tested. |
| `data.identifiers-and-indices` | References vented into the offset/region/index cases, including a negative `baseVertex`; 64-bit range remains residual. |

## Remaining residual inventory

These entries stay in `inventory/uncovered-behaviours.json` and are shown as *to be tested* on the
Validation page. The count is a result of scoping, not a target to drive to zero.

- `device.features-and-limits`, `device.request-required-features` — the full limit space and the
  deterministic refusal of an absent feature.
- `features.compressed-and-tiered`, `features.subgroups`,
  `features.not-exposed-by-reference-browser` — optional features declared in the contract but not
  exercised, including any the reference browser does not expose.
- `immediates.set-immediates` — `setImmediates` has a public signature and `maxImmediateSize` a
  limit, but no dedicated optional feature; a zero limit or an older browser cannot be reported as a
  missing feature. Extending it needs a capabilities/contract work item and is out of this catalogue.
- `texture.creation`, `texture.view-usage-aspect`, `texture.usage-and-formats`,
  `textures.storage-constraints`, `formats.depth-and-packed`, `sampling.limits` — texture creation
  constraints, StencilOnly views, compressed/tiered/packed/depth formats, storage-texture
  constraints and sampler limits.
- `render.primitive-and-multisample`, `render.discard-reinit`, `bundles.negative-validation` —
  points/lines and partial masks, discard-then-reinitialise, render-bundle negative validation.
- `errors.device-lost` — contract gap, not a missing test.
- `async.oom-and-internal-results` — not portably provokable.
- `data.identifiers-and-indices` — the full 64-bit range of the size/index/coordinate aliases.

## Known limits of this evidence

- **Timestamp observability.** Removing `timestampWrites` from the render pass leaves the
  `query.render-timestamp-writes` case green, because `resolveQuerySet` replaces the sentinel bytes
  even for a query the pass never wrote. The case proves valid query use, resolution and range
  preservation, not that a measurable time was recorded; no `end > begin` or `> 0` threshold is
  imposed.
- **Software backend.** All results above are functional SwiftShader results. They are not physical
  GPU results and not performance measurements.
- **`--backend=default`** only means "no SwiftShader flags"; it is never reported as hardware.
- **Native targets.** Native GPU execution belongs to the binding repositories. This repository
  compiles the shared modules for JVM, JS, Wasm JS, Linux x64 and macOS ARM64 and runs the browser
  cases only.
