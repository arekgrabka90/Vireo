# VIREO — Project State

**Stan dokumentu:** 2026-11-13
**Gałąź robocza:** `work/vireo-development`
**Zasada nadrzędna:** kod wygrywa nad dokumentacją. W razie rozbieżności należy sprawdzić aktualny kod źródłowy.

## 1. Źródło prawdy i stan repozytorium

Repozytorium GitHub `arekgrabka90/Vireo` jest źródłem prawdy dla projektu.

### Stan lokalny i zdalny — audyt 2026-11-13

- Branch: `work/vireo-development`
- Lokalny HEAD: `7d3ab08ac8d14291ec524d7ac270428bc7920ac0`
- GitHub branch HEAD: `7d3ab08ac8d14291ec524d7ac270428bc7920ac0`
- GitHub `main` HEAD: `fa213bb63857f93c6fad3baa2a6b5a057f3d74d1`
- Lokalny i zdalny HEAD `work/vireo-development` są zgodne (po pushu).
- Różnica commitów: 0 / 0 (0 do przodu, 0 do tyłu).
- Zmiany staged: brak.
- Zmiany tracked unstaged: 1
  - `docs/STATE.md` (ta aktualizacja, przed commitem)
- Pliki untracked: 2
  - `AGENTS.md` (zaktualizowane role; nadal poza repozytorium)
  - `.opencode-permission-probe.txt`
- Ostatni commit (lokalny i zdalny):
  `7d3ab08 fix(room): align kotlinx-serialization with room-testing requirement`
- Commity po poprzednim audycie (`beec446`, 2026-11-12):
  - `6c4d2a5` — `test(room): prepare schema export and migration testing`
  - `597d887` — `ci(room): publish Room schema artifact and prepare migration validation`
  - `8d5a0a5` — `ci(room): fix recursive schema artifact path`
  - `3555f05` — `docs: align project state with repository and record C1.2 scope`
  - `fecb0df` — `test(room): add Room migration test infrastructure (C1.2)`
  - `7d3ab08` — `fix(room): align kotlinx-serialization with room-testing requirement`
- Pliki untracked nie są usuwane, przenoszone, edytowane ani dodawane do Git.
- Backup przed tą aktualizacją dokumentacji:
  `~/VIREO_backups/DOCS_UPDATE_20261113_122325` (audyt stanu repozytorium)
  oraz `~/VIREO_backups/AGENTS_20261113_125345` (AGENTS.md).

### Historia wcześniejszych audytów

- Backup przed wcześniejszą synchronizacją:
  `~/VIREO_backups/PRE_SYNC_WIP_20261112_135918`
- Audyt z 2026-11-12 na `beec446` jest historyczny: opisywał stan, w którym
  lokalny i zdalny HEAD wynosiły `beec446`, różnica commitów 0 / 0.
  Został zastąpiony audytem z 2026-11-13 (oba HEAD `8d5a0a5`, 3 nowe commity Room/C1.1).
- Wcześniejszy audyt z 2026-11-12 wskazywał lokalny HEAD `744e9a3` oraz remote HEAD `b21fc01`.
  Jedyny dodatkowy commit zdalny to `b21fc01 docs: add multilingual VIREO README`,
  obejmujący wyłącznie `README.md`, `README.pl.md` i `README.es.md`.
  Ten stan jest historyczny i został zastąpiony kolejnymi audytami:
  obecnie oba HEAD wynoszą `8d5a0a5`, różnica commitów 0 / 0.
- Snapshot dla Claude (historyczny):
  - ZIP utworzony z `origin/work/vireo-development`.
  - Plik: `VIREO_CLAUDE_GITHUB_20261112_140358.zip`
  - SHA256:
    `99fbc30503c7b74e5f9ba4cf791d70e6d6fcdd9f34e77baedb18e9d6ac8c1bd0`
  - Lokalizacja:
    `~/VIREO_CLAUDE_EXPORT/`
  - ZIP pochodzi ze zdalnej gałęzi GitHub, nie z lokalnego HEAD.

### Historyczny Local WIP — 2026-11-11

Wcześniejszy raport wskazywał:

- Branch: `work/2b3a-tmdb-foundation-20261111_223310`
- HEAD: `40cb5a4`
- 4 zmodyfikowane pliki i 2 pliki untracked.

Ten stan jest historyczny i został zastąpiony audytem z 2026-11-13, który potwierdził
zgodność lokalnego i zdalnego HEAD na `8d5a0a5`. Nie należy traktować tamtych
sześciu plików jako aktualnego WIP.

## 2. Zasady pracy nad projektem

### Role

- **Użytkownik:** właściciel projektu; zatwierdza zakres, zmiany i akceptuje etapy.
- **OpenCode:** główny wykonawca; planuje podejście techniczne w zatwierdzonym
  zakresie, implementuje, prowadzi testy i dokumentację, weryfikuje wyniki,
  wykonuje commit i push oraz raportuje.
- **ChatGPT:** wyłącznie doradca; może być konsultowany, ale nie ma uprawnień
  zatwierdzających i nie jest obowiązkowym etapem workflow.
- **Claude:** nie jest uczestnikiem workflow.

### Obowiązkowy workflow

1. Audyt lokalnego stanu repozytorium i `docs/STATE.md`.
2. Zabezpieczenie lokalnej pracy (backup) przed zmianami.
3. Implementacja wyłącznie w zatwierdzonym zakresie.
4. Przegląd pełnego diffu i sprawdzenie braku zmian poza zakresem.
5. Aktualizacja dokumentacji wynikająca z zadania.
6. Commit zmian w logicznych porcjach.
7. Push na bieżącą gałąź roboczą (`work/vireo-development`).
8. Weryfikacja GitHub Actions i analiza logów; poprawki błędów w tym samym zakresie.

Push na `main`, merge/rebase, amend, tagi i synchronizacja gałęzi wymagają
osobnej zgody właściciela. Zakazane są `reset --hard`, `clean`, force push
i modyfikowanie `.git`.

Nie wolno utożsamiać poprawnego działania CI z akceptacją etapu.

### Status każdego etapu

Każdy etap musi mieć trzy osobne statusy:

- **W kodzie**
- **Testy przeszły**
- **Zaakceptowany**

Statusy są niezależne. Nie wolno uznawać etapu za zaakceptowany wyłącznie na podstawie udanego builda.

## 3. Architektura

### Gradle

Projekt zawiera jeden moduł Gradle:

- `:app`

Kod jest logicznie rozdzielony na pakiety, ale nie należy opisywać projektu jako wielomodułowego projektu Gradle.

### Główne obszary kodu

`app/src/main/java/app/vireo/`

- `data/` — repozytoria, źródła danych, ustawienia i warstwa lokalna.
  - `local/`
  - `security/`
  - `tmdb/`
  - `xtream/`
- `domain/` — modele domenowe i providerzy.
- `di/` — konfiguracja zależności.
- `player/` — odtwarzacz, stan sesji, HUD i kontrolery.
  - `controller/`
  - `engine/`
  - `hud/`
  - `session/`
  - `source/`
- `ui/` — interfejs aplikacji.
  - `browse/`
  - `catalog/`
  - `components/`
  - `detail/`
  - `home/`
  - `library/`
  - `list/`
  - `live/`
  - `more/`
  - `profiles/`
  - `search/`
  - `settings/`
  - `shell/`
  - `theme/`

Testy:
`app/src/test/java/app/vireo/`

### Kluczowe pliki Player

- `player/PlayerViewModel.kt`
- `player/PlayerScreen.kt`
- `player/PlaybackSession.kt`
- `player/PlayerHudState.kt`
- `player/PlayerHudOverlay.kt`
- `player/PlayerRuntimeState.kt`
- `player/engine/Media3PlayerEngine.kt`
- `player/engine/PlayerEngine.kt`
- `player/controller/PlayerController.kt`
- `player/hud/HudControls.kt`
- `player/hud/HudClock.kt`
- `player/hud/HudFormatters.kt`
- `player/hud/HudGuideOverlay.kt`
- `player/hud/HudInfoBlock.kt`
- `player/hud/HudLogoOrTitle.kt`
- `player/hud/HudPreviews.kt`
- `player/hud/HudScrims.kt`
- `player/hud/HudTileRow.kt`
- `player/hud/HudTimeline.kt`
- `player/hud/HudTokens.kt`
- `player/session/PlaybackQueue.kt`
- `player/session/PlaybackSessionState.kt`
- `player/source/PlaybackSource.kt`

### Ustawienia i profile

- `data/ProfileSettings.kt`
- `data/ProfileSettingsRepository.kt`
- `ui/settings/SettingsScreen.kt`
- `ui/settings/SettingsViewModel.kt`
- `ui/profiles/ProfileEditScreen.kt`
- `ui/profiles/ProfileEditViewModel.kt`
- `ui/profiles/ProfileSelectScreen.kt`
- `ui/profiles/ProfilesViewModel.kt`

## 4. Wersje zależności — stan zweryfikowany w kodzie

| Element | Wersja / wartość |
|---|---|
| Android Gradle Plugin | 8.13.2 |
| Kotlin | 2.3.20 |
| KSP | 2.3.10 |
| Hilt plugin | 2.58 |
| Gradle | 8.13 |
| compileSdk | 36 |
| minSdk | 24 |
| targetSdk | 34 |
| Java/JVM target | 17 |
| Media3 ExoPlayer/UI/HLS | 1.11.1 |
| Room | 2.8.5 |
| Compose BOM | 2024.06.00 |
| Coil | 2.6.0 |
| OkHttp | 4.12.0 |
| Coroutines | 1.8.1 |
| Kotlin Serialization | 1.8.1 |

Nie aktualizować tych wersji bez osobnej, zatwierdzonej decyzji.

`kotlinx-serialization-json` podniesiono z 1.6.3 do 1.8.1 w C1.2
(zgoda właściciela 2026-11-13). Powód: `room-migration:2.8.5`, używany przez
`room-testing` w `androidTest`, wymaga runtime 1.8.1. Przy 1.6.3 testy
instrumentowane kończyły się `AbstractMethodError`
(`GeneratedSerializer.typeParametersSerializers()`), mimo że testy jednostkowe
przechodziły.

## 5. Baza danych Room

Plik:
`app/src/main/java/app/vireo/data/local/AccountDb.kt`

Zweryfikowany stan kodu (2026-11-13):

- `VireoDatabase` — wersja 14.
- `exportSchema = true` (zmienione w `6c4d2a5`).
- KSP zapisuje schemat do `app/schemas/` (`app/build.gradle.kts`, blok `ksp`,
  `room.schemaLocation`).
- `androidTestImplementation("androidx.room:room-testing:2.8.5")` jest zadeklarowane
  i od C1.2 faktycznie używane przez test instrumentowany.
- C1.2 (2026-11-13): dodano `testInstrumentationRunner`, source set `androidTest`
  z assetami schematów oraz test `VireoDatabaseMigrationTest`.
- Istnieje migracja `MIGRATION_13_14`.
- Brak migracji `1 → 2` (lista migracji zaczyna się od `2 → 3`) — nadal otwarte.

### Wyeksportowany schemat v14

- Źródło: artefakt CI `vireo-room-schemas` z workflow #158 (commit `8d5a0a5`),
  pobrany przez `gh run download`.
- Plik wewnątrz artefaktu: `app.vireo.data.local.VireoDatabase/14.json`.
- Rozmiar artefaktu: 3 068 B. Dostępność artefaktu do 2026-12-12.
- `identityHash`: `410131225083a186b43291867168bf6e`.
- Liczba encji w schemacie: 14 (zgodnie z listą w `@Database`).
- C1.2: plik skopiowany do repozytorium jako
  `app/schemas/app.vireo.data.local.VireoDatabase/14.json` (35 974 B, sformatowany JSON)
  i wskazany jako asset testów instrumentowanych.
- Schemat potwierdza m.in.:
  - `library_items` bez kluczy obcych,
  - `live_channel_history` z FK tylko do `profiles` (`sourceId` bez FK),
  - istniejący indeks `index_epg_programs_sourceId_streamId_endTimeMs`.

### Testy migracji (C1.2)

Plik: `app/src/androidTest/java/app/vireo/data/local/VireoDatabaseMigrationTest.kt`

- `MigrationTestHelper(Instrumentation, VireoDatabase::class.java)`.
- Test 1: `createDatabase(TEST_DB, 14)` i weryfikacja obecności wszystkich 14 tabel.
- Test 2: ponowne otwarcie bazy przez `Room.databaseBuilder` — Room waliduje
  `identityHash` wyeksportowanego schematu (wykrywa rozjazd schematu i kodu).
- Zakres C1.2 obejmuje wyłącznie wersję 14 (smoke test). Ścieżki migracji
  `2 → 3` … `13 → 14` oraz przyszła `14 → 15` nie są jeszcze pokryte testami.
- Weryfikacja: CI #160 (`7d3ab08`) — 2/2 testy PASS na emulatorze API 34.

Migracja 13 → 14:

1. Dodaje kolumnę `logoUrl TEXT` do `xtream_catalog_items`.
2. Dla rekordów `contentType = 'LIVE'`, w których `posterUrl` nie jest NULL:
   - kopiuje `posterUrl` do `logoUrl`;
   - ustawia `posterUrl = NULL`.

To opis migracji zapisanej w kodzie. Niezależna walidacja migracji na rzeczywistej bazie urządzenia pozostaje do potwierdzenia.

## 6. Player — niezmienne wymagania projektowe

Poniższe zasady są obowiązkowe przy każdej zmianie HUD i odtwarzacza:

1. Warstwa wideo znajduje się pod HUD.
2. HUD wyświetla logo albo tytuł/tekst.
3. Plakat nigdy nie zastępuje logo.
4. Nie wolno fabrykować metadanych, których nie dostarcza źródło.
5. LIVE ma dwa oddzielne paski informacji.
6. HUD filmu nie może wyświetlać miniaturek odcinków.
7. Autoplay ma działać bez drugiego, konkurencyjnego mechanizmu uruchamiania.

### Reguła logo

- Jeśli logo jest dostępne, wyświetl logo.
- Jeśli logo nie jest dostępne, wyświetl zwykły tekst.
- Nigdy nie używaj plakatu jako zastępstwa logo.
- Zachowuj proporcje i czytelność oryginalnego logo.

## 7. Status etapów

Aktualny stan planowania:

- Aktywny etap implementacyjny: **C1.2** (infrastruktura testów migracji Room).
- Zatwierdzona specyfikacja zadania: C1.2, Wariant A (`androidTest` +
  `MigrationTestHelper`) i Opcja A1 w CI (emulator API 34 +
  `connectedDebugAndroidTest`) — decyzja właściciela z 2026-11-13.
- Ostatnio wykonany pakiet: **C1.2** (infrastruktura testów migracji Room),
  commity `fecb0df` i `7d3ab08`. Zrealizowany w kodzie, potwierdzony w CI #160
  (2/2 testy PASS na emulatorze API 34), **niezaakceptowany**.
- Poprzedni pakiet: **C1.1** (eksport schematu Room + publikacja artefaktu
  w CI), zrealizowany i potwierdzony w CI, niezaakceptowany.
- Migracja `14 → 15` **nie została rozpoczęta**.
- Nie rozpoczynać implementacji bez zatwierdzonej specyfikacji.
- Nie oznaczać etapów jako zakończonych bez potwierdzonych kryteriów i dowodów.

Statusy poniżej wymagają potwierdzenia na podstawie historii commitów, aktualnego kodu, testów i decyzji właściciela. Nie należy ich wywodzić wyłącznie z wyniku CI.

| Etap | W kodzie | Testy przeszły | Zaakceptowany |
|---|---|---|---|
| 2B.3A | Do potwierdzenia | Do potwierdzenia | Niepotwierdzone |
| 2B.3B | Do potwierdzenia | Do potwierdzenia | Niepotwierdzone |
| 2B.4 | Do potwierdzenia | Do potwierdzenia | Niepotwierdzone |
| 2B.5 | Do potwierdzenia | Do potwierdzenia | Niepotwierdzone |
| Stage 3 | Do potwierdzenia | Do potwierdzenia | Niepotwierdzone |
| C1.1 | Tak (`6c4d2a5`–`8d5a0a5`) | CI: unit tests PASS, schemat wygenerowany (#158) | Niepotwierdzone |
| C1.2 | Tak (`fecb0df`, `7d3ab08`) | CI #160: 2/2 testy PASS na emulatorze API 34 | Niepotwierdzone |

### Materiały referencyjne

- Claude reference ZIP, baza `ec2b35c`: materiał referencyjny, nie został automatycznie zastosowany do repozytorium.
- Snapshot GitHub z 2026-11-12 (`b21fc01`): historyczny; obecny HEAD gałęzi to `8d5a0a5`.
- `docs/audits/C1_1_ROOM_SCHEMA_AND_MIGRATION_TEST_PLAN.md`: plan pakietu C1.1
  (zrealizowany zakres + matryca testów migracji `14 → 15`).
- `docs/audits/SECOND_AUDIT_ROOM_SQLITE_CATALOG.md`: audyt historyczny na `beec446`
  (plik untracked), rekomendacja B = Room + nowy model katalogu.

## 8. CI — historia i aktualna weryfikacja

Workflow:
`.github/workflows/build.yml`

Zweryfikowane kroki:

- testy jednostkowe: `gradle testDebugUnitTest --stacktrace`
- przygotowanie stabilnego podpisywania z GitHub Secret
- build: `gradle assembleDebug --stacktrace`
- publikacja artefaktu `vireo-debug-apk`
- publikacja artefaktu `vireo-room-schemas` z `app/schemas/**/*.json`
  (`if-no-files-found: error`, retencja 30 dni)
- C1.2 (commit `fecb0df`): nowy job `android-test` —
  `reactivecircus/android-emulator-runner@v2` (API 34, `x86_64`, `default`),
  `gradle connectedDebugAndroidTest --stacktrace`, raport w artefakcie
  `vireo-android-test-report` (retencja 14 dni, `if-no-files-found: warn`).

Workflow używa Java 17 i Gradle 8.13. Uruchamia się dla `main`, `work/vireo-development` oraz `workflow_dispatch`.

### Ostatni potwierdzony sukces CI

- Workflow: #160
- Commit: `7d3ab08ac8d14291ec524d7ac270428bc7920ac0`
- Branch: `work/vireo-development`
- Conclusion: SUCCESS (oba joby: `build` i `android-test`)
- Zakończenie: 2026-11-13 12:34:07 UTC
- `android-test`: 2/2 testy `VireoDatabaseMigrationTest` przeszły na emulatorze
  `emulator-5554 - 14` (0 skipped, 0 failed), `BUILD SUCCESSFUL in 8m 52s`.
- Artefakty: `vireo-debug-apk` (14 377 418 B), `vireo-room-schemas` (3 068 B),
  `vireo-android-test-report` (5 158 B).

### C1.2 — pierwszy run z testami instrumentowanymi

- Workflow: #159
- Commit: `fecb0dfe749da577d8e99164184d18419c7f6254`
- Job `build`: SUCCESS (unit tests, podpisany APK, artefakt schematu).
- Job `android-test`: FAILURE — 2/2 testy zakończone `AbstractMethodError`:
  `kotlinx.serialization.KSerializer[] GeneratedSerializer.typeParametersSerializers()`
  (`PluginGeneratedSerialDescriptor`).
- Przyczyna: `room-migration:2.8.5` (zależność `room-testing`) wymaga
  `kotlinx-serialization-json:1.8.1`, a aplikacja deklarowała 1.6.3.
  W `androidTest` runtime serializacji był 1.8.1, kod skompilowany przeciw 1.6.3.
- Poprawka: podniesienie `kotlinx-serialization-json` do 1.8.1 (zgoda właściciela),
  commit `7d3ab08`, potwierdzona runem #160.
- Emulator API 34 wystartował poprawnie, testy zostały uruchomione — infrastruktura
  `android-test` działa; problem dotyczył wyłącznie wersji biblioteki.
- Artefakty runu: `vireo-debug-apk` (14 363 389 B), `vireo-room-schemas` (3 068 B),
  `vireo-android-test-report` (6 544 B).

Artefakty workflow #158 (potwierdzone przez `gh api`):

- `vireo-room-schemas` — 3 068 B, dostępność do 2026-12-12.
- `vireo-debug-apk` — 14 363 395 B, dostępność do 2027-02-10.

Ostrzeżenia workflow (nie błędy): deprecacja Node.js 20 w używanych akcjach,
deprecacja `actions/setup-java@v4`, zapowiedź migracji `ubuntu-latest` na Ubuntu 26.

Sukces CI nie zastępuje testów na urządzeniu ani akceptacji etapu.

### Historia workflow po `beec446`

- #155 (`beec446`) — SUCCESS.
- #156 (`6c4d2a5`) — SUCCESS; workflow nie zawierał jeszcze kroku publikacji
  schematu; jedyny artefakt `vireo-debug-apk` (14 363 400 B).
- #157 (`597d887`) — FAILURE; krok `Upload Room schema artifact` nie znalazł plików
  pod `app/schemas/*.json`. Room zapisuje schemat w podkatalogu
  `app/schemas/app.vireo.data.local.VireoDatabase/14.json`.
- #158 (`8d5a0a5`) — SUCCESS; poprawiona ścieżka `app/schemas/**/*.json`,
  artefakt `vireo-room-schemas` opublikowany.
- #159 (`fecb0df`) — FAILURE (job `android-test`); `build` SUCCESS.
  Testy instrumentowane uruchomione, ale oba zakończone `AbstractMethodError`
  z `kotlinx-serialization` (rozjazd 1.6.3 vs 1.8.1). Poprawka: wersja 1.8.1.
- #160 (`7d3ab08`) — SUCCESS; oba joby zielone, 2/2 testy migracji przeszły
  na emulatorze API 34.

Na commicie `8d5a0a5` nie ma otwartego błędu CI. Na `fecb0df` otwarty był
błąd `android-test`, naprawiony w `7d3ab08` i potwierdzony runem #160.

### Stan testów

- Pliki testowe: 39 plików `*.kt` w `app/src/test/`.
- Liczba adnotacji `@Test`: 223 (policzone lokalnie w kodzie).
- C1.2: `app/src/androidTest/` z plikiem `VireoDatabaseMigrationTest.kt`
  (2 testy) oraz `testInstrumentationRunner`.
- Testy instrumentowane: PASS w CI #160 (2/2, emulator API 34).
- Brak testów pokrywających ścieżki migracji `2 → 3` … `13 → 14`.

### Historia naprawionych błędów

- #139 — brak argumentu `LiveChannelHistoryDao` w `PlayerViewModelIntegrationTest.kt`; poprawka w #140.
- #145 i #148 — test `series HUD identifies current episode`; późniejsze uruchomienia #146 i #149 przeszły.
- #151 — błędy składni Kotlin w `HudControls.kt`; naprawione w #152.
- #154 — udany workflow na commicie `b21fc01` (historyczny): unit tests PASS,
  stable signing preparation PASS, signed APK build PASS, artifact upload PASS.
  Artefakt `vireo-debug-apk`, 14 363 386 bajtów, dostępność raportowana do 2027-02-10.
- #157 — krok publikacji schematu Room nie znalazł `app/schemas/*.json`;
  Room zapisuje plik w podkatalogu per klasa bazy. Poprawka w #158
  (`app/schemas/**/*.json`), workflow zakończony sukcesem.

## 9. Otwarte kwestie i ograniczenia

- Aktywny etap: C1.2 zaimplementowany, CI-zweryfikowany (#160), **niezaakceptowany**.
- C1.1 zrealizowany w kodzie i CI, niezaakceptowany.
- `app/schemas/app.vireo.data.local.VireoDatabase/14.json` jest w repozytorium
  (commit `fecb0df`) i stanowi źródło prawdy dla testów migracji.
  Artefakt CI `vireo-room-schemas` (ważny do 2026-12-13) nie jest już jedynym źródłem.
- C1.2 nie pokrywa ścieżek migracji `2 → 3` … `13 → 14`; wymaga osobnego zadania.
- Job CI `android-test` działa (run #160, ~17 min); emulator API 34 startuje poprawnie.
  Uwaga na czas trwania: każdy push na `main`/`work/vireo-development` uruchamia
  emulator ponownie.
- Brak migracji Room `1 → 2` — potwierdzone w kodzie (`AppModule`, lista migracji).
- `AGENTS.md` jest lokalny i nie jest wersjonowany (role zaktualizowane 2026-11-13).
- `.opencode-permission-probe.txt` wymaga identyfikacji.
- `docs/audits/SECOND_AUDIT_ROOM_SQLITE_CATALOG.md` jest w repozytorium od `3555f05`.
- Status historycznych etapów wymaga formalnego przeglądu.
- Nie potwierdzono walidacji migracji Room 13 → 14 na rzeczywistych danych urządzenia.
- Testy na fizycznym urządzeniu wymagają osobnego potwierdzenia.

Nie wpisywać nierozstrzygniętych kwestii jako błędów aplikacji bez dowodów.

## 10. Ograniczenia techniczne

Bez osobnej zgody użytkownika nie wolno:

- dodawać FFmpeg;
- zmieniać podpisywania aplikacji;
- modyfikować workflow GitHub Actions;
- zmieniać wersji Gradle, AGP, Kotlin ani Media3;
- zmieniać sekretów GitHub;
- wypychać zmian na `main` ani na gałąź inną niż bieżąca robocza;
- wykonywać merge, rebase lub amend, tworzyć tagi ani synchronizować gałęzi;
- uruchamiać lokalnych buildów Gradle w Termux.

Commit i push na bieżącą gałąź roboczą wykonuje OpenCode w ramach
zatwierdzonego zadania. Zakazane są `reset --hard`, `clean`, force push,
force checkout i modyfikowanie `.git`.

ARVIO może służyć jako techniczny punkt odniesienia, ale nie jako źródło do kopiowania implementacji.

### Zmiany konfiguracji już wykonane (kontekst)

W commitach `6c4d2a5`–`8d5a0a5`, na podstawie wcześniejszej zgody użytkownika,
zmieniono konfigurację chronioną:

- `app/build.gradle.kts`: `ksp { arg("room.schemaLocation", "$projectDir/schemas") }`
  oraz `androidTestImplementation("androidx.room:room-testing:2.8.5")`;
- `.github/workflows/build.yml`: krok publikacji artefaktu `vireo-room-schemas`;
- `app/src/main/java/app/vireo/data/local/AccountDb.kt`: `exportSchema = true`.

W C1.2 (commity `fecb0df` i `7d3ab08`, zgoda właściciela z 2026-11-13) dodatkowo:

- `app/build.gradle.kts`:
  `testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"`,
  source set `androidTest` z `assets.srcDir("$projectDir/schemas")`,
  `androidTestImplementation("androidx.test:runner:1.6.2")`,
  `androidTestImplementation("androidx.test.ext:junit:1.2.1")`;
- `.github/workflows/build.yml`: job `android-test` (emulator API 34, Opcja A1);
- `kotlinx-serialization-json` 1.6.3 → 1.8.1 (wymóg `room-migration:2.8.5`);
- nowe pliki: `app/schemas/.../14.json`, `VireoDatabaseMigrationTest.kt`.

Zmiany te nie zmieniły wersji Gradle, AGP, Kotlin, KSP, Media3 ani wersji bazy danych.
Dalsze zmiany w tych obszarach nadal wymagają osobnej zgody.

## 11. Kryteria zamknięcia etapu

Etap może zostać zamknięty dopiero po:

1. wdrożeniu zatwierdzonego zakresu;
2. przeglądzie pełnego diffu;
3. przejściu wymaganych testów CI;
4. wykonaniu i odnotowaniu wymaganych testów fizycznego urządzenia;
5. zachowaniu wszystkich Player invariants;
6. aktualizacji tego dokumentu w ramach tego samego etapu;
7. formalnej akceptacji użytkownika.

**Ten dokument opisuje stan zweryfikowany na 2026-11-13. Każda kolejna zmiana projektu wymaga aktualizacji odpowiednich sekcji.**
