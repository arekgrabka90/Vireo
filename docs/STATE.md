# VIREO — STATE

**Stan dokumentu:** 2026-11-17 · **Gałąź:** `work/vireo-development` · **Baza audytu:** zdalny HEAD `2db5db3`; zmiany po audycie (E1.1–E1.3) opisane w sekcjach 5.9, 5.10, 9 i 11

Ten dokument w całości zastępuje poprzedni `docs/STATE.md` (stan z 2026-11-13). Jest punktem wejścia dla kolejnych sesji. **Kod wygrywa nad dokumentacją.**

Audyt był statyczny (odczyt zdalny przez GitHub). **Nie uruchomiono** buildu, testów ani aplikacji i nie sprawdzono urządzeń. Znaczniki dowodów: **[K]** potwierdzone w kodzie, **[A2]** z audytu `beec446`, niezweryfikowane ponownie, **[H]** hipoteza, **[U]** wymaga testu na urządzeniu.

## 1. Produkt

VIREO to autorski, premium klient IPTV/VOD na Android TV i telefony (Compose). Priorytety: stabilność, cinematic design, sterowanie pilotem i dotykiem, rozbudowany LIVE TV z EPG i odtwarzaczem, filmy/seriale/odcinki, wiele profili i źródeł, niezależne biblioteki i ustawienia profili. Nie jest kopią ARVIO, Netflixa ani TiviMate (to tylko inspiracje funkcjonalne). Plan do wydania 1.0: `ROADMAP.md`.

## 2. Repozytorium i praca

- Repozytorium: `arekgrabka90/Vireo`; gałąź robocza: `work/vireo-development`.
- Zawartość katalogu głównego [K]: `.github/`, `app/`, `docs/`, `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`, `setup.sh`, `README.md`, `README.pl.md`, `README.es.md`, `VIREO_Premium_Player_HUD_Prompt.md` (80 KB), `.gitignore`. **Brak `gradlew` i `gradle/wrapper/`** [K].
- Kandydaci do porządków poza `docs/` (wymagają zgody): `reports/repo-cleanup-candidates.md`.
- Role i zasady pracy: `decisions/0001-roles-and-workflow.md`. W skrócie: właściciel decyduje i kontroluje CI; Claude projektuje, implementuje i aktualizuje dokumentację; commity i push na `work/vireo-development` zostały Claude'owi upoważnione 2026-11-14 (patrz uwaga o uprawnieniach konektora w `reports/2026-11-14-baseline-audit.md`). Nie wolno bez osobnej zgody zmieniać: `applicationId`, podpisywania i keystore, workflowów CI, wersji Gradle/AGP/Kotlin/compileSdk/Media3 1.11.1, istniejących zależności, ani dodawać zależności; nie wykonywać migracji grożących utratą danych; nie czytać ani nie zapisywać sekretów. Lokalny Gradle nie jest uruchamiany.
- Poza repozytorium (lokalnie u właściciela, poprzednia dokumentacja): `AGENTS.md` i `.opencode-permission-probe.txt`. Ich zawartości nie widzę; role w `AGENTS.md` mogą być nieaktualne [H].

## 3. CI

Plik `.github/workflows/build.yml` [K]. Wyzwalacze: `push` na `main` i `work/vireo-development` oraz `workflow_dispatch`. **Każdy push na gałąź roboczą uruchamia workflow automatycznie.** Joby:

- `build`: Java 17 (Temurin), Gradle 8.13 przez `gradle/actions/setup-gradle`, `gradle testDebugUnitTest`, przygotowanie klucza podpisu z sekretu, `gradle assembleDebug`, artefakty `vireo-debug-apk` i `vireo-room-schemas`.
- `android-test`: emulator API 34 (`reactivecircus/android-emulator-runner@v2`), `gradle connectedDebugAndroidTest`, raport w artefakcie `vireo-android-test-report`. Trwa ok. 17 min.

Nie ma: builda release, R8, lint, walidacji wrappera. Commit `2a7fb26` dodaje regułę udev nadającą dostęp do `/dev/kvm` przed uruchomieniem emulatora oraz zmienia opcję emulatora z `-no-snapshot-save` na `-no-snapshot`. Run #163 (`43604212356`) zakończył się sukcesem: joby `build` i `android-test` mają status SUCCESS. Liczba wykonanych testów nie została niezależnie potwierdzona raportem.

## 4. Konfiguracja techniczna [K]

| Element | Wartość |
|---|---|
| Moduły Gradle | jeden: `:app` |
| AGP / Kotlin / KSP / Hilt (plugin) | 8.13.2 / 2.3.20 / 2.3.10 / 2.58 |
| Gradle (CI) | 8.13 |
| compileSdk / minSdk / targetSdk | 36 / 24 / 34 |
| JVM | 17 |
| Media3 | exoplayer, ui, exoplayer-hls: 1.11.1 (brak `session`, `dash`, `datasource-okhttp`) |
| Room | 2.8.5 (KSP; `exportSchema = true`, schematy w `app/schemas`) |
| Hilt | 2.58 (kompilator przez `kapt`) |
| Compose | BOM 2024.06.00, `tv-material` 1.0.0, Navigation Compose 2.7.7 |
| Sieć / dane | OkHttp 4.12.0, kotlinx-serialization-json 1.8.1, coroutines 1.8.1, Coil 2.6.0 |
| Testy | JUnit 4.13.2, coroutines-test, Mockito 5.14.2; `androidTest`: room-testing 2.8.5, androidx.test runner 1.6.2, ext-junit 1.2.1 |
| Release | `isMinifyEnabled = false`; podpis przez zmienne środowiskowe (opcjonalny) |
| Wersja aplikacji | `versionName 0.1.0`, `versionCode = 1000 + numer runu CI` |
| Klucz TMDB | `BuildConfig.TMDB_API_KEY` z właściwości lub zmiennej środowiskowej (nie czytano wartości) |
| Manifest | INTERNET; `leanback` i `touchscreen` nieobowiązkowe; baner i `LEANBACK_LAUNCHER`; `allowBackup=false`; `usesCleartextTraffic=true`; brak `ACCESS_NETWORK_STATE`, usług, PiP |

Nie zmieniać tych wartości bez osobnej zgody. `kotlinx-serialization-json` podniesiono 1.6.3 → 1.8.1 w C1.2 (wymóg `room-migration:2.8.5`).

## 5. Architektura [K, o ile nie zaznaczono]

Kod: `app/src/main/java/app/vireo/` (`data/`, `domain/`, `di/`, `player/`, `ui/`), testy: `app/src/test/java/app/vireo/` (39 plików, 223 `@Test` wg poprzedniego audytu [A2]) i `app/src/androidTest/` (`VireoDatabaseMigrationTest`).

### 5.1 Nawigacja

- Zewnętrzny `NavHost` (`MainActivity`): `splash`, `profiles`, `profile_edit/{id}`, `main` (zawiera `MainShell`), `list/{type}/{categoryId}/{name}`, `detail/...`, `tmdb_catalog/{type}`, `tmdb_detail/...`, `player`. Odtwarzacz to osobna trasa najwyższego poziomu, a jego `PlayerViewModel` jest przypisany do wpisu trasy `player`.
- `MainShell` ma wewnętrzny `NavHost`: `home`, `live`, `search`, `mylist`, `recordings` (placeholder), `more`, `settings`, `settings/sources`, `settings/playback|appearance|epg|parental|about` (placeholdery).
- Układ powłoki: telefon <600 dp — dolny pasek i nagłówek; TV i tablet — górny pasek nawigacji (treść poza `home` ma stały odstęp 70 dp od góry). Aktywność ukrywa paski systemowe (`BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE`), edge-to-edge.
- Nawigacja ma zahardkodowane napisy po polsku (np. etykiety dolnego paska).

### 5.2 Dane lokalne (Room)

- `VireoDatabase` (`data/local/AccountDb.kt`): wersja 14, 14 encji: konta, profile, ustawienia profilu, ustawienia aplikacji, biblioteka, historia kanałów LIVE, ostatnie wyszukiwania, kategorie/elementy/stan synchronizacji katalogu Xtream, indeks treści Xtream, stan synchronizacji indeksu, programy EPG, stan synchronizacji EPG.
- Migracje zarejestrowane `2→3` … `13→14` (`di/AppModule.kt`); **brak `1→2`** i brak `fallbackToDestructiveMigration`. Decyzja P14 (2026-11-15): minimalna wspierana wersja bazy to 14; starsze bazy trafiają na ekran odzyskiwania bez resetu (sekcja 5.9). W `app/schemas` jest wyłącznie `14.json`, więc ścieżki `3→14` nie są testowalne przez `MigrationTestHelper` i nie są deklarowane jako przetestowane. Fabryka otwierania bazy jest opakowana tak, że uszkodzona baza nie jest usuwana automatycznie (E1.2c).
- `library_items`: klucz `(profileId, contentKey)`, **bez kluczy obcych**; `live_channel_history`: FK do profili, `sourceId` bez FK; pozostałe tabele katalogu: FK do `accounts` z kaskadą [A2 dla FK, K dla `library_items`].
- Hasła kont: AES-GCM w Android Keystore; PIN profili: PBKDF2 [K]. `server` i `username` przechowywane jawnie [A2].

### 5.3 Xtream, katalog i indeks

- `XtreamProvider` (`data/xtream`): zapytania `player_api.php`; **cała odpowiedź czytana do `String`, parsowana do drzewa `JsonElement`**, `strictObjects` — jeden niebędący obiektem element odrzuca całą listę. Adres LIVE budowany zawsze jako `.m3u8`. Poświadczenia w adresach URL.
- `xtream_catalog_items` (katalog **aktywnego** konta; LIVE, MOVIE, SERIES; przeglądanie po kategoriach) i `xtream_content_index` (indeks **wszystkich** kont; MOVIE i SERIES; dopasowanie TMDB). Różne role, ale te same dane i ten sam payload pobierany dwukrotnie. Synchronizacja: stale-while-revalidate, TTL 6 h, pełny delete+insert w transakcji, ochrona przed pustą odpowiedzią tylko w katalogu. Pierwsze wejście blokuje UI do końca synchronizacji.
- `DAO.getItems` zwraca całą kategorię bez `LIMIT`; indeks używa `LIKE '%token%'` z `LIMIT 1000`.

### 5.4 EPG

- `EpgRepository`: per kanał, `get_short_epg` (limit 50), cache w Room z TTL, tylko aktywne źródło. Brak parsera XMLTV, brak zbiorczego pobierania, brak siatki.

### 5.5 TMDB i dopasowanie

- Klient TMDB (`data/tmdb`): katalog (popularne/trendy), wyszukiwanie multi, discover z gatunkami, szczegóły, sezony i odcinki. Klucz jako `api_key` w URL; cache tylko w pamięci [A2]; wyszukiwanie wyłącznie przez TMDB [A2].
- `TmdbXtreamMatcher` (czysta logika, testowana): ID zewnętrzne, potem tytuł i rok ±1; statusy `EXACT_*`, `NEEDS_REVIEW`. Dopasowania nie są utrwalane; `TmdbXtreamMatchRepository` nie jest używany produkcyjnie [A2].
- Ścieżka VOD „TMDB-first”: szczegóły TMDB → dostępność u dostawców (indeks) → odcinki z wybranego źródła → odtwarzanie z alternatywami (failover między źródłami). Równolegle istnieje przeglądanie po kategoriach dostawcy (`BrowseViewModel`, `TitleListScreen`, `DetailScreen`).

### 5.6 Profile, ustawienia, biblioteka

- Profile z opcjonalnym PIN-em. E1.1: limit prób PIN (5 błędnych prób, potem blokada 30 s z podwajaniem do 15 min, licznik per profil w `SharedPreferences`) oraz wymóg PIN-u przed wejściem do edycji zabezpieczonego profilu w trybie zarządzania. To zabezpieczenie na poziomie UI (`ProfileEditViewModel.save/delete` nie weryfikują PIN-u); pełne egzekwowanie w logice jest zakresem E6.
- Flaga profilu dziecięcego jest zapisywana i przekazywana do fabryki ustawień, **która jej nie używa** — brak filtrowania treści [K].
- Ustawienia per profil: część zapisana, ale niekonsumowana (m.in. `liveTvStartOnLastChannel`, motyw, akcent) [A2].
- Biblioteka: klucz `contentKey` (`tmdb:TYPE:ID...`), moja lista, obejrzane, kontynuacja. E1.1: usunięcie profilu sprząta `library_items` w jednej transakcji (`ProfileDao.deleteWithData`; tabela nie ma FK do profilu); pozostałe tabele profilu usuwa kaskada FK (sprawdza to test instrumentowany). Źródła (konta) są globalne, jedno aktywne w całej aplikacji [K].

### 5.7 Odtwarzacz

- `PlayerEngine` (kontrakt niezależny od Media3) i `Media3PlayerEngine` (`@ViewModelScoped`): **ExoPlayer tworzony w inicjalizatorze silnika**, bez `AudioAttributes`, bez `LoadControl`, bez konfiguracji user-agenta; zwalniany razem z `PlayerViewModel`. `PlayerScreen` tworzy `PlayerView` w `AndroidView` i podpina ten sam obiekt gracza; przy zniszczeniu ekranu pauzuje i zapisuje postęp [K].
- `PlayerViewModel` korzysta bezpośrednio z silnika i singletona `PlaybackSession` (kolejka, alternatywy źródeł). `PlayerController`, `session/PlaybackQueue`, `PlaybackSessionState` nie są używane przez `PlayerViewModel` ani `PlayerScreen` (odczyt) i według audytu A2 mają referencje tylko w testach — **prawdopodobny martwy kod** [K/A2].
- Recovery: klasyfikacja błędów, 2 ponowienia, potem failover do alternatywnego źródła; osłony przed spóźnionymi callbackami; komunikaty błędów oczyszczane z adresów [K]. Brak watchdoga zawieszonego buforowania i nasłuchu sieci.
- Cykl życia: `ON_STOP` zapisuje postęp i pauzuje, `ON_START` wznawia, jeśli użytkownik chciał odtwarzać. Start odtwarzania w `LaunchedEffect` kluczowanym `url`, `contentKey`, `retryToken`, `settingsReady` — po odtworzeniu aktywności możliwy ponowny `startPlayback` [H][U]. Orientacja wymuszona na poziomą w odtwarzaczu (także na telefonie).
- HUD (`player/hud/*`): transport, ±10 s, kafelki odcinków i ostatnich kanałów, odliczanie do następnego odcinka, panel „teraz/następny” dla bieżącego kanału (to nie jest siatka). Brak: wyboru ścieżek audio/napisów/jakości, prędkości, proporcji, audio focus, MediaSession, PiP.
- Preferencje języków audio i napisów, włączenie i rozmiar napisów są stosowane z ustawień profilu. Niezmienniki HUD: `specs/player-invariants.md`.

### 5.8 Interfejs

Ekrany: splash, wybór i edycja profilu, Home (rzędy TMDB), katalog TMDB, szczegóły TMDB (45 KB, główny ekran VOD), Search, Discovery, biblioteka, ustawienia (30 KB), źródła, **LIVE TV = tylko lista kategorii** (`ui/live/LiveTvScreen.kt`), lista tytułów/kanałów kategorii (`TitleListScreen`). LIVE: kolejka odtwarzania ma jeden element, więc nie ma zappingu; brak ulubionych kanałów.

### 5.9 Odzyskiwanie bazy danych (E1.2) [K]

- **Kontrola przy starcie** (`ui/recovery/StartupGate`, `StartupViewModel`): zanim aplikacja dotknie Room, odczytywany jest `user_version` pliku bazy przez `SqliteDatabaseInspector` (własny `DatabaseErrorHandler`, który nie usuwa uszkodzonego pliku). `DatabaseClassifier` ocenia stan względem `DatabasePolicy` (`MIN_SUPPORTED_VERSION = 14`, `CURRENT_VERSION = 14`; wartość musi równać się `@Database(version)`, pilnuje tego test instrumentowany).
- **Kopia przed migracją:** dla bazy wspieranej, lecz starszej niż aplikacja, `DatabaseFiles.createBackup` kopiuje plik bazy i `-wal`/`-shm` do `files/db_backups/vireo-v{wersja}-{czas}/`; kopia jest sprawdzana `PRAGMA quick_check`, dopiero potem przycinane są stare (zostają 2). Przy braku miejsca lub nieudanej kopii ekran oferuje „Kontynuuj bez kopii”. Dziś zakres jest pusty (MIN = CURRENT = 14); pierwszy raz zadziała przy migracji `14→15`.
- **Ekran odzyskiwania** (`DatabaseRecoveryScreen`): ponów, kontynuuj bez kopii, eksport (ZIP przez wybór pliku; zapas: katalog aplikacji), przywróć najnowszą kopię przechodzącą kontrolę spójności, odsuń bazę i zacznij od nowa (po potwierdzeniu). Odsunięcie przenosi pliki do `db_backups/aside-*` (nie usuwa); katalogi `aside-*` nie są czyszczone automatycznie. Po przywróceniu i odsunięciu aplikacja restartuje się (`MainActivity.restartApp`; zachowanie na urządzeniu do potwierdzenia [U]).
- **Brak automatycznego usuwania:** `NonDestructiveOpenHelperFactory` (E1.2c) jest podpięta w `AppModule`; `onCorruption` niczego nie usuwa (domyślna implementacja `SupportSQLiteOpenHelper.Callback` usuwa plik bazy). Test instrumentowany odtwarza uszkodzenie, sprawdza, że plik zostaje, a test kontrolny, że domyślna fabryka by go usunęła.
- **Licznik niestabilnych startów** (E1.2d, `StartupCrashGuard`): trzy kolejne starty bez oznaczenia stabilności (normalne wyjście `ON_STOP` albo 15 s pracy) powodują, że czwarty start pokazuje ekran odzyskiwania (`REPEATED_CRASHES`). Ogranicza ryzyko pętli awarii przy uszkodzeniu stron bazy z poprawnym nagłówkiem, które po E1.2c nie kończy się już usunięciem pliku.
- **Poza zakresem:** czyszczenie katalogów `aside-*`, teksty ekranu zapisane na sztywno po polsku (E7.2).

### 5.10 Utrata klucza Keystore (E1.3) [K]

- `CryptoManager.decrypt` używa `secretKey(createIfMissing = false)`, a `encrypt` tworzy brakujący klucz. Po utracie klucza i zapisaniu nowego hasła powstaje nowy klucz, a stare szyfrogramy kończą się błędem tagu GCM. Dlatego za „hasło nieczytelne” uznajemy każdą awarię odszyfrowania (`CryptoManager.canDecrypt`), a nie tylko brak klucza.
- `ProviderRegistry.create` mapuje awarię odszyfrowania na `CredentialUnavailableException` (podklasa `IllegalStateException`, komunikat po polsku). Metody `ContentRepository` zwracające `Result` (`movieDetails`, `seriesDetails`) zwracają porażkę zamiast rzucać; `liveUrl` zwraca `null`. Synchronizacja katalogu i indeksu już łapała błędy `providers.create` per konto. `ContentRepository.provider()` oraz inne miejsca wywołujące `ProviderRegistry.create` (m.in. EPG, dostępność TMDB) nie były przeglądane pod tym kątem [H].
- Ekran Źródła pokazuje dla aktywnego konta z nieczytelnym hasłem komunikat i pole „Nowe hasło do źródła”. `ContentRepository.reauthenticate` sprawdza hasło logowaniem do serwera Xtream i zapisuje zaszyfrowane hasło tego samego konta (`AccountDao.updatePassword`); zapis jest uznawany za udany tylko po aktualizacji dokładnie jednego rekordu. Konto, katalog, biblioteka i profile pozostają nietknięte. Ekran Źródła pokazuje tylko konto aktywne, więc nieaktywne konta z utraconym kluczem nie mają jeszcze ścieżki naprawy. Teksty na sztywno po polsku (E7.2).
- PIN profili (PBKDF2) nie zależy od Keystore (test instrumentowany).
- Testy: `ProviderRegistryTest` (3), `CryptoManagerKeyLossTest` (3, osobny alias klucza, nie dotyka klucza aplikacji), `AccountPasswordUpdateTest` (2). Wyniki CI do uzupełnienia.

## 6. Matryca funkcji

| Obszar | Stan |
|---|---|
| Konta Xtream, logowanie, szyfrowanie hasła | działa (kod) |
| Synchronizacja katalogu i indeksu | działa, nieoptymalna (sekcja 5.3) |
| VOD TMDB-first, dopasowanie, alternatywy źródeł | działa (kod), niezweryfikowane na urządzeniu |
| Seriale: sezony, odcinki, następny odcinek | działa (kod) |
| Biblioteka, kontynuacja, obejrzane | działa (kod); „Kontynuuj” prawdopodobnie otwiera szczegóły [H] |
| Profile, PIN, ustawienia per profil | działa częściowo (PIN bez limitu, brak profilu z ograniczeniami) |
| LIVE TV | częściowe: kategorie i lista kanałów, odtwarzanie, historia, panel teraz/następny |
| EPG | tylko warstwa danych per kanał |
| Siatka EPG / TV Guide, LIVE Hub, panel kategorii LIVE | **brak** (specyfikacja: `specs/live-tv-hub-and-guide.md`) |
| Odtwarzacz: failover, retry, wznowienie, język audio/napisów | działa (kod) |
| Odtwarzacz: wybór ścieżek, audio focus, tło, MediaSession, PiP | **brak** |
| Nagrania, ustawienia EPG/odtwarzania/wyglądu/kontroli rodzicielskiej | placeholdery |
| Lokalne wyszukiwanie katalogu | brak (wyszukiwanie przez TMDB) |
| Release z R8, lint, crash reporting, polityka prywatności | brak |

## 7. Znane problemy (priorytet)

1. ~~P1 — awaria nawigacji~~ **naprawione (E1.1):** wpis `guide` w `MoreScreen` kieruje do `live`; przewodnik (siatka EPG) powstanie w E4.
2. **P1 — migracje:** brak `1→2` i testów ścieżek `3→13`; decyzja P14 (v14 minimalna). Baza w nieobsługiwanej wersji nie powoduje już crasha ani resetu: ekran odzyskiwania (E1.2b). Pierwszą migracją z kopią przedmigracyjną będzie `14→15` (E4.2).
3. ~~P1 — integralność~~ **naprawione (E1.1):** sprzątanie `library_items` przy usuwaniu profilu.
4. **P1 — synchronizacja:** podwójne pobranie, pełny delete+insert, parsowanie całej odpowiedzi w pamięci, `strictObjects`, zapytania bez `LIMIT`, `LIKE '%…%'` [K].
5. **P1 — bezpieczeństwo dostępu:** limit prób PIN i bramka PIN przed edycją profilu wdrożone (E1.1, na poziomie UI); profil z ograniczeniami nadal bez egzekwowanych ograniczeń [K].
6. **P1 — bezpieczeństwo sieci:** cleartext globalnie, poświadczenia w URL-ach (wymaga polityki, a nie ślepego wyłączenia — wielu dostawców IPTV używa HTTP) [K].
7. **P2 — LIVE:** brak zappingu, ulubionych, hosta odtwarzania i siatki EPG; adres LIVE tylko `.m3u8` [K].
8. **P2 — odtwarzacz:** brak audio focus, wyboru ścieżek, obsługi tła w dłuższej przerwie; napis `Retry` po angielsku [K].
9. **P2 — ustawienia:** zapisane, lecz niekonsumowane, m.in. `liveTvStartOnLastChannel` [A2]; placeholdery ustawień [K].
10. **P2 — martwy kod:** `PlayerController` i `session/*`, `TmdbXtreamMatchRepository`, potencjalnie `BrowseScreen` [K/A2].
11. **P2 — cache i wyszukiwanie:** cache TMDB tylko w pamięci, dopasowania nieutrwalane, wyszukiwanie tylko TMDB [A2].
12. **P2 — klucz TMDB** w kliencie jako `api_key` w URL (patrz `decisions/PENDING.md`) [K].
13. **P2 — CI:** tylko debug, bez lint i release; `android-test` uruchamia się przy każdym pushu [K].

Nie wpisywać hipotez [H] jako błędów bez dowodów.

## 8. Testy

- Jednostkowe: 223 testy w 39 plikach wg audytu bazowego [A2]; dodane w E1.1–E1.3: `PinAttemptPolicyTest` (9), `DatabaseClassifierTest` (13), `DatabaseFilesTest` (11), `StartupCrashGuardTest` (6), `ProviderRegistryTest` (3). Głównie polityki i logika czysta; brak testów repozytoriów na bazie i interfejsu.
- Instrumentowane: `VireoDatabaseMigrationTest` (2, tylko wersja 14) oraz dodane w E1.1–E1.3: `ProfileDeletionCleanupTest` (3), `DatabaseInspectorTest` (9), `NonDestructiveOpenHelperTest` (4), `CryptoManagerKeyLossTest` (3), `AccountPasswordUpdateTest` (2). Run #168: 18/18 (bez testów E1.3). Ścieżki `2→3` … `13→14` nie są pokryte.
- W tym audycie niczego nie uruchamiano.
- Wyniki CI: E1.1 run #164 SUCCESS; E1.2a (`3d2a195`) i E1.2b (`9d65719`) oba joby SUCCESS; E1.2c/d (`fa92f3b`) run #168 SUCCESS (`build` i `android-test`, 18/18 testów instrumentowanych), wg raportu właściciela. Wyniki E1.3: do uzupełnienia po CI.

## 9. Historia etapów (bez przenoszenia starych założeń)

- Etapy `2B.3A`, `2B.3B`, `2B.4`, `2B.5`, `Stage 3`: formalny przegląd w E0 (W kodzie / Testy / Zaakceptowany: do potwierdzenia).
- `C1.1` (eksport schematu Room, artefakt CI) i `C1.2` (infrastruktura testów migracji, CI run #160) — zrealizowane w kodzie i CI, niezaakceptowane przez właściciela. Szczegóły: `reports/2026-11-room-migration-infra-c1.md`.
- `E1.1` (dwa commity: poprawka nawigacji i funkcja profili; HEAD po nich `f89ba7c`): limit prób PIN, bramka PIN przy edycji, sprzątanie `library_items`, poprawka trasy `guide`. CI run #164 SUCCESS.
- `E1.2a` (`3d2a195`): klasyfikator stanu bazy i operacje na plikach (kopia, odsunięcie, przywracanie). `E1.2b` (`9d65719`): kontrola przy starcie, ekran odzyskiwania, wpięcie w `MainActivity`. CI SUCCESS.
- `E1.2c/d` (do `fa92f3b`, CI run #168 SUCCESS): nieusuwająca fabryka otwierania bazy, licznik niestabilnych startów, kontrola spójności kopii. `E1.3`: zmiany przygotowane lokalnie po `fa92f3b`, bez commita i bez wyniku CI; poprawki końcowe oczekują na weryfikację.
- Dotychczasowy plan `14→15` (przypisywanie źródeł do profili, źródło domyślne profilu) nie został rozpoczęty; w nowym planie jest decyzją otwartą (`decisions/PENDING.md`, P12).

## 10. Dokumentacja

- Plan: `ROADMAP.md`. Specyfikacje: `specs/`. Decyzje: `decisions/` (zatwierdzone) i `decisions/PENDING.md` (otwarte). Raporty: `reports/`.

## 11. Punkt wejścia dla kolejnej sesji

1. Sprawdź gałąź, HEAD i stan repozytorium; porównaj z `Baza audytu` na górze.
2. Aktualny etap: **E1 — dane i dostęp: E1.1 i E1.2 (a–d): w kodzie i CI; E1.3: zmiany lokalne, bez commita i bez CI. Status testów E1.3 i akceptacja właściciela: oczekują. Przed zamknięciem E1: ręczna weryfikacja na urządzeniu (restart po przywróceniu lub odsunięciu, eksport na TV, brak ekranu odzyskiwania przy zdrowej bazie), przegląd cleartextu i redakcji logów (P15), build release z R8 (wymaga zgody na zmianę CI).** Następne: **E2 i E3** (równolegle).
3. Przed zmianami: przeczytaj tylko potrzebne pliki kodu, nie powtarzaj audytu całego repozytorium.
4. Po etapie: zaktualizuj ten dokument i `ROADMAP.md`, dodaj raport, commit i push na `work/vireo-development`. Workflow CI uruchamia się automatycznie po pushu; wyniki sprawdza właściciel.
