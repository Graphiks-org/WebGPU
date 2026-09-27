package org.graphiks.webgpu.generator.mapper

import org.graphiks.webgpu.generator.domain.KDoc
import org.graphiks.webgpu.generator.domain.MapperContext

internal fun MapperContext.injectDocumentation(documentation: Map<String, String>) {
    interfaces.forEach { kInterface ->
        documentation.get(kInterface.name)?.let {
            kInterface.kDoc = KDoc(it)
        }

        kInterface.attributes.forEach { attribute ->
            documentation.get("${kInterface.name}#${attribute.name}")?.let {
                attribute.kDoc = KDoc(it, 1)
            }
        }

        kInterface.methods.forEach { attribute ->
            documentation.get("${kInterface.name}#${attribute.name}(${attribute.parameters.joinToString(", ") { it.name }})")
                ?.let {
                    attribute.kDoc = KDoc(it, 1)
                }
        }
    }

    descriptors.forEach { descriptor ->
        documentation.get(descriptor.name)?.let {
            descriptor.kDoc = KDoc(it)
        }

        descriptor.parameter.forEach { attribute ->
            documentation.get("${descriptor.name}#${attribute.name}")?.let {
                attribute.kDoc = KDoc(it, 1)
            }
        }

    }

    (commonEnumerations + commonWebEnumerations + commonNativeEnumerations)
        .forEach { enumeration ->
            documentation.get(enumeration.name)?.let {
                enumeration.kDoc = KDoc(it)
            }

            enumeration.values.forEach { value ->
                // Remove parameters from the documentation reference
                val name = value.name.substringBefore("(")
                documentation.get("${enumeration.name}#$name")?.let {
                    value.kDoc = KDoc(it, 1)
                }
            }
        }

}