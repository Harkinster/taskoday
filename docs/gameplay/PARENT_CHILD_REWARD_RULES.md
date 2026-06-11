# Regles Parent, Enfant, Actions et Recompenses

## Objectif

Ce document definit les regles produit a valider avant de coder les recompenses.
Il sert de reference commune pour Android, le backend, les tests et l'UX.

Principes directeurs :

- Le parent configure les objectifs et garde le controle des recompenses.
- L'enfant peut agir, proposer et progresser sans pouvoir fabriquer des ressources.
- Toute attribution de monnaie ou de progression est calculee et enregistree par le backend.
- Une action ne doit jamais attribuer deux fois la meme recompense.
- Une recompense reelle importante exige une decision explicite du parent.

## Roles et permissions

### Parent

Le parent peut creer pour un enfant :

- une Routine ;
- une Mission ;
- une Quete ;
- une Recompense reelle ;
- un Souhait, affiche ensuite dans la Caverne ;
- un Parchemin par validation d'une demande de Souhait.

Pour une Routine, une Mission ou une Quete, le parent peut definir :

- le montant de Flammeches ;
- le montant de Cristaux ;
- l'XP du Gardien, si ce systeme est conserve ;
- le mode de validation ;
- la repetition ;
- la priorite ;
- l'enfant concerne ;
- les dates, echeances ou conditions de completion.

Le parent peut egalement :

- accepter, refuser ou modifier une proposition de l'enfant ;
- valider manuellement une action terminee ;
- corriger une attribution erronee avec une trace d'historique ;
- accepter, refuser, expirer ou marquer comme utilisee une demande de Souhait ;
- choisir quel enfant actif il consulte.

Le parent ne doit pas pouvoir modifier les ressources sans trace. Toute correction
doit produire un historique indiquant la cause et l'auteur.

### Enfant

L'enfant peut :

- voir ses Routines ;
- voir ses Missions ;
- voir ses Quetes ;
- proposer une action personnelle ;
- cocher une action lorsque le parent l'a autorise ;
- consulter son Nid ;
- consulter son Inventaire ;
- consulter son Bestiaire ;
- demander un Souhait actif ;
- consulter ses soldes et son historique de progression.

L'enfant ne peut pas :

- s'attribuer automatiquement des Flammeches, Cristaux ou XP ;
- creer une action avec une recompense automatique ;
- modifier le montant d'une recompense ;
- valider seul une grosse recompense reelle ;
- ouvrir un Coffre sans payer le cout serveur ;
- modifier localement un solde, un loot, un Oeuf ou un Dragon ;
- reutiliser une validation ou une reponse reseau pour obtenir deux gains.

## Matrice de permissions

| Action | Parent | Enfant |
|---|---|---|
| Creer une Routine, Mission ou Quete recompensee | Oui | Non |
| Proposer une action personnelle sans recompense | Oui | Oui |
| Definir Flammeches, Cristaux ou XP | Oui | Non |
| Cocher une action | Selon configuration | Si autorise |
| Valider une completion | Oui | Seulement si validation enfant autorisee |
| Accepter ou transformer une proposition enfant | Oui | Non |
| Creer un Souhait / une Recompense reelle | Oui | Non |
| Demander un Souhait actif | Non necessaire | Oui |
| Accepter ou refuser un Souhait | Oui | Non |
| Ouvrir un Coffre avec les Cristaux disponibles | Oui si parcours autorise | Oui si parcours autorise |
| Modifier directement les ressources | Correction tracee uniquement | Non |

## Action proposee par l'enfant

Une action creee par l'enfant est une **proposition** ou une **action personnelle**.
Elle ne devient pas automatiquement une action recompensee.

Regles obligatoires :

- Sa recompense initiale est toujours nulle.
- Elle n'attribue aucune Flammeche, aucun Cristal et aucune XP.
- Son origine enfant est visible pour le parent.
- Le parent peut l'accepter telle quelle, la modifier, la transformer en Routine,
  Mission ou Quete, ou la refuser.
- Une transformation en action recompensee exige une decision explicite du parent.
- Les gains ne commencent qu'apres cette validation parentale.
- Une completion anterieure a la validation parentale ne doit pas declencher
  retroactivement une recompense sans confirmation explicite.

Statuts proposes :

| Statut | Signification |
|---|---|
| `personal` | Action personnelle visible par l'enfant, sans recompense. |
| `proposed` | Proposition envoyee au parent, sans recompense. |
| `approved` | Proposition acceptee ou transformee par le parent. |
| `refused` | Proposition refusee, conservee dans l'historique si necessaire. |
| `archived` | Action terminee ou retiree de l'affichage courant. |

Les noms techniques definitifs doivent etre valides avant toute migration ou
modification d'endpoint.

## Types d'actions et ressources

### Routine

Une Routine est une habitude reguliere et repetee.

Exemples : se brosser les dents, preparer son cartable, lire chaque soir.

- Elle possede une frequence ou des jours de repetition.
- Elle favorise une progression douce et reguliere.
- Son mode de validation peut etre autorise a l'enfant ou exige par le parent.

### Mission

Une Mission est une action ponctuelle avec un resultat clair.

Exemples : ranger sa chambre, terminer un devoir, aider a preparer le repas.

- Elle possede une echeance facultative.
- Elle peut demander une validation parentale.
- Sa recompense peut etre superieure a celle d'une Routine.

### Quete

Une Quete est un objectif important, exceptionnel ou compose de plusieurs efforts.

Exemples : terminer un projet, progresser sur une competence, relever un defi.

- Elle doit afficher clairement son objectif et sa progression.
- Sa validation parentale est recommandee par defaut.
- Sa recompense peut etre plus forte, mais reste soumise aux limites produit.

### Souhait et Parchemin

Un Souhait est une recompense reelle creee par un parent et demandable par l'enfant.

- Les Flammeches servent uniquement aux Souhaits.
- Une demande en attente ne depense aucune Flammeche.
- Le parent accepte ou refuse la demande.
- L'acceptation reverifie le solde, depense les Flammeches et cree un Parchemin.
- Le Parchemin represente un Souhait valide mais pas encore necessairement utilise.
- L'utilisation d'une grosse recompense reelle exige une action parentale explicite.

### Coffre

Un Coffre est une recompense virtuelle obtenue avec des Cristaux.

- Les Cristaux servent uniquement aux Coffres et au loot associe.
- Le cout, le tirage et le resultat sont determines par le backend.
- L'application Android affiche le resultat mais ne calcule pas le loot.
- Une ouverture ne doit jamais etre appliquee deux fois.

### Flammeches

Les Flammeches sont la monnaie des Souhaits et Parchemins.

- Elles peuvent etre gagnees uniquement par des actions validees.
- Elles ne servent pas aux Coffres.
- Elles sont depensees lors de l'acceptation d'un Souhait, apres verification serveur.

### Cristaux

Les Cristaux sont la monnaie des Coffres et du loot virtuel.

- Ils peuvent etre attribues par des actions validees selon la configuration parentale.
- Ils ne servent pas aux Souhaits.
- Leur depense et le resultat d'un Coffre sont controles par le backend.

### XP du Gardien

L'XP represente la progression du Gardien si ce systeme est conserve.

- Elle n'est jamais depensee.
- Elle peut faire progresser le niveau, le Nid ou l'historique du Profil.
- Son attribution doit suivre les memes regles de validation et d'idempotence que les monnaies.

## Validation et attribution

La validation d'une action et l'attribution de sa recompense sont liees, mais ne
doivent pas etre confondues dans l'UX.

Modes de validation a confirmer :

| Mode | Comportement |
|---|---|
| Validation enfant autorisee | L'enfant coche l'action ; le backend valide et attribue une seule fois les gains configures. |
| Validation parentale | L'enfant signale l'action terminee ; aucun gain n'est attribue avant validation du parent. |
| Validation parent directe | Le parent marque l'action terminee ; le backend attribue une seule fois les gains. |

Regles techniques indispensables :

- Le backend reste la source de verite des soldes et de la progression.
- Une completion possede un identifiant ou une garantie d'idempotence.
- Toute attribution produit un historique.
- Une erreur reseau ou un nouvel appui ne doit pas dupliquer les gains.
- Les montants affiches avant validation sont des recompenses prevues, pas des soldes acquis.
- Une recompense refusee ou annulee n'ajoute aucune ressource.

## Priorite d'implementation

| Ordre | Phase | Resultat attendu avant de poursuivre |
|---:|---|---|
| 1 | Affichage et session | Compte, role, enfant actif, persistance et deconnexion sont fiables. |
| 2 | Creation enfant et selection enfant | Le parent associe, selectionne et distingue clairement chaque enfant. |
| 3 | Creation et validation des actions | Permissions, propositions enfant, completion et validation parentale sont stables. |
| 4 | Recompenses | Flammeches, Cristaux et XP sont attribues une seule fois avec historique. |
| 5 | Coffres | Cout, ouverture, loot et inventaire sont controles de bout en bout. |
| 6 | Evolution Oeufs et Dragons | Les evolutions consomment les bonnes ressources et restent coherentes apres relance. |

Une phase ne doit pas commencer si la phase precedente peut encore melanger les
comptes, les enfants actifs ou les ressources.

## Points a valider avant codage

1. L'XP du Gardien est-elle conservee dans la V1 ?
2. Un parent peut-il choisir librement les montants ou uniquement parmi des baremes et limites ?
3. Les Cristaux peuvent-ils etre attribues directement par une action, ou seulement via une progression de Coffre ?
4. Quel mode de validation est applique par defaut a chaque type : Routine, Mission et Quete ?
5. Une proposition enfant acceptee peut-elle recompenser une completion deja effectuee, ou seulement les completions futures ?
6. Une action personnelle refusee reste-t-elle visible uniquement pour l'enfant ?
7. Qui peut ouvrir un Coffre : enfant uniquement, parent et enfant, ou option configurable ?
8. Quelles recompenses reelles exigent une seconde confirmation lors de l'utilisation du Parchemin ?
9. Quelles limites quotidiennes ou hebdomadaires evitent une inflation de Flammeches, Cristaux et XP ?
10. Quel historique le parent et l'enfant peuvent-ils consulter, et pendant combien de temps ?

Ces arbitrages doivent etre decides avant de modifier les regles metier, les DTO
ou les endpoints.
