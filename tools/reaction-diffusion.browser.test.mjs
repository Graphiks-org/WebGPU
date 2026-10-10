import { after, before, test } from 'node:test';
import assert from 'node:assert/strict';
import { mkdir, readFile } from 'node:fs/promises';
import { join } from 'node:path';
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

// Hooks are confined to the test environment: production exposes no test-only device or RAF state.
function instrumentDevice({ gate = false } = {}) {
  const state = globalThis.__rd = { handles: new Set(), destroyed: 0, pointerId: null, computePasses: 0 };
  const dispatch = GPUComputePassEncoder.prototype.dispatchWorkgroups;
  GPUComputePassEncoder.prototype.dispatchWorkgroups = function (...args) {
    state.computePasses++; return dispatch.apply(this, args);
  };
  const raf = globalThis.requestAnimationFrame.bind(globalThis);
  const cancel = globalThis.cancelAnimationFrame.bind(globalThis);
  globalThis.requestAnimationFrame = callback => {
    const handle = raf(time => { state.handles.delete(handle); callback(time); });
    state.handles.add(handle); return handle;
  };
  globalThis.cancelAnimationFrame = handle => { state.handles.delete(handle); cancel(handle); };
  addEventListener('pointerdown', e => { if (e.target.id === 'reaction-canvas') state.pointerId = e.pointerId; }, true);
  const adapterRequest = navigator.gpu.requestAdapter.bind(navigator.gpu);
  navigator.gpu.requestAdapter = async (...args) => {
    const adapter = await adapterRequest(...args);
    if (!adapter) return adapter;
    const deviceRequest = adapter.requestDevice.bind(adapter);
    adapter.requestDevice = async (...deviceArgs) => {
      const device = await deviceRequest(...deviceArgs);
      state.device = device;
      const destroy = device.destroy.bind(device);
      device.destroy = () => { state.destroyed++; destroy(); };
      if (gate) await new Promise(resolve => { state.release = resolve; });
      return device;
    };
    return adapter;
  };
}

async function demoPage(options = {}) {
  const context = await server.browser.newContext(options.context);
  const page = await context.newPage();
  const errors = [];
  page.on('pageerror', e => errors.push(String(e)));
  await page.addInitScript(instrumentDevice, { gate: options.gate ?? false });
  await page.goto(`${server.origin}/?demo=reaction-diffusion&lang=${options.lang ?? 'fr'}`);
  await page.waitForFunction(() => document.querySelector('#reaction-canvas')
    || document.body.textContent.includes('not available yet'));
  assert.equal(await page.locator('#reaction-canvas').count(), 1, 'Interactive canvas must exist');
  if (!options.gate) await page.locator('[data-ready="true"]').waitFor();
  return { page, context, errors };
}

async function settle(page) {
  await page.evaluate(() => new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve))));
}

test('interactive controls pause, step, reset and select parameters without replacing the device', async () => {
  const { page, context, errors } = await demoPage();
  try {
    assert.equal(await page.locator('#validation').isVisible(), false, 'The validation route must not appear above the demo');
    assert.equal(await page.getByRole('slider', { name: 'Alimentation A' }).count(), 1);
    assert.equal(await page.getByRole('slider', { name: 'Élimination B' }).count(), 1);
    assert.equal(await page.locator('#reaction-step').isEnabled(), false);
    await page.locator('#reaction-pause').click();
    assert.equal(await page.locator('#reaction-pause').textContent(), 'Reprendre');
    assert.equal(await page.locator('#reaction-step').isEnabled(), true);
    await page.locator('#reaction-reset').click(); await settle(page);
    const canvas = page.locator('#reaction-canvas');
    const before = await canvas.screenshot();
    await page.locator('#reaction-step').click(); await settle(page);
    assert.notDeepEqual(await canvas.screenshot(), before);
    assert.equal(await page.locator('#reaction-pause').textContent(), 'Reprendre');
    await page.locator('#reaction-palette').selectOption('Ember');
    await page.locator('#reaction-preset').selectOption('Labyrinth');
    assert.equal(Number(await page.locator('#reaction-feed').inputValue()), 0.029);
    assert.equal(Number(await page.locator('#reaction-kill').inputValue()), 0.057);
    assert.equal(await page.locator('#reaction-palette').inputValue(), 'Ember');
    const initial = await canvas.screenshot();
    await page.locator('#reaction-step').click(); await settle(page);
    await page.locator('#reaction-reset').click(); await settle(page);
    assert.deepEqual(await canvas.screenshot(), initial);
    assert.equal(await page.evaluate(() => __rd.destroyed), 0);
    assert.deepEqual(errors, []);
  } finally { await context.close(); }
});

test('hidden canvas preserves a requested step and resumes rendering after resize', async () => {
  const { page, context, errors } = await demoPage();
  try {
    await page.locator('#reaction-pause').click();
    const canvas = page.locator('#reaction-canvas');
    const before = await canvas.screenshot();
    await canvas.evaluate(c => { c.style.display = 'none'; });
    await page.locator('#reaction-step').click();
    await page.evaluate(() => dispatchEvent(new Event('resize'))); await settle(page);
    await canvas.evaluate(c => { c.style.display = ''; });
    await page.evaluate(() => dispatchEvent(new Event('resize'))); await settle(page);
    assert.notDeepEqual(await canvas.screenshot(), before);
    assert.deepEqual(errors, []);
  } finally { await context.close(); }
});

test('painting in pause captures one pointer, ignores another and clears cancellation', async () => {
  const { page, context, errors } = await demoPage();
  try {
    await page.locator('#reaction-pause').click();
    await page.locator('#reaction-reset').click(); await settle(page);
    const canvas = page.locator('#reaction-canvas');
    const box = await canvas.boundingBox();
    const initial = await canvas.screenshot();
    await page.mouse.move(box.x + box.width / 2, box.y + box.height / 2);
    await page.mouse.down(); await settle(page);
    const painted = await canvas.screenshot();
    assert.notDeepEqual(painted, initial);
    await canvas.dispatchEvent('pointerdown', { pointerId: 999, button: 0, clientX: box.x + 20, clientY: box.y + 20 });
    await canvas.dispatchEvent('pointermove', { pointerId: 999, clientX: box.x + 30, clientY: box.y + 30 });
    await settle(page); assert.deepEqual(await canvas.screenshot(), painted);
    const active = await page.evaluate(() => __rd.pointerId);
    // The window observer saw the synthetic second pointer; get the real mouse pointer from capture.
    const mouseId = await canvas.evaluate(c => [1, 2, 3, 4].find(id => c.hasPointerCapture(id)));
    assert.ok(mouseId !== undefined);
    await canvas.dispatchEvent('pointercancel', { pointerId: mouseId });
    await canvas.dispatchEvent('pointermove', { pointerId: mouseId, clientX: box.x + 50, clientY: box.y + 50 });
    await settle(page); assert.deepEqual(await canvas.screenshot(), painted);
    assert.equal(await canvas.evaluate((c, id) => c.hasPointerCapture(id), mouseId), false);
    assert.equal(active, 999);
    await page.mouse.up();
    // A fresh captured mouse can paint beyond the canvas; coordinates clamp to the grid.
    await page.mouse.move(box.x + box.width / 2, box.y + box.height / 2);
    await page.mouse.down();
    await page.mouse.move(box.x + box.width + 10, box.y + box.height / 2); await settle(page);
    assert.notDeepEqual(await canvas.screenshot(), painted);
    await page.mouse.up();
    assert.deepEqual(errors, []);
  } finally { await context.close(); }
});

test('touch paints on a square mobile canvas without changing pause state', async () => {
  const { page, context, errors } = await demoPage({ lang: 'en', context: { hasTouch: true, viewport: { width: 390, height: 844 } } });
  try {
    await page.locator('#reaction-pause').click();
    await page.locator('#reaction-reset').click(); await settle(page);
    const canvas = page.locator('#reaction-canvas');
    const box = await canvas.boundingBox();
    assert.ok(Math.abs(box.width - box.height) < 1);
    const before = await canvas.screenshot();
    await page.touchscreen.tap(box.x + box.width / 2, box.y + box.height / 2); await settle(page);
    assert.notDeepEqual(await canvas.screenshot(), before);
    assert.equal(await page.locator('#reaction-pause').textContent(), 'Resume');
    assert.equal(await canvas.evaluate(c => getComputedStyle(c).touchAction), 'none');
    assert.deepEqual(errors, []);
  } finally { await context.close(); }
});

test('device loss stops RAF, disables GPU controls and offers reload', async () => {
  const { page, context, errors } = await demoPage();
  try {
    await page.evaluate(() => __rd.device.destroy());
    await page.locator('#reaction-status[data-error="true"]').waitFor();
    assert.equal(await page.locator('#reaction-reload').isVisible(), true);
    for (const id of ['pause', 'step', 'reset', 'preset', 'speed', 'feed', 'kill', 'palette', 'display']) {
      assert.equal(await page.locator(`#reaction-${id}`).isEnabled(), false);
    }
    assert.equal(await page.evaluate(() => __rd.handles.size), 0);
    assert.deepEqual(errors, []);
  } finally { await context.close(); }
});

test('pagehide before device acquisition releases late resources and never starts animation', async () => {
  const { page, context, errors } = await demoPage({ gate: true });
  try {
    await page.waitForFunction(() => !!__rd.release);
    await page.evaluate(() => { dispatchEvent(new Event('pagehide')); __rd.release(); });
    await page.waitForFunction(() => __rd.destroyed > 0);
    assert.equal(await page.locator('[data-ready="true"]').count(), 0);
    assert.equal(await page.evaluate(() => __rd.handles.size), 0);
    assert.deepEqual(errors, []);
  } finally { await context.close(); }
});

test('pagehide during localization loading never creates a late device or animation', async () => {
  const context = await server.browser.newContext();
  const page = await context.newPage();
  const errors = [];
  page.on('pageerror', e => errors.push(String(e)));
  let release;
  const gate = new Promise(ok => { release = ok; });
  let requested;
  const requestSeen = new Promise(ok => { requested = ok; });
  let responded;
  const responseSent = new Promise(ok => { responded = ok; });
  try {
    await page.addInitScript(instrumentDevice, {});
    await page.route('**/demos/reaction-diffusion.fr.json', async route => {
      requested(); await gate;
      const body = await readFile('suite-browser/src/commonMain/resources/demos/reaction-diffusion.fr.json', 'utf8');
      await route.fulfill({ status: 200, contentType: 'application/json', body }); responded();
    });
    await page.goto(`${server.origin}/?demo=reaction-diffusion&lang=fr`);
    await requestSeen;
    await page.evaluate(() => dispatchEvent(new Event('pagehide')));
    release(); await responseSent;
    await page.waitForLoadState('networkidle'); await settle(page);
    assert.equal(await page.locator('#reaction-canvas').count(), 0,
      `A closed bootstrap must not create a late page: ${await page.locator('#demo-root').textContent()}`);
    assert.equal(await page.evaluate(() => !!__rd.device), false, 'A closed bootstrap must not request a device');
    assert.equal(await page.locator('[data-ready="true"]').count(), 0);
    assert.equal(await page.evaluate(() => __rd.handles.size), 0);
    assert.deepEqual(errors, []);
  } finally { release(); await context.close(); }
});

test('CSS-invisible and zero-rendered-geometry canvases preserve pending steps without surface acquisition', async () => {
  const { page, context, errors } = await demoPage();
  try {
    await page.locator('#reaction-pause').click();
    const canvas = page.locator('#reaction-canvas');
    await canvas.evaluate(c => {
      const gpu = c.getContext('webgpu');
      const get = gpu.getCurrentTexture.bind(gpu);
      globalThis.__rd.surfaceAcquisitions = 0;
      gpu.getCurrentTexture = () => { globalThis.__rd.surfaceAcquisitions++; return get(); };
    });
    for (const [property, value] of [['visibility', 'hidden'], ['transform', 'scale(0)']]) {
      await page.locator('#reaction-reset').click(); await settle(page);
      const before = await canvas.screenshot();
      const acquisitions = await page.evaluate(() => __rd.surfaceAcquisitions);
      await canvas.evaluate((c, [property, value]) => { c.style[property] = value; }, [property, value]);
      await page.locator('#reaction-step').click(); await settle(page);
      assert.equal(await page.evaluate(() => __rd.surfaceAcquisitions), acquisitions,
        'Invisible canvas must not acquire a WebGPU surface texture');
      await canvas.evaluate((c, property) => { c.style[property] = ''; }, property);
      await page.evaluate(() => dispatchEvent(new Event('resize'))); await settle(page);
      assert.notDeepEqual(await canvas.screenshot(), before, 'Requested step must survive until the canvas renders');
    }
    assert.deepEqual(errors, []);
  } finally { await context.close(); }
});

test('missing adapter or localized resource is visible and does not start animation', async () => {
  for (const failure of ['adapter', 'texts']) {
    const context = await server.browser.newContext();
    const page = await context.newPage();
    try {
      await page.addInitScript(instrumentDevice, {});
      if (failure === 'adapter') await page.addInitScript(() => { navigator.gpu.requestAdapter = async () => null; });
      else await page.route('**/demos/reaction-diffusion.fr.json', route => route.fulfill({ status: 404, body: 'missing' }));
      await page.goto(`${server.origin}/?demo=reaction-diffusion&lang=fr`);
      await page.locator('#demo-root [data-error="true"]').waitFor();
      assert.ok((await page.locator('#demo-root').textContent()).length > 0);
      assert.equal(await page.evaluate(() => __rd.handles.size), 0);
    } finally { await context.close(); }
  }
});

test('learning view is optional and displays the complete shaders used by the scene', async () => {
  const { page, context, errors } = await demoPage();
  try {
    const lesson = page.locator('#reaction-lesson');
    assert.equal(await lesson.count(), 1);
    assert.equal(await lesson.evaluate(e => e.open), false);
    await lesson.locator('summary').click();
    const source = await readFile('suite-demos/src/commonMain/kotlin/org/graphiks/webgpu/suite/demos/reactiondiffusion/ReactionDiffusionShaders.kt', 'utf8');
    const strings = [...source.matchAll(/"""([\s\S]*?)"""\.trimIndent\(\)/g)].map(m => {
      const lines = m[1].replace(/^\n|\n$/g, '').split('\n');
      const indent = Math.min(...lines.filter(l => l.trim()).map(l => l.match(/^ */)[0].length));
      return lines.map(l => l.slice(indent)).join('\n').trim();
    });
    const displayed = await lesson.locator('pre code').allTextContents();
    assert.deepEqual(displayed, [strings[0] + '\n' + strings[1], strings[0] + '\n' + strings[2], strings[3]]);
    assert.ok((await lesson.textContent()).includes('échange'));
    assert.ok((await lesson.textContent()).includes('pinceau'));
    await page.locator('#reaction-pause').click();
    const palette = await page.locator('#reaction-palette').inputValue();
    const feed = await page.locator('#reaction-feed').inputValue();
    await page.locator('#reaction-display').selectOption('A'); await settle(page);
    const a = await page.locator('#reaction-canvas').screenshot();
    await page.locator('#reaction-display').selectOption('B'); await settle(page);
    assert.notDeepEqual(await page.locator('#reaction-canvas').screenshot(), a);
    assert.equal(await page.locator('#reaction-palette').inputValue(), palette);
    assert.equal(await page.locator('#reaction-feed').inputValue(), feed);
    for (const width of [1440, 390]) {
      await page.setViewportSize({ width, height: 900 });
      assert.equal(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), true,
        'Open lesson must not cause horizontal page overflow');
    }
    assert.deepEqual(errors, []);
  } finally { await context.close(); }
});

test('three presets remain visibly distinct after at least 2000 GPU steps', { timeout: 120000 }, async () => {
  const { page, context, errors } = await demoPage();
  try {
    await page.locator('#reaction-pause').click();
    await page.locator('#reaction-speed').evaluate(e => { e.value = '16'; e.dispatchEvent(new Event('input')); });
    const images = [];
    for (const preset of ['Coral', 'Labyrinth', 'Spots']) {
      await page.locator('#reaction-preset').selectOption(preset);
      const start = await page.evaluate(() => __rd.computePasses);
      await page.locator('#reaction-pause').click();
      await page.waitForFunction(start => __rd.computePasses >= start + 2000, start, { timeout: 30000 });
      await page.locator('#reaction-pause').click();
      const options = {};
      if (process.env.GRAPHIKS_CAPTURE_DIR) {
        await mkdir(process.env.GRAPHIKS_CAPTURE_DIR, { recursive: true });
        options.path = join(process.env.GRAPHIKS_CAPTURE_DIR, `reaction-${preset.toLowerCase()}-2000.png`);
      }
      images.push(await page.locator('#reaction-canvas').screenshot(options));
    }
    assert.notDeepEqual(images[0], images[1]);
    assert.notDeepEqual(images[1], images[2]);
    assert.notDeepEqual(images[0], images[2]);
    assert.deepEqual(errors, []);
  } finally { await context.close(); }
});
