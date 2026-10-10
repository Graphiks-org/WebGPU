package org.graphiks.webgpu.suite.demos.instancedcity

private val CityFrameBinding = """
    struct Frame { viewProjection: mat4x4<f32>, options: vec4<f32> }
    @group(0) @binding(0) var<uniform> frame: Frame;
    // options: height multiplier, grid pitch, display mode, reserved.
""".trimIndent()

/** Exact source compiled for all buildings, also displayed by the teaching panel. */
val CityBuildingShader = CityFrameBinding + "\n" + """
    struct BuildingIn {
        @location(0) position: vec3<f32>, @location(1) normal: vec3<f32>,
        @location(2) uv: vec2<f32>, @location(3) footprint: vec4<f32>,
        @location(4) metadata: vec4<f32>,
    }
    struct BuildingOut {
        @builtin(position) position: vec4<f32>,
        @location(0) normal: vec3<f32>, @location(1) uv: vec2<f32>,
        @location(2) @interpolate(flat) dimensions: vec2<f32>,
        @location(3) @interpolate(flat) metadata: vec2<f32>,
    }
    @vertex fn buildingVertex(in: BuildingIn) -> BuildingOut {
        let size = vec3<f32>(in.footprint.z, in.metadata.x * frame.options.x, in.footprint.w);
        let world = in.position * size + vec3<f32>(in.footprint.x, 0.0, in.footprint.y);
        var out: BuildingOut;
        out.position = frame.viewProjection * vec4<f32>(world, 1.0);
        out.normal = in.normal; out.uv = in.uv;
        out.dimensions = vec2<f32>(select(size.x, size.z, abs(in.normal.x) > 0.5), size.y);
        out.metadata = in.metadata.yz;
        return out;
    }
    fn windowHash(cell: vec2<u32>, seed: u32) -> u32 {
        var n = cell.x * 1973u + cell.y * 9277u + seed * 26699u;
        n = (n ^ (n >> 13u)) * 1274126177u;
        return n ^ (n >> 16u);
    }
    @fragment fn buildingFragment(in: BuildingOut) -> @location(0) vec4<f32> {
        // Constant instance colors exercise the same geometry/pipeline in GPU checks.
        if (frame.options.z == 1.0) {
            return vec4<f32>(select(vec3<f32>(0,1,1), vec3<f32>(1,0,1), in.metadata.x == 1.0), 1);
        }
        let cells = in.uv * max(vec2<f32>(2.0, 3.0), in.dimensions * 2.0);
        let coord = fract(cells);
        let aa = max(fwidth(cells), vec2<f32>(0.01));
        let edge = smoothstep(vec2<f32>(0.12), vec2<f32>(0.12) + aa, coord)
            * (vec2<f32>(1.0) - smoothstep(vec2<f32>(0.78) - aa, vec2<f32>(0.78), coord));
        let lit = f32(windowHash(vec2<u32>(floor(cells)), u32(in.metadata.y)) % 10u < 6u);
        let windows = edge.x * edge.y * lit * f32(abs(in.normal.y) < 0.5);
        let diffuse = max(dot(in.normal, normalize(vec3<f32>(0.4, 1.0, 0.2))), 0.0);
        let dark = vec3<f32>(0.025, 0.018, 0.05) + diffuse * vec3<f32>(0.08, 0.06, 0.12);
        let neon = select(vec3<f32>(0.05,0.9,1.0), vec3<f32>(1.0,0.05,0.65), in.metadata.x == 1.0);
        return vec4<f32>(mix(dark, neon, windows), 1.0);
    }
""".trimIndent()

val CityGroundShader = CityFrameBinding + "\n" + """
    struct GroundOut { @builtin(position) position: vec4<f32>, @location(0) world: vec2<f32> }
    @vertex fn groundVertex(@builtin(vertex_index) index: u32) -> GroundOut {
        let points = array<vec2<f32>,6>(vec2<f32>(-80,-80), vec2<f32>(80,-80), vec2<f32>(80,80),
            vec2<f32>(-80,-80), vec2<f32>(80,80), vec2<f32>(-80,80));
        var out: GroundOut; out.world = points[index];
        out.position = frame.viewProjection * vec4<f32>(out.world.x,0,out.world.y,1);
        return out;
    }
    @fragment fn groundFragment(in: GroundOut) -> @location(0) vec4<f32> {
        let grid = (in.world + 64.0) / frame.options.y;
        let fraction = fract(grid);
        let distance = min(fraction, vec2<f32>(1.0) - fraction);
        let aa = max(fwidth(grid), vec2<f32>(0.0001));
        let lines = vec2<f32>(1.0) - smoothstep(vec2<f32>(0.006), vec2<f32>(0.006) + aa, distance);
        let sector = f32(all(abs(in.world) <= vec2<f32>(64.0)));
        let roads = f32(min(distance.x, distance.y) < 0.18) * sector;
        let color = vec3<f32>(0.012,0.012,0.028) + roads * vec3<f32>(0.025,0.025,0.045)
            + sector * (lines.x * vec3<f32>(0.0,0.32,0.42) + lines.y * vec3<f32>(0.4,0.0,0.26));
        return vec4<f32>(color,1.0);
    }
""".trimIndent()
