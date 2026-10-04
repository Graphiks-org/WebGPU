package org.graphiks.webgpu.suite.acid.textures

import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUErrorFilter
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.GPUTextureUsage
import org.graphiks.webgpu.GPUTextureViewDimension
import org.graphiks.webgpu.GPUValidationError
import org.graphiks.webgpu.descriptors.Extent3D
import org.graphiks.webgpu.descriptors.TextureDescriptor
import org.graphiks.webgpu.descriptors.TextureViewDescriptor
import org.graphiks.webgpu.suite.AcidCaseId
import org.graphiks.webgpu.suite.AcidFamily
import org.graphiks.webgpu.suite.AcidTest
import org.graphiks.webgpu.suite.acid.ApiSymbols
import org.graphiks.webgpu.suite.acid.withValidationScope
import kotlin.test.assertIs

/**
 * The view dimension must fit the texture it views. A 2D array of four layers accepts a
 * `TwoDArray` view, a single-layer 2D texture accepts its default view, and a six-layer array
 * accepts a `Cube` view — but a `Cube` view of the four-layer array fails validation because a
 * cube needs six layers, and a `ThreeD` view of the 2D texture fails because it is not a 3D
 * texture.
 */
@AcidTest(
    id = AcidCaseId.TexturesCreationConstraints,
    family = AcidFamily.TexturesViewsSamplers,
    contract = [
        ApiSymbols.GPUDevice_createTexture,
        ApiSymbols.GPUTextureDescriptor,
        ApiSymbols.GPUTexture_createView,
        ApiSymbols.GPUTextureViewDescriptor_dimension,
        ApiSymbols.GPUTextureViewDimension_TwoD,
        ApiSymbols.GPUTextureViewDimension_TwoDArray,
        ApiSymbols.GPUTextureViewDimension_Cube,
        ApiSymbols.GPUTextureViewDimension_ThreeD,
        ApiSymbols.GPUDevice_pushErrorScope,
        ApiSymbols.GPUDevice_popErrorScope,
    ],
)
suspend fun creationConstraints(device: GPUDevice) = withValidationScope(device) {
    device.createTexture(
        TextureDescriptor(
            size = Extent3D(2u, 2u, 4u),
            format = GPUTextureFormat.RGBA8Unorm,
            usage = GPUTextureUsage.TextureBinding,
        ),
    ).use { array ->
        array.createView(TextureViewDescriptor(dimension = GPUTextureViewDimension.TwoDArray)).close()
        array.createView(TextureViewDescriptor(dimension = GPUTextureViewDimension.TwoD)).close()

        device.pushErrorScope(GPUErrorFilter.Validation)
        try {
            array.createView(TextureViewDescriptor(dimension = GPUTextureViewDimension.Cube)).close()
        } finally {
            val error = device.popErrorScope().getOrThrow()
            assertIs<GPUValidationError>(error)
        }
    }

    device.createTexture(
        TextureDescriptor(
            size = Extent3D(2u, 2u, 1u),
            format = GPUTextureFormat.RGBA8Unorm,
            usage = GPUTextureUsage.TextureBinding,
        ),
    ).use { plain ->
        plain.createView().close()

        device.pushErrorScope(GPUErrorFilter.Validation)
        try {
            // A 3D view needs a 3D texture; this one is 2D.
            plain.createView(TextureViewDescriptor(dimension = GPUTextureViewDimension.ThreeD)).close()
        } finally {
            val error = device.popErrorScope().getOrThrow()
            assertIs<GPUValidationError>(error)
        }
    }

    device.createTexture(
        TextureDescriptor(
            size = Extent3D(2u, 2u, 6u),
            format = GPUTextureFormat.RGBA8Unorm,
            usage = GPUTextureUsage.TextureBinding,
        ),
    ).use { cubeSource ->
        cubeSource.createView(TextureViewDescriptor(dimension = GPUTextureViewDimension.Cube)).close()
    }
}
