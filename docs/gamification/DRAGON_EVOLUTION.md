# Dragon Evolution

## Principe

Les Dragons evoluent parce que le Gardien choisit de leur donner des ressources. Une tache ne doit pas verrouiller automatiquement une lignee precise.

Les lignees restent importantes pour le lore, les couleurs, les assets et l'attachement emotionnel, mais pas pour bloquer la progression.

## Oeufs

La progression d'un Oeuf represente une avancee materielle :

| Etat | Progression | Sens |
| --- | ---: | --- |
| `sleeping` | 0 a 20 % | Oeuf decouvert |
| `warm` | 21 a 45 % | Oeuf rechauffe |
| `glowing` | 46 a 75 % | Oeuf nourri par les materiaux |
| `cracked` | 76 a 99 % | Oeuf presque pret |
| `hatching` | 100 % | eclosion possible |

Sources possibles :

- consommables communs ou rares.
- fragments d'Oeuf.
- bonus doux donne par une quete.

## Dragons

Stades cibles :

| Stade | Usage |
| --- | --- |
| `baby` | premier compagnon |
| `young` | dragon en apprentissage |
| `medium` | dragon stable |
| `large` | dragon puissant |
| `legendary` | dragon legendaire |

Compatibilite :

- Le backend actuel connait `baby`, `young`, `guardian`.
- `guardian` doit rester lisible comme ancien equivalent de fin de progression.
- L'UI Android dispose deja des assets `medium`, `large`, `legendary`.

## Evolution choisie

Le Gardien peut investir :

- consommables.
- Cristaux si le balancing le permet.
- XP dragon dediee si elle est ajoutee plus tard.

La progression recommandee :

| Passage | Materiaux recommandes |
| --- | --- |
| `baby -> young` | common + uncommon |
| `young -> medium` | uncommon + rare |
| `medium -> large` | rare + plusieurs Cristaux |
| `large -> legendary` | `star_charm` + artefact legendaire |

## Artefacts legendaires

Chaque Dragon peut recevoir un artefact legendaire futur :

- `asset_artifact_fulmio_legendary`
- `asset_artifact_sylvyn_legendary`
- `asset_artifact_phenor_legendary`
- `asset_artifact_lunarys_legendary`
- `asset_artifact_pyron_legendary`
- `asset_artifact_chronyx_legendary`
- `asset_artifact_ambrio_legendary`
- `asset_artifact_cristao_legendary`

Tant que ces assets n'existent pas, Android doit utiliser `asset_item_star_charm`.

## Compagnon actif

L'ecran Dragons doit permettre :

- bouton `Definir comme compagnon`.
- badge `Compagnon actif`.

Le Nid affiche :

- le Dragon actif.
- son Perchoir.
- un Oeuf prioritaire si aucun Dragon actif n'est choisi.
- des actions douces pour investir les objets.

## Perchoirs

Les Perchoirs ont 5 niveaux :

| Niveau | Sens visuel |
| --- | --- |
| 1 | perchoir simple |
| 2 | perchoir renforce |
| 3 | perchoir chaleureux |
| 4 | perchoir magique |
| 5 | perchoir de compagnon legendaire |

Ils s'ameliorent avec des consommables comme `wood_logs`, `leaf_sprout`, `lantern`, `magic_book` et `gold_coin`.
