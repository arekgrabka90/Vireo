# 0006 — Odzyskiwanie bazy danych

**Status:** Wymagania zatwierdzone przez właściciela. Specyfikacja: `../specs/data-safety.md`.

- Mechanizm odzyskiwania **nie resetuje bazy automatycznie**. Najpierw kopia bezpieczeństwa.
- Rozróżniamy: błąd migracji, uszkodzenie bazy, utratę klucza Android Keystore (oraz bazę nowszą od aplikacji).
- Nigdy `fallbackToDestructiveMigration`.
- Szczegóły obsługi (kopia przed migracją, dwie ostatnie kopie w pamięci wewnętrznej, eksport na życzenie): w specyfikacji.
