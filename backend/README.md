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
- `GET /api/v1/profile/me`
- `GET /api/v1/children/{child_id}/profile`
- `GET /api/v1/children/{child_id}/xp-history`

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
