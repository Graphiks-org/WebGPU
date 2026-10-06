import test from 'node:test';
import assert from 'node:assert/strict';

import {
  compareReports,
  groupByWorkload,
  median,
  p95NearestRank,
  timingDelta,
} from './compare-arraybuffer-benchmarks.mjs';

function report(overrides = {}) {
  return {
    protocol: 'arraybuffer-cpu',
    environmentId: 'macos-aarch64-jvm-25',
    profile: 'standard',
    target: 'jvm',
    scenarios: [
      {
        scenarioId: 'scalar.write.i32.bytes-256.Checked',
        variant: 'Checked',
        workload: 'scalar.write.i32',
        bytesPerOperation: 256,
        operationsPerSample: 65536,
        status: 'completed',
        samplesMs: [10, 10, 10, 10, 10],
      },
    ],
    ...overrides,
  };
}

test('zero baseline never produces an infinite ratio', () => {
  assert.deepEqual(timingDelta(0, 1), { absoluteMs: 1, ratio: null, percent: null });
});

test('a ten percent difference remains a report, not a failure', () => {
  const result = timingDelta(10, 11);
  assert.equal(result.absoluteMs, 1);
  assert.equal(result.ratio, 1.1);
  assert.ok(Math.abs(result.percent - 10) < 1e-9);
});

test('percentiles and median are nearest-rank and stable', () => {
  assert.equal(median([1, 2, 3, 4]), 2.5);
  assert.equal(median([1, 2, 3]), 2);
  assert.equal(p95NearestRank([1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20]), 19);
  assert.equal(p95NearestRank([]), null);
});

test('a compatible pair produces a delta per scenario', () => {
  const comparison = compareReports(report(), report({ scenarios: [{
    ...report().scenarios[0],
    samplesMs: [11, 11, 11, 11, 11],
  }] }));
  assert.equal(comparison.ok, true);
  assert.equal(comparison.scenarios[0].delta.ratio, 1.1);
});

test('a different environment refuses the comparison', () => {
  const comparison = compareReports(report(), report({ environmentId: 'linux-x64-jvm-25' }));
  assert.equal(comparison.ok, false);
  assert.ok(comparison.problems.some((problem) => problem.includes('environment differs')));
});

test('a different repetition count refuses the strict comparison', () => {
  const candidate = report();
  candidate.scenarios[0].operationsPerSample = 32768;
  const comparison = compareReports(report(), candidate);
  assert.equal(comparison.ok, false);
  assert.ok(comparison.problems.some((problem) => problem.includes('operationsPerSample differs')));
});

test('normalize mode compares per-operation and keeps the raw delta', () => {
  const candidate = report();
  candidate.scenarios[0].operationsPerSample = 32768;
  candidate.scenarios[0].samplesMs = [5, 5, 5, 5, 5];
  const comparison = compareReports(report(), candidate, { normalize: true });
  assert.equal(comparison.ok, true);
  assert.equal(comparison.scenarios[0].operationsDiffer, true);
  // 10ms/65536 ops vs 5ms/32768 ops -> identical per-operation cost.
  assert.equal(comparison.scenarios[0].delta.ratio, 1);
  assert.equal(comparison.scenarios[0].rawDelta.ratio, 0.5);
});

test('a failed result is excluded and the campaign is flagged incomplete', () => {
  const candidate = report();
  candidate.scenarios[0].status = 'failed';
  candidate.scenarios[0].samplesMs = [];
  const comparison = compareReports(report(), candidate);
  assert.equal(comparison.ok, false);
  assert.equal(comparison.scenarios[0].excluded, true);
  assert.ok(comparison.problems.some((problem) => problem.includes('incomplete')));
});

test('deltas group by workload so controls and copies read apart', () => {
  const candidate = report();
  candidate.scenarios.push({
    scenarioId: 'bulk.bytes.bytes-256.Checked',
    variant: 'Checked',
    workload: 'bulk.bytes',
    bytesPerOperation: 256,
    operationsPerSample: 65536,
    status: 'completed',
    samplesMs: [10, 10, 10, 10, 10],
  });
  const baseline = report();
  baseline.scenarios.push(candidate.scenarios[1]);
  const comparison = compareReports(baseline, candidate);
  const groups = groupByWorkload(comparison);
  assert.deepEqual(Object.keys(groups).sort(), ['bulk.bytes', 'scalar.write.i32']);
});
