package org.graphiks.webgpu.generator

import de.fabmax.webidl.parser.WebIdlParser
import org.graphiks.webgpu.generator.domain.MapperContext
import org.graphiks.webgpu.generator.domain.YamlModel

internal fun testContext(idl: String) = MapperContext(
    WebIdlParser.parseFromInputStream(idl.byteInputStream()),
    YamlModel(
        copyright = "", name = "", enum_prefix = "",
        constants = emptyList(), typedefs = emptyList(), bitflags = emptyList(),
        structs = emptyList(), functions = emptyList(), objects = emptyList(), enums = emptyList(),
    ),
)
