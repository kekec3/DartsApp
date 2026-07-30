# Darts — Architecture & Implementation Reference

Prepared as a defense cheat-sheet: what is implemented, where it lives, and how the pieces talk
to each other. Package root: `com.example.darts` (`app/src/main/java/com/example/darts/`).

Scale: ~13.4k lines of Kotlin in `main`, ~2.9k lines in unit tests, 258 unit tests across 14 test
classes.

---

## 1. Technology stack

| Concern | Choice | Where declared |
|---|---|---|
| Language / UI | Kotlin + Jetpack Compose (Material 3) | `app/build.gradle.kts` |
| Architecture | MVVM + repository layer, unidirectional state (StateFlow) | `viewModel/`, `db/repositories/` |
| DI | Hilt (Dagger), KSP annotation processing | `DartsApplication`, `db/hilt/` |
| Persistence | Room (SQLite), DB version 7 | `db/` |
| Preferences | SharedPreferences wrapper | `db/repositories/SettingsRepository.kt` |
| Navigation | Navigation-Compose, **type-safe routes** (`@Serializable` data classes) | `ui/navigation/` |
| Maps | `maps-compose` + Play Services Location | `ui/screens/MapScreen.kt`, `db/hilt/LocationModule.kt` |
| Camera / vision | CameraX + ML Kit barcode scanning | `ui/screens/score_entry/`, `GameImportScreen` |
| Speech | Android `SpeechRecognizer` + custom fuzzy parser | `utils/VoiceImputManager.kt`, `utils/CommandParser.kt` |
| P2P sharing | Google Nearby Connections + QR token handshake | `network/NearbyManager.kt` |
| Serialization | Gson (payloads, turn history), kotlinx.serialization (nav routes) | `dto/`, `db/repositories/DartsExportRepository.kt` |
| Testing | JUnit4, MockK, kotlinx-coroutines-test | `app/src/test/` |

`minSdk 24`, `targetSdk 36`, `compileSdk 36`.

---

## 2. Layering and data flow

```
Composable screen  ──user intent──▶  ViewModel  ──▶  GameEngine (pure Kotlin, no Android)
      ▲                                  │
      │  StateFlow<*DisplayState>        └──▶  Repository ──▶  DAO ──▶  Room / SharedPreferences
      └──────────────────────────────────────────────────────────────────────┘
```

Key rules the code follows:

* **Composables are stateless w.r.t. domain data.** They receive a `*DisplayState` and emit lambdas.
* **Game rules live in `engine/`, which has no Android dependencies** — that is why the engines are
  the most heavily unit-tested part of the project (98 tests).
* **ViewModels translate** engine state → display state (`refresh()` / `mapPlayer()`), and persist
  results through repositories.
* **Navigation is driven by events**, not by the screen: the ViewModel emits
  `GameNavigationEvent.LegSummary` / `.MatchSummary` on a `SharedFlow`; `GameScreen` collects it and
  calls the navigation lambdas supplied by `NavGraph`.

---

## 3. Entry points and app shell

| File | Responsibility |
|---|---|
| `DartsApplication.kt` | `@HiltAndroidApp`. On startup deletes unfinished + guest games (`cleanupUnfinishedGames`). |
| `MainActivity.kt` | Single activity. Animated splash screen exit, edge-to-edge, `Scaffold` + bottom bar visibility rules, runtime permission request for Nearby (Bluetooth/Wi-Fi/location, version-gated), and `.darts` file intent handling for cold (`onCreate`) and warm (`onNewIntent`) starts. |
| `ui/navigation/Destinations.kt` | All routes as `@Serializable` objects / data classes — arguments are typed, no string keys. |
| `ui/navigation/NavGraph.kt` | The whole `NavHost`. Wires every screen, builds `XO1Config`/`CricketConfig` from route args, picks the correct ViewModel (`GameViewModelX01` vs `GameViewModelCricket`) behind the shared `BaseGameViewModel` interface. Also holds the *auto-navigation* effect that jumps to the import screen when a `.darts` payload is staged. |
| `ui/components/DartsBottomBar.kt` | Bottom navigation (Home / Battles / Players / Share) with back-stack handling. |
| `ui/theme/` | `Color.kt` (lime-on-black palette), `Theme.kt` (dark-only scheme), `Type.kt`, `Shapes.kt`. |

---

## 4. Domain model — `db/`

### Entities (`db/entities/`)

| Entity | Table | Notes |
|---|---|---|
| `Player` | `players` | username + avatar path. |
| `Battle` | `battles` | A series/session grouping games. `QuickPlay*` battles are hidden from the UI by DAO queries. |
| `Participate` | `participate` | Join table Player ↔ Battle, composite PK, cascading FKs. |
| `Game` | `games` | Belongs to a battle; `type` (`x01`/`cricket`), `location` ("lat,lng"), `finished`, `history` (JSON turn log). |
| `Moment` | `moments` | Photo / audio / emoji captured during a game, anchored to a `turnNumber`. Enum stored via `MomentConverters`. |
| `PlayerLegStats` | `player_leg_stats` | Immutable per-leg snapshot written once at leg end. |
| `PlayerCareerStats` | `player_career_stats` | One upserted row per player: lifetime totals. |

`db/DartsDatabase.kt` — Room database (version 7, `fallbackToDestructiveMigration`, `exportSchema = false`).

### DAOs (`db/daos/`)
`PlayerDAO`, `BattleDAO`, `GameDAO`, `ParticipateDAO`, `MomentDAO`, `StatDao`.
`StatDao` is an **abstract class rather than an interface** so `upsertCareerStats` can do a
read-modify-write inside a single `@Transaction`.

### Repositories (`db/repositories/`)

| Repository | Responsibility |
|---|---|
| `PlayerRepository` | Player reads. |
| `BattleRepository` | Battle creation with participants, guest/QuickPlay bootstrapping (`ensureGuestEntitiesExist`, `getQuickPlayBattleId`). |
| `GameRepository` | Game CRUD, mark finished, store history JSON, cleanup of abandoned games. |
| `MomentRepository` | Moment reads/writes. |
| `StatRepository` | Persists leg stats, folds them into career stats, exposes observable flows for the stats screen. |
| `SettingsRepository` | SharedPreferences-backed defaults (game type, starting score, legs, doubleOut, masterIn, cutThroat, starting player, sound, suggestions, location tracking). *Package is `com.example.darts.repository` even though the file sits under `db/repositories/` — see §10.* |
| `DartsExportRepository` | Builds/consumes the full transferable data graph (players, battles, participations, games, moments, stats **plus base64 media attachments**); GZIP + Base64 encoding; separate QR path that drops attachments to keep the payload small. On import it **remaps every primary key** (insert with `id = 0`, translate child references through old→new maps) inside a single Room transaction, because the payload carries the sender's ids. |

`dto/PlayerExportPayload.kt` — the wire format (`PlayerExportPayload`, `MomentAttachment`).

### DI modules (`db/hilt/`)
`DatabaseModule` provides the database + every DAO; `LocationModule` provides `FusedLocationProviderClient`.

---

## 5. Game engine — `engine/` (pure Kotlin, fully unit tested)

| File | Contents |
|---|---|
| `DartThrow.kt` | `Multiplier` enum and `DartThrow` (value + multiplier) with `score()`, `isDouble()`, `isTriple()`, `isMiss()`, display helpers. Bull is modelled as value 25. |
| `Turn.kt` | Up to three darts; `totalScore()`. |
| `GameEngine.kt` | Abstract base: keeps `turnHistory`, `submitTurn()`, and **undo by replay** — pop last turn, `resetGame()`, re-apply all remaining turns. |
| `GameEngineX01.kt` | X01 rules: scoring, **bust** (below 0, or 1 with doubleOut), **doubleOut** checkout validation, **masterIn** (only applies until the player has opened), leg/match completion (`legsWon > maxLegs / 2`), per-leg stat accumulation (darts, total scored, checkout attempts/hits, highest checkout, 180/140+/100+), leg snapshot + reset. |
| `GameEngineCricket.kt` | Cricket rules: marks on 15–20 + bull, capped at 3, overflow scoring, **cut-throat** variant (overflow points are given to opponents who have not closed), win condition = all numbers closed *and* score comparison, per-leg board reset. |
| `DartRecommendationEngine.kt` | Checkout finder: exhaustive DFS over all 62 targets up to 3 darts, only double/bull finishes accepted, then a heuristic ranking (`evaluate`: fewer darts ≫ preferred double ≫ triple routes). Also maps sectors to board angles for the AR overlay. |

---

## 6. ViewModels — `viewModel/`

### The game-play trio

| File | Role |
|---|---|
| `GameViewModel.kt` | *Contracts only, no implementation:* `GameConfig`/`XO1Config`/`CricketConfig`, the `BaseGameViewModel` interface (what `GameScreen` is allowed to call), `GameNavigationEvent`, and `TurnSummary`. This is the seam that lets one screen drive two rule sets. |
| `GameViewModelX01.kt` | Loads the game + battle players, applies starting-player policy (random / specific / declared order), drives `GameEngineX01`, auto-commits the turn on 3 darts / checkout / bust, maps state to `GameDisplayState` (3-dart average, last score, darts thrown), persists leg + career stats, serialises turn history to JSON at match end, plays sounds via `SoundManager` when enabled. |
| `GameViewModelCricket.kt` | Same lifecycle for Cricket. Additionally exposes `cricketUiState` (mark grid) for `CricketEntry`, computes MPR instead of an average, and stores MPR in the shared `average` column. Uses `replay = 1` on the navigation flow (hence a real `consumeNavigationEvent()`). |

### State holders (`viewModel/states/`)
`PlayerState` (marker interface), `PlayerStateX01`, `PlayerStateCricket` (+ `CricketNumber`),
`GameState<T>` (players, current player, leg, finished flags, completed-leg snapshot),
`GameDisplayState` (+ `PlayerDisplayState`, `TurnDisplayState`, `DartSlotState`, `StatRow`) — the
pure-UI projection.

### The rest

| File | Role |
|---|---|
| `HomeViewModel` | Resolves "most recent battle" for the start-game shortcut. |
| `QuickPlayViewModel` | One-tap match: reuses/creates a guest `QuickPlayN` battle, creates the game, hands config back to the nav graph. |
| `BattleViewModel` | Player selection, new player creation, battle creation **with duplicate-roster detection** (`saveBattle` → `onDuplicateFound`). |
| `GameCreationViewModel` | Per-battle game list + players, the `GameSettings` form state (seeded from `SettingsRepository`), and game creation including **GPS acquisition** (high-accuracy with 12 s timeout, `lastLocation` fallback, hard error if tracking is on but no fix). |
| `SettingsViewModel` | Mirrors every preference as a `StateFlow`, writes through on change. |
| `LegSummaryViewModel` / `MatchSummaryViewModel` / `BattleSummaryViewModel` | Aggregation at three granularities: one leg, one match, a whole battle (match wins → legs won → average tiebreak). `MatchSummaryViewModel` infers X01 vs Cricket from the data itself. |
| `PlayerStatsViewModel` | Career stats + per-leg average trend as live flows, player switching/search. |
| `MapViewModel` | Reads `battleId` from the route via `SavedStateHandle.toRoute()`; loads all games or one battle's games. |
| `GameSharingViewModel` | Nearby advertising with a random `DARTS_xxxxxx` token, QR payload state, export to public Downloads (MediaStore), WhatsApp text share, FileProvider file share. |
| `GameImportViewModel` | Activity-scoped (owned by `MainActivity`): stages a payload from a `.darts` file URI or from a scanned QR token via Nearby discovery, then commits it on confirmation. |

---

## 7. Screens — `ui/screens/`

| Screen | What it does |
|---|---|
| `HomeScreen` | Landing menu: primary "start game" action + tiles to battles, moments, players, stats, map, settings. |
| `QuickPlayScreen` | Player count (2–4) + X01/Cricket, then straight into a match. |
| `BattlesScreen` | List of battles, create new. |
| `PlayersScreen` | Player CRUD, avatar capture/pick (`saveBitmapToInternalStorage` / `saveUriToInternalStorage`), doubles as multi-select roster picker for a new battle (duplicate-roster dialog). |
| `GameCreateScreen` | Battle lobby: previous games, links to match summary / timeline / battle summary, start new game. |
| `GameSettingsScreen` | Match configuration (type, starting score, legs, doubleOut, masterIn, cut-throat, starting player, location tracking) with runtime location-permission handling. |
| **`GameScreen`** | The scoring screen and the hub of the app: top bar, player cards (row for ≤2, auto-scrolling `LazyRow` for 3–4), dart-slot bar with SUBMIT/BUST, animated swap between entry methods, entry-method bar, plus overlays for AR recommendation, turn history and in-game settings, and a confirm-exit `BackHandler`. |
| `LegSummaryScreen` / `MatchSummaryScreen` / `BattleSummaryScreen` | Per-leg, per-match and per-battle result tables. |
| `TurnHistoryScreen` | Live turn log (slide-up overlay in `GameScreen`). |
| `GameTimelineScreen` | Post-match zig-zag "game map": interleaves the deserialised turn history with captured moments by turn number; renders photos (Coil), plays audio (`MediaPlayer`), shows emoji. Pulls repositories through a Hilt `@EntryPoint` instead of a ViewModel. |
| `MomentScreen` | Capture tab bar: photo (camera → internal storage), ≤5 s audio (`MediaRecorder`, auto-stop, disposal on exit), emoji grid. |
| `MomentsGalleryScreen` | All captured moments. |
| `StatisticsOverviewScreen` | Career dashboard: KPI cards (matches, win rate, 3-dart avg, highest out, checkout %), **hand-drawn Canvas trend chart** with gridlines/labels/gradient fill, scoring distribution bars, detail rows, player search dropdown. |
| `MapScreen` | Dark-styled Google Map, one marker per located game, colour keyed to battle id, info card on selection. |
| `SettingsScreen` | Global defaults, entry point to import. |
| `GameSharingScreen` | Share a player's history: QR code generation (ZXing), Nearby advertising, `.darts` download, WhatsApp/file share. |
| `GameImportScreen` | Import UI: QR camera scanner (`QrCameraScanner`) or staged `.darts` file, confirm/cancel. |

### Score-entry subsystem — `ui/screens/score_entry/`

| File | What it does |
|---|---|
| `EntryMethod.kt` | Sealed class of the five methods (Board, Score, Voice, Camera, Cricket). `supportedEntryMethods` on the ViewModel decides which appear — X01 offers four, Cricket offers two. |
| `BoardButtonsScreen.kt` | Default entry: multiplier selector + number grid + specials + undo, and the shared `PlayerCardMinimal`, `EntryMethodBar`, `ScoreInputEntry` (whole-turn total) components. |
| `CricketEntry.kt` | Cricket mark grid with `CricketMarkCanvas` (drawn `/`, `X`, `⊗` marks). |
| `VoiceRecognitionScreen.kt` | Mic UI with pulse animation, live transcript/feedback, command reference; delegates to `VoiceInputManager`. |
| `CameraARRecommendation.kt` | Full-screen camera preview with a drawn dartboard overlay highlighting the recommended checkout route from `DartRecommendationEngine`. |
| `CameraScanScreen.kt`, `ScanState.kt`, `TypeAndEnterScreen.kt` | **Not wired into the app** — see §10. |

---

## 8. Supporting utilities

| File | Role |
|---|---|
| `utils/CommandParser.kt` | Voice command parser: control words (undo/submit), miss, bull variants, multiplier prefixes, number words **and a Levenshtein-similarity fuzzy matcher with an "accent dictionary"** so mispronunciations still resolve. 68 unit tests. |
| `utils/VoiceImputManager.kt` | `VoiceInputManager`: wraps `SpeechRecognizer`, auto-restarts listening, exposes `VoiceUiState`, feeds recognised candidates to `CommandParser`. |
| `utils/SoundManager.kt` | Singleton `MediaPlayer` wrapper; resolves `raw` resources by name (`an_<score>`, `gsm`, `game_on`) and silently skips missing files. |
| `utils/CustomShapes.kt` | `DartShape` — a `GenericShape` dart silhouette used in UI. |
| `network/NearbyManager.kt` | Nearby Connections: advertise under the QR token as endpoint name; the discoverer only connects to the endpoint whose name equals the scanned token, then receives the byte payload. |

---

## 9. Tests — `app/src/test/`

| Test class | Tests | Focus |
|---|---:|---|
| `engine/GameEngineX01Test` | 39 | Scoring, bust, doubleOut, masterIn, leg/match completion, milestones, undo, multi-player, checkout-attempt tracking. |
| `engine/DartThrowTest` | 30 | Score/double/triple/miss semantics and display strings. |
| `engine/DartRecommendationEngineTest` | 23 | Known checkouts (40→D20, 50→bull, 170, 167…), ranking rules, impossible scores. |
| `engine/GameEngineCricketTest` | 20 | Marks, caps, overflow, cut-throat distribution, leg reset, undo. |
| `engine/TurnTest` | 7 | Turn totals. |
| `utils/CommandParserTest` | 68 | Every command family plus fuzzy/accent cases (and two documented quirks). |
| `db/repositories/StatRepositoryTest` | 9 | DAO delegation and career-stat accumulation (MockK). |
| `viewModel/BattleViewModelTest` | 10 | Selection/name state. |
| `viewModel/states/*Test` | 46 | State defaults, copies, invariants. |
| `viewModel/GameSettingsTest` | 5 | Settings defaults. |
| **Total** | **258** | |

`androidTest/ExampleInstrumentedTest.kt` and `test/ExampleUnitTest.kt` are the unmodified project
templates.

---

## 10. Known gaps, quirks and honest talking points

Worth knowing before someone else finds them:

1. **Unused files (not referenced anywhere).** `ui/screens/PlayerStatsScreen.kt` (superseded by
   `StatisticsOverviewScreen`), `ui/screens/score_entry/CameraScanScreen.kt` + `ScanState.kt`
   (dartboard-scanning prototype), `ui/screens/score_entry/TypeAndEnterScreen.kt` (superseded by
   `ScoreInputEntry` inside `BoardButtonsScreen.kt`). Delete with:
   `git rm app/src/main/java/com/example/darts/ui/screens/PlayerStatsScreen.kt app/src/main/java/com/example/darts/ui/screens/score_entry/{CameraScanScreen,ScanState,TypeAndEnterScreen}.kt`
2. **Google Maps API key is hard-coded in `AndroidManifest.xml`** and committed to the repository.
   The clean fix is a `manifestPlaceholders` entry read from the (git-ignored) `local.properties`;
   the key should also be restricted to this app's package + signing certificate in the Cloud console.
3. **`fallbackToDestructiveMigration()`** means every schema change wipes user data. Fine for a
   student project — say so explicitly rather than being asked.
4. **`DartsDatabase` has both a hand-rolled singleton and a Hilt `@Singleton` provider.** Only the
   Hilt path is used by the app; `getDatabase()` is what it calls.
5. **`SettingsRepository` and `DartsExportRepository` live in `db/repositories/` but declare
   `package com.example.darts.repository`.** Legal in Kotlin, inconsistent with everything else.
6. **`Turn` allows fewer than three darts** (checkout/bust commit early), which is why Cricket MPR
   divides by `dartsThrown` rather than by rounds.
7. **`PlayerStatsViewModel.load()` starts a new collector on `allPlayers` per call** — repeated
   calls accumulate collectors.
8. **Cricket stores MPR in the `average` column** of `PlayerLegStats`; `MatchSummaryViewModel`
   detects the mode by checking whether all checkout fields are zero.
9. **`CommandParser` has two known quirks, both covered by named tests**: "outer" is parsed as a miss
   (substring match with "out"), and "100" fuzzy-matches to 10.
10. **`utils/VoiceImputManager.kt` is misspelled** (the class inside is `VoiceInputManager`).
11. **`QrCameraScanner` accepts an `onClose` lambda but never uses it** — the scanner overlay has no
    dismiss button, so the only way out without scanning is the system back gesture.
12. **Nearby sessions are single-shot per service id.** `startDiscovery` stops any previous session
    first and stops scanning once the target endpoint is found, and `GameImportViewModel` ignores a
    repeated scan of the same token. Without those guards Nearby returns
    `STATUS_ALREADY_DISCOVERED (8002)`, which is what the ML Kit analyzer used to trigger by
    delivering the same QR code on every camera frame.
13. **Nearby `Payload.fromBytes` is capped at 32 KB** (`ConnectionsClient.MAX_BYTES_DATA_SIZE`), but
    `startNearbyAdvertising` sends the export **with** base64 photo/audio attachments, and
    `NearbyManager` never checks the result of `sendPayload`. One captured photo will exceed the cap,
    the send will fail silently, and the receiver will simply never see data. Transfers larger than
    32 KB need `Payload.fromStream`. This is the next thing that will break in the sharing demo.
14. **Career stats are only imported for players the import creates.** If a username already exists
    locally, the local totals are kept — both devices recorded the same matches, so adding or
    overwriting them would corrupt the numbers. Player identity across devices is matched by
    username, so two different people sharing a username merge into one profile.
15. **Gson does not apply Kotlin default values for absent JSON fields.** A `.darts` file produced
    before `attachments`/`careerStats` existed deserialises those lists as `null`, and `importPayload`
    will NPE on them (reported as "Export payload is missing required sections"). Only relevant if
    you test with an old export file.

---

## 11. Likely defense questions → where to point

| Question | Answer / file |
|---|---|
| "Show me your architecture." | §2 diagram; `GameScreen` → `BaseGameViewModel` → `GameEngineX01` → `StatRepository` → `StatDao`. |
| "How do you support two different game types on one screen?" | `BaseGameViewModel` interface + `GameConfig` sealed hierarchy (`viewModel/GameViewModel.kt`); `NavGraph` chooses the implementation. |
| "Why is the engine a separate class?" | Testability — no Android types, 98 tests; see `engine/` and §9. |
| "How does undo work?" | `GameEngine.undoTurn()`: drop last turn, reset, replay. Simple and provably consistent; cost is O(turns). |
| "Where are the rules of X01?" | `GameEngineX01.processTurn` — the `when` block covers exact checkout, bust, and normal score. |
| "How is data persisted?" | Room entities + DAOs (§4); leg stats written once per leg, career stats upserted in a transaction (`StatDao.upsertCareerStats`). |
| "How does sharing work?" | QR code carries only a random token; the actual data goes over Nearby Connections after the discoverer matches that token (`network/NearbyManager.kt`, `GameSharingViewModel`, `GameImportViewModel`). Alternative channel: gzip+Base64 `.darts` file via FileProvider / MediaStore. |
| "How do you handle permissions?" | Nearby cluster requested up-front in `MainActivity` with version gating; camera/mic requested at point of use in `MomentScreen`; location via Accompanist permissions in `GameSettingsScreen`. |
| "What is the recommendation engine?" | DFS over all targets, double-only finishes, heuristic ranking (`DartRecommendationEngine.evaluate`), rendered as an AR overlay in `CameraARRecommendation.kt`. |
| "Any custom drawing?" | Yes — trend chart (`StatisticsOverviewScreen.AverageTrendChart`), cricket marks (`CricketMarkCanvas`), timeline curves (`GameMapNodeRow`), dartboard overlay (`CameraARRecommendation`), `DartShape`. |
| "What would you improve?" | §10 — proper Room migrations, API key out of VCS, delete dead screens, unify the repository packages, add instrumented UI tests. |
