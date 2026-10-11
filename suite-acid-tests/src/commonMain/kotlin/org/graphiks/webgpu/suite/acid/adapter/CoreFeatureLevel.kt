package org.graphiks.webgpu.suite.acid.adapter

import org.graphiks.webgpu.GPUFeatureLevel
import org.graphiks.webgpu.GPUFeatureName
import org.graphiks.webgpu.GPURequestAdapterOptions
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.GPUTextureUsage
import org.graphiks.webgpu.GPUTextureViewDimension
import org.graphiks.webgpu.descriptors.Extent3D
import org.graphiks.webgpu.descriptors.RequestAdapterOptions
import org.graphiks.webgpu.descriptors.TextureDescriptor
import org.graphiks.webgpu.descriptors.TextureViewDescriptor
import org.graphiks.webgpu.suite.AcidCaseId
import org.graphiks.webgpu.suite.AcidContext
import org.graphiks.webgpu.suite.AcidFamily
import org.graphiks.webgpu.suite.AcidInput
import org.graphiks.webgpu.suite.AcidTest
import org.graphiks.webgpu.suite.acid.ApiSymbols
import org.graphiks.webgpu.suite.acid.withValidationScope
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Omitted options, the descriptor default and explicit Core all enable core-only cube-array views. */
@AcidTest(
    id = AcidCaseId.AdapterCoreFeatureLevel,
    family = AcidFamily.AdapterDeviceFeaturesLimits,
    input = AcidInput.Context,
    contract = [
        ApiSymbols.GPURequestAdapterOptions_featureLevel,
        ApiSymbols.GPUFeatureLevel_Core,
        ApiSymbols.GPUAdapter_features,
        ApiSymbols.GPUAdapter_requestDevice,
        ApiSymbols.GPUDevice_features,
        ApiSymbols.GPUFeatureName_CoreFeaturesAndLimits,
        ApiSymbols.GPUTexture_createView,
        ApiSymbols.GPUTextureViewDimension_CubeArray,
    ],
)
suspend fun coreFeatureLevel(context: AcidContext) {
    assertEquals(GPUFeatureLevel.Core, RequestAdapterOptions().featureLevel)
    for (options in listOf(null, RequestAdapterOptions(), RequestAdapterOptions(featureLevel = GPUFeatureLevel.Core))) {
        verifyCoreFeatureLevel(context, options)
    }
}

// Keep the suspend loop separate from resource cleanup: Kotlin's coroutine liveness analysis
// otherwise repeatedly traverses the nested inlined use/try blocks on JS and Wasm.
private suspend fun verifyCoreFeatureLevel(context: AcidContext, options: GPURequestAdapterOptions?) {
    context.requestAdapter(options).getOrThrow().use { adapter ->
        assertTrue(GPUFeatureName.CoreFeaturesAndLimits in adapter.features, "Core must be available")
        withFeatureLevelDevice(adapter) { device ->
            assertEquals(setOf(GPUFeatureName.CoreFeaturesAndLimits), device.features,
                "Core is enabled by default, but optional features are not")
            withValidationScope(device) {
                device.createTexture(
                    TextureDescriptor(
                        size = Extent3D(1u, 1u, 6u),
                        format = GPUTextureFormat.RGBA8Unorm,
                        usage = GPUTextureUsage.TextureBinding,
                    ),
                ).use { texture ->
                    texture.createView(TextureViewDescriptor(dimension = GPUTextureViewDimension.CubeArray)).use { }
                }
            }
        }
    }
}
