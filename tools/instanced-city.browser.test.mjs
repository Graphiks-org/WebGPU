import { after, before, test } from 'node:test';
import assert from 'node:assert/strict';
import { browserTestServer } from './browser-test-server.mjs';

let server;
before(async () => { server = await browserTestServer(process.env.GRAPHIKS_DISTRIBUTION ?? 'suite-browser/build/dist/js/productionExecutable'); });
after(async () => { await server?.close(); });

test('city verification reports three genuine GPU checks', { timeout: 150000 }, async () => {
  const page = await server.browser.newPage();
  const errors = [];
  page.on('pageerror', e => errors.push(String(e)));
  try {
    await page.goto(`${server.origin}/?demo=instanced-city&verify=1`);
    await page.waitForFunction(() => typeof globalThis.graphiksDemoReport === 'string'
      || document.body.textContent.includes("Unknown demo 'instanced-city'"));
    const report = await page.evaluate(() => typeof globalThis.graphiksDemoReport === 'string'
      ? JSON.parse(globalThis.graphiksDemoReport) : null);
    assert.ok(report, 'The city verification route must exist');
    assert.equal(report.fatalError ?? null, null);
    assert.deepEqual(report.cases.map(c => c.id), ['instanced-city.instances-render-readback',
      'instanced-city.depth-occlusion', 'instanced-city.camera-resize-lifecycle']);
    assert.ok(report.cases.every(c => c.status === 'passed'), JSON.stringify(report));
    assert.deepEqual(errors, []);
  } finally { await page.close(); }
});
