package org.graphiks.webgpu.generator.mapper

import de.fabmax.webidl.model.IdlDictionary
import de.fabmax.webidl.model.IdlMember
import org.graphiks.webgpu.generator.domain.DescriptorClass
import org.graphiks.webgpu.generator.domain.MapperContext
import kotlin.collections.plusAssign

fun MapperContext.loadDescriptors() {
    idlModel.dictionaries
        .filter { it.name.fixName() !in unwantedTypesOnCommon }
        .forEach { loadDescriptor(it.name.fixName(), it) }
}

internal fun MapperContext.loadDescriptor(name: String, idlDictionary: IdlDictionary) {
    val parameters = getMembers(idlDictionary)
        .mapNotNull { mapCommonDictionaryMember(name, it, formatDescriptorDefault = true) }
        .map { DescriptorClass.Parameter(it.name, it.type, it.defaultValue) }
    descriptors += DescriptorClass(name, parameters)
}

fun MapperContext.getMembers(idlDictionary: IdlDictionary): List<IdlMember> {
    return idlDictionary.members +
            idlDictionary.superDictionaries.flatMap { getMembers(idlModel.dictionaries.find { dictionary -> dictionary.name == it }!!) } +
            getGhostMembers(idlDictionary)

}

/**
 * To remove when fixe on parsing library
 * Retrieves a list of ghost members related to the given `IdlDictionary`.
 *
 * Ghost members are those members derived from the `name` property of the provided `IdlDictionary`.
 * If `name` contains a colon (":"), a subset of types is parsed from it, excluding unwanted types,
 * and their corresponding members are fetched recursively.
 *
 * @param idlDictionary The `IdlDictionary` from which ghost members are extracted.
 * @return A list of `IdlMember` objects representing the ghost members. Returns an empty list if `name` does not contain a colon or there are no valid ghost members.
 */
fun MapperContext.getGhostMembers(idlDictionary: IdlDictionary): List<IdlMember> {
    return idlDictionary.name.takeIf { it.contains(":") }
        ?.let {
            it.substringAfter(":")
                .split(",")
                .map { it.trim() }
                .filter { it !in unwantedTypesOnCommon }
                .flatMap {
                    getMembers(idlModel.dictionaries.find { dictionary -> dictionary.name.fixName() == it }
                        ?: error("Ghost member not found: $it"))
                }
        } ?: emptyList()
}
