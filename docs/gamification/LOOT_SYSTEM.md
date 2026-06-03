# Loot System

## Chest Progress

- 5 chest points create 1 simple chest.
- Quests grant a rare chest directly.
- Chests are stored in `chest_inventory`.
- Opening a chest changes status from `unopened` to `opened`.

## Loot Items

MVP item keys:

- `pomme_dragon`
- `petit_cristal`
- `pierre_chaude`
- `plume_douce`
- `rune_ancienne`
- `fragment_oeuf`

Simple chest MVP loot:

- `pomme_dragon`: 2
- `petit_cristal`: 1
- `plume_douce`: 1

Rare chest MVP loot:

- `pomme_dragon`: 8
- `petit_cristal`: 6
- `pierre_chaude`: 1
- `rune_ancienne`: 3
- `fragment_oeuf`: 1
- Grants `oeuf_braise` if missing.

## Eggs

MVP egg keys:

- `oeuf_braise`
- `oeuf_lunaire`
- `oeuf_racine`

Hatching `oeuf_braise` consumes:

- 3 `pomme_dragon`
- 2 `petit_cristal`
- 1 `pierre_chaude`

Hatching creates or unlocks `dragon_braise`.

## Dragon Evolution

`dragon_braise` stages:

- `baby`
- `young`
- `guardian`

Baby to young consumes:

- 5 `pomme_dragon`
- 4 `petit_cristal`
- 2 `rune_ancienne`

Young to guardian consumes:

- 10 `pomme_dragon`
- 8 `petit_cristal`
- 5 `rune_ancienne`
