package org.graphiks.webgpu.generator.mapper

import de.fabmax.webidl.model.IdlDictionary
import de.fabmax.webidl.model.IdlInterface
import org.graphiks.webgpu.generator.domain.Interface
import org.graphiks.webgpu.generator.domain.MapperContext
import org.graphiks.webgpu.generator.domain.TypeAlias

internal fun MapperContext.loadWebInterfaces() {
    // Load web-specific interfaces from the IDL model
    idlModel.interfaces
        .filter { it.name.startsWith("GPU") && it.setLike == null}
        .forEach { idlInterface ->
            loadWebInterface(idlInterface)
        }

    // Load web-specific dictionaries from the IDL model
    idlModel.dictionaries
        .filter { it.name.startsWith("GPU") }
        .forEach { idlDictionary ->
            loadWebDictionary(idlDictionary)
        }

    // transform setlike as Typealias
    idlModel.interfaces
        .filter { it.name.startsWith("GPU") && it.setLike != null}
        .forEach { idlInterface ->
            val name = "W" + idlInterface.name.fixName()
            (webTypeAlias.find { it.name == name } ?: TypeAlias(
                name,
                "JsSet<JsAny /* ${idlInterface.setLike?.type} */>"
            ).also { webTypeAlias.add(it) })
        }

    //
    webInterfaces.forEach {  kinterface ->
        kinterface.extends = kinterface.extends
            .map { if(it.startsWith("GPU")) "W$it" else it }
            .toSet()

        kinterface.attributes
            .forEach {  attribute ->
                attribute.type = convertType(attribute.type)
            }

        kinterface.methods
            .forEach { method ->
                method.parameters.
                    forEach { parameter ->
                        parameter.type = convertType(parameter.type)
                    }

                method.returnType = convertType(method.returnType)
            }
    }
}

fun MapperContext.convertType(type: String): String = when {
    type.startsWith("GPU") -> when {
        webInterfaces.any { "W${type}" == it.name } -> "W${type}  /* $type */"
        webTypeAlias.any { "W${type}" == it.name } -> "W${type}  /* $type */"
        commonEnumerations.any { type == it.name } -> "String  /* $type */"
        isNumberTypeAlias(type) -> "JsNumber  /* $type */"
        else -> "JsAny /* $type */"
    }
    else -> type
}

fun MapperContext.isNumberTypeAlias(type: String): Boolean {
    typeAliases.find { it.name == type }?.let {
        return it.type in listOf("Float", "Double", "Short", "UShort", "Int", "UInt", "Long", "ULong")
    }

    return false
}

private fun MapperContext.loadWebInterface(idlInterface: IdlInterface) {
    val name = "W" + idlInterface.name.fixName()
    (webInterfaces.find { it.name == name } ?: Interface(name, external = true).also { webInterfaces.add(it) })
        .also { kinterface ->
            if (kinterface.extends.contains("JsAny").not()) kinterface.extends += "JsAny"

            // Add extends
            kinterface.extends += idlInterface.superInterfaces
            idlInterface.name.takeIf { it.contains(":") }
                ?.let {
                    kinterface.extends += it.substringAfter(":")
                        .split(",")
                        .map { it.trim() }
                }

            // Add attributes
            idlInterface.attributes
                .forEach {
                    kinterface.attributes += Interface.Attribute(it.name, it.type.toWebKotlinType(), false)
                }

            // Add methods
            idlInterface.functions
                .forEach { idlFunction ->
                    kinterface.methods += Interface.Method(
                        idlFunction.name,
                        idlFunction.returnType.toWebKotlinType(),
                        idlFunction.parameters.map {
                            Interface.Method.Parameter(
                                it.name,
                                it.type.toWebKotlinType()
                            )
                        },
                        // No suspend on Web
                        isSuspend = false
                    )
                }
        }
}

private fun MapperContext.loadWebDictionary(idlDictionary: IdlDictionary): Interface {
    val name = "W" + idlDictionary.name.fixName()
        .let { if (it.endsWith("Dict")) it.substringBeforeLast("Dict") else it }
    return (webInterfaces.find { it.name == name } ?: Interface(name, external = true).also { webInterfaces.add(it) })
        .also { kinterface ->
            if (kinterface.extends.contains("JsAny").not()) kinterface.extends += "JsAny"
            kinterface.extends += idlDictionary.superDictionaries
            idlDictionary.name.takeIf { it.contains(":") }
                ?.let {
                    kinterface.extends += it.substringAfter(":")
                        .split(",")
                        .map { it.trim() }
                }

            idlDictionary.members
                .forEach {
                    kinterface.attributes += Interface.Attribute(it.name, it.type.toWebKotlinType() , false)
                }
        }
}