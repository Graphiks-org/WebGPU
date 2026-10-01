# ArrayBuffer bounds and capacities

`ArrayBuffer` keeps its public sizes in `ULong`, but each implementation only addresses a bounded
logical capacity. This page is the contract the implementation and its tests enforce. The measured
cost of the checks is in [ArrayBuffer CPU performance](arraybuffer-performance.md).

## Representable sizes

| Target | Logical capacity | Notes |
| --- | --- | --- |
| Web (JS/Wasm) | `Int.MAX_VALUE` bytes | the typed-array offset is an `Int` |
| Android | `Int.MAX_VALUE` bytes | the direct `ByteBuffer` capacity is an `Int` |
| Native | `Int.MAX_VALUE` bytes | owned and **borrowed** wrapping; large external pointers need another addressing scheme |
| JVM | `Long.MAX_VALUE` bytes | a `MemorySegment` is addressed by `Long` |

These are the limits of the current representations, not the platforms' theoretical capacities, and
they do not promise that an allocation succeeds. `allocate(0uL)` and `of(emptyArray)` work on every
target. The JVM can hold a segment larger than a Kotlin array; converting it with `to*Array()`
refuses that case with `IllegalArgumentException`.

Sizes are computed in a wide type after a `count <= maximum / elementWidth` check, so an overflowing
product is never evaluated.

## Exceptions

| Condition | Exception |
| --- | --- |
| allocation or wrapping size not representable on the target | `IllegalArgumentException` |
| access range outside the declared size | `IndexOutOfBoundsException` |
| unaligned offset for an element wider than one byte | `IllegalArgumentException` |
| size not divisible by the element width, or element count too large for a Kotlin array | `IllegalArgumentException` |
| a real allocation failure | the platform error, not normalised into a range error |

## Validation order and empty operations

An access validates the range, then the alignment of a non-empty operation, then the numeric
conversion, and only then touches memory. A bulk copy validates its whole range once, outside the
copy loop. A write rejected by a precondition leaves the whole memory unchanged; this is not a
transactional guarantee against concurrent external changes.

A zero-length operation at `offset == size` is valid, and one past it fails. Empty operations never
dereference a pointer and never create a typed view that would require alignment: `setBytes(0uL,
byteArrayOf())` and `ArrayBuffer.allocate(0uL).toIntArray()` are valid on every target, and the Native
implementation allocates a one-byte sentinel for a zero-length owned buffer without exposing it.

## Scope of the guarantees

The checks protect the range the buffer declares. A wrapped pointer must still designate a real,
large enough and live memory region for the duration of the access. Ownership, mapping/unmapping and
concurrency are not redefined here. The `of` sharing/copy policy and the endianness are unchanged:
`of` shares the backing store in JS and copies on the other targets, and RGBA8 is written as four
bytes rather than an implicit-order `Int` because Android's byte order differs.

## Evidence

The contract is covered by the common tests in `webgpu-api` (arithmetic helpers, allocation,
scalar and bulk bounds, empty conversions) plus the platform tests for the JVM `MemorySegment` and
the Native pointer, and by the Android instrumented checks in `arraybuffer-benchmarks-android`
(`ArrayBufferAndroidTest`/`runSafetyChecks`). Run them with:

```sh
./gradlew :webgpu-api:jvmTest :webgpu-api:jsNodeTest :webgpu-api:wasmJsNodeTest :webgpu-api:macosArm64Test
./gradlew :arraybuffer-benchmarks-android:connectedReleaseAndroidTest
```
