# Raport: audyt bazowy 2026-11-14

**Baza:** gałąź `work/vireo-development`, zdalny HEAD `2db5db3`. **Metoda:** statyczny odczyt zdalny przez konektor GitHub; **nie uruchamiano** buildu, testów, aplikacji ani urządzeń. Raport rozdziela fakty potwierdzone w kodzie [K], ustalenia z wcześniejszego audytu [A2] (`beec446`, usunięty z `docs/audits/` w tej przebudowie, dostępny w historii Git) i hipotezy [H][U].

## 1. Co przeczytano

`docs/STATE.md` (poprzedni), `docs/audits/*`, `build.gradle.kts`, `settings.gradle.kts`, `app/build.gradle.kts`, `AndroidManifest.xml`, `.github/workflows/build.yml`, `MainActivity`, `MainShell`, `MoreScreen`, `AppModule`, `AccountDb`, `LibraryDb`, `XtreamCatalogDb`, `XtreamContentIndexDb`, `CryptoManager` (+ listing), `XtreamProvider`, `XtreamCatalogRepository`, `XtreamContentIndexRepository`, `TmdbXtreamMatcher`, `TmdbApi`, `ContentRepository`, `EpgRepository`, `LibraryRepository`, `ProfileRepository`, `ProfileSettings`, `ProfilesViewModel`, `LibraryViewModel`, `BrowseViewModel`, `TitleListViewModel`, `TmdbDetailViewModel`, `LiveTvScreen`, `PlayerEngine`, `Media3PlayerEngine`, `PlayerController`, `PlayerViewModel`, `PlayerScreen`, `PlaybackSession`, `PlaybackRecoveryPolicy`, `HudControls`, `HudGuideOverlay` oraz listingi katalogów `ui/*`, `player/*`, `data/*`, testów.

**Nie czytano:** `HomeViewModel`, `SearchViewModel`, `DiscoveryViewModel`, ekrany Settings i Sources, `TmdbXtreamAvailabilityRepository`, `XtreamEpgParser`, repozytoria TMDB (cache), zasoby stringów. Wyszukiwarka kodu GitHub zwracała puste wyniki, więc stwierdzenia „nie znaleziono użycia” dotyczą tylko czytanych plików.

## 2. Ustalenia potwierdzone w kodzie [K]

Szczegóły i dowody w `STATE.md` (sekcje 4–7). Najważniejsze:

- Trasa `guide` w `MoreScreen` bez odpowiednika w `MainShell` (awaria nawigacji).
- Brak migracji `1→2`, brak `fallbackToDestructiveMigration` (crash przy nieobsługiwanej wersji).
- `library_items` bez FK i bez sprzątania po usunięciu profilu.
- Dwie tabele katalogu z różnymi rolami i podwójnym pobraniem payloadu; pełny delete+insert; parsowanie całej odpowiedzi w pamięci; `strictObjects`; zapytania bez `LIMIT`.
- PIN bez limitu prób; flaga profilu dziecięcego bez działania.
- LIVE: lista kategorii, kolejka odtwarzania jednoelementowa, adres tylko `.m3u8`; EPG per kanał, tylko aktywne źródło.
- Odtwarzacz: silnik `@ViewModelScoped`, brak audio focus, wyboru ścieżek, obsługi tła w dłuższej przerwie; cykl życia ON_STOP/ON_START.
- Hasła kont szyfrowane (AES-GCM, Keystore) — **wcześniejsza hipoteza o jawnych hasłach jest nieprawdziwa**.
- Cleartext globalnie; klucz TMDB w kliencie jako `api_key` w URL.
- CI: tylko debug; `android-test` przy każdym pushu.

## 3. Ustalenia z wcześniejszego audytu [A2], niezweryfikowane ponownie

Cache TMDB tylko w pamięci; wyszukiwanie wyłącznie przez TMDB; dopasowania nieutrwalane, `TmdbXtreamMatchRepository` bez użycia produkcyjnego; `liveTvStartOnLastChannel` i część ustawień niekonsumowana; `onSelectEpisode` niepodpięte w HUD; `BrowseScreen` nieużywany (a `BrowseViewModel` używany przez LIVE); `EpgDao.deleteExpired` bez indeksu po `endTimeMs`; 6 wywołań `PlaceholderScreen`; `contentKey` Xtream bez `sourceId`; `live_channel_history.sourceId` bez FK; 39 plików testowych, 223 `@Test`.

## 4. Korekty względem wcześniejszych ustaleń

- Hasła kont są szyfrowane (korekta).
- Dwie tabele katalogu pełnią różne role; nie są prostą duplikacją (korekta formułowana wcześniej jako „scalić”). Scalenie jest decyzją warunkową po pomiarach.
- `PlayerController` jest prawdopodobnie martwym kodem (a nie warstwą odtwarzacza).
- Ustawienie `liveTvStartOnLastChannel` istnieje, ale według A2 nie jest konsumowane; wymaga implementacji w E4.1.

## 5. Ograniczenia

- Brak uruchomionych testów i buildów; brak pomiarów; brak urządzeń.
- Konektor GitHub miał uprawnienia tylko do odczytu (odpowiedź 403 na zapis zawartości i drzew); zmiany przeniesiono paczką i skryptem Termux (patrz `decisions/0001`, `PENDING.md` P16).
- Realny zasięg braku migracji `1→2` nieznany (brak tagów i wydań).
