# Integration Android des assets "Le Nid"

Ce document fixe la facon d'integrer les vrais assets Android sans modifier les ecrans un par un.

## Regles

- Ne jamais referencer directement un `R.drawable.*` depuis un ecran Compose.
- Toujours passer par `NestAssets`.
- Garder des noms Android en `snake_case`, minuscules, sans accent, sans espace.
- Copier les illustrations PNG finales dans `app/src/main/res/drawable-nodpi/`.
- Garder les planches sources et fichiers `ChatGPT Image ...` dans `docs/assetsbase/`.
- Utiliser PNG transparent pour personnages, oeufs, objets, coffres et parchemins.
- WebP reste possible plus tard pour les grands backgrounds optimises.
- Les backgrounds decoratifs doivent avoir une `contentDescription` a `null`.

## Fonctions de mapping

`NestAssets` expose les points d'entree suivants :

- `eggAsset(dragonKey, eggState)`
- `dragonAsset(dragonKey, dragonStage)`
- `artifactAsset(dragonKey)`
- `chestAsset(rarity)`
- `scrollAsset(status)`
- `interfaceAsset(type)`
- `itemAsset(itemKey)`
- `inventoryItemAsset(type)`
- `perchAsset(level)`

Chaque fonction a un fallback local pour eviter tout crash si un asset manque.

## Cles officielles

Dragons : `fulmio`, `sylvyn`, `phenor`, `lunarys`, `pyron`, `chronyx`, `ambrio`, `cristao`.

Etats d'oeuf : `sleeping`, `warm`, `glowing`, `cracked`, `hatching`.

Stades dragon : `baby`, `young`, `medium`, `large`, `legendary`.

Coffres : `common`, `rare`, `epic`.

Parchemins : `pending`, `approved`, `used`, `refused`, `expired` utilise le fallback `refused`.

Perchoirs : niveaux `1` a `5`.

Artefacts legendaires : un futur asset par dragon, au format `asset_artifact_{dragon}_legendary`.

## Patterns attendus

| Categorie | Pattern | Dossier |
| --- | --- | --- |
| Oeufs | `asset_egg_{dragon}_{state}.png` | `res/drawable-nodpi` |
| Dragons | `asset_dragon_{dragon}_{stage}.png` | `res/drawable-nodpi` |
| Coffres | `asset_chest_{rarity}.png` | `res/drawable-nodpi` |
| Parchemins | `asset_scroll_{status}.png` | `res/drawable-nodpi` |
| Interface | `asset_icon_{type}.png` | `res/drawable-nodpi` |
| Items inventaire | `asset_item_{type}.png` | `res/drawable-nodpi` |
| Artefacts legendaires | `asset_artifact_{dragon}_legendary.png` | `res/drawable-nodpi` |
| Perchoirs | `asset_perch_level{level}.png` | `res/drawable-nodpi` |

## Fallbacks actuels

| Besoin | Fallback |
| --- | --- |
| Oeuf absent | `asset_icon_oeuf_verrouille` |
| Dragon absent | `asset_dragon_pyron_baby` |
| Coffre absent | `asset_chest_common` |
| Parchemin absent | `asset_scroll_pending` |
| Interface absente | `asset_icon_nid` |
| Item absent | `asset_icon_cristal` |
| Artefact legendaire absent | `asset_item_star_charm` |
| Perchoir absent | `asset_perch_level1` |
| Background Nid absent | `asset_icon_nid` |
| Background Caverne absent | `asset_icon_nid` |

## Tailles recommandees

| Categorie | Taille cible |
| --- | --- |
| Oeufs | 512 x 512 |
| Dragons baby/young | 768 x 768 |
| Dragons medium/large/legendary | 1024 x 1024 |
| Coffres, parchemins, items | 512 x 512 |
| Icones interface | 256 x 256 |
| Artefacts legendaires | 512 x 512 |
| Perchoirs | 1024 x 1024 |
| Backgrounds futurs | 1440 x 1440 minimum |
