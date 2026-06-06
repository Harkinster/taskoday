# Taskoday Art Direction

Taskoday est une application Android de routines, missions et quetes avec une enveloppe fantasy douce appelee Le Nid. La cible visuelle est une interface premium de jeu mobile familial : violet royal profond, dore chaud, bois sculpte, parchemin creme, relief lisible, glow magique subtil et petit dragon chaleureux.

## Lore Officiel

- Le Nid : hub principal de progression.
- Gardien : identite de progression de l'enfant.
- XP du Gardien : progression uniquement, jamais depensee.
- Flammeches : monnaie des Souhaits.
- Cristaux : ressource de progression pour les Coffres.
- Coffres : recompenses de progression.
- Oeufs : collection a faire eclore.
- Dragons : compagnons du Gardien.
- Perchoir : emplacement du compagnon actif.
- Caverne aux Souhaits : ecran des recompenses externes creees par les parents.
- Souhait : recompense demandable.
- Parchemin : Souhait valide.

## Reference Visuelle Android

La cible visuelle principale pour l'application Android est `docs/design/references/reference_ui_taskoday_4screens.png`. Cette planche 4 ecrans fixe l'esprit attendu : header bois/violet, cartes parchemin, missions aventure, quetes epiques violet/dore, Le Nid comme hub RPG et bottom navigation sombre avec onglet actif dore.

References presentes dans le dossier `docs/design/references/` :

- `reference_ui_taskoday_4screens.png` : reference Android prioritaire.
- `reference_ui_routine_taskoday.png` : cible Routine; elle remplace le nom attendu `reference_screen_routine_taskoday.png`.
- `reference_screen_mission_quest_taskoday.png` : cible Mission et Quete.
- `reference_ui_nest_taskoday.png.png` : cible Le Nid; elle remplace le nom attendu `reference_screen_nest_taskoday.png`.
- `reference_ui_collection_taskoday.png` : cible collections, Inventaire, Oeufs, Dragons et Profil.
- `reference_ui_popups_taskoday.png` : cible popups, dialogues et recompenses.

Le fichier `reference_ui_components_taskoday.png` est attendu par le brief mais absent localement. Les composants doivent donc suivre les autres references et rester robustes grace aux fallbacks Compose.

Voir aussi `docs/design/DA_TASKODAY_REFERENCE.md` pour l'analyse detaillee.

## Palette

- Structure : violet royal profond, violet nuit et bois sombre sculpte.
- Action et recompense : dore chaud, or vieilli et orange braise.
- Lecture : parchemin creme, brun encre et ombre chaude.
- Magie : bleu cristal, violet lumineux et glow doux.
- Validation : vert feuille ou vert mousse.
- Erreur/refus : rouge doux, jamais agressif.

Eviter le beige plat, l'orange Material dominant, le blanc pur, le gris froid et les bordures jaunes pales.

## Couche De Skin

La DA ne doit pas etre redessinee ecran par ecran avec des rectangles Compose plats. Les assets UI fantasy sont centralises dans `FantasySkinAssets`, puis appliques par les composants reutilisables.

Assets UI attendus dans `app/src/main/res/drawable-nodpi/` :

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

Regles :

- Les assets metier restent centralises via `NestAssets`.
- Les assets UI restent centralises via `FantasySkinAssets`.
- Les ecrans ne referencent jamais directement `R.drawable.asset_*` ni `R.drawable.ui_*`.
- Chaque skin doit avoir un fallback Compose pour ne pas casser le build si une image manque.

## Composants

- `FantasyScreenBackground` : fond fantasy sombre/lumineux, jamais blanc plat.
- `FantasyHeader` / `TaskodayHeader` : panneau bois sculpte avec violet profond et dore.
- `FantasyCard` / `NeonCard` : panneaux parchemin ou violet avec assets de skin, relief et ombres chaudes.
- `FantasyButton` / `NeonButton` : boutons skinnes dore/violet, compacts, sans aplat orange plat.
- `FantasyProgressBar` : progression lisible avec rail sombre, remplissage dore/vert/violet.
- `FantasyAssetBubble` : icones metier dans cadre fantasy dore.
- `CurrencyPill` : Flammeches et Cristaux en pastilles premium.
- `FantasyBottomNavigation` / `TaskodayBottomBar` : bois/violet sombre, onglet actif dore.
- `QuickAddFab` : medaillon dore/violet, avec padding suffisant.
- `BestiaryRow` : ligne compacte avec image, nom, statut et badge/bouton court.

## Ecrans

- Routine : header compact, date lisible, cartes parchemin, recompenses en petites pastilles, validation en medaillon, panneau Missions disponibles violet.
- Mission : cartes aventure, icone encadree, objectif court, progression claire et recompenses visibles.
- Quete : cartes plus epiques, violet/dore, relief fort, recompenses mises en avant.
- Le Nid : hub RPG premium, perchoir + dragon ou oeuf au centre, XP Gardien, Flammeches, Cristaux, tuiles fantasy et Bestiaire compact.
- Caverne aux Souhaits : caverne magique douce, Souhaits et Parchemins, Flammeches visibles.
- Inventaire, Oeufs, Dragons : grilles de collection avec cadres dore/violet, assets assez grands et statuts lisibles.
- Popups : fond violet profond, blason/flamme, recompenses en pastilles, boutons dore/violet.

## Do

- Centraliser les assets via `NestAssets` et `FantasySkinAssets`.
- Garder les vrais assets remplacables sans modifier les ecrans.
- Utiliser les tokens de `Color.kt` et `FantasyTokens.kt`.
- Favoriser les ombres chaudes, le relief leger, les bordures dorees/violettes et les cartes compactes.
- Garder les textes courts et lisibles sur petit ecran.

## Don't

- Ne pas toucher au backend pour une evolution visuelle.
- Ne pas renommer les endpoints API, DTO ou routes existantes.
- Ne pas afficher de vocabulaire utilisateur hors lore comme Journee, Boutique, Acheter, Ecailles, Scales, Reward coupons ou Rewards.
- Ne pas utiliser de style admin, Material plat, beige plat, blanc pur ou gris froid.
- Ne pas creer de nouvelle mecanique gameplay pour justifier la DA.
