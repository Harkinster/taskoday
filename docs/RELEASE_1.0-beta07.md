# Taskoday 1.0-beta07

Version Android : **1.0-beta07**, code **8**, package **com.harkinster.taskoday**.
Cette bêta réunit les interfaces validées V4.1.3 et V4.2.1 sans nouvelle règle métier.

## Nouveautés

- Ma Maison harmonisée, avatars et navigation fantasy.
- Exploration personnelle et dashboard Suivi familial réservé aux parents.
- Le Nid et les sous-écrans Chronodria mieux organisés.
- Formulaires de création, attribution, récurrence et détail harmonisés.
- Connexion illustrée et inscription plus cohérentes ; libellé « Créer un compte ».
- Améliorations de lisibilité, accessibilité, petits écrans, stabilité et clavier.

Les notes distribuées par le système de mise à jour sont dans `CHANGELOG_1.0-beta07.json`.

## Artefact officiel

| Métadonnée | Valeur |
| --- | --- |
| Variante | Release, non débogable |
| Android | API 24 minimum, cible 36 |
| Taille APK | 93 368 991 octets |
| SHA-256 APK | `990fcaf7300d55f6ca084303df193dcc782cd40e6aec7fbffc54f6767683199e` |
| SHA-256 certificat | `4783e3e7f20c4bfec5e6b22fb0e3cfcaeb0dfddca3ebc380929c7fb1243a666b` |
| Signature | Clé Release existante, alias `taskoday-beta`, schéma APK v2 vérifié |
| Backend | `https://harkserv.ddns.net/taskoday-api/` |
| Manifeste | `https://harkserv.ddns.net/taskoday/version.json` |
| Téléchargement permanent | `https://harkserv.ddns.net/taskoday/latest.apk` |
| Archive immuable | `https://harkserv.ddns.net/taskoday/releases/v8-990fcaf7300d55f6/taskoday-1.0-beta07-8.apk` |
| Politique de mise à jour | Facultative ; version minimale supportée : 1 |

Le package, le certificat, le stockage de session et le schéma local Android restent compatibles avec la mise à jour de beta06. Ne pas désinstaller l'application pour la mettre à jour.

## Provenance et contrôles de publication

- V4.1.3 : `b3c124555fe0b9ca9db01ef935e13374a0e01b24`.
- V4.2.1 : `dfbe34649b31f41fdeb1814056ecfed6fc6eca4e`.
- Changement applicatif de cette préparation : uniquement l'incrément de version Gradle.
- Build `:app:assembleRelease` exécuté avec la signature officielle ; package, code, version, signature, certificat, CRC ZIP, ressources et destinations backend/updater vérifiés.
- Compatibilité serveur : 96 opérations Retrofit et 122 schémas associés identiques au contrat public ; services métier critiques et migrations jusqu'à 0018 identiques. Les protections serveur supplémentaires 0019 restent en place. Aucun déploiement backend, migration ou changement de données nécessaire.
- Inspection SQL directe indisponible pour le compte SSH ; aucune tentative d'écriture ni de contournement. La compatibilité est établie par les contrats publics et le code actif, sans afficher de données utilisateur.
- Les 468 tests unitaires, 24 instrumentés, 15 contrôles UI et 8 contrôles API du checkpoint sont des résultats historiques, non rejoués pendant cette publication. Aucune campagne sur téléphone physique.
- Publication par le mécanisme officiel existant : validation privée, archive immuable et vérification, puis bascule atomique du pointeur commun au manifeste et au téléchargement. Les releases précédentes sont conservées.

Les preuves de cette publication et son résultat effectif restent localement dans `qa/release-beta07/` ; aucune APK, clé, base, capture ou secret ne fait partie du commit.

## Retour arrière de la distribution

Utiliser le publisher officiel sur HarkServ :

```sh
python3 /opt/taskoday/repo/ops/android-distribution/publisher.py --channel production select-release --version-code 7 --confirm-rollback
python3 /opt/taskoday/repo/ops/android-distribution/publisher.py --channel production verify
```

Cela remet beta06 comme téléchargement proposé, sans supprimer beta07 ni modifier de base. Android ne rétrograde pas les installations déjà passées au code 8 ; un correctif ultérieur devra utiliser un code supérieur à tous les codes publiés.
