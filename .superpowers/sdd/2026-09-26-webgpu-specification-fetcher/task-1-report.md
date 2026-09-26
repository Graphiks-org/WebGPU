# Task 1 — moteur de rafraîchissement des spécifications WebGPU

## Résultat

Implémentation terminée. Le moteur récupère toutes les ressources et les valide avant de remplacer une cible, compare les SHA-256 au contenu réellement présent sur disque, actualise le cache au format `FileCache`, et nettoie les fichiers temporaires en cas de succès comme d’échec.

Le remplacement des ressources tente d’abord `ATOMIC_MOVE` et ne retente en mode non atomique que si le système signale `AtomicMoveNotSupportedException`. Le cache utilise uniquement `ATOMIC_MOVE`; son ancien contenu reste intact si cette opération échoue.

## Fichiers

- `build-logic/settings.gradle.kts` — included build et import du catalog `../gradle/libs.versions.toml`.
- `build-logic/build.gradle.kts` — Kotlin DSL, serialization, JUnit 4 et configuration de la tâche `test`.
- `build-logic/src/main/kotlin/io/ygdrasil/webgpu/fetcher/SpecificationRefreshService.kt` — interfaces publiques, récupération HTTP, staging, validation, hash SHA-256, JSON du cache et horodatage via `Clock`.
- `build-logic/src/main/kotlin/io/ygdrasil/webgpu/fetcher/AtomicFileReplacer.kt` — remplacement de ressource avec fallback limité au cas prescrit.
- `build-logic/src/main/kotlin/io/ygdrasil/webgpu/fetcher/AtomicCacheWriter.kt` — remplacement strictement atomique du cache.
- `build-logic/src/test/kotlin/io/ygdrasil/webgpu/fetcher/SpecificationRefreshServiceTest.kt` — 11 tests avec `HttpServer` loopback et horloge fixe.
- `build-logic/src/test/kotlin/io/ygdrasil/webgpu/fetcher/AtomicFileReplacerTest.kt` — 3 tests de politique de déplacement.

## Preuve RED

Commande exécutée :

```text
rtk ./gradlew -p build-logic test --tests 'io.ygdrasil.webgpu.fetcher.*Test'
```

Les classes de test ont compilé, puis les 14 tests ont échoué sur les méthodes placeholders (`NotImplementedError`), ce qui confirme qu’ils détectaient le comportement manquant :

```text
14 tests completed, 14 failed
BUILD FAILED
```

## Preuve GREEN et vérification

Après l’implémentation et une exécution finale ciblée :

```text
rtk ./gradlew -p build-logic test --tests 'io.ygdrasil.webgpu.fetcher.*Test'
BUILD SUCCESSFUL in 1s
```

Puis exécution de toute la suite de tests de la tâche :

```text
rtk ./gradlew -p build-logic test
BUILD SUCCESSFUL in 1s
```

Les rapports XML confirment `AtomicFileReplacerTest`: 3 tests, 0 échec, et `SpecificationRefreshServiceTest`: 11 tests, 0 échec. `rtk git diff --check` ne signale aucune erreur d’espacement.

Les cas couverts incluent les téléchargements HTTP 503 et les réponses vides, l’absence de remplacement avant validation de toutes les sources, le nettoyage des temporaires, la détection de contenu local modifié malgré un hash de cache identique, la réparation de cible absente, la normalisation des entrées de cache dupliquées, la préservation du cache lors d’un échec de remplacement ou de commit, et la réconciliation à la prochaine exécution.

## Auto-revue et réserves

- Les hashes retournés et écrits dans le cache proviennent des octets téléchargés; la décision de remplacer dépend du SHA-256 relu depuis le fichier cible réel. Le cache existant est seulement chargé pour vérifier qu’il est lisible et valide.
- Toutes les sources sont téléchargées et validées avant le début des remplacements.
- Les exceptions de refresh nomment l’opération et l’URL concernée; l’échec du commit cache inclut toutes les URL vérifiées.
- Aucun fallback non atomique n’existe sur le chemin du cache.
- Gradle affiche un avertissement de compatibilité : son plugin `kotlin-dsl` embarque Kotlin `2.3.20`, tandis que le catalog demande le plugin de serialization Kotlin `2.3.21`. Les tests passent malgré cet avertissement; il découle de la configuration versionnée demandée pour l’included build.
- Aucun test global du dépôt n’a été relancé; la vérification complète exécutée porte sur toute la suite du nouvel included build `build-logic`, comme demandé pour cette tâche.
