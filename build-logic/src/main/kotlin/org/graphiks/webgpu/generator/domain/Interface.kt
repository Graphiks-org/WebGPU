package org.graphiks.webgpu.generator.domain

class Interface(
    val name: String,
    var sealed: Boolean = false,
    val external: Boolean = false,
    val functional: Boolean = false,
) {
    var extends: Set<String> = emptySet()
    var attributes: List<Attribute> = emptyList()
    var methods: List<Method> = emptyList()
    var kDoc: KDoc? = null

    class Method(
        val name: String,
        var returnType: String,
        var parameters: List<Parameter>,
        val isSuspend: Boolean = false
    ) {
        var kDoc: KDoc? = null

        class Parameter(val name: String, var type: String, var defaultValue: String? = null) {
            override fun toString(): String = StringBuilder().apply {
                append("$name: $type")
                if (defaultValue != null) append(" = $defaultValue")
            }.toString()
        }

        override fun toString(): String = StringBuilder().apply {
            kDoc?.let { append(it) }
            append("\t")
            if (isSuspend) append("suspend ")
            append("fun $name(")
            append(parameters.joinToString(", "))
            append(")")
            if (returnType != "Unit") append(": $returnType")
        }.toString()
    }

    class Attribute(
        val name: String,
        var type: String,
        val isConstant: Boolean
    ) {

        var kDoc: KDoc? = null

        override fun toString(): String = StringBuilder().apply {
            kDoc?.let { append(it) }
            append("\t")
            if (isConstant) append("val") else append("var")
            append(" $name: $type")
        }.toString()
    }

    override fun toString(): String {
        val builder = StringBuilder().apply {
            kDoc?.let { append(it) }
            if (sealed) append("sealed ")
            if (external) append("external ")
            if (functional) append("fun ")
            append("interface $name")
            if (extends.isNotEmpty()) {
                append(" : ")
                append(extends.joinToString(", "))
            }
            if (attributes.isNotEmpty() || methods.isNotEmpty()) append(" {\n")
            append(attributes.joinToString("\n"))
            if (attributes.isNotEmpty()) append("\n")
            append(methods.joinToString("\n"))
            if (methods.isNotEmpty()) append("\n")
            if (attributes.isNotEmpty() || methods.isNotEmpty()) append("}\n")
        }
        return builder.toString()
    }
}