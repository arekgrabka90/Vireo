# VIREO — ROADMAP do wydania 1.0

**Stan:** 2026-11-17 · Kierunek i podział na etapy zatwierdzone przez właściciela. Rozmiar: S/M/L (względny, bez dat). Statusy: W kodzie / Testy / Zaakceptowany.

## Zasady

- Etapy realizujemy w kolejności zależności. Każdy etap ma zakres, DoD, weryfikację i raport w `reports/`.
- Nie dodajemy zależności ani nie zmieniamy konfiguracji chronionej bez osobnej zgody (`decisions/0001-roles-and-workflow.md`).
- Każda migracja Room ma test w CI przed wdrożeniem. Zakazane: `fallbackToDestructiveMigration`.
- Wydajność mierzymy według `specs/quality-metrics.md` (urządzenia referencyjne, powtarzalna metodologia).

## Ścieżka krytyczna i równoległość

**E0 → E1 → (E2 ∥ E3) → E4 → E6 → E8 → E9 → E10**; E5 równolegle z E4 po E2; E7.1 przed pracą nad UI E4, E7.2 równolegle do E3–E6.

## Etapy

### E0 — Porządki, dokumentacja i baza pomiarowa (S)
- **Cel:** wiarygodny punkt wyjścia.
- **Zakres:** przebudowa dokumentacji (ten commit); formalny przegląd etapów historycznych; potwierdzenie hipotez [H] ze `STATE.md`; lista kandydatów do usunięcia poza `docs/` (czeka na zgodę); decyzja o Gradle Wrapper; pomiary bazowe na urządzeniach referencyjnych.
- **Zależności:** brak. **DoD:** dokumentacja spójna z kodem; hipotezy potwierdzone lub odrzucone; zatwierdzona lista usunięć; zapisane wartości bazowe.
- **Weryfikacja:** przegląd diffu; pomiary według specyfikacji metryk.
- **Ryzyko:** zbyt szerokie czyszczenie (dlatego zgoda z góry). **Decyzje:** urządzenia referencyjne, usunięcia, Wrapper.

### E1 — Dane i dostęp (M)
- **Status (2026-11-17): ZAMKNIĘTY I ZAAKCEPTOWANY.** E1.1, E1.2 (a–d) i E1.3 — w kodzie i CI. E1.3: commit `14e864a`, run #169 SUCCESS (`build` i `android-test`). Właściciel potwierdził wykonanie testów na urządzeniu i zaakceptował cały E1 dnia 2026-11-17. P15 pozostaje osobną otwartą decyzją i nie blokuje zamknięcia E1. Release R8 wymaga osobnej zgody na zmianę CI.
- **Cel:** żadna aktualizacja nie niszczy danych; dostęp do profili i źródeł jest chroniony.
- **Zakres:** polityka wersji bazy (decyzja P14: minimalna wspierana wersja 14); odzyskiwanie bazy bez automatycznego resetu (`specs/data-safety.md`); sprzątanie danych po usunięciu profilu w transakcji; limit prób PIN; nieusuwająca fabryka otwierania bazy i licznik niestabilnych startów; testy migracji od wersji 14 w górę; przegląd cleartextu i redakcji logów; (osobna zgoda) build release z R8 w CI.
- **Zależności:** E0. **DoD:** każda migracja od wersji 14 w górę ma test w CI; baza w nieobsługiwanej wersji, uszkodzona lub nowsza od aplikacji trafia na ekran odzyskiwania i nigdy nie jest resetowana ani usuwana automatycznie; brak osieroconych wierszy po usunięciu profilu; PIN z limitem prób.
- **Weryfikacja:** testy instrumentowane (uszkodzony plik zachowany, kontrola spójności kopii, sprzątanie po usunięciu profilu), testy jednostkowe klasyfikatora i licznika startów, ręczna weryfikacja na urządzeniu (restart po przywróceniu, eksport na TV).
- **Ryzyko:** R8 może wyciąć klasy refleksyjne (Room, Hilt, serializacja). **Decyzje:** zmiana CI, polityka odzyskiwania.

### E2 — Synchronizacja Xtream (M, równolegle do E3)
- **Cel:** stabilna synchronizacja dużych katalogów bez zmiany modelu danych.
- **Zakres:** `specs/xtream-sync.md` — jedno pobranie zasilające obie projekcje, parser strumieniowy po spike'u, tolerancja na błędne wiersze, ochrona przed pustą odpowiedzią w indeksie, zapytania z `LIMIT`, zapytania kanałów po kategorii.
- **Zależności:** E1 — spełniona, etap zamknięty 2026-11-17. **DoD:** kryteria wydajności;
- **Weryfikacja:** testy jednostkowe parsera na korpusie złych odpowiedzi; benchmark na urządzeniu referencyjnym z lokalnego serwera testowego.
- **Ryzyko:** różnice między dostawcami. **Decyzje:** scalenie tabel, FTS lub Paging tylko jeśli pomiary je wymuszą.

### E3 — Odtwarzacz i host odtwarzania (L, równolegle do E2)
- **Cel:** niezawodne odtwarzanie, także po wyjściu z aplikacji i powrocie.
- **Zakres:** `specs/playback-host.md` — spike hosta (S1), wspólny host, tune coordinator, wybór audio i napisów, audio focus, `handleAudioBecomingNoisy`, watchdog buforowania, nasłuch sieci (wymaga `ACCESS_NETWORK_STATE`), klawisze multimedialne w aktywności, fallback `.m3u8` → `.ts` (`decisions/0008`), stabilność tła, przywracanie stanu po zabiciu procesu.
- **Zależności:** E1 — spełniona, etap zamknięty 2026-11-17. **DoD:** brak restartu transmisji
- **Weryfikacja:** testy jednostkowe polityk, testy na urządzeniach, soak.
- **Ryzyko:** refaktoryzacja hosta dotyka stabilnego kodu; dekodery Android TV. **Decyzje:** zmiana manifestu, rozstrzygnięcie martwego kodu.

### E4 — LIVE TV (L)
- **E4.1 LIVE Hub (bez siatki):** panel kategorii, wspólny stan LIVE, kolejka zappingu, ulubione per profil (nowa encja, migracja), ostatnie, numer kanału, wyszukiwanie kanałów, podgląd na hoście, układ kompaktowy telefonu (lista z NOW/NEXT z fallbacku EPG), zastosowanie `liveTvStartOnLastChannel`.
- **E4.2 Magazyn EPG:** XMLTV w tle, anulowalny, strefy czasowe; schemat po `epgChannelId`; migracja addytywna (`specs/epg-store.md`).
- **E4.3 Siatka w Hub:** układ ze specyfikacji (podgląd, informacje o programie, oś czasu).
- **E4.4 TV Guide Overlay** w odtwarzaczu.
- **E4.5 Integracja i wydajność.**
- **Zależności:** E2, E3, E7.1 (E4.3 wymaga E4.2; E4.4 wymaga E4.3 i E3). **Kryteria akceptacji:** `specs/live-tv-hub-and-guide.md`, sekcja „Kryteria akceptacji E4”.
- **Ryzyko:** rozmiar i limity XMLTV, jakość `epgChannelId`, wydajność siatki na słabym TV, własne zarządzanie fokusem D-pad.

### E5 — VOD, wyszukiwanie, biblioteka i klient TMDB (M, równolegle do E4)
- Bezpośrednie wznowienie z „Kontynuuj”, cache informacji o serialu, pozycje bez dopasowania TMDB w bibliotece, osobny klient HTTP dla TMDB z cache zgodnym z warunkami, nagłówek autoryzacji zamiast klucza w URL, konfigurowalny adres bazowy, pomiar precyzji i pokrycia dopasowań.
- **Zależności:** E2. **DoD:** wznowienie jednym kliknięciem po restarcie; kryteria dopasowania.
- **Decyzje:** które ścieżki przeglądania VOD zostają; strategia klucza TMDB (`decisions/PENDING.md`).

### E6 — Profil z ograniczeniami (M)
- `specs/restricted-profiles.md`: ograniczenia egzekwowane w warstwie danych i logiki, PIN właściciela dla operacji zarządzania, zakres i granice opisane w UI.
- **Zależności:** E1, podstawy E4 i E5. **Decyzje:** model danych (allowlista kategorii) i zmiany schematu.

### E7 — System projektowy i UX (L)
- **E7.1 (przed UI E4):** tokeny, komponenty, zestaw fokusu D-pad (siatki, listy, warstwy modalne), tryb immersyjny powłoki, rozróżnienie wizualne stanów (gra / zaznaczony / fokus).
- **E7.2:** stany ładowania, puste i błędów, dostępność, lokalizacja PL/EN/ES (napisy zahardkodowane, `Retry`), dopracowanie układów.

### E8 — Bramka jakości i wydajności (L)
- Testy regresji i instrumentowane, pomiary wg `specs/quality-metrics.md`, soak, matryca urządzeń, lint, lokalny eksport diagnostyki. **DoD:** kryteria spełnione lub formalnie zaakceptowane odstępstwa.

### E9 — Kandydat wydania i beta (M)
- Zamrożenie funkcji, zamknięta beta, build release z R8, dokumentacja użytkownika, polityka prywatności, atrybucja i licencje (TMDB), finalne rozwiązanie klucza TMDB, zgodność z regulaminem dystrybucji.

### E10 — Wydanie 1.0 (S)
- Publikacja, monitoring, proces hotfixów.

## Wydanie 1.0: obowiązkowe

- **LIVE TV:** Hub (lista/siatka z NOW/NEXT, panel kategorii, ulubione, ostatnie, zapping, numer kanału, wyszukiwanie kanałów, podgląd), **TV Guide Overlay**, EPG 1.0 (XMLTV + fallback).
- **Dane i bezpieczeństwo:** migracje przetestowane, odzyskiwanie bez automatycznego resetu, limit prób PIN, sprzątanie po usunięciu profilu, odporna synchronizacja, build release z R8.
- **Odtwarzacz:** wybór audio i napisów, audio focus, host odtwarzania, stabilność tła, odzyskiwanie po utracie sieci, fallback `.ts`, klawisze multimedialne.
- **VOD:** dopasowanie TMDB z mierzoną precyzją, wznowienie z „Kontynuuj”, biblioteka.
- **Profile:** profil z ograniczeniami egzekwowany w logice danych.
- **Produkt:** system projektowy, lokalizacja PL/EN/ES, dostępność podstawowa, dokumentacja, polityka prywatności, atrybucja TMDB, rozwiązanie klucza TMDB.

## Po 1.0 (1.1 i dalej)

MediaSession (`media3-session`), PiP i odtwarzanie w tle; archiwum (catch-up), przypomnienia, nagrywanie, wyszukiwanie w całym EPG, kolorowanie gatunków, ręczne mapowanie EPG, zewnętrzny XMLTV, wiele list ulubionych; wybór jakości HLS, prędkość, proporcje, zewnętrzne napisy; źródła per profil; M3U i XMLTV jako dodatkowe źródła; WorkManager, Baseline Profile; scalenie tabel katalogu, FTS, Paging (jeśli pomiary wymuszą wcześniej — przesuwane do E2); modularyzacja Gradle; migracja Hilt `kapt` → KSP.

## Checklista gotowości do wydania

- [ ] E0–E8 zaakceptowane (kod, testy, właściciel).
- [ ] Migracje przetestowane od najstarszej wspieranej wersji i na kopii realnej bazy; odzyskiwanie bazy przetestowane dla trzech klas błędów.
- [ ] Brak błędów blokujących, crashy i utraty danych; brak poświadczeń w logach.
- [ ] Kryteria `specs/quality-metrics.md` spełnione na urządzeniach referencyjnych.
- [ ] Soak 8 h LIVE bez crasha; przejścia Hub ↔ pełny ekran ↔ Guide bez przerw w transmisji.
- [ ] Test na min. 2 TV (w tym słabszy) i 2 telefonach.
- [ ] Build release z R8 przeszedł smoke test; ścieżka aktualizacji działa.
- [ ] Polityka prywatności, dokumentacja, atrybucja TMDB, licencje; rozwiązanie klucza TMDB wdrożone; kanał dystrybucji potwierdzony.
- [ ] Precyzja dopasowań TMDB zmierzona.
