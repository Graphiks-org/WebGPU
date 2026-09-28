package org.graphiks.webgpu.suite

/** The stable identity of every foundation case. The enum is the expected catalogue. */
enum class AcidCaseId(val id: String) {
    BuffersMappedAtCreation("buffers.mapped-at-creation"),
    TransfersCopyOffsets("transfers.copy-offsets"),
    TransfersWriteOffsets("transfers.write-offsets"),
    TransfersWriteRemaining("transfers.write-remaining"),
    BuffersPartialMapRemap("buffers.partial-map-remap"),
    ComputeAutoLayoutConstants("compute.auto-layout-constants"),
    ComputeExplicitLayoutEntrypoint("compute.explicit-layout-entrypoint"),
    ErrorsEmptyScope("errors.empty-scope"),
    ErrorsInvalidBufferUsage("errors.invalid-buffer-usage"),
    ErrorsMapAlignment("errors.map-alignment"),
    BuffersMapDestroyed("buffers.map-destroyed"),
}
