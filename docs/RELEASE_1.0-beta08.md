# Taskoday 1.0-beta08 — Release Candidate locale

Version Android : **1.0-beta08**, code **9**, package **com.harkinster.taskoday**.
Préparée pour revue ; aucune publication, modification de version.json/latest.apk, migration ou opération sur les données de production.

## Contenu

- Checkpoint gratuit : `54ba02dbd289b568af5683f489b82010d4cb7d8c`.
- Checkpoint notifications : `e8e711f020897af38cd030dd2bcdb2539026181a`.
- Organisation familiale gratuite : plafonds commerciaux et accès Premium supprimés, permissions Parent/Enfant conservées.
- Notifications locales à partir de l’API : échéances personnelles, missions enfant à valider, décisions de validation/reprise, retards importants. Préférences par compte, catégories et plage silencieuse.
- Progression Chronodria, coûts des récompenses, données et règles de sécurité préservés. Aucun achat réel ou abonnement activé.

## Livraison et limites

Notifications désactivées par défaut, permission Android demandée lors de l’activation explicite. WorkManager interroge l’API avec réseau, intervalle minimal de 15 minutes ; Android peut différer l’exécution. Le fonctionnement inter-appareils repose sur ce polling, sans push ni garantie d’immédiateté. Session valide et famille active nécessaires. Après arrêt forcé, rouvrir l’application. Une déconnexion annule les travaux et alertes de cet appareil.

Préférences et déduplication locales par compte et installation, sans synchronisation de lecture entre appareils. Fenêtre des événements de 24 heures ; occurrences de J−7 à J+1 et retards : les décisions sur une ancienne occurrence hors fenêtre peuvent être manquées. Pas d’alerte de récompense ajoutée.

Les anciens réglages globaux de résumé sont conservés, mais ne sont pas attribués automatiquement à un compte. Après mise à jour, chaque utilisateur doit activer ses nouveaux rappels dans Profil → Notifications, même si un ancien résumé global était configuré.

## Compatibilité et signature

- Signature Release officielle conservée : SHA-256 certificat `4783e3e7f20c4bfec5e6b22fb0e3cfcaeb0dfddca3ebc380929c7fb1243a666b`.
- API : `https://harkserv.ddns.net/taskoday-api/` ; updater : `https://harkserv.ddns.net/taskoday/version.json`.
- Même package et certificat que beta07 ; mise à jour sans désinstallation. Aucun changement de schéma local Room, aucune migration backend.
- Les contrats publics et services métier actifs sont vérifiés en lecture seule. Aucun compte réel n’est utilisé pour une mutation de test.

Les résultats réellement exécutés, la matrice Android, les empreintes du binaire définitif et la procédure de publication/retour arrière sont dans `qa/release-beta08-candidate/`. Les anciens résultats ne remplacent pas les contrôles de cette RC. Aucun tag définitif ou push avant autorisation de publication.

## Artefact local contrôlé

| Métadonnée | Valeur |
| --- | --- |
| APK RC | `qa/release-beta08-candidate/artifacts/Taskoday-1.0-beta08-rc.apk` |
| Taille | 93 402 147 octets |
| SHA-256 APK | `93b82285557b5916a6a7f15665662d04eeb91fecc6f54cfc94955996a33b6d10` |
| Signature | Release officielle, v2 valide, certificat identique à la beta07 publique |
| API / configuration | Production, sans suffixe Debug/QA ni valeur privée connue détectée |

Mise à jour beta07 signée → RC testée sur un AVD Android13, hors ligne et sans désinstallation : UID, fixture locale Room5 et fichier témoin conservés. Aucun compte réel utilisé. Build Release et Lint réussis ; 487 tests unitaires Android, 26 tests backend ciblés et 7 instrumentés pertinents réexécutés avec succès. Android14+ n’a pas été exécuté, faute d’AVD existant. Ces résultats ne remplacent pas un éventuel futur suivi d’autonomie sur téléphone.

## Publication ultérieure

Employer le publisher officiel existant après GO : sauvegarder les métadonnées, valider le binaire, créer l’archive immuable, vérifier intégrité/accessibilité et basculer atomiquement la distribution en dernier. Conserver beta07. En cas de retour arrière de distribution, sélectionner la release code 8 et vérifier ; Android ne rétrograde pas une installation déjà au code 9. Un correctif ultérieur devra avoir un code supérieur à tous les codes publiés.
