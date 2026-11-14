# Specyfikacja: synchronizacja katalogu Xtream

**Status:** zatwierdzona kierunkowo (decyzja 0005). Etap E2. Warunek: spike parsera i test porównawczy.

## 1. Stan obecny [K]

- `xtream_catalog_items` (aktywne konto; LIVE/MOVIE/SERIES; przeglądanie po kategoriach) i `xtream_content_index` (wszystkie konta; MOVIE/SERIES; dopasowanie TMDB). Obie z kluczem zawierającym `sourceId` i pełnym delete+insert w transakcji.
- Oba repozytoria pobierają ten sam payload (`get_vod_streams`, `get_series`) osobno. `XtreamProvider` czyta całą odpowiedź do `String`, parsuje do drzewa `JsonElement`, `strictObjects`.
- Ochrona przed pustą odpowiedzią tylko w katalogu. Wspólny mutex katalogu; pierwsze wejście blokuje UI do końca synchronizacji.
- `getItems` bez `LIMIT`; dopasowanie `LIKE '%token%'` z `LIMIT 1000`.

## 2. Jedno pobranie zasilające obie projekcje

Obie projekcje są funkcjami tego samego payloadu `(konto, typ)`, więc wspólne pobranie jest bezpieczne pod poniższymi warunkami.

**Przepływ per `(konto, typ ∈ {MOVIE, SERIES})`** (LIVE zasila tylko projekcję katalogu):
1. Strumień odpowiedzi → rozdzielacz elementów tablicy → parsowanie elementu → mapowanie na encję katalogu i encję indeksu.
2. Paczki po 1000; po jednej krótkiej transakcji na paczkę (upsert do obu tabel); wiersze noszą znacznik czasu startu synchronizacji (`updatedAt` w katalogu, `indexedAt` w indeksie — kolumny istnieją).
3. Na końcu krótka transakcja: usunięcie wierszy starszych niż start synchronizacji w obu tabelach, zapis obu stanów synchronizacji, sprawdzenie, czy konto nadal istnieje.

**Zasady bezpieczeństwa:**
1. Klucze obu tabel zawierają `sourceId` — wiele kont nigdy się nie miesza.
2. Projekcja katalogu zapisywana dla konta aktywnego lub konta z istniejącym stanem katalogu; pozostałe dostają ją przy aktywacji (istniejące `ensureCatalog`). Indeks zawsze, dla wszystkich kont. Konta przetwarzane sekwencyjnie, aktywne pierwsze (ograniczenie pamięci).
3. Zmiana aktywnego konta w trakcie nie unieważnia wyniku (wiersze kluczowane źródłem). `StaleSourceException` dotyczy cache providera w pamięci, nie tych tabel.
4. Usunięcie konta w trakcie: końcowa transakcja to wykrywa i odrzuca wynik (sprzątanie wierszy źródła; kaskada FK do potwierdzenia w schemacie).
5. Sukces zapisywany w obu stanach razem albo w żadnym.
6. Wiersz, który się nie zmapuje, jest liczony; jeśli pominiętych >2% (min. 50), synchronizacja się nie udaje i **nie usuwa** starych danych.
7. Ochrona przed pustą lub drastycznie mniejszą odpowiedzią (<50% poprzedniej liczby przy ≥200 poprzednich): brak usuwania. Dziś ma ją tylko katalog; indeks musi ją dostać.
8. Pamięć rzędu paczki, nie payloadu. Czytelnicy (Room WAL) widzą mieszankę starych i nowych wierszy w trakcie; akceptowalne i udokumentowane.

Zakładany brak migracji (kolumny znaczników istnieją); potwierdzenie w E2.

## 3. Parser strumieniowy (zgodny z aktualnymi zależnościami)

Zależności [K]: kotlinx-serialization-json 1.8.1, OkHttp 4.12.0 (z `okio`). **Nie zakładamy, że sama kotlinx.serialization zapewnia streaming dużych tablic JSON** (w tej wersji istnieje eksperymentalne `decodeToSequence`, ale jego zachowanie i tolerancja błędów wymagają potwierdzenia).

- **Wariant A (spike):** `decodeToSequence` na `InputStream`. Ograniczenie: błąd w jednym elemencie przerywa całą sekwencję, więc nie spełnia wymogu pomijania złych wierszy.
- **Wariant B (rekomendowany):** własny rozdzielacz elementów tablicy najwyższego poziomu na `okio.BufferedSource` (uwzględnia łańcuchy i znaki ucieczki), a każdy element parsowany osobno (`parseToJsonElement`) z `try/catch`. Zero nowych zależności; tolerancyjny na błędne wiersze.
- **Spike (test jednostkowy):** syntetyczna tablica ~150 tys. elementów ze strumienia; asercje: pierwszy element po przeczytaniu ograniczonej liczby bajtów, szczyt pamięci, zachowanie przy błędnym elemencie, tablica pusta, obiekt zamiast tablicy, ucięty strumień.

## 4. Weryfikacja

- **Test porównawczy:** ten sam payload przez starą i nową ścieżkę daje identyczną zawartość obu tabel.
- **Test dwóch kont** z nakładającymi się `streamId` (brak mieszania).
- **Testy odporności:** korpus złych odpowiedzi (null, string zamiast obiektu, brakujące pola, 0 elementów, ucięcie), przerwanie w połowie (stare dane nietknięte), usunięcie konta w trakcie.
- **Wydajność** na lokalnym serwerze testowym wg `quality-metrics.md` (pamięć szczytowa ≤150 MB dla 100 tys. pozycji; czas po stronie aplikacji ≤40 s; zapis ≤10 s).

## 5. Zapytania

- Wiersze przeglądania: zapytanie z `LIMIT` (zwykły Room), bez ładowania całej kategorii.
- Lista kanałów kategorii: `LIMIT/OFFSET` lub kluczowanie po tytule; decyzja po pomiarze (Paging tylko, jeśli pomiar nie spełnia kryteriów).
- Dopasowanie TMDB: pomiar; FTS tylko przy przekroczeniu kryterium (szczegóły TMDB → dostępność ≤1,5 s p90 przy indeksie 100 tys.).

## 6. Poza zakresem

Scalenie tabel, FTS, Paging, WorkManager — dopiero po pomiarach i osobnej decyzji.
