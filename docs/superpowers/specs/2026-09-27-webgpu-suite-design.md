# Graphiks WebGPU Suite — Conception

Date : 2026-09-27

Statut : conception approuvée par l’utilisateur, y compris les principes de lisibilité et de validation. Les plans sont rédigés dans cette session pour être transmis à un autre agent chargé de leur exécution. Ce document ne constitue pas un plan d’implémentation.

Actualisation du 2026-09-28 : premier incrément intégré dans `b4d750a` (PR #120). Les choix livrés remplacent les propositions initiales des plans : trois modules (`suite-core`, `suite-acid-tests`, `suite-browser`), onze cas annotés, inventaire généré et page Validation bilingue. `suite-demos`, les particules et `suite-benchmarks` restent à réaliser. Cette actualisation est une lecture du code ; les résultats d’exécution antérieurs sont consignés dans `docs/verification.md`.

Actualisation du 2026-09-29 : la première phase est désormais largement livrée. Les PR #121, #122 et #125 ont ajouté `suite-demos`, la scène de particules et `suite-benchmarks` ; les PR #123, #124, #125 et #126 ont étendu les acid tests à 83 cas. Le plan de finalisation `2026-09-29-webgpu-suite-acid-finalization.md` porte le catalogue à **123 cas** (118 obligatoires, 5 optionnels) et introduit le contrat d’exécution `AcidContext` : `AcidCase.run` reçoit le device emprunté et une factory d’adapter fournie par le binding, et les cas qui doivent créer leur propre device le font et le ferment. Le bilan de couverture et les résidus sont publiés dans `docs/acid-coverage.md` ; les preuves d’exécution JS/Wasm dans `docs/verification.md`. Cette actualisation est une lecture du code ; elle ne vaut pas certification de conformité WebGPU.

## 1. Intention et critères de réussite

Intégrer **Graphiks WebGPU Suite** dans le dépôt actuel `Graphiks-org/webgpu`, sous forme de sous-modules Gradle réutilisant les conventions du dépôt, pour servir trois objectifs, par ordre de priorité :

1. Démontrer la solidité technique du contrat public Graphiks WebGPU.
2. Faciliter son adoption par des exemples pédagogiques et des scènes lisibles.
3. Promouvoir la bibliothèque avec une galerie interactive et des résultats techniques contextualisés.

La suite exerce exclusivement l’API publique commune. Les extensions propres à `wgpu4k`, au futur `dawn4k` ou à d’autres bindings restent dans leurs dépôts respectifs.

La première phase, livrée en plusieurs incréments, doit permettre de consulter un inventaire complet du contrat, d’exécuter des acid tests sur ses fondations sous Kotlin/JS et Kotlin/Wasm JS, de découvrir une démo de particules pilotée par compute et de consulter des benchmarks documentés. Les contenus partagés doivent être publiés comme artifacts consommables par les bindings natifs. L’incrément intégré couvre la validation et son site ; démos et benchmarks restent à réaliser.

Réussir les tests atteste les cas couverts du contrat Graphiks pour les versions et environnements identifiés. Cela ne constitue pas une certification exhaustive de conformité WebGPU.

## 2. État du socle navigateur et préalable de validation

Le travail décrit ici s’inscrit après le chantier navigateur défini dans :

`docs/superpowers/plans/2026-09-27-webgpu-browser.md`

La première phase de rapatriement est intégrée au dépôt dans le commit `f1b0e1b` (`feat(web): add browser implementation, bindings, and four-module publication`, PR #116). L’examen statique du checkout confirme les quatre modules suivants, inscrits dans `settings.gradle.kts` et dans le workflow de publication :

| Artifact | Responsabilité |
| --- | --- |
| `org.graphiks:webgpu-api` | Contrat portable |
| `org.graphiks:webgpu-descriptors` | Implémentations des descripteurs |
| `org.graphiks:webgpu-web-bindings` | Interop JavaScript et JS/Wasm |
| `org.graphiks:webgpu-browser` | Implémentation navigateur et helpers canvas |

`webgpu-browser/build.gradle.kts` configure Kotlin/JS et Kotlin/Wasm JS, avec des environnements browser et Node. L’initialisation publique passe par `org.graphiks.webgpu.browser.requestAdapter(options): Result<Adapter>`, puis `GPUAdapter.requestDevice`. Les helpers `CanvasSurface`, `SurfaceConfiguration`, `SurfaceTexture` et `getCanvasSurface` sont présents dans le package `.browser`.

Le plan navigateur initial prévoyait également des tests de wrappers et conversions, des tests GPU compute/canvas/erreurs et une vérification de consommation Maven. Ces dispositifs ne doivent pas être supposés livrés du seul fait du rapatriement. Le dépôt dispose désormais du workflow GPU `.github/workflows/suite.yml`, appelé par les workflows Tests, Documentation et Publication, qui exécute les acid tests JS/Wasm. Il ne constitue pas une couverture des tests canvas ou internes envisagés dans l’ancien plan. La configuration de publication ne prouve pas à elle seule une publication distante effective.

Ces validations restent de la responsabilité du chantier `webgpu`. La suite apporte une validation portable du contrat public, plutôt qu’une dépendance aux détails internes de ces tests. L’intégration du code navigateur ne vaut donc pas attestation d’achèvement de toutes les vérifications du plan précédent.

Avant l’implémentation de la suite, identifier le commit de référence du dépôt et vérifier l’état des validations restantes. Le développement utilise les dépendances Gradle entre projets locaux et la publication réutilise les conventions existantes. Le plan navigateur sert de référence pour l’état du socle, sans rejouer ses travaux ni importer ses dispositifs de vérification de publication dans la suite. Le présent examen est statique : aucune compilation, exécution GPU ou résolution Maven n’a été effectuée pour cette actualisation documentaire.

## 3. Organisation dans le dépôt et responsabilités externes

### `Graphiks-org/webgpu`

Porte le contrat public, les descripteurs, les bindings web, l’implémentation navigateur, leur documentation de référence, leurs tests internes et d’intégration, ainsi que les modules de la suite.

### Modules Graphiks WebGPU Suite dans ce même dépôt

Portent les acid tests portables, les exemples et scènes de démonstration, les benchmarks, leur inventaire et leurs artifacts partagés. Ils sont inclus dans le `settings.gradle.kts` existant et réutilisent le wrapper, le catalogue de versions et les conventions `kmp` et `publish` de `buildSrc`. Aucun dépôt séparé ni sous-module Git n’est nécessaire.

La CI de ce dépôt exécute la partie navigateur. Le site de la suite est intégré au site public existant, sous une entrée dédiée, avec un déploiement Pages commun pour préserver la documentation de référence.

### Dépôts des bindings natifs

Consomment les artifacts de la suite, fournissent l’initialisation et l’environnement propres à leur implémentation, puis exécutent les contenus sur leurs cibles. Ils conservent leurs workflows, leurs résultats natifs et les contenus propres à leurs extensions.

La première livraison du site publie uniquement les résultats navigateur. La récupération, l’importation et l’agrégation des rapports natifs ne font pas partie de son périmètre initial.

## 4. Modules et dépendances

| Module | Responsabilité | Distribution |
| --- | --- | --- |
| `suite-core` | Contrat minimal d’exécution, identification des cas, contexte fourni par le runner et exigences de capabilities | Artifact partagé |
| `suite-acid-tests` | Cas ciblés, assertions et résultats attendus | Artifact partagé |
| `suite-demos` | Exemples pédagogiques et scènes portables | Artifact partagé |
| `suite-benchmarks` | Charges de travail et définition des mesures | Artifact partagé |
| `suite-browser` | Initialisation navigateur, exécution et présentation web | Application web |

Les trois modules de contenu dépendent de `suite-core`, sans dépendre les uns des autres. Ils utilisent l’API commune et les descripteurs lorsque nécessaires. Aucun ne dépend d’un binding concret, du DOM ou d’un système de fenêtres natif.

`suite-browser` consomme les modules partagés et `webgpu-browser` par des dépendances Gradle entre projets. Il fournit l’adaptation au canvas et l’environnement web. Les consommateurs externes utilisent les artifacts publiés.

Les artifacts partagés utilisent la politique de version commune du dépôt (`releaseVersion`, défaut `0.1.0-SNAPSHOT`) et sa convention de publication. Les deux coordonnées intégrées sont `org.graphiks:suite-core` et `org.graphiks:suite-acid-tests`, avec les cibles JVM 25, JS, Wasm JS, Linux x64 et macOS ARM64. Les deux autres artifacts concernent les futurs modules de démos et benchmarks. Chaque livraison documente la version de l’API contre laquelle elle est construite.

Les builds actuels de `webgpu-api` et `webgpu-descriptors` déclarent JS, Wasm JS, JVM, Android et plusieurs cibles Kotlin/Native (iOS, watchOS, macOS ARM64, Linux ARM64/x64, Windows x64 et Android Native ARM64/x64). Cette liste constitue le point de départ de la matrice de publication, pas une preuve de disponibilité des artifacts pour chaque cible. Un binding natif peut être consommé depuis JVM ou Kotlin/Native : la suite doit préciser les cibles Kotlin qu’elle publie, sans assimiler « backend natif » à « cible Kotlin/Native ».

La mutualisation de ressources ou de scènes supplémentaires sera introduite lorsqu’un besoin réel apparaît. Aucun module partagé supplémentaire n’est requis par défaut. Les exemples doivent rester lisibles et les acid tests minimaux.

## 5. Frontière entre contenus portables et runners

Le runner fournit l’initialisation de l’implémentation, les objets du contrat nécessaires à l’exécution et, pour une scène graphique, une cible de rendu compatible. Il gère également les interactions et la présentation propres à sa plateforme.

Les contenus portables expriment les opérations GPU, les attentes et les charges de travail au moyen de l’API publique commune. Les types canvas spécifiques à `webgpu-browser` restent dans le runner navigateur.

L’implémentation intégrée expose sa propre interface externe minimale `org.graphiks.webgpu.browser.HTMLCanvasElement`, et non directement un type DOM Kotlin wrappers. L’adaptation depuis le DOM utilisé par le site appartient à `suite-browser`. `CanvasSurface.preferredCanvasFormat` est nullable : le runner doit traiter explicitement l’absence de format utilisable. Les textures acquises par `getCurrentTexture()` sont empruntées au canvas ; `present()` est un no-op et `close()` déconfigure la surface. Ces particularités restent locales au runner web et ne deviennent pas des exigences des scènes portables.

Le contrat d’exécution doit expliciter :

- les capabilities et limites nécessaires à chaque cas ;
- les ressources fournies par le runner et celles créées par le cas ;
- la responsabilité de fermeture des ressources et le nettoyage après échec ;
- les paramètres reproductibles, notamment la graine et la taille d’une simulation ;
- la remontée des résultats et des diagnostics.

Un cas libère les ressources qu’il possède. Il ne détruit pas implicitement les ressources empruntées au runner. Une perte de device ou une impossibilité d’initialisation doit être signalée et ne peut pas produire un succès apparent.

Le contrat livré est `AcidCase(id: AcidCaseId, family: AcidFamily, contract: List<String>, requiredFeatures: Set<GPUFeatureName>, run: suspend (GPUDevice) -> Unit)`. Les titres et attentes ne font pas partie de ce type : le site les résout depuis les ressources localisées. Le runner navigateur acquiert un adapter et un device par cas. Les futurs contrats de scènes et de benchmarks restent à définir à partir de leurs usages réels.

## 6. Inventaire complet et couverture progressive

L’inventaire couvre le contrat public de la version de référence dès la première phase. Il distingue les comportements déjà testés de ceux restant à tester. Les fonctionnalités hors périmètre commun ne sont pas intégrées à cet inventaire.

La liste des types, méthodes et propriétés publiques sert de base de traçabilité. La couverture porte toutefois sur des comportements observables : la présence d’un test pour une méthode ne prouve pas que tous ses comportements sont couverts.

Chaque entrée identifie :

- la partie du contrat concernée et le comportement attendu ;
- les capabilities ou limites requises ;
- les acid tests associés, lorsqu’ils existent ;
- les exemples, démos ou benchmarks associés, lorsqu’ils existent.

Les démos et benchmarks ne comptent pas automatiquement comme preuves de validation. Une évolution du contrat entraîne une revue de l’inventaire correspondant.

Les métadonnées sont portées par `@AcidTest`. `AcidCaseId` et `AcidFamily` sont des enums ; les références au contrat utilisent les constantes générées `ApiSymbols`. Chaque famille définit son `packageName` et chaque cas réside dans son propre fichier sous le package correspondant.

Les textes sont dans `inventory/i18n/behaviours.en.json` et `.fr.json`, sous `cases`, `families` et `behaviours`, indexés par les identifiants sérialisés (par exemple `buffers.mapped-at-creation`), et non par le nom Kotlin de l’enum. Les comportements sans cas sont décrits dans `inventory/uncovered-behaviours.json`, avec leurs attentes dans les ressources localisées.

La génération livrée comporte deux étapes :

1. Le plugin `org.graphiks.webgpu-suite-inventory` de `build-logic`, appliqué à `suite-acid-tests`, fournit `generateSuiteInventory`. Il écrit `ApiSymbols.kt` et `FoundationCases.kt` sous `suite-acid-tests/build/generated/suite/commonMain/kotlin/`, ainsi que `cases.json`, `foundation-case-ids.json` et `baseline.json` sous `suite-acid-tests/build/suite-inventory/`.
2. `tools/build-inventory.mjs`, appelé par `tools/build-site.mjs`, assemble les sources de l’API, les manifestes générés, les comportements non couverts et les textes localisés pour produire l’inventaire publié sous `build/site/inventory/`.

Ces sorties ne sont pas versionnées. Leur génération sert la compilation et la présentation des usages ; elle n’ajoute pas de tests d’architecture. Les ressources actuelles décrivent 42 comportements : 11 associés aux cas et 31 sans cas. Ce recensement ne signifie pas que toutes les combinaisons de comportements possibles sont identifiées ou couvertes.

### Couverture et exécution

La couverture décrit l’existence de cas de validation pour un comportement. Le résultat d’exécution décrit ce qui s’est réellement produit dans un environnement donné. Ces deux informations sont affichées séparément.

Les résultats distinguent au minimum :

- **Réussi** : le cas a été exécuté et ses attentes sont satisfaites.
- **Échoué** : le cas a été exécuté et une attente n’est pas satisfaite, ou son exécution a échoué.
- **Non pris en charge** : une capability requise et déclarée est absente, avec une raison explicite.
- **Non exécuté** : aucune exécution exploitable n’est disponible, avec une raison lorsqu’elle est connue.

L’absence d’une fonctionnalité exigée par le contrat applicable ne doit pas être masquée par un statut de capability optionnelle. Une indisponibilité de WebGPU dans un environnement CI censé l’exécuter constitue un problème d’exécution, pas une réussite ni un skip silencieux.

## 7. Première phase : fondations

La couverture est approfondie par familles, en commençant par les fondations. Le rendu avancé et les autres familles sont visibles dans l’inventaire avant que leurs tests soient développés.

| Domaine | Priorités de validation |
| --- | --- |
| Buffers | Création, usages, mapping/unmapping, plages accessibles et cycle de vie |
| Transferts | Écritures via la queue, copies entre buffers, offsets, tailles et intégrité des données |
| Compute | Pipelines, bind groups, dispatch, lecture des résultats, layouts automatiques/explicites et constantes de spécialisation |
| Erreurs | Error scopes, absence d’erreur, erreurs de validation attendues et résultats asynchrones selon le contrat |

Les attentes privilégient les données mémoire déterministes, les catégories d’erreurs et les états observables. Les textes des erreurs peuvent être conservés à des fins de diagnostic sans exiger leur égalité entre implémentations.

Les exemples pédagogiques de cette phase montrent les entrées, les opérations GPU et les résultats obtenus avec un code Kotlin lisible.

## 8. Première démo visuelle : particules pilotées par compute

La première phase comprend une scène de particules dont les positions et vitesses sont mises à jour par compute puis affichées par une passe de rendu.

La présentation propose :

- le réglage du nombre de particules dans les limites de l’environnement ;
- la mise en pause et la réinitialisation ;
- une explication courte du chemin compute vers rendu ;
- un accès au code Kotlin de la scène.

La scène reste portable. Le runner gère la cible de rendu, les interactions et la boucle d’affichage. Les buffers, bind groups, pipelines et commandes de la scène utilisent l’API commune.

Cette démo introduit le rendu nécessaire à sa présentation sans élargir la première phase à une validation approfondie de toutes les fonctions de rendu. Son fonctionnement visuel ne remplace pas les acid tests.

La charge pourra être réutilisée pour un benchmark lorsque son protocole est défini ; son affichage interactif ne constitue pas à lui seul une mesure reproductible.

## 9. Benchmarks

Les premières charges portent sur les transferts et la soumission de workloads compute. Chaque benchmark décrit ce qui est inclus et exclu de la mesure.

Les mesures distinguent, selon le cas :

- la préparation des ressources ;
- le temps CPU de soumission ;
- le temps d’achèvement observé, qui ne doit pas être présenté comme un temps GPU pur ;
- le temps GPU lorsque les capabilities et le protocole le permettent.

Le protocole précise les tailles de charge, le warm-up, les répétitions, les points de synchronisation et la méthode de synthèse des résultats. Les chemins JS et Wasm sont identifiés séparément.

Les publications indiquent les versions de la suite, de l’API et du binding, ainsi que l’environnement disponible : navigateur, plateforme, adapter/backend et caractère logiciel ou matériel lorsqu’identifiable.

Le runner `tools/run-browser.mjs` configure Chromium avec les flags SwiftShader pour la validation fonctionnelle. Les mesures qui y seraient réalisées doivent être explicitement identifiées comme logicielles et ne représentent pas les performances d’un GPU matériel. Des mesures réalisées dans des environnements différents ne sont pas présentées comme une comparaison contrôlée.

## 10. Site public

Le site de la suite est publié depuis le dépôt `webgpu`, sous le chemin `suite/` du site existant, et comporte trois entrées :

### Validation

Inventaire du contrat, couverture des comportements, résultats des acid tests, versions et environnements testés, limitations connues. Les résultats JS et Wasm sont distincts. Les rapports publiés représentent des exécutions identifiées ; ils ne sont pas confondus avec un éventuel lancement interactif sur la machine du visiteur.

### Démos

Galerie interactive avec une explication et un accès au code pour chaque scène. La première scène visuelle est la simulation de particules. Un environnement navigateur incompatible donne lieu à un message explicite.

### Benchmarks

Protocoles, conditions de mesure et résultats navigateur contextualisés. L’interface ne réduit pas la présentation à un classement sans contexte.

Le site complète la documentation de référence et y renvoie. Le workflow Pages existant assemble la documentation et la suite avant un déploiement unique. Il ne centralise pas les résultats natifs dans cette première livraison.

## 11. Vérification de la livraison

La livraison de la suite devra apporter les preuves suivantes :

1. Les contenus partagés utilisent uniquement le contrat commun et sont indépendants des bindings concrets.
2. Les modules partagés sont compilés sur les cibles annoncées et intégrés à la convention et au workflow de publication existants pour leur utilisation par les bindings externes.
3. Les acid tests initiaux passent sous JS et Wasm dans un navigateur disposant réellement de WebGPU ; les échecs et absences de capacités sont explicitement rapportés.
4. La démo de particules fonctionne et ses commandes de pause, réinitialisation et changement de charge sont vérifiées.
5. Les benchmarks publient des mesures conformes à leur protocole et identifient leur environnement.
6. Le site expose l’inventaire complet, y compris les zones non couvertes, et relie les résultats aux versions testées.

Les exécutions GPU natives appartiennent aux dépôts des bindings. La suite ne crée pas de projet consommateur artificiel ni de scénario dédié à vérifier la publication ou la résolution Maven.

## 12. Principes de lisibilité et de validation

Le code produit doit privilégier la lisibilité humaine et la valeur métier : montrer l’usage de WebGPU et vérifier les comportements de son implémentation. Les noms, le déroulement des scènes et les attentes des tests doivent rendre cette intention directement compréhensible.

Privilégier des opérations explicites et des abstractions minimales, justifiées par les besoins réels des contenus. Le contexte d’exécution et les helpers partagés doivent faciliter la lecture, sans masquer les opérations GPU derrière une infrastructure générique. Les exemples pédagogiques doivent permettre de suivre la création des ressources, les commandes et le résultat attendu.

Les métadonnées structurées des cas vivent dans le code sous forme d’annotations typées, les textes dans des ressources localisées, et l’inventaire est généré au build. Ce sont de l’outillage de contenu et de build, pas des tests d’architecture : les annotations ne masquent pas les opérations GPU et restent lisibles au plus près du cas.

La stratégie de validation distingue :

- **Compilation et lancement des modules réels** : permettent de constater leur intégration au cours du développement et des exécutions normales, sans créer de projet ou de scénario dédié à tester cette intégration.
- **Tests** : démontrent un usage concret ou vérifient un comportement observable de l’implémentation WebGPU, avec une attente explicite sur les données, les pixels, les erreurs ou les états définis par le contrat.

Ne pas ajouter de tests dédiés à l’architecture mise en place : structure des modules, graphe de dépendances, câblage interne, abstractions de la suite ou chaîne de publication et de consommation Maven. Une compilation de projet artificiel destinée à vérifier cette chaîne reste un test d’architecture et est exclue. Ne pas écrire de tests qui reproduisent simplement l’implémentation. Chaque test doit être justifié par l’usage WebGPU qu’il démontre ou le comportement WebGPU qu’il valide.

## 13. Suites de phases et passage au plan

Après les fondations, étendre progressivement la couverture aux autres familles de comportements recensées, notamment le rendu et les fonctionnalités avancées. Chaque phase enrichit la couverture visible et peut ajouter des exemples, démos ou benchmarks pertinents.

Le présent document est approuvé par l’utilisateur. Les plans d’implémentation sont rédigés dans cette session, en respectant notamment les principes de lisibilité et de validation de la section 12, puis transmis à un autre agent pour exécution. Ils fixent les signatures d’exécution, la matrice de publication, les coordonnées des artifacts et les choix d’outillage du site à partir de l’état effectivement livré par le chantier navigateur. L’approbation de cette spec ne vaut pas approbation des futurs plans ni lancement de l’implémentation.
