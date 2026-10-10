package org.graphiks.webgpu.suite.browser.demos

import kotlin.math.floor
import org.graphiks.webgpu.suite.demos.reactiondiffusion.ReactionBrush
import org.graphiks.webgpu.suite.demos.reactiondiffusion.ReactionDisplay
import org.graphiks.webgpu.suite.demos.reactiondiffusion.ReactionPalette
import org.graphiks.webgpu.suite.demos.reactiondiffusion.ReactionParameters
import org.graphiks.webgpu.suite.demos.reactiondiffusion.ReactionPreset
import org.graphiks.webgpu.suite.demos.reactiondiffusion.validateReactionParameters
import org.graphiks.webgpu.suite.demos.reactiondiffusion.validateReactionSteps

internal class ReactionControls {
    var parameters = ReactionPreset.Coral.parameters
        set(value) { validateReactionParameters(value); field = value }
    var palette = ReactionPalette.Ocean
    var display = ReactionDisplay.Color
    var speed = 8
        set(value) { validateReactionSteps(value); require(value > 0); field = value }
    var paused = false
        private set
    private var stepPending = false

    fun togglePause() { paused = !paused; clearPendingStep() }
    fun requestStep() { if (paused) stepPending = true }
    fun clearPendingStep() { stepPending = false }
    fun takeSteps(): Int {
        val result = if (!paused) speed else if (stepPending) 1 else 0
        stepPending = false
        return result
    }
    fun selectPreset(preset: ReactionPreset) { parameters = preset.parameters; clearPendingStep() }
}

/** CSS geometry, not drawing-buffer pixels: works at any device pixel ratio. */
internal fun reactionBrushAt(clientX: Double, clientY: Double, left: Double, top: Double,
                             width: Double, height: Double): ReactionBrush? {
    if (!listOf(clientX, clientY, left, top, width, height).all { it.isFinite() } || width <= 0 || height <= 0) return null
    fun cell(value: Double, origin: Double, extent: Double) =
        floor(((value - origin) / extent).coerceIn(0.0, 1.0) * 256).toInt().coerceIn(0, 255)
    return ReactionBrush(cell(clientX, left, width), cell(clientY, top, height))
}
