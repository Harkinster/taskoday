# Démo MVP Taskoday

## Comptes de test

- Parent : `parent.test@harkserv.ddns.net`
- Enfant : `enfant.test@harkserv.ddns.net`
- Mot de passe : à récupérer auprès du mainteneur du projet, ne pas le publier dans ce fichier.

## Parcours démo recommandé

1. Ouvrir l'application et montrer l'écran de connexion.
2. Se connecter avec le compte parent.
3. Montrer l'accueil parent : enfant actif, XP, Flammèches, Cristaux, actions du jour.
4. Ouvrir le Profil : enfant actif, gestion des enfants, PIN parent, accès Premium.
5. Ouvrir Premium : limites gratuites et offre à venir.
6. Ouvrir Ajouter une action : choix Routine, Mission, Quête.
7. Ouvrir Le Nid : monnaies, progression, œuf/compagnon.
8. Ouvrir la Caverne : Souhaits et Coffres.
9. Montrer le Journal si des événements récents existent.

## Écrans à montrer

- Connexion / inscription parent.
- Accueil parent.
- Profil parent.
- Premium Taskoday.
- Ajouter une action.
- Aujourd'hui / Routine.
- Mission.
- Quête.
- Le Nid.
- Caverne.
- Journal.

## Déjà fonctionnel

- Connexion au backend réel via HTTPS.
- Sélection d'enfant actif côté parent.
- Création et gestion d'actions parent.
- Affichage des actions du jour.
- Validation d'actions côté enfant.
- Progression XP, Flammèches et Cristaux.
- Le Nid avec inventaire, bestiaire et œuf.
- Caverne avec Souhaits et Coffres.
- Limites gratuites et écran Premium temporaire.
- Mode enfant local préparé, avec retour parent protégé par PIN si configuré.

## Bugs connus non bloquants

- Le premier appel de login peut être lent selon l'état du backend/proxy ; relancer si un timeout apparaît.
- Le mode enfant local reste indisponible tant qu'aucun PIN parent n'est défini.
- L'écran Premium est informatif uniquement : aucun paiement réel n'est intégré.
- Certains comptes de test peuvent contenir des données issues de smoke tests précédents.
