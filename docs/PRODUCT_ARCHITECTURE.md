# Architecture produit Taskoday

> Taskoday transforme les actions du quotidien en progrès. Chronodria transforme ce progrès en aventure.

Dans Taskoday, Parent et Enfant sont des membres de famille avec des droits différents. Dans Chronodria, ils sont tous des joueurs.

## Frontières

`Action réelle → Taskoday → Validation → Reward Engine → Chronodria`

Taskoday organise la vie réelle : famille, personnes, calendrier, attribution et validation. Le Reward Engine traduit une action validée en récompense. Chronodria consomme cette récompense pour la progression et l'aventure. Taskoday ne connaît ni éclosion, ni évolution, ni inventaire ; Chronodria ne décide ni l'obligation, ni l'assignation, ni la famille d'une action.

Événement cible `ActionValidated` : identifiant de l'utilisateur/joueur, famille, `actionId`, `actionType`, horodatage, paramètres de difficulté éventuels. Pour une Quête Maison, le contrat cible est `QuestHouseValidated → Reward Engine → bonus`. Le Reward Engine doit éviter au Parent de régler manuellement des valeurs quotidiennes complexes.

## Glossaire

| Terme | Sens produit | Exemples | Récompense cible |
|---|---|---|---|
| Action | Terme générique interne | Toute action réelle suivie | Selon le type |
| Routine | Action personnelle répétitive | Dents, sac, matin | Normale |
| Mission | Obligation ou objectif personnel | Devoirs, sport, chambre | Normale, éventuellement accrue selon difficulté |
| Quête Maison | Action collective appartenant au foyer, avec zéro, un ou plusieurs participants | Table, lave-vaisselle, salon, poubelles | Bonus |

L'assignation d'une Quête Maison ne la transforme pas en Mission personnelle. Tous les membres la voient dans Ma maison. Le participant désigné conserve seul le droit de la terminer selon les règles actuelles, sauf Quête non attribuée, commune à tous.

## Navigation officielle

| Espace | Parent | Enfant |
|---|---|---|
| Ma maison | Quêtes Maison : voir et agir ; retard, aujourd'hui, terminées, heure et participants | Les mêmes Quêtes Maison collectives ; action seulement si autorisée |
| Exploration | Mon parcours personnel : mes Routines et Missions, retard/aujourd'hui/terminées | Son parcours personnel, écran d'arrivée, sans actions d'autrui |
| Suivi | Tableau synthétique de supervision : Routines et Missions par membre ; Quêtes Maison et leurs participants, états et validations | Absent |
| Le Nid | Entrée joueur vers Chronodria | Même statut de joueur |

Un Parent arrive sur Ma maison. Un Enfant connecté arrive sur Exploration. Les quatre noms restent inchangés. Le Nid n'a pas de rôle d'administration Chronodria. Le rôle Taskoday ne définit aucune hiérarchie entre joueurs.

## Mapping du modèle actuel

Le backend possède `Routine`, `Mission` et `Quest` dans son ancien moteur de planification pour ChildProfile. Ces tables et API demeurent intactes. Les Routines/Missions distantes synchronisées sont personnelles et restent dans Exploration et Suivi. Les anciennes `Quest` de ce moteur sont liées à un enfant ; elles ne sont pas automatiquement des Quêtes Maison au sens du nouveau produit.

`FamilyTask` est un modèle de tâche de famille avec membres assignés, occurrences, dates, récurrence et validation. Son champ `category` existe déjà dans le schéma et les réponses de définition, mais n'est pas présent dans la réponse d'occurrence. Android associe donc les occurrences aux catégories des définitions reçues par l'API normale, sans migration backend.

Pour les nouvelles actions, Android écrit les catégories `TASKODAY_HOUSE_QUEST`, `TASKODAY_PERSONAL_ROUTINE` ou `TASKODAY_PERSONAL_MISSION`. Les `FamilyTask` historiques sans catégorie reconnue restent **Quêtes Maison**. Ce choix conserve leur visibilité collective, y compris quand elles ont des assignataires. Il évite de reclasser silencieusement des données existantes à partir de la récurrence ou des assignataires. Une Routine personnelle créée dans ce modèle est limitée à un seul membre par l'UI Android ; l'API historique permet encore plusieurs assignataires. Les cas multi assignés anciens doivent être examinés avant toute migration future.

Les noms de tables, classes et routes backend ne sont pas renommés pour le vocabulaire produit. Aucun ancien enregistrement n'est modifié par ce mapping. Les actions créées depuis Ma maison sont des Quêtes Maison ; depuis Exploration ou Suivi, le Parent choisit Routine ou Mission et le même formulaire rapide est préconfiguré pour une personne.

## Droits et validation

Le Parent est membre avec des droits de création, édition, suppression, gestion de famille, validation et accès Suivi. L'Enfant est membre avec droits limités : pas de création/édition/suppression/gestion Parent, mais accomplissement d'une action accessible. `PENDING_VALIDATION` s'affiche « En attente de validation » ; `COMPLETED` et `VALIDATED` s'affichent « Terminée » selon le statut réel. Le backend garde l'autorité sur les transitions.

## Pas encore implémenté

- Reward Engine complet et calcul automatique des récompenses ;
- bonus réel des Quêtes Maison, si absent du moteur actuel ;
- inventaire Chronodria final, progression des créatures, grimoire enrichi ;
- défis familiaux ;
- duplication, modèles et tâches fréquentes ;
- normalisation backend des catégories et examen des anciennes données multi assignées ;
- parcours de création personnelle pour un Parent sans famille active (actuellement la création familiale requiert une famille).
- identité de joueur Chronodria autonome pour chaque Parent : Le Nid Android reste actuellement alimenté par un `ChildProfile` actif ; l'interface n'affiche pas de mode administrateur, mais la séparation des données joueur Parent/Enfant nécessite un contrat backend dédié.
