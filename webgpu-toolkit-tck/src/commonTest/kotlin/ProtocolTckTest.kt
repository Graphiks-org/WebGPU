package org.graphiks.webgpu.toolkit.tck

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.graphiks.webgpu.toolkit.AcquiredFrame
import org.graphiks.webgpu.toolkit.Extent2D
import org.graphiks.webgpu.toolkit.SurfaceTarget
import org.graphiks.webgpu.toolkit.TargetLease
import org.graphiks.webgpu.toolkit.TargetState
import org.graphiks.webgpu.toolkit.SessionConfig
import org.graphiks.webgpu.toolkit.ToolkitInstance
import org.graphiks.webgpu.toolkit.WebGpuToolkitExtension

/** A foreign binding extension: same behavior as the simulation, but a type the kit cannot script. */
private class ForeignExtension(
    private val delegate: SimulatedToolkitExtension,
) : WebGpuToolkitExtension {
    override val id: String = "foreign"

    override fun open(config: SessionConfig): ToolkitInstance = delegate.open(config)
}

/** A foreign surface target: delegates to the simulation, but a type the kit cannot script. */
private class ForeignTarget(
    private val delegate: SurfaceTarget,
) : SurfaceTarget {
    override val state: StateFlow<TargetState> get() = delegate.state

    override suspend fun retain(): TargetLease = delegate.retain()

    override suspend fun retire(lease: TargetLease) = delegate.retire(lease)
}

class ProtocolTckTest : FreeSpec({
    "runAll passes against the healthy simulation" {
        runTest {
            val extension = SimulatedToolkitExtension()
            val target = extension.newTarget()
            val report = ProtocolTck.runAll(extension, target)
            report.failures.shouldBeEmpty()
            report.skipped.shouldBeEmpty()
        }
    }

    "runAll names the rollback case when every lease is stale" {
        runTest {
            val extension = SimulatedToolkitExtension()
            val target = extension.newTarget()
            target.alwaysStale = true
            val report = ProtocolTck.runAll(extension, target)
            report.failures.any { it.startsWith("stale-lease rollback") } shouldBe true
            report.failures.none { it.startsWith("lifecycle walk") } shouldBe true
            report.failures.none { it.startsWith("loss to Lost") } shouldBe true
            report.skipped.shouldBeEmpty()
        }
    }

    "runAll names the cases a foreign fixture cannot script" {
        runTest {
            val simulated = SimulatedToolkitExtension()
            val extension = ForeignExtension(simulated)
            val target = ForeignTarget(simulated.newTarget())
            // The foreign extension gets no frames from the kit, so the test feeds the
            // delegated simulation directly: present-once then runs, and the report shows the
            // over-certification trap the skipped list closes — empty failures certify only
            // the four cases that ran, and the two script-only cases are named, not vanished.
            simulated.frames.send(
                AcquiredFrame(view = SimulatedTextureView(), extent = Extent2D(width = 640u, height = 480u)),
            )
            val report = ProtocolTck.runAll(extension, target)
            report.failures.shouldBeEmpty()
            report.skipped shouldContainExactly listOf("zero-extent suspension", "loss to Lost")
        }
    }
})
