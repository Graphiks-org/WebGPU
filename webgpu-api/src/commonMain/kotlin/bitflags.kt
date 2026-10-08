@file:Suppress("unused")
// This file has been generated DO NO EDIT
package org.graphiks.webgpu

@kotlin.jvm.JvmInline
public value class GPUBufferUsage private constructor(
  public val `value`: kotlin.ULong,
) {
  public operator fun contains(other: org.graphiks.webgpu.GPUBufferUsage): kotlin.Boolean = (value and other.value) == other.value

  public infix fun or(other: org.graphiks.webgpu.GPUBufferUsage): org.graphiks.webgpu.GPUBufferUsage = GPUBufferUsage(value or other.value)

  public infix fun of(values: kotlin.Array<org.graphiks.webgpu.GPUBufferUsage>): org.graphiks.webgpu.GPUBufferUsage = values.fold(GPUBufferUsage.None) { acc, enumeration -> acc or enumeration }

  public companion object {
    public val None: org.graphiks.webgpu.GPUBufferUsage = GPUBufferUsage(0uL)

    public val MapRead: org.graphiks.webgpu.GPUBufferUsage = GPUBufferUsage(1uL)

    public val MapWrite: org.graphiks.webgpu.GPUBufferUsage = GPUBufferUsage(2uL)

    public val CopySrc: org.graphiks.webgpu.GPUBufferUsage = GPUBufferUsage(4uL)

    public val CopyDst: org.graphiks.webgpu.GPUBufferUsage = GPUBufferUsage(8uL)

    public val Index: org.graphiks.webgpu.GPUBufferUsage = GPUBufferUsage(16uL)

    public val Vertex: org.graphiks.webgpu.GPUBufferUsage = GPUBufferUsage(32uL)

    public val Uniform: org.graphiks.webgpu.GPUBufferUsage = GPUBufferUsage(64uL)

    public val Storage: org.graphiks.webgpu.GPUBufferUsage = GPUBufferUsage(128uL)

    public val Indirect: org.graphiks.webgpu.GPUBufferUsage = GPUBufferUsage(256uL)

    public val QueryResolve: org.graphiks.webgpu.GPUBufferUsage = GPUBufferUsage(512uL)

    public val entries: kotlin.collections.Set<org.graphiks.webgpu.GPUBufferUsage> = setOf(None, MapRead, MapWrite, CopySrc, CopyDst, Index, Vertex, Uniform, Storage, Indirect, QueryResolve)

    public fun fromBits(`value`: kotlin.ULong): org.graphiks.webgpu.GPUBufferUsage = GPUBufferUsage(value)
  }
}

@kotlin.jvm.JvmInline
public value class GPUColorWrite private constructor(
  public val `value`: kotlin.ULong,
) {
  public operator fun contains(other: org.graphiks.webgpu.GPUColorWrite): kotlin.Boolean = (value and other.value) == other.value

  public infix fun or(other: org.graphiks.webgpu.GPUColorWrite): org.graphiks.webgpu.GPUColorWrite = GPUColorWrite(value or other.value)

  public infix fun of(values: kotlin.Array<org.graphiks.webgpu.GPUColorWrite>): org.graphiks.webgpu.GPUColorWrite = values.fold(GPUColorWrite.None) { acc, enumeration -> acc or enumeration }

  public companion object {
    public val None: org.graphiks.webgpu.GPUColorWrite = GPUColorWrite(0uL)

    public val Red: org.graphiks.webgpu.GPUColorWrite = GPUColorWrite(1uL)

    public val Green: org.graphiks.webgpu.GPUColorWrite = GPUColorWrite(2uL)

    public val Blue: org.graphiks.webgpu.GPUColorWrite = GPUColorWrite(4uL)

    public val Alpha: org.graphiks.webgpu.GPUColorWrite = GPUColorWrite(8uL)

    public val All: org.graphiks.webgpu.GPUColorWrite = GPUColorWrite(15uL)

    public val entries: kotlin.collections.Set<org.graphiks.webgpu.GPUColorWrite> = setOf(None, Red, Green, Blue, Alpha, All)

    public fun fromBits(`value`: kotlin.ULong): org.graphiks.webgpu.GPUColorWrite = GPUColorWrite(value)
  }
}

@kotlin.jvm.JvmInline
public value class GPUMapMode private constructor(
  public val `value`: kotlin.ULong,
) {
  public operator fun contains(other: org.graphiks.webgpu.GPUMapMode): kotlin.Boolean = (value and other.value) == other.value

  public infix fun or(other: org.graphiks.webgpu.GPUMapMode): org.graphiks.webgpu.GPUMapMode = GPUMapMode(value or other.value)

  public infix fun of(values: kotlin.Array<org.graphiks.webgpu.GPUMapMode>): org.graphiks.webgpu.GPUMapMode = values.fold(GPUMapMode.None) { acc, enumeration -> acc or enumeration }

  public companion object {
    public val None: org.graphiks.webgpu.GPUMapMode = GPUMapMode(0uL)

    public val Read: org.graphiks.webgpu.GPUMapMode = GPUMapMode(1uL)

    public val Write: org.graphiks.webgpu.GPUMapMode = GPUMapMode(2uL)

    public val entries: kotlin.collections.Set<org.graphiks.webgpu.GPUMapMode> = setOf(None, Read, Write)

    public fun fromBits(`value`: kotlin.ULong): org.graphiks.webgpu.GPUMapMode = GPUMapMode(value)
  }
}

@kotlin.jvm.JvmInline
public value class GPUShaderStage private constructor(
  public val `value`: kotlin.ULong,
) {
  public operator fun contains(other: org.graphiks.webgpu.GPUShaderStage): kotlin.Boolean = (value and other.value) == other.value

  public infix fun or(other: org.graphiks.webgpu.GPUShaderStage): org.graphiks.webgpu.GPUShaderStage = GPUShaderStage(value or other.value)

  public infix fun of(values: kotlin.Array<org.graphiks.webgpu.GPUShaderStage>): org.graphiks.webgpu.GPUShaderStage = values.fold(GPUShaderStage.None) { acc, enumeration -> acc or enumeration }

  public companion object {
    public val None: org.graphiks.webgpu.GPUShaderStage = GPUShaderStage(0uL)

    public val Vertex: org.graphiks.webgpu.GPUShaderStage = GPUShaderStage(1uL)

    public val Fragment: org.graphiks.webgpu.GPUShaderStage = GPUShaderStage(2uL)

    public val Compute: org.graphiks.webgpu.GPUShaderStage = GPUShaderStage(4uL)

    public val entries: kotlin.collections.Set<org.graphiks.webgpu.GPUShaderStage> = setOf(None, Vertex, Fragment, Compute)

    public fun fromBits(`value`: kotlin.ULong): org.graphiks.webgpu.GPUShaderStage = GPUShaderStage(value)
  }
}

@kotlin.jvm.JvmInline
public value class GPUTextureUsage private constructor(
  public val `value`: kotlin.ULong,
) {
  public operator fun contains(other: org.graphiks.webgpu.GPUTextureUsage): kotlin.Boolean = (value and other.value) == other.value

  public infix fun or(other: org.graphiks.webgpu.GPUTextureUsage): org.graphiks.webgpu.GPUTextureUsage = GPUTextureUsage(value or other.value)

  public infix fun of(values: kotlin.Array<org.graphiks.webgpu.GPUTextureUsage>): org.graphiks.webgpu.GPUTextureUsage = values.fold(GPUTextureUsage.None) { acc, enumeration -> acc or enumeration }

  public companion object {
    public val None: org.graphiks.webgpu.GPUTextureUsage = GPUTextureUsage(0uL)

    public val CopySrc: org.graphiks.webgpu.GPUTextureUsage = GPUTextureUsage(1uL)

    public val CopyDst: org.graphiks.webgpu.GPUTextureUsage = GPUTextureUsage(2uL)

    public val TextureBinding: org.graphiks.webgpu.GPUTextureUsage = GPUTextureUsage(4uL)

    public val StorageBinding: org.graphiks.webgpu.GPUTextureUsage = GPUTextureUsage(8uL)

    public val RenderAttachment: org.graphiks.webgpu.GPUTextureUsage = GPUTextureUsage(16uL)

    public val TransientAttachment: org.graphiks.webgpu.GPUTextureUsage = GPUTextureUsage(32uL)

    public val entries: kotlin.collections.Set<org.graphiks.webgpu.GPUTextureUsage> = setOf(None, CopySrc, CopyDst, TextureBinding, StorageBinding, RenderAttachment, TransientAttachment)

    public fun fromBits(`value`: kotlin.ULong): org.graphiks.webgpu.GPUTextureUsage = GPUTextureUsage(value)
  }
}
