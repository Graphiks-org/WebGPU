package org.graphiks.webgpu.generator.mapper

import de.fabmax.webidl.model.IdlFunction
import de.fabmax.webidl.model.IdlFunctionParameter
import de.fabmax.webidl.model.IdlInterface
import de.fabmax.webidl.model.IdlSimpleType
import de.fabmax.webidl.model.IdlType
import org.graphiks.webgpu.generator.domain.Interface
import org.graphiks.webgpu.generator.domain.MapperContext

fun MapperContext.loadInterfaces() {
    idlModel.interfaces
        .filter { it.name.fixName() !in unwantedTypesOnCommon }
        .forEach { idlInterface ->
            val name = idlInterface.name.fixName()
            (interfaces.find { it.name == name } ?: Interface(name).also { interfaces.add(it) })
                .also { kinterface ->
                    injectSuperInterfaces(kinterface, idlInterface)
                    injectAttributes(idlInterface, kinterface)
                    injectFunctions(idlInterface, kinterface)
                }
        }
}

private fun injectFunctions(idlInterface: IdlInterface, kinterface: Interface) {
    idlInterface.functions
        .filter { it.returnType is IdlSimpleType && (it.returnType as IdlSimpleType).typeName !in unwantedTypesOnCommon }
        .filter { it.parameters.all { p -> p.type is IdlSimpleType && (p.type as IdlSimpleType).typeName !in unwantedTypesOnCommon } }
        .forEach { idlFunction ->
            removeOverloadedMethodWithFewerParams(kinterface, idlFunction)

            kinterface.methods += Interface.Method(
                idlFunction.name,
                idlFunction.returnType.toKotlinType(),
                idlFunction.parameters.map {
                    var (value, type) = computeValueAndType(it)

                    Interface.Method.Parameter(
                        it.name,
                        type,
                        value
                    )
                },
                idlFunction.returnType.isPromise()
            )
        }
}

private fun computeValueAndType(parameter: IdlFunctionParameter): Pair<String?, String> {
    var value = parameter.defaultValue
    var type = parameter.type.toKotlinType()

    if (value == "{}") {
        value = "null"
        if (type.endsWith("?").not()) {
            type = "$type?"
        }
    } else if (value != null && type.lowercase().contains("signed").not()) {
        value = "${value}u"
    }

    if (parameter.isOptional && parameter.defaultValue == null) {
        value = "null"
        type = "$type?"
    }
    return Pair(value, type)
}

private fun injectAttributes(idlInterface: IdlInterface, kinterface: Interface) {
    idlInterface.attributes
        .filter { it.type is IdlSimpleType && (it.type as IdlSimpleType).typeName !in unwantedTypesOnCommon }
        .forEach {
            kinterface.attributes += Interface.Attribute(it.name, it.type.toKotlinType(), it.isReadonly)
        }
}

private fun injectSuperInterfaces(kinterface: Interface, idlInterface: IdlInterface) {
    kinterface.extends += idlInterface.superInterfaces
    idlInterface.name.takeIf { it.contains(":") }
        ?.let {
            kinterface.extends += it.substringAfter(":")
                .split(",")
                .map { it.trim() }
                .filter { it !in unwantedTypesOnCommon }
        }
}

private fun removeOverloadedMethodWithFewerParams(
    kinterface: Interface,
    targetFunction: IdlFunction
) {
    fun isOverloadedWithFewerParams(method: Interface.Method): Boolean =
        method.name == targetFunction.name &&
                method.parameters.size < targetFunction.parameters.size

    kinterface.methods = kinterface.methods.filterNot(::isOverloadedWithFewerParams)
}

private fun IdlType.isPromise(): Boolean {
    return (this as? IdlSimpleType)?.typeName == "Promise"
}