# Dragon Lineages

Ce registre documente les 8 familles officielles du Nid. Les lignees sont du lore, du design et de l'attachement. Elles ne verrouillent pas la progression.

## Regle produit

- Une tache terminee donne des ressources generiques.
- Le Gardien choisit ensuite quel Oeuf, Dragon ou Perchoir ameliorer.
- Les categories de taches peuvent inspirer le texte et les bonus, mais ne doivent pas forcer une lignee.

## Progression des Oeufs

| Progression | Etat visuel | Fonction Android |
| --- | --- | --- |
| 0 a 20 % | `sleeping` | `NestAssets.eggAsset(dragonKey, "sleeping")` |
| 21 a 45 % | `warm` | `NestAssets.eggAsset(dragonKey, "warm")` |
| 46 a 75 % | `glowing` | `NestAssets.eggAsset(dragonKey, "glowing")` |
| 76 a 99 % | `cracked` | `NestAssets.eggAsset(dragonKey, "cracked")` |
| 100 % | `hatching` | `NestAssets.eggAsset(dragonKey, "hatching")` |

## Progression des Dragons

| Stade visuel | Fonction Android |
| --- | --- |
| `baby` | `NestAssets.dragonAsset(dragonKey, "baby")` |
| `young` | `NestAssets.dragonAsset(dragonKey, "young")` |
| `medium` | `NestAssets.dragonAsset(dragonKey, "medium")` |
| `large` | `NestAssets.dragonAsset(dragonKey, "large")` |
| `legendary` | `NestAssets.dragonAsset(dragonKey, "legendary")` |

## Lignees officielles

| Dragon | Key | Lignee | Theme | Couleurs | Role lore |
| --- | --- | --- | --- | --- | --- |
| Fulmio | `fulmio` | Tempete | mouvement, elan, defis | bleu electrique, violet, blanc lumineux | canalise l'energie |
| Sylvyn | `sylvyn` | Racine | foyer, rangement, stabilite | vert mousse, brun bois, dore naturel | ancre les habitudes |
| Phenor | `phenor` | Phenix | reprise, perseverance | orange, or, rouge doux | valorise le retour |
| Lunarys | `lunarys` | Lunaire | soir, calme, sommeil | bleu nuit, violet, dore doux | accompagne l'apaisement |
| Pyron | `pyron` | Braise | motivation principale | orange braise, noir chaud, dore | porte la flamme du Nid |
| Chronyx | `chronyx` | Chronos | regularite, series, patience | dore, violet, brun ancien | valorise le temps |
| Ambrio | `ambrio` | Coeur | entraide, famille, gentillesse | dore, rose doux, orange chaud | renforce le lien familial |
| Cristao | `cristao` | Cristal | apprentissage, concentration | bleu cristallin, cyan, violet magique | soutient la clarte |

## Assets attendus par famille

Chaque famille doit fournir :

- `asset_egg_{dragon}_sleeping`
- `asset_egg_{dragon}_warm`
- `asset_egg_{dragon}_glowing`
- `asset_egg_{dragon}_cracked`
- `asset_egg_{dragon}_hatching`
- `asset_dragon_{dragon}_baby`
- `asset_dragon_{dragon}_young`
- `asset_dragon_{dragon}_medium`
- `asset_dragon_{dragon}_large`
- `asset_dragon_{dragon}_legendary`

Les artefacts legendaires futurs suivent le pattern `asset_artifact_{dragon}_legendary` et utilisent `asset_item_star_charm` en fallback.
