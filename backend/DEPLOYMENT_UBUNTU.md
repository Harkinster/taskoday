# Deploiement Ubuntu - Taskoday API

Ce document couvre un deploiement durable avec preparation de migration SQLite -> MariaDB,
sans casser l'API existante.

## 0) Pre-requis

```bash
cd /opt/taskoday/backend
python3 -m venv .venv
source .venv/bin/activate
pip install -U pip
pip install -r requirements.txt
```

## 1) Etat actuel (audit rapide)

Verifier la base SQLite en production locale:

```bash
cd /opt/taskoday/backend
sqlite3 taskoday_prod.db ".tables"
sqlite3 taskoday_prod.db "SELECT version_num FROM alembic_version;"
.venv/bin/alembic heads
.venv/bin/alembic current --verbose
```

Interpretation:
- si `alembic current` affiche `0001_initial`, la base est alignee.
- si `alembic current` est vide mais que les tables existent deja, il faut faire un `stamp` avant migration.

## 2) Configuration locale actuelle (SQLite)

Exemple `.env` local fonctionnel:

```env
HOST=127.0.0.1
PORT=8060
DATABASE_URL=sqlite:////opt/taskoday/backend/taskoday_prod.db
```

Service systemd attendu:

```ini
ExecStart=/opt/taskoday/backend/.venv/bin/uvicorn app.main:app --host 127.0.0.1 --port 8060 --workers 2
```

## 3) Aligner Alembic sur SQLite (si necessaire)

A executer uniquement si `alembic current` est vide alors que le schema existe deja:

```bash
cd /opt/taskoday/backend
source .venv/bin/activate
DATABASE_URL=sqlite:////opt/taskoday/backend/taskoday_prod.db .venv/bin/alembic stamp head
DATABASE_URL=sqlite:////opt/taskoday/backend/taskoday_prod.db .venv/bin/alembic current --verbose
```

Cette operation ne modifie pas les tables applicatives, elle synchronise seulement l'etat Alembic.

## 4) Preparer MariaDB cible (sans cutover)

Exemple de preparation SQL:

```sql
CREATE DATABASE IF NOT EXISTS taskoday CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'taskoday_api'@'127.0.0.1' IDENTIFIED BY 'CHANGE_ME_STRONG_PASSWORD';
GRANT ALL PRIVILEGES ON taskoday.* TO 'taskoday_api'@'127.0.0.1';
FLUSH PRIVILEGES;
```

## 5) Creer le schema MariaDB via Alembic

Ne pas changer `.env` tout de suite. Utiliser une variable inline:

```bash
cd /opt/taskoday/backend
source .venv/bin/activate
DATABASE_URL='mysql+pymysql://taskoday_api:CHANGE_ME_STRONG_PASSWORD@127.0.0.1:3306/taskoday' .venv/bin/alembic upgrade head
DATABASE_URL='mysql+pymysql://taskoday_api:CHANGE_ME_STRONG_PASSWORD@127.0.0.1:3306/taskoday' .venv/bin/alembic current --verbose
```

## 6) Copier les donnees SQLite -> MariaDB

Script fourni:
- `scripts/migrate_sqlite_to_mariadb.py`

Dry-run obligatoire d'abord:

```bash
cd /opt/taskoday/backend
source .venv/bin/activate
python scripts/migrate_sqlite_to_mariadb.py \
  --source-url 'sqlite:////opt/taskoday/backend/taskoday_prod.db' \
  --target-url 'mysql+pymysql://taskoday_api:CHANGE_ME_STRONG_PASSWORD@127.0.0.1:3306/taskoday' \
  --dry-run
```

Execution reelle (apres validation):

```bash
cd /opt/taskoday/backend
source .venv/bin/activate
python scripts/migrate_sqlite_to_mariadb.py \
  --source-url 'sqlite:////opt/taskoday/backend/taskoday_prod.db' \
  --target-url 'mysql+pymysql://taskoday_api:CHANGE_ME_STRONG_PASSWORD@127.0.0.1:3306/taskoday' \
  --execute
```

## 7) Basculer l'API vers MariaDB (cutover)

1. Sauvegarde locale de prudence:

```bash
cd /opt/taskoday/backend
cp taskoday_prod.db taskoday_prod.db.bak-$(date +%Y%m%d-%H%M%S)
cp .env .env.bak-$(date +%Y%m%d-%H%M%S)
```

2. Mettre `DATABASE_URL` MariaDB dans `.env`.

3. Redemarrer le service:

```bash
sudo systemctl daemon-reload
sudo systemctl restart taskoday-api
sudo systemctl status taskoday-api --no-pager -l
```

4. Smoke tests:

```bash
curl -i http://127.0.0.1:8060/health
curl -i https://harkserv.ddns.net/taskoday-api/health
```

## 8) Rollback rapide

Si un probleme apparait apres bascule:

1. remettre `DATABASE_URL=sqlite:////opt/taskoday/backend/taskoday_prod.db` dans `.env`
2. redemarrer le service

```bash
sudo systemctl restart taskoday-api
```

