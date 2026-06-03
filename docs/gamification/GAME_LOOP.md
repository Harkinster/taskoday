# Taskoday Game Loop

## Etat actuel audite

- Le backend expose deja `/api/v1/children/{child_id}/progress`, `/chests`, `/inventory`, `/eggs`, `/dragons`, `/flammeches` et les alias legacy `/scales`, `/rewards`, `/reward-coupons`.
- Les modeles persistants couvrent `guardian_progress`, `child_wallet`, `chest_inventory`, `item_inventory`, `child_eggs` et `child_dragons`.
- La version backend actuelle reste compatible MVP : coffres `simple` et `rare`, items legacy et stades `baby`, `young`, `guardian`.
- Android centralise les assets dans `NestAssets` et affiche deja Le Nid, Inventaire, Oeufs, Dragons, Caverne aux Souhaits et Parchemins avec des donnees locales de demonstration.

## Boucle cible

1. Le Gardien termine une routine, une mission ou une quete.
2. Le backend attribue de l'XP du Gardien et des Flammeches.
3. Les progres de tache peuvent donner des Cristaux, des Coffres ou un Coffre direct.
4. Les Cristaux servent a ouvrir ou obtenir des Coffres.
5. Les Coffres donnent du loot, des consommables, parfois un Oeuf ou un fragment.
6. Le Gardien choisit ou investir les consommables : Oeuf, Dragon actif ou Perchoir.
7. Les Oeufs progressent avec des materiaux, pas avec une energie de categorie obligatoire.
8. Les Dragons evoluent avec des consommables et/ou une XP dragon choisie.
9. Le Dragon defini comme compagnon apparait dans Le Nid avec son Perchoir.
10. Les parents creent des Souhaits dans la Caverne aux Souhaits.
11. L'enfant demande un Souhait avec ses Flammeches.
12. Le parent accepte ou refuse.
13. Si le parent accepte, les Flammeches sont retirees et un Parchemin est cree.

## Regles fortes

- L'XP du Gardien n'est jamais depensee.
- Les Flammeches servent uniquement aux Souhaits crees par les parents.
- Les Cristaux servent aux Coffres, pas aux Souhaits.
- Les consommables servent aux Oeufs, Dragons et Perchoirs.
- Les lignees de dragon sont du lore et du design. Elles ne verrouillent pas la progression.
- Les anciennes routes et champs `scales`, `cost_scales`, `reward-coupons` restent des alias backend pour compatibilite.

## Recompenses recommandees

| Type | XP Gardien | Flammeches | Cristaux | Coffres |
| --- | ---: | ---: | ---: | --- |
| Routine | +5 | +2 | +1 | progression coffre possible |
| Mission | +15 | +6 | +2 a +3 | progression coffre possible |
| Quete | +30 | +12 | +4 a +6 | coffre rare possible |

Les valeurs ci-dessus sont le barème produit cible. Le backend actuel garde ses valeurs legacy tant que la migration coffre/cristaux n'est pas appliquee.

## Coffres

| Coffre | Cout cible en Cristaux | Usage |
| --- | ---: | --- |
| Commun | 5 | loot courant, premiers materiaux |
| Rare | 15 | loot plus riche, chance d'Oeuf ou fragment |
| Epique | 40 | loot rare, consommables puissants, artefacts |

Les recompenses directes de Coffre par une mission ou une quete peuvent rester.

## Niveaux du Nid actuels

| Niveau | Nom backend actuel | Condition actuelle |
| --- | --- | --- |
| 1 | Vieux Nid | par defaut |
| 2 | Nid reveille | 100 XP |
| 3 | Grotte aux oeufs | 300 XP et au moins 1 Oeuf |
| 4 | Sanctuaire du dragon | 600 XP et `dragon_braise` debloque |

## Migration minimale proposee

Ajouter sans supprimer l'ancien systeme :

- `crystals_balance` ou un item special `crystal` dans `item_inventory`.
- `ChestType.EPIC` et un alias d'affichage `simple -> common`.
- champs optionnels sur dragon : `xp`, `is_active_companion`.
- table ou champs Perchoir : `level`, `items_invested`.
- progression d'Oeuf par materiaux : `progress`, `material_points`.
- conversion de doublon d'Oeuf en Cristaux, consommables ou fragments.

Preparation deja faite sans migration :

- catalogue backend enrichi avec les nouveaux consommables.
- payload inventaire enrichi avec `itemKey` et `usage`.
- payload Coffre enrichi avec `display_type` et `crystal_cost`.
