# Graphiks WebGPU Suite

The Graphiks WebGPU Suite exercises the public Graphiks WebGPU contract with portable acid tests
that run against a real browser WebGPU implementation. The **Validation** page presents the
contract inventory, the behaviour coverage, and the results of the published JS and Wasm runs, and
lets you execute the suite locally.

[Open the Validation page](suite/)

The demonstration gallery contains three interactive scenes: **compute particles** updates and
renders a shared GPU buffer, **reaction-diffusion** evolves patterns with a Gray–Scott simulation,
and the **cyberpunk city** renders thousands of buildings with instancing and depth testing.
Each scene offers bilingual controls and a learning panel showing the compiled WGSL.

[Open the demos](suite/demos/)

For requirements, commands, controls and native scene integration, see the
[running guide](https://github.com/Graphiks-org/WebGPU/blob/master/docs/running.md).

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
