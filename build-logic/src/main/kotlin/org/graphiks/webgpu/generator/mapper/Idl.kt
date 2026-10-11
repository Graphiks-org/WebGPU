package org.graphiks.webgpu.generator.mapper

import de.fabmax.webidl.model.IdlSimpleType
import de.fabmax.webidl.model.IdlType
import de.fabmax.webidl.model.IdlUnionType

internal val unwantedTypesOnCommon = setOf(
    // Types de navigateur
    "GPU",
    "EventTarget",
    "NavigatorGPU",
    "Navigator",
    "WorkerNavigator",
    "GPUPipelineErrorInit",
    "GPUPipelineError",
    "GPUPipelineErrorReason",

    // Types spécifiques au canvas web
    "GPUCanvasContext",
    "GPUCanvasConfiguration",
    "GPUCanvasAlphaMode",
    "GPUCanvasToneMappingMode",
    "GPUCanvasToneMapping",

    // Types dictionnaires redondants
    "GPUColorDict",
    "GPUOrigin2DDict",
    "GPUOrigin3DDict",
    "GPUExtent3DDict",

    // Types d'événements web
    "GPUUncapturedErrorEvent",
    "GPUUncapturedErrorEventInit",

    // Types liés aux textures web
    "GPUExternalTexture",
    "GPUExternalTextureDescriptor",
    "GPUExternalTextureBindingLayout",
    "GPUCopyExternalImageSource",
    "GPUCopyExternalImageDestInfo",
    "GPUCopyExternalImageSourceInfo"
)


internal fun IdlType.toWebKotlinType(): String = when (this) {
    is IdlSimpleType -> {
        val nullable = typeName.endsWith("?")
        val baseName = typeName.removeSuffix("?")
        val converted = when (baseName) {
            "unsigned long",
            "unsigned long long",
            "short",
            "unsigned short",
            "long",
            "long long",
            "float",
            "double" -> "JsNumber /* $this */"
            "boolean" -> "Boolean"
            "AllowSharedBufferSource" -> "js.buffer.ArrayBuffer /* $this */"
            "ArrayBuffer" -> "js.buffer.ArrayBuffer"
            "undefined" -> "Unit"
            "DOMString", "USVString" -> "String /* $this */"
            "sequence", "FrozenArray" -> {
                val elementType = this.parameterTypes?.get(0)
                val element = if (elementType?.endsWith("?") == true) "JsAny?" else "JsAny"
                "JsArray<$element> /* $this<$elementType> */"
            }
            "record" -> "WebGpuRecord /* $this<${this.parameterTypes?.get(0)}, ${this.parameterTypes?.get(1)}>  */"
            "Promise" -> {
                val nullableValue = parameterTypes?.firstOrNull()?.endsWith("?") == true
                "Promise<JsAny${if (nullableValue) "?" else ""}> /* $this */"
            }
            else -> when {
                baseName.startsWith("GPU") -> baseName
                else -> "JsAny /* $this */"
            }
        }
        converted.reapplyNullability(nullable)
    }
    is IdlUnionType ->  "JsAny /* $this */"
}

private fun String.reapplyNullability(nullable: Boolean): String {
    if (!nullable) return this
    val commentIndex = indexOf("/*")
    return if (commentIndex >= 0) {
        substring(0, commentIndex).trimEnd() + "? " + substring(commentIndex)
    } else {
        "$this?"
    }
}

internal fun IdlType.toKotlinType(): String = (this as IdlSimpleType).let {
    when (typeName) {
        "sequence", "FrozenArray" -> "List<${this.parameterTypes!!.first().toKotlinType()}>"
        "record" -> "Map<${this.parameterTypes!!.first().toKotlinType()}, ${this.parameterTypes!![1].toKotlinType()}>"
        "Promise" -> "Result<${this.parameterTypes!!.first().toKotlinType()}>"
        else -> typeName.toKotlinType()
    }
}

internal fun String.toKotlinType(): String {
    val nullable = endsWith("?")
    val baseName = removeSuffix("?")
    val converted = when (baseName) {
        "unsigned long" -> "UInt"
        "unsigned long long" -> "ULong"
        "short" -> "Short"
        "unsigned short" -> "UShort"
        "long" -> "Int"
        "long long" -> "Long"
        "float" -> "Float"
        "double" -> "Double"
        "DOMString", "USVString" -> "String"
        "boolean" -> "Boolean"
        "undefined" -> "Unit"
        "AllowSharedBufferSource" -> "ArrayBuffer"
        "Uint32Array" -> "List<UInt>"
        else -> baseName
    }
    return if (nullable) "$converted?" else converted
}

/**
 * Error on generator.parser, some interface name is on format Type : ExtendType instead of Type
 */
internal fun String.fixName(): String = (if (contains(':')) substringBefore(':') else this)
    .replace("\n", "")
    .trim()
