package org.graphiks.webgpu.suite.demos.reactiondiffusion

private val ComputeBindings = """
    @group(0) @binding(0) var inputState: texture_2d<f32>;
    @group(0) @binding(1) var outputState: texture_storage_2d<rgba32float, write>;
    @group(0) @binding(2) var<uniform> parameters: vec4<f32>;
""".trimIndent()

/** The exact WGSL compiled by the scene and displayed by the learning view. */
val ReactionSimulationShader = ComputeBindings + "\n" + """
    // The sampled texture is read-only; the other texture receives the next state.
    // Periodic boundaries connect opposite edges of the grid.
    fn readState(p: vec2<i32>) -> vec2<f32> {
        return textureLoad(inputState, (p + vec2<i32>(256)) % vec2<i32>(256), 0).rg;
    }
    @compute @workgroup_size(8, 8)
    fn simulate(@builtin(global_invocation_id) id: vec3<u32>) {
        if (id.x >= 256u || id.y >= 256u) { return; }
        let p = vec2<i32>(id.xy);
        let s = readState(p);
        let orthogonal = readState(p + vec2<i32>(-1, 0)) + readState(p + vec2<i32>(1, 0))
            + readState(p + vec2<i32>(0, -1)) + readState(p + vec2<i32>(0, 1));
        let diagonal = readState(p + vec2<i32>(-1, -1)) + readState(p + vec2<i32>(1, -1))
            + readState(p + vec2<i32>(-1, 1)) + readState(p + vec2<i32>(1, 1));
        let lap = -s + 0.2 * orthogonal + 0.05 * diagonal;
        // Gray-Scott, Euler step dt=1, diffusion rates DA=1 and DB=0.5.
        let reaction = s.x * s.y * s.y;
        let next = s + vec2<f32>(lap.x - reaction + parameters.x * (1.0 - s.x),
            0.5 * lap.y + reaction - (parameters.y + parameters.x) * s.y);
        textureStore(outputState, p, vec4<f32>(clamp(next, vec2<f32>(0.0), vec2<f32>(1.0)), 0.0, 1.0));
    }
""".trimIndent()

val ReactionBrushShader = ComputeBindings + "\n" + """
    // A separate pass injects B without advancing the reaction, even when paused.
    // This pass has its own uniform buffer: painting cannot overwrite simulation parameters.
    @compute @workgroup_size(8, 8)
    fn paint(@builtin(global_invocation_id) id: vec3<u32>) {
        if (id.x >= 256u || id.y >= 256u) { return; }
        let p = vec2<i32>(id.xy);
        let raw = abs(vec2<f32>(id.xy) - parameters.xy);
        let d = min(raw, vec2<f32>(256.0) - raw);
        var s = textureLoad(inputState, p, 0).rg;
        if (dot(d, d) <= parameters.z * parameters.z) { s = vec2<f32>(0.5, 1.0); }
        textureStore(outputState, p, vec4<f32>(s, 0.0, 1.0));
    }
""".trimIndent()

val ReactionRenderShader = """
    @group(0) @binding(0) var state: texture_2d<f32>;
    @group(0) @binding(1) var<uniform> appearance: vec4<f32>;
    struct VertexOutput {
        @builtin(position) position: vec4<f32>,
        @location(0) uv: vec2<f32>,
    }
    @vertex fn vertexMain(@builtin(vertex_index) index: u32) -> VertexOutput {
        let vertices = array<vec2<f32>, 3>(vec2<f32>(-1.0, -1.0), vec2<f32>(3.0, -1.0), vec2<f32>(-1.0, 3.0));
        let p = vertices[index];
        var out: VertexOutput;
        out.position = vec4<f32>(p, 0.0, 1.0);
        // UV origin is top-left, matching pointer and simulation coordinates.
        out.uv = p * vec2<f32>(0.5, -0.5) + vec2<f32>(0.5);
        return out;
    }
    @fragment fn fragmentMain(in: VertexOutput) -> @location(0) vec4<f32> {
        // textureLoad needs no sampler or optional float32 filtering feature.
        let p = clamp(vec2<i32>(in.uv * 256.0), vec2<i32>(0), vec2<i32>(255));
        let s = textureLoad(state, p, 0).rg;
        if (appearance.y == 1.0) { return vec4<f32>(vec3<f32>(s.x), 1.0); }
        if (appearance.y == 2.0) { return vec4<f32>(vec3<f32>(s.y), 1.0); }
        var c0 = vec3<f32>(0.01, 0.03, 0.12);
        var c1 = vec3<f32>(0.0, 0.8, 0.9);
        var c2 = vec3<f32>(1.0);
        if (appearance.x == 1.0) {
            c0 = vec3<f32>(0.0); c1 = vec3<f32>(0.9, 0.05, 0.01); c2 = vec3<f32>(1.0, 0.9, 0.1);
        }
        if (appearance.x == 2.0) { c0 = vec3<f32>(0.0); c1 = vec3<f32>(0.5); }
        // Color is a view of B, never an input to the simulation.
        if (s.y <= 0.5) { return vec4<f32>(mix(c0, c1, s.y * 2.0), 1.0); }
        return vec4<f32>(mix(c1, c2, s.y * 2.0 - 1.0), 1.0);
    }
""".trimIndent()
