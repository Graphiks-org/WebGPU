// Compares two ArrayBuffer CPU campaigns and reports per-scenario deltas. A comparison is only
// produced between compatible contexts (same protocol, environment, profile and repetition count);
// it never merges targets into a global score.
//
// Usage as a CLI: node tools/compare-arraybuffer-benchmarks.mjs <baseline.json> <candidate.json>
//                   [--output=path] [--label=baseline,candidate]
import { readFileSync, writeFileSync, mkdirSync } from 'node:fs';
import { dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

export const MILLISECONDS = 'ms';

/** Difference of two durations. A zero baseline yields `ratio: null`, never an infinite ratio. */
export function timingDelta(baselineMs, candidateMs) {
  const absoluteMs = candidateMs - baselineMs;
  if (baselineMs === 0) return { absoluteMs, ratio: null, percent: null };
  const ratio = candidateMs / baselineMs;
  return { absoluteMs, ratio, percent: (ratio - 1) * 100 };
}

export function median(values) {
  const sorted = [...values].sort((a, b) => a - b);
  const middle = Math.floor(sorted.length / 2);
  if (sorted.length % 2 === 1) return sorted[middle];
  return (sorted[middle - 1] + sorted[middle]) / 2;
}

export function p95NearestRank(values) {
  if (values.length === 0) return null;
  const sorted = [...values].sort((a, b) => a - b);
  const rank = Math.ceil(0.95 * sorted.length) - 1;
  return sorted[Math.max(0, Math.min(rank, sorted.length - 1))];
}

export function summarize(samplesMs) {
  return {
    median: median(samplesMs),
    min: Math.min(...samplesMs),
    max: Math.max(...samplesMs),
    p95: p95NearestRank(samplesMs),
    samples: samplesMs,
  };
}

function unwrap(document) {
  return document?.report ?? document;
}

export function compareReports(baselineDocument, candidateDocument, { labels = {}, normalize = false } = {}) {
  const baseline = unwrap(baselineDocument);
  const candidate = unwrap(candidateDocument);
  const problems = [];
  const scenarios = [];

  if (baseline == null || candidate == null) {
    return { ok: false, problems: ['a report is missing'], scenarios: [] };
  }
  if (baseline.protocol !== candidate.protocol) {
    problems.push(`protocol differs: ${baseline.protocol} vs ${candidate.protocol}`);
  }
  if (baseline.environmentId !== candidate.environmentId) {
    problems.push(`environment differs: ${baseline.environmentId} vs ${candidate.environmentId}`);
  }
  if (baseline.profile !== candidate.profile) {
    problems.push(`profile differs: ${baseline.profile} vs ${candidate.profile}`);
  }

  const candidateById = new Map((candidate.scenarios ?? []).map((entry) => [entry.scenarioId, entry]));
  let incomplete = false;

  for (const base of baseline.scenarios ?? []) {
    const next = candidateById.get(base.scenarioId);
    if (next == null) {
      problems.push(`missing in candidate: ${base.scenarioId}`);
      continue;
    }
    if (base.status !== 'completed' || next.status !== 'completed') {
      incomplete = true;
      scenarios.push({
        scenarioId: base.scenarioId,
        variant: base.variant,
        workload: base.workload,
        bytesPerOperation: base.bytesPerOperation,
        excluded: true,
        reason: `status ${base.status} -> ${next.status}`,
      });
      continue;
    }
    const operationsDiffer = base.operationsPerSample !== next.operationsPerSample;
    if (operationsDiffer && !normalize) {
      problems.push(
        `operationsPerSample differs for ${base.scenarioId}: ` +
          `${base.operationsPerSample} vs ${next.operationsPerSample}`,
      );
      continue;
    }
    const baselineSummary = summarize(base.samplesMs ?? []);
    const candidateSummary = summarize(next.samplesMs ?? []);
    const baselinePerOperation = baselineSummary.median / base.operationsPerSample;
    const candidatePerOperation = candidateSummary.median / next.operationsPerSample;
    scenarios.push({
      scenarioId: base.scenarioId,
      variant: base.variant,
      workload: base.workload,
      bytesPerOperation: base.bytesPerOperation,
      operationsPerSample: base.operationsPerSample,
      candidateOperationsPerSample: next.operationsPerSample,
      operationsDiffer,
      baselineMs: baselineSummary.median,
      candidateMs: candidateSummary.median,
      baselineMsPerOperation: baselinePerOperation,
      candidateMsPerOperation: candidatePerOperation,
      // Per-operation deltas are the comparable quantity when the two runs calibrated different
      // repetition counts; raw deltas are kept for transparency.
      delta: timingDelta(baselinePerOperation, candidatePerOperation),
      rawDelta: timingDelta(baselineSummary.median, candidateSummary.median),
      variance: {
        baselineMedianRange: [baselineSummary.min, baselineSummary.max],
        candidateMedianRange: [candidateSummary.min, candidateSummary.max],
        baselineP95: baselineSummary.p95,
        candidateP95: candidateSummary.p95,
      },
    });
  }

  if (incomplete) problems.push('at least one scenario was not completed; campaign is incomplete');

  return {
    ok: problems.length === 0,
    baseline: { target: baseline.target, environmentId: baseline.environmentId, profile: baseline.profile },
    candidate: { target: candidate.target, environmentId: candidate.environmentId, profile: candidate.profile },
    labels,
    problems,
    scenarios,
  };
}

/** Groups the per-scenario deltas by workload so control cost, copies and preparation read apart. */
export function groupByWorkload(comparison) {
  const groups = new Map();
  for (const scenario of comparison.scenarios ?? []) {
    if (scenario.excluded) continue;
    const key = scenario.workload;
    if (!groups.has(key)) groups.set(key, []);
    groups.get(key).push(scenario);
  }
  return Object.fromEntries(groups);
}

function main() {
  const baselinePath = process.argv[2];
  const candidatePath = process.argv[3];
  if (!baselinePath || !candidatePath) {
    console.error('usage: node tools/compare-arraybuffer-benchmarks.mjs <baseline.json> <candidate.json> [--output=path]');
    process.exit(2);
  }
  const outputArgument = process.argv.find((argument) => argument.startsWith('--output='));
  const output = outputArgument ? outputArgument.slice('--output='.length) : null;
  const normalize = process.argv.includes('--normalize');
  const baseline = JSON.parse(readFileSync(baselinePath, 'utf8'));
  const candidate = JSON.parse(readFileSync(candidatePath, 'utf8'));
  const comparison = compareReports(baseline, candidate, {
    labels: { baseline: baselinePath, candidate: candidatePath },
    normalize,
  });
  if (output) {
    mkdirSync(dirname(output), { recursive: true });
    writeFileSync(output, `${JSON.stringify(comparison, null, 2)}\n`);
  }
  if (comparison.problems.length > 0) {
    console.error('comparison refused or incomplete:');
    for (const problem of comparison.problems) console.error(`- ${problem}`);
    process.exitCode = 1;
    return;
  }
  console.log(`compared ${comparison.scenarios.length} scenarios on ${comparison.candidate.target} (${comparison.candidate.profile})`);
  if (output) console.log(`wrote ${output}`);
}

if (process.argv[1] && fileURLToPath(import.meta.url) === process.argv[1]) {
  main();
}
