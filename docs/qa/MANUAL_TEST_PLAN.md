# Plan de test manuel Taskoday

Ce document sert de checklist de recette avant toute nouvelle fonctionnalité.

## Informations de session

| Champ | Valeur |
|---|---|
| Date | |
| Testeur | |
| Version / commit | |
| Appareil / émulateur | |
| Version Android | |
| Backend | `https://harkserv.ddns.net/taskoday-api/` |
| Compte parent utilisé | |
| Compte enfant utilisé | |
| `child_id` actif | |

## Légende

- [ ] À tester
- [x] Validé
- [ ] Bloqué : préciser la raison dans la colonne Notes / bug
- Pour chaque anomalie, noter l'écran, les étapes, le résultat observé, le code HTTP éventuel et joindre une capture.

## Connexion et session

| Statut | ID | Action | Résultat attendu | Notes / bug |
|---|---|---|---|---|
| [ ] | AUTH-01 | Lancer l'app sans session puis saisir des identifiants valides. | La connexion réussit et l'app ouvre Routine avec la session backend active. | |
| [ ] | AUTH-02 | Saisir un email ou un mot de passe incorrect. | L'app reste sur Connexion et affiche une erreur claire sans exposer de détail technique. | |
| [ ] | AUTH-03 | Se connecter, fermer complètement l'app puis la relancer. | La session persistante est restaurée et l'utilisateur revient dans l'app sans ressaisir ses identifiants. | |
| [ ] | AUTH-04 | Depuis Profil, choisir Déconnexion puis Annuler. | Le dialogue se ferme et la session reste active. | |
| [ ] | AUTH-05 | Depuis Profil, choisir Déconnexion puis Se déconnecter. | Le token et l'enfant actif sont effacés, puis l'écran Connexion s'affiche. | |
| [ ] | AUTH-06 | Après déconnexion, utiliser le bouton Retour Android puis relancer l'app. | Aucun ancien écran ni ancien token n'est restauré ; Connexion reste affiché. | |
| [ ] | AUTH-07 | Se déconnecter puis se connecter avec un autre compte. | Le nouveau profil et ses données remplacent entièrement ceux du compte précédent. | |
| [ ] | AUTH-08 | Depuis Connexion, choisir Continuer en mode local. | L'app s'ouvre sans appel nécessitant un token et les données locales restent utilisables. | |

## Routine, Mission et Quête

| Statut | ID | Action | Résultat attendu | Notes / bug |
|---|---|---|---|---|
| [ ] | PLAN-01 | Ouvrir Routine avec un compte enfant. | Les routines de l'enfant actif sont chargées, lisibles et sans doublon. | |
| [ ] | PLAN-02 | Marquer une routine comme terminée. | L'état, la progression et les récompenses associées sont rafraîchis une seule fois. | |
| [ ] | PLAN-03 | Annuler une routine terminée si l'action est disponible. | L'état revient correctement sans incohérence de progression. | |
| [ ] | PLAN-04 | Ouvrir Mission. | Les missions, objectifs, progressions et récompenses du bon enfant sont affichés. | |
| [ ] | PLAN-05 | Terminer une mission disponible. | La mission passe à l'état terminé et les données du Nid sont rafraîchies. | |
| [ ] | PLAN-06 | Ouvrir Quête. | Les quêtes du bon enfant sont affichées avec progression et récompenses. | |
| [ ] | PLAN-07 | Terminer une quête disponible. | La quête passe à l'état terminé et les gains sont reflétés dans Le Nid. | |
| [ ] | PLAN-08 | Tester Routine, Mission et Quête sans contenu. | Un état vide clair s'affiche, sans grande carte vide ni erreur. | |

## Le Nid

| Statut | ID | Action | Résultat attendu | Notes / bug |
|---|---|---|---|---|
| [ ] | NEST-01 | Ouvrir Le Nid avec un compte connecté disposant d'un enfant actif. | Le Nid charge sans erreur et affiche les données du bon `child_id`. | |
| [ ] | NEST-02 | Vérifier les pastilles Flammèches et Cristaux. | Les soldes correspondent au backend et restent lisibles. | |
| [ ] | NEST-03 | Vérifier le compagnon actif. | Le nom, le stade et la progression du dragon actif sont corrects. | |
| [ ] | NEST-04 | Ouvrir Le Nid sans compagnon actif. | Un état vide propre propose d'aller au Bestiaire. | |
| [ ] | NEST-05 | Utiliser chaque accès rapide du Nid. | Inventaire, Œufs, Dragons, Parchemins et Caverne ouvrent le bon écran. | |

## Profil et comptes

| Statut | ID | Action | Résultat attendu | Notes / bug |
|---|---|---|---|---|
| [ ] | PROF-01 | Appuyer sur l'avatar en haut à droite. | L'écran Profil du compte courant s'ouvre. | |
| [ ] | PROF-02 | Comparer le nom, l'email, le rôle et les statistiques au backend. | Les informations appartiennent au compte et à l'enfant actifs. | |
| [ ] | PROF-03 | Ouvrir Profil en mode local. | Un profil local explicite s'affiche sans erreur réseau bloquante. | |
| [ ] | PROF-04 | Appuyer sur Déconnexion. | Un dialogue fantasy affiche « Se déconnecter de ce compte ? », Annuler et Se déconnecter. | |
| [ ] | PROF-05 | Changer de compte puis rouvrir Profil. | Aucune donnée du compte précédent n'est visible. | |

## Caverne, coffres et inventaire

| Statut | ID | Action | Résultat attendu | Notes / bug |
|---|---|---|---|---|
| [ ] | CAVE-01 | Depuis Le Nid, ouvrir Caverne > Souhaits. | Les souhaits disponibles s'affichent avec leur coût en Flammèches. | |
| [ ] | CAVE-02 | Depuis Le Nid ou la pastille Cristaux, ouvrir Caverne > Coffres. | La section Coffres est sélectionnée et le catalogue backend est chargé. | |
| [ ] | CHEST-01 | Vérifier les coffres commun, rare et épique. | Chaque coffre affiche rareté, coût et récompenses possibles. | |
| [ ] | CHEST-02 | Ouvrir un coffre avec assez de Cristaux. | Le coffre s'ouvre, le gain réel est affiché et le solde est rafraîchi. | |
| [ ] | CHEST-03 | Vérifier un résultat contenant `quantity` et `quantity_total`. | `quantity` est présenté comme gain ; `quantity_total` n'est pas présenté comme gain. | |
| [ ] | CHEST-04 | Tenter d'ouvrir un coffre sans assez de Cristaux. | L'action est refusée avec un message utilisateur simple et sans perte de données. | |
| [ ] | INV-01 | Ouvrir Inventaire. | Les monnaies, objets, matériaux, fragments, artefacts et coffres possédés sont affichés. | |
| [ ] | INV-02 | Ouvrir un coffre puis revenir dans Inventaire. | Les nouveaux objets et quantités totales sont correctement rafraîchis. | |

## Bestiaire, œufs et dragons

| Statut | ID | Action | Résultat attendu | Notes / bug |
|---|---|---|---|---|
| [ ] | BEST-01 | Ouvrir Bestiaire / Dragons. | Les familles sont regroupées, compactes et sans texte coupé. | |
| [ ] | BEST-02 | Vérifier une famille verrouillée et une famille découverte. | Les badges, assets, états d'œuf, stades dragon et artefacts sont cohérents. | |
| [ ] | EGG-01 | Ouvrir Œufs. | Chaque œuf possédé affiche son état courant et son asset correct. | |
| [ ] | EGG-02 | Faire évoluer un œuf autorisé. | L'état progresse d'une étape et Bestiaire, Inventaire et Nid sont rafraîchis. | |
| [ ] | EGG-03 | Faire éclore un œuf prêt. | Le résultat indique l'éclosion et le dragon baby obtenu apparaît. | |
| [ ] | DRAGON-01 | Ouvrir Dragons puis définir un dragon comme compagnon. | Le badge actif et Le Nid affichent immédiatement le nouveau compagnon. | |
| [ ] | DRAGON-02 | Faire évoluer un dragon autorisé. | Le stade, l'asset et les ressources consommées sont rafraîchis. | |
| [ ] | DRAGON-03 | Tenter une évolution sans ressources suffisantes. | L'action est refusée avec un message clair, sans modifier les données. | |

## Parchemins et souhaits

| Statut | ID | Action | Résultat attendu | Notes / bug |
|---|---|---|---|---|
| [ ] | REWARD-01 | Ouvrir Parchemins. | Les parchemins du bon enfant sont chargés et lisibles. | |
| [ ] | REWARD-02 | Utiliser un parchemin disponible. | Son état est mis à jour une seule fois et l'action suivante est cohérente. | |
| [ ] | REWARD-03 | Ouvrir Souhaits avec un compte enfant. | Les souhaits accessibles et leur coût en Flammèches sont affichés. | |
| [ ] | REWARD-04 | Demander un souhait avec un solde suffisant puis insuffisant. | La demande valide réussit ; le refus affiche une erreur claire sans débit incorrect. | |

## Erreurs API et réseau

| Statut | ID | Action | Résultat attendu | Notes / bug |
|---|---|---|---|---|
| [ ] | ERR-401 | Provoquer une session expirée ou un token invalide. | La session locale est effacée et l'app revient à Connexion sans boucle. | |
| [ ] | ERR-403 | Tenter une action non autorisée pour le rôle courant. | L'action est bloquée avec un message compréhensible ; aucune donnée n'est modifiée. | |
| [ ] | ERR-404 | Ouvrir ou agir sur une ressource inexistante. | L'app affiche « ressource introuvable » ou équivalent et reste stable. | |
| [ ] | ERR-422 | Envoyer une saisie invalide ou incomplète. | La validation est expliquée sans afficher le détail FastAPI brut. | |
| [ ] | ERR-500 | Simuler une erreur serveur. | L'app affiche une erreur générique, reste navigable et permet de réessayer. | |
| [ ] | NET-01 | Couper le réseau avant une ouverture d'écran distant. | Un message réseau clair s'affiche, sans crash ni chargement infini. | |
| [ ] | NET-02 | Rétablir le réseau puis rafraîchir ou rouvrir l'écran. | Les données distantes se chargent normalement. | |
| [ ] | NET-03 | Couper le réseau pendant une action d'écriture. | L'app évite les doubles actions et indique que l'opération n'a pas abouti. | |
| [ ] | RELAUNCH-01 | Fermer l'app depuis chaque écran principal puis la relancer. | L'app restaure une destination cohérente sans écran vide ni crash. | |
| [ ] | RELAUNCH-02 | Relancer après déconnexion. | Connexion s'affiche et aucun ancien compte n'est restauré. | |

## Bilan de recette

| Niveau | Nombre | Références |
|---|---:|---|
| Bloquants | 0 | |
| Importants | 0 | |
| Confort | 0 | |
| Tests bloqués | 0 | |

Décision de recette :

- [ ] Validé pour continuer
- [ ] Validé avec réserves
- [ ] Bloqué

Notes générales :

-

