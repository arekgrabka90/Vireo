# 0006 — Odzyskiwanie bazy danych

**Status:** Wymagania zatwierdzone przez właściciela. Specyfikacja: `../specs/data-safety.md`.

- Mechanizm odzyskiwania **nie resetuje bazy automatycznie**. Najpierw kopia bezpieczeństwa.
- Rozróżniamy: błąd migracji, uszkodzenie bazy, utratę klucza Android Keystore (oraz bazę nowszą od aplikacji).
- Nigdy `fallbackToDestructiveMigration`.
- Szczegóły obsługi (kopia przed migracją, dwie ostatnie kopie w pamięci wewnętrznej, eksport na życzenie): w specyfikacji.

## Uzupełnienie 2026-11-15

- Domyślne `SupportSQLiteOpenHelper.Callback.onCorruption` usuwa plik bazy, co jest automatycznym resetem. Dlatego baza jest otwierana przez `NonDestructiveOpenHelperFactory` (E1.2c), a odczyt wersji przy starcie używa `DatabaseErrorHandler`, który niczego nie usuwa.
- Minimalna wspierana wersja bazy: 14 (P14).
- Kopia przedmigracyjna jest weryfikowana (`PRAGMA quick_check`) przed przycięciem starszych kopii i przed przywróceniem.
- Licznik niestabilnych startów (trzy kolejne) przenosi aplikację na ekran odzyskiwania, żeby uniknąć pętli awarii przy uszkodzeniu stron bazy.
