# Specyfikacja: magazyn EPG

**Status:** zatwierdzona (decyzja 0004). Etap E4.2.

## 1. Stan obecny [K]

`EpgRepository` pobiera program per kanał (`get_short_epg`, limit 50), zapisuje w tabeli `epg_programs` kluczowanej `(sourceId, streamId, startTimeMs)` z TTL i działa tylko dla aktywnego źródła. Nie ma parsera XMLTV, pobierania zbiorczego ani siatki. Siatka na setkach kanałów wymagałaby setek żądań, więc stan obecny nie wystarcza.

## 2. Cele

- Jedno źródło danych dla NOW/NEXT w Hub i dla siatki (Hub i Overlay).
- Poprawne czasy (strefy), w tle, anulowalnie, bez wpływu na odtwarzanie.
- Warianty jakości tego samego kanału współdzielą program (identyczne wiersze na referencyjnych zdjęciach).

## 3. Źródła

1. **XMLTV** (`xmltv.php` dostawcy Xtream) — pobieranie zbiorcze, parsowanie strumieniowe przez `XmlPullParser` (wbudowany w Android, bez nowej zależności). Gzip: obsługa transparentna OkHttp oraz wykrycie nagłówka `1f 8b` w treści.
2. **Fallback `get_short_epg` per kanał** — leniwie dla widocznych wierszy bez danych (współbieżność ≤4), wynik trafia do tego samego magazynu.

Mapowanie kanał → program po `epgChannelId` kanału; kanały bez `epgChannelId` nie mają EPG (komunikat „Brak danych programu”).

## 3a. Czas

- Format XMLTV: `RRRRMMDDGGMMSS +SSMM`. Odczyt z uwzględnieniem przesunięcia strefy; zapis w UTC (`ms`).
- Brak przesunięcia w danych: zgodnie ze specyfikacją XMLTV traktowany jako UTC. Dostawcy przekazujący czas lokalny bez strefy wymagają ustawienia przesunięcia per źródło (minuty) — wymaga ekranu `settings/epg` (dziś placeholder); zakres minimalny do potwierdzenia w E4.2.
- Okno przechowywania: od teraz −6 h do teraz +48 h; starsze wiersze są czyszczone zapytaniem z indeksem po czasie zakończenia.
- Wyświetlanie: strefa urządzenia; zmiana czasu letniego/zimowego nie może przesuwać osi (obliczenia w ms UTC).

## 4. Schemat (propozycja, migracja addytywna `14→15`)

- `epg_programs_v2(sourceId, epgChannelId, startTimeMs PK, endTimeMs, title, description, category, generation)`; indeksy: `(sourceId, epgChannelId, endTimeMs)` i `(sourceId, endTimeMs)`.
- `epg_sync_state_v2(sourceId PK, lastAttemptAt, lastSuccessAt, etag, lastModified, status, error)`.
- Dotychczasowe `epg_programs` i `epg_sync_state` zostają do zakończenia migracji i weryfikacji; usunięcie osobną decyzją.
- Migracja addytywna, z testem w CI (`MigrationTestHelper`); bez utraty danych.

## 5. Pobieranie i parsowanie

- W tle (`applicationScope`), anulowalne (`isActive` sprawdzane w pętli; anulowanie przy opuszczeniu aplikacji, zmianie źródła i usunięciu konta).
- Paczki po 1000 programów; transakcja per paczka; wiersze noszą numer generacji bieżącej synchronizacji; na końcu krótka transakcja usuwa starsze generacje i zapisuje stan. Anulowanie lub błąd w środku zostawia spójne dane (poprzednie + częściowe nowe).
- Bez wczytywania całej treści do `String`; pamięć rzędu paczki.
- Filtr: tylko kanały obecne w katalogu źródła i okno czasowe.
- Odświeżanie: nie częściej niż interwał z ustawień (domyślnie 360 min); żądania warunkowe (`ETag`/`Last-Modified`); jedno pobieranie naraz na źródło; po błędzie wykładniczy odstęp (15 min → 1 h → 6 h); start po wyświetleniu katalogu, nie blokuje UI.
- Błąd lub brak EPG **nigdy** nie blokuje oglądania kanału.

## 6. Testy

- Parser: przesunięcia `+0100`, `-0500`, brak przesunięcia, przejście zmiany czasu, elementy uszkodzone pomijane z licznikiem, duży plik syntetyczny (pamięć), anulowanie w środku.
- DAO i migracja: instrumentowane (czyszczenie okna, generacje, `14→15`).
- Spójność: Hub i Overlay czytają ten sam magazyn (test: ten sam program NOW/NEXT w obu widokach).

## 7. Kryteria

- Import 50 MB syntetycznego XMLTV: bez OOM, szczyt pamięci ≤100 MB, ≤30 s po stronie aplikacji (lokalny serwer testowy).
- NOW/NEXT dla 12 widocznych wierszy ≤300 ms z magazynu.
- Anulowanie w środku importu nie psuje danych i nie blokuje UI.
