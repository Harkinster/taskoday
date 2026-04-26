# Import du skin Taskoday dans le projet Android

## 1. Copier les assets

Copie les PNG du dossier :

```txt
assets/logo/
```

vers :

```txt
app/src/main/res/drawable-nodpi/
```

Renomme si besoin :

```txt
taskoday_mascot_transparent.png
taskoday_wordmark_transparent.png
taskoday_logo_full_transparent.png
```

## 2. Copier le thème Compose

Copie les fichiers :

```txt
android-compose/theme/
android-compose/components/
```

dans ton projet, par exemple :

```txt
app/src/main/java/com/taskoday/ui/theme/
app/src/main/java/com/taskoday/ui/components/
```

Puis adapte les lignes `package com.taskoday...` selon le package réel de ton projet.

## 3. Encapsuler l'application

Dans ton `MainActivity` ou ton root Compose :

```kotlin
TaskodayTheme {
    TaskodayApp()
}
```

## 4. Remplacer progressivement les anciens composants

Priorité :

1. Header
2. Bottom navigation
3. Cards
4. Progress bars
5. Boutons
6. Écrans Accueil / Missions / Quêtes / Profil

## 5. Ne pas importer les screenshots comme fonds

Les images du dossier `references/` sont uniquement des références visuelles pour Codex / Windsurf.
Le rendu final doit être reconstruit en Compose.
