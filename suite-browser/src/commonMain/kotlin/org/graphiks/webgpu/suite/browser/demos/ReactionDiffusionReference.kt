package org.graphiks.webgpu.suite.browser.demos

import kotlin.math.abs
import kotlin.math.min
import org.graphiks.webgpu.suite.demos.reactiondiffusion.ReactionBrush
import org.graphiks.webgpu.suite.demos.reactiondiffusion.ReactionParameters
import org.graphiks.webgpu.suite.demos.reactiondiffusion.validateReactionBrush
import org.graphiks.webgpu.suite.demos.reactiondiffusion.validateReactionParameters
import org.graphiks.webgpu.suite.demos.reactiondiffusion.validateReactionState

/** Independent CPU oracle for GPU checks only. Never used to animate the demo. */
internal fun referenceReactionStep(state: FloatArray, parameters: ReactionParameters): FloatArray {
    validateReactionState(state)
    validateReactionParameters(parameters)
    fun sample(x: Int, y: Int, c: Int): Float = state[(((y + 256) % 256) * 256 + (x + 256) % 256) * 4 + c]
    fun lap(x: Int, y: Int, c: Int): Float = -sample(x, y, c) +
        0.2f * (sample(x - 1, y, c) + sample(x + 1, y, c) + sample(x, y - 1, c) + sample(x, y + 1, c)) +
        0.05f * (sample(x - 1, y - 1, c) + sample(x + 1, y - 1, c) + sample(x - 1, y + 1, c) + sample(x + 1, y + 1, c))
    val result = state.copyOf()
    for (y in 0 until 256) for (x in 0 until 256) {
        val i = (y * 256 + x) * 4
        val a = state[i]
        val b = state[i + 1]
        val reaction = a * b * b
        result[i] = (a + lap(x, y, 0) - reaction + parameters.feed * (1f - a)).coerceIn(0f, 1f)
        result[i + 1] = (b + 0.5f * lap(x, y, 1) + reaction - (parameters.kill + parameters.feed) * b).coerceIn(0f, 1f)
    }
    return result
}

internal fun referenceReactionBrush(state: FloatArray, brush: ReactionBrush): FloatArray {
    validateReactionState(state)
    validateReactionBrush(brush)
    val result = state.copyOf()
    for (y in 0 until 256) for (x in 0 until 256) {
        val dx = min(abs(x - brush.x), 256 - abs(x - brush.x))
        val dy = min(abs(y - brush.y), 256 - abs(y - brush.y))
        if (dx * dx + dy * dy <= 36) {
            val i = (y * 256 + x) * 4
            result[i] = 0.5f
            result[i + 1] = 1f
        }
    }
    return result
}
