# Deployment Ubuntu - Taskoday Backend

Ce guide deploie le backend FastAPI Taskoday sur Ubuntu avec MariaDB et Apache en reverse proxy.

## 1) Structure recommandee sur le serveur

```text
/opt/taskoday/
  backend/                 # code backend (ce dossier)
    app/
    alembic/
    requirements.txt
    alembic.ini
    .env
  logs/
    taskoday-api/
```

Utilisateur systeme recommande:
- utilisateur: `taskoday`
- groupe: `taskoday`
- service lance sous cet utilisateur (pas root)

## 2) Installation systeme (Ubuntu)

```bash
sudo apt update
sudo apt install -y python3 python3-venv python3-pip git mariadb-server apache2 curl
```

Optionnel (hardening de base):
```bash
sudo apt install -y ufw fail2ban
```

## 3) Preparation utilisateur + dossiers

```bash
sudo useradd --system --create-home --shell /bin/bash taskoday
sudo mkdir -p /opt/taskoday
sudo mkdir -p /opt/taskoday/logs/taskoday-api
sudo chown -R taskoday:taskoday /opt/taskoday
```

## 4) Deploiement du code backend

Exemple si vous deployez depuis un repo Git:

```bash
sudo -u taskoday -H bash -lc '
cd /opt/taskoday
git clone <URL_DU_REPO> backend
'
```

Si le repo est deja present, mettez simplement a jour:

```bash
sudo -u taskoday -H bash -lc '
cd /opt/taskoday/backend
git pull
'
```

## 5) Installation Python venv

```bash
sudo -u taskoday -H bash -lc '
cd /opt/taskoday/backend
python3 -m venv .venv
source .venv/bin/activate
pip install --upgrade pip
pip install -r requirements.txt
'
```

## 6) Configuration `.env`

```bash
sudo -u taskoday -H bash -lc '
cd /opt/taskoday/backend
cp .env.example .env
'
```

Editez ensuite:

```bash
sudo -u taskoday -H nano /opt/taskoday/backend/.env
```

Valeurs production minimales recommandees:

```env
APP_NAME=Taskoday API
APP_ENV=production
APP_DEBUG=false
APP_VERSION=0.1.0
API_V1_PREFIX=/api/v1
HOST=127.0.0.1
PORT=8000

DATABASE_URL=mysql+pymysql://taskoday_api:<DB_PASSWORD>@127.0.0.1:3306/taskoday

JWT_SECRET_KEY=<LONG_RANDOM_SECRET>
JWT_ALGORITHM=HS256
JWT_EXPIRE_MINUTES=120
```

## 7) Creation base MariaDB

```bash
sudo mysql
```

Dans le shell MariaDB:

```sql
CREATE DATABASE taskoday CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'taskoday_api'@'127.0.0.1' IDENTIFIED BY 'CHANGE_ME_STRONG_PASSWORD';
GRANT ALL PRIVILEGES ON taskoday.* TO 'taskoday_api'@'127.0.0.1';
FLUSH PRIVILEGES;
EXIT;
```

## 8) Migrations Alembic

```bash
sudo -u taskoday -H bash -lc '
cd /opt/taskoday/backend
source .venv/bin/activate
alembic upgrade head
'
```

Verifier la revision appliquee:

```bash
sudo -u taskoday -H bash -lc '
cd /opt/taskoday/backend
source .venv/bin/activate
alembic current
'
```

## 9) Lancement manuel Uvicorn (test)

```bash
sudo -u taskoday -H bash -lc '
cd /opt/taskoday/backend
source .venv/bin/activate
uvicorn app.main:app --host 127.0.0.1 --port 8000
'
```

Test rapide:

```bash
curl -i http://127.0.0.1:8000/health
```

## 10) Service systemd `taskoday-api.service`

Creez le fichier:

```bash
sudo tee /etc/systemd/system/taskoday-api.service > /dev/null <<'EOF'
[Unit]
Description=Taskoday FastAPI backend
After=network.target mariadb.service
Wants=mariadb.service

[Service]
Type=simple
User=taskoday
Group=taskoday
WorkingDirectory=/opt/taskoday/backend
EnvironmentFile=/opt/taskoday/backend/.env
ExecStart=/opt/taskoday/backend/.venv/bin/uvicorn app.main:app --host 127.0.0.1 --port 8000 --workers 2
Restart=always
RestartSec=3
TimeoutStopSec=30
StandardOutput=append:/opt/taskoday/logs/taskoday-api/stdout.log
StandardError=append:/opt/taskoday/logs/taskoday-api/stderr.log

[Install]
WantedBy=multi-user.target
EOF
```

Activer le service:

```bash
sudo systemctl daemon-reload
sudo systemctl enable taskoday-api
sudo systemctl restart taskoday-api
sudo systemctl status taskoday-api --no-pager
```

Logs:

```bash
sudo journalctl -u taskoday-api -f
```

## 11) Apache reverse proxy (exemple)

Activez les modules:

```bash
sudo a2enmod proxy proxy_http headers rewrite ssl
```

Exemple vhost HTTP (a adapter avec votre domaine):

```bash
sudo tee /etc/apache2/sites-available/taskoday-api.conf > /dev/null <<'EOF'
<VirtualHost *:80>
    ServerName api.example.com

    ProxyPreserveHost On
    ProxyRequests Off

    RequestHeader set X-Forwarded-Proto "http"
    RequestHeader set X-Forwarded-Port "80"

    ProxyPass / http://127.0.0.1:8000/
    ProxyPassReverse / http://127.0.0.1:8000/

    ErrorLog ${APACHE_LOG_DIR}/taskoday-api-error.log
    CustomLog ${APACHE_LOG_DIR}/taskoday-api-access.log combined
</VirtualHost>
EOF
```

Activer le site:

```bash
sudo a2ensite taskoday-api.conf
sudo apache2ctl configtest
sudo systemctl reload apache2
```

Quand le domaine est pret, ajoutez TLS:

```bash
sudo apt install -y certbot python3-certbot-apache
sudo certbot --apache -d api.example.com
```

## 12) Commandes de test curl

Health:
```bash
curl -i http://127.0.0.1:8000/health
curl -i http://api.example.com/health
```

Register parent:
```bash
curl -X POST http://api.example.com/auth/register-parent \
  -H "Content-Type: application/json" \
  -d '{
    "email":"parent@example.com",
    "password":"SuperSecret123!",
    "family_name":"Famille Martin"
  }'
```

Login:
```bash
curl -X POST http://api.example.com/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email":"parent@example.com",
    "password":"SuperSecret123!"
  }'
```

Profil:
```bash
curl http://api.example.com/auth/me \
  -H "Authorization: Bearer <ACCESS_TOKEN>"
```

Tests backend (optionnel mais recommande):
```bash
sudo -u taskoday -H bash -lc '
cd /opt/taskoday/backend
source .venv/bin/activate
pytest -q
'
```

## 13) Conseils securite (minimum)

- Passez `APP_ENV=production` et `APP_DEBUG=false`.
- Utilisez un `JWT_SECRET_KEY` long, aleatoire, unique (>= 32 caracteres).
- N'exposez pas MariaDB sur internet (`bind-address=127.0.0.1` ou reseau prive).
- Creez un user DB dedie (deja fait), pas `root`.
- Restreignez les ports avec UFW:
  - `sudo ufw allow OpenSSH`
  - `sudo ufw allow 80/tcp`
  - `sudo ufw allow 443/tcp`
  - `sudo ufw enable`
- Activez HTTPS (Certbot) des que le domaine est configure.
- Sauvegardez la base regulierement (`mysqldump`) + test de restauration.
- Gardez le systeme et les dependances a jour.

## 14) Routine de mise a jour

```bash
sudo -u taskoday -H bash -lc '
cd /opt/taskoday/backend
git pull
source .venv/bin/activate
pip install -r requirements.txt
alembic upgrade head
'
sudo systemctl restart taskoday-api
sudo systemctl status taskoday-api --no-pager
```
