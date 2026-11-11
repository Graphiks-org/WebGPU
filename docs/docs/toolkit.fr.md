# Le toolkit WebGPU

Le toolkit est la couche optionnelle par-dessus les bindings WebGPU : il prend en charge le cycle
de vie des sessions, la présentation et le contrat d’extension qui permet à un dépôt de binding de
fournir les mêmes helpers. Ce guide présente les coordonnées des modules, les points d’entrée de
session et de présentation, le contrat d’extension et la validation d’une extension avec le kit de
contrat.

## Ajouter un module

Les coordonnées publiées utilisent le groupe `org.graphiks.webgpu`. Choisissez les modules nécessaires :

```kotlin
dependencies {
    implementation("org.graphiks.webgpu:toolkit-core:<version>")
    // Contrats de présentation et de fenêtrage.
    // implementation("org.graphiks.webgpu:toolkit-surface:<version>")
    // Extension navigateur présentant vers des canvas HTML.
    // implementation("org.graphiks.webgpu:toolkit-web:<version>")
    // Kit de contrat de protocole.
    // implementation("org.graphiks.webgpu:toolkit-tck:<version>")
}
```

Remplacez `<version>` par une version release ou snapshot disponible. La valeur de développement
du dépôt est `0.1.0-SNAPSHOT` ; elle ne signifie pas qu’une publication existe déjà.

`webgpu-toolkit-core`, `webgpu-toolkit-surface` et `webgpu-toolkit-tck` suivent la matrice de
cibles commune (JVM, Android, JS, Wasm JS et les cibles Kotlin/Native configurées) ;
`webgpu-toolkit-web` configure JS et Wasm JS. Les points d’entrée se trouvent dans
`org.graphiks.webgpu.toolkit`, les helpers de présentation dans
`org.graphiks.webgpu.toolkit.surface`, l’extension navigateur dans
`org.graphiks.webgpu.toolkit.web` et le kit de contrat dans `org.graphiks.webgpu.toolkit.tck`.

## Ouvrir une session headless

`withSession` ouvre une session via une extension, exécute le bloc avec le périphérique actif et
arrête tout sur chaque chemin de sortie, y compris l’annulation de l’appelant :

```kotlin
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUQueue
import org.graphiks.webgpu.toolkit.WebGpuToolkitExtension
import org.graphiks.webgpu.toolkit.withSession

suspend fun <R> headless(
    extension: WebGpuToolkitExtension,
    block: suspend (GPUDevice, GPUQueue) -> R,
): R =
    withSession(extension) { session ->
        session.state.value // `Ready` : le périphérique est actif et admet du travail
        block(
            session.device, // emprunté : la session le ferme, les appelants jamais
            session.queue, // la file principale de session.device
        )
    }
```

Le démarrage est borné par un délai et chaque échec passe par l’état `Failed` avant d’être
relevé. `session.state` publie le cycle de vie au fil de sa progression : `Ready`, puis
`ShuttingDown` pendant la libération des handles, jusqu’à `Closed` ; une perte de périphérique
publie `Lost` et annule le travail admis avec `DeviceLostException`. Une session dont l’arrêt
dépasse son délai publie `ShutdownTimedOut` pendant que le nettoyage continue ; un nouvel appel
à `shutdown()` retente la libération.

## Présenter vers une cible

`withSurface` exécute le protocole de lease de cible — retenir une lease, demander un adaptateur
lié à la cible, négocier la configuration de surface depuis les capacités — et remet au bloc la
session et la surface obtenue :

```kotlin
import org.graphiks.webgpu.toolkit.surface.withSurface
import org.graphiks.webgpu.toolkit.web.CanvasSurfaceTarget
import org.graphiks.webgpu.toolkit.web.webToolkit

suspend fun presentToCanvas(canvas: HTMLCanvasElement) {
    withSurface(webToolkit(), CanvasSurfaceTarget(canvas)) { session, surface ->
        surface.configuration // StateFlow<PresentationConfiguration?> : format et mode alpha négociés
        surface.withFrame { frame ->
            // rendre dans frame.view à la taille frame.extent avec session.device
            frame.present() // optionnel : withFrame présente au retour normal
        }
    }
}
```

`withFrame` suspend tant que la cible n’est pas dessinable — indisponible ou d’étendue nulle — et
libère la frame sur chaque chemin de sortie ; un bloc qui retourne normalement voit sa frame
présentée exactement une fois sauf présentation explicite, et un second `present()` échoue. Sur le
web, `webToolkit()` sert la façade de l’implémentation navigateur et `CanvasSurfaceTarget(canvas)`
enveloppe un canvas du type `org.graphiks.webgpu.browser.HTMLCanvasElement` : la cible observe le
canvas avec un `ResizeObserver` et republie son étendue — en changeant de génération — quand le
canvas est redimensionné, si bien que le flux `state` émet réellement et qu’un `withFrame` suspendu
reprend ; quand le navigateur n’offre pas de `ResizeObserver`, chaque lecture du `state` de la cible
rafraîchit l’étendue à la place (le repli par tirage). Un changement d’étendue incrémente néanmoins
la génération, ce qui tue en v1 les surfaces louées contre l’ancienne : leur prochain usage échoue
immédiatement, et une nouvelle passe `withSurface` négocie une nouvelle surface. L’étendue publiée
est la taille du tampon de dessin du canvas — des pixels physiques : la cible ne redimensionne
jamais le canvas, donc pour un rendu net sur écrans denses, l’application le dimensionne avec le
rapport de densité de pixels (`canvas.width = largeurCss * devicePixelRatio`, et la hauteur
correspondante). La validation
navigateur de l’extension web arrive avec la démo toolkit dans un changement futur ;
aujourd’hui l’extension est couverte par les tests unitaires de son module.

## Ouvrir une fenêtre

Sur les cibles bureau, un fournisseur `WindowHost` ouvre des `Window` neutres vis-à-vis du
fenêtrage. `withWindow` compose la durée de vie de la fenêtre, la session et la surface négociée :

```kotlin
import org.graphiks.webgpu.toolkit.surface.WindowConfig
import org.graphiks.webgpu.toolkit.surface.withWindow

suspend fun runInWindow(extension: WebGpuToolkitExtension, host: WindowHost) {
    withWindow(extension, host, WindowConfig(title = "toolkit")) { session, window, surface ->
        window.events // demandes de fermeture et redimensionnements publiés par la fenêtre
        surface.withFrame { frame ->
            // rendre dans frame.view à la taille frame.extent
            frame.present()
        }
    }
}
```

Ce dépôt définit uniquement le contrat de fenêtrage : la manière d’ouvrir concrètement une
fenêtre relève de la discrétion de chaque extension native — les hôtes sont des artefacts
fournisseurs publiés là où vit leur extension, jamais ici. Quand l’hôte possède ses
fenêtres (`WindowRuntime.Owned`), `withWindow` détruit la fenêtre en sortie ; une fenêtre empruntée
(`WindowRuntime.Borrowed`) reste ouverte, sa durée de vie appartenant à l’embedder qui l’a prêtée.

## Le contrat d’extension

Une extension est tout ce que fournit un backend ; le contrat `WebGpuToolkitExtension` tient en
cinq membres :

1. `id` nomme le binding servi par l’extension, tel que `"web"`.
2. `open(config)` démarre une `ToolkitInstance`, la fabrique d’adaptateurs et de surfaces de la
   session.
3. `requestAdapter(hints)` acquiert un adaptateur ; `requestAdapter(forTarget, hints)` en acquiert
   un capable de présenter vers la cible retenue exacte.
4. `createSurface(adapter, lease)` crée le handle de surface d’une cible retenue : il publie ses
   capacités et configure, acquiert, présente et libère les frames.
5. `progression` et `pumpEvents` disent comment les événements de l’hôte avancent : les backends
   `Pumped` sont pompés par la session, les backends `EventLoop` suivent la boucle de l’hôte (la
   progression `Manual` est refusée en v1).

## Où vivent les adaptateurs

Le toolkit embarque le contrat et l’extension web. Les dépôts dawn4k et wgpu4k publient leurs
extensions toolkit aux côtés de leurs bindings ; ce dépôt ne fournit que l’extension web
(`webToolkit()` dans `webgpu-toolkit-web`). Les hôtes de fenêtrage suivent le même partage :
rien ici ne nomme ni ne prescrit un système de fenêtrage — chaque extension native ouvre ses
fenêtres comme elle l’entend et publie son artefact d’hôte à ses côtés.

## Valider une extension avec la TCK

`webgpu-toolkit-tck` exécute les cas de protocole que toute extension doit respecter.
`ProtocolTck.runAll(extension, target)` renvoie un `TckReport` dont la liste `failures`
porte une ligne par cas violé au lieu de lever à la première erreur, afin qu’un dépôt de binding
l’exécute contre sa propre extension dans ses tests et voie toutes les violations d’un coup.
Le `SimulatedToolkitExtension` du kit est la contrepartie scriptable qui prouve les chemins
stricts dans ce dépôt (parcours de cycle de vie, arrêt idempotent, présentation unique, suspension
à étendue nulle, repli de lease périmée, perte vers `Lost`) ; les cas qui demandent du scripting
s’omettent pour les fixtures incapables de les scripter, si bien qu’un rapport vide certifie les
cas exécutés. Les cas GPU réels — présenter via une pile graphique vivante — sont pour la v2.

Voir [Architecture](architecture.md) pour les frontières des modules et [Démarrer](getting-started.md)
pour l’API navigateur sous-jacente.
