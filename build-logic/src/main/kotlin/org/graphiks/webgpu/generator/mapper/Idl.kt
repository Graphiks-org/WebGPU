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
    is IdlSimpleType -> when (typeName) {
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
        "sequence", "FrozenArray" -> "JsArray<JsAny> /* $this<${this.parameterTypes?.get(0)}> */"
        "record" -> "JsMap<JsAny, JsAny> /* $this<${this.parameterTypes?.get(0)}, ${this.parameterTypes?.get(1)}>  */"
        "Promise" -> "Promise<JsAny> /* $this */"
        else -> when {
            typeName.startsWith("GPU") -> typeName
            else -> "JsAny /* $this */"
        }
    }
    is IdlUnionType ->  "JsAny /* $this */"
}

internal fun IdlType.toKotlinType(): String = (this as IdlSimpleType).let {
    when (typeName) {
        "sequence", "FrozenArray" -> "List<${this.parameterTypes!!.first().toKotlinType().removeSuffix("?")}>"
        "record" -> "Map<${this.parameterTypes!!.first().toKotlinType()}, ${this.parameterTypes!![1].toKotlinType()}>"
        "Promise" -> "Result<${this.parameterTypes!!.first().toKotlinType()}>"
        else -> typeName.toKotlinType()
    }
}

internal fun String.toKotlinType(): String = when (this) {
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
    else -> this
}

/**
 * Error on generator.parser, some interface name is on format Type : ExtendType instead of Type
 */
internal fun String.fixName(): String = (if (contains(':')) substringBefore(':') else this)
    .replace("\n", "")
    .trim()
