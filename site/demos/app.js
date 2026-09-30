// Renders the bilingual Demos gallery. Card texts come from the demo's own localized resource
// (shared with the browser runner), and the source links point at the commit of a published demo
// report when one is available. All dynamic text is inserted with textContent.
const LOCALES = ['en', 'fr'];
const DEFAULT_LOCALE = 'en';

const TEXT = {
  en: {
    title: 'Graphiks WebGPU Suite — Demos',
    subtitle: 'Interactive demonstrations of the public Graphiks WebGPU contract. Each demo runs in your browser against a real WebGPU implementation.',
    galleryTitle: 'Demos',
    validationLink: 'Validation',
    benchmarksLink: 'Benchmarks',
    launch: (label) => `Open in your browser — ${label}`,
    sourcesTitle: 'Scene source code',
    noSourceCommit: 'No published report names a commit yet; these links point at the source directory, not at a specific build.',
    note: 'These demos are illustrations, not measurements: they are not performance benchmarks and they do not count as contract coverage.',
    failed: 'The demo resource could not be loaded.',
  },
  fr: {
    title: 'Graphiks WebGPU Suite — Démos',
    subtitle: 'Démonstrations interactives du contrat public Graphiks WebGPU. Chaque démo s’exécute dans votre navigateur sur une vraie implémentation WebGPU.',
    galleryTitle: 'Démos',
    validationLink: 'Validation',
    benchmarksLink: 'Benchmarks',
    launch: (label) => `Ouvrir dans votre navigateur — ${label}`,
    sourcesTitle: 'Code source de la scène',
    noSourceCommit: 'Aucun rapport publié n’indique encore de commit ; ces liens pointent vers le dossier des sources, pas vers un build précis.',
    note: 'Ces démos sont des illustrations, pas des mesures : ce ne sont pas des benchmarks de performance et elles ne comptent pas comme couverture du contrat.',
    failed: 'La ressource de la démo n’a pas pu être chargée.',
  },
};

const REPO = 'https://github.com/Graphiks-org/WebGPU';
const SCENE_DIR = 'suite-demos/src/commonMain/kotlin/org/graphiks/webgpu/suite/demos/particles';
const SCENE_SOURCES = ['ParticleScene.kt', 'ParticleShaders.kt', 'ParticleData.kt'];

const DEMO = {
  dictPath: (locale) => `../run/js/demos/particles.${locale}.json`,
  launch: (target, locale) => `../run/${target}/?demo=particles&lang=${locale}`,
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

function renderChrome() {
  document.getElementById('page-title').textContent = t.title;
  document.getElementById('page-subtitle').textContent = t.subtitle;
  document.getElementById('gallery-title').textContent = t.galleryTitle;
  document.getElementById('gallery-note').textContent = t.note;

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
  nav.append(validation);
  const benchmarks = el('a', t.benchmarksLink);
  benchmarks.href = '../benchmarks/';
  nav.append(benchmarks);
}

async function publishedCommit() {
  for (const target of ['js', 'wasm']) {
    const report = await loadOptionalJson(`../reports/demos-${target}.json`);
    const commit = report?.buildCommit;
    if (commit && commit !== 'unknown') return commit;
  }
  return null;
}

function renderCard(dict, commit) {
  const card = el('article', null, 'card');
  card.append(el('h3', dict.title));
  card.append(el('p', dict.description));

  const links = el('div', null, 'card-links');
  for (const [target, label] of [['js', 'JS'], ['wasm', 'Wasm']]) {
    const link = el('a', t.launch(label), 'button');
    link.href = DEMO.launch(target, locale);
    links.append(link);
  }
  card.append(links);

  const sources = el('div', null, 'card-sources');
  sources.append(el('span', t.sourcesTitle, 'meta'));
  const base = commit ? `${REPO}/blob/${commit}/${SCENE_DIR}` : `${REPO}/tree/master/${SCENE_DIR}`;
  for (const file of SCENE_SOURCES) {
    const link = el('a', file);
    link.href = `${base}/${file}`;
    sources.append(link);
  }
  card.append(sources);
  if (!commit) card.append(el('p', t.noSourceCommit, 'meta'));
  return card;
}

async function main() {
  renderChrome();
  const [commit, dict] = await Promise.all([publishedCommit(), loadJson(DEMO.dictPath(locale))]);
  document.getElementById('gallery').append(renderCard(dict, commit));
}

main().catch((failure) => {
  const note = document.getElementById('gallery-note');
  note.textContent = `${t.failed} ${String(failure)}`;
  note.className = 'fatal';
});
