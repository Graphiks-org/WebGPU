package org.graphiks.webgpu.suite.browser.demos

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.graphiks.webgpu.suite.demos.reactiondiffusion.ReactionBrush
import org.graphiks.webgpu.suite.demos.reactiondiffusion.ReactionPalette
import org.graphiks.webgpu.suite.demos.reactiondiffusion.ReactionParameters
import org.graphiks.webgpu.suite.demos.reactiondiffusion.ReactionPreset

class ReactionDiffusionControlsTest {
    @Test
    fun singleStepOnlyAdvancesOnceWhilePaused() {
        val controls = ReactionControls()
        assertEquals(8, controls.takeSteps())
        controls.togglePause()
        assertEquals(0, controls.takeSteps())
        controls.requestStep()
        controls.requestStep()
        assertEquals(1, controls.takeSteps())
        assertEquals(0, controls.takeSteps())
        assertTrue(controls.paused)
        controls.togglePause()
        controls.requestStep()
        assertEquals(8, controls.takeSteps())
    }

    @Test
    fun validatesControlsAndPresetClearsPendingStepWithoutChangingPalette() {
        val controls = ReactionControls()
        for (n in listOf(0, 17)) assertFailsWith<IllegalArgumentException> { controls.speed = n }
        assertFailsWith<IllegalArgumentException> { controls.parameters = ReactionParameters(Float.NaN, 0.06f) }
        controls.speed = 16
        assertEquals(16, controls.takeSteps())
        controls.palette = ReactionPalette.Ember
        controls.togglePause()
        controls.requestStep()
        controls.selectPreset(ReactionPreset.Labyrinth)
        assertEquals(ReactionParameters(0.029f, 0.057f), controls.parameters)
        assertEquals(ReactionPalette.Ember, controls.palette)
        assertEquals(0, controls.takeSteps())
    }

    @Test
    fun pointerUsesCssCoordinatesClampsCapturedInputAndRejectsInvalidRectangles() {
        assertEquals(ReactionBrush(128, 128), reactionBrushAt(210.0, 220.0, 10.0, 20.0, 400.0, 400.0))
        assertEquals(ReactionBrush(255, 0), reactionBrushAt(999.0, -10.0, 10.0, 20.0, 400.0, 400.0))
        assertNull(reactionBrushAt(0.0, 0.0, 0.0, 0.0, 0.0, 10.0))
        assertNull(reactionBrushAt(Double.NaN, 0.0, 0.0, 0.0, 10.0, 10.0))
        assertNull(reactionBrushAt(0.0, 0.0, 0.0, 0.0, 10.0, -1.0))
        assertNull(reactionBrushAt(0.0, 0.0, Double.POSITIVE_INFINITY, 0.0, 10.0, 10.0))
    }
}
