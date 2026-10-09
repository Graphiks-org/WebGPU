@file:OptIn(ExperimentalUnsignedTypes::class)
@file:Suppress("DEPRECATION", "DEPRECATION_ERROR")

package org.graphiks.webgpu.benchmarks.runner

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.graphiks.webgpu.ArrayBuffer
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import sun.misc.Unsafe

@RunWith(AndroidJUnit4::class)
class ArrayBufferAndroidRegressionTest {
    @Test fun floatFactoryAndUniformWritesMatchGpuByteOrder() {
        val expected = byteArrayOf(0, 0, -128, 63)
        assertArrayEquals(expected, ArrayBuffer.of(floatArrayOf(1f)).toByteArray())
        val allocated = ArrayBuffer.allocate(4uL)
        allocated.setFloats(0uL, floatArrayOf(1f))
        assertArrayEquals(expected, allocated.toByteArray())
        assertEquals(1f, allocated.getFloat(0uL), 0f)
        assertEquals(1f, allocated.toFloatArray()[0], 0f)
    }
    @Test fun borrowedBulkReadsAndWritesWorkOnArtWithoutJvmUnsafeConstants() {
        val field = Unsafe::class.java.getDeclaredField("theUnsafe")
        field.isAccessible = true
        val unsafe = field.get(null) as Unsafe
        val address = unsafe.allocateMemory(16)
        try {
            val data = ArrayBuffer.wrap(address, 16uL)
            data.setBytes(0uL, byteArrayOf(1, 2, 3, 4))
            assertArrayEquals(byteArrayOf(1, 2, 3, 4), data.toByteArray().copyOf(4))
            data.setFloat(8uL, 1f)
            assertEquals(1f, data.toFloatArray()[2], 0f)
            assertEquals(1f, unsafe.getFloat(address + 8), 0f)
        } finally { unsafe.freeMemory(address) }
    }
}
