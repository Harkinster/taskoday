# Taskoday - Contrat inscription parent/enfant

Date: 2026-05-16

## Objectif
L'app Android envoie maintenant des infos differentes selon le type de compte a creer.

## Endpoints attendus

### 1) Inscription parent
- `POST /api/v1/auth/register-parent`
- Body JSON:

```json
{
  "email": "parent@example.com",
  "password": "password123",
  "family_name": "Famille Martin",
  "birth_date": "1988-01-20"
}
```

- Validation minimale:
  - `email` requis, format email.
  - `password` requis, min 8 caracteres.
  - `family_name` requis, non vide.
  - `birth_date` requis, format date ISO (`YYYY-MM-DD`).
- Reponse succes (`201`):

```json
{
  "access_token": "jwt",
  "token_type": "bearer",
  "expires_in": 3600,
  "role": "PARENT"
}
```

### 2) Inscription enfant
- `POST /api/v1/auth/register-child`
- Body JSON:

```json
{
  "email": "enfant@example.com",
  "password": "password123",
  "display_name": "Lina",
  "birth_date": "2014-07-03"
}
```

- `birth_date` est optionnel (`YYYY-MM-DD`).
- Validation minimale:
  - `email` requis, format email.
  - `password` requis, min 8 caracteres.
  - `display_name` requis, non vide.
  - `birth_date` optionnel, format date ISO.
- Reponse succes (`201`):

```json
{
  "access_token": "jwt",
  "token_type": "bearer",
  "expires_in": 3600,
  "role": "CHILD"
}
```

## Endpoints deja utilises apres inscription
- `POST /api/v1/auth/login`
- `GET /api/v1/auth/me`

Format `GET /api/v1/auth/me` attendu:

```json
{
  "id": 42,
  "email": "user@example.com",
  "role": "PARENT",
  "is_active": true,
  "family_ids": [1]
}
```

## Codes d'erreur recommandes
- `400` payload invalide.
- `401` credentials invalides (login).
- `403` utilisateur inactif/non autorise.
- `409` email deja utilise, ou nom de famille deja pris.

## Notes importantes pour compatibilite app
- Le role doit etre exactement `PARENT` ou `CHILD`.
- Les noms des champs JSON sont sensibles:
  - parent: `family_name`, `birth_date`
  - enfant: `display_name`, `birth_date`
- Les reponses `register-*` et `login` doivent garder le meme schema `TokenResponse`.
