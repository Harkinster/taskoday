# Proposition de passes beta08

Le dépôt conserve 1.0-beta07 / code 8 pendant cette intervention. Aucun changement de version.json ou latest.apk. L'incrément ne sera choisi que lors d'une publication explicitement autorisée.

| Passe | Priorité / résultat | Dépendances | Risques et validation |
| --- | --- | --- | --- |
| Gratuité | Indispensable : retirer plafonds, écran et compteurs ; conserver rôles, données et API | beta07 sauvegardée et audit précis | Anciens parcours, permissions, souhaits ; tests Android/backend/API et captures |
| Retours réels beta07 | Souhaitable : défauts reproduits au quotidien | Retours propriétaire et scénario isolé | Pas de refonte ni d'optimisation spéculative |
| Notifications locales | Première implémentation de développement : échéances, validation, reprise, retards importants ; préférences par compte | WorkManager, événements métier existants, QA isolée ; travaux non committés | Livraison non exacte et limitée à la famille active ; valider les limites Android et les retours d'usage avant toute publication |
| Réseau / sessions | Souhaitable : anomalies démontrées par traces expurgées | Reproduction et contrats API | Préserver sessions et réponses associées au bon compte |
| Release beta08 | Indispensable avant publication : recette ciblée, build signé, compatibilité et repli | Validation du périmètre final | Incrément alors seulement ; certificat inchangé, sauvegarde et contrôles publics |

À reporter : Premium Chronodria, nouvelle économie, achats intégrés, service cloud non validé, refonte graphique et fonctionnalités de jeu complexes.

La gratuité ne dépend ni des notifications ni d'une monétisation. Aucun déploiement backend ou migration nécessaire pour les plafonds retirés côté Android ; conserver les nouveaux tests de contrat en régression.

La transition gratuite est sauvegardée dans le checkpoint local 54ba02d. La base de notifications reçoit un checkpoint local dédié après sa recette API 30/API 33 : aucun push ni publication. Les alertes entre appareils fonctionnent par interrogation différée de l'API ; le push immédiat et la synchronisation de lecture restent à préparer. Seuls l'historique beta07 déjà publié et son tag sont synchronisés avec le distant.
