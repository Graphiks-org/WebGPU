@file:OptIn(ExperimentalUnsignedTypes::class)

package org.graphiks.webgpu

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue

class BitFlagsTest : FreeSpec({

    "contains means all requested bits are present" {
        val usage = GPUBufferUsage.CopyDst or GPUBufferUsage.Storage
        (GPUBufferUsage.CopyDst in usage).shouldBeTrue()
        (GPUBufferUsage.Storage in usage).shouldBeTrue()
        (GPUBufferUsage.None in usage).shouldBeTrue()
        (GPUBufferUsage.MapRead in usage).shouldBeFalse()
        (usage in usage).shouldBeTrue()
    }

    "fromBits preserves unknown bits" {
        val usage = GPUBufferUsage.CopyDst or GPUBufferUsage.Storage
        val raw = (1uL shl 63) or usage.value
        GPUBufferUsage.fromBits(raw).value shouldBe raw
    }

    "texture usage mask round-trips through fromBits" {
        val usage = GPUTextureUsage.TextureBinding or GPUTextureUsage.RenderAttachment
        val raw = (1uL shl 62) or usage.value
        val roundTripped = GPUTextureUsage.fromBits(raw)
        roundTripped.value shouldBe raw
        (GPUTextureUsage.TextureBinding in roundTripped).shouldBeTrue()
        (GPUTextureUsage.RenderAttachment in roundTripped).shouldBeTrue()
        (GPUTextureUsage.CopySrc in roundTripped).shouldBeFalse()
    }

    "or combines bits and None is the identity" {
        val combined = GPUBufferUsage.MapRead or GPUBufferUsage.CopySrc
        combined.value shouldBe (GPUBufferUsage.MapRead.value or GPUBufferUsage.CopySrc.value)
        (GPUBufferUsage.None or combined).value shouldBe combined.value
        GPUBufferUsage.None.value shouldBe 0uL
    }
})
