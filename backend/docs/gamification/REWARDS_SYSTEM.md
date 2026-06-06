# Souhaits et Parchemins

Les Souhaits sont les recompenses externes creees par les parents. Leur cout technique reste `cost_scales` pour compatibilite, mais la monnaie affichee et depensee est la Flammeche.

Le cycle d'une demande est `pending`, puis `approved`, `refused`, `used` ou `expired`.
Une approbation depense les Flammeches et cree un Parchemin. Un Parchemin peut ensuite etre marque `used`.
L'endpoint `scrolls` expose les coupons approuves dans `scrolls` et le suivi complet des demandes dans `requests`.

Les Cristaux sont reserves aux coffres et ne participent jamais au cycle des Souhaits ou Parchemins.
