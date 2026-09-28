package org.graphiks.webgpu.suite.demos.particles

/**
 * Moves every particle by its velocity and bounces it inside the `[-0.95, 0.95]` square.
 *
 * The particle array is the same buffer the render pass uses as an instanced vertex buffer:
 * compute writes it, rendering reads it, with no CPU copy in between. The dispatch may cover more
 * invocations than there are particles, so the guard at the top is what keeps the last partial
 * workgroup inside the buffer.
 */
internal const val ParticleComputeShader = """
struct Particle { position: vec2f, velocity: vec2f }
struct Parameters { delta: f32, aspect: f32, radius: f32, padding: f32 }
@group(0) @binding(0) var<storage, read_write> particles: array<Particle>;
@group(0) @binding(1) var<uniform> parameters: Parameters;

@compute @workgroup_size(64)
fn update(@builtin(global_invocation_id) id: vec3u) {
    if (id.x >= arrayLength(&particles)) { return; }
    var p = particles[id.x];
    p.position += p.velocity * parameters.delta;
    if (p.position.x > 0.95) { p.position.x = 0.95; p.velocity.x = -abs(p.velocity.x); }
    if (p.position.x < -0.95) { p.position.x = -0.95; p.velocity.x = abs(p.velocity.x); }
    if (p.position.y > 0.95) { p.position.y = 0.95; p.velocity.y = -abs(p.velocity.y); }
    if (p.position.y < -0.95) { p.position.y = -0.95; p.velocity.y = abs(p.velocity.y); }
    particles[id.x] = p;
}
"""

/**
 * Draws each particle as an opaque colored disk.
 *
 * The vertex buffer is instanced: every instance reads one particle center and expands it into a
 * two-triangle quad. The simulation square keeps its proportions by scaling the shorter canvas axis,
 * so the square stays a square whatever the window aspect ratio.
 */
internal const val ParticleRenderShader = """
struct Parameters { delta: f32, aspect: f32, radius: f32, padding: f32 }
@group(0) @binding(0) var<uniform> parameters: Parameters;

struct VertexOutput {
    @builtin(position) position: vec4f,
    @location(0) local: vec2f,
    @location(1) color: vec3f,
}

@vertex
fn vertexMain(@location(0) center: vec2f, @builtin(vertex_index) index: u32,
              @builtin(instance_index) instance: u32) -> VertexOutput {
    let corners = array<vec2f, 6>(vec2f(-1,-1), vec2f(1,-1), vec2f(-1,1),
                                 vec2f(-1,1), vec2f(1,-1), vec2f(1,1));
    let local = corners[index];
    let scale = vec2f(min(1.0 / parameters.aspect, 1.0), min(parameters.aspect, 1.0));
    var out: VertexOutput;
    out.position = vec4f((center + local * parameters.radius) * scale, 0, 1);
    out.local = local;
    let shade = f32(instance % 7u) / 6.0;
    out.color = vec3f(0.25 + shade * 0.65, 0.8, 1.0 - shade * 0.4);
    return out;
}

@fragment
fn fragmentMain(input: VertexOutput) -> @location(0) vec4f {
    if (dot(input.local, input.local) > 1.0) { discard; }
    return vec4f(input.color, 1);
}
"""
