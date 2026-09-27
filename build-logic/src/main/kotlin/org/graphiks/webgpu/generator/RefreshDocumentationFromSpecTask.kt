package org.graphiks.webgpu.generator

import org.graphiks.webgpu.generator.files.SpecificationResources
import org.graphiks.webgpu.generator.lm.getDocumentationKeys
import org.graphiks.webgpu.generator.tasks.ModelGenerator
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.nio.file.Files

/** Rebuilds documentation entries from the normative prose in the checked-in WebGPU HTML. */
open class RefreshDocumentationFromSpecTask : DefaultTask() {
    init {
        group = "generator"
    }

    @TaskAction
    fun refresh() {
        val resources = SpecificationResources(project.projectDir.toPath())
        val context = ModelGenerator(resources).context
        val htmlPath = resources.findFilePath(SpecificationResources.Files.webgpuHtml)
            ?: error("Cannot find the HTML specification")
        val jsonPath = resources.specificationsSourcePath.resolve(SpecificationResources.Files.documentationJson)
        val document = Jsoup.parse(htmlPath.toFile(), "UTF-8")
        val keys = buildList {
            context.interfaces.forEach { addAll(it.getDocumentationKeys()) }
            context.commonEnumerations.forEach { addAll(it.getDocumentationKeys()) }
        }.distinct()

        val resolved = linkedMapOf<String, String>()
        val unresolved = mutableListOf<String>()
        keys.forEach { key ->
            val curated = curatedDescription(key)
            val preferCurated = key in setOf(
                "GPUCompilationInfo", "GPUCompilationMessageType", "GPUTextureViewDescriptor", "GPUAddressMode",
                "GPUFilterMode", "GPUMipmapFilterMode", "GPUSamplerDescriptor", "GPUBindGroupLayoutDescriptor",
                "GPUTextureFormat", "GPUTextureViewDimension", "GPUVertexFormat", "GPUVertexStepMode",
                "GPUVertexState", "GPUShaderModule"
            )
            val description = if (preferCurated) curated else findDescription(document, key) ?: curated
            if (description.isNullOrBlank()) unresolved += key else resolved[key] = addSpecificationReference(document, key, description)
        }

        logger.lifecycle("Resolved ${resolved.size} of ${keys.size} documentation entries from webgpu.html")
        if (unresolved.isNotEmpty()) {
            logger.lifecycle("Unresolved entries (left untouched):\n${unresolved.joinToString("\n")}")
            throw org.gradle.api.GradleException("Cannot rebuild documentation: ${unresolved.size} entries have no source description")
        }

        Files.writeString(jsonPath, Json { prettyPrint = true }.encodeToString(resolved))
        logger.lifecycle("Rebuilt ${resolved.size} documentation entries from webgpu.html")
    }

    private fun findDescription(document: org.jsoup.nodes.Document, key: String): String? {
        val ids = documentationIds(key)
        for (id in ids) {
            val definition = document.getElementById(id) ?: continue
            descriptionFromDefinition(definition, allowSectionContext = '#' !in key)?.let { return it }
        }
        return null
    }

    /** Adds a precise source link to brief entries so readers can follow the normative definition. */
    private fun addSpecificationReference(document: org.jsoup.nodes.Document, key: String, description: String): String {
        if (description.length > 50) return description

        val directAnchor = documentationIds(key).firstOrNull { document.getElementById(it) != null }
        val (owner, rawMember) = key.split('#', limit = 2).let { it[0] to it.getOrNull(1)?.trim() }
        val parentAnchor = if (rawMember != null) {
            documentationIds(owner).firstOrNull { document.getElementById(it) != null }
        } else null
        val anchor = directAnchor ?: parentAnchor ?: return description
        val targetName = if (directAnchor != null && rawMember != null) "$owner.$rawMember" else owner
        return "$description\n\nSee [$targetName in the WebGPU specification](https://www.w3.org/TR/webgpu/#$anchor)."
    }

    private fun documentationIds(key: String): List<String> {
        if ('#' !in key) {
            val name = key.lowercase()
            return listOf("dictdef-$name", "enumdef-$name", "typedefdef-$name", name)
        }

        val (owner, rawMember) = key.split('#', limit = 2)
        val member = rawMember.trim()
        val ownerId = owner.lowercase()
        if ('(' in member && member.endsWith(')')) {
            val name = member.substringBefore('(').lowercase()
            val args = member.substringAfter('(').dropLast(1).split(',').map { it.trim().lowercase() }
            return listOf("dom-$ownerId-$name-${args.joinToString("-")}", "dom-$ownerId-$name")
        }

        val kebabMember = member.replace(Regex("([a-z0-9])([A-Z])"), "$1-$2").lowercase()
        val ownerIds = when (owner) {
            "GPUColor" -> listOf("gpucolordict", ownerId)
            "GPUOrigin2D" -> listOf("gpuorigin2ddict", ownerId)
            "GPUOrigin3D" -> listOf("gpuorigin3ddict", ownerId)
            "GPUExtent3D" -> listOf("gpuextent3ddict", ownerId)
            "GPUSupportedLimits" -> listOf("supported-limits", ownerId)
            else -> listOf(ownerId)
        }
        return ownerIds.flatMap { id ->
            listOf("dom-$id-${member.lowercase()}", "dom-$id-$kebabMember")
        }.distinct()
    }

    private fun descriptionFromDefinition(definition: Element, allowSectionContext: Boolean): String? {
        val definitionListItem = definition.closest("dt")
        if (definitionListItem != null) {
            var description = definitionListItem.nextElementSibling()
            while (description?.normalName() == "dt") description = description.nextElementSibling()
            description = description?.takeIf { it.normalName() == "dd" } ?: return null
            val summary = description.select("p")
                .filterNot { it.parents().any { parent -> parent.hasClass("algorithm") || parent.hasClass("note") || parent.hasClass("example") || parent.hasClass("validusage") } }
                .map { it.text().trim().substringBefore(" For example:") }
                .filter { it.isNotEmpty() && !it.endsWith(":") }
                .take(2)
                .joinToString(" ")
            if (summary.isEmpty()) return null
            if (definition.attr("data-dfn-type") != "method") return summary

            val parameters = description.select("table.argumentdef tr")
                .drop(1)
                .mapNotNull { row ->
                    val cells = row.select("td")
                    if (cells.size < 5) return@mapNotNull null
                    val name = cells.first()?.text()?.trim()?.removePrefix("var ")?.trim()?.takeIf { it.isNotEmpty() }
                        ?: return@mapNotNull null
                    val detail = cells.last()?.text()?.trim()?.takeIf { it.isNotEmpty() }
                        ?: return@mapNotNull null
                    "@param $name $detail"
                }
            return if (parameters.isEmpty()) summary else "$summary\n\n${parameters.joinToString("\n")}"
        }

        val tableRow = definition.closest("tr")
        if (tableRow != null) {
            val continuation = tableRow.nextElementSibling()
                ?.takeIf { it.hasClass("row-continuation") }
                ?.selectFirst("td")
                ?.text()
                ?.trim()
            if (!continuation.isNullOrEmpty()) return continuation

            val cells = tableRow.select("td").map { it.text().trim() }.filter { it.isNotEmpty() }
            if (cells.size > 1) return "Uses the value ${cells.drop(1).joinToString(", ")}."
        }

        val featureHeading = definition.closest("h1, h2, h3, h4, h5, h6")
            ?.takeIf { it.attr("data-dfn-type") == "enum-value" }
        if (featureHeading != null) {
            var next = featureHeading.nextElementSibling()
            repeat(8) {
                if (next == null || next.normalName() in setOf("h1", "h2", "h3", "h4", "h5", "h6")) return@repeat
                if (next.normalName() == "p" && next.text().trim() != "When enabled:") {
                    val text = next.text().trim().substringBefore(" New WGSL extensions:")
                    if (text.isNotEmpty() && !text.endsWith(":")) return text
                } else if (next.normalName() in setOf("ul", "ol")) {
                    val listText = next.select("li").map { it.text().trim() }.filter { it.isNotEmpty() }.distinct().take(2)
                    if (listText.isNotEmpty()) return listText.joinToString(" ")
                }
                next = next?.nextElementSibling()
            }
        }

        if (!allowSectionContext) return null
        val block = definition.closest("pre") ?: definition.closest("h1, h2, h3, h4, h5, h6") ?: definition
        var sibling = block.nextElementSibling()
        repeat(12) {
            if (sibling == null || sibling.normalName() in setOf("h1", "h2", "h3", "h4", "h5", "h6")) return@repeat
            if (sibling.normalName() == "p") {
                val text = sibling.text().trim()
                val incomplete = text.endsWith(":") || text.startsWith("Note:") ||
                    text.contains("has the following members") || text.contains("has the following methods") ||
                    text.contains("has the following device timeline properties") || text.contains("has the following immutable properties")
                if (!sibling.hasClass("note") && text.isNotEmpty() && !incomplete) return text
            }
            sibling = sibling?.nextElementSibling()
        }
        return null
    }

    /** Short API-specific summaries for Kotlin adaptations and compact spec tables without prose. */
    private fun curatedDescription(key: String): String? {
        val (owner, member) = key.split('#', limit = 2).let { it[0] to it.getOrNull(1)?.trim() }
        if (member == null) {
            return when (owner) {
                "GPUBindingResource" -> "A resource accepted by a bind group entry, such as a sampler, texture view, buffer binding, or external texture."
                "GPUBufferBinding" -> "A buffer resource and the byte range exposed to a shader through a bind group."
                "GPUColor" -> "An RGBA color represented by four components."
                "GPUOrigin2D" -> "A two-dimensional origin represented by x and y coordinates."
                "GPUOrigin3D" -> "A three-dimensional origin represented by x, y, and z coordinates."
                "GPUExtent3D" -> "A three-dimensional extent represented by width, height, and depth or array-layer count."
                "GPUObjectBase" -> "Common properties included by WebGPU objects. Its label can identify the object in diagnostics and developer tools."
                "GPUDeviceLostInfo" -> "Information resolved by GPUDevice.lost when the device is lost, including the reason and diagnostic message."
                "GPUDeviceLostReason" -> "Indicates why a GPUDevice was lost."
                "GPUCommandEncoder" -> "Encodes render, compute, and copy commands into a command buffer."
                "GPUDebugCommandsMixin" -> "Adds debug marker and debug group commands to command encoders and pass encoders."
                "GPURenderBundle" -> "A reusable sequence of render commands created by a GPUDevice."
                "GPURenderBundleEncoder" -> "Encodes render commands into a GPURenderBundle."
                "GPURequestAdapterOptions" -> "Options that constrain which GPU adapter is selected."
                "GPUShaderModuleDescriptor" -> "Describes the shader code and compilation options used to create a GPUShaderModule."
                "GPUShaderModuleCompilationHint" -> "A hint associating a shader entry point with the pipeline layout expected to use it."
                "GPUColorTargetState" -> "Describes the format, write mask, and optional blending for a fragment color target."
                "GPUBlendState" -> "Describes the color and alpha blend components applied to a color target."
                "GPUVertexBufferLayout" -> "Describes the stride, step mode, and attributes of a vertex buffer."
                "GPUVertexAttribute" -> "Describes one vertex attribute's format and byte offset in a vertex buffer."
                "GPUCommandBufferDescriptor" -> "Options used when finishing a GPUCommandEncoder to create a GPUCommandBuffer."
                "GPUCommandEncoderDescriptor" -> "Options used to create a GPUCommandEncoder."
                "GPUComputePassDescriptor" -> "Options for beginning a compute pass, including optional timestamp writes."
                "GPURenderPassDescriptor" -> "Describes the color, depth/stencil, and optional timestamp attachments for a render pass."
                "GPURenderPassColorAttachment" -> "Describes a color attachment and its load, store, and resolve operations for a render pass."
                "GPUTextureOrGPUTextureView" -> "A value that is either a GPUTexture or a GPUTextureView."
                "GPURenderPassDepthStencilAttachment" -> "Describes the depth and stencil operations for a render pass attachment."
                "GPURenderPassLayout" -> "Describes the attachment formats and sample count used by a render pass or render bundle."
                "GPURenderBundleDescriptor" -> "Options used when finishing a GPURenderBundleEncoder to create a GPURenderBundle."
                "GPURenderBundleEncoderDescriptor" -> "Options used to create a GPURenderBundleEncoder."
                "GPUQuerySetDescriptor" -> "Describes the query type and number of queries in a GPUQuerySet."
                "GPUUncapturedErrorCallback" -> "Callback invoked when a GPUDevice reports an error that is not captured by an error scope."
                "GPUBufferMapState" -> "The mapping state of a GPUBuffer: mapped or unmapped."
                "GPUCompareFunction" -> "Selects the comparison operation used by depth and stencil tests."
                "GPULoadOp" -> "Selects whether a render pass clears an attachment or loads its existing contents."
                "GPUStoreOp" -> "Selects whether a render pass stores or discards an attachment's contents when the pass ends."
                "GPUQueryType" -> "Selects the kind of result recorded by a GPUQuerySet."
                "GPUTextureAspect" -> "Selects which aspect of a texture is addressed, such as color, depth, stencil, or all aspects."
                "GPUTextureDimension" -> "Selects the dimensionality of a GPUTexture: 1D, 2D, or 3D."
                "GPUTextureFormat" -> "Selects the texel representation and sampling or attachment capabilities of a GPUTexture."
                "GPUTextureViewDimension" -> "Selects the dimensionality and array form of a GPUTextureView."
                "GPUAddressMode" -> "Selects how texture coordinates outside the normalized range are addressed."
                "GPUFilterMode" -> "Selects the filtering behavior used for minification and magnification sampling."
                "GPUMipmapFilterMode" -> "Selects how samples are filtered between mip levels."
                "GPUVertexFormat" -> "Selects the scalar type, component count, and layout of a vertex attribute."
                "GPUVertexStepMode" -> "Selects whether a vertex buffer advances once per vertex or once per instance."
                "GPUVertexState" -> "Describes the vertex shader stage and vertex buffer layouts for a render pipeline."
                "GPUShaderModule" -> "Contains shader code compiled for use in GPU pipelines."
                "GPUSamplerDescriptor" -> "Describes the address modes, filtering modes, and level-of-detail limits for a GPUSampler."
                "GPUBindGroupLayoutDescriptor" -> "Describes the entries that define a GPUBindGroupLayout."
                "GPUTextureViewDescriptor" -> "Describes the format, dimension, mip levels, array layers, and aspect of a GPUTextureView."
                "GPUBufferBindingType" -> "Selects how a buffer binding is accessed by shaders."
                "GPUSamplerBindingType" -> "Selects the sampling operations supported by a sampler binding."
                "GPUStorageTextureAccess" -> "Selects how a storage texture binding may be accessed by shaders."
                "GPUTextureSampleType" -> "Selects the value type produced when sampling a texture binding."
                "GPUFeatureName" -> "Names optional WebGPU features that can be checked on an adapter and requested when creating a device."
                "GPUCompilationInfo" -> "Contains the messages produced while compiling a GPUShaderModule."
                "GPUPipelineBase" -> "Common base for GPU pipelines, including access to bind group layouts."
                "GPUCommandBuffer" -> "A recorded sequence of GPU commands ready for submission to a GPUQueue."
                "GPUComputePassEncoder" -> "Encodes compute dispatches and related commands in a compute pass."
                "GPUQueue" -> "Submits command buffers and schedules writes and copies for execution by the GPUDevice."
                "GPUQuerySet" -> "Stores the results of a configured kind and count of GPU queries."
                "GPUCompilationMessageType" -> "Classifies a GPUCompilationMessage as an error, warning, or informational message."
                "GPUObjectDescriptorBase" -> "Common label field shared by descriptors that create WebGPU objects."
                "GPUBufferDescriptor" -> "Describes a GPUBuffer's size, usage, and optional mapped-at-creation state."
                "GPUTextureDescriptor" -> "Describes a GPUTexture's size, format, usage, and optional view formats."
                "GPUBindGroupLayoutEntry" -> "Describes one binding's index, shader visibility, and resource-specific layout."
                "GPUBufferBindingLayout" -> "Describes the type, dynamic-offset behavior, and minimum size of a buffer binding."
                "GPUSamplerBindingLayout" -> "Describes the sampler binding type expected by a shader."
                "GPUTextureBindingLayout" -> "Describes the sample type, view dimension, and multisampling of a texture binding."
                "GPUStorageTextureBindingLayout" -> "Describes the access mode, format, and view dimension of a storage texture binding."
                "GPUBindGroupDescriptor" -> "Describes the layout and resources used to create a GPUBindGroup."
                "GPUBindGroupEntry" -> "Associates a binding index with the GPU resource supplied for that binding."
                "GPUPipelineLayoutDescriptor" -> "Lists the GPUBindGroupLayouts used by a pipeline."
                "GPUPipelineDescriptorBase" -> "Common label field shared by render and compute pipeline descriptors."
                "GPUComputePipelineDescriptor" -> "Describes the layout and compute shader stage used to create a GPUComputePipeline."
                "GPURenderPipelineDescriptor" -> "Describes the layout and programmable stages and fixed-function state of a GPURenderPipeline."
                "GPUPrimitiveState" -> "Describes primitive topology, strip format, front face, and culling for a render pipeline."
                "GPUMultisampleState" -> "Describes sample count, sample mask, and alpha-to-coverage behavior."
                "GPUFragmentState" -> "Describes the fragment shader stage and its color targets."
                "GPUBlendComponent" -> "Describes one color or alpha blend component's operation and factors."
                "GPUDepthStencilState" -> "Describes depth testing, stencil testing, and depth bias for a render pipeline."
                "GPUStencilFaceState" -> "Describes stencil comparison and the operations applied to a stencil face."
                "GPUComputePassTimestampWrites" -> "Selects query slots for compute-pass beginning and end timestamps."
                "GPURenderPassTimestampWrites" -> "Selects query slots for render-pass beginning, end, and optional per-stage timestamps."
                "GPUBlendFactor" -> "Defines the source or destination factor used to scale color and alpha values during blending."
                "GPUBlendOperation" -> "Defines the operation used to combine source and destination blend factors."
                "GPUFrontFace" -> "Selects whether clockwise or counter-clockwise polygons are treated as front-facing."
                "GPUPrimitiveTopology" -> "Selects how vertex data is assembled into points, lines, or triangles."
                "GPUErrorFilter" -> "Selects the error category captured by a GPUDevice error scope."
                "GPUPowerPreference" -> "Hints whether adapter selection should favor lower power use or higher performance."
                "GPUCullMode" -> "Selects whether no polygons, front-facing polygons, or back-facing polygons are culled."
                "GPUStencilOperation" -> "Selects the operation applied to stencil values when a stencil test succeeds or fails."
                else -> if (owner.endsWith("Descriptor")) {
                    "Describes the options used to create or configure ${owner.removeSuffix("Descriptor")}."
                } else null
            }
        }

        if (owner == "GPURenderPassEncoder" && member == "beginOcclusionQuery(queryIndex)") {
            return "Begins an occlusion query at queryIndex; endOcclusionQuery marks the end of the query."
        }
        if (owner == "GPURenderPassEncoder" && member == "endOcclusionQuery()") {
            return "Ends the occlusion query begun by beginOcclusionQuery."
        }
        if (owner == "GPUDeviceDescriptor" && member == "onUncapturedError") {
            return "Callback invoked for errors that are not captured by an error scope."
        }
        if (owner == "GPUDeviceLostInfo") return when (member) {
            "reason" -> "Reason reported for the device loss."
            "message" -> "Implementation-provided diagnostic message describing the device loss."
            else -> null
        }
        if (owner == "GPUCompilationInfo" && member == "messages") {
            return "Compilation messages, including errors, warnings, and informational messages."
        }
        if (owner == "GPUOrigin2D" && member in setOf("x", "y")) {
            return "The ${member.uppercase()} coordinate of the origin; defaults to 0 when the origin is specified as a dictionary."
        }
        if (owner == "GPUOrigin3D" && member in setOf("x", "y", "z")) {
            return "The ${member.uppercase()} coordinate of the origin; defaults to 0 when the origin is specified as a dictionary."
        }
        if (owner == "GPUSamplerDescriptor") {
            return when (member) {
                "addressModeU" -> "Addressing mode applied to texture coordinates along the U axis."
                "addressModeV" -> "Addressing mode applied to texture coordinates along the V axis."
                "lodMinClamp" -> "Lower bound for the level-of-detail value used during sampling."
                else -> null
            }
        }
        if (owner == "GPUBindGroupLayoutEntry") {
            val bindingType = when (member) {
                "buffer" -> "GPUBufferBindingLayout"
                "sampler" -> "GPUSamplerBindingLayout"
                "texture" -> "GPUTextureBindingLayout"
                "storageTexture" -> "GPUStorageTextureBindingLayout"
                "externalTexture" -> "GPUExternalTextureBindingLayout"
                else -> return null
            }
            return "Describes this binding as a $bindingType. Exactly one binding-type member may be set."
        }
        if (owner == "GPUShaderModuleCompilationHint" && member == "entryPoint") {
            return "Name of the shader entry point associated with this compilation hint."
        }
        if (owner == "GPUUncapturedErrorCallback" && member.startsWith("onUncapturedError")) {
            return "Receives an uncaptured GPU error."
        }
        if (owner == "GPUTextureFormat") return textureFormatDescription(member)
        if (owner == "GPUVertexFormat") return vertexFormatDescription(member)
        if (owner == "GPUFeatureName") {
            val token = member.replace(Regex("([a-z0-9])([A-Z])"), "$1-$2")
                .replace(Regex("(?<=\\D)(\\d)(?=[A-Z])"), "-$1")
                .replace(Regex("(?<=\\d)([A-Z])"), "-$1")
                .lowercase()
            return "The \"$token\" optional feature name, used to check adapter support and request the feature when creating a device."
        }
        if (owner == "GPULoadOp") return when (member.lowercase()) {
            "clear" -> "Clear the attachment to its clear value at the start of the render pass."
            "load" -> "Preserve and load the attachment's existing contents at the start of the render pass."
            else -> null
        }
        if (owner == "GPUStoreOp") return when (member.lowercase()) {
            "store" -> "Store the attachment's results when the render pass ends."
            "discard" -> "Discard the attachment's contents when the render pass ends."
            else -> null
        }
        if (owner == "GPUQueryType") return when (member.lowercase()) {
            "occlusion" -> "Records whether any samples pass the fragment tests for a render pass."
            "timestamp" -> "Records a GPU timestamp at a query location."
            else -> null
        }
        if (owner == "GPUTextureDimension") return when (member.lowercase()) {
            "oned" -> "A one-dimensional texture."
            "twod" -> "A two-dimensional texture."
            "threed" -> "A three-dimensional texture."
            else -> null
        }
        if (owner == "GPUTextureViewDimension") return when (member.lowercase()) {
            "oned" -> "A one-dimensional texture view."
            "twod" -> "A two-dimensional texture view."
            "twodarray" -> "A two-dimensional array texture view."
            "threed" -> "A three-dimensional texture view."
            else -> null
        }
        if (owner == "GPUDeviceLostReason") return when (member) {
            "Unknown" -> "The device was lost for a reason other than an explicit destroy request."
            "Destroyed" -> "The device was lost because GPUDevice.destroy() was called."
            "CallbackCancelled" -> "The device-loss callback was canceled."
            "FailedCreation" -> "Device creation failed."
            else -> "The $member reason for device loss."
        }
        if (owner == "GPUCompilationMessageType") return "Classifies a compilation message as an error, warning, or informational message."
        if (owner == "GPUCullMode") return when (member) {
            "None" -> "No polygons are discarded."
            "Front" -> "Front-facing polygons are discarded."
            "Back" -> "Back-facing polygons are discarded."
            else -> null
        }
        if (owner == "GPUMipmapFilterMode") return when (member) {
            "Nearest" -> "Selects the nearest mip level."
            "Linear" -> "Linearly interpolates between mip levels."
            else -> null
        }
        if (owner == "GPUStencilOperation") return when (member) {
            "Keep" -> "Keep the current stencil value."
            "Zero" -> "Replace the stencil value with zero."
            "Replace" -> "Replace the stencil value with the reference value."
            "Invert" -> "Bitwise-invert the stencil value."
            "IncrementClamp" -> "Increment the stencil value and clamp at the maximum representable value."
            "DecrementClamp" -> "Decrement the stencil value and clamp at zero."
            "IncrementWrap" -> "Increment the stencil value and wrap on overflow."
            "DecrementWrap" -> "Decrement the stencil value and wrap on underflow."
            else -> null
        }
        if (owner == "GPUBufferBindingType" && member == "BindingNotUsed") return "Declares that this binding is not used by the pipeline."
        if (owner == "GPUBufferBindingType") return when (member) {
            "Uniform" -> "The buffer is accessed as a uniform buffer."
            "Storage" -> "The buffer is accessed as a read-write storage buffer."
            "ReadOnlyStorage" -> "The buffer is accessed as a read-only storage buffer."
            else -> null
        }
        if (owner == "GPUSamplerBindingType") return when (member) {
            "Filtering" -> "The sampler supports filtering operations."
            "NonFiltering" -> "The sampler supports non-filtering operations."
            "Comparison" -> "The sampler performs comparison sampling."
            "BindingNotUsed" -> "Declares that this binding is not used by the pipeline."
            else -> null
        }
        if (owner == "GPUStorageTextureAccess") return when (member) {
            "WriteOnly" -> "Shaders may write to this storage texture."
            "ReadOnly" -> "Shaders may read from this storage texture."
            "ReadWrite" -> "Shaders may read from and write to this storage texture."
            "BindingNotUsed" -> "Declares that this binding is not used by the pipeline."
            else -> null
        }
        if (owner == "GPUTextureSampleType") return when (member) {
            "Float" -> "The texture produces floating-point sample values."
            "UnfilterableFloat" -> "The texture produces floating-point values that cannot be filtered."
            "Depth" -> "The texture produces depth sample values."
            "Sint" -> "The texture produces signed-integer sample values."
            "Uint" -> "The texture produces unsigned-integer sample values."
            "BindingNotUsed" -> "Declares that this binding is not used by the pipeline."
            else -> null
        }
        return null
    }

    private fun textureFormatDescription(name: String): String? {
        val format = name.replace(Regex("([a-z0-9])([A-Z])"), "$1-$2").lowercase()
        val srgb = format.endsWith("-srgb")
        val base = format.removeSuffix("-srgb")
        val channels = when {
            base.startsWith("rgba") -> "four-channel"
            base.startsWith("bgra") -> "four-channel"
            base.startsWith("rgb") -> "three-channel"
            base.startsWith("rg") -> "two-channel"
            base.startsWith("r") -> "single-channel"
            else -> null
        }
        if (base.startsWith("depth") || base.startsWith("stencil")) return "The $format depth or stencil texture format."
        val blockCompressed = base.startsWith("bc") || base.startsWith("etc2") || base.startsWith("eac") || base.startsWith("astc")
        if (blockCompressed) return "The $format block-compressed texture format${if (srgb) " with an sRGB transfer function" else ""}."
        val channelDescription = channels ?: return null
        val sampleKind = when {
            base.endsWith("unorm") -> "unsigned normalized-integer"
            base.endsWith("snorm") -> "signed normalized-integer"
            base.endsWith("uint") -> "unsigned-integer"
            base.endsWith("sint") -> "signed-integer"
            base.endsWith("float") -> "floating-point"
            else -> return null
        }
        val width = Regex("(8|16|32)-?(?=(unorm|snorm|uint|sint|float)$)").find(base)?.groupValues?.get(1)
        return "A $channelDescription $sampleKind texture format${width?.let { " with $it bits per component" } ?: ""}${if (srgb) " and an sRGB transfer function" else ""}."
    }

    private fun vertexFormatDescription(name: String): String? {
        if (name == "Unorm1010102") return "A four-component unsigned normalized vertex format with 10 bits for each RGB component and 2 bits for alpha."
        val value = name.replace(Regex("([a-z0-9])([A-Z])"), "$1-$2").lowercase()
        val lanes = Regex("x([234])$").find(value)?.groupValues?.get(1)?.toIntOrNull() ?: 1
        val kind = when {
            value.contains("unorm") -> "unsigned normalized-integer"
            value.contains("snorm") -> "signed normalized-integer"
            value.contains("uint") -> "unsigned-integer"
            value.contains("sint") -> "signed-integer"
            value.contains("float") -> "floating-point"
            else -> return null
        }
        val width = Regex("(8|16|32)").find(value)?.value ?: return null
        return "A $lanes-component $width-bit $kind vertex attribute format."
    }
}
