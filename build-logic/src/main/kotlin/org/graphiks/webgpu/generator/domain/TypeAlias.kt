package org.graphiks.webgpu.generator.domain

class TypeAlias(val name: String, var type: String) {
    override fun toString(): String {
        return "typealias $name = $type"
    }
}