package generator.mapper

import de.fabmax.webidl.model.IdlMember
import de.fabmax.webidl.model.IdlSimpleType
import de.fabmax.webidl.model.IdlType
import de.fabmax.webidl.model.IdlUnionType
import generator.domain.Interface
import generator.domain.MapperContext

internal data class CommonDictionaryMember(
    val name: String,
    val type: String,
    val defaultValue: String?,
)

/** Maps a dictionary member once so its common interface and descriptor stay in sync. */
internal fun MapperContext.mapCommonDictionaryMember(
    dictionaryName: String,
    member: IdlMember,
    formatDescriptorDefault: Boolean = false,
): CommonDictionaryMember? {
    var defaultValue = member.defaultValue
    var type = when (val idlType = member.type) {
        is IdlSimpleType -> {
            if (idlType.typeName in unwantedTypesOnCommon && member.name != "layout") return null
            if (dictionaryName == "GPUTextureViewDescriptor" && member.name == "swizzle") {
                "GPUTextureSwizzle"
            } else {
                idlType.toKotlinType()
            }
        }

        is IdlUnionType -> {
            if (member.name == "layout") {
                // Keep the current API convention where null represents GPUAutoLayoutMode("auto").
                val layoutType = idlType.types
                    .filterIsInstance<IdlSimpleType>()
                    .firstOrNull { it.typeName == "GPUPipelineLayout" }
                    ?: unsupportedDictionaryMemberType(dictionaryName, member)
                defaultValue = "null"
                "${layoutType.toKotlinType()}?"
            } else {
                registerCommonGpuUnion(idlType.types, "$dictionaryName.${member.name}")
            }
        }

    }

    if (defaultValue == null && member.isRequired.not() && !type.endsWith("?")) {
        defaultValue = "null"
        type += "?"
    }

    if (formatDescriptorDefault && member.defaultValue != null) {
        when {
            dictionaryName == "GPUTextureViewDescriptor" && member.name == "swizzle" -> {
                require(defaultValue == "\"rgba\"") {
                    "Unsupported WebIDL default for GPUTextureViewDescriptor.swizzle: $defaultValue"
                }
                defaultValue = "GPUTextureSwizzle()"
            }
            defaultValue == "{}" && type.startsWith("Map<") -> defaultValue = "emptyMap()"
            defaultValue == "{}" -> defaultValue = "${type.removePrefix("GPU")}()"
            isUnsignedNumericType(type) -> defaultValue = "${defaultValue}u"
            isFloatType(type) -> defaultValue = "${defaultValue}f"
            defaultValue == "[]" -> defaultValue = "emptyList()"
            isEnumeration(type) -> defaultValue = "$type.${getEnumerationValueNameOnKotlin(type, defaultValue!!)}"
        }
    }

    return CommonDictionaryMember(member.name, type, defaultValue)
}

internal fun MapperContext.registerCommonGpuUnion(
    alternatives: List<IdlType>,
    context: String,
    unionName: String? = null,
): String {
    val alternativeNames = alternatives.map { alternative ->
        val simpleType = alternative as? IdlSimpleType
            ?: unsupportedGpuUnion(context, alternatives)
        simpleType.typeName
    }
    val declaredGpuTypes = (
        idlModel.interfaces.map { it.name.fixName() } + idlModel.dictionaries.map { it.name.fixName() }
    ).toSet()
    val unsupportedNames = alternativeNames.filter {
        !it.startsWith("GPU") || it in unwantedTypesOnCommon || it !in declaredGpuTypes
    }
    val normalizedNames = alternativeNames.distinct().sorted()

    if (unsupportedNames.isNotEmpty() || normalizedNames.size < 2) {
        unsupportedGpuUnion(context, alternatives)
    }

    val resolvedUnionName = unionName ?: normalizedNames.joinToString("Or")
    val existingUnion = interfaces.find { it.name == resolvedUnionName }
    if (existingUnion != null && !existingUnion.sealed) {
        throw IllegalArgumentException(
            "WebIDL union $resolvedUnionName at $context collides with a non-sealed Kotlin interface",
        )
    }
    if (existingUnion == null) {
        interfaces += Interface(resolvedUnionName, sealed = true)
    }

    normalizedNames.forEach { alternativeName ->
        if (alternativeName == resolvedUnionName) {
            unsupportedGpuUnion(context, alternatives)
        }
        val alternativeInterface = interfaces.find { it.name == alternativeName }
            ?: Interface(alternativeName).also { interfaces += it }
        alternativeInterface.extends += resolvedUnionName
    }

    return resolvedUnionName
}

private fun unsupportedDictionaryMemberType(dictionaryName: String, member: IdlMember): Nothing =
    throw IllegalArgumentException(
        "Unsupported WebIDL member type at $dictionaryName.${member.name}: ${member.type}",
    )

private fun unsupportedGpuUnion(context: String, alternatives: List<IdlType>): Nothing =
    throw IllegalArgumentException(
        "Unsupported WebIDL union at $context: ${alternatives.joinToString(" or ")}. " +
            "Expected at least two declared GPU interface or dictionary types.",
    )
