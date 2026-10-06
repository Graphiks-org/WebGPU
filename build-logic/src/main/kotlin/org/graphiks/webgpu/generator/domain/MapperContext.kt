package org.graphiks.webgpu.generator.domain

import com.squareup.kotlinpoet.TypeSpec
import de.fabmax.webidl.model.IdlModel
import org.graphiks.webgpu.generator.mapper.adaptRequiredLimits

class MapperContext(
    val idlModel: IdlModel,
    val yamlModel: YamlModel
) {

    val interfaces = mutableListOf<Interface>()
    var typeAliases = emptyList<TypeAlias>()
    var commonEnumerations = emptyList<Enumeration>()
    var commonWebEnumerations = emptyList<Enumeration>()
    var commonNativeEnumerations = emptyList<Enumeration>()
    var bitflagEnumerations = emptyList<TypeSpec>()
    val descriptors = mutableListOf<DescriptorClass>()
    val webInterfaces = mutableListOf<Interface>()
    val webTypeAlias = mutableListOf<TypeAlias>()

    fun adaptToGuidelines() {

        generateUncapturedErrorCallback()
        addAwaitLost()
        changeGPUErrorAsSealed()

        // If interface contains destroy, we set it as AutoCloseable
        interfaces.forEach { kinterface ->
            if (kinterface.methods.any { it.name == "destroy" }) {
                kinterface.methods = kinterface.methods.filter { it.name != "destroy" }
                kinterface.extends += "AutoCloseable"
            }
        }

        // Add AutoCloseable trait to specific classes as they require to be release on native
        interfaces.filter { it.name in interfaceToAddAutocloseableTrait }
            .forEach { it.extends += "AutoCloseable" }

        // Model the requested limits independently from the supported ones.
        adaptRequiredLimits()

        // Convert setlike to typealias
        val setLikes = idlModel.interfaces
            .filter { it.setLike != null }
            .map { it.name }
        interfaces.toList()
            .forEach {
                if (it.name in setLikes) {
                    interfaces -= it
                }
            }

        typeAliases += TypeAlias("GPUSupportedFeatures", "Set<GPUFeatureName>")

        interfaces.first { it.name == "GPUBindingCommandsMixin" }.apply {
            methods.first { it.name == "setBindGroup" }.apply {
                // remove dynamicOffsetsStart and dynamicOffsetsLength
                parameters = parameters.filter { it.name in listOf("index", "bindGroup", "dynamicOffsetsData") }
                parameters.first { it.name == "dynamicOffsetsData" }.defaultValue = "emptyList()"
            }
        }

        replaceBitFlagsReferences()
    }

    private fun replaceBitFlagsReferences() {
        typeAliases = typeAliases.filter { !it.name.endsWith("Flags") }

        interfaces.first { it.name == "GPUBuffer" }.apply {
            attributes.find { it.name == "usage" }!!.apply {
                type = "Set<GPUBufferUsage>"
            }
        }
        interfaces.first { it.name == "GPUTexture" }.apply {
            attributes.find { it.name == "usage" }!!.apply {
                type = "Set<GPUTextureUsage>"
            }
        }

        interfaces.first { it.name == "GPUBuffer" }.apply {
            methods.first { it.name == "mapAsync" }.apply {
                parameters.first { it.name == "mode" }.apply {
                    type = "GPUMapMode"
                }
            }
        }

        replaceDescriptorFieldType("GPUBufferDescriptor", "usage", "GPUBufferUsage")
        replaceDescriptorFieldType("GPUBindGroupLayoutEntry", "visibility", "GPUShaderStage")
        replaceDescriptorFieldType("GPUTextureDescriptor", "usage", "GPUTextureUsage")
        replaceDescriptorFieldType("GPUColorTargetState", "writeMask", "GPUColorWrite", "GPUColorWrite.All")
        replaceDescriptorFieldType("GPUTextureViewDescriptor", "usage", "GPUTextureUsage", "GPUTextureUsage.None")
    }

    private fun replaceDescriptorFieldType(className: String, fieldName: String, newType: String, newDefaultValue: String? = null) {
        interfaces.first { it.name == className }.apply {
            attributes.first { it.name == fieldName }.apply {
                type = newType
            }
        }
        descriptors.first { it.name == className }.apply {
            parameter.first { it.name == fieldName }.apply {
                defaultValue = newDefaultValue
                type = newType
            }
        }
    }

    /**
     * Replaces the JavaScript `lost` promise with a portable, cancellable observation of the
     * device loss. The loss itself is a successful result carrying a reason and a message; only an
     * interop failure is a `Result` failure.
     */
    private fun addAwaitLost() {
        interfaces.find { it.name == "GPUDevice" }!!.apply {
            methods = methods + Interface.Method(
                name = "awaitLost",
                returnType = "Result<GPUDeviceLostInfo>",
                parameters = emptyList(),
                isSuspend = true,
            ).apply {
                kDoc = KDoc(
                    """
                    Waits until this device is lost and resolves with the loss information.

                    The loss is a successful result: it carries a [GPUDeviceLostReason] and an
                    implementation-provided message, it is not a failure of the returned [Result].
                    Several observers may wait at the same time and all of them observe the same
                    loss; an observer that starts waiting after the loss resolves immediately.
                    Cancelling one observer neither cancels the other observers nor destroys the
                    device. Closing the device explicitly notifies the loss with the destruction
                    reason when the backend provides one.
                    """.trimIndent(),
                )
            }
        }
    }

    private fun changeGPUErrorAsSealed() {
        interfaces.find { it.name == "GPUError" }!!.apply {
            sealed = true
        }
    }

    private fun generateUncapturedErrorCallback() {
        interfaces.find { it.name == "GPUDevice" }!!.apply {
            attributes = attributes.filter { it.name !in listOf("lost", "onuncapturederror") }
        }
        descriptors.find { it.name == "GPUDeviceDescriptor" }!!.apply {
            parameter += DescriptorClass.Parameter(
                "onUncapturedError",
                "GPUUncapturedErrorCallback?",
                defaultValue = "null"
            )
        }
        interfaces.find { it.name == "GPUDeviceDescriptor" }!!.apply {
            attributes += Interface.Attribute("onUncapturedError", "GPUUncapturedErrorCallback?", true)
        }
        interfaces += Interface("GPUUncapturedErrorCallback", functional = true).apply {
            methods += Interface.Method(
                "onUncapturedError", "Unit", listOf(
                    Interface.Method.Parameter("error", "GPUError")
                )
            )
        }
    }

    fun isEnumeration(typeName: String) = idlModel.enums.any { it.name == typeName }

    fun getEnumerationValueNameOnKotlin(typeName: String, value: String): String {
        val fixedValue = value
            .replace("\"", "")
            .replace("-", "")
            .fixNameStartingWithNumeric()
            .lowercase()
        return commonEnumerations.find { it.name == typeName }
            ?.let { enum -> enum.values.find { it.name.lowercase() == fixedValue }?.name }
            ?: error("enumeration not found with type $typeName and value $fixedValue")
    }

    fun MapperContext.isUnsignedNumericType(type: String): Boolean {
        return (typeAliases.find { it.name == type }
            ?.let { isUnsignedNumericType(it.type) })
            ?: (type in listOf("UInt", "ULong", "UShort"))
    }

    fun MapperContext.isFloatType(type: String): Boolean {
        return (typeAliases.find { it.name == type }
            ?.let { isUnsignedNumericType(it.type) })
            ?: (type in listOf("Float"))
    }
}


internal fun String.fixNameStartingWithNumeric(): String {
    return if (first().isDigit()) {
        when (first()) {
            '1' -> "One${substring(1)}"
            '2' -> "Two${substring(1)}"
            '3' -> "Three${substring(1)}"
            '4' -> "Four${substring(1)}"
            '5' -> "Five${substring(1)}"
            '6' -> "Six${substring(1)}"
            '7' -> "Seven${substring(1)}"
            '8' -> "Eight${substring(1)}"
            '9' -> "Nine${substring(1)}"
            '0' -> "Zero${substring(1)}"
            else -> error("Invalid name starting with numeric: $this")
        }
    } else this
}

private val interfaceToAddAutocloseableTrait = listOf(
    "GPUAdapter",
    "GPUBindGroup",
    "GPUBindGroupLayout",
    "GPUCommandBuffer",
    "GPUComputePipeline",
    "GPUPipelineLayout",
    "GPURenderPipeline",
    "GPUSampler",
    "GPUShaderModule",
    "GPUTextureView",
    "GPURenderBundleEncoder",
    "GPUCommandEncoder"
)