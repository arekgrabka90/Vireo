# Specyfikacja: metodologia pomiarów i kryteria wydajności

**Status:** wymagania zatwierdzone (decyzja 0009); urządzenia referencyjne czekają na wskazanie (P5). Wartości są propozycjami do skalibrowania po pomiarach bazowych w E0.

## 1. Urządzenia referencyjne

Do wskazania przez właściciela: słaby TV (≤2 GB RAM, Android 9–11), średni TV lub Google TV, telefon niższej klasy, telefon średniej klasy. Przy `minSdk` 24 dodatkowo emulator API 24 do testów funkcjonalnych (nie wydajności). Każdy raport podaje model, wersję Androida, RAM, build aplikacji (commit), tryb (debug/release).

## 2. Metodologia

- **Narzędzia bez nowych zależności:** `adb shell am start -W` (start), `dumpsys gfxinfo` (płynność), `dumpsys meminfo` (pamięć), `dumpsys media.player` (instancje odtwarzaczy), logi czasów w aplikacji z podziałem etapów (sieć, parsowanie, zapis do bazy, interfejs).
- **Rozdzielenie aplikacji od sieci i dostawcy:** pomiary aplikacji na lokalnym serwerze testowym (skrypt Termux/Python serwujący syntetyczne odpowiedzi Xtream, XMLTV i strumień testowy HLS). Pomiary zależne od dostawcy (pierwsza klatka, EPG realnego konta) raportowane osobno jako informacyjne, z opisem sieci i dostawcy, bez kryterium zaliczenia; warunkiem zaliczenia jest część po stronie aplikacji.
- **Liczba prób:** start zimny 20 uruchomień po restarcie urządzenia (mediana i p90); synchronizacja 5; zapping 30; przewijanie siatki 3 sesje po 60 s; soak 1× 8 h (słaby TV) i 1× 4 h (telefon); próbkowanie pamięci co 5 min.
- Każdy wynik: wartości surowe, mediana, p90, odchylenie; odrzucenie pierwszego uruchomienia po instalacji tylko jeśli opisane.

## 3. Kryteria (propozycje)

1. Start zimny do pierwszego interaktywnego ekranu: p90 ≤3 s (TV), ≤2 s (telefon).
2. Pierwsza pełna synchronizacja ~100 tys. pozycji z lokalnego serwera: bez OOM, dodatkowa pamięć szczytowa ≤150 MB, czas aplikacji (parsowanie i zapis) ≤40 s, interfejs responsywny.
3. Zapis do bazy: ≤10 s dla 100 tys. wierszy.
4. Otwarcie kategorii (pierwsze 40 pozycji) po synchronizacji: ≤300 ms (p90).
5. Szczegóły TMDB → wynik dostępności przy indeksie 100 tys.: ≤1,5 s (p90); przekroczenie = sygnał do rozważenia FTS.
6. Zapping (część aplikacji, od wyboru do `prepare`): ≤300 ms; do pierwszej klatki z lokalnego strumienia ≤1,5 s.
7. LIVE Hub: pierwsza lista z cache ≤500 ms; NOW/NEXT dla 12 widocznych wierszy ≤300 ms z magazynu EPG.
8. TV Guide: otwarcie ≤700 ms; przewijanie ≤5% klatek janky (`dumpsys gfxinfo`) przy 300 kanałach i oknie 3 h.
9. Import XMLTV 50 MB (syntetyczny): bez OOM, szczyt pamięci ≤100 MB, ≤30 s po stronie aplikacji.
10. Pamięć po 1 h nawigacji: PSS ≤350 MB na TV 2 GB.
11. Soak 8 h LIVE: brak crasha i ANR, wzrost pamięci ≤10%.
12. Utrata sieci 30 s: odzyskanie ≤15 s po przywróceniu; failover do alternatywy ≤10 s.
13. Przejścia tło/pierwszy plan i Hub ↔ pełny ekran: kryteria z `playback-host.md`.
14. Panel kategorii: otwarcie ≤200 ms (p90).

## 4. TMDB: precyzja a pokrycie

- **Precyzja:** odsetek poprawnych wśród dopasowań przyjętych automatycznie (`EXACT_*`) na próbce ≥300, oceniane ręcznie. Cel ≥99% (błędne automatyczne dopasowanie jest gorsze niż brak). Raport z przedziałem ufności (przy n=300 nie wykażemy 99,5%).
- **Pokrycie:** odsetek tytułów faktycznie obecnych u dostawcy, które zostały dopasowane; próbka 200 losowych + 100 popularnych, etykiety ręczne. Cel informacyjny ≥85%.
- Osobno: odsetek `NEEDS_REVIEW` oraz odsetek pozycji katalogu dostawcy z powiązanym ID TMDB.
