// Assembles the static Validation page into build/site/.
//
// Copies the page, the two browser reports and the two runnable distributions, and builds the
// generated contract inventory. Fails when the inventory or a report is missing: an absent report
// is never treated as a success.
import { execFileSync } from 'node:child_process';
import { access, cp, mkdir, rm } from 'node:fs/promises';
import { constants } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const out = join(root, 'build', 'site');

const distributionByTarget = {
  js: join(root, 'suite-browser', 'build', 'dist', 'js', 'productionExecutable'),
  wasm: join(root, 'suite-browser', 'build', 'dist', 'wasmJs', 'productionExecutable'),
};

await access(join(root, 'suite-acid-tests', 'build', 'suite-inventory'), constants.R_OK);
for (const target of Object.keys(distributionByTarget)) {
  await access(join(root, 'build', 'reports', `${target}.json`), constants.R_OK);
  await access(distributionByTarget[target], constants.R_OK);
}

await rm(out, { recursive: true, force: true });
await mkdir(out, { recursive: true });

await cp(join(root, 'site'), out, { recursive: true });

await mkdir(join(out, 'reports'), { recursive: true });
await mkdir(join(out, 'run'), { recursive: true });
for (const [target, distribution] of Object.entries(distributionByTarget)) {
  await cp(join(root, 'build', 'reports', `${target}.json`), join(out, 'reports', `${target}.json`));
  await cp(distribution, join(out, 'run', target), { recursive: true });
}

execFileSync(process.execPath, [join(root, 'tools', 'build-inventory.mjs'), join(out, 'inventory')], { stdio: 'inherit' });

console.log(`built ${out}`);
