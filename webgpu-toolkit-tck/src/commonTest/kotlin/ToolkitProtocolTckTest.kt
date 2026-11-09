package org.graphiks.webgpu.toolkit.tck

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest

class ToolkitProtocolTckTest : FreeSpec({
    "runAll passes against the healthy simulation" {
        runTest {
            val extension = SimulatedToolkitExtension()
            val target = extension.newTarget()
            val report = ToolkitProtocolTck.runAll(extension, target)
            report.failures.shouldBeEmpty()
        }
    }

    "runAll names the rollback case when every lease is stale" {
        runTest {
            val extension = SimulatedToolkitExtension()
            val target = extension.newTarget()
            target.alwaysStale = true
            val report = ToolkitProtocolTck.runAll(extension, target)
            report.failures.any { it.startsWith("stale-lease rollback") } shouldBe true
            report.failures.none { it.startsWith("lifecycle walk") } shouldBe true
            report.failures.none { it.startsWith("loss to Lost") } shouldBe true
        }
    }
})
