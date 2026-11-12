# VIREO — Project State

**Stan dokumentu:** 2026-11-12
**Gałąź robocza:** `work/vireo-development`
**Zasada nadrzędna:** kod wygrywa nad dokumentacją. W razie rozbieżności należy sprawdzić aktualny kod źródłowy.

## 1. Źródło prawdy i stan repozytorium

Repozytorium GitHub `arekgrabka90/Vireo` jest źródłem prawdy dla projektu.

### Lokalny stan — audyt 2026-11-12

- Branch: `work/vireo-development`
- HEAD: `744e9a3e2d47ad4629f51bfe1e24ce95518fedd8`
- Working tree: czysty
- Zmiany staged: brak
- Zmiany unstaged: brak
- Pliki untracked: brak
- Backup przed synchronizacją:
  `~/VIREO_backups/PRE_SYNC_WIP_20261112_135918`

### Stan GitHub — po fetch 2026-11-12

- Remote HEAD: `b21fc01080f97e1d6ea4f3b6b705bfdffa3289e9`
- GitHub jest o jeden commit przed lokalną gałęzią.
- Jedyny dodatkowy commit:
  `b21fc01 docs: add multilingual VIREO README`
- Zmiany w tym commicie:
  - `README.md`
  - `README.pl.md`
  - `README.es.md`
- Nie stwierdzono zmian w kodzie aplikacji w tym dodatkowym commicie.
- Lokalne repozytorium nie zostało scalone ani zaktualizowane przez pull/merge.

**Wynik porównania:** lokalne pliki nie są identyczne z GitHub ze względu na trzy pliki README. Kod aplikacji nie został zmieniony przez jedyny dodatkowy commit zdalny.

### Snapshot dla Claude

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

Ten stan jest historyczny i został zastąpiony audytem z 2026-11-12, który potwierdził czyste lokalne repozytorium na `744e9a3`. Nie należy traktować tamtych sześciu plików jako aktualnego WIP.

## 2. Zasady pracy nad projektem

### Role

- **Claude:** główny projektant i recenzent; architektura, podział etapów, decyzje projektowe, przegląd diffów i logów.
- **ChatGPT:** implementacja zatwierdzonych ustaleń, przygotowanie skryptów Termux, analiza GitHub Actions i poprawki.
- **Użytkownik:** zatwierdzanie zmian, uruchamianie skryptów, decyzje końcowe oraz wykonywanie commitów i pushy.

### Obowiązkowy workflow

1. Najpierw audyt lokalnego repozytorium.
2. Przed synchronizacją zabezpieczenie lokalnej pracy.
3. Synchronizacja wyłącznie po wyraźnej zgodzie użytkownika.
4. Zmiany przygotowywane skryptami zapisanymi w `$HOME`.
5. Skrypt pokazuje pełny diff i nie wykonuje samodzielnie commita ani push.
6. Użytkownik zatwierdza zmiany i wykonuje commit/push.
7. Po publikacji analiza commita, diffu i GitHub Actions.
8. Błędy i konflikty naprawiane w tym samym kontrolowanym workflow.

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
| Kotlin Serialization | 1.6.3 |

Nie aktualizować tych wersji bez osobnej, zatwierdzonej decyzji.

## 5. Baza danych Room

Plik:
`app/src/main/java/app/vireo/data/local/AccountDb.kt`

Zweryfikowany stan kodu:

- `VireoDatabase` — wersja 14.
- `exportSchema = false`.
- Istnieje migracja `MIGRATION_13_14`.

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

Statusy poniżej wymagają potwierdzenia na podstawie historii commitów, aktualnego kodu, testów i decyzji projektanta. Nie należy ich wywodzić wyłącznie z wyniku CI.

| Etap | W kodzie | Testy przeszły | Zaakceptowany |
|---|---|---|---|
| 2B.3A | Do potwierdzenia | Do potwierdzenia | Niepotwierdzone |
| 2B.3B | Do potwierdzenia | Do potwierdzenia | Niepotwierdzone |
| 2B.4 | Do potwierdzenia | Do potwierdzenia | Niepotwierdzone |
| 2B.5 | Do potwierdzenia | Do potwierdzenia | Niepotwierdzone |
| Stage 3 | Do potwierdzenia | Do potwierdzenia | Niepotwierdzone |

### Materiały referencyjne

- Claude reference ZIP, baza `ec2b35c`: materiał referencyjny, nie został automatycznie zastosowany do repozytorium.
- Aktualny ZIP GitHub z 2026-11-12: snapshot `b21fc01`, suma SHA256 podana w sekcji 1.

## 8. CI — historia i aktualna weryfikacja

Workflow:
`.github/workflows/build.yml`

Zweryfikowane kroki:

- testy jednostkowe: `gradle testDebugUnitTest --stacktrace`
- przygotowanie stabilnego podpisywania z GitHub Secret
- build: `gradle assembleDebug --stacktrace`
- publikacja artefaktu `vireo-debug-apk`

Workflow używa Java 17 i Gradle 8.13. Uruchamia się dla `main`, `work/vireo-development` oraz `workflow_dispatch`.

### Historia naprawionych błędów

- #139 — brak argumentu `LiveChannelHistoryDao` w `PlayerViewModelIntegrationTest.kt`; poprawka w #140.
- #145 i #148 — test `series HUD identifies current episode`; późniejsze uruchomienia #146 i #149 przeszły.
- #151 — błędy składni Kotlin w `HudControls.kt`; naprawione w #152.
- #154 — ostatni zweryfikowany udany workflow na commicie `b21fc01`.

### Ostatni wynik CI

Workflow #154 zakończył się sukcesem:

- Unit tests: PASS
- Stable signing preparation: PASS
- Signed APK build: PASS
- Artifact upload: PASS

Artefakt: `vireo-debug-apk`, 14 363 386 bajtów. Dostępność raportowana do 2027-02-10.

Sukces CI nie zastępuje testów fizycznego urządzenia ani akceptacji etapu.

## 9. Otwarte kwestie i ograniczenia

- Brak obecnie potwierdzonego, otwartego błędu CI na ostatnim zweryfikowanym commicie `b21fc01`.
- Testy na fizycznym urządzeniu wymagają osobnego potwierdzenia.
- Statusy etapów 2B.3A–2B.5 oraz Stage 3 wymagają formalnego przeglądu.
- Walidacja migracji Room 13 → 14 na rzeczywistych danych urządzenia nie została tu potwierdzona.
- Lokalna gałąź jest o jeden commit za GitHub; synchronizacja nie została wykonana.

Nie wpisywać nierozstrzygniętych kwestii jako błędów aplikacji bez dowodów.

## 10. Ograniczenia techniczne

Bez osobnej zgody użytkownika nie wolno:

- dodawać FFmpeg;
- zmieniać podpisywania aplikacji;
- modyfikować workflow GitHub Actions;
- zmieniać wersji Gradle, AGP, Kotlin ani Media3;
- zmieniać sekretów GitHub;
- wykonywać commitów lub pushy w imieniu użytkownika;
- synchronizować lokalnej gałęzi bez wyraźnej zgody.

ARVIO może służyć jako techniczny punkt odniesienia, ale nie jako źródło do kopiowania implementacji.

## 11. Kryteria zamknięcia etapu

Etap może zostać zamknięty dopiero po:

1. wdrożeniu zatwierdzonego zakresu;
2. przeglądzie pełnego diffu;
3. przejściu wymaganych testów CI;
4. wykonaniu i odnotowaniu wymaganych testów fizycznego urządzenia;
5. zachowaniu wszystkich Player invariants;
6. aktualizacji tego dokumentu w ramach tego samego etapu;
7. formalnej akceptacji użytkownika.

**Ten dokument opisuje stan zweryfikowany na 2026-11-12. Każda kolejna zmiana projektu wymaga aktualizacji odpowiednich sekcji.**
