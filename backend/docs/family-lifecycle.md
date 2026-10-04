# Cycle de vie d'une famille

## Modèle réel et risque de suppression

Le backend servi par `app.main` utilise `app.models` et `app.routers`. Les anciens modules `app.api.routes` et certains anciens modèles ne sont pas inclus dans cette application. L'audit ci-dessous porte sur les tables chargées par `app.db.base` et les migrations jusqu'à `20260920_0009`.

| Table | Lien, nullabilité et comportement actuel | Risque d'un hard delete de `families` |
| --- | --- | --- |
| `families` | `created_by_user_id -> users.id`, non nul, `RESTRICT`; `created_at`, aucun ancien statut/archive/updated_at | Identité et chronologie du foyer perdues. |
| `family_members` | `family_id -> families.id`, non nul, `CASCADE`; `user_id -> users.id`, non nul, `CASCADE`; relation ORM `Family.members` en `all, delete-orphan` | Tous les liens Parent/Child disparaissent. Les comptes restent, mais leur contexte familial est perdu. |
| `family_invites` | `family_id -> families.id`, non nul, `CASCADE`; créateur non nul `RESTRICT`, accepteur nullable `SET NULL` vers User | Traces d'invitation et d'acceptation perdues. |
| `family_tasks` | `family_id -> families.id`, non nul, `CASCADE`; créateur non nul `RESTRICT` vers User | Tâches, descriptions, échéances et récurrences perdues. |
| `family_task_assignees` | `task_id -> family_tasks.id`, non nul, `CASCADE`; `user_id` non nul `CASCADE` | Assignations historiques perdues indirectement. |
| `family_task_occurrences` | `task_id -> family_tasks.id`, non nul, `CASCADE`; compléteur/validateur nullable, `RESTRICT` vers User | États, dates, complétions et validations perdus indirectement. |
| `users`, `child_profiles` | Aucun FK vers Family. `child_profiles.user_id` non nul, unique, `CASCADE` depuis User | Ne seraient pas effacés par une suppression de Family, mais leurs liens familiaux disparaîtraient. |
| `pairing_codes`, `refresh_tokens` | Liés à User, pas à Family (`CASCADE` depuis User; remplacement de refresh token nullable `SET NULL`) | Ne seraient pas supprimés. Un code de pairing doit toutefois refuser une famille archivée. |
| `routines`, `missions`, `quests`, `task_completions`, `xp_history` | Liés à l'utilisateur enfant et parfois à un créateur/compléteur User en `RESTRICT`; pas de FK Family | Historique global conservé, mais attribution historique à un foyer impossible si ses memberships disparaissent. |
| `child_wallet`, `guardian_progress`, `child_eggs`, `child_dragons`, `item_inventory`, `chest_inventory` | Liés à User enfant en `CASCADE`; définitions d'œufs/dragons séparées | État de jeu conservé mais plus de contexte familial si lien détruit. |
| `external_rewards`, `scale_transactions`, `reward_requests`, `reward_coupons` | Liés à User enfant (`CASCADE`), et éventuellement récompense (`SET NULL`) et acteurs User (`RESTRICT`); pas de FK Family | Historique des récompenses conservé sans contexte familial explicite. |

La relation ORM `FamilyTask.assignees` et `FamilyTask.occurrences` porte aussi `all, delete-orphan`. Les migrations Alembic `20260821_0004` à `20260823_0006` introduisent les tâches et invitations ; `20260920_0008` et `_0009` sont les dernières avant cette passe. Aucun FK `family_id` direct supplémentaire n'a été trouvé dans les modèles servis. Il n'existe pas de journal d'audit familial dédié.

Les notifications Android sont calculées à partir des tâches et préférences locales ; aucune table de notifications liée à Family n'est chargée par le backend. L'archive masque les tâches dans les endpoints normaux, ce qui empêche de nouvelles projections lors d'un rafraîchissement, mais les notifications déjà affichées par Android relèvent du système local.

## Comportement avant archivage

- `leave` et retrait suppriment un seul membership, ses assignations dans cette famille et invalident les invitations encore ouvertes du Parent. Le dernier Parent ne peut pas partir ; User, ChildProfile, tâches et autres familles restent.
- `/auth/me` et `/families/me` listaient tous les memberships. Aucun statut archive n'existait.
- Android garde `activeFamilyId` localement, valide sa présence dans `/auth/me`, puis utilise `/families/me` et la liste des membres. Après `leave`, le dépôt choisit la première famille encore accessible, ou efface la sélection. Plusieurs familles sont prises en charge ; sans famille, la création/rejointure restent visibles.

## Stratégies examinées

| Stratégie | Bénéfice | Risque et complexité |
| --- | --- | --- |
| Hard delete | Aucun enregistrement résiduel, stockage réduit | Cascades directes/indirectes effacent tâches, occurrences, validations et invitations. Restaurer nécessite des sauvegardes et reconstruire les relations. Forte complexité et risque historique ; comptes enfants globaux survivraient sans leur lien. |
| Archive | Historique et memberships conservés ; simple marqueur nullable et filtres actifs ; restauration future possible | Nécessite de garder tous les chemins d'accès et de mutation cohérents, y compris pairing/invitations et Android. Pas d'accès historique en lecture normale tant qu'aucune interface dédiée n'existe. |
| Hybride | Peut purger plus tard une famille réellement vide et sans historique | Il faut prouver l'absence de toutes les dépendances et gérer les courses ; aucun bénéfice immédiat. Peut être étudié après outillage d'audit et politique de rétention. |

**Choix : archive seule.** `families.archived_at` est nullable ; `NULL` signifie active. La migration n'efface ni ne réécrit les données existantes. Un Parent membre peut archiver seulement s'il est **l'unique membre**. Les autres Parents et enfants doivent avoir quitté ou été retirés explicitement. Un Child et un Parent non membre sont refusés. Le membership du Parent restant est conservé pour l'historique, mais masqué des listes actives. Les invitations ouvertes expirent ; leurs enregistrements restent. Aucune restauration publique n'est ajoutée : il faudrait concevoir les droits après archivage et la réactivation d'anciennes invitations. Aucun hard delete n'est ajouté.

Le nom unique d'une famille archivée reste réservé : une nouvelle famille du même nom reçoit 409. Le downgrade technique enlève `archived_at` ; s'il était exécuté après des archivages réels, ceux-ci redeviendraient visibles. Il ne doit donc être utilisé qu'avant usage de l'archivage, ou après une décision explicite sur ces données.

Une famille **sans aucun membre**, issue d'un ancien état incohérent ou d'une intervention hors API, n'a personne autorisé à demander l'archivage. Les routes normales ne créent pas cet état grâce au blocage du dernier Parent. Ces éventuels orphelins restent en base pour un audit administratif ultérieur ; aucune purge automatique risquant l'historique n'est introduite.

Les endpoints normaux filtrent ou refusent la famille archivée : `/auth/me`, `/families/me`, membres/enfants, tâches, invitations Parent, création d'enfant et pairing explicite ou implicite. Les opérations de membership sur la famille archivée sont refusées. La réponse d'archivage est 409 quand d'autres membres sont présents, 403 pour Child membre et 404 pour non membre/famille déjà archivée. Android recalcule `activeFamilyId` après succès et efface les membres affichés avant le rafraîchissement.

Le backend et la migration sont **locaux seulement** ; aucun déploiement ni migration de production dans cette passe. La famille de QA production `Famille QA Lifecycle` (ID 10) reste active pour tests futurs après déploiement autorisé. Les familles 7, 8 et 9 restent hors de cette fonctionnalité de QA.
