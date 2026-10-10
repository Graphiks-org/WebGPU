// Test-only static server and Chromium launcher, shared by the demo browser tests.
import { createServer } from 'node:http';
import { readFile } from 'node:fs/promises';
import { extname, isAbsolute, relative, resolve } from 'node:path';
import { chromium } from 'playwright';

export async function browserTestServer(directory) {
  const root = resolve(directory);
  const mime = { '.html': 'text/html', '.js': 'text/javascript', '.mjs': 'text/javascript',
    '.wasm': 'application/wasm', '.json': 'application/json', '.map': 'application/json' };
  const server = createServer(async (request, response) => {
    try {
      let pathname = decodeURIComponent(new URL(request.url, 'http://localhost').pathname);
      if (pathname.endsWith('/')) pathname += 'index.html';
      const file = resolve(root, `.${pathname}`);
      const within = relative(root, file);
      if (within.startsWith('..') || isAbsolute(within)) { response.writeHead(403); response.end(); return; }
      const body = await readFile(file);
      response.writeHead(200, { 'content-type': mime[extname(file)] ?? 'application/octet-stream' });
      response.end(body);
    } catch { response.writeHead(404); response.end('not found'); }
  });
  await new Promise(ok => server.listen(0, '127.0.0.1', ok));
  let browser;
  try {
    browser = await chromium.launch({ headless: true, args: [
      '--enable-unsafe-webgpu', '--enable-unsafe-swiftshader', '--use-angle=swiftshader',
    ] });
  } catch (error) { await new Promise(ok => server.close(ok)); throw error; }
  return { browser, origin: `http://127.0.0.1:${server.address().port}`,
    async close() { try { await browser.close(); } finally { await new Promise(ok => server.close(ok)); } } };
}
