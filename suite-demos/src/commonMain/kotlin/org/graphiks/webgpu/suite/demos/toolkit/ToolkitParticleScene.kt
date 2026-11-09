package org.graphiks.webgpu.suite.demos.toolkit

import org.graphiks.webgpu.GPUSupportedLimits
import org.graphiks.webgpu.suite.demos.particles.ParticleScene
import org.graphiks.webgpu.suite.demos.particles.initialParticles
import org.graphiks.webgpu.suite.demos.particles.maxParticleCount
import org.graphiks.webgpu.toolkit.SurfaceTarget
import org.graphiks.webgpu.toolkit.WebGpuToolkitExtension
import org.graphiks.webgpu.toolkit.surface.withSurface

/** Particle counts the demo proposes, mirroring the canvas particle page. */
private val ProposedCounts = listOf(256, 1024, 4096, 16384, 65536)

/** Particle count the toolkit demo starts with, mirroring the canvas particle page. */
private const val DefaultParticleCount = 4096

/** Simulation step applied to every frame after the first, matching a 60 Hz animation. */
private const val StepSeconds = 1f / 60f

/**
 * Renders the portable particle scene through the generic WebGPU toolkit: the living
 * integration example of [withSurface].
 *
 * The scene is built exactly as the canvas particle page builds it — one [ParticleScene] over
 * the session's device and the surface's negotiated format, sized by the device limits with the
 * same proposed counts and the same 4096 default — but every GPU interaction goes through the
 * strict toolkit protocol: [withSurface] opens the session and negotiates the canvas, and each
 * of the [frames] steps draws one frame through `ToolkitSurface.withFrame`, encoding the scene
 * into the frame's view at the frame's extent, submitting through the session's queue and
 * presenting once. The first frame renders the initial state with a zero step, like the page's
 * opening frame; later frames step the simulation by a fixed 1/60 s.
 *
 * The function is finite: it returns after presenting [frames] frames and releasing the scene,
 * the surface and the session on every exit path, so test harnesses and browsers can drive it
 * in bounded passes. It throws when any protocol or GPU step fails; it never swallows a
 * failure.
 */
suspend fun runToolkitParticles(
    extension: WebGpuToolkitExtension,
    target: SurfaceTarget,
    frames: Int,
) {
    require(frames > 0) { "Frame count must be positive, was $frames." }
    withSurface(extension, target) { session, surface ->
        val device = session.device
        val format = checkNotNull(surface.configuration.value?.format) {
            "The toolkit surface never negotiated a configuration."
        }
        val scene = ParticleScene.create(device, format, initialParticles(initialParticleCount(device.limits)))
        try {
            repeat(frames) { step ->
                surface.withFrame { frame ->
                    val encoder = device.createCommandEncoder()
                    try {
                        scene.encodeFrame(
                            encoder,
                            frame.view,
                            frame.extent.width.toInt(),
                            frame.extent.height.toInt(),
                            if (step == 0) 0f else StepSeconds,
                        )
                        val commandBuffer = encoder.finish()
                        try {
                            session.queue.submit(listOf(commandBuffer))
                        } finally {
                            commandBuffer.close()
                        }
                    } finally {
                        encoder.close()
                    }
                    frame.present()
                }
            }
        } finally {
            scene.close()
        }
    }
}

/**
 * Picks the starting particle count the canvas particle page picks: the largest proposed count
 * up to its default that the device limits allow, or the smallest proposed count when even the
 * default is out of reach.
 */
private fun initialParticleCount(limits: GPUSupportedLimits): Int {
    val maximum = maxParticleCount(limits)
    val available = ProposedCounts.filter { it <= maximum }
    check(available.isNotEmpty()) {
        "This device cannot render any of the proposed particle counts (maximum $maximum)."
    }
    return available.lastOrNull { it <= DefaultParticleCount } ?: available.first()
}
