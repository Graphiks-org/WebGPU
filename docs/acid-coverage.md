# Acid coverage and remaining contract gaps

This document is the balance of the portable acid-test catalogue. It
records what is validated, what is optional, what could not be exercised in the reference environment
and what remains uncovered. It is a coverage statement for a defined catalogue, not a WebGPU
conformance certificate.

The catalogue lives in `suite-acid-tests`; each case is one file annotated with `@AcidTest`, and the
runner is `suite-browser`. The generated inventory (`suite-acid-tests/build/suite-inventory/`) and
the published site are build outputs, never edited by hand.

## Catalogue

There are **147 cases**: **138 mandatory** and **9 optional**. A case is optional only when the
contract feature it needs is itself optional; the feature is declared in `requiredFeatures`, never
hidden.

| Optional case | Feature | Status in the reference environment |
| --- | --- | --- |
| `compute.shader-f16` | `ShaderF16` | `unsupported` (feature absent) |
| `render.indirect-first-instance` | `IndirectFirstInstance` | passed |
| `queries.timestamp-resolve` | `TimestampQuery` | passed |
| `query.render-timestamp-writes` | `TimestampQuery` | passed |
| `errors.compute-timestamp-indices` | `TimestampQuery` | passed |
| `errors.render-timestamp-indices` | `TimestampQuery` | passed |
| `texture.view-swizzle` | `TextureComponentSwizzle` | passed |
| `features.compressed-bc` | `TextureCompressionBC` | passed |
| `features.subgroups` | `Subgroups` | passed |

The nine optional cases cover six distinct features: `TimestampQuery` is used by four cases,
`TextureComponentSwizzle` by one, `ShaderF16` by one, `IndirectFirstInstance` by one,
`TextureCompressionBC` by one and `Subgroups` by one.

The runner guards every device against a feature it was asked for but did not receive: the requested
set must be a subset of `adapter.features`, and after `requestDevice` it must also be a subset of
`device.features`.

## Execution result

On the two browser targets, with Chromium 153.0.8010.12 / SwiftShader on `darwin`:

| Target | Passed | Unsupported | Failed | Total |
| --- | ---: | ---: | ---: | ---: |
| JS | 146 | 1 | 0 | 147 |
| Wasm JS | 146 | 1 | 0 | 147 |

All **138 mandatory cases pass** on both targets. The only non-passing case is optional:
`compute.shader-f16` is `unsupported` because the environment lacks `ShaderF16`. It is not presented
as evidence of support.

Full command list, environment metadata and the raw reports are recorded in
[verification.md](verification.md).

## Remaining residual inventory

These entries stay in `inventory/uncovered-behaviours.json` and are shown as *to be tested* on the
Validation page. The count is a result of scoping, not a target to drive to zero.

- `device.request-required-features` — the deterministic refusal of an absent optional feature has
  no portable case: which feature is absent varies with the environment.
- `features.compressed-and-tiered` — BC1 block decode is exercised with `TextureCompressionBC`; the
  ETC2 and ASTC compressed features and the tier1/tier2 format features are declared in the
  contract but not exercised.
- `texture.creation` — view-dimension and layer-count constraints are exercised; the remaining
  creation constraints, such as mip-level bounds and sample-count rules, are not.
- `texture.usage-and-formats` — `TransientAttachment` is exercised with its companion constraint and
  compressed formats through the compressed-bc case; the tiered formats and the remaining
  format-and-usage pairs are not.
- `textures.storage-constraints` — format-side storage refusals are exercised; the per-format
  access constraints beyond the exercised write-only case are not.
- `formats.depth-and-packed` — `Depth16Unorm` and packed `RGB10A2Uint` targets are exercised; the
  feature-gated packed and depth formats, such as `rg11b10ufloat` and `depth32float-stencil8`, are
  not.
- `sampling.comparison-pcf` — the filter-independent comparison envelope and both comparator
  directions are exercised over a split depth; the exact filtered fraction is
  implementation-dependent and deliberately not asserted.
- `data.identifiers-and-indices` — full-range 32-bit draw indices and the refused texture width
  are exercised; the full 64-bit extent of the size and offset aliases is not.

## Known limits of this evidence

- **Timestamp observability.** Removing `timestampWrites` from the render pass leaves the
  `query.render-timestamp-writes` case green, because `resolveQuerySet` replaces the sentinel bytes
  even for a query the pass never wrote. The case proves valid query use, resolution and range
  preservation, not that a measurable time was recorded; no `end > begin` or `> 0` threshold is
  imposed.
- **Comparison filtering.** The exact filtered result of a comparison sampler is
  implementation-dependent; `sampler.comparison-pcf` asserts the filter-independent envelope —
  references beyond both depths answered differently by `Less` and `Greater` — and the single-tap
  binary results, never a fractional value.
- **Compressed decode path.** On the reference backend the decoded texel of a compressed texture is
  observed through fragment sampling; a compute-side load returns zeros, so `features.compressed-bc`
  renders a sampling quad instead of loading texels.
- **Discarded attachments.** A render attachment stored with `GPUStoreOp.Discard` then attached with
  `Load` is asserted to read as transparent black `(0,0,0,0)`: the pinned specification guarantees
  the discarded subregion is cleared to zero, including for later `Load` attachments.
- **Software backend.** All results above are functional SwiftShader results. They are not physical
  GPU results and not performance measurements.
- **`--backend=default`** only means "no SwiftShader flags"; it is never reported as hardware.
- **Native targets.** Native GPU execution belongs to the binding repositories. This repository
  compiles the shared modules for JVM, JS, Wasm JS, Linux x64 and macOS ARM64 and runs the browser
  cases only.
- **Never-testable behaviours.** Device loss, `setImmediates`, out-of-memory/internal errors and
  features the reference browser does not expose have no portable case in this contract — no loss
  access path, no dedicated optional feature, not provokable portably. They are not tracked as
  residuals; they are permanent limits of the catalogue.
