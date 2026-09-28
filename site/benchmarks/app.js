// Loads the two benchmark envelopes and renders the bilingual Benchmarks page. Each target and its
// profile are shown separately; the page produces no score, no JS/Wasm ratio and no cross-target
// ranking. Medians, min/max and p95 are computed from the raw samples, and the raw report stays
// downloadable. All dynamic text is inserted with textContent.
const LOCALES = ['en', 'fr'];
const DEFAULT_LOCALE = 'en';
const REPO = 'https://github.com/Graphiks-org/WebGPU';

const TEXT = {
  en: {
    title: 'Graphiks WebGPU Suite — Benchmarks',
    subtitle: 'Portable transfer and compute workloads measured in a browser. Results depend on the runtime and the machine and are never a pure GPU time.',
    protocolTitle: 'Protocol and measurement',
    protocolFoundations: 'Protocol foundations-v1.',
    protocolTiming: 'Each sample stores two durations read from the same monotonic clock: the synchronous GPU calls, submission included for compute (cpuIssueMs), and the wait until queue.onSubmittedWorkDone() resolves (completionMs). The difference between the two is not presented as GPU time.',
    protocolWindow: 'Resource preparation, buffer creation and the result readbacks stay outside every measurement window; warm-ups run the same code and are discarded. The GPU output is read back and checked before and after each campaign, so a duration is only shown for a workload whose memory matched.',
    protocolProfile: 'The standard profile keeps 30 samples per scenario, the ci profile keeps 5; both run the same workloads at the same sizes and batch sizes.',
    ciObservation: 'The ci profile is a working check with a few observations, not a representative campaign.',
    softwareBackend: 'Requested software backend (SwiftShader): these are not hardware GPU performances.',
    jsTitle: 'JS run',
    wasmTitle: 'Wasm run',
    noReport: 'No published measurements: not run.',
    failed: 'The benchmark report could not be read.',
    profileLabel: 'Profile', browserLabel: 'Browser', platformLabel: 'Platform',
    backendLabel: 'Backend requested', headlessLabel: 'Headless', commitLabel: 'Commit', dateLabel: 'Date',
    adapterLabel: 'Adapter', fallbackLabel: 'Fallback adapter', featuresLabel: 'Features', limitsLabel: 'Limits used',
    unknown: 'unknown', yes: 'yes', no: 'no',
    headers: ['Scenario', 'Status', 'Size', 'Operations', 'Warm-ups', 'Samples', 'GPU output verified', 'Diagnostic', 'cpuIssueMs (ms)', 'completionMs (ms)'],
    statuses: { completed: 'Completed', failed: 'Failed', interrupted: 'Interrupted', 'not-run': 'Not run' },
    unit: { bytes: 'bytes', elements: 'elements' },
    lowResolution: 'Low resolution: the clock returned zero for some cpuIssueMs samples.',
    zeroSamples: 'samples at zero cpuIssueMs',
    p95: 'p95',
    downloadRaw: 'Download the raw samples (JSON)',
    runTitle: 'Run locally',
    runDescription: 'These links run the campaign in your browser. Results stay local and do not modify the published reports. The standard profile is used by default.',
    runLabel: (label) => `Run in your browser — ${label}`,
    validationLink: 'Validation', demosLink: 'Demos',
    refsTitle: 'References',
    refsProtocol: 'docs/benchmarks.md (protocol)',
    refsSources: 'suite-benchmarks sources',
    refsBaseline: 'baseline.json (reference version)',
  },
  fr: {
    title: 'Graphiks WebGPU Suite — Benchmarks',
    subtitle: 'Charges portables de transfert et de compute mesurées dans un navigateur. Les résultats dépendent du runtime et de la machine et ne sont jamais un temps GPU pur.',
    protocolTitle: 'Protocole et mesure',
    protocolFoundations: 'Protocole foundations-v1.',
    protocolTiming: 'Chaque échantillon stocke deux durées lues sur la même horloge monotone : les appels GPU synchrones, soumission incluse pour compute (cpuIssueMs), et l’attente jusqu’à la résolution de queue.onSubmittedWorkDone() (completionMs). La différence entre les deux n’est jamais présentée comme du temps GPU.',
    protocolWindow: 'La préparation des ressources, la création des buffers et les readbacks restent hors de chaque fenêtre de mesure ; les warm-ups exécutent le même code et sont écartés. Le résultat GPU est relu et vérifié avant et après chaque campagne : une durée n’est affichée que pour une charge dont la mémoire correspondait.',
    protocolProfile: 'Le profil standard conserve 30 échantillons par scénario, le profil ci en conserve 5 ; les deux exécutent les mêmes charges aux mêmes tailles et tailles de lot.',
    ciObservation: 'Le profil ci est un contrôle de fonctionnement avec quelques observations, pas une campagne représentative.',
    softwareBackend: 'Backend logiciel demandé (SwiftShader) : ce ne sont pas des performances de GPU matériel.',
    jsTitle: 'Exécution JS',
    wasmTitle: 'Exécution Wasm',
    noReport: 'Aucune mesure publiée : non exécuté.',
    failed: 'Le rapport de benchmarks n’a pas pu être lu.',
    profileLabel: 'Profil', browserLabel: 'Navigateur', platformLabel: 'Plateforme',
    backendLabel: 'Backend demandé', headlessLabel: 'Sans interface', commitLabel: 'Commit', dateLabel: 'Date',
    adapterLabel: 'Adapter', fallbackLabel: 'Adapter de secours', featuresLabel: 'Fonctionnalités', limitsLabel: 'Limites utilisées',
    unknown: 'inconnu', yes: 'oui', no: 'non',
    headers: ['Scénario', 'Statut', 'Taille', 'Opérations', 'Warm-ups', 'Échantillons', 'Résultat GPU vérifié', 'Diagnostic', 'cpuIssueMs (ms)', 'completionMs (ms)'],
    statuses: { completed: 'Terminé', failed: 'Échoué', interrupted: 'Interrompu', 'not-run': 'Non exécuté' },
    unit: { bytes: 'octets', elements: 'éléments' },
    lowResolution: 'Résolution faible : l’horloge a renvoyé zéro pour certains échantillons cpuIssueMs.',
    zeroSamples: 'échantillons à cpuIssueMs nul',
    p95: 'p95',
    downloadRaw: 'Télécharger les échantillons bruts (JSON)',
    runTitle: 'Exécution locale',
    runDescription: 'Ces liens exécutent la campagne dans votre navigateur. Les résultats restent locaux et ne modifient pas les rapports publiés. Le profil standard est utilisé par défaut.',
    runLabel: (label) => `Exécuter dans votre navigateur — ${label}`,
    validationLink: 'Validation', demosLink: 'Démos',
    refsTitle: 'Références',
    refsProtocol: 'docs/benchmarks.md (protocole)',
    refsSources: 'sources de suite-benchmarks',
    refsBaseline: 'baseline.json (version de référence)',
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

/** Median, min, max and, from 20 samples on, the p95 at the nearest rank above after sorting. */
function statistics(values) {
  const finite = values.filter((value) => Number.isFinite(value));
  if (finite.length === 0) return null;
  const sorted = finite.toSorted((a, b) => a - b);
  const middle = Math.floor(sorted.length / 2);
  const median = sorted.length % 2
    ? sorted[middle]
    : (sorted[middle - 1] + sorted[middle]) / 2;
  const p95 = sorted.length >= 20 ? sorted[Math.ceil(sorted.length * 0.95) - 1] : null;
  return { count: sorted.length, median, min: sorted[0], max: sorted[sorted.length - 1], p95 };
}

const fmt = (value) => value.toFixed(3);

const describe = (stats) => {
  if (!stats) return '—';
  const summary = `${fmt(stats.median)} [${fmt(stats.min)}..${fmt(stats.max)}]`;
  return stats.p95 === null ? summary : `${summary} · ${t.p95} ${fmt(stats.p95)}`;
};

function renderChrome() {
  document.getElementById('page-title').textContent = t.title;
  document.getElementById('page-subtitle').textContent = t.subtitle;
  document.getElementById('protocol-title').textContent = t.protocolTitle;
  document.getElementById('js-title').textContent = t.jsTitle;
  document.getElementById('wasm-title').textContent = t.wasmTitle;
  document.getElementById('run-title').textContent = t.runTitle;
  document.getElementById('run-description').textContent = t.runDescription;
  document.getElementById('refs-title').textContent = t.refsTitle;

  const protocol = document.getElementById('protocol-body');
  protocol.replaceChildren();
  for (const text of [t.protocolFoundations, t.protocolTiming, t.protocolWindow, t.protocolProfile]) {
    protocol.append(el('p', text, 'meta'));
  }

  const switchBox = document.getElementById('locale-switch');
  switchBox.replaceChildren();
  for (const code of LOCALES) {
    const link = el('a', code.toUpperCase(), code === locale ? 'locale-current' : null);
    link.href = `?lang=${code}`;
    switchBox.append(link, ' ');
  }

  const nav = document.getElementById('suite-nav');
  nav.replaceChildren();
  const validation = el('a', t.validationLink);
  validation.href = '../index.html';
  const demos = el('a', t.demosLink);
  demos.href = '../demos/';
  nav.append(validation, demos);

  const runLinks = document.getElementById('run-links');
  runLinks.replaceChildren();
  for (const [target, label] of [['js', 'JS'], ['wasm', 'Wasm']]) {
    const item = document.createElement('li');
    const link = el('a', t.runLabel(label));
    link.href = `../run/${target}/?benchmark=foundations&lang=${locale}`;
    item.append(link);
    runLinks.append(item);
  }
}

function renderReferences(envelope) {
  const refs = document.getElementById('reference-links');
  refs.replaceChildren();
  const commit = envelope?.suiteCommit && envelope.suiteCommit !== 'unknown' ? envelope.suiteCommit : 'master';
  const entries = [
    [t.refsProtocol, `${REPO}/blob/${commit}/docs/benchmarks.md`],
    [t.refsSources, `${REPO}/tree/${commit}/suite-benchmarks/src/commonMain/kotlin/org/graphiks/webgpu/suite/benchmarks`],
    [t.refsBaseline, '../inventory/baseline.json'],
  ];
  for (const [label, href] of entries) {
    const item = document.createElement('li');
    const link = el('a', label);
    link.href = href;
    item.append(link);
    refs.append(item);
  }
}

function scenarioRow(scenario) {
  const row = document.createElement('tr');
  const completed = scenario.status === 'completed';
  const cpu = completed ? statistics((scenario.samples ?? []).map((sample) => sample.cpuIssueMs)) : null;
  const completion = completed ? statistics((scenario.samples ?? []).map((sample) => sample.completionMs)) : null;
  const cells = [
    scenario.id,
    t.statuses[scenario.status] ?? scenario.status,
    `${scenario.size} ${t.unit[scenario.sizeUnit] ?? scenario.sizeUnit}`,
    scenario.operationsPerSample,
    scenario.warmups,
    `${scenario.samples?.length ?? 0}/${scenario.plannedSamples}`,
    scenario.outputVerified ? t.yes : t.no,
    scenario.diagnostic ?? '',
    describe(cpu),
    describe(completion),
  ];
  for (const value of cells) row.append(el('td', String(value)));
  return row;
}

function renderTarget(target, envelope) {
  const container = document.getElementById(`${target}-details`);
  container.replaceChildren();
  if (!envelope) {
    container.append(el('p', t.noReport, 'meta'));
    return;
  }
  const report = envelope.report;
  if (!report) {
    container.append(el('p', `${t.failed} ${envelope.fatalError ?? ''}`, 'fatal'));
    return;
  }

  const env = envelope.environment ?? {};
  const backend = env.backend ?? env.requestedBackend;
  const meta = [
    [t.profileLabel, report.profile],
    [t.browserLabel, env.browser],
    [t.platformLabel, env.platform],
    [t.backendLabel, backend],
    [t.headlessLabel, env.headless === true ? t.yes : t.no],
    [t.commitLabel, envelope.suiteCommit],
    [t.dateLabel, envelope.generatedAt],
  ].map(([label, value]) => `${label}: ${value ?? t.unknown}`).join(' · ');
  container.append(el('p', meta, 'meta'));
  if (report.profile === 'ci') container.append(el('p', t.ciObservation, 'meta'));
  if (backend === 'swiftshader') container.append(el('p', t.softwareBackend, 'meta'));
  if (envelope.fatalError) container.append(el('p', `${t.failed} ${envelope.fatalError}`, 'fatal'));
  if (envelope.pageErrors?.length) container.append(el('p', envelope.pageErrors.join(' | '), 'fatal'));

  const first = (report.scenarios ?? []).find((scenario) => scenario.status === 'completed');
  if (first) {
    const adapter = first.adapterDescription && first.adapterDescription.length > 0
      ? first.adapterDescription
      : t.unknown;
    const details = [`${t.adapterLabel}: ${adapter}`, `${t.fallbackLabel}: ${first.isFallbackAdapter ? t.yes : t.no}`];
    if (first.features?.length) details.push(`${t.featuresLabel}: ${first.features.join(', ')}`);
    if (first.limitsUsed && Object.keys(first.limitsUsed).length > 0) {
      details.push(`${t.limitsLabel}: ${Object.entries(first.limitsUsed).map(([name, value]) => `${name}=${value}`).join(', ')}`);
    }
    container.append(el('p', details.join(' · '), 'meta'));
  }

  const zeroScenarios = (report.scenarios ?? []).filter((scenario) => (scenario.zeroCpuSamples ?? 0) > 0);
  if (zeroScenarios.length > 0) {
    container.append(el(
      'p',
      `${t.lowResolution} (${t.zeroSamples}: ${zeroScenarios.map((scenario) => `${scenario.id}=${scenario.zeroCpuSamples}`).join(', ')})`,
      'meta',
    ));
  }

  const table = document.createElement('table');
  const headRow = document.createElement('tr');
  for (const label of t.headers) headRow.append(el('th', label));
  const head = document.createElement('thead');
  head.append(headRow);
  const body = document.createElement('tbody');
  for (const scenario of report.scenarios ?? []) body.append(scenarioRow(scenario));
  table.append(head, body);
  const wrap = el('div', null, 'table-wrap');
  wrap.append(table);
  container.append(wrap);

  const download = el('a', t.downloadRaw, 'button');
  download.href = `../reports/benchmarks-${target}.json`;
  download.setAttribute('download', `benchmarks-${target}.json`);
  container.append(download);
}

async function main() {
  renderChrome();
  const [js, wasm] = await Promise.all([
    loadOptionalJson('../reports/benchmarks-js.json'),
    loadOptionalJson('../reports/benchmarks-wasm.json'),
  ]);
  renderTarget('js', js);
  renderTarget('wasm', wasm);
  renderReferences(js ?? wasm);
}

main().catch((failure) => {
  const subtitle = document.getElementById('page-subtitle');
  subtitle.textContent = `${t.failed} ${String(failure)}`;
  subtitle.className = 'fatal';
});
