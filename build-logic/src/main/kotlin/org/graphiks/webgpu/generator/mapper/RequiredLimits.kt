package org.graphiks.webgpu.generator.mapper

import org.graphiks.webgpu.generator.domain.DescriptorClass
import org.graphiks.webgpu.generator.domain.Interface
import org.graphiks.webgpu.generator.domain.MapperContext

/**
 * Builds the partial required-limits contract from the attributes of `GPUSupportedLimits`.
 *
 * Every supported limit gets a nullable counterpart in the `GPURequiredLimits` interface and in
 * the `RequiredLimits` descriptor, with the same underlying numeric type and a `null` default.
 * `GPUDeviceDescriptor.requiredLimits` then references `GPURequiredLimits?`, so an absent limit
 * and an explicitly requested zero stay distinct.
 */
internal fun MapperContext.adaptRequiredLimits() {
    val supportedLimits = interfaces.single { it.name == "GPUSupportedLimits" }

    interfaces += Interface("GPURequiredLimits").apply {
        attributes = supportedLimits.attributes.map { attribute ->
            Interface.Attribute(attribute.name, "${attribute.type}?", attribute.isConstant)
        }
    }

    descriptors += DescriptorClass(
        "GPURequiredLimits",
        supportedLimits.attributes.map { attribute ->
            DescriptorClass.Parameter(attribute.name, "${attribute.type}?", defaultValue = "null")
        },
    )

    descriptors.first { it.name == "GPUDeviceDescriptor" }
        .parameter.first { it.name == "requiredLimits" }
        .apply {
            type = "GPURequiredLimits?"
            defaultValue = "null"
        }

    interfaces.first { it.name == "GPUDeviceDescriptor" }
        .attributes.first { it.name == "requiredLimits" }
        .type = "GPURequiredLimits?"
}
