# Taskoday 1.0-beta08 — Plus de liberté, plus de rappels

**Publication officielle vérifiée**, manifeste daté `2026-10-29T22:51:29.920162Z`. Version Android **1.0-beta08**, code **9**, package **com.harkinster.taskoday**. Mise à jour facultative, version minimale supportée : 1.

## Taskoday devient entièrement gratuit

Suppression des anciennes limitations Premium sur le nombre d’enfants, routines, missions, quêtes et souhaits. Toutes les fonctions d’organisation familiale sont accessibles sans abonnement. Les permissions Parent/Enfant, la progression Chronodria et les achats en monnaie de jeu restent inchangés. Aucun paiement réel ou abonnement Chronodria activé.

## Nouvelles notifications

- Rappels des échéances personnelles et alertes pour les retards importants.
- Missions en attente de validation, missions validées ou remises à faire.
- Préférences propres à chaque utilisateur : activation volontaire, catégories et plages silencieuses dans Profil → Notifications.
- Gestion des permissions Android, déduplication et annulation après déconnexion.

Les notifications entre appareils reposent sur une **synchronisation périodique**, sans garantie d’arrivée immédiate. WorkManager utilise un intervalle minimal de 15 minutes avec réseau ; Android peut différer l’exécution. Session valide et famille active nécessaires. Les anciennes préférences globales sont conservées mais les nouveaux rappels doivent être activés par chaque compte.

## Binaire officiel

| Métadonnée | Valeur |
| --- | --- |
| Variante | Release non débogable, non testOnly, signature v2 valide |
| Taille | 93 402 147 octets |
| SHA-256 APK | `93b82285557b5916a6a7f15665662d04eeb91fecc6f54cfc94955996a33b6d10` |
| SHA-256 certificat officiel | `4783e3e7f20c4bfec5e6b22fb0e3cfcaeb0dfddca3ebc380929c7fb1243a666b` |
| Archive immuable | https://harkserv.ddns.net/taskoday/releases/v9-93b82285557b5916/taskoday-1.0-beta08-9.apk |
| Téléchargement permanent | https://harkserv.ddns.net/taskoday/latest.apk |
| Manifeste | https://harkserv.ddns.net/taskoday/version.json |
| Site | https://harkserv.ddns.net/taskoday/ |
| Backend conservé | https://harkserv.ddns.net/taskoday-api/ |

L’APK est exactement la RC validée, sans reconstruction ni nouvelle signature. Son package et certificat sont identiques à beta07. Mise à jour sans désinstallation ; aucun changement de schéma Room5 ni migration backend.

## Provenance et contrôles

- Gratuité : `54ba02dbd289b568af5683f489b82010d4cb7d8c`.
- Notifications : `e8e711f020897af38cd030dd2bcdb2539026181a`.
- Code binaire / préparation RC : `69a334ebc62a52bdb58598653958926d08949ce5`. Le commit de publication modifie seulement les notes officielles et ce document ; le code applicatif reste identique.
- Contrôles de cette publication : taille/SHA/signature/version/package/alignement/configuration Release, validation privée du publisher officiel, téléchargements intégraux de l’archive et de latest.apk avant annonce puis après publication, schéma du manifeste, site JavaScript, services et journaux.
- Backend public : 96 opérations et 122 schémas associés conformes, sources actives inchangées ; aucun déploiement, redémarrage ou accès direct à une base métier.
- Aucun nouveau build ou campagne QA : les 487 tests unitaires, 26 backend ciblés et 7 instrumentés Android13 restent les résultats de la RC, pas de cette publication.

## Limites et retour arrière

Android14+ et autonomie physique non testés pendant la RC ; notifications périodiques ; ANR initial du banc non reproduit sur la RC. Ces limites ne sont pas des anomalies de production confirmées.

Beta05, beta06 et beta07 sont conservées. Pour restaurer la distribution beta07 avec le mécanisme officiel : `publisher.py --channel production select-release --version-code 8 --confirm-rollback`, puis `verify`. Cette sélection ne rétrograde pas un téléphone déjà au code9 ; tout correctif devra utiliser un code supérieur aux codes publiés. Aucun rollback exécuté lors de cette publication réussie.

Preuves détaillées, sauvegarde vérifiée et procédure ordonnée de repli : `qa/release-beta08/`. Aucun APK, rapport QA, secret, clé ou base inclus dans le commit.
