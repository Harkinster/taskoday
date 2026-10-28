# Notifications Taskoday : préparation technique

Inspection du 27 octobre 2026. Aucun nouvel envoi, service cloud ou SDK n'est activé. Les notifications d'organisation resteront gratuites.

## Existant

Le dossier features/notifications contient préférences, écran, contrôleur, scheduler WorkManager, worker et runner d'un résumé quotidien Parent. Travail unique différé avec contrainte réseau ; replanification quotidienne, horaire calculé avec ZonedDateTime. Le runner vérifie activation, session, rôle Parent et présence d'une famille. Le notifier contrôle POST_NOTIFICATIONS à partir d'Android 13, utilise un canal dédié et un PendingIntent immuable. Activation désactivée par défaut.

Limites à qualifier avant extension : préférences et travail communs à l'installation, non indexés par compte ; pas de boîte de réception serveur ni de registre durable des envois par événement ; pas de transport distant. Tester les changements de compte/famille pendant une exécution. Il s'agit de points d'architecture, pas d'une fuite reproduite dans cette passe.

## Architecture proposée

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
