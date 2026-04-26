# Taskoday — Skin UI Neon RPG

Ce pack transforme les images de référence en **direction artistique exploitable** pour le projet Android.

## Intention visuelle

Taskoday doit ressembler à une interface de jeu mobile fantasy/RPG, pas à une simple todo list Material Design.

Mots-clés :
- fantasy / RPG / aventure
- bleu nuit / indigo / violet / cyan
- cartes lumineuses
- contours néon
- avatar rond lumineux
- progression, XP, niveau, récompenses
- interface immersive mais lisible

## Écrans de référence

| Fichier | Rôle |
|---|---|
| `references/screens/accueil.png` | Accueil / routines du jour |
| `references/screens/missions.png` | Missions |
| `references/screens/quetes.png` | Quêtes / XP |
| `references/screens/profil.png` | Profil joueur |
| `references/screens/screenbot-logo.png` | Logo / mascotte |

## Découpage en composants

### Composants communs

- `TaskodayHeader`
  - logo / mascotte à gauche
  - wordmark Taskoday
  - notification à droite
  - avatar rond lumineux
  - ligne décorative techno/fantasy

- `FantasyBottomNavigation`
  - fond bleu nuit
  - icônes simples
  - item actif cyan avec glow
  - items inactifs bleu gris/violet

- `NeonCard`
  - fond bleu nuit semi-transparent
  - bordure violette/cyan
  - coins légèrement coupés ou arrondis
  - glow léger
  - contenu lisible

- `XpProgressBar`
  - track bleu sombre
  - remplissage gradient violet → cyan

- `NeonButton`
  - bouton contour néon
  - fond bleu/violet
  - texte blanc/cyan

### Accueil

- salutation : `Bonjour, Alex !`
- carte de progression : `ProgressHeroCard`
- sections de routine : `RoutineSectionCard`
- lignes de tâche : `RoutineItemRow`

### Missions

- sections : `À faire aujourd’hui`, `En cours`, `Terminées`
- carte mission : `MissionCard`
- progression mission : barre linéaire ou cercle
- état : à faire / en cours / terminé

### Quêtes

- carte niveau : `QuestHeroCard`
- carte quête : `QuestCard`
- récompense XP visible
- bouton : récupérer / continuer / commencer

### Profil

- carte profil : `ProfileHeroCard`
- actions : modifier profil / modifier photo
- statistiques : `StatsCard`
- récompenses : `RewardsCard`

## Règle importante

Les images ne doivent pas être utilisées comme fonds statiques.
Elles servent à reproduire le style en composants natifs Android/Compose.

