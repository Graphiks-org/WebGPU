// Validates the ArrayBuffer CPU campaign reports (`arraybuffer-cpu-v1`).
//
// The inventory mirrors webgpu-api/arraybuffer-benchmarks Protocol.kt. Keeping it here lets the
// collectors check a report against the protocol instead of trusting whatever the runner published.
//
// Usage as a CLI: node tools/arraybuffer-report.mjs <report.json> [--profile=ci|standard]
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';

export const PROTOCOL = 'arraybuffer-cpu-v1';
export const WRITERS_PROTOCOL = 'arraybuffer-cpu-writers-v1';
export const PROTOCOLS = [PROTOCOL, WRITERS_PROTOCOL];
export const SCHEMA_VERSION = 1;
export const PROFILE_SAMPLES = { ci: 5, standard: 30 };

const SCALAR_SIZES = [256, 65536, 8388608];
const SCATTER_SIZES = [65536, 8388608];
const BULK_SIZES = [256, 65536, 8388608];
const IMAGE_DIMENSIONS = [[256, 256], [1024, 1024], [4096, 4096]];
const VERTEX_COUNTS = [1024, 65536, 1048576];
const VERTEX_BYTES = 32;

const SCALAR_VARIANTS = ['Checked', 'Reference'];
const PRODUCE_VARIANTS = ['Checked', 'BulkPrepared', 'PrepareAndBulk'];

export function scenarioId(workload, bytes, variant) {
  return `${workload}.bytes-${bytes}.${variant}`;
}

export function inventory() {
  const out = [];
  const push = (workload, bytes, variant, width = 0, height = 0, count = 0) => {
    out.push({ id: scenarioId(workload, bytes, variant), workload, variant, bytes, width, height, count });
  };

  for (const size of SCALAR_SIZES) {
    for (const variant of SCALAR_VARIANTS) {
      push('scalar.write.i32', size, variant);
      push('scalar.read.i32', size, variant);
      push('scalar.write.f32', size, variant);
    }
  }
  for (const size of SCATTER_SIZES) {
    for (const variant of SCALAR_VARIANTS) push('scatter.write.i32', size, variant);
  }
  for (const size of BULK_SIZES) {
    for (const variant of SCALAR_VARIANTS) {
      push('bulk.bytes', size, variant);
      push('bulk.floats', size, variant);
    }
  }
  for (const [width, height] of IMAGE_DIMENSIONS) {
    const bytes = width * height * 4;
    for (const variant of PRODUCE_VARIANTS) push('image.rgba8', bytes, variant, width, height, 0);
  }
  for (const count of VERTEX_COUNTS) {
    const bytes = count * VERTEX_BYTES;
    for (const variant of PRODUCE_VARIANTS) push('vertices.p3n3uv2', bytes, variant, 0, 0, count);
  }
  return out;
}

/** Companion inventory: the writer prototypes re-measure their own controls. */
export function writersInventory() {
  const out = [];
  for (const [width, height] of IMAGE_DIMENSIONS) {
    const bytes = width * height * 4;
    for (const variant of ['Checked', 'BulkPrepared', 'PrepareAndBulk', 'RgbaWriter']) {
      out.push({ id: scenarioId('image.rgba8', bytes, variant), workload: 'image.rgba8', variant, bytes, width, height, count: 0 });
    }
  }
  for (const count of VERTEX_COUNTS) {
    const bytes = count * VERTEX_BYTES;
    for (const variant of ['Checked', 'BulkPrepared', 'PrepareAndBulk', 'VertexWriter']) {
      out.push({ id: scenarioId('vertices.p3n3uv2', bytes, variant), workload: 'vertices.p3n3uv2', variant, bytes, width: 0, height: 0, count });
    }
  }
  return out;
}

export function inventoryFor(protocol) {
  return protocol === WRITERS_PROTOCOL ? writersInventory() : inventory();
}

function duplicates(values) {
  return values.filter((value, index) => values.indexOf(value) !== index);
}

/**
 * Compact `scenarioId:operations;...` calibration extracted from a report so a later run can reuse
 * the exact repetition counts instead of recalibrating.
 */
export function calibrationSpec(document) {
  const report = document?.report ?? document;
  // Comma-separated: a semicolon would be parsed as a command separator by `adb shell`.
  return (report?.scenarios ?? [])
    .map((entry) => `${entry.scenarioId}:${entry.operationsPerSample}`)
    .join(',');
}

/** A duration is acceptable when it is a finite, non-negative number. Zero is kept, not rejected. */
export function validSample(sample) {
  return typeof sample === 'number' && Number.isFinite(sample) && sample >= 0;
}

export function validateReport(report, { profile = null } = {}) {
  const problems = [];
  if (report == null || typeof report !== 'object') {
    return { ok: false, problems: ['report is not an object'] };
  }
  if (!PROTOCOLS.includes(report.protocol)) {
    problems.push(`unexpected protocol: ${report.protocol}`);
  }
  const expected = inventoryFor(report.protocol);
  const expectedById = new Map(expected.map((entry) => [entry.id, entry]));

  if (report.schemaVersion !== SCHEMA_VERSION) problems.push(`unexpected schemaVersion: ${report.schemaVersion}`);
  if (profile != null && report.profile !== profile) {
    problems.push(`unexpected profile: ${report.profile} (expected ${profile})`);
  }
  if (report.fatalError) problems.push(`fatal error: ${report.fatalError}`);

  const scenarios = Array.isArray(report.scenarios) ? report.scenarios : null;
  if (scenarios == null) {
    problems.push('report has no scenarios array');
    return { ok: false, problems };
  }

  const ids = scenarios.map((entry) => entry.scenarioId);
  const duplicateIds = duplicates(ids);
  if (duplicateIds.length > 0) problems.push(`duplicate scenario ids: ${[...new Set(duplicateIds)].join(', ')}`);

  const missing = expected.filter((entry) => !ids.includes(entry.id)).map((entry) => entry.id);
  if (missing.length > 0) problems.push(`missing scenario ids: ${missing.join(', ')}`);

  const unexpected = ids.filter((id) => !expectedById.has(id));
  if (unexpected.length > 0) problems.push(`unexpected scenario ids: ${unexpected.join(', ')}`);

  const sampleCount = PROFILE_SAMPLES[profile] ?? PROFILE_SAMPLES[report.profile];
  for (const entry of scenarios) {
    const expectedEntry = expectedById.get(entry.scenarioId);
    if (expectedEntry == null) continue;
    if (entry.bytesPerOperation !== expectedEntry.bytes) {
      problems.push(`${entry.scenarioId}: bytesPerOperation=${entry.bytesPerOperation} expected ${expectedEntry.bytes}`);
    }
    if (entry.status !== 'completed') {
      problems.push(`${entry.scenarioId}: status=${entry.status}`);
      continue;
    }
    if (entry.outputVerified !== true) problems.push(`${entry.scenarioId}: output was not verified`);
    const samples = entry.samplesMs;
    if (!Array.isArray(samples)) {
      problems.push(`${entry.scenarioId}: samplesMs is not an array`);
      continue;
    }
    if (sampleCount != null && samples.length !== sampleCount) {
      problems.push(`${entry.scenarioId}: ${samples.length} samples, expected ${sampleCount}`);
    }
    samples.forEach((sample, index) => {
      if (!validSample(sample)) problems.push(`${entry.scenarioId}: invalid sample #${index}: ${sample}`);
    });
    if (entry.operationsPerSample == null || entry.operationsPerSample < 1) {
      problems.push(`${entry.scenarioId}: operationsPerSample missing`);
    }
  }

  return { ok: problems.length === 0, problems };
}

/** Ensures a report only claims what a profiler actually measured. */
export function validateAllocationClaim(report) {
  const problems = [];
  for (const entry of report?.scenarios ?? []) {
    const measurement = entry.allocationMeasurement;
    if (measurement === 'unavailable') continue;
    if (measurement === 'measured' && entry.allocatedBytesPerOperation == null) {
      problems.push(`${entry.scenarioId}: claims a measurement with no byte count`);
    }
    if (measurement === 'estimated' && entry.allocatedBytesPerOperation == null) {
      problems.push(`${entry.scenarioId}: claims an estimate with no byte count`);
    }
  }
  return { ok: problems.length === 0, problems };
}

function main() {
  const file = process.argv[2];
  if (!file) {
    console.error('usage: node tools/arraybuffer-report.mjs <report.json> [--profile=ci|standard]');
    process.exit(2);
  }
  const profileArgument = process.argv.find((argument) => argument.startsWith('--profile='));
  const profile = profileArgument ? profileArgument.slice('--profile='.length) : null;
  // Browser and Android runners wrap the campaign report in a collector envelope; the JVM and
  // native runners write the report directly. Accept both.
  const document = JSON.parse(readFileSync(file, 'utf8'));
  const report = document?.report ?? document;
  const result = validateReport(report, { profile });
  const allocation = validateAllocationClaim(report);
  const problems = [...result.problems, ...allocation.problems];
  if (problems.length > 0) {
    console.error(`invalid report ${file}:`);
    for (const problem of problems) console.error(`- ${problem}`);
    process.exitCode = 1;
    return;
  }
  console.log(`valid report ${file}: ${report.scenarios.length} scenarios (${report.profile})`);
}

if (process.argv[1] && fileURLToPath(import.meta.url) === process.argv[1]) {
  main();
}
