# 0004 — Architektura EPG 1.0

**Status:** Zatwierdzona. Specyfikacja: `../specs/epg-store.md`.

## Decyzja

- Magazyn EPG kluczowany `epgChannelId` (warianty jakości tego samego kanału współdzielą program).
- Źródło masowe: XMLTV (`xmltv.php`), parsowany strumieniowo w tle, anulowalnie, z poprawną obsługą stref czasowych; fallback `get_short_epg` per kanał dla widocznych wierszy.
- Brak EPG nigdy nie blokuje oglądania kanału.
- Migracja addytywna (nowa tabela), z testem migracji w CI; dotychczasowa tabela per `streamId` pozostaje do zakończenia migracji.
- Hub i Overlay czytają ten sam magazyn.

## Poza zakresem 1.0

Przypomnienia, archiwum (catch-up), nagrywanie, wyszukiwanie w całym EPG, ręczne mapowanie kanał↔EPG, zewnętrzny URL XMLTV.
