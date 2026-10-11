package org.graphiks.webgpu.suite.demos.reactiondiffusion

/** Fixed simulation resolution; the display can have a different size. */
const val ReactionGridSize = 256

/** Each texel contains [A, B, 0, 1] as four 32-bit floats. */
const val ReactionStateFloats = ReactionGridSize * ReactionGridSize * 4

data class ReactionParameters(val feed: Float, val kill: Float)

enum class ReactionPreset(val parameters: ReactionParameters) {
    Coral(ReactionParameters(0.0545f, 0.062f)),
    Labyrinth(ReactionParameters(0.029f, 0.057f)),
    Spots(ReactionParameters(0.0367f, 0.0649f)),
}

enum class ReactionPalette { Ocean, Ember, Grayscale }

enum class ReactionDisplay { Color, A, B }

/** Center of a six-cell-radius periodic brush, in simulation coordinates. */
data class ReactionBrush(val x: Int, val y: Int)

/** Reproducible nine seeds, freshly allocated on every call. */
fun initialReactionState(): FloatArray {
    val state = FloatArray(ReactionStateFloats)
    for (i in state.indices step 4) {
        state[i] = 1f
        state[i + 3] = 1f
    }
    for (cy in listOf(64, 128, 192)) for (cx in listOf(64, 128, 192)) {
        for (y in cy - 4 until cy + 4) for (x in cx - 4 until cx + 4) {
            val i = (y * ReactionGridSize + x) * 4
            state[i] = 0.5f
            state[i + 1] = 0.25f
        }
    }
    return state
}

fun validateReactionParameters(parameters: ReactionParameters) {
    require(parameters.feed.isFinite() && parameters.feed in 0f..0.1f) { "Invalid feed: ${parameters.feed}" }
    require(parameters.kill.isFinite() && parameters.kill in 0f..0.1f) { "Invalid kill: ${parameters.kill}" }
}

fun validateReactionSteps(steps: Int) {
    require(steps in 0..16) { "Steps must be in 0..16, was $steps" }
}

fun validateReactionBrush(brush: ReactionBrush) {
    require(brush.x in 0 until ReactionGridSize && brush.y in 0 until ReactionGridSize) {
        "Brush must be within the simulation grid, was $brush"
    }
}

/** Validate the entire state before allocating or writing GPU resources. */
fun validateReactionState(state: FloatArray) {
    require(state.size == ReactionStateFloats) { "Expected $ReactionStateFloats floats, was ${state.size}" }
    for (i in state.indices step 4) {
        require(state[i].isFinite() && state[i] in 0f..1f) { "Invalid A at float $i: ${state[i]}" }
        require(state[i + 1].isFinite() && state[i + 1] in 0f..1f) { "Invalid B at float ${i + 1}: ${state[i + 1]}" }
        require(state[i + 2] == 0f && state[i + 3] == 1f) { "Expected padding [0, 1] at float ${i + 2}" }
    }
}
