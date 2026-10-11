package org.graphiks.webgpu.suite.acid.adapter

import org.graphiks.webgpu.GPUErrorFilter
import org.graphiks.webgpu.GPUFeatureLevel
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.GPUTextureUsage
import org.graphiks.webgpu.GPUTextureViewDimension
import org.graphiks.webgpu.GPUValidationError
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
import kotlin.test.assertIs

/** Compatibility disables implicit features and rejects cube-array views; a cube view stays valid. */
@AcidTest(
    id = AcidCaseId.AdapterCompatibilityFeatureLevel,
    family = AcidFamily.AdapterDeviceFeaturesLimits,
    input = AcidInput.Context,
    contract = [
        ApiSymbols.GPURequestAdapterOptions_featureLevel,
        ApiSymbols.GPUFeatureLevel_Compatibility,
        ApiSymbols.GPUAdapter_requestDevice,
        ApiSymbols.GPUDevice_features,
        ApiSymbols.GPUFeatureName_CoreFeaturesAndLimits,
        ApiSymbols.GPUTexture_createView,
        ApiSymbols.GPUTextureViewDimension_Cube,
        ApiSymbols.GPUTextureViewDimension_CubeArray,
        ApiSymbols.GPUDevice_pushErrorScope,
        ApiSymbols.GPUDevice_popErrorScope,
        ApiSymbols.GPUValidationError,
    ],
)
suspend fun compatibilityFeatureLevel(context: AcidContext) {
    context.requestAdapter(RequestAdapterOptions(featureLevel = GPUFeatureLevel.Compatibility))
        .getOrThrow().use { adapter ->
            withFeatureLevelDevice(adapter) { device ->
                // The spec permits falling back to Core when stricter validation is unavailable.
                // This is not evidence that the Compatibility restrictions have passed.
                requireCompatibilityFeatures(device.features)
                withValidationScope(device) {
                    device.createTexture(
                        TextureDescriptor(
                            size = Extent3D(1u, 1u, 6u),
                            format = GPUTextureFormat.RGBA8Unorm,
                            usage = GPUTextureUsage.TextureBinding,
                        ),
                    ).use { texture ->
                        // Positive control: a valid cube proves the texture itself is not invalid.
                        withValidationScope(device) {
                            texture.createView(TextureViewDescriptor(dimension = GPUTextureViewDimension.Cube)).use { }
                        }
                        device.pushErrorScope(GPUErrorFilter.Validation)
                        try {
                            texture.createView(TextureViewDescriptor(dimension = GPUTextureViewDimension.CubeArray)).use { }
                        } finally {
                            assertIs<GPUValidationError>(device.popErrorScope().getOrThrow(),
                                "CubeArray views require CoreFeaturesAndLimits")
                        }
                    }
                }
            }
        }
}
