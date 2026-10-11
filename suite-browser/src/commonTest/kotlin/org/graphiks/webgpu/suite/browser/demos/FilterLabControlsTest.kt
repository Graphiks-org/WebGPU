package org.graphiks.webgpu.suite.browser.demos

import kotlin.test.*
import org.graphiks.webgpu.suite.demos.filterlab.*

class FilterLabControlsTest {
    @Test fun latestImportWinsAndCloseInvalidatesIt() {
        val controls = FilterLabControls()
        val first = controls.beginImport(); val second = controls.beginImport()
        assertFalse(controls.acceptsImport(first)); assertTrue(controls.acceptsImport(second))
        controls.close(); assertFalse(controls.acceptsImport(second))
    }
    @Test fun invalidUpdatePreservesState() {
        val controls = FilterLabControls()
        controls.update(FilterSettings(ImageFilter.Invert), 0.7f)
        assertFailsWith<IllegalArgumentException> { controls.update(FilterSettings(intensity=Float.NaN), 0f) }
        assertFailsWith<IllegalArgumentException> { controls.update(FilterSettings(), Float.NaN) }
        assertEquals(ImageFilter.Invert, controls.settings.filter); assertEquals(0.7f, controls.comparison)
    }
    @Test fun resetPreservesImportGeneration() {
        val controls = FilterLabControls(); val generation = controls.beginImport()
        controls.update(FilterSettings(ImageFilter.Sobel,0.2f,5,0.7f),0.1f)
        controls.reset()
        assertEquals(FilterSettings(),controls.settings); assertEquals(0.5f,controls.comparison)
        assertTrue(controls.acceptsImport(generation))
    }
}
