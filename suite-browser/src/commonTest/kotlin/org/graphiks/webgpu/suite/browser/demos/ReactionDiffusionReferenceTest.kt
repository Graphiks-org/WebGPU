package org.graphiks.webgpu.suite.browser.demos

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.graphiks.webgpu.suite.demos.reactiondiffusion.ReactionBrush
import org.graphiks.webgpu.suite.demos.reactiondiffusion.ReactionPreset
import org.graphiks.webgpu.suite.demos.reactiondiffusion.initialReactionState

class ReactionDiffusionReferenceTest {
    @Test
    fun uniformSeedCenterMatchesHandCalculation() {
        val result = referenceReactionStep(initialReactionState(), ReactionPreset.Coral.parameters)
        val i = (64 * 256 + 64) * 4
        assertTrue(kotlin.math.abs(result[i] - 0.496f) < 0.00001f)
        assertTrue(kotlin.math.abs(result[i + 1] - 0.252125f) < 0.00001f)
    }

    @Test
    fun equilibriumIsPreservedAndDiffusionCrossesPeriodicEdge() {
        val state = FloatArray(256 * 256 * 4)
        for (i in state.indices step 4) { state[i] = 1f; state[i + 3] = 1f }
        assertContentEquals(state, referenceReactionStep(state, ReactionPreset.Coral.parameters))
        state[1] = 0.25f
        val result = referenceReactionStep(state, ReactionPreset.Coral.parameters)
        assertTrue(kotlin.math.abs(result[255 * 4 + 1] - 0.025f) < 0.00001f)
    }

    @Test
    fun periodicBrushReachesOppositeEdgeButNotDistantCells() {
        val initial = initialReactionState()
        val result = referenceReactionBrush(initial, ReactionBrush(0, 0))
        assertEquals(1f, result[(255 * 256 + 255) * 4 + 1])
        assertEquals(0f, result[(10 * 256 + 10) * 4 + 1])
        assertEquals(0f, initial[1])
    }
}
