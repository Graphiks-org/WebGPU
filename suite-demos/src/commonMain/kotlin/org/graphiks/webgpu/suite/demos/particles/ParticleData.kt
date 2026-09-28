package org.graphiks.webgpu.suite.demos.particles

import org.graphiks.webgpu.GPUSupportedLimits

/** Hard ceiling for the demo: enough particles to fill a window, small enough for any real device. */
internal const val MaxParticleCount = 65536

/** Floats stored per particle: position x, position y, velocity x, velocity y. */
internal const val FloatsPerParticle = 4

/** Bytes per particle; the compute storage element and the instanced vertex stride share it. */
internal const val BytesPerParticle = FloatsPerParticle * 4

/** Invocations per compute workgroup. The scene dispatch must not exceed it. */
internal const val ParticleWorkgroupSize = 64

/**
 * Builds the initial particle state used by [ParticleScene].
 *
 * Each particle is four floats `[x, y, vx, vy]`. Positions start in `[-0.85, 0.85]` and velocities
 * in `[-0.3, 0.3]`, drawn from a small reproducible LCG so the same [seed] and [count] always
 * produce the same array. The CPU only runs this once per scene: afterwards the compute pass owns
 * the particle data.
 */
fun initialParticles(count: Int, seed: UInt = 1u): FloatArray {
    require(count in 1..MaxParticleCount) {
        "Particle count must be in 1..$MaxParticleCount, was $count"
    }
    var state = seed
    fun nextUnit(): Float {
        state = state * 1664525u + 1013904223u
        return (state shr 8).toFloat() / 16777216f
    }
    val particles = FloatArray(count * FloatsPerParticle)
    for (index in 0 until count) {
        val offset = index * FloatsPerParticle
        particles[offset] = nextUnit() * 1.7f - 0.85f
        particles[offset + 1] = nextUnit() * 1.7f - 0.85f
        particles[offset + 2] = nextUnit() * 0.6f - 0.3f
        particles[offset + 3] = nextUnit() * 0.6f - 0.3f
    }
    return particles
}

/**
 * Largest particle count the device limits allow for the compute and render passes.
 *
 * The usable count is the smallest of the demo ceiling, the storage buffer size, the storage
 * binding size and what a single dispatch may address. All comparisons are done in [ULong] so the
 * 64-bit limits are not truncated. A device that cannot run a [ParticleWorkgroupSize]-invocation
 * workgroup on X cannot run this demo at all, so the incompatibility is reported instead of a
 * silently reduced count.
 */
fun maxParticleCount(limits: GPUSupportedLimits): Int {
    require(
        limits.maxComputeWorkgroupSizeX >= ParticleWorkgroupSize.toUInt() &&
            limits.maxComputeInvocationsPerWorkgroup >= ParticleWorkgroupSize.toUInt(),
    ) {
        "This device cannot run a $ParticleWorkgroupSize-invocation compute workgroup " +
            "(sizeX=${limits.maxComputeWorkgroupSizeX}, invocations=${limits.maxComputeInvocationsPerWorkgroup})."
    }
    val byCeiling = MaxParticleCount.toULong()
    val byBuffer = limits.maxBufferSize / BytesPerParticle.toULong()
    val byBinding = limits.maxStorageBufferBindingSize / BytesPerParticle.toULong()
    val byDispatch = limits.maxComputeWorkgroupsPerDimension.toULong() * ParticleWorkgroupSize.toULong()
    return minOf(byCeiling, byBuffer, byBinding, byDispatch).toInt()
}
