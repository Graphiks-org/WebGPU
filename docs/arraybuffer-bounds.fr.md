# Bornes et capacités d’ArrayBuffer

`ArrayBuffer` garde ses tailles publiques en `ULong`, mais chaque implémentation n’adresse qu’une
capacité logique bornée. Cette page est le contrat que l’implémentation et ses tests appliquent. Le
coût mesuré de ces contrôles est dans [Performance CPU d’ArrayBuffer](arraybuffer-performance.fr.md).

## Tailles représentables

| Cible | Capacité logique | Notes |
| --- | --- | --- |
| Web (JS/Wasm) | `Int.MAX_VALUE` octets | l’offset d’un typed array est un `Int` |
| Android | `Int.MAX_VALUE` octets | la capacité d’un `ByteBuffer` direct est un `Int` |
| Native | `Int.MAX_VALUE` octets | encapsulation possédée et **empruntée** ; les grands pointeurs externes exigent un autre schéma d’adressage |
| JVM | `Long.MAX_VALUE` octets | un `MemorySegment` est adressé par un `Long` |

Ce sont les limites des représentations actuelles, pas les capacités théoriques des plateformes, et
elles ne promettent pas qu’une allocation réussit. `allocate(0uL)` et `of(emptyArray)` fonctionnent
sur chaque cible. La JVM peut contenir un segment plus grand qu’un tableau Kotlin ; le convertir
avec `to*Array()` refuse ce cas avec `IllegalArgumentException`.

Les tailles sont calculées dans un type large après une vérification `count <= maximum /
elementWidth`, donc un produit débordant n’est jamais évalué.

## Exceptions

| Condition | Exception |
| --- | --- |
| taille d’allocation ou d’encapsulation non représentable sur la cible | `IllegalArgumentException` |
| plage d’accès hors de la taille déclarée | `IndexOutOfBoundsException` |
| offset non aligné pour un élément plus large qu’un octet | `IllegalArgumentException` |
| taille non divisible par la largeur d’élément, ou nombre d’éléments trop grand pour un tableau Kotlin | `IllegalArgumentException` |
| échec réel d’allocation | l’erreur de la plateforme, pas normalisée en erreur de plage |

## Ordre de validation et opérations vides

Un accès valide la plage, puis l’alignement d’une opération non vide, puis la conversion numérique,
et seulement ensuite touche la mémoire. Une copie en bloc valide toute sa plage une fois, hors de la
boucle de copie. Une écriture rejetée par une précondition laisse toute la mémoire inchangée ; ce
n’est pas une garantie transactionnelle contre des changements externes concurrents.

Une opération de longueur nulle à `offset == size` est valide, et une position au-delà échoue. Les
opérations vides ne déréférencent jamais un pointeur et ne créent jamais une vue typée qui
exigerait un alignement : `setBytes(0uL, byteArrayOf())` et `ArrayBuffer.allocate(0uL).toIntArray()`
sont valides sur chaque cible, et l’implémentation Native alloue une sentinelle d’un octet pour un
tampon possédé de longueur nulle sans jamais l’exposer.

## Portée des garanties

Les contrôles protègent la plage que le tampon déclare. Un pointeur encapsulé doit toujours
désigner une région mémoire réelle, assez grande et vivante pendant toute la durée de l’accès. La
propriété, le mapping/unmapping et la concurrence ne sont pas redéfinis ici. La politique de
partage/copie de `of` et le boutisme sont inchangés : `of` partage le stockage sous-jacent en JS et
copie sur les autres cibles, et RGBA8 est écrit comme quatre octets plutôt qu’un `Int` à ordre
implicite, parce que l’ordre des octets d’Android diffère.

## Preuves

Le contrat est couvert par les tests communs de `webgpu-api` (helpers arithmétiques, allocation,
bornes scalaires et en bloc, conversions vides) plus les tests de plateforme pour le `MemorySegment`
JVM et le pointeur Native. Le plugin KMP de bibliothèque Android n’expose pas de tâche de test
unitaire, donc `commonTest` ne s’exécute jamais sur la cible Android : la matrice instrumentée
Android dans `arraybuffer-android-instrumentation` (`ArrayBufferAndroidBusinessCases`, appelée par
`runSafetyChecks`) reflète ces cas contre le vrai `ByteBuffer` direct sur ART et s’exécute sur
chaque pull request. Tout cas ajouté à `commonTest` doit y être reflété. Lancez-les avec :

```sh
./gradlew :webgpu-api:jvmTest :webgpu-api:jsNodeTest :webgpu-api:wasmJsNodeTest :webgpu-api:macosArm64Test
./gradlew :arraybuffer-android-instrumentation:connectedReleaseAndroidTest
```
