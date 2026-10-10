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

test('three routes retain all eight cases', () => {
  assert.deepEqual(aggregateDemoReports(DEMO_ROUTES.map(good)).cases.map(c => c.id), [
    'particles.compute-render-readback', 'particles.bounds-pause-reset',
    'reaction-diffusion.compute-render-readback', 'reaction-diffusion.pause-step-reset',
    'reaction-diffusion.brush-boundaries',
    'instanced-city.instances-render-readback', 'instanced-city.depth-occlusion',
    'instanced-city.camera-resize-lifecycle',
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
  assert.throws(() => aggregateDemoReports(DEMO_ROUTES.map(r => ({ ...good(r), buildCommit: r === route ? 'other' : 'abc' }))), /identit/);
});

test('explicit null schema and falsy non-string fatal errors are malformed, not omitted defaults', () => {
  const route = DEMO_ROUTES[0];
  for (const patch of [{ schemaVersion: null }, { fatalError: 0 }, { fatalError: false }, { fatalError: '' }]) {
    assert.ok(validateDemoReport(route, { ...good(route), ...patch }, baseline).length > 0,
      `Must reject ${JSON.stringify(patch)}`);
  }
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
      JSON.stringify({ ...good(DEMO_ROUTES[0]), buildCommit: 'other' }),
      JSON.stringify({ ...good(DEMO_ROUTES[0]), schemaVersion: null, fatalError: 0 }),
      JSON.stringify({ ...good(DEMO_ROUTES[0]), fatalError: false })]) {
      fixture = value; requests.length = 0;
      const result = await collectDemoReports(browser, `http://127.0.0.1:${server.address().port}`, baseline, { timeout: 1000 });
      assert.ok(result.fatalError);
      assert.ok(result.report.fatalError, 'Aggregated report itself must retain the failure diagnostic');
       assert.deepEqual(requests, ['particles', 'reaction-diffusion', 'instanced-city']);
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
    assert.equal(result.report.cases.length, 6);
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
     const reports = Object.fromEntries(DEMO_ROUTES.map(route => [route.name, good(route)]));
     reports['instanced-city'] = { ...reports['instanced-city'], schemaVersion: 2 };
    await writeFile(join(root, 'distribution/index.html'), `<script>
       globalThis.graphiksDemoReport = JSON.stringify(${JSON.stringify(reports)}[new URL(location.href).searchParams.get('demo')]);
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
     assert.ok(envelope.fatalError.includes('instanced-city'));
     assert.equal(envelope.report.cases.length, 8);
     assert.equal(envelope.report.cases.at(-1).id, 'instanced-city.camera-resize-lifecycle');
    assert.ok(stderr.includes('incomplete or failed run'));
  } finally { await rm(root, { recursive: true, force: true }); }
});

test('failed third route preserves the first five cases and diagnostics', { timeout: 30000 }, async () => {
  const browser = await chromium.launch({ headless: true });
  let fixture;
  const server = createServer((req, res) => {
    const name = new URL(req.url, 'http://localhost').searchParams.get('demo');
    const route = DEMO_ROUTES.find(r => r.name === name);
    res.writeHead(200, { 'content-type': 'text/html' });
    const value = name === 'instanced-city' ? fixture : JSON.stringify(good(route));
    res.end(value === undefined ? '' : `<script>globalThis.graphiksDemoReport=${JSON.stringify(value)};</script>`);
  });
  await new Promise(ok => server.listen(0, '127.0.0.1', ok));
  try {
    const route = DEMO_ROUTES.find(r => r.name === 'instanced-city');
    assert.ok(route, 'City must be a collected route');
    for (const value of [undefined, '{invalid', ...[
      { buildCommit: 'other' }, { cases: [good(route).cases[0],good(route).cases[0]] },
      { cases: good(route).cases.map(c => ({...c,status:'failed'})) },
      { schemaVersion: null }, { fatalError: false }, { fatalError: 0 },
    ].map(patch => JSON.stringify({...good(route),...patch}))]) {
      fixture = value;
      const result = await collectDemoReports(browser, `http://127.0.0.1:${server.address().port}`, baseline, {timeout:250});
      assert.ok(result.fatalError.includes('instanced-city'));
      assert.ok(result.report.fatalError.includes('instanced-city'));
      assert.deepEqual(result.report.cases.slice(0,5), DEMO_ROUTES.slice(0,2).flatMap(r => good(r).cases));
      assert.equal(browser.contexts().length, 0);
    }
  } finally { await browser.close(); await new Promise(ok => server.close(ok)); }
});
