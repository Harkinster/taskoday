# Rewards System

## Currency Split

- XP du Gardien is progression only.
- Ecailles are the only currency for external parent-created rewards.
- Loot items are the only materials for eggs and dragon evolution.

## Caverne aux Souhaits

Parents create active rewards for a child:

- `id`
- `title`
- `description`
- `cost_scales` as the backend-compatible field for Ecailles
- `is_active`
- optional visual `emoji`

MVP keeps compatibility with `/rewards` and adds `/wishes` aliases.

## Wish Flow

1. Child can request an active reward if current Ecailles balance covers the cost.
2. Request is created with `pending` status.
3. Pending request does not spend Ecailles.
4. Parent can approve or refuse.
5. Approval checks balance again, spends Ecailles, and creates a Parchemin.
6. Refusal does not spend Ecailles.

Request statuses:

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

## Compatibility

Existing names kept:

- `/children/{child_id}/scales`
- `/rewards/{reward_id}/requests`
- `/reward-coupons/{coupon_id}/use`

New aliases:

- `/children/{child_id}/flammeches` (compatibility route for Ecailles)
- `/children/{child_id}/wishes`
- `/wishes/{reward_id}/requests`
- `/children/{child_id}/scrolls`
- `/scrolls/{coupon_id}/use`
