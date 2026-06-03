# Asset Inventory

Inventaire genere depuis `docs/assetsbase/`. Les fichiers finaux `asset_*.png` ont ete copies dans `app/src/main/res/drawable-nodpi/`. Les planches sources restent dans `docs/assetsbase/`.

## Synthese

- Total fichiers source trouves : 124
- Assets finaux `asset_*.png` : 107
- Oeufs : 40
- Dragons : 40
- Coffres : 3
- Parchemins : 4
- Interface : 5
- Items inventaire : 10
- Perchoirs : 5
- Sources conservees hors Android : 17
- Assets a renommer : aucun. Les oeufs Sylvyn sont deja en `sleeping/warm/glowing/cracked/hatching`.
- Assets obligatoires manquants : aucun pour eggs, dragons, chests, scrolls, interface, inventory_items et perches.

## Assets optionnels manquants

| Asset | Categorie | Statut | Fallback actuel |
| --- | --- | --- | --- |
| `asset_nest_background` | backgrounds | manquant optionnel | `asset_icon_nid` |
| `asset_wish_cave_background` | backgrounds | manquant optionnel | `asset_icon_nid` |

## Inventaire complet

| Fichier source | Nom Android final | Categorie | Dragon | Etat/stade | Usage | Statut | Destination Android recommandee |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Coffres/asset_chest_common.png | asset_chest_common | chests | - | common | Coffres du Gardien et inventaire | OK | app/src/main/res/drawable-nodpi |
| Coffres/asset_chest_epic.png | asset_chest_epic | chests | - | epic | Coffres du Gardien et inventaire | OK | app/src/main/res/drawable-nodpi |
| Coffres/asset_chest_rare.png | asset_chest_rare | chests | - | rare | Coffres du Gardien et inventaire | OK | app/src/main/res/drawable-nodpi |
| Coffres/ChatGPT Image 3 juin 2026, 11_51_37.png | - | source | - | - | Planche source conservee dans docs/assetsbase | source uniquement | docs/assetsbase |
| interface/asset_icon_cristal.png | asset_icon_cristal | interface | - | cristal | Icones de Flammeches, cristaux, Nid et etats vides | OK | app/src/main/res/drawable-nodpi |
| interface/asset_icon_flammeche.png | asset_icon_flammeche | interface | - | flammeche | Icones de Flammeches, cristaux, Nid et etats vides | OK | app/src/main/res/drawable-nodpi |
| interface/asset_icon_inventaire_vide.png | asset_icon_inventaire_vide | interface | - | inventaire_vide | Icones de Flammeches, cristaux, Nid et etats vides | OK | app/src/main/res/drawable-nodpi |
| interface/asset_icon_nid.png | asset_icon_nid | interface | - | nid | Icones de Flammeches, cristaux, Nid et etats vides | OK | app/src/main/res/drawable-nodpi |
| interface/asset_icon_oeuf_verrouille.png | asset_icon_oeuf_verrouille | interface | - | oeuf_verrouille | Icones de Flammeches, cristaux, Nid et etats vides | OK | app/src/main/res/drawable-nodpi |
| interface/asset_item_feather_quill.png | asset_item_feather_quill | inventory_items | - | feather_quill | Objets affiches dans l inventaire | OK | app/src/main/res/drawable-nodpi |
| interface/asset_item_gold_coin.png | asset_item_gold_coin | inventory_items | - | gold_coin | Objets affiches dans l inventaire | OK | app/src/main/res/drawable-nodpi |
| interface/asset_item_lantern.png | asset_item_lantern | inventory_items | - | lantern | Objets affiches dans l inventaire | OK | app/src/main/res/drawable-nodpi |
| interface/asset_item_leaf_sprout.png | asset_item_leaf_sprout | inventory_items | - | leaf_sprout | Objets affiches dans l inventaire | OK | app/src/main/res/drawable-nodpi |
| interface/asset_item_magic_book.png | asset_item_magic_book | inventory_items | - | magic_book | Objets affiches dans l inventaire | OK | app/src/main/res/drawable-nodpi |
| interface/asset_item_mushroom.png | asset_item_mushroom | inventory_items | - | mushroom | Objets affiches dans l inventaire | OK | app/src/main/res/drawable-nodpi |
| interface/asset_item_potion.png | asset_item_potion | inventory_items | - | potion | Objets affiches dans l inventaire | OK | app/src/main/res/drawable-nodpi |
| interface/asset_item_sealed_letter.png | asset_item_sealed_letter | inventory_items | - | sealed_letter | Objets affiches dans l inventaire | OK | app/src/main/res/drawable-nodpi |
| interface/asset_item_star_charm.png | asset_item_star_charm | inventory_items | - | star_charm | Objets affiches dans l inventaire | OK | app/src/main/res/drawable-nodpi |
| interface/asset_item_wood_logs.png | asset_item_wood_logs | inventory_items | - | wood_logs | Objets affiches dans l inventaire | OK | app/src/main/res/drawable-nodpi |
| Oeufs et dragons/1 Fulmio/asset_dragon_fulmio_baby.png | asset_dragon_fulmio_baby | dragons | Fulmio | baby | Stade visuel des dragons dans Le Nid et l ecran Dragons | OK | app/src/main/res/drawable-nodpi |
| Oeufs et dragons/1 Fulmio/asset_dragon_fulmio_large.png | asset_dragon_fulmio_large | dragons | Fulmio | large | Stade visuel des dragons dans Le Nid et l ecran Dragons | OK | app/src/main/res/drawable-nodpi |
| Oeufs et dragons/1 Fulmio/asset_dragon_fulmio_legendary.png | asset_dragon_fulmio_legendary | dragons | Fulmio | legendary | Stade visuel des dragons dans Le Nid et l ecran Dragons | OK | app/src/main/res/drawable-nodpi |
| Oeufs et dragons/1 Fulmio/asset_dragon_fulmio_medium.png | asset_dragon_fulmio_medium | dragons | Fulmio | medium | Stade visuel des dragons dans Le Nid et l ecran Dragons | OK | app/src/main/res/drawable-nodpi |
| Oeufs et dragons/1 Fulmio/asset_dragon_fulmio_young.png | asset_dragon_fulmio_young | dragons | Fulmio | young | Stade visuel des dragons dans Le Nid et l ecran Dragons | OK | app/src/main/res/drawable-nodpi |
| Oeufs et dragons/1 Fulmio/asset_egg_fulmio_cracked.png | asset_egg_fulmio_cracked | eggs | Fulmio | cracked | Etat visuel des oeufs dans Le Nid et l ecran Oeufs | OK | app/src/main/res/drawable-nodpi |
| Oeufs et dragons/1 Fulmio/asset_egg_fulmio_glowing.png | asset_egg_fulmio_glowing | eggs | Fulmio | glowing | Etat visuel des oeufs dans Le Nid et l ecran Oeufs | OK | app/src/main/res/drawable-nodpi |
| Oeufs et dragons/1 Fulmio/asset_egg_fulmio_hatching.png | asset_egg_fulmio_hatching | eggs | Fulmio | hatching | Etat visuel des oeufs dans Le Nid et l ecran Oeufs | OK | app/src/main/res/drawable-nodpi |
| Oeufs et dragons/1 Fulmio/asset_egg_fulmio_sleeping.png | asset_egg_fulmio_sleeping | eggs | Fulmio | sleeping | Etat visuel des oeufs dans Le Nid et l ecran Oeufs | OK | app/src/main/res/drawable-nodpi |
| Oeufs et dragons/1 Fulmio/asset_egg_fulmio_warm.png | asset_egg_fulmio_warm | eggs | Fulmio | warm | Etat visuel des oeufs dans Le Nid et l ecran Oeufs | OK | app/src/main/res/drawable-nodpi |
| Oeufs et dragons/1 Fulmio/ChatGPT Image 3 juin 2026, 14_28_37.png | - | source | - | - | Planche source conservee dans docs/assetsbase | source uniquement | docs/assetsbase |
| Oeufs et dragons/1 Fulmio/oeuf fulmio.png | - | source | - | - | Planche source conservee dans docs/assetsbase | source uniquement | docs/assetsbase |
| Oeufs et dragons/2 Sylvyn/asset_dragon_sylvyn_baby.png | asset_dragon_sylvyn_baby | dragons | Sylvyn | baby | Stade visuel des dragons dans Le Nid et l ecran Dragons | OK | app/src/main/res/drawable-nodpi |
| Oeufs et dragons/2 Sylvyn/asset_dragon_sylvyn_large.png | asset_dragon_sylvyn_large | dragons | Sylvyn | large | Stade visuel des dragons dans Le Nid et l ecran Dragons | OK | app/src/main/res/drawable-nodpi |
| Oeufs et dragons/2 Sylvyn/asset_dragon_sylvyn_legendary.png | asset_dragon_sylvyn_legendary | dragons | Sylvyn | legendary | Stade visuel des dragons dans Le Nid et l ecran Dragons | OK | app/src/main/res/drawable-nodpi |
| Oeufs et dragons/2 Sylvyn/asset_dragon_sylvyn_medium.png | asset_dragon_sylvyn_medium | dragons | Sylvyn | medium | Stade visuel des dragons dans Le Nid et l ecran Dragons | OK | app/src/main/res/drawable-nodpi |
| Oeufs et dragons/2 Sylvyn/asset_dragon_sylvyn_young.png | asset_dragon_sylvyn_young | dragons | Sylvyn | young | Stade visuel des dragons dans Le Nid et l ecran Dragons | OK | app/src/main/res/drawable-nodpi |
| Oeufs et dragons/2 Sylvyn/asset_egg_sylvyn_cracked.png | asset_egg_sylvyn_cracked | eggs | Sylvyn | cracked | Etat visuel des oeufs dans Le Nid et l ecran Oeufs | OK | app/src/main/res/drawable-nodpi |
| Oeufs et dragons/2 Sylvyn/asset_egg_sylvyn_glowing.png | asset_egg_sylvyn_glowing | eggs | Sylvyn | glowing | Etat visuel des oeufs dans Le Nid et l ecran Oeufs | OK | app/src/main/res/drawable-nodpi |
| Oeufs et dragons/2 Sylvyn/asset_egg_sylvyn_hatching.png | asset_egg_sylvyn_hatching | eggs | Sylvyn | hatching | Etat visuel des oeufs dans Le Nid et l ecran Oeufs | OK | app/src/main/res/drawable-nodpi |
| Oeufs et dragons/2 Sylvyn/asset_egg_sylvyn_sleeping.png | asset_egg_sylvyn_sleeping | eggs | Sylvyn | sleeping | Etat visuel des oeufs dans Le Nid et l ecran Oeufs | OK | app/src/main/res/drawable-nodpi |
| Oeufs et dragons/2 Sylvyn/asset_egg_sylvyn_warm.png | asset_egg_sylvyn_warm | eggs | Sylvyn | warm | Etat visuel des oeufs dans Le Nid et l ecran Oeufs | OK | app/src/main/res/drawable-nodpi |
| Oeufs et dragons/2 Sylvyn/ChatGPT Image 3 juin 2026, 14_28_47.png | - | source | - | - | Planche source conservee dans docs/assetsbase | source uniquement | docs/assetsbase |
| Oeufs et dragons/2 Sylvyn/Sylvynn.png | - | source | - | - | Planche source conservee dans docs/assetsbase | source uniquement | docs/assetsbase |
| Oeufs et dragons/3 Phenor/asset_dragon_phenor_baby.png | asset_dragon_phenor_baby | dragons | Phenor | baby | Stade visuel des dragons dans Le Nid et l ecran Dragons | OK | app/src/main/res/drawable-nodpi |
| Oeufs et dragons/3 Phenor/asset_dragon_phenor_large.png | asset_dragon_phenor_large | dragons | Phenor | large | Stade visuel des dragons dans Le Nid et l ecran Dragons | OK | app/src/main/res/drawable-nodpi |
| Oeufs et dragons/3 Phenor/asset_dragon_phenor_legendary.png | asset_dragon_phenor_legendary | dragons | Phenor | legendary | Stade visuel des dragons dans Le Nid et l ecran Dragons | OK | app/src/main/res/drawable-nodpi |
| Oeufs et dragons/3 Phenor/asset_dragon_phenor_medium.png | asset_dragon_phenor_medium | dragons | Phenor | medium | Stade visuel des dragons dans Le Nid et l ecran Dragons | OK | app/src/main/res/drawable-nodpi |
| Oeufs et dragons/3 Phenor/asset_dragon_phenor_young.png | asset_dragon_phenor_young | dragons | Phenor | young | Stade visuel des dragons dans Le Nid et l ecran Dragons | OK | app/src/main/res/drawable-nodpi |
| Oeufs et dragons/3 Phenor/asset_egg_phenor_cracked.png | asset_egg_phenor_cracked | eggs | Phenor | cracked | Etat visuel des oeufs dans Le Nid et l ecran Oeufs | OK | app/src/main/res/drawable-nodpi |
| Oeufs et dragons/3 Phenor/asset_egg_phenor_glowing.png | asset_egg_phenor_glowing | eggs | Phenor | glowing | Etat visuel des oeufs dans Le Nid et l ecran Oeufs | OK | app/src/main/res/drawable-nodpi |
| Oeufs et dragons/3 Phenor/asset_egg_phenor_hatching.png | asset_egg_phenor_hatching | eggs | Phenor | hatching | Etat visuel des oeufs dans Le Nid et l ecran Oeufs | OK | app/src/main/res/drawable-nodpi |
| Oeufs et dragons/3 Phenor/asset_egg_phenor_sleeping.png | asset_egg_phenor_sleeping | eggs | Phenor | sleeping | Etat visuel des oeufs dans Le Nid et l ecran Oeufs | OK | app/src/main/res/drawable-nodpi |
| Oeufs et dragons/3 Phenor/asset_egg_phenor_warm.png | asset_egg_phenor_warm | eggs | Phenor | warm | Etat visuel des oeufs dans Le Nid et l ecran Oeufs | OK | app/src/main/res/drawable-nodpi |
| Oeufs et dragons/3 Phenor/ChatGPT Image 3 juin 2026, 14_28_59.png | - | source | - | - | Planche source conservee dans docs/assetsbase | source uniquement | docs/assetsbase |
| Oeufs et dragons/3 Phenor/Phénor.png | - | source | - | - | Planche source conservee dans docs/assetsbase | source uniquement | docs/assetsbase |
| Oeufs et dragons/4 Lunarys/asset_dragon_lunarys_baby.png | asset_dragon_lunarys_baby | dragons | Lunarys | baby | Stade visuel des dragons dans Le Nid et l ecran Dragons | OK | app/src/main/res/drawable-nodpi |
| Oeufs et dragons/4 Lunarys/asset_dragon_lunarys_large.png | asset_dragon_lunarys_large | dragons | Lunarys | large | Stade visuel des dragons dans Le Nid et l ecran Dragons | OK | app/src/main/res/drawable-nodpi |
| Oeufs et dragons/4 Lunarys/asset_dragon_lunarys_legendary.png | asset_dragon_lunarys_legendary | dragons | Lunarys | legendary | Stade visuel des dragons dans Le Nid et l ecran Dragons | OK | app/src/main/res/drawable-nodpi |
| Oeufs et dragons/4 Lunarys/asset_dragon_lunarys_medium.png | asset_dragon_lunarys_medium | dragons | Lunarys | medium | Stade visuel des dragons dans Le Nid et l ecran Dragons | OK | app/src/main/res/drawable-nodpi |
| Oeufs et dragons/4 Lunarys/asset_dragon_lunarys_young.png | asset_dragon_lunarys_young | dragons | Lunarys | young | Stade visuel des dragons dans Le Nid et l ecran Dragons | OK | app/src/main/res/drawable-nodpi |
| Oeufs et dragons/4 Lunarys/asset_egg_lunarys_cracked.png | asset_egg_lunarys_cracked | eggs | Lunarys | cracked | Etat visuel des oeufs dans Le Nid et l ecran Oeufs | OK | app/src/main/res/drawable-nodpi |
| Oeufs et dragons/4 Lunarys/asset_egg_lunarys_glowing.png | asset_egg_lunarys_glowing | eggs | Lunarys | glowing | Etat visuel des oeufs dans Le Nid et l ecran Oeufs | OK | app/src/main/res/drawable-nodpi |
| Oeufs et dragons/4 Lunarys/asset_egg_lunarys_hatching.png | asset_egg_lunarys_hatching | eggs | Lunarys | hatching | Etat visuel des oeufs dans Le Nid et l ecran Oeufs | OK | app/src/main/res/drawable-nodpi |
| Oeufs et dragons/4 Lunarys/asset_egg_lunarys_sleeping.png | asset_egg_lunarys_sleeping | eggs | Lunarys | sleeping | Etat visuel des oeufs dans Le Nid et l ecran Oeufs | OK | app/src/main/res/drawable-nodpi |
| Oeufs et dragons/4 Lunarys/asset_egg_lunarys_warm.png | asset_egg_lunarys_warm | eggs | Lunarys | warm | Etat visuel des oeufs dans Le Nid et l ecran Oeufs | OK | app/src/main/res/drawable-nodpi |
| Oeufs et dragons/4 Lunarys/ChatGPT Image 3 juin 2026, 14_29_07.png | - | source | - | - | Planche source conservee dans docs/assetsbase | source uniquement | docs/assetsbase |
| Oeufs et dragons/4 Lunarys/Lunarys.png | - | source | - | - | Planche source conservee dans docs/assetsbase | source uniquement | docs/assetsbase |
| Oeufs et dragons/5 Pyron/asset_dragon_pyron_baby.png | asset_dragon_pyron_baby | dragons | Pyron | baby | Stade visuel des dragons dans Le Nid et l ecran Dragons | OK | app/src/main/res/drawable-nodpi |
| Oeufs et dragons/5 Pyron/asset_dragon_pyron_large.png | asset_dragon_pyron_large | dragons | Pyron | large | Stade visuel des dragons dans Le Nid et l ecran Dragons | OK | app/src/main/res/drawable-nodpi |
| Oeufs et dragons/5 Pyron/asset_dragon_pyron_legendary.png | asset_dragon_pyron_legendary | dragons | Pyron | legendary | Stade visuel des dragons dans Le Nid et l ecran Dragons | OK | app/src/main/res/drawable-nodpi |
| Oeufs et dragons/5 Pyron/asset_dragon_pyron_medium.png | asset_dragon_pyron_medium | dragons | Pyron | medium | Stade visuel des dragons dans Le Nid et l ecran Dragons | OK | app/src/main/res/drawable-nodpi |
| Oeufs et dragons/5 Pyron/asset_dragon_pyron_young.png | asset_dragon_pyron_young | dragons | Pyron | young | Stade visuel des dragons dans Le Nid et l ecran Dragons | OK | app/src/main/res/drawable-nodpi |
| Oeufs et dragons/5 Pyron/asset_egg_pyron_cracked.png | asset_egg_pyron_cracked | eggs | Pyron | cracked | Etat visuel des oeufs dans Le Nid et l ecran Oeufs | OK | app/src/main/res/drawable-nodpi |
| Oeufs et dragons/5 Pyron/asset_egg_pyron_glowing.png | asset_egg_pyron_glowing | eggs | Pyron | glowing | Etat visuel des oeufs dans Le Nid et l ecran Oeufs | OK | app/src/main/res/drawable-nodpi |
| Oeufs et dragons/5 Pyron/asset_egg_pyron_hatching.png | asset_egg_pyron_hatching | eggs | Pyron | hatching | Etat visuel des oeufs dans Le Nid et l ecran Oeufs | OK | app/src/main/res/drawable-nodpi |
| Oeufs et dragons/5 Pyron/asset_egg_pyron_sleeping.png | asset_egg_pyron_sleeping | eggs | Pyron | sleeping | Etat visuel des oeufs dans Le Nid et l ecran Oeufs | OK | app/src/main/res/drawable-nodpi |
| Oeufs et dragons/5 Pyron/asset_egg_pyron_warm.png | asset_egg_pyron_warm | eggs | Pyron | warm | Etat visuel des oeufs dans Le Nid et l ecran Oeufs | OK | app/src/main/res/drawable-nodpi |
| Oeufs et dragons/5 Pyron/ChatGPT Image 3 juin 2026, 14_29_24.png | - | source | - | - | Planche source conservee dans docs/assetsbase | source uniquement | docs/assetsbase |
| Oeufs et dragons/5 Pyron/Pyron.png | - | source | - | - | Planche source conservee dans docs/assetsbase | source uniquement | docs/assetsbase |
| Oeufs et dragons/6 Chronyx/asset_dragon_chronyx_baby.png | asset_dragon_chronyx_baby | dragons | Chronyx | baby | Stade visuel des dragons dans Le Nid et l ecran Dragons | OK | app/src/main/res/drawable-nodpi |
| Oeufs et dragons/6 Chronyx/asset_dragon_chronyx_large.png | asset_dragon_chronyx_large | dragons | Chronyx | large | Stade visuel des dragons dans Le Nid et l ecran Dragons | OK | app/src/main/res/drawable-nodpi |
| Oeufs et dragons/6 Chronyx/asset_dragon_chronyx_legendary.png | asset_dragon_chronyx_legendary | dragons | Chronyx | legendary | Stade visuel des dragons dans Le Nid et l ecran Dragons | OK | app/src/main/res/drawable-nodpi |
| Oeufs et dragons/6 Chronyx/asset_dragon_chronyx_medium.png | asset_dragon_chronyx_medium | dragons | Chronyx | medium | Stade visuel des dragons dans Le Nid et l ecran Dragons | OK | app/src/main/res/drawable-nodpi |
| Oeufs et dragons/6 Chronyx/asset_dragon_chronyx_young.png | asset_dragon_chronyx_young | dragons | Chronyx | young | Stade visuel des dragons dans Le Nid et l ecran Dragons | OK | app/src/main/res/drawable-nodpi |
| Oeufs et dragons/6 Chronyx/asset_egg_chronyx_cracked.png | asset_egg_chronyx_cracked | eggs | Chronyx | cracked | Etat visuel des oeufs dans Le Nid et l ecran Oeufs | OK | app/src/main/res/drawable-nodpi |
| Oeufs et dragons/6 Chronyx/asset_egg_chronyx_glowing.png | asset_egg_chronyx_glowing | eggs | Chronyx | glowing | Etat visuel des oeufs dans Le Nid et l ecran Oeufs | OK | app/src/main/res/drawable-nodpi |
| Oeufs et dragons/6 Chronyx/asset_egg_chronyx_hatching.png | asset_egg_chronyx_hatching | eggs | Chronyx | hatching | Etat visuel des oeufs dans Le Nid et l ecran Oeufs | OK | app/src/main/res/drawable-nodpi |
| Oeufs et dragons/6 Chronyx/asset_egg_chronyx_sleeping.png | asset_egg_chronyx_sleeping | eggs | Chronyx | sleeping | Etat visuel des oeufs dans Le Nid et l ecran Oeufs | OK | app/src/main/res/drawable-nodpi |
| Oeufs et dragons/6 Chronyx/asset_egg_chronyx_warm.png | asset_egg_chronyx_warm | eggs | Chronyx | warm | Etat visuel des oeufs dans Le Nid et l ecran Oeufs | OK | app/src/main/res/drawable-nodpi |
| Oeufs et dragons/6 Chronyx/ChatGPT Image 3 juin 2026, 14_53_05.png | - | source | - | - | Planche source conservee dans docs/assetsbase | source uniquement | docs/assetsbase |
| Oeufs et dragons/6 Chronyx/Chronyx.png | - | source | - | - | Planche source conservee dans docs/assetsbase | source uniquement | docs/assetsbase |
| Oeufs et dragons/7 Ambrio/Ambrio.png | - | source | - | - | Planche source conservee dans docs/assetsbase | source uniquement | docs/assetsbase |
| Oeufs et dragons/7 Ambrio/asset_dragon_ambrio_baby.png | asset_dragon_ambrio_baby | dragons | Ambrio | baby | Stade visuel des dragons dans Le Nid et l ecran Dragons | OK | app/src/main/res/drawable-nodpi |
| Oeufs et dragons/7 Ambrio/asset_dragon_ambrio_large.png | asset_dragon_ambrio_large | dragons | Ambrio | large | Stade visuel des dragons dans Le Nid et l ecran Dragons | OK | app/src/main/res/drawable-nodpi |
| Oeufs et dragons/7 Ambrio/asset_dragon_ambrio_legendary.png | asset_dragon_ambrio_legendary | dragons | Ambrio | legendary | Stade visuel des dragons dans Le Nid et l ecran Dragons | OK | app/src/main/res/drawable-nodpi |
| Oeufs et dragons/7 Ambrio/asset_dragon_ambrio_medium.png | asset_dragon_ambrio_medium | dragons | Ambrio | medium | Stade visuel des dragons dans Le Nid et l ecran Dragons | OK | app/src/main/res/drawable-nodpi |
| Oeufs et dragons/7 Ambrio/asset_dragon_ambrio_young.png | asset_dragon_ambrio_young | dragons | Ambrio | young | Stade visuel des dragons dans Le Nid et l ecran Dragons | OK | app/src/main/res/drawable-nodpi |
| Oeufs et dragons/7 Ambrio/asset_egg_ambrio_cracked.png | asset_egg_ambrio_cracked | eggs | Ambrio | cracked | Etat visuel des oeufs dans Le Nid et l ecran Oeufs | OK | app/src/main/res/drawable-nodpi |
| Oeufs et dragons/7 Ambrio/asset_egg_ambrio_glowing.png | asset_egg_ambrio_glowing | eggs | Ambrio | glowing | Etat visuel des oeufs dans Le Nid et l ecran Oeufs | OK | app/src/main/res/drawable-nodpi |
| Oeufs et dragons/7 Ambrio/asset_egg_ambrio_hatching.png | asset_egg_ambrio_hatching | eggs | Ambrio | hatching | Etat visuel des oeufs dans Le Nid et l ecran Oeufs | OK | app/src/main/res/drawable-nodpi |
| Oeufs et dragons/7 Ambrio/asset_egg_ambrio_sleeping.png | asset_egg_ambrio_sleeping | eggs | Ambrio | sleeping | Etat visuel des oeufs dans Le Nid et l ecran Oeufs | OK | app/src/main/res/drawable-nodpi |
| Oeufs et dragons/7 Ambrio/asset_egg_ambrio_warm.png | asset_egg_ambrio_warm | eggs | Ambrio | warm | Etat visuel des oeufs dans Le Nid et l ecran Oeufs | OK | app/src/main/res/drawable-nodpi |
| Oeufs et dragons/7 Ambrio/ChatGPT Image 3 juin 2026, 14_53_18.png | - | source | - | - | Planche source conservee dans docs/assetsbase | source uniquement | docs/assetsbase |
| Oeufs et dragons/8 Cristao/asset_dragon_cristao_baby.png | asset_dragon_cristao_baby | dragons | Cristao | baby | Stade visuel des dragons dans Le Nid et l ecran Dragons | OK | app/src/main/res/drawable-nodpi |
| Oeufs et dragons/8 Cristao/asset_dragon_cristao_large.png | asset_dragon_cristao_large | dragons | Cristao | large | Stade visuel des dragons dans Le Nid et l ecran Dragons | OK | app/src/main/res/drawable-nodpi |
| Oeufs et dragons/8 Cristao/asset_dragon_cristao_legendary.png | asset_dragon_cristao_legendary | dragons | Cristao | legendary | Stade visuel des dragons dans Le Nid et l ecran Dragons | OK | app/src/main/res/drawable-nodpi |
| Oeufs et dragons/8 Cristao/asset_dragon_cristao_medium.png | asset_dragon_cristao_medium | dragons | Cristao | medium | Stade visuel des dragons dans Le Nid et l ecran Dragons | OK | app/src/main/res/drawable-nodpi |
| Oeufs et dragons/8 Cristao/asset_dragon_cristao_young.png | asset_dragon_cristao_young | dragons | Cristao | young | Stade visuel des dragons dans Le Nid et l ecran Dragons | OK | app/src/main/res/drawable-nodpi |
| Oeufs et dragons/8 Cristao/asset_egg_cristao_cracked.png | asset_egg_cristao_cracked | eggs | Cristao | cracked | Etat visuel des oeufs dans Le Nid et l ecran Oeufs | OK | app/src/main/res/drawable-nodpi |
| Oeufs et dragons/8 Cristao/asset_egg_cristao_glowing.png | asset_egg_cristao_glowing | eggs | Cristao | glowing | Etat visuel des oeufs dans Le Nid et l ecran Oeufs | OK | app/src/main/res/drawable-nodpi |
| Oeufs et dragons/8 Cristao/asset_egg_cristao_hatching.png | asset_egg_cristao_hatching | eggs | Cristao | hatching | Etat visuel des oeufs dans Le Nid et l ecran Oeufs | OK | app/src/main/res/drawable-nodpi |
| Oeufs et dragons/8 Cristao/asset_egg_cristao_sleeping.png | asset_egg_cristao_sleeping | eggs | Cristao | sleeping | Etat visuel des oeufs dans Le Nid et l ecran Oeufs | OK | app/src/main/res/drawable-nodpi |
| Oeufs et dragons/8 Cristao/asset_egg_cristao_warm.png | asset_egg_cristao_warm | eggs | Cristao | warm | Etat visuel des oeufs dans Le Nid et l ecran Oeufs | OK | app/src/main/res/drawable-nodpi |
| Oeufs et dragons/8 Cristao/ChatGPT Image 3 juin 2026, 14_53_31.png | - | source | - | - | Planche source conservee dans docs/assetsbase | source uniquement | docs/assetsbase |
| Oeufs et dragons/8 Cristao/cristao.png | - | source | - | - | Planche source conservee dans docs/assetsbase | source uniquement | docs/assetsbase |
| Parchemins/asset_scroll_approved.png | asset_scroll_approved | scrolls | - | approved | Demandes et Parchemins selon statut | OK | app/src/main/res/drawable-nodpi |
| Parchemins/asset_scroll_pending.png | asset_scroll_pending | scrolls | - | pending | Demandes et Parchemins selon statut | OK | app/src/main/res/drawable-nodpi |
| Parchemins/asset_scroll_refused.png | asset_scroll_refused | scrolls | - | refused | Demandes et Parchemins selon statut | OK | app/src/main/res/drawable-nodpi |
| Parchemins/asset_scroll_used.png | asset_scroll_used | scrolls | - | used | Demandes et Parchemins selon statut | OK | app/src/main/res/drawable-nodpi |
| Perchoirs/asset_perch_level1.png | asset_perch_level1 | perches | - | level1 | Perchoir du Nid selon niveau | OK | app/src/main/res/drawable-nodpi |
| Perchoirs/asset_perch_level2.png | asset_perch_level2 | perches | - | level2 | Perchoir du Nid selon niveau | OK | app/src/main/res/drawable-nodpi |
| Perchoirs/asset_perch_level3.png | asset_perch_level3 | perches | - | level3 | Perchoir du Nid selon niveau | OK | app/src/main/res/drawable-nodpi |
| Perchoirs/asset_perch_level4.png | asset_perch_level4 | perches | - | level4 | Perchoir du Nid selon niveau | OK | app/src/main/res/drawable-nodpi |
| Perchoirs/asset_perch_level5.png | asset_perch_level5 | perches | - | level5 | Perchoir du Nid selon niveau | OK | app/src/main/res/drawable-nodpi |
