# Raport E2.1 — parser strumieniowy Xtream (wariant B), niepodpięty

**Data:** 2026-11-18 · **Commit:** `af3d262` na `work/vireo-development` (baza `78e66f8`) · **CI:** zielone wg właściciela (numer runu niezapisany) · **Status:** w kodzie i CI; oczekuje akceptacji właściciela.

## Zakres

Cztery pliki dodane (622 linie), bez zmian w istniejących plikach:

- `data/xtream/JsonArrayElementSplitter.kt` (123 linie) — rozdzielacz elementów tablicy najwyższego poziomu na `okio.BufferedSource`; zakończenie: `COMPLETE`, `TRUNCATED`, `NOT_ARRAY`, `EMPTY`, `OVERSIZED`; limit elementu 1 MiB.
- `data/xtream/XtreamStreamingListParser.kt` (129) — mapowanie elementów na istniejące modele `Channel`, `Movie`, `Series` (lustro `XtreamProvider`), podsumowanie `XtreamListParseSummary` (zdekodowane, pominięte, zakończenie).
- `JsonArrayElementSplitterTest.kt` (151), `XtreamStreamingListParserTest.kt` (219).

Stara ścieżka (`XtreamProvider`, repozytoria, DI, Gradle, manifest) bez zmian. Parser jest niepodpięty. Pobieranie HTTP, upsert paczkami do obu tabel i polityki bezpieczeństwa synchronizacji należą do E2.2 i dalszych.

## Decyzje zatwierdzone przez właściciela (2026-11-18)

- D1: wiersz niemapowalny to wiersz bez `stream_id` / `series_id` (semantyka starej ścieżki); brak `name` daje `title = id`; wiersze nieobiektowe i uszkodzone są pomijane i liczone.
- D2: limit rozmiaru elementu 1 MiB.
- D3: testy porównawcze z fałszywym `Interceptor` OkHttp, bez nowych zależności.

## Różnica zamierzona względem starej ścieżki

Stara ścieżka (`strictObjects`) odrzuca całą listę, gdy którykolwiek element nie jest obiektem. Nowa pomija taki element i go liczy. Zakończenie inne niż `COMPLETE` ma być traktowane przez wywołującego jako nieudane pobranie (bez usuwania starych danych). Jest to udokumentowane testem.

## Testy

24 testy dodane w kodzie: 15 dla rozdzielacza (nawiasy w stringach, escape, zagnieżdżenia, skalary, puste wejścia, obiekt zamiast tablicy, BOM, ucięcie, element zbyt duży, UTF-8, odczyt po jednym bajcie, 20 tys. elementów) i 9 dla parsera (równoważność ze starym `XtreamProvider` dla filmów, seriali i kanałów, pominięcia, element uszkodzony, ucięcie, puste wejścia, `OVERSIZED`, dostarczanie elementów przed końcem wejścia).

Wykonanie: CI zielone wg właściciela. Liczby wykonanych testów nie potwierdzono niezależnym raportem.

## Weryfikacja wdrożenia

Skrypt Termux: preflight (branch, zgodność z `origin`, czysty katalog), zestaw staged dokładnie cztery pliki dodane, `git diff --cached --check`, push bez force. Lokalnie nie uruchamiano builda ani testów.

## Niezweryfikowane

- Wydajność na urządzeniu referencyjnym (czas, pamięć szczytowa według `specs/quality-metrics.md`) — E2.4.
- Zachowanie na rzeczywistych odpowiedziach dostawców.
- Równoważność zawartości obu tabel — E2.2.
- Koszt odczytu bajt po bajcie przy dużych payloadach na urządzeniu.

## Następny krok

E2.2 (wspólne pobranie zasilające obie projekcje, paczki, polityki pominięć i pustej odpowiedzi) — wymaga osobnej specyfikacji i zgody właściciela.
