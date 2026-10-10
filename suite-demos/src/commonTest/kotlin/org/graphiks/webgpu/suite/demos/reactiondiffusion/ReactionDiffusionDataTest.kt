package org.graphiks.webgpu.suite.demos.reactiondiffusion

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ReactionDiffusionDataTest {
    @Test
    fun initialStateIsReproducibleAndIndependentlyOwned() {
        val first = initialReactionState()
        assertEquals(256 * 256 * 4, first.size)
        assertContentEquals(first, initialReactionState())
        assertEquals(1f, first[0])
        assertEquals(0f, first[1])
        for (cy in listOf(64, 128, 192)) for (cx in listOf(64, 128, 192)) {
            val center = (cy * 256 + cx) * 4
            assertEquals(0.5f, first[center])
            assertEquals(0.25f, first[center + 1])
        }
        assertEquals(576, first.indices.count { it % 4 == 1 && first[it] == 0.25f })
        validateReactionState(first)
        first[0] = 0f
        assertEquals(1f, initialReactionState()[0])
    }

    @Test
    fun rejectsInvalidParametersAndStepsBeforeGpuUpload() {
        for (bad in listOf(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, -0.01f, 0.101f)) {
            assertFailsWith<IllegalArgumentException> { validateReactionParameters(ReactionParameters(bad, 0.06f)) }
            assertFailsWith<IllegalArgumentException> { validateReactionParameters(ReactionParameters(0.03f, bad)) }
        }
        for (n in listOf(-1, 17, Int.MAX_VALUE)) {
            assertFailsWith<IllegalArgumentException> { validateReactionSteps(n) }
        }
        for (n in listOf(0, 1, 8, 16)) validateReactionSteps(n)
        validateReactionParameters(ReactionParameters(0f, 0.1f))
        validateReactionParameters(ReactionParameters(0.1f, 0f))
        for (preset in ReactionPreset.entries) validateReactionParameters(preset.parameters)
    }

    @Test
    fun rejectsInvalidStateAndBrushCoordinates() {
        assertFailsWith<IllegalArgumentException> { validateReactionState(FloatArray(4)) }
        val state = initialReactionState()
        for ((offset, value) in listOf(0 to Float.NaN, 0 to 1.1f, 1 to -1f, 2 to 1f, 3 to 0f)) {
            val copy = state.copyOf()
            copy[offset] = value
            assertFailsWith<IllegalArgumentException> { validateReactionState(copy) }
        }
        for (brush in listOf(ReactionBrush(-1, 0), ReactionBrush(0, 256))) {
            assertFailsWith<IllegalArgumentException> { validateReactionBrush(brush) }
        }
        validateReactionBrush(ReactionBrush(0, 0))
        validateReactionBrush(ReactionBrush(255, 255))
    }
}
