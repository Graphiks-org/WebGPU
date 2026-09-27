# Entretien de la spécification

Exécutez ces commandes depuis la racine du dépôt. Le téléchargement des sources et la génération
des bindings sont des opérations distinctes et explicites ; aucune ne fait partie de `check`.

## 1. Télécharger les sources amont

```sh
./gradlew check-cache
```

Cette tâche télécharge la spécification HTML WebGPU du [W3C](https://www.w3.org/TR/webgpu/) et
l’IDL de [GPUWeb](https://gpuweb.github.io/gpuweb/webgpu.idl), les enregistre dans
`webgpu-specifications/src/jvmMain/resources/` et met à jour `cache.json`.

## 2. Reconstruire la documentation JSON

```sh
./gradlew refresh-documentation-from-spec
```

La tâche remplace `documentation.json` à partir des fichiers HTML et IDL versionnés. Relisez les
descriptions avant de générer les bindings.

## 3. Compléter facultativement les descriptions manquantes

```sh
./gradlew generate-doc-from-llm
```

Cette commande facultative demande un serveur de chat-completions compatible OpenAI à
`http://127.0.0.1:1234/v1`, avec le modèle `mistral-small-3.1-24b-instruct-2503`. La CI ne
l’exécute jamais.

## 4. Vérifier la couverture et convertir en YAML

```sh
./gradlew check-missing-doc
./gradlew tranform-json-doc-to-yaml
```

La première tâche affiche les clés manquantes. La seconde écrit `documentation.yaml` ; son nom
actuel est `tranform`, sans le deuxième « s ».

## 5. Générer les bindings

```sh
./gradlew generate-binding
```

Le générateur lit l’IDL et le YAML versionnés, puis réécrit les sources Kotlin générées dans
`webgpu-api`, `webgpu-descriptors` et `webgpu-web-bindings`. Relisez les changements et lancez les
[tests métier](testing.md). Ne modifiez pas directement les sources générées. `webgpu-browser` est
écrit à la main : il consomme les contrats et les bindings générés et ne contient pas de sources
générées.
