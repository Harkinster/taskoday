# Plan de comparaison et adaptation UI - Taskoday

## Objectif
Adapter toutes les pages Compose au skin Taskoday, avec validation visuelle systématique via screenshots.

## Sources de référence
- `docs/UIREFERENCE/01-screenbot.png` -> Splash
- `docs/UIREFERENCE/02-accueil.png` -> Home
- `docs/UIREFERENCE/03-Missions.png` -> Tasks (Missions)
- `docs/UIREFERENCE/04-quetes.png` -> Quests
- `docs/UIREFERENCE/05-profil.png` -> Settings (Profil)
- `docs/UIREFERENCE/UI_GUIDE.md` -> direction artistique globale

## Périmètre des pages (routes navigation)
- `splash`
- `auth/login`
- `auth/register-parent`
- `home`
- `tasks`
- `quests`
- `shop`
- `settings`
- `parent/planning`
- `week`
- `task_detail/{taskId}`
- `task_edit?taskId={taskId}`

## Méthode de travail
1. Verrouiller le cadre visuel global (thème, typographie, couleurs, glow, spacing).
2. Adapter les composants communs (header, bottom nav, cards, progress, boutons).
3. Adapter les écrans de référence (Splash, Home, Tasks, Quests, Settings).
4. Adapter les écrans secondaires avec les mêmes composants.
5. Vérifier via screenshots après chaque lot.

## Convention screenshots
- Références: `docs/UIREFERENCE/`
- Captures actuelles app: `docs/visual/current/`
- Captures après adaptation: `docs/visual/target/`
- Nommage: `NN-route-state.png` (ex: `03-tasks-default.png`)

## Workflow de comparaison (par page)
1. Ouvrir la page dans un état stable (données seedées, animations finies).
2. Capturer screenshot `current`.
3. Comparer à la référence (structure, spacing, hiérarchie, contrastes, icônes).
4. Corriger la page uniquement avec composants Compose (pas d'image de fond issue des références).
5. Capturer screenshot `target`.
6. Valider avec checklist visuelle.

## Checklist visuelle (Definition of Done)
- Logo/wordmark positionnés correctement.
- Header conforme (avatar, titre, actions).
- Bottom navigation cohérente (icône active/inactive, labels, spacing).
- Cards conformes (rayon, bordure, glow, ombres, paddings).
- Progress bars et badges conformes.
- Typographie cohérente (tailles, poids, contrastes).
- Marges et espacements cohérents entre sections.
- Lisibilité confirmée sur petit et grand téléphone.
- Aucun composant "legacy" non stylé restant sur la page.

## Plan d'exécution par lots

### Lot 0 - Préparation (0.5 jour)
- Vérifier que le thème global est appliqué à la racine de l'app.
- Créer dossiers `docs/visual/current` et `docs/visual/target`.
- Préparer les scénarios de navigation pour screenshots.
- Sortie attendue: base de capture prête.

### Lot 1 - Shell UI commun (1 jour)
- Header global.
- Bottom navigation.
- Primitives visuelles: `NeonCard`, `NeonButton`, `XpProgressBar`.
- Sortie attendue: composants partagés finalisés et utilisés par les pages top-level.

### Lot 2 - Pages de référence (2 jours)
- `splash` vs `01-screenbot.png`
- `home` vs `02-accueil.png`
- `tasks` vs `03-Missions.png`
- `quests` vs `04-quetes.png`
- `settings` vs `05-profil.png`
- Sortie attendue: match visuel proche des références sur les écrans principaux.

### Lot 3 - Pages secondaires (1.5 jour)
- `auth/login`
- `auth/register-parent`
- `shop`
- `parent/planning`
- `week`
- `task_detail/{taskId}`
- `task_edit?taskId={taskId}`
- Sortie attendue: cohérence complète avec le design system des pages principales.

### Lot 4 - QA finale (0.5 jour)
- Re-capturer toutes les pages en `target`.
- Vérifier régressions navigation et lisibilité.
- Vérifier états vides, chargement, erreur.
- Sortie attendue: passe de validation finale.

## Priorités de correction intra-page
1. Structure (header, navigation, zones principales).
2. Composants clés (cards, CTA, progressions).
3. Détails visuels (glow, icônes, micro-spacing).
4. Etats secondaires (empty/loading/error).

## Critères d'acceptation finaux
- Toutes les routes listées ont une capture `target`.
- Les 5 pages de référence sont alignées visuellement avec leur screen.
- Les pages secondaires héritent clairement du même langage visuel.
- Build `:app:compileDebugKotlin` passe sans erreur.

## Rythme de livraison recommandé
- Livraison incrémentale par lot.
- Validation visuelle à chaque lot avant de passer au suivant.
- Pas de refactor transversal non nécessaire pendant la phase d'adaptation.
