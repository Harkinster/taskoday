# Systeme de loot

Le catalogue Caverne propose:

- `common`: 3 Cristaux, materiaux communs.
- `rare`: 8 Cristaux, materiaux rares et chance garantie d'obtenir la famille Braise.
- `epic`: 15 Cristaux, materiaux epiques et chance garantie d'obtenir la famille Lunaire.

Une famille ne peut posseder qu'un seul oeuf. Si l'oeuf ou le dragon de la famille existe deja, le doublon est converti en 5 essences de famille. Si aucune essence specifique n'est definie, 5 fragments d'oeuf sont donnes.

Les coffres gagnes par l'ancien flux de progression restent ouvrables gratuitement. Cette compatibilite ne depense pas de Cristaux.

La reponse d'ouverture distingue les gains du coffre de l'inventaire final:

- `loot[].quantity` est le gain obtenu pendant cette ouverture.
- `loot[].quantity_total` est le total possede apres ajout.
- `loot[].is_duplicate_compensation` vaut `true` pour une compensation de doublon d'oeuf.
- `inventory_after` contient l'inventaire complet apres ouverture.
