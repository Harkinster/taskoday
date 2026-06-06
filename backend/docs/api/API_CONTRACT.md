# Contrat API du Nid

Toutes les routes ci-dessous sont sous `/api/v1` et necessitent l'acces a l'enfant.

## Monnaies

- `GET /children/{child_id}/flammeches`: solde utilise par les Souhaits.
- `GET /children/{child_id}/crystals`: solde utilise par les coffres.
- `GET /children/{child_id}/inventory`: retourne `currencies.flammeches`, `currencies.crystals`, les objets et les coffres possedes.

Les alias legacy `scales`, `rewards` et `reward-coupons`, ainsi que le champ `cost_scales`, restent exposes.

## Caverne et coffres

- `GET /children/{child_id}/chests/catalog`: coffres `common`, `rare`, `epic`, couts en Cristaux et apercu de loot.
- `POST /children/{child_id}/chests/catalog/{catalog_id}/open`: deduit les Cristaux et ouvre le coffre.
- `GET /children/{child_id}/chests`: coffres gagnes ou deja ouverts.
- `POST /children/{child_id}/chests/{chest_id}/open`: ouvre un coffre deja possede sans cout supplementaire.

## Collection

- `GET /children/{child_id}/eggs`
- `POST /children/{child_id}/eggs/{egg_id}/evolve`
- `POST /children/{child_id}/eggs/{egg_id}/hatch`
- `GET /children/{child_id}/dragons`
- `POST /children/{child_id}/dragons/{dragon_id}/evolve`
- `POST /children/{child_id}/dragons/{dragon_id}/activate`
- `GET /children/{child_id}/bestiary`

Les etats d'oeuf sont `sleeping`, `warm`, `glowing`, `cracked`, `hatching`.
Les stades dragon canoniques sont `baby`, `young`, `medium`, `large`, `legendary`.
Le stade legacy `guardian` reste lisible pour compatibilite.

## Souhaits et Parchemins

Les Souhaits consomment uniquement les Flammeches lors de leur approbation.
`GET /children/{child_id}/scrolls` conserve `scrolls` pour les coupons approuves et expose aussi `requests` pour suivre les statuts `pending`, `approved`, `refused`, `used` et `expired`.
Une ouverture de coffre en Cristaux ne cree jamais de Parchemin.
