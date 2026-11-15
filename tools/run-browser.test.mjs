// Guards the runner's behaviour when the distribution it was pointed at does not exist.
//
// A missing distribution is what a failed build leaves behind. The runner must report it at once
// instead of serving 404s to Chromium and waiting out its whole report budget: on CI that budget is
// one hour per target, so an absent build used to turn a 59-second failure into an hour-long step.
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { spawn } from 'node:child_process';
import { mkdtemp, readdir, rm } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const tools = dirname(fileURLToPath(import.meta.url));
const repository = resolve(tools, '..');

function run(args) {
  return new Promise((done) => {
    const started = Date.now();
    const child = spawn(process.execPath, [join(tools, 'run-browser.mjs'), ...args], {
      cwd: repository,
      stdio: ['ignore', 'pipe', 'pipe'],
    });
    let stdout = '';
    let stderr = '';
    child.stdout.on('data', (chunk) => { stdout += chunk; });
    child.stderr.on('data', (chunk) => { stderr += chunk; });
    child.on('close', (code) => done({ code, stdout, stderr, elapsedMs: Date.now() - started }));
  });
}

test('refuses a missing distribution without waiting for the report budget', async () => {
  const missing = join(tmpdir(), `graphiks-absent-distribution-${process.pid}`);
  const result = await run(['js', missing]);

  assert.equal(result.code, 1, `expected exit 1, got ${result.code}: ${result.stderr}`);
  assert.match(result.stderr, /distribution directory .* does not exist/);
  // The suite budget is an hour; a missing build must not spend any of it.
  assert.ok(result.elapsedMs < 20000, `took ${result.elapsedMs} ms`);
});
