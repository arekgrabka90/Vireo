# 0005 — Synchronizacja katalogu Xtream

**Status:** Zatwierdzona kierunkowo. **Warunek:** spike parsera strumieniowego i test porównawczy (`../specs/xtream-sync.md`).

## Decyzja

- Zachowujemy obie tabele (`xtream_catalog_items` — przeglądanie aktywnego konta; `xtream_content_index` — dopasowanie TMDB wszystkich kont); mają różne role.
- Jedno pobranie (konto, typ) zasila obie projekcje; projekcja katalogu tylko dla aktywnego konta lub konta z istniejącym stanem katalogu; indeks zawsze.
- Bez nowych zależności: parser strumieniowy na `okio` z parsowaniem elementów przez `kotlinx-serialization`; nie zakładamy, że sama biblioteka zapewnia streaming tablic JSON — to potwierdza spike.
- Scalenie tabel, FTS i Paging dopiero po pomiarach przekraczających kryteria (wymagają osobnej decyzji i, w razie nowej zależności, zgody).
