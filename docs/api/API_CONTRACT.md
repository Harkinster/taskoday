# API Contract

Base prefix in the active backend: `/api/v1`.

## Progress

`GET /children/{child_id}/progress`

Returns:

- `guardian.xp`
- `guardian.level`
- `wallet.flammeches` (balance d'Ecailles)
- `nest.level`
- `nest.name`
- `chest_progress.points`
- `chest_progress.points_required`
- `chest_progress.opened_chests`
- `chest_progress.unopened_chests`

## Task Completion

Existing endpoints:

- `POST /routines/{routine_id}/complete`
- `POST /missions/{mission_id}/complete`
- `POST /quests/{quest_id}/complete`

Generic alias:

`POST /children/{child_id}/tasks/{task_id}/complete?task_type=routine|mission|quest`

Rewards:

- routine: +5 XP, +2 Ecailles, +1 chest point
- mission: +15 XP, +6 Ecailles, +3 chest points
- quest: +30 XP, +12 Ecailles, +1 rare chest

## Chests

- `GET /children/{child_id}/chests`
- `POST /children/{child_id}/chests/{chest_id}/open`

Chest statuses:

- `unopened`
- `opened`

## Inventory

- `GET /children/{child_id}/inventory`

Items contain:

- `key`
- `title`
- `rarity`
- `quantity`

## Eggs

- `GET /children/{child_id}/eggs`
- `POST /children/{child_id}/eggs/{egg_id}/hatch`

Egg statuses:

- `locked`
- `available`
- `ready`
- `hatched`

## Dragons

- `GET /children/{child_id}/dragons`
- `POST /children/{child_id}/dragons/{dragon_id}/evolve`

Dragon stages:

- `baby`
- `young`
- `guardian`

## Ecailles

- `GET /children/{child_id}/flammeches`
- `GET /children/{child_id}/flammeches/history`

Compatibility endpoints/fields:

- `GET /children/{child_id}/scales`
- `GET /children/{child_id}/scales/history`
- `scales_balance`, `flammeches_balance`, `cost_scales`

## Wishes And Scrolls

Create/list parent rewards:

- `POST /children/{child_id}/wishes`
- `GET /children/{child_id}/wishes`

Compatibility:

- `POST /children/{child_id}/rewards`
- `GET /children/{child_id}/rewards`

Request a wish:

- `POST /wishes/{reward_id}/requests`
- `POST /rewards/{reward_id}/requests`

List/decide requests:

- `GET /children/{child_id}/wish-requests`
- `GET /children/{child_id}/reward-requests`
- `PATCH /reward-requests/{request_id}`

Scrolls:

- `GET /children/{child_id}/scrolls`
- `POST /scrolls/{coupon_id}/use`
- `POST /reward-coupons/{coupon_id}/use`
