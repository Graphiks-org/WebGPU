# Implémentation navigateur Graphiks WebGPU

## Objectif

Accueillir et publier dans ce dépôt une implémentation navigateur de l’API
Graphiks WebGPU pour Kotlin/JS et Kotlin/Wasm JS. Les utilisateurs doivent pouvoir
demander un adapter, créer un device, effectuer du calcul GPU et afficher dans un
canvas en utilisant les contrats communs et leurs descripteurs Kotlin.

Cette conception formalise les choix validés pendant la discussion. Sa revue
précède la rédaction du plan d’implémentation détaillé.

## Décisions validées

| Module publié | Package public | Responsabilité |
| --- | --- | --- |
| `org.graphiks:webgpu-api` | `org.graphiks.webgpu` | Contrats portables |
| `org.graphiks:webgpu-descriptors` | `org.graphiks.webgpu.descriptors` | Implémentations des descripteurs |
| `org.graphiks:webgpu-web-bindings` | `org.graphiks.webgpu.bindings` | Types JavaScript et interop JS/Wasm |
| `org.graphiks:webgpu-browser` | `org.graphiks.webgpu.browser` | Implémentation navigateur |

- `webgpu-web` devient `webgpu-web-bindings`.
- L’implémentation navigateur conserve l’architecture des descripteurs génériques.
- Les défauts de fidélité au contrat sont corrigés pendant l’intégration.
- Le dépôt externe reste hors du périmètre des modifications.
- La documentation décrit les modules et leur utilisation, sans historique de migration.
- Les notices de licence applicables sont conservées.

## État du dépôt d’accueil

Le group ID `org.graphiks` est déjà défini dans `build.gradle.kts`. La version
provient de `releaseVersion`, avec `0.1.0-SNAPSHOT` comme valeur par défaut.
Les sources publiques partagent actuellement le package `org.graphiks.webgpu`.

Le générateur écrit les descripteurs et les bindings dans ce même package.
`ModelWriter.kt` contient aussi le chemin de sortie `webgpu-web`. La séparation
des packages doit donc être prise en charge dans le générateur et dans les
sources présentes, plutôt que par une modification ponctuelle des fichiers générés.

Les workflows de publication énumèrent explicitement les artefacts. Le workflow
de tests possède une matrice Linux/macOS/Windows et des commandes Node pour JS/Wasm.

## Architecture proposée

### Dépendances

`webgpu-descriptors` dépend de `webgpu-api`. `webgpu-web-bindings` conserve les
dépendances nécessaires à ses types et à son interop. `webgpu-browser` expose
l’API commune et les bindings lorsque ceux-ci interviennent dans ses signatures
publiques. Il utilise les Kotlin wrappers nécessaires à l’intégration navigateur
et les coroutines pour les opérations asynchrones.

Les descripteurs concrets restent une commodité choisie par l’application :
l’implémentation consomme les interfaces de descripteurs de `webgpu-api`.
Les conversions internes sont regroupées dans `org.graphiks.webgpu.browser.mapper`.

### Organisation des sources

Le nouveau module cible JS et Wasm JS. Le code partagé entre ces deux cibles
réside dans `commonMain`; les adaptations propres à chaque cible résident dans
`jsMain` et `wasmJsMain`.

Les wrappers deviennent des classes propres à l’implémentation navigateur,
implémentant directement les interfaces communes. Les couples `expect`/`actual`
utiles uniquement pour choisir entre une implémentation native et une
implémentation web sont supprimés de cette extraction. Les différences réelles
d’interop JS/Wasm conservent une abstraction adaptée aux deux cibles.

### Points d’entrée et surfaces

Le module fournit l’acquisition de l’adapter, les wrappers des ressources et
encodeurs, les conversions de descripteurs et l’intégration canvas.
Les types auxiliaires nécessaires aux surfaces restent dans le package
`org.graphiks.webgpu.browser`; leur présence ne doit pas introduire de dépendance
vers une implémentation native dans l’API commune.

Un navigateur sans WebGPU ou sans adapter disponible produit un échec explicite
du point d’entrée asynchrone, plutôt qu’un wrapper construit autour de `null`.

## Fidélité au contrat

La revue des conversions prend pour référence les interfaces présentes dans ce
dépôt. Les cas suivants doivent être couverts par des régressions ciblées :

- `popErrorScope()` retourne un succès contenant `null` lorsqu’aucune erreur
  n’est capturée ; les erreurs GPU présentes sont correctement converties.
- Les arguments nullable de `setVertexBuffer` et `setBindGroup` sont transmis
  conformément au contrat dans chaque encodeur concerné.
- Le layout automatique est pris en charge pour les pipelines compute et render.
- Les champs `occlusionQuerySet` et `timestampWrites` sont transmis lorsqu’ils
  sont fournis et pris en charge par les interfaces actuelles.
- Les constantes de spécialisation vertex, fragment et compute sont transmises.
- Les usages d’une texture correspondent exactement aux bits du masque natif JS.
- Un rejet de `mapAsync()` respecte le contrat `Result<Unit>` et ne contourne
  pas ce retour par une exception non traitée ; l’annulation des coroutines est
  examinée séparément des erreurs GPU.
- Les compilation hints sont traités selon le contrat actuel, sans chemin public
  se terminant par un `TODO()`.

Les fonctionnalités optionnelles restent soumises aux capabilities du device.
La conversion ne doit pas supprimer silencieusement un champ fourni.

### Précisions issues de l’inventaire pour le plan

L’API actuelle comporte aussi `setImmediates`, cinq limites supplémentaires
(`maxImmediateSize`, les limites de storage buffers et textures par stage vertex
et fragment), `GPUTextureViewDescriptor.usage` et `swizzle`, ainsi que les unions
texture/texture view des attachments. L’extraction doit satisfaire ce contrat
actuel. Les records WebIDL des constantes et des limites doivent être des objets
JavaScript à propriétés énumérables, pas des instances de `Map`. La nullabilité
des paramètres des bindings doit permettre les appels nullable du contrat commun.

Le plan utilisera un petit build de tests navigateur autonome sous
`integration-tests/browser`. Il testera d’abord les projets via un composite
build, puis les mêmes imports publics depuis un dépôt Maven local isolé.
Cette organisation sépare les tests GPU réels des tests d’interop sous Node.

## Stratégie de validation proposée

1. Tests de génération pour les packages, les imports entre modules et les
   chemins de sortie ; une nouvelle génération conserve l’organisation validée.
2. Tests JS et Wasm d’interop et de conversion, utilisant des objets JavaScript
   contrôlés pour vérifier les valeurs transmises, les valeurs nulles et les rejets.
3. Tests navigateur sous Chromium pour les deux cibles : acquisition de device,
   exécution d’un calcul avec lecture mémoire et rendu simple dans un canvas.
4. Les tests de fonctionnalités optionnelles vérifient la capability avant de
   s’exécuter et signalent explicitement leur éventuelle non-exécution. L’absence
   de WebGPU fait échouer le job dédié aux tests GPU requis.
5. Vérification des tests existants de l’API et des nouvelles références dans
   les snapshots ABI affectés par les changements de packages.
6. Publication locale puis compilation d’un consommateur JS/Wasm à partir des
   artefacts publiés : résolution des dépendances transitives, imports publics,
   création d’un descripteur et appel du point d’entrée navigateur.

Le job GPU doit disposer d’un Chromium configuré avec un backend WebGPU utilisable.
Le plan détaillé devra préciser sa configuration et les tâches Gradle exactes.
Les tests de conversion n’exigent pas de matériel GPU.

## Publication et documentation

Les quatre artefacts partagent la politique de version actuelle. Les workflows
snapshot et release publient `webgpu-web-bindings` et `webgpu-browser` avec l’API
et les descripteurs. Les descriptions POM distinguent les responsabilités.

Les guides anglais et français, les exemples d’import, les dépendances Maven,
la documentation d’architecture et les commandes de test reflètent les quatre
modules. Le parcours utilisateur navigateur commence par `webgpu-browser` ;
`webgpu-web-bindings` est documenté comme accès direct à l’interop.

## Critères d’acceptation

- Les quatre modules utilisent les coordonnées et packages validés.
- L’implémentation navigateur est autonome vis-à-vis des bibliothèques natives.
- Les sources générées et manuscrites compilent avec les imports séparés.
- Les régressions de fidélité réussissent sur JS et Wasm JS.
- Le calcul avec lecture mémoire et le rendu canvas réussissent sous Chromium.
- Un consommateur résout et compile les artefacts issus de la publication locale.
- Les workflows de publication et la documentation exposent le nouveau module.

## Suivi de conception

- [x] Examiner les modules, la génération et les workflows existants.
- [x] Valider le périmètre, les noms, les packages et le group ID.
- [x] Formaliser l’architecture et proposer les critères de validation.
- [x] Faire relire cette conception par l’utilisateur (accord reçu le 27 septembre 2026).
- [ ] Rédiger et faire relire le plan d’implémentation détaillé.

L’utilisateur confie l’exécution à un autre agent dans un autre worktree.
Ce worktree est réservé au brainstorming, à la conception et au plan.
