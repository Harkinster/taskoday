# Taskoday Backend (FastAPI)

Backend API auto-heberge pour Taskoday. Le backend actif expose ses routes metier sous le prefixe `/api/v1`; seul `/health` reste sans prefixe.

## Stack

- FastAPI
- SQLAlchemy 2.x
- Alembic
- MariaDB / SQLite de test
- JWT
- Pydantic

## Installation locale

```bash
cd backend
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
copy .env.example .env
```

## Migrations

```bash
cd backend
alembic heads
alembic upgrade head
```

Le serveur ne modifie pas le schema au demarrage. Appliquer les migrations explicitement apres validation de la base cible.

## Lancement serveur

```bash
cd backend
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

## Endpoints actifs

- `GET /health`
- `POST /api/v1/auth/register-parent`
- `POST /api/v1/auth/register-child`
- `POST /api/v1/auth/login`
- `GET /api/v1/auth/me`
- `POST /api/v1/families`
- `GET /api/v1/families/me`
- `GET /api/v1/families/{family_id}/children`
- `POST /api/v1/pairing/generate-code`
- `GET /api/v1/pairing/my-code`
- `POST /api/v1/pairing/attach-child`
- `GET /api/v1/children`
- `GET /api/v1/children/{child_id}`
- `PATCH /api/v1/children/{child_id}`
- `GET /api/v1/children/{child_id}/stats`
- `GET /api/v1/children/{child_id}/routines`
- `POST /api/v1/children/{child_id}/routines`
- `PATCH /api/v1/routines/{routine_id}`
- `DELETE /api/v1/routines/{routine_id}`
- `POST /api/v1/routines/{routine_id}/complete`
- `POST /api/v1/routines/{routine_id}/uncomplete`
- `GET /api/v1/children/{child_id}/missions`
- `POST /api/v1/children/{child_id}/missions`
- `PATCH /api/v1/missions/{mission_id}`
- `DELETE /api/v1/missions/{mission_id}`
- `POST /api/v1/missions/{mission_id}/complete`
- `GET /api/v1/children/{child_id}/quests`
- `POST /api/v1/children/{child_id}/quests`
- `PATCH /api/v1/quests/{quest_id}`
- `DELETE /api/v1/quests/{quest_id}`
- `POST /api/v1/quests/{quest_id}/complete`
- `GET /api/v1/children/{child_id}/progress`
- `GET /api/v1/children/{child_id}/flammeches`
- `GET /api/v1/children/{child_id}/flammeches/history`
- `GET /api/v1/children/{child_id}/crystals`
- `GET /api/v1/children/{child_id}/chests`
- `GET /api/v1/children/{child_id}/chests/catalog`
- `POST /api/v1/children/{child_id}/chests/catalog/{catalog_id}/open`
- `POST /api/v1/children/{child_id}/chests/{chest_id}/open`
- `GET /api/v1/children/{child_id}/inventory`
- `GET /api/v1/children/{child_id}/eggs`
- `POST /api/v1/children/{child_id}/eggs/{egg_id}/evolve`
- `POST /api/v1/children/{child_id}/eggs/{egg_id}/hatch`
- `GET /api/v1/children/{child_id}/dragons`
- `POST /api/v1/children/{child_id}/dragons/{dragon_id}/evolve`
- `POST /api/v1/children/{child_id}/dragons/{dragon_id}/activate`
- `GET /api/v1/children/{child_id}/bestiary`
- `GET /api/v1/children/{child_id}/wishes`
- `POST /api/v1/children/{child_id}/wishes`
- `PATCH /api/v1/rewards/{reward_id}`
- `POST /api/v1/wishes/{reward_id}/requests`
- `GET /api/v1/children/{child_id}/wish-requests`
- `PATCH /api/v1/reward-requests/{request_id}`
- `GET /api/v1/children/{child_id}/scrolls`
- `POST /api/v1/scrolls/{coupon_id}/use`
- `GET /api/v1/profile/me`
- `GET /api/v1/children/{child_id}/profile`
- `GET /api/v1/children/{child_id}/xp-history`

## Flux parent/enfant

Un parent cree son compte avec `register-parent`. Un enfant cree son compte avec `register-child`, genere un code avec `pairing/generate-code`, puis le parent l'attache a sa famille via `pairing/attach-child`.

Il n'y a pas de `POST /api/v1/children` dans le backend actif.

## Nid, monnaies et souhaits

- L'XP n'est jamais depensee.
- Les Flammeches servent uniquement aux Souhaits crees par les parents.
- Les Cristaux servent uniquement aux coffres achetes dans la Caverne.
- L'Inventaire regroupe monnaies, objets et coffres possedes non ouverts.
- Le Bestiaire regroupe les etats d'oeuf et les stades de dragon par famille.
- Les Parchemins suivent les Souhaits approuves; les Cristaux ne creent jamais de Parchemin.
- Les champs techniques `scales_balance` et `cost_scales` restent des noms de compatibilite API.
- Les routes canoniques cote app sont `wishes`, `wish-requests` et `scrolls`.
- Les routes `scales`, `rewards` et `reward-coupons` restent exposees comme alias backend de compatibilite.

## Tests

```bash
cd backend
pytest -q
```
