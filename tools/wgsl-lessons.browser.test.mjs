import { after, before, test } from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { browserTestServer } from './browser-test-server.mjs';

let server;
before(async () => { server = await browserTestServer(process.env.GRAPHIKS_DISTRIBUTION ?? 'suite-browser/build/dist/js/productionExecutable'); });
after(async () => { await server?.close(); });

const demos = [
  { name:'particles', lesson:'particles-lesson', source:'particles/ParticleShaders.kt', blocks:2 },
  { name:'reaction-diffusion', lesson:'reaction-lesson', source:'reactiondiffusion/ReactionDiffusionShaders.kt', blocks:3 },
  { name:'instanced-city', lesson:'city-lesson', source:'instancedcity/CityShaders.kt', blocks:2 },
];

function shaderStrings(source, name) {
  const strings=[...source.matchAll(/"""([\s\S]*?)"""(\.trimIndent\(\))?/g)].map(m => {
    if (!m[2]) return m[1];
    const lines=m[1].replace(/^\n|\n$/g,'').split('\n');
    const indent=Math.min(...lines.filter(l=>l.trim()).map(l=>l.match(/^ */)[0].length));
    return lines.map(l=>l.slice(indent)).join('\n').trim();
  });
  if (name === 'particles') return strings;
  if (name === 'reaction-diffusion') return [strings[0]+'\n\n'+strings[1],strings[0]+'\n\n'+strings[2],strings[3]];
  return [strings[0]+'\n\n'+strings[1],strings[0]+'\n\n'+strings[2]];
}

for (const demo of demos) for (const lang of ['en','fr']) {
  test(`${demo.name} ${lang}: shared readable highlighted WGSL preserves compiled source`, async () => {
    const page=await server.browser.newPage({reducedMotion:'reduce'}), errors=[];
    page.on('pageerror',e=>errors.push(String(e)));
    try {
      await page.goto(`${server.origin}/?demo=${demo.name}&lang=${lang}`);
      await page.locator('#demo-root canvas').waitFor();
      const lesson=page.locator(`#${demo.lesson}`);
      assert.equal(await lesson.count(),1,'Every demo must offer its shader lesson');
      assert.equal(await lesson.evaluate(e=>e.open),false);
      await lesson.locator('summary').click();
      const source=await readFile(`suite-demos/src/commonMain/kotlin/org/graphiks/webgpu/suite/demos/${demo.source}`,'utf8');
      const code=lesson.locator('pre code');
      assert.equal(await code.count(),demo.blocks);
      assert.deepEqual(await code.allTextContents(),shaderStrings(source,demo.name));
      for (const kind of ['keyword','type','attribute','number']) {
        const token=code.first().locator(`.wgsl-${kind}`).first();
        assert.equal(await token.count(),1,`${kind} highlighting missing`);
        assert.equal(await token.evaluate(e=>getComputedStyle(e).color!==getComputedStyle(e.parentElement).color),true);
      }
      const metrics=await code.first().evaluate(e=>({
        spacing:parseFloat(getComputedStyle(e).lineHeight)/parseFloat(getComputedStyle(e).fontSize),
        padding:parseFloat(getComputedStyle(e.parentElement).paddingLeft),
      }));
      assert.ok(metrics.spacing>=1.6 && metrics.padding>=16,'Readable spacing required');
      for (const text of await code.allTextContents()) {
        assert.equal(text.split('\n').some(line=>/;\s*\S/.test(line.split('//')[0])),false,'Statements must have their own lines');
        assert.ok(text.includes('}\n\n'),'Separate declarations/functions with blank lines');
      }
      const headings=await lesson.locator('h2').allTextContents();
      assert.ok(headings.every(h=>h.includes('WGSL')));
      assert.ok((await lesson.locator('summary').textContent()).includes(lang==='fr'?'Comment':'How'));
      for (const width of [1440,390]) {
        await page.setViewportSize({width,height:900});
        assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth),true);
      }
      assert.equal(await lesson.locator('pre script, pre img').count(),0);
      assert.deepEqual(errors,[]);
    } finally { await page.close(); }
  });
}
