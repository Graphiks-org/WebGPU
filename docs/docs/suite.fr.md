# Graphiks WebGPU Suite

La Graphiks WebGPU Suite exerce le contrat public Graphiks WebGPU au moyen d'acid tests portables
exécutés sur une implémentation WebGPU réelle dans le navigateur. La page **Validation** présente
l'inventaire du contrat, la couverture des comportements et les résultats des exécutions JS et Wasm
publiées, et permet d'exécuter la suite localement.

[Ouvrir la page Validation](../suite/)

La suite fournit aussi une galerie de démonstrations. Sa première scène, **particules compute**, met
à jour un buffer de particules avec une passe compute et dessine ce même buffer comme vertex buffer
instancié.

[Ouvrir les démos](../suite/demos/)

La suite fournit aussi deux charges de benchmark portables. Elles mesurent `queue.writeBuffer` ainsi
que l'encodage et la soumission d'une charge compute, et publient leurs échantillons bruts avec le
protocole, le profil et l'environnement. Chaque cible et profil est présenté séparément ; une durée
est une observation de soumission CPU et d'achèvement, pas du temps GPU pur.

[Ouvrir les benchmarks](../suite/benchmarks/)

Le protocole et ses limites d'interprétation sont documentés dans
[`docs/benchmarks.md`](https://github.com/Graphiks-org/WebGPU/blob/master/docs/benchmarks.md).

Les résultats publiés proviennent d'exécutions identifiées et restent distincts d'un lancement
local. Les preuves enregistrées se trouvent dans
[`docs/verification.md`](https://github.com/Graphiks-org/WebGPU/blob/master/docs/verification.md).
