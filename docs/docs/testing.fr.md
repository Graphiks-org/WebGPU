# Tests

Lancez le cycle normal de tests métier depuis la racine du dépôt :

```sh
./gradlew check
```

Gradle exécute les tâches compatibles avec la machine. Les suites de `webgpu-api` vérifient les
buffers et les énumérations ; `webgpu-web-bindings` et `webgpu-browser` couvrent l’interop JS/Wasm et
les conversions sans GPU. Sur une machine adaptée, vous pouvez cibler les tâches suivantes :

```sh
./gradlew :webgpu-api:jvmTest :webgpu-api:checkKotlinAbi
./gradlew :webgpu-api:jsNodeTest :webgpu-web-bindings:jsNodeTest :webgpu-browser:jsNodeTest
./gradlew :webgpu-api:wasmJsNodeTest :webgpu-web-bindings:wasmJsNodeTest :webgpu-browser:wasmJsNodeTest
```

## Tests GPU navigateur

`integration-tests/browser` est un build autonome qui exécute de vrais tests WebGPU de calcul, de
canvas et d’erreurs sous un Chromium doté d’un backend WebGPU. Il n’a pas de cible Node et n’est pas
inclus dans le build racine.

```sh
./gradlew -p integration-tests/browser jsBrowserTest wasmJsBrowserTest
```

Sans `-PpublicationRepository`, le build utilise un composite build et substitue les projets du
dépôt. Pour tester plutôt les artefacts d’un dépôt Maven local isolé, renseignez
`publicationRepository` et `testedVersion` :

```sh
./gradlew -p integration-tests/browser \
  -PpublicationRepository="$MAVEN_REPO" \
  -PtestedVersion=0.1.0-browser-verification-SNAPSHOT \
  jsBrowserTest wasmJsBrowserTest
```

Renseignez `CHROME_BIN` si le navigateur n’est pas sur le chemin par défaut. Le lanceur personnalisé
transmet les options WebGPU SwiftShader ; une machine sans backend utilisable fait échouer la tâche
au lieu de l’ignorer.

Les tests Kotlin/Native dépendent de la machine. La CI lance les tests métier sous Linux, macOS
et Windows, ainsi qu’un workflow GPU navigateur dédié. Le téléchargement des spécifications, la
documentation LLM facultative et la régénération des bindings restent des opérations manuelles,
hors des contrôles de PR.
