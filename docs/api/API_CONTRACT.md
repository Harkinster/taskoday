# API Contract

Base prefix in the active backend: `/api/v1`.

## Progress

`GET /children/{child_id}/progress`

Returns:

- `guardian.xp`
- `guardian.level`
- `wallet.flammeches`
- `nest.level`
- `nest.name`
- `chest_progress.points`
- `chest_progress.points_required`
- `chest_progress.opened_chests`
- `chest_progress.unopened_chests`
- `chests[].crystal_cost` when returned by chest endpoints

## Task Completion

Existing endpoints:

- `POST /routines/{routine_id}/complete`
- `POST /missions/{mission_id}/complete`
- `POST /quests/{quest_id}/complete`

Generic alias:

`POST /children/{child_id}/tasks/{task_id}/complete?task_type=routine|mission|quest`

Current backend rewards:

- routine: +5 XP, +2 Flammeches, +1 chest point
- mission: +15 XP, +6 Flammeches, +3 chest points
- quest: +30 XP, +12 Flammeches, +1 rare chest

Target balancing adds Cristaux and keeps direct chest rewards compatible.

## Chests

- `GET /children/{child_id}/chests`
- `POST /children/{child_id}/chests/{chest_id}/open`

Chest statuses:

- `unopened`
- `opened`

Compatibility:

- backend `simple` should display as Coffre commun.
- backend `rare` should display as Coffre rare.
- target `epic` is documented but not required by old clients.

## Inventory

- `GET /children/{child_id}/inventory`

Items contain:

- `key`
- `itemKey`
- `title`
- `rarity`
- `usage`
- `quantity`

## Eggs

- `GET /children/{child_id}/eggs`
- `POST /children/{child_id}/eggs/{egg_id}/hatch`

Egg statuses:

- `locked`
- `available`
- `ready`
- `hatched`

Target progression is material based and can later expose `progress`.

## Dragons

- `GET /children/{child_id}/dragons`
- `POST /children/{child_id}/dragons/{dragon_id}/evolve`

Current backend stages:

- `baby`
- `young`
- `guardian`

Target visual stages:

- `baby`
- `young`
- `medium`
- `large`
- `legendary`

`guardian` remains a compatibility value and should be displayed as an advanced Dragon stage.

## Flammeches

- `GET /children/{child_id}/flammeches`
- `GET /children/{child_id}/flammeches/history`

Compatibility endpoints/fields:

- `GET /children/{child_id}/scales`
- `GET /children/{child_id}/scales/history`
- `scales_balance`, `flammeches_balance`, `cost_scales`

Legacy `scales` names represent Flammeches and must not be shown in user-facing Android copy.

## Wishes And Scrolls

Create/list parent Souhaits:

- `POST /children/{child_id}/wishes`
- `GET /children/{child_id}/wishes`

Compatibility:

- `POST /children/{child_id}/rewards`
- `GET /children/{child_id}/rewards`

Request a Souhait:

- `POST /wishes/{reward_id}/requests`
- `POST /rewards/{reward_id}/requests`

List/decide requests:

- `GET /children/{child_id}/wish-requests`
- `GET /children/{child_id}/reward-requests`
- `PATCH /reward-requests/{request_id}`

Parchemins:

- `GET /children/{child_id}/scrolls`
- `POST /scrolls/{coupon_id}/use`
- `POST /reward-coupons/{coupon_id}/use`
