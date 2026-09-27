// Loads the published inventory and the two browser reports and renders the Validation page.
// All dynamic text is inserted with textContent; diagnostics are never treated as HTML.
const STATUS_LABELS = {
  passed: 'Réussi',
  failed: 'Échoué',
  unsupported: 'Non pris en charge',
  'not-run': 'Non exécuté',
};

const targetLabels = { js: 'JS', wasm: 'Wasm' };

async function loadJson(path) {
  const response = await fetch(path);
  if (!response.ok) {
    throw new Error(`${path}: HTTP ${response.status}`);
  }
  return response.json();
}

async function loadOptionalJson(path) {
  try {
    return await loadJson(path);
  } catch {
    return null;
  }
}

function el(tag, text, className) {
  const node = document.createElement(tag);
  if (text !== undefined && text !== null) node.textContent = text;
  if (className) node.className = className;
  return node;
}

function statusLabel(status) {
  return STATUS_LABELS[status] ?? 'Non exécuté';
}

function resultFor(envelope, id) {
  return envelope?.report?.cases?.find((entry) => entry.id === id) ?? null;
}

function renderVersions(baseline, reports) {
  const container = document.getElementById('versions');
  container.replaceChildren();
  const entries = [
    ['Version de l’API', baseline.apiVersion],
    ['Version de la suite', baseline.suiteVersion],
    ['Commit de référence', baseline.commit],
    ['Commit des rapports', reports.js?.suiteCommit ?? reports.wasm?.suiteCommit ?? 'inconnu'],
  ];
  for (const [label, value] of entries) {
    const dt = el('dt', label);
    const dd = el('dd', value ?? '—');
    container.append(dt, dd);
  }
}

function renderCoverage(baseline, behaviors, reports) {
  const tbody = document.querySelector('#coverage tbody');
  tbody.replaceChildren();
  const covered = behaviors.filter((behavior) => behavior.caseIds.length > 0).length;
  document.getElementById('coverage-summary').textContent =
    `${behaviors.length} comportements décrits, ${covered} reliés à un cas exécutable. ` +
    '« À tester » signifie qu’aucun cas n’existe encore, jamais que le comportement est validé.';

  for (const behavior of behaviors) {
    const row = document.createElement('tr');
    row.append(
      el('td', behavior.id, 'mono'),
      el('td', behavior.family),
      el('td', behavior.expectation),
      el('td', behavior.caseIds.length > 0 ? behavior.caseIds.join(', ') : 'À tester'),
    );
    for (const target of ['js', 'wasm']) {
      const result = behavior.caseIds
        .map((id) => resultFor(reports[target], id))
        .find((entry) => entry !== null) ?? null;
      const cell = document.createElement('td');
      const status = result ? result.status : 'not-run';
      const badge = el('span', statusLabel(status), `status status-${status}`);
      cell.append(badge);
      row.append(cell);
    }
    tbody.append(row);
  }
}

function renderTarget(target, envelope) {
  const container = document.getElementById(`${target}-details`);
  container.replaceChildren();
  if (!envelope) {
    container.append(el('p', `Aucun rapport ${targetLabels[target]} publié : Non exécuté.`, 'meta'));
    return;
  }

  const meta = [
    `Cible : ${targetLabels[target]}`,
    `Commit : ${envelope.suiteCommit}`,
    `Généré : ${envelope.generatedAt}`,
    `Navigateur : ${envelope.environment?.browser ?? 'inconnu'}`,
    `Plateforme : ${envelope.environment?.platform ?? 'inconnue'}`,
    `Backend demandé : ${envelope.environment?.requestedBackend ?? 'inconnu'}`,
  ];
  container.append(el('p', meta.join(' · '), 'meta'));

  if (envelope.fatalError) {
    container.append(el('p', `Erreur fatale : ${envelope.fatalError}`, 'fatal'));
  }
  if (Array.isArray(envelope.pageErrors) && envelope.pageErrors.length > 0) {
    container.append(el('p', `Erreurs de page : ${envelope.pageErrors.join(' | ')}`, 'fatal'));
  }
  if (envelope.report?.fatalError) {
    container.append(el('p', `Erreur du runner : ${envelope.report.fatalError}`, 'fatal'));
  }

  const table = document.createElement('table');
  const thead = document.createElement('thead');
  const headRow = document.createElement('tr');
  for (const heading of ['Cas', 'Statut', 'Diagnostic', 'Adapter']) {
    headRow.append(el('th', heading));
  }
  thead.append(headRow);
  const tbody = document.createElement('tbody');
  const cases = envelope.report?.cases ?? [];
  for (const entry of cases) {
    const row = document.createElement('tr');
    const status = entry.status ?? 'not-run';
    row.append(
      el('td', entry.id, 'mono'),
      el('td', statusLabel(status), `status status-${status}`),
      el('td', entry.diagnostic ?? ''),
      el('td', entry.adapterDescription ?? ''),
    );
    tbody.append(row);
  }
  table.append(thead, tbody);
  const wrap = document.createElement('div');
  wrap.className = 'table-wrap';
  wrap.append(table);
  container.append(wrap);
}

async function main() {
  const [baseline, behaviors, js, wasm] = await Promise.all([
    loadJson('./inventory/baseline.json'),
    loadJson('./inventory/behaviors.json'),
    loadOptionalJson('./reports/js.json'),
    loadOptionalJson('./reports/wasm.json'),
  ]);
  const reports = { js, wasm };
  renderVersions(baseline, reports);
  renderCoverage(baseline, behaviors, reports);
  renderTarget('js', js);
  renderTarget('wasm', wasm);

  const source = document.getElementById('cases-source');
  source.href = `https://github.com/Graphiks-org/WebGPU/tree/${baseline.commit}/suite-acid-tests/src/commonMain/kotlin/org/graphiks/webgpu/suite/acid`;
}

main().catch((failure) => {
  const container = document.getElementById('coverage-summary');
  container.textContent = `Échec du chargement de l’inventaire : ${String(failure)}`;
  container.className = 'fatal';
});
