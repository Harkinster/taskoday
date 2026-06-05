# Prompt Visual Review

Objectif : analyser les screenshots reels generes par Maestro et corriger uniquement l'UI Android Compose.

## References DA

- `docs/design/references/reference_ui_taskoday_4screens.png`
- `docs/design/references/reference_ui_components_taskoday.png`
- `docs/design/references/reference_screen_nest_taskoday.png`
- `docs/design/references/reference_screen_routine_taskoday.png`
- `docs/design/references/reference_screen_mission_quest_taskoday.png`
- `docs/design/references/reference_ui_popups_taskoday.png`
- `docs/design/references/reference_ui_collection_taskoday.png`

## Screenshots Reels

- `qa/screenshots/00_login.png` (si l'app demarre sans session)
- `qa/screenshots/01_launch.png`
- `qa/screenshots/02_routine.png`
- `qa/screenshots/03_mission.png`
- `qa/screenshots/04_quete.png`
- `qa/screenshots/05_le_nid.png`

## A Detecter

- texte illisible
- texte coupe
- chevauchement
- bouton + qui masque le contenu
- cartes trop grandes ou vides
- images utilisees comme fonds genants
- bottom nav mal alignee
- header illisible
- fond trop pastel
- hierarchie visuelle confuse

## Contraintes

- Ne touche pas au backend.
- Ne modifie pas les endpoints.
- Ne modifie pas les DTO.
- Ne change pas les regles metier.
- Ne change pas la navigation.
- Corrige uniquement l'UI Android Compose.
