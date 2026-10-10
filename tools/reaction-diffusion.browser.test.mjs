import { after, before, test } from 'node:test';
import assert from 'node:assert/strict';
import { browserTestServer } from './browser-test-server.mjs';

let server;
before(async () => { server = await browserTestServer(process.env.GRAPHIKS_DISTRIBUTION ?? 'suite-browser/build/dist/js/productionExecutable'); });
after(async () => { await server?.close(); });

test('reaction-diffusion publishes three successful GPU checks', { timeout: 150000 }, async () => {
  const page = await server.browser.newPage();
  const errors = [];
  page.on('pageerror', e => errors.push(String(e)));
  try {
    await page.goto(`${server.origin}/?demo=reaction-diffusion&verify=1`);
    // Detect a missing route directly instead of waiting the entire timeout for a nonexistent report.
    await page.waitForFunction(() => typeof globalThis.graphiksDemoReport === 'string'
      || document.querySelector('#demo-root')?.textContent.includes('Unknown demo'), null, { timeout: 120000 });
    const report = await page.evaluate(() => typeof globalThis.graphiksDemoReport === 'string'
      ? JSON.parse(globalThis.graphiksDemoReport) : null);
    assert.ok(report, 'The reaction-diffusion verification route must publish a report');
    assert.equal(report.fatalError ?? null, null);
    assert.deepEqual(report.cases.map(c => c.id), [
      'reaction-diffusion.compute-render-readback', 'reaction-diffusion.pause-step-reset',
      'reaction-diffusion.brush-boundaries',
    ]);
    assert.ok(report.cases.every(c => c.status === 'passed'), JSON.stringify(report));
    assert.deepEqual(errors, []);
  } finally { await page.close(); }
});

test('unknown and conflicting routes fail visibly instead of starting a suite', async () => {
  const page = await server.browser.newPage();
  try {
    await page.goto(`${server.origin}/?demo=missing`);
    await page.waitForFunction(() => document.body.textContent.includes("Unknown demo 'missing'"));
    await page.goto(`${server.origin}/?demo=reaction-diffusion&benchmark=foundations`);
    await page.waitForFunction(() => document.body.textContent.includes("Use either 'demo' or 'benchmark'"));
    assert.equal(await page.evaluate(() => globalThis.graphiksSuiteReport), undefined);
  } finally { await page.close(); }
});
