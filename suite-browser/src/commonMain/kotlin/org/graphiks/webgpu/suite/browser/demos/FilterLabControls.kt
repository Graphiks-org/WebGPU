package org.graphiks.webgpu.suite.browser.demos

import org.graphiks.webgpu.suite.demos.filterlab.*

internal class FilterLabControls {
    var settings = FilterSettings()
        private set
    var comparison = 0.5f
        private set
    private var generation = 0
    private var closed = false
    fun update(settings: FilterSettings, comparison: Float) {
        check(!closed)
        validateFilterSettings(settings)
        require(comparison.isFinite() && comparison in 0f..1f)
        this.settings = settings; this.comparison = comparison
    }
    fun reset() { update(FilterSettings(),0.5f) }
    fun beginImport(): Int { check(!closed); return ++generation }
    fun acceptsImport(generation: Int): Boolean = !closed && this.generation == generation
    fun close() { closed = true }
}
