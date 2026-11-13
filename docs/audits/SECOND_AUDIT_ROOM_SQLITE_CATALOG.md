# Audyt VIREO — Room/SQLite, katalog lokalny i trwały cache metadanych

**Charakter pracy:** audyt statyczny, read-only. Nie zmieniłem żadnego istniejącego pliku, nie wykonałem żadnej operacji Git zapisującej stan, nie uruchamiałem Gradle ani testów. Poniższe ustalenia oddzielają **fakty z kodu** od **rekomendacji** i **hipotez**. Jedyną zmianą wykonaną w ramach tego zadania jest utworzenie tego pliku raportu.

**Legenda statusów:** `POTWIERDZONE` — potwierdzone w kodzie; `CZĘŚCIOWO POTWIERDZONE` — część tezy prawdziwa, część nie; `NIEPOTWIERDZONE` — brak dowodów; `FAŁSZYWY ALARM` — teza nieprawdziwa; `PYTANIE DO WŁAŚCICIELA` — wymaga decyzji, nie da się rozstrzygnąć kodem.

> ## Addendum 2026-11-13 — co zmieniło się po tym audycie
>
> Ten raport opisuje stan na commicie `beec446` i pozostaje **historyczny**.
> Poniżej wyłącznie fakty późniejsze; ustaleń audytu nie zmieniono.
>
> - HEAD gałęzi `work/vireo-development` to obecnie `8d5a0a5`
>   (commity `6c4d2a5`, `597d887`, `8d5a0a5`), lokalnie i zdalnie zgodne.
> - `exportSchema` zmieniono z `false` na `true` (`6c4d2a5`), więc ustalenia
>   z sekcji 2 poz. 1, 3.1 i 3.7 dotyczące `exportSchema` są nieaktualne.
> - Dodano `ksp { arg("room.schemaLocation", "$projectDir/schemas") }`
>   oraz `androidTestImplementation("androidx.room:room-testing:2.8.5")`.
> - Dodano krok CI publikujący artefakt `vireo-room-schemas`
>   (`app/schemas/**/*.json`, `if-no-files-found: error`).
> - Workflow #157 (`597d887`) zakończył się FAILURE z powodu złej ścieżki
>   artefaktu; #158 (`8d5a0a5`) zakończył się SUCCESS i opublikował schemat v14
>   (`app/schemas/app.vireo.data.local.VireoDatabase/14.json`,
>   `identityHash 410131225083a186b43291867168bf6e`, 14 encji).
> - Nadal brak `app/src/androidTest/`, `testInstrumentationRunner` i
>   `MigrationTestHelper` — dodana zależność `room-testing` nie jest używana.
> - Nadal brak migracji `1 → 2`; wersja bazy to nadal `14`.
> - `docs/STATE.md` został zaktualizowany do stanu `8d5a0a5` i nie jest już
>   rozbieżny z HEAD (pozostaje niezacommitowany).

---

## 1. Stan repozytorium i założenia audytu

**Fakty:**

- Branch: `work/vireo-development`
- HEAD lokalny: `beec44685dfe139c5a0acd8587a3af235a4dc99a`
- HEAD zdalny `origin/work/vireo-development`: `beec44685dfe139c5a0acd8587a3af235a4dc99a`
- `git status --porcelain` przed utworzeniem raportu:
  - `M docs/STATE.md`
  - `?? .opencode-permission-probe.txt`
  - `?? AGENTS.md`
- Brak stage’a/pliku specyfikacji w `docs/` poza `docs/STATE.md`. Audyt nie jest zgodą na implementację.
- `docs/STATE.md` deklaruje w linii 19: „Zmiany tracked unstaged: brak”, podczas gdy `docs/STATE.md` jest lokalnie zmodyfikowany (`M`). To rozbieżność dokumentacji ze stanem roboczym; nie ruszałem tego pliku.
- `AGENTS.md` i `.opencode-permission-probe.txt` to pliki użytkownika; nie były przedmiotem edycji.

**Zakres:** warstwa Room/SQLite, model katalogu LIVE/VOD/serie, synchronizacja katalogu, trwałość metadanych TMDB i dopasowań, wydajność zapytań, migracje, spójność danych, bezpieczeństwo przechowywania.

**Metoda:** wyłącznie statyczne czytanie źródeł, konfiguracji Gradle, manifestu, testów i dokumentacji. Brak profilowania runtime, pomiarów rozmiaru bazy, testów na urządzeniu i uruchomień CI.

**Wersje z dokumentacji:** Room `2.8.5` (`docs/STATE.md:189`), baza `VireoDatabase` w wersji `14` (`AccountDb.kt:117`). Wersje z dokumentacji nie były przeze mnie zmieniane ani weryfikowane buildem.

---

## 2. Weryfikacja poprzednich ustaleń

| # | Ustalenie z poprzedniego audytu | Status | Dowód | Pewność |
|---|---|---|---|---|
| 1 | Baza ma 12 migracji, wersja 14, `exportSchema=false` | POTWIERDZONE | `AccountDb.kt:117-118`; migracje `AccountDb.kt:133-518`; `AppModule.kt:64-77` | wysoka |
| 2 | Brak migracji 1→2 | POTWIERDZONE (nowe ustalenie) | `AppModule.kt:64-77`; historia Git: `version = 1` w commitach `8bc5934`, `dfb41d3`, `eed7cfd` | wysoka |
| 3 | Dwa nakładające się katalogi: `xtream_catalog_items` i `xtream_content_index` | POTWIERDZONE | `XtreamCatalogDb.kt:35-66`; `XtreamContentIndexDb.kt:12-42` | wysoka |
| 4 | Pełny `DELETE` + `INSERT` przy każdej synchronizacji | POTWIERDZONE | `XtreamCatalogDb.kt:171-194`; `XtreamContentIndexDb.kt:162-183` | wysoka |
| 5 | `LibraryItemEntity` bez FK do profili → osierocone rekordy | POTWIERDZONE | `LibraryDb.kt:11-31`; migracja `AccountDb.kt:197-226`; `ProfileRepository.kt:130-147` | wysoka |
| 6 | `getForSource(...).size` zamiast `COUNT(*)` | POTWIERDZONE | `XtreamContentIndexRepository.kt:119-121` | wysoka |
| 7 | `findCandidates` używa `LIKE '%token%'` z `LIMIT 1000` | POTWIERDZONE | `XtreamContentIndexDb.kt:103-114`; `XtreamContentIndexRepository.kt:275-283` | wysoka |
| 8 | Niepaginowane odczyty całych tabel | POTWIERDZONE | `XtreamCatalogDb.kt:103-116`; `XtreamContentIndexDb.kt:73-85`; `LibraryDb.kt:35-42` | wysoka |
| 9 | `XtreamProvider.fetch` buforuje całą odpowiedź JSON | POTWIERDZONE | `XtreamProvider.kt:220-236`, szczególnie `234` | wysoka |
| 10 | Katalog nie przechowuje `streamUrl` → odtwarzanie wymaga sieci | POTWIERDZONE | brak `streamUrl` w `XtreamCatalogDb.kt` i `XtreamContentIndexDb.kt`; `ContentRepository.kt:176-246`; `XtreamProvider.kt:117-184` | wysoka |
| 11 | Dopasowania TMDB nie są utrwalane; `TmdbXtreamMatchRepository` to martwy kod | POTWIERDZONE | `TmdbXtreamMatchRepository.kt:20-91`; brak wywołań w `app/src/main` poza definicją | wysoka |
| 12 | `TmdbXtreamAvailabilityRepository.find()` uruchamia sync przy każdym wejściu | POTWIERDZONE | `TmdbXtreamAvailabilityRepository.kt:48-52` | wysoka |
| 13 | `startEpisodeQueue` pobiera sezony/odcinki z sieci | POTWIERDZONE | `TmdbXtreamAvailabilityRepository.kt:276-303`; `294`; `354` | wysoka |
| 14 | Cache TMDB tylko w pamięci | POTWIERDZONE | `TmdbDetailRepository.kt:18-19`, TTL `148`; `TmdbCatalogRepository.kt:27`, TTL `246`; `TmdbDiscoveryRepository.kt:28`, TTL `135` | wysoka |
| 15 | Wyszukiwanie tylko po TMDB, brak lokalnego | POTWIERDZONE | `SearchViewModel.kt:42`, `181`, `259`; `DiscoveryViewModel.kt:46-47`, `203`, `270` | wysoka |
| 16 | Katalog synchronizuje tylko aktywne konto, indeks — wszystkie konta | POTWIERDZONE | `XtreamCatalogRepository.kt:181`; `XtreamContentIndexRepository.kt:98` | wysoka |
| 17 | Ustawienia profilu są zapisywane, ale część nie jest konsumowana | POTWIERDZONE | `ProfileSettingsDb.kt:24-72`; brak użycia m.in. `themeMode`, `accentColor`, `liveTvHudTechStyle`, `liveTvStartOnLastChannel`, `addonConfigsJson` poza UI ustawień | wysoka |
| 18 | Kids Mode nie jest egzekwowany | POTWIERDZONE | `ProfileSettings.kt:40-70` — parametr `kidsMode` przyjmowany, ale nieużywany w `createDefaults`; brak filtra treści | wysoka |
| 19 | `liveTvStartOnLastChannel` nie jest używane | POTWIERDZONE | brak referencji poza `SettingsScreen` i schematem DB | wysoka |
| 20 | Historia LIVE jest tylko zapisywana, nie używana | CZĘŚCIOWO POTWIERDZONE | jest używana do listy „ostatnich kanałów” w HUD: `PlayerViewModel.kt:176-194`; nie jest używana do automatycznego startu ostatniego kanału | wysoka |
| 21 | `onSelectEpisode` jest niezwarte z HUD | POTWIERDZONE | deklaracja `PlayerHudOverlay.kt:72`; przekazanie `PlayerScreen.kt:322`; brak wywołania wewnątrz HUD | wysoka |
| 22 | Zduplikowany listener ExoPlayer | FAŁSZYWY ALARM | `Media3PlayerEngine.kt:49`, `105-106` — jeden obiekt; `PlayerScreen.kt:167-168` — drugi, osobny obiekt; każdy rejestrowany raz na interfejs | wysoka |
| 23 | Placeholdery: poprzednio „7 użyć” | CZĘŚCIOWO POTWIERDZONE | faktycznie 6 wywołań `PlaceholderScreen`: `MainShell.kt:511,524,527,530,533,536` | wysoka |
| 24 | `guide` prowadzi donikąd | POTWIERDZONE (nowe) | `MoreScreen.kt:27`; brak trasy `guide` w `MainShell.kt:495-537` | wysoka |
| 25 | `PlayerController` to martwy kod | POTWIERDZONE | referencje tylko w `PlayerController.kt` i `PlayerControllerTest.kt` | wysoka |
| 26 | `BrowseScreen` nieużywany, `BrowseViewModel` używany przez LIVE | POTWIERDZONE | brak wywołań `BrowseScreen`; `BrowseViewModel` używany w `LiveTvScreen` | wysoka |
| 27 | Cleartext + dane logowania w URL | POTWIERDZONE | `AndroidManifest.xml:15`; `XtreamProvider.kt:90,135,167,205,225-226` | wysoka |
| 28 | Brak testów Room/migracji, brak `room-testing` | POTWIERDZONE | `app/build.gradle.kts:124-126`; brak `androidTest`; brak `MigrationTestHelper` | wysoka |
| 29 | Liczba plików testowych: poprzednio 38 | CZĘŚCIOWO POTWIERDZONE | faktycznie 39 plików `*.kt`, 223 `@Test` | wysoka |

**Wniosek z sekcji 2:** większość poprzednich ustaleń się potwierdza. Korekty dotyczą: historii LIVE (jest częściowo używana), rzekomego duplikatu listenera (fałszywy alarm), liczby placeholderów (6, nie 7) i liczby plików testowych (39, nie 38). Nowe, istotne ustalenie: **brak migracji 1→2 przy jednoczesnym braku `fallbackToDestructiveMigration`**.

---

## 3. Szczegółowy audyt Room/SQLite

### 3.1. Architektura bazy

**Fakty:**

- Jedna baza `vireo.db`, jedna klasa `VireoDatabase`, 14 encji (`AccountDb.kt:100-119`).
- Wersja `14`, `exportSchema = false` (`AccountDb.kt:117-118`).
- DAO: `AccountDao`, `ProfileDao`, `XtreamContentIndexDao`, `LibraryDao`, `XtreamCatalogDao`, `EpgDao`, `ProfileSettingsDao`, `AppSettingsDao`, `RecentSearchDao`, `LiveChannelHistoryDao` (`AccountDb.kt:121-130`).
- Brak `allowMainThreadQueries`, brak `runBlocking` w warstwie DB (grep nie znalazł).
- Brak `fallbackToDestructiveMigration` (`AppModule.kt:63-78`).
- Brak jawnie ustawionego `setJournalMode`, `setQueryExecutor`, `setTransactionExecutor`; Room używa domyślnych ustawień.
- Transakcje są używane w `replaceCatalog` i `replaceForSourceAndType` (`XtreamCatalogDb.kt:171-194`, `XtreamContentIndexDb.kt:162-183`).
- Większość tabel ma FK do `accounts` z `ON DELETE CASCADE`; wyjątki: `library_items` (brak FK) i `live_channel_history.sourceId` (brak FK).

**Ocena:** warstwa Room jest poprawnie skonfigurowana podstawowo (IO, transakcje, brak main-thread queries). Problemy nie wynikają z „ciężkości Room”, lecz z modelu i zapytań.

### 3.2. Migracje

**Fakty:**

- Zarejestrowane migracje: `2→3`, `3→4`, `4→5`, `5→6`, `6→7`, `7→8`, `8→9`, `9→10`, `10→11`, `11→12`, `12→13`, `13→14` (`AppModule.kt:64-77`).
- **Brak `1→2`.** W historii Git wersja `1` istniała w commitach `8bc5934`, `dfb41d3`, `eed7cfd`.
- Brak `fallbackToDestructiveMigration`. Urządzenie z bazą v1, aktualizowane bezpośrednio do v14, zakończy się `IllegalStateException: A migration from 1 to 2 was required but not found`.
- `MIGRATION_6_7` tworzy `library_items` bez FK (`AccountDb.kt:197-226`).
- `MIGRATION_12_13` tworzy `live_channel_history` z FK tylko do profili; `sourceId` nie ma FK (`AccountDb.kt:460-482`).
- `MIGRATION_13_14` dodaje `logoUrl` i przenosi `posterUrl` dla LIVE; wygląda na zachowującą dane (`AccountDb.kt:484-495`).
- `MIGRATION_10_11` tworzy `profile_settings` i `app_settings`, uzupełnia istniejące profile (`AccountDb.kt:375-458`).
- `MIGRATION_5_6` czyści `catalog_sync_state`, wymuszając reindeksację po dodaniu `tmdbId`/`imdbId` (`AccountDb.kt:228-242`).

**Ocena:** migracje są staranne, ale **brak walidacji na rzeczywistej bazie** i **brak ciągłości od v1**. To najpoważniejsze ryzyko „wybuchowe” w całym audycie, choć jego realny zasięg zależy od tego, czy v1 kiedykolwiek trafiła do użytkowników. Tego nie da się rozstrzygnąć z repo — brak tagów/release’ów (`git tag` puste).

### 3.3. Spójność i integralność danych

**Fakty:**

- `library_items` ma PK `(profileId, contentKey)`, indeksy `(profileId, updatedAt)` i `(profileId, watchlisted)`, ale **żadnego FK** (`LibraryDb.kt:11-31`).
- `live_channel_history` ma FK do `profiles`, ale `sourceId` nie ma FK (`LiveChannelHistoryDb.kt:13-34`).
- `xtream_catalog_items` ma FK do `accounts` i PK `(sourceId, contentType, itemId)` (`XtreamCatalogDb.kt:35-66`).
- `xtream_content_index` ma FK do `accounts` i PK `(sourceId, contentType, streamId)` (`XtreamContentIndexDb.kt:12-42`).
- `catalog_sync_state` i `xtream_catalog_sync_state` to dwa niezależne stany synchronizacji (`XtreamContentIndexDb.kt:44-61`, `XtreamCatalogDb.kt:68-86`).
- `contentKey` dla Xtream nie zawiera `sourceId`: `"xtream:${type.name}:${m.id}"` (`DetailViewModel.kt:84`, `105`). Dla LIVE zawiera `sourceId` (`PlayerViewModel.kt:562`), dla TMDB jest globalny (`TmdbXtreamAvailabilityRepository.kt:166`).
- Brak tabel: `episodes`, `seasons`, `external_ids`, `metadata_matches`, `sync_runs`, `tombstones`, `image_cache`.
- Brak kolumn: `lastSeenAt`, `providerUpdatedAt`, `contentHash`, `isAvailable`, `deletedAt`.
- `syncedAt` jest wspólny dla całej partii (`XtreamCatalogRepository.kt:162-165`; `XtreamContentIndexRepository.kt:152-166`), więc nie da się po nim rozróżnić zmian.

**Ocena:** model jest „płaski” i wystarczający dla małego katalogu, ale nie dla wielu źródeł, wielu profili i trwałego cache. Największe dziury integralności: `library_items` bez FK i `contentKey` bez `sourceId` (kolizje między kontami).

### 3.4. Wydajność zapytań

**Fakty i problemy:**

1. **`getItems` bez `LIMIT` i z `ORDER BY title COLLATE NOCASE`** (`XtreamCatalogDb.kt:103-116`, linia `109`).
   - Indeks `(sourceId, contentType, title)` jest w domyślnym porządku BINARY, więc sortowanie `COLLATE NOCASE` może wymagać tymczasowego B-tree.
   - `TitleListViewModel.kt:41-48` wczytuje całą kategorię do `StateFlow`.
2. **`findCandidatesContaining`** (`XtreamContentIndexDb.kt:103-114`):
   - `normalizedTitle LIKE '%' || :token || '%'` — wiodący `%` uniemożliwia użycie indeksu po `normalizedTitle`.
   - `ORDER BY sourceId` + `LIMIT 1000` — nadal może skanować i sortować dużo wierszy.
3. **`getForSource(...).size`** (`XtreamContentIndexRepository.kt:119-121`) — pełne wczytanie tabeli tylko po to, by policzyć wiersze; powinno być `SELECT COUNT(*)`.
4. **`LibraryDao.observeAll`** (`LibraryDb.kt:35-42`) — pełna lista biblioteki; `LibraryViewModel.kt:79-91` łączy trzy flow (watchlist, continue watching, all) i przy każdym zapisie postępu uruchamia trzy zapytania.
5. **`EpgDao.deleteExpired`** (`EpgDb.kt:116-117`): `DELETE FROM epg_programs WHERE endTimeMs < :cutoffMs` — indeksy `(sourceId, streamId, endTimeMs)` nie mogą być użyte bez warunków na wiodące kolumny; to pełny skan przy każdym `nowNext` (`EpgRepository.kt:133`).
6. **`BrowseViewModel`** (`BrowseViewModel.kt:60-139`) ładuje 10 kategorii równolegle, każda przez `repo.titles(...)`, a potem robi `.take(40)` w Kotlinie. To oznacza wczytanie całych kategorii z DB do pamięci. Ścieżka jest obecnie martwa (brak `BrowseScreen` w nawigacji), ale kod istnieje i może wrócić.
7. **`XtreamProvider.fetch`** (`XtreamProvider.kt:220-236`) robi `resp.body?.string()` i `parseToJsonElement` — cała odpowiedź w pamięci; przy dużych listach to skok alokacji.
8. **Brak `LIMIT/OFFSET` i Paging 3** w DAO katalogu i biblioteki.

**Ocena:** to są realne, mierzalne problemy SQL, ale **wszystkie są rozwiązywalne w Room** przez zmianę zapytań, indeksów i paginacji. Nie ma dowodu, że narzut Room jest tu istotny.

### 3.5. Synchronizacja i współbieżność

**Fakty:**

- `XtreamCatalogRepository` ma `Mutex`, TTL 6h, i pełny replace (`XtreamCatalogRepository.kt:131-199`, `281`).
- `XtreamContentIndexRepository` ma dwa `Mutex` (movie/series), TTL 6h, i pełny replace (`XtreamContentIndexRepository.kt:50-51`, `94-230`, `349-351`).
- Katalog: tylko aktywne konto (`XtreamCatalogRepository.kt:181`).
- Indeks: wszystkie konta (`XtreamContentIndexRepository.kt:98`).
- Brak wspólnej transakcji między katalogiem a indeksem; brak koordynacji, która tabela jest „źródłem prawdy”.
- Brak batchowania po stronie DAO (pojedyncze `insertAll(items)` z całą listą).
- Brak `ANALYZE`/`VACUUM`/`wal_checkpoint`; przy pełnym replace i dużych katalogach WAL i plik bazy mogą rosnąć.
- `shouldReplaceXtreamCatalog` chroni przed wyczyszczeniem katalogu pustą odpowiedzią (`XtreamCatalogSyncPolicy.kt:13`), co jest dobrą praktyką.

**Ocena:** mechanizmy bezpieczeństwa są, ale strategia „wszystko albo nic” jest głównym źródłem write amplification i braku inkrementalności.

### 3.6. Bezpieczeństwo przechowywania

**Fakty:**

- Hasło konta jest szyfrowane AES-GCM z kluczem w Android Keystore (`CryptoManager.kt:17-30`); PIN profili — PBKDF2 120k iteracji (`CryptoManager.kt:19-22`).
- `server` i `username` są przechowywane jawnie (`AccountDb.kt:18-27`).
- `usesCleartextTraffic="true"` (`AndroidManifest.xml:15`) — globalne zezwolenie na HTTP.
- Dane logowania Xtream są wstawiane do URL path/query (`XtreamProvider.kt:90,135,167,205,225-226`).
- Brak `HttpLoggingInterceptor` (grep nie znalazł) — dobrze, ale URL-e z poświadczeniami mogą trafić do logów systemowych, crash reports lub analityki.
- `allowBackup="false"` (`AndroidManifest.xml:10`) — dobrze.
- Brak `network_security_config` — nie ma polityki per-domena.

**Ocena:** szyfrowanie hasła jest poprawne, ale **cleartext + poświadczenia w URL** to problem P0 z punktu widzenia bezpieczeństwa, niezależny od Room.

### 3.7. Testy warstwy DB

**Fakty:**

- 39 plików testowych, 223 `@Test`.
- Zależności testowe: tylko JUnit4, `kotlinx-coroutines-test`, Mockito (`app/build.gradle.kts:124-126`).
- Brak `androidTest`, brak `room-testing`, brak `MigrationTestHelper`.
- Brak testów DAO, migracji, zapytań, integralności FK.
- Testy policyjne (`XtreamIndexSyncPolicyTest`, `XtreamCatalogSyncPolicyTest`) są dobre, ale nie dotykają SQL.

**Ocena:** brak testów migracji przy 12 migracjach i braku `exportSchema` to luka procesowa. Każda przyszła zmiana schematu będzie niezweryfikowana.

### 3.8. Pozytywy

- Brak `allowMainThreadQueries` i `runBlocking` w DB.
- Transakcje w replace.
- FK CASCADE dla większości tabel.
- Indeksy dla typowych zapytań katalogu, indeksu i biblioteki.
- Guard `shouldReplaceXtreamCatalog`.
- Polityka świeżości `isXtreamIndexFresh`.
- Szyfrowanie hasła i PIN.

---

## 4. Audyt katalogu LIVE/VOD/seriali

### 4.1. Co jest przechowywane

**LIVE:**
- `xtream_catalog_categories`: `sourceId`, `contentType`, `categoryId`, `name`, `updatedAt` (`XtreamCatalogDb.kt:12-33`).
- `xtream_catalog_items`: `sourceId`, `contentType`, `itemId`, `categoryId`, `title`, `posterUrl`, `logoUrl`, `rating`, `epgChannelId`, `tmdbId`, `imdbId`, `containerExtension`, ... (`XtreamCatalogDb.kt:52-66`).
- EPG: `epg_programs` i `epg_sync_state` (`EpgDb.kt:13-58`).
- Historia: `live_channel_history` (`LiveChannelHistoryDb.kt:13-34`).

**VOD:**
- Te same tabele co LIVE, z `contentType = MOVIE`.
- Brak `streamUrl`, brak `duration`, brak szczegółów (`get_vod_info`).

**Serie:**
- Te same tabele co LIVE, z `contentType = SERIES`.
- **Brak tabel sezonów i odcinków.**
- Brak `streamUrl` odcinków.

### 4.2. Czego nie ma

- `streamUrl` dla MOVIE/SERIES/odcinków.
- Sezonów, odcinków, mapowania odcinków.
- Pełnego EPG (tylko krótki EPG per kanał).
- Metadanych TMDB (poza `tmdbId`/`imdbId` dostarczonymi przez providera).
- Trwałych dopasowań TMDB ↔ Xtream.
- Historii synchronizacji i tombstone’ów.
- Cache obrazów w DB (Coil ma domyślny disk cache, ale nie ma indeksu/metadanych).
- Lokalnego wyszukiwania katalogu.

### 4.3. Przepływ synchronizacji katalogu

**Fakt:** `ensureCatalog(type)` (`XtreamCatalogRepository.kt:78-117`):

1. Jeśli brak danych — **blokujący** pełny sync przed pokazaniem UI (linie `88-95`).
2. Jeśli dane są świeże (TTL 6h) — zwraca lokalne dane.
3. Jeśli dane są stare — zwraca lokalne dane i uruchamia sync w tle (`97-107`).
4. `syncLocked` pobiera kategorie i pełną listę elementów, mapuje, sprawdza guard, robi `replaceCatalog` (pełny delete+insert).

**Skutki:**
- Pierwsze uruchomienie może trwać długo i nie ma progresu/partycji.
- Każdy sync to pełny zapis całego katalogu.
- Brak wykrywania zmian per rekord.
- Brak możliwości pokazania „częściowego” katalogu w trakcie sync.
- Tylko aktywne konto.

### 4.4. Przepływ indeksu TMDB

**Fakt:** `syncMovies()`/`syncSeries()` (`XtreamContentIndexRepository.kt:53-59`):

- Iteruje po **wszystkich** kontach (`98`).
- Dla każdego konta sprawdza TTL; jeśli świeże — liczy `getForSource(...).size` (pełny odczyt).
- Jeśli stare — pobiera pełną listę movie/series i robi `replaceForSourceAndType` (pełny delete+insert).
- Mapuje `normalizedTitle`, `year`, `posterUrl`, `categoryId`, `tmdbId`, `imdbId`.

**Skutki:**
- Dwa niezależne katalogi tych samych danych.
- Podwójny ruch sieciowy i podwójny zapis.
- Niespójność: katalog tylko aktywne konto, indeks wszystkie.
- Brak wspólnego stanu „co jest aktualne”.

### 4.5. Odtwarzanie

**Fakt:** `ContentRepository.resolvePlaybackSource` (`ContentRepository.kt:176-246`):

- LIVE: `provider.liveUrl(id)` — lokalnie, bez sieci.
- MOVIE: `provider.movieDetails(id)` — **sieć** (`XtreamProvider.kt:117-137`).
- SERIES: `provider.seriesDetails(id)` — **sieć** (`XtreamProvider.kt:139-184`).
- Alternatywne źródła: dla każdego kandydata `movieDetails`/`seriesDetails` — kolejne zapytania (`TmdbXtreamAvailabilityRepository.kt:127-158`, `350-363`).

**Skutki:**
- VOD/serie nie działają w pełni offline.
- Odtwarzanie zależy od dostępności API providera.
- Failover może wykonać wiele zapytań.
- `containerExtension` jest przechowywany, więc MOVIE **dałoby się** odtworzyć bez `get_vod_info`, gdyby URL był budowany lokalnie; obecny kod tego nie robi.

### 4.6. Wyszukiwanie

**Fakt:** `SearchViewModel` używa wyłącznie `TmdbCatalogRepository.searchMulti` (`SearchViewModel.kt:42`, `181`, `259`); `DiscoveryViewModel` używa `TmdbDiscoveryRepository`/`TmdbGenreRepository` (`DiscoveryViewModel.kt:46-47`, `203`, `270`). Brak zapytań do lokalnego katalogu.

**Skutki:**
- Użytkownik nie znajdzie treści, której nie ma w TMDB lub która jest tylko w Xtream.
- Brak wyszukiwania lokalnego/offline.
- Brak rankingu łączącego TMDB i Xtream.

### 4.7. Skala

Katalog „dziesiątki tysięcy rekordów” jest realnym scenariuszem, ale **sam rozmiar nie jest argumentem za porzuceniem Room**. Problemy pojawiają się przy konkretnych zapytaniach i strategii sync, a nie przy bibliotece dostępu.

---

## 5. Audyt dopasowywania metadanych (TMDB)

### 5.1. Pipeline

**Fakty:**

1. `TmdbCatalogRepository` — popular/trending/search; cache w pamięci (`TmdbCatalogRepository.kt:27`, TTL `246`).
2. `TmdbDetailRepository` — szczegóły i odcinki sezonów; cache w pamięci (`TmdbDetailRepository.kt:18-19`, TTL `148`).
3. `TmdbDiscoveryRepository` — discover; cache w pamięci (`TmdbDiscoveryRepository.kt:28`, TTL `135`).
4. `XtreamContentIndexRepository` — kandydaci z lokalnego indeksu (`findCandidates`, `findCandidatesByExternalIds`).
5. `TmdbXtreamMatcher` — czysta logika dopasowania (normalizacja, scoring, próg).
6. `TmdbXtreamAvailabilityRepository` — orkiestracja: sync, kandydaci, match, playback.
7. `TmdbXtreamMatchRepository` — dopasowanie pojedynczego elementu TMDB; **brak wywołań produkcyjnych**, tylko definicja.

### 5.2. Kryteria dopasowania

**Fakty z kodu:**

- Najpierw identyfikatory zewnętrzne: `tmdbId`, `imdbId` (`XtreamContentIndexRepository.kt:288-311`; indeksy `XtreamContentIndexDb.kt:116-140`).
- Potem `normalizedTitle` + `year` ±1 (`XtreamContentIndexDb.kt:87-101`).
- Potem tokeny z tytułu, `LIKE '%token%'`, `LIMIT 1000` (`XtreamContentIndexRepository.kt:240-286`).
- Normalizacja: usuwa rok w nawiasach, tagi jakości, sezon/odcinek, prefiksy językowe, znaki diakrytyczne (`XtreamContentIndexMapper.kt:9-63`).
- Matcher ma progi i warianty (`TmdbXtreamMatcher.kt:8-198`).

### 5.3. Problemy

1. **Brak trwałości dopasowań.** `TmdbXtreamAvailabilityRepository.find()` liczy dopasowanie przy każdym otwarciu szczegółów (`41-103`). `TmdbXtreamMatchRepository` nie jest używany produkcyjnie.
2. **Brak negatywnego cache.** Brak informacji „ten element nie ma dopasowania”; każde wejście powtarza próbę.
3. **Brak historii/manualnej korekty.** Użytkownik nie może zatwierdzić/odrzucić dopasowania; brak `reviewedAt`, `status`, `method`.
4. **Brak `matchInputHash`.** Zmiana tytułu/roku/ID nie ma mechanizmu unieważnienia.
5. **Sync przy każdym `find()`.** Nawet przy TTL 6h, świeża ścieżka ładuje cały indeks przez `.size`.
6. **Odcinki z TMDB, nie z lokalnej bazy.** `startEpisodeQueue` pobiera sezony TMDB (`294`) i szczegóły serialu Xtream dla każdego wariantu (`354`).
7. **Cache tylko w pamięci.** Po restarcie aplikacji wszystko jest ponownie pobierane.
8. **Brak tabeli `external_ids`.** `tmdbId`/`imdbId` są tylko kolumnami w indeksie i katalogu; brak innych ID (TVDB, Trakt) i brak odwrotnego wyszukiwania.
9. **Brak cache metadanych TMDB w DB.** `TmdbDetail`/`TmdbCatalogPage` nie są przechowywane.
10. **Brak deduplikacji między źródłami.** Ten sam film z dwóch kont Xtream to dwa wiersze; brak wspólnego „content identity”.

### 5.4. Pozytywy

- Matcher jest czysty i testowalny (`TmdbXtreamMatcherTest`).
- Normalizacja jest solidna.
- ID zewnętrzne są sprawdzane pierwsze.
- Są indeksy po `tmdbId`/`imdbId`.
- Są testy normalizatora i mappera.

---

## 6. Porównanie A/B/C

### 6.1. Definicje

- **A — Room, obecny model, optymalizacje punktowe.** Zostawiamy schemat, dodajemy indeksy, `COUNT`, `LIMIT`, paginację, poprawiamy sync i cache.
- **B — Room, przebudowa modelu katalogu.** Normalizujemy katalog (osobne tabele dla LIVE/VOD/serii/sezonów/odcinków), dodajemy inkrementalny sync, trwałe metadane TMDB, trwałe dopasowania, tombstone’y, `sync_runs`, FTS, Paging 3.
- **C — migracja na bezpośrednie SQLite.** Rezygnacja z Room, ręczne DAO, ręczne mapowanie cursorów, ręczne migracje, ręczne Flow/Paging.

### 6.2. Tabela porównawcza

| Kryterium | A: Room + optymalizacje | B: Room + nowy model | C: raw SQLite |
|---|---|---|---|
| Nakład pracy | mały/średni | duży | bardzo duży |
| Ryzyko regresji | niskie | średnie/wysokie | wysokie |
| Zgodność z obecnym kodem | pełna | częściowa (nowe DAO/tabele) | pełna przepisanka warstwy DB |
| Kompilacyjne sprawdzanie zapytań | tak | tak | nie |
| Flow/Paging/transakcje | wbudowane | wbudowane | ręcznie |
| Migracje | Room `Migration` | Room `Migration` | ręczne |
| Testy migracji | `room-testing` | `room-testing` | ręczne |
| FTS | FTS4 / `@RawQuery` | FTS4/FTS5 przez migrację | pełna kontrola |
| Rozwiązuje pełny replace | tak (zmiana logiki) | tak | tak, ale trzeba napisać |
| Rozwiązuje brak paginacji | tak | tak | tak, ale trzeba napisać |
| Rozwiązuje brak trwałych dopasowań | tak | tak | tak, ale trzeba napisać |
| Rozwiązuje brak `streamUrl`/odcinków | tak | tak | tak, ale trzeba napisać |
| Narzut runtime Room | znikomy | znikomy | teoretycznie niższy, praktycznie bez znaczenia |
| Utrzymanie | dobre | dobre | gorsze (boilerplate) |
| Ryzyko dla użytkownika | niskie | średnie | wysokie |

### 6.3. Odpowiedzi na 5 pytań

**1. Czy istnieją dowody, że problemy pochodzą z Room?**
**Nie.** Room jest tylko warstwą dostępu. Zidentyfikowane problemy to: duplikacja modelu, pełny replace, brak inkrementalności, brak paginacji, brak `COUNT`, `LIKE` z wiodącym `%`, brak trwałych dopasowań, brak `streamUrl`/odcinków, ponowne pobieranie z sieci. To problemy **schematu, zapytań i procesu sync**, nie biblioteki Room.

**2. Czy przejście na bezpośrednie SQLite rozwiązałoby te problemy?**
**Nie.** Te same zapytania i ten sam schemat nadal byłyby wolne. Trzeba by najpierw przepisać zapytania i model — a to samo można zrobić w Room. Raw SQLite nie da automatycznie inkrementalności, paginacji ani cache dopasowań; doda natomiast boilerplate i ryzyko błędów.

**3. Czy optymalizacja Room może osiągnąć ten sam lub lepszy efekt?**
**Tak.** Room wspiera: `COUNT(*)`, `LIMIT/OFFSET`, Paging 3, `@Upsert`, `@Transaction`, `withTransaction`, indeksy composite, FTS4, `@RawQuery`, `@Relation`, `Flow`, `PagingSource`, `MigrationTestHelper`. Wystarczy przepisać zapytania i schemat.

**4. Czy istnieje konkretny powód techniczny, by porzucić Room?**
**Nie.** Brak dowodu na limit Room. Ewentualne potrzeby (FTS5, `WITHOUT ROWID`, custom collation, zaawansowane triggery) można rozwiązać przez `@RawQuery`/migracje SQL lub są rzadkie. Pełna migracja to duże ryzyko bez mierzalnego zysku.

**5. Który wariant rekomendujesz i dlaczego?**
**B — Room + nowy model katalogu**, z natychmiastowym **A** jako zestawem hotfixów. **C odrzucam.** Skala „dziesiątki tysięcy” nie jest wystarczającym powodem migracji; powodem jest model i zapytania, a te można naprawić w Room.

---

## 7. Proponowany model danych (logiczny)

Poniższy model jest **propozycją**, nie zatwierdzonym projektem. Zachowuje Room i zakłada migrację etapową.

### 7.1. Tabele źródłowe

**`sources`** (obecne `accounts`):
- `id` PK, `name`, `providerType`, `server`, `username`, `passwordCipher`, `isActive`, `createdAt`, `lastVerifiedAt`.
- Indeks `(isActive)`.
- Hasło nadal szyfrowane przez `CryptoManager`; **nie** przechowujemy go w tabelach katalogu.

**`live_categories`**
- PK `(sourceId, categoryId)`, `name`, `sortOrder`, `providerUpdatedAt`, `firstSeenAt`, `lastSeenAt`, `isAvailable`.
- FK `sourceId` → `sources` CASCADE.
- Indeks `(sourceId, sortOrder)`.

**`live_channels`**
- PK `(sourceId, channelId)`.
- `categoryId`, `name`, `normalizedTitle`, `sortTitle`, `logoUrl`, `streamId`, `streamExtension`, `epgChannelId`, `providerUpdatedAt`, `firstSeenAt`, `lastSeenAt`, `isAvailable`, `contentHash`.
- Indeksy: `(sourceId, categoryId, sortTitle)`, `(sourceId, epgChannelId)`, `(sourceId, isAvailable, lastSeenAt)`.
- FK `sourceId` → `sources` CASCADE.
- **URL budowany przy odtwarzaniu** z `sources.server` + `streamId` + `streamExtension`; nie zapisujemy poświadczeń w każdym wierszu.

**`vod_categories`**
- PK `(sourceId, categoryId)`, `name`, `sortOrder`, `providerUpdatedAt`, `firstSeenAt`, `lastSeenAt`, `isAvailable`.

**`movies`**
- PK `(sourceId, movieId)`.
- `categoryId`, `title`, `normalizedTitle`, `sortTitle`, `year`, `posterUrl`, `backdropUrl`, `logoUrl`, `rating`, `genre`, `description`, `duration`, `streamId`, `containerExtension`, `tmdbId`, `imdbId`, `providerUpdatedAt`, `firstSeenAt`, `lastSeenAt`, `isAvailable`, `contentHash`.
- Indeksy: `(sourceId, categoryId, sortTitle)`, `(sourceId, normalizedTitle, year)`, `(sourceId, isAvailable, lastSeenAt)`, `(sourceId, tmdbId)`, `(sourceId, imdbId)`.

**`series_categories`**
- PK `(sourceId, categoryId)`, `name`, `sortOrder`, ... .

**`series`**
- PK `(sourceId, seriesId)`.
- `categoryId`, `title`, `normalizedTitle`, `sortTitle`, `year`, `posterUrl`, `backdropUrl`, `rating`, `genre`, `description`, `tmdbId`, `imdbId`, `providerUpdatedAt`, `firstSeenAt`, `lastSeenAt`, `isAvailable`, `contentHash`.
- Indeksy jak dla `movies`.

**`seasons`**
- PK `(sourceId, seriesId, seasonNumber)`.
- `title`, `posterUrl`, `providerUpdatedAt`, `lastSeenAt`, `isAvailable`.
- FK `(sourceId, seriesId)` → `series` CASCADE.
- Indeks `(sourceId, seriesId)`.

**`episodes`**
- PK `(sourceId, episodeId)`.
- Unikalny `(sourceId, seriesId, seasonNumber, episodeNumber)`.
- `seriesId`, `seasonNumber`, `episodeNumber`, `title`, `plot`, `duration`, `thumbUrl`, `streamId`, `containerExtension`, `providerUpdatedAt`, `firstSeenAt`, `lastSeenAt`, `isAvailable`, `contentHash`.
- FK `(sourceId, seriesId)` → `series` CASCADE.
- Indeksy: `(sourceId, seriesId, seasonNumber, episodeNumber)`, `(sourceId, isAvailable, lastSeenAt)`.

### 7.2. Tożsamość i identyfikatory zewnętrzne

**`catalog_external_ids`**
- PK `(sourceId, contentType, itemId, idType)`.
- `idValue`, `idType` (`TMDB`, `IMDB`, `TVDB`, `PROVIDER`), `updatedAt`.
- Indeks `(idType, idValue, contentType)` — odwrotne wyszukiwanie.
- Indeks `(sourceId, contentType, itemId)`.
- Brak FK (polimorficzne); integralność egzekwowana w aplikacji.
- Opcjonalnie denormalizowane `tmdbId`/`imdbId` w `movies`/`series` dla szybkich zapytań.

**Zasady unikalności:**
- ID providera są unikalne **per `sourceId` + typ treści**, nie globalnie.
- `stream_id` LIVE/VOD i `series_id` mogą na siebie nachodzić — rozdziela je typ/table.
- Odcinki: unikalność po `(sourceId, seriesId, seasonNumber, episodeNumber)`, bo `episodeId` bywa zawodne.
- TMDB ID są globalne per typ (`movie`/`tv`); dlatego w `tmdb_metadata` PK zawiera `mediaType`.

### 7.3. Cache metadanych TMDB

**`tmdb_metadata`**
- PK `(tmdbId, mediaType, language)`.
- `title`, `originalTitle`, `year`, `overview`, `posterPath`, `backdropPath`, `rating`, `runtime`, `genresJson`, `fetchedAt`, `expiresAt`, `etag`.

**`tmdb_seasons`**
- PK `(tmdbId, seasonNumber, language)`.
- `name`, `overview`, `posterPath`, `airDate`, `episodeCount`, `fetchedAt`, `expiresAt`.

**`tmdb_episodes`**
- PK `(tmdbId, seasonNumber, episodeNumber, language)`.
- `name`, `overview`, `stillPath`, `airDate`, `runtime`, `fetchedAt`, `expiresAt`.

**Zaleta:** szczegóły TMDB i odcinki są dostępne offline; TTL kontroluje odświeżanie.

### 7.4. Trwałe dopasowania

**`metadata_matches`**
- PK `(sourceId, contentType, itemId)` — jedno wybrane dopasowanie.
- `tmdbId`, `mediaType`, `confidence`, `method` (`EXTERNAL_ID`, `TITLE_YEAR`, `TOKEN`, `MANUAL`), `status` (`MATCHED`, `AMBIGUOUS`, `NOT_FOUND`, `REJECTED`, `MANUAL`), `matchInputHash`, `matchedAt`, `reviewedAt`, `notes`.
- Indeksy: `(tmdbId, mediaType)`, `(status)`, `(sourceId, contentType, status)`.

**`match_candidates`**
- PK `(sourceId, contentType, itemId, tmdbId)`.
- `score`, `confidence`, `reason`, `createdAt`.
- Indeks `(tmdbId, mediaType)`.
- Służy do ręcznego przeglądu; nie usuwamy kandydatów przy każdym przeliczeniu.

**Zasady:**
- `matchInputHash = hash(normalizedTitle | year | tmdbId | imdbId)`.
- Dopasowanie liczone raz; ponownie tylko przy zmianie hasha, wygaśnięciu `NOT_FOUND` lub ręcznym odświeżeniu.
- `NOT_FOUND` ma TTL (np. 30 dni), żeby nie katować API.
- Ręczna korekta ustawia `status = MANUAL` i `reviewedAt`.

### 7.5. Synchronizacja

**`sync_state`**
- PK `(sourceId, contentType)`.
- `lastAttemptAt`, `lastSuccessAt`, `status` (`IDLE`, `RUNNING`, `OK`, `ERROR`), `lastError`, `itemCount`, `etag`, `lastCursor`.

**`sync_runs`**
- PK `id` autoincrement.
- `sourceId`, `contentType`, `startedAt`, `finishedAt`, `status`, `itemsFetched`, `itemsInserted`, `itemsUpdated`, `itemsMarkedMissing`, `error`.
- Indeks `(sourceId, startedAt)`.

**`catalog_changes`** (opcjonalnie)
- `id`, `sourceId`, `contentType`, `itemId`, `changeType`, `changedAt`.
- Do audytu/debugowania.

### 7.6. EPG

**`epg_programs`** (istniejące):
- PK `(sourceId, streamId, startTimeMs)`.
- Dodać indeks `(sourceId, endTimeMs)` lub `(endTimeMs)` dla `deleteExpired`.
- `epg_sync_state` bez zmian.

### 7.7. Biblioteka i profil

**`library_items`**:
- PK `(profileId, sourceId, contentType, itemId)` — zamiast samego `contentKey`.
- `contentKey` jako kolumna legacy/kompatybilnościowa.
- FK `profileId` → `profiles` CASCADE.
- FK `sourceId` → `sources` CASCADE (nullable dla treści TMDB-only).
- Indeksy: `(profileId, updatedAt)`, `(profileId, watchlisted)`, `(profileId, watched, positionMs)`.
- To rozwiązuje kolizje `contentKey` i osierocone rekordy.

**`live_channel_history`**:
- FK `sourceId` → `sources` CASCADE (lub `SET NULL`).
- FK `profileId` → `profiles` CASCADE.

**`profile_settings`**, **`app_settings`**, **`recent_searches`** — bez zmian strukturalnych; ewentualnie dodać indeksy.

### 7.8. Obrazy i wyszukiwanie

**`image_cache_index`** (opcjonalnie):
- `url` PK, `localPath`, `sizeBytes`, `fetchedAt`, `lastUsedAt`, `etag`.
- Nie przechowywać BLOB-ów w SQLite; obrazy w Coil disk cache, tabela tylko do ewidencji/ewikcji.

**`search_fts`** (opcjonalnie):
- FTS4/FTS5 po `title`, `normalizedTitle`, `aliases`, `contentType`, `sourceId`, `itemId`.
- Używać do lokalnego wyszukiwania i rankingu; TMDB search może być uzupełnieniem.

### 7.9. Inkrementalna synchronizacja — algorytm

1. Pobierz pełną listę (Xtream nie oferuje delty) lub deltę, jeśli provider ją ma.
2. Podziel na batch’e (np. 500–1000).
3. Dla batch’a policz `contentHash` i porównaj z istniejącymi wierszami (`SELECT` po PK).
4. `@Upsert` tylko nowych/zmienionych; nie ruszaj niezmienionych.
5. Oznacz niewidziane: `isAvailable = 0`, `deletedAt = now`.
6. Usuń fizycznie dopiero po retencji (np. 30 dni).
7. Zapisz `sync_runs` i `sync_state`.
8. Wszystko w transakcjach per batch; UI czyta z DB przez `Flow`.

**Efekt:** brak pełnego delete+insert, mniejszy WAL, mniej inwalidacji UI, możliwość pokazania postępu.

---

## 8. Konkretne problemy z lokalizacjami

### 8.1. Room / migracje / integralność

**P1 — Brak migracji 1→2**
- Plik: `app/src/main/java/app/vireo/di/AppModule.kt:64-77`; `app/src/main/java/app/vireo/data/local/AccountDb.kt:117`.
- Dowód: lista migracji zaczyna się od `migration2To3`; `version = 1` występowało w historii Git (`8bc5934`, `dfb41d3`, `eed7cfd`); brak `fallbackToDestructiveMigration`.
- Wpływ: crash przy aktualizacji z v1; potencjalna utrata dostępu do aplikacji.
- Pewność: wysoka. Status: POTWIERDZONE. Priorytet: **P0** (jeśli v1 była wydana) / P1 (jeśli nie).

**P1 — Brak `exportSchema` i testów migracji**
- Plik: `AccountDb.kt:118`; `app/build.gradle.kts:124-126`.
- Dowód: `exportSchema = false`; brak `room-testing`, brak `androidTest`, brak `MigrationTestHelper`.
- Wpływ: każda migracja niezweryfikowana; ryzyko regresji przy v15+.
- Pewność: wysoka. Status: POTWIERDZONE. Priorytet: **P1**.

**P1 — `library_items` bez FK**
- Plik: `app/src/main/java/app/vireo/data/local/LibraryDb.kt:11-31`; migracja `AccountDb.kt:197-226`; `ProfileRepository.kt:130-147`.
- Dowód: `@Entity` nie ma `foreignKeys`; usunięcie profilu nie czyści biblioteki.
- Wpływ: osierocone rekordy, rosnąca baza, możliwe wycieki danych między profilami po ponownym użyciu ID.
- Pewność: wysoka. Status: POTWIERDZONE. Priorytet: **P1**.

**P2 — `live_channel_history.sourceId` bez FK**
- Plik: `LiveChannelHistoryDb.kt:13-34`; migracja `AccountDb.kt:460-482`.
- Dowód: FK tylko do `profiles`; `sourceId` bez FK.
- Wpływ: osierocone wpisy po usunięciu konta; błędne „ostatnie kanały”.
- Pewność: wysoka. Status: POTWIERDZONE. Priorytet: **P2**.

**P2 — `EpgDao.deleteExpired` bez indeksu po `endTimeMs`**
- Plik: `EpgDb.kt:116-117`; indeksy `EpgDb.kt:16-19`.
- Dowód: query filtruje tylko `endTimeMs`; indeksy mają wiodące `sourceId, streamId`.
- Wpływ: pełny skan przy każdym `nowNext` (`EpgRepository.kt:133`).
- Pewność: wysoka. Status: POTWIERDZONE. Priorytet: **P1/P2** (zależnie od liczby programów).

**P2 — Brak `lastSeenAt`/`contentHash`/tombstone’ów**
- Plik: `XtreamCatalogRepository.kt:162-165`; `XtreamContentIndexRepository.kt:152-166`.
- Dowód: `syncedAt` wspólny dla całej partii; brak kolumn śledzących zmiany.
- Wpływ: brak inkrementalności; tylko pełny replace.
- Pewność: wysoka. Status: POTWIERDZONE. Priorytet: **P1** (dla modelu B).

### 8.2. Katalog / synchronizacja

**P1 — Dwa katalogi tego samego**
- Plik: `XtreamCatalogDb.kt:35-66` vs `XtreamContentIndexDb.kt:12-42`.
- Dowód: te same dane (tytuł, poster, categoryId, tmdbId, imdbId) w dwóch tabelach; osobne sync i TTL.
- Wpływ: podwójny ruch, podwójny zapis, niespójność, trudność w utrzymaniu.
- Pewność: wysoka. Status: POTWIERDZONE. Priorytet: **P1**.

**P1 — Pełny `DELETE` + `INSERT`**
- Plik: `XtreamCatalogDb.kt:171-194`; `XtreamContentIndexDb.kt:162-183`.
- Dowód: `deleteItems` + `insertItems` w transakcji; brak diff/upsert.
- Wpływ: write amplification, duży WAL, inwalidacja UI, brak progresu.
- Pewność: wysoka. Status: POTWIERDZONE. Priorytet: **P1**.

**P1 — Katalog tylko aktywne konto, indeks wszystkie**
- Plik: `XtreamCatalogRepository.kt:181`; `XtreamContentIndexRepository.kt:98`.
- Dowód: `accountDao.getActive()` vs `accounts` (wszystkie).
- Wpływ: brak przeglądania katalogu z nieaktywnych kont; niespójne dane.
- Pewność: wysoka. Status: POTWIERDZONE. Priorytet: **P1**.

**P1 — `getItems` bez `LIMIT` i z `COLLATE NOCASE`**
- Plik: `XtreamCatalogDb.kt:103-116`, linia `109`.
- Dowód: brak `LIMIT`; sortowanie `COLLATE NOCASE` po kolumnie z indeksem BINARY.
- Wpływ: pełne wczytanie kategorii + sort; `TitleListViewModel.kt:41-48` trzyma całość w pamięci.
- Pewność: wysoka. Status: POTWIERDZONE. Priorytet: **P1**.

**P2 — `BrowseViewModel` 10× pełna kategoria**
- Plik: `BrowseViewModel.kt:60-139` (`.take(40)` w Kotlinie).
- Dowód: `repo.titles` zwraca całą listę, potem `take(40)`.
- Wpływ: skok pamięci/IO; obecnie ścieżka martwa (brak `BrowseScreen`), ale kod wróci.
- Pewność: wysoka. Status: POTWIERDZONE. Priorytet: **P2**.

**P1 — `findCandidatesContaining` z wiodącym `%`**
- Plik: `XtreamContentIndexDb.kt:103-114`; `XtreamContentIndexRepository.kt:275-283`.
- Dowód: `LIKE '%' || :token || '%'`, `LIMIT 1000`, `ORDER BY sourceId`.
- Wpływ: pełny skan indeksu; kosztowne dopasowanie.
- Pewność: wysoka. Status: POTWIERDZONE. Priorytet: **P1**.

**P1 — `getForSource(...).size`**
- Plik: `XtreamContentIndexRepository.kt:119-121`.
- Dowód: pełne wczytanie listy, potem `.size`.
- Wpływ: alokacja całego indeksu przy każdym `find()` w TTL.
- Pewność: wysoka. Status: POTWIERDZONE. Priorytet: **P1**.

**P1 — `find()` uruchamia sync**
- Plik: `TmdbXtreamAvailabilityRepository.kt:48-52`.
- Dowód: `syncMovies()`/`syncSeries()` przy każdym otwarciu szczegółów.
- Wpływ: powtarzalne sprawdzanie TTL; przy świeżym indeksie i tak pełny odczyt do `.size`.
- Pewność: wysoka. Status: POTWIERDZONE. Priorytet: **P1**.

**P1 — Buforowanie całego JSON**
- Plik: `XtreamProvider.kt:220-236`, linia `234`.
- Dowód: `resp.body?.string()` + `parseToJsonElement`.
- Wpływ: skok pamięci przy dużych katalogach; brak streamingu.
- Pewność: wysoka. Status: POTWIERDZONE. Priorytet: **P1**.

**P1 — Brak `streamUrl` i odcinków**
- Plik: `XtreamCatalogDb.kt:52-66`; `ContentRepository.kt:176-246`; `XtreamProvider.kt:117-184`.
- Dowód: brak `streamUrl` w lokalnych encjach; playback MOVIE/SERIES woła `movieDetails`/`seriesDetails`.
- Wpływ: sieć wymagana do odtwarzania; brak offline; dodatkowe opóźnienia.
- Pewność: wysoka. Status: POTWIERDZONE. Priorytet: **P1**.

**P1 — Brak lokalnego wyszukiwania**
- Plik: `SearchViewModel.kt:42`, `181`, `259`; `DiscoveryViewModel.kt:46-47`, `203`, `270`.
- Dowód: wyłącznie `TmdbCatalogRepository`/`TmdbDiscoveryRepository`.
- Wpływ: brak wyników spoza TMDB; brak offline.
- Pewność: wysoka. Status: POTWIERDZONE. Priorytet: **P1/P2**.

### 8.3. Dopasowania / cache

**P1 — Dopasowania nieutrwalane**
- Plik: `TmdbXtreamMatchRepository.kt:20-91`; `TmdbXtreamAvailabilityRepository.kt:41-103`.
- Dowód: brak DAO/encji; `match()` liczone na żądanie; brak wywołań produkcyjnych `TmdbXtreamMatchRepository`.
- Wpływ: powtarzalne dopasowania, brak historii, brak manualnej korekty.
- Pewność: wysoka. Status: POTWIERDZONE. Priorytet: **P1**.

**P1 — Cache TMDB tylko w pamięci**
- Plik: `TmdbDetailRepository.kt:18-19`, TTL `148`; `TmdbCatalogRepository.kt:27`, TTL `246`; `TmdbDiscoveryRepository.kt:28`, TTL `135`.
- Dowód: `ConcurrentHashMap`; brak DB.
- Wpływ: po restarcie ponowne pobieranie; limity API; brak offline.
- Pewność: wysoka. Status: POTWIERDZONE. Priorytet: **P1**.

**P2 — Brak negatywnego cache i `matchInputHash`**
- Plik: `TmdbXtreamMatcher.kt` (logika), `TmdbXtreamAvailabilityRepository.kt:54-69`.
- Dowód: brak stanu `NOT_FOUND`/`REJECTED`; brak hasha wejścia.
- Wpływ: powtarzalne próby dla treści bez dopasowania.
- Pewność: wysoka. Status: POTWIERDZONE. Priorytet: **P2**.

**P2 — Brak tabeli `external_ids`**
- Plik: `XtreamContentIndexDb.kt:40-41`; `XtreamCatalogDb.kt:62-63`.
- Dowód: tylko kolumny `tmdbId`/`imdbId`; brak innych ID i odwrotnego indeksu.
- Wpływ: ograniczone dopasowanie; duplikacja logiki.
- Pewność: wysoka. Status: POTWIERDZONE. Priorytet: **P2**.

**P2 — Klucz TMDB w URL**
- Plik: `TmdbApi.kt:203-204`.
- Dowód: `addQueryParameter("api_key", apiKey)`; HTTPS.
- Wpływ: klucz może trafić do logów/crash reports; mniejszy problem niż Xtream, ale wart polityki.
- Pewność: wysoka. Status: POTWIERDZONE. Priorytet: **P2**.

### 8.4. Biblioteka / UI / ustawienia

**P2 — `observeAll` + trzy flow**
- Plik: `LibraryDb.kt:35-42`; `LibraryViewModel.kt:79-91`.
- Dowód: `observeAll` ładuje wszystkie elementy; trzy zapytania przy każdym zapisie.
- Wpływ: zbędne odczyty przy każdym zapisie postępu.
- Pewność: wysoka. Status: POTWIERDZONE. Priorytet: **P2**.

**P2 — Zapis postępu read+upsert**
- Plik: `LibraryRepository.kt:36-68`; `PlayerScreen.kt` (zapis co ~15 s i na `ON_STOP`).
- Dowód: `dao.get` + `dao.upsert`.
- Wpływ: write churn; inwalidacje.
- Pewność: wysoka. Status: POTWIERDZONE. Priorytet: **P2**.

**P2 — Ustawienia zapisane, ale niekonsumowane**
- Plik: `ProfileSettingsDb.kt:24-72`; `Theme.kt:74`; brak referencji w HUD.
- Dowód: `themeMode`, `accentColor`, `liveTvHudTechStyle`, `liveTvHudShowEpgDescription`, `liveTvStartOnLastChannel`, `addonConfigsJson`, `appLanguageTag`, `notificationsEnabled`, `diagnosticsEnabled`.
- Wpływ: UI obiecuje funkcje, których nie ma.
- Pewność: wysoka. Status: POTWIERDZONE. Priorytet: **P2**.

**P1/P2 — Kids Mode nieegzekwowany**
- Plik: `ProfileSettings.kt:40-70`; `ProfileRepository.kt:57`; `SearchViewModel.kt:72`; `DiscoveryViewModel.kt:77`.
- Dowód: `kidsMode` przekazywany, ale nieużywany w `createDefaults`; brak filtra treści.
- Wpływ: tryb dziecięcy nie działa; ryzyko produktowe/rodzicielskie.
- Pewność: wysoka. Status: POTWIERDZONE. Priorytet: **P1** (produktowy).

**P2 — `onSelectEpisode` niezwarte**
- Plik: `PlayerHudOverlay.kt:72`; `PlayerScreen.kt:322`.
- Dowód: parametr przekazany, brak wywołania w HUD.
- Wpływ: wybór odcinka z HUD nie działa.
- Pewność: wysoka. Status: POTWIERDZONE. Priorytet: **P2**.

**P2 — `PlayerController` martwy kod**
- Plik: `PlayerController.kt:24-283`; referencje tylko w testach.
- Dowód: brak użycia w `app/src/main`.
- Wpływ: utrzymanie zbędnego kodu.
- Pewność: wysoka. Status: POTWIERDZONE. Priorytet: **P2**.

**P1 — Brak trasy `guide`**
- Plik: `MoreScreen.kt:27`; `MainShell.kt:495-537`.
- Dowód: `MoreItem("guide", ...)`; brak `composable("guide")`.
- Wpływ: kliknięcie „Przewodnik” może rzucić `IllegalArgumentException` i wywalić aplikację.
- Pewność: wysoka. Status: POTWIERDZONE. Priorytet: **P1**.

**P2 — Placeholdery**
- Plik: `MainShell.kt:511,524,527,530,533,536`.
- Dowód: 6 wywołań; 5 tras `settings/*` jest nieosiągalnych, bo `SettingsCategory.route` ma tylko `settings/sources` (`SettingsScreen.kt:50-80`, `518`); `recordings` jest osiągalne z `MoreScreen`/bottom nav.
- Wpływ: martwe trasy; mylący stan projektu.
- Pewność: wysoka. Status: POTWIERDZONE. Priorytet: **P2**.

### 8.5. Bezpieczeństwo

**P0 — Cleartext + poświadczenia w URL**
- Plik: `AndroidManifest.xml:15`; `XtreamProvider.kt:90,135,167,205,225-226`.
- Dowód: `usesCleartextTraffic="true"`; `$username/$password` w path/query.
- Wpływ: MITM, wyciek poświadczeń do logów/crash reports.
- Pewność: wysoka. Status: POTWIERDZONE. Priorytet: **P0**.

**Pozytyw — szyfrowanie hasła**
- Plik: `CryptoManager.kt:17-30`.
- Dowód: AES-GCM + Android Keystore; PIN PBKDF2 120k.
- Status: POTWIERDZONE (pozytyw). Priorytet: —

---

## 9. Rekomendacja techniczna

**Rekomendacja główna: B — pozostać przy Room i przebudować model katalogu oraz trwały cache metadanych/dopasowań. C — migracja na raw SQLite — odrzucona.**

**Uzasadnienie:**

1. **Brak dowodów na winę Room.** Wszystkie zidentyfikowane problemy to schemat, zapytania, sync i brak cache.
2. **Raw SQLite nie usuwa przyczyn.** Trzeba by napisać te same lepsze zapytania, a dodatkowo ręcznie obsłużyć mapowanie, transakcje, Flow, Paging, migracje i testy.
3. **Room daje narzędzia do naprawy:** `COUNT`, `LIMIT/OFFSET`, Paging 3, `@Upsert`, `@Transaction`, `withTransaction`, indeksy, FTS4, `@RawQuery`, `@Relation`, `MigrationTestHelper`.
4. **Skala nie jest argumentem za C.** Dziesiątki tysięcy rekordów to normalne obciążenie dla SQLite; problemem jest pełny replace i brak indeksów/paginacji, nie biblioteka.
5. **Ryzyko C jest wysokie, zysk nieudowodniony.** To duża przepisanka warstwy DB bez mierzalnego zysku.

**Zestaw natychmiastowy (A) — hotfixy, do zatwierdzenia osobno:**

- Dodać migrację `1→2` **albo** świadomie podnieść minimalną wersję DB / dodać `fallbackToDestructiveMigration` (decyzja właściciela; pierwsze rozwiązanie jest bezpieczniejsze dla danych).
- Dodać FK do `library_items` (przebudowa tabeli w migracji) i `sourceId` do `live_channel_history`.
- Dodać indeks `(sourceId, endTimeMs)` lub `(endTimeMs)` dla EPG.
- Zamienić `getForSource(...).size` na `COUNT(*)`.
- Dodać `LIMIT/OFFSET` lub Paging 3 do `getItems`, `observeAll`, `entriesForSource`.
- Dodać indeks pod sortowanie `sortTitle` (lowercase) zamiast `COLLATE NOCASE`.
- Dodać `contentHash`/`lastSeenAt`/`isAvailable` i przejść na `@Upsert` zamiast pełnego delete+insert.
- Utrwalić dopasowania TMDB i metadane w DB.
- Dodać `streamId`/`containerExtension` do odtwarzania VOD bez `get_vod_info`; dodać tabele odcinków.
- Naprawić trasę `guide` lub usunąć wpis.
- Podjąć decyzję o cleartext/poświadczeniach w URL (patrz sekcja 11).

**Zestaw średnioterminowy (B):**

- Nowy model z sekcji 7.
- Inkrementalny sync z tombstone’ami i `sync_runs`.
- Trwały cache TMDB + dopasowania + manual review.
- FTS lokalnego wyszukiwania.
- Paging 3 w UI.
- Wielokontowość katalogu (wszystkie konta albo świadomie tylko aktywne).
- WorkManager do sync w tle (do decyzji).
- Testy migracji i DAO.

**Czego nie robić:**

- Nie migrować na raw SQLite „dla wydajności” bez pomiarów.
- Nie usuwać Room tylko dlatego, że katalog jest duży.
- Nie zmieniać schematu bez zatwierdzenia i bez testów migracji.

---

## 10. Proponowany plan etapów (niezatwierdzony)

**Etap 0 — decyzje i przygotowanie**
- Zatwierdzenie modelu i zakresu.
- Backup repo/DB.
- Włączenie `exportSchema=true` + `room.schemaLocation` + `room-testing` (wymaga zgody na zmianę build config).
- Testy migracji dla obecnych 12 migracji.
- Kryteria: schema JSON w repo, testy migracji przechodzą, brak zmian funkcjonalnych.

**Etap 1 — bezpieczeństwo i integralność (hotfixy)**
- Migracja 1→2 lub decyzja o fallback.
- FK `library_items` + `live_channel_history.sourceId`.
- Indeks EPG `endTimeMs`.
- `COUNT(*)` zamiast `.size`.
- `contentKey` z `sourceId` + migracja istniejących kluczy.
- Trasa `guide`.
- Decyzja cleartext/URL.
- Kryteria: brak crashy migracji, brak osieroconych rekordów, testy migracji.

**Etap 2 — optymalizacja zapytań w obecnym modelu**
- `LIMIT/OFFSET` lub Paging 3.
- Indeksy pod sortowanie i filtry.
- Rozdzielenie `observeAll` na wyspecjalizowane zapytania.
- Batchowany `@Upsert` w sync.
- Kryteria: brak pełnych skanów w krytycznych ścieżkach; pomiary przed/po.

**Etap 3 — model katalogu v2**
- Nowe tabele z sekcji 7.
- Migracja danych z `xtream_catalog_items`/`xtream_content_index`.
- Inkrementalny sync, tombstone’y, `sync_runs`.
- Ujednolicenie źródła prawdy.
- Kryteria: jeden katalog, brak pełnego replace, spójność multi-account.

**Etap 4 — trwały cache TMDB i dopasowania**
- `tmdb_metadata`, `tmdb_seasons`, `tmdb_episodes`.
- `metadata_matches`, `match_candidates`.
- Pipeline dopasowania z `matchInputHash`, TTL, manual review.
- Kryteria: brak ponownych dopasowań przy każdym otwarciu; offline metadata.

**Etap 5 — odcinki i odtwarzanie offline**
- `seasons`, `episodes`.
- `streamId`/`containerExtension` dla MOVIE/SERIES.
- Budowa URL z `sources` zamiast `get_vod_info`.
- Kryteria: MOVIE/SERIES odtwarzane bez szczegółów providera; odcinki z DB.

**Etap 6 — UI i wyszukiwanie**
- Paging 3 w listach.
- Lokalne wyszukiwanie FTS + TMDB.
- Status sync, błędy, retry.
- Kids Mode (jeśli zatwierdzony).
- Kryteria: płynne przewijanie, wyniki lokalne, czytelne stany.

**Etap 7 — weryfikacja**
- Testy migracji, DAO, integracyjne.
- Testy na urządzeniu TV/mobile.
- Pomiary: czas pierwszego sync, rozmiar DB, pamięć, czas zapytań.
- Akceptacja właściciela.
- Kryteria: wszystkie testy, brak regresji, akceptacja.

Każdy etap powinien być osobnym, recenzowalnym commitem; bez łączenia etapów.

---

## 11. Pytania wymagające decyzji właściciela

1. **HTTP vs HTTPS:** czy dopuszczamy cleartext dla providerów Xtream, czy wymagamy HTTPS? Czy robimy wyjątki per źródło? Czy przenosimy poświadczenia z URL do nagłówków/proxy?
2. **Migracja 1→2:** czy dodajemy migrację, czy świadomie dopuszczamy `fallbackToDestructiveMigration` (utrata danych)? Czy v1 kiedykolwiek była wydana?
3. **Build config:** czy zatwierdzasz `exportSchema=true`, `room.schemaLocation` i `room-testing`? To zmiana konfiguracji chronionej.
4. **Schemat v15:** czy przebudowa `library_items` (FK) i dodanie tabel ma być jednym krokiem, czy etapami? Czy migracja istniejących `contentKey` jest dozwolona?
5. **Multi-account:** katalog ma pokazywać wszystkie konta czy tylko aktywne? Indeks już synchronizuje wszystkie — czy ujednolicamy?
6. **Kids Mode:** czy egzekwujemy przez certyfikaty TMDB, lokalne filtry, PIN, czy tylko oznaczamy? Jaki jest zakres treści dozwolonych?
7. **TMDB cache:** czy przechowywanie metadanych TMDB w DB jest zgodne z warunkami TMDB? Jaki TTL i atrybucja?
8. **Dopasowania:** czy potrzebny jest manual review (UI do zatwierdzania/odrzucania)? Czy dopuszczamy `AMBIGUOUS` bez wyboru?
9. **Odcinki:** czy źródłem prawdy są odcinki TMDB czy Xtream? Obecnie TMDB; czy to zostaje?
10. **Offline:** czy VOD/serie mają działać offline? Czy provider na to pozwala? Czy przechowujemy `streamId`/`containerExtension`?
11. **Wyszukiwanie lokalne:** czy dodajemy FTS po katalogu? Czy łączymy z TMDB i jak rankujemy?
12. **Obrazy:** Coil disk cache wystarczy, czy potrzebujemy tabeli `image_cache_index` i limitów rozmiaru?
13. **Sync w tle:** czy dodajemy WorkManager, czy sync tylko przy wejściu w ekran? Jaki interwał?
14. **Retencja:** ile dni trzymamy EPG, brakujące elementy katalogu, `sync_runs`, historię dopasowań?
15. **Martwy kod:** czy usuwamy `PlayerController`, `BrowseScreen`, nieosiągalne placeholdery i wpis `guide`? To osobny zakres porządkowy.
16. **Dokumentacja:** czy poprawiamy `docs/STATE.md` (rozbieżność `M` vs „brak zmian”) w ramach etapu dokumentacyjnego?
17. **Cele wydajności:** jaki maksymalny rozmiar katalogu, czas pierwszego sync, rozmiar DB, pamięć? Bez tego nie ma kryteriów akceptacji.

---

## 12. Ograniczenia audytu i elementy niezweryfikowane

**Ograniczenia:**

- Audyt był **statyczny**. Nie profilowałem aplikacji, nie mierzyłem czasu zapytań, rozmiaru DB, zużycia pamięci ani ruchu sieciowego.
- Nie uruchamiałem Gradle, testów, buildów ani CI. Nie mam wyników z tej sesji.
- Nie weryfikowałem migracji na rzeczywistej bazie urządzenia; `exportSchema=false` i brak testów migracji to właśnie luka.
- Nie wiem, jakie wersje DB istnieją na urządzeniach użytkowników. v1 jest w historii Git, ale brak tagów/release’ów; realny zasięg braku migracji 1→2 jest nieznany.
- Nie weryfikowałem zachowania UI na TV/mobile, fokusa, wydajności Compose, zachowania HUD na urządzeniu.
- Nie weryfikowałem limitów API TMDB, realnych rozmiarów odpowiedzi Xtream, liczby kont i katalogów.
- Nie czytałem sekretów, kluczy, keystore’ów ani wartości tokenów; nie ujawniam ich.
- Nie weryfikowałem statusu CI poza tym, co deklaruje `docs/STATE.md` (workflow #155 na `beec446`); nie uruchamiałem workflow.
- Nie rozstrzygam kwestii produktowych (Kids Mode, PVR, offline, multi-account) — to decyzje właściciela.
- Rozbieżność liczby plików testowych: poprzednio 38, obecnie 39; prawdopodobnie wcześniejszy licznik nie obejmował jednego pliku. Faktyczna liczba `@Test` to 223.
- Rozbieżność `docs/STATE.md:19` („brak tracked unstaged”) vs `M docs/STATE.md` — zgłoszona, nie poprawiana.

**Kontrola końcowa (wykonana):**

- `git status --porcelain` przed utworzeniem raportu:
  - `M docs/STATE.md`
  - `?? .opencode-permission-probe.txt`
  - `?? AGENTS.md`
- HEAD lokalny == HEAD zdalny: `beec44685dfe139c5a0acd8587a3af235a4dc99a`.
- **Zero zmian istniejących plików wykonanych przez audyt.**
- **Zero operacji Git zapisujących stan** (brak commit/push/merge/rebase/reset/checkout/clean/tag).
- **Zero uruchomień Gradle/testów/buildów.**
- Nie modyfikowałem `AGENTS.md`, `.opencode-permission-probe.txt`, `docs/STATE.md` ani żadnego istniejącego pliku projektu.
- Jedyną zmianą jest utworzenie tego pliku raportu.

**Podsumowanie jednym zdaniem:** Room nie jest przyczyną problemów; przyczyną jest model katalogu, pełny replace, brak paginacji/indeksów, brak trwałego cache dopasowań i ponowne pobieranie danych z sieci. Rekomendowany kierunek to **B (Room + nowy model + trwały cache)**, z **A** jako natychmiastowymi hotfixami; **C (raw SQLite) odrzucony**.
