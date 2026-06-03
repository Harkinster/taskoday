# Taskoday Game Loop

## Core Loop

1. The child completes a routine, mission, or quest.
2. The backend grants XP du Gardien, Ecailles, and/or chest progress.
3. The child opens coffres from Le Nid or Inventory.
4. Coffres grant loot items and sometimes eggs.
5. Loot items hatch eggs or evolve dragons.
6. Parents create real rewards in the Caverne aux Souhaits.
7. The child requests a reward with enough Ecailles.
8. The parent approves or refuses.
9. Approval spends Ecailles and creates a Parchemin.

## Completion Rewards

| Task type | XP du Gardien | Ecailles | Chest progress |
| --- | ---: | ---: | ---: |
| Routine | 5 | 2 | 1 |
| Mission | 15 | 6 | 3 |
| Quest | 30 | 12 | Rare chest guaranteed |

Rules:

- XP is never spent.
- Ecailles are never used for dragon progression.
- A task completion is idempotent.
- Existing `/scales` and `/flammeches` API names are kept for compatibility, but they represent Ecailles.

## Nest Levels

| Level | Name | Requirement |
| --- | --- | --- |
| 1 | Vieux Nid | Default |
| 2 | Nid reveille | 100 XP |
| 3 | Grotte aux oeufs | 300 XP and at least 1 egg |
| 4 | Sanctuaire du dragon | 600 XP and `dragon_braise` unlocked |
