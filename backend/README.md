# Taskoday Backend (FastAPI)

API backend pour Taskoday.

## Stack

- FastAPI / Uvicorn
- SQLAlchemy / Alembic
- MariaDB (via PyMySQL)
- JWT + bcrypt

## Lancement local

```bash
cd /opt/taskoday/backend
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
cp .env.example .env
uvicorn app.main:app --host 127.0.0.1 --port 8060
```

## Preparation migration SQLite -> MariaDB

Un script de copie est disponible (sans changer les routes API) :

```bash
cd /opt/taskoday/backend
source .venv/bin/activate
python scripts/migrate_sqlite_to_mariadb.py \
  --source-url 'sqlite:////opt/taskoday/backend/taskoday_prod.db' \
  --target-url 'mysql+pymysql://taskoday_api:CHANGE_ME@127.0.0.1:3306/taskoday' \
  --dry-run
```

Procedure complete : voir `DEPLOYMENT_UBUNTU.md`.

## Endpoints principaux

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
- `GET /api/v1/children/{child_id}/scales`
- `GET /api/v1/children/{child_id}/scales/history`
- `GET /api/v1/children/{child_id}/rewards`
- `POST /api/v1/children/{child_id}/rewards`
- `GET /api/v1/children/{child_id}/wishes`
- `POST /api/v1/children/{child_id}/wishes`
- `PATCH /api/v1/rewards/{reward_id}`
- `POST /api/v1/rewards/{reward_id}/requests`
- `POST /api/v1/wishes/{reward_id}/requests`
- `GET /api/v1/children/{child_id}/reward-requests`
- `GET /api/v1/children/{child_id}/wish-requests`
- `PATCH /api/v1/reward-requests/{request_id}`
- `GET /api/v1/children/{child_id}/scrolls`
- `POST /api/v1/scrolls/{coupon_id}/use`
- `POST /api/v1/reward-coupons/{coupon_id}/use`
- `GET /api/v1/profile/me`
- `GET /api/v1/children/{child_id}/profile`
- `GET /api/v1/children/{child_id}/xp-history`

## Boucle du Nid et recompenses externes

- L'XP n'est jamais depensee. Elle sert uniquement a la progression du profil enfant, du dragon et du sanctuaire.
- Les Flammeches sont la monnaie des Souhaits. Les noms techniques legacy `scales` et `cost_scales` restent compatibles.
- Les Cristaux sont la monnaie des coffres de la Caverne.
- Une routine completee rapporte 5 XP, 2 Flammeches, 1 Cristal et 1 point coffre.
- Une mission completee rapporte 15 XP, 6 Flammeches, 3 Cristaux et 3 points coffre.
- Une quete completee rapporte 30 XP, 12 Flammeches, 6 Cristaux et un coffre rare.
- Les coffres achetes coutent 3/8/15 Cristaux pour les raretes commune/rare/epique.
- Un doublon d'oeuf est converti en 5 essences de famille, ou en fragments generiques si l'essence n'existe pas.
- Une recompense est creee par un parent pour un enfant.
- L'enfant peut creer une demande si son solde de Flammeches couvre le cout.
- Le parent peut accepter, refuser ou expirer une demande.
- L'acceptation retire les Flammeches et cree un Parchemin.
- Un Parchemin approuve peut ensuite etre marque comme utilise.
- Statuts de demande: `pending`, `approved`, `refused`, `used`, `expired`.
- Statuts de Parchemin: `available`, `used`, `expired`, `cancelled`.

Les champs/routes `scales`, `flammeches`, `rewards` et `reward-coupons` restent disponibles pour compatibilite backend. Les routes canoniques cote app sont `wishes`, `wish-requests` et `scrolls`.

Creer un souhait parent:

```bash
curl -X POST http://127.0.0.1:8060/api/v1/children/2/wishes \
  -H "Authorization: Bearer <PARENT_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "title":"Choisir le dessert",
    "description":"Bonus du soir",
    "cost_scales":5,
    "emoji":"gift",
    "is_active":true
  }'
```

Demander un souhait enfant:

```bash
curl -X POST http://127.0.0.1:8060/api/v1/wishes/1/requests \
  -H "Authorization: Bearer <CHILD_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"note":"Je voudrais ce soir."}'
```

Accepter une demande et creer le Parchemin:

```bash
curl -X PATCH http://127.0.0.1:8060/api/v1/reward-requests/1 \
  -H "Authorization: Bearer <PARENT_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"status":"approved"}'
```

Marquer un Parchemin comme utilise:

```bash
curl -X POST http://127.0.0.1:8060/api/v1/scrolls/1/use \
  -H "Authorization: Bearer <PARENT_TOKEN>"
```

## Format de reponse

Succes :

```json
{
  "success": true,
  "data": {},
  "message": null
}
```

Erreur :

```json
{
  "success": false,
  "error": {
    "code": "FORBIDDEN",
    "message": "Action non autorisee."
  }
}
```
