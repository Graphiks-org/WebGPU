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

Les résultats publiés proviennent d'exécutions identifiées et restent distincts d'un lancement
local. Les preuves enregistrées se trouvent dans
[`docs/verification.md`](https://github.com/Graphiks-org/WebGPU/blob/master/docs/verification.md).
