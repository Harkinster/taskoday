# Asset Inventory

The MVP compiles without final assets. Placeholder Compose cards are allowed until bitmap assets are added.

## Requested Asset Keys

- `nest_vieux_nid`
- `nest_nid_reveille`
- `nest_grotte_aux_oeufs`
- `nest_sanctuaire_dragon`
- `chest_simple`
- `chest_rare`
- `item_pomme_dragon`
- `item_petit_cristal`
- `item_pierre_chaude`
- `item_plume_douce`
- `item_rune_ancienne`
- `item_fragment_oeuf`
- `egg_oeuf_braise`
- `egg_oeuf_lunaire`
- `egg_oeuf_racine`
- `dragon_braise_baby`
- `dragon_braise_young`
- `dragon_braise_guardian`
- `currency_flammeche`
- `scroll_parchemin`

## Current State

- Frontend MVP uses Compose cards in `features/gamification/GamificationScreens.kt`.
- Missing bitmap assets must not block compilation.
- Asset names should stay stable so DTOs and UI references can be wired later.
