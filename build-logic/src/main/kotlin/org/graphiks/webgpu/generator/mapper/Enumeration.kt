package org.graphiks.webgpu.generator.mapper

import com.squareup.kotlinpoet.ARRAY
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import org.graphiks.webgpu.generator.domain.Enumeration
import org.graphiks.webgpu.generator.domain.MapperContext
import org.graphiks.webgpu.generator.domain.fixNameStartingWithNumeric

fun MapperContext.loadEnums() {
    loadBitFlagEnums()
    loadConventionalEnums()
}

private fun MapperContext.loadBitFlagEnums() {
    yamlModel.bitflags.forEach { bitflag ->
        val name = bitflag.name.convertToKotlinClassName().let { if (it.endsWith("Mask")) it.substringBeforeLast("Mask") else it  }

        val className = ClassName("org.graphiks.webgpu", "GPU$name")
        bitflagEnumerations += TypeSpec.classBuilder(className)
            .addAnnotation(JvmInline::class)
            .addModifiers(KModifier.VALUE)
            .primaryConstructor(
                FunSpec.constructorBuilder()
                    .addParameter("value", ULong::class)
                    .addModifiers(KModifier.PRIVATE)
                    .build()
            )
            .addProperty(
                PropertySpec.builder("value", ULong::class)
                    .initializer("value")
                    .build()
            )
            .addType(
                TypeSpec.companionObjectBuilder()
                    .addProperties(
                        bitflag.entries.mapIndexed { index, entry ->
                            val value =
                                entry.value_combination?.sumOf { subPart -> indexToFlagValue(bitflag.entries.indexOfFirst { it.name == subPart }) }
                                    ?: indexToFlagValue(index)

                            PropertySpec.builder(entry.name.convertToKotlinClassName(), className)
                                .initializer("${className.simpleName}(${value}uL)")
                                .build()
                        }
                    )
                    .addProperty(
                        PropertySpec.builder("entries", ClassName("kotlin.collections", "Set").parameterizedBy(className))
                            .initializer("setOf(${bitflag.entries.joinToString { it.name.convertToKotlinClassName() }})")
                            .build()
                    )
                    .build()
            )
            .addFunction(
                FunSpec.builder("or")
                    .addModifiers(KModifier.INFIX)
                    .addParameter("other", className)
                    .returns(className)
                    .addCode("return ${className.simpleName}(value or other.value)")
                    .build()
            )
            .addFunction(
                FunSpec.builder("of")
                    .addModifiers(KModifier.INFIX)
                    .addParameter("values", ARRAY.parameterizedBy(className))
                    .returns(className)
                    .addCode("return values.fold(${className.simpleName}.None) { acc, enumeration -> acc or enumeration }")
                    .build()
            )
            .build()

    }
}

private fun indexToFlagValue(base: Int): Int = if (base == 0) 0 else 1 shl (base - 1)

private fun MapperContext.loadConventionalEnums() {
    yamlModel.enums.forEach { yamlEnum ->
        val name = "GPU${yamlEnum.name.convertToKotlinClassName()}"
        val idlEnum = idlModel.enums.find { it.name == name }
        if (idlEnum == null) {
            println("Skipping native enumeration = $name")
            return@forEach
        }
        commonEnumerations += Enumeration(
            name,
            yamlEnum.values
                .filter { it.name != "undefined" && it.name != "null" }
                .map { value ->
                    Enumeration.Value(value.name.convertToKotlinClassName()
                        .fixNameStartingWithNumeric())
                },
            isExpect = true
        ).also { enumeration ->
            commonWebEnumerations += enumeration.copy(
                isExpect = false, isActual = true,
                parameters = listOf("val value: String"),
                extra = generateExtra(name, "String"),
                values = enumeration.values.map { enumerationValue ->
                    val webValue = idlEnum.values.firstOrNull {
                        enumerationValue.name.lowercase() == it.replace("-", "").fixNameStartingWithNumeric().lowercase()
                    } ?: "unsupported"
                    Enumeration.Value("${enumerationValue.name}(\"$webValue\")")
                }
            )
            commonNativeEnumerations += enumeration.copy(
                isExpect = false, isActual = true,
                parameters = listOf("val value: UInt"),
                extra = generateExtra(name, "UInt"),
                values = enumeration.values.map { enumerationValue ->
                    val nativeValue = yamlEnum.values.first {
                        enumerationValue.name.lowercase() == it.name.convertToKotlinClassName().fixNameStartingWithNumeric()
                            .lowercase()
                    }.let {
                        it.value
                            ?: yamlEnum.values.indexOf(it)
                    }
                    Enumeration.Value("${enumerationValue.name}(${nativeValue}u)")
                }
            )
        }

    }

    idlModel.enums.filter { it.name.fixName() !in unwantedTypesOnCommon }
        .forEach { idlEnum ->
            if (commonEnumerations.none { it.name == idlEnum.name }) println("Enum from Idl not found: ${idlEnum.name}")
        }
}

private fun generateExtra(name: String, type: String): String = """

	companion object {
		/**
		 * Retrieves the corresponding [$type] for the given value.
		 *
		 * @param value The dependent platform value representing the WebGPU value.
		 * @return The matching [$type] or `null` if no match is found.
		 */
		fun of(value: $type): $name? {
			return entries.find { it.value == value }
		}
    }

"""

internal fun String.convertToKotlinClassName() = split("_")
        .joinToString("") { component -> component.replaceFirstChar { it.uppercase() } }



