# Architecture Android — La Marche des Âmes

**Proposition technique pour un APK sideloadé sur Pixel 7a, avec synchronisation des pas Garmin.**

Version 1.0 — novembre 2026.

---

## 1. Décision recommandée

| Sujet | Choix recommandé |
|---|---|
| Langage | **Kotlin 2.x** |
| UI | **Jetpack Compose** (Material 3) |
| Architecture | **Clean Architecture pragmatique + MVI/MVVM**, modules `app`, `game-engine`, `data` |
| Jeu | **Moteur Kotlin pur, déterministe, testable**, rendu **Compose Canvas** |
| Persistance | **Room** + **DataStore** |
| Sync de fond | **WorkManager** (15–30 min) + synchronisation à l'ouverture + pull-to-refresh |
| Source Garmin | **Garmin Connect Personal API** : clé personnelle → jeton OAuth2 Bearer 24 h |
| Authentification Garmin | **2FA dans le navigateur Garmin**, jamais dans l'APK |
| Secours | **Health Connect** (écriture Garmin → Health Connect, lecture locale) |
| Backend | **Aucun backend nécessaire** pour le mode Personnel |
| Distribution | APK signé, sideload via `adb install` / GitHub Releases |
| minSdk / targetSdk | **34 (Android 14) / 36 (Android 16)** |
| Licence | MIT pour le code, CC0 pour le pixel art, OFL pour les polices |

**En une phrase :** Kotlin + Compose + Room + WorkManager + Garmin Personal API (clé + bearer token) + Health Connect en secours, avec un moteur de jeu pur Kotlin et un rendu Compose Canvas.

---

## 2. Réalité de l'authentification Garmin

C'est le point le plus important, et il faut corriger une idée répandue : **Garmin n'expose pas un flux OAuth 2.0 « grand public » à trois pattes pour les particuliers**. Depuis le 21 octobre 2026, Garmin propose deux voies distinctes.

### 2.1 Voie recommandée — Garmin Connect Personal API

Garmin a ouvert un accès **Personnel**, gratuit, réservé à l'usage d'une seule personne sur ses propres données :

1. L'utilisateur se connecte à **Garmin Connect Web** (c'est là que la **2FA Garmin** est effectuée).
2. Il ouvre la page **Personal API Key**, crée une clé, choisit sa durée (30, 90, 180 ou 365 jours) et la copie. La clé n'est affichée **qu'une seule fois**.
3. L'application échange cette clé contre un **jeton Bearer OAuth2** valable **24 heures**.
4. Elle appelle l'API avec ce jeton. Il n'y a **pas de refresh token** : on redemande un jeton avec la clé tant que celle-ci est valide.
5. Quand la clé expire, l'utilisateur refait l'opération (login Garmin + 2FA + nouvelle clé).

**Conséquence :** on ne construit **jamais** un formulaire Garmin mot de passe + 2FA dans l'APK. On délègue l'identité à Garmin via un **Custom Tab** (navigateur intégré système), puis l'utilisateur colle la clé dans un champ sécurisé. C'est à la fois plus sûr et conforme aux conditions Garmin.

**Endpoints utiles :**

```http
POST https://personal.connectapi.garmin.com/personalapitoken-service/personal-api/personal-api-key/token
Authorization: ApiKey <clé-personnelle>
Accept: */*
```

La réponse est un **JWT en texte brut** (pas un JSON `access_token`). Ensuite :

```http
GET https://personal.connectapi.garmin.com/usersummary-service/stats/steps/daily/{startDate}/{endDate}
Authorization: Bearer <jeton>
```

- Plage maximale : **28 jours** pour les pas.
- Alternative : `GET /usersummary-service/v2/dailysummaries` (jusqu'à un an).
- Base Chine : `personal.connectapi.garmin.cn`.
- Jeton 24 h, pas de refresh token.
- Limite de débit à surveiller (de l'ordre d'une centaine de requêtes/minute selon les retours) ; gérer `429` et `retryInSeconds`.
- Une journée vide peut renvoyer `204` ou `null`/`[]`.
- L'API est **read-only**.
- La clé personnelle ne doit être utilisée que par son titulaire, pour ses propres outils. Elle ne doit pas être partagée ni servir une application multi-utilisateurs.

**Sources :**
- FAQ Personal API : <https://developer.garmin.com/gc-developer-program/personal/personal-faq/>
- Page de création de clé : <https://connect.garmin.com/app/settings/personalApiToken>
- Swagger Personal API : <https://developer.garmin.com/gc-developer-program/personal/swagger-docs/>
- Client officiel Python : <https://github.com/garmin/connect-personal-api-python>

### 2.2 Voie « vrai OAuth 2.0 à trois pattes » — Garmin Developer Program

Si tu veux absolument un consentement OAuth avec redirection, c'est la voie **business** (Starter ou Enterprise), avec **OAuth 2.0 Authorization Code + PKCE** :

1. L'app ouvre la page d'autorisation Garmin dans un Custom Tab.
2. L'utilisateur se connecte et fait sa **2FA chez Garmin**.
3. Garmin redirige vers un **backend** avec un code.
4. Le backend échange le code avec le `client_secret` (qui ne doit **jamais** être dans l'APK) et renvoie un jeton de session à l'app.
5. Le backend rafraîchit le jeton Garmin et sert les données à l'app.

Cette voie nécessite :
- une demande d'accès au Garmin Connect Developer Program ;
- un backend (Cloudflare Worker, petit Ktor, etc.) pour garder le secret et les jetons ;
- une gestion multi-utilisateurs si tu distribues l'app (hors Personal API).

Pour une application **personnelle sideloadée** avec un seul utilisateur, c'est **surdimensionné**. La Personal API est la voie officielle prévue pour ce cas.

### 2.3 Ce qu'il ne faut pas faire

- **Ne pas** reverse-engineer l'API mobile Garmin Connect ni scraper le site.
- **Ne pas** stocker le mot de passe Garmin ni le code 2FA.
- **Ne pas** demander à l'utilisateur de saisir son code 2FA dans une interface maison.
- **Ne pas** mettre la clé personnelle Garmin dans le dépôt, dans les logs, ou dans un APK public.
- **Ne pas** utiliser une clé personnelle pour un service multi-utilisateurs.

### 2.4 Le problème de la journée de jeu à 04:00

La spec actuelle prévoit un reset quotidien à **04:00**. Or les résumés quotidiens Garmin sont indexés sur `calendarDate`, c'est-à-dire **minuit local**. Une API de totaux journaliers ne permet pas de découper précisément 04:00 → 03:59.

Trois options :

1. **Recommandé pour le MVP** : aligner la journée de jeu sur **00:00 local**, comme Garmin. On modifie la spec et on garde un ledger idempotent.
2. Garder 04:00 et utiliser **Health Connect** : les `StepsRecord` ont des intervalles, on peut agréger 04:00 → 04:00. Mais l'écriture Garmin → Health Connect peut être retardée ou incomplète.
3. Garder 04:00 avec une approximation de 4 h, ce qui fausse les jours de marche et n'est pas recommandé.

**Décision à prendre avant le développement du ledger.** L'architecture isole ce choix dans un `GameDayPolicy(resetHour)`.

---

## 3. Architecture logicielle

### 3.1 Modules Gradle

Pour un projet solo, éviter 15 modules. Trois modules suffisent :

```
:app                 → Android, Compose, navigation, ViewModels, DI, WorkManager
:game-engine         → Kotlin/JVM pur, moteur déterministe, zéro dépendance Android
:data                → Room, DataStore, réseau Garmin, Health Connect, crypto, repositories
```

Plus tard, on peut extraire `:feature:home`, `:feature:inventory`, `:feature:upgrades`, `:feature:camp` sans casser le moteur.

```
idle-step-rpg-android/
├── app/
│   ├── src/main/java/.../MainActivity.kt
│   ├── ui/scene/              # Compose Canvas + boucle de rendu
│   ├── ui/home/               # scène + onglets Stats / Inventaire / Améliorations
│   ├── ui/camp/               # feu de camp, soins, journal
│   ├── ui/settings/           # compte Garmin, clé, sync, confidentialité
│   ├── ui/auth/               # Custom Tab + saisie sécurisée de la clé
│   ├── work/                  # GarminSyncWorker, HealthConnectSyncWorker
│   └── di/
├── game-engine/
│   ├── step/                  # StepLedger, StepDayPolicy, conversion 10 pas = 1 m
│   ├── run/                   # RunEngine, rencontres, paliers, gardiens
│   ├── combat/                # CombatEngine, critique, ferveur, rations
│   ├── economy/               # or, XP, upgrades, loot, coffres
│   └── model/                 # snapshots sérialisables
├── data/
│   ├── local/                 # Room, DAO, migrations, DataStore
│   ├── remote/garmin/         # OAuth token, client HTTP, DTO, mapping
│   ├── healthconnect/         # lecture StepsRecord, DataOrigin Garmin
│   ├── crypto/                # Keystore + stockage chiffré
│   └── repository/            # StepRepository, GameRepository, SyncLogRepository
└── gradle/libs.versions.toml
```

### 3.2 Stack technique

| Brique | Choix | Pourquoi |
|---|---|---|
| Langage | Kotlin 2.x | Standard Android, coroutines, interop Java |
| UI | Jetpack Compose + Material 3 | UI déclarative, rapide à itérer, bon support des tailles mobiles |
| DI | Hilt | Standard, testable ; alternative : DI manuelle pour un mini-projet |
| Persistance | Room | Transactions, migrations, requêtes par jour |
| Préférences | DataStore | Réglages, dernier état de sync |
| Réseau | Retrofit + OkHttp + kotlinx.serialization | Simple, MockWebServer pour les tests |
| Async | Coroutines + Flow | Sync, moteur, UI |
| Travail de fond | WorkManager | Contraintes réseau, périodique, robuste au reboot |
| Santé | Health Connect (`androidx.health.connect`) | Lecture locale des pas Garmin |
| Sécurité | Android Keystore + AES-GCM | Clé Garmin jamais en clair |
| Rendu | Compose Canvas + `ImageBitmap` | Pixel art net, pas de moteur lourd |
| Tests | JUnit, Turbine, MockWebServer, Robolectric, Compose UI Test | Moteur, sync, UI |

**Pas de Unity, Godot, Flutter ou React Native pour ce projet.** La scène est une vue 2D de sprites ; l'intégration Android (Health Connect, WorkManager, Keystore, Custom Tabs) est native. Un moteur externe ajouterait 20–60 Mo, complexifierait la 2FA et n'apporterait rien.

### 3.3 Pattern de présentation

**MVI pragmatique** :

- `HomeUiState` immuable : scène, stats, onglet actif, journal, état de sync.
- `HomeIntent` : `SyncRequested`, `BuyUpgrade`, `EquipItem`, `OpenCamp`, `Rest`, etc.
- `HomeEffect` : toast, vibration, ouverture de sheet, navigation.
- `HomeViewModel` : expose `StateFlow<HomeUiState>` et `SharedFlow<HomeEffect>`.
- Le moteur de jeu tourne hors du ViewModel, dans un `GameSession` sur `Dispatchers.Default`.
- L'UI ne contient aucune règle de jeu.

### 3.4 Le moteur de jeu

Le moteur est **pur Kotlin**, **déterministe** et **testable sans Android** :

```kotlin
interface GameClock { fun now(): Instant }

class StepLedger(
    private val dayPolicy: GameDayPolicy,
    private val random: Random
) {
    fun ingest(totalSteps: Int, date: LocalDate): StepDelta
}
```

Principes :

- **Tick fixe** (100 ms) pour les combats et la régénération.
- **RNG seedé** et persisté : un combat rejoué produit le même résultat.
- **Aucune dépendance à l'horloge murale** dans les formules de combat.
- **Snapshot sérialisable** : `RunState`, `CombatState`, `InventoryState`, `FerveurState`.
- **Avance hors ligne** : `advanceTo(now)` applique les ticks manquants, plafonnés (farm 20 min, combat 60 s).
- **Idempotence** : ingérer deux fois le même total de pas ne doit rien produire de plus.

Séparation :

```
StepLedger → RunEngine → CombatEngine → Economy
                ↑
          StepDelta (pas nouveaux)
```

Le rendu ne fait que lire un `GameSnapshot` ; il ne décide jamais.

### 3.5 Rendu pixel art

- `Canvas` Compose, `ImageBitmap`, `FilterQuality.None`.
- Boucle de rendu avec `withFrameNanos` ; état lu en phase de dessin (`drawWithCache`/`drawBehind`) pour éviter les recompositions.
- Scène ~48 % de la hauteur, tableau de bord ~52 %, comme la maquette.
- Sprites préchargés (PNG locaux), pas de chargement à chaque frame.
- 30 fps pour la scène suffisent ; 60 fps si tu ajoutes des particules.
- Si le profilage montre du jank : garder Compose pour le dashboard et passer la scène en `SurfaceView` + `Choreographer`. Ne pas changer d'architecture pour autant.
- Effets : `HapticFeedback`, `Vibrator` ; audio via `SoundPool`.

---

## 4. Données et synchronisation

### 4.1 Modèle Room

```sql
daily_steps(
  calendar_date TEXT NOT NULL,
  steps INTEGER NOT NULL,
  distance_m REAL,
  source TEXT NOT NULL,          -- GARMIN | HEALTH_CONNECT | MANUAL
  fetched_at INTEGER NOT NULL,
  PRIMARY KEY (calendar_date, source)
)

step_cursor(
  game_day TEXT PRIMARY KEY,
  processed_steps INTEGER NOT NULL,
  source TEXT NOT NULL,
  updated_at INTEGER NOT NULL
)

game_state(
  id INTEGER PRIMARY KEY CHECK (id = 1),
  snapshot_json TEXT NOT NULL,
  updated_at INTEGER NOT NULL
)

inventory(id TEXT PRIMARY KEY, payload_json TEXT NOT NULL)
journal(id INTEGER PRIMARY KEY AUTOINCREMENT, created_at INTEGER, type TEXT, text TEXT)
sync_log(id INTEGER PRIMARY KEY AUTOINCREMENT, at INTEGER, source TEXT, result TEXT, detail TEXT)
```

- `daily_steps` : toujours **remplacer** le total du jour, ne jamais l'additionner. Garmin met à jour le résumé au fil de la journée.
- `step_cursor` : le nombre de pas déjà consommés par le moteur pour la journée de jeu.
- `game_state` : snapshot JSON du moteur, transactionnel avec le curseur.
- Tout est local. Aucun envoi de données de santé vers un serveur dans le mode Personnel.

### 4.2 Algorithme de synchronisation

```
1. Vérifier le jeton Garmin (expire < 1 h → redemander un jeton avec la clé).
2. GET /usersummary-service/stats/steps/daily/{start}/{end}
   (aujourd'hui + hier ; 7 à 28 jours au premier branchement).
3. UPSERT daily_steps(date, total, source=GARMIN).
4. Dans une transaction :
     delta = max(0, total - cursor.processed_steps)
     cursor.processed_steps = max(cursor.processed_steps, total)
     snapshot = engine.ingest(delta, date)
     sauvegarder snapshot + cursor
5. Si Garmin échoue : lire Health Connect, filtrer DataOrigin Garmin Connect,
   agréger la journée, utiliser le même ledger avec source=HEALTH_CONNECT.
6. Journaliser le résultat et la source utilisée.
```

**Règles anti-double comptage :**

- Le total Garmin est **cumulatif** ; on ne traite que le delta positif.
- Si Garmin corrige à la baisse, on **ne retire pas** la distance déjà parcourue ; on journalise l'anomalie et on garde le maximum.
- Un changement de jour crée un nouveau curseur ; aucun report du reliquat de l'ancien jour.
- Le reliquat de conversion (pas non convertis en mètres) est stocké dans le snapshot.
- Une synchronisation interrompue est rejouable sans effet de bord.

### 4.3 Garmin Personal API : cycle de vie

| État | Déclencheur | Action |
|---|---|---|
| `MissingKey` | Premier lancement | Ouvrir la page Garmin, coller la clé |
| `Validating` | Clé saisie | POST token ; tester un GET steps |
| `Connected` | Jeton valide | Sync normale |
| `TokenExpiring` | < 1 h | Redemander un jeton avec la clé |
| `KeyExpired` | 401 / clé expirée | Demander une nouvelle clé (login + 2FA) |
| `RateLimited` | 429 | Attendre `retryInSeconds`, retry avec backoff |
| `Error` | Réseau / 5xx | Garder les données locales, retry WorkManager |

- La clé est stockée **chiffrée** (Keystore AES-GCM) dans un fichier/DataStore dédié.
- Le jeton 24 h peut être gardé en mémoire + stockage chiffré pour survivre au redémarrage.
- Pas de refresh token : on refait l'échange clé → jeton.
- Afficher la date d'expiration de la clé et un rappel 7 jours avant.
- `FLAG_SECURE` sur l'écran de saisie de la clé ; pas de logs ; pas de sauvegarde Android de la clé.

### 4.4 Health Connect (secours et quasi-temps réel)

Sur Android 14+, Garmin Connect peut écrire les pas dans **Health Connect** (transfert **unidirectionnel**, après synchro de la montre). L'app peut lire ces données avec `READ_STEPS`.

- Permission : `android.permission.health.READ_STEPS`.
- Lire `StepsRecord` sur la plage souhaitée, filtrer `DataOrigin` du package Garmin Connect, agréger.
- Mettre Garmin Connect en **priorité** dans Health Connect (sinon le podomètre du Pixel peut gagner).
- Health Connect ne remplace pas l'API Garmin pour l'exactitude : des utilisateurs rapportent des retards et des sous-comptages. C'est un **fallback** et un moyen d'avoir des données locales sans réseau.
- Ne pas écrire de pas dans Health Connect (l'app est lectrice) pour éviter les boucles.

### 4.5 Planification

- **À l'ouverture** : sync immédiate (réseau disponible).
- **WorkManager périodique** : toutes les 15–30 min, contrainte réseau, backoff exponentiel.
- **Pull-to-refresh** dans l'UI.
- **Pas de foreground service permanent** : inutile pour lire des pas ; coût batterie. Un foreground service uniquement pendant une session de jeu active, si tu veux des notifications de combat.
- Garmin Connect doit être autorisé en arrière-plan pour que la montre synchronise ses pas ; le jeu ne peut pas forcer cette synchro.

---

## 5. Authentification et sécurité

### 5.1 Flux recommandé (Personal API)

```
[App] --Custom Tab--> [Garmin Connect Web + 2FA] --création clé--> [Utilisateur copie la clé]
   |                                                                        |
   |<------------------ colle la clé dans un champ sécurisé ----------------|
   |
   |--POST token (ApiKey)--> [Garmin] --JWT 24 h--> [Keystore chiffré]
   |
   |--GET steps (Bearer)--> [Garmin] --> [Room] --> [Moteur de jeu]
```

- Le Custom Tab est le navigateur système ; l'app ne voit jamais le mot de passe ni le code 2FA.
- La clé est un secret long (30–365 jours) : la traiter comme un mot de passe.
- Le jeton 24 h est un secret court : ne pas l'écrire en clair.
- Après expiration de la clé, l'utilisateur refait la création (2FA incluse).
- Si tu veux éviter le copier-coller, tu peux proposer un bouton « Ouvrir Garmin Connect » puis un collage depuis le presse-papiers ; ne pas lire le presse-papiers sans action explicite.
- Un `WebView` pour le login Garmin est déconseillé : risque de phishing, mauvaise gestion des cookies, politiques Garmin. Utiliser **Custom Tabs**.

### 5.2 Stockage

- **Android Keystore** : générer une clé AES-256 non exportable ; chiffrer la clé Garmin et le jeton.
- **DataStore** pour l'état non sensible (dernière sync, expiration, source).
- `android:allowBackup="false"` ou règles d'exclusion pour le fichier chiffré.
- `FLAG_SECURE` sur l'écran de clé.
- Pas de log de la clé, du jeton, ni des URLs complètes.
- Écran « Révoquer l'accès » : supprimer clé + jeton + données Garmin locales (avec confirmation).
- Aucune analytique tierce par défaut ; données de santé locales.

### 5.3 Si tu veux la voie OAuth 2.0 à trois pattes

- Créer une app dans le Garmin Connect Developer Program (Starter/Enterprise).
- Backend obligatoire pour le `client_secret` et les jetons.
- OAuth 2.0 PKCE : Custom Tab → `connect.garmin.com/oauth2Confirm` → callback backend → session.
- 2FA toujours chez Garmin.
- L'app ne doit jamais embarquer le secret.
- Cette voie est utile si l'app doit servir **plusieurs utilisateurs** ; pour un seul utilisateur, la Personal API est plus simple, gratuite et suffisante.

---

## 6. APK et sideload sur Pixel 7a

### 6.1 Configuration Android

- `minSdk 34` : Pixel 7a est sur Android 14+ ; Health Connect est intégré.
- `targetSdk 36` / `compileSdk 36` (vérifier la version stable au moment du build).
- Une seule `Activity` + Navigation Compose.
- Permissions :
  - `INTERNET`
  - `android.permission.health.READ_STEPS`
  - `POST_NOTIFICATIONS` si notifications de jeu
  - éventuellement `FOREGROUND_SERVICE` si session active
- Health Connect : déclarer la permission, afficher une explication, ouvrir le contrat de permission.
- `android:allowBackup="false"`, `usesCleartextTraffic="false"`.

### 6.2 Build et signature

```bash
# Une seule fois : créer un keystore de release, à sauvegarder hors du dépôt
keytool -genkeypair -v \
  -keystore ~/keys/idle-step-rpg-release.jks \
  -alias idle-step-rpg \
  -keyalg RSA -keysize 4096 -validity 10000

# Build
./gradlew :app:assembleRelease

# Installation sur le Pixel 7a
adb install -r app/build/outputs/apk/release/app-release.apk
```

- Configurer `signingConfigs.release` via des propriétés locales (`~/.gradle/gradle.properties` ou variables d'environnement), **jamais** dans le dépôt.
- Sauvegarder le keystore et les mots de passe : sans eux, les mises à jour ne s'installent pas par-dessus l'ancienne version.
- Activer « Installer des applis inconnues » pour le gestionnaire de fichiers ou le navigateur utilisé.
- Play Protect peut afficher un avertissement : c'est normal pour un APK sideloadé.
- Mettre l'APK dans une **GitHub Release** pour faciliter le téléchargement ; ne pas le committer dans le dépôt.
- Option CI : un workflow GitHub Actions qui construit un APK debug à chaque tag, et un APK release signé avec des secrets si tu veux automatiser. Pour un projet personnel, un build local suffit.

### 6.3 Après installation

1. Ouvrir l'app, accorder la permission Health Connect (si utilisée).
2. Ouvrir Garmin Connect, vérifier que la montre a synchronisé aujourd'hui.
3. Créer la clé Personal API sur Garmin Connect Web (2FA), la coller dans l'app.
4. Lancer une première sync ; vérifier le total de pas et le total Garmin.
5. Désactiver l'optimisation batterie pour **Garmin Connect** (pour la synchro montre → téléphone) et éventuellement pour l'app.
6. Tester : avion, redémarrage, clé expirée, jeton expiré, 429, changement de jour.

---

## 7. Tests et qualité

### 7.1 Tests unitaires (JVM, sans Android)

- `StepLedger` : total cumulatif, delta, correction à la baisse, double ingestion, changement de jour, DST, 28 février, fuseau.
- `GameDayPolicy` : minuit vs 04:00, cohérence du curseur.
- `CombatEngine` : seed, critique, phase 2 du gardien, ration, défaite, farm.
- `Economy` : coûts croissants, plafonds, arrondis, puissance.
- `DropTable` : déterminisme, rareté, plafonds.
- `GameSnapshot` : sérialisation/désérialisation, migration de version.

### 7.2 Tests d'intégration

- **MockWebServer** : token 200/401/429, JWT texte, steps 200/204, pagination, timeout.
- **Fake Health Connect** : DataOrigin Garmin, agrégation, doublons, records vides.
- **WorkManager** : contrainte réseau, retry, worker unique.
- **Keystore** : chiffrement/déchiffrement, perte de clé, rotation.
- **Compose** : écran de clé masqué, camp, achat, équipement.

### 7.3 Tests manuels

- Création de clé + 2FA réelle.
- Clé expirée / révoquée.
- Jeton expiré après 24 h.
- Garmin indisponible, Health Connect disponible.
- Garmin et HC en désaccord.
- Changement de jour à 00:00 ou 04:00.
- App tuée pendant une transaction.
- Avion, réseau lent, 429.
- Sideload + mise à jour avec la même signature.
- Batterie : 24 h avec WorkManager, mesurer la consommation.

### 7.4 Observabilité locale

- Écran « Synchronisation » : dernière tentative, source, total, delta, erreur, expiration de la clé.
- `sync_log` en base, exportable en texte (sans secret).
- Pas d'analytics externe.

---

## 8. Feuille de route proposée

| Étape | Contenu | Résultat |
|---|---|---|
| 0 | Squelette Android + design system + moteur Kotlin pur | App qui affiche la scène et simule des pas |
| 1 | Room + StepLedger + Health Connect | Pas réels lus localement, jeu qui avance |
| 2 | Personal API Garmin + écran de clé + token | Sync Garmin officielle |
| 3 | WorkManager + réconciliation + offline + tests | Sync robuste au quotidien |
| 4 | Camp, inventaire, améliorations, journal, polish | Version 1 jouable |
| 5 | APK release signé + GitHub Release + captures | Sideload sur Pixel 7a |

**MVP recommandé :** scène + onglets + sync Garmin + un palier + un gardien + camp. Le reste suit.

---

## 9. Risques et parades

| Risque | Impact | Parade |
|---|---|---|
| Garmin change la Personal API / les conditions | Sync cassée | Isoler derrière `GarminStepSource` ; fallback Health Connect ; ne pas scraper |
| Clé personnelle expirée | Sync bloquée | Bandeau + rappel 7 jours ; parcours de renouvellement clair |
| Jeton 24 h expiré | Sync bloquée | Renouvellement automatique avec la clé ; pas de refresh token |
| 2FA à chaque renouvellement | Friction | Clé de 365 jours ; documenter ; Custom Tab |
| Journée de jeu 04:00 vs Garmin minuit | Comptage faux | Choisir 00:00 ou utiliser Health Connect (voir §2.4) |
| Health Connect en retard / sous-compte | Écart de pas | Garmin prioritaire ; afficher la source et l'heure |
| Limite de débit | 429 | Cache, 1 requête/jour, backoff, pas de polling |
| Perte du keystore | Impossible de mettre à jour | Sauvegarde hors ligne |
| APK non signé / debug | Installation instable | Release signé, même keystore à chaque version |
| Données de santé sensibles | Vie privée | Stockage local, Keystore, pas de logs, pas de backend |

---

## 10. Ce que je recommande concrètement

1. **Kotlin + Jetpack Compose**, pas de moteur externe.
2. **Trois modules** : `app`, `game-engine`, `data`.
3. **Garmin Personal API** comme source principale, avec **clé personnelle + jeton Bearer 24 h**.
4. **Health Connect** comme secours et source locale rapide.
5. **Aucun écran Garmin maison** : 2FA dans le Custom Tab, jamais dans l'APK.
6. **Aucun backend** pour la version personnelle.
7. **Moteur pur, déterministe, idempotent**, avec un ledger de pas et un curseur.
8. **Décider maintenant** : journée de jeu à 00:00 (simple) ou 04:00 (Health Connect / epoch).
9. **APK release signé**, distribué par GitHub Releases, installé via `adb install -r`.
10. **Tests du ledger et de la sync avant l'UI** : c'est là que les bugs de double comptage coûtent le plus cher.

---

## 11. Références

- Garmin Connect Personal API — FAQ : <https://developer.garmin.com/gc-developer-program/personal/personal-faq/>
- Garmin Connect Personal API — page de clé : <https://connect.garmin.com/app/settings/personalApiToken>
- Garmin Connect Personal API — Swagger : <https://developer.garmin.com/gc-developer-program/personal/swagger-docs/>
- Garmin — client Python officiel : <https://github.com/garmin/connect-personal-api-python>
- Garmin Connect Developer Program — FAQ OAuth 2.0 : <https://developer.garmin.com/gc-developer-program/enterprise/program-faq/>
- Android Health Connect — pas : <https://developer.android.com/health-and-fitness/health-connect/features/steps>
- Android Health Connect — prise en main : <https://support.google.com/android/answer/12201227>
- Garmin Connect → Health Connect (partage unidirectionnel) : à vérifier dans Garmin Connect → Réglages → Applications connectées → Health Connect sur ton Pixel 7a.

> Les API, les conditions et les quotas Garmin évoluent. Vérifier le Swagger et les conditions au moment du développement ; les informations ci-dessus reflètent l'état public de novembre 2026.
