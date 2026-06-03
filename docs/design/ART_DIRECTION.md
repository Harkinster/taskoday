# Taskoday Art Direction

## Identity

Taskoday is an Android Jetpack Compose app with a soft fantasy productivity skin. The gamification layer should feel like a small magical family sanctuary, not a separate game engine.

Official vocabulary:

- Le Nid: main progression hub.
- Gardien: progression identity for the child.
- XP du Gardien: progression only, never spent.
- Flammeches: external Souhait currency only.
- Cristaux: internal progression resource for Coffres.
- Coffres: loot containers earned or opened through progression.
- Loot items: materials used for Oeufs, Dragons and Perchoirs.
- Oeufs: hatchable collection items.
- Dragons: companions unlocked by hatching.
- Perchoir: place for the active companion in Le Nid.
- Caverne aux Souhaits: parent-created real rewards.
- Souhait: child request for a real reward.
- Parchemin: approved real reward coupon.

## Visual Rules

- Keep the soft fantasy UI kit: warm parchment surfaces, ember orange, soft gold, magic violet, night blue, wood brown and moss green.
- Do not introduce a game engine, Unity-like HUD or heavy animated scene for the MVP.
- Le Nid should be the first app signal after login.
- Real rewards must feel separate from progression: use Flammeches copy and avoid XP wording in the Caverne aux Souhaits.
- Dragons and Oeufs should use the real raster assets through `NestAssets`.
- Assets must never be referenced directly from Compose screens with `R.drawable`.

## Screen Tone

- Le Nid: calm, encouraging, progression-focused.
- Inventory: readable list of items and quantities.
- Oeufs: incubator-like, clear material progress.
- Dragons: collection, companion choice and evolution state.
- Caverne aux Souhaits: parent/child transaction surface, restrained and explicit.
- Parchemins: simple redemption tracking.
