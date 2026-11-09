package org.graphiks.webgpu.toolkit.tck

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.graphiks.webgpu.toolkit.AcquiredFrame
import org.graphiks.webgpu.toolkit.DeviceLostException
import org.graphiks.webgpu.toolkit.Extent2D
import org.graphiks.webgpu.toolkit.SurfaceTarget
import org.graphiks.webgpu.toolkit.TargetLease
import org.graphiks.webgpu.toolkit.TargetState
import org.graphiks.webgpu.toolkit.ToolkitSession
import org.graphiks.webgpu.toolkit.ToolkitSessionState
import org.graphiks.webgpu.toolkit.WebGpuToolkitExtension
import org.graphiks.webgpu.toolkit.surface.withSurface
import org.graphiks.webgpu.toolkit.withSession
import kotlin.time.Duration.Companion.seconds

/**
 * Report of one [ToolkitProtocolTck.runAll] run: [failures] carries one line per violated
 * case, each prefixed with the case name; an empty list means every case passed.
 */
data class TckReport(
    /** One line per violated protocol case, prefixed with the case name; empty when all passed. */
    val failures: List<String>,
)

/**
 * Protocol contract kit of the WebGPU toolkit: runs the strict session and surface protocol
 * cases against ANY [WebGpuToolkitExtension] and reports every violation instead of throwing,
 * so one broken case cannot mask the others. Binding repositories validate their adapters with
 * the same checks the reference implementation passes.
 *
 * The cases, each named in [TckReport.failures] when violated:
 *  - `lifecycle walk`: a session walks `Ready`, `ShuttingDown`, `Closed` in order, exposes the
 *    extension's device and queue while `Ready`, pumps events when progression is `Pumped`, and
 *    returns the block result.
 *  - `idempotent shutdown`: a second [ToolkitSession.shutdown] call is a no-op — the state
 *    stays `Closed` and the instance is closed exactly once.
 *  - `present-once`: presenting a frame twice fails with an `IllegalStateException`, and the
 *    frame is presented and released exactly once.
 *  - `zero-extent suspension`: frame acquisition suspends while the target extent is zero and
 *    completes once the target is drawable again.
 *  - `stale-lease rollback`: a lease of a stale generation is retired and the protocol retried
 *    until it converges on a fresh lease.
 *  - `loss to Lost`: a device loss publishes `Lost`, cancels the admitted block with
 *    [DeviceLostException], and the walk still ends `Closed`.
 *
 * Cases run sequentially against the shared [extension] and [target]; run `runAll` from a test
 * harness with virtual time (for example `kotlinx.coroutines.test.runTest`), where the
 * per-case timeout costs nothing. The scripted cases — `zero-extent suspension`,
 * `stale-lease rollback` and `loss to Lost` — need the kit's own scriptable fixtures to drive
 * the violated path: when the given target or extension is not a [SimulatedTarget] or
 * [SimulatedToolkitExtension], those cases verify what is observable and skip what cannot be
 * scripted; passing a foreign extension together with [SimulatedToolkitExtension.newTarget]
 * keeps every scripted target-side path active.
 *
 * GPU-real cases — presenting through a live graphics stack rather than the simulation — are
 * v2: in v1 the browser path exercises the real web extension through the toolkit demo.
 */
object ToolkitProtocolTck {
    /** Ceiling for one case; with virtual time this costs nothing, and it bounds hung real backends. */
    private val CASE_TIMEOUT = 30.seconds

    /** Retains a surface acquisition may burn before the case reports non-convergence. */
    private const val RETAIN_BUDGET = 8

    /** Extent the drawable simulation publishes. */
    private val DRAWABLE_EXTENT = Extent2D(width = 640u, height = 480u)

    /**
     * Runs every protocol case against [extension] presenting to [target] and returns the
     * report; violations are collected, never thrown.
     */
    suspend fun runAll(extension: WebGpuToolkitExtension, target: SurfaceTarget): TckReport {
        val failures = mutableListOf<String>()
        failures += case("lifecycle walk") { lifecycleWalk(extension) }
        failures += case("idempotent shutdown") { idempotentShutdown(extension) }
        failures += case("present-once") { presentOnce(extension, target) }
        failures += case("zero-extent suspension") { zeroExtentSuspension(extension, target) }
        failures += case("stale-lease rollback") { staleLeaseRollback(extension, target) }
        failures += case("loss to Lost") { lossToLost(extension) }
        return TckReport(failures)
    }

    /**
     * Runs one case, prefixing its violations with [name]; a case that throws or times out
     * becomes a failure line naming the case, and cancellation of the caller still propagates.
     */
    private suspend fun case(name: String, body: suspend () -> List<String>): List<String> = try {
        body().map { violation -> "$name: $violation" }
    } catch (e: TimeoutCancellationException) {
        listOf("$name: case timed out after $CASE_TIMEOUT (${e.message})")
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        listOf("$name: case threw ${e::class.simpleName}: ${e.message}")
    }

    /**
     * `lifecycle walk`: the session walks Ready, ShuttingDown, Closed.
     */
    private suspend fun lifecycleWalk(extension: WebGpuToolkitExtension): List<String> = withTimeout(CASE_TIMEOUT) {
        coroutineScope {
            val sessionRef = CompletableDeferred<ToolkitSession>()
            val states = mutableListOf<ToolkitSessionState>()
            val collector = launch { sessionRef.await().state.collect { states += it } }
            val failures = mutableListOf<String>()
            val result = withSession(extension) { session ->
                sessionRef.complete(session)
                yield()
                if (session.state.value != ToolkitSessionState.Ready) {
                    failures += "session state is ${session.state.value} while the block runs, expected Ready"
                }
                val simulated = extension as? SimulatedToolkitExtension
                if (simulated != null) {
                    if (session.device !== simulated.device) {
                        failures += "session device is not the device of the extension"
                    }
                    if (session.queue !== simulated.device.simulatedQueue) {
                        failures += "session queue is not the queue of the device"
                    }
                }
                "block-result"
            }
            if (result != "block-result") {
                failures += "withSession returned \"$result\" instead of the block result"
            }
            repeat(2) { yield() }
            collector.cancelAndJoin()
            val expectedWalk = listOf(
                ToolkitSessionState.Ready,
                ToolkitSessionState.ShuttingDown,
                ToolkitSessionState.Closed,
            )
            if (states != expectedWalk) {
                failures += "state walk was $states, expected $expectedWalk"
            }
            val simulated = extension as? SimulatedToolkitExtension
            if (simulated != null && simulated.records.pumps < 1) {
                failures += "the pump loop never pumped events while the session was live"
            }
            failures
        }
    }

    /**
     * `idempotent shutdown`: repeated shutdown calls close the instance exactly once.
     */
    private suspend fun idempotentShutdown(extension: WebGpuToolkitExtension): List<String> = withTimeout(CASE_TIMEOUT) {
        val failures = mutableListOf<String>()
        val closesBefore = (extension as? SimulatedToolkitExtension)?.records?.instanceCloses ?: 0
        withSession(extension) { session ->
            session.shutdown()
            session.shutdown()
            if (session.state.value != ToolkitSessionState.Closed) {
                failures += "session state is ${session.state.value} after two shutdown calls, expected Closed"
            }
        }
        val simulated = extension as? SimulatedToolkitExtension
        if (simulated != null && simulated.records.instanceCloses - closesBefore != 1) {
            failures += "the instance was closed ${simulated.records.instanceCloses - closesBefore} times " +
                "across repeated shutdowns, expected exactly 1"
        }
        failures
    }

    /**
     * `present-once`: a second present of the same frame fails.
     */
    private suspend fun presentOnce(extension: WebGpuToolkitExtension, target: SurfaceTarget): List<String> =
        withTimeout(CASE_TIMEOUT) {
            val failures = mutableListOf<String>()
            val simulated = extension as? SimulatedToolkitExtension
            val acquired: AcquiredFrame? = simulated?.let { extensionFrames ->
                val frame = simulatedFrame()
                extensionFrames.frames.send(frame)
                frame
            }
            withSurface(extension, bounded(target)) { _, surface ->
                surface.withFrame { frame ->
                    if (acquired != null && frame.view !== acquired.view) {
                        failures += "the acquired frame does not carry the scripted view"
                    }
                    frame.present()
                    try {
                        frame.present()
                        failures += "presenting the frame a second time did not fail"
                    } catch (e: IllegalStateException) {
                        if (e.message?.contains("already presented") != true) {
                            failures += "the second present failed with an unexpected message: ${e.message}"
                        }
                    }
                }
            }
            if (simulated != null && acquired != null) {
                if (simulated.records.presents.count { it === acquired } != 1) {
                    failures += "the frame was presented ${simulated.records.presents.count { it === acquired }} " +
                        "times, expected exactly 1"
                }
                if (simulated.records.releases.count { it === acquired } != 1) {
                    failures += "the frame was released ${simulated.records.releases.count { it === acquired }} " +
                        "times, expected exactly 1"
                }
            }
            failures
        }

    /**
     * `zero-extent suspension`: acquisition suspends on a zero extent and resumes when drawable.
     */
    private suspend fun zeroExtentSuspension(extension: WebGpuToolkitExtension, target: SurfaceTarget): List<String> =
        withTimeout(CASE_TIMEOUT) {
            val sim = target as? SimulatedTarget
                ?: return@withTimeout emptyList() // a foreign target cannot script a zero extent
            val failures = mutableListOf<String>()
            val generation = sim.mutableState.value.generation
            sim.mutableState.value = TargetState(generation = generation, extent = Extent2D(0u, 0u), available = true)
            val simulated = extension as? SimulatedToolkitExtension
            simulated?.frames?.send(simulatedFrame())
            withSurface(extension, bounded(sim)) { _, surface ->
                coroutineScope {
                    var completed = false
                    val job = launch {
                        surface.withFrame { }
                        completed = true
                    }
                    repeat(3) { yield() }
                    if (completed) {
                        failures += "withFrame completed while the target extent was zero; acquisition must suspend"
                    }
                    sim.mutableState.value = TargetState(
                        generation = generation,
                        extent = DRAWABLE_EXTENT,
                        available = true,
                    )
                    job.join()
                    if (!completed) {
                        failures += "withFrame did not complete after the target became drawable again"
                    }
                }
            }
            failures
        }

    /**
     * `stale-lease rollback`: a stale lease is retired and the protocol retried on a fresh one.
     */
    private suspend fun staleLeaseRollback(extension: WebGpuToolkitExtension, target: SurfaceTarget): List<String> =
        withTimeout(CASE_TIMEOUT) {
            val failures = mutableListOf<String>()
            val sim = target as? SimulatedTarget
            val retainsBefore = sim?.records?.retains?.size ?: 0
            val retiresBefore = sim?.records?.retires?.size ?: 0
            val stale: TargetLease? = sim?.let { scripted ->
                val lease = SimulatedLease(scripted.mutableState.value.generation - 1)
                scripted.retainScript.send(lease)
                lease
            }
            withSurface(extension, bounded(target)) { _, surface ->
                if (surface.configuration.value == null) {
                    failures += "the surface never negotiated a configuration"
                }
                if (sim != null && stale != null) {
                    val caseRetains = sim.records.retains.drop(retainsBefore)
                    val caseRetires = sim.records.retires.drop(retiresBefore)
                    if (caseRetains.firstOrNull() !== stale) {
                        failures += "the first retain did not hand out the scripted stale lease"
                    }
                    if (stale !in caseRetires) {
                        failures += "the stale lease was never retired"
                    }
                    if (caseRetains.size < 2) {
                        failures += "the rollback retried with only ${caseRetains.size} retain(s), expected a retry"
                    }
                }
            }
            failures
        }

    /**
     * `loss to Lost`: a device loss publishes Lost, cancels the block and still ends Closed.
     */
    private suspend fun lossToLost(extension: WebGpuToolkitExtension): List<String> = withTimeout(CASE_TIMEOUT) {
        val simulated = extension as? SimulatedToolkitExtension
            ?: return@withTimeout emptyList() // a foreign extension cannot script a device loss
        coroutineScope {
            val sessionRef = CompletableDeferred<ToolkitSession>()
            val states = mutableListOf<ToolkitSessionState>()
            val collector = launch { sessionRef.await().state.collect { states += it } }
            val failures = mutableListOf<String>()
            val lost: DeviceLostException? = try {
                withSession(extension) { session ->
                    sessionRef.complete(session)
                    yield()
                    simulated.loseDevice()
                    delay(10.seconds)
                }
                failures += "the device loss did not cancel the admitted block"
                null
            } catch (e: DeviceLostException) {
                e
            }
            if (lost?.info?.message != "simulated device lost") {
                failures += "the loss carried message '${lost?.info?.message}', expected 'simulated device lost'"
            }
            repeat(2) { yield() }
            collector.cancelAndJoin()
            val expectedWalk = listOf(
                ToolkitSessionState.Ready,
                ToolkitSessionState.Lost,
                ToolkitSessionState.ShuttingDown,
                ToolkitSessionState.Closed,
            )
            if (states != expectedWalk) {
                failures += "state walk after the loss was $states, expected $expectedWalk"
            }
            failures
        }
    }

    /**
     * A fresh frame carrying its own view, fed into the simulation's frame source.
     */
    private fun simulatedFrame(): AcquiredFrame = AcquiredFrame(view = SimulatedTextureView(), extent = DRAWABLE_EXTENT)

    /**
     * Guards a target against non-converging retain scripts: the [RETAIN_BUDGET]-th retain
     * fails instead of letting the surface protocol spin, so a sabotaged rollback becomes a
     * reported failure rather than a hang.
     */
    private fun bounded(target: SurfaceTarget): SurfaceTarget = object : SurfaceTarget by target {
        private var retains = 0

        override suspend fun retain(): TargetLease {
            check(++retains <= RETAIN_BUDGET) {
                "the surface target did not converge on a fresh lease within $RETAIN_BUDGET retains"
            }
            return target.retain()
        }
    }
}
