package org.graphiks.webgpu.suite.demos.filterlab

/** These exact strings are compiled and shown in the learning panel. */
val FilterLabComputeWgsl = """
    struct Params {
        size: vec2<u32>, mode: u32, radius: u32,
        intensity: f32, threshold: f32, padding: vec2<f32>,
    }
    @group(0) @binding(0) var source: texture_2d<f32>;
    @group(0) @binding(1) var result: texture_storage_2d<rgba8unorm, write>;
    @group(0) @binding(2) var<uniform> params: Params;

    // Repeat the nearest edge pixel; never read outside the source.
    fn sampleAt(p: vec2<i32>) -> vec3<f32> {
        let bounded = clamp(p, vec2<i32>(0), vec2<i32>(params.size) - vec2<i32>(1));
        return textureLoad(source, bounded, 0).rgb;
    }
    fn luminance(p: vec2<i32>) -> f32 {
        return dot(sampleAt(p), vec3<f32>(0.2126, 0.7152, 0.0722));
    }
    fn applyFilter(p: vec2<i32>, color: vec3<f32>) -> vec3<f32> {
        switch params.mode {
            case 0u: { return vec3<f32>(luminance(p)); }
            case 1u: {
                return clamp(vec3<f32>(
                    dot(color, vec3<f32>(0.393, 0.769, 0.189)),
                    dot(color, vec3<f32>(0.349, 0.686, 0.168)),
                    dot(color, vec3<f32>(0.272, 0.534, 0.131))), vec3<f32>(0), vec3<f32>(1));
            }
            case 2u: { return vec3<f32>(1) - color; }
            case 3u: {
                // Direct 2D Gaussian convolution, kept explicit for learning.
                let radius = i32(params.radius);
                let sigma = max(0.5, f32(radius) / 2.0);
                var sum = vec3<f32>(0);
                var total = 0.0;
                for (var y = -radius; y <= radius; y++) {
                    for (var x = -radius; x <= radius; x++) {
                        let weight = exp(-f32(x*x + y*y) / (2.0*sigma*sigma));
                        sum += sampleAt(p + vec2<i32>(x, y)) * weight;
                        total += weight;
                    }
                }
                return sum / total;
            }
            default: {
                // Sobel gradients in encoded RGB luminance, not linear-light color.
                let a = luminance(p + vec2<i32>(-1, -1));
                let b = luminance(p + vec2<i32>( 0, -1));
                let c = luminance(p + vec2<i32>( 1, -1));
                let d = luminance(p + vec2<i32>(-1,  0));
                let f = luminance(p + vec2<i32>( 1,  0));
                let g = luminance(p + vec2<i32>(-1,  1));
                let h = luminance(p + vec2<i32>( 0,  1));
                let i = luminance(p + vec2<i32>( 1,  1));
                let magnitude = clamp(length(vec2<f32>(-a+c-2.0*d+2.0*f-g+i,
                    -a-2.0*b-c+g+2.0*h+i)) / 4.0, 0.0, 1.0);
                return vec3<f32>(select(magnitude, 0.0, magnitude < params.threshold));
            }
        }
    }
    @compute @workgroup_size(8, 8)
    fn main(@builtin(global_invocation_id) id: vec3<u32>) {
        if (any(id.xy >= params.size)) { return; }
        let p = vec2<i32>(id.xy);
        let original = textureLoad(source, p, 0);
        let filtered = applyFilter(p, original.rgb);
        // Retain the central pixel's alpha, including for neighborhood filters.
        textureStore(result, p, vec4<f32>(mix(original.rgb, filtered, params.intensity), original.a));
    }
""".trimIndent()

val FilterLabRenderWgsl = """
    struct View { size: vec2<f32>, comparison: f32, padding: f32 }
    @group(0) @binding(0) var source: texture_2d<f32>;
    @group(0) @binding(1) var result: texture_2d<f32>;
    @group(0) @binding(2) var<uniform> view: View;

    @vertex fn vertexMain(@builtin(vertex_index) index: u32) -> @builtin(position) vec4<f32> {
        let positions = array<vec2<f32>, 3>(vec2<f32>(-1, -1), vec2<f32>(3, -1), vec2<f32>(-1, 3));
        return vec4<f32>(positions[index], 0, 1);
    }
    @fragment fn fragmentMain(@builtin(position) position: vec4<f32>) -> @location(0) vec4<f32> {
        let dimensions = vec2<f32>(textureDimensions(source));
        let scale = min(view.size.x / dimensions.x, view.size.y / dimensions.y);
        let drawn = dimensions * scale;
        let offset = (view.size - drawn) / 2.0;
        let uv = (position.xy - offset) / drawn;
        if (any(uv < vec2<f32>(0)) || any(uv >= vec2<f32>(1))) {
            return vec4<f32>(0.035, 0.045, 0.065, 1);
        }
        let p = min(vec2<i32>(uv * dimensions), vec2<i32>(dimensions) - vec2<i32>(1));
        var color = textureLoad(result, p, 0);
        if (uv.x < view.comparison) { color = textureLoad(source, p, 0); }
        // Straight-alpha composition onto a visible checkerboard.
        let cell = vec2<u32>(position.xy) / 12u;
        let gray = select(0.18, 0.32, (cell.x + cell.y) % 2u == 0u);
        return vec4<f32>(mix(vec3<f32>(gray), color.rgb, color.a), 1);
    }
""".trimIndent()
