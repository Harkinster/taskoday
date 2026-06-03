# Loot System

## Principe

Le loot est le moteur materiel du Nid. Il remplace toute progression obligatoire par energie de categorie.

Chemin cible :

`taches terminees -> Cristaux/Coffres -> loot -> consommables -> Oeuf, Dragon ou Perchoir choisi`

## Cristaux

- Les Cristaux sont une ressource de progression interne.
- Ils ne servent pas aux Souhaits.
- Ils permettent d'ouvrir ou d'obtenir des Coffres.
- L'interface affiche `Cristaux`.
- Le backend actuel peut les representer temporairement par un item `crystal` dans `item_inventory` avant migration dediee.

## Coffres

| Coffre | Cle cible | Cout Cristaux | Contenu attendu |
| --- | --- | ---: | --- |
| Commun | `common` | 5 | objets communs, petites quantites |
| Rare | `rare` | 15 | objets communs et rares, chance d'Oeuf |
| Epique | `epic` | 40 | objets rares, `star_charm`, fragments et artefacts |

Compatibilite backend :

- `simple` reste accepte et s'affiche comme Coffre commun.
- `rare` reste accepte.
- `epic` est le prochain slot a ajouter cote backend si absent.

## Loot et consommables

| Cle item | Nom visible | Rarete | Usage principal |
| --- | --- | --- | --- |
| `wood_logs` | Rondins de bois | common | Perchoir, Nid |
| `leaf_sprout` | Pousse de feuille | common | Oeufs, Perchoir |
| `mushroom` | Champignon | common | Oeufs |
| `potion` | Potion douce | uncommon | Oeufs, Dragon |
| `lantern` | Lanterne | uncommon | Perchoir |
| `feather_quill` | Plume de scribe | uncommon | Parchemins, quetes lore |
| `sealed_letter` | Lettre scellee | uncommon | quetes lore |
| `magic_book` | Livre magique | rare | Dragon, Perchoir |
| `gold_coin` | Piece doree | rare | ameliorations rares |
| `star_charm` | Charme etoile | epic | evolution legendaire et artefacts |
| `crystal` | Cristal | currency | Coffres |

Structure inventaire recommandee :

```json
{
  "itemKey": "wood_logs",
  "quantity": 4,
  "rarity": "common",
  "usage": "perch"
}
```

## Tables de loot cible

### Coffre commun

- `wood_logs`: 1 a 3
- `leaf_sprout`: 1 a 2
- `mushroom`: 1
- petite chance : `potion`

### Coffre rare

- `wood_logs`: 2 a 4
- `leaf_sprout`: 2 a 3
- `potion`: 1
- `lantern`: 1
- chance : `magic_book`, `gold_coin`, Oeuf ou fragment

### Coffre epique

- `magic_book`: 1 a 2
- `gold_coin`: 1 a 3
- `star_charm`: 1 possible
- chance elevee : fragment, Oeuf, artefact legendaire

## Doublons d'Oeufs

Si un Oeuf deja possede est tire :

- conversion principale : Cristaux.
- conversion secondaire possible : consommable rare.
- conversion speciale possible : fragment d'Oeuf.

Le doublon ne doit jamais etre affiche comme une perte.

## Etat backend actuel

Le backend actuel utilise encore ces items legacy :

- `pomme_dragon`
- `petit_cristal`
- `pierre_chaude`
- `plume_douce`
- `rune_ancienne`
- `fragment_oeuf`

Ils restent acceptes pour compatibilite. La prochaine migration doit mapper progressivement ces cles vers le catalogue cible ci-dessus.
