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

Dans une reponse d'ouverture, `loot` contient uniquement les gains du coffre:

- `loot[].quantity`: quantite gagnee pendant cette ouverture.
- `loot[].quantity_total`: quantite totale possedee apres ajout.
- `loot[].is_duplicate_compensation`: indique un gain cree par compensation d'un oeuf en doublon.
- `inventory_after`: inventaire complet apres l'ouverture.

## Collection

- `GET /children/{child_id}/eggs`
- `POST /children/{child_id}/eggs/{egg_id}/evolve`
- `POST /children/{child_id}/eggs/{egg_id}/hatch`
- `GET /children/{child_id}/dragons`
- `POST /children/{child_id}/dragons/{dragon_id}/evolve`
- `POST /children/{child_id}/dragons/{dragon_id}/activate`
- `GET /children/{child_id}/bestiary`

Les etats d'oeuf sont `sleeping`, `warm`, `glowing`, `cracked`, `hatching`.
`POST /children/{child_id}/eggs/{egg_id}/evolve` avance strictement d'un etat par appel. Un appel sur un oeuf `hatching` peut creer le dragon `baby`. Aucun dragon ne peut etre cree avant l'etat `hatching`.
L'endpoint de compatibilite `POST /children/{child_id}/eggs/{egg_id}/hatch` refuse egalement un oeuf qui n'est pas deja `hatching`.
Les stades dragon canoniques sont `baby`, `young`, `medium`, `large`, `legendary`.
Le stade legacy `guardian` reste lisible pour compatibilite.

## Souhaits et Parchemins

Les Souhaits consomment uniquement les Flammeches lors de leur approbation.
`GET /children/{child_id}/scrolls` conserve `scrolls` pour les coupons approuves et expose aussi `requests` pour suivre les statuts `pending`, `approved`, `refused`, `used` et `expired`.
Une ouverture de coffre en Cristaux ne cree jamais de Parchemin.

## Erreurs

- `400`: regle metier refusee, par exemple ressources insuffisantes ou transition d'etat invalide.
- `401`: token invalide.
- `403`: action non autorisee.
- `404`: ressource introuvable.
- `422`: validation FastAPI d'un payload ou parametre invalide lorsqu'elle est annoncee par OpenAPI.
