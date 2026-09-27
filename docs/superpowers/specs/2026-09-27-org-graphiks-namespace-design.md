# Migration du namespace `io.ygdrasil` vers `org.graphiks`

## Objectif

Faire adopter `org.graphiks` par le code et les artefacts appartenant à ce projet, après le renommage des modules en `webgpu-api`, `webgpu-descriptors`, `webgpu-web` et `webgpu-specifications`.

## Changements prévus

- Remplacer les packages publics `io.ygdrasil.webgpu` par `org.graphiks.webgpu` dans les sources des modules, leurs tests et tout code consommateur interne.
- Mettre à jour les namespaces Android en `org.graphiks.webgpu.ktypes` et `org.graphiks.webgpu.ktypes.descriptors`.
- Changer le groupe Maven du projet de `io.ygdrasil` à `org.graphiks`; les artefacts publiés garderont les noms correspondant aux modules.
- Renommer l’identifiant du plugin Gradle de spécification en `org.graphiks.webgpu-specification-fetcher`, mettre à jour son `implementationClass` et déplacer son package de `io.ygdrasil.webgpu.fetcher` à `org.graphiks.webgpu.fetcher`.
- Mettre à jour le générateur pour qu’il émette `org.graphiks.webgpu`, puis actualiser les sources générées et les snapshots ABI du module `webgpu-api`.
- Garder les coordonnées GitHub et le nom de projet racine inchangés; ils identifient le dépôt et ne sont pas des packages ou groupes de publication.

## Éléments explicitement conservés

La dépendance externe `io.ygdrasil:wgpu4k-native-specs-jvm`, son entrée dans le catalogue Gradle et le filtre de dépôt Sonatype Snapshots qui permet de la résoudre restent inchangés. Ces coordonnées appartiennent à un autre artefact et ne sont pas le groupe publié par ce projet.

## Compatibilité

Le changement est incompatible pour les consommateurs: les imports Kotlin/Java passent de `io.ygdrasil.webgpu.*` à `org.graphiks.webgpu.*`, et les dépendances Maven changent de groupe. Aucun artefact ou alias de compatibilité sous l’ancien package ou l’ancien groupe ne sera ajouté dans cette migration.

## Vérification

- Confirmer qu’aucun package, import, namespace Android, identifiant de plugin, groupe de publication, code généré ou snapshot ABI appartenant au projet ne garde l’ancien préfixe.
- Confirmer que l’exception externe `io.ygdrasil:wgpu4k-native-specs-jvm` et son filtre de résolution restent présents.
- Vérifier les références Gradle et les fichiers générés après la migration. Ne pas ajouter ni lancer de tests dans le cadre de cette tâche.
