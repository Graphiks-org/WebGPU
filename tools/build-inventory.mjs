// Builds the generated contract inventory for the Validation site from the generated case data,
// the localized texts and the public API sources. Nothing it writes is versioned.
//
// Usage: node tools/build-inventory.mjs [output-directory]
import { mkdir, readFile, writeFile } from 'node:fs/promises';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const outputDir = resolve(process.argv[2] ?? join(root, 'build', 'site', 'inventory'));

const generatedDir = join(root, 'suite-acid-tests', 'build', 'suite-inventory');
const apiDir = join(root, 'webgpu-api', 'src', 'commonMain', 'kotlin');
const apiFiles = ['ArrayBuffer.kt', 'FlagEnumeration.kt', 'GPUTextureSwizzle.kt', 'bitflags.kt', 'enumerations.kt', 'interfaces.kt', 'typealiases.kt'];
const locales = ['en', 'fr'];
const defaultLocale = 'en';

const readJson = async (path) => JSON.parse(await readFile(path, 'utf8'));

// --- Families ----------------------------------------------------------------------------------

const dataTypeFamily = 'types de données/descripteurs/flags/swizzle';
const adapterNames = new Set(['GPUAdapter', 'GPUAdapterInfo', 'GPURequestAdapterOptions', 'GPUDevice', 'GPUSupportedLimits', 'GPUFeatureName', 'GPUSupportedFeatures', 'GPUPowerPreference']);
const errorNames = new Set(['GPUError', 'GPUValidationError', 'GPUOutOfMemoryError', 'GPUInternalError', 'GPUUncapturedErrorCallback', 'GPUDeviceLostInfo', 'GPUDeviceLostReason', 'GPUErrorFilter']);
const familyRules = [
  [/^(GPUTexture(?!Copy)|GPUSampler|GPUAddressMode|GPUFilterMode|GPUMipmapFilterMode|GPUTextureFormat|GPUTextureSampleType|GPUTextureViewDimension|GPUTextureAspect|GPUStorageTextureAccess|GPUSamplerBindingType|GPUTextureDimension|GPUTextureUsage)/, 'textures/views/samplers'],
  [/^(GPURenderPass(?!Layout)|GPULoadOp|GPUStoreOp)/, 'rendu/passes/attachments'],
  [/^(GPURenderPipeline|GPUVertex|GPUPrimitiveState|GPUMultisampleState|GPUFragmentState|GPUColorTargetState|GPUBlend|GPUDepthStencilState|GPUStencilFaceState|GPURenderPassLayout|GPUPipelineBase|GPUPipelineDescriptorBase|GPURenderPipelineDescriptor|GPUColorWrite|GPUCompareFunction|GPUCullMode|GPUFrontFace|GPUPrimitiveTopology|GPUStencilOperation|GPUVertexFormat|GPUVertexStepMode|GPUIndexFormat|GPUStencilValue|GPUSampleMask|GPUDepthBias)/, 'pipelines/render state'],
  [/^(GPUComputePipeline|GPUComputePass)/, 'compute'],
  [/^(GPUShaderModule|GPUCompilation|GPUProgrammableStage|GPUShaderStage)/, 'shaders/compilation'],
  [/^GPUBindGroup|^GPUPipelineLayout/, 'bind groups/layouts'],
  [/^(GPUQuerySet|GPUQueryType|GPUComputePassTimestampWrites|GPURenderPassTimestampWrites|GPUComputePassDescriptor)/, 'queries/timestamps'],
  [/^GPUTexelCopy/, 'transferts buffers/textures'],
  [/^GPURenderBundle/, 'render bundles'],
  [/^(GPUQueue|GPUCommandEncoder|GPUCommandBuffer|GPUCommandsMixin)/, 'queue/commandes'],
  [/^(GPUBuffer(?!Binding)|GPUBufferDescriptor|GPUBufferMapState|GPUBufferUsage|GPUBufferDynamicOffset)/, 'buffers/mapping'],
];
const deviceMembers = {
  createBuffer: 'buffers/mapping', createTexture: 'textures/views/samplers', createSampler: 'textures/views/samplers',
  createBindGroup: 'bind groups/layouts', createBindGroupLayout: 'bind groups/layouts', createPipelineLayout: 'bind groups/layouts',
  createShaderModule: 'shaders/compilation', createComputePipeline: 'compute', createComputePipelineAsync: 'compute',
  createRenderPipeline: 'pipelines/render state', createRenderPipelineAsync: 'pipelines/render state',
  createRenderBundleEncoder: 'render bundles', createQuerySet: 'queries/timestamps', createCommandEncoder: 'queue/commandes',
  queue: 'queue/commandes', pushErrorScope: 'erreurs/asynchronisme', popErrorScope: 'erreurs/asynchronisme',
};
const encoderMembers = {
  beginRenderPass: 'rendu/passes/attachments', beginComputePass: 'compute',
  copyBufferToBuffer: 'transferts buffers/textures', copyBufferToTexture: 'transferts buffers/textures',
  copyTextureToBuffer: 'transferts buffers/textures', copyTextureToTexture: 'transferts buffers/textures',
  clearBuffer: 'transferts buffers/textures', resolveQuerySet: 'queries/timestamps',
};
const queueMembers = { submit: 'queue/commandes', onSubmittedWorkDone: 'erreurs/asynchronisme', writeBuffer: 'transferts buffers/textures', writeTexture: 'transferts buffers/textures' };

function familyFor(owner, member = '') {
  if (member === 'pushErrorScope' || member === 'popErrorScope') return 'erreurs/asynchronisme';
  if (owner === 'GPUDevice') return deviceMembers[member] ?? 'adapter/device/features/limits';
  if (owner === 'GPUCommandEncoder') return encoderMembers[member] ?? 'queue/commandes';
  if (owner === 'GPUQueue') return queueMembers[member] ?? 'queue/commandes';
  if (adapterNames.has(owner) || member === 'requestDevice') return 'adapter/device/features/limits';
  if (errorNames.has(owner)) return 'erreurs/asynchronisme';
  if (owner === 'GPUBufferBinding' || owner === 'GPUBufferBindingLayout') return 'bind groups/layouts';
  for (const [pattern, family] of familyRules) if (pattern.test(owner)) return family;
  return dataTypeFamily;
}

// --- Public API declarations (symbols.tsv) -----------------------------------------------------

async function parseSymbols() {
  const rows = [];
  const add = (symbol, kind, source, line) => {
    const [owner, member] = [symbol.split('.')[0], symbol.split('.').pop()];
    rows.push({ symbol, kind, source, line, family: familyFor(owner, member) });
  };

  for (const name of apiFiles) {
    const lines = (await readFile(join(apiDir, name), 'utf8')).split('\n');
    let owner = null;
    let enumOwner = null;
    let bitflagOwner = null;
    let swizzleEnum = false;

    lines.forEach((raw, index) => {
      const line = raw.replace(/\s+$/, '');
      const number = index + 1;

      if (name === 'interfaces.kt') {
        const decl = /^(?:sealed |fun )?interface (\w+)(.*)$/.exec(line);
        if (decl) {
          add(decl[1], 'interface', name, number);
          if (decl[2].includes('AutoCloseable')) add(`${decl[1]}.close`, 'function', name, number);
          if (decl[2].includes('GPUPipelineBase')) add(`${decl[1]}.getBindGroupLayout`, 'function', name, number);
          if (decl[2].includes('GPUBindingCommandsMixin')) {
            add(`${decl[1]}.setBindGroup`, 'function', name, number);
            add(`${decl[1]}.setImmediates`, 'function', name, number);
          }
          if (decl[2].includes('GPUDebugCommandsMixin')) {
            for (const member of ['pushDebugGroup', 'popDebugGroup', 'insertDebugMarker']) add(`${decl[1]}.${member}`, 'function', name, number);
          }
          if (decl[2].includes('GPURenderCommandsMixin')) {
            for (const member of ['setPipeline', 'setIndexBuffer', 'setVertexBuffer', 'draw', 'drawIndexed', 'drawIndirect', 'drawIndexedIndirect']) add(`${decl[1]}.${member}`, 'function', name, number);
          }
          owner = decl[1];
          return;
        }
        if (line.startsWith('}')) { owner = null; return; }
        const member = /^\t(?:suspend )?(?:val|var|fun) (\w+)/.exec(line);
        if (member && owner) add(`${owner}.${member[1]}`, 'member', name, number);
      } else if (name === 'enumerations.kt') {
        const decl = /^expect enum class (\w+)/.exec(line);
        if (decl) { add(decl[1], 'enum', name, number); enumOwner = decl[1]; return; }
        if (line.startsWith('}')) { enumOwner = null; return; }
        const entry = /^([A-Za-z_]\w*)\s*[,;]?$/.exec(line.trim());
        if (entry && enumOwner) add(`${enumOwner}.${entry[1]}`, 'enum-entry', name, number);
      } else if (name === 'bitflags.kt') {
        const decl = /^public value class (\w+)/.exec(line);
        if (decl) { add(decl[1], 'bitflag', name, number); bitflagOwner = decl[1]; return; }
        if (line.startsWith('}')) { bitflagOwner = null; return; }
        const entry = /^\s+public val\s+`?(\w+)`?:/.exec(line);
        if (entry && bitflagOwner) { add(`${bitflagOwner}.${entry[1]}`, 'bitflag-entry', name, number); return; }
        const fn = /^\s+public infix fun\s+(\w+)\(/.exec(line);
        if (fn && bitflagOwner) add(`${bitflagOwner}.${fn[1]}`, 'function', name, number);
      } else if (name === 'typealiases.kt') {
        const decl = /^typealias\s+(\w+)/.exec(line);
        if (decl) add(decl[1], 'typealias', name, number);
      } else if (name === 'ArrayBuffer.kt') {
        if (/^expect sealed interface\s+ArrayBuffer/.test(line)) { add('ArrayBuffer', 'interface', name, number); owner = 'ArrayBuffer'; return; }
        const top = /^    (?:val|var|fun)\s+(\w+)/.exec(line);
        if (top) { add(`ArrayBuffer.${top[1]}`, 'member', name, number); return; }
        const companion = /^        (?:actual )?fun\s+(\w+)\(/.exec(line);
        if (companion) add(`ArrayBuffer.${companion[1]}`, 'function', name, number);
      } else if (name === 'FlagEnumeration.kt') {
        if (/^interface\s+FlagEnumeration/.test(line)) { add('FlagEnumeration', 'interface', name, number); owner = 'FlagEnumeration'; return; }
        if (line.startsWith('}')) { owner = null; return; }
        const value = /^\s+val\s+(\w+)/.exec(line);
        if (value && owner) { add(`FlagEnumeration.${value[1]}`, 'member', name, number); return; }
        const fn = /^fun\s+Set<FlagEnumeration>\.(\w+)/.exec(line);
        if (fn) add(`Set<FlagEnumeration>.${fn[1]}`, 'function', name, number);
      } else if (name === 'GPUTextureSwizzle.kt') {
        const enumDecl = /^enum class\s+(\w+)/.exec(line);
        if (enumDecl) { add(enumDecl[1], 'enum', name, number); owner = enumDecl[1]; swizzleEnum = true; return; }
        if (swizzleEnum) {
          const entry = /^    ([A-Za-z_]\w*)\s*\(/.exec(line);
          if (entry) add(`${owner}.${entry[1]}`, 'enum-entry', name, number);
          if (line.startsWith('}')) { swizzleEnum = false; owner = null; }
          return;
        }
        const dataDecl = /^data class\s+(\w+)/.exec(line);
        if (dataDecl) { add(dataDecl[1], 'data-class', name, number); owner = dataDecl[1]; return; }
        const fn = /^    fun\s+(\w+)/.exec(line);
        if (fn && owner) { add(`${owner}.${fn[1]}`, 'function', name, number); return; }
        const value = /^    val\s+(\w+)/.exec(line);
        if (value && owner) add(`${owner}.${value[1]}`, 'member', name, number);
      }
    });
  }
  return rows;
}

// --- Behaviours --------------------------------------------------------------------------------

function buildBehaviours(locale, cases, uncovered, i18n) {
  const behaviours = [];
  for (const entry of cases) {
    const text = i18n.cases?.[entry.id];
    if (!text) throw new Error(`Missing i18n case text for ${entry.id} (${locale})`);
    behaviours.push({
      id: entry.id,
      title: text.title,
      family: entry.family,
      familyLabel: i18n.families?.[entry.family] ?? entry.family,
      contract: entry.contract,
      requiredFeatures: entry.requiredFeatures,
      expectation: text.expectation,
      caseIds: [entry.id],
    });
  }
  for (const entry of uncovered) {
    const expectation = i18n.behaviours?.[entry.id];
    if (!expectation) throw new Error(`Missing i18n behaviour text for ${entry.id} (${locale})`);
    behaviours.push({
      id: entry.id,
      title: null,
      family: entry.family,
      familyLabel: i18n.families?.[entry.family] ?? entry.family,
      contract: entry.contract,
      requiredFeatures: entry.requiredFeatures,
      expectation,
      caseIds: [],
    });
  }
  return behaviours;
}

function contractMarkdown(behaviours, symbols) {
  const lines = ['# Generated contract inventory', ''];
  lines.push(`Generated from the case annotations and the public API sources; not versioned. Locale: \`${defaultLocale}\`.`, '');
  lines.push(`Total: **${symbols.length} declarations**, **${behaviours.length} behaviours**.`, '');
  lines.push('A behaviour without a case is **to be tested**; that never means passing.', '');
  const families = new Map();
  for (const behaviour of behaviours) {
    if (!families.has(behaviour.familyLabel)) families.set(behaviour.familyLabel, []);
    families.get(behaviour.familyLabel).push(behaviour);
  }
  for (const [family, entries] of families) {
    lines.push(`## ${family}`, '');
    for (const behaviour of entries) {
      lines.push(`- **\`${behaviour.id}\`** — ${behaviour.expectation}`);
      lines.push(`  - ${behaviour.caseIds.length ? `Cases: ${behaviour.caseIds.map((id) => `\`${id}\``).join(', ')}` : 'To be tested'}`);
    }
    lines.push('');
  }
  return `${lines.join('\n')}\n`;
}

function symbolsTsv(symbols) {
  const header = 'symbol\tkind\tsource\tline\tfamily\n';
  const body = symbols.map((row) => [row.symbol, row.kind, row.source, row.line, row.family].join('\t')).join('\n');
  return `${header}${body}\n`;
}

// --- Entry point -------------------------------------------------------------------------------

const [cases, caseIds, baseline, uncovered, ...i18ns] = await Promise.all([
  readJson(join(generatedDir, 'cases.json')),
  readJson(join(generatedDir, 'foundation-case-ids.json')),
  readJson(join(generatedDir, 'baseline.json')),
  readJson(join(root, 'inventory', 'uncovered-behaviours.json')),
  ...locales.map((locale) => readJson(join(root, 'inventory', 'i18n', `behaviours.${locale}.json`))),
]);

const symbols = await parseSymbols();
await mkdir(outputDir, { recursive: true });

const byLocale = {};
for (const [index, locale] of locales.entries()) {
  byLocale[locale] = buildBehaviours(locale, cases, uncovered, i18ns[index]);
  await writeFile(join(outputDir, `behaviours.${locale}.json`), `${JSON.stringify(byLocale[locale], null, 2)}\n`);
}
await writeFile(join(outputDir, 'symbols.tsv'), symbolsTsv(symbols));
await writeFile(join(outputDir, 'contract.md'), contractMarkdown(byLocale[defaultLocale], symbols));
await writeFile(join(outputDir, 'baseline.json'), `${JSON.stringify(baseline, null, 2)}\n`);
await writeFile(join(outputDir, 'cases.json'), `${JSON.stringify(cases, null, 2)}\n`);
await writeFile(join(outputDir, 'foundation-case-ids.json'), `${JSON.stringify(caseIds, null, 2)}\n`);

console.log(`inventory built in ${outputDir}: ${symbols.length} symbols, ${byLocale[defaultLocale].length} behaviours per locale`);
