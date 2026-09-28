package org.graphiks.webgpu.suite

/**
 * The families of the public contract used by the inventory. An enum rather than free text keeps
 * the family of a case typed and stable.
 */
enum class AcidFamily(val id: String) {
    AdapterDeviceFeaturesLimits("adapter/device/features/limits"),
    QueueCommands("queue/commandes"),
    BuffersMapping("buffers/mapping"),
    TransfersBufferTexture("transferts buffers/textures"),
    BindGroupsLayouts("bind groups/layouts"),
    ShadersCompilation("shaders/compilation"),
    Compute("compute"),
    TexturesViewsSamplers("textures/views/samplers"),
    RenderPassesAttachments("rendu/passes/attachments"),
    PipelinesRenderState("pipelines/render state"),
    RenderBundles("render bundles"),
    QueriesTimestamps("queries/timestamps"),
    ErrorsAsync("erreurs/asynchronisme"),
    DataTypesDescriptorsFlagsSwizzle("types de données/descripteurs/flags/swizzle"),
}
