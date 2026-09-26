package generator.mapper

import de.fabmax.webidl.model.IdlDictionary
import generator.domain.Interface
import generator.domain.MapperContext

internal fun MapperContext.loadDictionaries() {
    idlModel.dictionaries
        .filter { it.name.fixName() !in unwantedTypesOnCommon }
        .forEach { idlDictionary ->
            val name = idlDictionary.name.fixName()
            loadDictionary(name, idlDictionary).also { kinterface ->
                idlDictionary.name.takeIf { it.contains(":") }
                    ?.let {
                        kinterface.extends += it.substringAfter(":")
                            .split(",")
                            .map { it.trim() }
                            .filter { it !in unwantedTypesOnCommon }
                    }
            }
        }
}

internal fun MapperContext.loadDictionary(name: String, idlDictionary: IdlDictionary): Interface {
    return (interfaces.find { it.name == name } ?: Interface(name).also { interfaces.add(it) })
        .also { kinterface ->
            kinterface.extends += idlDictionary.superDictionaries

            idlDictionary.members
                .mapNotNull { mapCommonDictionaryMember(name, it) }
                .forEach { member ->
                    kinterface.attributes += Interface.Attribute(member.name, member.type, true)
                }
        }
}
