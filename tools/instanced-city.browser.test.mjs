import { after, before, test } from 'node:test';
import assert from 'node:assert/strict';
import { mkdir, readFile } from 'node:fs/promises';
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

function instrumentCity({ gateDevice = false } = {}) {
  const state = globalThis.__city = { handles: new Set(), destroyedDevices: 0, destroyedBuffers: 0,
    acquisitions: 0, failAfter: null, failDepth:false, scopedOom:false };
  const raf = requestAnimationFrame.bind(globalThis), cancel = cancelAnimationFrame.bind(globalThis);
  globalThis.requestAnimationFrame = callback => {
    const handle = raf(time => { state.handles.delete(handle); callback(time); });
    state.handles.add(handle); return handle;
  };
  globalThis.cancelAnimationFrame = handle => { state.handles.delete(handle); cancel(handle); };
  const get = GPUCanvasContext.prototype.getCurrentTexture;
  GPUCanvasContext.prototype.getCurrentTexture = function () { state.acquisitions++; return get.call(this); };
  const request = navigator.gpu.requestAdapter.bind(navigator.gpu);
  navigator.gpu.requestAdapter = async (...args) => {
    const adapter = await request(...args);
    if (!adapter) return adapter;
    const acquire = adapter.requestDevice.bind(adapter);
    adapter.requestDevice = async (...args) => {
      const device = await acquire(...args); state.device = device;
      const destroy = device.destroy.bind(device);
      device.destroy = () => { state.destroyedDevices++; destroy(); };
      const createBuffer = device.createBuffer.bind(device);
      const scopes=[], push=device.pushErrorScope.bind(device), pop=device.popErrorScope.bind(device);
      device.pushErrorScope = filter => { scopes.push(filter); push(filter); };
      device.popErrorScope = async () => {
        const filter=scopes.pop(), error=await pop();
        if (state.scopedOom && filter === 'out-of-memory') {
          state.scopedOom=false; return new GPUOutOfMemoryError('fixture scoped allocation failure');
        }
        return error;
      };
      const createTexture = device.createTexture.bind(device);
      device.createTexture = (...args) => {
        if (state.failDepth && args[0].format === 'depth24plus') {
          state.failDepth = false;
          throw new Error('fixture depth allocation failed');
        }
        return createTexture(...args);
      };
      device.createBuffer = (...args) => {
        if (state.failAfter !== null && state.failAfter-- === 0) {
          state.failAfter = null; throw new Error('fixture candidate allocation failed');
        }
        const buffer = createBuffer(...args), destroy = buffer.destroy.bind(buffer);
        buffer.destroy = () => { state.destroyedBuffers++; destroy(); };
        return buffer;
      };
      if (gateDevice) await new Promise(ok => { state.releaseDevice = ok; });
      return device;
    };
    return adapter;
  };
}

async function cityPage({ lang = 'fr', contextOptions = {}, gateDevice = false } = {}) {
  const context = await server.browser.newContext({ reducedMotion: 'reduce', ...contextOptions });
  const page = await context.newPage();
  const errors = [];
  page.on('pageerror', e => errors.push(String(e)));
  await page.addInitScript(instrumentCity, { gateDevice });
  await page.goto(`${server.origin}/?demo=instanced-city&lang=${lang}`);
  await page.waitForFunction(() => document.querySelector('#city-canvas') || document.body.textContent.includes('not available yet'));
  assert.equal(await page.locator('#city-canvas').count(), 1, 'Interactive city canvas must exist');
  if (!gateDevice) await page.locator('[data-ready="true"]').waitFor();
  return { context, page, errors };
}
async function settle(page) { await page.evaluate(() => new Promise(ok => requestAnimationFrame(() => requestAnimationFrame(ok)))); }
async function range(page, id, value) {
  await page.locator(`#city-${id}`).evaluate((e, value) => { e.value = value; e.dispatchEvent(new Event('input')); }, String(value));
  await settle(page);
}

test('city controls redraw without replacing device and reset only the camera', async () => {
  const { context, page, errors } = await cityPage();
  try {
    assert.equal(await page.locator('#validation').isVisible(), false);
    assert.equal(await page.locator('#city-rotation').isChecked(), false);
    assert.equal(await page.getByRole('slider', { name: 'Hauteur', exact: true }).count(), 1);
    const initial = await page.locator('#city-canvas').screenshot();
    await range(page, 'height', 2); await range(page, 'distance', 100);
    await page.locator('#city-reset-view').click(); await settle(page);
    assert.equal(await page.locator('#city-height').inputValue(), '2');
    assert.equal(await page.locator('#city-distance').inputValue(), '180');
    assert.notDeepEqual(await page.locator('#city-canvas').screenshot(), initial);
    assert.equal(await page.evaluate(() => __city.destroyedDevices), 0);
    assert.deepEqual(errors, []);
  } finally { await context.close(); }
});

test('density roundtrip is reproducible and a failed replacement preserves the current scene', async () => {
  const { context, page, errors } = await cityPage();
  try {
    const canvas = page.locator('#city-canvas'), select = page.locator('#city-density');
    assert.equal(await select.inputValue(), 'Dense');
    const initial = await canvas.screenshot();
    await select.selectOption('Sparse'); await page.waitForFunction(() => !document.querySelector('#city-density').disabled); await settle(page);
    assert.notDeepEqual(await canvas.screenshot(), initial);
    await select.selectOption('Dense'); await page.waitForFunction(() => !document.querySelector('#city-density').disabled); await settle(page);
    assert.deepEqual(await canvas.screenshot(), initial);
    const freed = await page.evaluate(() => __city.destroyedBuffers);
    await page.evaluate(() => { __city.failAfter = 2; });
    await select.selectOption('Sparse');
    await page.locator('#city-status[data-error="true"]').waitFor();
    assert.equal(await select.inputValue(), 'Dense');
    assert.equal(await select.isEnabled(), true);
    assert.deepEqual(await canvas.screenshot(), initial);
    assert.equal(await page.evaluate(() => __city.destroyedDevices), 0);
    assert.equal(await page.evaluate(freed => __city.destroyedBuffers - freed, freed), 2, 'partially created candidate buffers are freed');
    await range(page, 'height', 2); assert.notDeepEqual(await canvas.screenshot(), initial);
    assert.deepEqual(errors, []);
  } finally { await context.close(); }
});

test('mouse orbit captures one pointer, ignores another, cancels and resets the view', async () => {
  const { context, page, errors } = await cityPage();
  try {
    const canvas = page.locator('#city-canvas'), box = await canvas.boundingBox();
    const initial = await canvas.screenshot();
    await page.mouse.move(box.x + box.width / 2, box.y + box.height / 2); await page.mouse.down();
    await page.mouse.move(box.x + box.width / 2 + 100, box.y + box.height / 2 + 20); await settle(page);
    const moved = await canvas.screenshot(); assert.notDeepEqual(moved, initial);
    const id = await canvas.evaluate(c => [1,2,3,4].find(i => c.hasPointerCapture(i)));
    assert.ok(id !== undefined);
    await canvas.dispatchEvent('pointerdown', { pointerId: 999, button: 0, clientX: 10, clientY: 10 });
    await canvas.dispatchEvent('pointermove', { pointerId: 999, clientX: 500, clientY: 500 }); await settle(page);
    assert.deepEqual(await canvas.screenshot(), moved);
    await canvas.dispatchEvent('pointercancel', { pointerId: id });
    assert.equal(await canvas.evaluate((c,id) => c.hasPointerCapture(id), id), false);
    await canvas.dispatchEvent('pointermove', { pointerId: id, clientX: 1e6, clientY: -1e6 }); await settle(page);
    assert.deepEqual(await canvas.screenshot(), moved);
    await page.mouse.up(); await page.locator('#city-reset-view').click(); await settle(page);
    assert.deepEqual(await canvas.screenshot(), initial);
    assert.deepEqual(errors, []);
  } finally { await context.close(); }
});

test('real touch orbit works on mobile and distance remains keyboard-accessible', async () => {
  const { context, page, errors } = await cityPage({ lang: 'en', contextOptions: { hasTouch: true, viewport: {width:390,height:844} } });
  try {
    const canvas = page.locator('#city-canvas'), box = await canvas.boundingBox();
    const initial = await canvas.screenshot(), cdp = await context.newCDPSession(page);
    const x = box.x + box.width/2, y = box.y + box.height/2;
    await cdp.send('Input.dispatchTouchEvent', {type:'touchStart',touchPoints:[{x,y,id:1}]});
    await cdp.send('Input.dispatchTouchEvent', {type:'touchMove',touchPoints:[{x:x+50,y:y+20,id:1}]});
    await cdp.send('Input.dispatchTouchEvent', {type:'touchEnd',touchPoints:[]}); await settle(page);
    assert.notDeepEqual(await canvas.screenshot(), initial);
    const distance = page.getByRole('slider', { name: 'Distance', exact: true });
    await distance.focus(); await page.keyboard.press('ArrowRight'); assert.equal(await distance.inputValue(), '181');
    assert.equal(await canvas.evaluate(c => getComputedStyle(c).touchAction), 'none');
    assert.deepEqual(errors, []);
  } finally { await context.close(); }
});

test('wheel delta modes zoom consistently and the rest of the page can scroll', async () => {
  const { context, page, errors } = await cityPage({ contextOptions: { viewport: {width:800,height:500} } });
  try {
    const canvas = page.locator('#city-canvas');
    for (const [deltaY,deltaMode] of [[40,0],[2.5,1],[.1,2]]) {
      await page.locator('#city-reset-view').click(); await settle(page);
      const box = await canvas.boundingBox();
      const delta = deltaMode === 2 ? 40 / box.height : deltaY;
      await canvas.dispatchEvent('wheel', {deltaY:delta,deltaMode,cancelable:true}); await settle(page);
      assert.equal(await page.locator('#city-distance').inputValue(), '184');
    }
    await canvas.hover(); await page.mouse.wheel(40, 40); await settle(page);
    assert.equal(await page.locator('#city-distance').inputValue(), '188');
    await page.mouse.move(10,10); await page.mouse.wheel(0,300);
    await page.waitForFunction(() => scrollY > 0);
    assert.deepEqual(errors, []);
  } finally { await context.close(); }
});

test('invisible and detached canvases never acquire surfaces and retain latest controls', async () => {
  const { context, page, errors } = await cityPage();
  try {
    const canvas = page.locator('#city-canvas');
    for (const [property,value] of [['display','none'],['visibility','hidden'],['transform','scale(0)']]) {
      await range(page, 'height', 1); const initial = await canvas.screenshot();
      const count = await page.evaluate(() => __city.acquisitions);
      await canvas.evaluate((c,[property,value]) => { c.style[property] = value; }, [property,value]);
      await range(page, 'height', 2); assert.equal(await page.evaluate(() => __city.acquisitions), count);
      await canvas.evaluate((c,property) => { c.style[property] = ''; }, property);
      await settle(page);
      assert.notDeepEqual(await canvas.screenshot(), initial);
    }
    const count = await page.evaluate(() => __city.acquisitions);
    await canvas.evaluate(c => { globalThis.detachedCityCanvas = c; c.remove(); });
    await range(page, 'distance', 100); assert.equal(await page.evaluate(() => __city.acquisitions), count);
    await page.evaluate(() => { document.querySelector('.city-demo .demo-stage').append(detachedCityCanvas); });
    await settle(page); assert.ok(await page.evaluate(() => __city.acquisitions > 0));
    assert.deepEqual(errors, []);
  } finally { await context.close(); }
});

test('depth preparation failure rejects only the candidate and preserves the working scene', async () => {
  const {context,page,errors}=await cityPage();
  try {
    const canvas=page.locator('#city-canvas'), initial=await canvas.screenshot();
    const freed=await page.evaluate(() => __city.destroyedBuffers);
    await page.evaluate(() => { __city.failDepth=true; });
    await page.locator('#city-density').selectOption('Sparse');
    await page.locator('#city-status[data-error="true"]').waitFor();
    assert.equal(await page.locator('[data-ready="true"]').count(), 1);
    assert.equal(await page.locator('#city-density').inputValue(), 'Dense');
    assert.equal(await page.locator('#city-height').isEnabled(), true);
    assert.equal(await page.evaluate(freed => __city.destroyedBuffers-freed,freed),4);
    assert.equal(await page.evaluate(() => __city.destroyedDevices),0);
    assert.deepEqual(await canvas.screenshot(),initial);
    await range(page,'height',2); assert.notDeepEqual(await canvas.screenshot(),initial);
    assert.deepEqual(errors,[]);
  } finally { await context.close(); }
});

test('allocation error scope rejects only the density candidate', async () => {
  const {context,page,errors}=await cityPage();
  try {
    await page.evaluate(() => { __city.scopedOom=true; });
    await page.locator('#city-density').selectOption('Sparse');
    await page.locator('#city-status[data-error="true"]').waitFor();
    assert.equal(await page.locator('[data-ready="true"]').count(),1);
    assert.equal(await page.locator('#city-density').inputValue(),'Dense');
    assert.equal(await page.evaluate(() => __city.scopedOom),false,'Candidate consumes local OutOfMemory scope');
    assert.equal(await page.evaluate(() => __city.destroyedDevices),0);
    assert.deepEqual(errors,[]);
  } finally { await context.close(); }
});

test('rotation honors reduced motion but allows explicit activation and stops on device loss', async () => {
  const { context, page, errors } = await cityPage();
  try {
    assert.equal(await page.locator('#city-rotation').isChecked(), false);
    assert.equal(await page.evaluate(() => __city.handles.size), 0);
    const canvas = page.locator('#city-canvas'), before = await canvas.screenshot();
    await page.locator('#city-rotation').check(); await settle(page);
    assert.equal(await page.evaluate(() => __city.handles.size), 1);
    assert.notDeepEqual(await canvas.screenshot(), before);
    await page.evaluate(() => __city.device.destroy());
    await page.locator('#city-status[data-error="true"]').waitFor();
    assert.equal(await page.evaluate(() => __city.handles.size), 0);
    for (const id of ['density','height','rotation','distance','reset-view']) assert.equal(await page.locator(`#city-${id}`).isEnabled(), false);
    assert.equal(await page.locator('#city-reload').isVisible(), true);
    assert.equal(await page.locator('#demo-selector').isEnabled(), true);
    assert.deepEqual(errors, []);
  } finally { await context.close(); }
});

test('pagehide during device acquisition destroys the late device exactly once', async () => {
  const { context, page, errors } = await cityPage({gateDevice:true});
  try {
    await page.waitForFunction(() => !!__city.releaseDevice);
    await page.evaluate(() => { dispatchEvent(new Event('pagehide')); __city.releaseDevice(); });
    await page.waitForFunction(() => __city.destroyedDevices === 1);
    assert.equal(await page.locator('[data-ready="true"]').count(), 0);
    assert.equal(await page.evaluate(() => __city.handles.size), 0);
    assert.deepEqual(errors, []);
  } finally { await context.close(); }
});

test('pagehide during text loading creates no late DOM or device', async () => {
  const context = await server.browser.newContext(), page = await context.newPage();
  let release; const gate = new Promise(ok => { release = ok; });
  let seen; const request = new Promise(ok => { seen = ok; });
  try {
    await page.addInitScript(instrumentCity, {});
    await page.route('**/demos/instanced-city.fr.json', async route => {
      seen(); await gate;
      await route.fulfill({json: JSON.parse(await readFile('suite-browser/src/commonMain/resources/demos/instanced-city.fr.json', 'utf8'))});
    });
    await page.goto(`${server.origin}/?demo=instanced-city&lang=fr`); await request;
    await page.evaluate(() => dispatchEvent(new Event('pagehide'))); release();
    await page.waitForLoadState('networkidle'); await settle(page);
    assert.equal(await page.locator('#city-canvas').count(), 0);
    assert.equal(await page.evaluate(() => !!__city.device), false);
    assert.equal(await page.evaluate(() => __city.handles.size), 0);
  } finally { release(); await context.close(); }
});

test('missing WebGPU or text resource fails visibly without animation', async () => {
  for (const failure of ['adapter','texts']) {
    const context = await server.browser.newContext(), page = await context.newPage();
    try {
      await page.addInitScript(instrumentCity, {});
      if (failure === 'adapter') await page.addInitScript(() => { navigator.gpu.requestAdapter = async () => null; });
      else await page.route('**/demos/instanced-city.fr.json', r => r.fulfill({status:404,body:'missing'}));
      await page.goto(`${server.origin}/?demo=instanced-city&lang=fr`);
      await page.locator('#demo-root [data-error="true"]').waitFor();
      assert.equal(await page.evaluate(() => __city.handles.size), 0);
    } finally { await context.close(); }
  }
});

test('teaching panel shows actual shaders, correct draw counts and fits mobile', async () => {
  const { context, page, errors } = await cityPage();
  try {
    const lesson = page.locator('#city-lesson'); assert.equal(await lesson.evaluate(e => e.open), false);
    await lesson.locator('summary').click();
    assert.ok((await lesson.textContent()).includes('49154'));
    const source = await readFile('suite-demos/src/commonMain/kotlin/org/graphiks/webgpu/suite/demos/instancedcity/CityShaders.kt', 'utf8');
    const blocks = [...source.matchAll(/"""([\s\S]*?)"""\.trimIndent\(\)/g)].map(m => {
      const lines = m[1].replace(/^\n|\n$/g,'').split('\n');
      const n = Math.min(...lines.filter(l=>l.trim()).map(l=>l.match(/^ */)[0].length));
      return lines.map(l=>l.slice(n)).join('\n').trim();
    });
    assert.deepEqual(await lesson.locator('pre code').allTextContents(), [blocks[0]+'\n'+blocks[1],blocks[0]+'\n'+blocks[2]]);
    for (const width of [1440,390]) {
      await page.setViewportSize({width,height:900});
      assert.equal(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), true);
    }
    assert.deepEqual(errors, []);
  } finally { await context.close(); }
});

test('city visual fixtures at all densities and height extremes', async () => {
  const { context, page, errors } = await cityPage();
  try {
    await mkdir('.openchamber/screenshots/city-fixtures', { recursive:true });
    for (const density of ['Sparse','Medium','Dense']) {
      await page.locator('#city-density').selectOption(density);
      await page.waitForFunction(() => !document.querySelector('#city-density').disabled);
      for (const height of [.5,2]) {
        await range(page,'height',height);
        await page.locator('#city-canvas').screenshot({path:`.openchamber/screenshots/city-fixtures/${density}-${height}.png`});
      }
    }
    const canvas=page.locator('#city-canvas'), box=await canvas.boundingBox();
    await page.mouse.move(box.x+box.width/2,box.y+box.height/2); await page.mouse.down();
    await page.mouse.move(box.x+box.width/2,box.y+1); await page.mouse.up(); await settle(page);
    await canvas.screenshot({path:'.openchamber/screenshots/city-fixtures/overhead.png'});
    await page.locator('#city-reset-view').click(); await range(page,'distance',100);
    await canvas.screenshot({path:'.openchamber/screenshots/city-fixtures/oblique-close.png'});
    assert.deepEqual(errors, []);
  } finally { await context.close(); }
});
