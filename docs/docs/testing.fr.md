# Tests

Lancez le cycle normal de tests métier depuis la racine du dépôt :

```sh
./gradlew check
```

Gradle exécute les tâches compatibles avec la machine. Les suites de `webgpu-api` vérifient les
buffers et les énumérations ; `webgpu-web-bindings` couvre l’interop JS/Wasm. Sur une machine adaptée,
vous pouvez cibler les tâches suivantes :

```sh
./gradlew :webgpu-api:jvmTest :webgpu-api:checkKotlinAbi
./gradlew :webgpu-api:jsNodeTest :webgpu-web-bindings:jsNodeTest
./gradlew :webgpu-api:wasmJsNodeTest :webgpu-web-bindings:wasmJsNodeTest
```

Les tests Kotlin/Native dépendent de la machine. La CI lance les tests métier sous Linux, macOS
et Windows. Le téléchargement des spécifications, la documentation LLM facultative et la
régénération des bindings restent des opérations manuelles, hors des contrôles de PR.

## Acid tests navigateur

Le catalogue d’acid tests (123 cas : 118 obligatoires, cinq optionnels) s’exécute sur WebGPU réel
via un workflow navigateur distinct ; `check` seul ne lance pas ce runner. Le bilan de couverture et
les résidus sont dans [`docs/acid-coverage.md`](https://github.com/Graphiks-org/WebGPU/blob/master/docs/acid-coverage.md).
Avec JDK 25 et Node.js 22 :

```sh
npm ci --prefix tools
npm exec --prefix tools -- playwright install chromium
./gradlew :suite-browser:jsBrowserDistribution :suite-browser:wasmJsBrowserDistribution
node tools/run-browser.mjs js suite-browser/build/dist/js/productionExecutable
node tools/run-browser.mjs wasm suite-browser/build/dist/wasmJs/productionExecutable
node tools/run-browser.mjs js suite-browser/build/dist/js/productionExecutable --demo-check
node tools/run-browser.mjs wasm suite-browser/build/dist/wasmJs/productionExecutable --demo-check
node tools/run-browser.mjs js suite-browser/build/dist/js/productionExecutable --benchmark --profile=ci --backend=swiftshader
node tools/run-browser.mjs wasm suite-browser/build/dist/wasmJs/productionExecutable --benchmark --profile=ci --backend=swiftshader
node tools/build-site.mjs
```

La compilation génère le catalogue depuis les cas annotés. Le runner produit des rapports JS et
Wasm séparés et échoue en cas d’exécution absente, échouée ou incomplète. Le mode `--benchmark`
exige les dix scénarios `completed` et vérifiés côté GPU avec le nombre d’échantillons du profil ;
il n’échoue jamais sur la valeur d’une durée. La [page Validation](suite.md) distingue couverture des
comportements et résultats d’exécution, et la [page Benchmarks](suite.md) présente les mesures
publiées et leur protocole. Les tests portent sur les données, erreurs et états WebGPU ; aucun projet
dédié ne teste l’architecture de publication ou de consommation Maven, ni la présentation statistique.
