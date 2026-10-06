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

## Reward Engine V1

Le backend est l'autorité et appelle le Reward Engine dans la même transaction que la transition finale. `COMPLETE` produit un grant uniquement quand aucune validation Parent n'est requise ; sinon `VALIDATE` le produit. `START`, `JOIN`, `RESCHEDULE` et `FAIL` ne donnent aucun point. Un `REOPEN` conserve les grants historiques et les marque révoqués pour le cycle rouvert ; le cycle suivant peut recevoir de nouveaux grants. `FAILED` ne donne jamais de récompense.

Les grants forment le registre de référence et sont idempotents par occurrence, cycle et bénéficiaire. Le solde actif est calculé à partir des grants non révoqués. Pour `PERSONAL`, le bénéficiaire est l'unique membre assigné, même si un Parent a effectué la completion. Pour `HOUSE`, seuls les contributeurs réels du cycle finalisé sont bénéficiaires ; les assignataires ne sont pas supposés avoir participé. Chaque contributeur reçoit le montant complet de la policy V1.

Les montants centralisés provisoires sont Routine 10, Mission 20 et Quête 30 + bonus Quête 10, avec un modificateur d'effort neutre de 1.0. Ces points mesurent l'effort reconnu par Taskoday ; ils ne constituent pas l'économie finale de Chronodria. Aucun grant rétroactif n'est créé pour les anciennes completions. Les règles d'équilibrage multi contributeurs restent provisoires.

## Droits et validation

Le Parent est un membre doté de droits supplémentaires de création, édition, désactivation, validation, gestion familiale et Suivi. L'Enfant a des droits limités, mais peut accomplir une action accessible selon l'attribution. Le backend filtre les actions personnelles et événements d'autres membres avant de répondre à un CHILD ; les actions Maison demeurent visibles collectivement. `PENDING_VALIDATION` s'affiche « En attente de validation », `COMPLETED` « Terminée », `VALIDATED` « Validée ». Le backend reste l'autorité des transitions.

## Pas encore implémenté

- économie et conversions Chronodria ;
- estimation automatique de l'effort et équilibrage final des points ;
- inventaire Chronodria final, créatures, grimoire enrichi et défis familiaux ;
- séries/streaks de Routines ;
- modèles persistants et tâches fréquentes ;
- création personnelle sans famille active ;
- identité de joueur Chronodria autonome pour chaque Parent.
