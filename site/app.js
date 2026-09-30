// Loads the generated localized inventory and the two browser reports and renders the bilingual
// Validation page. All dynamic text is inserted with textContent; diagnostics are never HTML.
const LOCALES = ['en', 'fr'];
const DEFAULT_LOCALE = 'en';

const TEXT = {
  en: {
    subtitle: 'Public contract inventory, behaviour coverage and browser execution results. Published results come from identified runs and are never conflated with a local launch.',
    coverageTitle: 'Coverage and results',
    summary: (total, covered) => `${total} behaviours described, ${covered} linked to an executable case. “To be tested” means no case exists yet, never that the behaviour passed.`,
    headers: ['Behaviour', 'Family', 'Expectation', 'Cases', 'JS', 'Wasm'],
    toTest: 'To be tested',
    jsTitle: 'JS run',
    wasmTitle: 'Wasm run',
    runTitle: 'Run locally',
    runDescription: 'These links run the suite in your browser. Results stay local and do not modify the published reports.',
    runLabel: (label) => `Run in your browser — local results (${label})`,
    demosLink: 'Demos',
    benchmarksLink: 'Benchmarks',
    refsTitle: 'References',
    refs: [['contract.md (generated inventory)', './inventory/contract.md'], ['symbols.tsv (declarations)', './inventory/symbols.tsv'], ['baseline.json (reference version)', './inventory/baseline.json']],
    caseSource: 'Case source code',
    noReport: 'No published report: Not run.',
    apiVersion: 'API version', suiteVersion: 'Suite version', referenceCommit: 'Reference commit', reportCommit: 'Report commit', reportDate: 'Report date',
    browser: 'Browser', platform: 'Platform', backend: 'Requested backend',
    fatal: 'Fatal error', pageErrors: 'Page errors', runnerError: 'Runner error',
    caseHeaders: ['Case', 'Status', 'Diagnostic', 'Adapter'],
    statuses: { passed: 'Passed', failed: 'Failed', unsupported: 'Unsupported', 'not-run': 'Not run' },
  },
  fr: {
    subtitle: 'Inventaire du contrat public, couverture des comportements et résultats d’exécution navigateur. Les résultats publiés proviennent d’exécutions identifiées et ne sont pas confondus avec un lancement local.',
    coverageTitle: 'Couverture et résultats',
    summary: (total, covered) => `${total} comportements décrits, ${covered} reliés à un cas exécutable. « À tester » signifie qu’aucun cas n’existe encore, jamais que le comportement est validé.`,
    headers: ['Comportement', 'Famille', 'Attente', 'Cas', 'JS', 'Wasm'],
    toTest: 'À tester',
    jsTitle: 'Exécution JS',
    wasmTitle: 'Exécution Wasm',
    runTitle: 'Exécution locale',
    runDescription: 'Ces liens exécutent la suite dans votre navigateur. Les résultats restent locaux et ne modifient pas les rapports publiés.',
    runLabel: (label) => `Exécuter dans votre navigateur — résultats locaux (${label})`,
    demosLink: 'Démos',
    benchmarksLink: 'Benchmarks',
    refsTitle: 'Références',
    refs: [['contract.md (inventaire généré)', './inventory/contract.md'], ['symbols.tsv (déclarations)', './inventory/symbols.tsv'], ['baseline.json (version de référence)', './inventory/baseline.json']],
    caseSource: 'Code source des cas',
    noReport: 'Aucun rapport publié : non exécuté.',
    apiVersion: 'Version de l’API', suiteVersion: 'Version de la suite', referenceCommit: 'Commit de référence', reportCommit: 'Commit des rapports', reportDate: 'Date des rapports',
    browser: 'Navigateur', platform: 'Plateforme', backend: 'Backend demandé',
    fatal: 'Erreur fatale', pageErrors: 'Erreurs de page', runnerError: 'Erreur du runner',
    caseHeaders: ['Cas', 'Statut', 'Diagnostic', 'Adapter'],
    statuses: { passed: 'Réussi', failed: 'Échoué', unsupported: 'Non pris en charge', 'not-run': 'Non exécuté' },
  },
};

const locale = (() => {
  const fromUrl = new URL(location.href).searchParams.get('lang');
  if (LOCALES.includes(fromUrl)) return fromUrl;
  const stored = localStorage.getItem('graphiks-suite-locale');
  return LOCALES.includes(stored) ? stored : DEFAULT_LOCALE;
})();
const t = TEXT[locale];
document.documentElement.lang = locale;

const el = (tag, text, className) => {
  const node = document.createElement(tag);
  if (text !== undefined && text !== null) node.textContent = text;
  if (className) node.className = className;
  return node;
};

const loadJson = async (path) => {
  const response = await fetch(path);
  if (!response.ok) throw new Error(`${path}: HTTP ${response.status}`);
  return response.json();
};
const loadOptionalJson = (path) => loadJson(path).catch(() => null);

function renderChrome(baseline, behaviours, reports) {
  document.getElementById('page-title').textContent = 'Graphiks WebGPU Suite — Validation';
  document.getElementById('page-subtitle').textContent = t.subtitle;
  document.getElementById('coverage-title').textContent = t.coverageTitle;
  document.getElementById('js-title').textContent = t.jsTitle;
  document.getElementById('wasm-title').textContent = t.wasmTitle;
  document.getElementById('run-title').textContent = t.runTitle;
  document.getElementById('run-description').textContent = t.runDescription;
  document.getElementById('refs-title').textContent = t.refsTitle;

  const nav = document.getElementById('suite-nav');
  nav.replaceChildren();
  const demosLink = el('a', t.demosLink);
  demosLink.href = './demos/';
  nav.append(demosLink);
  const benchmarksLink = el('a', t.benchmarksLink);
  benchmarksLink.href = './benchmarks/';
  nav.append(benchmarksLink);

  const switchBox = document.getElementById('locale-switch');
  switchBox.replaceChildren();
  for (const code of LOCALES) {
    const link = el('a', code.toUpperCase(), code === locale ? 'locale-current' : null);
    link.href = `?lang=${code}`;
    switchBox.append(link, ' ');
  }

  const versions = document.getElementById('versions');
  versions.replaceChildren();
  const report = reports.js ?? reports.wasm;
  for (const [label, value] of [
    [t.apiVersion, baseline.apiVersion],
    [t.suiteVersion, baseline.suiteVersion],
    [t.referenceCommit, baseline.commit],
    [t.reportCommit, report?.buildCommit ?? '—'],
    [t.reportDate, report?.generatedAt ?? '—'],
  ]) {
    versions.append(el('dt', label), el('dd', value ?? '—'));
  }

  const runLinks = document.getElementById('run-links');
  runLinks.replaceChildren();
  for (const [target, label] of [['js', 'JS'], ['wasm', 'Wasm']]) {
    const item = document.createElement('li');
    const link = el('a', t.runLabel(label));
    link.href = `./run/${target}/`;
    item.append(link);
    runLinks.append(item);
  }

  const refs = document.getElementById('reference-links');
  refs.replaceChildren();
  for (const [label, href] of t.refs) {
    const item = document.createElement('li');
    const link = el('a', label);
    link.href = href;
    item.append(link);
    refs.append(item);
  }
  const sourceItem = document.createElement('li');
  const sourceLink = el('a', t.caseSource);
  sourceLink.href = `https://github.com/Graphiks-org/WebGPU/tree/${baseline.commit}/suite-acid-tests/src/commonMain/kotlin/org/graphiks/webgpu/suite/acid`;
  sourceItem.append(sourceLink);
  refs.append(sourceItem);

  document.getElementById('coverage-summary').textContent = t.summary(behaviours.length, behaviours.filter((b) => b.caseIds.length > 0).length);
}

function statusFor(envelope, caseIds) {
  if (!envelope || caseIds.length === 0) return 'not-run';
  const results = caseIds.map((id) => envelope.report?.cases?.find((entry) => entry.id === id)).filter(Boolean);
  if (results.length === 0) return 'not-run';
  if (results.some((entry) => entry.status !== 'passed')) return results.find((entry) => entry.status !== 'passed').status;
  return 'passed';
}

function renderCoverage(behaviours, reports, baseline) {
  const head = document.querySelector('#coverage thead');
  const headRow = document.createElement('tr');
  for (const label of t.headers) headRow.append(el('th', label));
  head.replaceChildren(headRow);

  const body = document.querySelector('#coverage tbody');
  body.replaceChildren();
  for (const behaviour of behaviours) {
    const row = document.createElement('tr');
    const idCell = el('td', null, 'mono');
    if (behaviour.caseIds.length > 0 && behaviour.sourcePath) {
      const link = el('a', behaviour.id);
      link.href = `https://github.com/Graphiks-org/WebGPU/blob/${baseline.commit}/${behaviour.sourcePath}`;
      idCell.append(link);
    } else {
      idCell.textContent = behaviour.id;
    }
    row.append(
      idCell,
      el('td', behaviour.familyLabel),
      el('td', behaviour.expectation),
      el('td', behaviour.caseIds.length > 0 ? behaviour.caseIds.join(', ') : t.toTest),
    );
    for (const target of ['js', 'wasm']) {
      const status = statusFor(reports[target], behaviour.caseIds);
      row.append(el('td', t.statuses[status] ?? t.statuses['not-run'], `status status-${status}`));
    }
    body.append(row);
  }
}

function renderTarget(target, envelope) {
  const container = document.getElementById(`${target}-details`);
  container.replaceChildren();
  if (!envelope) {
    container.append(el('p', t.noReport, 'meta'));
    return;
  }
  const meta = [t.browser, t.platform, t.backend].map((label, index) => `${label}: ${[envelope.environment?.browser, envelope.environment?.platform, envelope.environment?.requestedBackend][index] ?? '—'}`).join(' · ');
  container.append(el('p', meta, 'meta'));
  if (envelope.fatalError) container.append(el('p', `${t.fatal}: ${envelope.fatalError}`, 'fatal'));
  if (envelope.pageErrors?.length) container.append(el('p', `${t.pageErrors}: ${envelope.pageErrors.join(' | ')}`, 'fatal'));
  if (envelope.report?.fatalError) container.append(el('p', `${t.runnerError}: ${envelope.report.fatalError}`, 'fatal'));

  const table = document.createElement('table');
  const headRow = document.createElement('tr');
  for (const label of t.caseHeaders) headRow.append(el('th', label));
  table.append(el('thead', null), el('tbody', null));
  table.querySelector('thead').append(headRow);
  const body = table.querySelector('tbody');
  for (const entry of envelope.report?.cases ?? []) {
    const status = entry.status ?? 'not-run';
    const row = document.createElement('tr');
    row.append(el('td', entry.id, 'mono'), el('td', t.statuses[status] ?? status, `status status-${status}`), el('td', entry.diagnostic ?? ''), el('td', entry.adapterDescription ?? ''));
    body.append(row);
  }
  const wrap = el('div', null, 'table-wrap');
  wrap.append(table);
  container.append(wrap);
}

async function main() {
  const [baseline, behaviours, js, wasm] = await Promise.all([
    loadJson('./inventory/baseline.json'),
    loadJson(`./inventory/behaviours.${locale}.json`),
    loadOptionalJson('./reports/js.json'),
    loadOptionalJson('./reports/wasm.json'),
  ]);
  const reports = { js, wasm };
  renderChrome(baseline, behaviours, reports);
  renderCoverage(behaviours, reports, baseline);
  renderTarget('js', js);
  renderTarget('wasm', wasm);
}

main().catch((failure) => {
  const summary = document.getElementById('coverage-summary');
  summary.textContent = `Inventory load failed: ${String(failure)}`;
  summary.className = 'fatal';
});
