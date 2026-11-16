# Architecture Android — La Marche des Âmes

**Proposition technique pour un APK sideloadé sur Pixel 7a, en pur Garmin.**

Version 1.1 — novembre 2026.

> **Décisions verrouillées**
> - Source de pas unique : **Garmin Connect Personal API**.
> - **Health Connect est explicitement hors périmètre** : aucune permission, aucun code, aucune dépendance.
> - La journée de jeu se réinitialise à **00:00 local**, comme Garmin (`calendarDate`).
> - La clé Garmin est saisie dans l'application, jamais dans le dépôt ni dans l'APK.
> - La 2FA Garmin est faite chez Garmin, jamais dans l'application.

---

## 1. Synthèse

| Sujet | Choix |
|---|---|
| Langage | **Kotlin 2.x** |
| UI | **Jetpack Compose** Material 3 |
| Architecture | **3 modules** : `:app`, `:game-engine`, `:data` |
| Moteur | Kotlin pur, déterministe, testable, tick fixe |
| Rendu | Compose Canvas, `ImageBitmap`, nearest-neighbor |
| Persistance | Room + DataStore + Android Keystore |
| Sync | WorkManager + sync à l'ouverture + pull-to-refresh |
| Garmin | Personal API : clé → **jeton Bearer 24 h** |
| Reset | **00:00 local** |
| Backend | Aucun |
| Distribution | APK release signé, GitHub Releases, sideload |
| minSdk / targetSdk | 34 / 36 |
| Sources de secours | **Aucune** : cache local + retry Garmin uniquement |

**En une phrase :** Kotlin + Compose + Room + WorkManager + Garmin Personal API, avec un moteur de jeu pur Kotlin et un rendu Compose Canvas, sans Health Connect ni backend.

---

## 2. Garmin Personal API

### 2.1 Pourquoi cette API

Depuis le 21 octobre 2026, Garmin propose une **Personal API** officielle pour les particuliers, en lecture seule, destinée à un usage strictement personnel. C'est la voie adaptée à une application sideloadée mono-utilisateur.

- Pas de consentement OAuth à trois pattes.
- Pas de backend.
- La clé est créée par l'utilisateur sur Garmin Connect Web, derrière son login et sa 2FA.
- La clé est échangée contre un jeton OAuth2 **Bearer** de 24 h.
- Pas de refresh token : on redemande un jeton avec la clé.
- La clé expire au bout de 30, 90, 180 ou 365 jours.

### 2.2 Authentification

```http
POST https://personal.connectapi.garmin.com/personalapitoken-service/personal-api/personal-api-key/token
Authorization: ApiKey <clé-personnelle>
Accept: */*
```

- La réponse est un **JWT en texte brut**, pas un JSON `access_token`.
- Le JWT contient un `exp` : expiration du **jeton**, 24 h au maximum, ou expiration de la clé si elle survient avant.
- Les appels suivants :

```http
GET https://personal.connectapi.garmin.com/usersummary-service/stats/steps/daily/{startDate}/{endDate}
Authorization: Bearer <jeton>
```

- Plage maximale : **28 jours** pour les pas.
- Alternative : `GET /usersummary-service/v2/dailysummaries` (jusqu'à un an).
- Base Chine : `personal.connectapi.garmin.cn`.
- API **read-only**.
- Limite de débit à surveiller ; gérer `429` et `retryInSeconds`.
- Journée vide : `204` ou `null`/`[]`.
- La clé ne doit être utilisée que par son titulaire, pour ses propres outils. Ne pas la partager, ne pas la mettre dans un service multi-utilisateurs.

### 2.3 2FA : ce que fait l'application, ce qu'elle ne fait pas

**L'application ne demande jamais le mot de passe Garmin ni le code 2FA.**

Le parcours est :

1. L'utilisateur appuie sur « Connecter Garmin ».
2. Un **Custom Tab** ouvre la page Garmin Connect **Personal API Key**.
3. Garmin affiche son login et sa **2FA**. L'application ne voit rien.
4. L'utilisateur crée la clé, la copie, revient dans l'app.
5. L'app affiche un champ sécurisé (`FLAG_SECURE`) pour coller la clé.
6. L'app échange la clé contre un jeton et affiche l'état « Connecté ».

C'est le seul mécanisme officiel et sûr. Un écran 2FA maison serait à la fois un risque de sécurité et une violation des conditions Garmin.

### 2.4 Expiration de la clé : ce que Garmin expose

**Garmin n'expose pas l'expiration de la clé personnelle via l'API.** La date est visible sur Garmin Connect Web, sur la page Personal API Key.

Ce que l'API permet de connaître :

- l'expiration du **jeton** via le `exp` du JWT (24 h, ou moins si la clé expire avant) ;
- l'échec d'un échange de clé (`401`) quand elle est expirée ou révoquée.

Ce que l'API ne permet pas :

- la date d'expiration propre à la clé ;
- le statut « clé encore valide » sans tenter un échange.

**Conséquence pour l'IHM :** l'utilisateur renseigne la date d'expiration de la clé, l'app la stocke (ce n'est pas un secret), affiche un compte à rebours et des rappels à 30, 7 et 1 jour. L'app vérifie aussi le `exp` du jeton à chaque connexion.

> On peut préremplir la date à +1 an par défaut, mais l'utilisateur doit pouvoir la corriger. La date du 17/11/2027 se saisit dans cet écran, pas dans le code.

### 2.5 IHM « Compte Garmin »

Écran **Réglages → Garmin** :

```
┌─────────────────────────────────────────┐
│ COMPTE GARMIN                           │
│                                         │
│ Statut          ● Connecté              │
│ Clé             ••••••••  [Remplacer]   │
│ Expire le       17/11/2027  [Modifier]  │
│ Jeton           valide jusqu'à 14:32    │
│ Dernière sync   aujourd'hui, 14:05      │
│                                         │
│ [ Tester la connexion ]                 │
│ [ Synchroniser maintenant ]             │
│ [ Révoquer la clé et effacer ]          │
│                                         │
│ ⓘ La clé est stockée chiffrée sur cet   │
│   appareil. Elle n'est jamais envoyée   │
│   à un serveur de l'application.        │
└─────────────────────────────────────────┘
```

Comportements :

- **Remplacer la clé** : Custom Tab Garmin + champ sécurisé + échange + validation.
- **Modifier l'expiration** : date picker, stockée dans DataStore.
- **Tester** : échange de clé + appel `GET steps` sur aujourd'hui ; affiche OK/401/429.
- **Révoquer** : supprime la clé, le jeton et les données Garmin locales ; confirmation explicite.
- **Rappels** : notification locale à 30/7/1 jour ; bandeau dans l'app.
- **Jeton expiré** : renouvellement silencieux avec la clé si elle est valide, sinon passage en `NeedsKey`.
- **Clé expirée** : bandeau « Clé Garmin expirée — remplacez-la » ; la sync est bloquée, pas le jeu.

### 2.6 Sécurité de la clé

- La clé est un secret long (30–365 jours) : la traiter comme un mot de passe.
- Stockage : **Android Keystore** (AES-256-GCM), clé non exportable.
- Le jeton 24 h est stocké chiffré ou gardé en mémoire + Keystore.
- `FLAG_SECURE` sur l'écran de clé ; pas de capture d'écran.
- Aucun log de la clé, du jeton ou des URLs signées.
- `android:allowBackup="false"` et exclusion des sauvegardes cloud pour le fichier chiffré.
- La clé n'est **jamais** dans le dépôt Git, dans l'APK, dans les ressources ou dans un fichier de configuration.
- Si la clé est exposée (partage, chat, capture), **la révoquer immédiatement** sur Garmin Connect et en créer une nouvelle.

---

## 3. Architecture logicielle

### 3.1 Modules Gradle

```
:app
  Compose, navigation, ViewModels, Hilt, WorkManager, Custom Tabs
:game-engine
  Kotlin/JVM pur, aucun Android : StepLedger, RunEngine, CombatEngine, Economy
:data
  Room, DataStore, Retrofit/OkHttp, Garmin DTO, crypto, repositories
```

Pas de module Health Connect. Pas de dépendance `androidx.health.connect`.

```
app/
  ui/scene/          # Canvas + boucle de rendu
  ui/home/           # scène + Stats / Inventaire / Améliorations
  ui/camp/           # feu de camp
  ui/settings/       # compte Garmin, clé, expiration, sync
  ui/auth/           # Custom Tab + saisie sécurisée
  work/              # GarminSyncWorker
  di/
game-engine/
  step/              # StepLedger, GameDayPolicy(00:00), conversion 10 pas = 1 m
  run/               # paliers, rencontres, gardiens
  combat/            # dégâts, critique, ferveur, rations
  economy/           # or, XP, upgrades, loot, coffres
  model/             # snapshots sérialisables
data/
  local/             # Room, DAO, migrations, DataStore
  remote/garmin/     # token, client, DTO, mapping
  crypto/            # Keystore + stockage chiffré
  repository/        # StepRepository, GameRepository, SyncLogRepository
```

### 3.2 Stack

| Brique | Choix |
|---|---|
| Langage | Kotlin 2.x |
| UI | Compose + Material 3 |
| DI | Hilt (ou DI manuelle) |
| Base | Room |
| Préférences | DataStore |
| Réseau | Retrofit + OkHttp + kotlinx.serialization |
| Async | Coroutines + Flow |
| Fond | WorkManager |
| Sécurité | Android Keystore + AES-GCM |
| Rendu | Compose Canvas + ImageBitmap |
| Tests | JUnit, MockWebServer, Turbine, Compose UI Test, Robolectric |

**Pas de Unity, Godot, Flutter ou React Native.** La scène est une vue 2D de sprites : Compose Canvas suffit sur le Tensor G2 du Pixel 7a, et l'intégration Keystore/Custom Tabs/WorkManager reste native. Un moteur externe ajouterait du poids et de la complexité pour aucun bénéfice.

### 3.3 Présentation

- MVI pragmatique : `UiState` immuable, `Intent`, `Effect`.
- `HomeViewModel` expose `StateFlow<HomeUiState>`.
- Le moteur tourne hors du ViewModel, sur `Dispatchers.Default`.
- L'UI ne contient aucune règle de jeu.
- Un seul `GameSession` par processus, protégé par un `Mutex`.

### 3.4 Moteur

- Tick fixe de 100 ms.
- RNG seedé, persisté.
- Snapshots sérialisables et versionnés.
- `advanceTo(now)` pour le hors ligne, plafonné (farm 20 min, combat 60 s).
- Ingestion de pas **idempotente**.
- Aucune dépendance à l'heure murale dans les formules de combat.

```
StepLedger → RunEngine → CombatEngine → Economy
                 ↑
            StepDelta
```

### 3.5 Rendu

- `Canvas` Compose, `ImageBitmap`, `FilterQuality.None`.
- Boucle `withFrameNanos`, état lu en phase de dessin, pas de recomposition par frame.
- Scène 48 % / dashboard 52 %, comme la maquette 390 × 844.
- Sprites préchargés ; pas d'allocation par frame.
- 30 fps suffisent ; 60 fps si particules.
- Si jank : `SurfaceView` pour la scène, Compose pour le dashboard.

---

## 4. Données et synchronisation Garmin

### 4.1 Room

```sql
daily_steps(
  calendar_date TEXT PRIMARY KEY,   -- yyyy-mm-dd, jour local
  steps INTEGER NOT NULL,
  distance_m REAL,
  fetched_at INTEGER NOT NULL
)

step_cursor(
  game_day TEXT PRIMARY KEY,        -- yyyy-mm-dd
  processed_steps INTEGER NOT NULL,
  updated_at INTEGER NOT NULL
)

game_state(
  id INTEGER PRIMARY KEY CHECK (id = 1),
  snapshot_json TEXT NOT NULL,
  updated_at INTEGER NOT NULL
)

key_meta(
  id INTEGER PRIMARY KEY CHECK (id = 1),
  expires_on TEXT,                  -- date saisie, non secrète
  last_validated_at INTEGER,
  token_expires_at INTEGER
)

sync_log(
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  at INTEGER,
  result TEXT,
  http_code INTEGER,
  detail TEXT
)
```

- `daily_steps` : **remplacer** le total du jour, jamais l'additionner. Garmin met à jour le résumé au fil de la journée.
- `step_cursor` : pas déjà consommés par le moteur pour la journée de jeu.
- `game_state` : snapshot transactionnel avec le curseur.
- La clé et le jeton ne sont **pas** en base : ils sont dans le Keystore chiffré.

### 4.2 Reset à 00:00 local

- Garmin renvoie `calendarDate` en date locale (minuit → minuit).
- La journée de jeu est donc exactement la journée Garmin : `GameDayPolicy(resetHour = 0)`.
- Aucun découpage 04:00, aucune approximation, aucun besoin de Health Connect.
- À 00:00, on ouvre un nouveau `step_cursor` avec `processed_steps = 0`.
- Le reliquat de conversion (pas non convertis en mètres) est dans le snapshot du moteur ; il ne traverse pas la journée.

### 4.3 Algorithme

```
1. Si le jeton expire dans < 1 h : POST token avec la clé.
2. GET /usersummary-service/stats/steps/daily/{start}/{end}
   - aujourd'hui + hier ;
   - 7 à 28 jours au premier branchement.
3. UPSERT daily_steps(date, total).
4. Transaction :
     delta = max(0, total - cursor.processed_steps)
     cursor.processed_steps = max(cursor.processed_steps, total)
     snapshot = engine.ingest(delta, date)
     sauvegarder snapshot + cursor
5. Journaliser la source, le code HTTP et le delta.
```

**Règles anti-double comptage :**

- Garmin donne un total **cumulatif** ; on ne traite que le delta positif.
- Si Garmin corrige à la baisse, on ne retire pas la distance déjà parcourue ; on journalise l'anomalie et on garde le maximum.
- Une synchronisation interrompue est rejouable sans effet de bord.
- Aucun autre podomètre, aucune autre source : Garmin uniquement.
- En cas d'échec Garmin : on garde le dernier état local, on affiche la dernière sync, on retente via WorkManager. Pas de source de substitution.

### 4.4 Planification

- Sync à l'ouverture.
- WorkManager périodique 15–30 min, contrainte réseau, backoff exponentiel.
- Pull-to-refresh.
- Pas de foreground service permanent.
- Garmin Connect doit pouvoir tourner en arrière-plan pour que la montre pousse les pas ; l'app ne peut pas forcer cette synchro.

### 4.5 Gestion des erreurs

| Cas | Comportement |
|---|---|
| 401 clé invalide/expirée | `NeedsKey` ; bandeau ; sync bloquée ; jeu jouable avec le cache |
| 401 jeton expiré | Re-échange clé → jeton, une fois |
| 429 | Attendre `retryInSeconds`, retry backoff |
| 5xx / réseau | Garder le cache, retry WorkManager |
| 204 / pas de données | Delta 0 ; afficher « aucune nouvelle donnée » |
| Total Garmin en baisse | Ne pas retirer la distance ; logguer |
| Clé expirée | Bandeau + rappel ; parcours de remplacement |

---

## 5. Build, signature et sideload

### 5.1 Configuration

- `minSdk 34` : Pixel 7a sur Android 14+.
- `targetSdk 36` / `compileSdk 36` (ou la version stable au moment du build).
- Permissions : `INTERNET`, `POST_NOTIFICATIONS` (si notifications). Aucune permission santé.
- `android:allowBackup="false"`, `usesCleartextTraffic="false"`.
- Une seule Activity + Navigation Compose.

### 5.2 Signature

```bash
keytool -genkeypair -v \
  -keystore ~/keys/idle-step-rpg-release.jks \
  -alias idle-step-rpg \
  -keyalg RSA -keysize 4096 -validity 10000

./gradlew :app:assembleRelease

adb install -r app/build/outputs/apk/release/app-release.apk
```

- Keystore hors du dépôt, sauvegardé hors ligne.
- Mots de passe via variables d'environnement ou `~/.gradle/gradle.properties`, jamais dans Git.
- Même signature pour toutes les mises à jour.
- APK publié dans une **GitHub Release**, pas commité.
- Activer « Installer des applis inconnues » pour le navigateur/gestionnaire de fichiers.
- Play Protect affichera un avertissement normal.

### 5.3 Après installation

1. Ouvrir l'app.
2. « Connecter Garmin » → Custom Tab → login + 2FA → créer la clé → coller.
3. Renseigner la date d'expiration (17/11/2027).
4. Tester la connexion, lancer une sync.
5. Vérifier le total Garmin et le total du jeu.
6. Autoriser Garmin Connect en arrière-plan ; désactiver son optimisation batterie si nécessaire.
7. Tester : clé révoquée, jeton expiré, avion, redémarrage, changement de jour.

---

## 6. Ce que je peux faire seul, et ce qui reste manuel

### 6.1 Côté code et build : oui, tout

Je peux produire et exécuter sans Android Studio :

- le projet Android complet (Kotlin, Compose, Gradle wrapper, versions catalog) ;
- le moteur Kotlin pur, les tests unitaires, les tests de sync ;
- l'IHM de clé, d'expiration, de sync, de révocation ;
- le client Garmin (token, JWT, 429, retry) ;
- le keystore de release, la signature, l'APK release ;
- les builds Gradle en ligne de commande ;
- la publication de l'APK dans une GitHub Release ;
- éventuellement un workflow GitHub Actions pour reconstruire l'APK.

L'environnement contient déjà :
- Android SDK (`platforms` 35 et 37, `build-tools` 36.0.0, `adb` 37.0.1) ;
- Java 24 et le JBR d'Android Studio en Java 25 ;
- un cache Gradle 9.8.1 ;
- 1 To d'espace libre.

Je fixerai la version de JDK/Gradle/AGP compatible (JDK 21 ou 17 via ligne de commande si nécessaire). Pas besoin que tu ouvres Android Studio ni que tu lances Gradle.

### 6.2 Ce qui reste inévitablement manuel

1. **Sur le Pixel** : autoriser l'installation d'applis inconnues et confirmer l'installation de l'APK. Android impose cette confirmation ; aucun outil ne peut la contourner.
2. **Si tu veux que j'installe via `adb`** : activer le débogage USB, brancher le téléphone, autoriser l'ordinateur. C'est une manipulation unique.
3. **Créer la clé Garmin** : login + 2FA chez Garmin. Je ne dois jamais connaître ton mot de passe ni ton code 2FA.
4. **Saisir la clé dans l'app** : je ne veux pas la recevoir dans le chat.
5. **Sauvegarder le keystore** : sans lui, pas de mise à jour par-dessus l'ancienne version.
6. **Autoriser Garmin Connect en arrière-plan** sur le téléphone.

Le reste — code, Gradle, tests, signature, APK, release — est automatisable par moi.

---

## 7. Tests

### 7.1 Unitaires

- `StepLedger` : total cumulatif, delta, correction à la baisse, double ingestion, changement de jour à 00:00, DST, fuseau.
- `GameDayPolicy` : minuit local, cohérence du curseur.
- `CombatEngine` : seed, critique, phase 2, ration, défaite, farm.
- `Economy` : coûts croissants, plafonds, raretés.
- Snapshot : sérialisation, migration.

### 7.2 Intégration

- **MockWebServer** : token 200/401/429, JWT texte, steps 200/204, timeout.
- **Keystore** : chiffrement, perte de clé, rotation.
- **WorkManager** : contrainte réseau, retry, worker unique.
- **Compose** : écran de clé masqué, camp, achat, équipement.

### 7.3 Manuels

- Création de clé + 2FA réelle.
- Clé révoquée/expirée.
- Jeton expiré après 24 h.
- Garmin indisponible → cache + retry, pas d'autre source.
- Changement de jour à 00:00.
- App tuée pendant une transaction.
- Avion, réseau lent, 429.
- Sideload + mise à jour même signature.
- Batterie sur 24 h.

---

## 8. Feuille de route

| Étape | Contenu |
|---|---|
| 0 | Squelette Android + design system + moteur Kotlin pur |
| 1 | Room + StepLedger + reset 00:00 + jeu qui avance avec des pas simulés |
| 2 | Écran Garmin : clé, expiration, token, test, sync |
| 3 | WorkManager + reprise + erreurs + tests |
| 4 | Camp, inventaire, améliorations, journal, polish |
| 5 | APK release signé + GitHub Release + sideload |

**MVP :** scène + onglets + sync Garmin + un palier + un gardien + camp.

---

## 9. Risques

| Risque | Impact | Parade |
|---|---|---|
| Clé Garmin exposée | Accès aux données | Révocation immédiate, clé dans Keystore, jamais dans Git/chat |
| Clé expirée | Sync bloquée | IHM date + rappels 30/7/1 j + remplacement guidé |
| Jeton 24 h expiré | Sync bloquée | Re-échange automatique avec la clé |
| 2FA au renouvellement | Friction | Clé 365 jours, Custom Tab, parcours clair |
| Garmin indisponible | Pas de nouveaux pas | Cache local, retry, aucune autre source |
| API Garmin modifiée | Sync cassée | Isoler derrière `GarminStepSource`, tests MockWebServer |
| Limite de débit | 429 | Cache, 1 requête/jour, backoff |
| Keystore perdu | Plus de mise à jour | Sauvegarde hors ligne |
| APK debug/non signé | Installation instable | Release signé + même keystore |

---

## 10. Références

- Garmin Connect Personal API — FAQ : <https://developer.garmin.com/gc-developer-program/personal/personal-faq/>
- Garmin Connect Personal API — clé : <https://connect.garmin.com/app/settings/personalApiToken>
- Garmin Connect Personal API — Swagger : <https://developer.garmin.com/gc-developer-program/personal/swagger-docs/>
- Client Python officiel : <https://github.com/garmin/connect-personal-api-python>
- Garmin Connect Developer Program — FAQ OAuth 2.0 : <https://developer.garmin.com/gc-developer-program/enterprise/program-faq/>

> Les API, conditions et quotas Garmin évoluent. Vérifier le Swagger et les conditions au moment du développement ; ces informations reflètent l'état public de novembre 2026.
