@file:OptIn(ExperimentalUnsignedTypes::class)

package org.graphiks.webgpu

import js.typedarrays.*

@Suppress("NOTHING_TO_INLINE")
internal actual inline fun ByteArray.toArrayBuffer(): ArrayBuffer
    = ArrayBuffer.wrap(unsafeCast<Int8Array<js.buffer.ArrayBuffer>>().buffer)

@Suppress("NOTHING_TO_INLINE")
internal actual inline fun ShortArray.toArrayBuffer(): ArrayBuffer
    = ArrayBuffer.wrap(unsafeCast<Int16Array<js.buffer.ArrayBuffer>>().buffer)

@Suppress("NOTHING_TO_INLINE")
internal actual inline fun IntArray.toArrayBuffer(): ArrayBuffer
    = ArrayBuffer.wrap(unsafeCast<Int32Array<js.buffer.ArrayBuffer>>().buffer)


@Suppress("NOTHING_TO_INLINE")
internal actual inline fun FloatArray.toArrayBuffer(): ArrayBuffer
    = ArrayBuffer.wrap(unsafeCast<Float32Array<js.buffer.ArrayBuffer>>().buffer)

@Suppress("NOTHING_TO_INLINE")
internal actual inline fun DoubleArray.toArrayBuffer(): ArrayBuffer
    = ArrayBuffer.wrap(unsafeCast<Float64Array<js.buffer.ArrayBuffer>>().buffer)

@Suppress("NOTHING_TO_INLINE")
internal actual inline fun UByteArray.toArrayBuffer(): ArrayBuffer
    = asByteArray().toArrayBuffer()

@Suppress("NOTHING_TO_INLINE")
internal actual inline fun UShortArray.toArrayBuffer(): ArrayBuffer
    = asShortArray().toArrayBuffer()

@Suppress("NOTHING_TO_INLINE")
internal actual inline fun UIntArray.toArrayBuffer(): ArrayBuffer
    = asIntArray().toArrayBuffer()

// Read methods - convert ArrayBuffer to typed arrays
@Suppress("NOTHING_TO_INLINE")
internal actual inline fun js.buffer.ArrayBuffer.readByteArray(): ByteArray {
    val view = Int8Array<js.buffer.ArrayBuffer>(this)
    return ByteArray(view.length) { view[it].unsafeCast<Byte>() }
}

@Suppress("NOTHING_TO_INLINE")
internal actual inline fun js.buffer.ArrayBuffer.readShortArray(): ShortArray {
    val view = Int16Array<js.buffer.ArrayBuffer>(this)
    return ShortArray(view.length) { view[it].unsafeCast<Short>() }
}

@Suppress("NOTHING_TO_INLINE")
internal actual inline fun js.buffer.ArrayBuffer.readIntArray(): IntArray {
    val view = Int32Array<js.buffer.ArrayBuffer>(this)
    return IntArray(view.length) { view[it].unsafeCast<Int>() }
}


@Suppress("NOTHING_TO_INLINE")
internal actual inline fun js.buffer.ArrayBuffer.readFloatArray(): FloatArray {
    val view = Float32Array<js.buffer.ArrayBuffer>(this)
    return FloatArray(view.length) { view[it].unsafeCast<Float>() }
}

@Suppress("NOTHING_TO_INLINE")
internal actual inline fun js.buffer.ArrayBuffer.readDoubleArray(): DoubleArray {
    val view = Float64Array<js.buffer.ArrayBuffer>(this)
    return DoubleArray(view.length) { view[it].unsafeCast<Double>() }
}

@Suppress("NOTHING_TO_INLINE")
internal actual inline fun js.buffer.ArrayBuffer.readUByteArray(): UByteArray {
    return readByteArray().asUByteArray()
}

@Suppress("NOTHING_TO_INLINE")
internal actual inline fun js.buffer.ArrayBuffer.readUShortArray(): UShortArray {
    return readShortArray().asUShortArray()
}

@Suppress("NOTHING_TO_INLINE")
internal actual inline fun js.buffer.ArrayBuffer.readUIntArray(): UIntArray {
    return readIntArray().asUIntArray()
}

// Indexed read methods
@Suppress("NOTHING_TO_INLINE")
internal actual inline fun js.buffer.ArrayBuffer.readByte(offset: Int): Byte {
    val view = Int8Array<js.buffer.ArrayBuffer>(
        this, offset, 1
    )
    return view[0].unsafeCast<Byte>()
}

@Suppress("NOTHING_TO_INLINE")
internal actual inline fun js.buffer.ArrayBuffer.readShort(offset: Int): Short {
    val view = Int16Array<js.buffer.ArrayBuffer>(
        this, offset, 1
    )
    return view[0].unsafeCast<Short>()
}

@Suppress("NOTHING_TO_INLINE")
internal actual inline fun js.buffer.ArrayBuffer.readInt(offset: Int): Int {
    val view = Int32Array<js.buffer.ArrayBuffer>(
        this, offset, 1
    )
    return view[0].unsafeCast<Int>()
}


@Suppress("NOTHING_TO_INLINE")
internal actual inline fun js.buffer.ArrayBuffer.readFloat(offset: Int): Float {
    val view = Float32Array<js.buffer.ArrayBuffer>(
        this, offset, 1
    )
    return view[0].unsafeCast<Float>()
}

@Suppress("NOTHING_TO_INLINE")
internal actual inline fun js.buffer.ArrayBuffer.readDouble(offset: Int): Double {
    val view = Float64Array<js.buffer.ArrayBuffer>(
        this, offset, 1
    )
    return view[0].unsafeCast<Double>()
}

@Suppress("NOTHING_TO_INLINE")
internal actual inline fun js.buffer.ArrayBuffer.readUByte(offset: Int): UByte {
    val view = Uint8Array<js.buffer.ArrayBuffer>(
        this, offset, 1
    )
    return view[0].unsafeCast<Int>().toUByte()
}

@Suppress("NOTHING_TO_INLINE")
internal actual inline fun js.buffer.ArrayBuffer.readUShort(offset: Int): UShort {
    val view = Uint16Array<js.buffer.ArrayBuffer>(
        this, offset, 1
    )
    return view[0].unsafeCast<Int>().toUShort()
}

@Suppress("NOTHING_TO_INLINE")
internal actual inline fun js.buffer.ArrayBuffer.readUInt(offset: Int): UInt {
    val view = Int32Array<js.buffer.ArrayBuffer>(
        this, offset, 1
    )
    return view[0].toUInt()
}


// Indexed write methods
@Suppress("NOTHING_TO_INLINE")
internal actual inline fun js.buffer.ArrayBuffer.writeByte(offset: Int, value: Byte) {
    val view = Int8Array<js.buffer.ArrayBuffer>(
        this, offset, 1
    )
    view[0] = value
}

@Suppress("NOTHING_TO_INLINE")
internal actual inline fun js.buffer.ArrayBuffer.writeShort(offset: Int, value: Short) {
    val view = Int16Array<js.buffer.ArrayBuffer>(
        this, offset, 1
    )
    view[0] = value
}

@Suppress("NOTHING_TO_INLINE")
internal actual inline fun js.buffer.ArrayBuffer.writeInt(offset: Int, value: Int) {
    val view = Int32Array<js.buffer.ArrayBuffer>(
        this, offset, 1
    )
    view[0] = value
}


@Suppress("NOTHING_TO_INLINE")
internal actual inline fun js.buffer.ArrayBuffer.writeFloat(offset: Int, value: Float) {
    val view = Float32Array<js.buffer.ArrayBuffer>(
        this, offset, 1
    )
    view[0] = value
}

@Suppress("NOTHING_TO_INLINE")
internal actual inline fun js.buffer.ArrayBuffer.writeDouble(offset: Int, value: Double) {
    val view = Float64Array<js.buffer.ArrayBuffer>(
        this, offset, 1
    )
    view[0] = value
}

@Suppress("NOTHING_TO_INLINE")
internal actual inline fun js.buffer.ArrayBuffer.writeUByte(offset: Int, value: UByte) {
    val view = Uint8Array<js.buffer.ArrayBuffer>(
        this, offset, 1
    )
    view[0] = value.toShort()
}

@Suppress("NOTHING_TO_INLINE")
internal actual inline fun js.buffer.ArrayBuffer.writeUShort(offset: Int, value: UShort) {
    val view = Uint16Array<js.buffer.ArrayBuffer>(
        this, offset, 1
    )
    view[0] = value.toInt()
}

@Suppress("NOTHING_TO_INLINE")
internal actual inline fun js.buffer.ArrayBuffer.writeUInt(offset: Int, value: UInt) {
    val view = Uint32Array<js.buffer.ArrayBuffer>(
        this, offset, 1
    )
    view[0] = value.toInt()
}

// Array write methods
@Suppress("NOTHING_TO_INLINE")
internal actual inline fun js.buffer.ArrayBuffer.writeByteArray(offset: Int, array: ByteArray) {
    val view = Int8Array<js.buffer.ArrayBuffer>(this, offset, array.size)
    array.forEachIndexed { index, value ->
        view[index] = value
    }
}

@Suppress("NOTHING_TO_INLINE")
internal actual inline fun js.buffer.ArrayBuffer.writeShortArray(offset: Int, array: ShortArray) {
    val view = Int16Array<js.buffer.ArrayBuffer>(this, offset, array.size)
    array.forEachIndexed { index, value ->
        view[index] = value
    }
}

@Suppress("NOTHING_TO_INLINE")
internal actual inline fun js.buffer.ArrayBuffer.writeIntArray(offset: Int, array: IntArray) {
    val view = Int32Array<js.buffer.ArrayBuffer>(this, offset, array.size)
    array.forEachIndexed { index, value ->
        view[index] = value
    }
}

@Suppress("NOTHING_TO_INLINE")
internal actual inline fun js.buffer.ArrayBuffer.writeFloatArray(offset: Int, array: FloatArray) {
    val view = Float32Array<js.buffer.ArrayBuffer>(this, offset, array.size)
    array.forEachIndexed { index, value ->
        view[index] = value
    }
}

@Suppress("NOTHING_TO_INLINE")
internal actual inline fun js.buffer.ArrayBuffer.writeDoubleArray(offset: Int, array: DoubleArray) {
    val view = Float64Array<js.buffer.ArrayBuffer>(this, offset, array.size)
    array.forEachIndexed { index, value ->
        view[index] = value
    }
}

@Suppress("NOTHING_TO_INLINE")
internal actual inline fun js.buffer.ArrayBuffer.writeUByteArray(offset: Int, array: UByteArray) {
    val view = Uint8Array<js.buffer.ArrayBuffer>(this, offset, array.size)
    array.forEachIndexed { index, value ->
        view[index] = value.toShort()
    }
}

@Suppress("NOTHING_TO_INLINE")
internal actual inline fun js.buffer.ArrayBuffer.writeUShortArray(offset: Int, array: UShortArray) {
    val view = Uint16Array<js.buffer.ArrayBuffer>(this, offset, array.size)
    array.forEachIndexed { index, value ->
        view[index] = value.toInt()
    }
}

@Suppress("NOTHING_TO_INLINE")
internal actual inline fun js.buffer.ArrayBuffer.writeUIntArray(offset: Int, array: UIntArray) {
    val view = Uint32Array<js.buffer.ArrayBuffer>(this, offset, array.size)
    array.forEachIndexed { index, value ->
        view[index] = value.toInt()
    }
}

