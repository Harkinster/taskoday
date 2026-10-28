# Proposition de passes beta08

Le dépôt conserve 1.0-beta07 / code 8 pendant cette intervention. Aucun changement de version.json ou latest.apk. L'incrément ne sera choisi que lors d'une publication explicitement autorisée.

| Passe | Priorité / résultat | Dépendances | Risques et validation |
| --- | --- | --- | --- |
| Gratuité | Indispensable : retirer plafonds, écran et compteurs ; conserver rôles, données et API | beta07 sauvegardée et audit précis | Anciens parcours, permissions, souhaits ; tests Android/backend/API et captures |
| Retours réels beta07 | Souhaitable : défauts reproduits au quotidien | Retours propriétaire et scénario isolé | Pas de refonte ni d'optimisation spéculative |
| Notifications locales | Souhaitable : résumé existant, portée utilisateur, annulation | Plan d'architecture et tests comptes/familles | Confidentialité, doublons, délais Android, fuseaux |
| Réseau / sessions | Souhaitable : anomalies démontrées par traces expurgées | Reproduction et contrats API | Préserver sessions et réponses associées au bon compte |
| Release beta08 | Indispensable avant publication : recette ciblée, build signé, compatibilité et repli | Validation du périmètre final | Incrément alors seulement ; certificat inchangé, sauvegarde et contrôles publics |

À reporter : Premium Chronodria, nouvelle économie, achats intégrés, service cloud non validé, refonte graphique et fonctionnalités de jeu complexes.

La gratuité ne dépend ni des notifications ni d'une monétisation. Aucun déploiement backend ou migration nécessaire pour les plafonds retirés côté Android ; conserver les nouveaux tests de contrat en régression.

La transition gratuite est sauvegardée dans un checkpoint local dédié. Les développements de notifications qui suivent restent non committés et non poussés pour validation. Seuls l'historique beta07 déjà publié et son tag sont synchronisés séparément.
