# Rewards System

## Separation des monnaies

- L'XP du Gardien est reservee a la progression. Elle n'est jamais depensee.
- Les Flammeches sont la seule monnaie pour les Souhaits crees par les parents.
- Les Cristaux et consommables servent au Nid, aux Coffres, aux Oeufs, aux Dragons et aux Perchoirs.

## Caverne aux Souhaits

Les parents creent des Souhaits actifs pour un enfant :

- `id`
- `title`
- `description`
- `cost_scales`, champ backend-compatible qui represente le cout en Flammeches
- `is_active`
- `emoji` optionnel

Le MVP garde les routes `/rewards` et ajoute les alias `/wishes`.

## Parcours Souhait

1. L'enfant peut demander un Souhait actif si son solde de Flammeches couvre le cout.
2. La demande est creee avec le statut `pending`.
3. Une demande en attente ne depense pas de Flammeches.
4. Le parent peut accepter ou refuser.
5. L'acceptation verifie le solde, retire les Flammeches et cree un Parchemin.
6. Le refus ne retire rien.

Statuts de demande :

- `pending`
- `approved`
- `refused`
- `used`
- `expired`

Statuts de Parchemin :

- `available`
- `used`
- `expired`
- `cancelled`

## Compatibilite

Noms historiques conserves :

- `/children/{child_id}/scales`
- `/rewards/{reward_id}/requests`
- `/reward-coupons/{coupon_id}/use`
- `cost_scales`
- `scales_balance`

Alias et vocabulaire cible :

- `/children/{child_id}/flammeches`
- `/children/{child_id}/wishes`
- `/wishes/{reward_id}/requests`
- `/children/{child_id}/scrolls`
- `/scrolls/{coupon_id}/use`
- `flammeches_balance`

Les noms legacy ne doivent pas etre affiches dans l'UI utilisateur.
