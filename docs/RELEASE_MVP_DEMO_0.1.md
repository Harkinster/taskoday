# Taskoday MVP Demo

Date : 2026-07-08

Version APK actuelle :
- versionName : 1.0
- versionCode : 1
- package : com.example.taskoday
- APK debug : app/build/outputs/apk/debug/app-debug.apk

## Fonctionnalités incluses

- Connexion parent au backend réel via HTTPS.
- Création et gestion d'enfant côté parent.
- Sélection d'un enfant actif.
- Limites Gratuit / Premium provisoires.
- Écran Premium informatif, sans paiement réel.
- PIN parent local.
- Mode enfant local depuis le compte parent.
- Création, modification et suppression de Routine, Mission et Quête.
- Modèles rapides pour les actions parent.
- Écran enfant Aujourd'hui avec actions du jour.
- Validation d'action enfant avec retour de récompense.
- Le Nid avec XP, Flammèches, Cristaux, inventaire, œuf et bestiaire.
- Caverne avec Souhaits et Coffres.
- Création de souhaits parent avec modèles rapides.
- Demande de souhait enfant, validation/refus parent, parchemin utilisé.
- Journal d'activité parent et enfant.
- Onboarding Premiers pas et états vides pour nouvel enfant.

## Parcours démo validé

- Ouvrir l'app et montrer la connexion.
- Se connecter avec le compte parent de test, sans exposer le mot de passe.
- Montrer le tableau de bord parent : enfant actif, XP, monnaies, actions du jour.
- Ouvrir Profil : gestion enfant, PIN parent, Premium, mode enfant.
- Ouvrir Premium : limites gratuites et offre à venir.
- Ouvrir Ajouter une action : choix Routine, Mission, Quête et idées rapides.
- Ouvrir Journal : dernières actions et souhaits.
- Ouvrir Le Nid : progression, monnaies, œuf suivi.
- Ouvrir Caverne : souhaits, coffres et parchemins.
- Passer en mode enfant local.
- Vérifier que les actions parent sont masquées.
- Tester Retour parent : mauvais PIN refusé, bon PIN accepté.

## Bugs connus non bloquants

- Certains comptes de test contiennent des données issues des smoke tests précédents.
- Le premier appel réseau peut être lent selon l'état du backend ou du reverse proxy.
- L'écran Premium est informatif uniquement : aucun achat réel n'est intégré.
- Le mode enfant local dépend d'un PIN parent défini sur l'appareil.

## Limites actuelles

- Gratuit : 1 enfant, 5 routines actives, 3 missions actives, 1 quête active, 3 souhaits actifs.
- Premium non commercialisé dans l'app.
- Pas de Google Play Billing.
- Pas de notification système.
- Pas de récupération de PIN parent.
- Pas de compte enfant multi-appareil dans cette version de démonstration.

## Prochaines priorités

- Nettoyer les données de démo avant présentation externe.
- Préparer un build signé hors debug.
- Ajouter des tests UI ciblés sur les parcours parent/enfant.
- Finaliser la stratégie Premium et paiement.
- Renforcer le suivi manuel des bugs bêta.
