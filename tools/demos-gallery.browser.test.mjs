import { after, before, test } from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { browserTestServer } from './browser-test-server.mjs';

let server;
before(async () => { server = await browserTestServer('site'); });
after(async () => { await server?.close(); });

async function gallery({ missingReport = false, missingReaction = false, hostile = false, lang = 'fr' } = {}) {
  const page = await server.browser.newPage();
  await page.route('**/reports/demos-*.json', route => route.fulfill(missingReport ? { status: 404, body: 'missing' }
    : { json: { schemaVersion: 1, buildCommit: 'fixture-commit', buildVersion: '0.1.0-SNAPSHOT',
      generatedAt: '2026-10-10T00:00:00Z', baseline: {}, selectedCaseIds: null, environment: {},
      report: { schemaVersion: 1, buildCommit: 'fixture-commit', buildVersion: '0.1.0-SNAPSHOT', cases: [], fatalError: null },
      pageErrors: [], fatalError: null } }));
  await page.route('**/run/js/demos/*.json', async route => {
    const name = new URL(route.request().url()).pathname.split('/').at(-1);
    if (missingReaction && name.startsWith('reaction-diffusion')) return route.fulfill({ status: 404, body: 'missing' });
    const dict = JSON.parse(await readFile(`suite-browser/src/commonMain/resources/demos/${name}`, 'utf8'));
    if (hostile) dict.title = '<img src=x onerror="globalThis.injected=true">';
    await route.fulfill({ json: dict });
  });
  await page.goto(`${server.origin}/demos/?lang=${lang}`, { waitUntil: 'networkidle' });
  return page;
}

test('gallery launches both demos on both targets and links sources to the published commit', async () => {
  const page = await gallery();
  try {
    assert.equal(await page.locator('#gallery article').count(), 2);
    for (const demo of ['particles', 'reaction-diffusion']) for (const target of ['js', 'wasm']) {
      assert.equal(await page.locator(`a[href="../run/${target}/?demo=${demo}&lang=fr"]`).count(), 1);
    }
    assert.equal(await page.locator('a[href*="/blob/fixture-commit/"]').count(), 6);
    assert.equal(await page.locator('a[href*="/reactiondiffusion/ReactionDiffusionScene.kt"]').count(), 1);
  } finally { await page.close(); }
});

test('absent published reports produce source-directory links and a visible warning', async () => {
  const page = await gallery({ missingReport: true, lang: 'en' });
  try {
    assert.equal(await page.locator('#gallery article').count(), 2);
    assert.equal(await page.locator('a[href*="/tree/master/"]').count(), 6);
    assert.ok((await page.locator('#gallery').textContent()).includes('No published report'));
  } finally { await page.close(); }
});

test('one missing localized resource keeps the other card and displays a per-demo error', async () => {
  const page = await gallery({ missingReaction: true });
  try {
    assert.equal(await page.locator('a[href="../run/js/?demo=particles&lang=fr"]').count(), 1);
    assert.equal(await page.locator('#gallery [data-demo="reaction-diffusion"] [data-error="true"]').count(), 1);
  } finally { await page.close(); }
});

test('localized text is rendered as text, not executable HTML', async () => {
  const page = await gallery({ hostile: true });
  try {
    assert.ok((await page.locator('#gallery').textContent()).includes('<img src=x'));
    assert.equal(await page.locator('#gallery img').count(), 0);
    assert.equal(await page.evaluate(() => globalThis.injected), undefined);
  } finally { await page.close(); }
});
