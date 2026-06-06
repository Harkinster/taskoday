# DA Taskoday Reference

Ce document extrait la direction artistique cible depuis les references visuelles disponibles. Ces images sont une direction artistique, pas une maquette a recopier pixel par pixel.

## References Analysees

- `docs/design/references/reference_ui_taskoday_4screens.png` : cible Android principale.
- `docs/design/references/reference_ui_routine_taskoday.png` : cible Routine; correspond au brief `reference_screen_routine_taskoday.png`.
- `docs/design/references/reference_screen_mission_quest_taskoday.png` : cible Mission et Quete.
- `docs/design/references/reference_ui_nest_taskoday.png.png` : cible Le Nid; correspond au brief `reference_screen_nest_taskoday.png`.
- `docs/design/references/reference_ui_popups_taskoday.png` : cible popups et recompenses.
- `docs/design/references/reference_ui_collection_taskoday.png` : cible collections, Inventaire, Oeufs, Dragons et Profil.
- `docs/design/reference_logo_taskoday.png` : identite logo.
- `docs/design/reference_world_taskoday.png` : ambiance monde.

Le fichier `docs/design/references/reference_ui_components_taskoday.png` est cite par le brief mais absent localement. Les composants reutilisables doivent donc appliquer les patterns observes dans les autres references, avec fallback si les assets UI ne sont pas presents.

## Synthese

Taskoday doit lire comme une UI fantasy mobile game premium : violet profond pour la structure, dore chaud pour l'action et la recompense, bois sculpte pour les supports, parchemin pour les contenus lisibles, glow magique subtil et ombres chaudes. L'effet attendu est compact, riche et lisible, pas une interface admin beige/orange.

La planche `reference_ui_taskoday_4screens.png` reste la reference prioritaire pour les proportions globales, la densite mobile, la bottom navigation, le traitement Routine/Mission/Quete et Le Nid. Les references detaillees precisent ensuite chaque famille d'ecrans.

## Palette

- Violet royal profond : structure, panneaux actifs, onglets, popups.
- Dore chaud : actions principales, titres importants, recompenses, bordures actives.
- Bois sombre sculpte : headers, bottom navigation, supports et cadres.
- Parchemin creme : cartes longues, listes, zones de lecture.
- Orange braise : Flammeches, feu, accents d'action.
- Bleu/violet cristal : Cristaux, magie, inventaire.
- Vert feuille : validation, progression positive, feuillage decoratif.

Eviter : beige plat, orange Material dominant, blanc pur, gris froid, bleu Android standard, bordures jaunes pales et surfaces sans relief.

## Composants Cibles

- Header : panneau bois sculpte, blason/flamme, violet profond et ornements feuilles/dore.
- Cartes Routine : parchemin compact, image ou icone encadree, recompenses en ligne courte, validation en medaillon.
- Cartes Mission : parchemin sur structure bois/violet, icone encadree, progression et recompenses separees en pastilles.
- Cartes Quete : violet/dore plus epique, cadre fort, recompenses mises en avant.
- Boutons : principal dore, secondaire violet, formes plus compactes, relief et glow doux.
- Badges : Actif et Verrouille compacts, lisibles, sans ecraser la ligne.
- Monnaies : Flammeches et Cristaux en pastilles premium, avec icone et fond skinnes.
- Bottom navigation : fond bois/violet sombre, onglet actif dore/violet, icones lisibles, aucune navigation Material claire.
- FAB : medaillon dore/violet, assez haut pour ne jamais couvrir le contenu scrollable.
- Popups : cadre violet/bois, blason/flamme, pastilles de recompense et boutons dore/violet.

## Couche De Skin

La DA doit passer par des composants reutilisables. Les ecrans ne doivent pas dessiner directement toute la DA avec `Card`, `Box`, `BorderStroke` et gradients simples.

Registres :

- `NestAssets` : assets metier existants, dragons, oeufs, coffres, parchemins, flammeches, cristaux.
- `FantasySkinAssets` : assets UI, panneaux, boutons, navigation, badges, pastilles et cadres.

Noms UI attendus :

- `ui_header_wood`
- `ui_panel_parchment`
- `ui_panel_purple`
- `ui_panel_quest_epic`
- `ui_button_gold`
- `ui_button_purple`
- `ui_nav_bar_wood`
- `ui_nav_tab_active`
- `ui_currency_flameche`
- `ui_currency_crystal`
- `ui_icon_frame_gold`
- `ui_bestiary_row`
- `ui_fab_gold`
- `ui_badge_active`
- `ui_badge_locked`

Chaque composant doit conserver un fallback Compose pour que le build reste stable si un asset manque.

## Ecrans

Routine :

- Header compact et premium.
- Cartes parchemin compactes, pas de gros rectangles creme.
- XP, Flammeches et Cristaux en petites pastilles.
- Validation en bouton rond ou medaillon.
- Panneau Missions disponibles violet premium.

Mission :

- Style aventure quotidienne.
- Cartes riches mais courtes.
- Icone encadree, objectif, progression et recompenses.
- Boutons compacts, jamais orange plat.

Quete :

- Style plus epique et noble.
- Surfaces violet/dore, cadre plus fort, ombre chaude.
- Recompenses et progression visibles sans surcharge.

Le Nid :

- Hub principal RPG.
- Zone centrale dragon, oeuf ou perchoir immersive.
- Flammeches et Cristaux en pastilles premium.
- Tuiles Inventaire, Oeufs, Dragons, Parchemins, Caverne aux Souhaits dans cadres fantasy.
- Bestiaire compact : image, nom, statut, bouton ou badge court.
- Aucun doublon "Dragons" inutile, aucun texte coupe.

Collections :

- Grilles lisibles avec cadres dore/violet.
- Objets assez grands.
- Rarete et statut visibles.
- Pas de grille Material froide.

Popups :

- Fond violet profond.
- Cadre bois/dore et blason/flamme.
- Recompenses en grosses pastilles.
- Boutons dore/violet.
- Pas de Material Dialog blanc.

## Garde-Fous

- Navigation principale conservee : Routine, Mission, Quete, Le Nid.
- Caverne aux Souhaits, Inventaire, Oeufs, Dragons et Parchemins restent accessibles depuis Le Nid.
- Aucun backend, endpoint, DTO ou service metier ne doit changer pour la DA.
- Ne pas modifier `backend/app/services/gamification_service.py`, `.idea/vcs.xml` ou `NE PAS TOUCHER/`.
- Pas de nouvelle mecanique gameplay.
- Pas de `R.drawable.asset_*` direct hors `NestAssets`.
- Pas de `R.drawable.ui_*` direct hors `FantasySkinAssets`.
- Pas de texte utilisateur interdit : Journee, Boutique, Acheter, Ecailles, Scales, Reward coupons, Rewards.
