package org.graphiks.webgpu.toolkit.web

import org.graphiks.webgpu.GPUFeatureLevel
import org.graphiks.webgpu.toolkit.AdapterHints
import org.graphiks.webgpu.toolkit.Progression
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WebExtensionTest {
    @Test
    fun extensionIdentityAndProgression() {
        val extension = webToolkit()

        assertEquals("web", extension.id)

        val instance = extension.open()
        try {
            assertEquals(Progression.EventLoop, instance.progression)
            instance.pumpEvents()
        } finally {
            instance.close()
        }
    }

    @Test
    fun adapterHintsMapToRequestOptions() {
        val options = toRequestAdapterOptions(AdapterHints(forceFallbackAdapter = true))

        assertTrue(options.forceFallbackAdapter)
        assertNull(options.powerPreference)
        assertEquals(GPUFeatureLevel.Core, options.featureLevel)
    }
}
