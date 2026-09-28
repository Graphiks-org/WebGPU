// Runs the browser suite distributions in headless Chromium and collects the published report.
//
// Usage: node tools/run-browser.mjs <js|wasm> <distribution-directory>
//
// Serves only the given distribution directory on 127.0.0.1 with an automatic port, waits for
// `globalThis.graphiksSuiteReport`, writes the report envelope to build/reports/<target>.json and
// exits non-zero when the run is incomplete or failed.
import { execFileSync } from 'node:child_process';
import { createServer } from 'node:http';
import { mkdir, readFile, writeFile } from 'node:fs/promises';
import { dirname, extname, isAbsolute, join, normalize, relative, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { chromium } from 'playwright';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const target = process.argv[2];
const distribution = process.argv[3];

if (!['js', 'wasm'].includes(target) || !distribution) {
  console.error('usage: node tools/run-browser.mjs <js|wasm> <distribution-directory>');
  process.exit(2);
}

const distRoot = resolve(distribution);
const reportsDir = join(root, 'build', 'reports');
const reportPath = join(reportsDir, `${target}.json`);
const generatedInventory = join(root, 'suite-acid-tests', 'build', 'suite-inventory');
const expectedIds = JSON.parse(
  await readFile(join(generatedInventory, 'foundation-case-ids.json'), 'utf8'),
);
const baseline = JSON.parse(await readFile(join(generatedInventory, 'baseline.json'), 'utf8'));

const suiteCommit = (() => {
  try {
    return execFileSync('git', ['rev-parse', 'HEAD'], { cwd: root, encoding: 'utf8' }).trim();
  } catch {
    return 'unknown';
  }
})();

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

let browser = null;
let page = null;
let pageErrors = [];
let report = null;
let fatalError = null;
const environment = {
  target,
  browser: 'unknown',
  userAgent: 'unknown',
  platform: process.platform,
  requestedBackend: 'swiftshader',
};

await new Promise((ok) => server.listen(0, '127.0.0.1', ok));
const { port } = server.address();
const url = `http://127.0.0.1:${port}/`;

try {
  browser = await chromium.launch({
    headless: true,
    args: [
      '--enable-unsafe-webgpu', '--enable-unsafe-swiftshader',
      '--use-angle=swiftshader', '--disable-dev-shm-usage',
    ],
  });
  environment.browser = browser.version();
  page = await browser.newPage();
  page.on('pageerror', (error) => pageErrors.push(String(error)));

  await page.goto(url);
  environment.userAgent = await page.evaluate(() => navigator.userAgent);
  await page.waitForFunction(() => typeof globalThis.graphiksSuiteReport === 'string', {
  }, { timeout: 420000 });
  report = JSON.parse(await page.evaluate(() => globalThis.graphiksSuiteReport));
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
  suiteCommit,
  generatedAt: new Date().toISOString(),
  environment,
  report,
  pageErrors,
  fatalError,
};

await mkdir(reportsDir, { recursive: true });
await writeFile(reportPath, `${JSON.stringify(envelope, null, 2)}\n`);

const cases = report?.cases ?? [];
const ids = cases.map((entry) => entry.id);
const duplicates = ids.filter((id, index) => ids.indexOf(id) !== index);
const missing = expectedIds.filter((id) => !ids.includes(id));
const unexpected = ids.filter((id) => !expectedIds.includes(id));
const notPassed = cases.filter((entry) => entry.status !== 'passed');

const problems = [];
if (fatalError) problems.push(`fatal error: ${fatalError.split('\n')[0]}`);
if (pageErrors.length > 0) problems.push(`${pageErrors.length} page error(s)`);
if (report?.fatalError) problems.push('the runner reported a fatal error');
if (duplicates.length > 0) problems.push(`duplicate case ids: ${duplicates.join(', ')}`);
if (missing.length > 0) problems.push(`missing case ids: ${missing.join(', ')}`);
if (unexpected.length > 0) problems.push(`unexpected case ids: ${unexpected.join(', ')}`);
if (notPassed.length > 0) {
  problems.push(`cases not passed: ${notPassed.map((entry) => `${entry.id}=${entry.status}`).join(', ')}`);
}

if (problems.length > 0) {
  console.error(`incomplete or failed run (${target}):`);
  for (const problem of problems) console.error(`- ${problem}`);
  process.exitCode = 1;
} else {
  console.log(`reported ${cases.length}/${expectedIds.length} cases passed on ${target} (${environment.browser})`);
}
