// Drives the ArrayBuffer CPU campaign on an Android device or emulator through instrumentation.
//
// Usage: node tools/run-arraybuffer-android.mjs [--profile=ci|standard] [--run-index=0]
//          [--serial=<adb-serial>] [--output=path]
//
// The host app is not debuggable in release, so the report is read from the instrumentation status
// stream (`arraybufferReport`), never through `run-as`. Build and install the host first:
//   ./gradlew :arraybuffer-android-instrumentation:installRelease
import { spawnSync } from 'node:child_process';
import { mkdir, writeFile } from 'node:fs/promises';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

import { readFile } from 'node:fs/promises';

import { calibrationSpec, validateAllocationClaim, validateReport } from './arraybuffer-report.mjs';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const profile = optionValue('--profile') ?? 'ci';
const runIndex = Number.parseInt(optionValue('--run-index') ?? '0', 10);
const serial = optionValue('--serial');
const output = optionValue('--output')
  ?? join(root, 'build', 'reports', 'arraybuffer', `android-cpu-run${runIndex}.json`);

function optionValue(name) {
  const prefix = `${name}=`;
  const match = process.argv.slice(2).find((argument) => argument.startsWith(prefix));
  return match ? match.slice(prefix.length) : null;
}

if (!['ci', 'standard'].includes(profile)) {
  console.error(`--profile must be ci or standard, was '${profile}'.`);
  process.exit(2);
}

// Raw instrumentation output (`-r`) is printed as `INSTRUMENTATION_STATUS:` lines; the pretty
// form drops bundle keys when stdout is not a TTY, so the runner always asks for raw output and
// merges stderr, where `am instrument` can place part of the stream.
function adb(args) {
  const result = spawnSync('adb', serial ? ['-s', serial, ...args] : args, {
    encoding: 'utf8',
    maxBuffer: 64 * 1024 * 1024,
  });
  return `${result.stdout ?? ''}\n${result.stderr ?? ''}`;
}

const adbOutput = (...args) => adb(args);

const devices = adbOutput('devices')
  .split('\n')
  .slice(1)
  .map((line) => line.trim())
  .filter((line) => line.endsWith('device'));
if (devices.length === 0) {
  console.error('no adb device or emulator is connected; Android campaign not run');
  process.exit(3);
}

const properties = {};
for (const name of ['ro.product.model', 'ro.build.version.sdk', 'ro.build.version.release', 'ro.product.cpu.abi']) {
  properties[name] = adbOutput('shell', 'getprop', name).trim();
}
const emulator = adbOutput('shell', 'getprop', 'ro.kernel.qemu').trim() === '1';

const component = 'org.graphiks.webgpu.benchmarks.runner.test/androidx.test.runner.AndroidJUnitRunner';
const calibrationPath = optionValue('--calibration');
const instrumentArgs = [
  'shell', 'am', 'instrument', '-w', '-r',
  '-e', 'profile', profile,
  '-e', 'runIndex', String(runIndex),
];
if (calibrationPath) {
  instrumentArgs.push('-e', 'calibration', calibrationSpec(JSON.parse(await readFile(resolve(calibrationPath), 'utf8'))));
}
if (process.argv.includes('--writers')) instrumentArgs.push('-e', 'writers', '1');
instrumentArgs.push(component);
const instrumentation = adbOutput(...instrumentArgs);

const problems = [];
const match = instrumentation.match(/^INSTRUMENTATION_STATUS: arraybufferReport=(.*)$/m);
let report = null;
if (match == null) {
  problems.push('no arraybufferReport in the instrumentation status stream');
} else {
  try {
    report = JSON.parse(match[1]);
  } catch (failure) {
    problems.push(`could not parse the report: ${failure.message}`);
  }
}
if (report != null) {
  problems.push(...validateReport(report, { profile }).problems);
  problems.push(...validateAllocationClaim(report).problems);
}

const envelope = {
  runner: 'run-arraybuffer-android',
  collectedAt: new Date().toISOString(),
  target: 'android',
  profile,
  runIndex,
  serial: serial ?? devices[0],
  device: {
    model: properties['ro.product.model'],
    sdk: properties['ro.build.version.sdk'],
    release: properties['ro.build.version.release'],
    abi: properties['ro.product.cpu.abi'],
    emulator,
  },
  instrumentation,
  problems,
  report,
};

await mkdir(dirname(output), { recursive: true });
await writeFile(output, `${JSON.stringify(envelope, null, 2)}\n`);

if (problems.length > 0) {
  console.error(`incomplete or failed android campaign (${profile}):`);
  for (const problem of problems) console.error(`- ${problem}`);
  process.exitCode = 1;
} else {
  console.log(`reported ${report.scenarios.length} scenarios on ${envelope.device.model} (${profile}, emulator=${emulator}) -> ${output}`);
}
