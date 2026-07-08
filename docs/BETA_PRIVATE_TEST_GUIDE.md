# Guide de test bêta privée Taskoday

## 1. Présentation

Taskoday est une application Android familiale qui aide un parent à créer des routines, missions, quêtes et souhaits pour son enfant. L'enfant avance dans ses actions du jour, gagne des récompenses virtuelles et fait progresser son Nid.

Cette version est une démo MVP destinée à un test privé avec une personne de confiance.

## 2. Télécharger l'application

Release GitHub :

https://github.com/Harkinster/taskoday/releases/tag/v0.1.0-mvp-demo

Télécharger le fichier APK attaché à la release :

`taskoday-v0.1.0-mvp-demo-debug.apk`

## 3. Installer l'APK Android

1. Télécharger l'APK sur le téléphone Android.
2. Ouvrir le fichier APK depuis le gestionnaire de fichiers ou les téléchargements.
3. Si Android le demande, autoriser l'installation depuis cette source.
4. Installer l'application.
5. Ouvrir Taskoday.

## 4. Avertissement

- Cette version est un APK debug.
- Android peut afficher un avertissement de sécurité pendant l'installation.
- L'application n'est pas encore disponible sur Google Play.
- Cette version sert uniquement à tester le parcours MVP.
- Ne pas utiliser de mot de passe personnel sensible pour ce test.

## 5. Parcours de test conseillé

1. Se connecter avec un compte parent de test.
2. Vérifier le tableau de bord parent.
3. Créer un enfant, ou sélectionner l'enfant déjà disponible.
4. Définir un PIN parent.
5. Créer une routine simple.
6. Passer en mode enfant.
7. Vérifier que les actions parent ne sont plus visibles.
8. Valider une action côté enfant.
9. Regarder Le Nid et les compteurs de progression.
10. Revenir côté parent avec le PIN.
11. Créer un souhait dans la Caverne.
12. Repasser en mode enfant et demander ce souhait.
13. Revenir côté parent et valider ou refuser la demande.

Comptes de test :

- Parent : `<email_parent_de_test>`
- Enfant : `<email_enfant_de_test>`
- Mot de passe : `<fourni séparément>`

## 6. Déjà fonctionnel

- Connexion parent au backend réel.
- Gestion d'un enfant actif.
- Création de routines, missions et quêtes.
- Modification et suppression d'actions parent.
- Tableau de bord parent.
- Mode enfant local.
- Protection du retour parent par PIN local.
- Actions du jour côté enfant.
- Validation d'action avec récompense.
- Le Nid : XP, Flammèches, Cristaux, inventaire, œuf, bestiaire.
- Caverne : souhaits, coffres et parchemins.
- Demande de souhait côté enfant.
- Validation, refus et utilisation de souhait côté parent.
- Journal d'activité parent et enfant.
- Écran Premium informatif.

## 7. Limites connues

- Premium non actif.
- Aucun paiement réel.
- Pas encore sur Google Play.
- Pas de notifications Android.
- Mode enfant local seulement.
- APK de test debug.
- Les comptes de test peuvent contenir des données de démonstration.
- Le premier chargement peut être un peu lent selon la connexion ou le serveur.

## 8. Retours attendus

Merci de noter en priorité :

- bugs ou crashs ;
- écrans confus ;
- textes incompréhensibles ;
- lenteurs ;
- actions difficiles à trouver ;
- idées vraiment utiles pour un parent ou un enfant.

## 9. Modèle de retour bug

```text
Téléphone utilisé :
Version Android :
Compte utilisé :
Écran concerné :
Action faite :
Résultat attendu :
Résultat obtenu :
Capture jointe : oui/non
Commentaire :
```
