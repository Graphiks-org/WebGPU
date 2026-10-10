// Route-local validation precedes aggregation: one successful route cannot hide a missing demo.
export const DEMO_ROUTES = [
  { name: 'particles', ids: ['particles.compute-render-readback', 'particles.bounds-pause-reset'] },
  { name: 'reaction-diffusion', ids: ['reaction-diffusion.compute-render-readback',
    'reaction-diffusion.pause-step-reset', 'reaction-diffusion.brush-boundaries'] },
];

export function validateDemoReport(route, report, baseline) {
  const problems = [];
  if (!report || typeof report !== 'object' || !Array.isArray(report.cases)) return ['malformed demo report'];
  // Kotlin omits schemaVersion=1 and fatalError=null by default, as in the particle report.
  if (report.schemaVersion !== undefined && report.schemaVersion !== 1) problems.push('unexpected schemaVersion');
  if (report.fatalError !== undefined && report.fatalError !== null) {
    problems.push(typeof report.fatalError === 'string' && report.fatalError.length > 0
      ? `runner fatal error: ${report.fatalError}` : 'malformed fatalError');
  }
  if (typeof report.buildCommit !== 'string' || !report.buildCommit || report.buildCommit !== baseline.commit) {
    problems.push('distribution commit does not match inventory');
  }
  if (typeof report.buildVersion !== 'string' || !report.buildVersion || report.buildVersion !== baseline.suiteVersion) {
    problems.push('distribution version does not match inventory');
  }
  const ids = report.cases.map(c => c?.id);
  for (const id of route.ids) if (!ids.includes(id)) problems.push(`missing case: ${id}`);
  for (const [index, entry] of report.cases.entries()) {
    if (!entry || !route.ids.includes(entry.id)) problems.push(`unexpected case: ${entry?.id}`);
    if (ids.indexOf(entry?.id) !== index) problems.push(`duplicate case: ${entry?.id}`);
    if (entry?.status !== 'passed') problems.push(`case not passed: ${entry?.id}=${entry?.status}`);
  }
  return problems;
}

export function aggregateDemoReports(reports) {
  if (reports.length !== DEMO_ROUTES.length || reports.some(r => !r || !Array.isArray(r.cases))) {
    throw new Error('Incomplete demo campaign');
  }
  const first = reports[0];
  if (reports.some(r => r.buildCommit !== first.buildCommit || r.buildVersion !== first.buildVersion)) {
    throw new Error('Mixed distribution identities');
  }
  return { schemaVersion: 1, buildCommit: first.buildCommit, buildVersion: first.buildVersion,
    cases: reports.flatMap(r => r.cases), fatalError: reports.map(r => r.fatalError).filter(Boolean).join('\n') || null };
}

/** Run each route in a fresh page and retain real partial results when anything fails. */
export async function collectDemoReports(browser, origin, baseline, { timeout = 120000 } = {}) {
  const reports = [];
  const pageErrors = [];
  const problems = [];
  let userAgent = 'unknown';
  for (const route of DEMO_ROUTES) {
    let page;
    try {
      page = await browser.newPage();
      page.on('pageerror', error => pageErrors.push(`${route.name}: ${String(error)}`));
      await page.goto(`${origin}/?demo=${route.name}&verify=1`);
      userAgent = await page.evaluate(() => navigator.userAgent);
      await page.waitForFunction(() => typeof globalThis.graphiksDemoReport === 'string', null, { timeout });
      const report = JSON.parse(await page.evaluate(() => globalThis.graphiksDemoReport));
      problems.push(...validateDemoReport(route, report, baseline).map(p => `${route.name}: ${p}`));
      if (report && Array.isArray(report.cases)) reports.push(report);
    } catch (error) {
      problems.push(`${route.name}: ${String(error)}`);
    } finally {
      try { await page?.close(); } catch (error) { problems.push(`${route.name}: close: ${String(error)}`); }
    }
  }
  let report;
  try { report = aggregateDemoReports(reports); } catch (error) {
    problems.push(String(error));
    report = { schemaVersion: 1, buildCommit: reports[0]?.buildCommit ?? null,
      buildVersion: reports[0]?.buildVersion ?? null, cases: reports.flatMap(r => r.cases),
      fatalError: problems.join('\n') };
  }
  if (pageErrors.length) problems.push(`${pageErrors.length} page error(s)`);
  if (problems.length) report.fatalError = problems.join('\n');
  return { report, pageErrors, userAgent, fatalError: problems.join('\n') || null };
}
