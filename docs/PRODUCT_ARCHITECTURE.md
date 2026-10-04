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

`FamilyTask` est un modèle de tâche de famille avec membres assignés, occurrences, dates, récurrence et validation. Son champ `category` décrit le type de la définition. Depuis la migration `20261004_0011`, chaque `FamilyTaskOccurrence` conserve son propre `category`, copié à sa création et exposé directement par l'API. Le client Android actuel continue provisoirement sa jointure avec les définitions ; il sera simplifié dans une passe suivante.

Pour les nouvelles actions, Android écrit les catégories `TASKODAY_HOUSE_QUEST`, `TASKODAY_PERSONAL_ROUTINE` ou `TASKODAY_PERSONAL_MISSION`. Les `FamilyTask` historiques avec `category` nulle, vide ou `Maison` restent **Quêtes Maison**. Ce choix conserve leur visibilité collective, y compris quand elles ont des assignataires. Une autre catégorie inconnue est une erreur de données. Il évite de reclasser silencieusement des données existantes à partir de la récurrence ou des assignataires. Une Routine personnelle créée dans ce modèle est limitée à un seul membre par l'UI Android ; l'API historique permet encore plusieurs assignataires. Les cas multi assignés anciens doivent être examinés avant toute migration future.

Les noms de tables, classes et routes backend ne sont pas renommés pour le vocabulaire produit. Aucun ancien enregistrement n'est modifié par ce mapping. Les actions créées depuis Ma maison sont des Quêtes Maison ; depuis Exploration ou Suivi, le Parent choisit Routine ou Mission et le même formulaire rapide est préconfiguré pour une personne.

## Invariants du type d'action

- `actionType` est une donnée métier explicite, indépendante des assignataires et de la récurrence. Une Quête Maison reste collective avec zéro, un ou plusieurs participants. Une Routine ou une Mission reste personnelle et possède exactement un participant dans le parcours Android.
- Une Routine doit avoir une récurrence ; la création propose « quotidien » et refuse « jamais ». Une Mission peut être ponctuelle ou récurrente. La récurrence ne reclasse jamais une Mission en Routine.
- Android ne propose pas de conversion du type lors d'une modification et son repository refuse une catégorie différente de celle de la définition chargée. Le backend refuse également un `PATCH category` différent avec HTTP 409 ; renvoyer la même valeur reste accepté. Titre, horaire, récurrence et participants restent modifiables selon les droits existants. Transformer une Mission en Routine impose à terme de désactiver l'ancienne définition et d'en créer une autre.
- Les catégories historiques `null`, vide ou `Maison` sont interprétées comme Quête Maison. Une valeur inconnue ou une occurrence sans définition correspondante est une erreur de données : Android ne la classe pas silencieusement dans Ma maison.
- Chaque occurrence snapshotte `category` dans une colonne non nullable au moment de sa création. Les réponses aujourd'hui, période, retard, completion, validation et réouverture lisent ce snapshot, jamais la catégorie courante de la définition. Android lit encore les définitions de la même famille et joint par `task_id` ; cette lecture supplémentaire reste à retirer côté client.
- Les occurrences antérieures à `20261004_0011` sont backfillées avec la meilleure catégorie actuellement connue de leur définition. `null`, vide et `Maison` deviennent `TASKODAY_HOUSE_QUEST` ; une catégorie inconnue ou une occurrence orpheline fait échouer la migration. Le type historique réel avant cette migration n'est pas reconstructible avec certitude. La garantie forte du snapshot commence avec les occurrences créées après migration. Le titre et les assignataires des anciennes occurrences restent projetés depuis la définition courante : ils ne sont pas des snapshots historiques.
- Un futur `ActionValidated` pourra prendre `actionType` de l'occurrence, avec `task_id`, `occurrence_id`, famille via la définition et horodatage de validation. Le participant récompensé et la structure de l'événement restent à définir ; aucun moteur de récompense n'est ajouté ici.

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
