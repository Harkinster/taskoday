# Architecture produit Taskoday

> Taskoday transforme les actions du quotidien en progrès. Chronodria transforme ce progrès en aventure.

Dans Taskoday, Parent et Enfant sont des membres de famille avec des droits différents. Dans Chronodria, ils sont tous des joueurs.

## Frontières

`Action réelle → Taskoday → Validation → Reward Engine → Chronodria`

Taskoday organise la vie réelle : famille, personnes, calendrier, attribution et validation. Le Reward Engine traduit une réalisation finale reconnue en points de récompense neutres. Chronodria consommera ces points plus tard pour la progression et l'aventure. Taskoday ne connaît ni éclosion, ni évolution, ni inventaire ; Chronodria ne décide ni qui devait agir, ni si une action était obligatoire, ni quelle famille la possède.

## Identité canonique d'une action

Deux dimensions indépendantes décrivent chaque `FamilyTask` :

| Dimension | Valeur | Sens |
|---|---|---|
| `scope` | `PERSONAL` | Action d'un membre ; Exploration de ce membre |
| `scope` | `HOUSE` | Action du foyer ; Ma maison, visible collectivement |
| `kind` | `ROUTINE` | Action répétitive, avec récurrence obligatoire et date de fin facultative |
| `kind` | `MISSION` | Action ponctuelle, sans récurrence, avec échéance facultative |
| `kind` | `QUEST` | Challenge bonus du foyer, ponctuel ou récurrent |

Les cinq combinaisons supportées sont `PERSONAL/ROUTINE`, `PERSONAL/MISSION`, `HOUSE/ROUTINE`, `HOUSE/MISSION` et `HOUSE/QUEST`. `PERSONAL/QUEST` est interdit pour l'instant. La répétition ne transforme pas une Quête en Routine ; l'attribution à une personne ne transforme pas une action Maison en action personnelle.

Une Routine possède une règle de répétition valide (`DAILY`, `WEEKLY` ou `SELECTED_WEEKDAYS`) ; son début est explicite ou calculé depuis la création et sa fin peut être absente. Une Mission a `recurrence=NONE` et peut rester ouverte sans échéance : son occurrence possède alors `scheduled_date=NULL`, sans fausse date d'échéance. Une Quête appartient toujours à `HOUSE` et peut avoir `recurrence=NONE` ou une règle de répétition. Le bonus des Quêtes sera défini plus tard par le Reward Engine.

Un Parent peut créer et gérer selon ses droits. Une action personnelle nouvelle concerne un membre dans le parcours Android ; les données historiques multi assignées restent conservées. Une action Maison peut être non attribuée, attribuée à une personne ou à plusieurs. Visibilité collective et permission de terminer restent distinctes.

## Navigation officielle

| Espace | Parent | Enfant |
|---|---|---|
| Ma maison | Routines, Missions et Quêtes Maison ; actions et participants | Les mêmes actions Maison collectives ; peut agir selon attribution |
| Exploration | Mes Routines et Missions personnelles | Ses Routines et Missions personnelles ; écran d'arrivée, sans celles d'autrui |
| Suivi | Supervision synthétique des actions personnelles des membres et des trois kinds Maison | Absent |
| Le Nid | Entrée joueur vers Chronodria | Même statut de joueur |

Le Parent arrive sur Ma maison. L'Enfant arrive sur Exploration. Les quatre noms restent inchangés. Suivi décrit **l'état actuel** ; le Journal secondaire raconte **les transitions passées**. Parent ouvre le Journal familial depuis Suivi ; Enfant ouvre Mon journal depuis Exploration. Le Journal Taskoday n'est pas un historique XP ou d'inventaire.

## Mapping technique et compatibilité

Le backend conserve provisoirement `category` comme champ de compatibilité, tandis que `scope` et `kind` deviennent l'identité explicite de la définition. `FamilyActionType` côté Android représente uniquement une combinaison supportée et centralise le mapping du champ legacy ; les décisions de visibilité utilisent `scope`, celles de récurrence et d'affichage utilisent `kind`.

| `category` historique | `scope` | `kind` |
|---|---|---|
| `TASKODAY_PERSONAL_ROUTINE` | `PERSONAL` | `ROUTINE` |
| `TASKODAY_PERSONAL_MISSION` | `PERSONAL` | `MISSION` |
| `TASKODAY_HOUSE_QUEST` | `HOUSE` | `QUEST` |
| `NULL`, vide ou `Maison` sur une définition ancienne | `HOUSE` | `QUEST` |

Les nouvelles combinaisons Maison utilisent temporairement `TASKODAY_HOUSE_ROUTINE` et `TASKODAY_HOUSE_MISSION` dans `category`. Une valeur inconnue fait échouer le mapping ; elle n'est jamais déduite de la récurrence ou des assignataires. Les anciennes tables `Routine`, `Mission` et `Quest` du moteur ChildProfile restent distinctes et ne sont pas renommées.

La migration `20261006_0013` backfille chaque définition depuis sa propre `category`, chaque occurrence depuis **son propre snapshot** et chaque événement depuis **son propre snapshot**. Elle ne réécrit pas l'identité historique à partir de la définition courante. Le backend expose `scope` et `kind` sur les définitions, occurrences et événements, en gardant `category` pendant la transition. Le client Android existant avant cette passe sait lire les trois catégories anciennes ; son déploiement doit précéder toute création des deux nouvelles catégories Maison en production.

Après création, `scope` et `kind` sont immuables. Un PATCH renvoyant les mêmes valeurs est accepté ; un changement retourne 409 avant mutation. Transformer une action impose une nouvelle définition. La validation backend refuse `PERSONAL/QUEST`, une Routine sans répétition et une Mission récurrente. Les définitions historiques déjà récurrentes avec `kind=MISSION` restent en base sans réécriture ; une nouvelle création invalide est refusée. La duplication crée une nouvelle définition avec le même `scope` et le même `kind`, sans copier occurrence ou completion. « Récemment créées » est un raccourci contextuel issu des définitions actives, pas un modèle persistant ; une Routine active n'est pas suggérée à la recréation quotidienne.

## Occurrences et historique

Depuis `20261004_0011`, une occurrence snapshotte sa catégorie à sa naissance. Depuis `20261006_0013`, elle snapshotte aussi `scope` et `kind`. Modifier ensuite titre, date, assignataires ou définition ne change pas ces snapshots. La migration des anciennes occurrences utilise la meilleure classification connue de leur propre `category` ; l'identité antérieure à 0011 n'est pas reconstructible avec certitude. Un événement task-events conserve séparément la catégorie et le couple `scope/kind` de l'occurrence, ainsi que son titre, son acteur et son horodatage.

Depuis `20261005_0012`, chaque transition effective `COMPLETE`, `VALIDATE` ou `REOPEN` reste dans un registre append-only. L'occurrence exprime l'état courant ; une réouverture remet cet état à `TODO` sans effacer les transitions précédentes. Le registre distingue l'acteur réel du participant prévu et permet plusieurs cycles. Les cycles rouverts avant 0012 sont irrécupérables. Une action Maison multi attribuée conserve une occurrence et un état partagés ; les bénéficiaires de récompense sont déterminés depuis les contributeurs réels du cycle.

Depuis `20261006_0014`, les actions `HOUSE` distinguent les assignataires prévus des contributeurs réels. `START` ajoute le premier contributeur, `JOIN` ajoute un membre de la famille, et seul un contributeur du cycle peut terminer l'occurrence partagée. `REOPEN` conserve les contributeurs du cycle précédent et ouvre un nouveau cycle sans participants. La validation Parent n'ajoute pas le Parent aux contributeurs. L'historique legacy ne déduit que l'acteur `COMPLETE` comme contribution certaine ; la collaboration complète est garantie à partir de 0014.

Une `MISSION` ponctuelle échue reste ouverte et en retard jusqu'à décision du Parent. Le Parent peut la reporter (`RESCHEDULE`, ancienne et nouvelle échéances journalisées) ou la marquer ratée (`FAIL`, état final `FAILED`). Une Mission sans échéance reste ouverte ; les Routines manquées et les Quêtes échues ne sont pas automatiquement marquées ratées.

Pour un futur `ActionValidated`, famille, action, occurrence, `scope`, `kind`, acteur de completion, acteur de validation et horodatages proviennent de l'occurrence et du registre.

## Reward Engine et ?conomie personnelle V1

Le backend est l'autorit? et attribue un bundle dans la m?me transaction que la finalisation reconnue. Sans validation Parent, `COMPLETE` attribue le bundle ; avec validation, `COMPLETE` reste sans r?compense et `VALIDATE` l'attribue. `START`, `JOIN`, `RESCHEDULE` et `FAIL` ne donnent rien. `REOPEN` conserve et r?voque toutes les composantes du bundle ; le nouveau cycle est ind?pendant.

Chaque membre poss?de ses propres ressources : aucun portefeuille familial, transfert, don ou partage. Les Points Taskoday sont permanents et non d?pensables ; ils mesurent l'effort total reconnu. Les Flamm?ches sont personnelles et d?pensables dans la future Caverne des souhaits. Les Cristaux sont personnels et destin?s aux futurs coffres Chronodria. Aucune d?pense ni conversion n'est impl?ment?e ici.

La policy centralis?e provisoire V2 (version 2, effort modifier 1.0) est : Routine = 10 Points, 1 Flamm?che, 1 Cristal ; Qu?te = 25 Points, 2 Flamm?ches, 3 Cristaux ; Mission = 30 Points, 3 Flamm?ches, 4 Cristaux, plus une chance ind?pendante de 25 % par b?n?ficiaire de gagner 2 Cristaux. Une Mission reste plus r?mun?ratrice qu'une Qu?te sans bonus. Ces chiffres sont des valeurs d'?quilibrage V1 modifiables dans un seul emplacement, pas l'?conomie finale Chronodria.

Les anciens `RewardGrant` conservent leurs montants et leur policy version 1 ; ils ne sont ni recalcul?s ni convertis en ressources. La migration `20261006_0016` ne cr?e aucun grant de ressources r?troactif. Le ledger des grants constitue la source de v?rit? ; les soldes sont la somme des grants actifs. Les lignes de Points et de ressources partagent beneficiary, occurrence, cycle et d?clencheur. Le bonus Mission est tir? une seule fois et son r?sultat (0 ou 2 Cristaux) est persist? sur le grant ainsi que sa composante ?ventuelle ; un retry ne peut pas relancer le tirage. Chaque contributeur Maison re?oit le bundle complet ; les assignataires et Parent validateur ne sont r?compens?s que s'ils sont eux-m?mes b?n?ficiaires selon les r?gles d'action. Le r?sultat du Parent validateur n'expose jamais les gains d'un autre membre comme siens.

Le Nid montre uniquement les ressources personnelles du compte connect?. Les futures r?gles de la Caverne (demande, validation Parent, d?bit apr?s acceptation) et les co?ts/contenus des coffres restent ? d?finir. Les futurs D?fis familiaux formeront un syst?me s?par? et ne mutualiseront aucun portefeuille.

## Utilisation des ressources V1

Depuis la migration `20261007_0017`, les Flammèches et Cristaux sont dépensables via des écritures append-only. Le solde est calculé depuis les grants actifs moins les dépenses non annulées ; aucun champ de solde mutable et aucun portefeuille familial ne sont ajoutés. Les Points Taskoday restent permanents et ne peuvent pas être dépensés.

La Caverne des souhaits utilise les Flammèches. Le Parent crée, modifie et désactive le catalogue de sa famille. Un Enfant peut créer une demande `PENDING` ou l'annuler tant qu'elle attend ; aucune Flammèche ne part avant acceptation. L'approbation Parent débite et passe la demande à `APPROVED` dans la même transaction. Un refus conserve `REJECTED` sans débit. Un Parent peut aussi `Obtenir` un souhait pour lui-même ; la demande personnelle est directement approuvée et débitée. Les demandes gardent leur titre et leur coût au moment de la demande.

Les Coffres utilisent les Cristaux du compte connecté et s'ouvrent directement : Commun = 3 Cristaux / 1 drop, Rare = 8 / 3 drops, Épique = 15 / 6 drops. La policy est centralisée et provisoire. Une ouverture persistée conserve son type, son coût, sa clé d'idempotence et tous ses drops. Les tirages réutilisent les objets du catalogue Chronodria existant et incrémentent `ItemInventory`, sans créer de portefeuille de coffres ni de nouvel inventaire parallèle. Parent et Enfant ont des collections distinctes. Les doublons augmentent la quantité ; aucun œuf n'éclot automatiquement.

Un retry d'approbation ou d'ouverture ne redébite pas et ne relance pas le tirage. Une réouverture d'action vérifie d'abord l'effet de révocation du bundle sur les Flammèches et Cristaux. Elle est refusée avec 409 si une balance deviendrait négative ; autrement le bundle entier est révoqué atomiquement. Les dépenses ne peuvent pas rendre un compte débiteur.

Les anciennes tables Chronodria (`ItemInventory`, oeufs, dragons, progression et coffres hérités) restent des systèmes persistés distincts. Le nouveau coffre utilise le catalogue `ITEM_CATALOG` et `ItemInventory` comme collection, mais n'altère pas le flux legacy d'ouverture, d'évolution ou d'éclosion. Les anciennes tables de portefeuille mutable restent hors du calcul des nouvelles balances.

La Caverne future peut demander une validation Parent avant de réaliser une récompense du monde réel ; le débit actuel a lieu à l'acceptation. Les Cristaux servent aux coffres. Les prix, raretés et tables de loot restent provisoires. Les Défis familiaux seront un système indépendant et n'utiliseront pas de portefeuille partagé.

## Droits et validation

Le Parent est un membre doté de droits supplémentaires de création, édition, désactivation, validation, gestion familiale et Suivi. L'Enfant a des droits limités, mais peut accomplir une action accessible selon l'attribution. Le backend filtre les actions personnelles et événements d'autres membres avant de répondre à un CHILD ; les actions Maison demeurent visibles collectivement. `PENDING_VALIDATION` s'affiche « En attente de validation », `COMPLETED` « Terminée », `VALIDATED` « Validée ». Le backend reste l'autorité des transitions.

## Pas encore implémenté

- économie et conversions Chronodria ;
- estimation automatique de l'effort et équilibrage final des points ;
- approfondissement de la collection Chronodria, créatures, grimoire enrichi et défis familiaux ;
- séries/streaks de Routines ;
- modèles persistants et tâches fréquentes ;
- création personnelle sans famille active ;
- identité de joueur Chronodria autonome pour chaque Parent.
