# Exécuter la Graphiks WebGPU Suite

[English](running.md)

La suite vérifie le contrat public Graphiks WebGPU avec des acid tests portables exécutés sur une
implémentation WebGPU réelle dans le navigateur. `suite-core` définit le contrat d’exécution,
`suite-acid-tests` contient les cas et `suite-browser` fournit le runner web. Les modules de contenu
sont aussi publiés sous forme d’artefacts pour les bindings natifs.

## Prérequis

- JDK 25 et le wrapper Gradle du dépôt pour compiler les modules et les distributions.
- Node.js 22 pour le runner Chromium. La version de Playwright est fixée dans `tools/package.json`.
  Installez le navigateur attendu avec `npm exec --prefix tools -- playwright install chromium`.

## Version et cibles des artefacts

L’API et la suite partagent la version du dépôt (`releaseVersion`, par défaut `0.1.0-SNAPSHOT`).
Le fichier généré `baseline.json`, publié sous `suite/inventory/baseline.json`, contient la version
de l’API, la révision de référence et les empreintes des sources pour identifier les résultats.

Les artefacts `org.graphiks:suite-core`, `org.graphiks:suite-acid-tests`, `org.graphiks:suite-demos`
et `org.graphiks:suite-benchmarks` déclarent les cibles suivantes :

| Cible | Compilation |
| --- | --- |
| JVM 25 | Compilation explicite dans le workflow CI de la suite |
| JS (navigateur) | Compilation et exécution navigateur |
| Wasm JS (navigateur) | Compilation et exécution navigateur |
| Linux x64 | `:suite-core:compileKotlinLinuxX64`, `:suite-acid-tests:compileKotlinLinuxX64`, `:suite-demos:compileKotlinLinuxX64` et `:suite-benchmarks:compileKotlinLinuxX64` sur un hôte compatible |
| macOS ARM64 | `:suite-core:compileKotlinMacosArm64`, `:suite-acid-tests:compileKotlinMacosArm64`, `:suite-demos:compileKotlinMacosArm64` et `:suite-benchmarks:compileKotlinMacosArm64` sur un hôte compatible |

Ne supposez pas la prise en charge d’une cible absente de cette liste. Le runner navigateur exécute
JS et Wasm JS. Les bindings consommateurs exécutent les tests GPU natifs dans leurs propres dépôts.

Cette matrice décrit les cibles déclarées, pas une publication distante réussie. Le workflow de la
suite compile explicitement JVM et les distributions navigateur. `check` ne dépend pas explicitement
de toutes les tâches de compilation des cinq cibles.

## Inventaire généré

La compilation navigateur lance `:suite-acid-tests:generateSuiteInventory`, fourni par le plugin
`org.graphiks.webgpu-suite-inventory` de `build-logic`. Il génère `ApiSymbols.kt` et
`FoundationCases.kt` sous `suite-acid-tests/build/generated/suite/commonMain/kotlin/`, ainsi que le
manifeste des cas et le baseline sous `suite-acid-tests/build/suite-inventory/`.

`tools/build-inventory.mjs`, appelé par `tools/build-site.mjs`, combine ces sorties avec les sources
de l’API, `inventory/uncovered-behaviours.json` et les ressources EN/FR pour produire
`build/site/inventory/`. Modifiez les annotations et les ressources sources, pas les fichiers générés.
Consultez [Ajouter un cas](adding-a-case.md) pour la structure et les clés de localisation, et
[la couverture acid](acid-coverage.fr.md) pour les fonctionnalités optionnelles et les limites du contrat testé.

## Exécuter la suite dans le navigateur

Compilez les distributions, puis lancez chaque cible. Le runner écrit `build/reports/<target>.json`
et retourne un code non nul en cas d’exécution incomplète ou d’échec. L’absence d’adaptateur et les
dépassements de délai ne deviennent jamais des réussites.

```sh
npm install --prefix tools
npm exec --prefix tools -- playwright install chromium

./gradlew :suite-browser:jsBrowserDistribution :suite-browser:wasmJsBrowserDistribution
node tools/run-browser.mjs js suite-browser/build/dist/js/productionExecutable
node tools/run-browser.mjs wasm suite-browser/build/dist/wasmJs/productionExecutable
node tools/build-site.mjs
```

`tools/build-site.mjs` assemble `build/site/` avec les deux rapports et les deux distributions
exécutables. L’assemblage échoue si un rapport ou l’inventaire manque. La page Validation présente
séparément JS et Wasm et permet un lancement local sans modifier les résultats publiés.

## Exécuter la démo de particules

La route `?demo=particles` lance une scène portable. `suite-demos` possède ses ressources GPU et le
runner fournit le périphérique, le format de texture et la cible de rendu.

- `?demo=particles&lang=en|fr` ouvre la démo interactive. Le sélecteur propose 256, 1024, 4096,
  16384 et 65536 particules, filtrées par `maxParticleCount(device.limits)`. Le nombre par défaut est
  4096. La réinitialisation reproduit la même disposition pour un nombre donné avec la graine `1u`.
- `?demo=particles&verify=1` lance deux tests GPU et publie `globalThis.graphiksDemoReport`.

Pour vérifier les démos, ajoutez `--demo-check` après compilation :

```sh
./gradlew :suite-browser:jsBrowserDistribution :suite-browser:wasmJsBrowserDistribution
node tools/run-browser.mjs js suite-browser/build/dist/js/productionExecutable --demo-check
node tools/run-browser.mjs wasm suite-browser/build/dist/wasmJs/productionExecutable --demo-check
```

Le collecteur ouvre `?demo=particles&verify=1`, `?demo=reaction-diffusion&verify=1` et
`?demo=instanced-city&verify=1` et `?demo=filter-lab&verify=1` dans des pages distinctes. Les onze cas doivent réussir. Le rapport
agrégé `build/reports/demos-<target>.json` reste séparé des rapports acid.

Une route absente, un JSON invalide, des identités de build incompatibles, des identifiants manquants,
dupliqués ou inattendus, un cas échoué ou une erreur de page entraîne une sortie non nulle.
Les résultats partiels sont conservés comme échecs, jamais remplacés par des réussites inventées.
La galerie `site/demos/` présente les quatre démos avec leurs liens de lancement et leurs sources.
Ce sont des illustrations, pas des benchmarks : elles ne mesurent ni temps GPU ni fréquence
d’affichage et n’établissent aucun classement.

## Exécuter la réaction-diffusion

Le sélecteur Démo en haut des quatre pages permet de passer entre Particules, Réaction-diffusion,
Ville cyberpunk et Filtres d’image en conservant la langue et le chemin JS/Wasm. Le changement recharge la page pour
libérer les ressources GPU de la scène précédente. L’état de simulation n’est pas conservé.

- `?demo=reaction-diffusion&lang=en|fr` ouvre une simulation Gray–Scott sur une grille fixe de
  256 × 256 cellules. Corail, Labyrinthe et Taches choisissent les paramètres d’alimentation et
  d’élimination, puis repartent des mêmes neuf carrés de germes. La souris ou le toucher injecte B
  dans un disque de six cellules de rayon, y compris à travers les bords périodiques et en pause.
  Un seul pointeur est pris en compte, à sa dernière position de chaque frame. Les traits ne sont
  pas interpolés.
- `?demo=reaction-diffusion&verify=1` lance trois tests GPU et publie `globalThis.graphiksDemoReport`.

La vitesse choisit de 1 à 16 étapes fixes **par frame**, avec 8 par défaut. Elle ne représente pas
des secondes physiques. Pause arrête la réaction. Un pas effectue une seule itération et reste en
pause. Réinitialiser restaure l’état sans changer les paramètres ni la palette. Alimentation et
élimination vont de 0 à 0,1. Océan, Braise et Niveaux de gris modifient seulement le rendu.
Les vues A et B montrent chaque concentration séparément.

Le panneau pédagogique explique diffusion, réaction, alternance des textures et passe du pinceau.
Il affiche le WGSL commenté réellement compilé. Deux textures RGBA32Float contiennent `[A, B, 0, 1]`.
Les passes compute lisent l’une et écrivent dans l’autre, puis échangent leurs rôles. `textureLoad`
ne nécessite pas de fonctionnalité optionnelle de filtrage flottant. Le rendu associe B à une couleur.
La perte du périphérique arrête l’animation, désactive les contrôles GPU et propose de recharger.
Sans WebGPU, la page affiche une erreur, sans repli sur une simulation CPU.

Le rapport combiné exige ces onze identifiants :

```text
particles.compute-render-readback
particles.bounds-pause-reset
reaction-diffusion.compute-render-readback
reaction-diffusion.pause-step-reset
reaction-diffusion.brush-boundaries
instanced-city.instances-render-readback
instanced-city.depth-occlusion
instanced-city.camera-resize-lifecycle
filter-lab.compute-readback
filter-lab.boundaries-comparison
filter-lab.reset-source-lifecycle
```

Pour vérifier les données, les contrôles et l’oracle sur CPU :

```sh
./gradlew :suite-demos:jvmTest :suite-browser:jsBrowserTest :suite-browser:wasmJsBrowserTest
```

Utilisez `--demo-check` avec les distributions compilées pour exécuter les tests GPU sur JS et Wasm.

Un binding natif peut utiliser la scène sans le runner DOM :

```kotlin
import org.graphiks.webgpu.suite.demos.reactiondiffusion.ReactionDiffusionScene
import org.graphiks.webgpu.suite.demos.reactiondiffusion.ReactionPreset

// Le binding fournit device, format et targetView et en conserve la propriété.
ReactionDiffusionScene.create(device, format).use { scene ->
    device.createCommandEncoder().use { encoder ->
        scene.encodeFrame(encoder, targetView, width, height, 8, ReactionPreset.Coral.parameters)
        encoder.finish().use { device.queue.submit(listOf(it)) }
    }
}
```

Soumettez les commandes après chaque `encodeFrame`, avant d’encoder une autre frame avec des uniformes
différents. Conservez la scène entre les frames dans une application animée. `reset()` écrit les deux
textures d’état. `encodeStateCopy(encoder, buffer)` copie l’état courant dans un buffer CopyDst emprunté
d’au moins 1 048 576 octets, avec 4096 octets par ligne. La scène ferme ses ressources, jamais le
périphérique fourni. Compiler pour une cible native ne prouve pas une exécution GPU native.

## Exécuter la ville cyberpunk instanciée

- `?demo=instanced-city&lang=en|fr` ouvre une ville nocturne reproductible de 256, 1024 ou
  4096 bâtiments. Seules les densités compatibles avec les limites du périphérique sont proposées.
  La plus grande est sélectionnée au départ.
- `?demo=instanced-city&verify=1` lance trois tests avec lecture GPU : transformations et couleurs
  des instances, occlusion indépendante de l’ordre, caméra et cycle de vie au redimensionnement.

Glissez à la souris ou au toucher pour tourner autour de la ville. Le canvas capture un seul pointeur
et ignore le second. La molette normalise les déplacements exprimés en pixels, lignes et pages.
Le curseur Distance permet le zoom au clavier ou au toucher. Le zoom à deux doigts n’est pas pris
en charge. Hauteur varie de 0,5 à 2 sans remplacer les données des instances.

La rotation automatique démarre activée, sauf si `prefers-reduced-motion` demande moins d’animations.
Vous pouvez l’activer explicitement. Réinitialiser la vue restaure seulement la caméra, sans changer
densité, hauteur ou rotation. Un remplacement de densité échoué conserve l’ancienne scène.
La perte du périphérique arrête l’animation et propose de recharger.

Un cube partagé de 24 sommets et 36 indices et un enregistrement de huit flottants par bâtiment
permettent un seul dessin indexé instancié. Un second dessin produit le sol et les rues, soit
`12 * count + 2` triangles soumis. Ce nombre décrit la géométrie, pas les performances.
`Depth24Plus` détermine l’occlusion. Fenêtres et lignes des rues sont procédurales, sans compute,
textures externes ni bloom. Le filtrage analytique conserve la lumière des fenêtres lointaines.

Le panneau pédagogique, replié par défaut, montre le WGSL compilé et explique les deux dessins.
Les quatre démos partagent une présentation aérée et une coloration sans dépendance des mots-clés,
types, attributs, nombres et commentaires. Les sources sont insérées comme du texte, sans
interprétation HTML, et restent intactes lors de la copie. Le panneau des particules présente
en EN/FR les shaders compute et de rendu instancié.

Les bindings natifs peuvent appeler `InstancedCityScene.create(device, format)`, puis
`encodeFrame(encoder, targetView, width, height, CityCamera(), heightScale, gridStep)`.
Conservez la scène entre les frames et soumettez les commandes après chaque encodage, avant de
changer les uniformes. La scène possède buffers, pipelines et texture de profondeur dépendante
de la taille. Elle emprunte le périphérique, l’encodeur et la vue de couleur. Fermez-la à la fin.
La compilation native ne constitue pas une vérification GPU native.

## Exécuter le laboratoire de filtres d’image

`?demo=filter-lab&lang=en|fr` ouvre une illustration originale. Importez un fichier PNG, JPEG ou WebP
pour expérimenter avec votre propre image. Le fichier reste dans le navigateur, sans téléversement.
Il doit être non vide, valide et ne pas dépasser 20 Mio. La page vérifie le format déclaré, la signature
du fichier et le décodage, pas seulement l’extension. Un import invalide conserve l’image précédente.

L’image de travail conserve ses proportions et est réduite, jamais agrandie, à un côté maximal de
`min(2048, device.limits.maxTextureDimension2D)` pixels. La page affiche ses dimensions et signale
la réduction. Cette limite borne le traitement GPU, pas la mémoire nécessaire au navigateur pour
décoder le fichier original.

Choisissez niveaux de gris, sépia, inversion, flou gaussien ou contours Sobel. L’intensité mélange
l’effet et la source de 0 à 100 %. Le flou utilise un rayon de 1 à 5 pixels, avec sigma égal à la moitié
du rayon. Sobel applique un seuil de 0 à 1 à l’amplitude du gradient divisée par quatre. Les lectures
de voisinage répètent le pixel du bord le plus proche. Chaque réglage repart de la source originale,
sans accumulation des filtres.

La séparation présente l’original à gauche et le résultat à droite. Ses extrêmes affichent l’une
des images seule. Réinitialiser les réglages restaure les niveaux de gris, l’intensité maximale,
le rayon 3, le seuil 0,2 et la séparation à 50 %, sans remplacer l’image. Utiliser l’illustration
remplace uniquement la source.

Les shaders compute traitent les valeurs RGB encodées, sans calcul en lumière linéaire ni gestion
des profils ICC. Ils conservent l’alpha du pixel central. Le shader de rendu compose la transparence
sur un damier et conserve le rapport d’aspect. Le panneau pédagogique EN/FR, replié par défaut,
affiche les deux sources de shaders exactement telles qu’elles sont compilées.

`?demo=filter-lab&verify=1` exécute les trois tests GPU inclus dans `--demo-check`. Les bindings natifs
peuvent utiliser la scène sans dépendance DOM :

```kotlin
import org.graphiks.webgpu.suite.demos.filterlab.FilterLabScene
import org.graphiks.webgpu.suite.demos.filterlab.FilterSettings
import org.graphiks.webgpu.suite.demos.filterlab.embeddedFilterImage

FilterLabScene.create(device, format, embeddedFilterImage()).use { scene ->
    device.createCommandEncoder().use { encoder ->
        scene.encodeFrame(encoder, targetView, width, height, FilterSettings())
        encoder.finish().use { commands -> device.queue.submit(listOf(commands)) }
    }
}
```

Conservez la scène entre les frames dans une application interactive. Soumettez chaque frame encodée
avant de modifier les uniformes. Passez `recompute = false` seulement après avoir calculé les mêmes
réglages, pour redimensionner ou déplacer la séparation. La scène possède textures, buffers, pipelines
et bind groups, mais emprunte le périphérique, l’encodeur et la vue cible. Fermez-la à la fin.
`encodeResultCopy` copie le RGBA8 dans un buffer CopyDst emprunté, avec `resultRowBytes` aligné
à 256 octets et une taille d’au moins `resultCopyBytes`.

## Exécuter les benchmarks

La route `?benchmark=foundations&profile=standard|ci` exécute deux charges portables : écriture de
buffers et encodage/soumission compute. Elle publie `globalThis.graphiksBenchmarkReport`.
`autorun=1` permet au collecteur de démarrer automatiquement. Sans ce paramètre, la page présente
un bouton de lancement. Le protocole, les dix scénarios et les bornes temporelles sont décrits dans
[les benchmarks (en anglais)](benchmarks.md).

Après compilation, ajoutez `--benchmark` :

```sh
./gradlew :suite-browser:jsBrowserDistribution :suite-browser:wasmJsBrowserDistribution
node tools/run-browser.mjs js suite-browser/build/dist/js/productionExecutable --benchmark --profile=ci --backend=swiftshader
node tools/run-browser.mjs wasm suite-browser/build/dist/wasmJs/productionExecutable --benchmark --profile=ci --backend=swiftshader
```

Le collecteur ouvre `?benchmark=foundations&profile=<profile>&autorun=1`. Il exige dix scénarios
`completed`, `outputVerified = true` et le nombre d’échantillons du profil. Il écrit
`build/reports/benchmarks-<target>.json`, séparément des rapports acid et démos. Une durée n’est
jamais un seuil d’échec.

`--profile=ci`, par défaut dans l’outil, conserve 5 échantillons par scénario.
`--profile=standard` en conserve 30. `--backend=swiftshader`, par défaut, force le backend logiciel.
`--backend=default` laisse Chromium choisir et enregistre `default`, pas `hardware`.
Exécutez JS et Wasm l’un après l’autre, jamais en parallèle sur la même machine. La page
`site/benchmarks/` présente séparément les rapports et permet de télécharger les échantillons bruts.

## Utiliser les artefacts depuis un binding

Un binding natif dépend de l’artefact publié et fournit son propre périphérique. `AcidCase.run`
reçoit un `AcidContext` contenant ce périphérique emprunté et une fabrique d’adaptateurs.
Chaque appel de la fabrique doit fournir un adaptateur **neuf**, sans stub : les cas de contexte
(`adapter.request-and-capabilities`, `device.required-limits`, `device.reject-excess-limit`,
`errors.uncaptured-error`) lui demandent leurs propres adaptateur et périphérique.

```kotlin
dependencies {
    implementation("org.graphiks:suite-acid-tests:<version>")
}
```

```kotlin
import org.graphiks.webgpu.GPUAdapter
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPURequestAdapterOptions
import org.graphiks.webgpu.suite.AcidContext
import org.graphiks.webgpu.suite.acid.foundationCases

// Le binding fournit un adaptateur neuf à chaque appel.
suspend fun validateSuppliedDevice(
    device: GPUDevice,
    requestAdapter: suspend (GPURequestAdapterOptions?) -> Result<GPUAdapter>,
) {
    val context = AcidContext(device = device, requestAdapter = requestAdapter)
    for (case in foundationCases()) {
        check(case.requiredFeatures.all { it in device.features })
        case.run(context)
    }
}
```

Les fonctions individuelles acceptent aussi directement un `GPUDevice` pour les bindings qui
souhaitent seulement les cas liés au périphérique :

```kotlin
import org.graphiks.webgpu.suite.acid.buffers.mappedAtCreation

suspend fun checkOne(device: GPUDevice) = mappedAtCreation(device)
```

Le runner choisit l’isolation et gère le cycle de vie du périphérique. Le runner navigateur crée un
adaptateur et un périphérique neufs par cas. Un cas ferme ses propres ressources, jamais celles
fournies par le runner. Un cas de contexte ferme les adaptateur et périphérique supplémentaires
qu’il a lui-même demandés.

La scène `ParticleScene` accepte le périphérique, le format cible et les données initiales :

```kotlin
import org.graphiks.webgpu.suite.demos.particles.ParticleScene
import org.graphiks.webgpu.suite.demos.particles.initialParticles

val scene = ParticleScene.create(device, format, initialParticles(4096))
try {
    val encoder = device.createCommandEncoder()
    try {
        scene.encodeFrame(encoder, targetView, width, height, deltaSeconds)
        val commands = encoder.finish()
        try { device.queue.submit(listOf(commands)) } finally { commands.close() }
    } finally {
        encoder.close()
    }
} finally {
    scene.close()
}
```

Un appel à `encodeFrame` correspond à une soumission. L’application possède et ferme l’encodeur,
la vue cible et le périphérique. La scène possède `particleBuffer`, le buffer de paramètres,
les pipelines et les bind groups. `particleBuffer` est accessible pour composer des lectures,
mais le consommateur ne doit pas le fermer. `deltaSeconds` reste dans `0f..0.05f`. Avec zéro,
la scène dessine l’état courant sans passe compute, ce qui permet un rendu en pause après
réinitialisation ou redimensionnement.

Les fonctions de benchmark empruntent aussi le périphérique, ferment leurs allocations et ne
ferment jamais le périphérique :

```kotlin
import org.graphiks.webgpu.suite.benchmarks.BenchmarkProfile
import org.graphiks.webgpu.suite.benchmarks.benchmarkWriteBuffer

val result = benchmarkWriteBuffer(device, 65536, 16, BenchmarkProfile.Standard)
```

Le binding gère ses scopes d’erreur, délais, erreurs non capturées et métadonnées d’environnement,
comme le runner navigateur. Consultez [les benchmarks (en anglais)](benchmarks.md) pour le protocole et
les limites d’interprétation.

## Statuts

Les rapports distinguent quatre statuts :

- **passed** : le cas s’est exécuté et ses attentes sont satisfaites.
- **failed** : une attente a échoué, l’exécution a levé une exception ou le délai a été dépassé.
- **unsupported** : une fonctionnalité optionnelle requise et déclarée manque, avec un motif.
- **not-run** : aucune exécution exploitable n’est disponible.

La couverture décrit l’existence d’un cas. Le résultat décrit son exécution dans un environnement.
Un comportement sans cas est indiqué comme *à tester*, jamais comme réussi.

## Mesures CPU d’ArrayBuffer

Le module `arraybuffer-benchmarks` exécute des mesures CPU sans périphérique GPU sur JVM, JS, Wasm,
Native et Android. Les commandes figurent dans [Performance CPU d’ArrayBuffer](arraybuffer-performance.fr.md).
Le contrat de bornes est décrit dans [Bornes et capacités d’ArrayBuffer](arraybuffer-bounds.fr.md).
