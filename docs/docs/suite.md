# Graphiks WebGPU Suite

The Graphiks WebGPU Suite exercises the public Graphiks WebGPU contract with portable acid tests
that run against a real browser WebGPU implementation. The **Validation** page presents the
contract inventory, the behaviour coverage, and the results of the published JS and Wasm runs, and
lets you execute the suite locally.

[Open the Validation page](suite/)

The suite also ships a demonstration gallery. Its first scene, **compute particles**, updates a
particle buffer with a compute pass and draws that same buffer as an instanced vertex buffer.

[Open the demos](suite/demos/)

The suite also ships two portable benchmark workloads. They measure `queue.writeBuffer` and the
encoding and submission of a compute load, and publish their raw samples with the protocol, the
profile and the environment. Each target and profile is shown separately; a duration is an
observation of CPU issue and completion, not pure GPU time.

[Open the benchmarks](suite/benchmarks/)

The protocol and its interpretation limits are documented in
[`docs/benchmarks.md`](https://github.com/Graphiks-org/WebGPU/blob/master/docs/benchmarks.md).

The published results come from identified runs and are kept separate from a local execution. The
recorded evidence is in
[`docs/verification.md`](https://github.com/Graphiks-org/WebGPU/blob/master/docs/verification.md).
