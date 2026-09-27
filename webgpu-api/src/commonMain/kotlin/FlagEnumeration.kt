@file:Suppress("unused")
package org.graphiks.webgpu

/**
 * Represents a generic interface for flag-based enumerations where each enumeration
 * is associated with a unique unsigned long integer value.
 *
 * Commonly used for defining bitflags that represent a combination of configuration options
 * or settings in a compact, efficient manner.
 *
 * Implementations of this interface should define their specific flag values as constants
 * within an enum class.
 */
interface FlagEnumeration {

    val value: ULong
}

/**
 * Converts a set of `FlagEnumeration` instances into a single integer representing the combined bitwise value.
 *
 * The computation aggregates the `value` property of all elements within the set using bitwise OR operations.
 * If the set is empty, the result is 0. If the set contains a single element, its value is returned.
 *
 * @return The combined bitmask as an integer, based on the union of all `FlagEnumeration` values in the set.
 */
fun Set<FlagEnumeration>.toFlagInt(): Int = when (size) {
    0 -> 0uL
    1 -> first().value
    else -> fold(0uL) { acc, enumerationWithValue -> acc or enumerationWithValue.value }
}.toInt()

/**
 * Converts a Set of FlagEnumeration elements into a ULong bitmask, combining their values
 * using bitwise OR operations.
 *
 * @return A ULong representing the combined bitmask of all FlagEnumeration values in the Set.
 *         Returns 0uL if the Set is empty, or the single element's value if the Set has one element.
 */
fun Set<FlagEnumeration>.toFlagULong(): ULong = when (size) {
    0 -> 0uL
    1 -> first().value
    else -> fold(0uL) { acc, enumerationWithValue -> acc or enumerationWithValue.value }
}
