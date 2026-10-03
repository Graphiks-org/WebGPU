// Runs the browser suite distributions in headless Chromium and collects the published report.
//
// Usage: node tools/run-browser.mjs <js|wasm> <distribution-directory> [--demo-check | --benchmark]
//                                     [--cases=id1,id2] [--profile=ci|standard]
//                                     [--backend=swiftshader|default]
//
// Serves only the given distribution directory on 127.0.0.1 with an automatic port, waits for the
// published report, writes the envelope to build/reports/<target>.json — demos-<target>.json with
// --demo-check, benchmarks-<target>.json with --benchmark — and exits non-zero when the run is
// incomplete or failed. --demo-check opens the particle verification route; --benchmark opens
// ?benchmark=foundations&profile=<profile>&autorun=1 and expects `globalThis.graphiksBenchmarkReport`.
// The three modes keep their own report and their own launch guard.
//
// --backend=swiftshader (the default) forces the software backend with explicit flags and is what
// CI uses; --backend=default lets Chromium choose without the SwiftShader flags and the report says
// `default`, never `hardware`.
import { createServer } from 'node:http';
import { mkdir, readFile, writeFile } from 'node:fs/promises';
import { dirname, extname, isAbsolute, join, normalize, relative, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { chromium } from 'playwright';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const target = process.argv[2];
const distribution = process.argv[3];
const demoCheck = process.argv.includes('--demo-check');
const benchmark = process.argv.includes('--benchmark');
const profile = optionValue('--profile') ?? 'ci';
const backend = optionValue('--backend') ?? 'swiftshader';
const casesOption = optionValue('--cases');

function optionValue(name) {
  const prefix = `${name}=`;
  const match = process.argv.slice(4).find((argument) => argument.startsWith(prefix));
  return match ? match.slice(prefix.length) : null;
}

if (!['js', 'wasm'].includes(target) || !distribution) {
  console.error('usage: node tools/run-browser.mjs <js|wasm> <distribution-directory> [--demo-check | --benchmark] [--cases=id1,id2] [--profile=ci|standard] [--backend=swiftshader|default]');
  process.exit(2);
}
if (demoCheck && benchmark) {
  console.error('--demo-check and --benchmark are mutually exclusive.');
  process.exit(2);
}
if (benchmark && !['ci', 'standard'].includes(profile)) {
  console.error(`--profile must be ci or standard, was '${profile}'.`);
  process.exit(2);
}
if (!['swiftshader', 'default'].includes(backend)) {
  console.error(`--backend must be swiftshader or default, was '${backend}'.`);
  process.exit(2);
}

const mode = benchmark ? 'benchmark' : demoCheck ? 'demo' : 'suite';
if (casesOption != null && mode !== 'suite') {
  console.error('--cases is only valid for the acid-test suite, not with --demo-check or --benchmark.');
  process.exit(2);
}
const profileSamples = { ci: 5, standard: 30 };

// The ten foundations-v1 scenario ids, in their published order. Kept here so the collector checks
// the report against the protocol instead of trusting whatever it received.
function benchmarkExpectedIds() {
  const ids = [];
  for (const size of [4096, 65536, 1048576]) {
    for (const batch of [1, 16]) ids.push(`transfer.write-buffer.bytes-${size}.batch-${batch}`);
  }
  for (const size of [1024, 65536]) {
    for (const batch of [1, 16]) ids.push(`compute.encode-submit.elements-${size}.batch-${batch}`);
  }
  return ids;
}

const distRoot = resolve(distribution);
const reportsDir = join(root, 'build', 'reports');
const generatedInventory = join(root, 'suite-acid-tests', 'build', 'suite-inventory');
const allCaseIds = mode === 'suite'
  ? JSON.parse(await readFile(join(generatedInventory, 'foundation-case-ids.json'), 'utf8'))
  : [];
const declaredFeaturesById = mode === 'suite'
  ? new Map(
    JSON.parse(await readFile(join(generatedInventory, 'cases.json'), 'utf8'))
      .map((entry) => [entry.id, entry.requiredFeatures ?? []]),
  )
  : new Map();

// The acid suite accepts an explicit subset for targeted development. An unknown or empty
// selection fails before a browser is launched; a targeted run keeps its own report so it never
// replaces the full catalogue's `<target>.json`.
const selectedCaseIds = (() => {
  if (casesOption == null) return null;
  const requested = casesOption.split(',').map((id) => id.trim()).filter((id) => id.length > 0);
  if (requested.length === 0) {
    console.error('--cases must select at least one case id.');
    process.exit(2);
  }
  const unknown = requested.filter((id) => !allCaseIds.includes(id));
  if (unknown.length > 0) {
    console.error(`--cases names unknown case id(s): ${unknown.join(', ')}`);
    process.exit(2);
  }
  return [...new Set(requested)];
})();

const reportName = mode === 'benchmark'
  ? `benchmarks-${target}.json`
  : mode === 'demo'
    ? `demos-${target}.json`
    : selectedCaseIds == null ? `${target}.json` : `selected-${target}.json`;
const reportPath = join(reportsDir, reportName);
const expectedIds = mode === 'benchmark'
  ? benchmarkExpectedIds()
  : mode === 'demo'
    ? ['particles.compute-render-readback', 'particles.bounds-pause-reset']
    : selectedCaseIds == null
      ? allCaseIds
      : allCaseIds.filter((id) => selectedCaseIds.includes(id));
const baseline = JSON.parse(await readFile(join(generatedInventory, 'baseline.json'), 'utf8'));

const MIME = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'text/javascript; charset=utf-8',
  '.mjs': 'text/javascript; charset=utf-8',
  '.wasm': 'application/wasm',
  '.json': 'application/json; charset=utf-8',
  '.map': 'application/json; charset=utf-8',
};

const server = createServer(async (request, response) => {
  try {
    const pathname = decodeURIComponent(new URL(request.url, 'http://127.0.0.1').pathname);
    const requested = pathname === '/' ? '/index.html' : pathname;
    const filePath = normalize(join(distRoot, requested));
    const relativePath = relative(distRoot, filePath);
    if (relativePath.startsWith('..') || isAbsolute(relativePath)) {
      response.writeHead(403);
      response.end('forbidden');
      return;
    }
    const body = await readFile(filePath);
    response.writeHead(200, { 'content-type': MIME[extname(filePath)] ?? 'application/octet-stream' });
    response.end(body);
  } catch {
    response.writeHead(404);
    response.end('not found');
  }
});

const urlQuery = mode === 'benchmark'
  ? `?benchmark=foundations&profile=${profile}&autorun=1`
  : mode === 'demo'
    ? '?demo=particles&verify=1'
    : selectedCaseIds == null ? '' : `?cases=${encodeURIComponent(selectedCaseIds.join(','))}`;
const reportGlobal = mode === 'benchmark'
  ? 'graphiksBenchmarkReport'
  : mode === 'demo' ? 'graphiksDemoReport' : 'graphiksSuiteReport';
// 30 s per case plus a minute of slack, capped at one hour: a larger budget never turns a stopped
// case into a success, it only lets a long catalogue finish.
const caseTimeoutMs = 30000;
const waitTimeout = mode === 'benchmark'
  ? 17 * 60 * 1000
  : mode === 'demo'
    ? 120000
    : Math.min(expectedIds.length * caseTimeoutMs + 60000, 60 * 60 * 1000);
const launchArgs = backend === 'swiftshader'
  ? ['--enable-unsafe-webgpu', '--enable-unsafe-swiftshader', '--use-angle=swiftshader', '--disable-dev-shm-usage']
  : ['--enable-unsafe-webgpu', '--disable-dev-shm-usage'];

let browser = null;
let page = null;
let pageErrors = [];
let report = null;
let fatalError = null;
const environment = {
  target,
  mode,
  demoCheck,
  benchmark,
  profile: mode === 'benchmark' ? profile : null,
  backend,
  requestedBackend: backend,
  headless: true,
  launchArgs,
  browser: 'unknown',
  userAgent: 'unknown',
  platform: process.platform,
};

await new Promise((ok) => server.listen(0, '127.0.0.1', ok));
const { port } = server.address();
const url = `http://127.0.0.1:${port}/${urlQuery}`;

try {
  browser = await chromium.launch({ headless: true, args: launchArgs });
  environment.browser = browser.version();
  page = await browser.newPage();
  page.on('pageerror', (error) => pageErrors.push(String(error)));

  await page.goto(url);
  environment.userAgent = await page.evaluate(() => navigator.userAgent);
  await page.waitForFunction(
    (name) => typeof globalThis[name] === 'string',
    reportGlobal,
    { timeout: waitTimeout },
  );
  report = JSON.parse(await page.evaluate((name) => globalThis[name], reportGlobal));
} catch (failure) {
  fatalError = String(failure && failure.stack ? failure.stack : failure);
} finally {
  try {
    if (page) await page.close();
  } catch (failure) {
    fatalError = fatalError ?? String(failure);
  }
  try {
    if (browser) await browser.close();
  } catch (failure) {
    fatalError = fatalError ?? String(failure);
  }
  server.close();
}

const envelope = {
  schemaVersion: 1,
  baseline,
  buildCommit: report?.buildCommit ?? null,
  buildVersion: report?.buildVersion ?? null,
  generatedAt: new Date().toISOString(),
  selectedCaseIds,
  environment,
  report,
  pageErrors,
  fatalError,
};

await mkdir(reportsDir, { recursive: true });
await writeFile(reportPath, `${JSON.stringify(envelope, null, 2)}\n`);

const problems = [];
if (fatalError) problems.push(`fatal error: ${fatalError.split('\n')[0]}`);
if (pageErrors.length > 0) problems.push(`${pageErrors.length} page error(s)`);
if (report?.fatalError) problems.push('the runner reported a fatal error');

// The distribution must carry the revision it was built from, and that revision must match the
// inventory its expectations come from. A missing identity is a failure, never a checkout HEAD.
const buildCommit = report?.buildCommit;
const buildVersion = report?.buildVersion;
if (typeof buildCommit !== 'string' || buildCommit.length === 0) {
  problems.push('report is missing buildCommit (distribution identity not embedded)');
}
if (typeof buildVersion !== 'string' || buildVersion.length === 0) {
  problems.push('report is missing buildVersion (distribution identity not embedded)');
}
if (buildCommit !== baseline.commit) {
  problems.push(`distribution commit ${buildCommit} does not match inventory commit ${baseline.commit}`);
}
if (buildVersion !== baseline.suiteVersion) {
  problems.push(`distribution version ${buildVersion} does not match inventory version ${baseline.suiteVersion}`);
}

if (mode === 'benchmark') {
  const scenarios = report?.scenarios ?? [];
  const ids = scenarios.map((entry) => entry.id);
  const duplicates = ids.filter((id, index) => ids.indexOf(id) !== index);
  const missing = expectedIds.filter((id) => !ids.includes(id));
  const unexpected = ids.filter((id) => !expectedIds.includes(id));
  const notCompleted = scenarios.filter((entry) => entry.status !== 'completed');
  const unverified = scenarios.filter((entry) => entry.outputVerified !== true);
  const wrongSamples = scenarios.filter(
    (entry) => entry.status === 'completed' && (entry.samples?.length ?? 0) !== profileSamples[profile],
  );
  if (report?.protocol !== 'foundations-v1') problems.push(`unexpected protocol: ${report?.protocol}`);
  if (report?.profile !== profile) problems.push(`unexpected profile: ${report?.profile}`);
  if (scenarios.length !== expectedIds.length) problems.push(`scenarios: ${scenarios.length}/${expectedIds.length}`);
  if (duplicates.length > 0) problems.push(`duplicate scenario ids: ${duplicates.join(', ')}`);
  if (missing.length > 0) problems.push(`missing scenario ids: ${missing.join(', ')}`);
  if (unexpected.length > 0) problems.push(`unexpected scenario ids: ${unexpected.join(', ')}`);
  if (notCompleted.length > 0) {
    problems.push(`scenarios not completed: ${notCompleted.map((entry) => `${entry.id}=${entry.status}`).join(', ')}`);
  }
  if (unverified.length > 0) {
    problems.push(`scenarios without a verified GPU output: ${unverified.map((entry) => entry.id).join(', ')}`);
  }
  if (wrongSamples.length > 0) {
    problems.push(`scenarios with the wrong sample count: ${wrongSamples.map((entry) => entry.id).join(', ')}`);
  }
} else {
  const cases = report?.cases ?? [];
  const ids = cases.map((entry) => entry.id);
  const duplicates = ids.filter((id, index) => ids.indexOf(id) !== index);
  const missing = expectedIds.filter((id) => !ids.includes(id));
  const unexpected = ids.filter((id) => !expectedIds.includes(id));
  if (duplicates.length > 0) problems.push(`duplicate case ids: ${duplicates.join(', ')}`);
  if (missing.length > 0) problems.push(`missing case ids: ${missing.join(', ')}`);
  if (unexpected.length > 0) problems.push(`unexpected case ids: ${unexpected.join(', ')}`);
  // `unsupported` is accepted only for a declared optional feature: the case must declare
  // features and every reported missing one must fall within that declaration. Anything else —
  // failed, not-run, or an undeclared "unsupported" — stays a real failure, never a silent skip.
  const notPassed = cases.filter((entry) => {
    if (entry.status === 'passed') return false;
    if (entry.status !== 'unsupported') return true;
    const declared = declaredFeaturesById.get(entry.id) ?? [];
    const reported = entry.missingFeatures ?? [];
    return declared.length === 0
      || reported.length === 0
      || !reported.every((feature) => declared.includes(feature));
  });
  if (notPassed.length > 0) {
    problems.push(`cases not passed: ${notPassed.map((entry) => `${entry.id}=${entry.status}`).join(', ')}`);
  }
}

if (problems.length > 0) {
  console.error(`incomplete or failed run (${target}${mode === 'benchmark' ? `, ${mode}/${profile}/${backend}` : ''}):`);
  for (const problem of problems) console.error(`- ${problem}`);
  process.exitCode = 1;
} else if (mode === 'suite') {
  const cases = report?.cases ?? [];
  const passed = cases.filter((entry) => entry.status === 'passed').length;
  const unsupported = cases.filter((entry) => entry.status === 'unsupported').length;
  console.log(
    `reported ${passed} passed and ${unsupported} unsupported of ${expectedIds.length} acid cases on ${target} (${environment.browser})`,
  );
} else {
  const label = mode === 'benchmark'
    ? `benchmark scenarios passed on ${target} (${profile}, ${backend}, ${environment.browser})`
    : `demo cases passed on ${target} (${environment.browser})`;
  const count = mode === 'benchmark' ? (report?.scenarios?.length ?? 0) : (report?.cases?.length ?? 0);
  console.log(`reported ${count}/${expectedIds.length} ${label}`);
}
