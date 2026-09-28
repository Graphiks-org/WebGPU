package org.graphiks.webgpu.suite

/**
 * The families of the public contract used by the inventory. An enum rather than free text keeps
 * the family of a case typed and stable. [packageName] is the source package the family's cases
 * live in; the generator rejects a case whose package does not match its family.
 */
enum class AcidFamily(val id: String, val packageName: String) {
    AdapterDeviceFeaturesLimits("adapter/device/features/limits", "adapter"),
    QueueCommands("queue/commandes", "queue"),
    BuffersMapping("buffers/mapping", "buffers"),
    TransfersBufferTexture("transferts buffers/textures", "transfers"),
    BindGroupsLayouts("bind groups/layouts", "bindgroups"),
    ShadersCompilation("shaders/compilation", "shaders"),
    Compute("compute", "compute"),
    TexturesViewsSamplers("textures/views/samplers", "textures"),
    RenderPassesAttachments("rendu/passes/attachments", "renderpasses"),
    PipelinesRenderState("pipelines/render state", "pipelines"),
    RenderBundles("render bundles", "renderbundles"),
    QueriesTimestamps("queries/timestamps", "queries"),
    ErrorsAsync("erreurs/asynchronisme", "errors"),
    DataTypesDescriptorsFlagsSwizzle("types de données/descripteurs/flags/swizzle", "datatypes"),
}
