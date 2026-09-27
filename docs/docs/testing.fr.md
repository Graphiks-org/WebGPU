# Tests

Lancez le cycle normal de tests métier depuis la racine du dépôt :

```sh
./gradlew check
```

Gradle exécute les tâches compatibles avec la machine. Les suites de `webgpu-api` vérifient les
buffers et les énumérations ; `webgpu-web` couvre l’interop JS/Wasm. Sur une machine adaptée,
vous pouvez cibler les tâches suivantes :

```sh
./gradlew :webgpu-api:jvmTest :webgpu-api:checkKotlinAbi
./gradlew :webgpu-api:jsNodeTest :webgpu-web:jsNodeTest
./gradlew :webgpu-api:wasmJsNodeTest :webgpu-web:wasmJsNodeTest
```

Les tests Kotlin/Native dépendent de la machine. La CI lance les tests métier sous Linux, macOS
et Windows. Le téléchargement des spécifications, la documentation LLM facultative et la
régénération des bindings restent des opérations manuelles, hors des contrôles de PR.
