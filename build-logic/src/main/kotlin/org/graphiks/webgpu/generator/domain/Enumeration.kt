package org.graphiks.webgpu.generator.domain

data class Enumeration(
    var name: String,
    val values: List<Value>,
    val parameters: List<String> = emptyList(),
    val isActual: Boolean = false, val isExpect: Boolean = false,
    val extra: String? = null,
    val extends: List<String> = emptyList()
) {
    var kDoc: KDoc? = null

    data class Value(val name: String) {
        var kDoc: KDoc? = null

        override fun toString(): String = StringBuilder().apply {
            kDoc?.let { append(it) }
            append("\t")
            append(name)
        }.toString()
    }

    init {
        if (isActual && isExpect) throw IllegalArgumentException("Enumeration cannot be actual and expect at the same time")
    }

    override fun toString(): String = StringBuilder().apply {
        kDoc?.let { append(it) }
        if (isActual) append("actual ")
        if (isExpect) append("expect ")
        append("enum class $name")
        if (parameters.isNotEmpty()) append("(${parameters.joinToString(", ")})")
        if (extends.isNotEmpty()) {
            append(": ")
            append(extends.joinToString(", "))
        }
        append(" {\n")
        append(values.joinToString(",\n") { it.toString() })
        append(";\n")

        if (extra != null) append(extra)

        append("}\n")
    }.toString()
}