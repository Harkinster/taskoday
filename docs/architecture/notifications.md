# Notifications Taskoday : première implémentation locale beta08

## Finalisation du 29 octobre 2026

Recette réelle sur Android 11 / API 30 et Android 13 / API 33 : suites instrumentées complètes, permission native acceptée/refusée, refus persistant, révocation système, et parcours de tâches conservé sans permission. La demande reste liée à l'activation explicite dans Mes notifications, jamais à chaque ouverture. Le canal Organisation familiale désactivé est maintenant détecté : notify() peut réussir sans affichage, donc le publisher retourne false pour laisser l'alerte en attente. Test de non-régression sur le service NotificationManager réel.

L'ancien TaskFlowInstrumentedTest attendait la navigation locale NavTasks, déjà absente de beta07. Il couvre maintenant les mêmes opérations sur le parcours familial authentifié : création personnelle, liste, détail, modification du titre et de la note, accomplissement, retrait confirmé. Les chargements asynchrones sont attendus explicitement. Il reste protégé par le package QA et le marqueur du backend isolé.

**Inter-appareils réellement opérationnel : détection par interrogation de l'API, sans push.** Un Enfant 1 sur l'AVD API 33 a terminé une mission par l'interface ; Papa, connecté avec une autre session sur l'AVD API 30, a reçu la demande de validation alors que son application restait en arrière-plan. Un travail ponctuel attendait le réseau : notification visible 2,28 secondes après reconnexion dans cet essai. Cette mesure locale ne garantit pas une livraison immédiate sur des téléphones réels.

En usage normal, WorkManager interroge l'API périodiquement, avec un intervalle minimal de 15 minutes. Cet intervalle n'est pas un délai maximal : réseau, Doze, économie d'énergie, restrictions du fabricant ou de l'utilisateur peuvent retarder l'exécution. Une session valide et l'activation des notifications sont nécessaires ; l'interface n'a pas besoin de rester ouverte. Après arrêt forcé, une réouverture est nécessaire. Une déconnexion annule travaux et alertes de cet appareil.

La déduplication et les préférences sont locales à chaque installation et compte. Deux appareils connectés au même compte peuvent chacun recevoir leur alerte. Pas de synchronisation de lecture/effacement entre appareils. Les alertes déjà affichées peuvent rester jusqu'à consultation/effacement ou annulation de session ; la file silencieuse écarte les états devenus inutiles.

Pour une livraison plus réactive, une future outbox transactionnelle et une boîte de réception par destinataire permettraient de conserver les événements, avec un signal push opaque et une réconciliation API. Ce chantier nécessitera une décision distincte et des migrations additives testées. Aucun Firebase, service externe, nouveau serveur, paiement ni déploiement ajouté dans cette passe. Android 14 et supérieur n'ont pas été exécutés ; la branche de permission API 33+ a été auditée et testée sur API 33.

## État au 28 octobre 2026

Le socle ci-dessous est implémenté en développement, non publié et désactivé par défaut. Aucun changement du backend de production, aucune migration, aucun Firebase ni service externe. Les notifications restent gratuites. Les sections de proposition historique plus bas décrivent la livraison distante future, pas une fonction déjà active.

- WorkManager effectue une vérification avec connexion, au minimum toutes les 15 minutes, et propose une vérification manuelle. Android peut retarder l'exécution ; aucune alarme exacte n'est promise.
- Périmètre : compte connecté et famille active. Les autres familles ne sont pas interrogées tant qu'elles ne sont pas sélectionnées. Le serveur revalide les permissions ; Android vérifie l'identité et l'appartenance avant et après la collecte.
- Échéances personnelles : tâche ouverte, échéance dans les 15 prochaines minutes ou depuis moins d'une heure. Sans heure, rappel à l'heure du résumé (08:00 par défaut). Retards importants : priorité HIGH/URGENT, tâches personnelles pour l'enfant, vue familiale autorisée pour le parent. Une tâche sans heure ne devient en retard qu'à la fin de sa journée locale.
- Validation : une mission accomplie par un enfant en PENDING_VALIDATION alerte les parents. Les événements VALIDATE et REOPEN informent uniquement la personne qui l'a accomplie, y compris une mission Maison. REOPEN signifie « à reprendre » ; aucun nouveau refus métier n'est inventé.
- Résumé quotidien Parent facultatif conservé ; aucune alerte de récompense n'est ajoutée et aucune récompense n'est calculée par ce système.
- Préférences locales par identifiant de compte : activation, catégories, plage silencieuse, heure du résumé. Elles ne sont pas synchronisées entre appareils. Les anciennes préférences globales sont conservées mais non attribuées automatiquement à un compte.
- Registre durable par compte/famille : clés d'événements et horodatages, sans contenu métier. Déduplication conservée 14 jours (2000 clés), attente silencieuse ou permission absente pendant au plus 24 heures (200 entrées). Les éléments devenus inutiles sont écartés. Une notification silencieuse et neutre par catégorie remplace les empilements.
- Changement de compte, déconnexion ou changement de famille : nouvelle génération de session, annulation des travaux et alertes détenus par ce système. Une réponse réseau ancienne ne peut publier dans la nouvelle session. Les nouvelles préférences et le registre sont exclus des sauvegardes Android.
- Lecture API bornée : occurrences des sept derniers jours à demain, plus retards et historique des événements sur 24 heures au maximum (20 pages de 100). Une réponse trop volumineuse échoue sans avancer le curseur. Les décisions sur une ancienne occurrence hors de cette fenêtre restent une limite de cette première version.
- Une panne réseau préserve le curseur et provoque une relance différée. Les heures silencieuses suivent le fuseau actuel de l'appareil, les événements serveur sont en UTC. WorkManager persiste ses travaux après redémarrage ; un arrêt forcé Android requiert une réouverture de l'application.
- Le texte système ne contient aucun titre de tâche, nom de membre ou nom de famille, et utilise VISIBILITY_PRIVATE avec une version publique générique. Le clic ouvre Taskoday sous la session courante ; il n'embarque pas d'identifiant métier donnant accès à une autre famille.

Sources : [travail périodique Android, minimum 15 minutes](https://developer.android.com/reference/androidx/work/PeriodicWorkRequest), [permission Android 13](https://developer.android.com/develop/ui/views/notifications/notification-permission). Les essais réellement exécutés et les limites de l'AVD sont dans le rapport QA beta08.

## Proposition historique et suite distante

Inspection du 27 octobre 2026. Aucun nouvel envoi, service cloud ou SDK n'est activé. Les notifications d'organisation resteront gratuites.

## État initial avant cette passe (historique)

Le dossier features/notifications contient préférences, écran, contrôleur, scheduler WorkManager, worker et runner d'un résumé quotidien Parent. Travail unique différé avec contrainte réseau ; replanification quotidienne, horaire calculé avec ZonedDateTime. Le runner vérifie activation, session, rôle Parent et présence d'une famille. Le notifier contrôle POST_NOTIFICATIONS à partir d'Android 13, utilise un canal dédié et un PendingIntent immuable. Activation désactivée par défaut.

Limites à qualifier avant extension : préférences et travail communs à l'installation, non indexés par compte ; pas de boîte de réception serveur ni de registre durable des envois par événement ; pas de transport distant. Tester les changements de compte/famille pendant une exécution. Il s'agit de points d'architecture, pas d'une fuite reproduite dans cette passe.

## Évolution distante proposée

1. FastAPI produit un événement durable dans la transaction de la transition métier. Une future outbox nécessitera une migration additive testée, pas réalisée maintenant.
2. Le serveur détermine les destinataires selon les rôles et appartenances encore actifs au moment de la livraison. Un événement n'accorde aucun accès à une tâche.
3. Une boîte de réception consultable après connexion déduplique par `(recipient_user_id, event_id, notification_kind)` ; distinguer réception, affichage et lecture.
4. Android revalide compte, famille, autorisation et pertinence avant affichage puis à l'ouverture. Annuler travaux et notifications de l'ancien compte après déconnexion/changement de compte.

| Événement | Déclencheur | Destinataires et regroupement |
| --- | --- | --- |
| Personnel aujourd'hui | Horaire choisi et état courant | Utilisateur concerné, résumé quotidien |
| À venir / retard | Échéance encore pertinente | Propriétaire ; Parent si autorisé, pas un rappel par tâche par défaut |
| Mission accomplie | Transition métier enregistrée | Parents concernés si une action est utile, regrouper |
| Validation en attente | Passage réel à PENDING_VALIDATION | Parents habilités, une entrée par occurrence |
| Mission validée | Transition persistée | Utilisateur concerné, retours regroupés |
| Récompense disponible | Progression confirmée | Propriétaire, catégorie facultative non commerciale |

La notification n'attribue jamais de récompense ; seul le moteur métier et ses clés d'idempotence le font.

## Préférences et confidentialité

Préférences par utilisateur/catégorie, désactivation complète, horaire de résumé et fenêtre silencieuse. Affichage neutre par défaut sur écran verrouillé, sans nom d'enfant, titre de tâche ou famille. Aucun message commercial dans le canal utilitaire. Révoquer les destinataires quittant une famille et ne jamais remettre une file de l'ancien compte au suivant. Pas d'emails, tokens ou contenus familiaux dans les journaux.

## Doublons, hors ligne et temps

Événements serveur en UTC ; préférences partagées avec un fuseau IANA explicite. Tester heure d'été/hiver, changement de fuseau, redémarrage, refus de permission et panne réseau. À la reconnexion, recalculer la pertinence : une tâche déjà validée ou une récompense consommée ne doit pas générer un vieux rappel.

WorkManager convient à la synchronisation et aux résumés différés, sans garantie d'alarme exacte. Définir expiration, regroupement, backoff borné et déduplication durable sur serveur et appareil. Une relance ne doit ni doubler l'affichage ni créer une seconde récompense.

## Transport et découpage

Commencer par consolider le socle local et la portée utilisateur. Pour des événements entre appareils lorsque l'application est fermée, évaluer FCM ultérieurement avec validation du propriétaire : tokens par compte/appareil, rotation, révocation à déconnexion, backend d'envoi et conservation.

Si FCM est retenu, transmettre un signal opaque invitant à synchroniser, pas de détails familiaux. Ne pas dépendre de l'ordre des messages ni du transport comme stockage métier ; TTL et collapse key ne remplacent pas l'idempotence. Aucun Firebase ajouté ici.

Passes proposées : tests de portée et annulation ; contrat événement et réception synthétique ; livraison différée/doublons/fuseaux ; puis choix et validation du transport distant avant activation.

Références primaires : [permission Android](https://developer.android.com/develop/ui/views/notifications/notification-permission), [WorkManager](https://developer.android.com/develop/background-work/background-tasks/persistent-work), [TTL FCM](https://firebase.google.com/docs/cloud-messaging/customize-messages/setting-message-lifespan), [regroupement FCM](https://firebase.google.com/docs/cloud-messaging/customize-messages/collapsible-message-types).
