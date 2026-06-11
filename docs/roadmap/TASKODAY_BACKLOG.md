# Backlog Taskoday

Ce backlog organise les travaux à qualifier avant d'ajouter de nouvelles fonctionnalités.

## Priorités

| Priorité | Signification |
|---|---|
| P0 | Bloquant : empêche la connexion, l'utilisation principale ou risque de perdre/corrompre des données. |
| P1 | Important : nécessaire pour une V1 cohérente et testable. |
| P2 | Confort : améliore nettement l'expérience sans bloquer le parcours principal. |
| P3 | Plus tard : idée à conserver après stabilisation de la V1. |

## Phases

| Phase | Objectif | Critère de sortie |
|---|---|---|
| Phase 1 | Stabilisation connexion / session / enfant | Connexion réelle, persistance, déconnexion, changement de compte et sélection enfant fiables. |
| Phase 2 | Nettoyage UI | Aucun texte coupé, chevauchement, écran vide ou action ambiguë sur les parcours principaux. |
| Phase 3 | Le Nid complet | Nid, profil, monnaies, compagnon et accès rapides entièrement branchés et stables. |
| Phase 4 | Récompenses Routine / Mission / Quête | Progression et gains cohérents après chaque action. |
| Phase 5 | Coffres / loot / inventaire | Achat, ouverture, loot, compensation et inventaire validés de bout en bout. |
| Phase 6 | Œufs / dragons / évolution | Cycle œuf vers dragon, compagnon actif et évolutions validés. |
| Phase 7 | Polish visuel / animations / notifications | Finitions visuelles, retours d'action et notifications prêtes pour diffusion. |

## Bugs bloquants

| Priorité | Phase | Sujet | Critère d'acceptation | Statut |
|---|---|---|---|---|
| P0 | Phase 1 | Qualifier tout retour automatique vers un ancien compte après déconnexion. | Aucun token, enfant actif ou écran authentifié n'est restauré après déconnexion et relance. | [ ] |
| P0 | Phase 1 | Gérer proprement un compte parent sans enfant associé. | L'app explique la situation et propose le parcours approprié sans crash ni données factices distantes. | [ ] |
| P0 | Phase 1 | Éliminer les boucles de navigation après `401`. | Une session expirée ramène une seule fois à Connexion et permet une nouvelle connexion. | [ ] |
| P0 | Phase 1 | Vérifier l'absence de double écriture lors d'un timeout réseau. | Une routine, mission, quête, demande ou ouverture de coffre n'est jamais appliquée deux fois. | [ ] |
| P0 | Phase 5 | Vérifier que `quantity` et `quantity_total` ne sont jamais confondus. | L'interface affiche uniquement `quantity` comme gain du coffre. | [ ] |

## UX indispensable

| Priorité | Phase | Sujet | Critère d'acceptation | Statut |
|---|---|---|---|---|
| P1 | Phase 1 | Livrer et valider Déconnexion / Changer de compte. | L'action est accessible depuis Profil, confirmée, testée avec deux comptes réels et couverte par tests. | [ ] |
| P1 | Phase 1 | Ajouter une sélection explicite de l'enfant actif pour les parents. | Le parent sait quel enfant est affiché et peut changer sans mélange de données. | [ ] |
| P1 | Phase 2 | Harmoniser chargement, état vide, erreur et bouton Réessayer. | Chaque écran distant possède ces quatre états sans chargement infini. | [ ] |
| P1 | Phase 2 | Vérifier tous les contenus derrière la bottom navigation et le FAB. | Aucun contenu ou bouton n'est masqué sur les tailles d'écran ciblées. | [ ] |
| P2 | Phase 2 | Rendre les messages d'erreur actionnables. | Les erreurs indiquent clairement reconnexion, réessai ou action impossible. | [ ] |
| P2 | Phase 2 | Ajouter une indication discrète du mode local. | L'utilisateur distingue toujours données locales et données backend. | [ ] |

## Gameplay V1

| Priorité | Phase | Sujet | Critère d'acceptation | Statut |
|---|---|---|---|---|
| P1 | Phase 4 | Stabiliser la boucle Routine → progression → récompense. | Une routine terminée met à jour une fois la progression et les monnaies attendues. | [ ] |
| P1 | Phase 4 | Stabiliser la boucle Mission → progression → récompense. | Une mission terminée met à jour une fois les objectifs et récompenses attendus. | [ ] |
| P1 | Phase 4 | Stabiliser la boucle Quête → progression → récompense. | Une quête terminée met à jour une fois les objectifs et récompenses attendus. | [ ] |
| P2 | Phase 4 | Clarifier les objectifs et conditions de complétion. | L'utilisateur comprend ce qu'il doit faire et ce qu'il gagne avant d'agir. | [ ] |
| P2 | Phase 4 | Ajouter un retour visuel compact après récompense. | Le gain est visible sans bloquer la navigation ni créer une nouvelle mécanique. | [ ] |

## Parent / enfant

| Priorité | Phase | Sujet | Critère d'acceptation | Statut |
|---|---|---|---|---|
| P0 | Phase 1 | Valider l'association parent / enfant sur le backend réel. | Le parent associe un enfant, le voit dans sa liste et accède uniquement à ses données. | [ ] |
| P1 | Phase 1 | Afficher clairement le rôle connecté. | Profil indique Parent ou Enfant et les actions non autorisées sont masquées ou expliquées. | [ ] |
| P1 | Phase 1 | Isoler toutes les données lors d'un changement d'enfant. | Aucun écran ne conserve les données de l'enfant précédent. | [ ] |
| P1 | Phase 4 | Valider les permissions de création et de complétion. | Parent et enfant ne voient que les actions prévues par les règles métier. | [ ] |
| P2 | Phase 2 | Améliorer l'expérience d'association par code. | Le code, son expiration et le résultat d'association sont compréhensibles. | [ ] |

## Récompenses

| Priorité | Phase | Sujet | Critère d'acceptation | Statut |
|---|---|---|---|---|
| P1 | Phase 4 | Vérifier la cohérence XP, Flammèches et Cristaux. | Chaque source de gain met à jour les bons soldes sans duplication. | [ ] |
| P1 | Phase 4 | Valider le parcours Souhait enfant → décision parent. | La demande, l'acceptation, le refus et les soldes sont cohérents. | [ ] |
| P1 | Phase 4 | Rafraîchir tous les écrans après une récompense. | Profil, Nid, Caverne et écrans concernés affichent les nouvelles valeurs. | [ ] |
| P2 | Phase 4 | Uniformiser l'affichage des récompenses. | Les mêmes icônes, noms et formats sont utilisés partout. | [ ] |
| P3 | Phase 7 | Ajouter des animations de gain légères. | Les animations renforcent le retour d'action sans ralentir l'app. | [ ] |

## Le Nid

| Priorité | Phase | Sujet | Critère d'acceptation | Statut |
|---|---|---|---|---|
| P1 | Phase 3 | Valider Le Nid avec plusieurs états backend réels. | Le Nid fonctionne avec compagnon actif, sans compagnon et avec ressources faibles. | [ ] |
| P1 | Phase 3 | Garantir la cohérence des monnaies et de la progression. | Les valeurs correspondent aux endpoints backend après chaque action. | [ ] |
| P1 | Phase 3 | Valider tous les accès rapides. | Chaque tuile ouvre le bon sous-écran et le retour au Nid est fiable. | [ ] |
| P2 | Phase 3 | Réduire la densité verticale sur petits écrans. | Les informations principales restent visibles et compactes sans texte coupé. | [ ] |
| P2 | Phase 3 | Clarifier l'état du compagnon actif. | Nom, stade, progression et prochaine évolution sont compris immédiatement. | [ ] |

## Caverne / coffres

| Priorité | Phase | Sujet | Critère d'acceptation | Statut |
|---|---|---|---|---|
| P1 | Phase 5 | Valider le catalogue de coffres réel. | Commun, rare et épique affichent coût, rareté et récompenses possibles corrects. | [ ] |
| P1 | Phase 5 | Valider l'ouverture de coffre de bout en bout. | Cristaux dépensés, solde, loot, compensation et œuf éventuel sont corrects. | [ ] |
| P1 | Phase 5 | Gérer le solde insuffisant sans ambiguïté. | L'ouverture est refusée avec un message clair et aucune modification locale trompeuse. | [ ] |
| P2 | Phase 5 | Clarifier la séparation Souhaits / Coffres dans la Caverne. | La monnaie utilisée et la section active sont toujours évidentes. | [ ] |
| P3 | Phase 7 | Ajouter une animation d'ouverture de coffre. | L'animation reste courte, interruptible et ne masque pas le résultat. | [ ] |

## Œufs / dragons

| Priorité | Phase | Sujet | Critère d'acceptation | Statut |
|---|---|---|---|---|
| P1 | Phase 6 | Valider les cinq états d'œuf. | Sleeping, warm, glowing, cracked et hatching s'affichent et progressent correctement. | [ ] |
| P1 | Phase 6 | Valider l'éclosion vers baby. | L'œuf prêt donne le bon dragon baby et rafraîchit Nid, Inventaire et Bestiaire. | [ ] |
| P1 | Phase 6 | Valider les cinq stades dragon. | Baby, young, medium, large et legendary sont cohérents avec les assets et ressources. | [ ] |
| P1 | Phase 6 | Valider le compagnon actif persistant. | Le compagnon choisi reste actif après navigation, relance et reconnexion. | [ ] |
| P2 | Phase 6 | Clarifier les ressources requises pour évoluer. | L'utilisateur comprend ce qui manque avant de lancer l'action. | [ ] |
| P3 | Phase 7 | Ajouter des animations d'évolution. | Les animations respectent la DA et ne bloquent pas l'accès aux données. | [ ] |

## Visuel / DA

| Priorité | Phase | Sujet | Critère d'acceptation | Statut |
|---|---|---|---|---|
| P1 | Phase 2 | Corriger tous les textes coupés et chevauchements. | Les captures ciblées ne montrent aucun texte tronqué critique ni recouvrement. | [ ] |
| P1 | Phase 2 | Vérifier les fonds et contrastes sur téléphone réel. | Les textes restent lisibles sur chaque panneau fantasy. | [ ] |
| P2 | Phase 2 | Harmoniser les dialogues et états d'erreur avec la DA fantasy. | Aucun dialogue principal ne ressemble à un composant Material brut. | [ ] |
| P2 | Phase 7 | Finaliser ombres, glow et relief. | Les effets restent cohérents et ne nuisent ni aux performances ni à la lisibilité. | [ ] |
| P2 | Phase 7 | Vérifier assets et cadrages sur plusieurs ratios. | Aucun logo, dragon, œuf ou cadre n'est étiré ou mal cadré. | [ ] |

## Technique

| Priorité | Phase | Sujet | Critère d'acceptation | Statut |
|---|---|---|---|---|
| P0 | Phase 1 | Ajouter un scénario automatisé connexion → déconnexion → reconnexion. | Le scénario détecte toute régression de session et de navigation. | [ ] |
| P1 | Phase 1 | Tester les mappings d'erreurs `401`, `403`, `404`, `422` et `500`. | Chaque statut produit un message utilisateur et un état stable. | [ ] |
| P1 | Phase 1 | Centraliser l'invalidation des données lors d'un changement de compte/enfant. | Aucun ViewModel ou cache ne conserve de données d'une ancienne session. | [ ] |
| P1 | Phase 5 | Ajouter des tests DTO sur les résultats de coffre. | Les champs gain, total, compensation et inventaire sont protégés contre les régressions. | [ ] |
| P2 | Phase 2 | Étendre les tags de test aux parcours critiques. | Maestro peut cibler connexion, profil, déconnexion, Nid et Caverne sans coordonnées fragiles. | [ ] |
| P2 | Phase 7 | Mettre en place une matrice de captures multi-écrans. | Les régressions visuelles sont comparables sur petits et grands appareils. | [ ] |

## Idées plus tard

| Priorité | Phase | Sujet | Critère d'acceptation | Statut |
|---|---|---|---|---|
| P3 | Phase 7 | Notifications locales pour routines et missions. | Notifications configurables, compréhensibles et non intrusives. | [ ] |
| P3 | Phase 7 | Notifications de récompense ou d'évolution prête. | L'utilisateur peut les désactiver et elles ouvrent le bon écran. | [ ] |
| P3 | Phase 7 | Historique de progression du Nid. | Une vue compacte explique les gains récents sans devenir un écran administratif. | [ ] |
| P3 | Phase 7 | Personnalisation légère du compagnon ou du Nid. | L'idée reste cosmétique et ne modifie pas l'équilibre métier V1. | [ ] |
| P3 | Après V1 | Accessibilité renforcée. | Taille de texte, contraste, descriptions et navigation clavier sont validés. | [ ] |

## Ordre de travail recommandé

1. Exécuter intégralement `docs/qa/MANUAL_TEST_PLAN.md`.
2. Créer un ticket pour chaque anomalie confirmée avec priorité et phase.
3. Corriger tous les P0 avant toute nouvelle fonctionnalité.
4. Terminer les P1 de la phase courante avant de passer à la suivante.
5. Rejouer la recette manuelle et les tests automatisés après chaque lot.

