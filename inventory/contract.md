# Inventaire du contrat public Graphiks WebGPU

Cet inventaire couvre le contrat public commun de la version de référence. La liste
exhaustive des déclarations est dans [`symbols.tsv`](symbols.tsv) ; les comportements
observables et leur couverture sont dans [`behaviors.json`](behaviors.json).

Version de référence : `0.1.0-SNAPSHOT` (API et suite). Commit et empreintes des sources :
[`baseline.json`](baseline.json). Les signatures ont été relues sur ces sources ;
`symbols.tsv` est extrait des sept fichiers `commonMain` de `webgpu-api` et non du seul
snapshot JVM.

## Comment lire ce document

- Un **comportement** est une attente observable, associée à des symboles du contrat et,
  le cas échéant, aux acid tests qui l'exercent. `caseIds` vide signifie **à tester**, jamais
  « réussi ».
- Un **type de support** est une déclaration qui structure le contrat (interface de
  descripteur, enum, alias, flag) sans porter à elle seule un comportement vérifiable.
  Elle est listée par famille pour expliciter son rôle ; cela ne la rend pas testée.
- Les familles dont l'analyse comportementale doit être approfondie sont signalées en fin
  de document. Aucun pourcentage de conformité n'est déduit du nombre de symboles.

Total : **834 déclarations** recensées dans 14 familles ; **54 comportements** décrits ; **11 cas exécutables**.

## adapter/device/features/limits

86 déclarations, 3 comportements décrits.

### Comportements

- **`adapter.request-and-capabilities`** — Demander un adapter expose ses features, ses limites et sa description ; la demande peut échouer explicitement.
  - Cas : **à tester**
- **`device.features-and-limits`** — Un device expose les features et limites négociées, ainsi que la description de l'adapter qui l'a créé.
  - Cas : **à tester**
- **`device.request-required-features`** — Une feature ou limite obligatoire absente fait échouer la demande de device au lieu de produire un device dégradé.
  - Cas : **à tester**

### Types de support

`GPUFeatureName.CoreFeaturesAndLimits`, `GPUFeatureName.DepthClipControl`, `GPUFeatureName.Depth32FloatStencil8`, `GPUFeatureName.TextureCompressionBC`, `GPUFeatureName.TextureCompressionBCSliced3D`, `GPUFeatureName.TextureCompressionETC2`, `GPUFeatureName.TextureCompressionASTC`, `GPUFeatureName.TextureCompressionASTCSliced3D`, `GPUFeatureName.TimestampQuery`, `GPUFeatureName.IndirectFirstInstance`, `GPUFeatureName.ShaderF16`, `GPUFeatureName.RG11B10UfloatRenderable`, `GPUFeatureName.BGRA8UnormStorage`, `GPUFeatureName.Float32Filterable`, `GPUFeatureName.Float32Blendable`, `GPUFeatureName.ClipDistances`, `GPUFeatureName.DualSourceBlending`, `GPUFeatureName.Subgroups`, `GPUFeatureName.TextureFormatsTier1`, `GPUFeatureName.TextureFormatsTier2`, `GPUFeatureName.PrimitiveIndex`, `GPUFeatureName.TextureComponentSwizzle`, `GPUPowerPreference.LowPower`, `GPUPowerPreference.HighPerformance`, `GPUSupportedLimits`, `GPUSupportedLimits.maxTextureDimension1D`, `GPUSupportedLimits.maxTextureDimension2D`, `GPUSupportedLimits.maxTextureDimension3D`, `GPUSupportedLimits.maxTextureArrayLayers`, `GPUSupportedLimits.maxBindGroups`, `GPUSupportedLimits.maxBindGroupsPlusVertexBuffers`, `GPUSupportedLimits.maxImmediateSize`, `GPUSupportedLimits.maxBindingsPerBindGroup`, `GPUSupportedLimits.maxDynamicUniformBuffersPerPipelineLayout`, `GPUSupportedLimits.maxDynamicStorageBuffersPerPipelineLayout`, `GPUSupportedLimits.maxSampledTexturesPerShaderStage`, `GPUSupportedLimits.maxSamplersPerShaderStage`, `GPUSupportedLimits.maxStorageBuffersPerShaderStage`, `GPUSupportedLimits.maxStorageBuffersInVertexStage`, `GPUSupportedLimits.maxStorageBuffersInFragmentStage`, `GPUSupportedLimits.maxStorageTexturesPerShaderStage`, `GPUSupportedLimits.maxStorageTexturesInVertexStage`, `GPUSupportedLimits.maxStorageTexturesInFragmentStage`, `GPUSupportedLimits.maxUniformBuffersPerShaderStage`, `GPUSupportedLimits.maxUniformBufferBindingSize`, `GPUSupportedLimits.maxStorageBufferBindingSize`, `GPUSupportedLimits.minUniformBufferOffsetAlignment`, `GPUSupportedLimits.minStorageBufferOffsetAlignment`, `GPUSupportedLimits.maxVertexBuffers`, `GPUSupportedLimits.maxBufferSize`, `GPUSupportedLimits.maxVertexAttributes`, `GPUSupportedLimits.maxVertexBufferArrayStride`, `GPUSupportedLimits.maxInterStageShaderVariables`, `GPUSupportedLimits.maxColorAttachments`, `GPUSupportedLimits.maxColorAttachmentBytesPerSample`, `GPUSupportedLimits.maxComputeWorkgroupStorageSize`, `GPUSupportedLimits.maxComputeInvocationsPerWorkgroup`, `GPUSupportedLimits.maxComputeWorkgroupSizeX`, `GPUSupportedLimits.maxComputeWorkgroupSizeY`, `GPUSupportedLimits.maxComputeWorkgroupSizeZ`, `GPUSupportedLimits.maxComputeWorkgroupsPerDimension`, `GPUAdapterInfo.vendor`, `GPUAdapterInfo.architecture`, `GPUAdapterInfo.device`, `GPUAdapterInfo.description`, `GPUAdapterInfo.subgroupMinSize`, `GPUAdapterInfo.subgroupMaxSize`, `GPUAdapterInfo.isFallbackAdapter`, `GPURequestAdapterOptions.featureLevel`, `GPURequestAdapterOptions.powerPreference`, `GPURequestAdapterOptions.forceFallbackAdapter`, `GPURequestAdapterOptions.xrCompatible`

## queue/commandes

11 déclarations, 3 comportements décrits.

### Comportements

- **`queue.submit-and-completion`** — Le travail soumis est observé une fois terminé, sans confondre soumission CPU et achèvement du travail GPU.
  - Cas : `transfers.copy-offsets`, `transfers.write-offsets`, `transfers.write-remaining`, `buffers.partial-map-remap`
- **`command.encoder-recording`** — Un encodeur enregistre des commandes puis produit un command buffer soumis une seule fois.
  - Cas : **à tester**
- **`command.debug-markers`** — Les marqueurs de debug encadrent des commandes sans modifier les résultats observables.
  - Cas : **à tester**

### Types de support

`GPUDevice.queue`, `GPUQueue`

## buffers/mapping

33 déclarations, 7 comportements décrits.

### Comportements

- **`buffers.creation-and-usage`** — Un buffer expose la taille et l'ensemble d'usages demandés.
  - Cas : `buffers.mapped-at-creation`
- **`buffers.usage-flags`** — Les flags d'usage se combinent et se relisent ; un usage vide est refusé.
  - Cas : `buffers.mapped-at-creation`, `errors.invalid-buffer-usage`
- **`buffers.mapping.initial-data`** — Un buffer créé mappé expose immédiatement sa plage complète et devient non mappé après unmap.
  - Cas : `buffers.mapped-at-creation`
- **`buffers.mapping.state-transitions`** — mapAsync fait passer le buffer de Unmapped à Pending puis Mapped, et unmap le ramène à Unmapped.
  - Cas : `buffers.mapped-at-creation`, `buffers.partial-map-remap`
- **`buffers.mapping.partial-range`** — Lire une plage alignée puis remapper le buffer entier sans conserver la vue invalidée.
  - Cas : `buffers.partial-map-remap`
- **`buffers.mapping.alignment-validation`** — Un offset de mapping non aligné sur 8 octets échoue la validation et laisse le buffer non mappé.
  - Cas : `errors.map-alignment`
- **`buffers.destroy-and-map`** — Mapper un buffer détruit échoue et ne peut pas être rapporté comme un succès.
  - Cas : `buffers.map-destroyed`

### Types de support

`GPUBufferUsage.value`, `GPUBufferMapState.Unmapped`, `GPUBufferMapState.Pending`, `GPUBufferMapState.Mapped`

## transferts buffers/textures

18 déclarations, 4 comportements décrits.

### Comportements

- **`transfers.buffer-copy-offsets`** — Une copie entre buffers respecte les offsets et tailles exprimés en octets et laisse intact le reste de la destination.
  - Cas : `transfers.copy-offsets`
- **`transfers.queue-write-offsets`** — writeBuffer écrit depuis un offset de données, avec taille explicite ou jusqu'à la fin des données.
  - Cas : `transfers.write-offsets`, `transfers.write-remaining`
- **`transfers.buffer-texture-copies`** — Les copies buffer/texture respectent le layout de texels, les offsets et les sous-ressources.
  - Cas : **à tester**
- **`transfers.clear-buffer`** — clearBuffer remet à zéro la sous-plage demandée du buffer.
  - Cas : **à tester**

### Types de support

`GPUTexelCopyBufferInfo.buffer`, `GPUTexelCopyTextureInfo.texture`

## bind groups/layouts

32 déclarations, 4 comportements décrits.

### Comportements

- **`bindgroup.auto-layout`** — Un pipeline à layout automatique expose un layout compatible que createBindGroup peut utiliser.
  - Cas : `compute.auto-layout-constants`
- **`bindgroup.explicit-layout`** — Un layout explicite déclare binding, visibilité et type de ressource, et le bind group s'y conforme.
  - Cas : `compute.explicit-layout-entrypoint`
- **`bindgroup.entries-and-resources`** — Chaque entrée relie un index de binding à une ressource du type attendu par le layout.
  - Cas : `compute.auto-layout-constants`, `compute.explicit-layout-entrypoint`
- **`pipelinelayout.bind-group-sequence`** — L'ordre des bind group layouts correspond aux @group du shader.
  - Cas : `compute.explicit-layout-entrypoint`

### Types de support

`GPUBindGroupLayoutDescriptor.entries`, `GPUBindGroupLayoutEntry.binding`, `GPUBindGroupLayoutEntry.visibility`, `GPUBindGroupLayoutEntry.buffer`, `GPUBindGroupLayoutEntry.sampler`, `GPUBindGroupLayoutEntry.texture`, `GPUBindGroupLayoutEntry.storageTexture`, `GPUBufferBindingLayout.type`, `GPUBufferBindingLayout.hasDynamicOffset`, `GPUBufferBindingLayout.minBindingSize`, `GPUBindGroupDescriptor.layout`, `GPUBindGroupDescriptor.entries`

## shaders/compilation

35 déclarations, 3 comportements décrits.

### Comportements

- **`shader.module-creation`** — Un shader valide est créé et un shader invalide fait échouer la validation.
  - Cas : `compute.auto-layout-constants`, `compute.explicit-layout-entrypoint`
- **`shader.compilation-info`** — Les messages de compilation sont disponibles avec leur sévérité et leur position, sans imposer un texte exact.
  - Cas : **à tester**
- **`shader.specialization-constants`** — Une constante de spécialisation override la valeur du shader et change le résultat du dispatch.
  - Cas : `compute.auto-layout-constants`

### Types de support

`GPUShaderStage.value`, `GPUShaderStage.or`, `GPUShaderStage.of`, `GPUShaderStage.None`, `GPUShaderStage.Vertex`, `GPUShaderStage.Fragment`, `GPUShaderStage.Compute`, `GPUShaderStage.entries`, `GPUCompilationMessageType.Error`, `GPUCompilationMessageType.Warning`, `GPUCompilationMessageType.Info`, `GPUCompilationInfo.messages`, `GPUShaderModuleDescriptor.compilationHints`, `GPUShaderModuleCompilationHint.entryPoint`, `GPUShaderModuleCompilationHint.layout`

## compute

17 déclarations, 5 comportements décrits.

### Comportements

- **`compute.pipeline-auto-layout`** — Un pipeline compute en layout automatique compile et peut être lié dans une passe.
  - Cas : `compute.auto-layout-constants`
- **`compute.pipeline-explicit-entrypoint`** — Un pipeline compute accepte un layout explicite et un entry point inféré lorsque le shader n'en a qu'un.
  - Cas : `compute.explicit-layout-entrypoint`
- **`compute.pass-dispatch`** — Une passe compute dispatche des workgroups et les écritures du shader sont lisibles ensuite.
  - Cas : `compute.auto-layout-constants`, `compute.explicit-layout-entrypoint`
- **`compute.pipeline-async`** — La création asynchrone résout un pipeline prêt à l'emploi ou échoue explicitement.
  - Cas : **à tester**
- **`compute.pass-descriptor`** — Le descripteur de passe compute porte les écritures de timestamp optionnelles.
  - Cas : **à tester**

### Types de support

`GPUComputePassTimestampWrites.querySet`, `GPUComputePassTimestampWrites.beginningOfPassWriteIndex`, `GPUComputePassTimestampWrites.endOfPassWriteIndex`

## textures/views/samplers

220 déclarations, 5 comportements décrits.

### Comportements

- **`texture.creation`** — Une texture expose ses dimensions, son format et ses usages ; une combinaison invalide échoue la validation.
  - Cas : **à tester**
- **`texture.view`** — Une vue sélectionne une sous-ressource compatible avec le format et l'usage demandés.
  - Cas : **à tester**
- **`texture.view-swizzle`** — Un swizzle non identitaire produit la chaîne DOMString attendue et exige la feature texture-component-swizzle.
  - Features optionnelles : `TextureComponentSwizzle`
  - Cas : **à tester**
- **`sampler.creation`** — Un sampler expose ses modes d'adressage et de filtrage et un sampler de comparaison l'indique.
  - Cas : **à tester**
- **`texture.usage-and-formats`** — Les usages de texture se combinent et chaque format déclare ses capacités de binding et d'attachement.
  - Cas : **à tester**

### Types de support

`GPUTextureSwizzleSource.Red`, `GPUTextureSwizzleSource.Green`, `GPUTextureSwizzleSource.Blue`, `GPUTextureSwizzleSource.Alpha`, `GPUTextureSwizzleSource.Zero`, `GPUTextureSwizzleSource.One`, `GPUTextureUsage.value`, `GPUAddressMode.ClampToEdge`, `GPUAddressMode.Repeat`, `GPUAddressMode.MirrorRepeat`, `GPUFilterMode.Nearest`, `GPUFilterMode.Linear`, `GPUMipmapFilterMode.Nearest`, `GPUMipmapFilterMode.Linear`, `GPUSamplerBindingType.BindingNotUsed`, `GPUSamplerBindingType.Filtering`, `GPUSamplerBindingType.NonFiltering`, `GPUSamplerBindingType.Comparison`, `GPUStorageTextureAccess.BindingNotUsed`, `GPUStorageTextureAccess.WriteOnly`, `GPUStorageTextureAccess.ReadOnly`, `GPUStorageTextureAccess.ReadWrite`, `GPUTextureAspect.All`, `GPUTextureAspect.StencilOnly`, `GPUTextureAspect.DepthOnly`, `GPUTextureDimension.OneD`, `GPUTextureDimension.TwoD`, `GPUTextureDimension.ThreeD`, `GPUTextureFormat.R8Unorm`, `GPUTextureFormat.R8Snorm`, `GPUTextureFormat.R8Uint`, `GPUTextureFormat.R8Sint`, `GPUTextureFormat.R16Unorm`, `GPUTextureFormat.R16Snorm`, `GPUTextureFormat.R16Uint`, `GPUTextureFormat.R16Sint`, `GPUTextureFormat.R16Float`, `GPUTextureFormat.RG8Unorm`, `GPUTextureFormat.RG8Snorm`, `GPUTextureFormat.RG8Uint`, `GPUTextureFormat.RG8Sint`, `GPUTextureFormat.R32Float`, `GPUTextureFormat.R32Uint`, `GPUTextureFormat.R32Sint`, `GPUTextureFormat.RG16Unorm`, `GPUTextureFormat.RG16Snorm`, `GPUTextureFormat.RG16Uint`, `GPUTextureFormat.RG16Sint`, `GPUTextureFormat.RG16Float`, `GPUTextureFormat.RGBA8Unorm`, `GPUTextureFormat.RGBA8UnormSrgb`, `GPUTextureFormat.RGBA8Snorm`, `GPUTextureFormat.RGBA8Uint`, `GPUTextureFormat.RGBA8Sint`, `GPUTextureFormat.BGRA8Unorm`, `GPUTextureFormat.BGRA8UnormSrgb`, `GPUTextureFormat.RGB10A2Uint`, `GPUTextureFormat.RGB10A2Unorm`, `GPUTextureFormat.RG11B10Ufloat`, `GPUTextureFormat.RGB9E5Ufloat`, `GPUTextureFormat.RG32Float`, `GPUTextureFormat.RG32Uint`, `GPUTextureFormat.RG32Sint`, `GPUTextureFormat.RGBA16Unorm`, `GPUTextureFormat.RGBA16Snorm`, `GPUTextureFormat.RGBA16Uint`, `GPUTextureFormat.RGBA16Sint`, `GPUTextureFormat.RGBA16Float`, `GPUTextureFormat.RGBA32Float`, `GPUTextureFormat.RGBA32Uint`, `GPUTextureFormat.RGBA32Sint`, `GPUTextureFormat.Stencil8`, `GPUTextureFormat.Depth16Unorm`, `GPUTextureFormat.Depth24Plus`, `GPUTextureFormat.Depth24PlusStencil8`, `GPUTextureFormat.Depth32Float`, `GPUTextureFormat.Depth32FloatStencil8`, `GPUTextureFormat.BC1RGBAUnorm`, `GPUTextureFormat.BC1RGBAUnormSrgb`, `GPUTextureFormat.BC2RGBAUnorm`, `GPUTextureFormat.BC2RGBAUnormSrgb`, `GPUTextureFormat.BC3RGBAUnorm`, `GPUTextureFormat.BC3RGBAUnormSrgb`, `GPUTextureFormat.BC4RUnorm`, `GPUTextureFormat.BC4RSnorm`, `GPUTextureFormat.BC5RGUnorm`, `GPUTextureFormat.BC5RGSnorm`, `GPUTextureFormat.BC6HRGBUfloat`, `GPUTextureFormat.BC6HRGBFloat`, `GPUTextureFormat.BC7RGBAUnorm`, `GPUTextureFormat.BC7RGBAUnormSrgb`, `GPUTextureFormat.ETC2RGB8Unorm`, `GPUTextureFormat.ETC2RGB8UnormSrgb`, `GPUTextureFormat.ETC2RGB8A1Unorm`, `GPUTextureFormat.ETC2RGB8A1UnormSrgb`, `GPUTextureFormat.ETC2RGBA8Unorm`, `GPUTextureFormat.ETC2RGBA8UnormSrgb`, `GPUTextureFormat.EACR11Unorm`, `GPUTextureFormat.EACR11Snorm`, `GPUTextureFormat.EACRG11Unorm`, `GPUTextureFormat.EACRG11Snorm`, `GPUTextureFormat.ASTC4x4Unorm`, `GPUTextureFormat.ASTC4x4UnormSrgb`, `GPUTextureFormat.ASTC5x4Unorm`, `GPUTextureFormat.ASTC5x4UnormSrgb`, `GPUTextureFormat.ASTC5x5Unorm`, `GPUTextureFormat.ASTC5x5UnormSrgb`, `GPUTextureFormat.ASTC6x5Unorm`, `GPUTextureFormat.ASTC6x5UnormSrgb`, `GPUTextureFormat.ASTC6x6Unorm`, `GPUTextureFormat.ASTC6x6UnormSrgb`, `GPUTextureFormat.ASTC8x5Unorm`, `GPUTextureFormat.ASTC8x5UnormSrgb`, `GPUTextureFormat.ASTC8x6Unorm`, `GPUTextureFormat.ASTC8x6UnormSrgb`, `GPUTextureFormat.ASTC8x8Unorm`, `GPUTextureFormat.ASTC8x8UnormSrgb`, `GPUTextureFormat.ASTC10x5Unorm`, `GPUTextureFormat.ASTC10x5UnormSrgb`, `GPUTextureFormat.ASTC10x6Unorm`, `GPUTextureFormat.ASTC10x6UnormSrgb`, `GPUTextureFormat.ASTC10x8Unorm`, `GPUTextureFormat.ASTC10x8UnormSrgb`, `GPUTextureFormat.ASTC10x10Unorm`, `GPUTextureFormat.ASTC10x10UnormSrgb`, `GPUTextureFormat.ASTC12x10Unorm`, `GPUTextureFormat.ASTC12x10UnormSrgb`, `GPUTextureFormat.ASTC12x12Unorm`, `GPUTextureFormat.ASTC12x12UnormSrgb`, `GPUTextureSampleType.BindingNotUsed`, `GPUTextureSampleType.Float`, `GPUTextureSampleType.UnfilterableFloat`, `GPUTextureSampleType.Depth`, `GPUTextureSampleType.Sint`, `GPUTextureSampleType.Uint`, `GPUTextureViewDimension.OneD`, `GPUTextureViewDimension.TwoD`, `GPUTextureViewDimension.TwoDArray`, `GPUTextureViewDimension.Cube`, `GPUTextureViewDimension.CubeArray`, `GPUTextureViewDimension.ThreeD`, `GPUSamplerDescriptor.addressModeU`, `GPUSamplerDescriptor.addressModeV`, `GPUSamplerDescriptor.addressModeW`, `GPUSamplerDescriptor.magFilter`, `GPUSamplerDescriptor.minFilter`, `GPUSamplerDescriptor.mipmapFilter`, `GPUSamplerDescriptor.lodMinClamp`, `GPUSamplerDescriptor.lodMaxClamp`, `GPUSamplerDescriptor.compare`, `GPUSamplerDescriptor.maxAnisotropy`, `GPUSamplerBindingLayout.type`, `GPUTextureBindingLayout.sampleType`, `GPUTextureBindingLayout.viewDimension`, `GPUTextureBindingLayout.multisampled`, `GPUTextureOrGPUTextureView`

## rendu/passes/attachments

39 déclarations, 4 comportements décrits.

### Comportements

- **`render.color-load-store`** — Les opérations de chargement et de stockage préservent, remplacent ou résolvent le contenu de l'attachement couleur.
  - Cas : **à tester**
- **`render.depth.load-store`** — Les opérations de chargement et de stockage préservent ou remplacent la profondeur selon le descripteur.
  - Cas : **à tester**
- **`render.pass-descriptor`** — Le descripteur de passe assemble les attachements et l'état de passe, et la passe écrit dans les attachements.
  - Cas : **à tester**
- **`render.occlusion-queries`** — Une requête d'occlusion encadre des draw calls et ses résultats sont écrits dans le query set.
  - Cas : **à tester**

### Types de support

`GPULoadOp.Load`, `GPULoadOp.Clear`, `GPUStoreOp.Store`, `GPUStoreOp.Discard`, `GPURenderPassDepthStencilAttachment.view`, `GPURenderPassDepthStencilAttachment.depthClearValue`, `GPURenderPassDepthStencilAttachment.depthLoadOp`, `GPURenderPassDepthStencilAttachment.depthStoreOp`, `GPURenderPassDepthStencilAttachment.depthReadOnly`, `GPURenderPassDepthStencilAttachment.stencilClearValue`, `GPURenderPassDepthStencilAttachment.stencilLoadOp`, `GPURenderPassDepthStencilAttachment.stencilStoreOp`, `GPURenderPassDepthStencilAttachment.stencilReadOnly`

## pipelines/render state

183 déclarations, 5 comportements décrits.

### Comportements

- **`render.pipeline.creation`** — Un pipeline render compile les états vertex/fragment et décrit ses cibles de couleur et leurs blends.
  - Cas : **à tester**
- **`render.vertex-state`** — Les layouts vertex relient buffers et attributs aux locations du shader, et les commandes de draw les consomment.
  - Cas : **à tester**
- **`render.primitive-and-multisample`** — La topologie, le culling et le multisampling sont validés par rapport aux attachements de la passe.
  - Cas : **à tester**
- **`render.depth-stencil-state`** — L'état profondeur/stencil de la pipeline est compatible avec le format d'attachement de la passe.
  - Cas : **à tester**
- **`render.pipeline-async`** — La création asynchrone résout une pipeline render prête ou échoue explicitement.
  - Cas : **à tester**

### Types de support

`GPUColorWrite.value`, `GPUColorWrite.or`, `GPUColorWrite.of`, `GPUColorWrite.None`, `GPUColorWrite.Red`, `GPUColorWrite.Green`, `GPUColorWrite.Blue`, `GPUColorWrite.Alpha`, `GPUColorWrite.All`, `GPUColorWrite.entries`, `GPUBlendFactor.Zero`, `GPUBlendFactor.One`, `GPUBlendFactor.Src`, `GPUBlendFactor.OneMinusSrc`, `GPUBlendFactor.SrcAlpha`, `GPUBlendFactor.OneMinusSrcAlpha`, `GPUBlendFactor.Dst`, `GPUBlendFactor.OneMinusDst`, `GPUBlendFactor.DstAlpha`, `GPUBlendFactor.OneMinusDstAlpha`, `GPUBlendFactor.SrcAlphaSaturated`, `GPUBlendFactor.Constant`, `GPUBlendFactor.OneMinusConstant`, `GPUBlendFactor.Src1`, `GPUBlendFactor.OneMinusSrc1`, `GPUBlendFactor.Src1Alpha`, `GPUBlendFactor.OneMinusSrc1Alpha`, `GPUBlendOperation.Add`, `GPUBlendOperation.Subtract`, `GPUBlendOperation.ReverseSubtract`, `GPUBlendOperation.Min`, `GPUBlendOperation.Max`, `GPUCompareFunction.Never`, `GPUCompareFunction.Less`, `GPUCompareFunction.Equal`, `GPUCompareFunction.LessEqual`, `GPUCompareFunction.Greater`, `GPUCompareFunction.NotEqual`, `GPUCompareFunction.GreaterEqual`, `GPUCompareFunction.Always`, `GPUCullMode.None`, `GPUCullMode.Front`, `GPUCullMode.Back`, `GPUFrontFace.CCW`, `GPUFrontFace.CW`, `GPUIndexFormat.Uint16`, `GPUIndexFormat.Uint32`, `GPUPrimitiveTopology.PointList`, `GPUPrimitiveTopology.LineList`, `GPUPrimitiveTopology.LineStrip`, `GPUPrimitiveTopology.TriangleList`, `GPUPrimitiveTopology.TriangleStrip`, `GPUStencilOperation.Keep`, `GPUStencilOperation.Zero`, `GPUStencilOperation.Replace`, `GPUStencilOperation.Invert`, `GPUStencilOperation.IncrementClamp`, `GPUStencilOperation.DecrementClamp`, `GPUStencilOperation.IncrementWrap`, `GPUStencilOperation.DecrementWrap`, `GPUVertexFormat.Uint8`, `GPUVertexFormat.Uint8x2`, `GPUVertexFormat.Uint8x4`, `GPUVertexFormat.Sint8`, `GPUVertexFormat.Sint8x2`, `GPUVertexFormat.Sint8x4`, `GPUVertexFormat.Unorm8`, `GPUVertexFormat.Unorm8x2`, `GPUVertexFormat.Unorm8x4`, `GPUVertexFormat.Snorm8`, `GPUVertexFormat.Snorm8x2`, `GPUVertexFormat.Snorm8x4`, `GPUVertexFormat.Uint16`, `GPUVertexFormat.Uint16x2`, `GPUVertexFormat.Uint16x4`, `GPUVertexFormat.Sint16`, `GPUVertexFormat.Sint16x2`, `GPUVertexFormat.Sint16x4`, `GPUVertexFormat.Unorm16`, `GPUVertexFormat.Unorm16x2`, `GPUVertexFormat.Unorm16x4`, `GPUVertexFormat.Snorm16`, `GPUVertexFormat.Snorm16x2`, `GPUVertexFormat.Snorm16x4`, `GPUVertexFormat.Float16`, `GPUVertexFormat.Float16x2`, `GPUVertexFormat.Float16x4`, `GPUVertexFormat.Float32`, `GPUVertexFormat.Float32x2`, `GPUVertexFormat.Float32x3`, `GPUVertexFormat.Float32x4`, `GPUVertexFormat.Uint32`, `GPUVertexFormat.Uint32x2`, `GPUVertexFormat.Uint32x3`, `GPUVertexFormat.Uint32x4`, `GPUVertexFormat.Sint32`, `GPUVertexFormat.Sint32x2`, `GPUVertexFormat.Sint32x3`, `GPUVertexFormat.Sint32x4`, `GPUVertexFormat.Unorm1010102`, `GPUVertexFormat.Unorm8x4BGRA`, `GPUVertexStepMode.Vertex`, `GPUVertexStepMode.Instance`, `GPUPipelineDescriptorBase.layout`, `GPURenderPipelineDescriptor.vertex`, `GPURenderPipelineDescriptor.primitive`, `GPURenderPipelineDescriptor.depthStencil`, `GPURenderPipelineDescriptor.multisample`, `GPURenderPipelineDescriptor.fragment`, `GPUPrimitiveState.topology`, `GPUPrimitiveState.stripIndexFormat`, `GPUPrimitiveState.frontFace`, `GPUPrimitiveState.cullMode`, `GPUPrimitiveState.unclippedDepth`, `GPUMultisampleState.count`, `GPUMultisampleState.mask`, `GPUMultisampleState.alphaToCoverageEnabled`, `GPUBlendState.color`, `GPUBlendState.alpha`, `GPUBlendComponent.operation`, `GPUBlendComponent.srcFactor`, `GPUBlendComponent.dstFactor`, `GPUDepthStencilState.format`, `GPUDepthStencilState.stencilFront`, `GPUDepthStencilState.stencilBack`, `GPUDepthStencilState.stencilReadMask`, `GPUDepthStencilState.stencilWriteMask`, `GPUDepthStencilState.depthBias`, `GPUDepthStencilState.depthBiasSlopeScale`, `GPUDepthStencilState.depthBiasClamp`, `GPUStencilFaceState.compare`, `GPUStencilFaceState.failOp`, `GPUStencilFaceState.depthFailOp`, `GPUStencilFaceState.passOp`, `GPUVertexBufferLayout.arrayStride`, `GPUVertexBufferLayout.stepMode`, `GPUVertexBufferLayout.attributes`, `GPUVertexAttribute.format`, `GPUVertexAttribute.offset`, `GPUVertexAttribute.shaderLocation`, `GPURenderPassLayout.colorFormats`, `GPURenderPassLayout.depthStencilFormat`, `GPURenderPassLayout.sampleCount`

## render bundles

8 déclarations, 1 comportements décrits.

### Comportements

- **`renderbundle.record-and-execute`** — Un render bundle enregistre des commandes réutilisables et ne partage pas l'état de la passe qui l'exécute.
  - Cas : **à tester**

### Types de support

`GPURenderBundleEncoderDescriptor.depthReadOnly`, `GPURenderBundleEncoderDescriptor.stencilReadOnly`

## queries/timestamps

15 déclarations, 2 comportements décrits.

### Comportements

- **`query.create-and-resolve`** — Un query set expose son type et son nombre, et resolveQuerySet écrit les résultats dans un buffer.
  - Cas : **à tester**
- **`query.timestamp-writes`** — Les écritures de timestamp encadrent une passe compute ou render et exigent la feature timestamp-query.
  - Features optionnelles : `TimestampQuery`
  - Cas : **à tester**

### Types de support

`GPUQueryType.Occlusion`, `GPUQueryType.Timestamp`, `GPURenderPassTimestampWrites.querySet`, `GPURenderPassTimestampWrites.beginningOfPassWriteIndex`, `GPURenderPassTimestampWrites.endOfPassWriteIndex`, `GPUQuerySetDescriptor.type`, `GPUQuerySetDescriptor.count`

## erreurs/asynchronisme

22 déclarations, 5 comportements décrits.

### Comportements

- **`errors.empty-scope`** — Un scope de validation sans opération résout à null et la pile de scopes reste équilibrée.
  - Cas : `errors.empty-scope`
- **`errors.invalid-buffer-usage`** — Une création invalide est capturée par le scope de validation sous forme de GPUValidationError.
  - Cas : `errors.invalid-buffer-usage`
- **`errors.uncaptured-error`** — Une erreur non capturée est signalée au callback du device et ne peut pas se transformer en succès apparent.
  - Cas : **à tester**
- **`errors.device-lost`** — Une perte de device expose une raison et un message exploitables par le runner.
  - Cas : **à tester**
- **`async.promise-results`** — Les opérations asynchrones distinguent succès, rejet de validation et erreur d'implémentation.
  - Cas : **à tester**

### Types de support

`GPUDeviceLostReason.Unknown`, `GPUDeviceLostReason.Destroyed`, `GPUDeviceLostReason.CallbackCancelled`, `GPUDeviceLostReason.FailedCreation`, `GPUErrorFilter.Validation`, `GPUErrorFilter.OutOfMemory`, `GPUErrorFilter.Internal`, `GPUDeviceLostInfo.reason`, `GPUDeviceLostInfo.message`

## types de données/descripteurs/flags/swizzle

115 déclarations, 3 comportements décrits.

### Comportements

- **`data.arraybuffer`** — Un ArrayBuffer expose sa taille et convertit ses octets vers et depuis les tableaux typés, avec des offsets exprimés en octets.
  - Cas : `buffers.mapped-at-creation`
- **`data.flags`** — Les flag enumerations se combinent en masque et un ensemble vide produit zéro.
  - Cas : `buffers.partial-map-remap`
- **`data.identifiers-and-indices`** — Les alias de tailles, d'index et de coordonnées portent les unités du contrat, et les descripteurs de base exposent un label.
  - Cas : **à tester**

### Types de support

`GPUMapMode.value`, `GPUMapMode.or`, `GPUMapMode.of`, `GPUBufferBindingType.BindingNotUsed`, `GPUBufferBindingType.Uniform`, `GPUBufferBindingType.Storage`, `GPUBufferBindingType.ReadOnlyStorage`, `GPUColor.r`, `GPUColor.g`, `GPUColor.b`, `GPUColor.a`, `GPUOrigin2D.x`, `GPUOrigin2D.y`, `GPUOrigin3D.x`, `GPUOrigin3D.y`, `GPUOrigin3D.z`, `GPUExtent3D.width`, `GPUExtent3D.height`, `GPUExtent3D.depthOrArrayLayers`, `GPUBindingCommandsMixin`, `GPUBindingCommandsMixin.setImmediates`, `GPUDebugCommandsMixin`, `GPURenderCommandsMixin`, `GPURenderCommandsMixin.setPipeline`, `GPURenderCommandsMixin.drawIndirect`, `GPURenderCommandsMixin.drawIndexedIndirect`, `GPUObjectDescriptorBase.label`, `GPUStorageTextureBindingLayout.access`, `GPUStorageTextureBindingLayout.format`, `GPUStorageTextureBindingLayout.viewDimension`

## Couverture des cas exécutables

Les onze cas suivants sont exécutés par le runner navigateur sur JS et Wasm. Le tableau
relie chaque cas aux comportements qu'il exerce ; un cas peut contribuer à plusieurs
comportements et un comportement non couvert reste visible ci-dessus.

| Cas | Comportements |
| --- | --- |
| `buffers.mapped-at-creation` | `buffers.creation-and-usage`, `buffers.usage-flags`, `buffers.mapping.initial-data`, `buffers.mapping.state-transitions`, `data.arraybuffer` |
| `transfers.copy-offsets` | `queue.submit-and-completion`, `transfers.buffer-copy-offsets` |
| `transfers.write-offsets` | `queue.submit-and-completion`, `transfers.queue-write-offsets` |
| `transfers.write-remaining` | `queue.submit-and-completion`, `transfers.queue-write-offsets` |
| `buffers.partial-map-remap` | `queue.submit-and-completion`, `buffers.mapping.state-transitions`, `buffers.mapping.partial-range`, `data.flags` |
| `compute.auto-layout-constants` | `bindgroup.auto-layout`, `bindgroup.entries-and-resources`, `shader.module-creation`, `shader.specialization-constants`, `compute.pipeline-auto-layout`, `compute.pass-dispatch` |
| `compute.explicit-layout-entrypoint` | `bindgroup.explicit-layout`, `bindgroup.entries-and-resources`, `pipelinelayout.bind-group-sequence`, `shader.module-creation`, `compute.pipeline-explicit-entrypoint`, `compute.pass-dispatch` |
| `errors.empty-scope` | `errors.empty-scope` |
| `errors.invalid-buffer-usage` | `buffers.usage-flags`, `errors.invalid-buffer-usage` |
| `errors.map-alignment` | `buffers.mapping.alignment-validation` |
| `buffers.map-destroyed` | `buffers.destroy-and-map` |

## Familles à approfondir

Ces familles sont recensées et décrites, mais aucun cas exécutable ne les exerce encore :

- adapter/device/features/limits
- textures/views/samplers
- rendu/passes/attachments
- pipelines/render state
- render bundles
- queries/timestamps

L'analyse comportementale de ces familles (chemins valides, paramètres optionnels, bornes
et erreurs) doit être approfondie avant de les considérer comme couvertes. La présence
d'un symbole dans `symbols.tsv` n'atteste pas d'un comportement testé.

## Références de comportement hors inventaire

Les entrées de comportement peuvent aussi nommer des membres hérités ou des relations
entre types qui n'apparaissent pas comme lignes de `symbols.tsv` :

`GPUBuffer.close`, `GPUCommandEncoder.label`, `GPUComputePassEncoder.setBindGroup`, `GPUComputePipeline.getBindGroupLayout`, `GPUComputePipelineDescriptor.layout`, `GPUDevice.label`, `GPUQueue.label`
