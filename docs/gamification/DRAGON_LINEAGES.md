# Dragon Lineages

Ce registre documente les 8 familles officielles du Nid. Il decrit le mapping visuel, l'energie associee et les intentions de progression, sans modifier les regles metier backend.

## Progression des oeufs

| Progression | Etat visuel | Fonction Android |
| --- | --- | --- |
| 0 a 20 % | `sleeping` | `NestAssets.eggAsset(dragonKey, "sleeping")` |
| 21 a 45 % | `warm` | `NestAssets.eggAsset(dragonKey, "warm")` |
| 46 a 75 % | `glowing` | `NestAssets.eggAsset(dragonKey, "glowing")` |
| 76 a 99 % | `cracked` | `NestAssets.eggAsset(dragonKey, "cracked")` |
| 100 % | `hatching` | `NestAssets.eggAsset(dragonKey, "hatching")` |

Apres eclosion, le dragon commence au stade `baby`.

## Progression des dragons

| XP de lignee | Stade visuel | Fonction Android |
| --- | --- | --- |
| 0 | `baby` | `NestAssets.dragonAsset(dragonKey, "baby")` |
| 500 | `young` | `NestAssets.dragonAsset(dragonKey, "young")` |
| 1500 | `medium` | `NestAssets.dragonAsset(dragonKey, "medium")` |
| 3500 | `large` | `NestAssets.dragonAsset(dragonKey, "large")` |
| 7000 | `legendary` | `NestAssets.dragonAsset(dragonKey, "legendary")` |

## Lignes officielles

| Dragon | Key | Lignee | Energie | Taches associees | Progression | Couleurs | Role gameplay |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Fulmio | `fulmio` | Tempete | `storm` | Activite physique, defi dynamique, mission rapide | Gagne avec les taches d'activite, sport et mouvement | Bleu electrique, violet, blanc lumineux | Canalise l'energie et encourage l'action |
| Sylvyn | `sylvyn` | Racine | `root` | Rangement, maison, aide familiale | Gagne avec missions maison et routines de rangement | Vert mousse, brun bois, dore naturel | Ancre les habitudes familiales |
| Phenor | `phenor` | Phenix | `phoenix` | Reprise apres pause, retour apres oubli, perseverance | Gagne surtout via bonus de retour, jamais via punition | Orange, or, rouge doux | Favorise la reprise douce |
| Lunarys | `lunarys` | Lunaire | `lunar` | Routine du soir, coucher, calme | Gagne avec les routines du soir | Bleu nuit, violet, dore doux | Accompagne le calme et le sommeil |
| Pyron | `pyron` | Braise | `ember` | Progression generale, routines simples, motivation principale | Dragon de depart recommande | Orange braise, noir chaud, dore | Porte la progression principale du Gardien |
| Chronyx | `chronyx` | Chronos | `chronos` | Series, regularite, habitudes | Gagne via flamme de suite et journees completes | Dore, violet, brun ancien | Valorise la patience et la constance |
| Ambrio | `ambrio` | Coeur | `heart` | Entraide, gentillesse, comportement positif | Gagne via missions d'aide et encouragements parent | Dore, rose doux, orange chaud | Renforce les gestes familiaux positifs |
| Cristao | `cristao` | Cristal | `crystal` | Devoirs, lecture, apprentissage | Gagne via quetes d'apprentissage et devoirs | Bleu cristallin, cyan, violet magique | Soutient concentration et apprentissage |

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

Tous ces assets sont presents dans `docs/assetsbase/` et copies dans `app/src/main/res/drawable-nodpi/`.
