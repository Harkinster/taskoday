# Taskoday Backend (FastAPI)

Backend API auto-heberge pour Taskoday (Ubuntu + MariaDB), dedie a la gestion des comptes parent/enfant.

## Stack
- FastAPI
- SQLAlchemy 2.x
- Alembic
- MariaDB (driver PyMySQL)
- JWT (`python-jose`)
- Hash mot de passe (`passlib[bcrypt]`)
- Pydantic + `pydantic-settings`
- `python-dotenv`

## Arborescence
```text
backend/
  app/
    api/
      deps/
        auth.py
      routes/
        auth.py
        health.py
    core/
      config.py
      security.py
    db/
      base.py
      session.py
    models/
      base.py
      enums.py
      family.py
      user.py
      child_profile.py
      family_member.py
    schemas/
      auth.py
      health.py
    services/
      auth_service.py
    main.py
  alembic/
    versions/
      20260425_0001_initial_schema.py
    env.py
    script.py.mako
  tests/
    conftest.py
    test_auth.py
    test_health.py
  .env.example
  alembic.ini
  requirements.txt
```

## Installation
Depuis le dossier racine du projet:

```bash
cd backend
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
cp .env.example .env
```

## Configuration MariaDB
Mettre a jour `DATABASE_URL` dans `.env`.

Exemple:
```env
DATABASE_URL=mysql+pymysql://taskoday:taskoday@127.0.0.1:3306/taskoday
```

## Migrations
Appliquer le schema initial:

```bash
alembic upgrade head
```

Generer une nouvelle migration (plus tard):

```bash
alembic revision --autogenerate -m "add something"
```

## Lancement serveur
```bash
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

Endpoint de sante:

```bash
curl http://127.0.0.1:8000/health
```

## Auth endpoints
- `POST /auth/register-parent`
- `POST /auth/login`
- `GET /auth/me` (Bearer token requis)
- `POST /families`
- `GET /families/current`
- `POST /children`
- `GET /children`
- `GET /children/{child_id}`
- `GET /children/{child_id}/planning?date=YYYY-MM-DD`
- `POST /children/{child_id}/routines`
- `POST /children/{child_id}/missions`
- `POST /children/{child_id}/quests`
- `PATCH /planning/{item_type}/{item_id}/complete?date=YYYY-MM-DD`
- `PATCH /planning/{item_type}/{item_id}/uncomplete?date=YYYY-MM-DD`
- `GET /children/{child_id}/points`
- `GET /children/{child_id}/points/history`
- `POST /children/{child_id}/rewards`
- `GET /children/{child_id}/rewards`
- `PATCH /rewards/{reward_id}`
- `POST /rewards/{reward_id}/purchase`
- `GET /children/{child_id}/reward-purchases`

### Exemples curl
Inscription parent:

```bash
curl -X POST http://127.0.0.1:8000/auth/register-parent \
  -H "Content-Type: application/json" \
  -d '{
    "email":"parent@example.com",
    "password":"supersecret123",
    "family_name":"Famille Martin"
  }'
```

Connexion:

```bash
curl -X POST http://127.0.0.1:8000/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email":"parent@example.com",
    "password":"supersecret123"
  }'
```

Profil utilisateur connecte:

```bash
curl http://127.0.0.1:8000/auth/me \
  -H "Authorization: Bearer <ACCESS_TOKEN>"
```

Creer une famille (parent sans famille):

```bash
curl -X POST http://127.0.0.1:8000/families \
  -H "Authorization: Bearer <PARENT_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"name":"Famille Martin"}'
```

Recuperer la famille courante:

```bash
curl http://127.0.0.1:8000/families/current \
  -H "Authorization: Bearer <PARENT_TOKEN>"
```

Ajouter un enfant:

```bash
curl -X POST http://127.0.0.1:8000/children \
  -H "Authorization: Bearer <PARENT_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "email":"child@example.com",
    "password":"childsecret123",
    "display_name":"Emma"
  }'
```

Lister les enfants visibles:

```bash
curl http://127.0.0.1:8000/children \
  -H "Authorization: Bearer <TOKEN>"
```

Voir un enfant:

```bash
curl http://127.0.0.1:8000/children/1 \
  -H "Authorization: Bearer <TOKEN>"
```

Planning d'un enfant pour une date:

```bash
curl "http://127.0.0.1:8000/children/1/planning?date=2026-04-27" \
  -H "Authorization: Bearer <TOKEN>"
```

Ajouter une routine:

```bash
curl -X POST http://127.0.0.1:8000/children/1/routines \
  -H "Authorization: Bearer <PARENT_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "title":"Brosser les dents",
    "day_part":"MATIN",
    "frequency":"DAILY",
    "points_reward":1,
    "is_active":true
  }'
```

Ajouter une mission:

```bash
curl -X POST http://127.0.0.1:8000/children/1/missions \
  -H "Authorization: Bearer <PARENT_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "title":"Apporter le cahier",
    "day_part":"MIDI",
    "scheduled_date":"2026-04-27",
    "points_reward":2,
    "is_active":true
  }'
```

Ajouter une quete:

```bash
curl -X POST http://127.0.0.1:8000/children/1/quests \
  -H "Authorization: Bearer <PARENT_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "title":"Lire 10 min",
    "day_part":"SOIR",
    "points_reward":3,
    "is_active":true
  }'
```

Cocher / decocher un item:

```bash
curl -X PATCH "http://127.0.0.1:8000/planning/routine/1/complete?date=2026-04-27" \
  -H "Authorization: Bearer <TOKEN>"

curl -X PATCH "http://127.0.0.1:8000/planning/routine/1/uncomplete?date=2026-04-27" \
  -H "Authorization: Bearer <TOKEN>"
```

Solde de points:

```bash
curl "http://127.0.0.1:8000/children/1/points" \
  -H "Authorization: Bearer <TOKEN>"
```

Historique de points:

```bash
curl "http://127.0.0.1:8000/children/1/points/history?limit=50" \
  -H "Authorization: Bearer <TOKEN>"
```

Creer une recompense (parent):

```bash
curl -X POST http://127.0.0.1:8000/children/1/rewards \
  -H "Authorization: Bearer <PARENT_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "title":"Choisir le dessert",
    "description":"Bonus du soir",
    "cost_points":10,
    "is_active":true
  }'
```

Lister les recompenses:

```bash
curl "http://127.0.0.1:8000/children/1/rewards" \
  -H "Authorization: Bearer <TOKEN>"
```

Acheter une recompense:

```bash
curl -X POST "http://127.0.0.1:8000/rewards/1/purchase" \
  -H "Authorization: Bearer <TOKEN>"
```

Historique des achats:

```bash
curl "http://127.0.0.1:8000/children/1/reward-purchases" \
  -H "Authorization: Bearer <TOKEN>"
```

## Tests
```bash
pytest -q
```

## Modeles V1
- `User`
- `Family`
- `ChildProfile`
- `FamilyMember`

Roles:
- `PARENT`
- `CHILD`
