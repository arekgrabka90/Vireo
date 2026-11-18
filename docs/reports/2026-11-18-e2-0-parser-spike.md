# Raport E2.0 — spike parsera strumieniowego Xtream

**Data:** 2026-11-18 · **Commit:** `78e66f8` (CI run #171 SUCCESS) · **Status:** wykonane; oczekuje akceptacji właściciela.

## Zakres

Test jednostkowy `app/src/test/java/app/vireo/data/spike/XtreamParserSpikeTest.kt`. Wyłącznie kod testowy, bez zmian w kodzie produkcyjnym. Porównuje:

- wariant A: `decodeToSequence` (kotlinx-serialization-json 1.8.1, tryb `ARRAY_WRAPPED`),
- wariant B: własny rozdzielacz elementów tablicy najwyższego poziomu na `okio.BufferedSource` i osobne parsowanie każdego elementu.

## Przypadki objęte testem (z kodu)

Równoważność wyników A i B na czystym payloadzie; liczba bajtów potrzebna do pierwszego elementu; 150 tys. elementów (B: żywa sterta poniżej 64 MB, A mierzone); pusta tablica; pusty strumień; obiekt zamiast tablicy; ucięty strumień; błędny element; skalary, `null` i elementy zagnieżdżone; wydruk sformatowany z BOM; element zbyt duży. Pomiary trafiają na stdout i do `build/e2-spike/report.txt`.

## Wynik

- Z asercji testu: A przerywa całą sekwencję na pierwszym błędnym elemencie (40 zdekodowanych, potem błąd); B pomija błędny element i go liczy. Wariant B spełnia wymóg tolerancji na błędne wiersze, wariant A nie.
- Asercje wariantu B (w tym żywa sterta poniżej 64 MB przy 150 tys. elementów) przeszły w CI run #171.
- Rekomendacja: wariant B, zgodnie z `specs/xtream-sync.md`, sekcja 3.

## Niezweryfikowane

- Wartości liczbowe pomiarów (czasy, szczyt sterty wariantu A) nie zostały przeniesione do tego raportu; nie odczytano ich w sesji, w której powstał raport. Źródło: log CI run #171 lub `build/e2-spike/report.txt`. Do uzupełnienia.
- Pomiary na urządzeniu referencyjnym (należą do E2.4).

## Następny krok

E2.1 (wykonane, patrz `2026-11-18-e2-1-streaming-parser.md`).
