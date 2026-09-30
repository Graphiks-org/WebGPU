// Runs the ArrayBuffer CPU campaign distribution in headless Chromium and collects the report
// published in `globalThis.graphiksArrayBufferReport`.
//
// Usage: node tools/run-arraybuffer-benchmarks.mjs <js|wasm> <distribution-directory>
//          [--profile=ci|standard] [--run-index=0] [--output=path]
//
// No GPU is required or created: the campaign measures CPU memory operations only, so the browser
// is launched without the WebGPU flags.
import { execFileSync } from 'node:child_process';
import { createHash } from 'node:crypto';
import { createServer } from 'node:http';
import { mkdir, readFile, readdir, writeFile } from 'node:fs/promises';
import { dirname, extname, isAbsolute, join, normalize, relative, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { chromium } from 'playwright';

import { validateAllocationClaim, validateReport } from './arraybuffer-report.mjs';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const target = process.argv[2];
const distribution = process.argv[3];
const profile = optionValue('--profile') ?? 'ci';
const runIndex = Number.parseInt(optionValue('--run-index') ?? '0', 10);

function optionValue(name) {
  const prefix = `${name}=`;
  const match = process.argv.slice(4).find((argument) => argument.startsWith(prefix));
  return match ? match.slice(prefix.length) : null;
}

if (!['js', 'wasm'].includes(target) || !distribution) {
  console.error('usage: node tools/run-arraybuffer-benchmarks.mjs <js|wasm> <distribution-directory> [--profile=ci|standard] [--run-index=0] [--output=path]');
  process.exit(2);
}
if (!['ci', 'standard'].includes(profile)) {
  console.error(`--profile must be ci or standard, was '${profile}'.`);
  process.exit(2);
}

const distRoot = resolve(distribution);
const output = optionValue('--output')
  ?? join(root, 'build', 'reports', 'arraybuffer', `${target}-cpu-run${runIndex}.json`);

function git(args) {
  try {
    return execFileSync('git', args, { cwd: root, encoding: 'utf8' }).trim();
  } catch {
    return null;
  }
}

async function contentHash(relativeDir) {
  const base = join(root, relativeDir);
  let entries;
  try {
    entries = await readdir(base, { recursive: true, withFileTypes: true });
  } catch {
    return 'unknown';
  }
  const digest = createHash('sha256');
  const files = entries
    .filter((entry) => entry.isFile())
    .map((entry) => join(entry.parentPath ?? entry.path, entry.name))
    .sort();
  for (const file of files) {
    digest.update(relative(root, file));
    digest.update(await readFile(file));
  }
  return digest.digest('hex');
}

const suiteCommit = git(['rev-parse', 'HEAD']) ?? 'unknown';
const dirty = (git(['status', '--porcelain']) ?? '').length > 0;
const harnessHash = await contentHash('arraybuffer-benchmarks/src');
const libraryHash = await contentHash('webgpu-api/src');

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

await new Promise((ok) => server.listen(0, '127.0.0.1', ok));
const { port } = server.address();

const query = new URLSearchParams({
  profile,
  runIndex: String(runIndex),
  target,
  commit: suiteCommit,
  dirty: String(dirty),
  harnessHash,
  libraryHash,
  os: process.platform,
  arch: process.arch,
});
const url = `http://127.0.0.1:${port}/?${query.toString()}`;

let browser = null;
let page = null;
const pageErrors = [];
let fatalError = null;
let report = null;
let browserVersion = 'unknown';
let userAgent = 'unknown';

try {
  browser = await chromium.launch({ headless: true });
  browserVersion = browser.version();
  page = await browser.newPage();
  page.on('pageerror', (error) => pageErrors.push(String(error)));
  // The campaign runs synchronously once the bundle loads, so the page never reaches `load`
  // until it finishes. `commit` lets navigation settle and the report is awaited below.
  await page.goto(url, { waitUntil: 'commit' });
  await page.waitForFunction(
    () => typeof globalThis.graphiksArrayBufferReport === 'string',
    undefined,
    { timeout: 30 * 60 * 1000 },
  );
  report = JSON.parse(await page.evaluate(() => globalThis.graphiksArrayBufferReport));
  userAgent = report?.environment?.browser ?? 'unknown';
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

const problems = [];
if (fatalError) problems.push(`fatal error: ${fatalError.split('\n')[0]}`);
if (pageErrors.length > 0) problems.push(`${pageErrors.length} page error(s)`);
if (report == null) {
  problems.push('no report was published');
} else {
  problems.push(...validateReport(report, { profile }).problems);
  problems.push(...validateAllocationClaim(report).problems);
}

const envelope = {
  runner: 'run-arraybuffer-benchmarks',
  collectedAt: new Date().toISOString(),
  target,
  profile,
  runIndex,
  distribution: distRoot,
  browser: browserVersion,
  userAgent,
  suiteCommit,
  dirty,
  harnessHash,
  libraryHash,
  pageErrors,
  fatalError,
  problems,
  report,
};

await mkdir(dirname(output), { recursive: true });
await writeFile(output, `${JSON.stringify(envelope, null, 2)}\n`);

if (problems.length > 0) {
  console.error(`incomplete or failed ${target} campaign (${profile}):`);
  for (const problem of problems) console.error(`- ${problem}`);
  process.exitCode = 1;
} else {
  console.log(`reported ${report.scenarios.length} scenarios on ${target} (${profile}, ${browserVersion}) -> ${output}`);
}
