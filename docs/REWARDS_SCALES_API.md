# Taskoday Rewards And Flammeches API

## Principles

- XP is never spent.
- XP is reserved for Gardien, sanctuary, levels and profile history.
- `scales` and `flammeches` remain backend compatibility names.
- The user-facing currency is Flammeches.
- Flammeches are only spent on parent-created Souhaits.
- Loot items, Cristaux and consumables are used for Coffres, Oeufs, Dragons and Perchoirs.
- Approval spends Flammeches and creates a Parchemin.

## Completion Rewards

- Routine: +5 XP, +2 Flammeches, +1 chest point.
- Mission: +15 XP, +6 Flammeches, +3 chest points.
- Quest: +30 XP, +12 Flammeches, rare chest guaranteed.

The target balancing can also award Cristaux without changing legacy endpoints.

## Statuses

Reward request statuses:

- `pending`
- `approved`
- `refused`
- `used`
- `expired`

Parchemin statuses:

- `available`
- `used`
- `expired`
- `cancelled`

## Endpoints

All endpoints below are under `/api/v1`.

### Flammeches

`GET /children/{child_id}/flammeches`

Compatibility: `GET /children/{child_id}/scales`

```json
{
  "success": true,
  "data": {
    "child_id": 2,
    "balance": 20,
    "currency": "flammeches"
  },
  "message": null
}
```

`GET /children/{child_id}/flammeches/history?limit=100`

Compatibility: `GET /children/{child_id}/scales/history?limit=100`

### Wishes

`POST /children/{child_id}/wishes`

Compatibility: `POST /children/{child_id}/rewards`

Parent only.

```json
{
  "title": "Choisir le dessert",
  "description": "Bonus du soir",
  "cost_scales": 10,
  "emoji": "gift",
  "is_active": true
}
```

`cost_scales` is a compatibility field that means Flammeches.

`GET /children/{child_id}/wishes`

Compatibility: `GET /children/{child_id}/rewards`

Parent or child. Children only see active Souhaits.

`PATCH /rewards/{reward_id}`

Parent only.

### Requests

`POST /wishes/{reward_id}/requests`

Compatibility: `POST /rewards/{reward_id}/requests`

Child only.

```json
{
  "note": "Je voudrais ce soir."
}
```

The request stays `pending`; Flammeches are not spent yet.

`GET /children/{child_id}/wish-requests`

Compatibility: `GET /children/{child_id}/reward-requests`

Parent or child.

`PATCH /reward-requests/{request_id}`

Parent only.

```json
{
  "status": "approved",
  "parent_note": "OK pour ce soir."
}
```

Allowed decision statuses from `pending`: `approved`, `refused`, `expired`.
Approval checks the balance again, spends Flammeches and creates a Parchemin.

### Scrolls

`GET /children/{child_id}/scrolls`

Lists approved Parchemins.

`POST /scrolls/{coupon_id}/use`

Compatibility: `POST /reward-coupons/{coupon_id}/use`

Parent only. Marks the request as `used`, sets Parchemin status to `used`, and fills `used_at`.
