import { test } from 'node:test';
import assert from 'node:assert/strict';
import { createServer } from 'node:http';
import { cp, mkdir, mkdtemp, readFile, rm, symlink, writeFile } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join, resolve } from 'node:path';
import { spawn } from 'node:child_process';
import { chromium } from 'playwright';
import { DEMO_ROUTES, aggregateDemoReports, collectDemoReports, validateDemoReport } from './demo-reports.mjs';

const baseline = { commit: 'abc', suiteVersion: '0.1.0-SNAPSHOT' };
const good = route => ({ schemaVersion: 1, buildCommit: 'abc', buildVersion: baseline.suiteVersion,
  cases: route.ids.map(id => ({ id, status: 'passed' })), fatalError: null });

test('both routes retain all five cases', () => {
  assert.deepEqual(aggregateDemoReports(DEMO_ROUTES.map(good)).cases.map(c => c.id), [
    'particles.compute-render-readback', 'particles.bounds-pause-reset',
    'reaction-diffusion.compute-render-readback', 'reaction-diffusion.pause-step-reset',
    'reaction-diffusion.brush-boundaries',
  ]);
});
test('accepts omitted default fields in the existing Kotlin JSON', () => {
  const report = good(DEMO_ROUTES[0]); delete report.schemaVersion; delete report.fatalError;
  assert.deepEqual(validateDemoReport(DEMO_ROUTES[0], report, baseline), []);
});
test('rejects incomplete, duplicate, unexpected and failed cases', () => {
  const route = DEMO_ROUTES[1];
  for (const cases of [[], [good(route).cases[0], good(route).cases[0]],
    [...good(route).cases, { id: 'extra', status: 'passed' }],
    good(route).cases.map(c => ({ ...c, status: 'unsupported' })),
    good(route).cases.map(c => ({ ...c, status: 'failed' })), [null]]) {
    assert.ok(validateDemoReport(route, { ...good(route), cases }, baseline).length > 0);
  }
});
test('rejects null, malformed structure, fatal errors, schema and revision mismatches', () => {
  const route = DEMO_ROUTES[0];
  for (const report of [null, {}, { ...good(route), cases: {} }, { ...good(route), fatalError: 'lost' },
    { ...good(route), schemaVersion: 2 }, { ...good(route), buildCommit: 'other' },
    { ...good(route), buildVersion: 'other' }]) {
    assert.ok(validateDemoReport(route, report, baseline).length > 0);
  }
  assert.throws(() => aggregateDemoReports([good(route)]), /Incomplete/);
  assert.throws(() => aggregateDemoReports([good(route), { ...good(DEMO_ROUTES[1]), buildCommit: 'other' }]), /identit/);
});

test('collector keeps failures and still visits the second route using fresh pages', { timeout: 30000 }, async () => {
  const browser = await chromium.launch({ headless: true });
  let fixture;
  const requests = [];
  const server = createServer((req, res) => {
    const name = new URL(req.url, 'http://localhost').searchParams.get('demo');
    if (!name) { res.end(''); return; }
    requests.push(name);
    const route = DEMO_ROUTES.find(r => r.name === name);
    const json = name === 'particles' ? fixture : JSON.stringify(good(route));
    res.writeHead(200, { 'content-type': 'text/html' });
    res.end(`<script>globalThis.graphiksDemoReport=${JSON.stringify(json)};</script>`);
  });
  await new Promise(ok => server.listen(0, '127.0.0.1', ok));
  try {
    for (const value of ['{invalid', JSON.stringify({ ...good(DEMO_ROUTES[0]), cases: [] }),
      JSON.stringify({ ...good(DEMO_ROUTES[0]), buildCommit: 'other' })]) {
      fixture = value; requests.length = 0;
      const result = await collectDemoReports(browser, `http://127.0.0.1:${server.address().port}`, baseline, { timeout: 1000 });
      assert.ok(result.fatalError);
      assert.deepEqual(requests, ['particles', 'reaction-diffusion']);
      assert.ok(result.report.cases.some(c => c.id === 'reaction-diffusion.brush-boundaries'));
      assert.equal(browser.contexts().length, 0, 'all pages/implicit contexts must be closed');
    }
  } finally {
    await browser.close();
    await new Promise(ok => server.close(ok));
  }
});

test('collector records missing reports and page errors instead of claiming success', { timeout: 30000 }, async () => {
  const browser = await chromium.launch({ headless: true });
  const server = createServer((req, res) => {
    const name = new URL(req.url, 'http://localhost').searchParams.get('demo');
    res.writeHead(200, { 'content-type': 'text/html' });
    const route = DEMO_ROUTES.find(r => r.name === name);
    res.end(name === 'particles' ? '<script>throw new Error("fixture page error")</script>'
      : `<script>globalThis.graphiksDemoReport=${JSON.stringify(JSON.stringify(good(route)))};</script>`);
  });
  await new Promise(ok => server.listen(0, '127.0.0.1', ok));
  try {
    const result = await collectDemoReports(browser, `http://127.0.0.1:${server.address().port}`, baseline, { timeout: 250 });
    assert.ok(result.fatalError);
    assert.ok(result.pageErrors.some(e => e.includes('particles') && e.includes('fixture page error')));
    assert.equal(result.report.cases.length, 3);
  } finally { await browser.close(); await new Promise(ok => server.close(ok)); }
});

test('CLI exits nonzero and writes a failed envelope without losing the other route', { timeout: 30000 }, async () => {
  // Run in an isolated fixture root, never putting synthetic results in the repository's reports.
  const root = await mkdtemp(join(tmpdir(), 'graphiks-demo-collector-'));
  try {
    await mkdir(join(root, 'tools'));
    await mkdir(join(root, 'suite-acid-tests/build/suite-inventory'), { recursive: true });
    await mkdir(join(root, 'distribution'));
    for (const file of ['run-browser.mjs', 'demo-reports.mjs']) await cp(`tools/${file}`, join(root, 'tools', file));
    await symlink(resolve('tools/node_modules'), join(root, 'tools/node_modules'), 'dir');
    await writeFile(join(root, 'suite-acid-tests/build/suite-inventory/baseline.json'), JSON.stringify(baseline));
    const reaction = good(DEMO_ROUTES[1]);
    const malformed = { ...good(DEMO_ROUTES[0]), schemaVersion: 2 };
    await writeFile(join(root, 'distribution/index.html'), `<script>
      globalThis.graphiksDemoReport = JSON.stringify(new URL(location.href).searchParams.get('demo') === 'particles'
        ? ${JSON.stringify(malformed)} : ${JSON.stringify(reaction)});
    </script>`);
    let stderr = '';
    const code = await new Promise((ok, fail) => {
      const child = spawn(process.execPath, ['tools/run-browser.mjs', 'js', 'distribution', '--demo-check'], { cwd: root });
      child.stderr.on('data', chunk => { stderr += chunk; });
      child.stdout.resume(); child.on('error', fail); child.on('close', ok);
    });
    assert.equal(code, 1, stderr);
    const envelope = JSON.parse(await readFile(join(root, 'build/reports/demos-js.json'), 'utf8'));
    assert.ok(envelope.fatalError.includes('schemaVersion'));
    assert.equal(envelope.report.cases.length, 5);
    assert.equal(envelope.report.cases.at(-1).id, 'reaction-diffusion.brush-boundaries');
    assert.ok(stderr.includes('incomplete or failed run'));
  } finally { await rm(root, { recursive: true, force: true }); }
});
