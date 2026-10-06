import test from 'node:test';
import assert from 'node:assert/strict';

import {
  inventory,
  validSample,
  validateAllocationClaim,
  validateReport,
} from './arraybuffer-report.mjs';

function validReport(profile = 'ci') {
  const sampleCount = profile === 'ci' ? 5 : 30;
  return {
    schemaVersion: 1,
    protocol: 'arraybuffer-cpu',
    target: 'jvm',
    buildMode: 'release',
    suiteCommit: 'fixture',
    libraryCommit: 'fixture',
    dirty: false,
    harnessHash: 'harness',
    libraryHash: 'library',
    environmentId: 'fixture',
    profile,
    runIndex: 0,
    environment: {
      os: 'macos',
      architecture: 'aarch64',
      runtime: 'jvm-25',
      kotlinVersion: '2.4.20',
    },
    variantOrder: [],
    scenarios: inventory().map((entry) => ({
      scenarioId: entry.id,
      workload: entry.workload,
      variant: entry.variant,
      bytesPerOperation: entry.bytes,
      width: entry.width,
      height: entry.height,
      count: entry.count,
      operationsPerSample: 1,
      warmups: 3,
      status: 'completed',
      outputVerified: true,
      samplesMs: Array.from({ length: sampleCount }, () => 0.4),
      allocationMeasurement: 'unavailable',
    })),
  };
}

test('a well-formed report is accepted', () => {
  assert.deepEqual(validateReport(validReport(), { profile: 'ci' }), { ok: true, problems: [] });
});

test('duplicate scenario ids are rejected', () => {
  const report = validReport();
  report.scenarios[1].scenarioId = report.scenarios[0].scenarioId;
  const result = validateReport(report, { profile: 'ci' });
  assert.equal(result.ok, false);
  assert.ok(result.problems.some((problem) => problem.includes('duplicate scenario ids')));
});

test('a missing scenario is rejected', () => {
  const report = validReport();
  report.scenarios.pop();
  const result = validateReport(report, { profile: 'ci' });
  assert.equal(result.ok, false);
  assert.ok(result.problems.some((problem) => problem.includes('missing scenario ids')));
});

test('a wrong byte size is rejected', () => {
  const report = validReport();
  report.scenarios[0].bytesPerOperation += 4;
  const result = validateReport(report, { profile: 'ci' });
  assert.equal(result.ok, false);
  assert.ok(result.problems.some((problem) => problem.includes('bytesPerOperation')));
});

test('the wrong sample count is rejected', () => {
  const report = validReport();
  report.scenarios[0].samplesMs = [1, 2, 3];
  const result = validateReport(report, { profile: 'ci' });
  assert.equal(result.ok, false);
  assert.ok(result.problems.some((problem) => problem.includes('samples')));
});

test('negative and NaN durations are rejected', () => {
  const report = validReport();
  report.scenarios[0].samplesMs = [0, -1, 2, 3, 4];
  assert.equal(validateReport(report, { profile: 'ci' }).ok, false);

  const nanReport = validReport();
  nanReport.scenarios[0].samplesMs = [0, Number.NaN, 2, 3, 4];
  assert.equal(validateReport(nanReport, { profile: 'ci' }).ok, false);
});

test('an unverified output is rejected', () => {
  const report = validReport();
  report.scenarios[0].outputVerified = false;
  const result = validateReport(report, { profile: 'ci' });
  assert.equal(result.ok, false);
  assert.ok(result.problems.some((problem) => problem.includes('not verified')));
});

test('a zero duration is kept, not rejected', () => {
  assert.equal(validSample(0), true);
  const report = validReport();
  report.scenarios[0].samplesMs = [0, 0, 0, 0, 0];
  assert.equal(validateReport(report, { profile: 'ci' }).ok, true);
});

test('an allocation measurement must carry a byte count', () => {
  const report = validReport();
  report.scenarios[0].allocationMeasurement = 'measured';
  const result = validateAllocationClaim(report);
  assert.equal(result.ok, false);
  assert.ok(result.problems.some((problem) => problem.includes('no byte count')));
});

test('the standard profile expects thirty samples', () => {
  assert.equal(validateReport(validReport('standard'), { profile: 'standard' }).ok, true);
  const report = validReport('standard');
  report.scenarios[0].samplesMs = report.scenarios[0].samplesMs.slice(0, 5);
  assert.equal(validateReport(report, { profile: 'standard' }).ok, false);
});
