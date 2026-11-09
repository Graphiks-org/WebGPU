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
import org.graphiks.webgpu.toolkit.Session
import org.graphiks.webgpu.toolkit.SessionState
import org.graphiks.webgpu.toolkit.WebGpuToolkitExtension
import org.graphiks.webgpu.toolkit.surface.withSurface
import org.graphiks.webgpu.toolkit.withSession
import kotlin.time.Duration.Companion.seconds

/**
 * Report of one [ProtocolTck.runAll] run: [failures] carries one line per violated
 * case, each prefixed with the case name, and [skipped] names every case that could not run
 * against the given fixtures. Empty [failures] with empty [skipped] means every case ran and
 * passed; an empty [failures] alone certifies only the cases that ran — see [ProtocolTck]
 * for the skip rule.
 */
data class TckReport(
    /** One line per violated protocol case, prefixed with the case name; empty when every case that ran passed. */
    val failures: List<String>,
    /** Names of the cases that could not run against the given fixtures; empty when every case ran. */
    val skipped: List<String> = emptyList(),
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
 *  - `idempotent shutdown`: a second [Session.shutdown] call is a no-op — the state
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
 * [SimulatedToolkitExtension], those cases cannot script anything and are recorded by name in
 * [TckReport.skipped] — a skipped case never vanishes silently. What a foreign run actually
 * covers: with a foreign extension and target, `lifecycle walk`, `idempotent shutdown` and
 * `present-once` run fully as long as the extension's own surface hands out frames; a foreign
 * extension without its own frame source times out in `present-once` and reports it as a case
 * failure.
 *
 * GPU-real cases — presenting through a live graphics stack rather than the simulation — are
 * v2: browser-side validation of the real web extension is future work, arriving with the
 * toolkit demo.
 */
object ProtocolTck {
    /** Ceiling for one case; with virtual time this costs nothing, and it bounds hung real backends. */
    private val CASE_TIMEOUT = 30.seconds

    /** Retains a surface acquisition may burn before the case reports non-convergence. */
    private const val RETAIN_BUDGET = 8

    /** Extent the drawable simulation publishes. */
    private val DRAWABLE_EXTENT = Extent2D(width = 640u, height = 480u)

    /**
     * Runs every protocol case against [extension] presenting to [target] and returns the
     * report; violations are collected, never thrown, and cases that cannot run against the
     * given fixtures are named in [TckReport.skipped].
     */
    suspend fun runAll(extension: WebGpuToolkitExtension, target: SurfaceTarget): TckReport {
        val failures = mutableListOf<String>()
        val skipped = mutableListOf<String>()
        failures += case("lifecycle walk", skipped) { lifecycleWalk(extension) }
        failures += case("idempotent shutdown", skipped) { idempotentShutdown(extension) }
        failures += case("present-once", skipped) { presentOnce(extension, target) }
        failures += case("zero-extent suspension", skipped) { zeroExtentSuspension(extension, target) }
        failures += case("stale-lease rollback", skipped) { staleLeaseRollback(extension, target) }
        failures += case("loss to Lost", skipped) { lossToLost(extension) }
        return TckReport(failures, skipped)
    }

    /**
     * Runs one case, prefixing its violations with [name]; a case that throws or times out
     * becomes a failure line naming the case, a case that returns `null` because its fixtures
     * cannot script it is recorded by name in [skipped], and cancellation of the caller still
     * propagates.
     */
    private suspend fun case(
        name: String,
        skipped: MutableList<String>,
        body: suspend () -> List<String>?,
    ): List<String> = try {
        val caseFailures = body()
        if (caseFailures == null) {
            skipped += name
            emptyList()
        } else {
            caseFailures.map { violation -> "$name: $violation" }
        }
    } catch (e: TimeoutCancellationException) {
        listOf("$name: case timed out after $CASE_TIMEOUT (${e.message})")
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        val cause = e.cause?.let { " — caused by ${it::class.simpleName}: ${it.message}" } ?: ""
        listOf("$name: case threw ${e::class.simpleName}: ${e.message}$cause")
    }

    /**
     * `lifecycle walk`: the session walks Ready, ShuttingDown, Closed.
     */
    private suspend fun lifecycleWalk(extension: WebGpuToolkitExtension): List<String> = withTimeout(CASE_TIMEOUT) {
        coroutineScope {
            val sessionRef = CompletableDeferred<Session>()
            val states = mutableListOf<SessionState>()
            val collector = launch { sessionRef.await().state.collect { states += it } }
            val failures = mutableListOf<String>()
            val result = withSession(extension) { session ->
                sessionRef.complete(session)
                yield()
                if (session.state.value != SessionState.Ready) {
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
                SessionState.Ready,
                SessionState.ShuttingDown,
                SessionState.Closed,
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
            if (session.state.value != SessionState.Closed) {
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
     * `zero-extent suspension`: acquisition suspends on a zero extent and resumes when the
     * target is drawable again. Recorded in [TckReport.skipped] when [target] is not a
     * [SimulatedTarget] — publishing a zero extent and restoring it is target-side scripting,
     * and v1 detects scriptability by type, so only the kit's [SimulatedTarget] (from
     * [SimulatedToolkitExtension.newTarget] or constructed directly) activates this case. To
     * run it, a binding repository passes that target together with an extension whose surface
     * produces frames: the kit feeds frames only to a [SimulatedToolkitExtension], so a
     * foreign extension must supply its own frame source or the case times out as a failure.
     */
    private suspend fun zeroExtentSuspension(extension: WebGpuToolkitExtension, target: SurfaceTarget): List<String>? =
        withTimeout(CASE_TIMEOUT) {
            val sim = target as? SimulatedTarget
                ?: return@withTimeout null // a foreign target cannot script a zero extent
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
    private suspend fun staleLeaseRollback(extension: WebGpuToolkitExtension, target: SurfaceTarget): List<String>? =
        withTimeout(CASE_TIMEOUT) {
            // Without a scriptable target there is no way to hand out a stale lease, so there
            // is nothing to verify: skip by name instead of running a partial, meaningless pass
            // (a foreign extension may also refuse further adapter requests mid-campaign).
            val sim = target as? SimulatedTarget ?: return@withTimeout null
            val failures = mutableListOf<String>()
            val retainsBefore = sim.records.retains.size
            val retiresBefore = sim.records.retires.size
            val stale: TargetLease = run {
                val lease = SimulatedLease(sim.mutableState.value.generation - 1)
                sim.retainScript.send(lease)
                lease
            }
            withSurface(extension, bounded(target)) { _, surface ->
                if (surface.configuration.value == null) {
                    failures += "the surface never negotiated a configuration"
                }
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
            failures
        }

    /**
     * `loss to Lost`: a device loss publishes Lost, cancels the admitted block and still ends
     * Closed. Recorded in [TckReport.skipped] when [extension] is not a
     * [SimulatedToolkitExtension] — triggering a device loss is extension-side scripting, and
     * v1 detects scriptability by type: only the kit's [SimulatedToolkitExtension], lost
     * through [SimulatedToolkitExtension.loseDevice], activates this case; a foreign fake with
     * its own loss trigger is not recognized in v1.
     */
    private suspend fun lossToLost(extension: WebGpuToolkitExtension): List<String>? = withTimeout(CASE_TIMEOUT) {
        val simulated = extension as? SimulatedToolkitExtension
            ?: return@withTimeout null // a foreign extension cannot script a device loss
        coroutineScope {
            val sessionRef = CompletableDeferred<Session>()
            val states = mutableListOf<SessionState>()
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
                SessionState.Ready,
                SessionState.Lost,
                SessionState.ShuttingDown,
                SessionState.Closed,
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
